package GUI;

import GUI.HoTro.MenuSidebarUtil;
import GUI.HoTro.TienIchGiaoDien;
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
    // ===================== KHAI BÁO BIẾN LƯU TRỮ DỮ LIỆU TẢI TRƯỚC =====================
    private final String maNhanVien;
    private final String tenNhanVien;
    private final List<LoaiSP> dsLoaiSP; // ✨ Chìa khóa vàng giải quyết lỗi đỏ!
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

        pnlCards.add(taoPanelGiuCho("TRANG CHỦ TỔNG QUAN"),            "TRANG_CHU");
        pnlCards.add(taoPanelGiuCho("BÁO CÁO THỐNG KÊ DOANH THU"),     "THONG_KE");
        pnlCards.add(taoPanelGiuCho("QUẢN LÝ SẢN PHẨM"),               "SAN_PHAM");
        pnlCards.add(taoPanelGiuCho("QUẢN LÝ CHƯƠNG TRÌNH GIẢM GIÁ"),  "GIAM_GIA");
        pnlCards.add(taoPanelGiuCho("QUẢN LÝ NHÀ CUNG CẤP"),           "NHA_CUNG_CAP");
        pnlCards.add(taoPanelGiuCho("QUẢN LÝ KHÁCH HÀNG THÀNH VIÊN"),  "KHACH_HANG");
        pnlCards.add(taoPanelGiuCho("DANH SÁCH NHÂN VIÊN"),            "NHAN_VIEN");

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

        JLabel lblTieuDe = taoLabel("QUẢN LÝ SIÊU THỊ", 13, Font.BOLD, CLR_ACCENT);
        lblTieuDe.setBorder(new EmptyBorder(0, 0, 12, 0));
        pnl.add(lblTieuDe);

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

        pnl.add(taoNhomMenuHover("  Tổng quan",
            taoMucDropdown("Trang chủ",  "TRANG_CHU"),
            taoMucDropdown("Thống kê",   "THONG_KE")
        ));

        pnl.add(taoNhomMenuHover("  Quản lý",
            taoMucDropdown("Sản phẩm",     "SAN_PHAM"),
            taoMucDropdown("Giảm giá",     "GIAM_GIA"),
            taoMucDropdown("Nhà cung cấp", "NHA_CUNG_CAP"),
            taoMucDropdown("Đơn hàng",     "DON_HANG"), 
            taoMucDropdown("Khách hàng",   "KHACH_HANG"),
            taoMucDropdown("Nhân viên",    "NHAN_VIEN"),
            taoMucDropdown("Ca làm",       "CA_LAM")
        ));

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

        // 3. Render các danh mục từ DANH SÁCH ĐÃ CACHE (Khỏi query DB!) 🌈
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
                    if (chiaCaUi == null) { chiaCaUi = new ChiaCaUi(this.maNhanVien); pnlCards.add(chiaCaUi, "CA_LAM"); }
                    break;
                case "TAI_KHOAN":
                    if (taiKhoanUi == null) { taiKhoanUi = new TaiKhoanUi(maNhanVien); pnlCards.add(taiKhoanUi, "TAI_KHOAN"); }
                    break;
                case "DON_HANG":
                    if (donHangUi == null) { 
                        // Truyền Cache cho DonHangUi để chạy với tốc độ ánh sáng! ⚡
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
                        // Lần đầu click mới khởi tạo giao diện và load data (Lazy Load)
                        khachHangUi = new DanhSachKhUi(); 
                        // Add vào CardLayout với cùng tên "KHACH_HANG" để đè lên Panel giữ chỗ cũ
                        pnlCards.add(khachHangUi, "KHACH_HANG"); 
                        pnlCards.revalidate(); 
                        pnlCards.repaint();
                    }
                    // Các lần click sau chỉ show ra thôi, không tạo lại để mượt mà
                    break;
                case "NHAN_VIEN":
                    if (danhSachNvUi == null) {
                        // Khởi tạo giao diện lần đầu tiên (Lazy Load)
                        danhSachNvUi = new DanhSachNvUi();
                        // Gắn nó vào CardLayout để thay thế cái Panel "Giữ chỗ" ban đầu
                        pnlCards.add(danhSachNvUi, "NHAN_VIEN");
                        pnlCards.revalidate();
                        pnlCards.repaint();
                    }
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

        wrapper.add(btnHeader, BorderLayout.CENTER);
        return wrapper;
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
            Object[][] dsMon    = banHangUi.layDuLieuGioHang();
            BigDecimal tongTien = banHangUi.layTongTienGioHang();
            thanhToanUi.nhanDuLieuTuGioHang(dsMon, tongTien);
            cardLayout.show(pnlCards, "THANH_TOAN");
        });
        thanhToanUi.setHanhDongQuayLai(
            () -> cardLayout.show(pnlCards, "BAN_HANG"));
        thanhToanUi.setHanhDongThanhToanThanhCong(
            () -> { banHangUi.lamMoiToanBoBanHang(); cardLayout.show(pnlCards, "BAN_HANG"); });
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
}