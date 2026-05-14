package Logic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import Dao.ConnectDB;

public class TaoMaTuDongLogic {

    private static String getNextId(String tenBang, String tenCot, String tienTo, int soChuSo) {
        String maMoi = "";
        String sql = "SELECT MAX(" + tenCot + ") FROM " + tenBang + " WHERE " + tenCot + " LIKE ?";
        
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, tienTo + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) {
                    String maCuMax = rs.getString(1);
                    String phanSoCu = maCuMax.substring(tienTo.length());
                    int soTiepTheo = Integer.parseInt(phanSoCu) + 1;
                    
                    maMoi = tienTo + String.format("%0" + soChuSo + "d", soTiepTheo);
                } else {
                    maMoi = tienTo + String.format("%0" + soChuSo + "d", 1);
                }
            }
        } catch (Exception e) {
            System.err.println("[TaoMaTuDongLogic] Lỗi tại bảng " + tenBang + ": " + e.getMessage());
        }
        return maMoi;
    }

    // --- MÃ CỘNG DỒN ĐƠN GIẢN ---
    public static String taoMaNhanVien() {
        return getNextId("NhanVien", "MaNV", "NV", 3);
    }

    public static String taoMaKhachHang() {
        return getNextId("KhachHang", "MaKH", "KH", 3);
    }

    public static String taoMaNhaCungCap() {
        return getNextId("NhaCungCap", "MaNCC", "NCC", 3);
    }

    // ĐÃ SỬA: SP cho Sản Phẩm, tránh nhầm với LH của Lô Hàng
    public static String taoMaSanPham() {
        return getNextId("SanPham", "MaSP", "SP", 3);
    }

    // --- MÃ THEO NGÀY (CÓ DẤU GẠCH NGANG) ---

    public static String taoMaHoaDon() {
        String ngay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return getNextId("HoaDon", "MaHD", "HD" + ngay + "-", 3);
    }

    // CHUẨN: LH cho Lô Hàng
    public static String taoMaLoHang() {
        String ngay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return getNextId("LoHang", "MaLoHang", "LH" + ngay + "-", 3);
    }

    public static String taoMaTraHang() {
        String ngay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return getNextId("CSuaHoaDon", "MaTraHang", "TH" + ngay + "-", 3);
    }
 // --- MÃ KIỂM KÊ KHO ---
    public static String taoMaKiemKe() {
        String ngay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return getNextId("KiemKeKho", "MaKiemKe", "KK" + ngay + "-", 3);
    }

    // --- MÃ GIẢM GIÁ ---
    public static String taoMaGiamGia() {
        return getNextId("GiamGia", "MaGiamGia", "GG", 3);
    }
    public static String taoMaCa() {
        String ngay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return getNextId("ChiaCa", "MaCa", "CA" + ngay + "-", 3);
    }
    public static String taoMaPhieuTieuHuy() {
        String ngay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return getNextId("PhieuTieuHuy", "MaPhieuHuy", "PTH" + ngay + "-", 3);
    }
}