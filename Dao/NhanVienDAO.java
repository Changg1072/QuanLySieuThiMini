package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import Data.NhanVien;

public class NhanVienDAO {

    private static final NhanVienDAO instance = new NhanVienDAO();

    private NhanVienDAO() {}

    public static NhanVienDAO getInstance() {
        return instance;
    }

    // ==============================
    // HELPER: MAP RESULTSET → OBJECT
    // ==============================
    private NhanVien mapResultSetToNhanVien(ResultSet rs) throws SQLException {
        String maNV = rs.getString("MaNV");
        String hoTen = rs.getString("HoTen");
        String sdt = rs.getString("SDT");
        String chucVu = rs.getString("ChucVu");
        String trangThai = rs.getString("TrangThai");
        BigDecimal luongGio = rs.getBigDecimal("LuongGio");

        Date vaoLamSQL = rs.getDate("NgayVaoLam");
        LocalDate ngayVaoLam = (vaoLamSQL != null) ? vaoLamSQL.toLocalDate() : null;

        Date nghiSQL = rs.getDate("NgayNghiViec");
        LocalDate ngayNghiViec = (nghiSQL != null) ? nghiSQL.toLocalDate() : null;

        return new NhanVien.ThoXayNhanVien()
                .ganMaNV(maNV)
                .ganHoTen(hoTen)
                .ganSDT(sdt)
                .ganChucVu(chucVu)
                .ganTrangThai(trangThai)
                .ganLuongGio(luongGio)
                .ganNgayVaoLam(ngayVaoLam)
                .ganNgayNghiViec(ngayNghiViec)
                .taoMoi();
    }

    // ==============================
    // 1. LẤY DANH SÁCH
    // ==============================
    public List<NhanVien> layDanhSachNhanVien() {
        List<NhanVien> list = new ArrayList<>();
        String sql = "SELECT * FROM NhanVien";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                list.add(mapResultSetToNhanVien(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachNhanVien", e);
        }

        return list;
    }
	// ==============================
    // 2. THÊM NHÂN VIÊN
    // ==============================
    public boolean themNhanVien(NhanVien nv) {
        // LUÔN GHI RÕ TÊN CỘT ĐỂ AN TOÀN TUYỆT ĐỐI
        String sql = "INSERT INTO NhanVien (MaNV, HoTen, SDT, ChucVu, TrangThai, LuongGio, NgayVaoLam, NgayNghiViec) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, nv.getMaNV());
            ps.setString(2, nv.getHoTen());
            ps.setString(3, nv.getSDT());
            ps.setString(4, nv.getChucVu());
            ps.setString(5, nv.getTrangThai());
            ps.setBigDecimal(6, nv.getLuongGio());

            ps.setDate(7, (nv.getNgayVaoLam() != null) ? Date.valueOf(nv.getNgayVaoLam()) : Date.valueOf(LocalDate.now()));
            ps.setNull(8, Types.DATE); // Thêm mới thì chưa có ngày nghỉ

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("themNhanVien", e);
        }
        return false;
    }

    // ==============================
    // 3. CẬP NHẬT
    // ==============================
    public boolean suaNhanVien(NhanVien nv) {
        String sql = "UPDATE NhanVien SET HoTen=?, SDT=?, ChucVu=?, TrangThai=?, LuongGio=?, NgayVaoLam=?, NgayNghiViec=? WHERE MaNV=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            // ĐIỀN ĐÚNG THỨ TỰ CỦA CÂU LỆNH UPDATE
            ps.setString(1, nv.getHoTen());
            ps.setString(2, nv.getSDT());
            ps.setString(3, nv.getChucVu());
            ps.setString(4, nv.getTrangThai());
            ps.setBigDecimal(5, nv.getLuongGio());

            ps.setDate(6, (nv.getNgayVaoLam() != null) ? Date.valueOf(nv.getNgayVaoLam()) : Date.valueOf(LocalDate.now()));

            if ("Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai())) {
                ps.setDate(7, Date.valueOf(LocalDate.now()));
            } else {
                ps.setNull(7, Types.DATE);
            }

            ps.setString(8, nv.getMaNV()); // WHERE MaNV = ? nằm ở vị trí số 8

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("suaNhanVien", e);
        }
        return false;
    }
    // ==============================
    // 4. LẤY THEO MÃ
    // ==============================
    public NhanVien layNhanVienTheoMa(String maNV) {
        String sql = "SELECT * FROM NhanVien WHERE MaNV=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maNV);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToNhanVien(rs);
                }
            }

        } catch (SQLException e) {
            logError("layNhanVienTheoMa", e);
        }

        return null;
    }

    // ==============================
    // 5. TÍNH THÂM NIÊN
    // ==============================
    public int tinhThamNien(String maNV) {
        String sql = "SELECT \r\n"
        		+ "    DATEDIFF(YEAR, NgayVaoLam, GETDATE()) \r\n"
        		+ "    - CASE \r\n"
        		+ "        WHEN MONTH(GETDATE()) < MONTH(NgayVaoLam)\r\n"
        		+ "          OR (MONTH(GETDATE()) = MONTH(NgayVaoLam) AND DAY(GETDATE()) < DAY(NgayVaoLam))\r\n"
        		+ "        THEN 1 ELSE 0\r\n"
        		+ "      END\r\n"
        		+ "FROM NhanVien\r\n"
        		+ "WHERE MaNV=?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maNV);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            logError("tinhThamNien", e);
        }

        return 0;
    }

    // ==============================
    // LOG LỖI CHUYÊN NGHIỆP
    // ==============================
    private void logError(String method, Exception e) {
        System.err.println("[NhanVienDAO - " + method + "] ERROR: " + e.getMessage());
    }
}