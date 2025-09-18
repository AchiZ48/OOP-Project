import java.awt.*;
import java.io.Serializable;

public abstract class Entity implements Serializable {
    String name;
    double x, y;
    int w, h;
    double posX, posY;
    SpriteAnim sprite;
    int hp, maxHp, level, exp, str, def;
    int id;

    abstract void update(double dt);

    void idleUpdate(double dt) {
        update(dt);
    }

    void draw(Graphics2D g, Camera cam) {
        if (sprite == null) return;
        int sx = (int) Math.round(x);
        int sy = (int) Math.round(y);
        sprite.draw(g, sx, sy, w, h);
    }
}
