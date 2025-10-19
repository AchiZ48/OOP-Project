import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SkillCatalog {
    private static final List<SkillDefinition> DEFINITIONS = createDefinitions();

    private SkillCatalog() {
    }

    private static List<SkillDefinition> createDefinitions() {
        List<SkillDefinition> defs = new ArrayList<>();
        defs.add(new SkillDefinition(
                "strike",
                "Strike",
                1,
                6,
                2,
                5,
                "Bonus damage +%d",
                10,
                5
        ));
        defs.add(new SkillDefinition(
                "power_attack",
                "Power Attack",
                5,
                10,
                4,
                5,
                "Heavy blow bonus +%d",
                18,
                8
        ));
        defs.add(new SkillDefinition(
                "guard",
                "Guard",
                1,
                4,
                2,
                5,
                "Defense +%d until next turn",
                12,
                6
        ));
        return Collections.unmodifiableList(defs);
    }

    static List<SkillDefinition> all() {
        return DEFINITIONS;
    }

    static SkillDefinition findById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (SkillDefinition def : DEFINITIONS) {
            if (def.getId().equals(id)) {
                return def;
            }
        }
        return null;
    }

    static final class SkillDefinition implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private final String name;
        private final int battleCost;
        private final int basePower;
        private final int powerPerLevel;
        private final int maxLevel;
        private final String descriptionFormat;
        private final int upgradeBaseCost;
        private final int upgradeCostPerLevel;

        SkillDefinition(String id,
                        String name,
                        int battleCost,
                        int basePower,
                        int powerPerLevel,
                        int maxLevel,
                        String descriptionFormat,
                        int upgradeBaseCost,
                        int upgradeCostPerLevel) {
            this.id = id;
            this.name = name;
            this.battleCost = Math.max(0, battleCost);
            this.basePower = basePower;
            this.powerPerLevel = powerPerLevel;
            this.maxLevel = Math.max(1, maxLevel);
            this.descriptionFormat = descriptionFormat != null ? descriptionFormat : "";
            this.upgradeBaseCost = Math.max(0, upgradeBaseCost);
            this.upgradeCostPerLevel = Math.max(0, upgradeCostPerLevel);
        }

        String getId() {
            return id;
        }

        String getName() {
            return name;
        }

        int getBattleCost() {
            return battleCost;
        }

        int getMaxLevel() {
            return maxLevel;
        }

        int computePower(int level) {
            int normalized = Math.max(1, level);
            return basePower + Math.max(0, normalized - 1) * powerPerLevel;
        }

        String describe(int level) {
            if (descriptionFormat.isEmpty()) {
                return "";
            }
            if (descriptionFormat.contains("%d")) {
                return String.format(descriptionFormat, computePower(level));
            }
            return descriptionFormat;
        }

        int computeUpgradeCost(int currentLevel) {
            if (currentLevel >= maxLevel) {
                return 0;
            }
            int offset = Math.max(0, currentLevel - 1);
            return upgradeBaseCost + upgradeCostPerLevel * offset;
        }

        boolean canUpgrade(int currentLevel) {
            return currentLevel < maxLevel;
        }
    }
}
