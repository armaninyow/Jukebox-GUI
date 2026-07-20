package com.armaninyow.jukeboxgui.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JukeboxThemeRegistry {

    private static final Gson GSON = new Gson();
    private static final Map<Integer, JukeboxTheme> THEMES = new LinkedHashMap<>();

    private static int currentThemeId = 1;
    private static boolean configLoaded = false;

    private JukeboxThemeRegistry() {}

    static {
        register(new JukeboxTheme1());
        register(new JukeboxTheme2());
        register(new JukeboxTheme3());
        register(new JukeboxTheme4());
        register(new JukeboxTheme5());
        register(new JukeboxTheme6());
        register(new JukeboxTheme7());
        register(new JukeboxTheme8());
        register(new JukeboxTheme9());
        register(new JukeboxTheme10());
        register(new JukeboxTheme11());
    }

    public static void register(JukeboxTheme theme) {
        THEMES.put(theme.id(), theme);
    }

    public static JukeboxTheme current() {
        ensureLoaded();
        JukeboxTheme theme = THEMES.get(currentThemeId);
        return theme != null ? theme : THEMES.values().iterator().next();
    }

    public static int currentId() {
        ensureLoaded();
        return currentThemeId;
    }

    public static void cycleTheme() {
        ensureLoaded();
        List<Integer> ids = new ArrayList<>(THEMES.keySet());
        int idx = ids.indexOf(currentThemeId);
        int nextIdx = (idx + 1) % ids.size();
        currentThemeId = ids.get(nextIdx);
        save();
    }

    public static void cycleThemeBackward() {
        ensureLoaded();
        List<Integer> ids = new ArrayList<>(THEMES.keySet());
        int idx = ids.indexOf(currentThemeId);
        int prevIdx = (idx - 1 + ids.size()) % ids.size();
        currentThemeId = ids.get(prevIdx);
        save();
    }

    private static void ensureLoaded() {
        if (configLoaded) return;
        configLoaded = true;
        try {
            File f = configFile();
            if (!f.exists()) return;
            try (FileReader r = new FileReader(f)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                if (obj != null && obj.has("theme")) {
                    int t = obj.get("theme").getAsInt();
                    if (THEMES.containsKey(t)) currentThemeId = t;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void save() {
        try {
            File f = configFile();
            f.getParentFile().mkdirs();
            JsonObject obj = new JsonObject();
            obj.addProperty("theme", currentThemeId);
            try (FileWriter w = new FileWriter(f)) {
                GSON.toJson(obj, w);
            }
        } catch (IOException ignored) {}
    }

    private static File configFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config/jukeboxgui.json");
    }
}