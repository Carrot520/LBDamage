package com.ribbu.lbdamage;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Correlates a local attack with the exact entity whose health should produce
 * a damage number. This avoids relying on distance and packet timing.
 */
public final class LBDamageAttackTargets {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final ConcurrentHashMap<UUID, Long> TARGETS = new ConcurrentHashMap<>();

    private LBDamageAttackTargets() {
    }

    public static void mark(Entity entity) {
        if (entity != null) {
            TARGETS.put(entity.getUUID(), System.nanoTime());
        }
    }

    public static boolean matches(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        Long markedAt = TARGETS.remove(entity.getUUID());
        if (markedAt == null) {
            return false;
        }
        return System.nanoTime() - markedAt <= WINDOW_NANOS;
    }
}
