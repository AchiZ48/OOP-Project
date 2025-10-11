import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

class PlayerSkills implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Integer> levels = new HashMap<>();
    private final EnumMap<Stats.StatType, Integer> statUpgrades = new EnumMap<>(Stats.StatType.class);

    int getLevel(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return 1;
        }
        return Math.max(1, levels.getOrDefault(skillId, 1));
    }

    int getLevel(SkillDefinition definition) {
        return definition == null ? 1 : getLevel(definition.getId());
    }

    void ensureSkill(String skillId, int defaultLevel) {
        if (skillId == null || skillId.isEmpty()) {
            return;
        }
        int level = Math.max(1, defaultLevel);
        levels.putIfAbsent(skillId, level);
    }

    void ensureStatEntry(Stats.StatType type) {
        if (type == null) {
            return;
        }
        statUpgrades.putIfAbsent(type, 0);
    }

    int getStatUpgradeLevel(Stats.StatType type) {
        if (type == null) {
            return 0;
        }
        return Math.max(0, statUpgrades.getOrDefault(type, 0));
    }

    boolean upgradeStat(Stats.StatType type, int maxLevel) {
        if (type == null) {
            return false;
        }
        int current = getStatUpgradeLevel(type);
        if (current >= Math.max(0, maxLevel)) {
            return false;
        }
        statUpgrades.put(type, current + 1);
        return true;
    }

    boolean canUpgradeStat(Stats.StatType type, int maxLevel) {
        if (type == null) {
            return false;
        }
        return getStatUpgradeLevel(type) < Math.max(0, maxLevel);
    }

    boolean upgrade(String skillId, int maxLevel) {
        if (skillId == null || skillId.isEmpty()) {
            return false;
        }
        int current = getLevel(skillId);
        int limit = Math.max(1, maxLevel);
        if (current >= limit) {
            return false;
        }
        levels.put(skillId, current + 1);
        return true;
    }

    boolean upgrade(SkillDefinition definition) {
        return definition != null && upgrade(definition.getId(), definition.getMaxLevel());
    }

    boolean canUpgrade(String skillId, int maxLevel) {
        return getLevel(skillId) < Math.max(1, maxLevel);
    }

    boolean canUpgrade(SkillDefinition definition) {
        return definition != null && canUpgrade(definition.getId(), definition.getMaxLevel());
    }

    void setLevel(String skillId, int level, int maxLevel) {
        if (skillId == null || skillId.isEmpty()) {
            return;
        }
        int bounded = Math.min(Math.max(1, level), Math.max(1, maxLevel));
        levels.put(skillId, bounded);
    }

    Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(levels);
    }

    Map<Stats.StatType, Integer> statSnapshot() {
        return Collections.unmodifiableMap(statUpgrades);
    }
}
