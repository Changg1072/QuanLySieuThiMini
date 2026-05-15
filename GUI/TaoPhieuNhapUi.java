package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import Data.ChiTietLoHang;
import Data.LoHang;
import Data.NhaCungCap;
import Data.SanPham;
import GUI.HoTro.DinhDangUtil;
import GUI.HoTro.TienIchGiaoDien;
import Logic.ChiTietLoHangLogic;
import Logic.LoHangLogic;
import Logic.NhaCungCapLogic;
import Logic.SanPhamLogic;
import Logic.TaoMaTuDongLogic;

public class TaoPhieuNhapUi extends JPanel {

    private DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LoHangLogic lhLogic = new LoHangLogic();
    private ChiTietLoHangLogic ctLogic = new ChiTietLoHangLogic();
    private NhaCungCapLogic nccLogic = new NhaCungCapLogic();
    private SanPhamLogic spLogic = new SanPhamLogic();

    private FormGroup txtNgayNhap, txtMaLo;
 // 🔥 ĐÃ SỬA: Nhận FormGroup để khóa được cả nút
    private void setTextFieldReadOnly(FormGroup group, boolean readOnly) {
        JTextField field = group.getField();
        field.setEnabled(!readOnly); 
        field.setFocusable(!readOnly);
        
        // Khóa luôn cái nút lịch nếu có
        if (group.btnAction != null) {
            group.btnAction.setEnabled(!readOnly);
        }
        
        if (readOnly) {
            field.setBackground(new Color(241, 245, 249)); 
            field.setDisabledTextColor(new Color(15, 23, 42)); 
        } else {
            field.setBackground(Color.WHITE);
            field.setForeground(new Color(15, 23, 42));
        }
    }
    private ComboGroup cbNhaCungCap, cbSanPham;
    private FormGroup txtGiaNhap, txtSoLuong, txtNSX, txtHSD;

    // --- Light Theme Colors ---
    private Color bgMain = new Color(245, 247, 250);
    private Color cardWhite = Color.WHITE;
    private Color borderNormal = new Color(226, 232, 240);
    private Color borderFocus = new Color(59, 130, 246);
    private Color textPrimary = new Color(15, 23, 42);
    private Color textSecondary = new Color(100, 116, 139);
 // Khai báo biến toàn cục trên đầu class
    private List<GioHangItem> danhSachGioHang = new ArrayList<>();
    private JPanel pnlRowListContainer; // Thay cho tblGioHang
    private JLabel lblTongTienPhi, lblTongLoai; // Thay cho nhãn tổng tiền cũ
 // ==========================================
    // BIẾN QUẢN LÝ UI GIỎ HÀNG & SELECTION
    // ==========================================
    private List<GioHangItem> selectedItems = new ArrayList<>(); // Lưu các SP đang được tick
    private ModernCheckBox cbSelectAll;
    private JButton btnXoaTrang, btnTao, btnXoa; // Khai báo ra ngoài để dễ ẩn/hiện
    
    private Color SELECTED_COLOR = new Color(239, 246, 255);
    private Color SELECT_BORDER = new Color(59, 130, 246);
    private Color HOVER_COLOR = new Color(248, 250, 252);
    // Class nội bộ chứa dữ liệu 1 dòng giỏ hàng
    class GioHangItem {
        String maSP, tenSP, nsx, hsd;
        java.math.BigDecimal giaNhap, thanhTien;
        int soLuong;
        public GioHangItem(String maSP, String tenSP, java.math.BigDecimal giaNhap, int soLuong, String nsx, String hsd, java.math.BigDecimal thanhTien) {
            this.maSP = maSP; this.tenSP = tenSP; this.giaNhap = giaNhap; this.soLuong = soLuong; this.nsx = nsx; this.hsd = hsd; this.thanhTien = thanhTien;
        }
    }
    public TaoPhieuNhapUi() {
        setLayout(new BorderLayout());
        setBackground(bgMain);
        setBorder(new EmptyBorder(20, 25, 20, 25));

        initUI();
        setupAutoData();
        setupRealtimeValidation();
    }

    private void initUI() {
        JPanel pnlCard = new JPanel(new BorderLayout(0, 15)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(226, 232, 240)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        pnlCard.setOpaque(false);
        pnlCard.setBorder(new EmptyBorder(20, 25, 20, 25));

        // ==========================================
        // 1. MASTER (Ngày nhập, Mã lô, Nhà cung cấp)
        // ==========================================
        JPanel pnlMaster = new JPanel(new GridLayout(1, 3, 20, 0)); 
        pnlMaster.setOpaque(false);
        txtNgayNhap = new FormGroup("Ngày nhập kho", true, "📅"); 
        txtMaLo = new FormGroup("Mã lô hàng", false, null);
        txtMaLo.getField().setFocusable(false);
        cbNhaCungCap = new ComboGroup("Nhà cung cấp", getDanhSachNccCombo(), true); 
        pnlMaster.add(txtNgayNhap); pnlMaster.add(txtMaLo); pnlMaster.add(cbNhaCungCap);

        cbNhaCungCap.btnAction.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            GUI.HoTro.ThemNhaCungCapDialog dialog = new GUI.HoTro.ThemNhaCungCapDialog(parentWindow);
            dialog.setVisible(true);
            if (dialog.isSuccess()) {
                cbNhaCungCap.reloadItems(getDanhSachNccCombo());
                if (dialog.getNhaCungCapMoi() != null) cbNhaCungCap.selectItemById(dialog.getNhaCungCapMoi().getMaNCC());
            }
        });

        // ==========================================
        // 2. DETAIL (Sản phẩm, Số lượng, Giá, NSX, HSD)
        // ==========================================
        JPanel pnlDetail = new JPanel(new GridBagLayout()); pnlDetail.setOpaque(false);
     // Đổi 1 thành 0 ở tham số đầu tiên của MatteBorder
        pnlDetail.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(15, 0, 15, 0), BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240))));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(5, 10, 5, 10);
        
        cbSanPham = new ComboGroup("Chọn Sản phẩm", getDanhSachSpCombo(), true);
        cbSanPham.btnAction.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            GUI.HoTro.ThemSanPhamDialog dialog = new GUI.HoTro.ThemSanPhamDialog(parentWindow);
            dialog.setVisible(true);
            
            if (dialog.isSuccess()) {
                // 1. Load lại danh sách mới từ DB (lúc này đã có SP mới)
                cbSanPham.reloadItems(getDanhSachSpCombo());
                
                // 2. Lấy thông tin SP vừa tạo
                SanPham spVuaTao = dialog.getSanPhamMoi();
                if (spVuaTao != null) {
                    // 3. Tự động chọn sản phẩm đó trên ComboGroup
                    cbSanPham.selectItemById(spVuaTao.getMaSP());
                    
                    // 4. Có thể tự điền đơn giá bán làm gợi ý cho giá nhập nếu muốn
                    txtGiaNhap.setText(spVuaTao.getGiaBan().toPlainString());
                    txtSoLuong.getField().requestFocus();
                }
            }
        });
        txtSoLuong = new FormGroup("Số lượng", true, null); txtGiaNhap = new FormGroup("Giá nhập", true, null);
        txtNSX = new FormGroup("NSX", true, "📅"); txtHSD = new FormGroup("HSD", true, "📅");

        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; gbc.weightx=0.4; pnlDetail.add(cbSanPham, gbc);
        gbc.gridx=2; gbc.gridy=0; gbc.gridwidth=1; gbc.weightx=0.2; pnlDetail.add(txtGiaNhap, gbc);
        gbc.gridx=3; gbc.gridy=0; gbc.gridwidth=1; gbc.weightx=0.1; pnlDetail.add(txtSoLuong, gbc);
        gbc.gridx=0; gbc.gridy=1; gbc.gridwidth=1; gbc.weightx=0.2; pnlDetail.add(txtNSX, gbc);
        gbc.gridx=1; gbc.gridy=1; gbc.gridwidth=1; gbc.weightx=0.2; pnlDetail.add(txtHSD, gbc);
        
        JButton btnThemVaoPhieu = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("⬇ Thêm vào giỏ", new Color(16, 185, 129));
        btnThemVaoPhieu.setPreferredSize(new Dimension(150, 42));
        btnThemVaoPhieu.addActionListener(e -> handleThemChiTiet());
        gbc.gridx=2; gbc.gridy=1; gbc.gridwidth=2; gbc.weightx=0.3; pnlDetail.add(btnThemVaoPhieu, gbc);

        JPanel pnlTopWrapper = new JPanel(new BorderLayout()); pnlTopWrapper.setOpaque(false);
        pnlTopWrapper.add(pnlMaster, BorderLayout.NORTH); pnlTopWrapper.add(pnlDetail, BorderLayout.CENTER);

     // ==========================================
        // 3. GIỎ HÀNG (Đồng bộ UI Nhà Cung Cấp)
        // ==========================================
        JPanel pnlCart = new JPanel(new BorderLayout(0, 10));
        pnlCart.setOpaque(false);

        // Header giả (Đã thêm Checkbox All & Giãn cột chuẩn)
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.setBackground(new Color(241, 245, 249));
        header.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        header.setPreferredSize(new Dimension(0, 45));
        
        cbSelectAll = new ModernCheckBox();
        cbSelectAll.setPreferredSize(new Dimension(40, 20));
        cbSelectAll.addActionListener(e -> handleSelectAll(cbSelectAll.isSelected()));
        
        header.add(cbSelectAll);
        header.add(createHeaderLabel("STT", 60, true));
        header.add(createHeaderLabel("Mã SP", 90, true));
        header.add(createHeaderLabel("Tên Sản Phẩm", 400, true));
        header.add(createHeaderLabel("Giá Nhập", 160, true));
        header.add(createHeaderLabel("Số Lượng", 160, true));
        header.add(createHeaderLabel("NSX", 160, true));
        header.add(createHeaderLabel("HSD", 160, true));
        header.add(createHeaderLabel("Thành Tiền", 130, true));
        pnlCart.add(header, BorderLayout.NORTH);

        pnlRowListContainer = new JPanel();
        pnlRowListContainer.setLayout(new BoxLayout(pnlRowListContainer, BoxLayout.Y_AXIS));
        pnlRowListContainer.setBackground(Color.WHITE);

        JScrollPane scrollCart = new JScrollPane(pnlRowListContainer);
        GUI.HoTro.TienIchGiaoDien.thietLapThanhCuon(scrollCart);
        scrollCart.setBorder(BorderFactory.createEmptyBorder());
        pnlCart.add(scrollCart, BorderLayout.CENTER);

        // ==========================================
        // 4. FOOTER (Logic Ẩn/Hiện nút & Giãn cách)
        // ==========================================
        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.setOpaque(false);
        pnlBottom.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        // Đổi FlowLayout.LEFT, 40, 0 -> 40px chính là khoảng cách giữa Tổng Sản Phẩm và Tổng Tiền
        JPanel pnlTong = new JPanel(new FlowLayout(FlowLayout.LEFT, 150, 0));
        pnlTong.setOpaque(false);
        
        lblTongLoai = new JLabel("Tổng sản phẩm: 0"); // Đã đổi text
        lblTongLoai.setFont(GUI.HoTro.TienIchGiaoDien.FONT_DAM.deriveFont(16f));
        lblTongLoai.setForeground(new Color(100, 116, 139));
        
        lblTongTienPhi = new JLabel("Tổng Tiền Phiếu: 0 đ");
        lblTongTienPhi.setFont(GUI.HoTro.TienIchGiaoDien.FONT_DAM.deriveFont(18f));
        lblTongTienPhi.setForeground(new Color(245, 158, 11)); 
        
        pnlTong.add(lblTongLoai);
        pnlTong.add(lblTongTienPhi);
        pnlBottom.add(pnlTong, BorderLayout.WEST);

        // Nút bấm
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); pnlBtns.setOpaque(false);
        
        btnXoa = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("❌ Xóa", new Color(239, 68, 68));
        btnXoa.addActionListener(e -> handleXoaSelected());
        btnXoa.setVisible(false); // Ẩn mặc định
        
        btnXoaTrang = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("Làm mới", new Color(100, 116, 139));
        btnXoaTrang.addActionListener(e -> resetToanBoForm());
        
        btnTao = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("💾 LƯU PHIẾU NHẬP", GUI.HoTro.TienIchGiaoDien.MAU_CHINH); 
        btnTao.setPreferredSize(new Dimension(180, 42)); btnTao.addActionListener(e -> handleLuuToanBoPhieu());
        
        pnlBtns.add(btnXoa);
        pnlBtns.add(btnXoaTrang); 
        pnlBtns.add(btnTao);

        pnlBottom.add(pnlBtns, BorderLayout.EAST);
        // ==========================================
        // 5. RÁP TOÀN BỘ VÀO KHUNG CHÍNH
        // ==========================================
        pnlCard.add(pnlTopWrapper, BorderLayout.NORTH);
        pnlCard.add(pnlCart, BorderLayout.CENTER);
        pnlCard.add(pnlBottom, BorderLayout.SOUTH);

        add(pnlCard, BorderLayout.CENTER);
    }
    private JLabel createHeaderLabel(String text, int width, boolean isBold) {
        JLabel lbl = new JLabel(text);
        // Tăng size lên 15f cho rõ, và dùng FONT_DAM cho cả 2 nếu sếp vẫn thấy nhạt
        lbl.setFont(isBold ? TienIchGiaoDien.FONT_DAM.deriveFont(15f) : TienIchGiaoDien.FONT_CHINH.deriveFont(15f));
        
        // 🔥 Đen tuyệt đối 0,0,0
        lbl.setForeground(new Color(0, 0, 0)); 
        
        lbl.setPreferredSize(new Dimension(width, 25)); 
        return lbl;
    }

    private void renderCart() {
        pnlRowListContainer.removeAll();
        java.math.BigDecimal tongTienAll = java.math.BigDecimal.ZERO;

        for (int i = 0; i < danhSachGioHang.size(); i++) {
            GioHangItem item = danhSachGioHang.get(i);
            tongTienAll = tongTienAll.add(item.thanhTien);
            
            JPanel row = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    boolean isSelected = selectedItems.contains(item);
                    if (isSelected) g2.setColor(SELECTED_COLOR); else g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                    if (isSelected) {
                        g2.setColor(SELECT_BORDER); g2.fillRoundRect(0, 0, 6, getHeight(), 15, 15);
                        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                    } else {
                        g2.setColor(borderNormal); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                    }
                    g2.dispose(); super.paintComponent(g);
                }
            };
            row.setOpaque(false); row.setPreferredSize(new Dimension(0, 60)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            
            JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 18));
            content.setOpaque(false);
            
            // 1. Checkbox
            ModernCheckBox cbSelect = new ModernCheckBox();
            cbSelect.setPreferredSize(new Dimension(50, 25));
            cbSelect.setSelected(selectedItems.contains(item));
            cbSelect.addActionListener(e -> handleSelection(item, cbSelect.isSelected(), row));
            content.add(cbSelect);
            
            // 2. Data
            JLabel lblSTT = createHeaderLabel(String.valueOf(i + 1), 60, true);
            
            lblSTT.setForeground(new Color(0, 0, 0));
            content.add(lblSTT);
            
            content.add(createHeaderLabel(item.maSP, 90, true));
            content.add(createHeaderLabel(item.tenSP, 400, true));
            content.add(createHeaderLabel(GUI.HoTro.DinhDangUtil.dinhDangTien(item.giaNhap), 160, false));
            
            JLabel lblSL = createHeaderLabel(String.valueOf(item.soLuong), 160, false);
            lblSL.setForeground(new Color(0, 102, 0));
            content.add(lblSL);
            
            content.add(createHeaderLabel(item.nsx, 160, false));
            content.add(createHeaderLabel(item.hsd, 160, false));
            
            JLabel lblTien = createHeaderLabel(GUI.HoTro.DinhDangUtil.dinhDangTien(item.thanhTien), 130, false);
            lblTien.setForeground(new Color(204, 0, 0));
            content.add(lblTien);

            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { if(!selectedItems.contains(item)) { row.setBackground(HOVER_COLOR); row.repaint(); } }
                @Override public void mouseExited(java.awt.event.MouseEvent e)  { row.setBackground(Color.WHITE); row.repaint(); }
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { cbSelect.doClick(); }
            });

            row.add(content, BorderLayout.CENTER);
            pnlRowListContainer.add(row);
            pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        lblTongLoai.setText("Tổng sản phẩm: " + danhSachGioHang.size());
        lblTongTienPhi.setText("Tổng Tiền Phiếu: " + GUI.HoTro.DinhDangUtil.dinhDangTien(tongTienAll));

        pnlRowListContainer.revalidate();
        pnlRowListContainer.repaint();
        updateSelectionUIState();
    }

    // ==========================================
    // LOGIC TICK CHỌN & ẨN/HIỆN NÚT
    // ==========================================
    private void handleSelection(GioHangItem item, boolean isSelected, JPanel rowPanel) {
        if (isSelected) { if (!selectedItems.contains(item)) selectedItems.add(item); } 
        else { selectedItems.remove(item); }
        rowPanel.repaint();
        updateSelectionUIState();
    }

    private void handleSelectAll(boolean isSelected) {
        selectedItems.clear();
        if (isSelected) selectedItems.addAll(danhSachGioHang);
        renderCart(); 
    }

    private void updateSelectionUIState() {
        boolean hasSelection = !selectedItems.isEmpty();
        btnXoa.setVisible(hasSelection);
        btnXoaTrang.setVisible(!hasSelection);
        btnTao.setVisible(!hasSelection);

        if (danhSachGioHang.isEmpty()) cbSelectAll.setSelected(false);
        else cbSelectAll.setSelected(selectedItems.size() == danhSachGioHang.size());
        cbSelectAll.repaint();
    }

    private void handleXoaSelected() {
        danhSachGioHang.removeAll(selectedItems);
        selectedItems.clear();
        renderCart();
    }

    // ==========================================
    // CHECKBOX GIAO DIỆN MODERN
    // ==========================================
    private class ModernCheckBox extends javax.swing.JCheckBox {
        public ModernCheckBox() { setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = 18; int y = (getHeight() - size) / 2; int x = (getWidth() - size) / 2; 
            if (isSelected()) {
                g2.setColor(new Color(59, 130, 246)); g2.fillRoundRect(x, y, size, size, 5, 5); 
                g2.setColor(Color.WHITE); g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 9, x + 8, y + 13); g2.drawLine(x + 8, y + 13, x + 14, y + 5); 
            } else {
                g2.setColor(Color.WHITE); g2.fillRoundRect(x, y, size, size, 5, 5);
                g2.setColor(new Color(180, 185, 195)); g2.drawRoundRect(x, y, size, size, 5, 5);
            }
            g2.dispose();
        }
    }
    private void setupAutoData() {
        txtNgayNhap.setText(LocalDate.now().format(fmt));
        try { txtMaLo.setText(TaoMaTuDongLogic.taoMaLoHang()); } catch (Exception e) { txtMaLo.setText("LH" + System.currentTimeMillis()%10000); }
    }

 // ==========================================
    // 🔥 FORM GROUP MỚI (CHUẨN MÀU XÁM)
    // ==========================================
    private class FormGroup extends JPanel {
        private JTextField txt; private JLabel lblError; private boolean isFocus = false;
        private MiniDatePicker datePicker;
        private long lastCloseTime = 0; 
        public JButton btnAction;
        private boolean isEditable; // 🔥 Thêm biến quản lý trạng thái

        public FormGroup(String title, boolean editable, String iconBtn) {
            this.isEditable = editable;
            setLayout(new BorderLayout(0, 6)); setOpaque(false);
            JLabel lbl = new JLabel(title); lbl.setFont(TienIchGiaoDien.FONT_DAM); lbl.setForeground(textPrimary);
            
            JPanel pnlWrap = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // 🔥 TỰ ĐỘNG ĐỔI MÀU NỀN DỰA VÀO BIẾN isEditable
                    g2.setColor(isEditable ? Color.WHITE : new Color(241, 245, 249)); 
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(isFocus ? borderFocus : borderNormal); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.dispose(); super.paintComponent(g);
                }
            };
            pnlWrap.setPreferredSize(new Dimension(0, 42)); pnlWrap.setOpaque(false);
            
            txt = new JTextField(); txt.setOpaque(false); txt.setBorder(new EmptyBorder(0, 15, 0, 5)); 
            txt.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(15f)); txt.setEditable(editable); txt.setForeground(textPrimary);
            txt.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { isFocus = true; pnlWrap.repaint(); } public void focusLost(FocusEvent e) { isFocus = false; pnlWrap.repaint(); } });
            pnlWrap.add(txt, BorderLayout.CENTER);
            
            if (iconBtn != null) {
            	btnAction = new JButton() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int size = 20; int x = (getWidth() - size) / 2; int y = (getHeight() - size) / 2;
                        g2.setColor(new Color(59, 130, 246)); g2.fillRoundRect(x, y, size, size, 6, 6);
                        g2.setColor(new Color(239, 68, 68)); g2.fillRoundRect(x, y, size, 7, 6, 6);
                        g2.setColor(Color.WHITE); g2.fillRoundRect(x + 4, y - 2, 3, 5, 2, 2); g2.fillRoundRect(x + 13, y - 2, 3, 5, 2, 2);
                        int startX = x + 3; int startY = y + 10;
                        for(int row=0; row<2; row++) { for(int col=0; col<3; col++) { g2.fillRect(startX + col*5, startY + row*4, 3, 3); } }
                        g2.dispose(); super.paintComponent(g);
                    }
                }; 
                btnAction.setContentAreaFilled(false); btnAction.setBorderPainted(false); btnAction.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnAction.setPreferredSize(new Dimension(42, 42));
                
                datePicker = new MiniDatePicker(txt);
                datePicker.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                    public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
                    public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { lastCloseTime = System.currentTimeMillis(); }
                    public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { lastCloseTime = System.currentTimeMillis(); }
                });

                btnAction.addActionListener(e -> { 
                    if (System.currentTimeMillis() - lastCloseTime < 300) return;
                    txt.requestFocusInWindow(); datePicker.updateDateFromText(); 
                    datePicker.show(btnAction, btnAction.getWidth() - datePicker.getPreferredSize().width, btnAction.getHeight() + 4);
                });
                pnlWrap.add(btnAction, BorderLayout.EAST);
            }
            lblError = new JLabel(" "); lblError.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f)); lblError.setForeground(Color.RED); 
            lblError.setPreferredSize(new Dimension(10, 18)); lblError.setMinimumSize(new Dimension(10, 18));
            add(lbl, BorderLayout.NORTH); add(pnlWrap, BorderLayout.CENTER); add(lblError, BorderLayout.SOUTH);
        }
        public void setText(String t) { txt.setText(t); } public String getText() { return txt.getText(); }
        public void setError(String m) { lblError.setText(m); } public void clearError() { lblError.setText(" "); }
        public JTextField getField() { return txt; }
        
        // 🔥 HÀM KHÓA TOÀN DIỆN MỚI
        public void setEditable(boolean b) {
            this.isEditable = b;
            txt.setEnabled(b);
            txt.setFocusable(b);
            if (btnAction != null) btnAction.setEnabled(b);
            txt.setDisabledTextColor(new Color(15, 23, 42));
            repaint(); // Bắt buộc vẽ lại để hiện màu xám
        }
    }



 // ==========================================
    // 🔥 COMBO GROUP: FIX LỖI CLICK ĐỂ THU LẠI 🔥
    // ==========================================
    private class ComboGroup extends JPanel {
        private JTextField txt; 
        public JButton btnAction;
        private JButton btnArrow;
        private ComboItem[] allItems;
        private ComboItem selectedItem = new ComboItem("", "");
        
        private JPopupMenu popup;
        private JPanel pnlList;
        private boolean isFocus = false;
        private boolean isSelecting = false; 
        private long lastCloseTime = 0; // 🔥 Thêm biến lưu thời gian đóng
        private boolean isEditable = true;

        public ComboGroup(String title, ComboItem[] items, boolean hasPlus) {
            setLayout(new BorderLayout(0, 6)); setOpaque(false);
            this.allItems = items;
            
            JLabel lbl = new JLabel(title); lbl.setFont(TienIchGiaoDien.FONT_DAM); lbl.setForeground(textPrimary);
            
            JPanel pnlMainWrap = new JPanel(new BorderLayout(5, 0)); pnlMainWrap.setOpaque(false);
            
            JPanel pnlInputWrap = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // 🔥 TỰ ĐỘNG ĐỔI MÀU NHƯ MÃ LÔ HÀNG
                    g2.setColor(isEditable ? Color.WHITE : new Color(241, 245, 249)); 
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(isFocus ? borderFocus : borderNormal); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                    g2.dispose(); super.paintComponent(g);
                }
            };
            pnlInputWrap.setPreferredSize(new Dimension(0, 42)); pnlInputWrap.setOpaque(false);

            txt = new JTextField(); txt.setOpaque(false); txt.setBorder(new EmptyBorder(0, 15, 0, 5)); 
            txt.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(15f)); txt.setForeground(textPrimary);
            
            txt.addFocusListener(new FocusAdapter() { 
                public void focusGained(FocusEvent e) { isFocus = true; pnlInputWrap.repaint(); } 
                public void focusLost(FocusEvent e) { isFocus = false; pnlInputWrap.repaint(); } 
            });

            btnArrow = new JButton("▼"); btnArrow.setContentAreaFilled(false); btnArrow.setBorderPainted(false); 
            btnArrow.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnArrow.setForeground(textSecondary);
            btnArrow.setFont(new Font("Arial", Font.PLAIN, 12));

            pnlInputWrap.add(txt, BorderLayout.CENTER);
            pnlInputWrap.add(btnArrow, BorderLayout.EAST);
            pnlMainWrap.add(pnlInputWrap, BorderLayout.CENTER);

            if (hasPlus) {
                btnAction = TienIchGiaoDien.taoNutHienDai("+", new Color(16, 185, 129));
                btnAction.setPreferredSize(new Dimension(42, 42)); 
                pnlMainWrap.add(btnAction, BorderLayout.EAST);
            }

            popup = new JPopupMenu(); 
            popup.setBorder(BorderFactory.createLineBorder(borderNormal));
            popup.setBackground(Color.WHITE);
            popup.setFocusable(false); 
            
            // 🔥 Lắng nghe sự kiện để chốt thời gian Popup biến mất
            popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                    lastCloseTime = System.currentTimeMillis();
                }
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
            });
            
            pnlList = new JPanel(); pnlList.setLayout(new BoxLayout(pnlList, BoxLayout.Y_AXIS));
            pnlList.setBackground(Color.WHITE);
            
            JScrollPane scroll = new JScrollPane(pnlList);
            TienIchGiaoDien.thietLapThanhCuon(scroll);
            scroll.setBorder(null);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            popup.add(scroll);

            txt.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filter(); }
                public void removeUpdate(DocumentEvent e) { filter(); }
                public void changedUpdate(DocumentEvent e) { filter(); }
            });

            // 🔥 FIX LỖI TOGGLE NÚT MŨI TÊN
            btnArrow.addActionListener(e -> {
                // Nếu popup vừa mới đóng (trong vòng 150 mili-giây) do click vào chính nút này -> Không mở lại nữa
                if (System.currentTimeMillis() - lastCloseTime < 300) {
                    return;
                }
                txt.requestFocusInWindow();
                showPopup(true); 
            });

            JLabel dmy = new JLabel(" "); dmy.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f));
            add(lbl, BorderLayout.NORTH); add(pnlMainWrap, BorderLayout.CENTER); add(dmy, BorderLayout.SOUTH);
        }

        private void filter() {
            if (isSelecting) return; 
            SwingUtilities.invokeLater(() -> {
                if (txt.getText().trim().isEmpty()) popup.setVisible(false);
                else showPopup(false);
            });
        }

        private void showPopup(boolean showAll) {
            String keyword = showAll ? "" : DinhDangUtil.loaiBoDauTiengViet(txt.getText().toLowerCase().trim());
            pnlList.removeAll();
            
            int count = 0;
            for (ComboItem item : allItems) {
                if (item.id == null || item.id.isEmpty()) continue;
                String nameVal = DinhDangUtil.loaiBoDauTiengViet(item.name.toLowerCase());
                String idVal = item.id.toLowerCase();
                
                if (showAll || nameVal.contains(keyword) || idVal.contains(keyword)) {
                    pnlList.add(createItemLabel(item));
                    count++;
                }
            }

            if (count == 0 && !showAll) {
                JLabel lblEmpty = new JLabel("Không tìm thấy...");
                lblEmpty.setBorder(new EmptyBorder(10, 15, 10, 15));
                lblEmpty.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(14f));
                lblEmpty.setForeground(Color.RED);
                pnlList.add(lblEmpty);
                count = 1;
            }

            int h = Math.min(count * 35, 250); 
            if (count == 0) h = 0;
            
            if (h > 0) {
                popup.setPreferredSize(new Dimension(txt.getWidth() + btnArrow.getWidth(), h));
                pnlList.revalidate(); pnlList.repaint();
                
                if (!popup.isVisible()) {
                    popup.show(txt, 0, txt.getHeight() + 4);
                }
                txt.requestFocusInWindow(); 
            } else {
                popup.setVisible(false);
            }
        }

        private JLabel createItemLabel(ComboItem item) {
            JLabel lbl = new JLabel(item.name);
            lbl.setOpaque(true); lbl.setBackground(Color.WHITE);
            lbl.setBorder(new EmptyBorder(8, 15, 8, 15));
            lbl.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(14f));
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35)); 
            
            lbl.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { lbl.setBackground(borderFocus); lbl.setForeground(Color.WHITE); }
                public void mouseExited(MouseEvent e) { lbl.setBackground(Color.WHITE); lbl.setForeground(textPrimary); }
                public void mousePressed(MouseEvent e) {
                    isSelecting = true;
                    selectedItem = item;
                    txt.setText(item.name);
                    popup.setVisible(false);
                    SwingUtilities.invokeLater(() -> isSelecting = false);
                }
            });
            return lbl;
        }

        public ComboItem getSelectedItem() { 
            if (selectedItem != null && txt.getText().equals(selectedItem.name)) return selectedItem;
            String text = txt.getText().trim();
            for(ComboItem item : allItems) {
                if(item.name.equals(text) || item.id.equals(text)) {
                    selectedItem = item; return item;
                }
            }
            return new ComboItem("", ""); 
        }
        
        public void reloadItems(ComboItem[] newItems) { 
            this.allItems = newItems; 
            this.selectedItem = new ComboItem("","");
            isSelecting = true; txt.setText(""); isSelecting = false;
        }
        
        public void selectItemById(String id) { 
            for (ComboItem item : allItems) { 
                if (item.id.equals(id)) { 
                    isSelecting = true;
                    selectedItem = item;
                    txt.setText(item.name); 
                    SwingUtilities.invokeLater(() -> isSelecting = false);
                    break; 
                } 
            } 
        }
        // Dán vào cuối class ComboGroup sếp nhé
        public void clearSelection() {
            isSelecting = true;
            selectedItem = new ComboItem("", "");
            txt.setText("");
            isSelecting = false;
        }
        public void setEditable(boolean b) {
            this.isEditable = b;
            txt.setEnabled(b);
            btnArrow.setEnabled(b);
            if (btnAction != null) btnAction.setEnabled(b);
            txt.setDisabledTextColor(new Color(15, 23, 42));
            repaint(); // Bắt buộc vẽ lại
        }
    }

    private void setupRealtimeValidation() {
        // 1. Validate Ngày Nhập Kho
        txtNgayNhap.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { check(); } public void removeUpdate(DocumentEvent e) { check(); } public void changedUpdate(DocumentEvent e) { check(); }
            private void check() {
                try { 
                    LocalDate d = LocalDate.parse(txtNgayNhap.getText(), fmt);
                    if (d.isAfter(LocalDate.now())) txtNgayNhap.setError("Không được chọn ngày tương lai!"); 
                    else txtNgayNhap.clearError();
                } catch (DateTimeParseException ex) { 
                    txtNgayNhap.setError("Sai định dạng dd/MM/yyyy"); 
                }
            }
        });

        // 2. 🔥 TỔNG HỢP VALIDATE CHO NSX & HSD 🔥
        DocumentListener dateListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { check(); } public void removeUpdate(DocumentEvent e) { check(); } public void changedUpdate(DocumentEvent e) { check(); }
            private void check() {
                try {
                    LocalDate nsx = null, hsd = null;
                    LocalDate today = LocalDate.now();

                    if(!txtNSX.getText().isEmpty()) nsx = LocalDate.parse(txtNSX.getText(), fmt);
                    if(!txtHSD.getText().isEmpty()) hsd = LocalDate.parse(txtHSD.getText(), fmt);
                    
                    txtNSX.clearError(); 
                    txtHSD.clearError();
                    
                    // Lỗi 1: Ngày sản xuất lớn hơn hiện tại
                    if (nsx != null && nsx.isAfter(today)) {
                        txtNSX.setError("NSX không được lớn hơn hôm nay!");
                    }
                    
                    // Lỗi 2: Hạn sử dụng bé hơn hiện tại
                    if (hsd != null && hsd.isBefore(today)) {
                        txtHSD.setError("Hàng đã hết hạn!");
                    }
                    
                    // Lỗi 3: NSX lớn hơn hoặc bằng HSD
                    if (nsx != null && hsd != null) {
                        if (!nsx.isBefore(hsd)) {
                            txtNSX.setError("NSX phải trước HSD!");
                            txtHSD.setError("HSD phải sau NSX!");
                        }
                    }
                    
                } catch (DateTimeParseException ex) {
                    if(!txtNSX.getText().matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$") && !txtNSX.getText().isEmpty()) {
                        txtNSX.setError("Sai định dạng");
                    }
                    if(!txtHSD.getText().matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$") && !txtHSD.getText().isEmpty()) {
                        txtHSD.setError("Sai định dạng");
                    }
                }
            }
        };
        
        txtNSX.getField().getDocument().addDocumentListener(dateListener);
        txtHSD.getField().getDocument().addDocumentListener(dateListener);
    }

    private ComboItem[] getDanhSachNccCombo() {
        List<ComboItem> items = new java.util.ArrayList<>();
        try { for (NhaCungCap ncc : nccLogic.layDanhSachNhaCungCap()) if (!"Ngừng hợp tác".equalsIgnoreCase(ncc.getTrangThai())) items.add(new ComboItem(ncc.getMaNCC(), ncc.getTenNCC())); } catch (Exception e) {}
        return items.isEmpty() ? new ComboItem[]{new ComboItem("", "-- Trống --")} : items.toArray(new ComboItem[0]);
    }
    private ComboItem[] getDanhSachSpCombo() {
        List<ComboItem> items = new java.util.ArrayList<>();
        try { 
            for (SanPham sp : spLogic.layDanhSachSanPham()) {
                // 🔥 ĐÃ SỬA: Chỉ lấy tên sản phẩm, bỏ mã SP ở đầu
                items.add(new ComboItem(sp.getMaSP(), sp.getTenSP())); 
            }
        } catch (Exception e) {}
        return items.isEmpty() ? new ComboItem[]{new ComboItem("", "-- Trống --")} : items.toArray(new ComboItem[0]);
    }

    private void handleThemChiTiet() {
        ComboItem sp = cbSanPham.getSelectedItem();
        String strGia = txtGiaNhap.getText().trim();
        String strSl = txtSoLuong.getText().trim();
        String strNSX = txtNSX.getText().trim();
        String strHSD = txtHSD.getText().trim();

        if (sp.id.isEmpty() || strGia.isEmpty() || strSl.isEmpty() || strNSX.isEmpty() || strHSD.isEmpty()) {
            GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, "Vui lòng nhập đầy đủ thông tin sản phẩm!", "ERROR");
            return;
        }

        try {
        	// 🔥 THÊM ĐOẠN NÀY ĐỂ CHẶN CỨNG LỖI NGÀY THÁNG
            LocalDate nsx = LocalDate.parse(strNSX, fmt);
            LocalDate hsd = LocalDate.parse(strHSD, fmt);
            LocalDate today = LocalDate.now();

            if (nsx.isAfter(today)) {
                GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, "Lỗi: Ngày sản xuất không được lớn hơn hôm nay!", "ERROR");
                return;
            }
            if (hsd.isBefore(today)) {
                GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, "Từ chối nhập: Sản phẩm này đã hết hạn sử dụng!", "ERROR");
                return;
            }
            if (!nsx.isBefore(hsd)) {
                GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, "Lỗi: Ngày sản xuất phải trước Hạn sử dụng!", "ERROR");
                return;
            }
            BigDecimal giaNhap = new BigDecimal(strGia.replace(".", ""));
            int slThem = Integer.parseInt(strSl);
            
            // 🔥 LOGIC CỘNG DỒN THÔNG MINH
            boolean daTonTai = false;
            for (GioHangItem item : danhSachGioHang) {
                if (item.maSP.equals(sp.id)) {
                    item.soLuong += slThem; // Cộng thêm số lượng mới vào
                    item.thanhTien = item.giaNhap.multiply(new BigDecimal(item.soLuong));
                    daTonTai = true;
                    break;
                }
            }

            if (!daTonTai) {
                BigDecimal thanhTien = giaNhap.multiply(new BigDecimal(slThem));
                danhSachGioHang.add(new GioHangItem(sp.id, sp.name, giaNhap, slThem, strNSX, strHSD, thanhTien));
            }

            renderCart(); // Vẽ lại giỏ hàng
            if (!danhSachGioHang.isEmpty()) {
                cbNhaCungCap.setEditable(false);       
                txtNgayNhap.setEditable(false); // Trực tiếp gọi hàm trên FormGroup
            }

            // Reset các ô nhập sản phẩm phía dưới như bình thường...
            txtGiaNhap.setText(""); 
            txtSoLuong.setText("");
            cbSanPham.clearSelection();
            txtNSX.setText(""); 
            txtHSD.setText("");
            cbSanPham.requestFocus();
        } catch (Exception ex) {
            GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, "Dữ liệu nhập không hợp lệ!", "ERROR");
        }
    }
    private void handleLuuToanBoPhieu() {
        try {
            if (danhSachGioHang.isEmpty()) throw new Exception("Giỏ hàng đang trống!");
            ComboItem ncc = cbNhaCungCap.getSelectedItem();
            if (ncc == null || ncc.id.isEmpty()) throw new Exception("Chưa chọn nhà cung cấp!");
            
            String maLo = txtMaLo.getText().trim();
            LocalDate ngayNhap = LocalDate.parse(txtNgayNhap.getText(), fmt);

            // Tính tổng tiền từ danh sách giỏ hàng
            BigDecimal tongTien = BigDecimal.ZERO;
            for (GioHangItem item : danhSachGioHang) tongTien = tongTien.add(item.thanhTien);

            // 1. Lưu Lô hàng
            LoHang lh = new LoHang.ThoXayLoHang().ganMaLoHang(maLo).ganMaNCC(ncc.id).ganNgayNhapKho(ngayNhap).ganThanhTien(tongTien).taoMoi();
            lhLogic.themLoHang(lh); 

            // 2. Lưu Chi tiết lô hàng
            for (GioHangItem item : danhSachGioHang) {
                ChiTietLoHang ct = new ChiTietLoHang.ThoXayChiTietLoHang()
                        .ganMaLoHang(maLo).ganMaSP(item.maSP)
                        .ganGiaNhap(item.giaNhap).ganSoLuongNhap(item.soLuong)
                        .ganNSX(LocalDate.parse(item.nsx, fmt)).ganHSD(LocalDate.parse(item.hsd, fmt))
                        .ganSoLuongTon(item.soLuong).taoMoi();
                ctLogic.themChiTietLoHang(ct);
            }
            
            TienIchGiaoDien.hienThiThongBao(this, "Lưu phiếu nhập thành công!", "SUCCESS"); 
            resetToanBoForm();
        } catch (Exception ex) { 
            TienIchGiaoDien.hienThiThongBao(this, ex.getMessage(), "ERROR"); 
        }
    }

    private void resetToanBoForm() {
        danhSachGioHang.clear();
        renderCart(); 
        cbNhaCungCap.setEditable(true);
        cbNhaCungCap.clearSelection();
        txtNgayNhap.setEditable(true);
        
     // 🔥 ĐÃ SỬA: Truyền thẳng txtNgayNhap vào
        setTextFieldReadOnly(txtNgayNhap, false);
        // Các ô khác reset như cũ...
        txtGiaNhap.setText(""); txtSoLuong.setText("");
        cbSanPham.clearSelection();
    }
    // ==========================================
    // 🔥 MINI DATE PICKER: THÊM HÀM UPDATE THEO TEXT 🔥
    // ==========================================
    private class MiniDatePicker extends JPopupMenu {
        private YearMonth currentMonth;
        private JTextField targetField;

        public MiniDatePicker(JTextField targetField) {
            this.targetField = targetField;
            currentMonth = YearMonth.now();
            setBackground(Color.WHITE); setBorder(BorderFactory.createLineBorder(borderFocus, 2));
            setLayout(new BorderLayout()); buildUI();
        }
        
        // Cập nhật lại lịch nếu sếp tự gõ tay một ngày bất kỳ rồi bấm mở lịch
        public void updateDateFromText() {
            try {
                if (!targetField.getText().isEmpty()) currentMonth = YearMonth.from(LocalDate.parse(targetField.getText(), fmt));
                else currentMonth = YearMonth.now();
            } catch(Exception ex) { currentMonth = YearMonth.now(); }
            buildUI(); revalidate(); repaint();
        }

        private void buildUI() {
            removeAll();
            JPanel pnlHeader = new JPanel(new BorderLayout()); pnlHeader.setBackground(TienIchGiaoDien.MAU_CHINH); pnlHeader.setBorder(new EmptyBorder(10,10,10,10));
            JButton btnPrev = new JButton("❮"); btnPrev.setContentAreaFilled(false); btnPrev.setForeground(Color.WHITE); btnPrev.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(16f)); btnPrev.setBorder(null); btnPrev.setCursor(new Cursor(Cursor.HAND_CURSOR));
            JButton btnNext = new JButton("❯"); btnNext.setContentAreaFilled(false); btnNext.setForeground(Color.WHITE); btnNext.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(16f)); btnNext.setBorder(null); btnNext.setCursor(new Cursor(Cursor.HAND_CURSOR));
            JLabel lblMonth = new JLabel("Tháng " + currentMonth.getMonthValue() + " - " + currentMonth.getYear(), SwingConstants.CENTER);
            lblMonth.setForeground(Color.WHITE); lblMonth.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(16f));
            btnPrev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); buildUI(); revalidate(); repaint(); });
            btnNext.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); buildUI(); revalidate(); repaint(); });
            pnlHeader.add(btnPrev, BorderLayout.WEST); pnlHeader.add(lblMonth, BorderLayout.CENTER); pnlHeader.add(btnNext, BorderLayout.EAST);
            add(pnlHeader, BorderLayout.NORTH);
            
            JPanel pnlDays = new JPanel(new GridLayout(0, 7, 5, 5)); pnlDays.setOpaque(false); pnlDays.setBorder(new EmptyBorder(10,10,10,10));
            String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (String d : days) { JLabel l = new JLabel(d, SwingConstants.CENTER); l.setForeground(textSecondary); l.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(13f)); pnlDays.add(l); }
            int dayOfWeek = currentMonth.atDay(1).getDayOfWeek().getValue();
            for (int i = 1; i < dayOfWeek; i++) pnlDays.add(new JLabel("")); 
            for (int i = 1; i <= currentMonth.lengthOfMonth(); i++) {
                int day = i; JButton b = new JButton(String.valueOf(day));
                b.setBackground(Color.WHITE); b.setForeground(textPrimary); b.setFocusPainted(false); b.setBorder(BorderFactory.createLineBorder(borderNormal));
                b.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(14f));
                b.setPreferredSize(new Dimension(35, 35)); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
                if (LocalDate.now().equals(currentMonth.atDay(day))) { b.setBackground(new Color(219, 234, 254)); b.setBorder(BorderFactory.createLineBorder(borderFocus)); }
                b.addActionListener(e -> { targetField.setText(currentMonth.atDay(day).format(fmt)); setVisible(false); });
                pnlDays.add(b);
            }
            add(pnlDays, BorderLayout.CENTER);
        }
    }
    class ComboItem { String id; String name; public ComboItem(String id, String name) { this.id = id; this.name = name; } @Override public String toString() { return name; } }
}