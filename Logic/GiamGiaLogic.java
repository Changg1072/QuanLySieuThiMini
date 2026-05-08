package Logic;

import Dao.GiamGiaDAO;
import Data.GiamGia;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GiamGiaLogic {
    private GiamGiaDAO dao = GiamGiaDAO.getInstance();

    public List<GiamGia> layDanhSachGiamGia() {
        return dao.layDanhSachGiamGia();
    }

    public void themGiamGia(GiamGia gg) throws Exception {
        kiemTraLoi(gg);
        BigDecimal dangGiam = dao.layMucGiamGiaHienTai(gg.getMaSP());
        if (dangGiam != null && dangGiam.compareTo(BigDecimal.ZERO) > 0) {
            throw new Exception("Lỗi: Sản phẩm '" + gg.getMaSP() + "' đang có một chương trình giảm giá khác diễn ra. Vui lòng hủy chương trình cũ trước khi tạo mới!");
        }
        boolean thanhCong = dao.themGiamGia(gg);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Thêm Chương trình giảm giá thất bại!");
        }
    }

    public void huyGiamGia(String maSP) throws Exception {
        if (maSP == null || maSP.trim().isEmpty()) {
            throw new Exception("Mã sản phẩm không hợp lệ để hủy giảm giá!");
        }
        
        boolean thanhCong = dao.huyGiamGia(maSP);
        if (!thanhCong) {
            throw new Exception("Sản phẩm này hiện không có chương trình giảm giá nào đang diễn ra để hủy!");
        }
    }

    public double tinhGiaGiamTuDong(int soNgayConLai, int soLuongTon, double giaNhap, double giaBan) {
    	if (giaBan <= 0) return 0.0;
        // 1. Chốt chặn an toàn: Không bao giờ bán dưới giá nhập
        double maxDiscount = (giaBan - giaNhap) / giaBan;

        // Nếu đã hết hạn -> Vứt bỏ
        if (soNgayConLai <= 0) {
            return 0.0;
        }

        int mocThoiGian = 30; 
        if (soNgayConLai > mocThoiGian) {
            return 0.0; 
        }

        double tyLeThoiGian = (double) (mocThoiGian - soNgayConLai) / mocThoiGian;

        int mocTonKho = 50; 
        double tyLeTonKho = (double) soLuongTon / mocTonKho;
        if (tyLeTonKho > 1.0) {
            tyLeTonKho = 1.0; 
        }

        double trongSoThoiGian = 0.7;
        double trongSoTonKho = 0.3;

        double phanTramGiam = maxDiscount * ((trongSoThoiGian * tyLeThoiGian) + (trongSoTonKho * tyLeTonKho));

        return Math.round(phanTramGiam * 100.0) / 100.0; 
    }
    // ==============================
    // THÊM GIẢM GIÁ HÀNG LOẠT (TỰ ĐỘNG BỎ QUA LỖI)
    // Dùng cho nút "Tính giảm giá tự động tất cả sản phẩm"
    // ==============================
    public String themGiamGiaHangLoat(List<GiamGia> danhSachGiamGiaMoi) {
        if (danhSachGiamGiaMoi == null || danhSachGiamGiaMoi.isEmpty()) {
            return "Không có dữ liệu giảm giá nào để xử lý!";
        }

        int soLuongThanhCong = 0;
        List<String> danhSachMaSPBiLoi = new ArrayList<>();

        for (GiamGia gg : danhSachGiamGiaMoi) {
            try {
                kiemTraLoi(gg); // Vẫn check lỗi cơ bản như bình thường

                // BẪY CHỒNG CHÉO: Nếu đang có khuyến mãi -> BỎ QUA (Không throw Exception)
                BigDecimal dangGiam = dao.layMucGiamGiaHienTai(gg.getMaSP());
                if (dangGiam != null && dangGiam.compareTo(BigDecimal.ZERO) > 0) {
                    danhSachMaSPBiLoi.add(gg.getMaSP()); // Ghi sổ đen
                    continue; // Bỏ qua thằng này, chạy tiếp vòng lặp
                }

                // Lưu xuống DB
                boolean thanhCong = dao.themGiamGia(gg);
                if (thanhCong) {
                    soLuongThanhCong++;
                } else {
                    danhSachMaSPBiLoi.add(gg.getMaSP() + " (Lỗi SQL)");
                }

            } catch (Exception e) {
                // Nếu bị lỗi data (thiếu ngày, sai ngày...) -> Ghi sổ đen, không làm sập vòng lặp
                danhSachMaSPBiLoi.add(gg.getMaSP() + " (" + e.getMessage() + ")");
            }
        }

        // TỔNG HỢP KẾT QUẢ TRẢ VỀ CHO GIAO DIỆN HIỂN THỊ 1 LẦN DUY NHẤT
        StringBuilder ketQua = new StringBuilder();
        ketQua.append("Cập nhật thành công: ").append(soLuongThanhCong).append(" sản phẩm.\n");

        if (!danhSachMaSPBiLoi.isEmpty()) {
            ketQua.append("\nĐã bỏ qua ").append(danhSachMaSPBiLoi.size()).append(" sản phẩm bị lỗi hoặc đang có Khuyến mãi khác:\n");
            // In tối đa 5 mã lỗi để giao diện không bị tràn chữ, nếu nhiều hơn thì hiển thị "..."
            int maxHienThi = Math.min(5, danhSachMaSPBiLoi.size());
            for (int i = 0; i < maxHienThi; i++) {
                ketQua.append("- ").append(danhSachMaSPBiLoi.get(i)).append("\n");
            }
            if (danhSachMaSPBiLoi.size() > 5) {
                ketQua.append("... và ").append(danhSachMaSPBiLoi.size() - 5).append(" sản phẩm khác.");
            }
        }

        return ketQua.toString();
    }

    public BigDecimal layMucGiamGiaHienTai(String maSP) throws Exception {
        if (maSP == null || maSP.trim().isEmpty()) {
            throw new Exception("Mã sản phẩm không hợp lệ để kiểm tra giảm giá!");
        }
        return dao.layMucGiamGiaHienTai(maSP);
    }

    public void truSoLuongGiamGia(String maSP, int soLuongDaMua) throws Exception {
        if (soLuongDaMua <= 0) {
            throw new Exception("Số lượng mua để trừ khuyến mãi phải lớn hơn 0!");
        }
        
        boolean thanhCong = dao.truSoLuongGiamGia(maSP, soLuongDaMua);
        if (!thanhCong) {
            System.out.println("Cảnh báo: Sản phẩm " + maSP + " không còn suất giảm giá hoặc lỗi trừ số lượng!");
        }
    }

    private void kiemTraLoi(GiamGia gg) throws Exception {
        if (gg.getMaGiamGia() == null || gg.getMaGiamGia().trim().isEmpty()) {
            throw new Exception("Mã giảm giá không được để trống!");
        }
        
        if (gg.getMaSP() == null || gg.getMaSP().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Sản phẩm để áp dụng giảm giá!");
        }
        
        if (gg.getBatDau() == null) {
            throw new Exception("Vui lòng nhập Thời gian bắt đầu!");
        }
        
        if (gg.getKetThuc() == null) {
            throw new Exception("Vui lòng nhập Thời gian kết thúc!");
        }
        
        if (gg.getBatDau().isAfter(gg.getKetThuc())) {
            throw new Exception("Logic thời gian sai: Thời gian bắt đầu phải TRƯỚC Thời gian kết thúc!");
        }
        
        if (gg.getGiamGia() == null || gg.getGiamGia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Mức giảm giá phải lớn hơn 0!");
        }
        
        if (gg.getSoLuongApDung() <= 0) {
            throw new Exception("Số lượng suất giảm giá phải lớn hơn 0!");
        }
        
        if (gg.getTrangThaiGiamGia() == null || gg.getTrangThaiGiamGia().trim().isEmpty()) {
            throw new Exception("Trạng thái giảm giá không được để trống!");
        }
        
        if (gg.getLoaiGiamGia() == null || gg.getLoaiGiamGia().trim().isEmpty()) {
            throw new Exception("Loại giảm giá không được để trống!");
        }
    }
}