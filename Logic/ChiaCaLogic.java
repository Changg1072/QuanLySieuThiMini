package Logic;

import Dao.ChiaCaDAO;
import Data.ChiaCa;
import Data.LoaiCa;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public class ChiaCaLogic {
	private ChiaCaDAO dao = ChiaCaDAO.getInstance();
	public List<ChiaCa> layDanhSachChiaCa() {
		return dao.layDanhSachChiaCa();
	}
	public void themChiaCa(ChiaCa cc) throws Exception {
        kiemTraLoiCoBan(cc);
        
        if (cc.getTienDauCa() != null && cc.getTienDauCa().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lỗi: Số tiền đầu ca cung cấp không được là số âm!");
        }
        
        try {
            boolean thanhCong = dao.themChiaCa(cc);
            if (!thanhCong) {
                throw new Exception("Lỗi hệ thống: Không có dữ liệu nào được ghi vào Database!");
            }
        } catch (java.sql.SQLException e) {
            // BÍ QUYẾT TÌM LỖI CHÍNH LÀ ĐOẠN NÀY:
            throw new Exception("Lỗi Database: " + e.getMessage());
        }
    }
	public void suaChiaCa(ChiaCa cc) throws Exception {
        if (cc.getMaCa() == null || cc.getMaCa().trim().isEmpty()) {
            throw new Exception("Mã ca làm việc không được để trống!");
        }
        // KHÔNG check ngày khi sửa - chỉ check khi thêm mới
        boolean thanhCong = dao.suaChiaCa(cc);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Cập nhật thông tin ca làm thất bại!");
        }
    }
	public void xoaChiaCa(String maCa, String tinhTrangHienTai) throws Exception {
        if (maCa == null || maCa.trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Ca làm việc cần xóa!");
        }
        if ("Đang làm việc".equals(tinhTrangHienTai) || "Đã hoàn thành".equals(tinhTrangHienTai)) {
            throw new Exception("Lỗi Nghiệp vụ: Ca này đang hoạt động hoặc đã hoàn thành, KHÔNG ĐƯỢC PHÉP XÓA để bảo vệ lịch sử đối soát!");
        }
        
        boolean thanhCong = dao.xoaChiaCa(maCa);
        if (!thanhCong) {
            throw new Exception("Lỗi hệ thống: Xóa ca làm thất bại!");
        }
    }
	public void thucHienCheckIn(String maCa, LocalDateTime thoiGianCheckIn, String tinhTrangHienTai, BigDecimal tienDauCaThucTe) throws Exception {
        if (thoiGianCheckIn == null) throw new Exception("Lỗi: Không xác định được thời gian Check-in!");

        // Lấy ca gốc và cập nhật toàn bộ Object -> Rất an toàn, không sợ lỗi câu lệnh UPDATE
        ChiaCa caCanCheckIn = timChiaCaTheoMa(maCa);
        if (caCanCheckIn != null) {
            caCanCheckIn.setThoiGianCheckIn(thoiGianCheckIn);
            caCanCheckIn.setTienDauCa(tienDauCaThucTe);
            caCanCheckIn.setTinhTrang("Đang làm việc"); // 🔥 Chuyển trạng thái bắt đầu ca

            suaChiaCa(caCanCheckIn); // Đẩy xuống DB
        } else {
            throw new Exception("Không tìm thấy dữ liệu ca làm trong hệ thống!");
        }
    }
	private void kiemTraLoiCoBan(ChiaCa cc) throws Exception {
        if (cc.getMaCa() == null || cc.getMaCa().trim().isEmpty()) {
            throw new Exception("Mã ca làm việc không được để trống!");
        }
        if (cc.getMaLoaiCa() == null || cc.getMaLoaiCa().trim().isEmpty()) {
            throw new Exception("Vui lòng chọn Loại ca (Sáng/Chiều/Tối)!");
        }
        if (cc.getNgayLam() == null) {
            throw new Exception("Vui lòng chọn Ngày làm việc!");
        }
        // Có thể thêm luật: Không cho phép xếp ca ở quá khứ
        if (cc.getNgayLam().isBefore(LocalDate.now())) {
            throw new Exception("Lỗi: Không thể xếp lịch làm việc cho một ngày trong quá khứ!");
        }
    }
	public void thucHienCheckOut(String maCa, LocalDateTime thoiGianCheckOut, BigDecimal tienCuoiCa, BigDecimal tienBanHangTrongCa, BigDecimal tienDauCa) throws Exception {
        if (thoiGianCheckOut == null) {
            throw new Exception("Lỗi hệ thống: Không xác định được thời gian Check-out hiện tại!");
        }
        if (tienCuoiCa == null || tienCuoiCa.compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Lỗi: Số tiền trong két lúc giao ca không được là số âm!");
        }
        
        BigDecimal tienGoc = (tienDauCa != null) ? tienDauCa : BigDecimal.ZERO;
        BigDecimal doanhThu = (tienBanHangTrongCa != null) ? tienBanHangTrongCa : BigDecimal.ZERO;
        BigDecimal tongTienLyThuyet = tienGoc.add(doanhThu);
        BigDecimal tienChenhLech = tienCuoiCa.subtract(tongTienLyThuyet); // Tiền dư/thiếu thực tế
        
        // 1. Tìm thông tin của cái ca vừa bấm nút để lấy Ngày Làm và Loại Ca
        List<ChiaCa> dsCa = layDanhSachChiaCa();
        ChiaCa caGoc = null;
        for (ChiaCa cc : dsCa) {
            if (cc.getMaCa().equals(maCa)) {
                caGoc = cc; break;
            }
        }
        if (caGoc == null) throw new Exception("Không tìm thấy dữ liệu ca làm!");

        // 2. ĐỒNG BỘ: Cập nhật cho TẤT CẢ các thành viên trong cùng ca đó
        for (ChiaCa cc : dsCa) {
            if (cc.getNgayLam() != null && cc.getNgayLam().equals(caGoc.getNgayLam()) 
                && cc.getMaLoaiCa().equals(caGoc.getMaLoaiCa())
                && "Đang làm việc".equals(cc.getTinhTrang())) {
                
                // Cập nhật tất cả về Đã hoàn thành và set chung số tiền
                cc.setTinhTrang("Đã hoàn thành");
                cc.setThoiGianCheckOut(thoiGianCheckOut);
                cc.setTienDauCa(tienGoc);
                cc.setTienBanHang(doanhThu);
                cc.setTienCuoiCa(tienCuoiCa);
                cc.setTienChenhLech(tienChenhLech);
                
                dao.suaChiaCa(cc); // Lưu xuống DB
            }
        }
    }
    public ChiaCa timChiaCaTheoMa(String maCa) {
        if (maCa == null || maCa.isEmpty()) return null;
        
        List<ChiaCa> dsCa = layDanhSachChiaCa();
        for (ChiaCa cc : dsCa) {
            if (cc.getMaCa().equals(maCa)) {
                return cc;
            }
        }
        return null;
    }
    public String xuLyCheckInKhiDangNhap(String maNV) throws Exception {
        LocalDate homNay = LocalDate.now();
        LocalTime bayGio = LocalTime.now();
        
        // 1. Xác định hiện tại đang thuộc Ca nào (Sáng, Chiều hay Tối)
        LoaiCaLogic lcLogic = new LoaiCaLogic();
        List<LoaiCa> dsLoaiCa = lcLogic.layDanhSachLoaiCa();
        String maCaHienTai = null;
        boolean laCaSang = false;

        for (LoaiCa lc : dsLoaiCa) {
            // Kiểm tra xem giờ hiện tại có nằm trong khoảng giờ của Ca này không
            if (bayGio.isAfter(lc.getGioBatDau()) && bayGio.isBefore(lc.getGioKetThuc())) {
                maCaHienTai = lc.getMaLoaiCa();
                if (lc.getTenCa().toLowerCase().contains("sáng")) laCaSang = true;
                break;
            }
        }

        if (maCaHienTai == null) {
            return "Hệ thống: Hiện tại không nằm trong khung giờ làm việc nào.";
        }

        // 2. Tìm xem nhân viên này CÓ LỊCH làm trong ca này không
        List<ChiaCa> tatCaCaHnay = layDanhSachChiaCa();
        ChiaCa caCuaToi = null;
        int tongNguoiTrongCa = 0;
        int soNguoiDaCheckIn = 0;

        for (ChiaCa cc : tatCaCaHnay) {
            if (cc.getNgayLam() != null && cc.getNgayLam().equals(homNay) && cc.getMaLoaiCa().equals(maCaHienTai)) {
                // Ca này chưa bị hủy
                if (!"Đã hủy".equals(cc.getTinhTrang())) {
                    tongNguoiTrongCa++;
                    if (cc.getThoiGianCheckIn() != null) soNguoiDaCheckIn++;
                    if (cc.getMaNV().equals(maNV)) caCuaToi = cc; // Đây là ca của người đang đăng nhập
                }
            }
        }

        if (caCuaToi == null) {
            return "Bạn không có lịch làm việc trong ca hiện tại!";
        }

        // 3. Thực hiện Check-in nếu chưa check-in
        if (caCuaToi.getThoiGianCheckIn() == null) {
            // 🔥 NẾU LÀ CA SÁNG: Mặc định trong két có 500,000đ tiền thối
            BigDecimal tienDauCa = laCaSang ? new BigDecimal("500000") : BigDecimal.ZERO; 
            
            // Hàm capNhatCheckIn trong DAO của bạn ĐÃ CÓ SẴN logic đổi trạng thái thành "Đang làm việc"
            boolean checkInOK = dao.capNhatCheckIn(caCuaToi.getMaCa(), LocalDateTime.now(), tienDauCa);
            if (checkInOK) soNguoiDaCheckIn++; 
        }

        // 4. Kiểm tra xem đã đủ người làm chưa (Trả về thông báo cho UI)
        if (soNguoiDaCheckIn < tongNguoiTrongCa) {
            int thieu = tongNguoiTrongCa - soNguoiDaCheckIn;
            return "WARNING_THIEU_NGUOI:Ca làm hiện tại đang thiếu " + thieu + " người chưa đến!";
        }

        return "SUCCESS:Check-in thành công. Đã đủ nhân sự ca làm!";
    }
    // ========================================================
    // TỰ ĐỘNG CHECK-IN & KIỂM TRA ĐIỂM DANH (GỌI LÚC ĐĂNG NHẬP)
    // ========================================================
    public String kiemTraDiemDanhCaHienTai(String maNVDangNhap) {
        try {
            LocalDate homNay = LocalDate.now();
            java.time.LocalTime bayGio = java.time.LocalTime.now();
            
            LoaiCaLogic lcLogic = new LoaiCaLogic();
            List<LoaiCa> dsLoaiCa = lcLogic.layDanhSachLoaiCa();
            String maCaHienTai = null;
            boolean laCaSang = false;

            // 1. Xem bây giờ đang là ca nào
            for (LoaiCa lc : dsLoaiCa) {
                if (bayGio.isAfter(lc.getGioBatDau()) && bayGio.isBefore(lc.getGioKetThuc())) {
                    maCaHienTai = lc.getMaLoaiCa();
                    if (lc.getTenCa().toLowerCase().contains("sáng")) laCaSang = true;
                    break;
                }
            }

            if (maCaHienTai == null) return null; // Không trong ca nào thì thôi

            List<ChiaCa> tatCaCaHnay = layDanhSachChiaCa();
            List<String> danhSachThieu = new ArrayList<>();
            NhanVienLogic nvLogic = new NhanVienLogic();

            for (ChiaCa cc : tatCaCaHnay) {
                if (cc.getNgayLam() != null && cc.getNgayLam().equals(homNay) 
                    && cc.getMaLoaiCa().equals(maCaHienTai) 
                    && !"Đã hủy".equals(cc.getTinhTrang())) {
                    
                    // 2. Nếu là tài khoản đang đăng nhập -> Auto Check-in
                    if (cc.getMaNV().equals(maNVDangNhap)) {
                        if (cc.getThoiGianCheckIn() == null) {
                            BigDecimal tienDauCa = laCaSang ? new BigDecimal("500000") : BigDecimal.ZERO; 
                            thucHienCheckIn(cc.getMaCa(), LocalDateTime.now(), cc.getTinhTrang(), tienDauCa);
                        }
                    } 
                    // 3. Nếu là đồng nghiệp cùng ca mà CHƯA check-in -> Đưa vào danh sách đen
                    else if (cc.getThoiGianCheckIn() == null) {
                        Data.NhanVien nvThieu = nvLogic.timNhanVienTheoMa(cc.getMaNV());
                        if (nvThieu != null) {
                            danhSachThieu.add(nvThieu.getHoTen() + " (" + nvThieu.getMaNV() + ")");
                        }
                    }
                }
            }

            // 4. Nếu có người thiếu, trả về thông báo
            if (!danhSachThieu.isEmpty()) {
                String ds = String.join("<br>- ", danhSachThieu);
                return "Hiện đang thiếu các nhân viên:<br>- " + ds + "<br><br><i>Nhân viên đã có mặt chưa? Vui lòng nhắc nhở đăng nhập hệ thống!</i>";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Đủ người, không cần thông báo
    }
    // ========================================================
    // TÌM DANH SÁCH CHƯA CHECK-IN (CHỈ HIỆN NẾU NGƯỜI ĐĂNG NHẬP CÓ TRONG CA)
    // ========================================================
    public List<ChiaCa> layDanhSachChuaCheckInCaHienTai(String maNVDangNhap, boolean isAdmin) {
        List<ChiaCa> dsThieu = new ArrayList<>();
        try {
            LocalDate homNay = LocalDate.now();
            java.time.LocalTime bayGio = java.time.LocalTime.now();

            Logic.LoaiCaLogic lcLogic = new Logic.LoaiCaLogic();
            List<Data.LoaiCa> dsLoaiCa = lcLogic.layDanhSachLoaiCa();
            String maCaHienTai = null;
            boolean laCaSang = false;

            // ==========================================
            // 🔥 ĐÃ FIX LOGIC THỜI GIAN & CA QUA ĐÊM
            // ==========================================
            for (Data.LoaiCa lc : dsLoaiCa) {
                java.time.LocalTime batDau = lc.getGioBatDau();
                java.time.LocalTime ketThuc = lc.getGioKetThuc();
                
                // Cho phép mở điểm danh SỚM 60 PHÚT (Vd: 21:00 đã có thể điểm danh cho ca 22:00)
                java.time.LocalTime batDauChoPhep = batDau.minusMinutes(60); 

                boolean trongCa = false;
                if (batDauChoPhep.isBefore(ketThuc)) {
                    // Ca bình thường trong ngày (VD: 06:00 -> 14:00)
                    trongCa = !bayGio.isBefore(batDauChoPhep) && !bayGio.isAfter(ketThuc);
                } else { 
                    // Ca làm xuyên đêm qua ngày hôm sau (VD: 22:00 -> 06:00)
                    trongCa = !bayGio.isBefore(batDauChoPhep) || !bayGio.isAfter(ketThuc);
                }

                if (trongCa) {
                    maCaHienTai = lc.getMaLoaiCa();
                    if (lc.getTenCa().toLowerCase().contains("sáng")) laCaSang = true;
                    break;
                }
            }
            // ==========================================

            if (maCaHienTai == null) {
                System.out.println("DEBUG: Lúc " + bayGio + " chưa tới giờ mở bất kỳ ca làm nào!");
                return dsThieu; // Trả về list rỗng
            }

            List<ChiaCa> tatCaCaHnay = layDanhSachChiaCa();

            // Kiểm tra người đăng nhập có thuộc ca này không
            boolean isThuocCa = false;
            ChiaCa caCuaToi = null;
            for (ChiaCa cc : tatCaCaHnay) {
                if (cc.getNgayLam() != null && cc.getNgayLam().equals(homNay)
                    && cc.getMaLoaiCa().equals(maCaHienTai)
                    && !"Đã hủy".equals(cc.getTinhTrang())
                    && cc.getMaNV() != null && cc.getMaNV().equals(maNVDangNhap)) {
                    isThuocCa = true;
                    caCuaToi = cc;
                    break;
                }
            }

            // NV thường không thuộc ca -> ẩn hoàn toàn
            if (!isAdmin && !isThuocCa) return dsThieu;
            /* 
            // Nếu thuộc ca mà chưa check-in -> tự động check-in
            if (isThuocCa && caCuaToi != null && caCuaToi.getThoiGianCheckIn() == null) {
                try {
                    BigDecimal tienDau = laCaSang ? new BigDecimal("500000") : BigDecimal.ZERO;
                    thucHienCheckIn(caCuaToi.getMaCa(), LocalDateTime.now(), caCuaToi.getTinhTrang(), tienDau);
                    caCuaToi.setThoiGianCheckIn(LocalDateTime.now());
                } catch (Exception ex) {
                    System.err.println("Lỗi auto check-in: " + ex.getMessage());
                }
            }
            */

            // Lấy những người chưa check-in đưa vào danh sách nhắc nhở
            for (ChiaCa cc : tatCaCaHnay) {
                if (cc.getNgayLam() != null && cc.getNgayLam().equals(homNay)
                    && cc.getMaLoaiCa().equals(maCaHienTai)
                    && !"Đã hủy".equals(cc.getTinhTrang())
                    && cc.getThoiGianCheckIn() == null) {
                    dsThieu.add(cc);
                }
            }

        } catch (Exception e) { e.printStackTrace(); }
        return dsThieu;
    }
    public BigDecimal layTienDauCaChuyenGiao(java.time.LocalDate ngayLam, String maLoaiCaHienTai) {
        try {
            Logic.LoaiCaLogic lcLogic = new Logic.LoaiCaLogic();
            List<Data.LoaiCa> dsLoaiCa = lcLogic.layDanhSachLoaiCa();
            
            String tenCaHienTai = "";
            for (Data.LoaiCa lc : dsLoaiCa) {
                if (lc.getMaLoaiCa().equals(maLoaiCaHienTai)) {
                    tenCaHienTai = lc.getTenCa().toLowerCase();
                    break;
                }
            }

            // 1. Chỉ có ca Sáng mới được làm mới 500k
            if (tenCaHienTai.contains("sáng")) {
                return new BigDecimal("500000");
            }

            // 2. Ca chiều/tối lấy Tiền Kết Ca của ca liền trước
            String tenCaTruoc = tenCaHienTai.contains("chiều") ? "sáng" : "chiều";
            String maLoaiCaTruoc = "";
            for (Data.LoaiCa lc : dsLoaiCa) {
                if (lc.getTenCa().toLowerCase().contains(tenCaTruoc)) {
                    maLoaiCaTruoc = lc.getMaLoaiCa();
                    break;
                }
            }

            List<ChiaCa> dsCa = layDanhSachChiaCa();
            BigDecimal tienKethua = BigDecimal.ZERO;
            
            for (ChiaCa cc : dsCa) {
                if (cc.getNgayLam() != null && cc.getNgayLam().equals(ngayLam) 
                    && cc.getMaLoaiCa().equals(maLoaiCaTruoc)
                    && "Đã hoàn thành".equals(cc.getTinhTrang())) {
                    
                    // Lấy số tiền két cuối cùng mà ca trước bàn giao lại
                    if (cc.getTienCuoiCa() != null && cc.getTienCuoiCa().compareTo(BigDecimal.ZERO) > 0) {
                        tienKethua = cc.getTienCuoiCa(); 
                    }
                }
            }
            return tienKethua;
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }
    // ========================================================
    // ĐỒNG NGHIỆP XÁC NHẬN CÓ MẶT (FIX LƯU 100% VÀO SQL)
    // ========================================================
    public void xacNhanCoMatTucThi(String maCa) throws Exception {
        ChiaCa target = timChiaCaTheoMa(maCa);
        if(target == null) throw new Exception("Ca làm việc không tồn tại!");
        
        BigDecimal tienDau = layTienDauCaChuyenGiao(LocalDate.now(), target.getMaLoaiCa());
        
        // 🔥 Xuyên thủng tầng DAO, gọi trực tiếp SQL để đảm bảo lưu 100%
        String sql = "UPDATE ChiaCa SET ThoiGianCheckIn = ?, TinhTrang = N'Đang làm việc', TienDauCa = ? WHERE MaCa = ?";
        
        try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
             java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
             
            // Chuyển đổi giờ máy tính sang giờ SQL
            pst.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            pst.setBigDecimal(2, tienDau);
            pst.setString(3, maCa);
            
            int rows = pst.executeUpdate();
            if (rows == 0) {
                throw new Exception("Lỗi Database: Không lưu được giờ điểm danh!");
            }
        } catch (Exception e) {
            throw new Exception("Lỗi khi ghi giờ vào SQL: " + e.getMessage());
        }
    }
    // ========================================================
    // 🔥 LOGIC GIÁM SÁT TỔNG THỂ (CHO CẢ NHÂN VIÊN & ADMIN)
    // ========================================================
    public Map<String, Object> giamSatHeThongCaLam() {
        Map<String, Object> ketQua = new HashMap<>();
        try {
            LocalDate homNay = LocalDate.now();
            java.time.LocalTime bayGio = java.time.LocalTime.now();
            
            // 1. Xác định ca làm hiện tại dựa trên giờ hệ thống
            Logic.LoaiCaLogic lcLogic = new Logic.LoaiCaLogic();
            List<Data.LoaiCa> dsLoaiCa = lcLogic.layDanhSachLoaiCa();
            Data.LoaiCa caHienTai = null;

            for (Data.LoaiCa lc : dsLoaiCa) {
                if (bayGio.isAfter(lc.getGioBatDau()) && bayGio.isBefore(lc.getGioKetThuc())) {
                    caHienTai = lc;
                    break;
                }
            }

            if (caHienTai == null) return null; // Không phải giờ làm việc

            // 2. Lấy danh sách ca làm đã phân công trong khung giờ này
            List<ChiaCa> dsCa = layDanhSachChiaCa();
            List<ChiaCa> dsThieu = new ArrayList<>();
            int tongSoNguoiDuKien = 0;
            int soNguoiDaCoMat = 0;

            for (ChiaCa cc : dsCa) {
                if (cc.getNgayLam() != null && cc.getNgayLam().equals(homNay) 
                    && cc.getMaLoaiCa().equals(caHienTai.getMaLoaiCa()) 
                    && !"Đã hủy".equals(cc.getTinhTrang())) {
                    
                    tongSoNguoiDuKien++;
                    if (cc.getThoiGianCheckIn() != null) {
                        soNguoiDaCoMat++;
                    } else {
                        dsThieu.add(cc);
                    }
                }
            }

            // 3. Phân loại cảnh báo
            if (tongSoNguoiDuKien > 0) {
                if (soNguoiDaCoMat == 0) {
                    // CA LÀM ĐANG TRỐNG (Quá 15p mà chưa ai vào)
                    if (bayGio.isAfter(caHienTai.getGioBatDau().plusMinutes(15))) {
                        ketQua.put("LOAI", "CA_TRONG");
                        ketQua.put("TEN_CA", caHienTai.getTenCa());
                    }
                } else if (soNguoiDaCoMat < tongSoNguoiDuKien) {
                    // THIẾU NHÂN VIÊN (Điểm danh)
                    ketQua.put("LOAI", "THIEU_NGUOI");
                    ketQua.put("DANH_SACH", dsThieu);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return ketQua.isEmpty() ? null : ketQua;
    }
    // ========================================================
    // KIỂM TRA ĐIỀU KIỆN THANH TOÁN (Đã đồng bộ Logic Ca Qua Đêm)
    // ========================================================
    public boolean kiemTraNhanVienDangTrongCa(String maNV) {
        try {
            LocalDate homNay = LocalDate.now();
            LocalTime bayGio = LocalTime.now();
            
            // Tìm mã ca hiện tại dựa theo giờ thực tế
            Logic.LoaiCaLogic lcLogic = new Logic.LoaiCaLogic();
            List<Data.LoaiCa> dsLoaiCa = lcLogic.layDanhSachLoaiCa();
            String maCaHienTai = null;

            // ==========================================
            // 🔥 ĐÃ FIX: Đồng bộ logic thời gian với Check-in
            // ==========================================
            for (Data.LoaiCa lc : dsLoaiCa) {
                java.time.LocalTime batDau = lc.getGioBatDau();
                java.time.LocalTime ketThuc = lc.getGioKetThuc();
                
                // Trừ đi 60 phút để khớp với logic cho phép vào ca sớm
                java.time.LocalTime batDauChoPhep = batDau.minusMinutes(60); 

                boolean trongCa = false;
                if (batDauChoPhep.isBefore(ketThuc)) {
                    // Ca bình thường
                    trongCa = !bayGio.isBefore(batDauChoPhep) && !bayGio.isAfter(ketThuc);
                } else { 
                    // Ca qua đêm
                    trongCa = !bayGio.isBefore(batDauChoPhep) || !bayGio.isAfter(ketThuc);
                }

                if (trongCa) {
                    maCaHienTai = lc.getMaLoaiCa();
                    break;
                }
            }

            // Nếu không quét được mã ca nào khớp giờ thì báo lỗi
            if (maCaHienTai == null) return false;

            // Kiểm tra nhân viên này có lịch hôm nay và đang ở trạng thái Đang làm việc không
            List<ChiaCa> dsCa = layDanhSachChiaCa();
            for (ChiaCa cc : dsCa) {
                if (cc.getNgayLam() != null && cc.getNgayLam().equals(homNay) 
                    && cc.getMaLoaiCa().equals(maCaHienTai)
                    && cc.getMaNV().equals(maNV)
                    && "Đang làm việc".equals(cc.getTinhTrang())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ========================================================
    // CỘNG DỒN TIỀN LIÊN TỤC VÀO CA LÀM
    // ========================================================
    public void capNhatTienGiaoDichRealtime(String maNV, BigDecimal tienBanHangThem, BigDecimal tienChenhLechThem) {
        // Dùng SQL trực tiếp để đảm bảo lưu 100%, không qua DAO trung gian
        String sqlLayCa = "SELECT MaCa, TienBanHang, TienChenhLech FROM ChiaCa " +
                        "WHERE MaNV = ? AND NgayLam = ? AND TinhTrang = N'Đang làm việc'";
        
        String sqlCapNhat = "UPDATE ChiaCa SET TienBanHang = ?, TienChenhLech = ? WHERE MaCa = ?";
        
        try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection()) {
            
            // Bước 1: Lấy ca đang làm + số tiền hiện tại
            String maCa = null;
            BigDecimal banHangCu = BigDecimal.ZERO;
            BigDecimal chenhLechCu = BigDecimal.ZERO;
            
            try (java.sql.PreparedStatement pst = con.prepareStatement(sqlLayCa)) {
                pst.setString(1, maNV);
                pst.setDate(2, java.sql.Date.valueOf(java.time.LocalDate.now()));
                java.sql.ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    maCa = rs.getString("MaCa");
                    java.math.BigDecimal bd1 = rs.getBigDecimal("TienBanHang");
                    java.math.BigDecimal bd2 = rs.getBigDecimal("TienChenhLech");
                    banHangCu  = (bd1 != null) ? bd1 : BigDecimal.ZERO;
                    chenhLechCu = (bd2 != null) ? bd2 : BigDecimal.ZERO;
                }
            }
            
            if (maCa == null) {
                System.err.println("[capNhatTienGiaoDichRealtime] Không tìm thấy ca đang làm cho NV: " + maNV);
                return;
            }
            
            // Bước 2: Cộng dồn rồi UPDATE thẳng vào DB
            BigDecimal banHangMoi  = banHangCu.add(tienBanHangThem);
            BigDecimal chenhLechMoi = chenhLechCu.add(tienChenhLechThem);
            
            try (java.sql.PreparedStatement pst = con.prepareStatement(sqlCapNhat)) {
                pst.setBigDecimal(1, banHangMoi);
                pst.setBigDecimal(2, chenhLechMoi);
                pst.setString(3, maCa);
                int rows = pst.executeUpdate();
                System.out.println("[capNhatTienGiaoDichRealtime] Đã cập nhật " + rows + 
                                " dòng | MaCa=" + maCa + 
                                " | TienBanHang=" + banHangMoi + 
                                " | TienChenhLech=" + chenhLechMoi);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ========================================================
    // TỰ ĐỘNG QUÉT VÀ KẾT CA QUÁ GIỜ (Gọi lúc khởi động/đăng nhập)
    // ========================================================
    public void kiemTraVaTuDongKetCa() {
        try {
            LocalDateTime thoiDiemCheck = LocalDateTime.now();
            List<ChiaCa> dsCa = layDanhSachChiaCa();
            LoaiCaLogic lcLogic = new LoaiCaLogic();
            List<LoaiCa> dsLoaiCa = lcLogic.layDanhSachLoaiCa();

            for (ChiaCa cc : dsCa) {
                if ("Đang làm việc".equals(cc.getTinhTrang())) {
                    LoaiCa loaiCa = dsLoaiCa.stream()
                        .filter(lc -> lc.getMaLoaiCa().equals(cc.getMaLoaiCa()))
                        .findFirst().orElse(null);

                    if (loaiCa != null && cc.getNgayLam() != null) {
                        LocalDateTime expectedEndTime = LocalDateTime.of(cc.getNgayLam(), loaiCa.getGioKetThuc());
                        
                        // Xử lý ca qua đêm
                        if (loaiCa.getGioKetThuc().isBefore(loaiCa.getGioBatDau())) {
                            expectedEndTime = expectedEndTime.plusDays(1);
                        }

                        // Nếu quá giờ kết thúc + 1 tiếng -> Tự động kết ca
                        if (thoiDiemCheck.isAfter(expectedEndTime.plusHours(1))) {
                            // Ghi giờ kết ca đúng bằng giờ endTime theo yêu cầu
                            xuLyNghiepVuKetCa(cc, expectedEndTime);
                            System.out.println("[LOG - AUTO] Tự động kết ca: " + cc.getMaCa());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi auto kết ca: " + e.getMessage());
        }
    }

    // ========================================================
    // HÀM XỬ LÝ CHUNG: DÀNH CHO CẢ NÚT BẤM VÀ AUTO
    // ========================================================
    public void xuLyNghiepVuKetCa(ChiaCa caCanKet, LocalDateTime thoiGianOut) throws Exception {
        if (caCanKet == null) throw new Exception("Dữ liệu ca làm không hợp lệ!");

        BigDecimal tienBanHang = BigDecimal.ZERO;
        BigDecimal tienChenhLech = BigDecimal.ZERO;
        
        String sqlLuuTru = "SELECT TienBanHang, TienChenhLech FROM ChiaCa WHERE MaCa = ?";
        try (java.sql.Connection con = Dao.ConnectDB.getInstance().getConnection();
             java.sql.PreparedStatement pst = con.prepareStatement(sqlLuuTru)) {
            pst.setString(1, caCanKet.getMaCa());
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    tienBanHang = rs.getBigDecimal("TienBanHang") != null ? rs.getBigDecimal("TienBanHang") : BigDecimal.ZERO;
                    tienChenhLech = rs.getBigDecimal("TienChenhLech") != null ? rs.getBigDecimal("TienChenhLech") : BigDecimal.ZERO;
                }
            }
        }

        BigDecimal tienDau = caCanKet.getTienDauCa() != null ? caCanKet.getTienDauCa() : BigDecimal.ZERO;
        BigDecimal tienCuoiCa = tienDau.add(tienBanHang).add(tienChenhLech);

        // Gộp chung 1 trạng thái duy nhất
        // String trangThaiMoi = "Đã kết ca";

        List<ChiaCa> allCa = layDanhSachChiaCa();
        for (ChiaCa cc : allCa) {
            if (cc.getNgayLam() != null && cc.getNgayLam().equals(caCanKet.getNgayLam())
                && cc.getMaLoaiCa().equals(caCanKet.getMaLoaiCa())
                && "Đang làm việc".equals(cc.getTinhTrang())) {
                
                // CHỈ DÙNG HÀM NÀY ĐỂ KẾT CA:
                boolean success = dao.capNhatCheckOut(cc.getMaCa(), thoiGianOut, tienCuoiCa, tienBanHang, tienChenhLech, "Đã hoàn thành");
                if (!success) throw new Exception("Lỗi commit DB!");
            }
        }
    }
}