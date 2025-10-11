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
            Path saves1 = Paths.get("saves");
            Files.createDirectories(saves1);
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(saves1, "*.sav")) {
                for (Path p : ds) {
                    String name = p.getFileName().toString().replaceFirst("\\.sav$", "");
                    saves.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh saves: " + e.getMessage());
        }
        saves.sort(String::compareToIgnoreCase);

        int totalOptions = saves.size() + 2;
        if (totalOptions <= 0) {
            totalOptions = 2;
        }
        if (selected >= totalOptions) {
            selected = totalOptions - 1;
        }
        if (selected < 0) {
            selected = 0;
        }
    }

    void update(double dt) {
        InputManager input = gp.input;
        int totalOptions = 2 + saves.size(); // New Game + saves + Back

        if (input.consumeIfPressed("UP")) selected = (selected - 1 + totalOptions) % totalOptions;
        if (input.consumeIfPressed("DOWN")) selected = (selected + 1) % totalOptions;
        if (input.consumeIfPressed("ENTER")) handleSelection();
        if (input.consumeIfPressed("ESC")) {
            gp.state = GamePanel.State.TITLE;
            return;
        }
        if (input.consumeIfPressed("N")) {
            selected = 0;
            handleSelection();
            return;
        }
        if (input.consumeIfPressed("L")) {
            if (!saves.isEmpty()) {
                if (selected == 0 || selected > saves.size()) {
                    selected = 1;
                }
                handleSelection();
            }
            return;
        }
        if (input.consumeIfPressed("DELETE")) {
            if (selected > 0 && selected <= saves.size()) {
                confirmDeleteSave(saves.get(selected - 1));
            }
        }
    }

    void handleSelection() {
        if (selected == 0) {
            promptNewGame();
        } else if (selected <= saves.size()) {
            String name = saves.get(selected - 1);
            if (gp.loadGame(name)) {
                gp.state = GamePanel.State.WORLD;
            }
        } else {
            gp.state = GamePanel.State.TITLE;
        }
    }

    void promptNewGame() {
        final String[] result = new String[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = JOptionPane.showInputDialog(gp,
                    "Enter save name (leave blank to skip saving):",
                    "New Game", JOptionPane.PLAIN_MESSAGE));
        } catch (Exception e) {
            result[0] = null;
        }

        String saveName = (result[0] != null) ? result[0].trim() : null;
        gp.startNewGame(saveName != null && !saveName.isEmpty() ? saveName : null);
        gp.state = GamePanel.State.WORLD;
        gp.requestFocusInWindow();
    }

    void confirmDeleteSave(String saveName) {
        final int[] result = new int[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = JOptionPane.showConfirmDialog(gp,
                    "Delete save '" + saveName + "'?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION));
        } catch (Exception ex) {
            result[0] = JOptionPane.NO_OPTION;
        }

        if (result[0] == JOptionPane.YES_OPTION) {
            if (gp.deleteSave(saveName)) refresh();
        }
    }

    void draw(Graphics2D g) {
        g.setColor(new Color(10, 10, 30));
        g.fillRect(0, 0, gp.vw, gp.vh);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("Save Menu", 24, 44);

        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Arrows: Move | Enter: Select | N: New | L: Load | Del: Delete | Esc: Back", 24, 72);

        int x = 24, y0 = 110, rowH = 24;

        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(selected == 0 ? Color.YELLOW : Color.WHITE);
        g.drawString((selected == 0 ? "> " : "  ") + "[ New Game ]", x, y0);

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
                g.setColor(Color.GRAY);
                g.drawString("  <empty>", x, y);
            }
        }

        int backIndex = saves.size() + 1;
        int backY = y0 + (maxVisible + 1) * rowH;
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(selected == backIndex ? Color.YELLOW : Color.WHITE);
        g.drawString((selected == backIndex ? "> " : "  ") + "[ Back ]", x, backY);

        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(Color.GRAY);
        g.drawString("Saves found: " + saves.size(), gp.vw - 150, gp.vh - 20);
    }
}
