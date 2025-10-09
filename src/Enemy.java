import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

public class Enemy extends Entity {
    // --- added for dynamic encounters (non-breaking) ---
    public String id;
    private final Stats stats;

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_SCALE = 3;




    public Enemy(String name, double x, double y) {
        this.name = name;
        this.stats = Stats.createDefault();
    }

    Stats getStats() {
        return stats;
    }

    static Enemy createSample(String name, double x, double y) {
        Sprite spr;
        try {
            BufferedImage img = ImageIO.read(new File("resources/sprites/" + name.toLowerCase() + ".png"));
            spr = Sprite.fromSheet(img, 64, 96, img.getWidth()/64, 4, img.getWidth()/64);
            System.out.println("Loaded sprite for " + name);
        } catch (Exception e) {
            BufferedImage bi = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            g.setColor(Color.GREEN);
            g.drawRect(0, 0, 15, 31);
            g.dispose();

            spr = Sprite.fromSheet(bi, 16, 16, 4, 1, 4);
        }

        Enemy enemy = new Enemy(name, x, y);
        Stats stats = enemy.getStats();
        switch (name.toLowerCase(Locale.ROOT)) {
            case "slime":
                stats.setBaseValue(Stats.StatType.ARCANE, 7);
                stats.setBaseValue(Stats.StatType.SPEED, 6);
                break;
            case "souri":
                stats.setBaseValue(Stats.StatType.SPEED, 8);
                stats.setBaseValue(Stats.StatType.LUCK, 6);
                stats.setBaseValue(Stats.StatType.DEFENSE, 4);
                break;
            case "bob":
                stats.setBaseValue(Stats.StatType.STRENGTH, 7);
                stats.setBaseValue(Stats.StatType.DEFENSE, 6);
                stats.setBaseValue(Stats.StatType.MAX_HP, 36);
                break;
            default:
                break;
        }
        stats.fullHeal();
        stats.fullRestoreBattlePoints();
        enemy.refreshDerivedStats();
        return enemy;
    }

    void refreshDerivedStats() {
        if (stats == null) {
            return;
        }
        maxHp = stats.getMaxHp();
        hp = stats.getCurrentHp();
        level = stats.getLevel();
        exp = stats.getExp();
        str = stats.getTotalValue(Stats.StatType.STRENGTH);
        def = stats.getTotalValue(Stats.StatType.DEFENSE);
    }

    @Override
    void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }


}
