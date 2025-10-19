import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Enemy extends Entity {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_SCALE = 3;
    // --- added for dynamic encounters (non-breaking) ---
    public String id;

    public Enemy() {
        this.stats = Stats.createDefault();
    }

    public Enemy(String name) {
        this.name = name;
        this.stats = Stats.createDefault();
    }

    public Enemy(String name, double x, double y) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.stats = Stats.createDefault();
    }

    static Enemy createSample(String name) {
        Sprite spr;
        try {
            BufferedImage img = ResourceLoader.loadImage("resources/sprites/" + name.toLowerCase(Locale.ROOT) + ".png");
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

        Enemy enemy = new Enemy(name);
        Stats stats = enemy.getStats();
        switch (name.toLowerCase(Locale.ROOT)) {
            case "slime":
                stats.setBaseValue(Stats.StatType.ARCANE, 7);
                stats.setBaseValue(Stats.StatType.SPEED, 6);
                break;
            case "souri":
                stats.setBaseValue(Stats.StatType.SPEED, 8);
                stats.setBaseValue(Stats.StatType.LUCK, 6);
                stats.setBaseValue(Stats.StatType.DEFENSE, 4);
                break;
            case "bob":
                stats.setBaseValue(Stats.StatType.STRENGTH, 7);
                stats.setBaseValue(Stats.StatType.DEFENSE, 6);
                stats.setBaseValue(Stats.StatType.MAX_HP, 36);
                break;
            default:
                break;
        }
        stats.fullHeal();
        return enemy;
    }

    private static Enemy scaledCopy(Enemy base, int level) {
        if (base == null) {
            return null;
        }
        Enemy copy = new Enemy();
        copy.id = base.id;
        copy.name = base.name;
        copy.stats.setLevel(level);

        double hpScale = 1.0 + 0.18 * (level - 1);
        double strScale = 1.0 + 0.15 * (level - 1);
        double defScale = 1.0 + 0.12 * (level - 1);

        copy.stats.setBaseValue(
                Stats.StatType.MAX_HP,
                Math.max(1, (int) Math.round(Math.max(1, base.stats.getBaseValue(Stats.StatType.MAX_HP) * hpScale)))
        );
        copy.stats.setBaseValue(
                Stats.StatType.STRENGTH,
                Math.max(1, (int) Math.round(Math.max(1, base.stats.getBaseValue(Stats.StatType.STRENGTH) * strScale)))
        );
        copy.stats.setBaseValue(
                Stats.StatType.DEFENSE,
                Math.max(0, (int) Math.round(Math.max(0, base.stats.getBaseValue(Stats.StatType.DEFENSE) * defScale)))
        );
        return copy;
    }

    Stats getStats() {
        return stats;
    }

    @Override
    void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }

    public static final class Registry {
        private static final Map<String, Enemy> TEMPLATES = new HashMap<>();

        private Registry() {
        }

        public static void register(Enemy template) {
            if (template != null && template.id != null) {
                TEMPLATES.put(template.id, template);
            }
        }

        public static Enemy template(String id) {
            return TEMPLATES.get(id);
        }

        public static Collection<Enemy> all() {
            return TEMPLATES.values();
        }

        public static void seedDefaultsIfEmpty() {
            if (!TEMPLATES.isEmpty()) {
                return;
            }
            Enemy base = new Enemy();
            base.name = "Moodeng";
            base.id = "moodeng";
            base.stats.setBaseValue(Stats.StatType.MAX_HP, 24);
            base.stats.setBaseValue(Stats.StatType.STRENGTH, 6);
            base.stats.setBaseValue(Stats.StatType.DEFENSE, 2);
            register(base);

            Enemy vegan = new Enemy();
            vegan.name = "MoodengVegan";
            vegan.id = "MoodengVegan";
            vegan.stats.setBaseValue(Stats.StatType.MAX_HP, 32);
            vegan.stats.setBaseValue(Stats.StatType.STRENGTH, 8);
            vegan.stats.setBaseValue(Stats.StatType.DEFENSE, 3);
            register(vegan);

            Enemy iced = new Enemy();
            iced.name = "IcedMoodeng";
            iced.id = "IcedMoodeng";
            iced.stats.setBaseValue(Stats.StatType.MAX_HP, 40);
            iced.stats.setBaseValue(Stats.StatType.STRENGTH, 12);
            iced.stats.setBaseValue(Stats.StatType.DEFENSE, 4);
            register(iced);

            Enemy boss = new Enemy();
            boss.name = "Boss";
            boss.id = "boss";
            boss.stats.setBaseValue(Stats.StatType.MAX_HP, 40);
            boss.stats.setBaseValue(Stats.StatType.STRENGTH, 20);
            boss.stats.setBaseValue(Stats.StatType.DEFENSE, 4);
            register(boss);
        }
    }

    public static final class PartyGenerator {
        private final Random rng = new Random();
        private final Map<Integer, List<String>> zonePools = new HashMap<>();

        public PartyGenerator() {
            Registry.seedDefaultsIfEmpty();
            zonePools.put(1, List.of("moodeng"));
            zonePools.put(2, List.of("MoodengVegan"));
            zonePools.put(4, List.of("IcedMoodeng"));
        }

        public List<Enemy> rollRandomParty(int avgPartyLevel, int zone) {
            List<String> pool = zonePools.getOrDefault(zone, zonePools.get(1));
            int count = 1 + rng.nextInt(3);
            List<Enemy> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String id = pool.get(rng.nextInt(pool.size()));
                Enemy template = Registry.template(id);
                if (template == null) {
                    continue;
                }
                int jitter = rng.nextInt(3) - 1;
                int level = Math.max(1, avgPartyLevel + jitter);
                Enemy scaled = scaledCopy(template, level);
                if (scaled != null) {
                    result.add(scaled);
                }
            }
            return result;
        }

        public List<Enemy> makeBossParty(String bossId, int bossLevel, int minions) {
            Enemy bossTemplate = Registry.template(bossId);
            if (bossTemplate == null) {
                return Collections.emptyList();
            }
            List<Enemy> result = new ArrayList<>();
            Enemy scaledBoss = scaledCopy(bossTemplate, bossLevel);
            if (scaledBoss != null) {
                result.add(scaledBoss);
            }
            for (int i = 0; i < minions; i++) {
                Enemy minionTemplate = Registry.template("moodeng");
                if (minionTemplate != null) {
                    Enemy scaled = scaledCopy(minionTemplate, Math.max(1, bossLevel - 1));
                    if (scaled != null) {
                        result.add(scaled);
                    }
                }
            }
            return result;
        }
    }
}
