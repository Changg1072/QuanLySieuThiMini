package GUI;

import Dao.TruyVanSieuTocDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🎨 LỊCH CHIA CA MINI (Dùng dữ liệu siêu tốc)
 */
public class BangChiaCaUi extends JPanel {

    // ================== THIẾT LẬP FONT & MÀU SẮC (THEME) ==================
    private static final Font FONT_THUONG = new Font("Calibri", Font.PLAIN, 18);
    private static final Font FONT_DAM = new Font("Calibri", Font.BOLD, 20);

    private static final Color MAU_NEN_CHINH = new Color(248, 249, 250);
    private static final Color MAU_VIEN = new Color(222, 226, 230);
    private static final Color MAU_CHU_NGAY = new Color(73, 80, 87);
    private static final Color MAU_CUOI_TUAN = new Color(233, 236, 239); 

    private static final Color CA_SANG_BG = new Color(204, 235, 255); 
    private static final Color CA_SANG_FG = new Color(0, 86, 179);
    private static final Color CA_CHIEU_BG = new Color(128, 191, 255); 
    private static final Color CA_CHIEU_FG = new Color(0, 64, 133);
    private static final Color CA_TOI_BG = new Color(0, 123, 255);    
    private static final Color CA_TOI_FG = Color.WHITE;

    private YearMonth thangHienTai;
    private JLabel lblThangNam;
    private JPanel pnlLich;
    
    // 🔥 DATA DÙNG CACHE SIÊU TỐC TỪ DB
    private TruyVanSieuTocDAO.DuLieuChiaCaDTO dataCache;
    private Map<Integer, List<String>> mapCaLamTheoNgay = new HashMap<>();

    public BangChiaCaUi() {
        thangHienTai = YearMonth.now();
        khoiTaoGiaoDien();
        taiDuLieuVaVeLich();
    }

    // =======================================================
    // 🔥 LOAD DỮ LIỆU BẰNG ĐỘNG CƠ TURBO
    // =======================================================
    private void taiDuLieuVaVeLich() {
        SwingWorker<TruyVanSieuTocDAO.DuLieuChiaCaDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected TruyVanSieuTocDAO.DuLieuChiaCaDTO doInBackground() {
                return TruyVanSieuTocDAO.getInstance().loadToanBoLichChiaCa();
            }

            @Override
            protected void done() {
                try {
                    dataCache = get();
                    phanTichDuLieuLich();
                    veLich();
                } catch (Exception e) {
                    System.err.println("Lỗi load Lịch Mini: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void phanTichDuLieuLich() {
        mapCaLamTheoNgay.clear();
        if (dataCache == null) return;

        for (Data.ChiaCa cc : dataCache.dsChiaCa) {
            if (cc.getNgayLam() != null 
                && cc.getNgayLam().getYear() == thangHienTai.getYear() 
                && cc.getNgayLam().getMonthValue() == thangHienTai.getMonthValue()
                && !cc.getTinhTrang().equals("Đã hủy")) {
                
                int ngay = cc.getNgayLam().getDayOfMonth();
                Data.LoaiCa lc = dataCache.mapLoaiCa.get(cc.getMaLoaiCa());
                String tenCa = (lc != null) ? lc.getTenCa() : "Ca Unknown";
                
                List<String> caTrongNgay = mapCaLamTheoNgay.getOrDefault(ngay, new ArrayList<>());
                if (!caTrongNgay.contains(tenCa)) {
                    caTrongNgay.add(tenCa);
                    mapCaLamTheoNgay.put(ngay, caTrongNgay);
                }
            }
        }
    }

    private void khoiTaoGiaoDien() {
        setLayout(new BorderLayout(10, 10));
        setBackground(MAU_NEN_CHINH);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel pnlHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlHeader.setOpaque(false);

        JPanel pnlArrows = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlArrows.setOpaque(false);
        JButton btnTruoc = taoNutDieuHuong("←");
        JButton btnSau = taoNutDieuHuong("→");
        
        btnTruoc.addActionListener(e -> {
            thangHienTai = thangHienTai.minusMonths(1);
            phanTichDuLieuLich();
            veLich();
        });
        btnSau.addActionListener(e -> {
            thangHienTai = thangHienTai.plusMonths(1);
            phanTichDuLieuLich();
            veLich();
        });

        pnlArrows.add(btnTruoc);
        pnlArrows.add(btnSau);

        lblThangNam = new JLabel();
        lblThangNam.setOpaque(true);
        lblThangNam.setBackground(Color.WHITE);
        lblThangNam.setFont(FONT_THUONG);
        lblThangNam.setForeground(MAU_CHU_NGAY);
        lblThangNam.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        
        lblThangNam.setIcon(taoIconLich());
        lblThangNam.setHorizontalTextPosition(SwingConstants.LEFT); 
        lblThangNam.setIconTextGap(40); 

        pnlHeader.add(pnlArrows);
        pnlHeader.add(lblThangNam);

        JPanel pnlCenter = new JPanel(new BorderLayout(0, 0));
        pnlCenter.setOpaque(false);

        JPanel pnlThu = new JPanel(new GridLayout(1, 7, 1, 1)); 
        pnlThu.setBackground(MAU_VIEN);
        String[] cacThu = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật"};
        for (int i = 0; i < cacThu.length; i++) {
            JLabel lblThu = new JLabel(cacThu[i], SwingConstants.CENTER);
            lblThu.setFont(FONT_DAM);
            lblThu.setOpaque(true);
            lblThu.setBackground(Color.WHITE);
            if (i >= 5) lblThu.setForeground(new Color(220, 53, 69)); 
            else lblThu.setForeground(MAU_CHU_NGAY);
            lblThu.setBorder(new EmptyBorder(15, 0, 15, 0));
            pnlThu.add(lblThu);
        }

        pnlLich = new JPanel(new GridLayout(0, 7, 1, 1));
        pnlLich.setBackground(MAU_VIEN); 

        pnlCenter.add(pnlThu, BorderLayout.NORTH);
        pnlCenter.add(pnlLich, BorderLayout.CENTER);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
    }

    private void veLich() {
        pnlLich.removeAll();
        lblThangNam.setText("Tháng " + thangHienTai.getMonthValue() + ", " + thangHienTai.getYear());

        LocalDate ngayDauThang = thangHienTai.atDay(1);
        int soNgayCuaThang = thangHienTai.lengthOfMonth();
        int thuCuaNgayDauThang = ngayDauThang.getDayOfWeek().getValue(); 

        for (int i = 1; i < thuCuaNgayDauThang; i++) {
            pnlLich.add(taoOCongViec("", false));
        }

        for (int ngay = 1; ngay <= soNgayCuaThang; ngay++) {
            int thuHienTai = thangHienTai.atDay(ngay).getDayOfWeek().getValue();
            boolean laCuoiTuan = (thuHienTai == 6 || thuHienTai == 7);
            
            JPanel pnlNgay = taoOCongViec(String.valueOf(ngay), laCuoiTuan);
            
            // Vẽ thẻ Ca làm dựa trên Data thật
            if (mapCaLamTheoNgay.containsKey(ngay)) {
                for (String tenCa : mapCaLamTheoNgay.get(ngay)) {
                    pnlNgay.add(new TagCaLam(tenCa));
                }
            }
            pnlLich.add(pnlNgay);
        }

        int tongSoO = (thuCuaNgayDauThang - 1) + soNgayCuaThang;
        int soODuocThem = (tongSoO % 7 != 0) ? 7 - (tongSoO % 7) : 0;
        for(int i = 0; i < soODuocThem; i++){
            pnlLich.add(taoOCongViec("", false));
        }

        pnlLich.revalidate();
        pnlLich.repaint();
    }

    private JPanel taoOCongViec(String soNgay, boolean laCuoiTuan) {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBackground(soNgay.isEmpty() ? MAU_NEN_CHINH : (laCuoiTuan ? MAU_CUOI_TUAN : Color.WHITE));
        pnl.setBorder(new EmptyBorder(5, 5, 5, 5));

        if (!soNgay.isEmpty()) {
            JLabel lblSo = new JLabel(soNgay);
            lblSo.setFont(FONT_THUONG);
            lblSo.setForeground(MAU_CHU_NGAY);
            lblSo.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            if (thangHienTai.equals(YearMonth.now()) && soNgay.equals(String.valueOf(LocalDate.now().getDayOfMonth()))) {
                lblSo.setFont(FONT_DAM);
                lblSo.setForeground(CA_TOI_BG); 
            }
            pnl.add(lblSo);
            pnl.add(Box.createVerticalStrut(5)); 
        }
        return pnl;
    }

    private JButton taoNutDieuHuong(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14)); 
        btn.setPreferredSize(new Dimension(36, 36)); 
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(100, 100, 100));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210), 1));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(240, 240, 240)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private Icon taoIconLich() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(120, 120, 120)); 
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y + 2, 14, 12, 3, 3);
                g2.drawLine(x, y + 6, x + 14, y + 6);
                g2.drawLine(x + 3, y, x + 3, y + 3);
                g2.drawLine(x + 11, y, x + 11, y + 3);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        };
    }

    private class TagCaLam extends JPanel {
        private String tenCa;
        private Color bgColor, fgColor;

        public TagCaLam(String tenCa) {
            this.tenCa = tenCa;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); 

            if (tenCa.toLowerCase().contains("sáng")) {
                bgColor = CA_SANG_BG; fgColor = CA_SANG_FG;
            } else if (tenCa.toLowerCase().contains("chiều")) {
                bgColor = CA_CHIEU_BG; fgColor = CA_CHIEU_FG;
            } else {
                bgColor = CA_TOI_BG; fgColor = CA_TOI_FG;
            }
            
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setCursor(new Cursor(Cursor.HAND_CURSOR)); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 8, 8);
            g2.setColor(fgColor);
            g2.setFont(FONT_DAM.deriveFont(18f)); 
            FontMetrics fm = g2.getFontMetrics();
            int x = 8;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(tenCa, x, y);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() { return new Dimension(100, 28); }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
        catch (Exception e) {}
        JFrame frame = new JFrame("Demo Lịch Chia Ca - Real Database!");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);
        frame.add(new BangChiaCaUi());
        frame.setVisible(true);
    }
}