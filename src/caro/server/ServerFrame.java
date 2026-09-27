package caro.server;

import caro.ui.RoundedButton;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Cửa sổ chạy Server dạng GIAO DIỆN (thay vì chỉ xem console NetBeans) -
 * dùng để CHIẾU MÀN HÌNH khi demo, dễ nhìn từ xa hơn khung Output thô.
 *
 * KHÔNG đổi bất kỳ logic server nào - chỉ "bọc" 1 lớp giao diện bên ngoài:
 *  1. Chuyển hướng System.out/System.err để mọi dòng log (Server/AuthServer/
 *     RoomManager/GameRoom/ClientHandler đang println như cũ) tự động chảy
 *     vào khung log trên cửa sổ này, đồng thời VẪN in ra console gốc.
 *  2. Gọi CaroServer.main(...) y hệt như chạy file đó bình thường, chỉ chạy
 *     trên 1 Thread nền để không treo giao diện Swing.
 *
 * CÁCH DÙNG KHI DEMO: chạy file này (Shift+F6) THAY CHO CaroServer.java.
 * Chạy CaroServer.java trực tiếp vẫn hoạt động bình thường (không đổi gì),
 * chỉ là không có giao diện đẹp để chiếu thôi.
 */
public class ServerFrame extends JFrame {

    private JTextArea logArea;
    private JLabel statusBadge;
    private int fontSize = 15;

    public ServerFrame() {
        setTitle("Cờ Caro Online - SERVER");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildLogPanel(), BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);

        setSize(980, 640);
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(null);

        redirectSystemOutToLog();
        startServerInBackground();
    }

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
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CARO ONLINE - SERVER");
        title.setFont(Theme.titleFont(22));
        title.setForeground(Theme.WHITE);
        JLabel subtitle = new JLabel("Cổng chơi game: 12345   |   Cổng xác thực (đăng nhập/đăng ký): 12346");
        subtitle.setFont(Theme.labelFont(13));
        subtitle.setForeground(new Color(224, 209, 195));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        statusBadge = new JLabel("● ĐANG KHỞI ĐỘNG...");
        statusBadge.setFont(Theme.boldFont(14));
        statusBadge.setForeground(Theme.AMBER_EARTH);

        header.add(left, BorderLayout.WEST);
        header.add(statusBadge, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildLogPanel() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(false);
        logArea.setBackground(new Color(45, 32, 28)); // nâu rất đậm, giống terminal cổ điển nhưng hợp tông gỗ
        logArea.setForeground(new Color(232, 213, 188)); // chữ kem sáng, dễ đọc từ xa khi chiếu
        logArea.setCaretColor(Color.WHITE);
        logArea.setFont(new Font("Consolas", Font.PLAIN, fontSize));
        logArea.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        // Luôn tự cuộn xuống dòng log mới nhất
        DefaultCaret caretFix = (DefaultCaret) logArea.getCaret();
        caretFix.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        return scroll;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel();
        bar.setBackground(Theme.CREAM);
        bar.setBorder(new EmptyBorder(10, 0, 10, 0));

        RoundedButton biggerFont = new RoundedButton("Chữ to hơn (A+)");
        biggerFont.setBackground(Theme.WOOD_MEDIUM);
        biggerFont.setBorder(new EmptyBorder(8, 16, 8, 16));
        biggerFont.addActionListener(e -> changeFontSize(2));

        RoundedButton smallerFont = new RoundedButton("Chữ nhỏ hơn (A-)");
        smallerFont.setBackground(Theme.TAUPE_WAIT);
        smallerFont.setBorder(new EmptyBorder(8, 16, 8, 16));
        smallerFont.addActionListener(e -> changeFontSize(-2));

        RoundedButton clearButton = new RoundedButton("Xóa log");
        clearButton.setBackground(Theme.TERRACOTTA);
        clearButton.setBorder(new EmptyBorder(8, 16, 8, 16));
        clearButton.addActionListener(e -> logArea.setText(""));

        bar.add(biggerFont);
        bar.add(smallerFont);
        bar.add(clearButton);
        return bar;
    }

    private void changeFontSize(int delta) {
        fontSize = Math.max(10, Math.min(32, fontSize + delta));
        logArea.setFont(new Font("Consolas", Font.PLAIN, fontSize));
    }

    /**
     * Chuyển hướng System.out/System.err để mọi println có sẵn trong
     * CaroServer/AuthServer/RoomManager/GameRoom/ClientHandler đều tự động
     * hiện lên khung log này - KHÔNG cần sửa bất kỳ dòng println nào ở
     * những file đó. Vẫn giữ in ra console gốc (NetBeans Output) song song,
     * để không mất khả năng debug bằng console nếu cần.
     */
    private void redirectSystemOutToLog() {
        PrintStream original = System.out;
        OutputStream teeStream = new OutputStream() {
            private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

            @Override
            public void write(int b) {
                original.write(b);
                if (b == '\n') {
                    final String line = decodeUtf8(buffer);
                    buffer.reset();
                    SwingUtilities.invokeLater(() -> appendLog(line));
                } else {
                    buffer.write(b);
                }
            }

            private String decodeUtf8(java.io.ByteArrayOutputStream buf) {
                try {
                    return new String(buf.toByteArray(), "UTF-8") + "\n";
                } catch (java.io.UnsupportedEncodingException e) {
                    return buf.toString() + "\n"; // thực tế không xảy ra, UTF-8 luôn có sẵn
                }
            }
        };
        PrintStream teePrintStream;
        try {
            teePrintStream = new PrintStream(teeStream, true, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            teePrintStream = new PrintStream(teeStream, true);
        }
        System.setOut(teePrintStream);
        System.setErr(teePrintStream);
    }

    private void appendLog(String line) {
        logArea.append(line);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void startServerInBackground() {
        Thread t = new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                statusBadge.setText("● ĐANG CHẠY");
                statusBadge.setForeground(new Color(140, 220, 120));
            });
            CaroServer.main(new String[0]); // gọi lại đúng logic server gốc, không đổi gì
        }, "ServerFrame-CaroServer");
        t.setDaemon(true);
        t.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerFrame().setVisible(true));
    }
}
