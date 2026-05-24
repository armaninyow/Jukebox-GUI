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
                if (client.screen instanceof JukeboxManagementScreen screen
                    && screen.getPos().equals(payload.pos())) {
                    screen.updateData(payload);
                }
            });
        });
    }
}
