package Logic.KhuyenMai.Rules;

import Logic.KhuyenMai.IGiamGiaRule;
import Logic.KhuyenMai.GiamGiaContext;
import java.time.temporal.ChronoUnit;

public class HanSuDungRule implements IGiamGiaRule {
    @Override
    public double tinhPhanTramGiam(GiamGiaContext ctx) {
        if (ctx.hanSuDung == null) return 0;
        
        long ngayConLai = ChronoUnit.DAYS.between(ctx.thoiGianHienTai.toLocalDate(), ctx.hanSuDung);
        
        if (ngayConLai <= 0) return 0; // Đã hỏng, chờ hủy
        if (ngayConLai == 1) return 0.50; // Còn 1 ngày: Giảm 50%
        if (ngayConLai <= 3) return 0.20; // Còn <=3 ngày: Giảm 20%
        
        return 0;
    }

    @Override
    public String layTagHienThi() { return "⏳ Cận Date"; }
}