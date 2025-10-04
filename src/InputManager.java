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
    JPanel panel;
    Set<String> keysPressed = new HashSet<>();
    Set<String> keysConsumed = new HashSet<>();
    Map<String, Runnable> oneShot = new HashMap<>();


    public InputManager(JPanel p) {
        this.panel = p;
        panel.setFocusTraversalKeysEnabled(false);
        setupBindings();

        // เพิ่ม KeyEventDispatcher เพื่อจับ SHIFT/CTRL/ALT
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

            // ถ้าอยากจับ CTRL / ALT ด้วยก็เพิ่มแบบเดียวกัน
            return false; // ส่งต่อ event ไป component อื่นด้วย
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

        // Control keys
        registerKey(KeyEvent.VK_ENTER, "ENTER");
        registerKey(KeyEvent.VK_ESCAPE, "ESC");
        registerKey(KeyEvent.VK_SPACE, "SPACE");
        registerKey(KeyEvent.VK_E, "E");
        registerKey(KeyEvent.VK_Q, "Q");
        registerKey(KeyEvent.VK_R, "R");
        registerKey(KeyEvent.VK_MULTIPLY, "*");
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
                // run oneShot ถ้ามี bind ไว้
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
        // Clear consumed keys ที่ไม่ถูกกดแล้ว
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
