import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class TileMap {
    int tileW, tileH, cols, rows, pixelWidth, pixelHeight;
    int[][] layer;          // Main visual layer
    int[][] decorationLayer; // Additional decoration layer
    int[][] collisionLayer;  // Collision layer
    boolean decorationVisible;

    // List ของ Tileset + firstGid
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
        tm.pixelWidth = width*tileW; tm.pixelHeight = height*tileH;

        tm.layer = new int[height][width];
        tm.decorationLayer = new int[height][width];
        tm.collisionLayer = new int[height][width];
        tm.decorationVisible = true;

        // ---------------- Load Tilesets ----------------
        NodeList tilesetNodes = doc.getElementsByTagName("tileset");
        for(int i=0;i<tilesetNodes.getLength();i++){
            Element tsEl = (Element) tilesetNodes.item(i);
            int firstGid = Integer.parseInt(tsEl.getAttribute("firstgid"));
            String source = tsEl.getAttribute("source");
            Tileset ts;
            if(!source.isEmpty()){
                File tsxFile = new File(f.getParentFile(), source);
                ts = Tileset.loadFromTSX(tsxFile.getPath(), firstGid);
            } else {
                // Inline tileset placeholder
                int tw = Integer.parseInt(tsEl.getAttribute("tilewidth"));
                int th = Integer.parseInt(tsEl.getAttribute("tileheight"));
                ts = Tileset.generatePlaceholder(tw, th);
            }
            tm.tilesets.add(new TilesetEntry(firstGid, ts));
        }

        // ---------------- Load layers ----------------
        NodeList layerNodes = doc.getElementsByTagName("layer");
        for(int i=0;i<layerNodes.getLength();i++){
            Element lEl = (Element) layerNodes.item(i);
            String name = lEl.getAttribute("name");
            String visibleAttr = lEl.getAttribute("visible");
            boolean isVisible = !visibleAttr.equals("0");

            Element dataEl = (Element) lEl.getElementsByTagName("data").item(0);
            String[] tokens = dataEl.getTextContent().trim().split("\\s*,\\s*");

            int[][] layerData = new int[height][width];
            int idx=0;
            for(int r=0;r<height;r++){
                for(int c=0;c<width;c++){
                    if(idx<tokens.length){
                        int gid = Integer.parseInt(tokens[idx++]);
                        if("collision".equalsIgnoreCase(name)){
                            layerData[r][c] = gid>0?1:0;
                        } else {
                            layerData[r][c] = gid;
                        }
                    } else layerData[r][c]=0;
                }
            }

            switch(name.toLowerCase()){
                case "collision": tm.collisionLayer = layerData; break;
                case "decoration": tm.decorationLayer = layerData; tm.decorationVisible = isVisible; break;
                case "ground": default: tm.layer = layerData; break;
            }
        }

        return tm;
    }

    // ---------------- Draw ----------------
    public void draw(Graphics2D g, Camera cam){
        drawLayer(g, cam, layer);
        if(decorationLayer!=null && decorationVisible) drawLayer(g, cam, decorationLayer);
    }

    private void drawLayer(Graphics2D g, Camera cam, int[][] layerData){
        int startCol = Math.max(0, (int)((cam.x - (double) cam.vw /2)/tileW)-1);
        int endCol = Math.min(cols, (int)((cam.x + (double) cam.vw /2)/tileW)+2);
        int startRow = Math.max(0, (int)((cam.y - (double) cam.vh /2)/tileH)-1);
        int endRow = Math.min(rows, (int)((cam.y + (double) cam.vh /2)/tileH)+2);

        for(int r=startRow;r<endRow;r++){
            for(int c=startCol;c<endCol;c++){
                int gid = layerData[r][c];
                if(gid<=0) continue;
                int wx = c*tileW, wy=r*tileH;
                int sx = cam.worldToScreenX(wx), sy=cam.worldToScreenY(wy);
                drawGID(g, gid, sx, sy, tileW, tileH);
            }
        }
    }

    private void drawGID(Graphics2D g, int gid, int x, int y, int w, int h){
        if(gid<=0) return;
        TilesetEntry entry=null;
        for(int i=tilesets.size()-1;i>=0;i--){
            if(gid >= tilesets.get(i).firstGid){
                entry = tilesets.get(i);
                break;
            }
        }
        if(entry==null) return;
        int localId = gid - entry.firstGid;
        entry.tileset.drawTile(g, localId, x, y, w, h);
    }

    // ---------------- Depth-aware drawing with entities ----------------
    public void drawWithDepth(Graphics2D g, Camera cam, List<Entity> entities){
        drawLayer(g, cam, layer); // Ground first

        int startCol = Math.max(0, (int)((cam.x - (double) cam.vw /2)/tileW)-1);
        int endCol = Math.min(cols, (int)((cam.x + (double) cam.vw /2)/tileW)+2);
        int startRow = Math.max(0, (int)((cam.y - (double) cam.vh /2)/tileH)-1);
        int endRow = Math.min(rows, (int)((cam.y + (double) cam.vh /2)/tileH)+2);

        for(int r=startRow;r<endRow;r++){
            int currentY = r*tileH;

            if(decorationLayer!=null && decorationVisible){
                for(int c=startCol;c<endCol;c++){
                    int gid = decorationLayer[r][c];
                    if(gid<=0) continue;
                    int wx = c*tileW, wy=r*tileH;
                    int sx = cam.worldToScreenX(wx), sy=cam.worldToScreenY(wy);
                    drawGID(g, gid, sx, sy, tileW, tileH);
                }
            }

            if(entities!=null){
                for(Entity e: entities){
                    int eb = e.y+e.h;
                    if(eb>currentY && eb <= currentY+tileH){
                        e.draw(g, cam);
                    }
                }
            }
        }

        // Draw entities below visible rows
        if(entities!=null){
            int belowY = endRow*tileH;
            for(Entity e: entities){
                if(e.y+e.h > belowY) e.draw(g, cam);
            }
        }
    }

    // ---------------- Collision ----------------
    public boolean isSolid(int tileX, int tileY){
        if(tileX<0||tileX>=cols||tileY<0||tileY>=rows) return true;
        return collisionLayer[tileY][tileX]>0;
    }
    public boolean isSolidAtPixel(int wx, int wy){
        return !isSolid(wx / tileW, wy / tileH);
    }
    public int getCollisionAt(int tileX,int tileY){
        if(tileX<0||tileX>=cols||tileY<0||tileY>=rows) return 1;
        return collisionLayer[tileY][tileX];
    }

    public void drawCollisionOverlay(Graphics2D g, Camera cam, Color color){
        if(collisionLayer==null) return;

        int startCol = Math.max(0, (int)((cam.x - (double) cam.vw /2)/tileW)-1);
        int endCol = Math.min(cols, (int)((cam.x + (double) cam.vw /2)/tileW)+2);
        int startRow = Math.max(0, (int)((cam.y - (double) cam.vh /2)/tileH)-1);
        int endRow = Math.min(rows, (int)((cam.y + (double) cam.vh /2)/tileH)+2);

        g.setColor(color);
        for(int r=startRow;r<endRow;r++){
            for(int c=startCol;c<endCol;c++){
                if(collisionLayer[r][c]>0){
                    int wx=c*tileW, wy=r*tileH;
                    int sx=cam.worldToScreenX(wx), sy=cam.worldToScreenY(wy);
                    g.fillRect(sx, sy, tileW, tileH);
                }
            }
        }
    }
}
