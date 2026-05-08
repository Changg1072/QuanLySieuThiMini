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
    // 1. KIỂM TRA ĐĂNG NHẬP (Hỗ trợ cả Tên TK hoặc SĐT)
    // ==========================================================
    public String[] kiemTraDangNhap(String tenDangNhapHoacSDT, String matKhau) {
        // JOIN với bảng NhanVien để cho phép đăng nhập bằng SĐT
        String sql = "SELECT t.MaNV, n.ChucVu FROM TaiKhoan t " 
                   + "JOIN NhanVien n ON t.MaNV = n.MaNV "
                   + "WHERE (t.TaiKhoan=? OR n.SDT=?) AND t.MatKhau=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tenDangNhapHoacSDT);
            ps.setString(2, tenDangNhapHoacSDT);
            ps.setString(3, matKhau); // Mật khẩu truyền xuống đã được băm từ Logic

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[] {
                        rs.getString("MaNV"), 
                        rs.getString("ChucVu").trim()
                    };
                }
            }
        } catch (SQLException e) {
            logError("kiemTraDangNhap", e);
        }
        return null;
    }

    // ==========================================================
    // 2. KIỂM TRA TÀI KHOẢN ĐÃ TỒN TẠI (Giữ nguyên check SDT)
    // ==========================================================
    public boolean kiemTraTaiKhoanDaTonTai(String thongTinKiemTra) {
        // Kiểm tra xem Tên tài khoản HOẶC Số điện thoại này đã được dùng cho ních nào chưa
        String sql = "SELECT COUNT(*) FROM TaiKhoan t " 
                   + "JOIN NhanVien n ON t.MaNV = n.MaNV "
                   + "WHERE t.TaiKhoan=? OR n.SDT=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, thongTinKiemTra);
            ps.setString(2, thongTinKiemTra);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logError("kiemTraTaiKhoanDaTonTai", e);
        }
        return false;
    }

    // ==========================================================
    // 3. KIỂM TRA 1 NHÂN VIÊN ĐÃ CÓ TÀI KHOẢN CHƯA
    // ==========================================================
    public boolean kiemTraNhanVienDaCoTaiKhoan(String maNV) {
        String sql = "SELECT COUNT(*) FROM TaiKhoan WHERE MaNV=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maNV);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logError("kiemTraNhanVienDaCoTaiKhoan", e);
        }
        return false;
    }

    // ==========================================================
    // 4. THÊM TÀI KHOẢN
    // ==========================================================
    public boolean themTaiKhoan(TaiKhoan tk) {
        String sql = "INSERT INTO TaiKhoan (MaNV, TaiKhoan, MatKhau) VALUES (?, ?, ?)";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tk.getMaNV());
            ps.setString(2, tk.getTaiKhoan()); 
            ps.setString(3, tk.getMatKhau()); // Mật khẩu đã băm
            
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("themTaiKhoan", e);
        }
        return false;
    }

    // ==========================================================
    // 5. ĐỔI MẬT KHẨU
    // ==========================================================
    public boolean doiMatKhau(String taiKhoan, String matKhauMoi) {
        String sql = "UPDATE TaiKhoan SET MatKhau=? WHERE TaiKhoan=?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, matKhauMoi);
            ps.setString(2, taiKhoan);
            
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("doiMatKhau", e);
        }
        return false;
    }

    // ==========================================================
    // 6. XÓA TÀI KHOẢN
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