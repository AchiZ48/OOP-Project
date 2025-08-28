import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class TitleScreen {
    GamePanel gp;
    BufferedImage backgroundImage;
    String path = "resources/title/title2.png";


    public TitleScreen(GamePanel gp) {
        this.gp = gp;
        try {
            File file = new File(path);
            backgroundImage = ImageIO.read(file);
        } catch (IOException e) {
            System.out.println("Failed to load from resource: " + path);
        }
    }

//    void update(double dt) {
//        // Could add title screen animations here
//    }

    void draw(Graphics2D g) {
        // Background gradient
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 40),
                0, gp.vh, new Color(40, 20, 60));
        g.setPaint(gradient);
        g.fillRect(0, 0, gp.vw, gp.vh);
        //Background image
        g.drawImage(backgroundImage, 0, 0, gp.vw, gp.vh, null);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(FontCustom.PressStart2P.deriveFont(32f));
        drawCenteredString(g, "Solstice Warriors", gp.vw / 2, gp.vh - 48);

        // Subtitle
        g.setFont(FontCustom.PressStart2P.deriveFont(16f));
        drawCenteredString(g, "Press ENTER to Start", gp.vw / 2, gp.vh - 80);

        // Instructions
        g.setFont(FontCustom.PressStart2P.deriveFont(16f));
        g.setColor(Color.LIGHT_GRAY);
        drawCenteredString(g, "Demo Edition", gp.vw / 2, gp.vh - 16);
    }

    void drawCenteredString(Graphics2D g, String s, int cx, int cy) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        int h = fm.getAscent();
        g.drawString(s, cx - w / 2, cy + h / 2);
    }
}
