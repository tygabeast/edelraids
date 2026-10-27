package com.tygabeast.edelraids.entity;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.bond.BondMessages;
import com.tygabeast.edelraids.bond.EdelRegistry;
import com.tygabeast.edelraids.bond.ReactManager;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.Elemental;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.data.WeaponForm;
import com.tygabeast.edelraids.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An Edel Raid companion. While she is Reacted this entity does not exist: she lives only as the weapon item,
 * so nothing ticks, pathfinds or syncs for her during combat.
 */
public class EdelRaidEntity extends TamableAnimal implements GeoEntity, Elemental {
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(EdelRaidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRANSFORM = SynchedEntityData.defineId(EdelRaidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REFORM = SynchedEntityData.defineId(EdelRaidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WOUNDED = SynchedEntityData.defineId(EdelRaidEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.edel_raid.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.edel_raid.walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.edel_raid.sit");
    private static final RawAnimation WOUNDED_IDLE = RawAnimation.begin().thenLoop("animation.edel_raid.wounded_idle");
    private static final RawAnimation WOUNDED_WALK = RawAnimation.begin().thenLoop("animation.edel_raid.wounded_walk");
    private static final RawAnimation REACT = RawAnimation.begin().thenPlayAndHold("animation.edel_raid.react");
    private static final RawAnimation REFORM = RawAnimation.begin().thenPlay("animation.edel_raid.reform");

    private static final int DEFAULT_COLOR = 0x4FC9A0;
    private static final String[] NAME_HEADS = {"Re", "Ku", "Ti", "Ce", "Li", "Sa", "Ve", "Mi", "Ari", "Ela", "Fio", "Nor"};
    private static final String[] NAME_TAILS = {"n", "ea", "lel", "ra", "lia", "rin", "na", "ssa", "ette", "wyn", "ria", "ne"};

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable private UUID edelId;
    private String edelName = "";
    private ResourceLocation element = EdelRaids.id("wind");
    private ResourceLocation form = EdelRaids.id("longsword");
    private int quality = 1;
    private int bond;
    private boolean awakened;
    private long stamp;
    private int woundedTicks;
    /** Ticks spent near her Pledger since the last bond point from time together. */
    private int companionTicks;
    /** Her weapon item as last held, minus her data: keeps enchantments, names and other mods' data across reforms. */
    private ItemStack storedWeapon = ItemStack.EMPTY;
    /** Server only: who asked her to React (cleared when React finishes or is cancelled). */
    @Nullable private UUID reactingFor;

    public EdelRaidEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, DEFAULT_COLOR);
        builder.define(DATA_TRANSFORM, 0);
        builder.define(DATA_REFORM, 0);
        builder.define(DATA_WOUNDED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true) {
            @Override public boolean canUse() { return !isBusy() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isBusy() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0, 8.0f, 2.5f));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    // ---------------------------------------------------------------- identity

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (edelId == null) rollIdentity(level.getRandom());
        return result;
    }

    /** A brand-new Edel Raid: a rollable element, a form from that element's pool, quality tier and a name. */
    private void rollIdentity(RandomSource random) {
        Registry<Element> elements = level().registryAccess().registryOrThrow(ModRegistries.ELEMENT);
        List<Holder.Reference<Element>> pool = elements.holders().filter(h -> h.value().rollable()).toList();
        if (pool.isEmpty()) pool = elements.holders().toList();
        ResourceLocation el = element;
        ResourceLocation fo = form;
        if (!pool.isEmpty()) {
            Holder.Reference<Element> picked = pool.get(random.nextInt(pool.size()));
            el = picked.key().location();
            List<ResourceLocation> forms = picked.value().forms();
            if (!forms.isEmpty()) {
                fo = forms.get(random.nextInt(forms.size()));
            } else {
                Optional<Holder.Reference<WeaponForm>> any = level().registryAccess().registryOrThrow(ModRegistries.WEAPON_FORM).getRandom(random);
                if (any.isPresent()) fo = any.get().key().location();
            }
        }
        int roll = random.nextInt(10);
        setIdentity(el, fo, roll < 6 ? 1 : roll < 9 ? 2 : 3,
                NAME_HEADS[random.nextInt(NAME_HEADS.length)] + NAME_TAILS[random.nextInt(NAME_TAILS.length)]);
    }

    /** Give her a fresh identity (new id, stamp 1) and register her in the world. Used by rolls and /edelraids create. */
    public void setIdentity(ResourceLocation newElement, ResourceLocation newForm, int newQuality, String name) {
        edelId = UUID.randomUUID();
        element = newElement;
        form = newForm;
        quality = Math.max(1, Math.min(10, newQuality));
        edelName = name;
        setCustomName(Component.literal(edelName));
        bond = 0;
        awakened = false;
        stamp = 1L;
        refreshColor();
        if (level() instanceof ServerLevel sl) EdelRegistry.get(sl.getServer()).setWorld(edelId, getUUID(), stamp);
    }

    /** A random name for /edelraids create when none is given. */
    public static String randomName(RandomSource random) {
        return NAME_HEADS[random.nextInt(NAME_HEADS.length)] + NAME_TAILS[random.nextInt(NAME_TAILS.length)];
    }

    public EdelRaidData toData() {
        return new EdelRaidData(edelId == null ? UUID.randomUUID() : edelId, edelName, element, form, quality,
                getHealth(), getMaxHealth(), Optional.ofNullable(getOwnerUUID()), bond, awakened, stamp);
    }

    /** Restore her from her weapon's data when she reforms. */
    public void applyData(EdelRaidData d, ItemStack weaponWithoutData) {
        edelId = d.id();
        edelName = d.name();
        element = d.element();
        form = d.form();
        quality = d.quality();
        bond = d.bond();
        awakened = d.awakened();
        stamp = d.stamp();
        storedWeapon = weaponWithoutData;
        if (d.pledger().isPresent()) {
            setTame(true, false);
            setOwnerUUID(d.pledger().get());
        }
        setCustomName(Component.literal(edelName));
        setHealth(Math.max(1f, Math.min(d.health(), getMaxHealth())));
        refreshColor();
    }

    private void refreshColor() {
        Element e = Elements.element(level().registryAccess(), element);
        entityData.set(DATA_COLOR, e == null ? DEFAULT_COLOR : e.color());
    }

    @Override public ResourceLocation getElementId() { return element; }
    @Nullable public UUID getEdelId() { return edelId; }
    public String getEdelName() { return edelName; }
    public ResourceLocation getForm() { return form; }
    public int getQuality() { return quality; }
    public int getBond() { return bond; }
    public long getStamp() { return stamp; }
    public void setStamp(long s) { stamp = s; }
    public ItemStack getStoredWeapon() { return storedWeapon; }
    public int getElementColor() { return entityData.get(DATA_COLOR); }

    public void setBond(int value, boolean awake) {
        bond = Math.max(0, value);
        awakened = awake;
    }

    // ---------------------------------------------------------------- state

    public int getTransformTicks() { return entityData.get(DATA_TRANSFORM); }
    public int getReformTicks() { return entityData.get(DATA_REFORM); }
    public boolean isWounded() { return entityData.get(DATA_WOUNDED); }

    /** Transforming, reforming or wounded: no fighting, no new React. */
    public boolean isBusy() {
        return getTransformTicks() > 0 || getReformTicks() > 0 || isWounded();
    }

    public void beginReact(ServerPlayer player) {
        reactingFor = player.getUUID();
        entityData.set(DATA_TRANSFORM, Config.TRANSFORM_TICKS.get());
        getNavigation().stop();
        setTarget(null);
    }

    public void beginReform() {
        entityData.set(DATA_REFORM, Config.TRANSFORM_TICKS.get());
    }

    public void setWounded(boolean wounded) {
        entityData.set(DATA_WOUNDED, wounded);
        woundedTicks = wounded ? Config.WOUNDED_TICKS.get() : 0;
        if (wounded) {
            setTarget(null);
            getNavigation().stop();
            if (getOwner() instanceof ServerPlayer owner) {
                owner.displayClientMessage(Component.translatable("message.edelraids.wounded", edelName), true);
            }
        }
    }

    /** Free her from her Pledger. The bond belongs to the Pledger, so it resets. */
    public void release() {
        setOwnerUUID(null);
        setTame(false, true);
        setOrderedToSit(false);
        setInSittingPose(false);
        bond = 0;
        awakened = false;
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || getTransformTicks() > 0 || getReformTicks() > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !isRemoved()) serverTick();
    }

    private void serverTick() {
        if (edelId == null) rollIdentity(random);
        if (getTarget() != null && StingRaidEntity.isOutOfFight(getTarget())) setTarget(null);

        int t = getTransformTicks();
        if (t > 0) {
            entityData.set(DATA_TRANSFORM, t - 1);
            if (t == 1) {
                UUID who = reactingFor;
                reactingFor = null;
                ReactManager.finishReact(this, who);
                if (isRemoved()) return;
            }
        }
        int r = getReformTicks();
        if (r > 0) entityData.set(DATA_REFORM, r - 1);

        if (isWounded() && --woundedTicks <= 0) {
            entityData.set(DATA_WOUNDED, false);
            if (getOwner() instanceof ServerPlayer owner) {
                owner.displayClientMessage(Component.translatable("message.edelraids.recovered", edelName), true);
            }
        }

        if (tickCount % 20 == 0) companionBond();

        int regen = Config.REGEN_INTERVAL.get();
        if (regen > 0 && tickCount % regen == 0 && getHealth() < getMaxHealth()) heal(1f);

        // Cheap stale-copy check: one HashMap lookup every 2 seconds, offset per entity so they don't all land on one tick.
        if ((tickCount + getId()) % 40 == 0) validateAgainstRegistry();
    }

    /** Every second: if her Pledger is within 16 blocks, count the time; each full interval adds 1 bond. */
    private void companionBond() {
        int need = Config.BOND_COMPANION_TICKS.get();
        if (need <= 0 || !isTame() || !(getOwner() instanceof ServerPlayer owner) || owner.level() != level()
                || owner.distanceToSqr(this) > 256.0) return;
        companionTicks += 20;
        if (companionTicks < need) return;
        companionTicks = 0;
        EdelRaidData before = toData();
        int newBond = bond + 1;
        setBond(newBond, awakened || newBond >= Config.AWAKEN_BOND.get());
        BondMessages.announce(owner, before, toData());
        if (awakened && !before.awakened()) {
            owner.displayClientMessage(Component.translatable("message.edelraids.awakened", edelName), false);
        }
    }

    private void validateAgainstRegistry() {
        if (!(level() instanceof ServerLevel sl) || edelId == null) return;
        EdelRegistry reg = EdelRegistry.get(sl.getServer());
        EdelRegistry.Entry entry = reg.get(edelId);
        if (entry == null) {
            reg.setWorld(edelId, getUUID(), stamp);
        } else if (entry.held() || entry.stamp() != stamp || !entry.holder().equals(getUUID())) {
            EdelRaids.debug("Discarding stale Edel Raid copy {} ({})", edelName, edelId);
            discard();
        }
    }

    // ---------------------------------------------------------------- interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) return super.mobInteract(player, hand);
        if (level().isClientSide) return (!isTame() || isOwnedBy(player)) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (isBusy()) {
            sp.displayClientMessage(Component.translatable(isWounded() ? "message.edelraids.is_wounded" : "message.edelraids.busy", edelName), true);
            return InteractionResult.CONSUME;
        }
        if (!isTame()) {
            tame(sp);
            setOrderedToSit(false);
            bond = 0;
            awakened = false;
            sp.displayClientMessage(Component.translatable("message.edelraids.bonded", edelName), false);
            return InteractionResult.SUCCESS;
        }
        if (!isOwnedBy(sp)) {
            sp.displayClientMessage(Component.translatable("message.edelraids.not_your_pledger", edelName), true);
            return InteractionResult.CONSUME;
        }
        if (sp.isShiftKeyDown()) {
            ReactManager.startReact(this, sp);
            return InteractionResult.SUCCESS;
        }
        boolean sit = !isOrderedToSit();
        setOrderedToSit(sit);
        setInSittingPose(sit);
        setJumping(false);
        getNavigation().stop();
        setTarget(null);
        sp.displayClientMessage(Component.translatable(sit ? "message.edelraids.stay" : "message.edelraids.follow", edelName), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return !isBusy() && !StingRaidEntity.isOutOfFight(target)
                && !(target instanceof TamableAnimal other && other.isOwnedBy(owner)) && super.wantsToAttack(target, owner);
    }

    // ---------------------------------------------------------------- damage, death, removal

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.isInvulnerableTo(source);
        return isBusy() || super.isInvulnerableTo(source);
    }

    /** No permadeath: lethal damage leaves her wounded. Only /kill truly removes her; the void sends her home. */
    @Override
    public void die(DamageSource source) {
        if (level().isClientSide || source.is(DamageTypes.GENERIC_KILL)) {
            super.die(source);
            return;
        }
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && level() instanceof ServerLevel sl) {
            LivingEntity owner = getOwner();
            boolean ownerSafe = owner != null && owner.isAlive() && owner.level() == sl && owner.getY() > sl.getMinBuildHeight();
            Vec3 safe = ownerSafe ? owner.position() : Vec3.atBottomCenterOf(sl.getSharedSpawnPos());
            teleportTo(safe.x, safe.y, safe.z);
            resetFallDistance();
            setDeltaMovement(Vec3.ZERO);
        }
        setHealth(1f);
        setWounded(true);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.KILLED && edelId != null && level() instanceof ServerLevel sl) {
            EdelRegistry.get(sl.getServer()).remove(edelId);
        }
        super.remove(reason);
    }

    // ---------------------------------------------------------------- save / load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (edelId != null) tag.putUUID("EdelId", edelId);
        tag.putString("EdelName", edelName);
        tag.putString("Element", element.toString());
        tag.putString("Form", form.toString());
        tag.putInt("Quality", quality);
        tag.putInt("Bond", bond);
        tag.putBoolean("Awakened", awakened);
        tag.putLong("Stamp", stamp);
        tag.putBoolean("Wounded", isWounded());
        tag.putInt("WoundedTicks", woundedTicks);
        tag.putInt("CompanionTicks", companionTicks);
        if (!storedWeapon.isEmpty()) tag.put("Weapon", storedWeapon.save(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("EdelId")) edelId = tag.getUUID("EdelId");
        edelName = tag.getString("EdelName");
        ResourceLocation el = ResourceLocation.tryParse(tag.getString("Element"));
        if (el != null) element = el;
        ResourceLocation fo = ResourceLocation.tryParse(tag.getString("Form"));
        if (fo != null) form = fo;
        quality = Math.max(1, tag.getInt("Quality"));
        bond = tag.getInt("Bond");
        awakened = tag.getBoolean("Awakened");
        stamp = tag.getLong("Stamp");
        entityData.set(DATA_WOUNDED, tag.getBoolean("Wounded"));
        woundedTicks = tag.getInt("WoundedTicks");
        companionTicks = tag.getInt("CompanionTicks");
        storedWeapon = tag.contains("Weapon") ? ItemStack.parse(registryAccess(), tag.get("Weapon")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        refreshColor();
    }

    // ---------------------------------------------------------------- vanilla animal plumbing

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public boolean canMate(net.minecraft.world.entity.animal.Animal other) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    // ---------------------------------------------------------------- GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (getTransformTicks() > 0) return state.setAndContinue(REACT);
            if (getReformTicks() > 0) return state.setAndContinue(REFORM);
            if (isWounded()) return state.setAndContinue(state.isMoving() ? WOUNDED_WALK : WOUNDED_IDLE);
            if (isInSittingPose()) return state.setAndContinue(SIT);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
