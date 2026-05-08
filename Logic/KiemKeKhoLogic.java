package Logic;

import Dao.KiemKeKhoDAO;
import Data.KiemKeKho;

import java.time.LocalDate;
import java.util.List;

public class KiemKeKhoLogic {
    private KiemKeKhoDAO dao = KiemKeKhoDAO.getInstance();

    public List<KiemKeKho> layDanhSachKiemKe() {
        return dao.layDanhSachKiemKe();
    }

    public void lapPhieuKiemKe(KiemKeKho kk) throws Exception {
        kiemTraLoi(kk);
        
        // VÁ LỖI 2: DÙNG HÀM CHECK TRÙNG MÃ ĐÃ VIẾT Ở DAO
        if (dao.kiemTraTrungMaKiemKe(kk.getMaKiemKe())) {
            throw new Exception("Lỗi: Mã Phiếu Kiểm Kê '" + kk.getMaKiemKe() + "' đã tồn tại trong hệ thống!");
        }
        
        int chenhLech = kk.getSoLuongThucTe() - kk.getSoLuongHeThong();
        
        if (chenhLech != 0 && (kk.getLyDo() == null || kk.getLyDo().trim().isEmpty())) {
            throw new Exception("Lỗi: Số lượng kho đang bị lệch (" + chenhLech + " sản phẩm). BẮT BUỘC nhân viên phải nhập Lý do (Vd: Hư hỏng, chuột cắn, thất lạc...)!");
        }
        
        if (chenhLech == 0 && (kk.getLyDo() == null || kk.getLyDo().trim().isEmpty())) {
            kk.setLyDo("Kho khớp với hệ thống, không có hao hụt.");
        }
        
        boolean thanhCong = dao.lapPhieuKiemKe(kk);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Lập phiếu kiểm kê thất bại!");
        }
    }

    public void duyetVaCanBangKho(KiemKeKho kk) throws Exception {
        if (kk.getMaKiemKe() == null || kk.getMaKiemKe().trim().isEmpty()) {
            throw new Exception("Không xác định được Phiếu kiểm kê cần duyệt!");
        }
        
        if ("Đã Cân Kho".equalsIgnoreCase(kk.getTrangThai())) {
            throw new Exception("Lỗi Nghiệp Vụ: Phiếu này ĐÃ ĐƯỢC DUYỆT VÀ CÂN BẰNG KHO rồi! Không thể duyệt lại để tránh sai lệch dữ liệu tồn kho.");
        }

        boolean thanhCong = dao.duyetVaCanBangKho(kk);
        if (!thanhCong) {
            throw new Exception("Lỗi Hệ Thống: Cân bằng kho thất bại!");
        }
        
        // VÁ LỖI 1: CÂN BẰNG KHO XONG PHẢI TẢI LẠI CACHE ĐỂ GIAO DIỆN CẬP NHẬT NGAY LẬP TỨC
        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
    }

    private void kiemTraLoi(KiemKeKho kk) throws Exception {
        if (kk.getMaKiemKe() == null || kk.getMaKiemKe().trim().isEmpty()) {
            throw new Exception("Mã Phiếu Kiểm Kê không được để trống!");
        }
        if (kk.getMaNV() == null || kk.getMaNV().trim().isEmpty()) {
            throw new Exception("Chưa xác định được Nhân viên đang đi kiểm kho!");
        }
        if (kk.getMaLoHang() == null || kk.getMaLoHang().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Lô hàng cần kiểm kê!");
        }
        if (kk.getMaSP() == null || kk.getMaSP().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Sản phẩm cần kiểm kê!");
        }
        
        if (kk.getNgayKiemKe() == null) {
            throw new Exception("Vui lòng nhập Ngày kiểm kê!");
        }
        if (kk.getNgayKiemKe().isAfter(LocalDate.now())) {
            throw new Exception("Lỗi: Ngày kiểm kê không thể là một ngày trong tương lai!");
        }
        
        if (kk.getSoLuongHeThong() < 0) {
            throw new Exception("Lỗi dữ liệu: Số lượng hệ thống đang bị âm!");
        }
        if (kk.getSoLuongThucTe() < 0) {
            throw new Exception("Lỗi: Số lượng đếm thực tế bằng tay không được là số âm!");
        }
    }
}