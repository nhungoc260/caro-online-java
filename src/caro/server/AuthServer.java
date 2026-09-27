package caro.server;

import caro.common.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Server RIÊNG cho việc xác thực (đăng nhập / đăng ký), chạy trên cổng
 * {@link #AUTH_PORT} - HOÀN TOÀN TÁCH BIỆT với cổng chơi game 12345 mà
 * CaroServer/ClientHandler/GameRoom đang dùng.
 *
 * LÝ DO TÁCH RIÊNG (thay vì nhét LOGIN/REGISTER vào ClientHandler cũ):
 *   CaroServer.main() hiện tại accept() ĐÚNG 2 lần rồi lập tức ghép thành
 *   1 GameRoom (xem code gốc). Nếu để LOGIN/REGISTER đi chung cổng đó, kết
 *   nối "chỉ để đăng nhập" sẽ bị server hiểu nhầm là 1 trong 2 người chơi,
 *   làm treo hoặc vỡ luồng ghép phòng đang chạy rất ổn định.
 *   => Tách cổng là cách AN TOÀN NHẤT để thêm tính năng mới mà không đụng
 *      1 dòng nào trong CaroServer/ClientHandler/GameRoom hiện tại.
 *
 * Ở Phase 5 (RoomManager đa phòng), khi CaroServer được viết lại để spawn
 * 1 thread ngay khi accept() (thay vì chờ đủ 2 người), AuthServer có thể
 * được gộp lại chung 1 cổng với logic phòng/lobby nếu muốn - nhưng đó là
 * quyết định của Phase 5, không phải bây giờ.
 */
public class AuthServer {

    public static final int AUTH_PORT = 12346;

    /**
     * Khởi động AuthServer trên 1 Thread nền (daemon), để không chặn
     * CaroServer.main() tiếp tục chạy vòng lặp game như bình thường.
     * Gọi 1 lần duy nhất từ CaroServer.main().
     */
    public static void startInBackground() {
        Thread t = new Thread(AuthServer::runAcceptLoop, "AuthServer");
        t.setDaemon(true);
        t.start();
    }

    private static void runAcceptLoop() {
        try (ServerSocket serverSocket = new ServerSocket(AUTH_PORT)) {
            System.out.println("[AuthServer] Đang lắng nghe tại port " + AUTH_PORT
                    + " (xử lý đăng nhập/đăng ký)...");
            while (true) {
                Socket socket = serverSocket.accept();
                Thread handlerThread = new Thread(new AuthClientHandler(socket));
                handlerThread.setDaemon(true);
                handlerThread.start();
            }
        } catch (IOException e) {
            System.out.println("[AuthServer] Không thể khởi động ở port " + AUTH_PORT
                    + ": " + e.getMessage());
            System.out.println("[AuthServer] Chức năng đăng nhập/đăng ký sẽ KHÔNG hoạt động, "
                    + "nhưng chơi game (port 12345) vẫn chạy bình thường.");
        }
    }

    /**
     * Xử lý 1 kết nối xác thực. Cho phép nhiều lần thử trên cùng 1 kết nối
     * (ví dụ: đăng ký thất bại vì trùng username -> thử lại ngay, không
     * cần LoginFrame/RegisterFrame phải mở lại socket mới).
     */
    private static class AuthClientHandler implements Runnable {
        private final Socket socket;
        private final AuthService authService = new AuthService();

        AuthClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (Socket s = socket) {
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());

                Message request;
                while ((request = (Message) in.readObject()) != null) {
                    Message response = handle(request);
                    if (response != null) {
                        out.writeObject(response);
                        out.flush();
                        out.reset();
                    }
                }
            } catch (EOFException | SocketException e) {
                // Client đóng kết nối sau khi xác thực xong - bình thường, không cần log lỗi
            } catch (Exception e) {
                System.out.println("[AuthServer] Lỗi xử lý kết nối: " + e.getMessage());
            }
        }

        private Message handle(Message request) {
            switch (request.type) {
                case LOGIN:
                    System.out.println("[AuthServer] Yêu cầu đăng nhập: " + request.username);
                    return authService.login(request.username, request.password);

                case REGISTER:
                    System.out.println("[AuthServer] Yêu cầu đăng ký: " + request.username);
                    return authService.register(request.username, request.password, request.displayName);

                default:
                    Message err = new Message(Message.Type.ERROR);
                    err.note = "AuthServer chỉ hỗ trợ LOGIN/REGISTER.";
                    return err;
            }
        }
    }

    private AuthServer() {
    }
}
