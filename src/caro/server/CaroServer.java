package caro.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Main class của Server.
 * Chạy file này TRƯỚC, sau đó mới chạy CaroClient (2 lần) để vào chơi.
 */
public class CaroServer {

    public static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("=== CARO SERVER ===");
        printLocalIpAddresses();

        // PHASE 2: khởi động AuthServer (cổng riêng 12346, xử lý đăng nhập/đăng ký)
        // chạy nền song song - KHÔNG ảnh hưởng gì tới vòng lặp ghép game bên dưới.
        AuthServer.startInBackground();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang lắng nghe tại port " + PORT + " ...");
            System.out.println("(PHASE 5: mỗi kết nối được xử lý ngay bởi 1 thread riêng,");
            System.out.println(" việc ghép cặp 2 người chơi do RoomManager quyết định qua Lobby)\n");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
                System.out.println("[Server] Kết nối mới từ " + socket.getInetAddress());
            }
        } catch (BindException e) {
            System.out.println();
            System.out.println("!!! KHÔNG THỂ KHỞI ĐỘNG SERVER !!!");
            System.out.println("Port " + PORT + " đang bị 1 tiến trình Server khác chiếm giữ");
            System.out.println("(có thể bạn đã Run File CaroServer.java từ trước đó và quên tắt).");
            System.out.println();
            System.out.println("Cách khắc phục:");
            System.out.println("  1. Nhìn xuống khung Output, tìm tab Server cũ đang chạy,");
            System.out.println("     bấm nút vuông đỏ (Stop) để tắt nó đi.");
            System.out.println("  2. Nếu không tìm thấy, đóng và mở lại NetBeans rồi thử lại.");
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * In ra các địa chỉ IP nội bộ (LAN) của máy đang chạy server,
     * để dễ đọc và nhập vào Client khi demo qua LAN/hotspot thật,
     * khỏi phải tự gõ lệnh ipconfig / ifconfig.
     */
    private static void printLocalIpAddresses() {
        try {
            System.out.println("Địa chỉ IP của máy này (dùng IP nào để bạn cùng team nhập vào Client):");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                for (InetAddress addr : Collections.list(addresses)) {
                    // Chỉ in IPv4 cho dễ đọc (VD: 192.168.x.x)
                    if (addr.getHostAddress().indexOf(':') == -1) {
                        System.out.println("   - " + ni.getDisplayName() + " : " + addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("   (Không lấy được danh sách IP, dùng lệnh ipconfig/ifconfig để xem thủ công)");
        }
        System.out.println();
    }
}
