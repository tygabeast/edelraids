package com.tygabeast.edelraids.entity;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.bond.StingRegistry;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.Elemental;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.data.StingArm;
import com.tygabeast.edelraids.event.CommonEvents;
import com.tygabeast.edelraids.registry.ModItems;
import com.tygabeast.edelraids.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * A Sting Raid: a girl implanted with an artificial stone. She Reacts by herself, turning her forearm into a weapon,
 * and the stone strains the longer she stays Reacted. Hunters' Sting Raids are hostile; beaten, she is subdued and can
 * be freed. Freed, she follows like an Edel Raid but never bonds, sings or Reacts with anyone.
 * <p>
 * All her visible state is one byte of flags, one arm byte and one coarse strain byte, so a fight sends very little.
 */
public class StingRaidEntity extends TamableAnimal implements GeoEntity, Elemental, RangedAttackMob {
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(StingRaidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ARM = SynchedEntityData.defineId(StingRaidEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(StingRaidEntity.class, EntityDataSerializers.BYTE);
    /** Strain in steps of 5 (0-20): enough for the gem's flicker, and at most 20 updates per full strain cycle. */
    private static final EntityDataAccessor<Byte> DATA_STRAIN = SynchedEntityData.defineId(StingRaidEntity.class, EntityDataSerializers.BYTE);

    private static final int F_REACTED = 1, F_REACTING = 2, F_STAGGERED = 4, F_SUBDUED = 8, F_STABILIZED = 16;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.sting_raid.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.sting_raid.walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.sting_raid.sit");
    private static final RawAnimation SUBDUED_IDLE = RawAnimation.begin().thenLoop("animation.sting_raid.wounded_idle");
    private static final RawAnimation SUBDUED_WALK = RawAnimation.begin().thenLoop("animation.sting_raid.wounded_walk");
    private static final RawAnimation SELF_REACT = RawAnimation.begin().thenPlay("animation.sting_raid.self_react");
    private static final RawAnimation REACTED_IDLE = RawAnimation.begin().thenLoop("animation.sting_raid.reacted_idle");
    private static final RawAnimation REACTED_WALK = RawAnimation.begin().thenLoop("animation.sting_raid.reacted_walk");
    private static final RawAnimation STAGGER = RawAnimation.begin().thenLoop("animation.sting_raid.stagger");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.sting_raid.attack");
    private static final RawAnimation AIM = RawAnimation.begin().thenLoop("animation.sting_raid.aim");

    private static final ResourceLocation REACT_DAMAGE = EdelRaids.id("sting_react");
    private static final ResourceLocation SUBDUED_SLOW = EdelRaids.id("sting_subdued");
    private static final int REACT_ANIM_TICKS = 10;
    private static final float RE_REACT_MAX = 50f;
    private static final int REST_RADIUS = 8;
    private static final int DEFAULT_COLOR = 0x9FB0A8;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private boolean rolled;
    private String stingName = "";
    private ResourceLocation element = EdelRaids.id("earth");
    private ResourceLocation form = EdelRaids.id("longsword");
    private float strain;
    private int reactAnimTicks;
    private int staggerTicks;
    private int idleTicks;
    private int subduedTicks;
    private int nextAttackTick;
    /** Set when a target goal tried to hand her a refused target; the goal is stopped on her next tick. */
    private boolean staleTargetGoal;
    /** A freed Sting Raid told to rest stays near restPos, does not follow, and does not count toward the active limit. */
    private boolean resting;
    /** Freed but not taken (her rescuer had no free active slot): counts down, and she leaves at 0. */
    private int lingerTicks;
    @Nullable private BlockPos restPos;

    public StingRaidEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, DEFAULT_COLOR);
        builder.define(DATA_ARM, (byte) 0);
        builder.define(DATA_FLAGS, (byte) 0);
        builder.define(DATA_STRAIN, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, StingArm.LAUNCHER.attackInterval, 15f) {
            @Override public boolean canUse() { return usesLauncher() && super.canUse(); }
            @Override public boolean canContinueToUse() { return usesLauncher() && super.canContinueToUse(); }
            @Override public void start() { super.start(); setAggressive(true); }
            @Override public void stop() { super.stop(); setAggressive(false); }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true) {
            @Override public boolean canUse() { return canFight() && !usesLauncher() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canFight() && !usesLauncher() && super.canContinueToUse(); }

            /** Her own cooldown, so the heavy arm can swing slower than vanilla's fixed 20 ticks. */
            @Override protected void checkAndPerformAttack(LivingEntity target) {
                if (tickCount < nextAttackTick || !canPerformAttack(target)) return;
                nextAttackTick = tickCount + (isReacted() ? getArm().attackInterval : 20);
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(target);
            }
        });
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0, 8.0f, 2.5f) {
            @Override public boolean canUse() { return !resting && super.canUse(); }
            @Override public boolean canContinueToUse() { return !resting && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return !isSubdued() && super.canUse(); }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this) {
            @Override public boolean canUse() { return !resting && super.canUse(); }
        });
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this) {
            @Override public boolean canUse() { return !resting && super.canUse(); }
        });
        targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers(StingRaidEntity.class));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> true) {
            @Override public boolean canUse() { return isHostile() && level().getDifficulty() != Difficulty.PEACEFUL && super.canUse(); }
        });
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, TamableAnimal.class, 10, true, false,
                t -> ((TamableAnimal) t).isTame() && (t instanceof EdelRaidEntity || t instanceof StingRaidEntity) && !isOutOfFight(t)) {
            @Override public boolean canUse() { return isHostile() && super.canUse(); }
        });
    }

    // ---------------------------------------------------------------- identity

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (!rolled) rollIdentity(level.getRandom());
        return result;
    }

    /** A rollable element (Ring 4 stays in Edel Garden), a form from its pool for her arm type, and a name. */
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
            if (!forms.isEmpty()) fo = forms.get(random.nextInt(forms.size()));
        }
        setIdentity(el, fo, EdelRaidEntity.randomName(random));
    }

    public void setIdentity(ResourceLocation newElement, ResourceLocation newForm, String name) {
        rolled = true;
        element = newElement;
        form = newForm;
        stingName = name;
        setCustomName(Component.literal(name));
        entityData.set(DATA_ARM, (byte) StingArm.of(level().registryAccess(), form).ordinal());
        refreshColor();
    }

    private void refreshColor() {
        Element e = Elements.element(level().registryAccess(), element);
        entityData.set(DATA_COLOR, e == null ? DEFAULT_COLOR : e.color());
    }

    @Override public ResourceLocation getElementId() { return element; }
    public ResourceLocation getForm() { return form; }
    public String getStingName() { return stingName; }
    public int getElementColor() { return entityData.get(DATA_COLOR); }
    public StingArm getArm() {
        int i = entityData.get(DATA_ARM);
        return StingArm.values()[Math.max(0, Math.min(StingArm.values().length - 1, i))];
    }

    // ---------------------------------------------------------------- state

    private boolean flag(int f) { return (entityData.get(DATA_FLAGS) & f) != 0; }

    private void setFlag(int f, boolean on) {
        byte b = entityData.get(DATA_FLAGS);
        byte n = (byte) (on ? b | f : b & ~f);
        if (n != b) entityData.set(DATA_FLAGS, n);
    }

    public boolean isReacted() { return flag(F_REACTED); }
    public boolean isReacting() { return flag(F_REACTING); }
    public boolean isStaggered() { return flag(F_STAGGERED); }
    public boolean isSubdued() { return flag(F_SUBDUED); }
    public boolean isStabilized() { return flag(F_STABILIZED); }
    /** A hunter's Sting Raid: not owned, and not freed and waiting for someone to take her. */
    public boolean isHostile() { return !isTame() && lingerTicks <= 0; }
    public boolean isLingering() { return !isTame() && lingerTicks > 0; }
    public boolean isResting() { return resting; }
    /** 0-100, in steps of 5 on the client. */
    public int getStrainDisplay() { return entityData.get(DATA_STRAIN) * 5; }

    private boolean canFight() { return !isSubdued() && !isStaggered() && !isReacting(); }
    private boolean usesLauncher() { return canFight() && isReacted() && getArm() == StingArm.LAUNCHER; }

    /**
     * Out of the fight, so nobody targets them: subdued Sting Raids, busy (transforming or wounded), captive or loyal
     * Edel Raids, and yielded Arc Aile agents.
     */
    public static boolean isOutOfFight(LivingEntity e) {
        return (e instanceof StingRaidEntity s && s.isSubdued())
                || (e instanceof EdelRaidEntity r && (r.isBusy() || r.isCaptive() || r.isLoyal() || r.isSheltered()))
                || (e instanceof ArcAileAgentEntity a && a.isYielded());
    }

    /** A hunter or an unbeaten hunter's Sting Raid within 16 blocks: a camp's captives cannot be freed while one remains. */
    public static boolean guardsNear(LivingEntity captive) {
        return !captive.level().getEntitiesOfClass(LivingEntity.class, captive.getBoundingBox().inflate(16.0),
                e -> e.isAlive() && (e instanceof HunterEntity || (e instanceof StingRaidEntity s && s.isHostile() && !s.isSubdued()))).isEmpty();
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || isReacting() || isStaggered();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !isRemoved()) serverTick();
    }

    private void serverTick() {
        if (!rolled) rollIdentity(random);

        // A target that became refused since it was set (she was freed, told to sit, or it was subdued), or a target goal
        // that keeps offering one, is dropped together with the goal holding it.
        LivingEntity target = getTarget();
        if (staleTargetGoal || (target != null && refusesTarget(target))) {
            staleTargetGoal = false;
            stopTargetGoals(this, targetSelector);
            target = null;
        }
        if (isOrderedToSit() && (isReacted() || isReacting())) revert(false);

        if (isLingering() && --lingerTicks == 0) {
            EdelRaids.debug("Sting Raid {} left after nobody took her", stingName);
            escape();
            return;
        }
        if (isSubdued()) {
            if (isHostile() && subduedTicks > 0 && --subduedTicks == 0) escape();
            return;
        }

        if (staggerTicks > 0 && --staggerTicks == 0) setFlag(F_STAGGERED, false);

        if (reactAnimTicks > 0 && --reactAnimTicks == 0) finishSelfReact();

        if (isReacted()) {
            idleTicks = target == null ? idleTicks + 1 : 0;
            double rate = Config.STING_STRAIN_PER_SECOND.get() * (isStabilized() ? Config.STING_STABILIZED_FACTOR.get() : 1.0);
            setStrain(strain + (float) (rate / 20.0));
            if (strain >= 100f) revert(true);
            else if (idleTicks >= Config.STING_REVERT_TICKS.get()) revert(false);
        } else {
            if (strain > 0f) setStrain(strain - (float) (Config.STING_STRAIN_RECOVERY.get() / 20.0));
            if (!isReacting() && !isStaggered() && strain <= RE_REACT_MAX && target != null && target.isAlive()
                    && distanceToSqr(target) < 256.0) startSelfReact();
        }

        int regen = Config.REGEN_INTERVAL.get();
        if (regen > 0 && tickCount % regen == 0 && getHealth() < getMaxHealth()) heal(1f);

        // A freed Sting Raid keeps her place in her owner's count even if the record was lost; checked every 10 seconds.
        if (isTame() && (tickCount + getId()) % 200 == 0 && getOwnerUUID() != null && level() instanceof ServerLevel sl) {
            StingRegistry.get(sl.getServer()).put(getOwnerUUID(), getUUID(), !resting);
        }
    }

    private void setStrain(float value) {
        strain = Math.max(0f, Math.min(100f, value));
        byte step = (byte) Math.round(strain / 5f);
        if (entityData.get(DATA_STRAIN) != step) entityData.set(DATA_STRAIN, step);
    }

    private void startSelfReact() {
        reactAnimTicks = REACT_ANIM_TICKS;
        setFlag(F_REACTING, true);
        getNavigation().stop();
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.3f);
    }

    private void finishSelfReact() {
        setFlag(F_REACTING, false);
        setFlag(F_REACTED, true);
        idleTicks = 0;
        EdelRaids.debug("Sting Raid {} self-Reacted ({})", stingName, getArm().id());
        AttributeInstance dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.addOrUpdateTransientModifier(new AttributeModifier(REACT_DAMAGE,
                getArm().bonusDamage * Config.STING_DAMAGE_SCALE.get(), AttributeModifier.Operation.ADD_VALUE));
    }

    /** Back to her own arm. Forced out by strain, she is staggered: a clear opening for whoever she is fighting. */
    private void revert(boolean staggered) {
        if (isReacted() || isReacting()) EdelRaids.debug("Sting Raid {} reverted (staggered: {}, strain {})", stingName, staggered, (int) strain);
        reactAnimTicks = 0;
        setFlag(F_REACTING, false);
        setFlag(F_REACTED, false);
        AttributeInstance dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.removeModifier(REACT_DAMAGE);
        if (staggered) {
            staggerTicks = Config.STING_STAGGER_TICKS.get();
            setFlag(F_STAGGERED, staggerTicks > 0);
            getNavigation().stop();
            playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.0f, 0.6f);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY(1.6), getZ(), 12, 0.2, 0.2, 0.2, 0.15);
            }
        }
    }

    // ---------------------------------------------------------------- subdued, freed, revived

    private void subdue(DamageSource source) {
        revert(false);
        staggerTicks = 0;
        setFlag(F_STAGGERED, false);
        setFlag(F_SUBDUED, true);
        EdelRaids.debug("Sting Raid {} subdued (hostile: {})", stingName, isHostile());
        setTarget(null);
        getNavigation().stop();
        setOrderedToSit(false);
        setInSittingPose(false);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(SUBDUED_SLOW, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (isHostile()) {
            subduedTicks = Config.STING_ESCAPE_TICKS.get();
            // Beating a hunter's Sting Raid counts like a kill for the Pledger's bond.
            if (source.getEntity() instanceof ServerPlayer killer) CommonEvents.gainBond(killer);
        } else if (getOwner() instanceof ServerPlayer owner) {
            owner.displayClientMessage(Component.translatable("message.edelraids.sting_failing", stingName), false);
        }
    }

    private void steadyStone() {
        setFlag(F_SUBDUED, false);
        subduedTicks = 0;
        setStrain(0f);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SUBDUED_SLOW);
        setHealth(Math.max(getHealth(), getMaxHealth() * 0.5f));
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(1.0), getZ(), 8, 0.3, 0.4, 0.3, 0.0);
    }

    /**
     * Freed from the hunters. With a free active slot she joins the player at once; otherwise she waits nearby for a
     * few minutes for anyone with room to take her, then leaves. Whatever she was fighting before is forgotten.
     */
    public void free(ServerPlayer p) {
        subduedTicks = 0;
        setLastHurtByMob(null);
        stopTargetGoals(this, targetSelector);
        int limit = Config.STING_LIMIT.get();
        if (StingRegistry.get(p.getServer()).activeCount(p.getUUID()) < limit) {
            join(p);
            p.displayClientMessage(Component.translatable("message.edelraids.sting_freed", stingName), false);
        } else {
            lingerTicks = Config.FREED_LINGER_TICKS.get();
            p.displayClientMessage(Component.translatable("message.edelraids.sting_freed_lingering", stingName, limit), false);
        }
    }

    /** She becomes this player's active companion. */
    private void join(ServerPlayer p) {
        lingerTicks = 0;
        tame(p);
        setOrderedToSit(false);
        setResting(false);
        StingRegistry.get(p.getServer()).put(p.getUUID(), getUUID(), true);
    }

    private void setResting(boolean rest) {
        resting = rest;
        if (rest) {
            restPos = blockPosition();
            restrictTo(restPos, REST_RADIUS);
            setOrderedToSit(false);
            setInSittingPose(false);
        } else {
            restPos = null;
            clearRestriction();
        }
        setTarget(null);
        getNavigation().stop();
    }

    /** Sneak + right-click: rest here, or come along again if the owner has room for another active Sting Raid. */
    private void toggleResting(ServerPlayer p) {
        StingRegistry reg = StingRegistry.get(p.getServer());
        if (resting) {
            int limit = Config.STING_LIMIT.get();
            if (reg.activeCount(p.getUUID()) >= limit) {
                p.displayClientMessage(Component.translatable("message.edelraids.sting_limit", limit), true);
                return;
            }
            setResting(false);
            reg.put(p.getUUID(), getUUID(), true);
            p.displayClientMessage(Component.translatable("message.edelraids.sting_active", stingName), true);
        } else {
            setResting(true);
            reg.put(p.getUUID(), getUUID(), false);
            p.displayClientMessage(Component.translatable("message.edelraids.sting_resting", stingName), true);
        }
    }

    /** /edelraids release: she leaves to find her own way, and her place in the owner's count is freed. */
    public void releaseAndLeave() {
        if (level() instanceof ServerLevel sl) {
            if (getOwnerUUID() != null) StingRegistry.get(sl.getServer()).remove(getOwnerUUID(), getUUID());
            sl.sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(), 16, 0.3, 0.6, 0.3, 0.02);
        }
        discard();
    }

    /** A subdued hunter's Sting Raid nobody freed slips away. */
    private void escape() {
        EdelRaids.debug("Sting Raid {} escaped", stingName);
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(), 16, 0.3, 0.6, 0.3, 0.02);
        discard();
    }

    // ---------------------------------------------------------------- interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return super.mobInteract(player, hand);
        ItemStack held = player.getItemInHand(hand);
        boolean stabilizer = held.is(ModItems.STONE_STABILIZER.get());
        boolean refined = held.is(ModItems.REFINED_STABILIZER.get());
        if (!held.isEmpty() && !stabilizer && !refined) return super.mobInteract(player, hand);
        boolean mine = isOwnedBy(player);
        if (level().isClientSide) return (mine || isLingering() || (!isTame() && isSubdued())) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (isTame() && !mine) {
            sp.displayClientMessage(Component.translatable("message.edelraids.sting_not_yours", stingName), true);
            return InteractionResult.CONSUME;
        }
        if (isHostile()) {
            if (!isSubdued()) return InteractionResult.PASS;
            free(sp);
            if (!stabilizer && !refined) {
                if (isTame()) sp.displayClientMessage(Component.translatable("message.edelraids.sting_failing", stingName), false);
                return InteractionResult.SUCCESS;
            }
        } else if (isLingering() && !stabilizer && !refined) {
            int limit = Config.STING_LIMIT.get();
            if (StingRegistry.get(sp.getServer()).activeCount(sp.getUUID()) >= limit) {
                sp.displayClientMessage(Component.translatable("message.edelraids.sting_limit", limit), true);
                return InteractionResult.CONSUME;
            }
            join(sp);
            sp.displayClientMessage(Component.translatable(isSubdued() ? "message.edelraids.sting_failing" : "message.edelraids.sting_freed", stingName), false);
            return InteractionResult.SUCCESS;
        }

        if (refined) {
            if (isStabilized() && !isSubdued()) {
                sp.displayClientMessage(Component.translatable("message.edelraids.sting_already_stable", stingName), true);
                return InteractionResult.CONSUME;
            }
            setFlag(F_STABILIZED, true);
            if (isSubdued()) steadyStone();
            held.consume(1, sp);
            sp.displayClientMessage(Component.translatable("message.edelraids.sting_stabilized", stingName), false);
            return InteractionResult.SUCCESS;
        }
        if (stabilizer) {
            if (!isSubdued()) {
                sp.displayClientMessage(Component.translatable("message.edelraids.sting_steady", stingName), true);
                return InteractionResult.CONSUME;
            }
            steadyStone();
            held.consume(1, sp);
            sp.displayClientMessage(Component.translatable("message.edelraids.sting_revived", stingName), false);
            return InteractionResult.SUCCESS;
        }

        if (isSubdued()) {
            sp.displayClientMessage(Component.translatable("message.edelraids.sting_failing", stingName), true);
            return InteractionResult.CONSUME;
        }
        if (sp.isShiftKeyDown()) {
            toggleResting(sp);
            return InteractionResult.SUCCESS;
        }
        if (resting) {
            sp.displayClientMessage(Component.translatable("message.edelraids.sting_is_resting", stingName), true);
            return InteractionResult.CONSUME;
        }
        boolean sit = !isOrderedToSit();
        setOrderedToSit(sit);
        setInSittingPose(sit);
        setJumping(false);
        getNavigation().stop();
        setTarget(null);
        sp.displayClientMessage(Component.translatable(sit ? "message.edelraids.stay" : "message.edelraids.follow", stingName), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (isOutOfFight(target)) return false;
        if (target instanceof TamableAnimal t && t.isOwnedBy(owner)) return false;
        return super.wantsToAttack(target, owner);
    }

    /**
     * Who she must never fight: an ally (including companions of her own owner, which covers a grudge from before she
     * was freed), anyone out of the fight, a player while hunters are peaceful, or anyone at all while she sits.
     */
    private boolean refusesTarget(LivingEntity t) {
        return isOrderedToSit() || isOutOfFight(t) || isAlliedTo(t)
                || (isHostile() && t instanceof Player && level().getDifficulty() == Difficulty.PEACEFUL);
    }

    /**
     * Vanilla's running target goal re-applies the target it remembers every tick, so clearing the target is not
     * enough: a refused target is turned away here, and the goal holding it is stopped on her next tick.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !level().isClientSide && refusesTarget(target)) {
            target = null;
            staleTargetGoal = true;
        }
        super.setTarget(target);
    }

    /** Stops every running target goal, which also forgets the target it was holding. Never called mid goal-tick. */
    public static void stopTargetGoals(Mob mob, GoalSelector targets) {
        mob.setTarget(null);
        targets.getAvailableGoals().forEach(g -> {
            if (g.isRunning()) g.stop();
        });
    }

    /** Hunters and their Sting Raids never fight each other, and neither do companions of the same owner. */
    @Override
    public boolean isAlliedTo(Entity other) {
        if (isHostile() && (other instanceof HunterEntity || (other instanceof StingRaidEntity s && s.isHostile()))) return true;
        return sameOwner(this, other) || super.isAlliedTo(other);
    }

    /** Both are owned (tamed) by the same player: Edel Raids, Sting Raids, wolves and so on. */
    public static boolean sameOwner(Entity a, Entity b) {
        if (!(a instanceof OwnableEntity oa) || !(b instanceof OwnableEntity ob)) return false;
        UUID owner = oa.getOwnerUUID();
        return owner != null && owner.equals(ob.getOwnerUUID());
    }

    // ---------------------------------------------------------------- ranged (launcher arm)

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(3.0 * Config.STING_DAMAGE_SCALE.get());
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + flat * 0.2, dz, 1.6f, 14 - level().getDifficulty().getId() * 4);
        swing(InteractionHand.MAIN_HAND);
        playSound(SoundEvents.CROSSBOW_SHOOT, 1.0f, 1.2f);
        level().addFreshEntity(arrow);
    }

    // ---------------------------------------------------------------- damage, death, removal

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.isInvulnerableTo(source);
        return isSubdued() || super.isInvulnerableTo(source);
    }

    /** No one is killed: lethal damage subdues her. Only /kill truly removes her; the void sends a freed one home. */
    @Override
    public void die(DamageSource source) {
        if (level().isClientSide || source.is(DamageTypes.GENERIC_KILL)) {
            super.die(source);
            return;
        }
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && level() instanceof ServerLevel sl) {
            if (!isTame()) {
                escape();
                return;
            }
            LivingEntity owner = getOwner();
            boolean ownerSafe = owner != null && owner.isAlive() && owner.level() == sl && owner.getY() > sl.getMinBuildHeight();
            Vec3 safe = ownerSafe ? owner.position() : Vec3.atBottomCenterOf(sl.getSharedSpawnPos());
            teleportTo(safe.x, safe.y, safe.z);
            resetFallDistance();
            setDeltaMovement(Vec3.ZERO);
        }
        setHealth(1f);
        subdue(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.KILLED && getOwnerUUID() != null && level() instanceof ServerLevel sl) {
            StingRegistry.get(sl.getServer()).remove(getOwnerUUID(), getUUID());
        }
        super.remove(reason);
    }

    // ---------------------------------------------------------------- save / load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Rolled", rolled);
        tag.putString("StingName", stingName);
        tag.putString("Element", element.toString());
        tag.putString("Form", form.toString());
        tag.putFloat("Strain", strain);
        tag.putBoolean("Stabilized", isStabilized());
        tag.putBoolean("Subdued", isSubdued());
        tag.putInt("SubduedTicks", subduedTicks);
        tag.putBoolean("Resting", resting);
        tag.putInt("LingerTicks", lingerTicks);
        if (restPos != null) tag.putLong("RestPos", restPos.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rolled = tag.getBoolean("Rolled");
        stingName = tag.getString("StingName");
        ResourceLocation el = ResourceLocation.tryParse(tag.getString("Element"));
        if (el != null) element = el;
        ResourceLocation fo = ResourceLocation.tryParse(tag.getString("Form"));
        if (fo != null) form = fo;
        setStrain(tag.getFloat("Strain"));
        setFlag(F_STABILIZED, tag.getBoolean("Stabilized"));
        subduedTicks = tag.getInt("SubduedTicks");
        resting = tag.getBoolean("Resting");
        lingerTicks = tag.getInt("LingerTicks");
        if (resting) {
            restPos = tag.contains("RestPos") ? BlockPos.of(tag.getLong("RestPos")) : blockPosition();
            restrictTo(restPos, REST_RADIUS);
        }
        if (tag.getBoolean("Subdued")) {
            setFlag(F_SUBDUED, true);
            AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(SUBDUED_SLOW, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (rolled) {
            if (!hasCustomName() && !stingName.isEmpty()) setCustomName(Component.literal(stingName));
            entityData.set(DATA_ARM, (byte) StingArm.of(registryAccess(), form).ordinal());
            refreshColor();
        }
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
            if (isReacting()) return state.setAndContinue(SELF_REACT);
            if (isStaggered()) return state.setAndContinue(STAGGER);
            if (isSubdued()) return state.setAndContinue(state.isMoving() ? SUBDUED_WALK : SUBDUED_IDLE);
            if (isInSittingPose()) return state.setAndContinue(SIT);
            if (isReacted()) return state.setAndContinue(state.isMoving() ? REACTED_WALK : REACTED_IDLE);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        // Only the weapon arm: swings and aiming play over whatever the body is doing.
        controllers.add(new AnimationController<>(this, "arm", 2, state -> {
            if (!isReacted() || isStaggered() || isSubdued()) return PlayState.STOP;
            if (swinging) return state.setAndContinue(ATTACK);
            if (getArm() == StingArm.LAUNCHER && isAggressive()) return state.setAndContinue(AIM);
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
