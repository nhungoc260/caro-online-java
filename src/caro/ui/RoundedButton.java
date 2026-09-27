package caro.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Nút bo góc, đổi màu nhẹ khi hover/press - phong cách giống hệt nút trong
 * CaroClient (nhưng đặt ở đây thành class PUBLIC để LoginFrame/RegisterFrame/
 * LobbyFrame... đều tái sử dụng được, không phải copy-paste code UI nhiều lần).
 */
public class RoundedButton extends JButton {

    public RoundedButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setForeground(Theme.WHITE);
        setFont(Theme.boldFont(14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color base = getBackground();
        Color fill;
        if (!isEnabled()) {
            fill = new Color(158, 158, 158);
        } else if (getModel().isPressed()) {
            fill = base.darker();
        } else if (getModel().isRollover()) {
            fill = base.brighter();
        } else {
            fill = base;
        }

        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.dispose();

        super.paintComponent(g);
    }
}
