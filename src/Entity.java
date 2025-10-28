import java.awt.*;
import java.io.Serializable;

public abstract class Entity implements Serializable {
    public Stats stats = new Stats();
    String name;
    double x, y;
    int w, h;
    private double posX, posY;
    Sprite sprite;

    abstract void update(double dt);


    void setPrecisePosition(double px, double py) {
        this.x = Math.floor(px);
        this.posX = px - this.x;
        this.y = Math.floor(py);
        this.posY = py - this.y;
    }
    String getName() {
        return name;
    }

    double getPreciseX() {
        return x + posX;
    }

    double getPreciseY() {
        return y + posY;
    }

    int getW(){ return w;}

    int getH(){ return h;}

    void draw(Graphics2D g, Camera cam) {
        if (sprite == null) return;
        sprite.draw(g, x, y, w, h);
    }
}
