package com.example.melhorias;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MelhoriasMod implements ModInitializer {
    public static final String MOD_ID = "melhorias";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.load();

        PayloadTypeRegistry.playC2S().register(RefreshOffersPayload.ID, RefreshOffersPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FishCaughtPayload.ID, FishCaughtPayload.CODEC);

        // Aldeões: botão "Atualizar ofertas"
        ServerPlayNetworking.registerGlobalReceiver(RefreshOffersPayload.ID,
                (payload, context) -> VillagerRefresh.handle(context.player()));

        // Pesca: quando o item pescado nasce no mundo, avisa o jogador (só visual; não mexe na loot table)
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item) FishingTracker.onItem(item);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> FishingTracker.end());
    }
}
