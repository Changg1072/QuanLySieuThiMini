package Data;

import java.math.BigDecimal;

public class ChiTietPhieuHuy {
    private String MaPhieuHuy;
    private String MaLoHang;
    private String MaSP;
    private int SoLuongHuy;
    private BigDecimal GiaTriHuy;
    private String LyDoChiTiet;

    public ChiTietPhieuHuy() {}

    private ChiTietPhieuHuy(ThoXayChiTietPhieuHuy builder) {
        this.MaPhieuHuy = builder.maPhieuHuy;
        this.MaLoHang = builder.maLoHang;
        this.MaSP = builder.maSP;
        this.SoLuongHuy = builder.soLuongHuy;
        this.GiaTriHuy = builder.giaTriHuy;
        this.LyDoChiTiet = builder.lyDoChiTiet;
    }

    // Getters & Setters
    public String getMaPhieuHuy() { return MaPhieuHuy; }
    public void setMaPhieuHuy(String maPhieuHuy) { MaPhieuHuy = maPhieuHuy; }

    public String getMaLoHang() { return MaLoHang; }
    public void setMaLoHang(String maLoHang) { MaLoHang = maLoHang; }

    public String getMaSP() { return MaSP; }
    public void setMaSP(String maSP) { MaSP = maSP; }

    public int getSoLuongHuy() { return SoLuongHuy; }
    public void setSoLuongHuy(int soLuongHuy) { SoLuongHuy = soLuongHuy; }

    public BigDecimal getGiaTriHuy() { return GiaTriHuy; }
    public void setGiaTriHuy(BigDecimal giaTriHuy) { GiaTriHuy = giaTriHuy; }

    public String getLyDoChiTiet() { return LyDoChiTiet; }
    public void setLyDoChiTiet(String lyDoChiTiet) { LyDoChiTiet = lyDoChiTiet; }

    // Builder Class
    public static class ThoXayChiTietPhieuHuy {
        private String maPhieuHuy;
        private String maLoHang;
        private String maSP;
        private int soLuongHuy;
        private BigDecimal giaTriHuy;
        private String lyDoChiTiet;

        public ThoXayChiTietPhieuHuy ganMaPhieuHuy(String maPhieuHuy) { this.maPhieuHuy = maPhieuHuy; return this; }
        public ThoXayChiTietPhieuHuy ganMaLoHang(String maLoHang) { this.maLoHang = maLoHang; return this; }
        public ThoXayChiTietPhieuHuy ganMaSP(String maSP) { this.maSP = maSP; return this; }
        public ThoXayChiTietPhieuHuy ganSoLuongHuy(int soLuongHuy) { this.soLuongHuy = soLuongHuy; return this; }
        public ThoXayChiTietPhieuHuy ganGiaTriHuy(BigDecimal giaTriHuy) { this.giaTriHuy = giaTriHuy; return this; }
        public ThoXayChiTietPhieuHuy ganLyDoChiTiet(String lyDoChiTiet) { this.lyDoChiTiet = lyDoChiTiet; return this; }

        public ChiTietPhieuHuy taoMoi() { return new ChiTietPhieuHuy(this); }
    }
}