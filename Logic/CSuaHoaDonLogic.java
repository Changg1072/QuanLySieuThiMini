package Logic;

import Dao.CSuaHoaDonDAO;
import Dao.HoaDonDAO; // Import thêm để check hóa đơn gốc
import Data.CSuaHoaDon;

import java.time.LocalDateTime;
import java.util.List;

public class CSuaHoaDonLogic {
    private CSuaHoaDonDAO dao = CSuaHoaDonDAO.getInstance();

    public List<CSuaHoaDon> layDanhSachCSuaHoaDon() {
        return dao.layDanhSachCSuaHoaDon();
    }
    
    // Cập nhật lại gọi hàm trả về List
    public List<CSuaHoaDon> layDanhSachCSuaTheoMaHD(String maHD) {
        if (maHD == null || maHD.trim().isEmpty()) return null;
        return dao.layDanhSachCSuaTheoMaHD(maHD);
    }

    public void themCSuaHoaDon(CSuaHoaDon cshd) throws Exception {
        kiemTraLoi(cshd);
        
        // 1. KIỂM TRA MÃ PHIẾU CHỈNH SỬA (Chống trùng khóa chính)
        // Dùng stream lướt qua danh sách hiện tại xem có mã này chưa
        boolean trungMaPhieu = layDanhSachCSuaHoaDon().stream()
                .anyMatch(phieu -> phieu.getMaTraHang().equalsIgnoreCase(cshd.getMaTraHang()));
        if (trungMaPhieu) {
            throw new Exception("Lỗi: Mã phiếu chỉnh sửa '" + cshd.getMaTraHang() + "' đã tồn tại!");
        }

        // 2. KIỂM TRA HÓA ĐƠN GỐC CÓ TỒN TẠI KHÔNG (Chống lỗi Bóng Ma)
        Data.HoaDon hdGoc = HoaDonDAO.getInstance().layHoaDonTheoMa(cshd.getMaHD());
        if (hdGoc == null) {
            throw new Exception("Lỗi: Không tìm thấy Hóa đơn gốc mã '" + cshd.getMaHD() + "' trong hệ thống! Không thể lập phiếu chỉnh sửa.");
        }
        
        boolean thanhCong = dao.themCSuaHoaDon(cshd);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể tạo Phiếu trả hàng / Chỉnh sửa!");
        }
    }

    private void kiemTraLoi(CSuaHoaDon cshd) throws Exception {
        if (cshd.getMaTraHang() == null || cshd.getMaTraHang().trim().isEmpty()) {
            throw new Exception("Mã Phiếu Chỉnh sửa/Trả hàng không được để trống!");
        }
        
        if (cshd.getMaHD() == null || cshd.getMaHD().trim().isEmpty()) {
            throw new Exception("Vui lòng nhập Mã Hóa Đơn gốc cần chỉnh sửa hoặc trả hàng!");
        }
        
        if (cshd.getMaNV() == null || cshd.getMaNV().trim().isEmpty()) {
            throw new Exception("Lỗi: Không xác định được Nhân viên thực hiện giao dịch này!");
        }
        
        if (cshd.getNgayCs() == null) {
            throw new Exception("Vui lòng xác định Ngày/Giờ thực hiện chỉnh sửa!");
        }
        
        if (cshd.getNgayCs().isAfter(LocalDateTime.now())) {
            throw new Exception("Thời gian chỉnh sửa không hợp lệ (Không được lớn hơn thời gian hiện tại)!");
        }
        
        if (cshd.getTrangThaiCS() == null || cshd.getTrangThaiCS().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Trạng thái của phiếu chỉnh sửa!");
        }
        
        if (cshd.getLyDoSua() == null || cshd.getLyDoSua().trim().isEmpty()) {
            throw new Exception("BẮT BUỘC: Vui lòng nhập Lý do chỉnh sửa/Trả hàng (VD: Khách trả lại do móp méo, Nhập sai số lượng...)!");
        }
    }
}