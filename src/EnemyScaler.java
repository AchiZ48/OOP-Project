class EnemyScaler {
    private static final double HP_PER_LEVEL = 0.20;
    private static final double STR_PER_LEVEL = 0.12;
    private static final double DEF_PER_LEVEL = 0.10;

    Enemy scale(Enemy base, int level) {
        if (base == null) {
            throw new IllegalArgumentException("Base enemy cannot be null");
        }
        int targetLevel = Math.max(1, level);
        Enemy scaled = base.copy();
        scaled.resetToBaseStats();
        scaled.level = targetLevel;

        double levelDelta = Math.max(0, targetLevel - 1);
        double hpMultiplier = 1.0 + levelDelta * HP_PER_LEVEL;
        double strMultiplier = 1.0 + levelDelta * STR_PER_LEVEL;
        double defMultiplier = 1.0 + levelDelta * DEF_PER_LEVEL;

        scaled.maxHp = (int) Math.round(scaled.baseMaxHp * hpMultiplier);
        scaled.hp = scaled.maxHp;
        scaled.str = Math.max(1, (int) Math.round(scaled.baseStr * strMultiplier));
        scaled.def = Math.max(0, (int) Math.round(Math.max(1, scaled.baseDef) * defMultiplier));
        return scaled;
    }

    Enemy scaleBoss(Enemy base, int level) {
        Enemy scaled = scale(base, level + 1);
        scaled.maxHp = (int) Math.round(scaled.maxHp * 1.5);
        scaled.hp = scaled.maxHp;
        scaled.str = (int) Math.round(scaled.str * 1.25);
        scaled.def = (int) Math.round(Math.max(1, scaled.def) * 1.2);
        return scaled;
    }
}
