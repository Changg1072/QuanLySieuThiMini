package Data;

import java.time.LocalDate;
import java.math.BigDecimal;

public class KhachHang {
    private String MaKH;
    private String HoTen;
    private String SDT;
    private BigDecimal DiemTichLuy;
    private LocalDate NgayDangKy;
    private String BacKH;

    private KhachHang(ThoXayKhachHang builder) {
        this.MaKH = builder.maKH;
        this.HoTen = builder.hoTen;
        this.SDT = builder.sdt;
        this.DiemTichLuy = builder.diemTichLuy;
        this.NgayDangKy = builder.ngayDangKy;
        this.BacKH = builder.bacKH;
    }

    public KhachHang() {}
    public String getMaKH() { return MaKH; }
    public void setMaKH(String maKH) { MaKH = maKH; }
    public String getHoTen() { return HoTen; }
    public void setHoTen(String hoTen) { HoTen = hoTen; }
    public String getSDT() { return SDT; }
    public void setSDT(String sdt) { SDT = sdt; }
    public BigDecimal getDiemTichLuy() { return DiemTichLuy; }
    public void setDiemTichLuy(BigDecimal diemTichLuy) { DiemTichLuy = diemTichLuy; }
    public LocalDate getNgayDangKy() { return NgayDangKy; }
    public void setNgayDangKy(LocalDate ngayDangKy) { NgayDangKy = ngayDangKy; }
    public String getBacKH() { return BacKH; }
    public void setBacKH(String bacKH) { BacKH = bacKH; }

    public static class ThoXayKhachHang {
        private String maKH;
        private String hoTen;
        private String sdt;
        private BigDecimal diemTichLuy;
        private LocalDate ngayDangKy;
        private String bacKH;

        public ThoXayKhachHang ganMaKH(String maKH) { this.maKH = maKH; return this; }
        public ThoXayKhachHang ganHoTen(String hoTen) { this.hoTen = hoTen; return this; }
        public ThoXayKhachHang ganSDT(String sdt) { this.sdt = sdt; return this; }
        public ThoXayKhachHang ganDiemTichLuy(BigDecimal diemTichLuy) { this.diemTichLuy = diemTichLuy; return this; }
        public ThoXayKhachHang ganNgayDangKy(LocalDate ngayDangKy) { this.ngayDangKy = ngayDangKy; return this; }
        public ThoXayKhachHang ganBacKH(String bacKH) { this.bacKH = bacKH; return this; }

        public KhachHang taoMoi() { return new KhachHang(this); }
    }
}