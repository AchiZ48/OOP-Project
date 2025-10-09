import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class EnemyPartyGenerator {
    private final EnemyRegister register;
    private final EnemyScaler scaler;

    EnemyPartyGenerator(EnemyRegister register, EnemyScaler scaler) {
        this.register = register;
        this.scaler = scaler;
    }

    List<Enemy> createSkirmish(String enemyId, int count, int level) {
        if (count <= 0) {
            throw new IllegalArgumentException("Enemy count must be positive");
        }
        List<String> blueprint = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            blueprint.add(enemyId);
        }
        return createPartyFromBlueprint(blueprint, level);
    }

    List<Enemy> createPartyFromBlueprint(List<String> enemyIds, int level) {
        if (enemyIds == null || enemyIds.isEmpty()) {
            throw new IllegalArgumentException("Enemy blueprint cannot be empty");
        }
        int targetLevel = Math.max(1, level);
        List<Enemy> enemies = new ArrayList<>();
        for (String id : enemyIds) {
            Enemy base = register.create(id);
            Enemy scaled = scaler.scale(base, targetLevel);
            scaled.resetForBattle();
            enemies.add(scaled);
        }
        return enemies;
    }

    List<Enemy> createBossEncounter(String bossId, int level) {
        Enemy boss = scaler.scaleBoss(register.create(bossId), Math.max(1, level));
        boss.resetForBattle();
        return Collections.singletonList(boss);
    }
}
