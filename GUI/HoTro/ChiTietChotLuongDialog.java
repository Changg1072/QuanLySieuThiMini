package GUI.HoTro;

import Logic.BangLuongLogic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.text.DecimalFormat;

public class ChiTietChotLuongDialog extends JDialog {

    private boolean isSuccess = false;

    // --- DỮ LIỆU ĐẦU VÀO ---
    private String maNV;
    private String thangNam;
    private BigDecimal luongCoBan;
    private BigDecimal heSoOT;
    private double gioLam;
    private double gioOT;
    private BigDecimal phatHeThong;
    
    // --- BIẾN TÍNH TOÁN REALTIME ---
    private BigDecimal tamTinh = BigDecimal.ZERO;
    private BigDecimal tongThucNhan = BigDecimal.ZERO;

    // --- MÀU SẮC CHỦ ĐẠO (Đồng bộ chuẩn 100% Mockup) ---
    private final Color COLOR_BG = new Color(30, 37, 50);        // Nền tối chính
    private final Color COLOR_CARD = new Color(42, 52, 69);      // Nền các thẻ con
    private final Color COLOR_TEXT_PRIMARY = new Color(248, 250, 252);
    private final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);
    private final Color COLOR_GREEN = new Color(16, 185, 129);   // Xanh lá (Thưởng)
    private final Color COLOR_RED = new Color(239, 68, 68);      // Đỏ (Phạt)
    private final Color COLOR_BTN_BLUE = new Color(37, 99, 235); // Nút xác nhận

    // --- UI COMPONENTS ---
    private ThongSoRow lblTamTinh;
    private JLabel lblTongThucNhan;
    private InputTien txtThuong, txtPhat;

    private DecimalFormat df = new DecimalFormat("#,###");

    /**
     * CONSTRUCTOR NHẬN DỮ LIỆU TỪ BẢNG LƯƠNG UI
     */
    public ChiTietChotLuongDialog(Window parent, String maNV, String tenNV, String chucVu, 
                                  BigDecimal luongCoBan, double gioLam, double gioOT, 
                                  BigDecimal heSoOT, BigDecimal phatHeThong, int soLanTre, String thangNam) {
        super(parent, "Chi tiết Chốt Lương", ModalityType.APPLICATION_MODAL);
        
        this.maNV = maNV;
        this.thangNam = thangNam;
        this.luongCoBan = (luongCoBan != null) ? luongCoBan : BigDecimal.ZERO;
        this.heSoOT = (heSoOT != null) ? heSoOT : new BigDecimal("1.5");
        this.gioLam = gioLam;
        this.gioOT = gioOT;
        this.phatHeThong = (phatHeThong != null) ? phatHeThong : BigDecimal.ZERO;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(850, 600);
        setLocationRelativeTo(parent);

        tinhToanGiaTriGoc();
        initUI(tenNV, chucVu, soLanTre);
        setupRealtimeCalculation();
    }

    private void tinhToanGiaTriGoc() {
        // Tạm tính = (Lương CB * Giờ) + (Lương CB * Hệ số OT * Giờ OT) - Phạt Hệ Thống
        BigDecimal tienGio = luongCoBan.multiply(BigDecimal.valueOf(gioLam));
        BigDecimal tienOT = luongCoBan.multiply(heSoOT).multiply(BigDecimal.valueOf(gioOT));
        tamTinh = tienGio.add(tienOT).subtract(phatHeThong);
        if (tamTinh.compareTo(BigDecimal.ZERO) < 0) tamTinh = BigDecimal.ZERO;
        tongThucNhan = tamTinh;
    }

    private void initUI(String tenNV, String chucVu, int soLanTre) {
        // MAIN PANEL BỌC BÊN NGOÀI
        JPanel pnlMain = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
                g2.setColor(new Color(255, 255, 255, 20)); // Viền mỏng sáng nhẹ
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 24, 24));
                g2.dispose();
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(20, 25, 20, 25));

        // 1. HEADER
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(new EmptyBorder(0, 0, 20, 0));
        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pnlTitle.setOpaque(false);
        JLabel lblIcon = new JLabel("📄");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        lblIcon.setForeground(COLOR_TEXT_PRIMARY);
        
        JButton btnClose = new JButton("✖");
        btnClose.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnClose.setForeground(COLOR_TEXT_MUTED);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        
        pnlHeader.add(lblIcon, BorderLayout.WEST);
        pnlHeader.add(btnClose, BorderLayout.EAST);
        pnlMain.add(pnlHeader, BorderLayout.NORTH);

        // 2. BODY CỘT TRÁI (THÔNG TIN) VÀ CỘT PHẢI (ĐIỀU CHỈNH)
        JPanel pnlBody = new JPanel(new GridLayout(1, 2, 35, 0));
        pnlBody.setOpaque(false);
        
        pnlBody.add(createLeftColumn(tenNV, chucVu, soLanTre));
        pnlBody.add(createRightColumn());
        
        pnlMain.add(pnlBody, BorderLayout.CENTER);

        // 3. FOOTER (NÚT BẤM)
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.setBorder(new EmptyBorder(25, 0, 0, 0));
        
        JButton btnHuy = new JButton("Hủy bỏ");
        btnHuy.setForeground(COLOR_TEXT_MUTED);
        btnHuy.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnHuy.setContentAreaFilled(false);
        btnHuy.setBorderPainted(false);
        btnHuy.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHuy.addActionListener(e -> dispose());
        
        JButton btnChot = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_BTN_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
                g2.dispose();
            }
        };

        // Tạo panel nội dung bên trong nút
        JPanel pnlBtnContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        pnlBtnContent.setOpaque(false);

        JLabel lblCheck = new JLabel("✓");
        lblCheck.setFont(new Font("Segoe UI Emoji", Font.BOLD, 15));
        lblCheck.setForeground(Color.WHITE);

        JLabel lblBtnText = new JLabel("Xác nhận Chốt Lương");
        lblBtnText.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblBtnText.setForeground(Color.WHITE);

        pnlBtnContent.add(lblCheck);
        pnlBtnContent.add(lblBtnText);

        btnChot.setLayout(new GridBagLayout()); // thay BorderLayout
        btnChot.add(pnlBtnContent); 
        btnChot.setPreferredSize(new Dimension(220, 48));
        btnChot.setContentAreaFilled(false);
        btnChot.setBorderPainted(false);
        btnChot.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChot.addActionListener(e -> thucHienChotLuong());
        
        pnlFooter.add(btnHuy, BorderLayout.WEST);
        pnlFooter.add(btnChot, BorderLayout.EAST);
        pnlMain.add(pnlFooter, BorderLayout.SOUTH);

        setContentPane(pnlMain);
    }

    // =========================================================
    // CỘT TRÁI: DỮ LIỆU CỐ ĐỊNH TỪ HỆ THỐNG
    // =========================================================
    private JPanel createLeftColumn(String tenNV, String chucVu, int soLanTre) {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setOpaque(false);

        // --- Profile Card ---
        JPanel pnlProfile = new JPanel(new BorderLayout(15, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        };
        pnlProfile.setOpaque(false);
        pnlProfile.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // 🚀 ĐÃ SỬA: Nới rộng không gian cho Card để tránh bị ép sát co rúm lại
        pnlProfile.setPreferredSize(new Dimension(0, 90));
        pnlProfile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel lblAvatar = new JLabel("👤", SwingConstants.CENTER);
        lblAvatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        
        JPanel pnlInfo = new JPanel(new GridLayout(3, 1));
        pnlInfo.setOpaque(false);
        
        JLabel lMa = new JLabel(maNV);
        lMa.setForeground(COLOR_TEXT_MUTED); lMa.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JLabel lTen = new JLabel(tenNV);
        lTen.setForeground(COLOR_TEXT_PRIMARY); lTen.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        JLabel lChucVu = new JLabel(chucVu);
        lChucVu.setForeground(COLOR_TEXT_MUTED); lChucVu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        pnlInfo.add(lMa); pnlInfo.add(lTen); pnlInfo.add(lChucVu);
        pnlProfile.add(lblAvatar, BorderLayout.WEST);
        pnlProfile.add(pnlInfo, BorderLayout.CENTER);

        // --- Danh sách thông số ---
        pnl.add(pnlProfile);
        pnl.add(Box.createRigidArea(new Dimension(0, 25)));

        pnl.add(new ThongSoRow("Lương cơ bản", df.format(luongCoBan) + " VNĐ/h", COLOR_TEXT_PRIMARY, false));
        pnl.add(Box.createRigidArea(new Dimension(0, 15)));
        
        pnl.add(new ThongSoRow("Giờ làm hệ thống", gioLam + "h", COLOR_TEXT_PRIMARY, false));
        pnl.add(Box.createRigidArea(new Dimension(0, 15)));
        
        pnl.add(new ThongSoRow("Tăng ca (OT) x" + heSoOT, gioLam > 0 && gioOT == 0 ? "0h" : gioOT + "h", COLOR_TEXT_PRIMARY, false));
        pnl.add(Box.createRigidArea(new Dimension(0, 15)));
        
        String phatStr = phatHeThong.compareTo(BigDecimal.ZERO) > 0 ? "-" + df.format(phatHeThong) + " VNĐ" : "0 VNĐ";
        
        // 🚀 ĐÃ SỬA: Đổi tên thành "Phạt hệ thống" cho chuẩn
        pnl.add(new ThongSoRow("Phạt hệ thống", phatStr, COLOR_RED, false));
        pnl.add(Box.createRigidArea(new Dimension(0, 5))); // Thu hẹp khoảng cách để nhét thêm dòng giải thích

        // =======================================================
        // 🚀 BẮT ĐẦU PHẦN GIẢI THÍCH CHI TIẾT "AUTO TƯ BẢN"
        // =======================================================
        int thang = 0, nam = 0;
        try {
            thang = Integer.parseInt(thangNam.split("/")[0]);
            nam = Integer.parseInt(thangNam.split("/")[1]);
        } catch (Exception e) {}
        
        int soLanNghi = 0;
        int soLanTreThucTe = 0;
        long soPhutTre = 0;
        
        try {
            Logic.BangLuongLogic.ChiTietKhauTru chiTiet = new Logic.BangLuongLogic().tinhChiTietKhauTru(maNV, thang, nam);
            soLanNghi = chiTiet.soLanNghi;
            soLanTreThucTe = chiTiet.soLanTre;
            soPhutTre = chiTiet.tongPhutTre;
        } catch (Exception e) {}

        // 🚀 ĐÃ SỬA: Bọc cả 3 dòng chữ vào 1 Panel dùng GridLayout để ép lề trái tuyệt đối
        JPanel pnlGiaiThich = new JPanel(new GridLayout(3, 1, 0, 3));
        pnlGiaiThich.setOpaque(false);

        JLabel lblNghi = new JLabel("Vắng mặt: Trừ 200k/ca. (Đã vắng " + soLanNghi + " ca)");
        lblNghi.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblNghi.setForeground(COLOR_TEXT_MUTED);
        pnlGiaiThich.add(lblNghi);

        JLabel lblTre = new JLabel("Đi trễ: >5p (-20k) | >15p (-50k) | >30p (-100k)");
        lblTre.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblTre.setForeground(COLOR_TEXT_MUTED);
        pnlGiaiThich.add(lblTre);

        JLabel lblTre2 = new JLabel("-> Vi phạm: " + soLanTreThucTe + " lần đi trễ (Tổng " + soPhutTre + " phút).");
        lblTre2.setFont(new Font("Segoe UI", Font.BOLD | Font.ITALIC, 12));
        lblTre2.setForeground(new Color(239, 68, 68)); 
        pnlGiaiThich.add(lblTre2);
        
        // Thêm khối giải thích đã căn lề vào lề trái của Box Layout chính
        pnl.add(pnlGiaiThich);
        pnl.add(Box.createRigidArea(new Dimension(0, 20)));
        // =======================================================

        // --- Kẻ ngang nét đứt ---
        JPanel dashedLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(COLOR_TEXT_MUTED);
                Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
                g2.setStroke(dashed);
                g2.drawLine(0, 1, getWidth(), 1); // 🚀 ĐÃ SỬA: Vẽ bám mép trên, không bị đè giữa
            }
        };
        dashedLine.setOpaque(false);
        // 🚀 ĐÃ SỬA: Ép cứng height=2px để đường nét đứt không lấn chiếm không gian
        dashedLine.setPreferredSize(new Dimension(0, 2));
        dashedLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        pnl.add(dashedLine);
        pnl.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- Tạm tính ---
        lblTamTinh = new ThongSoRow("TẠM TÍNH HỆ THỐNG", df.format(tamTinh) + " VNĐ", COLOR_TEXT_PRIMARY, true);
        pnl.add(lblTamTinh);

        return pnl;
    }

    // =========================================================
    // CỘT PHẢI: ĐIỀU CHỈNH THỦ CÔNG
    // =========================================================
    private JPanel createRightColumn() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setOpaque(false);

        JLabel lblTitle = new JLabel("ĐIỀU CHỈNH THỦ CÔNG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(COLOR_TEXT_MUTED);
        pnl.add(lblTitle);
        pnl.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- Input Thưởng ---
        JLabel lblThuong = new JLabel("Thưởng thêm");
        lblThuong.setForeground(COLOR_TEXT_PRIMARY); lblThuong.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pnl.add(lblThuong);
        
        txtThuong = new InputTien(true);
        pnl.add(Box.createRigidArea(new Dimension(0, 5)));
        pnl.add(txtThuong);
        
        JLabel lblGhiChuT = new JLabel("Thưởng hiệu suất tháng hoặc lễ tết");
        lblGhiChuT.setFont(new Font("Segoe UI", Font.ITALIC, 11)); lblGhiChuT.setForeground(COLOR_TEXT_MUTED);
        pnl.add(Box.createRigidArea(new Dimension(0, 3)));
        pnl.add(lblGhiChuT);
        
        pnl.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Input Phạt ---
        JLabel lblPhat = new JLabel("Phạt thêm");
        lblPhat.setForeground(COLOR_TEXT_PRIMARY); lblPhat.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pnl.add(lblPhat);
        
        txtPhat = new InputTien(false);
        pnl.add(Box.createRigidArea(new Dimension(0, 5)));
        pnl.add(txtPhat);
        
        JLabel lblGhiChuP = new JLabel("Các khoản khấu trừ ngoài hệ thống");
        lblGhiChuP.setFont(new Font("Segoe UI", Font.ITALIC, 11)); lblGhiChuP.setForeground(COLOR_TEXT_MUTED);
        pnl.add(Box.createRigidArea(new Dimension(0, 3)));
        pnl.add(lblGhiChuP);

        pnl.add(Box.createVerticalGlue());

        // --- BẢNG TỔNG THỰC NHẬN ---
        JPanel pnlTong = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42)); // Tối thẳm
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Vạch sáng màu xanh lá ở đáy
                g2.setColor(COLOR_GREEN);
                g2.fillRoundRect(20, getHeight() - 6, getWidth() - 40, 3, 3, 3);
                g2.dispose();
            }
        };
        pnlTong.setOpaque(false);
        pnlTong.setBorder(new EmptyBorder(25, 20, 30, 20));
        pnlTong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel lTongTitle = new JLabel("TỔNG THỰC NHẬN", SwingConstants.CENTER);
        lTongTitle.setForeground(COLOR_TEXT_MUTED);
        lTongTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        lblTongThucNhan = new JLabel(df.format(tongThucNhan) + " VNĐ", SwingConstants.CENTER);
        lblTongThucNhan.setForeground(COLOR_GREEN);
        lblTongThucNhan.setFont(new Font("Segoe UI", Font.BOLD, 32));

        pnlTong.add(lTongTitle, BorderLayout.NORTH);
        pnlTong.add(lblTongThucNhan, BorderLayout.CENTER);
        
        pnl.add(pnlTong);

        return pnl;
    }

    // =========================================================
    // LOGIC TÍNH TOÁN REALTIME KHI GÕ
    // =========================================================
    private void setupRealtimeCalculation() {
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calc(); }
            public void removeUpdate(DocumentEvent e) { calc(); }
            public void changedUpdate(DocumentEvent e) { calc(); }
        };
        txtThuong.txtSo.getDocument().addDocumentListener(dl);
        txtPhat.txtSo.getDocument().addDocumentListener(dl);
    }

    private void calc() {
        BigDecimal thuong = txtThuong.getGiaTri();
        BigDecimal phat = txtPhat.getGiaTri();
        
        tongThucNhan = tamTinh.add(thuong).subtract(phat);
        if (tongThucNhan.compareTo(BigDecimal.ZERO) < 0) tongThucNhan = BigDecimal.ZERO;
        
        lblTongThucNhan.setText(df.format(tongThucNhan) + " VNĐ");
    }

    // =========================================================
    // NÚT CHỐT LƯƠNG
    // =========================================================
        // =========================================================
    // NÚT CHỐT LƯƠNG
    // =========================================================
    private void thucHienChotLuong() {
        try {
            BangLuongLogic logic = new BangLuongLogic();
            
            BigDecimal thuong = txtThuong.getGiaTri();
            BigDecimal phatThuCong = txtPhat.getGiaTri();
            
            BigDecimal tongKhauTru = phatHeThong.add(phatThuCong);

            logic.chotVaLuuBangLuong(maNV, thangNam, BigDecimal.valueOf(gioLam), BigDecimal.valueOf(gioOT), thuong, tongKhauTru);
            
            isSuccess = true;
            dispose();
            
        } catch (Exception ex) {
            // 🚀 ĐÃ SỬA: Bỏ JOptionPane mặc định, gọi giao diện thông báo xịn xò của project
            TienIchGiaoDien.hienThiThongBao(this, "Lỗi khi chốt lương: " + ex.getMessage(), "ERROR");
        }
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    // =========================================================
    // COMPONENT HỖ TRỢ: ROW HIỂN THỊ TEXT 2 BÊN
    // =========================================================
    private class ThongSoRow extends JPanel {
        JLabel lblValue;
        public ThongSoRow(String leftText, String rightText, Color rightColor, boolean isBold) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

            JLabel lblKey = new JLabel(leftText);
            lblKey.setForeground(COLOR_TEXT_MUTED);
            lblKey.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 14));

            lblValue = new JLabel(rightText);
            lblValue.setForeground(rightColor);
            lblValue.setFont(new Font("Segoe UI", Font.BOLD, 15));

            add(lblKey, BorderLayout.WEST);
            add(lblValue, BorderLayout.EAST);
        }
    }

        // =========================================================
    // COMPONENT HỖ TRỢ: Ô NHẬP TIỀN CÓ ICON (+ / -)
    // =========================================================
    private class InputTien extends JPanel {
        JTextField txtSo;
        
        public InputTien(boolean laThuong) {
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(5, 15, 5, 15));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setOpaque(false);

            // ĐÃ SỬA: Đổi sang dấu + và - cơ bản, dùng font Arial để máy nào cũng đọc được
            JLabel lblIcon = new JLabel(laThuong ? "+" : "-");
            lblIcon.setFont(new Font("Arial", Font.BOLD, 28));
            lblIcon.setForeground(laThuong ? COLOR_GREEN : COLOR_RED);
            
            txtSo = new JTextField();
            txtSo.setOpaque(false);
            txtSo.setBorder(null);
            txtSo.setForeground(COLOR_TEXT_PRIMARY);
            txtSo.setFont(new Font("Segoe UI", Font.BOLD, 18));
            txtSo.setCaretColor(Color.WHITE);
            
            JLabel lblSuffix = new JLabel("VNĐ");
            lblSuffix.setForeground(COLOR_TEXT_MUTED);
            lblSuffix.setFont(new Font("Segoe UI", Font.BOLD, 13));

            add(lblIcon, BorderLayout.WEST);
            add(txtSo, BorderLayout.CENTER);
            add(lblSuffix, BorderLayout.EAST);

            // Auto format dấu phẩy khi gõ xong
            txtSo.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) {
                    BigDecimal val = getGiaTri();
                    if (val.compareTo(BigDecimal.ZERO) > 0) {
                        txtSo.setText(df.format(val));
                    } else {
                        txtSo.setText("");
                    }
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLOR_CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            if (txtSo.isFocusOwner()) {
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        public BigDecimal getGiaTri() {
            try {
                String raw = txtSo.getText().replaceAll("[^\\d]", "");
                if (raw.isEmpty()) return BigDecimal.ZERO;
                return new BigDecimal(raw);
            } catch (Exception e) { return BigDecimal.ZERO; }
        }
    }
}