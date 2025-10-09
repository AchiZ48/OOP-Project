import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    static class Skill {
        String name;
        int cost;
        int power;
        String description;

        Skill(String n, int c, int p, String desc) {
            name = n; cost = c; power = p; description = desc;
        }
    }

    public BattleScreen(GamePanel gp) {
        this.gp = gp;
        try {
            backgroundSprite = SpriteLoader.loadSheet("resources/battlebg/bg1.png", 640, 360);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        skills = Arrays.asList(
                new Skill("Strike", 1, 6, "Basic physical attack"),
                new Skill("Power Attack", 5, 10, "Strong physical attack"),
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
        this.party = new ArrayList<>(party);
        this.enemy = new ArrayList<>(enemy); // ต้องไม่ว่าง
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
            member.refreshDerivedStats();
        }

        for (Enemy e : this.enemy) {
            if (e != null) e.hp = e.maxHp;
        }

        if (ambush) {
            this.lastAction = "Ambushed! Enemies strike first!";
            this.currentEnemyIndex = 0;
            performEnemyTurn();
            this.waitingForInput = true;
            this.currentPlayerIndex = 0;
            this.preparedTurnIndex = -1;
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
                currentPlayer.refreshDerivedStats();
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
                            currentPlayer.refreshDerivedStats();
                            lastAction = currentPlayer.name + " meditates and restores " + "2" + " BP!";
                            if (gp != null) gp.playSfx("guard"); // ใช้เสียงเดิมแทนก่อน
                            preparedTurnIndex = -1;
                            waitingForInput = false;
                            nextTurn();
                            break;
                        }

                        case 2: // Flee
                            lastAction = currentPlayer.name + " flees!";
                            if (gp != null) gp.playSfx("bp_fail"); // หรือเสียงอื่นที่มี
                            gp.returnToWorld();
                            break;
                    }
                }
            }
            // ==== SKILL SUBMENU (ของเดิม) ====
            else {
                if (input.consumeIfPressed("ESC") || input.consumeIfPressed("LEFT")) {
                    inSkillMenu = false; // back ไปเมนูหลัก
                    return;
                }
                if (input.consumeIfPressed("UP"))   selectedSkill = (selectedSkill - 1 + skills.size()) % skills.size();
                if (input.consumeIfPressed("DOWN")) selectedSkill = (selectedSkill + 1) % skills.size();
                if (input.consumeIfPressed("1")) selectedSkill = 0;
                if (input.consumeIfPressed("2")) selectedSkill = 1;
                if (input.consumeIfPressed("3")) selectedSkill = 2;

                if (input.consumeIfPressed("ENTER")) {
                    Skill skill = skills.get(selectedSkill);
                    performPlayerSkill(currentPlayer, skill);
                    inSkillMenu = false; // หลังใช้สกิล จบรอบ-กลับเมนูหลักรอบหน้า
                    nextTurn();
                }
            }
        }
    }

    void nextTurn() {
        currentPlayerIndex++;
        waitingForInput = true;
        preparedTurnIndex = -1;
        if (currentPlayerIndex >= party.size()) {
            // All players have acted, enemy turn
            performEnemyTurn();
            currentPlayerIndex = 0;
        }
    }

    void performPlayerSkill(Player player, Skill skill) {
        if (player == null || skill == null) {
            return;
        }
        Stats stats = player.getStats();
        if (!stats.spendBattlePoints(Math.max(0, skill.cost))) {
            lastAction = player.name + " lacks BP for " + skill.name + "!";
            if (gp != null) gp.playSfx("bp_fail");
            waitingForInput = true;
            return;
        }

        if ("Guard".equals(skill.name)) {
            stats.applyTemporaryModifier(Collections.singletonMap(Stats.StatType.DEFENSE, 4));
            lastAction = player.name + " braces for impact!";
            if (gp != null) gp.playSfx("guard");
        } else {
            Enemy target = null;
            if (enemy != null) {
                for (Enemy e : enemy) {
                    if (e != null && e.hp > 0) { target = e; break; }
                }
            }
            if (target != null) {
                int strength = stats.getTotalValue(Stats.StatType.STRENGTH);
                int damage = Math.max(1, strength + skill.power - target.def);
                target.hp = Math.max(0, target.hp - damage);
                lastAction = player.name + " used " + skill.name + " on " + target.name + " for " + damage + " damage!";
            } else {
                lastAction = player.name + " used " + skill.name + " but no valid target.";
            }
            if (gp != null) {
                gp.playSfx("skill_strike");
            }
        }

        stats.regenerateBattlePoints(0.08);
        player.refreshDerivedStats();
        preparedTurnIndex = -1;
        waitingForInput = false;

        // แสดงสถานะสั้น ๆ แทนการอ้าง enemy เดี่ยว
        long alive = (enemy != null ? enemy.stream().filter(e -> e != null && e.hp > 0).count()
                : 0);
        System.out.println(lastAction + " (Enemies alive: " + alive + ")");
    }

    private boolean areAllEnemiesDead() {
        if (enemy == null || enemy.isEmpty()) return true;
        for (Enemy e : enemy) {
            if (e != null && e.hp > 0) return false;
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
        // รายชื่อผู้เล่นที่ยังมีชีวิต
        List<Player> aliveParty = new ArrayList<>();
        for (Player p : party) {
            if (p.getStats().getCurrentHp() > 0) aliveParty.add(p);
        }
        if (aliveParty.isEmpty()) return;

        StringBuilder sb = new StringBuilder();

        if (enemy != null && !enemy.isEmpty()) {
            // หลายศัตรู: ศัตรูที่ยังมีชีวิตโจมตีคนละ 1 ครั้ง
            for (Enemy e : enemy) {
                if (e == null || e.hp <= 0) continue;

                Player target = aliveParty.get((int)(Math.random() * aliveParty.size()));
                Stats targetStats = target.getStats();
                int defense = targetStats.getTotalValue(Stats.StatType.DEFENSE);

                // ใส่สุ่มนิดหน่อยให้ดาเมจมีสวิง
                int roll = (int)Math.round(-2 + Math.random()*5); // -2..+2
                int raw = e.str + roll - defense;
                int damage = Math.max(1, raw);

                targetStats.takeDamage(damage);
                target.refreshDerivedStats();

                sb.append(e.name).append(" attacks ").append(target.name)
                        .append(" for ").append(damage).append("! ");

                if (targetStats.getCurrentHp() <= 0) {
                    sb.append(target.name).append(" is knocked out! ");
                    // อัปเดตรายชื่อเป้าหมายที่ยังมีชีวิต
                    aliveParty.clear();
                    for (Player p : party) {
                        if (p.getStats().getCurrentHp() > 0) aliveParty.add(p);
                    }
                    if (aliveParty.isEmpty()) break;
                }

                if (gp != null) gp.playSfx("enemy_attack");
            }
        }

        lastAction = sb.length() > 0 ? sb.toString() : "Enemies hesitate...";

        // ล้างบัฟชั่วคราวฝั่งผู้เล่นเมื่อจบรอบศัตรู
        for (Player p : party) {
            Stats stats = p.getStats();
            stats.clearTemporaryModifiers();
            p.refreshDerivedStats();
        }

        preparedTurnIndex = -1;
        System.out.println(lastAction);
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
            int hpWidth = (int) (barW * currentEnemy.hp / (double) currentEnemy.maxHp);
            g.fillRect(barX, barY, hpWidth, barH);
            g.setColor(Color.WHITE);
            g.drawRect(barX, barY, barW, barH);

            // Enemy HP text
            g.setFont(FontCustom.MainFont.deriveFont(12.0f));
            g.drawString(currentEnemy.name + " Lvl " +currentEnemy.level , barX, barY - 4);
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
            // ----- SKILL SUBMENU (ของเดิม) -----
            g.drawString("1.1 Select Skills (ESC = Back)", skillTextX, skillY);
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
        g.setFont(FontCustom.MainFont.deriveFont(12.0f));
        g.drawString("Last Action: " + lastAction, 30, gp.vh - 10);

        // Instructions
        g.setColor(Color.GRAY);
        g.setFont(FontCustom.MainFont.deriveFont(12.0f));
        g.drawString("ESC: Flee | Enter: Use Skill", gp.vw - 200, gp.vh - 10);
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

    // เพิ่มตัวโหลดสำหรับศัตรู (fallback ได้หลายโฟลเดอร์/ชื่อไฟล์)
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
