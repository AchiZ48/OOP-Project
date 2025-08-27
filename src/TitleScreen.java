import java.awt.*;

class TitleScreen {
    GamePanel gp;

    public TitleScreen(GamePanel gp) {
        this.gp = gp;
    }

    void update(double dt) {
        // Could add title screen animations here
    }

    void draw(Graphics2D g) {
        // Background gradient
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 40),
                0, gp.vh, new Color(40, 20, 60));
        g.setPaint(gradient);
        g.fillRect(0, 0, gp.vw, gp.vh);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(FontCustom.PressStart2P.deriveFont(28f));
        drawCenteredString(g, "Turn-Based RPG", gp.vw / 2, gp.vh / 3);

        // Subtitle
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        drawCenteredString(g, "Press ENTER to Start", gp.vw / 2, gp.vh / 2);

        // Instructions
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(Color.LIGHT_GRAY);
        drawCenteredString(g, "Fixed & Enhanced Edition", gp.vw / 2, gp.vh * 3 / 4);
    }

    void drawCenteredString(Graphics2D g, String s, int cx, int cy) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        int h = fm.getAscent();
        g.drawString(s, cx - w / 2, cy + h / 2);
    }
}
