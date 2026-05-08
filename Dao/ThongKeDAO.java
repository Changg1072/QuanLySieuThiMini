package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import Data.ThongKe;

public class ThongKeDAO {

    private static final ThongKeDAO instance = new ThongKeDAO();

    private ThongKeDAO() {}

    public static ThongKeDAO getInstance() {
        return instance;
    }

    // =====================================================================
    // PHẦN 1: QUẢN LÝ CÁC BẢN BÁO CÁO ĐÃ CHỐT SỔ (LƯU TRỮ VÀO DATABASE)
    // =====================================================================

    public List<ThongKe> layDanhSachThongKe() {
        List<ThongKe> list = new ArrayList<>();
        String sql = "SELECT * FROM ThongKe ORDER BY NgayThongKe DESC";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql); 
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                list.add(mapResultSetToThongKe(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachThongKe", e);
        }
        return list;
    }

    public boolean themThongKe(ThongKe tk) {
        String sql = "INSERT INTO ThongKe (MaThongKe, NgayThongKe, TongDoanhThu, TongLoiNhuan, TongDonHang, TongKhachMoi, TongTienGiam, ChiPhiNhapHang, MaNV, ThietHai) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tk.getMaThongKe());
            ps.setDate(2, (tk.getNgayThongKe() != null) ? Date.valueOf(tk.getNgayThongKe()) : Date.valueOf(LocalDate.now()));
            
            ps.setBigDecimal(3, tk.getTongDoanhThu());
            ps.setBigDecimal(4, tk.getTongLoiNhuan());
            ps.setInt(5, tk.getTongDonHang());
            ps.setInt(6, tk.getTongKhachMoi());
            ps.setBigDecimal(7, tk.getTongTienGiam());
            ps.setBigDecimal(8, tk.getChiPhiNhapHang());
            ps.setString(9, tk.getMaNV());
            ps.setBigDecimal(10, tk.getThietHai());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("themThongKe", e);
        }
        return false;
    }

    // =====================================================================
    // PHẦN 2: CÁC HÀM PHÂN TÍCH & TÍNH TOÁN SỐ LIỆU THỰC TẾ
    // =====================================================================

    // 2.1 Thống kê doanh thu 12 tháng (Đã đổi sang BigDecimal)
    public List<Object[]> thongKeDoanhThuTheoNam(int nam) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT MONTH(NgayTao) AS Thang, SUM(ThanhTien) AS TongDoanhThu "
                   + "FROM HoaDon WHERE YEAR(NgayTao) = ? GROUP BY MONTH(NgayTao) ORDER BY Thang ASC";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, nam);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[2];
                    row[0] = "Tháng " + rs.getInt("Thang");
                    row[1] = rs.getBigDecimal("TongDoanhThu"); // Chính xác tuyệt đối
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logError("thongKeDoanhThuTheoNam", e);
        }
        return list;
    }

    // 2.2 Top khách hàng VIP (Đã đổi sang BigDecimal)
    public List<Object[]> topKhachHangVIP(int top) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT TOP " + top + " kh.HoTen, SUM(hd.ThanhTien) AS TongTienDaMua " 
                   + "FROM HoaDon hd "
                   + "JOIN KhachHang kh ON hd.MaKH = kh.MaKH " 
                   + "GROUP BY kh.HoTen ORDER BY TongTienDaMua DESC";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql); 
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                Object[] row = new Object[2];
                row[0] = rs.getString("HoTen");
                row[1] = rs.getBigDecimal("TongTienDaMua");
                list.add(row);
            }
        } catch (SQLException e) {
            logError("topKhachHangVIP", e);
        }
        return list;
    }
 // ==============================
    // 2.4 Xóa bản thống kê
    // ==============================
    public boolean xoaThongKe(String maThongKe) {
        String sql = "DELETE FROM ThongKe WHERE MaThongKe = ?";
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maThongKe);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("xoaThongKe", e);
        }
        return false;
    }

    // ==============================
    // 2.5 Top sản phẩm bán chạy nhất trong tháng
    // ==============================
    public List<Object[]> topSanPhamBanChay(int thang, int nam, int top) {
        List<Object[]> list = new ArrayList<>();
        // Kết hợp ChiTietHoaDon, HoaDon và SanPham để đếm số lượng bán
        String sql = "SELECT TOP " + top + " sp.TenSP, SUM(ct.SoLuong) AS TongSoLuongBan "
                   + "FROM ChiTietHoaDon ct "
                   + "JOIN HoaDon hd ON ct.MaHD = hd.MaHD "
                   + "JOIN SanPham sp ON ct.MaSP = sp.MaSP "
                   + "WHERE MONTH(hd.NgayTao) = ? AND YEAR(hd.NgayTao) = ? "
                   + "GROUP BY sp.TenSP ORDER BY TongSoLuongBan DESC";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, thang);
            ps.setInt(2, nam);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[2];
                    row[0] = rs.getString("TenSP");
                    row[1] = rs.getInt("TongSoLuongBan");
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logError("topSanPhamBanChay", e);
        }
        return list;
    }

    // 2.3 Thuật toán tính toán lợi nhuận (Dùng BigDecimal toàn diện)
    public BigDecimal[] thongKeLoiNhuanThang(int thang, int nam) {
        BigDecimal[] ketQua = new BigDecimal[4]; 
        // [0]DoanhThu, [1]Von, [2]ThietHai, [3]LoiNhuan
        for (int i = 0; i < 4; i++) ketQua[i] = BigDecimal.ZERO;

        try (Connection con = ConnectDB.getInstance().getConnection()) {
            
            // 1. Tổng Doanh Thu
            String sqlDoanhThu = "SELECT SUM(ThanhTien) FROM HoaDon WHERE MONTH(NgayTao)=? AND YEAR(NgayTao)=?";
            try (PreparedStatement ps1 = con.prepareStatement(sqlDoanhThu)) {
                ps1.setInt(1, thang); ps1.setInt(2, nam);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    if (rs1.next() && rs1.getBigDecimal(1) != null) ketQua[0] = rs1.getBigDecimal(1);
                }
            }

            // 2. Tổng Tiền Vốn
            String sqlVon = "SELECT SUM(cthd.SoLuong * ctlh.GiaNhap) FROM ChiTietHoaDon cthd "
                          + "JOIN ChiTietLoHang ctlh ON cthd.MaLoHang = ctlh.MaLoHang AND cthd.MaSP = ctlh.MaSP "
                          + "JOIN HoaDon hd ON cthd.MaHD = hd.MaHD " 
                          + "WHERE MONTH(hd.NgayTao)=? AND YEAR(hd.NgayTao)=?";
            try (PreparedStatement ps2 = con.prepareStatement(sqlVon)) {
                ps2.setInt(1, thang); ps2.setInt(2, nam);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next() && rs2.getBigDecimal(1) != null) ketQua[1] = rs2.getBigDecimal(1);
                }
            }

            // 3. Tính Thiệt Hại (Hao hụt + Hết hạn)
            BigDecimal thietHaiHaoHut = BigDecimal.ZERO;
            BigDecimal thietHaiHetDate = BigDecimal.ZERO;

            String sqlHaoHut = "SELECT SUM((kk.SoLuongHeThong - kk.SoLuongThucTe)*ctlh.GiaNhap) "
                             + "FROM KiemKeKho kk "
                             + "JOIN ChiTietLoHang ctlh ON ctlh.MaLoHang = kk.MaLoHang AND kk.MaSP = ctlh.MaSP "
                             + "WHERE MONTH(kk.NgayKiemKe)=? AND YEAR(kk.NgayKiemKe)=? "
                             + "AND kk.SoLuongHeThong > kk.SoLuongThucTe AND kk.TrangThai = N'Đã Cân Kho'";
            try (PreparedStatement ps3 = con.prepareStatement(sqlHaoHut)) {
                ps3.setInt(1, thang); ps3.setInt(2, nam);
                try (ResultSet rs3 = ps3.executeQuery()) {
                    if (rs3.next() && rs3.getBigDecimal(1) != null) thietHaiHaoHut = rs3.getBigDecimal(1);
                }
            }

            String sqlHetDate = "SELECT SUM(SoLuongTon * GiaNhap) FROM ChiTietLoHang "
                              + "WHERE SoLuongTon > 0 AND MONTH(HSD)=? AND YEAR(HSD)=? AND HSD < GETDATE()";
            try (PreparedStatement ps4 = con.prepareStatement(sqlHetDate)) {
                ps4.setInt(1, thang); ps4.setInt(2, nam);
                try (ResultSet rs4 = ps4.executeQuery()) {
                    if (rs4.next() && rs4.getBigDecimal(1) != null) thietHaiHetDate = rs4.getBigDecimal(1);
                }
            }

            ketQua[2] = thietHaiHaoHut.add(thietHaiHetDate);

            // 4. Lợi Nhuận Thực Tế = Doanh Thu - Vốn - Thiệt Hại
            ketQua[3] = ketQua[0].subtract(ketQua[1]).subtract(ketQua[2]);

        } catch (SQLException e) {
            logError("thongKeLoiNhuanThang", e);
        }
        
        return ketQua;
    }

    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS) - ĐƯỢC GIẤU XUỐNG CUỐI
    // =========================================================

    private ThongKe mapResultSetToThongKe(ResultSet rs) throws SQLException {
        Date sqlNgay = rs.getDate("NgayThongKe");
        LocalDate ngayThongKe = (sqlNgay != null) ? sqlNgay.toLocalDate() : null;
        
        return new ThongKe.ThoXayThongKe()
                .ganMaThongKe(rs.getString("MaThongKe"))
                .ganNgayThongKe(ngayThongKe)
                .ganTongDoanhThu(rs.getBigDecimal("TongDoanhThu"))
                .ganTongLoiNhuan(rs.getBigDecimal("TongLoiNhuan"))
                .ganTongDonHang(rs.getInt("TongDonHang"))
                .ganTongKhachMoi(rs.getInt("TongKhachMoi"))
                .ganTongTienGiam(rs.getBigDecimal("TongTienGiam"))
                .ganChiPhiNhapHang(rs.getBigDecimal("ChiPhiNhapHang"))
                .ganMaNV(rs.getString("MaNV"))
                .ganThietHai(rs.getBigDecimal("ThietHai"))
                .taoMoi();
    }

    private void logError(String method, Exception e) {
        System.err.println("[ThongKeDAO - " + method + "] ERROR: " + e.getMessage());
    }
}