package Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class ChiaCa {
    private String MaCa;
    private String MaLoaiCa;
    private LocalDate NgayLam;
    private String MaNV;
    private LocalDateTime ThoiGianCheckIn;
    private LocalDateTime ThoiGianCheckOut;
    private String TinhTrang;
    private BigDecimal TienDauCa;
    private BigDecimal TienCuoiCa;
    private BigDecimal TienChenhLech;
    private BigDecimal TienBanHang;

    private ChiaCa(ThoXayChiaCa builder) {
        this.MaCa = builder.maCa;
        this.MaLoaiCa = builder.maLoaiCa;
        this.NgayLam = builder.ngayLam;
        this.MaNV = builder.maNV;
        this.ThoiGianCheckIn = builder.thoiGianCheckIn;
        this.ThoiGianCheckOut = builder.thoiGianCheckOut;
        this.TinhTrang = builder.tinhTrang;
        this.TienDauCa = builder.tienDauCa;
        this.TienCuoiCa = builder.tienCuoiCa;
        this.TienChenhLech = builder.tienChenhLech;
        this.TienBanHang = builder.tienBanHang;
    }

    public ChiaCa() {}
    public String getMaCa() { return MaCa; }
    public void setMaCa(String maCa) { MaCa = maCa; }
    public String getMaLoaiCa() { return MaLoaiCa; }
    public void setMaLoaiCa(String maLoaiCa) { MaLoaiCa = maLoaiCa; }
    public LocalDate getNgayLam() { return NgayLam; }
    public void setNgayLam(LocalDate ngayLam) { NgayLam = ngayLam; }
    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }
    public LocalDateTime getThoiGianCheckIn() { return ThoiGianCheckIn; }
    public void setThoiGianCheckIn(LocalDateTime thoiGianCheckIn) { ThoiGianCheckIn = thoiGianCheckIn; }
    public LocalDateTime getThoiGianCheckOut() { return ThoiGianCheckOut; }
    public void setThoiGianCheckOut(LocalDateTime thoiGianCheckOut) { ThoiGianCheckOut = thoiGianCheckOut; }
    public String getTinhTrang() { return TinhTrang; }
    public void setTinhTrang(String tinhTrang) { TinhTrang = tinhTrang; }
    public BigDecimal getTienDauCa() { return TienDauCa; }
    public void setTienDauCa(BigDecimal tienDauCa) { TienDauCa = tienDauCa; }
    public BigDecimal getTienCuoiCa() { return TienCuoiCa; }
    public void setTienCuoiCa(BigDecimal tienCuoiCa) { TienCuoiCa = tienCuoiCa; }
    public BigDecimal getTienChenhLech() { return TienChenhLech; }
    public void setTienChenhLech(BigDecimal tienChenhLech) { TienChenhLech = tienChenhLech; }
    public BigDecimal getTienBanHang() { return TienBanHang; }
    public void setTienBanHang(BigDecimal tienBanHang) { TienBanHang = tienBanHang; }

    public static class ThoXayChiaCa {
        private String maCa;
        private String maLoaiCa;
        private LocalDate ngayLam;
        private String maNV;
        private LocalDateTime thoiGianCheckIn;
        private LocalDateTime thoiGianCheckOut;
        private String tinhTrang;
        private BigDecimal tienDauCa;
        private BigDecimal tienCuoiCa;
        private BigDecimal tienChenhLech;
        private BigDecimal tienBanHang;

        public ThoXayChiaCa ganMaCa(String maCa) { this.maCa = maCa; return this; }
        public ThoXayChiaCa ganMaLoaiCa(String maLoaiCa) { this.maLoaiCa = maLoaiCa; return this; }
        public ThoXayChiaCa ganNgayLam(LocalDate ngayLam) { this.ngayLam = ngayLam; return this; }
        public ThoXayChiaCa ganMaNV(String maNV) { this.maNV = maNV; return this; }
        public ThoXayChiaCa ganThoiGianCheckIn(LocalDateTime thoiGianCheckIn) { this.thoiGianCheckIn = thoiGianCheckIn; return this; }
        public ThoXayChiaCa ganThoiGianCheckOut(LocalDateTime thoiGianCheckOut) { this.thoiGianCheckOut = thoiGianCheckOut; return this; }
        public ThoXayChiaCa ganTinhTrang(String tinhTrang) { this.tinhTrang = tinhTrang; return this; }
        public ThoXayChiaCa ganTienDauCa(BigDecimal tienDauCa) { this.tienDauCa = tienDauCa; return this; }
        public ThoXayChiaCa ganTienCuoiCa(BigDecimal tienCuoiCa) { this.tienCuoiCa = tienCuoiCa; return this; }
        public ThoXayChiaCa ganTienChenhLech(BigDecimal tienChenhLech) { this.tienChenhLech = tienChenhLech; return this; }
        public ThoXayChiaCa ganTienBanHang(BigDecimal tienBanHang) { this.tienBanHang = tienBanHang; return this; }

        public ChiaCa taoMoi() { return new ChiaCa(this); }
    }
}