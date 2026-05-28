package GUI;

import Dao.TruyVanSieuTocDAO;
import GUI.HoTro.*;
import Data.SanPham;
import Logic.QuanLyAnh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DanhSachSPUi extends JPanel {

    // ==========================================
    // 🎨 HỆ MÀU UI MỚI (Lấy cảm hứng từ LichSuGiamGiaUI)
    // ==========================================
    public static final Color BG_MAIN = new Color(241, 245, 249);
    private final Color COLOR_WHITE = new Color(255, 255, 255);
    private final Color TEXT_MAIN = new Color(15, 23, 42);      
    private final Color TEXT_SUB = new Color(100, 116, 139);    
    private final Color ACCENT_HOVER = new Color(239, 246, 255); // Màu xanh dương nhạt khi Hover
    private final Font FONT_BOLD = new Font("Calibri", Font.BOLD, 15);
    private final Font FONT_REGULAR = new Font("Calibri", Font.PLAIN, 14);

    private CallBackGioHang gioHangCallback;
    private TheBongDo.RoundedTextField txtTimKiem;

    private JTable tableSP;
    private DefaultTableModel tableModel;
    private int hoveredRow = -1; // 🖱️ Biến track vị trí chuột để làm hiệu ứng Hover xịn sò

    public enum UIMode {
        BAN_HANG,
        QUAN_LY
    }
    private UIMode currentMode;
    public DanhSachSPUi(CallBackGioHang callback) {
        this(callback, UIMode.BAN_HANG); 
    }   
    
    private TruyVanSieuTocDAO.DuLieuBanHangDTO dataBanHangCache;
    private Map<String, TheSanPham> mapSanPham = new HashMap<>();

    public interface CallBackGioHang {
        void capNhatGioHang(SanPham sp, int soLuongThayDoi, TheSanPham card);
    }

    public DanhSachSPUi(CallBackGioHang callback, UIMode mode) {
        this.gioHangCallback = callback;
        this.currentMode = (mode != null) ? mode : UIMode.BAN_HANG;
        
        setLayout(new BorderLayout());
        setBackground(BG_MAIN);

        add(taoMainContent(), BorderLayout.CENTER);

        taiDuLieuBanHangSieuToc("ALL");

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                Component comp = (Component) event.getSource();
                if (txtTimKiem != null && comp != txtTimKiem && !SwingUtilities.isDescendingFrom(comp, txtTimKiem)) {
                    if (comp instanceof JPanel || comp instanceof JViewport || comp instanceof JScrollPane || comp instanceof JLabel || comp instanceof JTable) {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private JPanel taoMainContent() {
        // --- GIỮ NGUYÊN LOGIC THANH TÌM KIẾM ---
        JPanel pnlTopBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        pnlTopBar.setBackground(BG_MAIN);

        JPanel pnlSearchWrapper = new JPanel(new BorderLayout(10, 0));
        pnlSearchWrapper.setBackground(Color.WHITE);
        pnlSearchWrapper.setPreferredSize(new Dimension(650, 45));
        pnlSearchWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
            new EmptyBorder(0, 15, 0, 15)
        ));

        JLabel lblIcon = new JLabel("TÌM KIẾM"); 
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblIcon.setForeground(new Color(148, 163, 184)); 

        JTextField txtTimKiem = new JTextField("Nhập tên hoặc mã sản phẩm...");
        txtTimKiem.setBorder(null);
        txtTimKiem.setBackground(Color.WHITE);
        txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtTimKiem.setForeground(new Color(148, 163, 184)); 
        
        txtTimKiem.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtTimKiem.getText().equals("Nhập tên hoặc mã sản phẩm...")) {
                    txtTimKiem.setText("");
                    txtTimKiem.setForeground(new Color(30, 41, 59)); 
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (txtTimKiem.getText().isEmpty()) {
                    txtTimKiem.setText("Nhập tên hoặc mã sản phẩm...");
                    txtTimKiem.setForeground(new Color(148, 163, 184)); 
                }
            }
        });

        txtTimKiem.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String txt = txtTimKiem.getText();
                if (!txt.equals("Nhập tên hoặc mã sản phẩm...")) {
                    thucHienTimKiem(txt);
                }
            }
        });

        pnlSearchWrapper.add(lblIcon, BorderLayout.WEST);
        pnlSearchWrapper.add(txtTimKiem, BorderLayout.CENTER);
        pnlTopBar.add(pnlSearchWrapper);

        // --- 🚀 BẮT ĐẦU MAKE-OVER GIAO DIỆN BẢNG ---
        String[] columns = {"Hình ảnh", "Tên sản phẩm", "Mã loại", "Tồn kho", "Giá bán", "Trạng thái", "Hành động"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; 
            }
        };

        tableSP = new JTable(tableModel);
        
        // ✨ THỦ THUẬT UX/UI: Chuyển JTable thành dạng Card List
        tableSP.setRowHeight(68); // Tăng chiều cao để nhìn giống cái thẻ (Card)
        tableSP.setFillsViewportHeight(true);
        tableSP.setBackground(BG_MAIN); // Nền bảng trùng nền Panel
        tableSP.setShowGrid(false); // Xóa sạch đường kẻ
        tableSP.setIntercellSpacing(new Dimension(0, 8)); // 🌟 TẠO KHOẢNG CÁCH GIỮA CÁC HÀNG (CARD GAP)
        tableSP.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // ✨ HIỆU ỨNG HOVER SIÊU MƯỢT
        tableSP.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tableSP.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    tableSP.repaint();
                }
            }
        });
        tableSP.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                tableSP.repaint();
            }
        });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        tableSP.setRowSorter(sorter);
        sorter.setSortable(0, false); 
        sorter.setSortable(6, false); 

        // ✨ LÀM ĐẸP HEADER (Giống y hệt pnlHeaderRow của Lịch sử giảm giá)
        JTableHeader header = tableSP.getTableHeader();
        header.setPreferredSize(new Dimension(0, 45));
        header.setBorder(BorderFactory.createEmptyBorder());
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = new JLabel(value.toString(), SwingConstants.CENTER);
                lbl.setFont(FONT_BOLD.deriveFont(13f));
                lbl.setForeground(TEXT_SUB);
                lbl.setBackground(BG_MAIN);
                lbl.setOpaque(true);
                // Canh lề chữ header cho hợp lý
                if(column == 1) lbl.setHorizontalAlignment(SwingConstants.LEFT); 
                return lbl;
            }
        });

        tableSP.getColumnModel().getColumn(0).setPreferredWidth(80);
        tableSP.getColumnModel().getColumn(1).setPreferredWidth(220);
        tableSP.getColumnModel().getColumn(2).setPreferredWidth(90);
        tableSP.getColumnModel().getColumn(3).setPreferredWidth(80);
        tableSP.getColumnModel().getColumn(4).setPreferredWidth(120); 
        tableSP.getColumnModel().getColumn(5).setPreferredWidth(130); 
        tableSP.getColumnModel().getColumn(6).setPreferredWidth(110); 

        setupTableRenderers();

        JScrollPane scroll = new JScrollPane(tableSP);
        TienIchGiaoDien.thietLapThanhCuon(scroll); // Thanh cuộn xịn cũ giữ nguyên
        scroll.getViewport().setBackground(BG_MAIN);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel pnlWrapper = new JPanel(new BorderLayout());
        pnlWrapper.setBackground(BG_MAIN);
        pnlWrapper.setBorder(new EmptyBorder(0, 20, 20, 20));
        pnlWrapper.add(pnlTopBar, BorderLayout.NORTH);
        pnlWrapper.add(scroll, BorderLayout.CENTER);

        return pnlWrapper;
    }

    // ✨ HÀM PHỤ TRỢ XÁC ĐỊNH MÀU NỀN CỦA "CARD"
    private Color getRowBackgroundColor(int row, boolean isSelected) {
        if (isSelected || row == hoveredRow) return ACCENT_HOVER;
        return COLOR_WHITE;
    }

    private void setupTableRenderers() {
        // --- 1. RENDERER CHUNG CỦA CÁC CỘT TEXT ---
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                setBackground(getRowBackgroundColor(row, isSelected));
                setFont(FONT_REGULAR);
                setForeground(TEXT_MAIN);
                return c;
            }
        };

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(LEFT);
                setBackground(getRowBackgroundColor(row, isSelected));
                setFont(FONT_BOLD); // Tên SP in đậm đẹp hơn
                setForeground(TEXT_MAIN);
                return c;
            }
        };

        tableSP.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
        tableSP.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tableSP.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // --- 2. RENDERER HÌNH ẢNH ---
        tableSP.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setText("");
                lbl.setHorizontalAlignment(CENTER);
                if (value instanceof ImageIcon) lbl.setIcon((ImageIcon) value);
                lbl.setBackground(getRowBackgroundColor(row, isSelected));
                return lbl;
            }
        });

        // --- 3. RENDER CỘT GIÁ BÁN ---
        tableSP.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                lbl.setFont(FONT_BOLD);
                lbl.setBackground(getRowBackgroundColor(row, isSelected));

                if (value instanceof TheSanPham) {
                    TheSanPham wrapper = (TheSanPham) value;
                    if (wrapper.phanTramGiam > 0) {
                        String htmlGia = "<html><div style='text-align: center;'>" 
                            + "<div style='color:#ef4444;'>" + GUI.HoTro.DinhDangUtil.dinhDangTien(wrapper.giaThucTe) + "</div>" // Màu đỏ nổi bật
                            + "<div style='color:#94a3b8; font-size:10px; font-weight:normal; text-decoration:line-through;'>" + GUI.HoTro.DinhDangUtil.dinhDangTien(wrapper.sp.getGiaBan()) + "</div>"
                            + "</div></html>";
                        lbl.setText(htmlGia);
                    } else {
                        lbl.setText(GUI.HoTro.DinhDangUtil.dinhDangTien(wrapper.sp.getGiaBan()));
                        lbl.setForeground(new Color(239, 68, 68)); // Màu đỏ (giống LichSuGiamGia)
                    }
                }
                return lbl;
            }
        });

        // --- 4. RENDER CỘT TRẠNG THÁI (UI PASTEL CỰC XỊN) ---
        tableSP.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
                pnl.setOpaque(true);
                pnl.setBackground(getRowBackgroundColor(row, isSelected));

                String str = String.valueOf(value);
                JLabel badge = new JLabel(str, SwingConstants.CENTER);
                badge.setFont(FONT_BOLD.deriveFont(12f));
                badge.setPreferredSize(new Dimension(110, 26));

                // Vẽ badge bo góc tay như bên Lịch sử giảm giá
                JPanel pnlBadge = new JPanel(new BorderLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(getBackground());
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                pnlBadge.setOpaque(false);
                pnlBadge.setPreferredSize(new Dimension(110, 26));
                pnlBadge.add(badge, BorderLayout.CENTER);

                // Gắn hệ màu pastel
                if (str.equals("Hết Tồn Kho")) {
                    pnlBadge.setBackground(new Color(241, 245, 249)); // ST_ENDED_BG
                    badge.setForeground(new Color(100, 116, 139));    // ST_ENDED_FG
                } else if (str.equals("Đang giao dịch")) {
                    pnlBadge.setBackground(new Color(220, 252, 231)); // ST_ACTIVE_BG
                    badge.setForeground(new Color(22, 163, 74));      // ST_ACTIVE_FG
                } else {
                    pnlBadge.setBackground(new Color(254, 243, 199)); // ST_PENDING_BG
                    badge.setForeground(new Color(217, 119, 6));      // ST_PENDING_FG
                }

                pnl.add(pnlBadge);
                return pnl;
            }
        });

        // --- 5. RENDER CỘT HÀNH ĐỘNG (NÚT BẤM) ---
        tableSP.getColumnModel().getColumn(6).setCellRenderer(new ButtonActionRenderer());
        tableSP.getColumnModel().getColumn(6).setCellEditor(new ButtonActionEditor(new JCheckBox()));
    }

    // =======================================================
    // 🔥 LOGIC LOAD DỮ LIỆU GIỮ NGUYÊN HOÀN TOÀN
    // =======================================================
    public void taiDuLieuBanHangSieuToc(String maLoai) {
        tableModel.setRowCount(0);
        mapSanPham.clear(); 
        dataBanHangCache = TruyVanSieuTocDAO.getInstance().loadToanBoSanPhamBanHang();
        
        if (dataBanHangCache == null || dataBanHangCache.dsSanPham == null) return;

        List<SanPham> dsLoc = new ArrayList<>();
        for (SanPham sp : dataBanHangCache.dsSanPham) {
            if (maLoai.equals("ALL") || sp.getMaLoai().equals(maLoai)) {
                dsLoc.add(sp);
            }
        }

        dsLoc.sort((a, b) -> {
            int tonA = dataBanHangCache.mapTonKho.getOrDefault(a.getMaSP(), 0);
            int tonB = dataBanHangCache.mapTonKho.getOrDefault(b.getMaSP(), 0);
            int scoreA = tonA > 0 ? 0 : 1;
            int scoreB = tonB > 0 ? 0 : 1;
            return Integer.compare(scoreA, scoreB);
        });

        for (SanPham sp : dsLoc) {
            themDongVaoBang(sp);
        }
        
        tableSP.revalidate();
        tableSP.repaint();
    }

    public void loadDuLieuSanPham(String maLoai) {
        if (dataBanHangCache == null) return;
        mapSanPham.clear();
        tableModel.setRowCount(0);

        List<SanPham> dsLoc = new ArrayList<>();
        for (SanPham sp : dataBanHangCache.dsSanPham) {
            if (maLoai.equals("ALL") || sp.getMaLoai().equals(maLoai)) {
                dsLoc.add(sp);
            }
        }

        dsLoc.sort((a, b) -> {
            int tonA = dataBanHangCache.mapTonKho.getOrDefault(a.getMaSP(), 0);
            int tonB = dataBanHangCache.mapTonKho.getOrDefault(b.getMaSP(), 0);
            int scoreA = tonA > 0 ? 0 : 1;
            int scoreB = tonB > 0 ? 0 : 1;
            return Integer.compare(scoreA, scoreB);
        });

        for (SanPham sp : dsLoc) {
            themDongVaoBang(sp);
        }
    }

    private void thucHienTimKiem(String tuKhoa) {
        if (dataBanHangCache == null) return;
        mapSanPham.clear();
        tableModel.setRowCount(0);
        String tuKhoaThuong = DinhDangUtil.loaiBoDauTiengViet(tuKhoa.toLowerCase().trim());

        List<SanPham> dsLoc = new ArrayList<>();
        for (SanPham sp : dataBanHangCache.dsSanPham) {
            String tenSPThuong = DinhDangUtil.loaiBoDauTiengViet(sp.getTenSP().toLowerCase());
            if (tenSPThuong.contains(tuKhoaThuong)) {
                dsLoc.add(sp);
            }
        }

        dsLoc.sort((a, b) -> {
            int tonA = dataBanHangCache.mapTonKho.getOrDefault(a.getMaSP(), 0);
            int tonB = dataBanHangCache.mapTonKho.getOrDefault(b.getMaSP(), 0);
            int scoreA = tonA > 0 ? 0 : 1;
            int scoreB = tonB > 0 ? 0 : 1;
            return Integer.compare(scoreA, scoreB);
        });

        for (SanPham sp : dsLoc) {
            themDongVaoBang(sp);
        }
    }

    private void themDongVaoBang(SanPham sp) {
        int tonKho = dataBanHangCache.mapTonKho.getOrDefault(sp.getMaSP(), 0);
        int phanTram = dataBanHangCache.mapGiamGia != null ? dataBanHangCache.mapGiamGia.getOrDefault(sp.getMaSP(), 0) : 0;
        
        BigDecimal giaGoc = sp.getGiaBan();
        BigDecimal giaThucTe = giaGoc;
        if (phanTram > 0) {
            BigDecimal tienGiam = giaGoc.multiply(new BigDecimal(phanTram)).divide(new BigDecimal(100));
            giaThucTe = giaGoc.subtract(tienGiam);
        }

        TheSanPham wrapper = new TheSanPham(sp, tonKho, giaThucTe, phanTram);
        mapSanPham.put(sp.getMaSP(), wrapper);

        ImageIcon icon = QuanLyAnh.layIconAnh(sp.getLinkHinhAnh(), 40, 40);
        String trangThai = "Đang giao dịch";
        if (tonKho == 0) trangThai = "Hết Tồn Kho";

        Object[] rowData = {
                icon,
                sp.getTenSP(),
                sp.getMaLoai(),
                tonKho,
                wrapper, 
                trangThai,
                wrapper  
        };
        tableModel.addRow(rowData);
    }

    // ==========================================
    // LOGIC GIỎ HÀNG ẢO
    // ==========================================
    public TheSanPham getTheSanPham(String maSP) {
        return mapSanPham.get(maSP);
    }

    public Component[] layDanhSachTheSP() {
        return mapSanPham.values().toArray(new Component[0]);
    }

    public class TheSanPham extends JComponent {
        public SanPham sp;
        private int soLuongMua = 0;
        public int tonMax;
        public BigDecimal giaThucTe; 
        public int phanTramGiam;

        public TheSanPham(SanPham sp, int tonMax, BigDecimal giaThucTe, int phanTramGiam) {
            this.sp = sp;
            this.tonMax = tonMax;
            this.giaThucTe = giaThucTe;
            this.phanTramGiam = phanTramGiam;
        }

        public void thayDoiSoLuong(int delta) {
            if (soLuongMua + delta > tonMax) {
                TienIchGiaoDien.hienThiThongBao(DanhSachSPUi.this, "Vượt quá số lượng tồn kho khả dụng!", "WARNING");
                return;
            }
            soLuongMua += delta;
            if (soLuongMua <= 0) soLuongMua = 0;
            if (gioHangCallback != null) gioHangCallback.capNhatGioHang(sp, delta, this);
        }

        public void congTruTuGioHang(int delta) { thayDoiSoLuong(delta); }
        public void xoaKhoiGioHang() { thayDoiSoLuong(-soLuongMua); }
        public void resetTrangThai() { soLuongMua = 0; }
        public int getSoLuongMua() { return soLuongMua; } 
    }

    // ==========================================
    // 🔥 CỘT HÀNH ĐỘNG (Render & Editor đồng bộ màu Hover)
    // ==========================================
    class ButtonActionRenderer extends JPanel implements TableCellRenderer {
        private NutBoGoc btnAction;
        
        public ButtonActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 18)); // Căn giữa nút đẹp hơn với row height 68
            setOpaque(true);
            btnAction = new NutBoGoc(currentMode == UIMode.BAN_HANG ? "MUA" : "CHI TIẾT");
            btnAction.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnAction.setPreferredSize(new Dimension(85, 30)); // Thu nhỏ lại chút cho tinh tế
            add(btnAction);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(getRowBackgroundColor(row, isSelected));

            if (value instanceof TheSanPham) {
                TheSanPham wrapper = (TheSanPham) value;
                if (currentMode == UIMode.BAN_HANG) {
                    if (wrapper.tonMax == 0) {
                        btnAction.setColorBackground(new Color(203, 213, 225)); 
                        btnAction.setEnabled(false);
                    } else {
                        btnAction.setColorBackground(new Color(59, 130, 246)); // Màu ACCENT_BLUE 
                        btnAction.setEnabled(true);
                    }
                } else {
                    btnAction.setColorBackground(new Color(59, 130, 246));
                    btnAction.setEnabled(true);
                }
            }
            return this;
        }
    }

    class ButtonActionEditor extends DefaultCellEditor {
        private JPanel pnl;
        private NutBoGoc btnAction;
        private TheSanPham currentWrapper;

        public ButtonActionEditor(JCheckBox checkBox) {
            super(checkBox);
            pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 18));
            pnl.setOpaque(true);
            btnAction = new NutBoGoc(currentMode == UIMode.BAN_HANG ? "MUA" : "CHI TIẾT");
            btnAction.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnAction.setPreferredSize(new Dimension(85, 30));
            pnl.add(btnAction);

            btnAction.addActionListener(e -> {
                if (currentWrapper != null) {
                    if (currentMode == UIMode.BAN_HANG) {
                        if (currentWrapper.tonMax > 0) {
                            currentWrapper.thayDoiSoLuong(1);
                        }
                    } else {
                        ChiTietSanPham.showModal(DanhSachSPUi.this, currentWrapper.sp, currentWrapper.tonMax, () -> {
                            taiDuLieuBanHangSieuToc("ALL"); 
                            
                        });
                    }
                    fireEditingStopped();
                }
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            pnl.setBackground(ACCENT_HOVER); // Khi click Edit là chắc chắn đang focus
            if (value instanceof TheSanPham) {
                currentWrapper = (TheSanPham) value;
                if (currentMode == UIMode.BAN_HANG) {
                    if (currentWrapper.tonMax == 0) {
                        btnAction.setColorBackground(new Color(203, 213, 225));
                        btnAction.setEnabled(false);
                    } else {
                        btnAction.setColorBackground(new Color(59, 130, 246));
                        btnAction.setEnabled(true);
                    }
                } else {
                    btnAction.setColorBackground(new Color(59, 130, 246));
                    btnAction.setEnabled(true);
                }
            }
            return pnl;
        }
        @Override
        public Object getCellEditorValue() { return currentWrapper; }
    }
}