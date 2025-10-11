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
}
