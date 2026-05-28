package Logic;

import Dao.BangLuongDAO;
import Dao.CauHinhLuongDAO;
import Dao.ChiaCaDAO;
import Data.BangLuong;
import Data.CauHinhLuong;
import Data.ChiaCa;
import Data.LoaiCa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class BangLuongLogic {
    public static class ChiTietKhauTru {
        public int soLanNghi = 0;
        public int soLanTre = 0; // Thêm đếm số lần trễ
        public long tongPhutTre = 0;
        public BigDecimal tongTienPhat = BigDecimal.ZERO;
    }
    private BangLuongDAO blDao = BangLuongDAO.getInstance();
    private CauHinhLuongDAO chlDao = CauHinhLuongDAO.getInstance();
    private ChiaCaDAO chiaCaDao = ChiaCaDAO.getInstance();

    // ==========================================================
    // 1. TÍNH TỔNG GIỜ LÀM TRONG THÁNG
    // ==========================================================
    public BigDecimal tinhTongGioLamTrongThang(String maNV, int thang, int nam) {
        // Tận dụng hàm tính phút có sẵn trong ChiaCaDAO của bạn
        double tongGio = chiaCaDao.tinhTongGioLamTrongThang(maNV, thang, nam);
        // Chuyển sang BigDecimal, làm tròn 2 chữ số thập phân
        return BigDecimal.valueOf(tongGio).setScale(2, RoundingMode.HALF_UP);
    }

    // ==========================================================
    // 2. LOGIC TÍNH KHẤU TRỪ (ĐI MUỘN, NGHỈ KHÔNG PHÉP)
    // ==========================================================
    public BigDecimal tinhKhauTruVaoMuonNghiLam(String maNV, int thang, int nam) throws Exception {
        BigDecimal tongKhauTru = BigDecimal.ZERO;
        
        // --- CẤU HÌNH MỨC PHẠT (Có thể thay đổi) ---
        BigDecimal phatNghiKhongPhep = new BigDecimal("200000"); // Phạt 200k / 1 ca nghỉ
        BigDecimal phatDiMuonTheoPhut = new BigDecimal("2000");  // Phạt 2k / 1 phút đi muộn
        int soPhutDuDi = 5; // Cho phép đi muộn 5 phút không phạt

        // Lấy danh sách ca làm trong tháng & danh sách cấu hình giờ của Loại ca
        List<ChiaCa> dsCaThang = chiaCaDao.layDanhSachChiaCaTheoThang(maNV, thang, nam);
        List<LoaiCa> dsLoaiCa = new LoaiCaLogic().layDanhSachLoaiCa(); 

        LocalDate homNay = LocalDate.now();

        for (ChiaCa cc : dsCaThang) {
            // Bỏ qua ca Đã hủy hoặc ca của tương lai chưa làm
            if ("Đã hủy".equalsIgnoreCase(cc.getTinhTrang()) || cc.getNgayLam().isAfter(homNay)) {
                continue; 
            }

            // A. LOGIC NGHỈ LÀM: Ca làm đã qua (hoặc là hôm nay) nhưng ThoiGianCheckIn = NULL
            if (cc.getThoiGianCheckIn() == null) {
                // Tránh việc ca tối nay chưa làm đã bị tính là nghỉ, ta check thời gian kết thúc ca
                LoaiCa lc = timLoaiCa(dsLoaiCa, cc.getMaLoaiCa());
                if (lc != null && (cc.getNgayLam().isBefore(homNay) || 
                   (cc.getNgayLam().isEqual(homNay) && LocalTime.now().isAfter(lc.getGioKetThuc())))) {
                    
                    tongKhauTru = tongKhauTru.add(phatNghiKhongPhep);
                    continue; // Đã phạt nghỉ thì thôi không xét đi muộn nữa
                }
            }

            // B. LOGIC ĐI MUỘN: Có Check-in, đem so sánh với GioBatDau của Loại ca
            if (cc.getThoiGianCheckIn() != null) {
                LoaiCa lc = timLoaiCa(dsLoaiCa, cc.getMaLoaiCa());
                if (lc != null) {
                    LocalTime gioBatDauQuyDinh = lc.getGioBatDau();
                    LocalTime gioCheckInThucTe = cc.getThoiGianCheckIn().toLocalTime();

                    if (gioCheckInThucTe.isAfter(gioBatDauQuyDinh)) {
                        long phutDiMuon = Duration.between(gioBatDauQuyDinh, gioCheckInThucTe).toMinutes();
                        
                        // Nếu số phút đi muộn lớn hơn thời gian du di thì mới phạt
                        if (phutDiMuon > soPhutDuDi) {
                            BigDecimal tienPhatCaNay = phatDiMuonTheoPhut.multiply(new BigDecimal(phutDiMuon));
                            tongKhauTru = tongKhauTru.add(tienPhatCaNay);
                        }
                    }
                }
            }
        }
        return tongKhauTru;
    }

    // Hàm helper nhỏ để tìm Loại Ca nhanh
    private LoaiCa timLoaiCa(List<LoaiCa> list, String maLoaiCa) {
        for (LoaiCa lc : list) {
            if (lc.getMaLoaiCa().equals(maLoaiCa)) return lc;
        }
        return null;
    }
    // Overload 6 tham số: gioLam và khauTru được truyền từ bên ngoài (dùng cho chốt thủ công)
    public void chotVaLuuBangLuong(String maNV, String thangNam, BigDecimal tongGioLamNgoai,
                                    BigDecimal gioTangCa, BigDecimal thuongThem, BigDecimal khauTruNgoai) throws Exception {
        if (maNV == null || thangNam == null || !thangNam.matches("\\d{2}/\\d{4}")) {
            throw new Exception("Thông tin Tháng/Năm hoặc Mã nhân viên không hợp lệ (Định dạng MM/yyyy)!");
        }
        if (blDao.kiemTraDaTinhLuong(maNV, thangNam)) {
            throw new Exception("Nhân viên " + maNV + " đã được chốt lương cho kỳ " + thangNam + " rồi!");
        }

        CauHinhLuong cauHinh = chlDao.layCauHinhHienTaiTheoMaNV(maNV);
        if (cauHinh == null) {
            throw new Exception("Chưa cài đặt cấu hình lương cho nhân viên này!");
        }

        BigDecimal tongGioLam    = (tongGioLamNgoai != null) ? tongGioLamNgoai : BigDecimal.ZERO;
        if (gioTangCa  == null) gioTangCa  = BigDecimal.ZERO;
        if (thuongThem == null) thuongThem = BigDecimal.ZERO;
        BigDecimal khauTruThuCong = (khauTruNgoai != null) ? khauTruNgoai : BigDecimal.ZERO;

        BigDecimal tienLuongCoBan = tongGioLam.multiply(cauHinh.getLuongTheoGio()).multiply(cauHinh.getHeSoLuong());
        BigDecimal tienOT         = gioTangCa.multiply(cauHinh.getLuongTheoGio()).multiply(cauHinh.getHeSoTangCa());

        BigDecimal tongLuong = tienLuongCoBan.add(tienOT)
                .add(cauHinh.getPhuCapCoDinh())
                .add(thuongThem)
                .subtract(khauTruThuCong);
        if (tongLuong.compareTo(BigDecimal.ZERO) < 0) tongLuong = BigDecimal.ZERO;

        BangLuong bl = new BangLuong.ThoXayBangLuong()
                .ganMaNV(maNV).ganThangNam(thangNam).ganTongGioLam(tongGioLam)
                .ganGioTangCa(gioTangCa).ganLuongTheoGio(cauHinh.getLuongTheoGio())
                .ganThuong(thuongThem).ganPhuCap(cauHinh.getPhuCapCoDinh())
                .ganKhauTru(khauTruThuCong).ganTongLuong(tongLuong).taoMoi();

        if (!blDao.luuBangLuong(bl)) throw new Exception("Lỗi hệ thống: Chốt lương thất bại!");
    }

    // ==========================================================
    // 3. HÀM CHỐT LƯƠNG TỰ ĐỘNG (Đã tích hợp 2 hàm trên)
    // ==========================================================
    public void chotVaLuuBangLuong(String maNV, String thangNam, BigDecimal gioTangCa, BigDecimal thuongThem) throws Exception {
        
        // 1. Validate cơ bản
        if (maNV == null || thangNam == null || !thangNam.matches("\\d{2}/\\d{4}")) {
            throw new Exception("Thông tin Tháng/Năm hoặc Mã nhân viên không hợp lệ (Định dạng MM/yyyy)!");
        }
        if (blDao.kiemTraDaTinhLuong(maNV, thangNam)) {
            throw new Exception("Nhân viên " + maNV + " đã được chốt lương cho kỳ " + thangNam + " rồi!");
        }

        // Tách tháng và năm từ chuỗi "MM/yyyy"
        int thang = Integer.parseInt(thangNam.split("/")[0]);
        int nam = Integer.parseInt(thangNam.split("/")[1]);

        // 2. Lấy cấu hình lương ĐANG ÁP DỤNG của nhân viên
        CauHinhLuong cauHinh = chlDao.layCauHinhHienTaiTheoMaNV(maNV);
        if (cauHinh == null) {
            throw new Exception("Chưa cài đặt cấu hình lương cho nhân viên này! Vui lòng cài đặt trước khi chốt lương.");
        }

        // 3. 🚀 TỰ ĐỘNG TÍNH TOÁN: Lấy tổng giờ và tiền phạt hệ thống tự quét
        BigDecimal tongGioLam = tinhTongGioLamTrongThang(maNV, thang, nam);
        ChiTietKhauTru chiTietKhauTru = tinhChiTietKhauTru(maNV, thang, nam);
        BigDecimal khauTruThuCong = chiTietKhauTru.tongTienPhat;

        // Tránh NullPointer
        if (gioTangCa == null) gioTangCa = BigDecimal.ZERO;
        if (thuongThem == null) thuongThem = BigDecimal.ZERO;

        // 4. THỰC HIỆN TOÁN TÍNH LƯƠNG (Chính xác tới từng xu)
        BigDecimal tienLuongCoBan = tongGioLam.multiply(cauHinh.getLuongTheoGio()).multiply(cauHinh.getHeSoLuong());
        BigDecimal tienOT = gioTangCa.multiply(cauHinh.getLuongTheoGio()).multiply(cauHinh.getHeSoTangCa());

        // Tổng lương = Lương CB + OT + Phụ Cấp (cố định) + Thưởng (phát sinh) - Khấu trừ
        BigDecimal tongLuong = tienLuongCoBan
                .add(tienOT)
                .add(cauHinh.getPhuCapCoDinh())
                .add(thuongThem)
                .subtract(khauTruThuCong);

        if (tongLuong.compareTo(BigDecimal.ZERO) < 0) {
            tongLuong = BigDecimal.ZERO; // Lương không thể âm
        }

        // 5. Build đối tượng BangLuong và lưu xuống DataBase
        BangLuong bl = new BangLuong.ThoXayBangLuong()
                .ganMaNV(maNV)
                .ganThangNam(thangNam)
                .ganTongGioLam(tongGioLam)
                .ganGioTangCa(gioTangCa)
                .ganLuongTheoGio(cauHinh.getLuongTheoGio()) // Lưu cứng mức lương lúc trả
                .ganThuong(thuongThem)
                .ganPhuCap(cauHinh.getPhuCapCoDinh())
                .ganKhauTru(khauTruThuCong)
                .ganTongLuong(tongLuong)
                .taoMoi();

        boolean thanhCong = blDao.luuBangLuong(bl);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Chốt lương thất bại!");
        }
    }
    public ChiTietKhauTru tinhChiTietKhauTru(String maNV, int thang, int nam) throws Exception {
        ChiTietKhauTru chiTiet = new ChiTietKhauTru();
        
        // --- CẤU HÌNH MỨC PHẠT TƯ BẢN ---
        BigDecimal phatNghiKhongPhep = new BigDecimal("200000"); // Chốt: Phạt 200k / 1 ca nghỉ

        List<ChiaCa> dsCaThang = chiaCaDao.layDanhSachChiaCaTheoThang(maNV, thang, nam);
        List<LoaiCa> dsLoaiCa = Dao.LoaiCaDAO.getInstance().layDanhSachLoaiCa(); 

        LocalDate homNay = LocalDate.now();

        for (ChiaCa cc : dsCaThang) {
            if ("Đã hủy".equalsIgnoreCase(cc.getTinhTrang()) || cc.getNgayLam().isAfter(homNay)) {
                continue; 
            }

            // A. NGHỈ LÀM (AUTO TRỪ 200K)
            if (cc.getThoiGianCheckIn() == null) {
                LoaiCa lc = timLoaiCa(dsLoaiCa, cc.getMaLoaiCa());
                if (lc != null && (cc.getNgayLam().isBefore(homNay) || 
                   (cc.getNgayLam().isEqual(homNay) && LocalTime.now().isAfter(lc.getGioKetThuc())))) {
                    
                    chiTiet.soLanNghi++;
                    chiTiet.tongTienPhat = chiTiet.tongTienPhat.add(phatNghiKhongPhep);
                    continue; 
                }
            }

            // B. ĐI MUỘN TÍNH THEO BẬC THANG
            if (cc.getThoiGianCheckIn() != null) {
                LoaiCa lc = timLoaiCa(dsLoaiCa, cc.getMaLoaiCa());
                if (lc != null) {
                    LocalTime gioBatDauQuyDinh = lc.getGioBatDau();
                    LocalTime gioCheckInThucTe = cc.getThoiGianCheckIn().toLocalTime();

                    if (gioCheckInThucTe.isAfter(gioBatDauQuyDinh)) {
                        long phutDiMuon = Duration.between(gioBatDauQuyDinh, gioCheckInThucTe).toMinutes();
                        
                        // Nếu muộn trên 5 phút bắt đầu phạt
                        if (phutDiMuon > 5) {
                            chiTiet.soLanTre++;
                            chiTiet.tongPhutTre += phutDiMuon;
                            
                            BigDecimal tienPhatCaNay = BigDecimal.ZERO;
                            
                            if (phutDiMuon <= 15) {
                                tienPhatCaNay = new BigDecimal("20000"); // 5-15p: 20k
                            } else if (phutDiMuon <= 30) {
                                tienPhatCaNay = new BigDecimal("50000"); // 16-30p: 50k
                            } else {
                                tienPhatCaNay = new BigDecimal("100000"); // Trên 30p: 100k
                            }
                            
                            chiTiet.tongTienPhat = chiTiet.tongTienPhat.add(tienPhatCaNay);
                        }
                    }
                }
            }
        }
        return chiTiet;
    }
}
