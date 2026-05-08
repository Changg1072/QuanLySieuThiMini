package Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class HoaDon {
    private String MaHD;
    private LocalDateTime NgayTao;
    private String MaKH;
    private String MaNV;
    private BigDecimal ThanhTien;
    private BigDecimal TongGiamGia;
    private BigDecimal TruTichDiem;
    private String PhuongThucTT;
    private BigDecimal KhachDua;
    private BigDecimal TienThua;
    private String GhiChu;

    private HoaDon(ThoXayHoaDon builder) {
        this.MaHD = builder.maHD;
        this.NgayTao = builder.ngayTao;
        this.MaKH = builder.maKH;
        this.MaNV = builder.maNV;
        this.ThanhTien = builder.thanhTien;
        this.TongGiamGia = builder.tongGiamGia;
        this.TruTichDiem = builder.truTichDiem;
        this.PhuongThucTT = builder.phuongThucTT;
        this.KhachDua = builder.khachDua;
        this.TienThua = builder.tienThua;
        this.GhiChu = builder.ghiChu;
    }

    public HoaDon() {}
    public String getMaHD() { return MaHD; }
    public void setMaHD(String maHD) { MaHD = maHD; }
    public LocalDateTime getNgayTao() { return NgayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { NgayTao = ngayTao; }
    public String getMaKH() { return MaKH; }
    public void setMaKH(String maKH) { MaKH = maKH; }
    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }
    public BigDecimal getThanhTien() { return ThanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { ThanhTien = thanhTien; }
    public BigDecimal getTongGiamGia() { return TongGiamGia; }
    public void setTongGiamGia(BigDecimal tongGiamGia) { TongGiamGia = tongGiamGia; }
    public BigDecimal getTruTichDiem() { return TruTichDiem; }
    public void setTruTichDiem(BigDecimal truTichDiem) { TruTichDiem = truTichDiem; }
    public String getPhuongThucTT() { return PhuongThucTT; }
    public void setPhuongThucTT(String phuongThucTT) { PhuongThucTT = phuongThucTT; }
    public BigDecimal getKhachDua() { return KhachDua; }
    public void setKhachDua(BigDecimal khachDua) { KhachDua = khachDua; }
    public BigDecimal getTienThua() { return TienThua; }
    public void setTienThua(BigDecimal tienThua) { TienThua = tienThua; }
    public String getGhiChu() { return GhiChu; }
    public void setGhiChu(String ghiChu) { GhiChu = ghiChu; }

    public static class ThoXayHoaDon {
        private String maHD;
        private LocalDateTime ngayTao;
        private String maKH;
        private String maNV;
        private BigDecimal thanhTien;
        private BigDecimal tongGiamGia;
        private BigDecimal truTichDiem;
        private String phuongThucTT;
        private BigDecimal khachDua;
        private BigDecimal tienThua;
        private String ghiChu;

        public ThoXayHoaDon ganMaHD(String maHD) { this.maHD = maHD; return this; }
        public ThoXayHoaDon ganNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; return this; }
        public ThoXayHoaDon ganMaKH(String maKH) { this.maKH = maKH; return this; }
        public ThoXayHoaDon ganMaNV(String maNV) { this.maNV = maNV; return this; }
        public ThoXayHoaDon ganThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; return this; }
        public ThoXayHoaDon ganTongGiamGia(BigDecimal tongGiamGia) { this.tongGiamGia = tongGiamGia; return this; }
        public ThoXayHoaDon ganTruTichDiem(BigDecimal truTichDiem) { this.truTichDiem = truTichDiem; return this; }
        public ThoXayHoaDon ganPhuongThucTT(String phuongThucTT) { this.phuongThucTT = phuongThucTT; return this; }
        public ThoXayHoaDon ganKhachDua(BigDecimal khachDua) { this.khachDua = khachDua; return this; }
        public ThoXayHoaDon ganTienThua(BigDecimal tienThua) { this.tienThua = tienThua; return this; }
        public ThoXayHoaDon ganGhiChu(String ghiChu) { this.ghiChu = ghiChu; return this; }

        public HoaDon taoMoi() { return new HoaDon(this); }
    }
}