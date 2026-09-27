package caro.client;

import caro.ui.RoundedButton;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Cửa sổ "phòng chờ" - hiện ra sau khi người dùng TẠO PHÒNG thành công,
 * hiển thị mã phòng để chia sẻ cho bạn bè, và trạng thái đang chờ đối thủ.
 * Tự đóng khi LobbyFrame nhận được START (ván đấu bắt đầu).
 */
public class RoomFrame extends JDialog {

    private JLabel statusLabel;

    public RoomFrame(JFrame owner, String roomCode, Runnable onCancel) {
        super(owner, "Phòng chờ", true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        JPanel content = new JPanel();
        content.setBackground(Theme.CREAM);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(28, 32, 20, 32));

        JLabel title = new JLabel("Mã phòng của bạn:");
        title.setFont(Theme.labelFont(14));
        title.setForeground(Theme.TEXT_MUTED);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel codeLabel = new JLabel(roomCode);
        codeLabel.setFont(Theme.titleFont(36));
        codeLabel.setForeground(Theme.WOOD_DARK);
        codeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        codeLabel.setBorder(new EmptyBorder(8, 0, 16, 0));

        statusLabel = new JLabel("Đang chờ đối thủ tham gia...", SwingConstants.CENTER);
        statusLabel.setFont(Theme.labelFont(13));
        statusLabel.setForeground(Theme.MOSS_GREEN);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Chia sẻ mã này cho bạn bè để họ tham gia", SwingConstants.CENTER);
        hint.setFont(Theme.labelFont(11));
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setBorder(new EmptyBorder(4, 0, 20, 0));

        RoundedButton cancelButton = new RoundedButton("Hủy phòng");
        cancelButton.setBackground(Theme.TERRACOTTA);
        cancelButton.setBorder(new EmptyBorder(10, 24, 10, 24));
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(e -> {
            if (onCancel != null) onCancel.run();
            dispose();
        });

        content.add(title);
        content.add(codeLabel);
        content.add(statusLabel);
        content.add(hint);
        content.add(cancelButton);

        add(content, BorderLayout.CENTER);
        setSize(360, 320);
        setLocationRelativeTo(owner);
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }
}
