package Dao;

import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Data.SanPham;

public class SanPhamDAO {
    
    private static final SanPhamDAO instance = new SanPhamDAO();

    private SanPhamDAO() {}

    public static SanPhamDAO getInstance() {
        return instance;
    }

    // ==============================
    // 1. LẤY DANH SÁCH SẢN PHẨM
    // ==============================
    public List<SanPham> layDanhSachSanPham() {
        List<SanPham> list = new ArrayList<>();
        String sql = "SELECT * FROM SanPham";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                list.add(mapResultSetToSanPham(rs));
            }
        } catch (SQLException e) {
            logError("layDanhSachSanPham", e);
        }

        return list;
    }

    // ==============================
    // 2. THÊM SẢN PHẨM MỚI
    // ==============================
    public boolean themSanPham(SanPham sp) {
        String sql = "INSERT INTO SanPham (MaSP, TenSP, LinkHinhAnh, MaLoai, GiaBan, DonViTinh) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, sp.getMaSP());
            ps.setString(2, sp.getTenSP());
            
            String tenAnhGoc = layTenFileAnh(sp.getLinkHinhAnh());
            ps.setString(3, tenAnhGoc);
            
            ps.setString(4, sp.getMaLoai());
            ps.setBigDecimal(5, sp.getGiaBan());
            ps.setString(6, sp.getDonViTinh());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("themSanPham", e);
        }
        return false;
    }

    // ==============================
    // 3. SỬA THÔNG TIN SẢN PHẨM
    // ==============================
    public boolean suaSanPham(SanPham sp) {
        String sql = "UPDATE SanPham SET TenSP=?, LinkHinhAnh=?, MaLoai=?, GiaBan=?, DonViTinh=? WHERE MaSP=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, sp.getTenSP());
            
            String tenAnhGoc = layTenFileAnh(sp.getLinkHinhAnh());
            ps.setString(2, tenAnhGoc);
            
            ps.setString(3, sp.getMaLoai());
            ps.setBigDecimal(4, sp.getGiaBan()); 
            ps.setString(5, sp.getDonViTinh());
            
            ps.setString(6, sp.getMaSP());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("suaSanPham", e);
        }
        return false;
    }

    // ==============================
    // 4. XÓA SẢN PHẨM
    // ==============================
    public boolean xoaSanPham(String maSP) {
        String sql = "DELETE FROM SanPham WHERE MaSP=?";

        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maSP);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("xoaSanPham", e);
        }
        return false;
    }

    // ==============================
    // 5. LẤY SẢN PHẨM THEO MÃ
    // ==============================
    public SanPham laySanPhamTheoMa(String maSP) {
        String sql = "SELECT * FROM SanPham WHERE MaSP=?";
        
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, maSP);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSanPham(rs);
                }
            }

        } catch (SQLException e) {
            logError("laySanPhamTheoMa", e);
        }

        return null;
    }

 // ==============================
    // HÀM TỐI ƯU: LẤY SẢN PHẨM THEO TÊN
    // ==============================
    public SanPham laySanPhamTheoTen(String tenSP) {
        String sql = "SELECT * FROM SanPham WHERE TenSP=?";
        try (
            Connection con = ConnectDB.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, tenSP);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSanPham(rs);
                }
            }
        } catch (SQLException e) {
            logError("laySanPhamTheoTen", e);
        }
        return null;
    }
    // =========================================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS)
    // =========================================================

    private String layTenFileAnh(String duongDan) {
        if (duongDan == null || duongDan.trim().isEmpty()) {
            return null;
        }
        File file = new File(duongDan);
        return file.getName();
    }

    private SanPham mapResultSetToSanPham(ResultSet rs) throws SQLException {
        String maSP = rs.getString("MaSP");
        String tenSP = rs.getString("TenSP");
        String linkHinhAnh = rs.getString("LinkHinhAnh");
        String maLoai = rs.getString("MaLoai");
        BigDecimal giaBan = rs.getBigDecimal("GiaBan");
        String donViTinh = rs.getString("DonViTinh");

        return new SanPham.ThoXaySanPham()
                .ganMaSP(maSP)
                .ganTenSP(tenSP)
                .ganLinkHinhAnh(linkHinhAnh)
                .ganMaLoai(maLoai)
                .ganGiaBan(giaBan)
                .ganDonViTinh(donViTinh)
                .taoMoi();
    }

    private void logError(String method, Exception e) {
        System.err.println("[SanPhamDAO - " + method + "] ERROR: " + e.getMessage());
    }
}