-- ------------------------------------------------------------
-- 0. TẠO DATABASE
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS caro_online
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE caro_online;

-- Xoá bảng cũ nếu chạy lại script (theo đúng thứ tự để không vỡ FOREIGN KEY)
DROP TABLE IF EXISTS match_moves;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS users;

-- ------------------------------------------------------------
-- 1. BẢNG users
-- ------------------------------------------------------------
CREATE TABLE users (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    username      VARCHAR(32)  NOT NULL,
    password      VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hex string (64 ky tu), khong luu plain text',
    display_name  VARCHAR(64)  NOT NULL,
    wins          INT UNSIGNED NOT NULL DEFAULT 0,
    losses        INT UNSIGNED NOT NULL DEFAULT 0,
    draws         INT UNSIGNED NOT NULL DEFAULT 0,
    total_games   INT UNSIGNED NOT NULL DEFAULT 0,
    rating        INT          NOT NULL DEFAULT 1000,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login    DATETIME     NULL,
    status        ENUM('ONLINE','IN_LOBBY','IN_ROOM','PLAYING','OFFLINE')
                               NOT NULL DEFAULT 'OFFLINE',
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    KEY idx_users_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 2. BẢNG rooms (phục vụ RoomManager ở Phase 5, tạo trước cho đủ schema)
-- ------------------------------------------------------------
CREATE TABLE rooms (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    room_code     VARCHAR(10)  NOT NULL,
    host_id       INT UNSIGNED NOT NULL,
    guest_id      INT UNSIGNED NULL,
    status        ENUM('WAITING','PLAYING','FINISHED') NOT NULL DEFAULT 'WAITING',
    mode          ENUM('ONLINE','AI') NOT NULL DEFAULT 'ONLINE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_rooms_code (room_code),
    KEY idx_rooms_host (host_id),
    KEY idx_rooms_guest (guest_id),
    CONSTRAINT fk_rooms_host  FOREIGN KEY (host_id)  REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rooms_guest FOREIGN KEY (guest_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 3. BẢNG matches
-- ------------------------------------------------------------
CREATE TABLE matches (
    id               INT UNSIGNED NOT NULL AUTO_INCREMENT,
    player1_id       INT UNSIGNED NOT NULL,
    player2_id       INT UNSIGNED NULL COMMENT 'NULL khi mode = AI',
    winner_id        INT UNSIGNED NULL COMMENT 'NULL khi hoa/abandoned khong xac dinh',
    result           ENUM('WIN','LOSS','DRAW','ABANDONED') NOT NULL,
    mode             ENUM('ONLINE','AI') NOT NULL DEFAULT 'ONLINE',
    room_code        VARCHAR(10)  NULL,
    ai_difficulty    ENUM('EASY','MEDIUM','HARD') NULL COMMENT 'Do kho AI, NULL khi mode = ONLINE',
    started_at       DATETIME     NOT NULL,
    ended_at         DATETIME     NULL,
    duration_seconds INT UNSIGNED NULL,
    PRIMARY KEY (id),
    KEY idx_matches_player1 (player1_id),
    KEY idx_matches_player2 (player2_id),
    KEY idx_matches_winner  (winner_id),
    KEY idx_matches_started (started_at),
    CONSTRAINT fk_matches_player1 FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_player2 FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_winner  FOREIGN KEY (winner_id)  REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 4. BẢNG match_moves
-- ------------------------------------------------------------
CREATE TABLE match_moves (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    match_id     INT UNSIGNED NOT NULL,
    player_id    INT UNSIGNED NOT NULL,
    x            TINYINT UNSIGNED NOT NULL,
    y            TINYINT UNSIGNED NOT NULL,
    move_number  SMALLINT UNSIGNED NOT NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_moves_match (match_id, move_number),
    CONSTRAINT fk_moves_match  FOREIGN KEY (match_id)  REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT fk_moves_player FOREIGN KEY (player_id) REFERENCES users(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 5. DỮ LIỆU DEMO
-- ------------------------------------------------------------
-- Mật khẩu demo cho cả 3 tài khoản là: 123456
-- SHA-256("123456") = 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
-- (Java UserDAO sẽ tự hash khi đăng ký; các dòng dưới chỉ để demo nhanh khi chưa kịp đăng ký)
INSERT INTO users (username, password, display_name, wins, losses, draws, total_games, rating, status) VALUES
('demo1',  '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Demo Player 1', 5, 2, 1, 8, 1120, 'OFFLINE'),
('nguyena','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Nguyễn A',       10, 4, 2, 16, 1250, 'OFFLINE'),
('minh',   '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Minh',           3, 6, 1, 10, 980,  'OFFLINE');

INSERT INTO matches (player1_id, player2_id, winner_id, result, mode, room_code, started_at, ended_at, duration_seconds) VALUES
(2, 3, 2, 'WIN', 'ONLINE', 'DEMO01', '2026-09-23 10:00:00', '2026-09-23 10:04:21', 261),
(3, NULL, NULL, 'LOSS', 'AI', NULL, '2026-09-23 09:00:00', '2026-09-23 09:05:12', 312);

INSERT INTO match_moves (match_id, player_id, x, y, move_number) VALUES
(1, 2, 7, 7, 1),
(1, 3, 7, 8, 2),
(1, 2, 8, 7, 3),
(1, 3, 6, 8, 4),
(1, 2, 9, 7, 5);

-- ============================================================
-- HẾT FILE
-- ============================================================
