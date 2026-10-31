package com.example.melhorias;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> Cliente: "você pescou este item" (e qual é a entidade do item no chão). */
public record FishCaughtPayload(ItemStack stack, int itemEntityId) implements CustomPayload {
    public static final CustomPayload.Id<FishCaughtPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MelhoriasMod.MOD_ID, "fish_caught"));

    public static final PacketCodec<RegistryByteBuf, FishCaughtPayload> CODEC = PacketCodec.tuple(
            ItemStack.PACKET_CODEC, FishCaughtPayload::stack,
            PacketCodecs.VAR_INT, FishCaughtPayload::itemEntityId,
            FishCaughtPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
