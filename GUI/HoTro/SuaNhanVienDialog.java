package GUI.HoTro;

import Data.NhanVien;
import Logic.NhanVienLogic;
import Logic.TaiKhoanLogic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class SuaNhanVienDialog extends JDialog {

    private NhanVien nhanVienCanSua;
    private boolean isSuccess = false;

    // --- UI Components ---
    private JPanel pnlCard;
    // ĐÃ XÓA: txtLuong
    private FormGroup txtMaNV, txtHoTen, txtSDT;
    private ComboGroup cbChucVu;
    private ReactButton btnLuu, btnHuy;

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

    public SuaNhanVienDialog(Window parent, NhanVien nv) {
        super(parent, "Chỉnh sửa nhân viên", ModalityType.APPLICATION_MODAL);
        this.nhanVienCanSua = nv;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); 
        setSize(parent.getSize()); 
        setLocationRelativeTo(parent);

        taoHieuUngKinhMo(parent);
        initUI();
        loadDataVaoForm();
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

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (blurredBackground != null) g2.drawImage(blurredBackground, 0, 0, getWidth(), getHeight(), null);
        g2.setColor(new Color(15, 23, 42, 190)); 
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        super.paint(g); 
        g2.dispose();
    }

    private void initUI() {
        setLayout(new GridBagLayout()); 

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
        // ĐÃ CĂN CHỈNH LẠI KÍCH THƯỚC: Rút ngắn chiều cao do đã bớt 1 dòng Nhập liệu
        pnlCard.setPreferredSize(new Dimension(720, 410)); 
        pnlCard.setBorder(new EmptyBorder(30, 35, 30, 35));

        JLabel lblTitle = new JLabel("Chỉnh sửa nhân viên");
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(24f));
        lblTitle.setForeground(textPrimary);
        lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

        // ĐÃ SỬA BỐ CỤC LẠI CHỈ CÒN 2 DÒNG 2 CỘT
        JPanel pnlForm = new JPanel(new GridLayout(2, 2, 25, 20)); 
        pnlForm.setOpaque(false);

        txtMaNV = new FormGroup("Mã Nhân Viên", false);
        cbChucVu = new ComboGroup("Chức Vụ", new String[]{"ADMIN", "Thu Ngân"});
        txtHoTen = new FormGroup("Họ và Tên", true);
        txtSDT = new FormGroup("Số Điện Thoại", true);

        pnlForm.add(txtMaNV);    pnlForm.add(cbChucVu);
        pnlForm.add(txtHoTen);   pnlForm.add(txtSDT);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlBtns.setOpaque(false);
        pnlBtns.setBorder(new EmptyBorder(15, 0, 0, 0));

        btnHuy = new ReactButton("Hủy bỏ", new Color(71, 85, 105)); 
        btnHuy.addActionListener(e -> closeWithAnimation());

        btnLuu = new ReactButton("Lưu thay đổi", new Color(37, 99, 235)); 
        btnLuu.addActionListener(e -> xuLyLuu());

        pnlBtns.add(btnHuy); pnlBtns.add(btnLuu);

        pnlCard.add(lblTitle, BorderLayout.NORTH);
        pnlCard.add(pnlForm, BorderLayout.CENTER);
        pnlCard.add(pnlBtns, BorderLayout.SOUTH);

        add(pnlCard); 
    }

    private void loadDataVaoForm() {
        txtMaNV.setText(nhanVienCanSua.getMaNV());
        txtHoTen.setText(nhanVienCanSua.getHoTen());
        txtSDT.setText(nhanVienCanSua.getSDT());
        cbChucVu.setSelectedItem(nhanVienCanSua.getChucVu());
    }

    private class FormGroup extends JPanel {
        private JTextField txt;
        private JLabel lblError;
        private boolean isFocus = false;

        public FormGroup(String labelTitle, boolean editable) {
            setLayout(new BorderLayout(0, 6)); 
            setOpaque(false);

            JLabel lbl = new JLabel(labelTitle);
            lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
            lbl.setForeground(textSecondary);
            
            txt = new JTextField() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    g2.setColor(isEditable() ? bgDark : new Color(15, 23, 42, 120));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    
                    if (lblError.getText().trim().length() > 0) {
                        g2.setColor(new Color(239, 68, 68)); 
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
            lblError.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f));
            lblError.setForeground(new Color(239, 68, 68)); 

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

    private class ComboGroup extends JPanel {
        private JComboBox<String> cb;
        public ComboGroup(String labelTitle, String[] items) {
            setLayout(new BorderLayout(0, 6));
            setOpaque(false);

            JLabel lbl = new JLabel(labelTitle);
            lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
            lbl.setForeground(textSecondary);

            cb = new JComboBox<>(items) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bgDark);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(borderNormal);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            cb.setPreferredSize(new Dimension(0, 48)); 
            cb.setOpaque(false);
            cb.setBackground(new Color(0,0,0,0)); 
            cb.setForeground(textPrimary);
            cb.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(16f));
            cb.setBorder(new EmptyBorder(0, 15, 0, 15));
            cb.setFocusable(false);
            
            cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override
                public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {}
                @Override protected JButton createArrowButton() {
                    JButton btn = new JButton("▼");
                    btn.setFont(new Font("Arial", Font.PLAIN, 12));
                    btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
                    btn.setContentAreaFilled(false);
                    btn.setForeground(textSecondary);
                    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    return btn;
                }
            });

            cb.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    l.setBorder(new EmptyBorder(10, 10, 10, 10)); 
                    l.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(15f));
                    if (index == -1) {
                        l.setOpaque(false);
                        l.setForeground(textPrimary);
                    } else {
                        l.setOpaque(true);
                        if (isSelected) {
                            l.setBackground(borderFocus);
                            l.setForeground(Color.WHITE);
                        } else {
                            l.setBackground(cardDark);
                            l.setForeground(textPrimary);
                        }
                    }
                    return l;
                }
            });

            JLabel lblDummy = new JLabel(" ");
            lblDummy.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(11f));

            add(lbl, BorderLayout.NORTH);
            add(cb, BorderLayout.CENTER);
            add(lblDummy, BorderLayout.SOUTH);
        }
        public Object getSelectedItem() { return cb.getSelectedItem(); }
        public void setSelectedItem(Object o) { cb.setSelectedItem(o); }
    }

    private class ReactButton extends JButton {
        private float scaleBtn = 1.0f;
        
        public ReactButton(String text, Color baseColor) {
            super(text); 
            setBackground(baseColor); 
            
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setPreferredSize(new Dimension(140, 46)); 
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
            g2.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
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
                if (!sdt.isEmpty() && !sdt.matches("^0\\d{9}$")) txtSDT.setError("Chỉ gồm 10 số & bđ bằng 0");
                else txtSDT.clearError();
            }
        });
    }

    private void setupKeyBindings() {
        pnlCard.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        pnlCard.getActionMap().put("close", new AbstractAction() { public void actionPerformed(ActionEvent e) { closeWithAnimation(); } });
        pnlCard.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "save");
        pnlCard.getActionMap().put("save", new AbstractAction() { public void actionPerformed(ActionEvent e) { xuLyLuu(); } });
    }

    private void xuLyLuu() {
        txtHoTen.clearError(); txtSDT.clearError(); 
        String ten = txtHoTen.getText().trim();
        String sdt = txtSDT.getText().trim();

        if (ten.isEmpty()) { txtHoTen.setError("Bắt buộc"); return; }
        if (!sdt.matches("^0\\d{9}$")) { txtSDT.setError("Sai định dạng"); return; }

        AdminAuthDialog auth = new AdminAuthDialog(this);
        auth.setVisible(true);
        if (!auth.isAuthenticated()) return;

        try {
            nhanVienCanSua.setHoTen(ten);
            nhanVienCanSua.setSDT(sdt);
            nhanVienCanSua.setChucVu(cbChucVu.getSelectedItem().toString());
            // ĐÃ XÓA SET LƯƠNG TRỰC TIẾP TẠI ĐÂY

            new NhanVienLogic().suaNhanVien(nhanVienCanSua);
            isSuccess = true;
            TienIchGiaoDien.hienThiThongBao(this, "Cập nhật thành công!", "SUCCESS");
            closeWithAnimation();
        } catch (Exception ex) {
            TienIchGiaoDien.hienThiThongBao(this, "Lỗi: " + ex.getMessage(), "ERROR");
        }
    }

    private void closeWithAnimation() {
        Timer t = new Timer(10, e -> {
            opacity -= 0.08f; scale -= 0.01f;
            if(opacity <= 0) { dispose(); ((Timer)e.getSource()).stop(); }
            repaint();
        }); t.start();
    }

    public boolean isSuccess() { return isSuccess; }

    private class AdminAuthDialog extends JDialog {
        private boolean authenticated = false;
        private JPasswordField txtPass;  

        public AdminAuthDialog(JDialog parent) {
            super(parent, true);
            setUndecorated(true);
            setBackground(new Color(0, 0, 0, 0));

            JPanel pnl = new JPanel(new BorderLayout(0, 12)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(30, 41, 59, 240));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    g2.setColor(new Color(239, 68, 68));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                }
            };
            pnl.setBorder(new EmptyBorder(20, 25, 20, 25));
            pnl.setOpaque(false);

            JLabel lbl = new JLabel("🔑 Xác nhận mật khẩu ADMIN", SwingConstants.CENTER);
            lbl.setForeground(new Color(239, 68, 68));
            lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(16f));

            txtPass = new JPasswordField() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bgDark);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(isFocusOwner() ? borderFocus : borderNormal);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            txtPass.setPreferredSize(new Dimension(280, 48));
            txtPass.setOpaque(false);
            txtPass.setBorder(new EmptyBorder(0, 15, 0, 15));
            txtPass.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(16f));
            txtPass.setForeground(textPrimary);
            txtPass.setCaretColor(textPrimary);
            txtPass.setEchoChar('●');

            JLabel lblError = new JLabel(" ");
            lblError.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f));
            lblError.setForeground(new Color(239, 68, 68));

            JPanel pnlField = new JPanel(new BorderLayout(0, 6));
            pnlField.setOpaque(false);
            JLabel lblTitle = new JLabel("Mật khẩu Admin");
            lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14.5f));
            lblTitle.setForeground(textSecondary);
            pnlField.add(lblTitle, BorderLayout.NORTH);
            pnlField.add(txtPass, BorderLayout.CENTER);
            pnlField.add(lblError, BorderLayout.SOUTH);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btns.setOpaque(false);

            ReactButton btnHuyAuth = new ReactButton("Hủy", new Color(71, 85, 105));
            btnHuyAuth.addActionListener(e -> dispose());

            ReactButton btnCheck = new ReactButton("Xác Nhận", new Color(220, 38, 38));
            btnCheck.addActionListener(e -> verify(lblError));

            btns.add(btnHuyAuth);
            btns.add(btnCheck);

            pnl.add(lbl, BorderLayout.NORTH);
            pnl.add(pnlField, BorderLayout.CENTER);
            pnl.add(btns, BorderLayout.SOUTH);

            setContentPane(pnl);
            setSize(360, 220);
            setLocationRelativeTo(parent);

            SwingUtilities.invokeLater(() -> txtPass.requestFocusInWindow());

            pnl.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "auth");
            pnl.getActionMap().put("auth", new AbstractAction() { public void actionPerformed(ActionEvent e) { verify(lblError); } });
            pnl.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeAuth");
            pnl.getActionMap().put("closeAuth", new AbstractAction() { public void actionPerformed(ActionEvent e) { dispose(); } });
        }

        private void verify(JLabel lblError) {
            try {
                String[] res = new TaiKhoanLogic().xuLyDangNhap("admin", new String(txtPass.getPassword()));
                if (res != null) {
                    authenticated = true;
                    dispose();
                } else {
                    lblError.setText("Mật khẩu sai!");
                    txtPass.setText("");                    
                    txtPass.requestFocusInWindow();         
                }
            } catch (Exception e) {
                lblError.setText("Mật khẩu sai!");
                txtPass.setText("");
                txtPass.requestFocusInWindow();
            }
        }
        public boolean isAuthenticated() { return authenticated; }
    }
}
