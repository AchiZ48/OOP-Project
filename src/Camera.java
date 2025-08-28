public class Camera {
    int vw, vh;
    TileMap map;
    double x, y;
    double smooth = 8.0;

    // Zoom properties
    double zoom = 1.0;
    double targetZoom = 2.0;
    double minZoom = 0.5;
    double maxZoom = 4.0;
    double zoomSpeed = 5.0;

    // Virtual viewport dimensions (what we actually render)
    double virtualVw, virtualVh;

    public Camera(int vw, int vh, TileMap map) {
        this.vw = vw;
        this.vh = vh;
        this.map = map;
        x = vw / 2.0;
        y = vh / 2.0;
        updateVirtualDimensions();
    }

    public void update(double dt, Entity follow) {
        // Update zoom smoothly
        if (Math.abs(zoom - targetZoom) > 0.001) {
            zoom += (targetZoom - zoom) * Math.min(1.0, zoomSpeed * dt);
            updateVirtualDimensions();
        }

        if (follow == null) return;

        double targetX = follow.x + follow.w / 2.0;
        double targetY = follow.y + follow.h / 2.0;

        // Smooth following
        x += (targetX - x) * Math.min(1.0, smooth * dt);
        y += (targetY - y) * Math.min(1.0, smooth * dt);

        // Snap camera position to prevent sub-pixel positioning
        snapCameraPosition();
        clampToMap();
    }

    public void followEntity(Entity e) {
        if (e != null) {
            x = e.x + e.w / 2.0;
            y = e.y + e.h / 2.0;
            snapCameraPosition();
            clampToMap();
        }
    }

    void clampToMap() {
        if (map == null) return;

        double halfW = virtualVw / 2.0, halfH = virtualVh / 2.0;
        double maxX = Math.max(halfW, map.pixelWidth - halfW);
        double maxY = Math.max(halfH, map.pixelHeight - halfH);

        x = Math.max(halfW, Math.min(maxX, x));
        y = Math.max(halfH, Math.min(maxY, y));
    }

    // Snap camera position to pixel boundaries based on zoom level
    private void snapCameraPosition() {
        double pixelSize = 1.0 / zoom;
        x = Math.round(x / pixelSize) * pixelSize;
        y = Math.round(y / pixelSize) * pixelSize;
    }

    void updateVirtualDimensions() {
        // Virtual viewport is smaller at higher zoom (shows less world)
        virtualVw = vw / zoom;
        virtualVh = vh / zoom;
    }

    // Zoom controls
    public void zoomIn() {
        setTargetZoom(Math.min(maxZoom, targetZoom * 1.25));
    }

    public void zoomOut() {
        setTargetZoom(Math.max(minZoom, targetZoom / 1.25));
    }

    public void setTargetZoom(double newZoom) {
        targetZoom = Math.max(minZoom, Math.min(maxZoom, newZoom));
    }

    public double getZoom() {
        return zoom;
    }

    public void resetZoom() {
        setTargetZoom(1.0);
    }

    // Convert world coordinates to screen coordinates (no scaling, just offset)
    int worldToScreenX(double wx) {
        return (int) Math.round(wx - (x - virtualVw / 2.0));
    }

    int worldToScreenY(double wy) {
        return (int) Math.round(wy - (y - virtualVh / 2.0));
    }

}