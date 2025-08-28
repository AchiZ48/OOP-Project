import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serial;

class Player extends Entity {
    @Serial
    private static final long serialVersionUID = 1L;

    public Player(String name, SpriteAnim spr, int x, int y) {
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

    static Player createSample(String name, int x, int y) {
        SpriteAnim spr;
        try {
            BufferedImage img = ImageIO.read(new File("resources/sprites/" + name.toLowerCase() + ".png"));
            spr = new SpriteAnim(img, 16, 32, 1, 1, 4);
            System.out.println("Loaded sprite for " + name);
        } catch (Exception e) {
            // Generate procedural sprite based on name
            BufferedImage bi = new BufferedImage(16 * 4, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();

            // Create consistent color from name hash
            int hash = Math.abs(name.hashCode());
            Color baseColor = new Color((hash % 128) + 64,
                    ((hash >> 8) % 128) + 64,
                    ((hash >> 16) % 128) + 64);
            Color darkColor = baseColor.darker();

            // Draw 4 animation frames
            for (int i = 0; i < 4; i++) {
                int frameX = i * 16;

                // Body
                g.setColor(baseColor);
                g.fillOval(frameX + 4, 4, 8, 10);

                // Head
                g.setColor(baseColor.brighter());
                g.fillOval(frameX + 5, 2, 6, 6);

                // Eyes
                g.setColor(Color.BLACK);
                g.fillOval(frameX + 6, 4, 1, 1);
                g.setColor(Color.BLACK);
                g.fillOval(frameX + 9, 4, 1, 1);

                // Simple walk animation
                int legOffset = (i % 2 == 0) ? 0 : 1;
                g.setColor(darkColor);
                g.fillRect(frameX + 6, 12 + legOffset, 1, 2);
                g.fillRect(frameX + 9, 12 + (1 - legOffset), 1, 2);

                // Outline
                g.setColor(Color.BLACK);
                g.drawRect(frameX, 0, 15, 15);
            }
            g.dispose();

            spr = new SpriteAnim(bi, 16, 16, 4, 1, 4);
        }

        return new Player(name, spr, x, y);
    }

    public void updateWithInput(InputManager input, TileMap map, double dt) {
        int speed = 80;
        int dx = 0, dy = 0;

        if (input.isPressed("LEFT")) dx -= 1;
        if (input.isPressed("RIGHT")) dx += 1;
        if (input.isPressed("UP")) dy -= 1;
        if (input.isPressed("DOWN")) dy += 1;

        boolean isMoving = (dx != 0 || dy != 0);

        if (isMoving) {
            // Normalize diagonal movement
            double len = Math.sqrt(dx * dx + dy * dy);
            double vx = dx / len * speed;
            double vy = dy / len * speed;

            // Calculate movement deltas
            int deltaX = (int) Math.round(vx * dt);
            int deltaY = (int) Math.round(vy * dt);

            if (map != null) {
                // Try horizontal movement first
                int newX = x + deltaX;
                if (isCollidingAt(map, newX, y)) {
                    x = newX;
                }

                // Then try vertical movement
                int newY = y + deltaY;
                if (isCollidingAt(map, x, newY)) {
                    y = newY;
                }

                // Bounds checking (prevent going outside map)
                x = Math.max(0, Math.min(map.pixelWidth - w, x));
                y = Math.max(0, Math.min(map.pixelHeight - h, y));
            } else {
                // No map collision, just move normally
                x += deltaX;
                y += deltaY;
            }

            sprite.playing = true;
        } else {
            sprite.playing = false;
        }

        sprite.update(dt);
    }

    // Helper method to check if player would collide at given position
    private boolean isCollidingAt(TileMap map, int px, int py) {
        // Add small margin to prevent edge cases
        int margin = 1;

        // Check corners with margin
        int left = px + margin;
        int right = px + w - margin - 1;
        int top = py + margin;
        int bottom = py + h - margin - 1;

        // Make sure we don't check negative positions
        left = Math.max(0, left);
        top = Math.max(0, top);

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