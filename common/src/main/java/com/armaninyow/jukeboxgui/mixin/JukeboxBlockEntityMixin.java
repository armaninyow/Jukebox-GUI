package com.armaninyow.jukeboxgui.mixin;

import com.armaninyow.jukeboxgui.IJukeboxSongPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlockEntity.class)
public class JukeboxBlockEntityMixin {

    @Shadow
    private JukeboxSongPlayer jukeboxSongPlayer;

    @Inject(method = "setTheItem", at = @At("HEAD"))
    private void onSetTheItem(ItemStack stack, CallbackInfo ci) {
        if (jukeboxSongPlayer != null) {
            ((IJukeboxSongPlayer) jukeboxSongPlayer).jukeboxgui$setPaused(false);
        }
    }
}