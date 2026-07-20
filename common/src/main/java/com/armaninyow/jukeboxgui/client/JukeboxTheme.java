package com.armaninyow.jukeboxgui.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public interface JukeboxTheme {

    int id();

    Identifier toggleIcon();

    void render(JukeboxManagementScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    boolean handleClick(JukeboxManagementScreen screen, MouseButtonEvent event);

    int playPauseBtnX();

    int playPauseBtnY();

    int playPauseBtnSize();

    boolean skipSlot(int slotIndex);
}