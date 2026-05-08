// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//giảm giá tự động nên để xuống logic
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
    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS)
    // =========================================================

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
    // ==============================
    // 1. LẤY TOÀN BỘ DANH SÁCH GIẢM GIÁ
    // ==============================
    public List<GiamGia> layDanhSachGiamGia() {
        List<GiamGia> dsGiamGia = new ArrayList<>();
        String sql = "SELECT * FROM GiamGia";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsGiamGia.add(mapResultSetToGiamGia(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachGiamGia", e);
        }
        return dsGiamGia;
    }

    // ==============================
    // 2. TẠO CHƯƠNG TRÌNH GIẢM GIÁ MỚI
    // ==============================
    public boolean themGiamGia(GiamGia gg) {
        String sql = "INSERT INTO GiamGia (MaGiamGia, MaSP, BatDau, KetThuc, GiamGia, LoaiGiamGia, TrangThaiGiamGia, SoLuongApDung) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, gg.getMaGiamGia());
            pstmt.setString(2, gg.getMaSP());
            
            if (gg.getBatDau() != null) {
                pstmt.setTimestamp(3, Timestamp.valueOf(gg.getBatDau()));
            } else {
                pstmt.setNull(3, Types.TIMESTAMP);
            }
            
            if (gg.getKetThuc() != null) {
                pstmt.setTimestamp(4, Timestamp.valueOf(gg.getKetThuc()));
            } else {
                pstmt.setNull(4, Types.TIMESTAMP);
            }

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

    // ==============================
    // 3. LẤY MỨC GIẢM GIÁ HIỆN TẠI (DÙNG KHI BÁN HÀNG)
    // ==============================
    public BigDecimal layMucGiamGiaHienTai(String maSP) {
        BigDecimal mucGiamGia = BigDecimal.ZERO;
        String sql = "SELECT GiamGia FROM GiamGia " + 
                     "WHERE MaSP = ? " + 
                     "AND TrangThaiGiamGia = N'Đang diễn ra' " + 
                     "AND GETDATE() BETWEEN BatDau AND KetThuc " + 
                     "AND SoLuongApDung > 0";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
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

    // ==============================
    // 4. TRỪ SỐ LƯỢNG ÁP DỤNG
    // ==============================
    public boolean truSoLuongGiamGia(String maSP, int soLuongDaMua) {
        String sql = "UPDATE GiamGia SET SoLuongApDung = SoLuongApDung - ? " + 
                     "WHERE MaSP = ? AND TrangThaiGiamGia = N'Đang diễn ra' AND SoLuongApDung >= ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setInt(1, soLuongDaMua);
            pstmt.setString(2, maSP);
            pstmt.setInt(3, soLuongDaMua);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("truSoLuongGiamGia", e);
        }
        return false;
    }

    // ==============================
    // 5. HỦY CHƯƠNG TRÌNH GIẢM GIÁ
    // ==============================
    public boolean huyGiamGia(String maSP) {
        String sql = "UPDATE GiamGia SET TrangThaiGiamGia = N'Đã kết thúc' " + 
                     "WHERE MaSP = ? AND TrangThaiGiamGia = N'Đang diễn ra'";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
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