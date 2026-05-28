package GUI.HoTro;

import Data.SanPham;
import Dao.ConnectDB;
import Logic.QuanLyAnh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ✨ SUASANPHAMDIALOG - GIAO DIỆN DARK MODE KÍNH MỜ (Đã thu gọn & làm sáng nền)
 */
public class SuaSanPhamDialog extends JDialog {

    private boolean isSuccess = false;
    private SanPham spHienTai;
    private File fileAnhChon = null;

    // --- UI Components ---
    private JPanel pnlCard;
    private ONhapLieuHienDai txtMaSP, txtTenSP, txtGiaBan, txtDonVi;
    private FormGroupCombo cbLoaiSP;
    private FormGroupArea txtMoTa;
    private KhungAnh pnlKhungAnh;
    private ReactButton btnLuu, btnHuy;

    // --- Dữ liệu ---
    private Map<String, String> mapLoaiSP = new LinkedHashMap<>();

    // --- Theme Colors ---
    // Đã làm sáng màu nền thẻ (cardDark) và nền chính (bgDark) lên một chút
    private Color bgDark = new Color(25, 33, 50);
    private Color cardDark = new Color(45, 55, 72); // Sáng hơn so với bản cũ
    private Color textPrimary = Color.WHITE;
    private Color textSecondary = new Color(203, 213, 225);

    // --- Animation ---
    private float opacity = 0f;
    private float scale = 0.95f;
    private BufferedImage blurredBackground;
    private boolean isFormatting = false;
    
    // Logic 
    private Logic.SanPhamLogic spLogic = new Logic.SanPhamLogic();

    public SuaSanPhamDialog(Window parent, SanPham sp) {
        super(parent, "Chỉnh Sửa Sản Phẩm", ModalityType.APPLICATION_MODAL);
        this.spHienTai = sp;
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
        loadDuLieuLoaiSP(sp.getMaLoai()); 
        napDuLieuSanPham();
        setupRealtimeValidation();
        setupKeyBindings();

        // Animation mở form
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
                if (blurredBackground != null) g2.drawImage(blurredBackground, 0, 0, getWidth(), getHeight(), null);
                // Giảm độ đen của lớp mờ để trông sáng sủa hơn
                g2.setColor(new Color(20, 28, 45, 170));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        rootPanel.setOpaque(false);
        setContentPane(rootPanel);

        pnlCard = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(); int h = getHeight();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                g2.translate(w / 2.0, h / 2.0); g2.scale(scale, scale); g2.translate(-w / 2.0, -h / 2.0);
                g2.setColor(new Color(0, 0, 0, 60)); // Bóng đổ nhạt hơn
                g2.fillRoundRect(5, 8, w - 10, h - 8, 20, 20);
                g2.setColor(cardDark);
                g2.fillRoundRect(0, 0, w, h - 5, 16, 16);
                g2.dispose();
            }
        };
        pnlCard.setOpaque(false);
        // ĐÃ THU NHỎ KÍCH THƯỚC: từ 1150x780 xuống 980x680
        pnlCard.setPreferredSize(new Dimension(980, 680)); 
        pnlCard.setBorder(new EmptyBorder(20, 35, 15, 35));

        // Thu nhỏ font tiêu đề một chút cho cân đối
        JLabel lblTitle = new JLabel("Chỉnh sửa sản phẩm");
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 26f));
        lblTitle.setForeground(textPrimary);

        JPanel pnlBody = new JPanel(new GridLayout(1, 2, 40, 0));
        pnlBody.setOpaque(false);

        // --- CỘT TRÁI: KHUNG ẢNH ---
        JPanel pnlLeft = new JPanel(new BorderLayout(0, 15));
        pnlLeft.setOpaque(false);
        pnlLeft.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        pnlKhungAnh = new KhungAnh();
        ReactButton btnChonAnh = new ReactButton("Chọn ảnh mới", new Color(59, 73, 94)); // Sáng hơn bản cũ
        btnChonAnh.addActionListener(e -> chonAnhTuMay());
        
        pnlLeft.add(pnlKhungAnh, BorderLayout.CENTER);
        pnlLeft.add(btnChonAnh, BorderLayout.SOUTH);

        // --- CỘT PHẢI: CÁC Ô NHẬP LIỆU ---
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
        pnlRight.setOpaque(false);

        txtMaSP = new ONhapLieuHienDai("Mã Sản Phẩm (Không thể sửa)", false, false);
        txtTenSP = new ONhapLieuHienDai("Tên Sản Phẩm", true, false);
        cbLoaiSP = new FormGroupCombo("Loại Sản Phẩm");
        txtGiaBan = new ONhapLieuHienDai("Giá Bán (VNĐ)", true, false);
        txtDonVi = new ONhapLieuHienDai("Đơn Vị Tính", true, false);
        txtMoTa = new FormGroupArea("Mô Tả Sản Phẩm");

        // Giảm khoảng cách giữa các ô nhập liệu xuống còn 8px
        pnlRight.add(txtMaSP);   pnlRight.add(Box.createVerticalStrut(8));
        pnlRight.add(txtTenSP);  pnlRight.add(Box.createVerticalStrut(8));
        pnlRight.add(cbLoaiSP);  pnlRight.add(Box.createVerticalStrut(8));
        
        JPanel pnlRow = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlRow.setOpaque(false);
        pnlRow.add(txtGiaBan); pnlRow.add(txtDonVi);
        pnlRight.add(pnlRow);    pnlRight.add(Box.createVerticalStrut(8));
        
        pnlRight.add(txtMoTa);

        pnlBody.add(pnlLeft);
        pnlBody.add(pnlRight);

        // --- FOOTER ---
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlBtns.setOpaque(false);
        btnHuy = new ReactButton("Hủy bỏ", new Color(80, 95, 115));
        btnHuy.addActionListener(e -> closeWithAnimation());
        btnLuu = new ReactButton("Lưu thay đổi", new Color(37, 99, 235));
        btnLuu.addActionListener(e -> handleLuuThayDoi());
        pnlBtns.add(btnHuy); pnlBtns.add(btnLuu);

        pnlCard.add(lblTitle, BorderLayout.NORTH);
        pnlCard.add(pnlBody, BorderLayout.CENTER);
        pnlCard.add(pnlBtns, BorderLayout.SOUTH);
        rootPanel.add(pnlCard);
    }

    private void napDuLieuSanPham() {
        if (spHienTai != null) {
            txtMaSP.setText(spHienTai.getMaSP() != null ? spHienTai.getMaSP() : "");
            txtTenSP.setText(spHienTai.getTenSP() != null ? spHienTai.getTenSP() : "");
            txtDonVi.setText(spHienTai.getDonViTinh() != null ? spHienTai.getDonViTinh() : "");
            
            if (spHienTai.getGiaBan() != null) {
                txtGiaBan.setText(String.format("%.0f", spHienTai.getGiaBan().doubleValue()));
            }

            ImageIcon iconGoc = QuanLyAnh.layIconAnh(spHienTai.getLinkHinhAnh(), 600, 600); // Tải ảnh nét cho khung nhỏ
            if (iconGoc != null) {
                pnlKhungAnh.setImage(iconGoc.getImage());
            }
        }
    }

    // --- CÁC COMPONENT PHỤ TRỢ DARK THEME ---

    private class KhungAnh extends JPanel {
        private Image img;
        public KhungAnh() {
            setOpaque(false);
            setDropTarget(new DropTarget() {
                public synchronized void drop(DropTargetDropEvent evt) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY);
                        java.util.List<File> files = (java.util.List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) xuLyHienThiAnh(files.get(0));
                    } catch (Exception ex) {}
                }
            });
        }
        public void setImage(Image image) { this.img = image; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(25, 33, 50)); // Nền khung ảnh sáng hơn bản cũ
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            if (img != null) {
                g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
            } else {
                g2.setColor(textSecondary);
                g2.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(16f));
                String hint = "Kéo & Thả ảnh mới vào đây";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, (getWidth()-fm.stringWidth(hint))/2, (getHeight()+fm.getAscent())/2 - 10);
            }
            g2.dispose();
        }
    }

    private class FormGroupCombo extends JPanel {
        private JComboBox<String> cb;
        public FormGroupCombo(String label) {
            setLayout(new BorderLayout(0, 6));
            setOpaque(false);
            JLabel lblHeader = new JLabel(label);
            lblHeader.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 15f));
            lblHeader.setForeground(Color.WHITE);
            cb = new JComboBox<>();
            cb.setPreferredSize(new Dimension(0, 50)); // Giảm chiều cao cb để tiết kiệm không gian
            cb.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(15f));
            cb.setUI(new BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton btn = new JButton("▼");
                    btn.setBorder(BorderFactory.createEmptyBorder());
                    btn.setContentAreaFilled(false);
                    btn.setForeground(textSecondary);
                    return btn;
                }
            });
            cb.setBackground(new Color(60, 70, 85)); // Màu sáng hơn
            cb.setForeground(Color.WHITE);
            add(lblHeader, BorderLayout.NORTH);
            add(cb, BorderLayout.CENTER);
        }
        public JComboBox<String> getCombo() { return cb; }
    }

    private class FormGroupArea extends JPanel {
        private JTextArea ta;
        public FormGroupArea(String label) {
            setLayout(new BorderLayout(0, 6));
            setOpaque(false);
            JLabel lblHeader = new JLabel(label);
            lblHeader.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 15f));
            lblHeader.setForeground(Color.WHITE);
            
            ta = new JTextArea();
            ta.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(15f));
            ta.setBackground(new Color(60, 70, 85)); // Màu sáng hơn
            ta.setForeground(Color.WHITE);
            ta.setCaretColor(Color.WHITE);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setBorder(new EmptyBorder(8, 12, 8, 12));
            
            JScrollPane scroll = new JScrollPane(ta);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(110, 120, 135), 1, true));
            scroll.setBackground(new Color(60, 70, 85));
            TienIchGiaoDien.thietLapThanhCuon(scroll);

            add(lblHeader, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
        }
        public String getText() { return ta.getText(); }
    }

    private class ReactButton extends JButton {
        public ReactButton(String text, Color bg) {
            super(text);
            setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
            setForeground(Color.WHITE);
            setBackground(bg);
            setPreferredSize(new Dimension(160, 48)); // Cỡ nút gọn lại
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = getModel().isRollover() ? getBackground().brighter() : getBackground();
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    // --- LOGIC XỬ LÝ ---

    private void setupRealtimeValidation() {
        txtGiaBan.getField().getDocument().addDocumentListener(new DocumentListener() {
            private void format() {
                if (isFormatting) return;
                SwingUtilities.invokeLater(() -> {
                    isFormatting = true;
                    String raw = txtGiaBan.getText().replaceAll("[^\\d]", "");
                    if (!raw.isEmpty()) {
                        try {
                            String formatted = new DecimalFormat("#,###").format(new BigDecimal(raw));
                            if (!txtGiaBan.getText().equals(formatted)) txtGiaBan.setText(formatted);
                        } catch (Exception e) {}
                    }
                    txtGiaBan.clearError();
                    isFormatting = false;
                });
            }
            public void insertUpdate(DocumentEvent e) { format(); }
            public void removeUpdate(DocumentEvent e) { format(); }
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void setupKeyBindings() {
        pnlCard.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        pnlCard.getActionMap().put("close", new AbstractAction() { public void actionPerformed(ActionEvent e) { closeWithAnimation(); } });
    }

    private void loadDuLieuLoaiSP(String maLoaiHienTai) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try (Connection c = ConnectDB.getInstance().getConnection();
                     Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT MaLoai, TenLoai FROM LoaiSP")) {
                    while (rs.next()) mapLoaiSP.put(rs.getString("TenLoai"), rs.getString("MaLoai"));
                }
                return null;
            }
            @Override protected void done() {
                cbLoaiSP.getCombo().removeAllItems();
                String itemToSelect = null;
                for (Map.Entry<String, String> entry : mapLoaiSP.entrySet()) {
                    cbLoaiSP.getCombo().addItem(entry.getKey());
                    if (entry.getValue().equals(maLoaiHienTai)) itemToSelect = entry.getKey();
                }
                if (itemToSelect != null) cbLoaiSP.getCombo().setSelectedItem(itemToSelect);
            }
        }.execute();
    }

    private void chonAnhTuMay() {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chọn ảnh mới", FileDialog.LOAD);
        fd.setFile("*.jpg;*.png;*.jpeg");
        fd.setFilenameFilter((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|png|jpeg)$"));
        fd.setVisible(true);
        
        if (fd.getDirectory() != null && fd.getFile() != null) {
            xuLyHienThiAnh(new File(fd.getDirectory() + fd.getFile()));
        }
    }
  
    private void xuLyHienThiAnh(File f) {
        if (!f.getName().toLowerCase().matches(".*\\.(jpg|png|jpeg)$")) {
            TienIchGiaoDien.hienThiThongBao(this, "Chỉ hỗ trợ file ảnh!", "WARNING");
            return;
        }
        fileAnhChon = f;
        pnlKhungAnh.setImage(new ImageIcon(f.getAbsolutePath()).getImage());
    }

    private void handleLuuThayDoi() {
        if (txtTenSP.getText().trim().isEmpty()) { txtTenSP.setError("Không được để trống!"); return; }
        
        try {
            String giaRaw = txtGiaBan.getText().replaceAll("[^\\d]", "");
            if (giaRaw.isEmpty()) giaRaw = "0";

            // Lấy link ảnh (Nếu chọn ảnh mới thì lấy đường dẫn mới, không thì giữ link cũ)
            String linkAnhCuoiCung = (fileAnhChon != null) ? fileAnhChon.getAbsolutePath() : spHienTai.getLinkHinhAnh();

            SanPham spCapNhat = new SanPham.ThoXaySanPham()
                    .ganMaSP(txtMaSP.getText().trim())
                    .ganTenSP(txtTenSP.getText().trim())
                    .ganMaLoai(mapLoaiSP.get(cbLoaiSP.getCombo().getSelectedItem()))
                    .ganGiaBan(new BigDecimal(giaRaw))
                    .ganDonViTinh(txtDonVi.getText().trim())
                    .ganLinkHinhAnh(linkAnhCuoiCung)
                    // .ganMoTa(txtMoTa.getText()) -> Nếu DB bạn có lưu mô tả
                    .taoMoi();

            // Gọi Logic lưu vào Database
            spLogic.suaSanPham(spCapNhat);

            isSuccess = true;
            closeWithAnimation();
        } catch (Exception e) {
            TienIchGiaoDien.hienThiThongBao(this, e.getMessage(), "ERROR");
        }
    }

    private void closeWithAnimation() {
        Timer t = new Timer(10, e -> {
            opacity -= 0.08f; scale -= 0.01f;
            if (opacity <= 0) { dispose(); ((Timer)e.getSource()).stop(); }
            repaint();
        }); t.start();
    }

    public boolean isSuccess() { return isSuccess; }

    // =========================================================
    // HÀM HIỂN THỊ POPUP (GỌI TỪ CHITIETSANPHAM.JAVA)
    // =========================================================
    public static void hienThi(Window parentWindow, SanPham sp, Runnable onCloseCallback) {
        JFrame frame = null;
        if (parentWindow instanceof JFrame) frame = (JFrame) parentWindow;
        else if (parentWindow instanceof JDialog) frame = (JFrame) SwingUtilities.getWindowAncestor(parentWindow);

        SuaSanPhamDialog dialog = new SuaSanPhamDialog(frame, sp);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (onCloseCallback != null) {
                    onCloseCallback.run(); 
                }
            }
        });

        dialog.setVisible(true);
    }
}