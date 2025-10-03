import java.awt.*;
import java.awt.image.BufferedImage;

class Enemy extends Entity {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_SCALE = 3;

    public Enemy(String name, Sprite spr, double x, double y) {
        this.name = name;
        this.sprite = spr;
        this.w = spr != null ? spr.getFrameWidth() * DEFAULT_SCALE : 0;
        this.h = spr != null ? spr.getFrameHeight() * DEFAULT_SCALE : 0;
        this.maxHp = 200;
        this.hp = maxHp;
        this.level = 1;
        this.str = 4;
        this.def = 1;
    }

    static Enemy createSample(String name, double x, double y) {
        BufferedImage bi = new BufferedImage(16 * 4, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();

        // Create simple slime-like enemy
        for (int i = 0; i < 4; i++) {
            int frameX = i * 16;

            // Body (bouncing animation)
            int bounce = (i < 2) ? 0 : 1;
            g.setColor(Color.MAGENTA);
            g.fillOval(frameX + 2, 6 + bounce, 12, 8 - bounce);

            // Eyes
            g.setColor(Color.BLACK);
            g.fillOval(frameX + 5, 8 + bounce, 2, 2);
            g.fillOval(frameX + 9, 8 + bounce, 2, 2);

            // Highlight
            g.setColor(Color.WHITE);
            g.fillOval(frameX + 5, 8 + bounce, 1, 1);
            g.fillOval(frameX + 9, 8 + bounce, 1, 1);
        }
        g.dispose();

        Sprite spr = Sprite.fromSheet(bi, 16, 16, 4, 1, 3);
        return new Enemy(name, spr, x, y);
    }

    @Override
    void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }
}
