import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GamePanel extends JPanel {
    final int vw, vh;
    BufferedImage worldBackBuffer;
    BufferedImage HUDBackBuffer;
    TileMap map;
    Camera camera;
    List<Player> party;
    Enemy demoEnemy;
    int activeIndex = 0;
    boolean debugMode = false;
    boolean showPauseOverlay = false;
    enum State { TITLE, SAVE_MENU, WORLD, BATTLE }
    State state = State.TITLE;

    TitleScreen titleScreen;
    SaveMenuScreen saveMenu;
    BattleScreen battleScreen;

    InputManager input;
    Thread gameThread;
    volatile boolean running = false;
    final double LOGIC_FPS = 120;
    final double LOGIC_DT = 1.0/LOGIC_FPS;

    public GamePanel(int virtualWidth, int virtualHeight) {
        this.vw = virtualWidth;
        this.vh = virtualHeight;
        setBackground(Color.RED);
        setFocusable(true);
        FontCustom.loadFonts();
        worldBackBuffer = new BufferedImage(vw, vh, BufferedImage.TYPE_INT_ARGB);
        HUDBackBuffer = new BufferedImage(vw, vh, BufferedImage.TYPE_INT_ARGB);
        input = new InputManager(this);

        // Load tilemap with better error handling
        try {
            map = TileMap.loadFromTMX("resources/tiles/map.tmx");
            System.out.println("TMX map loaded successfully");
        } catch (Exception e) {
            System.out.println("TMX load failed (using placeholder): " + e.getMessage());
        }

        camera = new Camera(vw, vh, map);
        createDefaultParty();
        demoEnemy = Enemy.createSample("Slime", 400, 200);

        titleScreen = new TitleScreen(this);
        saveMenu = new SaveMenuScreen(this);
        battleScreen = new BattleScreen(this);

        addHierarchyListener(e -> {
            if (isDisplayable()) requestFocusInWindow();
        });
    }

    void createDefaultParty() {
        party = new ArrayList<>();
        party.add(Player.createSample("Bluu", 64, 64));
        party.add(Player.createSample("Souri", 96, 64));
        party.add(Player.createSample("Ferryman", 128, 64));
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

    void updateInput() {
        //debug Mode
        if (input.consumeIfPressed("P")){
            debugMode = !debugMode;
        }
        // Character switching
        if (input.consumeIfPressed("1")){
            switchToCharacter(0);
        }
        if (input.consumeIfPressed("2")){
            switchToCharacter(1);
        }
        if (input.consumeIfPressed("3")){
            switchToCharacter(2);
        }
        // Battle trigger
        if(input.consumeIfPressed("B")){
            if (state == State.WORLD) enterBattle();
        }
        // Main menu navigation
        if(input.consumeIfPressed("ENTER")){
            if (state == State.TITLE) {
                state = State.SAVE_MENU;
                saveMenu.refresh();
                return;
            }
            if (state == State.SAVE_MENU) {
                state = State.WORLD;
                return;
            }
        }
        // Quick escape to title
        if(input.consumeIfPressed("ESC")){
            switch (state) {
                case WORLD:
                case BATTLE:
                    showPauseOverlay = !showPauseOverlay;
                    System.out.println(showPauseOverlay);
                    break;
                case SAVE_MENU:
                    state = State.TITLE;
                    break;
                case TITLE:
                    int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit the game?", "Exit Game", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                    break;
            }
        }

        // NEW ZOOM CONTROLS
        if(input.consumeIfPressed("EQUALS")){
            if (state == State.WORLD && camera != null) {
                camera.zoomIn();
                System.out.println("Zoom: " + String.format("%.2f", camera.getZoom()));
            }
        }


        if(input.consumeIfPressed("MINUS")){
            if (state == State.WORLD && camera != null) {
                camera.zoomOut();
                System.out.println("Zoom: " + String.format("%.2f", camera.getZoom()));
            }
        }

        if(input.consumeIfPressed("0")){
            if (state == State.WORLD && camera != null) {
                camera.resetZoom();
                System.out.println("Zoom: " + String.format("%.2f", camera.getZoom()));
            }
        }
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
                Thread.sleep(1); // ~120 FPS cap
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    void updateLogic(double dt) {
        updateInput();
        switch (state) {
            case TITLE:
                titleScreen.update(dt);
                break;
            case SAVE_MENU:
                saveMenu.update(dt);
                break;
            case WORLD:
                if (!showPauseOverlay) {
                    updateWorld(dt);
                }
                break;
            case BATTLE:
                if (showPauseOverlay) {
                    battleScreen.update(dt);;
                }
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

        // --- World back buffer ---
        Graphics2D gWorld = worldBackBuffer.createGraphics();
        gWorld.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        gWorld.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gWorld.setColor(new Color(0x2b2b2b));
        gWorld.fillRect(0, 0, worldBackBuffer.getWidth(), worldBackBuffer.getHeight());

        // --- HUD / UI back buffer ---
        Graphics2D gUI = HUDBackBuffer.createGraphics();
        gUI.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        gUI.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // --- เคลียร์ HUDBackBuffer ทุกครั้งก่อนวาดใหม่ ---
        gUI.setComposite(AlphaComposite.Clear);
        gUI.fillRect(0, 0, HUDBackBuffer.getWidth(), HUDBackBuffer.getHeight());
        gUI.setComposite(AlphaComposite.SrcOver);

        try {
            switch (state) {
                case TITLE:
                    titleScreen.draw(gUI);
                    break;
                case SAVE_MENU:
                    saveMenu.draw(gUI);
                    break;
                case WORLD:
                    drawWorld(gWorld);   // world ลง backBuffer
                    drawHUD(gUI);        // HUD overlay ลง HUDBackBuffer
                    break;
                case BATTLE:
                    battleScreen.draw(gUI);
                    break;
            }
        } catch (Exception e) {
            Graphics2D gError = (state == State.WORLD) ? gWorld : gUI;
            gError.setColor(Color.RED);
            gError.drawString("Render Error: " + e.getMessage(), 10, 30);
        }

        gWorld.dispose();
        gUI.dispose();

        // --- Compose final screen ---
        Graphics2D g2 = (Graphics2D) g0;
        int pw = getWidth(), ph = getHeight();
        double sx = (double) pw / vw, sy = (double) ph / vh;
        double scale = Math.min(sx, sy);
        int drawW = (int) (vw * scale), drawH = (int) (vh * scale);
        int ox = (pw - drawW) / 2, oy = (ph - drawH) / 2;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, pw, ph);

        // Draw WORLD if applicable
        if (state == State.WORLD) {
            g2.drawImage(worldBackBuffer, ox, oy, drawW, drawH, null);
        }

        // Draw UI overlay (HUD for WORLD, UI for TITLE/SAVE/BATTLE)
        g2.drawImage(HUDBackBuffer, 0, 0, pw, ph, null);

        // Letterbox
        g2.setColor(new Color(0, 0, 0, 120));
        if (ox > 0) {
            g2.fillRect(0, 0, ox, ph);
            g2.fillRect(pw - ox, 0, ox, ph);
        } else if (oy > 0) {
            g2.fillRect(0, 0, pw, oy);
            g2.fillRect(0, ph - oy, pw, oy);
        }
        if (showPauseOverlay && (state == State.WORLD || state == State.BATTLE)) {
            drawPauseMenu(g2); // new method
        }
    }
    void drawPauseMenu(Graphics2D g) {
        int menuWidth = 220, menuHeight = 140;
        int x = (getWidth() - menuWidth) / 2;
        int y = (getHeight() - menuHeight) / 2;

        // Dim background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Menu box
        g.setColor(new Color(40, 40, 40));
        g.fillRoundRect(x, y, menuWidth, menuHeight, 15, 15);

        g.setColor(Color.WHITE);
        g.setFont(FontCustom.PressStart2P.deriveFont(10f));

        String[] options = { "Resume", "Save", "Main Menu", "Quit" };
        for (int i = 0; i < options.length; i++) {
            g.drawString(options[i], x + 30, y + 35 + i * 25);
        }
    }





    void drawWorld(Graphics2D g) {
        if (camera == null) return;

        AffineTransform oldTransform = g.getTransform();
        try {
            double zoomLevel = camera.getZoom();
            g.translate(vw / 2.0, vh / 2.0);
            g.scale(zoomLevel, zoomLevel);
            g.translate(-camera.getX(), -camera.getY());

            if (map != null) {
                map.draw(g, camera);
                if (debugMode) {
                    map.drawCollisionOverlay(g, camera);
                    map.drawZoneOverlay(g, camera);
                }
            }

            List<Entity> allEntities = new ArrayList<>();
            if (party != null) {
                allEntities.addAll(party);
            }
            if (demoEnemy != null) {
                allEntities.add(demoEnemy);
            }

            allEntities.sort(Comparator.comparingDouble(e -> e.y + e.h));

            for (Entity e : allEntities) {
                if (e != null) {
                    e.draw(g, camera);
                }
            }
        } finally {
            g.setTransform(oldTransform);
        }
    }



    void drawHUD(Graphics2D g) {
        if (party == null || party.isEmpty()) return;

        // HUD background
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(25, 285, 155, 65, 8, 8);

        // HUD content
        g.setColor(Color.WHITE);
        g.setFont(FontCustom.PressStart2P.deriveFont(7f));

        for (int i = 0; i < party.size(); i++) {
            Player p = party.get(i);
            int y0 = 300 + i * 20;
            String prefix = (i == activeIndex) ? "> " : "  ";
            String info = String.format("%s%s LV%d HP:%d/%d",
                    prefix, p.name, p.level, p.hp, p.maxHp);
            g.drawString(info, 16, y0);
        }

        // Show zoom level
        g.setColor(Color.CYAN);
        g.drawString(String.format("Zoom: %.2fx", camera.getZoom()), 16, 16);

//        g.setColor(Color.LIGHT_GRAY);
//        g.setFont(FontCustom.PressStart2P.deriveFont(8f));
//        g.drawString("Controls: 1/2/3=Switch | B=Battle | +/-=Zoom | 0=Reset", 16, 120);
    }
}
