package caro.client;

import caro.common.Message;
import caro.common.User;
import caro.server.AuthServer;
import caro.ui.RoundedButton;
import caro.ui.RoundedPanel;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Màn hình ĐĂNG NHẬP - điểm khởi đầu mới của ứng dụng Client (Phase 2).
 *
 * Luồng hoạt động:
 *   LoginFrame (main) -> kết nối AuthServer (port 12346) -> gửi LOGIN
 *   -> nhận LOGIN_SUCCESS/LOGIN_FAILED -> nếu thành công, đóng LoginFrame
 *   và mở CaroClient (game) như luồng cũ, dùng displayName thật từ DB.
 *
 * LƯU Ý: Ở Phase 4 (Lobby), bước "mở CaroClient trực tiếp" bên dưới sẽ được
 * thay bằng "mở LobbyFrame" - hiện tại làm vậy để có thể test toàn bộ luồng
 * đăng nhập -> chơi game thật ngay từ Phase 2, không phải chờ tới Phase 4.
 *
 * CaroClient.java KHÔNG bị sửa gì - LoginFrame chỉ gọi constructor có sẵn
 * của nó (new CaroClient(displayName)) đúng như cách người dùng tự nhập
 * tên trước đây.
 */
public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField serverIpField;
    private JLabel messageLabel;
    private RoundedButton loginButton;
    private RoundedButton registerButton;

    private final String serverIp;

    public LoginFrame(String serverIp) {
        this.serverIp = (serverIp == null || serverIp.trim().isEmpty()) ? "localhost" : serverIp.trim();
        setTitle("Cờ Caro Online - Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);

        setSize(460, 620);
        setMinimumSize(new Dimension(400, 580));
        setLocationRelativeTo(null);

        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel() {
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
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(28, 16, 24, 16));

        JLabel title = new JLabel("CỜ CARO ONLINE", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(26));
        title.setForeground(Theme.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Đăng nhập để bắt đầu", SwingConstants.CENTER);
        subtitle.setFont(Theme.labelFont(13));
        subtitle.setForeground(new Color(224, 209, 195));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(6, 0, 0, 0));

        header.add(title);
        header.add(subtitle);
        return header;
    }

    private JPanel buildFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CREAM);
        wrapper.setBorder(new EmptyBorder(24, 24, 24, 24));

        RoundedPanel card = new RoundedPanel(new GridBagLayout());
        card.setBg(Theme.WHITE);
        card.setBorder(new EmptyBorder(24, 24, 20, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        c.weightx = 1;

        int row = 0;

        c.gridy = row++;
        card.add(fieldLabel("Tên đăng nhập"), c);

        usernameField = styledTextField();
        c.gridy = row++;
        card.add(usernameField, c);

        c.gridy = row++;
        c.insets = new Insets(14, 0, 6, 0);
        card.add(fieldLabel("Mật khẩu"), c);

        passwordField = new JPasswordField();
        styleInput(passwordField);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 6, 0);
        card.add(passwordField, c);

        c.gridy = row++;
        c.insets = new Insets(14, 0, 6, 0);
        card.add(fieldLabel("Server đang kết nối"), c);

        serverIpField = styledTextField();
        serverIpField.setText(serverIp);
        serverIpField.setEditable(false);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 6, 0);
        card.add(serverIpField, c);

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setFont(Theme.labelFont(13));
        messageLabel.setForeground(Theme.TERRACOTTA);
        c.gridy = row++;
        c.insets = new Insets(14, 0, 4, 0);
        card.add(messageLabel, c);

        loginButton = new RoundedButton("ĐĂNG NHẬP");
        loginButton.setBackground(Theme.MOSS_GREEN);
        loginButton.setBorder(new EmptyBorder(12, 0, 12, 0));
        loginButton.addActionListener(e -> onLoginClick());
        c.gridy = row++;
        c.insets = new Insets(8, 0, 10, 0);
        card.add(loginButton, c);

        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        registerRow.setOpaque(false);
        JLabel noAccountLabel = new JLabel("Chưa có tài khoản?");
        noAccountLabel.setFont(Theme.labelFont(13));
        noAccountLabel.setForeground(Theme.TEXT_MUTED);
        registerButton = new RoundedButton("Đăng ký ngay");
        registerButton.setBackground(Theme.WOOD_MEDIUM);
        registerButton.setBorder(new EmptyBorder(6, 14, 6, 14));
        registerButton.setFont(Theme.boldFont(13));
        registerButton.addActionListener(e -> onRegisterClick());
        registerRow.add(noAccountLabel);
        registerRow.add(registerButton);
        c.gridy = row++;
        c.insets = new Insets(4, 0, 0, 0);
        card.add(registerRow, c);

        RoundedButton changeServerButton = new RoundedButton("Đổi server");
        changeServerButton.setBackground(Theme.TAUPE_WAIT);
        changeServerButton.setBorder(new EmptyBorder(6, 14, 6, 14));
        changeServerButton.setFont(Theme.boldFont(12));
        changeServerButton.addActionListener(e -> onChangeServerClick());
        c.gridy = row++;
        c.insets = new Insets(10, 60, 0, 60);
        card.add(changeServerButton, c);

        GridBagConstraints wc = new GridBagConstraints();
        wc.gridx = 0;
        wc.gridy = 0;
        wc.weightx = 1;
        wc.weighty = 1;
        wc.fill = GridBagConstraints.HORIZONTAL;
        wc.anchor = GridBagConstraints.NORTH;
        wrapper.add(card, wc);
        return wrapper;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.boldFont(12));
        label.setForeground(Theme.TEXT_DARK);
        return label;
    }

    private JTextField styledTextField() {
        JTextField field = new JTextField();
        styleInput(field);
        return field;
    }

    private void styleInput(JTextField field) {
        field.setFont(Theme.labelFont(14));
        field.setBackground(Theme.INPUT_BG);
        field.setForeground(Theme.TEXT_DARK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRID_LINE, 1),
                new EmptyBorder(8, 10, 8, 10)));
    }

    // ---------- Xử lý sự kiện ----------

    private void onRegisterClick() {
        RegisterFrame registerFrame = new RegisterFrame(this);
        registerFrame.setVisible(true);
        setVisible(false);
    }

    private void onLoginClick() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        final String finalIp = serverIp;

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", true);
            return;
        }

        setFormEnabled(false);
        showMessage("Đang kết nối tới server...", false);

        Message request = new Message(Message.Type.LOGIN);
        request.username = username;
        request.password = password;

        // Chạy trên Thread riêng để không làm treo giao diện khi đang chờ mạng
        new Thread(() -> {
            Message response = sendAuthRequest(finalIp, request);
            SwingUtilities.invokeLater(() -> handleLoginResponse(response));
        }).start();
    }

    private void handleLoginResponse(Message response) {
        setFormEnabled(true);
        if (response == null) {
            showMessage("Không thể kết nối tới server. Kiểm tra CaroServer đã chạy chưa?", true);
            return;
        }
        if (response.type == Message.Type.LOGIN_SUCCESS) {
            User user = response.user;
            showMessage("Đăng nhập thành công!", false);
            openLobbyAfterLogin(user);
        } else if (response.type == Message.Type.LOGIN_FAILED) {
            showMessage(response.note != null ? response.note : "Đăng nhập thất bại.", true);
        } else {
            showMessage("Phản hồi không hợp lệ từ server.", true);
        }
    }

    /**
     * Sau khi đăng nhập thành công: mở LobbyFrame (sảnh chờ chính).
     */
    private void openLobbyAfterLogin(User user) {
        final String finalIp = serverIp;

        dispose(); // đóng LoginFrame
        SwingUtilities.invokeLater(() -> new LobbyFrame(user, finalIp).setVisible(true));
    }

    /** IP server đã nhập ở màn hình Kết nối. RegisterFrame dùng lại giá trị này. */
    String getServerIp() {
        return serverIp;
    }

    private void onChangeServerClick() {
        dispose();
        new ConnectFrame(serverIp).setVisible(true);
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }

    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.setForeground(isError ? Theme.TERRACOTTA : Theme.MOSS_GREEN);
    }

    // ---------- Giao tiếp mạng với AuthServer ----------

    /**
     * Mở 1 kết nối NGẮN HẠN tới AuthServer, gửi đúng 1 request, nhận đúng 1
     * response rồi đóng kết nối. Đơn giản, dễ hiểu, phù hợp cho 1 request
     * độc lập như LOGIN - không cần giữ kết nối lâu dài như socket chơi game.
     *
     * @return Message phản hồi, hoặc null nếu không kết nối được / có lỗi mạng
     */
    static Message sendAuthRequest(String ip, Message request) {
        try (Socket socket = new Socket(ip, AuthServer.AUTH_PORT)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(request);
            out.flush();

            return (Message) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[LoginFrame] Lỗi kết nối AuthServer: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConnectFrame().setVisible(true));
    }
}
