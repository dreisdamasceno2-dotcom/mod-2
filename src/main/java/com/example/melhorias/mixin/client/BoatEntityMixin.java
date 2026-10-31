package com.example.melhorias.mixin.client;

import com.example.melhorias.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ajusta o movimento do barco só para quem está pilotando (o cliente controla o barco no jogo
 * original). Só atua na água; gelo e terra continuam como no original.
 */
@Mixin(BoatEntity.class)
public abstract class BoatEntityMixin {
    @Unique
    private float melhorias$yawBefore;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void melhorias$head(CallbackInfo ci) {
        melhorias$yawBefore = ((BoatEntity) (Object) this).getYaw();
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void melhorias$tail(CallbackInfo ci) {
        BoatEntity boat = (BoatEntity) (Object) this;
        ModConfig cfg = ModConfig.get();
        if (!cfg.boatImprovements || !boat.getWorld().isClient) return;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || boat.getControllingPassenger() != player) return;
        if (!boat.isTouchingWater()) return;

        Input in = player.input;
        boolean left = in.pressingLeft, right = in.pressingRight;
        boolean fwd = in.pressingForward, back = in.pressingBack;

        Vec3d vel = boat.getVelocity();
        double vx = vel.x, vz = vel.z;
        float yaw = boat.getYaw();
        double rad = yaw * 0.017453292519943295;
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double speed = Math.sqrt(vx * vx + vz * vz);

        // ---------- Direção: resposta imediata, estável em velocidade alta
        float sens = (float) cfg.boatSteering;
        float dir = (right ? 1f : 0f) - (left ? 1f : 0f);
        float vanillaDelta = MathHelper.wrapDegrees(yaw - melhorias$yawBefore);
        float newDelta;
        if (dir != 0f) {
            float speedFactor = (float) (1.0 - 0.25 * Math.min(1.0, speed / 0.45));
            float same = Math.signum(vanillaDelta) == dir ? Math.abs(vanillaDelta) : 0f;
            float minRate = 1.8f * sens * speedFactor;
            float mag = Math.max(same * sens * speedFactor, minRate);
            mag = Math.min(mag, 8.0f * sens);
            newDelta = dir * mag;
        } else {
            newDelta = vanillaDelta * 0.55f; // para de girar mais rápido ao soltar
        }
        boat.setYaw(melhorias$yawBefore + newDelta);

        // ---------- Aceleração e velocidade máxima
        double vmax = 0.40 * cfg.boatMaxSpeed;
        if (fwd && !back) {
            double push = 0.04 * (cfg.boatMaxSpeed - 1.0);
            if (speed < vmax * 0.6) push += 0.04 * (cfg.boatAcceleration - 1.0);
            vx += fx * push;
            vz += fz * push;
        }

        // ---------- Derrapagem: remove parte do deslize lateral
        double along = vx * fx + vz * fz;
        double grip = (1.0 - cfg.boatDrift) * 0.25;
        vx -= (vx - along * fx) * grip;
        vz -= (vz - along * fz) * grip;

        // ---------- Desaceleração
        if (!fwd && !back) {
            double f = MathHelper.clamp(1.0 - 0.05 * (cfg.boatDeceleration - 1.0), 0.8, 1.05);
            vx *= f;
            vz *= f;
        } else if (back && !fwd && along > 0.03) {
            double f = 1.0 - 0.06 * cfg.boatDeceleration;
            vx *= f;
            vz *= f;
        }

        // ---------- Limite de velocidade
        double s2 = vx * vx + vz * vz;
        if (s2 > vmax * vmax) {
            double k = vmax / Math.sqrt(s2);
            vx *= k;
            vz *= k;
        }

        boat.setVelocity(vx, vel.y, vz);
    }
}
