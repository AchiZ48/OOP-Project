import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

class SpriteAnim implements Serializable {
    private static final long serialVersionUID = 1L;

    transient BufferedImage sheet;
    int frameW, frameH, framesPerRow, rows;
    double fps;
    double timeAccumulator = 0.0;
    int currentFrame = 0;
    boolean playing = true;
    int scale = 3;

    public SpriteAnim(BufferedImage sheet, int fw, int fh, int framesPerRow, int rows, double fps) {
        this.sheet = sheet;
        this.frameW = fw;
        this.frameH = fh;
        this.framesPerRow = framesPerRow;
        this.rows = rows;
        this.fps = fps;
    }

    public void draw(Graphics2D g, int screenX, int screenY, int drawW, int drawH) {
        if (sheet == null) {
            // Fallback rendering
            g.setColor(Color.RED);
            g.fillRect(screenX, screenY, drawW, drawH);
            return;
        }

        int srcX = (currentFrame % framesPerRow) * frameW;
        int srcY = (currentFrame / framesPerRow) * frameH;

        g.drawImage(sheet, screenX, screenY, screenX + drawW, screenY + drawH,
                srcX, srcY, srcX + frameW, srcY + frameH, null);
    }

    public void update(double dt) {
        if (!playing || fps <= 0) return;

        timeAccumulator += dt;
        double frameTime = 1.0 / fps;

        while (timeAccumulator >= frameTime) {
            currentFrame = (currentFrame + 1) % (framesPerRow * rows);
            timeAccumulator -= frameTime;
        }
    }
}
