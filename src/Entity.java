import java.awt.*;
import java.io.Serializable;

public abstract class Entity implements Serializable {
    String name;
    int x, y, w, h;
    SpriteAnim sprite;
    int hp, maxHp, level, exp, str, def;
    int id;

    abstract void update(double dt);

    void idleUpdate(double dt) {
        update(dt);
    }

    void draw(Graphics2D g, Camera cam) {
        if (sprite == null) return;
        int sx = cam.worldToScreenX(x);
        int sy = cam.worldToScreenY(y);
        sprite.draw(g, sx, sy, w, h);
    }
}
