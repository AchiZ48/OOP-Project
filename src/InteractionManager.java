import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class InteractionManager {
    private final GamePanel gamePanel;
    private final List<Interactable> interactables = new ArrayList<>();
    private double interactionRange = 64.0;

    InteractionManager(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

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
