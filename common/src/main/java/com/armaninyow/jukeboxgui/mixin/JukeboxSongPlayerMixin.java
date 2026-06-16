package com.armaninyow.jukeboxgui.mixin;

import com.armaninyow.jukeboxgui.IJukeboxSongPlayer;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxSongPlayer.class)
public class JukeboxSongPlayerMixin implements IJukeboxSongPlayer {

    @Unique
    private boolean jukeboxgui$paused = false;

    @Override
    public boolean jukeboxgui$isPaused() { return jukeboxgui$paused; }

    @Override
    public void jukeboxgui$setPaused(boolean paused) { jukeboxgui$paused = paused; }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(LevelAccessor level, BlockState state, CallbackInfo ci) {
        if (jukeboxgui$paused) {
            ci.cancel();
        }
    }
}