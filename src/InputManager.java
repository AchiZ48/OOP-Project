import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InputManager {
    JPanel panel;
    Set<String> keysPressed = new HashSet<>();
    Set<String> keysConsumed = new HashSet<>();
    Map<String, Runnable> oneShot = new HashMap<>();

    public InputManager(JPanel p) {
        this.panel = p;
        setupBindings();
    }

    void setupBindings() {
        // Movement keys
        registerKey("LEFT", "LEFT");
        registerKey("RIGHT", "RIGHT");
        registerKey("UP", "UP");
        registerKey("DOWN", "DOWN");

        // Number keys
        registerKey("1", "1");
        registerKey("2", "2");
        registerKey("3", "3");

        // Action keys
        registerKey("N", "N");
        registerKey("L", "L");
        registerKey("D", "D");
        registerKey("B", "B");

        // Control keys
        registerKey("ENTER", "ENTER");
        registerKey("ESCAPE", "ESC");
    }

    void registerKey(String keyStroke, String name) {
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();

        im.put(KeyStroke.getKeyStroke(keyStroke), "pressed_" + name);
        im.put(KeyStroke.getKeyStroke("released " + keyStroke), "released_" + name);

        am.put("pressed_" + name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                keysPressed.add(name);
            }
        });

        am.put("released_" + name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                keysPressed.remove(name);
                keysConsumed.remove(name);
                Runnable r = oneShot.remove(name);
                if (r != null) r.run();
            }
        });
    }

    public void bindKey(String key, Runnable action) {
        oneShot.put(key, action);
    }

    public void update() {
        // Clear consumed keys that are no longer pressed
        keysConsumed.retainAll(keysPressed);
    }

    public boolean isPressed(String k) {
        return keysPressed.contains(k) && !keysConsumed.contains(k);
    }

    public boolean consumeIfPressed(String k) {
        if (keysPressed.contains(k) && !keysConsumed.contains(k)) {
            keysConsumed.add(k);
            return true;
        }
        return false;
    }
}
