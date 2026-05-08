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

    // 🔥 Cache dữ liệu siêu tốc tại chỗ (Lấy từ TruyVanSieuTocDAO)
    private TruyVanSieuTocDAO.DuLieuBanHangDTO dataBanHangCache;

    // Quản lý map các thẻ sản phẩm ảo để tương thích ngược với logic giỏ hàng cũ
    private Map<String, TheSanPham> mapSanPham = new HashMap<>();

    public interface CallBackGioHang {
        void capNhatGioHang(SanPham sp, int soLuongThayDoi, TheSanPham card);
    }

    public DanhSachSPUi(CallBackGioHang callback) {
        this.gioHangCallback = callback;
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
        JPanel pnlTopBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
        pnlTopBar.setBackground(BG_MAIN);

        txtTimKiem = new TheBongDo.RoundedTextField("Tìm kiếm sản phẩm theo tên...", 30);
        txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtTimKiem.setPreferredSize(new Dimension(600, 45));
        txtTimKiem.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                thucHienTimKiem(txtTimKiem.getText());
            }
        });
        pnlTopBar.add(txtTimKiem);

        String[] columns = {"Hình ảnh", "Tên sản phẩm", "Loại", "Tồn kho", "Trạng thái", "Hành động"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; 
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
        sorter.setSortable(5, false); 

        JTableHeader header = tableSP.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(226, 232, 240));
        header.setForeground(new Color(30, 41, 59));
        header.setPreferredSize(new Dimension(0, 40));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        tableSP.getColumnModel().getColumn(0).setPreferredWidth(80);
        tableSP.getColumnModel().getColumn(1).setPreferredWidth(250);
        tableSP.getColumnModel().getColumn(2).setPreferredWidth(120);
        tableSP.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableSP.getColumnModel().getColumn(4).setPreferredWidth(140);
        tableSP.getColumnModel().getColumn(5).setPreferredWidth(120);

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

        tableSP.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
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

        tableSP.getColumnModel().getColumn(5).setCellRenderer(new ButtonActionRenderer());
        tableSP.getColumnModel().getColumn(5).setCellEditor(new ButtonActionEditor(new JCheckBox()));
    }

    // =======================================================
    // 🔥 LOAD DỮ LIỆU SIÊU TỐC TỪ ĐỘNG CƠ TURBO
    // =======================================================
    public void taiDuLieuBanHangSieuToc(String maLoai) {
        if (txtTimKiem != null) txtTimKiem.setText("");
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Chỉ đi qua DB 1 LẦN DUY NHẤT để lấy trọn bộ Sản Phẩm + Tồn Kho[cite: 25]
                dataBanHangCache = TruyVanSieuTocDAO.getInstance().loadToanBoSanPhamBanHang();
                return null;
            }

            @Override
            protected void done() {
                loadDuLieuSanPham(maLoai);
            }
        };
        worker.execute();
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
        
        TheSanPham wrapper = new TheSanPham(sp, tonKho);
        mapSanPham.put(sp.getMaSP(), wrapper);

        ImageIcon icon = QuanLyAnh.layIconAnh(sp.getLinkHinhAnh(), 40, 40);

        String trangThai = "Đang giao dịch";
        if (tonKho == 0) trangThai = "Hết Tồn Kho";
        // Bổ sung logic Hết HSD tại đây nếu cần thiết trong tương lai

        Object[] rowData = {
                icon,
                sp.getTenSP(),
                sp.getMaLoai(),
                tonKho,
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

        public TheSanPham(SanPham sp, int tonMax) {
            this.sp = sp;
            this.tonMax = tonMax;
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
    }

    // ==========================================
    // NÚT MUA TRONG BẢNG
    // ==========================================
    class ButtonActionRenderer extends JPanel implements TableCellRenderer {
        private NutBoGoc btnMua;
        
        public ButtonActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 15));
            setOpaque(true);
            btnMua = new NutBoGoc("MUA");
            btnMua.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnMua.setPreferredSize(new Dimension(80, 30));
            add(btnMua);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (!isSelected) setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            else setBackground(new Color(224, 242, 254));

            if (value instanceof TheSanPham) {
                TheSanPham wrapper = (TheSanPham) value;
                if (wrapper.tonMax == 0) {
                    btnMua.setColorBackground(new Color(203, 213, 225));
                    btnMua.setEnabled(false);
                } else {
                    btnMua.setColorBackground(new Color(34, 197, 94));
                    btnMua.setEnabled(true);
                }
            }
            return this;
        }
    }

    class ButtonActionEditor extends DefaultCellEditor {
        private JPanel pnl;
        private NutBoGoc btnMua;
        private TheSanPham currentWrapper;

        public ButtonActionEditor(JCheckBox checkBox) {
            super(checkBox);
            pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
            pnl.setOpaque(true);
            btnMua = new NutBoGoc("MUA");
            btnMua.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnMua.setPreferredSize(new Dimension(80, 30));
            pnl.add(btnMua);

            btnMua.addActionListener(e -> {
                if (currentWrapper != null && currentWrapper.tonMax > 0) {
                    currentWrapper.thayDoiSoLuong(1);
                    fireEditingStopped();
                }
            });
        }
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            pnl.setBackground(table.getSelectionBackground());
            if (value instanceof TheSanPham) {
                currentWrapper = (TheSanPham) value;
                if (currentWrapper.tonMax == 0) {
                    btnMua.setColorBackground(new Color(203, 213, 225));
                    btnMua.setEnabled(false);
                } else {
                    btnMua.setColorBackground(new Color(34, 197, 94));
                    btnMua.setEnabled(true);
                }
            }
            return pnl;
        }
        @Override
        public Object getCellEditorValue() { return currentWrapper; }
    }
}