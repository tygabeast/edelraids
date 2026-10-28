package com.tygabeast.edelraids.entity;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.EdelRaids;
import com.tygabeast.edelraids.bond.ArcAileStanding;
import com.tygabeast.edelraids.bond.ReactManager;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.WeaponForm;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.registry.ModEntities;
import com.tygabeast.edelraids.registry.ModItems;
import com.tygabeast.edelraids.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An Arc Aile agent: a man whose Edel Raid partner fights as his weapon, Reacted in his hand. Arc Aile means well by its
 * own lights, so agents leave ordinary people alone and confront Pledgers, whom they see as exploiting Edel Raids.
 * <p>
 * Beaten, an agent yields instead of dying. He is cleansed of every effect, fire and freezing, nothing but a player's
 * own deliberate hit can harm him, and no companion will keep fighting him. The player who beat him (directly or
 * through a companion) then spares him (right-click), or any player finishes him (strikes again). Beaten by anything
 * else, he does not yield to a bystander: he and his partner retreat at once, and nobody's standing changes. Sparing raises the player's standing with Arc Aile; killing lowers it sharply, and
 * his partner, who chose this life, grieves and returns to Edel Garden. She never bonds with his killer.
 */
public class ArcAileAgentEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> DATA_YIELDED = SynchedEntityData.defineId(ArcAileAgentEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String[] CONFRONT_LINES = {"message.edelraids.agent_confront_1", "message.edelraids.agent_confront_2", "message.edelraids.agent_confront_3"};

    /** Ticks until a yielded agent (and his partner) withdraw; set on yield, shortened by a decision. */
    private int withdrawTicks;
    @Nullable private UUID partnerEntity;
    /** The player he yielded to: the one who beat him, or whose companion did. Only they may spare him. */
    @Nullable private UUID victor;
    private boolean staleTargetGoal;
    @Nullable private UUID lastConfronted;

    public ArcAileAgentEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setDropChance(EquipmentSlot.MAINHAND, 0f); // his partner is never a drop
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_YIELDED, false);
    }

    public boolean isYielded() {
        return entityData.get(DATA_YIELDED);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override public boolean canUse() { return !isYielded() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isYielded() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return !isYielded() && super.canUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this, ArcAileAgentEntity.class).setAlertOthers(ArcAileAgentEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p instanceof ServerPlayer sp && confronts(sp)));
        // Rival rescuers: a patrol that comes upon hunters fights them.
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HunterEntity.class, true));
    }

    /** Agents confront Pledgers, unless that player has earned a truce. Runs only when the target goal looks. */
    private boolean confronts(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return false;
        if (ArcAileStanding.get(p.getServer()).truce(p.getUUID())) return false;
        return isPledger(p);
    }

    /** Carries one of her own Edel Raids' weapon forms, or has one of them within 32 blocks. */
    public static boolean isPledger(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            EdelRaidData d = EdelWeaponItem.data(inv.getItem(i));
            if (d != null && d.isPledger(p.getUUID())) return true;
        }
        return !p.level().getEntitiesOfClass(EdelRaidEntity.class, p.getBoundingBox().inflate(32.0), e -> e.isOwnedBy(p)).isEmpty();
    }

    // ---------------------------------------------------------------- spawning and his partner

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (getMainHandItem().isEmpty()) equipPartner(level.getRandom());
        return result;
    }

    /** A partner of a rollable element with one of her element's melee forms, Reacted in his hand. */
    private void equipPartner(RandomSource random) {
        var access = level().registryAccess();
        List<Holder.Reference<Element>> pool = access.registryOrThrow(ModRegistries.ELEMENT).holders()
                .filter(h -> h.value().rollable()).toList();
        if (pool.isEmpty()) return;
        Holder.Reference<Element> el = pool.get(random.nextInt(pool.size()));
        List<ResourceLocation> forms = el.value().forms().stream().filter(f -> {
            WeaponForm wf = access.registryOrThrow(ModRegistries.WEAPON_FORM).get(f);
            return wf != null && !wf.ranged() && !wf.paired();
        }).toList();
        ResourceLocation form = forms.isEmpty() ? EdelRaids.id("longsword") : forms.get(random.nextInt(forms.size()));
        int roll = random.nextInt(10);
        EdelRaidData d = new EdelRaidData(UUID.randomUUID(), EdelRaidEntity.randomName(random), el.key().location(), form,
                roll < 5 ? 1 : roll < 9 ? 2 : 3, 20f, 20f, Optional.empty(), 0, false, 0L);
        setItemSlot(EquipmentSlot.MAINHAND, ReactManager.buildWeapon(d, ItemStack.EMPTY, access));
    }

    @Nullable
    public EdelRaidData partner() {
        return EdelWeaponItem.data(getMainHandItem());
    }

    // ---------------------------------------------------------------- targeting

    /** A yielded agent fights no one; nobody fights a yielded agent (see StingRaidEntity#isOutOfFight). */
    private boolean refusesTarget(LivingEntity t) {
        return isYielded() || StingRaidEntity.isOutOfFight(t) || isAlliedTo(t);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !level().isClientSide && refusesTarget(target)) {
            target = null;
            staleTargetGoal = true;
        }
        if (target instanceof ServerPlayer sp && !sp.getUUID().equals(lastConfronted)) {
            lastConfronted = sp.getUUID();
            sp.displayClientMessage(Component.translatable(CONFRONT_LINES[random.nextInt(CONFRONT_LINES.length)]), true);
        }
        super.setTarget(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof ArcAileAgentEntity || super.isAlliedTo(other);
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) return;
        if (staleTargetGoal || (getTarget() != null && refusesTarget(getTarget()))) {
            staleTargetGoal = false;
            StingRaidEntity.stopTargetGoals(this, targetSelector);
        }
        if (isYielded()) {
            if (--withdrawTicks <= 0) withdraw();
            return;
        }
        // An agent created without the usual spawn setup (a command with data, another mod) gets his partner here.
        if (getMainHandItem().isEmpty()) equipPartner(random);
        // Rival rescue: once a camp's guards are gone, Arc Aile takes its captives to Edel Garden. Every 2 seconds.
        if ((tickCount + getId()) % 40 == 0) takeCaptives();
    }

    private void takeCaptives() {
        List<EdelRaidEntity> captives = level().getEntitiesOfClass(EdelRaidEntity.class, getBoundingBox().inflate(16.0), EdelRaidEntity::isCaptive);
        if (captives.isEmpty() || StingRaidEntity.guardsNear(captives.get(0))) return;
        for (EdelRaidEntity c : captives) c.takenByArcAile();
    }

    // ---------------------------------------------------------------- yielding, sparing, killing

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.isInvulnerableTo(source);
        // Once he yields, only a player's own hit can harm him: no damage over time, no other mob, no stray companion.
        if (isYielded() && !(source.getEntity() instanceof Player)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public void die(DamageSource source) {
        if (level().isClientSide || source.is(DamageTypes.GENERIC_KILL)) {
            releasePartner(false, null);
            super.die(source);
            return;
        }
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            releasePartner(false, null);
            discard();
            return;
        }
        if (!isYielded()) {
            UUID won = victorOf(source.getEntity());
            // A blow with no attacker (burning from a Fire Edel Raid, a fall): whoever last hit him, if a player's side.
            if (won == null && source.getEntity() == null) won = victorOf(getLastHurtByMob());
            if (won == null) {
                // Beaten by hunters, other mobs or the world: he kneels to no bystander, and retreats with his partner.
                EdelRaids.debug("Arc Aile agent retreated (not beaten by a player)");
                withdraw();
                return;
            }
            victor = won;
            yieldTo(source);
            return;
        }
        // Struck again while yielded: only a player can do that (see isInvulnerableTo), and it is deliberate.
        ServerPlayer killer = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        onKilled(killer);
        super.die(source);
    }

    /** He goes down on one knee. Everything that could still hurt him is cleared, and his partner steps out beside him. */
    /** The player behind a blow: the attacker, or the owner of the companion (Edel Raid, Sting Raid, pet) who struck. */
    @Nullable
    private static UUID victorOf(@Nullable Entity e) {
        if (e instanceof Player p) return p.getUUID();
        if (e instanceof net.minecraft.world.entity.OwnableEntity o) return o.getOwnerUUID();
        return null;
    }

    private void yieldTo(DamageSource source) {
        setHealth(1f);
        removeAllEffects();
        clearFire();
        setTicksFrozen(0);
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        getNavigation().stop();
        entityData.set(DATA_YIELDED, true);
        StingRaidEntity.stopTargetGoals(this, targetSelector);
        withdrawTicks = Config.ARC_AILE_YIELD_TICKS.get();
        EdelRaids.debug("Arc Aile agent yielded");

        EdelRaidData d = partner();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (d != null && level() instanceof ServerLevel sl) {
            EdelRaidEntity p = ModEntities.EDEL_RAID.get().create(sl);
            if (p != null) {
                p.moveTo(getX() + 0.8, getY(), getZ() + 0.4, getYRot(), 0f);
                p.setIdentity(d.element(), d.form(), d.quality(), d.name());
                p.setLoyal(withdrawTicks + 100); // never outlives him, even if he is unloaded
                sl.addFreshEntity(p);
                partnerEntity = p.getUUID();
            }
        }
        for (ServerPlayer near : sl().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24.0))) {
            near.displayClientMessage(Component.translatable("message.edelraids.agent_yields"), true);
        }
    }

    private ServerLevel sl() {
        return (ServerLevel) level();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isYielded() || withdrawTicks <= 40) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (!sp.getUUID().equals(victor)) {
                sp.displayClientMessage(Component.translatable("message.edelraids.agent_not_yours"), true);
                return InteractionResult.CONSUME;
            }
            spare(sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** He and his partner withdraw together; the player's standing with Arc Aile rises. */
    private void spare(ServerPlayer sp) {
        ArcAileStanding st = ArcAileStanding.get(sp.getServer());
        boolean hadTruce = st.truce(sp.getUUID());
        st.add(sp.getUUID(), Config.ARC_AILE_SPARE.get());
        boolean truce = st.truce(sp.getUUID());
        sp.displayClientMessage(Component.translatable("message.edelraids.agent_spared"), false);
        if (truce && !hadTruce) {
            sp.displayClientMessage(Component.translatable("message.edelraids.arc_aile_truce"), false);
            if (st.claimWrit(sp.getUUID())) {
                ItemStack writ = new ItemStack(ModItems.WRIT_OF_PASSAGE.get());
                int free = sp.getInventory().getFreeSlot();
                if (free >= 0) sp.getInventory().setItem(free, writ);
                else sp.drop(writ, false);
                sp.displayClientMessage(Component.translatable("message.edelraids.arc_aile_writ"), false);
            }
        }
        withdrawTicks = 40;
        releasePartner(true, null);
    }

    /** The killer loses a great deal of standing, and his partner, who chose this life, will not forgive it. */
    private void onKilled(@Nullable ServerPlayer killer) {
        if (killer != null) {
            ArcAileStanding st = ArcAileStanding.get(killer.getServer());
            boolean hadTruce = st.truce(killer.getUUID());
            st.add(killer.getUUID(), -Config.ARC_AILE_KILL.get());
            if (hadTruce && !st.truce(killer.getUUID())) {
                killer.displayClientMessage(Component.translatable("message.edelraids.arc_aile_truce_broken"), false);
            }
            spawnAtLocation(new ItemStack(ModItems.AGENT_SIGIL.get()));
        }
        releasePartner(false, killer);
    }

    /** spared: she leaves with him; otherwise she grieves (telling his killer so) and returns to Edel Garden. */
    private void releasePartner(boolean spared, @Nullable ServerPlayer killer) {
        if (partnerEntity == null || !(level() instanceof ServerLevel s)) return;
        if (s.getEntity(partnerEntity) instanceof EdelRaidEntity p) {
            if (spared) {
                p.setLoyal(40);
            } else {
                p.setLoyal(80);
                if (killer != null) killer.displayClientMessage(Component.translatable("message.edelraids.partner_grieves", p.getEdelName()), false);
            }
        }
        partnerEntity = null;
    }

    /** Spared or not, a yielded agent eventually withdraws. Undecided withdrawals change nobody's standing. */
    private void withdraw() {
        releasePartner(true, null);
        if (level() instanceof ServerLevel s) s.sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(), 16, 0.3, 0.6, 0.3, 0.02);
        discard();
    }

    // ---------------------------------------------------------------- plumbing

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isYielded();
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Yielded", isYielded());
        tag.putInt("WithdrawTicks", withdrawTicks);
        if (victor != null) tag.putUUID("Victor", victor);
        if (partnerEntity != null) tag.putUUID("PartnerEntity", partnerEntity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_YIELDED, tag.getBoolean("Yielded"));
        withdrawTicks = tag.getInt("WithdrawTicks");
        victor = tag.hasUUID("Victor") ? tag.getUUID("Victor") : null;
        partnerEntity = tag.hasUUID("PartnerEntity") ? tag.getUUID("PartnerEntity") : null;
    }
}
