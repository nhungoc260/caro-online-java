package caro.client;

import caro.common.Message;
import caro.common.User;
import caro.ui.RoundedButton;
import caro.ui.RoundedPanel;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

/** Hồ sơ cá nhân: xem thống kê + đổi tên hiển thị. */
public class ProfileFrame extends JFrame {

    private final String serverIp;
    private User user;
    private final Consumer<User> onUpdated;

    private JLabel statsLabel;
    private JTextField displayNameField;
    private JLabel messageLabel;

    public ProfileFrame(String serverIp, User user, Consumer<User> onUpdated) {
        this.serverIp = serverIp;
        this.user = user;
        this.onUpdated = onUpdated;

        setTitle("Cờ Caro Online - Hồ sơ");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        add(buildContent(), BorderLayout.CENTER);

        setSize(420, 480);
        setLocationRelativeTo(null);
    }

    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CREAM);
        wrapper.setBorder(new EmptyBorder(24, 24, 24, 24));

        RoundedPanel card = new RoundedPanel(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBg(Theme.WHITE);
        card.setBorder(new EmptyBorder(24, 24, 20, 24));

        JLabel title = new JLabel("HỒ SƠ CÁ NHÂN", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(18));
        title.setForeground(Theme.WOOD_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel usernameLabel = new JLabel("@" + user.getUsername(), SwingConstants.CENTER);
        usernameLabel.setFont(Theme.labelFont(13));
        usernameLabel.setForeground(Theme.TEXT_MUTED);
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameLabel.setBorder(new EmptyBorder(2, 0, 16, 0));

        statsLabel = new JLabel();
        statsLabel.setFont(Theme.labelFont(13));
        statsLabel.setForeground(Theme.TEXT_DARK);
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        refreshStatsLabel();

        JLabel nameFieldLabel = new JLabel("Đổi tên hiển thị:");
        nameFieldLabel.setFont(Theme.boldFont(12));
        nameFieldLabel.setForeground(Theme.TEXT_DARK);
        nameFieldLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameFieldLabel.setBorder(new EmptyBorder(18, 0, 6, 0));

        displayNameField = new JTextField(user.getDisplayName());
        displayNameField.setMaximumSize(new Dimension(300, 36));
        displayNameField.setFont(Theme.labelFont(14));
        displayNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRID_LINE, 1),
                new EmptyBorder(6, 10, 6, 10)));
        displayNameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setFont(Theme.labelFont(12));
        messageLabel.setForeground(Theme.TERRACOTTA);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setBorder(new EmptyBorder(8, 0, 8, 0));

        RoundedButton saveButton = new RoundedButton("LƯU THAY ĐỔI");
        saveButton.setBackground(Theme.MOSS_GREEN);
        saveButton.setBorder(new EmptyBorder(10, 24, 10, 24));
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.addActionListener(e -> onSaveClick());

        card.add(title);
        card.add(usernameLabel);
        card.add(statsLabel);
        card.add(nameFieldLabel);
        card.add(displayNameField);
        card.add(messageLabel);
        card.add(saveButton);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        wrapper.add(card, c);
        return wrapper;
    }

    private void refreshStatsLabel() {
        String joined = user.getCreatedAt() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(user.getCreatedAt()) : "-";
        statsLabel.setText("<html><div style='text-align:center'>"
                + "Ngày tham gia: " + joined + "<br>"
                + "Rating: <b>" + user.getRating() + "</b><br>"
                + "Tổng trận: " + user.getTotalGames() + "<br>"
                + "Thắng: " + user.getWins() + "  -  Thua: " + user.getLosses()
                + "  -  Hòa: " + user.getDraws() + "<br>"
                + "Tỷ lệ thắng: " + String.format("%.1f%%", user.getWinRate())
                + "</div></html>");
    }

    private void onSaveClick() {
        String newName = displayNameField.getText().trim();
        if (newName.isEmpty()) {
            messageLabel.setForeground(Theme.TERRACOTTA);
            messageLabel.setText("Tên hiển thị không được để trống.");
            return;
        }
        messageLabel.setForeground(Theme.MOSS_GREEN);
        messageLabel.setText("Đang lưu...");

        Message req = new Message(Message.Type.PROFILE_UPDATE);
        req.displayName = newName;

        new Thread(() -> {
            Message response = NetUtil.sendGameRequest(serverIp, user, req);
            SwingUtilities.invokeLater(() -> handleSaveResponse(response));
        }).start();
    }

    private void handleSaveResponse(Message response) {
        if (response != null && response.type == Message.Type.PROFILE_UPDATED && response.user != null) {
            user = response.user;
            refreshStatsLabel();
            messageLabel.setForeground(Theme.MOSS_GREEN);
            messageLabel.setText("Đã lưu thành công!");
            if (onUpdated != null) onUpdated.accept(user);
        } else {
            messageLabel.setForeground(Theme.TERRACOTTA);
            messageLabel.setText(response != null && response.note != null
                    ? response.note : "Không thể lưu thay đổi.");
        }
    }
}
