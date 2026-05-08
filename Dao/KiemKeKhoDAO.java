package Dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import Data.KiemKeKho;

public class KiemKeKhoDAO {

    private static final KiemKeKhoDAO instance = new KiemKeKhoDAO();

    private KiemKeKhoDAO() {
    }

    public static KiemKeKhoDAO getInstance() {
        return instance;
    }

    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS) 
    // =========================================================
    private KiemKeKho mapResultSetToKiemKeKho(ResultSet rs) throws SQLException {
        Date sqlNgay = rs.getDate("NgayKiemKe");
        LocalDate ngayKiemKe = (sqlNgay != null) ? sqlNgay.toLocalDate() : null;

        return new KiemKeKho.ThoXayKiemKeKho()
                .ganMaKiemKe(rs.getString("MaKiemKe"))
                .ganNgayKiemKe(ngayKiemKe)
                .ganMaNV(rs.getString("MaNV"))
                .ganMaLoHang(rs.getString("MaLoHang"))
                .ganMaSP(rs.getString("MaSP"))
                .ganSoLuongHeThong(rs.getInt("SoLuongHeThong"))
                .ganSoLuongThucTe(rs.getInt("SoLuongThucTe"))
                .ganLyDo(rs.getString("LyDo"))
                .ganTrangThai(rs.getString("TrangThai"))
                .taoMoi();
    }

    // ==============================
    // 1. LẤY DANH SÁCH PHIẾU KIỂM KÊ
    // ==============================
    public List<KiemKeKho> layDanhSachKiemKe() {
        List<KiemKeKho> dsKiemKe = new ArrayList<>();
        String sql = "SELECT * FROM KiemKeKho ORDER BY NgayKiemKe DESC";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql); 
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsKiemKe.add(mapResultSetToKiemKeKho(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachKiemKe", e);
        }
        return dsKiemKe;
    }

    // ==============================
    // 2. LẤY PHIẾU THEO MÃ
    // ==============================
    public KiemKeKho layKiemKeTheoMa(String maKiemKe) {
        String sql = "SELECT * FROM KiemKeKho WHERE MaKiemKe = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maKiemKe);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToKiemKeKho(rs);
                }
            }
        } catch (SQLException e) {
            logError("layKiemKeTheoMa", e);
        }
        return null;
    }

    // ==============================
    // 3. LẬP PHIẾU KIỂM KÊ
    // ==============================
    public boolean lapPhieuKiemKe(KiemKeKho kk) {
        String sql = "INSERT INTO KiemKeKho(MaKiemKe, NgayKiemKe, MaNV, MaLoHang, MaSP, SoLuongHeThong, SoLuongThucTe, LyDo, TrangThai) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, kk.getMaKiemKe());
            
            if (kk.getNgayKiemKe() != null) {
                pstmt.setDate(2, Date.valueOf(kk.getNgayKiemKe()));
            } else {
                pstmt.setDate(2, Date.valueOf(LocalDate.now())); 
            }

            pstmt.setString(3, kk.getMaNV());
            pstmt.setString(4, kk.getMaLoHang());
            pstmt.setString(5, kk.getMaSP());
            pstmt.setInt(6, kk.getSoLuongHeThong());
            pstmt.setInt(7, kk.getSoLuongThucTe());
            pstmt.setString(8, kk.getLyDo());
            pstmt.setString(9, "Chờ Duyệt"); 

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("lapPhieuKiemKe", e);
        }
        return false;
    }

    // ==============================
    // 4. DUYỆT VÀ CÂN BẰNG KHO (Sử dụng Transaction an toàn)
    // ==============================
    public boolean duyetVaCanBangKho(KiemKeKho kk) {
        String sqlUpdateKiemKe = "UPDATE KiemKeKho SET TrangThai = ? WHERE MaKiemKe = ?";
        String sqlUpdateChiTietLoHang = "UPDATE ChiTietLoHang SET SoLuongTon = ? WHERE MaLoHang = ? AND MaSP = ?";

        // Dùng Try-with-resources để tự động quản lý Connection
        try (Connection con = ConnectDB.getInstance().getConnection()) {
            
            con.setAutoCommit(false); // Tắt auto-commit để gom nhóm lệnh
            
            try (
                PreparedStatement pst1 = con.prepareStatement(sqlUpdateKiemKe);
                PreparedStatement pst2 = con.prepareStatement(sqlUpdateChiTietLoHang)
            ) {
                // Lệnh 1: Cập nhật trạng thái phiếu
                pst1.setString(1, "Đã Cân Kho");
                pst1.setString(2, kk.getMaKiemKe());
                pst1.executeUpdate();

                // Lệnh 2: Điều chỉnh số lượng tồn thực tế vào lô hàng tương ứng
                pst2.setInt(1, kk.getSoLuongThucTe());
                pst2.setString(2, kk.getMaLoHang());
                pst2.setString(3, kk.getMaSP());
                pst2.executeUpdate();

                con.commit(); // Chốt giao dịch thành công
                return true;
                
            } catch (SQLException ex) {
                con.rollback(); // Lỗi ở lệnh 1 hay 2 thì đều quay xe ngay lập tức
                logError("duyetVaCanBangKho - Lỗi Transaction, đã Rollback", ex);
                return false;
            }
            
        } catch (SQLException e) {
            logError("duyetVaCanBangKho - Lỗi Connection", e);
            return false;
        }
        // Nhờ try-with-resources, connection sẽ tự động close() tại đây một cách an toàn!
    }

    // ==============================
    // 5. KIỂM TRA TRÙNG MÃ
    // ==============================
    public boolean kiemTraTrungMaKiemKe(String maKiemKe) {
        String sql = "SELECT COUNT(*) FROM KiemKeKho WHERE MaKiemKe = ?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maKiemKe);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logError("kiemTraTrungMaKiemKe", e);
        }
        return false;
    }

    private void logError(String method, Exception e) {
        System.err.println("[KiemKeKhoDAO - " + method + "] ERROR: " + e.getMessage());
    }
}