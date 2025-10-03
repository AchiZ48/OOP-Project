import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BattleScreen {
    GamePanel gp;
    Sprite backgroundSprite;
    Sprite nameBannerSprite;
    List<Player> party;
    Enemy enemy;
    int currentPlayerIndex = 0;
    List<Skill> skills;
    int selectedSkill = 0;
    boolean waitingForInput = true;
    String lastAction = "";
    final Map<String, Sprite> backSpriteCache = new HashMap<>();

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

        double backgroundFps = 8.0;
        try {
            backgroundSprite = SpriteLoader.loadSheet("resources/battlebg/bg1.png", 640, 360, 8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        nameBannerSprite = loadSprite("resources/sprites/bluu_back.png", 8, 1, 8);

        skills = Arrays.asList(
                new Skill("Strike", 1, 6, "Basic physical attack"),
                new Skill("Power Attack", 5, 10, "Strong physical attack"),
                new Skill("Guard", 1, 0, "Reduce incoming damage")
        );
    }
    private Sprite loadSprite(String path, int frameW, int framH, int fps) {
        try {
            return SpriteLoader.loadSheet(path, frameW, framH, fps);


        } catch (IOException e) {
            System.err.println("Failed to load sprite at " + path + ": " + e.getMessage());
            return null;
        }
    }
    void startBattle(List<Player> party, Enemy enemy) {
        this.party = new ArrayList<>(party);
        this.enemy = enemy;
        this.currentPlayerIndex = 0;
        this.waitingForInput = true;
        this.selectedSkill = 0;
        this.lastAction = "Battle started vs " + enemy.name + "!";

        // Reset enemy HP for new battle
        enemy.hp = enemy.maxHp;

        System.out.println("Battle started vs " + enemy.name);
    }

    void update(double dt) {
        InputManager input = gp.input;
        // Quick exit
        if (input.consumeIfPressed("ESC")) {
            gp.returnToWorld();
            return;
        }

        if (backgroundSprite != null) {
            backgroundSprite.update(dt);
        }
        if (nameBannerSprite != null) {
            nameBannerSprite.update(dt);
        }
        for (Sprite sprite : backSpriteCache.values()) {
            if (sprite != null) {
                sprite.update(dt);
            }
        }

        if (party == null || party.isEmpty() || enemy == null) return;
        // Check win/lose conditions
        if (enemy.hp <= 0) {
            lastAction = "Victory! " + enemy.name + " defeated!";
            if (input.consumeIfPressed("ENTER") || input.consumeIfPressed("ESC")) {
                gp.returnToWorld();
            }
            return;
        }

        boolean anyAlive = party.stream().anyMatch(p -> p.hp > 0);
        if (!anyAlive) {
            lastAction = "Defeat! All party members are down!";
            if (input.consumeIfPressed("ENTER") || input.consumeIfPressed("ESC")) {
                gp.returnToWorld();
            }
            return;
        }

        // Player turn
        if (currentPlayerIndex < party.size() && waitingForInput) {
            Player currentPlayer = party.get(currentPlayerIndex);
            if (currentPlayer.hp <= 0) {
                // Skip knocked out players
                nextTurn();
                return;
            }

            // Skill selection
            if (input.consumeIfPressed("UP")) {
                selectedSkill = (selectedSkill - 1 + skills.size()) % skills.size();
            }
            if (input.consumeIfPressed("DOWN")) {
                selectedSkill = (selectedSkill + 1) % skills.size();
            }

            // Quick skill selection
            if (input.consumeIfPressed("1")) selectedSkill = 0;
            if (input.consumeIfPressed("2")) selectedSkill = 1;
            if (input.consumeIfPressed("3")) selectedSkill = 2;

            // Execute skill
            if (input.consumeIfPressed("ENTER")) {
                Skill skill = skills.get(selectedSkill);
                performPlayerSkill(currentPlayer, skill);
                nextTurn();
            }
        }
    }

    void nextTurn() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= party.size()) {
            // All players have acted, enemy turn
            performEnemyTurn();
            currentPlayerIndex = 0;
        }
        waitingForInput = true;
    }

    void performPlayerSkill(Player player, Skill skill) {
        if ("Guard".equals(skill.name)) {
            // Defensive action
            player.def += 2; // Temporary defense boost
            lastAction = player.name + " guards defensively!";
        } else {
            // Attack action
            int damage = Math.max(1, player.str + skill.power - enemy.def);
            enemy.hp = Math.max(0, enemy.hp - damage);
            lastAction = player.name + " used " + skill.name + " for " + damage + " damage!";
        }

        System.out.println(lastAction + " (Enemy HP: " + enemy.hp + "/" + enemy.maxHp + ")");
        waitingForInput = false;
    }

    void performEnemyTurn() {
        // Find a living target
        List<Player> aliveParty = party.stream()
                .filter(p -> p.hp > 0)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (aliveParty.isEmpty()) return;

        Player target = aliveParty.get((int)(Math.random() * aliveParty.size()));
        int damage = Math.max(1, enemy.str - target.def);
        target.hp = Math.max(0, target.hp - damage);

        lastAction = enemy.name + " attacks " + target.name + " for " + damage + " damage!";
        if (target.hp <= 0) {
            lastAction += " " + target.name + " is knocked out!";
        }

        // Reset temporary defense boosts
        for (Player p : party) {
            if (p.def > (p.level + 2)) { // Original defense + guard bonus
                p.def = Math.max(2, p.level + 2);
            }
        }

        System.out.println(lastAction);
    }


    void draw(Graphics2D g) {
        // Background
        g.setColor(new Color(6, 6, 30));
        g.fillRect(0, 0, gp.vw, gp.vh);
        backgroundSprite.draw(g,0,0,640,360);
        Image image;
        try {
            image = ImageIO.read(new File("resources/sprites/Alice.png"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        g.drawImage(image,0, 0 , null);

        // Battle title
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Battle vs: " + (enemy != null ? enemy.name : "Unknown"), 24, 40);

        // Draw enemy
        if (enemy != null) {
            // Position enemy in center-right
            int enemyX = gp.vw - 200;
            int enemyY = 100;
            enemy.setPrecisePosition(enemyX, enemyY);

            // Create a simple camera for enemy rendering
            Camera battleCam = new Camera(gp.vw, gp.vh, gp.map);
            battleCam.setCenter(gp.vw / 2.0, gp.vh / 2.0);

            enemy.draw(g, battleCam);

            // Enemy HP bar
            int barW = 100, barH = 8;
            int barX = enemyX, barY = enemyY - 20;

            g.setColor(Color.RED);
            g.fillRect(barX, barY, barW, barH);
            g.setColor(Color.GREEN);
            int hpWidth = (int) (barW * enemy.hp / (double) enemy.maxHp);
            g.fillRect(barX, barY, hpWidth, barH);
            g.setColor(Color.WHITE);
            g.drawRect(barX, barY, barW, barH);

            // Enemy HP text
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.drawString(enemy.hp + "/" + enemy.maxHp, barX, barY - 4);
        }

        // Party status (left side)
        int partyX = 24, partyY = 100;
        int lineHeight = 20;

        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        for (int i = 0; i < party.size(); i++) {
            Player p = party.get(i);
            g.setColor(p.hp > 0 ? Color.WHITE : Color.DARK_GRAY);
            g.drawString(p.name + "  HP:" + p.hp + "/" + p.maxHp, partyX, partyY + i * lineHeight);
        }

        // Player selection panel (bottom)
        int panelH = 120;
        Sprite backSprite = null;
        int spriteAnchorX = 40;
        int spriteBaseY = gp.vh;
        int spriteBoxW = 384;
        int spriteBoxH = 250;

        if (party != null && !party.isEmpty() && currentPlayerIndex >= 0 && currentPlayerIndex < party.size()) {
            backSprite = getBackSpriteFor(party.get(currentPlayerIndex));
        }

        if (backSprite != null) {
            int frameW = Math.max(1, backSprite.getFrameWidth());
            int frameH = Math.max(1, backSprite.getFrameHeight());
            double scale = Math.min(1.0, Math.min(spriteBoxW / (double) frameW, spriteBoxH / (double) frameH));
            int drawW = Math.max(1, (int) Math.round(frameW * scale));
            int drawH = Math.max(1, (int) Math.round(frameH * scale));
            int drawX = spriteAnchorX + 30;
            int drawY = spriteBaseY - drawH;
            backSprite.draw(g, drawX, drawY, drawW, drawH);
        }

        int panelY = gp.vh - panelH - 40;
        if (panelY < 20) {
            panelY = Math.max(10, gp.vh - panelH - 20);
        }
        int panelX = Math.min(Math.max(260, spriteAnchorX + spriteBoxW + 40), Math.max(20, gp.vw - 360));
        int panelW = Math.max(280, gp.vw - panelX - 20);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 10, 10);

        int skillTextX = panelX + 20;
        String bannerName = null;
        if (waitingForInput && currentPlayerIndex >= 0 && currentPlayerIndex < party.size()) {
            Player active = party.get(currentPlayerIndex);
            if (active != null) {
                bannerName = active.name;
            }
        }

        int skillY = panelY + 32;
        if (bannerName != null && nameBannerSprite != null) {
            int bannerW = Math.max(1, nameBannerSprite.getFrameWidth());
            int bannerH = Math.max(1, nameBannerSprite.getFrameHeight());
            int bannerX = panelX + Math.max(10, (panelW - bannerW) / 2);
            int bannerY = skillY - bannerH - 12;
            nameBannerSprite.draw(g, bannerX, bannerY, bannerW, bannerH);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Monospaced", Font.PLAIN, 16));
            FontMetrics fmBanner = g.getFontMetrics();
            int textX = bannerX + (bannerW - fmBanner.stringWidth(bannerName)) / 2;
            int textY = bannerY + (bannerH + fmBanner.getAscent()) / 2 - 4;
            g.drawString(bannerName, textX, textY);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        int skillHeaderX = Math.max(skillTextX - 10, panelX + 10);
        g.drawString("Skills (Use 1/2/3 or Up/Down + Enter):", skillHeaderX, skillY);

        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            int y = skillY + 20 + i * 20;

            Color skillColor = (i == selectedSkill) ? Color.YELLOW : Color.WHITE;
            g.setColor(skillColor);

            String skillText = String.format("%d. %s (Cost: %d) - %s",
                    i + 1, skill.name, skill.cost, skill.description);
            g.drawString((i == selectedSkill ? "> " : "  ") + skillText, skillTextX, y);
        }

        // Action log
        g.setColor(Color.CYAN);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("Last Action: " + lastAction, 30, gp.vh - 10);

        // Instructions
        g.setColor(Color.GRAY);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("ESC: Flee | Enter: Use Skill", gp.vw - 200, gp.vh - 10);
    }

    private Sprite getBackSpriteFor(Player player) {
        if (player == null || player.name == null) {
            return null;
        }
        String normalized = player.name.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        Sprite sprite = backSpriteCache.get(normalized);
        if (sprite == null) {
            sprite = loadBackSprite(normalized);
            backSpriteCache.put(normalized, sprite);
        }
        return sprite;
    }

    private Sprite loadBackSprite(String nameKey) {
        if (nameKey == null || nameKey.isEmpty()) {
            return null;
        }
        String pathStr = "resources/sprites/" + nameKey + "_back.png";
        Sprite sprite = loadSprite(pathStr, 274, 326, 8);
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

