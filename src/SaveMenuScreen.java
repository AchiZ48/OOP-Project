import javax.swing.*;
import java.awt.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class SaveMenuScreen {
    GamePanel gp;
    List<String> saves = new ArrayList<>();
    int selected = 0;

    public SaveMenuScreen(GamePanel gp) {
        this.gp = gp;
        refresh();
    }

    void refresh() {
        saves.clear();
        try {
            Files.createDirectories(Paths.get("saves"));
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("saves"), "*.sav")) {
                for (Path p : ds) {
                    String name = p.getFileName().toString().replaceFirst("\\.sav$", "");
                    saves.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh saves: " + e.getMessage());
        }

        // Sort saves alphabetically
        saves.sort(String::compareToIgnoreCase);

        // Ensure selected index is valid
        int maxOptions = Math.max(3, 2 + saves.size()); // New Game, saves, Back
        if (selected >= maxOptions) selected = maxOptions - 1;
        if (selected < 0) selected = 0;
    }

    void update(double dt) {
        InputManager input = gp.input;
        int totalOptions = 2 + saves.size(); // New Game + saves + Back

        // Navigation
        if (input.consumeIfPressed("UP")) {
            selected = (selected - 1 + totalOptions) % totalOptions;
        }
        if (input.consumeIfPressed("DOWN")) {
            selected = (selected + 1) % totalOptions;
        }

        // Selection
        if (input.consumeIfPressed("ENTER")) {
            handleSelection();
        }

        // Quick keys
        if (input.consumeIfPressed("N")) {
            promptNewGame();
        }
        if (input.consumeIfPressed("L") && saves.size() > 0) {
            int saveIndex = selected - 1;
            if (saveIndex >= 0 && saveIndex < saves.size()) {
                String name = saves.get(saveIndex);
                if (gp.loadGame(name)) {
                    gp.state = GamePanel.State.WORLD;
                }
            }
        }
        if (input.consumeIfPressed("D") && saves.size() > 0) {
            int saveIndex = selected - 1;
            if (saveIndex >= 0 && saveIndex < saves.size()) {
                confirmDeleteSave(saves.get(saveIndex));
            }
        }
        if (input.consumeIfPressed("ESC")) {
            gp.state = GamePanel.State.TITLE;
        }
    }

    void handleSelection() {
        if (selected == 0) {
            // New Game
            promptNewGame();
        } else if (selected <= saves.size()) {
            // Load save
            String name = saves.get(selected - 1);
            if (gp.loadGame(name)) {
                gp.state = GamePanel.State.WORLD;
            }
        } else {
            // Back
            gp.state = GamePanel.State.TITLE;
        }
    }

    void promptNewGame() {
        final String[] result = new String[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                result[0] = JOptionPane.showInputDialog(gp,
                        "Enter save name (leave blank to skip saving):",
                        "New Game", JOptionPane.PLAIN_MESSAGE);
            });
        } catch (Exception e) {
            System.err.println("Dialog error: " + e.getMessage());
            result[0] = null;
        }

        if (result[0] != null) {
            String saveName = result[0].trim();
            gp.startNewGame(saveName.isEmpty() ? null : saveName);
        }
    }

    void confirmDeleteSave(String saveName) {
        final int[] result = new int[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                result[0] = JOptionPane.showConfirmDialog(gp,
                        "Delete save '" + saveName + "'?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
            });
        } catch (Exception ex) {
            System.err.println("Dialog error: " + ex.getMessage());
            result[0] = JOptionPane.NO_OPTION;
        }

        if (result[0] == JOptionPane.YES_OPTION) {
            if (gp.deleteSave(saveName)) {
                refresh();
            }
        }
    }

    void draw(Graphics2D g) {
        // Background
        g.setColor(new Color(10, 10, 30));
        g.fillRect(0, 0, gp.vw, gp.vh);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("Save Menu", 24, 44);

        // Instructions
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("N: New | L: Load | D: Delete | Esc: Back | Enter: Select", 24, 72);

        // Menu options
        int x = 24, y0 = 110;
        int rowH = 24;

        // New Game option
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(selected == 0 ? Color.YELLOW : Color.WHITE);
        g.drawString((selected == 0 ? "> " : "  ") + "[ New Game ]", x, y0);

        // Save files
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        int maxVisible = Math.max(5, saves.size());
        for (int i = 0; i < maxVisible; i++) {
            int y = y0 + (i + 1) * rowH;
            int optionIndex = i + 1;

            if (i < saves.size()) {
                String name = saves.get(i);
                g.setColor(selected == optionIndex ? Color.YELLOW : Color.WHITE);
                g.drawString((selected == optionIndex ? "> " : "  ") + name, x, y);
            } else {
                g.setColor(selected == optionIndex ? new Color(100, 100, 0) : Color.GRAY);
                g.drawString((selected == optionIndex ? "> " : "  ") + "<empty>", x, y);
            }
        }

        // Back option
        int backIndex = 1 + maxVisible;
        int backY = y0 + (maxVisible + 1) * rowH;
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(selected == backIndex ? Color.YELLOW : Color.WHITE);
        g.drawString((selected == backIndex ? "> " : "  ") + "[ Back ]", x, backY);

        // Show save count
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(Color.GRAY);
        g.drawString("Saves found: " + saves.size(), gp.vw - 150, gp.vh - 20);
    }
}
