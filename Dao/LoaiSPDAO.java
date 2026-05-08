package Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Data.LoaiSP;

public class LoaiSPDAO {
    
    // Eager Initialization: Khởi tạo Singleton an toàn
    private static final LoaiSPDAO instance = new LoaiSPDAO();

    private LoaiSPDAO() {
    }

    public static LoaiSPDAO getInstance() {
        return instance;
    }

    // ==============================
    // HELPER: MAP RESULTSET → OBJECT
    // ==============================
    private LoaiSP mapResultSetToLoaiSP(ResultSet rs) throws SQLException {
        return new LoaiSP.ThoXayLoaiSP()
                .ganMaLoai(rs.getString("MaLoai"))
                .ganTenLoai(rs.getString("TenLoai"))
                .taoMoi();
    }


    // ==============================
    // 1. LẤY DANH SÁCH 11 LOẠI SẢN PHẨM MẶC ĐỊNH
    // ==============================
    public List<LoaiSP> layDanhSachLoaiSP() {
        List<LoaiSP> dsLoai = new ArrayList<>();
        String sql = "SELECT * FROM LoaiSP";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql); 
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsLoai.add(mapResultSetToLoaiSP(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachLoaiSP", e);
        }
        return dsLoai;
    }

    // ==============================
    // 2. LẤY LOẠI SẢN PHẨM THEO MÃ
    // ==============================
    public LoaiSP layLoaiSPTheoMa(String maLoai) {
        String sql = "SELECT * FROM LoaiSP WHERE MaLoai = ?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maLoai);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLoaiSP(rs);
                }
            }
        } catch (SQLException e) {
            logError("layLoaiSPTheoMa", e);
        }
        return null;
    }
    // ==============================
    // LOG LỖI CHUYÊN NGHIỆP
    // ==============================
    private void logError(String method, Exception e) {
        System.err.println("[LoaiSPDAO - " + method + "] ERROR: " + e.getMessage());
    }

}