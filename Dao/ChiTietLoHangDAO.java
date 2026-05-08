package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import Data.ChiTietLoHang;

public class ChiTietLoHangDAO {
    
    private static final ChiTietLoHangDAO instance = new ChiTietLoHangDAO();

    private ChiTietLoHangDAO() {}

    public static ChiTietLoHangDAO getInstance() {
        return instance;
    }

    // ==============================
    // HELPER: MAP RESULTSET → OBJECT
    // ==============================
    private ChiTietLoHang mapResultSetToChiTietLoHang(ResultSet rs) throws SQLException {
        String maLoHang = rs.getString("MaLoHang");
        String maSP = rs.getString("MaSP");
        BigDecimal giaNhap = rs.getBigDecimal("GiaNhap");
        int soLuongNhap = rs.getInt("SoLuongNhap");

        Date sqlNSX = rs.getDate("NSX");
        LocalDate nsx = (sqlNSX != null) ? sqlNSX.toLocalDate() : null;

        Date sqlHSD = rs.getDate("HSD");
        LocalDate hsd = (sqlHSD != null) ? sqlHSD.toLocalDate() : null;

        int soLuongTon = rs.getInt("SoLuongTon");

        return new ChiTietLoHang.ThoXayChiTietLoHang()
                .ganMaLoHang(maLoHang)
                .ganMaSP(maSP)
                .ganGiaNhap(giaNhap)
                .ganSoLuongNhap(soLuongNhap)
                .ganNSX(nsx)
                .ganHSD(hsd)
                .ganSoLuongTon(soLuongTon)
                .taoMoi();
    }

    // ==============================
    // 1. LẤY TOÀN BỘ CHI TIẾT LÔ HÀNG
    // ==============================
    public List<ChiTietLoHang> layDanhSachChiTietLoHang() {
        List<ChiTietLoHang> dsChiTiet = new ArrayList<>();
        String sql = "SELECT * FROM ChiTietLoHang";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsChiTiet.add(mapResultSetToChiTietLoHang(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachChiTietLoHang", e);
        }
        return dsChiTiet;
    }

    // ==============================
    // 2. LẤY CHI TIẾT THEO MÃ LÔ HÀNG
    // ==============================
    public List<ChiTietLoHang> layChiTietTheoMaLo(String maLoHangTimKiem) {
        List<ChiTietLoHang> dsChiTiet = new ArrayList<>();
        String sql = "SELECT * FROM ChiTietLoHang WHERE MaLoHang = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maLoHangTimKiem);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dsChiTiet.add(mapResultSetToChiTietLoHang(rs));
                }
            }
        } catch (SQLException e) {
            logError("layChiTietTheoMaLo", e);
        }
        return dsChiTiet;
    }

    // ==============================
    // 3. THÊM CHI TIẾT LÔ HÀNG
    // ==============================
    public boolean themChiTietLoHang(ChiTietLoHang ct) {
        String sql = "INSERT INTO ChiTietLoHang (MaLoHang, MaSP, GiaNhap, SoLuongNhap, NSX, HSD, SoLuongTon) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, ct.getMaLoHang());
            pstmt.setString(2, ct.getMaSP());
            pstmt.setBigDecimal(3, ct.getGiaNhap()); // Đồng bộ dùng BigDecimal
            pstmt.setInt(4, ct.getSoLuongNhap());

            pstmt.setDate(5, (ct.getNSX() != null) ? Date.valueOf(ct.getNSX()) : null);
            pstmt.setDate(6, (ct.getHSD() != null) ? Date.valueOf(ct.getHSD()) : null);

            pstmt.setInt(7, ct.getSoLuongTon()); 

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("themChiTietLoHang", e);
        }
        return false;
    }

    // ==============================
    // 4. SỬA THÔNG TIN CHI TIẾT LÔ HÀNG
    // ==============================
    public boolean suaChiTietLoHang(ChiTietLoHang ct) {
        String sql = "UPDATE ChiTietLoHang SET GiaNhap = ?, SoLuongNhap = ?, NSX = ?, HSD = ?, SoLuongTon = ? WHERE MaLoHang = ? AND MaSP = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setBigDecimal(1, ct.getGiaNhap());
            pstmt.setInt(2, ct.getSoLuongNhap());
            pstmt.setDate(3, (ct.getNSX() != null) ? Date.valueOf(ct.getNSX()) : null);
            pstmt.setDate(4, (ct.getHSD() != null) ? Date.valueOf(ct.getHSD()) : null);
            pstmt.setInt(5, ct.getSoLuongTon());
            pstmt.setString(6, ct.getMaLoHang());
            pstmt.setString(7, ct.getMaSP());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("suaChiTietLoHang", e);
        }
        return false;
    }

    // ==============================
    // 5. XÓA CHI TIẾT LÔ HÀNG
    // ==============================
    public boolean xoaChiTietLoHang(String maLoHang, String maSP) {
        String sql = "DELETE FROM ChiTietLoHang WHERE MaLoHang = ? AND MaSP = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maLoHang);
            pstmt.setString(2, maSP);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("xoaChiTietLoHang", e);
        }
        return false;
    }

    // ==============================
    // 6. TRỪ SỐ LƯỢNG TỒN (Bán hàng)
    // ==============================
    public boolean truSoLuongTon(String maLoHang, String maSP, int soLuongBan) {
        String sql = "UPDATE ChiTietLoHang SET SoLuongTon = SoLuongTon - ? WHERE MaLoHang = ? AND MaSP = ? AND SoLuongTon >= ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setInt(1, soLuongBan);
            pstmt.setString(2, maLoHang);
            pstmt.setString(3, maSP);
            pstmt.setInt(4, soLuongBan); // Đảm bảo không bị trừ âm kho

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("truSoLuongTon", e);
        }
        return false;
    }

    // ==============================
    // 7. CỘNG BÙ SỐ LƯỢNG TỒN (Trả hàng)
    // ==============================
    public boolean congSoLuongTon(String maLoHang, String maSP, int soLuongCong) {
        String sql = "UPDATE ChiTietLoHang SET SoLuongTon = SoLuongTon + ? WHERE MaLoHang = ? AND MaSP = ?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setInt(1, soLuongCong);
            pstmt.setString(2, maLoHang);
            pstmt.setString(3, maSP);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("congSoLuongTon", e);
        }
        return false;
    }

    // ==============================
    // 8. LẤY GIÁ NHẬP CAO NHẤT
    // ==============================
    public BigDecimal layGiaNhapCaoNhat(String maSP) {
        String sql = "SELECT MAX(GiaNhap) FROM ChiTietLoHang WHERE MaSP = ?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maSP);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal maxGiaNhap = rs.getBigDecimal(1);
                    return maxGiaNhap != null ? maxGiaNhap : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            logError("layGiaNhapCaoNhat", e);
        }
        return BigDecimal.ZERO; // Nếu chưa từng nhập hàng thì trả về 0
    }

    // ==============================
    // 9. LẤY LỊCH SỬ NHẬP HÀNG THEO SẢN PHẨM
    // ==============================
    public List<Object[]> layLichSuNhapTheoSP(String maSP) {
        List<Object[]> ketQua = new ArrayList<>();
        
        String sql = "SELECT ct.MaLoHang, ncc.TenNCC, lh.NgayNhapKho, ct.GiaNhap, ct.SoLuongNhap, ct.SoLuongTon, ct.HSD " +
                     "FROM ChiTietLoHang ct " +
                     "JOIN LoHang lh ON ct.MaLoHang = lh.MaLoHang " +
                     "JOIN NhaCungCap ncc ON lh.MaNCC = ncc.MaNCC " +
                     "WHERE ct.MaSP = ? " +
                     "ORDER BY lh.NgayNhapKho DESC";
                     
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maSP);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Date sqlNgayNhap = rs.getDate("NgayNhapKho");
                    LocalDate ngayNhap = (sqlNgayNhap != null) ? sqlNgayNhap.toLocalDate() : null;
                    
                    Date sqlHSD = rs.getDate("HSD");
                    LocalDate hsd = (sqlHSD != null) ? sqlHSD.toLocalDate() : null;

                    Object[] dong = new Object[]{
                        rs.getString("MaLoHang"),
                        rs.getString("TenNCC"),
                        ngayNhap,
                        rs.getBigDecimal("GiaNhap"), // Trả về BigDecimal để tính toán không sai số
                        rs.getInt("SoLuongNhap"),
                        rs.getInt("SoLuongTon"),
                        hsd
                    };
                    ketQua.add(dong);
                }
            }
        } catch (SQLException e) {
            logError("layLichSuNhapTheoSP", e);
        }
        return ketQua;
    }

    // ==============================
    // LOG LỖI CHUYÊN NGHIỆP
    // ==============================
    private void logError(String method, Exception e) {
        System.err.println("[ChiTietLoHangDAO - " + method + "] ERROR: " + e.getMessage());
    }
}