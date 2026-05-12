package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Data.HoaDon;

public class HoaDonDAO {
    
    private static final HoaDonDAO instance = new HoaDonDAO();

    private HoaDonDAO() {}

    public static HoaDonDAO getInstance() {
        return instance;
    }

    // ==============================
    // HELPER: MAP RESULTSET → OBJECT
    // ==============================
    private HoaDon mapResultSetToHoaDon(ResultSet rs) throws SQLException {
        String maHD = rs.getString("MaHD");
        
        Timestamp ngaySQL = rs.getTimestamp("NgayTao");
        LocalDateTime ngayTao = (ngaySQL != null) ? ngaySQL.toLocalDateTime() : null;
        
        String maKH = rs.getString("MaKH");
        String maNV = rs.getString("MaNV");
        
        // Sử dụng getBigDecimal để đồng bộ với kiểu dữ liệu trong Data.HoaDon
        BigDecimal thanhTien = rs.getBigDecimal("ThanhTien");
        BigDecimal tongGiamGia = rs.getBigDecimal("TongGiamGia");
        BigDecimal truTichDiem = rs.getBigDecimal("TruTichDiem");
        String phuongThucTT = rs.getString("PhuongThucTT");
        BigDecimal khachDua = rs.getBigDecimal("KhachDua");
        BigDecimal tienThua = rs.getBigDecimal("TienThua");
        String ghiChu = rs.getString("GhiChu");
        boolean traHang = rs.getBoolean("TraHang");
        String lyDoTraHang = rs.getString("LyDoTraHang");
        return new HoaDon.ThoXayHoaDon()
                .ganMaHD(maHD)
                .ganNgayTao(ngayTao)
                .ganMaKH(maKH)
                .ganMaNV(maNV)
                .ganThanhTien(thanhTien)
                .ganTongGiamGia(tongGiamGia)
                .ganTruTichDiem(truTichDiem)
                .ganPhuongThucTT(phuongThucTT)
                .ganKhachDua(khachDua)
                .ganTienThua(tienThua)
                .ganGhiChu(ghiChu)
                .ganTraHang(traHang)           // Cập nhật
                .ganLyDoTraHang(lyDoTraHang)
                .taoMoi();
    }

    // ==============================
    // 1. LẤY DANH SÁCH HÓA ĐƠN
    // ==============================
    public List<HoaDon> layDanhSachHoaDon() {
        List<HoaDon> dsHoaDon = new ArrayList<>();
        // Đưa bill mới nhất lên đầu (quan trọng cho trải nghiệm người dùng)
        String sql = "SELECT * FROM HoaDon ORDER BY NgayTao DESC"; 

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsHoaDon.add(mapResultSetToHoaDon(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachHoaDon", e);
        }
        return dsHoaDon;
    }

    // ==============================
    // 2. THÊM HÓA ĐƠN
    // ==============================
    public boolean themHoaDon(HoaDon hd) {
        String sql = "INSERT INTO HoaDon (MaHD, NgayTao, MaKH, MaNV, ThanhTien, TongGiamGia, TruTichDiem, PhuongThucTT, KhachDua, TienThua, GhiChu, TraHang, LyDoTraHang) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, hd.getMaHD());
            
            // Xử lý ngày tạo
            if (hd.getNgayTao() != null) {
                pstmt.setTimestamp(2, Timestamp.valueOf(hd.getNgayTao()));
            } else {
                pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            // Xử lý mã khách hàng (nếu khách vãng lai không có mã)
            if (hd.getMaKH() == null || hd.getMaKH().trim().isEmpty()) {
                pstmt.setNull(3, Types.VARCHAR);
            } else {
                pstmt.setString(3, hd.getMaKH());
            }

            pstmt.setString(4, hd.getMaNV());
            
            // Cập nhật dùng BigDecimal
            pstmt.setBigDecimal(5, hd.getThanhTien());
            pstmt.setBigDecimal(6, hd.getTongGiamGia());
            pstmt.setBigDecimal(7, hd.getTruTichDiem());
            pstmt.setString(8, hd.getPhuongThucTT());
            pstmt.setBigDecimal(9, hd.getKhachDua());
            pstmt.setBigDecimal(10, hd.getTienThua());
            pstmt.setString(11, hd.getGhiChu());
            pstmt.setBoolean(12, hd.getTraHang() != null ? hd.getTraHang() : false);
            pstmt.setString(13, hd.getLyDoTraHang());

            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("themHoaDon", e);
        }
        return false;
    }

    // ==============================
    // 3. TÌM HÓA ĐƠN THEO MÃ
    // ==============================
    public HoaDon layHoaDonTheoMa(String maHD) {
        String sql = "SELECT * FROM HoaDon WHERE MaHD = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maHD);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHoaDon(rs);
                }
            }
        } catch (SQLException e) {
            logError("layHoaDonTheoMa", e);
        }
        return null;
    }
 // ==============================
    // 4. TÌM DANH SÁCH HÓA ĐƠN THEO SĐT KHÁCH HÀNG
    // ==============================
    public List<HoaDon> layHoaDonTheoSDT(String sdt) {
        List<HoaDon> dsHoaDon = new ArrayList<>();
        
        // Kết nối bảng HoaDon (hd) và KhachHang (kh) để lọc theo SDT
        // Sắp xếp bill mới nhất lên đầu
        String sql = "SELECT hd.* FROM HoaDon hd " +
                     "JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
                     "WHERE kh.SDT = ? " +
                     "ORDER BY hd.NgayTao DESC";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, sdt);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dsHoaDon.add(mapResultSetToHoaDon(rs)); // Dùng lại được helper cũ cực kỳ tiện lợi
                }
            }
        } catch (SQLException e) {
            logError("layHoaDonTheoSDT", e);
        }
        return dsHoaDon;
    }
    // ==============================
    // LOG LỖI CHUYÊN NGHIỆP
    // ==============================
    private void logError(String method, Exception e) {
        System.err.println("[HoaDonDAO - " + method + "] ERROR: " + e.getMessage());
    }
}