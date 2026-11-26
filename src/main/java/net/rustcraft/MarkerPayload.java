package net.rustcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Сервер -> клиент: сколько ударов получило дерево (count = -1, если дерево срублено). */
public record MarkerPayload(BlockPos base, int count) implements CustomPayload {
    public static final Id<MarkerPayload> ID = new Id<>(Identifier.of(RustCraft.MOD_ID, "marker"));
    public static final PacketCodec<RegistryByteBuf, MarkerPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, MarkerPayload::base,
            PacketCodecs.INTEGER, MarkerPayload::count,
            MarkerPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
