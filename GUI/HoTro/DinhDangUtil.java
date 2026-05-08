package GUI.HoTro;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public class DinhDangUtil {

    // ================== CONSTANT ==================
    private static final Pattern DIACRITIC_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final Locale VI_LOCALE = new Locale("vi", "VN");

    private static NumberFormat getFormatter() {
        return NumberFormat.getInstance(VI_LOCALE);
    }
    
    private static final String[] UNITS = {
            "", "một", "hai", "ba", "bốn",
            "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] GROUPS = {
            "", "nghìn", "triệu", "tỷ"
    };

    // ================== CHUẨN HÓA ==================
    public static String loaiBoDauTiengViet(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        String temp = DIACRITIC_PATTERN.matcher(normalized).replaceAll("");
        return temp.replace('đ', 'd').replace('Đ', 'D');
    }

    // ================== FORMAT ==================
    public static String dinhDangTien(BigDecimal tien) {
        if (tien == null) {
            return "0 ₫";
        }
        return getFormatter().format(tien) + " ₫";
    }

    public static String dinhDangSo(BigDecimal so) {
        if (so == null) return "0 đ";
        return getFormatter().format(so) + " đ";
    }
    // ================== ĐỌC SỐ ==================
    public static String docSoThanhChu(long number) {
        if (number == 0) return "Không đồng.";
        if (number < 0) return "Âm " + docSoThanhChu(Math.abs(number));

        StringBuilder result = new StringBuilder();
        int groupIndex = 0;
        boolean hasNonZeroRight = false; // 🔥 nhớ bên phải đã có số chưa

        while (number > 0) {
            int block = (int) (number % 1000);
            number /= 1000;

            if (block != 0) {
                // Nếu đã có block phía phải → block này là "ở giữa"
                boolean isMiddle = hasNonZeroRight;

                String blockText = docBlock(block, isMiddle);
                String groupName = getGroupName(groupIndex);

                result.insert(0, blockText + " " + groupName + " ");

                hasNonZeroRight = true;
            }

            groupIndex++;
        }

        return capitalize(clean(result.toString()) + " đồng.");
    }
    private static String docBlock(int number, boolean isMiddle) {
        int hundred = number / 100;
        int ten = (number % 100) / 10;
        int unit = number % 10;

        StringBuilder sb = new StringBuilder();

        // ===== HÀNG TRĂM =====
        if (hundred > 0) {
            sb.append(UNITS[hundred]).append(" trăm ");
        } else if (isMiddle && (ten > 0 || unit > 0)) {
            sb.append("không trăm ");
        }

        // ===== HÀNG CHỤC =====
        if (ten > 1) {
            sb.append(UNITS[ten]).append(" mươi ");
        } else if (ten == 1) {
            sb.append("mười ");
        } else if (ten == 0 && unit > 0 && (hundred > 0 || isMiddle)) {
            sb.append("lẻ ");
        }

        // ===== HÀNG ĐƠN VỊ =====
        if (ten > 1 && unit == 1) {
            sb.append("mốt");
        } else if (ten > 0 && unit == 5) {
            sb.append("lăm");
        } else if (unit > 0) {
            sb.append(UNITS[unit]);
        }

        return sb.toString().trim();
    }

    // ================== GROUP NAME ==================
    private static String getGroupName(int index) {
        if (index < GROUPS.length) return GROUPS[index];

        int base = index % 3;
        int level = index / 3;

        String prefix = GROUPS[base];
        StringBuilder suffix = new StringBuilder("tỷ");

        for (int i = 1; i < level; i++) {
            suffix.append(" tỷ");
        }

        return prefix.isEmpty() ? suffix.toString() : prefix + " " + suffix;
    }

    // ================== HELPER ==================
    private static String clean(String str) {
        return str == null ? "" : str.trim().replaceAll("\\s+", " ");
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase(VI_LOCALE) + str.substring(1);
    }
}