package Logic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import Dao.ChiTietLoHangDAO;
import Dao.SanPhamDAO;
import Data.ChiTietLoHang;
import Data.SanPham;

// Đã đổi tên Class thành QuanLyKhoCacheLogic cho chuyên nghiệp
public class QuanLyKhoCacheLogic {

	private static QuanLyKhoCacheLogic instance;
	private KetQuaPhanLoaiSP duLieuKhoCache; 

	private ChiTietLoHangDAO chiTietLoHangDAO;
	private SanPhamDAO sanPhamDAO;

	private QuanLyKhoCacheLogic() {
		// ĐÃ SỬA: Dùng getInstance() cho chuẩn bài, không dùng new
		this.chiTietLoHangDAO = ChiTietLoHangDAO.getInstance(); 
		this.sanPhamDAO = SanPhamDAO.getInstance(); 
	}

	public static QuanLyKhoCacheLogic getInstance() {
		if (instance == null) {
			instance = new QuanLyKhoCacheLogic();
		}
		return instance;
	}

	// --- HÀM KHỞI TẠO DỮ LIỆU ---
	public void taiLaiDuLieuKho() {
		this.duLieuKhoCache = phanLoaiToanBoSanPham();
		System.out.println("Đã tải và phân loại toàn bộ dữ liệu kho vào bộ nhớ tạm!");
	}

	public KetQuaPhanLoaiSP getDuLieuKho() {
		if (duLieuKhoCache == null) {
			taiLaiDuLieuKho(); 
		}
		return duLieuKhoCache;
	}

	// ==========================================================
	// CÁC HÀM CẬP NHẬT TỒN KHO 
	// ==========================================================
	public boolean banHangTruTonKho(String maLoHang, String maSP, int soLuongBan) {
		boolean thanhCong = chiTietLoHangDAO.truSoLuongTon(maLoHang, maSP, soLuongBan);
		if (thanhCong) {
			taiLaiDuLieuKho();
		}
		return thanhCong;
	}
	
	// ==========================================================
		// KHÁCH TRẢ HÀNG -> CỘNG BÙ VÀO KHO
		// ==========================================================
		public boolean traHangCongTonKho(String maLoHang, String maSP, int soLuongTra) {
			// Gọi hàm DAO vừa tạo ở Bước 1
			boolean thanhCong = chiTietLoHangDAO.congSoLuongTon(maLoHang, maSP, soLuongTra);
			
			if (thanhCong) {
				taiLaiDuLieuKho(); // Đồng bộ lại số lượng trên màn hình ngay lập tức!
			}
			return thanhCong;
		}

	public boolean nhapHangCongTonKho(ChiTietLoHang ctMoi) {
		boolean thanhCong = chiTietLoHangDAO.themChiTietLoHang(ctMoi);
		if (thanhCong) {
			taiLaiDuLieuKho();
		}
		return thanhCong;
	}

	// ==========================================================
	// LỚP CHỨA DỮ LIỆU
	// ==========================================================
	public class ThongTinTonKhoSP { 
		private SanPham sanPham;
		private int tongSoLuongTon;	 
		private int soLuongTonHopLe;	
		private String trangThai;	   

		public ThongTinTonKhoSP(SanPham sanPham, int tongSoLuongTon, int soLuongTonHopLe, String trangThai) {
			this.sanPham = sanPham;
			this.tongSoLuongTon = tongSoLuongTon;
			this.soLuongTonHopLe = soLuongTonHopLe;
			this.trangThai = trangThai;
		}

		public SanPham getSanPham() { return sanPham; }
		public int getTongSoLuongTon() { return tongSoLuongTon; }
		public int getSoLuongTonHopLe() { return soLuongTonHopLe; }
		public String getTrangThai() { return trangThai; }
	}

	public class KetQuaPhanLoaiSP { 
		private List<ThongTinTonKhoSP> danhSachConLai = new ArrayList<>();
		private List<ThongTinTonKhoSP> danhSachDaHet = new ArrayList<>();
		
		public List<ThongTinTonKhoSP> getDanhSachConLai() { return danhSachConLai; }
		public List<ThongTinTonKhoSP> getDanhSachDaHet() { return danhSachDaHet; }
		
		public void themVaoConLai(ThongTinTonKhoSP sp) { danhSachConLai.add(sp); }
		public void themVaoDaHet(ThongTinTonKhoSP sp) { danhSachDaHet.add(sp); }
	}

	// ==========================================================
	// LOGIC PHÂN LOẠI CỐT LÕI
	// ==========================================================
	private KetQuaPhanLoaiSP phanLoaiToanBoSanPham() {
		KetQuaPhanLoaiSP ketQua = new KetQuaPhanLoaiSP();
		List<SanPham> dsSanPham = sanPhamDAO.layDanhSachSanPham(); 
		
		// Sửa lại tên hàm layDanhSachChiTiet cho khớp với DAO của bạn (nếu có)
		List<ChiTietLoHang> dsChiTietLoHang = chiTietLoHangDAO.layDanhSachChiTietLoHang(); 
		LocalDate homNay = LocalDate.now();

		for (SanPham sp : dsSanPham) {
			int tongTonKho = 0;
			int tongTonHopLe = 0;

			for (ChiTietLoHang ct : dsChiTietLoHang) {
				if (ct.getMaSP().equals(sp.getMaSP())) { 
					int soLuong = ct.getSoLuongTon();
					tongTonKho += soLuong;
					
					boolean daHetHan = ct.getHSD() != null && ct.getHSD().isBefore(homNay); 
					if (!daHetHan) tongTonHopLe += soLuong; 
				}
			}

			if (tongTonHopLe > 0) {
				String trangThai = "Đang kinh doanh (Còn " + tongTonHopLe + " SP)";
				ketQua.themVaoConLai(new ThongTinTonKhoSP(sp, tongTonKho, tongTonHopLe, trangThai));
			} else {
				String trangThai = (tongTonKho <= 0) ? "Hết hàng hoàn toàn" : "Tồn " + tongTonKho + " SP đã hết hạn";
				ketQua.themVaoDaHet(new ThongTinTonKhoSP(sp, tongTonKho, tongTonHopLe, trangThai));
			}
		}
		return ketQua;
	}
}