package Dao;

import Data.CauHinhLuong;
import java.sql.*;

public class CauHinhLuongDAO {

    private static final CauHinhLuongDAO instance = new CauHinhLuongDAO();

    private CauHinhLuongDAO() {}

    public static CauHinhLuongDAO getInstance() {
        return instance;
    }

    private CauHinhLuong mapResultSetToCauHinhLuong(ResultSet rs) throws SQLException {
        Date ngayApDungSQL = rs.getDate("NgayApDung");
        
        return new CauHinhLuong.ThoXayCauHinhLuong()
                .ganMaCauHinh(rs.getInt("MaCauHinh"))
                .ganMaNV(rs.getString("MaNV"))
                .ganLuongTheoGio(rs.getBigDecimal("LuongTheoGio"))
                .ganHeSoLuong(rs.getBigDecimal("HeSoLuong"))
                .ganPhuCapCoDinh(rs.getBigDecimal("PhuCapCoDinh"))
                .ganHeSoTangCa(rs.getBigDecimal("HeSoTangCa"))
                .ganNgayApDung(ngayApDungSQL != null ? ngayApDungSQL.toLocalDate() : null)
                .ganTrangThai(rs.getString("TrangThai"))
                .taoMoi();
    }

    // ==========================================================
    // 1. LẤY CẤU HÌNH LƯƠNG HIỆN TẠI ĐANG ÁP DỤNG CỦA NHÂN VIÊN
    // ==========================================================
    public CauHinhLuong layCauHinhHienTaiTheoMaNV(String maNV) {
        String sql = "SELECT TOP 1 * FROM CauHinhLuong WHERE MaNV = ? AND TrangThai = N'Đang áp dụng' ORDER BY NgayApDung DESC";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToCauHinhLuong(rs);
            }
        } catch (SQLException e) {
            logError("layCauHinhHienTaiTheoMaNV", e);
        }
        return null;
    }

    // ==========================================================
    // 2. THÊM CẤU HÌNH LƯƠNG MỚI (Và tự động vô hiệu hóa cấu hình cũ)
    // ==========================================================
    public boolean themCauHinhLuuTruLichSu(CauHinhLuong chl) {
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return false;

        String sqlUpdateCu = "UPDATE CauHinhLuong SET TrangThai = N'Hết hiệu lực' WHERE MaNV = ? AND TrangThai = N'Đang áp dụng'";
        String sqlInsertMoi = "INSERT INTO CauHinhLuong (MaNV, LuongTheoGio, HeSoLuong, PhuCapCoDinh, HeSoTangCa, NgayApDung, TrangThai) VALUES (?, ?, ?, ?, ?, GETDATE(), N'Đang áp dụng')";

        try {
            con.setAutoCommit(false); // Bật Transaction

            // 1. Vô hiệu hóa cái cũ
            try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdateCu)) {
                psUpdate.setString(1, chl.getMaNV());
                psUpdate.executeUpdate();
            }

            // 2. Thêm cái mới
            try (PreparedStatement psInsert = con.prepareStatement(sqlInsertMoi)) {
                psInsert.setString(1, chl.getMaNV());
                psInsert.setBigDecimal(2, chl.getLuongTheoGio());
                psInsert.setBigDecimal(3, chl.getHeSoLuong());
                psInsert.setBigDecimal(4, chl.getPhuCapCoDinh());
                psInsert.setBigDecimal(5, chl.getHeSoTangCa());
                psInsert.executeUpdate();
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ex) {}
            logError("themCauHinhLuuTruLichSu", e);
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException ex) {}
        }
        return false;
    }

    private void logError(String method, Exception e) {
        System.err.println("[CauHinhLuongDAO - " + method + "] ERROR: " + e.getMessage());
    }
}