package Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Data.NhaCungCap;

public class NhaCungCapDAO {
    
    private static final NhaCungCapDAO instance = new NhaCungCapDAO();

    private NhaCungCapDAO() {
    }

    public static NhaCungCapDAO getInstance() {
        return instance;
    }

    // ==============================
    // 1. LẤY DANH SÁCH NHÀ CUNG CẤP
    // ==============================
    public List<NhaCungCap> layDanhSachNhaCungCap() {
        List<NhaCungCap> dsNhaCungCap = new ArrayList<>();
        String sql = "SELECT * FROM NhaCungCap";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                dsNhaCungCap.add(mapResultSetToNhaCungCap(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachNhaCungCap", e);
        }
        return dsNhaCungCap;
    }

    // ==============================
    // 2. THÊM MỘT NHÀ CUNG CẤP MỚI
    // ==============================
    public boolean themNhaCungCap(NhaCungCap ncc) {
        String sql = "INSERT INTO NhaCungCap(MaNCC, TenNCC, SDT, Email, DiaChi, TrangThai) VALUES (?, ?, ?, ?, ?, N'Đang hợp tác')";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, ncc.getMaNCC());
            pstmt.setString(2, ncc.getTenNCC());
            pstmt.setString(3, ncc.getSDT());
            pstmt.setString(4, ncc.getEmail());
            pstmt.setString(5, ncc.getDiaChi());

            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logError("themNhaCungCap", e);
        }
        return false;
    }

    // ==============================
    // 3. CẬP NHẬT THÔNG TIN
    // ==============================
    public boolean suaNhaCungCap(NhaCungCap ncc) {
        String sql = "UPDATE NhaCungCap SET TenNCC = ?, SDT = ?, Email = ?, DiaChi = ? WHERE MaNCC = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, ncc.getTenNCC());
            pstmt.setString(2, ncc.getSDT());
            pstmt.setString(3, ncc.getEmail());
            pstmt.setString(4, ncc.getDiaChi());
            pstmt.setString(5, ncc.getMaNCC());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("suaNhaCungCap", e);
        }
        return false;
    }

    // ==============================
    // 4. XÓA NHÀ CUNG CẤP (XÓA MỀM)
    // ==============================
    public boolean xoaNhaCungCap(String maNCC) {
        String sql = "UPDATE NhaCungCap SET TrangThai = N'Ngừng hợp tác' WHERE MaNCC = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maNCC);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("xoaNhaCungCap", e);
        }
        return false;
    }

    // ==============================
    // 5. LẤY NHÀ CUNG CẤP THEO TÊN
    // ==============================
    public NhaCungCap layNhaCungCapTheoTen(String tenNCC) {
        String sql = "SELECT * FROM NhaCungCap WHERE TenNCC = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, tenNCC);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToNhaCungCap(rs);
                }
            }
        } catch (SQLException e) { 
            logError("layNhaCungCapTheoTen", e); 
        }
        return null; 
    }

    // ==============================
    // 6. KIỂM TRA TRÙNG MÃ 
    // ==============================
    public boolean kiemTraTrungMaNCC(String maNCC) {
        String sql = "SELECT COUNT(*) FROM NhaCungCap WHERE MaNCC = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maNCC);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { 
            logError("kiemTraTrungMaNCC", e); 
        }
        return false;
    }

    // ==============================
    // 7. LẤY NHÀ CUNG CẤP THEO MÃ 
    // ==============================
    public NhaCungCap layNhaCungCapTheoMa(String maNCC) {
        String sql = "SELECT * FROM NhaCungCap WHERE MaNCC = ?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)
        ) {
            pstmt.setString(1, maNCC);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToNhaCungCap(rs);
                }
            }
        } catch (SQLException e) { 
            logError("layNhaCungCapTheoMa", e); 
        }
        return null;
    }

    // =========================================================
    // HELPER: MAP RESULTSET → OBJECT
    // =========================================================
    private NhaCungCap mapResultSetToNhaCungCap(ResultSet rs) throws SQLException {
        return new NhaCungCap.ThoXayNhaCungCap()
                .ganMaNCC(rs.getString("MaNCC"))
                .ganTenNCC(rs.getString("TenNCC"))
                .ganSDT(rs.getString("SDT"))
                .ganEmail(rs.getString("Email"))
                .ganDiaChi(rs.getString("DiaChi"))
                .ganTrangThai(rs.getString("TrangThai"))
                .taoMoi();
    }

    private void logError(String method, Exception e) {
        System.err.println("[NhaCungCapDAO - " + method + "] ERROR: " + e.getMessage());
    }
}