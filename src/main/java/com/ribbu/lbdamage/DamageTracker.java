/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.world.entity.Display$TextDisplay
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.ThrowableProjectile
 *  net.minecraft.world.entity.projectile.arrow.AbstractArrow
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 */
package com.ribbu.lbdamage;

import com.ribbu.lbdamage.DisplayAccess;
import com.ribbu.lbdamage.LBDamageClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class DamageTracker {
    private static final Map<UUID, Double> LAST_HEALTH = new HashMap<UUID, Double>();
    private static final Map<UUID, Integer> CRIT_UNTIL = new HashMap<UUID, Integer>();
    private static final List<Marker> MARKERS = new ArrayList<Marker>();
    private static final List<long[]> PLAYER_ATTACKS = new ArrayList<long[]>();
    private static final List<long[]> PROJECTILE_MARKS = new ArrayList<long[]>();
    public static int currentTick = 0;

    private DamageTracker() {
    }

    public static void tick(Minecraft minecraft) {
        AbstractArrow abstractArrow;
        ++currentTick;
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            LAST_HEALTH.clear();
            CRIT_UNTIL.clear();
            MARKERS.clear();
            PLAYER_ATTACKS.clear();
            PROJECTILE_MARKS.clear();
            return;
        }
        Iterator<Marker> iterator = MARKERS.iterator();
        while (iterator.hasNext()) {
            Iterator iterator2 = iterator.next();
            if (currentTick == ((Marker)((Object)iterator2)).spawnTick + 1 && ((Marker)((Object)iterator2)).display.isAlive()) {
                ((Marker)((Object)iterator2)).display.getEntityData().set(DisplayAccess.TRANSLATION, (Object)new Vector3f(0.0f, LBDamageClient.FLOAT_UP, 0.0f));
                ((Marker)((Object)iterator2)).display.onSyncedDataUpdated(DisplayAccess.TRANSLATION);
                ((Marker)((Object)iterator2)).display.onSyncedDataUpdated(DisplayAccess.INTERP_START);
                ((Marker)((Object)iterator2)).display.onSyncedDataUpdated(DisplayAccess.INTERP_DURATION);
            }
            if (currentTick < ((Marker)((Object)iterator2)).removeTick) continue;
            clientLevel.removeEntity(((Marker)((Object)iterator2)).display.getId(), Entity.RemovalReason.DISCARDED);
            iterator.remove();
        }
        CRIT_UNTIL.values().removeIf(n -> n <= currentTick);
        PLAYER_ATTACKS.removeIf(lArray -> DamageTracker.packedTick(lArray) < currentTick - 2);
        PROJECTILE_MARKS.removeIf(lArray -> DamageTracker.packedTick(lArray) < currentTick - 2);
        for (Entity entity : clientLevel.entitiesForRendering()) {
            ThrowableProjectile throwableProjectile;
            Vec3 vec3;
            if (entity instanceof AbstractArrow && (abstractArrow = (AbstractArrow)entity).getOwner() instanceof Player) {
                vec3 = abstractArrow.position();
                PROJECTILE_MARKS.add(DamageTracker.pack(currentTick, vec3.x, vec3.y, vec3.z));
                continue;
            }
            if (!(entity instanceof ThrowableProjectile) || !((throwableProjectile = (ThrowableProjectile)entity).getOwner() instanceof Player)) continue;
            vec3 = throwableProjectile.position();
            PROJECTILE_MARKS.add(DamageTracker.pack(currentTick, vec3.x, vec3.y, vec3.z));
        }
        for (Entity entity : clientLevel.entitiesForRendering()) {
            boolean bl;
            double d;
            if (!(entity instanceof LivingEntity)) continue;
            abstractArrow = (LivingEntity)entity;
            if (abstractArrow.isRemoved() || !abstractArrow.isAlive()) {
                LAST_HEALTH.remove(abstractArrow.getUUID());
                continue;
            }
            double d2 = abstractArrow.getHealth();
            Double d3 = LAST_HEALTH.put(abstractArrow.getUUID(), d2);
            if (d3 == null || (d = d3 - d2) < LBDamageClient.MIN_DAMAGE) continue;
            boolean bl2 = bl = CRIT_UNTIL.getOrDefault(abstractArrow.getUUID(), -1) >= currentTick;
            if (!bl && !DamageTracker.hasPlayerAttackContext((LivingEntity)abstractArrow)) continue;
            DamageTracker.spawnMarker(clientLevel, (LivingEntity)abstractArrow, d, bl);
        }
    }

    public static void markPlayerAttack(double d, double d2, double d3) {
        PLAYER_ATTACKS.add(DamageTracker.pack(currentTick, d, d2, d3));
    }

    public static void markCrit(ClientLevel clientLevel, double d, double d2, double d3) {
        LivingEntity livingEntity = null;
        double d4 = 1.6900000000000002;
        for (Entity entity : clientLevel.entitiesForRendering()) {
            LivingEntity livingEntity2;
            double d5;
            if (!(entity instanceof LivingEntity) || !((d5 = (livingEntity2 = (LivingEntity)entity).distanceToSqr(d, d2, d3)) < d4)) continue;
            d4 = d5;
            livingEntity = livingEntity2;
        }
        if (livingEntity != null) {
            CRIT_UNTIL.put(livingEntity.getUUID(), currentTick + 3);
        }
    }

    private static boolean hasPlayerAttackContext(LivingEntity livingEntity) {
        if (LBDamageAttackTargets.matches(livingEntity)) {
            return true;
        }
        double d;
        double d2;
        double d3;
        Vec3 vec3 = livingEntity.position();
        for (long[] lArray : PLAYER_ATTACKS) {
            if (DamageTracker.packedTick(lArray) < currentTick - 2 || !((d3 = DamageTracker.unpack(lArray, 1) - vec3.x) * d3 + (d2 = DamageTracker.unpack(lArray, 3) - vec3.y) * d2 + (d = DamageTracker.unpack(lArray, 2) - vec3.z) * d < 30.25)) continue;
            return true;
        }
        for (long[] lArray : PROJECTILE_MARKS) {
            if (DamageTracker.packedTick(lArray) < currentTick - 2 || !((d3 = DamageTracker.unpack(lArray, 1) - vec3.x) * d3 + (d2 = DamageTracker.unpack(lArray, 3) - vec3.y) * d2 + (d = DamageTracker.unpack(lArray, 2) - vec3.z) * d < 12.25)) continue;
            return true;
        }
        return false;
    }

    private static long[] pack(int n, double d, double d2, double d3) {
        return new long[]{n, Double.doubleToLongBits(d), Double.doubleToLongBits(d3), Double.doubleToLongBits(d2)};
    }

    private static int packedTick(long[] lArray) {
        return (int)lArray[0];
    }

    private static double unpack(long[] lArray, int n) {
        return Double.longBitsToDouble(lArray[n]);
    }

    private static void spawnMarker(ClientLevel clientLevel, LivingEntity livingEntity, double d, boolean bl) {
        if (!DisplayAccess.init()) {
            return;
        }
        float f = DamageTracker.random(LBDamageClient.RANDOM_OFFSET);
        float f2 = DamageTracker.random(LBDamageClient.RANDOM_OFFSET);
        float f3 = DamageTracker.random(LBDamageClient.RANDOM_OFFSET);
        double d2 = livingEntity.getX() + (double)f;
        double d3 = livingEntity.getY() + (double)livingEntity.getBbHeight() + (double)LBDamageClient.HEIGHT_OFFSET + (double)f2 * 0.5;
        double d4 = livingEntity.getZ() + (double)f3;
        Display.TextDisplay textDisplay = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, (Level)clientLevel);
        textDisplay.setPos(d2, d3, d4);
        DamageTracker.set(textDisplay, DisplayAccess.TEXT, DamageTracker.buildText(d, bl));
        DamageTracker.set(textDisplay, DisplayAccess.LINE_WIDTH, 300);
        DamageTracker.set(textDisplay, DisplayAccess.BACKGROUND_COLOR, 0);
        DamageTracker.set(textDisplay, DisplayAccess.BILLBOARD, (byte)3);
        float f4 = LBDamageClient.SCALE;
        DamageTracker.set(textDisplay, DisplayAccess.SCALE, new Vector3f(f4, f4, f4));
        DamageTracker.set(textDisplay, DisplayAccess.TRANSLATION, new Vector3f(0.0f, 0.0f, 0.0f));
        DamageTracker.set(textDisplay, DisplayAccess.INTERP_DURATION, LBDamageClient.DISPLAY_TICKS);
        textDisplay.onSyncedDataUpdated(DisplayAccess.SCALE);
        textDisplay.onSyncedDataUpdated(DisplayAccess.TRANSLATION);
        textDisplay.onSyncedDataUpdated(DisplayAccess.INTERP_DURATION);
        textDisplay.setId(LBDamageEntityIds.next());
        clientLevel.addEntity((Entity)textDisplay);
        MARKERS.add(new Marker(textDisplay, currentTick, currentTick + LBDamageClient.DISPLAY_TICKS + 2));
    }

    private static Component buildText(double d, boolean bl) {
        String string = DamageTracker.format(d);
        MutableComponent mutableComponent = Component.empty();
        if (bl) {
            MutableComponent mutableComponent2 = Component.literal((String)"\u2726 ");
            mutableComponent2.setStyle(mutableComponent2.getStyle().withColor(ChatFormatting.GOLD).withBold(Boolean.valueOf(LBDamageClient.CRIT_BOLD)));
            mutableComponent.append((Component)mutableComponent2);
            mutableComponent.append((Component)DamageTracker.styled(string, ChatFormatting.GOLD, LBDamageClient.CRIT_BOLD));
        } else {
            mutableComponent.append((Component)DamageTracker.styled(string, ChatFormatting.WHITE, false));
        }
        return mutableComponent;
    }

    private static MutableComponent styled(String string, ChatFormatting chatFormatting, boolean bl) {
        MutableComponent mutableComponent = Component.literal((String)DamageTracker.toDamageGlyphs(string));
        mutableComponent.setStyle(mutableComponent.getStyle().withColor(chatFormatting).withBold(Boolean.valueOf(bl)));
        return mutableComponent;
    }

    private static String toDamageGlyphs(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c >= '0' && c <= '9') {
                stringBuilder.append((char)(60160 + (c - 48)));
                continue;
            }
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    private static String format(double d) {
        double d2 = Math.max(0.0, d);
        if (LBDamageClient.DECIMALS <= 0) {
            return DamageTracker.group(String.valueOf(Math.round(d2)));
        }
        String string = String.format(Locale.US, "%." + LBDamageClient.DECIMALS + "f", d2);
        int n = string.indexOf(46);
        String string2 = n < 0 ? string : string.substring(0, n);
        String string3 = n < 0 ? "" : string.substring(n);
        return DamageTracker.group(string2) + string3;
    }

    private static String group(String string) {
        StringBuilder stringBuilder = new StringBuilder(string);
        for (int i = stringBuilder.length() - 3; i > 0; i -= 3) {
            stringBuilder.insert(i, ',');
        }
        return stringBuilder.toString();
    }

    private static float random(float f) {
        if (f <= 0.0f) {
            return 0.0f;
        }
        return ThreadLocalRandom.current().nextFloat(-f, f);
    }

    private static void set(Display.TextDisplay textDisplay, EntityDataAccessor<?> entityDataAccessor, Object object) {
        textDisplay.getEntityData().set(entityDataAccessor, object);
    }

    private static final class Marker {
        final Display.TextDisplay display;
        final int spawnTick;
        final int removeTick;

        Marker(Display.TextDisplay textDisplay, int n, int n2) {
            this.display = textDisplay;
            this.spawnTick = n;
            this.removeTick = n2;
        }
    }
}
