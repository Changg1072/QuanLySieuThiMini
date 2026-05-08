package GUI.HoTro;

import Logic.TaiKhoanLogic;
import java.util.Arrays;

public class AuthService {

    private static final TaiKhoanLogic taiKhoanLogic = new TaiKhoanLogic();
    private static String[] userSession = null; // [MaNV, ChucVu]

    public static String[] login(String username, char[] password) {
        try {
            String passStr = new String(password);
            userSession = taiKhoanLogic.xuLyDangNhap(username, passStr);
            return userSession;
        } catch (Exception e) {
            System.err.println("Lỗi đăng nhập: " + e.getMessage());
            return null;
        } finally {
            Arrays.fill(password, '\0'); // 🔥 clear memory
        }
    }

    public static String[] getCurrentUser() {
        return userSession;
    }

    public static void logout() {
        userSession = null;
    }
}