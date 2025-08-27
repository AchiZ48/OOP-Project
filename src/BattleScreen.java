import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BattleScreen {
    GamePanel gp;
    List<Player> party;
    Enemy enemy;
    int currentPlayerIndex = 0;
    List<Skill> skills;
    int selectedSkill = 0;
    boolean waitingForInput = true;
    String lastAction = "";

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
        skills = Arrays.asList(
                new Skill("Strike", 1, 6, "Basic physical attack"),
                new Skill("Power Attack", 2, 10, "Strong physical attack"),
                new Skill("Guard", 1, 0, "Reduce incoming damage")
        );
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

        // Battle title
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Battle vs: " + (enemy != null ? enemy.name : "Unknown"), 24, 40);

        // Draw enemy
        if (enemy != null) {
            // Position enemy in center-right
            int enemyX = gp.vw - 200;
            int enemyY = 100;
            enemy.x = enemyX;
            enemy.y = enemyY;

            // Create a simple camera for enemy rendering
            Camera battleCam = new Camera(gp.vw, gp.vh, gp.map);
            battleCam.x = gp.vw / 2.0;
            battleCam.y = gp.vh / 2.0;

            enemy.draw(g, battleCam);

            // Enemy HP bar
            int barW = 100, barH = 8;
            int barX = enemyX, barY = enemyY - 20;

            g.setColor(Color.RED);
            g.fillRect(barX, barY, barW, barH);
            g.setColor(Color.GREEN);
            int hpWidth = (int)(barW * enemy.hp / (double)enemy.maxHp);
            g.fillRect(barX, barY, hpWidth, barH);
            g.setColor(Color.WHITE);
            g.drawRect(barX, barY, barW, barH);

            // Enemy HP text
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.drawString(enemy.hp + "/" + enemy.maxHp, barX, barY - 4);
        }

        // Party status (left side)
        int partyX = 24, partyY = 100;
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        g.drawString("Party Status:", partyX, partyY);

        for (int i = 0; i < party.size(); i++) {
            Player p = party.get(i);
            int y = partyY + 20 + i * 22;

            // Highlight current player
            Color textColor = Color.WHITE;
            if (i == currentPlayerIndex && waitingForInput) {
                textColor = Color.YELLOW;
            } else if (p.hp <= 0) {
                textColor = Color.RED;
            }

            g.setColor(textColor);
            String status = p.name + " LV" + p.level + " HP:" + p.hp + "/" + p.maxHp;
            g.drawString(status, partyX, y);
        }

        // Skills panel (bottom)
        int skillY = gp.vh - 140;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(20, skillY - 20, gp.vw - 40, 120, 10, 10);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("Skills (Use 1/2/3 or Up/Down + Enter):", 30, skillY);

        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            int y = skillY + 20 + i * 20;

            Color skillColor = (i == selectedSkill) ? Color.YELLOW : Color.WHITE;
            g.setColor(skillColor);

            String skillText = String.format("%d. %s (Cost: %d) - %s",
                    i + 1, skill.name, skill.cost, skill.description);
            g.drawString((i == selectedSkill ? "> " : "  ") + skillText, 40, y);
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
}

