package com.armaninyow.jukeboxgui.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Renders a 15x15 disc texture upscaled to 45x45 with UV-based rotation.
 *
 * Uses a 15x15 grid where each sub-quad is 3x3 screen pixels = 1 texture pixel.
 * Each sub-quad gets inverse-rotated UV coordinates. NEAREST sampling gives
 * clean chunky Minecrafty 3x3 pixel blocks.
 */
@Environment(EnvType.CLIENT)
public class DiscRotationRenderState {

    private static final int GRID      = 15;
    private static final int TEX_SRC   = 15;
    private static final int CELL_SIZE = 3;

    public static void submit(
        GuiGraphicsExtractor graphics,
        Identifier texture,
        float absX, float absY, float size,
        float angleDeg
    ) {
        float half = size / 2f;
        float cx   = absX + half;
        float cy   = absY + half;
        float rad  = (float) Math.toRadians(angleDeg);
        float cosA = (float) Math.cos(rad);
        float sinA = (float) Math.sin(rad);

        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                float sx0 = absX + col * CELL_SIZE;
                float sy0 = absY + row * CELL_SIZE;
                float sx1 = sx0 + CELL_SIZE;
                float sy1 = sy0 + CELL_SIZE;

                // Use center of screen cell for UV lookup
                float scx = (sx0 + sx1) / 2f;
                float scy = (sy0 + sy1) / 2f;

                // Inverse-rotate cell center to texture space
                float dx = scx - cx;
                float dy = scy - cy;
                float lx =  cosA * dx + sinA * dy;
                float ly = -sinA * dx + cosA * dy;

                // Map to UV [0..1]
                float u = (lx + half) / size;
                float v = (ly + half) / size;

                // Skip if outside texture
                if (u < 0f || u > 1f || v < 0f || v > 1f) continue;

                // Map UV to exact texture pixel
                int texCol = Math.max(0, Math.min(TEX_SRC - 1, (int)(u * TEX_SRC)));
                int texRow = Math.max(0, Math.min(TEX_SRC - 1, (int)(v * TEX_SRC)));

                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    texture,
                    (int) sx0, (int) sy0,
                    texCol, texRow,
                    CELL_SIZE, CELL_SIZE,
                    1, 1,
                    TEX_SRC, TEX_SRC
                );
            }
        }
    }
}