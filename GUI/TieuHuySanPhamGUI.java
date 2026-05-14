package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Data.ChiTietLoHang;
import Data.PhieuTieuHuy;
import Data.ChiTietPhieuHuy;
import Data.SanPham;
import Logic.ChiTietPhieuHuyLogic;
import Logic.PhieuTieuHuyLogic;
import Logic.SanPhamLogic;
import Dao.ChiTietLoHangDAO;

public class TieuHuySanPhamGUI extends JPanel {

    // ==========================================
    // BẢNG MÀU HIỆN ĐẠI (MODERN PASTEL & NEUTRAL)
    // ==========================================
    private static final Color BG_COLOR = new Color(245, 247, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_MAIN = new Color(44, 62, 80);
    private static final Color TEXT_MUTED = new Color(127, 140, 141);
    private static final Color PRIMARY_COLOR = new Color(52, 152, 219);
    
    // Màu trạng thái
    private static final Color COLOR_EXPIRED_BG = new Color(253, 237, 237);
    private static final Color COLOR_EXPIRED_TEXT = new Color(211, 47, 47);
    private static final Color COLOR_WARNING_BG = new Color(255, 244, 229);
    private static final Color COLOR_WARNING_TEXT = new Color(230, 81, 0);
    private static final Color COLOR_LOST_BG = new Color(255, 243, 224);
    private static final Color COLOR_LOST_TEXT = new Color(230, 81, 0);
    private static final Color COLOR_NORMAL_BG = new Color(237, 247, 237);
    private static final Color COLOR_NORMAL_TEXT = new Color(46, 125, 50);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0 VNĐ");

    // ==========================================
    // COMPONENTS
    // ==========================================
    private JPanel pnlDanhSachCard; // Chứa các thẻ sản phẩm
    private JTextField txtTimKiem;
    private JComboBox<String> cbBoLoc, cbSapXep;
    
    // Chi tiết bên phải
    private JLabel lblHinhAnh, lblTenSP, lblMaLo, lblHSD, lblTonKho, lblGiaNhap;
    private PanelBoGoc pnlTrangThai;
    private JLabel lblTrangThaiText;
    private JTextField txtSoLuongHuy;
    private JComboBox<String> cbLyDoHuy;
    private JTextArea txtLyDoKhac;
    private JLabel lblTongThietHai;
    private JButton btnHoanTat, btnLuuTam;
    private JLabel lblStatHetHan;
    private JLabel lblStatCanDate;
    private JLabel lblStatThatThoat;
    private JLabel lblStatThietHai;

    private ChiTietLoHang loHangDangChon;
    private String tenSpDangChon = "";
    private List<ChiTietLoHang> danhSachGocCache = new ArrayList<>();
    private Map<String, String> mapTenSanPham = new HashMap<>();
    // Timer cho Debounce Search
    private Timer debounceTimer;

    public TieuHuySanPhamGUI() {
        khoiTaoGiaoDien();
        setupDebounceSearch();
        taiDanhSachHangCanHuyAsync(); // Load dữ liệu bất đồng bộ
    }

    private void khoiTaoGiaoDien() {
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // 1. HEADER & THỐNG KÊ
        add(taoHeaderVaThongKe(), BorderLayout.NORTH);

        // 2. MAIN SPLIT (Trái: Danh sách, Phải: Chi tiết)
        JPanel pnlMain = new JPanel(new BorderLayout(20, 0));
        pnlMain.setBackground(BG_COLOR);

        pnlMain.add(taoPanelTraiDanhSach(), BorderLayout.CENTER);
        pnlMain.add(taoPanelPhaiChiTiet(), BorderLayout.EAST);

        add(pnlMain, BorderLayout.CENTER);
    }

    // =====================================================================
    // VÙNG 1: HEADER VÀ THỐNG KÊ TỔNG QUAN
    // =====================================================================
    private JPanel taoHeaderVaThongKe() {
        JPanel pnlTop = new JPanel(new BorderLayout(0, 15));
        pnlTop.setBackground(BG_COLOR);

        JLabel lblTitle = new JLabel("QUẢN LÝ TIÊU HỦY & THẤT THOÁT");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(TEXT_MAIN);
        pnlTop.add(lblTitle, BorderLayout.NORTH);

        // Khởi tạo các Label với giá trị mặc định lúc đang tải
        lblStatHetHan = new JLabel("0 Lô");
        lblStatCanDate = new JLabel("0 Lô");
        lblStatThatThoat = new JLabel("0 Phiếu");
        lblStatThietHai = new JLabel("0 đ");

        // Panel thống kê nhanh (4 Cards)
        JPanel pnlStats = new JPanel(new GridLayout(1, 4, 15, 0));
        pnlStats.setBackground(BG_COLOR);
        
        pnlStats.add(taoCardThongKe("Hàng Hết Hạn", lblStatHetHan, COLOR_EXPIRED_TEXT, COLOR_EXPIRED_BG));
        pnlStats.add(taoCardThongKe("Cận Date", lblStatCanDate, COLOR_WARNING_TEXT, COLOR_WARNING_BG));
        pnlStats.add(taoCardThongKe("Đã hủy (Tháng)", lblStatThatThoat, COLOR_LOST_TEXT, COLOR_LOST_BG)); 
        pnlStats.add(taoCardThongKe("Ước tính thiệt hại", lblStatThietHai, TEXT_MAIN, Color.WHITE));

        pnlTop.add(pnlStats, BorderLayout.CENTER);
        return pnlTop;
    }

    private PanelBoGoc taoCardThongKe(String title, JLabel lblValue, Color valueColor, Color bgColor) {
        PanelBoGoc card = new PanelBoGoc(15);
        card.setBackground(bgColor);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblT = new JLabel(title);
        lblT.setFont(FONT_SMALL);
        lblT.setForeground(TEXT_MUTED);
        
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblValue.setForeground(valueColor);

        card.add(lblT);
        card.add(Box.createVerticalStrut(5));
        card.add(lblValue);
        return card;
    }

    // =====================================================================
    // VÙNG 2: SIDEBAR DANH SÁCH (TRÁI)
    // =====================================================================
    private JPanel taoPanelTraiDanhSach() {
        JPanel pnlTrai = new JPanel(new BorderLayout(0, 10));
        pnlTrai.setBackground(BG_COLOR);

        // Thanh công cụ tìm kiếm và lọc
        JPanel pnlFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlFilter.setBackground(BG_COLOR);

        txtTimKiem = new JTextField(20);
        txtTimKiem.setFont(FONT_NORMAL);
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tìm SP, Mã lô...");

        cbBoLoc = new JComboBox<>(new String[]{"Tất cả trạng thái", "Hết hạn", "Cận date", "Bình thường"});
        cbSapXep = new JComboBox<>(new String[]{"Sắp xếp: HSD Gần nhất", "Sắp xếp: Tồn kho giảm", "Sắp xếp: Giá trị giảm"});

        cbBoLoc.setFont(FONT_NORMAL); 
        cbSapXep.setFont(FONT_NORMAL);

        // 🔥 GẮN SỰ KIỆN: Hễ đổi lựa chọn là gọi thuật toán tìm kiếm/lọc ngay lập tức
        cbBoLoc.addActionListener(e -> timKiemRealtime());
        cbSapXep.addActionListener(e -> timKiemRealtime());

        pnlFilter.add(txtTimKiem);
        pnlFilter.add(cbBoLoc);
        pnlFilter.add(cbSapXep);

        // Khu vực chứa Card Item
        pnlDanhSachCard = new JPanel();
        pnlDanhSachCard.setLayout(new BoxLayout(pnlDanhSachCard, BoxLayout.Y_AXIS));
        pnlDanhSachCard.setBackground(BG_COLOR);

        JScrollPane scroll = new JScrollPane(pnlDanhSachCard);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(BG_COLOR);

        pnlTrai.add(pnlFilter, BorderLayout.NORTH);
        pnlTrai.add(scroll, BorderLayout.CENTER);

        return pnlTrai;
    }

    // =====================================================================
    // VÙNG 3: PANEL CHI TIẾT & FORM TIÊU HỦY (PHẢI)
    // =====================================================================
    private PanelBoGoc taoPanelPhaiChiTiet() {
        PanelBoGoc pnlPhai = new PanelBoGoc(20);
        pnlPhai.setBackground(CARD_BG);
        pnlPhai.setPreferredSize(new Dimension(450, 0));
        pnlPhai.setLayout(new BorderLayout());
        pnlPhai.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel pnlContent = new JPanel();
        pnlContent.setLayout(new BoxLayout(pnlContent, BoxLayout.Y_AXIS));
        pnlContent.setBackground(CARD_BG);

        // 3.1 Thông tin sản phẩm
        JLabel lblTitleCT = new JLabel("CHI TIẾT LÔ HÀNG");
        lblTitleCT.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitleCT.setForeground(PRIMARY_COLOR);

        lblTenSP = new JLabel("Chọn một sản phẩm để xem...");
        lblTenSP.setFont(FONT_TITLE);
        
        lblMaLo = new JLabel("Lô: --- | Tồn kho: ---");
        lblMaLo.setFont(FONT_NORMAL);
        lblMaLo.setForeground(TEXT_MUTED);

        lblHSD = new JLabel("HSD: ---");
        lblHSD.setFont(FONT_NORMAL);
        lblGiaNhap = new JLabel("Giá nhập: ---");
        lblGiaNhap.setFont(FONT_NORMAL);

        pnlTrangThai = new PanelBoGoc(10);
        pnlTrangThai.setBackground(COLOR_NORMAL_BG);
        pnlTrangThai.setBorder(new EmptyBorder(5, 10, 5, 10));
        lblTrangThaiText = new JLabel("Bình thường");
        lblTrangThaiText.setFont(FONT_BOLD);
        pnlTrangThai.add(lblTrangThaiText);

        pnlContent.add(lblTitleCT);
        pnlContent.add(Box.createVerticalStrut(15));
        pnlContent.add(lblTenSP);
        pnlContent.add(Box.createVerticalStrut(5));
        pnlContent.add(lblMaLo);
        pnlContent.add(Box.createVerticalStrut(5));
        pnlContent.add(lblHSD);
        pnlContent.add(Box.createVerticalStrut(5));
        pnlContent.add(lblGiaNhap);
        pnlContent.add(Box.createVerticalStrut(10));
        pnlContent.add(pnlTrangThai);
        pnlContent.add(Box.createVerticalStrut(30));

        // 3.2 Form nhập tiêu hủy
        pnlContent.add(new JSeparator());
        pnlContent.add(Box.createVerticalStrut(20));

        JLabel lblFormTitle = new JLabel("THAO TÁC TIÊU HỦY");
        lblFormTitle.setFont(FONT_BOLD);
        pnlContent.add(lblFormTitle);
        pnlContent.add(Box.createVerticalStrut(15));

        // KHÓA CHIỀU CAO TỐI ĐA CHO CÁC Ô NHẬP LIỆU (Khoảng 35px là đẹp)
        Dimension inputSize = new Dimension(Integer.MAX_VALUE, 35);
        Dimension labelSize = new Dimension(100, 35); // Cố định độ rộng nhãn để canh lề thẳng nhau

        // --- Số lượng ---
        JPanel pnlSL = new JPanel(new BorderLayout(10, 0));
        pnlSL.setBackground(CARD_BG);
        pnlSL.setMaximumSize(inputSize); // ÉP CHIỀU CAO
        
        JLabel lblSLTitle = new JLabel("Số lượng hủy:");
        lblSLTitle.setPreferredSize(labelSize);
        pnlSL.add(lblSLTitle, BorderLayout.WEST);
        
        txtSoLuongHuy = new JTextField();
        txtSoLuongHuy.setFont(FONT_BOLD);
        txtSoLuongHuy.setEnabled(false); // Disable khi chưa chọn SP
        // Thêm viền và padding nhẹ cho đẹp
        txtSoLuongHuy.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 10, 5, 10)));
        pnlSL.add(txtSoLuongHuy, BorderLayout.CENTER);
        
        // Cập nhật thiệt hại realtime khi nhập
        txtSoLuongHuy.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tinhGiaTriThietHai(); }
            public void removeUpdate(DocumentEvent e) { tinhGiaTriThietHai(); }
            public void changedUpdate(DocumentEvent e) { tinhGiaTriThietHai(); }
        });

        // --- Lý do ---
        JPanel pnlLyDo = new JPanel(new BorderLayout(10, 0));
        pnlLyDo.setBackground(CARD_BG);
        pnlLyDo.setMaximumSize(inputSize); // ÉP CHIỀU CAO
        
        JLabel lblLyDoTitle = new JLabel("Lý do:");
        lblLyDoTitle.setPreferredSize(labelSize);
        pnlLyDo.add(lblLyDoTitle, BorderLayout.WEST);
        
        cbLyDoHuy = new JComboBox<>(new String[]{"Hàng hết hạn", "Hư hỏng/Móp méo", "Nấm mốc", "Thất thoát/Mất", "Khác..."});
        cbLyDoHuy.setEnabled(false);
        pnlLyDo.add(cbLyDoHuy, BorderLayout.CENTER);

        // --- Lý do khác (JTextArea) ---
        txtLyDoKhac = new JTextArea(3, 20);
        txtLyDoKhac.setLineWrap(true);
        txtLyDoKhac.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        txtLyDoKhac.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70)); // Ép chiều cao tối đa cho JTextArea
        txtLyDoKhac.setVisible(false);

        cbLyDoHuy.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                txtLyDoKhac.setVisible(cbLyDoHuy.getSelectedItem().equals("Khác..."));
                revalidate();
            }
        });

        // Add các thành phần vào pnlContent
        pnlContent.add(pnlSL);
        pnlContent.add(Box.createVerticalStrut(15));
        pnlContent.add(pnlLyDo);
        pnlContent.add(Box.createVerticalStrut(10));
        pnlContent.add(txtLyDoKhac);
        
        // Đẩy phần thiệt hại xuống dưới cùng (Thêm keo co giãn)
        pnlContent.add(Box.createVerticalGlue()); 
        pnlContent.add(Box.createVerticalStrut(10));

        // Hiển thị thiệt hại
        PanelBoGoc pnlThietHai = new PanelBoGoc(15);
        pnlThietHai.setBackground(COLOR_EXPIRED_BG);
        pnlThietHai.setLayout(new BorderLayout());
        pnlThietHai.setBorder(new EmptyBorder(15, 20, 15, 20)); // Tăng padding trái phải cho thanh thoát
        
        // 🔥 CHÌA KHÓA Ở ĐÂY: Khóa chết chiều cao tối đa (65px) để ô không bị phình to!
        pnlThietHai.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65)); 

        JLabel lblThietHaiTitle = new JLabel("Tổng thiệt hại ước tính:");
        lblThietHaiTitle.setFont(FONT_NORMAL);
        lblThietHaiTitle.setForeground(COLOR_EXPIRED_TEXT);
        
        lblTongThietHai = new JLabel("0 VNĐ");
        lblTongThietHai.setFont(new Font("Segoe UI", Font.BOLD, 22)); // Tăng size nhẹ cho nổi bật
        lblTongThietHai.setForeground(COLOR_EXPIRED_TEXT);
        lblTongThietHai.setHorizontalAlignment(SwingConstants.RIGHT); // Ép text tiền căn phải

        // Đặt Tiêu đề bên trái (WEST), Tiền bên phải (CENTER tự chiếm không gian còn lại và đẩy text sang phải)
        pnlThietHai.add(lblThietHaiTitle, BorderLayout.WEST);
        pnlThietHai.add(lblTongThietHai, BorderLayout.CENTER);
        
        pnlContent.add(pnlThietHai);
        pnlContent.add(Box.createVerticalStrut(15));

        pnlPhai.add(pnlContent, BorderLayout.CENTER);

        // 3.3 Buttons
        JPanel pnlAction = new JPanel(new GridLayout(1, 2, 15, 0)); // Tăng khoảng cách 2 nút ra 15px cho thoáng
        pnlAction.setBackground(CARD_BG);
        pnlAction.setBorder(new EmptyBorder(10, 0, 0, 0)); // Cách phần trên một chút
        
        // 💡 Cách 1: Dùng hàm taoNutHienDai từ TienIchGiaoDien (Giao diện bo góc cực mượt, hover xịn)
        // Lưu ý: Đảm bảo bạn đã import GUI.HoTro.TienIchGiaoDien; ở đầu file nhé!
        
        btnLuuTam = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("Lưu Tạm", new Color(149, 165, 166)); // Màu xám thanh lịch
        
        btnHoanTat = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("Hoàn Tất Hủy", COLOR_EXPIRED_TEXT); // Màu đỏ cảnh báo nổi bật

        // Thêm sự kiện cho nút
        btnHoanTat.addActionListener(e -> xuLyHoanTatTieuHuy());

        pnlAction.add(btnLuuTam);
        pnlAction.add(btnHoanTat);
        pnlPhai.add(pnlAction, BorderLayout.SOUTH);

        return pnlPhai;
    }

    // =====================================================================
    // LOGIC NGHIỆP VỤ & TƯƠNG TÁC GIAO DIỆN
    // =====================================================================

    /**
     * Debounce tìm kiếm: Giảm tải DB khi gõ liên tục
     */
    private void setupDebounceSearch() {
        debounceTimer = new Timer(500, e -> timKiemRealtime());
        debounceTimer.setRepeats(false);
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounceTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { debounceTimer.restart(); }
            public void changedUpdate(DocumentEvent e) { debounceTimer.restart(); }
        });
    }

    private void timKiemRealtime() {
        if (danhSachGocCache == null || danhSachGocCache.isEmpty()) return;

        // 1. LẤY CÁC THAM SỐ TỪ GIAO DIỆN
        String tuKhoa = txtTimKiem.getText();
        String tuKhoaThuong = "";
        if (tuKhoa != null && !tuKhoa.trim().isEmpty() && !tuKhoa.equals("Tìm SP, Mã lô...")) {
            tuKhoaThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tuKhoa.toLowerCase().trim());
        }

        String boLoc = (String) cbBoLoc.getSelectedItem();
        String sapXep = (String) cbSapXep.getSelectedItem();
        LocalDate today = LocalDate.now();

        List<ChiTietLoHang> dsLoc = new ArrayList<>();

        // 2. GIAI ĐOẠN 1: LỌC DATA (FILTER)
        for (ChiTietLoHang lo : danhSachGocCache) {
            
            // --- Lọc theo từ khóa (Tìm Text) ---
            boolean matchTuKhoa = true;
            if (!tuKhoaThuong.isEmpty()) {
                String tenSP = mapTenSanPham.getOrDefault(lo.getMaSP(), "Không xác định");
                String tenSPThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tenSP.toLowerCase());
                String maLoThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(lo.getMaLoHang().toLowerCase());

                if (!tenSPThuong.contains(tuKhoaThuong) && !maLoThuong.contains(tuKhoaThuong)) {
                    matchTuKhoa = false;
                }
            }
            if (!matchTuKhoa) continue; // Bỏ qua nếu không khớp text

            // --- Lọc theo Trạng Thái (Combobox) ---
            boolean matchTrangThai = true;
            if (boLoc != null && !boLoc.equals("Tất cả trạng thái")) {
                // Tính số ngày còn lại
                long days = (lo.getHSD() != null) ? ChronoUnit.DAYS.between(today, lo.getHSD()) : 9999;
                
                if (boLoc.equals("Hết hạn") && days >= 0) matchTrangThai = false;
                else if (boLoc.equals("Cận date") && (days < 0 || days > 7)) matchTrangThai = false;
                else if (boLoc.equals("Bình thường") && days <= 7) matchTrangThai = false;
            }

            if (matchTrangThai) {
                dsLoc.add(lo); // Pass hết bài test thì đưa vào danh sách hiển thị
            }
        }

        // 3. GIAI ĐOẠN 2: SẮP XẾP DATA (SORT)
        if (sapXep != null && !dsLoc.isEmpty()) {
            dsLoc.sort((lo1, lo2) -> {
                if (sapXep.equals("Sắp xếp: HSD Gần nhất")) {
                    // Xử lý an toàn nếu HSD bị null
                    if (lo1.getHSD() == null && lo2.getHSD() == null) return 0;
                    if (lo1.getHSD() == null) return 1;
                    if (lo2.getHSD() == null) return -1;
                    // Tăng dần, ngày nào gần hôm nay nhất sẽ lên đầu
                    return lo1.getHSD().compareTo(lo2.getHSD()); 
                    
                } else if (sapXep.equals("Sắp xếp: Tồn kho giảm")) {
                    return Integer.compare(lo2.getSoLuongTon(), lo1.getSoLuongTon());
                    
                } else if (sapXep.equals("Sắp xếp: Giá trị giảm")) {
                    BigDecimal gt1 = lo1.getGiaNhap().multiply(new BigDecimal(lo1.getSoLuongTon()));
                    BigDecimal gt2 = lo2.getGiaNhap().multiply(new BigDecimal(lo2.getSoLuongTon()));
                    return gt2.compareTo(gt1);
                }
                return 0;
            });
        }

        // 4. ĐẨY DATA LÊN GIAO DIỆN
        hienThiDanhSachLenUI(dsLoc);
    }
    private void thucHienTimKiem(String tuKhoa) {
        if (danhSachGocCache == null || danhSachGocCache.isEmpty()) return;
        
        String tuKhoaThuong = "";
        if (tuKhoa != null && !tuKhoa.trim().isEmpty() && !tuKhoa.equals("Tìm SP, Mã lô...")) {
            tuKhoaThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tuKhoa.toLowerCase().trim());
        }

        List<ChiTietLoHang> dsLoc = new ArrayList<>();
        
        for (ChiTietLoHang lo : danhSachGocCache) {
            if (tuKhoaThuong.isEmpty()) {
                dsLoc.add(lo);
            } else {
                String tenSP = mapTenSanPham.getOrDefault(lo.getMaSP(), "Không xác định");
                String tenSPThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tenSP.toLowerCase());
                String maLoThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(lo.getMaLoHang().toLowerCase());

                if (tenSPThuong.contains(tuKhoaThuong) || maLoThuong.contains(tuKhoaThuong)) {
                    dsLoc.add(lo);
                }
            }
        }
        hienThiDanhSachLenUI(dsLoc);
    }
    private void hienThiDanhSachLenUI(List<ChiTietLoHang> danhSach) {
        pnlDanhSachCard.removeAll();
        
        if (danhSach.isEmpty()) {
            JLabel lblRong = new JLabel("  Không tìm thấy kết quả nào phù hợp!");
            lblRong.setFont(FONT_NORMAL);
            lblRong.setForeground(TEXT_MUTED);
            pnlDanhSachCard.add(lblRong);
        } else {
            // 🔥 ĐÃ SỬA: Đổi kiểu dữ liệu vòng lặp
            for (ChiTietLoHang lo : danhSach) { 
                JPanel card = taoCardItemUI(lo);
                pnlDanhSachCard.add(card);
                pnlDanhSachCard.add(Box.createVerticalStrut(10));
            }
        }
        
        pnlDanhSachCard.revalidate();
        pnlDanhSachCard.repaint();
    }
    /**
     * Tính giá trị thiệt hại Realtime khi người dùng nhập số lượng
     */
    private void tinhGiaTriThietHai() {
        if (loHangDangChon == null) return;
        
        SwingUtilities.invokeLater(() -> {
            try {
                String slStr = txtSoLuongHuy.getText().trim();
                if (slStr.isEmpty()) {
                    lblTongThietHai.setText("0 VNĐ");
                    return;
                }
                
                int soLuong = Integer.parseInt(slStr);
                
                // Validate cơ bản trên UI
                if (soLuong < 0) {
                    lblTongThietHai.setText("Lỗi: Số âm!");
                    return;
                }
                // 🔥 ĐÃ SỬA: Dùng getSoLuongTon()
                if (soLuong > loHangDangChon.getSoLuongTon()) { 
                    lblTongThietHai.setText("Lỗi: Vượt tồn kho!");
                    lblTongThietHai.setForeground(Color.RED);
                    return;
                }

                // 🔥 ĐÃ SỬA: Dùng getGiaNhap()
                BigDecimal thietHai = loHangDangChon.getGiaNhap().multiply(new BigDecimal(soLuong)); 
                lblTongThietHai.setText(moneyFormat.format(thietHai));
                lblTongThietHai.setForeground(COLOR_EXPIRED_TEXT);

            } catch (NumberFormatException ex) {
                lblTongThietHai.setText("Lỗi: Nhập sai số");
            }
        });
    }

    /**
     * Xử lý khi nhấn nút Hoàn Tất Tiêu Hủy
     */
    private void xuLyHoanTatTieuHuy() {
        if (loHangDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lô hàng cần tiêu hủy!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int soLuongHuy = Integer.parseInt(txtSoLuongHuy.getText().trim());
            
            // Cảnh báo UX như cũ
            if (soLuongHuy >= 50 || loHangDangChon.getGiaNhap().multiply(new BigDecimal(soLuongHuy)).compareTo(new BigDecimal(1000000)) > 0) {
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "CẢNH BÁO: Bạn đang tiêu hủy số lượng lớn hoặc giá trị cao.\nXác nhận tiếp tục?", 
                    "Xác nhận tiêu hủy lớn", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) return;
            }

            String lyDo = cbLyDoHuy.getSelectedItem().toString();
            if (lyDo.equals("Khác...")) lyDo = txtLyDoKhac.getText().trim();

            // ===============================================
            // GỌI LOGIC TẦNG BACKEND TẠI ĐÂY
            // ===============================================
            
            // 1. Khởi tạo Phiếu Tiêu Hủy
            PhieuTieuHuy phieu = new PhieuTieuHuy();
            // LƯU Ý: Chỗ này bạn truyền Mã Nhân Viên đang đăng nhập vào nhé! Tôi tạm fix "NV001"
            phieu.setMaNV("NV001"); 
            PhieuTieuHuyLogic.getInstance().taoPhieuTieuHuy(phieu); // Nó sẽ tự sinh MaPhieuHuy
            
            // 2. Nạp Chi Tiết (Nó sẽ tự trừ tồn kho và tính tổng tiền)
            ChiTietPhieuHuy ct = new ChiTietPhieuHuy();
            ct.setMaPhieuHuy(phieu.getMaPhieuHuy());
            ct.setMaLoHang(loHangDangChon.getMaLoHang());
            ct.setMaSP(loHangDangChon.getMaSP());
            ct.setSoLuongHuy(soLuongHuy);
            ct.setLyDoChiTiet(lyDo);
            
            ChiTietPhieuHuyLogic.getInstance().themChiTietPhieuHuy(ct);
            
            // 3. Hoàn tất & Chốt phiếu (Đổi trạng thái sang DA_TIEU_HUY)
            PhieuTieuHuyLogic.getInstance().hoanTatPhieuTieuHuy(phieu.getMaPhieuHuy());

            // ===============================================

            JOptionPane.showMessageDialog(this, "Đã ghi nhận tiêu hủy thành công " + soLuongHuy + " sản phẩm!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            
            // Reset form và load lại danh sách Realtime từ Database
            loHangDangChon = null;
            txtSoLuongHuy.setText("");
            lblTongThietHai.setText("0 VNĐ");
            txtSoLuongHuy.setEnabled(false);
            
            // Gọi lại hàm load kho
            taiDanhSachHangCanHuyAsync();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: Số lượng nhập vào không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Hiển thị thông tin Lô Hàng được click lên Form Chi tiết
     */
    private void chonLoHangDeTieuHuy(ChiTietLoHang lo) {
        this.loHangDangChon = lo;
        this.tenSpDangChon = mapTenSanPham.getOrDefault(lo.getMaSP(), "Sản phẩm ẩn danh");
        
        lblTenSP.setText(this.tenSpDangChon);
        lblMaLo.setText("Lô: " + lo.getMaLoHang() + "  |  Tồn hệ thống: " + lo.getSoLuongTon());
        
        if (lo.getHSD() != null) {
            lblHSD.setText("Hạn sử dụng: " + lo.getHSD().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            lblHSD.setText("Hạn sử dụng: Không có");
        }
        
        lblGiaNhap.setText("Giá nhập: " + moneyFormat.format(lo.getGiaNhap()));

        // Xác định tình trạng hiển thị
        long days = ChronoUnit.DAYS.between(LocalDate.now(), lo.getHSD());
        if (days < 0) {
            capNhatUIThongBao(COLOR_EXPIRED_BG, COLOR_EXPIRED_TEXT, "⛔ ĐÃ HẾT HẠN (" + Math.abs(days) + " ngày)");
            cbLyDoHuy.setSelectedItem("Hàng hết hạn");
        } else if (days <= 7) {
            capNhatUIThongBao(COLOR_WARNING_BG, COLOR_WARNING_TEXT, "⚠ CẬN DATE (Còn " + days + " ngày)");
        } else {
            capNhatUIThongBao(COLOR_NORMAL_BG, COLOR_NORMAL_TEXT, "✔ BÌNH THƯỜNG");
        }

        // Enable form nhập liệu
        txtSoLuongHuy.setEnabled(true);
        cbLyDoHuy.setEnabled(true);
        txtSoLuongHuy.setText(String.valueOf(lo.getSoLuongTon())); // Gợi ý hủy hết tồn kho
        txtSoLuongHuy.requestFocus();
        txtSoLuongHuy.selectAll();
    }

    private void capNhatUIThongBao(Color bg, Color text, String msg) {
        pnlTrangThai.setBackground(bg);
        lblTrangThaiText.setForeground(text);
        lblTrangThaiText.setText(msg);
        pnlTrangThai.repaint();
    }

    // =====================================================================
    // LOAD DỮ LIỆU BẤT ĐỒNG BỘ (TRUY VẤN SIÊU TỐC)
    // =====================================================================
    private void taiDanhSachHangCanHuyAsync() {
        pnlDanhSachCard.removeAll();
        pnlDanhSachCard.add(new JLabel(" Đang tải dữ liệu từ CSDL..."));
        pnlDanhSachCard.revalidate();
        pnlDanhSachCard.repaint();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 1. Tải Map Tên Sản Phẩm siêu tốc
                List<SanPham> listSP = new SanPhamLogic().layDanhSachSanPham();
                mapTenSanPham.clear();
                for (SanPham sp : listSP) {
                    mapTenSanPham.put(sp.getMaSP(), sp.getTenSP());
                }

                // 2. Tải toàn bộ Chi Tiết Lô Hàng từ CSDL
                List<ChiTietLoHang> tatCaLo = ChiTietLoHangDAO.getInstance().layDanhSachChiTietLoHang();
                
                danhSachGocCache.clear();
                for (ChiTietLoHang lo : tatCaLo) {
                    if (lo.getSoLuongTon() > 0) { // Chỉ đưa lên UI những lô còn hàng
                        danhSachGocCache.add(lo);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Bắt lỗi nếu có
                    
                    // 1. Tải xong thì kích hoạt thuật toán Tìm Kiếm để vẽ UI
                    thucHienTimKiem(txtTimKiem.getText());
                    
                    // 2. 🔥 GỌI CẬP NHẬT 4 CARD THỐNG KÊ TRÊN CÙNG
                    capNhatThongKe();
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TieuHuySanPhamGUI.this, "Lỗi tải dữ liệu: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    private void capNhatThongKe() {
        int countHetHan = 0;
        int countCanDate = 0;
        BigDecimal uocTinhThietHai = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        // Quét toàn bộ Lô Hàng trong kho (đã tải sẵn trên RAM)
        for (ChiTietLoHang lo : danhSachGocCache) {
            if (lo.getHSD() != null) {
                long days = ChronoUnit.DAYS.between(today, lo.getHSD());
                if (days < 0) {
                    countHetHan++;
                    // Cộng dồn thiệt hại = Giá Nhập * Số Lượng Tồn
                    uocTinhThietHai = uocTinhThietHai.add(lo.getGiaNhap().multiply(new BigDecimal(lo.getSoLuongTon())));
                } else if (days <= 7) {
                    countCanDate++;
                }
            }
        }

        // Tính số lượng phiếu đã hủy trong tháng hiện tại
        int countPhieuHuyThangNay = 0;
        try {
            List<PhieuTieuHuy> dsPhieu = Dao.PhieuTieuHuyDAO.getInstance().layDanhSachPhieuTieuHuy();
            for(PhieuTieuHuy p : dsPhieu) {
                if (p.getNgayTao() != null && p.getNgayTao().getMonthValue() == today.getMonthValue() && p.getNgayTao().getYear() == today.getYear()) {
                    countPhieuHuyThangNay++;
                }
            }
        } catch(Exception e) {}

        // Đẩy số liệu lên giao diện
        lblStatHetHan.setText(countHetHan + " Lô");
        lblStatCanDate.setText(countCanDate + " Lô");
        lblStatThatThoat.setText(countPhieuHuyThangNay + " Phiếu");
        lblStatThietHai.setText(GUI.HoTro.DinhDangUtil.dinhDangTien(uocTinhThietHai));
    }
    /**
     * Tạo UI cho từng Card Sản Phẩm trong danh sách
     */
    private JPanel taoCardItemUI(ChiTietLoHang lo) { 
        PanelBoGoc card = new PanelBoGoc(15);
        card.setLayout(new BorderLayout(15, 0));
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(10, 15, 10, 15));
        card.setMaximumSize(new Dimension(800, 90));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Icon thay cho ảnh
        JLabel lblIcon = new JLabel("📦");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        card.add(lblIcon, BorderLayout.WEST);

        // Lấy tên SP từ Map
        String tenSP = mapTenSanPham.getOrDefault(lo.getMaSP(), "Sản phẩm ẩn danh");

        // Info giữa
        JPanel pnlInfo = new JPanel(new GridLayout(2, 1));
        pnlInfo.setBackground(CARD_BG);
        
        JLabel lblName = new JLabel(tenSP); // Dùng tên SP thật
        lblName.setFont(FONT_BOLD);
        lblName.setForeground(TEXT_MAIN);
        
        // Dùng Getters của ChiTietLoHang
        String hsdStr = (lo.getHSD() != null) ? lo.getHSD().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Không có";
        JLabel lblSub = new JLabel("Lô: " + lo.getMaLoHang() + " | Tồn: " + lo.getSoLuongTon() + " | HSD: " + hsdStr);
        lblSub.setFont(FONT_SMALL);
        lblSub.setForeground(TEXT_MUTED);
        
        pnlInfo.add(lblName);
        pnlInfo.add(lblSub);
        card.add(pnlInfo, BorderLayout.CENTER);

        // Trạng thái bên phải
        JLabel lblStatus = new JLabel();
        lblStatus.setFont(FONT_BOLD);
        
        if (lo.getHSD() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), lo.getHSD());
            if (days < 0) {
                lblStatus.setText("HẾT HẠN");
                lblStatus.setForeground(COLOR_EXPIRED_TEXT);
            } else if (days <= 7) {
                lblStatus.setText("CẬN DATE");
                lblStatus.setForeground(COLOR_WARNING_TEXT);
            } else {
                lblStatus.setText("BÌNH THƯỜNG");
                lblStatus.setForeground(COLOR_NORMAL_TEXT);
            }
        } else {
            lblStatus.setText("BÌNH THƯỜNG");
            lblStatus.setForeground(COLOR_NORMAL_TEXT);
        }
        card.add(lblStatus, BorderLayout.EAST);

        // Hover & Click Effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { card.setBackground(new Color(240, 248, 255)); }
            @Override
            public void mouseExited(MouseEvent e) { card.setBackground(CARD_BG); }
            @Override
            public void mouseClicked(MouseEvent e) { chonLoHangDeTieuHuy(lo); }
        });

        return card;
    }

    // =====================================================================
    // CLASSES HỖ TRỢ BÊN TRONG (UI CUSTOM & DTO)
    // =====================================================================

    /**
     * Panel Custom bo góc hiện đại
     */
    private static class PanelBoGoc extends JPanel {
        private int radius;
        public PanelBoGoc(int radius) {
            this.radius = radius;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }

    /**
     * Custom Scrollbar mỏng, dẹt, hiện đại
     */
    private static class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(200, 200, 200);
            this.trackColor = BG_COLOR;
        }
        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() {
            JButton button = new JButton();
            Dimension zeroDim = new Dimension(0, 0);
            button.setPreferredSize(zeroDim);
            button.setMinimumSize(zeroDim);
            button.setMaximumSize(zeroDim);
            return button;
        }
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 10, 10);
            g2.dispose();
        }
    }

    // MAIN ĐỂ TEST GIAO DIỆN CHẠY ĐỘC LẬP
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        JFrame frame = new JFrame("Dashboard Tiêu Hủy - Retail System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.add(new TieuHuySanPhamGUI());
        frame.setVisible(true);
    }
}