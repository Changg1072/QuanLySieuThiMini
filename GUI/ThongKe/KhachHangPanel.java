package GUI.ThongKe;

import Dao.KhachHangDAO;
import Dao.TruyVanSieuTocDAO;
import Logic.ThongKeLogic;
import Data.KhachHang;
import Dao.HoaDonDAO;
import Data.HoaDon;
import GUI.HoTro.LichSuMuaHangDialog;
import Dao.KhachHangDAO;
import Data.KhachHang;
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

public class KhachHangPanel extends JPanel {

    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    private String lastCachedJson = null;
    private final Map<String, KhachHang> customerCache = new HashMap<>();
    private LocalDate filterStartDate = LocalDate.now().withDayOfMonth(1);
    private LocalDate filterEndDate = LocalDate.now();

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
                            + "<h3>Loi tai giao dien</h3>"
                            + "<p>Khong tim thay file <code>khachhang_dashboard.html</code>.</p>"
                            + "</body></html>");
                }
            } catch (Exception e) {
                System.err.println("[KhachHangPanel] Loi khoi tao: " + e.getMessage());
            }

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }

    public void pushLiveCustomerAnalytics(boolean forceRefresh) {
        if (!forceRefresh && lastCachedJson != null) {
            executeJavaScript("updateCustomerDashboard(" + lastCachedJson + ")");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject dataset = new JsonObject();

                // ✅ FIX CHÍNH: Lấy list rồi đưa ngay vào final List để lambda dùng được
                List<KhachHang> rawList = KhachHangDAO.getInstance().layDanhSachKhachHang();
                if (rawList == null) rawList = new ArrayList<>();
                customerCache.clear();
                for (KhachHang c : rawList) {
                    if (c != null && c.getMaKH() != null) {
                        customerCache.put(c.getMaKH(), c);
                    }
                }
                // Lọc dữ liệu rác — tạo list MỚI thay vì removeIf trên list gốc
                // để đảm bảo biến cuối cùng là effectively final khi dùng trong lambda
                final List<KhachHang> customers = rawList.stream()
                        .filter(c -> c != null
                                && c.getMaKH() != null
                                && !c.getMaKH().trim().isEmpty()
                                && !c.getMaKH().equalsIgnoreCase("null"))
                        .collect(Collectors.toList());

                // Lấy Hóa Đơn
                List<HoaDon> dsHoaDon = HoaDonDAO.getInstance().layDanhSachHoaDon();

                int currentMonthValue = LocalDate.now().getMonthValue();
                int currentYearValue = LocalDate.now().getYear();

                int totalCustomers = customers.size();

                // ✅ Dùng biến 'customers' (effectively final) trong tất cả lambda bên dưới
                long newCustomersThisMonth = customers.stream()
                        .filter(c -> c.getNgayDangKy() != null
                                && !c.getNgayDangKy().isBefore(filterStartDate)
                                && !c.getNgayDangKy().isAfter(filterEndDate))
                        .count();

                long vipCount = customers.stream()
                        .filter(c -> c.getBacKH() != null
                                && (c.getBacKH().equalsIgnoreCase("Vip")
                                    || c.getBacKH().equalsIgnoreCase("Vàng")))
                        .count();

                BigDecimal totalSystemLoyaltyPoints = customers.stream()
                        .filter(c -> c.getDiemTichLuy() != null)
                        .map(KhachHang::getDiemTichLuy)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Map hạng thẻ
                Map<String, String> khToTier = new HashMap<>();
                for (KhachHang c : customers) {
                    String t = "KhongHang";
                    if (c.getBacKH() != null) {
                        if (c.getBacKH().equalsIgnoreCase("Đồng"))  t = "Dong";
                        else if (c.getBacKH().equalsIgnoreCase("Bạc"))  t = "Bac";
                        else if (c.getBacKH().equalsIgnoreCase("Vàng")) t = "Vang";
                        else if (c.getBacKH().equalsIgnoreCase("Vip"))  t = "KimCuong";
                    }
                    khToTier.put(c.getMaKH(), t);
                }

                // Phân loại hóa đơn
                Map<String, Integer> customerOrderFrequencies = new HashMap<>();
                Map<String, Double> customerSpendingAggregateMap = new HashMap<>();
                BigDecimal totalOrderValueSum = BigDecimal.ZERO;
                int totalInvoiceCount = 0;
                int returnedInvoiceCount = 0; // 🔥 Đếm đơn trả hàng
                double totalRefundAmount = 0.0;
                int vangLaiOrdersCount = 0;
                double vangLaiTotalSpent = 0.0;

                String[] tiers = {"KhachVangLai", "KhongHang", "Dong", "Bac", "Vang", "KimCuong"};
                Map<String, Integer> customerReturnFreq = new HashMap<>();
                Map<String, Double> customerRefundMap = new HashMap<>();
                Map<String, Integer> tierCountMap = new HashMap<>();
                Map<String, Double> tierRevenueMap = new HashMap<>();
                for (String t : tiers) {
                    tierCountMap.put(t, 0);
                    tierRevenueMap.put(t, 0.0);
                }

                if (dsHoaDon != null) {
                    // Xóa totalInvoiceCount = dsHoaDon.size(); đi, ta sẽ tự đếm bên dưới
                    totalInvoiceCount = 0; 

                    for (HoaDon bill : dsHoaDon) {
                        
                        // 🔥 BƯỚC 1: KIỂM TRA ĐIỀU KIỆN LỌC THEO NGÀY THÁNG
                        LocalDate ngayTao = null;
                        if (bill.getNgayTao() != null) {
                            ngayTao = bill.getNgayTao().toLocalDate(); // Đổi từ LocalDateTime sang LocalDate
                        }
                        
                        // Nếu hóa đơn nằm ngoài khoảng thời gian lọc -> Bỏ qua không tính
                        if (ngayTao == null || ngayTao.isBefore(filterStartDate) || ngayTao.isAfter(filterEndDate)) {
                            continue;
                        }

                        // 🔥 BƯỚC 2: TÍNH TOÁN CHO CÁC HÓA ĐƠN HỢP LỆ BÊN TRONG MỐC THỜI GIAN
                        totalInvoiceCount++; // Tăng biến đếm đơn hàng hợp lệ
                        
                        double thanhTienBill = bill.getThanhTien() != null ? bill.getThanhTien().doubleValue() : 0.0;
                        
                        // Logic kiểm tra đơn trả hàng 
                        boolean isTraHang = false; 
                        try {
                            isTraHang = bill.getTraHang(); // LƯU Ý: Nếu dùng getTraHang(), giữ nguyên nhé!
                        } catch (Exception ignored) {}

                        if (isTraHang) {
                            returnedInvoiceCount++;
                            totalRefundAmount += thanhTienBill;
                        } else {
                            totalOrderValueSum = totalOrderValueSum.add(bill.getThanhTien() != null ? bill.getThanhTien() : BigDecimal.ZERO);
                        }

                        String maKH = bill.getMaKH();
                        String targetTier;

                        if (maKH != null && !maKH.trim().isEmpty() && khToTier.containsKey(maKH)) {
                            if (isTraHang) {
                                // Nếu là trả hàng, lưu vào Map trả hàng của Khách
                                customerReturnFreq.put(maKH, customerReturnFreq.getOrDefault(maKH, 0) + 1);
                                customerRefundMap.put(maKH, customerRefundMap.getOrDefault(maKH, 0.0) + thanhTienBill);
                            } else {
                                // Nếu mua bình thường
                                customerOrderFrequencies.put(maKH, customerOrderFrequencies.getOrDefault(maKH, 0) + 1);
                                customerSpendingAggregateMap.put(maKH, customerSpendingAggregateMap.getOrDefault(maKH, 0.0) + thanhTienBill);
                            }
                            targetTier = khToTier.get(maKH);
                        } else {
                            targetTier = "KhachVangLai";
                            if (!isTraHang) {
                                vangLaiOrdersCount++;
                                vangLaiTotalSpent += thanhTienBill;
                            }
                        }

                        if (!isTraHang) { // Chart doanh thu thường chỉ tính đơn thành công
                            tierCountMap.put(targetTier, tierCountMap.get(targetTier) + 1);
                            tierRevenueMap.put(targetTier, tierRevenueMap.get(targetTier) + thanhTienBill);
                        }
                    }
                }

                JsonObject tierStatsNode = new JsonObject();
                for (String t : tiers) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("count", tierCountMap.get(t));
                    obj.addProperty("revenue", tierRevenueMap.get(t));
                    tierStatsNode.add(t, obj);
                }
                dataset.add("tierStats", tierStatsNode);

                long returningCustomersCount = customerOrderFrequencies.values().stream()
                        .filter(count -> count >= 2).count();
                double returnCustomerRatePercentage = totalCustomers > 0
                        ? ((double) returningCustomersCount / totalCustomers) * 100.0 : 0.0;
                double systemAverageTicketSize = totalInvoiceCount > 0
                        ? totalOrderValueSum.doubleValue() / totalInvoiceCount : 0.0;

                dataset.addProperty("totalCustomers", totalCustomers);
                dataset.addProperty("newCustomersThisMonth", newCustomersThisMonth);
                dataset.addProperty("vipCount", vipCount);
                dataset.addProperty("totalLoyaltyPoints", totalSystemLoyaltyPoints);
                dataset.addProperty("totalInvoiceCount", totalInvoiceCount);
                dataset.addProperty("averageTicketSize", Math.round(systemAverageTicketSize));
                dataset.addProperty("returnedInvoiceCount", returnedInvoiceCount);
                dataset.addProperty("totalRefundAmount", Math.round(totalRefundAmount));

                // Donut chart phân hạng
                long khongHangCount = customers.stream()
                        .filter(c -> c.getBacKH() == null || c.getBacKH().trim().isEmpty()
                                || c.getBacKH().equalsIgnoreCase("Không hạng"))
                        .count();
                long copperCount  = customers.stream().filter(c -> c.getBacKH() != null && c.getBacKH().equalsIgnoreCase("Đồng")).count();
                long silverCount  = customers.stream().filter(c -> c.getBacKH() != null && c.getBacKH().equalsIgnoreCase("Bạc")).count();
                long goldCount    = customers.stream().filter(c -> c.getBacKH() != null && c.getBacKH().equalsIgnoreCase("Vàng")).count();
                long diamondCount = customers.stream().filter(c -> c.getBacKH() != null && c.getBacKH().equalsIgnoreCase("Vip")).count();

                JsonObject ranksNode = new JsonObject();
                ranksNode.addProperty("KhongHang", khongHangCount);
                ranksNode.addProperty("Dong",      copperCount);
                ranksNode.addProperty("Bac",       silverCount);
                ranksNode.addProperty("Vang",      goldCount);
                ranksNode.addProperty("KimCuong",  diamondCount);
                dataset.add("rankDistributions", ranksNode);

                // Leaderboard top 5 chi tiêu
                JsonArray leaderboardArray = new JsonArray();
                customerSpendingAggregateMap.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(5)
                        .forEach(entry -> {
                            String targetMaKH = entry.getKey();
                            // ✅ Dùng 'customers' (effectively final) — không lỗi
                            KhachHang customerObj = customers.stream()
                                    .filter(c -> c.getMaKH().equalsIgnoreCase(targetMaKH))
                                    .findFirst().orElse(null);
                            if (customerObj != null) {
                                JsonObject card = new JsonObject();
                                card.addProperty("id", customerObj.getMaKH());
                                card.addProperty("name", customerObj.getHoTen());
                                card.addProperty("tier", (customerObj.getBacKH() != null && !customerObj.getBacKH().trim().isEmpty())
                                        ? customerObj.getBacKH() : "Không hạng");
                                card.addProperty("totalSpent", entry.getValue());
                                card.addProperty("points", customerObj.getDiemTichLuy() != null
                                        ? customerObj.getDiemTichLuy() : BigDecimal.ZERO);
                                card.addProperty("ordersCount", customerOrderFrequencies.getOrDefault(targetMaKH, 0));
                                leaderboardArray.add(card);
                            }
                        });
                dataset.add("loyaltyLeaderboard", leaderboardArray);

                // Datatable
                JsonArray tableArray = new JsonArray();
                DateTimeFormatter datePrinter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (KhachHang c : customers) {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", c.getMaKH());
                    row.addProperty("name", c.getHoTen());
                    row.addProperty("phone", c.getSDT() != null ? c.getSDT() : "---");
                    row.addProperty("tier", (c.getBacKH() != null && !c.getBacKH().trim().isEmpty())
                            ? c.getBacKH() : "Không hạng");
                    row.addProperty("points", c.getDiemTichLuy() != null ? c.getDiemTichLuy() : BigDecimal.ZERO);
                    row.addProperty("totalSpent", customerSpendingAggregateMap.getOrDefault(c.getMaKH(), 0.0));
                    row.addProperty("ordersCount", customerOrderFrequencies.getOrDefault(c.getMaKH(), 0));
                    row.addProperty("joinDate", c.getNgayDangKy() != null ? c.getNgayDangKy().format(datePrinter) : "---");
                    row.addProperty("returnCount", customerReturnFreq.getOrDefault(c.getMaKH(), 0));
                    row.addProperty("refundAmount", customerRefundMap.getOrDefault(c.getMaKH(), 0.0));
                    tableArray.add(row);
                }

                // Hàng tổng hợp khách vãng lai
                JsonObject vangLaiRow = new JsonObject();
                vangLaiRow.addProperty("id", "KVL");
                vangLaiRow.addProperty("name", "Khách vãng lai (Tổng hợp)");
                vangLaiRow.addProperty("phone", "---");
                vangLaiRow.addProperty("tier", "Khách vãng lai");
                vangLaiRow.addProperty("points", 0);
                vangLaiRow.addProperty("totalSpent", vangLaiTotalSpent);
                vangLaiRow.addProperty("ordersCount", vangLaiOrdersCount);
                vangLaiRow.addProperty("joinDate", "---");
                tableArray.add(vangLaiRow);
                dataset.add("customerGridMatrix", tableArray);

                // AI Insights
                JsonArray insights = new JsonArray();
                insights.add("<i class='ti ti-flame' style='color:#f59e0b;font-size:15px;margin-right:6px;vertical-align:-2px;'></i> He thong dat "
                        + totalInvoiceCount + " giao dich (bao gom vang lai). Ty le giu chan CRM la "
                        + Math.round(returnCustomerRatePercentage) + "%.");
                if (vipCount > 0) {
                    insights.add("<i class='ti ti-diamond' style='color:#8b5cf6;font-size:15px;margin-right:6px;vertical-align:-2px;'></i> Ghi nhan "
                            + vipCount + " hoi vien dat phan hang Vang & VIP.");
                }
                // ✅ Dùng 'customers' (effectively final)
                long lowActivityCount = customers.stream()
                        .filter(c -> customerOrderFrequencies.getOrDefault(c.getMaKH(), 0) == 0)
                        .count();
                if (lowActivityCount > 0) {
                    insights.add("<i class='ti ti-alert-triangle' style='color:#ef4444;font-size:15px;margin-right:6px;vertical-align:-2px;'></i> "
                            + lowActivityCount + " tai khoan chua giao dich. Can remarketing.");
                }
                dataset.add("intelligenceInsights", insights);

                // Timeline 5 hóa đơn gần nhất
                JsonArray timelineArray = new JsonArray();
                if (dsHoaDon != null) {
                    dsHoaDon.stream()
                            .filter(h -> h.getNgayTao() != null)
                            .sorted((h1, h2) -> h2.getNgayTao().compareTo(h1.getNgayTao()))
                            .limit(5)
                            .forEach(invoice -> {
                                JsonObject timeCard = new JsonObject();
                                timeCard.addProperty("id", invoice.getMaHD() != null ? invoice.getMaHD() : "---");

                                String clientName = "Khach vang lai";
                                if (invoice.getMaKH() != null && khToTier.containsKey(invoice.getMaKH())) {
                                    // ✅ Dùng 'customers' (effectively final)
                                    KhachHang kh = customers.stream()
                                            .filter(c -> c.getMaKH().equals(invoice.getMaKH()))
                                            .findFirst().orElse(null);
                                    if (kh != null) clientName = kh.getHoTen();
                                }
                                timeCard.addProperty("customer", clientName);
                                timeCard.addProperty("amount", invoice.getThanhTien());
                                timeCard.addProperty("method", invoice.getPhuongThucTT() != null
                                        ? invoice.getPhuongThucTT() : "Tien mat");
                                timeCard.addProperty("time", invoice.getNgayTao()
                                        .format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
                                timelineArray.add(timeCard);
                            });
                }
                dataset.add("purchaseTimeline", timelineArray);

                lastCachedJson = gson.toJson(dataset);
                return lastCachedJson;

            } catch (Exception e) {
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
                if (webEngine != null) webEngine.executeScript(scriptText);
            } catch (Exception e) {
                // Suppressed
            }
        });
    }
    private void processClientDomSignal(String actionData) {
        SwingUtilities.invokeLater(() -> {
            if ("SYNC".equalsIgnoreCase(actionData)) {
                pushLiveCustomerAnalytics(true);
            } 
            // 🔥 XỬ LÝ LỌC THEO NGÀY
            else if (actionData.startsWith("DATE_SYNC|")) {
                try {
                    String[] parts = actionData.split("\\|");
                    filterStartDate = LocalDate.parse(parts[1]); // YYYY-MM-DD
                    filterEndDate = LocalDate.parse(parts[2]);
                    pushLiveCustomerAnalytics(true); // Yêu cầu tính lại toàn bộ
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } 
            // 🔥 XỬ LÝ NÚT XUẤT EXCEL KÈM TÊN FILE
            else if (actionData.startsWith("EXPORT|")) {
                String fileName = actionData.split("\\|")[1];
                JOptionPane.showMessageDialog(this, "Đang trích xuất dữ liệu: " + fileName);
                
                CompletableFuture.runAsync(() -> {
                    try {
                        String jsonFileName = fileName.replace(".xlsx", ".json");
                        
                        // LƯU VÀO DATABASE BẰNG DAO
                        if (lastCachedJson != null) {
                            Dao.LichSuThongKeDAO.getInstance().luuLichSu("KHACH_HANG", jsonFileName, lastCachedJson);
                        }
                        
                        // Gọi lại hàm load danh sách để Web tự động cập nhật Dropdown
                        processClientDomSignal("LOAD_HISTORY_LIST");
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
            else if (actionData.startsWith("VIEW_ALL_INVOICES")) {
                JOptionPane.showMessageDialog(this, "Mở giao diện tra cứu TOÀN BỘ HÓA ĐƠN của hệ thống!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                // GỌI FORM HÓA ĐƠN CỦA BẠN: LichSuHoaDonDialog.showModal(this); (hoặc tương tự)
            }
            else if (actionData.startsWith("VIEW_")) {
                String targetId = actionData.substring(5);
                if (targetId.equals("ALL_INVOICES")) {
                    JOptionPane.showMessageDialog(this, "Mở trình tra cứu hóa đơn...");
                } else {
                    KhachHang kh = customerCache.get(targetId);
                    if (kh != null) LichSuMuaHangDialog.showModal(this, kh);
                }
            }
            else if (actionData.equals("LOAD_HISTORY_LIST")) {
                reloadHistoryList();
            }
            else if (actionData.startsWith("LOAD_HISTORY_FILE|")) {
                String tenFile = actionData.substring("LOAD_HISTORY_FILE|".length());
                CompletableFuture.runAsync(() -> {
                    try {
                        // Đọc chuỗi Base64 trực tiếp từ SQL Server
                        String base64 = Dao.LichSuThongKeDAO.getInstance().docNoiDungLichSuBase64(tenFile);
                        if (base64 != null) {
                            String safeName = tenFile.replace("'", "\\'");
                            Platform.runLater(() -> {
                                if (webEngine != null) webEngine.executeScript("applyHistoricalStateBase64('" + base64 + "', '" + safeName + "')");
                            });
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
            else if (actionData.equals("EXIT_HISTORY")) {
                pushLiveCustomerAnalytics(true); 
            }
        });
    }
    private void reloadHistoryList() {
        CompletableFuture.runAsync(() -> {
            try {
                // Lấy danh sách lịch sử của phân hệ KHACH_HANG
                String jsonArrayData = Dao.LichSuThongKeDAO.getInstance().layDanhSachLichSu("KHACH_HANG");
                Platform.runLater(() -> {
                    if (webEngine != null) {
                        webEngine.executeScript("updateHistoryDropdown('" + jsonArrayData + "')");
                    }
                });
            } catch (Exception e) { 
                e.printStackTrace(); 
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame verifyFrame = new JFrame("Customer Intelligence CRM - Test");
            verifyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            verifyFrame.setSize(1400, 820);
            verifyFrame.setMinimumSize(new Dimension(1050, 650));
            verifyFrame.setLocationRelativeTo(null);
            verifyFrame.add(new KhachHangPanel(), BorderLayout.CENTER);
            verifyFrame.setVisible(true);
        });
    }
}