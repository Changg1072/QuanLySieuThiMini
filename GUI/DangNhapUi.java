package GUI;

import Dao.ConnectDB;
import Dao.TaiKhoanDAO;
import GUI.HoTro.ONhapLieuHienDai;
import GUI.HoTro.TienIchGiaoDien;
import Logic.QuanLyKhoCacheLogic;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class DangNhapUi extends JFrame {

    private ONhapLieuHienDai txtTaiKhoan;
    private ONhapLieuHienDai txtMatKhau;
    private JButton btnDangNhap;
    private BufferedImage backgroundImage; 
    private JPanel pnlLoadingContainer;
    private JLabel lblThoat; // Đưa lblThoat lên đây để dễ quản lý
    private JPanel pnlBottomArea;
    public DangNhapUi() {
        try { ConnectDB.getInstance().getConnection(); } 
        catch (Exception e) { System.err.println("DB Error: " + e.getMessage()); }

        setTitle("Đăng Nhập");
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true); 

        JPanel panelMain = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (backgroundImage == null) {
                    try {
                        File file = new File("Images\\Gemini_Generated_Image_d517g8d517g8d517.png");
                        if (file.exists()) backgroundImage = ImageIO.read(file);
                    } catch (Exception e) {}
                }

                if (backgroundImage != null) {
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    GradientPaint gp = new GradientPaint(0, 0, new Color(20, 24, 33), getWidth(), getHeight(), new Color(46, 52, 64));
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        panelMain.setLayout(new GridBagLayout());
        add(panelMain);

        // KHUNG LỚN HƠN: Đã tăng lên 550x700
        JPanel panelCard = new JPanel();
        panelCard.setPreferredSize(new Dimension(550, 700));
        panelCard.setOpaque(false); 
        panelCard.setLayout(new GridBagLayout());

        GridBagConstraints gbcMain = new GridBagConstraints();
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.weightx = 1.0; 
        gbcMain.weighty = 1.0;
        gbcMain.anchor = GridBagConstraints.EAST; 
        gbcMain.insets = new Insets(0, 0, 0, 150); 
        panelMain.add(panelCard, gbcMain);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 45, 15, 45);

        // CHỮ TIÊU ĐỀ BỰ HƠN (42f)
        JLabel lblTitle = new JLabel("SIGN IN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Soviet Constructivism", Font.PLAIN, 80));
        lblTitle.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(40, 45, 30, 45); // Tăng khoảng cách dưới tiêu đề
        panelCard.add(lblTitle, gbc);

        // Ô NHẬP TÀI KHOẢN TO HƠN (450x95)
        gbc.insets = new Insets(0, 45, 5, 45);
        
        txtTaiKhoan = new ONhapLieuHienDai("Tên đăng nhập", true, false);
        txtTaiKhoan.setFont(new Font("Calibri", Font.PLAIN, 18));
        txtTaiKhoan.setPlaceholder("Nhập tên tài khoản...");
        txtTaiKhoan.setPreferredSize(new Dimension(450, 95));
        gbc.gridy = 1;
        panelCard.add(txtTaiKhoan, gbc);

        // Ô NHẬP MẬT KHẨU TO HƠN (450x95)
        txtMatKhau = new ONhapLieuHienDai("Mật khẩu", true, true);
        txtMatKhau.setFont(new Font("Calibri", Font.PLAIN, 18));
        txtMatKhau.setPlaceholder("Nhập mật khẩu...");
        txtMatKhau.setPreferredSize(new Dimension(450, 95));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 45, 20, 45);
        panelCard.add(txtMatKhau, gbc);

        // NÚT BẤM TO HƠN (450x65)
        btnDangNhap = TienIchGiaoDien.taoNutHienDai("Sign In", new Color(50, 50, 50));
        btnDangNhap.setFont(new Font("Soviet Constructivism", Font.PLAIN, 24)); // Chữ nút to lên // Chữ nút to lên
        btnDangNhap.setForeground(Color.WHITE);
        btnDangNhap.setPreferredSize(new Dimension(450, 65));
        gbc.gridy = 3;
        gbc.insets = new Insets(30, 45, 20, 45);
        panelCard.add(btnDangNhap, gbc);

        // --- 1. NÚT THOÁT ỨNG DỤNG ---
        // --- 1. NÚT THOÁT ỨNG DỤNG ---
        lblThoat = new JLabel("Thoát ứng dụng", SwingConstants.CENTER);
        lblThoat.setFont(new Font("Calibri", Font.ITALIC, 18)); 
        lblThoat.setForeground(new Color(200, 200, 200));
        lblThoat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblThoat.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { if(lblThoat.isEnabled()) System.exit(0); }
            @Override
            public void mouseEntered(MouseEvent e) { if(lblThoat.isEnabled()) lblThoat.setForeground(Color.WHITE); }
            @Override
            public void mouseExited(MouseEvent e) { if(lblThoat.isEnabled()) lblThoat.setForeground(new Color(200, 200, 200)); }
        });
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 45, 5, 45); // Ép sát vào thanh loading bên dưới
        panelCard.add(lblThoat, gbc);

        // --- 2. KHU VỰC DƯỚI CÙNG (DÙNG CARDLAYOUT ĐỂ CHỐNG GIẬT MÀN HÌNH) ---
        pnlBottomArea = new JPanel(new CardLayout());
        pnlBottomArea.setOpaque(false);
        
        // Card 1: Lúc bình thường (Rỗng, nhưng vẫn chiếm không gian 40px)
        JPanel pnlEmpty = new JPanel();
        pnlEmpty.setOpaque(false);
        pnlEmpty.setPreferredSize(new Dimension(450, 40));

        // Card 2: Lúc Loading (Thanh mảnh, không thô)
        JPanel pnlLoading = new JPanel();
        pnlLoading.setLayout(new BoxLayout(pnlLoading, BoxLayout.Y_AXIS));
        pnlLoading.setOpaque(false);

        JLabel lblLoadingText = new JLabel("HỆ THỐNG ĐANG XỬ LÝ...", SwingConstants.CENTER);
        lblLoadingText.setFont(new Font("Calibri", Font.ITALIC, 12));
        lblLoadingText.setForeground(new Color(130, 130, 130)); // Xám mờ chìm vào nền
        lblLoadingText.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar progressBarInline = new JProgressBar();
        progressBarInline.setIndeterminate(true);
        progressBarInline.setPreferredSize(new Dimension(280, 3)); // THANH SIÊU MỎNG 3px
        progressBarInline.setMaximumSize(new Dimension(280, 3));
        progressBarInline.setBorder(null); // Xóa viền xấu xí
        progressBarInline.setBackground(new Color(40, 45, 55, 100)); // Nền tệp với form

        // Trổ tài Front-end: Vẽ tia sáng Neon chạy qua cực mượt
        progressBarInline.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintIndeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth();
                int h = c.getHeight();
                
                // Vẽ nền thanh
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, w, h, h, h);

                // Cục sáng chạy qua (Màu Cyan chuẩn thiết kế tương lai)
                g2.setColor(new Color(0, 255, 204));
                long time = System.currentTimeMillis() / 4; 
                int x = (int) (time % (w + 120)) - 120;
                g2.fillRoundRect(x, 0, 80, h, h, h); // Vệt sáng dài 80px
                
                g2.dispose();
                c.repaint();
            }
        });

        pnlLoading.add(Box.createVerticalStrut(5));
        pnlLoading.add(lblLoadingText);
        pnlLoading.add(Box.createVerticalStrut(5));
        pnlLoading.add(progressBarInline);

        // Nhét 2 trạng thái vào CardLayout
        pnlBottomArea.add(pnlEmpty, "EMPTY");
        pnlBottomArea.add(pnlLoading, "LOADING");

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 45, 30, 45); 
        panelCard.add(pnlBottomArea, gbc);

        xuLySuKien();
    }

    // =========================================================
    // HÀM KHÓA GIAO DIỆN VÀ CHUYỂN ĐỔI TRẠNG THÁI LOADING
    // =========================================================
    private void setTrangThaiForm(boolean isKichHoat) {
        txtTaiKhoan.setEditable(isKichHoat);
        txtMatKhau.setEditable(isKichHoat);
        btnDangNhap.setEnabled(isKichHoat);
        lblThoat.setEnabled(isKichHoat);
        
        // Tuyệt chiêu: Đổi CardLayout thay vì setVisible (Chống nhảy Layout)
        CardLayout cl = (CardLayout) pnlBottomArea.getLayout();
        if (isKichHoat) {
            cl.show(pnlBottomArea, "EMPTY");
        } else {
            cl.show(pnlBottomArea, "LOADING");
        }
    }

    private void xuLySuKien() {
        Runnable thucHienDangNhap = () -> {
            txtTaiKhoan.clearError();
            txtMatKhau.clearError();
            String inputDN = txtTaiKhoan.getText().trim();
            String matKhau = txtMatKhau.getText();
            
            if (inputDN.isEmpty()) { txtTaiKhoan.setError("Trống tên!"); return; }
            if (matKhau.isEmpty()) { txtMatKhau.setError("Trống mật khẩu!"); return; }

            // KHÓA FORM, BẬT LOADING BÊN DƯỚI NÚT THOÁT
            setTrangThaiForm(false);

            // BẮT ĐẦU CHẠY LUỒNG NGẦM (BACKGROUND WORKER)
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                private boolean isSuccess = false;
                private String[] ketQua = null;
                private java.util.List<Data.ChiaCa> dsThieu = null;
                
                private String tenNhanVienTảiTrước = "";
                private java.util.List<Data.LoaiSP> dsLoaiSPTảiTrước = null;
                private Dao.TruyVanSieuTocDAO.DuLieuBanHangDTO banHangCache = null;
                private Dao.TruyVanSieuTocDAO.DuLieuDonHangDTO donHangCache = null;

                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        Logic.TaiKhoanLogic tkLogic = new Logic.TaiKhoanLogic();
                        // Hàm xuLyDangNhap() của Logic sẽ TỰ ĐỘNG băm cái matKhau ra rồi mới gửi xuống DAO
                        ketQua = tkLogic.xuLyDangNhap(inputDN, matKhau); 
                    } catch (Exception logicEx) {
                        // Bắt lỗi nếu tài khoản/mật khẩu bị sai (Logic ném Exception)
                        ketQua = null; 
                    }
                    if (ketQua != null) {
                        isSuccess = true;
                        String maNV = ketQua[0];   
                        String vaiTro = ketQua[1];
                        boolean isAdmin = vaiTro.toUpperCase().contains("ADMIN") 
                            || vaiTro.equalsIgnoreCase("Quản lý");
                        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
                        Logic.ChiaCaLogic ccLogic = new Logic.ChiaCaLogic();

                        try { ccLogic.kiemTraVaTuDongKetCa(); } 
                        catch (Exception ex) { System.err.println("Lỗi auto kết ca: " + ex.getMessage()); }

                        dsThieu = ccLogic.layDanhSachChuaCheckInCaHienTai(maNV, isAdmin);
                        Data.NhanVien nv = Dao.NhanVienDAO.getInstance().layNhanVienTheoMa(maNV);
                        tenNhanVienTảiTrước = (nv != null) ? nv.getHoTen() : maNV;
                        
                        if (vaiTro.toUpperCase().contains("ADMIN") || vaiTro.equalsIgnoreCase("Quản lý")) {
                            
                            // ==========================================================
                            // 🚀 KHAI TRIỂN ĐA LUỒNG SONG SONG (ÉP XUNG TỐC ĐỘ X3)
                            // ==========================================================
                            java.util.concurrent.CompletableFuture<Void> task1 = java.util.concurrent.CompletableFuture.runAsync(() -> {
                                dsLoaiSPTảiTrước = new Logic.LoaiSPLogic().layDanhSachLoaiSP();
                            });
                            
                            java.util.concurrent.CompletableFuture<Void> task2 = java.util.concurrent.CompletableFuture.runAsync(() -> {
                                banHangCache = Dao.TruyVanSieuTocDAO.getInstance().loadToanBoSanPhamBanHang();
                            });
                            
                            java.util.concurrent.CompletableFuture<Void> task3 = java.util.concurrent.CompletableFuture.runAsync(() -> {
                                donHangCache = Dao.TruyVanSieuTocDAO.getInstance().loadToanBoDuLieuDonHang();
                            });

                            // 🛑 Đợi cả 3 luồng cùng chạy xong (Tốc độ sẽ bằng luồng nào chạy lâu nhất, không bị cộng dồn!)
                            java.util.concurrent.CompletableFuture.allOf(task1, task2, task3).join();
                            // ==========================================================
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); 
                        System.out.println("DEBUG dsThieu size: " + (dsThieu == null ? "NULL" : dsThieu.size()));
                        if (isSuccess) {
                            if (dsThieu != null && !dsThieu.isEmpty()) {
                                javax.swing.Timer delayTimer = new javax.swing.Timer(800, e -> {
                                    GUI.HoTro.ThongBaoDiemDanh.hienThi(dsThieu); // ← activeWindow lúc này vẫn là DangNhapUi!
                                });
                                delayTimer.setRepeats(false);
                                delayTimer.start();
                            }

                            String maNV = ketQua[0];
                            String vaiTro = ketQua[1]; 
                            
                            if (vaiTro.toUpperCase().contains("ADMIN") || vaiTro.equalsIgnoreCase("Quản lý")) {
                                
                                // Bật Admin lên đè vào Đăng Nhập
                                TrangADMIN trangAdmin = new TrangADMIN(maNV, tenNhanVienTảiTrước, dsLoaiSPTảiTrước, banHangCache, donHangCache);
                                trangAdmin.setVisible(true); 
                                
                                // Hủy Đăng Nhập ngầm phía sau (Chống chớp đen Desktop)
                                javax.swing.Timer timer = new javax.swing.Timer(500, evt -> {
                                    DangNhapUi.this.dispose();
                                });
                                timer.setRepeats(false);
                                timer.start();
                                
                            } else {
                                TrangThuNgan trangThuNgan = new TrangThuNgan(maNV);
                                trangThuNgan.setVisible(true);
                                
                                javax.swing.Timer timer = new javax.swing.Timer(500, evt -> {
                                    DangNhapUi.this.dispose();
                                });
                                timer.setRepeats(false);
                                timer.start();
                            }
                        } else {
                            setTrangThaiForm(true); 
                            TienIchGiaoDien.hienThiThongBao(DangNhapUi.this, "Sai tài khoản hoặc mật khẩu!", "ERROR");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        setTrangThaiForm(true); 
                        TienIchGiaoDien.hienThiThongBao(DangNhapUi.this, "Lỗi kết nối máy chủ!", "ERROR");
                    }
                }
            };
            worker.execute();
        };

        btnDangNhap.addActionListener(e -> thucHienDangNhap.run());
        txtTaiKhoan.getField().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) txtMatKhau.requestFocusField(); }
        });
        txtMatKhau.getField().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) thucHienDangNhap.run(); }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DangNhapUi().setVisible(true));
    }
}