package caro.server;

import caro.common.MatchSummary;
import caro.common.Message;
import caro.common.User;
import caro.database.DatabaseConnection;
import caro.database.MatchDAO;
import caro.database.UserDAO;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mỗi client kết nối vào server sẽ được gán 1 ClientHandler,
 * chạy trên 1 Thread riêng (đáp ứng yêu cầu "TCP + Thread").
 *
 * PHASE 4/5: Khác với bản gốc (chỉ xử lý MOVE/REPLAY/CHAT trong 1 GameRoom
 * cố định), ClientHandler giờ xử lý CẢ giai đoạn "ở Lobby" (tạo/tham gia
 * phòng, matchmaking, xem bảng xếp hạng/lịch sử) LẪN giai đoạn "đang chơi"
 * (room != null) - tất cả trên CÙNG 1 kết nối, CÙNG 1 vòng lặp đọc Message,
 * y hệt tinh thần code gốc, chỉ mở rộng thêm case trong switch.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int playerId;
    private GameRoom room;
    private String playerName = "Người chơi";

    // ==== PHASE 4: thông tin tài khoản đã đăng nhập (null nếu là kết nối
    // "vô danh" chạy CaroClient.java trực tiếp không qua LoginFrame) ====
    private Integer userId;
    private int rating = 1000;

    // ==== PHASE 5: mã phòng hiện đang đứng chờ (chỉ có giá trị khi ở trạng
    // thái WAITING trong 1 phòng do chính mình tạo) ====
    private String currentRoomCode;

    private final UserDAO userDAO = new UserDAO();
    private final MatchDAO matchDAO = new MatchDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void setPlayerId(int id) {
        this.playerId = id;
        // Chỉ đặt tên mặc định "Người chơi N" nếu CHƯA có tên thật nào được
        // set từ trước (qua NAME/đăng nhập) - tránh việc gán playerId lúc
        // ghép phòng (Phase 5) đè mất tên thật đã có.
        if (this.playerName == null || this.playerName.equals("Người chơi")) {
            this.playerName = "Người chơi " + id;
        }
    }

    public void setRoom(GameRoom room) { this.room = room; }
    public String getPlayerName() { return playerName; }
    public Integer getUserId() { return userId; }
    public int getRating() { return rating; }
    public void setCurrentRoomCode(String code) { this.currentRoomCode = code; }
    public String getCurrentRoomCode() { return currentRoomCode; }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[Server] Client kết nối: " + socket.getInetAddress());

            Message msg;
            while ((msg = (Message) in.readObject()) != null) {
                handle(msg);
            }
        } catch (EOFException | SocketException e) {
            System.out.println("[Server] " + playerName + " đã ngắt kết nối.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (room != null) room.notifyOpponentLeft(this);
            RoomManager.INSTANCE.handleDisconnect(this);
            if (userId != null) {
                try {
                    userDAO.updateStatus(userId, "OFFLINE");
                } catch (DatabaseConnection.DatabaseException ignored) {
                }
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handle(Message msg) {
        switch (msg.type) {
            case NAME:
                if (msg.note != null && !msg.note.trim().isEmpty()) {
                    playerName = msg.note.trim();
                }
                if (msg.user != null) {
                    userId = msg.user.getId();
                    rating = msg.user.getRating();
                }
                System.out.println("[Server] " + playerName + " đặt tên"
                        + (userId != null ? " (userId=" + userId + ")" : " (vô danh)"));
                if (room != null) room.notifyNameUpdated(this);
                break;

            // ---- Lobby: tạo/tham gia/rời phòng, danh sách phòng, matchmaking ----
            case CREATE_ROOM:
                RoomManager.INSTANCE.createRoom(this);
                break;

            case JOIN_ROOM:
                RoomManager.INSTANCE.joinRoom(this, msg.roomCode);
                break;

            case LEAVE_ROOM:
                RoomManager.INSTANCE.leaveRoom(this);
                break;

            case ROOM_LIST:
                sendMessage(RoomManager.INSTANCE.buildRoomListMessage());
                break;

            case FIND_MATCH:
                RoomManager.INSTANCE.enqueueMatchmaking(this);
                break;

            case CANCEL_FIND:
                RoomManager.INSTANCE.cancelMatchmaking(this);
                break;

            // ---- Trong ván đấu ----
            case MOVE:
                if (room != null) room.handleMove(playerId, msg.x, msg.y);
                break;

            case REPLAY:
                if (room != null) room.requestReplay(playerId);
                break;

            case CHAT:
                if (room != null) room.broadcastChat(this, msg.note);
                break;

            case SURRENDER:
                if (room != null) room.surrender(playerId);
                break;

            case OFFER_DRAW:
                if (room != null) room.offerDraw(playerId);
                break;

            case DRAW_ACCEPT:
                if (room != null) room.respondDraw(playerId, true);
                break;

            case DRAW_REJECT:
                if (room != null) room.respondDraw(playerId, false);
                break;

            // ---- Bảng xếp hạng / Lịch sử / Hồ sơ ----
            case LEADERBOARD:
                sendLeaderboard();
                break;

            case MATCH_HISTORY:
                sendMatchHistory();
                break;

            case PROFILE_UPDATE:
                handleProfileUpdate(msg);
                break;

            default:
                break;
        }
    }

    private void sendLeaderboard() {
        try {
            List<User> top = userDAO.getLeaderboard(50);
            Message reply = new Message(Message.Type.LEADERBOARD);
            reply.leaderboard = top;
            sendMessage(reply);
        } catch (DatabaseConnection.DatabaseException e) {
            Message err = new Message(Message.Type.ERROR);
            err.note = "Không thể tải bảng xếp hạng: " + e.getMessage();
            sendMessage(err);
        }
    }

    private void sendMatchHistory() {
        if (userId == null) {
            Message err = new Message(Message.Type.ERROR);
            err.note = "Bạn cần đăng nhập để xem lịch sử trận đấu.";
            sendMessage(err);
            return;
        }
        try {
            List<MatchDAO.MatchHistoryItem> items = matchDAO.getHistoryForUser(userId, 100);
            List<MatchSummary> summaries = new ArrayList<MatchSummary>();
            for (MatchDAO.MatchHistoryItem item : items) {
                MatchSummary s = new MatchSummary();
                s.matchId = item.matchId;
                s.opponentName = item.opponentName;
                s.mode = item.mode;
                s.result = item.result;
                s.startedAt = item.startedAt;
                s.durationSeconds = item.durationSeconds;
                summaries.add(s);
            }
            Message reply = new Message(Message.Type.MATCH_HISTORY);
            reply.matchHistory = summaries;
            sendMessage(reply);
        } catch (DatabaseConnection.DatabaseException e) {
            Message err = new Message(Message.Type.ERROR);
            err.note = "Không thể tải lịch sử trận đấu: " + e.getMessage();
            sendMessage(err);
        }
    }

    private void handleProfileUpdate(Message msg) {
        if (userId == null || msg.displayName == null || msg.displayName.trim().isEmpty()) {
            Message err = new Message(Message.Type.ERROR);
            err.note = "Không thể cập nhật hồ sơ.";
            sendMessage(err);
            return;
        }
        try {
            userDAO.updateDisplayName(userId, msg.displayName.trim());
            User updated = userDAO.getById(userId);
            playerName = updated.getDisplayName();
            Message reply = new Message(Message.Type.PROFILE_UPDATED);
            reply.user = updated;
            sendMessage(reply);
        } catch (DatabaseConnection.DatabaseException e) {
            Message err = new Message(Message.Type.ERROR);
            err.note = "Lỗi cập nhật hồ sơ: " + e.getMessage();
            sendMessage(err);
        }
    }

    /**
     * Gửi message xuống client của người chơi này.
     * synchronized để tránh 2 luồng ghi cùng lúc lên 1 stream.
     */
    public synchronized void sendMessage(Message m) {
        try {
            out.writeObject(m);
            out.flush();
            out.reset(); // tránh ObjectOutputStream cache lại object cũ khi gửi nhiều lần
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
