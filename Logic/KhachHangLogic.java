package Logic;

import Dao.KhachHangDAO;
import Data.KhachHang;

import java.math.BigDecimal;
import java.util.List;

public class KhachHangLogic {
    private KhachHangDAO dao = KhachHangDAO.getInstance();

    public List<KhachHang> layDanhSachKhachHang() {
        return dao.layDanhSachKhachHang();
    }

    public KhachHang timKhachHangTheoMa(String maKH) {
        if (maKH == null || maKH.trim().isEmpty()) return null;
        return dao.layKhachHangTheoMa(maKH);
    }
    
    public KhachHang timKhachHangTheoSDT(String sdt) {
        if (sdt == null || sdt.trim().isEmpty()) return null;
        return dao.layKhachHangTheoSDT(sdt);
    }

    public void themKhachHang(KhachHang kh) throws Exception {
        kiemTraLoi(kh);
        
        if (timKhachHangTheoSDT(kh.getSDT()) != null) {
            throw new Exception("Lỗi: Số điện thoại '" + kh.getSDT() + "' đã được đăng ký cho một khách hàng khác!");
        }
        
        if (timKhachHangTheoMa(kh.getMaKH()) != null) {
            throw new Exception("Lỗi: Mã khách hàng '" + kh.getMaKH() + "' đã tồn tại!");
        }
        
        boolean thanhCong = dao.themKhachHang(kh);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Thêm khách hàng thất bại!");
        }
    }

    public void suaKhachHang(KhachHang kh) throws Exception {
        kiemTraLoi(kh);
        KhachHang khTrungSDT = timKhachHangTheoSDT(kh.getSDT());
        if (khTrungSDT != null && !khTrungSDT.getMaKH().equals(kh.getMaKH())) {
            throw new Exception("Lỗi: Số điện thoại này đã thuộc về khách hàng: " + khTrungSDT.getHoTen() + "!");
        }
        boolean thanhCong = dao.suaKhachHang(kh);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Cập nhật thông tin khách hàng thất bại!");
        }
    }
    
    public void xoaKhachHang(String maKH) throws Exception {
        if (maKH == null || maKH.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Khách hàng cần xóa!");
        }
        
        boolean thanhCong = dao.xoaKhachHang(maKH);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể xóa vì Khách hàng này đã có Lịch sử mua hàng (Liên kết với Hóa Đơn)!");
        }
    }
 // 1. Hàm cầu nối để UI lấy phần trăm giảm giá theo Hạng (ĐÃ CHUẨN HÓA BIGDECIMAL)
    public BigDecimal layPhanTramGiamGia(String bacKH) {
        if (bacKH == null || bacKH.trim().isEmpty()) {
            return new BigDecimal("0.01"); // Mặc định 1%
        }
        // Trả thẳng BigDecimal từ DAO lên, KHÔNG ép kiểu gì hết!
        return dao.layPhanTramGiamGia(bacKH);
    }
 // 2. Hàm đồng bộ tên gọi cho UI (UI đang gọi hàm layKhachHangTheoSDT)
    public KhachHang layKhachHangTheoSDT(String sdt) {
        return timKhachHangTheoSDT(sdt); 
    }

    // 3. Hàm kích hoạt tính năng THĂNG HẠNG (Bạc, Vàng, VIP) cực xịn từ DAO
    public void capNhatDiemVaHang(String maKH, BigDecimal tienThanhToanSauKhuyenMai) throws Exception {
        if (maKH == null || maKH.trim().isEmpty()) {
            throw new Exception("Mã khách hàng không hợp lệ để cập nhật điểm!");
        }
        
        if (tienThanhToanSauKhuyenMai == null || tienThanhToanSauKhuyenMai.compareTo(BigDecimal.ZERO) < 0) {
            tienThanhToanSauKhuyenMai = BigDecimal.ZERO;
        }

        boolean thanhCong = dao.CapNhatDiemVaHang(maKH, tienThanhToanSauKhuyenMai);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể cập nhật Điểm và Hạng thành viên lúc này!");
        }
    }
    private void kiemTraLoi(KhachHang kh) throws Exception {
        if (kh.getMaKH() == null || kh.getMaKH().trim().isEmpty()) {
            throw new Exception("Mã khách hàng không được để trống!");
        }
        if (!kh.getMaKH().startsWith("KH")) {
            throw new Exception("Mã khách hàng phải bắt đầu bằng chữ 'KH' (Ví dụ: KH001)!");
        }
        
        if (kh.getHoTen() == null || kh.getHoTen().trim().isEmpty()) {
            throw new Exception("Họ tên khách hàng không được để trống!");
        }
        
        if (kh.getSDT() == null || !kh.getSDT().matches("^0\\d{9}$")) {
            throw new Exception("Số điện thoại không hợp lệ (Phải đủ 10 chữ số và bắt đầu bằng số 0)!");
        }
        
        if (kh.getDiemTichLuy() != null && kh.getDiemTichLuy().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Điểm tích lũy không thể là số âm!");
        }
    }
}