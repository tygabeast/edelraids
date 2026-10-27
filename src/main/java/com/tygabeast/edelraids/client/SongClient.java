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

    /** The meter sits to the right of the hotbar (past the off-hand slot for left-handed players). */
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui || p.isSpectator()) return;
        EdelRaidData d = held(p);
        if (d == null) return;

        ModAttachments.Resonance r = p.getData(ModAttachments.RESONANCE);
        Songs.Entry[] songs = Songs.forElement(mc.level.registryAccess(), d.element());
        int slot = Songs.effectiveSlot(d, songs, r.selected());
        Element el = Elements.element(mc.level.registryAccess(), d.element());
        int color = el == null ? 0x4FC9A0 : el.color();

        GuiGraphics g = event.getGuiGraphics();
        int barW = 62;
        int x = g.guiWidth() / 2 + 98 + (mc.options.mainHand().get() == HumanoidArm.LEFT ? 30 : 0);
        int y = g.guiHeight() - 12;
        g.fill(x - 1, y - 1, x + barW + 1, y + 5, 0xC0000000);
        g.fill(x, y, x + barW * Math.max(0, Math.min(100, r.value())) / 100, y + 4, 0xFF000000 | color);

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
        g.drawString(mc.font, label, x, y - 11, textColor, true);
    }
}
