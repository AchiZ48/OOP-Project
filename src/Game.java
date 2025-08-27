import javax.swing.*;

public class Game {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) { /* ignore */ }

            GameWindow gw = new GameWindow();
            gw.show();
        });
    }
}

