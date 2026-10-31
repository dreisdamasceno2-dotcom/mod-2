package com.example.melhorias.mixin.client;

import com.example.melhorias.RefreshOffersPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Botão "Atualizar ofertas" na tela de negociação.
 * O botão é sempre criado; a visibilidade é decidida a cada quadro, porque o servidor só
 * avisa se é um aldeão (e não o mercador errante) depois que a tela já abriu.
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends HandledScreen<MerchantScreenHandler> {

    @Unique
    private ButtonWidget melhorias$refreshButton;

    public MerchantScreenMixin(MerchantScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void melhorias$addRefreshButton(CallbackInfo ci) {
        int by = Math.min(this.y + this.backgroundHeight + 4, this.height - 22);
        melhorias$refreshButton = ButtonWidget.builder(Text.literal("Atualizar ofertas"),
                        b -> ClientPlayNetworking.send(new RefreshOffersPayload()))
                .dimensions(this.x + 5, by, 110, 20)
                .tooltip(Tooltip.of(Text.literal("Sorteia as ofertas de novo (só antes de negociar).")))
                .build();
        this.addDrawableChild(melhorias$refreshButton);
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void melhorias$updateButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (melhorias$refreshButton != null) {
            melhorias$refreshButton.visible = this.handler.isLeveled(); // só aldeões
        }
    }
}
