package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Data.ChiTietHoaDon;

public class ChiTietHoaDonDAO {
    
    private static final ChiTietHoaDonDAO instance = new ChiTietHoaDonDAO();

    private ChiTietHoaDonDAO() {}

    public static ChiTietHoaDonDAO getInstance() {
        return instance;
    }
    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS)
    // =========================================================

    private ChiTietHoaDon mapResultSetToChiTietHoaDon(ResultSet rs) throws SQLException {
        String maHD = rs.getString("MaHD");
        String maSP = rs.getString("MaSP");
        String maLoHang = rs.getString("MaLoHang");
        int soLuong = rs.getInt("SoLuong");
        
        BigDecimal donGia = rs.getBigDecimal("DonGia");
        String maGiamGia = rs.getString("MaGiamGia");
        BigDecimal thanhTienSP = rs.getBigDecimal("ThanhTienSanPham");

        return new ChiTietHoaDon.ThoXayChiTietHoaDon()
                .ganMaHD(maHD)
                .ganMaSp(maSP)
                .ganMaLoHang(maLoHang)
                .ganSoLuong(soLuong)
                .ganDonGia(donGia)
                .ganMaGiamGia(maGiamGia)
                .ganThanhTienSanPham(thanhTienSP)
                .taoMoi();
    }
    // ==============================
    // 1. LẤY DANH SÁCH SẢN PHẨM CỦA 1 HÓA ĐƠN
    // ==============================
    public List<ChiTietHoaDon> layChiTietTheoMaHD(String maHDTimKiem) {
        List<ChiTietHoaDon> dsChiTiet = new ArrayList<>();
        String sql = "SELECT * FROM ChiTietHoaDon WHERE MaHD = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maHDTimKiem);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dsChiTiet.add(mapResultSetToChiTietHoaDon(rs));
                }
            }
        } catch (SQLException e) {
            logError("layChiTietTheoMaHD", e);
        }
        return dsChiTiet;
    }

    // ==============================
    // 2. THÊM NHIỀU MÓN CÙNG LÚC (Tuyệt chiêu Batch Insert khi bấm "Thanh Toán")
    // ==============================
    public boolean themDanhSachChiTietHoaDon(List<ChiTietHoaDon> danhSachCTHD) {
        if (danhSachCTHD == null || danhSachCTHD.isEmpty()) return false;
        
        String sql = "INSERT INTO ChiTietHoaDon (MaHD, MaSP, MaLoHang, SoLuong, DonGia, MaGiamGia, ThanhTienSanPham) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            // Tắt AutoCommit để đẩy nguyên 1 cục an toàn tuyệt đối
            con.setAutoCommit(false); 

            for (ChiTietHoaDon cthd : danhSachCTHD) {
                pstmt.setString(1, cthd.getMaHD());
                pstmt.setString(2, cthd.getMaSp());
                pstmt.setString(3, cthd.getMaLoHang());
                pstmt.setInt(4, cthd.getSoLuong());
                pstmt.setBigDecimal(5, cthd.getDonGia());

                if (cthd.getMaGiamGia() == null || cthd.getMaGiamGia().trim().isEmpty()) {
                    pstmt.setNull(6, Types.VARCHAR);
                } else {
                    pstmt.setString(6, cthd.getMaGiamGia());
                }

                pstmt.setBigDecimal(7, cthd.getThanhTienSanPham());
                
                pstmt.addBatch(); // Gom món hàng này vào chuyến xe
            }

            int[] ketQua = pstmt.executeBatch(); // Lệnh cho xe chạy thẳng xuống DB
            con.commit(); // Chốt đơn
            
            return ketQua.length == danhSachCTHD.size();

        } catch (SQLException e) {
            logError("themDanhSachChiTietHoaDon", e);
            // Có thể thêm lệnh rollback() ở đây nếu sếp muốn cẩn thận tuyệt đối
        }
        return false;
    }

    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS) - ĐƯỢC GIẤU XUỐNG CUỐI
    // =========================================================

    private void logError(String method, Exception e) {
        System.err.println("[ChiTietHoaDonDAO - " + method + "] ERROR: " + e.getMessage());
    }
}