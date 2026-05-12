package Dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import Data.TaiKhoan;

public class TaiKhoanDAO {
    
    private static final TaiKhoanDAO instance = new TaiKhoanDAO();

    private TaiKhoanDAO() {}

    public static TaiKhoanDAO getInstance() {
        return instance;
    }

    // ==========================================================
    // HELPER: MAP DỮ LIỆU TỪ SQL SANG ĐỐI TƯỢNG JAVA
    // ==========================================================
    private TaiKhoan mapResultSetToTaiKhoan(ResultSet rs) throws SQLException {
        String maNV = rs.getString("MaNV");
        String taiKhoan = rs.getString("TaiKhoan");
        String matKhau = rs.getString("MatKhau");

        return new TaiKhoan.ThoXayTaiKhoan()
                .ganMaNV(maNV)
                .ganTaiKhoan(taiKhoan)
                .ganMatKhau(matKhau)
                .taoMoi();
    }

    // ==========================================================
    // 1. KIỂM TRA ĐĂNG NHẬP (ÉP PHÂN BIỆT HOA THƯỜNG 100%) 🚀
    // ==========================================================
    public String[] kiemTraDangNhap(String tenDangNhapHoacSDT, String matKhau) {
        // 🔥 ĐÃ CHỈNH SỬA: Lấy thêm Tài Khoản và Mật Khẩu từ DB lên để Java tự kiểm tra
        String sql = "SELECT NV.MaNV, NV.HoTen, TK.TaiKhoan, TK.MatKhau " +
                     "FROM TaiKhoan TK " +
                     "JOIN NhanVien NV ON TK.MaNV = NV.MaNV " +
                     "WHERE (TK.TaiKhoan = ? OR NV.SDT = ?) " +
                     "AND NV.TrangThai = N'Đang Làm Việc'";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tenDangNhapHoacSDT);
            ps.setString(2, tenDangNhapHoacSDT);
            
            // Dùng vòng lặp while để vét hết trường hợp SQL trả về nhiều dòng (ví dụ nv002, NV002)
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dbTaiKhoan = rs.getString("TaiKhoan");
                    String dbMatKhau = rs.getString("MatKhau");

                    // BƯỚC 1: KIỂM TRA MẬT KHẨU (Bắt buộc khớp chính xác hoa thường)
                    if (dbMatKhau.equals(matKhau)) {
                        
                        // BƯỚC 2: KIỂM TRA TÀI KHOẢN
                        // - Nếu nhập SĐT (toàn số) -> Bỏ qua soi hoa/thường, đăng nhập luôn
                        // - Nếu nhập Tên TK -> Phải khớp chính xác từng ký tự
                        if (tenDangNhapHoacSDT.matches("\\d+") || dbTaiKhoan.equals(tenDangNhapHoacSDT)) {
                            return new String[]{ rs.getString("MaNV"), rs.getString("HoTen") };
                        }
                    }
                }
            }

        } catch (SQLException e) {
            logError("kiemTraDangNhap", e);
        }
        // Nếu chạy hết vòng lặp mà không return được -> Sai tên TK hoặc Mật khẩu hoa/thường
        return null; 
    }

    // ==========================================================
    // 2. TÌM TÀI KHOẢN THEO MÃ NHÂN VIÊN
    // ==========================================================
    public TaiKhoan layTaiKhoanTheoMaNV(String maNV) {
        String sql = "SELECT * FROM TaiKhoan WHERE MaNV=?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maNV);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTaiKhoan(rs);
                }
            }
        } catch (SQLException e) {
            logError("layTaiKhoanTheoMaNV", e);
        }
        return null;
    }

    // ==========================================================
    // 3. KIỂM TRA TÊN ĐĂNG NHẬP ĐÃ TỒN TẠI CHƯA (Dùng khi Thêm mới)
    // ==========================================================
    public boolean kiemTraTaiKhoanTonTai(String tenTaiKhoan) {
        String sql = "SELECT 1 FROM TaiKhoan WHERE TaiKhoan=?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tenTaiKhoan);
            
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logError("kiemTraTaiKhoanTonTai", e);
        }
        return false;
    }

    // ==========================================================
    // 4. THÊM MỚI TÀI KHOẢN
    // ==========================================================
    public boolean themTaiKhoan(TaiKhoan tk) {
        String sql = "INSERT INTO TaiKhoan(MaNV, TaiKhoan, MatKhau) VALUES(?, ?, ?)";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tk.getMaNV());
            ps.setString(2, tk.getTaiKhoan());
            ps.setString(3, tk.getMatKhau());
            
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("themTaiKhoan", e);
        }
        return false;
    }

    // ==========================================================
    // 5. CẬP NHẬT MẬT KHẨU HOẶC TÊN TÀI KHOẢN
    // ==========================================================
    public boolean suaTaiKhoan(TaiKhoan tk) {
        String sql = "UPDATE TaiKhoan SET TaiKhoan=?, MatKhau=? WHERE MaNV=?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tk.getTaiKhoan());
            ps.setString(2, tk.getMatKhau());
            ps.setString(3, tk.getMaNV());
            
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("suaTaiKhoan", e);
        }
        return false;
    }

    // ==========================================================
    // 6. XÓA TÀI KHOẢN (Khi nhân viên nghỉ việc)
    // ==========================================================
    public boolean xoaTaiKhoan(String taiKhoan) {
        String sql = "DELETE FROM TaiKhoan WHERE TaiKhoan=?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, taiKhoan);
            
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("xoaTaiKhoan", e);
        }
        return false;
    }
    
    // ==========================================================
    // 7. LẤY DANH SÁCH TÀI KHOẢN 
    // ==========================================================
    public List<TaiKhoan> layDanhSachTaiKhoan() {
        List<TaiKhoan> list = new ArrayList<>();
        String sql = "SELECT * FROM TaiKhoan";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql); 
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                list.add(mapResultSetToTaiKhoan(rs));
            }
        } catch (SQLException e) { 
            logError("layDanhSachTaiKhoan", e);
        }
        return list;
    }

    // ==========================================================
    // LOG LỖI RA CONSOLE ĐỂ DỄ FIX
    // ==========================================================
    private void logError(String method, Exception e) {
        System.err.println("[TaiKhoanDAO - " + method + "] ERROR: " + e.getMessage());
    }
}