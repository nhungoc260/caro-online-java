package caro.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * AI mức KHÓ: dùng heuristic chấm điểm theo pattern (giống hướng dẫn "dùng
 * heuristic hoặc Minimax + Alpha-Beta" - ở đây chọn heuristic scoring vì
 * bàn 15x15 quá lớn để Minimax đầy đủ chạy đủ nhanh mà vẫn dễ hiểu cho
 * sinh viên trình bày).
 *
 * Với MỖI ô trống (chỉ xét các ô gần quân cờ đã có, để tăng tốc), tính:
 *   - điểm TẤN CÔNG: nếu AI đặt vào đây thì các chuỗi AI tạo được mạnh cỡ nào
 *   - điểm PHÒNG THỦ: nếu ĐỐI THỦ đặt vào đây thì họ tạo được chuỗi mạnh cỡ nào
 *     (điểm phòng thủ cao nghĩa là ô đó QUAN TRỌNG PHẢI CHẶN)
 * Tổng 2 điểm lại, chọn ô có tổng điểm cao nhất.
 */
public class HardAI implements CaroAI {

    private static final int SEARCH_RADIUS = 2;

    @Override
    public int[] nextMove(int[][] board, int aiPlayerId, int humanPlayerId) {
        List<int[]> candidates = collectCandidateCells(board);
        if (candidates.isEmpty()) return null;

        int[] best = null;
        long bestScore = Long.MIN_VALUE;
        for (int[] cell : candidates) {
            long offense = evaluateCell(board, cell[0], cell[1], aiPlayerId);
            long defense = evaluateCell(board, cell[0], cell[1], humanPlayerId);
            long total = offense + (long) (defense * 1.05); // ưu tiên phòng thủ nhỉnh hơn 1 chút
            if (total > bestScore) {
                bestScore = total;
                best = cell;
            }
        }
        return best;
    }

    /**
     * Chỉ xét các ô trống nằm trong bán kính SEARCH_RADIUS quanh 1 quân cờ
     * bất kỳ đã có trên bàn - giúp AI chạy nhanh trên bàn 15x15 (225 ô)
     * thay vì phải chấm điểm toàn bộ ô trống mỗi lượt.
     * Nếu bàn cờ trống hoàn toàn (nước đi đầu tiên) -> trả về ô giữa bàn.
     */
    private List<int[]> collectCandidateCells(int[][] board) {
        boolean[][] isCandidate = new boolean[SIZE][SIZE];
        boolean anyStone = false;
        List<int[]> result = new ArrayList<int[]>();

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] != 0) {
                    anyStone = true;
                    for (int di = -SEARCH_RADIUS; di <= SEARCH_RADIUS; di++) {
                        for (int dj = -SEARCH_RADIUS; dj <= SEARCH_RADIUS; dj++) {
                            int ni = i + di, nj = j + dj;
                            if (ni >= 0 && nj >= 0 && ni < SIZE && nj < SIZE
                                    && board[ni][nj] == 0 && !isCandidate[ni][nj]) {
                                isCandidate[ni][nj] = true;
                                result.add(new int[]{ni, nj});
                            }
                        }
                    }
                }
            }
        }

        if (!anyStone) {
            result.add(new int[]{SIZE / 2, SIZE / 2});
        }
        return result;
    }

    /** Tổng điểm heuristic của ô (x,y) nếu "player" đặt quân vào đó, cộng dồn cả 4 hướng. */
    private long evaluateCell(int[][] board, int x, int y, int player) {
        int[][] axes = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        long total = 0;
        for (int[] axis : axes) {
            total += lineScore(board, x, y, axis[0], axis[1], player);
        }
        return total;
    }

    private long lineScore(int[][] board, int x, int y, int dx, int dy, int player) {
        int count = 1; // tính cả ô (x,y) đang xét như thể đã đặt quân

        int nx = x + dx, ny = y + dy;
        while (inBounds(nx, ny) && board[nx][ny] == player) {
            count++;
            nx += dx; ny += dy;
        }
        boolean openForward = inBounds(nx, ny) && board[nx][ny] == 0;

        nx = x - dx; ny = y - dy;
        while (inBounds(nx, ny) && board[nx][ny] == player) {
            count++;
            nx -= dx; ny -= dy;
        }
        boolean openBackward = inBounds(nx, ny) && board[nx][ny] == 0;

        int openEnds = (openForward ? 1 : 0) + (openBackward ? 1 : 0);
        return patternScore(count, openEnds);
    }

    private long patternScore(int count, int openEnds) {
        if (count >= 5) return 1_000_000L;
        if (count == 4) return openEnds >= 1 ? 100_000L : 50L;
        if (count == 3) return openEnds == 2 ? 5_000L : (openEnds == 1 ? 300L : 10L);
        if (count == 2) return openEnds == 2 ? 150L : (openEnds == 1 ? 25L : 2L);
        return 1L;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE;
    }
}
