import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BattleScreen {
    GamePanel gp;
    Sprite backgroundSprite;
    Sprite nameBannerSprite;
    List<Player> party;
    List<Enemy> enemy;
    int currentPlayerIndex = 0;
    int currentEnemyIndex = 0;
    List<Skill> skills;
    int selectedSkill = 0;
    boolean waitingForInput = true;
    String lastAction = "";
    private int preparedTurnIndex = -1;
    final Map<String, Sprite> backSpriteCache = new HashMap<>();
    String[] mainMenu = {"Attack", "Meditate", "Flee"};
    int mainMenuIndex = 0;
    boolean inSkillMenu = false;
    private static final double ENEMY_ACTION_WINDUP = 0.45;
    private static final double ENEMY_ACTION_INTERVAL = 0.85;
    private final Deque<EnemyAction> enemyActionQueue = new ArrayDeque<>();
    private boolean enemyTurnActive = false;
    private double enemyActionTimer = 0.0;

    static class Skill {
        String name;
        int cost;
        int power;
        String description;

        Skill(String n, int c, int p, String desc) {
            name = n; cost = c; power = p; description = desc;
        }
    }

    private enum ActionResolution {
        NO_ACTION,
        TURN_CONSUMED,
        BATTLE_WON
    }

    public BattleScreen(GamePanel gp) {
        this.gp = gp;


        skills = Arrays.asList(
                new Skill("Strike", 1, 6, "Basic attack"),
                new Skill("Power Attack", 5, 10, "Strong attack"),
                new Skill("Guard", 1, 0, "Reduce incoming damage")
        );
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
        switch (gp.map.getZone((int)(leader.x / 32), (int)(leader.y / 32))){
            case 1 : path = "bg2"; break;
            case 4 : path = "snow"; break;
            default: path = "bg1"; break;
        }
        try {
            backgroundSprite = SpriteLoader.loadSheet("resources/battlebg/" + path +".png", 640, 360);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.party = new ArrayList<>(party);
        this.enemy = new ArrayList<>(enemy);
        System.out.println("Enttete");
        this.currentPlayerIndex = 0;
        this.currentEnemyIndex = 0;
        this.waitingForInput = true;
        this.selectedSkill = 0;
        this.lastAction = "Battle started vs " + joinEnemyNames() + "!";
        this.preparedTurnIndex = -1;
        this.selectedSkill = 0;
        this.mainMenuIndex = 0;
        this.inSkillMenu = false;

        for (Player member : this.party) {
            if (member == null) continue;
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
        if (input.consumeIfPressed("ESC")) { gp.returnToWorld(); return; }

        if (backgroundSprite != null) backgroundSprite.update(dt);
        if (nameBannerSprite != null) nameBannerSprite.update(dt);
        for (Sprite s : backSpriteCache.values()) if (s != null) s.update(dt);

        if (enemyTurnActive) {
            processEnemyActions(dt);
            return;
        }

        if (party == null || party.isEmpty() || enemy == null) return;

        if (areAllEnemiesDead()) {
            lastAction = "Victory! " + joinEnemyNames() + " defeated!";
            if (input.consumeIfPressed("ENTER") || input.consumeIfPressed("ESC")) gp.returnToWorld();
            return;
        }
        if (isPartyWiped()) {
            lastAction = "Defeat! All party members are down!";
            if (input.consumeIfPressed("ENTER") || input.consumeIfPressed("ESC")) gp.returnToWorld();
            return;
        }

        if (currentPlayerIndex < party.size() && waitingForInput) {
            Player currentPlayer = party.get(currentPlayerIndex);
            if (preparedTurnIndex != currentPlayerIndex) {
                Stats currentStats = currentPlayer.getStats();
                currentStats.restoreBattlePoints(1);
                preparedTurnIndex = currentPlayerIndex;
            }
            if (currentPlayer.getStats().getCurrentHp() <= 0) { nextTurn(); return; }

            // ==== MAIN MENU ====
            if (!inSkillMenu) {
                if (input.consumeIfPressed("UP"))   mainMenuIndex = (mainMenuIndex - 1 + mainMenu.length) % mainMenu.length;
                if (input.consumeIfPressed("DOWN")) mainMenuIndex = (mainMenuIndex + 1) % mainMenu.length;

                if (input.consumeIfPressed("ENTER")) {
                    switch (mainMenuIndex) {
                        case 0: // Attack -> go to skill submenu
                            inSkillMenu = true;
                            selectedSkill = Math.min(selectedSkill, Math.max(0, skills.size()-1));
                            break;

                        case 1: { // Meditate -> restore more BP then end turn
                            Stats stats = currentPlayer.getStats();
                            stats.restoreBattlePoints(2);
                            lastAction = currentPlayer.name + " meditates and restores " + "2" + " BP!";
                            if (gp != null) gp.playSfx("guard"); // ÃƒÂ Ã‚Â¹Ã†â€™ÃƒÂ Ã‚Â¸Ã…Â ÃƒÂ Ã‚Â¹Ã¢â‚¬Â°ÃƒÂ Ã‚Â¹Ã¢â€šÂ¬ÃƒÂ Ã‚Â¸Ã‚ÂªÃƒÂ Ã‚Â¸Ã‚ÂµÃƒÂ Ã‚Â¸Ã‚Â¢ÃƒÂ Ã‚Â¸Ã¢â‚¬Â¡ÃƒÂ Ã‚Â¹Ã¢â€šÂ¬ÃƒÂ Ã‚Â¸Ã¢â‚¬ÂÃƒÂ Ã‚Â¸Ã‚Â´ÃƒÂ Ã‚Â¸Ã‚Â¡ÃƒÂ Ã‚Â¹Ã‚ÂÃƒÂ Ã‚Â¸Ã¢â‚¬â€ÃƒÂ Ã‚Â¸Ã¢â€žÂ¢ÃƒÂ Ã‚Â¸Ã‚ÂÃƒÂ Ã‚Â¹Ã‹â€ ÃƒÂ Ã‚Â¸Ã‚Â­ÃƒÂ Ã‚Â¸Ã¢â€žÂ¢
                            preparedTurnIndex = -1;
                            waitingForInput = false;
                            nextTurn();
                            break;
                        }

                        case 2: // Flee
                            lastAction = currentPlayer.name + " flees!";
                            if (gp != null) gp.playSfx("bp_fail"); // ÃƒÂ Ã‚Â¸Ã‚Â«ÃƒÂ Ã‚Â¸Ã‚Â£ÃƒÂ Ã‚Â¸Ã‚Â·ÃƒÂ Ã‚Â¸Ã‚Â­ÃƒÂ Ã‚Â¹Ã¢â€šÂ¬ÃƒÂ Ã‚Â¸Ã‚ÂªÃƒÂ Ã‚Â¸Ã‚ÂµÃƒÂ Ã‚Â¸Ã‚Â¢ÃƒÂ Ã‚Â¸Ã¢â‚¬Â¡ÃƒÂ Ã‚Â¸Ã‚Â­ÃƒÂ Ã‚Â¸Ã‚Â·ÃƒÂ Ã‚Â¹Ã‹â€ ÃƒÂ Ã‚Â¸Ã¢â€žÂ¢ÃƒÂ Ã‚Â¸Ã¢â‚¬â€ÃƒÂ Ã‚Â¸Ã‚ÂµÃƒÂ Ã‚Â¹Ã‹â€ ÃƒÂ Ã‚Â¸Ã‚Â¡ÃƒÂ Ã‚Â¸Ã‚Âµ
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
                if (input.consumeIfPressed("UP"))   selectedSkill = (selectedSkill - 1 + skills.size()) % skills.size();
                if (input.consumeIfPressed("DOWN")) selectedSkill = (selectedSkill + 1) % skills.size();
                if (input.consumeIfPressed("1")) selectedSkill = 0;
                if (input.consumeIfPressed("2")) selectedSkill = 1;
                if (input.consumeIfPressed("3")) selectedSkill = 2;

                if (input.consumeIfPressed("ENTER")) {
                    Skill skill = skills.get(selectedSkill);
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
            return;
        }
    }

    ActionResolution performPlayerSkill(Player player, Skill skill) {
        if (player == null || skill == null) {
            return ActionResolution.NO_ACTION;
        }
        Stats stats = player.getStats();
        if (!stats.spendBattlePoints(Math.max(0, skill.cost))) {
            lastAction = player.name + " lacks BP for " + skill.name + "!";
            if (gp != null) gp.playSfx("bp_fail");
            waitingForInput = true;
            return ActionResolution.NO_ACTION;
        }

        if ("Guard".equals(skill.name)) {
            stats.applyTemporaryModifier(Collections.singletonMap(Stats.StatType.DEFENSE, 4));
            lastAction = player.name + " braces for impact!";
            if (gp != null) gp.playSfx("guard");
        } else {
            Enemy target = null;
            if (enemy != null) {
                for (Enemy e : enemy) {
                    if (e != null && e.stats.getCurrentHp() > 0) { target = e; break; }
                }
            }
            if (target != null) {
                int strength = stats.getTotalValue(Stats.StatType.STRENGTH);
                int damage = Math.max(1, strength + skill.power - target.stats.getTotalValue(Stats.StatType.DEFENSE));
                target.stats.setCurrentHp(Math.max(0, target.stats.getCurrentHp() - damage));
                lastAction = player.name + " used " + skill.name + " on " + target.name + " for " + damage + " damage!";
            } else {
                lastAction = player.name + " used " + skill.name + " but no valid target.";
            }
            if (gp != null) {
                gp.playSfx("skill_strike");
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
            return ActionResolution.BATTLE_WON;
        }
        return ActionResolution.TURN_CONSUMED;
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
            return;
        }
        int focusIndex = enemy.indexOf(attacker);
        if (focusIndex >= 0) {
            currentEnemyIndex = focusIndex;
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
            return;
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
    void draw(Graphics2D g) {
        // Background
        g.setColor(new Color(6, 6, 30));
        g.fillRect(0, 0, gp.vw, gp.vh);
        if (backgroundSprite != null) backgroundSprite.draw(g,0,0,640,360);

        // Battle title
        g.setColor(Color.WHITE);
        g.setFont(FontCustom.MainFont.deriveFont(24.0f));
        g.drawString("Battle vs: " + (enemy != null ? joinEnemyNames() : "Unknown"), 24, 40);

        // Party status (left side)
        int partyX = 24, partyY = 100;
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
                g.setColor(currentHp > 0 ? Color.WHITE : Color.DARK_GRAY);
                String line = String.format("%s  HP:%d/%d  BP:%d/%d", p.name, currentHp, maxHp, currentBp, maxBp);
                g.drawString(line, partyX, partyY + i * lineHeight);
            }
        }

        // Position enemy in center-right
        Sprite EnemySprite = null;
        int EnemySpriteBoxW = 84;
        int EnemySpriteBoxH = 84;
        int EnemySpriteAnchorX = Math.max(20, EnemySpriteBoxW + 300);
        int EnemySpriteBaseY  = Math.max(150, EnemySpriteBoxH + 100);

        Enemy currentEnemy = null;
        if (enemy != null && !enemy.isEmpty() && currentEnemyIndex >= 0 && currentEnemyIndex < enemy.size()) {
            currentEnemy = enemy.get(currentEnemyIndex);
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
            int barX = EnemySpriteAnchorX + EnemySpriteBoxW/2, barY = EnemySpriteBaseY - EnemySpriteBoxH - 20;

            g.setColor(Color.RED);
            g.fillRect(barX, barY, barW, barH);
            g.setColor(Color.GREEN);
            int hpWidth = (int) (barW * currentEnemy.stats.getCurrentHp() / (double) currentEnemy.stats.getMaxHp());
            g.fillRect(barX, barY, hpWidth, barH);
            g.setColor(Color.WHITE);
            g.drawRect(barX, barY, barW, barH);

            // Enemy HP text
            g.setFont(FontCustom.MainFont.deriveFont(12.0f));
            g.drawString(currentEnemy.name + " Lvl " +currentEnemy.stats.getLevel() , barX, barY - 4);
        }

        // Player selection panel (bottom)
        int panelH = 120;
        Sprite PlayerSprite = null;
        int PlayerSpriteAnchorX = 0;
        int PlayerSpriteBaseY = gp.vh;
        int PlayerSpriteBoxW = 384;
        int PlayerSpriteBoxH = 250;

        if (party != null && !party.isEmpty() && currentPlayerIndex >= 0 && currentPlayerIndex < party.size()) {
            PlayerSprite = getPlayerSprite(party.get(currentPlayerIndex));
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

        int panelY = gp.vh - panelH - 40;
        if (panelY < 20) {
            panelY = Math.max(10, gp.vh - panelH - 20);
        }
        int panelX = Math.min(Math.max(260, PlayerSpriteAnchorX + PlayerSpriteBoxW + 40), Math.max(20, gp.vw - 360));
        int panelW = Math.max(280, gp.vw - panelX - 20);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 10, 10);

        int skillTextX = panelX + 20;
        int skillY = panelY + 32;

        g.setColor(Color.WHITE);
        g.setFont(FontCustom.MainFont.deriveFont(14.0f));

        if (!inSkillMenu) {
            // ----- MAIN MENU -----
            g.drawString("Choose Action:", skillTextX, skillY);
            for (int i = 0; i < mainMenu.length; i++) {
                g.setColor(i == mainMenuIndex ? Color.YELLOW : Color.WHITE);
                g.drawString((i == mainMenuIndex ? "> " : "  ") + (i+1) + ". " + mainMenu[i],
                        skillTextX, skillY + 24 + i * 20);
            }
            g.setColor(Color.GRAY);
            g.setFont(FontCustom.MainFont.deriveFont(10.0f));
            g.drawString("Up/Down: Move  |  Enter: Confirm", panelX + 12, panelY + panelH - 12);
        } else {
            // ----- SKILL SUBMENU  -----
            g.drawString("Select Skills (Left = Back)", skillTextX, skillY);
            for (int i = 0; i < skills.size(); i++) {
                Skill skill = skills.get(i);
                int y = skillY + 20 + i * 20;
                g.setColor(i == selectedSkill ? Color.YELLOW : Color.WHITE);
                String skillText = String.format("%d. %s (Cost: %d) - %s",
                        i + 1, skill.name, skill.cost, skill.description);
                g.drawString((i == selectedSkill ? "> " : "  ") + skillText, skillTextX, y);
            }
            g.setColor(Color.GRAY);
            g.setFont(FontCustom.MainFont.deriveFont(10.0f));
            g.drawString("Up/Down or 1/2/3: Select  |  Enter: Use  |  ESC/Left: Back",
                    panelX + 12, panelY + panelH - 12);
        }

        // Action log
        g.setColor(Color.CYAN);
        g.setFont(FontCustom.MainFont.deriveFont(10.0f));
        g.drawString("Last Action: " + lastAction, 180, gp.vh - 10);

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

    private static final class EnemyAction {
        final Enemy attacker;

        EnemyAction(Enemy attacker) {
            this.attacker = attacker;
        }
    }
    private Sprite loadEnemySprite(String nameKey) {
        if (nameKey == null || nameKey.isEmpty()) {
            return null;
        }
        String pathStr = "resources/sprites/" + nameKey + "_back.png";
        Sprite sprite = loadSprite(pathStr, 84, 84);
        return sprite != null ? sprite : createPlaceholderSprite(64, 64, Color.MAGENTA);
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
}

