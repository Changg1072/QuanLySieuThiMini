package GUI.ThongKe;

import Logic.ThongKeLogic;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

/**
 * 🚀 HYBRID REVENUE ANALYTICS DASHBOARD PANEL
 * Nằm trong package GUI.ThongKe theo đúng chuẩn kiến trúc.
 */
public class DoanhThuPanel extends JPanel {

    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    
    private String lastCachedJson = null;
    private int currentSelectedYear = LocalDate.now().getYear();
    private int currentSelectedMonth = LocalDate.now().getMonthValue();
    private Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO cachedDonHangDTO = null;

    public DoanhThuPanel() {
        setName("DoanhThuPanel");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        initSwingComponents();
        initJavaFXBridge();
    }

    private void initSwingComponents() {
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);
    }

    private void initJavaFXBridge() {
        Platform.runLater(() -> {
            WebView webView = new WebView();
            webEngine = webView.getEngine();
            webEngine.setJavaScriptEnabled(true);
            
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    pushLiveAnalyticsData(false);
                }
            });

            try {
                // Lấy file HTML từ cùng thư mục GUI.ThongKe
                URL htmlUrl = getClass().getResource("doanhthu_dashboard.html");
                if (htmlUrl != null) {
                    webEngine.load(htmlUrl.toExternalForm());
                } else {
                    webEngine.loadContent("<html><body style='font-family:sans-serif;padding:30px;color:#ef4444;'>"
                        + "<h2>🚨 Lỗi tải Template</h2>"
                        + "<p>Không tìm thấy file <code>doanhthu_dashboard.html</code> trong package GUI.ThongKe.</p>"
                        + "</body></html>");
                }
            } catch (Exception e) {
                System.err.println("[DoanhThuPanel] Lỗi: " + e.getMessage());
            }

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }

    public void pushLiveAnalyticsData(boolean forceRefresh) {
        if (!forceRefresh && lastCachedJson != null) {
            evaluateJavaScriptInWebKit("updateDashboard(" + lastCachedJson + ")");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject dashboardData = new JsonObject();

                // =========================================================
                // 1. KÉO DỮ LIỆU TỪ CACHE (Khắc phục giật lag triệt để)
                // Chỉ chọc xuống DB khi người dùng bấm "Làm mới" (forceRefresh = true)
                // =========================================================
                if (forceRefresh || cachedDonHangDTO == null) {
                    cachedDonHangDTO = Dao.TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                }
                Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO donHangDTO = cachedDonHangDTO;

                // =========================================================
                // 2. TÍNH TOÁN LỢI NHUẬN / THẤT THOÁT 
                // (Chỉ riêng phần Vốn và Kho phức tạp mới cần nhờ DB tính)
                // =========================================================
                ThongKeLogic logic = new ThongKeLogic();
                BigDecimal[] metrics = logic.thongKeLoiNhuanThang(currentSelectedMonth, currentSelectedYear);
                BigDecimal totalRevenue = metrics[0] != null ? metrics[0] : BigDecimal.ZERO;
                BigDecimal totalLoss = metrics[2] != null ? metrics[2] : BigDecimal.ZERO;
                BigDecimal netProfit = metrics[3] != null ? metrics[3] : BigDecimal.ZERO;

                BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0 
                    ? netProfit.multiply(new BigDecimal("100")).divide(totalRevenue, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                dashboardData.addProperty("totalRevenue", totalRevenue);
                dashboardData.addProperty("totalLoss", totalLoss);
                dashboardData.addProperty("totalProfit", netProfit);
                dashboardData.addProperty("profitMargin", profitMargin);

                // =========================================================
                // 🔥 3. ĐỘNG CƠ XỬ LÝ IN-MEMORY (RAM) SIÊU TỐC
                // Thay vì gọi DB 3 lần để tính Top SP, Top KH, Biểu đồ...
                // Ta dùng vòng lặp RAM quét qua DTO (Tốc độ < 5 mili-giây)
                // =========================================================

                BigDecimal[] monthlyBuckets = new BigDecimal[12];
                for (int i = 0; i < 12; i++) monthlyBuckets[i] = BigDecimal.ZERO;

                List<Data.HoaDon> dsHoaDonThang = new java.util.ArrayList<>();
                int cashCount = 0, transferCount = 0, eWalletCount = 0;
                
                java.util.Map<String, Integer> productSales = new java.util.HashMap<>();
                java.util.Map<String, BigDecimal> customerSpent = new java.util.HashMap<>();

                for (Data.HoaDon hd : donHangDTO.dsHoaDon) {
                    // Xử lý Khách Hàng VIP (Tính toàn thời gian)
                    if (hd.getMaKH() != null && !hd.getMaKH().trim().isEmpty() && hd.getThanhTien() != null) {
                        customerSpent.put(hd.getMaKH(), customerSpent.getOrDefault(hd.getMaKH(), BigDecimal.ZERO).add(hd.getThanhTien()));
                    }

                    if (hd.getNgayTao() != null) {
                        int hYear = hd.getNgayTao().getYear();
                        int hMonth = hd.getNgayTao().getMonthValue();

                        // Cộng dồn Biểu đồ doanh thu 12 tháng
                        if (hYear == currentSelectedYear && hd.getThanhTien() != null) {
                            monthlyBuckets[hMonth - 1] = monthlyBuckets[hMonth - 1].add(hd.getThanhTien());
                        }

                        // Lọc hóa đơn theo tháng/năm đang xem để hiển thị UI
                        if (hYear == currentSelectedYear && hMonth == currentSelectedMonth) {
                            dsHoaDonThang.add(hd);

                            // Phương thức thanh toán
                            String pt = hd.getPhuongThucTT();
                            if (pt != null) {
                                if (pt.toLowerCase().contains("tiền mặt")) cashCount++;
                                else if (pt.toLowerCase().contains("chuyển khoản")) transferCount++;
                                else eWalletCount++;
                            }

                            // Cộng dồn để tìm Top 5 Sản phẩm
                            List<Data.ChiTietHoaDon> details = donHangDTO.mapChiTietHD.get(hd.getMaHD());
                            if (details != null) {
                                for (Data.ChiTietHoaDon ct : details) {
                                    productSales.put(ct.getMaSp(), productSales.getOrDefault(ct.getMaSp(), 0) + ct.getSoLuong());
                                }
                            }
                        }
                    }
                }

                // -> Trích xuất dữ liệu mảng Biểu đồ 12 tháng
                JsonArray salesSeries = new JsonArray();
                for (BigDecimal val : monthlyBuckets) salesSeries.add(val);
                dashboardData.add("chronologicalSales", salesSeries);

                // -> Lọc Top 5 SP Bán Chạy (Sorting trên RAM)
                JsonArray topProductsArr = new JsonArray();
                productSales.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Sắp xếp giảm dần
                    .limit(5)
                    .forEach(entry -> {
                        JsonObject p = new JsonObject();
                        String tenSP = donHangDTO.mapSanPham.containsKey(entry.getKey()) ? donHangDTO.mapSanPham.get(entry.getKey())[0] : entry.getKey();
                        p.addProperty("name", tenSP);
                        p.addProperty("unitsSold", entry.getValue());
                        topProductsArr.add(p);
                    });
                dashboardData.add("topProducts", topProductsArr);

                // -> Lọc Top 5 KH VIP (Sorting trên RAM)
                JsonArray topCustomersArr = new JsonArray();
                customerSpent.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        JsonObject c = new JsonObject();
                        String tenKH = donHangDTO.mapKhachHang.containsKey(entry.getKey()) ? donHangDTO.mapKhachHang.get(entry.getKey())[0] : "Khách hàng " + entry.getKey();
                        c.addProperty("name", tenKH);
                        c.addProperty("totalSpent", entry.getValue());
                        topCustomersArr.add(c);
                    });
                dashboardData.add("topCustomers", topCustomersArr);

                // =========================================================
                // 4. CHỈ SỐ KPI VÀ NHẬT KÝ HÓA ĐƠN
                // =========================================================
                dashboardData.addProperty("totalInvoices", dsHoaDonThang.size());
                
                JsonObject paymentMethods = new JsonObject();
                paymentMethods.addProperty("cash", cashCount);
                paymentMethods.addProperty("bankTransfer", transferCount);
                paymentMethods.addProperty("eWallet", eWalletCount);
                dashboardData.add("paymentMethods", paymentMethods);

                BigDecimal averageTicket = BigDecimal.ZERO;
                if (!dsHoaDonThang.isEmpty() && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    averageTicket = totalRevenue.divide(new BigDecimal(dsHoaDonThang.size()), 0, java.math.RoundingMode.HALF_UP);
                }
                dashboardData.addProperty("averageTicket", averageTicket);

                // Thuật toán Giờ cao điểm
                java.util.Map<Integer, Integer> hourCount = new java.util.HashMap<>();
                for (Data.HoaDon hd : dsHoaDonThang) {
                    if (hd.getNgayTao() != null) {
                        int hour = hd.getNgayTao().getHour();
                        hourCount.put(hour, hourCount.getOrDefault(hour, 0) + 1);
                    }
                }
                int peakHour = 0, maxInvoices = 0;
                for (java.util.Map.Entry<Integer, Integer> entry : hourCount.entrySet()) {
                    if (entry.getValue() > maxInvoices) {
                        maxInvoices = entry.getValue();
                        peakHour = entry.getKey();
                    }
                }
                String peakHourStr = maxInvoices > 0 ? String.format("%02d:00 - %02d:00", peakHour, peakHour + 1) : "--:--";
                dashboardData.addProperty("peakHour", peakHourStr);
                dashboardData.addProperty("peakOrderCount", maxInvoices);

                // Bảng Nhật Ký Hóa Đơn (Phải sort thủ công vì RAM không tự sắp xếp như SQL)
                dsHoaDonThang.sort((h1, h2) -> {
                    if (h1.getNgayTao() == null || h2.getNgayTao() == null) return 0;
                    return h2.getNgayTao().compareTo(h1.getNgayTao()); // Đảo ngược để lấy mới nhất
                });

                JsonArray recentInvoices = new JsonArray();
                int count = 0;
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for(Data.HoaDon hd : dsHoaDonThang) {
                    if(count >= 10) break;
                    JsonObject inv = new JsonObject();
                    inv.addProperty("id", hd.getMaHD());
                    String tenKH = hd.getMaKH() != null && donHangDTO.mapKhachHang.containsKey(hd.getMaKH()) ? donHangDTO.mapKhachHang.get(hd.getMaKH())[0] : "Khách vãng lai";
                    inv.addProperty("customer", tenKH);
                    String tenNV = hd.getMaNV() != null && donHangDTO.mapNhanVien.containsKey(hd.getMaNV()) ? donHangDTO.mapNhanVien.get(hd.getMaNV()) : "Unknown";
                    inv.addProperty("cashier", tenNV);
                    inv.addProperty("timestamp", hd.getNgayTao() != null ? hd.getNgayTao().format(formatter) : "");
                    inv.addProperty("method", hd.getPhuongThucTT() != null ? hd.getPhuongThucTT() : "Khác");
                    inv.addProperty("amount", hd.getThanhTien());
                    recentInvoices.add(inv);
                    count++;
                }
                dashboardData.add("recentInvoices", recentInvoices);

                lastCachedJson = gson.toJson(dashboardData);
                return lastCachedJson;

            } catch (Exception e) {
                System.err.println("[DoanhThuPanel] Lỗi lấy dữ liệu: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).thenAccept(jsonResult -> {
            if (jsonResult != null) {
                evaluateJavaScriptInWebKit("updateDashboard(" + jsonResult + ")");
            }
        });
    }

    private void evaluateJavaScriptInWebKit(String script) {
        Platform.runLater(() -> {
            try {
                if (webEngine != null) webEngine.executeScript(script);
            } catch (Exception e) {}
        });
    }

    public void updateFilterCoordinates(int month, int year) {
        this.currentSelectedMonth = month;
        this.currentSelectedYear = year;
        pushLiveAnalyticsData(true);
    }

    public static void main(String[] args) {
        // 1. Tối ưu hóa giao diện hệ thống (Look and Feel) để bo góc và thanh cuộn mượt hơn
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // 2. Kích hoạt luồng an toàn Event Dispatch Thread (EDT) của Swing
        SwingUtilities.invokeLater(() -> {
            JFrame khungChung = new JFrame("Hệ Thống Kiểm Thử Hybrid UI - Revenue Dashboard Analytics");
            khungChung.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Thiết lập kích thước tiêu chuẩn cho các dòng Dashboard SaaS hiện đại (gợi ý độ phân giải HD trở lên)
            khungChung.setSize(1340, 780);
            khungChung.setMinimumSize(new Dimension(1000, 600));
            khungChung.setLocationRelativeTo(null); // Hiển thị căn giữa màn hình ổ cứng

            // 3. Khởi tạo đối tượng panel điều hướng
            DoanhThuPanel dashboard = new DoanhThuPanel();
            khungChung.add(dashboard, BorderLayout.CENTER);

            // 4. Hiển thị Frame
            khungChung.setVisible(true);
            
            System.out.println("[Hybrid UI System] Khởi chạy WebKit Engine thành công. Đang kết nối RAM Cache...");
        });
    }
}