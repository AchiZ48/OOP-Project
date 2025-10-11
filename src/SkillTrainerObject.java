class SkillTrainerObject extends WorldObject {
    private final String skillStationName;

    SkillTrainerObject(String id, double x, double y, int width, int height, Sprite sprite, String displayName) {
        super(id, "skill_trainer", x, y, width, height, sprite);
        this.skillStationName = displayName != null ? displayName : "Skill Trainer";
        setPrompt("Upgrade Skills");
        setInteractionPriority(4);
    }

    String getStationName() {
        return skillStationName;
    }

    @Override
    protected void onInteract(InteractionContext context) {
        if (context == null) {
            return;
        }
        GamePanel panel = context.getGamePanel();
        Player actor = context.getActor();
        if (panel == null) {
            context.queueMessage("Nothing happens...");
            return;
        }
        panel.openSkillUpgradeMenu(actor, skillStationName);
    }
}
