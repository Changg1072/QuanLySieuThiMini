package GUI.ThongKe;

import Dao.TruyVanSieuTocDAO;
import GUI.KiemKeGUI;
import GUI.TaoPhieuNhapUi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public class KhoPanel extends JPanel {
    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    private TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO cachedData;
    private LocalDate filterStartDate = LocalDate.now().withDayOfMonth(1);
    private LocalDate filterEndDate = LocalDate.now();
    public KhoPanel() {
        setLayout(new BorderLayout());
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);
        initJavaFX();
    }
    
    public interface KhoPanelCallback {
        void moNhapHang();
        void moKiemKe();
    }

    private KhoPanelCallback callback;

    public void setCallback(KhoPanelCallback callback) {
        this.callback = callback;
    }

    private void initJavaFX() {
        Platform.runLater(() -> {
            WebView webView = new WebView();
            webEngine = webView.getEngine();
            webEngine.setJavaScriptEnabled(true);

            // 1. BẮT SỰ KIỆN NÚT BẤM TỪ JS (NHẬP HÀNG, KIỂM KÊ...)
            webEngine.setOnAlert(event -> {
                String action = event.getData();
                System.out.println("📢 Tín hiệu từ Web gửi lên Java: " + action);

                if (action.startsWith("ACTION:DATE_SYNC|")) {
                    try {
                        String[] parts = action.split("\\|");
                        this.filterStartDate = LocalDate.parse(parts[1]);
                        this.filterEndDate = LocalDate.parse(parts[2]);
                        System.out.println("✅ Java đã nhận ngày mới: " + filterStartDate + " ĐẾN " + filterEndDate);
                        refreshDashboardData(true); 
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi đọc ngày: " + e.getMessage());
                    }
                } 
                // =========================================================
                // 🔥 LOGIC TẢI DANH SÁCH LỊCH SỬ KHO
                // =========================================================
                else if (action.equals("ACTION:LOAD_HISTORY_LIST")) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            // Truy vấn danh sách từ SQL thay vì quét thư mục
                            String jsonArrayData = Dao.LichSuThongKeDAO.getInstance().layDanhSachLichSu("KHO");
                            
                            // Lưu ý: Nếu web báo lỗi không tìm thấy hàm, hãy đổi updateHistoryDropdown thành populateHistoryDropdown tùy theo file JS của bạn
                            Platform.runLater(() -> webEngine.executeScript("updateHistoryDropdown('" + jsonArrayData + "')"));
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                // =========================================================
                // 🔥 LOGIC XUẤT FILE MỚI
                // =========================================================
                else if (action.startsWith("ACTION:EXPORT_KHO|")) {
                    String jsonPayload = action.substring("ACTION:EXPORT_KHO|".length());
                    CompletableFuture.runAsync(() -> {
                        try {
                            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
                            String fileName = "ThongKe_Kho_" + filterStartDate.format(fmt) + "_den_" + filterEndDate.format(fmt) + ".json";

                            // Ghi thẳng chuỗi JSON xuống SQL Server
                            Dao.LichSuThongKeDAO.getInstance().luuLichSu("KHO", fileName, jsonPayload);
                            
                            Platform.runLater(() -> {
                                JOptionPane.showMessageDialog(this, "Đã lưu bản Thống kê Kho thành công vào Database:\n" + fileName, "Xuất Lịch Sử Thành Công", JOptionPane.INFORMATION_MESSAGE);
                                webEngine.executeScript("alert('ACTION:LOAD_HISTORY_LIST')"); // Yêu cầu JS tự động load lại danh sách
                            });
                        } catch (Exception e) { 
                            e.printStackTrace(); 
                            Platform.runLater(() -> JOptionPane.showMessageDialog(this, "Lỗi khi lưu Database: " + e.getMessage(), "Lỗi Xuất File", JOptionPane.ERROR_MESSAGE));
                        }
                    });
                }
                // =========================================================
                // 🔥 ĐÂY LÀ ĐOẠN ĐỌC FILE BỊ MẤT TÍCH LÚC NÃY
                // =========================================================
                else if (action.startsWith("ACTION:LOAD_HISTORY_FILE|")) {
                    String tenFile = action.substring("ACTION:LOAD_HISTORY_FILE|".length());
                    CompletableFuture.runAsync(() -> {
                        try {
                            // Lấy chuỗi Base64 từ SQL Server
                            String base64 = Dao.LichSuThongKeDAO.getInstance().docNoiDungLichSuBase64(tenFile);
                            if (base64 != null) {
                                String safeName = tenFile.replace("'", "\\'");
                                Platform.runLater(() -> webEngine.executeScript("applyHistoricalStateBase64('" + base64 + "', '" + safeName + "')"));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                // =========================================================
                // THOÁT LỊCH SỬ & CÁC NÚT ĐIỀU HƯỚNG
                // =========================================================
                else if (action.equals("ACTION:EXIT_HISTORY")) {
                    // 🔥 RESET NGÀY VỀ MẶC ĐỊNH KHI THOÁT CHẾ ĐỘ LỊCH SỬ
                    this.filterStartDate = LocalDate.now().withDayOfMonth(1);
                    this.filterEndDate = LocalDate.now();
                    refreshDashboardData(true);
                }
                else if (action.equals("ACTION:IMPORT")) {
                    if (callback != null) callback.moNhapHang();
                } 
                else if (action.equals("ACTION:AUDIT")) {
                    if (callback != null) callback.moKiemKe();
                } 
                else if (action.startsWith("ACTION:VIEW_")) {
                    String maSP = action.replace("ACTION:VIEW_", "").trim();
                    
                    if (cachedData != null) {
                        // 1. Tìm sản phẩm trong bộ nhớ RAM
                        Data.SanPham spTarget = null;
                        for (Data.SanPham sp : cachedData.dsSanPham) {
                            if (sp.getMaSP().trim().equalsIgnoreCase(maSP)) {
                                spTarget = sp;
                                break;
                            }
                        }
                        
                        // 2. Nếu tìm thấy, mở Popup
                        if (spTarget != null) {
                            int tonKhoHienTai = cachedData.mapTongTonKho.getOrDefault(maSP, 0);
                            Data.SanPham finalSp = spTarget; // Đóng gói để dùng trong luồng Lambda
                            
                            // Chuyển về luồng Swing để vẽ UI
                            SwingUtilities.invokeLater(() -> {
                                GUI.HoTro.ChiTietSanPham.showModal(
                                    KhoPanel.this, 
                                    finalSp, 
                                    tonKhoHienTai, 
                                    () -> refreshDashboardData(true) // TỰ ĐỘNG TẢI LẠI TRANG KHI ĐÓNG POPUP
                                );
                            });
                        } else {
                            JOptionPane.showMessageDialog(KhoPanel.this, "Không tìm thấy dữ liệu cho mã hàng: " + maSP, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

            // 2. KHI WEB TẢI XONG -> GỌI JAVA KÉO DỮ LIỆU ĐỔ VÀO MÀN HÌNH
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    refreshDashboardData(false);
                    reloadHistoryList();
                }
            });

            // 3. TẢI FILE GIAO DIỆN
            URL url = getClass().getResource("kho_dashboard.html");
            if (url != null) {
                webEngine.load(url.toExternalForm());
            } else {
                System.err.println("Lỗi: Không tìm thấy file kho_dashboard.html trong thư mục GUI/ThongKe/");
            }
            
            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }

    // =========================================================================
    // 🔥 ĐỘNG CƠ XỬ LÝ SIÊU TỐC TRÊN RAM BẰNG THUẬT TOÁN HASHMAP
    // =========================================================================
    public void refreshDashboardData(boolean forceRefresh) {
        // Chốt cứng mốc thời gian lọc để đảm bảo an toàn cho luồng Lambda (Đa luồng)
        final LocalDate start = this.filterStartDate;
        final LocalDate end = this.filterEndDate;

        CompletableFuture.supplyAsync(() -> {
            try {
                // Tải dữ liệu tồn kho hiện tại
                if (forceRefresh || cachedData == null) {
                    cachedData = Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuKiemKeSieuToc();
                }
                
                // 🌟 GỌI BỘ MÁY HỒI QUY 2 LẦN: Lấy số liệu chốt sổ Đầu Kỳ & Cuối Kỳ
                // 1. Lùi về thời điểm cuối kỳ (End Date)
                Dao.TruyVanSieuTocDAO.DuLieuHoiQuyDTO hoiQuyEnd = Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuHoiQuyKho(end);
                // 2. Lùi về thời điểm đầu kỳ (Trước Start Date 1 ngày)
                Dao.TruyVanSieuTocDAO.DuLieuHoiQuyDTO hoiQuyStart = Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuHoiQuyKho(start.minusDays(1));

                JsonObject json = new JsonObject();
                LocalDate homNay = LocalDate.now();
                
                // Tổng kết Cuối Kỳ
                int totalStockEnd = 0;
                BigDecimal warehouseValueEnd = BigDecimal.ZERO;
                
                // Tổng kết Đầu Kỳ (Dùng để so sánh Tăng/Giảm)
                int totalStockStart = 0;
                BigDecimal warehouseValueStart = BigDecimal.ZERO;

                int lowStockCount = 0;
                int outOfStockCount = 0;
                java.util.List<String> outOfStockNames = new java.util.ArrayList<>();
                java.util.List<String> lowStockNames = new java.util.ArrayList<>();
                JsonObject categoryData = new JsonObject();
                JsonArray table = new JsonArray();

                // =====================================================================
                // 1. ÁP DỤNG CÔNG THỨC HỒI QUY CHO TỪNG SẢN PHẨM
                // =====================================================================
                for (Data.SanPham sp : cachedData.dsSanPham) {
                    String maSP = sp.getMaSP().trim();
                    
                    // --- BƯỚC A: CHỐT SỐ LIỆU TỒN VÀ GIÁ TRỊ TẠI ĐÚNG THỜI ĐIỂM HIỆN TẠI ---
                    int tonHienTai = cachedData.mapTongTonKho.getOrDefault(maSP, 0);
                    BigDecimal giaTriHienTai = BigDecimal.ZERO;
                    
                    java.util.List<Data.ChiTietLoHang> dsLo = cachedData.mapDanhSachLo.get(maSP);
                    if (dsLo != null) {
                        for (Data.ChiTietLoHang lo : dsLo) {
                            if (lo.getGiaNhap() != null && lo.getSoLuongTon() > 0) {
                                giaTriHienTai = giaTriHienTai.add(lo.getGiaNhap().multiply(new BigDecimal(lo.getSoLuongTon())));
                            }
                        }
                    }

                    // --- BƯỚC B1: TÍNH TỒN VÀ GIÁ TRỊ CUỐI KỲ (END) ---
                    int xuatE    = hoiQuyEnd.mapXuatQty.getOrDefault(maSP, 0);
                    int huyE     = hoiQuyEnd.mapHuyQty.getOrDefault(maSP, 0);
                    int nhapE    = hoiQuyEnd.mapNhapQty.getOrDefault(maSP, 0);
                    int traE     = hoiQuyEnd.mapTraHangQty.getOrDefault(maSP, 0);
                    
                    BigDecimal valXuatE    = hoiQuyEnd.mapXuatVal.getOrDefault(maSP, BigDecimal.ZERO);
                    BigDecimal valHuyE     = hoiQuyEnd.mapHuyVal.getOrDefault(maSP, BigDecimal.ZERO);
                    BigDecimal valNhapE    = hoiQuyEnd.mapNhapVal.getOrDefault(maSP, BigDecimal.ZERO);
                    BigDecimal valTraE     = hoiQuyEnd.mapTraHangVal.getOrDefault(maSP, BigDecimal.ZERO);
                    
                    int tonCuoiKy = Math.max(0, tonHienTai + xuatE + huyE - nhapE - traE);
                    BigDecimal giaTriCuoiKy = giaTriHienTai.add(valXuatE).add(valHuyE).subtract(valNhapE).subtract(valTraE);
                    if (giaTriCuoiKy.compareTo(BigDecimal.ZERO) < 0) giaTriCuoiKy = BigDecimal.ZERO;

                    // --- BƯỚC B2: TÍNH TỒN VÀ GIÁ TRỊ ĐẦU KỲ (START) ---
                    int xuatS    = hoiQuyStart.mapXuatQty.getOrDefault(maSP, 0);
                    int huyS     = hoiQuyStart.mapHuyQty.getOrDefault(maSP, 0);
                    int nhapS    = hoiQuyStart.mapNhapQty.getOrDefault(maSP, 0);
                    int traS     = hoiQuyStart.mapTraHangQty.getOrDefault(maSP, 0);
                    
                    BigDecimal valXuatS    = hoiQuyStart.mapXuatVal.getOrDefault(maSP, BigDecimal.ZERO);
                    BigDecimal valHuyS     = hoiQuyStart.mapHuyVal.getOrDefault(maSP, BigDecimal.ZERO);
                    BigDecimal valNhapS    = hoiQuyStart.mapNhapVal.getOrDefault(maSP, BigDecimal.ZERO);
                    BigDecimal valTraS     = hoiQuyStart.mapTraHangVal.getOrDefault(maSP, BigDecimal.ZERO);
                    
                    int tonDauKy = Math.max(0, tonHienTai + xuatS + huyS - nhapS - traS);
                    BigDecimal giaTriDauKy = giaTriHienTai.add(valXuatS).add(valHuyS).subtract(valNhapS).subtract(valTraS);
                    if (giaTriDauKy.compareTo(BigDecimal.ZERO) < 0) giaTriDauKy = BigDecimal.ZERO;

                    // --- ĐẨY DỮ LIỆU VÀO TỔNG ---
                    totalStockEnd += tonCuoiKy;
                    warehouseValueEnd = warehouseValueEnd.add(giaTriCuoiKy);
                    
                    totalStockStart += tonDauKy;
                    warehouseValueStart = warehouseValueStart.add(giaTriDauKy);
                    if (tonCuoiKy == 0) {
                        outOfStockCount++; 
                        if (outOfStockNames.size() < 3) outOfStockNames.add(sp.getTenSP().trim());
                    } else if (tonCuoiKy <= 10) {
                        lowStockCount++; 
                        if (lowStockNames.size() < 3) lowStockNames.add(sp.getTenSP().trim());
                    }

                    // Xử lý cơ cấu biểu đồ Donut
                    String tenLoai = (cachedData.mapTenLoai != null && cachedData.mapTenLoai.containsKey(sp.getMaLoai())) 
                                     ? cachedData.mapTenLoai.get(sp.getMaLoai()) : sp.getMaLoai();
                    categoryData.addProperty(tenLoai, categoryData.has(tenLoai) ? categoryData.get(tenLoai).getAsInt() + tonCuoiKy : tonCuoiKy);
                    
                    // Đẩy dữ liệu vào Bảng (Hiển thị tồn kho cuối kỳ)
                    JsonObject row = new JsonObject();
                    row.addProperty("id", maSP);
                    row.addProperty("name", sp.getTenSP());
                    row.addProperty("category", tenLoai);
                    row.addProperty("stock", tonCuoiKy); 
                    row.addProperty("price", sp.getGiaBan());
                    table.add(row);
                }

                // Gửi số liệu ra JSON
                json.addProperty("totalStock", totalStockEnd);
                json.addProperty("totalStockStart", totalStockStart);
                json.addProperty("warehouseValue", warehouseValueEnd);
                json.addProperty("warehouseValueStart", warehouseValueStart);
                
                json.addProperty("totalSkus", cachedData.dsSanPham.size());
                json.addProperty("lowStockCount", lowStockCount);
                json.add("categoryDistribution", categoryData);
                json.add("inventoryTable", table);

                // =====================================================================
                // 2. CHI PHÍ & SỐ LƯỢNG TIÊU HỦY TRONG KHOẢNG NGÀY ĐÃ CHỌN
                // =====================================================================
                BigDecimal destructionCost = BigDecimal.ZERO;
                int destructionQty = 0;
                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection()) {
                    String sqlHuy = "SELECT SUM(TongGiaTriHuy), SUM(TongSoLuong) FROM PhieuTieuHuy WHERE NgayTao >= ? AND NgayTao < ?";
                    try (java.sql.PreparedStatement ps1 = con.prepareStatement(sqlHuy)) {
                        ps1.setTimestamp(1, java.sql.Timestamp.valueOf(start.atStartOfDay()));
                        ps1.setTimestamp(2, java.sql.Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
                        try (java.sql.ResultSet rs1 = ps1.executeQuery()) {
                            if (rs1.next()) {
                                String valCost = rs1.getString(1);
                                if (valCost != null) destructionCost = new BigDecimal(valCost);
                                destructionQty = rs1.getInt(2);
                            }
                        }
                    }
                } catch (Exception e) {} 
                json.addProperty("destructionCost", destructionCost);
                json.addProperty("destructionQty", destructionQty);

                // =====================================================================
                // 3. BẢNG TIN CẢNH BÁO TỰ ĐỘNG (Lọc HSD theo khoảng ngày chọn)
                // =====================================================================
                JsonArray alertsArray = new JsonArray();
                
                // --- 3.1 Cảnh báo Tồn kho (Kèm tên sản phẩm) ---
                if (outOfStockCount > 0) {
                    String names = String.join(", ", outOfStockNames);
                    if (outOfStockCount > 3) names += " và " + (outOfStockCount - 3) + " SP khác";
                    alertsArray.add("BÁO ĐỘNG ĐỎ: Có " + outOfStockCount + " mặt hàng ĐÃ HẾT SẠCH (" + names + "). Cần nhập khẩn cấp!");
                }
                if (lowStockCount > 0) {
                    String names = String.join(", ", lowStockNames);
                    if (lowStockCount > 3) names += " và " + (lowStockCount - 3) + " SP khác";
                    alertsArray.add("CẢNH BÁO: Phát hiện " + lowStockCount + " mặt hàng sắp hết (" + names + ").");
                }

                // --- 3.2 Cảnh báo Hạn Sử Dụng (Kèm tên sản phẩm) ---
                int hethan = 0, saphethan = 0;
                java.util.List<String> expiredNames = new java.util.ArrayList<>();
                java.util.List<String> expiringNames = new java.util.ArrayList<>();
                
                for (Data.SanPham sp : cachedData.dsSanPham) {
                    java.util.List<Data.ChiTietLoHang> dsLo = cachedData.mapDanhSachLo.get(sp.getMaSP());
                    if (dsLo != null) {
                        for (Data.ChiTietLoHang lo : dsLo) {
                            if (lo.getHSD() != null && lo.getSoLuongTon() > 0 && !lo.getHSD().isBefore(start) && !lo.getHSD().isAfter(end)) {
                                if (lo.getHSD().isBefore(homNay)) {
                                    hethan++;
                                    if (!expiredNames.contains(sp.getTenSP()) && expiredNames.size() < 3) expiredNames.add(sp.getTenSP().trim());
                                }
                                else if (lo.getHSD().isBefore(homNay.plusDays(30))) {
                                    saphethan++;
                                    if (!expiringNames.contains(sp.getTenSP()) && expiringNames.size() < 3) expiringNames.add(sp.getTenSP().trim());
                                }
                            }
                        }
                    }
                }
                
                if (hethan > 0) {
                    String names = String.join(", ", expiredNames);
                    if (hethan > 3) names += " và " + (hethan - 3) + " lô khác";
                    alertsArray.add("BÁO ĐỘNG ĐỎ: Có " + hethan + " lô hàng TRONG KHO ĐÃ HẾT HẠN (" + names + "). Đề nghị tiêu hủy ngay!");
                }
                if (saphethan > 0) {
                    String names = String.join(", ", expiringNames);
                    if (saphethan > 3) names += " và " + (saphethan - 3) + " lô khác";
                    alertsArray.add("CẢNH BÁO: Có " + saphethan + " lô hàng sắp hết hạn trong 30 ngày tới (" + names + ").");
                }

                // --- 3.3 Cảnh báo Kiểm Kê (LOẠI TRỪ PHIẾU GIẢI TRÌNH VÀ PHIẾU GỐC) ---
                int lechKhoAm = 0;    
                int lechKhoDuong = 0; 
                int daXuLy = 0; 
                
                java.util.List<String> amNames = new java.util.ArrayList<>();
                java.util.List<String> duongNames = new java.util.ArrayList<>();
                java.util.List<String> daXuLyNames = new java.util.ArrayList<>();

                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection()) {
                    // 🔥 THÊM RTRIM(k.MaKiemKe) để chống lỗi dư khoảng trắng
                    String sqlKiemKe = 
                        "SELECT k.MaKiemKe, k.SoLuongHeThong, k.SoLuongThucTe, s.TenSP, k.NgayKiemKe, " +
                        "CASE " +
                        // 1. Bản thân nó là phiếu ĐI GIẢI TRÌNH (Chứa chữ KK, Bù, Nhận, Đã kiểm tra)
                        "    WHEN k.LyDo IS NOT NULL AND (" +
                        "         k.LyDo LIKE '%KK%' " +
                        "      OR k.LyDo LIKE N'%Đã kiểm tra%' " +
                        "      OR k.LyDo LIKE N'%kiểm kê lại%' " +
                        "      OR k.LyDo LIKE N'%Bù %' " +
                        "      OR k.LyDo LIKE N'%Nhận %'" +
                        "    ) THEN 1 " +
                        // 2. Nó là phiếu GỐC, nhưng bị phiếu khác réo tên (Đã dùng RTRIM gọt chữ)
                        "    WHEN EXISTS ( " +
                        "        SELECT 1 FROM KiemKeKho k2 " +
                        "        WHERE k2.LyDo LIKE '%' + RTRIM(k.MaKiemKe) + '%' " +
                        "        AND k2.MaKiemKe != k.MaKiemKe " + 
                        "    ) THEN 1 " +
                        "    ELSE 0 " +
                        "END AS IsResolved " +
                        "FROM KiemKeKho k JOIN SanPham s ON k.MaSP = s.MaSP " +
                        "WHERE k.NgayKiemKe < ? " + // 🔥 ĐỔI TỪ <= THÀNH <
                        "AND (" +
                        "    k.SoLuongHeThong != k.SoLuongThucTe " + 
                        "    OR (" + // Lấy cả những phiếu có số lượng khớp nhưng dùng để giải trình
                        "         k.LyDo IS NOT NULL AND (" +
                        "         k.LyDo LIKE '%KK%' " +
                        "      OR k.LyDo LIKE N'%Đã kiểm tra%' " +
                        "      OR k.LyDo LIKE N'%kiểm kê lại%' " +
                        "      OR k.LyDo LIKE N'%Bù %' " +
                        "      OR k.LyDo LIKE N'%Nhận %'" +
                        "    ))" +
                        ")";
                    
                    try (java.sql.PreparedStatement ps = con.prepareStatement(sqlKiemKe)) {
                        // 🔥 BÍ QUYẾT XỬ LÝ LỖI NGÀY THÁNG: 
                        // Cộng thêm 1 ngày rồi mới quét '<'. (Ví dụ: EndDate = 03/06 -> Sẽ lấy mọi thứ < 04/06 00:00:00)
                        ps.setTimestamp(1, java.sql.Timestamp.valueOf(end.plusDays(1).atStartOfDay())); 
                        
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int sysQty = rs.getInt("SoLuongHeThong");
                                int realQty = rs.getInt("SoLuongThucTe");
                                String tenSP = rs.getString("TenSP");
                                boolean isResolved = rs.getInt("IsResolved") == 1;
                                
                                java.sql.Timestamp sqlNgay = rs.getTimestamp("NgayKiemKe");
                                LocalDate ngayKK = (sqlNgay != null) ? sqlNgay.toLocalDateTime().toLocalDate() : null;
                                
                                // Chặn hai đầu ngày để hiển thị chuẩn xác
                                boolean thuocGiaiDoanDangChon = (ngayKK != null && !ngayKK.isBefore(start) && !ngayKK.isAfter(end));

                                if (isResolved) {
                                    // 🟢 ĐÃ XỬ LÝ (Bao gồm cả Phiếu hụt cũ và Phiếu kiểm tra mới)
                                    if (thuocGiaiDoanDangChon) {
                                        daXuLy++;
                                        if (!daXuLyNames.contains(tenSP) && daXuLyNames.size() < 3) daXuLyNames.add(tenSP);
                                    }
                                } else {
                                    if (realQty < sysQty) {
                                        // 🚨 LUÔN BÁO ĐỘNG ĐỎ NẾU CHƯA AI XỬ LÝ
                                        lechKhoAm++;
                                        if (!amNames.contains(tenSP) && amNames.size() < 3) amNames.add(tenSP);
                                    }
                                    else if (realQty > sysQty) {
                                        // 🟡 DƯ THỪA
                                        if (thuocGiaiDoanDangChon) {
                                            lechKhoDuong++;
                                            if (!duongNames.contains(tenSP) && duongNames.size() < 3) duongNames.add(tenSP);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(); 
                } 
                
                if (lechKhoAm > 0) {
                    String names = String.join(", ", amNames);
                    if (lechKhoAm > 3) names += " và " + (lechKhoAm - 3) + " SP khác";
                    alertsArray.add("NGHIÊM TRỌNG: Phát hiện " + lechKhoAm + " trường hợp THẤT THOÁT HÀNG HÓA chưa rõ nguyên nhân (" + names + ").");
                }
                if (lechKhoDuong > 0) {
                    String names = String.join(", ", duongNames);
                    if (lechKhoDuong > 3) names += " và " + (lechKhoDuong - 3) + " SP khác";
                    alertsArray.add("CẢNH BÁO: Có " + lechKhoDuong + " trường hợp DƯ THỪA chưa xử lý (" + names + ").");
                }
                if (daXuLy > 0) {
                    String names = String.join(", ", daXuLyNames);
                    if (daXuLy > 3) names += " và " + (daXuLy - 3) + " SP khác";
                    alertsArray.add("ĐÃ XỬ LÝ: Có " + daXuLy + " ghi nhận lệch kho đã được xử lý(" + names + ").");
                }

                // --- 3.4 Báo cáo an toàn ---
                if (alertsArray.isEmpty()) alertsArray.add("TỐT: Kho vận hoạt động cực kỳ ổn định, không phát hiện rủi ro nào.");

                json.add("alertsFeed", alertsArray);
                
                return gson.toJson(json);
                
            } catch (Exception e) { 
                e.printStackTrace(); 
                return null; 
            }
        }).thenAccept(jsonResult -> {
            if (jsonResult != null) {
                Platform.runLater(() -> webEngine.executeScript("updateDashboard(" + jsonResult + ")"));
            }
        });
    }
    private void handleJavaAction(String action) {
        SwingUtilities.invokeLater(() -> {
            if (action.equals("IMPORT")) {
                if (callback != null) callback.moNhapHang();
            } else if (action.equals("AUDIT")) {
                if (callback != null) callback.moKiemKe();
            } else if (action.startsWith("VIEW_")) {
                String maSP = action.replace("VIEW_", "");
                JOptionPane.showMessageDialog(this, "Chi tiết mã hàng: " + maSP);
            }
        });
    }
    
    private void reloadHistoryList() {
        CompletableFuture.runAsync(() -> {
            try {
                // Truy vấn danh sách mốc lịch sử của phân hệ KHO từ Database
                String jsonArrayData = Dao.LichSuThongKeDAO.getInstance().layDanhSachLichSu("KHO");
                
                // Chống lỗi ký tự đặc biệt
                String safeJson = jsonArrayData.replace("'", "\\'"); 
                
                Platform.runLater(() -> {
                    if (webEngine != null) {
                        // SỬA THÀNH ĐÚNG TÊN HÀM CỦA KHO: populateHistoryDropdown
                        webEngine.executeScript("populateHistoryDropdown('" + safeJson + "')");
                    }
                });
            } catch (Exception e) { 
                e.printStackTrace(); 
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
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {}
        }

        SwingUtilities.invokeLater(() -> {
            JFrame khungChung = new JFrame("Warehouse Intelligence System - Standalone Runtime");
            khungChung.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            khungChung.setSize(1340, 780);
            khungChung.setMinimumSize(new Dimension(1000, 600));
            khungChung.setLocationRelativeTo(null);

            KhoPanel warehouseDashboard = new KhoPanel();
            khungChung.add(warehouseDashboard, BorderLayout.CENTER);
            khungChung.setVisible(true);
        });
    }
}
