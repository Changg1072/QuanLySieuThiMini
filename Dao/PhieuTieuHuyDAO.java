package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Data.PhieuTieuHuy;

public class PhieuTieuHuyDAO {

    private static final PhieuTieuHuyDAO instance = new PhieuTieuHuyDAO();

    private PhieuTieuHuyDAO() {}

    public static PhieuTieuHuyDAO getInstance() {
        return instance;
    }

    private PhieuTieuHuy mapResultSetToPhieuTieuHuy(ResultSet rs) throws SQLException {
        String maPhieuHuy = rs.getString("MaPhieuHuy");
        
        Timestamp ngayTaoSQL = rs.getTimestamp("NgayTao");
        LocalDateTime ngayTao = (ngayTaoSQL != null) ? ngayTaoSQL.toLocalDateTime() : null;
        
        String maNV = rs.getString("MaNV");
        int tongSoLuong = rs.getInt("TongSoLuong");
        BigDecimal tongGiaTriHuy = rs.getBigDecimal("TongGiaTriHuy");
        String lyDoHuy = rs.getString("LyDoHuy");
        String trangThaiHuy = rs.getString("TrangThaiHuy");

        return new PhieuTieuHuy.ThoXayPhieuTieuHuy()
                .ganMaPhieuHuy(maPhieuHuy)
                .ganNgayTao(ngayTao)
                .ganMaNV(maNV)
                .ganTongSoLuong(tongSoLuong)
                .ganTongGiaTriHuy(tongGiaTriHuy)
                .ganLyDoHuy(lyDoHuy)
                .ganTrangThaiHuy(trangThaiHuy)
                .taoMoi();
    }

    public List<PhieuTieuHuy> layDanhSachPhieuTieuHuy() {
        List<PhieuTieuHuy> dsPhieu = new ArrayList<>();
        String sql = "SELECT * FROM PhieuTieuHuy";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                dsPhieu.add(mapResultSetToPhieuTieuHuy(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachPhieuTieuHuy", e);
        }
        return dsPhieu;
    }

    public boolean themPhieuTieuHuy(PhieuTieuHuy phieu) {
        String sql = "INSERT INTO PhieuTieuHuy (MaPhieuHuy, NgayTao, MaNV, TongSoLuong, TongGiaTriHuy, LyDoHuy, TrangThaiHuy) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, phieu.getMaPhieuHuy());
            
            if (phieu.getNgayTao() != null) pstmt.setTimestamp(2, Timestamp.valueOf(phieu.getNgayTao()));
            else pstmt.setNull(2, Types.TIMESTAMP);
            
            pstmt.setString(3, phieu.getMaNV());
            pstmt.setInt(4, phieu.getTongSoLuong());
            pstmt.setBigDecimal(5, phieu.getTongGiaTriHuy());
            pstmt.setString(6, phieu.getLyDoHuy());
            pstmt.setString(7, phieu.getTrangThaiHuy());

            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("themPhieuTieuHuy", e);
        }
        return false;
    }

    public boolean capNhatTrangThai(String maPhieuHuy, String trangThaiMoi) {
        String sql = "UPDATE PhieuTieuHuy SET TrangThaiHuy = ? WHERE MaPhieuHuy = ?";
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
             
            pstmt.setString(1, trangThaiMoi);
            pstmt.setString(2, maPhieuHuy);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("capNhatTrangThai", e);
        }
        return false;
    }

    private void logError(String method, Exception e) {
        System.err.println("[PhieuTieuHuyDAO - " + method + "] ERROR: " + e.getMessage());
    }
}