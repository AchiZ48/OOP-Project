import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;

public class Tileset {
    BufferedImage image;
    int tileW, tileH, cols, rows;

    public static Tileset loadFromTSX(String path, int firstGid) throws Exception {
        File f = new File(path);
        if (!f.exists()) throw new FileNotFoundException("TSX not found: " + path);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(f);

        Element ts = (Element) doc.getElementsByTagName("tileset").item(0);
        int tileW = Integer.parseInt(ts.getAttribute("tilewidth"));
        int tileH = Integer.parseInt(ts.getAttribute("tileheight"));

        Element img = (Element) ts.getElementsByTagName("image").item(0);
        String src = img.getAttribute("source");
        int imgW = Integer.parseInt(img.getAttribute("width"));
        int imgH = Integer.parseInt(img.getAttribute("height"));

        // Try to load image from multiple locations
        BufferedImage bi = null;
        File imgFile = new File(f.getParentFile(), src);
        if (imgFile.exists()) {
            bi = ImageIO.read(imgFile);
        } else {
            File res = new File("resources/tiles/" + src);
            if (res.exists()) {
                bi = ImageIO.read(res);
            } else {
                bi = generatePlaceholderImage(tileW, tileH, imgW, imgH);
            }
        }

        Tileset tileset = new Tileset();
        tileset.image = bi;
        tileset.tileW = tileW;
        tileset.tileH = tileH;
        tileset.cols = bi.getWidth() / tileW;
        tileset.rows = bi.getHeight() / tileH;
        return tileset;
    }

    static BufferedImage generatePlaceholderImage(int tw, int th, int w, int h) {
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        for (int y = 0; y < h; y += th) {
            for (int x = 0; x < w; x += tw) {
                Color baseColor = ((x/tw + y/th) & 1) == 0 ? Color.LIGHT_GRAY : Color.GRAY;
                g.setColor(baseColor);
                g.fillRect(x, y, tw, th);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x, y, tw-1, th-1);

                // Add tile number for debugging
                g.setColor(Color.BLACK);
                g.setFont(new Font("Monospaced", Font.PLAIN, 8));
                int tileNum = (y/th) * (w/tw) + (x/tw);
                g.drawString(String.valueOf(tileNum), x+2, y+10);
            }
        }
        g.dispose();
        return bi;
    }

    public static Tileset generatePlaceholder(int tw, int th) {
        BufferedImage bi = new BufferedImage(tw*2, th*2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();

        // Tile 0 (index 0) - light gray
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, tw, th);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, tw-1, th-1);

        // Tile 1 (index 1) - gray
        g.setColor(Color.GRAY);
        g.fillRect(tw, 0, tw, th);
        g.setColor(Color.BLACK);
        g.drawRect(tw, 0, tw-1, th-1);

        // Tile 2 (index 2) - dark gray
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, th, tw, th);
        g.setColor(Color.BLACK);
        g.drawRect(0, th, tw-1, th-1);

        // Tile 3 (index 3) - orange
        g.setColor(Color.ORANGE);
        g.fillRect(tw, th, tw, th);
        g.setColor(Color.BLACK);
        g.drawRect(tw, th, tw-1, th-1);

        g.dispose();

        Tileset ts = new Tileset();
        ts.image = bi;
        ts.tileW = tw;
        ts.tileH = th;
        ts.cols = 2;
        ts.rows = 2;
        return ts;
    }

    public void drawTile(Graphics2D g, int index, int sx, int sy, int w, int h) {
        if (image == null || index < 0) return;

        int col = index % cols;
        int row = index / cols;
        if (row >= rows) return; // Invalid tile index

        int srcX = col * tileW;
        int srcY = row * tileH;

        g.drawImage(image, sx, sy, sx + w, sy + h,
                srcX, srcY, srcX + tileW, srcY + tileH, null);
    }
}