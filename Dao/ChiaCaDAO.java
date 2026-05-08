package Dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Data.ChiaCa;

public class ChiaCaDAO {
    
    private static final ChiaCaDAO instance = new ChiaCaDAO();

    private ChiaCaDAO() {}

    public static ChiaCaDAO getInstance() {
        return instance;
    }

    // ==============================
    // HELPER: MAP RESULTSET → OBJECT
    // ==============================
    private ChiaCa mapResultSetToChiaCa(ResultSet rs) throws SQLException {
        String maCa = rs.getString("MaCa");
        String maLoaiCa = rs.getString("MaLoaiCa");
        
        Date sqlNgayLam = rs.getDate("NgayLam");
        LocalDate ngayLam = (sqlNgayLam != null) ? sqlNgayLam.toLocalDate() : null;
        
        String maNV = rs.getString("MaNV");
        
        Timestamp sqlCheckIn = rs.getTimestamp("ThoiGianCheckIn");
        LocalDateTime checkIn = (sqlCheckIn != null) ? sqlCheckIn.toLocalDateTime() : null;
        
        Timestamp sqlCheckOut = rs.getTimestamp("ThoiGianCheckOut");
        LocalDateTime checkOut = (sqlCheckOut != null) ? sqlCheckOut.toLocalDateTime() : null;
        
        String tinhTrang = rs.getString("TinhTrang");
        
        BigDecimal tienDauCa = rs.getBigDecimal("TienDauCa");
        BigDecimal tienCuoiCa = rs.getBigDecimal("TienCuoiCa");
        BigDecimal tienChenhLech = rs.getBigDecimal("TienChenhLech");
        BigDecimal tienBanHang = rs.getBigDecimal("TienBanHang");

        return new ChiaCa.ThoXayChiaCa()
                .ganMaCa(maCa)
                .ganMaLoaiCa(maLoaiCa)
                .ganNgayLam(ngayLam)
                .ganMaNV(maNV)
                .ganThoiGianCheckIn(checkIn)
                .ganThoiGianCheckOut(checkOut)
                .ganTinhTrang(tinhTrang)
                .ganTienDauCa(tienDauCa)
                .ganTienCuoiCa(tienCuoiCa)
                .ganTienChenhLech(tienChenhLech)
                .ganTienBanHang(tienBanHang)
                .taoMoi();
    }

    // ==============================
    // 1. LẤY DANH SÁCH CHIA CA
    // ==============================
    public List<ChiaCa> layDanhSachChiaCa() {
        List<ChiaCa> dsChiaCa = new ArrayList<>();
        String sql = "SELECT * FROM ChiaCa";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsChiaCa.add(mapResultSetToChiaCa(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachChiaCa", e);
        }
        return dsChiaCa;
    }

    // ==============================
    // 2. QUẢN LÝ TẠO CA VÀ PHÂN LUÔN CHO NHÂN VIÊN
    // ==============================
    public boolean themChiaCa(ChiaCa cc) {
        String sql = "INSERT INTO ChiaCa(MaCa, MaLoaiCa, NgayLam, MaNV, TinhTrang, TienDauCa) VALUES (?, ?, ?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, cc.getMaCa());
            pstmt.setString(2, cc.getMaLoaiCa());
            pstmt.setDate(3, (cc.getNgayLam() != null) ? Date.valueOf(cc.getNgayLam()) : null);

            if (cc.getMaNV() == null || cc.getMaNV().trim().isEmpty()) {
                pstmt.setNull(4, Types.VARCHAR);
            } else {
                pstmt.setString(4, cc.getMaNV());
            }

            // Đã chốt trực tiếp trạng thái "Đã phân công"
            pstmt.setString(5, "Đã phân công");
            pstmt.setBigDecimal(6, BigDecimal.ZERO); 

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("themChiaCa", e);
        }
        return false;
    }

    // ==============================
    // 3. SỬA THÔNG TIN CA (Đổi người, đổi ngày...)
    // ==============================
    public boolean suaChiaCa(ChiaCa cc) {
        String sql = "UPDATE ChiaCa SET MaLoaiCa = ?, NgayLam = ?, MaNV = ?, TinhTrang = ? WHERE MaCa = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, cc.getMaLoaiCa());
            pstmt.setDate(2, (cc.getNgayLam() != null) ? Date.valueOf(cc.getNgayLam()) : null);
            pstmt.setString(3, cc.getMaNV());
            pstmt.setString(4, cc.getTinhTrang());
            pstmt.setString(5, cc.getMaCa());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("suaChiaCa", e);
        }
        return false;
    }

    // ==============================
    // 4. HỦY CA LÀM (Chỉ cập nhật trạng thái, không xóa)
    // ==============================
    public boolean xoaChiaCa(String maCa) {
        String sql = "UPDATE ChiaCa SET TinhTrang = N'Đã hủy' WHERE MaCa = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maCa);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("xoaChiaCa", e);
        }
        return false;
    }

    // ==============================
    // 5. NGHIỆP VỤ CHECK-IN (Khi nhân viên bắt đầu ca)
    // ==============================
    public boolean capNhatCheckIn(String maCa, LocalDateTime thoiGianCheckIn, BigDecimal tienDauCaThucTe) {
        String sql = "UPDATE ChiaCa SET ThoiGianCheckIn = ?, TinhTrang = N'Đang làm việc', TienDauCa = ? WHERE MaCa = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setTimestamp(1, Timestamp.valueOf(thoiGianCheckIn));
            pstmt.setBigDecimal(2, tienDauCaThucTe); 
            pstmt.setString(3, maCa);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logError("capNhatCheckIn", e);
        }
        return false;
    }

    // ==============================
    // 6. NGHIỆP VỤ CHECK-OUT BÀN GIAO CA (Đã cập nhật linh hoạt trạng thái)
    // ==============================
    public boolean capNhatCheckOut(String maCa, LocalDateTime thoiGianCheckOut, BigDecimal tienCuoiCa, BigDecimal tienBanHang, BigDecimal tienChenhLech, String trangThaiThucTe) {
        String sql = "UPDATE ChiaCa SET ThoiGianCheckOut = ?, TinhTrang = ?, TienCuoiCa = ?, TienBanHang = ?, TienChenhLech = ? WHERE MaCa = ?";

        try (
            java.sql.Connection con = ConnectDB.getInstance().getConnection();
            java.sql.PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(thoiGianCheckOut));
            pstmt.setString(2, trangThaiThucTe); 
            pstmt.setBigDecimal(3, tienCuoiCa);
            pstmt.setBigDecimal(4, tienBanHang);
            pstmt.setBigDecimal(5, tienChenhLech);
            pstmt.setString(6, maCa);
            
            return pstmt.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    // ==============================
    // 7. TÍNH TỔNG GIỜ LÀM TRONG THÁNG (Trả về double)
    // ==============================
    public double tinhTongGioLamTrongThang(String maNV, int thang, int nam) {
        double tongGioLam = 0;
        String sql = "SELECT SUM(DATEDIFF(MINUTE, ThoiGianCheckIn, ThoiGianCheckOut)) AS TongPhut " +
                     "FROM ChiaCa " +
                     "WHERE MaNV = ? AND MONTH(NgayLam) = ? AND YEAR(NgayLam) = ? AND TinhTrang = N'Đã hoàn thành'";
                     
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maNV);
            pstmt.setInt(2, thang);
            pstmt.setInt(3, nam);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    tongGioLam = rs.getInt("TongPhut") / 60.0; 
                }
            }
        } catch (SQLException e) {
            logError("tinhTongGioLamTrongThang", e);
        }
        return tongGioLam;
    }
    // ==============================
    // 8. TÍNH TỔNG TIỀN THIẾU KÉT (Phạt nhân viên)
    // ==============================
    public BigDecimal tinhTongTienThieuKet(String maNV, int thang, int nam) {
        BigDecimal tongTienPhat = BigDecimal.ZERO;
        String sql = "SELECT SUM(TienChenhLech) AS TongPhat " +
                     "FROM ChiaCa " +
                     "WHERE MaNV = ? AND MONTH(NgayLam) = ? AND YEAR(NgayLam) = ? " +
                     "AND TinhTrang = N'Đã hoàn thành' AND TienChenhLech < 0";
                     
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maNV);
            pstmt.setInt(2, thang);
            pstmt.setInt(3, nam);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal tongPhatRaw = rs.getBigDecimal("TongPhat");
                    if (tongPhatRaw != null) {
                        tongTienPhat = tongPhatRaw.abs();
                    }
                }
            }
        } catch (SQLException e) {
            logError("tinhTongTienThieuKet", e);
        }
        return tongTienPhat;
    }
    public ChiaCa layCaDangLamViec(String maNV, LocalDate ngayLam) {
        String sql = "SELECT * FROM ChiaCa WHERE MaNV = ? AND NgayLam = ? AND TinhTrang = N'Đang làm việc'";
        try (Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, maNV);
            pst.setDate(2, java.sql.Date.valueOf(ngayLam));
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                ChiaCa cc = new ChiaCa();
                cc.setMaCa(rs.getString("MaCa"));
                cc.setMaLoaiCa(rs.getString("MaLoaiCa"));
                cc.setMaNV(rs.getString("MaNV"));
                cc.setTinhTrang(rs.getString("TinhTrang"));
                
                java.sql.Date ngay = rs.getDate("NgayLam");
                if (ngay != null) cc.setNgayLam(ngay.toLocalDate());
                
                java.sql.Timestamp tsIn = rs.getTimestamp("ThoiGianCheckIn");
                if (tsIn != null) cc.setThoiGianCheckIn(tsIn.toLocalDateTime());
                
                java.sql.Timestamp tsOut = rs.getTimestamp("ThoiGianCheckOut");
                if (tsOut != null) cc.setThoiGianCheckOut(tsOut.toLocalDateTime());
                
                cc.setTienDauCa(rs.getBigDecimal("TienDauCa"));
                cc.setTienCuoiCa(rs.getBigDecimal("TienCuoiCa"));
                cc.setTienBanHang(rs.getBigDecimal("TienBanHang"));
                cc.setTienChenhLech(rs.getBigDecimal("TienChenhLech"));
                
                return cc;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // ==============================
    // LOG LỖI CHUYÊN NGHIỆP
    // ==============================
    private void logError(String method, Exception e) {
        System.err.println("[ChiaCaDAO - " + method + "] ERROR: " + e.getMessage());
    }
}