package com.apm23.custompickaxe;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Lightweight per-player global state shared by every custom pickaxe type. */
final class GlobalPickaxeState {
    private static final Map<UUID, Boolean> ENABLED = new ConcurrentHashMap<>();

    private GlobalPickaxeState() {}

    static boolean isEnabled(UUID playerId) {
        return ENABLED.getOrDefault(playerId, true);
    }

    static boolean toggle(UUID playerId) {
        boolean next = !isEnabled(playerId);
        ENABLED.put(playerId, next);
        return next;
    }
}
