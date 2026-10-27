package com.tygabeast.edelraids.event;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.bond.EdelRegistry;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.OnHitEffects;
import com.tygabeast.edelraids.registry.ModEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import com.tygabeast.edelraids.bond.ReactManager;
import com.tygabeast.edelraids.command.EdelCommands;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Elemental;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.entity.EdelRaidEntity;
import com.tygabeast.edelraids.entity.StingRaidEntity;
import com.tygabeast.edelraids.bond.BondMessages;
import com.tygabeast.edelraids.item.EdelTwinItem;
import com.tygabeast.edelraids.song.SongManager;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
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
        bus.addListener(CommonEvents::onDamagePost);
        bus.addListener(CommonEvents::onCritical);
        bus.addListener(CommonEvents::onEntityJoin);
        bus.addListener(CommonEvents::onLogin);
        bus.addListener(CommonEvents::onSwapHands);
        bus.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p) SongManager.tickPlayer(p);
        });
        bus.addListener((ServerTickEvent.Post e) -> SongManager.tickFields(e.getServer().getTickCount()));
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> SongManager.forget(e.getEntity().getUUID()));
        bus.addListener((ServerStoppedEvent e) -> SongManager.clearFields());
        bus.addListener(EventPriority.HIGHEST, CommonEvents::onDeath);
        bus.addListener(CommonEvents::onContainerClose);
        bus.addListener(CommonEvents::onEntityInteract);
        bus.addListener(CommonEvents::onEntityInteractSpecific);
        bus.addListener(CommonEvents::onRightClickBlock);
        bus.addListener(EdelCommands::register);
    }

    /**
     * The Edel Raid weapon behind a hit, for melee by a player or an arrow fired from her bow form; null otherwise.
     * Other damage (thorns, Thunder's chained magic, fire) is deliberately excluded so effects never re-trigger.
     */
    @Nullable
    private static EdelRaidData weaponHit(DamageSource src) {
        Entity direct = src.getDirectEntity();
        if (direct instanceof AbstractArrow arrow) return EdelWeaponItem.data(arrow.getWeaponItem());
        if (direct instanceof Player p && src.getEntity() == p && src.is(DamageTypes.PLAYER_ATTACK)) {
            return EdelWeaponItem.data(p.getMainHandItem());
        }
        return null;
    }

    /** A Reacted Sting Raid's own hit: her arm weapon in melee, or an arrow from her launcher arm. */
    @Nullable
    private static StingRaidEntity stingHit(DamageSource src) {
        if (src.getEntity() instanceof StingRaidEntity sting && sting.isReacted()
                && (src.getDirectEntity() == sting || src.getDirectEntity() instanceof AbstractArrow)) return sting;
        return null;
    }

    /**
     * Ring advantage and before-damage effects. Fires for every damage event, so it leaves on the cheapest checks:
     * client side, no living attacker, then no Edel weapon and not an Edel Raid's own melee.
     */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof LivingEntity attacker)) return;
        if (target instanceof ServerPlayer warded && !src.is(DamageTypes.THORNS)) SongManager.onWardedHit(warded, attacker);

        EdelRaidData weapon = weaponHit(src);
        StingRaidEntity sting = null;
        ResourceLocation attackerElement;
        if (weapon != null) {
            attackerElement = weapon.element();
        } else if (attacker instanceof EdelRaidEntity raid && src.getDirectEntity() == raid) {
            attackerElement = raid.getElementId();
        } else if ((sting = stingHit(src)) != null) {
            attackerElement = sting.getElementId();
        } else {
            return;
        }

        RegistryAccess access = target.level().registryAccess();
        Element atk = Elements.element(access, attackerElement);
        if (atk == null) return;
        if (target instanceof Elemental defender) {
            float m = Elements.multiplier(atk, Elements.element(access, defender.getElementId()));
            if (m != 1f) event.setAmount(event.getAmount() * m);
        }
        if (weapon != null || sting != null) OnHitEffects.incoming(atk.onHit(), event, attacker);
    }

    /** After-damage effects; same filter as above, and only when damage actually landed. */
    private static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        EdelRaidData weapon = weaponHit(event.getSource());
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        ResourceLocation element;
        if (weapon != null) {
            element = weapon.element();
        } else {
            StingRaidEntity sting = stingHit(event.getSource());
            if (sting == null) return;
            element = sting.getElementId();
        }
        Element atk = Elements.element(target.level().registryAccess(), element);
        if (atk != null) OnHitEffects.afterHit(atk.onHit(), target, attacker, event.getNewDamage());
        if (weapon != null && attacker instanceof ServerPlayer p) SongManager.onWeaponHit(p);
    }

    private static void onCritical(CriticalHitEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide) return;
        EdelRaidData d = EdelWeaponItem.data(p.getMainHandItem());
        if (d == null) return;
        Element atk = Elements.element(p.level().registryAccess(), d.element());
        if (atk != null) OnHitEffects.critical(atk.onHit(), event);
    }

    /** Quiet's Silenced effect: projectiles from a silenced entity never spawn. Cheapest check first: is it a projectile. */
    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Projectile proj) || event.loadedFromDisk() || event.getLevel().isClientSide) return;
        if (proj.getOwner() instanceof LivingEntity owner && owner.hasEffect(ModEffects.SILENCED)) event.setCanceled(true);
    }

    /** A weapon item that vanished while she was held (a removed mod, /clear) is restored from the registry's snapshot. */
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        List<EdelRaidData> held = EdelRegistry.get(p.getServer()).heldBy(p.getUUID());
        if (held.isEmpty()) return;
        Inventory inv = p.getInventory();
        for (EdelRaidData snap : held) {
            boolean present = false;
            for (int i = 0; i < inv.getContainerSize() && !present; i++) {
                ItemStack is = inv.getItem(i);
                EdelRaidData d = is.getItem() instanceof EdelWeaponItem ? EdelWeaponItem.data(is) : null;
                present = d != null && d.id().equals(snap.id()) && d.stamp() == snap.stamp();
            }
            if (!present) {
                ReactManager.reformFromSnapshot(p, snap);
                p.displayClientMessage(Component.translatable("message.edelraids.recovered_lost", snap.name()), false);
            }
        }
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
            } else if (s.getItem() instanceof EdelTwinItem) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        ItemStack carried = p.containerMenu.getCarried();
        if (carried.getItem() instanceof EdelWeaponItem) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
            ReactManager.reformAt(p, carried, false);
        }
    }

    /** Bond for the Edel Raid the player holds, as for a hostile kill (also used when a hunter's Sting Raid is subdued). */
    public static void gainBond(ServerPlayer killer) {
        int gain = Config.BOND_PER_KILL.get();
        if (gain <= 0) return;
        ItemStack weapon = killer.getMainHandItem();
        if (weapon.getItem() instanceof EdelTwinItem) {
            // A Better Combat off-hand swing swaps hands for the attack; the bond belongs on her real weapon.
            weapon = EdelTwinItem.primaryFor(killer, weapon);
            if (weapon == null) return;
        }
        EdelRaidData d = EdelWeaponItem.data(weapon);
        if (d == null || !d.isPledger(killer.getUUID())) return;

        int bond = d.bond() + gain;
        boolean awake = bond >= Config.AWAKEN_BOND.get();
        EdelRaidData updated = d.withBond(bond, awake);
        weapon.set(ModComponents.EDEL_RAID.get(), updated);
        EdelRegistry.get(killer.getServer()).updateSnapshot(updated);
        BondMessages.announce(killer, d, updated);
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
            if (s.getItem() instanceof EdelTwinItem) {
                slot.set(ItemStack.EMPTY);
                continue;
            }
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

    private static boolean isEdel(ItemStack stack) {
        return stack.getItem() instanceof EdelWeaponItem || stack.getItem() instanceof EdelTwinItem;
    }

    /** Swapping hands would split a pair (her weapon to the off hand, the twin to the main hand), so it is blocked. */
    private static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        ItemStack toOff = event.getItemSwappedToOffHand();
        ItemStack toMain = event.getItemSwappedToMainHand();
        if (toMain.getItem() instanceof EdelTwinItem || (toOff.getItem() instanceof EdelWeaponItem && toOff.has(ModComponents.PAIRED.get()))) {
            event.setCanceled(true);
        }
    }

    private static boolean storesItems(Entity target) {
        return target instanceof ItemFrame || target instanceof ArmorStand || target instanceof Allay;
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isEdel(event.getItemStack()) && storesItems(event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isEdel(event.getItemStack()) && storesItems(event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isEdel(event.getItemStack())
                && event.getLevel().getBlockState(event.getPos()).is(Blocks.DECORATED_POT)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }
}
