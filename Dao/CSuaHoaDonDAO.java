package Dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Data.CSuaHoaDon;

public class CSuaHoaDonDAO {

    private static final CSuaHoaDonDAO instance = new CSuaHoaDonDAO();

    private CSuaHoaDonDAO() {}

    public static CSuaHoaDonDAO getInstance() {
        return instance;
    }

    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS) 
    // =========================================================

    private CSuaHoaDon mapResultSetToCSuaHoaDon(ResultSet rs) throws SQLException {
        String maTraHang = rs.getString("MaTraHang");
        String maHD = rs.getString("MaHD");

        Timestamp ngaySQL = rs.getTimestamp("NgayCS");
        LocalDateTime ngayCS = (ngaySQL != null) ? ngaySQL.toLocalDateTime() : null;

        String trangThaiCS = rs.getString("TrangThaiCS");
        String lyDoSua = rs.getString("LyDoSua");
        String maNV = rs.getString("MaNV");

        return new CSuaHoaDon.ThoXayCSuaHoaDon()
                .ganMaTraHang(maTraHang)
                .ganMaHD(maHD)
                .ganNgayCs(ngayCS) 
                .ganTrangThaiCS(trangThaiCS)
                .ganLyDoSua(lyDoSua)
                .ganMaNV(maNV)
                .taoMoi();
    }
    // ==============================
    // 1. LẤY DANH SÁCH CHỈNH SỬA HÓA ĐƠN
    // ==============================
    public List<CSuaHoaDon> layDanhSachCSuaHoaDon() {
        List<CSuaHoaDon> dsCSHD = new ArrayList<>();
        // Ưu tiên hiện các ca chỉnh sửa mới nhất lên đầu để quản lý dễ thấy
        String sql = "SELECT * FROM CSuaHoaDon ORDER BY NgayCS DESC";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsCSHD.add(mapResultSetToCSuaHoaDon(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachCSuaHoaDon", e);
        }
        return dsCSHD;
    }

    // ==============================
    // 2. THÊM LỊCH SỬ CHỈNH SỬA HÓA ĐƠN
    // ==============================
    public boolean themCSuaHoaDon(CSuaHoaDon cshd) {
        String sql = "INSERT INTO CSuaHoaDon (MaTraHang, MaHD, NgayCS, TrangThaiCS, LyDoSua, MaNV) VALUES (?, ?, ?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, cshd.getMaTraHang());
            pstmt.setString(2, cshd.getMaHD());

            if (cshd.getNgayCs() != null) {
                pstmt.setTimestamp(3, Timestamp.valueOf(cshd.getNgayCs()));
            } else {
                pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now())); // Mặc định là bây giờ
            }

            pstmt.setString(4, cshd.getTrangThaiCS());
            pstmt.setString(5, cshd.getLyDoSua());
            pstmt.setString(6, cshd.getMaNV());

            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("themCSuaHoaDon", e);
        }
        return false;
    }
 // ==============================
    // 3. TÌM KIẾM LỊCH SỬ THEO MÃ HÓA ĐƠN GỐC (Trở về List vì có thể sửa nhiều lần)
    // ==============================
    public List<CSuaHoaDon> layDanhSachCSuaTheoMaHD(String maHD) {
        List<CSuaHoaDon> dsLichSu = new ArrayList<>();
        String sql = "SELECT * FROM CSuaHoaDon WHERE MaHD = ? ORDER BY NgayCS DESC";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maHD);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dsLichSu.add(mapResultSetToCSuaHoaDon(rs));
                }
            }
        } catch (SQLException e) {
            logError("layDanhSachCSuaTheoMaHD", e);
        }
        return dsLichSu;
    }

    private void logError(String method, Exception e) {
        System.err.println("[CSuaHoaDonDAO - " + method + "] ERROR: " + e.getMessage());
    }
}