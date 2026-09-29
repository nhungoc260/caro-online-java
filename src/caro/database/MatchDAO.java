package caro.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng "matches".
 * Vòng đời 1 trận đấu trong database:
 *   1. createMatch(...)  - gọi ngay khi GameRoom.startGame() (server)
 *   2. (nhiều lần) MoveDAO.saveMove(...) - mỗi nước đi
 *   3. finishMatch(...)  - gọi khi có WIN / DRAW / ABANDONED (đầu hàng, disconnect)
 * Toàn bộ do SERVER gọi - client không bao giờ được ghi trực tiếp vào bảng này.
 */
public class MatchDAO {

    /** Đại diện 1 dòng lịch sử trận đấu để hiển thị lên HistoryFrame. */
    public static class MatchHistoryItem {
        public int matchId;
        public String opponentName; // "AI" nếu mode = AI
        public String mode;         // ONLINE / AI
        public String result;       // WIN / LOSS / DRAW / ABANDONED (theo góc nhìn của user đang xem)
        public java.util.Date startedAt;
        public Integer durationSeconds;
    }

    /**
     * Tạo 1 bản ghi trận đấu mới, trạng thái coi như "đang diễn ra"
     * (ended_at = NULL cho tới khi finishMatch được gọi).
     *
     * @param player2Id truyền null nếu mode = AI (không có đối thủ là user thật)
     * @return id của match vừa tạo, dùng để gắn vào các nước đi (MoveDAO)
     */
    public int createMatch(int player1Id, Integer player2Id, String mode, String roomCode)
            throws DatabaseConnection.DatabaseException {
        return createMatch(player1Id, player2Id, mode, roomCode, null);
    }

    /** aiDifficulty: EASY / MEDIUM / HARD khi mode = AI, null khi đấu online. */
    public int createMatch(int player1Id, Integer player2Id, String mode, String roomCode,
                           String aiDifficulty)
            throws DatabaseConnection.DatabaseException {
        String sql = "INSERT INTO matches (player1_id, player2_id, result, mode, room_code, started_at, ai_difficulty) "
                + "VALUES (?, ?, 'ABANDONED', ?, ?, ?, ?)";
        // result tạm để 'ABANDONED' cho tới khi finishMatch cập nhật giá trị thật;
        // nếu server bị tắt đột ngột giữa chừng, bản ghi vẫn phản ánh đúng "trận bỏ dở".
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, player1Id);
            if (player2Id != null) {
                ps.setInt(2, player2Id);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, mode);
            ps.setString(4, roomCode);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            if (aiDifficulty != null) {
                ps.setString(6, aiDifficulty);
            } else {
                ps.setNull(6, java.sql.Types.VARCHAR);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi tạo bản ghi trận đấu: " + e.getMessage(), e);
        }
    }

    /**
     * Đóng 1 trận đấu: ghi kết quả cuối cùng, người thắng, thời gian kết thúc.
     *
     * @param winnerId  null nếu hòa hoặc không xác định được người thắng
     * @param result    "WIN" (có người thắng rõ ràng) / "DRAW" / "ABANDONED" (có người thoát/disconnect)
     */
    public void finishMatch(int matchId, Integer winnerId, String result, int durationSeconds)
            throws DatabaseConnection.DatabaseException {
        String sql = "UPDATE matches SET winner_id = ?, result = ?, ended_at = ?, "
                + "duration_seconds = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (winnerId != null) {
                ps.setInt(1, winnerId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, result);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setInt(4, durationSeconds);
            ps.setInt(5, matchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi lưu kết quả trận đấu: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy lịch sử trận đấu của 1 người chơi, mới nhất trước.
     * "opponentName" trả về display_name của đối thủ, hoặc "AI" nếu mode = AI.
     */
    public List<MatchHistoryItem> getHistoryForUser(int userId, int limit)
            throws DatabaseConnection.DatabaseException {
        String sql = "SELECT m.id, m.mode, m.result, m.winner_id, m.started_at, "
                + "m.duration_seconds, m.player1_id, m.player2_id, m.ai_difficulty, "
                + "u.display_name AS opponent_name "
                + "FROM matches m "
                + "LEFT JOIN users u ON u.id = (CASE WHEN m.player1_id = ? THEN m.player2_id ELSE m.player1_id END) "
                + "WHERE m.player1_id = ? OR m.player2_id = ? "
                + "ORDER BY m.started_at DESC LIMIT ?";
        List<MatchHistoryItem> list = new ArrayList<MatchHistoryItem>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatchHistoryItem item = new MatchHistoryItem();
                    item.matchId = rs.getInt("id");
                    item.mode = rs.getString("mode");
                    item.startedAt = rs.getTimestamp("started_at");
                    int durationSec = rs.getInt("duration_seconds");
                    item.durationSeconds = rs.wasNull() ? null : durationSec;

                    if ("AI".equals(item.mode)) {
                        String diff = rs.getString("ai_difficulty");
                        String label = "EASY".equals(diff) ? " (Dễ)"
                                : "MEDIUM".equals(diff) ? " (Trung bình)"
                                : "HARD".equals(diff) ? " (Khó)" : "";
                        item.opponentName = "Máy AI" + label;
                    } else {
                        String name = rs.getString("opponent_name");
                        item.opponentName = (name != null) ? name : "(tài khoản đã xóa)";
                    }

                    String rawResult = rs.getString("result");
                    int winnerIdValue = rs.getInt("winner_id");
                    Integer winnerId = rs.wasNull() ? null : winnerIdValue;
                    if ("DRAW".equals(rawResult)) {
                        item.result = "DRAW";
                    } else if ("ABANDONED".equals(rawResult)) {
                        item.result = "ABANDONED";
                    } else if (winnerId != null && winnerId == userId) {
                        item.result = "WIN";
                    } else {
                        item.result = "LOSS";
                    }
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi tải lịch sử trận đấu: " + e.getMessage(), e);
        }
        return list;
    }
}
