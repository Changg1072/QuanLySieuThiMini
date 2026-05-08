package Data;

public class NhaCungCap {
    private String MaNCC;
    private String TenNCC;
    private String SDT;
    private String Email;
    private String DiaChi;

    private NhaCungCap(ThoXayNhaCungCap builder) {
        this.MaNCC = builder.maNCC;
        this.TenNCC = builder.tenNCC;
        this.SDT = builder.sdt;
        this.Email = builder.email;
        this.DiaChi = builder.diaChi;
    }

    public NhaCungCap() {}
    public String getMaNCC() { return MaNCC; }
    public void setMaNCC(String maNCC) { MaNCC = maNCC; }
    public String getTenNCC() { return TenNCC; }
    public void setTenNCC(String tenNCC) { TenNCC = tenNCC; }
    public String getSDT() { return SDT; }
    public void setSDT(String sdt) { SDT = sdt; }
    public String getEmail() { return Email; }
    public void setEmail(String email) { Email = email; }
    public String getDiaChi() { return DiaChi; }
    public void setDiaChi(String diaChi) { DiaChi = diaChi; }

    public static class ThoXayNhaCungCap {
        private String maNCC;
        private String tenNCC;
        private String sdt;
        private String email;
        private String diaChi;

        public ThoXayNhaCungCap ganMaNCC(String maNCC) { this.maNCC = maNCC; return this; }
        public ThoXayNhaCungCap ganTenNCC(String tenNCC) { this.tenNCC = tenNCC; return this; }
        public ThoXayNhaCungCap ganSDT(String sdt) { this.sdt = sdt; return this; }
        public ThoXayNhaCungCap ganEmail(String email) { this.email = email; return this; }
        public ThoXayNhaCungCap ganDiaChi(String diaChi) { this.diaChi = diaChi; return this; }

        public NhaCungCap taoMoi() { return new NhaCungCap(this); }
    }
}