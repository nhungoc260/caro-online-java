package caro.client;

import caro.server.AuthServer;
import caro.ui.RoundedButton;
import caro.ui.RoundedPanel;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Màn hình ĐẦU TIÊN của ứng dụng: nhập IP máy chạy Server (mạng LAN / wifi).
 *
 *  - Máy chạy server (người chơi 1): nhập "localhost".
 *  - Máy còn lại (người chơi 2): nhập IP LAN/wifi của máy chạy server,
 *    ví dụ 192.168.1.5 (xem bằng lệnh ipconfig hoặc trong cửa sổ Server).
 *
 * Bấm KẾT NỐI: app thử kết nối tới server. Thành công mới mở màn hình
 * Đăng nhập / Đăng ký (cả hai đều dùng đúng IP này). Không cần chạy server
 * hay MySQL trên máy người chơi 2.
 */
public class ConnectFrame extends JFrame {

    private JTextField ipField;
    private JLabel messageLabel;
    private RoundedButton connectButton;

    public ConnectFrame() {
        this("localhost");
    }

    public ConnectFrame(String defaultIp) {
        setTitle("Cờ Caro Online - Kết nối server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildFormPanel(defaultIp), BorderLayout.CENTER);

        setSize(460, 470);
        setMinimumSize(new Dimension(400, 440));
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(connectButton);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.WOOD_DARK, 0, getHeight(), Theme.WOOD_MEDIUM));
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

        JLabel subtitle = new JLabel("Kết nối tới server qua mạng LAN / Wifi", SwingConstants.CENTER);
        subtitle.setFont(Theme.labelFont(13));
        subtitle.setForeground(new Color(224, 209, 195));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(6, 0, 0, 0));

        header.add(title);
        header.add(subtitle);
        return header;
    }

    private JPanel buildFormPanel(String defaultIp) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.CREAM);
        wrapper.setBorder(new EmptyBorder(24, 24, 24, 24));

        RoundedPanel card = new RoundedPanel(new GridBagLayout());
        card.setBg(Theme.WHITE);
        card.setBorder(new EmptyBorder(24, 24, 20, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int row = 0;

        JLabel label = new JLabel("IP máy chạy Server");
        label.setFont(Theme.boldFont(12));
        label.setForeground(Theme.TEXT_DARK);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 6, 0);
        card.add(label, c);

        ipField = new JTextField(defaultIp == null || defaultIp.trim().isEmpty() ? "localhost" : defaultIp.trim());
        ipField.setFont(Theme.labelFont(15));
        ipField.setBackground(Theme.INPUT_BG);
        ipField.setForeground(Theme.TEXT_DARK);
        ipField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRID_LINE, 1),
                new EmptyBorder(8, 10, 8, 10)));
        c.gridy = row++;
        card.add(ipField, c);

        JLabel hint = new JLabel("<html>Máy chạy server: nhập <b>localhost</b>.<br>"
                + "Máy khác: nhập IP wifi/LAN của máy chạy server (VD 192.168.1.5).</html>");
        hint.setFont(Theme.labelFont(12));
        hint.setForeground(Theme.TEXT_MUTED);
        c.gridy = row++;
        c.insets = new Insets(8, 0, 6, 0);
        card.add(hint, c);

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setFont(Theme.labelFont(13));
        messageLabel.setForeground(Theme.TERRACOTTA);
        c.gridy = row++;
        c.insets = new Insets(10, 0, 4, 0);
        card.add(messageLabel, c);

        connectButton = new RoundedButton("KẾT NỐI");
        connectButton.setBackground(Theme.MOSS_GREEN);
        connectButton.setBorder(new EmptyBorder(12, 0, 12, 0));
        connectButton.addActionListener(e -> onConnectClick());
        c.gridy = row++;
        c.insets = new Insets(8, 0, 0, 0);
        card.add(connectButton, c);

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

    private void onConnectClick() {
        String typed = ipField.getText().trim();
        final String ip = typed.isEmpty() ? "localhost" : typed;

        connectButton.setEnabled(false);
        ipField.setEnabled(false);
        showMessage("Đang kết nối tới " + ip + " ...", false);

        // Thử mở TCP tới AuthServer (timeout 3s) trên Thread riêng để không đơ giao diện
        new Thread(() -> {
            boolean ok = canReach(ip);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    dispose();
                    new LoginFrame(ip).setVisible(true);
                } else {
                    connectButton.setEnabled(true);
                    ipField.setEnabled(true);
                    showMessage("<html><center>Không kết nối được tới " + ip + ".<br>"
                            + "Kiểm tra: server đã chạy chưa, cùng wifi chưa, firewall.</center></html>", true);
                }
            });
        }).start();
    }

    private static boolean canReach(String ip) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, AuthServer.AUTH_PORT), 3000);
            return true;
        } catch (Exception e) {
            System.out.println("[ConnectFrame] Không kết nối được " + ip + ": " + e.getMessage());
            return false;
        }
    }

    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.setForeground(isError ? Theme.TERRACOTTA : Theme.MOSS_GREEN);
    }

    /** Điểm khởi động của ứng dụng Client. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConnectFrame().setVisible(true));
    }
}
