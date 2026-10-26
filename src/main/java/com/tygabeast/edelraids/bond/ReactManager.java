package com.tygabeast.edelraids.bond;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.data.WeaponForm;
import com.tygabeast.edelraids.entity.EdelRaidEntity;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.registry.ModComponents;
import com.tygabeast.edelraids.registry.ModEntities;
import com.tygabeast.edelraids.registry.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** React (Edel Raid to weapon) and reform (weapon to Edel Raid), plus the stats a weapon carries. Server only. */
public final class ReactManager {
    /** 10 points of item wear = 1 point of her health. */
    public static final float WEAR_PER_HEALTH = 10f;

    private ReactManager() {}

    public static boolean hasRoomFor(ServerPlayer p) {
        return p.getMainHandItem().isEmpty() || p.getInventory().getFreeSlot() != -1;
    }

    public static void startReact(EdelRaidEntity e, ServerPlayer p) {
        if (e.isBusy() || !e.isOwnedBy(p)) return;
        if (!hasRoomFor(p)) {
            p.displayClientMessage(Component.translatable("message.edelraids.inventory_full", e.getEdelName()), true);
            return;
        }
        e.beginReact(p);
        ServerLevel level = (ServerLevel) e.level();
        level.playSound(null, e, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1f, 1.2f);
        level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1.6, e.getZ(), 12, 0.25, 0.4, 0.25, 0.02);
    }

    /** Called by the entity when its transformation animation ends. */
    public static void finishReact(EdelRaidEntity e, @Nullable UUID playerId) {
        if (!(e.level() instanceof ServerLevel level) || e.getEdelId() == null) return;
        ServerPlayer p = playerId == null ? null : level.getServer().getPlayerList().getPlayer(playerId);
        double range = Config.REACT_RANGE.get();
        if (p == null || !p.isAlive() || p.level() != level || p.distanceToSqr(e) > range * range || !e.isOwnedBy(p) || !hasRoomFor(p)) {
            if (p != null) p.displayClientMessage(Component.translatable("message.edelraids.react_cancelled", e.getEdelName()), true);
            return;
        }
        EdelRegistry reg = EdelRegistry.get(level.getServer());
        long stamp = reg.nextStamp(e.getEdelId());
        e.setStamp(stamp);
        ItemStack weapon = buildWeapon(e.toData(), e.getStoredWeapon(), level.registryAccess());
        reg.setHeld(e.getEdelId(), p.getUUID(), stamp);

        ItemStack previous = p.getMainHandItem();
        p.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        if (!previous.isEmpty()) {
            // Not Inventory.add: in creative it deletes items that do not fit. hasRoomFor guaranteed a free slot.
            int free = p.getInventory().getFreeSlot();
            if (free >= 0) p.getInventory().setItem(free, previous);
            else p.drop(previous, false);
        }

        EdelWeaponItem.playFormAnimation(p, weapon, level);
        level.playSound(null, p, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
        EdelRaids.debug("{} Reacted with {} (stamp {})", e.getEdelName(), p.getGameProfile().getName(), stamp);
        e.discard();
    }

    /** A fresh weapon stack, or her previously held one (keeping enchantments and other data), carrying her data and stats. */
    public static ItemStack buildWeapon(EdelRaidData data, ItemStack previous, RegistryAccess access) {
        ItemStack stack = previous.is(ModItems.EDEL_WEAPON.get()) ? previous.copyWithCount(1) : new ItemStack(ModItems.EDEL_WEAPON.get());
        stack.set(ModComponents.EDEL_RAID.get(), data);
        applyStats(stack, data, access, true);
        return stack;
    }

    /**
     * Attack stats from form, quality and awakening; durability mirrors her health. Recomputed only on React or awakening.
     * @param setWearFromHealth true on React (wear = her missing health); false to keep the wear the weapon has taken since
     */
    public static void applyStats(ItemStack stack, EdelRaidData data, RegistryAccess access, boolean setWearFromHealth) {
        WeaponForm form = Elements.form(access, data.form());
        double damage = form.attackDamage() - 1.0
                + (data.quality() - 1) * Config.QUALITY_DAMAGE.get()
                + (data.awakened() ? Config.AWAKEN_DAMAGE.get() : 0.0);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, form.attackSpeed() - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build());
        int max = Math.max(1, Math.round(data.maxHealth() * WEAR_PER_HEALTH));
        stack.set(DataComponents.MAX_DAMAGE, max);
        if (setWearFromHealth) {
            stack.set(DataComponents.DAMAGE, Mth.clamp(Math.round((data.maxHealth() - data.health()) * WEAR_PER_HEALTH), 0, max - 1));
        }
    }

    /** True when this weapon stack is the one the registry currently points at (or the registry has no record of her). */
    public static boolean isCurrent(ServerLevel level, ItemStack stack) {
        EdelRaidData d = stack.get(ModComponents.EDEL_RAID.get());
        if (d == null) return false;
        EdelRegistry.Entry entry = EdelRegistry.get(level.getServer()).get(d.id());
        return entry == null || (entry.held() && entry.stamp() == d.stamp());
    }

    /**
     * Turn a weapon stack back into her. The caller has already taken the stack out of wherever it was.
     * Stale copies are dropped silently instead of creating a second Edel Raid.
     */
    @Nullable
    public static EdelRaidEntity reform(ServerLevel level, Vec3 pos, float yaw, ItemStack stack, boolean wounded) {
        EdelRaidData d = stack.get(ModComponents.EDEL_RAID.get());
        if (d == null || !isCurrent(level, stack)) {
            EdelRaids.debug("Dropped a stale Edel Raid weapon copy");
            return null;
        }
        EdelRaidEntity e = ModEntities.EDEL_RAID.get().create(level);
        if (e == null) return null;

        float health = wounded ? 1f : Math.max(1f, d.maxHealth() - stack.getDamageValue() / WEAR_PER_HEALTH);
        ItemStack kept = stack.copyWithCount(1);
        kept.remove(ModComponents.EDEL_RAID.get());
        kept.remove(DataComponents.ATTRIBUTE_MODIFIERS);
        kept.remove(DataComponents.MAX_DAMAGE);
        kept.remove(DataComponents.DAMAGE);

        EdelRegistry reg = EdelRegistry.get(level.getServer());
        long stamp = reg.nextStamp(d.id());
        e.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        e.applyData(d.withHealth(health).withStamp(stamp), kept);
        e.beginReform();
        if (wounded) e.setWounded(true);
        level.addFreshEntity(e);
        reg.setWorld(d.id(), e.getUUID(), stamp);

        level.playSound(null, e, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 0.9f);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z, 12, 0.3, 0.5, 0.3, 0.02);
        EdelRaids.debug("{} reformed (stamp {}, wounded {})", d.name(), stamp, wounded);
        return e;
    }

    /** Reform where the player stands (never inside a wall), facing the way they face. */
    @Nullable
    public static EdelRaidEntity reformAt(ServerPlayer p, ItemStack stack, boolean wounded) {
        return reform(p.serverLevel(), p.position(), p.getYRot(), stack, wounded);
    }
}
