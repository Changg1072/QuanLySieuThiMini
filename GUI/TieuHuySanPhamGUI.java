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
    // 🎨 BẢNG MÀU HIỆN ĐẠI (LIGHT THEME)
    // ==========================================
    private static final Color BG_MAIN = new Color(248, 250, 252);        
    private static final Color BG_CARD = Color.WHITE;                     
    private static final Color BORDER_COLOR = new Color(226, 232, 240);   
    private static final Color TEXT_TITLE = new Color(15, 23, 42);        
    private static final Color TEXT_SUB = new Color(100, 116, 139);       
    
    // Màu Pastel Trạng Thái
    private static final Color PASTEL_RED_BG = new Color(254, 226, 226);  
    private static final Color PASTEL_RED_FG = new Color(220, 38, 38);
    private static final Color PASTEL_ORANGE_BG = new Color(254, 243, 199);
    private static final Color PASTEL_ORANGE_FG = new Color(217, 119, 6);
    private static final Color PASTEL_GREEN_BG = new Color(220, 252, 231); 
    private static final Color PASTEL_GREEN_FG = new Color(22, 163, 74);
    private static final Color PASTEL_BLUE_BG = new Color(239, 246, 255);  
    private static final Color PASTEL_BLUE_FG = new Color(37, 99, 235);

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0 VNĐ");

    // ==========================================
    // COMPONENTS & STATE
    // ==========================================
    private JPanel pnlDanhSachCard; 
    private JTextField txtTimKiem;
    
    // Filter Pills
    private String currentFilter = "Tất cả";
    private List<JButton> filterPills = new ArrayList<>();

    // Chi tiết bên phải
    private JLabel lblSoLuongMatHang;
    private JLabel lblTongSoLuongHuy;
    private JComboBox<String> cbLyDoHuy;
    private JTextArea txtLyDoKhac;
    private JLabel lblTongThietHai;
    private JButton btnHoanTat, btnLuuTam;
    
    // Thống kê
    private JLabel lblStatHetHan, lblStatCanDate, lblStatThatThoat, lblStatThietHai;

    // Logic Data
    private List<ChiTietLoHang> danhSachChon = new ArrayList<>();
    private Map<String, Integer> mapSoLuongHuy = new HashMap<>(); // Lưu số lượng hủy của từng lô
    private List<ChiTietLoHang> danhSachGocCache = new ArrayList<>();
    private Map<String, String> mapTenSanPham = new HashMap<>();
    
    private Timer debounceTimer;
    private String maNVHienTai;
    private String tenNVHienTai;

    public TieuHuySanPhamGUI(String maNV) {
        this.maNVHienTai = maNV;
        Data.NhanVien nv = Dao.NhanVienDAO.getInstance().layNhanVienTheoMa(maNV);
        this.tenNVHienTai = (nv != null) ? nv.getHoTen() : "Không xác định";
        
        khoiTaoGiaoDien();
        setupDebounceSearch();
        taiDanhSachHangCanHuyAsync(); 
    }

    private void khoiTaoGiaoDien() {
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(20, 25, 20, 25));

        add(taoHeaderVaThongKe(), BorderLayout.NORTH);

        JPanel pnlMain = new JPanel(new BorderLayout(20, 0));
        pnlMain.setBackground(BG_MAIN);
        pnlMain.add(taoPanelTraiDanhSach(), BorderLayout.CENTER);
        pnlMain.add(taoPanelPhaiChiTiet(), BorderLayout.EAST);

        add(pnlMain, BorderLayout.CENTER);
    }

    // =====================================================================
    // VÙNG 1: HEADER VÀ THỐNG KÊ TỔNG QUAN
    // =====================================================================
    private JPanel taoHeaderVaThongKe() {
        JPanel pnlTop = new JPanel(new BorderLayout(0, 15));
        pnlTop.setBackground(BG_MAIN);

        JPanel pnlHeaderRow = new JPanel(new BorderLayout());
        pnlHeaderRow.setOpaque(false);
        JLabel lblTitle = new JLabel("Quản Lý Tiêu Hủy & Thất Thoát");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(TEXT_TITLE);
        pnlHeaderRow.add(lblTitle, BorderLayout.WEST);

        JLabel lblNhanVien = new JLabel("Nhân viên: " + tenNVHienTai + " (" + maNVHienTai + ")");
        lblNhanVien.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblNhanVien.setForeground(TEXT_SUB);
        pnlHeaderRow.add(lblNhanVien, BorderLayout.EAST);
        pnlTop.add(pnlHeaderRow, BorderLayout.NORTH);

        lblStatHetHan = new JLabel("0 Lô");
        lblStatCanDate = new JLabel("0 Lô");
        lblStatThatThoat = new JLabel("0 Phiếu");
        lblStatThietHai = new JLabel("0 VNĐ");

        JPanel pnlStats = new JPanel(new GridLayout(1, 4, 15, 0));
        pnlStats.setBackground(BG_MAIN);
        
        pnlStats.add(taoCardThongKe("Hàng Hết Hạn", lblStatHetHan, PASTEL_RED_FG, PASTEL_RED_BG));
        pnlStats.add(taoCardThongKe("Cận Date", lblStatCanDate, PASTEL_ORANGE_FG, PASTEL_ORANGE_BG));
        pnlStats.add(taoCardThongKe("Đã hủy (Tháng)", lblStatThatThoat, TEXT_SUB, BG_CARD)); 
        pnlStats.add(taoCardThongKe("Ước Tính Thiệt Hại", lblStatThietHai, TEXT_TITLE, BG_CARD));

        pnlTop.add(pnlStats, BorderLayout.CENTER);
        return pnlTop;
    }

    private PanelBoGoc taoCardThongKe(String title, JLabel lblValue, Color valueColor, Color bgColor) {
        PanelBoGoc card = new PanelBoGoc(12, bgColor, bgColor.equals(BG_CARD) ? BORDER_COLOR : bgColor, 1);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblT.setForeground(bgColor.equals(BG_CARD) ? TEXT_SUB : valueColor);
        
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValue.setForeground(valueColor);
        lblValue.setBorder(new EmptyBorder(8, 0, 0, 0));

        card.add(lblT);
        card.add(lblValue);
        return card;
    }

    // =====================================================================
    // VÙNG 2: SIDEBAR DANH SÁCH (TRÁI) - CHỨA Ô NHẬP TRỰC TIẾP
    // =====================================================================
    private JPanel taoPanelTraiDanhSach() {
        JPanel pnlTrai = new JPanel(new BorderLayout(0, 15));
        pnlTrai.setBackground(BG_MAIN);

        // --- 1. HEADER TRÁI (Tiêu đề + Pills) ---
        JPanel pnlFilter = new JPanel(new BorderLayout(0, 10));
        pnlFilter.setOpaque(false);
        
        JPanel pnlLeftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLeftTitle.setOpaque(false);
        JLabel lblListTitle = new JLabel("Danh sách hàng cần xử lý");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblListTitle.setForeground(TEXT_TITLE);
        pnlLeftTitle.add(lblListTitle);
        
        JPanel pnlPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlPills.setOpaque(false);
        
        String[] filters = {"Tất cả", "Hết hạn", "Cận date", "Bình thường"};
        for (String f : filters) {
            JButton btn = createPillButton(f);
            filterPills.add(btn);
            pnlPills.add(btn);
        }
        updatePillsUI();
        
        JPanel pnlFilterTop = new JPanel(new BorderLayout(0, 5));
        pnlFilterTop.setOpaque(false);
        pnlFilterTop.add(pnlLeftTitle, BorderLayout.NORTH);
        pnlFilterTop.add(pnlPills, BorderLayout.SOUTH);

        // --- 2. THANH SEARCH + NÚT LỊCH SỬ (MỚI) ---
        JPanel pnlSearchAndHistory = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlSearchAndHistory.setOpaque(false);

        txtTimKiem = new JTextField(20); // Giảm size 1 chút để đủ chỗ cho nút
        txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tìm theo tên, mã lô...");
        txtTimKiem.setPreferredSize(new Dimension(250, 36));
        txtTimKiem.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(0, 10, 0, 10)
        ));

        // Nút Lịch sử Tiêu hủy (Bo góc, đồng bộ theme)
        JButton btnLichSuHuy = new JButton("🕒 Lịch sử tiêu hủy") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Màu nền: Đỏ nhạt hoặc Trắng tùy sở thích
                g2.setColor(getModel().isRollover() ? new Color(254, 226, 226) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(239, 68, 68)); // Viền đỏ
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLichSuHuy.setForeground(new Color(239, 68, 68));
        btnLichSuHuy.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLichSuHuy.setContentAreaFilled(false);
        btnLichSuHuy.setBorderPainted(false);
        btnLichSuHuy.setFocusPainted(false);
        btnLichSuHuy.setPreferredSize(new Dimension(160, 36));
        btnLichSuHuy.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Sự kiện click mở danh sách lịch sử tiêu hủy
        btnLichSuHuy.addActionListener(e -> {
            GUI.HoTro.DanhSachLichSuTieuHuyDialog.showModal(TieuHuySanPhamGUI.this);
        });

        pnlSearchAndHistory.add(txtTimKiem);
        pnlSearchAndHistory.add(btnLichSuHuy);

        pnlFilter.add(pnlFilterTop, BorderLayout.NORTH);
        pnlFilter.add(pnlSearchAndHistory, BorderLayout.SOUTH);

        // --- 3. DANH SÁCH CARD ---
        pnlDanhSachCard = new JPanel();
        pnlDanhSachCard.setLayout(new BoxLayout(pnlDanhSachCard, BoxLayout.Y_AXIS));
        pnlDanhSachCard.setBackground(BG_MAIN);

        JScrollPane scroll = new JScrollPane(pnlDanhSachCard);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_MAIN);

        pnlTrai.add(pnlFilter, BorderLayout.NORTH);
        pnlTrai.add(scroll, BorderLayout.CENTER);

        return pnlTrai;
    }

    private JButton createPillButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (text.equals(currentFilter)) {
                    g2.setColor(PASTEL_RED_FG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                } else {
                    g2.setColor(BG_CARD);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                }
                FontMetrics fm = g2.getFontMetrics();
                Rectangle r = fm.getStringBounds(getText(), g2).getBounds();
                g2.setColor(text.equals(currentFilter) ? Color.WHITE : TEXT_SUB);
                g2.drawString(getText(), (getWidth() - r.width) / 2, (getHeight() - r.height) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false); btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(80, 28));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> { currentFilter = text; updatePillsUI(); timKiemRealtime(); });
        return btn;
    }

    private void updatePillsUI() { for (JButton b : filterPills) b.repaint(); }

    // =====================================================================
    // VÙNG 3: PANEL CHI TIẾT BÊN PHẢI (THU GỌN LẠI THÀNH BẢNG TÓM TẮT)
    // =====================================================================
    private PanelBoGoc taoPanelPhaiChiTiet() {
        PanelBoGoc pnlPhai = new PanelBoGoc(16, BG_CARD, BORDER_COLOR, 1);
        pnlPhai.setPreferredSize(new Dimension(360, 0));
        pnlPhai.setLayout(new BorderLayout());
        pnlPhai.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel pnlContent = new JPanel();
        pnlContent.setLayout(new BoxLayout(pnlContent, BoxLayout.Y_AXIS));
        pnlContent.setBackground(BG_CARD);

        // 1. Header
        JLabel lblT = new JLabel("Chi tiết Phiếu hủy");
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblT.setForeground(TEXT_TITLE);
        lblT.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlContent.add(lblT);
        pnlContent.add(Box.createVerticalStrut(20));

        // 2. Summary Row
        JPanel pnlSummary = new JPanel(new GridLayout(2, 2, 10, 15));
        pnlSummary.setOpaque(false);
        pnlSummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        pnlSummary.add(createSummaryLabel("Số lượng mặt hàng:"));
        lblSoLuongMatHang = createSummaryValue("0 sản phẩm");
        pnlSummary.add(lblSoLuongMatHang);
        
        pnlSummary.add(createSummaryLabel("Tổng số lượng hủy:"));
        lblTongSoLuongHuy = createSummaryValue("0 đơn vị");
        pnlSummary.add(lblTongSoLuongHuy);
        
        pnlContent.add(pnlSummary);
        pnlContent.add(Box.createVerticalStrut(25));
        
        // Đường kẻ ngang
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(BORDER_COLOR);
        pnlContent.add(sep);
        pnlContent.add(Box.createVerticalStrut(20));

        // 3. Form Input
        JLabel lblLyDoTitle = new JLabel("Lý do tiêu hủy chung:");
        lblLyDoTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLyDoTitle.setForeground(TEXT_SUB);
        lblLyDoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        cbLyDoHuy = new JComboBox<>(new String[]{"Hàng hết hạn", "Hư hỏng/Móp méo", "Nấm mốc", "Thất thoát/Mất", "Khác..."});
        cbLyDoHuy.setPreferredSize(new Dimension(300, 38));
        cbLyDoHuy.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cbLyDoHuy.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbLyDoHuy.setBackground(BG_CARD);
        
        JLabel lblGhiChuTitle = new JLabel("Ghi chú bổ sung:");
        lblGhiChuTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblGhiChuTitle.setForeground(TEXT_SUB);
        lblGhiChuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        txtLyDoKhac = new JTextArea(4, 20);
        txtLyDoKhac.setLineWrap(true);
        txtLyDoKhac.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtLyDoKhac.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(10, 10, 10, 10)
        ));
        txtLyDoKhac.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtLyDoKhac.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        pnlContent.add(lblLyDoTitle);
        pnlContent.add(Box.createVerticalStrut(8));
        pnlContent.add(cbLyDoHuy);
        pnlContent.add(Box.createVerticalStrut(15));
        pnlContent.add(lblGhiChuTitle);
        pnlContent.add(Box.createVerticalStrut(8));
        pnlContent.add(txtLyDoKhac);
        
        pnlContent.add(Box.createVerticalGlue()); // Lò xo đẩy phần dưới xuống đáy

        // 4. Box Tổng thiệt hại & Buttons
        JPanel pnlBottom = new JPanel();
        pnlBottom.setLayout(new BoxLayout(pnlBottom, BoxLayout.Y_AXIS));
        pnlBottom.setBackground(BG_CARD);

        PanelBoGoc pnlThietHai = new PanelBoGoc(12, PASTEL_RED_BG, PASTEL_RED_BG, 0);
        pnlThietHai.setLayout(new BorderLayout());
        pnlThietHai.setBorder(new EmptyBorder(15, 20, 15, 20)); 
        pnlThietHai.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlThietHai.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80)); 
        
        JLabel lblThietHaiTitle = new JLabel("TỔNG THIỆT HẠI ƯỚC TÍNH");
        lblThietHaiTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblThietHaiTitle.setForeground(PASTEL_RED_FG);
        
        lblTongThietHai = new JLabel("0 VNĐ");
        lblTongThietHai.setFont(new Font("Segoe UI", Font.BOLD, 22)); 
        lblTongThietHai.setForeground(PASTEL_RED_FG);
        lblTongThietHai.setHorizontalAlignment(SwingConstants.RIGHT); 
        
        pnlThietHai.add(lblThietHaiTitle, BorderLayout.NORTH);
        pnlThietHai.add(lblTongThietHai, BorderLayout.SOUTH);

        JPanel pnlAction = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlAction.setBackground(BG_CARD);
        pnlAction.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlAction.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        
        btnLuuTam = createModernButton("Lưu nháp", new Color(241, 245, 249), new Color(71, 85, 105));
        btnHoanTat = createModernButton("Hoàn tất hủy", PASTEL_BLUE_FG, Color.WHITE);
        btnHoanTat.addActionListener(e -> xuLyHoanTatTieuHuy());
        
        pnlAction.add(btnLuuTam);
        pnlAction.add(btnHoanTat);
        
        // Note Quy trình
        PanelBoGoc pnlNote = new PanelBoGoc(8, PASTEL_BLUE_BG, PASTEL_BLUE_BG, 0);
        pnlNote.setLayout(new BorderLayout(10, 0));
        pnlNote.setBorder(new EmptyBorder(10, 10, 10, 10));
        pnlNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlNote.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel lblIconInfo = new JLabel("ℹ"); lblIconInfo.setForeground(PASTEL_BLUE_FG); lblIconInfo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel lblTextInfo = new JLabel("<html><span style='font-size:9px; color:#3B82F6;'><b>Quy trình xử lý</b><br>Sau khi hoàn tất, hệ thống sẽ tự động trừ tồn kho.</span></html>");
        pnlNote.add(lblIconInfo, BorderLayout.WEST);
        pnlNote.add(lblTextInfo, BorderLayout.CENTER);

        pnlBottom.add(pnlThietHai);
        pnlBottom.add(Box.createVerticalStrut(20));
        pnlBottom.add(pnlAction);
        pnlBottom.add(Box.createVerticalStrut(20));
        pnlBottom.add(pnlNote);

        pnlPhai.add(pnlContent, BorderLayout.CENTER);
        pnlPhai.add(pnlBottom, BorderLayout.SOUTH);

        return pnlPhai;
    }
    
    private JLabel createSummaryLabel(String text) {
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lbl.setForeground(TEXT_SUB); return lbl;
    }
    private JLabel createSummaryValue(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.RIGHT); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(TEXT_TITLE); return lbl;
    }

    private JButton createModernButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                if (!isEnabled()) g2.setColor(BORDER_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                FontMetrics fm = g2.getFontMetrics();
                Rectangle r = fm.getStringBounds(getText(), g2).getBounds();
                g2.setColor(isEnabled() ? fg : TEXT_SUB);
                g2.drawString(getText(), (getWidth() - r.width) / 2, (getHeight() - r.height) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false); btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // =====================================================================
    // LOGIC TÌM KIẾM, RENDER & TÍNH TOÁN
    // =====================================================================

    private void setupDebounceSearch() {
        debounceTimer = new Timer(300, e -> timKiemRealtime());
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
        String tuKhoaThuong = (tuKhoa != null && !tuKhoa.equals("Tìm theo tên, mã lô...")) 
                ? GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tuKhoa.toLowerCase().trim()) : "";

        LocalDate today = LocalDate.now();
        List<ChiTietLoHang> dsLoc = new ArrayList<>();

        for (ChiTietLoHang lo : danhSachGocCache) {
            boolean matchTuKhoa = true;
            if (!tuKhoaThuong.isEmpty()) {
                String tenSP = mapTenSanPham.getOrDefault(lo.getMaSP(), "");
                String matchStr = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet((tenSP + " " + lo.getMaLoHang()).toLowerCase());
                if (!matchStr.contains(tuKhoaThuong)) matchTuKhoa = false;
            }
            if (!matchTuKhoa) continue;

            boolean matchTrangThai = true;
            long days = (lo.getHSD() != null) ? ChronoUnit.DAYS.between(today, lo.getHSD()) : 9999;
            if (currentFilter.equals("Hết hạn") && days >= 0) matchTrangThai = false;
            else if (currentFilter.equals("Cận date") && (days < 0 || days > 7)) matchTrangThai = false;
            else if (currentFilter.equals("Bình thường") && days <= 7) matchTrangThai = false;
            
            if (matchTrangThai) dsLoc.add(lo);
        }

        // Sắp xếp đưa những mục được check lên đầu
        dsLoc.sort((lo1, lo2) -> {
            boolean s1 = danhSachChon.contains(lo1);
            boolean s2 = danhSachChon.contains(lo2);
            if (s1 != s2) return s1 ? -1 : 1; 
            
            int p1 = getPriority(lo1, today);
            int p2 = getPriority(lo2, today);
            if (p1 != p2) return Integer.compare(p1, p2);
            if (lo1.getHSD() == null) return 1;
            if (lo2.getHSD() == null) return -1;
            return lo1.getHSD().compareTo(lo2.getHSD());
        });

        hienThiDanhSachLenUI(dsLoc);
    }

    private int getPriority(ChiTietLoHang lo, LocalDate today) {
        if (lo.getHSD() == null) return 2;
        long days = ChronoUnit.DAYS.between(today, lo.getHSD());
        if (days < 0) return 0;   
        if (days <= 7) return 1; 
        return 2;                
    }

    private void hienThiDanhSachLenUI(List<ChiTietLoHang> danhSach) {
        pnlDanhSachCard.removeAll();
        
        if (danhSach.isEmpty()) {
            JLabel lblRong = new JLabel("  Không có dữ liệu phù hợp.");
            lblRong.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            lblRong.setForeground(TEXT_SUB);
            pnlDanhSachCard.add(lblRong);
        } else {
            for (ChiTietLoHang lo : danhSach) { 
                JPanel card = taoCardItemUI(lo);
                pnlDanhSachCard.add(card);
                pnlDanhSachCard.add(Box.createVerticalStrut(12));
            }
        }
        
        pnlDanhSachCard.revalidate();
        pnlDanhSachCard.repaint();
    }

    // TÍNH TỔNG CẬP NHẬT BẢNG BÊN PHẢI
    private void tinhToanTongThietHai() {
        SwingUtilities.invokeLater(() -> {
            int tongHuy = 0;
            BigDecimal tongTien = BigDecimal.ZERO;
            boolean coLoi = false;

            for (ChiTietLoHang lo : danhSachChon) {
                String key = lo.getMaLoHang() + "_" + lo.getMaSP();
                int sl = mapSoLuongHuy.getOrDefault(key, lo.getSoLuongTon());
                
                if (sl < 0 || sl > lo.getSoLuongTon()) {
                    coLoi = true; break; 
                }
                tongHuy += sl;
                tongTien = tongTien.add(lo.getGiaNhap().multiply(new BigDecimal(sl)));
            }

            lblSoLuongMatHang.setText(String.format("%02d sản phẩm", danhSachChon.size()));
            lblTongSoLuongHuy.setText(tongHuy + " đơn vị");

            if (coLoi) {
                lblTongThietHai.setText("Lỗi số lượng");
                btnHoanTat.setEnabled(false);
            } else {
                lblTongThietHai.setText(moneyFormat.format(tongTien));
                btnHoanTat.setEnabled(!danhSachChon.isEmpty());
            }
        });
    }

    // =====================================================================
    // THIẾT KẾ LẠI CARD UI (GIỐNG HÌNH THAM KHẢO)
    // =====================================================================
    private JPanel taoCardItemUI(ChiTietLoHang lo) { 
        String key = lo.getMaLoHang() + "_" + lo.getMaSP();
        boolean isSelected = danhSachChon.contains(lo);
        
        PanelBoGoc card = new PanelBoGoc(12, isSelected ? BG_CARD : BG_CARD, 
                                        isSelected ? PASTEL_RED_FG : BORDER_COLOR, 1);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.CENTER;
        
        // 1. Checkbox
        ModernCheckBox cbSelect = new ModernCheckBox();
        cbSelect.setSelected(isSelected);
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 0, 15);
        card.add(cbSelect, gbc);

        // 2. Ảnh Sản Phẩm
        String tenAnh = mapAnhSanPham.getOrDefault(lo.getMaSP(), "");
        
        JLabel lblImg = new JLabel("", SwingConstants.CENTER);
        lblImg.setPreferredSize(new Dimension(50, 50));
        lblImg.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        ImageIcon icon = Logic.QuanLyAnh.layIconAnh(tenAnh, 50, 50);
        if (icon != null) lblImg.setIcon(icon); else { lblImg.setText("SP"); lblImg.setForeground(TEXT_SUB); }
        
        gbc.gridx = 1; gbc.insets = new Insets(0, 0, 0, 15);
        card.add(lblImg, gbc);

        // 3. Thông tin (Tên, Mã, HSD)
        JPanel pnlInfo = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlInfo.setOpaque(false);
        String tenSP = mapTenSanPham.getOrDefault(lo.getMaSP(), "Sản phẩm ẩn danh");
        String hsdStr = (lo.getHSD() != null) ? lo.getHSD().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Không có";
        
        JLabel lblName = new JLabel(tenSP);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblName.setForeground(TEXT_TITLE);
        
        JLabel lblSub = new JLabel("Mã: " + lo.getMaLoHang() + "   |   HSD: " + hsdStr);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_SUB);
        
        pnlInfo.add(lblName);
        pnlInfo.add(lblSub);
        
        gbc.gridx = 2; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(0, 0, 0, 10);
        card.add(pnlInfo, gbc);

                // 4. Ô NHẬP HỦY / TỒN (ĐÃ ĐẢO NGƯỢC VỊ TRÍ)
        JPanel pnlInputWrap = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlInputWrap.setOpaque(false);
        
        // 🟢 SỬA TEXT TIÊU ĐỀ
        JLabel lblTonHuyTitle = new JLabel("Hủy / Tồn", SwingConstants.CENTER); 
        lblTonHuyTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTonHuyTitle.setForeground(TEXT_SUB);
        
        JPanel pnlInputBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        pnlInputBox.setOpaque(false);
        
        // Khôi phục giá trị đã nhập nếu có, mặc định là bằng Tồn kho
        int currentSL = mapSoLuongHuy.getOrDefault(key, lo.getSoLuongTon());
        JTextField txtHuy = new JTextField(String.valueOf(currentSL), 3);
        txtHuy.setHorizontalAlignment(SwingConstants.CENTER);
        txtHuy.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtHuy.setForeground(TEXT_TITLE);
        txtHuy.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(4, 4, 4, 4)
        ));
        
        // 🟢 SỬA TEXT LABEL TỒN (Đưa dấu gạch chéo lên đằng trước số Tồn)
        JLabel lblTonVal = new JLabel(" / " + lo.getSoLuongTon());
        lblTonVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTonVal.setForeground(TEXT_TITLE);
        
        // Lắng nghe sự kiện gõ phím
        txtHuy.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateHuy(); }
            public void removeUpdate(DocumentEvent e) { updateHuy(); }
            public void changedUpdate(DocumentEvent e) { updateHuy(); }
            private void updateHuy() {
                try {
                    int sl = Integer.parseInt(txtHuy.getText().trim());
                    mapSoLuongHuy.put(key, sl);
                    tinhToanTongThietHai();
                } catch (Exception ex) {}
            }
        });
        
        // 🟢 ĐẢO LẠI THỨ TỰ ADD VÀO PANEL (Ô nhập txtHuy vào trước)
        pnlInputBox.add(txtHuy);    
        pnlInputBox.add(lblTonVal); 
        
        pnlInputWrap.add(lblTonHuyTitle);
        pnlInputWrap.add(pnlInputBox);

        gbc.gridx = 3; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.insets = new Insets(0, 0, 0, 20);
        card.add(pnlInputWrap, gbc);

        // 5. Badge trạng thái
        JLabel lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblStatus.setOpaque(true);
        lblStatus.setBorder(new EmptyBorder(4, 12, 4, 12));
        
        if (lo.getHSD() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), lo.getHSD());
            if (days < 0) { 
                lblStatus.setText("HẾT HẠN"); 
                lblStatus.setBackground(PASTEL_RED_BG);
                lblStatus.setForeground(PASTEL_RED_FG); 
            } else if (days <= 7) { 
                lblStatus.setText("CẬN DATE"); 
                lblStatus.setBackground(PASTEL_ORANGE_BG);
                lblStatus.setForeground(PASTEL_ORANGE_FG);
            } else { 
                lblStatus.setText("BÌNH THƯỜNG"); 
                lblStatus.setBackground(PASTEL_GREEN_BG);
                lblStatus.setForeground(PASTEL_GREEN_FG);
            }
        } else { 
            lblStatus.setText("BÌNH THƯỜNG"); 
            lblStatus.setBackground(PASTEL_GREEN_BG);
            lblStatus.setForeground(PASTEL_GREEN_FG);
        }
        
        gbc.gridx = 4; gbc.insets = new Insets(0, 0, 0, 0);
        card.add(lblStatus, gbc);

        // Sự kiện Click chọn thẻ
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (danhSachChon.contains(lo)) danhSachChon.remove(lo);
                else danhSachChon.add(lo);
                
                // Mặc định nạp số lượng hủy = Tồn kho nếu chưa từng nhập
                if (!mapSoLuongHuy.containsKey(key)) mapSoLuongHuy.put(key, lo.getSoLuongTon());
                
                timKiemRealtime();
                tinhToanTongThietHai(); 
            }
        };
        card.addMouseListener(ma);
        cbSelect.addMouseListener(ma); // Click checkbox cũng có tác dụng như click Card

        return card;
    }

    // =====================================================================
    // HÀM XỬ LÝ LƯU DATABASE (Giữ nguyên logic)
    // =====================================================================
    private void xuLyHoanTatTieuHuy() {
        if (danhSachChon.isEmpty()) return;

        try {
            String lyDoChung = cbLyDoHuy.getSelectedItem().toString();
            if (lyDoChung.equals("Khác...")) lyDoChung = txtLyDoKhac.getText().trim();

            int tongSoLuong = 0;
            BigDecimal tongGiaTriHuy = BigDecimal.ZERO;

            for (Data.ChiTietLoHang lo : danhSachChon) {
                String key = lo.getMaLoHang() + "_" + lo.getMaSP();
                int slHuy = mapSoLuongHuy.getOrDefault(key, lo.getSoLuongTon());
                
                if (slHuy <= 0 || slHuy > lo.getSoLuongTon()) {
                    JOptionPane.showMessageDialog(this, "Số lượng hủy của lô " + lo.getMaLoHang() + " không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                tongSoLuong += slHuy;
                BigDecimal giaTriTungLo = lo.getGiaNhap().multiply(new BigDecimal(slHuy));
                tongGiaTriHuy = tongGiaTriHuy.add(giaTriTungLo);
            }

            Data.PhieuTieuHuy phieu = new Data.PhieuTieuHuy();
            phieu.setMaNV(this.maNVHienTai); 
            phieu.setTongSoLuong(tongSoLuong); 
            phieu.setTongGiaTriHuy(tongGiaTriHuy); 
            phieu.setLyDoHuy(lyDoChung);
            Logic.PhieuTieuHuyLogic.getInstance().taoPhieuTieuHuy(phieu); 

            for (Data.ChiTietLoHang lo : danhSachChon) {
                String key = lo.getMaLoHang() + "_" + lo.getMaSP();
                int slHuy = mapSoLuongHuy.getOrDefault(key, lo.getSoLuongTon());
                BigDecimal giaTriHuyChiTiet = lo.getGiaNhap().multiply(new BigDecimal(slHuy));

                Data.ChiTietPhieuHuy ct = new Data.ChiTietPhieuHuy();
                ct.setMaPhieuHuy(phieu.getMaPhieuHuy()); 
                ct.setMaLoHang(lo.getMaLoHang());
                ct.setMaSP(lo.getMaSP());
                ct.setSoLuongHuy(slHuy);
                ct.setGiaTriHuy(giaTriHuyChiTiet); 
                ct.setLyDoChiTiet(lyDoChung);
                Logic.ChiTietPhieuHuyLogic.getInstance().themChiTietPhieuHuy(ct);
            }
            
            Logic.PhieuTieuHuyLogic.getInstance().hoanTatPhieuTieuHuy(phieu.getMaPhieuHuy());
            GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, "Đã lưu thông tin tiêu hủy (Gồm " + danhSachChon.size() + " chi tiết)!", "SUCCESS");
            
            danhSachChon.clear();
            mapSoLuongHuy.clear();
            tinhToanTongThietHai(); 
            taiDanhSachHangCanHuyAsync(); 

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu: " + ex.getMessage(), "Lỗi Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    // CLASSES & UTILS UI HỖ TRỢ
    // =====================================================================
    private Map<String, String> mapAnhSanPham = new HashMap<>();
    private void taiDanhSachHangCanHuyAsync() {
        pnlDanhSachCard.removeAll();
        pnlDanhSachCard.revalidate();
        pnlDanhSachCard.repaint();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Dao.TruyVanSieuTocDAO.DuLieuTieuHuyDTO dataTurbo = 
                    Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuKhoSieuToc();
                danhSachGocCache = dataTurbo.dsLoHangKho;
                mapTenSanPham = dataTurbo.mapTenSanPham;
                mapAnhSanPham = dataTurbo.mapAnhSanPham; // ✅ THÊM DÒNG NÀY
                return null;
            }
            @Override
            protected void done() {
                try { get(); timKiemRealtime(); capNhatThongKe(); } 
                catch (Exception e) { 
                    JOptionPane.showMessageDialog(TieuHuySanPhamGUI.this, "Lỗi tải dữ liệu: " + e.getMessage()); 
                }
            }
        };
        worker.execute();
    }

    private void capNhatThongKe() {
        int countHetHan = 0, countCanDate = 0;
        BigDecimal uocTinhThietHai = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        for (ChiTietLoHang lo : danhSachGocCache) {
            if (lo.getHSD() != null) {
                long days = ChronoUnit.DAYS.between(today, lo.getHSD());
                if (days < 0) {
                    countHetHan++;
                    uocTinhThietHai = uocTinhThietHai.add(lo.getGiaNhap().multiply(new BigDecimal(lo.getSoLuongTon())));
                } else if (days <= 7) countCanDate++;
            }
        }

        int countPhieuHuyThangNay = 0;
        try {
            List<PhieuTieuHuy> dsPhieu = Dao.PhieuTieuHuyDAO.getInstance().layDanhSachPhieuTieuHuy();
            for(PhieuTieuHuy p : dsPhieu) {
                if (p.getNgayTao() != null && p.getNgayTao().getMonthValue() == today.getMonthValue() && p.getNgayTao().getYear() == today.getYear()) countPhieuHuyThangNay++;
            }
        } catch(Exception e) {}

        lblStatHetHan.setText(countHetHan + " Lô");
        lblStatCanDate.setText(countCanDate + " Lô");
        lblStatThatThoat.setText(countPhieuHuyThangNay + " Phiếu");
        lblStatThietHai.setText(GUI.HoTro.DinhDangUtil.dinhDangTien(uocTinhThietHai));
    }

    private static class PanelBoGoc extends JPanel {
        private int radius; private Color bgColor, borderColor; private int borderThickness;
        public PanelBoGoc(int radius, Color bg, Color border, int thickness) {
            this.radius = radius; this.bgColor = bg; this.borderColor = border; this.borderThickness = thickness; setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (borderThickness > 0) { g2.setColor(borderColor); g2.setStroke(new BasicStroke(borderThickness)); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius); }
            g2.dispose(); super.paintComponent(g);
        }
    }

    private static class CustomScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { this.thumbColor = new Color(203, 213, 225); this.trackColor = BG_MAIN; }
        @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor); g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
            g2.dispose();
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}
    }

    private class ModernCheckBox extends JCheckBox {
        public ModernCheckBox() { setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = 18; int y = (getHeight() - size) / 2; int x = (getWidth() - size) / 2; 
            if (isSelected()) {
                g2.setColor(PASTEL_RED_FG); g2.fillRoundRect(x, y, size, size, 6, 6); 
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 9, x + 8, y + 13); g2.drawLine(x + 8, y + 13, x + 14, y + 5); 
            } else {
                g2.setColor(Color.WHITE); g2.fillRoundRect(x, y, size, size, 6, 6);
                g2.setColor(BORDER_COLOR); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(x, y, size, size, 6, 6);
            }
            g2.dispose();
        }
    }

    // =====================================================================
    // GIAO TIẾP VỚI MODULE KHÁC
    // =====================================================================
    public void chonNhanhVaSetLyDo(String maLo, String lyDo) {
        Timer waitTimer = new Timer(100, null);
        waitTimer.addActionListener(e -> {
            if (danhSachGocCache != null && !danhSachGocCache.isEmpty()) {
                ((Timer) e.getSource()).stop(); 
                ChiTietLoHang item = null;
                for (ChiTietLoHang lo : danhSachGocCache) {
                    if (lo.getMaLoHang().trim().equalsIgnoreCase(maLo.trim())) { item = lo; break; }
                }
                if (item != null) {
                    if (!danhSachChon.contains(item)) danhSachChon.add(item);
                    if (lyDo != null) cbLyDoHuy.setSelectedItem(lyDo);
                    timKiemRealtime();
                    tinhToanTongThietHai();
                }
            }
        });
        waitTimer.start();
    }

    public void nhanDuLieuChoTieuHuyNgam(String maLo, String lyDo) {
        Timer waitTimer = new Timer(100, null);
        waitTimer.addActionListener(e -> {
            if (danhSachGocCache != null && !danhSachGocCache.isEmpty()) {
                ((Timer) e.getSource()).stop(); 
                for (Data.ChiTietLoHang lo : danhSachGocCache) {
                    if (lo.getMaLoHang().trim().equalsIgnoreCase(maLo.trim())) {
                        if (!danhSachChon.contains(lo)) danhSachChon.add(0, lo);
                        SwingUtilities.invokeLater(() -> {
                            timKiemRealtime();        
                            tinhToanTongThietHai();  
                            cbLyDoHuy.setSelectedItem(lyDo != null ? lyDo : "Hàng hết hạn");
                        });
                        break;
                    }
                }
            }
        });
        waitTimer.start();
    }

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