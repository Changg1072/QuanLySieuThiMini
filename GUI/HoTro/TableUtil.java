package GUI.HoTro;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TableUtil {

    private static final Font BASE_FONT = UIManager.getFont("Table.font") != null
            ? UIManager.getFont("Table.font")
            : new Font("SansSerif", Font.PLAIN, 13);

    private static final Font EMOJI_FONT = new Font("Segoe UI Emoji", Font.PLAIN, 15);

    private static final NumberFormat VI_CURRENCY = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color COLOR_SUCCESS = new Color(40, 167, 69);
    private static final Color COLOR_DANGER = new Color(220, 53, 69);
    private static final Color COLOR_INFO = new Color(0, 123, 255);
    private static final Color COLOR_WARNING = new Color(255, 140, 0);
    private static final Color COLOR_PRIMARY = new Color(0, 102, 204);
    private static final Color COLOR_DARK = new Color(80, 80, 80);
    private static final Color COLOR_CART_ICON = new Color(159, 182, 205);
    private static final Color ZEBRA_COLOR = new Color(245, 247, 250);

    public static final DefaultTableCellRenderer CENTER_ALIGN = createAlignRenderer(SwingConstants.CENTER);
    public static final DefaultTableCellRenderer RIGHT_ALIGN = createAlignRenderer(SwingConstants.RIGHT);
    
    public static final DefaultTableCellRenderer TIEN_TE = new TienTeRenderer();
    public static final DefaultTableCellRenderer NGAY_THANG = new NgayThangRenderer();
    public static final DefaultTableCellRenderer TRANG_THAI = new TrangThaiBadgeRenderer();
    public static final DefaultTableCellRenderer CANH_BAO_HAN = new CanhBaoHanRenderer();
    public static final DefaultTableCellRenderer CART_ICON = new CartIconRenderer();

    private static void applyDefaultStyle(JLabel label, JTable table, boolean isSelected, int row) {
        label.setFont(BASE_FONT);
        label.setOpaque(true); 

        if (isSelected) {
            label.setForeground(table.getSelectionForeground());
            label.setBackground(table.getSelectionBackground());
        } else {
            label.setForeground(table.getForeground());
            label.setBackground((row % 2 == 0) ? table.getBackground() : ZEBRA_COLOR);
        }
    }

    private static void safeSetText(JLabel label, Object value) {
        label.setText(value != null ? value.toString() : "");
    }

    private static DefaultTableCellRenderer createAlignRenderer(int align) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(align);
                applyDefaultStyle(this, table, isSelected, row);
                safeSetText(this, value);
                return this;
            }
        };
    }

    private static class TienTeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            applyDefaultStyle(this, table, isSelected, row);

            if (value instanceof Number) {
                String text = VI_CURRENCY.format(value) + " ₫";
                setText(text);
                setToolTipText(text);
            } else {
                safeSetText(this, value);
                setToolTipText(null);
            }
            return this;
        }
    }

    private static class NgayThangRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            applyDefaultStyle(this, table, isSelected, row);

            if (value instanceof LocalDateTime) {
                String text = ((LocalDateTime) value).format(DATE_FORMATTER);
                setText(text);
                setToolTipText(text);
            } else {
                safeSetText(this, value);
                setToolTipText(null);
            }
            return this;
        }
    }

    private static class TrangThaiBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            applyDefaultStyle(this, table, isSelected, row);

            String val = (value != null) ? value.toString() : "";
            setText(val);
            setFont(BASE_FONT.deriveFont(Font.BOLD));

            if (!isSelected) {
                switch (val) {
                    case "Hoàn thành" -> setForeground(COLOR_SUCCESS);
                    case "Đã hủy" -> setForeground(COLOR_DANGER);
                    case "Đã sửa đổi" -> setForeground(COLOR_INFO);
                    default -> setForeground(table.getForeground()); 
                }
            }
            return this;
        }
    }

    private static class CanhBaoHanRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            applyDefaultStyle(this, table, isSelected, row);
            setFont(BASE_FONT.deriveFont(Font.BOLD));

            if (value instanceof Integer d) {
                if (d <= 0) {
                    setText("Hết hạn (" + d + ")");
                    if (!isSelected) {
                        setBackground(COLOR_DARK);
                        setForeground(Color.WHITE);
                    }
                } else if (d <= 3) {
                    setText(d + " ngày (Nguy hiểm)");
                    if (!isSelected) setForeground(COLOR_DANGER);
                } else if (d <= 7) {
                    setText(d + " ngày (Cận)");
                    if (!isSelected) setForeground(COLOR_WARNING);
                } else if (d <= 30) {
                    setText(d + " ngày (Sắp hết)");
                    if (!isSelected) setForeground(COLOR_PRIMARY);
                } else {
                    setText(d + " ngày");
                    if (!isSelected) setForeground(COLOR_SUCCESS);
                }
            } else {
                safeSetText(this, value);
            }
            return this;
        }
    }

    private static class CartIconRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            applyDefaultStyle(this, table, isSelected, row);

            setText("🛒");
            
            Font currentFont = UIManager.getFont("Label.font");
            if (EMOJI_FONT.getFamily().equals("Segoe UI Emoji")) {
                 setFont(EMOJI_FONT);
            } else {
                 setFont(currentFont != null ? currentFont : BASE_FONT);
            }

            if (!isSelected) {
                setForeground(COLOR_CART_ICON);
            }
            return this;
        }
    }
}