import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class EnemyRegistry {
    private static final Map<String, Enemy> TEMPLATES = new HashMap<>();

    public static void register(Enemy tpl) {
        if (tpl != null && tpl.id != null) TEMPLATES.put(tpl.id, tpl);
    }

    public static Enemy template(String id) {
        return TEMPLATES.get(id);
    }

    public static Collection<Enemy> all() {
        return TEMPLATES.values();
    }

    // Seed some defaults if empty (non-IO to keep it simple)
    public static void seedDefaultsIfEmpty() {
        if (!TEMPLATES.isEmpty()) return;
        Enemy moodeng = new Enemy();
        moodeng.name = "Moodeng";
        moodeng.id = "moodeng";
        moodeng.stats.setBaseValue(Stats.StatType.MAX_HP, 24);
        moodeng.stats.setBaseValue(Stats.StatType.STRENGTH, 6);
        moodeng.stats.setBaseValue(Stats.StatType.DEFENSE, 2);

        Enemy MoodengVegan = new Enemy();
        MoodengVegan.name = "MoodengVegan";
        MoodengVegan.id = "MoodengVegan";
        MoodengVegan.stats.setBaseValue(Stats.StatType.MAX_HP, 32);
        MoodengVegan.stats.setBaseValue(Stats.StatType.STRENGTH, 8);
        MoodengVegan.stats.setBaseValue(Stats.StatType.DEFENSE, 3);

        Enemy IcedMoodeng = new Enemy();
        IcedMoodeng.name = "IcedMoodeng";
        IcedMoodeng.id = "IcedMoodeng";
        IcedMoodeng.stats.setBaseValue(Stats.StatType.MAX_HP, 40);
        IcedMoodeng.stats.setBaseValue(Stats.StatType.STRENGTH, 12);
        IcedMoodeng.stats.setBaseValue(Stats.StatType.DEFENSE, 4);

        Enemy boss = new Enemy();
        boss.name = "Boss";
        boss.id = "boss";
        boss.stats.setBaseValue(Stats.StatType.MAX_HP, 40);
        boss.stats.setBaseValue(Stats.StatType.STRENGTH, 20);
        boss.stats.setBaseValue(Stats.StatType.DEFENSE, 4);

        register(moodeng);
        register(MoodengVegan);
        register(IcedMoodeng);
        register(boss);
    }
}
