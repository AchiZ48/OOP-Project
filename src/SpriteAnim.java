import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serializable;

class SpriteAnim implements Serializable {
    private static final long serialVersionUID = 1L;

    transient BufferedImage sheet;
    int frameW;
    int frameH;
    int framesPerRow;
    int rows;
    double fps;
    double timeAccumulator = 0.0;
    int currentFrame = 0; // column index inside the active row
    int currentRow = 0;
    boolean playing = true;
    int scale = 3;

    public SpriteAnim(BufferedImage sheet, int fw, int fh, int framesPerRow, int rows, double fps) {
        this.sheet = sheet;
        this.frameW = fw;
        this.frameH = fh;
        this.framesPerRow = Math.max(1, framesPerRow);
        this.rows = Math.max(1, rows);
        this.fps = fps;
    }

    public void draw(Graphics2D g, double screenX, double screenY, int drawW, int drawH) {
        if (sheet == null) {
            g.setColor(Color.RED);
            g.fillRect((int) Math.floor(screenX), (int) Math.floor(screenY), drawW, drawH);
            return;
        }

        int srcX = currentFrame * frameW;
        int srcY = currentRow * frameH;

        AffineTransform old = g.getTransform();
        g.translate(screenX, screenY);
        double scaleX = drawW / (double) frameW;
        double scaleY = drawH / (double) frameH;
        g.scale(scaleX, scaleY);
        g.drawImage(sheet, 0, 0, frameW, frameH, srcX, srcY, srcX + frameW, srcY + frameH, null);
        g.setTransform(old);
    }

    public void update(double dt) {
        if (!playing || fps <= 0) return;

        timeAccumulator += dt;
        double frameTime = 1.0 / fps;

        while (timeAccumulator >= frameTime) {
            currentFrame = (currentFrame + 1) % framesPerRow;
            timeAccumulator -= frameTime;
        }
    }

    public void setRow(int row) {
        int clamped = Math.max(0, Math.min(row, rows - 1));
        if (clamped != currentRow) {
            currentRow = clamped;
            resetFrame();
        }
    }

    public void resetFrame() {
        currentFrame = 0;
        timeAccumulator = 0.0;
    }

    public void setFrame(int frameIndex) {
        int clamped = Math.max(0, Math.min(frameIndex, framesPerRow - 1));
        if (clamped != currentFrame) {
            currentFrame = clamped;
            timeAccumulator = 0.0;
        }
    }

    public int getCurrentRow() {
        return currentRow;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }
}
