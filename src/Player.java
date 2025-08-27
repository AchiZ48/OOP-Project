import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

class Player extends Entity {
    private static final long serialVersionUID = 1L;

    public Player(String name, SpriteAnim spr, int x, int y) {
        this.name = name;
        this.sprite = spr;
        this.x = x;
        this.y = y;
        this.w = spr.frameW * spr.scale;
        this.h = spr.frameH * spr.scale;
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
            spr = new SpriteAnim(img, 16, 16, 4, 1, 4);
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

            // Update position
            int newX = x + (int) Math.round(vx * dt);
            int newY = y + (int) Math.round(vy * dt);

            // Bounds checking
            if (map != null) {
                newX = Math.max(0, Math.min(map.pixelWidth - w, newX));
                newY = Math.max(0, Math.min(map.pixelHeight - h, newY));
            }

            x = newX;
            y = newY;
            sprite.playing = true;
        } else {
            sprite.playing = false;
        }

        sprite.update(dt);
    }

    @Override
    void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }
}
