package GUI.HoTro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class TienIchGiaoDien {

    // ================== THEME ==================
    private static final Font BASE_FONT =
            UIManager.getFont("Label.font") != null
                    ? UIManager.getFont("Label.font")
                    : new Font("Segoe UI", Font.PLAIN, 14);

    public static final Font FONT_CHINH = BASE_FONT.deriveFont(Font.PLAIN, 14f);
    public static final Font FONT_DAM = BASE_FONT.deriveFont(Font.BOLD, 14f);

    public static final Color MAU_CHINH = new Color(37, 99, 235);
    public static final Color MAU_CHU_CHINH = new Color(15, 23, 42);
    public static final Color MAU_CHU_PHU = new Color(100, 116, 139);
    public static final Color MAU_VIEN = new Color(226, 232, 240);

    // ================== BUTTON ==================
    public static JButton taoNutHienDai(String text, Color bgColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color color = !isEnabled()
                        ? Color.LIGHT_GRAY
                        : (getModel().isPressed() ? bgColor.darker() : bgColor);

                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                
                g2.dispose();
            }
        };

        btn.setFont(FONT_DAM);
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return btn;
    }

    // ================== COMBOBOX ==================
    public static void trangTriComboBox(JComboBox<String> cb) {
        cb.setFont(FONT_CHINH);
        cb.setBackground(Color.WHITE);
        cb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1, true),
                new EmptyBorder(5, 10, 5, 5)
        ));

        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                // 🔥 ĐÃ SỬA TẠI ĐÂY: Tự vẽ tam giác Vector sắc nét
                JButton btn = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        // Đổi màu thành xanh khi trỏ chuột vào cho xịn
                        if (getModel().isRollover()) {
                            g2.setColor(MAU_CHINH);
                        } else {
                            g2.setColor(MAU_CHU_PHU);
                        }
                        
                        // Căn chỉnh tọa độ vẽ tam giác
                        int w = getWidth();
                        int h = getHeight();
                        int tW = 10; // Chiều rộng tam giác
                        int tH = 6;  // Chiều cao tam giác
                        int x = (w - tW) / 2;
                        int y = (h - tH) / 2;

                        int[] xPoints = {x, x + tW, x + tW / 2};
                        int[] yPoints = {y, y, y + tH};
                        
                        g2.fillPolygon(xPoints, yPoints, 3);
                        g2.dispose();
                    }
                };
                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.setPreferredSize(new Dimension(30, 0)); // Tạo chút không gian rộng rãi cho nút
                return btn;
            }
        });

        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(10, 15, 10, 15));
                if (isSelected) {
                    l.setBackground(MAU_CHINH);
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(Color.WHITE);
                    l.setForeground(MAU_CHU_CHINH);
                }
                return l;
            }
        });
    }

    // ================== DIALOG (THÔNG BÁO) ==================
    public static void hienThiThongBao(Component parent, String message, String type) {
        Window win = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(win, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                new EmptyBorder(25, 30, 20, 30)
        ));
        panel.setBackground(Color.WHITE);

        Color color = switch (type != null ? type.toUpperCase() : "") {
            case "ERROR" -> new Color(220, 53, 69);
            case "WARNING" -> new Color(243, 156, 18);
            case "SUCCESS" -> new Color(40, 167, 69);
            default -> MAU_CHINH;
        };

        JLabel lblTitle = new JLabel(type.toUpperCase(), SwingConstants.CENTER);
        lblTitle.setFont(FONT_DAM.deriveFont(18f));
        lblTitle.setForeground(color);

        JLabel lblMsg = new JLabel("<html><center>" + message + "</center></html>", SwingConstants.CENTER);
        lblMsg.setFont(FONT_CHINH);
        lblMsg.setForeground(MAU_CHU_CHINH);

        JButton btn = taoNutHienDai("Đóng", color);
        btn.addActionListener(e -> dialog.dispose());
        
        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlBtn.setOpaque(false);
        pnlBtn.add(btn);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(lblMsg, BorderLayout.CENTER);
        panel.add(pnlBtn, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();
        if (dialog.getWidth() < 300) dialog.setSize(300, dialog.getHeight());
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    // ================== DIALOG (XÁC NHẬN) ==================
    public static void hienThiXacNhan(Component parent, String message, Runnable actionConfirm) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                new EmptyBorder(30, 40, 25, 40)
        ));
        panel.setBackground(Color.WHITE);

        JLabel lblMsg = new JLabel("<html><center>" + message + "</center></html>", SwingConstants.CENTER);
        lblMsg.setFont(FONT_CHINH.deriveFont(15f));
        
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        pnlButtons.setOpaque(false);

        JButton btnYes = taoNutHienDai("Xác nhận", MAU_CHINH);
        btnYes.addActionListener(e -> {
            dialog.dispose();
            actionConfirm.run();
        });

        JButton btnNo = new JButton("Hủy bỏ");
        btnNo.setFont(FONT_DAM);
        btnNo.setForeground(MAU_CHU_PHU);
        btnNo.setContentAreaFilled(false);
        btnNo.setBorderPainted(false);
        btnNo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNo.addActionListener(e -> dialog.dispose());

        pnlButtons.add(btnNo);
        pnlButtons.add(btnYes);

        panel.add(lblMsg, BorderLayout.CENTER);
        panel.add(pnlButtons, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    // ================== SCROLL ==================
    public static void thietLapThanhCuon(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(200, 200, 200));
                g2.fillRoundRect(r.x + 2, r.y, r.width - 4, r.height, 10, 10);
                g2.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            @Override protected JButton createDecreaseButton(int o) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0,0));
                return b;
            }
        });
    }

    // ================== BLOCK THÔNG TIN ==================
    public static JPanel taoBlockThongTin(String title, JLabel value) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_CHINH.deriveFont(12f));
        lblTitle.setForeground(MAU_CHU_PHU);

        value.setFont(FONT_DAM.deriveFont(15f));
        value.setForeground(MAU_CHU_CHINH);

        p.add(lblTitle);
        p.add(value);
        return p;
    }

    // ================== CUSTOM COMPONENT: NÚT GẠT (TOGGLE) ==================
    public static class NutGat extends JCheckBox {
        public NutGat() {
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = 44;
            int height = 24;
            int x = 0;
            int y = (getHeight() - height) / 2;

            if (isSelected()) {
                g2.setColor(new Color(90, 215, 210)); 
                g2.fillRoundRect(x, y, width, height, height, height);
                g2.setColor(Color.WHITE);
                g2.fillOval(x + width - height + 2, y + 2, height - 4, height - 4);
            } else {
                g2.setColor(new Color(200, 205, 210)); 
                g2.fillRoundRect(x, y, width, height, height, height);
                g2.setColor(Color.WHITE);
                g2.fillOval(x + 2, y + 2, height - 4, height - 4);
            }
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(50, 30);
        }
    }
}