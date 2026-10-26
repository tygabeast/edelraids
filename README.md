# Edel Raids: Pledged Blades

A Minecraft 1.21.1 NeoForge mod by Tygabeast. Edel Raids are companions who React with their bonded Pledger
to become that player's weapon. A fan work inspired by the anime and manga *Elemental Gelade* by Mayumi Azuma;
not affiliated with or endorsed by its rights holders.

**Requires:** NeoForge 21.1.200 or newer, GeckoLib 4.7 or newer (1.21.1 build).

## How to play (phase 1)

| Action | How |
| --- | --- |
| Get an Edel Raid | Creative tab "Edel Raids: Pledged Blades" → Edel Raid Spawn Egg (natural sources come in phase 3) |
| Bond (become her Pledger) | Right-click an unbonded Edel Raid with an empty hand |
| Stay / follow | Right-click her with an empty hand |
| React | Sneak + right-click her with an empty hand. Takes about 1 second; stay within 16 blocks |
| Reform | Sneak + use while holding her weapon |
| Release her | `/edelraids release` (shows a warning), then `/edelraids release confirm` while looking at her or holding her |
| Info | `/edelraids info` while looking at her or holding her |
| Set bond (operators) | `/edelraids bond <value>` while looking at her or holding her |

Rules:

- An Edel Raid has one Pledger at a time and will not React with anyone else. A player can bond several.
- Her weapon's durability is her health (10 wear = 1 health). If it breaks she is forced out, wounded, and recovers over time.
- No permadeath: lethal damage leaves her wounded. Only `/kill` removes her for good; falling into the void sends her home.
- She reforms if her weapon is dropped, if her Pledger dies, or if her weapon is put in a container and the container is closed.
- Bond rises by 1 per hostile mob her Pledger kills with her weapon (placeholder). At 100 the weapon awakens: extra fins and +1 damage.
- Elements follow the four glittering rings: +25% damage against the element you beat, −20% against the one that beats you.
  Phase 1 ships only Wind (on-hit: extra knockback) and the longsword.

Everything above is configurable in the server config `edelraids-server.toml`.

## Data packs

Elements and weapon forms are datapack registries:

- `data/<namespace>/edelraids/element/<name>.json`: `ring` (1-4), `position` (0-5), `color` ("#RRGGBB"),
  optional `forms` (list of form ids), optional `on_hit` (`type`, `strength`).
- `data/<namespace>/edelraids/weapon_form/<name>.json`: `attack_damage`, `attack_speed`, optional `sweeps`,
  optional `better_combat_preset` (used from phase 2).

A form's client model is `assets/<namespace>/geo/item/weapon/<name>.geo.json` with texture
`assets/<namespace>/textures/item/weapon/<name>.png`; forms without a model render as the longsword.

## Building

```
python3 tools/gen.py      # regenerate models, animations, textures, lang and tags
gradle build              # jar in build/libs
```

GeckoLib is a compile-only dependency resolved from its official Maven; it is never bundled.

## 0.1.0 changes

- First build: Phase 1 core slice. Edel Raid companion, bonding, stay/follow, release, React and reform,
  single data-driven weapon item, anti-duplication registry, wounded state, bond and awakening, Wind element,
  longsword form, element and weapon-form datapack registries, server config, commands.
- Models and textures are placeholders.
