package Logic;

import Dao.LoaiCaDAO;
import Data.LoaiCa;

import java.util.List;

public class LoaiCaLogic {
    private LoaiCaDAO dao = LoaiCaDAO.getInstance();

    // ==============================
    // 1. LẤY DANH SÁCH LOẠI CA
    // ==============================
    public List<LoaiCa> layDanhSachLoaiCa() {
        return dao.layDanhSachLoaiCa();
    }

    // ==============================
    // 2. LẤY LOẠI CA THEO MÃ
    // ==============================
    public LoaiCa layLoaiCaTheoMa(String maLoaiCa) throws Exception {
        // Validation: Rào lỗi ở tầng Logic trước khi gọi xuống DAO
        if (maLoaiCa == null || maLoaiCa.trim().isEmpty()) {
            throw new Exception("Mã loại ca không được để trống!");
        }
        
        LoaiCa ca = dao.layLoaiCaTheoMa(maLoaiCa);
        
        if (ca == null) {
            throw new Exception("Không tìm thấy ca làm việc với mã: " + maLoaiCa);
        }
        
        return ca;
    }
    // ========================================================
    // TÌM LOẠI CA THEO MÃ
    // ========================================================
    public Data.LoaiCa timLoaiCaTheoMa(String maLoaiCa) {
        if (maLoaiCa == null || maLoaiCa.isEmpty()) return null;
        
        // Giả sử bạn đang có hàm layDanhSachLoaiCa() gọi từ DAO
        java.util.List<Data.LoaiCa> dsLoaiCa = layDanhSachLoaiCa(); 
        for (Data.LoaiCa lc : dsLoaiCa) {
            if (lc.getMaLoaiCa().equals(maLoaiCa)) {
                return lc;
            }
        }
        return null;
    }
}