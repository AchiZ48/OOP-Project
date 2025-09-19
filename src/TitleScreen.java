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

    void update(double dt) {
        // Could add title screen animations here
    }

    void draw(Graphics2D g) {
        // Background gradient
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 40),
                0, gp.vh, new Color(40, 20, 60));
        g.setPaint(gradient);
        g.fillRect(0, 0, gp.vw, gp.vh);

        // Background image with aspect-fit rendering
        if (backgroundImage != null) {
            drawImageAspectFit(g, backgroundImage);
        }

        // Title text
        g.setColor(Color.WHITE);
        g.setFont(FontCustom.PressStart2P.deriveFont(32f));
        drawCenteredString(g, "Solstice Warriors", gp.vw / 2, gp.vh - 48);

        // Subtitle
        g.setFont(FontCustom.PressStart2P.deriveFont(16f));
        drawCenteredString(g, "Press ENTER to Start", gp.vw / 2, gp.vh - 80);

        // Footer/edition label
        g.setColor(Color.LIGHT_GRAY);
        drawCenteredString(g, "Demo Edition", gp.vw / 2, gp.vh - 16);
    }

    private void drawImageAspectFit(Graphics2D g, BufferedImage img) {
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        if (imgW <= 0 || imgH <= 0) return;

        double scale = Math.min((double) gp.vw / imgW, (double) gp.vh / imgH);
        int drawW = (int) Math.round(imgW * scale);
        int drawH = (int) Math.round(imgH * scale);
        int x = (gp.vw - drawW) / 2;
        int y = (gp.vh - drawH) / 2;

        g.drawImage(img, x, y, drawW, drawH, null);
    }

    void drawCenteredString(Graphics2D g, String s, int cx, int cy) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        int h = fm.getAscent();
        g.drawString(s, cx - w / 2, cy + h / 2);
    }
}
