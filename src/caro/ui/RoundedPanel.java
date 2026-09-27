package caro.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Panel nền bo góc kiểu "card" hiện đại, có thể chọn có bóng đổ nhẹ hay không.
 * Dùng làm khung chứa form đăng nhập/đăng ký, hoặc các card trong Lobby sau này.
 */
public class RoundedPanel extends JPanel {

    private Color bg = Theme.WHITE;
    private int arc = 20;
    private boolean shadow = true;

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public void setBg(Color c) {
        this.bg = c;
        repaint();
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (shadow) {
            // Bóng đổ rất nhẹ, vẽ lệch xuống 3px, mờ dần - tạo cảm giác "nổi" nhẹ nhàng
            g2.setColor(new Color(0, 0, 0, 25));
            g2.fillRoundRect(2, 4, w - 4, h - 4, arc, arc);
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w - (shadow ? 4 : 0), h - (shadow ? 6 : 0), arc, arc);
        g2.dispose();

        super.paintComponent(g);
    }
}
