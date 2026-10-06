/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.multiplayer.ClientPacketListener
 *  net.minecraft.network.protocol.game.ClientboundAnimatePacket
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.ribbu.lbdamage.mixin;

import com.ribbu.lbdamage.DamageTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientPacketListener.class})
public class PlayerSwingMixin {
    @Inject(method={"handleAnimate(Lnet/minecraft/network/protocol/game/ClientboundAnimatePacket;)V"}, at={@At(value="HEAD")})
    private void lbdamage$playerSwing(ClientboundAnimatePacket clientboundAnimatePacket, CallbackInfo callbackInfo) {
        if (clientboundAnimatePacket.getAction() != 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            return;
        }
        Entity entity = clientLevel.getEntity(clientboundAnimatePacket.getId());
        if (entity instanceof Player) {
            Player player = (Player)entity;
            DamageTracker.markPlayerAttack(player.getX(), player.getY(), player.getZ());
        }
    }
}

