package GUI.HoTro;

import Dao.ConnectDB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 🚀 POPUP LỊCH SỬ CHI TIẾT 1 PHIẾU TIÊU HỦY
 */
public class LichSuTieuHuyDialog {

    public static void showModal(Component parentComponent, String maPhieuHuy) {
        Window parentWindow = SwingUtilities.getWindowAncestor(parentComponent);

        BufferedImage blurredBg = captureAndBlurScreen(parentWindow);
        JDialog overlay = new JDialog(parentWindow, Dialog.ModalityType.MODELESS);
        overlay.setUndecorated(true);
        if (parentWindow != null) overlay.setBounds(parentWindow.getBounds());
        overlay.setBackground(new Color(0, 0, 0, 0));

        JPanel pnlOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (blurredBg != null) g2.drawImage(blurredBg, 0, 0, getWidth(), getHeight(), null);
                g2.setColor(new Color(0, 0, 0, 100)); 
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        pnlOverlay.setOpaque(false);
        overlay.setContentPane(pnlOverlay);

        JDialog dialog = new JDialog(parentWindow, "Chi Tiết Tiêu Hủy", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel pnlMain = taoMainLayout(dialog, overlay, maPhieuHuy);
        dialog.setContentPane(pnlMain);
        dialog.pack();
        dialog.setLocationRelativeTo(parentWindow);

        pnlOverlay.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dongPopup(dialog, overlay); }
        });
        dialog.getRootPane().registerKeyboardAction(e -> dongPopup(dialog, overlay),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        overlay.setVisible(true);
        dialog.setOpacity(0f);
        chayAnimationFadeIn(dialog);
        dialog.setVisible(true);
    }

    private static JPanel taoMainLayout(JDialog dialog, JDialog overlay, String maPhieuHuy) {
        JPanel pnlMain = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(20, 25, 20, 25));
        pnlMain.setPreferredSize(new Dimension(950, 680));

        // --- HEADER ---
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)));

        JLabel lblTitle = new JLabel(" Chi tiết Phiếu tiêu hủy: " + maPhieuHuy);
        lblTitle.setIcon(new ImageIcon(taoIconTieuHuy(22, 22)));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 41, 59));
        lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnClose.setForeground(new Color(100, 116, 139));
        btnClose.setContentAreaFilled(false); btnClose.setBorderPainted(false); btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dongPopup(dialog, overlay));
        
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(btnClose, BorderLayout.EAST);

        // --- STATS CARDS ---
        JPanel pnlStats = new JPanel(new GridLayout(1, 2, 20, 0));
        pnlStats.setOpaque(false);
        
        JLabel lblTongSL = new JLabel("...");
        JLabel lblTongThietHai = new JLabel("...");
        
        pnlStats.add(taoCardThongKe("TỔNG SỐ LƯỢNG HỦY", lblTongSL, new Color(234, 88, 12))); // Màu Cam
        pnlStats.add(taoCardThongKe("TỔNG THIỆT HẠI", lblTongThietHai, new Color(220, 38, 38))); // Màu Đỏ

        // --- TABLE ---
        String[] cols = {"MÃ LÔ", "SẢN PHẨM", "SỐ LƯỢNG HỦY", "GIÁ TRỊ THIỆT HẠI", "LÝ DO"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        setupTableStyle(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(241, 245, 249)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        TienIchGiaoDien.thietLapThanhCuon(scrollPane);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // --- SEARCH ---
        JPanel pnlFilter = new JPanel(new BorderLayout());
        pnlFilter.setOpaque(false);
        TheBongDo.RoundedTextField txtSearch = new TheBongDo.RoundedTextField("🔍 Tìm kiếm sản phẩm, mã lô...", 20);
        txtSearch.setPreferredSize(new Dimension(350, 40));
        pnlFilter.add(txtSearch, BorderLayout.WEST);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void loc() {
                String text = txtSearch.getText().trim();
                if (text.isEmpty() || text.contains("Tìm kiếm")) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1)); 
            }
            public void insertUpdate(DocumentEvent e) { loc(); }
            public void removeUpdate(DocumentEvent e) { loc(); }
            public void changedUpdate(DocumentEvent e) { loc(); }
        });

        JPanel pnlTopWrap = new JPanel(new BorderLayout(0, 20));
        pnlTopWrap.setOpaque(false);
        pnlTopWrap.add(pnlStats, BorderLayout.NORTH);
        pnlTopWrap.add(pnlFilter, BorderLayout.CENTER);

        // --- FOOTER ---
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JLabel lblRecordCount = new JLabel("Đang tải dữ liệu...");
        lblRecordCount.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblRecordCount.setForeground(new Color(148, 163, 184));
        
        JPanel pnlActionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlActionBtns.setOpaque(false);
        
        JButton btnDong = taoNutBorder("Đóng");
        btnDong.addActionListener(e -> dongPopup(dialog, overlay));
        
        pnlActionBtns.add(btnDong);
        
        pnlFooter.add(lblRecordCount, BorderLayout.WEST);
        pnlFooter.add(pnlActionBtns, BorderLayout.EAST);

        // Lắp ráp
        pnlMain.add(pnlHeader, BorderLayout.NORTH);
        
        JPanel pnlCenterWrap = new JPanel(new BorderLayout(0, 20));
        pnlCenterWrap.setOpaque(false);
        pnlCenterWrap.add(pnlTopWrap, BorderLayout.NORTH);
        pnlCenterWrap.add(scrollPane, BorderLayout.CENTER);
        
        pnlMain.add(pnlCenterWrap, BorderLayout.CENTER);
        pnlMain.add(pnlFooter, BorderLayout.SOUTH);

        // --- TẢI DỮ LIỆU ---
        loadDataAsync(maPhieuHuy, model, lblTongSL, lblTongThietHai, lblRecordCount);

        return pnlMain;
    }

    private static void loadDataAsync(String maPhieuHuy, DefaultTableModel model, 
                                      JLabel lblTongSL, JLabel lblTongThietHai, JLabel lblRecordCount) {
        new SwingWorker<Void, Object[]>() {
            int tongSL = 0;
            BigDecimal tongThietHai = BigDecimal.ZERO;

            @Override
            protected Void doInBackground() throws Exception {
                String sql = "SELECT ct.MaLoHang, sp.TenSP, ct.SoLuongHuy, ct.GiaTriHuy, ct.LyDoChiTiet " +
                             "FROM ChiTietPhieuHuy ct " +
                             "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                             "WHERE ct.MaPhieuHuy = ?";

                try (Connection con = ConnectDB.getInstance().getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    
                    ps.setString(1, maPhieuHuy);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String maLo = rs.getString("MaLoHang");
                            String tenSP = rs.getString("TenSP");
                            int slHuy = rs.getInt("SoLuongHuy");
                            BigDecimal giaTri = rs.getBigDecimal("GiaTriHuy");
                            String lyDo = rs.getString("LyDoChiTiet");

                            tongSL += slHuy;
                            if (giaTri != null) tongThietHai = tongThietHai.add(giaTri);

                            publish(new Object[]{ maLo, tenSP, slHuy, giaTri, lyDo });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) model.addRow(row);
            }

            @Override
            protected void done() {
                lblTongSL.setText(String.format("%,d", tongSL));
                lblTongThietHai.setText(DinhDangUtil.dinhDangTien(tongThietHai));
                lblRecordCount.setText("Hiển thị " + model.getRowCount() + " mặt hàng bị tiêu hủy");
            }
        }.execute();
    }

    private static void setupTableStyle(JTable table) {
        table.setRowHeight(48);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(Color.WHITE);
        header.setForeground(new Color(148, 163, 184)); 
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                
                setHorizontalAlignment(col == 2 || col == 3 ? CENTER : LEFT);
                setBorder(new EmptyBorder(0, 15, 0, 15));
                
                if (!isSelected) setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 250, 252));
                else setBackground(new Color(254, 242, 242)); // Đỏ nhạt

                setFont(new Font("Segoe UI", Font.PLAIN, 14));

                if (col == 0) { // Mã lô 
                    setForeground(new Color(220, 38, 38));
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else if (col == 1) { // Tên sản phẩm
                    setForeground(new Color(15, 23, 42));
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else if (col == 2) { // Số lượng
                    setForeground(new Color(234, 88, 12));
                    setFont(new Font("Segoe UI", Font.BOLD, 15));
                } else if (col == 3) { // Giá trị
                    setForeground(new Color(15, 23, 42));
                    if (value instanceof BigDecimal) setText(DinhDangUtil.dinhDangTien((BigDecimal) value));
                } else if (col == 4) { // Lý do
                    setForeground(new Color(100, 116, 139));
                }
                return this;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(4).setPreferredWidth(250);
    }

    private static JPanel taoCardThongKe(String title, JLabel lblValue, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(5, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(254, 242, 242)); // Nền đỏ rất nhạt
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(252, 165, 165)); // Viền đỏ nhạt
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(220, 38, 38)); // Đỏ đậm

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(valueColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    private static JButton taoNutBorder(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                super.paintComponent(g); g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(new Color(71, 85, 105));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(80, 38)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static void dongPopup(JDialog dialog, JDialog overlay) { dialog.dispose(); overlay.dispose(); }
    private static void chayAnimationFadeIn(JDialog dialog) {
        Timer fadeIn = new Timer(10, new ActionListener() {
            float opacity = 0f;
            @Override public void actionPerformed(ActionEvent e) {
                opacity += 0.08f;
                if (opacity >= 1f) { opacity = 1f; ((Timer) e.getSource()).stop(); }
                dialog.setOpacity(opacity);
            }
        });
        fadeIn.start();
    }
    private static BufferedImage captureAndBlurScreen(Window parentWindow) {
        if (parentWindow == null || !parentWindow.isShowing()) return null;
        try {
            Robot robot = new Robot(); Rectangle rect = parentWindow.getBounds();
            BufferedImage screen = robot.createScreenCapture(rect);
            float weight = 1.0f / 49.0f; float[] data = new float[49];
            for (int i = 0; i < 49; i++) data[i] = weight;
            return new ConvolveOp(new Kernel(7, 7, data), ConvolveOp.EDGE_NO_OP, null).filter(screen, null);
        } catch (Exception ex) { return null; }
    }

    // Vẽ icon Thùng rác (Tiêu hủy)
    private static Image taoIconTieuHuy(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(220, 38, 38)); // Icon màu đỏ
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        g2.drawRoundRect(w/2 - 4, 2, 8, 3, 2, 2); // Tay cầm nắp
        g2.drawLine(2, 5, w-2, 5); // Nắp thùng rác
        g2.drawRoundRect(4, 5, w-8, h-7, 2, 2); // Thân thùng
        g2.drawLine(8, 9, 8, h-4); // Kẻ sọc 1
        g2.drawLine(w/2, 9, w/2, h-4); // Kẻ sọc 2
        g2.drawLine(w-8, 9, w-8, h-4); // Kẻ sọc 3
        g2.dispose();
        return img;
    }
}
