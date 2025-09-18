import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serial;

class Player extends Entity {
    @Serial
    private static final long serialVersionUID = 1L;

    public Player(String name, SpriteAnim spr, double x, double y) {
        this.name = name;
        this.sprite = spr;
        this.x = x;
        this.y = y;
        this.w = 16;
        this.h = 32;
        this.maxHp = 30;
        this.hp = maxHp;
        this.level = 1;
        this.exp = 0;
        this.str = 5;
        this.def = 2;
    }

    static Player createSample(String name, double x, double y) {
        SpriteAnim spr;
        try {
            BufferedImage img = ImageIO.read(new File("resources/sprites/" + name.toLowerCase() + ".png"));
            spr = new SpriteAnim(img, 16, 32, 1, 1, 4);
            System.out.println("Loaded sprite for " + name);
        } catch (Exception e) {
            // Generate procedural sprite based on name

            BufferedImage bi = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            // Draw hitbox outline
            g.setColor(Color.GREEN);
            g.drawRect(0, 0, 15, 31);
            g.dispose();

            spr = new SpriteAnim(bi, 16, 16, 4, 1, 4);
        }

        return new Player(name, spr, x, y);
    }

    public void updateWithInput(InputManager input, TileMap map, double dt) {
        double speed = 80;
        int dx = 0, dy = 0;

        if (input.isPressed("LEFT")) dx -= 1;
        if (input.isPressed("RIGHT")) dx += 1;
        if (input.isPressed("UP")) dy -= 1;
        if (input.isPressed("DOWN")) dy += 1;
        if (input.isPressed("SHIFT")) {
            speed = 140.0;
        }

        boolean isMoving = (dx != 0 || dy != 0);

        if (isMoving) {
            // Normalize diagonal movement
            double len = Math.sqrt(dx * dx + dy * dy);
            double vx = dx / len * speed;
            double vy = dy / len * speed;

            // Calculate movement deltas
            double deltaX = vx * dt;
            double deltaY = vy * dt;


            if (map != null) {
                // Try horizontal movement first
                double newX = x + deltaX;
                if (isCollidingAt(map, newX, y)) {
                    x = newX;
                }

                // Then try vertical movement
                double newY = y + deltaY;
                if (isCollidingAt(map, x, newY)) {
                    y = newY;
                }

                // Bounds checking (prevent going outside map)
                x = Math.max(0.0, Math.min(map.pixelWidth - w, x));
                y = Math.max(0.0, Math.min(map.pixelHeight - h, y));
            } else {
                // No map collision, just move normally
                x += deltaX;
                y += deltaY;
            }
            System.out.println(x + " " + y);
            sprite.playing = true;
        } else {
            sprite.playing = false;
        }

        sprite.update(dt);
    }

    // Helper method to check if player would collide at given position
    private boolean isCollidingAt(TileMap map, double px, double py) {

        // Check
        double left = px ;
        double right = px + w;
        double top = py;
        double bottom = py + h;

        // Make sure we don't check negative positions
        left = Math.max(0.0, left);
        top = Math.max(0.0, top);

        // Check the four corners of the player hitbox
        return map.isSolidAtPixel(left, top) &&           // top-left
                map.isSolidAtPixel(right, top) &&          // top-right
                map.isSolidAtPixel(left, bottom) &&        // bottom-left
                map.isSolidAtPixel(right, bottom);         // bottom-right
    }

    @Override
    void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }
}