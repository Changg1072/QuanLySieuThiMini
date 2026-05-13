package Logic.KhuyenMai.Rules;

import Logic.KhuyenMai.IGiamGiaRule;
import Logic.KhuyenMai.GiamGiaContext;

public class TonKhoRule implements IGiamGiaRule {
    @Override
    public double tinhPhanTramGiam(GiamGiaContext ctx) { // Đã sửa tên hàm ở đây
        if (ctx.tonKho >= 500) return 0.15; // Giảm 15%
        if (ctx.tonKho >= 300) return 0.10; // Giảm 10%
        if (ctx.tonKho >= 100) return 0.05; // Giảm 5%
        return 0;
    }

    @Override
    public String layTagHienThi() { 
        return "🔥 Xả Kho"; 
    }
}