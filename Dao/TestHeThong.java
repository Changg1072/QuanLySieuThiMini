//package Dao;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//
//import Data.ChiTietHoaDon;
//import Data.HoaDon;
//import Data.KhachHang;
//
//public class TestHeThong {
//	public static void main(String[] args) {
//		// 1. Kết nối CSDL
//		ConnectDB.getInstance().getConnection();
//		System.out.println("--- ĐÃ KẾT NỐI DATABASE ---");
//
//		// Khởi tạo các DAO
//		KhachHangDAO khDao = new KhachHangDAO();
//		HoaDonDAO hdDao = new HoaDonDAO();
//		ChiTietHoaDonDAO cthdDao = new ChiTietHoaDonDAO();
//		
//		System.out.println("\n=== 1. TEST THÊM KHÁCH HÀNG BẰNG THỢ XÂY ===");
//		KhachHang khMoi = new KhachHang.ThoXayKhachHang()
//				.ganMaKH("KH_TEST01")
//				.ganHoTen("Nguyễn Văn Dũng")
//				.ganSDT("0987654321")
//				.ganDiemTichLuy(0)
//				.ganNgayDangKy(LocalDate.now())
//				.ganBacKH("Vip")
//				.taoMoi();
//		
//		boolean kqThemKH = khDao.themKhachHang(khMoi);
//		System.out.println("-> Thêm Khách Hàng: " + (kqThemKH ? "THÀNH CÔNG" : "THẤT BẠI"));
//
//		System.out.println("\n=== 2. TEST TẠO HÓA ĐƠN CHO KHÁCH VỪA RỒI ===");
//		HoaDon hdMoi = new HoaDon.ThoXayHoaDon()
//				.ganMaHD("HD_TEST01")
//				.ganNgayTao(LocalDate.now())
//				.ganMaKH("KH_TEST01") // Khớp với mã khách vừa tạo ở trên
//				.ganMaNV("NV01")      // Giả sử có nhân viên NV01 trong DB
//				.ganThanhTien(150000)
//				.ganPhuongThucTT("Tiền mặt")
//				.taoMoi();
//				
//		boolean kqThemHD = hdDao.themHoaDon(hdMoi);
//		System.out.println("-> Tạo Hóa Đơn: " + (kqThemHD ? "THÀNH CÔNG" : "THẤT BẠI"));
//
//		System.out.println("\n=== 3. TEST THÊM MÓN HÀNG VÀO HÓA ĐƠN ===");
//		ChiTietHoaDon ctMoi = new ChiTietHoaDon.ThoXayChiTietHoaDon()
//				.ganMaHD("HD_TEST01") // Khớp mã hóa đơn ở trên
//				.ganMaSp("SP01")      // Giả sử có SP01 trong DB
//				.ganSoLuong(2)
//				.ganDonGia(75000)
//				.ganThanhTienSanPham(150000)
//				.taoMoi();
//				
//		boolean kqThemCT = cthdDao.themChiTietHoaDon(ctMoi);
//		System.out.println("-> Thêm Chi Tiết HĐ: " + (kqThemCT ? "THÀNH CÔNG" : "THẤT BẠI"));
//		
//		System.out.println("\n=== HOÀN TẤT TEST LUỒNG BÁN HÀNG ===");
//	}
//}