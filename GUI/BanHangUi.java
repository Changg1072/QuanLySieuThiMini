package GUI;

import GUI.HoTro.*;
import Data.SanPham;
import Data.ChiTietHoaDon;
import Dao.SanPhamDAO;

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
    
    // === 🔥 TRẠNG THÁI ĐỔI HÀNG ===
    private boolean isCheDoDoiHang = false;
    private BigDecimal tongTienHoaDonCu = BigDecimal.ZERO;
    private JLabel lblThongBaoDoiHang; // Label hiển thị cảnh báo đỏ khi đổi hàng
    
    private class ChiTietGio {
        SanPham sp;
        int soLuong;
        BigDecimal giaThucTe; // 🔥 Thêm giá thực tế (sau khi giảm)
        
        public ChiTietGio(SanPham sp, int sl, BigDecimal giaThucTe) { 
            this.sp = sp; 
            this.soLuong = sl; 
            this.giaThucTe = giaThucTe;
        }
    }
    
    private Map<String, ChiTietGio> gioHangData = new HashMap<>();

    public BanHangUi() {
        setLayout(new BorderLayout());
        setBackground(DanhSachSPUi.BG_MAIN);

        pnlDanhSachSP = new DanhSachSPUi(this, DanhSachSPUi.UIMode.BAN_HANG);
        taoGioHang();

        add(pnlDanhSachSP, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> hienThiCanhBaoHeThong());
    }

    public DanhSachSPUi getPnlDanhSachSP() {
        return this.pnlDanhSachSP;
    }

    private void taoGioHang() {
        pnlGioHang = new JPanel(new BorderLayout());
        pnlGioHang.setBackground(Color.WHITE); 
        pnlGioHang.setPreferredSize(new Dimension(420, 0)); 
        pnlGioHang.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(200, 200, 200)));

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(Color.WHITE);
        
        JLabel lblTitle = new JLabel("🛒 GIỎ HÀNG CỦA BẠN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Calibri", Font.BOLD, 22)); 
        lblTitle.setForeground(new Color(15, 23, 42)); 
        lblTitle.setBorder(new EmptyBorder(20, 10, 10, 10));
        pnlHeader.add(lblTitle, BorderLayout.NORTH);

        // Label thông báo khi đang trong chế độ đổi hàng
        lblThongBaoDoiHang = new JLabel("", SwingConstants.CENTER);
        lblThongBaoDoiHang.setFont(new Font("Calibri", Font.ITALIC, 14));
        lblThongBaoDoiHang.setForeground(new Color(239, 68, 68)); // Màu đỏ cảnh báo
        lblThongBaoDoiHang.setBorder(new EmptyBorder(0, 10, 10, 10));
        pnlHeader.add(lblThongBaoDoiHang, BorderLayout.SOUTH);

        pnlGioHang.add(pnlHeader, BorderLayout.NORTH);

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
            
            // 🔥 LOGIC CHẶN THANH TOÁN NẾU ĐANG ĐỔI HÀNG MÀ TIỀN CHƯA ĐẠT CHUẨN
            if (isCheDoDoiHang) {
                BigDecimal tongMoi = layTongTienGioHang();
                if (tongMoi.compareTo(tongTienHoaDonCu) < 0) {
                    TienIchGiaoDien.hienThiThongBao(this, "LỖI ĐỔI HÀNG:\nTổng tiền giỏ hàng mới (" + DinhDangUtil.dinhDangTien(tongMoi) + ") \nphải LỚN HƠN HOẶC BẰNG hóa đơn cũ (" + DinhDangUtil.dinhDangTien(tongTienHoaDonCu) + ")!", "ERROR");
                    return;
                }
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
        
        // 🔥 Lấy giá thực tế từ thẻ sản phẩm, nếu không có thẻ thì mặc định lấy giá gốc
        BigDecimal giaDangBan = (card != null) ? card.giaThucTe : sp.getGiaBan();
        
        // Truyền giaDangBan vào constructor
        ChiTietGio item = gioHangData.getOrDefault(maSP, new ChiTietGio(sp, 0, giaDangBan));
        item.soLuong += soLuongThayDoi;
        
        // Cập nhật lại giá nhỡ chương trình giảm giá có thay đổi
        if (card != null) {
            item.giaThucTe = card.giaThucTe; 
        }

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
            BigDecimal thanhTien = item.giaThucTe.multiply(new BigDecimal(sl));

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
                else capNhatGioHang(sp, -1, null); // Dự phòng nếu ko tìm thấy card
            });

            JTextField txtQty = new JTextField(String.valueOf(sl));
            txtQty.setFont(new Font("Calibri", Font.BOLD, 17));
            txtQty.setHorizontalAlignment(JTextField.CENTER);
            txtQty.setPreferredSize(new Dimension(45, 28)); // Kích thước vừa vặn
            txtQty.setBorder(null);
            txtQty.setBackground(Color.WHITE);
            
            // Xử lý logic khi nhập số mới
            java.util.function.Consumer<String> capNhatSoLuongTuGhi = (text) -> {
                try {
                    int soLuongMoi = Integer.parseInt(text.trim());
                    
                    if (soLuongMoi < 0) {
                        txtQty.setText(String.valueOf(sl)); // Khôi phục lại số cũ
                        TienIchGiaoDien.hienThiThongBao(BanHangUi.this, "Số lượng không hợp lệ!", "ERROR");
                        return;
                    }
                    
                    // Nếu không thay đổi gì thì bỏ qua
                    if (soLuongMoi == sl) return; 

                    // Nếu gõ 0 thì xóa luôn khỏi giỏ
                    if (soLuongMoi == 0) {
                        DanhSachSPUi.TheSanPham card = pnlDanhSachSP.getTheSanPham(ma);
                        if (card != null) card.xoaKhoiGioHang();
                        else capNhatGioHang(sp, -sl, null);
                        return;
                    }

                    // Tính độ chênh lệch để cộng/trừ
                    int chenhLech = soLuongMoi - sl;
                    DanhSachSPUi.TheSanPham card = pnlDanhSachSP.getTheSanPham(ma);
                    
                    if (card != null) {
                        if (card.getSoLuongMua() + chenhLech > card.tonMax) {
                            txtQty.setText(String.valueOf(sl)); // Khôi phục
                            TienIchGiaoDien.hienThiThongBao(BanHangUi.this, "Chỉ còn " + card.tonMax + " sản phẩm trong kho!", "WARNING");
                            return;
                        }
                        card.congTruTuGioHang(chenhLech);
                    } else {
                        capNhatGioHang(sp, chenhLech, null);
                    }

                } catch (NumberFormatException ex) {
                    txtQty.setText(String.valueOf(sl)); // Khôi phục nếu gõ chữ
                    TienIchGiaoDien.hienThiThongBao(BanHangUi.this, "Vui lòng chỉ gõ số nguyên!", "WARNING");
                }
            };

            // Lưu thay đổi khi nhấn phím Enter
            txtQty.addActionListener(e -> capNhatSoLuongTuGhi.accept(txtQty.getText()));

            // Lưu thay đổi khi click chuột ra chỗ khác (mất focus)
            txtQty.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    capNhatSoLuongTuGhi.accept(txtQty.getText());
                }
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    txtQty.selectAll(); // Tự động bôi đen để gõ đè số mới luôn
                }
            });

            NutBoGoc btnCong = new NutBoGoc("+");
            btnCong.setFont(new Font("Calibri", Font.BOLD, 17)); 
            btnCong.setColorBackground(new Color(241, 245, 249));
            btnCong.setForeground(new Color(71, 85, 105));
            btnCong.setMargin(new Insets(2, 8, 2, 8));
            btnCong.addActionListener(e -> {
                DanhSachSPUi.TheSanPham card = pnlDanhSachSP.getTheSanPham(ma);
                if (card != null) card.congTruTuGioHang(1);
                else capNhatGioHang(sp, 1, null);
            });

            NutBoGoc btnXoa = new NutBoGoc("X");
            btnXoa.setFont(new Font("Calibri", Font.PLAIN, 14)); 
            btnXoa.setColorBackground(new Color(254, 226, 226)); 
            btnXoa.setForeground(new Color(220, 38, 38)); 
            btnXoa.setMargin(new Insets(4, 10, 4, 10));
            btnXoa.addActionListener(e -> {
                DanhSachSPUi.TheSanPham card = pnlDanhSachSP.getTheSanPham(ma);
                if (card != null) card.xoaKhoiGioHang();
                else capNhatGioHang(sp, -sl, null);
            });

            pnlRight.add(btnTru);
            pnlRight.add(txtQty);
            pnlRight.add(btnCong);
            pnlRight.add(btnXoa);

            pnlRow.add(pnlLeft, BorderLayout.CENTER);
            pnlRow.add(pnlRight, BorderLayout.EAST);

            pnlDanhSachMon.add(pnlRow);
        }

        lblTongSL.setText(String.valueOf(tongSl));
        lblTongTien.setText(DinhDangUtil.dinhDangTien(tongTien));

        // Nếu đang đổi hàng mà chưa đủ tiền, hiện chữ màu đỏ cảnh báo ở Tổng tiền
        if (isCheDoDoiHang && tongTien.compareTo(tongTienHoaDonCu) < 0) {
            lblTongTien.setForeground(new Color(239, 68, 68)); // Đỏ
        } else {
            lblTongTien.setForeground(new Color(34, 197, 94)); // Xanh lá
        }

        pnlDanhSachMon.revalidate();
        pnlDanhSachMon.repaint();

        if (gioHangData.isEmpty() && !isCheDoDoiHang) {
            anGioHang();
        }
    }

    private void xuLyHuyGioHang() {
        TienIchGiaoDien.hienThiXacNhan(this, "Bạn có chắc muốn xóa sạch giỏ hàng (Hủy đổi hàng) không?", () -> {
            lamMoiGioHang();
        });
    }

    public void lamMoiGioHang() {
        gioHangData.clear(); 
        isCheDoDoiHang = false;
        tongTienHoaDonCu = BigDecimal.ZERO;
        lblThongBaoDoiHang.setText(""); // Xóa cảnh báo

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
        // Tăng kích thước mảng lên 8 cột để chứa thêm % giảm giá
        Object[][] data = new Object[gioHangData.size()][8]; 
        int i = 0;
        for (ChiTietGio item : gioHangData.values()) {
            data[i][0] = item.sp.getTenSP();
            data[i][1] = item.sp.getDonViTinh(); 
            data[i][2] = item.soLuong;
            
            // Cột 3: Đơn giá GỐC (Giá thật)
            BigDecimal giaGoc = item.sp.getGiaBan();
            data[i][3] = giaGoc;
            
            // Cột 4: Thành tiền (Giá đã giảm * Số lượng)
            data[i][4] = item.giaThucTe.multiply(new BigDecimal(item.soLuong));
            
            data[i][5] = item.sp.getMaSP(); 
            
            // Cột 6: Giá thực tế (Dự phòng nếu cần tính toán thêm)
            data[i][6] = item.giaThucTe;
            
            // Cột 7: Xử lý chuỗi hiển thị cột "Giảm giá" = Tiền giảm + (% giảm)
            BigDecimal tienGiamMotSP = giaGoc.subtract(item.giaThucTe);
            if (tienGiamMotSP.compareTo(BigDecimal.ZERO) > 0) {
                // Tính % giảm
                BigDecimal phanTram = tienGiamMotSP.multiply(new BigDecimal("100"))
                                                   .divide(giaGoc, 0, java.math.RoundingMode.HALF_UP);
                
                // Tổng tiền giảm của dòng = Tiền giảm 1 SP * Số lượng
                BigDecimal tongTienGiam = tienGiamMotSP.multiply(new BigDecimal(item.soLuong));
                
                // Kết quả hiển thị: VD "5.000 đ (10%)"
                data[i][7] = DinhDangUtil.dinhDangTien(tongTienGiam) + " (" + phanTram + "%)";
            } else {
                data[i][7] = "0 đ"; // Không giảm giá
            }
            i++;
        }
        return data;
    }

    public BigDecimal layTongTienGioHang() {
        BigDecimal tong = BigDecimal.ZERO;
        for (ChiTietGio item : gioHangData.values()) {
            BigDecimal thanhTien = item.giaThucTe.multiply(new BigDecimal(item.soLuong));
            tong = tong.add(thanhTien);
        }
        return tong;
    }
    private void hienThiCanhBaoHeThong() {
        new Thread(() -> {
            try {
                java.util.List<String> dsCanhBao = Logic.KiemKeLogic.getInstance().quetCanhBaoHeThong();
                java.util.List<String> dsBatThuong = Logic.KiemKeLogic.getInstance().phatHienBatThuongKiemKe();
                
                if (!dsCanhBao.isEmpty() || !dsBatThuong.isEmpty()) {
                    StringBuilder thongDiep = new StringBuilder("<html><h3>CẢNH BÁO KHO TỰ ĐỘNG:</h3><ul>");
                    
                    int count = 0;
                    // Ưu tiên hiển thị cảnh báo bất thường/lệch kho trước
                    for (String bt : dsBatThuong) { if(count++ < 3) thongDiep.append("<li><b>").append(bt).append("</b></li>"); }
                    for (String cb : dsCanhBao)   { if(count++ < 5) thongDiep.append("<li>").append(cb).append("</li>"); }
                    
                    if (dsCanhBao.size() + dsBatThuong.size() > 5) {
                        thongDiep.append("<li><i>... và nhiều cảnh báo khác. Xem tại màn hình Quản lý.</i></li>");
                    }
                    thongDiep.append("</ul></html>");
                    
                    SwingUtilities.invokeLater(() -> {
                        // Hiển thị một Dialog nhỏ không chắn tầm nhìn (hoặc dùng JLabel cảnh báo ở Header)
                        GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, thongDiep.toString(), "WARNING");
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
    // ====================================================================
    // 🔥 TUYỆT CHIÊU ĐỔI HÀNG: ĐẨY DỮ LIỆU TỪ HÓA ĐƠN CŨ VÀO GIỎ MỚI
    // ====================================================================
    public void kichHoatCheDoDoiHang(java.util.List<ChiTietHoaDon> dsChiTietCu, BigDecimal tongTienCu) {
        lamMoiGioHang(); // Xóa sạch giỏ hiện tại trước
        this.isCheDoDoiHang = true;
        this.tongTienHoaDonCu = tongTienCu;

        // Cập nhật giao diện cảnh báo
        lblThongBaoDoiHang.setText("<html><center>ĐANG ĐỔI HÀNG<br>Yêu cầu giỏ hàng mới >= " + DinhDangUtil.dinhDangTien(tongTienCu) + "</center></html>");

        // Bê sản phẩm cũ vào giỏ
        for (ChiTietHoaDon ct : dsChiTietCu) {
            SanPham sp = SanPhamDAO.getInstance().laySanPhamTheoMa(ct.getMaSp());
            if (sp != null) {
                DanhSachSPUi.TheSanPham card = pnlDanhSachSP.getTheSanPham(sp.getMaSP());
                BigDecimal giaHienTai = (card != null) ? card.giaThucTe : sp.getGiaBan();
                
                ChiTietGio item = new ChiTietGio(sp, ct.getSoLuong(), giaHienTai);
                gioHangData.put(sp.getMaSP(), item);
                
                // ĐÃ XÓA ĐOẠN GỌI CARD BỊ LỖI Ở ĐÂY!
            }
        }

        // Bật panel giỏ hàng lên ngay lập tức
        if (pnlGioHang.getParent() == null) {
            this.add(pnlGioHang, BorderLayout.EAST);
            this.revalidate();
        }
        renderLaiDanhSachMon();
    }
    public boolean isCheDoDoiHang() {
        return this.isCheDoDoiHang;
    }

    public BigDecimal getTongTienHoaDonCu() {
        return this.tongTienHoaDonCu;
    }
}