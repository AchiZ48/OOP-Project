import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;

public class TileMap {
    int tileW, tileH, cols, rows, pixelWidth, pixelHeight;
    int[][] layer;
    Tileset tileset;

    public TileMap() {}

    public static TileMap loadFromTMX(String path) throws Exception {
        File f = new File(path);
        if (!f.exists()) throw new FileNotFoundException("TMX not found: " + path);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(f);

        Element mapEl = (Element) doc.getElementsByTagName("map").item(0);
        int tileW = Integer.parseInt(mapEl.getAttribute("tilewidth"));
        int tileH = Integer.parseInt(mapEl.getAttribute("tileheight"));
        int width = Integer.parseInt(mapEl.getAttribute("width"));
        int height = Integer.parseInt(mapEl.getAttribute("height"));

        // Load tileset
        Element tilesetEl = (Element) doc.getElementsByTagName("tileset").item(0);
        String tsxSource = tilesetEl.getAttribute("source");
        int firstGid = Integer.parseInt(tilesetEl.getAttribute("firstgid"));
        File tsxFile = new File(f.getParentFile(), tsxSource);
        Tileset ts = Tileset.loadFromTSX(tsxFile.getPath(), firstGid);

        // Load layer data
        Element layerEl = (Element) doc.getElementsByTagName("layer").item(0);
        Element dataEl = (Element) layerEl.getElementsByTagName("data").item(0);
        String csv = dataEl.getTextContent().trim();
        String[] tokens = csv.split("\\s*,\\s*");

        int[][] layer = new int[height][width];
        int idx = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (idx < tokens.length) {
                    int gid = Integer.parseInt(tokens[idx++]);
                    layer[r][c] = Math.max(0, gid - firstGid + 1);
                } else {
                    layer[r][c] = 0;
                }
            }
        }

        TileMap tm = new TileMap();
        tm.tileW = tileW; tm.tileH = tileH;
        tm.cols = width; tm.rows = height;
        tm.pixelWidth = width * tileW;
        tm.pixelHeight = height * tileH;
        tm.layer = layer;
        tm.tileset = ts;
        return tm;
    }

    public static TileMap generatePlaceholder(int w, int h, int tw, int th) {
        TileMap tm = new TileMap();
        tm.tileW = tw; tm.tileH = th;
        tm.cols = w; tm.rows = h;
        tm.pixelWidth = w * tw;
        tm.pixelHeight = h * th;
        tm.layer = new int[h][w];

        // Generate checkerboard pattern
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                tm.layer[r][c] = ((r + c) & 1) + 1;
            }
        }

        tm.tileset = Tileset.generatePlaceholder(tw, th);
        return tm;
    }

    public void draw(Graphics2D g, Camera cam) {
        if (tileset == null) return;

        // Calculate visible tile range for performance
        int startCol = Math.max(0, (int)((cam.x - cam.vw/2) / tileW) - 1);
        int endCol = Math.min(cols, (int)((cam.x + cam.vw/2) / tileW) + 2);
        int startRow = Math.max(0, (int)((cam.y - cam.vh/2) / tileH) - 1);
        int endRow = Math.min(rows, (int)((cam.y + cam.vh/2) / tileH) + 2);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                int id = layer[r][c];
                if (id <= 0) continue;

                int wx = c * tileW, wy = r * tileH;
                int sx = cam.worldToScreenX(wx), sy = cam.worldToScreenY(wy);
                tileset.drawTile(g, id - 1, sx, sy, tileW, tileH);
            }
        }
    }
}
