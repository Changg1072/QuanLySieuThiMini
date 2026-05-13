package Logic.KhuyenMai.Rules;

import Logic.KhuyenMai.IGiamGiaRule;
import Logic.KhuyenMai.GiamGiaContext;

public class GioVangRule implements IGiamGiaRule {
    @Override
    public double tinhPhanTramGiam(GiamGiaContext ctx) {
        // Chỉ áp dụng cho hàng tươi sống, rau củ, đồ ăn trong ngày
        if (!ctx.maLoai.equals("RAU_CU") && !ctx.maLoai.equals("THUC_PHAM_TUOI") && !ctx.maLoai.equals("DO_AN_TRONG_NGAY")) {
            return 0;
        }

        int gioHienTai = ctx.thoiGianHienTai.getHour();
        int phutHienTai = ctx.thoiGianHienTai.getMinute();

        // Thuật toán: Càng về khuya, tồn càng nhiều -> Giảm càng mạnh
        if (gioHienTai >= 21 && phutHienTai >= 30) {
            return (ctx.tonKho > 50) ? 0.50 : 0.30; // Sau 21h30
        } else if (gioHienTai >= 20) {
            return 0.20; // Từ 20h - 21h30
        } else if (gioHienTai >= 17) {
            return 0.10; // Từ 17h - 20h
        }
        
        return 0;
    }

    @Override
    public String layTagHienThi() { return "⚡ Giờ Vàng"; }
}