import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class WorldObjectManager {
    private final GamePanel gamePanel;
    private final InteractionManager interactionManager;
    private final List<WorldObject> worldObjects = new ArrayList<>();

    WorldObjectManager(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.interactionManager = new InteractionManager(gamePanel);
    }

    InteractionManager getInteractionManager() {
        return interactionManager;
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

    Interactable findBestInteractable(Player actor) {
        return interactionManager.findBestInteractable(actor);
    }

    boolean tryInteract(Player actor) {
        return interactionManager.tryInteract(actor);
    }
}
