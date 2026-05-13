package Logic.KhuyenMai;

public interface IGiamGiaRule {
    // Trả về mức phần trăm giảm giá (VD: 0.15 = 15%)
    double tinhPhanTramGiam(GiamGiaContext ctx);
    
    // Trả về Tag để UI hiển thị (VD: "Giờ vàng", "Cận date")
    String layTagHienThi();
}