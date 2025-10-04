import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FastTravelNetwork {
    private final Map<String, FastTravelPoint> points = new HashMap<>();

    void register(FastTravelPoint point) {
        if (point == null || point.getPointId() == null) {
            return;
        }
        points.put(point.getPointId(), point);
    }

    FastTravelPoint getPoint(String pointId) {
        return points.get(pointId);
    }

    void unlock(String pointId) {
        FastTravelPoint point = points.get(pointId);
        if (point != null) {
            point.setUnlocked(true);
        }
    }

    boolean isUnlocked(String pointId) {
        FastTravelPoint point = points.get(pointId);
        return point != null && point.isUnlocked();
    }

    List<FastTravelPoint> getUnlockedPoints() {
        List<FastTravelPoint> unlocked = new ArrayList<>();
        for (FastTravelPoint point : points.values()) {
            if (point.isUnlocked()) {
                unlocked.add(point);
            }
        }
        return unlocked;
    }

    void clear() {
        points.clear();
    }

    Collection<FastTravelPoint> getAllPoints() {
        return Collections.unmodifiableCollection(points.values());
    }
}
