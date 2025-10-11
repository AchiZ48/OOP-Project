import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StatUpgradeCatalog {
    private static final List<StatUpgradeDefinition> DEFINITIONS = createDefinitions();

    private StatUpgradeCatalog() {
    }

    private static List<StatUpgradeDefinition> createDefinitions() {
        List<StatUpgradeDefinition> defs = new ArrayList<>();
        defs.add(new StatUpgradeDefinition(
                Stats.StatType.MAX_HP,
                "Vitality",
                "Total HP bonus +%d",
                12,
                10,
                18,
                6
        ));
        defs.add(new StatUpgradeDefinition(
                Stats.StatType.STRENGTH,
                "Strength",
                "Melee damage bonus +%d",
                1,
                8,
                14,
                6
        ));
        defs.add(new StatUpgradeDefinition(
                Stats.StatType.DEFENSE,
                "Defense",
                "Damage reduction bonus +%d",
                1,
                8,
                14,
                6
        ));
        defs.add(new StatUpgradeDefinition(
                Stats.StatType.ARCANE,
                "Arcane",
                "Mystic damage bonus +%d",
                1,
                8,
                14,
                6
        ));
        return Collections.unmodifiableList(defs);
    }

    static List<StatUpgradeDefinition> all() {
        return DEFINITIONS;
    }
}
