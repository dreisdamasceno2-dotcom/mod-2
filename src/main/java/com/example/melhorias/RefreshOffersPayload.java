package com.example.melhorias;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente -> Servidor: "atualize as ofertas do aldeão com quem estou negociando". */
public record RefreshOffersPayload() implements CustomPayload {
    public static final CustomPayload.Id<RefreshOffersPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MelhoriasMod.MOD_ID, "refresh_offers"));

    public static final PacketCodec<RegistryByteBuf, RefreshOffersPayload> CODEC =
            PacketCodec.unit(new RefreshOffersPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
