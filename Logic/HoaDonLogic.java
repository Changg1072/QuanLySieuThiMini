package Logic;

import Dao.HoaDonDAO;
import Data.HoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class HoaDonLogic {
    private HoaDonDAO dao = HoaDonDAO.getInstance();

    public List<HoaDon> layDanhSachHoaDon() {
        return dao.layDanhSachHoaDon();
    }

    public HoaDon timHoaDonTheoMa(String maHD) {
        if (maHD == null || maHD.trim().isEmpty()) return null;
        return dao.layHoaDonTheoMa(maHD);
    }
    // ==============================
    // TÌM LỊCH SỬ HÓA ĐƠN THEO SĐT KHÁCH
    // ==============================
    public List<HoaDon> layHoaDonTheoSDT(String sdt) {
        if (sdt == null || sdt.trim().isEmpty()) {
            return null; // Trả về null hoặc new ArrayList<HoaDon>() đều được
        }
        return dao.layHoaDonTheoSDT(sdt);
    }
    public void themHoaDon(HoaDon hd) throws Exception {
    	if (hd.getNgayTao() == null) {
            hd.setNgayTao(LocalDateTime.now());
        }
        
        kiemTraLoi(hd);
        if (timHoaDonTheoMa(hd.getMaHD()) != null) {
            throw new Exception("Lỗi: Mã hóa đơn '" + hd.getMaHD() + "' đã tồn tại! Hệ thống sẽ tự động tạo mã mới.");
        }
        
        BigDecimal giamGia = (hd.getTongGiamGia() != null) ? hd.getTongGiamGia() : BigDecimal.ZERO;
        BigDecimal tichDiem = (hd.getTruTichDiem() != null) ? hd.getTruTichDiem() : BigDecimal.ZERO;
        
        BigDecimal tongKhauTru = giamGia.add(tichDiem);
        BigDecimal tienKhachPhaiTra = hd.getThanhTien().subtract(tongKhauTru);
        
        if (tienKhachPhaiTra.compareTo(BigDecimal.ZERO) < 0) {
            tienKhachPhaiTra = BigDecimal.ZERO; 
        }
        
        if ("Tiền mặt".equalsIgnoreCase(hd.getPhuongThucTT())) {
            BigDecimal khachDua = (hd.getKhachDua() != null) ? hd.getKhachDua() : BigDecimal.ZERO;
            
            if (khachDua.compareTo(tienKhachPhaiTra) < 0) {
                throw new Exception("Lỗi Thanh Toán: Khách đưa không đủ tiền! (Cần trả: " + tienKhachPhaiTra + "đ, Khách đưa: " + khachDua + "đ)");
            }
            hd.setTienThua(khachDua.subtract(tienKhachPhaiTra));
            
        } else {
            hd.setKhachDua(tienKhachPhaiTra);
            hd.setTienThua(BigDecimal.ZERO);
        }
        
        boolean thanhCong = dao.themHoaDon(hd);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể lưu Hóa đơn vào CSDL!");
        }
    }

    private void kiemTraLoi(HoaDon hd) throws Exception {
        if (hd.getMaHD() == null || hd.getMaHD().trim().isEmpty()) {
            throw new Exception("Lỗi: Mã hóa đơn không được để trống!");
        }
        
        if (hd.getMaNV() == null || hd.getMaNV().trim().isEmpty()) {
            throw new Exception("Lỗi: Không xác định được Nhân viên thu ngân thực hiện giao dịch này!");
        }
        
        if (hd.getThanhTien() == null || hd.getThanhTien().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lỗi: Tổng thành tiền của hóa đơn không được là số âm!");
        }
        
        if (hd.getTongGiamGia() != null && hd.getTongGiamGia().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lỗi: Tổng giảm giá không hợp lệ!");
        }
        
        if (hd.getTruTichDiem() != null && hd.getTruTichDiem().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lỗi: Số tiền trừ từ điểm tích lũy không hợp lệ!");
        }
        
        if (hd.getPhuongThucTT() == null || hd.getPhuongThucTT().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Phương thức thanh toán (Tiền mặt/Chuyển khoản)!");
        }
    }
}