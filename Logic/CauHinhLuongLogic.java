package Logic;

import Dao.CauHinhLuongDAO;
import Data.CauHinhLuong;
import java.math.BigDecimal;

public class CauHinhLuongLogic {
    private CauHinhLuongDAO dao = CauHinhLuongDAO.getInstance();

    public CauHinhLuong layCauHinhHienTai(String maNV) throws Exception {
        if (maNV == null || maNV.trim().isEmpty()) {
            throw new Exception("Mã nhân viên không hợp lệ!");
        }
        return dao.layCauHinhHienTaiTheoMaNV(maNV);
    }

    public void capNhatCauHinhLuongMoi(CauHinhLuong chl) throws Exception {
        if (chl.getMaNV() == null || chl.getMaNV().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn nhân viên cần cài đặt lương!");
        }
        if (chl.getLuongTheoGio() == null || chl.getLuongTheoGio().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lương theo giờ không được là số âm!");
        }

        // Setup giá trị mặc định nếu UI quên truyền
        if (chl.getHeSoLuong() == null) chl.setHeSoLuong(new BigDecimal("1.0"));
        if (chl.getHeSoTangCa() == null) chl.setHeSoTangCa(new BigDecimal("1.5"));
        if (chl.getPhuCapCoDinh() == null) chl.setPhuCapCoDinh(BigDecimal.ZERO);

        boolean thanhCong = dao.themCauHinhLuuTruLichSu(chl);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể lưu cấu hình lương mới!");
        }
    }
}