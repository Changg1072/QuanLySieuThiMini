package Dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

public class LichSuThongKeDAO {
    
    private static LichSuThongKeDAO instance;
    public static LichSuThongKeDAO getInstance() {
        if (instance == null) instance = new LichSuThongKeDAO();
        return instance;
    }

    // 1. Lưu file mới vào DB (Thay thế cho Files.write)
    public boolean luuLichSu(String phanHe, String tenLichSu, String noiDungJson) {
        String sql = "INSERT INTO LichSuThongKe (PhanHe, TenLichSu, NoiDungJson, NgayTao) VALUES (?, ?, ?, GETDATE())";
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phanHe);
            ps.setString(2, tenLichSu);
            ps.setString(3, noiDungJson);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Lấy danh sách đưa lên Dropdown (Thay thế cho dir.listFiles)
    // Trả về chuỗi JSON mảng [{"name": "...", "path": "..."}] để Web tự render
    public String layDanhSachLichSu(String phanHe) {
        String sql = "SELECT TenLichSu FROM LichSuThongKe WHERE PhanHe = ? ORDER BY NgayTao DESC";
        JsonArray arr = new JsonArray();
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phanHe);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    String ten = rs.getString("TenLichSu");
                    obj.addProperty("name", ten);
                    obj.addProperty("path", ten); // Dùng chính TenLichSu làm key (path) gửi về Java
                    arr.add(obj);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return arr.toString();
    }

    // 3. Đọc nội dung file khi User bấm chọn (Thay thế cho Files.readAllBytes)
    // Trả về chuỗi Base64 y hệt như logic đọc file vật lý cũ để JS không bị lỗi
    public String docNoiDungLichSuBase64(String tenLichSu) {
        String sql = "SELECT NoiDungJson FROM LichSuThongKe WHERE TenLichSu = ?";
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tenLichSu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("NoiDungJson");
                    byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    return Base64.getEncoder().encodeToString(bytes);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
}