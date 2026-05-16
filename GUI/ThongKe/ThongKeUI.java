package GUI.ThongKe;

import Dao.TruyVanSieuTocDAO;
import Logic.ThongKeLogic;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

/**
 * 📊 THONGKEUI.JAVA - THE HYBRID CONTROL CENTER & CORE ROUTER PANEL
 * Tích hợp công nghệ JLayeredPane để xếp lớp các Panel Swing đè lên trên nền HTML WebKit.
 */
public class ThongKeUI extends JPanel {

    private JLayeredPane layeredPane;
    private JFXPanel jfxPanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // 🔥 ĐÃ SỬA: Khai báo 5 Panel con chức năng Hybrid chuẩn
    private DoanhThuPanel doanhThuPanel;
    private KhoPanel khoPanel;
    private KhachHangPanel khachHangPanel;
    private NhanVienPanel nhanVienPanel;
    private SanPhamPanel sanPhamPanel; // Dùng SanPhamPanel chuẩn SaaS

    private WebEngine webEngine;
    private final Gson gson = new Gson();
    
    // Global filter state
    private int selectedYear = LocalDate.now().getYear();
    private int selectedMonth = LocalDate.now().getMonthValue();
    private String selectedCategoryFilter = "ALL";
    private String currentActiveTab = "OVERVIEW";
    
    private String lastCompiledGlobalOverviewJson = null;

    public ThongKeUI() {
        setName("ThongKeUI");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        initSwingComponents();
        initJavaFXBridge();
    }

    private void initSwingComponents() {
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        jfxPanel = new JFXPanel();
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false); // Trong suốt để nhìn thấu xuống HTML bên dưới

        // 1. Khởi tạo Panel trống cho trang Tổng Quan
        JPanel emptyPanel = new JPanel();
        emptyPanel.setOpaque(false);
        cardPanel.add(emptyPanel, "OVERVIEW");

        // 2. Khởi tạo an toàn 5 Panel chức năng Hybrid
        try { doanhThuPanel = new DoanhThuPanel(); cardPanel.add(doanhThuPanel, "REVENUE"); } catch (Throwable t) {}
        try { khoPanel = new KhoPanel(); cardPanel.add(khoPanel, "WAREHOUSE"); } catch (Throwable t) {}
        try { khachHangPanel = new KhachHangPanel(); cardPanel.add(khachHangPanel, "CUSTOMER"); } catch (Throwable t) {}
        try { nhanVienPanel = new NhanVienPanel(); cardPanel.add(nhanVienPanel, "STAFF"); } catch (Throwable t) {}
        try { sanPhamPanel = new SanPhamPanel(); cardPanel.add(sanPhamPanel, "PRODUCT"); } catch (Throwable t) {} // Sửa thành SanPhamPanel

        // 3. Xếp lớp: JFXPanel (HTML) nằm dưới cùng [0], CardPanel (Swing) nằm đè lên trên [1]
        layeredPane.add(jfxPanel, Integer.valueOf(0));
        layeredPane.add(cardPanel, Integer.valueOf(1));

        // 4. Thuật toán tự động resize: Giam CardPanel cách lề trái đúng 280px (Vừa bằng thanh Sidebar)
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                jfxPanel.setBounds(0, 0, w, h);
                cardPanel.setBounds(280, 0, w - 280, h); // Phủ hoàn hảo lên vùng bên phải
            }
        });
    }

    private void initJavaFXBridge() {
        Platform.runLater(() -> {
            WebView webView = new WebView();
            webEngine = webView.getEngine();
            webEngine.setJavaScriptEnabled(true);

            // Bắt sự kiện chuyển Tab từ HTML
            webEngine.setOnAlert(event -> {
                String rawSignal = event.getData();
                if (rawSignal != null) {
                    if (rawSignal.startsWith("TAB_SWITCH:")) {
                        String targetTabId = rawSignal.substring(11);
                        this.currentActiveTab = targetTabId;
                        
                        // Đảo trang trên Java Swing lập tức
                        SwingUtilities.invokeLater(() -> {
                            cardLayout.show(cardPanel, targetTabId);
                            // 🔥 ĐÃ SỬA: Gọi hàm làm mới dữ liệu của SanPhamPanel
                            try {
                                if ("REVENUE".equals(targetTabId) && doanhThuPanel != null) doanhThuPanel.pushLiveAnalyticsData(false);
                                else if ("WAREHOUSE".equals(targetTabId) && khoPanel != null) khoPanel.refreshDashboardData(false);
                                else if ("CUSTOMER".equals(targetTabId) && khachHangPanel != null) khachHangPanel.pushLiveCustomerAnalytics(false);
                                else if ("STAFF".equals(targetTabId) && nhanVienPanel != null) nhanVienPanel.pushLiveWorkforceAnalytics(false);
                                else if ("PRODUCT".equals(targetTabId) && sanPhamPanel != null) sanPhamPanel.pushProductDashboardData(false);
                                else if ("OVERVIEW".equals(targetTabId)) synchronizeControlCenterMetrics(false);
                            } catch(Exception ex) {
                                System.out.println("Lỗi làm mới sub-panel: " + ex.getMessage());
                            }
                        });

                    } else if (rawSignal.startsWith("FILTER_CHANGE:")) {
                        parseIncomingFilterToken(rawSignal.substring(14));
                    } else if (rawSignal.startsWith("ACTION:")) {
                        handleGlobalActionIntercepts(rawSignal.substring(7));
                    }
                }
            });

            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    synchronizeControlCenterMetrics(false);
                }
            });

            try {
                URL htmlUrl = getClass().getResource("thongke_dashboard.html");
                if (htmlUrl != null) webEngine.load(htmlUrl.toExternalForm());
            } catch (Exception e) {}

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }

    public void synchronizeControlCenterMetrics(boolean forceRefresh) {
        if (!forceRefresh && lastCompiledGlobalOverviewJson != null) {
            executeJavaScriptCall("updateGlobalControlCenter(" + lastCompiledGlobalOverviewJson + ")");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject dataset = new JsonObject();
                dataset.addProperty("currentTab", currentActiveTab);
                dataset.addProperty("filterMonth", selectedMonth);
                dataset.addProperty("filterYear", selectedYear);
                dataset.addProperty("filterCategory", selectedCategoryFilter);

                TruyVanSieuTocDAO.DuLieuDonHangDTO ordersDTO = TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO inventoryDTO = TruyVanSieuTocDAO.getInstance().loadDuLieuKiemKeSieuToc();
                ThongKeLogic thongKeLogic = new ThongKeLogic();

                BigDecimal[] monthlyMetrics = thongKeLogic.thongKeLoiNhuanThang(selectedMonth, selectedYear);
                dataset.addProperty("totalRevenue", monthlyMetrics[0] != null ? monthlyMetrics[0] : BigDecimal.ZERO);
                dataset.addProperty("totalLosses", monthlyMetrics[2] != null ? monthlyMetrics[2] : BigDecimal.ZERO);
                dataset.addProperty("netProfits", monthlyMetrics[3] != null ? monthlyMetrics[3] : BigDecimal.ZERO);

                int invoicesCount = 0;
                Map<String, BigDecimal> customerSpendingMap = new HashMap<>();
                Map<String, Integer> productQuantitiesSoldMap = new HashMap<>();

                if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                    for (var bill : ordersDTO.dsHoaDon) {
                        if (bill.getNgayTao() != null && bill.getNgayTao().getMonthValue() == selectedMonth && bill.getNgayTao().getYear() == selectedYear) {
                            invoicesCount++;
                            if (bill.getMaKH() != null && bill.getThanhTien() != null) {
                                customerSpendingMap.put(bill.getMaKH().trim(), customerSpendingMap.getOrDefault(bill.getMaKH().trim(), BigDecimal.ZERO).add(bill.getThanhTien()));
                            }
                            var details = ordersDTO.mapChiTietHD.get(bill.getMaHD());
                            if (details != null) {
                                for (var ct : details) {
                                    productQuantitiesSoldMap.put(ct.getMaSp(), productQuantitiesSoldMap.getOrDefault(ct.getMaSp(), 0) + ct.getSoLuong());
                                }
                            }
                        }
                    }
                }
                dataset.addProperty("totalInvoices", invoicesCount);

                int lowStockCount = 0;
                if (inventoryDTO != null && inventoryDTO.mapTongTonKho != null) {
                    for (int val : inventoryDTO.mapTongTonKho.values()) if (val <= 10) lowStockCount++;
                }
                dataset.addProperty("lowStockAlertsCount", lowStockCount);

                JsonArray topProductsArray = new JsonArray();
                productQuantitiesSoldMap.entrySet().stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).limit(5).forEach(entry -> {
                    JsonObject row = new JsonObject();
                    row.addProperty("name", (ordersDTO != null && ordersDTO.mapSanPham.containsKey(entry.getKey())) ? ordersDTO.mapSanPham.get(entry.getKey())[0] : entry.getKey());
                    row.addProperty("value", entry.getValue());
                    topProductsArray.add(row);
                });
                dataset.add("topMovingProducts", topProductsArray);

                JsonArray topCustomersArray = new JsonArray();
                customerSpendingMap.entrySet().stream().sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).limit(5).forEach(entry -> {
                    JsonObject row = new JsonObject();
                    row.addProperty("name", (ordersDTO != null && ordersDTO.mapKhachHang.containsKey(entry.getKey())) ? ordersDTO.mapKhachHang.get(entry.getKey())[0] : "Hội viên " + entry.getKey());
                    row.addProperty("spent", entry.getValue());
                    topCustomersArray.add(row);
                });
                dataset.add("topLoyalCustomers", topCustomersArray);

                BigDecimal[] annualTrendLine = new BigDecimal[12];
                for (int i = 0; i < 12; i++) annualTrendLine[i] = BigDecimal.ZERO;
                if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                    for (var bill : ordersDTO.dsHoaDon) {
                        if (bill.getNgayTao() != null && bill.getNgayTao().getYear() == selectedYear && bill.getThanhTien() != null) {
                            int monthIdx = bill.getNgayTao().getMonthValue() - 1;
                            annualTrendLine[monthIdx] = annualTrendLine[monthIdx].add(bill.getThanhTien());
                        }
                    }
                }
                JsonArray chronologicalSalesSeries = new JsonArray();
                for (BigDecimal val : annualTrendLine) chronologicalSalesSeries.add(val);
                dataset.add("annualRevenueTrend", chronologicalSalesSeries);

                lastCompiledGlobalOverviewJson = gson.toJson(dataset);
                return lastCompiledGlobalOverviewJson;
            } catch (Throwable e) { return null; }
        }).thenAccept(json -> {
            if (json != null) executeJavaScriptCall("updateGlobalControlCenter(" + json + ")");
        });
    }

    private void parseIncomingFilterToken(String tokenData) {
        try {
            String[] clusters = tokenData.split("&");
            for (String splitPair : clusters) {
                String[] elements = splitPair.split("=");
                if (elements.length == 2) {
                    if ("month".equalsIgnoreCase(elements[0])) this.selectedMonth = Integer.parseInt(elements[1]);
                    else if ("year".equalsIgnoreCase(elements[0])) this.selectedYear = Integer.parseInt(elements[1]);
                    else if ("category".equalsIgnoreCase(elements[0])) this.selectedCategoryFilter = elements[1];
                }
            }
            synchronizeControlCenterMetrics(true);
        } catch (Exception e) {}
    }

    private void handleGlobalActionIntercepts(String actionCommand) {
        SwingUtilities.invokeLater(() -> {
            if ("GLOBAL_REFRESH".equalsIgnoreCase(actionCommand)) {
                synchronizeControlCenterMetrics(true);
                JOptionPane.showMessageDialog(this, "Hệ thống đã làm mới dữ liệu thống kê toàn diện!", "Đồng Bộ Hệ Thống", JOptionPane.INFORMATION_MESSAGE);
            } else if ("EXPORT_REPORT".equalsIgnoreCase(actionCommand)) {
                JOptionPane.showMessageDialog(this, "Đang khởi động tác vụ nền: Trích xuất Báo Cáo Tài Chính Tổng Hợp ra file Excel...", "Xuất Báo Cáo", JOptionPane.INFORMATION_MESSAGE);
            } else if ("FULLSCREEN".equalsIgnoreCase(actionCommand)) {
                Window parentFrame = SwingUtilities.getWindowAncestor(this);
                if (parentFrame instanceof JFrame) {
                    JFrame jf = (JFrame) parentFrame;
                    jf.setExtendedState(jf.getExtendedState() == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
                }
            }
        });
    }

    private void executeJavaScriptCall(String scriptText) {
        Platform.runLater(() -> { try { if (webEngine != null) webEngine.executeScript(scriptText); } catch (Exception e) {} });
    }
    public interface ThongKeUiRouterCallback {
        void dieuHuongTrang(String maTrang);
    }
    private ThongKeUiRouterCallback routerCallback;

    public void setRouterCallback(ThongKeUiRouterCallback cb) {
        this.routerCallback = cb;
        
        // 1. Nối dây thần kinh xuống Nhân Viên
        if (nhanVienPanel != null) {
            nhanVienPanel.setCallback(new NhanVienPanel.NhanVienPanelCallback() {
                @Override
                public void moThemNhanVien() { if (routerCallback != null) routerCallback.dieuHuongTrang("NHAN_VIEN"); }
                @Override
                public void moPhanCa() { if (routerCallback != null) routerCallback.dieuHuongTrang("CA_LAM"); }
            });
        }
        
        // 2. Nối dây thần kinh xuống KhoPanel (Để giữ lại tính năng của khoPanel cũ)
        try {
            if (khoPanel != null) {
                khoPanel.setCallback(new KhoPanel.KhoPanelCallback() {
                    @Override
                    public void moNhapHang() { if (routerCallback != null) routerCallback.dieuHuongTrang("NHAP_HANG_MODULE"); }
                    @Override
                    public void moKiemKe() { if (routerCallback != null) routerCallback.dieuHuongTrang("KIEM_KE"); }
                });
            }
        } catch (Throwable t) {}
    }
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Trung Tâm Phân Tích Hệ Thống Tổng Hợp");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1500, 860);
            frame.setMinimumSize(new Dimension(1150, 700));
            frame.setLocationRelativeTo(null);
            frame.add(new ThongKeUI(), BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
}