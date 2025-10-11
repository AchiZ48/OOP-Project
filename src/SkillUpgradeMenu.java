import java.awt.*;
import java.util.List;

class SkillUpgradeMenu {
    private static final double MESSAGE_DURATION = 2.6;

    private final GamePanel gamePanel;
    private final List<SkillDefinition> skillDefinitions;
    private final List<StatUpgradeDefinition> statDefinitions;
    private boolean open;
    private int playerIndex;
    private int skillIndex;
    private int statIndex;
    private boolean viewingStats;
    private String stationName = "Skill Trainer";
    private String feedback;
    private double feedbackTimer;

    SkillUpgradeMenu(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.skillDefinitions = SkillCatalog.all();
        this.statDefinitions = StatUpgradeCatalog.all();
    }

    boolean isOpen() {
        return open;
    }

    void open(Player actor, String sourceName) {
        List<Player> party = gamePanel.party;
        if (party == null || party.isEmpty()) {
            return;
        }
        for (Player member : party) {
            if (member != null) {
                member.initializeDefaultSkills();
            }
        }
        stationName = (sourceName != null && !sourceName.isEmpty()) ? sourceName : "Skill Trainer";
        feedback = null;
        feedbackTimer = 0.0;
        playerIndex = Math.max(0, Math.min(party.size() - 1, actor != null ? party.indexOf(actor) : 0));
        skillIndex = Math.max(0, Math.min(skillDefinitions.size() - 1, skillIndex));
        statIndex = Math.max(0, Math.min(statDefinitions.size() - 1, statIndex));
        if (!open) {
            open = true;
            gamePanel.playSfx("menu_open");
        }
    }

    void close() {
        close(false);
    }

    void close(boolean silent) {
        if (!open) {
            return;
        }
        open = false;
        if (!silent) {
            gamePanel.playSfx("menu_cancel");
        }
    }

    void reset() {
        open = false;
        playerIndex = 0;
        skillIndex = 0;
        statIndex = 0;
        viewingStats = false;
        feedback = null;
        feedbackTimer = 0.0;
    }

    void update() {
        if (!open) {
            return;
        }
        List<Player> party = gamePanel.party;
        if (party == null || party.isEmpty()) {
            close(true);
            return;
        }
        InputManager input = gamePanel.input;
        boolean moved = false;

        if (input.consumeIfPressed("ESC") || input.consumeIfPressed("C")) {
            close();
            return;
        }

        if (input.consumeIfPressed("TAB")) {
            viewingStats = !viewingStats;
            gamePanel.playSfx("menu_move");
            return;
        }

        if (input.consumeIfPressed("LEFT")) {
            playerIndex = (playerIndex - 1 + party.size()) % party.size();
            moved = true;
        }
        if (input.consumeIfPressed("RIGHT")) {
            playerIndex = (playerIndex + 1) % party.size();
            moved = true;
        }

        if (viewingStats) {
            if (input.consumeIfPressed("UP")) {
                statIndex = (statIndex - 1 + statDefinitions.size()) % statDefinitions.size();
                moved = true;
            }
            if (input.consumeIfPressed("DOWN")) {
                statIndex = (statIndex + 1) % statDefinitions.size();
                moved = true;
            }
        } else {
            if (input.consumeIfPressed("UP")) {
                skillIndex = (skillIndex - 1 + skillDefinitions.size()) % skillDefinitions.size();
                moved = true;
            }
            if (input.consumeIfPressed("DOWN")) {
                skillIndex = (skillIndex + 1) % skillDefinitions.size();
                moved = true;
            }
        }

        if (moved) {
            gamePanel.playSfx("menu_move");
        }

        if (input.consumeIfPressed("ENTER") || input.consumeIfPressed("SPACE")) {
            attemptUpgrade();
        }
    }

    void tick(double dt) {
        if (feedbackTimer > 0.0) {
            feedbackTimer = Math.max(0.0, feedbackTimer - dt);
            if (feedbackTimer == 0.0) {
                feedback = null;
            }
        }
    }

    void draw(Graphics2D g) {
        if (!open) {
            return;
        }
        List<Player> party = gamePanel.party;
        if (party == null || party.isEmpty()) {
            return;
        }
        playerIndex = Math.max(0, Math.min(playerIndex, party.size() - 1));
        skillIndex = Math.max(0, Math.min(skillIndex, skillDefinitions.size() - 1));
        Player selectedPlayer = party.get(playerIndex);
        if (selectedPlayer == null) {
            return;
        }

        int width = Math.min(gamePanel.vw - 80, 520);
        int height = Math.min(gamePanel.vh - 80, 360);
        int x = (gamePanel.vw - width) / 2;
        int y = (gamePanel.vh - height) / 2;

        g.setColor(new Color(0, 0, 0, 220));
        g.fillRoundRect(x, y, width, height, 18, 18);
        g.setColor(new Color(130, 190, 255));
        g.drawRoundRect(x, y, width, height, 18, 18);

        Font headerFont = FontCustom.MainFont.deriveFont(Font.PLAIN, 18);
        g.setFont(headerFont);
        FontMetrics headerMetrics = g.getFontMetrics();
        int headerBaseline = y + headerMetrics.getAscent() + 14;
        g.setColor(Color.WHITE);
        String modeLabel = viewingStats ? "Stat Training" : "Skill Techniques";
        g.drawString(stationName + " - " + modeLabel, x + 20, headerBaseline);

        Font infoFont = FontCustom.MainFont.deriveFont(Font.PLAIN, 14);
        g.setFont(infoFont);
        FontMetrics infoMetrics = g.getFontMetrics();
        int leftColumnWidth = 160;
        int listTop = headerBaseline + 12;
        int rowHeight = infoMetrics.getHeight() + 6;

        for (int i = 0; i < party.size(); i++) {
            Player member = party.get(i);
            if (member == null) continue;
            int rowY = listTop + i * rowHeight;
            boolean selected = i == playerIndex;
            if (selected) {
                g.setColor(new Color(55, 105, 190, 170));
                g.fillRoundRect(x + 12, rowY - 4, leftColumnWidth, infoMetrics.getHeight() + 8, 10, 10);
            }
            g.setColor(selected ? Color.WHITE : new Color(200, 200, 200));
            g.drawString(member.name, x + 20, rowY + infoMetrics.getAscent());
        }

        int skillsPanelX = x + leftColumnWidth + 36;
        int skillsPanelWidth = width - (skillsPanelX - x) - 20;
        int skillsPanelHeight = height - (listTop - y) - 28;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(skillsPanelX - 12, listTop - 10, Math.max(200, skillsPanelWidth), skillsPanelHeight, 14, 14);

        PlayerSkills skills = selectedPlayer.getSkillProgression();
        Stats stats = selectedPlayer.getStats();
        int entryHeight = infoMetrics.getHeight() + 8;

        int detailY;

        if (viewingStats) {
            StatUpgradeDefinition selectedStat = statDefinitions.get(statIndex);
            int rowY = listTop;
            for (int i = 0; i < statDefinitions.size(); i++) {
                StatUpgradeDefinition def = statDefinitions.get(i);
                int level = skills.getStatUpgradeLevel(def.getStatType());
                boolean selected = i == statIndex;
                String label = String.format("%s Lv%d/%d (+%d per level)", def.getDisplayName(), level, def.getMaxLevel(), def.getIncrement());
                if (selected) {
                    g.setColor(new Color(100, 160, 220, 190));
                    g.fillRoundRect(skillsPanelX - 8, rowY - 4, skillsPanelWidth - 16, entryHeight, 10, 10);
                }
                g.setColor(selected ? Color.WHITE : new Color(210, 210, 210));
                g.drawString(label, skillsPanelX, rowY + infoMetrics.getAscent());
                rowY += entryHeight;
            }

            detailY = listTop + statDefinitions.size() * entryHeight + 12;
            g.setColor(new Color(210, 230, 255));
            g.drawString("Essence: " + gamePanel.getEssence(), skillsPanelX, detailY);
            detailY += infoMetrics.getHeight() + 4;

            int currentLevel = skills.getStatUpgradeLevel(selectedStat.getStatType());
            int baseValue = stats != null ? stats.getBaseValue(selectedStat.getStatType()) : 0;
            int nextValue = selectedStat.canUpgrade(currentLevel)
                    ? baseValue + selectedStat.getIncrement()
                    : baseValue;

            g.setColor(Color.WHITE);
            g.drawString("Selected: " + selectedStat.getDisplayName(), skillsPanelX, detailY);
            detailY += infoMetrics.getHeight();
            g.setColor(new Color(200, 200, 200));
            g.drawString("Current base: " + baseValue, skillsPanelX, detailY);
            detailY += infoMetrics.getHeight();
            g.drawString("After upgrade: " + nextValue, skillsPanelX, detailY);
            detailY += infoMetrics.getHeight();
            int upgradeCost = selectedStat.computeUpgradeCost(currentLevel);
            g.drawString("Upgrade cost: " + (upgradeCost > 0 ? upgradeCost + " essence" : "N/A"), skillsPanelX, detailY);
            detailY += infoMetrics.getHeight();
            g.setColor(new Color(220, 220, 255));
            g.drawString(selectedStat.describe(currentLevel), skillsPanelX, detailY);
        } else {
            SkillDefinition selectedSkill = skillDefinitions.get(skillIndex);
            int rowY = listTop;
            for (int i = 0; i < skillDefinitions.size(); i++) {
                SkillDefinition def = skillDefinitions.get(i);
                int level = skills.getLevel(def);
                String label = String.format("%s Lv%d/%d (BP %d)", def.getName(), level, def.getMaxLevel(), def.getBattleCost());
                boolean selected = i == skillIndex;
                if (selected) {
                    g.setColor(new Color(80, 130, 210, 190));
                    g.fillRoundRect(skillsPanelX - 8, rowY - 4, skillsPanelWidth - 16, entryHeight, 10, 10);
                }
                g.setColor(selected ? Color.WHITE : new Color(210, 210, 210));
                g.drawString(label, skillsPanelX, rowY + infoMetrics.getAscent());
                rowY += entryHeight;
            }

            detailY = listTop + skillDefinitions.size() * entryHeight + 12;
            g.setColor(new Color(210, 230, 255));
            g.drawString("Essence: " + gamePanel.getEssence(), skillsPanelX, detailY);
            detailY += infoMetrics.getHeight() + 4;

            int currentLevel = skills.getLevel(selectedSkill);
            g.setColor(Color.WHITE);
            g.drawString("Selected: " + selectedSkill.getName(), skillsPanelX, detailY);
            detailY += infoMetrics.getHeight();
            g.setColor(new Color(200, 200, 200));
            g.drawString("Current power: " + selectedSkill.computePower(currentLevel), skillsPanelX, detailY);
            detailY += infoMetrics.getHeight();
            String nextPower = selectedSkill.canUpgrade(currentLevel)
                    ? String.valueOf(selectedSkill.computePower(currentLevel + 1))
                    : "-";
            g.drawString("Next power: " + nextPower, skillsPanelX, detailY);
            detailY += infoMetrics.getHeight();
            int upgradeCost = selectedSkill.computeUpgradeCost(currentLevel);
            g.drawString("Upgrade cost: " + (upgradeCost > 0 ? upgradeCost + " essence" : "N/A"), skillsPanelX, detailY);
            detailY += infoMetrics.getHeight();
            g.setColor(new Color(220, 220, 255));
            g.drawString(selectedSkill.describe(currentLevel), skillsPanelX, detailY);
        }

        g.setColor(new Color(180, 180, 180));
        g.drawString("Tab: Toggle Skills/Stats   Left/Right: Party   Up/Down: Select   Enter: Upgrade   Esc: Close", x + 20, y + height - 18);

        if (feedback != null && !feedback.isEmpty()) {
            double alpha = Math.min(1.0, feedbackTimer / MESSAGE_DURATION);
            int boxWidth = infoMetrics.stringWidth(feedback) + 24;
            int boxHeight = infoMetrics.getHeight() + 12;
            int boxX = x + (width - boxWidth) / 2;
            int boxY = y + height - boxHeight - 40;
            g.setColor(new Color(0, 0, 0, (int) (160 * alpha)));
            g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 12, 12);
            g.setColor(new Color(255, 255, 255, (int) (220 * alpha)));
            g.drawString(feedback, boxX + 12, boxY + boxHeight - 8);
        }
    }

    private void attemptUpgrade() {
        if (!open) {
            return;
        }
        List<Player> party = gamePanel.party;
        if (party == null || party.isEmpty()) {
            return;
        }
        Player player = party.get(playerIndex);
        PlayerSkills progress = player != null ? player.getSkillProgression() : null;
        Stats stats = player != null ? player.getStats() : null;
        if (player == null || progress == null) {
            return;
        }

        if (viewingStats) {
            StatUpgradeDefinition definition = statDefinitions.get(statIndex);
            if (definition == null) {
                return;
            }
            Stats.StatType type = definition.getStatType();
            int currentLevel = progress.getStatUpgradeLevel(type);
            if (!definition.canUpgrade(currentLevel)) {
                setFeedback(definition.getDisplayName() + " already enhanced.");
                gamePanel.playSfx("bp_fail");
                return;
            }
            int cost = definition.computeUpgradeCost(currentLevel);
            if (cost > gamePanel.getEssence() || !gamePanel.spendEssence(cost)) {
                setFeedback("Not enough essence (" + cost + " needed).");
                gamePanel.playSfx("bp_fail");
                return;
            }
            if (!progress.upgradeStat(type, definition.getMaxLevel())) {
                setFeedback("Upgrade failed.");
                return;
            }
            if (stats != null) {
                stats.setBaseValue(type, stats.getBaseValue(type) + definition.getIncrement());
            }
            setFeedback(definition.getDisplayName() + " raised to Lv" + progress.getStatUpgradeLevel(type) + "!");
            gamePanel.playSfx("dialog_select");
        } else {
            SkillDefinition definition = skillDefinitions.get(skillIndex);
            if (definition == null) {
                return;
            }
            int currentLevel = progress.getLevel(definition);
            if (!definition.canUpgrade(currentLevel)) {
                setFeedback(definition.getName() + " already mastered.");
                gamePanel.playSfx("bp_fail");
                return;
            }
            int cost = definition.computeUpgradeCost(currentLevel);
            if (cost > gamePanel.getEssence() || !gamePanel.spendEssence(cost)) {
                setFeedback("Not enough essence (" + cost + " needed).");
                gamePanel.playSfx("bp_fail");
                return;
            }
            if (!progress.upgrade(definition)) {
                setFeedback("Upgrade failed.");
                return;
            }
            player.initializeDefaultSkills();
            setFeedback(definition.getName() + " improved to Lv" + progress.getLevel(definition) + "!");
            gamePanel.playSfx("dialog_select");
        }
    }

    private void setFeedback(String message) {
        feedback = message;
        feedbackTimer = MESSAGE_DURATION;
    }
}
