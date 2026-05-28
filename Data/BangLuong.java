package Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BangLuong {
    private int MaBangLuong;
    private String MaNV;
    private String ThangNam;
    private BigDecimal TongGioLam;
    private BigDecimal GioTangCa;
    private BigDecimal LuongTheoGio;
    private BigDecimal Thuong;
    private BigDecimal KhauTru;
    private BigDecimal PhuCap;
    private BigDecimal TongLuong;
    private LocalDate NgayTinhLuong;

    private BangLuong(ThoXayBangLuong builder) {
        this.MaBangLuong = builder.maBangLuong;
        this.MaNV = builder.maNV;
        this.ThangNam = builder.thangNam;
        this.TongGioLam = builder.tongGioLam;
        this.GioTangCa = builder.gioTangCa;
        this.LuongTheoGio = builder.luongTheoGio;
        this.Thuong = builder.thuong;
        this.KhauTru = builder.khauTru;
        this.PhuCap = builder.phuCap;
        this.TongLuong = builder.tongLuong;
        this.NgayTinhLuong = builder.ngayTinhLuong;
    }

    public BangLuong() {}

    // Getters & Setters
    public int getMaBangLuong() { return MaBangLuong; }
    public void setMaBangLuong(int maBangLuong) { MaBangLuong = maBangLuong; }
    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }
    public String getThangNam() { return ThangNam; }
    public void setThangNam(String thangNam) { ThangNam = thangNam; }
    public BigDecimal getTongGioLam() { return TongGioLam; }
    public void setTongGioLam(BigDecimal tongGioLam) { TongGioLam = tongGioLam; }
    public BigDecimal getGioTangCa() { return GioTangCa; }
    public void setGioTangCa(BigDecimal gioTangCa) { GioTangCa = gioTangCa; }
    public BigDecimal getLuongTheoGio() { return LuongTheoGio; }
    public void setLuongTheoGio(BigDecimal luongTheoGio) { LuongTheoGio = luongTheoGio; }
    public BigDecimal getThuong() { return Thuong; }
    public void setThuong(BigDecimal thuong) { Thuong = thuong; }
    public BigDecimal getKhauTru() { return KhauTru; }
    public void setKhauTru(BigDecimal khauTru) { KhauTru = khauTru; }
    public BigDecimal getPhuCap() { return PhuCap; }
    public void setPhuCap(BigDecimal phuCap) { PhuCap = phuCap; }
    public BigDecimal getTongLuong() { return TongLuong; }
    public void setTongLuong(BigDecimal tongLuong) { TongLuong = tongLuong; }
    public LocalDate getNgayTinhLuong() { return NgayTinhLuong; }
    public void setNgayTinhLuong(LocalDate ngayTinhLuong) { NgayTinhLuong = ngayTinhLuong; }

    public static class ThoXayBangLuong {
        private int maBangLuong;
        private String maNV;
        private String thangNam;
        private BigDecimal tongGioLam;
        private BigDecimal gioTangCa;
        private BigDecimal luongTheoGio;
        private BigDecimal thuong;
        private BigDecimal khauTru;
        private BigDecimal phuCap;
        private BigDecimal tongLuong;
        private LocalDate ngayTinhLuong;

        public ThoXayBangLuong ganMaBangLuong(int maBangLuong) { this.maBangLuong = maBangLuong; return this; }
        public ThoXayBangLuong ganMaNV(String maNV) { this.maNV = maNV; return this; }
        public ThoXayBangLuong ganThangNam(String thangNam) { this.thangNam = thangNam; return this; }
        public ThoXayBangLuong ganTongGioLam(BigDecimal tongGioLam) { this.tongGioLam = tongGioLam; return this; }
        public ThoXayBangLuong ganGioTangCa(BigDecimal gioTangCa) { this.gioTangCa = gioTangCa; return this; }
        public ThoXayBangLuong ganLuongTheoGio(BigDecimal luongTheoGio) { this.luongTheoGio = luongTheoGio; return this; }
        public ThoXayBangLuong ganThuong(BigDecimal thuong) { this.thuong = thuong; return this; }
        public ThoXayBangLuong ganKhauTru(BigDecimal khauTru) { this.khauTru = khauTru; return this; }
        public ThoXayBangLuong ganPhuCap(BigDecimal phuCap) { this.phuCap = phuCap; return this; }
        public ThoXayBangLuong ganTongLuong(BigDecimal tongLuong) { this.tongLuong = tongLuong; return this; }
        public ThoXayBangLuong ganNgayTinhLuong(LocalDate ngayTinhLuong) { this.ngayTinhLuong = ngayTinhLuong; return this; }

        public BangLuong taoMoi() { return new BangLuong(this); }
    }
}
