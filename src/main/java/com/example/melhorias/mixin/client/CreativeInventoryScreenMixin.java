package com.example.melhorias.mixin.client;

import com.example.melhorias.client.FuzzySearch;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Busca do inventário criativo: corrige o texto digitado antes de pesquisar. */
@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {
    @ModifyVariable(method = "search", at = @At(value = "STORE", ordinal = 0), ordinal = 0, require = 0)
    private String melhorias$fixQuery(String query) {
        return FuzzySearch.correct(query);
    }
}
