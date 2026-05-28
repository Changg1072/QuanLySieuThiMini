package Dao;

import Data.BangLuong;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BangLuongDAO {

    private static final BangLuongDAO instance = new BangLuongDAO();

    private BangLuongDAO() {}

    public static BangLuongDAO getInstance() {
        return instance;
    }

    private BangLuong mapResultSetToBangLuong(ResultSet rs) throws SQLException {
        Date ngayTinhSQL = rs.getDate("NgayTinhLuong");

        return new BangLuong.ThoXayBangLuong()
                .ganMaBangLuong(rs.getInt("MaBangLuong"))
                .ganMaNV(rs.getString("MaNV"))
                .ganThangNam(rs.getString("ThangNam"))
                .ganTongGioLam(rs.getBigDecimal("TongGioLam"))
                .ganGioTangCa(rs.getBigDecimal("GioTangCa"))
                .ganLuongTheoGio(rs.getBigDecimal("LuongTheoGio"))
                .ganThuong(rs.getBigDecimal("Thuong"))
                .ganKhauTru(rs.getBigDecimal("KhauTru"))
                .ganPhuCap(rs.getBigDecimal("PhuCap"))
                .ganTongLuong(rs.getBigDecimal("TongLuong"))
                .ganNgayTinhLuong(ngayTinhSQL != null ? ngayTinhSQL.toLocalDate() : null)
                .taoMoi();
    }

    // ==========================================================
    // 1. TẠO BẢNG LƯƠNG CHO THÁNG
    // ==========================================================
    public boolean luuBangLuong(BangLuong bl) {
        String sql = "INSERT INTO BangLuong (MaNV, ThangNam, TongGioLam, GioTangCa, LuongTheoGio, Thuong, KhauTru, PhuCap, TongLuong, NgayTinhLuong) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, bl.getMaNV());
            ps.setString(2, bl.getThangNam());
            ps.setBigDecimal(3, bl.getTongGioLam());
            ps.setBigDecimal(4, bl.getGioTangCa());
            ps.setBigDecimal(5, bl.getLuongTheoGio());
            ps.setBigDecimal(6, bl.getThuong());
            ps.setBigDecimal(7, bl.getKhauTru());
            ps.setBigDecimal(8, bl.getPhuCap());
            ps.setBigDecimal(9, bl.getTongLuong());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("luuBangLuong", e);
        }
        return false;
    }

    // ==========================================================
    // 2. KIỂM TRA ĐÃ TÍNH LƯƠNG THÁNG NÀY CHƯA
    // ==========================================================
    public boolean kiemTraDaTinhLuong(String maNV, String thangNam) {
        String sql = "SELECT 1 FROM BangLuong WHERE MaNV = ? AND ThangNam = ?";
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maNV);
            ps.setString(2, thangNam);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logError("kiemTraDaTinhLuong", e);
        }
        return false;
    }

    // ==========================================================
    // 3. LẤY LỊCH SỬ BẢNG LƯƠNG CỦA NHÂN VIÊN
    // ==========================================================
    public List<BangLuong> layLichSuLuongTheoMaNV(String maNV) {
        List<BangLuong> ds = new ArrayList<>();
        String sql = "SELECT * FROM BangLuong WHERE MaNV = ? ORDER BY NgayTinhLuong DESC";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(mapResultSetToBangLuong(rs));
                }
            }
        } catch (SQLException e) {
            logError("layLichSuLuongTheoMaNV", e);
        }
        return ds;
    }

    private void logError(String method, Exception e) {
        System.err.println("[BangLuongDAO - " + method + "] ERROR: " + e.getMessage());
    }
}