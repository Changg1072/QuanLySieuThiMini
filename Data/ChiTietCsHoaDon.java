package Data;

import java.math.BigDecimal;

public class ChiTietCsHoaDon {
    private String MaTraHang;
    private String MaSP;
    private int SoLuongTra;
    private BigDecimal ThanhTienHoanTra;

    private ChiTietCsHoaDon(ThoXayChiTietCsHoaDon builder) {
        this.MaTraHang = builder.maTraHang;
        this.MaSP = builder.maSP;
        this.SoLuongTra = builder.soLuongTra;
        this.ThanhTienHoanTra = builder.thanhTienHoanTra;
    }

    public ChiTietCsHoaDon() {}
    public String getMaTraHang() { return MaTraHang; }
    public void setMaTraHang(String maTraHang) { MaTraHang = maTraHang; }
    public String getMaSP() { return MaSP; }
    public void setMaSP(String maSP) { MaSP = maSP; }
    public int getSoLuongTra() { return SoLuongTra; }
    public void setSoLuongTra(int soLuongTra) { SoLuongTra = soLuongTra; }
    public BigDecimal getThanhTienHoanTra() { return ThanhTienHoanTra; }
    public void setThanhTienHoanTra(BigDecimal thanhTienHoanTra) { ThanhTienHoanTra = thanhTienHoanTra; }

    public static class ThoXayChiTietCsHoaDon {
        private String maTraHang;
        private String maSP;
        private int soLuongTra;
        private BigDecimal thanhTienHoanTra;

        public ThoXayChiTietCsHoaDon ganMaTraHang(String maTraHang) { this.maTraHang = maTraHang; return this; }
        public ThoXayChiTietCsHoaDon ganMaSP(String maSP) { this.maSP = maSP; return this; }
        public ThoXayChiTietCsHoaDon ganSoLuongTra(int soLuongTra) { this.soLuongTra = soLuongTra; return this; }
        public ThoXayChiTietCsHoaDon ganThanhTienHoanTra(BigDecimal thanhTienHoanTra) { this.thanhTienHoanTra = thanhTienHoanTra; return this; }

        public ChiTietCsHoaDon taoMoi() { return new ChiTietCsHoaDon(this); }
    }
}