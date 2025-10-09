import java.awt.*;
import java.awt.image.BufferedImage;

class Enemy extends Entity {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_SCALE = 3;

    int baseMaxHp;
    int baseStr;
    int baseDef;

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
        setBaseStatsFromCurrent();
    }

    private Enemy(Enemy other) {
        this.name = other.name;
        this.sprite = other.sprite != null ? other.sprite.copy() : null;
        this.w = other.w;
        this.h = other.h;
        this.x = other.x;
        this.y = other.y;
        this.posX = other.posX;
        this.posY = other.posY;
        this.maxHp = other.maxHp;
        this.hp = other.hp;
        this.level = other.level;
        this.exp = other.exp;
        this.str = other.str;
        this.def = other.def;
        this.id = other.id;
        this.baseMaxHp = other.baseMaxHp;
        this.baseStr = other.baseStr;
        this.baseDef = other.baseDef;
    }

    Enemy copy() {
        return new Enemy(this);
    }

    void setBaseStatsFromCurrent() {
        this.baseMaxHp = this.maxHp;
        this.baseStr = this.str;
        this.baseDef = this.def;
    }

    void resetToBaseStats() {
        this.maxHp = baseMaxHp;
        this.hp = maxHp;
        this.str = baseStr;
        this.def = baseDef;
    }

    void resetForBattle() {
        this.hp = maxHp;
        if (sprite != null) {
            sprite.resetFrame();
        }
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
