package Data;

import java.math.BigDecimal;

public class SanPham {
    private String MaSP;
    private String TenSP;
    private String LinkHinhAnh;
    private String MaLoai;
    private BigDecimal GiaBan;
    private String DonViTinh;

    private SanPham(ThoXaySanPham builder) {
        this.MaSP = builder.maSP;
        this.TenSP = builder.tenSP;
        this.LinkHinhAnh = builder.linkHinhAnh;
        this.MaLoai = builder.maLoai;
        this.GiaBan = builder.giaBan;
        this.DonViTinh = builder.donViTinh;
    }

    public SanPham() {}
    public String getMaSP() { return MaSP; }
    public void setMaSP(String maSP) { MaSP = maSP; }
    public String getTenSP() { return TenSP; }
    public void setTenSP(String tenSP) { TenSP = tenSP; }
    public String getLinkHinhAnh() { return LinkHinhAnh; }
    public void setLinkHinhAnh(String linkHinhAnh) { LinkHinhAnh = linkHinhAnh; }
    public String getMaLoai() { return MaLoai; }
    public void setMaLoai(String maLoai) { MaLoai = maLoai; }
    public BigDecimal getGiaBan() { return GiaBan; }
    public void setGiaBan(BigDecimal giaBan) { GiaBan = giaBan; }
    public String getDonViTinh() { return DonViTinh; }
    public void setDonViTinh(String donViTinh) { DonViTinh = donViTinh; }

    public static class ThoXaySanPham {
        private String maSP;
        private String tenSP;
        private String linkHinhAnh;
        private String maLoai;
        private BigDecimal giaBan;
        private String donViTinh;

        public ThoXaySanPham ganMaSP(String maSP) { this.maSP = maSP; return this; }
        public ThoXaySanPham ganTenSP(String tenSP) { this.tenSP = tenSP; return this; }
        public ThoXaySanPham ganLinkHinhAnh(String linkHinhAnh) { this.linkHinhAnh = linkHinhAnh; return this; }
        public ThoXaySanPham ganMaLoai(String maLoai) { this.maLoai = maLoai; return this; }
        public ThoXaySanPham ganGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; return this; }
        public ThoXaySanPham ganDonViTinh(String donViTinh) { this.donViTinh = donViTinh; return this; }

        public SanPham taoMoi() { return new SanPham(this); }
    }
}