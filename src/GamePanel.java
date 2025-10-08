import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GamePanel extends JPanel {
    final int vw, vh;
    BufferedImage worldBackBuffer;
    BufferedImage HUDBackBuffer;
    TileMap map;
    Camera camera;
    List<Player> party;
    Enemy demoEnemy;
    WorldObjectManager worldObjectManager;
    final List<WorldMessage> worldMessages = new ArrayList<>();
    private static final double WORLD_MESSAGE_DURATION = 3.5;
    private Interactable highlightedInteractable;
    private final QuestManager questManager = new QuestManager();
    private final FastTravelNetwork fastTravelNetwork = new FastTravelNetwork();
    private final DialogManager dialogManager = new DialogManager();
    private final List<NPC> npcs = new ArrayList<>();
    private final List<FastTravelPoint> fastTravelOptions = new ArrayList<>();
    private FastTravelPoint fastTravelOrigin;
    private boolean fastTravelMenuOpen = false;
    private int fastTravelSelectionIndex = 0;
    private final Map<String, Sprite> portraitCache = new HashMap<>();
    private final Set<String> missingAudio = new HashSet<>();
    private final AmbushManager ambushManager = new AmbushManager();
    private final SoundManager soundManager = new SoundManager();
    private String currentAmbientTrack = "ambient_overworld";
    private static final int BOSS_KEYS_REQUIRED = 3;
    private final StatsMenuController statsMenu = new StatsMenuController(this);
    private final HudRenderer hudRenderer = new HudRenderer(this, statsMenu);
    private int lastAmbientZoneId = -1;
    private int gold = 50;
    private int essence = 0;
    private int bossKeys = 0;
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
    final double LOGIC_FPS = 60;
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
        worldObjectManager = new WorldObjectManager(this);
        spawnInitialObjects();
        playAmbientTrack(currentAmbientTrack);

        addHierarchyListener(e -> {
            if (isDisplayable()) requestFocusInWindow();
        });
    }

    void createDefaultParty() {
        party = new ArrayList<>();
        gold = 0;
        essence = 0;
        fastTravelNetwork.clear();
        worldMessages.clear();
        highlightedInteractable = null;
        npcs.clear();
        fastTravelOptions.clear();
        fastTravelMenuOpen = false;
        fastTravelOrigin = null;
        statsMenu.reset();
        party.add(Player.createSample("Bluu", 3600, 3600));
        party.add(Player.createSample("Souri", 3600, 3600));
        party.add(Player.createSample("Bob", 3600, 3600));
        activeIndex = 0;
        if (worldObjectManager != null) {
            spawnInitialObjects();
        }
        if (camera != null) {
            camera.followEntity(party.get(activeIndex));
        }
        lastAmbientZoneId = -1;
        if (!party.isEmpty()) {
            updateAmbientTrack(party.get(activeIndex));
        } else {
            playAmbientTrack(currentAmbientTrack);
        }
    }

    private void spawnInitialObjects() {
        if (worldObjectManager == null) {
            return;
        }
        worldObjectManager.clear();
        fastTravelOptions.clear();
        fastTravelMenuOpen = false;
        fastTravelOrigin = null;

        worldObjectManager.add(WorldObjectFactory.createChest("starter_chest_west", 3610, 3600, 40, 1, true));
        worldObjectManager.add(WorldObjectFactory.createChest("starter_chest_central", 3620, 3600, 55, 2, true));
        worldObjectManager.add(WorldObjectFactory.createChest("starter_chest_east", 3630, 3600, 65, 3, true));
        worldObjectManager.add(WorldObjectFactory.createDoor("starter_door", 3640, 3600, false));

        FastTravelPoint village = WorldObjectFactory.createWaypoint("waypoint_village", "village", "Village Plaza", 3700, 3600, 0, 4);
        village.setUnlocked(true);
        worldObjectManager.add(village);

        FastTravelPoint ruins = WorldObjectFactory.createWaypoint("waypoint_ruins", "ruins", "Ruined Outpost", 512, 256, 25, 12);
        worldObjectManager.add(ruins);

        spawnNPCs();
        ambushManager.reset();
    }

    private void spawnNPCs() {
        npcs.clear();
        if (worldObjectManager == null) {
            return;
        }
        InteractionManager interactions = worldObjectManager.getInteractionManager();
        if (interactions == null) {
            return;
        }
        Sprite medicSprite = loadNpcSprite("medic.png", 32, 48, 6);
        MedicNPC medic = new MedicNPC("npc_medic", "Selene", medicSprite, 280, 150);
        npcs.add(medic);
        interactions.register(medic);
    }

    private Sprite loadNpcSprite(String fileName, int frameW, int frameH, int fps) {
        try {
            return SpriteLoader.loadSheet("resources/sprites/" + fileName, frameW, frameH, fps);
        } catch (IOException e) {
            BufferedImage image = new BufferedImage(frameW, frameH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(204, 220, 255));
            g.fillRect(0, 0, frameW, frameH);
            g.setColor(new Color(60, 90, 150));
            g.drawRect(0, 0, frameW - 1, frameH - 1);
            g.dispose();
            return Sprite.forStaticImage(image);
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
        boolean dialogActive = dialogManager.isActive();

        if (input.consumeIfPressed("C")) {
            if (statsMenu.isOpen()) {
                statsMenu.close();
            } else if (!fastTravelMenuOpen && !dialogActive && state == State.WORLD) {
                statsMenu.open();
            }
        }

        if (fastTravelMenuOpen) {
            return;
        }

        if (statsMenu.isOpen()) {
            statsMenu.update();
            return;
        }

        if (dialogActive) {
            return;
        }

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
        if (state == State.TITLE) {
            if (input.consumeIfPressed("ENTER")) {
                state = State.SAVE_MENU;
                saveMenu.refresh();
                return;
            }
        } else if (state == State.SAVE_MENU) {
            if (input.consumeIfPressed("ENTER")) {
                state = State.WORLD;
                return;
            }
        }
        // Quick escape behaviour
        if (state != State.BATTLE && input.consumeIfPressed("ESC")){
            switch (state) {
                case WORLD:
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
        if (state == State.WORLD && index >= 0 && index < party.size() && index != activeIndex) {
            activeIndex = index;
            Player leader = party.get(activeIndex);
            camera.followEntity(leader);
            System.out.println("Switched to " + leader.name);
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
        final double updateInterval = 1_000_000_000.0 / LOGIC_FPS;
        double accumulator = 0.0;
        long lastTime = System.nanoTime();
        long timer = 0L;
        int frameCount = 0;
        boolean needsRepaint = false;

        while (running) {
            long currentTime = System.nanoTime();
            long elapsed = currentTime - lastTime;
            lastTime = currentTime;

            accumulator += elapsed;
            timer += elapsed;

            // Prevent spiral of death
            accumulator = Math.min(accumulator, updateInterval * 3);

            // Update logic at fixed timestep
            while (accumulator >= updateInterval) {
                updateLogic(LOGIC_DT);
                accumulator -= updateInterval;
                needsRepaint = true;
            }

            // Repaint only once per frame, only if needed
            if (needsRepaint) {
                repaint();

                needsRepaint = false;
                frameCount++;
            }

            // FPS counter
            if (timer >= 1_000_000_000L) {
                if (debugMode) {
                    System.out.println("FPS: " + frameCount);
                }
                frameCount = 0;
                timer = 0;
            }

            // Prevent CPU spinning
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }



    void updateLogic(double dt) {
        updateInput();
        highlightedInteractable = null;
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
                } else {
                    updateWorldMessages(dt);
                }
                break;
            case BATTLE:
                if (!showPauseOverlay) {
                    battleScreen.update(dt);
                }
                break;
        }
    }

    void updateWorld(double dt) {
        if (party == null || party.isEmpty()) {
            updateWorldMessages(dt);
            return;
        }

        Player leader = party.get(activeIndex);
        boolean dialogActive = dialogManager.isActive();

        if (statsMenu.isOpen()) {
            updateWorldMessages(dt);
            return;
        }

        if (fastTravelMenuOpen) {
            updateFastTravelMenu();
            updateWorldMessages(dt);
            return;
        }

        if (!dialogActive) {
            leader.updateWithInput(input, map, dt);

            final double spacing = 48.0;
            Player previous = leader;
            for (int offset = 1; offset < party.size(); offset++) {
                int idx = (activeIndex + offset) % party.size();
                Player follower = party.get(idx);
                follower.follow(previous, map, dt, spacing);
                previous = follower;
            }
        } else {
            updateDialog(dt);
        }

        if (demoEnemy != null) {
            demoEnemy.update(dt);
        }

        if (activeIndex < party.size()) {
            camera.update(dt, leader);
        }

        if (worldObjectManager != null) {
            worldObjectManager.update(dt);
        }

        for (NPC npc : npcs) {
            if (npc != null) {
                npc.update(dt);
            }
        }

        if (!dialogActive && !fastTravelMenuOpen && worldObjectManager != null) {
            highlightedInteractable = worldObjectManager.findBestInteractable(leader);
        } else {
            highlightedInteractable = null;
        }

        if (!dialogActive && !fastTravelMenuOpen) {
            handleWorldInteractionInput(leader);
        }

        updateAmbientTrack(leader);

        if (!dialogActive && !fastTravelMenuOpen && !showPauseOverlay) {
            if (ambushManager.tryTrigger(leader, map, dt)) {
                queueWorldMessage("Ambushed!");
                enterBattle(true);
                return;
            }
        }

        updateWorldMessages(dt);
    }

    private void handleWorldInteractionInput(Player leader) {
        if (worldObjectManager == null || leader == null) {
            return;
        }
        if (fastTravelMenuOpen || dialogManager.isActive()) {
            return;
        }
        PlacementManager placement = worldObjectManager.getPlacementManager();
        boolean placementChanged = false;
        if (placement != null) {
            if (input.consumeIfPressed("Q")) {
                placement.previousType();
                placementChanged = true;
            }
            if (input.consumeIfPressed("R")) {
                placement.nextType();
                placementChanged = true;
            }
            if (placementChanged) {
                queueWorldMessage("Placement: " + placement.getCurrentLabel());
            }
            if (input.consumeIfPressed("SPACE")) {
                String label = placement.getCurrentLabel();
                if (label == null || label.isEmpty()) {
                    label = placement.getCurrent().name();
                }
                WorldObject placed = worldObjectManager.placeCurrent(leader, map);
                if (placed != null) {
                    queueWorldMessage(label + " placed.");
                } else {
                    queueWorldMessage("Cannot place " + label.toLowerCase(Locale.ROOT) + " here.");
                }
            }
        }
        if (input.consumeIfPressed("E")) {
            if (!worldObjectManager.tryInteract(leader)) {
                queueWorldMessage("Nothing to interact with.");
            }
        }
    }

    private void updateDialog(double dt) {
        dialogManager.setSpeedMultiplier(input.isPressed("SHIFT") ? 3.0 : 1.0);
        dialogManager.update(dt);

        if (input.consumeIfPressed("UP")) {
            dialogManager.moveSelection(-1);
            playSfx("dialog_move");
        }
        if (input.consumeIfPressed("DOWN")) {
            dialogManager.moveSelection(1);
            playSfx("dialog_move");
        }

        List<DialogChoice> choices = dialogManager.getAvailableChoices();
        if (dialogManager.isAwaitingChoice() && !choices.isEmpty()) {
            if (input.consumeIfPressed("1") && choices.size() >= 1) {
                dialogManager.selectChoice(0);
                playSfx("dialog_select");
                return;
            }
            if (input.consumeIfPressed("2") && choices.size() >= 2) {
                dialogManager.selectChoice(1);
                playSfx("dialog_select");
                return;
            }
            if (input.consumeIfPressed("3") && choices.size() >= 3) {
                dialogManager.selectChoice(2);
                playSfx("dialog_select");
                return;
            }
        }

        boolean confirm = input.consumeIfPressed("ENTER") || input.consumeIfPressed("SPACE") || input.consumeIfPressed("E");
        if (confirm) {
            if (!dialogManager.isTextComplete()) {
                dialogManager.skipTextReveal();
                playSfx("dialog_skip");
            } else if (dialogManager.isAwaitingChoice()) {
                dialogManager.selectCurrentChoice();
                playSfx("dialog_select");
            } else {
                dialogManager.advance();
                playSfx("dialog_advance");
            }
        }

        if (input.consumeIfPressed("ESC")) {
            dialogManager.clear();
            playSfx("dialog_cancel");
        }
    }

    private void updateFastTravelMenu() {
        if (!fastTravelMenuOpen) {
            return;
        }
        if (fastTravelOptions.isEmpty() || party == null || party.isEmpty()) {
            closeFastTravelMenu();
            return;
        }

        int optionCount = fastTravelOptions.size();
        if (input.consumeIfPressed("UP")) {
            fastTravelSelectionIndex = (fastTravelSelectionIndex - 1 + optionCount) % optionCount;
            playSfx("menu_move");
        }
        if (input.consumeIfPressed("DOWN")) {
            fastTravelSelectionIndex = (fastTravelSelectionIndex + 1) % optionCount;
            playSfx("menu_move");
        }

        if (input.consumeIfPressed("ESC")) {
            closeFastTravelMenu();
            playSfx("menu_cancel");
            return;
        }

        if (input.consumeIfPressed("ENTER") || input.consumeIfPressed("SPACE")) {
            FastTravelPoint destination = fastTravelOptions.get(Math.max(0, Math.min(fastTravelSelectionIndex, optionCount - 1)));
            int cost = Math.max(0, destination.getTravelCost());
            if (cost > 0 && !spendGold(cost)) {
                queueWorldMessage("Need " + cost + " gold to travel.");
                playSfx("bp_fail");
                return;
            }
            teleportPartyTo(destination);
            queueWorldMessage("Traveled to " + destination.getDisplayName() + (cost > 0 ? " for " + cost + " gold." : "."));
            playSfx("fast_travel");
            closeFastTravelMenu();
        }
    }

    private void closeFastTravelMenu() {
        fastTravelMenuOpen = false;
        fastTravelOrigin = null;
        fastTravelOptions.clear();
        fastTravelSelectionIndex = 0;
    }

    private void teleportPartyTo(FastTravelPoint destination) {
        if (destination == null || party == null || party.isEmpty() || map == null) {
            return;
        }
        Player leader = party.get(activeIndex);
        double destCenterX = destination.getX() + destination.getWidth() / 2.0;
        double destBottomY = destination.getY() + destination.getHeight();
        double targetX = destCenterX - leader.w / 2.0;
        double targetY = destBottomY - leader.h;

        targetX = Math.max(0.0, Math.min(map.pixelWidth - leader.w, targetX));
        targetY = Math.max(0.0, Math.min(map.pixelHeight - leader.h, targetY));
        leader.setPrecisePosition(targetX, targetY);

        double spacing = 36.0;
        for (int i = 1; i < party.size(); i++) {
            Player member = party.get((activeIndex + i) % party.size());
            double offsetX = Math.max(0.0, Math.min(map.pixelWidth - member.w, targetX - i * spacing));
            double offsetY = Math.max(0.0, Math.min(map.pixelHeight - member.h, targetY + i * 6));
            member.setPrecisePosition(offsetX, offsetY);
        }

        camera.setCenter(leader.getPreciseX(), leader.getPreciseY());
        camera.followEntity(leader);
        lastAmbientZoneId = -1;
        updateAmbientTrack(leader);
        ambushManager.reset();
    }

    private void updateAmbientTrack(Player leader) {
        if (leader == null || map == null) {
            return;
        }
        if (map.zoneLayer == null) {
            if (!"ambient_overworld".equals(currentAmbientTrack)) {
                currentAmbientTrack = "ambient_overworld";
                playAmbientTrack(currentAmbientTrack);
            }
            return;
        }
        int tileX = Math.max(0, Math.min(map.cols - 1, (int) (leader.getPreciseX() / map.tileW)));
        int tileY = Math.max(0, Math.min(map.rows - 1, (int) (leader.getPreciseY() / map.tileH)));
        int zoneId = map.zoneLayer[tileY][tileX];
        if (zoneId == lastAmbientZoneId) {
            return;
        }
        lastAmbientZoneId = zoneId;
        String track = switch (zoneId) {
            case 1027 -> "ambient_ruins";
            case 1028 -> "ambient_cavern";
            case 1026 -> "ambient_forest";
            default -> "ambient_overworld";
        };
        if (!track.equals(currentAmbientTrack)) {
            currentAmbientTrack = track;
            playAmbientTrack(track);
        }
    }

    private void playAmbientTrack(String trackId) {
        if (trackId == null || trackId.isEmpty()) {
            return;
        }
        if (!ensureAudioAsset(trackId)) {
            return;
        }
        soundManager.stopChannel(SoundManager.Channel.AMBIENT);
        soundManager.playAmbient(trackId);
    }

    private void playBattleTrack(String trackId) {
        if (trackId == null || trackId.isEmpty()) {
            return;
        }
        if (!ensureAudioAsset(trackId)) {
            return;
        }
        soundManager.stopChannel(SoundManager.Channel.AMBIENT);
        soundManager.stopChannel(SoundManager.Channel.BATTLE);
        soundManager.playBattle(trackId);
    }

    void playSfx(String sfxId) {
        if (sfxId == null || sfxId.isEmpty()) {
            return;
        }
        if (!ensureAudioAsset(sfxId)) {
            return;
        }
        soundManager.playSfx(sfxId);
    }

    private boolean ensureAudioAsset(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (missingAudio.contains(id)) {
            return false;
        }
        File file = new File("resources/audio/" + id + ".wav");
        if (!file.exists()) {
            missingAudio.add(id);
            return false;
        }
        return true;
    }

    private void updateWorldMessages(double dt) {
        if (worldMessages.isEmpty()) {
            return;
        }
        Iterator<WorldMessage> iterator = worldMessages.iterator();
        while (iterator.hasNext()) {
            WorldMessage message = iterator.next();
            message.update(dt);
            if (message.isExpired()) {
                iterator.remove();
            }
        }
    }
    void enterBattle() {
        if (!hasRequiredBossKeys()) {
            queueWorldMessage("The boss gate remains sealed. Keys " + bossKeys + "/" + BOSS_KEYS_REQUIRED + ".");
            playSfx("door_locked");
            return;
        }
        spendBossKeys();
        queueWorldMessage("Boss gate unlocked!");
        queueWorldMessage("Remaining keys: " + bossKeys + "/" + BOSS_KEYS_REQUIRED + ".");
        startBattle(false);
    }

    void enterBattle(boolean ambush) {
        startBattle(ambush);
    }

    private void startBattle(boolean ambush) {
        if (party != null && !party.isEmpty() && demoEnemy != null) {
            showPauseOverlay = false;
            closeFastTravelMenu();
            statsMenu.close(true);
            dialogManager.clear();
            battleScreen.startBattle(new ArrayList<>(party), demoEnemy, ambush);
            playBattleTrack(ambush ? "battle_ambush" : "battle_default");
            state = State.BATTLE;
            System.out.println(ambush ? "Ambush battle started!" : "Battle started!");
        }
    }
    void returnToWorld() {
        showPauseOverlay = false;
        closeFastTravelMenu();
        statsMenu.close(true);
        ambushManager.reset();
        state = State.WORLD;
        soundManager.stopChannel(SoundManager.Channel.BATTLE);
        if (party != null && !party.isEmpty()) {
            lastAmbientZoneId = -1;
            updateAmbientTrack(party.get(activeIndex));
        } else {
            playAmbientTrack(currentAmbientTrack);
        }
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
            statsMenu.reset();
            lastAmbientZoneId = -1;
            updateAmbientTrack(party.isEmpty() ? null : party.get(activeIndex));
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

    void queueWorldMessage(String message) {
        if (message == null) {
            return;
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!worldMessages.isEmpty()) {
            WorldMessage last = worldMessages.get(worldMessages.size() - 1);
            if (last.text().equals(trimmed)) {
                last.restart();
                return;
            }
        }
        if (worldMessages.size() >= 6) {
            worldMessages.remove(0);
        }
        worldMessages.add(new WorldMessage(trimmed, WORLD_MESSAGE_DURATION));
    }

    void startDialog(DialogTree tree, InteractionContext context) {
        if (tree == null) {
            return;
        }
        dialogManager.start(tree, context);
    }

    void addGold(int amount) {
        if (amount <= 0) {
            return;
        }
        gold += amount;
    }

    boolean spendGold(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (gold < amount) {
            return false;
        }
        gold -= amount;
        return true;
    }

    int getGold() {
        return gold;
    }

    void addEssence(int amount) {
        if (amount <= 0) {
            return;
        }
        essence += amount;
    }

    boolean spendEssence(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (essence < amount) {
            return false;
        }
        essence -= amount;
        return true;
    }

    double getCooldown() { return ambushManager.getCooldown(); }

    int getEssence() {
        return essence;
    }

    boolean addBossKey() {
        if (bossKeys >= BOSS_KEYS_REQUIRED) {
            return false;
        }
        bossKeys++;
        return true;
    }

    void spendBossKeys() {
        bossKeys = Math.max(0, bossKeys - BOSS_KEYS_REQUIRED);
    }

    boolean hasRequiredBossKeys() {
        return bossKeys >= BOSS_KEYS_REQUIRED;
    }

    int getBossKeys() {
        return bossKeys;
    }

    int getBossKeysRequired() {
        return BOSS_KEYS_REQUIRED;
    }

    double getAveragePartyLevel() {
        if (party == null || party.isEmpty()) {
            return 1.0;
        }
        double total = 0.0;
        int count = 0;
        for (Player player : party) {
            if (player == null) {
                continue;
            }
            Stats stats = player.getStats();
            if (stats == null) {
                continue;
            }
            total += Math.max(1, stats.getLevel());
            count++;
        }
        return count == 0 ? 1.0 : total / count;
    }

    int scaleChestGold(int baseGold) {
        if (baseGold <= 0) {
            return 0;
        }
        double averageLevel = getAveragePartyLevel();
        double multiplier = 1.0 + averageLevel * 0.2;
        return (int) Math.round(baseGold * multiplier);
    }

    int scaleChestEssence(int baseEssence) {
        if (baseEssence <= 0) {
            return 0;
        }
        double averageLevel = getAveragePartyLevel();
        int bonus = (int) Math.floor(Math.max(0.0, averageLevel) / 2.0);
        return baseEssence + bonus;
    }

    QuestManager getQuestManager() {
        return questManager;
    }

    void unlockFastTravel(String pointId) {
        if (pointId == null || pointId.isEmpty()) {
            return;
        }
        fastTravelNetwork.unlock(pointId);
    }

    void registerFastTravelPoint(FastTravelPoint point) {
        if (point == null) {
            return;
        }
        fastTravelNetwork.register(point);
        if (point.isUnlocked()) {
            fastTravelNetwork.unlock(point.getPointId());
        }
    }

    void openFastTravel(FastTravelPoint point) {
        if (point == null) {
            return;
        }
        statsMenu.close(true);
        registerFastTravelPoint(point);
        if (!fastTravelNetwork.isUnlocked(point.getPointId())) {
            fastTravelNetwork.unlock(point.getPointId());
        }
        fastTravelOptions.clear();
        for (FastTravelPoint candidate : fastTravelNetwork.getUnlockedPoints()) {
            if (candidate != null && !candidate.getPointId().equals(point.getPointId())) {
                fastTravelOptions.add(candidate);
            }
        }
        fastTravelOptions.sort(Comparator.comparing(FastTravelPoint::getDisplayName));
        if (fastTravelOptions.isEmpty()) {
            queueWorldMessage("No other attuned waypoints yet.");
            return;
        }
        fastTravelMenuOpen = true;
        fastTravelOrigin = point;
        fastTravelSelectionIndex = 0;
        playSfx("menu_open");
    }
    void healPartyPercentage(double fraction) {
        if (party == null || party.isEmpty()) {
            return;
        }
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        for (Player player : party) {
            if (player == null) {
                continue;
            }
            Stats stats = player.getStats();
        if (stats == null) {
            return;
        }
            int maxHp = stats.getMaxHp();
            if (maxHp <= 0) {
                continue;
            }
            int healAmount = (int) Math.max(1, Math.round(maxHp * clamped));
            stats.heal(healAmount);
            player.refreshDerivedStats();
        }
    }

    void healPartyFull() {
        if (party == null) {
            return;
        }
        for (Player player : party) {
            if (player != null) {
                player.getStats().fullHeal();
                player.refreshDerivedStats();
            }
        }
    }

    void markQuestTarget(String questId) {
        if (questId == null || questId.isEmpty()) {
            return;
        }
        questManager.setQuestStatus(questId, QuestStatus.ACTIVE);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        // --- World back buffer ---
        Graphics2D gWorld = worldBackBuffer.createGraphics();
        gWorld.setColor(new Color(0x2b2b2b));
        gWorld.fillRect(0, 0, worldBackBuffer.getWidth(), worldBackBuffer.getHeight());

        // --- HUD / UI back buffer ---
        Graphics2D gUI = HUDBackBuffer.createGraphics();


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

//        gWorld.dispose();
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
        g2.drawImage(HUDBackBuffer, ox, oy, drawW, drawH, null);

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
        g.setFont(FontCustom.PressStart2P.deriveFont(Font.PLAIN, 14));

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
            double camRenderX = camera.getRenderX();
            double camRenderY = camera.getRenderY();
            g.translate(vw / 2.0, vh / 2.0);
            g.scale(zoomLevel, zoomLevel);
            g.translate(-camRenderX, -camRenderY);

            if (map != null) {
                map.drawGround(g, camera);
            }

            List<DepthRenderable> renderQueue = new ArrayList<>();

            if (worldObjectManager != null) {
                for (WorldObject object : worldObjectManager.getWorldObjects()) {
                    if (object == null) {
                        continue;
                    }
                    WorldObject drawObject = object;
                    renderQueue.add(new DepthRenderable(drawObject.getY() + drawObject.getHeight(),
                            graphics -> drawObject.draw(graphics)));
                }
            }

            if (!npcs.isEmpty()) {
                for (NPC npc : npcs) {
                    if (npc == null) {
                        continue;
                    }
                    NPC drawNpc = npc;
                    renderQueue.add(new DepthRenderable(drawNpc.getPreciseY() + drawNpc.h,
                            graphics -> drawNpc.draw(graphics, camera)));
                }
            }

            if (party != null) {
                for (Player player : party) {
                    if (player == null) {
                        continue;
                    }
                    Player drawPlayer = player;
                    renderQueue.add(new DepthRenderable(drawPlayer.getPreciseY() + drawPlayer.h,
                            graphics -> drawPlayer.draw(graphics, camera)));
                }
            }

            if (demoEnemy != null) {
                Enemy enemy = demoEnemy;
                renderQueue.add(new DepthRenderable(enemy.getPreciseY() + enemy.h,
                        graphics -> enemy.draw(graphics, camera)));
            }

            renderQueue.sort(Comparator.comparingDouble(entry -> entry.depth));

            for (DepthRenderable entry : renderQueue) {
                entry.drawCommand.accept(g);
            }

            if (map != null) {
                map.drawDecorations(g, camera);
                if (debugMode) {
                    map.drawCollisionOverlay(g, camera);
                    map.drawZoneOverlay(g, camera);
                }
            }
        } finally {
            g.setTransform(oldTransform);
        }

    }

    void   drawHUD(Graphics2D g) {
        hudRenderer.draw(g, highlightedInteractable, dialogManager.isActive(), fastTravelMenuOpen);
        if (fastTravelMenuOpen) {
            drawFastTravelMenu(g);
        }
        if (dialogManager.isActive()) {
            drawDialog(g);
        }
    }
    private void drawFastTravelMenu(Graphics2D g) {
        int optionCount = fastTravelOptions.size();
        int visibleCount = Math.min(6, Math.max(1, optionCount));
        int width = Math.min(vw - 120, 380);
        int height = Math.min(vh - 120, 120 + visibleCount * 26);
        int x = (vw - width) / 2;
        int y = (vh - height) / 2;

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(x, y, width, height, 16, 16);
        g.setColor(new Color(120, 180, 255));
        g.drawRoundRect(x, y, width, height, 16, 16);

        Font font = FontCustom.PressStart2P.deriveFont(Font.PLAIN, 16);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textX = x + 18;
        int textY = y + fm.getAscent() + 18;

        g.setColor(Color.WHITE);
        g.drawString("Fast Travel", textX, textY);
        textY += fm.getHeight() + 6;

        String origin = fastTravelOrigin != null ? fastTravelOrigin.getDisplayName() : "Unknown";
        g.setColor(new Color(200, 220, 255));
        g.drawString("From: " + origin, textX, textY);
        textY += fm.getHeight() + 8;

        if (fastTravelOptions.isEmpty()) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("No destinations attuned.", textX, textY);
        } else {
            int startIndex = 0;
            if (optionCount > visibleCount) {
                startIndex = Math.max(0, fastTravelSelectionIndex - visibleCount / 2);
                startIndex = Math.min(startIndex, optionCount - visibleCount);
            }

            if (startIndex > 0) {
                g.setColor(new Color(180, 180, 180));
                g.drawString("▲ more", textX, textY - 4);
            }

            for (int i = 0; i < visibleCount; i++) {
                int index = startIndex + i;
                FastTravelPoint option = fastTravelOptions.get(index);
                String label = option.getDisplayName();
                int cost = Math.max(0, option.getTravelCost());
                String costText = cost > 0 ? cost + "G" : "Free";
                boolean selected = index == fastTravelSelectionIndex;

                int itemY = textY + i * (fm.getHeight() + 6);
                if (selected) {
                    g.setColor(new Color(60, 110, 200, 180));
                    g.fillRoundRect(x + 12, itemY - fm.getAscent() - 4, width - 24, fm.getHeight() + 8, 10, 10);
                }

                g.setColor(selected ? Color.WHITE : new Color(215, 215, 215));
                g.drawString(label, textX, itemY);

                Color costColor = (cost > gold) ? new Color(255, 140, 140) : new Color(200, 230, 255);
                g.setColor(selected ? costColor : costColor.darker());
                int costX = x + width - 20 - fm.stringWidth(costText);
                g.drawString(costText, costX, itemY);
            }

            if (startIndex + visibleCount < optionCount) {
                int indicatorY = textY + visibleCount * (fm.getHeight() + 6);
                g.setColor(new Color(180, 180, 180));
                g.drawString("▼ more", textX, indicatorY);
            }
        }

        g.setColor(new Color(180, 180, 180));
        g.drawString("Enter: Travel   ESC: Cancel", textX, y + height - fm.getDescent() - 14);
    }

    private void drawDialog(Graphics2D g) {
        if (!dialogManager.isActive()) {
            return;
        }

        int boxWidth = vw - 80;
        int boxHeight = Math.min(vh / 3 + 40, 240);
        int x = 40;
        int y = vh - boxHeight - 28;

        g.setColor(new Color(0, 0, 0, 210));
        g.fillRoundRect(x, y, boxWidth, boxHeight, 18, 18);
        g.setColor(new Color(120, 180, 255));
        g.drawRoundRect(x, y, boxWidth, boxHeight, 18, 18);

        Font textFont = FontCustom.PressStart2P.deriveFont(Font.PLAIN, 16);
        g.setFont(textFont);
        FontMetrics fm = g.getFontMetrics();

        int contentX = x + 20;
        int contentY = y + fm.getAscent() + 18;

        String speaker = dialogManager.getSpeaker();
        if (speaker != null && !speaker.isEmpty()) {
            g.setColor(new Color(200, 220, 255));
            g.drawString(speaker, contentX, contentY);
            contentY += fm.getHeight() + 6;
        }

        Sprite portrait = getPortraitSprite(dialogManager.getPortraitId());
        int portraitSize = 84;
        int textAreaX = contentX;
        int textAreaWidth = boxWidth - 40;
        if (portrait != null) {
            portrait.draw(g, contentX, contentY, portraitSize, portraitSize);
            textAreaX += portraitSize + 14;
            textAreaWidth -= portraitSize + 14;
        }
        textAreaWidth = Math.max(120, textAreaWidth);

        g.setColor(Color.WHITE);
        List<String> lines = wrapText(dialogManager.getVisibleText(), fm, textAreaWidth);
        int lineY = contentY + fm.getAscent();
        for (String line : lines) {
            g.drawString(line, textAreaX, lineY);
            lineY += fm.getHeight() + 2;
        }

        List<DialogChoice> choices = dialogManager.getAvailableChoices();
        if (dialogManager.isAwaitingChoice() && !choices.isEmpty()) {
            lineY += 6;
            for (int i = 0; i < choices.size(); i++) {
                DialogChoice choice = choices.get(i);
                boolean selected = i == dialogManager.getSelectedChoiceIndex();
                if (selected) {
                    g.setColor(new Color(60, 110, 200, 160));
                    g.fillRoundRect(textAreaX - 6, lineY - fm.getAscent() - 4, textAreaWidth + 12, fm.getHeight() + 8, 10, 10);
                }
                g.setColor(selected ? Color.WHITE : new Color(215, 215, 215));
                String choiceText = choice.text != null ? choice.text : "...";
                String optionLabel = (i + 1) + ". " + choiceText;
                g.drawString(optionLabel, textAreaX, lineY);
                lineY += fm.getHeight() + 4;
            }
        } else if (!dialogManager.isTextComplete()) {
            g.setColor(new Color(200, 200, 200));
            g.drawString("...", textAreaX, lineY + fm.getAscent());
        }

        g.setColor(new Color(170, 170, 170));
        String hint = "Space/Enter: Next   Shift: Speed   Esc: Skip";
        g.drawString(hint, contentX, y + boxHeight - fm.getDescent() - 14);
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return lines;
        }
        for (String paragraph : text.split("\n")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (String word : trimmed.split("\s+")) {
                if (builder.length() == 0) {
                    builder.append(word);
                    continue;
                }
                String candidate = builder + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth) {
                    builder.append(' ').append(word);
                } else {
                    lines.add(builder.toString());
                    builder.setLength(0);
                    builder.append(word);
                }
            }
            if (builder.length() > 0) {
                lines.add(builder.toString());
            }
        }
        return lines;
    }

    private Sprite getPortraitSprite(String portraitId) {
        if (portraitId == null || portraitId.isEmpty()) {
            return null;
        }
        Sprite cached = portraitCache.get(portraitId);
        if (cached != null) {
            return cached;
        }
        String[] candidates = new String[] {
                "resources/sprites/portrait_" + portraitId + ".png",
                "resources/sprites/" + portraitId + "_portrait.png",
                "resources/sprites/" + portraitId + ".png"
        };
        for (String path : candidates) {
            File file = new File(path);
            if (!file.exists()) {
                continue;
            }
            try {
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    Sprite sprite = Sprite.forStaticImage(image);
                    portraitCache.put(portraitId, sprite);
                    return sprite;
                }
            } catch (IOException ignored) {
            }
        }
        BufferedImage placeholder = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(new Color(90, 120, 200));
        g.fillRect(0, 0, placeholder.getWidth(), placeholder.getHeight());
        g.setColor(Color.DARK_GRAY);
        g.drawRect(0, 0, placeholder.getWidth() - 1, placeholder.getHeight() - 1);
        g.dispose();
        Sprite sprite = Sprite.forStaticImage(placeholder);
        portraitCache.put(portraitId, sprite);
        return sprite;
    }

    private static final class DepthRenderable {
        final double depth;
        final Consumer<Graphics2D> drawCommand;

        DepthRenderable(double depth, Consumer<Graphics2D> drawCommand) {
            this.depth = depth;
            this.drawCommand = drawCommand;
        }
    }
}











