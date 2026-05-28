package Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CauHinhLuong {
    private int MaCauHinh;
    private String MaNV;
    private BigDecimal LuongTheoGio;
    private BigDecimal HeSoLuong;
    private BigDecimal PhuCapCoDinh;
    private BigDecimal HeSoTangCa;
    private LocalDate NgayApDung;
    private String TrangThai;

    private CauHinhLuong(ThoXayCauHinhLuong builder) {
        this.MaCauHinh = builder.maCauHinh;
        this.MaNV = builder.maNV;
        this.LuongTheoGio = builder.luongTheoGio;
        this.HeSoLuong = builder.heSoLuong;
        this.PhuCapCoDinh = builder.phuCapCoDinh;
        this.HeSoTangCa = builder.heSoTangCa;
        this.NgayApDung = builder.ngayApDung;
        this.TrangThai = builder.trangThai;
    }

    public CauHinhLuong() {}

    public int getMaCauHinh() { return MaCauHinh; }
    public void setMaCauHinh(int maCauHinh) { MaCauHinh = maCauHinh; }
    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }
    public BigDecimal getLuongTheoGio() { return LuongTheoGio; }
    public void setLuongTheoGio(BigDecimal luongTheoGio) { LuongTheoGio = luongTheoGio; }
    public BigDecimal getHeSoLuong() { return HeSoLuong; }
    public void setHeSoLuong(BigDecimal heSoLuong) { HeSoLuong = heSoLuong; }
    public BigDecimal getPhuCapCoDinh() { return PhuCapCoDinh; }
    public void setPhuCapCoDinh(BigDecimal phuCapCoDinh) { PhuCapCoDinh = phuCapCoDinh; }
    public BigDecimal getHeSoTangCa() { return HeSoTangCa; }
    public void setHeSoTangCa(BigDecimal heSoTangCa) { HeSoTangCa = heSoTangCa; }
    public LocalDate getNgayApDung() { return NgayApDung; }
    public void setNgayApDung(LocalDate ngayApDung) { NgayApDung = ngayApDung; }
    public String getTrangThai() { return TrangThai; }
    public void setTrangThai(String trangThai) { TrangThai = trangThai; }

    public static class ThoXayCauHinhLuong {
        private int maCauHinh;
        private String maNV;
        private BigDecimal luongTheoGio;
        private BigDecimal heSoLuong;
        private BigDecimal phuCapCoDinh;
        private BigDecimal heSoTangCa;
        private LocalDate ngayApDung;
        private String trangThai;

        public ThoXayCauHinhLuong ganMaCauHinh(int maCauHinh) { this.maCauHinh = maCauHinh; return this; }
        public ThoXayCauHinhLuong ganMaNV(String maNV) { this.maNV = maNV; return this; }
        public ThoXayCauHinhLuong ganLuongTheoGio(BigDecimal luongTheoGio) { this.luongTheoGio = luongTheoGio; return this; }
        public ThoXayCauHinhLuong ganHeSoLuong(BigDecimal heSoLuong) { this.heSoLuong = heSoLuong; return this; }
        public ThoXayCauHinhLuong ganPhuCapCoDinh(BigDecimal phuCapCoDinh) { this.phuCapCoDinh = phuCapCoDinh; return this; }
        public ThoXayCauHinhLuong ganHeSoTangCa(BigDecimal heSoTangCa) { this.heSoTangCa = heSoTangCa; return this; }
        public ThoXayCauHinhLuong ganNgayApDung(LocalDate ngayApDung) { this.ngayApDung = ngayApDung; return this; }
        public ThoXayCauHinhLuong ganTrangThai(String trangThai) { this.trangThai = trangThai; return this; }

        public CauHinhLuong taoMoi() { return new CauHinhLuong(this); }
    }
}
