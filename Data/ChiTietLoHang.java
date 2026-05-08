package Data;

import java.time.LocalDate;
import java.math.BigDecimal;

public class ChiTietLoHang {
    private String MaLoHang;
    private String MaSP;
    private BigDecimal GiaNhap;
    private int SoLuongNhap;
    private LocalDate NSX;
    private LocalDate HSD;
    private int SoLuongTon;

    private ChiTietLoHang(ThoXayChiTietLoHang builder) {
        this.MaLoHang = builder.maLoHang;
        this.MaSP = builder.maSP;
        this.GiaNhap = builder.giaNhap;
        this.SoLuongNhap = builder.soLuongNhap;
        this.NSX = builder.nsx;
        this.HSD = builder.hsd;
        this.SoLuongTon = builder.soLuongTon;
    }

    public ChiTietLoHang() {}
    public String getMaLoHang() { return MaLoHang; }
    public void setMaLoHang(String maLoHang) { MaLoHang = maLoHang; }
    public String getMaSP() { return MaSP; }
    public void setMaSP(String maSP) { MaSP = maSP; }
    public BigDecimal getGiaNhap() { return GiaNhap; }
    public void setGiaNhap(BigDecimal giaNhap) { GiaNhap = giaNhap; }
    public int getSoLuongNhap() { return SoLuongNhap; }
    public void setSoLuongNhap(int soLuongNhap) { SoLuongNhap = soLuongNhap; }
    public LocalDate getNSX() { return NSX; }
    public void setNSX(LocalDate nsx) { NSX = nsx; }
    public LocalDate getHSD() { return HSD; }
    public void setHSD(LocalDate hsd) { HSD = hsd; }
    public int getSoLuongTon() { return SoLuongTon; }
    public void setSoLuongTon(int soLuongTon) { SoLuongTon = soLuongTon; }

    public static class ThoXayChiTietLoHang {
        private String maLoHang;
        private String maSP;
        private BigDecimal giaNhap;
        private int soLuongNhap;
        private LocalDate nsx;
        private LocalDate hsd;
        private int soLuongTon;

        public ThoXayChiTietLoHang ganMaLoHang(String maLoHang) { this.maLoHang = maLoHang; return this; }
        public ThoXayChiTietLoHang ganMaSP(String maSP) { this.maSP = maSP; return this; }
        public ThoXayChiTietLoHang ganGiaNhap(BigDecimal giaNhap) { this.giaNhap = giaNhap; return this; }
        public ThoXayChiTietLoHang ganSoLuongNhap(int soLuongNhap) { this.soLuongNhap = soLuongNhap; return this; }
        public ThoXayChiTietLoHang ganNSX(LocalDate nsx) { this.nsx = nsx; return this; }
        public ThoXayChiTietLoHang ganHSD(LocalDate hsd) { this.hsd = hsd; return this; }
        public ThoXayChiTietLoHang ganSoLuongTon(int soLuongTon) { this.soLuongTon = soLuongTon; return this; }

        public ChiTietLoHang taoMoi() { return new ChiTietLoHang(this); }
    }
}