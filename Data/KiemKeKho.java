package Data;

import java.time.LocalDate;

public class KiemKeKho {
    private String MaKiemKe;
    private LocalDate NgayKiemKe;
    private String MaNV;
    private String MaLoHang;
    private String MaSP;
    private int SoLuongHeThong;
    private int SoLuongThucTe;
    private String LyDo;
    private String TrangThai;

    private KiemKeKho(ThoXayKiemKeKho builder) {
        this.MaKiemKe = builder.maKiemKe;
        this.NgayKiemKe = builder.ngayKiemKe;
        this.MaNV = builder.maNV;
        this.MaLoHang = builder.maLoHang;
        this.MaSP = builder.maSP;
        this.SoLuongHeThong = builder.soLuongHeThong;
        this.SoLuongThucTe = builder.soLuongThucTe;
        this.LyDo = builder.lyDo;
        this.TrangThai = builder.trangThai;
    }

    public KiemKeKho() {}
    public String getMaKiemKe() { return MaKiemKe; }
    public void setMaKiemKe(String maKiemKe) { MaKiemKe = maKiemKe; }
    public LocalDate getNgayKiemKe() { return NgayKiemKe; }
    public void setNgayKiemKe(LocalDate ngayKiemKe) { NgayKiemKe = ngayKiemKe; }
    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }
    public String getMaLoHang() { return MaLoHang; }
    public void setMaLoHang(String maLoHang) { MaLoHang = maLoHang; }
    public String getMaSP() { return MaSP; }
    public void setMaSP(String maSP) { MaSP = maSP; }
    public int getSoLuongHeThong() { return SoLuongHeThong; }
    public void setSoLuongHeThong(int soLuongHeThong) { SoLuongHeThong = soLuongHeThong; }
    public int getSoLuongThucTe() { return SoLuongThucTe; }
    public void setSoLuongThucTe(int soLuongThucTe) { SoLuongThucTe = soLuongThucTe; }
    public String getLyDo() { return LyDo; }
    public void setLyDo(String lyDo) { LyDo = lyDo; }
    public String getTrangThai() { return TrangThai; }
    public void setTrangThai(String trangThai) { TrangThai = trangThai; }

    public static class ThoXayKiemKeKho {
        private String maKiemKe;
        private LocalDate ngayKiemKe;
        private String maNV;
        private String maLoHang;
        private String maSP;
        private int soLuongHeThong;
        private int soLuongThucTe;
        private String lyDo;
        private String trangThai;

        public ThoXayKiemKeKho ganMaKiemKe(String ma) { this.maKiemKe = ma; return this; }
        public ThoXayKiemKeKho ganNgayKiemKe(LocalDate ngay) { this.ngayKiemKe = ngay; return this; }
        public ThoXayKiemKeKho ganMaNV(String nv) { this.maNV = nv; return this; }
        public ThoXayKiemKeKho ganMaLoHang(String lo) { this.maLoHang = lo; return this; }
        public ThoXayKiemKeKho ganMaSP(String sp) { this.maSP = sp; return this; }
        public ThoXayKiemKeKho ganSoLuongHeThong(int sl) { this.soLuongHeThong = sl; return this; }
        public ThoXayKiemKeKho ganSoLuongThucTe(int sl) { this.soLuongThucTe = sl; return this; }
        public ThoXayKiemKeKho ganLyDo(String lyDo) { this.lyDo = lyDo; return this; }
        public ThoXayKiemKeKho ganTrangThai(String tt) { this.trangThai = tt; return this; }

        public KiemKeKho taoMoi() { return new KiemKeKho(this); }
    }
}