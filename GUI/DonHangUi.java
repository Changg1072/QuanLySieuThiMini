package GUI;

import GUI.HoTro.DinhDangUtil;
import GUI.HoTro.NutBoGoc;
import GUI.HoTro.TheBongDo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DonHangUi extends JPanel {

    // 🎨 ĐÃ TRẢ LẠI BẢNG MÀU CAM ĐỎ NGUYÊN THỦY CỦA BẠN!
    private final Color COLOR_BG = new Color(240, 242, 245); 
    private final Color COLOR_CARD_BG = Color.WHITE; 
    private final Color COLOR_PRIMARY = new Color(238, 77, 45); // Sắc cam đỏ năng động!
    private final Color COLOR_TEXT_MAIN = new Color(36, 36, 36);
    private final Color COLOR_TEXT_SUB = new Color(130, 130, 130);
    private final Color COLOR_DISCOUNT = new Color(38, 170, 153);
    private final Color COLOR_BORDER = new Color(230, 230, 230); 
    private final Color COLOR_HOVER = new Color(245, 247, 250);

    private JPanel centerPanel;
    private List<JLabel> tabLabels = new ArrayList<>();
    
    // 💡 CÁC BIẾN LƯU TRẠNG THÁI UI (STATE)
    private String boLocHienTai = "Tất cả"; 
    private final TheBongDo.RoundedTextField txtSearch;
    
    // 🌟 VŨ KHÍ BÍ MẬT: LỊCH XỔ XUỐNG TỰ CHẾ
    private LichXoXuongCustom dateChooserNgay;

    public DonHangUi() {
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        txtSearch = new TheBongDo.RoundedTextField("🔍 Tìm kiếm mã hóa đơn, tên khách...", 30);

        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        
        taiDuLieuTuDatabase();
    }

    // =======================================================
    // 1. PHẦN TOP: BỘ LỌC, SEARCH BAR, LỊCH SANG CHẢNH 🌟
    // =======================================================
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(COLOR_CARD_BG);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

        // --- 🏷️ Dãy Tabs ---
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        tabPanel.setBackground(COLOR_CARD_BG);
        tabPanel.setBorder(new EmptyBorder(10, 10, 0, 10));

        String[] tabs = {"Tất cả", "Trả Hàng", "Khách Vãng Lai", "Khách Vip", "Khách Vàng", "Khách Bạc", "Khách Không Hạng"};
        for (String tabName : tabs) {
            JLabel lblTab = createTabLabel(tabName);
            tabLabels.add(lblTab);
            tabPanel.add(lblTab);
        }
        setActiveTab(tabLabels.get(0));

        // --- 🔍 Search Bar & Component Ngày Tháng ---
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(COLOR_CARD_BG);
        searchPanel.setBorder(new EmptyBorder(10, 15, 12, 15));
        
        txtSearch.setPreferredSize(new Dimension(350, 40)); 
        txtSearch.setFont(new Font("Calibri", Font.PLAIN, 15));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { taiDuLieuTuDatabase(); }
            public void removeUpdate(DocumentEvent e) { taiDuLieuTuDatabase(); }
            public void changedUpdate(DocumentEvent e) { taiDuLieuTuDatabase(); }
        });

        // 📅 LỊCH TỰ CHẾ (Không dùng Checkbox)
        dateChooserNgay = new LichXoXuongCustom();
        dateChooserNgay.setPreferredSize(new Dimension(160, 40));
        
        // Sự kiện khi Click chọn ngày trên Lịch tự chế
        dateChooserNgay.setHanhDongChonNgay(() -> {
            taiDuLieuTuDatabase();
        });

        JPanel pnlLeftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLeftSearch.setOpaque(false);
        pnlLeftSearch.add(txtSearch);
        pnlLeftSearch.add(Box.createHorizontalStrut(20));
        pnlLeftSearch.add(dateChooserNgay); // Lịch gắn trực tiếp, sang xịn mịn

        NutBoGoc btnRefresh = new NutBoGoc("🔄 Làm mới");
        btnRefresh.setColorBackground(COLOR_DISCOUNT);
        btnRefresh.setArc(15);
        btnRefresh.setPreferredSize(new Dimension(120, 40));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText(""); 
            boLocHienTai = "Tất cả"; 
            setActiveTab(tabLabels.get(0)); 
            
            // Xóa bộ lọc ngày, đưa lịch về mặc định
            dateChooserNgay.setNgay(null); 
            taiDuLieuTuDatabase(); 
        });

        JPanel pnlRightRefresh = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlRightRefresh.setOpaque(false);
        pnlRightRefresh.add(btnRefresh);

        searchPanel.add(pnlLeftSearch, BorderLayout.WEST);
        searchPanel.add(pnlRightRefresh, BorderLayout.EAST);

        topPanel.add(tabPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        return topPanel;
    }

    private JLabel createTabLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Calibri", Font.PLAIN, 16));
        label.setForeground(COLOR_TEXT_SUB);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setBorder(new EmptyBorder(10, 5, 10, 5));
        
        label.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { 
                setActiveTab(label); 
                boLocHienTai = text; 
                taiDuLieuTuDatabase(); 
            }
        });
        return label;
    }

    private void setActiveTab(JLabel activeLabel) {
        for (JLabel label : tabLabels) {
            if (label == activeLabel) {
                label.setForeground(COLOR_PRIMARY);
                label.setFont(new Font("Calibri", Font.BOLD, 16));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 3, 0, COLOR_PRIMARY),
                        new EmptyBorder(10, 5, 7, 5)
                ));
            } else {
                label.setForeground(COLOR_TEXT_SUB);
                label.setFont(new Font("Calibri", Font.PLAIN, 16));
                label.setBorder(new EmptyBorder(10, 5, 10, 5));
            }
        }
    }

    // =======================================================
    // 2. KHUNG CENTER CHỨA DANH SÁCH 
    // =======================================================
    private JScrollPane createCenterPanel() {
        centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(COLOR_BG); 
        centerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    // =======================================================
    // 3. ĐỘNG CƠ TURBO + LỌC NGÀY CHUẨN 🚀
    // =======================================================
    public void taiDuLieuTuDatabase() {
        centerPanel.removeAll();
        String tuKhoa = txtSearch.getText().trim().toLowerCase();
        
        // Lấy ngày từ Lịch Custom
        final LocalDate finalNgayCanLoc = dateChooserNgay.getNgay(); 
        
        SwingWorker<List<JPanel>, Void> worker = new SwingWorker<List<JPanel>, Void>() {
            @Override
            protected List<JPanel> doInBackground() throws Exception {
                List<JPanel> listCards = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO data = Dao.TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();

                List<Data.HoaDon> danhSachDaSapXep = new ArrayList<>(data.dsHoaDon);
                danhSachDaSapXep.sort((hd1, hd2) -> {
                    java.time.LocalDateTime time1 = hd1.getNgayTao();
                    java.time.LocalDateTime time2 = hd2.getNgayTao();
                    
                    if (time1 != null && time2 != null) {
                        int dateCompare = time2.compareTo(time1);
                        if (dateCompare != 0) return dateCompare;
                    } else if (time1 == null && time2 != null) return 1;
                    else if (time2 == null && time1 != null) return -1;
                    
                    String ma1 = hd1.getMaHD() != null ? hd1.getMaHD() : "";
                    String ma2 = hd2.getMaHD() != null ? hd2.getMaHD() : "";
                    return ma1.compareTo(ma2);
                });

                for (Data.HoaDon hd : danhSachDaSapXep) {
                    String[] khInfo = data.mapKhachHang.get(hd.getMaKH());
                    String tenKH = (khInfo != null) ? khInfo[0] : "Khách Vãng Lai";
                    String hangKH = (khInfo != null && khInfo[1] != null) ? khInfo[1] : "Không hạng";
                    String trangThaiThat = (hd.getTraHang() != null && hd.getTraHang()) ? "Đã trả hàng" : "Hoàn thành";

                    boolean hopLeNgay = true;
                    if (finalNgayCanLoc != null) {
                        if (hd.getNgayTao() != null) {
                            LocalDate ngayTaoHD = hd.getNgayTao().toLocalDate();
                            hopLeNgay = ngayTaoHD.equals(finalNgayCanLoc);
                        } else {
                            hopLeNgay = false; 
                        }
                    }

                    boolean hopLeTab = false;
                    switch(boLocHienTai) {
                        case "Tất cả": hopLeTab = true; break;
                        case "Trả Hàng": hopLeTab = trangThaiThat.equals("Đã trả hàng"); break;
                        case "Khách Vãng Lai": hopLeTab = hangKH.equals("Không hạng") && tenKH.equals("Khách Vãng Lai"); break;
                        case "Khách Vip": hopLeTab = hangKH.equals("Vip"); break;
                        case "Khách Vàng": hopLeTab = hangKH.equals("Vàng"); break;
                        case "Khách Bạc": hopLeTab = hangKH.equals("Bạc"); break;
                        case "Khách Không Hạng": hopLeTab = hangKH.equals("Không hạng"); break;
                    }

                    boolean hopLeTuKhoa = tuKhoa.isEmpty() || 
                                          (hd.getMaHD() != null && hd.getMaHD().toLowerCase().contains(tuKhoa)) || 
                                          tenKH.toLowerCase().contains(tuKhoa);

                    if (hopLeTab && hopLeTuKhoa && hopLeNgay) {
                        String tenNV = data.mapNhanVien.getOrDefault(hd.getMaNV(), "Chưa rõ");

                        DonHangModel orderUi = new DonHangModel(
                            hd.getMaHD(),
                            hd.getNgayTao() != null ? hd.getNgayTao().format(formatter) : "N/A",
                            tenKH, hangKH, tenNV, trangThaiThat,
                            hd.getPhuongThucTT() != null ? hd.getPhuongThucTT() : "Tiền mặt",
                            hd.getTongGiamGia() != null ? hd.getTongGiamGia().toString() : "0",
                            hd.getKhachDua() != null ? hd.getKhachDua().toString() : "0",
                            hd.getTienThua() != null ? hd.getTienThua().toString() : "0"
                        );

                        List<Data.ChiTietHoaDon> dsChiTiet = data.mapChiTietHD.getOrDefault(hd.getMaHD(), new ArrayList<>());
                        for (Data.ChiTietHoaDon ct : dsChiTiet) {
                            String[] spInfo = data.mapSanPham.get(ct.getMaSp());
                            String tenSP = (spInfo != null) ? spInfo[0] : "Sản phẩm (" + ct.getMaSp() + ")";
                            String loaiSP = (spInfo != null) ? "Danh mục: " + spInfo[1] : "Mã lô: " + ct.getMaLoHang();

                            orderUi.addItem(new SanPhamModel(
                                tenSP, loaiSP,
                                ct.getDonGia() != null ? ct.getDonGia().toString() : "0",
                                ct.getSoLuong()
                            ));
                        }
                        listCards.add(createOrderCard(orderUi));
                    }
                }
                return listCards;
            }

            @Override
            protected void done() {
                try {
                    List<JPanel> loadedCards = get();
                    if (loadedCards.isEmpty()) {
                        JLabel lblEmpty = new JLabel("📭 Ôi không! Không tìm thấy đơn hàng nào khớp với yêu cầu!", SwingConstants.CENTER);
                        lblEmpty.setFont(new Font("Calibri", Font.ITALIC, 18));
                        lblEmpty.setForeground(COLOR_TEXT_SUB);
                        lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);
                        lblEmpty.setBorder(new EmptyBorder(60, 0, 0, 0));
                        centerPanel.add(lblEmpty);
                    } else {
                        for (JPanel card : loadedCards) {
                            centerPanel.add(card);
                            centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
                        }
                        // 🔥 ĐÃ FIX: Thêm lò xo hấp thụ khoảng trắng thừa, đẩy mọi thứ lên sát Top
                        centerPanel.add(Box.createVerticalGlue()); 
                    }
                    centerPanel.revalidate();
                    centerPanel.repaint();
                } catch (Exception e) {}
            }
        };
        worker.execute();
    }

    private JPanel createOrderCard(DonHangModel order) {
        TheBongDo card = new TheBongDo(15) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(1200, getPreferredSize().height);
            }
        };
        card.setLayout(new BorderLayout());
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1), new EmptyBorder(5, 5, 5, 5)));
        card.setMaximumSize(new Dimension(1200, Integer.MAX_VALUE));

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_PRIMARY, 2), new EmptyBorder(4, 4, 4, 4)));
                card.setBackground(new Color(255, 250, 248)); 
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1), new EmptyBorder(5, 5, 5, 5)));
                card.setBackground(COLOR_CARD_BG);
            }
        });

        // =====================================
        // 1. HEADER PANEL
        // =====================================
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER), new EmptyBorder(10, 15, 10, 15)));

        JPanel row1 = new JPanel(new BorderLayout());
        row1.setOpaque(false);
        JLabel lblMaHD = new JLabel(order.maHD + "  |  " + order.ngayTao);
        lblMaHD.setFont(new Font("Calibri", Font.BOLD, 15));
        lblMaHD.setForeground(COLOR_TEXT_MAIN);

        JLabel lblStatus = new JLabel(order.status.toUpperCase());
        lblStatus.setFont(new Font("Calibri", Font.BOLD, 14));
        
        // 🔥 IN ĐẬM CHỮ MÀU ĐỎ CẢNH BÁO
        if (order.status.equalsIgnoreCase("Đã trả hàng")) {
            lblStatus.setForeground(new Color(220, 53, 69)); // Màu đỏ
        } else {
            lblStatus.setForeground(COLOR_PRIMARY); // Màu cam mặc định
        }

        row1.add(lblMaHD, BorderLayout.WEST);
        row1.add(lblStatus, BorderLayout.EAST);

        JPanel row2 = new JPanel(new BorderLayout());
        row2.setOpaque(false);
        row2.setBorder(new EmptyBorder(8, 0, 0, 0)); 

        JLabel lblCustomer = new JLabel("👤 Khách hàng: " + order.customerName + " (" + order.customerTier + ")");
        lblCustomer.setFont(new Font("Calibri", Font.PLAIN, 14));
        lblCustomer.setForeground(COLOR_TEXT_SUB);

        JLabel lblStaff = new JLabel("💼 Nhân viên: " + order.tenNhanVien);
        lblStaff.setFont(new Font("Calibri", Font.PLAIN, 14));
        lblStaff.setForeground(COLOR_TEXT_SUB);

        row2.add(lblCustomer, BorderLayout.WEST);
        row2.add(lblStaff, BorderLayout.EAST);

        headerPanel.add(row1); headerPanel.add(row2);

        // =====================================
        // 2. BODY PANEL (Danh sách SP)
        // =====================================
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);

        if (!order.items.isEmpty()) {
            bodyPanel.add(createProductItem(order.items.get(0)));

            if (order.items.size() > 1) {
                JPanel hiddenPanel = new JPanel();
                hiddenPanel.setLayout(new BoxLayout(hiddenPanel, BoxLayout.Y_AXIS));
                hiddenPanel.setOpaque(false);
                hiddenPanel.setVisible(false); 

                for (int i = 1; i < order.items.size(); i++) {
                    hiddenPanel.add(createProductItem(order.items.get(i)));
                }

                JLabel lblToggle = new JLabel("Xem thêm " + (order.items.size() - 1) + " sản phẩm ˅");
                lblToggle.setFont(new Font("Calibri", Font.PLAIN, 14));
                lblToggle.setForeground(COLOR_PRIMARY);
                lblToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
                lblToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
                lblToggle.setBorder(new EmptyBorder(5, 0, 10, 0));

                lblToggle.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        boolean isExpanded = hiddenPanel.isVisible();
                        hiddenPanel.setVisible(!isExpanded);
                        lblToggle.setText(isExpanded ? "Xem thêm " + (order.items.size() - 1) + " sản phẩm ˅" : "Thu gọn ˄");
                        card.revalidate(); card.repaint();
                    }
                });

                bodyPanel.add(hiddenPanel); bodyPanel.add(lblToggle);
            }
        }

        // =====================================
        // 3. FOOTER PANEL (NẰM NGANG)
        // =====================================
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER), new EmptyBorder(15, 15, 15, 15)));

        JPanel westFooter = new JPanel();
        westFooter.setLayout(new BoxLayout(westFooter, BoxLayout.X_AXIS)); 
        westFooter.setOpaque(false);

        NutBoGoc btnChiTiet = new NutBoGoc("👁 Chi Tiết");
        btnChiTiet.setColorBackground(new Color(38, 170, 153)); 
        btnChiTiet.setPreferredSize(new Dimension(130, 35)); 
        btnChiTiet.setMaximumSize(new Dimension(130, 35));

        NutBoGoc btnTraHang = new NutBoGoc("↩ Trả Hàng");
        btnTraHang.setPreferredSize(new Dimension(130, 35)); 
        btnTraHang.setMaximumSize(new Dimension(130, 35));

        JLabel lblPaymentMethod = new JLabel("💳 Thanh toán: " + order.phuongThucTT);
        lblPaymentMethod.setFont(new Font("Calibri", Font.PLAIN, 14));
        lblPaymentMethod.setForeground(COLOR_TEXT_SUB);

        btnChiTiet.setAlignmentY(Component.CENTER_ALIGNMENT);
        btnTraHang.setAlignmentY(Component.CENTER_ALIGNMENT);
        lblPaymentMethod.setAlignmentY(Component.CENTER_ALIGNMENT);

        long soNgayCachBiet = 0;
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            java.time.LocalDate ngayLap = java.time.LocalDate.parse(order.ngayTao, formatter);
            soNgayCachBiet = java.time.temporal.ChronoUnit.DAYS.between(ngayLap, java.time.LocalDate.now());
        } catch (Exception ex) {
            soNgayCachBiet = 999; 
        }

        if (soNgayCachBiet > 7 || order.status.equalsIgnoreCase("Đã hủy") 
            || order.status.equalsIgnoreCase("Đã sửa đổi") 
            || order.status.equalsIgnoreCase("Đã trả hàng")) { 
            
            btnTraHang.setEnabled(false);
            btnTraHang.setColorBackground(new Color(200, 200, 200)); 
            btnTraHang.setToolTipText("Hóa đơn đã quá hạn hoặc không thể trả hàng nữa.");
            
            if (order.status.equalsIgnoreCase("Đã trả hàng")) {
                btnTraHang.setText("Đã Khóa"); 
            }
        } else {
            btnTraHang.setColorBackground(COLOR_PRIMARY); 
            btnTraHang.addActionListener(e -> {
                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
                java.util.List<Data.ChiTietHoaDon> dsCt = Dao.TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang().mapChiTietHD.get(order.maHD);
                new GUI.HoTro.PopupTraHang(topFrame, this, order.maHD, dsCt);
            });
        }

        btnChiTiet.addActionListener(e -> {
            Window topFrame = SwingUtilities.getWindowAncestor(this);
            JDialog dialog = new JDialog(topFrame, "Chi tiết hóa đơn", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setUndecorated(true);
            dialog.setBackground(new Color(0, 0, 0, 0)); 
            
            java.util.List<Data.ChiTietHoaDon> dsCt = Dao.TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang().mapChiTietHD.get(order.maHD);
            
            Object[][] items = new Object[dsCt.size()][5];
            for (int i = 0; i < dsCt.size(); i++) {
                Data.ChiTietHoaDon ct = dsCt.get(i);
                String tenSP = Dao.SanPhamDAO.getInstance().laySanPhamTheoMa(ct.getMaSp()).getTenSP();
                
                items[i][0] = tenSP;
                items[i][1] = ""; 
                items[i][2] = ct.getSoLuong();
                items[i][3] = ct.getDonGia();
                items[i][4] = ct.getThanhTienSanPham();
            }
            
            ChiTietHoaDonUi chiTietUi = new ChiTietHoaDonUi();
            chiTietUi.setPreferredSize(new Dimension(650, 800));
            boolean isTienMat = order.phuongThucTT.equalsIgnoreCase("Tiền mặt");
            chiTietUi.setDuLieuHoaDon(
                order.maHD, order.tenNhanVien, order.customerName, order.customerTier, 
                order.khachDua, items, order.tongGiamGia, new java.math.BigDecimal("0"), 0, isTienMat
            );
            
            JPanel pnlContainer = new JPanel(new BorderLayout());
            pnlContainer.setOpaque(false);
            pnlContainer.add(chiTietUi, BorderLayout.CENTER);
            
            NutBoGoc btnDong = new NutBoGoc("Đóng");
            btnDong.setColorBackground(new Color(220, 53, 69)); 
            btnDong.addActionListener(closeEvent -> dialog.dispose());
            
            JPanel pnlDong = new JPanel(new FlowLayout(FlowLayout.CENTER));
            pnlDong.setOpaque(false);
            pnlDong.setBorder(new EmptyBorder(15, 0, 0, 0));
            pnlDong.add(btnDong);
            
            pnlContainer.add(pnlDong, BorderLayout.SOUTH);
            
            JPanel pnlGlass = new JPanel(new GridBagLayout());
            pnlGlass.setBackground(new Color(0, 0, 0, 150)); 
            pnlGlass.setBorder(new EmptyBorder(20, 20, 20, 20));
            pnlGlass.add(pnlContainer);
            
            dialog.setContentPane(pnlGlass);
            dialog.setSize(topFrame.getSize()); 
            dialog.setLocationRelativeTo(topFrame);
            dialog.setVisible(true);
        });

        westFooter.add(btnChiTiet);
        westFooter.add(Box.createRigidArea(new Dimension(15, 0))); 
        westFooter.add(btnTraHang);
        westFooter.add(Box.createRigidArea(new Dimension(25, 0))); 
        westFooter.add(lblPaymentMethod);
        
        footerPanel.add(westFooter, BorderLayout.WEST);

        // --- Góc phải: Bảng thống kê tiền bạc ---
        JPanel eastFooter = new JPanel();
        eastFooter.setLayout(new BoxLayout(eastFooter, BoxLayout.Y_AXIS));
        eastFooter.setOpaque(false);

        JPanel summaryTable = new JPanel(new GridBagLayout());
        summaryTable.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.EAST; gbc.insets = new Insets(3, 30, 3, 0); 

        int rowIdx = 0;
        addSummaryRow(summaryTable, "Tổng tiền hàng:", DinhDangUtil.dinhDangTien(order.totalPrice), COLOR_TEXT_MAIN, new Font("Calibri", Font.PLAIN, 14), gbc, rowIdx++);
        addSummaryRow(summaryTable, "Tổng giảm giá:", "-" + DinhDangUtil.dinhDangTien(order.tongGiamGia), COLOR_DISCOUNT, new Font("Calibri", Font.PLAIN, 14), gbc, rowIdx++);
        addSummaryRow(summaryTable, "Khách đưa:", DinhDangUtil.dinhDangTien(order.khachDua), COLOR_TEXT_MAIN, new Font("Calibri", Font.PLAIN, 14), gbc, rowIdx++);
        addSummaryRow(summaryTable, "Tiền thừa:", DinhDangUtil.dinhDangTien(order.tienThua), COLOR_TEXT_SUB, new Font("Calibri", Font.PLAIN, 14), gbc, rowIdx++);
        
        gbc.insets = new Insets(10, 30, 5, 0); 
        BigDecimal thanhTien = order.totalPrice.subtract(order.tongGiamGia);
        addSummaryRow(summaryTable, "Thành tiền:", DinhDangUtil.dinhDangTien(thanhTien), COLOR_PRIMARY, new Font("Calibri", Font.BOLD, 19), gbc, rowIdx++);

        eastFooter.add(summaryTable);
        footerPanel.add(eastFooter, BorderLayout.EAST);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(bodyPanel, BorderLayout.CENTER);
        card.add(footerPanel, BorderLayout.SOUTH);

        return card;
    }
    private void addSummaryRow(JPanel panel, String label, String value, Color valColor, Font valFont, GridBagConstraints gbc, int row) {
        gbc.gridy = row; gbc.gridx = 0; gbc.weightx = 1.0;
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("Calibri", Font.PLAIN, 14)); lbl.setForeground(COLOR_TEXT_SUB); panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.0;
        JLabel val = new JLabel(value); val.setFont(valFont); val.setForeground(valColor); panel.add(val, gbc);
    }

    private JPanel createProductItem(SanPhamModel product) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false); panel.setBorder(new EmptyBorder(12, 15, 12, 15));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(0, 15, 0, 0); 

        JLabel lblImage = new JLabel("Ảnh", SwingConstants.CENTER);
        lblImage.setPreferredSize(new Dimension(60, 60)); lblImage.setBackground(new Color(245, 245, 245)); lblImage.setOpaque(true);
        lblImage.setForeground(COLOR_TEXT_SUB); lblImage.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 2; gbc.anchor = GridBagConstraints.CENTER; gbc.insets = new Insets(0, 0, 0, 0); panel.add(lblImage, gbc);

        JLabel lblName = new JLabel(product.name); lblName.setFont(new Font("Calibri", Font.PLAIN, 16)); lblName.setForeground(COLOR_TEXT_MAIN);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.SOUTHWEST; gbc.insets = new Insets(0, 15, 2, 0); panel.add(lblName, gbc);

        JLabel lblCategory = new JLabel(product.category); lblCategory.setFont(new Font("Calibri", Font.PLAIN, 13)); lblCategory.setForeground(COLOR_TEXT_SUB);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.NORTHWEST; gbc.insets = new Insets(2, 15, 0, 0); panel.add(lblCategory, gbc);

        JLabel lblPriceQty = new JLabel("<html>" + DinhDangUtil.dinhDangTien(product.price) + " &nbsp;&nbsp;&nbsp; <b>x" + product.quantity + "</b></html>");
        lblPriceQty.setFont(new Font("Calibri", Font.PLAIN, 15)); lblPriceQty.setForeground(COLOR_TEXT_MAIN);
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 2; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; panel.add(lblPriceQty, gbc);

        return panel;
    }

    class DonHangModel {
        String maHD, ngayTao, customerName, customerTier, tenNhanVien, status, phuongThucTT;
        BigDecimal tongGiamGia, khachDua, tienThua, totalPrice = BigDecimal.ZERO;
        List<SanPhamModel> items = new ArrayList<>();
        public DonHangModel(String maHD, String ngayTao, String cName, String cTier, String tNV, String stt, String pttt, String giamGia, String dua, String thoi) {
            this.maHD = maHD; this.ngayTao = ngayTao; this.customerName = cName; this.customerTier = cTier; this.tenNhanVien = tNV; this.status = stt; this.phuongThucTT = pttt;
            this.tongGiamGia = new BigDecimal(giamGia); this.khachDua = new BigDecimal(dua); this.tienThua = new BigDecimal(thoi);
        }
        public void addItem(SanPhamModel p) { items.add(p); totalPrice = totalPrice.add(p.price.multiply(BigDecimal.valueOf(p.quantity))); }
    }
    class SanPhamModel {
        String name, category; BigDecimal price; int quantity;
        public SanPhamModel(String name, String category, String price, int quantity) { this.name = name; this.category = category; this.price = new BigDecimal(price); this.quantity = quantity; }
    }
    
    // =========================================================================================
    // 🌟 PHÉP THUẬT FRONT-END: LỊCH XỔ XUỐNG TỐI GIẢN (Cùng tông Cam đỏ)
    // =========================================================================================
    class LichXoXuongCustom extends JPanel {
        private final JLabel lblNgayHienThi;
        private final JButton btnMoLich;
        private final JPopupMenu popupLich;
        
        private LocalDate ngayDangChon = null; 
        private LocalDate thangHienTaiCuaLich = LocalDate.now();
        
        private final JPanel pnlLuoiNgay;
        private final JLabel lblThangNam;
        private Runnable hanhDongChonNgay;

        public LichXoXuongCustom() {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

            lblNgayHienThi = new JLabel("Ngày: Tất cả", SwingConstants.CENTER);
            lblNgayHienThi.setFont(new Font("Calibri", Font.PLAIN, 15));
            lblNgayHienThi.setForeground(COLOR_TEXT_SUB);
            lblNgayHienThi.setOpaque(true);
            lblNgayHienThi.setBackground(Color.WHITE);
            
            btnMoLich = new JButton("📅");
            btnMoLich.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
            btnMoLich.setFocusPainted(false);
            btnMoLich.setBackground(new Color(245, 245, 245));
            btnMoLich.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, COLOR_BORDER));
            btnMoLich.setCursor(new Cursor(Cursor.HAND_CURSOR));

            add(lblNgayHienThi, BorderLayout.CENTER);
            add(btnMoLich, BorderLayout.EAST);

            popupLich = new JPopupMenu();
            popupLich.setLayout(new BorderLayout());
            popupLich.setBackground(Color.WHITE);
            popupLich.setBorder(BorderFactory.createLineBorder(COLOR_PRIMARY, 1));

            JPanel pnlHeaderLich = new JPanel(new BorderLayout());
            pnlHeaderLich.setBackground(COLOR_PRIMARY);
            
            JButton btnTruoc = taoNutChuyenThang("<");
            JButton btnSau = taoNutChuyenThang(">");
            lblThangNam = new JLabel("", SwingConstants.CENTER);
            lblThangNam.setForeground(Color.WHITE);
            lblThangNam.setFont(new Font("Calibri", Font.BOLD, 15));
            
            btnTruoc.addActionListener(e -> { thangHienTaiCuaLich = thangHienTaiCuaLich.minusMonths(1); veLich(); });
            btnSau.addActionListener(e -> { thangHienTaiCuaLich = thangHienTaiCuaLich.plusMonths(1); veLich(); });
            
            pnlHeaderLich.add(btnTruoc, BorderLayout.WEST);
            pnlHeaderLich.add(lblThangNam, BorderLayout.CENTER);
            pnlHeaderLich.add(btnSau, BorderLayout.EAST);

            JPanel pnlBodyLich = new JPanel(new BorderLayout());
            pnlBodyLich.setBackground(Color.WHITE);
            
            JPanel pnlThu = new JPanel(new GridLayout(1, 7));
            pnlThu.setBackground(Color.WHITE);
            String[] thuArray = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (String thu : thuArray) {
                JLabel lblThu = new JLabel(thu, SwingConstants.CENTER);
                lblThu.setFont(new Font("Calibri", Font.BOLD, 13));
                lblThu.setForeground(COLOR_TEXT_SUB);
                pnlThu.add(lblThu);
            }
            
            pnlLuoiNgay = new JPanel(new GridLayout(0, 7, 2, 2));
            pnlLuoiNgay.setBackground(Color.WHITE);
            pnlLuoiNgay.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            pnlBodyLich.add(pnlThu, BorderLayout.NORTH);
            pnlBodyLich.add(pnlLuoiNgay, BorderLayout.CENTER);

            popupLich.add(pnlHeaderLich, BorderLayout.NORTH);
            popupLich.add(pnlBodyLich, BorderLayout.CENTER);

            btnMoLich.addActionListener(e -> {
                veLich();
                popupLich.show(this, 0, getHeight());
            });
        }

        private JButton taoNutChuyenThang(String text) {
            JButton btn = new JButton(text);
            btn.setForeground(Color.WHITE);
            btn.setBackground(COLOR_PRIMARY);
            btn.setBorder(new EmptyBorder(5, 10, 5, 10));
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
        }

        private void veLich() {
            pnlLuoiNgay.removeAll();
            lblThangNam.setText("Tháng " + thangHienTaiCuaLich.getMonthValue() + " - " + thangHienTaiCuaLich.getYear());
            
            YearMonth yearMonth = YearMonth.from(thangHienTaiCuaLich);
            int soNgayTrongThang = yearMonth.lengthOfMonth();
            LocalDate ngayDauThang = thangHienTaiCuaLich.withDayOfMonth(1);
            int thuCuaNgayDauThang = ngayDauThang.getDayOfWeek().getValue(); 
            
            for (int i = 1; i < thuCuaNgayDauThang; i++) {
                pnlLuoiNgay.add(new JLabel(""));
            }
            
            for (int i = 1; i <= soNgayTrongThang; i++) {
                final int ngay = i;
                JButton btnNgay = new JButton(String.valueOf(ngay));
                btnNgay.setFont(new Font("Calibri", Font.PLAIN, 13));
                btnNgay.setFocusPainted(false);
                btnNgay.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                btnNgay.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                if (ngayDangChon != null && ngay == ngayDangChon.getDayOfMonth() && 
                    thangHienTaiCuaLich.getMonth() == ngayDangChon.getMonth() && 
                    thangHienTaiCuaLich.getYear() == ngayDangChon.getYear()) {
                    btnNgay.setBackground(COLOR_PRIMARY);
                    btnNgay.setForeground(Color.WHITE);
                } else {
                    btnNgay.setBackground(COLOR_HOVER);
                    btnNgay.setForeground(COLOR_TEXT_MAIN);
                }
                
                btnNgay.addActionListener(e -> {
                    setNgay(thangHienTaiCuaLich.withDayOfMonth(ngay));
                    popupLich.setVisible(false);
                });
                pnlLuoiNgay.add(btnNgay);
            }
            pnlLuoiNgay.revalidate();
            pnlLuoiNgay.repaint();
        }

        public LocalDate getNgay() { return ngayDangChon; }

        public void setNgay(LocalDate ngay) {
            this.ngayDangChon = ngay;
            if (ngay == null) {
                this.thangHienTaiCuaLich = LocalDate.now();
                lblNgayHienThi.setText("Ngày: Tất cả");
                lblNgayHienThi.setForeground(COLOR_TEXT_SUB);
            } else {
                this.thangHienTaiCuaLich = ngay;
                lblNgayHienThi.setText(ngayDangChon.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                lblNgayHienThi.setForeground(COLOR_PRIMARY); 
            }
            if (hanhDongChonNgay != null) hanhDongChonNgay.run();
        }

        public void setHanhDongChonNgay(Runnable r) { this.hanhDongChonNgay = r; }
    }
}