class FastTravelPoint extends WorldObject {
    private final String pointId;
    private final String displayName;
    private final int unlockCost;
    private final int travelCost;
    private boolean unlocked;

    FastTravelPoint(String id,
                    String pointId,
                    String displayName,
                    double x,
                    double y,
                    int width,
                    int height,
                    Sprite sprite,
                    int unlockCost,
                    int travelCost) {
        super(id, "fast_travel", x, y, width, height, sprite);
        this.pointId = pointId;
        this.displayName = displayName;
        this.unlockCost = Math.max(0, unlockCost);
        this.travelCost = Math.max(0, travelCost);
        this.unlocked = false;
        setPrompt("Activate Waypoint");
        setInteractionPriority(2);
    }

    String getPointId() {
        return pointId;
    }

    String getDisplayName() {
        return displayName;
    }

    boolean isUnlocked() {
        return unlocked;
    }

    void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
        if (unlocked) {
            setPrompt("Use Waypoint");
        }
    }

    int getTravelCost() {
        return travelCost;
    }

    int getUnlockCost() {
        return unlockCost;
    }

    @Override
    protected void onInteract(InteractionContext context) {
        if (!unlocked) {
            if (unlockCost > 0) {
                if (!context.spendGold(unlockCost)) {
                    context.queueMessage("You need " + unlockCost + " gold to attune this waypoint.");
                    return;
                }
                context.queueMessage("Paid " + unlockCost + " gold to attune the waypoint.");
            }
            unlocked = true;
            setPrompt("Use Waypoint");
            context.queueMessage("Waypoint " + displayName + " is now accessible.");
            context.unlockFastTravel(pointId);
            return;
        }
        context.openFastTravel(this);
    }
}
