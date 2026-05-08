package GUI.HoTro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class MenuSidebarUtil {

    // ================== CONSTANT ==================
    private static final Font BASE_FONT =
            UIManager.getFont("Label.font") != null
                    ? UIManager.getFont("Label.font")
                    : new Font("SansSerif", Font.PLAIN, 13);

    private static final Font MENU_FONT = BASE_FONT.deriveFont(Font.PLAIN, 14f);
    private static final Font ACTIVE_FONT = BASE_FONT.deriveFont(Font.BOLD, 14f);

    // ================== CREATE BUTTON ==================
    public static JButton createMenuButton(String title,
                                           Icon icon,
                                           String cardName,
                                           Color sidebarColor,
                                           Color hoverColor,
                                           Color activeColor,
                                           int width) {

        JButton btn = new JButton(title != null ? title : "");
        btn.setPreferredSize(new Dimension(width, 40));
        btn.setMaximumSize(new Dimension(width, 40));

        // ===== ICON =====
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(12);
            btn.setBorder(new EmptyBorder(0, 20, 0, 0));
        } else {
            btn.setBorder(new EmptyBorder(0, 15, 0, 0));
        }

        // ===== STYLE =====
        btn.setFont(MENU_FONT);

        Color textColor = UIManager.getColor("Button.foreground");
        btn.setForeground(textColor != null ? textColor : Color.WHITE);

        btn.setBackground(sidebarColor);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setRolloverEnabled(true);

        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ===== STATE =====
        btn.putClientProperty("active", false);
        btn.putClientProperty("defaultColor", sidebarColor);
        btn.putClientProperty("hoverColor", hoverColor);
        btn.putClientProperty("activeColor", activeColor);
        btn.putClientProperty("cardName", cardName);

        // ===== MOUSE EFFECT =====
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!Boolean.TRUE.equals(btn.getClientProperty("active"))) {
                    btn.setBackground((Color) btn.getClientProperty("hoverColor"));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!Boolean.TRUE.equals(btn.getClientProperty("active"))) {
                    btn.setBackground((Color) btn.getClientProperty("defaultColor"));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Hiệu ứng click nhẹ làm tối nút đi
                if (!Boolean.TRUE.equals(btn.getClientProperty("active"))) {
                    btn.setBackground(((Color) btn.getClientProperty("hoverColor")).darker());
                } else {
                    btn.setBackground(((Color) btn.getClientProperty("activeColor")).darker());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Trả lại màu chuẩn sau khi nhả chuột
                if (Boolean.TRUE.equals(btn.getClientProperty("active"))) {
                    btn.setBackground((Color) btn.getClientProperty("activeColor"));
                } else {
                    // Nếu nhả chuột ra mà con trỏ vẫn nằm trong nút thì về màu hover, ngược lại về default
                    if (btn.contains(e.getPoint())) {
                        btn.setBackground((Color) btn.getClientProperty("hoverColor"));
                    } else {
                        btn.setBackground((Color) btn.getClientProperty("defaultColor"));
                    }
                }
            }
        });

        return btn;
    }

    // ================== OVERLOAD ==================
    public static JButton createMenuButton(String title,
                                           String cardName,
                                           Color sidebarColor,
                                           Color hoverColor,
                                           Color activeColor,
                                           int width) {
        return createMenuButton(title, null, cardName, sidebarColor, hoverColor, activeColor, width);
    }

    // ================== SET ACTIVE ==================
    public static void setActive(JButton btn, boolean active) {
        btn.putClientProperty("active", active);

        if (active) {
            btn.setBackground((Color) btn.getClientProperty("activeColor"));
            btn.setFont(ACTIVE_FONT);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground((Color) btn.getClientProperty("defaultColor"));
            btn.setFont(MENU_FONT);
            btn.setForeground(Color.WHITE);
        }
    }

    // ================== GROUP ==================
    public static void setActiveMenu(List<JButton> buttons, JButton selected) {
        for (JButton btn : buttons) {
            setActive(btn, btn == selected);
        }
    }
}