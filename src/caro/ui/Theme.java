package caro.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Bảng màu + font dùng CHUNG cho toàn bộ giao diện (LoginFrame, RegisterFrame,
 * LobbyFrame, ... ở các phase sau).
 *
 * QUAN TRỌNG: các giá trị Color dưới đây được sao chép NGUYÊN VẸN từ các
 * hằng số COLOR_* đang khai báo private trong CaroClient.java, để đảm bảo
 * toàn hệ thống dùng chung 1 tông màu gỗ/nâu/đỏ đất - không tạo bảng màu mới.
 *
 * CaroClient.java KHÔNG bị sửa ở Phase 2 (vẫn giữ các hằng số riêng của nó)
 * để tránh rủi ro phá vỡ giao diện bàn cờ đang chạy tốt. Việc gộp CaroClient
 * về dùng chung Theme này sẽ làm ở Phase 11 (UI polish) khi đã test kỹ.
 */
public class Theme {

    // ---- Nền / header (giống COLOR_HEADER_TOP/BOTTOM/ACCENT trong CaroClient) ----
    public static final Color WOOD_DARK   = new Color(62, 39, 35);    // brown-900 - nền header trên
    public static final Color WOOD_MEDIUM = new Color(93, 64, 55);    // brown-700 - nền header dưới
    public static final Color WOOD_ACCENT = new Color(212, 165, 116); // vàng gỗ nhạt - viền nhấn

    // ---- Trạng thái / pill (giống COLOR_PILL_* trong CaroClient) ----
    public static final Color MOSS_GREEN   = new Color(56, 118, 29);   // xanh lá rêu - thành công/lượt của mình
    public static final Color TAUPE_WAIT   = new Color(109, 91, 74);   // nâu xám - đang chờ
    public static final Color TERRACOTTA   = new Color(150, 40, 27);   // đỏ đất nung - lỗi/thua
    public static final Color AMBER_EARTH  = new Color(168, 111, 26);  // vàng cam đất - cảnh báo/hòa

    // ---- Nền ô / bề mặt (giống COLOR_CELL_* trong CaroClient) ----
    public static final Color CREAM       = new Color(245, 222, 179); // wheat - nền sáng
    public static final Color BEIGE       = new Color(222, 191, 145); // burlywood - nền tối hơn 1 chút
    public static final Color GRID_LINE   = new Color(141, 110, 99);  // nâu đậm - đường viền

    // ---- Chữ / input ----
    public static final Color TEXT_DARK   = new Color(62, 39, 35);
    public static final Color TEXT_MUTED  = new Color(120, 100, 90);
    public static final Color WHITE       = Color.WHITE;
    public static final Color INPUT_BG    = new Color(250, 244, 234);

    // ---- Font ----
    public static final String FONT_FAMILY = "Segoe UI";

    public static Font titleFont(int size)   { return new Font(FONT_FAMILY, Font.BOLD, size); }
    public static Font labelFont(int size)   { return new Font(FONT_FAMILY, Font.PLAIN, size); }
    public static Font boldFont(int size)    { return new Font(FONT_FAMILY, Font.BOLD, size); }

    private Theme() {
    }
}
