package GUI.ThongKe;

import javax.swing.*;
import java.awt.*;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import java.net.URL;

public class ThongKePanel extends JPanel {

    private CardLayout cardLayout;
    private JPanel centerContentPanel;
    private JFXPanel sidebarJFXPanel;
    private WebEngine sidebarEngine;

    // 🔥 TẠO BIẾN GIỮ CHÂN CẦU NỐI ĐỂ JAVA KHÔNG XÓA (SỬA LỖI KHÔNG BẤM ĐƯỢC MENU)
    private SidebarBridge javaConnectorBridge;

    // 5 Module thống kê
    private DoanhThuPanel doanhThuPanel;
    private KhachHangPanel khachHangPanel;
    private KhoPanel khoPanel;
    private NhanVienPanel nhanVienPanel;
    private SanPhamPanel sanPhamPanel;

    public ThongKePanel() {
        setName("ThongKePanel");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        initComponents();
    }

    private void initComponents() {
        // ==========================================
        // 1. KHỞI TẠO KHU VỰC HIỂN THỊ CHÍNH (CENTER)
        // ==========================================
        cardLayout = new CardLayout();
        centerContentPanel = new JPanel(cardLayout);
        centerContentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Nạp 5 Panel vào bộ nhớ
        doanhThuPanel = new DoanhThuPanel();
        khoPanel = new KhoPanel();
        khachHangPanel = new KhachHangPanel();
        nhanVienPanel = new NhanVienPanel();
        sanPhamPanel = new SanPhamPanel();

        // Gắn Key nhận diện cho từng Panel ĐÚNG NHƯ TRÊN HTML GỬI XUỐNG
        centerContentPanel.add(doanhThuPanel, "DOANH_THU");
        centerContentPanel.add(khoPanel, "KHO");
        centerContentPanel.add(khachHangPanel, "KHACH_HANG");
        centerContentPanel.add(nhanVienPanel, "NHAN_VIEN");
        centerContentPanel.add(sanPhamPanel, "SAN_PHAM");

        add(centerContentPanel, BorderLayout.CENTER);

        // ==========================================
        // 2. KHỞI TẠO THANH SIDEBAR TỪ HTML/CSS
        // ==========================================
        sidebarJFXPanel = new JFXPanel();
        sidebarJFXPanel.setPreferredSize(new Dimension(280, 0)); // Cố định chiều rộng Sidebar 280px
        add(sidebarJFXPanel, BorderLayout.WEST);

        Platform.runLater(() -> {
            WebView webView = new WebView();
            sidebarEngine = webView.getEngine();
            sidebarEngine.setJavaScriptEnabled(true);

            // Bơm JavaConnector vào HTML khi trang tải xong
            sidebarEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    
                    // 🔥 KHỞI TẠO VÀ LƯU VÀO BIẾN TOÀN CỤC ĐỂ CHỐNG GARBAGE COLLECTOR
                    javaConnectorBridge = new SidebarBridge();
                    
                    JSObject window = (JSObject) sidebarEngine.executeScript("window");
                    window.setMember("javaConnector", javaConnectorBridge);
                }
            });

            // Tải file sidebar_menu.html
            try {
                URL htmlUrl = getClass().getResource("sidebar_menu.html");
                if (htmlUrl != null) {
                    sidebarEngine.load(htmlUrl.toExternalForm());
                } else {
                    sidebarEngine.loadContent("<html><body><h3>🚨 Lỗi tải Sidebar</h3></body></html>");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Scene scene = new Scene(webView);
            sidebarJFXPanel.setScene(scene);
        });
    }

    // ==========================================
    // CẦU NỐI KẾT NỐI TỪ CLICK JS SANG JAVA
    // ==========================================
    public class SidebarBridge {
        public void switchTab(String tabId) {
            System.out.println("🔄 Yêu cầu chuyển giao diện sang: " + tabId); // 🔥 Báo log để dễ Debug
            
            SwingUtilities.invokeLater(() -> {
                // Lật thẻ bằng CardLayout
                cardLayout.show(centerContentPanel, tabId);
                
                // (Tùy chọn) Reload data tự động khi lật trang
                if (tabId.equals("DOANH_THU")) {
                    doanhThuPanel.pushLiveAnalyticsData(false);
                } else if (tabId.equals("KHACH_HANG")) {
                    khachHangPanel.pushLiveCustomerAnalytics(false);
                } else if (tabId.equals("KHO")) {
                    khoPanel.refreshDashboardData(false);
                } else if (tabId.equals("NHAN_VIEN")) {
                    nhanVienPanel.pushLiveWorkforceAnalytics(false);
                } else if (tabId.equals("SAN_PHAM")) {
                    sanPhamPanel.pushProductDashboardData(false);
                }
            });
        }
    }
    

    // Lấy instance để kết nối form bên ngoài
    public KhoPanel getKhoPanel() { return khoPanel; }
    public NhanVienPanel getNhanVienPanel() { return nhanVienPanel; }

    // ==========================================
    // TEST ĐỘC LẬP
    // ==========================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hệ Thống Phân Tích Tổng Hợp");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1500, 850);
            frame.setLocationRelativeTo(null);
            frame.add(new ThongKePanel());
            frame.setVisible(true);
        });
    }
}
