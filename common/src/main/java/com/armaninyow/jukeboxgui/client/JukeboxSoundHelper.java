package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.mixin.SoundEngineAccessor;
import com.armaninyow.jukeboxgui.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

public class JukeboxSoundHelper {

    public static boolean isChannelActive(Minecraft mc, BlockPos pos) {
        SoundEngine engine = ((SoundManagerAccessor) mc.getSoundManager()).jukeboxgui$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> map =
            ((SoundEngineAccessor) engine).jukeboxgui$getInstanceToChannel();
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : map.entrySet()) {
            SoundInstance si = entry.getKey();
            if (si.getSource() != SoundSource.RECORDS) continue;
            BlockPos soundPos = BlockPos.containing(si.getX(), si.getY(), si.getZ());
            if (!soundPos.equals(pos)) continue;
            return !entry.getValue().isStopped();
        }
        return false;
    }

    public static void pauseChannel(Minecraft mc, BlockPos pos) {
        setChannelPaused(mc, pos, true);
    }

    public static void unpauseChannel(Minecraft mc, BlockPos pos) {
        setChannelPaused(mc, pos, false);
    }

    private static void setChannelPaused(Minecraft mc, BlockPos pos, boolean pause) {
        SoundEngine engine = ((SoundManagerAccessor) mc.getSoundManager()).jukeboxgui$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> map =
            ((SoundEngineAccessor) engine).jukeboxgui$getInstanceToChannel();
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : map.entrySet()) {
            SoundInstance si = entry.getKey();
            if (si.getSource() != SoundSource.RECORDS) continue;
            BlockPos soundPos = BlockPos.containing(si.getX(), si.getY(), si.getZ());
            if (!soundPos.equals(pos)) continue;
            entry.getValue().execute(pause ? ch -> ch.pause() : ch -> ch.unpause());
            return;
        }
    }
}