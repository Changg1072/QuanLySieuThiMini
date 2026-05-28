package GUI.HoTro;

import Dao.ConnectDB;
import Data.CauHinhLuong;
import Data.NhanVien;
import Logic.CauHinhLuongLogic;
import Logic.NhanVienLogic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CaiDatLuongDialog extends JDialog {

    private boolean isSuccess = false;
    private NhanVien nvDuocChon = null;
    
    private NhanVienLogic nvLogic = new NhanVienLogic();
    private CauHinhLuongLogic chlLogic = new CauHinhLuongLogic();
    private List<NhanVien> dsNhanVienHienTai = new ArrayList<>();

    // --- MÀU SẮC CHỦ ĐẠO (Theo Mockup Light Theme) ---
    private final Color COLOR_BG = Color.WHITE;
    private final Color COLOR_BORDER = new Color(226, 232, 240);
    private final Color COLOR_TEXT_MAIN = new Color(30, 41, 59);
    private final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private final Color COLOR_PRIMARY = new Color(37, 99, 235);     // Xanh Blue dương
    private final Color COLOR_CARD_BG = new Color(248, 250, 252);   // Nền xám xanh cực nhạt
    private final Color COLOR_SUCCESS = new Color(16, 185, 129);    // Xanh lá
    private final Color COLOR_DANGER = new Color(239, 68, 68);      // Đỏ

    // --- UI COMPONENTS ---
    private JTextField txtSearchNV;
    private JPopupMenu popupSearch;
    private JList<String> listSearch;
    private DefaultListModel<String> listModelSearch;
    
    // Card Nhân Viên
    private JPanel pnlCardNV;
    private JLabel lblTenNV, lblMaNV, lblBadgeNV;

    // Inputs Cấu Hình
    private InputTienBig txtLuongGio;
    private InputTextThuong txtHeSo, txtPhuCap, txtGhiChu;

    // Bảng Lịch sử
    private JTable tblLichSu;
    private DefaultTableModel modelLichSu;
    private JLabel lblLastUpdate;

    private DecimalFormat df = new DecimalFormat("#,###");
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CaiDatLuongDialog(Window parent) {
        super(parent, "Cài đặt lương nhân viên", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); // Để làm bo góc trong suốt
        setSize(850, 720);
        setLocationRelativeTo(parent);

        // Load danh sách nhân viên để phục vụ ô tìm kiếm
        dsNhanVienHienTai = nvLogic.layDanhSachNhanVien();

        initUI();
        setupSearchLogic();
    }

    // =========================================================
    // 1. KHỞI TẠO GIAO DIỆN CHÍNH
    // =========================================================
    private void initUI() {
        JPanel pnlMain = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Nền trắng bo góc
                g2.setColor(COLOR_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                // Viền xám mỏng
                g2.setColor(COLOR_BORDER);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
                g2.dispose();
            }
        };
        pnlMain.setOpaque(false);
        pnlMain.setBorder(new EmptyBorder(25, 30, 25, 30));

        // --- HEADER ---
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // 🚀 SỬA TẠI ĐÂY: Đổi sang BorderLayout để không bị ép chữ
        JPanel pnlTitle = new JPanel(new BorderLayout(15, 0)); 
        pnlTitle.setOpaque(false);
        
        JLabel lblIcon = new JLabel("⚙️"); 
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        lblIcon.setForeground(COLOR_PRIMARY);
        
        JPanel pnlTextTitle = new JPanel(new GridLayout(2, 1));
        pnlTextTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Cài đặt lương nhân viên");
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 20f));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        
        JLabel lblSub = new JLabel("Cấu hình thông số lương cơ bản và phụ cấp");
        lblSub.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(13f));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        
        pnlTextTitle.add(lblTitle); 
        pnlTextTitle.add(lblSub);
        
        // 🚀 SỬA TẠI ĐÂY: Xếp Icon bên trái, Nhóm chữ ở giữa để nó fill toàn bộ khoảng trống
        pnlTitle.add(lblIcon, BorderLayout.WEST); 
        pnlTitle.add(pnlTextTitle, BorderLayout.CENTER); 

        JButton btnClose = new JButton("✖");
        btnClose.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16)); // Font hỗ trợ tốt Unicode
        btnClose.setForeground(COLOR_TEXT_MUTED);
        btnClose.setContentAreaFilled(false); 
        btnClose.setBorderPainted(false); 
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());

        pnlHeader.add(pnlTitle, BorderLayout.WEST);
        pnlHeader.add(btnClose, BorderLayout.EAST);
        
        // Kẻ ngang dưới Header
        JPanel pnlHeaderWrap = new JPanel(new BorderLayout());
        pnlHeaderWrap.setOpaque(false);
        pnlHeaderWrap.add(pnlHeader, BorderLayout.CENTER);
        pnlHeaderWrap.add(taoKeNgang(), BorderLayout.SOUTH);

        // --- BODY (Center) ---
        JPanel pnlBody = new JPanel(new BorderLayout(0, 25));
        pnlBody.setOpaque(false);
        pnlBody.setBorder(new EmptyBorder(20, 0, 20, 0));

        // 2 Cột: Trái (Chọn NV) - Phải (Cấu hình)
        JPanel pnlTopBody = new JPanel(new GridLayout(1, 2, 40, 0));
        pnlTopBody.setOpaque(false);
        
        pnlTopBody.add(taoCotTrai());
        pnlTopBody.add(taoCotPhai());
        
        pnlBody.add(pnlTopBody, BorderLayout.NORTH);
        
        // Lịch sử thay đổi
        JPanel pnlHistory = taoPanelLichSu();
        pnlBody.add(pnlHistory, BorderLayout.CENTER);

        // --- FOOTER (Nút bấm) ---
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.add(taoKeNgang(), BorderLayout.NORTH);

        JPanel pnlFooterContent = new JPanel(new BorderLayout());
        pnlFooterContent.setOpaque(false);
        pnlFooterContent.setBorder(new EmptyBorder(20, 0, 0, 0));

        JButton btnDelete = new JButton("🗑️ Xóa cấu hình");
        btnDelete.setForeground(COLOR_DANGER);
        btnDelete.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f));
        btnDelete.setContentAreaFilled(false); btnDelete.setBorderPainted(false); btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel pnlBtnRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlBtnRight.setOpaque(false);
        
        JButton btnHuy = TienIchGiaoDien.taoNutHienDai("Hủy bỏ", COLOR_CARD_BG);
        btnHuy.setForeground(COLOR_TEXT_MAIN);
        btnHuy.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        btnHuy.addActionListener(e -> dispose());
        
        JButton btnLuu = TienIchGiaoDien.taoNutHienDai("💾 Lưu thay đổi", COLOR_PRIMARY);
        btnLuu.addActionListener(e -> luuCauHinh());

        pnlBtnRight.add(btnHuy); pnlBtnRight.add(btnLuu);
        
        pnlFooterContent.add(btnDelete, BorderLayout.WEST);
        pnlFooterContent.add(pnlBtnRight, BorderLayout.EAST);
        pnlFooter.add(pnlFooterContent, BorderLayout.CENTER);

        // Gắn vào Main
        pnlMain.add(pnlHeaderWrap, BorderLayout.NORTH);
        pnlMain.add(pnlBody, BorderLayout.CENTER);
        pnlMain.add(pnlFooter, BorderLayout.SOUTH);

        setContentPane(pnlMain);
    }

    // =========================================================
    // 2. TẠO CÁC VÙNG GIAO DIỆN CON
    // =========================================================
    private JPanel taoCotTrai() {
        JPanel pnl = new JPanel(new BorderLayout(0, 15));
        pnl.setOpaque(false);

        JLabel lblTitle = new JLabel("CHỌN NHÂN VIÊN");
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12f));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        // Ô tìm kiếm
        JPanel pnlSearch = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(txtSearchNV.isFocusOwner() ? COLOR_PRIMARY : COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        pnlSearch.setOpaque(false);
        pnlSearch.setBorder(new EmptyBorder(10, 15, 10, 15));
        // 🚀 SỬA TẠI ĐÂY: Ép cứng chiều cao ô tìm kiếm
        pnlSearch.setPreferredSize(new Dimension(0, 45)); 
        
        JLabel lblIconSearch = new JLabel("🔍");
        txtSearchNV = new JTextField();
        txtSearchNV.setBorder(null);
        txtSearchNV.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(14f));
        TienIchGiaoDien.datPlaceholder(txtSearchNV, "Tìm tên hoặc mã NV...");
        
        pnlSearch.add(lblIconSearch, BorderLayout.WEST);
        pnlSearch.add(txtSearchNV, BorderLayout.CENTER);

        // Card Nhân Viên 
        pnlCardNV = new JPanel(new BorderLayout(15, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD_BG); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(COLOR_PRIMARY.brighter()); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        pnlCardNV.setOpaque(false);
        pnlCardNV.setBorder(new EmptyBorder(15, 15, 15, 15));
        // 🚀 SỬA TẠI ĐÂY: Ép cứng chiều cao Card Nhân Viên để không bị kéo giãn thòng lòng
        pnlCardNV.setPreferredSize(new Dimension(0, 85)); 
        pnlCardNV.setVisible(false); // Ban đầu ẩn

        JLabel lblAvatar = new JLabel("👤", SwingConstants.CENTER);
        lblAvatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));

        JPanel pnlInfo = new JPanel(new GridLayout(3, 1, 0, 3));
        pnlInfo.setOpaque(false);
        
        lblTenNV = new JLabel("---");
        lblTenNV.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(16f));
        lblTenNV.setForeground(COLOR_TEXT_MAIN);
        
        lblMaNV = new JLabel("Mã NV: ---");
        lblMaNV.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f));
        lblMaNV.setForeground(COLOR_TEXT_MUTED);

        lblBadgeNV = new JLabel("ĐANG LÀM VIỆC", SwingConstants.CENTER);
        lblBadgeNV.setOpaque(true);
        lblBadgeNV.setBackground(new Color(209, 250, 229));
        lblBadgeNV.setForeground(COLOR_SUCCESS);
        lblBadgeNV.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(10f));
        lblBadgeNV.setBorder(new EmptyBorder(2, 8, 2, 8));
        
        JPanel pnlBadgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlBadgeWrap.setOpaque(false); pnlBadgeWrap.add(lblBadgeNV);

        pnlInfo.add(lblTenNV); pnlInfo.add(lblMaNV); pnlInfo.add(pnlBadgeWrap);
        pnlCardNV.add(lblAvatar, BorderLayout.WEST);
        pnlCardNV.add(pnlInfo, BorderLayout.CENTER);

        // 🚀 SỬA TẠI ĐÂY: Bọc tất cả vào 1 Panel phụ rồi nhét vào NORTH để nó dồn lên trên cùng
        JPanel pnlTopWrap = new JPanel(new BorderLayout(0, 15));
        pnlTopWrap.setOpaque(false);
        pnlTopWrap.add(lblTitle, BorderLayout.NORTH);
        pnlTopWrap.add(pnlSearch, BorderLayout.CENTER);
        
        JPanel pnlChongDanDoc = new JPanel(new BorderLayout(0, 15));
        pnlChongDanDoc.setOpaque(false);
        pnlChongDanDoc.add(pnlTopWrap, BorderLayout.NORTH);
        pnlChongDanDoc.add(pnlCardNV, BorderLayout.CENTER);

        // Add cục chống giãn vào NORTH của Panel chính cột trái
        pnl.add(pnlChongDanDoc, BorderLayout.NORTH);

        return pnl;
    }

    private JPanel taoCotPhai() {
        JPanel pnl = new JPanel(new GridLayout(3, 1, 0, 15));
        pnl.setOpaque(false);

        // Dòng 1: Lương theo giờ
        JPanel pnlLuong = new JPanel(new BorderLayout(0, 8));
        pnlLuong.setOpaque(false);
        JLabel l1 = new JLabel("Lương theo giờ (Hourly Wage) *"); l1.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f)); l1.setForeground(COLOR_TEXT_MUTED);
        txtLuongGio = new InputTienBig();
        pnlLuong.add(l1, BorderLayout.NORTH); pnlLuong.add(txtLuongGio, BorderLayout.CENTER);

        // Dòng 2: Hệ số & Phụ cấp
        JPanel pnlRow2 = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlRow2.setOpaque(false);
        
        JPanel pnlHS = new JPanel(new BorderLayout(0, 8)); pnlHS.setOpaque(false);
        JLabel l2 = new JLabel("Hệ số lương (Coefficient)"); l2.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f)); l2.setForeground(COLOR_TEXT_MUTED);
        txtHeSo = new InputTextThuong("1.0", false);
        pnlHS.add(l2, BorderLayout.NORTH); pnlHS.add(txtHeSo, BorderLayout.CENTER);

        JPanel pnlPC = new JPanel(new BorderLayout(0, 8)); pnlPC.setOpaque(false);
        JLabel l3 = new JLabel("Phụ cấp cố định (Allowance)"); l3.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f)); l3.setForeground(COLOR_TEXT_MUTED);
        txtPhuCap = new InputTextThuong("0", true);
        pnlPC.add(l3, BorderLayout.NORTH); pnlPC.add(txtPhuCap, BorderLayout.CENTER);

        pnlRow2.add(pnlHS); pnlRow2.add(pnlPC);

        // Dòng 3: Ghi chú
        JPanel pnlGC = new JPanel(new BorderLayout(0, 8));
        pnlGC.setOpaque(false);
        JLabel l4 = new JLabel("Ghi chú thay đổi"); l4.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f)); l4.setForeground(COLOR_TEXT_MUTED);
        txtGhiChu = new InputTextThuong("Lý do điều chỉnh lương...", false);
        pnlGC.add(l4, BorderLayout.NORTH); pnlGC.add(txtGhiChu, BorderLayout.CENTER);

        pnl.add(pnlLuong);
        pnl.add(pnlRow2);
        pnl.add(pnlGC);

        return pnl;
    }

    private JPanel taoPanelLichSu() {
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setOpaque(false);

        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);
        JLabel lblTitle = new JLabel("⏱ Lịch sử thay đổi lương");
        lblTitle.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f));
        lblTitle.setForeground(COLOR_PRIMARY);
        
        lblLastUpdate = new JLabel("Cập nhật lần cuối: ---");
        lblLastUpdate.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(11f));
        lblLastUpdate.setForeground(COLOR_TEXT_MUTED);
        
        pnlHead.add(lblTitle, BorderLayout.WEST);
        pnlHead.add(lblLastUpdate, BorderLayout.EAST);

        // Tạo Bảng
        String[] cols = {"NGÀY HIỆU LỰC", "MỨC LƯƠNG CŨ", "MỨC LƯƠNG MỚI", "TRẠNG THÁI"};
        modelLichSu = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblLichSu = new JTable(modelLichSu);
        tblLichSu.setRowHeight(40);
        tblLichSu.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(13f));
        tblLichSu.setShowGrid(false);
        tblLichSu.setIntercellSpacing(new Dimension(0, 0));
        tblLichSu.setSelectionBackground(COLOR_CARD_BG);
        tblLichSu.setSelectionForeground(COLOR_TEXT_MAIN);

        // Header Bảng
        JTableHeader header = tblLichSu.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(11f));
        header.setBackground(COLOR_CARD_BG);
        header.setForeground(COLOR_TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

        // Custom Cột Lương Mới (Màu xanh đậm)
        tblLichSu.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(13f));
                c.setForeground(COLOR_PRIMARY);
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(tblLichSu);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        TienIchGiaoDien.thietLapThanhCuon(scroll);
        scroll.setPreferredSize(new Dimension(0, 150));

        pnl.add(pnlHead, BorderLayout.NORTH);
        pnl.add(scroll, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel taoKeNgang() {
        JPanel line = new JPanel();
        line.setBackground(COLOR_BORDER);
        line.setPreferredSize(new Dimension(0, 1));
        return line;
    }

    // =========================================================
    // 3. LOGIC TÌM KIẾM & LOAD DỮ LIỆU
    // =========================================================
    private void setupSearchLogic() {
        popupSearch = new JPopupMenu();
        popupSearch.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        listModelSearch = new DefaultListModel<>();
        listSearch = new JList<>(listModelSearch);
        listSearch.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(13f));
        listSearch.setSelectionBackground(COLOR_CARD_BG);
        listSearch.setSelectionForeground(COLOR_PRIMARY);
        
        JScrollPane scrollSearch = new JScrollPane(listSearch);
        scrollSearch.setBorder(null);
        popupSearch.add(scrollSearch);

        txtSearchNV.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter() ;}
            public void removeUpdate(DocumentEvent e) { filter() ;}
            public void changedUpdate(DocumentEvent e) { filter() ;}
            private void filter() {
                String key = txtSearchNV.getText().toLowerCase().trim();
                listModelSearch.clear();
                if (key.isEmpty()) { popupSearch.setVisible(false); return; }
                
                for (NhanVien nv : dsNhanVienHienTai) {
                    if ("Đã Nghỉ".equals(nv.getTrangThai())) continue; // Bỏ qua người nghỉ việc
                    if (nv.getHoTen().toLowerCase().contains(key) || nv.getMaNV().toLowerCase().contains(key)) {
                        listModelSearch.addElement(nv.getMaNV() + " - " + nv.getHoTen());
                    }
                }
                if (!listModelSearch.isEmpty()) {
                    popupSearch.setPreferredSize(new Dimension(txtSearchNV.getWidth() + 30, Math.min(listModelSearch.size() * 30, 150)));
                    popupSearch.show(txtSearchNV, -15, txtSearchNV.getHeight() + 10);
                    txtSearchNV.requestFocus();
                } else {
                    popupSearch.setVisible(false);
                }
            }
        });

        listSearch.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 1) {
                    String selected = listSearch.getSelectedValue();
                    if (selected != null) {
                        String maNV = selected.split(" - ")[0];
                        NhanVien nv = dsNhanVienHienTai.stream().filter(n -> n.getMaNV().equals(maNV)).findFirst().orElse(null);
                        if (nv != null) hienThiNhanVien(nv);
                        popupSearch.setVisible(false);
                    }
                }
            }
        });
    }

    private void hienThiNhanVien(NhanVien nv) {
        this.nvDuocChon = nv;
        txtSearchNV.setText("");
        
        // 1. Hiện thông tin lên Card
        pnlCardNV.setVisible(true);
        lblTenNV.setText(nv.getHoTen());
        lblMaNV.setText("Mã NV: " + nv.getMaNV());
        
        // 2. Load Cấu hình hiện tại
        try {
            CauHinhLuong chlHienTai = chlLogic.layCauHinhHienTai(nv.getMaNV());
            if (chlHienTai != null) {
                txtLuongGio.setText(df.format(chlHienTai.getLuongTheoGio()));
                txtHeSo.setText(chlHienTai.getHeSoLuong().toPlainString());
                txtPhuCap.setText(df.format(chlHienTai.getPhuCapCoDinh()));
            } else {
                // Trắng trơn nếu chưa có
                txtLuongGio.setText("0"); txtHeSo.setText("1.0"); txtPhuCap.setText("0");
            }
            
            // 3. Load bảng lịch sử (Dùng JDBC trực tiếp cho nhanh gọn lẹ)
            loadLichSu(nv.getMaNV());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLichSu(String maNV) {
        modelLichSu.setRowCount(0);
        String sql = "SELECT LuongTheoGio, NgayApDung, TrangThai FROM CauHinhLuong WHERE MaNV = ? ORDER BY NgayApDung DESC, MaCauHinh DESC";
        
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maNV);
            ResultSet rs = ps.executeQuery();
            
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getDate("NgayApDung") != null ? rs.getDate("NgayApDung").toLocalDate() : LocalDate.now(),
                    rs.getBigDecimal("LuongTheoGio"),
                    rs.getString("TrangThai")
                });
            }

            for (int i = 0; i < rows.size(); i++) {
                LocalDate ngay = (LocalDate) rows.get(i)[0];
                BigDecimal luongMoi = (BigDecimal) rows.get(i)[1];
                String trangThai = (String) rows.get(i)[2];
                
                // Mức lương cũ là mức lương của dòng tiếp theo (do đang sếp DESC)
                String luongCuStr = "---";
                if (i + 1 < rows.size()) {
                    BigDecimal luongCu = (BigDecimal) rows.get(i+1)[1];
                    luongCuStr = df.format(luongCu) + " VNĐ";
                }

                modelLichSu.addRow(new Object[]{
                    dtf.format(ngay),
                    luongCuStr,
                    df.format(luongMoi) + " VNĐ",
                    trangThai
                });

                if (i == 0) lblLastUpdate.setText("Cập nhật lần cuối: " + dtf.format(ngay));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // 4. LƯU CẤU HÌNH LƯƠNG
    // =========================================================
    private void luuCauHinh() {
        if (nvDuocChon == null) {
            TienIchGiaoDien.hienThiThongBao(this, "Vui lòng tìm và chọn nhân viên trước!", "WARNING");
            return;
        }

        try {
            BigDecimal luong = new BigDecimal(txtLuongGio.getText().replace(",", ""));
            BigDecimal heSo = new BigDecimal(txtHeSo.getText());
            BigDecimal phuCap = new BigDecimal(txtPhuCap.getText().replace(",", ""));

            CauHinhLuong chlMoi = new CauHinhLuong.ThoXayCauHinhLuong()
                .ganMaNV(nvDuocChon.getMaNV())
                .ganLuongTheoGio(luong)
                .ganHeSoLuong(heSo)
                .ganPhuCapCoDinh(phuCap)
                .ganHeSoTangCa(new BigDecimal("1.5")) // Mặc định
                .taoMoi();

            chlLogic.capNhatCauHinhLuongMoi(chlMoi);
            TienIchGiaoDien.hienThiThongBao(this, "Cập nhật cấu hình lương thành công!", "SUCCESS");
            
            // Reload lại UI
            hienThiNhanVien(nvDuocChon);
            isSuccess = true;

        } catch (Exception e) {
            TienIchGiaoDien.hienThiThongBao(this, "Lỗi định dạng số hoặc lỗi hệ thống: " + e.getMessage(), "ERROR");
        }
    }

    public boolean isSuccess() { return isSuccess; }

    // =========================================================
    // COMPONENT HỖ TRỢ UI ĐẸP
    // =========================================================
    private class InputTienBig extends JPanel {
        JTextField txtSo;
        public InputTienBig() {
            setLayout(new BorderLayout(10, 0)); setOpaque(false);
            setBorder(new EmptyBorder(8, 15, 8, 15));

            txtSo = new JTextField();
            txtSo.setOpaque(false); txtSo.setBorder(null);
            txtSo.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 28f));
            txtSo.setForeground(COLOR_PRIMARY); // Chữ màu xanh dương to bự
            
            JLabel lblSuffix = new JLabel("VNĐ/h");
            lblSuffix.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f));
            lblSuffix.setForeground(COLOR_TEXT_MUTED);

            add(txtSo, BorderLayout.CENTER);
            add(lblSuffix, BorderLayout.EAST);

            // Tự động format phẩy
            txtSo.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) { formatTuDong(); }
            });
        }
        private void formatTuDong() {
            try {
                String raw = txtSo.getText().replaceAll("[^\\d]", "");
                if (!raw.isEmpty()) txtSo.setText(df.format(new BigDecimal(raw)));
            } catch (Exception ex) {}
        }
        public String getText() { return txtSo.getText(); }
        public void setText(String t) { txtSo.setText(t); formatTuDong(); }
        
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(txtSo.isFocusOwner() ? COLOR_PRIMARY : COLOR_BORDER);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8); g2.dispose();
        }
    }

    private class InputTextThuong extends JPanel {
        JTextField txt;
        public InputTextThuong(String hint, boolean isTien) {
            setLayout(new BorderLayout()); setOpaque(false);
            setBorder(new EmptyBorder(8, 15, 8, 15));
            txt = new JTextField(); txt.setOpaque(false); txt.setBorder(null);
            txt.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(14f));
            txt.setForeground(COLOR_TEXT_MAIN);
            TienIchGiaoDien.datPlaceholder(txt, hint);
            add(txt, BorderLayout.CENTER);

            if (isTien) {
                JLabel lblSuffix = new JLabel("VNĐ");
                lblSuffix.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f)); lblSuffix.setForeground(COLOR_TEXT_MUTED);
                add(lblSuffix, BorderLayout.EAST);
                txt.addFocusListener(new FocusAdapter() {
                    @Override public void focusLost(FocusEvent e) {
                        try { String r = txt.getText().replaceAll("[^\\d]", ""); if(!r.isEmpty()) txt.setText(df.format(new BigDecimal(r))); } catch (Exception ex) {}
                    }
                });
            }
        }
        public String getText() { return txt.getText(); }
        public void setText(String t) { txt.setText(t); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(txt.isFocusOwner() ? COLOR_PRIMARY : COLOR_BORDER); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8); g2.dispose();
        }
    }
}