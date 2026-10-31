package com.example.melhorias.mixin.client;

import com.example.melhorias.client.FuzzySearch;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Busca do livro de receitas: corrige o texto digitado antes de pesquisar. */
@Mixin(RecipeBookWidget.class)
public abstract class RecipeBookWidgetMixin {
    @ModifyVariable(method = "refreshResults", at = @At(value = "STORE", ordinal = 0), ordinal = 0, require = 0)
    private String melhorias$fixQuery(String query) {
        return FuzzySearch.correct(query);
    }
}
