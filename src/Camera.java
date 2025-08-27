public class Camera {
    int vw, vh;
    TileMap map;
    double x, y;
    double smooth = 8.0;

    public Camera(int vw, int vh, TileMap map) {
        this.vw = vw;
        this.vh = vh;
        this.map = map;
        x = vw / 2.0;
        y = vh / 2.0;
    }

    public void update(double dt, Entity follow) {
        if (follow == null) return;

        double targetX = follow.x + follow.w / 2.0;
        double targetY = follow.y + follow.h / 2.0;

        // Smooth following
        x += (targetX - x) * Math.min(1.0, smooth * dt);
        y += (targetY - y) * Math.min(1.0, smooth * dt);

        clampToMap();
    }

    public void followEntity(Entity e) {
        if (e != null) {
            x = e.x + e.w / 2.0;
            y = e.y + e.h / 2.0;
            clampToMap();
        }
    }

    void clampToMap() {
        if (map == null) return;

        double halfW = vw / 2.0, halfH = vh / 2.0;
        double minX = halfW, minY = halfH;
        double maxX = Math.max(halfW, map.pixelWidth - halfW);
        double maxY = Math.max(halfH, map.pixelHeight - halfH);

        x = Math.max(minX, Math.min(maxX, x));
        y = Math.max(minY, Math.min(maxY, y));
    }

    int worldToScreenX(double wx) {
        return (int) Math.round(wx - (x - vw / 2.0));
    }

    int worldToScreenY(double wy) {
        return (int) Math.round(wy - (y - vh / 2.0));
    }
}
