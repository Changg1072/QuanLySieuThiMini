package Logic.KhuyenMai;

import java.util.ArrayList;
import java.util.List;

public class DiscountEngine {
    private List<IGiamGiaRule> danhSachQuyTac;
    private final double MAX_DISCOUNT_PERCENT = 0.70; // Giảm tối đa 70%

    public DiscountEngine() {
        danhSachQuyTac = new ArrayList<>();
    }

    // Đăng ký các rule vào Engine
    public void addRule(IGiamGiaRule rule) {
        danhSachQuyTac.add(rule);
    }

    // Class chứa kết quả trả về
    public static class KetQuaGiamGia {
        public double phanTramGiamCuoiCung;
        public List<String> cacTagHienThi = new ArrayList<>();
    }

    public KetQuaGiamGia xuLyGiamGia(GiamGiaContext ctx) {
        KetQuaGiamGia ketQua = new KetQuaGiamGia();
        double tongPhanTramGiam = 0.0;

        // 1. Duyệt qua tất cả các rule, cộng dồn mức giảm
        for (IGiamGiaRule rule : danhSachQuyTac) {
            double giam = rule.tinhPhanTramGiam(ctx);
            if (giam > 0) {
                tongPhanTramGiam += giam;
                ketQua.cacTagHienThi.add(rule.layTagHienThi());
            }
        }

        // 2. Chặn mức giảm tối đa (VD: không quá 70%)
        if (tongPhanTramGiam > MAX_DISCOUNT_PERCENT) {
            tongPhanTramGiam = MAX_DISCOUNT_PERCENT;
        }

        // 3. CHỐT CHẶN BẢO VỆ VỐN (QUAN TRỌNG NHẤT)
        // Giá bán sau khi giảm không được thấp hơn Giá Nhập (Vốn)
        double giaSauKhiGiam = ctx.giaBan * (1 - tongPhanTramGiam);
        if (giaSauKhiGiam < ctx.giaNhap) {
            // Ép giá bán bằng đúng giá nhập (Hoặc giá nhập + 2% chi phí vận hành)
            giaSauKhiGiam = ctx.giaNhap; 
            // Tính ngược lại phần trăm giảm
            tongPhanTramGiam = (ctx.giaBan - giaSauKhiGiam) / ctx.giaBan;
        }

        // Làm tròn đến 2 chữ số thập phân
        ketQua.phanTramGiamCuoiCung = Math.round(tongPhanTramGiam * 100.0) / 100.0;
        return ketQua;
    }
}