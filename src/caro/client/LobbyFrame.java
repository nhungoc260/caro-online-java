package caro.client;

import caro.common.Message;
import caro.common.RoomInfo;
import caro.common.User;
import caro.ui.RoundedButton;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * SẢNH CHỜ (Lobby) - màn hình trung tâm sau khi đăng nhập thành công.
 *
 * Giữ 1 kết nối TCP DUY NHẤT, LÂU DÀI tới CaroServer (port 12345) trong
 * suốt thời gian ở Lobby, để có thể nhận các thông báo đẩy (push) từ
 * server như "đã ghép được đối thủ", "có người vào phòng"...
 *
 * Khi trận đấu thật sự bắt đầu (nhận Message.Type.START), LobbyFrame
 * "bàn giao" NGUYÊN VẸN kết nối đó (out/in) cho 1 cửa sổ CaroClient mới
 * qua attachExistingConnection(...) - không mở kết nối mới, không đụng gì
 * tới logic bàn cờ/server đã có.
 */
public class LobbyFrame extends JFrame {

    private final User currentUser;
    private final String serverIp;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connectionHandedOff = false;

    private JLabel welcomeLabel;
    private JLabel connectionStatusLabel;

    // Dialog "đang tìm đối thủ" (matchmaking) - null nếu không đang mở
    private JDialog findMatchDialog;
    // Dialog "phòng chờ" (sau khi tạo phòng) - null nếu không đang mở
    private RoomFrame roomWaitingDialog;
    // Tên đối thủ mới nhất nhận được từ server (qua ROOM_JOINED/NAME),
    // lưu tạm ở đây để truyền cho CaroClient lúc bàn giao kết nối khi START tới.
    private String pendingOpponentName = "Đối thủ";

    public LobbyFrame(User user, String serverIp) {
        this.currentUser = user;
        this.serverIp = (serverIp == null || serverIp.trim().isEmpty()) ? "localhost" : serverIp.trim();

        setTitle("Cờ Caro Online - Sảnh chờ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMenuGrid(), BorderLayout.CENTER);

        setSize(640, 620);
        setMinimumSize(new Dimension(560, 560));
        setLocationRelativeTo(null);

        connectPersistent();
    }

    // ---------- Giao diện ----------

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, Theme.WOOD_DARK, 0, getHeight(), Theme.WOOD_MEDIUM);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.WOOD_ACCENT);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CỜ CARO ONLINE");
        title.setFont(Theme.titleFont(20));
        title.setForeground(Theme.WHITE);
        welcomeLabel = new JLabel("Xin chào, " + currentUser.getDisplayName()
                + "  |  Rating: " + currentUser.getRating());
        welcomeLabel.setFont(Theme.labelFont(13));
        welcomeLabel.setForeground(new Color(224, 209, 195));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(welcomeLabel);

        connectionStatusLabel = new JLabel("Đang kết nối...");
        connectionStatusLabel.setFont(Theme.labelFont(12));
        connectionStatusLabel.setForeground(new Color(224, 209, 195));

        header.add(left, BorderLayout.WEST);
        header.add(connectionStatusLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMenuGrid() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CREAM);
        wrapper.setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel grid = new JPanel(new GridLayout(4, 2, 16, 16));
        grid.setOpaque(false);

        grid.add(menuButton("CHƠI ONLINE", Theme.MOSS_GREEN, e -> onPlayOnline()));
        grid.add(menuButton("CHƠI VỚI AI", Theme.AMBER_EARTH, e -> onPlayAI()));
        grid.add(menuButton("TẠO PHÒNG", Theme.WOOD_MEDIUM, e -> onCreateRoom()));
        grid.add(menuButton("THAM GIA PHÒNG", Theme.WOOD_MEDIUM, e -> onJoinRoom()));
        grid.add(menuButton("PHÒNG ĐANG CHỜ", Theme.TAUPE_WAIT, e -> onRoomList()));
        grid.add(menuButton("BẢNG XẾP HẠNG", Theme.TERRACOTTA, e -> onLeaderboard()));
        grid.add(menuButton("LỊCH SỬ TRẬN", Theme.TERRACOTTA, e -> onHistory()));
        grid.add(menuButton("HỒ SƠ", Theme.WOOD_DARK, e -> onProfile()));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(grid, BorderLayout.CENTER);

        RoundedButton logoutButton = new RoundedButton("ĐĂNG XUẤT");
        logoutButton.setBackground(new Color(120, 100, 90));
        logoutButton.setBorder(new EmptyBorder(10, 0, 10, 0));
        logoutButton.addActionListener(e -> onLogout());
        JPanel logoutRow = new JPanel(new BorderLayout());
        logoutRow.setOpaque(false);
        logoutRow.setBorder(new EmptyBorder(20, 0, 0, 0));
        logoutRow.add(logoutButton, BorderLayout.CENTER);
        content.add(logoutRow, BorderLayout.SOUTH);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        wrapper.add(content, c);
        return wrapper;
    }

    private RoundedButton menuButton(String text, Color bg, java.awt.event.ActionListener listener) {
        RoundedButton b = new RoundedButton(text);
        b.setBackground(bg);
        b.setFont(Theme.boldFont(14));
        b.setBorder(new EmptyBorder(18, 8, 18, 8));
        b.addActionListener(listener);
        return b;
    }

    // ---------- Kết nối lâu dài tới server ----------

    private void connectPersistent() {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, 12345);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                Message identify = new Message(Message.Type.NAME);
                identify.note = currentUser.getDisplayName();
                identify.user = currentUser;
                sendMessage(identify);

                SwingUtilities.invokeLater(() -> connectionStatusLabel.setText("Đã kết nối server"));
                listenLoop();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    connectionStatusLabel.setText("Mất kết nối!");
                    JOptionPane.showMessageDialog(LobbyFrame.this,
                            "Không thể kết nối tới server game (port 12345): " + e.getMessage()
                                    + "\nKiểm tra CaroServer đã chạy chưa?",
                            "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "LobbyFrame-Connect").start();
    }

    private void listenLoop() {
        try {
            Message msg;
            while (!connectionHandedOff && (msg = (Message) in.readObject()) != null) {
                if (msg.type == Message.Type.START) {
                    // QUAN TRỌNG: đặt cờ và dừng đọc NGAY trên chính thread này,
                    // trước khi bàn giao kết nối cho CaroClient. Nếu không, thread
                    // này có thể lặp lại và gọi in.readObject() thêm 1 lần trong
                    // lúc CaroClient cũng vừa bắt đầu đọc từ CÙNG in - gây tranh
                    // chấp (race condition) làm hỏng luồng dữ liệu, rớt kết nối.
                    connectionHandedOff = true;
                    final int assignedId = msg.playerId;
                    SwingUtilities.invokeLater(() -> startGameHandoff(assignedId));
                    break;
                }
                final Message finalMsg = msg;
                if (msg.type == Message.Type.NAME || msg.type == Message.Type.ROOM_JOINED) {
                    // Cập nhật tên đối thủ NGAY trên thread đang đọc (đồng bộ),
                    // đảm bảo có giá trị đúng trước khi START tới ngay sau đó.
                    if (msg.note != null && !msg.note.trim().isEmpty()) {
                        pendingOpponentName = msg.note.trim();
                    }
                }
                SwingUtilities.invokeLater(() -> handleServerMessage(finalMsg));
            }
        } catch (Exception e) {
            if (!connectionHandedOff) {
                SwingUtilities.invokeLater(() -> connectionStatusLabel.setText("Mất kết nối tới server."));
            }
        }
    }

    private void handleServerMessage(Message msg) {
        switch (msg.type) {
            case WAITING:
                if (findMatchDialog != null) {
                    // đang chờ ghép trận - không cần làm gì thêm, dialog đã hiện sẵn
                }
                break;

            case ROOM_CREATED:
                closeFindMatchDialog();
                roomWaitingDialog = new RoomFrame(this, msg.roomCode, () -> {
                    sendMessage(new Message(Message.Type.LEAVE_ROOM));
                    roomWaitingDialog = null;
                });
                roomWaitingDialog.setVisible(true);
                break;

            case ROOM_JOINED:
                // Cả host lẫn guest đều nhận được - game sắp bắt đầu (START sẽ tới ngay sau).
                if (roomWaitingDialog != null) {
                    roomWaitingDialog.setStatusText("Đối thủ đã vào: " + msg.note + " - đang bắt đầu ván...");
                }
                break;

            case ROOM_JOIN_FAILED:
                JOptionPane.showMessageDialog(this, msg.note, "Không thể vào phòng", JOptionPane.WARNING_MESSAGE);
                break;

            case ROOM_LIST:
                showRoomListDialog(msg.roomList);
                break;

            case ERROR:
                JOptionPane.showMessageDialog(this, msg.note, "Lỗi", JOptionPane.ERROR_MESSAGE);
                break;

            default:
                break;
        }
    }

    /**
     * Ván đấu bắt đầu (matchmaking đã ghép được đối thủ, hoặc phòng đã đủ
     * người). Bàn giao kết nối hiện tại cho 1 cửa sổ CaroClient mới, ẩn
     * LobbyFrame đi (không đóng hẳn, để quay lại sau khi ván kết thúc -
     * xem CaroClient, nút "CHƠI LẠI"/đóng cửa sổ sẽ đưa người dùng ra khỏi
     * ván; quay lại Lobby bằng cách mở lại LobbyFrame/đăng nhập lại ở
     * bản hiện tại - polish thêm "Về Lobby" sau khi ván kết thúc có thể
     * bổ sung ở Phase 11).
     */
    private void startGameHandoff(int assignedPlayerId) {
        closeFindMatchDialog();
        if (roomWaitingDialog != null) {
            roomWaitingDialog.dispose();
            roomWaitingDialog = null;
        }

        CaroClient client = new CaroClient(currentUser.getDisplayName());
        client.setVisible(true);
        client.attachExistingConnection(socket, out, in, assignedPlayerId, pendingOpponentName,
                this::returnFromGame);

        setVisible(false);
    }

    /**
     * Callback được CaroClient gọi khi người dùng bấm "Về sảnh chờ".
     * Kết nối cũ (đã bị CaroClient đóng) không dùng lại được nữa - mở 1
     * kết nối MỚI hoàn toàn tới server rồi hiện lại LobbyFrame.
     */
    private void returnFromGame() {
        SwingUtilities.invokeLater(() -> {
            connectionHandedOff = false;
            pendingOpponentName = "Đối thủ";
            connectionStatusLabel.setText("Đang kết nối...");
            setVisible(true);
            connectPersistent();
        });
    }

    // ---------- Xử lý các nút menu ----------

    private void onPlayOnline() {
        sendMessage(new Message(Message.Type.FIND_MATCH));
        findMatchDialog = buildFindMatchDialog();
        findMatchDialog.setVisible(true);
    }

    private JDialog buildFindMatchDialog() {
        JDialog dialog = new JDialog(this, "Đang tìm đối thủ", true);
        dialog.setLayout(new BorderLayout());
        JLabel label = new JLabel("Đang tìm đối thủ...", SwingConstants.CENTER);
        label.setFont(Theme.labelFont(14));
        label.setBorder(new EmptyBorder(30, 30, 10, 30));
        RoundedButton cancelBtn = new RoundedButton("Hủy tìm");
        cancelBtn.setBackground(Theme.TERRACOTTA);
        cancelBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        cancelBtn.addActionListener(e -> {
            sendMessage(new Message(Message.Type.CANCEL_FIND));
            closeFindMatchDialog();
        });
        JPanel btnRow = new JPanel();
        btnRow.setBorder(new EmptyBorder(0, 0, 20, 0));
        btnRow.add(cancelBtn);
        dialog.add(label, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setSize(320, 160);
        dialog.setLocationRelativeTo(this);
        return dialog;
    }

    private void closeFindMatchDialog() {
        if (findMatchDialog != null) {
            findMatchDialog.dispose();
            findMatchDialog = null;
        }
    }

    private void onPlayAI() {
        String[] options = {"Dễ", "Trung bình", "Khó"};
        int choice = JOptionPane.showOptionDialog(this, "Chọn độ khó AI:", "Chơi với AI",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        String difficulty = (choice == 0) ? "EASY" : (choice == 1) ? "MEDIUM" : "HARD";

        AIGameFrame aiFrame = new AIGameFrame(currentUser, difficulty);
        aiFrame.setVisible(true);
    }

    private void onCreateRoom() {
        sendMessage(new Message(Message.Type.CREATE_ROOM));
    }

    private void onJoinRoom() {
        String code = JOptionPane.showInputDialog(this, "Nhập mã phòng:", "Tham gia phòng",
                JOptionPane.PLAIN_MESSAGE);
        if (code == null || code.trim().isEmpty()) return;
        Message m = new Message(Message.Type.JOIN_ROOM);
        m.roomCode = code.trim().toUpperCase();
        sendMessage(m);
    }

    private void onRoomList() {
        sendMessage(new Message(Message.Type.ROOM_LIST));
    }

    private void showRoomListDialog(List<RoomInfo> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hiện không có phòng nào đang chờ.",
                    "Danh sách phòng", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] items = new String[rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            RoomInfo r = rooms.get(i);
            items[i] = r.roomCode + "  -  Chủ phòng: " + r.hostName;
        }
        String chosen = (String) JOptionPane.showInputDialog(this, "Chọn phòng để tham gia:",
                "Danh sách phòng đang chờ", JOptionPane.PLAIN_MESSAGE, null, items, items[0]);
        if (chosen == null) return;
        String code = chosen.split(" ")[0];
        Message m = new Message(Message.Type.JOIN_ROOM);
        m.roomCode = code;
        sendMessage(m);
    }

    private void onLeaderboard() {
        LeaderboardFrame frame = new LeaderboardFrame(serverIp, currentUser);
        frame.setVisible(true);
    }

    private void onHistory() {
        HistoryFrame frame = new HistoryFrame(serverIp, currentUser);
        frame.setVisible(true);
    }

    private void onProfile() {
        ProfileFrame frame = new ProfileFrame(serverIp, currentUser, updatedUser -> {
            welcomeLabel.setText("Xin chào, " + updatedUser.getDisplayName()
                    + "  |  Rating: " + updatedUser.getRating());
        });
        frame.setVisible(true);
    }

    private void onLogout() {
        connectionHandedOff = true;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    // ---------- Gửi message ----------

    private synchronized void sendMessage(Message m) {
        try {
            if (out == null) return;
            out.writeObject(m);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.out.println("[LobbyFrame] Lỗi gửi message: " + e.getMessage());
        }
    }
}
