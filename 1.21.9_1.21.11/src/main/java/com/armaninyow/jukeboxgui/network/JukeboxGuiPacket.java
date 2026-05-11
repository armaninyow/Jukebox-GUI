package com.armaninyow.jukeboxgui.network;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.Optional;

// 1.21.9_1.21.11
public class JukeboxGuiPacket {

	public static final Identifier OPEN_GUI_ID = Identifier.of(JukeboxGUI.MOD_ID, "open_gui");

	public record Payload(
		boolean isPlaying,
		long tickCount,
		long recordStartTick,
		Optional<ItemStack> discStack,
		String songTitle,
		float totalSeconds,
		boolean isFinished,
		BlockPos pos
	) implements CustomPayload {
		public static final Id<Payload> ID = new Id<>(OPEN_GUI_ID);

		public static final PacketCodec<RegistryByteBuf, Payload> CODEC = PacketCodec.of(
			(value, buf) -> {
				buf.writeBoolean(value.isPlaying);
				buf.writeLong(value.tickCount);
				buf.writeLong(value.recordStartTick);
				buf.writeBoolean(value.discStack.isPresent());
				value.discStack.ifPresent(stack -> ItemStack.PACKET_CODEC.encode(buf, stack));
				buf.writeString(value.songTitle);
				buf.writeFloat(value.totalSeconds);
				buf.writeBoolean(value.isFinished);
				buf.writeBlockPos(value.pos);
			},
			buf -> new Payload(
				buf.readBoolean(),
				buf.readLong(),
				buf.readLong(),
				buf.readBoolean() ? Optional.of(ItemStack.PACKET_CODEC.decode(buf)) : Optional.empty(),
				buf.readString(),
				buf.readFloat(),
				buf.readBoolean(),
				buf.readBlockPos()
			)
		);

		@Override
		public Id<? extends CustomPayload> getId() { return ID; }
	}

	public static void registerServerPackets() {
		PayloadTypeRegistry.playS2C().register(Payload.ID, Payload.CODEC);
	}

	public static void sendToClient(ServerPlayerEntity player, JukeboxBlockEntity jukebox, BlockPos pos) {
		ItemStack disc = jukebox.getStack(0);
		Optional<ItemStack> discOpt = disc.isEmpty() ? Optional.empty() : Optional.of(disc);

		String songTitle = "";
		float totalSeconds = 0f;

		// Get registry from the jukebox's own world — avoids any player world getter issues
		RegistryWrapper.WrapperLookup registries =
			((net.minecraft.server.world.ServerWorld) jukebox.getWorld()).getRegistryManager();

		if (discOpt.isPresent()) {
			var songOpt = JukeboxSong.getSongEntryFromStack(registries, discOpt.get());
			if (songOpt.isPresent()) {
				JukeboxSong song = songOpt.get().value();
				net.minecraft.text.Text desc = song.description();
				if (desc != null) {
					songTitle = desc.getString();
				} else {
					// Fallback: use item name if description is null
					songTitle = discOpt.get().getItem().getName().getString();
				}
				totalSeconds = song.lengthInSeconds();
			} else {
				// Fallback: use item name if song entry not found
				songTitle = discOpt.get().getItem().getName().getString();
				// Estimate duration for unknown discs (default to 3 minutes)
				totalSeconds = 180f;
			}
		}

		// 1.21.10 jukebox NBT only has "ticks_since_song_started"
		NbtCompound nbt = jukebox.createNbt(registries);
		long ticksSinceSongStarted = nbt.getLong("ticks_since_song_started", -1L);
		boolean isPlaying = ticksSinceSongStarted >= 0;
		long tickCount = isPlaying ? ticksSinceSongStarted : 0L;
		long recordStartTick = 0L;

		// Song is finished if disc is present, not playing, and ticks ran past song length
		boolean isFinished = !disc.isEmpty() && !isPlaying && totalSeconds > 0
			&& ticksSinceSongStarted < 0; // ticks_since_song_started absent means song ended naturally

		ServerPlayNetworking.send(player, new Payload(
			isPlaying, tickCount, recordStartTick,
			discOpt, songTitle, totalSeconds, isFinished, pos
		));
	}
}