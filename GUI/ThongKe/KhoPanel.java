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
                if (action.startsWith("ACTION:")) {
                    handleJavaAction(action.substring(7));
                }
            });

            // 2. KHI WEB TẢI XONG -> GỌI JAVA KÉO DỮ LIỆU ĐỔ VÀO MÀN HÌNH
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    refreshDashboardData(false); // Đã sửa lỗi thiếu tham số false ở đây!
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

    // 🔥 HÀM XỬ LÝ LOGIC DỮ LIỆU VÀ ĐẨY SANG WEB
    public void refreshDashboardData(boolean forceRefresh) {
        CompletableFuture.supplyAsync(() -> {
            try {
                if (forceRefresh || cachedData == null) {
                    cachedData = Dao.TruyVanSieuTocDAO.getInstance().loadDuLieuKiemKeSieuToc();
                }
                
                JsonObject json = new JsonObject();
                
                // 1. CHỈ SỐ TỔNG QUAN (KPIs)
                int totalStock = cachedData.mapTongTonKho.values().stream().mapToInt(Integer::intValue).sum();
                json.addProperty("totalStock", totalStock);
                json.addProperty("totalSkus", cachedData.dsSanPham.size());
                
                BigDecimal warehouseValue = BigDecimal.ZERO;
                for(var entry : cachedData.mapDanhSachLo.entrySet()) {
                    for(Data.ChiTietLoHang lo : entry.getValue()) {
                        if(lo.getGiaNhap() != null && lo.getSoLuongTon() > 0) {
                            warehouseValue = warehouseValue.add(lo.getGiaNhap().multiply(new BigDecimal(lo.getSoLuongTon())));
                        }
                    }
                }
                json.addProperty("warehouseValue", warehouseValue);
                
                // Lấy Thiệt hại từ ThongKeLogic
                Logic.ThongKeLogic tkLogic = new Logic.ThongKeLogic();
                BigDecimal[] metrics = tkLogic.thongKeLoiNhuanThang(LocalDate.now().getMonthValue(), LocalDate.now().getYear());
                json.addProperty("totalLoss", metrics[2] != null ? metrics[2] : BigDecimal.ZERO);

                // =========================================================================
                // 🔥 2. LIÊN KẾT BẢNG VÀ BIỂU ĐỒ (DÙNG RAM CACHE SIÊU TỐC)
                // KHÔNG CÒN BẤT KỲ CÂU LỆNH SQL NÀO Ở ĐÂY NỮA
                // =========================================================================
                JsonObject categoryData = new JsonObject();
                JsonArray table = new JsonArray();
                
                int lowStockCount = 0;
                for (Data.SanPham sp : cachedData.dsSanPham) {
                    int ton = cachedData.mapTongTonKho.getOrDefault(sp.getMaSP(), 0);
                    if (ton <= 10) lowStockCount++;
                    
                    // Lôi Tên Loại từ bộ nhớ đệm DTO ra dùng luôn
                    String tenLoai = sp.getMaLoai();
                    if (cachedData.mapTenLoai != null && cachedData.mapTenLoai.containsKey(sp.getMaLoai())) {
                        tenLoai = cachedData.mapTenLoai.get(sp.getMaLoai());
                    }
                    
                    categoryData.addProperty(tenLoai, categoryData.has(tenLoai) ? categoryData.get(tenLoai).getAsInt() + ton : ton);
                    
                    JsonObject row = new JsonObject();
                    row.addProperty("id", sp.getMaSP());
                    row.addProperty("name", sp.getTenSP());
                    row.addProperty("category", tenLoai);
                    row.addProperty("stock", ton);
                    row.addProperty("price", sp.getGiaBan());
                    table.add(row);
                }
                json.add("categoryDistribution", categoryData);
                json.add("inventoryTable", table);
                json.addProperty("lowStockCount", lowStockCount);

                // =========================================================================
                // 3. LIÊN KẾT LOGIC CẢNH BÁO THÔNG MINH TỪ KIEMKELOGIC
                // =========================================================================
                Logic.KiemKeLogic kiemKeLogic = Logic.KiemKeLogic.getInstance();
                java.util.List<String> canhBaoHeThong = kiemKeLogic.quetCanhBaoHeThong();
                java.util.List<String> batThuongKiemKe = kiemKeLogic.phatHienBatThuongKiemKe();
                
                JsonArray alertsArray = new JsonArray();
                for(String cb : canhBaoHeThong) alertsArray.add(cb);
                for(String bt : batThuongKiemKe) alertsArray.add(bt);
                json.add("alertsFeed", alertsArray);

                return gson.toJson(json);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAccept(jsonResult -> {
            if (jsonResult != null) {
                Platform.runLater(() -> {
                    try {
                        webEngine.executeScript("updateDashboard(" + jsonResult + ")");
                    } catch (Exception e) {
                        System.err.println("Lỗi gọi hàm JS updateDashboard: " + e.getMessage());
                    }
                });
            }
        });
    }
    // 🔥 HÀM BẮT SỰ KIỆN TỪ GIAO DIỆN WEB GỌI VỀ JAVA
    private void handleJavaAction(String action) {
        SwingUtilities.invokeLater(() -> {
            if (action.equals("IMPORT")) {
                // Gọi ngược lên TrangADMIN
                if (callback != null) callback.moNhapHang();
                
            } else if (action.equals("AUDIT")) {
                // Gọi ngược lên TrangADMIN
                if (callback != null) callback.moKiemKe();
                
            } else if (action.startsWith("VIEW_")) {
                String maSP = action.replace("VIEW_", "");
                JOptionPane.showMessageDialog(this, "Chi tiết mã hàng: " + maSP);
            }
        });
    }

    // =========================================================================
    // 🔥 HÀM MAIN ĐỘC LẬP - KHỞI CHẠY KIỂM THỬ GIAO DIỆN KHO (STANDALONE RUNTIME)
    // =========================================================================
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