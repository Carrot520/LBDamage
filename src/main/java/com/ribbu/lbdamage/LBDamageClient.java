/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.loader.api.FabricLoader
 */
package com.ribbu.lbdamage;

import com.ribbu.lbdamage.DamageTracker;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Properties;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public class LBDamageClient
implements ClientModInitializer {
    public static float SCALE = 0.6f;
    public static int DISPLAY_TICKS = 40;
    public static float FLOAT_UP = 0.8f;
    public static float RANDOM_OFFSET = 0.3f;
    public static float HEIGHT_OFFSET = 1.4f;
    public static double MIN_DAMAGE = 0.05;
    public static int DECIMALS = 1;
    public static boolean CRIT_BOLD = true;

    public void onInitializeClient() {
        this.loadConfig();
        ClientTickEvents.END_CLIENT_TICK.register(DamageTracker::tick);
    }

    private void loadConfig() {
        block17: {
            try {
                Object object;
                Path path = FabricLoader.getInstance().getGameDir().resolve("config");
                Files.createDirectories(path, new FileAttribute[0]);
                Path path2 = path.resolve("lbdamage.properties");
                if (!Files.exists(path2, new LinkOption[0])) {
                    object = LBDamageClient.class.getResourceAsStream("/lbdamage-defaults.properties");
                    try {
                        if (object != null) {
                            Files.copy((InputStream)object, path2, new CopyOption[0]);
                        }
                    }
                    finally {
                        if (object != null) {
                            ((InputStream)object).close();
                        }
                    }
                }
                if (!Files.exists(path2, new LinkOption[0])) break block17;
                object = new Properties();
                try (InputStream inputStream = Files.newInputStream(path2, new OpenOption[0]);){
                    ((Properties)object).load(inputStream);
                }
                SCALE = LBDamageClient.floatOf((Properties)object, "scale", SCALE);
                DISPLAY_TICKS = LBDamageClient.intOf((Properties)object, "display-ticks", DISPLAY_TICKS);
                FLOAT_UP = LBDamageClient.floatOf((Properties)object, "float-up", FLOAT_UP);
                RANDOM_OFFSET = LBDamageClient.floatOf((Properties)object, "random-offset", RANDOM_OFFSET);
                HEIGHT_OFFSET = LBDamageClient.floatOf((Properties)object, "height-offset", HEIGHT_OFFSET);
                MIN_DAMAGE = LBDamageClient.doubleOf((Properties)object, "min-damage", MIN_DAMAGE);
                DECIMALS = Math.max(0, Math.min(2, LBDamageClient.intOf((Properties)object, "decimal-places", DECIMALS)));
                CRIT_BOLD = Boolean.parseBoolean(((Properties)object).getProperty("crit-bold", String.valueOf(CRIT_BOLD)));
            }
            catch (Throwable throwable) {
                System.out.println("[LBDamage] config load failed, using defaults: " + String.valueOf(throwable));
            }
        }
    }

    private static float floatOf(Properties properties, String string, float f) {
        try {
            return Float.parseFloat(properties.getProperty(string, String.valueOf(f)));
        }
        catch (NumberFormatException numberFormatException) {
            return f;
        }
    }

    private static double doubleOf(Properties properties, String string, double d) {
        try {
            return Double.parseDouble(properties.getProperty(string, String.valueOf(d)));
        }
        catch (NumberFormatException numberFormatException) {
            return d;
        }
    }

    private static int intOf(Properties properties, String string, int n) {
        try {
            return Integer.parseInt(properties.getProperty(string, String.valueOf(n)));
        }
        catch (NumberFormatException numberFormatException) {
            return n;
        }
    }
}

