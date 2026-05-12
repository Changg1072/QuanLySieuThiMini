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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import Data.LoHang;
import GUI.HoTro.DinhDangUtil;
import GUI.HoTro.ONhapLieuHienDai;
import GUI.HoTro.TienIchGiaoDien;
import Logic.ChiTietLoHangLogic; // Thêm Logic Chi tiết lô hàng để tính tổng SP
import Logic.LoHangLogic;
import Logic.NhaCungCapLogic;

public class DanhSachNhapHangUi extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color BG_MAIN = new Color(245, 247, 250);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color HOVER_COLOR = new Color(238, 242, 246);
    private static final Color SELECTED_COLOR = new Color(239, 246, 255);
    private static final Color SELECT_BORDER = new Color(59, 130, 246);

    // ================= LOGIC & DỮ LIỆU =================
    private LoHangLogic lhLogic = new LoHangLogic();
    private NhaCungCapLogic nccLogic = new NhaCungCapLogic(); 
    private ChiTietLoHangLogic ctLogic = new ChiTietLoHangLogic(); // Động cơ tính Tổng số lượng
    private Logic.SanPhamLogic spLogic = new Logic.SanPhamLogic();

    private List<LoHangViewModel> danhSachGoc = new ArrayList<>();
    private List<LoHangViewModel> currentDisplayedList = new ArrayList<>();
    private List<LoHangViewModel> selectedPhieu = new ArrayList<>();

    // ================= UI COMPONENTS =================
    private ONhapLieuHienDai txtTimKiem;
    private JComboBox<String> cbSapXep;
    private JPanel pnlRowListContainer;
    private JLabel lblTongDon, lblTongSoLuong, lblTongTien; // Thêm label đếm Số lượng
    private ModernCheckBox cbSelectAll;
    private JButton btnXoa;
    private boolean isLoading = false;
    private Dao.TruyVanSieuTocDAO.DuLieuNhapHangDTO cacheData;

    public DanhSachNhapHangUi() {
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(20, 25, 20, 25));

        initUI();
        loadDataSieuToc(); 
        setupListeners();
        this.setFocusable(true); 
        // Bắt sự kiện click chuột lên nền trống
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Ép Panel nền cướp tiêu điểm (focus) từ ô Text
                requestFocusInWindow(); 
            }
        });
    }

    private void initUI() {
        JPanel pnlNorth = new JPanel(new BorderLayout(20, 0));
        pnlNorth.setOpaque(false);
        
        // --- Cụm Tìm kiếm & Sắp xếp bên trái ---
        JPanel pnlLeftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLeftSearch.setOpaque(false);

        // 1. Ô Tìm kiếm (Kích thước chuẩn 65px chiều cao)
        txtTimKiem = new ONhapLieuHienDai("", true, false);
        txtTimKiem.setPlaceholder("🔍 Tìm mã lô, tên nhà cung cấp...");
        txtTimKiem.setPreferredSize(new Dimension(450, 65)); 
        
        // 2. Menu Sắp xếp
        JPanel pnlSortWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12)); 
        pnlSortWrap.setOpaque(false);
        pnlSortWrap.setBorder(new EmptyBorder(0, 60, 0, 0)); // Cách ô tìm kiếm 60px
        
        JLabel lblSort = new JLabel("Sắp xếp theo:");
        lblSort.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14.5f));
        lblSort.setForeground(TienIchGiaoDien.MAU_CHU_PHU);
        
        cbSapXep = new JComboBox<>(new String[]{
            "Mặc định (Mã lô)", "Ngày nhập: Mới nhất", "Ngày nhập: Cũ nhất", "Tổng tiền: Tăng dần", "Tổng tiền: Giảm dần"
        });
        TienIchGiaoDien.trangTriComboBox(cbSapXep);
        cbSapXep.setPreferredSize(new Dimension(220, 42));
        cbSapXep.setFocusable(false); // Tắt bôi đen khi click
        
        pnlSortWrap.add(lblSort);
        pnlSortWrap.add(cbSapXep);

        pnlLeftSearch.add(txtTimKiem);
        pnlLeftSearch.add(pnlSortWrap);
        
        pnlNorth.add(pnlLeftSearch, BorderLayout.WEST);

        // --- Các phần Center (Header giả & Danh sách) ---
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

        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(createStatsPanel(), BorderLayout.EAST);
        add(createActionButtons(), BorderLayout.SOUTH);
    }

    // =========================================================
    // HEADER & ROW PANEL (🔥 ĐÃ GIÃN CỘT RỘNG MÊNH MÔNG)
    // =========================================================
    private JPanel createFakeHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.setBackground(new Color(241, 245, 249));
        header.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 45));

        cbSelectAll = new ModernCheckBox();
        cbSelectAll.setPreferredSize(new Dimension(40, 20));
        cbSelectAll.addActionListener(e -> handleSelectAll(cbSelectAll.isSelected()));
        
        header.add(cbSelectAll);
        
        // 🔥 GIÃN CỘT: Tăng thêm ~300px tổng cộng để tràn sang phải
        header.add(createHeaderLabel("Mã Lô", 140));            // Tăng 20px
        header.add(createHeaderLabel("Tên Nhà Cung Cấp", 500)); // Tăng 130px
        header.add(createHeaderLabel("Ngày Nhập", 200));        // Tăng 30px
        header.add(createHeaderLabel("Tổng Tiền", 130));        // Tăng 70px

        return header;
    }

    private JLabel createHeaderLabel(String text, int width) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
        lbl.setForeground(new Color(30, 41, 59)); 
        lbl.setPreferredSize(new Dimension(width, 20));
        return lbl;
    }

    private void renderList(List<LoHangViewModel> data) {
        pnlRowListContainer.removeAll();
        if (isLoading) {
            for (int i = 0; i < 5; i++) {
                pnlRowListContainer.add(createSkeletonRow());
                pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        } else {
            for (LoHangViewModel phieu : data) {
                pnlRowListContainer.add(createRowPanel(phieu));
                pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }
        pnlRowListContainer.revalidate();
        pnlRowListContainer.repaint();
    }

    private JPanel createRowPanel(LoHangViewModel phieu) {
        JPanel row = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isSelected = selectedPhieu.contains(phieu);
                if (isSelected) g2.setColor(SELECTED_COLOR); else g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                if (isSelected) {
                    g2.setColor(SELECT_BORDER); g2.fillRoundRect(0, 0, 6, getHeight(), 15, 15);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                } else {
                    g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                }
                g2.dispose(); super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 60)); 
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        content.setOpaque(false);

        ModernCheckBox cbSelect = new ModernCheckBox();
        cbSelect.setPreferredSize(new Dimension(40, 25));
        cbSelect.setSelected(selectedPhieu.contains(phieu));
        cbSelect.addActionListener(e -> {
            if (cbSelect.isSelected()) selectedPhieu.add(phieu); else selectedPhieu.remove(phieu);
            row.repaint();
            updateSelectionUIState();
        });
        content.add(cbSelect);

        // 🔥 GIÃN CỘT: Khớp chính xác thông số Width với Header
        content.add(createCell(phieu.loHang.getMaLoHang(), 140, true));
        content.add(createCell(phieu.tenNcc, 500, true));
        
        String strNgay = (phieu.loHang.getNgayNhapKho() != null) ? phieu.loHang.getNgayNhapKho().format(DATE_FMT) : "—";
        content.add(createCell(strNgay, 200, false));
        
        JLabel lblTien = createCell(DinhDangUtil.dinhDangTien(phieu.loHang.getThanhTien()), 130, true);
        lblTien.setForeground(new Color(16, 185, 129)); 
        content.add(lblTien);

        // NÚT CHI TIẾT 
        JButton btnDetail = new JButton("📄 Chi tiết");
        btnDetail.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f));
        btnDetail.setForeground(new Color(59, 130, 246)); 
        btnDetail.setContentAreaFilled(false); btnDetail.setBorderPainted(false); btnDetail.setFocusPainted(false);
        btnDetail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnDetail.addMouseListener(new MouseAdapter() {
            @Override 
            public void mouseEntered(MouseEvent e) {
                // Đổi màu Đỏ
                btnDetail.setForeground(new Color(239, 68, 68)); 
                
                // Phông 14f + In đậm
                Font hoverFont = TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 14f);
                
                // Gạch chân
                java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>(hoverFont.getAttributes());
                attributes.put(java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON);
                btnDetail.setFont(hoverFont.deriveFont(attributes));
            }
            
            @Override 
            public void mouseExited(MouseEvent e) {
                // Trả về xanh lam & phông 13f
                btnDetail.setForeground(new Color(59, 130, 246)); 
                btnDetail.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(13f)); 
            }
        });
        
        btnDetail.addActionListener(e -> {
            // 🔥 Xóa cái thông báo cũ đi, thay bằng gọi hàm này
            showChiTietDialog(phieu);
        });
        
        // 🔥 Đẩy nút chi tiết lùi ra sau một tí cho thoáng
        content.add(Box.createHorizontalStrut(50)); 
        content.add(btnDetail);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (!selectedPhieu.contains(phieu)) { row.setBackground(HOVER_COLOR); row.repaint(); } }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(Color.WHITE); row.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { cbSelect.doClick(); }
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
    // THỐNG KÊ & HÀNH ĐỘNG (🔥 ĐÃ FIX XẾP DỌC THÀNH TỪNG HÀNG)
    // =========================================================
    private JPanel createStatsPanel() {
        JPanel pnl = new JPanel(new BorderLayout(0, 15));
        pnl.setOpaque(false);
        pnl.setPreferredSize(new Dimension(250, 0));

        // 🔥 Dùng GridLayout(0, 1) để ép hệ thống xếp thành MỘT CỘT DỌC DUY NHẤT
        JPanel pnlCards = new JPanel(new GridLayout(0, 1, 0, 15));
        pnlCards.setOpaque(false);

        lblTongDon     = createStatCard(pnlCards, "Tổng số đơn nhập", "0", TienIchGiaoDien.MAU_CHINH);
        lblTongSoLuong = createStatCard(pnlCards, "Tổng số lượng SP", "0", new Color(16, 185, 129)); // Màu Xanh lá
        lblTongTien    = createStatCard(pnlCards, "Tổng tiền nhập", "0 đ", new Color(245, 158, 11)); // Màu Cam

        JPanel pnlEmpty = new JPanel(); pnlEmpty.setOpaque(false);
        pnlCards.add(pnlEmpty); // Đệm vào để các ô không bị kéo giãn quá mức

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

    private JPanel createActionButtons() {
        JPanel pnl = new JPanel(new BorderLayout()); 
        pnl.setOpaque(false);

        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); pnlLeft.setOpaque(false);
        JButton btnReload = TienIchGiaoDien.taoNutHienDai("Làm mới ↻", new Color(100, 116, 139));
        btnReload.addActionListener(e -> handleReload());
        pnlLeft.add(btnReload);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); pnlRight.setOpaque(false);
        
        btnXoa = TienIchGiaoDien.taoNutHienDai("❌ Xóa lô", new Color(239, 68, 68));
        btnXoa.setVisible(false);
        btnXoa.addActionListener(e -> handleXoa());
        
        // 🔥 ĐÃ XÓA SẠCH NÚT "+ NHẬP HÀNG" VÀ CỬA SỔ DIALOG TỐI Ở ĐÂY 🔥

        pnlRight.add(btnXoa);

        pnl.add(pnlLeft, BorderLayout.WEST); pnl.add(pnlRight, BorderLayout.EAST);
        return pnl;
    }

    // =========================================================
    // DỮ LIỆU & LOGIC XỬ LÝ DATABASE (ĐÃ LẮP ĐỘNG CƠ TURBO 🚀)
    // =========================================================
    private void loadDataSieuToc() {
        isLoading = true;
        renderList(new ArrayList<>());
        
        SwingWorker<List<LoHangViewModel>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LoHangViewModel> doInBackground() {
                try { Thread.sleep(200); } catch (Exception ignored) {} // Hiệu ứng skeleton loading
                
                // 🚀 GỌI ĐỘNG CƠ TURBO: Tải tất cả 1 lần duy nhất vào RAM
                cacheData = Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuNhapHangSieuToc();
                
                List<LoHangViewModel> listResult = new ArrayList<>();
                if (cacheData != null) {
                    for (Data.LoHang lh : cacheData.dsLoHang) {
                        // Móc dữ liệu từ Map (RAM) với tốc độ O(1)
                        String tenCty = cacheData.mapTenNcc.getOrDefault(lh.getMaNCC(), lh.getMaNCC());
                        int tongSl = cacheData.mapTongSoLuong.getOrDefault(lh.getMaLoHang(), 0);
                        
                        listResult.add(new LoHangViewModel(lh, tenCty, tongSl));
                    }
                }
                return listResult;
            }

            @Override
            protected void done() {
                try {
                    danhSachGoc = get();
                    isLoading = false;
                    selectedPhieu.clear(); 
                    filterData(); 
                } catch (Exception e) { e.printStackTrace(); isLoading = false; }
            }
        };
        worker.execute();
    }

    private void filterData() {
        if (isLoading) return;
        String key = DinhDangUtil.loaiBoDauTiengViet(txtTimKiem.getText().toLowerCase().trim());
        
        // 1. Lọc theo từ khóa
        List<LoHangViewModel> filtered = danhSachGoc.stream().filter(pn -> {
            boolean matchKey = true;
            if (!key.isEmpty()) {
                String ten = DinhDangUtil.loaiBoDauTiengViet(pn.tenNcc.toLowerCase());
                String ma = pn.loHang.getMaLoHang().toLowerCase();
                matchKey = ten.contains(key) || ma.contains(key);
            }
            return matchKey;
        }).collect(Collectors.toList());
        
        // 2. 🔥 LOGIC SẮP XẾP 🔥
        int sortType = cbSapXep.getSelectedIndex();
        filtered.sort((p1, p2) -> {
            switch (sortType) {
                case 1: // Ngày nhập: Mới nhất (Giảm dần)
                    return p2.loHang.getNgayNhapKho().compareTo(p1.loHang.getNgayNhapKho());
                case 2: // Ngày nhập: Cũ nhất (Tăng dần)
                    return p1.loHang.getNgayNhapKho().compareTo(p2.loHang.getNgayNhapKho());
                case 3: // Tổng tiền: Tăng dần
                    return p1.loHang.getThanhTien().compareTo(p2.loHang.getThanhTien());
                case 4: // Tổng tiền: Giảm dần
                    return p2.loHang.getThanhTien().compareTo(p1.loHang.getThanhTien());
                default: // Mặc định: Theo Mã Lô
                    return p1.loHang.getMaLoHang().compareTo(p2.loHang.getMaLoHang());
            }
        });
        
        currentDisplayedList = filtered;
        renderList(currentDisplayedList);
        updateStats();
        updateSelectionUIState();
    }

    private void updateStats() {
        lblTongDon.setText(String.valueOf(currentDisplayedList.size()));

        // Đếm tổng số lượng SP
        int tongSL = currentDisplayedList.stream().mapToInt(pn -> pn.tongSoLuong).sum();
        lblTongSoLuong.setText(String.format("%,d", tongSL));

        BigDecimal tongTien = currentDisplayedList.stream()
                .map(pn -> pn.loHang.getThanhTien() != null ? pn.loHang.getThanhTien() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTongTien.setText(DinhDangUtil.dinhDangTien(tongTien));
    }

    private void setupListeners() {
        // Lắng nghe ô tìm kiếm
        txtTimKiem.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterData(); }
            public void removeUpdate(DocumentEvent e) { filterData(); }
            public void changedUpdate(DocumentEvent e) { filterData(); }
        });

        // 🔥 Lắng nghe Menu sắp xếp 🔥
        cbSapXep.addActionListener(e -> filterData());
    }

    private void handleSelectAll(boolean isSelected) {
        selectedPhieu.clear();
        if (isSelected) selectedPhieu.addAll(currentDisplayedList);
        renderList(currentDisplayedList); 
        updateSelectionUIState();
    }
    
    private void updateSelectionUIState() {
        btnXoa.setVisible(!selectedPhieu.isEmpty());
        if (currentDisplayedList.isEmpty()) {
            cbSelectAll.setSelected(false);
        } else {
            boolean allSelected = currentDisplayedList.stream().allMatch(pn -> selectedPhieu.contains(pn));
            cbSelectAll.setSelected(allSelected);
        }
        cbSelectAll.repaint();
    }
    
    private void handleXoa() {
        if (selectedPhieu.isEmpty()) return;
        TienIchGiaoDien.hienThiXacNhan(this, "Xóa vĩnh viễn " + selectedPhieu.size() + " lô hàng này?", () -> {
            try {
                for (LoHangViewModel pn : selectedPhieu) {
                    lhLogic.xoaLoHang(pn.loHang.getMaLoHang());
                }
                TienIchGiaoDien.hienThiThongBao(this, "Đã xóa thành công!", "SUCCESS");
                loadDataSieuToc(); 
            } catch (Exception e) {
                TienIchGiaoDien.hienThiThongBao(this, e.getMessage(), "ERROR");
            }
        });
    }
    private void handleReload() {
        txtTimKiem.clear(); // Xóa trắng ô tìm kiếm
        loadDataSieuToc();  // Tải lại dữ liệu từ DB
    }
 // =========================================================
    // 🔥 POPUP CHI TIẾT LÔ HÀNG (BẢNG THU NHỎ)
    // =========================================================
    private void showChiTietDialog(LoHangViewModel phieu) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        
        // 1. TẠO HIỆU ỨNG KÍNH MỜ
        java.awt.image.BufferedImage blurredBg = null;
        if (parentWindow != null && parentWindow.isShowing()) {
            try {
                java.awt.Robot robot = new java.awt.Robot();
                Rectangle rect = parentWindow.getBounds();
                java.awt.image.BufferedImage screen = robot.createScreenCapture(rect);
                float weight = 1.0f / 25.0f; float[] data = new float[25];
                for (int i = 0; i < 25; i++) data[i] = weight;
                java.awt.image.Kernel kernel = new java.awt.image.Kernel(5, 5, data);
                java.awt.image.ConvolveOp op = new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);
                blurredBg = op.filter(screen, null);
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        final java.awt.image.BufferedImage finalBlur = blurredBg;
        JDialog overlay = new JDialog(parentWindow, Dialog.ModalityType.MODELESS);
        overlay.setUndecorated(true);
        if (parentWindow != null) overlay.setBounds(parentWindow.getBounds());
        overlay.setBackground(new Color(0, 0, 0, 0)); 
        
        JPanel pnlOverlay = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create();
                if (finalBlur != null) g2.drawImage(finalBlur, 0, 0, getWidth(), getHeight(), null);
                g2.setColor(new Color(15, 23, 42, 190)); g2.fillRect(0, 0, getWidth(), getHeight()); g2.dispose();
            }
        };
        pnlOverlay.setOpaque(false); overlay.setContentPane(pnlOverlay);

        // 2. KHUNG DIALOG CHÍNH
        JDialog dialog = new JDialog(parentWindow, "Chi tiết", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true); dialog.setBackground(new Color(0, 0, 0, 0)); 
        
        JPanel pnlMain = new JPanel(new BorderLayout(0, 15)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(59, 130, 246)); g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20); g2.dispose();
            }
        };
        pnlMain.setOpaque(false); pnlMain.setBorder(new EmptyBorder(25, 30, 25, 30));
        pnlMain.setPreferredSize(new Dimension(1050, 600));

        // --- HEADER ---
        JPanel pnlHeader = new JPanel(new BorderLayout()); pnlHeader.setOpaque(false);
        JLabel lblTitle = new JLabel("CHI TIẾT LÔ HÀNG: " + phieu.loHang.getMaLoHang());
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(20f)); lblTitle.setForeground(new Color(15, 23, 42));
        
        JLabel lblSub = new JLabel("Nhà CC: " + phieu.tenNcc + "  |  Ngày nhập: " + (phieu.loHang.getNgayNhapKho() != null ? phieu.loHang.getNgayNhapKho().format(DATE_FMT) : "—"));
        lblSub.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(14f)); lblSub.setForeground(new Color(100, 116, 139));
        
        pnlHeader.add(lblTitle, BorderLayout.NORTH); pnlHeader.add(lblSub, BorderLayout.SOUTH);
        pnlMain.add(pnlHeader, BorderLayout.NORTH);

     // --- BẢNG THU NHỎ ---
        JPanel pnlTable = new JPanel(new BorderLayout()); pnlTable.setOpaque(false);
        JPanel pnlColHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        pnlColHeader.setBackground(new Color(241, 245, 249));
        pnlColHeader.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        pnlColHeader.setPreferredSize(new Dimension(0, 40));
        
        // 🔥 ĐÃ BÓP NHỎ WIDTH ĐỂ VỪA KHÍT 1 DÒNG
     // --- Header Bảng ---
        pnlColHeader.add(createCell("STT", 40, true));
        pnlColHeader.add(createCell("Mã SP", 70, true));
        pnlColHeader.add(createCell("Tên Sản Phẩm", 240, true));
        pnlColHeader.add(createCell("Giá Nhập", 110, true));
        pnlColHeader.add(createCell("Số Lượng", 90, true)); // 🔥 Đã nới rộng thành 90
        pnlColHeader.add(createCell("NSX", 90, true));
        pnlColHeader.add(createCell("HSD", 90, true));
        pnlColHeader.add(createCell("Thành Tiền", 130, true)); // Nới thêm một tí cho số tiền to
        pnlTable.add(pnlColHeader, BorderLayout.NORTH);

        JPanel pnlRows = new JPanel(); pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.Y_AXIS)); pnlRows.setBackground(Color.WHITE);
        JScrollPane scrollRows = new JScrollPane(pnlRows); TienIchGiaoDien.thietLapThanhCuon(scrollRows); scrollRows.setBorder(BorderFactory.createEmptyBorder());
        pnlTable.add(scrollRows, BorderLayout.CENTER);
        
        // 🔥 LẤY CHI TIẾT TRỰC TIẾP TỪ RAM SIÊU TỐC (Thay vì query DB qua ctLogic)
        List<Data.ChiTietLoHang> chiTiets = null;
        if (cacheData != null) {
            chiTiets = cacheData.mapChiTiet.get(phieu.loHang.getMaLoHang());
        }

        if (chiTiets != null) {
            for (int i = 0; i < chiTiets.size(); i++) {
                Data.ChiTietLoHang ct = chiTiets.get(i);
                
                // 🔥 LẤY TÊN SẢN PHẨM TỪ RAM VỚI TỐC ĐỘ O(1) (Thay vì query qua spLogic)
                String tenSP = "Không rõ";
                if (cacheData != null) {
                    tenSP = cacheData.mapTenSp.getOrDefault(ct.getMaSP(), "Không rõ");
                }
                
                BigDecimal thanhTien = ct.getGiaNhap().multiply(new BigDecimal(ct.getSoLuongNhap()));

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12)) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                        g2.dispose(); super.paintComponent(g);
                    }
                };
                row.setOpaque(false); row.setPreferredSize(new Dimension(0, 50)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                
                // 🔥 ĐỒNG BỘ WIDTH NHƯ HEADER
                // --- Dữ liệu Bảng ---
                row.add(createCell(String.valueOf(i + 1), 40, true));
                row.add(createCell(ct.getMaSP(), 70, true));
                row.add(createCell(tenSP, 240, true));
                row.add(createCell(DinhDangUtil.dinhDangTien(ct.getGiaNhap()), 110, false));
                
                JLabel lblSl = createCell(String.valueOf(ct.getSoLuongNhap()), 90, true); // 🔥 Khớp với 90 ở trên
                lblSl.setForeground(new Color(21, 128, 61)); row.add(lblSl);
                
                row.add(createCell(ct.getNSX() != null ? ct.getNSX().format(DATE_FMT) : "", 90, false));
                row.add(createCell(ct.getHSD() != null ? ct.getHSD().format(DATE_FMT) : "", 90, false));
                
                JLabel lblTien = createCell(DinhDangUtil.dinhDangTien(thanhTien), 130, true);
                lblTien.setForeground(new Color(234, 88, 12)); row.add(lblTien);

                pnlRows.add(row); pnlRows.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }
        pnlMain.add(pnlTable, BorderLayout.CENTER);

        // --- FOOTER ---
        JPanel pnlBot = new JPanel(new BorderLayout()); pnlBot.setOpaque(false);
        JLabel lblTotal = new JLabel("Tổng cộng: " + DinhDangUtil.dinhDangTien(phieu.loHang.getThanhTien()));
        lblTotal.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(18f)); lblTotal.setForeground(new Color(245, 158, 11));
        pnlBot.add(lblTotal, BorderLayout.WEST);

        JButton btnClose = TienIchGiaoDien.taoNutHienDai("Đóng", new Color(100, 116, 139));
        btnClose.setPreferredSize(new Dimension(120, 40));
        btnClose.addActionListener(e -> { dialog.dispose(); overlay.dispose(); });
        pnlBot.add(btnClose, BorderLayout.EAST);
        
        pnlMain.add(pnlBot, BorderLayout.SOUTH);

        dialog.add(pnlMain); dialog.pack(); dialog.setLocationRelativeTo(parentWindow);
        overlay.setVisible(true); dialog.setVisible(true); 
    }
    // Lớp Model Nội Bộ ghép Lô Hàng, Tên NCC và Tổng Số Lượng
    class LoHangViewModel {
        Data.LoHang loHang;
        String tenNcc;
        int tongSoLuong;
        public LoHangViewModel(Data.LoHang loHang, String tenNcc, int tongSoLuong) {
            this.loHang = loHang; 
            this.tenNcc = tenNcc;
            this.tongSoLuong = tongSoLuong;
        }
    }

    private class ModernCheckBox extends JCheckBox {
        public ModernCheckBox() { setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = 18; int y = (getHeight() - size) / 2; int x = (getWidth() - size) / 2; 
            if (isSelected()) {
                g2.setColor(TienIchGiaoDien.MAU_CHINH); g2.fillRoundRect(x, y, size, size, 5, 5); 
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 9, x + 8, y + 13); g2.drawLine(x + 8, y + 13, x + 14, y + 5); 
            } else {
                g2.setColor(Color.WHITE); g2.fillRoundRect(x, y, size, size, 5, 5);
                g2.setColor(new Color(180, 185, 195)); g2.drawRoundRect(x, y, size, size, 5, 5);
            }
            g2.dispose();
        }
    }
}