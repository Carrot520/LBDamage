/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.core.particles.ParticleTypes
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.ribbu.lbdamage.mixin;

import com.ribbu.lbdamage.DamageTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientLevel.class})
public class CritParticleMixin {
    @Inject(method={"addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"}, at={@At(value="HEAD")})
    private void lbdamage$critParticle(ParticleOptions particleOptions, double d, double d2, double d3, double d4, double d5, double d6, CallbackInfo callbackInfo) {
        ParticleType particleType = particleOptions.getType();
        if (particleType == ParticleTypes.CRIT) {
            DamageTracker.markCrit((ClientLevel)this, d, d2, d3);
        } else if (particleType == ParticleTypes.ENCHANTED_HIT) {
            DamageTracker.markPlayerAttack(d, d2, d3);
        }
    }
}

