//
//import java.util.*;
//
//public final class EnemyPartyGenerator {
//    public enum PackSize { SMALL, MEDIUM, LARGE }
//    private final Random rng = new Random();
//
//    private final Map<String, List<String>> zonePools = new HashMap<>();
//
//    public EnemyPartyGenerator(){
//        // default pools (can be extended externally)
//        zonePools.put("forest", Arrays.asList("slime_green","slime_green","wolf","bandit"));
//        zonePools.put("road",   Arrays.asList("bandit","bandit","slime_green"));
//        zonePools.put("cave",   Arrays.asList("wolf","wolf","bandit"));
//    }
//
//    public List<Enemy> rollRandomParty(int avgPartyLv, String zone, PackSize size){
//        List<String> pool = zonePools.getOrDefault(zone, Collections.singletonList("slime_green"));
//        int count;
//        switch(size){
//            case SMALL:  count = 1 + rng.nextInt(2); break; // 1-2
//            case MEDIUM: count = 2 + rng.nextInt(2); break; // 2-3
//            default:     count = 3 + rng.nextInt(2); break; // 3-4
//        }
//        List<Enemy> list = new ArrayList<>();
//        for (int i=0;i<count;i++){
//            String id = pool.get(rng.nextInt(pool.size()));
//            Enemy tpl = EnemyRegistry.template(id);
//            if (tpl==null) continue;
//            int jitter = rng.nextInt(3)-1; // -1..+1
//            int lv = Math.max(1, avgPartyLv + jitter);
//            list.add(EnemyScaler.atLevel(tpl, lv));
//        }
//        if (list.size()>=3 && rng.nextDouble()<0.35){
//            Enemy elite = list.get(rng.nextInt(list.size()));
//            elite.name = "Elite " + elite.name;
//            elite.maxHp = elite.hp = (int)(elite.maxHp * 1.35);
//            elite.str  = (int)(elite.str * 1.25);
//            elite.def  = (int)(elite.def * 1.20);
//        }
//        return list;
//    }
//
//    public List<Enemy> makeBossParty(String bossId, int bossLevel, int minions){
//        Enemy bossTpl = EnemyRegistry.template(bossId);
//        if (bossTpl==null) return Collections.emptyList();
//        List<Enemy> res = new ArrayList<>();
//        res.add(EnemyScaler.atLevel(bossTpl, bossLevel));
//        for (int i=0;i<minions;i++){
//            Enemy mTpl = EnemyRegistry.template("bandit");
//            if (mTpl!=null) res.add(EnemyScaler.atLevel(mTpl, Math.max(1, bossLevel-1)));
//        }
//        return res;
//    }
//}
