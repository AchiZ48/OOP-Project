import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

final class EquipmentCatalog {
    private static final Map<String, Supplier<EquipmentItem>> REGISTRY = new HashMap<>();

    static {
        register("bluu_staff", () -> EquipmentItem.makeWeapon("bluu_staff", "Apprentice Staff", 1, 3, "Starter stave that boosts arcane power."));
        register("bluu_sigil", () -> EquipmentItem.makeAccessory("bluu_sigil", "Mana Focus", 0, 1, "Focus gem that sharpens spellcasting."));
        register("souri_blades", () -> EquipmentItem.makeWeapon("souri_blades", "Twin Blades", 3, 0, "Lightweight dual blades for quick strikes."));
        register("souri_cloak", () -> EquipmentItem.makeArmor("souri_cloak", "Scout Cloak", 2, 2, "Weathered cloak that heightens awareness."));
        register("bob_hammer", () -> EquipmentItem.makeWeapon("bob_hammer", "Bulwark Hammer", 4, 0, "Heavy hammer that guards wield with pride."));
        register("bob_plate", () -> EquipmentItem.makeArmor("bob_plate", "Bronze Plate", 4, 1, "Armor that keeps blows at bay."));
    }

    private EquipmentCatalog() {
    }

    private static void register(String id, Supplier<EquipmentItem> supplier) {
        REGISTRY.put(id, supplier);
    }

    static EquipmentItem create(String id) {
        Supplier<EquipmentItem> supplier = REGISTRY.get(id);
        return supplier != null ? supplier.get() : null;
    }

    static boolean isKnown(String id) {
        return REGISTRY.containsKey(id);
    }
}
