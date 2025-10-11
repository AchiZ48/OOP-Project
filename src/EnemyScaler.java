public final class EnemyScaler {
    public static Enemy atLevel(Enemy base, int lv) {
        if (base == null) return null;
        Enemy e = new Enemy();
        e.id = base.id;
        e.name = base.name;
        e.stats.setLevel(lv);
        double hpS = 1.0 + 0.18 * (lv - 1);
        double strS = 1.0 + 0.15 * (lv - 1);
        double defS = 1.0 + 0.12 * (lv - 1);
        e.stats.setBaseValue(Stats.StatType.MAX_HP, Math.max(1, (int) Math.round(Math.max(1, base.stats.getBaseValue(Stats.StatType.MAX_HP) * hpS))));
        e.stats.setBaseValue(Stats.StatType.STRENGTH, Math.max(1, (int) Math.round(Math.max(1, base.stats.getBaseValue(Stats.StatType.STRENGTH) * strS))));
        e.stats.setBaseValue(Stats.StatType.DEFENSE, Math.max(0, (int) Math.round(Math.max(0, base.stats.getBaseValue(Stats.StatType.DEFENSE)) * defS)));
        return e;
    }
}
