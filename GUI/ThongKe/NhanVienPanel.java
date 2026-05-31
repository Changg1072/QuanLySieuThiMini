package GUI.ThongKe;

import Dao.CauHinhLuongDAO;
import Dao.ChiaCaDAO;
import Dao.TruyVanSieuTocDAO;
import Data.CauHinhLuong;
import Data.ChiaCa;
import Data.NhanVien;
import Logic.BangLuongLogic;
import Logic.NhanVienLogic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        void xemHoSoLuong(String maNV);
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
                        + "<p>Không tìm thấy file nhanvien_dashboard.html</p></body></html>");
                }
            } catch (Exception e) {
                System.err.println("[NhanVienPanel Bridge] Lỗi khởi tạo: " + e.getMessage());
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
                
                // 1. Khởi tạo các Logic & DAO
                BangLuongLogic bangLuongLogic = new BangLuongLogic();
                NhanVienLogic nhanVienLogic = new NhanVienLogic();
                CauHinhLuongDAO cauHinhLuongDAO = CauHinhLuongDAO.getInstance();
                ChiaCaDAO chiaCaDAO = ChiaCaDAO.getInstance();
                TruyVanSieuTocDAO.DuLieuDonHangDTO ordersDTO = TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                
                // BỔ SUNG FIX 1: Lấy danh sách Loại Ca để map Tên Ca chính xác
                Logic.LoaiCaLogic loaiCaLogic = new Logic.LoaiCaLogic();
                List<Data.LoaiCa> dsLoaiCa = loaiCaLogic.layDanhSachLoaiCa();
                
                List<NhanVien> dsNhanVien = nhanVienLogic.layDanhSachNhanVien();
                int targetMonth = LocalDate.now().getMonthValue();
                int targetYear = LocalDate.now().getYear();
                LocalDate homNay = LocalDate.now();

                int totalEmployeesCount = dsNhanVien.size();
                int activeStaffCount = 0;
                BigDecimal globalWorkforceRevenueSum = BigDecimal.ZERO;
                BigDecimal totalPayrollPool = BigDecimal.ZERO;
                
                int totalLateOccurrences = 0;
                double aggregatedWorkHours = 0.0;
                int shiftMorningCount = 0, shiftAfternoonCount = 0, shiftNightCount = 0;
                int pastOrActiveShiftsCount = 0; // Mẫu số tính Tỷ lệ đúng giờ

                List<ChiaCa> tatCaCaThangNay = new ArrayList<>();
                List<JsonObject> allEmployeeStats = new ArrayList<>();

                // 2. Quét từng nhân viên để tổng hợp liệu thống kê
                for (NhanVien nv : dsNhanVien) {
                    
                    activeStaffCount++;
                    String empId = nv.getMaNV();

                    // --- Tính Doanh Thu ---
                    BigDecimal staffRevenue = BigDecimal.ZERO;
                    int invoiceCount = 0;
                    if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                        for (var hd : ordersDTO.dsHoaDon) {
                            if (empId.equals(hd.getMaNV()) && hd.getThanhTien() != null) {
                                staffRevenue = staffRevenue.add(hd.getThanhTien());
                                invoiceCount++;
                            }
                        }
                    }
                    globalWorkforceRevenueSum = globalWorkforceRevenueSum.add(staffRevenue);

                    // --- BỔ SUNG FIX 2: Thống kê Chia ca chuẩn xác ---
                    List<ChiaCa> dsCaCuaNV = chiaCaDAO.layDanhSachChiaCaTheoThang(empId, targetMonth, targetYear);
                    if (dsCaCuaNV != null) {
                        for (ChiaCa shift : dsCaCuaNV) {
                            // Lọc bỏ ca bị hủy
                            if ("Đã hủy".equalsIgnoreCase(shift.getTinhTrang())) continue;
                            
                            tatCaCaThangNay.add(shift);

                            // Map ID loại ca thành Tên loại ca
                            boolean daPhanLoai = false;
                            String maCaCuaShift = (shift.getMaLoaiCa() != null) ? shift.getMaLoaiCa().trim() : "";

                            if (dsLoaiCa != null && !maCaCuaShift.isEmpty()) {
                                for (Data.LoaiCa lc : dsLoaiCa) {
                                    String maLoaiCaTruyXuat = (lc.getMaLoaiCa() != null) ? lc.getMaLoaiCa().trim() : "";
                                    
                                    // Dùng equalsIgnoreCase và trim để đảm bảo so sánh chuỗi chính xác 100%
                                    if (maLoaiCaTruyXuat.equalsIgnoreCase(maCaCuaShift)) {
                                        
                                        if (lc.getGioBatDau() != null) {
                                            int gioBatDau = lc.getGioBatDau().getHour();
                                            
                                            if (gioBatDau >= 4 && gioBatDau < 12) {
                                                shiftMorningCount++;       // Ca Sáng (4h - 11h59)
                                            } else if (gioBatDau >= 12 && gioBatDau < 18) {
                                                shiftAfternoonCount++;     // Ca Chiều (12h - 17h59)
                                            } else {
                                                shiftNightCount++;         // Ca Tối (18h trở đi)
                                            }
                                            daPhanLoai = true;
                                        }
                                        break;
                                    }
                                }
                            }

                            // Nếu không khớp mã nào thì đưa vào ca trống/tối
                            if (!daPhanLoai) {
                                shiftNightCount++;
                            }
                            // Đếm số ca đã qua hoặc đang làm để làm mẫu số tính kỷ luật
                            if (shift.getNgayLam() != null && !shift.getNgayLam().isAfter(homNay)) {
                                pastOrActiveShiftsCount++;
                            }
                        }
                    }

                    // --- Tính Lương & Khấu Trừ ---
                    BigDecimal hoursWorked = bangLuongLogic.tinhTongGioLamTrongThang(empId, targetMonth, targetYear);
                    aggregatedWorkHours += hoursWorked.doubleValue();

                    BangLuongLogic.ChiTietKhauTru chiTietKhauTru = bangLuongLogic.tinhChiTietKhauTru(empId, targetMonth, targetYear);
                    totalLateOccurrences += chiTietKhauTru.soLanTre;

                    CauHinhLuong cauHinh = cauHinhLuongDAO.layCauHinhHienTaiTheoMaNV(empId);
                    BigDecimal hourlyWage = (cauHinh != null && cauHinh.getLuongTheoGio() != null) ? cauHinh.getLuongTheoGio() : BigDecimal.ZERO;
                    
                    BigDecimal estimatedSalary = hourlyWage.multiply(hoursWorked).setScale(0, RoundingMode.HALF_UP);
                    totalPayrollPool = totalPayrollPool.add(estimatedSalary);

                    // --- Lưu JSON ---
                    JsonObject stats = new JsonObject();
                    stats.addProperty("id", empId);
                    stats.addProperty("name", nv.getHoTen());
                    stats.addProperty("role", nv.getChucVu());
                    stats.addProperty("status", nv.getTrangThai());
                    stats.addProperty("phone", nv.getSDT() != null ? nv.getSDT() : "---");
                    stats.addProperty("workHours", hoursWorked);
                    stats.addProperty("revenue", staffRevenue);
                    stats.addProperty("salary", estimatedSalary);
                    stats.addProperty("lateCount", chiTietKhauTru.soLanTre);
                    stats.addProperty("penalty", chiTietKhauTru.tongTienPhat);
                    stats.addProperty("invoicesCount", invoiceCount);
                    allEmployeeStats.add(stats);
                }

                allEmployeeStats.sort((a, b) -> b.get("revenue").getAsBigDecimal().compareTo(a.get("revenue").getAsBigDecimal()));

                JsonArray datatableArray = new JsonArray();
                JsonArray chartCategories = new JsonArray();
                JsonArray chartRevenues = new JsonArray();
                JsonArray chartSalaries = new JsonArray();
                JsonArray leaderboardArray = new JsonArray();

                BigDecimal highestRevenue = allEmployeeStats.isEmpty() ? BigDecimal.ZERO : allEmployeeStats.get(0).get("revenue").getAsBigDecimal();
                String topPerformerName = allEmployeeStats.isEmpty() ? "Chưa ghi nhận" : allEmployeeStats.get(0).get("name").getAsString();

                for (int i = 0; i < allEmployeeStats.size(); i++) {
                    JsonObject s = allEmployeeStats.get(i);
                    datatableArray.add(s);
                    if (i < 5) {
                        chartCategories.add(s.get("name").getAsString());
                        chartRevenues.add(s.get("revenue").getAsBigDecimal());
                        chartSalaries.add(s.get("salary").getAsBigDecimal());

                        JsonObject card = new JsonObject();
                        card.addProperty("id", s.get("id").getAsString());
                        card.addProperty("name", s.get("name").getAsString());
                        card.addProperty("role", s.get("role").getAsString());
                        card.addProperty("revenue", s.get("revenue").getAsBigDecimal());
                        
                        double targetRatio = highestRevenue.compareTo(BigDecimal.ZERO) > 0 ? (s.get("revenue").getAsDouble() / highestRevenue.doubleValue()) * 100.0 : 100.0;
                        card.addProperty("efficiencyRate", Math.round(targetRatio));
                        leaderboardArray.add(card);
                    }
                }

                // 3. Gắn thông số KPI
                mainContainer.addProperty("totalEmployees", totalEmployeesCount);
                mainContainer.addProperty("activeStaffCount", activeStaffCount); 
                mainContainer.addProperty("globalWorkforceRevenue", globalWorkforceRevenueSum);
                mainContainer.addProperty("topPerformerName", topPerformerName);
                mainContainer.addProperty("totalWorkHours", Math.round(aggregatedWorkHours));
                mainContainer.addProperty("totalLateOccurrences", totalLateOccurrences);
                mainContainer.addProperty("totalPayroll", totalPayrollPool);

                // BỔ SUNG FIX 3: Tính tỷ lệ đúng giờ chỉ trên các ca ĐÃ XẢY RA
                double punctualityRate = pastOrActiveShiftsCount > 0 ? ((double)(pastOrActiveShiftsCount - totalLateOccurrences) / pastOrActiveShiftsCount) * 100.0 : 100.0;
                mainContainer.addProperty("punctualityRate", Math.max(0, Math.min(100, Math.round(punctualityRate))));

                JsonObject shiftsChartNode = new JsonObject();
                shiftsChartNode.addProperty("Ca Sáng", shiftMorningCount);
                shiftsChartNode.addProperty("Ca Chiều", shiftAfternoonCount);
                shiftsChartNode.addProperty("Ca Tối", shiftNightCount);
                mainContainer.add("shiftDistribution", shiftsChartNode);

                JsonObject dualChart = new JsonObject();
                dualChart.add("categories", chartCategories);
                dualChart.add("revenues", chartRevenues);
                dualChart.add("salaries", chartSalaries);
                mainContainer.add("dualBarChart", dualChart);

                mainContainer.add("employeeLeaderboard", leaderboardArray);
                mainContainer.add("employeeGridMatrix", datatableArray);

                // AI Insights
                JsonArray operationalInsights = new JsonArray();
                if (highestRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    operationalInsights.add("<i class='ti ti-flame' style='color:#f59e0b;margin-right:6px'></i> Chiến thần doanh số '" + topPerformerName + "' xuất sắc dẫn đầu chi nhánh.");
                }
                if (globalWorkforceRevenueSum.compareTo(BigDecimal.ZERO) > 0) {
                    double payrollRatio = (totalPayrollPool.doubleValue() / globalWorkforceRevenueSum.doubleValue()) * 100;
                    if (payrollRatio > 40) {
                        operationalInsights.add("<i class='ti ti-alert-triangle' style='color:#f43f5e;margin-right:6px'></i> BÁO ĐỘNG ĐỎ: Chi phí lương chiếm " + String.format("%.1f", payrollRatio) + "% doanh thu. Cần tối ưu lại lịch phân ca hoặc đẩy mạnh bán hàng!");
                    } else {
                        operationalInsights.add("<span class='emo'>✅</span> Tỷ suất Lương / Doanh thu đạt mức " + String.format("%.1f", payrollRatio) + "%. Hiệu suất chi phí đang được kiểm soát tốt.");
                    }
                }
                mainContainer.add("workforceInsights", operationalInsights);

                // BỔ SUNG FIX 4: Timeline logic chuẩn xác
                JsonArray timelineArray = new JsonArray();
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                tatCaCaThangNay.stream()
                        .filter(c -> c.getNgayLam() != null)
                        .sorted((c1, c2) -> {
                            int dateCompare = c2.getNgayLam().compareTo(c1.getNgayLam());
                            if (dateCompare != 0) return dateCompare;
                            if (c1.getThoiGianCheckIn() == null && c2.getThoiGianCheckIn() == null) return 0;
                            if (c1.getThoiGianCheckIn() == null) return 1;
                            if (c2.getThoiGianCheckIn() == null) return -1;
                            return c2.getThoiGianCheckIn().compareTo(c1.getThoiGianCheckIn());
                        })
                        .limit(8)
                        .forEach(shift -> {
                            JsonObject cell = new JsonObject();
                            String nvName = shift.getMaNV();
                            for (JsonObject s : allEmployeeStats) {
                                if (s.get("id").getAsString().equals(shift.getMaNV())) { nvName = s.get("name").getAsString(); break; }
                            }
                            cell.addProperty("staff", nvName);
                            
                            // Map lại tên ca cho đẹp (VD: LC01 -> Ca Sáng)
                            String hienThiCa = shift.getMaLoaiCa();
                            if (dsLoaiCa != null) {
                                for (Data.LoaiCa lc : dsLoaiCa) {
                                    // Áp dụng trim() ở đây nữa để timeline hiện đúng chữ "Ca Sáng", "Ca Chiều"
                                    if (lc.getMaLoaiCa() != null && lc.getMaLoaiCa().trim().equalsIgnoreCase(shift.getMaLoaiCa().trim())) {
                                        hienThiCa = lc.getTenCa(); 
                                        break;
                                    }
                                }
                            }
                            cell.addProperty("shiftId", shift.getMaCa() + " (" + hienThiCa + ")");
                            cell.addProperty("dateLabel", shift.getNgayLam().format(DateTimeFormatter.ofPattern("dd/MM")));
                            
                            String checkInStr = shift.getThoiGianCheckIn() != null ? shift.getThoiGianCheckIn().format(timeFormatter) : "--:--";
                            String checkOutStr = "";
                            
                            if (shift.getThoiGianCheckOut() != null) {
                                checkOutStr = shift.getThoiGianCheckOut().format(timeFormatter);
                            } else if (shift.getThoiGianCheckIn() != null) {
                                checkOutStr = "Đang ca";
                            } else if (shift.getNgayLam().isAfter(homNay)) {
                                checkOutStr = "Chưa tới giờ";
                            } else {
                                checkOutStr = "Vắng mặt";
                            }
                            
                            cell.addProperty("timeFrame", checkInStr + " → " + checkOutStr);
                            timelineArray.add(cell);
                        });
                mainContainer.add("shiftTimeline", timelineArray);

                lastCachedDashboardJson = gson.toJson(mainContainer);
                return lastCachedDashboardJson;
                
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
                if (webEngine != null) webEngine.executeScript(scriptText);
            } catch (Exception e) {}
        });
    }

    private void handleWebActionSignal(String actionToken) {
        SwingUtilities.invokeLater(() -> {
            if ("SYNC".equalsIgnoreCase(actionToken)) {
                pushLiveWorkforceAnalytics(true);
            } else if ("ADD_STAFF".equalsIgnoreCase(actionToken)) {
                if (actionCallback != null) actionCallback.moThemNhanVien();
            } else if ("ASSIGN_SHIFT".equalsIgnoreCase(actionToken)) {
                if (actionCallback != null) actionCallback.moPhanCa();
            } else if (actionToken.startsWith("VIEW_")) {
                
                // --- ĐÃ SỬA: Thay vì hiện thông báo popup, gọi callback ---
                String staffId = actionToken.substring(5);
                if (actionCallback != null) {
                    actionCallback.xemHoSoLuong(staffId);
                }
                
            }
        });
    }
    // =========================================================================
    // HÀM MAIN CHẠY TEST ĐỘC LẬP
    // =========================================================================
    public static void main(String[] args) {
        // Đảm bảo giao diện Swing được khởi chạy an toàn trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            JFrame devFrame = new JFrame("Workforce Intelligence - Standalone Test");
            devFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            devFrame.setSize(1440, 850); // Kích thước cửa sổ mô phỏng desktop
            devFrame.setLocationRelativeTo(null); // Hiển thị ở giữa màn hình

            // Khởi tạo Panel Nhân Viên và gắn vào Frame
            NhanVienPanel viewPanel = new NhanVienPanel();
            devFrame.add(viewPanel, BorderLayout.CENTER);
            
            // Hiển thị Frame
            devFrame.setVisible(true);
        });
    }
}