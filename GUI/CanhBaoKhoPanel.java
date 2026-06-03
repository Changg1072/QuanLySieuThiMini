package GUI;

import Dao.TruyVanSieuTocDAO;
import Data.ChiTietLoHang;
import Data.SanPham;
import GUI.CanhBaoKhoPanel.AlertItem;
import GUI.CanhBaoKhoPanel.ModernActionPopup;
import GUI.CanhBaoKhoPanel.ModernButton;
import GUI.CanhBaoKhoPanel.WarehouseAlertActionHandler;
import java.awt.event.ActionListener;
import Data.PhieuTieuHuy;
import Data.ChiTietPhieuHuy;
import Logic.PhieuTieuHuyLogic;
import Logic.ChiTietPhieuHuyLogic;
import java.math.BigDecimal;

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
    private static final Color PURPLE_FG = new Color(147, 51, 234);       // Tím đậm (Lệch kho)
    private static final Color PURPLE_BG = new Color(243, 232, 255);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);

    // ==========================================
    // COMPONENTS & STATE VARIABLES CẦN CẬP NHẬT ĐỘNG
    // ==========================================
    private JPanel pnlAlertList;
    private JLabel lblCountNghiemTrong, lblCountLuuY;
    private JLabel lblCountTatCa, lblCountDaHetHan, lblCountSapHetHan, lblCountTonThap, lblCountLechKho;
    private List<AlertItem> currentAlerts = new ArrayList<>();
    
    // 🔥 BIẾN LƯU TRẠNG THÁI LỌC 
    private AlertType currentFilterType = null;         // null = Tất cả
    private AlertPriority currentFilterPriority = null; // null = Tất cả
    private List<JButton> lstPriorityChips = new ArrayList<>(); // Lưu list chip để đổi màu động
    public interface NavigationCallback {
        void navigateTo(String moduleName, String maSP);
    }
    private NavigationCallback navigationCallback;
    public CanhBaoKhoPanel(NavigationCallback callback) {
        this();
        this.navigationCallback = callback;
    }
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
    // 🔥 LOGIC LIÊN KẾT DATABASE VÀ XỬ LÝ NGÀY THÁNG + KIỂM KÊ
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
                java.util.Set<String> dsDangGiamGia = TruyVanSieuTocDAO.getInstance()
                    .getTapHopSanPhamDangGiamGia();
                LocalDate today = LocalDate.now();

                // =====================================================================
                // 🆕 BƯỚC 0: RÀ SOÁT & NẠP DANH SÁCH LÔ ĐÃ KIỂM TRA HOÀN TẤT
                // Logic: Lô có LyDo bắt đầu bằng "Đã kiểm tra [MaLoHang]" → đã xử lý xong
                // → Loại khỏi tất cả cảnh báo (cả lệch kho lần 1 và lần 2)
                // =====================================================================
                java.util.Set<String> dsLoaDaKiemTra = new java.util.HashSet<>();
                String sqlDaKiem = "SELECT DISTINCT MaLoHang FROM KiemKeKho " +
                                   "WHERE LyDo LIKE N'Đã kiểm tra %'";
                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                     java.sql.Statement stKiem = con.createStatement();
                     java.sql.ResultSet rsKiem = stKiem.executeQuery(sqlDaKiem)) {
                    while (rsKiem.next()) {
                        dsLoaDaKiemTra.add(rsKiem.getString("MaLoHang"));
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi lấy danh sách lô đã kiểm tra: " + e.getMessage());
                }

                // 1. QUÉT HẠN SỬ DỤNG & TỒN THẤP
                for (SanPham sp : duLieuSQL.dsSanPham) {
                    List<ChiTietLoHang> dsLo = duLieuSQL.mapDanhSachLo.get(sp.getMaSP());
                    if (dsLo == null) continue;

                    for (ChiTietLoHang lo : dsLo) {
                        int tonKho = lo.getSoLuongTon();
                        if (tonKho <= 0) continue;

                        // 🆕 BỎ QUA LÔ ĐÃ ĐƯỢC KIỂM TRA HOÀN TẤT
                        if (dsLoaDaKiemTra.contains(lo.getMaLoHang())) continue;

                        boolean daCanhBaoDate = false;

                        if (lo.getHSD() != null) {
                            long daysBetween = ChronoUnit.DAYS.between(today, lo.getHSD());

                            if (daysBetween < 0) {
                                long expiredDays = Math.abs(daysBetween);
                                list.add(new AlertItem(
                                    sp.getTenSP(), "Đã hết hạn " + expiredDays + " ngày",
                                    lo.getMaLoHang(), "Kho chính",
                                    AlertPriority.HIGH, AlertType.EXPIRED,
                                    sp.getMaSP(), tonKho, lo.getGiaNhap(),
                                    sp.getLinkHinhAnh() // 🟢 TRUYỀN ẢNH VÀO ĐÂY
                                ));
                                daCanhBaoDate = true;

                            } else if (daysBetween <= 30) {
                                if (dsDangGiamGia.contains(sp.getMaSP())) {
                                    daCanhBaoDate = true;
                                    continue;
                                }

                                AlertPriority pri = (daysBetween <= 7) ? AlertPriority.HIGH : AlertPriority.MEDIUM;
                                list.add(new AlertItem(
                                    sp.getTenSP(), "Còn " + daysBetween + " ngày hết hạn",
                                    lo.getMaLoHang(), "Kho chính",
                                    pri, AlertType.EXPIRING_SOON,
                                    sp.getMaSP(), tonKho, lo.getGiaNhap(),
                                    sp.getLinkHinhAnh() // 🟢 TRUYỀN ẢNH VÀO ĐÂY
                                ));
                                daCanhBaoDate = true;
                            }
                        }

                        if (!daCanhBaoDate && tonKho <= 15) {
                            list.add(new AlertItem(
                                sp.getTenSP(), "Tồn kho thấp (" + tonKho + " " + sp.getDonViTinh() + ")",
                                lo.getMaLoHang(), "Kho chính",
                                AlertPriority.LOW, AlertType.LOW_STOCK,
                                sp.getMaSP(), tonKho, lo.getGiaNhap(),
                                sp.getLinkHinhAnh() // 🟢 TRUYỀN ẢNH VÀO ĐÂY
                            ));
                        }
                    }
                }

                // 2. ĐỘNG CƠ QUÉT LỆCH KHO TỪ BẢNG KiemKeKho
               String sqlLechKho = "SELECT k.MaKiemKe, k.MaSP, s.TenSP, s.LinkHinhAnh, k.MaLoHang, k.SoLuongHeThong, k.SoLuongThucTe, k.LyDo " +
                                    "FROM KiemKeKho k JOIN SanPham s ON k.MaSP = s.MaSP " +
                                    "WHERE k.SoLuongHeThong <> k.SoLuongThucTe";

                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                     java.sql.Statement st = con.createStatement();
                     java.sql.ResultSet rs = st.executeQuery(sqlLechKho)) {

                    while (rs.next()) {
                        // 🛠 THAY ĐỔI 2: Lấy thêm biến MaKiemKe từ Database
                        String maKiemKe = rs.getString("MaKiemKe"); 
                        
                        String tenSP = rs.getString("TenSP");
                        String maLo  = rs.getString("MaLoHang");
                        int slHT     = rs.getInt("SoLuongHeThong");
                        int slTT     = rs.getInt("SoLuongThucTe");
                        String hinhAnh = rs.getString("LinkHinhAnh");
                        String lyDo  = rs.getString("LyDo");

                        // CHẶN: Đã bù trừ "Huề kho" → bỏ qua
                        if (lyDo != null && lyDo.contains("Huề kho")) continue;

                        // 🆕 CHẶN: Lô đã được kiểm tra hoàn tất → bỏ qua
                        // 🛠 THAY ĐỔI 3: Nhận dạng qua LyDo = "Đã kiểm tra " + Mã Kiểm Kê (thay vì mã lô)
                        if (lyDo != null && lyDo.startsWith("Đã kiểm tra " + maKiemKe)) continue;

                        // Dự phòng thêm: nếu mã lô nằm trong Set đã thu thập ở Bước 0 → bỏ qua
                        if (dsLoaDaKiemTra.contains(maLo)) continue;

                        int lech = slTT - slHT;
                        String textCanhBao = (lech > 0 ? "Phát hiện dư: +" : "Phát hiện thiếu: ") + lech + " SP";
                        if (lyDo != null && !lyDo.trim().isEmpty()) {
                            textCanhBao += " (" + lyDo + ")";
                        }

                        list.add(new AlertItem(
                            tenSP, textCanhBao, maLo, "Kho chờ xử lý",
                            AlertPriority.HIGH, AlertType.LECH_KHO,
                            rs.getString("MaSP"), slHT, BigDecimal.ZERO, 
                            hinhAnh 
                        ));
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi quét lệch kho: " + e.getMessage());
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
        int daHetHan = 0, sapHetHan = 0, tonThap = 0, lechKho = 0; // Thêm lechKho

        for (AlertItem a : currentAlerts) {
            if (a.priority == AlertPriority.HIGH) nghiemTrong++;
            else luuY++;

            if (a.type == AlertType.EXPIRED) daHetHan++;
            else if (a.type == AlertType.EXPIRING_SOON) sapHetHan++;
            else if (a.type == AlertType.LOW_STOCK) tonThap++;
            else if (a.type == AlertType.LECH_KHO) lechKho++; // 🎯 Đếm lệch kho
        }

        lblCountNghiemTrong.setText("Nghiêm trọng: " + nghiemTrong);
        lblCountLuuY.setText("Cần lưu ý: " + luuY);
        
        lblCountTatCa.setText(String.valueOf(currentAlerts.size()));
        lblCountDaHetHan.setText(String.valueOf(daHetHan));
        lblCountSapHetHan.setText(String.valueOf(sapHetHan));
        lblCountTonThap.setText(String.valueOf(tonThap));
        
        // 🎯 Gắn số lên Badge Lệch Kho
        if (lblCountLechKho != null) {
            lblCountLechKho.setText(String.valueOf(lechKho));
        }

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
        lblCountLechKho   = taoLabelBadge("0", PURPLE_FG, PURPLE_BG); // 🎯 Thêm Badge Màu Tím

        ButtonGroup groupFilter = new ButtonGroup();
        
        box1.add(taoFilterRow("Tất cả",       lblCountTatCa,     true,  groupFilter, null));
        box1.add(Box.createVerticalStrut(6));
        box1.add(taoFilterRow("Đã hết hạn",   lblCountDaHetHan,  false, groupFilter, AlertType.EXPIRED));
        box1.add(Box.createVerticalStrut(6));
        box1.add(taoFilterRow("Sắp hết hạn",  lblCountSapHetHan, false, groupFilter, AlertType.EXPIRING_SOON));
        box1.add(Box.createVerticalStrut(6));
        box1.add(taoFilterRow("Tồn kho thấp", lblCountTonThap,   false, groupFilter, AlertType.LOW_STOCK));
        box1.add(Box.createVerticalStrut(6));
        box1.add(taoFilterRow("Lệch kho",     lblCountLechKho,   false, groupFilter, AlertType.LECH_KHO)); // 🎯 Thêm Radio Button Lệch Kho

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
        
        lstPriorityChips.clear();
        pnlChips.add(taoPriorityChip("Cao",        AlertPriority.HIGH));
        pnlChips.add(taoPriorityChip("Trung bình", AlertPriority.MEDIUM));
        pnlChips.add(taoPriorityChip("Thấp",       AlertPriority.LOW));
        capNhatGiaoDienChips(); 
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
    // UTILS & CLASSES
    // ==========================================
    public enum AlertPriority { HIGH, MEDIUM, LOW }
    public enum AlertType { EXPIRED, EXPIRING_SOON, LOW_STOCK, LECH_KHO }

    public static class AlertItem {
        public String productName;
        public String statusText;
        public String lotNumber;
        public String location;
        public AlertPriority priority;
        public AlertType type;
        
        // Dữ liệu ngầm để tiêu hủy trực tiếp
        public String maSP;
        public int soLuongTon;
        public BigDecimal giaNhap;
        public String hinhAnh;

        public AlertItem(String p, String s, String l, String loc, AlertPriority pri, AlertType t, String maSP, int soLuongTon, BigDecimal giaNhap, String hinhAnh) {
            this.productName = p; this.statusText = s; this.lotNumber = l; 
            this.location = loc; this.priority = pri; this.type = t;
            this.maSP = maSP; this.soLuongTon = soLuongTon; this.giaNhap = giaNhap;
            this.hinhAnh = hinhAnh; 
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
    // ==========================================
    // 🚦 WORKFLOW HANDLER (ERP/WMS STYLE)
    // ==========================================
    public static class WarehouseAlertActionHandler {
        public static void routeAction(AlertType type, AlertItem data, Component parent) {
            
            switch (type) {
                case EXPIRED:
                    // 🔥 BẢNG XÁC NHẬN CUSTOM: NỀN TRẮNG, BO GÓC MỊN THEO LAYOUT MỚI
                    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), Dialog.ModalityType.APPLICATION_MODAL);
                    dialog.setUndecorated(true);
                    dialog.setBackground(new Color(0, 0, 0, 0)); // Trong suốt để vẽ bo góc

                    // Panel chính tự vẽ nền bo góc
                    JPanel panel = new JPanel(new BorderLayout(0, 25)) {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            
                            // Vẽ bóng mờ (Shadow) nhẹ
                            g2.setColor(new Color(0, 0, 0, 15));
                            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);
                            
                            // Nền trắng chính
                            g2.setColor(Color.WHITE);
                            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 16, 16);
                            
                            // Viền xám mỏng
                            g2.setColor(new Color(226, 232, 240));
                            g2.setStroke(new BasicStroke(1.2f));
                            g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 16, 16);
                            
                            g2.dispose();
                        }
                    };
                    panel.setOpaque(false);
                    panel.setBorder(new EmptyBorder(30, 35, 25, 35));

                    // Dùng HTML để format Text giống hệt ảnh mẫu
                    String htmlText = "<html><div style='text-align: center; width: 320px;'>"
                        + "<span style='font-family: Segoe UI; font-size: 15px; font-weight: bold; color: #0F172A;'>"
                        + "Bạn muốn <span style='color: #DC2626;'>tiêu hủy trực tiếp (SQL)</span><br>"
                        + "toàn bộ " + data.soLuongTon + " sản phẩm của Lô " + data.lotNumber + "?</span><br><br>"
                        + "<span style='font-family: Segoe UI; font-size: 12px; font-style: italic; color: #64748B;'>"
                        + "(Ấn Xác nhận để xóa DB ngay, ấn Hủy bỏ để đưa vào danh sách chờ)</span>"
                        + "</div></html>";
                        
                    JLabel lblMsg = new JLabel(htmlText, SwingConstants.CENTER);

                    JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
                    pnlButtons.setOpaque(false);

                    // ♻️ NÚT HỦY BỎ (Style Viền xám nhạt, nền trắng, chữ xám đậm)
                    JButton btnNo = new JButton("Hủy bỏ") {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(getModel().isRollover() ? new Color(248, 250, 252) : Color.WHITE);
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                            
                            g2.setColor(new Color(203, 213, 225)); // Viền
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                            g2.dispose();
                            super.paintComponent(g);
                        }
                    };
                    btnNo.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    btnNo.setForeground(new Color(71, 85, 105));
                    btnNo.setPreferredSize(new Dimension(120, 38));
                    btnNo.setContentAreaFilled(false);
                    btnNo.setFocusPainted(false);
                    btnNo.setBorderPainted(false);
                    btnNo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    
                    btnNo.addActionListener(e -> {
                        dialog.dispose();
                        if (parent instanceof AlertCard) {
                            ((AlertCard) parent).markAsPending("CHỜ TIÊU HỦY", "Đã lưu vào form");
                        }
                        Container p = parent.getParent();
                        while (p != null && !(p instanceof CanhBaoKhoPanel)) { p = p.getParent(); }
                        if (p instanceof CanhBaoKhoPanel) {
                            CanhBaoKhoPanel panelGoc = (CanhBaoKhoPanel) p;
                            if (panelGoc.navigationCallback != null) {
                                panelGoc.navigationCallback.navigateTo("TieuHuyNgam", data.lotNumber);
                            }
                        }
                    });

                    // 🛑 NÚT XÁC NHẬN (Style nền Xanh dương, chữ trắng)
                    JButton btnYes = new JButton("Xác nhận") {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(getModel().isRollover() ? new Color(29, 78, 216) : new Color(37, 99, 235));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                            g2.dispose();
                            super.paintComponent(g);
                        }
                    };
                    btnYes.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    btnYes.setForeground(Color.WHITE);
                    btnYes.setPreferredSize(new Dimension(120, 38));
                    btnYes.setContentAreaFilled(false);
                    btnYes.setFocusPainted(false);
                    btnYes.setBorderPainted(false);
                    btnYes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    
                    btnYes.addActionListener(e -> {
                        dialog.dispose();
                        if (parent instanceof AlertCard) ((AlertCard) parent).markAsPending();
                        
                        SwingWorker<Void, Void> worker = new SwingWorker<>() {
                            @Override protected Void doInBackground() throws Exception {
                                BigDecimal giaTriHuy = data.giaNhap.multiply(new BigDecimal(data.soLuongTon));
                                Data.PhieuTieuHuy phieu = new Data.PhieuTieuHuy();
                                phieu.setMaNV("NV001"); 
                                phieu.setTongSoLuong(data.soLuongTon);
                                phieu.setTongGiaTriHuy(giaTriHuy);
                                phieu.setLyDoHuy("Hàng hết hạn");
                                Logic.PhieuTieuHuyLogic.getInstance().taoPhieuTieuHuy(phieu);
                                
                                Data.ChiTietPhieuHuy ct = new Data.ChiTietPhieuHuy();
                                ct.setMaPhieuHuy(phieu.getMaPhieuHuy());
                                ct.setMaLoHang(data.lotNumber);
                                ct.setMaSP(data.maSP);
                                ct.setSoLuongHuy(data.soLuongTon);
                                ct.setGiaTriHuy(giaTriHuy);
                                ct.setLyDoChiTiet("Hàng hết hạn");
                                Logic.ChiTietPhieuHuyLogic.getInstance().themChiTietPhieuHuy(ct);
                                
                                Logic.PhieuTieuHuyLogic.getInstance().hoanTatPhieuTieuHuy(phieu.getMaPhieuHuy());
                                return null;
                            }
                            @Override protected void done() {
                                try {
                                    get();
                                    GUI.HoTro.TienIchGiaoDien.hienThiThongBao(parent, "Đã tiêu hủy thành công Lô <b>" + data.lotNumber + "</b>!", "SUCCESS");
                                    Container p = parent.getParent();
                                    while (p != null && !(p instanceof CanhBaoKhoPanel)) p = p.getParent();
                                    if (p instanceof CanhBaoKhoPanel) ((CanhBaoKhoPanel) p).taiDuLieuThucTeTuKho();
                                } catch (Exception ex) {
                                    GUI.HoTro.TienIchGiaoDien.hienThiThongBao(parent, "Lỗi tiêu hủy: " + ex.getMessage(), "ERROR");
                                }
                            }
                        };
                        worker.execute();
                    });

                    pnlButtons.add(btnNo);
                    pnlButtons.add(btnYes);

                    panel.add(lblMsg, BorderLayout.CENTER);
                    panel.add(pnlButtons, BorderLayout.SOUTH);

                    dialog.setContentPane(panel);
                    dialog.pack();
                    dialog.setLocationRelativeTo(parent);
                    dialog.setVisible(true);
                    break;
                
                case LOW_STOCK:
                    System.out.println("➡️ Action: Mở Module Nhập Hàng (QuanLyNhapHangModule)");
                    break;
                case EXPIRING_SOON:
                    System.out.println("➡️ Action: Mở Module Giảm Giá (GiamGiaUI)");
                    break;
                case LECH_KHO:
                    System.out.println("➡️ Action: Mở Module Kiểm Kê (KiemKeGUI) -> Auto focus mã: " + data.lotNumber);
                    break;
            }
        }
    }


    // ==========================================
    // 🎨 MODERN ACTION SHEET POPUP
    // ==========================================
    class ModernActionPopup extends JPopupMenu {
        public ModernActionPopup(AlertCard parentCard, AlertItem data) {
            setOpaque(false);
            setBorder(new EmptyBorder(5, 5, 5, 5));
            setBackground(new Color(0, 0, 0, 0));

            JPanel pnlContainer = new RoundedPanel(12, Color.WHITE);
            pnlContainer.setLayout(new BoxLayout(pnlContainer, BoxLayout.Y_AXIS));
            pnlContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 5, 5, 5)
            ));

            if (data.type == AlertType.EXPIRING_SOON) {
                // ✅ OPTION 1: Giảm giá NGAY tại chỗ (Mở Popup Realtime mới) giữ nguyên...
                pnlContainer.add(createPopupItem("⚡ Giảm giá ngay lập tức", e -> {
                    setVisible(false);
                    xuLyGiamGiaNgay(parentCard, data);
                }));
                
                // ✅ OPTION 2: GỬI NGẦM SANG MODULE GIẢM GIÁ (ĐÃ FIX)
                pnlContainer.add(createPopupItem("📤 Gửi sang khu vực Xử lý Giảm giá", e -> {
                    setVisible(false);
                    // 1. Đổi UI Card sang trạng thái khóa (Màu cam/vàng) tại chỗ
                    parentCard.markAsPending("CHỜ GIẢM GIÁ", "Đã lưu vào form");
                    
                    // 2. Bắn tín hiệu ngầm (Không dùng hàm chuyenSangGiamGiaUI cũ nữa)
                    if (navigationCallback != null) {
                        navigationCallback.navigateTo("GiamGiaNgam", data.maSP);
                    }
                }));
                
                // ❌ ĐÃ XÓA TÙY CHỌN: "Chuyển kho / Đảo hàng" (Không hợp lý với hàng cận Date)
            } else if (data.type == AlertType.EXPIRED) {
                pnlContainer.add(createPopupItem("⏳ Chờ tiêu hủy", e -> {
                    setVisible(false);
                    
                    // 1. Đổi UI tại chỗ
                    parentCard.markAsPending("CHỜ TIÊU HỦY", "Đã gửi vào danh sách");
                    
                    // 2. 🔥 Cải tiến: Gọi thẳng callback để chuyển tab và đẩy dữ liệu
                    if (navigationCallback != null) {
                        // Truyền cả mã lô và lý do mặc định
                        navigationCallback.navigateTo("TieuHuy", data.lotNumber);
                    }
                }));
            } else if (data.type == AlertType.LECH_KHO) {
                pnlContainer.add(createPopupItem("📋 Mở phiếu kiểm kê", e -> {
                    setVisible(false);
                    // 1. Đổi UI tại chỗ báo hiệu đang xử lý
                    parentCard.markAsPending("ĐANG KIỂM KÊ", "Đã mở form");
                    
                    // 2. Bắn tín hiệu ngầm: Ghép Mã SP và Mã Lô bằng dấu "_"
                    if (navigationCallback != null) {
                        navigationCallback.navigateTo("KiemKeNgam", data.maSP + "_" + data.lotNumber);
                    }
                }));

            } else if (data.type == AlertType.LOW_STOCK) {
                // 1. NHẬP HÀNG NGAY -> Chuyển thẳng trang
                pnlContainer.add(createPopupItem("📦 Nhập hàng ngay", e -> {
                    setVisible(false);
                    parentCard.markAsPending("ĐANG NHẬP", "Đã mở form");
                    if (navigationCallback != null) {
                        // =========================================================
                        // 🧠 THUẬT TOÁN ĐẶT HÀNG THÔNG MINH (DEMAND-DRIVEN ERP)
                        // =========================================================
                        // (Thực tế sếp có thể query các số này từ lịch sử bán hàng trong DB)
                        int sucBanNgay = 12;         // Tốc độ bán trung bình: 12 SP / ngày
                        int thoiGianGiaoHang = 3;    // Lead time: NCC mất 3 ngày để chở hàng tới
                        int chuKyNhapHang = 14;      // Chu kỳ: Muốn nhập 1 lần đủ bán trong 2 tuần (14 ngày)
                        int quyCachDongGoi = 10;     // Quy cách: Lốc 10 cái, Thùng 24 cái...

                        // Bước 1: Tính Tồn kho an toàn (Đủ bán trong lúc chờ hàng về + 2 ngày rủi ro kẹt xe)
                        int tonKhoAnToan = sucBanNgay * (thoiGianGiaoHang + 2); 

                        // Bước 2: Tính Mức tồn kho mục tiêu (Đủ bán trong 1 chu kỳ + Tồn an toàn)
                        int tonKhoMucTieu = (sucBanNgay * chuKyNhapHang) + tonKhoAnToan;

                        // Bước 3: Tính số lượng thực tế cần bù (Mục tiêu - Đang có)
                        int slCanBu = tonKhoMucTieu - data.soLuongTon;
                        if (slCanBu <= 0) slCanBu = quyCachDongGoi; // Rủi ro dữ liệu âm -> Gợi ý nhập 1 thùng tối thiểu

                        // Bước 4: Làm tròn lên theo quy cách đóng gói của Nhà cung cấp
                        int slDeXuat = (int) (Math.ceil((double) slCanBu / quyCachDongGoi) * quyCachDongGoi);
                        // =========================================================

                        navigationCallback.navigateTo("NhapHangNgay", data.maSP + "|" + data.productName + "|" + slDeXuat);
                    }
                }));
                
                // 2. NHẬP HÀNG SAU -> Bắn tín hiệu ngầm dạng Gợi ý
                pnlContainer.add(createPopupItem("📝 Lưu gợi ý nhập sau", e -> {
                    setVisible(false);
                    parentCard.markAsPending("CHỜ NHẬP", "Đã ghim gợi ý");
                    if (navigationCallback != null) {
                        // (ÁP DỤNG CÙNG THUẬT TOÁN ĐỂ ĐỒNG BỘ CON SỐ GỢI Ý)
                        int sucBanNgay = 12;
                        int thoiGianGiaoHang = 3;
                        int chuKyNhapHang = 14;
                        int quyCachDongGoi = 10;

                        int tonKhoAnToan = sucBanNgay * (thoiGianGiaoHang + 2); 
                        int tonKhoMucTieu = (sucBanNgay * chuKyNhapHang) + tonKhoAnToan;
                        int slCanBu = tonKhoMucTieu - data.soLuongTon;
                        
                        if (slCanBu <= 0) slCanBu = quyCachDongGoi; 
                        int slDeXuat = (int) (Math.ceil((double) slCanBu / quyCachDongGoi) * quyCachDongGoi);

                        navigationCallback.navigateTo("NhapHangSau", data.productName + "|" + slDeXuat);
                    }
                }));
            }

            add(pnlContainer);
        }

        // =============================================
        // 🔥 XỬ LÝ GIẢM GIÁ NGAY: Tính tự động + Dialog xác nhận
        // =============================================
        private void xuLyGiamGiaNgay(AlertCard parentCard, AlertItem data) {
            // 1. Chạy engine tính mức giảm tối ưu trong luồng nền
            SwingWorker<Logic.GiamGiaLogic, Void> calcWorker = new SwingWorker<>() {
                @Override
                protected Logic.GiamGiaLogic doInBackground() {
                    return new Logic.GiamGiaLogic();
                }

                @Override
                protected void done() {
                    try {
                        Logic.GiamGiaLogic logic = get();

                        // Lấy thêm thông tin từ DB để tính đúng
                        int soNgayConLai = 0;
                        java.math.BigDecimal giaBan = java.math.BigDecimal.ZERO;

                        try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                            java.sql.PreparedStatement ps = con.prepareStatement(
                                "SELECT TOP 1 ct.HSD, sp.GiaBan FROM ChiTietLoHang ct " +
                                "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                                "WHERE ct.MaLoHang = ?")) {
                            ps.setString(1, data.lotNumber);
                            java.sql.ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                java.sql.Date hsd = rs.getDate("HSD");
                                if (hsd != null) {
                                    soNgayConLai = (int) java.time.temporal.ChronoUnit.DAYS
                                        .between(java.time.LocalDate.now(), hsd.toLocalDate());
                                }
                                giaBan = rs.getBigDecimal("GiaBan");
                            }
                        } catch (Exception ignored) {}

                        // Tính mức giảm đề xuất
                        double giaNhapDouble = data.giaNhap != null ? data.giaNhap.doubleValue() : 0.0;
                        double giaBanDouble  = giaBan.doubleValue();
                        double phanTramDeXuat = logic.tinhGiaGiamTuDong(
                            soNgayConLai, data.soLuongTon, giaNhapDouble, giaBanDouble
                        );
                        int phanTramInt = (int) Math.round(phanTramDeXuat * 100);

                        // 2. Hiện dialog xác nhận với mức giảm đề xuất
                        hienDialogXacNhanGiamGia(parentCard, data, phanTramInt, giaBanDouble, logic);

                    } catch (Exception e) {
                        GUI.HoTro.TienIchGiaoDien.hienThiThongBao(parentCard,
                            "Lỗi tính mức giảm: " + e.getMessage(), "ERROR");
                    }
                }
            };
            calcWorker.execute();
        }

        private void hienDialogXacNhanGiamGia(AlertCard parentCard, AlertItem data,
                                            int phanTramDeXuat, double giaBan,
                                            Logic.GiamGiaLogic logic) {

            // ========== KHỞI TẠO DIALOG ==========
            Window parentWindow = SwingUtilities.getWindowAncestor(parentCard);

            JDialog dialog = new JDialog(
                    parentWindow instanceof Frame ? (Frame) parentWindow : null,
                    "Xác nhận Giảm giá Ngay",
                    true
            );

            dialog.setUndecorated(true);
            dialog.setSize(420, 340);
            dialog.setLocationRelativeTo(parentCard);

            // ========== BẢNG MÀU LIGHT THEME ==========
            Color BG_MAIN = new Color(255, 255, 255);
            Color BG_INPUT = new Color(248, 250, 252);
            Color BORDER = new Color(226, 232, 240);

            Color TEXT_PRIMARY = new Color(15, 23, 42);
            Color TEXT_SECONDARY = new Color(100, 116, 139);

            Color ACCENT_BLUE = new Color(37, 99, 235);
            Color ACCENT_BLUE_HOVER = new Color(29, 78, 216);

            Color ACCENT_RED = new Color(220, 38, 38);

            // ========== PANEL CHÍNH ==========
            JPanel mainPanel = new JPanel(new BorderLayout()) {

                @Override
                protected void paintComponent(Graphics g) {

                    Graphics2D g2 = (Graphics2D) g.create();

                    g2.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    // Shadow
                    g2.setColor(new Color(0, 0, 0, 18));
                    g2.fillRoundRect(4, 6, getWidth() - 8, getHeight() - 8, 22, 22);

                    // Background
                    g2.setColor(BG_MAIN);
                    g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);

                    // Border
                    g2.setColor(BORDER);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 20, 20);

                    g2.dispose();
                }
            };

            mainPanel.setOpaque(false);
            mainPanel.setBorder(new EmptyBorder(22, 24, 20, 24));

            // ========== HEADER ==========
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(false);

            JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titleWrap.setOpaque(false);

            JLabel iconTitle = new JLabel("🏷 ");
            iconTitle.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

            JLabel lblTitle = new JLabel("Xác nhận Giảm giá Ngay");
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
            lblTitle.setForeground(TEXT_PRIMARY);

            titleWrap.add(iconTitle);
            titleWrap.add(lblTitle);

            JButton btnClose = new JButton("✕");

            btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnClose.setForeground(TEXT_SECONDARY);
            btnClose.setBorder(null);
            btnClose.setFocusPainted(false);
            btnClose.setContentAreaFilled(false);
            btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnClose.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent e) {
                    btnClose.setForeground(TEXT_PRIMARY);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    btnClose.setForeground(TEXT_SECONDARY);
                }
            });

            btnClose.addActionListener(e -> dialog.dispose());

            headerPanel.add(titleWrap, BorderLayout.WEST);
            headerPanel.add(btnClose, BorderLayout.EAST);

            // ========== CONTENT ==========
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(new EmptyBorder(18, 0, 10, 0));

            // ========== CARD THÔNG TIN ==========
            JPanel infoCard = new JPanel();
            infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
            infoCard.setOpaque(true);
            infoCard.setBackground(BG_INPUT);
            infoCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1, true),
                    new EmptyBorder(14, 16, 14, 16)
            ));

            infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

            JLabel lblSanPham = new JLabel("Sản phẩm: " + data.productName);
            lblSanPham.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblSanPham.setForeground(TEXT_PRIMARY);

            JLabel lblLoHang = new JLabel("Lô hàng: " + data.lotNumber);
            lblLoHang.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblLoHang.setForeground(TEXT_SECONDARY);
            lblLoHang.setBorder(new EmptyBorder(6, 0, 0, 0));

            infoCard.add(lblSanPham);
            infoCard.add(lblLoHang);

            // ========== PANEL ĐỀ XUẤT ==========
            JPanel deXuatPanel = new JPanel(new BorderLayout());
            deXuatPanel.setOpaque(false);
            deXuatPanel.setBorder(new EmptyBorder(18, 0, 0, 0));
            deXuatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            deXuatPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            JLabel lblDeXuatText = new JLabel("✨ Mức giảm đề xuất");
            lblDeXuatText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblDeXuatText.setForeground(TEXT_SECONDARY);

            JLabel lblDeXuatValue = new JLabel(phanTramDeXuat + "%");
            lblDeXuatValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblDeXuatValue.setForeground(ACCENT_RED);

            deXuatPanel.add(lblDeXuatText, BorderLayout.WEST);
            deXuatPanel.add(lblDeXuatValue, BorderLayout.EAST);

            // ========== INPUT ==========
            // ========== INPUT (ĐÃ CHỈNH SỬA) ==========
            JPanel inputPanel = new JPanel(new BorderLayout(0, 8));
            inputPanel.setOpaque(false);
            inputPanel.setBorder(new EmptyBorder(18, 0, 0, 0));
            inputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            inputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            JLabel lblInputTitle = new JLabel("Mức giảm (%)");
            lblInputTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblInputTitle.setForeground(TEXT_SECONDARY);

            JSpinner spinnerMucGiam = new JSpinner(
                    new SpinnerNumberModel(Math.max(1, phanTramDeXuat), 1, 90, 1)
            );

            spinnerMucGiam.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            spinnerMucGiam.setPreferredSize(new Dimension(110, 42));

            JComponent editor = spinnerMucGiam.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                tf.setBackground(BG_INPUT);
                tf.setForeground(TEXT_PRIMARY);
                tf.setCaretColor(TEXT_PRIMARY);
                tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                
                // THAY ĐỔI Ở ĐÂY: Sử dụng Margin thay vì đặt Border trực tiếp lên Textfield
                tf.setMargin(new Insets(5, 10, 5, 10)); 
            }

            // Chỉ set border cho bản thân cái JSpinner, không set vào TextField bên trong
            spinnerMucGiam.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            spinnerMucGiam.setBackground(BG_INPUT);

            inputPanel.add(lblInputTitle, BorderLayout.NORTH);
            inputPanel.add(spinnerMucGiam, BorderLayout.CENTER);

            // ========== NOTE ==========
            JLabel lblNote = new JLabel(
                    "<html>" +
                    "<span style='color:#64748B;'>" +
                    "ⓘ Thay đổi này sẽ áp dụng ngay lập tức cho báo giá tiếp theo trong hệ thống." +
                    "</span>" +
                    "</html>"
            );

            lblNote.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblNote.setBorder(new EmptyBorder(18, 0, 0, 0));
            lblNote.setAlignmentX(Component.LEFT_ALIGNMENT);

            // ========== ADD CONTENT ==========
            contentPanel.add(infoCard);
            contentPanel.add(deXuatPanel);
            contentPanel.add(inputPanel);
            contentPanel.add(lblNote);

            // ========== FOOTER ==========
            JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            footerPanel.setOpaque(false);
            footerPanel.setBorder(new EmptyBorder(18, 0, 0, 0));

            // ========== BUTTON HỦY ==========
            JButton btnHuy = new JButton("Hủy") {

                @Override
                protected void paintComponent(Graphics g) {

                    Graphics2D g2 = (Graphics2D) g.create();

                    g2.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    g2.setColor(BG_INPUT);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                    g2.setColor(BORDER);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                    g2.dispose();

                    super.paintComponent(g);
                }
            };

            btnHuy.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnHuy.setForeground(TEXT_PRIMARY);

            btnHuy.setPreferredSize(new Dimension(90, 38));

            btnHuy.setBorder(new EmptyBorder(8, 16, 8, 16));

            btnHuy.setFocusPainted(false);
            btnHuy.setContentAreaFilled(false);

            btnHuy.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnHuy.addActionListener(e -> dialog.dispose());

            // ========== BUTTON OK ==========
            JButton btnOK = new JButton("Áp dụng") {

                @Override
                protected void paintComponent(Graphics g) {

                    Graphics2D g2 = (Graphics2D) g.create();

                    g2.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    ButtonModel model = getModel();

                    if (model.isRollover()) {
                        g2.setColor(ACCENT_BLUE_HOVER);
                    } else {
                        g2.setColor(ACCENT_BLUE);
                    }

                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                    g2.dispose();

                    super.paintComponent(g);
                }
            };

            btnOK.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnOK.setForeground(Color.WHITE);

            btnOK.setPreferredSize(new Dimension(100, 38));

            btnOK.setBorder(new EmptyBorder(8, 16, 8, 16));

            btnOK.setFocusPainted(false);
            btnOK.setContentAreaFilled(false);

            btnOK.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnOK.addActionListener(e -> {

                int mucGiamCuoiCung = (int) spinnerMucGiam.getValue();

                dialog.dispose();

                luuGiamGiaVaoDB(
                        parentCard,
                        data,
                        mucGiamCuoiCung,
                        logic
                );
            });

            footerPanel.add(btnHuy);
            footerPanel.add(btnOK);

            // ========== GỘP ==========
            mainPanel.add(headerPanel, BorderLayout.NORTH);
            mainPanel.add(contentPanel, BorderLayout.CENTER);
            mainPanel.add(footerPanel, BorderLayout.SOUTH);

            dialog.setContentPane(mainPanel);

            // Transparent để bo góc đẹp
            dialog.setBackground(new Color(0, 0, 0, 0));

            dialog.setVisible(true);
        }

        private void luuGiamGiaVaoDB(AlertCard parentCard, AlertItem data,
                                    int phanTramGiam, Logic.GiamGiaLogic logic) {
            SwingWorker<String, Void> saveWorker = new SwingWorker<>() {
                @Override
                protected String doInBackground() throws Exception {
                    String maGG = Logic.TaoMaTuDongLogic.taoMaGiamGia();

                    Data.GiamGia gg = new Data.GiamGia();
                    gg.setMaGiamGia(maGG);
                    gg.setMaSP(data.maSP);
                    gg.setBatDau(java.time.LocalDateTime.now());
                    // Hết hạn 3 ngày hoặc đúng ngày HSD sản phẩm
                    gg.setKetThuc(java.time.LocalDateTime.now().plusDays(3));
                    gg.setGiamGia(new java.math.BigDecimal(phanTramGiam));
                    gg.setLoaiGiamGia("Xả kho cận date");
                    gg.setTrangThaiGiamGia("Đang diễn ra");
                    gg.setSoLuongApDung(data.soLuongTon);

                    logic.themGiamGia(gg);
                    return maGG;
                }

                @Override
                protected void done() {
                    try {
                        String maGG = get();
                        parentCard.markAsPending();
                        GUI.HoTro.TienIchGiaoDien.hienThiThongBao(parentCard,
                            "Đã áp dụng giảm giá <b>" + phanTramGiam + "%</b> cho lô <b>"
                            + data.lotNumber + "</b>. Mã KM: " + maGG, "SUCCESS");

                        // Tải lại panel sau 1s để thẻ phản ánh trạng thái mới
                        Timer t = new Timer(1000, e -> {
                            Container p = parentCard.getParent();
                            while (p != null && !(p instanceof CanhBaoKhoPanel)) p = p.getParent();
                            if (p instanceof CanhBaoKhoPanel) ((CanhBaoKhoPanel) p).taiDuLieuThucTeTuKho();
                        });
                        t.setRepeats(false);
                        t.start();

                    } catch (Exception ex) {
                        GUI.HoTro.TienIchGiaoDien.hienThiThongBao(parentCard,
                            "Lỗi lưu giảm giá: " + ex.getMessage(), "ERROR");
                    }
                }
            };
            saveWorker.execute();
        }

        // =============================================
        // 📋 CHUYỂN SANG GiamGiaUI (khi không xác nhận giảm ngay)
        // =============================================
        private void chuyenSangGiamGiaUI(AlertItem data) {
            // Tìm CanhBaoKhoPanel cha để lấy callback
            Container p = CanhBaoKhoPanel.this;
            if (p instanceof CanhBaoKhoPanel) {
                CanhBaoKhoPanel panel = (CanhBaoKhoPanel) p;
                if (panel.navigationCallback != null) {
                    // Gọi navigate với maSP để GiamGiaUI tự filter/highlight SP đó
                    panel.navigationCallback.navigateTo("GiamGia", data.maSP);
                } else {
                    // Fallback: thông báo nếu chưa gắn callback
                    JOptionPane.showMessageDialog(null,
                        "<html>Chuyển sang module <b>Giảm Giá</b>.<br>" +
                        "Tìm kiếm mã SP: <b>" + data.maSP + "</b></html>",
                        "Điều hướng", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        private void chuyenSangTieuHuyUI(AlertItem data) {
            Container p = CanhBaoKhoPanel.this;
            if (p instanceof CanhBaoKhoPanel) {
                CanhBaoKhoPanel panel = (CanhBaoKhoPanel) p;
                if (panel.navigationCallback != null) {
                    // Truyền MÃ LÔ (lotNumber) sang vì module Tiêu Hủy thao tác theo Lô
                    panel.navigationCallback.navigateTo("TieuHuy", data.lotNumber);
                }
            }
        }

        private JButton createPopupItem(String text, java.awt.event.ActionListener action) {
            JButton btn = new JButton(text);
            
            // 🎯 FIX LỖI 1: Đổi font sang Segoe UI Emoji để render icon màu mè mượt mà
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14)); 
            
            btn.setForeground(TEXT_MAIN);
            btn.setBackground(Color.WHITE);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(true);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // 🎯 FIX LỖI 2: Cởi trói chiều ngang (Integer.MAX_VALUE), cho phép popup tự phình to theo chữ
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); 
            btn.setBorder(new EmptyBorder(8, 15, 8, 20)); // Tăng margin bên phải cho dễ thở
            
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(new Color(248, 250, 252));
                    btn.setForeground(INFO_FG);
                }
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(Color.WHITE);
                    btn.setForeground(TEXT_MAIN);
                }
            });
            btn.addActionListener(action);
            return btn;
        }


        @Override protected void paintComponent(Graphics g) { super.paintComponent(g); }
    }
    // ==========================================
    // COMPONENT: CARD CẢNH BÁO (ĐÃ NÂNG CẤP WORKFLOW)
    // ==========================================
    class AlertCard extends RoundedPanel {
        
        private StatusBadge lblStatus;
        private ModernButton btnAction;
        private ModernButton btnView;
        private AlertItem data;

        public AlertCard(AlertItem data) {
            super(15, BG_CARD);
            this.data = data;
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 110)); // Giữ nguyên cấu trúc

            JPanel pnlPriority = new JPanel();
            pnlPriority.setPreferredSize(new Dimension(6, 110));

            // --- 1. XỬ LÝ MÀU SẮC ĐỒNG BỘ TRONG 1 LẦN KHAI BÁO ---
            Color bgBadge = BG_MAIN, fgBadge = TEXT_SUB;
            String iconBadge = "";
            
            switch (data.priority) {
                case HIGH: 
                    pnlPriority.setBackground(DANGER_FG); 
                    bgBadge = DANGER_BG; fgBadge = DANGER_FG; iconBadge = "🔴"; 
                    break;
                case MEDIUM: 
                    pnlPriority.setBackground(WARNING_FG); 
                    bgBadge = WARNING_BG; fgBadge = WARNING_FG; iconBadge = "🟡"; 
                    break;
                case LOW: 
                    pnlPriority.setBackground(INFO_FG); 
                    bgBadge = INFO_BG; fgBadge = INFO_FG; iconBadge = "📉"; 
                    break;
            }
            
            // 🎯 Ghi đè màu sắc nếu là Lệch Kho (Tím đặc trưng)
            if (data.type == AlertType.LECH_KHO) {
                pnlPriority.setBackground(PURPLE_FG); // Đổi dải màu bên mép trái thành Tím luôn
                bgBadge = PURPLE_BG; 
                fgBadge = PURPLE_FG; 
                iconBadge = "⚖️"; 
            }

            // --- 2. BỐ TRÍ LAYOUT VÀ UI ---
            JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
            pnlLeft.setOpaque(false);
            
            RoundedPanel pnlImage = new RoundedPanel(10, BORDER_COLOR);
            pnlImage.setPreferredSize(new Dimension(70, 70));
            pnlImage.setLayout(new BorderLayout());
            JLabel lblImgText = new JLabel("", SwingConstants.CENTER); // Xóa chữ "SP" mặc định
            
            // Gọi Cache lấy ảnh với kích thước 70x70
            ImageIcon icon = Logic.QuanLyAnh.layIconAnh(data.hinhAnh, 70, 70); 
            
            if (icon != null) {
                lblImgText.setIcon(icon);
            } else {
                // Fallback hiển thị chữ SP nếu sản phẩm chưa có ảnh
                lblImgText.setText("SP");
                lblImgText.setForeground(TEXT_SUB);
            }
            
            pnlImage.add(lblImgText);
            
            pnlLeft.add(pnlPriority);
            pnlLeft.add(pnlImage);

            // Cột giữa: Tên SP + Badge + Info
            JPanel pnlCenter = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
            pnlCenter.setOpaque(false);
            pnlCenter.setBorder(new EmptyBorder(15, 10, 15, 10));

            JPanel pnlNameWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pnlNameWrap.setOpaque(false);
            pnlNameWrap.setPreferredSize(new Dimension(400, 25));
            JLabel lblName = new JLabel(data.productName);
            lblName.setFont(FONT_TITLE.deriveFont(Font.BOLD, 18f));
            lblName.setForeground(NAVY_BTN);
            pnlNameWrap.add(lblName);
            
            // Khởi tạo Badge Trạng Thái với màu đã setup ở trên
            JPanel pnlBadgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pnlBadgeWrap.setOpaque(false);
            pnlBadgeWrap.setPreferredSize(new Dimension(400, 25));
            lblStatus = new StatusBadge(iconBadge + " " + data.statusText, fgBadge, bgBadge);
            pnlBadgeWrap.add(lblStatus);

            JPanel pnlInfoWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pnlInfoWrap.setOpaque(false);
            pnlInfoWrap.setPreferredSize(new Dimension(400, 20));
            JLabel lblInfo = new JLabel("📦 Lô " + data.lotNumber + "   |   📍 " + data.location);
            lblInfo.setFont(FONT_REGULAR);
            lblInfo.setForeground(TEXT_SUB);
            pnlInfoWrap.add(lblInfo);

            pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
            pnlCenter.add(pnlNameWrap);
            pnlCenter.add(pnlBadgeWrap);
            pnlCenter.add(pnlInfoWrap);

            JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 30));
            pnlRight.setOpaque(false);
            
            String mainBtnText = (data.type == AlertType.LOW_STOCK) ? "Nhập hàng" : "Xử lý kho";
            btnAction = new ModernButton(mainBtnText, NAVY_BTN, Color.WHITE);
            btnView = new ModernButton("Chi tiết", Color.WHITE, TEXT_MAIN);
            btnView.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

            // =====================================
            // 🎯 ROUTING & POPUP THEO CHUẨN ERP
            // =====================================
            btnAction.addActionListener(e -> {
                if (data.type == AlertType.EXPIRING_SOON || data.type == AlertType.LOW_STOCK || data.type == AlertType.LECH_KHO) {
                    ModernActionPopup popup = new ModernActionPopup(this, data);
                    popup.show(btnAction, 0, btnAction.getHeight() + 4);
                } else {
                    // Đối với EXPIRED, gọi xuống Handler để mở Bảng Xác Nhận
                    WarehouseAlertActionHandler.routeAction(data.type, data, this);
                }
            });

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

        // =====================================
        // ✨ STATE MUTATION (KHÔNG RELOAD PANEL)
        // =====================================

        public void markAsPending(String badgeText, String buttonText) {
            SwingUtilities.invokeLater(() -> {
                // 1. Cập nhật Badge: Nền cam nhạt, chữ cam đậm, icon đồng hồ
                lblStatus.updateStyle("⏳ " + badgeText, 
                    new Color(217, 119, 6),   // Cam đậm (Warning Text)
                    new Color(254, 243, 199)); // Cam nhạt (Warning BG)
                
                // 2. Chuyển nút sang trạng thái Disabled chuẩn UI (Màu xám)
                btnAction.setText(buttonText);
                btnAction.setBackground(new Color(241, 245, 249)); // Slate 100
                btnAction.setForeground(new Color(148, 163, 184)); // Slate 400
                btnAction.setEnabled(false);
                btnAction.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                
                btnView.setForeground(new Color(148, 163, 184)); // Mờ nút chi tiết đi
                
                // 3. Hiệu ứng flash nhẹ báo hiệu chuyển State thành công
                setBackground(new Color(254, 252, 232)); 
                Timer t = new Timer(350, e -> setBackground(BG_CARD)); // BG_CARD là màu gốc
                t.setRepeats(false);
                t.start();
                
                revalidate();
                repaint();
            });
        }

        // Giữ lại hàm cũ dạng overload để không vỡ logic ở các chỗ khác gọi markAsPending()
        public void markAsPending() {
            markAsPending("ĐANG CHỜ XỬ LÝ", "Xử lý kho");
        }

    }
    // ==========================================
    // 🎨 COMPONENT: BADGE TRẠNG THÁI BO GÓC
    // ==========================================
    class StatusBadge extends JLabel {
        private Color bgColor;

        public StatusBadge(String text, Color fg, Color bg) {
            super(text);
            setForeground(fg);
            this.bgColor = bg;
            setFont(FONT_BOLD.deriveFont(12f));
            setBorder(new EmptyBorder(4, 10, 4, 10)); // Padding gọn gàng
            setOpaque(false);
        }

        public void updateStyle(String text, Color fg, Color bg) {
            setText(text);
            setForeground(fg);
            this.bgColor = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Vẽ nền bo góc
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            super.paintComponent(g);
            g2.dispose();
        }
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