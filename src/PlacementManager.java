import java.util.EnumMap;
import java.util.Map;

class PlacementManager {
    enum PlacementType {
        CHEST,
        DOOR,
        HERB_PATCH,
        WAYPOINT
    }

    @FunctionalInterface
    interface PlacementFactory {
        WorldObject create(String id, Player player, int sequence);
    }

    private static final class PlacementRecipe {
        final String label;
        final PlacementFactory factory;

        PlacementRecipe(String label, PlacementFactory factory) {
            this.label = label;
            this.factory = factory;
        }
    }

    private final Map<PlacementType, PlacementRecipe> recipes = new EnumMap<>(PlacementType.class);
    private final EnumMap<PlacementType, Integer> counters = new EnumMap<>(PlacementType.class);
    private PlacementType current = PlacementType.CHEST;

    PlacementManager() {
        registerDefaults();
    }

    private void registerDefaults() {
        register(PlacementType.CHEST, "Chest", (id, player, sequence) ->
                WorldObjectFactory.createChest(id, player.getPreciseX() + 32, player.getPreciseY(), 20, "bluu_sigil"));

        register(PlacementType.DOOR, "Door", (id, player, sequence) ->
                WorldObjectFactory.createDoor(id, player.getPreciseX() + 16, player.getPreciseY(), sequence % 2 == 0, null));

        register(PlacementType.HERB_PATCH, "Herb Patch", (id, player, sequence) ->
                WorldObjectFactory.createHerbPatch(id, player.getPreciseX() + 24, player.getPreciseY() + 8, "herb_dawnblossom", 1));

        register(PlacementType.WAYPOINT, "Waypoint", (id, player, sequence) ->
                WorldObjectFactory.createWaypoint(id, id, "Waypoint " + sequence, player.getPreciseX() + 48, player.getPreciseY(), 10, 10));
    }

    void register(PlacementType type, String label, PlacementFactory factory) {
        if (type == null || factory == null) {
            return;
        }
        recipes.put(type, new PlacementRecipe(label, factory));
        counters.putIfAbsent(type, 0);
    }

    PlacementType getCurrent() {
        return current;
    }

    void nextType() {
        PlacementType[] values = PlacementType.values();
        int idx = (current.ordinal() + 1) % values.length;
        current = values[idx];
    }

    void previousType() {
        PlacementType[] values = PlacementType.values();
        int idx = (current.ordinal() - 1 + values.length) % values.length;
        current = values[idx];
    }

    String getCurrentLabel() {
        PlacementRecipe recipe = recipes.get(current);
        return recipe != null ? recipe.label : current.name();
    }

    WorldObject place(Player player) {
        if (player == null) {
            return null;
        }
        PlacementRecipe recipe = recipes.get(current);
        if (recipe == null) {
            return null;
        }
        int sequence = counters.merge(current, 1, Integer::sum);
        String id = "placed_" + current.name().toLowerCase() + "_" + sequence;
        return recipe.factory.create(id, player, sequence);
    }
}