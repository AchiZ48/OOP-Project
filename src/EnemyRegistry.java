//
//import java.util.*;
//public final class EnemyRegistry {
//    private static final Map<String, Enemy> TEMPLATES = new HashMap<>();
//    public static void register(Enemy tpl){ if (tpl!=null && tpl.id!=null) TEMPLATES.put(tpl.id, tpl); }
//    public static Enemy template(String id){ return TEMPLATES.get(id); }
//    public static Collection<Enemy> all(){ return TEMPLATES.values(); }
//
//    // Seed some defaults if empty (non-IO to keep it simple)
//    public static void seedDefaultsIfEmpty(){
//        if (!TEMPLATES.isEmpty()) return;
//        Enemy slime = new Enemy(); slime.id="slime_green"; slime.name="Green Slime"; slime.baseHp=24; slime.baseStr=6; slime.baseDef=2; slime.maxHp=slime.hp=24; slime.str=6; slime.def=2;
//        Enemy wolf  = new Enemy(); wolf.id="wolf"; wolf.name="Wolf"; wolf.baseHp=32; wolf.baseStr=8; wolf.baseDef=3; wolf.maxHp=wolf.hp=32; wolf.str=8; wolf.def=3;
//        Enemy bandit= new Enemy(); bandit.id="bandit"; bandit.name="Bandit"; bandit.baseHp=40; bandit.baseStr=10; bandit.baseDef=4; bandit.maxHp=bandit.hp=40; bandit.str=10; bandit.def=4;
//        register(slime); register(wolf); register(bandit);
//    }
//}
