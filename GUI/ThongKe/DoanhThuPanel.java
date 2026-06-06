package GUI.ThongKe;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

/**
 * 🚀 HYBRID REVENUE ANALYTICS DASHBOARD PANEL (Bản nâng cấp Khoảng thời gian)
 */
public class DoanhThuPanel extends JPanel {

    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    
    private String lastCachedJson = null;
    private Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO cachedDonHangDTO = null;

    // 🔥 THAY THẾ Tháng/Năm bằng 2 mốc Ngày Bắt Đầu và Ngày Kết Thúc (Mặc định: Đầu tháng -> Hôm nay)
    private LocalDate filterStartDate = LocalDate.now().withDayOfMonth(1);
    private LocalDate filterEndDate = LocalDate.now();

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
            
            // 🔥 LẮNG NGHE TÍN HIỆU TỪ LỊCH TRÊN WEB GỬI XUỐNG
            webEngine.setOnAlert(event -> {
                String action = event.getData();
                if (action.startsWith("ACTION:DATE_SYNC|")) {
                    try {
                        String[] parts = action.split("\\|");
                        this.filterStartDate = LocalDate.parse(parts[1]);
                        this.filterEndDate = LocalDate.parse(parts[2]);
                        
                        System.out.println("✅ Java đã nhận khoảng thời gian mới: " + filterStartDate + " ĐẾN " + filterEndDate);
                        pushLiveAnalyticsData(true); // Load lại dữ liệu theo ngày mới
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (action.equals("ACTION:EXIT_HISTORY")) {
                    // 🔥 RESET BIẾN NGÀY TRÊN JAVA VỀ MẶC ĐỊNH: Đầu tháng -> Hôm nay
                    this.filterStartDate = LocalDate.now().withDayOfMonth(1);
                    this.filterEndDate = LocalDate.now();
                    System.out.println("🔄 Đã trả về khoảng thời gian mặc định: " + filterStartDate + " ĐẾN " + filterEndDate);
                    pushLiveAnalyticsData(true); 
                } else if (action.equals("ACTION:LOAD_HISTORY_LIST")) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            // Đường dẫn lưu trữ cho Doanh Thu
                            File dir = new File("D:\\Code\\QuanLySieuThiMini\\XuatThongKe\\DoanhThu");
                            if (!dir.exists()) dir.mkdirs();
                            
                            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
                            JsonArray arr = new JsonArray();
                            if (files != null) {
                                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                                for (File f : files) {
                                    JsonObject obj = new JsonObject();
                                    obj.addProperty("name", f.getName());
                                    obj.addProperty("path", f.getAbsolutePath().replace("\\", "/"));
                                    arr.add(obj);
                                }
                            }
                            String json = gson.toJson(arr);
                            Platform.runLater(() -> webEngine.executeScript("updateHistoryDropdown('" + json + "')"));
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                else if (action.startsWith("ACTION:LOAD_HISTORY_FILE|")) {
                    String tenFile = action.substring("ACTION:LOAD_HISTORY_FILE|".length());
                    CompletableFuture.runAsync(() -> {
                        try {
                            // GỌI DATABASE LẤY CHUỖI BASE64
                            String base64 = Dao.LichSuThongKeDAO.getInstance().docNoiDungLichSuBase64(tenFile);
                            if (base64 != null) {
                                String safeName = tenFile.replace("'", "\\'");
                                Platform.runLater(() -> webEngine.executeScript("applyHistoricalStateBase64('" + base64 + "', '" + safeName + "')"));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                else if (action.equals("ACTION:LOAD_HISTORY_LIST")) {
                    reloadHistoryList();
                }
                else if (action.startsWith("ACTION:VIEW_INVOICE|")) {
                    String maHD = action.substring("ACTION:VIEW_INVOICE|".length());
                    
                    SwingUtilities.invokeLater(() -> {
                        try {
                            // 1. Kéo dữ liệu từ DB + RAM cache
                            Data.HoaDon hd = Dao.HoaDonDAO.getInstance().layHoaDonTheoMa(maHD);
                            if (hd == null) {
                                JOptionPane.showMessageDialog(this, "Không tìm thấy hóa đơn: " + maHD, "Lỗi", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            java.util.List<Data.ChiTietHoaDon> dsChiTiet =
                                Dao.ChiTietHoaDonDAO.getInstance().layChiTietTheoMaHD(maHD);

                            // 2. Resolve tên NV và tên KH từ RAM cache
                            Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO dto = cachedDonHangDTO;

                            String tenNV = (dto != null && hd.getMaNV() != null && dto.mapNhanVien.containsKey(hd.getMaNV()))
                                ? dto.mapNhanVien.get(hd.getMaNV())
                                : (hd.getMaNV() != null ? hd.getMaNV() : "Unknown");

                            String tenKH = "Khách vãng lai";
                            String bacKH = "Không hạng";
                            if (hd.getMaKH() != null && dto != null && dto.mapKhachHang.containsKey(hd.getMaKH())) {
                                String[] khInfo = dto.mapKhachHang.get(hd.getMaKH());
                                tenKH = khInfo[0];
                                // mapKhachHang[1] là bacKH nếu bạn có lưu, nếu không thì query thêm
                                if (khInfo.length > 1 && khInfo[1] != null) bacKH = khInfo[1];
                            }

                            // 3. Build Object[][] items cho setDuLieuHoaDon
                            // Signature: Object[] = { tenSP, dvt, soLuong, donGia, thanhTienSP, ..., ..., giamGiaHienThi }
                            Object[][] items = new Object[dsChiTiet.size()][8];
                            for (int i = 0; i < dsChiTiet.size(); i++) {
                                Data.ChiTietHoaDon ct = dsChiTiet.get(i);

                                // Resolve tên sản phẩm từ cache
                                String tenSP = ct.getMaSp();
                                if (dto != null && dto.mapSanPham.containsKey(ct.getMaSp())) {
                                    tenSP = dto.mapSanPham.get(ct.getMaSp())[0];
                                }

                                java.math.BigDecimal donGia = ct.getDonGia() != null
                                    ? ct.getDonGia() : java.math.BigDecimal.ZERO;
                                java.math.BigDecimal thanhTienSP = ct.getThanhTienSanPham() != null
                                    ? ct.getThanhTienSanPham() : java.math.BigDecimal.ZERO;

                                // Tính chuỗi giảm giá hiển thị
                                java.math.BigDecimal tongGiaGoc = donGia.multiply(new java.math.BigDecimal(ct.getSoLuong()));
                                java.math.BigDecimal giamGiaSP = tongGiaGoc.subtract(thanhTienSP);
                                String giamGiaHienThi = "0 đ";
                                if (giamGiaSP.compareTo(java.math.BigDecimal.ZERO) > 0 && tongGiaGoc.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                    java.math.BigDecimal phanTram = giamGiaSP
                                        .multiply(new java.math.BigDecimal("100"))
                                        .divide(tongGiaGoc, 0, java.math.RoundingMode.HALF_UP);
                                    giamGiaHienThi = GUI.HoTro.DinhDangUtil.dinhDangSo(giamGiaSP) + " đ (" + phanTram + "%)";
                                }

                                items[i] = new Object[]{
                                    tenSP,                          // [0] Tên SP
                                    "",                             // [1] ĐVT (không dùng trong display)
                                    ct.getSoLuong(),                // [2] Số lượng
                                    donGia,                         // [3] Đơn giá
                                    thanhTienSP,                    // [4] Thành tiền SP
                                    null, null,                     // [5][6] placeholder
                                    giamGiaHienThi                  // [7] Chuỗi giảm giá đã format
                                };
                            }

                            // 4. Tính các khoản tổng để truyền vào
                            java.math.BigDecimal giamGiaHang = hd.getTongGiamGia() != null
                                ? hd.getTongGiamGia() : java.math.BigDecimal.ZERO;
                            java.math.BigDecimal truTichLuy = hd.getTruTichDiem() != null
                                ? hd.getTruTichDiem() : java.math.BigDecimal.ZERO;
                            java.math.BigDecimal khachDua = hd.getKhachDua() != null
                                ? hd.getKhachDua() : hd.getThanhTien();
                            boolean isTienMat = hd.getPhuongThucTT() != null
                                && hd.getPhuongThucTT().toLowerCase().contains("tiền mặt");

                            // 5. Tạo dialog và đổ dữ liệu
                            JDialog dialog = new JDialog(
                                (JFrame) SwingUtilities.getWindowAncestor(this),
                                "Chi Tiết Hóa Đơn: " + maHD, true
                            );
                            dialog.setSize(750, 950);
                            dialog.setLocationRelativeTo(this);
                            dialog.getContentPane().setBackground(new Color(243, 244, 246));
                            dialog.setLayout(new GridBagLayout());

                            GUI.ChiTietHoaDonUi ui = new GUI.ChiTietHoaDonUi();
                            ui.setPreferredSize(new Dimension(650, 840));

                            ui.setDuLieuHoaDon(
                                maHD, tenNV, tenKH, bacKH,
                                khachDua,
                                items,
                                giamGiaHang,
                                truTichLuy,
                                0,          // congTichLuy — không có sẵn trong HoaDon, để 0 hoặc tính từ diemTichLuy
                                isTienMat
                            );

                            dialog.add(ui);
                            dialog.setVisible(true);

                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(this,
                                "Không thể mở chi tiết hóa đơn: " + e.getMessage(),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        }
                    });
                }
                else if (action.startsWith("ACTION:EXPORT|")) {
                    String fileName = action.split("\\|")[1];
                    CompletableFuture.runAsync(() -> {
                        try {
                            String jsonFileName = fileName.replace(".xlsx", ".json");
                            
                            // GỌI DATABASE ĐỂ LƯU CHUỖI JSON
                            if (lastCachedJson != null) {
                                Dao.LichSuThongKeDAO.getInstance().luuLichSu("DOANH_THU", jsonFileName, lastCachedJson);
                            }
                            
                            // Hiển thị thông báo và gọi hàm load lại danh sách
                            Platform.runLater(() -> webEngine.executeScript("alert('Đã lưu lịch sử: " + jsonFileName + "')"));
                            
                            reloadHistoryList(); // Cập nhật lại dropdown lịch sử
                            
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
            });

            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    pushLiveAnalyticsData(false);
                }
            });

            try {
                URL htmlUrl = getClass().getResource("doanhthu_dashboard.html");
                if (htmlUrl != null) {
                    webEngine.load(htmlUrl.toExternalForm());
                } else {
                    webEngine.loadContent("<html><body style='font-family:sans-serif;padding:30px;color:#ef4444;'>"
                        + "<h2>🚨 Lỗi tải Template</h2></body></html>");
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

                // 1. KÉO DỮ LIỆU TỪ RAM CACHE
                if (forceRefresh || cachedDonHangDTO == null) {
                    cachedDonHangDTO = Dao.TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                }
                Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO donHangDTO = cachedDonHangDTO;

                BigDecimal totalRevenue = BigDecimal.ZERO;
                int totalInvoices = 0;
                java.util.Map<Integer, Integer> hourCount = new java.util.HashMap<>();
                
                // =========================================================
                // 🔥 CHUẨN BỊ TRỤC X CHO BIỂU ĐỒ (DOANH THU & LỢI NHUẬN)
                // =========================================================
                java.util.Map<LocalDate, BigDecimal> dailyRevenueMap = new java.util.TreeMap<>();
                java.util.Map<LocalDate, BigDecimal> dailyProfitMap = new java.util.TreeMap<>(); 
                
                LocalDate current = filterStartDate;
                while (!current.isAfter(filterEndDate)) {
                    dailyRevenueMap.put(current, BigDecimal.ZERO);
                    dailyProfitMap.put(current, BigDecimal.ZERO);
                    current = current.plusDays(1);
                }

                // Các biến chứa dữ liệu phụ
                List<Data.HoaDon> dsHoaDonTrongKhoang = new java.util.ArrayList<>();
                java.util.Map<String, Integer> productSales = new java.util.HashMap<>();
                java.util.Map<String, BigDecimal> customerSpent = new java.util.HashMap<>();
                int cashCount = 0, transferCount = 0, eWalletCount = 0;

                // 2. QUÉT HÓA ĐƠN TRÊN RAM 
                for (Data.HoaDon hd : donHangDTO.dsHoaDon) {
                    if (hd.getNgayTao() != null) {
                        LocalDate hdDate = hd.getNgayTao().toLocalDate();

                        if (!hdDate.isBefore(filterStartDate) && !hdDate.isAfter(filterEndDate)) {
                            if (hd.getTraHang() != null && hd.getTraHang()) continue; 

                            dsHoaDonTrongKhoang.add(hd);
                            totalInvoices++; 

                            if (hd.getThanhTien() != null) {
                                totalRevenue = totalRevenue.add(hd.getThanhTien()); 
                                // Cộng tiền vào cả Doanh Thu và Lợi Nhuận của ngày đó
                                dailyRevenueMap.put(hdDate, dailyRevenueMap.get(hdDate).add(hd.getThanhTien()));
                                dailyProfitMap.put(hdDate, dailyProfitMap.get(hdDate).add(hd.getThanhTien()));
                            }

                            int hour = hd.getNgayTao().getHour();
                            hourCount.put(hour, hourCount.getOrDefault(hour, 0) + 1);

                            String pt = hd.getPhuongThucTT();
                            if (pt != null) {
                                if (pt.toLowerCase().contains("tiền mặt")) cashCount++;
                                else if (pt.toLowerCase().contains("chuyển khoản")) transferCount++;
                                else eWalletCount++;
                            }

                            if (hd.getMaKH() != null && hd.getThanhTien() != null) {
                                customerSpent.put(hd.getMaKH(), customerSpent.getOrDefault(hd.getMaKH(), BigDecimal.ZERO).add(hd.getThanhTien()));
                            }

                            List<Data.ChiTietHoaDon> details = donHangDTO.mapChiTietHD.get(hd.getMaHD());
                            if (details != null) {
                                for (Data.ChiTietHoaDon ct : details) {
                                    productSales.put(ct.getMaSp(), productSales.getOrDefault(ct.getMaSp(), 0) + ct.getSoLuong());
                                }
                            }
                        }
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

                // 3. TÍNH TOÁN CÁC KHOẢN TRỪ (CHI PHÍ)
                BigDecimal totalLoss = BigDecimal.ZERO;
                
                // 3.1. Trừ tiền Tiêu Hủy (Từ RAM) theo TỪNG NGÀY
                for (Data.PhieuTieuHuy ph : donHangDTO.dsTieuHuy) {
                    if (ph.getNgayTao() != null) {
                        LocalDate phDate = ph.getNgayTao().toLocalDate();
                        if (!phDate.isBefore(filterStartDate) && !phDate.isAfter(filterEndDate)) {
                            if (ph.getTongGiaTriHuy() != null) {
                                totalLoss = totalLoss.add(ph.getTongGiaTriHuy());
                                // Trừ thẳng vào biểu đồ lợi nhuận của ngày đó
                                dailyProfitMap.put(phDate, dailyProfitMap.get(phDate).subtract(ph.getTongGiaTriHuy()));
                            }
                        }
                    }
                }

                BigDecimal totalCOGS = BigDecimal.ZERO;
                BigDecimal totalSalary = BigDecimal.ZERO;
                BigDecimal totalImportCost = BigDecimal.ZERO;
                
                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection()) {
                    java.sql.Timestamp tsStart = java.sql.Timestamp.valueOf(filterStartDate.atStartOfDay());
                    java.sql.Timestamp tsEnd = java.sql.Timestamp.valueOf(filterEndDate.plusDays(1).atStartOfDay());
                    java.sql.Date dStart = java.sql.Date.valueOf(filterStartDate);
                    java.sql.Date dEnd = java.sql.Date.valueOf(filterEndDate.plusDays(1));

                    // 3.2. Trừ tiền Lệch Kho theo TỪNG NGÀY
                    String sqlLechKho = "SELECT CAST(k.NgayKiemKe AS DATE) AS Ngay, SUM((k.SoLuongHeThong - k.SoLuongThucTe) * lh.GiaNhap) AS Tien " +
                                        "FROM KiemKeKho k JOIN ChiTietLoHang lh ON k.MaLoHang = lh.MaLoHang AND k.MaSP = lh.MaSP " +
                                        "WHERE k.NgayKiemKe >= ? AND k.NgayKiemKe < ? AND k.SoLuongThucTe < k.SoLuongHeThong AND k.TrangThai = N'Đã Cân Kho' " +
                                        "GROUP BY CAST(k.NgayKiemKe AS DATE)";
                    try (java.sql.PreparedStatement ps = con.prepareStatement(sqlLechKho)) {
                        ps.setTimestamp(1, tsStart); ps.setTimestamp(2, tsEnd);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                BigDecimal tien = rs.getBigDecimal("Tien");
                                if (tien != null) {
                                    totalLoss = totalLoss.add(tien);
                                    LocalDate d = rs.getDate("Ngay").toLocalDate();
                                    if (dailyProfitMap.containsKey(d)) dailyProfitMap.put(d, dailyProfitMap.get(d).subtract(tien));
                                }
                            }
                        }
                    }

                    // 4.1. Trừ tiền Giá vốn (COGS) theo TỪNG NGÀY
                    String sqlVon = "SELECT CAST(hd.NgayTao AS DATE) AS Ngay, SUM(ct.SoLuong * lh.GiaNhap) AS Tien " +
                                    "FROM ChiTietHoaDon ct JOIN HoaDon hd ON ct.MaHD = hd.MaHD " +
                                    "JOIN ChiTietLoHang lh ON ct.MaLoHang = lh.MaLoHang AND ct.MaSP = lh.MaSP " +
                                    "WHERE hd.NgayTao >= ? AND hd.NgayTao < ? AND (hd.TraHang = 0 OR hd.TraHang IS NULL) " +
                                    "GROUP BY CAST(hd.NgayTao AS DATE)";
                    try (java.sql.PreparedStatement ps = con.prepareStatement(sqlVon)) {
                        ps.setTimestamp(1, tsStart); ps.setTimestamp(2, tsEnd);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                BigDecimal tien = rs.getBigDecimal("Tien");
                                if (tien != null) {
                                    totalCOGS = totalCOGS.add(tien);
                                    LocalDate d = rs.getDate("Ngay").toLocalDate();
                                    if (dailyProfitMap.containsKey(d)) dailyProfitMap.put(d, dailyProfitMap.get(d).subtract(tien));
                                }
                            }
                        }
                    }
                    
                    // 4.2. Trừ tiền Lương nhân viên theo TỪNG NGÀY
                    String sqlSalary = 
                        "SELECT cc.NgayLam AS Ngay, SUM((DATEDIFF(MINUTE, cc.ThoiGianCheckIn, cc.ThoiGianCheckOut) / 60.0) * cl.LuongTheoGio) - SUM(CASE WHEN cc.TienChenhLech < 0 THEN ABS(cc.TienChenhLech) ELSE 0 END) AS Tien " +
                        "FROM ChiaCa cc JOIN CauHinhLuong cl ON cc.MaNV = cl.MaNV " +
                        "WHERE cc.TinhTrang = N'Đã hoàn thành' AND cl.TrangThai = N'Đang áp dụng' " +
                        "AND cc.ThoiGianCheckIn IS NOT NULL AND cc.ThoiGianCheckOut IS NOT NULL " +
                        "AND cc.NgayLam >= ? AND cc.NgayLam < ? " +
                        "GROUP BY cc.NgayLam";
                    try (java.sql.PreparedStatement ps = con.prepareStatement(sqlSalary)) {
                        ps.setDate(1, dStart); ps.setDate(2, dEnd);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                BigDecimal tien = rs.getBigDecimal("Tien");
                                if (tien != null) {
                                    totalSalary = totalSalary.add(tien);
                                    LocalDate d = rs.getDate("Ngay").toLocalDate();
                                    if (dailyProfitMap.containsKey(d)) dailyProfitMap.put(d, dailyProfitMap.get(d).subtract(tien));
                                }
                            }
                        }
                    }

                    // 4.3 Tiền nhập kho (Không trừ vào biểu đồ lợi nhuận vì là tài sản)
                    String sqlNhapKho = "SELECT SUM(c.SoLuongNhap * c.GiaNhap) FROM ChiTietLoHang c " +
                                        "JOIN LoHang h ON c.MaLoHang = h.MaLoHang " +
                                        "WHERE h.NgayNhapKho >= ? AND h.NgayNhapKho < ?";
                    try (java.sql.PreparedStatement ps = con.prepareStatement(sqlNhapKho)) {
                        ps.setDate(1, dStart); ps.setDate(2, dEnd);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getBigDecimal(1) != null) totalImportCost = rs.getBigDecimal(1);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Lỗi truy vấn chi phí hàng ngày: " + e.getMessage());
                }

                BigDecimal actualProfit = totalRevenue.subtract(totalCOGS).subtract(totalLoss).subtract(totalSalary);

                dashboardData.addProperty("totalRevenue", totalRevenue);
                dashboardData.addProperty("totalInvoices", totalInvoices);
                dashboardData.addProperty("peakHour", peakHourStr);
                dashboardData.addProperty("peakOrderCount", maxInvoices);
                
                dashboardData.addProperty("totalCOGS", totalCOGS);
                dashboardData.addProperty("totalLoss", totalLoss);
                dashboardData.addProperty("totalSalary", totalSalary);
                dashboardData.addProperty("actualProfit", actualProfit);
                dashboardData.addProperty("totalImportCost", totalImportCost);
                
                // =========================================================
                // 🔥 ĐÓNG GÓI 2 ĐƯỜNG DỮ LIỆU ĐỂ VẼ BIỂU ĐỒ (DOANH THU & LỢI NHUẬN)
                // =========================================================
                JsonArray lblArray = new JsonArray();
                JsonArray dataArray = new JsonArray();
                JsonArray profitArray = new JsonArray(); // Dữ liệu Lợi Nhuận
                
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
                for (LocalDate d = filterStartDate; !d.isAfter(filterEndDate); d = d.plusDays(1)) {
                    lblArray.add(d.format(fmt)); 
                    dataArray.add(dailyRevenueMap.get(d));          
                    profitArray.add(dailyProfitMap.get(d)); 
                }
                
                dashboardData.add("chronologicalLabels", lblArray);
                dashboardData.add("chronologicalSales", dataArray);
                dashboardData.add("chronologicalProfit", profitArray); // Bắn lên JS

                // CÁC THỐNG KÊ PHỤ BÊN DƯỚI (Giữ nguyên)
                JsonObject paymentMethods = new JsonObject();
                paymentMethods.addProperty("cash", cashCount);
                paymentMethods.addProperty("bankTransfer", transferCount);
                paymentMethods.addProperty("eWallet", eWalletCount);
                dashboardData.add("paymentMethods", paymentMethods);

                JsonArray topProductsArr = new JsonArray();
                productSales.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).limit(5).forEach(entry -> {
                        JsonObject p = new JsonObject();
                        String tenSP = donHangDTO.mapSanPham.containsKey(entry.getKey()) ? donHangDTO.mapSanPham.get(entry.getKey())[0] : entry.getKey();
                        p.addProperty("name", tenSP);
                        p.addProperty("unitsSold", entry.getValue());
                        topProductsArr.add(p);
                    });
                dashboardData.add("topProducts", topProductsArr);

                JsonArray topCustomersArr = new JsonArray();
                customerSpent.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).limit(5).forEach(entry -> {
                        JsonObject c = new JsonObject();
                        String tenKH = donHangDTO.mapKhachHang.containsKey(entry.getKey()) ? donHangDTO.mapKhachHang.get(entry.getKey())[0] : "Khách hàng " + entry.getKey();
                        c.addProperty("name", tenKH);
                        c.addProperty("totalSpent", entry.getValue());
                        topCustomersArr.add(c);
                    });
                dashboardData.add("topCustomers", topCustomersArr);

                dsHoaDonTrongKhoang.sort((h1, h2) -> {
                    if (h1.getNgayTao() == null || h2.getNgayTao() == null) return 0;
                    return h2.getNgayTao().compareTo(h1.getNgayTao()); 
                });
                JsonArray recentInvoices = new JsonArray();
                java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for(int i = 0; i < Math.min(10, dsHoaDonTrongKhoang.size()); i++) {
                    Data.HoaDon hd = dsHoaDonTrongKhoang.get(i);
                    JsonObject inv = new JsonObject();
                    inv.addProperty("id", hd.getMaHD());
                    String tenKH = hd.getMaKH() != null && donHangDTO.mapKhachHang.containsKey(hd.getMaKH()) ? donHangDTO.mapKhachHang.get(hd.getMaKH())[0] : "Khách vãng lai";
                    inv.addProperty("customer", tenKH);
                    String tenNV = hd.getMaNV() != null && donHangDTO.mapNhanVien.containsKey(hd.getMaNV()) ? donHangDTO.mapNhanVien.get(hd.getMaNV()) : "Unknown";
                    inv.addProperty("cashier", tenNV);
                    inv.addProperty("timestamp", hd.getNgayTao() != null ? hd.getNgayTao().format(timeFmt) : "");
                    inv.addProperty("method", hd.getPhuongThucTT() != null ? hd.getPhuongThucTT() : "Khác");
                    inv.addProperty("amount", hd.getThanhTien());
                    recentInvoices.add(inv);
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
    private void reloadHistoryList() {
        CompletableFuture.runAsync(() -> {
            try {
                File dir = new File("D:\\Code\\QuanLySieuThiMini\\XuatThongKe\\DoanhThu");
                if (!dir.exists()) dir.mkdirs();
                
                File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
                JsonArray arr = new JsonArray();
                if (files != null) {
                    Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    for (File f : files) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("name", f.getName());
                        obj.addProperty("path", f.getAbsolutePath().replace("\\", "/"));
                        arr.add(obj);
                    }
                }
                String json = gson.toJson(arr);
                Platform.runLater(() -> {
                    if (webEngine != null) {
                        webEngine.executeScript("updateHistoryDropdown('" + json + "')");
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
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
            JFrame khungChung = new JFrame("Hệ Thống Kiểm Thử Hybrid UI - Revenue Dashboard");
            khungChung.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            khungChung.setSize(1340, 780);
            khungChung.setLocationRelativeTo(null);
            khungChung.add(new DoanhThuPanel(), BorderLayout.CENTER);
            khungChung.setVisible(true);
        });
    }
}