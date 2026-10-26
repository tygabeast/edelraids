package com.tygabeast.edelraids.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.bond.EdelRegistry;
import com.tygabeast.edelraids.bond.ReactManager;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.entity.EdelRaidEntity;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.registry.ModComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * /edelraids release [confirm]  - anyone: free the Edel Raid you look at or hold (bond resets)
 * /edelraids info              - anyone: show her name, element, form, quality, bond and registry state
 * /edelraids bond <value>      - operators: set her bond, for testing awakening
 */
public final class EdelCommands {
    private EdelCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("edelraids")
                .then(Commands.literal("release")
                        .executes(ctx -> releasePrompt(ctx))
                        .then(Commands.literal("confirm").executes(EdelCommands::release)))
                .then(Commands.literal("info").executes(EdelCommands::info))
                .then(Commands.literal("bond").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(ctx -> setBond(ctx, IntegerArgumentType.getInteger(ctx, "value"))))));
    }

    /** Her as an entity you are looking at (within 6 blocks) and own, or null. */
    @Nullable
    private static EdelRaidEntity lookedAt(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        EdelRaidEntity best = null;
        double bestDot = 0.95;
        for (EdelRaidEntity e : p.level().getEntitiesOfClass(EdelRaidEntity.class, p.getBoundingBox().inflate(6.0), e -> e.isOwnedBy(p))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            double dot = to.dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = e;
            }
        }
        return best;
    }

    private static int releasePrompt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String name = targetName(p);
        if (name == null) {
            ctx.getSource().sendFailure(Component.translatable("command.edelraids.no_target"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.release_prompt", name), false);
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        EdelRaidEntity e = lookedAt(p);
        if (e == null) {
            ItemStack held = p.getMainHandItem();
            EdelRaidData d = EdelWeaponItem.data(held);
            if (d != null && d.isPledger(p.getUUID())) {
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                e = ReactManager.reformAt(p, held, false);
            }
        }
        if (e == null) {
            ctx.getSource().sendFailure(Component.translatable("command.edelraids.no_target"));
            return 0;
        }
        String name = e.getEdelName();
        e.release();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.released", name), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        EdelRaidData d = targetData(p);
        if (d == null) {
            ctx.getSource().sendFailure(Component.translatable("command.edelraids.no_target"));
            return 0;
        }
        EdelRegistry.Entry entry = EdelRegistry.get(p.getServer()).get(d.id());
        String where = entry == null ? "untracked" : (entry.held() ? "weapon" : "companion") + ", stamp " + entry.stamp();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.info", d.name(),
                EdelWeaponItem.elementName(d.element()), EdelWeaponItem.formName(d.form()),
                Component.translatable("edelraids.quality." + d.quality()), d.bond(), Config.AWAKEN_BOND.get(), where), false);
        return 1;
    }

    private static int setBond(CommandContext<CommandSourceStack> ctx, int value) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        boolean awake = value >= Config.AWAKEN_BOND.get();
        EdelRaidEntity e = lookedAt(p);
        if (e != null) {
            e.setBond(value, awake);
        } else {
            ItemStack held = p.getMainHandItem();
            EdelRaidData d = EdelWeaponItem.data(held);
            if (d == null) {
                ctx.getSource().sendFailure(Component.translatable("command.edelraids.no_target"));
                return 0;
            }
            EdelRaidData updated = d.withBond(value, awake);
            held.set(ModComponents.EDEL_RAID.get(), updated);
            ReactManager.applyStats(held, updated, p.level().registryAccess(), false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.bond_set", value), true);
        return 1;
    }

    @Nullable
    private static String targetName(ServerPlayer p) {
        EdelRaidData d = targetData(p);
        return d == null ? null : d.name();
    }

    @Nullable
    private static EdelRaidData targetData(ServerPlayer p) {
        EdelRaidEntity e = lookedAt(p);
        if (e != null) return e.toData();
        return EdelWeaponItem.data(p.getMainHandItem());
    }
}
