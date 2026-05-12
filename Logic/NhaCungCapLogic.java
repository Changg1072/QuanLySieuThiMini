package Logic;

import Dao.NhaCungCapDAO;
import Data.NhaCungCap;

import java.util.List;

public class NhaCungCapLogic {
	private NhaCungCapDAO dao = NhaCungCapDAO.getInstance();

	// 1. Lấy danh sách nhà cung cấp
	public List<NhaCungCap> layDanhSachNhaCungCap() {
		return dao.layDanhSachNhaCungCap();
	}

	// 2. Tìm nhà cung cấp theo mã (Hỗ trợ cho việc hiển thị ở DanhSachNhapHangUi)
	public NhaCungCap timNhaCungCapTheoMa(String maNCC) {
		if (maNCC == null || maNCC.trim().isEmpty()) return null;
		return dao.layNhaCungCapTheoMa(maNCC);
	}

	// 3. Thêm nhà cung cấp mới
	public void themNhaCungCap(NhaCungCap ncc) throws Exception {
		kiemTraLoi(ncc);
		
		if (dao.kiemTraTrungMaNCC(ncc.getMaNCC())) {
			throw new Exception("Lỗi: Mã nhà cung cấp '" + ncc.getMaNCC() + "' đã tồn tại trong hệ thống!");
		}

		// Gán trạng thái mặc định nếu chưa có
		if (ncc.getTrangThai() == null || ncc.getTrangThai().trim().isEmpty()) {
			ncc.setTrangThai("Đang hợp tác");
		}
		
		boolean thanhCong = dao.themNhaCungCap(ncc);
		if (!thanhCong) {
			throw new Exception("Lỗi hệ thống: Thêm nhà cung cấp thất bại!");
		}
	}

	// 4. Sửa thông tin nhà cung cấp
	public void suaNhaCungCap(NhaCungCap ncc) throws Exception {
		kiemTraLoi(ncc);
		
		boolean thanhCong = dao.suaNhaCungCap(ncc);
		if (!thanhCong) {
			throw new Exception("Lỗi hệ thống: Cập nhật thông tin thất bại!");
		}
	}

	// 5. Xóa (Ngừng hợp tác) nhà cung cấp
	public void xoaNhaCungCap(String maNCC) throws Exception {
		if (maNCC == null || maNCC.trim().isEmpty()) {
			throw new Exception("Lỗi: Mã nhà cung cấp không hợp lệ!");
		}
		
		boolean thanhCong = dao.xoaNhaCungCap(maNCC);
		if (!thanhCong) {
			throw new Exception("Lỗi hệ thống: Chuyển trạng thái ngừng hợp tác thất bại!");
		}
	}

	// ==========================================
	// HÀM KIỂM TRA LỖI (VALIDATION) CHUNG
	// ==========================================
	private void kiemTraLoi(NhaCungCap ncc) throws Exception {
		if (ncc.getMaNCC() == null || ncc.getMaNCC().trim().isEmpty()) {
			throw new Exception("Mã nhà cung cấp không được để trống!");
		}
		if (ncc.getTenNCC() == null || ncc.getTenNCC().trim().isEmpty()) {
			throw new Exception("Tên nhà cung cấp không được để trống!");
		}
		if (ncc.getSDT() == null || !ncc.getSDT().matches("^0\\d{9}$")) {
			throw new Exception("Số điện thoại không hợp lệ (Phải đủ 10 số và bắt đầu bằng số 0)!");
		}
		if (ncc.getEmail() == null || !ncc.getEmail().contains("@")) {
			throw new Exception("Email không hợp lệ (Phải chứa ký tự '@')!");
		}
		if (ncc.getDiaChi() == null || ncc.getDiaChi().trim().isEmpty()) {
			throw new Exception("Địa chỉ không được để trống!");
		}
	}
}