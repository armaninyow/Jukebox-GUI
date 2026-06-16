package com.armaninyow.jukeboxgui.mixin;

import com.armaninyow.jukeboxgui.client.JukeboxManagementScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    /** After resume(), re-pause any channels for positions we have paused. */
    @Inject(method = "resume", at = @At("TAIL"))
    private void onResumePost(CallbackInfo ci) {
        if (!JukeboxManagementScreen.hasAnyPausedPosition()) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) return;
        for (net.minecraft.core.BlockPos pos : JukeboxManagementScreen.pausedPositions) {
            com.armaninyow.jukeboxgui.client.JukeboxSoundHelper.pauseChannel(mc, pos);
        }
    }

    /** Block anything restoring RECORDS volume while any jukebox is paused. */
    @Inject(method = "updateCategoryVolume", at = @At("HEAD"), cancellable = true)
    private void onUpdateCategoryVolume(SoundSource source, float gain, CallbackInfo ci) {
        if (source == SoundSource.RECORDS && gain > 0f
                && JukeboxManagementScreen.hasAnyPausedPosition()) {
            ci.cancel();
        }
    }
}