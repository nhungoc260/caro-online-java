package caro.client;

import caro.common.Message;
import caro.common.User;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Bảng xếp hạng người chơi theo rating. */
public class LeaderboardFrame extends JFrame {

    public LeaderboardFrame(String serverIp, User currentUser) {
        setTitle("Cờ Caro Online - Bảng xếp hạng");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        JLabel title = new JLabel("BẢNG XẾP HẠNG", SwingConstants.CENTER);
        title.setFont(Theme.titleFont(20));
        title.setForeground(Theme.WOOD_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(18, 0, 14, 0));
        add(title, BorderLayout.NORTH);

        String[] columns = {"Hạng", "Tên", "Rating", "Trận", "Thắng", "Thua", "Hòa", "Tỷ lệ thắng"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(Theme.labelFont(13));
        table.getTableHeader().setFont(Theme.boldFont(13));
        add(new JScrollPane(table), BorderLayout.CENTER);

        setSize(560, 480);
        setLocationRelativeTo(null);

        new Thread(() -> {
            Message response = NetUtil.sendGameRequest(serverIp, currentUser,
                    new Message(Message.Type.LEADERBOARD));
            SwingUtilities.invokeLater(() -> populate(model, response));
        }).start();
    }

    private void populate(DefaultTableModel model, Message response) {
        if (response == null || response.leaderboard == null) {
            JOptionPane.showMessageDialog(this,
                    "Không thể tải bảng xếp hạng. Kiểm tra kết nối server.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<User> list = response.leaderboard;
        int rank = 1;
        for (User u : list) {
            model.addRow(new Object[]{
                    rank++, u.getDisplayName(), u.getRating(), u.getTotalGames(),
                    u.getWins(), u.getLosses(), u.getDraws(),
                    String.format("%.1f%%", u.getWinRate())
            });
        }
        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa có dữ liệu xếp hạng.",
                    "Bảng xếp hạng", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
