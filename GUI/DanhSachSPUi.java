package GUI;

import Dao.TruyVanSieuTocDAO;
import GUI.HoTro.*;
import Data.SanPham;
import Logic.QuanLyAnh;
import Dao.LoaiSPDAO;
import Data.LoaiSP;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DanhSachSPUi extends JPanel {

    // ==========================================
    // 🎨 HỆ MÀU UI MỚI
    // ==========================================
    public static final Color BG_MAIN = new Color(241, 245, 249);
    private final Color COLOR_WHITE = new Color(255, 255, 255);
    private final Color TEXT_MAIN = new Color(15, 23, 42);      
    private final Color TEXT_SUB = new Color(100, 116, 139);    
    private final Color ACCENT_HOVER = new Color(239, 246, 255); 
    private final Color BORDER_COLOR = new Color(226, 232, 240);

    private CallBackGioHang gioHangCallback;
    private TheBongDo.RoundedTextField txtTimKiem;

    private JPanel pnlRowListContainer;
    private JPanel pnlHeaderContainer; // Chứa Header để vẽ lại khi thu gọn

    // 🔥 CỜ BẬT/TẮT THU GỌN GIAO DIỆN KHI MỞ GIỎ HÀNG
    private boolean isThuGon = false;

    public enum UIMode {
        BAN_HANG,   
        QUAN_LY     
    }
    private UIMode currentMode;
    
    private TruyVanSieuTocDAO.DuLieuBanHangDTO dataBanHangCache;
    private Map<String, TheSanPham> mapSanPham = new HashMap<>();
    private Map<String, String> mapTenLoai = new HashMap<>();

    public interface CallBackGioHang {
        void capNhatGioHang(SanPham sp, int soLuongThayDoi, TheSanPham card);
    }

    public DanhSachSPUi(CallBackGioHang callback) {
        this(callback, UIMode.BAN_HANG); 
    }   

    public DanhSachSPUi(CallBackGioHang callback, UIMode mode) {
        this.gioHangCallback = callback;
        this.currentMode = (mode != null) ? mode : UIMode.BAN_HANG;
        
        setLayout(new BorderLayout());
        setBackground(BG_MAIN);

        for (LoaiSP loai : LoaiSPDAO.getInstance().layDanhSachLoaiSP()) {
            mapTenLoai.put(loai.getMaLoai(), loai.getTenLoai());
        }

        initUI();
        taiDuLieuBanHangSieuToc("ALL");

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                Component comp = (Component) event.getSource();
                if (txtTimKiem != null && comp != txtTimKiem && !SwingUtilities.isDescendingFrom(comp, txtTimKiem)) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    // =======================================================
    // 🛠️ HÀM CÔNG TẮC: BẬT/TẮT THU GỌN BẢNG
    // =======================================================
    public void setGiaoDienThuGon(boolean thuGon) {
        if (this.isThuGon != thuGon) {
            this.isThuGon = thuGon;
            veLaiHeader();                  // Ép Header đổi kích thước
            taiDuLieuBanHangSieuToc("ALL"); // Ép các dòng dữ liệu đổi kích thước theo
        }
    }

    private void initUI() {
        // Đổi sang BorderLayout để đẩy 2 khối ra 2 góc (Trái - Phải)
        JPanel pnlTopBar = new JPanel(new BorderLayout());
        pnlTopBar.setBackground(BG_MAIN);
        pnlTopBar.setBorder(new EmptyBorder(10, 0, 15, 0)); // Tạo khoảng cách với bảng bên dưới

        // =====================================
        // 1. Ô TÌM KIẾM (GÓC TRÁI)
        // =====================================
        txtTimKiem = new GUI.HoTro.TheBongDo.RoundedTextField("\uD83D\uDD0D Tìm tên hoặc mã sản phẩm...", 0);
        txtTimKiem.setPreferredSize(new Dimension(400, 45)); 
        txtTimKiem.setFont(GUI.HoTro.TienIchGiaoDien.FONT_DAM.deriveFont(16f));
        
        txtTimKiem.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { thucHienTimKiem(txtTimKiem.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { thucHienTimKiem(txtTimKiem.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { thucHienTimKiem(txtTimKiem.getText()); }
        });

        // Bọc vòng kim cô chống dãn
        JPanel pnlSearchWrap = new JPanel(new BorderLayout());
        pnlSearchWrap.setPreferredSize(new Dimension(400, 45));
        pnlSearchWrap.setOpaque(false);
        pnlSearchWrap.add(txtTimKiem, BorderLayout.CENTER);

        JPanel pnlSearchAlignLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlSearchAlignLeft.setOpaque(false);
        pnlSearchAlignLeft.add(pnlSearchWrap);

        pnlTopBar.add(pnlSearchAlignLeft, BorderLayout.WEST);

        // =====================================
        // 2. NÚT LÀM MỚI (GÓC PHẢI)
        // =====================================
        JButton btnRefresh = GUI.HoTro.TienIchGiaoDien.taoNutHienDai("Làm mới ↻", new Color(100, 116, 139));
        btnRefresh.setPreferredSize(new Dimension(120, 45));
        btnRefresh.addActionListener(e -> {
            txtTimKiem.setText(""); // Xóa text tìm kiếm
            taiDuLieuBanHangSieuToc("ALL"); // Tải lại toàn bộ dữ liệu
        });

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlRight.setOpaque(false);
        pnlRight.setBorder(new EmptyBorder(0, 0, 0, 40)); // 🔥 Đệm thêm 20px bên phải để đẩy nút sang trái
        pnlRight.add(btnRefresh);

        pnlTopBar.add(pnlRight, BorderLayout.EAST);

        JPanel pnlCenter = new JPanel(new BorderLayout(0, 10));
        pnlCenter.setOpaque(false);

        // Khởi tạo vùng chứa Header
        pnlHeaderContainer = new JPanel(new BorderLayout());
        pnlHeaderContainer.setOpaque(false);
        veLaiHeader(); // Chạy hàm vẽ Header
        pnlCenter.add(pnlHeaderContainer, BorderLayout.NORTH);

        pnlRowListContainer = new JPanel();
        pnlRowListContainer.setLayout(new BoxLayout(pnlRowListContainer, BoxLayout.Y_AXIS));
        pnlRowListContainer.setBackground(BG_MAIN);

        JScrollPane scrollPane = new JScrollPane(pnlRowListContainer);
        TienIchGiaoDien.thietLapThanhCuon(scrollPane);
        scrollPane.getViewport().setBackground(BG_MAIN);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        pnlCenter.add(scrollPane, BorderLayout.CENTER);

        JPanel pnlWrapper = new JPanel(new BorderLayout());
        pnlWrapper.setBackground(BG_MAIN);
        pnlWrapper.setBorder(new EmptyBorder(0, 20, 20, 20));
        pnlWrapper.add(pnlTopBar, BorderLayout.NORTH);
        pnlWrapper.add(pnlCenter, BorderLayout.CENTER);

        add(pnlWrapper, BorderLayout.CENTER);
    }

    // =======================================================
    // 🎨 HÀM VẼ LẠI HEADER (Bắt được chế độ isThuGon)
    // =======================================================
    private void veLaiHeader() {
        pnlHeaderContainer.removeAll();
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.setBackground(new Color(241, 245, 249)); // <-- Đổi thành mã màu xám này
        header.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 45));

        // 🔥 THÔNG SỐ TO/NHỎ Ở ĐÂY (Sếp tha hồ tự chỉnh)
        int wTen = isThuGon ? 320 : 600;
        int wLoai = isThuGon ? 200 : 280;
        int wTon = isThuGon ? 100 : 110;
        int wGia = isThuGon ? 130 : 160;
        int wTrangThai = isThuGon ? 150 : 160;

        header.add(createHeaderLabel("Hình Ảnh", 80));
        header.add(createHeaderLabel("Tên Sản Phẩm", wTen));
        header.add(createHeaderLabel("Loại", wLoai));
        header.add(createHeaderLabel("Tồn Kho", wTon));
        header.add(createHeaderLabel("Giá Bán", wGia));
        header.add(createHeaderLabel("Trạng Thái", wTrangThai));
        String tenCotCuoi = (currentMode == UIMode.BAN_HANG) ? "Hành Động" : "";
        header.add(createHeaderLabel(tenCotCuoi, 120));

        pnlHeaderContainer.add(header, BorderLayout.CENTER);
        pnlHeaderContainer.revalidate();
        pnlHeaderContainer.repaint();
    }

    private JLabel createHeaderLabel(String text, int width) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f)); 
        lbl.setForeground(TEXT_MAIN);
        lbl.setPreferredSize(new Dimension(width, 25));
        return lbl;
    }

    private JLabel createCell(String text, int width, boolean isBold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(isBold ? TienIchGiaoDien.FONT_DAM.deriveFont(15f) : TienIchGiaoDien.FONT_CHINH.deriveFont(15f));
        lbl.setForeground(isBold ? TEXT_MAIN : TEXT_SUB);
        lbl.setPreferredSize(new Dimension(width, 25));
        return lbl;
    }

    private JLabel createBadge(String text) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); 
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setBorder(new EmptyBorder(6, 12, 6, 12)); 

        if (text.equals("Hết Tồn Kho")) {
            badge.setBackground(new Color(241, 245, 249)); 
            badge.setForeground(new Color(100, 116, 139)); 
        } else {
            badge.setBackground(new Color(220, 252, 231)); 
            badge.setForeground(new Color(22, 163, 74)); 
        }
        return badge;
    }

    // =======================================================
    // TẠO ROW PANEL CHO MỖI SẢN PHẨM
    // =======================================================
    private JPanel createRowPanel(SanPham sp, TheSanPham wrapper) {
        JPanel row = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 75)); 
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        content.setOpaque(false);

        // 🔥 THÔNG SỐ TO/NHỎ Ở ĐÂY
        int wTen = isThuGon ? 320 : 600;
        int wLoai = isThuGon ? 200 : 280;
        int wTon = isThuGon ? 100 : 110;
        int wGia = isThuGon ? 130 : 160;
        int wTrangThai = isThuGon ? 150 : 160;

        // 1. Hình ảnh
        JLabel lblImg = new JLabel();
        lblImg.setPreferredSize(new Dimension(50, 50));
        ImageIcon icon = QuanLyAnh.layIconAnh(sp.getLinkHinhAnh(), 50, 50);
        if (icon != null) lblImg.setIcon(icon);
        else lblImg.setText("Ảnh");
        lblImg.setHorizontalAlignment(SwingConstants.CENTER);
        lblImg.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        JPanel pnlImg = new JPanel(new BorderLayout());
        pnlImg.setOpaque(false);
        pnlImg.setPreferredSize(new Dimension(80, 50));
        pnlImg.add(lblImg, BorderLayout.CENTER);
        content.add(pnlImg);

        // 2. Tên Sản Phẩm, Loại, Tồn Kho
        content.add(createCell(sp.getTenSP(), wTen, true));
        String tenLoai = mapTenLoai.getOrDefault(sp.getMaLoai(), sp.getMaLoai()); 
        content.add(createCell(tenLoai, wLoai, false));
        
        JLabel lblTon = createCell(String.valueOf(wrapper.tonMax), wTon, true);
        if (wrapper.tonMax == 0) lblTon.setForeground(new Color(239, 68, 68)); 
        content.add(lblTon);

        // 3. Giá Bán 
        JLabel lblPrice = new JLabel();
        lblPrice.setHorizontalAlignment(SwingConstants.LEFT);
        // Nếu có giảm giá, đặt chiều cao là 50 (đủ cho 2 dòng), nếu không thì 25
        int hGia = (wrapper.phanTramGiam > 0) ? 50 : 25;
        lblPrice.setPreferredSize(new Dimension(wGia, hGia));

        if (wrapper.phanTramGiam > 0) {
            String htmlGia = "<html><div style='line-height:1.2;'>" // Thêm line-height để căn chỉnh
                + "<div style='color:#ef4444; font-size:14px;'>" + GUI.HoTro.DinhDangUtil.dinhDangTien(wrapper.giaThucTe) + "</div>"
                + "<div style='color:#94a3b8; font-size:11px; font-weight:normal; text-decoration:line-through;'>" + GUI.HoTro.DinhDangUtil.dinhDangTien(sp.getGiaBan()) + "</div>"
                + "</div></html>";
            lblPrice.setText(htmlGia);
        } else {
            lblPrice.setForeground(new Color(37, 99, 235));
            lblPrice.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
            lblPrice.setText(GUI.HoTro.DinhDangUtil.dinhDangTien(sp.getGiaBan()));
        }
        content.add(lblPrice);
        // 4. Trạng thái
        String trangThai = (wrapper.tonMax == 0) ? "Hết Tồn Kho" : "Đang giao dịch";
        JPanel pnlStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
        pnlStatus.setPreferredSize(new Dimension(wTrangThai, 50));
        pnlStatus.setOpaque(false);
        pnlStatus.add(createBadge(trangThai));
        content.add(pnlStatus);

        // 5. Nút Hành Động
        JPanel pnlAction = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        pnlAction.setPreferredSize(new Dimension(120, 50));
        pnlAction.setOpaque(false);

        if (currentMode == UIMode.BAN_HANG) {
            NutBoGoc btnMua = new NutBoGoc("MUA");
            btnMua.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(13f));
            btnMua.setPreferredSize(new Dimension(90, 32));
            if (wrapper.tonMax == 0) {
                btnMua.setColorBackground(new Color(203, 213, 225)); 
                btnMua.setEnabled(false);
            } else {
                btnMua.setColorBackground(new Color(16, 185, 129)); 
                btnMua.addActionListener(e -> wrapper.thayDoiSoLuong(1));
            }
            pnlAction.add(btnMua);
        } else {
            JButton btnDetail = new JButton("📄 Chi tiết");
            btnDetail.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f));
            btnDetail.setForeground(new Color(59, 130, 246));
            btnDetail.setContentAreaFilled(false); 
            btnDetail.setBorderPainted(false); 
            btnDetail.setFocusPainted(false);
            btnDetail.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            btnDetail.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    btnDetail.setForeground(new Color(239, 68, 68)); 
                    Font hoverFont = TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 14f);
                    java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>(hoverFont.getAttributes());
                    attributes.put(java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON);
                    btnDetail.setFont(hoverFont.deriveFont(attributes));
                }
                @Override public void mouseExited(MouseEvent e) {
                    btnDetail.setForeground(new Color(59, 130, 246)); 
                    btnDetail.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f)); 
                }
            });
            btnDetail.addActionListener(e -> {
                ChiTietSanPham.showModal(DanhSachSPUi.this, sp, wrapper.tonMax, () -> {
                    taiDuLieuBanHangSieuToc("ALL"); 
                });
            });
            pnlAction.add(btnDetail);
        }
        content.add(pnlAction);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(ACCENT_HOVER); row.repaint(); }
            @Override public void mouseExited(MouseEvent e) { row.setBackground(COLOR_WHITE); row.repaint(); }
        });

        row.add(content, BorderLayout.CENTER);
        return row;
    }

    private void renderList(List<SanPham> data) {
        pnlRowListContainer.removeAll();
        for (SanPham sp : data) {
            TheSanPham wrapper = mapSanPham.get(sp.getMaSP());
            if (wrapper != null) {
                pnlRowListContainer.add(createRowPanel(sp, wrapper));
                pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 8))); 
            }
        }
        pnlRowListContainer.revalidate();
        pnlRowListContainer.repaint();
    }

    public void taiDuLieuBanHangSieuToc(String maLoai) {
        // 1. Xóa rỗng danh sách trên màn hình ngay lập tức để tạo cảm giác phản hồi nhanh
        pnlRowListContainer.removeAll();
        pnlRowListContainer.revalidate();
        pnlRowListContainer.repaint();

        // 2. Dùng SwingWorker đẩy việc tải Database xuống luồng ngầm (Hết lag UI)
        SwingWorker<List<SanPham>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SanPham> doInBackground() {
                // Tải dữ liệu dưới nền
                dataBanHangCache = TruyVanSieuTocDAO.getInstance().loadToanBoSanPhamBanHang();
                if (dataBanHangCache == null || dataBanHangCache.dsSanPham == null) return new ArrayList<>();

                List<SanPham> dsLoc = new ArrayList<>();
                for (SanPham sp : dataBanHangCache.dsSanPham) {
                    if (maLoai.equals("ALL") || sp.getMaLoai().equals(maLoai)) {
                        dsLoc.add(sp);
                    }
                }

                // Sắp xếp
                dsLoc.sort((a, b) -> {
                    int tonA = dataBanHangCache.mapTonKho.getOrDefault(a.getMaSP(), 0);
                    int tonB = dataBanHangCache.mapTonKho.getOrDefault(b.getMaSP(), 0);
                    int scoreA = tonA > 0 ? 0 : 1;
                    int scoreB = tonB > 0 ? 0 : 1;
                    return Integer.compare(scoreA, scoreB);
                });

                return dsLoc;
            }

            @Override
            protected void done() {
                try {
                    // Lấy kết quả từ luồng ngầm và Render lên giao diện
                    List<SanPham> dsLoc = get();
                    mapSanPham.clear();

                    for (SanPham sp : dsLoc) {
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
                    }

                    renderList(dsLoc); // Vẽ UI
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute(); // Kích hoạt luồng ngầm
    }

    public void loadDuLieuSanPham(String maLoai) {
        taiDuLieuBanHangSieuToc(maLoai);
    }

    private void thucHienTimKiem(String tuKhoa) {
        if (dataBanHangCache == null) return;
        
        String tuKhoaThuong = GUI.HoTro.DinhDangUtil.loaiBoDauTiengViet(tuKhoa.toLowerCase().trim());
        if (!tuKhoa.equals("Nhập tên sản phẩm...")) {
            tuKhoaThuong = DinhDangUtil.loaiBoDauTiengViet(tuKhoa.toLowerCase().trim());
        }

        List<SanPham> dsLoc = new ArrayList<>();
        for (SanPham sp : dataBanHangCache.dsSanPham) {
            String tenSPThuong = DinhDangUtil.loaiBoDauTiengViet(sp.getTenSP().toLowerCase());
            String maSPThuong = DinhDangUtil.loaiBoDauTiengViet(sp.getMaSP().toLowerCase());
            
            if (tuKhoaThuong.isEmpty() || tenSPThuong.contains(tuKhoaThuong) || maSPThuong.contains(tuKhoaThuong)) {
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
        
        renderList(dsLoc);
    }

    public Component[] layDanhSachTheSP() {
        return mapSanPham.values().toArray(new Component[0]); 
    }
    
    public TheSanPham getTheSanPham(String maSP) {
        return mapSanPham.get(maSP);
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
}