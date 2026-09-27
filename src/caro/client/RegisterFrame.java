package caro.client;

import caro.common.Message;
import caro.ui.RoundedButton;
import caro.ui.RoundedPanel;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

/**
 * Màn hình ĐĂNG KÝ tài khoản mới.
 * Validate đầy đủ ở phía Client (phản hồi tức thì, đỡ tốn round-trip mạng),
 * nhưng AuthService ở server vẫn validate lại toàn bộ - client không được
 * tin tưởng tuyệt đối theo nguyên tắc bảo mật của project.
 */
public class RegisterFrame extends JFrame {

    private final LoginFrame parentLogin;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField displayNameField;
    private JLabel messageLabel;
    private RoundedButton registerButton;

    public RegisterFrame(LoginFrame parentLogin) {
        this.parentLogin = parentLogin;

        setTitle("Cờ Caro Online - Đăng ký tài khoản");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);

        setSize(460, 660);
        setMinimumSize(new Dimension(400, 600));
        setLocationRelativeTo(parentLogin);

        // Nếu người dùng đóng cửa sổ đăng ký (bấm X) thay vì bấm "Quay lại
        // đăng nhập", vẫn phải hiện lại LoginFrame - không để mất luôn cả app.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (parentLogin != null) parentLogin.setVisible(true);
            }
        });

        getRootPane().setDefaultButton(registerButton);
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
        header.setBorder(new EmptyBorder(24, 16, 20, 16));

        JLabel title = new JLabel("TẠO TÀI KHOẢN MỚI", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(22));
        title.setForeground(Theme.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        return header;
    }

    private JPanel buildFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CREAM);
        wrapper.setBorder(new EmptyBorder(20, 24, 20, 24));

        RoundedPanel card = new RoundedPanel(new GridBagLayout());
        card.setBg(Theme.WHITE);
        card.setBorder(new EmptyBorder(22, 24, 18, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int row = 0;

        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        card.add(fieldLabel("Tên đăng nhập *"), c);
        usernameField = styledTextField();
        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        card.add(usernameField, c);

        c.gridy = row++; c.insets = new Insets(10, 0, 6, 0);
        card.add(fieldLabel("Mật khẩu (tối thiểu 6 ký tự) *"), c);
        passwordField = new JPasswordField();
        styleInput(passwordField);
        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        card.add(passwordField, c);

        c.gridy = row++; c.insets = new Insets(10, 0, 6, 0);
        card.add(fieldLabel("Xác nhận mật khẩu *"), c);
        confirmPasswordField = new JPasswordField();
        styleInput(confirmPasswordField);
        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        card.add(confirmPasswordField, c);

        c.gridy = row++; c.insets = new Insets(10, 0, 6, 0);
        card.add(fieldLabel("Tên hiển thị (để trống = dùng tên đăng nhập)"), c);
        displayNameField = styledTextField();
        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        card.add(displayNameField, c);

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setFont(Theme.labelFont(13));
        messageLabel.setForeground(Theme.TERRACOTTA);
        c.gridy = row++; c.insets = new Insets(14, 0, 4, 0);
        card.add(messageLabel, c);

        registerButton = new RoundedButton("ĐĂNG KÝ");
        registerButton.setBackground(Theme.MOSS_GREEN);
        registerButton.setBorder(new EmptyBorder(12, 0, 12, 0));
        registerButton.addActionListener(e -> onRegisterClick());
        c.gridy = row++; c.insets = new Insets(8, 0, 10, 0);
        card.add(registerButton, c);

        RoundedButton backButton = new RoundedButton("Quay lại đăng nhập");
        backButton.setBackground(Theme.TAUPE_WAIT);
        backButton.setBorder(new EmptyBorder(10, 0, 10, 0));
        backButton.setFont(Theme.labelFont(13));
        backButton.addActionListener(e -> backToLogin());
        c.gridy = row++; c.insets = new Insets(0, 0, 0, 0);
        card.add(backButton, c);

        GridBagConstraints wc = new GridBagConstraints();
        wc.gridx = 0; wc.gridy = 0; wc.weightx = 1; wc.weighty = 1;
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

    private void backToLogin() {
        dispose();
        if (parentLogin != null) parentLogin.setVisible(true);
    }

    private void onRegisterClick() {
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        char[] confirmChars = confirmPasswordField.getPassword();
        String password = new String(passwordChars);
        String confirm = new String(confirmChars);
        String displayName = displayNameField.getText().trim();

        String validationError = validateClientSide(username, password, confirm);
        if (validationError != null) {
            showError(validationError);
            // Xoá dữ liệu mật khẩu tạm trong mảng char cho an toàn
            Arrays.fill(passwordChars, '0');
            Arrays.fill(confirmChars, '0');
            return;
        }

        setFormEnabled(false);
        showError("Đang gửi yêu cầu đăng ký...");

        Message request = new Message(Message.Type.REGISTER);
        request.username = username;
        request.password = password;
        request.displayName = displayName.isEmpty() ? username : displayName;

        new Thread(() -> {
            Message response = LoginFrame.sendAuthRequest(resolveServerIp(), request);
            SwingUtilities.invokeLater(() -> handleRegisterResponse(response));
        }).start();

        Arrays.fill(passwordChars, '0');
        Arrays.fill(confirmChars, '0');
    }

    private String resolveServerIp() {
        // RegisterFrame không có ô nhập IP riêng (theo đúng thiết kế màn hình
        // đăng ký ở mục V - chỉ có Username/Password/Confirm/DisplayName).
        // Dùng chung server localhost mặc định; nếu cần đăng ký từ máy khác
        // trong LAN, người dùng nên đăng ký trực tiếp trên máy chạy server.
        return "localhost";
    }

    /**
     * Kiểm tra hợp lệ phía Client theo đúng mục V (ĐĂNG KÝ) của yêu cầu:
     * không bỏ trống, username >= 3 ký tự, password >= 6 ký tự, confirm khớp.
     */
    private String validateClientSide(String username, String password, String confirm) {
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            return "Vui lòng nhập đầy đủ các trường bắt buộc (*).";
        }
        if (username.length() < 3) {
            return "Tên đăng nhập phải có ít nhất 3 ký tự.";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }
        if (!password.equals(confirm)) {
            return "Mật khẩu xác nhận không khớp.";
        }
        return null;
    }

    private void handleRegisterResponse(Message response) {
        setFormEnabled(true);
        if (response == null) {
            showError("Không thể kết nối tới server. Kiểm tra CaroServer đã chạy chưa?");
            return;
        }
        if (response.type == Message.Type.REGISTER_SUCCESS) {
            JOptionPane.showMessageDialog(this,
                    "Đăng ký tài khoản thành công.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            backToLogin();
        } else if (response.type == Message.Type.REGISTER_FAILED) {
            showError(response.note != null ? response.note : "Đăng ký thất bại.");
        } else {
            showError("Phản hồi không hợp lệ từ server.");
        }
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmPasswordField.setEnabled(enabled);
        displayNameField.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }

    private void showError(String text) {
        messageLabel.setText(text);
        messageLabel.setForeground(Theme.TERRACOTTA);
    }
}
