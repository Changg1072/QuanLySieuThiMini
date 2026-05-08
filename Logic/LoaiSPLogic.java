package Logic;

import Dao.LoaiSPDAO;
import Data.LoaiSP;

import java.util.List;

public class LoaiSPLogic {
    private LoaiSPDAO dao = LoaiSPDAO.getInstance();

    // ==============================
    // 1. LẤY DANH SÁCH LOẠI SẢN PHẨM
    // ==============================
    public List<LoaiSP> layDanhSachLoaiSP() {
        return dao.layDanhSachLoaiSP();
    }

    // ==============================
    // 2. TÌM LOẠI SẢN PHẨM THEO MÃ
    // ==============================
    public LoaiSP timLoaiTheoMa(String maLoai) throws Exception {
        // Rào lỗi ngay tại tầng Logic
        if (maLoai == null || maLoai.trim().isEmpty()) {
            throw new Exception("Lỗi: Mã loại sản phẩm không được để trống!");
        }
        
        LoaiSP loaiSP = dao.layLoaiSPTheoMa(maLoai);
        
        if (loaiSP == null) {
            throw new Exception("Không tìm thấy Loại sản phẩm nào mang mã: " + maLoai);
        }
        
        return loaiSP;
    }
}