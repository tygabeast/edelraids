package com.tygabeast.edelraids.item;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.client.EdelWeaponRenderer;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.registry.ModComponents;
import com.tygabeast.edelraids.registry.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * The off-hand half of a paired form (twin blades, gauntlets). It carries a copy of her data for rendering and for
 * element effects on off-hand hits (Better Combat dual wielding), but it is not her: it exists only while her real
 * weapon is in the main hand and it in the off hand, it cannot be dropped or stored, and any wear goes to her weapon.
 */
public class EdelTwinItem extends Item implements GeoItem {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public EdelTwinItem(Properties props) {
        super(props);
    }

    /** Shimmers like her main weapon when she is a Glittering Jewel. */
    @Override
    public boolean isFoil(ItemStack stack) {
        EdelRaidData d = EdelWeaponItem.data(stack);
        return (d != null && d.quality() >= 4) || super.isFoil(stack);
    }

    /** A twin made from her weapon: same components (stats, enchantments, Better Combat preset), without durability. */
    public static ItemStack twinOf(ItemStack primary) {
        ItemStack twin = primary.transmuteCopy(ModItems.EDEL_TWIN.get(), 1);
        twin.remove(DataComponents.DAMAGE);
        twin.remove(DataComponents.MAX_DAMAGE);
        return twin;
    }

    /** Her real weapon in the player's hands, matching this twin; null if she is not there. */
    @Nullable
    public static ItemStack primaryFor(Player p, ItemStack twin) {
        EdelRaidData d = EdelWeaponItem.data(twin);
        if (d == null) return null;
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof EdelWeaponItem) {
                EdelRaidData o = EdelWeaponItem.data(s);
                if (o != null && o.id().equals(d.id())) return s;
            }
        }
        return null;
    }

    /** Cheap check every 5 ticks: if it is not in the off hand with her in the main hand, it disappears. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 5 != 0) return;
        if (!(entity instanceof Player p) || p.getOffhandItem() != stack) {
            stack.setCount(0);
            return;
        }
        ItemStack main = p.getMainHandItem();
        EdelRaidData mine = EdelWeaponItem.data(stack);
        EdelRaidData hers = main.getItem() instanceof EdelWeaponItem ? EdelWeaponItem.data(main) : null;
        if (mine == null || hers == null || !hers.id().equals(mine.id()) || !main.has(ModComponents.PAIRED.get())) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity itemEntity) {
        if (!itemEntity.level().isClientSide) itemEntity.discard();
        return true;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }

    // ---------------------------------------------------------------- combat: off-hand hits wear her weapon

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide || !(attacker instanceof ServerPlayer p)) return;
        int wear = Config.WEAR_PER_HIT.get();
        ItemStack primary = primaryFor(p, stack);
        if (wear > 0 && primary != null) {
            primary.hurtAndBreak(wear, p, primary == p.getMainHandItem() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return stack.has(ModComponents.SWEEPS.get()) && ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(ability);
    }

    @Override
    public Component getName(ItemStack stack) {
        EdelRaidData d = EdelWeaponItem.data(stack);
        if (d == null) return super.getName(stack);
        return Component.translatable("item.edelraids.edel_weapon.named", d.name(), EdelWeaponItem.formName(d.form()));
    }

    // ---------------------------------------------------------------- GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private EdelWeaponRenderer<EdelTwinItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new EdelWeaponRenderer<>();
                return renderer;
            }
        });
    }
}
