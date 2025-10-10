
import java.util.*;
public final class EnemyRegistry {
    private static final Map<String, Enemy> TEMPLATES = new HashMap<>();
    public static void register(Enemy tpl){ if (tpl!=null && tpl.id!=null) TEMPLATES.put(tpl.id, tpl); }
    public static Enemy template(String id){ return TEMPLATES.get(id); }
    public static Collection<Enemy> all(){ return TEMPLATES.values(); }

    // Seed some defaults if empty (non-IO to keep it simple)
    public static void seedDefaultsIfEmpty(){
        if (!TEMPLATES.isEmpty()) return;
        Enemy slime = new Enemy();
        slime.name = "GreenSlime";
        slime.id = "slime_green";
        slime.stats.setBaseValue(Stats.StatType.MAX_HP, 24);
        slime.stats.setBaseValue(Stats.StatType.STRENGTH, 6);
        slime.stats.setBaseValue(Stats.StatType.DEFENSE, 2);

        Enemy wolf = new Enemy();
        wolf.name="Wolf";
        wolf.id="wolf";
        wolf.stats.setBaseValue(Stats.StatType.MAX_HP, 32);
        wolf.stats.setBaseValue(Stats.StatType.STRENGTH, 8);
        wolf.stats.setBaseValue(Stats.StatType.DEFENSE, 3);

        Enemy bandit = new Enemy();
        bandit.name="Bandit";
        bandit.id="bandit";
        bandit.stats.setBaseValue(Stats.StatType.MAX_HP, 40);
        bandit.stats.setBaseValue(Stats.StatType.STRENGTH, 12);
        bandit.stats.setBaseValue(Stats.StatType.DEFENSE, 4);

        register(slime); register(wolf); register(bandit);
    }
}
