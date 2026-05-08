package Logic;

import Dao.NhaCungCapDAO;
import Data.NhaCungCap;

import java.util.List;

public class NhaCungCapLogic {
    private NhaCungCapDAO dao = NhaCungCapDAO.getInstance();

    public List<NhaCungCap> layDanhSachNhaCungCap() {
        return dao.layDanhSachNhaCungCap();
    }

    public NhaCungCap timNhaCungCapTheoMa(String maNCC) {
        if (maNCC == null || maNCC.trim().isEmpty()) {
            return null; 
        }
        return dao.layNhaCungCapTheoMa(maNCC);
    }

    public void themNhaCungCap(NhaCungCap ncc) throws Exception {
        kiemTraLoi(ncc);
        
        if (dao.kiemTraTrungMaNCC(ncc.getMaNCC())) {
            throw new Exception("Lỗi: Mã nhà cung cấp '" + ncc.getMaNCC() + "' đã tồn tại!");
        }
        
        NhaCungCap nccTrungTen = dao.layNhaCungCapTheoTen(ncc.getTenNCC());

if (nccTrungTen != null) {
    throw new Exception("Lỗi: Tên nhà cung cấp '" + ncc.getTenNCC() + "' đã có trong hệ thống!");
}
        
        boolean thanhCong = dao.themNhaCungCap(ncc);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Thêm nhà cung cấp thất bại!");
        }
    }

    public void suaNhaCungCap(NhaCungCap ncc) throws Exception {
        kiemTraLoi(ncc);
        // Trong hàm suaNhaCungCap()
NhaCungCap nccTrungTen = dao.layNhaCungCapTheoTen(ncc.getTenNCC());

// Nếu TÌM THẤY một thằng có tên đó, VÀ mã của thằng tìm thấy KHÁC với mã mình đang sửa
if (nccTrungTen != null && !nccTrungTen.getMaNCC().equals(ncc.getMaNCC())) {
    throw new Exception("Lỗi: Tên nhà cung cấp này đã bị trùng với một Nhà cung cấp khác trong hệ thống!");
}
        boolean thanhCong = dao.suaNhaCungCap(ncc);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Cập nhật thông tin thất bại!");
        }
    }

    public void xoaNhaCungCap(String maNCC) throws Exception {
        if (maNCC == null || maNCC.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Nhà cung cấp cần xóa!");
        }
        
        boolean thanhCong = dao.xoaNhaCungCap(maNCC);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể xóa vì Nhà cung cấp này đang có dữ liệu trong Lô Hàng/Sản Phẩm!");
        }
    }

    private void kiemTraLoi(NhaCungCap ncc) throws Exception {
        if (ncc.getMaNCC() == null || ncc.getMaNCC().trim().isEmpty()) {
            throw new Exception("Mã nhà cung cấp không được để trống!");
        }
        if (!ncc.getMaNCC().startsWith("NCC")) {
            throw new Exception("Mã nhà cung cấp phải bắt đầu bằng 'NCC' (Ví dụ: NCC001)!");
        }
        
        if (ncc.getTenNCC() == null || ncc.getTenNCC().trim().isEmpty()) {
            throw new Exception("Tên nhà cung cấp không được để trống!");
        }
        
        if (ncc.getSDT() == null || !ncc.getSDT().matches("^0\\d{9}$")) {
            throw new Exception("Số điện thoại không hợp lệ (Phải đủ 10 số và bắt đầu bằng số 0)!");
        }
        
        if (ncc.getEmail() != null && !ncc.getEmail().trim().isEmpty()) {
            if (!ncc.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new Exception("Định dạng Email không hợp lệ (Ví dụ đúng: abc@gmail.com)!");
            }
        }
        
        if (ncc.getDiaChi() == null || ncc.getDiaChi().trim().isEmpty()) {
            throw new Exception("Địa chỉ không được để trống!");
        }
    }
}