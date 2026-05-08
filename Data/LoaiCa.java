package Data;

import java.time.LocalTime;

public class LoaiCa {
    private String MaLoaiCa;
    private String TenCa;
    private LocalTime GioBatDau;
    private LocalTime GioKetThuc;

    private LoaiCa(ThoXayLoaiCa builder) {
        this.MaLoaiCa = builder.maLoaiCa;
        this.TenCa = builder.tenCa;
        this.GioBatDau = builder.gioBatDau;
        this.GioKetThuc = builder.gioKetThuc;
    }

    public LoaiCa() {}
    public String getMaLoaiCa() { return MaLoaiCa; }
    public void setMaLoaiCa(String maLoaiCa) { MaLoaiCa = maLoaiCa; }
    public String getTenCa() { return TenCa; }
    public void setTenCa(String tenCa) { TenCa = tenCa; }
    public LocalTime getGioBatDau() { return GioBatDau; }
    public void setGioBatDau(LocalTime gioBatDau) { GioBatDau = gioBatDau; }
    public LocalTime getGioKetThuc() { return GioKetThuc; }
    public void setGioKetThuc(LocalTime gioKetThuc) { GioKetThuc = gioKetThuc; }

    public static class ThoXayLoaiCa {
        private String maLoaiCa;
        private String tenCa;
        private LocalTime gioBatDau;
        private LocalTime gioKetThuc;

        public ThoXayLoaiCa ganMaLoaiCa(String maLoaiCa) { this.maLoaiCa = maLoaiCa; return this; }
        public ThoXayLoaiCa ganTenCa(String tenCa) { this.tenCa = tenCa; return this; }
        public ThoXayLoaiCa ganGioBatDau(LocalTime gioBatDau) { this.gioBatDau = gioBatDau; return this; }
        public ThoXayLoaiCa ganGioKetThuc(LocalTime gioKetThuc) { this.gioKetThuc = gioKetThuc; return this; }

        public LoaiCa taoMoi() { return new LoaiCa(this); }
    }
}