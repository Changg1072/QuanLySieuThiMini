package GUI;

import Dao.BangLuongDAO;
import Dao.CauHinhLuongDAO;
import Dao.ChiaCaDAO;
import Data.BangLuong;
import Data.CauHinhLuong;
import Data.NhanVien;
import GUI.HoTro.*;
import Logic.BangLuongLogic;
import Logic.NhanVienLogic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BangLuongUi extends JPanel {

    // ================= MÀU SẮC CHỦ ĐẠO (Đồng bộ chuẩn UI) =================
    private static final Color BG_MAIN = new Color(245, 248, 253);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color HOVER_COLOR = new Color(241, 245, 249);
    private static final Color TEXT_MAIN = new Color(30, 41, 59);
    private static final Color TEXT_SUB = new Color(100, 116, 139);
    
    // Màu cho Stat Cards
    private static final Color COLOR_MONEY = new Color(5, 150, 105);   // Xanh ngọc
    private static final Color COLOR_PROGRESS = new Color(217, 119, 6); // Cam
    private static final Color COLOR_BONUS = new Color(37, 99, 235);    // Xanh dương
    private static final Color COLOR_PENALTY = new Color(220, 38, 38);  // Đỏ

    // ================= LOGIC & DỮ LIỆU =================
    private NhanVienLogic nvLogic = new NhanVienLogic();
    private BangLuongLogic blLogic = new BangLuongLogic();
    
    private List<PhieuLuongRowData> danhSachGoc = new ArrayList<>();
    private List<PhieuLuongRowData> danhSachHienThi = new ArrayList<>();
    private boolean isLoading = false;
    private boolean listenersActive = false;

    // ================= UI COMPONENTS =================
    private WheelPicker wheelThang, wheelNam;
    private JTextField txtTimKiem;
    private JPanel pnlRowListContainer;
    
    // Labels thống kê
    private JLabel lblTongQuyLuong, lblTienDo, lblTongThuong, lblTongKhauTru;
    private JProgressBar progressBar;

    private boolean isNhanVienMode = false;
    private String maNhanVienHienTai = null;

    public BangLuongUi() {
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        initUI();
        setupListeners();
        
        // Tự động load dữ liệu tháng hiện tại lúc mới mở
        loadDuLieuBangLuong();
    }
    public BangLuongUi(String maNV) {
        this.isNhanVienMode = true;
        this.maNhanVienHienTai = maNV;
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        initUI();
        setupListeners();
        loadDuLieuBangLuong();
    }
    // =========================================================
    // 1. KIẾN TRÚC UI CHÍNH
    // =========================================================
    private void initUI() {
        // --- NORTH: Header & Thống Kê ---
        JPanel pnlNorth = new JPanel(new BorderLayout(0, 25));
        pnlNorth.setOpaque(false);
        
        pnlNorth.add(createTopBar(), BorderLayout.NORTH);
        pnlNorth.add(createStatsPanel(), BorderLayout.CENTER);

        // --- CENTER: Danh sách Row Panel ---
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 10));
        pnlCenter.setOpaque(false);
        
        JLabel lblDanhSach = new JLabel("Danh sách nhân viên");
        lblDanhSach.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(18f));
        lblDanhSach.setForeground(TEXT_MAIN);
        
        JPanel pnlCenterHeader = new JPanel(new BorderLayout());
        pnlCenterHeader.setOpaque(false);
        pnlCenterHeader.add(lblDanhSach, BorderLayout.WEST);
        pnlCenterHeader.add(createFakeHeader(), BorderLayout.SOUTH);
        pnlCenter.add(pnlCenterHeader, BorderLayout.NORTH);

        pnlRowListContainer = new JPanel();
        pnlRowListContainer.setLayout(new BoxLayout(pnlRowListContainer, BoxLayout.Y_AXIS));
        pnlRowListContainer.setBackground(BG_MAIN);

        JScrollPane scrollPane = new JScrollPane(pnlRowListContainer);
        TienIchGiaoDien.thietLapThanhCuon(scrollPane);
        scrollPane.getViewport().setBackground(BG_MAIN);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        pnlCenter.add(scrollPane, BorderLayout.CENTER);

        // --- SOUTH: Các nút chức năng dưới cùng ---
        pnlCenter.add(createBottomActionPanel(), BorderLayout.SOUTH);

        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
    }

        private JPanel createTopBar() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);

        // --- BÊN TRÁI: Logo ---
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        pnlLeft.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Chi tiết bảng lương");
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 22f));
        lblTitle.setForeground(new Color(37, 99, 235));
        lblTitle.setPreferredSize(new Dimension(200, 38)); // Đồng bộ 38px
        lblTitle.setVerticalAlignment(SwingConstants.CENTER);
        pnlLeft.add(lblTitle);

        // --- BÊN PHẢI: Gộp Ngày Tháng + Tìm Kiếm + Thoát ---
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); 
        pnlRight.setOpaque(false);
        
        // 1. Cụm chọn Tháng/Năm
        JPanel pnlThangNam = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0)); 
        pnlThangNam.setOpaque(false);
        pnlThangNam.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
        pnlThangNam.setBackground(Color.WHITE);
        pnlThangNam.setOpaque(true);
        // 🚀 Tăng chiều cao lên 60px để hiển thị đẹp vòng xoay
        pnlThangNam.setPreferredSize(new Dimension(200, 60)); 
        
        wheelThang = new WheelPicker(new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"});
        
        int namHienTai = LocalDate.now().getYear();
        String[] dsNam = new String[5];
        for (int i = 0; i < 5; i++) {
            dsNam[i] = String.valueOf(namHienTai - 2 + i);
        }
        wheelNam = new WheelPicker(dsNam);
        
        wheelThang.setSelectedItem(String.format("%02d", LocalDate.now().getMonthValue()));
        wheelNam.setSelectedItem(String.valueOf(namHienTai));
        
        JLabel lblThang = new JLabel("Tháng", SwingConstants.CENTER);
        lblThang.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f));
        lblThang.setForeground(TEXT_SUB);

        // Dấu hai chấm (hoặc xuyệt) phân cách ở giữa giống hình của bạn
        JLabel lblColon = new JLabel(":", SwingConstants.CENTER);
        lblColon.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(22f));
        lblColon.setForeground(TEXT_MAIN);
        lblColon.setPreferredSize(new Dimension(15, 60));
        
        // Bố trí layout cho Picker
        pnlThangNam.add(wheelThang);
        pnlThangNam.add(lblColon);
        pnlThangNam.add(wheelNam);
        
        if (isNhanVienMode) {
            wheelThang.setVisible(false);
            lblColon.setVisible(false);
        }
        // 2. Ô Tìm Kiếm
        // 2. Ô Tìm Kiếm - Dùng JTextField thô thay vì ONhapLieuHienDai để kiểm soát chiều cao
        JTextField txtField = new JTextField();
        txtField.putClientProperty("caretWidth", 2);
        txtField.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(14f));
        txtField.setBorder(null);
        txtField.setOpaque(false);

        // Tạo wrapper panel tự vẽ border, kiểm soát hoàn toàn chiều cao
        JPanel pnlSearch = new JPanel(new BorderLayout(6, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        pnlSearch.setOpaque(false);
        pnlSearch.setBorder(new EmptyBorder(0, 10, 0, 10));
        pnlSearch.setPreferredSize(new Dimension(250, 60));
        pnlSearch.setMinimumSize(new Dimension(250, 60));
        pnlSearch.setMaximumSize(new Dimension(250, 60));

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        // Placeholder behavior
        txtField.setForeground(TEXT_SUB);
        txtField.setText("Tìm mã, tên NV...");
        txtTimKiem = txtField; 
        txtField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (txtField.getText().equals("Tìm mã, tên NV...")) {
                    txtField.setText("");
                    txtField.setForeground(TEXT_MAIN);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (txtField.getText().isEmpty()) {
                    txtField.setForeground(TEXT_SUB);
                    txtField.setText("Tìm mã, tên NV...");
                }
            }
        });

        pnlSearch.add(lblSearchIcon, BorderLayout.WEST);
        pnlSearch.add(txtField, BorderLayout.CENTER);
        // 3. Nút Thoát
        JButton btnThoat = TienIchGiaoDien.taoNutHienDai("Thoát ✕", new Color(239, 68, 68));
        btnThoat.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 14f));
        // 🚀 Ép cứng chiều cao 38px (Hoàn toàn cân bằng)
        btnThoat.setPreferredSize(new Dimension(100, 60)); 
        btnThoat.setMinimumSize(new Dimension(100, 60));
        btnThoat.setMaximumSize(new Dimension(100, 60));
        
        btnThoat.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof TrangADMIN) {
                TrangADMIN adminFrame = (TrangADMIN) window;
                adminFrame.chuyenTabGiaoDien("NHAN_VIEN"); 
            }
        });
        
        pnlRight.add(pnlThangNam);
        pnlRight.add(pnlSearch);
        pnlRight.add(btnThoat);

        pnl.add(pnlLeft, BorderLayout.WEST);
        pnl.add(pnlRight, BorderLayout.EAST);
        return pnl;
    }

    private JPanel createStatsPanel() {
        JPanel pnl = new JPanel(new GridLayout(1, 4, 20, 0));
        pnl.setOpaque(false);
        pnl.setPreferredSize(new Dimension(0, 110));

        lblTongQuyLuong = new JLabel("0 VNĐ");
        lblTienDo = new JLabel("0/0 nhân viên");
        lblTongThuong = new JLabel("0 VNĐ");
        lblTongKhauTru = new JLabel("0 VNĐ");

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(150, 6));
        progressBar.setForeground(COLOR_PROGRESS);
        progressBar.setBackground(BORDER_COLOR);
        progressBar.setBorderPainted(false);

        pnl.add(createStatCard("TỔNG QUỸ LƯƠNG", lblTongQuyLuong, COLOR_MONEY, null));
        
        // Thẻ tiến độ đặc biệt
        JPanel pnlTienDo = createStatCard("TIẾN ĐỘ CHỐT LƯƠNG", lblTienDo, COLOR_PROGRESS, null);
        pnlTienDo.add(progressBar, BorderLayout.SOUTH);
        pnl.add(pnlTienDo);
        
        pnl.add(createStatCard("TỔNG THƯỞNG", lblTongThuong, COLOR_BONUS, "💰"));
        pnl.add(createStatCard("TỔNG KHẤU TRỪ", lblTongKhauTru, COLOR_PENALTY, "📉"));

        return pnl;
    }

    private JPanel createStatCard(String title, JLabel lblValue, Color valueColor, String iconText) {
        JPanel card = new JPanel(new BorderLayout(10, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblT = new JLabel(title);
        lblT.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12f));
        lblT.setForeground(TEXT_SUB);

        lblValue.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 22f));
        lblValue.setForeground(valueColor);

        JPanel pnlContent = new JPanel(new BorderLayout());
        pnlContent.setOpaque(false);
        pnlContent.add(lblT, BorderLayout.NORTH);
        pnlContent.add(lblValue, BorderLayout.CENTER);

        card.add(pnlContent, BorderLayout.CENTER);

        if (iconText != null) {
            JLabel lblIcon = new JLabel(iconText, SwingConstants.CENTER);
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
            lblIcon.setPreferredSize(new Dimension(40, 40));
            card.add(lblIcon, BorderLayout.EAST);
        }

        return card;
    }

    private JPanel createFakeHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.setBackground(BG_MAIN);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 45));

        header.add(createHeaderLabel("NHÂN VIÊN", 300));
        header.add(createHeaderLabel("TỔNG GIỜ CÔNG", 150));
        header.add(createHeaderLabel("LƯƠNG THỰC NHẬN", 200));
        header.add(createHeaderLabel("TRẠNG THÁI", 150));
        header.add(createHeaderLabel("THAO TÁC", 120));

        return header;
    }
    
    private JLabel createHeaderLabel(String text, int width) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(13f));
        lbl.setForeground(TEXT_SUB); 
        lbl.setPreferredSize(new Dimension(width, 20));
        return lbl;
    }

    private JPanel createBottomActionPanel() {
        if (isNhanVienMode) return new JPanel() {{ setOpaque(false); }};
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(15, 0, 0, 0)); // Tạo khoảng cách với bảng bên trên

        // --- BÊN TRÁI: Nút Cài đặt cấu hình lương ---
        // ĐÃ SỬA: Đưa margin về 0 để nút bám sát mép lề trái, thẳng hàng với cột bảng
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLeft.setOpaque(false);
        
        JButton btnSetting = TienIchGiaoDien.taoNutHienDai("⚙ Cài đặt cấu hình lương", new Color(241, 245, 249));
        btnSetting.setForeground(COLOR_BONUS);
        btnSetting.setBorder(BorderFactory.createLineBorder(COLOR_BONUS));
        
        // 🚀 ĐÃ SỬA: Ép cứng kích thước bằng đúng nút bên kia (220x45), căn chữ ra giữa đẹp mắt
        btnSetting.setPreferredSize(new Dimension(220, 45)); 
        btnSetting.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f)); 
        
        btnSetting.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            GUI.HoTro.CaiDatLuongDialog dialog = new GUI.HoTro.CaiDatLuongDialog(parentWindow);
            dialog.setVisible(true);
            
            if (dialog.isSuccess()) {
                loadDuLieuBangLuong(); 
            }
        });
        
        pnlLeft.add(btnSetting);
        // (Đã xóa bỏ hoàn toàn nút Xuất phiếu lương)

        // --- BÊN PHẢI: Nút Chốt lương hàng loạt ---
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlRight.setOpaque(false);
        
        JButton btnChotHangLoat = TienIchGiaoDien.taoNutHienDai("✓ Chốt lương hàng loạt", COLOR_BONUS);
        // 🚀 ĐÃ SỬA: Đồng bộ kích thước 220x45
        btnChotHangLoat.setPreferredSize(new Dimension(220, 45));
        btnChotHangLoat.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
        btnChotHangLoat.addActionListener(e -> chotLuongHangLoat());
        
        pnlRight.add(btnChotHangLoat);

        pnl.add(pnlLeft, BorderLayout.WEST);
        pnl.add(pnlRight, BorderLayout.EAST);
        return pnl;
    }

    // =========================================================
    // 2. RENDER LIST (ROW PANELS)
    // =========================================================
    private void renderList(List<PhieuLuongRowData> data) {
        pnlRowListContainer.removeAll();

        if (isLoading) {
            // Skeleton load
            for (int i = 0; i < 5; i++) {
                JPanel skeleton = new JPanel();
                skeleton.setOpaque(false);
                skeleton.setPreferredSize(new Dimension(0, 75));
                skeleton.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
                pnlRowListContainer.add(skeleton);
                pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        } else {
            for (PhieuLuongRowData rowData : data) {
                pnlRowListContainer.add(createRowPanel(rowData));
            }
        }

        pnlRowListContainer.revalidate();
        pnlRowListContainer.repaint();
    }

        private JPanel createRowPanel(PhieuLuongRowData data) {
        boolean daChot = data.bangLuong != null;

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0); // Vuông vức theo thiết kế web
                g2.setColor(BORDER_COLOR);
                g2.drawRect(0, getHeight() - 1, getWidth(), 1); // Border bottom
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 75));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        // 1. Cột Nhân viên (Avatar + Tên / Hoặc hiển thị Kỳ lương nếu là NV xem)
        JPanel pnlName = new JPanel(new BorderLayout(15, 0));
        pnlName.setOpaque(false);
        pnlName.setPreferredSize(new Dimension(300, 50));
        
        JLabel lblAvatar = new JLabel("👤", SwingConstants.CENTER); // Fake avatar
        lblAvatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        
        JPanel pnlTextName = new JPanel(new GridLayout(2, 1));
        pnlTextName.setOpaque(false);
        
        // 🚀 ĐÃ SỬA: Đổi tiêu đề cột thành "Phiếu lương kỳ MM/yyyy" nếu là nhân viên đang xem
        String txtTieuDe = isNhanVienMode ? "Phiếu lương kỳ " + data.kyLuong : data.nhanVien.getHoTen();
        JLabel lblTen = new JLabel(txtTieuDe);
        lblTen.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
        lblTen.setForeground(TEXT_MAIN);
        
        JLabel lblMaChucVu = new JLabel(data.nhanVien.getMaNV() + " - " + data.nhanVien.getChucVu());
        lblMaChucVu.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f));
        lblMaChucVu.setForeground(TEXT_SUB);
        
        pnlTextName.add(lblTen);
        pnlTextName.add(lblMaChucVu);
        
        pnlName.add(lblAvatar, BorderLayout.WEST);
        pnlName.add(pnlTextName, BorderLayout.CENTER);
        row.add(pnlName);

        // 2. Cột Giờ công
        JLabel lblGio = createCell(String.format("%.2f", data.tongGioLam) + "h", 150, false);
        row.add(lblGio);

        // 3. Cột Lương
        String txtLuong = "";
        
        // Trích xuất cấu hình để tính toán (tránh lỗi null)
        final BigDecimal luongCB = (data.cauHinh != null && data.cauHinh.getLuongTheoGio() != null) ? data.cauHinh.getLuongTheoGio() : BigDecimal.ZERO;
        final BigDecimal heSo = (data.cauHinh != null && data.cauHinh.getHeSoLuong() != null) ? data.cauHinh.getHeSoLuong() : BigDecimal.ONE;
        final BigDecimal phuCap = (data.cauHinh != null && data.cauHinh.getPhuCapCoDinh() != null) ? data.cauHinh.getPhuCapCoDinh() : BigDecimal.ZERO;
        
        // Tính tiền tạm tính
        BigDecimal calcTienLuong = luongCB.multiply(heSo).multiply(BigDecimal.valueOf(data.tongGioLam));
        BigDecimal calcTongTamTinh = calcTienLuong.add(phuCap).subtract(data.tienPhatDuKien);
        if (calcTongTamTinh.compareTo(BigDecimal.ZERO) < 0) calcTongTamTinh = BigDecimal.ZERO;
        final BigDecimal tongTamTinh = calcTongTamTinh;

        if (daChot) {
            txtLuong = DinhDangUtil.dinhDangTien(data.bangLuong.getTongLuong()) + " VNĐ";
        } else {
            txtLuong = "Tạm tính: " + DinhDangUtil.dinhDangTien(tongTamTinh) + " VNĐ";
        }

        JLabel lblLuong = createCell(txtLuong, 200, true);
        if (daChot) lblLuong.setForeground(COLOR_BONUS);
        else lblLuong.setForeground(new Color(217, 119, 6)); // Màu cam cho tiền tạm tính
        row.add(lblLuong);

        // 4. Cột Trạng thái (Badge)
        JPanel pnlBadgeContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        pnlBadgeContainer.setOpaque(false);
        pnlBadgeContainer.setPreferredSize(new Dimension(150, 50));
        
        JLabel lblBadge = new JLabel(daChot ? "Đã chốt" : "Chưa chốt", SwingConstants.CENTER);
        lblBadge.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12f));
        lblBadge.setOpaque(true);
        lblBadge.setPreferredSize(new Dimension(90, 26));
        
        if (daChot) {
            lblBadge.setBackground(new Color(209, 250, 229));
            lblBadge.setForeground(COLOR_MONEY);
        } else {
            lblBadge.setBackground(new Color(241, 245, 249));
            lblBadge.setForeground(TEXT_SUB);
        }
        lblBadge.setBorder(BorderFactory.createLineBorder(lblBadge.getBackground().darker(), 1, true));
        pnlBadgeContainer.add(lblBadge);
        row.add(pnlBadgeContainer);

        // 5. Cột Thao tác
        // 🚀 ĐÃ SỬA: Thay đổi text của nút bấm tùy theo chế độ Quản lý hay Nhân viên
        JButton btnAction = TienIchGiaoDien.taoNutHienDai(isNhanVienMode ? "👁 Xem chi tiết" : (daChot ? "👁 Chi tiết" : "⚙ Chi tiết & Chốt"), new Color(241, 245, 249));
        btnAction.setPreferredSize(new Dimension(120, 30));
        btnAction.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12f));
        btnAction.setForeground(daChot ? TEXT_MAIN : COLOR_BONUS);
        btnAction.setBorder(BorderFactory.createLineBorder(daChot ? BORDER_COLOR : COLOR_BONUS, 1));
        
        btnAction.addActionListener(e -> {
            String thangNamPhieu = data.kyLuong;
            
            if (isNhanVienMode) {
                // CHẾ ĐỘ NHÂN VIÊN
                BangLuong blHienThi = data.bangLuong;
                
                // Nếu chưa chốt, tạo Phiếu Lương Ảo để hiển thị form
                if (blHienThi == null) {
                    blHienThi = new BangLuong.ThoXayBangLuong()
                        .ganMaNV(data.nhanVien.getMaNV())
                        .ganThangNam(thangNamPhieu)
                        .ganTongGioLam(BigDecimal.valueOf(data.tongGioLam))
                        .ganGioTangCa(BigDecimal.ZERO)
                        .ganLuongTheoGio(luongCB)
                        .ganThuong(BigDecimal.ZERO)
                        .ganKhauTru(data.tienPhatDuKien)
                        .ganTongLuong(tongTamTinh)
                        .taoMoi();
                }

                GUI.HoTro.ChiTietChotLuongDialog dialog = new GUI.HoTro.ChiTietChotLuongDialog(
                    (Window) SwingUtilities.getWindowAncestor(this),
                    blHienThi,
                    data.nhanVien.getHoTen(),
                    data.nhanVien.getChucVu(),
                    0
                );
                dialog.setVisible(true);
            } else {
                // CHẾ ĐỘ QUẢN LÝ (Giữ nguyên)
                String maNV = data.nhanVien.getMaNV();
                String tenNV = data.nhanVien.getHoTen();
                String chucVu = data.nhanVien.getChucVu();
                double gioLam = data.tongGioLam;
                BigDecimal phat = data.tienPhatDuKien;
                
                GUI.HoTro.ChiTietChotLuongDialog dialog = new GUI.HoTro.ChiTietChotLuongDialog(
                    (Window) SwingUtilities.getWindowAncestor(this),
                    maNV, tenNV, chucVu, luongCB, gioLam, 0, BigDecimal.valueOf(1.5), phat, 0, thangNamPhieu
                );
                dialog.setVisible(true);
                if (dialog.isSuccess()) loadDuLieuBangLuong(); 
            }
        });
        row.add(btnAction);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(HOVER_COLOR); row.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(Color.WHITE); row.repaint(); }
        });

        return row;
    }

    private JLabel createCell(String text, int width, boolean isBold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(isBold ? TienIchGiaoDien.FONT_DAM.deriveFont(14f) : TienIchGiaoDien.FONT_CHINH.deriveFont(14f));
        lbl.setForeground(TEXT_MAIN);
        lbl.setPreferredSize(new Dimension(width, 50));
        return lbl;
    }

    // =========================================================
    // 3. LOGIC DATA & THỐNG KÊ
    // =========================================================
    private void setupListeners() {
        wheelThang.addChangeListener(e -> loadDuLieuBangLuong());
        wheelNam.addChangeListener(e -> loadDuLieuBangLuong());

        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { locDuLieu(); }
            public void removeUpdate(DocumentEvent e) { locDuLieu(); }
            public void changedUpdate(DocumentEvent e) { locDuLieu(); }
        });
        listenersActive = true;
    }
    private void loadDuLieuBangLuong() {
        if (isLoading) return;
        isLoading = true;
        renderList(new ArrayList<>()); 

        String thangStr = wheelThang.getSelectedItem();
        String namStr = wheelNam.getSelectedItem();
        String thangNamDangChon = thangStr + "/" + namStr;

        final int thangInt = Integer.parseInt(thangStr);
        final int namInt = Integer.parseInt(namStr);

        SwingWorker<List<PhieuLuongRowData>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<PhieuLuongRowData> doInBackground() throws Exception {
                List<PhieuLuongRowData> result = new ArrayList<>();

                if (isNhanVienMode) {
                    // ==========================================
                    // CHẾ ĐỘ NHÂN VIÊN (Siêu tốc O(1))
                    // ==========================================
                    NhanVien nv = nvLogic.timNhanVienTheoMa(maNhanVienHienTai); 
                    if (nv == null) return result;

                    CauHinhLuong cauHinh = Dao.CauHinhLuongDAO.getInstance().layCauHinhHienTaiTheoMaNV(maNhanVienHienTai);
                    Dao.TruyVanSieuTocDAO.DuLieuBangLuongDTO dto = Dao.TruyVanSieuTocDAO.getInstance().loadBangLuongNhanVienSieuToc(maNhanVienHienTai, namInt);
                    LocalDate now = LocalDate.now();

                    for (int i = 12; i >= 1; i--) {
                        String thangNamKiemTra = String.format("%02d/%d", i, namInt);
                        BangLuong blThangNay = dto.mapBangLuongChot.get(thangNamKiemTra);
                        
                        if (blThangNay != null) {
                            PhieuLuongRowData row = new PhieuLuongRowData();
                            row.nhanVien = nv; row.bangLuong = blThangNay; row.cauHinh = cauHinh;
                            row.tongGioLam = blThangNay.getTongGioLam() != null ? blThangNay.getTongGioLam().doubleValue() : 0;
                            row.kyLuong = thangNamKiemTra;
                            result.add(row);
                        } else {
                            if (namInt > now.getYear() || (namInt == now.getYear() && i > now.getMonthValue())) continue;
                            
                            double gioLam = dto.mapTongGioLam.getOrDefault(thangNamKiemTra, 0.0);
                            if (gioLam > 0 || (i == now.getMonthValue() && namInt == now.getYear())) {
                                PhieuLuongRowData row = new PhieuLuongRowData();
                                row.nhanVien = nv; row.bangLuong = null; row.cauHinh = cauHinh;
                                row.tongGioLam = gioLam;
                                row.tienPhatDuKien = dto.mapTongPhat.getOrDefault(thangNamKiemTra, BigDecimal.ZERO);
                                row.kyLuong = thangNamKiemTra;
                                result.add(row);
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // CHẾ ĐỘ QUẢN LÝ (Siêu tốc O(1)) - Tránh N+1 Query
                    // ==========================================
                    List<NhanVien> tatCaNV = nvLogic.layDanhSachNhanVien(); // Chỉ load danh sách NV cơ bản
                    Dao.TruyVanSieuTocDAO.DuLieuBangLuongDTO dto = Dao.TruyVanSieuTocDAO.getInstance().loadBangLuongQuanLySieuToc(thangNamDangChon, thangInt, namInt);

                    for (NhanVien n : tatCaNV) {
                        String maNV = n.getMaNV();
                        BangLuong bl = dto.mapBangLuongChot.get(maNV); // O(1) Lookup từ RAM
                        
                        PhieuLuongRowData row = new PhieuLuongRowData();
                        row.nhanVien = n; 
                        row.bangLuong = bl;
                        row.cauHinh = dto.mapCauHinh.get(maNV);
                        row.kyLuong = thangNamDangChon;

                        if (bl != null) {
                            row.tongGioLam = bl.getTongGioLam() != null ? bl.getTongGioLam().doubleValue() : 0;
                        } else {
                            row.tongGioLam = dto.mapTongGioLam.getOrDefault(maNV, 0.0);
                            row.tienPhatDuKien = dto.mapTongPhat.getOrDefault(maNV, BigDecimal.ZERO);
                        }
                        result.add(row);
                    }
                }
                return result;
            }

            @Override
            protected void done() {
                isLoading = false; 
                try {
                    danhSachGoc = get();
                    locDuLieu();
                } catch (Exception e) {
                    e.printStackTrace();
                    renderList(new ArrayList<>()); 
                }
            }
        };
        worker.execute();
    }

    private void locDuLieu() {
        String raw = txtTimKiem.getText().equals("Tìm mã, tên NV...") ? "" : txtTimKiem.getText();
        String key = DinhDangUtil.loaiBoDauTiengViet(raw.toLowerCase().trim());
        danhSachHienThi = danhSachGoc.stream().filter(row -> {
            if (key.isEmpty()) return true;
            return (row.nhanVien.getMaNV().toLowerCase().contains(key) || 
                    DinhDangUtil.loaiBoDauTiengViet(row.nhanVien.getHoTen().toLowerCase()).contains(key));
        }).collect(Collectors.toList());

        capNhatThongKe();
        renderList(danhSachHienThi);
    }

    private void capNhatThongKe() {
        BigDecimal tongQuy = BigDecimal.ZERO;
        BigDecimal tongThuong = BigDecimal.ZERO;
        BigDecimal tongPhat = BigDecimal.ZERO;
        int daChot = 0;

        for (PhieuLuongRowData row : danhSachHienThi) {
            if (row.bangLuong != null) {
                // ĐÃ CHỐT: Lấy cứng từ DB
                daChot++;
                tongQuy = tongQuy.add(row.bangLuong.getTongLuong());
                tongThuong = tongThuong.add(row.bangLuong.getThuong());
                tongPhat = tongPhat.add(row.bangLuong.getKhauTru());
            } else if (isNhanVienMode) {
                // CHƯA CHỐT VÀ LÀ NHÂN VIÊN: Tính cộng dồn số Tạm tính lên Thẻ Thống Kê
                BigDecimal luongCB = (row.cauHinh != null && row.cauHinh.getLuongTheoGio() != null) ? row.cauHinh.getLuongTheoGio() : BigDecimal.ZERO;
                BigDecimal heSo = (row.cauHinh != null && row.cauHinh.getHeSoLuong() != null) ? row.cauHinh.getHeSoLuong() : BigDecimal.ONE;
                BigDecimal phuCap = (row.cauHinh != null && row.cauHinh.getPhuCapCoDinh() != null) ? row.cauHinh.getPhuCapCoDinh() : BigDecimal.ZERO;
                
                BigDecimal tienLuong = luongCB.multiply(heSo).multiply(BigDecimal.valueOf(row.tongGioLam));
                BigDecimal tamTinh = tienLuong.add(phuCap).subtract(row.tienPhatDuKien);
                if (tamTinh.compareTo(BigDecimal.ZERO) < 0) tamTinh = BigDecimal.ZERO;

                tongQuy = tongQuy.add(tamTinh);
                tongPhat = tongPhat.add(row.tienPhatDuKien);
            }
        }

        lblTongQuyLuong.setText(DinhDangUtil.dinhDangTien(tongQuy) + " VNĐ");
        lblTongThuong.setText(DinhDangUtil.dinhDangTien(tongThuong) + " VNĐ");
        lblTongKhauTru.setText(DinhDangUtil.dinhDangTien(tongPhat) + " VNĐ");
        if (isNhanVienMode) {
            lblTienDo.setText(daChot + "/" + danhSachHienThi.size() + " tháng");
        } else {
            lblTienDo.setText(daChot + "/" + danhSachHienThi.size() + " nhân viên");
        }
        int pt = danhSachHienThi.size() == 0 ? 0 : (int)((daChot * 100.0f) / danhSachHienThi.size());
        progressBar.setValue(pt);
    }

    private void chotLuongHangLoat() {
        String thangNamStr = wheelThang.getSelectedItem() + "/" + wheelNam.getSelectedItem();
        int countChuaChot = 0;
        for (PhieuLuongRowData r : danhSachGoc) {
            if (r.bangLuong == null) countChuaChot++;
        }

        if (countChuaChot == 0) {
            TienIchGiaoDien.hienThiThongBao(this, "Tất cả nhân viên đã được chốt lương trong tháng này!", "INFO");
            return;
        }

        // TẠO BIẾN FINAL ĐỂ TRUYỀN VÀO LAMBDA TRÁNH LỖI
        final int soNguoiCanChot = countChuaChot;

        TienIchGiaoDien.hienThiXacNhan(this, "Hệ thống sẽ chốt tự động cho " + soNguoiCanChot + " nhân viên.\nTiền thưởng thêm sẽ mặc định là 0đ. Bạn có chắc chắn?", () -> {
            int success = 0;
            for (PhieuLuongRowData r : danhSachGoc) {
                if (r.bangLuong == null && r.cauHinh != null) {
                    try {
                        // TRUYỀN ĐỦ 6 THAM SỐ (Bao gồm giờ làm, OT, Thưởng, Khấu trừ)
                        blLogic.chotVaLuuBangLuong(
                            r.nhanVien.getMaNV(), 
                            thangNamStr, 
                            BigDecimal.valueOf(r.tongGioLam), // Giờ làm
                            BigDecimal.ZERO,                  // Giờ OT (Mặc định 0 khi chốt hàng loạt)
                            BigDecimal.ZERO,                  // Thưởng
                            r.tienPhatDuKien                  // Khấu trừ (Đi muộn...)
                        );
                        success++;
                    } catch (Exception e) {
                        System.err.println("Lỗi chốt lương cho NV " + r.nhanVien.getMaNV() + ": " + e.getMessage());
                    }
                }
            }
            TienIchGiaoDien.hienThiThongBao(this, "Đã chốt thành công " + success + "/" + soNguoiCanChot + " nhân viên!", "SUCCESS");
            loadDuLieuBangLuong();
        });
    }

    // =========================================================
    // LỚP DTO CHỨA DỮ LIỆU GỘP CHO 1 DÒNG
    // =========================================================
    class PhieuLuongRowData {
        NhanVien nhanVien;
        CauHinhLuong cauHinh;
        BangLuong bangLuong; // Bằng NULL nếu chưa chốt
        double tongGioLam = 0;
        BigDecimal tienPhatDuKien = BigDecimal.ZERO;
        String kyLuong;
    }
        // =========================================================
    // COMPONENT CHỌN NGÀY THÁNG DẠNG VÒNG XOAY (WHEEL PICKER)
    // =========================================================
    class WheelPicker extends JPanel {
        private String[] items;
        private int selectedIndex = 0;
        private List<javax.swing.event.ChangeListener> listeners = new ArrayList<>();

        public WheelPicker(String[] items) {
            this.items = items;
            setOpaque(false);
            // Kích thước đủ cao để hiện 3 dòng số (Số trước - Số hiện tại - Số sau)
            setPreferredSize(new Dimension(60, 60)); 
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Hỗ trợ lăn chuột
            addMouseWheelListener(e -> {
                if (e.getWheelRotation() < 0 && selectedIndex > 0) {
                    selectedIndex--;
                    fireChangeEvent();
                } else if (e.getWheelRotation() > 0 && selectedIndex < items.length - 1) {
                    selectedIndex++;
                    fireChangeEvent();
                }
            });

            // Hỗ trợ click nửa trên/nửa dưới để chuyển số
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getY() < getHeight() / 2 && selectedIndex > 0) {
                        selectedIndex--; fireChangeEvent();
                    } else if (e.getY() > getHeight() / 2 && selectedIndex < items.length - 1) {
                        selectedIndex++; fireChangeEvent();
                    }
                }
            });
        }

        public String getSelectedItem() {
            return items[selectedIndex];
        }

        public void setSelectedItem(String item) {
            for (int i = 0; i < items.length; i++) {
                if (items[i].equals(item)) {
                    selectedIndex = i;
                    repaint();
                    break;
                }
            }
        }

        public void addChangeListener(javax.swing.event.ChangeListener l) {
            listeners.add(l);
        }

        private void fireChangeEvent() {
            repaint();
            javax.swing.event.ChangeEvent e = new javax.swing.event.ChangeEvent(this);
            for (javax.swing.event.ChangeListener l : listeners) {
                l.stateChanged(e);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int centerY = h / 2;

            // Font cho số đang được chọn (To, Đậm, Đen)
            g2.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 22f));
            FontMetrics fmMain = g2.getFontMetrics();
            String current = items[selectedIndex];
            int textX = (w - fmMain.stringWidth(current)) / 2;
            int textY = centerY + fmMain.getAscent() / 2 - 4; // Căn giữa trục Y
            
            g2.setColor(TEXT_MAIN);
            g2.drawString(current, textX, textY);

            // Font cho số trước và sau (Nhỏ, Mờ)
            g2.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(16f));
            FontMetrics fmSub = g2.getFontMetrics();
            g2.setColor(new Color(148, 163, 184)); // TEXT_SUB nhạt hơn chút

            if (selectedIndex > 0) {
                String prev = items[selectedIndex - 1];
                int px = (w - fmSub.stringWidth(prev)) / 2;
                g2.drawString(prev, px, textY - 24); // Đẩy lên trên
            }

            if (selectedIndex < items.length - 1) {
                String next = items[selectedIndex + 1];
                int nx = (w - fmSub.stringWidth(next)) / 2;
                g2.drawString(next, nx, textY + 24); // Đẩy xuống dưới
            }
            g2.dispose();
        }
    }

    public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        JFrame frame = new JFrame("BangLuongUi - Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1300, 750);
        frame.setLocationRelativeTo(null);

        // Đổi dòng dưới để test chế độ NV: new BangLuongUi("NV001")
        frame.add(new BangLuongUi());

        frame.setVisible(true);
    });
}
}
