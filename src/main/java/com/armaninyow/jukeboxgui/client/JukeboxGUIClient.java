package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import com.armaninyow.jukeboxgui.network.JukeboxGuiPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@Environment(EnvType.CLIENT)
public class JukeboxGUIClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// Register the screen for our screen handler type
		HandledScreens.register(JukeboxGUI.JUKEBOX_SCREEN_HANDLER, JukeboxManagementScreen::new);

		// Clear persistent data when joining world (like vanilla discs)
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			JukeboxManagementScreen.clearPersistentData();
		});

		// S2C: when the server sends jukebox state, update the open screen in-place
		ClientPlayNetworking.registerGlobalReceiver(JukeboxGuiPacket.Payload.ID, (payload, context) -> {
			context.client().execute(() -> {
				MinecraftClient client = context.client();
				if (client.currentScreen instanceof JukeboxManagementScreen screen
					&& screen.getPos().equals(payload.pos())) {
					screen.updateData(payload);
				}
			});
		});
	}
}