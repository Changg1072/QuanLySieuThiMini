package GUI;

import GUI.HoTro.TienIchGiaoDien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class QuanLyGiamGiaModule extends JPanel {

    private CardLayout cardLayout;
    private JPanel pnlContent;
    private PillMenu tabMenu;

    // Khai báo 2 tab UI dựa trên các file bạn đã cung cấp
    private GiamGiaUI tabGiamGia;
    private LichSuGiamGiaUI tabLichSu;

    public QuanLyGiamGiaModule() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // =========================================================
        // 1. THANH ĐIỀU HƯỚNG TABS (Chuẩn Chrome)
        // =========================================================
        JPanel pnlHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        pnlHeader.setBackground(Color.WHITE);
        pnlHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        // Thiết lập dữ liệu cho 2 Tab: Quản lý giảm giá & Lịch sử
        List<String> rawNames = List.of("GIAM_GIA", "LICH_SU");
        List<String> displayNames = List.of("Quản lý Khuyến Mãi", "Lịch sử Giảm Giá");
        List<String> icons = List.of("🔥", "📜");
        List<Color> colors = List.of(new Color(239, 68, 68), new Color(59, 130, 246));

        tabMenu = new PillMenu(rawNames, displayNames, icons, colors, tabName -> {
            switch (tabName) {
                case "GIAM_GIA":
                    // Xóa instance cũ và tạo mới để luôn cập nhật dữ liệu mới nhất
                    if (tabGiamGia != null) { 
                        pnlContent.remove(tabGiamGia); 
                    }
                    tabGiamGia = new GiamGiaUI(); 
                    pnlContent.add(tabGiamGia, "GIAM_GIA");
                    cardLayout.show(pnlContent, "GIAM_GIA");
                    break;
                    
                case "LICH_SU":
                    if (tabLichSu != null) { 
                        pnlContent.remove(tabLichSu); 
                    }
                    tabLichSu = new LichSuGiamGiaUI(); 
                    pnlContent.add(tabLichSu, "LICH_SU");
                    cardLayout.show(pnlContent, "LICH_SU");
                    break;
            }
        });
        pnlHeader.add(tabMenu);

        // =========================================================
        // 2. KHU VỰC CHỨA CÁC TAB (CARD LAYOUT)
        // =========================================================
        cardLayout = new CardLayout();
        pnlContent = new JPanel(cardLayout);
        pnlContent.setOpaque(false);

        // Khởi tạo các View lần đầu
        tabGiamGia = new GiamGiaUI();
        tabLichSu = new LichSuGiamGiaUI();

        pnlContent.add(tabGiamGia, "GIAM_GIA");
        pnlContent.add(tabLichSu, "LICH_SU");

        // Set tab mặc định khi vừa mở Module lên
        cardLayout.show(pnlContent, "GIAM_GIA");
        tabMenu.setActiveTab("GIAM_GIA");

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlContent, BorderLayout.CENTER);
    }

    // =========================================================
    // PILL MENU CHUẨN GOOGLE (CÓ VÁCH NGĂN VÀ ICON CHUẨN)
    // =========================================================
    private class PillMenu extends JPanel {
        private List<JButton> btns = new ArrayList<>();

        public PillMenu(List<String> rawNames, List<String> displayNames, List<String> icons, List<Color> colors, java.util.function.Consumer<String> onSelect) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);

            for (int i = 0; i < rawNames.size(); i++) {
                String rawName = rawNames.get(i);
                boolean isLast = (i == rawNames.size() - 1);

                JButton btn = new JButton(displayNames.get(i)) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create(); 
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        if (Boolean.TRUE.equals(getClientProperty("active"))) { 
                            g2.setColor(new Color(239, 246, 255)); 
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10); 
                            g2.setColor(TienIchGiaoDien.MAU_CHINH); 
                            g2.fillRect(0, getHeight()-3, getWidth(), 3);
                        } else { 
                            g2.setColor(Color.WHITE); 
                            g2.fillRect(0, 0, getWidth(), getHeight()); 
                            if (!isLast) {
                                g2.setColor(new Color(226, 232, 240));
                                g2.fillRect(getWidth() - 1, 10, 1, getHeight() - 20);
                            }
                        }
                        g2.dispose(); super.paintComponent(g);
                    }
                };
                
                // Set Icon tự vẽ để nằm cùng 1 baseline với chữ
                btn.setIcon(new EmojiIcon(icons.get(i), colors.get(i), 15));
                btn.setIconTextGap(8); // Khoảng cách giữa icon và chữ
                
                btn.putClientProperty("rawId", rawName);
                btn.putClientProperty("active", i == 0); // Sửa lại: Mặc định chọn tab đầu tiên (i==0)
                
                btn.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f)); 
                btn.setForeground(TienIchGiaoDien.MAU_CHU_PHU);
                btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false); 
                btn.setBorder(new EmptyBorder(10, 20, 10, 20)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                btn.addActionListener(e -> { 
                    setActiveTab(rawName); 
                    onSelect.accept(rawName); 
                });
                btns.add(btn); 
                add(btn);
            }
        }

        public void setActiveTab(String rawId) {
            for (JButton b : btns) { 
                boolean isActive = b.getClientProperty("rawId").equals(rawId); 
                b.putClientProperty("active", isActive); 
                b.setForeground(isActive ? TienIchGiaoDien.MAU_CHINH : TienIchGiaoDien.MAU_CHU_PHU); 
                b.repaint(); 
            }
        }
    }

    // =========================================================
    // CLASS PHỤ TRỢ VẼ ICON CHUẨN KÍCH THƯỚC
    // =========================================================
    private class EmojiIcon implements Icon {
        private String emoji; 
        private Color color; 
        private int size;

        public EmojiIcon(String emoji, Color color, int size) { 
            this.emoji = emoji; 
            this.color = color; 
            this.size = size; 
        }

        @Override public int getIconWidth() { return size + 4; }
        @Override public int getIconHeight() { return size; }
        
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, size));
            FontMetrics fm = g2.getFontMetrics();
            
            // Hạ xuống +1 để Icon nằm cùng trên 1 đường thẳng ngang với chữ
            g2.drawString(emoji, x, y + fm.getAscent() + 1); 
            
            g2.dispose();
        }
    }
    public static void main(String[] args) {
        // Bật antialiasing cho font chữ mượt mà hơn
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                // Sử dụng LookAndFeel của hệ thống (Windows/Mac)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            JFrame frame = new JFrame("Test Quản Lý Giảm Giá Module");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1366, 768);
            frame.setLocationRelativeTo(null); // Giữa màn hình
            
            // Thêm Module vào Frame
            frame.add(new QuanLyGiamGiaModule());
            
            frame.setVisible(true);
        });
    }
}