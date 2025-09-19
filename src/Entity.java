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


    void setPrecisePosition(double px, double py) {
        this.x = Math.floor(px);
        this.posX = px - this.x;
        this.y = Math.floor(py);
        this.posY = py - this.y;
    }


    double getPreciseX() {
        return x + posX;
    }

    double getPreciseY() {
        return y + posY;
    }


    void draw(Graphics2D g, Camera cam) {
        if (sprite == null) return;
        sprite.draw(g, x, y, w, h);
    }
}


