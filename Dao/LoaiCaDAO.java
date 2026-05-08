package Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import Data.LoaiCa;

public class LoaiCaDAO {

    // Eager Initialization: Khởi tạo Singleton an toàn
    private static final LoaiCaDAO instance = new LoaiCaDAO();

    private LoaiCaDAO() {
    }

    public static LoaiCaDAO getInstance() {
        return instance;
    }

    // ==============================
    // HELPER: MAP RESULTSET → OBJECT
    // ==============================
    private LoaiCa mapResultSetToLoaiCa(ResultSet rs) throws SQLException {
        // Ép kiểu an toàn từ SQL Time sang LocalTime của Java
        Time sqlBatDau = rs.getTime("GioBatDau");
        LocalTime gioBatDau = (sqlBatDau != null) ? sqlBatDau.toLocalTime() : null;

        Time sqlKetThuc = rs.getTime("GioKetThuc");
        LocalTime gioKetThuc = (sqlKetThuc != null) ? sqlKetThuc.toLocalTime() : null;

        return new LoaiCa.ThoXayLoaiCa()
                .ganMaLoaiCa(rs.getString("MaLoaiCa"))
                .ganTenCa(rs.getString("TenCa"))
                .ganGioBatDau(gioBatDau)
                .ganGioKetThuc(gioKetThuc)
                .taoMoi();
    }

    // ==============================
    // LOG LỖI CHUYÊN NGHIỆP
    // ==============================
    private void logError(String method, Exception e) {
        System.err.println("[LoaiCaDAO - " + method + "] ERROR: " + e.getMessage());
    }

    // ==============================
    // 1. LẤY DANH SÁCH LOẠI CA
    // ==============================
    public List<LoaiCa> layDanhSachLoaiCa() {
        List<LoaiCa> dsLoaiCa = new ArrayList<>();
        String sql = "SELECT * FROM LoaiCa";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql); 
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsLoaiCa.add(mapResultSetToLoaiCa(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachLoaiCa", e);
        }
        return dsLoaiCa;
    }

    // ==============================
    // 2. LẤY LOẠI CA THEO MÃ
    // ==============================
    public LoaiCa layLoaiCaTheoMa(String maLoaiCa) {
        String sql = "SELECT * FROM LoaiCa WHERE MaLoaiCa = ?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maLoaiCa);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLoaiCa(rs);
                }
            }
        } catch (SQLException e) {
            logError("layLoaiCaTheoMa", e);
        }
        return null;
    }

}