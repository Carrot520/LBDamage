package com.ribbu.lbdamage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Local-only entity IDs for floating damage TextDisplay entities.
 *
 * Minecraft 26.2 initializes Entity.id to zero and requires callers that
 * create client-only entities to assign an id before ClientLevel.addEntity().
 * Negative ids cannot collide with the server's normal positive entity ids.
 */
public final class LBDamageEntityIds {
    private static final AtomicInteger NEXT = new AtomicInteger(Integer.MIN_VALUE);

    private LBDamageEntityIds() {
    }

    public static int next() {
        return NEXT.getAndIncrement();
    }
}
