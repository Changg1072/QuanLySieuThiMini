package GUI;

import GUI.HoTro.*;
import Logic.NhanVienLogic;
import Logic.TaiKhoanLogic;
import Data.NhanVien;
import Data.TaiKhoan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class TaiKhoanUi extends JPanel {

    // ================== BIẾN LƯU TRỮ ==================
    private String maNVHienTai;
    private NhanVienLogic nvLogic;
    private TaiKhoanLogic tkLogic;

    // ================== COMPONENTS ==================
    private ONhapLieuHienDai txtHoTen, txtSdt, txtLuongGio, txtNgayVaoLam, txtNgayNghiViec;
    private JComboBox<String> cbChucVu;
    private TienIchGiaoDien.NutGat tglTrangThai; 
    private JLabel lblTrangThaiText;
    
    private NutBoGoc btnDoiMatKhau;
    private JButton btnHuyBo, btnCapNhat;

    // ================== FONTS & COLORS ==================
    private final Font FONT_CALIBRI_BOLD = new Font("Calibri", Font.BOLD, 18); 
    private final Font FONT_CALIBRI_PLAIN = new Font("Calibri", Font.PLAIN, 16); 
    
    private final Color COLOR_BG_LIGHT = new Color(240, 242, 245); 
    private final Color COLOR_CARD_DARK = new Color(85, 95, 110); 
    
    // ================== CONSTRUCTOR ==================
    public TaiKhoanUi(String maNV) {
        this.maNVHienTai = maNV;
        this.nvLogic = new NhanVienLogic();
        this.tkLogic = new TaiKhoanLogic();
        
        setLayout(new BorderLayout());
        initComponents();
        
        // Gọi hàm load dữ liệu TỪ DATABASE
        loadThongTinNhanVien();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel cardChinh = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD_DARK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cardChinh.setOpaque(false);
        cardChinh.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.weightx = 1.0;

        // ================= KHỞI TẠO CÁC TRƯỜNG =================
        txtHoTen = new ONhapLieuHienDai("Họ và tên *", true, false);
        txtSdt = new ONhapLieuHienDai("Số điện thoại", true, false);
        txtLuongGio = new ONhapLieuHienDai("Lương Giờ (Cố định)", false, false);
        txtNgayVaoLam = new ONhapLieuHienDai("Ngày vào làm", false, false);
        txtNgayNghiViec = new ONhapLieuHienDai("Ngày nghỉ việc (Trống nếu đang làm)", false, false);

        setFontChoONhapLieu(txtHoTen);
        setFontChoONhapLieu(txtSdt);
        setFontChoONhapLieu(txtLuongGio);
        setFontChoONhapLieu(txtNgayVaoLam);
        setFontChoONhapLieu(txtNgayNghiViec);

        fixMauChuChoONhapLieu(txtLuongGio);
        fixMauChuChoONhapLieu(txtNgayVaoLam);
        fixMauChuChoONhapLieu(txtNgayNghiViec);

        cbChucVu = new JComboBox<>(new String[]{"ADMIN", "Thu Ngân"});
        TienIchGiaoDien.trangTriComboBox(cbChucVu);
        cbChucVu.setFont(FONT_CALIBRI_PLAIN); 
        cbChucVu.setEnabled(false); // Nhân viên tự đổi thông tin không được tự tăng chức

        tglTrangThai = new TienIchGiaoDien.NutGat();
        tglTrangThai.setSelected(true); 
        tglTrangThai.setEnabled(false); 
        
        lblTrangThaiText = new JLabel("Đang làm việc");
        lblTrangThaiText.setFont(FONT_CALIBRI_PLAIN); 
        lblTrangThaiText.setForeground(Color.WHITE);

        btnDoiMatKhau = new NutBoGoc("ĐỔI MẬT KHẨU");
        btnDoiMatKhau.setColorBackground(new Color(110, 125, 145)); 
        btnDoiMatKhau.setFont(FONT_CALIBRI_BOLD);
        btnDoiMatKhau.addActionListener(e -> hienThiDialogDoiMatKhau());

        btnHuyBo = TienIchGiaoDien.taoNutHienDai("HỦY BỎ", new Color(148, 163, 184));
        btnHuyBo.setFont(FONT_CALIBRI_BOLD);
        btnHuyBo.addActionListener(e -> loadThongTinNhanVien()); // Hủy thì load lại data cũ
        
        btnCapNhat = TienIchGiaoDien.taoNutHienDai("CẬP NHẬT", new Color(16, 185, 129)); 
        btnCapNhat.setFont(FONT_CALIBRI_BOLD);
        btnCapNhat.addActionListener(e -> capNhatThongTinNhanVien()); // Gọi hàm Update

        // ================= SẮP XẾP BỐ CỤC =================
        int row = 0;

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        cardChinh.add(txtHoTen, gbc);

        gbc.gridy = row++;
        cardChinh.add(txtSdt, gbc);

        gbc.gridy = row++; gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.gridx = 0; cardChinh.add(wrapCombo("Chức vụ *", cbChucVu), gbc);
        gbc.gridx = 1; cardChinh.add(wrapTogglePanel("Trạng Thái Làm Việc", tglTrangThai, lblTrangThaiText), gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        cardChinh.add(txtLuongGio, gbc);

        gbc.gridy = row++; gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.gridx = 0; cardChinh.add(txtNgayVaoLam, gbc);
        gbc.gridx = 1; cardChinh.add(txtNgayNghiViec, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(20, 10, 5, 10);
        JLabel lblBaoMat = new JLabel("BẢO MẬT TÀI KHOẢN");
        lblBaoMat.setFont(FONT_CALIBRI_BOLD.deriveFont(18.5f)); 
        lblBaoMat.setForeground(new Color(190, 210, 230)); 
        cardChinh.add(lblBaoMat, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(5, 10, 20, 10);
        JPanel pnlDoiMK = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlDoiMK.setOpaque(false);
        pnlDoiMK.add(btnDoiMatKhau);
        cardChinh.add(pnlDoiMK, gbc);

        gbc.gridy = row++;
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(120, 130, 145));
        sep.setBackground(COLOR_CARD_DARK);
        cardChinh.add(sep, gbc);

        gbc.gridy = row;
        gbc.insets = new Insets(20, 10, 0, 10);
        JPanel pnlAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlAction.setOpaque(false);
        pnlAction.add(btnHuyBo);
        pnlAction.add(btnCapNhat);
        cardChinh.add(pnlAction, gbc);

        JScrollPane scroll = new JScrollPane(cardChinh);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        TienIchGiaoDien.thietLapThanhCuon(scroll);

        add(scroll, BorderLayout.CENTER);
    }

    // ================== CÁC HÀM XỬ LÝ DATABASE ==================
    
    // 1. Hàm Load Dữ Liệu Lên Form
    private void loadThongTinNhanVien() {
        try {
            NhanVien nv = nvLogic.timNhanVienTheoMa(maNVHienTai);
            if (nv != null) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                // Đổ dữ liệu text
                txtHoTen.setText(nv.getHoTen());
                txtSdt.setText(nv.getSDT());
                
                // Tiền và Ngày tháng (Chỉ đọc)
                if(nv.getLuongGio() != null) {
                    txtLuongGio.setText(DinhDangUtil.dinhDangTien(nv.getLuongGio()));
                }
                if(nv.getNgayVaoLam() != null) {
                    txtNgayVaoLam.setText(dtf.format(nv.getNgayVaoLam()));
                }
                if(nv.getNgayNghiViec() != null) {
                    txtNgayNghiViec.setText(dtf.format(nv.getNgayNghiViec()));
                } else {
                    txtNgayNghiViec.setText("");
                }

                // Chức vụ & Trạng thái
                cbChucVu.setSelectedItem(nv.getChucVu());
                
                boolean isDangLam = "Đang Làm Việc".equalsIgnoreCase(nv.getTrangThai());
                tglTrangThai.setSelected(isDangLam);
                lblTrangThaiText.setText(isDangLam ? "Đang làm việc" : "Đã nghỉ việc");
            }
        } catch (Exception e) {
            TienIchGiaoDien.hienThiThongBao(this, "Không thể tải thông tin nhân viên: " + e.getMessage(), "ERROR");
        }
    }

    // 2. Hàm Cập Nhật Dữ Liệu
    private void capNhatThongTinNhanVien() {
        try {
            // Lấy lại NV cũ để giữ nguyên các field bị khóa (Ngày vào làm, mã NV, lương...)
            NhanVien nv = nvLogic.timNhanVienTheoMa(maNVHienTai);
            if (nv != null) {
                nv.setHoTen(txtHoTen.getText().trim());
                nv.setSDT(txtSdt.getText().trim());
                // Gọi hàm sửa bên Logic (đã có check rỗng, validate các kiểu)
                nvLogic.suaNhanVien(nv);
                
                TienIchGiaoDien.hienThiThongBao(this, "Cập nhật hồ sơ thành công!", "SUCCESS");
                loadThongTinNhanVien(); // Tải lại cho chắc
            }
        } catch (Exception ex) {
            TienIchGiaoDien.hienThiThongBao(this, ex.getMessage(), "ERROR");
        }
    }

    // ================== CÁC HÀM TIỆN ÍCH GIAO DIỆN ==================
    private void setFontChoONhapLieu(ONhapLieuHienDai comp) {
        comp.getField().setFont(FONT_CALIBRI_PLAIN);
        BorderLayout layout = (BorderLayout) comp.getLayout();
        Component title = layout.getLayoutComponent(BorderLayout.NORTH);
        if (title instanceof JLabel) {
            ((JLabel) title).setFont(FONT_CALIBRI_BOLD);
        }
    }

    private void fixMauChuChoONhapLieu(ONhapLieuHienDai comp) {
        if (!comp.getField().isEditable()) {
            comp.getField().setForeground(new Color(60, 60, 60));
        }
    }

    private JPanel wrapCombo(String title, JComboBox<String> comp) {
        JPanel pnl = new JPanel(new BorderLayout(0, 8));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(0, 0, 15, 0)); 
        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_CALIBRI_BOLD); 
        lbl.setForeground(Color.WHITE); 
        comp.setPreferredSize(new Dimension(200, 48)); 
        comp.setBorder(BorderFactory.createCompoundBorder(
                comp.getBorder(), new EmptyBorder(8, 0, 8, 0)));
        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(comp, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel wrapTogglePanel(String title, TienIchGiaoDien.NutGat toggle, JLabel lblText) {
        JPanel pnl = new JPanel(new BorderLayout(0, 8));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(0, 0, 15, 0)); 
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_CALIBRI_BOLD); 
        lblTitle.setForeground(Color.WHITE); 
        JPanel pnlCenter = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        pnlCenter.setOpaque(false);
        pnlCenter.setPreferredSize(new Dimension(200, 48)); 
        pnlCenter.add(toggle);
        pnlCenter.add(lblText);
        pnl.add(lblTitle, BorderLayout.NORTH);
        pnl.add(pnlCenter, BorderLayout.CENTER);
        return pnl;
    }

    // ================== DIALOG ĐỔI MẬT KHẨU ==================
    private void hienThiDialogDoiMatKhau() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Đổi Mật Khẩu", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0)); 

        JPanel pnlMain = new JPanel(new BorderLayout(0, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD_DARK); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.setColor(new Color(130, 140, 155));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel lblTitle = new JLabel("ĐỔI MẬT KHẨU", SwingConstants.CENTER);
        lblTitle.setFont(FONT_CALIBRI_BOLD.deriveFont(24f)); 
        lblTitle.setForeground(Color.WHITE);
        pnlMain.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlFields = new JPanel(new GridLayout(3, 1, 0, 5));
        pnlFields.setOpaque(false);
        
        ONhapLieuHienDai txtOld = new ONhapLieuHienDai("Mật khẩu hiện tại *", true, true);
        ONhapLieuHienDai txtNew = new ONhapLieuHienDai("Mật khẩu mới *", true, true);
        ONhapLieuHienDai txtConfirm = new ONhapLieuHienDai("Xác nhận mật khẩu *", true, true);
        
        setFontChoONhapLieu(txtOld);
        setFontChoONhapLieu(txtNew);
        setFontChoONhapLieu(txtConfirm);

        pnlFields.add(txtOld);
        pnlFields.add(txtNew);
        pnlFields.add(txtConfirm);
        pnlMain.add(pnlFields, BorderLayout.CENTER);

        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlBtn.setOpaque(false);
        
        JButton btnClose = TienIchGiaoDien.taoNutHienDai("HỦY", new Color(148, 163, 184));
        btnClose.setFont(FONT_CALIBRI_BOLD);
        btnClose.addActionListener(e -> dialog.dispose());
        
        JButton btnSave = TienIchGiaoDien.taoNutHienDai("LƯU THAY ĐỔI", new Color(37, 99, 235)); 
        btnSave.setFont(FONT_CALIBRI_BOLD);
        btnSave.addActionListener(e -> {
            try {
                // 1. Tìm tên đăng nhập (Tài khoản) của ông NV này
                String tenTaiKhoan = null;
                for (TaiKhoan tk : tkLogic.layDanhSachTaiKhoan()) {
                    if (tk.getMaNV().equals(maNVHienTai)) {
                        tenTaiKhoan = tk.getTaiKhoan();
                        break;
                    }
                }
                
                if (tenTaiKhoan == null) {
                    throw new Exception("Không tìm thấy tài khoản hệ thống của nhân viên này!");
                }

                // 2. Gọi hàm đổi pass trong Logic (Đã xử lý mã hóa, bắt lỗi, check trùng pass)
                tkLogic.xuLyDoiMatKhau(
                    tenTaiKhoan, 
                    txtOld.getText(), 
                    txtNew.getText(), 
                    txtConfirm.getText()
                );
                
                TienIchGiaoDien.hienThiThongBao(this, "Đổi mật khẩu thành công!", "SUCCESS");
                dialog.dispose();

            } catch (Exception ex) {
                // Nếu sai pass, độ dài không đủ, pass không khớp... thì thông báo ở đây
                TienIchGiaoDien.hienThiThongBao(dialog, ex.getMessage(), "ERROR");
            }
        });

        pnlBtn.add(btnClose);
        pnlBtn.add(btnSave);
        pnlMain.add(pnlBtn, BorderLayout.SOUTH);

        dialog.setContentPane(pnlMain);
        dialog.pack();
        dialog.setSize(450, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}