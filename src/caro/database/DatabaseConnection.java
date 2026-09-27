package caro.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý việc tạo kết nối JDBC tới MySQL.
 *
 * Toàn bộ DAO (UserDAO, MatchDAO, MoveDAO...) đều gọi
 * DatabaseConnection.getConnection() để lấy 1 Connection MỚI cho mỗi
 * thao tác, dùng xong đóng lại ngay (try-with-resources) - đơn giản,
 * an toàn cho nhiều Thread (mỗi ClientHandler chạy 1 Thread riêng) vì
 * không có Connection nào bị 2 Thread dùng chung cùng lúc.
 *
 * KHÔNG dùng Connection Pool phức tạp (HikariCP, DBCP...) vì:
 *  - project ưu tiên đơn giản, dễ hiểu, phù hợp sinh viên
 *  - không dùng framework nặng
 *  - số lượng client demo (LAN/localhost) nhỏ, mở/đóng Connection trực
 *    tiếp là đủ dùng và dễ debug.
 */
public class DatabaseConnection {

    private static volatile boolean driverChecked = false;
    private static volatile boolean driverAvailable = false;

    /**
     * Nạp Driver JDBC (chỉ cần chạy 1 lần cho toàn ứng dụng).
     * Nếu thiếu JAR "mysql-connector-java" trong Project Libraries của
     * NetBeans, bước này sẽ ném DatabaseException với hướng dẫn rõ ràng.
     */
    private static synchronized void ensureDriverLoaded() throws DatabaseException {
        if (driverChecked) {
            if (!driverAvailable) {
                throw new DatabaseException(
                        "Thiếu MySQL JDBC Driver trong project.\n"
                        + "Cách khắc phục:\n"
                        + "  1. Tải file mysql-connector-java-8.0.x.jar\n"
                        + "  2. NetBeans -> chuột phải Project \"CaroOnline\" -> Properties\n"
                        + "  3. Libraries -> Compile -> Add JAR/Folder -> chọn file .jar vừa tải\n"
                        + "  4. Nhấn OK và Run lại project.");
            }
            return;
        }
        try {
            Class.forName(DatabaseConfig.DRIVER_CLASS);
            driverAvailable = true;
        } catch (ClassNotFoundException e) {
            driverAvailable = false;
        } finally {
            driverChecked = true;
        }
        if (!driverAvailable) {
            throw new DatabaseException(
                    "Thiếu MySQL JDBC Driver trong project.\n"
                    + "Cách khắc phục:\n"
                    + "  1. Tải file mysql-connector-java-8.0.x.jar\n"
                    + "  2. NetBeans -> chuột phải Project \"CaroOnline\" -> Properties\n"
                    + "  3. Libraries -> Compile -> Add JAR/Folder -> chọn file .jar vừa tải\n"
                    + "  4. Nhấn OK và Run lại project.");
        }
    }

    /**
     * Mở 1 Connection mới tới MySQL.
     * Gọi hàm này trong try-with-resources ở tầng DAO, ví dụ:
     *
     *   try (Connection conn = DatabaseConnection.getConnection()) {
     *       ...
     *   }
     *
     * @throws DatabaseException nếu thiếu driver, sai cấu hình, hoặc
     *         MySQL/WampServer chưa được khởi động - kèm thông báo dễ hiểu
     *         để hiển thị lên giao diện cho người dùng cuối.
     */
    public static Connection getConnection() throws DatabaseException {
        ensureDriverLoaded();
        try {
            return DriverManager.getConnection(
                    DatabaseConfig.DB_URL,
                    DatabaseConfig.DB_USER,
                    DatabaseConfig.DB_PASSWORD);
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Không thể kết nối tới cơ sở dữ liệu MySQL.\n"
                    + "Kiểm tra lại:\n"
                    + "  1. WampServer đã bật (icon màu xanh lá) chưa?\n"
                    + "  2. MySQL service đã Start chưa (WampServer -> MySQL -> "
                    + "Start/Resume Service)?\n"
                    + "  3. Database \"" + DatabaseConfig.DB_NAME + "\" đã được tạo "
                    + "bằng file database/caro_online.sql chưa?\n"
                    + "  4. Thông tin kết nối trong DatabaseConfig.java (host/port/"
                    + "user/password) có đúng với máy bạn không?\n\n"
                    + "Chi tiết lỗi kỹ thuật: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra nhanh xem có kết nối được database hay không - dùng khi
     * CaroServer khởi động để cảnh báo sớm thay vì để lỗi rơi vãi khi
     * client đã đăng nhập vào giữa chừng.
     */
    public static boolean testConnection() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private DatabaseConnection() {
    }

    /**
     * Exception riêng cho tầng database, bọc lại SQLException/
     * ClassNotFoundException thành thông báo tiếng Việt dễ hiểu, để
     * tầng UI/Server chỉ cần bắt 1 loại exception và hiển thị message
     * trực tiếp cho người dùng, không bao giờ để lộ stack trace ra GUI.
     */
    public static class DatabaseException extends Exception {
        public DatabaseException(String message) {
            super(message);
        }

        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
