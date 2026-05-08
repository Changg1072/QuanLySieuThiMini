package Data;

import java.time.LocalDate;
import java.math.BigDecimal;

public class NhanVien {
    private String MaNV;
    private String HoTen;
    private String SDT;
    private String ChucVu;
    private String TrangThai;
    private BigDecimal LuongGio;
    private LocalDate NgayVaoLam;
    private LocalDate NgayNghiViec;

    private NhanVien(ThoXayNhanVien builder) {
        this.MaNV = builder.maNV;
        this.HoTen = builder.hoTen;
        this.SDT = builder.sdt;
        this.ChucVu = builder.chucVu;
        this.TrangThai = builder.trangThai;
        this.LuongGio = builder.luongGio;
        this.NgayVaoLam = builder.ngayVaoLam;
        this.NgayNghiViec = builder.ngayNghiViec;
    }

    public NhanVien() {}
    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }
    public String getHoTen() { return HoTen; }
    public void setHoTen(String hoTen) { HoTen = hoTen; }
    public String getSDT() { return SDT; }
    public void setSDT(String sdt) { SDT = sdt; }
    public String getChucVu() { return ChucVu; }
    public void setChucVu(String chucVu) { ChucVu = chucVu; }
    public String getTrangThai() { return TrangThai; }
    public void setTrangThai(String trangThai) { TrangThai = trangThai; }
    public BigDecimal getLuongGio() { return LuongGio; }
    public void setLuongGio(BigDecimal luongGio) { LuongGio = luongGio; }
    public LocalDate getNgayVaoLam() { return NgayVaoLam; }
    public void setNgayVaoLam(LocalDate ngayVaoLam) { NgayVaoLam = ngayVaoLam; }
    public LocalDate getNgayNghiViec() { return NgayNghiViec; }
    public void setNgayNghiViec(LocalDate ngayNghiViec) { NgayNghiViec = ngayNghiViec; }

    public static class ThoXayNhanVien {
        private String maNV;
        private String hoTen;
        private String sdt;
        private String chucVu;
        private String trangThai;
        private BigDecimal luongGio;
        private LocalDate ngayVaoLam;
        private LocalDate ngayNghiViec;

        public ThoXayNhanVien ganMaNV(String maNV) { this.maNV = maNV; return this; }
        public ThoXayNhanVien ganHoTen(String hoTen) { this.hoTen = hoTen; return this; }
        public ThoXayNhanVien ganSDT(String sdt) { this.sdt = sdt; return this; }
        public ThoXayNhanVien ganChucVu(String chucVu) { this.chucVu = chucVu; return this; }
        public ThoXayNhanVien ganTrangThai(String trangThai) { this.trangThai = trangThai; return this; }
        public ThoXayNhanVien ganLuongGio(BigDecimal luongGio) { this.luongGio = luongGio; return this; }
        public ThoXayNhanVien ganNgayVaoLam(LocalDate ngayVaoLam) { this.ngayVaoLam = ngayVaoLam; return this; }
        public ThoXayNhanVien ganNgayNghiViec(LocalDate ngayNghiViec) { this.ngayNghiViec = ngayNghiViec; return this; }

        public NhanVien taoMoi() { return new NhanVien(this); }
    }
}