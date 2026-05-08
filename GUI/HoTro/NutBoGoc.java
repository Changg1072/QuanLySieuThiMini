package GUI.HoTro;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class NutBoGoc extends JButton {

    private int arc = 15;
    private Color colorBackground = new Color(30, 110, 160);
    private Shape shape;

    public NutBoGoc(String text) {
        super(text);

        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);

        setForeground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);

        Font baseFont = UIManager.getFont("Label.font");
        setFont(baseFont != null
                ? baseFont.deriveFont(Font.BOLD, 14f)
                : new Font("SansSerif", Font.BOLD, 14));

        setMargin(new Insets(6, 16, 6, 16));
    }

    public void setArc(int arc) {
        this.arc = arc;
        this.shape = null;
        repaint();
    }

    public int getArc() {
        return arc;
    }

    public void setColorBackground(Color color) {
        if (color != null) {
            this.colorBackground = color;
            repaint();
        }
    }

    public Color getColorBackground() {
        return colorBackground;
    }

    private Color adjustColor(Color c, float factor) {
        int r = Math.min(255, Math.max(0, (int) (c.getRed() * factor)));
        int g = Math.min(255, Math.max(0, (int) (c.getGreen() * factor)));
        int b = Math.min(255, Math.max(0, (int) (c.getBlue() * factor)));
        return new Color(r, g, b);
    }

    @Override
    public boolean contains(int x, int y) {
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc);
        }
        return shape.contains(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        ButtonModel model = getModel();
        Color bg = colorBackground;

        if (!isEnabled()) {
            bg = adjustColor(bg, 0.7f);
        } else if (model.isPressed()) {
            bg = adjustColor(bg, 0.85f);
        } else if (model.isRollover()) {
            bg = adjustColor(bg, 1.15f);
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        g2.setColor(new Color(0, 0, 0, 40));
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        g2.dispose();

        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        if (hasFocus()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(new Color(255, 255, 255, 150));
            g2.setStroke(new BasicStroke(2f));
            
            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, arc, arc);
            g2.dispose();
        }
    }
}