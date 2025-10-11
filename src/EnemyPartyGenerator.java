import java.util.*;

public final class EnemyPartyGenerator {
    private final Random rng = new Random();

    private final Map<Integer, List<String>> zonePools = new HashMap<>();

    public EnemyPartyGenerator() {
        // default pools (can be extended externally)
        EnemyRegistry.seedDefaultsIfEmpty();
        zonePools.put(1, List.of("moodeng"));
        zonePools.put(2, List.of("MoodengVegan"));
        zonePools.put(4, List.of("IcedMoodeng"));
    }

    public List<Enemy> rollRandomParty(int avgPartyLv, int zone) {
        List<String> pool = zonePools.getOrDefault(zone, zonePools.get(1));
        int count;
        count = 1 + rng.nextInt(3); // 1-3

        List<Enemy> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = pool.get(rng.nextInt(pool.size()));
            Enemy tpl = EnemyRegistry.template(id);
            if (tpl == null) continue;
            int jitter = rng.nextInt(3) - 1; // -1..+1
            int lv = Math.max(1, avgPartyLv + jitter);
            list.add(EnemyScaler.atLevel(tpl, lv));
        }
        return list;
    }

    public List<Enemy> makeBossParty(String bossId, int bossLevel, int minions) {
        Enemy bossTpl = EnemyRegistry.template(bossId);
        if (bossTpl == null) return Collections.emptyList();
        List<Enemy> res = new ArrayList<>();
        res.add(EnemyScaler.atLevel(bossTpl, bossLevel));
        for (int i = 0; i < minions; i++) {
            Enemy mTpl = EnemyRegistry.template("moodeng");
            if (mTpl != null) res.add(EnemyScaler.atLevel(mTpl, Math.max(1, bossLevel - 1)));
        }
        return res;
    }
}
