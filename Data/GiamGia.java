package Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class GiamGia {
    private String MaGiamGia;
    private String MaSP;
    private LocalDateTime BatDau;
    private LocalDateTime KetThuc;
    private BigDecimal GiamGia;
    private String LoaiGiamGia;
    private String TrangThaiGiamGia;
    private int SoLuongApDung;

    private GiamGia(ThoXayGiamGia builder) {
        this.MaGiamGia = builder.maGiamGia;
        this.MaSP = builder.maSP;
        this.BatDau = builder.batDau;
        this.KetThuc = builder.ketThuc;
        this.GiamGia = builder.giamGia;
        this.LoaiGiamGia = builder.loaiGiamGia;
        this.TrangThaiGiamGia = builder.trangThaiGiamGia;
        this.SoLuongApDung = builder.soLuongApDung;
    }

    public GiamGia() {}
    public String getMaGiamGia() { return MaGiamGia; }
    public void setMaGiamGia(String maGiamGia) { MaGiamGia = maGiamGia; }
    public String getMaSP() { return MaSP; }
    public void setMaSP(String maSP) { MaSP = maSP; }
    public LocalDateTime getBatDau() { return BatDau; }
    public void setBatDau(LocalDateTime batDau) { BatDau = batDau; }
    public LocalDateTime getKetThuc() { return KetThuc; }
    public void setKetThuc(LocalDateTime ketThuc) { KetThuc = ketThuc; }
    public BigDecimal getGiamGia() { return GiamGia; }
    public void setGiamGia(BigDecimal giamGia) { GiamGia = giamGia; }
    public String getLoaiGiamGia() { return LoaiGiamGia; }
    public void setLoaiGiamGia(String loaiGiamGia) { LoaiGiamGia = loaiGiamGia; }
    public String getTrangThaiGiamGia() { return TrangThaiGiamGia; }
    public void setTrangThaiGiamGia(String trangThaiGiamGia) { TrangThaiGiamGia = trangThaiGiamGia; }
    public int getSoLuongApDung() { return SoLuongApDung; }
    public void setSoLuongApDung(int soLuongApDung) { SoLuongApDung = soLuongApDung; }

    public static class ThoXayGiamGia {
        private String maGiamGia;
        private String maSP;
        private LocalDateTime batDau;
        private LocalDateTime ketThuc;
        private BigDecimal giamGia;
        private String loaiGiamGia;
        private String trangThaiGiamGia;
        private int soLuongApDung;

        public ThoXayGiamGia ganMaGiamGia(String maGiamGia) { this.maGiamGia = maGiamGia; return this; }
        public ThoXayGiamGia ganMaSP(String maSP) { this.maSP = maSP; return this; }
        public ThoXayGiamGia ganBatDau(LocalDateTime batDau) { this.batDau = batDau; return this; }
        public ThoXayGiamGia ganKetThuc(LocalDateTime ketThuc) { this.ketThuc = ketThuc; return this; }
        public ThoXayGiamGia ganGiamGia(BigDecimal giamGia) { this.giamGia = giamGia; return this; }
        public ThoXayGiamGia ganLoaiGiamGia(String loaiGiamGia) { this.loaiGiamGia = loaiGiamGia; return this; }
        public ThoXayGiamGia ganTrangThaiGiamGia(String trangThaiGiamGia) { this.trangThaiGiamGia = trangThaiGiamGia; return this; }
        public ThoXayGiamGia ganSoLuongApDung(int soLuongApDung) { this.soLuongApDung = soLuongApDung; return this; }

        public GiamGia taoMoi() { return new GiamGia(this); }
    }
}