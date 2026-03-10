package com.armaninyow.jukeboxgui.mixin;

import net.minecraft.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(JukeboxBlockEntity.class)
public interface JukeboxBlockEntityAccessor {

	@Accessor("isPlaying")
	boolean isPlaying();

	@Accessor("isPlaying")
	void setIsPlaying(boolean isPlaying);

	@Accessor("tickCount")
	long getTickCount();

	@Accessor("recordStartTick")
	long getRecordStartTick();

	@Accessor("recordStartTick")
	void setRecordStartTick(long tick);

	@Invoker("startPlaying")
	void invokeStartPlaying();

	@Invoker("stopPlaying")
	void invokeStopPlaying();
}