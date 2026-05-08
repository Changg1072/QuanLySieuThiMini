package GUI.HoTro;

import javax.swing.*;
import java.awt.*;

public class AvatarNhanVien extends JPanel {
    private static final long serialVersionUID = 1L;

    private String letter;
    private Color bgColor;

    public AvatarNhanVien(String name, Color bgColor) {
        this.letter = formatLetter(name);
        this.bgColor = (bgColor != null) ? bgColor : generateColor(this.letter);
        setOpaque(false);
    }

    // ================= SIZE =================
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(60, 60);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    // ================= PAINT =================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight());
        int xOffset = (getWidth() - size) / 2;
        int yOffset = (getHeight() - size) / 2;

        // 🎨 nền
        g2d.setColor(bgColor);
        g2d.fillOval(xOffset, yOffset, size, size);

        // 🧱 viền nhẹ
        g2d.setColor(new Color(0, 0, 0, 35));
        g2d.drawOval(xOffset, yOffset, size - 1, size - 1);

        // 🔠 chữ
        g2d.setColor(Color.WHITE);

        int fontSize = (int) (size * 0.42);
        Font baseFont = getFont() != null ? getFont() : new Font("SansSerif", Font.BOLD, fontSize);
        g2d.setFont(baseFont.deriveFont(Font.BOLD, (float) fontSize));

        FontMetrics fm = g2d.getFontMetrics();
        int textX = xOffset + (size - fm.stringWidth(letter)) / 2;
        int textY = yOffset + ((size - fm.getHeight()) / 2) + fm.getAscent();

        g2d.drawString(letter, textX, textY);

        g2d.dispose();
    }

    // ================= HELPER =================
    private String formatLetter(String input) {
        if (input == null || input.trim().isEmpty()) return "?";

        input = input.trim().toUpperCase();
        String[] parts = input.split("\\s+");

        if (parts.length >= 2) {
            return "" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0);
        }

        // 1 từ → chỉ lấy 1 chữ (UX đẹp hơn)
        return String.valueOf(input.charAt(0));
    }

    private Color generateColor(String text) {
        float hue = (text.hashCode() & 0xfffffff) / (float) 0xfffffff;
        return Color.getHSBColor(hue, 0.5f, 0.85f);
    }

    // ================= SETTER =================
    public void setLetter(String letter) {
        this.letter = formatLetter(letter);
        repaint();
    }

    public void setBgColor(Color bgColor) {
        this.bgColor = (bgColor != null) ? bgColor : generateColor(letter);
        repaint();
    }
}