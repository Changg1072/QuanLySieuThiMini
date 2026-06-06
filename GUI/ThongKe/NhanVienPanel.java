package GUI.ThongKe;

import Dao.TruyVanSieuTocDAO;
import Dao.ChiaCaDAO;
import Data.ChiaCa;
import Data.NhanVien;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

import netscape.javascript.JSObject;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;

public class NhanVienPanel extends JPanel {

    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    private String lastCachedDashboardJson = null;
    
    // Mặc định khoảng thời gian lọc: Đầu tháng đến hôm nay
    private LocalDate filterStartDate = LocalDate.now().withDayOfMonth(1);
    private LocalDate filterEndDate = LocalDate.now();
    
    private JsBridge jsBridgeInstance;
    
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

            // 1. LẮNG NGHE TÍN HIỆU ACTION QUA ALERT (DỰ PHÒNG)
            webEngine.setOnAlert(event -> {
                String signal = event.getData();
                if (signal != null && signal.startsWith("ACTION:")) {
                    handleWebActionSignal(signal.substring(7));
                }
            });

            // 2. TIÊM JS BRIDGE KHI WEB TẢI XONG
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    try {
                        jsBridgeInstance = new JsBridge();
                        netscape.javascript.JSObject window = (netscape.javascript.JSObject) webEngine.executeScript("window");
                        window.setMember("javaConnector", jsBridgeInstance);

                        // Tự động load danh sách lịch sử khi mở trang
                        webEngine.executeScript(
                            "setTimeout(function(){ " +
                            "  if(typeof loadExportHistory === 'function') loadExportHistory(); " +
                            "}, 300);"
                        );

                        pushLiveWorkforceAnalytics(false);
                        reloadHistoryList(); // Ép load list từ DB
                        
                    } catch (Exception e) {
                        System.err.println("Lỗi tiêm javaConnector: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // 3. LOAD GIAO DIỆN HTML
            try {
                java.net.URL htmlUrl = getClass().getResource("nhanvien_dashboard.html");
                if (htmlUrl != null) {
                    webEngine.load(htmlUrl.toExternalForm());
                } else {
                    webEngine.loadContent("<html><body><h3>🚨 Lỗi tải giao diện</h3></body></html>");
                }
            } catch (Exception e) {
                System.err.println("[NhanVienPanel Bridge] Lỗi khởi tạo: " + e.getMessage());
            }

            javafx.scene.Scene scene = new javafx.scene.Scene(webView);
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
                
                // =========================================================================
                // 🔥 BƯỚC 1: LOAD TOÀN BỘ DỮ LIỆU LÊN RAM CHỈ VỚI VÀI CÂU QUERY (TỐI ƯU O(1))
                // Thay vì Query vào DB liên tục cho từng Nhân Viên, ta gom chung 1 lần!
                // =========================================================================
                NhanVienLogic nhanVienLogic = new NhanVienLogic();
                List<NhanVien> dsNhanVien = nhanVienLogic.layDanhSachNhanVien();
                
                Logic.LoaiCaLogic loaiCaLogic = new Logic.LoaiCaLogic();
                List<Data.LoaiCa> dsLoaiCa = loaiCaLogic.layDanhSachLoaiCa();
                
                TruyVanSieuTocDAO.DuLieuDonHangDTO ordersDTO = TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                
                // Lấy TOÀN BỘ ca làm trên hệ thống (Chỉ mất vài mili-giây)
                List<ChiaCa> toanBoCaLam = ChiaCaDAO.getInstance().layDanhSachChiaCa();
                
                // Gom Lương Cấu Hình vào Map (Lấy từ bảng CauHinhLuong)
                Map<String, BigDecimal> mapLuongGio = new HashMap<>();
                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                     java.sql.PreparedStatement ps = con.prepareStatement("SELECT MaNV, LuongTheoGio FROM CauHinhLuong WHERE TrangThai = N'Đang áp dụng'");
                     java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        mapLuongGio.put(rs.getString("MaNV"), rs.getBigDecimal("LuongTheoGio"));
                    }
                } catch (Exception ignored) {}

                // Gom Nhóm Hóa Đơn vào Map theo Mã NV (Lọc trước theo thời gian)
                Map<String, List<Data.HoaDon>> mapHoaDonTheoNV = new HashMap<>();
                if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                    for (Data.HoaDon hd : ordersDTO.dsHoaDon) {
                        LocalDate ngayTao = hd.getNgayTao() != null ? hd.getNgayTao().toLocalDate() : null;
                        if (ngayTao == null || ngayTao.isBefore(filterStartDate) || ngayTao.isAfter(filterEndDate)) continue;
                        mapHoaDonTheoNV.computeIfAbsent(hd.getMaNV(), k -> new ArrayList<>()).add(hd);
                    }
                }

                // Gom Nhóm Ca Làm vào Map theo Mã NV (Lọc trước theo thời gian)
                Map<String, List<ChiaCa>> mapCaLamTheoNV = new HashMap<>();
                if (toanBoCaLam != null) {
                    for (ChiaCa cc : toanBoCaLam) {
                        if ("Đã hủy".equalsIgnoreCase(cc.getTinhTrang())) continue;
                        LocalDate ngayLam = cc.getNgayLam();
                        if (ngayLam == null || ngayLam.isBefore(filterStartDate) || ngayLam.isAfter(filterEndDate)) continue;
                        mapCaLamTheoNV.computeIfAbsent(cc.getMaNV(), k -> new ArrayList<>()).add(cc);
                    }
                }

                LocalDate homNay = LocalDate.now();

                int totalEmployeesCount = dsNhanVien.size();
                int activeStaffCount = 0;
                BigDecimal globalWorkforceRevenueSum = BigDecimal.ZERO;
                BigDecimal totalPayrollPool = BigDecimal.ZERO;
                
                int totalLateOccurrences = 0;
                double aggregatedWorkHours = 0.0;
                int shiftMorningCount = 0, shiftAfternoonCount = 0, shiftNightCount = 0;
                int pastOrActiveShiftsCount = 0;

                List<ChiaCa> tatCaCaThangNay = new ArrayList<>();
                List<JsonObject> allEmployeeStats = new ArrayList<>();

                // =========================================================================
                // 🔥 BƯỚC 2: TÍNH TOÁN HOÀN TOÀN TRÊN RAM BẰNG THUẬT TOÁN HASHMAP
                // Quá trình này không có bất kỳ lệnh Database nào -> Tốc độ ánh sáng!
                // =========================================================================
                for (NhanVien nv : dsNhanVien) {
                    activeStaffCount++;
                    String empId = nv.getMaNV();

                    // --- Tính Doanh Số (Lấy từ RAM) ---
                    BigDecimal staffRevenue = BigDecimal.ZERO;
                    int invoiceCount = 0;
                    List<Data.HoaDon> listHD = mapHoaDonTheoNV.getOrDefault(empId, new ArrayList<>());
                    for (Data.HoaDon hd : listHD) {
                        if (hd.getThanhTien() != null) {
                            staffRevenue = staffRevenue.add(hd.getThanhTien());
                            invoiceCount++;
                        }
                    }
                    globalWorkforceRevenueSum = globalWorkforceRevenueSum.add(staffRevenue);

                    // --- Tính Giờ Làm, Số lần Trễ, Tiền Phạt (Lấy từ RAM) ---
                    List<ChiaCa> dsCaCuaNV = mapCaLamTheoNV.getOrDefault(empId, new ArrayList<>());
                    
                    double staffFilteredHours = 0.0;
                    int staffFilteredLateCount = 0;
                    BigDecimal staffTotalPenalty = BigDecimal.ZERO;

                    for (ChiaCa shift : dsCaCuaNV) {
                        tatCaCaThangNay.add(shift);

                        // Phân loại ca Sáng/Chiều/Tối
                        boolean daPhanLoai = false;
                        Data.LoaiCa loaiCaHienTai = null;
                        String maCaCuaShift = (shift.getMaLoaiCa() != null) ? shift.getMaLoaiCa().trim() : "";

                        if (dsLoaiCa != null && !maCaCuaShift.isEmpty()) {
                            for (Data.LoaiCa lc : dsLoaiCa) {
                                if (lc.getMaLoaiCa() != null && lc.getMaLoaiCa().trim().equalsIgnoreCase(maCaCuaShift)) {
                                    loaiCaHienTai = lc;
                                    if (lc.getGioBatDau() != null) {
                                        int gioBatDau = lc.getGioBatDau().getHour();
                                        if (gioBatDau >= 4 && gioBatDau < 12) shiftMorningCount++;
                                        else if (gioBatDau >= 12 && gioBatDau < 18) shiftAfternoonCount++;
                                        else shiftNightCount++;
                                        daPhanLoai = true;
                                    }
                                    break;
                                }
                            }
                        }
                        if (!daPhanLoai) shiftNightCount++;

                        if (shift.getNgayLam() != null && !shift.getNgayLam().isAfter(homNay)) {
                            pastOrActiveShiftsCount++;
                        }

                        // Tính giờ làm thực tế
                        if (shift.getThoiGianCheckIn() != null && shift.getThoiGianCheckOut() != null) {
                            long minutes = java.time.Duration.between(shift.getThoiGianCheckIn(), shift.getThoiGianCheckOut()).toMinutes();
                            if (minutes > 0) staffFilteredHours += (minutes / 60.0);
                        }

                        // Tính Đi Trễ & Phạt (Thuật toán phạt bậc thang)
                        if (shift.getThoiGianCheckIn() != null && loaiCaHienTai != null && loaiCaHienTai.getGioBatDau() != null) {
                            if (shift.getThoiGianCheckIn().toLocalTime().isAfter(loaiCaHienTai.getGioBatDau())) {
                                long phutDiMuon = java.time.Duration.between(loaiCaHienTai.getGioBatDau(), shift.getThoiGianCheckIn().toLocalTime()).toMinutes();
                                if (phutDiMuon > 5) { // Cho phép du di 5 phút
                                    staffFilteredLateCount++;
                                    if (phutDiMuon <= 15) {
                                        staffTotalPenalty = staffTotalPenalty.add(new BigDecimal("20000")); // 20k
                                    } else if (phutDiMuon <= 30) {
                                        staffTotalPenalty = staffTotalPenalty.add(new BigDecimal("50000")); // 50k
                                    } else {
                                        staffTotalPenalty = staffTotalPenalty.add(new BigDecimal("100000")); // 100k
                                    }
                                }
                            }
                        }
                    }

                    aggregatedWorkHours += staffFilteredHours;
                    totalLateOccurrences += staffFilteredLateCount;

                    // --- Tính Tiền Lương ---
                    BigDecimal hourlyWage = mapLuongGio.getOrDefault(empId, BigDecimal.ZERO);
                    BigDecimal estimatedSalary = hourlyWage.multiply(BigDecimal.valueOf(staffFilteredHours)).setScale(0, RoundingMode.HALF_UP);
                    
                    totalPayrollPool = totalPayrollPool.add(estimatedSalary);

                    JsonObject stats = new JsonObject();
                    stats.addProperty("id", empId);
                    stats.addProperty("name", nv.getHoTen());
                    stats.addProperty("role", nv.getChucVu());
                    stats.addProperty("status", nv.getTrangThai());
                    stats.addProperty("phone", nv.getSDT() != null ? nv.getSDT() : "---");
                    stats.addProperty("workHours", Math.round(staffFilteredHours * 10.0) / 10.0);
                    stats.addProperty("revenue", staffRevenue);
                    stats.addProperty("salary", estimatedSalary);
                    stats.addProperty("lateCount", staffFilteredLateCount);
                    stats.addProperty("penalty", staffTotalPenalty);
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

                mainContainer.addProperty("totalEmployees", totalEmployeesCount);
                mainContainer.addProperty("activeStaffCount", activeStaffCount); 
                mainContainer.addProperty("globalWorkforceRevenue", globalWorkforceRevenueSum);
                mainContainer.addProperty("topPerformerName", topPerformerName);
                mainContainer.addProperty("totalWorkHours", Math.round(aggregatedWorkHours));
                mainContainer.addProperty("totalLateOccurrences", totalLateOccurrences);
                mainContainer.addProperty("totalPayroll", totalPayrollPool);

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
                            
                            String hienThiCa = shift.getMaLoaiCa();
                            if (dsLoaiCa != null && hienThiCa != null) {
                                for (Data.LoaiCa lc : dsLoaiCa) {
                                    if (lc.getMaLoaiCa() != null && lc.getMaLoaiCa().trim().equalsIgnoreCase(shift.getMaLoaiCa().trim())) {
                                        hienThiCa = lc.getTenCa(); 
                                        break;
                                    }
                                }
                            }
                            cell.addProperty("shiftId", shift.getMaCa() + " (" + hienThiCa + ")");
                            cell.addProperty("dateLabel", shift.getNgayLam().format(DateTimeFormatter.ofPattern("dd/MM")));
                            
                            String checkInStr = shift.getThoiGianCheckIn() != null ? shift.getThoiGianCheckIn().format(timeFormatter) : "--:--";
                            String checkOutStr;
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
            } catch (Exception e) {
                System.err.println("[JS Error] " + e.getMessage());
            }
        });
    }

    private void handleWebActionSignal(String action) {
        if (action.startsWith("DATE_SYNC|")) {
            try {
                String[] parts = action.split("\\|");
                this.filterStartDate = LocalDate.parse(parts[1]);
                this.filterEndDate = LocalDate.parse(parts[2]);
                pushLiveWorkforceAnalytics(true);
            } catch (Exception e) { e.printStackTrace(); }
        } 
        else if (action.equals("EXIT_HISTORY")) {
            this.filterStartDate = LocalDate.now().withDayOfMonth(1);
            this.filterEndDate = LocalDate.now();
            pushLiveWorkforceAnalytics(true);
        }
        else if (action.equals("LOAD_HISTORY_LIST")) {
            reloadHistoryList();
        }
        else if (action.startsWith("LOAD_HISTORY_FILE|")) {
            String fileName = action.substring("LOAD_HISTORY_FILE|".length());
            jsBridgeInstance.readAndLoadExportFile(fileName); // Chuyển cho JsBridge đọc DB
        }
        else if (action.startsWith("EXPORT|")) {
            String fileName = action.split("\\|")[1];
            jsBridgeInstance.exportDashboardData(fileName); // Chuyển cho JsBridge lưu DB
        }
    }

    public class JsBridge {

        // LƯU LỊCH SỬ VÀO DATABASE
        public void exportDashboardData(String customFileName) {
            CompletableFuture.runAsync(() -> {
                try {
                    String finalFileName = customFileName; 
                    if (finalFileName == null || finalFileName.trim().isEmpty()) {
                        finalFileName = "Thong_Ke_Nhan_Vien_" + System.currentTimeMillis() + ".json";
                    } else {
                        finalFileName = finalFileName.replace(".xlsx", ".json");
                    }

                    if (lastCachedDashboardJson == null || lastCachedDashboardJson.trim().isEmpty()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(NhanVienPanel.this,
                                "Dữ liệu chưa được tải! Hãy đợi dashboard load xong.",
                                "Chưa Có Dữ Liệu", JOptionPane.WARNING_MESSAGE));
                        return;
                    }

                    // Gọi DAO để lưu vào Database chung
                    Dao.LichSuThongKeDAO.getInstance().luuLichSu("NHAN_VIEN", finalFileName, lastCachedDashboardJson);

                    String successMsg = finalFileName; 
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(NhanVienPanel.this,
                            "✅ Đã xuất dữ liệu thành công vào cơ sở dữ liệu!\n\n" +
                            "📄 Tên bản ghi: " + successMsg + "\n\n" +
                            "Bạn có thể xem lại ngay qua Dropdown Lịch Sử.",
                            "Xuất Báo Cáo Hoàn Tất", JOptionPane.INFORMATION_MESSAGE));
                    
                    reloadHistoryList(); // Kích hoạt làm mới dropdown trên Web

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(NhanVienPanel.this,
                            "❌ Lỗi ghi Database:\n" + ex.getMessage(), "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE));
                    ex.printStackTrace();
                }
            });
        }

        public String getExportHistoryList() {
            try {
                // 1. Lấy chuỗi JSON đối tượng từ Database
                String daoJson = Dao.LichSuThongKeDAO.getInstance().layDanhSachLichSu("NHAN_VIEN");
                JsonArray daoArr = gson.fromJson(daoJson, JsonArray.class);
                
                // 2. Trích xuất chỉ lấy phần "name" đưa vào mảng chuỗi đơn giản giống hệt ổ D ngày xưa
                JsonArray simpleArr = new JsonArray();
                if (daoArr != null) {
                    for (int i = 0; i < daoArr.size(); i++) {
                        JsonObject obj = daoArr.get(i).getAsJsonObject();
                        if (obj.has("name")) {
                            simpleArr.add(obj.get("name").getAsString());
                        }
                    }
                }
                return simpleArr.toString(); // Trả về dạng ["file1.json", "file2.json"]
            } catch (Exception e) {
                System.err.println("[JsBridge] getExportHistoryList lỗi: " + e.getMessage());
                return "[]";
            }
        }

        public void readAndLoadExportFile(String fileName) {
            CompletableFuture.runAsync(() -> {
                try {
                    String safeFileName = new java.io.File(fileName).getName();
                    
                    // Gọi DAO lấy Base64 trực tiếp từ SQL Server
                    String base64Data = Dao.LichSuThongKeDAO.getInstance().docNoiDungLichSuBase64(safeFileName);
                    
                    if (base64Data == null || base64Data.isEmpty()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(NhanVienPanel.this,
                                "Bản ghi không tồn tại trong hệ thống Database: " + safeFileName, "Lỗi", JOptionPane.ERROR_MESSAGE));
                        return;
                    }

                    Platform.runLater(() -> {
                        try {
                            webEngine.executeScript("applyHistoricalStateBase64('" + base64Data + "', '" + safeFileName.replace("'", "\\'") + "')");
                        } catch (Exception ex) {
                            System.err.println("[JsBridge] Lỗi render file lịch sử: " + ex.getMessage());
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(NhanVienPanel.this,
                            "Lỗi đọc dữ liệu lịch sử từ Database:\n" + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
                    e.printStackTrace();
                }
            });
        }
    }
    private void reloadHistoryList() {
        Platform.runLater(() -> {
            try {
                if (webEngine != null) {
                    // Ép phía giao diện web chạy lại hàm nạp lịch sử để đọc mảng dữ liệu mới từ JsBridge
                    webEngine.executeScript("if(typeof loadExportHistory === 'function') loadExportHistory();");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame devFrame = new JFrame("Workforce Intelligence - Standalone Test");
            devFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            devFrame.setSize(1440, 850);
            devFrame.setLocationRelativeTo(null);

            NhanVienPanel viewPanel = new NhanVienPanel();
            devFrame.add(viewPanel, BorderLayout.CENTER);
            devFrame.setVisible(true);
        });
    }
}
