package GUI.HoTro;

import Data.NhaCungCap;
import Logic.TaoMaTuDongLogic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class ThemNhaCungCapDialog extends JDialog {

    private boolean isSuccess = false;
    private NhaCungCap nhaCungCapMoi;

    // --- UI Components ---
    private JPanel pnlCard;
    private FormGroup txtMaNCC, txtTenNCC, txtSDT, txtEmail, txtDiaChi;
    private ReactButton btnTao, btnHuy;

    // --- Theme Colors ---
    private Color bgDark = new Color(15, 23, 42);       
    private Color cardDark = new Color(30, 41, 59);     
    private Color borderNormal = new Color(51, 65, 85); 
    private Color borderFocus = new Color(59, 130, 246);
    private Color textPrimary = Color.WHITE;
    private Color textSecondary = new Color(203, 213, 225);

    // --- Animation ---
    private float opacity = 0f;
    private float scale = 0.95f;
    private BufferedImage blurredBackground;

    public ThemNhaCungCapDialog(Window parent) {
        super(parent, "Tạo Nhà Cung Cấp Mới", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); 
        
        if (parent != null) {
            setBounds(parent.getBounds());
        } else {
            setSize(1366, 768);
            setLocationRelativeTo(null);
        }

        taoHieuUngKinhMo(parent);
        initUI();
        setupAutoData();
        setupRealtimeValidation();
        setupKeyBindings();

        Timer timer = new Timer(10, e -> {
            opacity += 0.06f;
            scale += 0.006f;
            if (opacity >= 1f) {
                opacity = 1f; scale = 1f;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        timer.start();
    }

    private void taoHieuUngKinhMo(Window parent) {
        if (parent == null || !parent.isShowing()) return;
        try {
            Robot robot = new Robot();
            Rectangle rect = parent.getBounds();
            BufferedImage screen = robot.createScreenCapture(rect);
            float weight = 1.0f / 25.0f;
            float[] data = new float[25];
            for (int i = 0; i < 25; i++) data[i] = weight;
            Kernel kernel = new Kernel(5, 5, data);
            ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
            blurredBackground = op.filter(screen, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (blurredBackground != null) {
                    g2.drawImage(blurredBackground, 0, 0, getWidth(), getHeight(), null);
                }
                g2.setColor(new Color(15, 23, 42, 190)); 
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                g2.dispose();
            }
        };
        rootPanel.setOpaque(false);
        setContentPane(rootPanel);

        pnlCard = new JPanel(new BorderLayout(0, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(); int h = getHeight();

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                g2.translate(w / 2.0, h / 2.0); g2.scale(scale, scale); g2.translate(-w / 2.0, -h / 2.0);

                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillRoundRect(5, 10, w - 10, h - 10, 20, 20);

                g2.setColor(cardDark);
                g2.fillRoundRect(0, 0, w, h - 5, 16, 16);

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(255, 255, 255, 25));
                g2.drawRoundRect(0, 0, w - 1, h - 6, 16, 16);

                g2.dispose();
            }
        };
        pnlCard.setOpaque(false);
        pnlCard.setPreferredSize(new Dimension(720, 520)); 
        pnlCard.setBorder(new EmptyBorder(30, 35, 30, 35));

        JLabel lblTitle = new JLabel("Tạo nhà cung cấp mới");
        // 🔥 ĐÃ ĐỒNG BỘ: Ép cứng Font.BOLD
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 24f));
        lblTitle.setForeground(textPrimary);
        lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

        JPanel pnlForm = new JPanel(new GridLayout(3, 2, 25, 20)); 
        pnlForm.setOpaque(false);

        txtMaNCC = new FormGroup("Mã Nhà Cung Cấp", false);
        txtTenNCC = new FormGroup("Tên Nhà Cung Cấp", true);
        txtSDT = new FormGroup("Số Điện Thoại", true);
        txtEmail = new FormGroup("Email Liên Hệ", true);
        txtDiaChi = new FormGroup("Địa Chỉ", true);
        
        JPanel pnlEmpty = new JPanel(); pnlEmpty.setOpaque(false);

        pnlForm.add(txtMaNCC);    pnlForm.add(txtTenNCC);
        pnlForm.add(txtSDT);      pnlForm.add(txtEmail);
        pnlForm.add(txtDiaChi);   pnlForm.add(pnlEmpty); 

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlBtns.setOpaque(false);
        pnlBtns.setBorder(new EmptyBorder(15, 0, 0, 0));

        btnHuy = new ReactButton("Hủy bỏ", new Color(71, 85, 105));
        btnHuy.addActionListener(e -> closeWithAnimation());

        btnTao = new ReactButton("+ Tạo mới", new Color(37, 99, 235));
        btnTao.addActionListener(e -> handleTaoNhaCungCap());

        pnlBtns.add(btnHuy); pnlBtns.add(btnTao);

        pnlCard.add(lblTitle, BorderLayout.NORTH);
        pnlCard.add(pnlForm, BorderLayout.CENTER);
        pnlCard.add(pnlBtns, BorderLayout.SOUTH);

        rootPanel.add(pnlCard); 
    }

    private void setupAutoData() {
        txtMaNCC.setText(TaoMaTuDongLogic.taoMaNhaCungCap());
    }

    private class FormGroup extends JPanel {
        private JTextField txt;
        private JLabel lblError;
        private boolean isFocus = false;

        public FormGroup(String labelTitle, boolean editable) {
            setLayout(new BorderLayout(0, 6)); 
            setOpaque(false);

            JLabel lbl = new JLabel(labelTitle);
            // 🔥 ĐÃ ĐỒNG BỘ: Ép cứng Font.BOLD
            lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 15f));
            lbl.setForeground(textSecondary);
            
            txt = new JTextField() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    g2.setColor(isEditable() ? bgDark : new Color(15, 23, 42, 120));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    
                    if (lblError.getText().trim().length() > 0) {
                        g2.setColor(new Color(255, 99, 103)); 
                        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    } else if (isFocus) {
                        g2.setColor(new Color(59, 130, 246, 50)); g2.setStroke(new BasicStroke(3f));
                        g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 8, 8); 
                        g2.setColor(borderFocus); g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8); 
                    } else {
                        g2.setColor(borderNormal);
                        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            txt.setPreferredSize(new Dimension(0, 48)); 
            txt.setOpaque(false);
            txt.setBorder(new EmptyBorder(0, 15, 0, 15)); 
            txt.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(16f));
            txt.setCaretColor(textPrimary);
            
            txt.setEditable(editable);
            if (!editable) {
                txt.setFocusable(false);
                txt.setForeground(new Color(170, 180, 195)); 
            } else {
                txt.setForeground(textPrimary);
            }

            txt.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { isFocus = true; repaint(); }
                public void focusLost(FocusEvent e) { isFocus = false; repaint(); }
            });

            lblError = new JLabel(" ");
            // 🔥 ĐÃ ĐỒNG BỘ: Ép cứng Font.BOLD
            lblError.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(Font.BOLD, 13.5f));
            lblError.setForeground(new Color(255, 99, 103)); 

            add(lbl, BorderLayout.NORTH);
            add(txt, BorderLayout.CENTER);
            add(lblError, BorderLayout.SOUTH);
        }

        public void setText(String t) { txt.setText(t); }
        public String getText() { return txt.getText(); }
        public void setError(String msg) { lblError.setText(msg); txt.repaint(); }
        public void clearError() { lblError.setText(" "); txt.repaint(); }
        public JTextField getField() { return txt; }
    }

    private class ReactButton extends JButton {
        private float scaleBtn = 1.0f;
        
        public ReactButton(String text, Color baseColor) {
            super(text); 
            setBackground(baseColor); 
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setPreferredSize(new Dimension(150, 46)); 
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { animate(0.95f); }
                public void mouseReleased(MouseEvent e) { animate(1.0f); }
            });
        }
        
        private void animate(float target) {
            Timer t = new Timer(10, e -> {
                scaleBtn += (target - scaleBtn) * 0.4f;
                if(Math.abs(target - scaleBtn) < 0.01f) ((Timer)e.getSource()).stop();
                repaint();
            }); t.start();
        }
        
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(); int h = getHeight();
            
            g2.translate(w/2.0, h/2.0); g2.scale(scaleBtn, scaleBtn); g2.translate(-w/2.0, -h/2.0);
            
            Color bg = getModel().isRollover() ? getBackground().brighter() : getBackground();
            g2.setColor(bg); g2.fillRoundRect(0, 0, w, h, 8, 8);
            
            g2.setColor(Color.WHITE);
            // 🔥 ĐÃ ĐỒNG BỘ: Ép cứng Font.BOLD
            g2.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 15f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (w-fm.stringWidth(getText()))/2, (h+fm.getAscent()-fm.getDescent())/2);
            g2.dispose();
        }
    }
    
    private void setupRealtimeValidation() {
        txtSDT.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { check(); }
            public void removeUpdate(DocumentEvent e) { check(); }
            public void changedUpdate(DocumentEvent e) { check(); }
            private void check() {
                String sdt = txtSDT.getText();
                if (!sdt.isEmpty() && !sdt.matches("^0\\d{9}$")) txtSDT.setError("Chỉ gồm 10 số & bắt đầu bằng 0");
                else txtSDT.clearError();
            }
        });
    }

    private void setupKeyBindings() {
        pnlCard.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        pnlCard.getActionMap().put("close", new AbstractAction() { public void actionPerformed(ActionEvent e) { closeWithAnimation(); } });
        pnlCard.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "save");
        pnlCard.getActionMap().put("save", new AbstractAction() { public void actionPerformed(ActionEvent e) { handleTaoNhaCungCap(); } });
    }

    private void handleTaoNhaCungCap() {
        txtTenNCC.clearError(); txtSDT.clearError(); txtEmail.clearError(); txtDiaChi.clearError();
        boolean valid = true;
        if (txtTenNCC.getText().trim().isEmpty()) { txtTenNCC.setError("Vui lòng nhập tên NCC!"); valid = false; }
        String sdt = txtSDT.getText().trim();
        if (!sdt.matches("^0\\d{9}$")) { txtSDT.setError("SĐT gồm 10 số, bắt đầu là 0"); valid = false; }
        String email = txtEmail.getText().trim();
        if (email.isEmpty() || !email.contains("@")) { txtEmail.setError("Email không hợp lệ!"); valid = false; }
        if (txtDiaChi.getText().trim().isEmpty()) { txtDiaChi.setError("Vui lòng nhập địa chỉ!"); valid = false; }
        if (!valid) return;

        nhaCungCapMoi = new NhaCungCap.ThoXayNhaCungCap()
                .ganMaNCC(txtMaNCC.getText().trim())
                .ganTenNCC(txtTenNCC.getText().trim())
                .ganSDT(sdt)
                .ganEmail(email)
                .ganDiaChi(txtDiaChi.getText().trim())
                .taoMoi();

        isSuccess = true;
        closeWithAnimation();
    }

    private void closeWithAnimation() {
        Timer t = new Timer(10, e -> {
            opacity -= 0.08f; scale -= 0.01f;
            if(opacity <= 0) { dispose(); ((Timer)e.getSource()).stop(); }
            repaint();
        }); t.start();
    }

    public boolean isSuccess() { return isSuccess; }
    public NhaCungCap getNhaCungCapMoi() { return nhaCungCapMoi; }
}