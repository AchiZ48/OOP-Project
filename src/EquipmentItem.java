import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

class EquipmentItem implements Serializable {
    private static final long serialVersionUID = 1L;

    enum Slot {
        WEAPON,
        ARMOR,
        ACCESSORY
    }

    private final String id;
    private final String displayName;
    private final Slot slot;
    private final EnumMap<Stats.StatType, Integer> modifiers;
    private final String description;
    private final int value;

    EquipmentItem(String id,
                  String displayName,
                  Slot slot,
                  Map<Stats.StatType, Integer> modifiers,
                  String description,
                  int value) {
        this.id = id;
        this.displayName = displayName;
        this.slot = slot;
        this.modifiers = new EnumMap<>(Stats.StatType.class);
        if (modifiers != null) {
            this.modifiers.putAll(modifiers);
        }
        this.description = description != null ? description : "";
        this.value = Math.max(0, value);
    }

    String getId() {
        return id;
    }

    String getDisplayName() {
        return displayName;
    }

    Slot getSlot() {
        return slot;
    }

    Map<Stats.StatType, Integer> getModifiers() {
        return Collections.unmodifiableMap(modifiers);
    }

    String getDescription() {
        return description;
    }

    int getValue() {
        return value;
    }

    static EquipmentItem makeWeapon(String id,
                                    String name,
                                    int strengthBonus,
                                    int arcaneBonus,
                                    String description) {
        EnumMap<Stats.StatType, Integer> mods = new EnumMap<>(Stats.StatType.class);
        if (strengthBonus != 0) {
            mods.put(Stats.StatType.STRENGTH, strengthBonus);
        }
        if (arcaneBonus != 0) {
            mods.put(Stats.StatType.ARCANE, arcaneBonus);
        }
        return new EquipmentItem(id, name, Slot.WEAPON, mods, description, 25 + Math.abs(strengthBonus + arcaneBonus) * 10);
    }

    static EquipmentItem makeArmor(String id,
                                   String name,
                                   int hpBonus,
                                   int awarenessBonus,
                                   String description) {
        EnumMap<Stats.StatType, Integer> mods = new EnumMap<>(Stats.StatType.class);
        if (hpBonus != 0) {
            mods.put(Stats.StatType.MAX_HP, hpBonus);
        }
        if (awarenessBonus != 0) {
            mods.put(Stats.StatType.AWARENESS, awarenessBonus);
        }
        return new EquipmentItem(id, name, Slot.ARMOR, mods, description, 30 + Math.abs(hpBonus + awarenessBonus) * 8);
    }

    static EquipmentItem makeAccessory(String id,
                                       String name,
                                       int speedBonus,
                                       int luckBonus,
                                       String description) {
        EnumMap<Stats.StatType, Integer> mods = new EnumMap<>(Stats.StatType.class);
        if (speedBonus != 0) {
            mods.put(Stats.StatType.SPEED, speedBonus);
        }
        if (luckBonus != 0) {
            mods.put(Stats.StatType.LUCK, luckBonus);
        }
        return new EquipmentItem(id, name, Slot.ACCESSORY, mods, description, 20 + Math.abs(speedBonus + luckBonus) * 12);
    }
}
