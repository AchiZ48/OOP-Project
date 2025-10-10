import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

public class Enemy extends Entity {
    // --- added for dynamic encounters (non-breaking) ---
    public String id;

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_SCALE = 3;


    public Enemy(){
        this.stats = Stats.createDefault();
    }

    public Enemy(String name) {
        this.name = name;
        this.stats = Stats.createDefault();
    }
    public Enemy(String name, double x, double y) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.stats = Stats.createDefault();
    }


    Stats getStats() {
        return stats;
    }

    static Enemy createSample(String name) {
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

        Enemy enemy = new Enemy(name);
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
        return enemy;
    }



    @Override
    void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }


}
