package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import Data.KhachHang;

public class KhachHangDAO {

    private static final KhachHangDAO instance = new KhachHangDAO();

    private KhachHangDAO() {}

    public static KhachHangDAO getInstance() {
        return instance;
    }

    // ==============================
    // HELPER: MAP RESULTSET → OBJECT
    // ==============================
    private KhachHang mapResultSetToKhachHang(ResultSet rs) throws SQLException {
        String maKH = rs.getString("MaKH");
        String hoTen = rs.getString("HoTen");
        String sdt = rs.getString("SDT");
        BigDecimal diem = rs.getBigDecimal("DiemTichLuy");
        String bacKH = rs.getString("BacKH");

        Date ngayDK = rs.getDate("NgayDangKy");
        LocalDate ngayDangKy = (ngayDK != null) ? ngayDK.toLocalDate() : null;

        return new KhachHang.ThoXayKhachHang()
                .ganMaKH(maKH)
                .ganHoTen(hoTen)
                .ganSDT(sdt)
                .ganDiemTichLuy(diem)
                .ganNgayDangKy(ngayDangKy)
                .ganBacKH(bacKH)
                .taoMoi();
    }

    // ==============================
    // 1. LẤY DANH SÁCH KHÁCH HÀNG
    // ==============================
    public List<KhachHang> layDanhSachKhachHang() {
        List<KhachHang> dsKhachHang = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                dsKhachHang.add(mapResultSetToKhachHang(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachKhachHang", e);
        }

        return dsKhachHang;
    }

    // ==============================
    // 2. THÊM KHÁCH HÀNG
    // ==============================
    public boolean themKhachHang(KhachHang kh) {
        String sql = "INSERT INTO KhachHang (MaKH, HoTen, SDT, DiemTichLuy, NgayDangKy, BacKH) VALUES (?, ?, ?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, kh.getMaKH());
            ps.setString(2, kh.getHoTen());
            ps.setString(3, kh.getSDT());
            ps.setBigDecimal(4, kh.getDiemTichLuy());
            ps.setDate(5, (kh.getNgayDangKy() != null) ? Date.valueOf(kh.getNgayDangKy()) : Date.valueOf(LocalDate.now()));
            ps.setString(6, kh.getBacKH());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("themKhachHang", e);
        }
        return false;
    }

    // ==============================
    // 3. SỬA KHÁCH HÀNG
    // ==============================
    public boolean suaKhachHang(KhachHang kh) {
        String sql = "UPDATE KhachHang SET HoTen=?, SDT=?, DiemTichLuy=?, NgayDangKy=?, BacKH=? WHERE MaKH=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, kh.getHoTen());
            ps.setString(2, kh.getSDT());
            ps.setBigDecimal(3, kh.getDiemTichLuy());
            ps.setDate(4, (kh.getNgayDangKy() != null) ? Date.valueOf(kh.getNgayDangKy()) : Date.valueOf(LocalDate.now()));
            ps.setString(5, kh.getBacKH());
            ps.setString(6, kh.getMaKH());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("suaKhachHang", e);
        }
        return false;
    }

    // ==============================
    // 4. XÓA KHÁCH HÀNG
    // ==============================
    public boolean xoaKhachHang(String maKH) {
        String sql = "DELETE FROM KhachHang WHERE MaKH=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maKH);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("xoaKhachHang", e);
        }
        return false;
    }

    // ==============================
    // 5. TÌM KIẾM THEO SĐT GẦN ĐÚNG
    // ==============================
    public List<KhachHang> timKiemTheoSDT(String sdtTimKiem) {
        List<KhachHang> dsKetQua = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang WHERE SDT LIKE ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, "%" + sdtTimKiem + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dsKetQua.add(mapResultSetToKhachHang(rs));
                }
            }
        } catch (SQLException e) {
            logError("timKiemTheoSDT", e);
        }
        return dsKetQua;
    }

    // ==============================
    // 6. LẤY KHÁCH HÀNG THEO MÃ
    // ==============================
    public KhachHang layKhachHangTheoMa(String maKH) {
        String sql = "SELECT * FROM KhachHang WHERE MaKH = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maKH);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToKhachHang(rs);
                }
            }
        } catch (SQLException e) {
            logError("layKhachHangTheoMa", e);
        }
        return null;
    }

    // ==============================
    // 7. LẤY KHÁCH HÀNG THEO SĐT CHÍNH XÁC
    // ==============================
    public KhachHang layKhachHangTheoSDT(String sdt) {
        String sql = "SELECT * FROM KhachHang WHERE SDT = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, sdt);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToKhachHang(rs);
                }
            }
        } catch (SQLException e) {
            logError("layKhachHangTheoSDT", e);
        }
        return null;
    }

 // ==============================
    // 9. TRANSACTION: CẬP NHẬT ĐIỂM & HẠNG (Đồng bộ 100% BigDecimal)
    // ==============================
    public boolean CapNhatDiemVaHang(String maKH, BigDecimal tienThanhToanSauKhuyenMai) {
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return false;

        try {
            con.setAutoCommit(false);

            // 1. CỘNG ĐIỂM (100,000đ = 1 điểm. Dùng hàm divide của BigDecimal)
            BigDecimal tyLeQuyDoi = new BigDecimal("100000");
            // Chia lấy 2 số thập phân, làm tròn Half-Up
            BigDecimal diemCongThem = tienThanhToanSauKhuyenMai.divide(tyLeQuyDoi, 2, java.math.RoundingMode.HALF_UP);
            
            String sqlUpdateDiem = "UPDATE KhachHang SET DiemTichLuy = DiemTichLuy + ? WHERE MaKH = ?";
            try (PreparedStatement ps1 = con.prepareStatement(sqlUpdateDiem)) {
                ps1.setBigDecimal(1, diemCongThem);
                ps1.setString(2, maKH);
                ps1.executeUpdate();
            }

            // 2. TÍNH TỔNG TIỀN QUÝ (Hứng bằng BigDecimal)
            String sqlSumQuy = "SELECT SUM(ThanhTien) AS TongTienQuy FROM HoaDon " + 
                               "WHERE MaKH = ? " + 
                               "AND DATEPART(qq, NgayTao) = DATEPART(qq, GETDATE()) " + 
                               "AND YEAR(NgayTao) = YEAR(GETDATE())";
                               
            BigDecimal tongTienQuy = BigDecimal.ZERO;
            
            try (PreparedStatement ps2 = con.prepareStatement(sqlSumQuy)) {
                ps2.setString(1, maKH);
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next() && rs.getBigDecimal("TongTienQuy") != null) {
                        tongTienQuy = rs.getBigDecimal("TongTienQuy");
                    }
                }
            }

            // 3. CẬP NHẬT HẠNG (Dùng compareTo để so sánh)
            String hangMoi = "Không hạng";
            if (tongTienQuy.compareTo(new BigDecimal("6000000")) >= 0) {
                hangMoi = "Vip";
            } else if (tongTienQuy.compareTo(new BigDecimal("3000000")) >= 0) {
                hangMoi = "Vàng";
            } else if (tongTienQuy.compareTo(new BigDecimal("1000000")) >= 0) {
                hangMoi = "Bạc";
            }

            String sqlUpdateHang = "UPDATE KhachHang SET BacKH = ? WHERE MaKH = ?";
            try (PreparedStatement ps3 = con.prepareStatement(sqlUpdateHang)) {
                ps3.setString(1, hangMoi);
                ps3.setString(2, maKH);
                ps3.executeUpdate();
            }
            
            con.commit();
            return true;

        } catch (SQLException e) {
            logError("CapNhatDiemVaHang", e);
            try {
                if (con != null) con.rollback();
            } catch (SQLException ex) {
                logError("CapNhatDiemVaHang - Rollback", ex);
            }
            return false;
        } finally {
            try {
                if (con != null) con.setAutoCommit(true);
            } catch (SQLException e) {
                logError("CapNhatDiemVaHang - SetAutoCommit", e);
            }
        }
    }

    // ==============================
    // 10. LẤY PHẦN TRĂM GIẢM GIÁ (Đồng bộ 100% BigDecimal)
    // ==============================
    public BigDecimal layPhanTramGiamGia(String bacKH) {
        if (bacKH == null || bacKH.trim().isEmpty() || bacKH.equalsIgnoreCase("Không hạng")) {
            return new BigDecimal("0.01"); // Giảm 1%
        }
        switch (bacKH.trim()) {
            case "Vip":
                return new BigDecimal("0.10"); // Giảm 10%
            case "Vàng":
                return new BigDecimal("0.06"); // Giảm 6%
            case "Bạc":
                return new BigDecimal("0.03"); // Giảm 3%
            default:
                return new BigDecimal("0.01");
        }
    }

    // ==============================
    // LOG LỖI CHUYÊN NGHIỆP
    // ==============================
    private void logError(String method, Exception e) {
        System.err.println("[KhachHangDAO - " + method + "] ERROR: " + e.getMessage());
    }
}