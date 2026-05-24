package com.armaninyow.jukeboxgui.network;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record JukeboxGuiRefreshPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Identifier REFRESH_ID =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "refresh_gui");
    public static final Type<JukeboxGuiRefreshPacket> ID = new Type<>(REFRESH_ID);

    public static final StreamCodec<FriendlyByteBuf, JukeboxGuiRefreshPacket> CODEC = StreamCodec.of(
        (buf, value) -> buf.writeBlockPos(value.pos),
        buf -> new JukeboxGuiRefreshPacket(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}
