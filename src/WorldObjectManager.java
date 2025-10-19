import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class WorldObjectManager {
    private final GamePanel gamePanel;
    private final Interactions interactions;
    private final List<WorldObject> worldObjects = new ArrayList<>();

    WorldObjectManager(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.interactions = new Interactions();
    }

    Interactions getInteractionManager() {
        return interactions;
    }

    List<WorldObject> getWorldObjects() {
        return Collections.unmodifiableList(worldObjects);
    }

    void add(WorldObject object) {
        if (object == null || worldObjects.contains(object)) {
            return;
        }
        worldObjects.add(object);
        interactions.register(object);
        if (gamePanel != null && object instanceof WorldObjectFactory.FastTravelPoint) {
            gamePanel.registerFastTravelPoint((WorldObjectFactory.FastTravelPoint) object);
        }
    }

    void remove(WorldObject object) {
        if (object == null) {
            return;
        }
        interactions.unregister(object);
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
                interactions.unregister(next);
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    void clear() {
        interactions.clear();
        worldObjects.clear();
    }

    void update(double dt) {
        interactions.update(dt);
    }

    void draw(Graphics2D g) {
        for (WorldObject object : worldObjects) {
            if (object != null) {
                object.draw(g);
            }
        }
    }

    Interactable findBestInteractable(Player actor) {
        return interactions.findBestInteractable(actor);
    }

    boolean tryInteract(Player actor) {
        return interactions.tryInteract(actor);
    }

    interface Interactable {
        Rectangle getInteractionBounds();

        boolean isActive();

        String getInteractionPrompt();

        int getInteractionPriority();

        void interact(InteractionContext context);

        default void update(double dt) {
        }
    }

    static final class InteractionContext {
        private final GamePanel gamePanel;
        private final Player actor;
        private final Interactions interactions;

        InteractionContext(GamePanel gamePanel, Player actor, Interactions interactions) {
            this.gamePanel = gamePanel;
            this.actor = actor;
            this.interactions = interactions;
        }

        GamePanel getGamePanel() {
            return gamePanel;
        }

        Player getActor() {
            return actor;
        }

        Interactions getManager() {
            return interactions;
        }

        void startDialog(DialogTree tree) {
            if (gamePanel != null) {
                gamePanel.startDialog(tree, this);
            }
        }

        void addGold(int amount) {
            if (gamePanel != null) {
                gamePanel.addGold(amount);
            }
        }

        boolean spendGold(int amount) {
            return gamePanel != null && gamePanel.spendGold(amount);
        }

        int getGold() {
            return gamePanel != null ? gamePanel.getGold() : 0;
        }

        void addEssence(int amount) {
            if (gamePanel != null) {
                gamePanel.addEssence(amount);
            }
        }

        boolean spendEssence(int amount) {
            return gamePanel != null && gamePanel.spendEssence(amount);
        }

        int getEssence() {
            return gamePanel != null ? gamePanel.getEssence() : 0;
        }

        QuestManager getQuestManager() {
            return gamePanel != null ? gamePanel.getQuestManager() : null;
        }

        void unlockFastTravel(String pointId) {
            if (gamePanel != null) {
                gamePanel.unlockFastTravel(pointId);
            }
        }

        void queueMessage(String message) {
            if (gamePanel != null) {
                gamePanel.queueWorldMessage(message);
            }
        }

        void openFastTravel(WorldObjectFactory.FastTravelPoint point) {
            if (gamePanel != null) {
                gamePanel.openFastTravel(point);
            }
        }

        void playSfx(String sfxId) {
            if (gamePanel != null) {
                gamePanel.playSfx(sfxId);
            }
        }
    }

    final class Interactions {
        private final List<Interactable> interactables = new ArrayList<>();
        private double interactionRange = 64.0;

        void register(Interactable interactable) {
            if (interactable == null || interactables.contains(interactable)) {
                return;
            }
            interactables.add(interactable);
        }

        void unregister(Interactable interactable) {
            interactables.remove(interactable);
        }

        void clear() {
            interactables.clear();
        }

        void setInteractionRange(double range) {
            interactionRange = Math.max(16.0, range);
        }

        List<Interactable> getInteractables() {
            return Collections.unmodifiableList(interactables);
        }

        void update(double dt) {
            for (int i = 0; i < interactables.size(); i++) {
                Interactable interactable = interactables.get(i);
                if (interactable == null || !interactable.isActive()) {
                    continue;
                }
                interactable.update(dt);
            }
        }

        boolean tryInteract(Player actor) {
            Interactable candidate = findBestInteractable(actor);
            if (candidate == null) {
                return false;
            }
            InteractionContext context = new InteractionContext(gamePanel, actor, this);
            candidate.interact(context);
            return true;
        }

        Interactable findBestInteractable(Player actor) {
            if (actor == null || interactables.isEmpty()) {
                return null;
            }
            Rectangle actorBounds = createActorBounds(actor);
            double bestScore = Double.MAX_VALUE;
            Interactable best = null;
            for (Interactable interactable : interactables) {
                if (interactable == null || !interactable.isActive()) {
                    continue;
                }
                Rectangle bounds = interactable.getInteractionBounds();
                if (bounds == null) {
                    continue;
                }
                double score = scoreInteraction(actorBounds, bounds, interactable.getInteractionPriority());
                if (score < bestScore && score <= interactionRange * interactionRange) {
                    bestScore = score;
                    best = interactable;
                }
            }
            return best;
        }

        private Rectangle createActorBounds(Player actor) {
            int w = Math.max(1, actor.w);
            int h = Math.max(1, actor.h);
            int x = (int) Math.round(actor.getPreciseX());
            int y = (int) Math.round(actor.getPreciseY());
            return new Rectangle(x, y, w, h);
        }

        private double scoreInteraction(Rectangle actorBounds, Rectangle targetBounds, int priority) {
            double dx = centerX(actorBounds) - centerX(targetBounds);
            double dy = centerY(actorBounds) - centerY(targetBounds);
            double distanceSq = dx * dx + dy * dy;
            double priorityWeight = 1.0 + Math.max(0, priority);
            return distanceSq / priorityWeight;
        }

        private double centerX(Rectangle rect) {
            return rect.getX() + rect.getWidth() / 2.0;
        }

        private double centerY(Rectangle rect) {
            return rect.getY() + rect.getHeight() / 2.0;
        }
    }
}
