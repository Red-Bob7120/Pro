package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

public class ProgressCircle extends JPanel {

    private double progress = 0.0;

    public void setProgress(double p) {
        this.progress = Math.max(0, Math.min(1, p));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 40;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        // 바깥 원
        g2.setColor(new Color(234, 238, 245));
        g2.fill(new Ellipse2D.Double(x, y, size, size));

        // 진행 아크
        g2.setColor(new Color(0, 122, 255));
        g2.fill(new Arc2D.Double(x, y, size, size,
                90, -progress * 360, Arc2D.PIE));

        // 안쪽 가림 원
        g2.setColor(getBackground());
        int inner = size - 40;
        g2.fill(new Ellipse2D.Double(x + 20, y + 20, inner, inner));

        // 퍼센트 텍스트
        g2.setColor(new Color(45, 45, 45));
        g2.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        String txt = (int) Math.round(progress * 100) + "%";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (getWidth() - fm.stringWidth(txt)) / 2;
        int ty = (getHeight() + fm.getAscent() / 2) / 2;
        g2.drawString(txt, tx, ty);

        g2.dispose();
    }
}
