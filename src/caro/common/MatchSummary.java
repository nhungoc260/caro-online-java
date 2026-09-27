package caro.common;

import java.io.Serializable;
import java.util.Date;

/** Thông tin tóm tắt 1 trận đấu, dùng cho màn hình Lịch sử trận đấu. */
public class MatchSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    public int matchId;
    public String opponentName;
    public String mode;             // ONLINE / AI
    public String result;           // WIN / LOSS / DRAW / ABANDONED
    public Date startedAt;
    public Integer durationSeconds; // có thể null nếu trận chưa kết thúc đúng cách

    public MatchSummary() {
    }
}
