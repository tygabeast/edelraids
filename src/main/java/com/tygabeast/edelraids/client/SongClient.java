package com.tygabeast.edelraids.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.data.Songs;
import com.tygabeast.edelraids.item.EdelTwinItem;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.network.ModNetwork;
import com.tygabeast.edelraids.registry.ModAttachments;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** Sing and Next Song keys, and the resonance meter. Everything returns at once unless the player holds her. */
final class SongClient {
    static final KeyMapping SING = new KeyMapping("key.edelraids.sing", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.edelraids");
    static final KeyMapping NEXT_SONG = new KeyMapping("key.edelraids.next_song", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.edelraids");

    private SongClient() {}

    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SING);
        event.register(NEXT_SONG);
    }

    @Nullable
    private static EdelRaidData held(LocalPlayer p) {
        ItemStack main = p.getMainHandItem();
        return main.getItem() instanceof EdelWeaponItem || main.getItem() instanceof EdelTwinItem ? EdelWeaponItem.data(main) : null;
    }

    static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        boolean holding = held(p) != null;
        while (SING.consumeClick()) if (holding) PacketDistributor.sendToServer(new ModNetwork.Sing());
        while (NEXT_SONG.consumeClick()) if (holding) PacketDistributor.sendToServer(new ModNetwork.NextSong());
    }

    /** The meter defaults to the right of the hotbar (past the off-hand slot for left-handed players); see ClientConfig. */
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui || p.isSpectator() || !ClientConfig.SHOW_METER.get()) return;
        EdelRaidData d = held(p);
        if (d == null) return;

        ModAttachments.Resonance r = p.getData(ModAttachments.RESONANCE);
        Songs.Entry[] songs = Songs.forElement(mc.level.registryAccess(), d.element());
        int slot = Songs.effectiveSlot(d, songs, r.selected());
        Element el = Elements.element(mc.level.registryAccess(), d.element());
        int color = el == null ? 0x4FC9A0 : el.color();

        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        boolean vertical = ClientConfig.ORIENTATION.get() == ClientConfig.Orientation.VERTICAL;
        int len = ClientConfig.LENGTH.get();
        int bw = vertical ? THICKNESS : len, bh = vertical ? len : THICKNESS;
        boolean leftHanded = mc.options.mainHand().get() == HumanoidArm.LEFT;

        // Bar position from the anchor. align: -1 = the label grows rightward from the bar, 1 = leftward, 0 = centred.
        int x, y, align;
        int hotbarY = vertical ? h - 8 - bh : h - 12;
        switch (ClientConfig.ANCHOR.get()) {
            case HOTBAR_LEFT -> { x = w / 2 - 98 - (leftHanded ? 0 : 30) - bw; y = hotbarY; align = 1; }
            case TOP_LEFT -> { x = MARGIN; y = MARGIN; align = -1; }
            case TOP_CENTER -> { x = (w - bw) / 2; y = MARGIN; align = 0; }
            case TOP_RIGHT -> { x = w - MARGIN - bw; y = MARGIN; align = 1; }
            case MIDDLE_LEFT -> { x = MARGIN; y = (h - bh) / 2; align = -1; }
            case MIDDLE_RIGHT -> { x = w - MARGIN - bw; y = (h - bh) / 2; align = 1; }
            case BOTTOM_LEFT -> { x = MARGIN; y = h - MARGIN - bh; align = -1; }
            case BOTTOM_RIGHT -> { x = w - MARGIN - bw; y = h - MARGIN - bh; align = 1; }
            default -> { x = w / 2 + 98 + (leftHanded ? 30 : 0); y = hotbarY; align = -1; }
        }
        x = Math.max(1, Math.min(w - 1 - bw, x + ClientConfig.OFFSET_X.get()));
        y = Math.max(1, Math.min(h - 1 - bh, y + ClientConfig.OFFSET_Y.get()));

        int fill = Math.max(0, Math.min(100, r.value()));
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xC0000000);
        if (vertical) g.fill(x, y + bh - bh * fill / 100, x + bw, y + bh, 0xFF000000 | color);
        else g.fill(x, y, x + bw * fill / 100, y + bh, 0xFF000000 | color);

        if (!ClientConfig.SHOW_SONG_NAME.get()) return;
        Component label;
        int textColor = 0xFFFFFF;
        if (slot == 0) {
            label = Component.translatable("hud.edelraids.no_song");
            textColor = 0x9A9A9A;
        } else {
            Songs.Entry e = songs[slot - 1];
            label = Component.translatable("hud.edelraids.song", e.name(), e.song().cost());
            if (r.value() < e.song().cost() && !p.getAbilities().instabuild) textColor = 0x9A9A9A;
        }
        // The label sits on the side facing the middle of the screen, so it never runs off an edge.
        int tw = mc.font.width(label), th = mc.font.lineHeight;
        boolean lowerHalf = y + bh / 2 > h / 2;
        int tx, ty;
        if (vertical) {
            tx = align == 1 ? x - 4 - tw : x + bw + 4;
            ty = lowerHalf ? y + bh - th + 1 : y;
        } else {
            tx = align == 1 ? x + bw - tw : align == 0 ? x + (bw - tw) / 2 : x;
            ty = lowerHalf ? y - th - 2 : y + bh + 3;
        }
        tx = Math.max(1, Math.min(w - 1 - tw, tx));
        g.drawString(mc.font, label, tx, ty, textColor, true);
    }

    private static final int THICKNESS = 4;
    private static final int MARGIN = 5;
}
