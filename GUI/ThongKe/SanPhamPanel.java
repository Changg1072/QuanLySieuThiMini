package GUI.ThongKe;

import Logic.SanPhamLogic;
import Dao.TruyVanSieuTocDAO;
import Data.SanPham;
import Data.ChiTietLoHang;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
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
import netscape.javascript.JSObject;

public class SanPhamPanel extends JPanel {

    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    
    private String lastCachedDashboardJson = null;
    private String currentCategoryFilter = "ALL";
    private String currentSearchQuery = "";
    
    // Khai báo biến Lịch lọc ngày tháng
    private LocalDate filterStartDate = LocalDate.now().withDayOfMonth(1);
    private LocalDate filterEndDate = LocalDate.now();
    
    // Internal cache to prevent constant database thrashing
    private TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO cachedKiemKeDTO = null;
    private final JsBridge myJsBridge = new JsBridge();

    public SanPhamPanel() {
        setName("SanPhamPanel");
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
            
            // 🔥 THÊM LẮNG NGHE TÍN HIỆU ĐỔI NGÀY TỪ WEB
            webEngine.setOnAlert(event -> {
                String signal = event.getData();
                if (signal != null && signal.startsWith("ACTION:")) {
                    String actionData = signal.substring(7);
                    
                    if (actionData.startsWith("DATE_SYNC|")) {
                        try {
                            String[] parts = actionData.split("\\|");
                            filterStartDate = LocalDate.parse(parts[1]); 
                            filterEndDate = LocalDate.parse(parts[2]);
                            pushProductDashboardData(true); 
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if ("SYNC".equals(actionData)) {
                        pushProductDashboardData(true);
                    }
                }
            });

            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaConnector", myJsBridge);
                    executeJavaScript("setTimeout(function(){ if(typeof loadExportHistory === 'function') loadExportHistory(); }, 300);");
                    pushProductDashboardData(false);
                }
            });

            try {
                URL htmlUrl = getClass().getResource("sanpham_dashboard.html");
                if (htmlUrl != null) {
                    webEngine.load(htmlUrl.toExternalForm());
                } else {
                    webEngine.loadContent("<html><body><h3>🚨 Lỗi tải giao diện Html</h3></body></html>");
                }
            } catch (Exception e) {
                System.err.println("[SanPhamPanel] Lỗi: " + e.getMessage());
            }

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }

    public void pushProductDashboardData(boolean forceRefresh) {
        if (!forceRefresh && lastCachedDashboardJson != null) {
            executeJavaScript("updateProductDashboard(" + lastCachedDashboardJson + ")");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject mainContainer = new JsonObject();
                
                if (forceRefresh || cachedKiemKeDTO == null) {
                    cachedKiemKeDTO = TruyVanSieuTocDAO.getInstance().loadDuLieuKiemKeSieuToc();
                }
                TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO kiemKeData = cachedKiemKeDTO;
                
                Map<String, String> mapTenLoai = new HashMap<>();
                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                     java.sql.Statement st = con.createStatement();
                     java.sql.ResultSet rs = st.executeQuery("SELECT MaLoai, TenLoai FROM LoaiSP")) { 
                    while (rs.next()) {
                        mapTenLoai.put(rs.getString("MaLoai"), rs.getString("TenLoai"));
                    }
                } catch (Exception e) {
                    try (java.sql.Connection con2 = Dao.ConnectDB.getInstance().getConnection();
                         java.sql.Statement st2 = con2.createStatement();
                         java.sql.ResultSet rs2 = st2.executeQuery("SELECT MaLoai, TenLoai FROM LoaiSanPham")) {
                        while (rs2.next()) { mapTenLoai.put(rs2.getString("MaLoai"), rs2.getString("TenLoai")); }
                    } catch (Exception ex) {}
                }

                // =========================================================================
                // 🔥 LỌC DOANH SỐ THEO KHOẢNG NGÀY CHỌN TRÊN LỊCH
                // =========================================================================
                TruyVanSieuTocDAO.DuLieuDonHangDTO ordersDTO = TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                Map<String, Integer> topSellingMap = new HashMap<>(); 
                
                if (ordersDTO != null && ordersDTO.dsHoaDon != null) {
                    for (Data.HoaDon hd : ordersDTO.dsHoaDon) {
                        LocalDate ngayTao = hd.getNgayTao() != null ? hd.getNgayTao().toLocalDate() : null;
                        // Chặn lại nếu Hóa Đơn không nằm trong khoảng Ngày
                        if (ngayTao == null || ngayTao.isBefore(filterStartDate) || ngayTao.isAfter(filterEndDate)) {
                            continue; 
                        }
                        
                        List<Data.ChiTietHoaDon> listCT = ordersDTO.mapChiTietHD.get(hd.getMaHD());
                        if (listCT != null) {
                            for (Data.ChiTietHoaDon ct : listCT) {
                                topSellingMap.put(ct.getMaSp(), topSellingMap.getOrDefault(ct.getMaSp(), 0) + ct.getSoLuong());
                            }
                        }
                    }
                }

                // Sắp xếp lại để lấy Top 10 Bán Chạy trong khoảng thời gian này
                List<Map.Entry<String, Integer>> sortedSales = new ArrayList<>(topSellingMap.entrySet());
                sortedSales.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                int totalProductsCount = kiemKeData.dsSanPham.size();
                long uniqueCategoriesCount = kiemKeData.dsSanPham.stream().map(SanPham::getMaLoai).distinct().count();
                int lowStockThreshold = 10;
                long lowStockCount = kiemKeData.mapTongTonKho.values().stream().filter(stock -> stock <= lowStockThreshold).count();
                BigDecimal totalInventoryValuation = BigDecimal.ZERO;
                
                for (SanPham sp : kiemKeData.dsSanPham) {
                    List<ChiTietLoHang> associatedLots = kiemKeData.mapDanhSachLo.get(sp.getMaSP());
                    if (associatedLots != null) {
                        for (ChiTietLoHang lot : associatedLots) {
                            if (lot.getSoLuongTon() > 0 && lot.getGiaNhap() != null) {
                                BigDecimal lotValue = lot.getGiaNhap().multiply(new BigDecimal(lot.getSoLuongTon()));
                                totalInventoryValuation = totalInventoryValuation.add(lotValue);
                            }
                        }
                    }
                }

                mainContainer.addProperty("totalProducts", totalProductsCount);
                mainContainer.addProperty("totalCategories", uniqueCategoriesCount);
                mainContainer.addProperty("lowStockAlerts", lowStockCount);
                mainContainer.addProperty("inventoryValuation", totalInventoryValuation);

                // Dữ liệu cho Biểu Đồ Cột (Top 5 Sản phẩm bán chạy nhất)
                JsonArray topProductsArray = new JsonArray();
                for (int i = 0; i < Math.min(5, sortedSales.size()); i++) {
                    Map.Entry<String, Integer> entry = sortedSales.get(i);
                    String maSp = entry.getKey();
                    int qty = entry.getValue();
                    
                    String tenSp = kiemKeData.dsSanPham.stream()
                                    .filter(s -> s.getMaSP().equals(maSp))
                                    .map(Data.SanPham::getTenSP)
                                    .findFirst().orElse(maSp);
                                    
                    JsonObject item = new JsonObject();
                    item.addProperty("name", tenSp);
                    item.addProperty("quantity", qty);
                    topProductsArray.add(item);
                }
                mainContainer.add("topSellingProducts", topProductsArray);

                Map<String, Integer> categoryStockCount = new HashMap<>();
                for (SanPham sp : kiemKeData.dsSanPham) {
                    int stock = kiemKeData.mapTongTonKho.getOrDefault(sp.getMaSP(), 0);
                    String tenLoaiHienThi = mapTenLoai.getOrDefault(sp.getMaLoai(), sp.getMaLoai());
                    categoryStockCount.put(tenLoaiHienThi, categoryStockCount.getOrDefault(tenLoaiHienThi, 0) + stock);
                }
                
                JsonObject categoryChartNode = new JsonObject();
                categoryStockCount.forEach(categoryChartNode::addProperty);
                mainContainer.add("categoryDistribution", categoryChartNode);

                long optimalStock = kiemKeData.mapTongTonKho.values().stream().filter(v -> v > 10 && v <= 100).count();
                long overstocked = kiemKeData.mapTongTonKho.values().stream().filter(v -> v > 100).count();
                JsonObject healthNode = new JsonObject();
                healthNode.addProperty("low", lowStockCount);
                healthNode.addProperty("optimal", optimalStock);
                healthNode.addProperty("excess", overstocked);
                mainContainer.add("inventoryHealth", healthNode);

                JsonArray comprehensiveProductGrid = new JsonArray();
                for (SanPham sp : kiemKeData.dsSanPham) {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("id", sp.getMaSP());
                    entry.addProperty("name", sp.getTenSP());
                    
                    String tenLoaiHienThi = mapTenLoai.getOrDefault(sp.getMaLoai(), sp.getMaLoai());
                    entry.addProperty("category", tenLoaiHienThi); 
                    entry.addProperty("price", sp.getGiaBan());
                    
                    int currentStock = kiemKeData.mapTongTonKho.getOrDefault(sp.getMaSP(), 0);
                    entry.addProperty("stock", currentStock);
                    entry.addProperty("unit", sp.getDonViTinh());
                    
                    String imgFile = sp.getLinkHinhAnh();
                    if (imgFile != null && !imgFile.trim().isEmpty()) {
                        java.io.File file = new java.io.File(System.getProperty("user.dir") + java.io.File.separator + "images" + java.io.File.separator + imgFile);
                        if (file.exists()) { entry.addProperty("image", file.toURI().toString()); } 
                        else { entry.addProperty("image", ""); }
                    } else { entry.addProperty("image", ""); }
                    
                    // Lấy số lượng đã bán từ danh sách Lọc
                    int unitsSold = topSellingMap.getOrDefault(sp.getMaSP(), 0);
                    entry.addProperty("unitsSold", unitsSold);
                    
                    String structuralStatus = "OPTIMAL";
                    if (currentStock <= lowStockThreshold) structuralStatus = "LOW_STOCK";
                    else if (unitsSold > 50) structuralStatus = "BEST_SELLER";
                    else if (unitsSold == 0 && currentStock > 80) structuralStatus = "SLOW_MOVING";
                    entry.addProperty("intelligenceTag", structuralStatus);
                    
                    comprehensiveProductGrid.add(entry);
                }
                mainContainer.add("productGridMatrix", comprehensiveProductGrid);

                JsonArray smartInsights = new JsonArray();
                if (lowStockCount > 0) {
                    smartInsights.add("<span class='txt-badge-icon warn-badge'>!</span> Phát hiện " + lowStockCount + " mặt hàng chạm ngưỡng an toàn tối thiểu. Đề xuất chuẩn bị đơn nhập kho mới.");
                }
                if (!sortedSales.isEmpty()) {
                    String tenTop1 = kiemKeData.dsSanPham.stream().filter(s -> s.getMaSP().equals(sortedSales.get(0).getKey())).map(Data.SanPham::getTenSP).findFirst().orElse("");
                    smartInsights.add("<span class='txt-badge-icon good-badge'>+</span> Sản phẩm '" + tenTop1 + "' đạt hiệu suất kinh doanh vượt bậc trong khoảng thời gian này.");
                }
                smartInsights.add("<span class='txt-badge-icon info-badge'>i</span> Các nhóm hàng thuộc danh mục '" + (categoryStockCount.keySet().stream().findFirst().orElse("Chưa phân loại")) + "' chiếm tỷ lệ lưu kho cao nhất.");
                mainContainer.add("operationalInsights", smartInsights);

                lastCachedDashboardJson = gson.toJson(mainContainer);
                return lastCachedDashboardJson;

            } catch (Exception e) {
                System.err.println("[SanPhamPanel Calculation Engine] Lỗi: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).thenAccept(jsonResult -> {
            if (jsonResult != null) {
                executeJavaScript("updateProductDashboard(" + jsonResult + ")");
            }
        });
    }

    private void executeJavaScript(String executableScript) {
        Platform.runLater(() -> {
            try {
                if (webEngine != null) webEngine.executeScript(executableScript);
            } catch (Exception e) {}
        });
    }

    public void executeGlobalFilterUpdate(String category, String searchKeyword) {
        this.currentCategoryFilter = category;
        this.currentSearchQuery = searchKeyword;
        executeJavaScript(String.format("applyClientSideFilters('%s', '%s')", category, searchKeyword));
    }

    public class JsBridge {
        public void openProductDetail(String maSP) {
            SwingUtilities.invokeLater(() -> {
                if (cachedKiemKeDTO != null) {
                    SanPham targetSp = cachedKiemKeDTO.dsSanPham.stream()
                            .filter(sp -> sp.getMaSP().equals(maSP))
                            .findFirst().orElse(null);
                    
                    if (targetSp != null) {
                        int tonKho = cachedKiemKeDTO.mapTongTonKho.getOrDefault(maSP, 0);
                        GUI.HoTro.ChiTietSanPham.showModal(SanPhamPanel.this, targetSp, tonKho, () -> {
                            pushProductDashboardData(true); 
                        });
                    }
                }
            });
        }
        
        public void exportDashboardData(String customFileName) {
            CompletableFuture.runAsync(() -> {
                try {
                    String finalFileName = customFileName;
                    if (finalFileName == null || finalFileName.trim().isEmpty()) {
                        finalFileName = "Thong_Ke_San_Pham_" + System.currentTimeMillis() + ".json";
                    } else {
                        finalFileName = finalFileName.replace(".xlsx", ".json");
                    }

                    if (lastCachedDashboardJson == null || lastCachedDashboardJson.trim().isEmpty()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(SanPhamPanel.this,
                                "Dữ liệu chưa được tải! Hãy đợi dashboard load xong.",
                                "Chưa Có Dữ Liệu", JOptionPane.WARNING_MESSAGE));
                        return;
                    }

                    // Gọi DAO để lưu vào Database
                    Dao.LichSuThongKeDAO.getInstance().luuLichSu("SAN_PHAM", finalFileName, lastCachedDashboardJson);

                    String successMsg = finalFileName;
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(SanPhamPanel.this,
                            "Đã xuất dữ liệu thành công vào cơ sở dữ liệu!\n\n" +
                            "Ten ban ghi: " + successMsg + "\n\n" +
                            "Ban co the xem lai ngay qua Dropdown Lich Su.",
                            "Xuat Bao Cao Hoan Tat", JOptionPane.INFORMATION_MESSAGE));

                    reloadHistoryList();

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(SanPhamPanel.this,
                            "Loi ghi Database:\n" + ex.getMessage(), "Loi He Thong", JOptionPane.ERROR_MESSAGE));
                    ex.printStackTrace();
                }
            });
        }

        public String getExportHistoryList() {
            try {
                // Lấy danh sách lịch sử từ Database theo phân hệ SAN_PHAM
                String daoJson = Dao.LichSuThongKeDAO.getInstance().layDanhSachLichSu("SAN_PHAM");
                com.google.gson.JsonArray daoArr = gson.fromJson(daoJson, com.google.gson.JsonArray.class);

                // Trả về mảng tên file đơn giản giống NhanVienPanel
                com.google.gson.JsonArray simpleArr = new com.google.gson.JsonArray();
                if (daoArr != null) {
                    for (int i = 0; i < daoArr.size(); i++) {
                        com.google.gson.JsonObject obj = daoArr.get(i).getAsJsonObject();
                        if (obj.has("name")) {
                            simpleArr.add(obj.get("name").getAsString());
                        }
                    }
                }
                return simpleArr.toString();
            } catch (Exception e) {
                System.err.println("[JsBridge] getExportHistoryList loi: " + e.getMessage());
                return "[]";
            }
        }

        public void readAndLoadExportFile(String fileName) {
            CompletableFuture.runAsync(() -> {
                try {
                    String safeFileName = new java.io.File(fileName).getName();

                    // Lấy nội dung Base64 từ Database
                    String base64Data = Dao.LichSuThongKeDAO.getInstance().docNoiDungLichSuBase64(safeFileName);

                    if (base64Data == null || base64Data.isEmpty()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(SanPhamPanel.this,
                                "Ban ghi khong ton tai trong he thong Database: " + safeFileName,
                                "Loi", JOptionPane.ERROR_MESSAGE));
                        return;
                    }

                    Platform.runLater(() -> {
                        try {
                            webEngine.executeScript(
                                "applyHistoricalStateBase64('" + base64Data + "', '" + safeFileName.replace("'", "\\'") + "')"
                            );
                        } catch (Exception ex) {
                            System.err.println("[JsBridge] Loi render file lich su: " + ex.getMessage());
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(SanPhamPanel.this,
                            "Loi doc du lieu lich su tu Database:\n" + e.getMessage(),
                            "Loi", JOptionPane.ERROR_MESSAGE));
                    e.printStackTrace();
                }
            });
        }
    }
    private void reloadHistoryList() {
        Platform.runLater(() -> {
            try {
                if (webEngine != null) {
                    webEngine.executeScript("if(typeof loadExportHistory === 'function') loadExportHistory();");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame devFrame = new JFrame("Product Intelligence System - Standalone Runtime Verification");
            devFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            devFrame.setSize(1440, 850);
            devFrame.setLocationRelativeTo(null);
            SanPhamPanel viewPanel = new SanPhamPanel();
            devFrame.add(viewPanel, BorderLayout.CENTER);
            devFrame.setVisible(true);
        });
    }
}
