package com.tygabeast.edelraids.event;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.bond.ReactManager;
import com.tygabeast.edelraids.command.EdelCommands;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Elemental;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.entity.EdelRaidEntity;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.registry.ModComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Game-bus listeners. Each one returns on its cheapest check first, since several fire for every entity. */
public final class CommonEvents {
    private CommonEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(CommonEvents::onIncomingDamage);
        bus.addListener(EventPriority.HIGHEST, CommonEvents::onDeath);
        bus.addListener(CommonEvents::onContainerClose);
        bus.addListener(CommonEvents::onEntityInteract);
        bus.addListener(CommonEvents::onEntityInteractSpecific);
        bus.addListener(CommonEvents::onRightClickBlock);
        bus.addListener(EdelCommands::register);
    }

    /** Glittering-ring advantage. Fires for every damage event, so non-elemental targets leave on the first check. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Elemental defender)) return;
        DamageSource src = event.getSource();
        Entity attacker = src.getEntity();
        if (attacker == null || src.getDirectEntity() != attacker) return;

        ResourceLocation attackerElement = null;
        if (attacker instanceof EdelRaidEntity raid) {
            attackerElement = raid.getElementId();
        } else if (attacker instanceof Player p) {
            EdelRaidData d = EdelWeaponItem.data(p.getMainHandItem());
            if (d != null) attackerElement = d.element();
        }
        if (attackerElement == null) return;

        RegistryAccess access = event.getEntity().level().registryAccess();
        float m = Elements.multiplier(Elements.element(access, attackerElement), Elements.element(access, defender.getElementId()));
        if (m != 1f) event.setAmount(event.getAmount() * m);
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        if (dead instanceof ServerPlayer p) {
            reformAllCarried(p);
        } else if (dead instanceof Enemy && event.getSource().getEntity() instanceof ServerPlayer killer) {
            gainBond(killer);
        }
    }

    /** A Pledger's death forces every Edel Raid they carry back out, before any items drop. */
    private static void reformAllCarried(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof EdelWeaponItem) {
                inv.setItem(i, ItemStack.EMPTY);
                ReactManager.reformAt(p, s, false);
            }
        }
        ItemStack carried = p.containerMenu.getCarried();
        if (carried.getItem() instanceof EdelWeaponItem) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
            ReactManager.reformAt(p, carried, false);
        }
    }

    private static void gainBond(ServerPlayer killer) {
        int gain = Config.BOND_PER_KILL.get();
        if (gain <= 0) return;
        ItemStack weapon = killer.getMainHandItem();
        EdelRaidData d = EdelWeaponItem.data(weapon);
        if (d == null || !d.isPledger(killer.getUUID())) return;

        int bond = d.bond() + gain;
        boolean awake = bond >= Config.AWAKEN_BOND.get();
        EdelRaidData updated = d.withBond(bond, awake);
        weapon.set(ModComponents.EDEL_RAID.get(), updated);
        if (awake && !d.awakened()) {
            ReactManager.applyStats(weapon, updated, killer.level().registryAccess(), false);
            killer.displayClientMessage(Component.translatable("message.edelraids.awakened", d.name()), false);
            killer.level().playSound(null, killer, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
    }

    /** Edel Raids cannot be stored: anything left in a container's own slots goes back to the player, or she reforms. */
    private static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        AbstractContainerMenu menu = event.getContainer();
        Inventory inv = p.getInventory();
        boolean returned = false;
        for (Slot slot : menu.slots) {
            if (slot.container == inv) continue;
            ItemStack s = slot.getItem();
            if (!(s.getItem() instanceof EdelWeaponItem)) continue;
            ItemStack taken = s.copy();
            slot.set(ItemStack.EMPTY);
            // Never Inventory.add: in creative it reports success on a full inventory and deletes the item.
            int free = inv.getFreeSlot();
            if (free >= 0) inv.setItem(free, taken);
            else ReactManager.reformAt(p, taken, false);
            returned = true;
        }
        if (returned) p.displayClientMessage(Component.translatable("message.edelraids.no_storage"), true);
    }

    private static boolean storesItems(Entity target) {
        return target instanceof ItemFrame || target instanceof ArmorStand || target instanceof Allay;
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getItemStack().getItem() instanceof EdelWeaponItem && storesItems(event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getItemStack().getItem() instanceof EdelWeaponItem && storesItems(event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof EdelWeaponItem
                && event.getLevel().getBlockState(event.getPos()).is(Blocks.DECORATED_POT)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }
}
