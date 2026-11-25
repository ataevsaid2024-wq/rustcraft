package net.rustcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Клиент -> сервер: удар по дереву или каменной глыбе. */
public record HitPayload(BlockPos pos) implements CustomPayload {
    public static final Id<HitPayload> ID = new Id<>(Identifier.of(RustCraft.MOD_ID, "hit"));
    public static final PacketCodec<RegistryByteBuf, HitPayload> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, HitPayload::pos, HitPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
