package caro.common;

import java.io.Serializable;

/**
 * Lớp Message dùng để đóng gói dữ liệu trao đổi giữa Client và Server
 * qua ObjectOutputStream / ObjectInputStream (Serialization).
 *
 * Class này PHẢI giống hệt nhau ở cả phía server và client,
 * nếu không sẽ bị lỗi ClassNotFoundException / InvalidClassException.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        NAME,           // Client -> Server: gửi tên người chơi ngay sau khi kết nối
                        // Server -> Client: chuyển tiếp tên của đối thủ
        WAITING,        // Server -> Client: đang chờ đối thủ vào phòng
        START,          // Server -> Client: đủ 2 người, bắt đầu ván mới
        MOVE,           // Client -> Server: người chơi đánh 1 nước
                        // Server -> Client: broadcast lại nước đi cho cả 2 bên
        WIN,            // Server -> Client: có người thắng
        DRAW,           // Server -> Client: hòa (hết ô trống)
        OPPONENT_LEFT,  // Server -> Client: đối thủ đã ngắt kết nối
        REPLAY,         // Client -> Server: xin chơi lại
                        // Server -> Client: báo đối thủ đang chờ chơi lại
        CHAT,           // Client -> Server: gửi tin nhắn chat
                        // Server -> Client: chuyển tiếp tin nhắn (kèm tên người gửi)
        ERROR,

        // ==== PHASE 2: Đăng ký / Đăng nhập (đi qua AuthServer, cổng riêng) ====
        LOGIN,             // Client -> Server: gửi username + password để đăng nhập
        LOGIN_SUCCESS,     // Server -> Client: đăng nhập đúng, kèm theo "user" (thông tin tài khoản)
        LOGIN_FAILED,      // Server -> Client: sai username/password, lý do nằm trong "note"
        REGISTER,          // Client -> Server: gửi username + password + displayName để đăng ký
        REGISTER_SUCCESS,  // Server -> Client: đăng ký thành công, kèm theo "user"
        REGISTER_FAILED,   // Server -> Client: đăng ký thất bại, lý do nằm trong "note"

        // ==== PHASE 4/5: Lobby + Room + Matchmaking ====
        CREATE_ROOM,       // Client -> Server: tạo phòng mới
        ROOM_CREATED,      // Server -> Client: tạo phòng thành công, roomCode chứa mã phòng
        JOIN_ROOM,         // Client -> Server: xin vào phòng, roomCode chứa mã phòng muốn vào
        ROOM_JOINED,       // Server -> Client (cả 2 bên): vào phòng thành công
        ROOM_JOIN_FAILED,  // Server -> Client: không vào được phòng, lý do trong "note"
        LEAVE_ROOM,        // Client -> Server: rời phòng đang chờ (trước khi đủ 2 người)
        ROOM_LIST,         // Client -> Server: xin danh sách phòng đang chờ
                           // Server -> Client: trả về danh sách, chứa trong "roomList"
        FIND_MATCH,        // Client -> Server: xin ghép trận ngẫu nhiên (matchmaking)
        CANCEL_FIND,       // Client -> Server: hủy tìm trận

        // ==== PHASE 7: Đầu hàng / Đề nghị hòa ====
        SURRENDER,         // Client -> Server: đầu hàng
        OFFER_DRAW,        // Client -> Server: đề nghị hòa
                           // Server -> Client: chuyển tiếp đề nghị hòa cho đối thủ
        DRAW_ACCEPT,       // Client -> Server: đồng ý đề nghị hòa
        DRAW_REJECT,       // Client -> Server: từ chối đề nghị hòa
                           // Server -> Client: báo người đề nghị biết đã bị từ chối

        // ==== PHASE 8/9: Lịch sử + Bảng xếp hạng ====
        MATCH_HISTORY,     // Client -> Server: xin lịch sử trận đấu của mình
                           // Server -> Client: trả về, chứa trong "matchHistory"
        LEADERBOARD,       // Client -> Server: xin bảng xếp hạng
                           // Server -> Client: trả về, chứa trong "leaderboard"

        // ==== Phase 4: Hồ sơ ====
        PROFILE_UPDATE,    // Client -> Server: đổi tên hiển thị, displayName chứa tên mới
        PROFILE_UPDATED    // Server -> Client: cập nhật thành công, kèm theo "user" mới
    }

    public Type type;
    public int playerId;    // 1 hoặc 2 - id người chơi
    public int x, y;        // toạ độ ô cờ (dùng cho MOVE)
    public int nextTurn;    // playerId của người được đánh tiếp theo (dùng cho MOVE)
    public int winnerId;    // playerId người thắng (dùng cho WIN)
    public String note;     // tên người chơi / thông điệp phụ / thông báo lỗi, hiển thị lên GUI
    public int[] winLineX;  // toạ độ x của các ô nằm trên đường thắng (để tô sáng)
    public int[] winLineY;  // toạ độ y của các ô nằm trên đường thắng (để tô sáng)

    // ==== PHASE 2: các field phục vụ LOGIN/REGISTER ====
    public String username;      // dùng cho LOGIN, REGISTER
    public String password;      // dùng cho LOGIN, REGISTER (dạng plain text TRONG BỘ NHỚ,
                                  // server sẽ hash trước khi so sánh/lưu - không bao giờ lưu plain text)
    public String displayName;   // dùng cho REGISTER, PROFILE_UPDATE
    public User user;            // dùng cho LOGIN_SUCCESS, REGISTER_SUCCESS, PROFILE_UPDATED

    // ==== PHASE 4/5: Lobby/Room/Matchmaking ====
    public String roomCode;                 // dùng cho CREATE_ROOM/ROOM_CREATED/JOIN_ROOM/ROOM_JOINED
    public java.util.List<RoomInfo> roomList;       // dùng cho ROOM_LIST (response)

    // ==== PHASE 8/9: Lịch sử + Bảng xếp hạng ====
    public java.util.List<MatchSummary> matchHistory; // dùng cho MATCH_HISTORY (response)
    public java.util.List<User> leaderboard;           // dùng cho LEADERBOARD (response)

    public Message(Type type) {
        this.type = type;
    }
}
