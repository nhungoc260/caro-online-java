package caro.client;

import caro.ai.CaroAI;
import caro.ai.EasyAI;
import caro.ai.HardAI;
import caro.ai.MediumAI;
import caro.common.User;
import caro.database.DatabaseConnection;
import caro.database.MatchDAO;
import caro.database.MoveDAO;
import caro.database.UserDAO;
import caro.ui.RoundedButton;
import caro.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Bàn cờ chơi với AI - chạy HOÀN TOÀN LOCAL trên máy client, KHÔNG cần
 * TCP/Server (đúng theo mục XV của yêu cầu: "AI không cần dùng TCP nếu
 * thiết kế local"). Kết quả trận vẫn được lưu vào MySQL, nhưng client tự
 * gọi thẳng DAO (giống cách server làm) vì đây là chế độ chơi đơn/offline,
 * không có tranh chấp trạng thái giữa 2 máy cần 1 "trọng tài" trung gian.
 */
public class AIGameFrame extends JFrame {

    private static final int SIZE = 15;
    private static final int HUMAN = 1;
    private static final int AI = 2;

    private final int[][] board = new int[SIZE][SIZE];
    private final JButton[][] cells = new JButton[SIZE][SIZE];
    private final User currentUser;
    private final String difficulty;
    private final CaroAI ai;
    private final int aiApproxRating;

    private boolean gameOver = false;
    private boolean humanTurn = true;
    private int moveCounter = 0;
    private int lastX = -1, lastY = -1;

    private Integer matchId; // null nếu user chưa đăng nhập (không nên xảy ra vì AIGameFrame chỉ mở từ Lobby)
    private long startTimeMillis;

    private JLabel statusLabel;

    public AIGameFrame(User user, String difficulty) {
        this.currentUser = user;
        this.difficulty = difficulty;
        if ("EASY".equals(difficulty)) {
            ai = new EasyAI();
            aiApproxRating = 800;
        } else if ("HARD".equals(difficulty)) {
            ai = new HardAI();
            aiApproxRating = 1300;
        } else {
            ai = new MediumAI();
            aiApproxRating = 1000;
        }

        setTitle("Cờ Caro Online - Chơi với AI (" + vietnameseDifficulty() + ")");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CREAM);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBoard(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        setSize(760, 840);
        setMinimumSize(new Dimension(600, 680));
        setLocationRelativeTo(null);

        startNewMatch();
    }

    private String vietnameseDifficulty() {
        if ("EASY".equals(difficulty)) return "Dễ";
        if ("HARD".equals(difficulty)) return "Khó";
        return "Trung bình";
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, Theme.WOOD_DARK, 0, getHeight(), Theme.WOOD_MEDIUM);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel(currentUser.getDisplayName() + "  (X)   vs   AI - " + vietnameseDifficulty() + "  (O)");
        title.setFont(Theme.boldFont(15));
        title.setForeground(Theme.WHITE);

        statusLabel = new JLabel("Lượt của bạn", SwingConstants.RIGHT);
        statusLabel.setFont(Theme.boldFont(14));
        statusLabel.setForeground(new Color(224, 209, 195));

        header.add(title, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBoard() {
        JPanel boardPanel = new JPanel(new GridLayout(SIZE, SIZE));
        boardPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        boardPanel.setBackground(Theme.CREAM);

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                final int x = i, y = j;
                JButton cell = new JButton();
                cell.setFont(new Font("Segoe UI", Font.BOLD, 16));
                cell.setBackground(((i + j) % 2 == 0) ? Theme.CREAM : Theme.BEIGE);
                cell.setBorder(BorderFactory.createLineBorder(Theme.GRID_LINE, 1));
                cell.setFocusPainted(false);
                cell.addActionListener(e -> onCellClick(x, y));
                cells[i][j] = cell;
                boardPanel.add(cell);
            }
        }
        return boardPanel;
    }

    private JPanel buildBottom() {
        JPanel bottom = new JPanel();
        bottom.setBackground(Theme.CREAM);
        bottom.setBorder(new EmptyBorder(0, 0, 14, 0));

        RoundedButton backButton = new RoundedButton("VỀ SẢNH CHỜ");
        backButton.setBackground(Theme.WOOD_MEDIUM);
        backButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        backButton.addActionListener(e -> dispose());

        bottom.add(backButton);
        return bottom;
    }

    // ---------- Logic ván đấu ----------

    private void startNewMatch() {
        startTimeMillis = System.currentTimeMillis();
        moveCounter = 0;
        matchId = null;
        if (currentUser != null) {
            new Thread(() -> {
                try {
                    int id = new MatchDAO().createMatch(currentUser.getId(), null, "AI", null, difficulty);
                    matchId = id;
                } catch (DatabaseConnection.DatabaseException e) {
                    System.out.println("[AIGameFrame] Không thể tạo match trong DB: " + e.getMessage());
                }
            }).start();
        }
    }

    private void onCellClick(int x, int y) {
        if (gameOver || !humanTurn || board[x][y] != 0) return;
        placeMark(x, y, HUMAN);
        if (checkGameEnd(x, y, HUMAN)) return;

        humanTurn = false;
        statusLabel.setText("AI đang suy nghĩ...");
        setBoardEnabled(false);

        // Chạy AI trên thread riêng để không treo giao diện (đặc biệt là HardAI)
        new Thread(() -> {
            int[] move = ai.nextMove(board, AI, HUMAN);
            SwingUtilities.invokeLater(() -> {
                if (gameOver || move == null) return;
                placeMark(move[0], move[1], AI);
                if (!checkGameEnd(move[0], move[1], AI)) {
                    humanTurn = true;
                    setBoardEnabled(true);
                    statusLabel.setText("Lượt của bạn");
                }
            });
        }, "AI-Thinking").start();
    }

    private void placeMark(int x, int y, int player) {
        board[x][y] = player;
        moveCounter++;
        JButton cell = cells[x][y];
        cell.setText(player == HUMAN ? "X" : "O");
        cell.setForeground(player == HUMAN ? new Color(56, 118, 29) : new Color(150, 40, 27));
        cell.setEnabled(false);

        if (lastX != -1) {
            cells[lastX][lastY].setBackground(((lastX + lastY) % 2 == 0) ? Theme.CREAM : Theme.BEIGE);
        }
        cell.setBackground(new Color(255, 224, 130));
        lastX = x; lastY = y;

        if (currentUser != null && matchId != null) {
            final int mid = matchId, mv = moveCounter;
            new Thread(() -> {
                try {
                    new MoveDAO().saveMove(mid, currentUser.getId(), x, y, mv);
                } catch (DatabaseConnection.DatabaseException e) {
                    System.out.println("[AIGameFrame] Lỗi lưu nước đi: " + e.getMessage());
                }
            }).start();
        }
    }

    /** @return true nếu ván đã kết thúc (thắng/hòa), false nếu vẫn tiếp tục */
    private boolean checkGameEnd(int x, int y, int player) {
        if (checkWin(x, y, player)) {
            gameOver = true;
            setBoardEnabled(false);
            boolean humanWon = (player == HUMAN);
            statusLabel.setText(humanWon ? "BẠN THẮNG!" : "BẠN THUA!");
            JOptionPane.showMessageDialog(this, humanWon ? "Chúc mừng, bạn đã thắng AI!" : "AI đã thắng. Chơi lại nhé!",
                    humanWon ? "Chiến thắng" : "Thua cuộc", JOptionPane.INFORMATION_MESSAGE);
            saveResult(humanWon ? "WIN" : "LOSS");
            return true;
        }
        if (isBoardFull()) {
            gameOver = true;
            setBoardEnabled(false);
            statusLabel.setText("HÒA!");
            JOptionPane.showMessageDialog(this, "Ván đấu hòa!", "Hòa", JOptionPane.INFORMATION_MESSAGE);
            saveResult("DRAW");
            return true;
        }
        return false;
    }

    private void saveResult(String outcome) {
        if (currentUser == null) return;
        new Thread(() -> {
            try {
                int durationSec = (int) Math.max(0, (System.currentTimeMillis() - startTimeMillis) / 1000);
                if (matchId != null) {
                    Integer winnerUserId = "WIN".equals(outcome) ? currentUser.getId() : null;
                    new MatchDAO().finishMatch(matchId, winnerUserId, outcome, durationSec);
                }
                new UserDAO().updateStatsAfterMatch(currentUser.getId(), outcome, aiApproxRating);
            } catch (DatabaseConnection.DatabaseException e) {
                System.out.println("[AIGameFrame] Lỗi lưu kết quả: " + e.getMessage());
            }
        }).start();
    }

    private boolean checkWin(int x, int y, int player) {
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : directions) {
            int count = 1;
            count += countDir(x, y, d[0], d[1], player);
            count += countDir(x, y, -d[0], -d[1], player);
            if (count >= 5) return true;
        }
        return false;
    }

    private int countDir(int x, int y, int dx, int dy, int player) {
        int count = 0;
        int nx = x + dx, ny = y + dy;
        while (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && board[nx][ny] == player) {
            count++;
            nx += dx; ny += dy;
        }
        return count;
    }

    private boolean isBoardFull() {
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                if (board[i][j] == 0) return false;
        return true;
    }

    private void setBoardEnabled(boolean enabled) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) cells[i][j].setEnabled(enabled);
            }
        }
    }
}
