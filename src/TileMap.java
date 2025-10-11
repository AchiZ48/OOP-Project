import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileMap {
    // ======== constants for Tiled flip flags ========
    private static final long GID_MASK = 0x0FFFFFFFL;
    private final List<Layer> backgroundLayers = new ArrayList<>();
    private final List<Layer> foregroundLayers = new ArrayList<>();

    // ======== internal: layer buckets แบบใหม่ ========
    private final List<Layer> collisionLayers = new ArrayList<>();
    private final List<Layer> zoneLayers = new ArrayList<>();
    // ======== public fields (คงชื่อเดิมไว้ให้เข้ากับโค้ดเก่า) ========
    int tileW, tileH, cols, rows, pixelWidth, pixelHeight;
    boolean decorationVisible;
    List<TilesetEntry> tilesets = new ArrayList<>();

    public TileMap() {
    }

    // ---------------- Load TMX ----------------
    public static TileMap loadFromTMX(String path) throws Exception {
        String normalizedPath = ResourceLoader.normalize(path);
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            throw new IllegalArgumentException("TMX path is empty");
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc;
        try (InputStream stream = ResourceLoader.openStream(normalizedPath)) {
            doc = db.parse(stream);
        }
        Element mapEl = (Element) doc.getElementsByTagName("map").item(0);
        int tileW = Integer.parseInt(mapEl.getAttribute("tilewidth"));
        int tileH = Integer.parseInt(mapEl.getAttribute("tileheight"));
        int width = Integer.parseInt(mapEl.getAttribute("width"));
        int height = Integer.parseInt(mapEl.getAttribute("height"));

        TileMap tm = new TileMap();
        tm.tileW = tileW;
        tm.tileH = tileH;
        tm.cols = width;
        tm.rows = height;
        tm.pixelWidth = width * tileW;
        tm.pixelHeight = height * tileH;


        // ---------------- Load Tilesets ----------------
        NodeList tilesetNodes = doc.getElementsByTagName("tileset");
        for (int i = 0; i < tilesetNodes.getLength(); i++) {
            Element tsEl = (Element) tilesetNodes.item(i);
            int firstGid = Integer.parseInt(tsEl.getAttribute("firstgid"));
            String source = tsEl.getAttribute("source");
            Tileset ts;
            if (!source.isEmpty()) {
                String tsxPath = ResourceLoader.resolve(normalizedPath, source);
                ts = Tileset.loadFromTSX(tsxPath, firstGid);
            } else {
                int tw = Integer.parseInt(tsEl.getAttribute("tilewidth"));
                int th = Integer.parseInt(tsEl.getAttribute("tileheight"));
                ts = Tileset.generatePlaceholder(tw, th);
            }
            tm.tilesets.add(new TilesetEntry(firstGid, ts));
        }

        // ---------------- Load layers ----------------
        NodeList layerNodes = doc.getElementsByTagName("layer");
        int bgCount = 0, fgCount = 0;
        for (int i = 0; i < layerNodes.getLength(); i++) {
            Element lEl = (Element) layerNodes.item(i);
            String name = lEl.getAttribute("name");
            boolean isVisible = !"0".equals(lEl.getAttribute("visible"));

            // อ่าน properties
            Map<String, String> props = readProperties(lEl);
            int type = parseInt(props.get("type"));

            Element dataEl = (Element) lEl.getElementsByTagName("data").item(0);
            // รองรับ CSV เท่านั้นในโค้ดนี้
            String[] tokens = dataEl.getTextContent().trim().split("\\s*,\\s*");

            int[][] layerData = new int[height][width];
            int idx = 0;

            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    if (idx < tokens.length) {
                        String token = tokens[idx++].trim();
                        if (token.isEmpty()) {
                            layerData[r][c] = 0;
                            continue;
                        }

                        long raw = Long.parseUnsignedLong(token);
                        int gid = (int) (raw & GID_MASK);
                        layerData[r][c] = gid;
                    } else {
                        layerData[r][c] = 0;
                    }
                }
            }

            // สร้าง Layer object
            Layer L = new Layer();
            L.name = name.isEmpty() ? "layer_" + i : name;
            L.visible = isVisible;
            L.type = type;
            L.data = layerData;
            L.props.putAll(props);

            // กระจายเข้าบัคเก็ต
            switch (type) {
                case 0 -> {
                    tm.backgroundLayers.add(L);
                    bgCount++;
                }
                case 1 -> {
                    tm.foregroundLayers.add(L);
                    System.out.println("add fore");
                    fgCount++;
                }
                case 2 -> tm.collisionLayers.add(L);
                case 3 -> tm.zoneLayers.add(L);

            }


        }

        // ถ้าไม่มีชื่อ decoration แต่มี foreground ให้เปิด decorationVisible ไว้
        tm.decorationVisible = !tm.foregroundLayers.isEmpty(); // ตามเดิม

        return tm;
    }

    // ======== helpers ========
    private static Map<String, String> readProperties(Element layerEl) {
        Map<String, String> map = new HashMap<>();
        NodeList propsNodes = layerEl.getElementsByTagName("properties");
        if (propsNodes.getLength() == 0) return map;
        Element propsEl = (Element) propsNodes.item(0);
        NodeList propList = propsEl.getElementsByTagName("property");
        for (int i = 0; i < propList.getLength(); i++) {
            Element p = (Element) propList.item(i);
            String key = p.getAttribute("name");
            String v = p.getAttribute("value");
            if (v.isEmpty()) v = p.getTextContent();
            if (!key.isEmpty() && v != null) {
                map.put(key, v.trim());
            }
        }
        return map;
    }

    private static int parseInt(String s) {
        if (s == null) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------- Draw ----------------
    public void draw(Graphics2D g, Camera cam) {
        drawGround(g, cam);
        drawDecorations(g, cam);
    }

    public void drawGround(Graphics2D g, Camera cam) {
        // วาดเฉพาะ background (role=RENDER) ที่ visible
        for (Layer L : backgroundLayers) {
            if (L != null && L.visible) {
                drawLayerTiles(g, L.data, cam);
            }
        }
    }

    public void drawDecorations(Graphics2D g, Camera cam) {
        if (!decorationVisible) {
            return;
        }
        for (Layer L : foregroundLayers) {
            if (L != null && L.visible) {
                drawLayerTiles(g, L.data, cam);
            }
        }
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

        // เช็คทุก collision layer ใหม่
        for (Layer L : collisionLayers) {
            if (L != null && L.data != null) {
                if (L.data[tileY][tileX] > 0) return true;
            }
        }
        return false;
    }

    public int getZone(int tileX, int tileY) {
        for (Layer L : zoneLayers) {
            if (L == null || L.data == null) continue;
            if (tileY < 0 || tileY >= L.data.length || tileX < 0 || tileX >= L.data[0].length)
                continue; // นอกขอบ map


            int gid = L.data[tileY][tileX];
            if (gid <= 0) continue;

            return switch (gid) {
                case 769 -> 1;
                case 770 -> 2;
                case 771 -> 3;
                case 772 -> 4;
                default -> 0;
            };
        }
        return 0;
    }

    public boolean isSolidAtPixel(double wx, double wy) {
        int tileX = (int) (wx / tileW);
        int tileY = (int) (wy / tileH);
        return isSolid(tileX, tileY);
    }

    public void drawCollisionOverlay(Graphics2D g, Camera cam) {
        // วาดจากทุก collision layer
        g.setColor(new Color(255, 0, 0, 128));
        for (Layer L : collisionLayers) {

            if (L == null || L.data == null) continue;

            forVisibleTiles(L.data, cam, (tileX, tileY, gid) -> {
                if (gid <= 0) return;
                int wx = tileX * tileW;
                int wy = tileY * tileH;
                g.fillRect(wx, wy, tileW, tileH);
            });
        }
    }

    public void drawZoneOverlay(Graphics2D g, Camera cam) {
        if (zoneLayers.isEmpty()) return;

        forVisibleTiles(zoneLayers.get(0).data, cam, (tileX, tileY, gid) -> {
            int zone = getZone(tileX, tileY);
            if (zone == 0) return; // ไม่มีโซนตรงนี้

            // สีแต่ละโซน (ตาม zone ID จาก getZone)
            switch (zone) {
                case 1 -> g.setColor(new Color(0, 255, 16, 128));   // green zone
                case 2 -> g.setColor(new Color(0, 104, 10, 128));   // dark green
                case 3 -> g.setColor(new Color(255, 144, 0, 128));  // orange zone
                case 4 -> g.setColor(new Color(0, 217, 255, 128));  // cyan zone
                default -> g.setColor(new Color(255, 255, 255, 128)); // unknown zone
            }

            int wx = tileX * tileW;
            int wy = tileY * tileH;
            g.fillRect(wx, wy, tileW, tileH);
        });
    }

    private interface TileVisitor {
        void accept(int tileX, int tileY, int gid);
    }

    private static final class Layer {
        String name;
        int[][] data;
        boolean visible = true;
        int type = 0;
        // เก็บ properties เผื่อใช้ต่อ
        Map<String, String> props = new HashMap<>();
    }

    static class TilesetEntry {
        int firstGid;
        Tileset tileset;

        public TilesetEntry(int firstGid, Tileset ts) {
            this.firstGid = firstGid;
            this.tileset = ts;
        }
    }


}
