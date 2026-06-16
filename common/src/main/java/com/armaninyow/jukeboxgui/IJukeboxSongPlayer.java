package com.armaninyow.jukeboxgui;

/** Interface added to JukeboxSongPlayer via mixin to allow pause/resume. */
public interface IJukeboxSongPlayer {
    boolean jukeboxgui$isPaused();
    void jukeboxgui$setPaused(boolean paused);
}