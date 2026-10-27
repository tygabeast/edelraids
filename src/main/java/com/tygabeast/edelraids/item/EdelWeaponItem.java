package com.tygabeast.edelraids.item;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.bond.EdelRegistry;
import com.tygabeast.edelraids.bond.ReactManager;
import com.tygabeast.edelraids.client.EdelWeaponRenderer;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
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
    public static EdelRaidData data(@Nullable ItemStack stack) {
        return stack == null ? null : stack.get(ModComponents.EDEL_RAID.get());
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
        if (!player.isShiftKeyDown()) {
            if (!stack.has(ModComponents.RANGED.get())) return InteractionResultHolder.pass(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer sp) {
            ItemStack taken = stack.copy();
            player.setItemInHand(hand, ItemStack.EMPTY);
            ReactManager.reformAt(sp, taken, false);
        }
        return InteractionResultHolder.sidedSuccess(ItemStack.EMPTY, level.isClientSide);
    }

    // ---------------------------------------------------------------- bow form: draws like a bow, needs no arrows

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return stack.has(ModComponents.RANGED.get()) ? 72000 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return stack.has(ModComponents.RANGED.get()) ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int timeLeft) {
        if (!stack.has(ModComponents.RANGED.get()) || !(level instanceof ServerLevel sl)) return;
        float power = BowItem.getPowerForTime(getUseDuration(stack, shooter) - timeLeft);
        if (power < 0.1f) return;
        EdelRaidData d = data(stack);
        Arrow arrow = new Arrow(sl, shooter, new ItemStack(Items.ARROW), stack);
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        double bonus = d == null ? 0.0 : (d.quality() - 1) * Config.QUALITY_DAMAGE.get() + (d.awakened() ? Config.AWAKEN_DAMAGE.get() : 0.0);
        arrow.setBaseDamage(arrow.getBaseDamage() + bonus * 0.5);
        arrow.setCritArrow(power >= 1f);
        arrow.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0f, power * 3f, 1f);
        sl.addFreshEntity(arrow);
        sl.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1f, 1f / (sl.getRandom().nextFloat() * 0.4f + 1.2f) + power * 0.5f);
        int wear = Config.WEAR_PER_HIT.get();
        if (wear > 0) stack.hurtAndBreak(wear, shooter, shooter.getUsedItemHand() == InteractionHand.OFF_HAND
                ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
    }

    // ---------------------------------------------------------------- melee (element effects live in CommonEvents)

    /** Must return true: vanilla only calls postHurtEnemy (where wear is applied) when this does. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
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

    /** Forms that sweep carry a marker component, so this per-swing check needs no registry lookup. */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return stack.has(ModComponents.SWEEPS.get()) && ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(ability);
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
        long time = level.getGameTime();
        if (selected && time % 5 == 0 && stack.has(ModComponents.PAIRED.get())) syncTwin(p, stack, time % 20 == 0);
        if ((time + slot) % Config.VALIDATE_INTERVAL.get() != 0) return;

        EdelRaidData d = data(stack);
        if (d == null) {
            stack.setCount(0);
            return;
        }
        EdelRegistry reg = EdelRegistry.get(p.getServer());
        EdelRegistry.Entry entry = reg.get(d.id());
        if (entry == null) {
            reg.setHeld(d.id(), p.getUUID(), d.stamp(), d);
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

    /**
     * Paired forms: keep the matching twin in the off hand while she is in the main hand. Only fills an empty off hand,
     * and every second refreshes the twin if her data (bond, awakening, enchantments) changed.
     */
    private static void syncTwin(ServerPlayer p, ItemStack primary, boolean refresh) {
        ItemStack off = p.getOffhandItem();
        if (off.isEmpty()) {
            p.setItemInHand(InteractionHand.OFF_HAND, EdelTwinItem.twinOf(primary));
        } else if (refresh && off.getItem() instanceof EdelTwinItem) {
            ItemStack expected = EdelTwinItem.twinOf(primary);
            if (!ItemStack.isSameItemSameComponents(off, expected)) p.setItemInHand(InteractionHand.OFF_HAND, expected);
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
            private EdelWeaponRenderer<EdelWeaponItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new EdelWeaponRenderer<>();
                return renderer;
            }
        });
    }
}
