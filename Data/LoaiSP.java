package Data;

public class LoaiSP {
    private String MaLoai;
    private String TenLoai;

    private LoaiSP(ThoXayLoaiSP builder) {
        this.MaLoai = builder.maLoai;
        this.TenLoai = builder.tenLoai;
    }

    public LoaiSP() {}
    public String getMaLoai() { return MaLoai; }
    public void setMaLoai(String maLoai) { MaLoai = maLoai; }
    public String getTenLoai() { return TenLoai; }
    public void setTenLoai(String tenLoai) { TenLoai = tenLoai; }

    public static class ThoXayLoaiSP {
        private String maLoai;
        private String tenLoai;

        public ThoXayLoaiSP ganMaLoai(String maLoai) { this.maLoai = maLoai; return this; }
        public ThoXayLoaiSP ganTenLoai(String tenLoai) { this.tenLoai = tenLoai; return this; }

        public LoaiSP taoMoi() { return new LoaiSP(this); }
    }
}