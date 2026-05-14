package GUI;

import GUI.HoTro.ChiTietGiamGia;
import Dao.TruyVanSieuTocDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LichSuGiamGiaUI extends JPanel {

    private final Color BG_MAIN = new Color(248, 250, 252);     
    private final Color COLOR_WHITE = new Color(255, 255, 255);
    private final Color TEXT_MAIN = new Color(15, 23, 42);      
    private final Color TEXT_SUB = new Color(100, 116, 139);    
    private final Color BORDER_COLOR = new Color(226, 232, 240);
    private final Color ACCENT_BLUE = new Color(59, 130, 246);  
    private final Color ACCENT_HOVER = new Color(239, 246, 255);

    private final Color ST_ACTIVE_BG = new Color(220, 252, 231);
    private final Color ST_ACTIVE_FG = new Color(22, 163, 74);
    private final Color ST_PENDING_BG = new Color(254, 243, 199);
    private final Color ST_PENDING_FG = new Color(217, 119, 6);
    private final Color ST_ENDED_BG = new Color(241, 245, 249);
    private final Color ST_ENDED_FG = new Color(100, 116, 139);

    private final Font FONT_BOLD = new Font("Calibri", Font.BOLD, 15);
    private final Font FONT_REGULAR = new Font("Calibri", Font.PLAIN, 14);
    private final Font FONT_TITLE = new Font("Calibri", Font.BOLD, 22);

    private JPanel pnlListContainer;
    private JPanel pnlRightStats;
    
    // Đưa txtSearch ra làm biến toàn cục để truy cập từ hàm tải dữ liệu
    private JTextField txtSearch;
    private final String PLACEHOLDER = " Tìm theo mã, tên SP...";

    public LichSuGiamGiaUI() {
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        pnlRightStats = new JPanel();
        pnlRightStats.setLayout(new BoxLayout(pnlRightStats, BoxLayout.Y_AXIS));
        pnlRightStats.setBackground(BG_MAIN);
        pnlRightStats.setPreferredSize(new Dimension(260, 0));

        add(taoTopBar(), BorderLayout.NORTH);
        add(taoCenterContent(), BorderLayout.CENTER);
        add(pnlRightStats, BorderLayout.EAST);

        taiDuLieuThucTe();
    }

    private void taiDuLieuThucTe() {
        // ✨ LẤY VÀ BỎ DẤU TỪ KHÓA TÌM KIẾM
        String tuKhoaRaw = (txtSearch != null) ? txtSearch.getText() : "";
        final String tuKhoa = tuKhoaRaw.equals(PLACEHOLDER) ? "" 
                : GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tuKhoaRaw.trim().toLowerCase());

        SwingWorker<Object[], Void> worker = new SwingWorker<>() {
            @Override
            protected Object[] doInBackground() {
                // Lấy dữ liệu từ DB ở luồng nền
                List<TruyVanSieuTocDAO.LichSuGiamGiaDTO> rawData = TruyVanSieuTocDAO.getInstance().layLichSuGiamGiaSieuToc();
                TruyVanSieuTocDAO.ThongKeGiamGiaDTO thongKe = TruyVanSieuTocDAO.getInstance().layThongKeTongQuanGiamGia();
                
                List<TruyVanSieuTocDAO.LichSuGiamGiaDTO> filteredData = new java.util.ArrayList<>();
                int dangHoatDong = 0;

                for (TruyVanSieuTocDAO.LichSuGiamGiaDTO d : rawData) {
                    if ("Đang hoạt động".equals(d.trangThai)) dangHoatDong++;
                    
                    // ✨ ÁP DỤNG THUẬT TOÁN TÌM KIẾM TỪ DANHSACHSP
                    if (!tuKhoa.isEmpty()) {
                        String maGG = (d.maGiamGia != null) ? GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(d.maGiamGia.toLowerCase()) : "";
                        String ten = (d.tenSP != null) ? GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(d.tenSP.toLowerCase()) : "";
                        
                        // So sánh các chuỗi đã bị loại bỏ dấu
                        if (!maGG.contains(tuKhoa) && !ten.contains(tuKhoa)) {
                            continue; 
                        }
                    }
                    filteredData.add(d);
                }
                // Trả về 1 mảng chứa các kết quả đã lọc
                return new Object[]{filteredData, rawData.size(), dangHoatDong, thongKe};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                // Đổ dữ liệu lên UI
                try {
                    Object[] result = get();
                    List<TruyVanSieuTocDAO.LichSuGiamGiaDTO> filteredData = (List<TruyVanSieuTocDAO.LichSuGiamGiaDTO>) result[0];
                    int tongSo = (int) result[1];
                    int dangHoatDong = (int) result[2];
                    TruyVanSieuTocDAO.ThongKeGiamGiaDTO thongKe = (TruyVanSieuTocDAO.ThongKeGiamGiaDTO) result[3];

                    pnlListContainer.removeAll();
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    
                    for (TruyVanSieuTocDAO.LichSuGiamGiaDTO d : filteredData) {
                        String tg = (d.batDau != null ? d.batDau.format(dtf) : "...") + " - " + (d.ketThuc != null ? d.ketThuc.format(dtf) : "...");
                        String mucGiam = "0";
                        if (d.giamGia != null) {
                            if (d.giamGia.compareTo(new BigDecimal("100")) <= 0) mucGiam = d.giamGia.stripTrailingZeros().toPlainString() + "%";
                            else mucGiam = GUI.HoTro.DinhDangUtil.dinhDangTien(d.giamGia);
                        }
                        pnlListContainer.add(taoDongGiamGia(d.maGiamGia, d.tenSP, tg, mucGiam, d.loaiGiamGia, d.trangThai, String.valueOf(d.soLuong)));
                    }

                    capNhatTheThongKe(tongSo, dangHoatDong, thongKe.tongLuotApDung, thongKe.tongTienGiam);
                    pnlListContainer.revalidate();
                    pnlListContainer.repaint();
                } catch (Exception e) {}
            }
        };
        worker.execute();
    }

    private void capNhatTheThongKe(int tong, int hoatDong, int luot, BigDecimal tienGiam) {
        pnlRightStats.removeAll();
        pnlRightStats.add(taoTheThongKe("Tổng số giảm giá", tong + " Chương trình", "📦", new Color(238, 242, 255), new Color(79, 70, 229)));
        pnlRightStats.add(Box.createVerticalStrut(15));
        pnlRightStats.add(taoTheThongKe("Đang hoạt động", hoatDong + " Chương trình", "⚡", ST_ACTIVE_BG, ST_ACTIVE_FG));
        pnlRightStats.add(Box.createVerticalStrut(15));
        pnlRightStats.add(taoTheThongKe("Tổng lượt áp dụng", luot + " Lượt", "🔥", new Color(255, 237, 213), new Color(234, 88, 12)));
        pnlRightStats.add(Box.createVerticalStrut(15));
        
        String tienStr = (tienGiam != null && tienGiam.compareTo(BigDecimal.ZERO) > 0) 
                         ? GUI.HoTro.DinhDangUtil.dinhDangTien(tienGiam) 
                         : "0 đ";
        pnlRightStats.add(taoTheThongKe("Tổng tiền đã giảm", tienStr, "💎", new Color(224, 242, 254), new Color(2, 132, 199)));
        
        pnlRightStats.revalidate();
        pnlRightStats.repaint();
    }

    private JPanel taoTopBar() {
        JPanel pnlTop = new JPanel(new BorderLayout(20, 0));
        pnlTop.setBackground(BG_MAIN);

        JLabel lblTitle = new JLabel("Lịch sử giảm giá");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(TEXT_MAIN);

        JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlControls.setBackground(BG_MAIN);

        txtSearch = new JTextField(PLACEHOLDER);
        txtSearch.setPreferredSize(new Dimension(220, 36));
        txtSearch.setForeground(TEXT_SUB);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));

        // ✨ Xử lý Placeholder (chữ mờ)
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtSearch.getText().equals(PLACEHOLDER)) {
                    txtSearch.setText("");
                    txtSearch.setForeground(TEXT_MAIN);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtSearch.getText().isEmpty()) {
                    txtSearch.setText(PLACEHOLDER);
                    txtSearch.setForeground(TEXT_SUB);
                }
            }
        });

        // ✨ NỐI DÂY: Tìm kiếm mượt mà (Debounce 300ms)
        Timer searchTimer = new Timer(300, e -> taiDuLieuThucTe());
        searchTimer.setRepeats(false);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { searchTimer.restart(); }
            public void changedUpdate(DocumentEvent e) { searchTimer.restart(); }
        });

        JButton btnRefresh = taoNutBoGoc("Làm mới", ACCENT_BLUE, COLOR_WHITE, ACCENT_BLUE);
        btnRefresh.addActionListener(e -> {
            txtSearch.setText(PLACEHOLDER);
            txtSearch.setForeground(TEXT_SUB);
            taiDuLieuThucTe();
        });

        pnlControls.add(txtSearch);
        pnlControls.add(btnRefresh);

        pnlTop.add(lblTitle, BorderLayout.WEST);
        pnlTop.add(pnlControls, BorderLayout.EAST);
        return pnlTop;
    }

    private JPanel taoCenterContent() {
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 0));
        pnlCenter.setBackground(BG_MAIN);

        JPanel pnlHeaderRow = new JPanel(new GridBagLayout());
        pnlHeaderRow.setBackground(BG_MAIN);
        pnlHeaderRow.setBorder(new EmptyBorder(10, 35, 10, 35)); 
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        themO(pnlHeaderRow, taoLabelCol("MÃ GIẢM GIÁ", 100, SwingConstants.CENTER), gbc, 0, 0.15);
        themO(pnlHeaderRow, taoLabelCol("SẢN PHẨM", 200, SwingConstants.LEFT), gbc, 1, 0.25);  
        themO(pnlHeaderRow, taoLabelCol("THỜI GIAN", 160, SwingConstants.CENTER), gbc, 2, 0.20);
        themO(pnlHeaderRow, taoLabelCol("MỨC GIẢM", 80, SwingConstants.CENTER), gbc, 3, 0.10);
        themO(pnlHeaderRow, taoLabelCol("LOẠI", 100, SwingConstants.CENTER), gbc, 4, 0.10);
        themO(pnlHeaderRow, taoLabelCol("TRẠNG THÁI", 120, SwingConstants.CENTER), gbc, 5, 0.15);
        themO(pnlHeaderRow, taoLabelCol("SL", 50, SwingConstants.CENTER), gbc, 6, 0.05);
        themO(pnlHeaderRow, new JLabel(" "), gbc, 7, 0.0); 
        
        pnlListContainer = new JPanel();
        pnlListContainer.setLayout(new BoxLayout(pnlListContainer, BoxLayout.Y_AXIS));
        pnlListContainer.setBackground(BG_MAIN);
        pnlListContainer.setBorder(new EmptyBorder(0, 20, 0, 20)); 

        JScrollPane scrollPane = new JScrollPane(pnlListContainer);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_MAIN);
        
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setOpaque(false);

        scrollPane.setColumnHeaderView(pnlHeaderRow);
        scrollPane.getColumnHeader().setBackground(BG_MAIN);

        pnlCenter.add(scrollPane, BorderLayout.CENTER);
        return pnlCenter;
    }

    private void themO(JPanel p, Component c, GridBagConstraints g, int x, double w) {
        g.gridx = x; g.weightx = w; p.add(c, g);
    }

    private JLabel taoLabelCol(String t, int w, int align) {
        JLabel l = new JLabel(t, align);
        l.setFont(FONT_BOLD.deriveFont(13f)); l.setForeground(TEXT_SUB);
        l.setPreferredSize(new Dimension(w, 30)); return l;
    }

    private JLabel taoLabelText(String t, int w, Font f, Color c, int align) {
        JLabel l = new JLabel(t, align); 
        l.setFont(f); l.setForeground(c);
        l.setPreferredSize(new Dimension(w, 30)); return l;
    }

    private JPanel taoDongGiamGia(String ma, String ten, String tg, String mucGiam, String loai, String trangThai, String sl) {
        RoundedPanel row = new RoundedPanel(12, COLOR_WHITE);
        row.setLayout(new GridBagLayout()); 
        row.setBorder(new EmptyBorder(10, 15, 10, 15));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1.0;

        Color bgSt = ST_ENDED_BG; Color fgSt = ST_ENDED_FG;
        if(trangThai.equals("Đang hoạt động")) { bgSt = ST_ACTIVE_BG; fgSt = ST_ACTIVE_FG; }
        else if(trangThai.equals("Sắp diễn ra")) { bgSt = ST_PENDING_BG; fgSt = ST_PENDING_FG; }

        RoundedPanel pnlStatus = new RoundedPanel(6, bgSt);
        pnlStatus.setLayout(new BorderLayout());
        JLabel lblSt = new JLabel(trangThai, SwingConstants.CENTER);
        lblSt.setFont(FONT_BOLD.deriveFont(12f)); lblSt.setForeground(fgSt);
        pnlStatus.add(lblSt, BorderLayout.CENTER);
        pnlStatus.setPreferredSize(new Dimension(110, 26));

        JPanel pnlStWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlStWrapper.setOpaque(false); pnlStWrapper.add(pnlStatus);

        JButton btnDetail = taoNutBoGoc("Chi tiết", COLOR_WHITE, ACCENT_BLUE, ACCENT_BLUE);
        btnDetail.setPreferredSize(new Dimension(85, 30));
        btnDetail.addActionListener(e -> new ChiTietGiamGia((JFrame) SwingUtilities.getWindowAncestor(this), ma, ten, loai, trangThai, tg).setVisible(true));

        themO(row, taoLabelText(ma, 100, FONT_BOLD, TEXT_MAIN, SwingConstants.CENTER), gbc, 0, 0.15);
        themO(row, taoLabelText(ten, 200, FONT_REGULAR, TEXT_MAIN, SwingConstants.LEFT), gbc, 1, 0.25);
        themO(row, taoLabelText(tg, 160, FONT_REGULAR, TEXT_SUB, SwingConstants.CENTER), gbc, 2, 0.20);
        themO(row, taoLabelText(mucGiam, 80, FONT_BOLD, new Color(239, 68, 68), SwingConstants.CENTER), gbc, 3, 0.10);
        themO(row, taoLabelText(loai, 100, FONT_REGULAR, TEXT_SUB, SwingConstants.CENTER), gbc, 4, 0.10);
        themO(row, pnlStWrapper, gbc, 5, 0.15);
        themO(row, taoLabelText(sl, 50, FONT_BOLD, TEXT_MAIN, SwingConstants.CENTER), gbc, 6, 0.05);
        themO(row, btnDetail, gbc, 7, 0.0);

        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(ACCENT_HOVER); }
            public void mouseExited(MouseEvent e) { row.setBackground(COLOR_WHITE); }
        });

        JPanel marginWrapper = new JPanel(new BorderLayout());
        marginWrapper.setOpaque(false);
        marginWrapper.add(row, BorderLayout.CENTER);
        marginWrapper.setBorder(new EmptyBorder(0, 0, 10, 0));

        return marginWrapper;
    }

    private JPanel taoTheThongKe(String t, String v, String i, Color bgI, Color fgI) {
        RoundedPanel c = new RoundedPanel(15, COLOR_WHITE);
        c.setLayout(new BorderLayout(15, 0)); c.setBorder(new EmptyBorder(15, 15, 15, 15));
        c.setMaximumSize(new Dimension(260, 80));
        JLabel lblI = new JLabel(i, SwingConstants.CENTER); lblI.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        lblI.setForeground(fgI);
        RoundedPanel pI = new RoundedPanel(10, bgI); pI.setPreferredSize(new Dimension(45, 45));
        pI.setLayout(new BorderLayout()); pI.add(lblI);
        JPanel pT = new JPanel(new GridLayout(2,1)); pT.setOpaque(false);
        pT.add(new JLabel(t)); 
        JLabel lblV = new JLabel(v);
        lblV.setFont(FONT_BOLD.deriveFont(18f)); lblV.setForeground(TEXT_MAIN);
        pT.add(lblV);
        c.add(pI, BorderLayout.WEST); c.add(pT, BorderLayout.CENTER);
        return c;
    }

    private JButton taoNutBoGoc(String t, Color b, Color f, Color br) {
        JButton btn = new JButton(t) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground()); g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setColor(br); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setBackground(b); btn.setForeground(f); btn.setFocusPainted(false);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        return btn;
    }

    class RoundedPanel extends JPanel {
        private int radius; private Color bgColor;
        public RoundedPanel(int r, Color bg) { this.radius = r; this.bgColor = bg; setOpaque(false); }
        public void setBackground(Color bg) { this.bgColor = bg; repaint(); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-2, radius, radius);
            g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-2, getHeight()-2, radius, radius);
            g2.dispose();
        }
    }

    class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(203, 213, 225));
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 10, 10);
            g2.dispose();
        }
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}
        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() {
            JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b;
        }
    }
    
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("PBL3 - Lịch Sử Giảm Giá");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800); frame.setLocationRelativeTo(null); 
            frame.add(new LichSuGiamGiaUI());
            frame.setVisible(true);
        });
    }
}