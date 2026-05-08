package GUI;

import GUI.HoTro.DinhDangUtil;
import GUI.HoTro.NutBoGoc;
import GUI.HoTro.TheBongDo;
import GUI.HoTro.TienIchGiaoDien;
import Logic.ChiaCaLogic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GiaoCaDialog extends JDialog {

    private String maCaHienTai;
    private BigDecimal tienDauCa;
    private BigDecimal tienBanHang;

    private TheBongDo.RoundedTextField txtTienCuoiCa;
    private JLabel lblChenhLech;
    private JLabel lblCLText;

    public GiaoCaDialog(JFrame parent, String maCa, BigDecimal tienDau, BigDecimal tienBan) {
        super(parent, "Kết Thúc Ca Làm Việc", true);
        this.maCaHienTai = maCa;
        this.tienDauCa = tienDau != null ? tienDau : BigDecimal.ZERO;
        this.tienBanHang = tienBan != null ? tienBan : BigDecimal.ZERO;

        khoiTaoGiaoDien();
        pack();
        setLocationRelativeTo(parent);
    }

    private void khoiTaoGiaoDien() {
        JPanel pnlMain = new JPanel(new BorderLayout(0, 15));
        pnlMain.setBackground(Color.WHITE);
        pnlMain.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel lblTitle = new JLabel("BÀN GIAO KÉT TIỀN KẾT CA", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Calibri", Font.BOLD, 22));
        lblTitle.setForeground(new Color(15, 23, 42));
        pnlMain.add(lblTitle, BorderLayout.NORTH);

        // --- Khu vực thông tin ---
        JPanel pnlInfo = new JPanel(new GridLayout(4, 1, 0, 15));
        pnlInfo.setBackground(Color.WHITE);

        pnlInfo.add(taoDongThongTin("Mã ca làm:", maCaHienTai, false));
        pnlInfo.add(taoDongThongTin("Tiền đầu ca (Kế thừa từ ca trước):", DinhDangUtil.dinhDangTien(tienDauCa), false));
        pnlInfo.add(taoDongThongTin("Tổng tiền bán hàng trong ca:", DinhDangUtil.dinhDangTien(tienBanHang), false));
        // Ô Nhập tiền cuối ca
        JPanel pnlNhapTien = new JPanel(new BorderLayout(10, 0));
        pnlNhapTien.setBackground(Color.WHITE);
        JLabel lblNhap = new JLabel("Số tiền mặt thực tế đang có: ");
        lblNhap.setFont(new Font("Calibri", Font.BOLD, 18));
        txtTienCuoiCa = new TheBongDo.RoundedTextField("0", 0);
        txtTienCuoiCa.setFont(new Font("Calibri", Font.BOLD, 18));
        txtTienCuoiCa.setForeground(new Color(37, 99, 235)); 
        pnlNhapTien.add(lblNhap, BorderLayout.WEST);
        pnlNhapTien.add(txtTienCuoiCa, BorderLayout.CENTER);
        pnlInfo.add(pnlNhapTien);

        pnlMain.add(pnlInfo, BorderLayout.CENTER);

        // --- Khu vực Chênh lệch & Nút bấm ---
        JPanel pnlBottom = new JPanel(new BorderLayout(0, 20));
        pnlBottom.setBackground(Color.WHITE);

        JPanel pnlChenhLech = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlChenhLech.setBackground(Color.WHITE);
        lblCLText = new JLabel("Khoản chi ra / Chênh lệch: ");
        lblCLText.setFont(new Font("Calibri", Font.BOLD, 20));
        lblChenhLech = new JLabel("0 đ");
        lblChenhLech.setFont(new Font("Calibri", Font.BOLD, 24));
        pnlChenhLech.add(lblCLText);
        pnlChenhLech.add(lblChenhLech);
        
        pnlBottom.add(pnlChenhLech, BorderLayout.NORTH);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        pnlBtns.setBackground(Color.WHITE);

        NutBoGoc btnHuy = new NutBoGoc("Hủy bỏ");
        btnHuy.setColorBackground(new Color(226, 232, 240));
        btnHuy.setForeground(new Color(15, 23, 42));
        btnHuy.addActionListener(e -> dispose());

        NutBoGoc btnXacNhan = new NutBoGoc("Xác Nhận Kết Ca");
        btnXacNhan.setColorBackground(new Color(16, 185, 129)); 
        btnXacNhan.addActionListener(e -> xuLyKetCa());

        pnlBtns.add(btnHuy);
        pnlBtns.add(btnXacNhan);
        pnlBottom.add(pnlBtns, BorderLayout.SOUTH);

        pnlMain.add(pnlBottom, BorderLayout.SOUTH);

        txtTienCuoiCa.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tinhChenhLech(); }
            public void removeUpdate(DocumentEvent e) { tinhChenhLech(); }
            public void changedUpdate(DocumentEvent e) { tinhChenhLech(); }
        });

        setContentPane(pnlMain);
    }

    private JPanel taoDongThongTin(String nhan, String giaTri, boolean isBold) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBackground(Color.WHITE);
        JLabel lblTitle = new JLabel(nhan);
        lblTitle.setFont(new Font("Calibri", Font.PLAIN, 18));
        JLabel lblValue = new JLabel(giaTri);
        lblValue.setFont(new Font("Calibri", isBold ? Font.BOLD : Font.PLAIN, 18));
        pnl.add(lblTitle, BorderLayout.WEST);
        pnl.add(lblValue, BorderLayout.EAST);
        return pnl;
    }

    private void tinhChenhLech() {
        try {
            String text = txtTienCuoiCa.getText().replaceAll("[^\\d]", "");
            if (text.isEmpty()) text = "0";
            BigDecimal tienCuoiCa = new BigDecimal(text);
            
            BigDecimal tongLyThuyet = tienDauCa.add(tienBanHang);
            BigDecimal chenhLech = tienCuoiCa.subtract(tongLyThuyet);

            lblChenhLech.setText(DinhDangUtil.dinhDangTien(chenhLech));
            
            if (chenhLech.compareTo(BigDecimal.ZERO) < 0) {
                lblCLText.setText("Hụt két / Đã chi ra: ");
                lblChenhLech.setForeground(new Color(220, 38, 38)); // Màu đỏ
            } else if (chenhLech.compareTo(BigDecimal.ZERO) > 0) {
                lblCLText.setText("Dư két: ");
                lblChenhLech.setForeground(new Color(22, 163, 74)); // Màu xanh
            } else {
                lblCLText.setText("Chênh lệch: ");
                lblChenhLech.setForeground(new Color(15, 23, 42)); // Đen
            }
        } catch (Exception ignored) {}
    }

    private void xuLyKetCa() {
        try {
            String text = txtTienCuoiCa.getText().replaceAll("[^\\d]", "");
            if (text.isEmpty()) throw new Exception("Vui lòng nhập số tiền cuối ca!");
            BigDecimal tienCuoiCa = new BigDecimal(text);

            ChiaCaLogic logic = new ChiaCaLogic();
            logic.thucHienCheckOut(maCaHienTai, LocalDateTime.now(), tienCuoiCa, tienBanHang, tienDauCa);

            TienIchGiaoDien.hienThiThongBao(this, "Đã giao ca thành công! Tiền kết ca của bạn sẽ được chuyển giao cho ca sau.", "SUCCESS");
            dispose();
        } catch (Exception ex) {
            TienIchGiaoDien.hienThiThongBao(this, ex.getMessage(), "ERROR");
        }
    }
}