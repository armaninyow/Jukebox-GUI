package com.armaninyow.jukeboxgui;

import net.minecraft.core.BlockPos;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JukeboxPauseState {
    public static final Set<BlockPos> serverPausedPositions = ConcurrentHashMap.newKeySet();
}