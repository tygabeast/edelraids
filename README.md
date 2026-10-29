# Edel Raids: Pledged Blades

A Minecraft 1.21.1 NeoForge mod by Tygabeast. Edel Raids are companions who React with their bonded Pledger
to become that player's weapon. A fan work inspired by the anime and manga *Elemental Gelade* by Mayumi Azuma;
not affiliated with or endorsed by its rights holders.

**Requires:** NeoForge 21.1.200 or newer, GeckoLib 4.7 or newer (1.21.1 build).
**Optional:** Better Combat 2.1 or newer: each weapon form gets its own combo set.

## How to play

| Action | How |
| --- | --- |
| Find an Edel Raid | Hunter camps hold captive Edel Raids (see below). Creative: Edel Raid Spawn Egg |
| Bond (become her Pledger) | Right-click an unbonded Edel Raid with an empty hand |
| Stay / follow | Right-click her with an empty hand |
| React | Sneak + right-click her with an empty hand. Takes about 1 second; stay within 16 blocks |
| Reform | Sneak + use while holding her weapon |
| Release her | `/edelraids release` (shows a warning), then `/edelraids release confirm` while looking at her or holding her |
| Info | `/edelraids info` while looking at her or holding her |
| Bow form | Hold use to draw and release to fire; needs no arrows. Sneak + use still reforms her |
| Paired forms | Twin blades and gauntlets: while she is in your main hand, her second blade or gauntlet appears in an empty off hand. With Better Combat you dual-wield them |
| Sing | **R** (rebindable) while holding her: sings the selected song if you have the resonance |
| Next song | **Z** (rebindable): cycles through the songs she knows |
| Set bond (operators) | `/edelraids bond <value>` while looking at her or holding her |
| Create one (operators) | `/edelraids create <element> [form] [quality 1-3]`, any element including ring 4 |

Rules:

- An Edel Raid has one Pledger at a time and will not React with anyone else. A player can bond several.
- Her weapon's durability is her health (10 wear = 1 health). If it breaks she is forced out, wounded, and recovers over time.
- No permadeath: lethal damage leaves her wounded. Only `/kill` removes her for good; falling into the void sends her home.
- She reforms if her weapon is dropped, if her Pledger dies, or if her weapon is put in a container and the container is closed.
- Bond rises by 1 per hostile mob her Pledger kills with her weapon, and by 1 for every 5 minutes she spends within
  16 blocks of her Pledger as a companion. At 100 the weapon awakens: extra details and +1 damage.

## Hunter camps

Hunters capture Edel Raids to sell. Their camps (tents, a campfire, a loot chest and iron-barred cages) appear in
plains, meadows, forests, savannas, taigas, snowy plains and badlands, about as rarely as pillager outposts and never
near villages. A camp holds 1 to 3 captive Edel Raids (and always one empty cage), guarded by 2 hunters, up to 2 crossbow marksmen and 1 or 2
hunter's Sting Raids. `/locate structure edelraids:hunter_camp` finds the nearest one.

- A captive cannot be freed while any hunter or unbeaten hunter's Sting Raid is within 16 blocks. Open her cage's
  gate and right-click her once they are dealt with.
- A freed captive is an ordinary unbonded Edel Raid: right-click her again to become her Pledger. If nobody bonds her
  within 5 minutes, she thanks you and goes her own way.
- Hunters drop emeralds (marksmen also arrows) and sometimes a Stone Stabilizer; the camp chest has supplies.
- Camps are datapack worldgen (`data/edelraids/worldgen/...`, the biome tag `edelraids:has_structure/hunter_camp`
  and the templates in `data/edelraids/structure/hunter_camp/`), so packs can move, retune or remove them.

## Arc Aile

Arc Aile means to protect Edel Raids, by sealing them away in Edel Garden, far from the world and from time. Its agents
patrol in pairs of a man and his Edel Raid partner, who fights Reacted in his hand. They leave ordinary people alone,
but they confront Pledgers. They also fight hunters, and if they reach a camp's captives first, they take them to Edel
Garden.

- Patrols of 1 or 2 agents sometimes appear 32 to 48 blocks from a player in the Overworld (about once every 20
  minutes of play, never two near one player) and wander off when nobody is around.
- An agent beaten by you or one of your companions yields to you instead of dying (beaten by anything other than
  hunters, he and his partner simply retreat, and nobody's standing changes). If hunters or their Sting Raids beat
  him, he dies and the hunters lock his partner in their camp's empty cage, where she can be freed like any captive;
  with no camp nearby, she returns to Edel Garden. He is cleared of every effect, fire and freezing; nothing but your own hit
  can harm him, and your companions stop fighting him. Right-click him to spare him (only the player he yielded to can):
  he and his partner withdraw, and your standing with Arc Aile rises. Strike him again to finish him: your standing falls sharply, he leaves an
  Agent's Sigil, and his partner, who chose this life, grieves and returns to Edel Garden. She never bonds with you.
  If you decide nothing, he withdraws after a minute.
- Freeing a hunter camp's captives also raises your standing a little. At the truce threshold (about five spares)
  Arc Aile's agents stop confronting you, and the agent you spared hands you a Writ of Passage. Killing an agent
  breaks the truce.
- The Writ of Passage and the Agent's Sigil are the two ways into Edel Garden (see below).
- `/edelraids standing` shows your standing; operators can set it with `/edelraids standing set <value>` and summon a
  patrol with `/edelraids patrol`.

## Edel Garden and Arc Aile's fortresses

Arc Aile guards the way into Edel Garden from fortresses: rare walled strongholds with corner towers, a gatehouse,
barracks and, at the centre, the Garden Gate on a pale shrine, all held by posted defenders (agents who never leave
their post). `/locate structure edelraids:arc_aile_fortress` finds the nearest one.

- Defenders are Arc Aile agents: they confront Pledgers unless you have a truce, yield instead of dying, and can be
  spared or finished like any agent.
- Use a **Writ of Passage** on the Garden Gate to enter peacefully; it is never spent. An **Agent's Sigil** forces
  the gate and breaks. With neither, the gate stays sealed. Companions following you (not sitting, resting or
  wounded) cross with you.
- Edel Garden is its own dimension: a vast, quiet land of meadows and cherry groves dotted with ponds, with no seas,
  caves, ores, rain or monsters. Time does not pass there: it is always day.
- You arrive at the arrival court; its Return Gate takes you back to where you entered.
- Sanctuaries, the Edel Raids who live in them, and the Glittering Jewels come in 0.8.0.
- Operators can cross without the gate with `/edelraids garden` and `/edelraids garden return`.

## Sting Raids

Sting Raids are girls who have been implanted with an artificial stone. They look like Edel Raids, except that the
stone is cut a little wrong, has a hairline crack and flickers instead of glowing steadily. Hunters make them; players
never do.

| Action | How |
| --- | --- |
| Meet one | Hunter camps; creative: Sting Raid Spawn Egg (a hunter's Sting Raid) |
| Free one | Beat a hunter's Sting Raid: she is subdued, not killed. Right-click her within 2 minutes to free her, or she escapes |
| Revive her | Use a **Stone Stabilizer** on a subdued Sting Raid. A freed one stays subdued until you do |
| Steady her stone | Use a **Refined Stabilizer** on a freed Sting Raid once: her stone strains half as fast from then on |
| Stay / follow | Right-click her with an empty hand. A sitting Sting Raid drops whatever she was fighting |
| Rest / bring along | Sneak + right-click her with an empty hand. A resting Sting Raid stays within 8 blocks of where you left her, defends herself if attacked, and does not count toward your active limit |
| Let her go | `/edelraids release`, then `/edelraids release confirm`, while looking at her: she leaves to find her own way |
| Info | `/edelraids info` while looking at her: element, arm, strain, stabilizer and how many you look after |
| Create one (operators) | `/edelraids sting <element> [form] [hostile\|freed]` |

- **Self-React:** when she has a target she Reacts by herself: her forearm becomes an arm blade, a heavy arm or a
  launcher arm, picked by her element's weapon forms. She reverts after 10 seconds without a target.
- **Reacted:** much harder hits (or arrows from the launcher), each carrying her element's effect and ring advantage.
- **Strain:** staying Reacted strains her stone (full in 20 seconds). At full strain she is forced out and staggered
  for 3 seconds, and she can React again once strain falls to half. Her gem flickers faster as strain builds.
- **Freed Sting Raids** follow, stay and fight like an Edel Raid, but cannot bond, sing, awaken or React with you.
  Up to 2 can be active (following you) at a time. One you free while at the limit waits nearby for 5 minutes: let
  one of yours rest (sneak + right-click) and right-click her to take her along, or she leaves. Subduing a hunter's Sting Raid counts as a kill for your Edel Raid's bond.
- Companions of the same player (Edel Raids, Sting Raids, wolves and other pets) never target each other.
- **Stone Stabilizer** (makes 2): amethyst shard above and below, iron ingot either side, redstone in the middle.
  **Refined Stabilizer:** a diamond surrounded by four Stone Stabilizers, with amethyst shards in the corners.

## Resonance and songs

- Each hit with her weapon form (or an arrow from her bow) adds 5 resonance, up to 100. After 10 seconds without a hit
  it drains at 2 per second. The meter shows to the right of the hotbar while you hold her; its position,
  orientation, length and label can be changed in `edelraids-client.toml` or in-game under Mods > Edel Raids > Config.
- Each element has three songs: one at bond 25 (costs 25), one at bond 50 (costs 50), and her full song when her
  weapon awakens (costs 100). Songs carry her element's effect and ring advantage, and need no cooldown beyond 1 second.
- Song shapes: **Burst** (everything around you), **Wave** (a cone in front), **Dash** (you lunge forward and strike
  what you pass), **Ward** (absorption; attackers take her element's effect and a little damage back), **Field**
  (a zone that pulses every half second), **Call** (strikes what you look at).
- Songs never hit you, your allies, anything you own, or other players unless PvP is on. Silenced players cannot sing.
- All 72 song names and numbers are data files (`data/<namespace>/edelraids/song/`).
- If her weapon item is ever lost while she is Reacted (removed by a command, or a mod that added data to it was
  uninstalled), she reforms beside her Pledger the next time they log in.
- Elements follow the four glittering rings: +25% damage against the element you beat, −20% against the one that beats you.
  Ring 4 Edel Raids are not rolled by spawn eggs; they will come from Edel Garden.

## Elements

| Ring | Element (beats the next one) | On hit |
| --- | --- | --- |
| 1 | Earth | Shattered: −2 armor per level, stacks to 3 |
| 1 | Wind | Extra knockback |
| 1 | Fire | Sets the target on fire, 3 s |
| 1 | Water | Slowness, 2 s |
| 1 | Greenery | Rooted in place, 2 s |
| 1 | Wisdom | 20% chance of +50% damage |
| 2 | Beauty | 30% chance the target forgets who it was fighting |
| 2 | Sound | Knocks a raised shield down |
| 2 | Shield | You gain 1 absorption per hit, up to 2 hearts |
| 2 | Sword | Ignores 40% of armor |
| 2 | Love | Heals you for 10% of damage dealt |
| 2 | Void | Removes one beneficial effect from the target |
| 3 | Light | +50% against undead; target glows |
| 3 | Darkness | Blindness, 2 s |
| 3 | Movement | Speed for you, 2 s |
| 3 | Quiet | Silenced: the target cannot fire projectiles, 3 s |
| 3 | Life | Regeneration for you, 3 s |
| 3 | Death | Wither, 3 s |
| 4 | Thunder | Arcs 3 damage to the nearest other hostile within 4 blocks |
| 4 | Steel | +1.5 damage |
| 4 | Ice | Freezing builds up like powder snow |
| 4 | Time | Timeworn: −30% attack speed, −15% movement, 3 s |
| 4 | Dream | Nausea 4 s, slowness 2 s |
| 4 | Star | Critical hits deal 50% more |

## Weapon forms

| Form | Damage | Speed | Reach | Notes | Rings |
| --- | --- | --- | --- | --- | --- |
| Longsword | 7 | 1.6 | | Sweeps | 1, 2, 3 |
| Greatsword | 9 | 1.0 | +0.5 | Sweeps | 1, 4 |
| Spear | 7.5 | 1.2 | +1.5 | | 1, 3, 4 |
| Twin Blades | 6.5 | 1.8 | | Sweeps; a pair of swords | 2, 3 |
| Scythe | 8.5 | 1.1 | +1.0 | Sweeps | 3, 4 |
| Axe | 9 | 0.9 | | | 1, 4 |
| War Hammer | 10 | 0.8 | | | 1, 2, 4 |
| Rapier | 6 | 2.0 | +0.5 | | 2, 3 |
| Gauntlets | 5 | 2.4 | −1.0 | Worn as a pair | 2, 3 |
| Bow | 3 melee | 1.0 | | Fires arrows without ammunition | 1, 2, 4 |

Damage is at Third Class; Second and First Class add +1 and +2, awakening adds +1. With Better Combat installed,
reach comes from its presets instead.

Gameplay settings are in the server config `edelraids-server.toml`; the meter's layout is in the client config
`edelraids-client.toml`. Both can be edited from the in-game config screen (the server file only in single player).

## Data packs

Elements and weapon forms are datapack registries:

- `data/<namespace>/edelraids/element/<name>.json`: `ring` (1-4), `position` (0-5), `color` ("#RRGGBB"),
  optional `forms` (list of form ids), optional `on_hit` (`type`, `strength`), optional `rollable` (default true).
- `data/<namespace>/edelraids/weapon_form/<name>.json`: `attack_damage`, `attack_speed`, optional `sweeps`,
  `reach_bonus`, `ranged`, `paired`, `better_combat_preset` (a Better Combat weapon-attributes id), and
  `sting_arm` (`blade`, `heavy` or `launcher`; the arm a Sting Raid of this form grows).

A form's client model is `assets/<namespace>/geo/item/weapon/<name>.geo.json` with texture
`assets/<namespace>/textures/item/weapon/<name>.png`; forms without a model render as the longsword.

## Building

```
python3 tools/gen.py      # regenerate models, animations, textures, lang and tags
gradle build              # jar in build/libs
```

GeckoLib is a compile-only dependency resolved from its official Maven; it is never bundled.

## 0.7.1 changes

- Fixed a server crash a few seconds after entering Edel Garden: a garden pond placed near a chunk's edge asked for
  a chunk too far away while generating. Ponds now stay inside their own chunk.

## 0.7.0 changes

- Arc Aile fortresses: rare overworld strongholds with posted defenders and the Garden Gate.
- Edel Garden: a new dimension, entered through the Garden Gate with a Writ of Passage (kept) or an Agent's Sigil
  (spent, a forced entry), left through the Return Gate in its arrival court. Following companions travel with you.
- The `garden` operator command.

## 0.6.2 changes

- Every hunter camp has one extra, empty cage.
- An Arc Aile agent beaten by hunters (or their Sting Raids) now dies, and his partner is locked in the nearest empty
  cage within 24 blocks as a captive; with none nearby she returns to Edel Garden.

## 0.6.1 changes

- Fixed: an agent beaten by hunters (or anything else) yielded to whichever player was nearby, who could then spare
  or finish him for standing. An agent now yields only to the player who beat him, directly or through a companion,
  and only that player can spare him; beaten by anything else, he and his partner retreat at once.

## 0.6.0 changes

- Arc Aile: agents with Edel Raid partners, patrols, yielding, sparing or finishing, standing and truce, rival rescues
  at hunter camps, the Writ of Passage and the Agent's Sigil, and the `standing` and `patrol` commands.
- Companions, hunters and songs never target anyone out of the fight (a yielded agent, a captive, a loyal partner).
- An Edel weapon held by a mob carries her element's effect and ring advantage in its hits, and only a player's
  weapon wears.
- Cleanup: the stabilizers and Arc Aile passes share one simple tooltip item class.

## 0.5.0 changes

- Hunter camps: hunters (sword) and hunter marksmen (crossbow) with their Sting Raids, guarding 1 to 3 captive Edel
  Raids in cages. Three camp sizes; natural worldgen in seven biome groups.
- Freed captives you do not bond, and freed Sting Raids you have no active slot for, wait 5 minutes and then leave
  (`companion.freedLingerTicks`).
- Hunters and their Sting Raids are allies; captives cannot be hurt or targeted.
- Edel Raids rolled during world generation register with the anti-duplication registry on their first tick instead
  of from the world-generation thread.

## 0.4.2 changes

- Fixed: a freed Sting Raid could keep fighting another of her owner's companions. Vanilla's targeting goal re-applies
  the target it remembers every tick, so clearing the target was not enough. Edel Raids and Sting Raids now refuse a
  target they must not fight (a companion of the same owner, anyone subdued or wounded, or anyone while sitting) and
  stop the goal that was holding it.

## 0.4.1 changes

- Fixed: two freed Sting Raids could fight each other, because a Sting Raid kept her grudge from before she was
  freed. Companions of the same owner are now allies, freeing clears old targets, and a sitting companion drops
  her target and stops Reacting.
- The Sting Raid limit now counts active ones only. Sneak + right-click lets a freed Sting Raid rest near where you
  left her; freeing one while at the limit leaves her resting.

## 0.4.0 changes

- Sting Raids: hunters' Sting Raids to fight and free, self-React into three arm weapons, strain and stagger,
  Stone and Refined Stabilizers, a limit of 2 freed per player, and the `/edelraids sting` test command.
- Weapon forms have an optional `sting_arm` field (`blade`, `heavy` or `launcher`).
- Edel Raids no longer attack any of their Pledger's own tamed animals, and nobody targets a wounded Edel Raid or a
  subdued Sting Raid.
- Songs no longer hit companions belonging to players you could not hurt (PvP off, or a teammate).
- Cleanup: the anti-friendly-fire check is shared by Edel Raids and Sting Raids.

## 0.3.1 changes

- Wave songs reach further: 6.5 / 7.5 / 9.5 blocks (was 5 / 6 / 8).
- New client config for the resonance meter: position (beside the hotbar, any corner, or the middle of either side),
  horizontal or vertical, bar length, offsets, and toggles for the meter and the song name.
- The mod now has an in-game config screen with named options.

## 0.3.0 changes

- Resonance meter, 72 songs in six shapes, and the Sing (R) and Next Song (Z) keys.
- Bond also grows from time together; Pledgers are told when she learns a song.
- Songs are a new datapack registry (`edelraids:song`).
- Cleanup: Sword's armor piercing and Sound's shield breaking share their logic with songs; no unused imports or
  missing language entries remain.

## 0.2.2 changes

- Gauntlets: the cuff now sits on the forearm with steel over the hand and gold knuckles at the fist; the left
  gauntlet in third person and the right in first person face the right way; the left first-person fist tilts
  toward the centre like the right.

## 0.2.1 changes

- Each weapon form has its own hand, inventory and ground positions. The spear is held like a trident in first
  person; the bow is held like a vanilla bow and has a proper inventory icon; gauntlets are worn on the forearms.
- Twin blades are now a pair of swords, and gauntlets a pair: the second piece appears in an empty off hand while she is
  in your main hand, so Better Combat can dual-wield them. Off-hand hits wear her weapon and carry her element.
  Swapping hands is blocked while holding a pair.
- The axe and scythe heads now face forward.
- Rooted (Greenery) lasts 2 seconds instead of 1.

## 0.2.0 changes

- All 24 elements across the four glittering rings, each with its on-hit effect, and five new effects
  (Shattered, Rooted, Warded, Silenced, Timeworn).
- All 10 weapon forms with their own stats and placeholder models; each ring rolls from its own pool of 6 forms.
- Bow form draws and fires arrows without ammunition.
- Optional Better Combat support: each form uses a Better Combat preset (no code dependency).
- Lost-weapon recovery on login, and `/edelraids create` for operators.
- Element effects now also apply to arrows from the bow form, and only to melee and arrow hits.
- Cleanup: the old single-hook on-hit system was replaced; unused imports checked (none left).

## 0.1.1 changes

- Fixed: closing a container with her weapon inside, in creative with a full inventory, deleted her. She now reforms beside you.
- Fixed: the sitting pose bent her legs backward; she now sits with legs forward.
- Wounded Edel Raids now have their own slouched idle and limping walk instead of the sitting pose.

## 0.1.0 changes

- First build: Phase 1 core slice. Edel Raid companion, bonding, stay/follow, release, React and reform,
  single data-driven weapon item, anti-duplication registry, wounded state, bond and awakening, Wind element,
  longsword form, element and weapon-form datapack registries, server config, commands.
- Models and textures are placeholders.
