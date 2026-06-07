package main;

import GUI.DangNhapUi;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        // 1. Cải thiện font chữ và render đồ họa mượt mà hơn (Anti-aliasing)
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // 2. Thiết lập Look and Feel giống với hệ điều hành (Windows/Mac)
        try {
            // Cố gắng sử dụng giao diện hệ thống mặc định
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Không thể thiết lập LookAndFeel hệ thống. Đang dùng giao diện mặc định của Java.");
            e.printStackTrace();
        }

        // 3. Khởi chạy màn hình Đăng Nhập trong Event Dispatch Thread (EDT) của Swing
        // Điều này đảm bảo an toàn luồng (thread-safe) cho giao diện UI
        SwingUtilities.invokeLater(() -> {
            try {
                DangNhapUi dangNhap = new DangNhapUi();
                dangNhap.setVisible(true);
            } catch (Exception e) {
                System.err.println("Lỗi khi khởi chạy màn hình Đăng Nhập!");
                e.printStackTrace();
            }
        });
    }
}
