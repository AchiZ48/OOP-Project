import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GamePanel extends JPanel {
    final int vw, vh;
    BufferedImage backBuffer;
    TileMap map;
    Camera camera;
    List<Player> party;
    Enemy demoEnemy;
    int activeIndex = 0;

    enum State { TITLE, SAVE_MENU, WORLD, BATTLE }
    State state = State.TITLE;

    TitleScreen titleScreen;
    SaveMenuScreen saveMenu;
    BattleScreen battleScreen;

    InputManager input;
    Thread gameThread;
    volatile boolean running = false;
    final double LOGIC_FPS = 60.0;
    final double LOGIC_DT = 1.0/LOGIC_FPS;

    public GamePanel(int virtualWidth, int virtualHeight) {
        this.vw = virtualWidth;
        this.vh = virtualHeight;
        setBackground(Color.BLACK);
        setFocusable(true);
        FontCustom.loadFonts();
        backBuffer = new BufferedImage(vw, vh, BufferedImage.TYPE_INT_ARGB);
        input = new InputManager(this);

        // Load tilemap with better error handling
        try {
            map = TileMap.loadFromTMX("resources/tiles/map.tmx");
            System.out.println("TMX map loaded successfully");
        } catch (Exception e) {
            System.out.println("TMX load failed (using placeholder): " + e.getMessage());
            map = TileMap.generatePlaceholder(50, 30, 16, 16);
        }

        camera = new Camera(vw, vh, map);
        createDefaultParty();
        demoEnemy = Enemy.createSample("Slime", 400, 200);

        titleScreen = new TitleScreen(this);
        saveMenu = new SaveMenuScreen(this);
        battleScreen = new BattleScreen(this);

        bindDefaultKeys();
        addHierarchyListener(e -> {
            if (isDisplayable()) requestFocusInWindow();
        });
    }

    void createDefaultParty() {
        party = new ArrayList<>();
        party.add(Player.createSample("Alice", 64, 64));
        party.add(Player.createSample("Bob", 96, 64));
        party.add(Player.createSample("Cleo", 128, 64));
        activeIndex = 0;
        if (camera != null) {
            camera.followEntity(party.get(activeIndex));
        }
    }

    public void startNewGame(String saveName) {
        createDefaultParty();
        state = State.WORLD;
        camera.followEntity(party.get(activeIndex));
        if (saveName != null && !saveName.trim().isEmpty()) {
            saveGame(saveName.trim());
        }
        System.out.println("New game started" + (saveName != null ? " and saved as: " + saveName : ""));
    }

    void bindDefaultKeys() {
        // Character switching
        input.bindKey("1", () -> switchToCharacter(0));
        input.bindKey("2", () -> switchToCharacter(1));
        input.bindKey("3", () -> switchToCharacter(2));

        // Battle trigger
        input.bindKey("B", () -> {
            if (state == State.WORLD) enterBattle();
        });

        // Main menu navigation
        input.bindKey("ENTER", () -> {
            if (state == State.TITLE) {
                state = State.SAVE_MENU;
                saveMenu.refresh();
            }
        });

        // Quick escape to title
        input.bindKey("ESC", () -> {
            if (state != State.TITLE) {
                state = State.TITLE;
            }
        });
    }

    void switchToCharacter(int index) {
        if (state == State.WORLD && index >= 0 && index < party.size()) {
            activeIndex = index;
            camera.followEntity(party.get(activeIndex));
            System.out.println("Switched to " + party.get(activeIndex).name);
        }
    }

    public void onWindowResize() {
        repaint();
    }

    void start() {
        if (!running) {
            running = true;
            gameThread = new Thread(this::gameLoop, "GameLoop");
            gameThread.start();
        }
    }

    void stop() {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void gameLoop() {
        double accumulator = 0.0;
        long lastTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            double frameTime = (now - lastTime) / 1e9;
            lastTime = now;

            // Cap frame time to prevent spiral of death
            if (frameTime > 0.25) frameTime = 0.25;
            accumulator += frameTime;

            input.update();

            // Fixed timestep logic updates
            while (accumulator >= LOGIC_DT) {
                updateLogic(LOGIC_DT);
                accumulator -= LOGIC_DT;
            }

            // Render
            repaint();

            try {
                Thread.sleep(8); // ~120 FPS cap
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    void updateLogic(double dt) {
        switch (state) {
            case TITLE:
                titleScreen.update(dt);
                break;
            case SAVE_MENU:
                saveMenu.update(dt);
                break;
            case WORLD:
                updateWorld(dt);
                break;
            case BATTLE:
                battleScreen.update(dt);
                break;
        }
    }

    void updateWorld(double dt) {
        for (int i = 0; i < party.size(); i++) {
            Player p = party.get(i);
            if (i == activeIndex) {
                p.updateWithInput(input, map, dt);
            } else {
                p.idleUpdate(dt);
            }
        }

        // Update enemy
        if (demoEnemy != null) {
            demoEnemy.update(dt);
        }

        // Update camera
        if (activeIndex < party.size()) {
            camera.update(dt, party.get(activeIndex));
        }
    }

    void enterBattle() {
        if (party != null && !party.isEmpty() && demoEnemy != null) {
            battleScreen.startBattle(new ArrayList<>(party), demoEnemy);
            state = State.BATTLE;
            System.out.println("Battle started!");
        }
    }

    void returnToWorld() {
        state = State.WORLD;
        System.out.println("Returned to world");
    }

    void saveGame(String name) {
        try {
            Files.createDirectories(Paths.get("saves"));
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream("saves/" + name + ".sav"))) {
                SaveData sd = SaveData.fromParty(party);
                oos.writeObject(sd);
                System.out.println("Game saved: saves/" + name + ".sav");
            }
        } catch (Exception e) {
            System.err.println("Save failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    boolean loadGame(String name) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("saves/" + name + ".sav"))) {
            SaveData sd = (SaveData) ois.readObject();
            party = sd.toParty();
            activeIndex = 0;
            if (!party.isEmpty()) {
                camera.followEntity(party.get(activeIndex));
            }
            state = State.WORLD;
            System.out.println("Game loaded: " + name);
            return true;
        } catch (Exception e) {
            System.err.println("Load failed for " + name + ": " + e.getMessage());
            return false;
        }
    }

    boolean deleteSave(String name) {
        try {
            boolean deleted = Files.deleteIfExists(Paths.get("saves/" + name + ".sav"));
            if (deleted) {
                System.out.println("Deleted save: " + name);
            }
            return deleted;
        } catch (IOException e) {
            System.err.println("Delete failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);

        // Clear back buffer
        Graphics2D gbb = backBuffer.createGraphics();
        gbb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        gbb.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gbb.setColor(new Color(0x2b2b2b));
        gbb.fillRect(0, 0, vw, vh);

        // Render current state
        try {
            switch (state) {
                case TITLE:
                    titleScreen.draw(gbb);
                    break;
                case SAVE_MENU:
                    saveMenu.draw(gbb);
                    break;
                case WORLD:
                    drawWorld(gbb);
                    break;
                case BATTLE:
                    battleScreen.draw(gbb);
                    break;
            }
        } catch (Exception e) {
            // Fallback rendering
            gbb.setColor(Color.RED);
            gbb.drawString("Render Error: " + e.getMessage(), 10, 30);
        }

        gbb.dispose();

        // Scale and draw to screen
        Graphics2D g2 = (Graphics2D) g0;
        int pw = getWidth(), ph = getHeight();
        double sx = (double) pw / vw, sy = (double) ph / vh;
        double scale = Math.min(sx, sy);
        int drawW = (int) (vw * scale), drawH = (int) (vh * scale);
        int ox = (pw - drawW) / 2, oy = (ph - drawH) / 2;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, pw, ph);
        g2.drawImage(backBuffer, ox, oy, drawW, drawH, null);

        // Draw letterboxing
        g2.setColor(new Color(0, 0, 0, 120));
        if (ox > 0) {
            g2.fillRect(0, 0, ox, ph);
            g2.fillRect(pw - ox, 0, ox, ph);
        } else if (oy > 0) {
            g2.fillRect(0, 0, pw, oy);
            g2.fillRect(0, ph - oy, pw, oy);
        }
    }

    void drawWorld(Graphics2D g) {
        // Draw tilemap
        if (map != null) {
            map.draw(g, camera);
        }

        // Collect and sort entities by Y position
        List<Entity> allEntities = new ArrayList<>();
        if (party != null) {
            allEntities.addAll(party);
        }
        if (demoEnemy != null) {
            allEntities.add(demoEnemy);
        }

        allEntities.sort(Comparator.comparingInt(e -> e.y + e.h));

        // Draw entities
        for (Entity e : allEntities) {
            if (e != null) {
                e.draw(g, camera);
            }
        }

        // Draw HUD
        drawHUD(g);
    }



    void drawHUD(Graphics2D g) {
        if (party == null || party.isEmpty()) return;



        // HUD background
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(8, 8, 300, 110, 8, 8);

        // HUD content
        g.setColor(Color.WHITE);
        g.setFont(FontCustom.PressStart2P);

        for (int i = 0; i < party.size(); i++) {
            Player p = party.get(i);
            int y0 = 24 + i * 22;
            String prefix = (i == activeIndex) ? "> " : "  ";
            String info = String.format("%s%s LV%d HP:%d/%d",
                    prefix, p.name, p.level, p.hp, p.maxHp);
            g.drawString(info, 16, y0);
        }

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(FontCustom.PressStart2P.deriveFont(8f));
        g.drawString("Controls: 1/2/3=Switch | B=Battle | Enter=Menu", 16, 100);
    }
}
