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
        // Lưu tổng tồn kho khả dụng của từng mã SP
        public Map<String, Integer> mapTonKho = new HashMap<>(); 
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
            // 📦 RS 2: Danh sách Loại Ca
            if (cs.getMoreResults()) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        LoaiCa lc = new LoaiCa();
                        lc.setMaLoaiCa(rs.getString("MaLoaiCa"));
                        lc.setTenCa(rs.getString("TenCa"));
                        // Nếu GioBatDau trong Data của cậu là LocalTime thì dùng .toLocalTime()
                        // Nếu là java.sql.Time thì bỏ .toLocalTime() đi nhé!
                        lc.setGioBatDau(rs.getTime("GioBatDau").toLocalTime());
                        lc.setGioKetThuc(rs.getTime("GioKetThuc").toLocalTime());
                        
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
            String sqlCTHD = "INSERT INTO ChiTietHoaDon (MaHD, MaSP, MaLoHang, SoLuong, DonGia, ThanhTienSanPham) VALUES (?, ?, ?, ?, ?, ?)";
            psCTHD = con.prepareStatement(sqlCTHD);
            for (Data.ChiTietHoaDon ct : dsChiTiet) {
                psCTHD.setString(1, ct.getMaHD());
                psCTHD.setString(2, ct.getMaSp());
                psCTHD.setString(3, ct.getMaLoHang()); // Nhớ gán sẵn mã lô từ UI nhé
                psCTHD.setInt(4, ct.getSoLuong());
                psCTHD.setBigDecimal(5, ct.getDonGia());
                psCTHD.setBigDecimal(6, ct.getThanhTienSanPham());
                
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
}