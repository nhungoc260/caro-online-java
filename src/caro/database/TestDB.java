package caro.database;

import caro.common.User;

import java.util.List;

/**
 * Class TEST TẠM THỜI - chỉ dùng để kiểm tra tầng Database (Phase 1)
 * hoạt động đúng trước khi bắt đầu viết giao diện Login/Register (Phase 2).
 *
 * CÁCH CHẠY:
 *   Trong NetBeans, chuột phải file này -> Run File (Shift+F6).
 *   Xem kết quả in ra ở khung Output phía dưới.
 *
 * Sau khi test xong và mọi thứ OK, bạn có thể XÓA file này đi -
 * nó không được gọi bởi bất kỳ phần nào khác của chương trình.
 */
public class TestDB {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println(" TEST PHASE 1 - DATABASE + JDBC");
        System.out.println("========================================\n");

        // ---- TEST 1: Kết nối database ----
        System.out.println("[TEST 1] Kiểm tra kết nối MySQL...");
        if (DatabaseConnection.testConnection()) {
            System.out.println("  ✓ OK - Kết nối MySQL thành công!\n");
        } else {
            System.out.println("  ✗ THẤT BẠI - Không kết nối được MySQL.");
            System.out.println("  Kiểm tra lại: WampServer đã Start MySQL chưa? "
                    + "Database caro_online đã import chưa? "
                    + "File DatabaseConfig.java đúng cấu hình chưa?");
            return; // các test sau chắc chắn fail nên dừng luôn
        }

        UserDAO userDAO = new UserDAO();

        // ---- TEST 2: Đăng nhập với tài khoản demo có sẵn trong SQL ----
        System.out.println("[TEST 2] Đăng nhập tài khoản demo \"nguyena\" / \"123456\"...");
        try {
            User loginUser = userDAO.authenticate("nguyena", "123456");
            if (loginUser != null) {
                System.out.println("  ✓ OK - Đăng nhập thành công: " + loginUser);
                System.out.println("    Rating: " + loginUser.getRating()
                        + " | Thắng: " + loginUser.getWins()
                        + " | Thua: " + loginUser.getLosses()
                        + " | Hòa: " + loginUser.getDraws()
                        + " | Tỷ lệ thắng: " + String.format("%.1f", loginUser.getWinRate()) + "%\n");
            } else {
                System.out.println("  ✗ THẤT BẠI - Sai username/password "
                        + "(kiểm tra lại đã import đúng file SQL demo data chưa)\n");
            }
        } catch (DatabaseConnection.DatabaseException e) {
            System.out.println("  ✗ LỖI: " + e.getMessage() + "\n");
        }

        // ---- TEST 3: Đăng nhập sai mật khẩu (phải thất bại) ----
        System.out.println("[TEST 3] Đăng nhập với mật khẩu SAI (kỳ vọng: thất bại)...");
        try {
            User wrongLogin = userDAO.authenticate("nguyena", "mat-khau-sai");
            if (wrongLogin == null) {
                System.out.println("  ✓ OK - Hệ thống từ chối đúng như kỳ vọng.\n");
            } else {
                System.out.println("  ✗ NGUY HIỂM - Đăng nhập được với mật khẩu sai! "
                        + "Kiểm tra lại logic hash trong UserDAO.\n");
            }
        } catch (DatabaseConnection.DatabaseException e) {
            System.out.println("  ✗ LỖI: " + e.getMessage() + "\n");
        }

        // ---- TEST 4: Đăng ký tài khoản mới ----
        String newUsername = "test_" + System.currentTimeMillis(); // tránh trùng khi chạy nhiều lần
        System.out.println("[TEST 4] Đăng ký tài khoản mới \"" + newUsername + "\"...");
        try {
            boolean taken = userDAO.isUsernameTaken(newUsername);
            System.out.println("  - Username đã tồn tại? " + taken + " (kỳ vọng: false)");

            User newUser = userDAO.register(newUsername, "matkhau123", "Người Test");
            System.out.println("  ✓ OK - Đăng ký thành công: " + newUser + "\n");

            // Đăng ký trùng username -> phải bị từ chối
            System.out.println("[TEST 4b] Đăng ký TRÙNG username \"" + newUsername
                    + "\" (kỳ vọng: bị từ chối)...");
            try {
                userDAO.register(newUsername, "matkhau-khac", "Người Test 2");
                System.out.println("  ✗ NGUY HIỂM - Đăng ký trùng username vẫn thành công!\n");
            } catch (DatabaseConnection.DatabaseException dupEx) {
                System.out.println("  ✓ OK - Bị từ chối đúng như kỳ vọng: " + dupEx.getMessage() + "\n");
            }

            // ---- TEST 5: Cập nhật thống kê sau 1 trận thắng ----
            System.out.println("[TEST 5] Cập nhật thống kê sau 1 trận THẮNG cho \"" + newUsername + "\"...");
            userDAO.updateStatsAfterMatch(newUser.getId(), "WIN", 1000);
            User afterWin = userDAO.getById(newUser.getId());
            System.out.println("  ✓ OK - Sau khi thắng: wins=" + afterWin.getWins()
                    + ", total_games=" + afterWin.getTotalGames()
                    + ", rating=" + afterWin.getRating() + " (trước đó 1000)\n");

            // ---- TEST 6: Lưu 1 trận đấu + các nước đi ----
            System.out.println("[TEST 6] Tạo match + lưu nước đi (MatchDAO/MoveDAO)...");
            MatchDAO matchDAO = new MatchDAO();
            MoveDAO moveDAO = new MoveDAO();
            int matchId = matchDAO.createMatch(newUser.getId(), null, "AI", null);
            moveDAO.saveMove(matchId, newUser.getId(), 7, 7, 1);
            moveDAO.saveMove(matchId, newUser.getId(), 7, 8, 2);
            matchDAO.finishMatch(matchId, newUser.getId(), "WIN", 42);
            List<MoveDAO.MoveRecord> moves = moveDAO.getMovesByMatch(matchId);
            System.out.println("  ✓ OK - Tạo match #" + matchId + ", lưu và đọc lại "
                    + moves.size() + " nước đi (kỳ vọng: 2)\n");

        } catch (DatabaseConnection.DatabaseException e) {
            System.out.println("  ✗ LỖI: " + e.getMessage() + "\n");
        }

        // ---- TEST 7: Bảng xếp hạng ----
        System.out.println("[TEST 7] Lấy bảng xếp hạng Top 5...");
        try {
            List<User> leaderboard = userDAO.getLeaderboard(5);
            int rank = 1;
            for (User u : leaderboard) {
                System.out.println("  #" + rank + "  " + u.getDisplayName()
                        + "  -  rating " + u.getRating()
                        + "  -  " + u.getWins() + "T/" + u.getLosses() + "B/" + u.getDraws() + "H");
                rank++;
            }
            System.out.println("  ✓ OK - Lấy được " + leaderboard.size() + " người chơi.\n");
        } catch (DatabaseConnection.DatabaseException e) {
            System.out.println("  ✗ LỖI: " + e.getMessage() + "\n");
        }

        System.out.println("========================================");
        System.out.println(" HOÀN TẤT TEST PHASE 1");
        System.out.println("========================================");
    }
}
