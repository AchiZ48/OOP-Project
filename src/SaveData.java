import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

class SaveData implements Serializable {
    private static final long serialVersionUID = 2L;

    final List<PlayerData> players = new ArrayList<>();
    final List<QuestData> quests = new ArrayList<>();
    final List<WorldObjectData> worldObjects = new ArrayList<>();
    int activeIndex;
    int gold;
    int essence;
    int bossKeys;

    static SaveData capture(GamePanel gp) {
        SaveData data = new SaveData();
        if (gp.party != null) {
            for (Player player : gp.party) {
                if (player == null) {
                    continue;
                }
                PlayerData pd = new PlayerData();
                pd.name = player.name;
                pd.x = player.getPreciseX();
                pd.y = player.getPreciseY();
                Stats stats = player.getStats();
                if (stats != null) {
                    pd.stats = stats.copy();
                }
                PlayerSkills skills = player.getSkillProgression();
                if (skills != null) {
                    pd.skills = skills.copy();
                }
                data.players.add(pd);
            }
        }

        data.activeIndex = Math.max(0, Math.min(gp.activeIndex, data.players.isEmpty() ? 0 : data.players.size() - 1));
        data.gold = gp.getGold();
        data.essence = gp.getEssence();
        data.bossKeys = gp.getBossKeys();

        QuestManager questManager = gp.getQuestManager();
        if (questManager != null) {
            for (Quest quest : questManager.getQuests()) {
                if (quest == null || quest.id == null) {
                    continue;
                }
                QuestData qd = new QuestData();
                qd.id = quest.id;
                qd.name = quest.name;
                qd.description = quest.description;
                qd.rewardGold = quest.getRewardGold();
                qd.status = quest.getStatus();
                data.quests.add(qd);
            }
        }

        if (gp.worldObjectManager != null) {
            for (WorldObject object : gp.worldObjectManager.getWorldObjects()) {
                WorldObjectData wod = WorldObjectData.from(object);
                if (wod != null) {
                    data.worldObjects.add(wod);
                }
            }
        }
        return data;
    }

    static final class PlayerData implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        double x;
        double y;
        Stats stats;
        PlayerSkills skills;
    }

    static final class QuestData implements Serializable {
        private static final long serialVersionUID = 1L;
        String id;
        String name;
        String description;
        QuestStatus status;
        int rewardGold;
    }

    static final class WorldObjectData implements Serializable {
        private static final long serialVersionUID = 1L;
        String id;
        String type;
        double x;
        double y;
        int width;
        int height;
        String prompt;
        int priority;
        EnumSet<WorldObject.StateFlag> flags = EnumSet.noneOf(WorldObject.StateFlag.class);

        Integer chestGold;
        Integer chestEssence;
        Boolean chestGrantsKey;

        Boolean doorLocked;
        Boolean doorOpen;

        String pointId;
        String displayName;
        Integer unlockCost;
        Integer travelCost;
        Boolean fastTravelUnlocked;

        String stationName;

        static WorldObjectData from(WorldObject object) {
            if (object == null) {
                return null;
            }
            WorldObjectData data = new WorldObjectData();
            data.id = object.getId();
            data.type = object.getType();
            data.x = object.getX();
            data.y = object.getY();
            data.width = object.getWidth();
            data.height = object.getHeight();
            data.prompt = object.getInteractionPrompt();
            data.priority = object.getInteractionPriority();

            for (WorldObject.StateFlag flag : WorldObject.StateFlag.values()) {
                if (object.hasFlag(flag)) {
                    data.flags.add(flag);
                }
            }

            if (object instanceof ChestObject chest) {
                data.chestGold = chest.getBaseGold();
                data.chestEssence = chest.getBaseEssence();
                data.chestGrantsKey = chest.grantsBossKey();
            } else if (object instanceof DoorObject door) {
                data.doorLocked = door.isLocked();
                data.doorOpen = door.isOpen();
            } else if (object instanceof FastTravelPoint point) {
                data.pointId = point.getPointId();
                data.displayName = point.getDisplayName();
                data.unlockCost = point.getUnlockCost();
                data.travelCost = point.getTravelCost();
                data.fastTravelUnlocked = point.isUnlocked();
            } else if (object instanceof SkillTrainerObject trainer) {
                data.stationName = trainer.getStationName();
            }
            return data;
        }

        WorldObject rebuild() {
            WorldObject rebuilt = switch (type) {
                case "chest" -> WorldObjectFactory.createChest(
                        id,
                        x,
                        y,
                        chestGold != null ? chestGold : 0,
                        chestEssence != null ? chestEssence : 0,
                        chestGrantsKey != null && chestGrantsKey);
                case "door" -> WorldObjectFactory.createDoor(
                        id,
                        x,
                        y,
                        Boolean.TRUE.equals(doorLocked));
                case "fast_travel" -> WorldObjectFactory.createWaypoint(
                        id,
                        pointId != null ? pointId : id,
                        displayName != null ? displayName : id,
                        x,
                        y,
                        unlockCost != null ? unlockCost : 0,
                        travelCost != null ? travelCost : 0);
                case "skill_trainer" -> WorldObjectFactory.createSkillTrainer(
                        id,
                        x,
                        y,
                        stationName != null ? stationName : "Skill Trainer");
                default -> null;
            };
            if (rebuilt == null) {
                return null;
            }
            rebuilt.setPosition(x, y);
            rebuilt.setInteractionPriority(priority);
            for (WorldObject.StateFlag flag : WorldObject.StateFlag.values()) {
                rebuilt.setFlag(flag, flags.contains(flag));
            }

            if (rebuilt instanceof DoorObject door) {
                if (doorLocked != null) {
                    door.setLocked(doorLocked);
                }
                if (doorOpen != null) {
                    door.setOpenState(doorOpen);
                }
            } else if (rebuilt instanceof FastTravelPoint point) {
                if (fastTravelUnlocked != null) {
                    point.setUnlocked(fastTravelUnlocked);
                }
            }
            if (prompt != null) {
                rebuilt.setPrompt(prompt);
            }
            return rebuilt;
        }
    }
}
