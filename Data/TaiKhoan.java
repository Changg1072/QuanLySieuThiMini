package Data;

public class TaiKhoan {
	private String MaNV;
	private String TaiKhoan;
	private String MatKhau;

	private TaiKhoan(ThoXayTaiKhoan builder) {
		this.MaNV = builder.maNV;
		this.TaiKhoan = builder.taiKhoan;
		this.MatKhau = builder.matKhau;
	}

	public TaiKhoan() {}
	public String getMaNV() { return MaNV; }
	public void setMaNV(String maNV) { MaNV = maNV; }
	public String getTaiKhoan() { return TaiKhoan; }
	public void setTaiKhoan(String taiKhoan) { TaiKhoan = taiKhoan; }
	public String getMatKhau() { return MatKhau; }
	public void setMatKhau(String matKhau) { MatKhau = matKhau; }

	public static class ThoXayTaiKhoan {
		private String maNV;
		private String taiKhoan;
		private String matKhau;

		public ThoXayTaiKhoan ganMaNV(String maNV) { this.maNV = maNV; return this; }
		public ThoXayTaiKhoan ganTaiKhoan(String taiKhoan) { this.taiKhoan = taiKhoan; return this; }
		public ThoXayTaiKhoan ganMatKhau(String matKhau) { this.matKhau = matKhau; return this; }

		public TaiKhoan taoMoi() { return new TaiKhoan(this); }
	}
}