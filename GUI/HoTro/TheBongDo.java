package GUI.HoTro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class TheBongDo extends JPanel {

    private static final int SHADOW_SIZE = 6;
    private static final int PADDING = 8;
    private int radius;

    public TheBongDo(int radius) {
        this.radius = radius;
        setOpaque(false);
        setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        for (int i = 0; i < SHADOW_SIZE; i++) {
            float progress = (float) i / SHADOW_SIZE;
            float alpha = 0.02f + (progress * progress * 0.08f); 
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(i, i, w - i * 2, h - i * 2, radius, radius);
        }

        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(SHADOW_SIZE, SHADOW_SIZE, w - SHADOW_SIZE * 2, h - SHADOW_SIZE * 2, radius, radius);

        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(220, 220, 220));
        g2.drawRoundRect(SHADOW_SIZE, SHADOW_SIZE, getWidth() - SHADOW_SIZE * 2 - 1, getHeight() - SHADOW_SIZE * 2 - 1, radius, radius);

        g2.dispose();
    }

    // ===============================================
    // 🔥 TEXTFIELD ĐÃ FIX LỖI BỊ CẮT LẸM VIỀN
    // ===============================================
    public static class RoundedTextField extends JTextField {
        private String hint;
        private int radius = 25; 
        
        private Color hintColor = new Color(130, 135, 145); 
        private Color borderColor = new Color(160, 165, 175); 
        private Color focusColor = new Color(37, 99, 235);
        private Color hoverColor = new Color(130, 140, 150);
        private boolean isHover = false;

        public RoundedTextField(String hint, int columns) {
            super(columns);
            this.hint = hint;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setForeground(new Color(15, 23, 42)); 

            Font base = UIManager.getFont("TextField.font");
            setFont(base != null ? base.deriveFont(15f) : new Font("SansSerif", Font.PLAIN, 15));

            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { repaint(); }
                public void focusLost(FocusEvent e) { repaint(); }
            });

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { isHover = true; repaint(); }
                public void mouseExited(MouseEvent e) { isHover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Vẽ nền trắng (Lùi vào 1px để đảm bảo an toàn)
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(1, 1, w - 2, h - 2, radius, radius);

            // 2. Vẽ chữ
            super.paintComponent(g2); 

            // 3. Vẽ Placeholder (chữ mờ)
            if (getText().isEmpty() && hint != null && !isFocusOwner()) {
                g2.setColor(hintColor);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, 18, y);
            }

            // 4. Vẽ viền (Lùi vào 1px, giảm kích thước đi 3px để Stroke dày không bị xén mất)
            if (isFocusOwner()) {
                g2.setColor(focusColor);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(isHover ? hoverColor : borderColor);
                g2.setStroke(new BasicStroke(1.2f)); 
            }
            g2.drawRoundRect(1, 1, w - 3, h - 3, radius, radius);
            
            g2.dispose();
        }
    }

    // ===============================================
    // 🔥 PASSWORDFIELD ĐÃ FIX TƯƠNG TỰ
    // ===============================================
    public static class RoundedPasswordField extends JPasswordField {
        private String hint;
        private int radius = 25;
        private Color hintColor = new Color(130, 135, 145);
        private Color borderColor = new Color(160, 165, 175);
        private Color focusColor = new Color(37, 99, 235);
        private Color hoverColor = new Color(130, 140, 150);
        private boolean isHover = false;

        public RoundedPasswordField(String hint, int columns) {
            super(columns);
            this.hint = hint;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setEchoChar('•'); 
            setForeground(new Color(15, 23, 42));

            Font base = UIManager.getFont("TextField.font");
            setFont(base != null ? base.deriveFont(15f) : new Font("SansSerif", Font.PLAIN, 15));

            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { repaint(); }
                public void focusLost(FocusEvent e) { repaint(); }
            });

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { isHover = true; repaint(); }
                public void mouseExited(MouseEvent e) { isHover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(1, 1, w - 2, h - 2, radius, radius);

            super.paintComponent(g2); 

            if (getPassword().length == 0 && hint != null && !isFocusOwner()) {
                g2.setColor(hintColor);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, 18, y);
            }

            if (isFocusOwner()) {
                g2.setColor(focusColor);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(isHover ? hoverColor : borderColor);
                g2.setStroke(new BasicStroke(1.2f));
            }
            g2.drawRoundRect(1, 1, w - 3, h - 3, radius, radius);
            
            g2.dispose();
        }
    }
}