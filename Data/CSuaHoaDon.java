package Data;

import java.time.LocalDateTime;

public class CSuaHoaDon {
    private String MaTraHang;
    private String MaHD;
    private LocalDateTime NgayCs;
    private String TrangThaiCS;
    private String LyDoSua;
    private String MaNV;

    private CSuaHoaDon(ThoXayCSuaHoaDon builder) {
        this.MaTraHang = builder.maTraHang;
        this.MaHD = builder.maHD;
        this.NgayCs = builder.ngayCs;
        this.TrangThaiCS = builder.trangThaiCS;
        this.LyDoSua = builder.lyDoSua;
        this.MaNV = builder.maNV;
    }

    public CSuaHoaDon() {}
    public String getMaTraHang() { return MaTraHang; }
    public void setMaTraHang(String maTraHang) { MaTraHang = maTraHang; }
    public String getMaHD() { return MaHD; }
    public void setMaHD(String maHD) { MaHD = maHD; }
    public LocalDateTime getNgayCs() { return NgayCs; }
    public void setNgayCs(LocalDateTime ngayCs) { NgayCs = ngayCs; }
    public String getTrangThaiCS() { return TrangThaiCS; }
    public void setTrangThaiCS(String trangThaiCS) { TrangThaiCS = trangThaiCS; }
    public String getLyDoSua() { return LyDoSua; }
    public void setLyDoSua(String lyDoSua) { LyDoSua = lyDoSua; }
    public String getMaNV() { return MaNV; }
    public void setMaNV(String maNV) { MaNV = maNV; }

    public static class ThoXayCSuaHoaDon {
        private String maTraHang;
        private String maHD;
        private LocalDateTime ngayCs;
        private String trangThaiCS;
        private String lyDoSua;
        private String maNV;

        public ThoXayCSuaHoaDon ganMaTraHang(String maTraHang) { this.maTraHang = maTraHang; return this; }
        public ThoXayCSuaHoaDon ganMaHD(String maHD) { this.maHD = maHD; return this; }
        public ThoXayCSuaHoaDon ganNgayCs(LocalDateTime ngayCs) { this.ngayCs = ngayCs; return this; }
        public ThoXayCSuaHoaDon ganTrangThaiCS(String trangThaiCS) { this.trangThaiCS = trangThaiCS; return this; }
        public ThoXayCSuaHoaDon ganLyDoSua(String lyDoSua) { this.lyDoSua = lyDoSua; return this; }
        public ThoXayCSuaHoaDon ganMaNV(String maNV) { this.maNV = maNV; return this; }

        public CSuaHoaDon taoMoi() { return new CSuaHoaDon(this); }
    }
}