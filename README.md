# Edel Raids: Pledged Blades

A Minecraft 1.21.1 NeoForge mod by Tygabeast. Edel Raids are companions who React with their bonded Pledger
to become that player's weapon. A fan work inspired by the anime and manga *Elemental Gelade* by Mayumi Azuma;
not affiliated with or endorsed by its rights holders.

**Requires:** NeoForge 21.1.200 or newer, GeckoLib 4.7 or newer (1.21.1 build).
**Optional:** Better Combat 2.1 or newer: each weapon form gets its own combo set.

## How to play

| Action | How |
| --- | --- |
| Get an Edel Raid | Creative tab "Edel Raids: Pledged Blades" → Edel Raid Spawn Egg (natural sources come in phase 3) |
| Bond (become her Pledger) | Right-click an unbonded Edel Raid with an empty hand |
| Stay / follow | Right-click her with an empty hand |
| React | Sneak + right-click her with an empty hand. Takes about 1 second; stay within 16 blocks |
| Reform | Sneak + use while holding her weapon |
| Release her | `/edelraids release` (shows a warning), then `/edelraids release confirm` while looking at her or holding her |
| Info | `/edelraids info` while looking at her or holding her |
| Bow form | Hold use to draw and release to fire; needs no arrows. Sneak + use still reforms her |
| Set bond (operators) | `/edelraids bond <value>` while looking at her or holding her |
| Create one (operators) | `/edelraids create <element> [form] [quality 1-3]`, any element including ring 4 |

Rules:

- An Edel Raid has one Pledger at a time and will not React with anyone else. A player can bond several.
- Her weapon's durability is her health (10 wear = 1 health). If it breaks she is forced out, wounded, and recovers over time.
- No permadeath: lethal damage leaves her wounded. Only `/kill` removes her for good; falling into the void sends her home.
- She reforms if her weapon is dropped, if her Pledger dies, or if her weapon is put in a container and the container is closed.
- Bond rises by 1 per hostile mob her Pledger kills with her weapon (placeholder). At 100 the weapon awakens: extra fins and +1 damage.
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
| 1 | Greenery | Rooted in place, 1 s |
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
| Twin Blades | 6.5 | 1.8 | | Sweeps | 2, 3 |
| Scythe | 8.5 | 1.1 | +1.0 | Sweeps | 3, 4 |
| Axe | 9 | 0.9 | | | 1, 4 |
| War Hammer | 10 | 0.8 | | | 1, 2, 4 |
| Rapier | 6 | 2.0 | +0.5 | | 2, 3 |
| Gauntlets | 5 | 2.4 | −1.0 | | 2, 3 |
| Bow | 3 melee | 1.0 | | Fires arrows without ammunition | 1, 2, 4 |

Damage is at Third Class; Second and First Class add +1 and +2, awakening adds +1. With Better Combat installed,
reach comes from its presets instead.

Everything above is configurable in the server config `edelraids-server.toml`.

## Data packs

Elements and weapon forms are datapack registries:

- `data/<namespace>/edelraids/element/<name>.json`: `ring` (1-4), `position` (0-5), `color` ("#RRGGBB"),
  optional `forms` (list of form ids), optional `on_hit` (`type`, `strength`), optional `rollable` (default true).
- `data/<namespace>/edelraids/weapon_form/<name>.json`: `attack_damage`, `attack_speed`, optional `sweeps`,
  `reach_bonus`, `ranged`, and `better_combat_preset` (a Better Combat weapon-attributes id).

A form's client model is `assets/<namespace>/geo/item/weapon/<name>.geo.json` with texture
`assets/<namespace>/textures/item/weapon/<name>.png`; forms without a model render as the longsword.

## Building

```
python3 tools/gen.py      # regenerate models, animations, textures, lang and tags
gradle build              # jar in build/libs
```

GeckoLib is a compile-only dependency resolved from its official Maven; it is never bundled.

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
