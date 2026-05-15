package GUI.HoTro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class ONhapLieuHienDai extends JPanel {

    private JTextField field;
    private JLabel lblHeader;
    private JLabel lblHelper;

    private String placeholder = "";
    private boolean isError = false;
    private boolean isHover = false;

    // ================== STYLE ==================
    private static final int ARC = 15; 

    private final Color colorTextSub = Color.WHITE; 
    private final Color colorPrimary = Color.WHITE; 
    private final Color colorError = new Color(250, 80, 80); 
    private final Color colorBgDisabled = new Color(35, 45, 60);
    
    // Nền xám sáng như bạn vừa yêu cầu
    private final Color colorFieldBg = new Color(55, 60, 75); 
    
    private final Color colorBorder = new Color(100, 105, 115); 
    private final Color colorHover = new Color(140, 145, 155); 
    private final Color colorPlaceholder = new Color(170, 175, 185); 

    // ================== CONSTRUCTOR ==================
    public ONhapLieuHienDai(String label, boolean editable, boolean isPw) {
        setLayout(new BorderLayout(0, 8)); 
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 5, 0));

        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont == null) baseFont = new Font("SansSerif", Font.PLAIN, 12);

        // ===== HEADER =====
        lblHeader = new JLabel(label != null ? label : "");
        lblHeader.setFont(baseFont.deriveFont(Font.BOLD, 16f)); 
        lblHeader.setForeground(Color.WHITE);
        // ===== FIELD =====
        field = isPw ? new JPasswordField() : new JTextField();
        field.setFont(baseFont.deriveFont(Font.PLAIN, 16f)); 
        field.setEditable(editable);
        field.setOpaque(false); 
        
        // 🔥 ĐÃ SỬA LỖI MẤT CHỮ Ở ĐÂY 🔥
        // Giảm lề trên/dưới xuống còn 10px để chữ có không gian "thở"
        field.setBorder(new EmptyBorder(10, 20, 10, 20)); 
        
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        
        // Vẫn giữ cho ô chữ có độ cao lớn (60px)
        field.setPreferredSize(new Dimension(280, 60)); 

        // ===== HELPER =====
        lblHelper = new JLabel(" ");
        lblHelper.setFont(baseFont.deriveFont(13f));
        lblHelper.setForeground(colorTextSub);

        // ===== EVENTS =====
        addFocusEffect();
        addHoverEffect();
        addTextListener();

        add(lblHeader, BorderLayout.NORTH);
        add(field, BorderLayout.CENTER);
        add(lblHelper, BorderLayout.SOUTH);
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        if (preferredSize != null && preferredSize.height < 100) { 
            preferredSize = new Dimension(preferredSize.width, 100);
        }
        super.setPreferredSize(preferredSize);
    }

    private void addFocusEffect() {
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.isEditable() && !isError) {
                    lblHeader.setForeground(colorPrimary);
                }
                repaint();
            }
            public void focusLost(FocusEvent e) {
                lblHeader.setForeground(isError ? colorError : colorTextSub);
                repaint();
            }
        });
    }

    private void addHoverEffect() {
        field.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (field.isEditable()) {
                    isHover = true;
                    repaint();
                }
            }
            public void mouseExited(MouseEvent e) {
                isHover = false;
                repaint();
            }
        });
    }

    private void addTextListener() {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onChange(); }
            public void removeUpdate(DocumentEvent e) { onChange(); }
            public void changedUpdate(DocumentEvent e) { onChange(); }

            private void onChange() {
                if (isError) clearError(); 
                repaint();
            }
        });
    }


    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = field.getX();
        int y = field.getY();
        int w = field.getWidth();
        int h = field.getHeight();

        Shape shape = new RoundRectangle2D.Float(x, y, w - 1, h - 1, ARC, ARC);

        g2.setColor(field.isEditable() ? colorFieldBg : colorBgDisabled);
        g2.fill(shape);
        if (!field.isEditable()) {
            field.setForeground(new Color(150, 160, 175));
        }
        super.paintChildren(g);

        boolean isEmpty = (field instanceof JPasswordField)
                ? ((JPasswordField) field).getPassword().length == 0
                : field.getText().isEmpty();

        if (placeholder != null && isEmpty && !field.isFocusOwner()) {
            g2.setColor(colorPlaceholder);
            g2.setFont(field.getFont());

            FontMetrics fm = g2.getFontMetrics();
            int textX = x + 20; 
            int textY = y + (h - fm.getHeight()) / 2 + fm.getAscent();

            g2.drawString(placeholder, textX, textY);
        }

        if (isError) {
            g2.setColor(colorError);
            g2.setStroke(new BasicStroke(1.5f));
        } else if (field.isFocusOwner()) {
            g2.setColor(colorPrimary);
            g2.setStroke(new BasicStroke(1.5f));
        } else if (isHover) {
            g2.setColor(colorHover);
            g2.setStroke(new BasicStroke(1f));
        } else {
            g2.setColor(colorBorder);
            g2.setStroke(new BasicStroke(1f));
        }

        g2.draw(shape);
        g2.dispose();
    }

    public void setPlaceholder(String text) { this.placeholder = text; repaint(); }
    public void setError(String msg) { this.isError = true; lblHelper.setText(msg != null ? msg : ""); lblHelper.setForeground(colorError); lblHeader.setForeground(colorError); repaint(); }
    public void clearError() { this.isError = false; lblHelper.setText(" "); lblHelper.setForeground(colorTextSub); lblHeader.setForeground(field.isFocusOwner() ? colorPrimary : colorTextSub); repaint(); }
    public void setHelperText(String text) { lblHelper.setText(text != null ? text : " "); lblHelper.setForeground(colorTextSub); }
    public void setHeaderText(String text) { lblHeader.setText(text != null ? text : ""); }
    public void clear() { field.setText(""); clearError(); }
    public void setEditable(boolean editable) { field.setEditable(editable); repaint(); }
    public void requestFocusField() { field.requestFocusInWindow(); }
    public String getText() { if (field instanceof JPasswordField) { return new String(((JPasswordField) field).getPassword()); } return field.getText(); }
    public void setText(String t) { field.setText(t); }
    public char[] getPasswordChars() { if (field instanceof JPasswordField) { return ((JPasswordField) field).getPassword(); } return new char[0]; }
    public JTextField getField() { return field; }
}