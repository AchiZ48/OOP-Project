class ChestObject extends WorldObject {
    private final int baseGold;
    private final int baseEssence;
    private final boolean grantsBossKey;

    ChestObject(String id, double x, double y, int width, int height, Sprite sprite, int baseGold, int baseEssence, boolean grantsBossKey) {
        super(id, "chest", x, y, width, height, sprite);
        this.baseGold = Math.max(0, baseGold);
        this.baseEssence = Math.max(0, baseEssence);
        this.grantsBossKey = grantsBossKey;
        setPrompt("Open Chest");
        setInteractionPriority(3);
    }

    @Override
    protected void onInteract(InteractionContext context) {
        if (isFlagSet(StateFlag.LOCKED)) {
            context.queueMessage("The chest is locked tight.");
            return;
        }
        if (isFlagSet(StateFlag.OPEN)) {
            context.queueMessage("You already emptied this chest.");
            return;
        }

        GamePanel panel = context.getGamePanel();
        int goldReward = baseGold;
        int essenceReward = baseEssence;
        if (panel != null) {
            goldReward = panel.scaleChestGold(baseGold);
            essenceReward = panel.scaleChestEssence(baseEssence);
        }

        context.playSfx("chest_open");
        if (goldReward > 0) {
            context.addGold(goldReward);
            context.queueMessage("Received " + goldReward + " gold.");
        }
        if (essenceReward > 0) {
            context.addEssence(essenceReward);
            context.queueMessage("Absorbed " + essenceReward + " essence.");
        }

        if (grantsBossKey && panel != null) {
            if (panel.addBossKey()) {
                context.queueMessage("Boss key acquired (" + panel.getBossKeys() + "/" + panel.getBossKeysRequired() + ").");
            } else {
                context.queueMessage("You already hold all boss keys.");
            }
        }

        setFlag(StateFlag.OPEN, true);
        setFlag(StateFlag.CONSUMED, true);
        setFlag(StateFlag.USABLE, false);
        setPrompt("Empty Chest");
    }
}
