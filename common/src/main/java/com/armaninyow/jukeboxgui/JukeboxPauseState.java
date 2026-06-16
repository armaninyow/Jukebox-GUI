package com.armaninyow.jukeboxgui;

import net.minecraft.core.BlockPos;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Shared pause state accessible from both server (mixin) and packet handler. */
public class JukeboxPauseState {
    public static final Set<BlockPos> serverPausedPositions = ConcurrentHashMap.newKeySet();
}