/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.MultiPlayerGameMode
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.ribbu.lbdamage.mixin;

import com.ribbu.lbdamage.DamageTracker;
import com.ribbu.lbdamage.LBDamageAttackTargets;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MultiPlayerGameMode.class})
public class LocalAttackMixin {
    @Inject(method={"attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"}, at={@At(value="HEAD")})
    private void lbdamage$localAttack(Player player, Entity entity, CallbackInfo callbackInfo) {
        LBDamageAttackTargets.mark(entity);
        DamageTracker.markPlayerAttack(entity.getX(), entity.getY(), entity.getZ());
    }
}
