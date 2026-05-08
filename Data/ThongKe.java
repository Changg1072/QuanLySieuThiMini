package Data;

import java.time.LocalDate;
import java.math.BigDecimal;

public class ThongKe {
    private String MaThongKe;
    private LocalDate NgayThongKe;
    private BigDecimal TongDoanhThu;
    private BigDecimal TongLoiNhuan;
    private int TongDonHang;
    private int TongKhachMoi;
    private BigDecimal TongTienGiam;
    private BigDecimal ChiPhiNhapHang;
    private String MaNV;
    private BigDecimal ThietHai;

    private ThongKe(ThoXayThongKe builder) {
        this.MaThongKe = builder.maThongKe;
        this.NgayThongKe = builder.ngayThongKe;
        this.TongDoanhThu = builder.tongDoanhThu;
        this.TongLoiNhuan = builder.tongLoiNhuan;
        this.TongDonHang = builder.tongDonHang;
        this.TongKhachMoi = builder.tongKhachMoi;
        this.TongTienGiam = builder.tongTienGiam;
        this.ChiPhiNhapHang = builder.chiPhiNhapHang;
        this.MaNV = builder.maNV;
        this.ThietHai = builder.thietHai;
    }

    public ThongKe() {}
    public String getMaThongKe() { return MaThongKe; }
    public void setMaThongKe(String maThongKe) { MaThongKe = maThongKe; }
    public LocalDate getNgayThongKe() { return NgayThongKe; }
    public void setNgayThongKe(LocalDate ngayThongKe) { NgayThongKe = ngayThongKe; }
    public BigDecimal getTongDoanhThu() { return TongDoanhThu; }
    public void setTongDoanhThu(BigDecimal tongDoanhThu) { TongDoanhThu = tongDoanhThu; }
    public BigDecimal getTongLoiNhuan() { return TongLoiNhuan; }
    public void setTongLoiNhuan(BigDecimal tongLoiNhuan) { TongLoiNhuan = tongLoiNhuan; }
    public int getTongDonHang() { return TongDonHang; }
    public void setTongDonHang(int tongDonHang) { TongDonHang = tongDonHang; }
    public int getTongKhachMoi() { return TongKhachMoi; }
    public void setTongKhachMoi(int tongKhachMoi) { TongKhachMoi = tongKhachMoi; }
    public BigDecimal getTongTienGiam() { return TongTienGiam; }
    public void setTongTienGiam(BigDecimal tongTienGiam) { TongTienGiam = tongTienGiam; }
    public BigDecimal getChiPhiNhapHang() { return ChiPhiNhapHang; }
    public void setChiPhiNhapHang(BigDecimal chiPhiNhapHang) { ChiPhiNhapHang = chiPhiNhapHang; }
    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }
    public BigDecimal getThietHai() { return ThietHai; }
    public void setThietHai(BigDecimal thietHai) { ThietHai = thietHai; }

    public static class ThoXayThongKe {
        private String maThongKe;
        private LocalDate ngayThongKe;
        private BigDecimal tongDoanhThu;
        private BigDecimal tongLoiNhuan;
        private int tongDonHang;
        private int tongKhachMoi;
        private BigDecimal tongTienGiam;
        private BigDecimal chiPhiNhapHang;
        private String maNV;
        private BigDecimal thietHai;

        public ThoXayThongKe ganMaThongKe(String ma) { this.maThongKe = ma; return this; }
        public ThoXayThongKe ganNgayThongKe(LocalDate ngay) { this.ngayThongKe = ngay; return this; }
        public ThoXayThongKe ganTongDoanhThu(BigDecimal tien) { this.tongDoanhThu = tien; return this; }
        public ThoXayThongKe ganTongLoiNhuan(BigDecimal tien) { this.tongLoiNhuan = tien; return this; }
        public ThoXayThongKe ganTongDonHang(int sl) { this.tongDonHang = sl; return this; }
        public ThoXayThongKe ganTongKhachMoi(int sl) { this.tongKhachMoi = sl; return this; }
        public ThoXayThongKe ganTongTienGiam(BigDecimal tien) { this.tongTienGiam = tien; return this; }
        public ThoXayThongKe ganChiPhiNhapHang(BigDecimal tien) { this.chiPhiNhapHang = tien; return this; }
        public ThoXayThongKe ganMaNV(String nv) { this.maNV = nv; return this; }
        public ThoXayThongKe ganThietHai(BigDecimal tien) { this.thietHai = tien; return this; }

        public ThongKe taoMoi() { return new ThongKe(this); }
    }
}