package Logic;

import Dao.SanPhamDAO;
import Dao.ChiTietLoHangDAO;
import Data.SanPham;

import java.math.BigDecimal;
import java.util.List;

public class SanPhamLogic {
    private SanPhamDAO dao = SanPhamDAO.getInstance();

    public List<SanPham> layDanhSachSanPham() {
        return dao.layDanhSachSanPham();
    }

    public SanPham timSanPhamTheoMa(String maSP) throws Exception {
        if (maSP == null || maSP.trim().isEmpty()) {
            throw new Exception("Lỗi: Mã sản phẩm cần tìm không được để trống!");
        }
        SanPham sp = dao.laySanPhamTheoMa(maSP);
        if (sp == null) {
            throw new Exception("Không tìm thấy sản phẩm có mã: " + maSP);
        }
        return sp;
    }

    public void themSanPham(SanPham sp) throws Exception {
        kiemTraLoi(sp);
        
        if (dao.laySanPhamTheoMa(sp.getMaSP()) != null) {
            throw new Exception("Lỗi: Mã sản phẩm '" + sp.getMaSP() + "' đã tồn tại!");
        }

        SanPham spTrungTen = dao.laySanPhamTheoTen(sp.getTenSP());
        if (spTrungTen != null) {
            throw new Exception("Lỗi: Sản phẩm '" + sp.getTenSP() + "' đã có trong Danh mục!");
        }
        
        if (sp.getLinkHinhAnh() != null && !sp.getLinkHinhAnh().isEmpty()) {
            String tenAnhMoi = QuanLyAnh.copyAnhVaoProject(sp.getLinkHinhAnh());
            sp.setLinkHinhAnh(tenAnhMoi);
        }

        boolean thanhCong = dao.themSanPham(sp);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Thêm sản phẩm thất bại");
        }
    }

    public void suaSanPham(SanPham sp) throws Exception {
        kiemTraLoi(sp);
        SanPham spTrungTen = dao.laySanPhamTheoTen(sp.getTenSP());
        if (spTrungTen != null && !spTrungTen.getMaSP().equals(sp.getMaSP())) {
            throw new Exception("Lỗi: Tên sản phẩm này đã bị trùng với một mặt hàng khác!");
        }
        ChiTietLoHangDAO ctDao = ChiTietLoHangDAO.getInstance();
        BigDecimal giaNhapMax = ctDao.layGiaNhapCaoNhat(sp.getMaSP());
        
        if (giaNhapMax != null && giaNhapMax.compareTo(BigDecimal.ZERO) > 0 && sp.getGiaBan().compareTo(giaNhapMax) <= 0) {
            throw new Exception("Cảnh báo lỗ vốn: Giá bán mới (" + String.format("%,.0f", sp.getGiaBan()) + "đ) đang thấp hơn hoặc bằng Giá nhập cao nhất trong lịch sử (" + String.format("%,.0f", giaNhapMax) + "đ)!");
        }
        
        if (sp.getLinkHinhAnh() != null && !sp.getLinkHinhAnh().isEmpty()) {
            if (sp.getLinkHinhAnh().contains("\\") || sp.getLinkHinhAnh().contains("/")) {
                String tenAnhMoi = QuanLyAnh.copyAnhVaoProject(sp.getLinkHinhAnh());
                sp.setLinkHinhAnh(tenAnhMoi);
            }
        }

        boolean thanhCong = dao.suaSanPham(sp);
        if (!thanhCong) {
            throw new Exception("Lỗi CSDL: Cập nhật thông tin sản phẩm thất bại!");
        }
    }
    
    public void xoaSanPham(String maSP) throws Exception {
        if (maSP == null || maSP.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn sản phẩm cần xóa!");
        }
        
        boolean thanhCong = dao.xoaSanPham(maSP);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Không thể xóa vì sản phẩm này đang nằm trong Hóa đơn hoặc Lô hàng!");
        }
    }

    private void kiemTraLoi(SanPham sp) throws Exception {
        if (sp.getMaSP() == null || sp.getMaSP().trim().isEmpty()) {
            throw new Exception("Mã sản phẩm không được để trống!");
        }
        if (!sp.getMaSP().startsWith("SP")) {
            throw new Exception("Mã sản phẩm phải bắt đầu bằng chữ 'SP' (Ví dụ: SP001)!");
        }
        if (sp.getTenSP() == null || sp.getTenSP().trim().isEmpty()) {
            throw new Exception("Tên sản phẩm không được để trống!");
        }
        if (sp.getMaLoai() == null || sp.getMaLoai().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Loại sản phẩm!");
        }
        if (sp.getGiaBan() == null || sp.getGiaBan().compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Giá bán sản phẩm phải lớn hơn 0 VNĐ!");
        }
        if (sp.getDonViTinh() == null || sp.getDonViTinh().trim().isEmpty()) {
            throw new Exception("Đơn vị tính không được để trống (Ví dụ: Chai, Hộp, Gói...)!");
        }
    }
}