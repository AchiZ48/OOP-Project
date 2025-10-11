import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

final class WorldObjectFactory {
    private WorldObjectFactory() {
    }

    static ChestObject createChest(String id, double x, double y, int baseGold, int baseEssence, boolean grantsBossKey) {
        Sprite spr;
        BufferedImage img = null;
        try {
            img = ResourceLoader.loadImage("resources/sprites/chest.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        spr = Sprite.fromSheet(img, 32, 32, img.getWidth()/32, 1, img.getWidth()/32);
        System.out.println("Loaded sprite for chest");
        return new ChestObject(id, x, y, 32, 32, spr, baseGold, baseEssence, grantsBossKey);
    }

    static FastTravelPoint createWaypoint(String id,
                                          String pointId,
                                          String displayName,
                                          double x,
                                          double y,
                                          int unlockCost,
                                          int travelCost) {

        return new FastTravelPoint(id, pointId, displayName, x, y, 0, 0, null, unlockCost, travelCost);
    }

    static DoorObject createDoor(String id, double x, double y, boolean locked) {
        Sprite sprite = createRectSprite(32, 48, locked ? new Color(110, 80, 40) : new Color(156, 118, 72));
        return new DoorObject(id, x, y, 32, 48, sprite, locked);
    }

    static SkillTrainerObject createSkillTrainer(String id, double x, double y, String displayName) {
        Sprite spr;
        BufferedImage img = null;
        try {
            img = ResourceLoader.loadImage("resources/sprites/trainer.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        spr = Sprite.fromSheet(img, 64, 96, img.getWidth()/64, 1, img.getWidth()/64);
        System.out.println("Loaded sprite for trainer");
        return new SkillTrainerObject(id, x, y, 32, 64, spr, displayName);
    }

    private static Sprite createRectSprite(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.setColor(color.darker());
        g.drawRect(0, 0, width - 1, height - 1);
        g.dispose();
        return Sprite.forStaticImage(image);
    }
}

