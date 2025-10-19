import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Locale;

class Player extends Entity {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final double COLLIDER_INSET = 6.0;
    private static final double FOOT_HEIGHT = 0;
    private final PlayerSkills skillProgression = new PlayerSkills();
    private Direction facing = Direction.DOWN;

    public Player(String name, Sprite spr, double x, double y) {
        this.name = name;
        this.sprite = spr;
        setPrecisePosition(x, y);
        this.w = 32;
        this.h = 64;
        this.stats = Stats.createDefault();

        applyFacingToSprite();
    }

    static Player createSample(String name, double x, double y) {
        Sprite spr;
        try {
            BufferedImage img = ResourceLoader.loadImage("resources/sprites/" + name.toLowerCase() + ".png");
            spr = Sprite.fromSheet(img, 64, 96, img.getWidth() / 64, 4, img.getWidth() / 64);
            System.out.println("Loaded sprite for " + name);
        } catch (Exception e) {
            BufferedImage bi = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            g.setColor(Color.GREEN);
            g.drawRect(0, 0, 15, 31);
            g.dispose();

            spr = Sprite.fromSheet(bi, 16, 16, 4, 1, 4);
        }

        Player player = new Player(name, spr, x, y);
        Stats stats = player.getStats();
        player.initializeDefaultSkills();
        switch (name.toLowerCase(Locale.ROOT)) {
            case "bluu":
                stats.setBaseValue(Stats.StatType.ARCANE, 7);
                stats.setBaseValue(Stats.StatType.SPEED, 6);
                stats.setBaseValue(Stats.StatType.MAX_HP, 50);
                break;
            case "souri":
                stats.setBaseValue(Stats.StatType.SPEED, 8);
                stats.setBaseValue(Stats.StatType.LUCK, 6);
                stats.setBaseValue(Stats.StatType.DEFENSE, 4);
                stats.setBaseValue(Stats.StatType.MAX_HP, 50);
                break;
            case "bob":
                stats.setBaseValue(Stats.StatType.STRENGTH, 7);
                stats.setBaseValue(Stats.StatType.DEFENSE, 6);
                stats.setBaseValue(Stats.StatType.MAX_HP, 50);
                break;
            default:
                break;
        }
        stats.fullHeal();
        stats.fullRestoreBattlePoints();
        return player;
    }

    Stats getStats() {
        return stats;
    }

    PlayerSkills getSkillProgression() {
        return skillProgression;
    }

    void initializeDefaultSkills() {
        skillProgression.ensureSkill("strike", 1);
        skillProgression.ensureSkill("power_attack", 1);
        skillProgression.ensureSkill("guard", 1);
        for (StatUpgradeCatalog.StatUpgradeDefinition def : StatUpgradeCatalog.all()) {
            skillProgression.ensureStatEntry(def.getStatType());
        }
    }

    void applyStats(Stats newStats) {
        if (newStats == null) {
            return;
        }
        stats.copyFrom(newStats);
    }

    void applySkillProgression(PlayerSkills savedProgression) {
        if (savedProgression == null) {
            return;
        }
        skillProgression.copyFrom(savedProgression);
    }

    public void updateWithInput(InputManager input, TileMap map, double dt) {
        double baseSpeed = 90 + stats.getTotalValue(Stats.StatType.SPEED) * 6;
        double speed = baseSpeed;
        int dx = 0, dy = 0;

        if (input.isPressed("LEFT")) dx -= 1;
        if (input.isPressed("RIGHT")) dx += 1;
        if (input.isPressed("UP")) dy -= 1;
        if (input.isPressed("DOWN")) dy += 1;
        if (input.isPressed("SHIFT")) {
            speed *= 1.4;
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
                sprite.setPlaying(true);
            }
        } else {
            if (sprite != null) {
                sprite.setPlaying(false);
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
        double followSpeed = 140 + stats.getTotalValue(Stats.StatType.SPEED) * 6;

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
                sprite.setPlaying(true);
            }
        } else if (sprite != null) {
            sprite.setPlaying(false);
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
        double footTop = Math.max(py, bottom - FOOT_HEIGHT);
        double midTop = Math.max(py, footTop - map.tileH);

        left = Math.max(0.0, left);
        right = Math.min(map.pixelWidth - 1.0, right);
        bottom = Math.min(map.pixelHeight - 1.0, bottom);
        footTop = Math.max(0.0, Math.min(bottom, footTop));
        midTop = Math.max(0.0, Math.min(bottom, midTop));

        if (map.isSolidAtPixel(left, footTop)
                || map.isSolidAtPixel(right, footTop)
                || map.isSolidAtPixel(left, bottom)
                || map.isSolidAtPixel(right, bottom)) {
            return false;
        }

        if (midTop < footTop) {
            return !map.isSolidAtPixel(left, midTop) && !map.isSolidAtPixel(right, midTop);
        }

        return true;
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
}

