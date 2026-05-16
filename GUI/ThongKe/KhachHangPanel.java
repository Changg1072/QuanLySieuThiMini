package GUI.ThongKe;

import Dao.KhachHangDAO;
import Dao.TruyVanSieuTocDAO;
import Logic.ThongKeLogic;
import Data.KhachHang;
//import Data.Layer.KhachHangCustom; // Fallback mapping verification
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

/**
 * 🚀 HYBRID CUSTOMER INTELLIGENCE & CRM ANALYTICS PANEL
 * Designed for standard enterprise Java Swing environments utilizing modern WebKit layout abstractions.
 * Leverages in-memory processing structures to prevent standard main UI thread blockages.
 */
public class KhachHangPanel extends JPanel {

    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    private String lastCachedJson = null;

    public KhachHangPanel() {
        setName("KhachHangPanel");
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

            // Interface signal hook listener to process events triggered inside Web DOM
            webEngine.setOnAlert(event -> {
                String signal = event.getData();
                if (signal != null && signal.startsWith("ACTION:")) {
                    processClientDomSignal(signal.substring(7));
                }
            });

            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    pushLiveCustomerAnalytics(false);
                }
            });

            try {
                URL htmlUrl = getClass().getResource("khachhang_dashboard.html");
                if (htmlUrl != null) {
                    webEngine.load(htmlUrl.toExternalForm());
                } else {
                    webEngine.loadContent("<html><body style='padding:30px;font-family:sans-serif;color:#ef4444;'>"
                            + "<h3>🚨 File Configuration Failure</h3>"
                            + "<p>Unable to compile or extract resource token path for <code>khachhang_dashboard.html</code>.</p>"
                            + "</body></html>");
                }
            } catch (Exception e) {
                System.err.println("[KhachHangPanel Bridge] Core startup exception: " + e.getMessage());
            }

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }

    /**
     * Aggregates relational client-side customer and transactional matrix indicators into a
     * fully compiled complex JSON package and injects it straight into WebKit memory boundaries.
     */
    public void pushLiveCustomerAnalytics(boolean forceRefresh) {
        if (!forceRefresh && lastCachedJson != null) {
            executeJavaScript("updateCustomerDashboard(" + lastCachedJson + ")");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject dataset = new JsonObject();
                
                // 1. Core Fetch Layers from Turbo DAO Engine and standard repositories
                List<KhachHang> rawCustomers = KhachHangDAO.getInstance().layDanhSachKhachHang();
                if (rawCustomers == null) rawCustomers = new ArrayList<>();

                TruyVanSieuTocDAO.DuLieuDonHangDTO ordersDTO = TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                
                int currentMonthValue = LocalDate.now().getMonthValue();
                int currentYearValue = LocalDate.now().getYear();

                // 2. Process KPI Targets
                int totalCustomers = rawCustomers.size();
                long newCustomersThisMonth = rawCustomers.stream()
                        .filter(c -> c.getNgayDangKy() != null 
                                && c.getNgayDangKy().getMonthValue() == currentMonthValue 
                                && c.getNgayDangKy().getYear() == currentYearValue)
                        .count();

                long vipCount = rawCustomers.stream()
                        .filter(c -> c.getBacKH() != null && (c.getBacKH().equalsIgnoreCase("Vip") || c.getBacKH().equalsIgnoreCase("Vàng")))
                        .count();

                BigDecimal totalSystemLoyaltyPoints = rawCustomers.stream()
                        .filter(c -> c.getDiemTichLuy() != null)
                        .map(KhachHang::getDiemTichLuy)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Compute Return Customer Rates accurately using transactional maps
                Map<String, Integer> customerOrderFrequencies = new HashMap<>();
                BigDecimal totalOrderValueSum = BigDecimal.ZERO;
                int totalInvoiceCount = 0;

                if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                    totalInvoiceCount = ordersDTO.dsHoaDon.size();
                    for (var bill : ordersDTO.dsHoaDon) {
                        if (bill.getMaKH() != null && !bill.getMaKH().trim().isEmpty()) {
                            customerOrderFrequencies.put(bill.getMaKH(), customerOrderFrequencies.getOrDefault(bill.getMaKH(), 0) + 1);
                        }
                        if (bill.getThanhTien() != null) {
                            totalOrderValueSum = totalOrderValueSum.add(bill.getThanhTien());
                        }
                    }
                }

                long returningCustomersCount = customerOrderFrequencies.values().stream().filter(count -> count >= 2).count();
                double returnCustomerRatePercentage = totalCustomers > 0 ? ((double) returningCustomersCount / totalCustomers) * 100.0 : 0.0;
                double systemAverageTicketSize = totalInvoiceCount > 0 ? totalOrderValueSum.doubleValue() / totalInvoiceCount : 0.0;

                dataset.addProperty("totalCustomers", totalCustomers);
                dataset.addProperty("newCustomersThisMonth", newCustomersThisMonth);
                dataset.addProperty("vipCount", vipCount);
                dataset.addProperty("totalLoyaltyPoints", totalSystemLoyaltyPoints);
                dataset.addProperty("returnCustomerRate", Math.round(returnCustomerRatePercentage * 10.0) / 10.0);
                dataset.addProperty("averageTicketSize", Math.round(systemAverageTicketSize));

                // 3. Compute Structural Rank Distributions (Donut chart coordinates)
                long copperCount = rawCustomers.stream().filter(c -> c.getBacKH() == null || c.getBacKH().equalsIgnoreCase("Đồng") || c.getBacKH().equalsIgnoreCase("Không hạng")).count();
                long silverCount = rawCustomers.stream().filter(c -> c.getBacKH() != null && c.getBacKH().equalsIgnoreCase("Bạc")).count();
                long goldCount = rawCustomers.stream().filter(c -> c.getBacKH() != null && c.getBacKH().equalsIgnoreCase("Vàng")).count();
                long diamondCount = rawCustomers.stream().filter(c -> c.getBacKH() != null && c.getBacKH().equalsIgnoreCase("Vip")).count();

                JsonObject ranksNode = new JsonObject();
                ranksNode.addProperty("Dong", copperCount);
                ranksNode.addProperty("Bac", silverCount);
                ranksNode.addProperty("Vang", goldCount);
                ranksNode.addProperty("KimCuong", diamondCount);
                dataset.add("rankDistributions", ranksNode);

                // 4. Generate Leaderboard Ranking Node Array (Top Spent Customers)
                Map<String, Double> customerSpendingAggregateMap = new HashMap<>();
                if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                    for (var bill : ordersDTO.dsHoaDon) {
                        if (bill.getMaKH() != null && bill.getThanhTien() != null) {
                            customerSpendingAggregateMap.put(bill.getMaKH(), customerSpendingAggregateMap.getOrDefault(bill.getMaKH(), 0.0) + bill.getThanhTien().doubleValue());
                        }
                    }
                }

                JsonArray leaderboardArray = new JsonArray();
                List<Map.Entry<String, Double>> sortedSpendingList = customerSpendingAggregateMap.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(5)
                        .collect(Collectors.toList());

                for (var entry : sortedSpendingList) {
                    String targetMaKH = entry.getKey();
                    KhachHang customerObj = rawCustomers.stream().filter(c -> c.getMaKH().equalsIgnoreCase(targetMaKH)).findFirst().orElse(null);
                    if (customerObj != null) {
                        JsonObject card = new JsonObject();
                        card.addProperty("id", customerObj.getMaKH());
                        card.addProperty("name", customerObj.getHoTen());
                        card.addProperty("tier", customerObj.getBacKH() != null ? customerObj.getBacKH() : "Đồng");
                        card.addProperty("totalSpent", entry.getValue());
                        card.addProperty("points", customerObj.getDiemTichLuy());
                        card.addProperty("ordersCount", customerOrderFrequencies.getOrDefault(targetMaKH, 0));
                        leaderboardArray.add(card);
                    }
                }
                dataset.add("loyaltyLeaderboard", leaderboardArray);

                // 5. Complete Customer Matrix Datatable Array Payload
                JsonArray tableArray = new JsonArray();
                DateTimeFormatter datePrinter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (KhachHang c : rawCustomers) {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", c.getMaKH());
                    row.addProperty("name", c.getHoTen());
                    row.addProperty("phone", c.getSDT() != null ? c.getSDT() : "---");
                    row.addProperty("tier", c.getBacKH() != null ? c.getBacKH() : "Đồng");
                    row.addProperty("points", c.getDiemTichLuy() != null ? c.getDiemTichLuy() : BigDecimal.ZERO);
                    row.addProperty("totalSpent", customerSpendingAggregateMap.getOrDefault(c.getMaKH(), 0.0));
                    row.addProperty("ordersCount", customerOrderFrequencies.getOrDefault(c.getMaKH(), 0));
                    row.addProperty("joinDate", c.getNgayDangKy() != null ? c.getNgayDangKy().format(datePrinter) : "---");
                    tableArray.add(row);
                }
                dataset.add("customerGridMatrix", tableArray);

                // 6. Generate Context Insights Bullets
                JsonArray insights = new JsonArray();
                insights.add("🔥 Tỷ lệ giữ chân và quay lại mua sắm đạt mức lý tưởng " + Math.round(returnCustomerRatePercentage) + "%.");
                if (vipCount > 0) {
                    insights.add("💎 Đã ghi nhận tổng cộng " + vipCount + " hội viên đạt phân hạng Vàng & VIP trên toàn hệ thống.");
                }
                long lowActivityCount = rawCustomers.stream().filter(c -> customerOrderFrequencies.getOrDefault(c.getMaKH(), 0) == 0).count();
                if (lowActivityCount > 0) {
                    insights.add("⚠️ Phát hiện " + lowActivityCount + " tài khoản chưa phát sinh giao dịch. Đề xuất gửi mã voucher kích cầu mua sắm.");
                }
                dataset.add("intelligenceInsights", insights);

                // 7. Generate Micro-Transaction Purchase Timeline Cards (Last 5 Global Invoices)
                JsonArray timelineArray = new JsonArray();
                if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                    ordersDTO.dsHoaDon.stream()
                            .sorted((h1, h2) -> {
                                if (h1.getNgayTao() == null || h2.getNgayTao() == null) return 0;
                                return h2.getNgayTao().compareTo(h1.getNgayTao());
                            })
                            .limit(5)
                            .forEach(invoice -> {
                                JsonObject timeCard = new JsonObject();
                                timeCard.addProperty("id", invoice.getMaHD());
                                String clientName = "Khách vãng lai";
                                if (invoice.getMaKH() != null && ordersDTO.mapKhachHang.containsKey(invoice.getMaKH())) {
                                    clientName = ordersDTO.mapKhachHang.get(invoice.getMaKH())[0];
                                }
                                timeCard.addProperty("customer", clientName);
                                timeCard.addProperty("amount", invoice.getThanhTien());
                                timeCard.addProperty("method", invoice.getPhuongThucTT() != null ? invoice.getPhuongThucTT() : "Tiền mặt");
                                timeCard.addProperty("time", invoice.getNgayTao() != null ? invoice.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "---");
                                timelineArray.add(timeCard);
                            });
                }
                dataset.add("purchaseTimeline", timelineArray);

                lastCachedJson = gson.toJson(dataset);
                return lastCachedJson;
            } catch (Exception e) {
                System.err.println("[KhachHangPanel Math Core] Failure building analytical trees: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).thenAccept(jsonResult -> {
            if (jsonResult != null) {
                executeJavaScript("updateCustomerDashboard(" + jsonResult + ")");
            }
        });
    }

    private void executeJavaScript(String scriptText) {
        Platform.runLater(() -> {
            try {
                if (webEngine != null) {
                    webEngine.executeScript(scriptText);
                }
            } catch (Exception e) {
                // Suppressed noise
            }
        });
    }

    private void processClientDomSignal(String actionData) {
        SwingUtilities.invokeLater(() -> {
            if ("SYNC".equalsIgnoreCase(actionData)) {
                pushLiveCustomerAnalytics(true);
            } else if ("ADD_CUSTOMER".equalsIgnoreCase(actionData)) {
                // Đã sửa INFORMATION_METHOD thành INFORMATION_MESSAGE
                JOptionPane.showMessageDialog(this, "Hệ thống chuẩn bị kích hoạt Form: THÊM MỚI KHÁCH HÀNG", "CRM Bridge Notification", JOptionPane.INFORMATION_MESSAGE);
            } else if ("EXPORT".equalsIgnoreCase(actionData)) {
                JOptionPane.showMessageDialog(this, "Đang trích xuất báo cáo phân tích tệp khách hàng CRM ra định dạng Excel...", "Excel Tool Link", JOptionPane.INFORMATION_MESSAGE);
            } else if (actionData.startsWith("VIEW_")) {
                String targetId = actionData.substring(5);
                JOptionPane.showMessageDialog(this, "Đang mở hồ sơ CRM đối soát lịch sử hành vi của Khách hàng: " + targetId, "Hồ sơ Khách hàng VIP", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    /**
     * Standalone Test Window Launcher Package for local testing validation.
     */
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ex) {}
        }

        SwingUtilities.invokeLater(() -> {
            JFrame verifyFrame = new JFrame("Customer Intelligence CRM SaaS Platform - Verification Runtime");
            verifyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            verifyFrame.setSize(1400, 820);
            verifyFrame.setMinimumSize(new Dimension(1050, 650));
            verifyFrame.setLocationRelativeTo(null);

            KhachHangPanel crmPanel = new KhachHangPanel();
            verifyFrame.add(crmPanel, BorderLayout.CENTER);
            verifyFrame.setVisible(true);
        });
    }
}