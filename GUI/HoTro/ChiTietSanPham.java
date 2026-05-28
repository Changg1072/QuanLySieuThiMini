package GUI.HoTro;

import Data.ChiTietLoHang;
import Data.LoHang;
import Data.SanPham;
import Logic.ChiTietLoHangLogic;
import Logic.LoHangLogic;
import Logic.NhaCungCapLogic;
import Logic.QuanLyAnh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChiTietSanPham {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =========================================================
    // DTO NỘI BỘ: Gom dữ liệu lịch sử nhập cho 1 dòng bảng
    // =========================================================
    private static class LichSuNhapRow {
        String maLoHang;
        String ngayNhap;
        int soLuong;
        BigDecimal giaNhap;
        String tenNCC;

        LichSuNhapRow(String maLoHang, String ngayNhap, int soLuong, BigDecimal giaNhap, String tenNCC) {
            this.maLoHang = maLoHang;
            this.ngayNhap = ngayNhap;
            this.soLuong  = soLuong;
            this.giaNhap  = giaNhap;
            this.tenNCC   = tenNCC;
        }
    }

    // =========================================================
    // HÀM GỌI CHÍNH (STATIC) ĐỂ HIỂN THỊ POPUP Ở BẤT CỨ ĐÂU
    // Cập nhật: Thêm tham số onCloseCallback để tải lại dữ liệu
    // =========================================================
    public static void showModal(Component parentComponent, SanPham sp, int tonKho, Runnable onCloseCallback) {
        Window parentWindow = SwingUtilities.getWindowAncestor(parentComponent);

        // 1. TẠO OVERLAY KÍNH MỜ (BLUR BACKGROUND)
        BufferedImage blurredBg = captureAndBlurScreen(parentWindow);
        JDialog overlay = new JDialog(parentWindow, Dialog.ModalityType.MODELESS);
        overlay.setUndecorated(true);
        if (parentWindow != null) overlay.setBounds(parentWindow.getBounds());
        overlay.setBackground(new Color(0, 0, 0, 0));

        JPanel pnlOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (blurredBg != null) {
                    g2.drawImage(blurredBg, 0, 0, getWidth(), getHeight(), null);
                }
                g2.setColor(new Color(15, 23, 42, 170));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        pnlOverlay.setOpaque(false);
        overlay.setContentPane(pnlOverlay);

        // 2. TẠO DIALOG POPUP CHÍNH
        JDialog dialog = new JDialog(parentWindow, "Chi Tiết Sản Phẩm", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setOpacity(0f);

        // 3. QUERY DB TRÊN LUỒNG CHÍNH
        List<LichSuNhapRow> lichSuList = truyVanLichSuNhap(sp.getMaSP());
        int     tongSLDaNhap = lichSuList.stream().mapToInt(r -> r.soLuong).sum();
        String  tenNCCGanNhat = lichSuList.isEmpty() ? "Chưa có dữ liệu" : lichSuList.get(0).tenNCC;

        JPanel pnlMain = taoMainLayout(dialog, overlay, sp, tonKho, lichSuList, tongSLDaNhap, tenNCCGanNhat, onCloseCallback);
        dialog.setContentPane(pnlMain);
        dialog.pack();
        dialog.setLocationRelativeTo(parentWindow);

        // Đóng khi click ra ngoài hoặc bấm ESC
        pnlOverlay.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dongPopup(dialog, overlay); }
        });
        dialog.getRootPane().registerKeyboardAction(e -> dongPopup(dialog, overlay),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 4. HIỂN THỊ VÀ CHẠY ANIMATION
        overlay.setVisible(true);
        chayAnimationFadeIn(dialog);
        dialog.setVisible(true);
    }

    // =========================================================
    // 🔥 QUERY DỮ LIỆU THẬT TỪ DB
    // =========================================================
    private static List<LichSuNhapRow> truyVanLichSuNhap(String maSP) {
        List<LichSuNhapRow> result = new ArrayList<>();
        try {
            ChiTietLoHangLogic ctLogic = new ChiTietLoHangLogic();
            List<Object[]> dsLichSu = ctLogic.layLichSuNhapTheoSP(maSP);
            
            if (dsLichSu != null) {
                for (Object[] row : dsLichSu) {
                    String maLoHang = (String) row[0];
                    String tenNCC = (String) row[1];
                    LocalDate ngayNhapKho = (LocalDate) row[2];
                    BigDecimal giaNhap = (BigDecimal) row[3];
                    int soLuongNhap = (int) row[4];
                    String ngayNhap = (ngayNhapKho != null) ? ngayNhapKho.format(DATE_FMT) : "—";
                    
                    result.add(new LichSuNhapRow(maLoHang, ngayNhap, soLuongNhap, giaNhap, tenNCC));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // =========================================================
    // XÂY DỰNG LAYOUT CHÍNH (CHIA TỈ LỆ 3/7)
    // =========================================================
    private static JPanel taoMainLayout(JDialog dialog, JDialog overlay, SanPham sp, int tonKho,
                                         List<LichSuNhapRow> lichSuList,
                                         int tongSLDaNhap, String tenNCCGanNhat, 
                                         Runnable onCloseCallback) {
        JPanel pnlMain = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 255, 255, 250),
                                                     0, getHeight(), new Color(230, 250, 245, 250));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(226, 232, 240, 150));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
                g2.dispose();
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(30, 30, 30, 30));
        pnlMain.setPreferredSize(new Dimension(1100, 640));

        // Nút X đóng
        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnClose.setForeground(new Color(156, 163, 175));
        btnClose.setContentAreaFilled(false); btnClose.setBorderPainted(false); btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dongPopup(dialog, overlay));
        btnClose.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnClose.setForeground(new Color(239, 68, 68)); }
            public void mouseExited(MouseEvent e)  { btnClose.setForeground(new Color(156, 163, 175)); }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; 
        gbc.weighty = 1.0; 

        // 👉 PANEL TRÁI (30%) - Truyền callback vào
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3; 
        gbc.insets = new Insets(0, 0, 0, 25); 
        pnlMain.add(taoPanelTrai_ThongTinSP(dialog, overlay, sp, tonKho, onCloseCallback), gbc);

        // 👉 PANEL PHẢI (70%)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7; 
        gbc.insets = new Insets(0, 0, 0, 0);
        pnlMain.add(taoPanelPhai_LichSuNhap(sp, btnClose, lichSuList, tongSLDaNhap, tenNCCGanNhat), gbc);

        return pnlMain;
    }

    // ================== NỬA TRÁI: THÔNG TIN SẢN PHẨM & NÚT SỬA ==================
    private static JPanel taoPanelTrai_ThongTinSP(JDialog dialog, JDialog overlay, SanPham sp, int tonKho, Runnable onCloseCallback) {
        JPanel pnlLeft = new JPanel();
        pnlLeft.setLayout(new BoxLayout(pnlLeft, BoxLayout.Y_AXIS));
        pnlLeft.setOpaque(false);
        pnlLeft.setBorder(new EmptyBorder(0, 10, 10, 10)); 

        // 1. Hình ảnh sản phẩm (Bo viền xịn xò)
        JLabel lblImg = new JLabel("", SwingConstants.CENTER);
        lblImg.setPreferredSize(new Dimension(180, 180));
        lblImg.setMaximumSize(new Dimension(180, 180));
        lblImg.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 2));
        lblImg.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon icon = QuanLyAnh.layIconAnh(sp.getLinkHinhAnh(), 180, 180);
        if (icon != null) {
            lblImg.setIcon(icon);
        } else {
            lblImg.setText("Không có ảnh");
            lblImg.setForeground(Color.GRAY);
        }

        // 2. Tên sản phẩm
        JLabel lblTen = new JLabel("<html><center>" + sp.getTenSP() + "</center></html>", SwingConstants.CENTER);
        lblTen.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTen.setForeground(new Color(15, 23, 42));
        lblTen.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTen.setBorder(new EmptyBorder(15, 0, 10, 0));

        // 3. 🟢 NÚT SỬA SẢN PHẨM (ĐÃ FIX: Bo góc đẹp, không dư ảnh)
        JButton btnSua = new JButton("Sửa Sản Phẩm") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Hiệu ứng đổi màu mượt mà khi hover và click
                if (getModel().isPressed()) {
                    g2.setColor(new Color(10, 88, 202)); // Màu đậm khi click
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(38, 128, 255)); // Màu sáng khi trỏ chuột vào
                } else {
                    g2.setColor(new Color(13, 110, 253)); // Màu xanh chuẩn như hình
                }
                
                // Vẽ khung bo tròn (Bo góc 20px cho giống viên thuốc pill-shape)
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Vẽ chữ thủ công để KHÔNG cần gọi super.paintComponent() -> Khắc phục 100% dư ảnh
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
                
                g2.dispose();
            }
        };
        btnSua.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSua.setForeground(Color.WHITE);
        btnSua.setContentAreaFilled(false); // Xóa viền/nền mặc định của Swing
        btnSua.setBorderPainted(false);     // Xóa đường kẻ viền
        btnSua.setFocusPainted(false);      // XÓA VIỀN NÉT ĐỨT KHI CLICK (Thủ phạm chính)
        btnSua.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSua.setMaximumSize(new Dimension(170, 42));
        btnSua.setPreferredSize(new Dimension(170, 42));
        btnSua.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 🚀 Xử lý sự kiện mở Popup sửa và Callback
        btnSua.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(dialog);
            GUI.HoTro.SuaSanPhamDialog.hienThi(parentWindow, sp, () -> {
                dongPopup(dialog, overlay); // Đóng thẻ chi tiết cũ đi
                if (onCloseCallback != null) {
                    onCloseCallback.run(); // Gọi hàm refresh lại danh sách ở màn hình gốc
                }
            });
        });

        // 4. Bảng thông tin chi tiết
        JPanel pnlInfoGrid = new JPanel(new GridBagLayout());
        pnlInfoGrid.setOpaque(false);
        pnlInfoGrid.setBorder(new EmptyBorder(25, 0, 0, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 15, 0);

        gbc.gridy = 0; pnlInfoGrid.add(taoRowThongTinHienDai("💠", "Mã sản phẩm", sp.getMaSP(), false, new Color(15, 23, 42), false), gbc);
        gbc.gridy = 1; pnlInfoGrid.add(taoRowThongTinHienDai("📁", "Loại sản phẩm", sp.getMaLoai(), false, new Color(15, 23, 42), false), gbc);
        gbc.gridy = 2; pnlInfoGrid.add(taoRowThongTinHienDai("🏷️", "Giá bán", GUI.HoTro.DinhDangUtil.dinhDangTien(sp.getGiaBan()), true, new Color(239, 68, 68), false), gbc);
        gbc.gridy = 3; pnlInfoGrid.add(taoRowThongTinHienDai("📦", "Đơn vị tính", sp.getDonViTinh(), false, new Color(15, 23, 42), false), gbc);

        String txtTonKho = tonKho > 0 ? "Sẵn sàng" : "Hết hàng";
        Color colorTonKho = tonKho > 0 ? new Color(16, 185, 129) : new Color(239, 68, 68);
        gbc.gridy = 4; pnlInfoGrid.add(taoRowThongTinHienDai("✓", "Trạng thái kho", txtTonKho, false, colorTonKho, true), gbc);

        // Ráp nối
        pnlLeft.add(lblImg);
        pnlLeft.add(lblTen);
        pnlLeft.add(btnSua); 
        pnlLeft.add(pnlInfoGrid);
        pnlLeft.add(Box.createVerticalGlue());

        return pnlLeft;
    }

    // ================== HELPER VẼ DÒNG THÔNG TIN CÓ ICON & BADGE ==================
    private static JPanel taoRowThongTinHienDai(String iconStr, String lbl, String val, boolean isValHighlight, Color valColor, boolean isBadge) {
        JPanel pnl = new JPanel(new BorderLayout(15, 0));
        pnl.setOpaque(false);

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTitle.setOpaque(false);
        
        JLabel lblIcon = new JLabel(iconStr);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16)); 
        lblIcon.setForeground(new Color(14, 165, 233));

        JLabel title = new JLabel(lbl);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setForeground(new Color(71, 85, 105)); 

        pnlTitle.add(lblIcon);
        pnlTitle.add(title);

        JComponent compRight;
        if (isBadge) {
            JLabel badge = new JLabel(" " + val + " ", SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(valColor.getRed(), valColor.getGreen(), valColor.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
            badge.setForeground(valColor);
            badge.setBorder(new EmptyBorder(4, 12, 4, 12));
            
            JPanel pnlBadgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            pnlBadgeWrap.setOpaque(false);
            pnlBadgeWrap.add(badge);
            compRight = pnlBadgeWrap;
        } else {
            JLabel value = new JLabel("<html><div style='text-align: right; width: 150px;'>" + val + "</div></html>", SwingConstants.RIGHT);
            value.setFont(new Font("Segoe UI", isValHighlight ? Font.BOLD : Font.PLAIN, isValHighlight ? 18 : 14));
            value.setForeground(valColor);
            compRight = value;
        }

        pnl.add(pnlTitle, BorderLayout.WEST);
        pnl.add(compRight, BorderLayout.EAST);
        pnl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240, 150)));
        
        return pnl;
    }

    // ================== NỬA PHẢI: BẢNG NHẬP KHO ==================
    private static JPanel taoPanelPhai_LichSuNhap(SanPham sp, JButton btnClose,
                                                    List<LichSuNhapRow> lichSuList,
                                                    int tongSLDaNhap, String tenNCCGanNhat) {
        JPanel pnlRight = new JPanel(new BorderLayout(0, 20));
        pnlRight.setOpaque(false);

        // Header có nút X
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        JLabel lblTitle = new JLabel("Lịch Sử Nhập Kho");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 41, 59));
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(btnClose, BorderLayout.EAST);

        // THỐNG KÊ
        JPanel pnlStats = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlStats.setOpaque(false);
        String strTongSL = String.format("%,d %s", tongSLDaNhap, sp.getDonViTinh());
        pnlStats.add(taoCardThongKe("Tổng số lượng đã nhập",  strTongSL,     new Color(14, 165, 233)));
        pnlStats.add(taoCardThongKe("Nhà CC gần nhất",        tenNCCGanNhat, new Color(16, 185, 129)));

        // BẢNG LỊCH SỬ 
        String[] cols = {"Mã Lô", "Ngày Nhập", "Nhà CC", "Số Lượng", "Giá Nhập"};
        DefaultTableModel modelLS = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (lichSuList.isEmpty()) {
            modelLS.addRow(new Object[]{"—", "—", "Chưa có dữ liệu nhập kho", "—", "—"});
        } else {
            for (LichSuNhapRow row : lichSuList) {
                modelLS.addRow(new Object[]{
                        row.maLoHang,
                        row.ngayNhap,
                        row.tenNCC,
                        row.soLuong,
                        GUI.HoTro.DinhDangUtil.dinhDangTien(row.giaNhap)
                });
            }
        }

        JTable tableLS = new JTable(modelLS);
        tableLS.setRowHeight(45);
        tableLS.setShowVerticalLines(false);
        tableLS.setIntercellSpacing(new Dimension(0, 0));
        tableLS.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tableLS.getTableHeader().setBackground(new Color(241, 245, 249));
        tableLS.getTableHeader().setForeground(new Color(71, 85, 105));
        tableLS.getTableHeader().setPreferredSize(new Dimension(0, 40));

        tableLS.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setHorizontalAlignment(col == 2 ? LEFT : CENTER); 
                if (!sel) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    if (col == 3) setForeground(new Color(16, 185, 129));
                    else if (col == 4) setForeground(new Color(245, 158, 11));
                    else setForeground(new Color(30, 41, 59));
                } else {
                    setBackground(new Color(224, 242, 254));
                    setForeground(new Color(15, 23, 42));
                }
                setFont(new Font("Segoe UI", col == 0 || col == 3 || col == 4 ? Font.BOLD : Font.PLAIN, 14));
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        int[] colWidths = {90, 100, 200, 90, 110};
        for (int i = 0; i < colWidths.length; i++) {
            tableLS.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
        }

        JScrollPane scrollLS = new JScrollPane(tableLS);
        GUI.HoTro.TienIchGiaoDien.thietLapThanhCuon(scrollLS);
        scrollLS.getViewport().setBackground(Color.WHITE);
        scrollLS.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        pnlRight.add(pnlHeader, BorderLayout.NORTH);

        JPanel pnlContentRight = new JPanel(new BorderLayout(0, 20));
        pnlContentRight.setOpaque(false);
        pnlContentRight.add(pnlStats,  BorderLayout.NORTH);
        pnlContentRight.add(scrollLS,  BorderLayout.CENTER);

        pnlRight.add(pnlContentRight, BorderLayout.CENTER);

        return pnlRight;
    }

    // ================== HELPER METHODS ==================
    private static JPanel taoCardThongKe(String title, String val, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 5, getHeight(), 5, 5);
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 15));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(new Color(100, 116, 139));
        JLabel lblVal = new JLabel(val);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblVal.setForeground(color);
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblVal,   BorderLayout.CENTER);
        return card;
    }

    private static void dongPopup(JDialog dialog, JDialog overlay) {
        dialog.dispose();
        overlay.dispose();
    }

    private static void chayAnimationFadeIn(JDialog dialog) {
        Timer fadeIn = new Timer(15, new ActionListener() {
            float opacity = 0f;
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += 0.08f;
                if (opacity >= 1f) { opacity = 1f; ((Timer) e.getSource()).stop(); }
                dialog.setOpacity(opacity);
            }
        });
        fadeIn.start();
    }

    private static BufferedImage captureAndBlurScreen(Window parentWindow) {
        if (parentWindow == null || !parentWindow.isShowing()) return null;
        try {
            Robot robot = new Robot();
            Rectangle rect = parentWindow.getBounds();
            BufferedImage screen = robot.createScreenCapture(rect);
            float weight = 1.0f / 49.0f;
            float[] data = new float[49];
            for (int i = 0; i < 49; i++) data[i] = weight;
            Kernel kernel = new Kernel(7, 7, data);
            ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
            return op.filter(screen, null);
        } catch (Exception ex) {
            return null;
        }
    }
}
