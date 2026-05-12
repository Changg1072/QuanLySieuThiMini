package GUI.HoTro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class ThongBaoNoi extends JDialog {

    private Point initialClick;

    // SỬA LẠI CONSTRUCTOR: Nhận Window làm owner để không bị đè
    public ThongBaoNoi(Window owner, String tieuDe, String thongBao) {
        super(owner, Dialog.ModalityType.MODELESS);
        setUndecorated(true);
        setAlwaysOnTop(true);
        setFocusableWindowState(false); // Không giật chuột của người dùng
        setBackground(new Color(0, 0, 0, 0)); 

        JPanel pnlMain = new JPanel(new BorderLayout(15, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 41, 59, 240)); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(15, 20, 15, 20));

        pnlMain.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { initialClick = e.getPoint(); }
        });
        pnlMain.addMouseMotionListener(new MouseMotionAdapter() {
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
                g2.fillOval(0, 5, 26, 26);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 18));
                g2.drawString("!", 11, 24);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(35, 35); }
        };
        pnlIcon.setOpaque(false);

        JPanel pnlText = new JPanel(new GridLayout(2, 1, 0, 5));
        pnlText.setOpaque(false);
        
        JLabel lblTitle = new JLabel(tieuDe);
        lblTitle.setFont(new Font("Calibri", Font.BOLD, 18));
        lblTitle.setForeground(new Color(245, 158, 11));
        
        JLabel lblMsg = new JLabel("<html><div style='width: 250px;'>" + thongBao + "</div></html>");
        lblMsg.setFont(new Font("Calibri", Font.PLAIN, 16));
        lblMsg.setForeground(Color.WHITE);
        
        pnlText.add(lblTitle);
        pnlText.add(lblMsg);

        JLabel lblClose = new JLabel("X");
        lblClose.setFont(new Font("Arial", Font.BOLD, 18));
        lblClose.setForeground(Color.LIGHT_GRAY);
        lblClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblClose.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { dispose(); }
            public void mouseEntered(MouseEvent e) { lblClose.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { lblClose.setForeground(Color.LIGHT_GRAY); }
        });

        pnlMain.add(pnlIcon, BorderLayout.WEST);
        pnlMain.add(pnlText, BorderLayout.CENTER);
        pnlMain.add(lblClose, BorderLayout.EAST);

        setContentPane(pnlMain);
        pack();

        // SỬA LẠI TỌA ĐỘ: Trừ đi kích thước Taskbar để không bị che
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setLocation(20, bounds.height - getHeight() - 20); 
    }

    public static void hienThi(String tieuDe, String thongBao) {
        SwingUtilities.invokeLater(() -> {
            // TUYỆT CHIÊU: Bắt Frame đang hiển thị làm Owner
            Window activeWin = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            
            ThongBaoNoi toast = new ThongBaoNoi(activeWin, tieuDe, thongBao);
            toast.setVisible(true);
            
            // Tặng kèm tính năng tự động biến mất sau 4 giây
            Timer autoClose = new Timer(4000, e -> toast.dispose());
            autoClose.setRepeats(false);
            autoClose.start();
        });
    }
}