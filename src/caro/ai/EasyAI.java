package caro.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** AI mức DỄ: chọn 1 ô trống hoàn toàn ngẫu nhiên. */
public class EasyAI implements CaroAI {

    private final Random random = new Random();

    @Override
    public int[] nextMove(int[][] board, int aiPlayerId, int humanPlayerId) {
        List<int[]> emptyCells = new ArrayList<int[]>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) emptyCells.add(new int[]{i, j});
            }
        }
        if (emptyCells.isEmpty()) return null;
        return emptyCells.get(random.nextInt(emptyCells.size()));
    }
}
