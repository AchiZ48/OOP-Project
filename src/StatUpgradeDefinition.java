import java.io.Serializable;

class StatUpgradeDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Stats.StatType statType;
    private final String displayName;
    private final String descriptionFormat;
    private final int increment;
    private final int maxLevel;
    private final int baseCost;
    private final int costPerLevel;

    StatUpgradeDefinition(Stats.StatType statType,
                          String displayName,
                          String descriptionFormat,
                          int increment,
                          int maxLevel,
                          int baseCost,
                          int costPerLevel) {
        this.statType = statType;
        this.displayName = displayName;
        this.descriptionFormat = descriptionFormat != null ? descriptionFormat : "";
        this.increment = increment;
        this.maxLevel = Math.max(0, maxLevel);
        this.baseCost = Math.max(0, baseCost);
        this.costPerLevel = Math.max(0, costPerLevel);
    }

    Stats.StatType getStatType() {
        return statType;
    }

    String getDisplayName() {
        return displayName;
    }

    int getIncrement() {
        return increment;
    }

    int getMaxLevel() {
        return maxLevel;
    }

    boolean canUpgrade(int currentLevel) {
        return currentLevel < maxLevel;
    }

    int computeUpgradeCost(int currentLevel) {
        if (currentLevel >= maxLevel) {
            return 0;
        }
        return baseCost + costPerLevel * currentLevel;
    }

    String describe(int currentLevel) {
        if (descriptionFormat.isEmpty()) {
            return "";
        }
        int nextBonus = increment * (currentLevel + 1);
        if (descriptionFormat.contains("%d")) {
            return String.format(descriptionFormat, nextBonus);
        }
        return descriptionFormat;
    }

    String describeCurrent(int currentLevel) {
        int totalBonus = increment * currentLevel;
        if (descriptionFormat.contains("%d")) {
            return String.format(descriptionFormat, totalBonus);
        }
        return descriptionFormat;
    }
}


