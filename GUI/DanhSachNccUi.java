package GUI;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import Data.NhaCungCap;
import GUI.HoTro.DinhDangUtil;
import GUI.HoTro.ONhapLieuHienDai;
import GUI.HoTro.TienIchGiaoDien;
import Logic.NhaCungCapLogic;

public class DanhSachNccUi extends JPanel {

    // ================= MÀU SẮC CHỦ ĐẠO =================
    private static final Color BG_MAIN = new Color(245, 247, 250);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color HOVER_COLOR = new Color(238, 242, 246);
    private static final Color SELECTED_COLOR = new Color(239, 246, 255);
    private static final Color SELECT_BORDER = new Color(59, 130, 246);

    // ================= LOGIC & DỮ LIỆU =================
    private NhaCungCapLogic nccLogic = new NhaCungCapLogic();
    private List<NhaCungCap> danhSachGoc = new ArrayList<>();
    private List<NhaCungCap> currentDisplayedList = new ArrayList<>(); // Danh sách đang hiển thị trên màn hình
    private List<NhaCungCap> selectedNCCs = new ArrayList<>();
    private Logic.LoHangLogic lhLogic = new Logic.LoHangLogic();
    private boolean isLoading = false;

    // ================= UI COMPONENTS =================
    private ONhapLieuHienDai txtTimKiem;
    private JPanel pnlRowListContainer;
    private JLabel lblTongNCC;
    
    // Thêm các biến để quản lý UX mới
    private JButton btnXoa; 
    private ModernCheckBox cbSelectAll; 

    public DanhSachNccUi() {
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(20, 25, 20, 25));

        initUI();
        loadDataSieuToc();
        setupListeners();
        this.setFocusable(true); 
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                requestFocusInWindow(); // Ép nền cướp lấy focus
            }
        });
    }

    // =========================================================
    // 1. KIẾN TRÚC UI CHÍNH
    // =========================================================
    private void initUI() {
        JPanel pnlNorth = new JPanel(new BorderLayout(20, 0));
        pnlNorth.setOpaque(false);

        txtTimKiem = new ONhapLieuHienDai("", true, false);
        txtTimKiem.setPlaceholder("🔍 Tìm mã NCC, tên, SĐT, Email...");
        txtTimKiem.setPreferredSize(new Dimension(500, 65)); 
        pnlNorth.add(txtTimKiem, BorderLayout.WEST);

        JPanel pnlCenter = new JPanel(new BorderLayout(0, 10));
        pnlCenter.setOpaque(false);
        pnlCenter.add(createFakeHeader(), BorderLayout.NORTH);

        pnlRowListContainer = new JPanel();
        pnlRowListContainer.setLayout(new BoxLayout(pnlRowListContainer, BoxLayout.Y_AXIS));
        pnlRowListContainer.setBackground(BG_MAIN);

        JScrollPane scrollPane = new JScrollPane(pnlRowListContainer);
        TienIchGiaoDien.thietLapThanhCuon(scrollPane);
        scrollPane.getViewport().setBackground(BG_MAIN);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        pnlCenter.add(scrollPane, BorderLayout.CENTER);

        JPanel pnlEast = createStatsPanel();
        JPanel pnlSouth = createActionButtons();

        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlEast, BorderLayout.EAST);
        add(pnlSouth, BorderLayout.SOUTH);
    }

    private JPanel createFakeHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.setBackground(new Color(241, 245, 249));
        header.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 45));

        cbSelectAll = new ModernCheckBox();
        cbSelectAll.setPreferredSize(new Dimension(40, 20));
        cbSelectAll.addActionListener(e -> handleSelectAll(cbSelectAll.isSelected()));
        header.add(cbSelectAll);       
        
        // 🔥 ĐÃ DỒN CỘT: Thu nhỏ các cột cũ để chừa chỗ cho cột Chi tiết
        header.add(createHeaderLabel("Mã NCC", 80));            
        header.add(createHeaderLabel("Tên Nhà Cung Cấp", 350)); // Giảm từ 420 -> 330
        header.add(createHeaderLabel("SĐT", 110));              // Giảm từ 140 -> 110
        header.add(createHeaderLabel("Email", 220));            // Giảm từ 250 -> 200
        header.add(createHeaderLabel("Địa chỉ", 130));          
        header.add(createHeaderLabel("Trạng thái", 160));       
        header.add(createHeaderLabel("Chi tiết", 100));         // Cột mới thêm

        return header;
    }
    
    private JLabel createHeaderLabel(String text, int width) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
        lbl.setForeground(new Color(30, 41, 59)); 
        lbl.setPreferredSize(new Dimension(width, 20));
        return lbl;
    }

    // =========================================================
    // 3. THỐNG KÊ (EAST)
    // =========================================================
    private JPanel createStatsPanel() {
        JPanel pnl = new JPanel(new BorderLayout(0, 15));
        pnl.setOpaque(false);
        pnl.setPreferredSize(new Dimension(250, 0));

        JPanel pnlCards = new JPanel(new GridLayout(2, 1, 0, 15));
        pnlCards.setOpaque(false);

        lblTongNCC = createStatCard(pnlCards, "Tổng Nhà Cung Cấp", "0", TienIchGiaoDien.MAU_CHINH);
        
        JPanel pnlEmpty = new JPanel();
        pnlEmpty.setOpaque(false);
        pnlCards.add(pnlEmpty);

        pnl.add(pnlCards, BorderLayout.NORTH);
        return pnl;
    }

    private JLabel createStatCard(JPanel parent, String title, String val, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(color); g2.fillRoundRect(0, 0, 5, getHeight(), 5, 5);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(15, 20, 15, 15));
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12.5f)); lblTitle.setForeground(new Color(100, 116, 139));
        JLabel lblVal = new JLabel(val); lblVal.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(26f)); lblVal.setForeground(color);
        card.add(lblTitle, BorderLayout.NORTH); card.add(lblVal, BorderLayout.CENTER); parent.add(card); return lblVal;
    }

    // =========================================================
    // 4. ACTION BUTTONS (SOUTH)
    // =========================================================
    private JPanel createActionButtons() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);

        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlLeft.setOpaque(false);
        
        JButton btnReload = TienIchGiaoDien.taoNutHienDai("Làm mới ↻", new Color(100, 116, 139));
        btnReload.addActionListener(e -> handleReload());
        pnlLeft.add(btnReload);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlRight.setOpaque(false);

        // 🔥 NÚT XÓA: Khởi tạo nhưng ẨN đi mặc định
        btnXoa = TienIchGiaoDien.taoNutHienDai("❌ Xóa", new Color(239, 68, 68));
        btnXoa.addActionListener(e -> handleXoa());
        btnXoa.setVisible(false); // Ẩn khi không có ai được tick

        JButton btnThem = TienIchGiaoDien.taoNutHienDai("+ Thêm mới", TienIchGiaoDien.MAU_CHINH);
        btnThem.addActionListener(e -> handleThem());

        pnlRight.add(btnXoa);
        pnlRight.add(Box.createHorizontalStrut(10));
        pnlRight.add(btnThem);

        pnl.add(pnlLeft, BorderLayout.WEST);
        pnl.add(pnlRight, BorderLayout.EAST);

        return pnl;
    }

    // =========================================================
    // 5. RENDER DANH SÁCH (ROW PANEL SYSTEM)
    // =========================================================
    private void renderList(List<NhaCungCap> data) {
        pnlRowListContainer.removeAll();

        if (isLoading) {
            for (int i = 0; i < 6; i++) {
                pnlRowListContainer.add(createSkeletonRow());
                pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        } else {
            for (NhaCungCap ncc : data) {
                pnlRowListContainer.add(createRowPanel(ncc));
                pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        pnlRowListContainer.revalidate();
        pnlRowListContainer.repaint();
    }

    private JPanel createRowPanel(Data.NhaCungCap ncc) {
        boolean isNgungHopTac = "Ngừng hợp tác".equalsIgnoreCase(ncc.getTrangThai());

        JPanel row = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isSelected = selectedNCCs.contains(ncc);

                if (isSelected) g2.setColor(SELECTED_COLOR);
                else            g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                if (isSelected) {
                    g2.setColor(SELECT_BORDER);
                    g2.fillRoundRect(0, 0, 6, getHeight(), 15, 15);
                    g2.setColor(SELECT_BORDER);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                } else {
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                }

                // Xóa mềm: Phủ sương xám lên dòng bị vô hiệu hóa
                if (isNgungHopTac) {
                    g2.setColor(new Color(255, 255, 255, 180));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 60)); 
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        content.setOpaque(false);

        ModernCheckBox cbSelect = new ModernCheckBox();
        cbSelect.setPreferredSize(new Dimension(40, 25)); 
        cbSelect.setSelected(selectedNCCs.contains(ncc));
        cbSelect.addActionListener(e -> handleSelection(ncc, cbSelect.isSelected(), row));
        content.add(cbSelect);

        // 🔥 Đã căn chỉnh Width thu gọn lại để chừa 100px cho cột Chi tiết
        content.add(createCell(ncc.getMaNCC(), 80, true));
        content.add(createCell(ncc.getTenNCC() != null ? ncc.getTenNCC() : "—", 350, true));
        content.add(createCell(ncc.getSDT() != null ? ncc.getSDT() : "—", 110, false));
        content.add(createCell(ncc.getEmail() != null ? ncc.getEmail() : "—", 220, false));
        
        JLabel lblAddress = createCell(ncc.getDiaChi() != null ? ncc.getDiaChi() : "—", 130, false);
        lblAddress.setToolTipText(ncc.getDiaChi()); // Rê chuột vào vẫn hiện full địa chỉ siêu dài
        content.add(lblAddress);
        
        // 🔥 HIỆU ỨNG BADGE TRẠNG THÁI (Kích thước 110px)
        JPanel pnlBadge = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Nền
                if (isNgungHopTac) g2.setColor(new Color(254, 226, 226)); // Đỏ nhạt
                else g2.setColor(new Color(209, 250, 229)); // Xanh nhạt
                
                g2.fillRoundRect(0, 0, getWidth(), 25, 25, 25);
                
                // Chấm tròn
                if (isNgungHopTac) g2.setColor(new Color(220, 38, 38)); 
                else g2.setColor(new Color(16, 185, 129)); 
                
                g2.fillOval(10, 7, 10, 10); 
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlBadge.setOpaque(false);
        pnlBadge.setPreferredSize(new Dimension(140, 25)); 
        
        JLabel lblStatus = new JLabel(isNgungHopTac ? "Ngừng hợp tác" : "Đang hợp tác");
        lblStatus.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12f));
        lblStatus.setBorder(new EmptyBorder(0, 28, 0, 0)); 
        
        if (isNgungHopTac) lblStatus.setForeground(new Color(153, 27, 27)); 
        else lblStatus.setForeground(new Color(4, 120, 87)); 
        
        pnlBadge.add(lblStatus, BorderLayout.CENTER);
        content.add(pnlBadge);
        
     // 🔥 NÚT CHI TIẾT
        JButton btnDetail = new JButton("📄 Chi tiết");
        
        // Khởi tạo phông 14f giống hệt bên Nhập hàng
        btnDetail.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f));
        btnDetail.setForeground(new Color(59, 130, 246));
        btnDetail.setContentAreaFilled(false); 
        btnDetail.setBorderPainted(false); 
        btnDetail.setFocusPainted(false);
        btnDetail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // ❌ BÍ QUYẾT: Đã XÓA dòng setPreferredSize(new Dimension(100, 25)) ở đây
        // Để nút được tự do co giãn chiều cao khi hover!
        
        btnDetail.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override 
            public void mouseEntered(java.awt.event.MouseEvent e) {
                // Đổi màu Đỏ
                btnDetail.setForeground(new Color(239, 68, 68)); 
                
                // Phông 14f + In đậm
                Font hoverFont = TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 14f);
                
                // Gạch chân (Yếu tố làm nút cao lên để đẩy cả dòng lệch xuống)
                java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>(hoverFont.getAttributes());
                attributes.put(java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON);
                btnDetail.setFont(hoverFont.deriveFont(attributes));
            }
            
            @Override 
            public void mouseExited(java.awt.event.MouseEvent e) {
                // Trả về xanh lam & phông 13f
                btnDetail.setForeground(new Color(59, 130, 246)); 
                btnDetail.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(13f)); 
            }
        });

        btnDetail.addActionListener(e -> handleChiTiet(ncc));
        content.add(btnDetail);

        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!selectedNCCs.contains(ncc)) { row.setBackground(HOVER_COLOR); row.repaint(); }
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  {
                row.setBackground(Color.WHITE); row.repaint();
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                cbSelect.doClick(); 
            }
        });

        row.add(content, BorderLayout.CENTER);
        return row;
    }
    private JLabel createCell(String text, int width, boolean isBold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(isBold ? TienIchGiaoDien.FONT_DAM.deriveFont(16f) : TienIchGiaoDien.FONT_CHINH.deriveFont(15f));
        lbl.setForeground(isBold ? new Color(17, 24, 39) : new Color(55, 65, 81));
        lbl.setPreferredSize(new Dimension(width, 25));
        return lbl;
    }

    private JPanel createSkeletonRow() {
        JPanel skeleton = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
            }
        };
        skeleton.setOpaque(false); skeleton.setPreferredSize(new Dimension(0, 60)); skeleton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); return skeleton;
    }

    // =========================================================
    // 6. LOGIC & DATA (HỖ TRỢ UX MỚI)
    // =========================================================
    private void loadDataSieuToc() {
        isLoading = true;
        renderList(new ArrayList<>());

        SwingWorker<List<NhaCungCap>, Void> worker = new SwingWorker<>() {
            @Override protected List<NhaCungCap> doInBackground() {
                try { Thread.sleep(200); } catch (Exception ignored) {} 
                return nccLogic.layDanhSachNhaCungCap();
            }
            @Override protected void done() {
                try {
                    danhSachGoc = get();
                    isLoading = false;
                    updateStats();
                    selectedNCCs.clear(); // Reset tick
                    filterData(); 
                } catch (Exception e) { e.printStackTrace(); isLoading = false; }
            }
        };
        worker.execute();
    }
    
    private void filterData() {
        if (isLoading) return;

        String key = DinhDangUtil.loaiBoDauTiengViet(txtTimKiem.getText().toLowerCase().trim());

        currentDisplayedList = danhSachGoc.stream().filter(ncc -> {
            boolean matchKey = true;
            if (!key.isEmpty()) {
                String ten = ncc.getTenNCC() != null ? DinhDangUtil.loaiBoDauTiengViet(ncc.getTenNCC().toLowerCase()) : "";
                String ma = ncc.getMaNCC() != null ? ncc.getMaNCC().toLowerCase() : "";
                String sdt = ncc.getSDT() != null ? ncc.getSDT() : "";
                String mail = ncc.getEmail() != null ? ncc.getEmail().toLowerCase() : "";
                matchKey = ten.contains(key) || ma.contains(key) || sdt.contains(key) || mail.contains(key);
            }
            return matchKey;
        }).collect(Collectors.toList());

        renderList(currentDisplayedList);
        updateSelectionUIState(); // Cập nhật lại nút Xóa và Chọn tất cả
    }

    private void updateStats() {
        lblTongNCC.setText(String.valueOf(danhSachGoc.size()));
    }

    // =========================================================
    // 7. HANDLERS & UX LOGIC
    // =========================================================
    private void setupListeners() {
        txtTimKiem.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterData(); }
            public void removeUpdate(DocumentEvent e) { filterData(); }
            public void changedUpdate(DocumentEvent e) { filterData(); }
        });
    }

    // Xử lý khi tick/untick từng hàng
    private void handleSelection(NhaCungCap ncc, boolean isSelected, JPanel rowPanel) {
        if (isSelected) {
            if (!selectedNCCs.contains(ncc)) selectedNCCs.add(ncc);
        } else {
            selectedNCCs.remove(ncc);
        }
        rowPanel.repaint();
        updateSelectionUIState();
    }

    // Xử lý khi bấm nút "Chọn tất cả" trên Header
    private void handleSelectAll(boolean isSelected) {
        selectedNCCs.clear();
        if (isSelected) {
            // Chỉ chọn những dòng đang hiển thị trên màn hình (đã qua tìm kiếm)
            selectedNCCs.addAll(currentDisplayedList);
        }
        renderList(currentDisplayedList); // Render lại để cập nhật màu hàng loạt
        updateSelectionUIState();
    }

    // Tự động kiểm tra và ẩn/hiện Nút Xóa + Ô Chọn Tất Cả
    private void updateSelectionUIState() {
        // 1. Ẩn hiện nút xóa
        btnXoa.setVisible(!selectedNCCs.isEmpty());

        // 2. Đồng bộ trạng thái ô Chọn Tất Cả
        if (currentDisplayedList.isEmpty()) {
            cbSelectAll.setSelected(false);
        } else {
            boolean allSelected = currentDisplayedList.stream().allMatch(ncc -> selectedNCCs.contains(ncc));
            cbSelectAll.setSelected(allSelected);
        }
        cbSelectAll.repaint();
    }
    private void handleChiTiet(Data.NhaCungCap ncc) {
        // 1. Lọc danh sách lô hàng của NCC này
        List<Data.LoHang> shipments = lhLogic.layDanhSachLoHang().stream()
                .filter(lh -> lh.getMaNCC().equals(ncc.getMaNCC()))
                .collect(java.util.stream.Collectors.toList());

        java.math.BigDecimal tongTien = java.math.BigDecimal.ZERO;
        
        StringBuilder sb = new StringBuilder("<html><body style='width: 320px; font-family: sans-serif; font-size: 13px;'>");
        sb.append("<h2 style='color: #1E40AF; text-align: center; margin-top: 5px;'>Báo cáo cung ứng: " + ncc.getTenNCC() + "</h2>");
        sb.append("<p style='font-size: 14px;'><b>Tổng số lô hàng:</b> <span style='color: #10B981; font-size: 14px;'>" + shipments.size() + " lô</span></p>");
        sb.append("<hr><table style='width: 100%; font-size: 14px;'>");
        
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (Data.LoHang lh : shipments) {
            String ngay = lh.getNgayNhapKho().format(fmt);
            String tien = GUI.HoTro.DinhDangUtil.dinhDangTien(lh.getThanhTien());
            tongTien = tongTien.add(lh.getThanhTien());
            
            sb.append("<tr>")
              .append("<td style='padding: 4px 0;'><b>" + lh.getMaLoHang() + "</b> (" + ngay + ")</td>")
              .append("<td style='text-align: right; color: #16A34A;'>: " + tien + "</td>")
              .append("</tr>");
        }
        
        sb.append("</table><hr>");
        sb.append("<h3 style='text-align: right; color: #EA580C; font-size: 15px;'>TỔNG TIỀN: " + GUI.HoTro.DinhDangUtil.dinhDangTien(tongTien) + "</h3>");
        sb.append("</body></html>");

        // =========================================================
        // 🔥 TẠO POPUP CUSTOM VỚI HIỆU ỨNG KÍNH MỜ (CHỤP MÀN HÌNH BẰNG ROBOT)
        // =========================================================
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        
        // 1. Chụp và làm mờ màn hình hiện tại
        java.awt.image.BufferedImage blurredBg = null;
        if (parentWindow != null && parentWindow.isShowing()) {
            try {
                java.awt.Robot robot = new java.awt.Robot();
                Rectangle rect = parentWindow.getBounds();
                java.awt.image.BufferedImage screen = robot.createScreenCapture(rect);
                
                // Thuật toán làm mờ y hệt ThemNhaCungCapDialog
                float weight = 1.0f / 25.0f;
                float[] data = new float[25];
                for (int i = 0; i < 25; i++) data[i] = weight;
                java.awt.image.Kernel kernel = new java.awt.image.Kernel(5, 5, data);
                java.awt.image.ConvolveOp op = new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);
                blurredBg = op.filter(screen, null);
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        final java.awt.image.BufferedImage finalBlur = blurredBg;

        // 2. Tạo lớp phủ mờ (Dark Overlay)
        JDialog overlay = new JDialog(parentWindow, Dialog.ModalityType.MODELESS);
        overlay.setUndecorated(true);
        if (parentWindow != null) overlay.setBounds(parentWindow.getBounds());
        overlay.setBackground(new Color(0, 0, 0, 0)); 

        JPanel pnlOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                // Vẽ ảnh nền đã làm mờ
                if (finalBlur != null) {
                    g2.drawImage(finalBlur, 0, 0, getWidth(), getHeight(), null);
                }
                // Phủ màu tối y hệt file ThemNhaCungCapDialog
                g2.setColor(new Color(15, 23, 42, 190)); 
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        pnlOverlay.setOpaque(false);
        overlay.setContentPane(pnlOverlay);

        // 3. Tạo Hộp thoại Chi tiết
        JDialog dialog = new JDialog(parentWindow, "Chi tiết", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true); 
        dialog.setBackground(new Color(0, 0, 0, 0)); 
        
        JPanel pnlMain = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                g2.setColor(new Color(59, 130, 246));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
                g2.dispose();
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel lblTitle = new JLabel("CHI TIẾT", SwingConstants.CENTER);
        lblTitle.setFont(GUI.HoTro.TienIchGiaoDien.FONT_DAM.deriveFont(18f));
        lblTitle.setForeground(new Color(59, 130, 246));
        pnlMain.add(lblTitle, BorderLayout.NORTH);

        JLabel lblContent = new JLabel(sb.toString());
        lblContent.setBorder(new EmptyBorder(10, 0, 15, 0));
        pnlMain.add(lblContent, BorderLayout.CENTER);

        JButton btnClose = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("Đóng", new Color(59, 130, 246));
        btnClose.setPreferredSize(new Dimension(120, 40));
        btnClose.addActionListener(e -> {
            dialog.dispose();  
            overlay.dispose(); 
        });
        JPanel pnlBot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlBot.setOpaque(false);
        pnlBot.add(btnClose);
        pnlMain.add(pnlBot, BorderLayout.SOUTH);

        dialog.add(pnlMain);
        dialog.pack();
        dialog.setLocationRelativeTo(parentWindow);

        // Hiển thị
        overlay.setVisible(true);
        dialog.setVisible(true); 
    }
    private void handleReload() {
        txtTimKiem.clear();
        loadDataSieuToc();
    }

    private void handleThem() {
        // Lấy cửa sổ cha để làm mờ nền
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        GUI.HoTro.ThemNhaCungCapDialog dialog = new GUI.HoTro.ThemNhaCungCapDialog(parentWindow);
        dialog.setVisible(true);

        if (dialog.isSuccess()) {
            try {
                // Lấy data từ form
                Data.NhaCungCap nccMoi = dialog.getNhaCungCapMoi();
                
                // 🔥 ĐÃ SỬA: Dùng Tầng Logic để ném qua bộ lọc kiểm tra lỗi, trùng lặp
                nccLogic.themNhaCungCap(nccMoi);
                
                // Nếu vượt qua Logic mà không ném lỗi -> Thành công
                GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, "Thêm nhà cung cấp thành công!", "SUCCESS");
                loadDataSieuToc(); // Load lại bảng
                
            } catch (Exception e) { 
                // Hứng trọn bộ thông báo lỗi từ file Logic (Trùng tên, sai SĐT...) để báo cho người dùng
                GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, e.getMessage(), "ERROR"); 
            }
        }
    }	
    private void handleXoa() {
        if (selectedNCCs.isEmpty()) return;
        
        TienIchGiaoDien.hienThiXacNhan(this, "Chuyển " + selectedNCCs.size() + " nhà cung cấp vào danh sách NGỪNG HỢP TÁC?", () -> {
            try {
                for (NhaCungCap ncc : selectedNCCs) {
                    nccLogic.xoaNhaCungCap(ncc.getMaNCC());
                }
                TienIchGiaoDien.hienThiThongBao(this, "Đã cập nhật trạng thái ngừng hợp tác!", "SUCCESS");
                loadDataSieuToc(); // Load lại để thấy phủ sương mờ
            } catch (Exception e) {
                TienIchGiaoDien.hienThiThongBao(this, "Lỗi: " + e.getMessage(), "ERROR");
            }
        });
    }

    // =========================================================
    // 🎨 COMPONENT: MODERN CHECKBOX (O TICK CHUẨN GMAIL)
    // =========================================================
    private class ModernCheckBox extends JCheckBox {
        public ModernCheckBox() {
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int size = 18; // Kích thước ô tick
            int y = (getHeight() - size) / 2;
            int x = (getWidth() - size) / 2; // Căn giữa nếu cần

            if (isSelected()) {
                // Nền xanh khi tick
                g2.setColor(TienIchGiaoDien.MAU_CHINH); 
                g2.fillRoundRect(x, y, size, size, 5, 5); // Bo góc vuông
                
                // Vẽ dấu Tick trắng (Dùng line chéo)
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 9, x + 8, y + 13); // Nét ngắn
                g2.drawLine(x + 8, y + 13, x + 14, y + 5); // Nét dài
            } else {
                // Nền trắng viền xám khi bỏ tick
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(x, y, size, size, 5, 5);
                g2.setColor(new Color(180, 185, 195));
                g2.drawRoundRect(x, y, size, size, 5, 5);
            }
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        // 🔥 XÓA HOẶC COMMENT DÒNG NÀY ĐI ĐỂ TẮT THEME WINDOWS:
        // try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Quản Lý Nhà Cung Cấp");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1200, 700);
            f.setLocationRelativeTo(null);
            f.add(new DanhSachNccUi());
            f.setVisible(true);
        });
    }
}