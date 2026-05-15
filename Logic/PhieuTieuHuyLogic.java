package Logic;

import Dao.PhieuTieuHuyDAO;
import Dao.ChiTietLoHangDAO;
import Data.ChiTietPhieuHuy;
import Data.PhieuTieuHuy;
import Data.ChiTietLoHang;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PhieuTieuHuyLogic {
    private static final PhieuTieuHuyLogic instance = new PhieuTieuHuyLogic();
    private final PhieuTieuHuyDAO phieuTieuHuyDao = PhieuTieuHuyDAO.getInstance();
    private final ChiTietPhieuHuyLogic chiTietLogic = ChiTietPhieuHuyLogic.getInstance();

    private PhieuTieuHuyLogic() {}

    public static PhieuTieuHuyLogic getInstance() {
        return instance;
    }

    // =========================================================================
    // 1. NGHIỆP VỤ VÒNG ĐỜI PHIẾU TIÊU HỦY
    // =========================================================================

    /**
     * Khởi tạo phiếu tiêu hủy mới
     */
    public void taoPhieuTieuHuy(PhieuTieuHuy phieu) throws Exception {
        if (phieu.getMaNV() == null || phieu.getMaNV().trim().isEmpty()) {
            throw new Exception("Lỗi: Không xác định được Nhân viên thao tác!");
        }

        // Tự động sinh mã nếu chưa có
        if (phieu.getMaPhieuHuy() == null || phieu.getMaPhieuHuy().isEmpty()) {
            phieu.setMaPhieuHuy(TaoMaTuDongLogic.taoMaPhieuTieuHuy());
        }

        phieu.setNgayTao(LocalDateTime.now());
        
        // BỎ 2 DÒNG RESET CỨNG, HOẶC CHỈ GÁN 0 NẾU GUI KHÔNG TRUYỀN XUỐNG
        if (phieu.getTongGiaTriHuy() == null) {
            phieu.setTongGiaTriHuy(BigDecimal.ZERO);
        }
        
        phieu.setTrangThaiHuy("CHO_XU_LY"); // Mặc định ở trạng thái Chờ xử lý

        boolean isTao = phieuTieuHuyDao.themPhieuTieuHuy(phieu);
        if (!isTao) {
            throw new Exception("Lỗi hệ thống: Không thể khởi tạo Phiếu Tiêu Hủy!");
        }
    }
    public void hoanTatPhieuTieuHuy(String maPhieuHuy) throws Exception {
        List<ChiTietPhieuHuy> dsChiTiet = chiTietLogic.layDanhSachChiTietTheoMaPhieu(maPhieuHuy);
        
        if (dsChiTiet == null || dsChiTiet.isEmpty()) {
            throw new Exception("Từ chối hoàn tất: Phiếu tiêu hủy hiện đang trống. Vui lòng thêm sản phẩm cần hủy!");
        }

        int tongSoLuong = 0;
        BigDecimal tongGiaTri = BigDecimal.ZERO;

        for (ChiTietPhieuHuy ct : dsChiTiet) {
            tongSoLuong += ct.getSoLuongHuy();
            tongGiaTri = tongGiaTri.add(ct.getGiaTriHuy());
        }

        // Hiện tại dùng hàm cập nhật trạng thái
        boolean isCapNhat = phieuTieuHuyDao.capNhatTrangThai(maPhieuHuy, "DA_TIEU_HUY");
        if (!isCapNhat) {
            throw new Exception("Lỗi hệ thống: Cập nhật trạng thái phiếu thất bại!");
        }
        
        // Lưu ý: Việc cập nhật ThongKe.ThietHai sẽ được gom chung lúc Kế toán chốt báo cáo tháng
        // Vì trong ThongKeDAO hiện tại đang dùng query realtime từ KiemKeKho và ChiTietLoHang.
    }

    /**
     * Hủy bỏ thao tác tạo phiếu: Hoàn trả lại số lượng kho
     */
    public void huyPhieuTieuHuy(String maPhieuHuy) throws Exception {
        List<PhieuTieuHuy> dsPhieu = phieuTieuHuyDao.layDanhSachPhieuTieuHuy();
        PhieuTieuHuy phieuHienTai = dsPhieu.stream()
                .filter(p -> p.getMaPhieuHuy().equals(maPhieuHuy))
                .findFirst()
                .orElseThrow(() -> new Exception("Không tìm thấy phiếu tiêu hủy này!"));

        if ("DA_TIEU_HUY".equals(phieuHienTai.getTrangThaiHuy())) {
            throw new Exception("Từ chối thao tác: Không thể hủy phiếu đã chốt sổ (ĐÃ TIÊU HỦY). Cần quyền Admin để rollback!");
        }

        // 1. Hoàn trả tồn kho từ chi tiết
        chiTietLogic.rollbackTonKho(maPhieuHuy);

        // 2. Đổi trạng thái phiếu
        boolean isHuy = phieuTieuHuyDao.capNhatTrangThai(maPhieuHuy, "DA_HUY");
        if (!isHuy) {
            throw new Exception("Lỗi hệ thống: Không thể cập nhật trạng thái hủy phiếu!");
        }
    }

    // =========================================================================
    // 2. NGHIỆP VỤ THỐNG KÊ & GỢI Ý HÀNG HÓA
    // =========================================================================

    /**
     * Quét toàn bộ kho, trả về danh sách các mặt hàng đã Hết Hạn Sử Dụng cần tiêu hủy ngay
     */
    public List<ChiTietLoHang> layDanhSachHangCanHuy() {
        ChiTietLoHangDAO loHangDao = ChiTietLoHangDAO.getInstance();
        List<ChiTietLoHang> toanBoKho = loHangDao.layDanhSachChiTietLoHang();
        
        LocalDate homNay = LocalDate.now();

        return toanBoKho.stream()
                .filter(lo -> lo.getSoLuongTon() > 0) // Chỉ xét lô còn hàng
                .filter(lo -> lo.getHSD() != null && (lo.getHSD().isBefore(homNay) || lo.getHSD().isEqual(homNay)))
                .collect(Collectors.toList());
    }

    /**
     * Thống kê tổng thiệt hại tiêu hủy theo tháng hiện tại
     */
    public BigDecimal thongKeThietHaiTheoThang(int thang, int nam) {
        List<PhieuTieuHuy> dsPhieu = phieuTieuHuyDao.layDanhSachPhieuTieuHuy();
        
        return dsPhieu.stream()
                .filter(p -> "DA_TIEU_HUY".equals(p.getTrangThaiHuy())) // Chỉ tính phiếu đã chốt
                .filter(p -> p.getNgayTao() != null && p.getNgayTao().getMonthValue() == thang && p.getNgayTao().getYear() == nam)
                .map(PhieuTieuHuy::getTongGiaTriHuy)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}