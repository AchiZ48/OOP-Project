import java.util.Random;

class AmbushManager {
    private final Random random = new Random();
    private double cooldown = 0.0;
    private double lastPosX = Double.NaN;
    private double lastPosY = Double.NaN;

    boolean tryTrigger(Player leader, TileMap map, double dt) {
        if (leader == null || map == null) {
            return false;
        }
        cooldown = Math.max(0.0, cooldown - dt);
        if (cooldown > 0.0) {
            return false;
        }

        double posX = leader.getPreciseX();
        double posY = leader.getPreciseY();
        if (!hasMoved(posX, posY)) {
            return false;
        }

        if (map.zoneLayer == null) {
            cooldown = 1.0;
            lastPosX = posX;
            lastPosY = posY;
            return false;
        }

        int tileX = Math.max(0, Math.min(map.cols - 1, (int) (posX / map.tileW)));
        int tileY = Math.max(0, Math.min(map.rows - 1, (int) (posY / map.tileH)));
        int zoneId = map.zoneLayer[tileY][tileX];
        double baseChance = baseChanceForZone(zoneId);
        if (baseChance <= 0.0) {
            cooldown = 1.0;
            lastPosX = posX;
            lastPosY = posY;
            return false;
        }

        int awareness = leader.getStats().getTotalValue(Stats.StatType.AWARENESS);
        double reduction = Math.min(0.75, awareness * 0.05);
        double finalChance = baseChance * (1.0 - reduction);
        finalChance = Math.max(0.01, finalChance);

        boolean ambush = random.nextDouble() < finalChance;
        cooldown = ambush ? 6.0 : 2.0;
        lastPosX = posX;
        lastPosY = posY;
        return ambush;
    }

    void reset() {
        cooldown = 2.0;
        lastPosX = Double.NaN;
        lastPosY = Double.NaN;
    }

    private boolean hasMoved(double x, double y) {
        if (Double.isNaN(lastPosX) || Double.isNaN(lastPosY)) {
            lastPosX = x;
            lastPosY = y;
            return false;
        }
        double dx = x - lastPosX;
        double dy = y - lastPosY;
        return (dx * dx + dy * dy) > 4.0;
    }

    private double baseChanceForZone(int zoneId) {
        return switch (zoneId) {
            case 1027 -> 0.35; // ruins
            case 1028 -> 0.25; // cavern
            case 1026 -> 0.12; // forest
            default -> 0.0;
        };
    }
}
