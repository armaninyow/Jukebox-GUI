package com.armaninyow.jukeboxgui.network;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import java.util.Optional;

public class JukeboxGuiPacket {

    public static final Identifier OPEN_GUI_ID =
        Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID, "open_gui");

    public record Payload(
        boolean isPlaying,
        long tickCount,
        long recordStartTick,
        Optional<ItemStack> discStack,
        String songTitle,
        float totalSeconds,
        boolean isFinished,
        BlockPos pos
    ) implements CustomPacketPayload {
        public static final Type<Payload> ID = new Type<>(OPEN_GUI_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeBoolean(value.isPlaying);
                buf.writeLong(value.tickCount);
                buf.writeLong(value.recordStartTick);
                buf.writeBoolean(value.discStack.isPresent());
                value.discStack.ifPresent(stack -> ItemStack.STREAM_CODEC.encode(buf, stack));
                buf.writeUtf(value.songTitle);
                buf.writeFloat(value.totalSeconds);
                buf.writeBoolean(value.isFinished);
                buf.writeBlockPos(value.pos);
            },
            buf -> new Payload(
                buf.readBoolean(),
                buf.readLong(),
                buf.readLong(),
                buf.readBoolean() ? Optional.of(ItemStack.STREAM_CODEC.decode(buf)) : Optional.empty(),
                buf.readUtf(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBlockPos()
            )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static void registerServerPackets() {
        PayloadTypeRegistry.clientboundPlay().register(Payload.ID, Payload.CODEC);
    }

    public static void sendToClient(ServerPlayer player, JukeboxBlockEntity jukebox, BlockPos pos) {
        ItemStack disc = jukebox.getTheItem();
        Optional<ItemStack> discOpt = disc.isEmpty() ? Optional.empty() : Optional.of(disc);

        String songTitle = "";
        float totalSeconds = 0f;

        if (discOpt.isPresent()) {
            var songOpt = JukeboxSong.fromStack(discOpt.get());
            if (songOpt.isPresent()) {
                JukeboxSong song = songOpt.get().value();
                net.minecraft.network.chat.Component desc = song.description();
                songTitle = desc != null ? desc.getString() : disc.getHoverName().getString();
                totalSeconds = song.lengthInSeconds();
            } else {
                songTitle = disc.getHoverName().getString();
                totalSeconds = 180f;
            }
        }

        boolean isPlaying = jukebox.getSongPlayer().isPlaying();
        long tickCount = isPlaying ? jukebox.getSongPlayer().getTicksSinceSongStarted() : 0L;
        boolean isFinished = !disc.isEmpty() && !isPlaying;

        ServerPlayNetworking.send(player, new Payload(
            isPlaying, tickCount, 0L,
            discOpt, songTitle, totalSeconds, isFinished, pos
        ));
    }
}
