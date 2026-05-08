package Data;

public class NhanVienViewModel extends NhanVien {
    private String trangThaiLamViec; // "Đang làm việc" hoặc "Không trong ca"

    public String getTrangThaiLamViec() {
        return trangThaiLamViec;
    }

    public void setTrangThaiLamViec(String trangThaiLamViec) {
        this.trangThaiLamViec = trangThaiLamViec;
    }
}