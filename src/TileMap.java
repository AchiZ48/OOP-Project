import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
public class TileMap {
    int tileW, tileH, cols, rows, pixelWidth, pixelHeight;
    int[][] layer;
    int[][] decorationLayer;
    int[][] collisionLayer;
    int[][] zoneLayer;
    boolean decorationVisible;
    List<TilesetEntry> tilesets = new ArrayList<>();
    // pre-render images
    BufferedImage groundImage;
    BufferedImage decorationImage;
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
                case "decoration": tm.decorationLayer = layerData; tm.decorationVisible = true; break;
                case "ground": tm.layer = layerData; break;
                case "zone": tm.zoneLayer = layerData; break;
                default: tm.layer = layerData; break;
            }
        }
        // ---------- Pre-render images ----------
        tm.groundImage = tm.renderLayer(tm.layer);
        tm.decorationImage = tm.renderLayer(tm.decorationLayer);
        return tm;
    }
    // Render one layer into BufferedImage
    private BufferedImage renderLayer(int[][] layerData){
        if(layerData == null) return null;
        BufferedImage img = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        for(int r=0;r<rows;r++){
            for(int c=0;c<cols;c++){
                int gid = layerData[r][c];
                if(gid<=0) continue;
                int wx = c*tileW, wy=r*tileH;
                drawGID(g, gid, wx, wy, tileW, tileH);
            }
        }
        g.dispose();
        return img;
    }
    // ---------------- Draw ----------------
    public void draw(Graphics2D g, Camera cam){
        if(groundImage != null){
            g.drawImage(groundImage, 0, 0, null);
        }
        if(decorationImage != null && decorationVisible){
            g.drawImage(decorationImage, 0, 0, null);
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
    // ---------------- Collision ----------------
    public boolean isSolid(int tileX, int tileY){
        if(tileX<0||tileX>=cols||tileY<0||tileY>=rows) return true;
        return collisionLayer[tileY][tileX]>0;
    }
    public boolean isSolidAtPixel(double wx, double wy){
        int tileX = (int) (wx / tileW);
        int tileY = (int) (wy / tileH);
        return !isSolid(tileX, tileY);
    }
    public int getCollisionAt(int tileX,int tileY){
        if(tileX<0||tileX>=cols||tileY<0||tileY>=rows) return 1;
        return collisionLayer[tileY][tileX];
    }
    public void drawCollisionOverlay(Graphics2D g, Camera cam){
        if(collisionLayer==null) return;
        g.setColor(new Color(255, 0, 0, 128));
        for(int r=0;r<rows;r++){
            for(int c=0;c<cols;c++){
                if(collisionLayer[r][c]>0){
                    int wx=c*tileW, wy=r*tileH;
                    g.fillRect(wx, wy, tileW, tileH);
                }
            }
        }
    }
    public void drawZoneOverlay(Graphics2D g, Camera cam){
        if(zoneLayer==null) return;
        for(int r=0;r<rows;r++){
            for(int c=0;c<cols;c++){
                if(zoneLayer[r][c]>0){
                    switch (zoneLayer[r][c]){
                        case 1025 : g.setColor(new Color(0, 255, 16, 128)); break;
                        case 1026 : g.setColor(new Color(0, 104, 10, 128)); break;
                        case 1027 : g.setColor(new Color(255, 144, 0, 128)); break;
                        case 1028 : g.setColor(new Color(0, 217, 255, 128)); break;
                    }
                    int wx=c*tileW, wy=r*tileH;
                    g.fillRect(wx, wy, tileW, tileH);
                }
            }
        }
    }
}
