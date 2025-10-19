import java.awt.*;
import java.io.Serializable;
import java.util.EnumMap;

abstract class WorldObject implements WorldObjectManager.Interactable, Serializable {
    final String id;
    final String type;
    private final EnumMap<StateFlag, Boolean> flags = new EnumMap<>(StateFlag.class);
    double x;
    double y;
    int width;
    int height;
    transient Sprite sprite;
    private int interactionPriority = 1;
    private String prompt = "Interact";

    WorldObject(String id, String type, double x, double y, int width, int height, Sprite sprite) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.sprite = sprite;
        setFlag(StateFlag.USABLE, true);
    }

    String getId() {
        return id;
    }

    String getType() {
        return type;
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    void setPrompt(String prompt) {
        this.prompt = prompt != null ? prompt : "Interact";
    }

    void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    void setFlag(StateFlag flag, boolean value) {
        if (flag == null) {
            return;
        }
        if (value) {
            flags.put(flag, Boolean.TRUE);
        } else {
            flags.remove(flag);
        }
    }

    boolean hasFlag(StateFlag flag) {
        return flags.getOrDefault(flag, Boolean.FALSE);
    }

    boolean isFlagSet(StateFlag flag) {
        return hasFlag(flag);
    }

    boolean isUsable() {
        return isFlagSet(StateFlag.USABLE) && !isFlagSet(StateFlag.BROKEN) && !isFlagSet(StateFlag.CONSUMED);
    }

    void markBroken(String promptOverride) {
        setFlag(StateFlag.BROKEN, true);
        setFlag(StateFlag.USABLE, false);
        if (promptOverride != null && !promptOverride.isEmpty()) {
            setPrompt(promptOverride);
        }
    }

    void restoreUsable() {
        setFlag(StateFlag.BROKEN, false);
        setFlag(StateFlag.CONSUMED, false);
        setFlag(StateFlag.USABLE, true);
    }

    @Override
    public Rectangle getInteractionBounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    @Override
    public boolean isActive() {
        return isUsable();
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
        this.interactionPriority = priority;
    }

    @Override
    public void interact(WorldObjectManager.InteractionContext context) {
        if (!isActive()) {
            onInactiveInteract(context);
            return;
        }
        onInteract(context);
    }

    protected void onInactiveInteract(WorldObjectManager.InteractionContext context) {
        if (context != null && prompt != null && !prompt.isEmpty()) {
            context.queueMessage(prompt);
        }
    }

    protected abstract void onInteract(WorldObjectManager.InteractionContext context);

    void draw(Graphics2D g) {
        if (!isActive()) {
            drawInactive(g);
            return;
        }
        if (sprite != null) {
            sprite.draw(g, x, y, width, height);
        } else {
            g.setColor(new Color(160, 120, 64));
            g.fillRect((int) Math.round(x), (int) Math.round(y), width, height);
        }
    }

    private void drawInactive(Graphics2D g) {
        g.setColor(new Color(64, 64, 64, 180));
        g.fillRect((int) Math.round(x), (int) Math.round(y), width, height);
    }

    enum StateFlag {
        LOCKED,
        OPEN,
        BROKEN,
        USABLE,
        CONSUMED
    }
}

