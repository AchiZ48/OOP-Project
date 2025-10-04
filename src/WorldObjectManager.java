import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class WorldObjectManager {
    private final GamePanel gamePanel;
    private final InteractionManager interactionManager;
    private final PlacementManager placementManager;
    private final List<WorldObject> worldObjects = new ArrayList<>();

    WorldObjectManager(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.interactionManager = new InteractionManager(gamePanel);
        this.placementManager = new PlacementManager();
    }

    InteractionManager getInteractionManager() {
        return interactionManager;
    }

    PlacementManager getPlacementManager() {
        return placementManager;
    }

    List<WorldObject> getWorldObjects() {
        return Collections.unmodifiableList(worldObjects);
    }

    void add(WorldObject object) {
        if (object == null || worldObjects.contains(object)) {
            return;
        }
        worldObjects.add(object);
        interactionManager.register(object);
        if (gamePanel != null && object instanceof FastTravelPoint) {
            gamePanel.registerFastTravelPoint((FastTravelPoint) object);
        }
    }

    void remove(WorldObject object) {
        if (object == null) {
            return;
        }
        interactionManager.unregister(object);
        worldObjects.remove(object);
    }

    boolean removeById(String id) {
        if (id == null) {
            return false;
        }
        Iterator<WorldObject> it = worldObjects.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            WorldObject next = it.next();
            if (next != null && id.equals(next.getId())) {
                interactionManager.unregister(next);
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    void clear() {
        interactionManager.clear();
        worldObjects.clear();
    }

    void update(double dt) {
        interactionManager.update(dt);
    }

    void draw(Graphics2D g) {
        for (WorldObject object : worldObjects) {
            if (object != null) {
                object.draw(g);
            }
        }
    }

    WorldObject placeCurrent(Player player, TileMap map) {
        WorldObject candidate = placementManager.place(player);
        if (candidate == null) {
            return null;
        }
        if (collides(candidate, map)) {
            return null;
        }
        add(candidate);
        return candidate;
    }

    private boolean collides(WorldObject candidate, TileMap map) {
        Rectangle bounds = candidate.getInteractionBounds();
        if (bounds == null) {
            return false;
        }
        for (WorldObject existing : worldObjects) {
            if (existing == null) {
                continue;
            }
            Rectangle other = existing.getInteractionBounds();
            if (other != null && other.intersects(bounds)) {
                return true;
            }
        }
        if (map != null) {
            if (bounds.x < 0 || bounds.y < 0) {
                return true;
            }
            if (bounds.x + bounds.width > map.pixelWidth || bounds.y + bounds.height > map.pixelHeight) {
                return true;
            }
            double left = bounds.getMinX();
            double right = bounds.getMaxX();
            double top = bounds.getMinY();
            double bottom = bounds.getMaxY();
            if (map.isSolidAtPixel(left, top) || map.isSolidAtPixel(right, top)
                    || map.isSolidAtPixel(left, bottom) || map.isSolidAtPixel(right, bottom)) {
                return true;
            }
        }
        return false;
    }

    Interactable findBestInteractable(Player actor) {
        return interactionManager.findBestInteractable(actor);
    }

    boolean tryInteract(Player actor) {
        return interactionManager.tryInteract(actor);
    }
}