package GUI.HoTro;

import Dao.TruyVanSieuTocDAO;
import Dao.TruyVanSieuTocDAO.ChiTietGiamGiaDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChiTietGiamGia extends JDialog {

    private final Color COLOR_WHITE = new Color(255, 255, 255);
    private final Color TEXT_MAIN = new Color(15, 23, 42);
    private final Color TEXT_SUB = new Color(100, 116, 139);
    private final Color BORDER_COLOR = new Color(226, 232, 240);
    private final Color BG_LIGHT = new Color(248, 250, 252);
    
    private final Font FONT_BOLD = new Font("Calibri", Font.BOLD, 15);
    private final Font FONT_REGULAR = new Font("Calibri", Font.PLAIN, 14);

    private float opacityValue = 0.0f;
    private Timer fadeInTimer;

    // SỬA: Thêm tham số maGG vào constructor
    public ChiTietGiamGia(JFrame parent, String maGG, String tenGG, String loaiGG, String trangThai, String thoiGian) {
        super(parent, true);
        setUndecorated(true);
        setSize(850, 600);
        setLocationRelativeTo(parent);
        setBackground(new Color(0, 0, 0, 0)); 

        // 🔥 GỌI XUỐNG DB ĐỂ LẤY DỮ LIỆU CỦA MÃ NÀY
        List<ChiTietGiamGiaDTO> listChiTiet = TruyVanSieuTocDAO.getInstance().layChiTietApDungGiamGia(maGG);

        // 🔥 TÍNH TOÁN CÁC THÔNG SỐ THỐNG KÊ (Dùng Stream API cho nhanh)
        int tongHoaDon = (int) listChiTiet.stream().map(d -> d.maHD).distinct().count();
        int tongSP = listChiTiet.stream().mapToInt(d -> d.soLuong).sum();
        BigDecimal tongTienGiam = listChiTiet.stream().map(d -> d.tienGiam).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal doanhThu = listChiTiet.stream().map(d -> d.thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);

        JPanel pnlWrapper = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0, 30));
                g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 20, 20);
                g2.setColor(COLOR_WHITE);
                g2.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 20, 20); 
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-4, getHeight()-4, 20, 20); 
                g2.dispose();
            }
        };
        pnlWrapper.setOpaque(false);
        pnlWrapper.setLayout(new BorderLayout(0, 20)); 
        pnlWrapper.setBorder(new EmptyBorder(25, 30, 25, 30)); 

        // Truyền các chỉ số thống kê vào Header
        pnlWrapper.add(taoHeader(tenGG, loaiGG, thoiGian, tongHoaDon, tongSP, tongTienGiam, doanhThu), BorderLayout.NORTH);
        
        // Truyền list dữ liệu vào Bảng
        pnlWrapper.add(taoBangChiTiet(listChiTiet), BorderLayout.CENTER);
        
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(FONT_BOLD);
        btnClose.setBackground(BG_LIGHT);
        btnClose.setForeground(TEXT_MAIN);
        btnClose.setFocusPainted(false);
        btnClose.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(8, 25, 8, 25)));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); 
        pnlBottom.setOpaque(false);
        pnlBottom.add(btnClose);
        pnlWrapper.add(pnlBottom, BorderLayout.SOUTH);

        setContentPane(pnlWrapper);

        setOpacity(0.0f);
        fadeInTimer = new Timer(15, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                opacityValue += 0.05f;
                if (opacityValue >= 1.0f) {
                    opacityValue = 1.0f;
                    fadeInTimer.stop();
                }
                setOpacity(opacityValue);
            }
        });
        fadeInTimer.start();
    }

    private JPanel taoHeader(String tenGG, String loaiGG, String thoiGian, int hd, int sp, BigDecimal tienGiam, BigDecimal dThu) {
        JPanel pnlTop = new JPanel(new BorderLayout(0, 20));
        pnlTop.setOpaque(false);

        JPanel pnlTitleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        pnlTitleArea.setOpaque(false);
        pnlTitleArea.setBorder(new EmptyBorder(0, -15, 0, 0)); 
        
        JLabel lblTitle = new JLabel("Chương trình: " + tenGG);
        lblTitle.setFont(new Font("Calibri", Font.BOLD, 24));
        lblTitle.setForeground(TEXT_MAIN);

        JLabel lblBadge = new JLabel(loaiGG);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(new Color(238, 242, 255));
        lblBadge.setForeground(new Color(79, 70, 229));
        lblBadge.setFont(FONT_BOLD.deriveFont(12f));
        lblBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(199, 210, 254)),
                new EmptyBorder(4, 10, 4, 10) 
        ));

        pnlTitleArea.add(lblTitle);
        pnlTitleArea.add(lblBadge);

        JLabel lblTime = new JLabel("Thời gian áp dụng: " + thoiGian);
        lblTime.setFont(FONT_REGULAR);
        lblTime.setForeground(TEXT_SUB);

        JPanel pnlHeaderLeft = new JPanel(new BorderLayout(0, 5));
        pnlHeaderLeft.setOpaque(false);
        pnlHeaderLeft.add(pnlTitleArea, BorderLayout.NORTH);
        pnlHeaderLeft.add(lblTime, BorderLayout.CENTER);

        // Gắn thông số linh động vào 4 thẻ Mini
        JPanel pnlMiniStats = new JPanel(new GridLayout(1, 4, 15, 0));
        pnlMiniStats.setOpaque(false);
        pnlMiniStats.add(taoMiniCard("Hóa đơn AD", hd + ""));
        pnlMiniStats.add(taoMiniCard("Sản phẩm", sp + ""));
        
        String tgFormat = (tienGiam.compareTo(BigDecimal.ZERO) == 0) ? "0 đ" : GUI.HoTro.DinhDangUtil.dinhDangTien(tienGiam);
        String dtFormat = (dThu.compareTo(BigDecimal.ZERO) == 0) ? "0 đ" : GUI.HoTro.DinhDangUtil.dinhDangTien(dThu);
        
        pnlMiniStats.add(taoMiniCard("Tổng tiền giảm", tgFormat));
        pnlMiniStats.add(taoMiniCard("Doanh thu", dtFormat));

        pnlTop.add(pnlHeaderLeft, BorderLayout.NORTH);
        pnlTop.add(pnlMiniStats, BorderLayout.CENTER);

        return pnlTop;
    }

    private JPanel taoMiniCard(String title, String value) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_LIGHT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblT = new JLabel(title);
        lblT.setFont(FONT_REGULAR.deriveFont(13f));
        lblT.setForeground(TEXT_SUB);

        JLabel lblV = new JLabel(value);
        lblV.setFont(FONT_BOLD.deriveFont(18f));
        lblV.setForeground(TEXT_MAIN);

        card.add(lblT);
        card.add(lblV);
        return card;
    }

    private JPanel taoBangChiTiet(List<ChiTietGiamGiaDTO> list) {
        JPanel pnlTable = new JPanel(new BorderLayout());
        pnlTable.setOpaque(false);
        pnlTable.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                "Danh sách giao dịch (" + list.size() + ")", 
                0, 0, FONT_BOLD, TEXT_SUB));

        String[] cols = {"STT", "Hóa đơn", "Sản phẩm", "Khách hàng", "Ngày", "SL", "Thành tiền", "Tiền giảm"};
        
        // 🔥 ĐỔ DỮ LIỆU ĐỘNG TỪ DB VÀO BẢNG
        Object[][] data = new Object[list.size()][8];
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (int i = 0; i < list.size(); i++) {
            ChiTietGiamGiaDTO d = list.get(i);
            data[i][0] = i + 1;
            data[i][1] = d.maHD;
            data[i][2] = d.tenSP;
            data[i][3] = d.tenKH;
            data[i][4] = (d.ngayTao != null) ? d.ngayTao.format(dtf) : "";
            data[i][5] = d.soLuong;
            data[i][6] = GUI.HoTro.DinhDangUtil.dinhDangTien(d.thanhTien);
            data[i][7] = GUI.HoTro.DinhDangUtil.dinhDangTien(d.tienGiam);
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(FONT_REGULAR);
        table.setForeground(TEXT_MAIN);
        table.setRowHeight(35);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setSelectionForeground(TEXT_MAIN);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(BG_LIGHT);
        header.setForeground(TEXT_SUB);
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(110); // Nới rộng chút cho hiển thị giờ
        table.getColumnModel().getColumn(5).setPreferredWidth(40);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);
        table.getColumnModel().getColumn(7).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scroll.getViewport().setBackground(COLOR_WHITE);

        pnlTable.add(scroll, BorderLayout.CENTER);
        return pnlTable;
    }
}