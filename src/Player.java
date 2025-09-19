import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serial;

class Player extends Entity {
    @Serial
    private static final long serialVersionUID = 1L;

    enum Direction {
        DOWN(0),
        LEFT(1),
        RIGHT(2),
        UP(3);

        final int rowIndex;

        Direction(int rowIndex) {
            this.rowIndex = rowIndex;
        }
    }

    private Direction facing = Direction.DOWN;
    private static final double COLLIDER_INSET = 6.0;
    private static final double FOOT_HEIGHT = 16.0;


    public Player(String name, SpriteAnim spr, double x, double y) {
        this.name = name;
        this.sprite = spr;
        setPrecisePosition(x, y);
        this.w = 32;
        this.h = 48;
        this.maxHp = 30;
        this.hp = maxHp;
        this.level = 1;
        this.exp = 0;
        this.str = 5;
        this.def = 2;

        applyFacingToSprite();
    }

    static Player createSample(String name, double x, double y) {
        SpriteAnim spr;
        try {
            BufferedImage img = ImageIO.read(new File("resources/sprites/" + name.toLowerCase() + ".png"));
            spr = new SpriteAnim(img, 64, 96, img.getWidth()/64, 4, img.getWidth()/64);
            System.out.println("Loaded sprite for " + name);
        } catch (Exception e) {
            BufferedImage bi = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            g.setColor(Color.GREEN);
            g.drawRect(0, 0, 15, 31);
            g.dispose();

            spr = new SpriteAnim(bi, 16, 16, 4, 1, 4);
        }

        return new Player(name, spr, x, y);
    }

    public void updateWithInput(InputManager input, TileMap map, double dt) {
        double speed = 120;
        int dx = 0, dy = 0;

        if (input.isPressed("LEFT")) dx -= 1;
        if (input.isPressed("RIGHT")) dx += 1;
        if (input.isPressed("UP")) dy -= 1;
        if (input.isPressed("DOWN")) dy += 1;
        if (input.isPressed("SHIFT")) {
            speed = 180;
        }

        updateFacingFromInput(input);

        boolean isMoving = (dx != 0 || dy != 0);
        double preciseX = getPreciseX();
        double preciseY = getPreciseY();

        if (isMoving) {
            double len = Math.sqrt(dx * dx + dy * dy);
            double vx = dx / len * speed;
            double vy = dy / len * speed;

            double deltaX = vx * dt;
            double deltaY = vy * dt;

            double nextX = preciseX + deltaX;
            double nextY = preciseY + deltaY;

            if (map != null) {
                double updatedX = preciseX;
                if (canMoveTo(map, nextX, preciseY)) {
                    updatedX = nextX;
                }

                double updatedY = preciseY;
                if (canMoveTo(map, updatedX, nextY)) {
                    updatedY = nextY;
                }

                updatedX = Math.max(0.0, Math.min(map.pixelWidth - w, updatedX));
                updatedY = Math.max(0.0, Math.min(map.pixelHeight - h, updatedY));

                preciseX = updatedX;
                preciseY = updatedY;
            } else {
                preciseX = nextX;
                preciseY = nextY;
            }

            setPrecisePosition(preciseX, preciseY);
            if (sprite != null) {
                sprite.playing = true;
            }
        } else {
            if (sprite != null) {
                sprite.playing = false;
                sprite.setFrame(0);
            }
        }

        if (sprite != null) {
            sprite.update(dt);
        }
    }

    void follow(Player target, TileMap map, double dt, double spacing) {
        if (target == null) return;

        double preciseX = getPreciseX();
        double preciseY = getPreciseY();
        double targetX = target.getPreciseX();
        double targetY = target.getPreciseY();

        double dx = targetX - preciseX;
        double dy = targetY - preciseY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double followSpeed = 200;

        boolean shouldMove = distance > Math.max(8.0, spacing);
        if (shouldMove && distance > 1e-6) {
            double moveDistance = Math.min(followSpeed * dt, Math.max(0.0, distance - spacing));
            double nx = dx / distance;
            double ny = dy / distance;

            double deltaX = nx * moveDistance;
            double deltaY = ny * moveDistance;

            double nextX = preciseX + deltaX;
            double nextY = preciseY + deltaY;

            if (map != null) {
                double updatedX = preciseX;
                if (canMoveTo(map, nextX, preciseY)) {
                    updatedX = nextX;
                }

                double updatedY = preciseY;
                if (canMoveTo(map, updatedX, nextY)) {
                    updatedY = nextY;
                }

                updatedX = Math.max(0.0, Math.min(map.pixelWidth - w, updatedX));
                updatedY = Math.max(0.0, Math.min(map.pixelHeight - h, updatedY));

                preciseX = updatedX;
                preciseY = updatedY;
            } else {
                preciseX = nextX;
                preciseY = nextY;
            }

            setPrecisePosition(preciseX, preciseY);
            updateFacingFromVector(nx, ny);
            if (sprite != null) {
                sprite.playing = true;
            }
        } else if (sprite != null) {
            sprite.playing = false;
            sprite.setFrame(0);
        }

        if (sprite != null) {
            sprite.update(dt);
        }
    }

    private void updateFacingFromVector(double vx, double vy) {
        if (Math.abs(vx) < 1e-3 && Math.abs(vy) < 1e-3) return;
        Direction newFacing = facing;
        if (Math.abs(vx) > Math.abs(vy)) {
            newFacing = vx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            newFacing = vy > 0 ? Direction.DOWN : Direction.UP;
        }
        setFacing(newFacing);
    }

    private void updateFacingFromInput(InputManager input) {
        Direction newFacing = facing;
        if (input.isPressed("DOWN")) newFacing = Direction.DOWN;
        if (input.isPressed("LEFT")) newFacing = Direction.LEFT;
        if (input.isPressed("RIGHT")) newFacing = Direction.RIGHT;
        if (input.isPressed("UP")) newFacing = Direction.UP;
        setFacing(newFacing);
    }

    private void setFacing(Direction dir) {
        if (dir == null) return;
        facing = dir;
        applyFacingToSprite();
    }

    private void applyFacingToSprite() {
        if (sprite != null) {
            sprite.setRow(facing.rowIndex);
        }
    }

    private boolean canMoveTo(TileMap map, double px, double py) {
        if (map == null) {
            return true;
        }

        double left = px + COLLIDER_INSET;
        double right = px + w - 1.0 - COLLIDER_INSET;
        double bottom = py + h - 1.0;
        double top = Math.max(py, bottom - FOOT_HEIGHT);

        left = Math.max(0.0, left);
        top = Math.max(0.0, top);
        right = Math.min(map.pixelWidth - 1.0, right);
        bottom = Math.min(map.pixelHeight - 1.0, bottom);

        return !(map.isSolidAtPixel(left, top)
                || map.isSolidAtPixel(right, top)
                || map.isSolidAtPixel(left, bottom)
                || map.isSolidAtPixel(right, bottom));
    }

    @Override
    void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }

    public double getX() {
        return getPreciseX();
    }

    public double getY() {
        return getPreciseY();
    }
}
