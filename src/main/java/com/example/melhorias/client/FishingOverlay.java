package com.example.melhorias.client;

import com.example.melhorias.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.Util;

import java.util.Locale;

/**
 * Ciclo da pesca na tela (só visual, não bloqueia nada):
 *  WAITING : começa quando o anzol toca a água; barra "PESCANDO..." com suspense.
 *  FILLING : o jogador recolheu com peixe; a barra completa rápido.
 *  REVEAL  : mostra ícone e nome do item; dura até o jogador pegar o item.
 *  ENDING  : some suavemente.
 */
public final class FishingOverlay {
    private FishingOverlay() {}

    private enum Phase { IDLE, WAITING, FILLING, REVEAL, ENDING }

    private static Phase phase = Phase.IDLE;
    private static long phaseStart;
    private static long waitStart;
    private static long endingStart;
    private static long hookGoneAt = -1;
    private static float fillFrom;
    private static ItemStack stack = ItemStack.EMPTY;
    private static int itemId = -1;
    private static boolean seenItem;
    private static boolean sounded;

    private static final float REVEAL_MS = 500f;
    private static final int[] FLASH = {35, 60, 95, 130}; // intensidade do clarão por raridade

    private static long now() { return Util.getMeasuringTimeMs(); }

    private static void set(Phase p) {
        phase = p;
        phaseStart = now();
    }

    private static void reset() {
        phase = Phase.IDLE;
        stack = ItemStack.EMPTY;
        itemId = -1;
        seenItem = false;
        sounded = false;
        hookGoneAt = -1;
    }

    private static void beginEnding() {
        if (phase == Phase.REVEAL) {
            endingStart = now();
            phase = Phase.ENDING;
        }
    }

    private static float waitProgress() {
        float t = now() - waitStart;
        return 0.9f * (1f - (float) Math.exp(-t / 6000f));
    }

    // ------------------------------------------------------------ eventos

    /** O servidor avisou: o jogador recolheu a vara e pegou este item. */
    public static void onCaught(ItemStack s, int entityId) {
        if (!ModConfig.get().fishingUi) return;
        float current = 0.6f;
        if (phase == Phase.WAITING) {
            current = waitProgress();
        } else {
            waitStart = now();
        }
        stack = s;
        itemId = entityId;
        seenItem = false;
        sounded = false;
        fillFrom = current;
        set(Phase.FILLING);
    }

    /** Roda todo tick do cliente: detecta quando o anzol toca a água e quando o item é pego. */
    public static void clientTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }
        if (!ModConfig.get().fishingUi) {
            if (phase != Phase.IDLE) reset();
            return;
        }
        FishingBobberEntity hook = mc.player.fishHook;
        boolean hookOk = hook != null && !hook.isRemoved();
        long t = now();

        switch (phase) {
            case IDLE -> {
                if (hookOk && hook.isTouchingWater()) {
                    waitStart = t;
                    hookGoneAt = -1;
                    set(Phase.WAITING);
                }
            }
            case WAITING -> {
                if (hookOk) {
                    hookGoneAt = -1;
                } else if (hookGoneAt < 0) {
                    hookGoneAt = t;
                } else if (t - hookGoneAt > 600) {
                    reset(); // recolheu sem pescar nada
                }
            }
            case REVEAL -> {
                if (itemId >= 0) {
                    Entity e = mc.world.getEntityById(itemId);
                    if (e != null) seenItem = true;
                    else if (seenItem) beginEnding(); // o jogador pegou o item
                }
                if (phase == Phase.REVEAL && t - phaseStart > 8000) beginEnding();
                if (phase == Phase.REVEAL && hookOk && hook.isTouchingWater() && t - phaseStart > 1500) {
                    // lançou a vara de novo
                    reset();
                    waitStart = t;
                    set(Phase.WAITING);
                }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------ desenho

    public static void render(DrawContext ctx) {
        if (phase == Phase.IDLE) return;
        long t = now();
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 5;

        switch (phase) {
            case WAITING -> {
                int a = (int) Math.min(255, (t - phaseStart) * 255 / 300);
                if (a >= 8) drawBar(ctx, tr, cx, cy, waitProgress(), t - waitStart, a);
            }
            case FILLING -> {
                float k = Math.min(1f, (t - phaseStart) / 450f);
                drawBar(ctx, tr, cx, cy, fillFrom + (1f - fillFrom) * smooth(k), t - waitStart, 255);
                if (k >= 1f) set(Phase.REVEAL);
            }
            case REVEAL -> drawReveal(ctx, tr, mc, cx, cy, t - phaseStart, 255);
            case ENDING -> {
                float k = (t - endingStart) / 400f;
                if (k >= 1f) {
                    reset();
                    return;
                }
                int a = (int) (255 * (1f - k));
                if (a >= 8) drawReveal(ctx, tr, mc, cx, cy, t - phaseStart, a);
            }
            default -> { }
        }
    }

    private static void drawBar(DrawContext ctx, TextRenderer tr, int cx, int cy, float p, float tMs, int a) {
        int dots = 1 + (int) (tMs / 300f) % 3;
        ctx.drawCenteredTextWithShadow(tr, Text.literal("PESCANDO" + ".".repeat(dots)), cx, cy - 16, col(0xFFFFFF, a));

        int bw = 180, bh = 10, slot = 20, gap = 6;
        int xs = cx - (slot + gap + bw) / 2;
        int slotX = xs + (p > 0.7f ? Math.round((float) Math.sin(tMs / 40.0)) : 0);
        int slotY = cy + bh / 2 - slot / 2;
        int barX = xs + slot + gap;
        int barY = slotY + 5;

        // caixa do "?"
        ctx.fill(slotX - 1, slotY - 1, slotX + slot + 1, slotY + slot + 1, col(0x000000, a));
        ctx.fill(slotX, slotY, slotX + slot, slotY + slot, col(0x3A3A3A, a));
        ctx.fill(slotX + 1, slotY + 1, slotX + slot - 1, slotY + slot - 1, col(0x1A1A1A, a));
        float pulse = 1.4f + 0.25f * (float) Math.sin(tMs / 120.0);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(slotX + slot / 2f, slotY + slot / 2f, 0f);
        ctx.getMatrices().scale(pulse, pulse, 1f);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("?"), 0, -4, col(0xFFD84A, a));
        ctx.getMatrices().pop();

        // barra
        ctx.fill(barX - 2, barY - 2, barX + bw + 2, barY + bh + 2, col(0x000000, a));
        ctx.fill(barX - 1, barY - 1, barX + bw + 1, barY + bh + 1, col(0x3A3A3A, a));
        ctx.fill(barX, barY, barX + bw, barY + bh, col(0x151515, a));
        int fill = (int) (bw * Math.max(0f, Math.min(1f, p)));
        ctx.fill(barX, barY, barX + fill, barY + bh, col(0x3FA9F5, a));
        ctx.fill(barX, barY, barX + fill, barY + 3, col(0x9AD8FF, a / 2));
    }

    private static float smooth(float x) { return x * x * (3f - 2f * x); }

    private static void drawReveal(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int cx, int cy, float rt, int a) {
        float u = Math.min(1f, rt / REVEAL_MS);
        Rarity rarity = stack.getRarity();
        int tier = rarity.ordinal();
        int rgb = switch (rarity) {
            case COMMON -> 0xFFFFFF;
            case UNCOMMON -> 0xFFFF55;
            case RARE -> 0x55FFFF;
            case EPIC -> 0xFF55FF;
        };

        if (!sounded) {
            sounded = true;
            if (tier >= 2) {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2f, 0.6f));
            } else if (tier == 1) {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.4f, 0.7f));
            } else {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.35f));
            }
        }

        int iconY = cy + 14;

        // clarão rápido
        float flashT = rt / 400f;
        if (flashT < 1f) {
            int fa = (int) (FLASH[Math.min(tier, 3)] * (1f - flashT) * (a / 255f));
            if (fa > 0) ctx.fill(cx - 110, cy - 28, cx + 110, cy + 66, (fa << 24) | 0xFFFFFF);
        }

        // brilhos (itens comuns: nenhum, para ficar discreto)
        if (tier >= 1) {
            int n = tier == 1 ? 6 : tier == 2 ? 14 : 20;
            int size = tier >= 2 ? 3 : 2;
            for (int i = 0; i < n; i++) {
                double ang = i * (Math.PI * 2 / n) + rt * 0.002;
                double dist = 16 + 34 * u + Math.sin(rt * 0.006 + i) * 3;
                int sx = cx + (int) (Math.cos(ang) * dist);
                int sy = iconY + (int) (Math.sin(ang) * dist);
                int sa = (int) (a * (0.35 + 0.65 * Math.abs(Math.sin(rt * 0.008 + i * 1.7))));
                if (sa >= 8) ctx.fill(sx, sy, sx + size, sy + size, col(rgb, sa));
            }
        }

        int ta = (int) (a * u);
        if (ta >= 8) ctx.drawCenteredTextWithShadow(tr, Text.literal("VOCÊ PESCOU"), cx, cy - 16, col(0xAAAAAA, ta));

        // ícone com "pulinho"
        float s = 2.0f * easeOutBack(u);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, iconY, 0f);
        ctx.getMatrices().scale(s, s, 1f);
        ctx.drawItem(stack, -8, -8);
        ctx.getMatrices().pop();

        // nome
        String name = stack.getName().getString().toUpperCase(Locale.ROOT);
        if (stack.getCount() > 1) name += " x" + stack.getCount();
        if (ta >= 8) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(cx, iconY + 26, 0f);
            ctx.getMatrices().scale(1.3f, 1.3f, 1f);
            ctx.drawCenteredTextWithShadow(tr, Text.literal(name), 0, 0, col(rgb, ta));
            ctx.getMatrices().pop();

            if (tier >= 2) {
                String tag = tier == 2 ? "ITEM RARO!" : "ITEM ÉPICO!";
                ctx.drawCenteredTextWithShadow(tr, Text.literal(tag), cx, iconY + 42, col(0xFFAA00, ta));
            }
        }
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        float y = x - 1f;
        return 1f + c3 * y * y * y + c1 * y * y;
    }

    private static int col(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }
}
