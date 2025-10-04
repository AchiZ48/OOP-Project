import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

final class WorldObjectFactory {
    private WorldObjectFactory() {
    }

    static ChestObject createChest(String id, double x, double y, int gold, String equipmentId) {
        return createChest(id, x, y, gold, equipmentId, null, 0);
    }

    static ChestObject createChest(String id,
                                   double x,
                                   double y,
                                   int gold,
                                   String equipmentId,
                                   String inventoryItemId,
                                   int inventoryAmount) {
        Sprite sprite = createRectSprite(32, 32, new Color(172, 116, 48));
        return new ChestObject(id, x, y, 32, 32, sprite, gold, equipmentId, inventoryItemId, inventoryAmount);
    }

    static FastTravelPoint createWaypoint(String id,
                                          String pointId,
                                          String displayName,
                                          double x,
                                          double y,
                                          int unlockCost,
                                          int travelCost) {
        Sprite sprite = createRectSprite(28, 40, new Color(80, 180, 220));
        return new FastTravelPoint(id, pointId, displayName, x, y, 28, 40, sprite, unlockCost, travelCost);
    }

    static DoorObject createDoor(String id, double x, double y, boolean locked, String requiredItemId) {
        Sprite sprite = createRectSprite(32, 48, locked ? new Color(110, 80, 40) : new Color(156, 118, 72));
        return new DoorObject(id, x, y, 32, 48, sprite, locked, requiredItemId);
    }

    static HerbPatchObject createHerbPatch(String id, double x, double y, String itemId, int amount) {
        Sprite sprite = createRectSprite(24, 24, new Color(34, 139, 34));
        return new HerbPatchObject(id, x, y, 24, 24, sprite, itemId, amount);
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
