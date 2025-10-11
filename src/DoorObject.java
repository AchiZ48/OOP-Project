import java.awt.*;

class DoorObject extends WorldObject {
    private boolean locked;
    private boolean open;

    DoorObject(String id, double x, double y, int width, int height, Sprite sprite, boolean locked) {
        super(id, "door", x, y, width, height, sprite);
        this.locked = locked;
        this.open = false;
        if (locked) {
            setFlag(StateFlag.LOCKED, true);
        }
        setPrompt(locked ? "Locked Door" : "Open Door");
        setInteractionPriority(2);
    }

    boolean isLocked() {
        return locked;
    }

    void setLocked(boolean locked) {
        this.locked = locked;
        setFlag(StateFlag.LOCKED, locked);
        if (locked) {
            setPrompt("Locked Door");
        } else if (open) {
            setPrompt("Close Door");
        } else {
            setPrompt("Open Door");
        }
    }

    boolean isOpen() {
        return open;
    }

    void setOpenState(boolean open) {
        this.open = open;
        setFlag(StateFlag.OPEN, open);
        if (open) {
            setPrompt("Close Door");
        } else if (locked) {
            setPrompt("Locked Door");
        } else {
            setPrompt("Open Door");
        }
    }

    @Override
    protected void onInteract(InteractionContext context) {
        if (locked) {
            context.queueMessage("The door is locked.");
            context.playSfx("door_locked");
            return;
        }
        boolean newState = !open;
        setOpenState(newState);
        context.playSfx(newState ? "door_open" : "door_close");
        context.queueMessage(newState ? "Door opened." : "Door closed.");
    }

    @Override
    void draw(Graphics2D g) {
        Color color = locked ? new Color(120, 80, 40) : new Color(160, 110, 60);
        if (open) {
            color = new Color(190, 190, 190);
        }
        g.setColor(color);
        g.fillRect((int) Math.round(x), (int) Math.round(y), width, height);
        if (!open) {
            g.setColor(Color.DARK_GRAY);
            g.drawRect((int) Math.round(x), (int) Math.round(y), width, height);
        }
    }
}
