class ChestObject extends WorldObject {
    private final int goldReward;
    private final String equipmentId;
    private final String inventoryItemId;
    private final int inventoryAmount;

    ChestObject(String id, double x, double y, int width, int height, Sprite sprite, int goldReward, String equipmentId, String inventoryItemId, int inventoryAmount) {
        super(id, "chest", x, y, width, height, sprite);
        this.goldReward = Math.max(0, goldReward);
        this.equipmentId = equipmentId;
        this.inventoryItemId = inventoryItemId;
        this.inventoryAmount = Math.max(0, inventoryAmount);
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
        context.playSfx("chest_open");
        if (goldReward > 0) {
            context.addGold(goldReward);
            context.queueMessage("Received " + goldReward + " gold.");
        }
        if (equipmentId != null) {
            EquipmentItem item = EquipmentCatalog.create(equipmentId);
            if (item != null) {
                context.grantItemToLeader(item);
                context.queueMessage("Found " + item.getDisplayName() + "!");
            }
        }
        if (inventoryItemId != null && inventoryAmount > 0) {
            context.addInventoryItem(inventoryItemId, inventoryAmount);
            context.queueMessage("Stored " + inventoryAmount + " " + inventoryItemId.replace('_', ' ') + ".");
        }
        setFlag(StateFlag.OPEN, true);
        setFlag(StateFlag.CONSUMED, true);
        setFlag(StateFlag.USABLE, false);
        setPrompt("Empty Chest");
    }
}

