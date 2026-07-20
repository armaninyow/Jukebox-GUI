package com.armaninyow.jukeboxgui.mixin;

import com.armaninyow.jukeboxgui.client.JukeboxManagementScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LevelEventHandler.class)
public class LevelEventHandlerMixin {

    @Inject(method = "playJukeboxSong", at = @At("HEAD"), cancellable = true)
    private void onPlayJukeboxSong(Holder<JukeboxSong> songHolder, BlockPos pos, CallbackInfo ci) {
        if (JukeboxManagementScreen.pausedPositions.contains(pos)) {
            ci.cancel();
            return;
        }
        JukeboxManagementScreen.clientPlayedPositions.add(pos);
    }
}