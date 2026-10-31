package com.example.melhorias;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;

import java.util.List;

/**
 * Sorteia as ofertas de novo do mesmo jeito que o jogo faz quando a profissão muda
 * (o jogo zera as ofertas), mas sem tirar a profissão de verdade: ela é trocada e
 * devolvida no mesmo instante. Nível, experiência, bancada e vínculo ficam intactos.
 */
public final class VillagerRefresh {
    private VillagerRefresh() {}

    public static void handle(ServerPlayerEntity player) {
        if (!(player.currentScreenHandler instanceof MerchantScreenHandler)) return;

        ServerWorld world = player.getServerWorld();
        List<VillagerEntity> found = world.getEntitiesByClass(VillagerEntity.class,
                player.getBoundingBox().expand(16), v -> v.getCustomer() == player);
        if (found.isEmpty()) {
            msg(player, "Não foi possível atualizar as ofertas deste comerciante.", Formatting.RED);
            return;
        }

        VillagerEntity villager = found.get(0);
        VillagerData data = villager.getVillagerData();
        VillagerProfession profession = data.getProfession();

        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            msg(player, "Este aldeão não tem ofertas para atualizar.", Formatting.RED);
            return;
        }

        boolean locked = villager.getExperience() > 0 || data.getLevel() > 1;
        if (locked && !ModConfig.get().allowRefreshAfterTrade) {
            msg(player, "Ofertas travadas: você já negociou com este aldeão.", Formatting.RED);
            return;
        }

        // Troca a profissão e devolve na hora: o jogo apaga as ofertas e sorteia de novo.
        villager.setVillagerData(data.withProfession(VillagerProfession.NONE));
        villager.setVillagerData(data.withProfession(profession));

        TradeOfferList offers = villager.getOffers(); // gera as novas ofertas

        // Atualiza a tela aberta na hora.
        player.sendTradeOffers(player.currentScreenHandler.syncId, offers,
                data.getLevel(), villager.getExperience(),
                villager.isLeveledMerchant(), villager.canRefreshTrades());

        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                villager.getX(), villager.getEyeY(), villager.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
        world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES,
                SoundCategory.NEUTRAL, 1.0f, 1.0f);
        msg(player, "Ofertas atualizadas!", Formatting.GREEN);
    }

    private static void msg(ServerPlayerEntity p, String text, Formatting color) {
        p.sendMessage(Text.literal(text).formatted(color), true);
    }
}
