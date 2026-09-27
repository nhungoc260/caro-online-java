package caro.database;

import caro.common.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) cho bảng "users".
 * Toàn bộ thao tác đăng ký / đăng nhập / cập nhật thống kê sau trận /
 * bảng xếp hạng đều đi qua class này - không có nơi nào khác trong
 * project được phép chạy SQL trực tiếp lên bảng users.
 */
public class UserDAO {

    /** Rating mặc định khi tạo tài khoản mới. */
    private static final int DEFAULT_RATING = 1000;

    // ---------------------------------------------------------
    // Băm mật khẩu (SHA-256) - không dùng thư viện ngoài để tương
    // thích tuyệt đối với Java 8 + NetBeans 8.0.2 không cần thêm JAR.
    // ---------------------------------------------------------
    private String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            // SHA-256 và UTF-8 luôn có sẵn trên mọi JVM chuẩn -> thực tế không xảy ra
            throw new RuntimeException("Lỗi hệ thống khi băm mật khẩu", e);
        }
    }

    // ---------------------------------------------------------
    // ĐĂNG KÝ
    // ---------------------------------------------------------

    /** @return true nếu username đã tồn tại (không phân biệt hoa thường vì cột dùng collation *_ci) */
    public boolean isUsernameTaken(String username) throws DatabaseConnection.DatabaseException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi kiểm tra username: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo tài khoản mới. KHÔNG kiểm tra trùng username ở đây (caller nên
     * gọi isUsernameTaken trước để hiển thị lỗi sớm); tuy nhiên cột
     * username có UNIQUE KEY ở database nên nếu có race-condition giữa
     * 2 request đăng ký cùng username, MySQL sẽ tự chặn và SQLException
     * (mã lỗi trùng khoá) sẽ được ném ra dưới dạng DatabaseException.
     *
     * @return đối tượng User vừa tạo (đã có id do MySQL tự sinh)
     */
    public User register(String username, String plainPassword, String displayName)
            throws DatabaseConnection.DatabaseException {
        String sql = "INSERT INTO users (username, password, display_name, rating) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hashPassword(plainPassword));
            ps.setString(3, displayName);
            ps.setInt(4, DEFAULT_RATING);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                int newId = keys.next() ? keys.getInt(1) : -1;
                return new User(newId, username, displayName, 0, 0, 0, 0, DEFAULT_RATING);
            }
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new DatabaseConnection.DatabaseException(
                        "Tên đăng nhập \"" + username + "\" đã tồn tại. Vui lòng chọn tên khác.", e);
            }
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi đăng ký tài khoản: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------
    // ĐĂNG NHẬP
    // ---------------------------------------------------------

    /**
     * Kiểm tra username/password. Trả về User nếu đúng, null nếu sai
     * username hoặc sai password (không phân biệt 2 trường hợp này khi
     * trả kết quả, để tránh lộ thông tin username nào tồn tại).
     */
    public User authenticate(String username, String plainPassword)
            throws DatabaseConnection.DatabaseException {
        String sql = "SELECT id, username, password, display_name, wins, losses, draws, "
                + "total_games, rating, created_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null; // sai username

                String storedHash = rs.getString("password");
                String inputHash = hashPassword(plainPassword);
                if (!storedHash.equals(inputHash)) return null; // sai password

                User user = mapRow(rs);
                touchLastLogin(user.getId());
                return user;
            }
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi đăng nhập: " + e.getMessage(), e);
        }
    }

    private void touchLastLogin(int userId) throws DatabaseConnection.DatabaseException {
        String sql = "UPDATE users SET last_login = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi cập nhật lần đăng nhập cuối: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------
    // TRUY VẤN
    // ---------------------------------------------------------

    public User getById(int userId) throws DatabaseConnection.DatabaseException {
        String sql = "SELECT id, username, password, display_name, wins, losses, draws, "
                + "total_games, rating, created_at FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi tải thông tin người dùng: " + e.getMessage(), e);
        }
    }

    /** Bảng xếp hạng: sắp xếp theo rating giảm dần, giới hạn số dòng trả về. */
    public List<User> getLeaderboard(int limit) throws DatabaseConnection.DatabaseException {
        String sql = "SELECT id, username, password, display_name, wins, losses, draws, "
                + "total_games, rating, created_at FROM users "
                + "ORDER BY rating DESC, wins DESC LIMIT ?";
        List<User> result = new ArrayList<User>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi tải bảng xếp hạng: " + e.getMessage(), e);
        }
        return result;
    }

    // ---------------------------------------------------------
    // CẬP NHẬT SAU TRẬN ĐẤU
    // ---------------------------------------------------------

    /** Hệ số K dùng cho công thức Elo đơn giản hoá. */
    private static final int RATING_K = 20;

    /**
     * Cập nhật thống kê (wins/losses/draws/total_games) và rating đơn giản
     * kiểu Elo cho 1 người chơi sau khi trận đấu kết thúc.
     *
     * @param userId        id người chơi cần cập nhật
     * @param outcome       "WIN" / "LOSS" / "DRAW"
     * @param opponentRating rating hiện tại của đối thủ (dùng để tính Elo);
     *                       truyền DEFAULT_RATING nếu chơi với AI hoặc không có đối thủ thật
     */
    public void updateStatsAfterMatch(int userId, String outcome, int opponentRating)
            throws DatabaseConnection.DatabaseException {
        String sql = "SELECT rating FROM users WHERE id = ?";
        int currentRating = DEFAULT_RATING;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) currentRating = rs.getInt("rating");
            }
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi đọc rating hiện tại: " + e.getMessage(), e);
        }

        double actualScore; // 1 = thắng, 0.5 = hòa, 0 = thua
        String updateColumn;
        if ("WIN".equals(outcome)) {
            actualScore = 1.0;
            updateColumn = "wins";
        } else if ("DRAW".equals(outcome)) {
            actualScore = 0.5;
            updateColumn = "draws";
        } else {
            actualScore = 0.0;
            updateColumn = "losses";
        }

        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentRating - currentRating) / 400.0));
        int ratingDelta = (int) Math.round(RATING_K * (actualScore - expectedScore));
        int newRating = Math.max(0, currentRating + ratingDelta);

        String update = "UPDATE users SET " + updateColumn + " = " + updateColumn + " + 1, "
                + "total_games = total_games + 1, rating = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setInt(1, newRating);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi cập nhật thống kê sau trận: " + e.getMessage(), e);
        }
    }

    /** Cập nhật trạng thái online (ONLINE/IN_LOBBY/IN_ROOM/PLAYING/OFFLINE). */
    public void updateStatus(int userId, String status) throws DatabaseConnection.DatabaseException {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi cập nhật trạng thái: " + e.getMessage(), e);
        }
    }

    /** Đổi display name (dùng cho màn hình Hồ sơ - Phase sau). */
    public void updateDisplayName(int userId, String newDisplayName)
            throws DatabaseConnection.DatabaseException {
        String sql = "UPDATE users SET display_name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newDisplayName);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi cập nhật tên hiển thị: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setDisplayName(rs.getString("display_name"));
        u.setWins(rs.getInt("wins"));
        u.setLosses(rs.getInt("losses"));
        u.setDraws(rs.getInt("draws"));
        u.setTotalGames(rs.getInt("total_games"));
        u.setRating(rs.getInt("rating"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
