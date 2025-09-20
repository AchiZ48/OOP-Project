import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class TileMap {
    int tileW, tileH, cols, rows, pixelWidth, pixelHeight;
    int[][] groundLayer;
    int[][] ground2Layer;
    int[][] decorationLayer;
    int[][] collisionLayer;
    int[][] zoneLayer;
    boolean decorationVisible;
    List<TilesetEntry> tilesets = new ArrayList<>();

    static class TilesetEntry {
        int firstGid;
        Tileset tileset;
        public TilesetEntry(int firstGid, Tileset ts) {
            this.firstGid = firstGid;
            this.tileset = ts;
        }
    }

    public TileMap() {}

    // ---------------- Load TMX ----------------
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
        TileMap tm = new TileMap();
        tm.tileW = tileW; tm.tileH = tileH;
        tm.cols = width; tm.rows = height;
        tm.pixelWidth = width * tileW; tm.pixelHeight = height * tileH;
        tm.groundLayer = new int[height][width];
        tm.ground2Layer = new int[height][width];
        tm.decorationLayer = new int[height][width];
        tm.collisionLayer = new int[height][width];
        tm.zoneLayer = new int[height][width];
        tm.decorationVisible = true;

        // ---------------- Load Tilesets ----------------
        NodeList tilesetNodes = doc.getElementsByTagName("tileset");
        for (int i = 0; i < tilesetNodes.getLength(); i++) {
            Element tsEl = (Element) tilesetNodes.item(i);
            int firstGid = Integer.parseInt(tsEl.getAttribute("firstgid"));
            String source = tsEl.getAttribute("source");
            Tileset ts;
            if (!source.isEmpty()) {
                File tsxFile = new File(f.getParentFile(), source);
                ts = Tileset.loadFromTSX(tsxFile.getPath(), firstGid);
            } else {
                int tw = Integer.parseInt(tsEl.getAttribute("tilewidth"));
                int th = Integer.parseInt(tsEl.getAttribute("tileheight"));
                ts = Tileset.generatePlaceholder(tw, th);
            }
            tm.tilesets.add(new TilesetEntry(firstGid, ts));
        }

        // ---------------- Load layers ----------------
        NodeList layerNodes = doc.getElementsByTagName("layer");
        for (int i = 0; i < layerNodes.getLength(); i++) {
            Element lEl = (Element) layerNodes.item(i);
            String name = lEl.getAttribute("name");
            String visibleAttr = lEl.getAttribute("visible");
            boolean isVisible = !visibleAttr.equals("0");
            Element dataEl = (Element) lEl.getElementsByTagName("data").item(0);
            String[] tokens = dataEl.getTextContent().trim().split("\s*,\s*");
            int[][] layerData = new int[height][width];
            int idx = 0;
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    if (idx < tokens.length) {
                        String token = tokens[idx++].trim();
                        int gid = token.isEmpty() ? 0 : Integer.parseInt(token);
                        if ("collision".equalsIgnoreCase(name)) {
                            layerData[r][c] = gid > 0 ? 1 : 0;
                        } else {
                            layerData[r][c] = gid;
                        }
                    } else layerData[r][c] = 0;
                }
            }
            switch (name.toLowerCase()) {
                case "collision":
                    tm.collisionLayer = layerData;
                    break;
                case "decoration":
                    tm.decorationLayer = layerData;
                    tm.decorationVisible = isVisible;
                    break;
                case "ground":
                    tm.groundLayer = layerData;
                    break;
                case "ground2":
                    tm.ground2Layer = layerData;
                    break;
                case "zone":
                    tm.zoneLayer = layerData;
                    break;
                default:
                    tm.groundLayer = layerData;
                    break;
            }
        }
        return tm;
    }

    // ---------------- Draw ----------------
    public void draw(Graphics2D g, Camera cam) {
        drawGround(g, cam);
        drawDecorations(g, cam);
    }

    public void drawGround(Graphics2D g, Camera cam) {
        drawLayerTiles(g, groundLayer, cam);
        drawLayerTiles(g, ground2Layer, cam);
    }

    public void drawDecorations(Graphics2D g, Camera cam) {
        if (!decorationVisible) {
            return;
        }
        drawLayerTiles(g, decorationLayer, cam);
    }

    private void drawLayerTiles(Graphics2D g, int[][] layerData, Camera cam) {
        if (layerData == null || g == null) {
            return;
        }
        forVisibleTiles(layerData, cam, (tileX, tileY, gid) -> {
            if (gid <= 0) {
                return;
            }
            int wx = tileX * tileW;
            int wy = tileY * tileH;
            drawGID(g, gid, wx, wy, tileW, tileH);
        });
    }

    private void forVisibleTiles(int[][] layerData, Camera cam, TileVisitor visitor) {
        if (layerData == null || visitor == null) {
            return;
        }
        if (cols <= 0 || rows <= 0 || tileW <= 0 || tileH <= 0) {
            return;
        }

        int startCol = 0;
        int endCol = cols - 1;
        int startRow = 0;
        int endRow = rows - 1;

        if (cam != null) {
            double viewWidth = cam.getViewWidth();
            double viewHeight = cam.getViewHeight();
            double centerX = cam.getX();
            double centerY = cam.getY();

            double left = centerX - viewWidth / 2.0 - tileW;
            double top = centerY - viewHeight / 2.0 - tileH;
            double right = centerX + viewWidth / 2.0 + tileW;
            double bottom = centerY + viewHeight / 2.0 + tileH;

            double viewLeft = Math.max(0, left);
            double viewTop = Math.max(0, top);
            double viewRight = Math.min(pixelWidth, right);
            double viewBottom = Math.min(pixelHeight, bottom);

            if (viewRight <= viewLeft || viewBottom <= viewTop) {
                return;
            }

            startCol = Math.max(0, (int) (viewLeft / tileW));
            endCol = Math.min(cols - 1, (int) ((viewRight - 1) / tileW));
            startRow = Math.max(0, (int) (viewTop / tileH));
            endRow = Math.min(rows - 1, (int) ((viewBottom - 1) / tileH));

            if (startCol > endCol || startRow > endRow) {
                return;
            }
        }

        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            int[] row = layerData[rowIndex];
            for (int colIndex = startCol; colIndex <= endCol; colIndex++) {
                visitor.accept(colIndex, rowIndex, row[colIndex]);
            }
        }
    }

    private void drawGID(Graphics2D g, int gid, int x, int y, int w, int h) {
        if (gid <= 0) return;
        TilesetEntry entry = null;
        for (int i = tilesets.size() - 1; i >= 0; i--) {
            if (gid >= tilesets.get(i).firstGid) {
                entry = tilesets.get(i);
                break;
            }
        }
        if (entry == null) return;
        int localId = gid - entry.firstGid;
        entry.tileset.drawTile(g, localId, x, y, w, h);
    }

    // ---------------- Collision ----------------
    public boolean isSolid(int tileX, int tileY) {
        if (tileX < 0 || tileX >= cols || tileY < 0 || tileY >= rows) return true;
        return collisionLayer[tileY][tileX] > 0;
    }

    public boolean isSolidAtPixel(double wx, double wy) {
        int tileX = (int) (wx / tileW);
        int tileY = (int) (wy / tileH);
        return isSolid(tileX, tileY);
    }

    public void drawCollisionOverlay(Graphics2D g, Camera cam) {
        if (collisionLayer == null) return;
        g.setColor(new Color(255, 0, 0, 128));
        forVisibleTiles(collisionLayer, cam, (tileX, tileY, gid) -> {
            if (gid <= 0) {
                return;
            }
            int wx = tileX * tileW;
            int wy = tileY * tileH;
            g.fillRect(wx, wy, tileW, tileH);
        });
    }

    public void drawZoneOverlay(Graphics2D g, Camera cam) {
        if (zoneLayer == null) return;
        forVisibleTiles(zoneLayer, cam, (tileX, tileY, gid) -> {
            if (gid <= 0) {
                return;
            }
            switch (gid) {
                case 1025:
                    g.setColor(new Color(0, 255, 16, 128));
                    break;
                case 1026:
                    g.setColor(new Color(0, 104, 10, 128));
                    break;
                case 1027:
                    g.setColor(new Color(255, 144, 0, 128));
                    break;
                case 1028:
                    g.setColor(new Color(0, 217, 255, 128));
                    break;
                default:
                    g.setColor(new Color(255, 255, 255, 128));
                    break;
            }
            int wx = tileX * tileW;
            int wy = tileY * tileH;
            g.fillRect(wx, wy, tileW, tileH);
        });
    }

    private interface TileVisitor {
        void accept(int tileX, int tileY, int gid);
    }
}
