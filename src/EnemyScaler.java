//
//public final class EnemyScaler {
//    public static Enemy atLevel(Enemy base, int lv){
//        if (base==null) return null;
//        Enemy e = new Enemy();
//        e.id = base.id; e.name = base.name; e.level = lv;
//        e.baseHp = base.baseHp; e.baseStr = base.baseStr; e.baseDef = base.baseDef;
//        double hpS  = 1.0 + 0.18 * (lv-1);
//        double strS = 1.0 + 0.15 * (lv-1);
//        double defS = 1.0 + 0.12 * (lv-1);
//        e.maxHp = e.hp = Math.max(1, (int)Math.round(Math.max(1, base.baseHp) * hpS));
//        e.str            = Math.max(1, (int)Math.round(Math.max(1, base.baseStr) * strS));
//        e.def            = Math.max(0, (int)Math.round(Math.max(0, base.baseDef) * defS));
//        return e;
//    }
//}
