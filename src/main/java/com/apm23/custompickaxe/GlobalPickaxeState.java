package com.apm23.custompickaxe;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Lightweight per-player global level shared by every custom pickaxe type. */
final class GlobalPickaxeState {
    static final int OFF = 0;
    static final int LEVEL_1 = 1;
    static final int LEVEL_2 = 2;
    static final int LEVEL_3 = 3;

    private static final Map<UUID, Integer> LEVELS = new ConcurrentHashMap<>();

    private GlobalPickaxeState() {}

    static int level(UUID playerId) {
        return LEVELS.getOrDefault(playerId, OFF);
    }

    static boolean isEnabled(UUID playerId) {
        return level(playerId) != OFF;
    }

    static int cycle(UUID playerId) {
        int next = switch (level(playerId)) {
            case OFF -> LEVEL_1;
            case LEVEL_1 -> LEVEL_2;
            case LEVEL_2 -> LEVEL_3;
            default -> OFF;
        };
        LEVELS.put(playerId, next);
        return next;
    }

    static int sideForLevel(int level) {
        return switch (level) {
            case LEVEL_1 -> 8;
            case LEVEL_2 -> 16;
            case LEVEL_3 -> 64;
            default -> 0;
        };
    }
}
