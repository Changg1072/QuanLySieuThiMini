package GUI;

import GUI.HoTro.*;
import Data.SanPham;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class BanHangUi extends JPanel implements DanhSachSPUi.CallBackGioHang {

    private DanhSachSPUi pnlDanhSachSP;
    private JPanel pnlGioHang; 
    private JPanel pnlDanhSachMon; 
    
    private JLabel lblTongTien, lblTongSL;
    
    private class ChiTietGio {
        SanPham sp;
        int soLuong;
        public ChiTietGio(SanPham sp, int sl) { this.sp = sp; this.soLuong = sl; }
    }
    
    private Map<String, ChiTietGio> gioHangData = new HashMap<>();

    public BanHangUi() {
        setLayout(new BorderLayout());
        setBackground(DanhSachSPUi.BG_MAIN);

        pnlDanhSachSP = new DanhSachSPUi(this, DanhSachSPUi.UIMode.BAN_HANG);
        taoGioHang();

        add(pnlDanhSachSP, BorderLayout.CENTER);
    }

    // --- THÊM HÀM NÀY ĐỂ ADMIN TRUY CẬP LỌC DỮ LIỆU ---
    public DanhSachSPUi getPnlDanhSachSP() {
        return this.pnlDanhSachSP;
    }

    private void taoGioHang() {
        pnlGioHang = new JPanel(new BorderLayout());
        pnlGioHang.setBackground(Color.WHITE); // Đổi màu giỏ hàng cho sáng
        pnlGioHang.setPreferredSize(new Dimension(420, 0)); 
        pnlGioHang.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(200, 200, 200)));

        JLabel lblTitle = new JLabel("🛒 GIỎ HÀNG CỦA BẠN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Calibri", Font.BOLD, 22)); 
        lblTitle.setForeground(new Color(15, 23, 42)); 
        lblTitle.setBorder(new EmptyBorder(20, 10, 20, 10));
        pnlGioHang.add(lblTitle, BorderLayout.NORTH);

        pnlDanhSachMon = new JPanel();
        pnlDanhSachMon.setLayout(new BoxLayout(pnlDanhSachMon, BoxLayout.Y_AXIS));
        pnlDanhSachMon.setBackground(Color.WHITE);
        
        JScrollPane scrollCart = new JScrollPane(pnlDanhSachMon);
        TienIchGiaoDien.thietLapThanhCuon(scrollCart);
        scrollCart.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(220, 220, 220)));
        pnlGioHang.add(scrollCart, BorderLayout.CENTER);

        // --- FOOTER ---
        JPanel pnlFooter = new JPanel(new BorderLayout(0, 20));
        pnlFooter.setOpaque(false);
        pnlFooter.setBorder(new EmptyBorder(20, 20, 25, 20)); 

        JPanel pnlInfo = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlInfo.setOpaque(false);
        
        JPanel pnlSL = new JPanel(new GridLayout(2, 1));
        pnlSL.setOpaque(false);
        JLabel lblTitleSL = new JLabel("Tổng SL:");
        lblTitleSL.setFont(new Font("Calibri", Font.PLAIN, 16)); 
        lblTitleSL.setForeground(new Color(100, 116, 139));
        lblTongSL = new JLabel("0");
        lblTongSL.setFont(new Font("Calibri", Font.BOLD, 24)); 
        lblTongSL.setForeground(new Color(15, 23, 42));
        pnlSL.add(lblTitleSL);
        pnlSL.add(lblTongSL);

        JPanel pnlTien = new JPanel(new GridLayout(2, 1));
        pnlTien.setOpaque(false);
        JLabel lblTitleTien = new JLabel("Tổng Tiền:");
        lblTitleTien.setFont(new Font("Calibri", Font.PLAIN, 16)); 
        lblTitleTien.setForeground(new Color(100, 116, 139));
        lblTongTien = new JLabel("0 đ");
        lblTongTien.setFont(new Font("Calibri", Font.BOLD, 24)); 
        lblTongTien.setForeground(new Color(34, 197, 94)); 
        pnlTien.add(lblTitleTien);
        pnlTien.add(lblTongTien);

        pnlInfo.add(pnlSL);
        pnlInfo.add(pnlTien);

        JPanel pnlBtn = new JPanel(new GridLayout(1, 2, 15, 0)); 
        pnlBtn.setOpaque(false);
        pnlBtn.setPreferredSize(new Dimension(0, 55)); 
        
        NutBoGoc btnHuy = new NutBoGoc("HỦY");
        btnHuy.setFont(new Font("Calibri", Font.BOLD, 17)); 
        btnHuy.setColorBackground(new Color(248, 113, 113)); 
        btnHuy.addActionListener(e -> xuLyHuyGioHang());

        NutBoGoc btnThanhToan = new NutBoGoc("THANH TOÁN");
        btnThanhToan.setFont(new Font("Calibri", Font.BOLD, 17)); 
        btnThanhToan.setColorBackground(new Color(96, 165, 250)); 
        btnThanhToan.addActionListener(e -> {
            if (gioHangData.isEmpty()) {
                TienIchGiaoDien.hienThiThongBao(this, "Giỏ hàng đang trống, hãy chọn món trước!", "WARNING");
                return;
            }
            if (hanhDongThanhToan != null) {
                hanhDongThanhToan.run();
            }
        });
        
        pnlBtn.add(btnHuy);
        pnlBtn.add(btnThanhToan);

        pnlFooter.add(pnlInfo, BorderLayout.NORTH);
        pnlFooter.add(pnlBtn, BorderLayout.CENTER);

        pnlGioHang.add(pnlFooter, BorderLayout.SOUTH);
    }

    @Override
    public void capNhatGioHang(SanPham sp, int soLuongThayDoi, DanhSachSPUi.TheSanPham card) {
        if (pnlGioHang.getParent() == null) {
            this.add(pnlGioHang, BorderLayout.EAST);
            this.revalidate();
        }

        String maSP = sp.getMaSP();
        ChiTietGio item = gioHangData.getOrDefault(maSP, new ChiTietGio(sp, 0));
        item.soLuong += soLuongThayDoi;

        if (item.soLuong <= 0) {
            gioHangData.remove(maSP);
        } else {
            gioHangData.put(maSP, item);
        }

        renderLaiDanhSachMon(); 
    }

    private void renderLaiDanhSachMon() {
        pnlDanhSachMon.removeAll();
        int tongSl = 0;
        BigDecimal tongTien = BigDecimal.ZERO;

        for (Map.Entry<String, ChiTietGio> entry : gioHangData.entrySet()) {
            String ma = entry.getKey();
            ChiTietGio item = entry.getValue();
            SanPham sp = item.sp;
            int sl = item.soLuong;
            BigDecimal thanhTien = sp.getGiaBan().multiply(new BigDecimal(sl));

            tongSl += sl;
            tongTien = tongTien.add(thanhTien);

            JPanel pnlRow = new JPanel(new BorderLayout(5, 5));
            pnlRow.setBackground(Color.WHITE);
            pnlRow.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                    new EmptyBorder(12, 10, 12, 10)
            ));
            pnlRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75)); 

            JPanel pnlLeft = new JPanel(new GridLayout(2, 1, 0, 5));
            pnlLeft.setOpaque(false);
            
            JLabel lblTen = new JLabel(sp.getTenSP());
            lblTen.setFont(new Font("Calibri", Font.BOLD, 15)); 
            
            JLabel lblGia = new JLabel(DinhDangUtil.dinhDangTien(thanhTien));
            lblGia.setFont(new Font("Calibri", Font.BOLD, 15)); 
            lblGia.setForeground(new Color(239, 68, 68)); 
            
            pnlLeft.add(lblTen);
            pnlLeft.add(lblGia);

            JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            pnlRight.setOpaque(false);
            
            NutBoGoc btnTru = new NutBoGoc("-");
            btnTru.setFont(new Font("Calibri", Font.BOLD, 17)); 
            btnTru.setColorBackground(new Color(241, 245, 249));
            btnTru.setForeground(new Color(71, 85, 105));
            btnTru.setMargin(new Insets(2, 8, 2, 8)); 
            btnTru.addActionListener(e -> {
                DanhSachSPUi.TheSanPham card = pnlDanhSachSP.getTheSanPham(ma);
                if (card != null) card.congTruTuGioHang(-1);
            });

            JLabel lblQty = new JLabel(String.valueOf(sl), SwingConstants.CENTER);
            lblQty.setFont(new Font("Calibri", Font.BOLD, 17)); 
            lblQty.setPreferredSize(new Dimension(25, 25));

            NutBoGoc btnCong = new NutBoGoc("+");
            btnCong.setFont(new Font("Calibri", Font.BOLD, 17)); 
            btnCong.setColorBackground(new Color(241, 245, 249));
            btnCong.setForeground(new Color(71, 85, 105));
            btnCong.setMargin(new Insets(2, 8, 2, 8));
            btnCong.addActionListener(e -> {
                DanhSachSPUi.TheSanPham card = pnlDanhSachSP.getTheSanPham(ma);
                if (card != null) card.congTruTuGioHang(1);
            });

            NutBoGoc btnXoa = new NutBoGoc("X");
            btnXoa.setFont(new Font("Calibri", Font.PLAIN, 14)); 
            btnXoa.setColorBackground(new Color(254, 226, 226)); 
            btnXoa.setForeground(new Color(220, 38, 38)); 
            btnXoa.setMargin(new Insets(4, 10, 4, 10));
            btnXoa.addActionListener(e -> {
                DanhSachSPUi.TheSanPham card = pnlDanhSachSP.getTheSanPham(ma);
                if (card != null) card.xoaKhoiGioHang();
            });

            pnlRight.add(btnTru);
            pnlRight.add(lblQty);
            pnlRight.add(btnCong);
            pnlRight.add(btnXoa);

            pnlRow.add(pnlLeft, BorderLayout.CENTER);
            pnlRow.add(pnlRight, BorderLayout.EAST);

            pnlDanhSachMon.add(pnlRow);
        }

        lblTongSL.setText(String.valueOf(tongSl));
        lblTongTien.setText(DinhDangUtil.dinhDangTien(tongTien));

        pnlDanhSachMon.revalidate();
        pnlDanhSachMon.repaint();

        if (gioHangData.isEmpty()) {
            anGioHang();
        }
    }

    private void xuLyHuyGioHang() {
        TienIchGiaoDien.hienThiXacNhan(this, "Bạn có chắc muốn xóa sạch giỏ hàng không?", () -> {
            lamMoiGioHang();
        });
    }

    public void lamMoiGioHang() {
        gioHangData.clear(); 
        renderLaiDanhSachMon(); 
        for (Component comp : pnlDanhSachSP.layDanhSachTheSP()) {
            if (comp instanceof DanhSachSPUi.TheSanPham) {
                ((DanhSachSPUi.TheSanPham) comp).resetTrangThai();
            }
        }
        anGioHang(); 
    }

    public void lamMoiToanBoBanHang() {
        lamMoiGioHang(); 
        if (pnlDanhSachSP != null) {
            this.remove(pnlDanhSachSP);
        }
        pnlDanhSachSP = new DanhSachSPUi(this, DanhSachSPUi.UIMode.BAN_HANG);
        this.add(pnlDanhSachSP, BorderLayout.CENTER);
        this.revalidate();
        this.repaint();
    }

    private void anGioHang() {
        this.remove(pnlGioHang);
        this.revalidate();
        this.repaint();
    }

    private Runnable hanhDongThanhToan;

    public void setHanhDongThanhToan(Runnable hanhDongThanhToan) {
        this.hanhDongThanhToan = hanhDongThanhToan;
    }

    public Object[][] layDuLieuGioHang() {
        Object[][] data = new Object[gioHangData.size()][6]; 
        int i = 0;
        for (ChiTietGio item : gioHangData.values()) {
            data[i][0] = item.sp.getTenSP();
            data[i][1] = item.sp.getDonViTinh(); 
            data[i][2] = item.soLuong;
            data[i][3] = item.sp.getGiaBan();
            data[i][4] = item.sp.getGiaBan().multiply(new BigDecimal(item.soLuong));
            data[i][5] = item.sp.getMaSP(); 
            i++;
        }
        return data;
    }

    public BigDecimal layTongTienGioHang() {
        BigDecimal tong = BigDecimal.ZERO;
        for (ChiTietGio item : gioHangData.values()) {
            BigDecimal thanhTien = item.sp.getGiaBan().multiply(new BigDecimal(item.soLuong));
            tong = tong.add(thanhTien);
        }
        return tong;
    }
}