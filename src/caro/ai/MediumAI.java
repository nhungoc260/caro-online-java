package caro.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AI mức TRUNG BÌNH:
 *   1. Nếu có nước đi giúp AI thắng ngay -> đánh luôn.
 *   2. Nếu đối thủ sắp thắng (còn 1 nước là đủ 5) -> chặn ngay ô đó.
 *   3. Ngược lại, ưu tiên ô tạo được chuỗi liên tiếp DÀI NHẤT cho AI
 *      (khuyến khích tạo chuỗi 3, 4 quân).
 *   4. Nếu bàn cờ trống hoàn toàn -> đánh vào giữa bàn.
 */
public class MediumAI implements CaroAI {

    private final Random random = new Random();

    @Override
    public int[] nextMove(int[][] board, int aiPlayerId, int humanPlayerId) {
        List<int[]> emptyCells = collectEmptyCells(board);
        if (emptyCells.isEmpty()) return null;

        if (isBoardEmpty(board)) {
            return new int[]{SIZE / 2, SIZE / 2};
        }

        for (int[] cell : emptyCells) {
            if (wouldWin(board, cell[0], cell[1], aiPlayerId)) return cell;
        }

        for (int[] cell : emptyCells) {
            if (wouldWin(board, cell[0], cell[1], humanPlayerId)) return cell;
        }

        int[] best = null;
        int bestScore = -1;
        for (int[] cell : emptyCells) {
            int score = maxLineIfPlaced(board, cell[0], cell[1], aiPlayerId);
            if (score > bestScore) {
                bestScore = score;
                best = cell;
            }
        }
        return best != null ? best : emptyCells.get(random.nextInt(emptyCells.size()));
    }

    private boolean isBoardEmpty(int[][] board) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] != 0) return false;
            }
        }
        return true;
    }

    private List<int[]> collectEmptyCells(int[][] board) {
        List<int[]> list = new ArrayList<int[]>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) list.add(new int[]{i, j});
            }
        }
        return list;
    }

    /** Nếu đặt "player" vào (x,y) thì có tạo đủ 5 quân liên tiếp không? */
    static boolean wouldWin(int[][] board, int x, int y, int player) {
        return maxLineIfPlaced(board, x, y, player) >= 5;
    }

    /** Nếu đặt "player" vào (x,y), chuỗi liên tiếp DÀI NHẤT (theo 1 trong 4 hướng) là bao nhiêu? */
    static int maxLineIfPlaced(int[][] board, int x, int y, int player) {
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        int best = 1;
        for (int[] d : directions) {
            int count = 1;
            count += countDirection(board, x, y, d[0], d[1], player);
            count += countDirection(board, x, y, -d[0], -d[1], player);
            if (count > best) best = count;
        }
        return best;
    }

    private static int countDirection(int[][] board, int x, int y, int dx, int dy, int player) {
        int count = 0;
        int nx = x + dx, ny = y + dy;
        while (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && board[nx][ny] == player) {
            count++;
            nx += dx;
            ny += dy;
        }
        return count;
    }
}
