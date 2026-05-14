package Logic;

import Dao.ChiTietLoHangDAO;
import Dao.ChiTietPhieuHuyDAO;
import Data.ChiTietLoHang;
import Data.ChiTietPhieuHuy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ChiTietPhieuHuyLogic {
    private static final ChiTietPhieuHuyLogic instance = new ChiTietPhieuHuyLogic();
    private final ChiTietPhieuHuyDAO chiTietPhieuHuyDao = ChiTietPhieuHuyDAO.getInstance();
    private final ChiTietLoHangDAO chiTietLoHangDao = ChiTietLoHangDAO.getInstance();

    private ChiTietPhieuHuyLogic() {}

    public static ChiTietPhieuHuyLogic getInstance() {
        return instance;
    }

    public List<ChiTietPhieuHuy> layDanhSachChiTietTheoMaPhieu(String maPhieuHuy) {
        if (maPhieuHuy == null || maPhieuHuy.trim().isEmpty()) return null;
        return chiTietPhieuHuyDao.layDanhSachChiTietTheoMaPhieu(maPhieuHuy);
    }

    /**
     * Thêm chi tiết tiêu hủy: Validate -> Tính giá trị hủy -> Trừ tồn kho -> Lưu DB
     */
    public void themChiTietPhieuHuy(ChiTietPhieuHuy ct) throws Exception {
        kiemTraLoiValidate(ct);

        // 1. Lấy thông tin Lô hàng thực tế
        List<ChiTietLoHang> dsLo = chiTietLoHangDao.layChiTietTheoMaLo(ct.getMaLoHang());
        ChiTietLoHang loHienTai = dsLo.stream()
                .filter(lo -> lo.getMaSP().equalsIgnoreCase(ct.getMaSP()))
                .findFirst()
                .orElseThrow(() -> new Exception("Lỗi: Không tìm thấy sản phẩm " + ct.getMaSP() + " trong lô " + ct.getMaLoHang()));

        // 2. Kiểm tra Âm Kho (Chống xuất quá tồn kho)
        if (ct.getSoLuongHuy() > loHienTai.getSoLuongTon()) {
            throw new Exception("Từ chối tiêu hủy: Số lượng hủy (" + ct.getSoLuongHuy() + 
                                ") lớn hơn tồn kho thực tế của lô (" + loHienTai.getSoLuongTon() + ")!");
        }

        // 3. Tự động kiểm tra Date và Append vào lý do
        if (loHienTai.getHSD() != null && (loHienTai.getHSD().isBefore(LocalDate.now()) || loHienTai.getHSD().isEqual(LocalDate.now()))) {
            String lyDoCu = ct.getLyDoChiTiet() != null ? ct.getLyDoChiTiet() : "";
            ct.setLyDoChiTiet("[HÀNG HẾT HẠN SỬ DỤNG] " + lyDoCu);
        }

        // 4. Tính Giá Trị Hủy (Retail Audit: Tính theo Giá Nhập, không phải Giá Bán)
        BigDecimal giaTriHuy = loHienTai.getGiaNhap().multiply(new BigDecimal(ct.getSoLuongHuy()));
        ct.setGiaTriHuy(giaTriHuy); // Tự động set, không cho User nhập tay

        // 5. Cập nhật tồn kho (Trừ kho thực tế)
        boolean isTruKho = chiTietLoHangDao.truSoLuongTon(ct.getMaLoHang(), ct.getMaSP(), ct.getSoLuongHuy());
        if (!isTruKho) {
            throw new Exception("Lỗi hệ thống: Không thể trừ tồn kho, có thể do xung đột dữ liệu. Vui lòng thử lại!");
        }

        // 6. Lưu vào DB
        boolean isLuu = chiTietPhieuHuyDao.themChiTietPhieuHuy(ct);
        if (!isLuu) {
            // Rollback lại kho nếu lưu chi tiết phiếu thất bại
            chiTietLoHangDao.congSoLuongTon(ct.getMaLoHang(), ct.getMaSP(), ct.getSoLuongHuy());
            throw new Exception("Lỗi Database: Không thể lưu chi tiết tiêu hủy!");
        }

        // Xóa cache kho (nếu có)
        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
    }

    /**
     * Hoàn trả tồn kho (Rollback) khi Hủy phiếu tiêu hủy đang tạo dở
     */
    public void rollbackTonKho(String maPhieuHuy) throws Exception {
        List<ChiTietPhieuHuy> dsChiTiet = layDanhSachChiTietTheoMaPhieu(maPhieuHuy);
        for (ChiTietPhieuHuy ct : dsChiTiet) {
            boolean rollbackThanhCong = chiTietLoHangDao.congSoLuongTon(ct.getMaLoHang(), ct.getMaSP(), ct.getSoLuongHuy());
            if (!rollbackThanhCong) {
                throw new Exception("Cảnh báo nghiêm trọng: Lỗi rollback tồn kho cho sản phẩm " + ct.getMaSP() + " (Lô: " + ct.getMaLoHang() + ")");
            }
        }
    }

    private void kiemTraLoiValidate(ChiTietPhieuHuy ct) throws Exception {
        if (ct.getMaPhieuHuy() == null || ct.getMaPhieuHuy().trim().isEmpty()) {
            throw new Exception("Lỗi: Mã phiếu hủy không được để trống!");
        }
        if (ct.getMaLoHang() == null || ct.getMaLoHang().trim().isEmpty()) {
            throw new Exception("Lỗi: Chưa xác định Lô hàng cần hủy!");
        }
        if (ct.getMaSP() == null || ct.getMaSP().trim().isEmpty()) {
            throw new Exception("Lỗi: Vui lòng chọn Sản phẩm cần hủy!");
        }
        if (ct.getSoLuongHuy() <= 0) {
            throw new Exception("Lỗi thao tác: Số lượng tiêu hủy phải lớn hơn 0!");
        }
    }
}