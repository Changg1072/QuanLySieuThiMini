package GUI.HoTro;

import Dao.TruyVanSieuTocDAO;
import Data.ChiTietHoaDon;
import GUI.DonHangUi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopupTraHang extends JDialog {

    private float opacity = 0f;
    private final DonHangUi parentUi;
    private final String maHD;
    private final List<ChiTietHoaDon> dsChiTiet;

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private Map<ChiTietHoaDon, JTextField> mapLyDo = new HashMap<>();

    public PopupTraHang(JFrame parentFrame, DonHangUi parentUi, String maHD, List<ChiTietHoaDon> dsChiTiet) {
        super(parentFrame, true);
        this.parentUi = parentUi;
        this.maHD = maHD;
        this.dsChiTiet = dsChiTiet;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); // Nền trong suốt
        setSize(parentFrame.getSize());
        setLocationRelativeTo(parentFrame);

        setLayout(new GridBagLayout()); // Để căn giữa Card

        initUI();
        hienThiVoiAnimation();
    }

    private void initUI() {
        // --- Overlay Background (Glassmorphism nhẹ) ---
        JPanel pnlOverlay = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity * 0.6f));
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        pnlOverlay.setOpaque(false);
        setContentPane(pnlOverlay);

        // --- Main Box ---
        TheBongDo mainBox = new TheBongDo(25);
        mainBox.setBackground(Color.WHITE);
        mainBox.setPreferredSize(new Dimension(650, 450));
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        cardPanel.add(taoBuocXacNhan(), "STEP_1");
        cardPanel.add(taoBuocChonSanPham(), "STEP_2");

        mainBox.setLayout(new BorderLayout());
        mainBox.add(cardPanel, BorderLayout.CENTER);

        pnlOverlay.add(mainBox);
    }

    // ==========================================
    // STEP 1: XÁC NHẬN (UI HIỆN ĐẠI)
    // ==========================================
    private JPanel taoBuocXacNhan() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel lblIcon = new JLabel("⚠️", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));

        JLabel lblTitle = new JLabel("Xác Nhận Trả Hàng?", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Calibri", Font.BOLD, 28));
        lblTitle.setForeground(new Color(36, 36, 36));

        JLabel lblSub = new JLabel("<html><center>Bạn sắp thực hiện quy trình trả hàng cho hóa đơn <b>" + maHD + "</b>.<br>Toàn bộ sản phẩm sẽ được hoàn lại kho. Bạn có muốn tiếp tục?</center></html>", SwingConstants.CENTER);
        lblSub.setFont(new Font("Calibri", Font.PLAIN, 16));
        lblSub.setForeground(new Color(130, 130, 130));

        JPanel pnlCenter = new JPanel(new GridLayout(3, 1, 0, 10));
        pnlCenter.setOpaque(false);
        pnlCenter.add(lblIcon);
        pnlCenter.add(lblTitle);
        pnlCenter.add(lblSub);

        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        pnlBtn.setOpaque(false);

        NutBoGoc btnHuy = new NutBoGoc("Không, quay lại");
        btnHuy.setColorBackground(new Color(220, 220, 220));
        btnHuy.setForeground(new Color(80, 80, 80));
        btnHuy.setPreferredSize(new Dimension(150, 45));
        btnHuy.addActionListener(e -> dongPopup());

        NutBoGoc btnTiepTuc = new NutBoGoc("Có, chọn lý do");
        btnTiepTuc.setColorBackground(new Color(238, 77, 45));
        btnTiepTuc.setPreferredSize(new Dimension(150, 45));
        btnTiepTuc.addActionListener(e -> cardLayout.show(cardPanel, "STEP_2"));

        pnlBtn.add(btnHuy);
        pnlBtn.add(btnTiepTuc);

        pnl.add(pnlCenter, BorderLayout.CENTER);
        pnl.add(pnlBtn, BorderLayout.SOUTH);
        return pnl;
    }

    // ==========================================
    // STEP 2: CHỌN SẢN PHẨM & LÝ DO
    // ==========================================
    private JPanel taoBuocChonSanPham() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("Ghi nhận lý do trả hàng");
        lblTitle.setFont(new Font("Calibri", Font.BOLD, 22));
        
        JLabel lblNote = new JLabel("Lưu ý: Dù chỉ báo lỗi 1 sản phẩm, hệ thống vẫn sẽ thu hồi toàn bộ hóa đơn.");
        lblNote.setFont(new Font("Calibri", Font.ITALIC, 14));
        lblNote.setForeground(new Color(238, 77, 45));

        JPanel pnlHeader = new JPanel(new GridLayout(2, 1));
        pnlHeader.setOpaque(false);
        pnlHeader.add(lblTitle);
        pnlHeader.add(lblNote);

        // Danh sách SP Scroll
        JPanel pnlList = new JPanel();
        pnlList.setLayout(new BoxLayout(pnlList, BoxLayout.Y_AXIS));
        pnlList.setOpaque(false);

        for (ChiTietHoaDon ct : dsChiTiet) {
            pnlList.add(taoItemSanPham(ct));
            pnlList.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane scroll = new JScrollPane(pnlList);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        TienIchGiaoDien.thietLapThanhCuon(scroll); // Tận dụng code có sẵn của bạn

        // Button action
        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBtn.setOpaque(false);

        NutBoGoc btnHuy = new NutBoGoc("Hủy");
        btnHuy.setColorBackground(new Color(220, 220, 220));
        btnHuy.setForeground(new Color(80, 80, 80));
        btnHuy.addActionListener(e -> dongPopup());

        NutBoGoc btnXacNhan = new NutBoGoc("Xác Nhận Trả Hàng");
        btnXacNhan.setColorBackground(new Color(238, 77, 45));
        btnXacNhan.addActionListener(e -> xuLyTraHang());

        pnlBtn.add(btnHuy);
        pnlBtn.add(btnXacNhan);

        pnl.add(pnlHeader, BorderLayout.NORTH);
        pnl.add(scroll, BorderLayout.CENTER);
        pnl.add(pnlBtn, BorderLayout.SOUTH);

        return pnl;
    }

    private JPanel taoItemSanPham(ChiTietHoaDon ct) {
        JPanel pnlItem = new JPanel(new BorderLayout(10, 0));
        pnlItem.setBackground(new Color(250, 252, 255));
        pnlItem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Lấy tên SP thật từ DB (hoặc Cache)
        String tenSP = Dao.SanPhamDAO.getInstance().laySanPhamTheoMa(ct.getMaSp()).getTenSP();

        JCheckBox chkChon = new JCheckBox(tenSP + " (SL: " + ct.getSoLuong() + ")");
        chkChon.setFont(new Font("Calibri", Font.BOLD, 15));
        chkChon.setOpaque(false);

        TheBongDo.RoundedTextField txtLyDo = new TheBongDo.RoundedTextField("Nhập lý do trả sản phẩm này...", 30);
        txtLyDo.setVisible(false);
        
        mapLyDo.put(ct, txtLyDo); // Lưu reference để lấy text sau này

        chkChon.addActionListener(e -> {
            txtLyDo.setVisible(chkChon.isSelected());
            pnlItem.revalidate();
            pnlItem.repaint();
        });

        pnlItem.add(chkChon, BorderLayout.NORTH);
        pnlItem.add(txtLyDo, BorderLayout.CENTER);

        return pnlItem;
    }

    // ==========================================
    // LOGIC XỬ LÝ TRẢ HÀNG (ĐÃ THÊM CHECK CA LÀM)
    // ==========================================
    private void xuLyTraHang() {
        // --- 🛡️ BƯỚC BẢO MẬT: KIỂM TRA CA LÀM VIỆC ---
        try {
            // Lấy mã nhân viên đang đăng nhập từ hệ thống (Ví dụ: "NV001")
            // Bạn có thể lấy từ biến static hoặc session của bạn
            String maNVHienTai = "NV001"; 

            Logic.ChiaCaLogic ccLogic = new Logic.ChiaCaLogic(); //
            if (!ccLogic.kiemTraNhanVienDangTrongCa(maNVHienTai)) { //
                GUI.HoTro.TienIchGiaoDien.hienThiThongBao(this, 
                    "BẠN KHÔNG TRONG CA LÀM VIỆC!\n" +
                    "Vui lòng điểm danh 'Có mặt' trước khi thực hiện trả hàng.", "ERROR"); //
                return; 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // ----------------------------------------------

        List<String> dsLyDo = new ArrayList<>();
        String homNay = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        for (Map.Entry<ChiTietHoaDon, JTextField> entry : mapLyDo.entrySet()) {
            if (entry.getValue().isVisible() && !entry.getValue().getText().trim().isEmpty()) {
                String tenSP = Dao.SanPhamDAO.getInstance().laySanPhamTheoMa(entry.getKey().getMaSp()).getTenSP();
                String lyDo = entry.getValue().getText().trim();
                dsLyDo.add(tenSP + "_" + lyDo + "_" + homNay);
            }
        }

        String lyDoGop = dsLyDo.isEmpty() ? "Khách trả toàn bộ đơn_" + homNay : String.join(" + ", dsLyDo);

        BigDecimal tongTienCu = BigDecimal.ZERO;
        for (ChiTietHoaDon ct : dsChiTiet) {
            tongTienCu = tongTienCu.add(ct.getThanhTienSanPham());
        }
        final BigDecimal finalTongTienCu = tongTienCu;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Sử dụng Transaction để hủy đơn và trả kho an toàn
                Dao.TruyVanSieuTocDAO.getInstance().xuLyTraHangToanBoSieuToc(maHD, lyDoGop, dsChiTiet);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    dongPopup();
                    parentUi.taiDuLieuTuDatabase();

                    Window topFrame = SwingUtilities.getWindowAncestor(parentUi);
                    GUI.BanHangUi banHangUiThucTe = timManHinhBanHang((Container) topFrame);

                    if (banHangUiThucTe != null) {
                        banHangUiThucTe.kichHoatCheDoDoiHang(dsChiTiet, finalTongTienCu);
                        GUI.HoTro.TienIchGiaoDien.hienThiThongBao(topFrame, "Đã hủy đơn cũ! Vui lòng chọn món để ĐỔI HÀNG.", "SUCCESS");
                        tuDongChuyenTabBanHang((Container) topFrame);
                    }

                } catch (Exception e) {
                    GUI.HoTro.TienIchGiaoDien.hienThiThongBao(parentUi, e.getMessage(), "ERROR");
                }
            }
        };
        worker.execute();
    }
    // ==========================================
    // CÁC HÀM HỖ TRỢ TÌM KIẾM GIAO DIỆN (ĐỆ QUY)
    // ==========================================
    
    // Tìm Panel BanHangUi đang ẩn bên dưới
    private GUI.BanHangUi timManHinhBanHang(Container container) {
        if (container == null) return null;
        for (Component c : container.getComponents()) {
            if (c instanceof GUI.BanHangUi) {
                return (GUI.BanHangUi) c;
            }
            if (c instanceof Container) {
                GUI.BanHangUi found = timManHinhBanHang((Container) c);
                if (found != null) return found;
            }
        }
        return null;
    }

    // 🔥 Tìm nút "Bán Hàng" trên thanh Menu dọc (Sidebar) và giả lập thao tác click chuột
    private void tuDongChuyenTabBanHang(Container container) {
        if (container == null) return;
        for (Component c : container.getComponents()) {
            // Nếu component này là một Nút bấm (JButton)
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                String text = btn.getText();
                // Quét xem chữ trên nút có chứa từ "bán hàng" không (không phân biệt hoa thường)
                if (text != null && text.toLowerCase().contains("bán hàng")) {
                    // Tự động ra lệnh CLICK! Nó sẽ kích hoạt đổi thẻ CardLayout và đổi màu Menu
                    btn.doClick(); 
                    return;
                }
            }
            // Tiếp tục lục lọi các tầng bên trong
            if (c instanceof Container) {
                tuDongChuyenTabBanHang((Container) c);
            }
        }
    }

    private void hienThiVoiAnimation() {
        Timer timer = new Timer(15, e -> {
            opacity += 0.1f;
            if (opacity >= 1f) {
                opacity = 1f;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        timer.start();
        setVisible(true);
    }

    private void dongPopup() {
        Timer timer = new Timer(10, e -> {
            opacity -= 0.1f;
            if (opacity <= 0f) {
                ((Timer) e.getSource()).stop();
                dispose();
            }
            repaint();
        });
        timer.start();
    }
}