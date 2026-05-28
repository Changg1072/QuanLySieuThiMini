package GUI.HoTro;

import Data.KhachHang;
import Dao.TruyVanSieuTocDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 🚀 POPUP LỊCH SỬ MUA HÀNG KHÁCH HÀNG (Chuẩn UI/UX Light Mode)
 * - Đã thêm tính năng TÌM KIẾM THEO MÃ & LỌC THEO TRẠNG THÁI (Realtime)
 */
public class LichSuMuaHangDialog {

    public static void showModal(Component parentComponent, KhachHang kh) {
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
                g2.setColor(new Color(0, 0, 0, 100)); // Nền đen mờ 40%
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        pnlOverlay.setOpaque(false);
        overlay.setContentPane(pnlOverlay);

        // 2. TẠO DIALOG POPUP CHÍNH
        JDialog dialog = new JDialog(parentWindow, "Lịch Sử Mua Hàng", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        // 3. XÂY DỰNG GIAO DIỆN CHÍNH
        JPanel pnlMain = taoMainLayout(dialog, overlay, kh);
        dialog.setContentPane(pnlMain);
        dialog.pack();
        dialog.setLocationRelativeTo(parentWindow);

        pnlOverlay.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dongPopup(dialog, overlay); }
        });
        dialog.getRootPane().registerKeyboardAction(e -> dongPopup(dialog, overlay),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 4. HIỂN THỊ ANIMATION 
        overlay.setVisible(true);
        dialog.setOpacity(0f);
        chayAnimationFadeIn(dialog); 
        dialog.setVisible(true);     
    }

    // =========================================================
    // XÂY DỰNG LAYOUT CHÍNH
    // =========================================================
    private static JPanel taoMainLayout(JDialog dialog, JDialog overlay, KhachHang kh) {
        JPanel pnlMain = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(226, 232, 240)); 
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(20, 25, 20, 25));
        pnlMain.setPreferredSize(new Dimension(850, 650));

        // --- 1. HEADER ---
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)));

        JLabel lblTitle = new JLabel(" Lịch sử mua hàng khách hàng");
        lblTitle.setIcon(new ImageIcon(taoIconLichSu(20, 20))); 
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 41, 59));
        lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnClose.setForeground(new Color(100, 116, 139));
        btnClose.setContentAreaFilled(false); btnClose.setBorderPainted(false); btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dongPopup(dialog, overlay));
        
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(btnClose, BorderLayout.EAST);

        // --- 2. THỐNG KÊ (CARDS) ---
        JPanel pnlStats = new JPanel(new GridLayout(1, 2, 20, 0));
        pnlStats.setOpaque(false);
        
        JLabel lblTongDon = new JLabel("Đang tải...");
        JLabel lblTongGiaTri = new JLabel("Đang tải...");
        
        pnlStats.add(taoCardThongKe("TỔNG SỐ ĐƠN HÀNG", lblTongDon, new Color(15, 23, 42)));
        pnlStats.add(taoCardThongKe("TỔNG GIÁ TRỊ TÍCH LŨY", lblTongGiaTri, new Color(37, 99, 235))); 

        // --- 3. DỮ LIỆU BẢNG (TABLE) ---
        String[] cols = {"NGÀY", "MÃ HÓA ĐƠN", "SỐ LƯỢNG", "GIÁ TRỊ", "TRẠNG THÁI"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        setupTableStyle(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(241, 245, 249)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        TienIchGiaoDien.thietLapThanhCuon(scrollPane);

        // 🚀 CÔNG CỤ LỌC SIÊU TỐC BẰNG TABLEROWSORTER
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // --- 4. THANH TÌM KIẾM VÀ LỌC ---
        JPanel pnlFilter = new JPanel(new BorderLayout());
        pnlFilter.setOpaque(false);
        
        TheBongDo.RoundedTextField txtSearch = new TheBongDo.RoundedTextField("🔍 Tìm kiếm mã hóa đơn...", 20);
        txtSearch.setPreferredSize(new Dimension(300, 40));
        
        JButton btnFilter = new JButton("  Lọc: Tất cả") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btnFilter.setIcon(new ImageIcon(taoIconLoc(14, 14)));
        btnFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnFilter.setContentAreaFilled(false); btnFilter.setBorderPainted(false); btnFilter.setFocusPainted(false);
        btnFilter.setPreferredSize(new Dimension(150, 40)); // Tăng chiều dài nút Lọc một chút
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));

        pnlFilter.add(txtSearch, BorderLayout.WEST);
        pnlFilter.add(btnFilter, BorderLayout.EAST);

        JPanel pnlTopWrap = new JPanel(new BorderLayout(0, 20));
        pnlTopWrap.setOpaque(false);
        pnlTopWrap.add(pnlStats, BorderLayout.NORTH);
        pnlTopWrap.add(pnlFilter, BorderLayout.CENTER);

        // --- 5. FOOTER ---
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JLabel lblRecordCount = new JLabel("Đang tải dữ liệu...");
        lblRecordCount.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblRecordCount.setForeground(new Color(148, 163, 184));
        
        JPanel pnlActionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlActionBtns.setOpaque(false);
        
        JButton btnDong = taoNutBorder("Đóng");
        btnDong.addActionListener(e -> dongPopup(dialog, overlay));
        
        JButton btnExport = taoNutDen("Tải báo cáo (PDF)");
        
        pnlActionBtns.add(btnDong);
        pnlActionBtns.add(btnExport);
        
        pnlFooter.add(lblRecordCount, BorderLayout.WEST);
        pnlFooter.add(pnlActionBtns, BorderLayout.EAST);

        // Lắp ráp Layout
        pnlMain.add(pnlHeader, BorderLayout.NORTH);
        
        JPanel pnlCenterWrap = new JPanel(new BorderLayout(0, 20));
        pnlCenterWrap.setOpaque(false);
        pnlCenterWrap.add(pnlTopWrap, BorderLayout.NORTH);
        pnlCenterWrap.add(scrollPane, BorderLayout.CENTER);
        
        pnlMain.add(pnlCenterWrap, BorderLayout.CENTER);
        pnlMain.add(pnlFooter, BorderLayout.SOUTH);

        // 🚀 CÀI ĐẶT SỰ KIỆN TÌM KIẾM & LỌC (LOGIC TẠI ĐÂY)
        String[] currentStatus = {"Tất cả"}; // Lưu trạng thái đang lọc hiện tại

        Runnable applyFilter = () -> {
            String searchText = txtSearch.getText().trim();
            List<RowFilter<Object, Object>> filters = new ArrayList<>();

            // Điều kiện 1: Tìm kiếm theo mã hóa đơn (Cột số 1)
            if (!searchText.isEmpty() && !searchText.contains("Tìm kiếm")) {
                filters.add(RowFilter.regexFilter("(?i)" + searchText, 1)); 
            }

            // Điều kiện 2: Lọc theo trạng thái Hoàn tất / Trả hàng (Cột số 4)
            if (!currentStatus[0].equals("Tất cả")) {
                filters.add(RowFilter.regexFilter("(?i)" + currentStatus[0], 4));
            }

            if (filters.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.andFilter(filters));
            }
            lblRecordCount.setText("Hiển thị " + sorter.getViewRowCount() + " bản ghi trên tổng số " + model.getRowCount());
        };

        // Lắng nghe thanh tìm kiếm Gõ là lọc
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter.run(); }
            public void removeUpdate(DocumentEvent e) { applyFilter.run(); }
            public void changedUpdate(DocumentEvent e) { applyFilter.run(); }
        });

        // Tạo Popup Menu cho nút Lọc Trạng Thái
        JPopupMenu popupLoc = new JPopupMenu();
        popupLoc.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        
        String[] luaChonLoc = {"Tất cả", "Hoàn tất", "Trả hàng"};
        for (String luaChon : luaChonLoc) {
            JMenuItem item = new JMenuItem("   " + luaChon + "   ");
            item.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            item.setBackground(Color.WHITE);
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            item.addActionListener(e -> {
                currentStatus[0] = luaChon;
                btnFilter.setText("  Lọc: " + luaChon);
                applyFilter.run(); // Cập nhật lại bảng
            });
            popupLoc.add(item);
        }

        // Hiện Popup Lọc khi bấm nút
        btnFilter.addActionListener(e -> popupLoc.show(btnFilter, 0, btnFilter.getHeight()));

        // --- 6. TẢI DỮ LIỆU BẰNG TRUY VẤN SIÊU TỐC ---
        loadDataAsync(kh, model, lblTongDon, lblTongGiaTri, applyFilter);

        return pnlMain;
    }

    // =========================================================
    // BẮN TRUY VẤN SIÊU TỐC Ở BACKGROUND
    // =========================================================
    private static void loadDataAsync(KhachHang kh, DefaultTableModel model, 
                                      JLabel lblTongDon, JLabel lblTongGiaTri, Runnable updateCountCallback) {
        new SwingWorker<Void, Object[]>() {
            int totalOrders = 0;
            BigDecimal totalValue = BigDecimal.ZERO;

            @Override
            protected Void doInBackground() throws Exception {
                // 🚀 GỌI TRỰC TIẾP DAO SIÊU TỐC
                TruyVanSieuTocDAO daoSieuToc = TruyVanSieuTocDAO.getInstance();
                List<TruyVanSieuTocDAO.LichSuMuaHangKhachHangDTO> listHD = daoSieuToc.layLichSuMuaHangKhachHangSieuToc(kh.getSDT());
                
                if (listHD != null && !listHD.isEmpty()) {
                    for (TruyVanSieuTocDAO.LichSuMuaHangKhachHangDTO dto : listHD) {
                        totalOrders++;
                        
                        // Tính tổng giá trị (Chỉ cộng các đơn HOÀN TẤT)
                        if ("HOÀN TẤT".equals(dto.trangThai) && dto.giaTri != null) {
                            totalValue = totalValue.add(dto.giaTri);
                        }

                        // Đổi chữ HOÀN TRẢ của DB cũ thành TRẢ HÀNG cho đồng bộ hiển thị
                        String trangThaiHienThi = "HOÀN TẤT".equals(dto.trangThai) ? "HOÀN TẤT" : "TRẢ HÀNG";

                        // Đẩy từng dòng lên giao diện
                        publish(new Object[]{
                                dto.ngayTao,
                                dto.maHD,
                                String.format("%02d mặt hàng", dto.soLuongMatHang),
                                dto.giaTri,
                                trangThaiHienThi
                        });
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    model.addRow(row);
                }
            }

            @Override
            protected void done() {
                lblTongDon.setText(totalOrders + " đơn");
                lblTongGiaTri.setText(DinhDangUtil.dinhDangTien(totalValue));
                updateCountCallback.run(); // Kích hoạt đếm lại dòng sau khi nạp xong
            }
        }.execute();
    }

    // =========================================================
    // STYLE HELPERS VÀ RENDERER
    // =========================================================
    
    private static void setupTableStyle(JTable table) {
        table.setRowHeight(45);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(Color.WHITE);
        header.setForeground(new Color(148, 163, 184)); 
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                
                setHorizontalAlignment(col == 2 || col == 3 ? RIGHT : (col == 4 ? CENTER : LEFT));
                setBorder(new EmptyBorder(0, 15, 0, 15));
                
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 250, 252));
                } else {
                    setBackground(new Color(239, 246, 255));
                }

                setFont(new Font("Segoe UI", Font.BOLD, 13));

                if (col == 0) { 
                    setForeground(new Color(100, 116, 139));
                } else if (col == 1) { 
                    setForeground(new Color(37, 99, 235));
                } else if (col == 2) { 
                    setForeground(new Color(71, 85, 105));
                    setFont(new Font("Segoe UI", Font.PLAIN, 13));
                } else if (col == 3) { 
                    setForeground(new Color(15, 23, 42));
                    if (value instanceof BigDecimal) {
                        setText(DinhDangUtil.dinhDangTien((BigDecimal) value));
                    }
                }

                if (col == 4) {
                    String status = value.toString().toUpperCase();
                    JLabel badge = new JLabel(status, SwingConstants.CENTER) {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            if (status.equals("HOÀN TẤT")) {
                                g2.setColor(new Color(209, 250, 229)); 
                            } else {
                                g2.setColor(new Color(254, 243, 199)); 
                            }
                            g2.fillRoundRect(0, 2, getWidth(), getHeight()-4, 8, 8);
                            super.paintComponent(g);
                            g2.dispose();
                        }
                    };
                    badge.setOpaque(false);
                    badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    if (status.equals("HOÀN TẤT")) {
                        badge.setForeground(new Color(16, 185, 129)); 
                    } else {
                        badge.setForeground(new Color(245, 158, 11)); // Màu cam cho Trả hàng
                    }
                    JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
                    wrap.setOpaque(false); wrap.setBackground(getBackground());
                    badge.setPreferredSize(new Dimension(80, 28));
                    wrap.add(badge);
                    return wrap;
                }

                return this;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    private static JPanel taoCardThongKe(String title, JLabel lblValue, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(5, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(226, 232, 240)); 
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(100, 116, 139));

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(valueColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    private static JButton taoNutDen(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(0, 0, 0) : new Color(15, 23, 42));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g); g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(140, 36)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static JButton taoNutBorder(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                super.paintComponent(g); g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(new Color(71, 85, 105));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(80, 36)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // =========================================================
    // UTILS & ICONS
    // =========================================================
    private static void dongPopup(JDialog dialog, JDialog overlay) {
        dialog.dispose();
        overlay.dispose();
    }

    private static void chayAnimationFadeIn(JDialog dialog) {
        Timer fadeIn = new Timer(10, new ActionListener() {
            float opacity = 0f;
            @Override public void actionPerformed(ActionEvent e) {
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
            float weight = 1.0f / 49.0f; float[] data = new float[49];
            for (int i = 0; i < 49; i++) data[i] = weight;
            Kernel kernel = new Kernel(7, 7, data);
            ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
            return op.filter(screen, null);
        } catch (Exception ex) { return null; }
    }

    private static Image taoIconLichSu(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(37, 99, 235));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(2, 2, w-4, h-4, 45, 270);
        g2.fillPolygon(new int[]{w/2 + 2, w/2 + 8, w/2 + 2}, new int[]{0, 4, 8}, 3);
        g2.drawLine(w/2, h/2, w/2-3, h/2-3);
        g2.dispose();
        return img;
    }

    private static Image taoIconLoc(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(100, 116, 139));
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(0, 2, w, 2);
        g2.drawLine(2, 7, w-2, 7);
        g2.drawLine(4, 12, w-4, 12);
        g2.dispose();
        return img;
    }
}