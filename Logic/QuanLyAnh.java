package Logic;

import java.awt.Image;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

public class QuanLyAnh {

    // Lấy đường dẫn thư mục gốc của project hiện tại và thêm thư mục "images"
    private static final String THU_MUC_ANH = System.getProperty("user.dir") + File.separator + "images";
    
    // BỘ NHỚ ĐỆM (CACHE) SIÊU TỐC ĐỘ: Lưu lại các ảnh đã bóp nhỏ để JTable cuộn mượt mà
    private static Map<String, ImageIcon> cacheAnh = new HashMap<>();

    public static String copyAnhVaoProject(String duongDanGoc) {
        // 1. Kiểm tra chuỗi rỗng
        if (duongDanGoc == null || duongDanGoc.trim().isEmpty()) {
            return ""; 
        }

        File fileGoc = new File(duongDanGoc);

        // 2. Nếu chuỗi truyền vào chỉ là tên file
        if (!fileGoc.isAbsolute()) {
            return fileGoc.getName();
        }

        // 3. Nếu là đường dẫn tuyệt đối nhưng file không tồn tại
        if (!fileGoc.exists()) {
            return fileGoc.getName(); 
        }

        try {
            // 4. Tạo thư mục 'images' nếu chưa có
            File thuMuc = new File(THU_MUC_ANH);
            if (!thuMuc.exists()) {
                thuMuc.mkdirs(); 
            }

            // 5. Copy đè file nếu đã tồn tại
            String tenFile = fileGoc.getName();
            Path duongDanDich = Paths.get(THU_MUC_ANH, tenFile);
            Files.copy(fileGoc.toPath(), duongDanDich, StandardCopyOption.REPLACE_EXISTING);

            return tenFile; 

        } catch (Exception e) {
            e.printStackTrace();
            return new File(duongDanGoc).getName(); // Fallback an toàn
        }
    }

    // =========================================================================
    // HÀM MỚI BỔ SUNG: CHUYÊN LẤY ẢNH VÀ XỬ LÝ KÍCH THƯỚC TRẢ CHO GIAO DIỆN
    // =========================================================================
    public static ImageIcon layIconAnh(String tenFileAnh, int chieuRong, int chieuCao) {
        if (tenFileAnh == null || tenFileAnh.trim().isEmpty()) {
            return null; // Có thể sau này sếp trả về 1 cái ảnh "no-image.jpg" mặc định ở đây
        }

        // Tạo khóa (key) cho Cache (VD: "sanpham1.jpg_70x70")
        String cacheKey = tenFileAnh + "_" + chieuRong + "x" + chieuCao;

        // Nếu trong Cache đã có ảnh kích thước này rồi -> Trả về ngay lập tức (Rất nhanh!)
        if (cacheAnh.containsKey(cacheKey)) {
            return cacheAnh.get(cacheKey);
        }

        // Nếu chưa có, tiến hành vào ổ cứng lấy ảnh và bóp nhỏ lại
        File fileAnh = new File(THU_MUC_ANH + File.separator + tenFileAnh);
        if (fileAnh.exists()) {
            ImageIcon iconGoc = new ImageIcon(fileAnh.getAbsolutePath());
            Image imgScale = iconGoc.getImage().getScaledInstance(chieuRong, chieuCao, Image.SCALE_SMOOTH);
            ImageIcon iconDaXuLy = new ImageIcon(imgScale);
            
            // Lưu vào bộ nhớ đệm để lần sau dùng lại
            cacheAnh.put(cacheKey, iconDaXuLy);
            return iconDaXuLy;
        }

        return null; // Không tìm thấy file
    }
}