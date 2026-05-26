package GUI;

import Dao.TruyVanSieuTocDAO;
import Data.ChiTietLoHang;
import Data.SanPham;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * GIAO DIỆN TRUNG TÂM CẢNH BÁO KHO (ERP/WMS STYLE)
 * ĐÃ LIÊN KẾT DATABASE VÀ NỐI DÂY BỘ LỌC HOÀN CHỈNH
 */
public class CanhBaoKhoPanel extends JPanel {

    // ==========================================
    // 🎨 BẢNG MÀU ERP HIỆN ĐẠI
    // ==========================================
    private static final Color BG_MAIN = new Color(248, 250, 252);        
    private static final Color BG_CARD = Color.WHITE;
    private static final Color TEXT_MAIN = new Color(15, 23, 42);         
    private static final Color TEXT_SUB = new Color(100, 116, 139);       
    private static final Color BORDER_COLOR = new Color(226, 232, 240);   
    
    private static final Color NAVY_BTN = new Color(15, 23, 42);          
    private static final Color DANGER_FG = new Color(220, 38, 38);        
    private static final Color DANGER_BG = new Color(254, 226, 226);      
    private static final Color WARNING_FG = new Color(217, 119, 6);       
    private static final Color WARNING_BG = new Color(254, 243, 199);     
    private static final Color INFO_FG = new Color(37, 99, 235);          
    private static final Color INFO_BG = new Color(219, 234, 254);        

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);

    // ==========================================
    // COMPONENTS & STATE VARIABLES CẦN CẬP NHẬT ĐỘNG
    // ==========================================
    private JPanel pnlAlertList;
    private JLabel lblCountNghiemTrong, lblCountLuuY;
    private JLabel lblCountTatCa, lblCountDaHetHan, lblCountSapHetHan, lblCountTonThap;
    
    private List<AlertItem> currentAlerts = new ArrayList<>();
    
    // 🔥 BIẾN LƯU TRẠNG THÁI LỌC 
    private AlertType currentFilterType = null;         // null = Tất cả
    private AlertPriority currentFilterPriority = null; // null = Tất cả
    private List<JButton> lstPriorityChips = new ArrayList<>(); // Lưu list chip để đổi màu động

    public CanhBaoKhoPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        add(taoHeader(), BorderLayout.NORTH);
        
        JPanel pnlCenter = new JPanel(new GridBagLayout());
        pnlCenter.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0; 

        gbc.gridx = 0;
        gbc.weightx = 0.1; 
        gbc.insets = new Insets(0, 0, 0, 10); 
        pnlCenter.add(taoSidebarFilter(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.9; 
        gbc.insets = new Insets(0, 10, 0, 0); 
        pnlCenter.add(taoDanhSachCanhBao(), gbc);
        
        add(pnlCenter, BorderLayout.CENTER);

        // KÍCH HOẠT TẢI DỮ LIỆU TỪ DATABASE
        taiDuLieuThucTeTuKho();
    }

    // ==========================================
    // 🔥 LOGIC LIÊN KẾT DATABASE VÀ XỬ LÝ NGÀY THÁNG
    // ==========================================
    private void taiDuLieuThucTeTuKho() {
        pnlAlertList.removeAll();
        JLabel lblLoading = new JLabel("Đang phân tích dữ liệu kho tự động...", SwingConstants.CENTER);
        lblLoading.setFont(FONT_BOLD.deriveFont(16f));
        lblLoading.setForeground(INFO_FG);
        pnlAlertList.add(lblLoading);
        pnlAlertList.revalidate();
        pnlAlertList.repaint();

        SwingWorker<List<AlertItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AlertItem> doInBackground() throws Exception {
                List<AlertItem> list = new ArrayList<>();
                TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO duLieuSQL = TruyVanSieuTocDAO.getInstance().loadDuLieuKiemKeSieuToc();
                LocalDate today = LocalDate.now();

                for (SanPham sp : duLieuSQL.dsSanPham) {
                    List<ChiTietLoHang> dsLo = duLieuSQL.mapDanhSachLo.get(sp.getMaSP());
                    if (dsLo == null) continue;

                    for (ChiTietLoHang lo : dsLo) {
                        int tonKho = lo.getSoLuongTon();
                        if (tonKho <= 0) continue;

                        boolean daCanhBaoDate = false;

                        if (lo.getHSD() != null) {
                            long daysBetween = ChronoUnit.DAYS.between(today, lo.getHSD());

                            if (daysBetween < 0) {
                                long expiredDays = Math.abs(daysBetween);
                                list.add(new AlertItem(sp.getTenSP(), "Đã hết hạn " + expiredDays + " ngày", lo.getMaLoHang(), "Khu vực chung", AlertPriority.HIGH, AlertType.EXPIRED));
                                daCanhBaoDate = true;
                            } else if (daysBetween <= 30) {
                                AlertPriority pri = (daysBetween <= 7) ? AlertPriority.HIGH : AlertPriority.MEDIUM;
                                list.add(new AlertItem(sp.getTenSP(), "Còn " + daysBetween + " ngày hết hạn", lo.getMaLoHang(), "Khu vực chung", pri, AlertType.EXPIRING_SOON));
                                daCanhBaoDate = true;
                            }
                        }

                        if (!daCanhBaoDate && tonKho <= 15) {
                            list.add(new AlertItem(sp.getTenSP(), "Tồn kho thấp (" + tonKho + " " + sp.getDonViTinh() + ")", lo.getMaLoHang(), "Khu vực chung", AlertPriority.LOW, AlertType.LOW_STOCK));
                        }
                    }
                }
                return list;
            }

            @Override
            protected void done() {
                try {
                    currentAlerts = get();
                    capNhatGiaoDienVoiDuLieuMoi();
                } catch (Exception e) {
                    pnlAlertList.removeAll();
                    JLabel lblErr = new JLabel("Lỗi đồng bộ dữ liệu: " + e.getMessage(), SwingConstants.CENTER);
                    lblErr.setForeground(DANGER_FG);
                    pnlAlertList.add(lblErr);
                }
            }
        };
        worker.execute();
    }

    private void capNhatGiaoDienVoiDuLieuMoi() {
        int nghiemTrong = 0, luuY = 0;
        int daHetHan = 0, sapHetHan = 0, tonThap = 0;

        for (AlertItem a : currentAlerts) {
            if (a.priority == AlertPriority.HIGH) nghiemTrong++;
            else luuY++;

            if (a.type == AlertType.EXPIRED) daHetHan++;
            else if (a.type == AlertType.EXPIRING_SOON) sapHetHan++;
            else if (a.type == AlertType.LOW_STOCK) tonThap++;
        }

        lblCountNghiemTrong.setText("Nghiêm trọng: " + nghiemTrong);
        lblCountLuuY.setText("Cần lưu ý: " + luuY);
        
        lblCountTatCa.setText(String.valueOf(currentAlerts.size()));
        lblCountDaHetHan.setText(String.valueOf(daHetHan));
        lblCountSapHetHan.setText(String.valueOf(sapHetHan));
        lblCountTonThap.setText(String.valueOf(tonThap));

        // Bỏ việc gọi list trực tiếp ở đây, nhường cho hàm Lọc Data quyết định
        hienThiDanhSachDaLoc();
    }

    // ==========================================
    // ⚙️ THUẬT TOÁN LỌC DỮ LIỆU VÀ VẼ LẠI MÀN HÌNH
    // ==========================================
    private void hienThiDanhSachDaLoc() {
        pnlAlertList.removeAll();

        List<AlertItem> filteredList = new ArrayList<>();
        
        // Quét dữ liệu và áp dụng 2 điều kiện lọc
        for (AlertItem item : currentAlerts) {
            boolean matchType = (currentFilterType == null || item.type == currentFilterType);
            boolean matchPriority = (currentFilterPriority == null || item.priority == currentFilterPriority);

            if (matchType && matchPriority) {
                filteredList.add(item);
            }
        }

        // Vẽ UI sau khi lọc
        if (filteredList.isEmpty()) {
            pnlAlertList.add(Box.createVerticalStrut(50)); // Tạo khoảng trống đẩy text xuống xíu
            JLabel lblEmpty = new JLabel("Kho đang trống trải hoặc không có cảnh báo nào phù hợp với bộ lọc! 🎉", SwingConstants.CENTER);
            lblEmpty.setFont(FONT_BOLD.deriveFont(16f));
            lblEmpty.setForeground(TEXT_SUB);
            lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
            pnlAlertList.add(lblEmpty);
        } else {
            for (AlertItem item : filteredList) {
                pnlAlertList.add(new AlertCard(item));
                pnlAlertList.add(Box.createVerticalStrut(15));
            }
        }
        
        pnlAlertList.revalidate();
        pnlAlertList.repaint();
    }

    // ==========================================
    // UI BUILDERS
    // ==========================================
    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel pnlLeft = new JPanel(new GridLayout(2, 1, 0, 5));
        pnlLeft.setOpaque(false);
        JLabel lblTitle = new JLabel("Trung tâm Cảnh báo Kho");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(NAVY_BTN);
        
        JLabel lblSub = new JLabel("Giám sát và xử lý các sự cố tồn kho trong thời gian thực.");
        lblSub.setFont(FONT_SUBTITLE);
        lblSub.setForeground(TEXT_SUB);
        
        pnlLeft.add(lblTitle);
        pnlLeft.add(lblSub);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        pnlRight.setOpaque(false);
        
        lblCountNghiemTrong = new JLabel("Nghiêm trọng: 0");
        lblCountNghiemTrong.setFont(FONT_BOLD);
        lblCountNghiemTrong.setForeground(DANGER_FG);

        lblCountLuuY = new JLabel("Cần lưu ý: 0");
        lblCountLuuY.setFont(FONT_BOLD);
        lblCountLuuY.setForeground(WARNING_FG);

        pnlRight.add(taoBadgeThongKe(lblCountNghiemTrong, DANGER_BG, "🔴"));
        pnlRight.add(taoBadgeThongKe(lblCountLuuY, WARNING_BG, "🟡"));

        ModernButton btnReload = new ModernButton("Tải lại", NAVY_BTN, Color.WHITE);
        btnReload.addActionListener(e -> taiDuLieuThucTeTuKho());
        pnlRight.add(btnReload);

        header.add(pnlLeft, BorderLayout.WEST);
        header.add(pnlRight, BorderLayout.EAST);
        return header;
    }

    private JPanel taoBadgeThongKe(JLabel lblText, Color bg, String icon) {
        RoundedPanel badge = new RoundedPanel(20, bg);
        badge.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));
        badge.add(new JLabel(icon));
        badge.add(lblText);
        return badge;
    }

    private JPanel taoSidebarFilter() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 12));
        sidebar.setOpaque(false);

        RoundedPanel box1 = new RoundedPanel(12, BG_CARD);
        box1.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(15, 15, 15, 15)
        ));
        box1.setLayout(new BoxLayout(box1, BoxLayout.Y_AXIS));

        JLabel lblTitle1 = new JLabel("≡  LOẠI CẢNH BÁO");
        lblTitle1.setFont(FONT_BOLD.deriveFont(12f));
        lblTitle1.setForeground(NAVY_BTN);
        lblTitle1.setAlignmentX(Component.LEFT_ALIGNMENT);
        box1.add(lblTitle1);
        box1.add(Box.createVerticalStrut(12));

        lblCountTatCa     = taoLabelBadge("0", new Color(71, 85, 105),  new Color(241, 245, 249));
        lblCountDaHetHan  = taoLabelBadge("0", new Color(220, 38, 38),  new Color(254, 226, 226));
        lblCountSapHetHan = taoLabelBadge("0", new Color(217, 119, 6),  new Color(254, 243, 199));
        lblCountTonThap   = taoLabelBadge("0", new Color(37, 99, 235),  new Color(219, 234, 254));

        ButtonGroup groupFilter = new ButtonGroup();
        
        // Nối dây: Truyền AlertType tương ứng vào để RadioButton tự bắt sự kiện
        box1.add(taoFilterRow("Tất cả",       lblCountTatCa,     true,  groupFilter, null));
        box1.add(Box.createVerticalStrut(6));
        box1.add(taoFilterRow("Đã hết hạn",   lblCountDaHetHan,  false, groupFilter, AlertType.EXPIRED));
        box1.add(Box.createVerticalStrut(6));
        box1.add(taoFilterRow("Sắp hết hạn",  lblCountSapHetHan, false, groupFilter, AlertType.EXPIRING_SOON));
        box1.add(Box.createVerticalStrut(6));
        box1.add(taoFilterRow("Tồn kho thấp", lblCountTonThap,   false, groupFilter, AlertType.LOW_STOCK));

        RoundedPanel box2 = new RoundedPanel(12, BG_CARD);
        box2.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(15, 15, 15, 15)
        ));
        box2.setLayout(new BoxLayout(box2, BoxLayout.Y_AXIS));

        JLabel lblTitle2 = new JLabel("!  ĐỘ ƯU TIÊN");
        lblTitle2.setFont(FONT_BOLD.deriveFont(12f));
        lblTitle2.setForeground(NAVY_BTN);
        lblTitle2.setAlignmentX(Component.LEFT_ALIGNMENT);
        box2.add(lblTitle2);
        box2.add(Box.createVerticalStrut(10));

        JPanel pnlChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlChips.setOpaque(false);
        pnlChips.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        pnlChips.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Reset list để tránh nạp lại rác nếu hàm này vô tình được gọi 2 lần
        lstPriorityChips.clear();
        pnlChips.add(taoPriorityChip("Cao",        AlertPriority.HIGH));
        pnlChips.add(taoPriorityChip("Trung bình", AlertPriority.MEDIUM));
        pnlChips.add(taoPriorityChip("Thấp",       AlertPriority.LOW));
        
        capNhatGiaoDienChips(); // Vẽ style lần đầu tiên cho chip
        
        box2.add(pnlChips);

        JPanel pnlBox1Wrapper = new JPanel(new BorderLayout());
        pnlBox1Wrapper.setOpaque(false);
        pnlBox1Wrapper.add(box1, BorderLayout.CENTER);

        JPanel pnlBox2Wrapper = new JPanel(new BorderLayout());
        pnlBox2Wrapper.setOpaque(false);
        pnlBox2Wrapper.setBorder(new EmptyBorder(12, 0, 0, 0)); 
        pnlBox2Wrapper.add(box2, BorderLayout.NORTH); 

        sidebar.add(pnlBox1Wrapper, BorderLayout.NORTH);
        sidebar.add(pnlBox2Wrapper, BorderLayout.CENTER);

        return sidebar;
    }

    private JLabel taoLabelBadge(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        lbl.setFont(FONT_BOLD.deriveFont(11f));
        lbl.setForeground(fg);
        lbl.setBackground(bg);
        lbl.setOpaque(false);
        lbl.setBorder(new EmptyBorder(2, 7, 2, 7)); 
        return lbl;
    }

    // ⚙️ TRUYỀN THÊM TYPEFILTER ĐỂ XỬ LÝ LỌC
    private JPanel taoFilterRow(String text, JLabel lblCount, boolean isSelected, ButtonGroup group, AlertType typeFilter) {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); 
        
        JRadioButton rad = new JRadioButton(text, isSelected);
        rad.setFont(FONT_REGULAR.deriveFont(13f));
        rad.setForeground(TEXT_MAIN);
        rad.setOpaque(false);
        rad.setFocusPainted(false);
        rad.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rad.setIconTextGap(8);
        rad.setIcon(new CustomRadioIcon(false));
        rad.setSelectedIcon(new CustomRadioIcon(true));
        
        // ⚙️ GẮN SỰ KIỆN CLICK CHO RADIO BẬT LỌC VÀ CHẠY HÀM HIỂN THỊ
        rad.addActionListener(e -> {
            if(rad.isSelected()) {
                currentFilterType = typeFilter;
                hienThiDanhSachDaLoc();
            }
        });
        
        group.add(rad);
        p.add(rad, BorderLayout.CENTER);
        
        JPanel pnlBadgeWrapper = new JPanel(new GridBagLayout());
        pnlBadgeWrapper.setOpaque(false);
        pnlBadgeWrapper.add(lblCount);
        p.add(pnlBadgeWrapper, BorderLayout.EAST);
        return p;
    }

    // ⚙️ NÚT CHIP LÀM MỚI THEO LOGIC ON/OFF
    private JButton taoPriorityChip(String text, AlertPriority prio) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); 
                FontMetrics fm = g2.getFontMetrics();
                Rectangle r = fm.getStringBounds(getText(), g2).getBounds();
                g2.setColor(getForeground());
                g2.drawString(getText(), (getWidth() - r.width) / 2, (getHeight() - r.height) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setFont(FONT_REGULAR.deriveFont(12f));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Lưu giá trị ưu tiên ẩn bên dưới nút
        btn.putClientProperty("priority", prio);

        // ⚙️ LOGIC LỌC: Bấm 1 lần thì chọn, Bấm phát nữa vào chính nó thì hủy chọn (trả về hiển thị toàn bộ)
        btn.addActionListener(e -> {
            if (currentFilterPriority == prio) {
                currentFilterPriority = null; // Bật tắt (Toggle off)
            } else {
                currentFilterPriority = prio; // Kích hoạt (Toggle on)
            }
            capNhatGiaoDienChips();
            hienThiDanhSachDaLoc();
        });

        lstPriorityChips.add(btn);
        return btn;
    }

    // ⚙️ HÀM CẬP NHẬT MÀU SẮC CHIP ĐỒNG BỘ VỚI BIẾN currentFilterPriority
    private void capNhatGiaoDienChips() {
        for (JButton btn : lstPriorityChips) {
            AlertPriority p = (AlertPriority) btn.getClientProperty("priority");
            if (p == currentFilterPriority) {
                // Style nút đang Active
                btn.setBackground(NAVY_BTN);
                btn.setForeground(Color.WHITE);
                btn.setBorder(new EmptyBorder(5, 13, 5, 13));
            } else {
                // Style nút Inactive
                btn.setBackground(Color.WHITE);
                btn.setForeground(TEXT_MAIN);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(20, BORDER_COLOR),
                    new EmptyBorder(4, 12, 4, 12)
                ));
            }
            btn.repaint();
        }
    }

    private JScrollPane taoDanhSachCanhBao() {
        pnlAlertList = new JPanel();
        pnlAlertList.setLayout(new BoxLayout(pnlAlertList, BoxLayout.Y_AXIS));
        pnlAlertList.setBackground(BG_MAIN);
        
        JScrollPane scroll = new JScrollPane(pnlAlertList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_MAIN);
        scroll.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    // ==========================================
    // COMPONENT: CARD CẢNH BÁO
    // ==========================================
    class AlertCard extends RoundedPanel {
        public AlertCard(AlertItem data) {
            super(15, BG_CARD);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

            JPanel pnlPriority = new JPanel();
            pnlPriority.setPreferredSize(new Dimension(6, 110));
            switch (data.priority) {
                case HIGH: pnlPriority.setBackground(DANGER_FG); break;
                case MEDIUM: pnlPriority.setBackground(WARNING_FG); break;
                case LOW: pnlPriority.setBackground(INFO_FG); break;
            }

            JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
            pnlLeft.setOpaque(false);
            
            RoundedPanel pnlImage = new RoundedPanel(10, BORDER_COLOR);
            pnlImage.setPreferredSize(new Dimension(70, 70));
            pnlImage.setLayout(new BorderLayout());
            JLabel lblImgText = new JLabel("SP", SwingConstants.CENTER);
            lblImgText.setForeground(TEXT_SUB);
            pnlImage.add(lblImgText);
            
            pnlLeft.add(pnlPriority);
            pnlLeft.add(pnlImage);

            JPanel pnlCenter = new JPanel(new GridLayout(3, 1, 0, 5));
            pnlCenter.setOpaque(false);
            pnlCenter.setBorder(new EmptyBorder(15, 10, 15, 10));

            JLabel lblName = new JLabel(data.productName);
            lblName.setFont(FONT_TITLE.deriveFont(Font.BOLD, 18f));
            lblName.setForeground(NAVY_BTN);
            
            Color bgBadge = BG_MAIN, fgBadge = TEXT_SUB;
            String iconBadge = "";
            switch (data.priority) {
                case HIGH: bgBadge = DANGER_BG; fgBadge = DANGER_FG; iconBadge = "🔴"; break;
                case MEDIUM: bgBadge = WARNING_BG; fgBadge = WARNING_FG; iconBadge = "🟡"; break;
                case LOW: bgBadge = INFO_BG; fgBadge = INFO_FG; iconBadge = "📉"; break;
            }
            JLabel lblStatus = new JLabel(iconBadge + " " + data.statusText);
            lblStatus.setFont(FONT_BOLD.deriveFont(13f));
            lblStatus.setForeground(fgBadge);

            JLabel lblInfo = new JLabel("📦 Lô " + data.lotNumber + "   |   📍 " + data.location);
            lblInfo.setFont(FONT_REGULAR);
            lblInfo.setForeground(TEXT_SUB);

            pnlCenter.add(lblName);
            pnlCenter.add(lblStatus);
            pnlCenter.add(lblInfo);

            JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 30));
            pnlRight.setOpaque(false);
            
            String mainBtnText = (data.type == AlertType.LOW_STOCK) ? "Nhập hàng" : "Xử lý kho";
            ModernButton btnAction = new ModernButton(mainBtnText, NAVY_BTN, Color.WHITE);
            ModernButton btnView = new ModernButton("Chi tiết", Color.WHITE, TEXT_MAIN);
            btnView.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

            pnlRight.add(btnAction);
            pnlRight.add(btnView);

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(new Color(248, 250, 252)); }
                public void mouseExited(MouseEvent e) { setBackground(BG_CARD); }
            });

            add(pnlLeft, BorderLayout.WEST);
            add(pnlCenter, BorderLayout.CENTER);
            add(pnlRight, BorderLayout.EAST);
        }
    }

    // ==========================================
    // UTILS & CLASSES
    // ==========================================
    public enum AlertPriority { HIGH, MEDIUM, LOW }
    public enum AlertType { EXPIRED, EXPIRING_SOON, LOW_STOCK }

    public static class AlertItem {
        public String productName;
        public String statusText;
        public String lotNumber;
        public String location;
        public AlertPriority priority;
        public AlertType type;

        public AlertItem(String p, String s, String l, String loc, AlertPriority pri, AlertType t) {
            this.productName = p; this.statusText = s; this.lotNumber = l; 
            this.location = loc; this.priority = pri; this.type = t;
        }
    }

    class RoundedPanel extends JPanel {
        private int radius;
        private Color bgColor;
        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius; this.bgColor = bgColor; setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class ModernButton extends JButton {
        private Color bg, fg;
        public ModernButton(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg; this.fg = fg;
            setFont(FONT_BOLD); setForeground(fg); setBackground(bg);
            setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(100, 36));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(bg.darker()); }
                public void mouseExited(MouseEvent e) { setBackground(bg); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            FontMetrics fm = g2.getFontMetrics();
            Rectangle r = fm.getStringBounds(getText(), g2).getBounds();
            g2.setColor(getForeground());
            g2.drawString(getText(), (getWidth() - r.width) / 2, (getHeight() - r.height) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
    class RoundedBorder implements javax.swing.border.Border {
        private int radius;
        private Color color;
        public RoundedBorder(int radius, Color color) {
            this.radius = radius; this.color = color;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(5, 13, 5, 13); }
        @Override public boolean isBorderOpaque() { return false; }
    }
    class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(203, 213, 225));
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
            g2.dispose();
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}
        @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
    }
    class CustomRadioIcon implements Icon {
        private boolean isSelected;
        public CustomRadioIcon(boolean isSelected) { this.isSelected = isSelected; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = getIconWidth();
            
            if (isSelected) {
                g2.setColor(NAVY_BTN); 
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.fillOval(x + 5, y + 5, size - 10, size - 10);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillOval(x, y, size, size);
                g2.setColor(new Color(203, 213, 225));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);
            }
            g2.dispose();
        }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dashboard Cảnh Báo Kho Tồn - Realtime Data");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 750);
            frame.setLocationRelativeTo(null);
            frame.add(new CanhBaoKhoPanel());
            frame.setVisible(true);
        });
    }
}