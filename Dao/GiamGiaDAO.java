package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Data.GiamGia;

public class GiamGiaDAO {

    private static final GiamGiaDAO instance = new GiamGiaDAO();

    private GiamGiaDAO() {}

    public static GiamGiaDAO getInstance() {
        return instance;
    }

    private GiamGia mapResultSetToGiamGia(ResultSet rs) throws SQLException {
        String maGiamGia = rs.getString("MaGiamGia");
        String maSP = rs.getString("MaSP");
        
        Timestamp bdSQL = rs.getTimestamp("BatDau");
        LocalDateTime batDau = (bdSQL != null) ? bdSQL.toLocalDateTime() : null;
        
        Timestamp ktSQL = rs.getTimestamp("KetThuc");
        LocalDateTime ketThuc = (ktSQL != null) ? ktSQL.toLocalDateTime() : null;
        
        BigDecimal giamGia = rs.getBigDecimal("GiamGia");
        String loaiGiamGia = rs.getString("LoaiGiamGia");
        String trangThai = rs.getString("TrangThaiGiamGia");
        int soLuong = rs.getInt("SoLuongApDung");

        return new GiamGia.ThoXayGiamGia()
                .ganMaGiamGia(maGiamGia)
                .ganMaSP(maSP)
                .ganBatDau(batDau)
                .ganKetThuc(ketThuc)
                .ganGiamGia(giamGia)
                .ganLoaiGiamGia(loaiGiamGia)
                .ganTrangThaiGiamGia(trangThai)
                .ganSoLuongApDung(soLuong)
                .taoMoi();
    }

    public List<GiamGia> layDanhSachGiamGia() {
        List<GiamGia> dsGiamGia = new ArrayList<>();
        String sql = "SELECT * FROM GiamGia";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                dsGiamGia.add(mapResultSetToGiamGia(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachGiamGia", e);
        }
        return dsGiamGia;
    }

    public boolean themGiamGia(GiamGia gg) {
        String sql = "INSERT INTO GiamGia (MaGiamGia, MaSP, BatDau, KetThuc, GiamGia, LoaiGiamGia, TrangThaiGiamGia, SoLuongApDung) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, gg.getMaGiamGia());
            pstmt.setString(2, gg.getMaSP());
            
            if (gg.getBatDau() != null) pstmt.setTimestamp(3, Timestamp.valueOf(gg.getBatDau()));
            else pstmt.setNull(3, Types.TIMESTAMP);
            
            if (gg.getKetThuc() != null) pstmt.setTimestamp(4, Timestamp.valueOf(gg.getKetThuc()));
            else pstmt.setNull(4, Types.TIMESTAMP);

            pstmt.setBigDecimal(5, gg.getGiamGia());
            pstmt.setString(6, gg.getLoaiGiamGia());
            pstmt.setString(7, gg.getTrangThaiGiamGia());
            pstmt.setInt(8, gg.getSoLuongApDung());

            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("themGiamGia", e);
        }
        return false;
    }

    public BigDecimal layMucGiamGiaHienTai(String maSP) {
        BigDecimal mucGiamGia = BigDecimal.ZERO;
        String sql = "SELECT GiamGia FROM GiamGia " + 
                     "WHERE MaSP = ? " + 
                     "AND TrangThaiGiamGia = N'Đang diễn ra' " + 
                     "AND GETDATE() BETWEEN BatDau AND KetThuc " + 
                     "AND SoLuongApDung > 0";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
             
            pstmt.setString(1, maSP);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    mucGiamGia = rs.getBigDecimal("GiamGia");
                }
            }
        } catch (SQLException e) {
            logError("layMucGiamGiaHienTai", e);
        }
        return mucGiamGia;
    }

    // =========================================================================
    // HÀM ĐÃ ĐƯỢC NÂNG CẤP: TỰ ĐỘNG KẾT THÚC KHI HẾT SUẤT
    // =========================================================================
    public boolean truSoLuongGiamGia(String maSP, int soLuongDaMua) {
        String sql = "UPDATE GiamGia SET " +
                     "SoLuongApDung = SoLuongApDung - ?, " +
                     "TrangThaiGiamGia = CASE WHEN SoLuongApDung - ? <= 0 THEN N'Đã kết thúc' ELSE TrangThaiGiamGia END " + 
                     "WHERE MaSP = ? AND TrangThaiGiamGia = N'Đang diễn ra' AND SoLuongApDung >= ?";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
             
            pstmt.setInt(1, soLuongDaMua);
            pstmt.setInt(2, soLuongDaMua); // Truyền lần 2 cho biểu thức CASE WHEN
            pstmt.setString(3, maSP);
            pstmt.setInt(4, soLuongDaMua);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("truSoLuongGiamGia", e);
        }
        return false;
    }
    public String layMaGiamGiaHienTai(String maSP) {
        String maGG = null;
        String sql = "SELECT MaGiamGia FROM GiamGia " + 
                     "WHERE MaSP = ? " + 
                     "AND TrangThaiGiamGia = N'Đang diễn ra' " + 
                     "AND GETDATE() BETWEEN BatDau AND KetThuc " + 
                     "AND SoLuongApDung > 0";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
             
            pstmt.setString(1, maSP);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    maGG = rs.getString("MaGiamGia");
                }
            }
        } catch (SQLException e) {
            logError("layMaGiamGiaHienTai", e);
        }
        return maGG;
    }

    public boolean huyGiamGia(String maSP) {
        String sql = "UPDATE GiamGia SET TrangThaiGiamGia = N'Đã kết thúc' " + 
                     "WHERE MaSP = ? AND TrangThaiGiamGia = N'Đang diễn ra'";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
             
            pstmt.setString(1, maSP);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("huyGiamGia", e);
        }
        return false;
    }

    private void logError(String method, Exception e) {
        System.err.println("[GiamGiaDAO - " + method + "] ERROR: " + e.getMessage());
    }
}