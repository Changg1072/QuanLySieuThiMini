package GUI.HoTro;

import Data.SanPham;
import Logic.TaoMaTuDongLogic;
import Dao.ConnectDB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
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
 * ✨ THEMSANPHAMDIALOG - PHIÊN BẢN NÂNG CẤP SỬ DỤNG ONHAPLIEUHIENDAI
 */
public class ThemSanPhamDialog extends JDialog {

    private boolean isSuccess = false;
    private SanPham sanPhamMoi;
    private File fileAnhChon;

    // --- UI Components sử dụng file có sẵn ---
    private JPanel pnlCard;
    private ONhapLieuHienDai txtMaSP, txtTenSP, txtGiaBan, txtDonVi; 
    private FormGroupCombo cbLoaiSP;
    private KhungAnh pnlKhungAnh;
    private ReactButton btnTao, btnHuy;

    // --- Dữ liệu ---
    private Map<String, String> mapLoaiSP = new LinkedHashMap<>();

    // --- Theme Colors (Đồng bộ với ONhapLieuHienDai) ---
    private Color bgDark = new Color(15, 23, 42);       
    private Color cardDark = new Color(30, 41, 59);     
    private Color borderFocus = new Color(59, 130, 246);
    private Color textPrimary = Color.WHITE;
    private Color textSecondary = new Color(203, 213, 225);

    private float opacity = 0f;
    private float scale = 0.95f;
    private BufferedImage blurredBackground;
    private Logic.SanPhamLogic spLogic = new Logic.SanPhamLogic();
    private boolean isFormatting = false;

    public ThemSanPhamDialog(Window parent) {
        super(parent, "Thêm Sản Phẩm Mới", ModalityType.APPLICATION_MODAL);
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
        loadDuLieuLoaiSP();

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
                g2.dispose();
            }
        };
        pnlCard.setOpaque(false);
        pnlCard.setPreferredSize(new Dimension(1100, 750)); // Tăng size tổng thể
        pnlCard.setBorder(new EmptyBorder(35, 45, 30, 45));

        JLabel lblTitle = new JLabel("Thêm sản phẩm mới");
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 30f)); // Tiêu đề lớn hơn
        lblTitle.setForeground(textPrimary);

        JPanel pnlBody = new JPanel(new GridLayout(1, 2, 50, 0));
        pnlBody.setOpaque(false);
        
        // --- CỘT TRÁI: KHUNG ẢNH ---
        JPanel pnlLeft = new JPanel(new BorderLayout(0, 25));
        pnlLeft.setOpaque(false);
        pnlKhungAnh = new KhungAnh();
        ReactButton btnChonAnh = new ReactButton("Chọn ảnh từ máy", new Color(51, 65, 85));
        btnChonAnh.addActionListener(e -> chonAnhTuMay());
        pnlLeft.add(pnlKhungAnh, BorderLayout.CENTER);
        pnlLeft.add(btnChonAnh, BorderLayout.SOUTH);

        // --- CỘT PHẢI: CÁC Ô NHẬP LIỆU ---
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
        pnlRight.setOpaque(false);

        // Khởi tạo sử dụng file ONhapLieuHienDai có sẵn
        txtMaSP = new ONhapLieuHienDai("Mã Sản Phẩm", false, false);
        txtTenSP = new ONhapLieuHienDai("Tên Sản Phẩm", true, false);
        cbLoaiSP = new FormGroupCombo("Loại Sản Phẩm");
        txtGiaBan = new ONhapLieuHienDai("Giá Bán (VNĐ)", true, false);
        txtDonVi = new ONhapLieuHienDai("Đơn Vị Tính", true, false);

        // Thêm vào panel với khoảng cách rộng rãi
        pnlRight.add(txtMaSP);    pnlRight.add(Box.createVerticalStrut(15));
        pnlRight.add(txtTenSP);   pnlRight.add(Box.createVerticalStrut(15));
        pnlRight.add(cbLoaiSP);   pnlRight.add(Box.createVerticalStrut(15));
        pnlRight.add(txtGiaBan);  pnlRight.add(Box.createVerticalStrut(15));
        pnlRight.add(txtDonVi);

        pnlBody.add(pnlLeft);
        pnlBody.add(pnlRight);

        // --- FOOTER ---
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        pnlBtns.setOpaque(false);
        btnHuy = new ReactButton("Hủy bỏ", new Color(71, 85, 105));
        btnHuy.addActionListener(e -> closeWithAnimation());
        btnTao = new ReactButton("+ Lưu Sản Phẩm", new Color(37, 99, 235));
        btnTao.addActionListener(e -> handleTaoSanPham());
        pnlBtns.add(btnHuy); pnlBtns.add(btnTao);

        pnlCard.add(lblTitle, BorderLayout.NORTH);
        pnlCard.add(pnlBody, BorderLayout.CENTER);
        pnlCard.add(pnlBtns, BorderLayout.SOUTH);
        rootPanel.add(pnlCard); 
    }

    private void setupAutoData() {
        txtMaSP.setText(TaoMaTuDongLogic.taoMaSanPham());
        txtDonVi.setText("Cái");
    }

    // --- CÁC COMPONENT PHỤ TRỢ ---

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
            g2.setColor(new Color(15, 23, 42)); // Background khung ảnh
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            if (img != null) {
                g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
            } else {
                g2.setColor(textSecondary);
                g2.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(18f));
                String hint = "Kéo & Thả ảnh vào đây";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, (getWidth()-fm.stringWidth(hint))/2, (getHeight()+fm.getAscent())/2 - 10);
            }
            g2.dispose();
        }
    }

    private class FormGroupCombo extends JPanel {
        private JComboBox<String> cb;
        private JLabel lblHeader;
        public FormGroupCombo(String label) {
            setLayout(new BorderLayout(0, 8));
            setOpaque(false);
            lblHeader = new JLabel(label);
            lblHeader.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 16f));
            lblHeader.setForeground(Color.WHITE);
            cb = new JComboBox<>();
            cb.setPreferredSize(new Dimension(0, 60)); // Cao 60px khớp với ONhapLieuHienDai
            cb.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(16f));
            cb.setUI(new BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton btn = new JButton("▼");
                    btn.setBorder(BorderFactory.createEmptyBorder());
                    btn.setContentAreaFilled(false);
                    btn.setForeground(textSecondary);
                    return btn;
                }
            });
            cb.setBackground(new Color(55, 60, 75)); // Màu nền khớp với file có sẵn
            cb.setForeground(Color.WHITE);
            add(lblHeader, BorderLayout.NORTH);
            add(cb, BorderLayout.CENTER);
        }
        public JComboBox<String> getCombo() { return cb; }
    }

    private class ReactButton extends JButton {
        public ReactButton(String text, Color bg) {
            super(text);
            setFont(TienIchGiaoDien.FONT_DAM.deriveFont(16f));
            setForeground(Color.WHITE);
            setBackground(bg);
            setPreferredSize(new Dimension(180, 55));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);        // ← Thêm dòng này
            setBorder(BorderFactory.createEmptyBorder()); // ← Thêm dòng này
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Hover effect
            Color bg = getModel().isRollover() ? getBackground().brighter() : getBackground();
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            
            // Vẽ chữ thủ công, KHÔNG gọi super.paintComponent
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
                if (isFormatting) return; // Nếu đang trong quá trình định dạng thì thoát
                
                SwingUtilities.invokeLater(() -> {
                    isFormatting = true;
                    String raw = txtGiaBan.getText().replaceAll("[^\\d]", "");
                    if (!raw.isEmpty()) {
                        try {
                            String formatted = new DecimalFormat("#,###").format(new BigDecimal(raw));
                            // Chỉ set text nếu giá trị hiển thị khác với giá trị định dạng mới
                            if (!txtGiaBan.getText().equals(formatted)) {
                                txtGiaBan.setText(formatted);
                            }
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

    private void loadDuLieuLoaiSP() {
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
                mapLoaiSP.keySet().forEach(cbLoaiSP.getCombo()::addItem);
            }
        }.execute();
    }

    private void chonAnhTuMay() {
        // Dùng FileDialog thay vì JFileChooser để ra cửa sổ native Windows
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chọn ảnh", FileDialog.LOAD);
        fd.setFile("*.jpg;*.png;*.jpeg");  // Lọc file ảnh
        fd.setFilenameFilter((dir, name) -> 
            name.toLowerCase().matches(".*\\.(jpg|png|jpeg)$")
        );
        fd.setVisible(true);
        
        String dir = fd.getDirectory();
        String file = fd.getFile();
        
        if (dir != null && file != null) {
            xuLyHienThiAnh(new File(dir + file));
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

    private void handleTaoSanPham() {
        if (txtTenSP.getText().trim().isEmpty()) { txtTenSP.setError("Không được để trống!"); return; }
        if (fileAnhChon == null) { TienIchGiaoDien.hienThiThongBao(this, "Vui lòng chọn ảnh!", "WARNING"); return; }
        
        try {
            String giaRaw = txtGiaBan.getText().replaceAll("[^\\d]", "");
            if (giaRaw.isEmpty()) giaRaw = "0";

            sanPhamMoi = new SanPham.ThoXaySanPham()
                    .ganMaSP(txtMaSP.getText().trim())
                    .ganTenSP(txtTenSP.getText().trim())
                    .ganMaLoai(mapLoaiSP.get(cbLoaiSP.getCombo().getSelectedItem()))
                    .ganGiaBan(new BigDecimal(giaRaw))
                    .ganDonViTinh(txtDonVi.getText().trim())
                    .ganLinkHinhAnh(fileAnhChon.getAbsolutePath())
                    .taoMoi();

            // 🔥 QUAN TRỌNG: Gọi tầng Logic để lưu vào DB
            spLogic.themSanPham(sanPhamMoi); 

            isSuccess = true;
            closeWithAnimation();
        } catch (Exception e) {
            // Nếu lỗi (trùng mã, trùng tên, mất kết nối), hiển thị thông báo ngay
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
    public SanPham getSanPhamMoi() { return sanPhamMoi; }
    public File getFileAnh() { return fileAnhChon; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame(); f.setSize(1200, 800); f.setLocationRelativeTo(null); f.setVisible(true);
            new ThemSanPhamDialog(f).setVisible(true);
        });
    }
}