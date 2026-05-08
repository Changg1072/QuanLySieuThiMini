package Logic;

import Dao.LoHangDAO;
import Data.LoHang;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LoHangLogic {
    private LoHangDAO dao = LoHangDAO.getInstance();

    public List<LoHang> layDanhSachLoHang() {
        return dao.layDanhSachLoHang();	 
    }

    public LoHang timLoHangTheoMa(String maLoHang) {
        if (maLoHang == null || maLoHang.trim().isEmpty()) return null;
        return dao.layLoHangTheoMa(maLoHang);
    }

    public void themLoHang(LoHang lh) throws Exception {
        kiemTraLoi(lh);
        
        if (timLoHangTheoMa(lh.getMaLoHang()) != null) {
            throw new Exception("Lỗi: Mã lô hàng '" + lh.getMaLoHang() + "' đã tồn tại trong hệ thống! Vui lòng nhập mã khác.");
        }
        
        boolean thanhCong = dao.themLoHang(lh);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Thêm Lô hàng thất bại!");
        }
    }

    public void suaLoHang(LoHang lh) throws Exception {
        kiemTraLoi(lh);
        
        boolean thanhCong = dao.suaLoHang(lh);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Cập nhật thông tin Lô hàng thất bại!");
        }
    }

    public void xoaLoHang(String maLoHang) throws Exception {
        if (maLoHang == null || maLoHang.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Lô hàng cần xóa!");
        }
        
        boolean thanhCong = dao.xoaLoHang(maLoHang);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể xóa vì Lô hàng này đang chứa Sản phẩm bên trong (Lỗi khóa ngoại)!");
        }
    }

    private void kiemTraLoi(LoHang lh) throws Exception {
        if (lh.getMaLoHang() == null || lh.getMaLoHang().trim().isEmpty()) {
            throw new Exception("Mã lô hàng không được để trống!");
        }
        if (!lh.getMaLoHang().startsWith("LH")) {
            throw new Exception("Mã lô hàng phải bắt đầu bằng 'LH' (Ví dụ: LH001)!");
        }
        
        if (lh.getMaNCC() == null || lh.getMaNCC().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Nhà cung cấp cho lô hàng này!");
        }
        
        if (lh.getNgayNhapKho() == null) {
            throw new Exception("Vui lòng chọn Ngày nhập kho!");
        }
        if (lh.getNgayNhapKho().isAfter(LocalDate.now())) {
            throw new Exception("Ngày nhập kho không thể lớn hơn ngày hôm nay!");
        }
        
        if (lh.getThanhTien() != null && lh.getThanhTien().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Tổng thành tiền của lô hàng không được là số âm!");
        }
    }
}