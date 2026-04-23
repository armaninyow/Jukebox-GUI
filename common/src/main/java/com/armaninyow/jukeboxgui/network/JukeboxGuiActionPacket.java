package com.armaninyow.jukeboxgui.network;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Sent client→server when the player performs a GUI action:
 *   TOGGLE_PLAY  — pause or resume playback
 *   INSERT_DISC  — player wants to place the cursor item into the jukebox slot
 *   EJECT_DISC   — player wants to remove the disc from the jukebox
 */
public record JukeboxGuiActionPacket(Action action, BlockPos pos) implements CustomPayload {

	public enum Action { TOGGLE_PLAY, INSERT_DISC, EJECT_DISC }

	public static final Identifier ACTION_ID = Identifier.of(JukeboxGUI.MOD_ID, "gui_action");
	public static final Id<JukeboxGuiActionPacket> ID = new Id<>(ACTION_ID);

	public static final PacketCodec<PacketByteBuf, JukeboxGuiActionPacket> CODEC = PacketCodec.of(
		(value, buf) -> {
			buf.writeEnumConstant(value.action);
			buf.writeBlockPos(value.pos);
		},
		buf -> new JukeboxGuiActionPacket(buf.readEnumConstant(Action.class), buf.readBlockPos())
	);

	@Override
	public Id<? extends CustomPayload> getId() { return ID; }
}