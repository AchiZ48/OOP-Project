import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Integer> items = new HashMap<>();

    void addItem(String id, int amount) {
        if (id == null || amount <= 0) {
            return;
        }
        items.merge(id, amount, Integer::sum);
    }

    boolean hasItem(String id, int amount) {
        if (id == null || amount <= 0) {
            return false;
        }
        return items.getOrDefault(id, 0) >= amount;
    }

    boolean consumeItem(String id, int amount) {
        if (!hasItem(id, amount)) {
            return false;
        }
        int remaining = items.get(id) - amount;
        if (remaining <= 0) {
            items.remove(id);
        } else {
            items.put(id, remaining);
        }
        return true;
    }

    int getCount(String id) {
        return items.getOrDefault(id, 0);
    }

    void clear() {
        items.clear();
    }

    Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(items);
    }
}
