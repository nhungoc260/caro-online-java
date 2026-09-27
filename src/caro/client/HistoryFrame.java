package caro.client;

import caro.common.MatchSummary;
import caro.common.Message;
import caro.common.User;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/** Lịch sử trận đấu (online lẫn AI) của người chơi hiện tại. */
public class HistoryFrame extends JFrame {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public HistoryFrame(String serverIp, User currentUser) {
        setTitle("Cờ Caro Online - Lịch sử trận đấu");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        JLabel title = new JLabel("LỊCH SỬ TRẬN ĐẤU", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(20));
        title.setForeground(Theme.WOOD_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(18, 0, 14, 0));
        add(title, BorderLayout.NORTH);

        String[] columns = {"Ngày giờ", "Đối thủ", "Chế độ", "Kết quả", "Thời gian"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(Theme.labelFont(13));
        table.getTableHeader().setFont(Theme.boldFont(13));
        add(new JScrollPane(table), BorderLayout.CENTER);

        setSize(620, 480);
        setLocationRelativeTo(null);

        new Thread(() -> {
            Message response = NetUtil.sendGameRequest(serverIp, currentUser,
                    new Message(Message.Type.MATCH_HISTORY));
            SwingUtilities.invokeLater(() -> populate(model, response));
        }).start();
    }

    private void populate(DefaultTableModel model, Message response) {
        if (response == null || response.matchHistory == null) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tải lịch sử trận đấu. Kiểm tra kết nối server.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<MatchSummary> list = response.matchHistory;
        for (MatchSummary m : list) {
            String duration = m.durationSeconds == null ? "-" : formatDuration(m.durationSeconds);
            String result = translateResult(m.result);
            String mode = "AI".equals(m.mode) ? "AI" : "ONLINE";
            model.addRow(new Object[]{
                    m.startedAt != null ? DATE_FMT.format(m.startedAt) : "-",
                    m.opponentName != null ? m.opponentName : "?",
                    mode, result, duration
            });
        }
        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn chưa có trận đấu nào.",
                    "Lịch sử trận đấu", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String translateResult(String result) {
        if ("WIN".equals(result)) return "THẮNG";
        if ("LOSS".equals(result)) return "THUA";
        if ("DRAW".equals(result)) return "HÒA";
        if ("ABANDONED".equals(result)) return "ĐỐI THỦ THOÁT";
        return result;
    }

    private String formatDuration(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
