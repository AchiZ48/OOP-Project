import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class EnemyRegister {
    private final Map<String, Enemy> registeredEnemies = new HashMap<>();

    void register(String id, Enemy template) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Enemy id cannot be null or blank");
        }
        if (template == null) {
            throw new IllegalArgumentException("Enemy template cannot be null for id " + id);
        }
        String normalizedId = normalize(id);
        registeredEnemies.put(normalizedId, template.copy());
    }

    boolean isRegistered(String id) {
        return registeredEnemies.containsKey(normalize(id));
    }

    Enemy create(String id) {
        Enemy template = registeredEnemies.get(normalize(id));
        if (template == null) {
            throw new IllegalArgumentException("Enemy not registered: " + id);
        }
        return template.copy();
    }

    Map<String, Enemy> snapshot() {
        Map<String, Enemy> copy = new HashMap<>();
        for (Map.Entry<String, Enemy> entry : registeredEnemies.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return Collections.unmodifiableMap(copy);
    }

    private String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).trim();
    }
}
