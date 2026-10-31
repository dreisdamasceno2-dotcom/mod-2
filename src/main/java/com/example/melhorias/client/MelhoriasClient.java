package com.example.melhorias.client;

import com.example.melhorias.FishCaughtPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class MelhoriasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FishCaughtPayload.ID,
                (payload, context) -> FishingOverlay.onCaught(payload.stack(), payload.itemEntityId()));
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> FishingOverlay.render(ctx));
        ClientTickEvents.END_CLIENT_TICK.register(FishingOverlay::clientTick);
    }
}
