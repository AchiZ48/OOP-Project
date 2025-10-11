import java.awt.*;

abstract class NPC extends Entity implements Interactable {
    final String id;
    private final Rectangle bounds = new Rectangle();
    private String prompt = "Talk";
    private int interactionPriority = 2;
    private boolean active = true;

    NPC(String id, String name, Sprite sprite, double x, double y, int width, int height) {
        this.id = id;
        this.name = name;
        this.sprite = sprite;
        this.w = Math.max(1, width);
        this.h = Math.max(1, height);
        setPrecisePosition(x, y);
        setHealth(1, 1);
        updateBounds();
    }

    private void updateBounds() {
        bounds.setBounds(
                (int) Math.round(getPreciseX()),
                (int) Math.round(getPreciseY()),
                Math.max(1, w),
                Math.max(1, h)
        );
    }

    void setPrompt(String prompt) {
        this.prompt = (prompt == null || prompt.isEmpty()) ? "Talk" : prompt;
    }

    protected void setHealth(int currentHp, int maximumHp) {
        this.stats.setBaseValue(Stats.StatType.MAX_HP, Math.max(1, maximumHp));
        this.stats.setCurrentHp(Math.max(0, (Math.min(this.stats.getMaxHp(), currentHp))));
    }

    protected int getCurrentHp() {
        return this.stats.getCurrentHp();
    }

    @Override
    public Rectangle getInteractionBounds() {
        updateBounds();
        return bounds;
    }

    @Override
    public boolean isActive() {
        return active && getCurrentHp() > 0;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getInteractionPrompt() {
        return prompt;
    }

    @Override
    public int getInteractionPriority() {
        return interactionPriority;
    }

    void setInteractionPriority(int priority) {
        interactionPriority = Math.max(0, priority);
    }

    @Override
    public void interact(InteractionContext context) {
        if (!isActive()) {
            return;
        }
        onInteract(context);
    }

    @Override
    public void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }

    protected abstract void onInteract(InteractionContext context);
}