import java.awt.image.BufferedImage;
import java.io.IOException;

final class SpriteLoader {
    private SpriteLoader() {
    }

    static Sprite loadSheet(String path, int frameW, int frameH) throws IOException {
        BufferedImage img = readImage(path);
        int framesPerRow = img.getWidth() / frameW;
        int rows = img.getHeight() / frameH;
        return Sprite.fromSheet(img, frameW, frameH, framesPerRow, rows, framesPerRow);
    }

    private static BufferedImage readImage(String path) throws IOException {
        return ResourceLoader.loadImage(path);
    }
}
