package com.tygabeast.edelraids.song;

import com.tygabeast.edelraids.Config;
import com.tygabeast.edelraids.data.EdelRaidData;
import com.tygabeast.edelraids.data.Elemental;
import com.tygabeast.edelraids.data.Element;
import com.tygabeast.edelraids.data.Elements;
import com.tygabeast.edelraids.data.OnHitEffects;
import com.tygabeast.edelraids.data.Song;
import com.tygabeast.edelraids.data.Songs;
import com.tygabeast.edelraids.item.EdelTwinItem;
import com.tygabeast.edelraids.item.EdelWeaponItem;
import com.tygabeast.edelraids.registry.ModAttachments;
import com.tygabeast.edelraids.registry.ModAttachments.Resonance;
import com.tygabeast.edelraids.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resonance and songs, server side. Songs run once when sung (one entity search per song), except Field songs,
 * which pulse every 10 ticks for their duration, and Wards, which only act when their wearer is hit.
 * Per-player state that does not need saving (last hit, cooldown, active ward) lives in a map cleared on logout.
 */
public final class SongManager {
    private static final int FIELD_PULSE = 10;

    private record PlayerState(long lastHit, long cooldownUntil, @Nullable ResourceLocation wardElement, long wardUntil, float wardPower) {
        static final PlayerState EMPTY = new PlayerState(0, 0, null, 0, 0);
    }

    private record Field(ServerLevel level, Vec3 centre, float radius, float power, ResourceLocation element, UUID owner, long until) {}

    private static final Map<UUID, PlayerState> STATE = new HashMap<>();
    private static final List<Field> FIELDS = new ArrayList<>();

    private SongManager() {}

    private static PlayerState state(Player p) {
        return STATE.getOrDefault(p.getUUID(), PlayerState.EMPTY);
    }

    /** Her data from the weapon in the player's hand: her real weapon, or (mid Better Combat off-hand swing) her twin. */
    @Nullable
    public static EdelRaidData held(Player p) {
        ItemStack main = p.getMainHandItem();
        if (main.getItem() instanceof EdelWeaponItem || main.getItem() instanceof EdelTwinItem) return EdelWeaponItem.data(main);
        return null;
    }

    // ------------------------------------------------------------------ resonance

    public static void onWeaponHit(ServerPlayer p) {
        int gain = Config.RESONANCE_PER_HIT.get();
        PlayerState s = state(p);
        STATE.put(p.getUUID(), new PlayerState(p.level().getGameTime(), s.cooldownUntil(), s.wardElement(), s.wardUntil(), s.wardPower()));
        if (gain <= 0) return;
        Resonance r = p.getData(ModAttachments.RESONANCE);
        if (r.value() < 100) p.setData(ModAttachments.RESONANCE, r.withValue(Math.min(100, r.value() + gain)));
    }

    /** Per player per tick: returns at once unless there is resonance to drain, and only acts every 10 ticks. */
    public static void tickPlayer(ServerPlayer p) {
        long time = p.level().getGameTime();
        if (time % 10 != 0 || !p.hasData(ModAttachments.RESONANCE)) return;
        Resonance r = p.getData(ModAttachments.RESONANCE);
        int perSecond = Config.RESONANCE_DRAIN_PER_SECOND.get();
        if (r.value() <= 0 || perSecond <= 0) return;
        if (time - state(p).lastHit() < Config.RESONANCE_DRAIN_DELAY.get()) return;
        int step = Math.max(1, perSecond / 2);
        p.setData(ModAttachments.RESONANCE, r.withValue(Math.max(0, r.value() - step)));
    }

    public static void forget(UUID player) {
        STATE.remove(player);
    }

    // ------------------------------------------------------------------ selection

    public static void cycle(ServerPlayer p) {
        EdelRaidData d = held(p);
        if (d == null) return;
        Songs.Entry[] songs = Songs.forElement(p.level().registryAccess(), d.element());
        Resonance r = p.getData(ModAttachments.RESONANCE);
        int current = Songs.effectiveSlot(d, songs, r.selected());
        if (current == 0) {
            p.displayClientMessage(Component.translatable("message.edelraids.no_songs", d.name(), Config.SONG_BOND_1.get()), true);
            return;
        }
        for (int i = 1; i <= 3; i++) {
            int slot = (current - 1 + i) % 3 + 1;
            if (songs[slot - 1] != null && Songs.unlocked(d, slot)) {
                p.setData(ModAttachments.RESONANCE, r.withSelected(slot));
                p.displayClientMessage(songs[slot - 1].name(), true);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ singing

    public static void sing(ServerPlayer p) {
        if (!Config.SONGS_ENABLED.get() || !p.isAlive() || p.isSpectator()) return;
        EdelRaidData d = held(p);
        if (d == null) return;
        if (p.hasEffect(ModEffects.SILENCED)) {
            p.displayClientMessage(Component.translatable("message.edelraids.silenced"), true);
            return;
        }
        long time = p.level().getGameTime();
        PlayerState st = state(p);
        if (time < st.cooldownUntil()) return;

        Songs.Entry[] songs = Songs.forElement(p.level().registryAccess(), d.element());
        Resonance r = p.getData(ModAttachments.RESONANCE);
        int slot = Songs.effectiveSlot(d, songs, r.selected());
        Songs.Entry entry = Songs.get(songs, slot);
        if (entry == null) {
            p.displayClientMessage(Component.translatable("message.edelraids.no_songs", d.name(), Config.SONG_BOND_1.get()), true);
            return;
        }
        Song song = entry.song();
        if (r.value() < song.cost() && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("message.edelraids.need_resonance", song.cost()), true);
            return;
        }
        Element el = Elements.element(p.level().registryAccess(), d.element());
        if (el == null) return;

        if (!p.getAbilities().instabuild) p.setData(ModAttachments.RESONANCE, r.withValue(r.value() - song.cost()).withSelected(slot));
        STATE.put(p.getUUID(), new PlayerState(st.lastHit(), time + Config.SONG_COOLDOWN.get(), st.wardElement(), st.wardUntil(), st.wardPower()));

        float power = (float) (song.power() * Config.SONG_DAMAGE_SCALE.get()) + (d.quality() - 1) * 0.5f + (d.awakened() ? 1f : 0f);
        ServerLevel level = p.serverLevel();
        SongFx fx = new SongFx(level, el.color());
        switch (song.shape()) {
            case BURST -> burst(p, el, power, song.size(), fx);
            case WAVE -> wave(p, el, power, song.size(), fx);
            case DASH -> dash(p, el, power, song.size(), fx);
            case WARD -> ward(p, d.element(), power, song.duration(), fx);
            case FIELD -> {
                FIELDS.add(new Field(level, p.position(), song.size(), power, d.element(), p.getUUID(), time + song.duration()));
                fx.ring(p.position(), song.size(), 24);
            }
            case CALL -> call(p, el, power, song.size(), fx);
        }
        level.playSound(null, p, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, slot == 3 ? 0.7f : 1.1f);
        level.playSound(null, p, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 1.3f);
        p.displayClientMessage(entry.name(), true);
    }

    /** Song targets: living, attackable, not you, not your allies or anything you own, and other players only where PvP allows. */
    private static boolean isTarget(ServerPlayer p, LivingEntity t) {
        if (t == p || !t.isAlive() || !t.isAttackable() || t.isSpectator() || t.isAlliedTo(p)) return false;
        if (t instanceof OwnableEntity o && o.getOwnerUUID() != null) {
            // Your own companions are never hit; someone else's only where you could hurt that player.
            if (p.getUUID().equals(o.getOwnerUUID())) return false;
            if (o.getOwner() instanceof Player owner ? !p.canHarmPlayer(owner) : !p.server.isPvpAllowed()) return false;
        }
        return !(t instanceof Player other) || p.canHarmPlayer(other);
    }

    private static void strike(ServerPlayer p, LivingEntity t, Element el, float power) {
        float amount = power;
        if (t instanceof Elemental def) amount *= Elements.multiplier(el, Elements.element(p.level().registryAccess(), def.getElementId()));
        amount = OnHitEffects.songDamage(el.onHit(), t, p, amount);
        // Magic damage on purpose: it never counts as a weapon hit, so it cannot trigger effects or resonance again.
        if (t.hurt(p.damageSources().indirectMagic(p, p), amount)) OnHitEffects.afterHit(el.onHit(), t, p, amount);
    }

    private static void burst(ServerPlayer p, Element el, float power, float radius, SongFx fx) {
        double r2 = radius * radius;
        for (LivingEntity t : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), t -> isTarget(p, t))) {
            if (t.distanceToSqr(p) <= r2) strike(p, t, el, power);
        }
        fx.ring(p.position(), radius, 32);
    }

    private static void wave(ServerPlayer p, Element el, float power, float length, SongFx fx) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double cos = Math.cos(Math.toRadians(50));
        for (LivingEntity t : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(length), t -> isTarget(p, t))) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist <= length && (dist < 1.0 || to.scale(1.0 / dist).dot(look) >= cos)) strike(p, t, el, power);
        }
        fx.cone(eye, look, length);
    }

    private static void dash(ServerPlayer p, Element el, float power, float length, SongFx fx) {
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, Math.max(-0.2, Math.min(0.4, look.y)), look.z).normalize();
        Vec3 start = p.position().add(0, 0.9, 0);
        Vec3 end = start.add(dir.scale(length));
        var hit = p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        end = hit.getLocation();
        AABB sweep = new AABB(start, end).inflate(1.5);
        for (LivingEntity t : p.level().getEntitiesOfClass(LivingEntity.class, sweep, t -> isTarget(p, t))) {
            if (t.getBoundingBox().inflate(0.9).clip(start, end).isPresent()) strike(p, t, el, power);
        }
        // Movement is client-authoritative: give the player the push and let the client carry it.
        double speed = Math.min(3.0, start.distanceTo(end) * 0.32);
        p.setDeltaMovement(dir.scale(speed));
        p.hurtMarked = true;
        p.resetFallDistance();
        fx.line(start, end);
    }

    private static void ward(ServerPlayer p, ResourceLocation id, float power, int duration, SongFx fx) {
        int amplifier = Math.max(0, Math.round(power / 4f) - 1);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, false, true, true));
        PlayerState s = state(p);
        STATE.put(p.getUUID(), new PlayerState(s.lastHit(), s.cooldownUntil(), id, p.level().getGameTime() + duration, power));
        fx.spiral(p.position(), 1.0, 2.2);
    }

    /** While a Ward lasts, whatever hits its wearer takes her element's effect and a quarter of the song's power. */
    public static void onWardedHit(ServerPlayer p, LivingEntity attacker) {
        PlayerState s = STATE.get(p.getUUID());
        if (s == null || s.wardElement() == null || p.level().getGameTime() > s.wardUntil() || attacker == p) return;
        Element el = Elements.element(p.level().registryAccess(), s.wardElement());
        if (el == null) return;
        OnHitEffects.afterHit(el.onHit(), attacker, p, 0f);
        attacker.hurt(p.damageSources().thorns(p), s.wardPower() * 0.25f);
    }

    private static void call(ServerPlayer p, Element el, float power, float range, SongFx fx) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        var blockHit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        end = blockHit.getLocation();
        AABB box = p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box,
                e -> e instanceof LivingEntity l && isTarget(p, l), 0.5f);
        if (hit != null && hit.getEntity() instanceof LivingEntity t) {
            strike(p, t, el, power);
            end = t.getBoundingBox().getCenter();
        }
        fx.line(eye.add(0, -0.3, 0), end);
    }

    // ------------------------------------------------------------------ fields

    /** Once per server tick: leaves at once when no field exists, and pulses fields only every 10 ticks. */
    public static void tickFields(long serverTicks) {
        if (FIELDS.isEmpty() || serverTicks % FIELD_PULSE != 0) return;
        Iterator<Field> it = FIELDS.iterator();
        while (it.hasNext()) {
            Field f = it.next();
            ServerPlayer owner = f.level().getServer().getPlayerList().getPlayer(f.owner());
            if (f.level().getGameTime() > f.until() || owner == null || owner.level() != f.level()) {
                it.remove();
                continue;
            }
            Element el = Elements.element(f.level().registryAccess(), f.element());
            if (el == null) {
                it.remove();
                continue;
            }
            double r2 = f.radius() * f.radius();
            AABB box = new AABB(f.centre(), f.centre()).inflate(f.radius(), 2.0, f.radius());
            for (LivingEntity t : f.level().getEntitiesOfClass(LivingEntity.class, box, t -> isTarget(owner, t))) {
                if (t.position().distanceToSqr(f.centre()) <= r2) strike(owner, t, el, f.power());
            }
            new SongFx(f.level(), el.color()).ring(f.centre(), f.radius(), 16);
        }
    }

    public static void clearFields() {
        FIELDS.clear();
        STATE.clear();
    }
}
