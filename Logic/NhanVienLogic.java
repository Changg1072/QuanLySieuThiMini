package Logic;

import Dao.NhanVienDAO;
import Data.NhanVien;

import java.math.BigDecimal;
import java.util.List;

public class NhanVienLogic {
	private NhanVienDAO dao = NhanVienDAO.getInstance();

	public List<NhanVien> layDanhSachNhanVien() {
		return dao.layDanhSachNhanVien();
	}
	public NhanVien timNhanVienTheoMa(String maNV) {
		if (maNV == null || maNV.trim().isEmpty()) return null;
		return dao.layNhanVienTheoMa(maNV);
	}
	public void themNhanVien(NhanVien nv) throws Exception {
		kiemTraLoi(nv);
		if (timNhanVienTheoMa(nv.getMaNV()) != null) {
			throw new Exception("Lỗi: Mã nhân viên '" + nv.getMaNV() + "' đã tồn tại trong hệ thống!");
		}

		if (nv.getTrangThai() == null || nv.getTrangThai().trim().isEmpty()) {
			nv.setTrangThai("Đang Làm Việc");
		}
		
		// GÁN MẶC ĐỊNH 20K NẾU QUÊN NHẬP LƯƠNG
		if (nv.getLuongGio() == null || nv.getLuongGio().compareTo(BigDecimal.ZERO) <= 0) {
            nv.setLuongGio(new BigDecimal("20000")); 
        }
		boolean thanhCong = dao.themNhanVien(nv);
		if (!thanhCong) {
			throw new Exception("Lỗi hệ thống: Thêm nhân viên thất bại!");
		}
	}

	public void suaNhanVien(NhanVien nv) throws Exception {
		kiemTraLoi(nv);
		
		boolean thanhCong = dao.suaNhanVien(nv);
		if (!thanhCong) {
			throw new Exception("Lỗi hệ thống: Cập nhật thông tin thất bại!");
		}
	}
	private void kiemTraLoi(NhanVien nv) throws Exception {
		// Kiểm tra Mã NV
		if (nv.getMaNV() == null || nv.getMaNV().trim().isEmpty()) {
			throw new Exception("Mã nhân viên không được để trống!");
		}
		if (!nv.getMaNV().startsWith("NV")) {
			throw new Exception("Mã nhân viên phải bắt đầu bằng chữ 'NV' (Ví dụ: NV001)!");
		}
		if (nv.getHoTen() == null || nv.getHoTen().trim().length() < 2) {
			throw new Exception("Họ tên nhân viên không hợp lệ (Phải có ít nhất 2 ký tự)!");
		}
		if (nv.getSDT() == null || !nv.getSDT().matches("^0\\d{9}$")) {
			throw new Exception("Số điện thoại không hợp lệ (Phải đủ 10 số và bắt đầu bằng số 0)!");
		}
		if (nv.getLuongGio() != null && nv.getLuongGio().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lương không thể là số âm!");
        }
		if (nv.getNgayVaoLam() == null) {
			throw new Exception("Vui lòng chọn Ngày vào làm!");
		}
		if (nv.getNgayNghiViec() != null && nv.getNgayNghiViec().isBefore(nv.getNgayVaoLam())) {
			throw new Exception("Ngày nghỉ việc không thể trước Ngày vào làm!");
		}
		if ("Đã Nghỉ".equals(nv.getTrangThai()) && nv.getNgayNghiViec() == null) {
		    throw new Exception("Nhân viên 'Đã Nghỉ' thì phải nhập Ngày nghỉ việc!");
		}
		if ("Đang Làm Việc".equals(nv.getTrangThai()) && nv.getNgayNghiViec() != null) {
		    throw new Exception("Nhân viên 'Đang Làm Việc' thì không được có Ngày nghỉ việc!");
		}
	}
	
	// 5. TÍNH THÂM NIÊN
	public int tinhThamNien(String maNV) {
		if (maNV == null || maNV.trim().isEmpty()) {
			return 0; 
		}
		return dao.tinhThamNien(maNV);
	}

	public BigDecimal tinhLuongCuoiThang(BigDecimal mucLuongTheoGio, double tongGioLam, int soNamThamNien, BigDecimal tongTienThieuKet, BigDecimal tienPhatViPhamThuCong) {
        if (mucLuongTheoGio == null) mucLuongTheoGio = BigDecimal.ZERO;
        if (tongTienThieuKet == null) tongTienThieuKet = BigDecimal.ZERO;
        if (tienPhatViPhamThuCong == null) tienPhatViPhamThuCong = BigDecimal.ZERO;
        BigDecimal luongCoBan = mucLuongTheoGio.multiply(BigDecimal.valueOf(tongGioLam));
     // Ép soNamThamNien thành chuỗi, rồi nhân với 0.10 bằng hàm multiply của BigDecimal
        BigDecimal heSoThuong = new BigDecimal(String.valueOf(soNamThamNien)).multiply(new BigDecimal("0.10"));
        BigDecimal tienThuongThamNien = luongCoBan.multiply(heSoThuong);
        BigDecimal tongThuNhap = luongCoBan.add(tienThuongThamNien);
        BigDecimal tongKhauTru = tongTienThieuKet.add(tienPhatViPhamThuCong);
        BigDecimal tongLuongThucLanh = tongThuNhap.subtract(tongKhauTru);
        if (tongLuongThucLanh.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return tongLuongThucLanh;
    }
}