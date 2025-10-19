import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

final class WorldObjectFactory {
    private WorldObjectFactory() {
    }

    static ChestObject createChest(String id, double x, double y, int baseGold, int baseEssence, boolean grantsBossKey) {
        Sprite spr;
        BufferedImage img = null;
        try {
            img = ResourceLoader.loadImage("resources/sprites/chest.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        spr = Sprite.fromSheet(img, 32, 32, img.getWidth() / 32, 1, img.getWidth() / 32);
        System.out.println("Loaded sprite for chest");
        return new ChestObject(id, x, y, 32, 32, spr, baseGold, baseEssence, grantsBossKey);
    }

    static FastTravelPoint createWaypoint(String id,
                                          String pointId,
                                          String displayName,
                                          double x,
                                          double y,
                                          int unlockCost,
                                          int travelCost) {

        return new FastTravelPoint(id, pointId, displayName, x, y, 0, 0, null, unlockCost, travelCost);
    }

    static DoorObject createDoor(String id, double x, double y, boolean locked) {
        Sprite sprite = createRectSprite(32, 48, locked ? new Color(110, 80, 40) : new Color(156, 118, 72));
        return new DoorObject(id, x, y, 32, 48, sprite, locked);
    }

    static SkillTrainerObject createSkillTrainer(String id, double x, double y, String displayName) {
        Sprite spr;
        BufferedImage img = null;
        try {
            img = ResourceLoader.loadImage("resources/sprites/trainer.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        spr = Sprite.fromSheet(img, 64, 96, img.getWidth() / 64, 1, img.getWidth() / 64);
        System.out.println("Loaded sprite for trainer");
        return new SkillTrainerObject(id, x, y, 32, 64, spr, displayName);
    }

    private static Sprite createRectSprite(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.setColor(color.darker());
        g.drawRect(0, 0, width - 1, height - 1);
        g.dispose();
        return Sprite.forStaticImage(image);
    }

    static final class ChestObject extends WorldObject {
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

        int getBaseGold() {
            return baseGold;
        }

        int getBaseEssence() {
            return baseEssence;
        }

        boolean grantsBossKey() {
            return grantsBossKey;
        }

        @Override
        protected void onInteract(WorldObjectManager.InteractionContext context) {
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

    static final class DoorObject extends WorldObject {
        private boolean locked;
        private boolean open;

        DoorObject(String id, double x, double y, int width, int height, Sprite sprite, boolean locked) {
            super(id, "door", x, y, width, height, sprite);
            this.locked = locked;
            this.open = false;
            if (locked) {
                setFlag(StateFlag.LOCKED, true);
            }
            setPrompt(locked ? "Locked Door" : "Open Door");
            setInteractionPriority(2);
        }

        boolean isLocked() {
            return locked;
        }

        void setLocked(boolean locked) {
            this.locked = locked;
            setFlag(StateFlag.LOCKED, locked);
            if (locked) {
                setPrompt("Locked Door");
            } else if (open) {
                setPrompt("Close Door");
            } else {
                setPrompt("Open Door");
            }
        }

        boolean isOpen() {
            return open;
        }

        void setOpenState(boolean open) {
            this.open = open;
            setFlag(StateFlag.OPEN, open);
            if (open) {
                setPrompt("Close Door");
            } else if (locked) {
                setPrompt("Locked Door");
            } else {
                setPrompt("Open Door");
            }
        }

        @Override
        protected void onInteract(WorldObjectManager.InteractionContext context) {
            if (locked) {
                context.queueMessage("The door is locked.");
                context.playSfx("door_locked");
                return;
            }
            boolean newState = !open;
            setOpenState(newState);
            context.playSfx(newState ? "door_open" : "door_close");
            context.queueMessage(newState ? "Door opened." : "Door closed.");
        }

        @Override
        void draw(Graphics2D g) {
            Color color = locked ? new Color(120, 80, 40) : new Color(160, 110, 60);
            if (open) {
                color = new Color(190, 190, 190);
            }
            g.setColor(color);
            g.fillRect((int) Math.round(x), (int) Math.round(y), width, height);
            if (!open) {
                g.setColor(Color.DARK_GRAY);
                g.drawRect((int) Math.round(x), (int) Math.round(y), width, height);
            }
        }
    }

    static final class FastTravelPoint extends WorldObject {
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
        protected void onInteract(WorldObjectManager.InteractionContext context) {
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

    static final class SkillTrainerObject extends WorldObject {
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
        protected void onInteract(WorldObjectManager.InteractionContext context) {
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
}

