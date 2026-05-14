package GUI;

import Dao.ConnectDB;
import Data.GiamGia;
import Logic.GiamGiaLogic;
import Logic.TaoMaTuDongLogic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class GiamGiaUI extends JPanel {

    // ================= MÀU SẮC THEME =================
    private final Color BG_CHINH      = new Color(244, 247, 251);
    private final Color BG_CARD       = Color.WHITE;
    private final Color BORDER_COLOR  = new Color(226, 232, 240);
    private final Color TEXT_MAIN     = new Color(30, 41, 59);
    private final Color TEXT_MUTED    = new Color(100, 116, 139);
    private final Color ACCENT_BLUE   = new Color(59, 130, 246);
    private final Color HOVER_COLOR   = new Color(248, 250, 252);

    private final Color ST_SAFE_BG    = new Color(220, 252, 231);
    private final Color ST_SAFE_FG    = new Color(22, 163, 74);
    private final Color ST_WARN_BG    = new Color(254, 243, 199);
    private final Color ST_WARN_FG    = new Color(217, 119, 6);
    private final Color ST_DANGER_BG  = new Color(254, 226, 226);
    private final Color ST_DANGER_FG  = new Color(220, 38, 38);
    private final Color ST_EXPIRED_BG = new Color(225, 29, 72, 30);
    private final Color ST_EXPIRED_FG = new Color(225, 29, 72);

    private Font fontChinh;
    private Font fontDam;

    // ================= CÁC THÀNH PHẦN UI =================
    private JPanel          pnlDanhSachSanPham;
    private JPanel          pnlPhai;
    private CardLayout      cardLayout;
    private JPanel          pnlCards;
    private List<NutTab>    danhSachNutTab      = new ArrayList<>();

    private List<String>        danhSachSPDuocChon  = new ArrayList<>();
    private CheckBoxBoGoc       chkAll;
    private List<CheckBoxBoGoc> danhSachRowCheckboxes = new ArrayList<>();
    private boolean             isUpdatingCheckboxes  = false;

    private TextFieldBoGoc    txtTimKiem;
    private JComboBox<String> cbTrangThai;
    private JComboBox<String> cbNhomHang;

    private TextFieldBoGoc    txtGiam, txtSoLuong;
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
            fontDam   = new Font("Segoe UI", Font.BOLD,  14);
        } catch (Exception e) {
            fontChinh = new Font("SansSerif", Font.PLAIN, 14);
            fontDam   = new Font("SansSerif", Font.BOLD,  14);
        }
    }

    // =========================================================
    // HEADER: THANH LỌC MỀM MẠI + KẾT NỐI SỰ KIỆN TÌM KIẾM
    // =========================================================
    private void KhoiTaoHeader() {
        JPanel pnlHeader = new JPanel(new BorderLayout(20, 0));
        pnlHeader.setOpaque(false);

        JPanel pnlLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlLoc.setOpaque(false);

        // 1. Tạo ô tìm kiếm bo góc có icon kính lúp
        txtTimKiem = new TextFieldBoGoc("Tìm tên, mã SP, mã lô...", true);
        txtTimKiem.setPreferredSize(new Dimension(260, 40));
        
        Timer searchTimer = new Timer(400, e -> TaiDanhSachSanPham()); 
        searchTimer.setRepeats(false);

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchTimer.restart(); }
        });

        // 2. Tạo ComboBox với viền bo tròn nhẹ
        cbTrangThai = TaoComboBox(new String[]{"Tất cả sản phẩm", "⏳ Sắp hết hạn", "Quá hạn", "🔥 Đang Sale"});
        cbNhomHang  = TaoComboBox(new String[]{"Tất cả nhóm hàng", "Thực phẩm", "Đồ uống", "Mì gói", "Hóa mỹ phẩm"});

        cbTrangThai.addActionListener(e -> TaiDanhSachSanPham());
        cbNhomHang.addActionListener(e -> TaiDanhSachSanPham());

        pnlLoc.add(txtTimKiem);
        pnlLoc.add(cbTrangThai);
        pnlLoc.add(cbNhomHang);

        JPanel pnlThongKe = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlThongKe.setOpaque(false);
        pnlThongKe.add(TaoCardThongKe("Tổng SP",   "1,245", ACCENT_BLUE));
        pnlThongKe.add(TaoCardThongKe("Cảnh Báo",  "12",    ST_DANGER_FG));

        pnlHeader.add(pnlLoc,      BorderLayout.WEST);
        pnlHeader.add(pnlThongKe,  BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);
    }

    // =========================================================
    // KHU VỰC DANH SÁCH SẢN PHẨM (CENTER)
    // =========================================================
    private void KhoiTaoKhuVucDanhSach() {
        JPanel pnlCenter = new JPanel(new BorderLayout());
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

        JPanel pnlTieuDe = TaoLayoutDong(
            chkAll,
            TaoLabelHeader("Sản phẩm",   SwingConstants.LEFT),
            TaoLabelHeader("Mã lô",       SwingConstants.CENTER),
            TaoLabelHeader("Hạn sử dụng", SwingConstants.CENTER),
            TaoLabelHeader("Tồn kho",     SwingConstants.CENTER),
            TaoLabelHeader("Giá gốc",     SwingConstants.CENTER),
            TaoLabelHeader("Giảm",        SwingConstants.CENTER),
            TaoLabelHeader("Giá mới",     SwingConstants.CENTER),
            TaoLabelHeader("Trạng thái",  SwingConstants.CENTER),
            TaoLabelHeader("Thao tác",    SwingConstants.CENTER)
        );
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

    // =========================================================
    // TẢI DỮ LIỆU BẰNG LUỒNG NỀN (CHỐNG KHỰNG UI 100%)
    // =========================================================
    public void TaiDanhSachSanPham() {
        if (pnlDanhSachSanPham == null) return;
        
        // 1. Lấy dữ liệu lọc ngay trên luồng UI (ÁP DỤNG THUẬT TOÁN BỎ DẤU TIẾNG VIỆT)
        String tuKhoaRaw = (txtTimKiem != null) ? txtTimKiem.getText() : "";
        final String tuKhoa = tuKhoaRaw.equals("Tìm tên, mã SP, mã lô...") ? "" 
                : GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tuKhoaRaw.trim().toLowerCase());
        final String locTrangThai = (cbTrangThai != null) ? cbTrangThai.getSelectedItem().toString() : "Tất cả sản phẩm";

        // 2. Kích hoạt SwingWorker
        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> listData = new ArrayList<>();
                String sql = "SELECT sp.MaSP, sp.TenSP, ct.MaLoHang, ct.HSD, ct.SoLuongTon, sp.GiaBan, " +
                             "ISNULL(gg.GiamGia, 0) AS GiamGia " +
                             "FROM ChiTietLoHang ct " +
                             "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                             "LEFT JOIN GiamGia gg ON sp.MaSP = gg.MaSP AND gg.TrangThaiGiamGia = N'Đang diễn ra' AND gg.SoLuongApDung > 0 " +
                             "WHERE ct.SoLuongTon > 0 ORDER BY ct.HSD ASC";
                
                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                     java.sql.PreparedStatement ps = con.prepareStatement(sql);
                     java.sql.ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        String maSP   = rs.getString("MaSP");
                        String tenSP  = rs.getString("TenSP");
                        String maLo   = rs.getString("MaLoHang");
                        java.sql.Date hsd = rs.getDate("HSD");
                        int tonKho    = rs.getInt("SoLuongTon");
                        int giaGoc    = rs.getInt("GiaBan");
                        int phanTram  = rs.getInt("GiamGia");

                        String trangThai  = "Còn lâu";
                        long ngayConLai = 0;
                        if (hsd != null) {
                            ngayConLai = ChronoUnit.DAYS.between(java.time.LocalDate.now(), hsd.toLocalDate());
                            if (ngayConLai < 0) trangThai = "Quá hạn";
                            else if (ngayConLai <= 3) trangThai = "Nguy hiểm";
                            else if (ngayConLai <= 15) trangThai = "Sắp hết hạn";
                        }
                        if (phanTram > 0) trangThai = "Đang Sale";

                        // ✨ ÁP DỤNG THUẬT TOÁN TÌM KIẾM TỪ DANHSACHSP (BỎ DẤU TRƯỚC KHI SO SÁNH)
                        if (!tuKhoa.isEmpty()) {
                            String tenSPThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tenSP.toLowerCase());
                            String maSPThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(maSP.toLowerCase());
                            String maLoThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(maLo.toLowerCase());

                            boolean match = tenSPThuong.contains(tuKhoa) || 
                                            maSPThuong.contains(tuKhoa) || 
                                            maLoThuong.contains(tuKhoa);
                            if (!match) continue; // Không khớp thì bỏ qua dòng này
                        }

                        // Lọc trạng thái
                        if (!locTrangThai.equals("Tất cả sản phẩm")) {
                            if (locTrangThai.equals("⏳ Sắp hết hạn") && !trangThai.equals("Sắp hết hạn") && !trangThai.equals("Nguy hiểm")) continue;
                            if (locTrangThai.equals("Quá hạn") && !trangThai.equals("Quá hạn")) continue;
                            if (locTrangThai.equals("🔥 Đang Sale") && !trangThai.equals("Đang Sale")) continue;
                        }
                        
                        listData.add(new Object[]{tenSP, maLo, (int) ngayConLai, tonKho, giaGoc, phanTram, trangThai, maSP});
                    }
                } catch (Exception e) { e.printStackTrace(); }
                return listData;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> data = get();
                    pnlDanhSachSanPham.removeAll();
                    danhSachSPDuocChon.clear();
                    danhSachRowCheckboxes.clear();
                    if (chkAll != null) chkAll.setSelected(false);
                    
                    for (Object[] rowData : data) {
                        pnlDanhSachSanPham.add(TaoDongSanPham(rowData));
                        pnlDanhSachSanPham.add(Box.createRigidArea(new Dimension(0, 8)));
                    }
                    pnlDanhSachSanPham.revalidate();
                    pnlDanhSachSanPham.repaint();
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        worker.execute();
    }

    private JPanel TaoDongSanPham(Object[] data) {
        String tenSP     = (String) data[0];
        String maLo      = (String) data[1];
        int ngayConLai   = (int)    data[2];
        int tonKho       = (int)    data[3];
        int giaGoc       = (int)    data[4];
        int phanTram     = (int)    data[5];
        String trangThai = (String) data[6];
        String maSP      = (String) data[7];

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
                if (danhSachSPDuocChon.size() == danhSachRowCheckboxes.size()) {
                    isUpdatingCheckboxes = true; chkAll.setSelected(true); isUpdatingCheckboxes = false;
                }
            } else {
                danhSachSPDuocChon.remove(maSP);
                isUpdatingCheckboxes = true; chkAll.setSelected(false); isUpdatingCheckboxes = false;
            }
        });

        Color bgStatus, fgStatus;
        if      (ngayConLai < 0)   { bgStatus = ST_EXPIRED_BG; fgStatus = ST_EXPIRED_FG; }
        else if (ngayConLai <= 3)  { bgStatus = ST_DANGER_BG;  fgStatus = ST_DANGER_FG;  }
        else if (ngayConLai <= 15) { bgStatus = ST_WARN_BG;    fgStatus = ST_WARN_FG;    }
        else                       { bgStatus = ST_SAFE_BG;    fgStatus = ST_SAFE_FG;    }

        int giaSauGiam = giaGoc - (giaGoc * phanTram / 100);

        JLabel lblTen    = new JLabel("<html><b>" + tenSP + "</b></html>", SwingConstants.LEFT);
        lblTen.setFont(fontChinh); lblTen.setForeground(TEXT_MAIN);

        JLabel lblMa     = new JLabel(maLo, SwingConstants.CENTER);
        lblMa.setFont(fontChinh); lblMa.setForeground(TEXT_MUTED);

        JLabel lblHSD    = new JLabel(ngayConLai < 0 ? "Quá hạn" : ngayConLai + " ngày", SwingConstants.CENTER);
        lblHSD.setFont(fontDam); lblHSD.setForeground(fgStatus);

        JLabel lblTon    = new JLabel(String.valueOf(tonKho), SwingConstants.CENTER);
        lblTon.setFont(fontChinh); lblTon.setForeground(TEXT_MAIN);

        JLabel lblGiaGoc = new JLabel(String.format("%,d ₫", giaGoc), SwingConstants.CENTER);
        lblGiaGoc.setFont(fontChinh); lblGiaGoc.setForeground(TEXT_MUTED);
        if (phanTram > 0)
            lblGiaGoc.setText("<html><strike>" + String.format("%,d ₫", giaGoc) + "</strike></html>");

        JLabel lblGiam   = new JLabel(phanTram > 0 ? "-" + phanTram + "%" : "-", SwingConstants.CENTER);
        lblGiam.setFont(fontDam); lblGiam.setForeground(ST_DANGER_FG);

        JLabel lblGiaMoi = new JLabel(String.format("%,d ₫", giaSauGiam), SwingConstants.CENTER);
        lblGiaMoi.setFont(fontDam); lblGiaMoi.setForeground(ACCENT_BLUE);

        BadgeLabel badgeStatus = new BadgeLabel(trangThai, bgStatus, fgStatus);
        JPanel pnlBadgeWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        pnlBadgeWrap.setOpaque(false); pnlBadgeWrap.add(badgeStatus);

        JComponent cmpThaoTac;
        if (phanTram > 0) {
            NutHienDai btnHuy = new NutHienDai("Hủy", ST_DANGER_BG, ST_DANGER_FG);
            btnHuy.setFont(fontDam.deriveFont(11f));
            btnHuy.setPreferredSize(new Dimension(80, 30));
            btnHuy.addActionListener(e -> {
                int cf = JOptionPane.showConfirmDialog(this,
                    "Kết thúc ngay chương trình giảm giá của: " + tenSP + "?",
                    "Xác nhận Hủy", JOptionPane.YES_NO_OPTION);
                if (cf == JOptionPane.YES_OPTION) {
                    try {
                        new GiamGiaLogic().huyGiamGia(maSP);
                        TaiDanhSachSanPham();
                    } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage()); }
                }
            });
            JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
            pnlBtn.setOpaque(false); pnlBtn.add(btnHuy); cmpThaoTac = pnlBtn;
        } else {
            cmpThaoTac = new JLabel("");
        }

        JPanel pnlData = TaoLayoutDong(chkChon, lblTen, lblMa, lblHSD, lblTon, lblGiaGoc, lblGiam, lblGiaMoi, pnlBadgeWrap, cmpThaoTac);
        row.add(pnlData, BorderLayout.CENTER);

        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(HOVER_COLOR); }
            public void mouseExited (MouseEvent e) { row.setBackground(BG_CARD); }
            public void mouseClicked(MouseEvent e) { chkChon.setSelected(!chkChon.isSelected()); }
        });

        return row;
    }

    private JPanel TaoLayoutDong(JComponent chk, JComponent c1, JComponent c2, JComponent c3, JComponent c4, JComponent c5, JComponent c6, JComponent c7, JComponent c8, JComponent c9) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH; gbc.weighty = 1.0;

        row.add(wrapGrid(chk), taoGBC(gbc, 0, 0.04));
        row.add(wrapGrid(c1),  taoGBC(gbc, 1, 0.18));
        row.add(wrapGrid(c2),  taoGBC(gbc, 2, 0.10));
        row.add(wrapGrid(c3),  taoGBC(gbc, 3, 0.11));
        row.add(wrapGrid(c4),  taoGBC(gbc, 4, 0.07));
        row.add(wrapGrid(c5),  taoGBC(gbc, 5, 0.10));
        row.add(wrapGrid(c6),  taoGBC(gbc, 6, 0.08));
        row.add(wrapGrid(c7),  taoGBC(gbc, 7, 0.10));
        row.add(wrapGrid(c8),  taoGBC(gbc, 8, 0.12));
        row.add(wrapGrid(c9),  taoGBC(gbc, 9, 0.10));
        return row;
    }

    private GridBagConstraints taoGBC(GridBagConstraints gbc, int x, double weightx) {
        gbc.gridx = x; gbc.weightx = weightx; return gbc;
    }

    private JPanel wrapGrid(JComponent comp) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 40)); p.add(comp, BorderLayout.CENTER); return p;
    }

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
        pnlCards   = new JPanel(cardLayout);
        pnlCards.setOpaque(false);
        pnlCards.add(TaoTabThuCong(), "Thủ công");
        pnlCards.add(TaoTabTuDong(),  "Tự động");

        TaoNutChuyenTab(pnlTabs, "Thủ công", true);
        TaoNutChuyenTab(pnlTabs, "Tự động",  false);

        pnlCenter.add(pnlTabs,   BorderLayout.NORTH);
        pnlCenter.add(pnlCards,  BorderLayout.CENTER);

        container.add(pnlCenter, BorderLayout.CENTER);
        pnlPhai.add(container,   BorderLayout.CENTER);
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

        JLabel lblHuongDan = new JLabel("<html><i>Tick chọn sản phẩm bên trái để áp dụng mức giảm giá.</i></html>");
        lblHuongDan.setFont(fontChinh.deriveFont(12f)); lblHuongDan.setForeground(TEXT_MUTED);
        lblHuongDan.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblHuongDan); p.add(Box.createVerticalStrut(15));

        txtGiam    = new TextFieldBoGoc("Nhập mức giảm (%)", false);
        txtSoLuong = new TextFieldBoGoc("Số lượng mã tối đa", false);
        txtBatDau  = new ChonNgayGioCustom("Chọn ngày giờ bắt đầu...");
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
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()); }
        });

        for (JComponent comp : new JComponent[]{txtGiam, txtSoLuong, txtBatDau, txtKetThuc, btnApDung, btnHuy}) {
            comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        AddLabel(p, "Mức giảm giá (%):"); p.add(txtGiam);    p.add(Box.createVerticalStrut(10));
        AddLabel(p, "Số lượng áp dụng:"); p.add(txtSoLuong); p.add(Box.createVerticalStrut(10));
        AddLabel(p, "Ngày bắt đầu:");     p.add(txtBatDau);  p.add(Box.createVerticalStrut(10));
        AddLabel(p, "Ngày kết thúc:");    p.add(txtKetThuc); p.add(Box.createVerticalStrut(20));
        p.add(btnApDung); p.add(Box.createVerticalStrut(8)); p.add(btnHuy);

        return p;
    }

    private JPanel TaoTabTuDong() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false);

        JLabel lblInfo = new JLabel("<html><b>Hệ Thống Phân Tích Tự Động</b><br><br>"
            + "Thuật toán quét toàn bộ danh sách, phân tích Số ngày còn lại & Tồn kho "
            + "để cấu hình mức xả hàng tối ưu.<br><br>"
            + "<i>• Dưới 3 ngày → Đề xuất ~40%<br>• Dưới 7 ngày → Đề xuất ~25%</i></html>");
        lblInfo.setFont(fontChinh); lblInfo.setForeground(TEXT_MUTED); lblInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlSliderWrap = new JPanel(new BorderLayout(0, 10));
        pnlSliderWrap.setOpaque(false); pnlSliderWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        pnlSliderWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlSliderHeader = new JPanel(new BorderLayout()); pnlSliderHeader.setOpaque(false);

        JLabel lblGiaTriRealtime = new JLabel("Tiêu chuẩn (60%)");
        lblGiaTriRealtime.setFont(fontDam); lblGiaTriRealtime.setForeground(ACCENT_BLUE);

        pnlSliderHeader.add(TaoLabelForm("Mức độ ưu tiên xả kho:"), BorderLayout.WEST);
        pnlSliderHeader.add(lblGiaTriRealtime,                        BorderLayout.EAST);

        JSlider slider = new JSlider(0, 100, 60);
        slider.setOpaque(false); slider.setMajorTickSpacing(50); slider.setMinorTickSpacing(10);
        slider.setPaintTicks(true); slider.setPaintLabels(true);

        java.util.Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();
        labelTable.put(0,   new JLabel("Giữ giá"));
        labelTable.put(50,  new JLabel("Tiêu chuẩn"));
        labelTable.put(100, new JLabel("Xả lỗ"));
        slider.setLabelTable(labelTable);

        slider.addChangeListener(e -> {
            int val = slider.getValue();
            if      (val <= 30) { lblGiaTriRealtime.setText("Thận trọng ("   + val + "%)"); lblGiaTriRealtime.setForeground(ST_SAFE_FG);  }
            else if (val <= 75) { lblGiaTriRealtime.setText("Tiêu chuẩn ("   + val + "%)"); lblGiaTriRealtime.setForeground(ACCENT_BLUE); }
            else                { lblGiaTriRealtime.setText("Xả gấp ("        + val + "%)"); lblGiaTriRealtime.setForeground(ST_DANGER_FG);}
        });

        pnlSliderWrap.add(pnlSliderHeader, BorderLayout.NORTH);
        pnlSliderWrap.add(slider,           BorderLayout.CENTER);

        NutHienDai btnKichHoat = new NutHienDai("✨ Kích Hoạt Quét Tự Động", ST_SAFE_FG, Color.WHITE);
        btnKichHoat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnKichHoat.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnKichHoat.addActionListener(e -> TuDongDeXuatMucGiam());

        p.add(lblInfo); p.add(Box.createVerticalStrut(25));
        p.add(pnlSliderWrap); p.add(Box.createVerticalStrut(30)); p.add(btnKichHoat);
        return p;
    }

    private void KhoiTaoPanelDuoi() {
        JPanel pnlBottom = new JPanel(new BorderLayout()); pnlBottom.setOpaque(false);
        pnlBottom.setBorder(new EmptyBorder(15, 0, 0, 0));

        PanelBoGoc container = new PanelBoGoc(15, BG_CARD);
        container.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));

        JLabel lbl = new JLabel("💡 Gợi ý: Chọn sản phẩm quá hạn hoặc cận date và áp dụng quét Khuyến mãi để xả hàng.");
        lbl.setFont(fontChinh); lbl.setForeground(ST_WARN_FG); container.add(lbl);

        pnlBottom.add(container, BorderLayout.CENTER); add(pnlBottom, BorderLayout.SOUTH);
    }

    private void ApDungGiamGia() {
        if (danhSachSPDuocChon.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng tick chọn ít nhất 1 sản phẩm!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if(txtGiam.getText().isEmpty() || txtSoLuong.getText().isEmpty()){
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ mức giảm và số lượng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            BigDecimal mucGiam = new BigDecimal(txtGiam.getText().trim());
            int soLuong = Integer.parseInt(txtSoLuong.getText().trim());
            LocalDateTime batDau  = txtBatDau.getDateTime();
            LocalDateTime ketThuc = txtKetThuc.getDateTime();

            if (batDau == null || ketThuc == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ ngày và giờ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            GiamGiaLogic logic = new GiamGiaLogic();
            List<GiamGia> dsGiamGiaMoi = new ArrayList<>();

            String maGoc = TaoMaTuDongLogic.taoMaGiamGia();
            int soHienTai = Integer.parseInt(maGoc.substring(2));

            for (String maSP : danhSachSPDuocChon) {
                GiamGia gg = new GiamGia();
                gg.setMaGiamGia("GG" + String.format("%03d", soHienTai++));
                gg.setMaSP(maSP);
                gg.setBatDau(batDau);
                gg.setKetThuc(ketThuc);
                gg.setGiamGia(mucGiam);
                gg.setLoaiGiamGia("Tự Nhập");
                gg.setTrangThaiGiamGia("Đang diễn ra");
                gg.setSoLuongApDung(soLuong);
                dsGiamGiaMoi.add(gg);
            }

            String ketQua = logic.themGiamGiaHangLoat(dsGiamGiaMoi);
            JOptionPane.showMessageDialog(this, ketQua, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            TaiDanhSachSanPham();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi nhập liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void TuDongDeXuatMucGiam() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Hệ thống AI sẽ quét toàn bộ kho và đề xuất mức xả hàng tối ưu.\nBạn có chắc chắn?",
            "Xác nhận Quét Tự Động", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            GiamGiaLogic logic = new GiamGiaLogic();
            List<GiamGia> dsGiamGiaMoi = new ArrayList<>();

            List<Object[]> danhSachKho = Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuQuetKhoAISieuToc();

            String maGoc = TaoMaTuDongLogic.taoMaGiamGia();
            int soHienTai = Integer.parseInt(maGoc.substring(2));

            LocalDateTime now = LocalDateTime.now();

            for (Object[] row : danhSachKho) {
                String maSP   = (String)       row[0];
                String maLoai = (String)        row[1];
                java.sql.Date sqlHSD  = (java.sql.Date) row[2];
                int tonKho    = (int)           row[3];
                double giaNhap = (double)       row[4];
                double giaBan  = (double)       row[5];
                java.sql.Date sqlNhap = (java.sql.Date) row[6];

                java.time.LocalDate hsd         = (sqlHSD  != null) ? sqlHSD.toLocalDate()  : null;
                java.time.LocalDate ngayNhapKho = (sqlNhap != null) ? sqlNhap.toLocalDate() : null;

                Logic.KhuyenMai.DiscountEngine.KetQuaGiamGia ketQuaAI =
                    logic.tinhGiaGiamTuDongSieuThi(maSP, maLoai, tonKho, giaNhap, giaBan, ngayNhapKho, hsd);

                if (ketQuaAI.phanTramGiamCuoiCung > 0) {
                    GiamGia gg = new GiamGia();
                    gg.setMaGiamGia("GG" + String.format("%03d", soHienTai++));
                    gg.setMaSP(maSP);
                    gg.setBatDau(now);
                    LocalDateTime ketThuc = (hsd != null) ? hsd.atTime(23, 59, 59) : now.plusDays(3);
                    gg.setKetThuc(ketThuc);
                    BigDecimal mucGiam = new BigDecimal(ketQuaAI.phanTramGiamCuoiCung * 100).setScale(0, java.math.RoundingMode.HALF_UP);
                    gg.setGiamGia(mucGiam);
                    gg.setLoaiGiamGia("Tự động");
                    gg.setTrangThaiGiamGia("Đang diễn ra");
                    gg.setSoLuongApDung(tonKho);
                    dsGiamGiaMoi.add(gg);
                }
            }

            if (dsGiamGiaMoi.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kho hàng đang an toàn. Không có sản phẩm nào cần xả kho lúc này!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String ketQua = logic.themGiamGiaHangLoat(dsGiamGiaMoi);
            JOptionPane.showMessageDialog(this, ketQua, "Kết quả Quét Tự Động", JOptionPane.INFORMATION_MESSAGE);
            TaiDanhSachSanPham();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi quét tự động AI: " + ex.getMessage(), "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel TaoLabelForm(String text) {
        JLabel lbl = new JLabel(text); lbl.setFont(fontDam); lbl.setForeground(TEXT_MAIN); return lbl;
    }

    private void AddLabel(JPanel p, String text) {
        JLabel lbl = TaoLabelForm(text); lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl); p.add(Box.createVerticalStrut(5));
    }

    private JLabel TaoLabelHeader(String text, int alignment) {
        JLabel lbl = new JLabel(text, alignment); lbl.setFont(fontDam.deriveFont(13f));
        lbl.setForeground(TEXT_MUTED); return lbl;
    }

    // Tự thiết kế lại ComboBox cho mềm mại và đẹp mắt
    private JComboBox<String> TaoComboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(fontChinh);
        cb.setBackground(Color.WHITE);
        cb.setForeground(TEXT_MAIN);
        cb.setPreferredSize(new Dimension(180, 40));
        // Viền bo tròn nhẹ bằng CompoundBorder
        cb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(5, 10, 5, 10)));
        cb.setFocusable(false);
        cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return cb;
    }

    private JPanel TaoCardThongKe(String title, String value, Color accentColor) {
        PanelBoGoc card = new PanelBoGoc(12, BG_CARD);
        card.setLayout(new GridLayout(2, 1));
        card.setBorder(new EmptyBorder(8, 15, 8, 15));
        card.setPreferredSize(new Dimension(130, 60));
        JLabel lblTitle = new JLabel(title, SwingConstants.RIGHT);
        lblTitle.setFont(fontChinh.deriveFont(12f)); lblTitle.setForeground(TEXT_MUTED);
        JLabel lblVal = new JLabel(value, SwingConstants.RIGHT);
        lblVal.setFont(fontDam.deriveFont(20f)); lblVal.setForeground(accentColor);
        card.add(lblTitle); card.add(lblVal);
        return card;
    }

    private void TuyChinhScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            protected void configureScrollBarColors() { this.thumbColor = new Color(203, 213, 225); this.trackColor = BG_CHINH; }
            protected JButton createDecreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
            protected JButton createIncreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
    }

    // =========================================================
    // ✨ CLASS MỚI: TEXT FIELD BO GÓC THÔNG MINH
    // =========================================================
    class TextFieldBoGoc extends JTextField {
        private String hint;
        private boolean hasIcon;

        public TextFieldBoGoc(String hint, boolean hasIcon) {
            this.hint = hint;
            this.hasIcon = hasIcon;
            setOpaque(false);
            setFont(fontChinh);
            setForeground(TEXT_MAIN);
            // Thụt lề trái nếu có icon kính lúp
            int paddingLeft = hasIcon ? 35 : 15;
            setBorder(new EmptyBorder(5, paddingLeft, 5, 15));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Vẽ nền trắng bo góc
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

            // Vẽ viền (Đổi màu Xanh dương khi click vào)
            if (hasFocus()) {
                g2.setColor(ACCENT_BLUE);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

            // Tự vẽ icon kính lúp nếu được yêu cầu
            if (hasIcon) {
                g2.setColor(TEXT_MUTED);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(12, getHeight() / 2 - 6, 10, 10);
                g2.drawLine(20, getHeight() / 2 + 2, 24, getHeight() / 2 + 6);
            }

            super.paintComponent(g);

            // Vẽ chữ chìm (Placeholder) cực chuẩn, không làm sai lệch kết quả getText()
            if (getText().isEmpty()) {
                g2.setColor(new Color(148, 163, 184)); // Lighter text cho placeholder
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, hasIcon ? 35 : 15, y);
            }
            g2.dispose();
        }
    }

    // =========================================================
    // CÁC CLASS ĐỒ HOẠ CUSTOM (GIỮ NGUYÊN)
    // =========================================================
    class CheckBoxBoGoc extends JCheckBox {
        public CheckBoxBoGoc() {
            setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
            setIcon(new CustomIcon(false)); setSelectedIcon(new CustomIcon(true));
        }
        class CustomIcon implements Icon {
            private boolean isSelected;
            public CustomIcon(boolean isSelected) { this.isSelected = isSelected; }
            public int getIconWidth()  { return 22; } public int getIconHeight() { return 22; }
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
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER_COLOR); g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.setColor(bgColor); g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, radius, radius));
            g2.dispose();
        }
    }

    class NutTab extends JButton {
        private boolean isActive = false;
        public NutTab(String text) {
            super(text); setFont(fontDam.deriveFont(13f)); setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); CapNhatMauSac();
            addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { if (!isActive) setForeground(ACCENT_BLUE); } public void mouseExited (MouseEvent e) { CapNhatMauSac(); } });
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
            addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { bgColor = bg.darker(); repaint(); } public void mouseExited (MouseEvent e) { bgColor = bg; repaint(); } });
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

    class ChonNgayGioCustom extends JPanel {
        private JLabel lblHienThi; private java.time.LocalDateTime selectedDateTime; private JPopupMenu popup;
        public ChonNgayGioCustom(String placeholder) {
            setLayout(new BorderLayout()); setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(5, 15, 5, 15)));
            lblHienThi = new JLabel("📅 " + placeholder); lblHienThi.setForeground(TEXT_MUTED); lblHienThi.setFont(fontChinh);
            add(lblHienThi, BorderLayout.CENTER); setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked (MouseEvent e) { hienThiPopup(); }
                public void mouseEntered(MouseEvent e) { setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1, true), new EmptyBorder(5, 15, 5, 15))); }
                public void mouseExited (MouseEvent e) { setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(5, 15, 5, 15))); }
            });
        }
        public java.time.LocalDateTime getDateTime() { return selectedDateTime; }
        private void hienThiPopup() {
            if (popup != null && popup.isVisible()) { popup.setVisible(false); return; }
            popup = new JPopupMenu(); popup.setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1)); popup.setBackground(Color.WHITE);
            JPanel pnlMain = new JPanel(new BorderLayout()); pnlMain.setBackground(Color.WHITE); pnlMain.setBorder(new EmptyBorder(10, 10, 10, 10));
            LichPanel pnlLich = new LichPanel();
            JPanel pnlTime = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); pnlTime.setBackground(Color.WHITE); pnlTime.setBorder(new EmptyBorder(15, 0, 15, 0));
            TimeWheel wheelGio  = new TimeWheel(0, 23, java.time.LocalTime.now().getHour());
            TimeWheel wheelPhut = new TimeWheel(0, 59, java.time.LocalTime.now().getMinute());
            JLabel lblHaiCham   = new JLabel(":"); lblHaiCham.setFont(fontDam.deriveFont(24f)); lblHaiCham.setForeground(TEXT_MAIN);
            pnlTime.add(wheelGio); pnlTime.add(lblHaiCham); pnlTime.add(wheelPhut);
            NutHienDai btnXacNhan = new NutHienDai("Xong", ACCENT_BLUE, Color.WHITE); btnXacNhan.setPreferredSize(new Dimension(100, 35));
            btnXacNhan.addActionListener(e -> {
                java.time.LocalDate date = pnlLich.getSelectedDate(); if (date == null) date = java.time.LocalDate.now();
                selectedDateTime = java.time.LocalDateTime.of(date, java.time.LocalTime.of(wheelGio.getValue(), wheelPhut.getValue()));
                lblHienThi.setText("📅 " + selectedDateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))); lblHienThi.setForeground(TEXT_MAIN); popup.setVisible(false);
            });
            pnlMain.add(pnlLich, BorderLayout.NORTH); pnlMain.add(pnlTime, BorderLayout.CENTER); pnlMain.add(btnXacNhan, BorderLayout.SOUTH); popup.add(pnlMain); popup.show(this, 0, getHeight());
        }
    }

    class TimeWheel extends JPanel {
        private int min, max, current;
        public TimeWheel(int min, int max, int current) {
            this.min = min; this.max = max; this.current = current; setPreferredSize(new Dimension(50, 90)); setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseWheelListener(e -> { if (e.getWheelRotation() > 0) cuonLen(); else cuonXuong(); });
            addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { if (e.getY() < getHeight() / 3) cuonXuong(); else if (e.getY() > getHeight() * 2 / 3) cuonLen(); } });
        }
        private void cuonLen()   { current++; if (current > max) current = min; repaint(); }
        private void cuonXuong() { current--; if (current < min) current = max; repaint(); }
        public int getValue() { return current; }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight(), w = getWidth(); int prev = current - 1; if (prev < min) prev = max; int next = current + 1; if (next > max) next = min;
            g2.setFont(fontChinh.deriveFont(18f)); g2.setColor(new Color(203, 213, 225)); drawCenteredString(g2, String.format("%02d", prev), w, h / 4);
            g2.setFont(fontDam.deriveFont(28f)); g2.setColor(TEXT_MAIN); drawCenteredString(g2, String.format("%02d", current), w, h / 2);
            g2.setFont(fontChinh.deriveFont(18f)); g2.setColor(new Color(203, 213, 225)); drawCenteredString(g2, String.format("%02d", next), w, h * 3 / 4);
            g2.dispose();
        }
        private void drawCenteredString(Graphics2D g2, String text, int width, int yCenter) {
            FontMetrics m = g2.getFontMetrics(g2.getFont()); int x = (width - m.stringWidth(text)) / 2; int y = yCenter - m.getHeight() / 2 + m.getAscent(); g2.drawString(text, x, y);
        }
    }

    class LichPanel extends JPanel {
        private java.time.YearMonth currentMonth; private java.time.LocalDate selectedDate; private JLabel lblThangNam; private JPanel pnlNgay;
        public LichPanel() {
            setLayout(new BorderLayout(0, 10)); setOpaque(false); currentMonth = java.time.YearMonth.now(); selectedDate = java.time.LocalDate.now();
            JPanel pnlHeader = new JPanel(new BorderLayout()); pnlHeader.setOpaque(false);
            JButton btnPrev = taoNutDH("<"); btnPrev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); renderLich(); });
            JButton btnNext = taoNutDH(">"); btnNext.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); renderLich(); });
            lblThangNam = new JLabel("", SwingConstants.CENTER); lblThangNam.setForeground(ACCENT_BLUE); lblThangNam.setFont(fontDam);
            pnlHeader.add(btnPrev, BorderLayout.WEST); pnlHeader.add(lblThangNam, BorderLayout.CENTER); pnlHeader.add(btnNext, BorderLayout.EAST);
            pnlNgay = new JPanel(new GridLayout(0, 7, 2, 2)); pnlNgay.setOpaque(false);
            add(pnlHeader, BorderLayout.NORTH); add(pnlNgay, BorderLayout.CENTER); renderLich();
        }
        public java.time.LocalDate getSelectedDate() { return selectedDate; }
        private JButton taoNutDH(String text) {
            JButton btn = new JButton(text); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setForeground(TEXT_MAIN); btn.setFont(fontDam); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); return btn;
        }
        private void renderLich() {
            pnlNgay.removeAll(); lblThangNam.setText("Tháng " + currentMonth.getMonthValue() + " - " + currentMonth.getYear());
            for (String t : new String[]{"T2","T3","T4","T5","T6","T7","CN"}) { JLabel lbl = new JLabel(t, SwingConstants.CENTER); lbl.setForeground(TEXT_MUTED); lbl.setFont(fontChinh.deriveFont(12f)); pnlNgay.add(lbl); }
            int dayOfWeek = currentMonth.atDay(1).getDayOfWeek().getValue(); int daysInMonth = currentMonth.lengthOfMonth();
            for (int i = 1; i < dayOfWeek; i++) pnlNgay.add(new JLabel(""));
            for (int i = 1; i <= daysInMonth; i++) {
                int d = i; JButton btnNgay = new JButton(String.valueOf(i)); btnNgay.setFont(fontChinh); btnNgay.setFocusPainted(false); btnNgay.setBorderPainted(false); btnNgay.setCursor(new Cursor(Cursor.HAND_CURSOR));
                if (selectedDate != null && selectedDate.equals(currentMonth.atDay(i))) { btnNgay.setBackground(ACCENT_BLUE); btnNgay.setForeground(Color.WHITE); btnNgay.setOpaque(true); } else { btnNgay.setContentAreaFilled(false); btnNgay.setForeground(TEXT_MAIN); }
                btnNgay.addActionListener(e -> { selectedDate = currentMonth.atDay(d); renderLich(); }); pnlNgay.add(btnNgay);
            }
            pnlNgay.revalidate(); pnlNgay.repaint();
        }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on"); System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
            JFrame frame = new JFrame("Test Giảm Giá UI"); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1366, 768); frame.setLocationRelativeTo(null); frame.add(new GiamGiaUI()); frame.setVisible(true);
        });
    }
}