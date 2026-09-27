package caro.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng "match_moves".
 * Lưu lại toàn bộ nước đi của 1 trận, phục vụ:
 *  - Kiểm tra/đối chiếu (chống gian lận) nếu cần sau này
 *  - Xem lại trận đấu (replay) ở màn hình chi tiết lịch sử
 */
public class MoveDAO {

    /** Đại diện 1 nước đi đã lưu, dùng khi tải lại để "xem lại trận". */
    public static class MoveRecord {
        public int playerId;
        public int x;
        public int y;
        public int moveNumber;
    }

    /**
     * Lưu 1 nước đi. Gọi ngay sau khi server chấp nhận nước đi hợp lệ
     * (trong GameRoom.applyMove), KHÔNG chờ tới cuối trận mới lưu hàng
     * loạt, để không mất dữ liệu nếu server crash giữa chừng.
     */
    public void saveMove(int matchId, int playerId, int x, int y, int moveNumber)
            throws DatabaseConnection.DatabaseException {
        String sql = "INSERT INTO match_moves (match_id, player_id, x, y, move_number) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, playerId);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, moveNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Lỗi lưu 1 nước đi không nên làm sập cả ván đang chơi -> server
            // (ClientHandler/GameRoom) nên chỉ log lỗi này ra console, KHÔNG
            // ném ngược lên làm gián đoạn gameplay thời gian thực.
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi lưu nước đi: " + e.getMessage(), e);
        }
    }

    /** Lấy toàn bộ nước đi của 1 trận, theo đúng thứ tự đã đánh - dùng để xem lại trận. */
    public List<MoveRecord> getMovesByMatch(int matchId) throws DatabaseConnection.DatabaseException {
        String sql = "SELECT player_id, x, y, move_number FROM match_moves "
                + "WHERE match_id = ? ORDER BY move_number ASC";
        List<MoveRecord> list = new ArrayList<MoveRecord>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MoveRecord m = new MoveRecord();
                    m.playerId = rs.getInt("player_id");
                    m.x = rs.getInt("x");
                    m.y = rs.getInt("y");
                    m.moveNumber = rs.getInt("move_number");
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnection.DatabaseException(
                    "Lỗi khi tải lại nước đi của trận đấu: " + e.getMessage(), e);
        }
        return list;
    }
}
