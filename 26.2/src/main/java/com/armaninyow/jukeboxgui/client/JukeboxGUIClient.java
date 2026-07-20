package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class JukeboxGUIClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(JukeboxGUI.JUKEBOX_SCREEN_HANDLER, JukeboxManagementScreen::new);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            JukeboxManagementScreen.clearPersistentData();
        });

        ClientPlayNetworking.registerGlobalReceiver(JukeboxGuiPacket.Payload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft client = context.client();

                BlockPos pos = payload.pos();
                ItemStack incoming = payload.discStack().orElse(ItemStack.EMPTY);
                if (JukeboxManagementScreen.pausedPositions.contains(pos)) {
                    if (incoming.isEmpty()) {
                        JukeboxManagementScreen.pausedPositions.remove(pos);
                        JukeboxManagementScreen.pausedElapsedMap.remove(pos);
                    }
                }
                if (incoming.isEmpty()) {
                    JukeboxManagementScreen.clientPlayedPositions.remove(pos);
                }

                if (client.gui.screen() instanceof JukeboxManagementScreen screen
                    && screen.getPos().equals(pos)) {
                    screen.updateData(payload);
                }
            });
        });
    }
}