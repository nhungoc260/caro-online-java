package caro.server;

import caro.common.Message;
import caro.common.RoomInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý TẤT CẢ các phòng chơi (multi-room) và hàng đợi "Tìm đối thủ"
 * (matchmaking). Đây là điểm khác biệt lớn nhất so với server cũ: thay vì
 * chỉ ghép cố định 2 client 1 lần rồi lặp lại, giờ server có thể xử lý
 * NHIỀU phòng cùng lúc.
 *
 * GameRoom.java (logic bàn cờ/thắng-thua/timer) được TÁI SỬ DỤNG NGUYÊN VẸN
 * - class này chỉ lo việc "ai chơi với ai", không đụng gì tới luật chơi.
 */
public class RoomManager {

    /** Instance dùng chung cho toàn bộ server (1 server = 1 RoomManager). */
    public static final RoomManager INSTANCE = new RoomManager();

    private final ConcurrentHashMap<String, RoomEntry> rooms = new ConcurrentHashMap<String, RoomEntry>();
    private final Random random = new Random();

    /** Người chơi đang chờ được ghép ngẫu nhiên (FIND_MATCH), tối đa 1 người tại 1 thời điểm. */
    private ClientHandler waitingForMatch = null;
    private final Object matchLock = new Object();

    private RoomManager() {
    }

    /** 1 phòng = mã phòng + GameRoom (logic) + host/guest + trạng thái hiển thị ở Lobby. */
    private static class RoomEntry {
        String roomCode;
        GameRoom gameRoom;
        ClientHandler host;
        ClientHandler guest;
        String status; // WAITING / PLAYING
    }

    // ---------------------------------------------------------
    // TẠO PHÒNG / THAM GIA PHÒNG (theo mã)
    // ---------------------------------------------------------

    public synchronized void createRoom(ClientHandler host) {
        String code = generateUniqueRoomCode();
        RoomEntry entry = new RoomEntry();
        entry.roomCode = code;
        entry.gameRoom = new GameRoom();
        entry.gameRoom.setRoomCode(code);
        entry.host = host;
        entry.status = "WAITING";
        rooms.put(code, entry);

        host.setCurrentRoomCode(code);

        Message reply = new Message(Message.Type.ROOM_CREATED);
        reply.roomCode = code;
        host.sendMessage(reply);
        System.out.println("[RoomManager] " + host.getPlayerName() + " tạo phòng " + code);
    }

    public synchronized void joinRoom(ClientHandler guest, String roomCode) {
        RoomEntry entry = (roomCode == null) ? null : rooms.get(roomCode.trim().toUpperCase());
        if (entry == null) {
            fail(guest, "Không tìm thấy phòng \"" + roomCode + "\".");
            return;
        }
        if (!"WAITING".equals(entry.status)) {
            fail(guest, "Phòng \"" + roomCode + "\" đã đầy hoặc đang chơi.");
            return;
        }
        if (entry.host == guest) {
            fail(guest, "Bạn không thể tự tham gia phòng của chính mình.");
            return;
        }

        entry.guest = guest;
        entry.status = "PLAYING";
        guest.setCurrentRoomCode(entry.roomCode);
        startGameInRoom(entry);
    }

    /** Rời phòng đang WAITING (trước khi có guest) - ví dụ host đổi ý không muốn chờ nữa. */
    public synchronized void leaveRoom(ClientHandler who) {
        String code = who.getCurrentRoomCode();
        if (code == null) return;
        RoomEntry entry = rooms.get(code);
        if (entry != null && "WAITING".equals(entry.status) && entry.host == who) {
            rooms.remove(code);
            System.out.println("[RoomManager] Phòng " + code + " đã bị hủy (host rời đi).");
        }
        who.setCurrentRoomCode(null);
    }

    public synchronized Message buildRoomListMessage() {
        List<RoomInfo> list = new ArrayList<RoomInfo>();
        for (RoomEntry e : rooms.values()) {
            if ("WAITING".equals(e.status)) {
                list.add(new RoomInfo(e.roomCode, e.host.getPlayerName(), e.status));
            }
        }
        Message m = new Message(Message.Type.ROOM_LIST);
        m.roomList = list;
        return m;
    }

    // ---------------------------------------------------------
    // MATCHMAKING (FIND_MATCH - ghép ngẫu nhiên)
    // ---------------------------------------------------------

    public void enqueueMatchmaking(ClientHandler player) {
        ClientHandler opponent = null;
        synchronized (matchLock) {
            if (waitingForMatch == null) {
                waitingForMatch = player;
            } else if (waitingForMatch == player) {
                return; // đã ở trong hàng đợi rồi, bỏ qua click đúp
            } else {
                opponent = waitingForMatch;
                waitingForMatch = null;
            }
        }

        if (opponent == null) {
            Message waiting = new Message(Message.Type.WAITING);
            waiting.note = "Đang tìm đối thủ...";
            player.sendMessage(waiting);
        } else {
            String code = generateUniqueRoomCode();
            RoomEntry entry = new RoomEntry();
            entry.roomCode = code;
            entry.gameRoom = new GameRoom();
            entry.gameRoom.setRoomCode(code);
            entry.host = opponent;
            entry.guest = player;
            entry.status = "PLAYING";
            rooms.put(code, entry);
            opponent.setCurrentRoomCode(code);
            player.setCurrentRoomCode(code);
            startGameInRoom(entry);
        }
    }

    public void cancelMatchmaking(ClientHandler player) {
        synchronized (matchLock) {
            if (waitingForMatch == player) {
                waitingForMatch = null;
            }
        }
    }

    /** Gọi khi 1 client ngắt kết nối - dọn dẹp khỏi hàng đợi/phòng chờ nếu có. */
    public synchronized void handleDisconnect(ClientHandler who) {
        synchronized (matchLock) {
            if (waitingForMatch == who) waitingForMatch = null;
        }
        leaveRoom(who);
    }

    // ---------------------------------------------------------
    private void startGameInRoom(RoomEntry entry) {
        entry.host.setPlayerId(1);
        entry.guest.setPlayerId(2);
        entry.gameRoom.setPlayer1(entry.host);
        entry.gameRoom.setPlayer2(entry.guest);
        entry.host.setRoom(entry.gameRoom);
        entry.guest.setRoom(entry.gameRoom);

        Message joined1 = new Message(Message.Type.ROOM_JOINED);
        joined1.roomCode = entry.roomCode;
        joined1.note = entry.guest.getPlayerName();
        entry.host.sendMessage(joined1);

        Message joined2 = new Message(Message.Type.ROOM_JOINED);
        joined2.roomCode = entry.roomCode;
        joined2.note = entry.host.getPlayerName();
        entry.guest.sendMessage(joined2);

        // Gửi kèm tên đối thủ qua Message.Type.NAME cho cả 2 bên NGAY LÚC NÀY.
        // Lý do: cơ chế cũ (ClientHandler.handle case NAME -> room.notifyNameUpdated)
        // chỉ hoạt động khi "room" đã được gán - nhưng ở luồng Lobby mới, client
        // gửi NAME nhận diện tài khoản NGAY LÚC KẾT NỐI, tức là TRƯỚC khi room
        // được gán (trước cả CREATE_ROOM/FIND_MATCH) - nên tin nhắn NAME ban đầu
        // đó không bao giờ kích hoạt notifyNameUpdated được. Gửi lại ở đây để
        // CaroClient nhận đúng tên đối thủ hiển thị trên tiêu đề/giao diện.
        Message nameForHost = new Message(Message.Type.NAME);
        nameForHost.note = entry.guest.getPlayerName();
        entry.host.sendMessage(nameForHost);

        Message nameForGuest = new Message(Message.Type.NAME);
        nameForGuest.note = entry.host.getPlayerName();
        entry.guest.sendMessage(nameForGuest);

        System.out.println("[RoomManager] Bắt đầu ván tại phòng " + entry.roomCode
                + ": " + entry.host.getPlayerName() + " vs " + entry.guest.getPlayerName());
        entry.gameRoom.startGame(); // tái sử dụng nguyên vẹn logic GameRoom cũ
    }

    private void fail(ClientHandler client, String reason) {
        Message m = new Message(Message.Type.ROOM_JOIN_FAILED);
        m.note = reason;
        client.sendMessage(m);
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            code = generateRoomCode();
        } while (rooms.containsKey(code));
        return code;
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bỏ ký tự dễ nhầm (I, O, 0, 1)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
