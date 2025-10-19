public class Camera {
    // Virtual viewport dimensions (what we actually render)
    double virtualVw, virtualVh;
    private final int vw;
    private final int vh;
    private final TileMap map;
    private double x, y;
    private double fracX = 0.0;
    private double fracY = 0.0;
    private final double smooth = 8.0;
    // Zoom properties
    private double zoom = 1.0;
    private double targetZoom = 1.0;
    private final double minZoom = 0.5;
    private final double maxZoom = 4.0;
    private final double zoomSpeed = 5.0;

    public Camera(int vw, int vh, TileMap map) {
        this.vw = vw;
        this.vh = vh;
        this.map = map;
        setPreciseCenter(vw / 2.0, vh / 2.0);
        updateVirtualDimensions();
    }

    public void update(double dt, Entity follow) {
        // Update zoom smoothly
        if (Math.abs(zoom - targetZoom) > 0.001) {
            zoom += (targetZoom - zoom) * Math.min(1.0, zoomSpeed * dt);
            updateVirtualDimensions();
            clampToMap();
        }

        if (follow == null) return;

        double targetX = follow.getPreciseX() + follow.w / 2.0;
        double targetY = follow.getPreciseY() + follow.h / 2.0;

        double currentX = getPreciseX();
        double currentY = getPreciseY();

        // Smooth following with fractional accumulator
        double followFactor = 1.0 - Math.exp(-smooth * dt);
        currentX += (targetX - currentX) * followFactor;
        currentY += (targetY - currentY) * followFactor;

        setPreciseCenter(clampX(currentX), clampY(currentY));
    }

    public void followEntity(Entity e) {
        if (e != null) {
            double centerX = clampX(e.getPreciseX() + e.w / 2.0);
            double centerY = clampY(e.getPreciseY() + e.h / 2.0);
            setPreciseCenter(centerX, centerY);
        }
    }

    public void setCenter(double px, double py) {
        setPreciseCenter(clampX(px), clampY(py));
    }

    void clampToMap() {
        setPreciseCenter(clampX(getPreciseX()), clampY(getPreciseY()));
    }

    void updateVirtualDimensions() {
        // Virtual viewport is smaller at higher zoom (shows less world)
        virtualVw = vw / zoom;
        virtualVh = vh / zoom;
    }

    // Zoom controls
    public void zoomIn() {
        setTargetZoom(Math.min(maxZoom, targetZoom + 0.1));
    }

    public void zoomOut() {
        setTargetZoom(Math.max(minZoom, targetZoom - 0.1));
    }

    public void setTargetZoom(double newZoom) {
        targetZoom = Math.max(minZoom, Math.min(maxZoom, newZoom));
    }

    public double getViewWidth() {
        return virtualVw;
    }

    public double getViewHeight() {
        return virtualVh;
    }

    public double getZoom() {
        return zoom;
    }

    public double getX() {
        return getPreciseX();
    }

    public double getY() {
        return getPreciseY();
    }

    double getPreciseX() {
        return x + fracX;
    }

    double getPreciseY() {
        return y + fracY;
    }

    private void setPreciseCenter(double px, double py) {
        double flooredX = Math.floor(px);
        fracX = px - flooredX;
        x = flooredX;
        double flooredY = Math.floor(py);
        fracY = py - flooredY;
        y = flooredY;
    }

    private double clampX(double value) {
        if (map == null) return value;

        double halfW = virtualVw / 2.0;
        double maxX = Math.max(halfW, map.pixelWidth - halfW);
        return Math.max(halfW, Math.min(maxX, value));
    }

    private double clampY(double value) {
        if (map == null) return value;

        double halfH = virtualVh / 2.0;
        double maxY = Math.max(halfH, map.pixelHeight - halfH);
        return Math.max(halfH, Math.min(maxY, value));
    }

    public double getRenderX() {
        return x;
    }

    public double getRenderY() {
        return y;
    }

    public void resetZoom() {
        setTargetZoom(1.0);
    }

}



