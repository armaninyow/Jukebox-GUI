package com.armaninyow.jukeboxgui;

import com.armaninyow.jukeboxgui.network.JukeboxGuiActionPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPacket;
import com.armaninyow.jukeboxgui.network.JukeboxGuiRefreshPacket;
import com.armaninyow.jukeboxgui.screen.JukeboxScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 26.1.x (Mojang mappings + Fabric API 0.149.0)
public class JukeboxGUI implements ModInitializer {
    public static final String MOD_ID = "jukeboxgui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ExtendedMenuType<JukeboxScreenHandler, BlockPos> JUKEBOX_SCREEN_HANDLER;

    @Override
    public void onInitialize() {
        JUKEBOX_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(MOD_ID, "jukebox"),
            new ExtendedMenuType<>(
                (syncId, playerInv, pos) -> new JukeboxScreenHandler(syncId, playerInv, pos),
                BlockPos.STREAM_CODEC
            )
        );

        JukeboxGuiPacket.registerServerPackets();

        PayloadTypeRegistry.serverboundPlay().register(JukeboxGuiRefreshPacket.ID, JukeboxGuiRefreshPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(JukeboxGuiActionPacket.ID, JukeboxGuiActionPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(JukeboxGuiRefreshPacket.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            BlockPos pos = payload.pos();
            ServerLevel world = (ServerLevel) player.level();
            if (!(world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) return;
            JukeboxGuiPacket.sendToClient(player, jukebox, pos);
        });

        ServerPlayNetworking.registerGlobalReceiver(JukeboxGuiActionPacket.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            BlockPos pos = payload.pos();
            ServerLevel world = (ServerLevel) player.level();
            if (!(world.getBlockState(pos).getBlock() instanceof JukeboxBlock)) return;
            if (!(world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) return;

            switch (payload.action()) {
                case TOGGLE_PLAY -> {
                    if (!jukebox.getTheItem().isEmpty()) {
                        if (jukebox.getSongPlayer().isPlaying()) {
                            // stop(level, blockState)
                            jukebox.getSongPlayer().stop(world, world.getBlockState(pos));
                        } else {
                            // Re-insert triggers play via setTheItem
                            ItemStack disc = jukebox.getTheItem();
                            jukebox.setTheItem(disc);
                        }
                        JukeboxGuiPacket.sendToClient(player, jukebox, pos);
                    }
                }
                case EJECT_DISC -> {
                    ItemStack disc = jukebox.getTheItem().copy();
                    if (!disc.isEmpty()) {
                        // setTheItem(EMPTY) calls stop internally and updates block state
                        jukebox.setTheItem(ItemStack.EMPTY);
                        if (!player.getInventory().add(disc)) {
                            player.drop(disc, false);
                        }
                        JukeboxGuiPacket.sendToClient(player, jukebox, pos);
                    }
                }
                case INSERT_DISC -> {
                    ItemStack cursorItem = player.containerMenu.getCarried();
                    if (!cursorItem.isEmpty()
                        && cursorItem.has(DataComponents.JUKEBOX_PLAYABLE)
                        && jukebox.getTheItem().isEmpty()) {
                        ItemStack toInsert = cursorItem.copyWithCount(1);
                        cursorItem.shrink(1);
                        // setTheItem calls play internally and updates block state
                        jukebox.setTheItem(toInsert);
                        JukeboxGuiPacket.sendToClient(player, jukebox, pos);
                    }
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            if (!(world.getBlockState(pos).getBlock() instanceof JukeboxBlock)) return InteractionResult.PASS;

            if (world.isClientSide()) return InteractionResult.SUCCESS;

            if (!(world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) return InteractionResult.PASS;

            if (player instanceof ServerPlayer serverPlayer) {
                ServerLevel serverWorld = (ServerLevel) world;

                // Proxy that mirrors slot changes back to the jukebox
                final boolean[] initializing = {true};
                SimpleContainer proxyInv = new SimpleContainer(1) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        if (initializing[0]) return;
                        ItemStack newDisc = this.getItem(0);
                        ItemStack current = jukebox.getTheItem();
                        if (!ItemStack.matches(newDisc, current)) {
                            // setTheItem handles stop/play/blockstate internally
                            jukebox.setTheItem(newDisc.isEmpty() ? ItemStack.EMPTY : newDisc.copy());
                            JukeboxGuiPacket.sendToClient(serverPlayer, jukebox, pos);
                        }
                    }
                };
                proxyInv.setItem(0, jukebox.getTheItem().copy());
                initializing[0] = false;

                serverPlayer.openMenu(new ExtendedMenuProvider<BlockPos>() {
                    @Override
                    public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player p) {
                        return new JukeboxScreenHandler(syncId, playerInv, proxyInv,
                            ContainerLevelAccess.create(world, pos), pos);
                    }
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.jukebox");
                    }
                    @Override
                    public BlockPos getScreenOpeningData(ServerPlayer p) {
                        return pos;
                    }
                });

                JukeboxGuiPacket.sendToClient(serverPlayer, jukebox, pos);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
