package com.tygabeast.edelraids.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.bond.BondMessages;
import com.tygabeast.edelraids.bond.EdelRegistry;
import com.tygabeast.edelraids.bond.ReactManager;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.entity.EdelRaidEntity;
import com.tygabeast.edelraids.entity.StingRaidEntity;
import com.tygabeast.edelraids.bond.StingRegistry;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.registry.ModComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.registry.ModEntities;
import com.tygabeast.edelraids.registry.ModRegistries;
import java.util.concurrent.CompletableFuture;
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
 * /edelraids create <element> [form] [quality] - operators: create a specific Edel Raid (any ring, for testing)
 * /edelraids sting <element> [form] [hostile|freed] - operators: create a Sting Raid (for testing)
 * release and info also work on a freed Sting Raid you look at; releasing one lets her leave.
 */
public final class EdelCommands {
    private EdelCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("edelraids")
                .then(Commands.literal("release")
                        .executes(ctx -> releasePrompt(ctx))
                        .then(Commands.literal("confirm").executes(EdelCommands::release)))
                .then(Commands.literal("info").executes(EdelCommands::info))
                .then(Commands.literal("create").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("element", ResourceLocationArgument.id()).suggests(EdelCommands::suggestElements)
                                .executes(ctx -> create(ctx, null, 1))
                                .then(Commands.argument("form", ResourceLocationArgument.id()).suggests(EdelCommands::suggestForms)
                                        .executes(ctx -> create(ctx, ResourceLocationArgument.getId(ctx, "form"), 1))
                                        .then(Commands.argument("quality", IntegerArgumentType.integer(1, 3))
                                                .executes(ctx -> create(ctx, ResourceLocationArgument.getId(ctx, "form"),
                                                        IntegerArgumentType.getInteger(ctx, "quality")))))))
                .then(Commands.literal("sting").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("element", ResourceLocationArgument.id()).suggests(EdelCommands::suggestElements)
                                .executes(ctx -> createSting(ctx, null, true))
                                .then(Commands.argument("form", ResourceLocationArgument.id()).suggests(EdelCommands::suggestForms)
                                        .executes(ctx -> createSting(ctx, ResourceLocationArgument.getId(ctx, "form"), true))
                                        .then(Commands.literal("hostile").executes(ctx -> createSting(ctx, ResourceLocationArgument.getId(ctx, "form"), true)))
                                        .then(Commands.literal("freed").executes(ctx -> createSting(ctx, ResourceLocationArgument.getId(ctx, "form"), false))))))
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

    /** A freed Sting Raid you own and look at (within 6 blocks), or null. */
    @Nullable
    private static StingRaidEntity lookedAtSting(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        StingRaidEntity best = null;
        double bestDot = 0.95;
        for (StingRaidEntity e : p.level().getEntitiesOfClass(StingRaidEntity.class, p.getBoundingBox().inflate(6.0), e -> e.isOwnedBy(p))) {
            double dot = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye).normalize().dot(look);
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
            StingRaidEntity sting = lookedAtSting(p);
            if (sting != null) {
                ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.release_sting_prompt", sting.getStingName()), false);
                return 1;
            }
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
            StingRaidEntity sting = lookedAtSting(p);
            if (sting != null) {
                String stingName = sting.getStingName();
                sting.releaseAndLeave();
                ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.released_sting", stingName), false);
                return 1;
            }
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
            StingRaidEntity s = lookedAtSting(p);
            if (s == null) {
                ctx.getSource().sendFailure(Component.translatable("command.edelraids.no_target"));
                return 0;
            }
            int count = StingRegistry.get(p.getServer()).count(p.getUUID());
            ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.sting_info", s.getStingName(),
                    EdelWeaponItem.elementName(s.getElementId()), Component.translatable("edelraids.sting_arm." + s.getArm().id()),
                    s.getStrainDisplay(), Component.translatable(s.isStabilized() ? "edelraids.sting.stabilized" : "edelraids.sting.unstabilized"),
                    count, Config.STING_LIMIT.get()), false);
            return 1;
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
            EdelRaidData before = e.toData();
            e.setBond(value, awake);
            BondMessages.announce(p, before, e.toData());
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
            EdelRegistry.get(p.getServer()).updateSnapshot(updated);
            BondMessages.announce(p, d, updated);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.bond_set", value), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestElements(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggestResource(ctx.getSource().registryAccess().registryOrThrow(ModRegistries.ELEMENT).keySet(), b);
    }

    private static CompletableFuture<Suggestions> suggestForms(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggestResource(ctx.getSource().registryAccess().registryOrThrow(ModRegistries.WEAPON_FORM).keySet(), b);
    }

    private static int create(CommandContext<CommandSourceStack> ctx, @Nullable ResourceLocation formId, int quality) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ResourceLocation elementId = ResourceLocationArgument.getId(ctx, "element");
        Element element = Elements.element(p.level().registryAccess(), elementId);
        if (element == null) {
            ctx.getSource().sendFailure(Component.translatable("command.edelraids.unknown_element", elementId.toString()));
            return 0;
        }
        ResourceLocation form = formId;
        if (form == null) {
            form = element.forms().isEmpty() ? EdelRaids.id("longsword") : element.forms().get(p.getRandom().nextInt(element.forms().size()));
        } else if (p.level().registryAccess().registryOrThrow(ModRegistries.WEAPON_FORM).get(form) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.edelraids.unknown_form", form.toString()));
            return 0;
        }
        EdelRaidEntity e = ModEntities.EDEL_RAID.get().create(p.serverLevel());
        if (e == null) return 0;
        e.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0f);
        e.setIdentity(elementId, form, quality, EdelRaidEntity.randomName(p.getRandom()));
        p.serverLevel().addFreshEntity(e);
        ResourceLocation finalForm = form;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.edelraids.created", e.getEdelName(),
                EdelWeaponItem.elementName(elementId), EdelWeaponItem.formName(finalForm),
                Component.translatable("edelraids.quality." + e.getQuality())), true);
        return 1;
    }

    private static int createSting(CommandContext<CommandSourceStack> ctx, @Nullable ResourceLocation formId, boolean hostile) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ResourceLocation elementId = ResourceLocationArgument.getId(ctx, "element");
        Element element = Elements.element(p.level().registryAccess(), elementId);
        if (element == null) {
            ctx.getSource().sendFailure(Component.translatable("command.edelraids.unknown_element", elementId.toString()));
            return 0;
        }
        ResourceLocation form = formId;
        if (form == null) {
            form = element.forms().isEmpty() ? EdelRaids.id("longsword") : element.forms().get(p.getRandom().nextInt(element.forms().size()));
        } else if (p.level().registryAccess().registryOrThrow(ModRegistries.WEAPON_FORM).get(form) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.edelraids.unknown_form", form.toString()));
            return 0;
        }
        if (!hostile && StingRegistry.get(p.getServer()).count(p.getUUID()) >= Config.STING_LIMIT.get()) {
            ctx.getSource().sendFailure(Component.translatable("message.edelraids.sting_limit", Config.STING_LIMIT.get()));
            return 0;
        }
        StingRaidEntity e = ModEntities.STING_RAID.get().create(p.serverLevel());
        if (e == null) return 0;
        e.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0f);
        e.setIdentity(elementId, form, EdelRaidEntity.randomName(p.getRandom()));
        if (!hostile) {
            e.tame(p);
            StingRegistry.get(p.getServer()).add(p.getUUID(), e.getUUID());
        }
        p.serverLevel().addFreshEntity(e);
        ctx.getSource().sendSuccess(() -> Component.translatable(hostile ? "command.edelraids.sting_created_hostile" : "command.edelraids.sting_created",
                e.getStingName(), EdelWeaponItem.elementName(elementId), Component.translatable("edelraids.sting_arm." + e.getArm().id())), true);
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
