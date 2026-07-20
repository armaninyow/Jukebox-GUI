package com.armaninyow.jukeboxgui.client;

import com.armaninyow.jukeboxgui.JukeboxGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JukeboxBandData {

    private static final float[] FIXED_MAX_DB = {
        -44f, -44f, -45f, -46f, -46f, -47f, -48f, -49f,
        -50f, -51f, -52f, -54f, -56f, -58f, -61f, -64f
    };
    private static final float[] FIXED_MIN_DB = {
        -59f, -59f, -60f, -61f, -61f, -62f, -63f, -64f,
        -65f, -66f, -67f, -69f, -71f, -73f, -76f, -79f
    };

    private static final Map<String, LoudnessProfile> CACHE = new HashMap<>();
    private static final Map<String, Boolean> MISSING = new HashMap<>();

    private JukeboxBandData() {}

    private record LoudnessProfile(float[] dbValues, float samplesPerSecond) {}

    public static String bandFileSuffix(int bandIndex) {
        return String.format("band%02d", bandIndex + 1);
    }

    public static float getNormalizedLevel(String discName, int bandIndex, float elapsedSeconds, float totalSeconds) {
        float value = getRawDb(discName, bandIndex, elapsedSeconds, totalSeconds);
        if (value == Float.NEGATIVE_INFINITY) return 1f;

        int b = Math.max(0, Math.min(FIXED_MAX_DB.length - 1, bandIndex));
        float min = FIXED_MIN_DB[b];
        float max = FIXED_MAX_DB[b];
        return Math.max(0f, Math.min(1f, (value - min) / (max - min)));
    }

    public static float getRawDb(String discName, int bandIndex, float elapsedSeconds, float totalSeconds) {
        if (discName == null) return Float.NEGATIVE_INFINITY;
        LoudnessProfile profile = load(discName, bandIndex);
        if (profile == null) return Float.NEGATIVE_INFINITY;

        float[] db = profile.dbValues();
        if (db.length == 0) return Float.NEGATIVE_INFINITY;

        float rate = profile.samplesPerSecond() > 0 ? profile.samplesPerSecond()
            : (totalSeconds > 0 ? db.length / totalSeconds : 1f);

        float exactIndex = elapsedSeconds * rate;
        int i0 = Math.max(0, Math.min(db.length - 1, (int) Math.floor(exactIndex)));
        int i1 = Math.min(db.length - 1, i0 + 1);
        float frac = exactIndex - (float) Math.floor(exactIndex);
        return db[i0] + (db[i1] - db[i0]) * frac;
    }

    public static float getCombinedDb(String discName, float elapsedSeconds, float totalSeconds) {
        if (discName == null) return Float.NEGATIVE_INFINITY;
        double energySum = 0;
        boolean anyData = false;
        for (int b = 0; b < FIXED_MAX_DB.length; b++) {
            float db = getRawDb(discName, b, elapsedSeconds, totalSeconds);
            if (db == Float.NEGATIVE_INFINITY) continue;
            energySum += Math.pow(10.0, db / 10.0);
            anyData = true;
        }
        if (!anyData || energySum <= 0) return Float.NEGATIVE_INFINITY;
        return (float) (10.0 * Math.log10(energySum));
    }

    private static final float FIXED_ONSET_MAX_DB = -26f;
    private static final float FIXED_ONSET_MIN_DB = -41f;
    private static final String ONSET_FILE_SUFFIX = "onset";

    public static float getOnsetNormalizedLevel(String discName, float elapsedSeconds, float totalSeconds) {
        float value = getOnsetRawDb(discName, elapsedSeconds, totalSeconds);
        if (value == Float.NEGATIVE_INFINITY) return 1f;
        return Math.max(0f, Math.min(1f, (value - FIXED_ONSET_MIN_DB) / (FIXED_ONSET_MAX_DB - FIXED_ONSET_MIN_DB)));
    }

    public static float getOnsetRawDb(String discName, float elapsedSeconds, float totalSeconds) {
        if (discName == null) return Float.NEGATIVE_INFINITY;
        LoudnessProfile profile = load(discName, ONSET_FILE_SUFFIX);
        if (profile == null) return Float.NEGATIVE_INFINITY;

        float[] db = profile.dbValues();
        if (db.length == 0) return Float.NEGATIVE_INFINITY;

        float rate = profile.samplesPerSecond() > 0 ? profile.samplesPerSecond()
            : (totalSeconds > 0 ? db.length / totalSeconds : 1f);

        float exactIndex = elapsedSeconds * rate;
        int i0 = Math.max(0, Math.min(db.length - 1, (int) Math.floor(exactIndex)));
        int i1 = Math.min(db.length - 1, i0 + 1);
        float frac = exactIndex - (float) Math.floor(exactIndex);
        return db[i0] + (db[i1] - db[i0]) * frac;
    }

    private static LoudnessProfile load(String discName, int bandIndex) {
        return load(discName, bandFileSuffix(bandIndex));
    }

    private static LoudnessProfile load(String discName, String fileSuffix) {
        String key = discName + "_" + fileSuffix;
        LoudnessProfile cached = CACHE.get(key);
        if (cached != null) return cached;
        if (Boolean.TRUE.equals(MISSING.get(key))) return null;

        Identifier loc = Identifier.fromNamespaceAndPath(JukeboxGUI.MOD_ID,
            "bands/" + discName + "/" + fileSuffix + ".txt");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (resource.isEmpty()) {
            MISSING.put(key, true);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
            List<Float> values = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                values.add(Float.parseFloat(line));
            }
            float[] arr = new float[values.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = values.get(i);

            LoudnessProfile profile = new LoudnessProfile(arr, 0f);
            CACHE.put(key, profile);
            return profile;
        } catch (IOException | NumberFormatException e) {
            JukeboxGUI.LOGGER.warn("Failed to load band data for '{}'", key, e);
            MISSING.put(key, true);
            return null;
        }
    }
}