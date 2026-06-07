package GUI;

import GUI.HoTro.MenuSidebarUtil;
import GUI.HoTro.TienIchGiaoDien;
import GUI.ThongKe.KhoPanel;
import Data.LoaiSP;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TrangADMIN extends JFrame {

    // ===================== CARD LAYOUT =====================
    private CardLayout cardLayout;
    private JPanel pnlCards;
    private List<JButton> danhSachNutMenu = new ArrayList<>();

    // ===================== THEME COLORS =====================
    private final Color CLR_SIDEBAR_BG   = new Color(30, 31, 34);
    private final Color CLR_HEADER_BG    = new Color(24, 25, 28);
    private final Color CLR_HOVER        = new Color(45, 90, 140);
    private final Color CLR_ACTIVE       = new Color(22, 119, 190);
    private final Color CLR_SEPARATOR    = new Color(55, 56, 60);
    private final Color CLR_TEXT_PRIMARY = new Color(220, 221, 225);
    private final Color CLR_TEXT_MUTED   = new Color(130, 132, 140);
    private final Color CLR_ACCENT       = new Color(56, 189, 172);
    private final Color CLR_BAN_HANG     = new Color(39, 174, 96);
    private final Color CLR_BAN_HANG_HV  = new Color(33, 150, 83);
    private final Color CLR_LOGOUT       = new Color(192, 57, 43);
    private final Color CLR_LOGOUT_HV    = new Color(231, 76, 60);
    private final Color CLR_CONTENT_BG   = new Color(245, 246, 250);

    private final int SIDEBAR_W = 230;
    private final int ITEM_H    = 40;

    // ===================== LAZY PANELS =====================
    private BanHangUi    banHangUi   = null;
    private ThanhToanUi  thanhToanUi = null;
    private ChiaCaUi     chiaCaUi    = null;
    private TaiKhoanUi   taiKhoanUi  = null;
    private DonHangUi    donHangUi   = null;
    private DanhSachKhUi khachHangUi = null;
    private DanhSachNvUi danhSachNvUi = null;
    private QuanLyNhapHangModule nhapHangModuleUi = null;
    private DanhSachSPUi quanLySpUi = null;
    private CanhBaoKhoPanel canhBaoKhoPanel = null;
    
    private KiemKeGUI kiemKeUi = null;
    private QuanLyGiamGiaModule quanLyGiamGiaModuleUi = null; 
    private TieuHuySanPhamGUI tieuHuyUi = null;
    private GUI.ThongKe.ThongKePanel thongKePanel = null;
    // ===================== KHAI BÁO BIẾN LƯU TRỮ DỮ LIỆU TẢI TRƯỚC =====================
    private final String maNhanVien;
    private final String tenNhanVien;
    private final List<LoaiSP> dsLoaiSP; 
    private final Dao.TruyVanSieuTocDAO.DuLieuBanHangDTO banHangDataCache;
    private final Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO donHangDataCache;

    // =========================================================
    // CONSTRUCTOR "TURBO CHARGED" 🚀
    // =========================================================
    public TrangADMIN(String maNV, String tenNV, List<LoaiSP> dsLoai, 
                      Dao.TruyVanSieuTocDAO.DuLieuBanHangDTO banHangCache, 
                      Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO donHangCache) {
        
        // 1. Nhận toàn bộ dữ liệu đã được DangNhapUi chuẩn bị sẵn (0ms delay)
        this.maNhanVien = maNV;
        this.tenNhanVien = tenNV;           
        this.dsLoaiSP = dsLoai;             
        this.banHangDataCache = banHangCache; 
        this.donHangDataCache = donHangCache; 

        // 2. Bắt đầu vẽ giao diện (Vẽ ngay lập tức vì không phải chờ đợi truy vấn DB)
        setTitle("Phần Mềm Quản Lý Siêu Thị - ADMIN (" + maNV + ")");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1200, 750);
        setLayout(new BorderLayout());

        // ===================== CARDS (content area) =====================
        cardLayout = new CardLayout();
        pnlCards   = new JPanel(cardLayout);
        pnlCards.setBackground(CLR_CONTENT_BG);
        BangLuongUi tabBangLuong = new BangLuongUi();
        pnlCards.add(tabBangLuong, "TAB_BANGLUONG");
        pnlCards.add(taoPanelGiuCho("TRANG CHỦ TỔNG QUAN"),            "TRANG_CHU");
        //pnlCards.add(taoPanelGiuCho("BÁO CÁO THỐNG KÊ DOANH THU"),     "THONG_KE");
        // pnlCards.add(taoPanelGiuCho("QUẢN LÝ SẢN PHẨM"),               "SAN_PHAM");
        // pnlCards.add(taoPanelGiuCho("QUẢN LÝ CHƯƠNG TRÌNH GIẢM GIÁ"),  "GIAM_GIA");
        pnlCards.add(taoPanelGiuCho("QUẢN LÝ NHÀ CUNG CẤP"),           "NHA_CUNG_CAP");
        // pnlCards.add(taoPanelGiuCho("QUẢN LÝ KHÁCH HÀNG THÀNH VIÊN"),  "KHACH_HANG");
        
        // 🔥 ĐÃ TẮT PANEL GIỮ CHỖ CỦA NHÂN VIÊN ĐỂ UI THẬT HIỂN THỊ LÊN
        // pnlCards.add(taoPanelGiuCho("DANH SÁCH NHÂN VIÊN"),            "NHAN_VIEN");      "NHAN_VIEN");

        // ===================== SIDEBAR =====================
        JPanel pnlSidebar = xaySidebar();

        JScrollPane scrollSidebar = new JScrollPane(pnlSidebar);
        scrollSidebar.setBorder(null);
        scrollSidebar.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollSidebar.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSidebar.setPreferredSize(new Dimension(SIDEBAR_W, 0));
        scrollSidebar.getVerticalScrollBar().setUnitIncrement(8);
        TienIchGiaoDien.thietLapThanhCuon(scrollSidebar);

        add(scrollSidebar, BorderLayout.WEST);
        add(pnlCards,      BorderLayout.CENTER);

        // Kích hoạt item đầu tiên
        if (!danhSachNutMenu.isEmpty()) danhSachNutMenu.get(0).doClick();
    }

    // =========================================================
    //  XÂY DỰNG TOÀN BỘ SIDEBAR
    // =========================================================
    private JPanel xaySidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BorderLayout());
        sidebar.setBackground(CLR_SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_W, 0));

        sidebar.add(taoHeader(),    BorderLayout.NORTH);
        sidebar.add(taoMenuChinh(), BorderLayout.CENTER);
        sidebar.add(taoFooter(),    BorderLayout.SOUTH);

        return sidebar;
    }

        // =========================================================
    //  PHẦN 1 — HEADER
    // =========================================================
    private JPanel taoHeader() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBackground(CLR_HEADER_BG);
        pnl.setBorder(new EmptyBorder(20, 16, 16, 16));

        // 🔥 NÂNG CẤP: Gộp Tiêu đề và Chuông vào 1 dòng (BorderLayout)
        JPanel pnlTitleRow = new JPanel(new BorderLayout());
        pnlTitleRow.setOpaque(false);
        pnlTitleRow.setMaximumSize(new Dimension(SIDEBAR_W, 36));
        
        // 🎯 THÊM DÒNG NÀY ĐỂ ÉP TOÀN BỘ HEADER ÉP SÁT LỀ TRÁI CHUẨN XÁC
        pnlTitleRow.setAlignmentX(Component.LEFT_ALIGNMENT); 

        JLabel lblTieuDe = taoLabel("QUẢN LÝ SIÊU THỊ", 13, Font.BOLD, CLR_ACCENT);
        NotificationBell bell = new NotificationBell(); // Gọi chiếc chuông thần thánh
        
        pnlTitleRow.add(lblTieuDe, BorderLayout.WEST);
        pnlTitleRow.add(bell, BorderLayout.EAST);
        
        pnl.add(pnlTitleRow);
        pnl.add(Box.createRigidArea(new Dimension(0, 8))); // Khoảng cách tới đường line

        pnl.add(taoSeparator());
        pnl.add(Box.createRigidArea(new Dimension(0, 12)));

        pnl.add(taoLabel("Xin chào,", 12, Font.PLAIN, CLR_TEXT_MUTED));
        pnl.add(Box.createRigidArea(new Dimension(0, 2)));

        pnl.add(taoLabel(tenNhanVien.toUpperCase(), 16, Font.BOLD, CLR_TEXT_PRIMARY));
        pnl.add(Box.createRigidArea(new Dimension(0, 8)));

        pnl.add(taoBadgeVaiTro("QUẢN TRỊ VIÊN"));

        return pnl;
    }
        // =========================================================
    //  HÀM ĐIỀU HƯỚNG: MỞ TRUNG TÂM CẢNH BÁO
    // =========================================================
    private void moTrangCanhBaoKho() {
        if (canhBaoKhoPanel == null) {
            canhBaoKhoPanel = new CanhBaoKhoPanel((moduleName, identifier) -> {
                
                if ("GiamGia".equals(moduleName)) {
                    taoMucDropdown("Giảm giá", "GIAM_GIA").doClick(); 
                } 
                else if ("TieuHuy".equals(moduleName)) {
                    taoMucDropdown("Tiêu hủy", "TIEU_HUY").doClick(); 
                    if (tieuHuyUi != null) tieuHuyUi.chonNhanhVaSetLyDo(identifier, "Hàng hết hạn");
                }
                else if ("TieuHuyNgam".equals(moduleName)) {
                    if (tieuHuyUi == null) {
                        tieuHuyUi = new TieuHuySanPhamGUI(this.maNhanVien); 
                        pnlCards.add(tieuHuyUi, "TIEU_HUY");
                    }
                    tieuHuyUi.nhanDuLieuChoTieuHuyNgam(identifier, "Chờ tiêu hủy");
                }
                // 🚀 TÍNH NĂNG MỚI: TRUYỀN DỮ LIỆU NGẦM SANG TAB GIẢM GIÁ
                else if ("GiamGiaNgam".equals(moduleName)) {
                    if (quanLyGiamGiaModuleUi == null) {
                        quanLyGiamGiaModuleUi = new QuanLyGiamGiaModule(); 
                        pnlCards.add(quanLyGiamGiaModuleUi, "GIAM_GIA");
                    }
                    if (GiamGiaUI.getInstance() != null) {
                        GiamGiaUI.getInstance().nhanDuLieuChoGiamGiaNgam(identifier);
                    }
                }
                // 🚀 TÍNH NĂNG MỚI: TRUYỀN DỮ LIỆU SANG TAB KIỂM KÊ (FIX LỆCH KHO)
                else if ("KiemKeNgam".equals(moduleName)) {
                    // 1. Gọi lệnh Click ảo để khởi tạo và mở trang Kiểm Kê
                    taoMucDropdown("Kiểm kê kho", "KIEM_KE").doClick();
                    
                    // 2. Tách chuỗi identifier (Ví dụ: "SP001_LH2412-001")
                    String[] parts = identifier.split("_");
                    if (parts.length == 2 && kiemKeUi != null) {
                        // Bắn dữ liệu vào UI Kiểm Kê
                        kiemKeUi.nhanDuLieuCanhBaoLechKho(parts[0], parts[1]);
                    }
                }
                else if ("NhapHangNgay".equals(moduleName)) {
                    // 🔥 SỬA TÊN Ở ĐÂY THÀNH "NHAP_HANG_MODULE"
                    taoMucDropdown("Nhập hàng", "NHAP_HANG_MODULE").doClick();
                    if (nhapHangModuleUi != null) { 
                        nhapHangModuleUi.chuyenDuLieuNhapHangNgay(identifier);
                    }
                }
                // 🚀 TÍNH NĂNG MỚI: NHẬP HÀNG SAU (TẠO POPUP GỢI Ý NGẦM)
                else if ("NhapHangSau".equals(moduleName)) {
                    if (nhapHangModuleUi == null) {
                        nhapHangModuleUi = new QuanLyNhapHangModule();
                        // 🔥 SỬA TÊN Ở ĐÂY THÀNH "NHAP_HANG_MODULE"
                        pnlCards.add(nhapHangModuleUi, "NHAP_HANG_MODULE");
                    }
                    nhapHangModuleUi.chuyenDuLieuNhapHangSau(identifier);
                }
            });
            pnlCards.add(canhBaoKhoPanel, "CANH_BAO");
        }
        
        for (JButton btn : danhSachNutMenu) {
            btn.setBackground(CLR_SIDEBAR_BG);
            btn.setForeground(CLR_TEXT_PRIMARY);
        }
        
        cardLayout.show(pnlCards, "CANH_BAO");
    }

    // =========================================================
    //  PHẦN 2 — MENU CHÍNH
    // =========================================================
    private JPanel taoMenuChinh() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBackground(CLR_SIDEBAR_BG);
        pnl.setBorder(new EmptyBorder(10, 0, 10, 0));

        pnl.add(taoNutBanHang());
        pnl.add(Box.createRigidArea(new Dimension(0, 6)));
        pnl.add(taoSeparatorFull());
        pnl.add(Box.createRigidArea(new Dimension(0, 6)));

        pnl.add(taoNhomMenuHover("  Kho & Thống kê",
            taoMucDropdown("Kiểm kê kho", "KIEM_KE"),
            taoMucDropdown("Thống kê",    "THONG_KE")
        ));

        // 🔥 XÂY DỰNG MENU "QUẢN LÝ" THỦ CÔNG ĐỂ CÓ SUB-MENU SẢN PHẨM 🔥
        JPopupMenu popupQuanLy = taoPopupDark();

        // --- Bắt đầu Sub-menu (Cấp 2) cho Sản phẩm ---
        JMenu menuSanPham = taoJMenuDark("Sản phẩm   ›");
        
        // 🌟 CHIÊU THỨC: ÉP JMENU NHẬN SỰ KIỆN CLICK CHUỘT
        menuSanPham.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    chuyenTrangSanPhamAdmin("ALL"); 
                    popupQuanLy.setVisible(false);  
                }
            }
        });

        JMenuItem itemTatCa = taoMenuItemDark("Tất cả sản phẩm");
        itemTatCa.addActionListener(e -> {
            chuyenTrangSanPhamAdmin("ALL");
            popupQuanLy.setVisible(false); 
        });
        menuSanPham.add(itemTatCa);
        menuSanPham.addSeparator();

        if (this.dsLoaiSP != null) {
            for (Data.LoaiSP loai : this.dsLoaiSP) {
                JMenuItem itemLoai = taoMenuItemDark(loai.getTenLoai());
                itemLoai.addActionListener(e -> {
                    chuyenTrangSanPhamAdmin(loai.getMaLoai()); 
                    popupQuanLy.setVisible(false); 
                });
                menuSanPham.add(itemLoai);
            }
        }
        popupQuanLy.add(menuSanPham);
        // --- Kết thúc Sub-menu Sản phẩm ---

        // Thêm các mục khác vào popup Quản lý như bình thường
        popupQuanLy.add(taoMenuItemTuButton(taoMucDropdown("Giảm giá",     "GIAM_GIA")));
        popupQuanLy.add(taoMenuItemTuButton(taoMucDropdown("Nhập hàng",    "NHAP_HANG_MODULE")));
        popupQuanLy.add(taoMenuItemTuButton(taoMucDropdown("Đơn hàng",     "DON_HANG")));
        popupQuanLy.add(taoMenuItemTuButton(taoMucDropdown("Khách hàng",   "KHACH_HANG")));
        popupQuanLy.add(taoMenuItemTuButton(taoMucDropdown("Nhân viên",    "NHAN_VIEN")));
        popupQuanLy.add(taoMenuItemTuButton(taoMucDropdown("Ca làm",       "CA_LAM")));
        popupQuanLy.add(taoMenuItemTuButton(taoMucDropdown("Tiêu hủy",     "TIEU_HUY")));

        pnl.add(taoSidebarItemVoiPopup("  Quản lý", popupQuanLy));

        pnl.add(taoNhomMenuHover("  Hệ thống",
            taoMucDropdown("Tài khoản", "TAI_KHOAN")
        ));

        return pnl;
    }

    // =========================================================
    //  PHẦN 3 — FOOTER
    // =========================================================
    private JPanel taoFooter() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBackground(CLR_SIDEBAR_BG);
        pnl.setBorder(new EmptyBorder(0, 0, 0, 0));

        pnl.add(taoSeparatorFull(), BorderLayout.NORTH);

        JButton btnDangXuat = new JButton("  Đăng xuất");
        btnDangXuat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnDangXuat.setForeground(new Color(200, 90, 90));
        btnDangXuat.setBackground(CLR_SIDEBAR_BG);
        btnDangXuat.setBorder(new EmptyBorder(0, 16, 0, 16));
        btnDangXuat.setOpaque(true);
        btnDangXuat.setBorderPainted(false);
        btnDangXuat.setFocusPainted(false);
        btnDangXuat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDangXuat.setHorizontalAlignment(SwingConstants.LEFT);
        btnDangXuat.setPreferredSize(new Dimension(SIDEBAR_W, 44));
        btnDangXuat.setMaximumSize(new Dimension(SIDEBAR_W, 44));

        btnDangXuat.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btnDangXuat.setBackground(new Color(80, 30, 30));
                btnDangXuat.setForeground(CLR_LOGOUT_HV);
            }
            @Override public void mouseExited(MouseEvent e) {
                btnDangXuat.setBackground(CLR_SIDEBAR_BG);
                btnDangXuat.setForeground(new Color(200, 90, 90));
            }
        });

        btnDangXuat.addActionListener(e ->
            TienIchGiaoDien.hienThiXacNhan(this, "Bạn có chắc chắn muốn đăng xuất?",
                () -> { this.dispose(); new DangNhapUi().setVisible(true); })
        );

        pnl.add(btnDangXuat, BorderLayout.CENTER);
        return pnl;
    }

    // =========================================================
    //  NÚT BÁN HÀNG — Tích hợp Hover Danh mục hàng hóa
    // =========================================================
    private JPanel taoNutBanHang() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CLR_SIDEBAR_BG);
        wrapper.setBorder(new EmptyBorder(4, 10, 4, 10));
        wrapper.setMaximumSize(new Dimension(SIDEBAR_W, ITEM_H + 10));

        JButton btnRounded = new JButton("  \uD83D\uDED2  BÁN HÀNG   ›") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnRounded.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRounded.setForeground(Color.WHITE);
        btnRounded.setBackground(CLR_BAN_HANG);
        btnRounded.setOpaque(false);
        btnRounded.setContentAreaFilled(false);
        btnRounded.setBorderPainted(false);
        btnRounded.setFocusPainted(false);
        btnRounded.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRounded.setHorizontalAlignment(SwingConstants.LEFT);
        btnRounded.setBorder(new EmptyBorder(0, 16, 0, 16));
        btnRounded.setPreferredSize(new Dimension(SIDEBAR_W - 20, ITEM_H));
        btnRounded.setMaximumSize(new Dimension(SIDEBAR_W - 20, ITEM_H));

        JPopupMenu popup = taoPopupDark();

        JMenuItem itemTatCa = taoMenuItemDark("Tất cả sản phẩm");
        itemTatCa.addActionListener(e -> {
            MenuSidebarUtil.setActiveMenu(danhSachNutMenu, btnRounded);
            initBanHangUiIfNotExists();
            if (banHangUi != null) banHangUi.getPnlDanhSachSP().loadDuLieuSanPham("ALL");
            cardLayout.show(pnlCards, "BAN_HANG");
        });
        popup.add(itemTatCa);
        popup.addSeparator();

        // Render các danh mục
        if (this.dsLoaiSP != null && !this.dsLoaiSP.isEmpty()) {
            for (LoaiSP loai : this.dsLoaiSP) {
                JMenuItem item = taoMenuItemDark(loai.getTenLoai());
                item.addActionListener(e -> {
                    MenuSidebarUtil.setActiveMenu(danhSachNutMenu, btnRounded);
                    initBanHangUiIfNotExists();
                    cardLayout.show(pnlCards, "BAN_HANG");
                    if (banHangUi != null && banHangUi.getPnlDanhSachSP() != null)
                        banHangUi.getPnlDanhSachSP().loadDuLieuSanPham(loai.getMaLoai());
                });
                popup.add(item);
            }
        }

        Timer[] timers = creatHoverTimers(btnRounded, popup);
        Timer timerShow = timers[0];
        Timer timerHide = timers[1];

        btnRounded.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { 
                btnRounded.setBackground(CLR_BAN_HANG_HV); 
                btnRounded.repaint(); 
                timerHide.stop(); 
                timerShow.start(); 
            }
            @Override public void mouseExited(MouseEvent e)  { 
                btnRounded.setBackground(CLR_BAN_HANG);    
                btnRounded.repaint(); 
                timerShow.stop(); 
                timerHide.start(); 
            }
        });

        popup.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { timerHide.stop(); }
            @Override public void mouseExited(MouseEvent e)  { timerHide.start(); }
        });
        for (Component c : popup.getComponents()) {
            c.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { timerHide.stop(); }
                @Override public void mouseExited(MouseEvent e)  { timerHide.start(); }
            });
        }

        btnRounded.addActionListener(e -> {
            MenuSidebarUtil.setActiveMenu(danhSachNutMenu, btnRounded);
            initBanHangUiIfNotExists();
            if (banHangUi != null) banHangUi.getPnlDanhSachSP().loadDuLieuSanPham("ALL");
            cardLayout.show(pnlCards, "BAN_HANG");
        });

        danhSachNutMenu.add(btnRounded);
        wrapper.add(btnRounded, BorderLayout.CENTER);
        return wrapper;
    }

    // =========================================================
    //  TẠO MỤC DROPDOWN (dùng trong popup)
    // =========================================================
    private JButton taoMucDropdown(String title, String cardName) {
        JButton btn = new JButton(title);
        btn.addActionListener(e -> {
            switch (cardName) {
                case "BAN_HANG":
                    initBanHangUiIfNotExists();
                    if (banHangUi != null) banHangUi.getPnlDanhSachSP().loadDuLieuSanPham("ALL");
                    break;
                case "CA_LAM":
                    if (chiaCaUi == null) { 
                        chiaCaUi = new ChiaCaUi(this.maNhanVien, true); // 🔥 true: Bật toàn quyền Quản lý
                        pnlCards.add(chiaCaUi, "CA_LAM"); 
                    }
                    break;
                case "TAI_KHOAN":
                    if (taiKhoanUi == null) { taiKhoanUi = new TaiKhoanUi(maNhanVien); pnlCards.add(taiKhoanUi, "TAI_KHOAN"); }
                    break;
                case "NHAP_HANG_MODULE":
                    if (nhapHangModuleUi == null) {
                        nhapHangModuleUi = new QuanLyNhapHangModule(); 
                        pnlCards.add(nhapHangModuleUi, "NHAP_HANG_MODULE"); 
                        pnlCards.revalidate();
                        pnlCards.repaint();
                    }
                    break;
                case "DON_HANG":
                    if (donHangUi == null) { 
                        donHangUi = new DonHangUi(); 
                        pnlCards.add(donHangUi, "DON_HANG"); 
                        pnlCards.revalidate(); 
                        pnlCards.repaint();
                    } else {
                        donHangUi.taiDuLieuTuDatabase(); 
                    }
                    break;
                case "KHACH_HANG":
                    if (khachHangUi == null) { 
                        khachHangUi = new DanhSachKhUi(); 
                        pnlCards.add(khachHangUi, "KHACH_HANG"); 
                        pnlCards.revalidate(); 
                        pnlCards.repaint();
                    }
                    break;
                case "NHAN_VIEN":
                    if (danhSachNvUi == null) {
                        danhSachNvUi = new DanhSachNvUi();
                        pnlCards.add(danhSachNvUi, "NHAN_VIEN");
                        pnlCards.revalidate();
                        pnlCards.repaint();
                    }
                    break;
                case "SAN_PHAM":
                    if (quanLySpUi == null) {
                        quanLySpUi = new DanhSachSPUi(null, DanhSachSPUi.UIMode.QUAN_LY);
                        pnlCards.add(quanLySpUi, "SAN_PHAM");
                        pnlCards.revalidate();
                        pnlCards.repaint();
                    } else {
                        quanLySpUi.taiDuLieuBanHangSieuToc("ALL");
                    }
                    break;
                    
                // 🔥 ĐÃ SỬA: Khởi tạo QuanLyGiamGiaModule thay vì GiamGiaUI cũ
                case "GIAM_GIA":
                    if (quanLyGiamGiaModuleUi == null) {
                        quanLyGiamGiaModuleUi = new QuanLyGiamGiaModule();
                        pnlCards.add(quanLyGiamGiaModuleUi, "GIAM_GIA");
                        pnlCards.revalidate();
                        pnlCards.repaint();
                    }
                    break;
                case "KIEM_KE":
                    if (kiemKeUi == null) {
                        kiemKeUi = new KiemKeGUI(); 
                        kiemKeUi.setNhanVienTruyenVao(this.maNhanVien, this.tenNhanVien);
                        pnlCards.add(kiemKeUi, "KIEM_KE"); 
                        pnlCards.revalidate();
                        pnlCards.repaint();
                    }
                    break;
                case "TIEU_HUY":
                    if (tieuHuyUi == null) {
                        // Khởi tạo và truyền mã nhân viên hiện tại vào để lưu Database
                        tieuHuyUi = new TieuHuySanPhamGUI(this.maNhanVien); 
                        pnlCards.add(tieuHuyUi, "TIEU_HUY");
                        pnlCards.revalidate();
                        pnlCards.repaint();
                    }
                    break;
                case "THONG_KE":
                    if (thongKePanel == null) {
                        thongKePanel = new GUI.ThongKe.ThongKePanel();

                        // ── KhoPanel callback (đã có) ──
                        if (thongKePanel.getKhoPanel() != null) {
                            thongKePanel.getKhoPanel().setCallback(new GUI.ThongKe.KhoPanel.KhoPanelCallback() {
                                @Override
                                public void moNhapHang() {
                                    SwingUtilities.invokeLater(() ->
                                        taoMucDropdown("Nhập hàng", "NHAP_HANG_MODULE").doClick()
                                    );
                                }
                                @Override
                                public void moKiemKe() {
                                    SwingUtilities.invokeLater(() ->
                                        taoMucDropdown("Kiểm kê kho", "KIEM_KE").doClick()
                                    );
                                }
                            });
                        }

                        // 🔥 THÊM MỚI: NhanVienPanel callback
                        if (thongKePanel.getNhanVienPanel() != null) {
                            thongKePanel.getNhanVienPanel().setCallback(new GUI.ThongKe.NhanVienPanel.NhanVienPanelCallback() {
                                @Override
                                public void moThemNhanVien() {
                                    // Chuyển sang tab Nhân viên (DanhSachNvUi) để thêm mới
                                    SwingUtilities.invokeLater(() ->
                                        taoMucDropdown("Nhân viên", "NHAN_VIEN").doClick()
                                    );
                                }

                                @Override
                                public void moPhanCa() {
                                    // Chuyển sang tab Ca làm (ChiaCaUi)
                                    SwingUtilities.invokeLater(() ->
                                        taoMucDropdown("Ca làm", "CA_LAM").doClick()
                                    );
                                }

                                @Override
                                public void xemHoSoLuong(String maNV) {
                                    // Chuyển sang BangLuongUi với mã NV cụ thể
                                    SwingUtilities.invokeLater(() ->
                                        TrangADMIN.this.chuyenTabGiaoDien("TAB_BANGLUONG")
                                    );
                                }
                            });
                        }

                        pnlCards.add(thongKePanel, "THONG_KE");
                    }

                    cardLayout.show(pnlCards, "THONG_KE");
                    pnlCards.revalidate();
                    pnlCards.repaint();
                    break;
            }
            cardLayout.show(pnlCards, cardName);
        });
        return btn;
    }

    // =========================================================
    //  TẠO NHÓM MENU HOVER DROPDOWN GENERIC
    // =========================================================
    private JPanel taoNhomMenuHover(String tenMuc, JButton... cacNut) {
        JPopupMenu popup = taoPopupDark();
        for (JButton btn : cacNut) {
            JMenuItem item = taoMenuItemDark(btn.getText());
            item.addActionListener(e -> btn.doClick());
            popup.add(item);
        }
        return taoSidebarItemVoiPopup(tenMuc, popup);
    }

    // =========================================================
    //  TẠO SIDEBAR ITEM + GẮN POPUP HOVER 
    // =========================================================
    private JPanel taoSidebarItemVoiPopup(String tenMuc, JPopupMenu popup) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CLR_SIDEBAR_BG);
        wrapper.setMaximumSize(new Dimension(SIDEBAR_W, ITEM_H));
        wrapper.setPreferredSize(new Dimension(SIDEBAR_W, ITEM_H));

        JButton btnHeader = taoSidebarButton(tenMuc + "   ›");

        Timer[] timers = creatHoverTimers(btnHeader, popup);
        Timer timerShow = timers[0];
        Timer timerHide = timers[1];

        btnHeader.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                timerHide.stop(); timerShow.start();
                btnHeader.setBackground(CLR_HOVER); btnHeader.setForeground(Color.WHITE);
            }
            @Override public void mouseExited(MouseEvent e) {
                timerShow.stop(); timerHide.start();
            }
        });

        ganSuKienHoverChoMenu(popup, timerHide);

        wrapper.add(btnHeader, BorderLayout.CENTER);
        return wrapper;
    }

    // 🔥 HÀM ĐỆ QUY GIÚP GIỮ POPUP KHÔNG BỊ TẮT KHI RÊ CHUỘT VÀO MENU CON
    private void ganSuKienHoverChoMenu(Component comp, Timer timerHide) {
        comp.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { timerHide.stop(); }
            @Override public void mouseExited(MouseEvent e)  { timerHide.start(); }
        });

        if (comp instanceof Container) {
            for (Component c : ((Container) comp).getComponents()) {
                ganSuKienHoverChoMenu(c, timerHide);
            }
        }
        
        if (comp instanceof JMenu) {
            ganSuKienHoverChoMenu(((JMenu) comp).getPopupMenu(), timerHide);
        }
    }

    private Timer[] creatHoverTimers(JButton anchor, JPopupMenu popup) {
        Timer timerShow = new Timer(100, e -> {
            if (!popup.isVisible())
                popup.show(anchor, SIDEBAR_W - 8, 0);
        });
        timerShow.setRepeats(false);

        Timer timerHide = new Timer(160, e -> {
            popup.setVisible(false);
            anchor.setBackground(CLR_SIDEBAR_BG);
            anchor.setForeground(CLR_TEXT_PRIMARY);
        });
        timerHide.setRepeats(false);

        return new Timer[]{ timerShow, timerHide };
    }

    // =========================================================
    //  HELPERS — Button, Label, Badge, Separator
    // =========================================================
    private JButton taoSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(CLR_TEXT_PRIMARY);
        btn.setBackground(CLR_SIDEBAR_BG);
        btn.setBorder(new EmptyBorder(0, 16, 0, 12));
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(SIDEBAR_W, ITEM_H));
        btn.setPreferredSize(new Dimension(SIDEBAR_W, ITEM_H));
        return btn;
    }

    private JLabel taoLabel(String text, int size, int style, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", style, size));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel taoBadgeVaiTro(String text) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setBackground(CLR_HEADER_BG);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel badge = new JLabel(" " + text + " ");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(CLR_ACCENT);
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CLR_ACCENT, 1),
            new EmptyBorder(1, 4, 1, 4)
        ));
        wrapper.add(badge);
        return wrapper;
    }

    private JSeparator taoSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(CLR_SEPARATOR);
        sep.setMaximumSize(new Dimension(SIDEBAR_W, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private JPanel taoSeparatorFull() {
        JPanel line = new JPanel();
        line.setBackground(CLR_SEPARATOR);
        line.setMaximumSize(new Dimension(SIDEBAR_W, 1));
        line.setPreferredSize(new Dimension(SIDEBAR_W, 1));
        return line;
    }

    private JPopupMenu taoPopupDark() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(new Color(40, 42, 46));
        popup.setBorder(BorderFactory.createLineBorder(CLR_SEPARATOR, 1));
        return popup;
    }

    private JMenuItem taoMenuItemDark(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.setForeground(CLR_TEXT_PRIMARY);
        item.setBackground(new Color(40, 42, 46));
        item.setOpaque(true);
        item.setPreferredSize(new Dimension(200, 34));
        item.setBorder(new EmptyBorder(0, 18, 0, 16));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.addChangeListener(e -> {
            if (item.isArmed()) {
                item.setBackground(CLR_HOVER);
                item.setForeground(Color.WHITE);
            } else {
                item.setBackground(new Color(40, 42, 46));
                item.setForeground(CLR_TEXT_PRIMARY);
            }
        });
        return item;
    }

    // =========================================================
    //  LAZY INIT BAN HANG
    // =========================================================
    private void initBanHangUiIfNotExists() {
        if (banHangUi == null) {
            banHangUi   = new BanHangUi();
            thanhToanUi = new ThanhToanUi();
            thanhToanUi.setNhanVien(maNhanVien, tenNhanVien);
            setupHanhDongBanHang();
            pnlCards.add(banHangUi,   "BAN_HANG");
            pnlCards.add(thanhToanUi, "THANH_TOAN");
            pnlCards.revalidate();
        }
    }

    private void setupHanhDongBanHang() {
        banHangUi.setHanhDongThanhToan(() -> {
            Object[][] dsSanPham = banHangUi.layDuLieuGioHang();
            BigDecimal tongTien = banHangUi.layTongTienGioHang();
            thanhToanUi.nhanDuLieuTuGioHang(
                dsSanPham, 
                tongTien, 
                banHangUi.isCheDoDoiHang(),       
                banHangUi.getTongTienHoaDonCu()   
            );
            cardLayout.show(pnlCards, "THANH_TOAN");
        });
        thanhToanUi.setHanhDongQuayLai(
            () -> cardLayout.show(pnlCards, "BAN_HANG"));
        thanhToanUi.setHanhDongThanhToanThanhCong(
            () -> { banHangUi.lamMoiToanBoBanHang(); cardLayout.show(pnlCards, "BAN_HANG"); });
    }
    // =========================================================
    //  CÁC HÀM HỖ TRỢ MENU ĐA CẤP (SUB-MENU)
    // =========================================================
    
    // 1. Tạo JMenu 
    private JMenu taoJMenuDark(String text) {
        JMenu menu = new JMenu(text);
        menu.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        menu.setForeground(CLR_TEXT_PRIMARY);
        menu.setBackground(new Color(40, 42, 46));
        menu.setOpaque(true);
        menu.setPreferredSize(new Dimension(200, 34));
        menu.setBorder(new EmptyBorder(0, 18, 0, 16));
        menu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        menu.getPopupMenu().setBackground(new Color(40, 42, 46));
        menu.getPopupMenu().setBorder(BorderFactory.createLineBorder(CLR_SEPARATOR, 1));
        
        menu.addChangeListener(e -> {
            if (menu.isSelected()) {
                menu.setBackground(CLR_HOVER);
                menu.setForeground(Color.WHITE);
            } else {
                menu.setBackground(new Color(40, 42, 46));
                menu.setForeground(CLR_TEXT_PRIMARY);
            }
        });
        return menu;
    }

    // 2. Ép kiểu Button thành JMenuItem 
    private JMenuItem taoMenuItemTuButton(JButton btn) {
        JMenuItem item = taoMenuItemDark(btn.getText());
        item.addActionListener(e -> btn.doClick());
        return item;
    }

    // 3. Hàm logic: Khởi tạo/Chuyển trang 
    private void chuyenTrangSanPhamAdmin(String maLoai) {
        if (quanLySpUi == null) {
            quanLySpUi = new DanhSachSPUi(null, DanhSachSPUi.UIMode.QUAN_LY);
            pnlCards.add(quanLySpUi, "SAN_PHAM");
        }
        cardLayout.show(pnlCards, "SAN_PHAM");
        quanLySpUi.taiDuLieuBanHangSieuToc(maLoai); 
    }
    // =========================================================
    //  PANEL GIỮ CHỖ
    // =========================================================
    private JPanel taoPanelGiuCho(String tieuDe) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBackground(CLR_CONTENT_BG);

        JPanel pnlHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlHeader.setBackground(Color.WHITE);
        pnlHeader.setPreferredSize(new Dimension(0, 60));
        pnlHeader.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel lblTieuDe = new JLabel(tieuDe);
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTieuDe.setForeground(TienIchGiaoDien.MAU_CHU_CHINH);
        pnlHeader.add(lblTieuDe);

        pnl.add(pnlHeader, BorderLayout.NORTH);
        pnl.add(new JLabel("(Khu vực tính năng sẽ nằm ở đây)", SwingConstants.CENTER), BorderLayout.CENTER);
        return pnl;
    }
    // =========================================================
    // 🔔 COMPONENT: CHUÔNG THÔNG BÁO HIỆN ĐẠI (NOTIFICATION HUB)
    // =========================================================
    class NotificationBell extends JPanel {
        private int alertCount = 0;
        private boolean isHovered = false;
        private Timer syncTimer;

        public NotificationBell() {
            setPreferredSize(new Dimension(36, 36));
            setMaximumSize(new Dimension(36, 36));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Hiệu ứng Hover - Đổi màu nền mờ Glassmorphism
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { 
                    isHovered = true; repaint(); 
                }
                @Override public void mouseExited(MouseEvent e) { 
                    isHovered = false; repaint(); 
                }
                @Override public void mouseClicked(MouseEvent e) {
                    moTrangCanhBaoKho();
                }
            });

            // ⏱️ TIMER ĐỒNG BỘ: Mỗi 15 giây quét nhẹ Database 1 lần để đếm cảnh báo
            syncTimer = new Timer(15000, e -> refreshNotificationsAsync());
            syncTimer.start();
            
            // Chạy ngay lần đầu khi mở app
            refreshNotificationsAsync();
        }

        // Cập nhật số lượng và gọi UI vẽ lại
        public void setNotificationCount(int count) {
            if (this.alertCount != count) {
                this.alertCount = count;
                repaint();
            }
        }

        // Kéo dữ liệu ngầm không làm đơ UI (Tận dụng hàm KiemKeSieuToc đã có)
        private void refreshNotificationsAsync() {
            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() {
                    int count = 0;
                    try {
                        // === BƯỚC 0: Lấy danh sách lô đã kiểm tra hoàn tất (giống CanhBaoKhoPanel) ===
                        java.util.Set<String> dsLoaDaKiemTra = new java.util.HashSet<>();
                        String sqlDaKiem = "SELECT DISTINCT MaLoHang FROM KiemKeKho " +
                                        "WHERE LyDo LIKE N'Đã kiểm tra %'";
                        try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                            java.sql.Statement stKiem = con.createStatement();
                            java.sql.ResultSet rsKiem = stKiem.executeQuery(sqlDaKiem)) {
                            while (rsKiem.next()) {
                                dsLoaDaKiemTra.add(rsKiem.getString("MaLoHang"));
                            }
                        } catch (Exception ignored) {}
        
                        // === BƯỚC 1: Lấy danh sách SP đang giảm giá (giống CanhBaoKhoPanel) ===
                        java.util.Set<String> dsDangGiamGia = Dao.TruyVanSieuTocDAO.getInstance()
                                .getTapHopSanPhamDangGiamGia();
        
                        // === BƯỚC 2: Quét HSD & Tồn thấp (đồng bộ logic với taiDuLieuThucTeTuKho) ===
                        Dao.TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO duLieu =
                                Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuKiemKeSieuToc();
                        java.time.LocalDate today = java.time.LocalDate.now();
        
                        for (Data.SanPham sp : duLieu.dsSanPham) {
                            java.util.List<Data.ChiTietLoHang> dsLo = duLieu.mapDanhSachLo.get(sp.getMaSP());
                            if (dsLo == null) continue;
        
                            for (Data.ChiTietLoHang lo : dsLo) {
                                int tonKho = lo.getSoLuongTon();
                                if (tonKho <= 0) continue;
        
                                // Bỏ qua lô đã kiểm tra hoàn tất
                                if (dsLoaDaKiemTra.contains(lo.getMaLoHang())) continue;
        
                                boolean daCanhBaoDate = false;
        
                                if (lo.getHSD() != null) {
                                    long daysBetween = java.time.temporal.ChronoUnit.DAYS
                                            .between(today, lo.getHSD());
        
                                    if (daysBetween < 0) {
                                        // Hết hạn
                                        count++;
                                        daCanhBaoDate = true;
        
                                    } else if (daysBetween <= 30) {
                                        // Sắp hết hạn nhưng đang giảm giá → bỏ qua
                                        if (dsDangGiamGia.contains(sp.getMaSP())) {
                                            daCanhBaoDate = true;
                                            continue;
                                        }
                                        count++;
                                        daCanhBaoDate = true;
                                    }
                                }
        
                                // Tồn thấp (chỉ đếm nếu chưa cảnh báo date)
                                if (!daCanhBaoDate && tonKho <= 15) {
                                    count++;
                                }
                            }
                        }
        
                        // === BƯỚC 3: Đếm lệch kho (đồng bộ logic với taiDuLieuThucTeTuKho) ===
                        String sqlLechKho = "SELECT k.MaLoHang, k.LyDo " +
                                            "FROM KiemKeKho k " +
                                            "WHERE k.SoLuongHeThong <> k.SoLuongThucTe";
                        try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                            java.sql.Statement st = con.createStatement();
                            java.sql.ResultSet rs = st.executeQuery(sqlLechKho)) {
                            while (rs.next()) {
                                String maLo = rs.getString("MaLoHang");
                                String lyDo = rs.getString("LyDo");
        
                                // Bỏ qua: đã bù trừ Huề kho
                                if (lyDo != null && lyDo.contains("Huề kho")) continue;
        
                                // Bỏ qua: đã kiểm tra hoàn tất
                                if (lyDo != null && lyDo.startsWith("Đã kiểm tra " + maLo)) continue;
                                if (dsLoaDaKiemTra.contains(maLo)) continue;
        
                                count++;
                            }
                        } catch (Exception ignored) {}
        
                    } catch (Exception ignored) {}
                    return count;
                }
        
                @Override
                protected void done() {
                    try { setNotificationCount(get()); } catch (Exception ignored) {}
                }
            };
            worker.execute();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Nền Glassmorphism khi Hover
            if (isHovered) {
                g2.setColor(new Color(255, 255, 255, 15)); // Màu trắng mờ
                g2.fillRoundRect(0, 0, w, h, 12, 12);
            }

            // 2. Vẽ Vector Chuông (Màu Accent Cyan khi hover, Muted khi bình thường)
            g2.setColor(isHovered ? CLR_ACCENT : CLR_TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            // Toạ độ vẽ chuông
            int bx = w/2 - 7;
            int by = h/2 - 6;
            
            // Đỉnh chuông (Arc)
            g2.drawArc(bx, by, 14, 14, 0, 180);
            // Thân chuông (Line)
            g2.drawLine(bx, by + 7, bx, by + 12);
            g2.drawLine(bx + 14, by + 7, bx + 14, by + 12);
            // Vành chuông dưới (Flared bottom)
            g2.drawRoundRect(bx - 2, by + 12, 18, 3, 2, 2);
            // Quả lắc (Clapper)
            g2.drawArc(bx + 5, by + 15, 4, 4, 180, 180);

            // 3. Vẽ Badge Đỏ nếu có cảnh báo (> 0)
            if (alertCount > 0) {
                String text = alertCount > 99 ? "99+" : String.valueOf(alertCount);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(text);
                
                int badgeH = 14;
                int badgeW = Math.max(badgeH, textW + 6); // Thành viên nhộng nếu text dài
                int badgeX = w - badgeW - 2;
                int badgeY = 2;

                // Nền Đỏ Danger
                g2.setColor(new Color(239, 68, 68)); // Đỏ hiện đại
                g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, badgeH, badgeH);
                
                // Viền mỏng tách biệt với nền (Cutout effect)
                g2.setColor(CLR_HEADER_BG);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, badgeH, badgeH);

                // Chữ Trắng
                g2.setColor(Color.WHITE);
                g2.drawString(text, badgeX + (badgeW - textW) / 2, badgeY + badgeH - 3);
            }
            g2.dispose();
        }
    }
    public void chuyenTabGiaoDien(String tenTab) {
        cardLayout.show(pnlCards, tenTab);
        
        // Nếu chuyển sang bảng lương, tắt highlight của các nút menu Sidebar
        if ("TAB_BANGLUONG".equals(tenTab)) {
            for (JButton btn : danhSachNutMenu) {
                btn.putClientProperty("active", false);
                btn.setBackground(CLR_SIDEBAR_BG); 
                btn.setForeground(CLR_TEXT_PRIMARY);
            }
            repaint();
        }
    }
}