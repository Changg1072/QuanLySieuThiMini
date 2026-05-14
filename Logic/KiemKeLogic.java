package Logic;

import Dao.ChiTietLoHangDAO;
import Dao.KiemKeKhoDAO;
import Dao.SanPhamDAO;
import Data.ChiTietLoHang;
import Data.KiemKeKho;
import Data.SanPham;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CORE LOGIC QUẢN LÝ KIỂM KÊ & KHO (CHUẨN RETAIL MINIMART)
 * Tích hợp FEFO, Cycle Counting và Cảnh báo thất thoát thông minh.
 */
public class KiemKeLogic {
    private static final KiemKeLogic instance = new KiemKeLogic();

    // Các ngưỡng cấu hình hệ thống (Có thể đưa vào DB Settings trong thực tế)
    private static final int MUC_CANH_BAO_TON_KHO = 5; // Dưới 5 SP -> Cảnh báo
    private static final int NGAY_CANH_BAO_HET_HAN = 7; // Còn 7 ngày hết hạn -> Xả hàng
    private static final int SO_LAN_LECH_KHO_BAT_THUONG = 3; // Lệch 3 lần/tháng -> Báo động đỏ

    private final KiemKeKhoDAO kiemKeDao = KiemKeKhoDAO.getInstance();
    private final ChiTietLoHangDAO loHangDao = ChiTietLoHangDAO.getInstance();
    private final SanPhamDAO sanPhamDao = SanPhamDAO.getInstance();

    private KiemKeLogic() {}

    public static KiemKeLogic getInstance() {
        return instance;
    }

    // =========================================================================
    // 1. NGHIỆP VỤ CYCLE COUNTING (KIỂM KÊ XOAY VÒNG THEO LỊCH)
    // =========================================================================
    
    /**
     * Lấy danh sách sản phẩm cần kiểm kê hôm nay dựa theo lịch phân công.
     * VD: Thứ 2 kiểm Đồ uống, Thứ 3 kiểm Đông lạnh...
     */
    public List<SanPham> taoDanhSachKiemKeCycleCounting(String maLoaiCanKiemKe) {
        // Lấy toàn bộ SP
        List<SanPham> allSp = sanPhamDao.layDanhSachSanPham();
        // Lọc theo loại hàng được giao kiểm kê hôm nay
        return allSp.stream()
                .filter(sp -> sp.getMaLoai().equalsIgnoreCase(maLoaiCanKiemKe))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 2. NGHIỆP VỤ KIỂM KÊ THỰC TẾ & XỬ LÝ LỆCH KHO (CÓ TRANSACTION TRÁNH DEADLOCK)
    // =========================================================================

    /**
     * Xử lý kết quả đếm thực tế của nhân viên.
     * Logic: Tính lệch kho -> Yêu cầu lý do -> Lưu lịch sử -> Khớp lại tồn hệ thống.
     */
    public void xuLyKetQuaKiemKe(KiemKeKho phieuKiemKe) throws Exception {
        validateDuLieuKiemKe(phieuKiemKe);

        // Đảm bảo lấy tồn hệ thống realtime ngay tại thời điểm submit (tránh nhân viên đếm xong ngâm phiếu)
        List<ChiTietLoHang> dsLo = loHangDao.layChiTietTheoMaLo(phieuKiemKe.getMaLoHang());
        ChiTietLoHang loHienTai = dsLo.stream()
                .filter(lo -> lo.getMaSP().equalsIgnoreCase(phieuKiemKe.getMaSP()))
                .findFirst()
                .orElseThrow(() -> new Exception("Lỗi: Không tìm thấy Lô hàng hệ thống!"));

        int tonHeThong = loHienTai.getSoLuongTon();
        phieuKiemKe.setSoLuongHeThong(tonHeThong);

        int lechKho = phieuKiemKe.getSoLuongThucTe() - tonHeThong;

        // Phân tích nguyên nhân nếu có sự cố lệch kho
        if (lechKho != 0) {
            String phanLoai = (lechKho > 0) ? "LỆCH DƯ (+)" : "LỆCH ÂM (-)";
            if (phieuKiemKe.getLyDo() == null || phieuKiemKe.getLyDo().trim().isEmpty()) {
                throw new Exception("Bắt buộc nhập lý do! Hệ thống phát hiện " + phanLoai + " " + Math.abs(lechKho) + " sản phẩm.\n"
                        + "Gợi ý: Lệch âm (Bán thiếu scan, thất thoát, rách/hỏng). Lệch dương (Nhập sai, tráo hàng).");
            }
        } else {
            phieuKiemKe.setLyDo("Khớp kho.");
        }

        // Gọi DAO thực thi Transaction (Vừa tạo phiếu kiểm kê, vừa Update lại ChiTietLoHang)
        // Lưu ý: KiemKeKhoDAO.duyetVaCanBangKho phải chạy khối lệnh SQL trong 1 Transaction (đã có trong file DAO của bạn)
        boolean isLuuPhieu = kiemKeDao.lapPhieuKiemKe(phieuKiemKe);
        if (isLuuPhieu) {
            boolean isCanBang = kiemKeDao.duyetVaCanBangKho(phieuKiemKe);
            if (!isCanBang) {
                throw new Exception("Lỗi Database: Lưu lịch sử kiểm kê thành công nhưng không thể cân bằng kho!");
            }
        } else {
            throw new Exception("Lỗi Database: Không thể tạo phiếu kiểm kê!");
        }

        // Cập nhật lại Cache nếu có
        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
    }

    // =========================================================================
    // 3. THUẬT TOÁN XUẤT KHO FEFO (FIRST EXPIRED FIRST OUT)
    // =========================================================================

    /**
     * Bán hàng tự động cấn trừ vào các lô hàng theo nguyên tắc: Lô nào gần hết hạn thì xuất trước.
     */
    public void xuatHangTheoFefo(String maSP, int soLuongCanBan) throws Exception {
        if (soLuongCanBan <= 0) throw new Exception("Số lượng xuất kho phải > 0");

        // 1. Lấy tất cả các lô của mã SP này
        List<Object[]> rawData = loHangDao.layLichSuNhapTheoSP(maSP);
        List<ChiTietLoHang> dsLoHang = mapRawDataToLoHang(rawData, maSP);

        // 2. Lọc bỏ các lô đã hết hàng, và SẮP XẾP TĂNG DẦN THEO HẠN SỬ DỤNG (FEFO)
        List<ChiTietLoHang> danhSachLoKhaDung = dsLoHang.stream()
                .filter(lo -> lo.getSoLuongTon() > 0)
                .sorted(Comparator.comparing(ChiTietLoHang::getHSD, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // 3. Kiểm tra tổng tồn kho có đủ bán không
        int tongTon = danhSachLoKhaDung.stream().mapToInt(ChiTietLoHang::getSoLuongTon).sum();
        if (tongTon < soLuongCanBan) {
            throw new Exception("Hệ thống báo HẾT HÀNG! Yêu cầu xuất " + soLuongCanBan + " nhưng tổng tồn kho chỉ còn " + tongTon);
        }

        // 4. Bắt đầu thuật toán cấn trừ FEFO
        int soLuongConLai = soLuongCanBan;
        for (ChiTietLoHang lo : danhSachLoKhaDung) {
            if (soLuongConLai <= 0) break; // Đã lấy đủ hàng

            int soLuongTruCaLo = Math.min(lo.getSoLuongTon(), soLuongConLai);
            
            // Gọi xuống DAO để Update (Hàm truSoLuongTon của bạn đã có tính năng WHERE SoLuongTon >= soLuongBan để tránh âm kho)
            boolean truThanhCong = loHangDao.truSoLuongTon(lo.getMaLoHang(), maSP, soLuongTruCaLo);
            
            if (truThanhCong) {
                soLuongConLai -= soLuongTruCaLo;
            } else {
                throw new Exception("Lỗi đồng bộ (Concurrency): Lô hàng " + lo.getMaLoHang() + " vừa bị thay đổi bởi người khác. Vui lòng thử lại!");
            }
        }
    }

    // =========================================================================
    // 4. HỆ THỐNG CẢNH BÁO (ALERTS) & PHÂN TÍCH
    // =========================================================================

    /**
     * Quét hệ thống trả về danh sách cảnh báo (Hết hàng, Cận date)
     */
    public List<String> quetCanhBaoHeThong() {
        List<String> canhBao = new ArrayList<>();
        List<SanPham> dsSanPham = sanPhamDao.layDanhSachSanPham();
        LocalDate homNay = LocalDate.now();

        for (SanPham sp : dsSanPham) {
            List<Object[]> rawData = loHangDao.layLichSuNhapTheoSP(sp.getMaSP());
            List<ChiTietLoHang> dsLo = mapRawDataToLoHang(rawData, sp.getMaSP());

            int tongTonKho = 0;
            for (ChiTietLoHang lo : dsLo) {
                tongTonKho += lo.getSoLuongTon();

                // Cảnh báo CẬN DATE
                if (lo.getSoLuongTon() > 0 && lo.getHSD() != null) {
                    long soNgayConLai = ChronoUnit.DAYS.between(homNay, lo.getHSD());
                    if (soNgayConLai < 0) {
                        canhBao.add("🔴 NGHIÊM TRỌNG: SP " + sp.getTenSP() + " (Lô " + lo.getMaLoHang() + ") ĐÃ HẾT HẠN " + Math.abs(soNgayConLai) + " ngày nhưng vẫn còn trên quầy!");
                    } else if (soNgayConLai <= NGAY_CANH_BAO_HET_HAN) {
                        canhBao.add("🟡 CẬN DATE: SP " + sp.getTenSP() + " (Lô " + lo.getMaLoHang() + ") sẽ hết hạn sau " + soNgayConLai + " ngày. Yêu cầu dán tem giảm giá!");
                    }
                }
            }

            // Cảnh báo SẮP HẾT HÀNG
            if (tongTonKho <= MUC_CANH_BAO_TON_KHO) {
                canhBao.add("🟠 SẮP HẾT HÀNG: " + sp.getTenSP() + " hiện chỉ còn " + tongTonKho + " " + sp.getDonViTinh() + ". Cần lên đơn nhập hàng gấp!");
            }
        }
        return canhBao;
    }

    /**
     * Phân tích gian lận/lỗi nghiệp vụ: Phát hiện những sản phẩm liên tục bị chênh lệch kho.
     */
    public List<String> phatHienBatThuongKiemKe() {
        List<String> batThuong = new ArrayList<>();
        List<KiemKeKho> dsKiemKe = kiemKeDao.layDanhSachKiemKe();
        
        // Group by MaSP và đếm số lần Lệch (SoLuongHeThong != SoLuongThucTe)
        Map<String, Long> tanSuatLech = dsKiemKe.stream()
                .filter(kk -> kk.getSoLuongThucTe() != kk.getSoLuongHeThong())
                .collect(Collectors.groupingBy(KiemKeKho::getMaSP, Collectors.counting()));

        for (Map.Entry<String, Long> entry : tanSuatLech.entrySet()) {
            if (entry.getValue() >= SO_LAN_LECH_KHO_BAT_THUONG) {
                SanPham sp = sanPhamDao.laySanPhamTheoMa(entry.getKey());
                String tenSP = sp != null ? sp.getTenSP() : entry.getKey();
                batThuong.add("🚨 BÁO ĐỘNG ĐỎ: Mặt hàng '" + tenSP + "' đã bị lệch kho " + entry.getValue() + " lần. Nghi ngờ thất thoát hoặc nhân viên thao tác sai quy trình!");
            }
        }
        return batThuong;
    }

    // =========================================================================
    // HÀM HỖ TRỢ (HELPER METHODS)
    // =========================================================================

    private void validateDuLieuKiemKe(KiemKeKho kk) throws Exception {
        if (kk.getMaSP() == null || kk.getMaLoHang() == null) {
            throw new Exception("Phải xác định rõ Mã Sản Phẩm và Mã Lô Hàng khi kiểm kê.");
        }
        if (kk.getSoLuongThucTe() < 0) {
            throw new Exception("Lỗi thao tác: Số lượng đếm thực tế không được phép âm!");
        }
    }

    /**
     * Chuyển đổi Object[] từ DAO thành Data Model (Do thiết kế DAO cũ của bạn trả về Object[])
     */
    private List<ChiTietLoHang> mapRawDataToLoHang(List<Object[]> rawData, String maSP) {
        List<ChiTietLoHang> ds = new ArrayList<>();
        for (Object[] row : rawData) {
            ChiTietLoHang lo = new ChiTietLoHang();
            lo.setMaLoHang((String) row[0]);
            lo.setMaSP(maSP);
            lo.setSoLuongTon((Integer) row[5]);
            lo.setHSD((LocalDate) row[6]);
            ds.add(lo);
        }
        return ds;
    }
}