/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.world.entity.Display
 *  net.minecraft.world.entity.Display$TextDisplay
 *  org.joml.Vector3fc
 */
package com.ribbu.lbdamage;

import java.lang.reflect.Field;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.joml.Vector3fc;

public final class DisplayAccess {
    public static EntityDataAccessor<Component> TEXT;
    public static EntityDataAccessor<Integer> LINE_WIDTH;
    public static EntityDataAccessor<Integer> BACKGROUND_COLOR;
    public static EntityDataAccessor<Byte> BILLBOARD;
    public static EntityDataAccessor<Vector3fc> SCALE;
    public static EntityDataAccessor<Vector3fc> TRANSLATION;
    public static EntityDataAccessor<Integer> INTERP_START;
    public static EntityDataAccessor<Integer> INTERP_DURATION;
    private static boolean ready;

    private DisplayAccess() {
    }

    public static synchronized boolean init() {
        if (ready) {
            return true;
        }
        try {
            Class<Display.TextDisplay> clazz = Display.TextDisplay.class;
            Class<Display> clazz2 = Display.class;
            TEXT = DisplayAccess.accessor(clazz, "DATA_TEXT_ID");
            LINE_WIDTH = DisplayAccess.accessor(clazz, "DATA_LINE_WIDTH_ID");
            BACKGROUND_COLOR = DisplayAccess.accessor(clazz, "DATA_BACKGROUND_COLOR_ID");
            BILLBOARD = DisplayAccess.accessor(clazz2, "DATA_BILLBOARD_RENDER_CONSTRAINTS_ID");
            SCALE = DisplayAccess.accessor(clazz2, "DATA_SCALE_ID");
            TRANSLATION = DisplayAccess.accessor(clazz2, "DATA_TRANSLATION_ID");
            INTERP_START = DisplayAccess.accessor(clazz2, "DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID");
            INTERP_DURATION = DisplayAccess.accessor(clazz2, "DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID");
            ready = true;
        }
        catch (Throwable throwable) {
            System.out.println("[LBDamage] display accessor init failed: " + String.valueOf(throwable));
        }
        return ready;
    }

    private static <T> EntityDataAccessor<T> accessor(Class<?> clazz, String string) throws Exception {
        Field field = clazz.getDeclaredField(string);
        field.setAccessible(true);
        return (EntityDataAccessor)field.get(null);
    }
}

