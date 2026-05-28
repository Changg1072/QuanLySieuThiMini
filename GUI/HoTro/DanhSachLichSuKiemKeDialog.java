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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 🚀 POPUP: DANH SÁCH TỔNG HỢP CÁC ĐỢT KIỂM KÊ
 * Giao diện hiển thị tất cả lịch sử, có nút "Xem" để mở chi tiết đợt.
 */
public class DanhSachLichSuKiemKeDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void showModal(Component parentComponent) {
        Window parentWindow = SwingUtilities.getWindowAncestor(parentComponent);

        // 1. TẠO OVERLAY KÍNH MỜ
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
                if (blurredBg != null) {
                    g2.drawImage(blurredBg, 0, 0, getWidth(), getHeight(), null);
                }
                g2.setColor(new Color(0, 0, 0, 120)); // Tối hơn một chút để nổi bật
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        pnlOverlay.setOpaque(false);
        overlay.setContentPane(pnlOverlay);

        // 2. TẠO DIALOG POPUP
        JDialog dialog = new JDialog(parentWindow, "Danh Sách Các Đợt Kiểm Kê", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        // 3. XÂY DỰNG GIAO DIỆN
        JPanel pnlMain = taoMainLayout(dialog, overlay);
        dialog.setContentPane(pnlMain);
        dialog.pack();
        dialog.setLocationRelativeTo(parentWindow);

        pnlOverlay.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dongPopup(dialog, overlay); }
        });
        dialog.getRootPane().registerKeyboardAction(e -> dongPopup(dialog, overlay),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 4. HIỂN THỊ ANIMATION
        overlay.setVisible(true);
        dialog.setOpacity(0f);
        chayAnimationFadeIn(dialog);
        dialog.setVisible(true);
    }

    private static JPanel taoMainLayout(JDialog dialog, JDialog overlay) {
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
        pnlMain.setPreferredSize(new Dimension(1000, 600));

        // --- HEADER ---
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)));

        JLabel lblTitle = new JLabel(" Lịch sử các đợt kiểm kê");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 41, 59));
        lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnClose.setForeground(new Color(100, 116, 139));
        btnClose.setContentAreaFilled(false); btnClose.setBorderPainted(false); btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dongPopup(dialog, overlay));
        
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(btnClose, BorderLayout.EAST);

        // --- FILTER BAR ---
        JPanel pnlFilter = new JPanel(new BorderLayout());
        pnlFilter.setOpaque(false);
        pnlFilter.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        TheBongDo.RoundedTextField txtSearch = new TheBongDo.RoundedTextField("🔍 Tìm kiếm mã đợt, nhân viên...", 20);
        txtSearch.setPreferredSize(new Dimension(350, 40));
        pnlFilter.add(txtSearch, BorderLayout.WEST);

        // --- TABLE ---
        String[] cols = {"MÃ ĐỢT", "NGÀY KIỂM", "NGƯỜI KIỂM KÊ", "SỐ SP", "TRẠNG THÁI", "HÀNH ĐỘNG"};
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

        // Xử lý sự kiện Tìm kiếm
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void loc() {
                String text = txtSearch.getText().trim();
                if (text.isEmpty() || text.contains("Tìm kiếm")) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 2)); // Tìm theo Mã và Nhân viên
            }
            public void insertUpdate(DocumentEvent e) { loc(); }
            public void removeUpdate(DocumentEvent e) { loc(); }
            public void changedUpdate(DocumentEvent e) { loc(); }
        });

        // 🚀 BẮT SỰ KIỆN CLICK NÚT "XEM" (Cột 5)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 5) {
                    int modelRow = table.convertRowIndexToModel(row);
                    String maKiemKe = model.getValueAt(modelRow, 0).toString();
                    
                    // MỞ POPUP CHI TIẾT ĐỢT KIỂM KÊ
                    GUI.HoTro.LichSuKiemKeDialog.showModal(dialog, maKiemKe);
                }
            }
        });

        // Tải dữ liệu
        loadDataAsync(model);

        pnlMain.add(pnlHeader, BorderLayout.NORTH);
        
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 15));
        pnlCenter.setOpaque(false);
        pnlCenter.add(pnlFilter, BorderLayout.NORTH);
        pnlCenter.add(scrollPane, BorderLayout.CENTER);
        
        pnlMain.add(pnlCenter, BorderLayout.CENTER);

        return pnlMain;
    }

    private static void loadDataAsync(DefaultTableModel model) {
        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Gom nhóm phiếu kiểm kê theo MaKiemKe
                String sql = "SELECT kk.MaKiemKe, MAX(kk.NgayKiemKe) as NgayKiem, nv.HoTen, " +
                             "COUNT(kk.MaSP) as SoSP, SUM(ABS(kk.SoLuongThucTe - kk.SoLuongHeThong)) as TongLech " +
                             "FROM KiemKeKho kk " +
                             "LEFT JOIN NhanVien nv ON kk.MaNV = nv.MaNV " +
                             "GROUP BY kk.MaKiemKe, nv.HoTen " +
                             "ORDER BY NgayKiem DESC";

                try (Connection con = ConnectDB.getInstance().getConnection();
                     PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    
                    while (rs.next()) {
                        String maKK = rs.getString("MaKiemKe");
                        java.sql.Date sqlDate = rs.getDate("NgayKiem");
                        String ngay = (sqlDate != null) ? sqlDate.toLocalDate().format(DATE_FMT) : "—";
                        String nhanVien = rs.getString("HoTen") != null ? rs.getString("HoTen") : "Nhân viên (N/A)";
                        int soSP = rs.getInt("SoSP");
                        int tongLech = rs.getInt("TongLech");
                        
                        String trangThai = tongLech == 0 ? "Khớp hoàn toàn" : "Có chênh lệch";

                        publish(new Object[]{ maKK, ngay, nhanVien, soSP, trangThai, "👁 Xem" });
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
                
                setHorizontalAlignment(col == 3 || col == 4 || col == 5 ? CENTER : LEFT);
                setBorder(new EmptyBorder(0, 15, 0, 15));
                
                if (!isSelected) setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 250, 252));
                else setBackground(new Color(239, 246, 255));

                setFont(new Font("Segoe UI", Font.PLAIN, 14));

                if (col == 0) {
                    setForeground(new Color(37, 99, 235));
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else if (col == 1 || col == 3) {
                    setForeground(new Color(100, 116, 139));
                } else if (col == 2) {
                    setForeground(new Color(15, 23, 42));
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else if (col == 4) { // Trạng thái
                    String status = value.toString();
                    JLabel badge = new JLabel(status, SwingConstants.CENTER) {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            if (status.equals("Khớp hoàn toàn")) g2.setColor(new Color(209, 250, 229));
                            else g2.setColor(new Color(254, 226, 226));
                            g2.fillRoundRect(0, 4, getWidth(), getHeight()-8, 8, 8);
                            super.paintComponent(g); g2.dispose();
                        }
                    };
                    badge.setOpaque(false);
                    badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    badge.setForeground(status.equals("Khớp hoàn toàn") ? new Color(5, 150, 105) : new Color(220, 38, 38));
                    
                    JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
                    wrap.setOpaque(false); wrap.setBackground(getBackground());
                    badge.setPreferredSize(new Dimension(110, 32));
                    wrap.add(badge);
                    return wrap;
                } else if (col == 5) { // Nút XEM
                    JLabel btnView = new JLabel(value.toString(), SwingConstants.CENTER) {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new Color(239, 246, 255));
                            g2.fillRoundRect(0, 6, getWidth(), getHeight()-12, 6, 6);
                            super.paintComponent(g); g2.dispose();
                        }
                    };
                    btnView.setOpaque(false);
                    btnView.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    btnView.setForeground(new Color(37, 99, 235));
                    btnView.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    
                    JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                    wrap.setOpaque(false); wrap.setBackground(getBackground());
                    btnView.setPreferredSize(new Dimension(80, 48));
                    wrap.add(btnView);
                    return wrap;
                }
                return this;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
    }

    private static void dongPopup(JDialog dialog, JDialog overlay) {
        dialog.dispose();
        overlay.dispose();
    }

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
            Robot robot = new Robot();
            Rectangle rect = parentWindow.getBounds();
            BufferedImage screen = robot.createScreenCapture(rect);
            float weight = 1.0f / 49.0f; float[] data = new float[49];
            for (int i = 0; i < 49; i++) data[i] = weight;
            Kernel kernel = new Kernel(7, 7, data);
            ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
            return op.filter(screen, null);
        } catch (Exception ex) { return null; }
    }
}