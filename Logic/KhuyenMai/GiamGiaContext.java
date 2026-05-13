package Logic.KhuyenMai;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GiamGiaContext {
    public String maSP;
    public String maLoai; // Ví dụ: "RAU_CU", "DONG_LANH", "HANG_KHO"...
    public int tonKho;
    public double giaNhap;
    public double giaBan;
    public LocalDate ngayNhapKho;
    public LocalDate hanSuDung;
    public LocalDateTime thoiGianHienTai;
    public double tocDoBanHang; // VD: Số lượng bán / ngày

    // Constructor và Getters/Setters...
    public GiamGiaContext(String maSP, String maLoai, int tonKho, double giaNhap, double giaBan, 
                          LocalDate ngayNhapKho, LocalDate hanSuDung) {
        this.maSP = maSP;
        this.maLoai = maLoai;
        this.tonKho = tonKho;
        this.giaNhap = giaNhap;
        this.giaBan = giaBan;
        this.ngayNhapKho = ngayNhapKho;
        this.hanSuDung = hanSuDung;
        this.thoiGianHienTai = LocalDateTime.now();
    }
}