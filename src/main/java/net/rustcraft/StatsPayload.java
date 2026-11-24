package net.rustcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StatsPayload(boolean rust, int water, int food) implements CustomPayload {
    public static final Id<StatsPayload> ID = new Id<>(Identifier.of(RustCraft.MOD_ID, "stats"));
    public static final PacketCodec<RegistryByteBuf, StatsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, StatsPayload::rust,
            PacketCodecs.INTEGER, StatsPayload::water,
            PacketCodecs.INTEGER, StatsPayload::food,
            StatsPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
