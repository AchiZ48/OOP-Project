class InteractionContext {
    private final GamePanel gamePanel;
    private final Player actor;
    private final InteractionManager manager;

    InteractionContext(GamePanel gamePanel, Player actor, InteractionManager manager) {
        this.gamePanel = gamePanel;
        this.actor = actor;
        this.manager = manager;
    }

    GamePanel getGamePanel() {
        return gamePanel;
    }

    Player getActor() {
        return actor;
    }

    InteractionManager getManager() {
        return manager;
    }

    void startDialog(DialogTree tree) {
        if (gamePanel != null) {
            gamePanel.startDialog(tree, this);
        }
    }

    void addGold(int amount) {
        if (gamePanel != null) {
            gamePanel.addGold(amount);
        }
    }

    boolean spendGold(int amount) {
        return gamePanel != null && gamePanel.spendGold(amount);
    }

    int getGold() {
        return gamePanel != null ? gamePanel.getGold() : 0;
    }

    void addEssence(int amount) {
        if (gamePanel != null) {
            gamePanel.addEssence(amount);
        }
    }

    boolean spendEssence(int amount) {
        return gamePanel != null && gamePanel.spendEssence(amount);
    }

    int getEssence() {
        return gamePanel != null ? gamePanel.getEssence() : 0;
    }

    QuestManager getQuestManager() {
        return gamePanel != null ? gamePanel.getQuestManager() : null;
    }

    void unlockFastTravel(String pointId) {
        if (gamePanel != null) {
            gamePanel.unlockFastTravel(pointId);
        }
    }

    void queueMessage(String message) {
        if (gamePanel != null) {
            gamePanel.queueWorldMessage(message);
        }
    }

    void openFastTravel(FastTravelPoint point) {
        if (gamePanel != null) {
            gamePanel.openFastTravel(point);
        }
    }

    void playSfx(String sfxId) {
        if (gamePanel != null) {
            gamePanel.playSfx(sfxId);
        }
    }
}


