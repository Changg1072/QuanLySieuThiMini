package Data;

import java.math.BigDecimal;

public class ChiTietHoaDon {
    private String MaHD;
    private String MaSp;
    private String MaLoHang;
    private int SoLuong;
    private BigDecimal DonGia;
    private String MaGiamGia;
    private BigDecimal ThanhTienSanPham;

    private ChiTietHoaDon(ThoXayChiTietHoaDon builder) {
        this.MaHD = builder.maHD;
        this.MaSp = builder.maSp;
        this.MaLoHang = builder.maLoHang;
        this.SoLuong = builder.soLuong;
        this.DonGia = builder.donGia;
        this.MaGiamGia = builder.maGiamGia;
        this.ThanhTienSanPham = builder.thanhTienSanPham;
    }

    public ChiTietHoaDon() {}
    public String getMaHD() { return MaHD; }
    public void setMaHD(String maHD) { MaHD = maHD; }
    public String getMaSp() { return MaSp; }
    public void setMaSp(String maSp) { MaSp = maSp; }
    public String getMaLoHang() { return MaLoHang; }
    public void setMaLoHang(String maLoHang) { MaLoHang = maLoHang; }
    public int getSoLuong() { return SoLuong; }
    public void setSoLuong(int soLuong) { SoLuong = soLuong; }
    public BigDecimal getDonGia() { return DonGia; }
    public void setDonGia(BigDecimal donGia) { DonGia = donGia; }
    public String getMaGiamGia() { return MaGiamGia; }
    public void setMaGiamGia(String maGiamGia) { MaGiamGia = maGiamGia; }
    public BigDecimal getThanhTienSanPham() { return ThanhTienSanPham; }
    public void setThanhTienSanPham(BigDecimal thanhTienSanPham) { ThanhTienSanPham = thanhTienSanPham; }

    public static class ThoXayChiTietHoaDon {
        private String maHD;
        private String maSp;
        private String maLoHang;
        private int soLuong;
        private BigDecimal donGia;
        private String maGiamGia;
        private BigDecimal thanhTienSanPham;

        public ThoXayChiTietHoaDon ganMaHD(String maHD) { this.maHD = maHD; return this; }
        public ThoXayChiTietHoaDon ganMaSp(String maSp) { this.maSp = maSp; return this; }
        public ThoXayChiTietHoaDon ganMaLoHang(String maLoHang) { this.maLoHang = maLoHang; return this; }
        public ThoXayChiTietHoaDon ganSoLuong(int soLuong) { this.soLuong = soLuong; return this; }
        public ThoXayChiTietHoaDon ganDonGia(BigDecimal donGia) { this.donGia = donGia; return this; }
        public ThoXayChiTietHoaDon ganMaGiamGia(String maGiamGia) { this.maGiamGia = maGiamGia; return this; }
        public ThoXayChiTietHoaDon ganThanhTienSanPham(BigDecimal thanhTienSanPham) { this.thanhTienSanPham = thanhTienSanPham; return this; }

        public ChiTietHoaDon taoMoi() { return new ChiTietHoaDon(this); }
    }
}