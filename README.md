<<<<<<< HEAD
#  Cờ Caro Online
=======
# Cờ Caro Online (Client - Server)
>>>>>>> 1cc587e724554b092f27c8da60db50552c736f7c

Hệ thống chơi **Cờ Caro (Gomoku) Online** hoàn chỉnh: có tài khoản, đăng
nhập, sảnh chờ (lobby), tạo/tham gia phòng, ghép trận ngẫu nhiên
(matchmaking), chơi 1vs1 qua mạng, chơi với AI (3 mức độ), đầu hàng, đề
nghị hòa, lịch sử trận đấu và bảng xếp hạng — tất cả lưu trên MySQL.

Dự án được nâng cấp từ 1 bản demo Caro 2 người chơi đơn giản (Client-Server
qua TCP Socket) thành 1 hệ thống game online đầy đủ, **không phá bỏ** bất
kỳ phần lõi nào của bản gốc (bàn cờ 15x15, kiểm tra thắng, timer, chat,
âm thanh, giao diện tông gỗ).

---

## 1. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 8 |
| Giao diện | Java Swing (tự vẽ bằng `Graphics2D`, không dùng thư viện UI ngoài) |
| Giao tiếp mạng | TCP Socket (`java.net.Socket`, `ServerSocket`) |
| Đa luồng | `Thread`, `synchronized`, `ConcurrentHashMap` |
| Truyền dữ liệu | Java Object Serialization (`ObjectInputStream`/`ObjectOutputStream`) |
| Cơ sở dữ liệu | MySQL 5.7.44 (qua WampServer) |
| Kết nối DB | JDBC thuần (`mysql-connector-j`), không dùng Hibernate/ORM |
| IDE / Build | NetBeans IDE 8.0.2, Ant |

---

## 2. Kiến trúc hệ thống

```
                         CARO ONLINE SYSTEM
                                |
                 +--------------+---------------+
                 |                              |
          AuthServer (port 12346)       CaroServer (port 12345)
          xử lý ĐĂNG NHẬP/ĐĂNG KÝ        xử lý LOBBY + GAME
                 |                              |
             UserDAO                      RoomManager
                 |                              |
              MySQL                    +--------+--------+
                                        |                 |
                                  GameRoom (ván 1)   GameRoom (ván 2)...
                                        |
                              MatchDAO / MoveDAO / UserDAO
                                        |
                                     MySQL
```

**2 cổng TCP riêng biệt** (thiết kế có chủ đích, không phải trùng lặp thừa):

- **Cổng 12346 (AuthServer):** chỉ xử lý `LOGIN`/`REGISTER`. Là các kết nối
  ngắn hạn (mở → gửi 1 yêu cầu → nhận phản hồi → đóng).
- **Cổng 12345 (CaroServer):** xử lý mọi thứ liên quan tới lobby và ván
  đấu — mỗi client giữ **1 kết nối lâu dài** trong suốt thời gian ở Lobby
  và khi đang chơi. Server tạo **1 Thread riêng cho mỗi client** ngay khi
  kết nối (`ClientHandler`), không giới hạn số người chơi cùng lúc.

**`RoomManager`** (singleton) quản lý toàn bộ phòng đang hoạt động bằng
`ConcurrentHashMap<String, Room>` — cho phép **nhiều ván đấu chạy song
song** mà không ảnh hưởng lẫn nhau, cùng với 1 hàng đợi ghép trận ngẫu
nhiên (matchmaking) đơn giản.

**`GameRoom`** đóng vai trò *trọng tài*: giữ bàn cờ 15x15 của đúng 1 ván,
kiểm tra lượt đi, giới hạn 20 giây/lượt, xác định thắng/thua/hòa. Client
**không bao giờ tự quyết định** kết quả trận đấu — mọi phán quyết đều đến
từ server, tránh gian lận và đảm bảo 2 client luôn đồng bộ trạng thái.

---

## 3. Cấu trúc thư mục

```
CaroOnline/
├── database/
│   └── caro_online.sql        # Toàn bộ schema + dữ liệu demo
├── src/caro/
│   ├── common/                # Lớp dùng chung giữa Client & Server
│   │   ├── Message.java       # Giao thức truyền tin (Serializable)
│   │   ├── User.java          # Thông tin tài khoản (không chứa password)
│   │   ├── RoomInfo.java      # Thông tin tóm tắt 1 phòng
│   │   └── MatchSummary.java  # Thông tin tóm tắt 1 trận (cho Lịch sử)
│   │
│   ├── database/              # Tầng JDBC
│   │   ├── DatabaseConfig.java, DatabaseConnection.java
│   │   └── UserDAO.java, MatchDAO.java, MoveDAO.java
│   │
│   ├── server/
│   │   ├── CaroServer.java    # Điểm khởi động server (port 12345)
│   │   ├── ServerFrame.java   # Giao diện chạy server để demo
│   │   ├── AuthServer.java / AuthService.java   # Đăng nhập/đăng ký (port 12346)
│   │   ├── RoomManager.java   # Quản lý đa phòng + matchmaking
│   │   ├── ClientHandler.java # 1 Thread xử lý 1 client
│   │   └── GameRoom.java      # Luật chơi + lưu kết quả DB
│   │
│   ├── ai/                    # AI chơi Caro (chạy local, không qua mạng)
│   │   ├── CaroAI.java (interface)
│   │   ├── EasyAI.java        # Ngẫu nhiên
│   │   ├── MediumAI.java      # Chặn thua / tạo chuỗi 3-4
│   │   └── HardAI.java        # Heuristic tấn công + phòng thủ
│   │
│   ├── ui/                    # Component giao diện dùng chung
│   │   └── Theme.java, RoundedButton.java, RoundedPanel.java
│   │
│   └── client/
│       ├── LoginFrame.java / RegisterFrame.java
│       ├── LobbyFrame.java    # Sảnh chờ chính
│       ├── RoomFrame.java     # Cửa sổ phòng chờ (sau khi tạo phòng)
│       ├── CaroClient.java    # Bàn cờ khi chơi ONLINE
│       ├── AIGameFrame.java   # Bàn cờ khi chơi với AI
│       ├── LeaderboardFrame.java, HistoryFrame.java, ProfileFrame.java
│       └── NetUtil.java       # Tiện ích gửi request ngắn hạn tới server
```

---

## 4. Tính năng đầy đủ

- Đăng ký / Đăng nhập (mật khẩu băm SHA-256, không lưu plain text)
- Sảnh chờ (Lobby) sau khi đăng nhập
- Tạo phòng riêng (chia sẻ mã 6 ký tự cho bạn bè)
- Tham gia phòng bằng mã / xem danh sách phòng đang chờ
- Ghép trận ngẫu nhiên (matchmaking) — bấm "Chơi Online" là tự tìm đối thủ
- Bàn cờ 15x15, kiểm tra thắng 5 quân theo 4 hướng, giới hạn 20s/lượt
- Chat trong ván đấu
- Đầu hàng / Đề nghị hòa (đối thủ có thể Đồng ý / Từ chối)
- Chơi lại sau khi ván kết thúc (2 bên cùng đồng ý)
- Chơi với AI — 3 mức: Dễ / Trung bình / Khó (không cần server, chạy local)
- Lịch sử trận đấu (cả Online lẫn AI)
- Bảng xếp hạng theo rating (tính kiểu Elo đơn giản)
- Hồ sơ cá nhân — xem thống kê, đổi tên hiển thị
- Server tự phát hiện & hiển thị địa chỉ IP LAN để demo nhiều máy
- Giao diện Server riêng (`ServerFrame`) để chiếu màn hình khi demo

---

## 5. Ghi chú kỹ thuật quan trọng

- **Server là trọng tài duy nhất**: mọi kết quả thắng/thua/hòa, lượt đi,
  và cập nhật điểm số đều do server quyết định — Client không có quyền tự
  công bố kết quả trận đấu (chống gian lận).
- **Chế độ AI chạy hoàn toàn local**, không cần TCP — nhưng kết quả trận
  vẫn được lưu vào MySQL (client tự gọi thẳng tầng DAO, vì đây là chế độ
  chơi đơn không có tranh chấp trạng thái giữa 2 máy cần trọng tài).
- **Mật khẩu** được băm SHA-256 trước khi lưu, không bao giờ lưu hoặc gửi
  qua mạng dạng plain text.
- Hệ thống hiện **không có phân quyền admin** — mọi tài khoản đều có
  quyền như nhau (chơi, xem xếp hạng/lịch sử, sửa hồ sơ của chính mình).

---

## 6. Giới hạn đã biết

- Sau khi 1 ván online kết thúc, muốn quay lại Lobby cần đóng cửa sổ chơi
  và đăng nhập lại (chưa có nút "Về Lobby" liền mạch).
- Chế độ AI không có timer/chat (không cần thiết cho chơi 1 mình).
- Chưa có giao diện quản trị (admin panel) — không nằm trong phạm vi yêu
  cầu ban đầu.
