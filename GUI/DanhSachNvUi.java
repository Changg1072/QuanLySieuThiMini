package GUI;

import Dao.TruyVanSieuTocDAO;
import Data.NhanVien;
import Data.TaiKhoan;
import GUI.HoTro.*;
import Logic.NhanVienLogic;
import Logic.TaiKhoanLogic;
import Logic.TaoMaTuDongLogic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🚀 SẢN PHẨM: QUẢN LÝ NHÂN VIÊN VỚI ROW PANEL LIST (MODERN UI)
 * Thiết kế hoàn toàn không dùng JTable, mượt mà và linh hoạt!
 */
public class DanhSachNvUi extends JPanel {

    // ================= FORMATTER DÙNG CHUNG =================
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ================= MÀU SẮC CHỦ ĐẠO =================
    private static final Color BG_MAIN = new Color(245, 247, 250);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color HOVER_COLOR = new Color(238, 242, 246);
    private static final Color SELECTED_COLOR = new Color(239, 246, 255);
    private static final Color SELECT_BORDER = new Color(59, 130, 246);

    // ================= LOGIC & DỮ LIỆU =================
    private NhanVienLogic nvLogic = new NhanVienLogic();
    private TaiKhoanLogic tkLogic = new TaiKhoanLogic();
   // Thay thế List<NhanVien> thành List<NhanVienViewModel>
    private List<Data.NhanVienViewModel> danhSachGoc = new ArrayList<>();
    private List<Data.NhanVienViewModel> selectedEmployees = new ArrayList<>();

    private String currentRoleFilter = "Tất cả";
    private boolean isLoading = false;

    // ================= UI COMPONENTS =================
    private ONhapLieuHienDai txtTimKiem;
    private PillMenu tabChucVu;
    private JPanel pnlRowListContainer;
    private JLabel lblTongNV, lblAdmin, lblThuNgan, lblNghiViec;
    private String userRoleDangNhap = "ADMIN";
    private JComboBox<String> cbFilterTrangThai;

    public DanhSachNvUi() {
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(20, 25, 20, 25));

        initUI();
        loadDataSieuToc();
        setupListeners();
    }

    // =========================================================
    // HELPER: FORMAT NGÀY AN TOÀN (NULL-SAFE)
    // =========================================================

    /**
     * Format LocalDate sang chuỗi dd/MM/yyyy.
     * Trả về chuỗi mặc định nếu date là null.
     */
    private String formatNgay(LocalDate date, String defaultValue) {
        if (date == null) return defaultValue;
        return date.format(DATE_FMT);
    }

    // =========================================================
    // 1. KIẾN TRÚC UI CHÍNH
    // =========================================================
    private void initUI() {
        JPanel pnlNorth = new JPanel(new BorderLayout(20, 0));
        pnlNorth.setOpaque(false);

        txtTimKiem = new ONhapLieuHienDai("", true, false);
        txtTimKiem.setPlaceholder("🔍 Tìm mã NV, tên, SĐT...");
        txtTimKiem.setPreferredSize(new Dimension(400, 65));
        pnlNorth.add(txtTimKiem, BorderLayout.WEST);

        tabChucVu = new PillMenu(List.of("Tất cả", "ADMIN", "Thu Ngân"), tab -> {
            currentRoleFilter = tab;
            filterData();
        });
        cbFilterTrangThai = new JComboBox<>(new String[]{"Tất cả trạng thái", "Đang làm việc", "Không trong ca", "Đã nghỉ việc"});
        TienIchGiaoDien.trangTriComboBox(cbFilterTrangThai);
        cbFilterTrangThai.setPreferredSize(new Dimension(160, 40));
        cbFilterTrangThai.addActionListener(e -> filterData());

        JPanel pnlFilter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 15));
        pnlFilter.setOpaque(false);
        pnlFilter.add(tabChucVu);
        pnlNorth.add(pnlFilter, BorderLayout.EAST);

        JPanel pnlCenter = new JPanel(new BorderLayout(0, 10));
        pnlCenter.setOpaque(false);
        pnlCenter.add(createFakeHeader(), BorderLayout.NORTH);

        pnlRowListContainer = new JPanel();
        pnlRowListContainer.setLayout(new BoxLayout(pnlRowListContainer, BoxLayout.Y_AXIS));
        pnlRowListContainer.setBackground(BG_MAIN);

        JScrollPane scrollPane = new JScrollPane(pnlRowListContainer);
        TienIchGiaoDien.thietLapThanhCuon(scrollPane);
        scrollPane.getViewport().setBackground(BG_MAIN);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        pnlCenter.add(scrollPane, BorderLayout.CENTER);

        JPanel pnlEast = createStatsPanel();
        JPanel pnlSouth = createActionButtons();

        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlEast, BorderLayout.EAST);
        add(pnlSouth, BorderLayout.SOUTH);
    }

    // =========================================================
    // 2. FAKE HEADER
    // =========================================================
   private JPanel createFakeHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.setBackground(new Color(241, 245, 249));
        header.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 45)); // Tăng nhẹ chiều cao header cho thoáng

        // Đã canh lại tỷ lệ VÀNG, tổng width phủ kín đẹp không tì vết!
        header.add(createHeaderLabel("", 50));
        header.add(createHeaderLabel("Mã NV", 100));
        header.add(createHeaderLabel("Họ Tên", 250)); // Nới rộng chừa chỗ cho tên dài
        header.add(createHeaderLabel("SĐT", 130));
        header.add(createHeaderLabel("Chức vụ", 120));
        header.add(createHeaderLabel("Lương/Giờ", 130));
        header.add(createHeaderLabel("Ngày vào", 110));
        header.add(createHeaderLabel("Trạng thái", 160)); 

        return header;
    }

    private JLabel createHeaderLabel(String text, int width) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(15f));
        lbl.setForeground(new Color(30, 41, 59)); 
        lbl.setPreferredSize(new Dimension(width, 20));
        return lbl;
    }

    // =========================================================
    // 3. THỐNG KÊ (EAST)
    // =========================================================
    private JPanel createStatsPanel() {
        JPanel pnl = new JPanel(new BorderLayout(0, 15));
        pnl.setOpaque(false);
        pnl.setPreferredSize(new Dimension(250, 0));

        JPanel pnlCards = new JPanel(new GridLayout(4, 1, 0, 15));
        pnlCards.setOpaque(false);

        lblTongNV   = createStatCard(pnlCards, "Tổng nhân viên",         "0", TienIchGiaoDien.MAU_CHINH);
        lblAdmin    = createStatCard(pnlCards, "Quản trị viên (ADMIN)",   "0", new Color(245, 158, 11));
        lblThuNgan  = createStatCard(pnlCards, "Thu Ngân",                "0", new Color(16, 185, 129));
        lblNghiViec = createStatCard(pnlCards, "Đã nghỉ việc",            "0", new Color(239, 68, 68));

        pnl.add(pnlCards, BorderLayout.NORTH);
        return pnl;
    }

    private JLabel createStatCard(JPanel parent, String title, String val, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 5, getHeight(), 5, 5);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 15));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12.5f));
        lblTitle.setForeground(new Color(100, 116, 139));

        JLabel lblVal = new JLabel(val);
        lblVal.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(26f));
        lblVal.setForeground(color);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);
        parent.add(card);
        return lblVal;
    }
    // =========================================================
    // 4. ACTION BUTTONS (SOUTH)
    // =========================================================
    private JPanel createActionButtons() {
        JPanel pnl = new JPanel(new BorderLayout()); // Dùng BorderLayout để đẩy 2 bên
        pnl.setOpaque(false);

        // --- NHÓM 1: CÔNG CỤ (Bên trái) ---
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlLeft.setOpaque(false);
        
        JButton btnReload = TienIchGiaoDien.taoNutHienDai("Làm mới ↻", new Color(100, 116, 139));
        btnReload.setToolTipText("Tải lại danh sách nhân viên mới nhất");
        btnReload.addActionListener(e -> handleReload());

        JButton btnTinhLuong = TienIchGiaoDien.taoNutHienDai("💰 Bảng Lương", new Color(16, 185, 129));
        btnTinhLuong.setToolTipText("Xem tổng hợp lương và xuất file Excel/PDF");
        btnTinhLuong.addActionListener(e -> handleMoBangLuong()); // Gọi popup bảng lương

        pnlLeft.add(btnReload);
        pnlLeft.add(btnTinhLuong);

        // --- NHÓM 2 & 3: QUẢN TRỊ & THÊM MỚI (Bên phải) ---
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlRight.setOpaque(false);

        JButton btnResetPass = TienIchGiaoDien.taoNutHienDai("🔑 Reset Pass", new Color(245, 158, 11));
        btnResetPass.setToolTipText("Khôi phục mật khẩu về mặc định (Chỉ ADMIN)");
        btnResetPass.addActionListener(e -> handleResetPassword());

        JButton btnNghiViec = TienIchGiaoDien.taoNutHienDai("❌ Cho nghỉ", new Color(239, 68, 68));
        btnNghiViec.setToolTipText("Chuyển trạng thái nhân viên thành Đã nghỉ (Chỉ ADMIN)");
        btnNghiViec.addActionListener(e -> handleNghiViec());

        JButton btnThem = TienIchGiaoDien.taoNutHienDai("+ Thêm mới", TienIchGiaoDien.MAU_CHINH);
        btnThem.setToolTipText("Tạo hồ sơ nhân viên mới");
        btnThem.addActionListener(e -> handleThemNhanVien());

        // 🔒 BẢO MẬT: Ẩn/Hiện nút theo phân quyền
        if (!"ADMIN".equalsIgnoreCase(userRoleDangNhap)) {
            btnResetPass.setVisible(false);
            btnNghiViec.setVisible(false);
        }

        pnlRight.add(btnResetPass);
        pnlRight.add(btnNghiViec);
        pnlRight.add(Box.createHorizontalStrut(20)); // Tạo vách ngăn UX
        pnlRight.add(btnThem);

        pnl.add(pnlLeft, BorderLayout.WEST);
        pnl.add(pnlRight, BorderLayout.EAST);

        return pnl;
    }

    // =========================================================
    // 5. RENDER DANH SÁCH (ROW PANEL SYSTEM)
    // =========================================================
    private void renderList(List<Data.NhanVienViewModel> data) {
        pnlRowListContainer.removeAll();
        selectedEmployees.clear();

        if (isLoading) {
            for (int i = 0; i < 5; i++) {
                pnlRowListContainer.add(createSkeletonRow());
                pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        } else {
            for (Data.NhanVienViewModel nv : data) {
                pnlRowListContainer.add(createRowPanel(nv));
                pnlRowListContainer.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        pnlRowListContainer.revalidate();
        pnlRowListContainer.repaint();
    }

    private JPanel createRowPanel(Data.NhanVienViewModel nv) {
        // Kiểm tra trạng thái nghỉ việc
        boolean isNghiViec = "Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());

        // ── NULL-SAFE: format ngày vào làm ──────────────────────────────────
        String strNgayVao = formatNgay(nv.getNgayVaoLam(), "Chưa cập nhật");

        // ── NULL-SAFE: format ngày nghỉ việc ────────────────────────────────
        String strNgayNghi = formatNgay(nv.getNgayNghiViec(), "Chưa xác định");

        JPanel row = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isSelected = selectedEmployees.contains(nv);

                if (isSelected) g2.setColor(SELECTED_COLOR);
                else            g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                if (isSelected) {
                    g2.setColor(SELECT_BORDER);
                    g2.fillRoundRect(0, 0, 6, getHeight(), 15, 15);
                    g2.setColor(SELECT_BORDER);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                } else {
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                }

                if (isNghiViec) {
                    g2.setColor(new Color(255, 255, 255, 180));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, isNghiViec ? 85 : 65));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isNghiViec ? 85 : 65));

        // -- Nội dung Row --
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        content.setOpaque(false);

        // 1. Checkbox / Toggle
        TienIchGiaoDien.NutGat cbSelect = new TienIchGiaoDien.NutGat();
        cbSelect.setPreferredSize(new Dimension(50, 25));
        cbSelect.addActionListener(e -> handleSelection(nv, cbSelect.isSelected(), row));
        content.add(cbSelect);

        // 2. Mã NV
        content.add(createCell(nv.getMaNV(), 100, true));

        // 3. Họ tên + dòng phụ nghỉ việc
        JPanel pnlName = new JPanel(new GridLayout(isNghiViec ? 2 : 1, 1, 0, 2));
        pnlName.setOpaque(false);
        pnlName.setPreferredSize(new Dimension(250, isNghiViec ? 50 : 25));
        pnlName.add(createCell(nv.getHoTen() != null ? nv.getHoTen() : "—", 250, true));
        if (isNghiViec) {
            JLabel lblNghi = createCell("Nghỉ từ: " + strNgayNghi, 250, false);
            lblNghi.setForeground(new Color(239, 68, 68)); // Giữ màu đỏ rực rỡ cảnh báo
            lblNghi.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f)); 
            pnlName.add(lblNghi);
        }
        content.add(pnlName);

        // 4. SĐT
        content.add(createCell(nv.getSDT() != null ? nv.getSDT() : "—", 130, false));

        // 5. Chức vụ
        content.add(createCell(nv.getChucVu() != null ? nv.getChucVu() : "—", 120, false));

        // 6. Lương/giờ
        content.add(createCell(DinhDangUtil.dinhDangTien(nv.getLuongGio()), 120, false));

        // 7. Ngày vào làm (NULL-SAFE ở đây)
        content.add(createCell(strNgayVao, 110, false));
        // 8. 🔴 HIỆU ỨNG BADGE TRẠNG THÁI (ĐÃ NÂNG CẤP MÀU SẮC & TRỰC QUAN) 🔴
        boolean isDaNghi = "Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());
        boolean isWorking = "Đang làm việc".equals(nv.getTrangThaiLamViec()) && !isDaNghi;
        
        JPanel pnlBadge = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Set nền theo trạng thái
                if (isDaNghi) {
                    g2.setColor(new Color(254, 226, 226)); // Nền đỏ hồng (Đã nghỉ)
                } else if (isWorking) {
                    g2.setColor(new Color(209, 250, 229)); // Nền xanh lá (Đang làm)
                } else {
                    g2.setColor(new Color(241, 245, 249)); // Nền xám nhạt (Không trong ca)
                }
                g2.fillRoundRect(0, 4, getWidth(), 24, 24, 24);
                
                // Vẽ dấu chấm (Dot status)
                if (isDaNghi) {
                    g2.setColor(new Color(220, 38, 38)); // Chấm đỏ
                } else if (isWorking) {
                    g2.setColor(new Color(16, 185, 129)); // Chấm xanh
                } else {
                    g2.setColor(new Color(148, 163, 184)); // Chấm xám
                }
                g2.fillOval(10, 11, 10, 10); // Vẽ icon tròn
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlBadge.setOpaque(false);
        pnlBadge.setPreferredSize(new Dimension(160, 32));
        
        // Text hiển thị
        String textStatus = isDaNghi ? "Đã nghỉ việc" : (isWorking ? "Đang làm việc" : "Không trong ca");
        JLabel lblStatus = new JLabel(textStatus);
        lblStatus.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12f));
        lblStatus.setBorder(new EmptyBorder(0, 28, 0, 0)); // Đẩy text sang phải nhường chỗ cho Dot
        
        // Màu chữ
        if (isDaNghi) {
            lblStatus.setForeground(new Color(153, 27, 27)); // Đỏ thẫm
        } else if (isWorking) {
            lblStatus.setForeground(new Color(4, 120, 87)); // Xanh thẫm
        } else {
            lblStatus.setForeground(new Color(71, 85, 105)); // Xám thẫm
        }
        
        pnlBadge.add(lblStatus, BorderLayout.CENTER);
        content.add(pnlBadge);

        // --- 9. NÚT CHỈNH SỬA (Nằm ở cuối hàng) ---
        JButton btnEdit = new JButton("✏️ Sửa");
        btnEdit.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(13f));
        btnEdit.setForeground(new Color(59, 130, 246)); // Màu xanh dương uy tín
        btnEdit.setContentAreaFilled(false);
        btnEdit.setBorderPainted(false);
        btnEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnEdit.addActionListener(e -> {
            // Lấy Object NhanVien chuẩn từ DB lên để sửa (an toàn data)
            NhanVien nvFull = nvLogic.timNhanVienTheoMa(nv.getMaNV());
            if(nvFull != null) {
                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                SuaNhanVienDialog dialog = new SuaNhanVienDialog(parentWindow, nvFull);
                dialog.setVisible(true);

                // Nếu sửa & pass lớp bảo mật thành công -> Reload lại nguyên cái bảng & Thống kê
                if (dialog.isSuccess()) {
                    loadDataSieuToc(); 
                }
            }
        });
        
        content.add(Box.createHorizontalStrut(10)); // Tạo khoảng cách nhỏ
        content.add(btnEdit);

        // Hover effect
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!selectedEmployees.contains(nv)) { row.setBackground(HOVER_COLOR); row.repaint(); }
            }
            @Override public void mouseExited(MouseEvent e)  {
                row.setBackground(Color.WHITE); row.repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                cbSelect.doClick();
            }
        });

        row.add(content, BorderLayout.CENTER);
        return row;
    }

    private JLabel createCell(String text, int width, boolean isBold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(isBold
                ? TienIchGiaoDien.FONT_DAM.deriveFont(16f)
                : TienIchGiaoDien.FONT_CHINH.deriveFont(15f));
        lbl.setForeground(isBold ? new Color(17, 24, 39) : new Color(55, 65, 81));
        lbl.setPreferredSize(new Dimension(width, 25));
        return lbl;
    }

    private JPanel createSkeletonRow() {
        JPanel skeleton = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
            }
        };
        skeleton.setOpaque(false);
        skeleton.setPreferredSize(new Dimension(0, 65));
        skeleton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        return skeleton;
    }

    // =========================================================
    // 6. LOGIC & DATA
    // =========================================================
    private void loadDataSieuToc() {
        isLoading = true;
        renderList(new ArrayList<>());

        SwingWorker<List<Data.NhanVienViewModel>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Data.NhanVienViewModel> doInBackground() {
                try { Thread.sleep(300); } catch (Exception ignored) {}
                // Gọi duy nhất 1 truy vấn thần tốc!
                return TruyVanSieuTocDAO.getInstance().getDanhSachNhanVienKemTrangThai();
            }

            @Override
            protected void done() {
                try {
                    danhSachGoc = get();
                    isLoading = false;
                    updateStats();
                    filterData(); // Bộ lọc tự chạy trên memory, không chọc DB
                } catch (Exception e) {
                    e.printStackTrace();
                    isLoading = false;
                }
            }
        };
        worker.execute();
    }
    
    
    private void filterData() {
        if (isLoading) return;

        String key = DinhDangUtil.loaiBoDauTiengViet(txtTimKiem.getText().toLowerCase().trim());

        List<Data.NhanVienViewModel> filtered = danhSachGoc.stream().filter(nv -> {
            // Lọc chức vụ
            boolean matchRole = currentRoleFilter.equals("Tất cả")
                    || (nv.getChucVu() != null && nv.getChucVu().equalsIgnoreCase(currentRoleFilter));

            // Lọc trạng thái (MỚI)
            String cboFilter = cbFilterTrangThai.getSelectedItem().toString();
            boolean matchTrangThai = true;
            if (cboFilter.equals("Đã nghỉ việc")) {
                matchTrangThai = "Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());
            } else if (cboFilter.equals("Đang làm việc")) {
                matchTrangThai = "Đang làm việc".equals(nv.getTrangThaiLamViec()) && !"Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());
            } else if (cboFilter.equals("Không trong ca")) {
                matchTrangThai = "Không trong ca".equals(nv.getTrangThaiLamViec()) && !"Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());
            }

            // Lọc tìm kiếm Text
            boolean matchKey = true;
            if (!key.isEmpty()) {
                if (key.matches("\\d+")) {
                    matchKey = nv.getSDT() != null && nv.getSDT().contains(key);
                } else if (key.startsWith("nv")) {
                    matchKey = nv.getMaNV() != null && nv.getMaNV().toLowerCase().contains(key);
                } else {
                    matchKey = nv.getHoTen() != null
                            && DinhDangUtil.loaiBoDauTiengViet(nv.getHoTen().toLowerCase()).contains(key);
                }
            }
            return matchRole && matchTrangThai && matchKey;
        }).collect(Collectors.toList());

        // Sort: đang làm lên trước, nghỉ việc xuống dưới
        filtered.sort((n1, n2) -> {
            int s1 = "Đã Nghỉ".equalsIgnoreCase(n1.getTrangThai()) ? 1 : 0;
            int s2 = "Đã Nghỉ".equalsIgnoreCase(n2.getTrangThai()) ? 1 : 0;
            return Integer.compare(s1, s2);
        });

        renderList(filtered);
    }

    private void updateStats() {
        lblTongNV.setText(String.valueOf(danhSachGoc.size()));
        lblAdmin.setText(String.valueOf(
                danhSachGoc.stream().filter(n -> "ADMIN".equalsIgnoreCase(n.getChucVu())).count()));
        lblThuNgan.setText(String.valueOf(
                danhSachGoc.stream().filter(n -> "Thu Ngân".equalsIgnoreCase(n.getChucVu())).count()));
        lblNghiViec.setText(String.valueOf(
                danhSachGoc.stream().filter(n -> "Đã Nghỉ".equalsIgnoreCase(n.getTrangThai())).count()));
    }

    // =========================================================
    // 7. HANDLERS
    // =========================================================
    private void setupListeners() {
        txtTimKiem.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterData(); }
            public void removeUpdate(DocumentEvent e) { filterData(); }
            public void changedUpdate(DocumentEvent e) { filterData(); }
        });
    }

    private void handleSelection(Data.NhanVienViewModel nv, boolean isSelected, JPanel rowPanel) {
        if (isSelected) {
            if (!selectedEmployees.contains(nv)) selectedEmployees.add(nv);
        } else {
            selectedEmployees.remove(nv);
        }
        rowPanel.repaint();
    }

    private void handleReload() {
        txtTimKiem.clear();
        tabChucVu.setActiveTab("Tất cả");
        currentRoleFilter = "Tất cả";
        loadDataSieuToc();
    }

    private void handleThemNhanVien() {
        // Lấy window cha để popup có thể hiển thị chính giữa và dim nền đúng chuẩn
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        
        // Mở Popup
        ThemNhanVienDialog dialog = new ThemNhanVienDialog(parentWindow);
        dialog.setVisible(true);

        // Kiểm tra kết quả sau khi đóng popup
        if (dialog.isSuccess()) {
            try {
                // Lấy data từ popup
                NhanVien nvMoi = dialog.getNhanVienMoi();
                TaiKhoan tkMoi = dialog.getTaiKhoanMoi();

                // Lưu vào Database
                nvLogic.themNhanVien(nvMoi);
                tkLogic.themTaiKhoan(tkMoi);

                // Thông báo & Load lại bảng
                TienIchGiaoDien.hienThiThongBao(this, "Đã tạo tài khoản thành công cho: " + nvMoi.getHoTen(), "SUCCESS");
                loadDataSieuToc();
            } catch (Exception e) {
                TienIchGiaoDien.hienThiThongBao(this, "Lỗi khi lưu Database: " + e.getMessage(), "ERROR");
            }
        }
    }

    private void handleResetPassword() {
        if (selectedEmployees.isEmpty()) {
            TienIchGiaoDien.hienThiThongBao(this, "Vui lòng chọn ít nhất 1 nhân viên!", "WARNING");
            return;
        }
        TienIchGiaoDien.hienThiXacNhan(this,
                "Reset mật khẩu cho " + selectedEmployees.size() + " nhân viên về mã NV?", () -> {
            try {
                for (NhanVien nv : selectedEmployees) {
                    tkLogic.resetMatKhau(nv.getMaNV());
                }
                TienIchGiaoDien.hienThiThongBao(this, "Reset mật khẩu thành công!", "SUCCESS");
                selectedEmployees.clear();
                filterData();
            } catch (Exception e) {
                TienIchGiaoDien.hienThiThongBao(this, e.getMessage(), "ERROR");
            }
        });
    }

    private void handleNghiViec() {
        if (selectedEmployees.isEmpty()) {
            TienIchGiaoDien.hienThiThongBao(this, "Vui lòng chọn ít nhất 1 nhân viên!", "WARNING");
            return;
        }
        TienIchGiaoDien.hienThiXacNhan(this,
                "Cho " + selectedEmployees.size() + " nhân viên nghỉ việc?", () -> {
            try {
                for (NhanVien nv : selectedEmployees) {
                    nv.setTrangThai("Đã Nghỉ");
                    nv.setNgayNghiViec(LocalDate.now());
                    nvLogic.suaNhanVien(nv);
                }
                TienIchGiaoDien.hienThiThongBao(this, "Cập nhật trạng thái thành công!", "SUCCESS");
                loadDataSieuToc();
            } catch (Exception e) {
                TienIchGiaoDien.hienThiThongBao(this, e.getMessage(), "ERROR");
            }
        });
    }

    // =========================================================
    // PILL MENU COMPONENT NỘI BỘ
    // =========================================================
    private class PillMenu extends JPanel {
        private List<JButton> btns = new ArrayList<>();

        public PillMenu(List<String> items, java.util.function.Consumer<String> onSelect) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
            setOpaque(false);
            for (String item : items) {
                JButton btn = new JButton(item) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        if (Boolean.TRUE.equals(getClientProperty("active"))) {
                            g2.setColor(TienIchGiaoDien.MAU_CHINH);
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                        } else {
                            g2.setColor(Color.WHITE);
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                            g2.setColor(BORDER_COLOR);
                            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
                        }
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                btn.putClientProperty("active", item.equals(items.get(0)));
                btn.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12.5f));
                btn.setForeground(item.equals(items.get(0)) ? Color.WHITE : TienIchGiaoDien.MAU_CHU_PHU);
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setBorder(new EmptyBorder(8, 16, 8, 16));
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> { setActiveTab(item); onSelect.accept(item); });
                btns.add(btn);
                add(btn);
            }
        }

        public void setActiveTab(String name) {
            for (JButton b : btns) {
                boolean isActive = b.getText().equals(name);
                b.putClientProperty("active", isActive);
                b.setForeground(isActive ? Color.WHITE : TienIchGiaoDien.MAU_CHU_PHU);
                b.repaint();
            }
        }
    }
    private void handleMoBangLuong() {
        // Code mở một Dialog mới chứa JTable chi tiết giờ làm + Biểu đồ
        // Dialog này sẽ có 2 nút: "Xuất Excel" và "Xuất PDF"
        TienIchGiaoDien.hienThiThongBao(this, "Chức năng xem Bảng Lương và Xuất Excel đang được thiết kế ở Module riêng!", "INFO");
        
        /* Skeleton Xuất Excel (Dùng Apache POI):
           HSSFWorkbook workbook = new HSSFWorkbook();
           HSSFSheet sheet = workbook.createSheet("Bảng Lương");
           // Vòng lặp duyệt danh sách nhân viên và tính tổng giờ làm x Lương/Giờ
           // ...
           FileOutputStream out = new FileOutputStream(new File("BangLuong.xls"));
           workbook.write(out);
           out.close();
        */
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame();
            f.setSize(1200, 700);
            f.setLocationRelativeTo(null);
            f.add(new DanhSachNvUi());
            f.setVisible(true);
        });
    }
}