import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

class Stats implements Serializable {
    private static final long serialVersionUID = 1L;

    enum StatType {
        MAX_HP,
        STRENGTH,
        DEFENSE,
        ARCANE,
        SPEED,
        LUCK,
        AWARENESS
    }

    private final EnumMap<StatType, Integer> baseValues = new EnumMap<>(StatType.class);
    private final EnumMap<StatType, Integer> equipmentBonus = new EnumMap<>(StatType.class);
    private final EnumMap<StatType, Integer> temporaryBonus = new EnumMap<>(StatType.class);

    private int level = 1;
    private int exp = 0;
    private int currentHp = 1;
    private int maxBattlePoints = 10;
    private int currentBattlePoints = 10;

    static Stats createDefault() {
        Stats stats = new Stats();
        stats.setBaseValue(StatType.MAX_HP, 30);
        stats.setBaseValue(StatType.STRENGTH, 5);
        stats.setBaseValue(StatType.DEFENSE, 3);
        stats.setBaseValue(StatType.ARCANE, 3);
        stats.setBaseValue(StatType.SPEED, 6);
        stats.setBaseValue(StatType.LUCK, 4);
        stats.setBaseValue(StatType.AWARENESS, 4);
        stats.currentHp = stats.getMaxHp();
        stats.maxBattlePoints = 10;
        stats.currentBattlePoints = stats.maxBattlePoints;
        return stats;
    }

    int getLevel() {
        return level;
    }

    void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    int getExp() {
        return exp;
    }

    void setExp(int exp) {
        this.exp = Math.max(0, exp);
    }

    void gainExp(int amount) {
        if (amount <= 0) {
            return;
        }
        exp += amount;
        while (exp >= expToNextLevel()) {
            exp -= expToNextLevel();
            level++;
            onLevelUp();
        }
    }

    private int expToNextLevel() {
        return 50 + level * 50;
    }

    private void onLevelUp() {
        setBaseValue(StatType.MAX_HP, getBaseValue(StatType.MAX_HP) + 5);
        setBaseValue(StatType.STRENGTH, getBaseValue(StatType.STRENGTH) + 2);
        setBaseValue(StatType.DEFENSE, getBaseValue(StatType.DEFENSE) + 1);
        setBaseValue(StatType.ARCANE, getBaseValue(StatType.ARCANE) + 2);
        setBaseValue(StatType.SPEED, getBaseValue(StatType.SPEED) + 1);
        setBaseValue(StatType.LUCK, getBaseValue(StatType.LUCK) + 1);
        setBaseValue(StatType.AWARENESS, getBaseValue(StatType.AWARENESS) + 1);
        setMaxBattlePoints(maxBattlePoints + 2);
        heal(5);
        restoreBattlePoints(2);
    }

    void setBaseValue(StatType type, int value) {
        baseValues.put(type, Math.max(0, value));
        clampDerivedValues();
    }

    int getBaseValue(StatType type) {
        return baseValues.getOrDefault(type, 0);
    }

    int getEquipmentBonus(StatType type) {
        return equipmentBonus.getOrDefault(type, 0);
    }

    int getTemporaryBonus(StatType type) {
        return temporaryBonus.getOrDefault(type, 0);
    }

    int getTotalValue(StatType type) {
        return getBaseValue(type) + getEquipmentBonus(type) + getTemporaryBonus(type);
    }

    void applyEquipmentModifier(Map<StatType, Integer> modifiers) {
        adjustModifiers(equipmentBonus, modifiers, 1);
    }

    void removeEquipmentModifier(Map<StatType, Integer> modifiers) {
        adjustModifiers(equipmentBonus, modifiers, -1);
    }

    void applyTemporaryModifier(Map<StatType, Integer> modifiers) {
        adjustModifiers(temporaryBonus, modifiers, 1);
    }

    void clearTemporaryModifiers() {
        temporaryBonus.clear();
        clampDerivedValues();
    }

    private void adjustModifiers(EnumMap<StatType, Integer> target,
                                 Map<StatType, Integer> modifiers,
                                 int direction) {
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }
        for (Map.Entry<StatType, Integer> entry : modifiers.entrySet()) {
            StatType type = entry.getKey();
            int delta = entry.getValue() * direction;
            int updated = target.getOrDefault(type, 0) + delta;
            if (updated == 0) {
                target.remove(type);
            } else {
                target.put(type, updated);
            }
        }
        clampDerivedValues();
    }

    int getCurrentHp() {
        return currentHp;
    }

    void setCurrentHp(int hp) {
        currentHp = clamp(hp, 0, getMaxHp());
    }

    int getMaxHp() {
        return Math.max(1, getTotalValue(StatType.MAX_HP));
    }

    void heal(int amount) {
        if (amount <= 0) {
            return;
        }
        setCurrentHp(currentHp + amount);
    }

    void fullHeal() {
        currentHp = getMaxHp();
    }

    void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        setCurrentHp(currentHp - amount);
    }

    int getMaxBattlePoints() {
        return Math.max(1, maxBattlePoints);
    }

    void setMaxBattlePoints(int value) {
        maxBattlePoints = Math.max(1, value);
        currentBattlePoints = clamp(currentBattlePoints, 0, maxBattlePoints);
    }

    int getCurrentBattlePoints() {
        return currentBattlePoints;
    }

    void setCurrentBattlePoints(int value) {
        currentBattlePoints = clamp(value, 0, maxBattlePoints);
    }

    boolean spendBattlePoints(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (currentBattlePoints < amount) {
            return false;
        }
        currentBattlePoints -= amount;
        return true;
    }

    void restoreBattlePoints(int amount) {
        if (amount <= 0) {
            return;
        }
        currentBattlePoints = clamp(currentBattlePoints + amount, 0, maxBattlePoints);
    }

    void regenerateBattlePoints(double ratio) {
        if (ratio <= 0.0) {
            return;
        }
        int amount = (int) Math.ceil(maxBattlePoints * ratio);
        restoreBattlePoints(Math.max(1, amount));
    }

    void fullRestoreBattlePoints() {
        currentBattlePoints = maxBattlePoints;
    }

    void copyFrom(Stats other) {
        if (other == null) {
            return;
        }
        baseValues.clear();
        baseValues.putAll(other.baseValues);
        equipmentBonus.clear();
        equipmentBonus.putAll(other.equipmentBonus);
        temporaryBonus.clear();
        temporaryBonus.putAll(other.temporaryBonus);
        level = other.level;
        exp = other.exp;
        maxBattlePoints = other.maxBattlePoints;
        currentBattlePoints = other.currentBattlePoints;
        currentHp = other.currentHp;
        clampDerivedValues();
    }

    EnumMap<StatType, Integer> getAggregatedSnapshot() {
        EnumMap<StatType, Integer> snapshot = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            snapshot.put(type, getTotalValue(type));
        }
        return snapshot;
    }

    EnumMap<StatType, Integer> getBaseSnapshot() {
        EnumMap<StatType, Integer> snapshot = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            snapshot.put(type, getBaseValue(type));
        }
        return snapshot;
    }

    EnumMap<StatType, Integer> getEquipmentSnapshot() {
        EnumMap<StatType, Integer> snapshot = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            snapshot.put(type, getEquipmentBonus(type));
        }
        return snapshot;
    }

    Stats copy() {
        Stats copy = new Stats();
        copy.baseValues.putAll(baseValues);
        copy.equipmentBonus.putAll(equipmentBonus);
        copy.temporaryBonus.putAll(temporaryBonus);
        copy.level = level;
        copy.exp = exp;
        copy.maxBattlePoints = maxBattlePoints;
        copy.currentBattlePoints = currentBattlePoints;
        copy.currentHp = currentHp;
        return copy;
    }

    private void clampDerivedValues() {
        currentHp = clamp(currentHp, 0, getMaxHp());
        currentBattlePoints = clamp(currentBattlePoints, 0, maxBattlePoints);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}





