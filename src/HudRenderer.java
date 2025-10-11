import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

class HudRenderer {
    private final GamePanel gamePanel;
    private final StatsMenuController statsMenu;

    HudRenderer(GamePanel gamePanel, StatsMenuController statsMenu) {
        this.gamePanel = gamePanel;
        this.statsMenu = statsMenu;
    }

    void draw(Graphics2D g, Interactable highlighted, boolean dialogActive, boolean fastTravelMenuOpen) {
        List<Player> party = gamePanel.party;
        if (party == null || party.isEmpty()) {
            return;
        }

        int panelW = 120;
        int panelH = 26;
        int gap = 5;
        int visibleParty = Math.min(3, party.size());

        String goldText = "Gold: " + gamePanel.getGold();
        String essenceText = "Essence: " + gamePanel.getEssence();
        String keyText = "Boss Keys: " + gamePanel.getBossKeys() + "/" + gamePanel.getBossKeysRequired();
        Player leader = party.get(gamePanel.activeIndex);



        Font currencyFont = FontCustom.MainFont.deriveFont(Font.PLAIN, 16);
        g.setFont(currencyFont);
        FontMetrics currencyMetrics = g.getFontMetrics();
        int currencyPanelWidth = Math.max(Math.max(currencyMetrics.stringWidth(goldText), currencyMetrics.stringWidth(essenceText)), currencyMetrics.stringWidth(keyText)) + 16;
        int currencyLineHeight = currencyMetrics.getHeight();
        int currencyPanelHeight = currencyLineHeight;
        int currencyX = 12;
        int currencyBottomMargin = 8;
        int currencyY = gamePanel.vh - currencyPanelHeight - currencyBottomMargin;
        g.drawString(Integer.toString(gamePanel.map.getZone((int)(leader.x / 32), (int)(leader.y / 32))), 100, 50);
        int x = 12;
        int y = currencyY - (panelH + gap) * visibleParty;
        if (y < 20) {
            y = 20;
        }
        for (int i = 0; i < visibleParty; i++) {
            Player player = party.get(i);
            int yy = y + i * (panelH + gap);
            drawStatPanel(g, x + i * gap, yy, panelW, panelH, player, i == gamePanel.activeIndex);
        }

        drawCurrencyPanel(g, currencyX, currencyY, currencyPanelWidth, currencyPanelHeight, currencyMetrics, goldText, essenceText, keyText);

        g.setColor(new Color(180, 220, 255));
        g.setFont(FontCustom.MainFont.deriveFont(Font.PLAIN, 16));
        g.drawString(String.format("Zoom: %.2fx", gamePanel.camera.getZoom()), 16, 18);

        if (gamePanel.state == GamePanel.State.WORLD) {
            drawWorldMessages(g);
        }

        if (statsMenu.isOpen()) {
            statsMenu.draw(g);
        }

        if (gamePanel.state == GamePanel.State.WORLD
                && !gamePanel.showPauseOverlay
                && !dialogActive
                && !fastTravelMenuOpen
                && !statsMenu.isOpen()) {
            drawPlacementHud(g);
            drawInteractionPrompt(g, highlighted);
        }
    }

    private void drawCurrencyPanel(Graphics2D g, int x, int y, int width, int height, FontMetrics fm, String goldText, String essenceText, String keyText) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(x, y, width, height, 10, 10);
        int textX = x + 8;
        int textY = y + fm.getAscent();
        g.setColor(new Color(255, 225, 120));
        g.drawString(goldText, textX, textY);
        textX = x + fm.stringWidth(goldText) + 6;
        g.setColor(new Color(176, 210, 255));
        g.drawString(essenceText, textX, textY);
        textY += fm.getHeight();
        Color keyColor = gamePanel.hasRequiredBossKeys() ? new Color(150, 255, 170) : new Color(255, 200, 150);
        g.setColor(keyColor);
        g.drawString(keyText, textX, textY);
        g.drawString(String.valueOf(gamePanel.getCooldown()), textX, textY + 10);
    }

    private void drawWorldMessages(Graphics2D g) {
        List<WorldMessage> messages = gamePanel.worldMessages;
        if (messages.isEmpty()) {
            return;
        }
        Font font = FontCustom.MainFont.deriveFont(Font.PLAIN, 16);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight() + 6;
        int startY = 32;
        for (int i = 0; i < messages.size(); i++) {
            WorldMessage message = messages.get(i);
            float alpha = message.alpha();
            String text = message.text();
            int textWidth = fm.stringWidth(text);
            int boxWidth = textWidth + 16;
            int x = (gamePanel.vw - boxWidth) / 2;
            int y = startY + i * lineHeight + fm.getAscent();
            int boxY = y - fm.getAscent() - 6;
            g.setColor(new Color(0, 0, 0, (int) (160 * alpha)));
            g.fillRoundRect(x, boxY, boxWidth, fm.getHeight() + 12, 10, 10);
            g.setColor(new Color(255, 255, 255, (int) (230 * alpha)));
            g.drawString(text, x + 8, y);
        }
    }

    private void drawPlacementHud(Graphics2D g) {
        if (gamePanel.worldObjectManager == null) {
            return;
        }
        PlacementManager placement = gamePanel.worldObjectManager.getPlacementManager();
        if (placement == null) {
            return;
        }
        String header = "Placement";
        String name = placement.getCurrentLabel();
        String placeInstruction = "Space: Place";
        String cycleInstruction = "Q/R: Cycle   E: Use";
        Font font = FontCustom.MainFont.deriveFont(Font.PLAIN, 8);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int width = Math.max(Math.max(fm.stringWidth(header), fm.stringWidth(name)),
                Math.max(fm.stringWidth(placeInstruction), fm.stringWidth(cycleInstruction))) + 14;
        int lineHeight = fm.getHeight();
        int height = lineHeight * 4 + 16;
        int x = gamePanel.vw - width - 16;
        int y = gamePanel.vh - height - 16;
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(x, y, width, height, 10, 10);
        int textX = x + 8;
        int textY = y + fm.getAscent() + 8;
        g.setColor(new Color(180, 220, 255));
        g.drawString(header, textX, textY);
        textY += lineHeight;
        g.setColor(Color.WHITE);
        g.drawString(name, textX, textY);
        textY += lineHeight;
        g.setColor(new Color(210, 210, 210));
        g.drawString(placeInstruction, textX, textY);
        textY += lineHeight;
        g.drawString(cycleInstruction, textX, textY);
    }

    private void drawInteractionPrompt(Graphics2D g, Interactable highlighted) {
        if (highlighted == null) {
            return;
        }
        String prompt = highlighted.getInteractionPrompt();
        if (prompt == null || prompt.isEmpty()) {
            return;
        }
        String text = "E: " + prompt;
        Font font = FontCustom.MainFont.deriveFont(Font.PLAIN, 16);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int paddingX = 12;
        int paddingY = 6;
        int width = fm.stringWidth(text) + paddingX * 2;
        int height = fm.getHeight() + paddingY * 2;
        int x = (gamePanel.vw - width) / 2;
        int y = gamePanel.vh - height - 20;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setColor(Color.WHITE);
        g.drawString(text, x + paddingX, y + height - paddingY - fm.getDescent());
    }

    private void drawStatPanel(Graphics2D g, int x, int y, int w, int h, Player player, boolean active) {
        if (player == null) {
            return;
        }

        if (active) {
            java.awt.Stroke oldStroke = g.getStroke();
            g.setStroke(new java.awt.BasicStroke(1.6f));
            g.setColor(new Color(0, 0, 0));
            g.drawRoundRect(x, y, w, h, 2, 2);
            g.setColor(new Color(13, 64, 109));
            g.fillRoundRect(x, y, w, h, 2, 2);
            g.setStroke(oldStroke);
        }
        else{
            g.setColor(new Color(22, 28, 42));
            g.fillRoundRect(x, y, w, h, 3, 3);
        }

        Stats stats = player.getStats();
        Font font = FontCustom.MainFont.deriveFont(Font.PLAIN, 8);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        int textX = x + 10;
        int textY = y + fm.getAscent()+2;
        g.setColor(Color.WHITE);
        String header = stats != null
                ? String.format("%s                     Lv%02d", player.name, stats.getLevel())
                : player.name;
        g.drawString(header, textX, textY);
        textY += 3;

        int hp;
        int maxHp;
        int bp;
        int maxBp;
        if (stats != null) {
            hp = stats.getCurrentHp();
            maxHp = stats.getMaxHp();
            bp = stats.getCurrentBattlePoints();
            maxBp = stats.getMaxBattlePoints();
        } else return;

        int barWidth = w - 20;
        String hpLabel = String.format("%d/%d", Math.max(0, hp), Math.max(1, maxHp));
        g.setColor(new Color(210, 210, 210));
//        g.drawString("HP", textX, textY);
//        g.drawString(hpLabel, x + w - 10 - fm.stringWidth(hpLabel), textY);
        drawBar(g, x + 8, textY, barWidth, 8, hp, maxHp, new Color(255, 130, 118), new Color(215, 54, 44));
        textY -= fm.getHeight() -2 ;

        String bpLabel = String.format("%d/%d", Math.max(0, bp), Math.max(0, maxBp));
        g.setColor(new Color(200, 200, 200));
//        g.drawString(bpLabel, x + w - 10 - fm.stringWidth(bpLabel), textY + 12);
        barWidth = w - 70;
        drawBar(g, x+(w/2) - barWidth/2, textY, barWidth, 6, bp, maxBp, new Color(90, 160, 255), new Color(40, 100, 210));
    }

    private void drawBar(Graphics2D g, int x, int y, int w, int h,
                         int value, int max,
                         Color cLight, Color cDark) {
        g.setColor(new Color(24, 24, 28, 200));
        g.fillRoundRect(x, y, w, h, 3, 3);
        if (max <= 0) {
            g.setColor(new Color(80, 80, 90));
            g.drawRoundRect(x, y, w, h, 3, 3);
            return;
        }
        int clampedValue = Math.max(0, Math.min(value, max));
        int fillWidth = (int) Math.round((w - 2) * (clampedValue / (double) max));
        if (fillWidth > 0) {
            java.awt.Paint previous = g.getPaint();
            g.setPaint(new java.awt.GradientPaint(x, y, cLight, x + w, y, cDark));
            g.fillRoundRect(x + 1, y + 1, Math.max(1, fillWidth), Math.max(1, h - 2), 3, 3);
            g.setPaint(previous);
        }
    }
}
