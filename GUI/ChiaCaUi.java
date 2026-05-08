package GUI;

import GUI.HoTro.NutBoGoc;
import GUI.HoTro.TheBongDo;
import GUI.HoTro.TienIchGiaoDien;
import Dao.TruyVanSieuTocDAO; // Nhúng Động cơ Turbo vào đây 🚀
import Logic.ChiaCaLogic;
import Logic.LoaiCaLogic;
import Logic.TaoMaTuDongLogic; 
import Data.NhanVien;
import Data.ChiaCa;
import Data.LoaiCa;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChiaCaUi extends JPanel {

    private static final Font FONT_THUONG = new Font("Calibri", Font.PLAIN, 18);
    private static final Font FONT_DAM = new Font("Calibri", Font.BOLD, 20);
    private static final Color MAU_NEN = new Color(248, 249, 250);
    private static final Color MAU_VIEN = new Color(222, 226, 232);
    private static final Color MAU_CHU_CHINH = new Color(30, 41, 59);
    private static final Color MAU_HIGHLIGHT_NEN = new Color(224, 242, 254);
    private static final Color MAU_HIGHLIGHT_CHU = new Color(3, 105, 161);

    private JComboBox<String> cbCaLam, cbTinhTrang;
    private TheBongDo.RoundedTextField txtTimNhanVien, txtNgayApDung, txtMaCa;
    private TheBongDo.RoundedTextField txtCheckIn, txtCheckOut;
    private TheBongDo.RoundedTextField txtTienDauCa, txtTienCuoiCa, txtTienBanHang, txtTienChenhLech;
    private JPanel pnlDanhSachTag;
    
    private NutBoGoc btnApDung, btnHuyCa, btnKetCa, btnDuyetCa;
    private Timer timerRealtime;

    private JPopupMenu suggestPopup;
    private JList<NhanVien> suggestList;
    private DefaultListModel<NhanVien> suggestModel;
    
    // 🔥 DATA CACHE SIÊU TỐC
    private TruyVanSieuTocDAO.DuLieuChiaCaDTO cachedDataCa;
    private List<NhanVien> listNhanVienAll = new ArrayList<>();
    private List<LoaiCa> listLoaiCaAll = new ArrayList<>();
    
    private List<String> danhSachMaNVDaChon = new ArrayList<>();
    private List<ChiaCa> danhSachCaDangChon = new ArrayList<>();

    private BangChiaCaRight pnlLichRight;
    private ChiaCaLogic ccLogic = new ChiaCaLogic(); // Vẫn giữ Logic để xử lý Thêm/Sửa/Xóa
    private String maNVDangNhap = null;

    public ChiaCaUi(String maNV) {
        this.maNVDangNhap = maNV;
        khoiTaoGiaoDien();
        
        // Tải dữ liệu siêu tốc bằng Background Thread, sau khi tải xong thì tự động load ca của NV
        taiDuLieuCaTuDatabase(() -> tuDongHienThiCaCuaNV(maNV));
        
        timerRealtime = new Timer(3000, e -> capNhatSoLieuRealtime());
        timerRealtime.start();
    }
    
    public ChiaCaUi() {
        khoiTaoGiaoDien();
        taiDuLieuCaTuDatabase(null);
    }

    // =======================================================
    // 🔥 TRÁI TIM CỦA TỐC ĐỘ: LOAD MULTIPLE RESULT SETS
    // =======================================================
    private void taiDuLieuCaTuDatabase(Runnable onSuccessCallback) {
        SwingWorker<TruyVanSieuTocDAO.DuLieuChiaCaDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected TruyVanSieuTocDAO.DuLieuChiaCaDTO doInBackground() {
                try {
                    // Quét Auto Checkout trước khi kéo dữ liệu về
                    ccLogic.kiemTraVaTuDongKetCa();
                } catch (Exception e) {}
                
                // Gọi Động Cơ Turbo (Lấy 3 bảng ChiaCa, LoaiCa, NhanVien trong 1 nốt nhạc)
                return TruyVanSieuTocDAO.getInstance().loadToanBoLichChiaCa();
            }

            @Override
            protected void done() {
                try {
                    cachedDataCa = get();
                    listNhanVienAll = new ArrayList<>(cachedDataCa.mapNhanVien.values());
                    listLoaiCaAll = new ArrayList<>(cachedDataCa.mapLoaiCa.values());

                    // Nạp Combobox Loại Ca
                    cbCaLam.removeAllItems();
                    for (LoaiCa lc : listLoaiCaAll) cbCaLam.addItem(lc.getTenCa());

                    if (pnlLichRight != null) pnlLichRight.taiDuLieuLenLich();
                    if (onSuccessCallback != null) onSuccessCallback.run();
                } catch (Exception e) {
                    System.err.println("Lỗi load dữ liệu siêu tốc Chia Ca: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void tuDongHienThiCaCuaNV(String maNV) {
        if (cachedDataCa == null) return;
        java.time.LocalDate homNay = java.time.LocalDate.now();
        
        for (ChiaCa cc : cachedDataCa.dsChiaCa) {
            if (cc.getMaNV() != null && cc.getMaNV().trim().equals(maNV.trim())
                && cc.getNgayLam() != null && cc.getNgayLam().equals(homNay)
                && "Đang làm việc".equals(cc.getTinhTrang())) {

                String tenCa = layTenCa(cc.getMaLoaiCa());
                String ngayFormat = homNay.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                
                hienThiChiTietCaLam(tenCa, ngayFormat, maNV); 
                return;
            }
        }
    }

    private void hienThiChiTietCaLam(String tenCa, String ngayFormat, String maNVUuTien) {
        if (cachedDataCa == null) return;
        xoaRongForm(ngayFormat); 
        cbCaLam.setSelectedItem(tenCa);

        try {
            LocalDate date = LocalDate.parse(ngayFormat, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String maLoaiCa = "";
            for (LoaiCa lc : listLoaiCaAll) {
                if (lc.getTenCa().equals(tenCa)) { maLoaiCa = lc.getMaLoaiCa(); break; }
            }

            List<ChiaCa> matchingCa = new ArrayList<>();
            ChiaCa caCuaToi = null; 

            // Đọc trực tiếp từ Cache (O(N) bộ nhớ, 0 truy vấn DB)
            for (ChiaCa cc : cachedDataCa.dsChiaCa) {
                if (cc.getNgayLam() != null && cc.getNgayLam().equals(date) 
                    && cc.getMaLoaiCa().equals(maLoaiCa) 
                    && !cc.getTinhTrang().equals("Đã hủy")) {
                    
                    matchingCa.add(cc);
                    if (maNVUuTien != null && cc.getMaNV().trim().equals(maNVUuTien.trim())) {
                        caCuaToi = cc;
                    }
                }
            }

            if (!matchingCa.isEmpty()) {
                danhSachCaDangChon = matchingCa;
                
                for (ChiaCa cc : matchingCa) {
                    NhanVien nv = cachedDataCa.mapNhanVien.get(cc.getMaNV());
                    if (nv != null) { themTagNhanVien(nv); }
                }

                ChiaCa displayCa = (caCuaToi != null) ? caCuaToi : matchingCa.get(0);
                
                txtMaCa.setText(displayCa.getMaCa()); 

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                txtCheckIn.setText(displayCa.getThoiGianCheckIn() != null ? displayCa.getThoiGianCheckIn().format(formatter) : "");
                txtCheckOut.setText(displayCa.getThoiGianCheckOut() != null ? displayCa.getThoiGianCheckOut().format(formatter) : "");

                cbTinhTrang.setSelectedItem(displayCa.getTinhTrang() != null ? displayCa.getTinhTrang() : "Chưa Phân Công");
                txtTienDauCa.setText(displayCa.getTienDauCa() != null ? displayCa.getTienDauCa().toString() : "0.00");
                txtTienCuoiCa.setText(displayCa.getTienCuoiCa() != null ? displayCa.getTienCuoiCa().toString() : "0.00");
                txtTienBanHang.setText(displayCa.getTienBanHang() != null ? displayCa.getTienBanHang().toString() : "0.00");
                txtTienChenhLech.setText(displayCa.getTienChenhLech() != null ? displayCa.getTienChenhLech().toString() : "0.00");
                
                btnApDung.setVisible(false);
                
                if ("Đã phân công".equalsIgnoreCase(displayCa.getTinhTrang())) {
                    btnDuyetCa.setVisible(false); // Không cần nút duyệt nữa vì DB gộp chung vào "Đã phân công"
                    btnHuyCa.setVisible(true);
                    btnKetCa.setVisible(false);
                } else if ("Đang làm việc".equalsIgnoreCase(displayCa.getTinhTrang())) {
                    btnDuyetCa.setVisible(false);
                    btnHuyCa.setVisible(false); 
                    btnKetCa.setVisible(true);
                    if (timerRealtime != null) timerRealtime.restart();
                } else {
                    // Đã hoàn thành hoặc Đã hủy thì ẩn hết nút thao tác
                    btnDuyetCa.setVisible(false);
                    btnHuyCa.setVisible(false);
                    btnKetCa.setVisible(false);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void hienThiChiTietCaLam(String tenCa, String ngayFormat) {
        hienThiChiTietCaLam(tenCa, ngayFormat, maNVDangNhap);
    }

    // Giao diện (Giữ nguyên cấu trúc xịn xò của cậu)
    private void khoiTaoGiaoDien() {
        setLayout(new BorderLayout(0, 0));
        setBackground(MAU_NEN);

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(Color.WHITE);
        pnlHeader.setBorder(new EmptyBorder(15, 25, 15, 25));
        JLabel lblTitle = new JLabel("Hệ thống quản lý ca làm việc");
        lblTitle.setFont(FONT_DAM.deriveFont(28f));
        
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlButtons.setBackground(Color.WHITE);
        
        btnDuyetCa = new NutBoGoc("Duyệt Ca");
        btnDuyetCa.setColorBackground(new Color(59, 130, 246)); 
        btnDuyetCa.addActionListener(e -> xuLyNutDuyetCa());
        btnDuyetCa.setVisible(false);

        btnHuyCa = new NutBoGoc("Hủy Ca");
        btnHuyCa.setColorBackground(new Color(220, 53, 69)); 
        btnHuyCa.addActionListener(e -> thucHienHuyCa());
        btnHuyCa.setVisible(false); 
        
        NutBoGoc btnHuy = new NutBoGoc("Làm mới");
        btnHuy.setColorBackground(new Color(241, 245, 249));
        btnHuy.setForeground(MAU_CHU_CHINH);
        btnHuy.addActionListener(e -> xoaRongForm(txtNgayApDung.getText())); 
        
        btnApDung = new NutBoGoc("Áp dụng");
        btnApDung.setColorBackground(new Color(249, 115, 22));
        btnApDung.addActionListener(e -> thucHienLuuChiaCa());
        
        btnKetCa = new NutBoGoc("Kết Ca");
        btnKetCa.setColorBackground(new Color(16, 185, 129)); 
        btnKetCa.addActionListener(e -> xuLyNutKetCa());
        btnKetCa.setVisible(false);

        pnlButtons.add(btnDuyetCa);
        pnlButtons.add(btnHuyCa);
        pnlButtons.add(btnKetCa);
        pnlButtons.add(btnHuy); 
        pnlButtons.add(btnApDung);
        pnlHeader.add(lblTitle, BorderLayout.WEST); pnlHeader.add(pnlButtons, BorderLayout.EAST);

        JPanel pnlBody = new JPanel(new GridBagLayout());
        pnlBody.setBackground(MAU_NEN);
        pnlBody.setBorder(new EmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.weightx = 0.3; gbc.insets = new Insets(0, 0, 0, 15);
        pnlBody.add(taoScrollForm(), gbc);

        gbc.gridx = 1; gbc.weightx = 0.7; gbc.insets = new Insets(0, 15, 0, 0);
        pnlBody.add(taoLichRight(), gbc);

        add(pnlHeader, BorderLayout.NORTH); add(pnlBody, BorderLayout.CENTER);
    }

    private JScrollPane taoScrollForm() {
        JPanel pnlForm = new JPanel();
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.Y_AXIS));
        pnlForm.setBackground(Color.WHITE);
        pnlForm.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel lblHeader = new JLabel("Thông tin chi tiết ca làm");
        lblHeader.setFont(FONT_DAM.deriveFont(24f));
        lblHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlForm.add(lblHeader); pnlForm.add(Box.createVerticalStrut(30));

        txtMaCa = new TheBongDo.RoundedTextField("", 0);
        txtMaCa.setText(TaoMaTuDongLogic.taoMaCa()); 
        txtMaCa.setEnabled(false); txtMaCa.setDisabledTextColor(new Color(100, 100, 100)); 
        pnlForm.add(taoRow("Mã ca :", txtMaCa)); pnlForm.add(Box.createVerticalStrut(15));

        cbCaLam = new JComboBox<>();
        TienIchGiaoDien.trangTriComboBox(cbCaLam);
        cbCaLam.setRenderer(taoRendererCombo());
        pnlForm.add(taoRow("Chọn ca * :", cbCaLam)); pnlForm.add(Box.createVerticalStrut(15));

        pnlForm.add(taoRow("Ngày áp dụng :", taoMiniDatePicker())); pnlForm.add(Box.createVerticalStrut(15));

        txtTimNhanVien = new TheBongDo.RoundedTextField("Tìm kiếm nhân viên...", 0);
        pnlForm.add(taoRow("Nhân viên :", txtTimNhanVien)); pnlForm.add(Box.createVerticalStrut(10));
        
        pnlDanhSachTag = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)); pnlDanhSachTag.setOpaque(false);
        JPanel tagWrap = new JPanel(new BorderLayout()); tagWrap.setOpaque(false);
        JLabel indent = new JLabel(""); indent.setPreferredSize(new Dimension(125, 0));
        tagWrap.add(indent, BorderLayout.WEST); tagWrap.add(pnlDanhSachTag, BorderLayout.CENTER);
        
        JScrollPane scrollTags = new JScrollPane(tagWrap);
        scrollTags.setBorder(null);
        TienIchGiaoDien.thietLapThanhCuon(scrollTags);
        pnlForm.add(scrollTags); pnlForm.add(Box.createVerticalStrut(15));
        
        thietLapAutoSuggestNhanVien();

        txtCheckIn = new TheBongDo.RoundedTextField("HH:mm dd/MM/yyyy", 0);
        txtCheckIn.setEnabled(false); txtCheckIn.setDisabledTextColor(Color.GRAY);
        pnlForm.add(taoRow("Giờ Check-in :", txtCheckIn)); pnlForm.add(Box.createVerticalStrut(15));

        txtCheckOut = new TheBongDo.RoundedTextField("HH:mm dd/MM/yyyy", 0);
        txtCheckOut.setEnabled(false); txtCheckOut.setDisabledTextColor(Color.GRAY);
        pnlForm.add(taoRow("Giờ Check-out :", txtCheckOut)); pnlForm.add(Box.createVerticalStrut(15));

       // Giảm bớt các trạng thái màu mè, đưa về chuẩn của DB
        cbTinhTrang = new JComboBox<>(new String[]{"Chưa Phân Công", "Đã phân công", "Đang làm việc", "Đã hoàn thành", "Đã hủy"});
        cbTinhTrang.setEnabled(false); 
        TienIchGiaoDien.trangTriComboBox(cbTinhTrang);
        cbTinhTrang.setRenderer(taoRendererCombo());
        pnlForm.add(taoRow("Tình trạng :", cbTinhTrang)); pnlForm.add(Box.createVerticalStrut(15));

        txtTienDauCa = new TheBongDo.RoundedTextField("0.00", 0); txtTienDauCa.setEnabled(false);
        pnlForm.add(taoRow("Tiền đầu ca :", txtTienDauCa)); pnlForm.add(Box.createVerticalStrut(15));
        
        txtTienCuoiCa = new TheBongDo.RoundedTextField("0.00", 0); txtTienCuoiCa.setEnabled(false);
        pnlForm.add(taoRow("Tiền cuối ca :", txtTienCuoiCa)); pnlForm.add(Box.createVerticalStrut(15));
        
        txtTienBanHang = new TheBongDo.RoundedTextField("0.00", 0); txtTienBanHang.setEnabled(false);
        pnlForm.add(taoRow("Tiền bán hàng :", txtTienBanHang)); pnlForm.add(Box.createVerticalStrut(15));
        
        txtTienChenhLech = new TheBongDo.RoundedTextField("0.00", 0); txtTienChenhLech.setEnabled(false);
        pnlForm.add(taoRow("Chênh lệch :", txtTienChenhLech));

        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1, true));
        wrapper.add(pnlForm, BorderLayout.CENTER);
        return new JScrollPane(wrapper);
    }

    private DefaultListCellRenderer taoRendererCombo() {
        return new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(10, 15, 10, 15));
                if (index == -1) { l.setBackground(Color.WHITE); l.setForeground(MAU_CHU_CHINH); } 
                else if (isSelected) { l.setBackground(MAU_HIGHLIGHT_NEN); l.setForeground(MAU_HIGHLIGHT_CHU); } 
                else { l.setBackground(Color.WHITE); l.setForeground(MAU_CHU_CHINH); }
                return l;
            }
        };
    }

    private JPanel taoRow(String labelText, JComponent inputComp) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_THUONG); lbl.setForeground(MAU_CHU_CHINH); 
        lbl.setPreferredSize(new Dimension(125, 45));
        row.add(lbl, BorderLayout.WEST); row.add(inputComp, BorderLayout.CENTER);
        return row;
    }

    private JPanel taoMiniDatePicker() {
        JPanel pnl = new JPanel(new BorderLayout(5, 0)); pnl.setOpaque(false);
        txtNgayApDung = new TheBongDo.RoundedTextField("dd/MM/yyyy", 0);
        txtNgayApDung.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        NutBoGoc btn = new NutBoGoc("📅"); btn.setPreferredSize(new Dimension(45, 45));
        btn.setColorBackground(new Color(236, 242, 255)); btn.setMargin(new Insets(2, 2, 2, 2));
        JPopupMenu popup = taoPopupLich();
        btn.addActionListener(e -> popup.show(btn, 0, btn.getHeight()));
        pnl.add(txtNgayApDung, BorderLayout.CENTER); pnl.add(btn, BorderLayout.EAST);
        return pnl;
    }

    private void thietLapAutoSuggestNhanVien() {
        suggestPopup = new JPopupMenu();
        suggestModel = new DefaultListModel<>(); suggestList = new JList<>(suggestModel);
        suggestList.setFont(FONT_THUONG); suggestList.setFixedCellHeight(40);
        
        suggestList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NhanVien) {
                    NhanVien nv = (NhanVien) value; setText(nv.getMaNV() + " - " + nv.getHoTen()); 
                }
                setBorder(new EmptyBorder(0, 15, 0, 15));
                if (isSelected) { setBackground(MAU_HIGHLIGHT_NEN); setForeground(MAU_HIGHLIGHT_CHU); } 
                else { setBackground(Color.WHITE); setForeground(MAU_CHU_CHINH); }
                return this;
            }
        });
        suggestPopup.add(new JScrollPane(suggestList));

        txtTimNhanVien.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
            private void update() {
                String text = txtTimNhanVien.getText().toLowerCase(); suggestModel.clear();
                if (text.isEmpty()) { suggestPopup.setVisible(false); return; }
                for (NhanVien nv : listNhanVienAll) {
                    if ((nv.getHoTen().toLowerCase().contains(text) || nv.getMaNV().toLowerCase().contains(text))
                         && !danhSachMaNVDaChon.contains(nv.getMaNV())) {
                        suggestModel.addElement(nv);
                    }
                }
                if (!suggestModel.isEmpty()) {
                    suggestPopup.show(txtTimNhanVien, 0, txtTimNhanVien.getHeight()); txtTimNhanVien.requestFocus();
                } else suggestPopup.setVisible(false);
            }
        });

        suggestList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                NhanVien selected = suggestList.getSelectedValue();
                if (selected != null) { themTagNhanVien(selected); txtTimNhanVien.setText(""); suggestPopup.setVisible(false); }
            }
        });
    }

    private void themTagNhanVien(NhanVien nv) {
        if (danhSachMaNVDaChon.contains(nv.getMaNV())) return; 
        danhSachMaNVDaChon.add(nv.getMaNV());
        JPanel tag = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(230, 235, 245));
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15); g2.dispose();
            }
        };
        tag.setOpaque(false); tag.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JLabel lbl = new JLabel(nv.getHoTen() + " (" + nv.getMaNV() + ")"); lbl.setFont(FONT_THUONG.deriveFont(15f));
        JLabel close = new JLabel("✕"); close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                danhSachMaNVDaChon.remove(nv.getMaNV());
                pnlDanhSachTag.remove(tag); pnlDanhSachTag.revalidate(); pnlDanhSachTag.repaint();
            }
        });
        tag.add(lbl); tag.add(close);
        pnlDanhSachTag.add(tag); pnlDanhSachTag.revalidate(); pnlDanhSachTag.repaint();
    }

    private BigDecimal tinhTienDauCa(LocalDate ngayLam, String maLoaiCa, String tenCa) {
        if (tenCa.toLowerCase().contains("sáng")) return new BigDecimal("500000");
        
        String tenCaTruoc = tenCa.toLowerCase().contains("chiều") ? "sáng" : "chiều";
        String maLoaiCaTruoc = "";
        for (LoaiCa lc : listLoaiCaAll) {
            if (lc.getTenCa().toLowerCase().contains(tenCaTruoc)) { maLoaiCaTruoc = lc.getMaLoaiCa(); break; }
        }
        
        BigDecimal tienKeThua = BigDecimal.ZERO;
        if (cachedDataCa != null) {
            for (ChiaCa cc : cachedDataCa.dsChiaCa) {
                if (cc.getNgayLam() != null && cc.getNgayLam().equals(ngayLam)
                    && cc.getMaLoaiCa().equals(maLoaiCaTruoc)
                    && ("Đã hoàn thành".equals(cc.getTinhTrang()) || "Đã kết ca".equals(cc.getTinhTrang()))) {
                    if (cc.getTienCuoiCa() != null) tienKeThua = cc.getTienCuoiCa();
                }
            }
        }
        return tienKeThua;
    }

    private void thucHienLuuChiaCa() {
        if (danhSachMaNVDaChon.isEmpty()) {
            TienIchGiaoDien.hienThiThongBao(this, "Vui lòng chọn ít nhất 1 nhân viên!", "WARNING");
            return;
        }
        try {
            LocalDate ngayLam = LocalDate.parse(txtNgayApDung.getText(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            if (ngayLam.isBefore(LocalDate.now())) {
                TienIchGiaoDien.hienThiThongBao(this, "Lỗi: Không tạo ca làm việc cho quá khứ!", "ERROR"); return;
            }

            int selectedIdx = cbCaLam.getSelectedIndex();
            if (listLoaiCaAll.isEmpty()) return;
            String maLoaiCa = listLoaiCaAll.get(selectedIdx).getMaLoaiCa();
            String tenCa = listLoaiCaAll.get(selectedIdx).getTenCa();
            
            // Check trùng ca trong Cache thay vì DB
            for (ChiaCa caDB : cachedDataCa.dsChiaCa) {
                if (caDB.getNgayLam() != null && caDB.getNgayLam().equals(ngayLam) 
                    && caDB.getMaLoaiCa().equals(maLoaiCa) 
                    && !caDB.getTinhTrang().equals("Đã hủy")
                    && danhSachMaNVDaChon.contains(caDB.getMaNV())) {
                    TienIchGiaoDien.hienThiThongBao(this, "Lỗi: 1 hoặc nhiều NV đã có ca này trong ngày!", "ERROR");
                    return;
                }
            }

            BigDecimal tienDau = tinhTienDauCa(ngayLam, maLoaiCa, tenCa);
            int soCaThanhCong = 0;
            
            for (String maNV : danhSachMaNVDaChon) {
                String maCaMoi = TaoMaTuDongLogic.taoMaCa();
                ChiaCa ca = new ChiaCa.ThoXayChiaCa().ganMaCa(maCaMoi).ganMaLoaiCa(maLoaiCa)
                    .ganNgayLam(ngayLam).ganMaNV(maNV).ganTinhTrang("Đã phân công").ganTienDauCa(tienDau).taoMoi();
                ccLogic.themChiaCa(ca); // Ghi xuống DB
                soCaThanhCong++;
                try { Thread.sleep(10); } catch (Exception ignored) {}
            }

            TienIchGiaoDien.hienThiThongBao(this, "Đã tạo thành công " + soCaThanhCong + " ca làm việc!", "SUCCESS");
            
            // 🔥 SAU KHI LƯU DB, GỌI ĐỘNG CƠ TẢI LẠI DỮ LIỆU ĐỂ RENDER UI
            taiDuLieuCaTuDatabase(() -> xoaRongForm(txtNgayApDung.getText()));
            
        } catch (Exception e) {
            TienIchGiaoDien.hienThiThongBao(this, "Lỗi khi lưu DB: " + e.getMessage(), "ERROR");
        }
    }

    private void xuLyNutDuyetCa() {
        if (danhSachCaDangChon.isEmpty()) return;
        try {
            for (ChiaCa cc : danhSachCaDangChon) {
                cc.setTinhTrang("Đã Duyệt");
                ccLogic.suaChiaCa(cc);
            }
            TienIchGiaoDien.hienThiThongBao(this, "Đã duyệt ca thành công!", "SUCCESS");
            taiDuLieuCaTuDatabase(() -> xoaRongForm(txtNgayApDung.getText()));
        } catch (Exception ex) {}
    }
    
    private void thucHienHuyCa() {
        if (danhSachCaDangChon.isEmpty()) return;
        TienIchGiaoDien.hienThiXacNhan(this, "Bạn có chắc chắn muốn HỦY ca làm việc này không?", () -> {
            try {
                for (ChiaCa cc : danhSachCaDangChon) {
                    cc.setTinhTrang("Đã hủy");
                    ccLogic.suaChiaCa(cc); 
                }
                TienIchGiaoDien.hienThiThongBao(this, "Đã hủy lịch phân công!", "SUCCESS");
                taiDuLieuCaTuDatabase(() -> xoaRongForm(txtNgayApDung.getText()));
            } catch (Exception ex) {}
        });
    }

    private void xuLyNutKetCa() {
        if (danhSachCaDangChon.isEmpty()) return;
        ChiaCa firstCa = danhSachCaDangChon.get(0);
        try {
            ccLogic.xuLyNghiepVuKetCa(firstCa, LocalDateTime.now());
            TienIchGiaoDien.hienThiThongBao(this, "Đã kết ca thủ công thành công!", "SUCCESS");
            taiDuLieuCaTuDatabase(() -> xoaRongForm(txtNgayApDung.getText()));
        } catch (Exception ex) {
            TienIchGiaoDien.hienThiThongBao(this, "Lỗi kết ca: " + ex.getMessage(), "ERROR");
        }
    }

    private void capNhatSoLieuRealtime() {
        if (txtMaCa == null || txtMaCa.getText().isEmpty()) return;
        if (!"Đang làm việc".equals(cbTinhTrang.getSelectedItem())) return; 
        String maCaHienTai = txtMaCa.getText().trim();

        new Thread(() -> {
            String sql = "SELECT TienBanHang, TienChenhLech FROM ChiaCa WHERE MaCa = ?";
            try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
                java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setString(1, maCaHienTai);
                java.sql.ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    java.math.BigDecimal banHang   = rs.getBigDecimal("TienBanHang");
                    java.math.BigDecimal chenhLech = rs.getBigDecimal("TienChenhLech");
                    String sBanHang   = (banHang != null) ? banHang.toPlainString() : "0.00";
                    String sChenhLech = (chenhLech != null) ? chenhLech.toPlainString() : "0.00";
                    
                    SwingUtilities.invokeLater(() -> {
                        if (!txtTienBanHang.getText().equals(sBanHang)) txtTienBanHang.setText(sBanHang);
                        if (!txtTienChenhLech.getText().equals(sChenhLech)) txtTienChenhLech.setText(sChenhLech);
                    });
                }
            } catch (Exception e) {}
        }).start();
    }

    private void xoaRongForm(String ngayFormat) {
        danhSachCaDangChon.clear(); 
        if(btnApDung != null) btnApDung.setVisible(true);
        if(btnHuyCa != null) btnHuyCa.setVisible(false);
        if(btnDuyetCa != null) btnDuyetCa.setVisible(false);

        txtNgayApDung.setText(ngayFormat);
        if (cbCaLam.getItemCount() > 0) cbCaLam.setSelectedIndex(0);
        
        txtMaCa.setText(TaoMaTuDongLogic.taoMaCa());
        danhSachMaNVDaChon.clear(); pnlDanhSachTag.removeAll();
        pnlDanhSachTag.revalidate(); pnlDanhSachTag.repaint();
        
        txtCheckIn.setText(""); txtCheckOut.setText("");
        txtTienDauCa.setText("0.00"); txtTienCuoiCa.setText("0.00");
        txtTienBanHang.setText("0.00"); txtTienChenhLech.setText("0.00");
        
        cbTinhTrang.setSelectedItem("Chưa Phân Công");
        if (timerRealtime != null) timerRealtime.stop();
    }

    private String layTenCa(String maLoaiCa) {
        for (LoaiCa lc : listLoaiCaAll) {
            if (lc.getMaLoaiCa().equals(maLoaiCa)) return lc.getTenCa();
        }
        return "Ca Unknown";
    }

    private JPanel taoLichRight() {
        pnlLichRight = new BangChiaCaRight(new LichInteractionListener() {
            @Override public void onNgayDuocChon(String f) { xoaRongForm(f); }
            @Override public void onCaDuocChon(String c, String f) { hienThiChiTietCaLam(c, f); }
        });
        return pnlLichRight;
    }

    private JPopupMenu taoPopupLich() {
        JPopupMenu p = new JPopupMenu(); p.setLayout(new BorderLayout());
        YearMonth[] ym = {YearMonth.now()};
        JPanel h = new JPanel(new BorderLayout());
        JButton pre = new JButton("◄"); JButton nxt = new JButton("►");
        pre.setMargin(new Insets(2, 2, 2, 2)); nxt.setMargin(new Insets(2, 2, 2, 2));
        pre.setFocusPainted(false); pre.setBackground(Color.WHITE); nxt.setFocusPainted(false); nxt.setBackground(Color.WHITE);
        JLabel l = new JLabel("", SwingConstants.CENTER); l.setFont(FONT_DAM);
        h.add(pre, BorderLayout.WEST); h.add(l, BorderLayout.CENTER); h.add(nxt, BorderLayout.EAST);
        JPanel g = new JPanel(new GridLayout(0, 7, 2, 2));
        Runnable d = () -> {
            g.removeAll(); l.setText(ym[0].getMonthValue() + "/" + ym[0].getYear());
            for (String s : new String[]{"T2","T3","T4","T5","T6","T7","CN"}) g.add(new JLabel(s, SwingConstants.CENTER));
            int st = ym[0].atDay(1).getDayOfWeek().getValue();
            for (int i=1; i<st; i++) g.add(new JLabel(""));
            for (int i=1; i<=ym[0].lengthOfMonth(); i++) {
                JButton b = new JButton(String.valueOf(i)); final int day = i;
                b.setBackground(Color.WHITE); b.setBorder(BorderFactory.createLineBorder(MAU_VIEN)); b.setFocusPainted(false);
                b.addActionListener(e -> { 
                    String ngayFmt = String.format("%02d/%02d/%d", day, ym[0].getMonthValue(), ym[0].getYear());
                    xoaRongForm(ngayFmt); 
                    p.setVisible(false); 
                });
                g.add(b);
            }
            g.revalidate(); g.repaint();
        };
        pre.addActionListener(e -> { ym[0]=ym[0].minusMonths(1); d.run(); }); nxt.addActionListener(e -> { ym[0]=ym[0].plusMonths(1); d.run(); });
        d.run(); p.add(h, BorderLayout.NORTH); p.add(g, BorderLayout.CENTER); return p;
    }

    public interface LichInteractionListener { void onNgayDuocChon(String f); void onCaDuocChon(String c, String f); }
    
    private class BangChiaCaRight extends JPanel {
        private YearMonth ym = YearMonth.now(); private JPanel grid; private JLabel lblYM;
        private Map<Integer, List<String>> mapCaLamHienTai = new HashMap<>(); 
        private LichInteractionListener listener;

        public BangChiaCaRight(LichInteractionListener l) {
            this.listener = l;
            setLayout(new BorderLayout(10, 10)); setOpaque(false);
            JPanel h = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); h.setOpaque(false);
            NutBoGoc p = new NutBoGoc("◄"); NutBoGoc n = new NutBoGoc("►");
            p.setPreferredSize(new Dimension(36,36)); n.setPreferredSize(new Dimension(36,36));
            p.setMargin(new Insets(2, 2, 2, 2)); n.setMargin(new Insets(2, 2, 2, 2));
            lblYM = new JLabel(); lblYM.setFont(FONT_THUONG);
            h.add(p); h.add(n); h.add(lblYM);
            grid = new JPanel(new GridLayout(0, 7, 1, 1)); grid.setBackground(MAU_VIEN);
            add(h, BorderLayout.NORTH); add(grid, BorderLayout.CENTER);
            p.addActionListener(e -> { ym=ym.minusMonths(1); taiDuLieuLenLich(); }); 
            n.addActionListener(e -> { ym=ym.plusMonths(1); taiDuLieuLenLich(); }); 
        }

        public void taiDuLieuLenLich() {
            if (cachedDataCa == null) return;
            mapCaLamHienTai.clear();
            
            // Xử lý thần tốc bằng Memory O(N)
            for (ChiaCa cc : cachedDataCa.dsChiaCa) {
                if (cc.getNgayLam() != null && cc.getNgayLam().getYear() == ym.getYear() 
                    && cc.getNgayLam().getMonthValue() == ym.getMonthValue()
                    && !cc.getTinhTrang().equals("Đã hủy")) {
                    
                    int ngay = cc.getNgayLam().getDayOfMonth();
                    String tenCa = layTenCa(cc.getMaLoaiCa());
                    
                    List<String> caTrongNgay = mapCaLamHienTai.getOrDefault(ngay, new ArrayList<>());
                    if (!caTrongNgay.contains(tenCa)) {
                        caTrongNgay.add(tenCa);
                        mapCaLamHienTai.put(ngay, caTrongNgay);
                    }
                }
            }
            draw();
        }

        private void draw() {
            grid.removeAll(); lblYM.setText("Tháng " + ym.getMonthValue() + ", " + ym.getYear());
            String[] thu = {"Thứ 2","Thứ 3","Thứ 4","Thứ 5","Thứ 6","Thứ 7","CN"};
            for (int i=0; i<7; i++) {
                JLabel t = new JLabel(thu[i], SwingConstants.CENTER); t.setFont(FONT_DAM);
                t.setOpaque(true); t.setBackground(Color.WHITE); t.setForeground(i >= 5 ? Color.RED : MAU_CHU_CHINH);
                t.setBorder(new EmptyBorder(10,0,10,0)); grid.add(t);
            }
            
            int st = ym.atDay(1).getDayOfWeek().getValue();
            for (int i=1; i<st; i++) grid.add(taoO(""));
            
            for (int i=1; i<=ym.lengthOfMonth(); i++) {
                JPanel pnlNgay = taoO(String.valueOf(i));
                final int finalNgay = i;
                
                pnlNgay.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        String dateStr = String.format("%02d/%02d/%d", finalNgay, ym.getMonthValue(), ym.getYear());
                        listener.onNgayDuocChon(dateStr);
                    }
                    public void mouseEntered(MouseEvent e) { pnlNgay.setBackground(new Color(240, 248, 255)); }
                    public void mouseExited(MouseEvent e) { pnlNgay.setBackground(Color.WHITE); }
                });
                
                if (mapCaLamHienTai.containsKey(i)) {
                    for (String tenCa : mapCaLamHienTai.get(i)) {
                        JPanel tagCa = new TagCaLamGiamLuoc(tenCa);
                        tagCa.addMouseListener(new MouseAdapter() {
                            public void mouseClicked(MouseEvent e) {
                                String dateStr = String.format("%02d/%02d/%d", finalNgay, ym.getMonthValue(), ym.getYear());
                                listener.onCaDuocChon(tenCa, dateStr);
                                e.consume(); 
                            }
                        });
                        pnlNgay.add(tagCa);
                    }
                }
                grid.add(pnlNgay);
            }
            grid.revalidate(); grid.repaint();
        }

        private JPanel taoO(String s) {
            JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(s.isEmpty() ? MAU_NEN : Color.WHITE); p.setBorder(new EmptyBorder(5,5,5,5));
            if (!s.isEmpty()) { 
                JLabel l = new JLabel(s); l.setFont(FONT_THUONG); p.add(l); 
                p.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            return p;
        }
        
        private class TagCaLamGiamLuoc extends JPanel {
            private String text;
            public TagCaLamGiamLuoc(String text) {
                this.text = text; setOpaque(false); setMaximumSize(new Dimension(200, 25)); setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (text.contains("Đang làm việc")) { g2.setColor(new Color(34, 197, 94)); } 
                else if (text.contains("Đã kết ca") || text.contains("Đã hoàn thành")) { g2.setColor(new Color(156, 163, 175)); } 
                else {
                    if (text.toLowerCase().contains("sáng")) g2.setColor(new Color(0, 123, 255)); 
                    else if (text.toLowerCase().contains("chiều")) g2.setColor(new Color(255, 153, 51)); 
                    else g2.setColor(new Color(100, 116, 139)); 
                }

                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6); g2.setColor(Color.WHITE);
                String tenHienThi = text.split("-")[0].trim(); g2.setFont(FONT_THUONG.deriveFont(14f)); 
                g2.drawString(tenHienThi, 5, 17); g2.dispose();
            }
        }
    }
}