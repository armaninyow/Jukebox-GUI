package com.armaninyow.jukeboxgui.network;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent from client to server when the player clicks pause or resume.
 * For PAUSE: carries the current ticksSinceSongStarted so server can restore it on resume.
 * For RESUME: ticksSinceSongStarted is the saved value to restore.
 */
public record JukeboxGuiPausePacket(boolean pausing, long ticksSinceSongStarted, BlockPos pos)
        implements CustomPacketPayload {

    public static final Identifier PAUSE_ID =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "pause_disc");
    public static final Type<JukeboxGuiPausePacket> ID = new Type<>(PAUSE_ID);

    public static final StreamCodec<FriendlyByteBuf, JukeboxGuiPausePacket> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeBoolean(value.pausing);
            buf.writeLong(value.ticksSinceSongStarted);
            buf.writeBlockPos(value.pos);
        },
        buf -> new JukeboxGuiPausePacket(buf.readBoolean(), buf.readLong(), buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}