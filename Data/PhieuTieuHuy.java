package Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PhieuTieuHuy {
    private String MaPhieuHuy;
    private LocalDateTime NgayTao;
    private String MaNV;
    private int TongSoLuong;
    private BigDecimal TongGiaTriHuy;
    private String LyDoHuy;
    private String TrangThaiHuy;

    public PhieuTieuHuy() {}

    private PhieuTieuHuy(ThoXayPhieuTieuHuy builder) {
        this.MaPhieuHuy = builder.maPhieuHuy;
        this.NgayTao = builder.ngayTao;
        this.MaNV = builder.maNV;
        this.TongSoLuong = builder.tongSoLuong;
        this.TongGiaTriHuy = builder.tongGiaTriHuy;
        this.LyDoHuy = builder.lyDoHuy;
        this.TrangThaiHuy = builder.trangThaiHuy;
    }

    // Getters & Setters
    public String getMaPhieuHuy() { return MaPhieuHuy; }
    public void setMaPhieuHuy(String maPhieuHuy) { MaPhieuHuy = maPhieuHuy; }

    public LocalDateTime getNgayTao() { return NgayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { NgayTao = ngayTao; }

    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }

    public int getTongSoLuong() { return TongSoLuong; }
    public void setTongSoLuong(int tongSoLuong) { TongSoLuong = tongSoLuong; }

    public BigDecimal getTongGiaTriHuy() { return TongGiaTriHuy; }
    public void setTongGiaTriHuy(BigDecimal tongGiaTriHuy) { TongGiaTriHuy = tongGiaTriHuy; }

    public String getLyDoHuy() { return LyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { LyDoHuy = lyDoHuy; }

    public String getTrangThaiHuy() { return TrangThaiHuy; }
    public void setTrangThaiHuy(String trangThaiHuy) { TrangThaiHuy = trangThaiHuy; }

    // Builder Class
    public static class ThoXayPhieuTieuHuy {
        private String maPhieuHuy;
        private LocalDateTime ngayTao;
        private String maNV;
        private int tongSoLuong;
        private BigDecimal tongGiaTriHuy;
        private String lyDoHuy;
        private String trangThaiHuy;

        public ThoXayPhieuTieuHuy ganMaPhieuHuy(String maPhieuHuy) { this.maPhieuHuy = maPhieuHuy; return this; }
        public ThoXayPhieuTieuHuy ganNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; return this; }
        public ThoXayPhieuTieuHuy ganMaNV(String maNV) { this.maNV = maNV; return this; }
        public ThoXayPhieuTieuHuy ganTongSoLuong(int tongSoLuong) { this.tongSoLuong = tongSoLuong; return this; }
        public ThoXayPhieuTieuHuy ganTongGiaTriHuy(BigDecimal tongGiaTriHuy) { this.tongGiaTriHuy = tongGiaTriHuy; return this; }
        public ThoXayPhieuTieuHuy ganLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; return this; }
        public ThoXayPhieuTieuHuy ganTrangThaiHuy(String trangThaiHuy) { this.trangThaiHuy = trangThaiHuy; return this; }

        public PhieuTieuHuy taoMoi() { return new PhieuTieuHuy(this); }
    }
}