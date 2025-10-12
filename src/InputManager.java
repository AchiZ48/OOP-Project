import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InputManager {
    private JPanel panel;
    private Set<String> keysPressed = new HashSet<>();
    private Set<String> keysConsumed = new HashSet<>();
    private Map<String, Runnable> oneShot = new HashMap<>();


    public InputManager(JPanel p) {
        this.panel = p;
        panel.setFocusTraversalKeysEnabled(false);
        setupBindings();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            int code = e.getKeyCode();

            // SHIFT
            if (code == KeyEvent.VK_SHIFT) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    keysPressed.add("SHIFT");
                } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                    keysPressed.remove("SHIFT");
                    keysConsumed.remove("SHIFT");
                }
            }

            return false;
        });
    }

    void setupBindings() {
        // Movement keys
        registerKey(KeyEvent.VK_A, "LEFT");
        registerKey(KeyEvent.VK_D, "RIGHT");
        registerKey(KeyEvent.VK_W, "UP");
        registerKey(KeyEvent.VK_S, "DOWN");
        registerKey(KeyEvent.VK_LEFT, "LEFT");
        registerKey(KeyEvent.VK_RIGHT, "RIGHT");
        registerKey(KeyEvent.VK_UP, "UP");
        registerKey(KeyEvent.VK_DOWN, "DOWN");
        registerKey(KeyEvent.VK_SHIFT, "SHIFT");

        // Number keys
        registerKey(KeyEvent.VK_1, "1");
        registerKey(KeyEvent.VK_2, "2");
        registerKey(KeyEvent.VK_3, "3");

        // Action keys
        registerKey(KeyEvent.VK_B, "B");
        registerKey(KeyEvent.VK_P, "P");
        registerKey(KeyEvent.VK_C, "C");
        registerKey(KeyEvent.VK_EQUALS, "EQUALS");
        registerKey(KeyEvent.VK_MINUS, "MINUS");
        registerKey(KeyEvent.VK_0, "0");
        registerKey(KeyEvent.VK_TAB, "TAB");

        // Control keys
        registerKey(KeyEvent.VK_ENTER, "ENTER");
        registerKey(KeyEvent.VK_ESCAPE, "ESC");
        registerKey(KeyEvent.VK_SPACE, "SPACE");
        registerKey(KeyEvent.VK_E, "E");
        registerKey(KeyEvent.VK_Q, "Q");
        registerKey(KeyEvent.VK_R, "R");
        registerKey(KeyEvent.VK_MULTIPLY, "*");
        registerKey(KeyEvent.VK_N, "N");
        registerKey(KeyEvent.VK_L, "L");
        registerKey(KeyEvent.VK_DELETE, "DELETE");
    }

    void registerKey(int keyStroke, String name) {
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();

        // press
        im.put(KeyStroke.getKeyStroke(keyStroke, 0, false), "pressed_" + name);
        // release
        im.put(KeyStroke.getKeyStroke(keyStroke, 0, true), "released_" + name);
        im.put(KeyStroke.getKeyStroke(keyStroke, InputEvent.SHIFT_DOWN_MASK, false), "pressed_" + name);
        im.put(KeyStroke.getKeyStroke(keyStroke, InputEvent.SHIFT_DOWN_MASK, true), "released_" + name);

        am.put("pressed_" + name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                keysPressed.add(name);
                Runnable r = oneShot.remove(name);
                if (r != null) r.run();
            }
        });

        am.put("released_" + name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                keysPressed.remove(name);
                keysConsumed.remove(name);
            }
        });
    }

    public void bindKey(String key, Runnable action) {
        oneShot.put(key, action);
    }

    public void update() {
        keysConsumed.retainAll(keysPressed);
    }

    public boolean isPressed(String k) {
        return keysPressed.contains(k) && !keysConsumed.contains(k);
    }

    public boolean consumeIfPressed(String k) {
        if (!keysPressed.contains(k)) {
            keysConsumed.remove(k);
            return false;
        }
        if (keysConsumed.contains(k)) {
            return false;
        }
        keysConsumed.add(k);
        return true;
    }
}
