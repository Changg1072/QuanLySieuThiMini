package GUI.HoTro;

import Data.ChiaCa;
import Logic.ChiaCaLogic;
import Logic.NhanVienLogic;
import Data.NhanVien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

public class ThongBaoDiemDanh extends JDialog {

    private JPanel pnlList;
    private List<ChiaCa> dsThieu;
    private ChiaCaLogic ccLogic;
    private NhanVienLogic nvLogic;
    private Point initialClick;

    // SỬA LẠI CONSTRUCTOR: Nhận Window làm owner
    public ThongBaoDiemDanh(Window owner, List<ChiaCa> ds) {
        super(owner, Dialog.ModalityType.MODELESS);
        this.dsThieu = ds;
        this.ccLogic = new ChiaCaLogic();
        this.nvLogic = new NhanVienLogic();
        
        setUndecorated(true);
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setBackground(new Color(0, 0, 0, 0)); 

        JPanel pnlMain = new JPanel(new BorderLayout(15, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 41, 59, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        
        pnlHeader.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { initialClick = e.getPoint(); }
        });
        pnlHeader.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int thisX = getLocation().x;
                int thisY = getLocation().y;
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                setLocation(thisX + xMoved, thisY + yMoved);
            }
        });

        JPanel pnlIcon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(245, 158, 11));
                g2.fillOval(0, 2, 22, 22);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 15));
                g2.drawString("!", 9, 18);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(30, 26); }
        };
        pnlIcon.setOpaque(false);

        JLabel lblTitle = new JLabel("ĐIỂM DANH CA LÀM");
        lblTitle.setFont(new Font("Calibri", Font.BOLD, 18));
        lblTitle.setForeground(new Color(245, 158, 11));

        JLabel lblClose = new JLabel("X"); 
        lblClose.setFont(new Font("Arial", Font.BOLD, 16));
        lblClose.setForeground(Color.LIGHT_GRAY);
        lblClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblClose.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { dispose(); }
            public void mouseEntered(MouseEvent e) { lblClose.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { lblClose.setForeground(Color.LIGHT_GRAY); }
        });

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.add(pnlIcon);
        pnlTitle.add(lblTitle);

        pnlHeader.add(pnlTitle, BorderLayout.WEST);
        pnlHeader.add(lblClose, BorderLayout.EAST);

        pnlList = new JPanel();
        pnlList.setLayout(new BoxLayout(pnlList, BoxLayout.Y_AXIS));
        pnlList.setOpaque(false);
        
        for (ChiaCa cc : dsThieu) {
            pnlList.add(taoRowNhanVien(cc));
            pnlList.add(Box.createVerticalStrut(8));
        }

        JLabel lblNote = new JLabel("<html><i>Dùng chung máy: Đồng nghiệp tới hãy click 'Có mặt'!</i></html>");
        lblNote.setFont(new Font("Calibri", Font.PLAIN, 14));
        lblNote.setForeground(new Color(148, 163, 184));

        pnlMain.add(pnlHeader, BorderLayout.NORTH);
        pnlMain.add(pnlList, BorderLayout.CENTER);
        pnlMain.add(lblNote, BorderLayout.SOUTH);

        setContentPane(pnlMain);
        pack();
        
        // SỬA LẠI TỌA ĐỘ: Tránh thanh Taskbar Windows
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setLocation(bounds.width - getWidth() - 30, bounds.height - getHeight() - 30);
    }

    private JPanel taoRowNhanVien(ChiaCa cc) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setOpaque(false);
        
        NhanVien nv = nvLogic.timNhanVienTheoMa(cc.getMaNV());
        String ten = (nv != null) ? nv.getHoTen() : cc.getMaNV();

        JLabel lblName = new JLabel("- " + ten);
        lblName.setFont(new Font("Calibri", Font.PLAIN, 18));
        lblName.setForeground(Color.WHITE);

        NutBoGoc btnCoMat = new NutBoGoc("Có mặt");
        btnCoMat.setColorBackground(new Color(34, 197, 94)); 

        btnCoMat.addActionListener(e -> {
            try {
                ccLogic.xacNhanCoMatTucThi(cc.getMaCa());
                ThongBaoNoi toast = new ThongBaoNoi(getOwner(), "Thành công", 
                                             "Đã ghi nhận điểm danh cho " + ten);
                toast.setVisible(true);
                Timer autoClose = new Timer(4000, ev -> toast.dispose());
                autoClose.setRepeats(false);
                autoClose.start();

                pnlList.remove(row);
                pnlList.revalidate();
                pnlList.repaint();
                pack(); 

                if (pnlList.getComponentCount() == 0) {
                    dispose(); 
                }
            } catch (Exception ex) {
                GUI.HoTro.ThongBaoNoi.hienThi("Lỗi điểm danh", ex.getMessage());
            }
        });

        row.add(lblName, BorderLayout.CENTER);
        row.add(btnCoMat, BorderLayout.EAST);
        return row;
    }

    public static void hienThiCanhBaoTrong(String tieuDe, String noiDung) {
        SwingUtilities.invokeLater(() -> {
            GUI.HoTro.ThongBaoNoi.hienThi(tieuDe, noiDung);
        });
    }

    public static void hienThi(List<ChiaCa> ds) {
        SwingUtilities.invokeLater(() -> {
            // Lấy Frame chủ để không bị lấp đè
            Window activeWin = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            
            ThongBaoDiemDanh toast = new ThongBaoDiemDanh(activeWin, ds);
            toast.setVisible(true);
        });
    }
}