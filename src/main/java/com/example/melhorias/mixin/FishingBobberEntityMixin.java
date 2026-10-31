package com.example.melhorias.mixin;

import com.example.melhorias.FishingTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marca o início e o fim do "recolher a vara" no servidor. */
@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {

    @Inject(method = "use", at = @At("HEAD"), require = 0)
    private void melhorias$begin(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        PlayerEntity owner = ((FishingBobberEntity) (Object) this).getPlayerOwner();
        if (owner instanceof ServerPlayerEntity sp) FishingTracker.begin(sp);
    }

    @Inject(method = "use", at = @At("RETURN"), require = 0)
    private void melhorias$end(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        FishingTracker.end();
    }
}
