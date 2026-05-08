package Logic;

import Dao.ChiTietCsHoaDonDAO;
import Data.ChiTietCsHoaDon;

import java.math.BigDecimal;
import java.util.List;

public class ChiTietCsHoaDonLogic {
    private ChiTietCsHoaDonDAO dao = ChiTietCsHoaDonDAO.getInstance();

    public List<ChiTietCsHoaDon> layChiTietTheoMaTraHang(String maTraHang) {
        return dao.layChiTietTheoMaTraHang(maTraHang);
    }

    // ==========================================================
    // XỬ LÝ TRẢ HÀNG HÀNG LOẠT (Đã đồng bộ với Batch Insert của DAO)
    // ==========================================================
    public void themDanhSachChiTietTraHang(List<ChiTietCsHoaDon> dsTra, List<String> dsMaLoHangGoc) throws Exception {
        if (dsTra == null || dsTra.isEmpty()) {
            throw new Exception("Lỗi: Danh sách trả hàng đang trống!");
        }
        if (dsMaLoHangGoc == null || dsTra.size() != dsMaLoHangGoc.size()) {
            throw new Exception("Lỗi dữ liệu: Số lượng mã lô hàng không khớp với số lượng món hàng trả!");
        }

        // 1. Kiểm tra lỗi từng món đầu vào
        for (ChiTietCsHoaDon ct : dsTra) {
            kiemTraLoi(ct);
        }

        // 2. TRẢ HÀNG VÀO KHO TRƯỚC
        for (int i = 0; i < dsTra.size(); i++) {
            ChiTietCsHoaDon ct = dsTra.get(i);
            String maLoHang = dsMaLoHangGoc.get(i);
            
            boolean traKhoThanhCong = QuanLyKhoCacheLogic.getInstance().traHangCongTonKho(maLoHang, ct.getMaSP(), ct.getSoLuongTra());
            if (!traKhoThanhCong) {
                // Nếu lỗi kho, tiến hành Rollback (Bán lại để trừ đi số lượng mấy món đã lỡ cộng trước đó)
                for (int j = 0; j < i; j++) {
                    QuanLyKhoCacheLogic.getInstance().banHangTruTonKho(dsMaLoHangGoc.get(j), dsTra.get(j).getMaSP(), dsTra.get(j).getSoLuongTra());
                }
                throw new Exception("Cảnh báo: Không thể cộng dồn hàng trả về kệ cho Sản phẩm '" + ct.getMaSP() + "'. Đã hoàn tác toàn bộ lệnh trả hàng!");
            }
        }

        // 3. GHI NHẬN VÀO SỔ SÁCH KẾ TOÁN (DB)
        boolean thanhCong = dao.themDanhSachChiTietTraHang(dsTra);
        if (!thanhCong) {
            // Nếu lưu SQL thất bại -> Trừ lại kho để Rollback
            for (int i = 0; i < dsTra.size(); i++) {
                QuanLyKhoCacheLogic.getInstance().banHangTruTonKho(dsMaLoHangGoc.get(i), dsTra.get(i).getMaSP(), dsTra.get(i).getSoLuongTra());
            }
            throw new Exception("Lỗi hệ thống: Ghi nhận thông tin trả hàng vào CSDL thất bại!");
        }

        // 4. CHỐT ĐƠN -> GỌI THỦ KHO CẬP NHẬT LẠI KỆ HÀNG TRÊN RAM (1 LẦN DUY NHẤT)
        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
    }

    private void kiemTraLoi(ChiTietCsHoaDon ct) throws Exception {
        if (ct.getMaTraHang() == null || ct.getMaTraHang().trim().isEmpty()) {
            throw new Exception("Lỗi: Mã Phiếu Trả Hàng không được để trống!");
        }
        
        if (ct.getMaSP() == null || ct.getMaSP().trim().isEmpty()) {
            throw new Exception("Lỗi: Chưa xác định được Sản phẩm khách muốn trả!");
        }
        
        if (ct.getSoLuongTra() <= 0) {
            throw new Exception("Số lượng hàng trả lại phải lớn hơn 0!");
        }
        
        if (ct.getThanhTienHoanTra() == null || ct.getThanhTienHoanTra().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lỗi Kế toán: Số tiền hoàn trả cho khách không được là số âm!");
        }
    }
}