package GUI;

import GUI.HoTro.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChiTietHoaDonUi extends TheBongDo {

    private final Color TEXT_MAIN = new Color(15, 23, 42);
    private final Font fontThuong = new Font("Calibri", Font.PLAIN, 15); 
    private final Font fontDam = new Font("Calibri", Font.BOLD, 15);
    private final Font fontTieuDe = new Font("Calibri", Font.BOLD, 22);

    private JLabel lblNgayMua, lblNhanVien, lblKhachHang, lblSoHD;
    private JTable tblChiTiet;
    private DefaultTableModel model;
    
    private JLabel lblTongSL, lblTongCong, lblGiamGiaBacTitle, lblGiamGiaBac, lblTruTichLuy, lblTienCanTra;
    private JLabel lblPhuongThucTitle, lblPhuongThucTen, lblTienKhach, lblTienThua, lblCongTichLuy;
    private JLabel lblBangChu;

    public ChiTietHoaDonUi() {
        super(15); 
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initUI();
    }

    private void initUI() {
        JPanel pnlMain = new JPanel(new BorderLayout(0, 5)); 
        pnlMain.setBackground(Color.WHITE);
        pnlMain.setBorder(new EmptyBorder(15, 25, 15, 25)); 

        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setBackground(Color.WHITE);

        JLabel lblTenCH = new JLabel("SIÊU THỊ MINI", SwingConstants.CENTER);
        lblTenCH.setFont(new Font("Calibri", Font.BOLD, 21)); 
        lblTenCH.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDiaChi = new JLabel("Địa chỉ: 54 Nguyễn Lương Bằng, Đà Nẵng", SwingConstants.CENTER);
        lblDiaChi.setFont(fontThuong); lblDiaChi.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblHotline = new JLabel("Hotline: 0345632721", SwingConstants.CENTER);
        lblHotline.setFont(fontThuong); lblHotline.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("HÓA ĐƠN BÁN LẺ", SwingConstants.CENTER);
        lblTitle.setFont(fontTieuDe);
        lblTitle.setBorder(new EmptyBorder(10, 0, 10, 0)); 
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlHeader.add(lblTenCH); pnlHeader.add(lblDiaChi);
        pnlHeader.add(lblHotline); pnlHeader.add(lblTitle);

        JPanel pnlInfo = new JPanel(new GridLayout(2, 2, 5, 5)); 
        pnlInfo.setBackground(Color.WHITE);

        lblNgayMua = taoLabelValue("---"); lblNhanVien = taoLabelValue("---");
        lblKhachHang = taoLabelValue("---"); lblSoHD = taoLabelValue("---");

        JPanel pnlNgay = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); pnlNgay.setBackground(Color.WHITE);
        pnlNgay.add(taoLabelTitle("Ngày mua: ")); pnlNgay.add(lblNgayMua);

        JPanel pnlKhach = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); pnlKhach.setBackground(Color.WHITE);
        pnlKhach.setBorder(new EmptyBorder(0, 55, 0, 0)); 
        pnlKhach.add(taoLabelTitle("Khách hàng: ")); pnlKhach.add(lblKhachHang);

        JPanel pnlNV = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); pnlNV.setBackground(Color.WHITE);
        pnlNV.add(taoLabelTitle("NV bán hàng: ")); pnlNV.add(lblNhanVien);

        JPanel pnlHD = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); pnlHD.setBackground(Color.WHITE);
        pnlHD.setBorder(new EmptyBorder(0, 55, 0, 0)); 
        pnlHD.add(taoLabelTitle("Số HĐ: ")); pnlHD.add(lblSoHD);

        pnlInfo.add(pnlNgay); pnlInfo.add(pnlKhach);
        pnlInfo.add(pnlNV); pnlInfo.add(pnlHD);

        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Color.WHITE);
        pnlTop.add(pnlHeader, BorderLayout.NORTH);
        pnlTop.add(pnlInfo, BorderLayout.CENTER);
        pnlTop.add(taoDuongKeNgang(), BorderLayout.SOUTH);

        String[] columns = {"Tên mặt hàng", "SL", "Đơn giá", "Giảm giá", "Thành tiền"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblChiTiet = new JTable(model);
        tblChiTiet.setFont(fontThuong);
        tblChiTiet.setRowHeight(26); 
        tblChiTiet.setShowGrid(false); 
        
        // ĐÃ FIX AN TOÀN: Bọc DefaultRenderer lại để vừa căn giữa, vừa giữ được vạch kẻ | của hệ thống
        tblChiTiet.getTableHeader().setFont(fontDam);
        TableCellRenderer baseRenderer = tblChiTiet.getTableHeader().getDefaultRenderer();
        tblChiTiet.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = baseRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                }
                return c;
            }
        });

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

     // ĐÃ FIX: Tăng bề ngang cột Tên lên 290 để ăn gian hết phần khoảng trống mới nới ra
        tblChiTiet.getColumnModel().getColumn(0).setPreferredWidth(250); // Cột Tên SP (giảm đi 1 chút)
        tblChiTiet.getColumnModel().getColumn(1).setPreferredWidth(40);
        tblChiTiet.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblChiTiet.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblChiTiet.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        tblChiTiet.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblChiTiet.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tblChiTiet.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblChiTiet.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        
        JScrollPane scroll = new JScrollPane(tblChiTiet);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        // Bảo vệ chiều cao bảng không bị bóp nghẹt
        scroll.setPreferredSize(new Dimension(500, 200));

        JPanel pnlFooter = new JPanel();
        pnlFooter.setLayout(new BoxLayout(pnlFooter, BoxLayout.Y_AXIS));
        pnlFooter.setBackground(Color.WHITE);

        JPanel pnlTong = new JPanel(new BorderLayout());
        pnlTong.setBackground(Color.WHITE);
        
        FlowLayout flowTrai = new FlowLayout(FlowLayout.LEFT, 0, 0);
        flowTrai.setAlignOnBaseline(true); 
        
        JPanel pnlTongTrai = new JPanel(flowTrai);
        pnlTongTrai.setBackground(Color.WHITE);
        
        lblTongSL = new JLabel("0"); lblTongSL.setFont(fontDam);
        JLabel lblTextTongSL = new JLabel("Tổng số lượng: "); lblTextTongSL.setFont(fontThuong);
        
        pnlTongTrai.add(lblTextTongSL); 
        pnlTongTrai.add(lblTongSL);

        JPanel pnlTongPhai = new JPanel(new GridLayout(4, 1, 0, 5));
        pnlTongPhai.setBackground(Color.WHITE);
        pnlTongPhai.setBorder(new EmptyBorder(0, 250, 0, 0));

        lblTongCong = taoLabelValue("0"); 
        lblGiamGiaBac = taoLabelValue("0"); 
        lblTruTichLuy = taoLabelValue("0");
        lblGiamGiaBacTitle = new JLabel("Giảm giá Bậc (...):");
        
        lblTienCanTra = taoLabelValue("0"); 
        lblTienCanTra.setFont(fontDam); 
        
        pnlTongPhai.add(createRow(new JLabel("Tổng cộng:"), lblTongCong));
        pnlTongPhai.add(createRow(lblGiamGiaBacTitle, lblGiamGiaBac));
        pnlTongPhai.add(createRow(new JLabel("Trừ tích lũy:"), lblTruTichLuy));
        
        JPanel rowCanTra = new JPanel(new BorderLayout()); 
        rowCanTra.setBackground(Color.WHITE);
        JLabel lblTitleCanTra = new JLabel("Tiền cần trả:"); 
        lblTitleCanTra.setFont(fontDam); 
        
        rowCanTra.add(lblTitleCanTra, BorderLayout.WEST); 
        rowCanTra.add(lblTienCanTra, BorderLayout.EAST);
        pnlTongPhai.add(rowCanTra);

        pnlTong.add(pnlTongTrai, BorderLayout.WEST);
        pnlTong.add(pnlTongPhai, BorderLayout.CENTER);

        JPanel pnlThanhToan = new JPanel(new GridLayout(4, 1, 0, 5));
        pnlThanhToan.setBackground(Color.WHITE);
        
        lblPhuongThucTitle = new JLabel("Phương thức thanh toán"); lblPhuongThucTitle.setFont(fontDam);
        lblPhuongThucTen = new JLabel("   TIỀN MẶT"); lblPhuongThucTen.setFont(fontThuong);
        lblTienKhach = taoLabelValue("0");
        lblTienThua = taoLabelValue("0");
        lblCongTichLuy = new JLabel("0", SwingConstants.RIGHT); lblCongTichLuy.setFont(fontThuong);

        pnlThanhToan.add(lblPhuongThucTitle);
        pnlThanhToan.add(createRow(lblPhuongThucTen, lblTienKhach));
        pnlThanhToan.add(createRow(new JLabel("   Tiền thừa trả lại:"), lblTienThua));
        pnlThanhToan.add(createRow(new JLabel("   Cộng tích lũy:"), lblCongTichLuy));

        JPanel pnlBangChu = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlBangChu.setBackground(Color.WHITE);
        pnlBangChu.setBorder(new EmptyBorder(5, 0, 10, 0)); 

        lblBangChu = new JLabel("Bằng chữ: ---", SwingConstants.RIGHT); 
        lblBangChu.setFont(fontThuong.deriveFont(Font.ITALIC, 14f));
        pnlBangChu.add(lblBangChu);

        JPanel pnlLoiChao = new JPanel(new GridLayout(2, 1, 0, 5));
        pnlLoiChao.setBackground(Color.WHITE);
        pnlLoiChao.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        JLabel lblNote = new JLabel("Cảm ơn quý khách hàng đã mua hàng", SwingConstants.CENTER);
        lblNote.setFont(fontThuong); 
        
        JLabel lblThanks = new JLabel("Rất mong nhận được sự góp ý về dịch vụ!", SwingConstants.CENTER);
        lblThanks.setFont(fontThuong); 

        pnlLoiChao.add(lblNote);
        pnlLoiChao.add(lblThanks);

        pnlFooter.add(taoDuongKeNgang());
        pnlFooter.add(pnlTong);
        pnlFooter.add(taoDuongKeNgang());
        pnlFooter.add(pnlThanhToan);
        pnlFooter.add(pnlBangChu); 
        pnlFooter.add(taoDuongKeNgang());
        pnlFooter.add(pnlLoiChao); 

        pnlMain.add(pnlTop, BorderLayout.NORTH);
        pnlMain.add(scroll, BorderLayout.CENTER);
        pnlMain.add(pnlFooter, BorderLayout.SOUTH);

        add(pnlMain, BorderLayout.CENTER);
    }

    private JPanel taoDuongKeNgang() {
        JPanel pnlLine = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.LIGHT_GRAY);
                Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0);
                g2d.setStroke(dashed);
                g2d.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
                g2d.dispose();
            }
        };
        pnlLine.setBackground(Color.WHITE);
        pnlLine.setPreferredSize(new Dimension(0, 20)); 
        return pnlLine;
    }

    private JLabel taoLabelTitle(String text) {
        JLabel lbl = new JLabel(text); lbl.setFont(fontDam); lbl.setForeground(TEXT_MAIN); return lbl;
    }
    
    private JLabel taoLabelValue(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.RIGHT); lbl.setFont(fontThuong); lbl.setForeground(TEXT_MAIN); return lbl;
    }
    
    private JPanel createRow(JLabel title, JLabel value) {
        JPanel row = new JPanel(new BorderLayout()); row.setBackground(Color.WHITE);
        title.setFont(fontThuong); row.add(title, BorderLayout.WEST); row.add(value, BorderLayout.EAST); return row;
    }

    public void setDuLieuHoaDon(String maHD, String tenNV, String tenKH, String bacKH, BigDecimal khachDua, Object[][] items, 
                                BigDecimal giamGiaHang, BigDecimal truTichLuy, int congTichLuy, boolean isTienMat) {
        lblSoHD.setText(maHD);
        lblNhanVien.setText(tenNV);
        lblKhachHang.setText(tenKH);
        lblNgayMua.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        model.setRowCount(0);
        BigDecimal tongTienHang = BigDecimal.ZERO;
        int tongSL = 0;

        for (Object[] item : items) {
            BigDecimal donGia = (BigDecimal) item[3];
            int sl = (int) item[2];
            BigDecimal thanhTienSP = (BigDecimal) item[4];
            
            // Xử lý hiển thị cột Giảm giá
            String giamGiaHienThi = "0 đ";
            
            // Nếu dữ liệu đẩy từ BanHangUi sang (mảng có độ dài > 7) -> Lấy chuỗi đã format sẵn
            if (item.length > 7 && item[7] != null) {
                giamGiaHienThi = item[7].toString();
            } else {
                // Fallback dự phòng: Tự tính toán lại nếu chạy bằng hàm main() (dữ liệu giả)
                BigDecimal tongGiaGocSP = donGia.multiply(new BigDecimal(sl));
                BigDecimal giamGiaSP = tongGiaGocSP.subtract(thanhTienSP);
                if (giamGiaSP.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal phanTram = giamGiaSP.multiply(new BigDecimal("100"))
                                                   .divide(tongGiaGocSP, 0, java.math.RoundingMode.HALF_UP);
                    giamGiaHienThi = DinhDangUtil.dinhDangSo(giamGiaSP) + " đ (" + phanTram + "%)";
                }
            }

            model.addRow(new Object[]{ 
                item[0], 
                sl, 
                DinhDangUtil.dinhDangSo(donGia) + " đ",  // Cột Đơn giá (Giá thực tế gốc)
                giamGiaHienThi,                          // Cột Giảm giá (Tiền giảm kèm %)
                DinhDangUtil.dinhDangSo(thanhTienSP) + " đ" // Cột Thành tiền (Sau khi giảm)
            });
            tongSL += sl;
            tongTienHang = tongTienHang.add(thanhTienSP);
        }

        BigDecimal tongCanThanhToan = tongTienHang.subtract(giamGiaHang).subtract(truTichLuy);
        if(tongCanThanhToan.compareTo(BigDecimal.ZERO) < 0) tongCanThanhToan = BigDecimal.ZERO;

        lblTongSL.setText(String.valueOf(tongSL));
        lblTongCong.setText(DinhDangUtil.dinhDangSo(tongTienHang)); 
        
        lblGiamGiaBacTitle.setText("Giảm giá Bậc (" + bacKH + "):");
        lblGiamGiaBac.setText(giamGiaHang.compareTo(BigDecimal.ZERO) > 0 ? "-" + DinhDangUtil.dinhDangSo(giamGiaHang) : "0 đ");
        lblTruTichLuy.setText(truTichLuy.compareTo(BigDecimal.ZERO) > 0 ? "-" + DinhDangUtil.dinhDangSo(truTichLuy) : "0 đ");
        lblTienCanTra.setText(DinhDangUtil.dinhDangSo(tongCanThanhToan));
        lblPhuongThucTen.setText(isTienMat ? "   TIỀN MẶT" : "   CHUYỂN KHOẢN");
        lblTienKhach.setText(DinhDangUtil.dinhDangSo(khachDua));
        
        BigDecimal thua = khachDua.subtract(tongCanThanhToan);
        lblTienThua.setText(thua.compareTo(BigDecimal.ZERO) > 0 ? DinhDangUtil.dinhDangSo(thua) : "0 đ");
        
        lblCongTichLuy.setText(String.valueOf(congTichLuy));
        lblBangChu.setText("Bằng chữ: " + DinhDangUtil.docSoThanhChu(tongCanThanhToan.longValue()));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        
        JFrame f = new JFrame("Chi Tiết Hóa Đơn"); 
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // ĐÃ FIX: Kéo rộng cửa sổ ngoài lên 730
        f.setSize(730, 920); 
        f.getContentPane().setBackground(new Color(243, 244, 246));
        f.setLayout(new GridBagLayout()); 
        
        ChiTietHoaDonUi ui = new ChiTietHoaDonUi(); 
        // ĐÃ FIX: Kéo rộng tờ hóa đơn lên 650px (đụng tới đường kẻ đỏ của sếp)
        ui.setPreferredSize(new Dimension(650, 840)); 
        
        Object[][] testData = { 
            {"NGK SPRITE SLEEK 320ML*6", "Gói", 4, new BigDecimal("35000"), new BigDecimal("140000")}, 
            {"MILO UHT KG MANG CO", "Lốc", 1, new BigDecimal("315000"), new BigDecimal("300000")} 
        };
        ui.setDuLieuHoaDon("HD_AUTO", "Thu Ngân 1", "Khách vãng lai", "Vàng", new BigDecimal("500000"), testData, new BigDecimal("10000"), new BigDecimal("0"), 4549, true);
        
        f.add(ui); 
        f.setLocationRelativeTo(null); 
        f.setVisible(true);
    }
}