package GUI;

import Dao.ConnectDB;
import Data.GiamGia;
import Logic.GiamGiaLogic;
import Logic.TaoMaTuDongLogic;
import Logic.KhuyenMai.DiscountEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class GiamGiaUI extends JPanel {

    // ================= MÀU SẮC THEME HIỆN ĐẠI =================
    private final Color BG_CHINH = new Color(244, 247, 251);        
    private final Color BG_CARD = Color.WHITE;
    private final Color BORDER_COLOR = new Color(226, 232, 240);    
    private final Color TEXT_MAIN = new Color(30, 41, 59);          
    private final Color TEXT_MUTED = new Color(100, 116, 139);      
    private final Color ACCENT_BLUE = new Color(59, 130, 246);      
    private final Color HOVER_COLOR = new Color(248, 250, 252);     

    private final Color ST_SAFE_BG = new Color(220, 252, 231);      
    private final Color ST_SAFE_FG = new Color(22, 163, 74);        
    private final Color ST_WARN_BG = new Color(254, 243, 199);      
    private final Color ST_WARN_FG = new Color(217, 119, 6);        
    private final Color ST_DANGER_BG = new Color(254, 226, 226);    
    private final Color ST_DANGER_FG = new Color(220, 38, 38);      
    private final Color ST_EXPIRED_BG = new Color(225, 29, 72, 30); 
    private final Color ST_EXPIRED_FG = new Color(225, 29, 72);     

    private Font fontChinh;
    private Font fontDam;
    
    private JPanel pnlDanhSachSanPham;
    private JPanel pnlPhai;
    private CardLayout cardLayout;
    private JPanel pnlCards;
    private List<NutTab> danhSachNutTab = new ArrayList<>();
    
    // Lưu trữ các Sản Phẩm đang được Check
    private List<String> danhSachSPDuocChon = new ArrayList<>();
    private CheckBoxBoGoc chkAll; 
    private List<CheckBoxBoGoc> danhSachRowCheckboxes = new ArrayList<>(); 
    private boolean isUpdatingCheckboxes = false;
    private JTextField txtTimKiem;
    private JComboBox<String> cbTrangThai;
    private JComboBox<String> cbNhomHang;
    // Các ô nhập liệu cho form Giảm Giá
    private JTextField txtGiam, txtSoLuong;
    private ChonNgayGioCustom txtBatDau, txtKetThuc;

    public GiamGiaUI() {
        KhoiTaoFont();
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_CHINH);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        KhoiTaoHeader();
        KhoiTaoKhuVucDanhSach();
        KhoiTaoPanelPhai();
        KhoiTaoPanelDuoi();

        TaiDanhSachSanPham();
    }

    private void KhoiTaoFont() {
        try {
            fontChinh = new Font("Segoe UI", Font.PLAIN, 14);
            fontDam = new Font("Segoe UI", Font.BOLD, 14);
        } catch (Exception e) {
            fontChinh = new Font("SansSerif", Font.PLAIN, 14);
            fontDam = new Font("SansSerif", Font.BOLD, 14);
        }
    }

    private void KhoiTaoHeader() {
        JPanel pnlHeader = new JPanel(new BorderLayout(20, 0));
        pnlHeader.setOpaque(false);

        JPanel pnlLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlLoc.setOpaque(false);
        
        // 1. Khởi tạo các ô nhập liệu
        txtTimKiem = TaoONhapLieu("🔍 Tìm tên / mã...", 250);
        cbTrangThai = TaoComboBox(new String[]{"Tất cả sản phẩm", "⏳ Sắp hết hạn", "Quá hạn", "🔥 Đang Sale"});
        cbNhomHang = TaoComboBox(new String[]{"Tất cả nhóm hàng", "Thực phẩm", "Đồ uống", "Mì gói", "Hóa mỹ phẩm"});

        // 2. GẮN SỰ KIỆN (EVENT LISTENERS) ĐỂ LỌC DỮ LIỆU
        // Lắng nghe sự kiện gõ phím (Gõ tới đâu lọc tới đó)
        txtTimKiem.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TaiDanhSachSanPham(); 
            }
        });
        
        // Lắng nghe sự kiện đổi Menu Combobox
        cbTrangThai.addActionListener(e -> TaiDanhSachSanPham());
        cbNhomHang.addActionListener(e -> TaiDanhSachSanPham());

        pnlLoc.add(txtTimKiem);
        pnlLoc.add(cbTrangThai);
        pnlLoc.add(cbNhomHang);

        JPanel pnlThongKe = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlThongKe.setOpaque(false);
        pnlThongKe.add(TaoCardThongKe("Tổng SP", "1,245", ACCENT_BLUE));
        pnlThongKe.add(TaoCardThongKe("Cảnh Báo", "12", ST_DANGER_FG));

        pnlHeader.add(pnlLoc, BorderLayout.WEST);
        pnlHeader.add(pnlThongKe, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);
    }

    // =========================================================================
    // KHU VỰC DANH SÁCH (THÊM CỘT THAO TÁC HỦY GIẢM GIÁ)
    // =========================================================================
    private void KhoiTaoKhuVucDanhSach() {
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 0));
        pnlCenter.setOpaque(false);

        JPanel pnlHeaderWrap = new JPanel(new BorderLayout());
        pnlHeaderWrap.setOpaque(false);
        pnlHeaderWrap.setBorder(new EmptyBorder(10, 20, 10, 20)); 

        chkAll = new CheckBoxBoGoc();
        chkAll.addItemListener(e -> {
            if (isUpdatingCheckboxes) return; 
            isUpdatingCheckboxes = true;
            boolean isChecked = chkAll.isSelected();
            for (CheckBoxBoGoc cb : danhSachRowCheckboxes) cb.setSelected(isChecked);
            isUpdatingCheckboxes = false;
        });

        JLabel l1 = TaoLabelHeader("Sản phẩm", SwingConstants.LEFT);
        JLabel l2 = TaoLabelHeader("Mã lô", SwingConstants.CENTER);     // Căn giữa
        JLabel l3 = TaoLabelHeader("Hạn sử dụng", SwingConstants.CENTER);
        JLabel l4 = TaoLabelHeader("Tồn kho", SwingConstants.CENTER);
        JLabel l5 = TaoLabelHeader("Giá gốc", SwingConstants.CENTER);
        JLabel l6 = TaoLabelHeader("Giảm", SwingConstants.CENTER);
        JLabel l7 = TaoLabelHeader("Giá mới", SwingConstants.CENTER);
        JLabel l8 = TaoLabelHeader("Trạng thái", SwingConstants.CENTER);
        JLabel l9 = TaoLabelHeader("Thao tác", SwingConstants.CENTER);

        // Truyền đủ 9 cột vào Layout
        JPanel pnlTieuDe = TaoLayoutDong(chkAll, l1, l2, l3, l4, l5, l6, l7, l8, l9);
        pnlHeaderWrap.add(pnlTieuDe, BorderLayout.CENTER);

        pnlDanhSachSanPham = new JPanel();
        pnlDanhSachSanPham.setLayout(new BoxLayout(pnlDanhSachSanPham, BoxLayout.Y_AXIS));
        pnlDanhSachSanPham.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(pnlDanhSachSanPham);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        TuyChinhScrollBar(scrollPane);

        scrollPane.setColumnHeaderView(pnlHeaderWrap);
        scrollPane.getColumnHeader().setOpaque(false);

        pnlCenter.add(scrollPane, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);
    }

    public void TaiDanhSachSanPham()  {
        pnlDanhSachSanPham.removeAll();
        danhSachSPDuocChon.clear();
        danhSachRowCheckboxes.clear();
        if (chkAll != null) chkAll.setSelected(false);

        // --- BƯỚC 1: LẤY GIÁ TRỊ TỪ CÁC Ô TÌM KIẾM ---
        String tuKhoa = "";
        if (txtTimKiem != null && !txtTimKiem.getText().equals("🔍 Tìm tên / mã...")) {
            tuKhoa = txtTimKiem.getText().trim().toLowerCase();
        }
        String locTrangThai = (cbTrangThai != null) ? cbTrangThai.getSelectedItem().toString() : "Tất cả sản phẩm";

        // Query gốc (Tải toàn bộ từ DB lên)
        String sql = "SELECT sp.MaSP, sp.TenSP, ct.MaLoHang, ct.HSD, ct.SoLuongTon, sp.GiaBan, " +
                     "ISNULL(gg.GiamGia, 0) AS GiamGia " +
                     "FROM ChiTietLoHang ct " +
                     "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "LEFT JOIN GiamGia gg ON sp.MaSP = gg.MaSP AND gg.TrangThaiGiamGia = N'Đang diễn ra' AND gg.SoLuongApDung > 0 " +
                     "WHERE ct.SoLuongTon > 0 ORDER BY ct.HSD ASC";

        try (java.sql.Connection con = ConnectDB.getInstance().getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String maSP = rs.getString("MaSP");
                String tenSP = rs.getString("TenSP");
                String maLo = rs.getString("MaLoHang");
                java.sql.Date hsd = rs.getDate("HSD");
                int tonKho = rs.getInt("SoLuongTon");
                int giaGoc = rs.getInt("GiaBan");
                int phanTram = rs.getInt("GiamGia");
                
                // Xác định trạng thái của sản phẩm
                String trangThai = "Còn lâu";
                long ngayConLai = 0;
                if (hsd != null) {
                    ngayConLai = ChronoUnit.DAYS.between(java.time.LocalDate.now(), hsd.toLocalDate());
                    if (ngayConLai < 0) trangThai = "Quá hạn";
                    else if (ngayConLai <= 3) trangThai = "Nguy hiểm";
                    else if (ngayConLai <= 15) trangThai = "Sắp hết hạn";
                }
                if (phanTram > 0) trangThai = "Đang Sale";

                // --- BƯỚC 2: MÀNG LỌC TÌM KIẾM TRÊN RAM (O(1)) ---
                
                // Lọc theo Tên SP, Mã SP hoặc Mã Lô
                if (!tuKhoa.isEmpty() && !tenSP.toLowerCase().contains(tuKhoa) 
                    && !maSP.toLowerCase().contains(tuKhoa) && !maLo.toLowerCase().contains(tuKhoa)) {
                    continue; // Không khớp -> Bỏ qua dòng này
                }
                
                // Lọc theo Trạng thái Combobox
                if (!locTrangThai.equals("Tất cả sản phẩm")) {
                    if (locTrangThai.equals("⏳ Sắp hết hạn") && !trangThai.equals("Sắp hết hạn") && !trangThai.equals("Nguy hiểm")) continue;
                    if (locTrangThai.equals("Quá hạn") && !trangThai.equals("Quá hạn")) continue;
                    if (locTrangThai.equals("🔥 Đang Sale") && !trangThai.equals("Đang Sale")) continue;
                }

                // Nếu lọt qua hết các màng lọc -> Vẽ lên Giao diện
                Object[] data = {tenSP, maLo, (int)ngayConLai, tonKho, giaGoc, phanTram, trangThai, maSP};
                pnlDanhSachSanPham.add(TaoDongSanPham(data));
                pnlDanhSachSanPham.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        } catch (Exception e) { e.printStackTrace(); }
        pnlDanhSachSanPham.revalidate(); pnlDanhSachSanPham.repaint();
    }
    private JPanel TaoDongSanPham(Object[] data) {
        String tenSP = (String) data[0];
        String maLo = (String) data[1];
        int ngayConLai = (int) data[2];
        int tonKho = (int) data[3];
        int giaGoc = (int) data[4];
        int phanTram = (int) data[5];
        String trangThai = (String) data[6];
        String maSP = (String) data[7];

        PanelBoGoc row = new PanelBoGoc(15, BG_CARD);
        row.setLayout(new BorderLayout()); 
        row.setBorder(new EmptyBorder(10, 20, 10, 20));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        CheckBoxBoGoc chkChon = new CheckBoxBoGoc();
        danhSachRowCheckboxes.add(chkChon);
        chkChon.addItemListener(e -> {
            if (isUpdatingCheckboxes) return;
            if (chkChon.isSelected()) {
                if (!danhSachSPDuocChon.contains(maSP)) danhSachSPDuocChon.add(maSP);
                if (!isUpdatingCheckboxes && danhSachSPDuocChon.size() == danhSachRowCheckboxes.size()) {
                    isUpdatingCheckboxes = true; chkAll.setSelected(true); isUpdatingCheckboxes = false;
                }
            } else {
                danhSachSPDuocChon.remove(maSP);
                if (!isUpdatingCheckboxes) {
                    isUpdatingCheckboxes = true; chkAll.setSelected(false); isUpdatingCheckboxes = false;
                }
            }
        });

        Color bgStatus, fgStatus;
        if (ngayConLai < 0) { bgStatus = ST_EXPIRED_BG; fgStatus = ST_EXPIRED_FG; }
        else if (ngayConLai <= 3) { bgStatus = ST_DANGER_BG; fgStatus = ST_DANGER_FG; }
        else if (ngayConLai <= 15) { bgStatus = ST_WARN_BG; fgStatus = ST_WARN_FG; }
        else { bgStatus = ST_SAFE_BG; fgStatus = ST_SAFE_FG; }

        int giaSauGiam = giaGoc - (giaGoc * phanTram / 100);

        // --- SỬA Ở ĐÂY: Chuyển các thông số sang căn giữa (CENTER) ---
        JLabel lblTen = new JLabel("<html><b>" + tenSP + "</b></html>", SwingConstants.LEFT);
        lblTen.setFont(fontChinh); lblTen.setForeground(TEXT_MAIN);

        JLabel lblMa = new JLabel(maLo, SwingConstants.CENTER);         // Căn giữa
        lblMa.setFont(fontChinh); lblMa.setForeground(TEXT_MUTED);

        JLabel lblHSD = new JLabel(ngayConLai < 0 ? "Quá hạn" : ngayConLai + " ngày", SwingConstants.CENTER);
        lblHSD.setFont(fontDam); lblHSD.setForeground(fgStatus);

        JLabel lblTon = new JLabel(String.valueOf(tonKho), SwingConstants.CENTER);
        lblTon.setFont(fontChinh); lblTon.setForeground(TEXT_MAIN);

        JLabel lblGiaGoc = new JLabel(String.format("%,d ₫", giaGoc), SwingConstants.CENTER);
        lblGiaGoc.setFont(fontChinh); lblGiaGoc.setForeground(TEXT_MUTED);
        if(phanTram > 0) lblGiaGoc.setText("<html><strike>" + lblGiaGoc.getText() + "</strike></html>");

        JLabel lblGiam = new JLabel(phanTram > 0 ? "-" + phanTram + "%" : "-", SwingConstants.CENTER);
        lblGiam.setFont(fontDam); lblGiam.setForeground(ST_DANGER_FG);

        JLabel lblGiaMoi = new JLabel(String.format("%,d ₫", giaSauGiam), SwingConstants.CENTER);
        lblGiaMoi.setFont(fontDam); lblGiaMoi.setForeground(ACCENT_BLUE);

        BadgeLabel badgeStatus = new BadgeLabel(trangThai, bgStatus, fgStatus);
        JPanel pnlBadgeWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        pnlBadgeWrap.setOpaque(false);
        pnlBadgeWrap.add(badgeStatus);

        // 🚀 CỘT 9: TẠO NÚT HỦY KHUYẾN MÃI NHANH NẾU ĐANG SALE
        JComponent cmpThaoTac;
        if (phanTram > 0) {
            NutHienDai btnHuyNhanh = new NutHienDai("Hủy", ST_DANGER_BG, ST_DANGER_FG);
            btnHuyNhanh.setFont(fontDam.deriveFont(11f));
            btnHuyNhanh.setPreferredSize(new Dimension(80, 30));
            btnHuyNhanh.addActionListener(e -> {
                int cf = JOptionPane.showConfirmDialog(this, 
                    "Bạn muốn kết thúc ngay chương trình giảm giá của: " + tenSP + "?", 
                    "Xác nhận Hủy", JOptionPane.YES_NO_OPTION);
                if (cf == JOptionPane.YES_OPTION) {
                    try {
                        new Logic.GiamGiaLogic().huyGiamGia(maSP);
                        TaiDanhSachSanPham(); // Refresh bảng
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage());
                    }
                }
            });
            JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
            pnlBtn.setOpaque(false);
            pnlBtn.add(btnHuyNhanh);
            cmpThaoTac = pnlBtn;
        } else {
            cmpThaoTac = new JLabel(""); // Nếu không Sale thì để trống
        }

        // Gắn dữ liệu vào Layout đồng bộ 9 cột
        JPanel pnlData = TaoLayoutDong(chkChon, lblTen, lblMa, lblHSD, lblTon, lblGiaGoc, lblGiam, lblGiaMoi, pnlBadgeWrap, cmpThaoTac);
        row.add(pnlData, BorderLayout.CENTER);

        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(HOVER_COLOR); }
            public void mouseExited(MouseEvent e) { row.setBackground(BG_CARD); }
            public void mouseClicked(MouseEvent e) { chkChon.setSelected(!chkChon.isSelected()); }
        });

        return row;
    }

    private JPanel TaoLayoutDong(JComponent chk, JComponent c1, JComponent c2, JComponent c3, 
                                 JComponent c4, JComponent c5, JComponent c6, JComponent c7, JComponent c8, JComponent c9) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; 
        gbc.weighty = 1.0;
        
        // Căn chỉnh tỷ lệ lại cho vừa khớp 100% (Tổng weightx = 1.0)
        row.add(wrapGrid(chk), taoGBC(gbc, 0, 0.04)); // Checkbox 4%
        row.add(wrapGrid(c1), taoGBC(gbc, 1, 0.18));  // Tên SP giảm từ 20% xuống 18%
        row.add(wrapGrid(c2), taoGBC(gbc, 2, 0.10));  // Mã lô 10%
        row.add(wrapGrid(c3), taoGBC(gbc, 3, 0.11));  // HSD 11%
        row.add(wrapGrid(c4), taoGBC(gbc, 4, 0.07));  // Tồn 7%
        row.add(wrapGrid(c5), taoGBC(gbc, 5, 0.10));  // Giá gốc 10%
        row.add(wrapGrid(c6), taoGBC(gbc, 6, 0.08));  // Giảm 8%
        row.add(wrapGrid(c7), taoGBC(gbc, 7, 0.10));  // Giá mới 10%
        row.add(wrapGrid(c8), taoGBC(gbc, 8, 0.12));  // Trạng thái 12%
        row.add(wrapGrid(c9), taoGBC(gbc, 9, 0.10));  // Thao tác (Nút Hủy) chiếm 10%

        return row;
    }

    private GridBagConstraints taoGBC(GridBagConstraints gbc, int x, double weightx) {
        gbc.gridx = x;
        gbc.weightx = weightx;
        return gbc;
    }

    private JPanel wrapGrid(JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 40)); 
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // KHU VỰC MENU BÊN PHẢI (GỘP THÀNH 2 TAB)
    // =========================================================================
    private void KhoiTaoPanelPhai() {
        pnlPhai = new JPanel(new BorderLayout());
        pnlPhai.setPreferredSize(new Dimension(380, 0));
        pnlPhai.setOpaque(false);
        pnlPhai.setBorder(new EmptyBorder(0, 20, 0, 0));

        PanelBoGoc container = new PanelBoGoc(20, BG_CARD);
        container.setLayout(new BorderLayout(0, 20));
        container.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblTitle = new JLabel("Bảng Điều Khiển");
        lblTitle.setFont(fontDam.deriveFont(18f));
        lblTitle.setForeground(TEXT_MAIN);
        container.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlCenter = new JPanel(new BorderLayout(0, 20));
        pnlCenter.setOpaque(false);

        PanelBoGoc pnlTabs = new PanelBoGoc(12, new Color(241, 245, 249)); 
        pnlTabs.setLayout(new GridLayout(1, 2, 5, 5)); 
        pnlTabs.setBorder(new EmptyBorder(5, 5, 5, 5));

        cardLayout = new CardLayout();
        pnlCards = new JPanel(cardLayout);
        pnlCards.setOpaque(false);

        pnlCards.add(TaoTabThuCong(), "Thủ công");
        pnlCards.add(TaoTabTuDong(), "Tự động");

        TaoNutChuyenTab(pnlTabs, "Thủ công", true);
        TaoNutChuyenTab(pnlTabs, "Tự động", false);

        pnlCenter.add(pnlTabs, BorderLayout.NORTH);
        pnlCenter.add(pnlCards, BorderLayout.CENTER);

        container.add(pnlCenter, BorderLayout.CENTER);
        pnlPhai.add(container, BorderLayout.CENTER);
        add(pnlPhai, BorderLayout.EAST);
    }

    private void TaoNutChuyenTab(JPanel pnlTabs, String tenTab, boolean isActive) {
        NutTab btnTab = new NutTab(tenTab);
        danhSachNutTab.add(btnTab);
        if (isActive) btnTab.setActive(true);

        btnTab.addActionListener(e -> {
            for (NutTab nut : danhSachNutTab) nut.setActive(false);
            btnTab.setActive(true);
            cardLayout.show(pnlCards, tenTab);
        });
        pnlTabs.add(btnTab);
    }

    private JPanel TaoTabThuCong() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); 
        p.setOpaque(false);
        
        JLabel lblHuongDan = new JLabel("<html><i>Tick chọn các sản phẩm bên trái để áp dụng mức giảm giá.</i></html>");
        lblHuongDan.setFont(fontChinh.deriveFont(12f));
        lblHuongDan.setForeground(TEXT_MUTED);
        lblHuongDan.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblHuongDan);
        p.add(Box.createVerticalStrut(15));
        
        txtGiam = TaoONhapLieu("Nhập mức giảm (%)", 0);
        txtSoLuong = TaoONhapLieu("Số lượng mã tối đa", 0);
        txtBatDau = new ChonNgayGioCustom("Chọn ngày giờ bắt đầu...");
        txtKetThuc = new ChonNgayGioCustom("Chọn ngày giờ kết thúc...");

        NutHienDai btnApDung = new NutHienDai("Áp Dụng Khuyến Mãi", ACCENT_BLUE, Color.WHITE);
        btnApDung.addActionListener(e -> ApDungGiamGia());

        NutHienDai btnHuy = new NutHienDai("Hủy Giảm Giá", new Color(241, 245, 249), TEXT_MAIN);
        btnHuy.addActionListener(e -> {
            try {
                GiamGiaLogic logic = new GiamGiaLogic();
                for (String ma : danhSachSPDuocChon) logic.huyGiamGia(ma);
                TaiDanhSachSanPham();
                JOptionPane.showMessageDialog(this, "Đã gỡ bỏ khuyến mãi!");
            } catch (Exception ex) {}
        });

        for(JComponent comp : new JComponent[]{txtGiam, txtSoLuong, txtBatDau, txtKetThuc, btnApDung, btnHuy}) {
            comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        AddLabel(p, "Mức giảm giá (%):"); p.add(txtGiam); p.add(Box.createVerticalStrut(10));
        AddLabel(p, "Số lượng áp dụng:"); p.add(txtSoLuong); p.add(Box.createVerticalStrut(10));
        AddLabel(p, "Ngày bắt đầu:"); p.add(txtBatDau); p.add(Box.createVerticalStrut(10));
        AddLabel(p, "Ngày kết thúc:"); p.add(txtKetThuc); p.add(Box.createVerticalStrut(20));
        p.add(btnApDung); p.add(Box.createVerticalStrut(8)); p.add(btnHuy);
        
        return p;
    }

    // =========================================================================
    // TAB 2: TỰ ĐỘNG (XỬ LÝ AI) - NÂNG CẤP HIỂN THỊ TRỰC QUAN
    // =========================================================================
    private JPanel TaoTabTuDong() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        
        JLabel lblInfo = new JLabel("<html><b>Hệ Thống Phân Tích Tự Động</b><br><br>"
                + "Thuật toán sẽ quét toàn bộ danh sách, phân tích Số ngày còn lại & Tồn kho "
                + "để cấu hình mức xả hàng tối ưu nhất.<br><br>"
                + "<i>• Dưới 3 ngày → Đề xuất ~40%<br>• Dưới 7 ngày → Đề xuất ~25%</i></html>");
        lblInfo.setFont(fontChinh);
        lblInfo.setForeground(TEXT_MUTED);
        lblInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- BẮT ĐẦU NÂNG CẤP KHU VỰC SLIDER ---
        JPanel pnlSliderWrap = new JPanel(new BorderLayout(0, 10));
        pnlSliderWrap.setOpaque(false);
        pnlSliderWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        pnlSliderWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 1. Header của Slider (Gồm Tiêu đề và Chỉ số hiển thị % realtime)
        JPanel pnlSliderHeader = new JPanel(new BorderLayout());
        pnlSliderHeader.setOpaque(false);
        
        JLabel lblTitle = TaoLabelForm("Mức độ ưu tiên xả kho:");
        JLabel lblGiaTriRealtime = new JLabel("Tiêu chuẩn (60%)");
        lblGiaTriRealtime.setFont(fontDam);
        lblGiaTriRealtime.setForeground(ACCENT_BLUE); // Mặc định màu xanh
        
        pnlSliderHeader.add(lblTitle, BorderLayout.WEST);
        pnlSliderHeader.add(lblGiaTriRealtime, BorderLayout.EAST);

        // 2. Thiết kế Slider xịn xò có vạch và nhãn
        JSlider slider = new JSlider(0, 100, 60);
        slider.setOpaque(false);
        slider.setMajorTickSpacing(50); // Vạch chia lớn ở 0, 50, 100
        slider.setMinorTickSpacing(10); // Vạch chia nhỏ
        slider.setPaintTicks(true);     // Hiện vạch
        slider.setPaintLabels(true);    // Hiện nhãn chữ
        
        // Custom nhãn chữ cho các mốc quan trọng
        java.util.Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();
        labelTable.put(0, new JLabel("Giữ giá"));
        labelTable.put(50, new JLabel("Tiêu chuẩn"));
        labelTable.put(100, new JLabel("Xả lỗ"));
        slider.setLabelTable(labelTable);

        // Bắt sự kiện khi kéo thanh gạt -> Cập nhật Text và Đổi màu
        slider.addChangeListener(e -> {
            int val = slider.getValue();
            if (val <= 30) {
                lblGiaTriRealtime.setText("Thận trọng (" + val + "%)");
                lblGiaTriRealtime.setForeground(ST_SAFE_FG); // Màu xanh lá
            } else if (val <= 75) {
                lblGiaTriRealtime.setText("Tiêu chuẩn (" + val + "%)");
                lblGiaTriRealtime.setForeground(ACCENT_BLUE); // Màu xanh dương
            } else {
                lblGiaTriRealtime.setText("Xả gấp (" + val + "%)");
                lblGiaTriRealtime.setForeground(ST_DANGER_FG); // Màu đỏ báo động
            }
        });

        pnlSliderWrap.add(pnlSliderHeader, BorderLayout.NORTH);
        pnlSliderWrap.add(slider, BorderLayout.CENTER);
        // --- KẾT THÚC NÂNG CẤP SLIDER ---

        NutHienDai btnKichHoat = new NutHienDai("✨ Kích Hoạt Quét Tự Động", ST_SAFE_FG, Color.WHITE);
        btnKichHoat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnKichHoat.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnKichHoat.addActionListener(e -> TuDongDeXuatMucGiam());

        p.add(lblInfo);
        p.add(Box.createVerticalStrut(25));
        p.add(pnlSliderWrap); // Thêm cụm Slider mới vào Panel chính
        p.add(Box.createVerticalStrut(30));
        p.add(btnKichHoat);
        
        return p;
    }
    private void KhoiTaoPanelDuoi() {
        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.setOpaque(false);
        pnlBottom.setBorder(new EmptyBorder(15, 0, 0, 0));

        PanelBoGoc container = new PanelBoGoc(15, BG_CARD);
        container.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));

        JLabel lblTimeline = new JLabel("💡 Gợi ý hệ thống: Chọn các sản phẩm quá hạn hoặc cận date và áp dụng quét Khuyến mãi để xả hàng.");
        lblTimeline.setFont(fontChinh);
        lblTimeline.setForeground(ST_WARN_FG);
        container.add(lblTimeline);
        pnlBottom.add(container, BorderLayout.CENTER);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    // =========================================================================
    // LOGIC THÊM GIẢM GIÁ (ĐÃ FIX LỖI TRÙNG MÃ KHI BATCH INSERT)
    // =========================================================================
    private void ApDungGiamGia() {
        if (danhSachSPDuocChon.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng tick chọn ít nhất 1 sản phẩm từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BigDecimal mucGiam = new BigDecimal(txtGiam.getText().trim());
            int soLuong = Integer.parseInt(txtSoLuong.getText().trim());
            
            LocalDateTime batDau = txtBatDau.getDateTime();
            LocalDateTime ketThuc = txtKetThuc.getDateTime();

            if (batDau == null || ketThuc == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ ngày và giờ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            GiamGiaLogic logic = new GiamGiaLogic();
            List<GiamGia> dsGiamGiaMoi = new ArrayList<>();

            // 🚀 BƯỚC QUAN TRỌNG: Lấy mã gốc 1 lần duy nhất từ DB, sau đó tự đếm trên RAM
            String maGiamGiaGoc = TaoMaTuDongLogic.taoMaGiamGia(); // VD: "GG003"
            int soHienTai = Integer.parseInt(maGiamGiaGoc.substring(2)); // Cắt lấy số 3

            for (String maSP : danhSachSPDuocChon) {
                GiamGia ggMoi = new GiamGia();
                
                // Tự động ép kiểu chuỗi với format 3 chữ số (003, 004, 005...)
                ggMoi.setMaGiamGia("GG" + String.format("%03d", soHienTai++));
                
                ggMoi.setMaSP(maSP);
                ggMoi.setBatDau(batDau);
                ggMoi.setKetThuc(ketThuc);
                ggMoi.setGiamGia(mucGiam);
                ggMoi.setLoaiGiamGia("Tự Nhập");          
                ggMoi.setTrangThaiGiamGia("Đang diễn ra"); 
                ggMoi.setSoLuongApDung(soLuong);
                
                dsGiamGiaMoi.add(ggMoi);
            }
            
            // Đẩy toàn bộ danh sách xuống Logic để chạy Truy Vấn Siêu Tốc
            String ketQua = logic.themGiamGiaHangLoat(dsGiamGiaMoi);
            JOptionPane.showMessageDialog(this, ketQua, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            TaiDanhSachSanPham(); 
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi nhập liệu: " + e.getMessage() + "\nVui lòng kiểm tra lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    // =========================================================================
    // HÀM XỬ LÝ AI (ĐÃ FIX LỖI MẤT KẾT NỐI VÀ LỖI TRÙNG KHÓA CHÍNH PRIMARY KEY)
    // =========================================================================
    private void TuDongDeXuatMucGiam() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Hệ thống AI sẽ quét toàn bộ kho, phân tích Giờ Vàng, Hạn Sử Dụng, Lượng Tồn và Ngày Nhập để xả hàng.\nBạn có chắc chắn muốn thực hiện?", 
            "Xác nhận Quét Tự Động", JOptionPane.YES_NO_OPTION);
            
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            GiamGiaLogic logic = new GiamGiaLogic();
            List<GiamGia> dsGiamGiaMoi = new ArrayList<>();
            
            // BƯỚC 1: Gọi DAO Siêu Tốc - Tải toàn bộ kho hàng lên RAM
            List<Object[]> danhSachHangTrongKho = Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuQuetKhoAISieuToc();

            // 🚀 BƯỚC QUAN TRỌNG: Lấy mã gốc 1 lần duy nhất, AI tự đếm số thứ tự trên RAM
            String maGiamGiaGoc = TaoMaTuDongLogic.taoMaGiamGia(); 
            int soHienTai = Integer.parseInt(maGiamGiaGoc.substring(2));

            // BƯỚC 2: CHẠY ĐỘNG CƠ AI CHO TỪNG SẢN PHẨM TRÊN RAM
            LocalDateTime now = LocalDateTime.now();
            Logic.KhuyenMai.DiscountEngine engine = new Logic.KhuyenMai.DiscountEngine(); 
            
            for (Object[] row : danhSachHangTrongKho) {
                String maSP = (String) row[0];
                String maLoai = (String) row[1];
                java.sql.Date sqlHSD = (java.sql.Date) row[2];
                int tonKho = (int) row[3];
                double giaNhap = (double) row[4];
                double giaBan = (double) row[5];
                java.sql.Date sqlNhap = (java.sql.Date) row[6];

                java.time.LocalDate hsd = (sqlHSD != null) ? sqlHSD.toLocalDate() : null;
                java.time.LocalDate ngayNhapKho = (sqlNhap != null) ? sqlNhap.toLocalDate() : null;

                // Nạp Context vào AI
                Logic.KhuyenMai.DiscountEngine.KetQuaGiamGia ketQuaAI = logic.tinhGiaGiamTuDongSieuThi(maSP, maLoai, tonKho, giaNhap, giaBan, ngayNhapKho, hsd);
                
                if (ketQuaAI.phanTramGiamCuoiCung > 0) {
                    GiamGia ggMoi = new GiamGia();
                    
                    // Gắn mã giảm giá và tăng số đếm lên 1 cho vòng lặp tiếp theo
                    ggMoi.setMaGiamGia("GG" + String.format("%03d", soHienTai++));
                    
                    ggMoi.setMaSP(maSP);
                    ggMoi.setBatDau(now);
                    
                    LocalDateTime ketThuc = (hsd != null) ? hsd.atTime(23, 59, 59) : now.plusDays(3);
                    ggMoi.setKetThuc(ketThuc);
                    
                    BigDecimal mucGiam = new BigDecimal(ketQuaAI.phanTramGiamCuoiCung * 100).setScale(0, java.math.RoundingMode.HALF_UP);
                    ggMoi.setGiamGia(mucGiam);
                    ggMoi.setLoaiGiamGia("Tự động");          
                    ggMoi.setTrangThaiGiamGia("Đang diễn ra"); 
                    ggMoi.setSoLuongApDung(tonKho); 
                    
                    dsGiamGiaMoi.add(ggMoi);
                }
            }

            if (dsGiamGiaMoi.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kho hàng đang an toàn/chưa tới Giờ vàng. Không có sản phẩm nào cần xả kho lúc này!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // BƯỚC 3: Lưu hàng loạt vào DB qua Batch Insert siêu tốc
            String ketQua = logic.themGiamGiaHangLoat(dsGiamGiaMoi);
            JOptionPane.showMessageDialog(this, ketQua, "Kết quả Quét Tự Động", JOptionPane.INFORMATION_MESSAGE);
            
            TaiDanhSachSanPham(); // Refresh bảng UI
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi quét tự động AI: " + ex.getMessage(), "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =====================================================================
    // TIỆN ÍCH UI VÀ ĐỒ HOẠ CUSTOM NỘI BỘ
    // =====================================================================

    private JLabel TaoLabelForm(String text) {
        JLabel lbl = new JLabel(text); lbl.setFont(fontDam); lbl.setForeground(TEXT_MAIN); return lbl;
    }

    private void AddLabel(JPanel p, String text) {
        JLabel lbl = TaoLabelForm(text); lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl); p.add(Box.createVerticalStrut(5));
    }

    private JLabel TaoLabelHeader(String text, int alignment) {
        JLabel lbl = new JLabel(text, alignment); lbl.setFont(fontDam.deriveFont(13f)); lbl.setForeground(TEXT_MUTED); return lbl;
    }

    private JTextField TaoONhapLieu(String placeholder, int width) {
        JTextField txt = new JTextField(placeholder); txt.setFont(fontChinh); txt.setForeground(TEXT_MAIN);
        if(width > 0) txt.setPreferredSize(new Dimension(width, 40));
        txt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(5, 15, 5, 15)));
        txt.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) { if (txt.getText().equals(placeholder)) txt.setText(""); }
            public void focusLost(java.awt.event.FocusEvent evt) { if (txt.getText().isEmpty()) txt.setText(placeholder); }
        });
        return txt;
    }

    private JComboBox<String> TaoComboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items); cb.setFont(fontChinh); cb.setBackground(Color.WHITE); cb.setPreferredSize(new Dimension(180, 40)); return cb;
    }

    private JPanel TaoCardThongKe(String title, String value, Color accentColor) {
        PanelBoGoc card = new PanelBoGoc(12, BG_CARD);
        card.setLayout(new GridLayout(2, 1)); card.setBorder(new EmptyBorder(8, 15, 8, 15)); card.setPreferredSize(new Dimension(130, 60));
        JLabel lblTitle = new JLabel(title, SwingConstants.RIGHT); lblTitle.setFont(fontChinh.deriveFont(12f)); lblTitle.setForeground(TEXT_MUTED);
        JLabel lblVal = new JLabel(value, SwingConstants.RIGHT); lblVal.setFont(fontDam.deriveFont(20f)); lblVal.setForeground(accentColor);
        card.add(lblTitle); card.add(lblVal); return card;
    }

    private void TuyChinhScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            protected void configureScrollBarColors() { this.thumbColor = new Color(203, 213, 225); this.trackColor = BG_CHINH; }
            protected JButton createDecreaseButton(int orientation) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
            protected JButton createIncreaseButton(int orientation) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
    }

    // --- CÁC CLASS ĐỒ HOẠ ---
    class CheckBoxBoGoc extends JCheckBox {
        public CheckBoxBoGoc() {
            setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
            setIcon(new CustomIcon(false)); setSelectedIcon(new CustomIcon(true));
        }
        class CustomIcon implements Icon {
            private boolean isSelected;
            public CustomIcon(boolean isSelected) { this.isSelected = isSelected; }
            public int getIconWidth() { return 22; } public int getIconHeight() { return 22; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected) {
                    g2.setColor(ACCENT_BLUE); g2.fillRoundRect(x, y + 2, 18, 18, 6, 6); 
                    g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 4, y + 12, x + 8, y + 16); g2.drawLine(x + 8, y + 16, x + 14, y + 7); 
                } else {
                    g2.setColor(Color.WHITE); g2.fillRoundRect(x, y + 2, 18, 18, 6, 6);
                    g2.setColor(BORDER_COLOR); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(x, y + 2, 18, 18, 6, 6);
                }
                g2.dispose();
            }
        }
    }

    class PanelBoGoc extends JPanel {
        private int radius; private Color bgColor;
        public PanelBoGoc(int radius, Color bgColor) { this.radius = radius; this.bgColor = bgColor; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER_COLOR); g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.setColor(bgColor); g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, radius, radius)); g2.dispose();
        }
    }

    class NutTab extends JButton {
        private boolean isActive = false;
        public NutTab(String text) {
            super(text); setFont(fontDam.deriveFont(13f)); setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); CapNhatMauSac();
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (!isActive) setForeground(ACCENT_BLUE); }
                public void mouseExited(MouseEvent e) { CapNhatMauSac(); }
            });
        }
        public void setActive(boolean active) { this.isActive = active; CapNhatMauSac(); }
        private void CapNhatMauSac() { setForeground(isActive ? Color.WHITE : TEXT_MUTED); repaint(); }
        protected void paintComponent(Graphics g) {
            if (isActive) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_BLUE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10); g2.dispose();
            }
            super.paintComponent(g); 
        }
    }

    class NutHienDai extends JButton {
        private Color bgColor;
        public NutHienDai(String text, Color bg, Color fg) {
            super(text); this.bgColor = bg; setForeground(fg); setFont(fontDam); setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { bgColor = bg.darker(); repaint(); }
                public void mouseExited(MouseEvent e) { bgColor = bg; repaint(); }
            });
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); g2.dispose(); super.paintComponent(g);
        }
    }

    class BadgeLabel extends JLabel {
        private Color bgColor;
        public BadgeLabel(String text, Color bg, Color fg) {
            super(text, SwingConstants.CENTER); this.bgColor = bg; setForeground(fg); setFont(fontDam.deriveFont(12f)); setBorder(new EmptyBorder(4, 12, 4, 12)); setOpaque(false);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); g2.dispose(); super.paintComponent(g);
        }
    }

    // =====================================================================
    // HỆ THỐNG CUSTOM DATE-TIME PICKER (LỊCH + VÒNG XOAY GIỜ KIỂU IOS)
    // =====================================================================
    class ChonNgayGioCustom extends JPanel {
        private JLabel lblHienThi; private java.time.LocalDateTime selectedDateTime; private JPopupMenu popup;
        public ChonNgayGioCustom(String placeholder) {
            setLayout(new BorderLayout()); setBackground(Color.WHITE); 
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(5, 15, 5, 15)));
            lblHienThi = new JLabel("📅 " + placeholder); lblHienThi.setForeground(TEXT_MUTED); lblHienThi.setFont(fontChinh); add(lblHienThi, BorderLayout.CENTER);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { hienThiPopup(); }
                public void mouseEntered(MouseEvent e) { setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1, true), new EmptyBorder(5, 15, 5, 15))); }
                public void mouseExited(MouseEvent e) { setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(5, 15, 5, 15))); }
            });
        }
        public java.time.LocalDateTime getDateTime() { return selectedDateTime; }
        private void hienThiPopup() {
            if (popup != null && popup.isVisible()) { popup.setVisible(false); return; }
            popup = new JPopupMenu(); popup.setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1)); popup.setBackground(Color.WHITE);
            JPanel pnlMain = new JPanel(new BorderLayout()); pnlMain.setBackground(Color.WHITE); pnlMain.setBorder(new EmptyBorder(10, 10, 10, 10));
            LichPanel pnlLich = new LichPanel();
            JPanel pnlTime = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); pnlTime.setBackground(Color.WHITE); pnlTime.setBorder(new EmptyBorder(15, 0, 15, 0));
            TimeWheel wheelGio = new TimeWheel(0, 23, java.time.LocalTime.now().getHour());
            TimeWheel wheelPhut = new TimeWheel(0, 59, java.time.LocalTime.now().getMinute());
            JLabel lblHaiCham = new JLabel(":"); lblHaiCham.setFont(fontDam.deriveFont(24f)); lblHaiCham.setForeground(TEXT_MAIN);
            pnlTime.add(wheelGio); pnlTime.add(lblHaiCham); pnlTime.add(wheelPhut);
            NutHienDai btnXacNhan = new NutHienDai("Xong", ACCENT_BLUE, Color.WHITE); btnXacNhan.setPreferredSize(new Dimension(100, 35));
            btnXacNhan.addActionListener(e -> {
                java.time.LocalDate date = pnlLich.getSelectedDate(); if(date == null) date = java.time.LocalDate.now();
                selectedDateTime = java.time.LocalDateTime.of(date, java.time.LocalTime.of(wheelGio.getValue(), wheelPhut.getValue()));
                lblHienThi.setText("📅 " + selectedDateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                lblHienThi.setForeground(TEXT_MAIN); popup.setVisible(false);
            });
            pnlMain.add(pnlLich, BorderLayout.NORTH); pnlMain.add(pnlTime, BorderLayout.CENTER); pnlMain.add(btnXacNhan, BorderLayout.SOUTH);
            popup.add(pnlMain); popup.show(this, 0, getHeight());
        }
    }

    class TimeWheel extends JPanel {
        private int min, max, current;
        public TimeWheel(int min, int max, int current) {
            this.min = min; this.max = max; this.current = current; setPreferredSize(new Dimension(50, 90)); setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseWheelListener(e -> { if (e.getWheelRotation() > 0) cuonLen(); else cuonXuong(); });
            addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { if (e.getY() < getHeight()/3) cuonXuong(); else if (e.getY() > getHeight()*2/3) cuonLen(); }});
        }
        private void cuonLen() { current++; if(current > max) current = min; repaint(); }
        private void cuonXuong() { current--; if(current < min) current = max; repaint(); }
        public int getValue() { return current; }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight(); int w = getWidth(); int prev = current - 1; if (prev < min) prev = max; int next = current + 1; if (next > max) next = min;
            g2.setFont(fontChinh.deriveFont(18f)); g2.setColor(new Color(203, 213, 225)); drawCenteredString(g2, String.format("%02d", prev), w, h/4);
            g2.setFont(fontDam.deriveFont(28f)); g2.setColor(TEXT_MAIN); drawCenteredString(g2, String.format("%02d", current), w, h/2);
            g2.setFont(fontChinh.deriveFont(18f)); g2.setColor(new Color(203, 213, 225)); drawCenteredString(g2, String.format("%02d", next), w, h*3/4); g2.dispose();
        }
        private void drawCenteredString(Graphics2D g2, String text, int width, int yCenter) {
            FontMetrics metrics = g2.getFontMetrics(g2.getFont()); int x = (width - metrics.stringWidth(text)) / 2; int y = yCenter - metrics.getHeight() / 2 + metrics.getAscent(); g2.drawString(text, x, y);
        }
    }

    class LichPanel extends JPanel {
        private java.time.YearMonth currentMonth; private java.time.LocalDate selectedDate; private JLabel lblThangNam; private JPanel pnlNgay;
        public LichPanel() {
            setLayout(new BorderLayout(0, 10)); setOpaque(false); currentMonth = java.time.YearMonth.now(); selectedDate = java.time.LocalDate.now();
            JPanel pnlHeader = new JPanel(new BorderLayout()); pnlHeader.setOpaque(false);
            JButton btnPrev = taoNutDieuHuong("<"); btnPrev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); renderLich(); });
            JButton btnNext = taoNutDieuHuong(">"); btnNext.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); renderLich(); });
            lblThangNam = new JLabel("", SwingConstants.CENTER); lblThangNam.setForeground(ACCENT_BLUE); lblThangNam.setFont(fontDam);
            pnlHeader.add(btnPrev, BorderLayout.WEST); pnlHeader.add(lblThangNam, BorderLayout.CENTER); pnlHeader.add(btnNext, BorderLayout.EAST);
            pnlNgay = new JPanel(new GridLayout(0, 7, 2, 2)); pnlNgay.setOpaque(false);
            add(pnlHeader, BorderLayout.NORTH); add(pnlNgay, BorderLayout.CENTER); renderLich();
        }
        public java.time.LocalDate getSelectedDate() { return selectedDate; }
        private JButton taoNutDieuHuong(String text) { JButton btn = new JButton(text); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setForeground(TEXT_MAIN); btn.setFont(fontDam); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); return btn; }
        private void renderLich() {
            pnlNgay.removeAll(); lblThangNam.setText("Tháng " + currentMonth.getMonthValue() + " - " + currentMonth.getYear());
            String[] thu = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (String t : thu) { JLabel lbl = new JLabel(t, SwingConstants.CENTER); lbl.setForeground(TEXT_MUTED); lbl.setFont(fontChinh.deriveFont(12f)); pnlNgay.add(lbl); }
            java.time.LocalDate firstDay = currentMonth.atDay(1); int dayOfWeek = firstDay.getDayOfWeek().getValue(); int daysInMonth = currentMonth.lengthOfMonth();
            for (int i = 1; i < dayOfWeek; i++) pnlNgay.add(new JLabel("")); 
            for (int i = 1; i <= daysInMonth; i++) {
                int d = i; JButton btnNgay = new JButton(String.valueOf(i)); btnNgay.setFont(fontChinh); btnNgay.setFocusPainted(false); btnNgay.setBorderPainted(false); btnNgay.setCursor(new Cursor(Cursor.HAND_CURSOR));
                if (selectedDate != null && selectedDate.equals(currentMonth.atDay(i))) { btnNgay.setBackground(ACCENT_BLUE); btnNgay.setForeground(Color.WHITE); btnNgay.setOpaque(true); } 
                else { btnNgay.setContentAreaFilled(false); btnNgay.setForeground(TEXT_MAIN); }
                btnNgay.addActionListener(e -> { selectedDate = currentMonth.atDay(d); renderLich(); }); pnlNgay.add(btnNgay);
            }
            pnlNgay.revalidate(); pnlNgay.repaint();
        }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) { e.printStackTrace(); }
            JFrame frame = new JFrame("Enterprise POS - Quản Lý Giảm Giá (Test Độc Lập)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
            frame.setSize(1366, 768); 
            frame.setLocationRelativeTo(null); 
            frame.add(new GiamGiaUI());
            frame.setVisible(true);
            System.out.println("✅ Khởi chạy thành công giao diện Giảm Giá POS!");
        });
    }
}