import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class BattleScreen {
    private static final int SKILL_VISIBLE_MAX = 5;
    private static final String[] DEFEAT_OPTIONS = {"Load Last Save", "Main Menu"};
    private static final String[] VICTORY_OPTIONS = {"Load Last Save", "Main Menu"};
    private static final double ENEMY_ACTION_WINDUP = 0.45;
    private static final double ENEMY_ACTION_INTERVAL = 0.85;
    final Map<String, Sprite> backSpriteCache = new HashMap<>();
    private final Deque<EnemyAction> enemyActionQueue = new ArrayDeque<>();
    private GamePanel gp;
    private Sprite backgroundSprite;
    private Sprite nameBannerSprite;
    private List<Player> party;
    private List<Enemy> enemy;
    private int currentPlayerIndex = 0;
    private int currentEnemyIndex = 0;
    private List<SkillCatalog.SkillDefinition> skills;
    private int selectedSkill = 0;
    private boolean waitingForInput = true;
    private String lastAction = "";
    private String[] mainMenu = {"Attack", "Meditate", "Flee"};
    private int mainMenuIndex = 0;
    boolean inSkillMenu = false;
    private int preparedTurnIndex = -1;
    private int skillScroll = 0;
    private boolean defeatScreenActive = false;
    private int defeatMenuIndex = 0;
    private boolean victoryScreenActive = false;
    private int victoryMenuIndex = 0;
    private boolean bossBattleActive = false;
    private boolean bossVictoryEndTriggered = false;
    private boolean enemyTurnActive = false;
    private double enemyActionTimer = 0.0;
    private boolean victoryProcessed = false;

    public BattleScreen(GamePanel gp) {
        this.gp = gp;
        skills = new ArrayList<>(SkillCatalog.all());
    }

    void setBossBattle(boolean bossBattleActive) {
        this.bossBattleActive = bossBattleActive;
        if (!bossBattleActive) {
            bossVictoryEndTriggered = false;
        }
    }

    private Sprite loadSprite(String path, int frameW, int framH) {
        try {
            return SpriteLoader.loadSheet(path, frameW, framH);
        } catch (IOException e) {
            System.err.println("Failed to load sprite at " + path + ": " + e.getMessage());
            return null;
        }
    }

    void startBattle(List<Player> party, List<Enemy> enemy, boolean ambush) {
        Player leader = gp.party.get(gp.activeIndex);
        String path;
        switch (gp.map.getZone((int) (leader.x / 32), (int) (leader.y / 32))) {
            case 1:
                path = "bg2";
                break;
            case 2:
                path = "forest";
                break;
            case 4:
                path = "snow";
                break;
            default:
                path = "bg1";
                break;
        }
        if (Objects.equals(enemy.get(0).name, "Boss")) {
            path = "boss";
        }
        try {
            backgroundSprite = SpriteLoader.loadSheet("resources/battlebg/" + path + ".png", 640, 360);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.party = new ArrayList<>(party);
        this.enemy = new ArrayList<>(enemy);
        this.currentPlayerIndex = 0;
        this.currentEnemyIndex = 0;
        this.waitingForInput = true;
        this.selectedSkill = 0;
        this.lastAction = "Battle started vs " + joinEnemyNames() + "!";
        this.preparedTurnIndex = -1;
        this.selectedSkill = 0;
        this.mainMenuIndex = 0;
        this.inSkillMenu = false;
        this.victoryProcessed = false;
        this.defeatScreenActive = false;
        this.defeatMenuIndex = 0;
        this.victoryScreenActive = false;
        this.victoryMenuIndex = 0;
        this.skillScroll = 0;
        this.bossVictoryEndTriggered = false;

        for (Player member : this.party) {
            if (member == null) continue;
            member.initializeDefaultSkills();
            Stats stats = member.getStats();
            stats.clearTemporaryModifiers();
            stats.setCurrentBattlePoints(1);
        }

        for (Enemy e : this.enemy) {
            if (e != null) e.stats.setCurrentHp(e.stats.getMaxHp());
        }

        refocusEnemyIndex();

        if (ambush) {
            this.lastAction = "Ambushed! Enemies strike first!";
            performEnemyTurn();
        }

        System.out.println("Battle started vs " + joinEnemyNames());
    }

    private String joinEnemyNames() {
        if (enemy == null || enemy.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < enemy.size(); i++) {
            Enemy e = enemy.get(i);
            if (e == null) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.name);
        }
        return sb.toString();
    }

    void update(double dt) {
        InputManager input = gp.input;
        if (input.consumeIfPressed("ESC")) {
            gp.returnToWorld();
            return;
        }

        if (backgroundSprite != null) backgroundSprite.update(dt);
        if (nameBannerSprite != null) nameBannerSprite.update(dt);
        for (Sprite s : backSpriteCache.values()) if (s != null) s.update(dt);

        if (enemyTurnActive) {
            processEnemyActions(dt);
            return;
        }

        if (party == null || party.isEmpty() || enemy == null) return;

        if (victoryScreenActive) {
            handleVictoryInput(input);
            return;
        }
        if (defeatScreenActive) {
            handleDefeatInput(input);
            return;
        }

        if (areAllEnemiesDead()) {
            if (bossBattleActive) {
                if (!bossVictoryEndTriggered) {
                    bossVictoryEndTriggered = true;
                    victoryScreenActive = false;
                    victoryMenuIndex = 0;
                    lastAction = "Victory! The boss has fallen.";
                    if (gp != null) {
                        gp.stopBattleMusic();
                        gp.endGameAfterBossVictory();
                    }
                    bossBattleActive = false;
                }
            } else {
                if (!victoryProcessed) {
                    gp.stopBattleMusic();
                    gp.ensureAmbientMusicPlaying();
                }
                lastAction = "Victory! " + joinEnemyNames() + " defeated!";
                if (input.consumeIfPressed("ENTER") || input.consumeIfPressed("ESC")) {
                    bossBattleActive = false;
                    victoryScreenActive = false;
                    gp.returnToWorld();
                }
            }
            return;
        }
        if (isPartyWiped()) {
            if (!defeatScreenActive) {
                defeatScreenActive = true;
                defeatMenuIndex = 0;
                lastAction = "The party has fallen.";
                gp.stopBattleMusic();
            }
            handleDefeatInput(input);
            return;
        }

        if (currentPlayerIndex < party.size() && waitingForInput) {
            Player currentPlayer = party.get(currentPlayerIndex);
            if (preparedTurnIndex != currentPlayerIndex) {
                Stats currentStats = currentPlayer.getStats();
                currentStats.restoreBattlePoints(1);
                preparedTurnIndex = currentPlayerIndex;
            }
            if (currentPlayer.getStats().getCurrentHp() <= 0) {
                nextTurn();
                return;
            }

            // ==== MAIN MENU ====
            if (!inSkillMenu) {
                if (input.consumeIfPressed("UP"))
                    mainMenuIndex = (mainMenuIndex - 1 + mainMenu.length) % mainMenu.length;
                if (input.consumeIfPressed("DOWN")) mainMenuIndex = (mainMenuIndex + 1) % mainMenu.length;

                if (input.consumeIfPressed("ENTER")) {
                    switch (mainMenuIndex) {
                        case 0: // Attack -> go to skill submenu
                            inSkillMenu = true;
                            selectedSkill = Math.min(selectedSkill, Math.max(0, skills.size() - 1));
                            break;

                        case 1: { // Meditate -> restore more BP then end turn
                            Stats stats = currentPlayer.getStats();
                            stats.restoreBattlePoints(2);
                            lastAction = currentPlayer.name + " meditates and restores " + "2" + " BP!";
                            if (gp != null)
                                gp.playSfx("guard"); // ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â°ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂªÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂµÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â´ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚ÂÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢
                            preparedTurnIndex = -1;
                            waitingForInput = false;
                            nextTurn();
                            break;
                        }

                        case 2: // Flee
                            lastAction = currentPlayer.name + " flees!";
                            if (gp != null)
                                gp.playSfx("bp_fail"); // ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â«ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â·ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂªÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂµÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â·ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚ÂÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂµÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¹ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Âµ
                            gp.returnToWorld();
                            break;
                    }
                }
            }
            // ==== SKILL SUBMENU ====
            else {
                if (input.consumeIfPressed("ESC") || input.consumeIfPressed("LEFT")) {
                    inSkillMenu = false; // back
                    return;
                }
                if (input.consumeIfPressed("UP")) selectedSkill = (selectedSkill - 1 + skills.size()) % skills.size();
                if (input.consumeIfPressed("DOWN")) selectedSkill = (selectedSkill + 1) % skills.size();
                if (input.consumeIfPressed("1")) selectedSkill = 0;
                if (input.consumeIfPressed("2")) selectedSkill = 1;
                if (input.consumeIfPressed("3")) selectedSkill = 2;

                if (input.consumeIfPressed("ENTER")) {
                    SkillCatalog.SkillDefinition skill = skills.get(selectedSkill);
                    ActionResolution resolution = performPlayerSkill(currentPlayer, skill);
                    if (resolution != ActionResolution.NO_ACTION) {
                        inSkillMenu = false;
                    }
                    if (resolution == ActionResolution.TURN_CONSUMED) {
                        nextTurn();
                    }
                }
            }
        }
    }

    void nextTurn() {
        currentPlayerIndex++;
        waitingForInput = true;
        preparedTurnIndex = -1;
        if (areAllEnemiesDead()) {
            return;
        }
        if (currentPlayerIndex >= party.size()) {
            // All players have acted, enemy turn
            performEnemyTurn();
        }
    }

    ActionResolution performPlayerSkill(Player player, SkillCatalog.SkillDefinition skill) {
        if (player == null || skill == null) {
            return ActionResolution.NO_ACTION;
        }
        PlayerSkills progression = player.getSkillProgression();
        if (progression != null) {
            progression.ensureSkill(skill.getId(), 1);
        }
        int level = progression != null ? progression.getLevel(skill) : 1;
        Stats stats = player.getStats();
        int bpCost = Math.max(0, skill.getBattleCost());
        if (!stats.spendBattlePoints(bpCost)) {
            lastAction = player.name + " lacks BP for " + skill.getName() + "!";
            if (gp != null) gp.playSfx("bp_fail");
            waitingForInput = true;
            return ActionResolution.NO_ACTION;
        }

        int effectValue = skill.computePower(level);
        if ("guard".equals(skill.getId())) {
            stats.applyTemporaryModifier(Collections.singletonMap(Stats.StatType.DEFENSE, effectValue));
            lastAction = player.name + " braces for impact (DEF +" + effectValue + ", Lv" + level + " " + skill.getName() + ")!";
            if (gp != null) gp.playSfx("guard");
        } else {
            Enemy target = null;
            if (enemy != null) {
                for (Enemy e : enemy) {
                    if (e != null && e.stats.getCurrentHp() > 0) {
                        target = e;
                        break;
                    }
                }
            }
            if (target != null) {
                int strength = stats.getTotalValue(Stats.StatType.STRENGTH);
                int damage = Math.max(1, strength + effectValue - target.stats.getTotalValue(Stats.StatType.DEFENSE));
                target.stats.setCurrentHp(Math.max(0, target.stats.getCurrentHp() - damage));
                lastAction = player.name + " used " + skill.getName() + " Lv" + level + " on " + target.name + " for " + damage + " damage!";
                if (gp != null) {
                    gp.playSfx("skill_strike");
                }
            } else {
                lastAction = player.name + " used " + skill.getName() + " but no valid target.";
            }
        }

        stats.regenerateBattlePoints(0.08);
        preparedTurnIndex = -1;
        waitingForInput = false;

        long alive = (enemy != null ? enemy.stream().filter(e -> e != null && e.stats.getCurrentHp() > 0).count()
                : 0);
        System.out.println(lastAction + " (Enemies alive: " + alive + ")");
        refocusEnemyIndex();
        if (alive == 0) {
            waitingForInput = true;
            processVictoryRewards();
            return ActionResolution.BATTLE_WON;
        }
        return ActionResolution.TURN_CONSUMED;
    }

    private void processVictoryRewards() {
        if (victoryProcessed) {
            return;
        }
        victoryProcessed = true;
        int goldReward = 0;
        int essenceReward = 0;
        int xpReward = 0;
        List<Player> party = gp.party;
        if (enemy != null) {
            for (Enemy e : enemy) {
                if (e == null || e.stats == null) {
                    continue;
                }
                Stats es = e.stats;
                int level = Math.max(1, es.getLevel());
                int maxHp = Math.max(20, es.getMaxHp());
                goldReward += 8 + level * 5 + maxHp / 12;
                essenceReward += 4 + level * 3;
                xpReward += 5 + level * 10;
            }
        }
        if (gp != null) {
            gp.onBattleVictory(goldReward, essenceReward);
            for (Player x : party) {
                x.stats.gainExp(xpReward);
            }
        }
    }

    private boolean areAllEnemiesDead() {
        if (enemy == null || enemy.isEmpty()) return true;
        for (Enemy e : enemy) {
            if (e != null && e.stats.getCurrentHp() > 0) return false;
        }
        return true;
    }

    private boolean isAnyPlayerAlive() {
        if (party == null || party.isEmpty()) return false;
        for (Player p : party) {
            if (p != null && p.getStats().getCurrentHp() > 0) return true;
        }
        return false;
    }

    private boolean isPartyWiped() {
        return !isAnyPlayerAlive();
    }

    void performEnemyTurn() {
        enemyActionQueue.clear();
        enemyActionTimer = ENEMY_ACTION_WINDUP;
        enemyTurnActive = false;

        if (enemy == null || enemy.isEmpty()) {
            finishEnemyTurn();
            return;
        }

        if (isPartyWiped()) {
            return;
        }

        for (Enemy e : enemy) {
            if (e == null || e.stats.getCurrentHp() <= 0) continue;
            enemyActionQueue.addLast(new EnemyAction(e));
        }

        if (enemyActionQueue.isEmpty()) {
            finishEnemyTurn();
            return;
        }

        lastAction = "Enemies are preparing their attacks...";
        enemyTurnActive = true;
        waitingForInput = false;
        refocusEnemyIndex();
    }

    private void processEnemyActions(double dt) {
        enemyActionTimer -= dt;
        if (enemyActionTimer > 0) {
            return;
        }
        if (enemyActionQueue.isEmpty()) {
            finishEnemyTurn();
            return;
        }
        enemyActionTimer = ENEMY_ACTION_INTERVAL;
        EnemyAction action = enemyActionQueue.pollFirst();
        Enemy attacker = action.attacker;
        if (attacker == null || attacker.stats.getCurrentHp() <= 0) {
            refocusEnemyIndex();
            return;
        }
        int focusIndex = enemy.indexOf(attacker);
        if (focusIndex >= 0) {
            currentEnemyIndex = focusIndex;
        } else {
            refocusEnemyIndex();
        }
        Player target = selectRandomAlivePlayer();
        if (target == null) {
            enemyActionQueue.clear();
            finishEnemyTurn();
            return;
        }
        Stats targetStats = target.getStats();
        int defense = targetStats.getTotalValue(Stats.StatType.DEFENSE);
        int roll = (int) Math.round(-2 + Math.random() * 5);
        int raw = attacker.stats.getTotalValue(Stats.StatType.STRENGTH) + roll - defense;
        int damage = Math.max(1, raw);
        targetStats.takeDamage(damage);
        lastAction = attacker.name + " attacks " + target.name + " for " + damage + "!";
        if (targetStats.getCurrentHp() <= 0) {
            lastAction += " " + target.name + " is knocked out!";
        }
        if (gp != null) gp.playSfx("enemy_attack");
        if (isPartyWiped()) {
            enemyActionQueue.clear();
            enemyTurnActive = false;
        }
    }

    private void finishEnemyTurn() {
        enemyTurnActive = false;
        enemyActionTimer = 0.0;
        enemyActionQueue.clear();
        if (party != null) {
            for (Player p : party) {
                if (p != null) {
                    p.getStats().clearTemporaryModifiers();
                }
            }
        }
        currentPlayerIndex = 0;
        preparedTurnIndex = -1;
        waitingForInput = true;
        refocusEnemyIndex();
    }

    private Player selectRandomAlivePlayer() {
        List<Player> alive = new ArrayList<>();
        if (party != null) {
            for (Player p : party) {
                if (p != null && p.getStats().getCurrentHp() > 0) {
                    alive.add(p);
                }
            }
        }
        if (alive.isEmpty()) {
            return null;
        }
        int index = (int) (Math.random() * alive.size());
        return alive.get(index);
    }

    private void refocusEnemyIndex() {
        currentEnemyIndex = findFirstAliveEnemyIndex();
    }

    private int findFirstAliveEnemyIndex() {
        if (enemy == null || enemy.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < enemy.size(); i++) {
            Enemy candidate = enemy.get(i);
            if (candidate != null && candidate.stats.getCurrentHp() > 0) {
                return i;
            }
        }
        return -1;
    }

    private Enemy resolveCurrentEnemy() {
        if (enemy == null || enemy.isEmpty()) {
            return null;
        }
        if (currentEnemyIndex < 0 || currentEnemyIndex >= enemy.size()) {
            currentEnemyIndex = findFirstAliveEnemyIndex();
        }
        if (currentEnemyIndex < 0 || currentEnemyIndex >= enemy.size()) {
            return null;
        }
        Enemy candidate = enemy.get(currentEnemyIndex);
        if (candidate == null || candidate.stats.getCurrentHp() <= 0) {
            currentEnemyIndex = findFirstAliveEnemyIndex();
            if (currentEnemyIndex < 0 || currentEnemyIndex >= enemy.size()) {
                return null;
            }
            candidate = enemy.get(currentEnemyIndex);
        }
        return candidate;
    }

    void draw(Graphics2D g) {
        // Background
        g.setColor(new Color(6, 6, 30));
        g.fillRect(0, 0, gp.vw, gp.vh);
        if (backgroundSprite != null) backgroundSprite.draw(g, 0, 0, 640, 360);

        // Battle title
        g.setColor(Color.BLACK);
        g.setFont(FontCustom.MainFont.deriveFont(24.0f));
        g.drawString("Battle vs: " + (enemy != null ? joinEnemyNames() : "Unknown"), 24, 20);

        // Party status (left side)
        int partyX = 24, partyY = 50;
        int lineHeight = 20;

        g.setFont(FontCustom.MainFont.deriveFont(16.0f));
        if (party != null) {
            for (int i = 0; i < party.size(); i++) {
                Player p = party.get(i);
                Stats stats = p.getStats();
                int currentHp = stats.getCurrentHp();
                int maxHp = stats.getMaxHp();
                int currentBp = stats.getCurrentBattlePoints();
                int maxBp = stats.getMaxBattlePoints();
                g.setColor(currentHp > 0 ? Color.BLACK : Color.RED);
                String line = String.format("%s  HP:%d/%d  BP:%d/%d", p.name, currentHp, maxHp, currentBp, maxBp);
                g.drawString(line, partyX, partyY + i * lineHeight);
            }
        }

        // Position enemy in center-right
        Enemy currentEnemy = resolveCurrentEnemy();
        boolean isBoss = (currentEnemy != null && currentEnemy.name != null && currentEnemy.name.toLowerCase(Locale.ROOT).contains("boss"));
        Sprite EnemySprite = null;
        int EnemySpriteBoxW = isBoss ? 168 : 84;
        int EnemySpriteBoxH = isBoss ? 168 : 84;
        int EnemySpriteAnchorX = Math.max(20, EnemySpriteBoxW + (isBoss ? 150 : 300));
        int EnemySpriteBaseY = Math.max(150, EnemySpriteBoxH + (isBoss ? 50 : 100));

        if (currentEnemy != null) {
            EnemySprite = getEnemySprite(currentEnemy);
        }

        if (EnemySprite != null) {
            int frameW = Math.max(1, EnemySprite.getFrameWidth());
            int frameH = Math.max(1, EnemySprite.getFrameHeight());
            double scale = Math.min(1.0, Math.min(EnemySpriteBoxW / (double) frameW, EnemySpriteBoxH / (double) frameH));
            int drawW = Math.max(1, (int) Math.round(frameW * scale));
            int drawH = Math.max(1, (int) Math.round(frameH * scale));
            int drawX = EnemySpriteAnchorX + 30;
            int drawY = EnemySpriteBaseY - drawH;
            EnemySprite.draw(g, drawX, drawY, drawW, drawH);
        }

        // Enemy HP bar
        if (currentEnemy != null) {
            int barW = 100, barH = 8;
            int barX = EnemySpriteAnchorX + EnemySpriteBoxW / 2, barY = EnemySpriteBaseY - EnemySpriteBoxH - 20;

            g.setColor(Color.RED);
            g.fillRect(barX, barY, barW, barH);
            g.setColor(Color.GREEN);
            int hpWidth = (int) (barW * currentEnemy.stats.getCurrentHp() / (double) currentEnemy.stats.getMaxHp());
            g.fillRect(barX, barY, hpWidth, barH);
            g.setColor(Color.black);
            g.drawRect(barX, barY, barW, barH);

            // Enemy HP text
            g.setFont(FontCustom.MainFont.deriveFont(12.0f));
            g.drawString(currentEnemy.name + " Lvl " + currentEnemy.stats.getLevel(), barX, barY - 4);
        }

        // Player selection panel (bottom)
        int panelH = 100;
        Sprite PlayerSprite = null;
        int PlayerSpriteAnchorX = 0;
        int PlayerSpriteBaseY = gp.vh;
        int PlayerSpriteBoxW = 384;
        int PlayerSpriteBoxH = 250;

        Player currentPlayer = resolveCurrentPlayer();
        if (currentPlayer != null) {
            PlayerSprite = getPlayerSprite(currentPlayer);
        }

        if (PlayerSprite != null) {
            int frameW = Math.max(1, PlayerSprite.getFrameWidth());
            int frameH = Math.max(1, PlayerSprite.getFrameHeight());
            double scale = Math.min(1.0, Math.min(PlayerSpriteBoxW / (double) frameW, PlayerSpriteBoxH / (double) frameH));
            int drawW = Math.max(1, (int) Math.round(frameW * scale));
            int drawH = Math.max(1, (int) Math.round(frameH * scale));
            int drawX = PlayerSpriteAnchorX + 30;
            int drawY = PlayerSpriteBaseY - drawH;
            PlayerSprite.draw(g, drawX, drawY, drawW, drawH);
        }

        int panelY = gp.vh - panelH - 20;
        if (panelY < 20) {
            panelY = Math.max(10, gp.vh - panelH - 20);
        }
        int panelX = gp.vw - 360;
        int panelW = gp.vw - panelX - 10;

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 10, 10);

        int skillTextX = panelX + 10;
        int skillY = panelY + 20;

        g.setColor(Color.WHITE);
        g.setFont(FontCustom.MainFont.deriveFont(14.0f));

        if (!inSkillMenu) {
            // ----- MAIN MENU -----
            g.drawString("Choose Action:", skillTextX, skillY);
            for (int i = 0; i < mainMenu.length; i++) {
                g.setColor(i == mainMenuIndex ? Color.YELLOW : Color.WHITE);
                g.drawString((i == mainMenuIndex ? "> " : "  ") + (i + 1) + ". " + mainMenu[i],
                        skillTextX, skillY + 24 + i * 20);
            }
        } else {
            // ----- SKILL SUBMENU  -----
            g.drawString("Select Skills (Left = Back)", skillTextX, skillY);
            for (int i = 0; i < skills.size(); i++) {
                SkillCatalog.SkillDefinition skill = skills.get(i);
                int y = skillY + 20 + i * 20;
                g.setColor(i == selectedSkill ? Color.YELLOW : Color.WHITE);
                int level = 1;
                if (currentPlayer != null && currentPlayer.getSkillProgression() != null) {
                    level = currentPlayer.getSkillProgression().getLevel(skill);
                }
                String desc = skill.describe(level);
                String baseText = String.format("%d. %s ", i + 1, skill.getName());
                String fullText = desc.isEmpty() ? baseText : baseText + " - " + desc;
                g.drawString((i == selectedSkill ? "> " : "  ") + fullText, skillTextX, y);
            }
        }

        // Action log
        g.setColor(Color.CYAN);
        g.setFont(FontCustom.MainFont.deriveFont(10.0f));
        FontMetrics fm = g.getFontMetrics();
        int logX = Math.max(24, gp.vw - fm.stringWidth(lastAction) - 24);
        g.drawString(lastAction, logX, gp.vh - 10);
        if (victoryScreenActive) {
            drawVictoryOverlay(g);
        }
        if (defeatScreenActive) {
            drawDefeatOverlay(g);
        }
    }

    private void handleVictoryInput(InputManager input) {
        if (!victoryScreenActive) {
            return;
        }
        if (input.consumeIfPressed("UP")) {
            victoryMenuIndex = (victoryMenuIndex - 1 + VICTORY_OPTIONS.length) % VICTORY_OPTIONS.length;
            gp.playSfx("menu_move");
        } else if (input.consumeIfPressed("DOWN")) {
            victoryMenuIndex = (victoryMenuIndex + 1) % VICTORY_OPTIONS.length;
            gp.playSfx("menu_move");
        }
        if (input.consumeIfPressed("ENTER")) {
            gp.playSfx("menu_select");
            switch (victoryMenuIndex) {
                case 0 -> {
                    if (gp.loadMostRecentSave()) {
                        victoryScreenActive = false;
                        bossBattleActive = false;
                        gp.ensureAmbientMusicPlaying();
                    } else {
                        lastAction = "No recent save to load.";
                        gp.playSfx("menu_cancel");
                    }
                }
                case 1 -> {
                    gp.returnToTitleFromBattle();
                    victoryScreenActive = false;
                    bossBattleActive = false;
                }
            }
        } else if (input.consumeIfPressed("ESC")) {
            victoryMenuIndex = 1;
            gp.playSfx("menu_move");
        }
    }

    private void drawVictoryOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, gp.vw, gp.vh);

        g.setFont(FontCustom.MainFont.deriveFont(Font.BOLD, 54f));
        g.setColor(new Color(220, 200, 80));
        String title = "VICTORY";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (gp.vw - fm.stringWidth(title)) / 2;
        int titleY = gp.vh / 2 - 90;
        g.drawString(title, titleX, titleY);

        g.setFont(FontCustom.MainFont.deriveFont(18f));
        g.setColor(new Color(230, 230, 230));
        String subtitle = "The boss has been defeated.";
        int subX = (gp.vw - g.getFontMetrics().stringWidth(subtitle)) / 2;
        g.drawString(subtitle, subX, titleY + 32);

        g.setFont(FontCustom.MainFont.deriveFont(18f));
        for (int i = 0; i < VICTORY_OPTIONS.length; i++) {
            boolean selected = i == victoryMenuIndex;
            g.setColor(selected ? Color.WHITE : new Color(200, 200, 200));
            String option = VICTORY_OPTIONS[i];
            int optWidth = g.getFontMetrics().stringWidth(option);
            int optX = (gp.vw - optWidth) / 2;
            int optY = titleY + 80 + i * 34;
            g.drawString(option, optX, optY);
        }
    }

    private void handleDefeatInput(InputManager input) {
        if (!defeatScreenActive) {
            return;
        }
        if (input.consumeIfPressed("UP")) {
            defeatMenuIndex = (defeatMenuIndex - 1 + DEFEAT_OPTIONS.length) % DEFEAT_OPTIONS.length;
            gp.playSfx("menu_move");
        } else if (input.consumeIfPressed("DOWN")) {
            defeatMenuIndex = (defeatMenuIndex + 1) % DEFEAT_OPTIONS.length;
            gp.playSfx("menu_move");
        }
        if (input.consumeIfPressed("ENTER")) {
            gp.playSfx("menu_select");
            switch (defeatMenuIndex) {
                case 0 -> {
                    if (gp.loadMostRecentSave()) {
                        defeatScreenActive = false;
                        bossBattleActive = false;
                        gp.ensureAmbientMusicPlaying();
                    } else {
                        lastAction = "No recent save to load.";
                        gp.playSfx("menu_cancel");
                    }
                }
                case 1 -> {
                    gp.returnToTitleFromBattle();
                    defeatScreenActive = false;
                    bossBattleActive = false;
                }
            }
        } else if (input.consumeIfPressed("ESC")) {
            defeatMenuIndex = 1;
            gp.playSfx("menu_move");
        }
    }

    private void drawDefeatOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, gp.vw, gp.vh);

        g.setFont(FontCustom.MainFont.deriveFont(Font.BOLD, 56f));
        g.setColor(new Color(200, 40, 30));
        String title = "YOU DIED";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (gp.vw - fm.stringWidth(title)) / 2;
        int titleY = gp.vh / 2 - 80;
        g.drawString(title, titleX, titleY);

        g.setFont(FontCustom.MainFont.deriveFont(18f));
        for (int i = 0; i < DEFEAT_OPTIONS.length; i++) {
            boolean selected = i == defeatMenuIndex;
            g.setColor(selected ? Color.WHITE : new Color(200, 200, 200));
            String option = DEFEAT_OPTIONS[i];
            int optWidth = g.getFontMetrics().stringWidth(option);
            int optX = (gp.vw - optWidth) / 2;
            int optY = titleY + 80 + i * 32;
            g.drawString(option, optX, optY);
        }
    }

    private Sprite getPlayerSprite(Player player) {
        if (player == null || player.name == null) {
            return null;
        }
        String normalized = player.name.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        String key = "player:" + normalized;
        Sprite sprite = backSpriteCache.get(key);
        if (sprite == null) {
            sprite = loadBackSprite(normalized);
            backSpriteCache.put(key, sprite);
        }
        return sprite;
    }

    private Sprite getEnemySprite(Enemy enemy) {
        if (enemy == null || enemy.name == null) {
            return null;
        }
        String normalized = enemy.name.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        String key = "enemy:" + normalized;
        Sprite sprite = backSpriteCache.get(key);
        if (sprite == null) {
            sprite = loadEnemySprite(normalized);
            backSpriteCache.put(key, sprite);
        }
        return sprite;
    }

    private Sprite loadBackSprite(String nameKey) {
        if (nameKey == null || nameKey.isEmpty()) {
            return null;
        }
        String pathStr = "resources/sprites/" + nameKey + "_back.png";
        Sprite sprite = loadSprite(pathStr, 274, 326);
        return sprite != null ? sprite : createPlaceholderSprite(64, 64, Color.MAGENTA);
    }

    private Sprite loadEnemySprite(String nameKey) {
        if (nameKey == null || nameKey.isEmpty()) {
            return null;
        }
        String pathStr = "resources/sprites/" + nameKey + "_back.png";
        Sprite sprite;
        if (nameKey.equals("boss")) {
            sprite = loadSprite(pathStr, 168, 168);
            System.out.println("boss");
        } else {
            sprite = loadSprite(pathStr, 84, 84);
        }
        return sprite != null ? sprite : createPlaceholderSprite(64, 64, Color.MAGENTA);
    }

    private Player resolveCurrentPlayer() {
        if (party == null || party.isEmpty()) {
            return null;
        }
        currentPlayerIndex = Math.max(0, Math.min(currentPlayerIndex, party.size() - 1));
        Player candidate = party.get(currentPlayerIndex);
        if (candidate == null || candidate.getStats().getCurrentHp() <= 0) {
            for (int i = 0; i < party.size(); i++) {
                Player p = party.get(i);
                if (p != null && p.getStats().getCurrentHp() > 0) {
                    currentPlayerIndex = i;
                    candidate = p;
                    break;
                }
            }
        }
        return candidate != null && candidate.getStats().getCurrentHp() > 0 ? candidate : null;
    }

    private java.util.List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        java.util.List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        String[] words = text.split(" +");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
                continue;
            }
            String candidate = current + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) {
                current.append(" ").append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private Sprite createPlaceholderSprite(int width, int height, Color bodyColor) {
        BufferedImage placeholder = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(bodyColor);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, width - 1, height - 1);
        g.dispose();
        return Sprite.forStaticImage(placeholder);
    }

    private enum ActionResolution {
        NO_ACTION,
        TURN_CONSUMED,
        BATTLE_WON
    }

    private static final class EnemyAction {
        final Enemy attacker;

        EnemyAction(Enemy attacker) {
            this.attacker = attacker;
        }
    }
}


