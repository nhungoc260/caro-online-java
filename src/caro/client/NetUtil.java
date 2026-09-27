package caro.client;

import caro.common.Message;
import caro.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Tiện ích dùng chung: mở 1 kết nối NGẮN HẠN tới CaroServer (port 12345),
 * tự nhận diện bằng tài khoản đã đăng nhập (gửi NAME kèm "user"), gửi 1
 * request, đọc đúng 1 response rồi đóng kết nối.
 *
 * Dùng cho các màn hình chỉ cần "hỏi 1 câu, nhận 1 câu trả lời" như
 * LeaderboardFrame, HistoryFrame, ProfileFrame - không cần giữ kết nối
 * lâu dài như LobbyFrame (đang chờ push event) hay CaroClient (đang chơi).
 */
class NetUtil {

    static Message sendGameRequest(String ip, User identifyAs, Message request) {
        try (Socket socket = new Socket(ip, 12345)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            if (identifyAs != null) {
                Message identify = new Message(Message.Type.NAME);
                identify.note = identifyAs.getDisplayName();
                identify.user = identifyAs;
                out.writeObject(identify);
                out.flush();
                out.reset();
            }

            out.writeObject(request);
            out.flush();

            return (Message) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[NetUtil] Lỗi kết nối server: " + e.getMessage());
            return null;
        }
    }

    private NetUtil() {
    }
}
