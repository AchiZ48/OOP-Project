import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

class Sprite implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    transient BufferedImage sheet;
    private final int frameW;
    private final int frameH;
    private final int framesPerRow;
    private final int rows;
    private final double fps;
    private final boolean supportsAnimation;

    private double timeAccumulator = 0.0;
    private int currentFrame = 0;
    private int currentRow = 0;
    private boolean playing;

    private Sprite(BufferedImage sheet,
                   int frameW,
                   int frameH,
                   int framesPerRow,
                   int rows,
                   double fps,
                   boolean supportsAnimation) {
        this.sheet = sheet;
        this.frameW = Math.max(1, frameW);
        this.frameH = Math.max(1, frameH);
        this.framesPerRow = Math.max(1, framesPerRow);
        this.rows = Math.max(1, rows);
        this.fps = fps;
        this.supportsAnimation = supportsAnimation && (this.framesPerRow > 1 || this.rows > 1);
        this.playing = this.supportsAnimation && this.fps > 0;
    }

    static Sprite forStaticImage(BufferedImage image) {
        if (image == null) {
            return new Sprite(null, 1, 1, 1, 1, 0.0, false);
        }
        return new Sprite(image, image.getWidth(), image.getHeight(), 1, 1, 0.0, false);
    }

    static Sprite fromSheet(BufferedImage sheet,
                            int frameW,
                            int frameH,
                            int framesPerRow,
                            int rows,
                            double fps) {
        return new Sprite(sheet, frameW, frameH, framesPerRow, rows, fps, true);
    }

    Sprite copy() {
        Sprite clone = new Sprite(sheet, frameW, frameH, framesPerRow, rows, fps, supportsAnimation);
        clone.currentFrame = this.currentFrame;
        clone.currentRow = this.currentRow;
        clone.playing = this.playing;
        clone.timeAccumulator = 0.0;
        return clone;
    }

    void update(double dt) {
        if (!supportsAnimation || !playing || fps <= 0) {
            return;
        }

        timeAccumulator += dt;
        double frameTime = 1.0 / fps;

        while (timeAccumulator >= frameTime) {
            currentFrame = (currentFrame + 1) % framesPerRow;
            timeAccumulator -= frameTime;
        }
    }

    void draw(Graphics2D g, double screenX, double screenY, int drawW, int drawH) {
        if (sheet == null) {
            g.setColor(Color.RED);
            g.fillRect((int) Math.floor(screenX), (int) Math.floor(screenY), drawW, drawH);
            return;
        }

        int srcX = supportsAnimation ? currentFrame * frameW : 0;
        int srcY = supportsAnimation ? currentRow * frameH : 0;

        AffineTransform old = g.getTransform();
        g.translate(screenX, screenY);
        double scaleX = drawW / (double) frameW;
        double scaleY = drawH / (double) frameH;
        g.scale(scaleX, scaleY);
        g.drawImage(sheet, 0, 0, frameW, frameH, srcX, srcY, srcX + frameW, srcY + frameH, null);
        g.setTransform(old);
    }

    void setRow(int row) {
        if (!supportsAnimation) {
            return;
        }
        int clamped = Math.max(0, Math.min(row, rows - 1));
        if (clamped != currentRow) {
            currentRow = clamped;
            resetFrame();
        }
    }

    void setFrame(int frameIndex) {
        if (!supportsAnimation) {
            return;
        }
        int clamped = Math.max(0, Math.min(frameIndex, framesPerRow - 1));
        if (clamped != currentFrame) {
            currentFrame = clamped;
            timeAccumulator = 0.0;
        }
    }

    void resetFrame() {
        currentFrame = 0;
        timeAccumulator = 0.0;
    }

    void setPlaying(boolean playing) {
        if (!supportsAnimation) {
            return;
        }
        this.playing = playing;
        if (!playing) {
            resetFrame();
        }
    }

    boolean isPlaying() {
        return supportsAnimation && playing;
    }

    boolean isAnimated() {
        return supportsAnimation;
    }

    int getFrameWidth() {
        return frameW;
    }

    int getFrameHeight() {
        return frameH;
    }

    int getCurrentFrame() {
        return currentFrame;
    }

    int getCurrentRow() {
        return currentRow;
    }

    void setSheet(BufferedImage sheet) {
        this.sheet = sheet;
    }
}
