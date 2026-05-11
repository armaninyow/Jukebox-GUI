package com.armaninyow.jukeboxgui;

import com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiRefreshPacket;
import com.armaninyow.jukeboxgui.screen.JukeboxScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 1.21.5
public class JukeboxGUI implements ModInitializer {
	public static final String MOD_ID = "jukeboxgui";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Extended type sends BlockPos to the client constructor so the screen knows its jukebox. */
	public static ExtendedScreenHandlerType<JukeboxScreenHandler, BlockPos> JUKEBOX_SCREEN_HANDLER;

	@Override
	public void onInitialize() {
		// Register screen handler type — BlockPos codec sends pos to client constructor
		JUKEBOX_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of(MOD_ID, "jukebox"),
			new ExtendedScreenHandlerType<>(
				(syncId, playerInv, pos) -> new JukeboxScreenHandler(syncId, playerInv, pos),
				BlockPos.PACKET_CODEC
			)
		);

		// Register S2C open-gui packet
		JukeboxGuiPacket.registerServerPackets();

		// Register C2S refresh packet
		PayloadTypeRegistry.playC2S().register(JukeboxGuiRefreshPacket.ID, JukeboxGuiRefreshPacket.CODEC);

		// Register C2S action packet
		PayloadTypeRegistry.playC2S().register(JukeboxGuiActionPacket.ID, JukeboxGuiActionPacket.CODEC);

		// Handle refresh: re-send jukebox state to client
		ServerPlayNetworking.registerGlobalReceiver(JukeboxGuiRefreshPacket.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			BlockPos pos = payload.pos();
			ServerWorld world = (ServerWorld) player.getEntityWorld();
			if (!(world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) return;
			JukeboxGuiPacket.sendToClient(player, jukebox, pos);
		});

		// Handle GUI actions sent from the client
		ServerPlayNetworking.registerGlobalReceiver(JukeboxGuiActionPacket.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			BlockPos pos = payload.pos();
			ServerWorld world = (ServerWorld) player.getEntityWorld();
			if (!(world.getBlockState(pos).getBlock() instanceof JukeboxBlock)) return;
			if (!(world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) return;

			switch (payload.action()) {
				case TOGGLE_PLAY -> {
					if (!jukebox.getStack(0).isEmpty()) {
						if (jukebox.getManager().isPlaying()) {
							jukebox.getManager().stopPlaying(world, null);
							world.syncWorldEvent(WorldEvents.JUKEBOX_STOPS_PLAYING, pos, 0);
							world.setBlockState(pos, world.getBlockState(pos)
								.with(JukeboxBlock.HAS_RECORD, false), 3);
						} else {
							world.setBlockState(pos, world.getBlockState(pos)
								.with(JukeboxBlock.HAS_RECORD, true), 3);
						}
						jukebox.markDirty();
						JukeboxGuiPacket.sendToClient(player, jukebox, pos);
					}
				}
				case EJECT_DISC -> {
					ItemStack disc = jukebox.getStack(0);
					if (!disc.isEmpty()) {
						jukebox.getManager().stopPlaying(world, null);
						world.syncWorldEvent(WorldEvents.JUKEBOX_STOPS_PLAYING, pos, 0);
						jukebox.setStack(0, ItemStack.EMPTY);
						world.setBlockState(pos, world.getBlockState(pos)
							.with(JukeboxBlock.HAS_RECORD, false), 3);
						jukebox.markDirty();
						if (!player.getInventory().insertStack(disc)) {
							player.dropItem(disc, false);
						}
						JukeboxGuiPacket.sendToClient(player, jukebox, pos);
					}
				}
				case INSERT_DISC -> {
					ItemStack cursorItem = player.currentScreenHandler.getCursorStack();
					if (!cursorItem.isEmpty()
						&& cursorItem.contains(DataComponentTypes.JUKEBOX_PLAYABLE)
						&& jukebox.getStack(0).isEmpty()) {
						ItemStack toInsert = cursorItem.copyWithCount(1);
						cursorItem.decrement(1);
						jukebox.setStack(0, toInsert);
						world.setBlockState(pos, world.getBlockState(pos)
							.with(JukeboxBlock.HAS_RECORD, true), 3);
						jukebox.markDirty();
						JukeboxGuiPacket.sendToClient(player, jukebox, pos);
					}
				}
			}
		});

		// Intercept Shift+Right-Click on a jukebox to open the custom GUI
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
			if (!player.isSneaking()) return ActionResult.PASS;

			BlockPos pos = hitResult.getBlockPos();
			if (!(world.getBlockState(pos).getBlock() instanceof JukeboxBlock)) return ActionResult.PASS;

			// On the client, return SUCCESS as soon as we confirm it's a jukebox + sneak.
			// This cancels vanilla's client-side item-use prediction (block ghost / hotbar flicker)
			// regardless of what the player is holding.
			if (world.isClient()) return ActionResult.SUCCESS;

			if (!(world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) return ActionResult.PASS;

			if (player instanceof ServerPlayerEntity serverPlayer) {
				ServerWorld serverWorld = (ServerWorld) world;

				// Proxy inventory mirrors slot changes back into the real jukebox
				final boolean[] initializing = {true};
				SimpleInventory proxyInv = new SimpleInventory(1) {
					@Override
					public void markDirty() {
						super.markDirty();
						if (initializing[0]) return;
						ItemStack newDisc = this.getStack(0);
						ItemStack current = jukebox.getStack(0);
						if (!ItemStack.areEqual(newDisc, current)) {
							if (!current.isEmpty()) {
								jukebox.getManager().stopPlaying(serverWorld, null);
								serverWorld.syncWorldEvent(WorldEvents.JUKEBOX_STOPS_PLAYING, pos, 0);
								serverWorld.setBlockState(pos, serverWorld.getBlockState(pos)
									.with(JukeboxBlock.HAS_RECORD, false), 3);
							}
							if (newDisc.isEmpty()) {
								jukebox.setStack(0, ItemStack.EMPTY);
							} else {
								jukebox.setStack(0, newDisc.copy());
								serverWorld.setBlockState(pos, serverWorld.getBlockState(pos)
									.with(JukeboxBlock.HAS_RECORD, true), 3);
							}
							jukebox.markDirty();
							JukeboxGuiPacket.sendToClient(serverPlayer, jukebox, pos);
						}
					}
				};
				proxyInv.setStack(0, jukebox.getStack(0).copy());
				initializing[0] = false;

				serverPlayer.openHandledScreen(
					new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<BlockPos>() {
						@Override
						public net.minecraft.screen.ScreenHandler createMenu(int syncId,
						    net.minecraft.entity.player.PlayerInventory playerInv,
						    net.minecraft.entity.player.PlayerEntity p) {
							return new JukeboxScreenHandler(syncId, playerInv, proxyInv,
								ScreenHandlerContext.create(world, pos), pos);
						}

						@Override
						public Text getDisplayName() {
							return Text.translatable("container.jukebox");
						}

						@Override
						public BlockPos getScreenOpeningData(ServerPlayerEntity p) {
							return pos;
						}
					}
				);

				JukeboxGuiPacket.sendToClient(serverPlayer, jukebox, pos);
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});
	}
}