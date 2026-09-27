package caro.common;

import java.io.Serializable;
import java.util.Date;

/**
 * Thông tin 1 tài khoản người chơi.
 * Implements Serializable vì object này sẽ được gửi qua lại giữa
 * Server và Client thông qua Message.user (Object Serialization),
 * giống cách project hiện tại đang gửi Message.
 *
 * KHÔNG chứa field "password" - object này chỉ mang dữ liệu hiển thị
 * (public profile), không bao giờ gửi mật khẩu/hash qua mạng.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String displayName;
    private int wins;
    private int losses;
    private int draws;
    private int totalGames;
    private int rating;
    private Date createdAt;
    private Date lastLogin;
    private String status;

    public User() {
    }

    public User(int id, String username, String displayName, int wins, int losses,
                int draws, int totalGames, int rating) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.totalGames = totalGames;
        this.rating = rating;
    }

    public double getWinRate() {
        if (totalGames <= 0) return 0.0;
        return (wins * 100.0) / totalGames;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }

    public int getTotalGames() { return totalGames; }
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', displayName='" + displayName
                + "', rating=" + rating + "}";
    }
}
