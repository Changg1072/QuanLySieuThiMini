package Logic;

import Dao.ChiTietLoHangDAO;
import Data.ChiTietLoHang;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ChiTietLoHangLogic {
    private ChiTietLoHangDAO dao = ChiTietLoHangDAO.getInstance();

    public List<ChiTietLoHang> layChiTietTheoMaLo(String maLoHang) {
        if (maLoHang == null || maLoHang.trim().isEmpty()) return null;
        return dao.layChiTietTheoMaLo(maLoHang);
    }

    public void themChiTietLoHang(ChiTietLoHang ct) throws Exception {
        kiemTraLoi(ct);
        
        boolean daTonTai = layChiTietTheoMaLo(ct.getMaLoHang()).stream()
                .anyMatch(item -> item.getMaSP().equalsIgnoreCase(ct.getMaSP()));
        if (daTonTai) {
            throw new Exception("Lỗi: Sản phẩm này đã được thêm vào phiếu nhập hiện tại. Vui lòng chọn dòng đó để 'Sửa' số lượng thay vì thêm mới!");
        }

        Dao.SanPhamDAO spDao = Dao.SanPhamDAO.getInstance();
        Data.SanPham spGoc = spDao.laySanPhamTheoMa(ct.getMaSP());
        
        if (spGoc != null && ct.getGiaNhap().compareTo(spGoc.getGiaBan()) >= 0) {
            throw new Exception("Cảnh báo lỗ vốn: Giá nhập (" + String.format("%,.0f", ct.getGiaNhap()) + "đ) đang cao hơn hoặc bằng Giá bán (" + String.format("%,.0f", spGoc.getGiaBan()) + "đ)!");
        }
        
        boolean thanhCong = dao.themChiTietLoHang(ct);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Thêm chi tiết lô hàng thất bại!");
        }
        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
    }

    public void suaChiTietLoHang(ChiTietLoHang ct) throws Exception {
        kiemTraLoi(ct);
        
        // COPY TỪ HÀM THÊM XUỐNG ĐỂ CHỐNG "LÁCH LUẬT" KHI SỬA
        Dao.SanPhamDAO spDao = Dao.SanPhamDAO.getInstance();
        Data.SanPham spGoc = spDao.laySanPhamTheoMa(ct.getMaSP());
        
        if (spGoc != null && ct.getGiaNhap().compareTo(spGoc.getGiaBan()) >= 0) {
            throw new Exception("Cảnh báo lỗ vốn: Giá nhập mới sửa (" + String.format("%,.0f", ct.getGiaNhap()) + "đ) đang cao hơn hoặc bằng Giá bán (" + String.format("%,.0f", spGoc.getGiaBan()) + "đ)!");
        }

        boolean thanhCong = dao.suaChiTietLoHang(ct);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Cập nhật chi tiết lô hàng thất bại!");
        }
        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
    }

    public void xoaChiTietLoHang(String maLoHang, String maSP) throws Exception {
        if (maLoHang == null || maSP == null || maLoHang.trim().isEmpty() || maSP.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn chính xác món hàng cần xóa khỏi phiếu nhập!");
        }
        
        boolean thanhCong = dao.xoaChiTietLoHang(maLoHang, maSP);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể xóa chi tiết lô hàng này!");
        }
        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
    }
    
    public void truSoLuongTon(String maLoHang, String maSP, int soLuongBan) throws Exception {
        if (soLuongBan <= 0) {
            throw new Exception("Số lượng bán phải lớn hơn 0!");
        }
        
        boolean thanhCong = dao.truSoLuongTon(maLoHang, maSP, soLuongBan);
        if (!thanhCong) {
            throw new Exception("Lỗi trừ kho: Sản phẩm " + maSP + " trong lô " + maLoHang + " không đủ số lượng tồn để bán!");
        }
    }
    // Hàm lấy danh sách các lô hàng của một sản phẩm để phục vụ trừ kho khi thanh toán
    public java.util.List<Object[]> layLichSuNhapTheoSP(String maSP) {
        if (maSP == null || maSP.trim().isEmpty()) {
            return new java.util.ArrayList<>(); // Trả về list rỗng nếu mã SP không hợp lệ
        }
        
        // Gọi xuống tầng DAO để lấy dữ liệu từ SQL
        return Dao.ChiTietLoHangDAO.getInstance().layLichSuNhapTheoSP(maSP);
    }

    private void kiemTraLoi(ChiTietLoHang ct) throws Exception {
        if (ct.getMaLoHang() == null || ct.getMaLoHang().trim().isEmpty()) {
            throw new Exception("Lỗi hệ thống: Chưa xác định được Mã Lô Hàng!");
        }
        if (ct.getMaSP() == null || ct.getMaSP().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Sản phẩm cần nhập!");
        }
        
        if (ct.getGiaNhap() == null || ct.getGiaNhap().compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Giá nhập của sản phẩm phải lớn hơn 0 VNĐ!");
        }
        if (ct.getSoLuongNhap() <= 0) {
            throw new Exception("Số lượng nhập kho phải lớn hơn 0!");
        }
        
        if (ct.getSoLuongTon() < 0 || ct.getSoLuongTon() > ct.getSoLuongNhap()) {
            throw new Exception("Số lượng tồn kho không hợp lệ (Phải từ 0 đến tối đa bằng số lượng nhập)!");
        }
        
        if (ct.getNSX() == null) {
            throw new Exception("Vui lòng nhập Ngày sản xuất!");
        }
        if (ct.getHSD() == null) {
            throw new Exception("Vui lòng nhập Hạn sử dụng!");
        }
        
        if (ct.getNSX().isAfter(ct.getHSD()) || ct.getNSX().isEqual(ct.getHSD())) {
            throw new Exception("Logic thời gian sai: Ngày sản xuất phải TRƯỚC Hạn sử dụng!");
        }
        
        if (ct.getHSD().isBefore(LocalDate.now())) {
            throw new Exception("Từ chối nhập kho: Sản phẩm này đã hết hạn sử dụng!");
        }
    }
}