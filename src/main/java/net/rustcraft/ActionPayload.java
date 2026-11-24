package net.rustcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Клиент -> сервер. type 1 = открыть инвентарь Rust, type 2 = выбрать постройку (value). */
public record ActionPayload(int type, int value) implements CustomPayload {
    public static final Id<ActionPayload> ID = new Id<>(Identifier.of(RustCraft.MOD_ID, "action"));
    public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, ActionPayload::type,
            PacketCodecs.INTEGER, ActionPayload::value,
            ActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
