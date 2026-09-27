package caro.database;

/**
 * Nơi cấu hình DUY NHẤT cho kết nối MySQL.
 *
 * Toàn bộ project chỉ đọc thông tin kết nối từ đây - không hard-code
 * URL/USER/PASSWORD ở bất kỳ file nào khác.
 *
 * Môi trường mặc định: WampServer (Apache + MySQL 5.7.44 + phpMyAdmin 5.2.3),
 * MySQL chạy local tại 127.0.0.1:3306, user mặc định "root" không mật khẩu.
 *
 * Nếu máy bạn có mật khẩu MySQL khác, hoặc chạy XAMPP/MySQL Workbench...
 * thì CHỈ CẦN SỬA 3 DÒNG DƯỚI ĐÂY.
 */
public class DatabaseConfig {

    /** Tên database (phải khớp với CREATE DATABASE trong database/caro_online.sql) */
    public static final String DB_NAME = "caro_online";

    /** Host + port MySQL. WampServer mặc định chạy tại 127.0.0.1:3306 */
    public static final String DB_HOST = "127.0.0.1";
    public static final int DB_PORT = 3306;

    /**
     * JDBC URL.
     * useSSL=false        : MySQL local không cần SSL, tránh warning/lỗi handshake
     * serverTimezone=UTC  : tránh lỗi "The server time zone value is unrecognized"
     * useUnicode + characterEncoding=UTF-8 : hiển thị đúng tiếng Việt có dấu
     * allowPublicKeyRetrieval=true : cần thiết với driver mysql-connector-java 8.x
     *                                khi dùng caching_sha2_password (mặc định MySQL 8),
     *                                không ảnh hưởng gì khi dùng MySQL 5.7.
     */
    public static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&useSSL=false"
                    + "&serverTimezone=UTC"
                    + "&allowPublicKeyRetrieval=true";

    /** Tài khoản MySQL. WampServer mặc định: user "root", password rỗng "" */
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "";

    /**
     * Tên class Driver JDBC.
     * - mysql-connector-java 8.x (khuyên dùng, tương thích Java 8 + MySQL 5.7):
     *       "com.mysql.cj.jdbc.Driver"
     * - mysql-connector-java 5.1.x (bản cũ hơn):
     *       "com.mysql.jdbc.Driver"
     * Từ Java 6 trở lên, driver JDBC 4.0+ tự đăng ký (không bắt buộc gọi
     * Class.forName), nhưng NetBeans 8.0.2 / Ant cũ đôi khi cần gọi tường minh
     * nên DatabaseConnection vẫn gọi Class.forName cho chắc chắn.
     */
    public static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private DatabaseConfig() {
        // Không khởi tạo - class chỉ chứa hằng số cấu hình
    }
}
