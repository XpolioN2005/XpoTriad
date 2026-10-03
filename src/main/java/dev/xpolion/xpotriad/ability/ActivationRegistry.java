package dev.xpolion.xpotriad.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps UUID → AbilityContext for active projectile-based ability executions.
 * Allows projectile hit events to retrieve the originating context.
 * Entries must be removed after use to prevent memory leaks.
 */
public final class ActivationRegistry {

    private final Map<UUID, AbilityContext> registry = new ConcurrentHashMap<>();

    public void register(UUID id, AbilityContext context) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        registry.put(id, context);
    }

    public AbilityContext get(UUID id) {
        return registry.get(id);
    }

    public AbilityContext remove(UUID id) {
        return registry.remove(id);
    }

    public boolean contains(UUID id) {
        return registry.containsKey(id);
    }
}
