package GUI;

import GUI.HoTro.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ThanhToanUi extends JPanel {

    private final Color BG_MAIN = new Color(243, 244, 246); 
    private final Color TEXT_MAIN = new Color(31, 41, 55); 
    private final Color TEXT_SUB = new Color(107, 114, 128);
    private final Color RED_PASTEL = new Color(239, 68, 68); 
    private final Color GREEN_PASTEL = new Color(34, 197, 94); 
    private final Color BLUE_PASTEL = new Color(59, 130, 246); 
    private final Color GRAY_BORDER = new Color(203, 213, 225);
    private final Color YELLOW_QUAY_LAI = new Color(251, 191, 36); 
    
    // ĐÃ FIX: Mã màu Xanh mòng két (Dark Blue) chính xác theo hình của sếp
    private final Color BTN_BLUE = new Color(34, 110, 153);

    private final Font fontThuong = new Font("Calibri", Font.PLAIN, 15);
    private final Font fontDam = new Font("Calibri", Font.BOLD, 15);
    private final Font fontTitle = new Font("Calibri", Font.BOLD, 22); 
    
    private JRadioButton radDungDiemCo, radDungDiemKhong;
    private CardLayout cardThanhToan;
    private JPanel pnlCardThanhToan;
    private JLabel lblSoTienChuyenKhoan;
    
    private BigDecimal khachCanTraValue = BigDecimal.ZERO;
    private BigDecimal tongTienHoaDon = BigDecimal.ZERO; 
    
    private boolean isThanhToanTienMat = true;

    private String maNhanVienThucTe = "NV001";
    private String tenNhanVienThucTe = "Thu Ngân";
    
    public void setNhanVien(String maNV, String tenNV) {
        this.maNhanVienThucTe = maNV;
        this.tenNhanVienThucTe = tenNV;
    }

    private ChiTietHoaDonUi pnlHoaDonPreview;
    
    private JLabel lblKhachCanTra, lblTienThuaNhanh;
    private JTextField txtTienKhachDua;
    private NutBoGoc btnTienMat, btnChuyenKhoan, btnVuaDu;
    
    private JPanel pnlTienNhanh;
    private CardLayout cardAction;
    private JPanel pnlActionContainer;
    private Object[][] currentItems; 
    private String tenKhachHang = "Khách vãng lai";
    private String maHoaDonHienTai = "HD_AUTO";
    private JTextField txtSearch;
    private NutBoGoc btnTim;
    private JLabel lblTenKHValue, lblHangTVValue, lblGiamGiaValue, lblDiemValue;
    private Data.KhachHang khachHangHienTai = null;

    public ThanhToanUi() {
        setLayout(new GridLayout(1, 2, 20, 0)); 
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        pnlHoaDonPreview = new ChiTietHoaDonUi();
        add(pnlHoaDonPreview); 

        add(taoPanelPhaiThanhToan());
        loadDuLieuMau();
    }
    
    private JPanel taoPanelPhaiThanhToan() {
        // ĐÃ FIX: Dùng BorderLayout để ép khối, KHÔNG cho phép trượt lướt xuống dưới
        JPanel pnlRight = new JPanel(new BorderLayout(0, 15));
        pnlRight.setBackground(BG_MAIN);

        // ==========================================
        // 1. PANEL KHÁCH HÀNG (Ép lên Cạnh Bắc - NORTH)
        // ==========================================
        TheBongDo pnlKhachHang = new TheBongDo(12);
        pnlKhachHang.setLayout(new BorderLayout(0, 8)); 
        pnlKhachHang.setBackground(Color.WHITE);
        pnlKhachHang.setBorder(new EmptyBorder(10, 15, 10, 15)); // Ép nhỏ viền lại

        JPanel pnlKHTop = new JPanel(new BorderLayout(0, 8));
        pnlKHTop.setBackground(Color.WHITE);
        
        JLabel lblTitleKH = new JLabel("THÔNG TIN KHÁCH HÀNG");
        lblTitleKH.setFont(fontTitle);
        lblTitleKH.setForeground(TEXT_MAIN);
        lblTitleKH.setBorder(new EmptyBorder(0, 0, 5, 0));

        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(Color.WHITE);
        txtSearch = taoOTextChuan("Tìm khách hàng bằng SĐT...");
        txtSearch.setPreferredSize(new Dimension(0, 34)); // Ép chiều cao thanh search
        btnTim = new NutBoGoc("Tìm");
        btnTim.setColorBackground(BLUE_PASTEL); btnTim.setArc(8); btnTim.setPreferredSize(new Dimension(90, 34));
        btnTim.addActionListener(e -> xuLyTimKiemKhachHang());
        txtSearch.addActionListener(e -> xuLyTimKiemKhachHang());
        pnlSearch.add(txtSearch, BorderLayout.CENTER); pnlSearch.add(btnTim, BorderLayout.EAST);

        pnlKHTop.add(lblTitleKH, BorderLayout.NORTH);
        pnlKHTop.add(pnlSearch, BorderLayout.CENTER);

        JPanel pnlKHCenter = new JPanel(new BorderLayout(0, 5));
        pnlKHCenter.setBackground(Color.WHITE);

        lblTenKHValue = new JLabel(tenKhachHang); lblTenKHValue.setFont(fontDam); lblTenKHValue.setForeground(TEXT_MAIN);
        lblHangTVValue = new JLabel("---"); lblHangTVValue.setFont(fontDam); lblHangTVValue.setForeground(TEXT_MAIN);
        lblGiamGiaValue = new JLabel("0%"); lblGiamGiaValue.setFont(fontDam); lblGiamGiaValue.setForeground(RED_PASTEL);
        lblDiemValue = new JLabel("0"); lblDiemValue.setFont(fontDam); lblDiemValue.setForeground(GREEN_PASTEL);

        JPanel pnlInfoKhach = new JPanel(new GridLayout(1, 4, 10, 0));
        pnlInfoKhach.setBackground(new Color(248, 250, 252));
        pnlInfoKhach.setBorder(new EmptyBorder(8, 10, 8, 10)); // Ép nhỏ viền
        pnlInfoKhach.add(taoBlockInfoKH("Tên KH", lblTenKHValue));
        pnlInfoKhach.add(taoBlockInfoKH("Hạng TV", lblHangTVValue));
        pnlInfoKhach.add(taoBlockInfoKH("Giảm giá", lblGiamGiaValue));
        pnlInfoKhach.add(taoBlockInfoKH("Điểm TL", lblDiemValue));

        JPanel pnlUuDai = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlUuDai.setBackground(Color.WHITE);
        
        JLabel lblUuDaiText = new JLabel("Sử dụng điểm tích lũy:");
        lblUuDaiText.setFont(fontDam.deriveFont(16f)); 
        pnlUuDai.add(lblUuDaiText);
        
        radDungDiemCo = new JRadioButton("Có"); radDungDiemKhong = new JRadioButton("Không", true);
        radDungDiemCo.setBackground(Color.WHITE); radDungDiemKhong.setBackground(Color.WHITE);
        radDungDiemCo.setFont(fontThuong.deriveFont(16f));
        radDungDiemKhong.setFont(fontThuong.deriveFont(16f));

        ButtonGroup bgUuDai = new ButtonGroup(); bgUuDai.add(radDungDiemCo); bgUuDai.add(radDungDiemKhong);
        pnlUuDai.add(radDungDiemCo); pnlUuDai.add(radDungDiemKhong);
        radDungDiemCo.addActionListener(e -> tinhToanVaCapNhatUI());
        radDungDiemKhong.addActionListener(e -> tinhToanVaCapNhatUI());

        pnlKHCenter.add(pnlInfoKhach, BorderLayout.NORTH);
        pnlKHCenter.add(pnlUuDai, BorderLayout.CENTER);

        JPanel pnlDangKy = new JPanel(new BorderLayout(10, 0));
        pnlDangKy.setBackground(Color.WHITE);
        pnlDangKy.setBorder(BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GRAY_BORDER), "Hoặc đăng ký khách mới", TitledBorder.LEFT, TitledBorder.TOP, fontThuong.deriveFont(Font.ITALIC, 16f), TEXT_SUB));

        JTextField txtHoTenDK = taoOTextChuan("Nhập họ tên...");
        JTextField txtSdtDK = taoOTextChuan("Nhập SĐT...");
        NutBoGoc btnTaoChon = new NutBoGoc("Tạo & Chọn");
        btnTaoChon.setColorBackground(new Color(241, 245, 249)); btnTaoChon.setForeground(TEXT_MAIN); btnTaoChon.setArc(8);
        btnTaoChon.setPreferredSize(new Dimension(140, 36)); 

        JPanel pnlDKInputs = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlDKInputs.setBackground(Color.WHITE);
        pnlDKInputs.add(txtHoTenDK); pnlDKInputs.add(txtSdtDK);

        pnlDangKy.add(pnlDKInputs, BorderLayout.CENTER);
        pnlDangKy.add(btnTaoChon, BorderLayout.EAST);

        btnTaoChon.addActionListener(e -> {
            String hoTen = txtHoTenDK.getText().trim();
            String sdt = txtSdtDK.getText().trim();

            if (hoTen.isEmpty() || hoTen.equals("Nhập họ tên...")) {
                TienIchGiaoDien.hienThiThongBao(this, "Vui lòng nhập họ tên khách hàng!", "WARNING");
                txtHoTenDK.requestFocus(); return;
            }
            if (sdt.isEmpty() || sdt.equals("Nhập SĐT...")) {
                TienIchGiaoDien.hienThiThongBao(this, "Vui lòng nhập số điện thoại!", "WARNING");
                txtSdtDK.requestFocus(); return;
            }

            try {
                Logic.KhachHangLogic logicKH = new Logic.KhachHangLogic();
                String maKHMoi = Logic.TaoMaTuDongLogic.taoMaKhachHang();
                Data.KhachHang khMoi = new Data.KhachHang.ThoXayKhachHang()
                    .ganMaKH(maKHMoi).ganHoTen(hoTen).ganSDT(sdt)
                    .ganDiemTichLuy(BigDecimal.ZERO).ganBacKH("Không hạng").taoMoi();

                logicKH.themKhachHang(khMoi);
                TienIchGiaoDien.hienThiThongBao(this, "Đăng ký khách hàng thành công!", "SUCCESS");

                txtSearch.setText(sdt); txtSearch.setForeground(TEXT_MAIN);
                xuLyTimKiemKhachHang();

                txtHoTenDK.setText("Nhập họ tên..."); txtHoTenDK.setForeground(Color.GRAY);
                txtSdtDK.setText("Nhập SĐT..."); txtSdtDK.setForeground(Color.GRAY);

            } catch (Exception ex) {
                TienIchGiaoDien.hienThiThongBao(this, ex.getMessage(), "ERROR");
            }
        });

        pnlKhachHang.add(pnlKHTop, BorderLayout.NORTH);
        pnlKhachHang.add(pnlKHCenter, BorderLayout.CENTER);
        pnlKhachHang.add(pnlDangKy, BorderLayout.SOUTH);

        // ==========================================
        // 2. PANEL THANH TOÁN (Ép chèn vào Giữa - CENTER tự động giãn)
        // ==========================================
        TheBongDo pnlThanhToan = new TheBongDo(12);
        pnlThanhToan.setLayout(new BorderLayout(0, 10));
        pnlThanhToan.setBackground(Color.WHITE);
        pnlThanhToan.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel pnlTTTop = new JPanel(new BorderLayout(0, 8));
        pnlTTTop.setBackground(Color.WHITE);

        JLabel lblTitleTT = new JLabel("CHI TIẾT THANH TOÁN");
        lblTitleTT.setFont(fontTitle); lblTitleTT.setForeground(TEXT_MAIN);
        lblTitleTT.setBorder(new EmptyBorder(5, 0, 5, 0));

        JPanel pnlCanTra = new JPanel(new BorderLayout());
        pnlCanTra.setBackground(new Color(254, 242, 242)); 
        pnlCanTra.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(252, 165, 165)), new EmptyBorder(5, 15, 5, 15)));
        
        JLabel lblCanTraText = new JLabel("KHÁCH CẦN TRẢ");
        lblCanTraText.setFont(fontDam.deriveFont(18f)); 
        lblCanTraText.setForeground(RED_PASTEL);

        lblKhachCanTra = new JLabel("0 đ");
        lblKhachCanTra.setFont(new Font("Calibri", Font.BOLD, 28)); 
        lblKhachCanTra.setForeground(RED_PASTEL);
        pnlCanTra.add(lblCanTraText, BorderLayout.WEST);
        pnlCanTra.add(lblKhachCanTra, BorderLayout.EAST);

        pnlTTTop.add(lblTitleTT, BorderLayout.NORTH);
        pnlTTTop.add(pnlCanTra, BorderLayout.CENTER);

        JPanel pnlTTCenter = new JPanel(new BorderLayout(0, 8));
        pnlTTCenter.setBackground(Color.WHITE);

        JPanel pnlPhuongThuc = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlPhuongThuc.setBackground(Color.WHITE);
        pnlPhuongThuc.setPreferredSize(new Dimension(0, 36));

        btnTienMat = new NutBoGoc("Tiền mặt"); btnTienMat.setArc(8); btnTienMat.setFont(fontDam.deriveFont(16f));
        btnChuyenKhoan = new NutBoGoc("Chuyển khoản"); btnChuyenKhoan.setArc(8); btnChuyenKhoan.setFont(fontDam.deriveFont(16f));

        btnTienMat.addActionListener(e -> setPhuongThuc(true));
        btnChuyenKhoan.addActionListener(e -> setPhuongThuc(false));
        pnlPhuongThuc.add(btnTienMat); pnlPhuongThuc.add(btnChuyenKhoan);

     // --- Card Tiền mặt ---
        JPanel pnlTienMatLayout = new JPanel(new BorderLayout(0, 10)); 
        pnlTienMatLayout.setBackground(Color.WHITE);

        JPanel pnlNhapTien = new JPanel(new BorderLayout(10, 0));
        pnlNhapTien.setBackground(Color.WHITE);
        
        JLabel lblTienKhachDuaText = new JLabel("Tiền khách đưa:");
        lblTienKhachDuaText.setFont(fontDam.deriveFont(18f));

        txtTienKhachDua = new JTextField();
        txtTienKhachDua.setFont(new Font("Calibri", Font.BOLD, 20)); 
        txtTienKhachDua.setHorizontalAlignment(JTextField.RIGHT);
        txtTienKhachDua.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GRAY_BORDER, 1, true), new EmptyBorder(0, 10, 0, 10)));
        txtTienKhachDua.setPreferredSize(new Dimension(0, 34));
        
        txtTienKhachDua.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tinhTienThua(); }
            public void removeUpdate(DocumentEvent e) { tinhTienThua(); }
            public void changedUpdate(DocumentEvent e) { tinhTienThua(); }
        });

        pnlTienNhanh = new JPanel(new GridLayout(2, 5, 8, 8)); // Tăng khoảng cách (gap) lên 8px cho thoáng
        pnlTienNhanh.setBackground(Color.WHITE);
        // ĐÃ FIX CHÍNH XÁC: Khóa cứng chiều cao của toàn bộ lưới nút (80px), không cho nó phình to ra
        pnlTienNhanh.setPreferredSize(new Dimension(0, 80)); 
        
        long[] menhGiaArr = {1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000};
        
        for (long gia : menhGiaArr) {
            NutBoGoc btn = new NutBoGoc(DinhDangUtil.dinhDangSo(BigDecimal.valueOf(gia)));
            btn.setFont(fontDam.deriveFont(15f)); 
            btn.setColorBackground(Color.WHITE);
            btn.setForeground(TEXT_MAIN);
            btn.setBorder(BorderFactory.createLineBorder(GRAY_BORDER));
            btn.setArc(5);
            btn.addActionListener(e -> {
                long currentAmount = 0;
                String currentText = txtTienKhachDua.getText().replaceAll("[^\\d]", "");
                if (!currentText.isEmpty()) {
                    try { currentAmount = Long.parseLong(currentText); } catch (Exception ignored) {}
                }
                long newAmount = currentAmount + gia;
                txtTienKhachDua.setText(String.valueOf(newAmount));
                txtTienKhachDua.requestFocus(); 
            });
            pnlTienNhanh.add(btn);
        }
        
     // ĐÃ FIX: Xóa chữ NutBoGoc ở đầu, để giá trị khởi tạo là "0 đ"
        btnVuaDu = new NutBoGoc("0 đ"); 
        btnVuaDu.setColorBackground(new Color(220, 252, 231)); 
        btnVuaDu.setForeground(GREEN_PASTEL); // Vẫn giữ nguyên màu xanh lá tuyệt đẹp nha
        btnVuaDu.setArc(5);
        btnVuaDu.setFont(fontDam.deriveFont(15f)); 
        btnVuaDu.addActionListener(e -> {
            txtTienKhachDua.setText(khachCanTraValue.setScale(0, RoundingMode.HALF_UP).toPlainString());
            txtTienKhachDua.requestFocus();
        });
        pnlTienNhanh.add(btnVuaDu);

        pnlNhapTien.add(lblTienKhachDuaText, BorderLayout.WEST);
        pnlNhapTien.add(txtTienKhachDua, BorderLayout.CENTER);

        // ĐÃ FIX: Nhốt lưới nút vào BorderLayout.NORTH của một Panel phụ để chặn đứng việc kéo giãn chiều dọc
        JPanel pnlWrapperTienNhanh = new JPanel(new BorderLayout());
        pnlWrapperTienNhanh.setBackground(Color.WHITE);
        pnlWrapperTienNhanh.add(pnlTienNhanh, BorderLayout.NORTH);

        JPanel pnlTienMatTop = new JPanel(new BorderLayout(0, 15));
        pnlTienMatTop.setBackground(Color.WHITE);
        pnlTienMatTop.add(pnlNhapTien, BorderLayout.NORTH);
        pnlTienMatTop.add(pnlWrapperTienNhanh, BorderLayout.CENTER); // Nhét cái lồng nhốt vào đây

        JPanel pnlThua = new JPanel(new BorderLayout());
        // ... (Phần code pnlThua giữ nguyên bên dưới)
        pnlThua.setBackground(new Color(248, 250, 252));
        pnlThua.setBorder(new EmptyBorder(5, 15, 5, 15));
        
        JLabel lblThuaText = new JLabel("Tiền thừa trả khách:");
        lblThuaText.setFont(fontDam.deriveFont(18f));
        
        lblTienThuaNhanh = new JLabel("0 đ");
        lblTienThuaNhanh.setFont(fontDam.deriveFont(24f)); 
        lblTienThuaNhanh.setForeground(BLUE_PASTEL);
        pnlThua.add(lblThuaText, BorderLayout.WEST);
        pnlThua.add(lblTienThuaNhanh, BorderLayout.EAST);

        pnlTienMatLayout.add(pnlTienMatTop, BorderLayout.CENTER);
        pnlTienMatLayout.add(pnlThua, BorderLayout.SOUTH);
     // --- Card Chuyển khoản (ĐÃ FIX DÀN NGANG) ---
        // Dùng GridLayout(1, 2) để chia đôi: Cột trái (Chữ), Cột phải (QR)
        JPanel pnlChuyenKhoan = new JPanel(new GridLayout(1, 2, 10, 0)); 
        pnlChuyenKhoan.setBackground(Color.WHITE);
        pnlChuyenKhoan.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 2.1 Cột Trái: Chứa Chữ SỐ TIỀN CẦN CHUYỂN
        JPanel pnlTextCK = new JPanel(new GridBagLayout()); // GridBagLayout ép phần tử ra giữa
        pnlTextCK.setBackground(Color.WHITE);
        
        JPanel pnlTextWrapper = new JPanel();
        pnlTextWrapper.setLayout(new BoxLayout(pnlTextWrapper, BoxLayout.Y_AXIS));
        pnlTextWrapper.setBackground(Color.WHITE);
        
        JLabel lblTextCK = new JLabel("SỐ TIỀN CẦN CHUYỂN:", SwingConstants.CENTER);
        lblTextCK.setFont(fontDam.deriveFont(18f)); 
        lblTextCK.setForeground(TEXT_SUB);
        lblTextCK.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        lblSoTienChuyenKhoan = new JLabel("0 đ", SwingConstants.CENTER);
        lblSoTienChuyenKhoan.setFont(new Font("Calibri", Font.BOLD, 30)); 
        lblSoTienChuyenKhoan.setForeground(RED_PASTEL);
        lblSoTienChuyenKhoan.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        pnlTextWrapper.add(lblTextCK); 
        pnlTextWrapper.add(Box.createVerticalStrut(10)); // Thêm khoảng cách giữa 2 chữ
        pnlTextWrapper.add(lblSoTienChuyenKhoan);
        
        pnlTextCK.add(pnlTextWrapper); // Bỏ wrapper vào GridBagLayout để nó tự căn giữa

        // 2.2 Cột Phải: Chứa QR Code
        JLabel lblQR = new JLabel("QR CODE", SwingConstants.CENTER);
        lblQR.setForeground(Color.LIGHT_GRAY);
        lblQR.setBorder(BorderFactory.createLineBorder(GRAY_BORDER));
        try {
            ImageIcon icon = new ImageIcon("Images\\img.png");
            // Scale to ra 220px cho dễ quét
            Image img = icon.getImage().getScaledInstance(220, 220, Image.SCALE_SMOOTH); 
            lblQR.setIcon(new ImageIcon(img));
            lblQR.setText("");
        } catch (Exception e) {}

        pnlChuyenKhoan.add(pnlTextCK);
        pnlChuyenKhoan.add(lblQR);

        cardThanhToan = new CardLayout();
        pnlCardThanhToan = new JPanel(cardThanhToan);
        pnlCardThanhToan.setBackground(Color.WHITE);
        pnlCardThanhToan.add(pnlTienMatLayout, "TIEN_MAT");
        pnlCardThanhToan.add(pnlChuyenKhoan, "CHUYEN_KHOAN");

        pnlTTCenter.add(pnlPhuongThuc, BorderLayout.NORTH);
        pnlTTCenter.add(pnlCardThanhToan, BorderLayout.CENTER);

        pnlThanhToan.add(pnlTTTop, BorderLayout.NORTH);
        pnlThanhToan.add(pnlTTCenter, BorderLayout.CENTER);

        // ==========================================
        // 3. ACTION BUTTONS & LƯỚI LOADING ẢO THUẬT
        // ==========================================
        cardAction = new CardLayout();
        pnlActionContainer = new JPanel(cardAction);
        pnlActionContainer.setOpaque(false);
        pnlActionContainer.setPreferredSize(new Dimension(0, 48)); // Nhích cao lên xíu cho đẹp

        // Card 1: Khu vực 2 nút bấm bình thường
        JPanel pnlButtons = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlButtons.setBackground(BG_MAIN);

        NutBoGoc btnQuayLai = new NutBoGoc("QUAY LẠI");
        btnQuayLai.setColorBackground(YELLOW_QUAY_LAI); btnQuayLai.setForeground(Color.BLACK); btnQuayLai.setArc(10);
        btnQuayLai.addActionListener(e -> { if (hanhDongQuayLai != null) hanhDongQuayLai.run(); });

        NutBoGoc btnInHD = new NutBoGoc("THU TIỀN & IN HĐ");
        btnInHD.setColorBackground(GREEN_PASTEL); btnInHD.setArc(10);
        btnInHD.addActionListener(e -> XuLyThanhToan());

        pnlButtons.add(btnQuayLai);
        pnlButtons.add(btnInHD);

        // Card 2: Hoạt ảnh Loading phong cách Cyberpunk mượt mà
        JPanel pnlLoading = new JPanel(new BorderLayout(0, 5));
        pnlLoading.setBackground(BG_MAIN);
        
        JLabel lblLoadingText = new JLabel("ĐANG BƠM DỮ LIỆU VÀO KHO...", SwingConstants.CENTER);
        lblLoadingText.setFont(fontDam.deriveFont(15f));
        lblLoadingText.setForeground(BLUE_PASTEL);

        JProgressBar progressThanhToan = new JProgressBar();
        progressThanhToan.setIndeterminate(true);
        progressThanhToan.setPreferredSize(new Dimension(0, 6)); // Thanh mảnh khảnh
        progressThanhToan.setBorder(null);
        progressThanhToan.setBackground(GRAY_BORDER);

        // Code vẽ tia sáng chạy qua siêu mượt
        progressThanhToan.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintIndeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(); int h = c.getHeight();
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(BLUE_PASTEL); // Màu xanh chạy qua
                long time = System.currentTimeMillis() / 4; 
                int x = (int) (time % (w + 100)) - 100;
                g2.fillRoundRect(x, 0, 80, h, h, h);
                g2.dispose();
                c.repaint();
            }
        });

        pnlLoading.add(lblLoadingText, BorderLayout.CENTER);
        pnlLoading.add(progressThanhToan, BorderLayout.SOUTH);

        // Nhét 2 thẻ vào lồng kính
        pnlActionContainer.add(pnlButtons, "BUTTONS");
        pnlActionContainer.add(pnlLoading, "LOADING");

        pnlRight.add(pnlKhachHang, BorderLayout.NORTH);
        pnlRight.add(pnlThanhToan, BorderLayout.CENTER);
        pnlRight.add(pnlActionContainer, BorderLayout.SOUTH); // Thay thế pnlAction cũ bằng Container mới

        return pnlRight;
    }
    
    private JPanel taoBlockInfoKH(String title, JLabel lblV) {
        JPanel pnl = new JPanel(new GridLayout(2, 1, 0, 3)); pnl.setOpaque(false);
        JLabel lblT = new JLabel(title); lblT.setFont(fontThuong.deriveFont(Font.ITALIC, 16f)); lblT.setForeground(TEXT_SUB);
        pnl.add(lblT); pnl.add(lblV); return pnl;
    }

    private JTextField taoOTextChuan(String placeholder) {
        JTextField txt = new JTextField(placeholder);
        txt.setFont(fontThuong.deriveFont(Font.ITALIC, 16f)); txt.setForeground(Color.GRAY); 
        txt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GRAY_BORDER, 1, true), new EmptyBorder(0, 10, 0, 10)));
        txt.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (txt.getText().equals(placeholder)) { txt.setText(""); txt.setForeground(TEXT_MAIN); txt.setFont(fontThuong.deriveFont(16f));}
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (txt.getText().isEmpty()) { txt.setForeground(Color.GRAY); txt.setText(placeholder); txt.setFont(fontThuong.deriveFont(Font.ITALIC, 16f));}
            }
        });
        return txt;
    }

    // ĐÃ FIX: Hai nút luôn giữ màu xanh đậm (BTN_BLUE), viền đen in đậm khi chọn
    private void setPhuongThuc(boolean isTienMat) {
        this.isThanhToanTienMat = isTienMat;
        
        btnTienMat.setColorBackground(BTN_BLUE); 
        btnTienMat.setForeground(Color.WHITE); 
        btnChuyenKhoan.setColorBackground(BTN_BLUE); 
        btnChuyenKhoan.setForeground(Color.WHITE); 

        if (isTienMat) {
            btnTienMat.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 2), new EmptyBorder(3, 10, 3, 10)));
            btnChuyenKhoan.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BTN_BLUE, 2), new EmptyBorder(3, 10, 3, 10)));
            if (cardThanhToan != null && pnlCardThanhToan != null) cardThanhToan.show(pnlCardThanhToan, "TIEN_MAT");
        } else {
            btnChuyenKhoan.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 2), new EmptyBorder(3, 10, 3, 10)));
            btnTienMat.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BTN_BLUE, 2), new EmptyBorder(3, 10, 3, 10)));
            if (cardThanhToan != null && pnlCardThanhToan != null) cardThanhToan.show(pnlCardThanhToan, "CHUYEN_KHOAN");
        }
        btnTienMat.repaint(); btnChuyenKhoan.repaint();
        tinhTienThua(); // Kéo tín hiệu sang hóa đơn
    }

    // Hàm 1: Không truyền tham số
    private void tinhTienThua() {
        BigDecimal tienKhachDua = BigDecimal.ZERO;
        try {
            String input = txtTienKhachDua.getText().replaceAll("[^\\d.]", "");
            if (!input.isEmpty()) tienKhachDua = new BigDecimal(input);
        } catch (NumberFormatException ignored) {}

        BigDecimal tienThua = tienKhachDua.subtract(khachCanTraValue);
        
        if (tienThua.compareTo(BigDecimal.ZERO) < 0) {
            lblTienThuaNhanh.setText("0 đ");
        } else {
            BigDecimal phanDuLe = tienThua.remainder(new BigDecimal("1000"));
            BigDecimal tienThoiKhach = tienThua.subtract(phanDuLe);

            if (phanDuLe.compareTo(BigDecimal.ZERO) > 0) {
                // UI mới: Hiển thị tiền thối to rõ màu xanh, và dòng nhỏ màu cam báo tiền lẻ giữ lại két
                lblTienThuaNhanh.setText("<html><div style='text-align:right;'>" + 
                    DinhDangUtil.dinhDangTien(tienThoiKhach) + 
                    "<br><span style='font-size:14px; color:#f59e0b;'>Tiền lẻ vào két: +" + 
                    DinhDangUtil.dinhDangTien(phanDuLe) + "</span></div></html>");
            } else {
                lblTienThuaNhanh.setText(DinhDangUtil.dinhDangTien(tienThua));
            }
        }
        
        if (currentItems != null) tinhToanVaCapNhatUI();
    }

    // Hàm 2: Có truyền tham số
    private void tinhTienThua(BigDecimal giamGiaHang, BigDecimal truTichLuy, int congTichLuy) {
        BigDecimal tienKhachDua = BigDecimal.ZERO;
        try {
            String input = txtTienKhachDua.getText().replaceAll("[^\\d.]", "");
            if (!input.isEmpty()) tienKhachDua = new BigDecimal(input);
        } catch (NumberFormatException ignored) {}

        BigDecimal tienThua = tienKhachDua.subtract(khachCanTraValue); 
        
        if (tienThua.compareTo(BigDecimal.ZERO) < 0) {
            lblTienThuaNhanh.setText("0 đ");
        } else {
            BigDecimal phanDuLe = tienThua.remainder(new BigDecimal("1000"));
            BigDecimal tienThoiKhach = tienThua.subtract(phanDuLe);

            if (phanDuLe.compareTo(BigDecimal.ZERO) > 0) {
                lblTienThuaNhanh.setText("<html><div style='text-align:right;'>" + 
                    DinhDangUtil.dinhDangTien(tienThoiKhach) + 
                    "<br><span style='font-size:14px; color:#f59e0b;'>Tiền lẻ vào két: +" + 
                    DinhDangUtil.dinhDangTien(phanDuLe) + "</span></div></html>");
            } else {
                lblTienThuaNhanh.setText(DinhDangUtil.dinhDangTien(tienThua));
            }
        }
        
        if (currentItems != null) {
            String tenNV = this.tenNhanVienThucTe != null ? this.tenNhanVienThucTe : "Thu Ngân";
            String bacKH = khachHangHienTai != null ? khachHangHienTai.getBacKH() : "Không hạng";
            if (bacKH == null || bacKH.isEmpty()) bacKH = "Không hạng";
            boolean isTienMat = this.isThanhToanTienMat;

            pnlHoaDonPreview.setDuLieuHoaDon(
                this.maHoaDonHienTai, tenNV, this.tenKhachHang, bacKH, 
                tienKhachDua, currentItems, giamGiaHang, truTichLuy, congTichLuy, isTienMat
            );
        }
    }

    private void XuLyThanhToan() {
        if (currentItems == null || currentItems.length == 0) {
            TienIchGiaoDien.hienThiThongBao(this, "Giỏ hàng đang trống, không thể thanh toán!", "WARNING");
            return;
        }

        // ========================================================
        // 1. KIỂM TRA ĐIỀU KIỆN (CHẠY TRÊN LUỒNG UI RẤT NHANH)
        // ========================================================
        try {
            Logic.ChiaCaLogic ccLogic = new Logic.ChiaCaLogic();
            if (!ccLogic.kiemTraNhanVienDangTrongCa(this.maNhanVienThucTe)) {
                TienIchGiaoDien.hienThiThongBao(this, "Bạn không thuộc ca làm việc hiện tại hoặc chưa bấm 'Có mặt' điểm danh!\nKhông thể thực hiện thanh toán.", "ERROR");
                return; 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isTienMat = this.isThanhToanTienMat;
        BigDecimal tienKhachDua = BigDecimal.ZERO;

        if (isTienMat) {
            if (txtTienKhachDua.getText().isEmpty()) {
                TienIchGiaoDien.hienThiThongBao(this, "Bạn chưa nhập tiền khách đưa!", "WARNING");
                txtTienKhachDua.requestFocus(); return;
            }
            try {
                tienKhachDua = new BigDecimal(txtTienKhachDua.getText().replaceAll("[^\\d.]", ""));
                if (tienKhachDua.compareTo(khachCanTraValue) < 0) {
                    TienIchGiaoDien.hienThiThongBao(this, "Khách đưa chưa đủ tiền!", "ERROR"); return;
                }
            } catch (Exception e) {
                TienIchGiaoDien.hienThiThongBao(this, "Số tiền khách đưa không hợp lệ!", "ERROR"); return;
            }
        } else {
            tienKhachDua = khachCanTraValue; 
        }

        // Tính toán các con số Final
        Logic.KhachHangLogic khLogic = new Logic.KhachHangLogic();
        BigDecimal phanTram = khachHangHienTai != null ? khLogic.layPhanTramGiamGia(khachHangHienTai.getBacKH()) : BigDecimal.ZERO;
        BigDecimal tongGiamGiaHang = tongTienHoaDon.multiply(phanTram);
        BigDecimal finalTruTichDiem = BigDecimal.ZERO;

        if (khachHangHienTai != null && radDungDiemCo.isSelected()) {
            BigDecimal tienSauKhiGiamHang = tongTienHoaDon.subtract(tongGiamGiaHang);
            int diemHienCo = khachHangHienTai.getDiemTichLuy() != null ? khachHangHienTai.getDiemTichLuy().intValue() : 0;
            finalTruTichDiem = tienSauKhiGiamHang.min(new BigDecimal(diemHienCo)); 
        }

        BigDecimal tongCanThanhToanThucTe = tongTienHoaDon.subtract(tongGiamGiaHang).subtract(finalTruTichDiem);
        if (tongCanThanhToanThucTe.compareTo(BigDecimal.ZERO) < 0) tongCanThanhToanThucTe = BigDecimal.ZERO;

        BigDecimal tienThua = isTienMat ? tienKhachDua.subtract(khachCanTraValue) : BigDecimal.ZERO;
        
        final String maHDMoi = this.maHoaDonHienTai;
        final BigDecimal tienKhachDuaFinal = tienKhachDua;
        final BigDecimal truDiemFinal = finalTruTichDiem;
        final BigDecimal thanhToanFinal = tongCanThanhToanThucTe;

        // ========================================================
        // 2. HIỆU ỨNG UI: KHÓA GIAO DIỆN (CHỐNG DOUBLE CLICK)
        // ========================================================
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        // 🪄 Úm ba la xì bùa: Đổi nút bấm thành thanh Loading!
        cardAction.show(pnlActionContainer, "LOADING");
        // ========================================================
        // 3. SWING WORKER: BƠM DỮ LIỆU SIÊU TỐC VÀO DB
        // ========================================================
        // ========================================================
        // 3. SWING WORKER: GỌI ĐỘNG CƠ "BATCH TRANSACTION" 🚀
        // ========================================================
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String errorMsg = "";

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // 1. Chuẩn bị Dữ liệu Hóa Đơn
                    Data.HoaDon hd = new Data.HoaDon.ThoXayHoaDon()
                        .ganMaHD(maHDMoi).ganMaKH(khachHangHienTai != null ? khachHangHienTai.getMaKH() : null)
                        .ganMaNV(maNhanVienThucTe).ganThanhTien(tongTienHoaDon).ganTongGiamGia(tongGiamGiaHang)
                        .ganTruTichDiem(truDiemFinal).ganPhuongThucTT(isTienMat ? "Tiền mặt" : "Chuyển khoản")
                        .ganKhachDua(tienKhachDuaFinal).ganTienThua(tienThua).ganGhiChu("Mua trực tiếp tại quầy")
                        .taoMoi();

                    // 2. Chuẩn bị Dữ liệu Chi Tiết
                    // 2. Chuẩn bị Dữ liệu Chi Tiết
                    List<Data.ChiTietHoaDon> danhSachChiTiet = new ArrayList<>();
                    
                    // Kéo DAO ra để làm việc đàng hoàng, không lách luật nữa!
                    Dao.ChiTietLoHangDAO ctLoHangDAO = Dao.ChiTietLoHangDAO.getInstance();

                    for (Object[] item : currentItems) {
                        String maSP = item[5].toString(); 
                        int soLuong = (int) item[2];
                        BigDecimal donGia = (BigDecimal) item[3]; 
                        BigDecimal thanhTienSP = (BigDecimal) item[4];
                        
                        String maLoHople = "LH001"; // Default dự phòng
                        
                        // ⚡ TRỊ BỆNH Ở ĐÂY: Truy xuất DB để lấy mã lô hàng thực tế có đủ tồn kho!
                        List<Object[]> dsLo = ctLoHangDAO.layLichSuNhapTheoSP(maSP); 
                        if (dsLo != null && !dsLo.isEmpty()) {
                            for (Object[] lo : dsLo) {
                                // Trong layLichSuNhapTheoSP: lo[0] = MaLoHang, lo[5] = SoLuongTon
                                if ((int)lo[5] >= soLuong) { 
                                    maLoHople = lo[0].toString(); 
                                    ctLoHangDAO.truSoLuongTon(maLoHople, maSP, soLuong);
                                    break; // Tìm thấy lô đủ hàng là chốt đơn lô này!
                                }
                            }
                        }
                        
                        // Đóng gói với mã lô HỢP LỆ
                        danhSachChiTiet.add(new Data.ChiTietHoaDon.ThoXayChiTietHoaDon()
                            .ganMaHD(maHDMoi).ganMaSp(maSP).ganMaLoHang(maLoHople) 
                            .ganSoLuong(soLuong).ganDonGia(donGia).ganThanhTienSanPham(thanhTienSP).taoMoi());
                    }
                    // 3. Tính toán Tiền Ca Làm & Khách hàng
                    BigDecimal chenhLechGiaoDich = BigDecimal.ZERO;
                    if (isTienMat) {
                        BigDecimal tienThuaThucTe = tienKhachDuaFinal.subtract(thanhToanFinal);
                        if (tienThuaThucTe.compareTo(BigDecimal.ZERO) > 0) {
                            chenhLechGiaoDich = tienThuaThucTe.remainder(new BigDecimal("1000")); 
                        }
                    }

                    if (khachHangHienTai != null) {
                        int diemMoi = khachHangHienTai.getDiemTichLuy().intValue() - truDiemFinal.intValue();
                        // Cộng thêm 1% điểm tích lũy từ đơn hàng này (tùy logic của bạn)
                        diemMoi += thanhToanFinal.multiply(new BigDecimal("0.01")).intValue();
                        khachHangHienTai.setDiemTichLuy(new BigDecimal(diemMoi));
                    }

                    // 🌟 4. KÍCH HOẠT ĐỘNG CƠ: Chạy 1 phát ăn ngay!
                    Dao.TruyVanSieuTocDAO.getInstance().thanhToanGopChungSieuToc(
                        hd, danhSachChiTiet, khachHangHienTai, maNhanVienThucTe, thanhToanFinal, chenhLechGiaoDich
                    );

                    success = true;
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                cardAction.show(pnlActionContainer, "BUTTONS"); 
                
                if (success) {
                    TienIchGiaoDien.hienThiThongBao(ThanhToanUi.this, "THANH TOÁN THÀNH CÔNG!\nMã Hóa Đơn: " + maHDMoi, "SUCCESS");
                    lamMoiThongTinKhachHang();
                    if (hanhDongThanhToanThanhCong != null) hanhDongThanhToanThanhCong.run(); 
                } else {
                    TienIchGiaoDien.hienThiThongBao(ThanhToanUi.this, "Lỗi hệ thống: " + errorMsg, "ERROR");
                }
            }
        };
        worker.execute(); // Bắt đầu chạy ngầm!
    }
    private void loadDuLieuMau() {
        currentItems = new Object[][]{ {"Kẹo Chupa Chups", "Cây", 1, new BigDecimal("5000"), new BigDecimal("5000"), "SP002"} };
        tongTienHoaDon = new BigDecimal("5000");
        lblKhachCanTra.setText(DinhDangUtil.dinhDangTien(tongTienHoaDon));
        
        // ĐÃ FIX: Sửa thành chuỗi rỗng để ô nhập tiền trắng tinh lúc mới mở
        txtTienKhachDua.setText(""); 
        
        tinhTienThua();
    }

    private Runnable hanhDongQuayLai; private Runnable hanhDongThanhToanThanhCong;
    public void setHanhDongQuayLai(Runnable hanhDongQuayLai) { this.hanhDongQuayLai = hanhDongQuayLai; }
    public void setHanhDongThanhToanThanhCong(Runnable r) { this.hanhDongThanhToanThanhCong = r; }
    
    public void nhanDuLieuTuGioHang(Object[][] dsSanPham, BigDecimal tongTien) {
        this.currentItems = dsSanPham; this.tongTienHoaDon = tongTien;
        this.maHoaDonHienTai = Logic.TaoMaTuDongLogic.taoMaHoaDon();
        lblKhachCanTra.setText(DinhDangUtil.dinhDangTien(tongTienHoaDon));
        txtTienKhachDua.setText(""); lblTienThuaNhanh.setText("0 đ");
        if (pnlHoaDonPreview != null) tinhToanVaCapNhatUI();   
    }

    private void xuLyTimKiemKhachHang() {
        String sdt = txtSearch.getText().trim();
        if (sdt.isEmpty() || sdt.equals("Tìm khách hàng bằng SĐT...")) {
            TienIchGiaoDien.hienThiThongBao(this, "Vui lòng nhập số điện thoại cần tìm!", "WARNING");
            txtSearch.requestFocus(); return;
        }

        try {
            Logic.KhachHangLogic khLogic = new Logic.KhachHangLogic();
            Data.KhachHang kh = khLogic.layKhachHangTheoSDT(sdt);

            if (kh != null) {
                this.khachHangHienTai = kh; this.tenKhachHang = kh.getHoTen();
                lblTenKHValue.setText(kh.getHoTen());
                String hang = (kh.getBacKH() == null || kh.getBacKH().isEmpty()) ? "Không hạng" : kh.getBacKH();
                lblHangTVValue.setText(hang);
                
                BigDecimal phanTram = khLogic.layPhanTramGiamGia(hang); 
                BigDecimal phanTramHienThi = phanTram.multiply(new BigDecimal("100")).setScale(0, java.math.RoundingMode.HALF_UP);
                lblGiamGiaValue.setText(phanTramHienThi.toPlainString() + "%");
                
                BigDecimal diem = kh.getDiemTichLuy() != null ? kh.getDiemTichLuy() : BigDecimal.ZERO;
                lblDiemValue.setText(DinhDangUtil.dinhDangSo(diem));

                TienIchGiaoDien.hienThiThongBao(this, "Đã áp dụng khách hàng: " + kh.getHoTen(), "SUCCESS");
                if (pnlHoaDonPreview != null && currentItems != null) tinhToanVaCapNhatUI();
            } else { throw new Exception("Không tìm thấy"); }
        } catch (Exception ex) {
            TienIchGiaoDien.hienThiThongBao(this, "Không tìm thấy khách hàng với SĐT này!", "ERROR");
            this.khachHangHienTai = null; this.tenKhachHang = "Khách vãng lai";
            lblTenKHValue.setText(this.tenKhachHang); lblHangTVValue.setText("---");
            lblGiamGiaValue.setText("0%"); lblDiemValue.setText("0");
            if (pnlHoaDonPreview != null && currentItems != null) tinhToanVaCapNhatUI();
        }
    }

    private boolean dangTinhToan = false; 
    
    private void tinhToanVaCapNhatUI() {
        if (dangTinhToan) return;
        dangTinhToan = true;
        try {
            BigDecimal giamGiaHang = BigDecimal.ZERO;
            BigDecimal giamGiaDiem = BigDecimal.ZERO;
            int diemHienTai = 0;
            int diemTuDonHang = tongTienHoaDon.multiply(new BigDecimal("0.01")).intValue();

            if (khachHangHienTai != null) {
                radDungDiemCo.setEnabled(true); radDungDiemKhong.setEnabled(true);
                diemHienTai = khachHangHienTai.getDiemTichLuy() != null ? khachHangHienTai.getDiemTichLuy().intValue() : 0;

                lblDiemValue.setText(DinhDangUtil.dinhDangSo(new BigDecimal(diemHienTai + diemTuDonHang))); 

                Logic.KhachHangLogic khLogic = new Logic.KhachHangLogic();
                BigDecimal phanTram = khLogic.layPhanTramGiamGia(khachHangHienTai.getBacKH());
                giamGiaHang = tongTienHoaDon.multiply(phanTram);
                
                if (radDungDiemCo.isSelected()) {
                    giamGiaDiem = tongTienHoaDon.subtract(giamGiaHang).min(new BigDecimal(diemHienTai));
                }
            } else {
                radDungDiemKhong.setSelected(true); 
                radDungDiemCo.setEnabled(false); radDungDiemKhong.setEnabled(false);
                lblDiemValue.setText("0"); 
                giamGiaHang = BigDecimal.ZERO; giamGiaDiem = BigDecimal.ZERO; 
            }

            BigDecimal tongGiamGia = giamGiaHang.add(giamGiaDiem);
            khachCanTraValue = tongTienHoaDon.subtract(tongGiamGia);
            if(khachCanTraValue.compareTo(BigDecimal.ZERO) < 0) khachCanTraValue = BigDecimal.ZERO;
            
            lblKhachCanTra.setText(DinhDangUtil.dinhDangTien(khachCanTraValue));
            if (lblSoTienChuyenKhoan != null) lblSoTienChuyenKhoan.setText(DinhDangUtil.dinhDangTien(khachCanTraValue));
            if (btnVuaDu != null) btnVuaDu.setText(DinhDangUtil.dinhDangTien(khachCanTraValue));
            tinhTienThua(giamGiaHang, giamGiaDiem, khachHangHienTai != null ? diemTuDonHang : 0);
        } finally {
            dangTinhToan = false;
        }
    }

    private void lamMoiThongTinKhachHang() {
        this.khachHangHienTai = null; this.tenKhachHang = "Khách vãng lai";
        lblTenKHValue.setText(this.tenKhachHang); lblHangTVValue.setText("---");
        lblGiamGiaValue.setText("0%"); lblDiemValue.setText("0");
        if (txtSearch != null) { txtSearch.setText("Tìm khách hàng bằng SĐT..."); txtSearch.setForeground(Color.GRAY); }
        if (radDungDiemKhong != null) radDungDiemKhong.setSelected(true);
        if (txtTienKhachDua != null) txtTienKhachDua.setText("");
        if (lblTienThuaNhanh != null) lblTienThuaNhanh.setText("0 đ");
    }
    
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Thanh Toán Bán Hàng - Đã Chuẩn Hóa Big Decimal");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setSize(1200, 900); 
            frame.setLocationRelativeTo(null); frame.add(new ThanhToanUi()); frame.setVisible(true);
        });
    }
}
