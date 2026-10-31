package com.example.melhorias;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Só observa: guarda quem está recolhendo a vara e, quando o item pescado nasce no mundo,
 * manda uma cópia para o cliente. Loot tables e probabilidades NÃO são alteradas.
 */
public final class FishingTracker {
    private FishingTracker() {}

    private static ServerPlayerEntity current;
    private static boolean sent;

    public static void begin(ServerPlayerEntity player) {
        current = player;
        sent = false;
    }

    public static void end() {
        current = null;
        sent = false;
    }

    public static void onItem(ItemEntity item) {
        if (current == null || sent) return;
        if (!ModConfig.get().fishingUi) return;
        if (!ServerPlayNetworking.canSend(current, FishCaughtPayload.ID)) return;
        ItemStack stack = item.getStack();
        if (stack.isEmpty()) return;
        sent = true;
        ServerPlayNetworking.send(current, new FishCaughtPayload(stack.copy(), item.getId()));
    }
}
