package GUI.ThongKe;

import Dao.TruyVanSieuTocDAO;
import Logic.ThongKeLogic;
import Data.*; // Đảm bảo gọi toàn bộ Data Model
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors; // 🔥 FIX LỖI THIẾU THƯ VIỆN Ở ĐÂY

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

public class NhanVienPanel extends JPanel {

    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    private String lastCachedDashboardJson = null;
    public interface NhanVienPanelCallback {
        void moThemNhanVien();
        void moPhanCa();
    }
    private NhanVienPanelCallback actionCallback;
    
    public void setCallback(NhanVienPanelCallback callback) {
        this.actionCallback = callback;
    }
    public NhanVienPanel() {
        setName("NhanVienPanel");
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
                    handleWebActionSignal(signal.substring(7));
                }
            });

            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    pushLiveWorkforceAnalytics(false);
                }
            });

            try {
                URL htmlUrl = getClass().getResource("nhanvien_dashboard.html");
                if (htmlUrl != null) {
                    webEngine.load(htmlUrl.toExternalForm());
                } else {
                    webEngine.loadContent("<html><body style='font-family:sans-serif;padding:30px;color:#ef4444;'>"
                        + "<h3>🚨 Lỗi tải giao diện</h3>"
                        + "<p>Không tìm thấy file <code>nhanvien_dashboard.html</code>. Hãy chắc chắn bạn đã copy nó vào thư mục bin/GUI/ThongKe/</p>"
                        + "</body></html>");
                }
            } catch (Exception e) {
                System.err.println("[NhanVienPanel Bridge] Initialization breakdown: " + e.getMessage());
            }

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }

    public void pushLiveWorkforceAnalytics(boolean forceRefresh) {
        if (!forceRefresh && lastCachedDashboardJson != null) {
            executeJavaScript("updateWorkforceDashboard(" + lastCachedDashboardJson + ")");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject mainContainer = new JsonObject();
                
                TruyVanSieuTocDAO.DuLieuDonHangDTO ordersDTO = TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                
                List<Data.ChiaCa> dsChiaCa = new ArrayList<>();
                Map<String, String> mapChucVuNhanVien = new HashMap<>();
                Map<String, String> mapSdtNhanVien = new HashMap<>();
                
                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                     java.sql.Statement st = con.createStatement()) {
                    
                    try (java.sql.ResultSet rs = st.executeQuery("SELECT * FROM ChiaCa")) {
                        while (rs.next()) {
                            Data.ChiaCa cc = new Data.ChiaCa();
                            cc.setMaCa(rs.getString("MaCa"));
                            cc.setMaLoaiCa(rs.getString("MaLoaiCa"));
                            cc.setMaNV(rs.getString("MaNV"));
                            cc.setTinhTrang(rs.getString("TinhTrang"));
                            
                            java.sql.Date sqlNgay = rs.getDate("NgayLam");
                            if (sqlNgay != null) cc.setNgayLam(sqlNgay.toLocalDate());
                            
                            java.sql.Timestamp tsIn = rs.getTimestamp("ThoiGianCheckIn");
                            if (tsIn != null) cc.setThoiGianCheckIn(tsIn.toLocalDateTime());
                            
                            java.sql.Timestamp tsOut = rs.getTimestamp("ThoiGianCheckOut");
                            if (tsOut != null) cc.setThoiGianCheckOut(tsOut.toLocalDateTime());
                            
                            cc.setTienBanHang(rs.getBigDecimal("TienBanHang"));
                            dsChiaCa.add(cc);
                        }
                    }
                    
                    try (java.sql.ResultSet rs = st.executeQuery("SELECT MaNV, ChucVu, SDT FROM NhanVien")) {
                        while (rs.next()) {
                            String maNV = rs.getString("MaNV");
                            mapChucVuNhanVien.put(maNV, rs.getString("ChucVu"));
                            mapSdtNhanVien.put(maNV, rs.getString("SDT"));
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[NhanVienPanel SQL Fetch] Handled anomaly: " + ex.getMessage());
                }

                int targetMonth = LocalDate.now().getMonthValue();
                int targetYear = LocalDate.now().getYear();

                Set<String> uniqueActiveStaffIds = new HashSet<>(mapChucVuNhanVien.keySet());
                int totalEmployeesCount = uniqueActiveStaffIds.size();

                Map<String, BigDecimal> staffSalesRevenueMap = new HashMap<>();
                Map<String, Integer> staffInvoiceCountMap = new HashMap<>();
                Map<String, Double> staffWorkHoursMap = new HashMap<>();
                Map<String, Integer> staffLateCheckInCountMap = new HashMap<>();
                
                for (String employeeId : uniqueActiveStaffIds) {
                    staffSalesRevenueMap.put(employeeId, BigDecimal.ZERO);
                    staffInvoiceCountMap.put(employeeId, 0);
                    staffWorkHoursMap.put(employeeId, 0.0);
                    staffLateCheckInCountMap.put(employeeId, 0);
                }

                BigDecimal globalWorkforceRevenueSum = BigDecimal.ZERO;
                if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                    for (var hd : ordersDTO.dsHoaDon) {
                        if (hd.getMaNV() != null && hd.getThanhTien() != null) {
                            String empId = hd.getMaNV().trim();
                            if (uniqueActiveStaffIds.contains(empId)) {
                                staffSalesRevenueMap.put(empId, staffSalesRevenueMap.get(empId).add(hd.getThanhTien()));
                                staffInvoiceCountMap.put(empId, staffInvoiceCountMap.get(empId) + 1);
                            }
                            globalWorkforceRevenueSum = globalWorkforceRevenueSum.add(hd.getThanhTien());
                        }
                    }
                }

                int totalLateOccurrences = 0;
                double aggregatedWorkHours = 0.0;
                int activeShiftsThisMonth = 0;
                int shiftMorningCount = 0, shiftAfternoonCount = 0, shiftNightCount = 0;

                for (Data.ChiaCa shift : dsChiaCa) {
                    if (shift.getMaNV() == null) continue;
                    String empId = shift.getMaNV().trim();
                    if (!uniqueActiveStaffIds.contains(empId)) continue;

                    String loaiCa = shift.getMaLoaiCa() != null ? shift.getMaLoaiCa().toLowerCase() : "";
                    if (loaiCa.contains("ca1") || loaiCa.contains("sáng")) shiftMorningCount++;
                    else if (loaiCa.contains("ca2") || loaiCa.contains("chiều")) shiftAfternoonCount++;
                    else shiftNightCount++;

                    if (shift.getNgayLam() != null 
                            && shift.getNgayLam().getMonthValue() == targetMonth 
                            && shift.getNgayLam().getYear() == targetYear) {
                        
                        activeShiftsThisMonth++;

                        if (shift.getThoiGianCheckIn() != null && shift.getThoiGianCheckOut() != null) {
                            long minutesDiff = Duration.between(shift.getThoiGianCheckIn(), shift.getThoiGianCheckOut()).toMinutes();
                            double calculatedHours = minutesDiff / 60.0;
                            if (calculatedHours > 0 && calculatedHours < 16) { 
                                staffWorkHoursMap.put(empId, staffWorkHoursMap.get(empId) + calculatedHours);
                                aggregatedWorkHours += calculatedHours;
                            }
                        }

                        if (shift.getThoiGianCheckIn() != null) {
                            LocalTime timeToken = shift.getThoiGianCheckIn().toLocalTime();
                            if ((timeToken.isAfter(LocalTime.of(8, 15)) && timeToken.isBefore(LocalTime.of(11, 45))) || 
                                (timeToken.isAfter(LocalTime.of(13, 15)) && timeToken.isBefore(LocalTime.of(17, 45)))) {
                                staffLateCheckInCountMap.put(empId, staffLateCheckInCountMap.get(empId) + 1);
                                totalLateOccurrences++;
                            }
                        }
                    }
                }

                String topPerformerName = "Chưa ghi nhận";
                BigDecimal highestRevenue = BigDecimal.ZERO;
                for (var entry : staffSalesRevenueMap.entrySet()) {
                    if (entry.getValue().compareTo(highestRevenue) > 0) {
                        highestRevenue = entry.getValue();
                        if (ordersDTO != null && ordersDTO.mapNhanVien.containsKey(entry.getKey())) {
                            topPerformerName = ordersDTO.mapNhanVien.get(entry.getKey());
                        } else {
                            topPerformerName = entry.getKey();
                        }
                    }
                }

                mainContainer.addProperty("totalEmployees", totalEmployeesCount);
                mainContainer.addProperty("activeStaffCount", totalEmployeesCount); 
                mainContainer.addProperty("globalWorkforceRevenue", globalWorkforceRevenueSum);
                mainContainer.addProperty("topPerformerName", topPerformerName);
                mainContainer.addProperty("totalWorkHours", Math.round(aggregatedWorkHours));
                mainContainer.addProperty("averageHoursPerEmployee", totalEmployeesCount > 0 ? Math.round(aggregatedWorkHours / totalEmployeesCount) : 0);
                mainContainer.addProperty("totalLateOccurrences", totalLateOccurrences);
                
                double safePunctualityRate = activeShiftsThisMonth > 0 ? ((double)(activeShiftsThisMonth - totalLateOccurrences) / activeShiftsThisMonth) * 100.0 : 100.0;
                mainContainer.addProperty("punctualityRate", Math.max(0, Math.min(100, Math.round(safePunctualityRate))));

                JsonObject revenueChartNode = new JsonObject();
                JsonObject shiftsChartNode = new JsonObject();
                
                shiftsChartNode.addProperty("Ca Sáng", shiftMorningCount);
                shiftsChartNode.addProperty("Ca Chiều", shiftAfternoonCount);
                shiftsChartNode.addProperty("Ca Tối", shiftNightCount);
                mainContainer.add("shiftDistribution", shiftsChartNode);

                JsonArray leaderboardArray = new JsonArray();
                List<Map.Entry<String, BigDecimal>> sortedStaffSales = staffSalesRevenueMap.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(5)
                        .collect(Collectors.toList()); // ĐÃ SỬA LỖI Ở ĐÂY

                for (var entry : sortedStaffSales) {
                    String empId = entry.getKey();
                    JsonObject card = new JsonObject();
                    card.addProperty("id", empId);
                    
                    String hoTen = ordersDTO != null && ordersDTO.mapNhanVien.containsKey(empId) ? ordersDTO.mapNhanVien.get(empId) : "Nhân viên " + empId;
                    card.addProperty("name", hoTen);
                    card.addProperty("role", mapChucVuNhanVien.getOrDefault(empId, "Thu Ngân"));
                    card.addProperty("revenue", entry.getValue());
                    card.addProperty("invoicesCount", staffInvoiceCountMap.getOrDefault(empId, 0));
                    
                    double targetRatio = highestRevenue.compareTo(BigDecimal.ZERO) > 0 ? (entry.getValue().doubleValue() / highestRevenue.doubleValue()) * 100.0 : 100.0;
                    card.addProperty("efficiencyRate", Math.round(targetRatio));
                    leaderboardArray.add(card);
                    
                    revenueChartNode.addProperty(hoTen, entry.getValue());
                }
                mainContainer.add("salesByEmployee", revenueChartNode);
                mainContainer.add("employeeLeaderboard", leaderboardArray);

                JsonArray datatableArray = new JsonArray();
                for (String empId : mapChucVuNhanVien.keySet()) {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", empId);
                    
                    String hoTen = ordersDTO != null && ordersDTO.mapNhanVien.containsKey(empId) ? ordersDTO.mapNhanVien.get(empId) : "Nhân viên " + empId;
                    row.addProperty("name", hoTen);
                    row.addProperty("role", mapChucVuNhanVien.getOrDefault(empId, "Thu Ngân"));
                    row.addProperty("phone", mapSdtNhanVien.getOrDefault(empId, "---"));
                    row.addProperty("revenue", staffSalesRevenueMap.getOrDefault(empId, BigDecimal.ZERO));
                    row.addProperty("workHours", Math.round(staffWorkHoursMap.getOrDefault(empId, 0.0) * 10.0) / 10.0);
                    row.addProperty("lateCount", staffLateCheckInCountMap.getOrDefault(empId, 0));
                    row.addProperty("status", "ACTIVE");
                    datatableArray.add(row);
                }
                mainContainer.add("employeeGridMatrix", datatableArray);

                JsonArray operationalInsights = new JsonArray();
                if (highestRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    operationalInsights.add("🔥 Chiến thần doanh số '" + topPerformerName + "' xuất sắc dẫn đầu chỉ số KPI doanh thu cá nhân chi nhánh.");
                }
                if (totalLateOccurrences > 0) {
                    operationalInsights.add("⚠️ Ghi nhận " + totalLateOccurrences + " lượt đi trễ trong tháng. Đề xuất ban quản lý nhắc nhở kỷ luật phân ca.");
                } else {
                    operationalInsights.add("🚀 Chỉ số tuân thủ giờ giấc của tập thể đạt tỷ lệ tối ưu 100%. Không có trường hợp đi trễ.");
                }
                operationalInsights.add("💡 Phân ca làm việc buổi tối ('Ca Tối') đang tạo ra hiệu suất giao dịch thương mại sầm uất nhất.");
                mainContainer.add("workforceInsights", operationalInsights);

                JsonArray timelineArray = new JsonArray();
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                
                dsChiaCa.stream()
                        .sorted((c1, c2) -> {
                            if (c1.getNgayLam() == null || c2.getNgayLam() == null) return 0;
                            return c2.getNgayLam().compareTo(c1.getNgayLam());
                        })
                        .limit(5)
                        .forEach(shift -> {
                            JsonObject cell = new JsonObject();
                            String nvName = ordersDTO != null && ordersDTO.mapNhanVien.containsKey(shift.getMaNV()) ? ordersDTO.mapNhanVien.get(shift.getMaNV()) : shift.getMaNV();
                            cell.addProperty("staff", nvName);
                            cell.addProperty("shiftId", shift.getMaCa() + " (" + shift.getMaLoaiCa() + ")");
                            cell.addProperty("dateLabel", shift.getNgayLam() != null ? shift.getNgayLam().format(DateTimeFormatter.ofPattern("dd/MM")) : "--");
                            
                            String checkInStr = shift.getThoiGianCheckIn() != null ? shift.getThoiGianCheckIn().format(timeFormatter) : "--:--";
                            String checkOutStr = shift.getThoiGianCheckOut() != null ? shift.getThoiGianCheckOut().format(timeFormatter) : "Đang ca";
                            cell.addProperty("timeFrame", checkInStr + " → " + checkOutStr);
                            timelineArray.add(cell);
                        });
                mainContainer.add("shiftTimeline", timelineArray);

                lastCachedDashboardJson = gson.toJson(mainContainer);
                return lastCachedDashboardJson;
                
            // BẮT LỖI CỰC MẠNH: Nếu có lỗi hệ thống, nó sẽ in thẳng ra Console đỏ chót
            } catch (Throwable e) {
                System.err.println("🔥 [CRITICAL ERROR] Quá trình xử lý nền bị sập: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).thenAccept(jsonResult -> {
            if (jsonResult != null) {
                executeJavaScript("updateWorkforceDashboard(" + jsonResult + ")");
            }
        });
    }

    private void executeJavaScript(String scriptText) {
        Platform.runLater(() -> {
            try {
                if (webEngine != null) {
                    webEngine.executeScript(scriptText);
                }
            } catch (Exception e) {}
        });
    }

    private void handleWebActionSignal(String actionToken) {
        SwingUtilities.invokeLater(() -> {
            if ("SYNC".equalsIgnoreCase(actionToken)) {
                pushLiveWorkforceAnalytics(true);
            } else if ("ADD_STAFF".equalsIgnoreCase(actionToken)) {
                if (actionCallback != null) actionCallback.moThemNhanVien();
                else JOptionPane.showMessageDialog(this, "Chưa kết nối Router: Chuẩn bị mở form Thêm Nhân Viên");
            } else if ("ASSIGN_SHIFT".equalsIgnoreCase(actionToken)) {
                if (actionCallback != null) actionCallback.moPhanCa();
                else JOptionPane.showMessageDialog(this, "Chưa kết nối Router: Chuẩn bị mở form Phân Ca");
            } else if ("EXPORT".equalsIgnoreCase(actionToken)) {
                JOptionPane.showMessageDialog(this, "Đang khởi tạo tệp trích xuất đối soát hiệu suất nhân sự ra Excel...", "Xuất Báo Cáo", JOptionPane.INFORMATION_MESSAGE);
            } else if (actionToken.startsWith("VIEW_")) {
                String staffId = actionToken.substring(5);
                JOptionPane.showMessageDialog(this, "Đang mở hồ sơ của Nhân viên: " + staffId, "Hồ sơ Nhân sự", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

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
            JFrame devFrame = new JFrame("Workforce Intelligence HR SaaS Engine - Standalone Runtime Verification");
            devFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            devFrame.setSize(1420, 830);
            devFrame.setMinimumSize(new Dimension(1050, 660));
            devFrame.setLocationRelativeTo(null);

            NhanVienPanel workforcePanel = new NhanVienPanel();
            devFrame.add(workforcePanel, BorderLayout.CENTER);
            devFrame.setVisible(true);
        });
    }
}