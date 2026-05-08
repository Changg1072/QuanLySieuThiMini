package GUI.HoTro;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Locale;

public class GiaoDienUtil {

    // ================== CONSTANT ==================
    private static final Locale DEFAULT_LOCALE = new Locale("vi", "VN");

    private static final Font BASE_FONT =
            UIManager.getFont("Label.font") != null
                    ? UIManager.getFont("Label.font")
                    : new Font("SansSerif", Font.PLAIN, 13);

    private static final Font TITLE_FONT = BASE_FONT.deriveFont(Font.BOLD, 13f);
    private static final Font VALUE_FONT = BASE_FONT.deriveFont(Font.PLAIN, 13f);

    private static final Color VALUE_COLOR =
            UIManager.getColor("Label.foreground") != null
                    ? UIManager.getColor("Label.foreground")
                    : Color.BLACK;

    private static final Color TITLE_COLOR = VALUE_COLOR.darker();

    private static final int INDENT = 50;
    private static final int DEFAULT_MARGIN = 5;

    private static final Insets TITLE_INSETS = new Insets(10, 5, 10, 5);
    private static final Insets VALUE_INSETS = new Insets(10, 5, 10, 5);

    // ================== MAIN ==================
    public static JLabel addDongThongTin(JPanel panel,
                                         GridBagConstraints gbc,
                                         int row,
                                         int colStart,
                                         String tieuDe,
                                         JLabel lblGiaTri) {

        // ===== VALIDATE =====
        if (panel == null || gbc == null || lblGiaTri == null) {
            throw new IllegalArgumentException("Tham số không được null");
        }

        GridBagConstraints gbcLocal = (GridBagConstraints) gbc.clone();

        int leTrai = (colStart > 0) ? INDENT : DEFAULT_MARGIN;

        // ===== TITLE =====
        gbcLocal.gridy = row;
        gbcLocal.gridx = colStart;
        gbcLocal.weightx = 0.0;
        gbcLocal.insets = new Insets(
                TITLE_INSETS.top,
                leTrai,
                TITLE_INSETS.bottom,
                TITLE_INSETS.right
        );
        gbcLocal.anchor = GridBagConstraints.WEST;

        JLabel lblTitle = new JLabel(tieuDe != null ? tieuDe : "");
        lblTitle.setFont(TITLE_FONT);
        lblTitle.setForeground(TITLE_COLOR);

        panel.add(lblTitle, gbcLocal);

        // ===== VALUE =====
        gbcLocal.gridx = colStart + 1;
        gbcLocal.weightx = 1.0;
        gbcLocal.insets = VALUE_INSETS;
        gbcLocal.fill = GridBagConstraints.HORIZONTAL;
        gbcLocal.anchor = GridBagConstraints.WEST;

        // Chỉ set font nếu là font mặc định hệ thống
        if (lblGiaTri.getFont() instanceof FontUIResource) {
            lblGiaTri.setFont(VALUE_FONT);
        }

        lblGiaTri.setForeground(VALUE_COLOR);

        panel.add(lblGiaTri, gbcLocal);

        return lblGiaTri;
    }

    // ================== OVERLOAD ==================
    public static JLabel addDongThongTin(JPanel panel,
                                         GridBagConstraints gbc,
                                         int row,
                                         int colStart,
                                         String tieuDe,
                                         String giaTri) {

        JLabel lbl = new JLabel(giaTri != null ? giaTri : "");
        return addDongThongTin(panel, gbc, row, colStart, tieuDe, lbl);
    }

    // ================== LABEL VALUE ==================
    public static JLabel taoLabelGiaTri(String text) {
        JLabel lbl = new JLabel(text != null ? text : "");
        lbl.setFont(VALUE_FONT);
        lbl.setForeground(VALUE_COLOR);
        return lbl;
    }

    // ================== LABEL ĐA DÒNG ==================
    public static JLabel taoLabelDaDong(String text, int widthPX) {
        String safeText = escapeHtml(text != null ? text : "");

        String htmlText = String.format(
                "<html><div style='width:%dpx;'>%s</div></html>",
                widthPX,
                safeText
        );

        JLabel lbl = new JLabel(htmlText);
        lbl.setFont(VALUE_FONT);
        lbl.setForeground(VALUE_COLOR);
        lbl.setVerticalAlignment(SwingConstants.TOP);

        return lbl;
    }

    // ================== GBC DEFAULT ==================
    public static GridBagConstraints taoGbcMacDinh() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }

    // ================== SPACER ==================
    public static void addSpacer(JPanel panel,
                                GridBagConstraints gbc,
                                int row,
                                int height) {

        if (panel == null || gbc == null) return;

        GridBagConstraints gbcLocal = (GridBagConstraints) gbc.clone();
        gbcLocal.gridy = row;
        gbcLocal.gridx = 0;
        gbcLocal.gridwidth = 10;

        panel.add(Box.createVerticalStrut(height), gbcLocal);
    }

    // ================== HELPER ==================
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}