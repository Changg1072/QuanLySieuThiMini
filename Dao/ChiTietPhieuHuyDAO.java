package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Data.ChiTietPhieuHuy;

public class ChiTietPhieuHuyDAO {

    private static final ChiTietPhieuHuyDAO instance = new ChiTietPhieuHuyDAO();

    private ChiTietPhieuHuyDAO() {}

    public static ChiTietPhieuHuyDAO getInstance() {
        return instance;
    }

    private ChiTietPhieuHuy mapResultSetToChiTiet(ResultSet rs) throws SQLException {
        String maPhieuHuy = rs.getString("MaPhieuHuy");
        String maLoHang = rs.getString("MaLoHang");
        String maSP = rs.getString("MaSP");
        int soLuongHuy = rs.getInt("SoLuongHuy");
        BigDecimal giaTriHuy = rs.getBigDecimal("GiaTriHuy");
        String lyDoChiTiet = rs.getString("LyDoChiTiet");

        return new ChiTietPhieuHuy.ThoXayChiTietPhieuHuy()
                .ganMaPhieuHuy(maPhieuHuy)
                .ganMaLoHang(maLoHang)
                .ganMaSP(maSP)
                .ganSoLuongHuy(soLuongHuy)
                .ganGiaTriHuy(giaTriHuy)
                .ganLyDoChiTiet(lyDoChiTiet)
                .taoMoi();
    }

    public List<ChiTietPhieuHuy> layDanhSachChiTietTheoMaPhieu(String maPhieuHuy) {
        List<ChiTietPhieuHuy> dsChiTiet = new ArrayList<>();
        String sql = "SELECT * FROM ChiTietPhieuHuy WHERE MaPhieuHuy = ?";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
             
            pstmt.setString(1, maPhieuHuy);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dsChiTiet.add(mapResultSetToChiTiet(rs));
                }
            }
        } catch (SQLException e) {
            logError("layDanhSachChiTietTheoMaPhieu", e);
        }
        return dsChiTiet;
    }

    public boolean themChiTietPhieuHuy(ChiTietPhieuHuy ct) {
        String sql = "INSERT INTO ChiTietPhieuHuy (MaPhieuHuy, MaLoHang, MaSP, SoLuongHuy, GiaTriHuy, LyDoChiTiet) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, ct.getMaPhieuHuy());
            pstmt.setString(2, ct.getMaLoHang());
            pstmt.setString(3, ct.getMaSP());
            pstmt.setInt(4, ct.getSoLuongHuy());
            pstmt.setBigDecimal(5, ct.getGiaTriHuy());
            pstmt.setString(6, ct.getLyDoChiTiet());

            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("themChiTietPhieuHuy", e);
        }
        return false;
    }

    private void logError(String method, Exception e) {
        System.err.println("[ChiTietPhieuHuyDAO - " + method + "] ERROR: " + e.getMessage());
    }
}