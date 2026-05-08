package Data;

import java.time.LocalDate;
import java.math.BigDecimal;

public class LoHang {
    private String MaLoHang;
    private String MaNCC;
    private LocalDate NgayNhapKho;
    private BigDecimal ThanhTien;

    private LoHang(ThoXayLoHang builder) {
        this.MaLoHang = builder.maLoHang;
        this.MaNCC = builder.maNCC;
        this.NgayNhapKho = builder.ngayNhapKho;
        this.ThanhTien = builder.thanhTien;
    }

    public LoHang() {}
    public String getMaLoHang() { return MaLoHang; }
    public void setMaLoHang(String maLoHang) { MaLoHang = maLoHang; }
    public String getMaNCC() { return MaNCC; }
    public void setMaNCC(String maNCC) { MaNCC = maNCC; }
    public LocalDate getNgayNhapKho() { return NgayNhapKho; }
    public void setNgayNhapKho(LocalDate ngayNhapKho) { NgayNhapKho = ngayNhapKho; }
    public BigDecimal getThanhTien() { return ThanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { ThanhTien = thanhTien; }

    public static class ThoXayLoHang {
        private String maLoHang;
        private String maNCC;
        private LocalDate ngayNhapKho;
        private BigDecimal thanhTien;

        public ThoXayLoHang ganMaLoHang(String maLoHang) { this.maLoHang = maLoHang; return this; }
        public ThoXayLoHang ganMaNCC(String maNCC) { this.maNCC = maNCC; return this; }
        public ThoXayLoHang ganNgayNhapKho(LocalDate ngayNhapKho) { this.ngayNhapKho = ngayNhapKho; return this; }
        public ThoXayLoHang ganThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; return this; }

        public LoHang taoMoi() { return new LoHang(this); }
    }
}