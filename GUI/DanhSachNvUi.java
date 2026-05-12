package GUI;

import Dao.TruyVanSieuTocDAO;
import Data.NhanVien;
import Data.TaiKhoan;
import GUI.HoTro.*;
import Logic.NhanVienLogic;
import Logic.TaiKhoanLogic;

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
 * 🚀 QUẢN LÝ NHÂN VIÊN VỚI ROW PANEL LIST (MODERN UI)
 * - Đã đồng bộ UI/UX (Ô tick Gmail, Chọn tất cả, Ẩn/Hiện nút tự động)
 * - Đã nới rộng full bảng.
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
    
    private List<Data.NhanVienViewModel> danhSachGoc = new ArrayList<>();
    private List<Data.NhanVienViewModel> currentDisplayedList = new ArrayList<>(); // Danh sách đang hiển thị
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

    // Các Component UX mới
    private ModernCheckBox cbSelectAll;
    private JButton btnResetPass;
    private JButton btnNghiViec;

    public DanhSachNvUi() {
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_MAIN);
        setBorder(new EmptyBorder(20, 25, 20, 25));

        initUI();
        loadDataSieuToc();
        setupListeners();
        this.setFocusable(true); 
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                requestFocusInWindow(); // Ép nền cướp lấy focus
            }
        });
    }

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
    // 2. FAKE HEADER (Đã nới rộng full màn hình)
    // =========================================================
   private JPanel createFakeHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.setBackground(new Color(241, 245, 249));
        header.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 45));

        // 🔥 THÊM CHỌN TẤT CẢ VÀO HEADER
        cbSelectAll = new ModernCheckBox();
        cbSelectAll.setPreferredSize(new Dimension(40, 20));
        cbSelectAll.addActionListener(e -> handleSelectAll(cbSelectAll.isSelected()));
        header.add(cbSelectAll);
        header.add(createHeaderLabel("Mã NV", 70));
        header.add(createHeaderLabel("Họ Tên", 230));    // Giảm từ 350 -> 230
        header.add(createHeaderLabel("SĐT", 120));       // Giảm từ 150 -> 120
        header.add(createHeaderLabel("Chức vụ", 120));   // Giảm từ 150 -> 120
        header.add(createHeaderLabel("Lương/Giờ", 140)); // Giảm từ 150 -> 120
        header.add(createHeaderLabel("Ngày vào làm", 140)); // Giảm từ 150 -> 120
        header.add(createHeaderLabel("Trạng thái", 150)); // Nới rộng
        // Nút sửa nằm ở cuối cùng không cần label

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
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(color); g2.fillRoundRect(0, 0, 5, getHeight(), 5, 5);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false); card.setBorder(new EmptyBorder(15, 20, 15, 15));
        JLabel lblTitle = new JLabel(title); lblTitle.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12.5f)); lblTitle.setForeground(new Color(100, 116, 139));
        JLabel lblVal = new JLabel(val); lblVal.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(26f)); lblVal.setForeground(color);
        card.add(lblTitle, BorderLayout.NORTH); card.add(lblVal, BorderLayout.CENTER); parent.add(card); return lblVal;
    }
    // =========================================================
    // 4. ACTION BUTTONS (SOUTH)
    // =========================================================
    private JPanel createActionButtons() {
        JPanel pnl = new JPanel(new BorderLayout()); 
        pnl.setOpaque(false);

        // --- NHÓM 1: CÔNG CỤ (Bên trái) ---
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlLeft.setOpaque(false);
        
        JButton btnReload = TienIchGiaoDien.taoNutHienDai("Làm mới ↻", new Color(100, 116, 139));
        btnReload.addActionListener(e -> handleReload());

        JButton btnTinhLuong = TienIchGiaoDien.taoNutHienDai("💰 Bảng Lương", new Color(16, 185, 129));
        btnTinhLuong.addActionListener(e -> handleMoBangLuong()); 

        pnlLeft.add(btnReload);
        pnlLeft.add(btnTinhLuong);

        // --- NHÓM 2 & 3: QUẢN TRỊ & THÊM MỚI (Bên phải) ---
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlRight.setOpaque(false);

        // 🔥 Khởi tạo nút nhưng mặc định ẨN
        btnResetPass = TienIchGiaoDien.taoNutHienDai("🔑 Reset Pass", new Color(245, 158, 11));
        btnResetPass.addActionListener(e -> handleResetPassword());
        btnResetPass.setVisible(false);

        btnNghiViec = TienIchGiaoDien.taoNutHienDai("❌ Cho nghỉ", new Color(239, 68, 68));
        btnNghiViec.addActionListener(e -> handleNghiViec());
        btnNghiViec.setVisible(false);

        JButton btnThem = TienIchGiaoDien.taoNutHienDai("+ Thêm mới", TienIchGiaoDien.MAU_CHINH);
        btnThem.addActionListener(e -> handleThemNhanVien());

        pnlRight.add(btnResetPass);
        pnlRight.add(btnNghiViec);
        pnlRight.add(Box.createHorizontalStrut(20)); 
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
        boolean isNghiViec = "Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());
        String strNgayVao = formatNgay(nv.getNgayVaoLam(), "Chưa cập nhật");
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
        row.setPreferredSize(new Dimension(0, isNghiViec ? 85 : 60));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isNghiViec ? 85 : 60));

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        content.setOpaque(false);

        // 1. Checkbox Modern
        ModernCheckBox cbSelect = new ModernCheckBox();
        cbSelect.setPreferredSize(new Dimension(40, 25));
        cbSelect.setSelected(selectedEmployees.contains(nv));
        cbSelect.addActionListener(e -> handleSelection(nv, cbSelect.isSelected(), row));
        content.add(cbSelect);

        content.add(createCell(nv.getMaNV(), 70, true));

        // Khung Họ tên
        JPanel pnlName = new JPanel(new GridLayout(isNghiViec ? 2 : 1, 1, 0, 2));
        pnlName.setOpaque(false);
        pnlName.setPreferredSize(new Dimension(230, isNghiViec ? 50 : 25)); // Sửa thành 230
        pnlName.add(createCell(nv.getHoTen() != null ? nv.getHoTen() : "—", 230, true)); 
        if (isNghiViec) {
            JLabel lblNghi = createCell("Nghỉ từ: " + strNgayNghi, 230, false); 
            lblNghi.setForeground(new Color(239, 68, 68)); 
            lblNghi.setFont(TienIchGiaoDien.FONT_CHINH.deriveFont(12f)); 
            pnlName.add(lblNghi);
        }
        content.add(pnlName);

        content.add(createCell(nv.getSDT() != null ? nv.getSDT() : "—", 120, false));
        content.add(createCell(nv.getChucVu() != null ? nv.getChucVu() : "—", 120, false));
        content.add(createCell(DinhDangUtil.dinhDangTien(nv.getLuongGio()), 140, false));
        content.add(createCell(strNgayVao, 140, false));
     // HIỆU ỨNG BADGE TRẠNG THÁI
        boolean isDaNghi = "Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());
        boolean isWorking = "Đang làm việc".equals(nv.getTrangThaiLamViec()) && !isDaNghi;
        
        JPanel pnlBadge = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isDaNghi) g2.setColor(new Color(254, 226, 226)); 
                else if (isWorking) g2.setColor(new Color(209, 250, 229)); 
                else g2.setColor(new Color(241, 245, 249)); 
                
                // 🔥 ĐÃ ÉP CÂN: Vẽ khung nền với chiều cao 25px (y=0)
                g2.fillRoundRect(0, 0, getWidth(), 25, 25, 25);
                
                if (isDaNghi) g2.setColor(new Color(220, 38, 38)); 
                else if (isWorking) g2.setColor(new Color(16, 185, 129)); 
                else g2.setColor(new Color(148, 163, 184)); 
                
                // 🔥 Căn giữa lại dấu chấm tròn (y=7)
                g2.fillOval(10, 7, 10, 10); 
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlBadge.setOpaque(false);
        // 🔥 BÍ QUYẾT LÀ ĐÂY: Hạ chiều cao Badge từ 32px xuống 25px (bằng với các ô text)
        pnlBadge.setPreferredSize(new Dimension(150, 25));
        
        String textStatus = isDaNghi ? "Đã nghỉ việc" : (isWorking ? "Đang làm việc" : "Không trong ca");
        JLabel lblStatus = new JLabel(textStatus);
        lblStatus.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12f));
        lblStatus.setBorder(new EmptyBorder(0, 28, 0, 0)); 
        
        if (isDaNghi) lblStatus.setForeground(new Color(153, 27, 27)); 
        else if (isWorking) lblStatus.setForeground(new Color(4, 120, 87)); 
        else lblStatus.setForeground(new Color(71, 85, 105)); 
        
        pnlBadge.add(lblStatus, BorderLayout.CENTER);
        content.add(pnlBadge);	

     // NÚT CHỈNH SỬA (MƯỢN Y XÌ ĐÚC TỪ FILE NHẬP HÀNG)
        JButton btnEdit = new JButton("✏️ Sửa");
        btnEdit.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(14f)); // Trả về 13f y hệt Chi tiết
        btnEdit.setForeground(new Color(59, 130, 246)); 
        btnEdit.setContentAreaFilled(false); 
        btnEdit.setBorderPainted(false); 
        btnEdit.setFocusPainted(false);
        btnEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnEdit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Đổi màu Đỏ
                btnEdit.setForeground(new Color(239, 68, 68)); 
                
                // Phông 14f + In đậm + Gạch chân
                Font hoverFont = TienIchGiaoDien.FONT_DAM.deriveFont(Font.BOLD, 14f);
                java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>(hoverFont.getAttributes());
                attributes.put(java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON);
                btnEdit.setFont(hoverFont.deriveFont(attributes));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Trả về xanh lam & phông 13f (TUYỆT ĐỐI KHÔNG SET LẠI BORDER)
                btnEdit.setForeground(new Color(59, 130, 246)); 
                btnEdit.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(13f)); 
            }
        });
        
        btnEdit.addActionListener(e -> {
            NhanVien nvFull = nvLogic.timNhanVienTheoMa(nv.getMaNV());
            if(nvFull != null) {
                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                SuaNhanVienDialog dialog = new SuaNhanVienDialog(parentWindow, nvFull);
                dialog.setVisible(true);
                if (dialog.isSuccess()) loadDataSieuToc(); 
            }
        });

        // Ẩn nút nếu đã nghỉ việc
        if (!isNghiViec) {
            content.add(Box.createHorizontalStrut(20)); 
            content.add(btnEdit);
        }

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
        lbl.setFont(isBold ? TienIchGiaoDien.FONT_DAM.deriveFont(16f) : TienIchGiaoDien.FONT_CHINH.deriveFont(15f));
        lbl.setForeground(isBold ? new Color(17, 24, 39) : new Color(55, 65, 81));
        lbl.setPreferredSize(new Dimension(width, 25));
        return lbl;
    }

    private JPanel createSkeletonRow() {
        JPanel skeleton = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
            }
        };
        skeleton.setOpaque(false); skeleton.setPreferredSize(new Dimension(0, 65)); skeleton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65)); return skeleton;
    }

    // =========================================================
    // 6. LOGIC & DATA
    // =========================================================
    private void loadDataSieuToc() {
        isLoading = true;
        renderList(new ArrayList<>());

        SwingWorker<List<Data.NhanVienViewModel>, Void> worker = new SwingWorker<>() {
            @Override protected List<Data.NhanVienViewModel> doInBackground() {
                try { Thread.sleep(200); } catch (Exception ignored) {}
                return TruyVanSieuTocDAO.getInstance().getDanhSachNhanVienKemTrangThai();
            }
            @Override protected void done() {
                try {
                    danhSachGoc = get();
                    isLoading = false;
                    updateStats();
                    selectedEmployees.clear(); // Reset tick
                    filterData(); 
                } catch (Exception e) { e.printStackTrace(); isLoading = false; }
            }
        };
        worker.execute();
    }
    
    private void filterData() {
        if (isLoading) return;

        String key = DinhDangUtil.loaiBoDauTiengViet(txtTimKiem.getText().toLowerCase().trim());

        currentDisplayedList = danhSachGoc.stream().filter(nv -> {
            boolean matchRole = currentRoleFilter.equals("Tất cả") || (nv.getChucVu() != null && nv.getChucVu().equalsIgnoreCase(currentRoleFilter));
            String cboFilter = cbFilterTrangThai.getSelectedItem().toString();
            boolean matchTrangThai = true;
            if (cboFilter.equals("Đã nghỉ việc")) matchTrangThai = "Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());
            else if (cboFilter.equals("Đang làm việc")) matchTrangThai = "Đang làm việc".equals(nv.getTrangThaiLamViec()) && !"Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());
            else if (cboFilter.equals("Không trong ca")) matchTrangThai = "Không trong ca".equals(nv.getTrangThaiLamViec()) && !"Đã Nghỉ".equalsIgnoreCase(nv.getTrangThai());

            boolean matchKey = true;
            if (!key.isEmpty()) {
                if (key.matches("\\d+")) matchKey = nv.getSDT() != null && nv.getSDT().contains(key);
                else if (key.startsWith("nv")) matchKey = nv.getMaNV() != null && nv.getMaNV().toLowerCase().contains(key);
                else matchKey = nv.getHoTen() != null && DinhDangUtil.loaiBoDauTiengViet(nv.getHoTen().toLowerCase()).contains(key);
            }
            return matchRole && matchTrangThai && matchKey;
        }).collect(Collectors.toList());

        currentDisplayedList.sort((n1, n2) -> {
            int s1 = "Đã Nghỉ".equalsIgnoreCase(n1.getTrangThai()) ? 1 : 0;
            int s2 = "Đã Nghỉ".equalsIgnoreCase(n2.getTrangThai()) ? 1 : 0;
            return Integer.compare(s1, s2);
        });

        renderList(currentDisplayedList);
        updateSelectionUIState(); // Cập nhật lại UI nút bấm
    }

    private void updateStats() {
        lblTongNV.setText(String.valueOf(danhSachGoc.size()));
        lblAdmin.setText(String.valueOf(danhSachGoc.stream().filter(n -> "ADMIN".equalsIgnoreCase(n.getChucVu())).count()));
        lblThuNgan.setText(String.valueOf(danhSachGoc.stream().filter(n -> "Thu Ngân".equalsIgnoreCase(n.getChucVu())).count()));
        lblNghiViec.setText(String.valueOf(danhSachGoc.stream().filter(n -> "Đã Nghỉ".equalsIgnoreCase(n.getTrangThai())).count()));
    }

    // =========================================================
    // 7. HANDLERS & UX LOGIC
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
        updateSelectionUIState();
    }

    private void handleSelectAll(boolean isSelected) {
        selectedEmployees.clear();
        if (isSelected) {
            selectedEmployees.addAll(currentDisplayedList);
        }
        renderList(currentDisplayedList); 
        updateSelectionUIState();
    }

    private void updateSelectionUIState() {
        boolean hasSelection = !selectedEmployees.isEmpty();
        
        // CHỈ HIỆN KHI CÓ QUYỀN ADMIN VÀ CÓ CHỌN ÍT NHẤT 1 NV
        if ("ADMIN".equalsIgnoreCase(userRoleDangNhap)) {
            if (btnNghiViec != null) btnNghiViec.setVisible(hasSelection);
            if (btnResetPass != null) btnResetPass.setVisible(hasSelection);
        }

        if (currentDisplayedList.isEmpty()) {
            cbSelectAll.setSelected(false);
        } else {
            boolean allSelected = currentDisplayedList.stream().allMatch(nv -> selectedEmployees.contains(nv));
            cbSelectAll.setSelected(allSelected);
        }
        cbSelectAll.repaint();
    }

    private void handleReload() {
        txtTimKiem.clear();
        tabChucVu.setActiveTab("Tất cả");
        currentRoleFilter = "Tất cả";
        loadDataSieuToc();
    }

    private void handleThemNhanVien() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ThemNhanVienDialog dialog = new ThemNhanVienDialog(parentWindow);
        dialog.setVisible(true);

        if (dialog.isSuccess()) {
            try {
                NhanVien nvMoi = dialog.getNhanVienMoi();
                TaiKhoan tkMoi = dialog.getTaiKhoanMoi();
                nvLogic.themNhanVien(nvMoi);
                tkLogic.themTaiKhoan(tkMoi);
                TienIchGiaoDien.hienThiThongBao(this, "Đã tạo tài khoản thành công cho: " + nvMoi.getHoTen(), "SUCCESS");
                loadDataSieuToc();
            } catch (Exception e) { TienIchGiaoDien.hienThiThongBao(this, "Lỗi khi lưu Database: " + e.getMessage(), "ERROR"); }
        }
    }

    private void handleResetPassword() {
        if (selectedEmployees.isEmpty()) return;
        TienIchGiaoDien.hienThiXacNhan(this, "Reset mật khẩu cho " + selectedEmployees.size() + " nhân viên về mã NV?", () -> {
            try {
                for (Data.NhanVienViewModel nv : selectedEmployees) { tkLogic.resetMatKhau(nv.getMaNV()); }
                TienIchGiaoDien.hienThiThongBao(this, "Reset mật khẩu thành công!", "SUCCESS");
                selectedEmployees.clear(); filterData();
            } catch (Exception e) { TienIchGiaoDien.hienThiThongBao(this, e.getMessage(), "ERROR"); }
        });
    }

    private void handleNghiViec() {
        if (selectedEmployees.isEmpty()) return;
        TienIchGiaoDien.hienThiXacNhan(this, "Cho " + selectedEmployees.size() + " nhân viên nghỉ việc?", () -> {
            try {
                for (Data.NhanVienViewModel nvView : selectedEmployees) {
                    NhanVien nvFull = nvLogic.timNhanVienTheoMa(nvView.getMaNV());
                    if (nvFull != null) {
                        nvFull.setTrangThai("Đã Nghỉ");
                        nvFull.setNgayNghiViec(LocalDate.now());
                        nvLogic.suaNhanVien(nvFull);
                    }
                }
                TienIchGiaoDien.hienThiThongBao(this, "Cập nhật trạng thái thành công!", "SUCCESS");
                loadDataSieuToc();
            } catch (Exception e) { TienIchGiaoDien.hienThiThongBao(this, e.getMessage(), "ERROR"); }
        });
    }

    private void handleMoBangLuong() {
        TienIchGiaoDien.hienThiThongBao(this, "Chức năng xem Bảng Lương đang được phát triển!", "INFO");
    }

    // =========================================================
    // 🎨 COMPONENT: MODERN CHECKBOX (O TICK CHUẨN GMAIL)
    // =========================================================
    private class ModernCheckBox extends JCheckBox {
        public ModernCheckBox() { setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = 18; int y = (getHeight() - size) / 2; int x = (getWidth() - size) / 2; 
            if (isSelected()) {
                g2.setColor(TienIchGiaoDien.MAU_CHINH); g2.fillRoundRect(x, y, size, size, 5, 5); 
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 9, x + 8, y + 13); g2.drawLine(x + 8, y + 13, x + 14, y + 5); 
            } else {
                g2.setColor(Color.WHITE); g2.fillRoundRect(x, y, size, size, 5, 5);
                g2.setColor(new Color(180, 185, 195)); g2.drawRoundRect(x, y, size, size, 5, 5);
            }
            g2.dispose();
        }
    }

    // =========================================================
    // PILL MENU COMPONENT NỘI BỘ
    // =========================================================
    private class PillMenu extends JPanel {
        private List<JButton> btns = new ArrayList<>();
        public PillMenu(List<String> items, java.util.function.Consumer<String> onSelect) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0)); setOpaque(false);
            for (String item : items) {
                JButton btn = new JButton(item) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        if (Boolean.TRUE.equals(getClientProperty("active"))) { g2.setColor(TienIchGiaoDien.MAU_CHINH); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); } 
                        else { g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight()); }
                        g2.dispose(); super.paintComponent(g);
                    }
                };
                btn.putClientProperty("active", item.equals(items.get(0)));
                btn.setFont(TienIchGiaoDien.FONT_DAM.deriveFont(12.5f)); btn.setForeground(item.equals(items.get(0)) ? Color.WHITE : TienIchGiaoDien.MAU_CHU_PHU);
                btn.setContentAreaFilled(false); btn.setBorderPainted(false);btn.setFocusPainted(false); btn.setBorder(new EmptyBorder(8, 16, 8, 16)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> { setActiveTab(item); onSelect.accept(item); });
                btns.add(btn); add(btn);
            }
        }
        public void setActiveTab(String name) {
            for (JButton b : btns) { boolean isActive = b.getText().equals(name); b.putClientProperty("active", isActive); b.setForeground(isActive ? Color.WHITE : TienIchGiaoDien.MAU_CHU_PHU); b.repaint(); }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame(); f.setSize(1366, 768); f.setLocationRelativeTo(null);
            f.add(new DanhSachNvUi()); f.setVisible(true);
        });
    }
}