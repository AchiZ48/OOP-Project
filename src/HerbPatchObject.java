class HerbPatchObject extends WorldObject {
    private final String itemId;
    private final int amountPerHarvest;
    private boolean harvested;

    HerbPatchObject(String id, double x, double y, int width, int height, Sprite sprite, String itemId, int amountPerHarvest) {
        super(id, "herb_patch", x, y, width, height, sprite);
        this.itemId = itemId;
        this.amountPerHarvest = Math.max(1, amountPerHarvest);
        this.harvested = false;
        setPrompt("Harvest Herb");
        setInteractionPriority(1);
    }

    @Override
    protected void onInteract(InteractionContext context) {
        if (harvested) {
            context.queueMessage("The herbs need time to regrow.");
            return;
        }
        context.playSfx("herb_pick");
        context.addInventoryItem(itemId, amountPerHarvest);
        context.queueMessage("Gathered " + amountPerHarvest + " " + itemId.replace('_', ' ') + ".");
        setHarvested(true);
    }

    String getItemId() {
        return itemId;
    }

    int getAmountPerHarvest() {
        return amountPerHarvest;
    }

    boolean isHarvested() {
        return harvested || isFlagSet(StateFlag.CONSUMED);
    }

    void setHarvested(boolean harvested) {
        this.harvested = harvested;
        setFlag(StateFlag.CONSUMED, harvested);
        setPrompt(harvested ? "Picked clean" : "Harvest Herb");
    }
}
