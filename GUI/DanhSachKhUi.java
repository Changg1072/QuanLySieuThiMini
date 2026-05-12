package GUI;

import Data.KhachHang;
import Logic.KhachHangLogic;
import GUI.HoTro.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🎨 SẢN PHẨM: DASHBOARD KHÁCH HÀNG (V6 - ROUNDED UI & SMART POPUP)
 * - Lịch Pop-up bo góc tròn (Border-radius) chuẩn UI/UX hiện đại.
 * - Đưa hướng dẫn (Hint 15 ngày) vào thẳng trong Popup Lịch.
 * - Giao diện chính sạch sẽ, không text thừa.
 */
public class DanhSachKhUi extends JPanel {

    // ================= MÀU SẮC CHỦ ĐẠO =================
    private static final Color BG_MAIN = new Color(245, 247, 250); 
    private static final Color BG_FILTER = new Color(238, 242, 246);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    
    // ================= DỮ LIỆU =================
    private List<KhachHang> danhSachGoc = new ArrayList<>();
    private DefaultListModel<KhachHang> listModel;
    private KhachHangLogic khLogic = new KhachHangLogic();

    // ================= TRẠNG THÁI =================
    private String currentHangFilter = "Tất cả";
    private String currentNgayFilter = "Tất cả";
    private boolean isCustomDateActive = false;
    private LocalDate customFromDate = null;
    private LocalDate customToDate = null;
    private boolean isLoading = true;

    // ================= UI COMPONENTS =================
    private ONhapLieuHienDai txtTimKiem;
    private PillMenu tabHang, tabNgay;
    
    // 🔥 Nút Calendar siêu gọn
    private JButton btnChonNgay;
    private DatePickerPopup calendarPopup; 
    
    private JList<KhachHang> listKhachHang;
    private int hoveredIndex = -1; 

    private JLabel lblTongKhach, lblKhachVIP, lblKhachMoiThang;
    private ChartPanel chartPanel;

    public DanhSachKhUi() {
        setLayout(new BorderLayout(0, 0)); 
        setBackground(BG_MAIN);
        initUI();
        loadDataWithSkeleton();
        setupListeners();
    }

    private void initUI() {
        JPanel pnlTopSticky = new JPanel(new BorderLayout());
        pnlTopSticky.setOpaque(false);

        // --- ZONE 1: HEADER ---
        JPanel pnlHeader = new ShadowPanel(Color.WHITE, 0, 4); 
        pnlHeader.setLayout(new BorderLayout(20, 0));
        pnlHeader.setBorder(new EmptyBorder(15, 25, 15, 25));

        txtTimKiem = new ONhapLieuHienDai("Tìm kiếm khách hàng...", true, false);
        txtTimKiem.setPlaceholder("Tìm Kiếm theo tên hoặc số điện thoại...");
        txtTimKiem.setPreferredSize(new Dimension(450, 65));
        
        JButton btnRefresh = TienIchGiaoDien.taoNutHienDai("Làm mới ↻", TienIchGiaoDien.MAU_CHINH);
        btnRefresh.setPreferredSize(new Dimension(130, 45));
        btnRefresh.addActionListener(e -> { resetFilters(); loadDataWithSkeleton(); });

        pnlHeader.add(txtTimKiem, BorderLayout.WEST);
        
        JPanel pnlHeaderRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        pnlHeaderRight.setOpaque(false);
        pnlHeaderRight.add(btnRefresh);
        pnlHeader.add(pnlHeaderRight, BorderLayout.EAST);

        // --- ZONE 2: FILTER ZONE ---
        JPanel pnlFilterZone = new JPanel(new BorderLayout(0, 15));
        pnlFilterZone.setBackground(BG_FILTER);
        pnlFilterZone.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, BORDER_COLOR),
            new EmptyBorder(15, 25, 15, 25)
        ));

        tabHang = new PillMenu(Arrays.asList("Tất cả", "VIP", "Vàng", "Bạc", "Không hạng"), tab -> {
            currentHangFilter = tab; xuLyBoLoc();
        });

        JPanel pnlTimeFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlTimeFilter.setOpaque(false);
        
        tabNgay = new PillMenu(Arrays.asList("Tất cả", "Hôm nay", "7 ngày", "30 ngày", "Tháng này"), tab -> {
            currentNgayFilter = tab;
            isCustomDateActive = false; 
            if(btnChonNgay != null) btnChonNgay.setText("🗓️ Chọn ngày..."); 
            xuLyBoLoc();
        });

        // 🔥 GIAO DIỆN CHỌN NGÀY SIÊU GỌN (Chỉ còn nút bấm và Xóa lọc)
        JPanel pnlInputDate = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlInputDate.setOpaque(false);
        
        btnChonNgay = new JButton("🗓️ Chọn ngày...");
        btnChonNgay.setPreferredSize(new Dimension(200, 38));
        btnChonNgay.setFont(TienIchGiaoDien.FONT_DAM);
        btnChonNgay.setBackground(Color.WHITE);
        btnChonNgay.setForeground(TienIchGiaoDien.MAU_CHU_CHINH);
        btnChonNgay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChonNgay.setBorder(new EmptyBorder(5, 15, 5, 15)); // Bỏ viền vuông
        
        // Bo góc cho nút chọn ngày
        btnChonNgay.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 20, 20); // Góc bo tròn nhẹ
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, c.getWidth()-1, c.getHeight()-1, 20, 20);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        btnChonNgay.setContentAreaFilled(false);
        
        calendarPopup = new DatePickerPopup();
        btnChonNgay.addActionListener(e -> calendarPopup.show(btnChonNgay, 0, btnChonNgay.getHeight() + 5));

        JButton btnXoaLoc = new JButton("Xóa lọc");
        btnXoaLoc.setFont(TienIchGiaoDien.FONT_DAM);
        btnXoaLoc.setForeground(new Color(239, 68, 68)); 
        btnXoaLoc.setContentAreaFilled(false); btnXoaLoc.setBorderPainted(false);
        btnXoaLoc.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnXoaLoc.addActionListener(e -> {
            btnChonNgay.setText("🗓️ Chọn ngày..."); 
            tabNgay.setActiveTab("Tất cả");
            isCustomDateActive = false;
            xuLyBoLoc();
        });

        pnlInputDate.add(new JLabel(" Hoặc từ ngày: "));
        pnlInputDate.add(btnChonNgay); 
        pnlInputDate.add(btnXoaLoc);

        pnlTimeFilter.add(tabNgay);
        pnlTimeFilter.add(pnlInputDate);

        pnlFilterZone.add(createFilterWrapper("HẠNG THÀNH VIÊN", tabHang), BorderLayout.NORTH);
        pnlFilterZone.add(createFilterWrapper("THỜI GIAN ĐĂNG KÍ", pnlTimeFilter), BorderLayout.SOUTH);

        pnlTopSticky.add(pnlHeader, BorderLayout.NORTH);
        pnlTopSticky.add(pnlFilterZone, BorderLayout.SOUTH);

        // --- ZONE 3 & 4: MAIN CONTENT ---
        JPanel pnlBody = new JPanel(new BorderLayout(20, 0));
        pnlBody.setOpaque(false);
        pnlBody.setBorder(new EmptyBorder(20, 25, 20, 25)); 

        listModel = new DefaultListModel<>();
        listKhachHang = new JList<>(listModel);
        listKhachHang.setCellRenderer(new CustomerRowItem()); 
        listKhachHang.setFixedCellHeight(95); 
        listKhachHang.setBackground(BG_MAIN);
        listKhachHang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listKhachHang.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JScrollPane scrollList = new JScrollPane(listKhachHang);
        TienIchGiaoDien.thietLapThanhCuon(scrollList);
        scrollList.getViewport().setBackground(BG_MAIN);

        JPanel pnlSidebar = new JPanel(new BorderLayout(0, 20));
        pnlSidebar.setOpaque(false);
        pnlSidebar.setPreferredSize(new Dimension(360, 0));

        JPanel pnlStats = new JPanel(new GridLayout(3, 1, 0, 15));
        pnlStats.setOpaque(false);
        lblTongKhach = createStatCard(pnlStats, "Tổng khách hàng", "0", TienIchGiaoDien.MAU_CHINH);
        lblKhachVIP = createStatCard(pnlStats, "Tỷ lệ VIP", "0%", new Color(245, 158, 11));
        lblKhachMoiThang = createStatCard(pnlStats, "Mới tháng này", "0", new Color(16, 185, 129));

        chartPanel = new ChartPanel();
        pnlSidebar.add(pnlStats, BorderLayout.NORTH);
        pnlSidebar.add(chartPanel, BorderLayout.CENTER);

        pnlBody.add(scrollList, BorderLayout.CENTER);
        pnlBody.add(pnlSidebar, BorderLayout.EAST);

        add(pnlTopSticky, BorderLayout.NORTH);
        add(pnlBody, BorderLayout.CENTER);
    }

    private JPanel createFilterWrapper(String title, JComponent comp) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 5));
        wrapper.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(11f));
        lblTitle.setForeground(new Color(100, 116, 139));
        wrapper.add(lblTitle, BorderLayout.NORTH);
        wrapper.add(comp, BorderLayout.CENTER);
        return wrapper;
    }

    private void resetFilters() {
        txtTimKiem.setText("");
        tabHang.setActiveTab("Tất cả"); currentHangFilter = "Tất cả";
        tabNgay.setActiveTab("Tất cả"); currentNgayFilter = "Tất cả";
        isCustomDateActive = false;
        btnChonNgay.setText("🗓️ Chọn ngày..."); 
    }

    private void applyCustomDate(LocalDate selectedDate) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        customFromDate = selectedDate;
        customToDate = customFromDate.plusDays(14); 

        // Update nút bấm thành khoảng thời gian 15 ngày
        btnChonNgay.setText("Từ " + customFromDate.format(dtf) + " (+15 ngày)");
        btnChonNgay.setForeground(new Color(16, 185, 129)); // Chuyển xanh báo hiệu đã chọn

        isCustomDateActive = true;
        tabNgay.clearActive(); 
        xuLyBoLoc();
    }

    private void loadDataWithSkeleton() {
        isLoading = true; listModel.clear();
        for(int i=0; i<8; i++) listModel.addElement(null); listKhachHang.repaint();
        Timer timer = new Timer(400, e -> {
            danhSachGoc = khLogic.layDanhSachKhachHang();
            if(danhSachGoc == null) danhSachGoc = new ArrayList<>();
            isLoading = false; capNhatDashboard(); xuLyBoLoc();
        });
        timer.setRepeats(false); timer.start();
    }

    private void xuLyBoLoc() {
        if(isLoading) return;
        String keyword = DinhDangUtil.loaiBoDauTiengViet(txtTimKiem.getText().toLowerCase().trim());
        LocalDate now = LocalDate.now();

        List<KhachHang> dsLoc = danhSachGoc.stream().filter(kh -> {
            boolean matchKey = keyword.isEmpty() || 
                (kh.getHoTen() != null && DinhDangUtil.loaiBoDauTiengViet(kh.getHoTen().toLowerCase()).contains(keyword)) ||
                (kh.getSDT() != null && kh.getSDT().contains(keyword));
            boolean matchHang = currentHangFilter.equals("Tất cả") || 
                (kh.getBacKH() != null && kh.getBacKH().equalsIgnoreCase(currentHangFilter));
            boolean matchNgay = true;
            LocalDate dk = kh.getNgayDangKy();
            if (dk != null) {
                if (isCustomDateActive) {
                    matchNgay = !dk.isBefore(customFromDate) && !dk.isAfter(customToDate);
                } else if (!currentNgayFilter.equals("Tất cả")) {
                    switch (currentNgayFilter) {
                        case "Hôm nay": matchNgay = dk.isEqual(now); break;
                        case "7 ngày": matchNgay = dk.isAfter(now.minusDays(7)) || dk.isEqual(now.minusDays(7)); break;
                        case "30 ngày": matchNgay = dk.isAfter(now.minusDays(30)) || dk.isEqual(now.minusDays(30)); break;
                        case "Tháng này": matchNgay = (dk.getMonthValue() == now.getMonthValue() && dk.getYear() == now.getYear()); break;
                    }
                }
            }
            return matchKey && matchHang && matchNgay;
        }).collect(Collectors.toList());

        dsLoc.sort((k1, k2) -> {
            int w1 = "Vip".equalsIgnoreCase(k1.getBacKH()) ? 1 : 0;
            int w2 = "Vip".equalsIgnoreCase(k2.getBacKH()) ? 1 : 0;
            if (w1 != w2) return Integer.compare(w2, w1);
            LocalDate d1 = k1.getNgayDangKy() != null ? k1.getNgayDangKy() : LocalDate.MIN;
            LocalDate d2 = k2.getNgayDangKy() != null ? k2.getNgayDangKy() : LocalDate.MIN;
            if (!d1.isEqual(d2)) return d2.compareTo(d1);
            BigDecimal p1 = k1.getDiemTichLuy() != null ? k1.getDiemTichLuy() : BigDecimal.ZERO;
            BigDecimal p2 = k2.getDiemTichLuy() != null ? k2.getDiemTichLuy() : BigDecimal.ZERO;
            return p2.compareTo(p1);
        });

        listModel.clear();
        for (KhachHang kh : dsLoc) listModel.addElement(kh);
    }

    private void setupListeners() {
        txtTimKiem.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { xuLyBoLoc(); }
            public void removeUpdate(DocumentEvent e) { xuLyBoLoc(); }
            public void changedUpdate(DocumentEvent e) { xuLyBoLoc(); }
        });
        listKhachHang.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int index = listKhachHang.locationToIndex(e.getPoint());
                if (index != hoveredIndex) { hoveredIndex = index; listKhachHang.repaint(); }
            }
        });
        listKhachHang.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hoveredIndex = -1; listKhachHang.repaint(); }
        });
    }

    // =========================================================
    // 📅 COMPONENT: POPUP LỊCH BO TRÒN TÍCH HỢP HƯỚNG DẪN 
    // =========================================================
    private class DatePickerPopup extends JPopupMenu {
        private LocalDate currentViewMonth;
        private JPanel pnlGrid;
        private JLabel lblMonthYear;

        public DatePickerPopup() {
            currentViewMonth = LocalDate.now();
            setOpaque(false); // Xóa nền xám cứng nhắc của JPopupMenu
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); 

            // Panel chính vẽ Border-radius 
            JPanel pnlMain = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Vẽ bóng mờ nhẹ
                    g2.setColor(new Color(0, 0, 0, 15));
                    g2.fillRoundRect(2, 4, getWidth()-4, getHeight()-4, 20, 20);
                    
                    // Vẽ nền trắng bo góc
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth()-4, getHeight()-6, 20, 20);
                    
                    // Vẽ Header màu xanh lượn góc trên
                    g2.setColor(TienIchGiaoDien.MAU_CHINH);
                    g2.fillRoundRect(0, 0, getWidth()-4, 45, 20, 20);
                    g2.fillRect(0, 25, getWidth()-4, 20); // Vuông góc dưới của header
                    
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(0, 0, getWidth()-4, getHeight()-6, 20, 20);
                    g2.dispose();
                }
            };
            pnlMain.setOpaque(false);

            // --- HEADER LỊCH ---
            JPanel pnlHeader = new JPanel(new BorderLayout());
            pnlHeader.setOpaque(false);
            pnlHeader.setPreferredSize(new Dimension(280, 40));
            pnlHeader.setBorder(new EmptyBorder(0, 10, 0, 10));

            JButton btnPrev = createNavButton("◀");
            JButton btnNext = createNavButton("▶");
            btnPrev.addActionListener(e -> { currentViewMonth = currentViewMonth.minusMonths(1); renderCalendar(); });
            btnNext.addActionListener(e -> { currentViewMonth = currentViewMonth.plusMonths(1); renderCalendar(); });

            lblMonthYear = new JLabel("", SwingConstants.CENTER);
            lblMonthYear.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f));
            lblMonthYear.setForeground(Color.WHITE);

            pnlHeader.add(btnPrev, BorderLayout.WEST);
            pnlHeader.add(lblMonthYear, BorderLayout.CENTER);
            pnlHeader.add(btnNext, BorderLayout.EAST);

            // --- GRID LỊCH ---
            pnlGrid = new JPanel(new GridLayout(0, 7, 5, 5)); // Khoảng cách các ô
            pnlGrid.setOpaque(false);
            pnlGrid.setBorder(new EmptyBorder(10, 10, 5, 10));

            // --- FOOTER (HƯỚNG DẪN 15 NGÀY) ---
            JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.CENTER));
            pnlFooter.setOpaque(false);
            pnlFooter.setBorder(new EmptyBorder(5, 5, 10, 5));
            JLabel lblHint = new JLabel("✨ Hệ thống sẽ chọn 15 ngày tiếp theo");
            lblHint.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(11.5f));
            lblHint.setForeground(new Color(245, 158, 11)); // Màu cam
            pnlFooter.add(lblHint);

            pnlMain.add(pnlHeader, BorderLayout.NORTH);
            pnlMain.add(pnlGrid, BorderLayout.CENTER);
            pnlMain.add(pnlFooter, BorderLayout.SOUTH); // Gắn hướng dẫn vô đây!

            add(pnlMain);
            renderCalendar();
        }

        private JButton createNavButton(String text) {
            JButton btn = new JButton(text);
            btn.setContentAreaFilled(false); btn.setBorderPainted(false);
            btn.setForeground(Color.WHITE); btn.setFont(TienIchGiaoDien.FONT_DAM);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
        }

        private void renderCalendar() {
            pnlGrid.removeAll();
            lblMonthYear.setText("Tháng " + currentViewMonth.getMonthValue() + " / " + currentViewMonth.getYear());

            String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (String d : days) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(11f));
                lbl.setForeground(new Color(100, 116, 139));
                pnlGrid.add(lbl);
            }

            LocalDate firstDay = currentViewMonth.withDayOfMonth(1);
            int startDayOfWeek = firstDay.getDayOfWeek().getValue(); 
            int daysInMonth = currentViewMonth.lengthOfMonth();

            for (int i = 1; i < startDayOfWeek; i++) {
                pnlGrid.add(new JLabel(""));
            }

            for (int i = 1; i <= daysInMonth; i++) {
                int day = i;
                
                // Nút ngày bo tròn
                JButton btnDay = new JButton(String.valueOf(day)) {
                    @Override protected void paintComponent(Graphics g) {
                        if (getModel().isRollover() || LocalDate.now().equals(currentViewMonth.withDayOfMonth(day))) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(getModel().isRollover() ? new Color(226, 232, 240) : new Color(239, 246, 255));
                            g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 15, 15); // Nền tròn
                            g2.dispose();
                        }
                        super.paintComponent(g);
                    }
                };
                
                btnDay.setFont(TienIchGiaoDien.FONT_CHINH);
                btnDay.setForeground(TienIchGiaoDien.MAU_CHU_CHINH);
                btnDay.setContentAreaFilled(false);
                btnDay.setBorderPainted(false);
                btnDay.setFocusPainted(false);
                btnDay.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                if (LocalDate.now().equals(currentViewMonth.withDayOfMonth(day))) {
                    btnDay.setForeground(TienIchGiaoDien.MAU_CHINH);
                    btnDay.setFont(TienIchGiaoDien.FONT_DAM);
                }

                btnDay.addActionListener(e -> {
                    LocalDate selected = currentViewMonth.withDayOfMonth(day);
                    setVisible(false); 
                    applyCustomDate(selected); 
                });

                pnlGrid.add(btnDay);
            }
            revalidate(); repaint();
        }
    }

    // =========================================================
    // III. CUSTOM ROW COMPONENT (V7 - BỔ SUNG NGÀY ĐĂNG KÝ)
    // =========================================================
    private class CustomerRowItem extends JPanel implements ListCellRenderer<KhachHang> {
        private AvatarNhanVien avatar;
        private JLabel lblTen, lblInfo, lblBadge, lblIcon; // Đổi lblSDT thành lblInfo cho đa năng
        private CustomProgressBar pbDiem;

        public CustomerRowItem() {
            setLayout(new BorderLayout(20, 0)); setOpaque(false); 
            avatar = new AvatarNhanVien("?", Color.GRAY); avatar.setPreferredSize(new Dimension(55, 55)); 
            
            JPanel pnlCenter = new JPanel(new GridLayout(2, 1, 0, 6)); pnlCenter.setOpaque(false);
            
            // Dòng trên: Icon + Tên
            JPanel pnlName = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); pnlName.setOpaque(false);
            lblIcon = new JLabel("⭐ "); lblIcon.setForeground(new Color(245, 158, 11));
            lblTen = new JLabel("Name"); lblTen.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(16.5f)); lblTen.setForeground(TienIchGiaoDien.MAU_CHU_CHINH);
            pnlName.add(lblIcon); pnlName.add(lblTen);
            
            // Dòng dưới: Thông tin (SĐT + Ngày) + Thanh Progress
            JPanel pnlProgress = new JPanel(new BorderLayout(15, 0)); pnlProgress.setOpaque(false);
            
            // 🔥 Nâng cấp: Label này giờ sẽ chứa cả SĐT và Ngày ĐK
            lblInfo = new JLabel("090xxxxxxx • dd/MM/yyyy"); 
            lblInfo.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(13.0f)); // Nhỏ font lại chút xíu để vừa vặn
            lblInfo.setForeground(new Color(100, 116, 139)); 
            lblInfo.setPreferredSize(new Dimension(170, 20)); // Tăng độ rộng để chứa đủ chữ
            
            pbDiem = new CustomProgressBar(); pbDiem.setPreferredSize(new Dimension(220, 10)); 
            pnlProgress.add(lblInfo, BorderLayout.WEST); pnlProgress.add(pbDiem, BorderLayout.CENTER);
            
            pnlCenter.add(pnlName); pnlCenter.add(pnlProgress);

            // Badge Hạng (Bên phải)
            lblBadge = new JLabel("Hạng", SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); 
                    super.paintComponent(g); g2.dispose();
                }
            };
            lblBadge.setOpaque(false); lblBadge.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12.5f)); lblBadge.setForeground(Color.WHITE); lblBadge.setPreferredSize(new Dimension(90, 30));
            JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 15)); pnlRight.setOpaque(false); pnlRight.add(lblBadge);
            
            add(avatar, BorderLayout.WEST); add(pnlCenter, BorderLayout.CENTER); add(pnlRight, BorderLayout.EAST);
        }

        @Override public Component getListCellRendererComponent(JList<? extends KhachHang> list, KhachHang kh, int index, boolean isSelected, boolean cellHasFocus) {
            if (isLoading || kh == null) {
                avatar.setBgColor(new Color(226, 232, 240)); avatar.setLetter("");
                lblTen.setText("████████"); lblTen.setForeground(new Color(226, 232, 240));
                lblInfo.setText("█████ • ████"); lblInfo.setForeground(new Color(241, 245, 249));
                lblIcon.setVisible(false); pbDiem.setValue(0); lblBadge.setBackground(new Color(241, 245, 249)); lblBadge.setText("");
            } else {
                avatar.setLetter(kh.getHoTen()); 
                lblTen.setText(kh.getHoTen()); 
                lblTen.setForeground(TienIchGiaoDien.MAU_CHU_CHINH);
                
                // 🔥 Nâng cấp: Format Ngày Đăng Ký và ghép với SĐT
                String sdt = (kh.getSDT() != null && !kh.getSDT().isEmpty()) ? kh.getSDT() : "Trống SĐT";
                String ngayDK = "N/A";
                if (kh.getNgayDangKy() != null) {
                    ngayDK = kh.getNgayDangKy().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }
                
                // Ghép nối cực kỳ "chanh sả": 0901234567 • 04/05/2026
                lblInfo.setText(sdt + " • " + ngayDK);
                lblInfo.setForeground(new Color(100, 116, 139));
                
                BigDecimal diem = kh.getDiemTichLuy() != null ? kh.getDiemTichLuy() : BigDecimal.ZERO;
                pbDiem.setValue(diem.intValue()); pbDiem.setPointText(DinhDangUtil.dinhDangSo(diem));
                String hang = kh.getBacKH() != null ? kh.getBacKH().toLowerCase() : "không hạng"; lblIcon.setVisible(hang.equals("vip"));
                switch (hang) {
                    case "vip": lblBadge.setBackground(new Color(220, 38, 38)); lblBadge.setText("VIP"); pbDiem.setGradientColors(new Color(248, 113, 113), new Color(220, 38, 38)); avatar.setBgColor(new Color(254, 226, 226)); break;
                    case "vàng": lblBadge.setBackground(new Color(217, 119, 6)); lblBadge.setText("VÀNG"); pbDiem.setGradientColors(new Color(252, 211, 77), new Color(245, 158, 11)); avatar.setBgColor(new Color(254, 243, 199)); break;
                    case "bạc": lblBadge.setBackground(new Color(100, 116, 139)); lblBadge.setText("BẠC"); pbDiem.setGradientColors(new Color(148, 163, 184), new Color(71, 85, 105)); avatar.setBgColor(new Color(241, 245, 249)); break;
                    default: lblBadge.setBackground(new Color(148, 163, 184)); lblBadge.setText("THƯỜNG"); pbDiem.setGradientColors(new Color(45, 212, 191), new Color(14, 165, 233)); avatar.setBgColor(new Color(241, 245, 249));
                }
            }
            setBorder(new EmptyBorder(12, 25, 12, 25));
            putClientProperty("state", isSelected ? "selected" : (index == hoveredIndex && !isLoading) ? "hover" : "normal");
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String state = (String) getClientProperty("state"); int w = getWidth() - 6; int h = getHeight() - 6;
            if ("hover".equals(state) || "selected".equals(state)) {
                g2.setColor(new Color(0, 0, 0, 12)); g2.fillRoundRect(4, 5, w, h, 15, 15);
                g2.setColor(Color.WHITE); g2.fillRoundRect(3, 3, w, h, 15, 15);
                if("selected".equals(state)) { g2.setColor(TienIchGiaoDien.MAU_CHINH); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(3, 3, w, h, 15, 15); } 
                else { g2.setColor(BORDER_COLOR); g2.drawRoundRect(3, 3, w, h, 15, 15); }
            } else {
                g2.setColor(Color.WHITE); g2.fillRoundRect(3, 3, w, h, 15, 15); g2.setColor(BORDER_COLOR); g2.drawRoundRect(3, 3, w, h, 15, 15);
            }
            g2.dispose(); super.paintComponent(g);
        }
    }
    // =========================================================
    // IV. COMPONENTS PHỤ (PILL MENU, PROGRESS, CHART)
    // =========================================================
    private class PillMenu extends JPanel {
        private List<JButton> btns = new ArrayList<>();
        public PillMenu(List<String> items, java.util.function.Consumer<String> onSelect) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0)); setOpaque(false);
            for (String item : items) {
                JButton btn = new JButton(item) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        if (getClientProperty("active").equals(true)) { g2.setColor(TienIchGiaoDien.MAU_CHINH); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); } 
                        else { g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight()); }
                        g2.dispose(); super.paintComponent(g);
                    }
                };
                btn.putClientProperty("active", item.equals(items.get(0)));
                btn.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12.5f));
                btn.setForeground(item.equals(items.get(0)) ? Color.WHITE : TienIchGiaoDien.MAU_CHU_PHU);
                btn.setContentAreaFilled(false); btn.setBorderPainted(false); 
                btn.setFocusPainted(false);
                btn.setBorder(new EmptyBorder(8, 16, 8, 16)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> { setActiveTab(item); onSelect.accept(item); });
                btns.add(btn); add(btn);
            }
        }
        public void setActiveTab(String name) { for (JButton b : btns) { boolean isActive = b.getText().equals(name); b.putClientProperty("active", isActive); b.setForeground(isActive ? Color.WHITE : TienIchGiaoDien.MAU_CHU_PHU); b.repaint(); } }
        public void clearActive() { for (JButton b : btns) { b.putClientProperty("active", false); b.setForeground(TienIchGiaoDien.MAU_CHU_PHU); b.repaint(); } }
    }

    private class CustomProgressBar extends JPanel {
        private int value = 0; private int max = 10000; private Color c1 = Color.GRAY, c2 = Color.DARK_GRAY; private String text = "0 đ";
        public CustomProgressBar() { setOpaque(false); }
        public void setValue(int v) { this.value = Math.min(v, max); repaint(); }
        public void setGradientColors(Color start, Color end) { this.c1 = start; this.c2 = end; }
        public void setPointText(String t) { this.text = t; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 75; int h = 10; int y = (getHeight()-h)/2; int pW = (int) ((double) value / max * w); if (pW < h) pW = h; 
            g2.setColor(new Color(241, 245, 249)); g2.fillRoundRect(0, y, w, h, h, h);
            if (value > 0) { g2.setPaint(new GradientPaint(0, y, c1, pW, y, c2)); g2.fillRoundRect(0, y, pW, h, h, h); }
            g2.setColor(new Color(100, 116, 139)); g2.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12f)); FontMetrics fm = g2.getFontMetrics(); g2.drawString(text, w + 12, y + (h - fm.getHeight())/2 + fm.getAscent()); g2.dispose();
        }
    }

    private JLabel createStatCard(JPanel parent, String title, String val, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(color); g2.fillRoundRect(0, 0, 5, getHeight(), 5, 5); 
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16); g2.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(18, 25, 18, 15));
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(13f)); lblTitle.setForeground(new Color(100, 116, 139));
        JLabel lblVal = new JLabel(val); lblVal.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(28f)); lblVal.setForeground(color);
        card.add(lblTitle, BorderLayout.NORTH); card.add(lblVal, BorderLayout.CENTER); parent.add(card); return lblVal;
    }

    private void capNhatDashboard() {
        int tong = danhSachGoc.size(); lblTongKhach.setText(String.valueOf(tong));
        long vipCount = danhSachGoc.stream().filter(k -> "Vip".equalsIgnoreCase(k.getBacKH())).count(); lblKhachVIP.setText(tong > 0 ? (vipCount * 100 / tong) + "%" : "0%");
        long newThisMonth = danhSachGoc.stream().filter(k -> k.getNgayDangKy() != null && k.getNgayDangKy().getMonthValue() == LocalDate.now().getMonthValue() && k.getNgayDangKy().getYear() == LocalDate.now().getYear()).count(); lblKhachMoiThang.setText(String.valueOf(newThisMonth));
        chartPanel.setData(danhSachGoc);
    }

    // =========================================================
    // 📈 COMPONENT: BIỂU ĐỒ TĂNG TRƯỞNG (CHART PANEL)
    // =========================================================
    private class ChartPanel extends JPanel {
        // Mảng lưu trữ dữ liệu 12 tháng (Index 0 = Tháng 1, ..., Index 11 = Tháng 12)
        private int[] dataMonths = new int[12];

        public ChartPanel() { 
            setOpaque(false); // Trong suốt để ăn theo màu nền của Panel cha
            // Tạo viền (Border) có tiêu đề "📈 BIỂU ĐỒ TĂNG TRƯỞNG" cực kỳ tinh tế
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0,0,0,0)), // Viền tàng hình
                "📈 BIỂU ĐỒ TĂNG TRƯỞNG", 
                0, 0, 
                TienIchGiaoDien.FONT_DAM.deriveFont(13f), 
                new Color(148, 163, 184) // Màu xám nhạt sang trọng
            )); 
        }

        // 🛠️ HÀM NẠP DỮ LIỆU VÀO BIỂU ĐỒ
        public void setData(List<KhachHang> list) { 
            Arrays.fill(dataMonths, 0); // Reset data về 0 trước khi đếm lại
            int currentYear = LocalDate.now().getYear(); 
            
            for (KhachHang kh : list) { 
                // Chỉ lấy khách hàng đăng ký trong năm nay
                if (kh.getNgayDangKy() != null && kh.getNgayDangKy().getYear() == currentYear) { 
                    // Tăng biến đếm của tháng tương ứng (Tháng 1 -> Index 0)
                    dataMonths[kh.getNgayDangKy().getMonthValue() - 1]++; 
                } 
            } 
            repaint(); // Bắt buộc gọi để giao diện vẽ lại biểu đồ với data mới
        }

        // 🎨 HÀM VẼ GIAO DIỆN CHÍNH (Nơi phép thuật Graphics2D xảy ra)
        @Override 
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); 
            
            // Ép kiểu sang Graphics2D và bật khử răng cưa (Anti-aliasing) để nét vẽ mịn màng, không bị rỗ
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 1. LẤY THÔNG SỐ KÍCH THƯỚC KHUNG VẼ
            int w = getWidth(); 
            int h = getHeight() - 30; // Trừ hao không gian cho cái Tiêu đề ở trên
            int padding = 30; // Lề trái/phải để biểu đồ không dính sát viền
            int startY = 20;  // Tọa độ Y bắt đầu vẽ (đẩy xuống một chút)
            
            // 2. VẼ CÁI NỀN TRẮNG BO GÓC BÊN DƯỚI BIỂU ĐỒ
            g2.setColor(Color.WHITE); 
            g2.fillRoundRect(0, 15, w, getHeight()-15, 16, 16); 
            g2.setColor(BORDER_COLOR); 
            g2.drawRoundRect(0, 15, w-1, getHeight()-16, 16, 16);

            // 🛑 BẮT LỖI UX: Nếu đang loading thì không vẽ đường
            if (isLoading) return; 

            // 3. TÌM GIÁ TRỊ LỚN NHẤT ĐỂ CHIA TỶ LỆ (SCALE)
            int maxData = Arrays.stream(dataMonths).max().orElse(1); 
            if (maxData == 0) maxData = 1; // 🐛 Lỗ hổng: Tránh lỗi chia cho 0 nếu chưa có data nào!

            // 4. SETUP NÉT VẼ CHO ĐƯỜNG LINE BIỂU ĐỒ
            g2.setColor(TienIchGiaoDien.MAU_CHINH); // Lấy màu chủ đạo (Xanh dương)
            // Stroke 3px, bo tròn các đoạn nối và đầu nối (CAP_ROUND, JOIN_ROUND) cho nét mềm mại
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            // 5. VẼ ĐƯỜNG ĐỒ THỊ (Line Chart) BẰNG PATH2D
            Path2D path = new Path2D.Float(); 
            for (int i = 0; i < 12; i++) { 
                // Tính tọa độ X: Chia đều chiều rộng cho 11 khoảng (từ tháng 1 đến tháng 12)
                int x = padding + i * (w - 2 * padding) / 11; 
                
                // Tính tọa độ Y: Càng nhiều khách (data cao) thì Y càng nhỏ (vì gốc tọa độ Y=0 ở trên cùng)
                int y = h - (dataMonths[i] * (h - 2 * padding) / maxData) + startY; 
                
                if (i == 0) {
                    path.moveTo(x, y); // Điểm đầu tiên (Tháng 1) thì nhấc bút đặt xuống
                } else {
                    path.lineTo(x, y); // Các điểm tiếp theo thì kéo một đường thẳng tới
                }
            } 
            g2.draw(path); // Chính thức vẽ cái đường nãy giờ vừa phác thảo ra màn hình!

            // 6. ĐỔ MÀU GRADIENT XUỐNG ĐÁY BIỂU ĐỒ (Tạo hiệu ứng ma mị, sang trọng)
            Path2D gradientPath = new Path2D.Float(path); // Copy lại cái đường line ở trên
            // Kéo 2 điểm xuống góc dưới cùng bên phải và trái để tạo thành một khối khép kín
            gradientPath.lineTo(padding + 11 * (w - 2 * padding) / 11, h + startY); 
            gradientPath.lineTo(padding, h + startY); 
            gradientPath.closePath(); // Đóng khối lại
            
            // Phối màu Gradient: Trên đậm (Alpha=80) mờ dần xuống dưới (Alpha=0)
            g2.setPaint(new GradientPaint(
                0, startY, new Color(37, 99, 235, 80), 
                0, h + startY, new Color(37, 99, 235, 0)
            )); 
            g2.fill(gradientPath); // Đổ màu vào cái khối vừa tạo
            
            g2.dispose(); // Dọn dẹp bút vẽ cho khỏi tốn RAM nha!
        }
    }

    private class ShadowPanel extends JPanel {
        private Color bgColor; private int shadowSize, yOffset;
        public ShadowPanel(Color bg, int s, int yOff) { this.bgColor = bg; this.shadowSize = s; this.yOffset = yOff; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 10)); g2.fillRect(0, yOffset, getWidth(), getHeight() - yOffset);
            g2.setColor(bgColor); g2.fillRect(0, 0, getWidth(), getHeight() - shadowSize); g2.dispose(); super.paintComponent(g);
        }
    }
    public static void main(String[] args) {
        
        // 🎨 BƯỚC 1: TRANG ĐIỂM CHO HỆ THỐNG (Look & Feel)
        try {
            // Cài đặt giao diện theo chuẩn của Hệ điều hành (Windows/macOS) nhìn cho "chanh sả"
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // 🐛 Vá lỗi UI: Xóa cái viền focus (nét đứt) bao quanh nút bấm cực kỳ "kém sang" của Java Swing cổ điển
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));
            
        } catch (Exception e) {
            System.out.println("⚠️ Không thể load System LookAndFeel, sẽ dùng giao diện mặc định!");
        }

        // 🛡️ BƯỚC 2: KHỞI CHẠY GIAO DIỆN TRONG LUỒNG AN TOÀN (Event Dispatch Thread)
        // Kỹ sư Front-end xịn là không bao giờ để GUI chạy ở main thread kẻo bị giật lag nha!
        SwingUtilities.invokeLater(() -> {
            
            // Tạo cửa sổ chính
            JFrame frame = new JFrame("🚀 Dashboard Khách Hàng - V6 (Smart Calendar)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Thiết lập kích thước chuẩn HD cho rộng rãi, thoáng mát
            frame.setSize(1366, 768); 
            
            // Căn giữa màn hình cho chuẩn form
            frame.setLocationRelativeTo(null); 
            
            // Thêm siêu phẩm Panel Lịch Pop-up 15 ngày của chúng ta vào giữa frame
            DanhSachKhUi dashboard = new DanhSachKhUi();
            frame.add(dashboard);
            
            // Bùm! Hiện lên nào! ✨
            frame.setVisible(true);
        });
    }
}