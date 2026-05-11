package com.armaninyow.jukeboxgui.network;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Sent client→server to request a fresh JukeboxGuiPacket.Payload for the given pos.
 * The screen sends this every second while open.
 */
public record JukeboxGuiRefreshPacket(BlockPos pos) implements CustomPayload {

	public static final Identifier REFRESH_ID = Identifier.of(JukeboxGUI.MOD_ID, "refresh_gui");
	public static final Id<JukeboxGuiRefreshPacket> ID = new Id<>(REFRESH_ID);

	public static final PacketCodec<PacketByteBuf, JukeboxGuiRefreshPacket> CODEC = PacketCodec.of(
		(value, buf) -> buf.writeBlockPos(value.pos),
		buf -> new JukeboxGuiRefreshPacket(buf.readBlockPos())
	);

	@Override
	public Id<? extends CustomPayload> getId() { return ID; }
}