package Logic;

import java.util.List;
import java.security.MessageDigest;

import Dao.TaiKhoanDAO;
import Data.TaiKhoan;

public class TaiKhoanLogic {
    private TaiKhoanDAO dao = TaiKhoanDAO.getInstance();
    
    // ==========================================================
    // HÀM DÙNG CHUNG: MÃ HÓA MẬT KHẨU (SHA-256)
    // ==========================================================
    private String maHoaSHA256(String matKhauGoc) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(matKhauGoc.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            System.err.println("Lỗi băm mật khẩu: " + ex.getMessage());
            return null;
        }
    }

    // ==========================================================
    // 1. ĐĂNG NHẬP
    // ==========================================================
    public String[] xuLyDangNhap(String taiKhoan, String matKhau) throws Exception {
        if (taiKhoan == null || taiKhoan.trim().isEmpty()) {
            throw new Exception("Vui lòng nhập tên Tài khoản hoặc Số điện thoại!");
        }
        if (matKhau == null || matKhau.trim().isEmpty()) {
            throw new Exception("Vui lòng nhập Mật khẩu!");
        }
        
        // Băm mật khẩu người dùng vừa nhập
        String matKhauDaBam = maHoaSHA256(matKhau);
        
        // Gửi mật khẩu đã băm xuống DB để kiểm tra
        String[] ketQua = dao.kiemTraDangNhap(taiKhoan, matKhauDaBam);
        
        if (ketQua == null) {
            throw new Exception("Tài khoản hoặc mật khẩu không chính xác!");
        }
        return ketQua;
    }
    
    // ==========================================================
    // 2. ĐỔI MẬT KHẨU
    // ==========================================================
    public void xuLyDoiMatKhau(String taiKhoan, String matKhauCu, String matKhauMoi, String xacNhanMatKhau) throws Exception {
        if (matKhauCu == null || matKhauMoi == null || xacNhanMatKhau == null || 
            matKhauCu.trim().isEmpty() || matKhauMoi.trim().isEmpty() || xacNhanMatKhau.trim().isEmpty()) {
            throw new Exception("Vui lòng nhập đầy đủ thông tin (Mật khẩu cũ, Mật khẩu mới và Xác nhận)!");
        }
        
        // Kiểm tra mật khẩu cũ (Phải băm ra mới so sánh được)
        String matKhauCuDaBam = maHoaSHA256(matKhauCu);
        if (dao.kiemTraDangNhap(taiKhoan, matKhauCuDaBam) == null) {
            throw new Exception("Mật khẩu cũ không chính xác!");
        }
        
        if (matKhauCu.equals(matKhauMoi)) {
            throw new Exception("Mật khẩu mới phải khác với mật khẩu hiện tại!");
        }
        
        if (matKhauMoi.length() < 6) {
            throw new Exception("Mật khẩu mới phải có ít nhất 6 ký tự để đảm bảo an toàn!");
        }
        
        if (!matKhauMoi.equals(xacNhanMatKhau)) {
            throw new Exception("Mật khẩu mới và phần xác nhận mật khẩu không trùng khớp!");
        }
        
        // Băm mật khẩu mới trước khi lưu
        String matKhauMoiDaBam = maHoaSHA256(matKhauMoi);
        boolean thanhCong = dao.doiMatKhau(taiKhoan, matKhauMoiDaBam);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể đổi mật khẩu lúc này!");
        }
    }

    // ==========================================================
    // 3. LẤY DANH SÁCH TÀI KHOẢN    
    // ==========================================================
    public List<TaiKhoan> layDanhSachTaiKhoan() {
        return dao.layDanhSachTaiKhoan();
    }
    
    // ==========================================================
    // 4. THÊM TÀI KHOẢN
    // ==========================================================
    public void themTaiKhoan(TaiKhoan tk) throws Exception {
        if (tk.getMaNV() == null || tk.getMaNV().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Mã nhân viên để cấp tài khoản!");
        }
        if (dao.kiemTraNhanVienDaCoTaiKhoan(tk.getMaNV())) {
            throw new Exception("Nhân viên có mã '" + tk.getMaNV() + "' đã được cấp tài khoản rồi! Không thể tạo thêm.");
        }
        if (tk.getTaiKhoan() == null || tk.getTaiKhoan().trim().isEmpty()) {
            throw new Exception("Tên tài khoản không được để trống!");
        }
        if (tk.getMatKhau() == null || tk.getMatKhau().length() < 6) {
            throw new Exception("Mật khẩu phải có ít nhất 6 ký tự");
        }
        if (dao.kiemTraTaiKhoanDaTonTai(tk.getTaiKhoan())) {
            throw new Exception("Tên tài khoản '" + tk.getTaiKhoan() + "' đã tồn tại! Vui lòng chọn tên khác.");
        }

        // Mã hóa mật khẩu thành chuỗi 64 ký tự trước khi lưu
        String matKhauDaBam = maHoaSHA256(tk.getMatKhau());
        tk.setMatKhau(matKhauDaBam);

        boolean thanhCong = dao.themTaiKhoan(tk);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể thêm tài khoản!");
        }
    }

    // ==========================================================
    // 5. XÓA TÀI KHOẢN
    // ==========================================================
    public void xoaTaiKhoan(String taiKhoan) throws Exception {
        if (taiKhoan == null || taiKhoan.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn tài khoản cần xóa!");
        }
        
        if (taiKhoan.equalsIgnoreCase("admin")) {
            throw new Exception("Không được phép xóa tài khoản admin!");
        }

        boolean thanhCong = dao.xoaTaiKhoan(taiKhoan);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể xóa tài khoản!");
        }
    }

    // ==========================================================
    // 6. RESET MẬT KHẨU (Dành cho Quản lý / Admin)
    // ==========================================================
    public void resetMatKhau(String taiKhoan) throws Exception {
        if (taiKhoan == null || taiKhoan.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn tài khoản cần đặt lại mật khẩu!");
        }
        
        // Mật khẩu mặc định khi reset là "123456"
        String matKhauMacDinh = "123456";
        String matKhauDaBam = maHoaSHA256(matKhauMacDinh);
        
        // Tái sử dụng hàm đổi mật khẩu ở DAO
        boolean thanhCong = dao.doiMatKhau(taiKhoan, matKhauDaBam);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể đặt lại mật khẩu lúc này!");
        }
    }
}