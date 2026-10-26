package com.tygabeast.edelraids.item;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.bond.EdelRegistry;
import com.tygabeast.edelraids.bond.ReactManager;
import com.tygabeast.edelraids.client.EdelWeaponRenderer;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.data.OnHitEffects;
import com.tygabeast.edelraids.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * The single weapon item for every Edel Raid. Element, form, quality and bond come from the stack's data component,
 * so one item and one renderer cover every combination.
 */
public class EdelWeaponItem extends Item implements GeoItem {
    private static final RawAnimation FORM = RawAnimation.begin().thenPlay("animation.edel_weapon.form");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public EdelWeaponItem(Properties props) {
        super(props);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Nullable
    public static EdelRaidData data(ItemStack stack) {
        return stack.get(ModComponents.EDEL_RAID.get());
    }

    public static void playFormAnimation(ServerPlayer player, ItemStack stack, ServerLevel level) {
        if (stack.getItem() instanceof EdelWeaponItem item) {
            item.triggerAnim(player, GeoItem.getOrAssignId(stack, level), "main", "form");
        }
    }

    // ---------------------------------------------------------------- revert on demand: sneak + use

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            ItemStack taken = stack.copy();
            player.setItemInHand(hand, ItemStack.EMPTY);
            ReactManager.reformAt(sp, taken, false);
        }
        return InteractionResultHolder.sidedSuccess(ItemStack.EMPTY, level.isClientSide);
    }

    // ---------------------------------------------------------------- combat

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            EdelRaidData d = data(stack);
            Element el = d == null ? null : Elements.element(attacker.level().registryAccess(), d.element());
            if (el != null) OnHitEffects.apply(el.onHit(), target, attacker);
        }
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide) return;
        int wear = Config.WEAR_PER_HIT.get();
        if (wear > 0) stack.hurtAndBreak(wear, attacker, EquipmentSlot.MAINHAND);
    }

    /** Her health is the weapon's durability: instead of breaking, she is forced out, wounded. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        if (amount > 0 && stack.getDamageValue() + amount >= stack.getMaxDamage() && entity instanceof ServerPlayer sp) {
            ItemStack taken = stack.copy();
            stack.setCount(0);
            ReactManager.reformAt(sp, taken, true);
            return 0;
        }
        return amount;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        // Phase 1 ships only the longsword; phase 2 makes this follow each form's "sweeps" flag.
        return ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(ability);
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }

    // ---------------------------------------------------------------- anti-duplication and "she cannot be stored"

    /**
     * Runs every tick for each stack in a player's inventory, so it exits on the cheapest checks first and only
     * consults the registry once per validate interval.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p)) return;
        if ((level.getGameTime() + slot) % Config.VALIDATE_INTERVAL.get() != 0) return;

        EdelRaidData d = data(stack);
        if (d == null) {
            stack.setCount(0);
            return;
        }
        EdelRegistry reg = EdelRegistry.get(p.getServer());
        EdelRegistry.Entry entry = reg.get(d.id());
        if (entry == null) {
            reg.setHeld(d.id(), p.getUUID(), d.stamp());
            return;
        }
        if (!entry.held() || entry.stamp() != d.stamp()) {
            EdelRaids.debug("Voided a stale weapon copy of {} held by {}", d.name(), p.getGameProfile().getName());
            stack.setCount(0);
            return;
        }
        if (!entry.holder().equals(p.getUUID())) {
            // The real weapon reached someone other than the registered holder by some other route: she reforms beside them.
            ItemStack taken = stack.copy();
            stack.setCount(0);
            ReactManager.reformAt(p, taken, false);
            return;
        }
        int first = firstCopyIndex(p.getInventory(), d);
        if (first != -1 && p.getInventory().getItem(first) != stack) {
            EdelRaids.debug("Voided a duplicate weapon copy of {} in {}'s inventory", d.name(), p.getGameProfile().getName());
            stack.setCount(0);
        }
    }

    private static int firstCopyIndex(Inventory inv, EdelRaidData d) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof EdelWeaponItem) {
                EdelRaidData o = data(s);
                if (o != null && o.id().equals(d.id())) return i;
            }
        }
        return -1;
    }

    /** Dropped, thrown or spilled from a broken container: she reforms where the item landed. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity itemEntity) {
        if (itemEntity.level() instanceof ServerLevel level) {
            ItemStack taken = stack.copy();
            itemEntity.discard();
            ReactManager.reform(level, itemEntity.position(), itemEntity.getYRot(), taken, false);
        }
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged || !newStack.is(this) || !oldStack.is(this)) return true;
        EdelRaidData a = data(oldStack), b = data(newStack);
        return a == null || b == null || !a.id().equals(b.id());
    }

    // ---------------------------------------------------------------- display

    @Override
    public Component getName(ItemStack stack) {
        EdelRaidData d = data(stack);
        if (d == null) return super.getName(stack);
        return Component.translatable("item.edelraids.edel_weapon.named", d.name(), formName(d.form()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EdelRaidData d = data(stack);
        if (d == null) return;
        tooltip.add(Component.translatable("tooltip.edelraids.element", elementName(d.element())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.edelraids.quality", Component.translatable("edelraids.quality." + d.quality())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(d.awakened() ? "tooltip.edelraids.bond_awakened" : "tooltip.edelraids.bond", d.bond())
                .withStyle(d.awakened() ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.edelraids.revert").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static Component formName(ResourceLocation form) {
        return Component.translatable("edelraids.form." + form.getNamespace() + "." + form.getPath());
    }

    public static Component elementName(ResourceLocation element) {
        return Component.translatable("edelraids.element." + element.getNamespace() + "." + element.getPath());
    }

    // ---------------------------------------------------------------- GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> PlayState.STOP)
                .triggerableAnim("form", FORM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private EdelWeaponRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new EdelWeaponRenderer();
                return renderer;
            }
        });
    }
}
