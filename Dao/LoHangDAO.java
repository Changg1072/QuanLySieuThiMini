package Dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import Data.LoHang;

public class LoHangDAO {

    // Eager Initialization: Khởi tạo Singleton ngay từ đầu
    private static final LoHangDAO instance = new LoHangDAO();

    private LoHangDAO() {
    }

    public static LoHangDAO getInstance() {
        return instance;
    }

    // ==============================
    // 1. LẤY DANH SÁCH LÔ HÀNG 
    // ==============================
    public List<LoHang> layDanhSachLoHang() {
        List<LoHang> dsLoHang = new ArrayList<>();
        String sql = "SELECT * FROM LoHang";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql); 
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsLoHang.add(mapResultSetToLoHang(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachLoHang", e);
        }
        return dsLoHang;
    }

    // ==============================
    // 2. THÊM LÔ HÀNG MỚI
    // ==============================
    public boolean themLoHang(LoHang lh) {
        String sql = "INSERT INTO LoHang(MaLoHang, MaNCC, NgayNhapKho, ThanhTien) VALUES (?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, lh.getMaLoHang());
            pstmt.setString(2, lh.getMaNCC());
            
            if (lh.getNgayNhapKho() != null) {
                pstmt.setDate(3, Date.valueOf(lh.getNgayNhapKho()));
            } else {
                pstmt.setDate(3, Date.valueOf(LocalDate.now())); // Mặc định là hôm nay nếu null
            }

            // CẬP NHẬT: Gán giá trị BigDecimal
            pstmt.setBigDecimal(4, lh.getThanhTien());

            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("themLoHang", e);
        }
        return false;
    }

    // ==============================
    // 3. SỬA LÔ HÀNG
    // ==============================
    public boolean suaLoHang(LoHang lh) {
        String sql = "UPDATE LoHang SET MaNCC = ?, NgayNhapKho = ?, ThanhTien = ? WHERE MaLoHang = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, lh.getMaNCC());

            if (lh.getNgayNhapKho() != null) {
                pstmt.setDate(2, Date.valueOf(lh.getNgayNhapKho()));
            } else {
                pstmt.setDate(2, Date.valueOf(LocalDate.now())); // An toàn tuyệt đối
            }

            // CẬP NHẬT: Gán giá trị BigDecimal
            pstmt.setBigDecimal(3, lh.getThanhTien());
            pstmt.setString(4, lh.getMaLoHang());

            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("suaLoHang", e);
        }
        return false;
    }

    // ==============================
    // 4. XÓA LÔ HÀNG
    // ==============================
    public boolean xoaLoHang(String maLoHang) {
        String sql = "DELETE FROM LoHang WHERE MaLoHang = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maLoHang);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("xoaLoHang", e);
        }
        return false;
    }

    // ==============================
    // 6. LẤY LÔ HÀNG THEO MÃ
    // ==============================
    public LoHang layLoHangTheoMa(String maLoHang) {
        String sql = "SELECT * FROM LoHang WHERE MaLoHang = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maLoHang);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLoHang(rs); 
                }
            }
        } catch (SQLException e) {
            logError("layLoHangTheoMa", e);
        }
        return null;
    }

    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS) - ĐƯỢC GIẤU XUỐNG CUỐI
    // =========================================================

    private LoHang mapResultSetToLoHang(ResultSet rs) throws SQLException {
        // Ép kiểu Date của SQL sang LocalDate của Java
        Date sqlNgayNhap = rs.getDate("NgayNhapKho");
        LocalDate ngayNhapKho = (sqlNgayNhap != null) ? sqlNgayNhap.toLocalDate() : null;

        // CẬP NHẬT: Lấy ThanhTien bằng kiểu BigDecimal
        BigDecimal thanhTien = rs.getBigDecimal("ThanhTien");

        return new LoHang.ThoXayLoHang()
                .ganMaLoHang(rs.getString("MaLoHang"))
                .ganMaNCC(rs.getString("MaNCC"))
                .ganNgayNhapKho(ngayNhapKho)
                .ganThanhTien(thanhTien)
                .taoMoi();
    }

    private void logError(String method, Exception e) {
        System.err.println("[LoHangDAO - " + method + "] ERROR: " + e.getMessage());
    }
}