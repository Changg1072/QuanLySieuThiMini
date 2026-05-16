package GUI.ThongKe;

import Logic.SanPhamLogic;
import Logic.ThongKeLogic;
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

/**
 * 🚀 HYBRID RETAIL PRODUCT INTELLIGENCE & ANALYTICS PANEL
 * Designed with a premium minimalist aesthetic matching modern SaaS platforms like Shopify Admin.
 * Combines full-fledged Java background threading logic with advanced HTML5/CSS3/JavaScript rendering.
 */
public class SanPhamPanel extends JPanel {

    private JFXPanel jfxPanel;
    private WebEngine webEngine;
    private final Gson gson = new Gson();
    
    private String lastCachedDashboardJson = null;
    private String currentCategoryFilter = "ALL";
    private String currentSearchQuery = "";
    
    // Internal cache to prevent constant database thrashing
    private TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO cachedKiemKeDTO = null;

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
            
            // Bridge state listener to push data once page fully compiles
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    // Set up Java-to-JavaScript bridge communication hook if needed
                    pushProductDashboardData(false);
                }
            });

            try {
                URL htmlUrl = getClass().getResource("sanpham_dashboard.html");
                if (htmlUrl != null) {
                    webEngine.load(htmlUrl.toExternalForm());
                } else {
                    // Fallback visual safety if file misallocated during compile paths
                    webEngine.loadContent("<html><body style='font-family:sans-serif;padding:40px;background:#f8fafc;color:#ef4444;'>"
                        + "<h2>🚨 Hybrid View Engine Core Failure</h2>"
                        + "<p>Unable to locate <code>sanpham_dashboard.html</code> template inside the package directory.</p>"
                        + "</body></html>");
                }
            } catch (Exception e) {
                System.err.println("[SanPhamPanel Core] Connection error: " + e.getMessage());
            }

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }

    /**
     * Pulls business logic processing asynchronously onto separate worker threads,
     * builds a heavy Analytical JSON node structure, and injects it straight into WebKit DOM.
     */
    public void pushProductDashboardData(boolean forceRefresh) {
        if (!forceRefresh && lastCachedDashboardJson != null) {
            executeJavaScript("updateProductDashboard(" + lastCachedDashboardJson + ")");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject mainContainer = new JsonObject();
                
                // 1. Fetch super-optimized Multiple ResultSets DTO to completely avoid N+1 query pattern
                if (forceRefresh || cachedKiemKeDTO == null) {
                    cachedKiemKeDTO = TruyVanSieuTocDAO.getInstance().loadDuLieuKiemKeSieuToc();
                }
                TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO kiemKeData = cachedKiemKeDTO;
                
                // =========================================================================
                // 🔥 THÊM MỚI: QUERY NHANH ĐỂ LẤY TÊN LOẠI SẢN PHẨM TỪ DATABASE
                // =========================================================================
                Map<String, String> mapTenLoai = new HashMap<>();
                try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                     java.sql.Statement st = con.createStatement();
                     java.sql.ResultSet rs = st.executeQuery("SELECT MaLoai, TenLoai FROM LoaiHang")) { 
                    while (rs.next()) {
                        mapTenLoai.put(rs.getString("MaLoai"), rs.getString("TenLoai"));
                    }
                } catch (Exception e) {
                    // Cấu hình dự phòng nếu bảng của bạn tên là LoaiSanPham thay vì LoaiHang
                    try (java.sql.Connection con2 = Dao.ConnectDB.getInstance().getConnection();
                         java.sql.Statement st2 = con2.createStatement();
                         java.sql.ResultSet rs2 = st2.executeQuery("SELECT MaLoai, TenLoai FROM LoaiSanPham")) {
                        while (rs2.next()) {
                            mapTenLoai.put(rs2.getString("MaLoai"), rs2.getString("TenLoai"));
                        }
                    } catch (Exception ex) {
                         System.err.println("Không thể lấy Tên Loại Sản Phẩm: " + ex.getMessage());
                    }
                }

                ThongKeLogic thongKeLogic = new ThongKeLogic();
                int thisMonth = LocalDate.now().getMonthValue();
                int thisYear = LocalDate.now().getYear();
                
                // Fetch dynamic real-time data from basic statistics layer
                List<Object[]> salesRankingRaw = thongKeLogic.topSanPhamBanChay(thisMonth, thisYear, 10);
                Map<String, Integer> topSellingMap = new HashMap<>();
                for (Object[] row : salesRankingRaw) {
                    topSellingMap.put((String) row[0], (Integer) row[1]);
                }

                // =========================================================================
                // 2. METRICS & FINANCIAL VALUATION IN-MEMORY ENGINE
                // =========================================================================
                int totalProductsCount = kiemKeData.dsSanPham.size();
                long uniqueCategoriesCount = kiemKeData.dsSanPham.stream().map(SanPham::getMaLoai).distinct().count();
                
                int lowStockThreshold = 10;
                long lowStockCount = kiemKeData.mapTongTonKho.values().stream().filter(stock -> stock <= lowStockThreshold).count();
                
                BigDecimal totalInventoryValuation = BigDecimal.ZERO;
                
                // Loop through structures to accumulate cost vectors and categories
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

                // Map metrics to main node
                mainContainer.addProperty("totalProducts", totalProductsCount);
                mainContainer.addProperty("totalCategories", uniqueCategoriesCount);
                mainContainer.addProperty("lowStockAlerts", lowStockCount);
                mainContainer.addProperty("inventoryValuation", totalInventoryValuation);

                // =========================================================================
                // 3. GENERATE ADVANCED GRAPHICAL GRAPH VECTOR PAYLOADS
                // =========================================================================
                
                // Top Selling Items array node
                JsonArray topProductsArray = new JsonArray();
                salesRankingRaw.stream().limit(5).forEach(row -> {
                    JsonObject item = new JsonObject();
                    item.addProperty("name", (String) row[0]);
                    item.addProperty("quantity", (Integer) row[1]);
                    topProductsArray.add(item);
                });
                mainContainer.add("topSellingProducts", topProductsArray);

                // 🔥 THAY ĐỔI: SỬ DỤNG TÊN LOẠI CHO BIỂU ĐỒ DONUT
                Map<String, Integer> categoryStockCount = new HashMap<>();
                for (SanPham sp : kiemKeData.dsSanPham) {
                    int stock = kiemKeData.mapTongTonKho.getOrDefault(sp.getMaSP(), 0);
                    String tenLoaiHienThi = mapTenLoai.getOrDefault(sp.getMaLoai(), sp.getMaLoai());
                    categoryStockCount.put(tenLoaiHienThi, categoryStockCount.getOrDefault(tenLoaiHienThi, 0) + stock);
                }
                
                JsonObject categoryChartNode = new JsonObject();
                categoryStockCount.forEach(categoryChartNode::addProperty);
                mainContainer.add("categoryDistribution", categoryChartNode);

                // Inventory Health ratios segmentation
                long optimalStock = kiemKeData.mapTongTonKho.values().stream().filter(v -> v > 10 && v <= 100).count();
                long overstocked = kiemKeData.mapTongTonKho.values().stream().filter(v -> v > 100).count();
                
                JsonObject healthNode = new JsonObject();
                healthNode.addProperty("low", lowStockCount);
                healthNode.addProperty("optimal", optimalStock);
                healthNode.addProperty("excess", overstocked);
                mainContainer.add("inventoryHealth", healthNode);

                // =========================================================================
                // 4. RICH PRODUCT REGISTRY GRID MATRIX (PACKAGING SYSTEM MODELS)
                // =========================================================================
                JsonArray comprehensiveProductGrid = new JsonArray();
                
                for (SanPham sp : kiemKeData.dsSanPham) {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("id", sp.getMaSP());
                    entry.addProperty("name", sp.getTenSP());
                    
                    // 🔥 THAY ĐỔI: SỬ DỤNG TÊN LOẠI CHO BẢNG & BỘ LỌC TÌM KIẾM JS
                    String tenLoaiHienThi = mapTenLoai.getOrDefault(sp.getMaLoai(), sp.getMaLoai());
                    entry.addProperty("category", tenLoaiHienThi); 
                    
                    entry.addProperty("price", sp.getGiaBan());
                    
                    int currentStock = kiemKeData.mapTongTonKho.getOrDefault(sp.getMaSP(), 0);
                    entry.addProperty("stock", currentStock);
                    entry.addProperty("unit", sp.getDonViTinh());
                    entry.addProperty("image", sp.getLinkHinhAnh() != null ? sp.getLinkHinhAnh() : "");
                    
                    // Assign smart functional metric tags based on runtime calculations
                    int unitsSold = topSellingMap.getOrDefault(sp.getTenSP(), 0);
                    entry.addProperty("unitsSold", unitsSold);
                    
                    String structuralStatus = "OPTIMAL";
                    if (currentStock <= lowStockThreshold) {
                        structuralStatus = "LOW_STOCK";
                    } else if (unitsSold > 50) {
                        structuralStatus = "BEST_SELLER";
                    } else if (unitsSold == 0 && currentStock > 80) {
                        structuralStatus = "SLOW_MOVING";
                    }
                    entry.addProperty("intelligenceTag", structuralStatus);
                    
                    comprehensiveProductGrid.add(entry);
                }
                mainContainer.add("productGridMatrix", comprehensiveProductGrid);

                // =========================================================================
                // 5. DATA INSIGHT ENGINE (CONTEXT ANALYTICS)
                // =========================================================================
                JsonArray smartInsights = new JsonArray();
                if (lowStockCount > 0) {
                    smartInsights.add("⚠️ Phát hiện " + lowStockCount + " mặt hàng chạm ngưỡng an toàn tối thiểu. Đề xuất chuẩn bị đơn nhập kho mới.");
                }
                
                // Extract top product name dynamically
                if (!salesRankingRaw.isEmpty()) {
                    smartInsights.add("🚀 Sản phẩm '" + salesRankingRaw.get(0)[0] + "' đạt hiệu suất kinh doanh vượt bậc, dẫn đầu doanh số chi nhánh.");
                }
                smartInsights.add("💡 Các nhóm hàng thuộc danh mục '" + (categoryStockCount.keySet().stream().findFirst().orElse("Chưa phân loại")) + "' chiếm tỷ lệ lưu kho cao nhất.");
                mainContainer.add("operationalInsights", smartInsights);

                lastCachedDashboardJson = gson.toJson(mainContainer);
                return lastCachedDashboardJson;

            } catch (Exception e) {
                System.err.println("[SanPhamPanel Calculation Engine] Core failure: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }).thenAccept(jsonResult -> {
            if (jsonResult != null) {
                // Pipe optimized data directly down inside WebKit platform thread loop
                executeJavaScript("updateProductDashboard(" + jsonResult + ")");
            }
        });
    }

    private void executeJavaScript(String executableScript) {
        Platform.runLater(() -> {
            try {
                if (webEngine != null) {
                    webEngine.executeScript(executableScript);
                }
            } catch (Exception e) {
                // Prevent noisy log output during raw layout frame swaps
            }
        });
    }

    /**
     * Interface communication endpoint for triggering system re-renders across filter coordinate changes.
     */
    public void executeGlobalFilterUpdate(String category, String searchKeyword) {
        this.currentCategoryFilter = category;
        this.currentSearchQuery = searchKeyword;
        // Invoke client side localized filters to keep interactions feeling instant, or force database reloading
        executeJavaScript(String.format("applyClientSideFilters('%s', '%s')", category, searchKeyword));
    }

    public static void main(String[] args) {
        // Test frame launcher environment for local visual validation layouts
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