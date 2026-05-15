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

    private List<ChiTietLoHang> danhSachChon = new ArrayList<>();
    private Map<String, JTextField> mapTxtSoLuong = new HashMap<>();
    private JPanel pnlDanhSachChonPhai;
    private List<ChiTietLoHang> danhSachGocCache = new ArrayList<>();
    private Map<String, String> mapTenSanPham = new HashMap<>();
    // Timer cho Debounce Search
    private Timer debounceTimer;
    private String maNVHienTai;
    private String tenNVHienTai;
    public TieuHuySanPhamGUI(String maNV) {
        this.maNVHienTai = maNV;
        // Lấy thông tin nhân viên thật từ DAO
        Data.NhanVien nv = Dao.NhanVienDAO.getInstance().layNhanVienTheoMa(maNV);
        this.tenNVHienTai = (nv != null) ? nv.getHoTen() : "Không xác định";
        
        khoiTaoGiaoDien();// Gọi hàm khởi tạo giao diện
        taiDanhSachHangCanHuyAsync(); 
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

        // --- DÒNG TIÊU ĐỀ & NHÂN VIÊN ---
        JPanel pnlHeaderRow = new JPanel(new BorderLayout());
        pnlHeaderRow.setOpaque(false);

        // Tiêu đề bên trái
        JLabel lblTitle = new JLabel("QUẢN LÝ TIÊU HỦY & THẤT THOÁT");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(TEXT_MAIN);
        pnlHeaderRow.add(lblTitle, BorderLayout.WEST);

        // 🔥 HIỂN THỊ NHÂN VIÊN Ở GÓC PHẢI (GIỐNG KIỂM KÊ)
        JLabel lblNhanVien = new JLabel("👤 Nhân viên: " + tenNVHienTai + " (" + maNVHienTai + ")");
        lblNhanVien.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblNhanVien.setForeground(TEXT_MUTED);
        lblNhanVien.setBorder(new EmptyBorder(0, 0, 0, 10)); // Cách mép phải một chút
        pnlHeaderRow.add(lblNhanVien, BorderLayout.EAST);

        pnlTop.add(pnlHeaderRow, BorderLayout.NORTH);

        // --- PHẦN THỐNG KÊ (4 Cards bên dưới) ---
        // Giữ nguyên các Label thống kê cũ
        lblStatHetHan = new JLabel("0 Lô");
        lblStatCanDate = new JLabel("0 Lô");
        lblStatThatThoat = new JLabel("0 Phiếu");
        lblStatThietHai = new JLabel("0 đ");

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
        cbSapXep = new JComboBox<>(new String[]{
            "Sắp xếp: Ưu tiên", // Hết hạn -> Cận date -> Bth
            "Sắp xếp: Tồn kho giảm", 
            "Sắp xếp: Giá trị giảm"
        });

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
        pnlPhai.setBorder(new EmptyBorder(25, 20, 25, 20));

        JPanel pnlContent = new JPanel(new BorderLayout(0, 15));
        pnlContent.setBackground(CARD_BG);

        // Tiêu đề
        lblTenSP = new JLabel("CHƯA CHỌN LÔ HÀNG NÀO"); // Tận dụng lại biến cũ để làm Title
        lblTenSP.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTenSP.setForeground(PRIMARY_COLOR);
        pnlContent.add(lblTenSP, BorderLayout.NORTH);

        // Danh sách các lô đã chọn (Vùng giữa)
        pnlDanhSachChonPhai = new JPanel();
        pnlDanhSachChonPhai.setLayout(new BoxLayout(pnlDanhSachChonPhai, BoxLayout.Y_AXIS));
        pnlDanhSachChonPhai.setBackground(CARD_BG);
        
        JScrollPane scroll = new JScrollPane(pnlDanhSachChonPhai);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scroll.getViewport().setBackground(CARD_BG);
        pnlContent.add(scroll, BorderLayout.CENTER);

        // Vùng dưới: Lý do hủy & Tổng thiệt hại & Nút
        JPanel pnlBottom = new JPanel();
        pnlBottom.setLayout(new BoxLayout(pnlBottom, BoxLayout.Y_AXIS));
        pnlBottom.setBackground(CARD_BG);

        JPanel pnlLyDo = new JPanel(new BorderLayout(10, 0));
        pnlLyDo.setBackground(CARD_BG);
        pnlLyDo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        pnlLyDo.add(new JLabel("Lý do chung:"), BorderLayout.WEST);
        
        cbLyDoHuy = new JComboBox<>(new String[]{"Hàng hết hạn", "Hư hỏng/Móp méo", "Nấm mốc", "Thất thoát/Mất", "Khác..."});
        pnlLyDo.add(cbLyDoHuy, BorderLayout.CENTER);
        
        txtLyDoKhac = new JTextArea(2, 20);
        txtLyDoKhac.setLineWrap(true);
        txtLyDoKhac.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        txtLyDoKhac.setVisible(false);
        cbLyDoHuy.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                txtLyDoKhac.setVisible(cbLyDoHuy.getSelectedItem().equals("Khác..."));
                revalidate();
            }
        });

        PanelBoGoc pnlThietHai = new PanelBoGoc(15);
        pnlThietHai.setBackground(COLOR_EXPIRED_BG);
        pnlThietHai.setLayout(new BorderLayout());
        pnlThietHai.setBorder(new EmptyBorder(15, 20, 15, 20)); 
        pnlThietHai.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65)); 
        
        JLabel lblThietHaiTitle = new JLabel("Tổng thiệt hại ước tính:");
        lblThietHaiTitle.setFont(FONT_NORMAL);
        lblThietHaiTitle.setForeground(COLOR_EXPIRED_TEXT);
        lblTongThietHai = new JLabel("0 VNĐ");
        lblTongThietHai.setFont(new Font("Segoe UI", Font.BOLD, 22)); 
        lblTongThietHai.setForeground(COLOR_EXPIRED_TEXT);
        lblTongThietHai.setHorizontalAlignment(SwingConstants.RIGHT); 
        pnlThietHai.add(lblThietHaiTitle, BorderLayout.WEST);
        pnlThietHai.add(lblTongThietHai, BorderLayout.CENTER);

        JPanel pnlAction = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlAction.setBackground(CARD_BG);
        pnlAction.setBorder(new EmptyBorder(10, 0, 0, 0)); 
        btnLuuTam = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("Lưu Tạm", new Color(149, 165, 166)); 
        btnHoanTat = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("Hoàn Tất Hủy", COLOR_EXPIRED_TEXT); 
        btnHoanTat.addActionListener(e -> xuLyHoanTatTieuHuy());
        pnlAction.add(btnLuuTam);
        pnlAction.add(btnHoanTat);

        pnlBottom.add(new JSeparator());
        pnlBottom.add(Box.createVerticalStrut(15));
        pnlBottom.add(pnlLyDo);
        pnlBottom.add(Box.createVerticalStrut(5));
        pnlBottom.add(txtLyDoKhac);
        pnlBottom.add(Box.createVerticalStrut(15));
        pnlBottom.add(pnlThietHai);
        pnlBottom.add(pnlAction);

        pnlContent.add(pnlBottom, BorderLayout.SOUTH);
        pnlPhai.add(pnlContent, BorderLayout.CENTER);

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

        String tuKhoa = txtTimKiem.getText();
        String tuKhoaThuong = (tuKhoa != null && !tuKhoa.equals("Tìm SP, Mã lô...")) 
                ? GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tuKhoa.toLowerCase().trim()) : "";

        String boLoc = (String) cbBoLoc.getSelectedItem();
        String sapXep = (String) cbSapXep.getSelectedItem();
        LocalDate today = LocalDate.now();

        List<ChiTietLoHang> dsLoc = new ArrayList<>();

        // 1. GIAI ĐOẠN LỌC (FILTER)
        for (ChiTietLoHang lo : danhSachGocCache) {
            // Lọc theo Text
            boolean matchTuKhoa = true;
            if (!tuKhoaThuong.isEmpty()) {
                String tenSP = mapTenSanPham.getOrDefault(lo.getMaSP(), "");
                String matchStr = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet((tenSP + " " + lo.getMaLoHang()).toLowerCase());
                if (!matchStr.contains(tuKhoaThuong)) matchTuKhoa = false;
            }
            if (!matchTuKhoa) continue;

            // Lọc theo Trạng thái (Combobox)
            boolean matchTrangThai = true;
            if (boLoc != null && !boLoc.equals("Tất cả trạng thái")) {
                long days = (lo.getHSD() != null) ? ChronoUnit.DAYS.between(today, lo.getHSD()) : 9999;
                if (boLoc.equals("Hết hạn") && days >= 0) matchTrangThai = false;
                else if (boLoc.equals("Cận date") && (days < 0 || days > 7)) matchTrangThai = false;
                else if (boLoc.equals("Bình thường") && days <= 7) matchTrangThai = false;
            }
            if (matchTrangThai) dsLoc.add(lo);
        }

        // 2. GIAI ĐOẠN SẮP XẾP ĐA TẦNG (SORTING) 🔥
        dsLoc.sort((lo1, lo2) -> {
            // --- TẦNG 1: ƯU TIÊN HÀNG ĐANG ĐƯỢC CHỌN LÊN ĐẦU ---
            boolean s1 = danhSachChon.contains(lo1);
            boolean s2 = danhSachChon.contains(lo2);
            if (s1 != s2) return s1 ? -1 : 1; // Thằng nào được chọn (true) thì lên trước (-1)

            // --- TẦNG 2: SẮP XẾP THEO TRẠNG THÁI CẢNH BÁO ---
            if (sapXep == null || sapXep.equals("Sắp xếp: Ưu tiên") || sapXep.equals("Sắp xếp: HSD Gần nhất")) {
                int p1 = getPriority(lo1, today);
                int p2 = getPriority(lo2, today);
                if (p1 != p2) return Integer.compare(p1, p2);
                
                // Nếu cùng trạng thái -> HSD cũ hơn lên trước
                if (lo1.getHSD() == null) return 1;
                if (lo2.getHSD() == null) return -1;
                return lo1.getHSD().compareTo(lo2.getHSD());
            } 
            
            // Các kiểu sắp xếp khác
            else if (sapXep.equals("Sắp xếp: Tồn kho giảm")) {
                return Integer.compare(lo2.getSoLuongTon(), lo1.getSoLuongTon());
            }
            return 0;
        });

        hienThiDanhSachLenUI(dsLoc);
    }

    // Hàm phụ trợ tính độ ưu tiên dựa trên ngày (Dùng cho sắp xếp)
    private int getPriority(ChiTietLoHang lo, LocalDate today) {
        if (lo.getHSD() == null) return 2;
        long days = ChronoUnit.DAYS.between(today, lo.getHSD());
        if (days < 0) return 0;   // Hết hạn -> Rank 0
        if (days <= 7) return 1; // Cận date -> Rank 1
        return 2;                // Bình thường -> Rank 2
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

    // 1. Hàm vẽ lại danh sách bên phải mỗi khi user tick/bỏ tick
    private void capNhatPanelPhaiDaChon() {
        pnlDanhSachChonPhai.removeAll();
        mapTxtSoLuong.clear(); // Reset map ô nhập liệu
        
        if (danhSachChon.isEmpty()) {
            lblTenSP.setText("CHƯA CHỌN LÔ HÀNG NÀO");
            btnHoanTat.setEnabled(false);
            lblTongThietHai.setText("0 VNĐ");
        } else {
            lblTenSP.setText("ĐÃ CHỌN " + danhSachChon.size() + " LÔ HÀNG");
            btnHoanTat.setEnabled(true);
            
            for (ChiTietLoHang lo : danhSachChon) {
                JPanel pnlItem = new JPanel(new BorderLayout(10, 0));
                pnlItem.setBackground(Color.WHITE);
                pnlItem.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                    new EmptyBorder(10, 5, 10, 5)
                ));
                pnlItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

                String tenSP = mapTenSanPham.getOrDefault(lo.getMaSP(), "SP...");
                JLabel lblInfo = new JLabel("<html><b>" + tenSP + "</b><br><font color='#7f8c8d'>Lô: " + lo.getMaLoHang() + " | Tồn: " + lo.getSoLuongTon() + "</font></html>");
                pnlItem.add(lblInfo, BorderLayout.CENTER);

                // Ô nhập số lượng hủy cho lô này
                JTextField txtSL = new JTextField(String.valueOf(lo.getSoLuongTon()), 4);
                txtSL.setHorizontalAlignment(SwingConstants.CENTER);
                txtSL.setFont(FONT_BOLD);
                // Lắng nghe gõ phím để tính tiền
                txtSL.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) { tinhGiaTriThietHai(); }
                    public void removeUpdate(DocumentEvent e) { tinhGiaTriThietHai(); }
                    public void changedUpdate(DocumentEvent e) { tinhGiaTriThietHai(); }
                });
                
                String key = lo.getMaLoHang() + "_" + lo.getMaSP();
                mapTxtSoLuong.put(key, txtSL); // Lưu vào Map để lát lấy data
                
                JPanel pnlSL = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                pnlSL.setOpaque(false);
                pnlSL.add(new JLabel("Hủy: "));
                pnlSL.add(txtSL);
                
                pnlItem.add(pnlSL, BorderLayout.EAST);
                pnlDanhSachChonPhai.add(pnlItem);
            }
        }
        
        pnlDanhSachChonPhai.revalidate();
        pnlDanhSachChonPhai.repaint();
        tinhGiaTriThietHai(); // Gọi hàm tính tiền
    }

    // 2. Hàm tính tiền duyệt qua toàn bộ Map
    private void tinhGiaTriThietHai() {
        if (danhSachChon.isEmpty()) {
            lblTongThietHai.setText("0 VNĐ"); return;
        }
        
        SwingUtilities.invokeLater(() -> {
            BigDecimal tongTien = BigDecimal.ZERO;
            boolean coLoi = false;

            for (ChiTietLoHang lo : danhSachChon) {
                String key = lo.getMaLoHang() + "_" + lo.getMaSP();
                JTextField txt = mapTxtSoLuong.get(key);
                if (txt == null) continue;

                try {
                    int sl = Integer.parseInt(txt.getText().trim());
                    if (sl < 0 || sl > lo.getSoLuongTon()) {
                        coLoi = true; break; // Nhập lố tồn kho hoặc âm
                    }
                    tongTien = tongTien.add(lo.getGiaNhap().multiply(new BigDecimal(sl)));
                } catch (Exception ex) { coLoi = true; break; }
            }

            if (coLoi) {
                lblTongThietHai.setText("Lỗi số lượng!");
                lblTongThietHai.setForeground(Color.RED);
            } else {
                lblTongThietHai.setText(moneyFormat.format(tongTien));
                lblTongThietHai.setForeground(COLOR_EXPIRED_TEXT);
            }
        });
    }

    // 3. Hàm hoàn tất duyệt vòng lặp lưu Multi Database 🚀
    private void xuLyHoanTatTieuHuy() {
        if (danhSachChon.isEmpty()) return;

        try {
            // 1. Lấy lý do chung
            String lyDoChung = cbLyDoHuy.getSelectedItem().toString();
            if (lyDoChung.equals("Khác...")) lyDoChung = txtLyDoKhac.getText().trim();

            int tongSoLuong = 0;
            BigDecimal tongGiaTriHuy = BigDecimal.ZERO;

            // 2. VÒNG LẶP 1: Xác thực an toàn và tính toán TỔNG cho Phiếu (Master)
            for (Data.ChiTietLoHang lo : danhSachChon) {
                String key = lo.getMaLoHang() + "_" + lo.getMaSP();
                int slHuy = Integer.parseInt(mapTxtSoLuong.get(key).getText().trim());
                
                if (slHuy <= 0 || slHuy > lo.getSoLuongTon()) {
                    JOptionPane.showMessageDialog(this, "Số lượng hủy của lô " + lo.getMaLoHang() + " không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                tongSoLuong += slHuy;
                BigDecimal giaTriTungLo = lo.getGiaNhap().multiply(new BigDecimal(slHuy));
                tongGiaTriHuy = tongGiaTriHuy.add(giaTriTungLo);
            }

            // ===============================================
            // 3. TẠO PHIẾU TỔNG (Đẩy đủ 7 cột xuống SQL)
            // ===============================================
            Data.PhieuTieuHuy phieu = new Data.PhieuTieuHuy();
            
            // Bơm dữ liệu chuẩn chỉnh:
            phieu.setMaNV(this.maNVHienTai); // Đã liên kết mã nhân viên từ lúc đăng nhập
            phieu.setTongSoLuong(tongSoLuong); 
            phieu.setTongGiaTriHuy(tongGiaTriHuy); 
            phieu.setLyDoHuy(lyDoChung);
            
            // Logic sẽ tự sinh MaPhieuHuy, NgayTao (Hiện tại) và gán trạng thái
            Logic.PhieuTieuHuyLogic.getInstance().taoPhieuTieuHuy(phieu); 

            // ===============================================
            // 4. TẠO CHI TIẾT PHIẾU (Đẩy đủ 6 cột xuống SQL)
            // ===============================================
            for (Data.ChiTietLoHang lo : danhSachChon) {
                String key = lo.getMaLoHang() + "_" + lo.getMaSP();
                int slHuy = Integer.parseInt(mapTxtSoLuong.get(key).getText().trim());
                BigDecimal giaTriHuyChiTiet = lo.getGiaNhap().multiply(new BigDecimal(slHuy));

                Data.ChiTietPhieuHuy ct = new Data.ChiTietPhieuHuy();
                
                // Bơm dữ liệu:
                ct.setMaPhieuHuy(phieu.getMaPhieuHuy()); // Nối đúng với mã phiếu tổng ở trên
                ct.setMaLoHang(lo.getMaLoHang());
                ct.setMaSP(lo.getMaSP());
                ct.setSoLuongHuy(slHuy);
                ct.setGiaTriHuy(giaTriHuyChiTiet); // Giá trị thiệt hại riêng của lô này
                ct.setLyDoChiTiet(lyDoChung);
                
                Logic.ChiTietPhieuHuyLogic.getInstance().themChiTietPhieuHuy(ct);
            }
            
            // 5. Chốt sổ phiếu (Cập nhật trạng thái DA_TIEU_HUY)
            Logic.PhieuTieuHuyLogic.getInstance().hoanTatPhieuTieuHuy(phieu.getMaPhieuHuy());
            // ===============================================

            JOptionPane.showMessageDialog(this, "Đã lưu thông tin tiêu hủy vào Database (Gồm " + danhSachChon.size() + " chi tiết)!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            
            // Reset toàn bộ UI sau khi xử lý xong
            danhSachChon.clear();
            capNhatPanelPhaiDaChon(); 
            taiDanhSachHangCanHuyAsync(); // Load lại kho siêu tốc bằng Turbo

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Có lỗi xảy ra khi lưu SQL: " + ex.getMessage(), "Lỗi Database", JOptionPane.ERROR_MESSAGE);
        }
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
        pnlDanhSachCard.add(new JLabel(" 🚀 Đang tải dữ liệu từ Động cơ Turbo..."));
        pnlDanhSachCard.revalidate();
        pnlDanhSachCard.repaint();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 🔥 KÍCH HOẠT TRUY VẤN SIÊU TỐC (Chỉ mất vài mili-giây)
                Dao.TruyVanSieuTocDAO.DuLieuTieuHuyDTO dataTurbo = Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuKhoSieuToc();
                
                // Nạp thẳng vào bộ nhớ đệm (Cache) trên RAM
                danhSachGocCache = dataTurbo.dsLoHangKho;
                mapTenSanPham = dataTurbo.mapTenSanPham;
                
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Bắt lỗi nếu có
                    
                    // 1. Gọi thuật toán tìm kiếm/lọc Real-time (Đã gộp ở bước trước) để vẽ UI
                    timKiemRealtime(); 
                    
                    // 2. Cập nhật 4 thẻ thống kê trên cùng
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
        
        // Kiểm tra xem Lô này đang được chọn hay không để đổi màu viền
        boolean isSelected = danhSachChon.contains(lo);
        if (isSelected) {
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                new EmptyBorder(8, 13, 8, 13)
            ));
            card.setBackground(new Color(240, 248, 255));
        } else {
            card.setBorder(new EmptyBorder(10, 15, 10, 15));
        }
        
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 🔥 THÊM CHECKBOX BÊN TRÁI CÙNG
        ModernCheckBox cbSelect = new ModernCheckBox();
        cbSelect.setPreferredSize(new Dimension(30, 30));
        cbSelect.setSelected(isSelected);
        card.add(cbSelect, BorderLayout.WEST);

        // Lấy tên SP từ Map
        String tenSP = mapTenSanPham.getOrDefault(lo.getMaSP(), "Sản phẩm ẩn danh");

        // Info giữa
        JPanel pnlInfo = new JPanel(new GridLayout(2, 1));
        pnlInfo.setOpaque(false);
        
        JLabel lblName = new JLabel(tenSP);
        lblName.setFont(FONT_BOLD);
        lblName.setForeground(TEXT_MAIN);
        
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
            if (days < 0) { lblStatus.setText("HẾT HẠN"); lblStatus.setForeground(COLOR_EXPIRED_TEXT); } 
            else if (days <= 7) { lblStatus.setText("CẬN DATE"); lblStatus.setForeground(COLOR_WARNING_TEXT); } 
            else { lblStatus.setText("BÌNH THƯỜNG"); lblStatus.setForeground(COLOR_NORMAL_TEXT); }
        } else { lblStatus.setText("BÌNH THƯỜNG"); lblStatus.setForeground(COLOR_NORMAL_TEXT); }
        card.add(lblStatus, BorderLayout.EAST);

        // Hiệu ứng Click
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (danhSachChon.contains(lo)) {
                    danhSachChon.remove(lo); // Bỏ chọn
                } else {
                    danhSachChon.add(lo); // Chọn thêm
                }
                timKiemRealtime();
                capNhatPanelPhaiDaChon(); // Đẩy dữ liệu sang form phải
            }
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
    // =========================================================
    // 🎨 COMPONENT: MODERN CHECKBOX (O TICK CHUẨN GMAIL)
    // =========================================================
    private class ModernCheckBox extends JCheckBox {
        public ModernCheckBox() { setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = 18; int y = (getHeight() - size) / 2; int x = (getWidth() - size) / 2; 
            if (isSelected()) {
                g2.setColor(PRIMARY_COLOR); g2.fillRoundRect(x, y, size, size, 5, 5); 
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 9, x + 8, y + 13); g2.drawLine(x + 8, y + 13, x + 14, y + 5); 
            } else {
                g2.setColor(Color.WHITE); g2.fillRoundRect(x, y, size, size, 5, 5);
                g2.setColor(new Color(180, 185, 195)); g2.drawRoundRect(x, y, size, size, 5, 5);
            }
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
        frame.add(new TieuHuySanPhamGUI("NV001"));
        frame.setVisible(true);
    }
}