package GUI.HoTro;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class LoadingUtil {

    private static JDialog dialog;
    private static JProgressBar progressBar;
    private static JLabel lblMessage;

    /**
     * Hiển thị màn hình Loading Pixel Art
     * @param parent Component cha để căn giữa
     * @param message Thông báo hiển thị (ví dụ: "Đang tải dữ liệu...")
     */
    public static void showLoading(Component parent, String message) {
        if (dialog != null && dialog.isShowing()) return;

        Window owner = SwingUtilities.getWindowAncestor(parent);
        dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0)); // Trong suốt nền dialog

        // Panel chính thiết kế theo style tối giản (White & Black)
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Vẽ nền trắng bo góc nhẹ
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Vẽ viền đen mảnh cho sang trọng
                g2.setColor(new Color(230, 230, 230));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        // 1. Label chữ "LOADING" Pixel Style
        lblMessage = new JLabel(message.toUpperCase());
        lblMessage.setFont(new Font("Monospaced", Font.BOLD, 18));
        lblMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblMessage.setForeground(new Color(40, 40, 40));

        // 2. Custom ProgressBar kiểu Pixel
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(300, 30));
        progressBar.setMaximumSize(new Dimension(300, 30));
        progressBar.setStringPainted(false);
        progressBar.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        progressBar.setBackground(Color.WHITE);
        progressBar.setForeground(Color.BLACK); // Màu các khối vuông loading

        // Tùy chỉnh giao diện ProgressBar thành các khối vuông (Pixel Art)
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintIndeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = c.getWidth();
                int h = c.getHeight();
                
                // Tạo hiệu ứng các khối chạy qua lại
                long time = System.currentTimeMillis() / 100;
                int blockWidth = 20;
                int gap = 5;
                
                g2.setColor(Color.BLACK);
                for (int i = 0; i < 15; i++) {
                    int x = (int) ((time + i) % 15) * (blockWidth + gap);
                    if (x < w) {
                        g2.fillRect(x, 4, blockWidth, h - 8);
                    }
                }
                g2.dispose();
            }
        });

        panel.add(lblMessage);
        panel.add(Box.createVerticalStrut(20));
        panel.add(progressBar);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        
        // Chạy trong thread riêng để không block EDT
        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }

    /**
     * Đóng màn hình Loading
     */
    public static void hideLoading() {
        if (dialog != null) {
            dialog.dispose();
            dialog = null;
        }
    }

    /**
     * Hàm thực thi một tác vụ nặng có màn hình chờ
     * @param parent Component gọi
     * @param task Tác vụ cần thực hiện (Runnable)
     */
    public static void executeWithLoading(Component parent, Runnable task, String msg) {
        showLoading(parent, msg);
        
        // Sử dụng SwingWorker để xử lý ngầm (Tránh chết màn hình)
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run(); // Chạy tác vụ của bạn ở đây
                return null;
            }

            @Override
            protected void done() {
                hideLoading(); // Xong việc thì ẩn loading đi
            }
        };
        worker.execute();
    }
}