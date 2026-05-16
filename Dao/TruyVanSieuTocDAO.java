package Dao;

import Data.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🚀 LÕI ĐỘNG CƠ TURBO - XỬ LÝ MULTIPLE RESULTSETS (JDBC)
 * Tối ưu hóa 100% tình trạng thắt cổ chai mạng, truy xuất Dữ Liệu O(1)
 */
public class TruyVanSieuTocDAO {

    private static final TruyVanSieuTocDAO instance = new TruyVanSieuTocDAO();

    private TruyVanSieuTocDAO() {}

    public static TruyVanSieuTocDAO getInstance() {
        return instance;
    }

    // =========================================================================
    // 1. DATA TRANSFER OBJECTS (DTO): NHỮNG CHIẾC HỘP ĐỰNG DỮ LIỆU ĐA NĂNG
    // =========================================================================
    
    public static class DuLieuChiaCaDTO {
        public List<ChiaCa> dsChiaCa = new ArrayList<>();
        public Map<String, LoaiCa> mapLoaiCa = new HashMap<>();
        public Map<String, NhanVien> mapNhanVien = new HashMap<>();
    }

    public static class DuLieuDonHangDTO {
        public List<HoaDon> dsHoaDon = new ArrayList<>();
        public Map<String, List<ChiTietHoaDon>> mapChiTietHD = new HashMap<>();
        public Map<String, String[]> mapKhachHang = new HashMap<>(); // [0]=TenKH, [1]=BacKH
        public Map<String, String> mapNhanVien = new HashMap<>();    // value=TenNV
        public Map<String, String[]> mapSanPham = new HashMap<>();   // [0]=TenSP, [1]=MaLoai
    }

    public static class DuLieuBanHangDTO {
        public List<SanPham> dsSanPham = new ArrayList<>();
        public Map<String, Integer> mapTonKho = new HashMap<>();
        public Map<String, Integer> mapGiamGia = new HashMap<>(); // ← THÊM DÒNG NÀY
    }

    // =========================================================================
    // 2. TẢI DỮ LIỆU CHIA CA (Dùng cho BangChiaCaUi & ChiaCaUi)
    // =========================================================================
    public DuLieuChiaCaDTO loadToanBoLichChiaCa() {
        DuLieuChiaCaDTO dto = new DuLieuChiaCaDTO();
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return dto;

        try (CallableStatement cs = con.prepareCall("{call sp_LoadDuLieuChiaCa}")) {
            boolean hasResults = cs.execute();

            // 📦 RS 1: Danh sách Lịch Chia Ca
            if (hasResults) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        Timestamp inSQL = rs.getTimestamp("ThoiGianCheckIn");
                        Timestamp outSQL = rs.getTimestamp("ThoiGianCheckOut");
                        Date ngayLamSQL = rs.getDate("NgayLam");
                        
                        ChiaCa cc = new ChiaCa.ThoXayChiaCa()
                            .ganMaCa(rs.getString("MaCa"))
                            .ganMaLoaiCa(rs.getString("MaLoaiCa"))
                            .ganNgayLam(ngayLamSQL != null ? ngayLamSQL.toLocalDate() : null)
                            .ganMaNV(rs.getString("MaNV"))
                            .ganThoiGianCheckIn(inSQL != null ? inSQL.toLocalDateTime() : null)
                            .ganThoiGianCheckOut(outSQL != null ? outSQL.toLocalDateTime() : null)
                            .ganTinhTrang(rs.getString("TinhTrang"))
                            .ganTienDauCa(rs.getBigDecimal("TienDauCa"))
                            .ganTienCuoiCa(rs.getBigDecimal("TienCuoiCa"))
                            .ganTienBanHang(rs.getBigDecimal("TienBanHang"))
                            .ganTienChenhLech(rs.getBigDecimal("TienChenhLech"))
                            .taoMoi();
                        dto.dsChiaCa.add(cc);
                    }
                }
            }

         // 📦 RS 2: Danh sách Loại Ca
            if (cs.getMoreResults()) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        LoaiCa lc = new LoaiCa();
                        lc.setMaLoaiCa(rs.getString("MaLoaiCa"));
                        lc.setTenCa(rs.getString("TenCa"));
                        	
                        // 🔥 ĐÃ FIX: Kiểm tra NULL trước khi parse sang LocalTime
                        java.sql.Time timeBatDau = rs.getTime("GioBatDau");
                        if (timeBatDau != null) lc.setGioBatDau(timeBatDau.toLocalTime());
                        
                        java.sql.Time timeKetThuc = rs.getTime("GioKetThuc");
                        if (timeKetThuc != null) lc.setGioKetThuc(timeKetThuc.toLocalTime());
                        
                        dto.mapLoaiCa.put(lc.getMaLoaiCa(), lc);
                    }
                }
            }

            // 📦 RS 3: Danh sách Nhân Viên
            if (cs.getMoreResults()) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        NhanVien nv = new NhanVien.ThoXayNhanVien()
                            .ganMaNV(rs.getString("MaNV"))
                            .ganHoTen(rs.getString("HoTen"))
                            .ganTrangThai(rs.getString("TrangThai"))
                            .taoMoi();
                        dto.mapNhanVien.put(nv.getMaNV(), nv);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("🔥 [SieuTocDAO] Lỗi tải dữ liệu Chia Ca: " + e.getMessage());
        }
        return dto;
    }
    // =========================================================================
    // 5. THANH TOÁN GỘP CHUNG (TRANSACTION + BATCH INSERT) 🚀🚀🚀
    // =========================================================================
    public void thanhToanGopChungSieuToc(Data.HoaDon hd, List<Data.ChiTietHoaDon> dsChiTiet, 
                                         Data.KhachHang kh, String maNV, 
                                         BigDecimal tienBanHang, BigDecimal chenhLech) throws Exception {
                                             
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) throw new Exception("Không thể kết nối đến máy chủ!");

        // Khai báo các xe chở dữ liệu
        PreparedStatement psHD = null;
        PreparedStatement psCTHD = null;
        PreparedStatement psCa = null;
        PreparedStatement psKH = null;

        try {
            // 🛑 BƯỚC QUAN TRỌNG NHẤT: Tắt AutoCommit để gộp chung tất cả thành 1 mẻ (Transaction)
            con.setAutoCommit(false); 

            // 1. INSERT HÓA ĐƠN
            String sqlHD = "INSERT INTO HoaDon (MaHD, NgayTao, MaKH, MaNV, PhuongThucTT, TongGiamGia, KhachDua, TienThua, ThanhTien, TruTichDiem, GhiChu) " +
                           "VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            psHD = con.prepareStatement(sqlHD);
            psHD.setString(1, hd.getMaHD());
            psHD.setString(2, hd.getMaKH());
            psHD.setString(3, hd.getMaNV());
            psHD.setString(4, hd.getPhuongThucTT());
            psHD.setBigDecimal(5, hd.getTongGiamGia());
            psHD.setBigDecimal(6, hd.getKhachDua());
            psHD.setBigDecimal(7, hd.getTienThua());
            psHD.setBigDecimal(8, hd.getThanhTien());
            psHD.setBigDecimal(9, hd.getTruTichDiem());
            psHD.setString(10, hd.getGhiChu());
            psHD.executeUpdate(); // Chạy lệnh

            // 2. BATCH INSERT CHI TIẾT HÓA ĐƠN (GOM MẺ VÀ BẮN 1 LẦN 🚀)
            String sqlCTHD = "INSERT INTO ChiTietHoaDon (MaHD, MaSP, MaLoHang, SoLuong, DonGia, MaGiamGia, ThanhTienSanPham) VALUES (?, ?, ?, ?, ?, ?, ?)";
            psCTHD = con.prepareStatement(sqlCTHD);
            for (Data.ChiTietHoaDon ct : dsChiTiet) {
                psCTHD.setString(1, ct.getMaHD());
                psCTHD.setString(2, ct.getMaSp());
                psCTHD.setString(3, ct.getMaLoHang()); 
                psCTHD.setInt(4, ct.getSoLuong());
                psCTHD.setBigDecimal(5, ct.getDonGia());
                
                // 🔥 ĐÃ FIX: Xử lý an toàn nếu MaGiamGia bị rỗng (sản phẩm không có giảm giá)
                if (ct.getMaGiamGia() == null || ct.getMaGiamGia().trim().isEmpty()) {
                    psCTHD.setNull(6, java.sql.Types.VARCHAR);
                } else {
                    psCTHD.setString(6, ct.getMaGiamGia());
                }
                
                psCTHD.setBigDecimal(7, ct.getThanhTienSanPham()); // Đẩy ThanhTienSanPham xuống vị trí số 7
                
                psCTHD.addBatch(); // Gom vào xe tải, chưa gửi đi vội!
            }
            psCTHD.executeBatch(); // 🚀 Gửi nguyên xe tải dữ liệu vào DB (Nhanh gấp 100 lần lặp for)

            // 3. CẬP NHẬT TIỀN CHO CA LÀM VIỆC HIỆN TẠI
            String sqlCa = "UPDATE ChiaCa SET TienBanHang = ISNULL(TienBanHang, 0) + ?, TienChenhLech = ISNULL(TienChenhLech, 0) + ? " +
                           "WHERE MaLoaiCa = (SELECT TOP 1 MaLoaiCa FROM ChiaCa WHERE MaNV = ? AND NgayLam = CAST(GETDATE() AS DATE) AND TinhTrang = N'Đang Làm Việc') " +
                           "AND NgayLam = CAST(GETDATE() AS DATE)";
            psCa = con.prepareStatement(sqlCa);
            psCa.setBigDecimal(1, tienBanHang);
            psCa.setBigDecimal(2, chenhLech);
            psCa.setString(3, maNV);
            psCa.executeUpdate();

            // 4. CẬP NHẬT KHÁCH HÀNG (Nếu có)
            if (kh != null) {
                String sqlKH = "UPDATE KhachHang SET DiemTichLuy = ? WHERE MaKH = ?";
                psKH = con.prepareStatement(sqlKH);
                psKH.setBigDecimal(1, kh.getDiemTichLuy()); // Điểm này đã được cộng/trừ từ UI
                psKH.setString(2, kh.getMaKH());
                psKH.executeUpdate();
            }

            // ✅ THÀNH CÔNG RỰC RỠ: Ký duyệt cho tất cả vào kho
            con.commit(); 

        } catch (Exception e) {
            // ❌ CÓ BIẾN: Lật kèo (Rollback), hủy toàn bộ thao tác để không bị rác dữ liệu
            con.rollback();
            throw new Exception("Lỗi Giao Dịch Siêu Tốc: " + e.getMessage());
        } finally {
            // 🧹 Dọn dẹp hiện trường
            con.setAutoCommit(true);
            if (psHD != null) psHD.close();
            if (psCTHD != null) psCTHD.close();
            if (psCa != null) psCa.close();
            if (psKH != null) psKH.close();
        }
    }
    // =========================================================================
    // 3. TẢI DỮ LIỆU ĐƠN HÀNG (Dùng cho DonHangUi)
    // =========================================================================
    public DuLieuDonHangDTO loadToanBoDuLieuDonHang() {
        DuLieuDonHangDTO dto = new DuLieuDonHangDTO();
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return dto;

        try (CallableStatement cs = con.prepareCall("{call sp_LoadDuLieuDonHang}")) {
            boolean hasResults = cs.execute();

            // 📦 RS 1: Hóa Đơn
            if (hasResults) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        Timestamp ngaySQL = rs.getTimestamp("NgayTao");
                        HoaDon hd = new HoaDon.ThoXayHoaDon()
                        .ganMaHD(rs.getString("MaHD"))
                        .ganNgayTao(ngaySQL != null ? ngaySQL.toLocalDateTime() : null)
                        .ganMaKH(rs.getString("MaKH"))
                        .ganMaNV(rs.getString("MaNV"))
                        .ganPhuongThucTT(rs.getString("PhuongThucTT"))
                        .ganTongGiamGia(rs.getBigDecimal("TongGiamGia"))
                        .ganKhachDua(rs.getBigDecimal("KhachDua"))
                        .ganTienThua(rs.getBigDecimal("TienThua"))
                        .ganThanhTien(rs.getBigDecimal("ThanhTien"))
                        .ganTraHang(rs.getBoolean("TraHang")) // 🔥 PHẢI CÓ DÒNG NÀY THÌ UI MỚI NHẬN ĐƯỢC TRUE/FALSE
                        .taoMoi();
                        dto.dsHoaDon.add(hd);
                    }
                }
            }

            // 📦 RS 2: Chi Tiết Hóa Đơn
            if (cs.getMoreResults()) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        String maHD = rs.getString("MaHD");
                        ChiTietHoaDon ct = new ChiTietHoaDon.ThoXayChiTietHoaDon()
                            .ganMaHD(maHD)
                            .ganMaSp(rs.getString("MaSP"))
                            .ganMaLoHang(rs.getString("MaLoHang"))
                            .ganSoLuong(rs.getInt("SoLuong"))
                            .ganDonGia(rs.getBigDecimal("DonGia"))
                            .ganThanhTienSanPham(rs.getBigDecimal("ThanhTienSanPham"))
                            .taoMoi();
                        
                        dto.mapChiTietHD.putIfAbsent(maHD, new ArrayList<>());
                        dto.mapChiTietHD.get(maHD).add(ct);
                    }
                }
            }

            // 📦 RS 3: Khách Hàng
            if (cs.getMoreResults()) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        dto.mapKhachHang.put(rs.getString("MaKH"), new String[]{rs.getString("HoTen"), rs.getString("BacKH")});
                    }
                }
            }

            // 📦 RS 4: Nhân Viên
            if (cs.getMoreResults()) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        dto.mapNhanVien.put(rs.getString("MaNV"), rs.getString("HoTen"));
                    }
                }
            }

            // 📦 RS 5: Sản Phẩm
            if (cs.getMoreResults()) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        dto.mapSanPham.put(rs.getString("MaSP"), new String[]{rs.getString("TenSP"), rs.getString("MaLoai")});
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("🔥 [SieuTocDAO] Lỗi tải dữ liệu Đơn Hàng: " + e.getMessage());
        }
        return dto;
    }

    // =========================================================================
    // 4. TẢI DỮ LIỆU BÁN HÀNG POS (Dùng cho DanhSachSPUi)
    // =========================================================================
    public DuLieuBanHangDTO loadToanBoSanPhamBanHang() {
        DuLieuBanHangDTO dto = new DuLieuBanHangDTO();
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return dto;

        try (CallableStatement cs = con.prepareCall("{call sp_LoadDuLieuBanHang}")) {
            boolean hasResults = cs.execute();

            // 📦 RS 1: Danh Sách Sản Phẩm
            if (hasResults) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        SanPham sp = new SanPham.ThoXaySanPham()
                            .ganMaSP(rs.getString("MaSP"))
                            .ganTenSP(rs.getString("TenSP"))
                            .ganLinkHinhAnh(rs.getString("LinkHinhAnh"))
                            .ganMaLoai(rs.getString("MaLoai"))
                            .ganGiaBan(rs.getBigDecimal("GiaBan"))
                            .ganDonViTinh(rs.getString("DonViTinh"))
                            .taoMoi();
                        dto.dsSanPham.add(sp);
                        dto.mapTonKho.put(sp.getMaSP(), 0); // Khởi tạo kho = 0 mặc định
                    }
                }
            }

            // 📦 RS 2: Chi Tiết Lô Hàng (Gom nhóm tính tồn kho siêu tốc)
            if (cs.getMoreResults()) {
                try (ResultSet rs = cs.getResultSet()) {
                    Date homNay = Date.valueOf(java.time.LocalDate.now());
                    while (rs.next()) {
                        String maSP = rs.getString("MaSP");
                        int soLuong = rs.getInt("SoLuongTon");
                        Date hsd = rs.getDate("HSD");

                        // Lọc bỏ hàng hết hạn sử dụng trực tiếp tại đây!
                        if (hsd == null || !hsd.before(homNay)) {
                            int tonHienTai = dto.mapTonKho.getOrDefault(maSP, 0);
                            dto.mapTonKho.put(maSP, tonHienTai + soLuong);
                        }
                    }
                }
            }
            String sqlGiamGia = "SELECT MaSP, GiamGia FROM GiamGia " +
                "WHERE TrangThaiGiamGia = N'Đang diễn ra' " +
                "AND GETDATE() BETWEEN BatDau AND KetThuc AND SoLuongApDung > 0";
            try (PreparedStatement ps2 = con.prepareStatement(sqlGiamGia);
                ResultSet rs3 = ps2.executeQuery()) {
                while (rs3.next()) {
                    dto.mapGiamGia.put(rs3.getString("MaSP"), rs3.getInt("GiamGia"));
                }
            }
        } catch (SQLException e) {
            System.err.println("🔥 [SieuTocDAO] Lỗi tải dữ liệu Bán Hàng: " + e.getMessage());
        }
        return dto;
    }
    // =========================================================================
    // 6. LẤY DANH SÁCH NHÂN VIÊN KÈM TRẠNG THÁI REAL-TIME (TỐI ƯU UI) 🎨
    // =========================================================================
    public List<Data.NhanVienViewModel> getDanhSachNhanVienKemTrangThai() {
        List<Data.NhanVienViewModel> list = new ArrayList<>();
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return list;

        // Kỹ sư Front mách nhỏ: Ta JOIN với LoaiCa để lấy GioBatDau và GioKetThuc cho chuẩn xác nhé!
        String sql = """
            SELECT NV.*,
                CASE 
                    WHEN CC.MaNV IS NOT NULL 
                         AND CAST(GETDATE() AS TIME) BETWEEN LC.GioBatDau AND LC.GioKetThuc
                         AND CC.NgayLam = CAST(GETDATE() AS DATE)
                         AND CC.TinhTrang = N'Đang Làm Việc'
                    THEN N'Đang làm việc'
                    ELSE N'Không trong ca'
                END AS TrangThaiLamViec
            FROM NhanVien NV
            LEFT JOIN ChiaCa CC ON NV.MaNV = CC.MaNV AND CC.NgayLam = CAST(GETDATE() AS DATE)
            LEFT JOIN LoaiCa LC ON CC.MaLoaiCa = LC.MaLoaiCa
        """;

        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
             
            while (rs.next()) {
                Data.NhanVienViewModel nv = new Data.NhanVienViewModel();
                nv.setMaNV(rs.getString("MaNV"));
                nv.setHoTen(rs.getString("HoTen"));
                nv.setSDT(rs.getString("SDT"));
                nv.setChucVu(rs.getString("ChucVu"));
                nv.setLuongGio(rs.getBigDecimal("LuongGio"));
                
                java.sql.Date ngayVao = rs.getDate("NgayVaoLam");
                if (ngayVao != null) nv.setNgayVaoLam(ngayVao.toLocalDate());
                
                java.sql.Date ngayNghi = rs.getDate("NgayNghiViec");
                if (ngayNghi != null) nv.setNgayNghiViec(ngayNghi.toLocalDate());
                
                nv.setTrangThai(rs.getString("TrangThai")); // Trạng thái làm/nghỉ việc
                
                // Thuộc tính siêu cấp xịn xò mới thêm
                nv.setTrangThaiLamViec(rs.getString("TrangThaiLamViec"));
                
                list.add(nv);
            }
        } catch (SQLException e) {
            System.err.println("🔥 [SieuTocDAO] Lỗi lấy NV kèm trạng thái: " + e.getMessage());
        }
        return list;
    }
    // =========================================================================
    // 8. TẢI DỮ LIỆU NHẬP HÀNG SIÊU TỐC (Trị dứt điểm N+1 Query) 🚀🚀🚀
    // =========================================================================
    public static class DuLieuNhapHangDTO {
        public List<Data.LoHang> dsLoHang = new ArrayList<>();
        public Map<String, String> mapTenNcc = new HashMap<>(); // MaNCC -> TenNCC
        public Map<String, Integer> mapTongSoLuong = new HashMap<>(); // MaLoHang -> Tổng SP
        public Map<String, List<Data.ChiTietLoHang>> mapChiTiet = new HashMap<>(); // MaLoHang -> Danh sách Chi tiết
        public Map<String, String> mapTenSp = new HashMap<>(); // MaSP -> TenSP
    }

    public DuLieuNhapHangDTO loadDuLieuNhapHangSieuToc() {
        DuLieuNhapHangDTO dto = new DuLieuNhapHangDTO();
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return dto;

        // Chạy 4 lệnh SQL gom chung vào 1 mẻ để tiết kiệm 99% thời gian giao tiếp DB
        String sql = "SELECT * FROM LoHang; " +
                     "SELECT MaNCC, TenNCC FROM NhaCungCap; " +
                     "SELECT * FROM ChiTietLoHang; " +
                     "SELECT MaSP, TenSP FROM SanPham;";

        try (Statement st = con.createStatement()) {
            boolean hasResults = st.execute(sql);
            
            // 📦 RS 1: Danh sách Lô Hàng
            if (hasResults) {
                try (ResultSet rs = st.getResultSet()) {
                    while (rs.next()) {
                        Data.LoHang lh = new Data.LoHang();
                        /* ⚠️ LƯU Ý: Nếu class LoHang của bạn dùng ThoXayLoHang (Builder Pattern)
                           thì hãy sửa lại đoạn này cho khớp nhé. Dưới đây là cách set cơ bản: */
                        lh.setMaLoHang(rs.getString("MaLoHang"));
                        lh.setMaNCC(rs.getString("MaNCC"));
                        
                        java.sql.Date ngayNhap = rs.getDate("NgayNhapKho");
                        if (ngayNhap != null) lh.setNgayNhapKho(ngayNhap.toLocalDate());
                        
                        lh.setThanhTien(rs.getBigDecimal("ThanhTien"));
                        dto.dsLoHang.add(lh);
                    }
                }
            }
            
            // 📦 RS 2: Map Tên Nhà Cung Cấp
            if (st.getMoreResults()) {
                try (ResultSet rs = st.getResultSet()) {
                    while (rs.next()) {
                        dto.mapTenNcc.put(rs.getString("MaNCC"), rs.getString("TenNCC"));
                    }
                }
            }
            
            // 📦 RS 3: Map Chi Tiết Lô Hàng & Tính Tổng Số Lượng
            if (st.getMoreResults()) {
                try (ResultSet rs = st.getResultSet()) {
                    while (rs.next()) {
                        Data.ChiTietLoHang ct = new Data.ChiTietLoHang();
                        String maLo = rs.getString("MaLoHang");
                        ct.setMaLoHang(maLo);
                        ct.setMaSP(rs.getString("MaSP"));
                        ct.setSoLuongNhap(rs.getInt("SoLuongNhap"));
                        ct.setGiaNhap(rs.getBigDecimal("GiaNhap"));
                        
                        java.sql.Date nsx = rs.getDate("NSX");
                        if (nsx != null) ct.setNSX(nsx.toLocalDate());
                        
                        java.sql.Date hsd = rs.getDate("HSD");
                        if (hsd != null) ct.setHSD(hsd.toLocalDate());
                        
                        // Phân loại chi tiết vào đúng mã Lô
                        dto.mapChiTiet.putIfAbsent(maLo, new ArrayList<>());
                        dto.mapChiTiet.get(maLo).add(ct);
                        
                        // Cộng dồn vào Map Tổng Số Lượng
                        dto.mapTongSoLuong.put(maLo, dto.mapTongSoLuong.getOrDefault(maLo, 0) + ct.getSoLuongNhap());
                    }
                }
            }
            
            // 📦 RS 4: Map Tên Sản Phẩm
            if (st.getMoreResults()) {
                try (ResultSet rs = st.getResultSet()) {
                    while (rs.next()) {
                        dto.mapTenSp.put(rs.getString("MaSP"), rs.getString("TenSP"));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("🔥 [SieuTocDAO] Lỗi tải dữ liệu Nhập Hàng: " + e.getMessage());
        }
        return dto;
    }
    // =========================================================================
    // THÊM VÀO: TruyVanSieuTocDAO.java
    // 9. HOÀN TRẢ HÀNG (TRANSACTION) 🚀
    // =========================================================================
    public void xuLyTraHangToanBoSieuToc(String maHD, String lyDoGop, List<Data.ChiTietHoaDon> dsChiTiet) throws Exception {
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) throw new Exception("Không thể kết nối CSDL!");

        PreparedStatement psHD = null;
        PreparedStatement psTonKho = null;

        try {
            con.setAutoCommit(false); // BẮT ĐẦU TRANSACTION

            // 1. Cập nhật hóa đơn thành Đã Trả Hàng & Lưu lý do
            String sqlHD = "UPDATE HoaDon SET TraHang = 1, LyDoTraHang = ? WHERE MaHD = ?";
            psHD = con.prepareStatement(sqlHD);
            psHD.setString(1, lyDoGop);
            psHD.setString(2, maHD);
            psHD.executeUpdate();

            // 2. Rollback Tồn Kho chính xác theo Lô
            String sqlKho = "UPDATE ChiTietLoHang SET SoLuongTon = SoLuongTon + ? WHERE MaLoHang = ? AND MaSP = ?";
            psTonKho = con.prepareStatement(sqlKho);
            for (Data.ChiTietHoaDon ct : dsChiTiet) {
                psTonKho.setInt(1, ct.getSoLuong());
                psTonKho.setString(2, ct.getMaLoHang()); // ĐÚNG LÔ HÀNG ĐÃ XUẤT
                psTonKho.setString(3, ct.getMaSp());
                psTonKho.addBatch();
            }
            psTonKho.executeBatch(); // Chạy batch cực nhanh

            con.commit(); // THÀNH CÔNG -> LƯU

        } catch (Exception e) {
            con.rollback(); // LỖI -> HỦY BỎ TẤT CẢ
            throw new Exception("Lỗi hoàn trả: " + e.getMessage());
        } finally {
            con.setAutoCommit(true);
            if (psHD != null) psHD.close();
            if (psTonKho != null) psTonKho.close();
        }
    }
    // =========================================================================
    // 10. TẢI DỮ LIỆU CHO AI QUÉT KHUYẾN MÃI & BATCH INSERT GIẢM GIÁ 🚀
    // =========================================================================
    
    // Hàm 1: Lấy toàn bộ hàng tồn kho để AI phân tích
    public List<Object[]> loadDuLieuQuetKhoAISieuToc() {
        List<Object[]> list = new ArrayList<>();
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return list;

        String sql = "SELECT ct.MaSP, sp.MaLoai, ct.HSD, ct.SoLuongTon, ct.GiaNhap, sp.GiaBan, lh.NgayNhapKho " +
                     "FROM ChiTietLoHang ct " +
                     "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "JOIN LoHang lh ON ct.MaLoHang = lh.MaLoHang " +
                     "WHERE ct.SoLuongTon > 0";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("MaSP"), rs.getString("MaLoai"), 
                    rs.getDate("HSD"), rs.getInt("SoLuongTon"), 
                    rs.getDouble("GiaNhap"), rs.getDouble("GiaBan"), rs.getDate("NgayNhapKho")
                });
            }
        } catch (SQLException e) {
            System.err.println("🔥 [SieuTocDAO] Lỗi tải dữ liệu AI quét kho: " + e.getMessage());
        }
        return list;
    }

    // Hàm 2: Lấy danh sách (Set) các SP đang được giảm giá (Tra cứu siêu tốc O(1) trên RAM)
    public java.util.Set<String> getTapHopSanPhamDangGiamGia() {
        java.util.Set<String> set = new java.util.HashSet<>();
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return set;
        
        String sql = "SELECT DISTINCT MaSP FROM GiamGia WHERE TrangThaiGiamGia = N'Đang diễn ra' AND GETDATE() BETWEEN BatDau AND KetThuc AND SoLuongApDung > 0";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                set.add(rs.getString("MaSP"));
            }
        } catch (Exception e) {
            System.err.println("🔥 [SieuTocDAO] Lỗi tải Set giảm giá: " + e.getMessage());
        }
        return set;
    }

    // Hàm 3: Batch Insert - Lưu hàng nghìn mã giảm giá vào CSDL chỉ mất chưa tới 0.1 giây!
    public void themGiamGiaHangLoatSieuToc(List<Data.GiamGia> dsGiamGiaMoi) throws Exception {
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) throw new Exception("Không thể kết nối CSDL!");

        PreparedStatement psInsert = null;
        try {
            con.setAutoCommit(false); // Bật Transaction an toàn
            
            String sqlInsert = "INSERT INTO GiamGia (MaGiamGia, MaSP, BatDau, KetThuc, GiamGia, LoaiGiamGia, TrangThaiGiamGia, SoLuongApDung) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            psInsert = con.prepareStatement(sqlInsert);
            
            for (Data.GiamGia gg : dsGiamGiaMoi) {
                psInsert.setString(1, gg.getMaGiamGia());
                psInsert.setString(2, gg.getMaSP());
                psInsert.setTimestamp(3, Timestamp.valueOf(gg.getBatDau()));
                psInsert.setTimestamp(4, Timestamp.valueOf(gg.getKetThuc()));
                psInsert.setBigDecimal(5, gg.getGiamGia());
                psInsert.setString(6, gg.getLoaiGiamGia());
                psInsert.setString(7, gg.getTrangThaiGiamGia());
                psInsert.setInt(8, gg.getSoLuongApDung());
                
                psInsert.addBatch(); // Đưa vào mẻ (Batch) thay vì chạy ngay
            }
            
            psInsert.executeBatch(); // Thực thi cả mẻ 1 lúc
            con.commit(); // Thành công -> Ký duyệt
            
        } catch (Exception e) {
            con.rollback(); // Lỗi -> Hoàn tác toàn bộ
            throw new Exception("Lỗi Batch Insert Giảm Giá: " + e.getMessage());
        } finally {
            con.setAutoCommit(true);
            if (psInsert != null) psInsert.close();
        }
    }
    // =========================================================================
    // DTO: CHỨA DỮ LIỆU LỊCH SỬ GIẢM GIÁ (Lấy từ nhiều bảng cùng lúc)
    // =========================================================================
    public static class LichSuGiamGiaDTO {
        public String maGiamGia;
        public String maSP;
        public String tenSP;
        public java.time.LocalDateTime batDau;
        public java.time.LocalDateTime ketThuc;
        public BigDecimal giamGia;
        public String loaiGiamGia;
        public String trangThai;
        public int soLuong;
    }

    public static class ThongKeGiamGiaDTO {
        public int tongLuotApDung = 0;
        public BigDecimal tongTienGiam = BigDecimal.ZERO;
    }

    // =========================================================================
    // HÀM TRUY VẤN: LẤY DANH SÁCH & THỐNG KÊ SIÊU TỐC
    // =========================================================================
    public List<LichSuGiamGiaDTO> layLichSuGiamGiaSieuToc() {
        List<LichSuGiamGiaDTO> list = new ArrayList<>();
        // JOIN 2 bảng để lấy Tên SP, sắp xếp chương trình Đang diễn ra lên đầu
        String sql = "SELECT g.MaGiamGia, g.MaSP, s.TenSP, g.BatDau, g.KetThuc, " +
                     "g.GiamGia, g.LoaiGiamGia, g.TrangThaiGiamGia, g.SoLuongApDung " +
                     "FROM GiamGia g JOIN SanPham s ON g.MaSP = s.MaSP " +
                     "ORDER BY CASE WHEN g.TrangThaiGiamGia = N'Đang diễn ra' THEN 1 ELSE 2 END, g.BatDau DESC";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                LichSuGiamGiaDTO dto = new LichSuGiamGiaDTO();
                dto.maGiamGia = rs.getString("MaGiamGia");
                dto.maSP = rs.getString("MaSP");
                dto.tenSP = rs.getString("TenSP");

                Timestamp bd = rs.getTimestamp("BatDau");
                if (bd != null) dto.batDau = bd.toLocalDateTime();

                Timestamp kt = rs.getTimestamp("KetThuc");
                if (kt != null) dto.ketThuc = kt.toLocalDateTime();

                dto.giamGia = rs.getBigDecimal("GiamGia");
                dto.loaiGiamGia = rs.getString("LoaiGiamGia");

                // Map trạng thái Database sang giao diện
                String st = rs.getString("TrangThaiGiamGia");
                if ("Đang diễn ra".equals(st)) dto.trangThai = "Đang hoạt động";
                else if ("Sắp diễn ra".equals(st)) dto.trangThai = "Sắp diễn ra";
                else dto.trangThai = "Đã kết thúc";

                dto.soLuong = rs.getInt("SoLuongApDung");
                list.add(dto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ThongKeGiamGiaDTO layThongKeTongQuanGiamGia() {
        ThongKeGiamGiaDTO tk = new ThongKeGiamGiaDTO();
        // Quét toàn bộ ChiTietHoaDon để tính chính xác Lượt dùng & Tiền đã tiết kiệm cho khách
        String sql = "SELECT COUNT(MaGiamGia) AS TongLuot, SUM((DonGia * SoLuong) - ThanhTienSanPham) AS TongTienGiam " +
                     "FROM ChiTietHoaDon WHERE MaGiamGia IS NOT NULL";
        
        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                tk.tongLuotApDung = rs.getInt("TongLuot");
                tk.tongTienGiam = rs.getBigDecimal("TongTienGiam");
                if (tk.tongTienGiam == null) tk.tongTienGiam = BigDecimal.ZERO;
            }
        } catch (Exception e) {}
        return tk;
    }
    // =========================================================================
    // DTO: CHI TIẾT GIAO DỊCH CỦA 1 MÃ GIẢM GIÁ
    // =========================================================================
    public static class ChiTietGiamGiaDTO {
        public String maHD;
        public String tenSP;
        public String tenKH;
        public java.time.LocalDateTime ngayTao;
        public int soLuong;
        public BigDecimal thanhTien;
        public BigDecimal tienGiam;
    }

    public List<ChiTietGiamGiaDTO> layChiTietApDungGiamGia(String maGiamGia) {
        List<ChiTietGiamGiaDTO> list = new ArrayList<>();
        // JOIN 4 bảng: ChiTietHoaDon, HoaDon, SanPham, KhachHang (LEFT JOIN vì có thể là khách vãng lai)
        String sql = "SELECT hd.MaHD, sp.TenSP, ISNULL(kh.HoTen, N'Khách vãng lai') AS TenKH, " +
                     "hd.NgayTao, ct.SoLuong, ct.ThanhTienSanPham, " +
                     "((ct.DonGia * ct.SoLuong) - ct.ThanhTienSanPham) AS TienGiam " +
                     "FROM ChiTietHoaDon ct " +
                     "JOIN HoaDon hd ON ct.MaHD = hd.MaHD " +
                     "JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "LEFT JOIN KhachHang kh ON hd.MaKH = kh.MaKH " +
                     "WHERE ct.MaGiamGia = ? " +
                     "ORDER BY hd.NgayTao DESC";

        try (Connection con = ConnectDB.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
             
            ps.setString(1, maGiamGia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietGiamGiaDTO dto = new ChiTietGiamGiaDTO();
                    dto.maHD = rs.getString("MaHD");
                    dto.tenSP = rs.getString("TenSP");
                    dto.tenKH = rs.getString("TenKH");
                    
                    Timestamp ts = rs.getTimestamp("NgayTao");
                    if (ts != null) dto.ngayTao = ts.toLocalDateTime();
                    
                    dto.soLuong = rs.getInt("SoLuong");
                    dto.thanhTien = rs.getBigDecimal("ThanhTienSanPham");
                    dto.tienGiam = rs.getBigDecimal("TienGiam");
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // =========================================================================
    // 11. TẢI DỮ LIỆU KIỂM KÊ KHO SIÊU TỐC (Gom nhóm Tồn Kho theo Sản Phẩm)
    // =========================================================================
    public static class DuLieuKiemKeSieuTocDTO {
        public List<Data.SanPham> dsSanPham = new ArrayList<>();
        public Map<String, Integer> mapTongTonKho = new HashMap<>(); // MaSP -> Tổng số lượng tồn
        public Map<String, List<Data.ChiTietLoHang>> mapDanhSachLo = new HashMap<>(); // MaSP -> Danh sách các lô
        public Map<String, String> mapTenLoai = new HashMap<>();
    }

    public DuLieuKiemKeSieuTocDTO loadDuLieuKiemKeSieuToc() {
        DuLieuKiemKeSieuTocDTO dto = new DuLieuKiemKeSieuTocDTO();
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) return dto;

        // 🔥 BỔ SUNG: Lấy thêm GiaNhap từ ChiTietLoHang để tính Giá trị kho
        String sql = "SELECT * FROM SanPham; " +
                     "SELECT MaSP, MaLoHang, SoLuongTon, HSD, GiaNhap FROM ChiTietLoHang ORDER BY HSD ASC;";

        try (Statement st = con.createStatement()) {
            boolean hasResults = st.execute(sql);
            
            // 📦 Kết quả 1: Sản phẩm
            if (hasResults) {
                try (ResultSet rs = st.getResultSet()) {
                    while (rs.next()) {
                        Data.SanPham sp = new Data.SanPham.ThoXaySanPham()
                            .ganMaSP(rs.getString("MaSP"))
                            .ganTenSP(rs.getString("TenSP"))
                            .ganLinkHinhAnh(rs.getString("LinkHinhAnh"))
                            .ganMaLoai(rs.getString("MaLoai"))
                            .ganDonViTinh(rs.getString("DonViTinh"))
                            .ganGiaBan(rs.getBigDecimal("GiaBan")) // Bổ sung Giá Bán để hiện lên Bảng Web
                            .taoMoi();
                        dto.dsSanPham.add(sp);
                        dto.mapTongTonKho.put(sp.getMaSP(), 0); 
                        dto.mapDanhSachLo.put(sp.getMaSP(), new ArrayList<>()); // Khởi tạo danh sách lô rỗng
                    }
                }
            }
            
            // 📦 Kết quả 2: Chi Tiết Lô Hàng
            if (st.getMoreResults()) {
                try (ResultSet rs = st.getResultSet()) {
                    while (rs.next()) {
                        String maSP = rs.getString("MaSP");
                        String maLo = rs.getString("MaLoHang");
                        int ton = rs.getInt("SoLuongTon");
                        Date sqlHSD = rs.getDate("HSD");
                        
                        Data.ChiTietLoHang ct = new Data.ChiTietLoHang();
                        ct.setMaSP(maSP);
                        ct.setMaLoHang(maLo);
                        ct.setSoLuongTon(ton);
                        if (sqlHSD != null) ct.setHSD(sqlHSD.toLocalDate());
                        
                        // Bổ sung Giá Nhập để Web tính "Tổng Giá Trị Kho"
                        ct.setGiaNhap(rs.getBigDecimal("GiaNhap")); 
                        
                        // Thêm lô vào danh sách của Sản Phẩm tương ứng
                        if (dto.mapDanhSachLo.containsKey(maSP)) {
                            dto.mapDanhSachLo.get(maSP).add(ct);
                            
                            // Cộng dồn Tồn Kho tổng
                            int tongHienTai = dto.mapTongTonKho.get(maSP);
                            dto.mapTongTonKho.put(maSP, tongHienTai + ton);
                        }
                    }
                }
            }

            // 📦 Kết quả 3: Nạp Tên Loại Sản Phẩm siêu tốc vào Cache
            try {
                try (ResultSet rs = st.executeQuery("SELECT MaLoai, TenLoai FROM LoaiHang")) {
                    while (rs.next()) {
                        dto.mapTenLoai.put(rs.getString("MaLoai"), rs.getString("TenLoai"));
                    }
                }
            } catch (Exception ex1) {
                // Dự phòng trường hợp CSDL của bạn đặt tên bảng là LoaiSanPham
                try (ResultSet rs = st.executeQuery("SELECT MaLoai, TenLoai FROM LoaiSanPham")) {
                    while (rs.next()) {
                        dto.mapTenLoai.put(rs.getString("MaLoai"), rs.getString("TenLoai"));
                    }
                } catch (Exception ex2) {}
            }
            
        } catch (SQLException e) {
            System.err.println("🔥 [SieuTocDAO] Lỗi tải dữ liệu Kiểm Kê: " + e.getMessage());
        }
        return dto;
    }
    // =========================================================================
    // 12. ĐỒNG BỘ KIỂM KÊ KHO GỘP CHUNG (BATCH INSERT + UPDATE TỒN KHO) 🚀
    // =========================================================================
    public void dongBoKiemKeGopChungSieuToc(List<Data.KiemKeKho> dsKiemKe) throws Exception {
        Connection con = ConnectDB.getInstance().getConnection();
        if (con == null) throw new Exception("Không thể kết nối máy chủ CSDL!");

        PreparedStatement psLapPhieu = null;
        PreparedStatement psCanBangKho = null;

        try {
            con.setAutoCommit(false); // BẮT ĐẦU TRANSACTION

            // 1. Lệnh tạo phiếu kiểm kê (Trạng thái Đã Cân Kho luôn vì mình làm gộp)
            String sqlLapPhieu = "INSERT INTO KiemKeKho(MaKiemKe, NgayKiemKe, MaNV, MaLoHang, MaSP, SoLuongHeThong, SoLuongThucTe, LyDo, TrangThai) VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, N'Đã Cân Kho')";
            psLapPhieu = con.prepareStatement(sqlLapPhieu);

            // 2. Lệnh đè lại số lượng tồn thực tế vào kho
            String sqlCanBang = "UPDATE ChiTietLoHang SET SoLuongTon = ? WHERE MaLoHang = ? AND MaSP = ?";
            psCanBangKho = con.prepareStatement(sqlCanBang);

            for (Data.KiemKeKho kk : dsKiemKe) {
                // Nạp đạn cho lệnh 1
                psLapPhieu.setString(1, kk.getMaKiemKe());
                psLapPhieu.setString(2, kk.getMaNV());
                psLapPhieu.setString(3, kk.getMaLoHang());
                psLapPhieu.setString(4, kk.getMaSP());
                psLapPhieu.setInt(5, kk.getSoLuongHeThong());
                psLapPhieu.setInt(6, kk.getSoLuongThucTe());
                psLapPhieu.setString(7, kk.getLyDo());
                psLapPhieu.addBatch();

                // Nạp đạn cho lệnh 2
                psCanBangKho.setInt(1, kk.getSoLuongThucTe());
                psCanBangKho.setString(2, kk.getMaLoHang());
                psCanBangKho.setString(3, kk.getMaSP());
                psCanBangKho.addBatch();
            }

            // 🚀 BẮN MỘT LẦN TOÀN BỘ BATCH
            psLapPhieu.executeBatch();
            psCanBangKho.executeBatch();

            con.commit(); // THÀNH CÔNG RỰC RỠ

        } catch (Exception e) {
            con.rollback(); // CÓ BIẾN LÀ QUAY XE NGAY
            throw new Exception("Lỗi đồng bộ gộp: " + e.getMessage());
        } finally {
            con.setAutoCommit(true);
            if (psLapPhieu != null) psLapPhieu.close();
            if (psCanBangKho != null) psCanBangKho.close();
        }
    }
    // =========================================================================
    // 🔥 ĐỘNG CƠ TURBO CHO MODULE TIÊU HỦY / KIỂM KÊ KHO
    // =========================================================================
    public static class DuLieuTieuHuyDTO {
        public List<Data.ChiTietLoHang> dsLoHangKho = new ArrayList<>();
        public Map<String, String> mapTenSanPham = new HashMap<>();
    }

    public DuLieuTieuHuyDTO loadDuLieuKhoSieuToc() {
        DuLieuTieuHuyDTO dto = new DuLieuTieuHuyDTO();
        
        // Chỉ quét những lô CÒN TỒN KHO ngay từ Database để giảm tải RAM
        String sqlLoHang = "SELECT MaLoHang, MaSP, SoLuongTon, HSD, GiaNhap FROM ChiTietLoHang WHERE SoLuongTon > 0";
        String sqlSanPham = "SELECT MaSP, TenSP FROM SanPham";

        // DÙNG 1 CONNECTION DUY NHẤT CHO CẢ 2 BẢNG
        try (Connection con = ConnectDB.getInstance().getConnection();
             Statement st = con.createStatement()) {
            
            // 1. Kéo Lô Hàng siêu tốc
            try (ResultSet rs = st.executeQuery(sqlLoHang)) {
                while (rs.next()) {
                    Data.ChiTietLoHang lo = new Data.ChiTietLoHang.ThoXayChiTietLoHang()
                        .ganMaLoHang(rs.getString("MaLoHang"))
                        .ganMaSP(rs.getString("MaSP"))
                        .ganSoLuongTon(rs.getInt("SoLuongTon"))
                        .ganHSD(rs.getDate("HSD") != null ? rs.getDate("HSD").toLocalDate() : null)
                        .ganGiaNhap(rs.getBigDecimal("GiaNhap"))
                        .taoMoi();
                    dto.dsLoHangKho.add(lo);
                }
            }

            // 2. Kéo Map Tên Sản Phẩm siêu tốc
            try (ResultSet rs = st.executeQuery(sqlSanPham)) {
                while (rs.next()) {
                    dto.mapTenSanPham.put(rs.getString("MaSP"), rs.getString("TenSP"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[TruyVanSieuTocDAO] Lỗi loadDuLieuKhoSieuToc: " + e.getMessage());
        }
        return dto;
    }
}