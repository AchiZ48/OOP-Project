import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

class StatsMenuController {
    private final GamePanel gamePanel;
    private boolean open;
    private int selection;

    StatsMenuController(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.open = false;
        this.selection = 0;
    }

    boolean isOpen() {
        return open;
    }

    void open() {
        List<Player> party = gamePanel.party;
        if (party == null || party.isEmpty()) {
            return;
        }
        refreshSelection();
        selection = Math.max(0, Math.min(gamePanel.activeIndex, party.size() - 1));
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
        selection = 0;
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
        refreshSelection();
        InputManager input = gamePanel.input;
        int partySize = party.size();
        int previous = selection;

        if (input.consumeIfPressed("UP") || input.consumeIfPressed("LEFT")) {
            selection = (selection - 1 + partySize) % partySize;
        }
        if (input.consumeIfPressed("DOWN") || input.consumeIfPressed("RIGHT")) {
            selection = (selection + 1) % partySize;
        }
        if (selection != previous) {
            gamePanel.playSfx("menu_move");
        }

        if (input.consumeIfPressed("ESC") || input.consumeIfPressed("C")) {
            close();
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
        refreshSelection();
        int clampedIndex = Math.max(0, Math.min(selection, party.size() - 1));
        Player selected = party.get(clampedIndex);
        if (selected == null) {
            return;
        }
        Stats stats = selected.getStats();
        if (stats == null) {
            return;
        }

        int width = Math.min(gamePanel.vw - 80, 440);
        int height = Math.min(gamePanel.vh - 80, 300);
        int x = (gamePanel.vw - width) / 2;
        int y = (gamePanel.vh - height) / 2;

        g.setColor(new Color(0, 0, 0, 220));
        g.fillRoundRect(x, y, width, height, 18, 18);
        g.setColor(new Color(120, 180, 255));
        g.drawRoundRect(x, y, width, height, 18, 18);

        Font headerFont = FontCustom.MainFont.deriveFont(Font.PLAIN, 16);
        g.setFont(headerFont);
        FontMetrics headerMetrics = g.getFontMetrics();
        int textX = x + 20;
        int headerBaseline = y + headerMetrics.getAscent() + 14;
        g.setColor(Color.WHITE);
        g.drawString("Party Status", textX, headerBaseline);

        Font itemFont = FontCustom.MainFont.deriveFont(Font.PLAIN, 16);
        g.setFont(itemFont);
        FontMetrics itemMetrics = g.getFontMetrics();
        int listTop = headerBaseline + 10;
        int listWidth = 140;
        int rowHeight = itemMetrics.getHeight() + 6;

        for (int i = 0; i < party.size(); i++) {
            int rowY = listTop + i * rowHeight;
            boolean selectedRow = i == clampedIndex;
            int baseline = rowY + itemMetrics.getAscent();
            if (selectedRow) {
                g.setColor(new Color(60, 110, 200, 170));
                g.fillRoundRect(textX - 8, rowY - 4, listWidth + 16, itemMetrics.getHeight() + 8, 10, 10);
            }
            g.setColor(selectedRow ? Color.WHITE : new Color(210, 210, 210));
            g.drawString(party.get(i).name, textX, baseline);
        }

        int panelX = textX + listWidth + 28;
        int panelWidth = width - (panelX - x) - 20;
        int panelY = listTop - 6;
        int panelHeight = height - (panelY - y) - 30;
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(panelX - 12, panelY - 10, Math.max(160, panelWidth), panelHeight, 14, 14);

        g.setColor(Color.WHITE);
        int infoBaseline = panelY + itemMetrics.getAscent();
        g.drawString(String.format("Name: %s", selected.name), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight();
        g.drawString(String.format("Level %02d", stats.getLevel()), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight();
        int expToNext = 50 + stats.getLevel() * 50;
        g.drawString(String.format("EXP %d / %d", stats.getExp(), expToNext), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight() + 6;

        g.drawString(String.format("HP %d / %d", stats.getCurrentHp(), stats.getMaxHp()), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight();
        g.drawString(String.format("BP %d / %d", stats.getCurrentBattlePoints(), stats.getMaxBattlePoints()), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight() + 6;

        g.drawString(String.format("STR %d (base %d)", stats.getTotalValue(Stats.StatType.STRENGTH), stats.getBaseValue(Stats.StatType.STRENGTH)), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight();
        g.drawString(String.format("ARC %d (base %d)", stats.getTotalValue(Stats.StatType.ARCANE), stats.getBaseValue(Stats.StatType.ARCANE)), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight();
        g.drawString(String.format("DEF %d (base %d)", stats.getTotalValue(Stats.StatType.DEFENSE), stats.getBaseValue(Stats.StatType.DEFENSE)), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight();
        g.drawString(String.format("SPD %d (base %d)", stats.getTotalValue(Stats.StatType.SPEED), stats.getBaseValue(Stats.StatType.SPEED)), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight();
        g.drawString(String.format("LCK %d (base %d)", stats.getTotalValue(Stats.StatType.LUCK), stats.getBaseValue(Stats.StatType.LUCK)), panelX, infoBaseline);
        infoBaseline += itemMetrics.getHeight();
        g.drawString(String.format("AWR %d (base %d)", stats.getTotalValue(Stats.StatType.AWARENESS), stats.getBaseValue(Stats.StatType.AWARENESS)), panelX, infoBaseline);

        g.setColor(new Color(180, 180, 180));
        g.drawString("C/ESC: Close   Up/Down: Switch", x + 20, y + height - 18);
    }

    private void refreshSelection() {
        if (gamePanel.party == null || gamePanel.party.isEmpty()) {
            selection = 0;
        } else {
            selection = Math.max(0, Math.min(selection, gamePanel.party.size() - 1));
        }
    }
}
