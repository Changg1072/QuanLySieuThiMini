package GUI;

import Dao.TruyVanSieuTocDAO;
import Data.ChiTietLoHang;
import Data.KiemKeKho;
import Data.SanPham;
import Logic.KiemKeLogic;
import Logic.TaoMaTuDongLogic;
import GUI.HoTro.TienIchGiaoDien;
import GUI.HoTro.LoadingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GIAO DIỆN KIỂM KÊ KHO RETAIL HIỆN ĐẠI
 * Đã fix: Lỗi Dropdown Lô Hàng không nảy số Tồn Kho Hệ Thống (Dùng ItemListener chuẩn)
 */
public class KiemKeGUI extends JPanel {

    // ==========================================
    // BẢNG MÀU HIỆN ĐẠI
    // ==========================================
    private final Color MAU_NEN_APP = new Color(245, 247, 250);     
    private final Color MAU_TRANG = new Color(255, 255, 255);       
    private final Color MAU_CHU_CHINH = new Color(31, 41, 55);      
    private final Color MAU_CHU_NHAT = new Color(107, 114, 128);    
    private final Color MAU_XANH_PRIMARY = new Color(59, 130, 246); 
    private final Color MAU_XANH_NHAT = new Color(239, 246, 255);   
    private final Color MAU_XANH_LA = new Color(16, 185, 129);      
    private final Color MAU_DO_NHAT = new Color(254, 226, 226);     
    private final Color MAU_DO_DAM = new Color(239, 68, 68);        
    private final Color MAU_VANG_NHAT = new Color(254, 243, 199);   
    private final Color MAU_VANG_DAM = new Color(245, 158, 11);     
    private final Color MAU_VIEN = new Color(229, 231, 235);        

    private final Color MAU_NEN_TRANG_THAI = new Color(248, 250, 252);
    private final Color MAU_TIM = new Color(168, 85, 247); 
    private final Color MAU_NEN_ICON_XANH = new Color(224, 242, 254);
    private final Color MAU_NEN_ICON_LA = new Color(220, 252, 231);
    private final Color MAU_NEN_ICON_CAM = new Color(255, 237, 213);
    private final Color MAU_CAM_DAM = new Color(249, 115, 22);

    // ==========================================
    // CÁC COMPONENT GIAO DIỆN
    // ==========================================
    private JPanel panelDanhSachSP;
    private JLabel lblHeaderTongSP;
    private JComboBox<String> cboSapXep;
    private JTextField txtTimKiem;
    private JLabel lblTenSPChiTiet, lblMaSPChiTiet, lblAnhSPChiTiet;
    private JComboBox<String> cboLoHang; 
    private JLabel lblTonHeThong, lblDVT1, lblDVT2, lblDVT3, lblChenhLech;
    private JTextField txtKiemDem;
    private PanelBoGoc panelTrangThai;
    private JLabel lblIconTrangThai, lblTextTrangThai;
    private PanelBoGoc panelLyDo;
    private JTextArea txtLyDo;
    private JLabel lblDemKyTu;
    private JProgressBar progressKiemKe;
    private JLabel lblTienDo;
    private JLabel lblTextChenhLechNho; 
    private JLabel lblBadgeTrangThai;
    private CardLayout cardFooter;
    private JPanel pnlFooterContainer;
    private final String CARD_BUTTONS = "BUTTONS";
    private final String CARD_LOADING = "LOADING";
    private TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO duLieuSQL;
    private Map<String, ChiTietKiemKeUI> mapTrangThaiUI = new HashMap<>(); 
    private Map<String, TheSanPhamUI> mapTheSP = new HashMap<>();
    
    private String maSPDangChon = "";
    private String maLoDangChon = "";
    
    private int tongSoLoCanKiem = 0;
    private int tongSoLoDaKiem = 0;
    private String maNhanVienHienTai = "NV001"; 
    private String tenNhanVienHienTai = "Chưa rõ";
    private JLabel lblUserInfo;
    private JLabel lblTongKiemKeSP;
    public void setNhanVienTruyenVao(String maNV, String tenNV) {
        this.maNhanVienHienTai = maNV;
        this.tenNhanVienHienTai = tenNV;
        
        // Cập nhật label nếu đã được khởi tạo
        if (lblUserInfo != null) {
            lblUserInfo.setText("Mã Nhân viên: " + maNV + "  |  " + LocalDate.now().toString());
        }
    }
    private ItemListener loHangListener = e -> {
        // Chỉ thực thi khi một lô hàng mới CHÍNH THỨC được chọn
        if (e.getStateChange() == ItemEvent.SELECTED && cboLoHang.getSelectedIndex() >= 0) {
            hienThiChiTietCuaLoHangDuocChon();
        }
    };
    public KiemKeGUI() {
        thietLapCuaSo();
        taoGiaoDien();
        taiDuLieuTuDatabase(); 
    }

    private void thietLapCuaSo() {
        setBackground(MAU_NEN_APP); // Giữ lại
        setLayout(new BorderLayout(0, 0));
    }

    private void taoGiaoDien() {
        add(taoHeader(), BorderLayout.NORTH);
        
        // Tạo 2 panel
        JPanel sidebar = taoSidebarSanPham();
        JPanel chiTiet = taoPanelChiTiet();
        
        // Dùng JSplitPane để chia 3/7
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, chiTiet);
        splitPane.setOpaque(false);
        splitPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        splitPane.setDividerSize(6);
        splitPane.setResizeWeight(0.32); // 40% bên trái, 60% bên phải
        splitPane.setContinuousLayout(true);
        
        // Đặt divider location sau khi frame hiển thị
        SwingUtilities.invokeLater(() -> {
            int totalWidth = getWidth();
            splitPane.setDividerLocation((int)(totalWidth * 0.32));
        });
        
        add(splitPane, BorderLayout.CENTER);
        add(taoFooter(), BorderLayout.SOUTH);
    }
    // ==========================================
    // KẾT NỐI DATABASE VÀ GIAO DIỆN
    // ==========================================
    private void taiDuLieuTuDatabase() {
        lblHeaderTongSP.setText("Đang tải dữ liệu kho...");
        panelDanhSachSP.removeAll();
        panelDanhSachSP.repaint();

        SwingWorker<TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected TruyVanSieuTocDAO.DuLieuKiemKeSieuTocDTO doInBackground() throws Exception {
                return TruyVanSieuTocDAO.getInstance().loadDuLieuKiemKeSieuToc();
            }

            @Override
            protected void done() {
                try {
                    duLieuSQL = get(); 
                    tongSoLoCanKiem = 0;
                    
                    for (SanPham sp : duLieuSQL.dsSanPham) {
                        List<ChiTietLoHang> dsLo = duLieuSQL.mapDanhSachLo.get(sp.getMaSP());
                        if (dsLo.isEmpty()) {
                            ChiTietLoHang dummyLo = new ChiTietLoHang();
                            dummyLo.setMaSP(sp.getMaSP());
                            dummyLo.setMaLoHang("LOT-DEFAULT");
                            dummyLo.setSoLuongTon(0);
                            dsLo.add(dummyLo);
                        }
                        
                        for (ChiTietLoHang lo : dsLo) {
                            String compositeKey = sp.getMaSP() + "_" + lo.getMaLoHang();
                            mapTrangThaiUI.put(compositeKey, new ChiTietKiemKeUI());
                            tongSoLoCanKiem++;
                        }
                    }

                    lblHeaderTongSP.setText("Danh sách sản phẩm (" + duLieuSQL.dsSanPham.size() + ")");
                    progressKiemKe.setMaximum(tongSoLoCanKiem); 
                    
                    sapXepSanPham(0); 
                    capNhatProgress();
                } catch (Exception e) {
                    // 🔥 THAY JOPTIONPANE BẰNG TIỆN ÍCH GIAO DIỆN
                    TienIchGiaoDien.hienThiThongBao(KiemKeGUI.this, "Lỗi tải dữ liệu: " + e.getMessage(), "ERROR");
                }
            }
        };
        worker.execute();
    }

    // ==========================================
    // CÁC HÀM TẠO GIAO DIỆN
    // ==========================================
    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(MAU_TRANG);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, MAU_VIEN),
            new EmptyBorder(15, 30, 15, 30)
        ));

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlTitle.setOpaque(false);
        JLabel lblIcon = new JLabel("");
        lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        JLabel lblTitle = new JLabel("KIỂM KÊ KHO");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(MAU_CHU_CHINH);
        pnlTitle.add(lblIcon);
        pnlTitle.add(lblTitle);

        JPanel pnlSearch = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        pnlSearch.setOpaque(false);
        txtTimKiem = new JTextField(30);
        txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtTimKiem.setPreferredSize(new Dimension(400, 40));
        txtTimKiem.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MAU_VIEN, 1, true),
            new EmptyBorder(5, 15, 5, 15)
        ));
        txtTimKiem.setText("Tìm sản phẩm...");
        txtTimKiem.setForeground(MAU_CHU_NHAT);
        txtTimKiem.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { if (txtTimKiem.getText().equals("Tìm sản phẩm...")) { txtTimKiem.setText(""); txtTimKiem.setForeground(MAU_CHU_CHINH); } }
            public void focusLost(FocusEvent e) { if (txtTimKiem.getText().isEmpty()) { txtTimKiem.setText("Tìm sản phẩm..."); txtTimKiem.setForeground(MAU_CHU_NHAT); } }
        });
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { timKiemRealtime(); }
            public void removeUpdate(DocumentEvent e) { timKiemRealtime(); }
            public void changedUpdate(DocumentEvent e) { timKiemRealtime(); }
        });
        pnlSearch.add(txtTimKiem);

        JPanel pnlInfo = new JPanel(new GridLayout(2, 1, 0, 5));
        pnlInfo.setOpaque(false);
        lblUserInfo = new JLabel("Mã Nhân viên: " + maNhanVienHienTai + "  |  " + LocalDate.now().toString(), SwingConstants.RIGHT);
        lblUserInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUserInfo.setForeground(MAU_CHU_CHINH);
        
        JPanel pnlProgress = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlProgress.setOpaque(false);
        lblTienDo = new JLabel("Đã kiểm: 0 / 0 Lô");
        lblTienDo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        progressKiemKe = new JProgressBar(0, 100);
        progressKiemKe.setPreferredSize(new Dimension(200, 8));
        progressKiemKe.setForeground(MAU_XANH_LA);
        progressKiemKe.setBackground(MAU_VIEN);
        progressKiemKe.setBorderPainted(false);
        
        pnlProgress.add(lblTienDo);
        pnlProgress.add(progressKiemKe);
        
        pnlInfo.add(lblUserInfo);
        pnlInfo.add(pnlProgress);

        header.add(pnlTitle, BorderLayout.WEST);
        header.add(pnlSearch, BorderLayout.CENTER);
        header.add(pnlInfo, BorderLayout.EAST);

        return header;
    }

    private JPanel taoSidebarSanPham() {
        PanelBoGoc sidebar = new PanelBoGoc(20, MAU_TRANG);
        sidebar.setLayout(new BorderLayout());
        //sidebar.setPreferredSize(new Dimension(650, 0));
        sidebar.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel pnlTop = new JPanel(new BorderLayout(10, 10));
        pnlTop.setOpaque(false);
        pnlTop.setBorder(new EmptyBorder(0, 0, 15, 0));

        lblHeaderTongSP = new JLabel("Đang tải...");
        lblHeaderTongSP.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHeaderTongSP.setForeground(MAU_CHU_CHINH);

        String[] options = {"Theo tên A-Z", "Theo tên Z-A", "Theo mã sản phẩm"};
        cboSapXep = new JComboBox<>(options);
        cboSapXep.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboSapXep.setBackground(MAU_TRANG);
        cboSapXep.setFocusable(false);
        cboSapXep.addActionListener(e -> sapXepSanPham(cboSapXep.getSelectedIndex()));

        pnlTop.add(lblHeaderTongSP, BorderLayout.WEST);
        pnlTop.add(cboSapXep, BorderLayout.EAST);

        panelDanhSachSP = new JPanel();
        panelDanhSachSP.setLayout(new BoxLayout(panelDanhSachSP, BoxLayout.Y_AXIS));
        panelDanhSachSP.setBackground(MAU_TRANG);

        JScrollPane scrollPane = new JScrollPane(panelDanhSachSP);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(MAU_TRANG);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        sidebar.add(pnlTop, BorderLayout.NORTH);
        sidebar.add(scrollPane, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel taoPanelChiTiet() {
        PanelBoGoc pnlChiTiet = new PanelBoGoc(20, MAU_NEN_APP); 
        pnlChiTiet.setLayout(new BorderLayout(0, 20));

        PanelBoGoc cardInfo = new PanelBoGoc(20, MAU_TRANG);
        cardInfo.setLayout(new BorderLayout(20, 0));
        cardInfo.setBorder(new EmptyBorder(20, 30, 20, 30));

        lblAnhSPChiTiet = new JLabel("Ảnh", SwingConstants.CENTER);
        lblAnhSPChiTiet.setPreferredSize(new Dimension(100, 100));
        lblAnhSPChiTiet.setBorder(BorderFactory.createLineBorder(MAU_VIEN));

        JPanel pnlTextInfo = new JPanel(new GridLayout(5, 1, 0, 5));
        pnlTextInfo.setOpaque(false);
        lblTenSPChiTiet = new JLabel("Vui lòng chọn sản phẩm...");
        lblTenSPChiTiet.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTenSPChiTiet.setForeground(MAU_CHU_CHINH);
        
        lblMaSPChiTiet = new JLabel("Mã: ---  |  Loại: ---  |  ĐVT: ---");
        lblMaSPChiTiet.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblMaSPChiTiet.setForeground(MAU_CHU_NHAT);
        
        lblTongKiemKeSP = new JLabel("Tổng kiểm đếm: -- / --");
        lblTongKiemKeSP.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblTongKiemKeSP.setForeground(MAU_XANH_PRIMARY);

        JPanel pnlChonLo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlChonLo.setOpaque(false);
        JLabel lblLo = new JLabel("Lô Đang Đếm:  ");
        lblLo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblLo.setForeground(MAU_XANH_PRIMARY);
        
        cboLoHang = new JComboBox<>();
        cboLoHang.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cboLoHang.setPreferredSize(new Dimension(280, 30));
        cboLoHang.setBackground(MAU_TRANG);
        
        cboLoHang.addItemListener(loHangListener);
        
        pnlChonLo.add(lblLo);
        pnlChonLo.add(cboLoHang);

        pnlTextInfo.add(lblTenSPChiTiet);
        pnlTextInfo.add(lblMaSPChiTiet);
        pnlTextInfo.add(lblTongKiemKeSP);
        pnlTextInfo.add(pnlChonLo);
        
        cardInfo.add(lblAnhSPChiTiet, BorderLayout.WEST);
        cardInfo.add(pnlTextInfo, BorderLayout.CENTER);

        JPanel pnlThongKe = new JPanel(new GridLayout(1, 3, 20, 0));
        pnlThongKe.setOpaque(false);

        PanelBoGoc cardTon = taoCardThongKe("TỒN KHO LÔ NÀY", "📦", MAU_XANH_PRIMARY, MAU_NEN_ICON_XANH, MAU_XANH_NHAT);
        lblTonHeThong = taoLabelSoLieu("0", MAU_XANH_PRIMARY);
        lblDVT1 = taoLabelDVT("Gói");
        
        JPanel pnlCenterTon = new JPanel(new GridBagLayout()); 
        pnlCenterTon.setOpaque(false);
        pnlCenterTon.add(lblTonHeThong);
        
        JPanel pnlWrapTon = new JPanel(new BorderLayout());
        pnlWrapTon.setOpaque(false);
        pnlWrapTon.add(pnlCenterTon, BorderLayout.CENTER);
        pnlWrapTon.add(lblDVT1, BorderLayout.SOUTH);
        cardTon.add(pnlWrapTon, BorderLayout.CENTER);

        PanelBoGoc cardKiem = taoCardThongKe("KIỂM ĐẾM THỰC TẾ", "📋", MAU_XANH_LA, MAU_NEN_ICON_LA, new Color(244, 253, 248));
        
        PanelBoGoc pnlSpinner = new PanelBoGoc(15, MAU_TRANG);
        pnlSpinner.setLayout(new BorderLayout());
        pnlSpinner.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        pnlSpinner.setPreferredSize(new Dimension(240, 85));

        JButton btnMinus = taoNutSpinner("-");
        JButton btnPlus = taoNutSpinner("+");
        btnMinus.addActionListener(e -> tangGiamSoLuong(-1));
        btnPlus.addActionListener(e -> tangGiamSoLuong(1));

        txtKiemDem = new JTextField();
        txtKiemDem.setFont(new Font("Segoe UI", Font.BOLD, 50));
        txtKiemDem.setHorizontalAlignment(JTextField.CENTER);
        txtKiemDem.setBorder(new EmptyBorder(5, 10, 5, 10)); 
        txtKiemDem.setOpaque(false);
        txtKiemDem.setEnabled(false);
        txtKiemDem.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tinhChenhLech(); }
            public void removeUpdate(DocumentEvent e) { tinhChenhLech(); }
            public void changedUpdate(DocumentEvent e) { tinhChenhLech(); }
        });
        txtKiemDem.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) { if (!Character.isDigit(e.getKeyChar())) e.consume(); }
        });

        pnlSpinner.add(btnMinus, BorderLayout.WEST);
        pnlSpinner.add(txtKiemDem, BorderLayout.CENTER);
        pnlSpinner.add(btnPlus, BorderLayout.EAST);
        
        JPanel pnlCenterKiem = new JPanel(new GridBagLayout()); 
        pnlCenterKiem.setOpaque(false);
        pnlCenterKiem.add(pnlSpinner);

        lblDVT2 = taoLabelDVT("Gói");
        JPanel pnlWrapKiem = new JPanel(new BorderLayout());
        pnlWrapKiem.setOpaque(false);
        pnlWrapKiem.add(pnlCenterKiem, BorderLayout.CENTER);
        pnlWrapKiem.add(lblDVT2, BorderLayout.SOUTH);
        cardKiem.add(pnlWrapKiem, BorderLayout.CENTER);

        PanelBoGoc cardLech = taoCardThongKe("CHÊNH LỆCH KHO", "⚖", MAU_CAM_DAM, MAU_NEN_ICON_CAM, new Color(248, 250, 252));
        lblChenhLech = taoLabelSoLieu("0", MAU_XANH_LA);
        lblTextChenhLechNho = new JLabel("Khớp", SwingConstants.CENTER);
        lblTextChenhLechNho.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTextChenhLechNho.setForeground(MAU_XANH_LA);
        lblDVT3 = taoLabelDVT("Gói");

        FlowLayout fl = new FlowLayout(FlowLayout.CENTER, 10, 0);
        fl.setAlignOnBaseline(true); 
        
        JPanel pnlSoVaChu = new JPanel(fl);
        pnlSoVaChu.setOpaque(false);
        pnlSoVaChu.add(lblChenhLech);
        pnlSoVaChu.add(lblTextChenhLechNho);
        
        JPanel pnlCenterLech = new JPanel(new GridBagLayout()); 
        pnlCenterLech.setOpaque(false);
        pnlCenterLech.add(pnlSoVaChu);
        
        JPanel pnlWrapLech = new JPanel(new BorderLayout());
        pnlWrapLech.setOpaque(false);
        pnlWrapLech.add(pnlCenterLech, BorderLayout.CENTER);
        pnlWrapLech.add(lblDVT3, BorderLayout.SOUTH);
        cardLech.add(pnlWrapLech, BorderLayout.CENTER);

        pnlThongKe.add(cardTon);
        pnlThongKe.add(cardKiem);
        pnlThongKe.add(cardLech);

        JPanel pnlBottom = new JPanel(new BorderLayout(0, 15));
        pnlBottom.setOpaque(false);

        panelTrangThai = new PanelBoGoc(12, new Color(248, 250, 252));
        panelTrangThai.setLayout(new BorderLayout(15, 0));
        panelTrangThai.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        lblIconTrangThai = taoIconTron("i", MAU_CHU_NHAT);
        lblIconTrangThai.setBackground(MAU_VIEN);
        
        JPanel pnlTextTrangThai = new JPanel(new GridLayout(2, 1));
        pnlTextTrangThai.setOpaque(false);
        JLabel lblTitleTrangThai = new JLabel("TRẠNG THÁI KIỂM KÊ (CỦA LÔ NÀY)");
        lblTitleTrangThai.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitleTrangThai.setForeground(MAU_CHU_NHAT);
        
        lblTextTrangThai = new JLabel("Chưa kiểm đếm");
        lblTextTrangThai.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTextTrangThai.setForeground(MAU_CHU_CHINH);
        
        pnlTextTrangThai.add(lblTitleTrangThai);
        pnlTextTrangThai.add(lblTextTrangThai);

        lblBadgeTrangThai = new JLabel("CHỜ ĐẾM", SwingConstants.CENTER);
        lblBadgeTrangThai.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBadgeTrangThai.setOpaque(true);
        lblBadgeTrangThai.setBackground(MAU_VIEN);
        lblBadgeTrangThai.setForeground(MAU_CHU_NHAT);
        lblBadgeTrangThai.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MAU_VIEN, 1, true),
            new EmptyBorder(5, 15, 5, 15)
        ));
        
        panelTrangThai.add(lblIconTrangThai, BorderLayout.WEST);
        panelTrangThai.add(pnlTextTrangThai, BorderLayout.CENTER);
        panelTrangThai.add(lblBadgeTrangThai, BorderLayout.EAST);

        panelLyDo = new PanelBoGoc(12, MAU_TRANG);
        panelLyDo.setLayout(new BorderLayout(5, 10));
        panelLyDo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MAU_VIEN, 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        JPanel pnlTitleLyDo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTitleLyDo.setOpaque(false);
        JLabel lblIconLyDo = taoIconTron("📄", MAU_TIM);
        lblIconLyDo.setBackground(new Color(243, 232, 255));
        lblIconLyDo.setPreferredSize(new Dimension(28, 28));
        lblIconLyDo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel lblTitleLyDo = new JLabel("LÝ DO CHÊNH LỆCH (nếu có)");
        lblTitleLyDo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitleLyDo.setForeground(MAU_TIM); 
        pnlTitleLyDo.add(lblIconLyDo);
        pnlTitleLyDo.add(lblTitleLyDo);
        
        txtLyDo = new JTextArea(3, 20);
        txtLyDo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtLyDo.setLineWrap(true);
        txtLyDo.setWrapStyleWord(true);
        txtLyDo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MAU_VIEN, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        txtLyDo.setText("Nhập lý do chênh lệch nếu có...");
        txtLyDo.setForeground(MAU_CHU_NHAT);
        txtLyDo.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { if (txtLyDo.getText().equals("Nhập lý do chênh lệch nếu có...")) { txtLyDo.setText(""); txtLyDo.setForeground(MAU_CHU_CHINH); } }
            public void focusLost(FocusEvent e) { if (txtLyDo.getText().isEmpty()) { txtLyDo.setText("Nhập lý do chênh lệch nếu có..."); txtLyDo.setForeground(MAU_CHU_NHAT); } }
        });
        
        lblDemKyTu = new JLabel("0/200", SwingConstants.RIGHT);
        lblDemKyTu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDemKyTu.setForeground(MAU_CHU_NHAT);
        txtLyDo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { capNhatDemKyTu(); }
            public void removeUpdate(DocumentEvent e) { capNhatDemKyTu(); }
            public void changedUpdate(DocumentEvent e) { capNhatDemKyTu(); }
        });

        panelLyDo.add(pnlTitleLyDo, BorderLayout.NORTH);
        panelLyDo.add(new JScrollPane(txtLyDo), BorderLayout.CENTER);
        panelLyDo.add(lblDemKyTu, BorderLayout.SOUTH);

        pnlBottom.add(panelTrangThai, BorderLayout.NORTH);
        pnlBottom.add(panelLyDo, BorderLayout.CENTER);

        pnlChiTiet.add(cardInfo, BorderLayout.NORTH);
        pnlChiTiet.add(pnlThongKe, BorderLayout.CENTER);
        pnlChiTiet.add(pnlBottom, BorderLayout.SOUTH);

        return pnlChiTiet;
    }

    private JPanel taoFooter() {
        cardFooter = new CardLayout();
        pnlFooterContainer = new JPanel(cardFooter);
        pnlFooterContainer.setOpaque(false);

        // --- CARD 1: DÀN NÚT BẤM ---
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        pnlButtons.setBackground(MAU_TRANG);
        pnlButtons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MAU_VIEN));

        NutBamHienDai btnLichSu = new NutBamHienDai("Xem Lịch Sử", MAU_CHU_NHAT, MAU_TRANG);
        NutBamHienDai btnLuu = new NutBamHienDai("LƯU LÔ NÀY", MAU_XANH_PRIMARY, MAU_TRANG);
        NutBamHienDai btnTiepTheo = new NutBamHienDai("Lô Hoặc SP Tiếp Theo", MAU_XANH_PRIMARY, MAU_TRANG);
        btnTiepTheo.setPreferredSize(new Dimension(220, 42)); 
        NutBamHienDai btnHoanTat = new NutBamHienDai("NỘP KẾT QUẢ KIỂM KÊ", MAU_XANH_LA, MAU_TRANG);
        btnHoanTat.setPreferredSize(new Dimension(280, 42)); 

        btnLuu.addActionListener(e -> luuKiemKeLoHienTai());
        btnTiepTheo.addActionListener(e -> chuyenSangLoHoacSanPhamTiepTheo());
        btnHoanTat.addActionListener(e -> dongBoDatabase()); 

        pnlButtons.add(btnLichSu);
        pnlButtons.add(btnLuu);
        pnlButtons.add(btnTiepTheo);
        pnlButtons.add(btnHoanTat);

        // --- CARD 2: ANIMATION LOADING (Giống ThanhToanUi) ---
        JPanel pnlLoading = new JPanel(new BorderLayout(0, 5));
        pnlLoading.setBackground(MAU_TRANG);
        pnlLoading.setBorder(new EmptyBorder(10, 30, 10, 30));
        
        JLabel lblLoadingText = new JLabel("HỆ THỐNG ĐANG XỬ LÝ ĐỒNG BỘ DỮ LIỆU...", SwingConstants.CENTER);
        lblLoadingText.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblLoadingText.setForeground(MAU_XANH_PRIMARY);

        JProgressBar progressNopPhieu = new JProgressBar();
        progressNopPhieu.setIndeterminate(true);
        progressNopPhieu.setPreferredSize(new Dimension(0, 8)); 
        progressNopPhieu.setBorder(null);
        progressNopPhieu.setBackground(MAU_VIEN);

        // Custom UI cho ProgressBar (Hiệu ứng dải màu chạy qua lại)
        progressNopPhieu.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintIndeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(); int h = c.getHeight();
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(MAU_XANH_PRIMARY); 
                long time = System.currentTimeMillis() / 4; 
                int x = (int) (time % (w + 150)) - 150;
                g2.fillRoundRect(x, 0, 120, h, h, h);
                g2.dispose();
                c.repaint();
            }
        });

        pnlLoading.add(lblLoadingText, BorderLayout.CENTER);
        pnlLoading.add(progressNopPhieu, BorderLayout.SOUTH);

        // Gom vào Container
        pnlFooterContainer.add(pnlButtons, CARD_BUTTONS);
        pnlFooterContainer.add(pnlLoading, CARD_LOADING);

        return pnlFooterContainer;
    }
    private void capNhatTongKiemDeSP() {
        if (maSPDangChon.isEmpty()) return;
        
        int tongTonHT = duLieuSQL.mapTongTonKho.getOrDefault(maSPDangChon, 0);
        int tongThucTe = 0;
        
        // Lấy danh sách các lô của sản phẩm này
        List<ChiTietLoHang> dsLo = duLieuSQL.mapDanhSachLo.get(maSPDangChon);
        for (ChiTietLoHang lo : dsLo) {
            ChiTietKiemKeUI ct = mapTrangThaiUI.get(maSPDangChon + "_" + lo.getMaLoHang());
            if (ct != null && ct.daKiem) {
                tongThucTe += ct.soLuongThucTe;
            } else {
                // Nếu lô chưa đếm, ta coi như thực tế đang bằng 0 (hoặc có thể cộng dồn theo ý cậu)
                tongThucTe += 0; 
            }
        }
        
        lblTongKiemKeSP.setText("Tổng kiểm đếm: " + tongThucTe + " / " + tongTonHT + " (Toàn bộ lô)");
        
        // Đổi màu để nhấn mạnh cho nhân viên biết
        if (tongThucTe == tongTonHT) {
            lblTongKiemKeSP.setText("⭐ Tổng kiểm đếm: " + tongThucTe + " / " + tongTonHT + " (Khớp tổng kho)");
            lblTongKiemKeSP.setForeground(MAU_XANH_LA);
        } else {
            lblTongKiemKeSP.setForeground(MAU_XANH_PRIMARY);
        }
    }
    // ==========================================
    // LOGIC NGHIỆP VỤ & LƯU DB THỰC TẾ
    // ==========================================

    private void hienThiDanhSachSanPham(String tuKhoa) {
        if (duLieuSQL == null) return;
        panelDanhSachSP.removeAll();
        mapTheSP.clear();

        for (SanPham sp : duLieuSQL.dsSanPham) {
            boolean match = tuKhoa.isEmpty() || sp.getTenSP().toLowerCase().contains(tuKhoa.toLowerCase());
            if (match) {
                List<ChiTietLoHang> dsLo = duLieuSQL.mapDanhSachLo.get(sp.getMaSP());
                int tongTonSP = duLieuSQL.mapTongTonKho.getOrDefault(sp.getMaSP(), 0);
                
                String tinhTrangSP = "✔ Khớp toàn bộ lô";
                Color mauTinhTrang = MAU_XANH_PRIMARY;
                
                int soLoDaKiem = 0;
                boolean coLech = false;
                
                for(ChiTietLoHang lo : dsLo) {
                    ChiTietKiemKeUI ct = mapTrangThaiUI.get(sp.getMaSP() + "_" + lo.getMaLoHang());
                    if(ct.daKiem) {
                        soLoDaKiem++;
                        if(ct.soLuongThucTe != lo.getSoLuongTon()) coLech = true;
                    }
                }
                
                if (soLoDaKiem == 0) {
                    tinhTrangSP = "Chưa kiểm lô nào"; mauTinhTrang = MAU_CHU_NHAT;
                } else if (soLoDaKiem < dsLo.size()) {
                    tinhTrangSP = "Đang kiểm dở (" + soLoDaKiem + "/" + dsLo.size() + ")"; mauTinhTrang = MAU_VANG_DAM;
                } else if (coLech) {
                    tinhTrangSP = "⚠ Lệch kho"; mauTinhTrang = MAU_DO_DAM;
                }

                TheSanPhamUI theUI = new TheSanPhamUI(sp, tongTonSP, dsLo.size(), tinhTrangSP, mauTinhTrang);
                theUI.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) { chonSanPham(sp.getMaSP()); }
                });

                panelDanhSachSP.add(theUI);
                panelDanhSachSP.add(Box.createRigidArea(new Dimension(0, 10))); 
                mapTheSP.put(sp.getMaSP(), theUI);
            }
        }
        panelDanhSachSP.revalidate();
        panelDanhSachSP.repaint();
    }

    private void sapXepSanPham(int kieu) {
        if (duLieuSQL == null) return;
        Collections.sort(duLieuSQL.dsSanPham, (sp1, sp2) -> {
            switch (kieu) {
                case 0: return sp1.getTenSP().compareToIgnoreCase(sp2.getTenSP());
                case 1: return sp2.getTenSP().compareToIgnoreCase(sp1.getTenSP());
                case 2: return sp1.getMaSP().compareToIgnoreCase(sp2.getMaSP());
                default: return 0;
            }
        });
        hienThiDanhSachSanPham(txtTimKiem.getText().equals("Tìm sản phẩm...") ? "" : txtTimKiem.getText());
    }

    private void timKiemRealtime() {
        String text = txtTimKiem.getText().trim();
        if (text.equals("Tìm sản phẩm...")) text = "";
        hienThiDanhSachSanPham(text);
    }

    private void chonSanPham(String maSP) {
        if (!maSPDangChon.isEmpty() && mapTheSP.containsKey(maSPDangChon)) {
            mapTheSP.get(maSPDangChon).setHighlight(false);
        }
        
        maSPDangChon = maSP;
        
        TheSanPhamUI theUI = mapTheSP.get(maSP);
        if (theUI != null) theUI.setHighlight(true);
        capNhatThongTinSanPham(maSP);
    }

    private void capNhatThongTinSanPham(String maSP) {
        SanPham sp = duLieuSQL.dsSanPham.stream()
            .filter(s -> s.getMaSP().equals(maSP)).findFirst().orElse(null);
        if (sp == null) return;

        lblTenSPChiTiet.setText(sp.getTenSP());
        lblMaSPChiTiet.setText("Mã: " + sp.getMaSP() + "  |  Loại: " + sp.getMaLoai() + "  |  ĐVT: " + sp.getDonViTinh());
        lblDVT1.setText(sp.getDonViTinh()); 
        lblDVT2.setText(sp.getDonViTinh()); 
        lblDVT3.setText(sp.getDonViTinh());

        // 1. Tắt listener
        cboLoHang.removeItemListener(loHangListener);
        cboLoHang.removeAllItems();
        
        List<ChiTietLoHang> dsLo = duLieuSQL.mapDanhSachLo.get(maSP);
        for (ChiTietLoHang lo : dsLo) {
            String moTaLo = lo.getMaLoHang().equals("LOT-DEFAULT")
                ? "Lô Mặc Định (Tồn: 0)"
                : "Lô: " + lo.getMaLoHang() + " (HSD: " + (lo.getHSD() != null ? lo.getHSD().toString() : "N/A") + ")";
            cboLoHang.addItem(moTaLo);
        }
        
        // 2. Gắn listener lại
        cboLoHang.addItemListener(loHangListener);
        
        if (cboLoHang.getItemCount() > 0) {
            // 3. setSelectedIndex(-1) trước để ép Swing luôn fire event dù index 0 → 0
            cboLoHang.setSelectedIndex(-1);
            cboLoHang.setSelectedIndex(0); // → fire ItemListener → hienThiChiTietCuaLoHangDuocChon()
        }
        
        // 👉 GỌI Ở ĐÂY NÈ: Cập nhật tổng số lượng kiểm đếm của toàn bộ lô mỗi khi click chọn sản phẩm khác
        capNhatTongKiemDeSP();
    }
    
    // 🔥 ĐÃ FIX: Hàm này giờ sẽ chắc chắn ép thay đổi số TỒN HỆ THỐNG
    private void hienThiChiTietCuaLoHangDuocChon() {
        int idx = cboLoHang.getSelectedIndex();
        if(idx < 0) return;
        
        ChiTietLoHang lo = duLieuSQL.mapDanhSachLo.get(maSPDangChon).get(idx);
        maLoDangChon = lo.getMaLoHang();
        
        // 1. CẬP NHẬT TỒN KHO HỆ THỐNG CỦA LÔ ĐANG CHỌN
        int tonKhoLo = lo.getSoLuongTon();
        System.out.println("TEST -> Chọn Lô: " + maLoDangChon + " | Tồn kho thực của Lô: " + tonKhoLo);
        lblTonHeThong.setText(String.valueOf(tonKhoLo));
        lblTonHeThong.repaint();
        
        ChiTietKiemKeUI ct = mapTrangThaiUI.get(maSPDangChon + "_" + maLoDangChon);
        
        txtKiemDem.setEnabled(true);
        if (ct.daKiem) {
            // Load lại dữ liệu thực tế đã đếm của lô này
            txtKiemDem.setText(String.valueOf(ct.soLuongThucTe));
            txtLyDo.setText(ct.lyDo);
            txtLyDo.setForeground(MAU_CHU_CHINH);
            
            // 🔥 QUAN TRỌNG NHẤT: Bắt buộc gọi lại hàm tính chênh lệch 
            // để số chênh lệch cập nhật chuẩn xác với Tồn Hệ Thống mới
            tinhChenhLech(); 
        } else {
            // Lô chưa đếm: Dọn dẹp sạch sẽ UI
            txtKiemDem.setText("");
            txtLyDo.setText("Nhập lý do chênh lệch nếu có...");
            txtLyDo.setForeground(MAU_CHU_NHAT);
            
            lblChenhLech.setText("--");
            lblTextChenhLechNho.setText("");
            lblChenhLech.setForeground(MAU_CHU_CHINH);
            
            capNhatTrangThaiUI("Chưa kiểm đếm lô này", "i", MAU_VIEN, "CHƯA KIỂM", MAU_VIEN, MAU_CHU_NHAT);
        }
        txtKiemDem.requestFocus();
    }
    private void tangGiamSoLuong(int amount) {
        if (!txtKiemDem.isEnabled()) return;
        try {
            int current = txtKiemDem.getText().isEmpty() ? 0 : Integer.parseInt(txtKiemDem.getText());
            int newVal = current + amount;
            if (newVal >= 0) txtKiemDem.setText(String.valueOf(newVal));
        } catch (Exception e) {}
    }

    private void tinhChenhLech() {
        if (!txtKiemDem.isEnabled() || maSPDangChon.isEmpty()) return;
        try {
            int tonHT = Integer.parseInt(lblTonHeThong.getText());
            String text = txtKiemDem.getText().trim();
            if (text.isEmpty()) {
                lblChenhLech.setText("--");
                lblTextChenhLechNho.setText("");
                capNhatTrangThaiUI("Đang chờ nhập...", "i", MAU_VIEN, "CHỜ", MAU_NEN_APP, MAU_CHU_NHAT);
                
                // 👉 GỌI Ở ĐÂY NÈ (Cập nhật lại tổng khi ô nhập liệu bị xóa trắng)
                capNhatTongKiemDeSP(); 
                
                return;
            }

            int thucTe = Integer.parseInt(text);
            int lech = thucTe - tonHT;

            if (lech == 0) {
                lblChenhLech.setText("0");
                lblChenhLech.setForeground(MAU_XANH_LA);
                lblTextChenhLechNho.setText("Khớp");
                lblTextChenhLechNho.setForeground(MAU_XANH_LA);
                capNhatTrangThaiUI("Lô này khớp tồn kho", "✔", MAU_XANH_PRIMARY, "✔ KHỚP", MAU_NEN_ICON_LA, MAU_XANH_LA);
            } else if (lech > 0) {
                lblChenhLech.setText("+" + lech);
                lblChenhLech.setForeground(MAU_CAM_DAM);
                lblTextChenhLechNho.setText("Dư");
                lblTextChenhLechNho.setForeground(MAU_CAM_DAM);
                capNhatTrangThaiUI("Lô này đang dư " + lech, "⚠", MAU_CAM_DAM, "⚠ DƯ", MAU_NEN_ICON_CAM, MAU_CAM_DAM);
            } else {
                lblChenhLech.setText(String.valueOf(Math.abs(lech))); 
                lblChenhLech.setForeground(MAU_DO_DAM); 
                lblTextChenhLechNho.setText("Thiếu");
                lblTextChenhLechNho.setForeground(MAU_DO_DAM);
                capNhatTrangThaiUI("Lô này đang thiếu " + Math.abs(lech), "!", MAU_DO_DAM, "⚠ THIẾU", MAU_DO_NHAT, MAU_DO_DAM);
            }
            
            // 👉 VÀ GỌI Ở ĐÂY NỮA NÈ (Cập nhật tổng ngay khi gõ số liệu mới)
            capNhatTongKiemDeSP();

        } catch (NumberFormatException ex) {}
    }

    private void capNhatTrangThaiUI(String text, String icon, Color iconBgColor, String badgeText, Color badgeBg, Color badgeFg) {
        lblTextTrangThai.setText(text);
        lblIconTrangThai.setText(icon);
        lblIconTrangThai.setBackground(iconBgColor);
        lblIconTrangThai.setForeground(Color.WHITE);
        lblIconTrangThai.repaint();
        lblBadgeTrangThai.setText(badgeText);
        lblBadgeTrangThai.setBackground(badgeBg);
        lblBadgeTrangThai.setForeground(badgeFg);
        lblBadgeTrangThai.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(badgeBg, 1, true),
            new EmptyBorder(5, 15, 5, 15)
        ));
    }

    private void capNhatDemKyTu() {
        int len = txtLyDo.getText().length();
        if (txtLyDo.getText().equals("Nhập lý do chênh lệch nếu có...")) len = 0;
        
        lblDemKyTu.setText(len + "/200");
        if (len > 200) lblDemKyTu.setForeground(MAU_DO_DAM);
        else lblDemKyTu.setForeground(MAU_CHU_NHAT);
    }

    private void luuKiemKeLoHienTai() {
        if (maSPDangChon.isEmpty() || maLoDangChon.isEmpty()) {
            // 🔥 THAY JOPTIONPANE
            TienIchGiaoDien.hienThiThongBao(this, "Vui lòng chọn sản phẩm và Lô hàng trước!", "WARNING"); 
            return;
        }
        if (txtKiemDem.getText().isEmpty()) {
            // 🔥 THAY JOPTIONPANE
            TienIchGiaoDien.hienThiThongBao(this, "Bạn chưa nhập số lượng đếm thực tế!", "WARNING"); 
            return;
        }

        int tonHT = Integer.parseInt(lblTonHeThong.getText());
        int thucTe = Integer.parseInt(txtKiemDem.getText());
        int lech = thucTe - tonHT;

        String lyDo = txtLyDo.getText().trim();
        if (lyDo.equals("Nhập lý do chênh lệch nếu có...")) lyDo = "";

        if (lech != 0 && lyDo.isEmpty()) {
            // 🔥 THAY JOPTIONPANE
            TienIchGiaoDien.hienThiThongBao(this, "Hệ thống phát hiện có chênh lệch.<br>Vui lòng nhập lý do!", "WARNING"); 
            return;
        }

        ChiTietKiemKeUI ct = mapTrangThaiUI.get(maSPDangChon + "_" + maLoDangChon);
        if (!ct.daKiem) {
            ct.daKiem = true;
            tongSoLoDaKiem++;
            capNhatProgress();
        }
        ct.soLuongThucTe = thucTe;
        ct.lyDo = lyDo;

        hienThiDanhSachSanPham(txtTimKiem.getText().equals("Tìm sản phẩm...") ? "" : txtTimKiem.getText());
        
        TheSanPhamUI theUI = mapTheSP.get(maSPDangChon);
        if (theUI != null) theUI.setHighlight(true);

        chuyenSangLoHoacSanPhamTiepTheo();
    }

    private void chuyenSangLoHoacSanPhamTiepTheo() {
        int idxLoHienTai = cboLoHang.getSelectedIndex();
        if(idxLoHienTai < cboLoHang.getItemCount() - 1) {
            cboLoHang.setSelectedIndex(idxLoHienTai + 1);
            return;
        }
        
        for (SanPham sp : duLieuSQL.dsSanPham) {
            List<ChiTietLoHang> dsLo = duLieuSQL.mapDanhSachLo.get(sp.getMaSP());
            for(ChiTietLoHang lo : dsLo) {
                ChiTietKiemKeUI ct = mapTrangThaiUI.get(sp.getMaSP() + "_" + lo.getMaLoHang());
                if (!ct.daKiem && !sp.getMaSP().equals(maSPDangChon)) {
                    chonSanPham(sp.getMaSP());
                    return;
                }
            }
        }
        // 🔥 THAY JOPTIONPANE
        TienIchGiaoDien.hienThiThongBao(this, "Tuyệt vời! Bạn đã đếm xong toàn bộ TẤT CẢ CÁC LÔ của tất cả Sản Phẩm.<br>Hãy ấn nút <b>NỘP PHIẾU</b> để đẩy kết quả lên hệ thống.", "SUCCESS");
    }

    private void capNhatProgress() {
        if(duLieuSQL == null) return;
        lblTienDo.setText("Đã kiểm: " + tongSoLoDaKiem + " / " + tongSoLoCanKiem + " Lô");
        progressKiemKe.setValue(tongSoLoDaKiem);
    }

    private void dongBoDatabase() {
        if (tongSoLoDaKiem == 0) {
            TienIchGiaoDien.hienThiThongBao(this, "Bạn chưa kiểm đếm bất kỳ lô hàng nào!", "WARNING");
            return;
        }

        TienIchGiaoDien.hienThiXacNhan(this, 
            "Bạn có chắc chắn muốn nộp " + tongSoLoDaKiem + " phiếu kiểm kê lên hệ thống?", 
            () -> {
                // 1. Hiển thị thanh Loading tại Footer
                cardFooter.show(pnlFooterContainer, CARD_LOADING);
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // 2. Chạy xử lý ngầm (SwingWorker)
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    private List<KiemKeKho> tatCaPhieuChoLuu = new ArrayList<>();
                    private int soSPSauCung = 0;
                    private boolean isSuccess = false;
                    private String errorMsg = "";

                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            KiemKeLogic logic = KiemKeLogic.getInstance(); 
                            Map<String, List<KiemKeKho>> mapGomNhomTheoSP = new HashMap<>();

                            // Gom nhóm & Khởi tạo phiếu (Giữ nguyên logic batch của cậu)
                            for (Map.Entry<String, ChiTietKiemKeUI> entry : mapTrangThaiUI.entrySet()) {
                                ChiTietKiemKeUI uiData = entry.getValue();
                                if (uiData.daKiem) {
                                    String[] parts = entry.getKey().split("_");
                                    String maSP = parts[0];
                                    String maLo = parts[1];
                                    int tonHTCuaLo = duLieuSQL.mapDanhSachLo.get(maSP).stream()
                                        .filter(l -> l.getMaLoHang().equals(maLo)).findFirst().get().getSoLuongTon();

                                    KiemKeKho kk = new KiemKeKho.ThoXayKiemKeKho()
                                        .ganMaKiemKe("CHUA_CO_MA").ganMaNV(maNhanVienHienTai)
                                        .ganMaLoHang(maLo).ganMaSP(maSP)
                                        .ganSoLuongHeThong(tonHTCuaLo).ganSoLuongThucTe(uiData.soLuongThucTe)
                                        .ganLyDo(uiData.lyDo).taoMoi();
                                    mapGomNhomTheoSP.computeIfAbsent(maSP, k -> new ArrayList<>()).add(kk);
                                }
                            }

                            for (List<KiemKeKho> dsPhieu : mapGomNhomTheoSP.values()) {
                                logic.xuLyBuTruCheoTruocKhiLuu(dsPhieu);
                                for (KiemKeKho kk : dsPhieu) {
                                    kk.setMaKiemKe(TaoMaTuDongLogic.taoMaKiemKe());
                                    if (kk.getLyDo() != null && kk.getLyDo().contains("🔄 Nhận")) soSPSauCung++;
                                    tatCaPhieuChoLuu.add(kk);
                                }
                            }

                            // 🔥 GỌI DAO LƯU SIÊU TỐC (BATCH)
                            TruyVanSieuTocDAO.getInstance().dongBoKiemKeGopChungSieuToc(tatCaPhieuChoLuu);
                            isSuccess = true;
                        } catch (Exception ex) {
                            errorMsg = ex.getMessage();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        // 3. Xong việc -> Trả lại Footer bình thường
                        setCursor(Cursor.getDefaultCursor());
                        cardFooter.show(pnlFooterContainer, CARD_BUTTONS);

                        if (isSuccess) {
                            String msg = "Đồng bộ thành công " + tatCaPhieuChoLuu.size() + " phiếu!";
                            if (soSPSauCung > 0) msg += "<br><span style='color:#D97706;'>✨ Đã tự động bù trừ sai sót xếp nhầm lô.</span>";
                            TienIchGiaoDien.hienThiThongBao(KiemKeGUI.this, msg, "SUCCESS");
                            taiDuLieuTuDatabase(); // Load lại kho mới
                        } else {
                            TienIchGiaoDien.hienThiThongBao(KiemKeGUI.this, "Lỗi: " + errorMsg, "ERROR");
                        }
                    }
                };
                worker.execute();
            }
        );
    }
    // ==========================================
    // CLASS BỔ TRỢ & CUSTOM COMPONENTS
    // ==========================================

    private class ChiTietKiemKeUI {
        boolean daKiem = false;
        int soLuongThucTe = 0;
        String lyDo = "";
    }

    private class TheSanPhamUI extends PanelBoGoc {
        private boolean isHighlighted = false;

        public TheSanPhamUI(SanPham sp, int tongTonHT, int soLuongLo, String textTrangThai, Color colorTrangThai) {
            super(15, MAU_TRANG);
            setLayout(new BorderLayout(15, 0));
            
            setBorder(new EmptyBorder(12, 12, 12, 25));
            setMaximumSize(new Dimension(800, 85));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel lblImg = new JLabel("Ảnh", SwingConstants.CENTER);
            lblImg.setPreferredSize(new Dimension(50, 50));
            lblImg.setBorder(BorderFactory.createLineBorder(MAU_VIEN));

            JPanel pnlInfo = new JPanel(new GridLayout(2, 1));
            pnlInfo.setOpaque(false);
            JLabel lblTen = new JLabel(sp.getTenSP());
            lblTen.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblTen.setForeground(MAU_CHU_CHINH);
            
            JLabel lblTon = new JLabel("Tổng tồn: " + tongTonHT + "  |  Gồm: " + soLuongLo + " Lô");
            lblTon.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblTon.setForeground(MAU_CHU_NHAT);
            
            pnlInfo.add(lblTen); pnlInfo.add(lblTon);

            JLabel lblTrangThai = new JLabel(textTrangThai, SwingConstants.CENTER);
            lblTrangThai.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblTrangThai.setOpaque(true);
            lblTrangThai.setBorder(new EmptyBorder(4, 10, 4, 10));
            lblTrangThai.setBackground(MAU_NEN_APP);
            lblTrangThai.setForeground(colorTrangThai);

            JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            pnlRight.setOpaque(false);
            pnlRight.add(lblTrangThai);

            add(lblImg, BorderLayout.WEST);
            add(pnlInfo, BorderLayout.CENTER);
            add(pnlRight, BorderLayout.EAST);
        }

        public void setHighlight(boolean highlight) {
            this.isHighlighted = highlight;
            setBackground(highlight ? MAU_XANH_NHAT : MAU_TRANG);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(highlight ? MAU_XANH_PRIMARY : MAU_VIEN, 1, true),
                new EmptyBorder(11, 11, 11, 25) 
            ));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!isHighlighted) {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { if (!isHighlighted) setBackground(MAU_NEN_APP); }
                    public void mouseExited(MouseEvent e) { if (!isHighlighted) setBackground(MAU_TRANG); }
                });
            }
        }
    }

    private PanelBoGoc taoCardThongKe(String title, String icon, Color iconColor, Color iconBgColor, Color cardBgColor) {
        PanelBoGoc card = new PanelBoGoc(12, cardBgColor);
        card.setLayout(new BorderLayout(0, 5));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JPanel pnlHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        pnlHeader.setOpaque(false);
        
        JLabel lblIcon = taoIconTron(icon, iconColor);
        lblIcon.setBackground(iconBgColor);
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(new Color(31, 41, 55));
        
        pnlHeader.add(lblIcon);
        pnlHeader.add(lblTitle);
        
        card.add(pnlHeader, BorderLayout.NORTH);
        return card;
    }

    private JLabel taoIconTron(String text, Color fgColor) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.fillOval(x, y, size, size);
                super.paintComponent(g);
            }
        };
        lbl.setPreferredSize(new Dimension(36, 36));
        lbl.setMinimumSize(new Dimension(36, 36)); 
        // 🔥 ÉP FONT EMOJI Ở ĐÂY ĐỂ TRÁNH LỖI Ô VUÔNG
        lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lbl.setForeground(fgColor);
        lbl.setOpaque(false);
        return lbl;
    }

    private JButton taoNutSpinner(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 32)); 
        btn.setForeground(MAU_CHU_CHINH);
        btn.setBackground(new Color(243, 244, 246));
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.setPreferredSize(new Dimension(60, 85)); 
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel taoLabelSoLieu(String text, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 50));
        lbl.setForeground(color);
        return lbl;
    }

    private JLabel taoLabelDVT(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lbl.setForeground(MAU_CHU_NHAT);
        return lbl;
    }

    private class PanelBoGoc extends JPanel {
        private int banKinh;
        public PanelBoGoc(int banKinh, Color mauNen) {
            this.banKinh = banKinh;
            setBackground(mauNen);
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), banKinh, banKinh));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class NutBamHienDai extends JButton {
        public NutBamHienDai(String text, Color bg, Color fg) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setBackground(bg); setForeground(fg);
            setFocusPainted(false); setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(bg.darker()); }
                public void mouseExited(MouseEvent e) { setBackground(bg); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            FontMetrics fm = g2.getFontMetrics();
            Rectangle r = fm.getStringBounds(getText(), g2).getBounds();
            g2.setColor(getForeground());
            g2.drawString(getText(), (getWidth() - r.width) / 2, (getHeight() - r.height) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() { this.thumbColor = new Color(209, 213, 219); }
        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() {
            JButton jbutton = new JButton();
            jbutton.setPreferredSize(new Dimension(0, 0));
            return jbutton;
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new KiemKeGUI().setVisible(true));
    }
}