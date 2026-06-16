package com.armaninyow.jukeboxgui.network;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record JukeboxGuiActionPacket(Action action, BlockPos pos) implements CustomPacketPayload {

    public enum Action { TOGGLE_PLAY, INSERT_DISC, EJECT_DISC, RESTART_DISC }

    public static final Identifier ACTION_ID =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "gui_action");
    public static final Type<JukeboxGuiActionPacket> ID = new Type<>(ACTION_ID);

    public static final StreamCodec<FriendlyByteBuf, JukeboxGuiActionPacket> CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeEnum(value.action);
            buf.writeBlockPos(value.pos);
        },
        buf -> new JukeboxGuiActionPacket(buf.readEnum(Action.class), buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}