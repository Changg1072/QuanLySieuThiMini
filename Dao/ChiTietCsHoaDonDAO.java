package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Data.ChiTietCsHoaDon;

public class ChiTietCsHoaDonDAO {

    private static final ChiTietCsHoaDonDAO instance = new ChiTietCsHoaDonDAO();

    private ChiTietCsHoaDonDAO() {}

    public static ChiTietCsHoaDonDAO getInstance() {
        return instance;
    }

    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS)
    // =========================================================

    private ChiTietCsHoaDon mapResultSetToChiTietCsHoaDon(ResultSet rs) throws SQLException {
        String maTraHang = rs.getString("MaTraHang");
        String maSP = rs.getString("MaSP");
        int soLuongTra = rs.getInt("SoLuongTra");
        BigDecimal thanhTienHoanTra = rs.getBigDecimal("ThanhTienHoanTra");

        return new ChiTietCsHoaDon.ThoXayChiTietCsHoaDon()
                .ganMaTraHang(maTraHang)
                .ganMaSP(maSP)
                .ganSoLuongTra(soLuongTra)
                .ganThanhTienHoanTra(thanhTienHoanTra)
                .taoMoi();
    }

    // ==============================
    // 1. LẤY DANH SÁCH MÓN HÀNG TRẢ LẠI THEO MÃ TRẢ HÀNG
    // ==============================
    public List<ChiTietCsHoaDon> layChiTietTheoMaTraHang(String maTraHangTimKiem) {
        List<ChiTietCsHoaDon> dsChiTiet = new ArrayList<>();
        String sql = "SELECT * FROM ChiTietCsHoaDon WHERE MaTraHang = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maTraHangTimKiem);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dsChiTiet.add(mapResultSetToChiTietCsHoaDon(rs));
                }
            }
        } catch (SQLException e) {
            logError("layChiTietTheoMaTraHang", e);
        }
        return dsChiTiet;
    }

    // ==============================
    // 2. THÊM DANH SÁCH MÓN TRẢ (Batch Insert - Tối ưu tốc độ)
    // ==============================
    public boolean themDanhSachChiTietTraHang(List<ChiTietCsHoaDon> dsTra) {
        if (dsTra == null || dsTra.isEmpty()) return false;

        String sql = "INSERT INTO ChiTietCsHoaDon (MaTraHang, MaSP, SoLuongTra, ThanhTienHoanTra) VALUES (?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            con.setAutoCommit(false); // Bắt đầu Transaction để đảm bảo an toàn

            for (ChiTietCsHoaDon ct : dsTra) {
                pstmt.setString(1, ct.getMaTraHang());
                pstmt.setString(2, ct.getMaSP());
                pstmt.setInt(3, ct.getSoLuongTra());
                pstmt.setBigDecimal(4, ct.getThanhTienHoanTra());
                
                pstmt.addBatch(); // Gom hàng vào chuyến xe
            }

            int[] results = pstmt.executeBatch(); // Đẩy nguyên lô xuống DB
            con.commit(); // Chốt đơn

            return results.length == dsTra.size();
        } catch (SQLException e) {
            logError("themDanhSachChiTietTraHang", e);
        }
        return false;
    }

    private void logError(String method, Exception e) {
        System.err.println("[ChiTietCsHoaDonDAO - " + method + "] ERROR: " + e.getMessage());
    }
}