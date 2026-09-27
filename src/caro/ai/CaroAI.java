package caro.ai;

/**
 * Interface chung cho AI chơi Caro - mỗi độ khó (Easy/Medium/Hard) implement
 * 1 chiến lược chọn nước đi khác nhau, nhưng dùng chung 1 "hợp đồng":
 * cho bàn cờ hiện tại, trả về toạ độ {x, y} AI muốn đánh.
 */
public interface CaroAI {

    int SIZE = 15;

    /**
     * @param board       bàn cờ hiện tại, board[x][y]: 0 = trống, 1/2 = quân người chơi
     * @param aiPlayerId  giá trị đại diện AI trên bàn cờ (1 hoặc 2)
     * @param humanPlayerId giá trị đại diện người chơi (1 hoặc 2, khác aiPlayerId)
     * @return toạ độ {x, y} AI chọn đánh - LUÔN là 1 ô đang trống
     */
    int[] nextMove(int[][] board, int aiPlayerId, int humanPlayerId);
}
