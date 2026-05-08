package Logic;

import Dao.ThongKeDAO;
import Data.ThongKe;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ThongKeLogic {
    private ThongKeDAO dao = ThongKeDAO.getInstance();

    public List<ThongKe> layDanhSachThongKe() {
        return dao.layDanhSachThongKe();
    }

    public void themThongKe(ThongKe tk) throws Exception {
        kiemTraLoiBaoCao(tk);
        
        boolean thanhCong = dao.themThongKe(tk);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể chốt sổ báo cáo!");
        }
    }

    public void xoaThongKe(String maThongKe) throws Exception {
        if (maThongKe == null || maThongKe.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn bản Báo cáo cần xóa!");
        }
        
        boolean thanhCong = dao.xoaThongKe(maThongKe);
        if (!thanhCong) {
            throw new Exception("Lỗi CSDL: Xóa báo cáo thất bại!");
        }
    }

    public List<Object[]> thongKeDoanhThuTheoNam(int nam) throws Exception {
        kiemTraNam(nam);
        return dao.thongKeDoanhThuTheoNam(nam);
    }

    public List<Object[]> topSanPhamBanChay(int thang, int nam, int top) throws Exception {
        kiemTraThangNam(thang, nam);
        kiemTraTop(top);
        return dao.topSanPhamBanChay(thang, nam, top);
    }

    public List<Object[]> topKhachHangVIP(int top) throws Exception {
        kiemTraTop(top);
        return dao.topKhachHangVIP(top);
    }
public BigDecimal[] thongKeLoiNhuanThang(int thang, int nam) throws Exception {
        kiemTraThangNam(thang, nam);
        
        // Đổi từ double[] sang BigDecimal[] cho khớp với DAO
        BigDecimal[] ketQua = dao.thongKeLoiNhuanThang(thang, nam);
        
        // Kiểm tra lợi nhuận âm bằng compareTo
        if (ketQua[3].compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("CẢNH BÁO TÀI CHÍNH: Siêu thị đang chịu mức lợi nhuận ÂM trong tháng " + thang + "/" + nam);
        }
        
        return ketQua;
    }

    private void kiemTraLoiBaoCao(ThongKe tk) throws Exception {
        if (tk.getMaThongKe() == null || tk.getMaThongKe().trim().isEmpty()) {
            throw new Exception("Mã báo cáo thống kê không được để trống!");
        }
        if (tk.getMaNV() == null || tk.getMaNV().trim().isEmpty()) {
            throw new Exception("Không xác định được Nhân viên/Quản lý thực hiện chốt sổ báo cáo này!");
        }
        if (tk.getNgayThongKe() != null && tk.getNgayThongKe().isAfter(LocalDate.now())) {
            throw new Exception("Không thể lập báo cáo chốt sổ cho một ngày trong tương lai!");
        }
        if (tk.getTongDoanhThu() != null && tk.getTongDoanhThu().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lỗi dữ liệu: Tổng doanh thu không thể là số âm!");
        }
        if (tk.getChiPhiNhapHang() != null && tk.getChiPhiNhapHang().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lỗi dữ liệu: Chi phí nhập hàng không thể là số âm!");
        }
    }

    private void kiemTraThangNam(int thang, int nam) throws Exception {
        if (thang < 1 || thang > 12) {
            throw new Exception("Tháng thống kê phải nằm trong khoảng từ 1 đến 12!");
        }
        kiemTraNam(nam);
    }

    private void kiemTraNam(int nam) throws Exception {
        int namHienTai = LocalDate.now().getYear();
        if (nam < 2026 || nam > namHienTai) {
            throw new Exception("Năm thống kê không hợp lệ (Phải từ năm 2026 đến năm hiện tại " + namHienTai + ")!");
        }
    }

    private void kiemTraTop(int top) throws Exception {
        if (top <= 0 || top > 100) {
            throw new Exception("Số lượng Top xếp hạng phải từ 1 đến 100 (Ví dụ: Top 10, Top 5)!");
        }
    }
}