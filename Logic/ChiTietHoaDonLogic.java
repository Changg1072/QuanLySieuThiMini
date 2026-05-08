package Logic;

import Dao.ChiTietHoaDonDAO;
import Dao.SanPhamDAO;
import Dao.GiamGiaDAO;
import Data.ChiTietHoaDon;
import Data.SanPham;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ChiTietHoaDonLogic {
    private ChiTietHoaDonDAO dao = ChiTietHoaDonDAO.getInstance();

    // ==============================
    // 1. XEM LẠI BILL CŨ (Lịch sử)
    // ==============================
    public List<ChiTietHoaDon> layChiTietTheoMaHD(String maHD) {
        if (maHD == null || maHD.trim().isEmpty()) return null;
        return dao.layChiTietTheoMaHD(maHD);
    }

    // ==============================
    // 2. THANH TOÁN TOÀN BỘ GIỎ HÀNG (TUYỆT CHIÊU BATCH)
    // ==============================
    public void thanhToanDanhSachChiTiet(List<ChiTietHoaDon> danhSachGioHang) throws Exception {
        if (danhSachGioHang == null || danhSachGioHang.isEmpty()) {
            throw new Exception("Lỗi: Giỏ hàng trống! Không có sản phẩm nào để thanh toán.");
        }

        // BƯỚC 1: KIỂM TRA DỮ LIỆU & TÍNH TOÁN GIÁ TIỀN (Chưa đụng vào Kho)
        for (ChiTietHoaDon cthd : danhSachGioHang) {
            kiemTraLoi(cthd);
            
            // Tự động kéo giá mới nhất và khuyến mãi
            SanPham spGoc = SanPhamDAO.getInstance().laySanPhamTheoMa(cthd.getMaSp());
            if (spGoc == null) {
                throw new Exception("Lỗi: Không tìm thấy Sản phẩm " + cthd.getMaSp() + " trong danh mục!");
            }
            
            BigDecimal giaGoc = spGoc.getGiaBan();
            cthd.setDonGia(giaGoc);
            
            BigDecimal phanTramGiam = GiamGiaDAO.getInstance().layMucGiamGiaHienTai(cthd.getMaSp());
            BigDecimal giaSauGiam = giaGoc;
            
            if (phanTramGiam != null && phanTramGiam.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal heSoGiam = BigDecimal.ONE.subtract(phanTramGiam);
                giaSauGiam = giaGoc.multiply(heSoGiam);
            }
            
            BigDecimal thanhTien = giaSauGiam.multiply(BigDecimal.valueOf(cthd.getSoLuong()));
            cthd.setThanhTienSanPham(thanhTien); 
        }

        // BƯỚC 2: TRỪ TỒN KHO THỰC TẾ (Rất nhạy cảm)
        List<ChiTietHoaDon> danhSachDaTruKho = new ArrayList<>();
        
        for (ChiTietHoaDon cthd : danhSachGioHang) {
            boolean truKhoThanhCong = QuanLyKhoCacheLogic.getInstance().banHangTruTonKho(
                    cthd.getMaLoHang(), 
                    cthd.getMaSp(), 
                    cthd.getSoLuong()
            );
            
            if (truKhoThanhCong) {
                danhSachDaTruKho.add(cthd); // Lưu nháp vào danh sách đã trừ thành công
            } else {
                // ĐẠI HỌA: KHO BÁO HẾT HÀNG! PHẢI ROLLBACK (TRẢ LẠI KHO) NHỮNG MÓN TRƯỚC ĐÓ
                for (ChiTietHoaDon daTru : danhSachDaTruKho) {
                    QuanLyKhoCacheLogic.getInstance().traHangCongTonKho(daTru.getMaLoHang(), daTru.getMaSp(), daTru.getSoLuong());
                }
                throw new Exception("Lỗi Kho: Sản phẩm '" + cthd.getMaSp() + "' không đủ số lượng tồn! Đã hoàn tác toàn bộ giỏ hàng.");
            }
        }
        

        // BƯỚC 3: KHO ĐÃ OK -> GỌI XE TẢI CHỞ CẢ LIST XUỐNG DATABASE
        boolean thanhCong = dao.themDanhSachChiTietHoaDon(danhSachGioHang);
        if (!thanhCong) {
            // LỠ SQL BỊ SẬP NGUỒN -> ROLLBACK KHO LẦN NỮA
            for (ChiTietHoaDon cthd : danhSachGioHang) {
                QuanLyKhoCacheLogic.getInstance().traHangCongTonKho(cthd.getMaLoHang(), cthd.getMaSp(), cthd.getSoLuong());
            }
            throw new Exception("Lỗi Database: Không thể lưu hóa đơn! Đã hoàn trả hàng lại vào kho.");
        }
        QuanLyKhoCacheLogic.getInstance().taiLaiDuLieuKho();
    }

    // ==============================
    // 3. HÀM BẮT LỖI CƠ BẢN
    // ==============================
    private void kiemTraLoi(ChiTietHoaDon cthd) throws Exception {
        if (cthd.getMaHD() == null || cthd.getMaHD().trim().isEmpty()) {
            throw new Exception("Lỗi: Chưa xác định được Mã Hóa Đơn!");
        }
        if (cthd.getMaSp() == null || cthd.getMaSp().trim().isEmpty()) {
            throw new Exception("Lỗi: Chưa xác định được Mã Sản Phẩm!");
        }
        if (cthd.getMaLoHang() == null || cthd.getMaLoHang().trim().isEmpty()) {
            throw new Exception("Lỗi: Sản phẩm này chưa được xác định lấy từ Lô hàng nào!");
        }
        if (cthd.getSoLuong() <= 0) {
            throw new Exception("Số lượng mua phải lớn hơn 0!");
        }
    }
}