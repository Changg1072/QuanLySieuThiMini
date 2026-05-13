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

    public static final Color BG_MAIN = new Color(248, 250, 252);
    private CallBackGioHang gioHangCallback;
    private TheBongDo.RoundedTextField txtTimKiem;

    private JTable tableSP;
    private DefaultTableModel tableModel;

    // 🔥 THÊM CỜ (MODE) ĐỂ PHÂN BIỆT GIAO DIỆN
    public enum UIMode {
        BAN_HANG,   // Hiện nút MUA
        QUAN_LY     // Hiện nút CHI TIẾT
    }
    private UIMode currentMode;
    public DanhSachSPUi(CallBackGioHang callback) {
        // Gọi ngầm định sang constructor mới với chế độ BAN_HANG
        this(callback, UIMode.BAN_HANG); 
    }   
    // 🔥 Cache dữ liệu siêu tốc tại chỗ (Lấy từ TruyVanSieuTocDAO)
    private TruyVanSieuTocDAO.DuLieuBanHangDTO dataBanHangCache;

    // Quản lý map các thẻ sản phẩm ảo để tương thích ngược với logic giỏ hàng cũ
    private Map<String, TheSanPham> mapSanPham = new HashMap<>();

    public interface CallBackGioHang {
        void capNhatGioHang(SanPham sp, int soLuongThayDoi, TheSanPham card);
    }

    // 🔥 Sửa Constructor nhận thêm tham số UIMode
    public DanhSachSPUi(CallBackGioHang callback, UIMode mode) {
        this.gioHangCallback = callback;
        this.currentMode = (mode != null) ? mode : UIMode.BAN_HANG; // Mặc định là bán hàng nếu không truyền
        
        setLayout(new BorderLayout());
        setBackground(BG_MAIN);

        add(taoMainContent(), BorderLayout.CENTER);

        // Gọi động cơ Turbo tải dữ liệu ngầm ngay khi khởi tạo
        taiDuLieuBanHangSieuToc("ALL");

        // Clear focus khi click ra ngoài
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
        JPanel pnlTopBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        pnlTopBar.setBackground(BG_MAIN);

        // --- FIX THANH TÌM KIẾM: LIỀN KHỐI & BỎ VIỀN LỖI ---
        JPanel pnlSearchWrapper = new JPanel(new BorderLayout(10, 0));
        pnlSearchWrapper.setBackground(Color.WHITE);
        pnlSearchWrapper.setPreferredSize(new Dimension(650, 45));
        
        // 1. Chỉ dùng 1 viền bo góc duy nhất ở lớp ngoài cùng
        pnlSearchWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
            new EmptyBorder(0, 15, 0, 15)
        ));

        // 2. Fix lỗi icon ô vuông (Dùng chữ thường hoặc icon an toàn)
        JLabel lblIcon = new JLabel("TÌM KIẾM"); 
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblIcon.setForeground(new Color(148, 163, 184)); 
        // (Nếu bạn có file ảnh kính lúp, có thể thay bằng: new JLabel(new ImageIcon("duong-dan/kinhlup.png")))

        // 3. Dùng JTextField nguyên thủy, tắt viền, tắt nền để nó "hòa tan" vào Wrapper
        JTextField txtTimKiem = new JTextField("Nhập tên hoặc mã sản phẩm...");
        txtTimKiem.setBorder(null);
        txtTimKiem.setBackground(Color.WHITE);
        txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtTimKiem.setForeground(new Color(148, 163, 184)); // Màu chữ mờ cho Placeholder
        
        // 4. Sự kiện giả làm Placeholder mượt mà
        txtTimKiem.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtTimKiem.getText().equals("Nhập tên hoặc mã sản phẩm...")) {
                    txtTimKiem.setText("");
                    txtTimKiem.setForeground(new Color(30, 41, 59)); // Chữ đậm lên khi gõ
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (txtTimKiem.getText().isEmpty()) {
                    txtTimKiem.setText("Nhập tên hoặc mã sản phẩm...");
                    txtTimKiem.setForeground(new Color(148, 163, 184)); // Chữ mờ đi khi bỏ chuột
                }
            }
        });

        // 5. Sự kiện gõ tới đâu lọc tới đó
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

        // --- Khúc dưới giữ nguyên ---
        String[] columns = {"Hình ảnh", "Tên sản phẩm", "Loại", "Tồn kho", "Giá bán", "Trạng thái", "Hành động"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; 
            }
        };

        tableSP = new JTable(tableModel);
        tableSP.setRowHeight(60);
        tableSP.setFillsViewportHeight(true);
        tableSP.setShowVerticalLines(false);
        tableSP.setIntercellSpacing(new Dimension(0, 0));
        tableSP.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        tableSP.setRowSorter(sorter);
        sorter.setSortable(0, false); 
        sorter.setSortable(6, false); // Cập nhật lại cột không sort

        JTableHeader header = tableSP.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(226, 232, 240));
        header.setForeground(new Color(30, 41, 59));
        header.setPreferredSize(new Dimension(0, 40));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        // --- Cập nhật lại độ rộng các cột cho vừa vặn ---
        tableSP.getColumnModel().getColumn(0).setPreferredWidth(80);
        tableSP.getColumnModel().getColumn(1).setPreferredWidth(200);
        tableSP.getColumnModel().getColumn(2).setPreferredWidth(100);
        tableSP.getColumnModel().getColumn(3).setPreferredWidth(80);
        tableSP.getColumnModel().getColumn(4).setPreferredWidth(110); // Cột Giá Bán mới
        tableSP.getColumnModel().getColumn(5).setPreferredWidth(120); // Trạng thái
        tableSP.getColumnModel().getColumn(6).setPreferredWidth(130); // Hành động

        setupTableRenderers();

        JScrollPane scroll = new JScrollPane(tableSP);
        TienIchGiaoDien.thietLapThanhCuon(scroll);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel pnlWrapper = new JPanel(new BorderLayout());
        pnlWrapper.setBackground(BG_MAIN);
        pnlWrapper.setBorder(new EmptyBorder(0, 20, 20, 20));
        pnlWrapper.add(pnlTopBar, BorderLayout.NORTH);
        pnlWrapper.add(scroll, BorderLayout.CENTER);

        return pnlWrapper;
    }

    private void setupTableRenderers() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                else c.setBackground(new Color(224, 242, 254));
                setFont(new Font("Segoe UI", Font.PLAIN, 14));
                return c;
            }
        };

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(LEFT);
                if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                else c.setBackground(new Color(224, 242, 254));
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                setForeground(new Color(15, 23, 42));
                return c;
            }
        };

        tableSP.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
        tableSP.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tableSP.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        tableSP.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setText("");
                lbl.setHorizontalAlignment(CENTER);
                if (value instanceof ImageIcon) lbl.setIcon((ImageIcon) value);
                if (!isSelected) lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                else lbl.setBackground(new Color(224, 242, 254));
                return lbl;
            }
        });

        // ... (phần render cột 0, 1, 2, 3 giữ nguyên) ...

        // --- 🚀 RENDER CỘT 4: GIÁ BÁN (HIỂN THỊ HTML GẠCH NGANG) ---
        tableSP.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                
                // 1. CHỐT CỨNG FONT SIZE 14 CHO TOÀN BỘ CỘT NÀY
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));

                if (!isSelected) lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                else lbl.setBackground(new Color(224, 242, 254));

                if (value instanceof TheSanPham) {
                    TheSanPham wrapper = (TheSanPham) value;
                    if (wrapper.phanTramGiam > 0) {
                        // 2. Bỏ font-size ở giá mới (để nó tự kế thừa font size 14 ở trên)
                        // Thêm font-weight:normal cho giá cũ để nó bớt đậm, nhìn tinh tế hơn
                        String htmlGia = "<html><div style='text-align: center;'>" 
                            + "<div style='color:#3b82f6;'>" + GUI.HoTro.DinhDangUtil.dinhDangTien(wrapper.giaThucTe) + "</div>"
                            + "<div style='color:#94a3b8; font-size:10px; font-weight:normal; text-decoration:line-through;'>" + GUI.HoTro.DinhDangUtil.dinhDangTien(wrapper.sp.getGiaBan()) + "</div>"
                            + "</div></html>";
                        lbl.setText(htmlGia);
                    } else {
                        // Không giảm giá thì in màu đen bình thường
                        lbl.setText(GUI.HoTro.DinhDangUtil.dinhDangTien(wrapper.sp.getGiaBan()));
                        lbl.setForeground(new Color(15, 23, 42));
                    }
                }
                return lbl;
            }
        });

        // --- RENDER CỘT 5: TRẠNG THÁI ---
        tableSP.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
                pnl.setOpaque(true);
                if (!isSelected) pnl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                else pnl.setBackground(new Color(224, 242, 254));

                String str = String.valueOf(value);
                JLabel badge = new JLabel(str);
                badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
                badge.setForeground(Color.WHITE);
                badge.setOpaque(true);
                badge.setBorder(new EmptyBorder(5, 12, 5, 12));

                if (str.equals("Hết HSD")) badge.setBackground(new Color(239, 68, 68)); 
                else if (str.equals("Hết Tồn Kho")) badge.setBackground(new Color(156, 163, 175)); 
                else badge.setBackground(new Color(245, 158, 11)); 

                pnl.add(badge);
                return pnl;
            }
        });

        // --- RENDER CỘT 6: HÀNH ĐỘNG (NÚT MUA) ---
        tableSP.getColumnModel().getColumn(6).setCellRenderer(new ButtonActionRenderer());
        tableSP.getColumnModel().getColumn(6).setCellEditor(new ButtonActionEditor(new JCheckBox()));
    }
    // =======================================================
    // 🔥 LOAD DỮ LIỆU SIÊU TỐC TỪ ĐỘNG CƠ TURBO
    // =======================================================
    public void taiDuLieuBanHangSieuToc(String maLoai) {
        tableModel.setRowCount(0);
        mapSanPham.clear(); // Xóa sạch mapping cũ trước khi tải mới

        // 1. Tải dữ liệu siêu tốc và NẠP VÀO CACHE để thanh tìm kiếm sử dụng
        dataBanHangCache = TruyVanSieuTocDAO.getInstance().loadToanBoSanPhamBanHang();
        
        if (dataBanHangCache == null || dataBanHangCache.dsSanPham == null) return;

        // 2. Lọc danh sách theo mã loại (Nếu là "ALL" thì lấy toàn bộ)
        List<SanPham> dsLoc = new ArrayList<>();
        for (SanPham sp : dataBanHangCache.dsSanPham) {
            if (maLoai.equals("ALL") || sp.getMaLoai().equals(maLoai)) {
                dsLoc.add(sp);
            }
        }

        // 3. Sắp xếp thông minh: Sản phẩm còn tồn kho đẩy lên trên, hết hàng đẩy xuống cuối
        dsLoc.sort((a, b) -> {
            int tonA = dataBanHangCache.mapTonKho.getOrDefault(a.getMaSP(), 0);
            int tonB = dataBanHangCache.mapTonKho.getOrDefault(b.getMaSP(), 0);
            int scoreA = tonA > 0 ? 0 : 1;
            int scoreB = tonB > 0 ? 0 : 1;
            return Integer.compare(scoreA, scoreB);
        });

        // 4. Dùng hàm themDongVaoBang để đẩy ĐẦY ĐỦ 7 CỘT dữ liệu vào giao diện
        for (SanPham sp : dsLoc) {
            themDongVaoBang(sp);
        }
        
        // Yêu cầu bảng vẽ lại
        tableSP.revalidate();
        tableSP.repaint();
    }

    // Đẩy data từ Cache (RAM) lên bảng UI
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

        // Sắp xếp: Còn hàng lên trên, hết hàng xuống dưới
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
        
        // --- 🚀 LẤY DỮ LIỆU GIẢM GIÁ TỪ CACHE SIÊU TỐC ---
        int phanTram = dataBanHangCache.mapGiamGia != null ? dataBanHangCache.mapGiamGia.getOrDefault(sp.getMaSP(), 0) : 0;
        
        BigDecimal giaGoc = sp.getGiaBan();
        BigDecimal giaThucTe = giaGoc;
        if (phanTram > 0) {
            BigDecimal tienGiam = giaGoc.multiply(new BigDecimal(phanTram)).divide(new BigDecimal(100));
            giaThucTe = giaGoc.subtract(tienGiam);
        }

        // Tạo wrapper chứa thông tin giá để đưa vào Bảng
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
                wrapper, // Truyền wrapper vào cột thứ 4 (Giá bán) để Renderer tự phân tích vẽ HTML
                trangThai,
                wrapper  // Truyền wrapper vào cột thứ 6 (Nút hành động)
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
        
        // --- NÂNG CẤP: LƯU TRỮ GIÁ THỰC TẾ VÀ % GIẢM ĐỂ VẼ LÊN UI ---
        public BigDecimal giaThucTe; 
        public int phanTramGiam;

        // --- Cập nhật Constructor nhận thêm Giá Thực Tế và % Giảm ---
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
        public int getSoLuongMua() { return soLuongMua; } // Hàm phụ trợ lấy số lượng
    }

    // ==========================================
    // 🔥 CỘT HÀNH ĐỘNG (THAY ĐỔI THEO MODE)
    // ==========================================
    class ButtonActionRenderer extends JPanel implements TableCellRenderer {
        private NutBoGoc btnAction;
        
        public ButtonActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 15));
            setOpaque(true);
            
            // Nếu Mode BÁN HÀNG -> Nút MUA. Ngược lại -> Nút CHI TIẾT
            btnAction = new NutBoGoc(currentMode == UIMode.BAN_HANG ? "MUA" : "CHI TIẾT");
            btnAction.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnAction.setPreferredSize(new Dimension(110, 32));
            add(btnAction);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (!isSelected) setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            else setBackground(new Color(224, 242, 254));

            if (value instanceof TheSanPham) {
                TheSanPham wrapper = (TheSanPham) value;
                if (currentMode == UIMode.BAN_HANG) {
                    if (wrapper.tonMax == 0) {
                        btnAction.setColorBackground(new Color(203, 213, 225)); // Xám khóa
                        btnAction.setEnabled(false);
                    } else {
                        btnAction.setColorBackground(new Color(34, 197, 94)); // Xanh lá
                        btnAction.setEnabled(true);
                    }
                } else {
                    // Mode Quản Lý - Luôn hiển thị màu Xanh mint premium
                    btnAction.setColorBackground(new Color(14, 165, 233));
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
            pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
            pnl.setOpaque(true);
            btnAction = new NutBoGoc(currentMode == UIMode.BAN_HANG ? "MUA" : "CHI TIẾT");
            btnAction.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnAction.setPreferredSize(new Dimension(110, 32));
            pnl.add(btnAction);

            btnAction.addActionListener(e -> {
                if (currentWrapper != null) {
                    if (currentMode == UIMode.BAN_HANG) {
                        if (currentWrapper.tonMax > 0) {
                            currentWrapper.thayDoiSoLuong(1);
                        }
                    } else {
                        // GỌI COMPONENT CHI TIẾT SẢN PHẨM Ở ĐÂY
                        ChiTietSanPham.showModal(DanhSachSPUi.this, currentWrapper.sp, currentWrapper.tonMax);
                    }
                    fireEditingStopped();
                }
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            pnl.setBackground(table.getSelectionBackground());
            if (value instanceof TheSanPham) {
                currentWrapper = (TheSanPham) value;
                if (currentMode == UIMode.BAN_HANG) {
                    if (currentWrapper.tonMax == 0) {
                        btnAction.setColorBackground(new Color(203, 213, 225));
                        btnAction.setEnabled(false);
                    } else {
                        btnAction.setColorBackground(new Color(34, 197, 94));
                        btnAction.setEnabled(true);
                    }
                } else {
                    btnAction.setColorBackground(new Color(14, 165, 233));
                    btnAction.setEnabled(true);
                }
            }
            return pnl;
        }
        @Override
        public Object getCellEditorValue() { return currentWrapper; }
    }
}