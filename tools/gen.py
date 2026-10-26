#!/usr/bin/env python3
"""Generates Edel Raids: Pledged Blades resources (placeholder models, animations, textures, lang, item models, tags).

Run from the project root before building whenever any of these change:  python3 tools/gen.py
Everything here is a phase 1 placeholder; final art replaces the textures and models later.
Stdlib only. Output goes to src/main/resources and is deterministic.
"""
import json
import math
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
MODID = 'edelraids'
ASSETS = os.path.join(ROOT, 'assets', MODID)
DATA = os.path.join(ROOT, 'data')


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


# ------------------------------------------------------------------ PNG

def write_png(path, w, h, px):
    """px: list of rows of (r, g, b, a)."""
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *p) for p in row) for row in px)

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(png)


def canvas(w, h):
    return [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]


def rect(px, x, y, w, h, col):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px[yy][xx] = col


def shade(col, f):
    return (max(0, min(255, int(col[0] * f))), max(0, min(255, int(col[1] * f))), max(0, min(255, int(col[2] * f))), col[3])


def box_uv(px, u, v, sx, sy, sz, cols):
    """Paint a Bedrock box-UV layout. cols: dict face -> color for top,bottom,right,front,left,back."""
    rect(px, u + sz, v, sx, sz, cols['top'])
    rect(px, u + sz + sx, v, sx, sz, cols['bottom'])
    rect(px, u, v + sz, sz, sy, cols['right'])
    rect(px, u + sz, v + sz, sx, sy, cols['front'])
    rect(px, u + sz + sx, v + sz, sz, sy, cols['left'])
    rect(px, u + 2 * sz + sx, v + sz, sx, sy, cols['back'])


def all_faces(col, top=1.08, bottom=0.8, side=0.92, back=0.88):
    return {'top': shade(col, top), 'bottom': shade(col, bottom), 'right': shade(col, side),
            'front': col, 'left': shade(col, side), 'back': shade(col, back)}


# ------------------------------------------------------------------ Edel Raid entity (placeholder humanoid)

SKIN = (236, 200, 172, 255)
HAIR = (92, 64, 52, 255)
DRESS = (242, 234, 214, 255)
TRIM = (70, 150, 128, 255)
BOOT = (88, 70, 62, 255)
EYE = (60, 110, 140, 255)
GEM = (245, 245, 245, 255)  # white so the element tint shows true


def entity_texture():
    px = canvas(64, 64)
    # head 8x8x8 at uv 0,0: hair on top/back/sides, face on front
    box_uv(px, 0, 0, 8, 8, 8, {'top': HAIR, 'bottom': SKIN, 'right': HAIR, 'front': SKIN, 'left': HAIR, 'back': HAIR})
    rect(px, 8, 8, 8, 2, HAIR)               # fringe
    rect(px, 8, 10, 1, 4, HAIR)
    rect(px, 15, 10, 1, 4, HAIR)
    rect(px, 10, 12, 1, 1, EYE)
    rect(px, 13, 12, 1, 1, EYE)
    rect(px, 11, 14, 2, 1, shade(SKIN, 0.85))
    # torso 8x12x4 at uv 16,16
    box_uv(px, 16, 16, 8, 12, 4, all_faces(DRESS))
    rect(px, 20, 20, 8, 1, TRIM)
    rect(px, 23, 20, 2, 12, TRIM)
    # right arm at 40,16 and left arm at 32,48 (4x12x4): sleeve then skin
    for (u, v) in ((40, 16), (32, 48)):
        box_uv(px, u, v, 4, 12, 4, all_faces(SKIN))
        rect(px, u, v + 4, 16, 4, DRESS)
        rect(px, u, v + 7, 16, 1, TRIM)
    # legs at 0,16 and 16,48: stockings and boots
    for (u, v) in ((0, 16), (16, 48)):
        box_uv(px, u, v, 4, 12, 4, all_faces(shade(DRESS, 0.9)))
        rect(px, u, v + 12, 16, 4, BOOT)
    # skirt 9x4x5 at 0,32
    box_uv(px, 0, 32, 9, 4, 5, all_faces(DRESS))
    rect(px, 0, 40, 28, 1, TRIM)
    # gem 2x2x1 at 56,0
    box_uv(px, 56, 0, 2, 2, 1, all_faces(GEM, 1, 1, 1, 1))
    return px


def cube(origin, size, uv, pivot=None, rotation=None):
    c = {'origin': origin, 'size': size, 'uv': uv}
    if pivot is not None:
        c['pivot'] = pivot
        c['rotation'] = rotation
    return c


def entity_geo():
    bones = [
        {'name': 'root', 'pivot': [0, 0, 0]},
        {'name': 'body', 'parent': 'root', 'pivot': [0, 24, 0], 'cubes': [
            cube([-4, 12, -2], [8, 12, 4], [16, 16]),
            cube([-4.5, 9, -2.5], [9, 4, 5], [0, 32]),
        ]},
        {'name': 'head', 'parent': 'body', 'pivot': [0, 24, 0], 'cubes': [cube([-4, 24, -4], [8, 8, 8], [0, 0])]},
        {'name': 'gem', 'parent': 'head', 'pivot': [0, 30, -4], 'cubes': [cube([-1, 29, -4.5], [2, 2, 1], [56, 0])]},
        {'name': 'right_arm', 'parent': 'body', 'pivot': [-5, 22, 0], 'cubes': [cube([-8, 12, -2], [4, 12, 4], [40, 16])]},
        {'name': 'left_arm', 'parent': 'body', 'pivot': [5, 22, 0], 'cubes': [cube([4, 12, -2], [4, 12, 4], [32, 48])]},
        {'name': 'right_leg', 'parent': 'root', 'pivot': [-1.9, 12, 0], 'cubes': [cube([-3.9, 0, -2], [4, 12, 4], [0, 16])]},
        {'name': 'left_leg', 'parent': 'root', 'pivot': [1.9, 12, 0], 'cubes': [cube([-0.1, 0, -2], [4, 12, 4], [16, 48])]},
    ]
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.edel_raid', 'texture_width': 64, 'texture_height': 64,
                        'visible_bounds_width': 2, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1.5, 0]},
        'bones': bones}]}


def entity_animations():
    return {'format_version': '1.8.0', 'animations': {
        'animation.edel_raid.idle': {'loop': True, 'animation_length': 3.0, 'bones': {
            'right_arm': {'rotation': {'0.0': [0, 0, 3], '1.5': [0, 0, 5], '3.0': [0, 0, 3]}},
            'left_arm': {'rotation': {'0.0': [0, 0, -3], '1.5': [0, 0, -5], '3.0': [0, 0, -3]}},
            'body': {'position': {'0.0': [0, 0, 0], '1.5': [0, 0.25, 0], '3.0': [0, 0, 0]}},
        }},
        'animation.edel_raid.walk': {'loop': True, 'animation_length': 1.0, 'bones': {
            'right_leg': {'rotation': {'0.0': [25, 0, 0], '0.5': [-25, 0, 0], '1.0': [25, 0, 0]}},
            'left_leg': {'rotation': {'0.0': [-25, 0, 0], '0.5': [25, 0, 0], '1.0': [-25, 0, 0]}},
            'right_arm': {'rotation': {'0.0': [-20, 0, 3], '0.5': [20, 0, 3], '1.0': [-20, 0, 3]}},
            'left_arm': {'rotation': {'0.0': [20, 0, -3], '0.5': [-20, 0, -3], '1.0': [20, 0, -3]}},
        }},
        'animation.edel_raid.sit': {'loop': True, 'animation_length': 1.0, 'bones': {
            'root': {'position': [0, -10, 0]},
            'right_leg': {'rotation': [85, 10, 0]},
            'left_leg': {'rotation': [85, -10, 0]},
            'right_arm': {'rotation': [30, 0, 5]},
            'left_arm': {'rotation': [30, 0, -5]},
        }},
        'animation.edel_raid.react': {'loop': 'hold_on_last_frame', 'animation_length': 1.0, 'bones': {
            'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.4': [-160, 0, 10], '1.0': [-170, 0, 10]}},
            'left_arm': {'rotation': {'0.0': [0, 0, 0], '0.4': [-160, 0, -10], '1.0': [-170, 0, -10]}},
            'root': {'scale': {'0.0': [1, 1, 1], '0.6': [0.9, 1.15, 0.9], '1.0': [0, 0, 0]},
                     'position': {'0.0': [0, 0, 0], '1.0': [0, 8, 0]}},
        }},
        'animation.edel_raid.reform': {'loop': False, 'animation_length': 1.0, 'bones': {
            'root': {'scale': {'0.0': [0, 0, 0], '0.6': [0.9, 1.15, 0.9], '1.0': [1, 1, 1]},
                     'position': {'0.0': [0, 8, 0], '1.0': [0, 0, 0]}},
        }},
    }}


# ------------------------------------------------------------------ Longsword weapon form (placeholder)

STEEL = (214, 226, 232, 255)
STEEL_EDGE = (244, 250, 252, 255)
GUARD = (196, 168, 96, 255)
GRIP = (74, 54, 46, 255)
WING = (180, 236, 220, 255)


def longsword_texture():
    px = canvas(32, 32)
    box_uv(px, 0, 0, 1, 5, 1, all_faces(GRIP))                    # grip
    box_uv(px, 4, 0, 2, 1, 2, all_faces(GUARD))                   # pommel
    box_uv(px, 0, 8, 6, 1, 2, all_faces(GUARD))                   # guard
    box_uv(px, 20, 0, 2, 2, 1, all_faces(GEM, 1, 1, 1, 1))         # gem
    box_uv(px, 0, 12, 2, 14, 1, all_faces(STEEL, 1.05, 0.9, 1.0, 0.95))  # blade
    rect(px, 1, 13, 1, 14, STEEL_EDGE)
    rect(px, 4, 13, 1, 14, STEEL_EDGE)
    box_uv(px, 8, 12, 1, 2, 1, all_faces(STEEL_EDGE))              # tip
    box_uv(px, 12, 12, 2, 8, 1, all_faces(WING, 1.05, 0.9, 1.0, 0.95))  # awakened fin, left
    box_uv(px, 12, 22, 2, 8, 1, all_faces(WING, 1.05, 0.9, 1.0, 0.95))  # awakened fin, right
    return px


def longsword_geo():
    bones = [
        {'name': 'root', 'pivot': [0, 0, 0]},
        {'name': 'grip', 'parent': 'root', 'pivot': [0, 0, 0], 'cubes': [
            cube([-0.5, 0, -0.5], [1, 5, 1], [0, 0]),
            cube([-1, -1, -1], [2, 1, 2], [4, 0]),
        ]},
        {'name': 'guard', 'parent': 'root', 'pivot': [0, 5, 0], 'cubes': [cube([-3, 5, -1], [6, 1, 2], [0, 8])]},
        {'name': 'gem', 'parent': 'guard', 'pivot': [0, 5.5, -1], 'cubes': [cube([-1, 4.5, -1.5], [2, 2, 1], [20, 0])]},
        {'name': 'blade', 'parent': 'root', 'pivot': [0, 6, 0], 'cubes': [
            cube([-1, 6, -0.5], [2, 14, 1], [0, 12]),
            cube([-0.5, 20, -0.5], [1, 2, 1], [8, 12]),
        ]},
        {'name': 'awakened', 'parent': 'blade', 'pivot': [0, 8, 0], 'cubes': [
            cube([-3, 7, -0.5], [2, 8, 1], [12, 12]),
            cube([1, 7, -0.5], [2, 8, 1], [12, 22]),
        ]},
    ]
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.edel_weapon_longsword', 'texture_width': 32, 'texture_height': 32,
                        'visible_bounds_width': 2, 'visible_bounds_height': 2.5, 'visible_bounds_offset': [0, 0.75, 0]},
        'bones': bones}]}


def weapon_animations():
    return {'format_version': '1.8.0', 'animations': {
        'animation.edel_weapon.form': {'loop': False, 'animation_length': 0.5, 'bones': {
            'root': {'scale': {'0.0': [0.2, 0.2, 0.2], '0.35': [1.1, 1.1, 1.1], '0.5': [1, 1, 1]}},
        }},
    }}


# ------------------------------------------------------------------ item display transforms
# The weapon model points straight up from its grip at the origin. These transforms are derived from the vanilla
# "handheld" sword transforms so the blade sits in the hand where a vanilla sword would.

def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0], [0, c, -s], [0, s, c]]


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s], [0, 1, 0], [-s, 0, c]]


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def xyz(rx, ry, rz):
    """Matches JOML rotationXYZ, which Minecraft's ItemTransform uses: Rx * Ry * Rz."""
    return mul(mul(rot_x(math.radians(rx)), rot_y(math.radians(ry))), rot_z(math.radians(rz)))


def to_xyz(m):
    """Decompose R = Rx * Ry * Rz back into degrees."""
    b = math.asin(max(-1.0, min(1.0, m[0][2])))
    if abs(m[0][2]) < 0.99999:
        a = math.atan2(-m[1][2], m[2][2])
        c = math.atan2(-m[0][1], m[0][0])
    else:
        a = math.atan2(m[2][1], m[1][1])
        c = 0.0
    return [round(math.degrees(a), 2), round(math.degrees(b), 2), round(math.degrees(c), 2)]


def from_vanilla(rot, trans, scale, handle=(-5.5, -5.5, 0.0), tilt=-45.0):
    """Our model upright with grip at origin == vanilla sprite tilted 45 degrees with its grip at `handle` (pixels)."""
    rv = xyz(*rot)
    r = mul(rv, rot_z(math.radians(tilt)))
    off = apply(rv, [h * scale for h in handle])
    t = [round(trans[i] + off[i], 2) for i in range(3)]
    return {'rotation': to_xyz(r), 'translation': t, 'scale': [scale, scale, scale]}


def weapon_item_model():
    left_handle = (5.5, -5.5, 0.0)
    return {
        'parent': 'builtin/entity',
        'gui_light': 'front',
        'display': {
            'thirdperson_righthand': from_vanilla((0, -90, 55), (0, 4.0, 0.5), 0.85),
            'thirdperson_lefthand': from_vanilla((0, 90, -55), (0, 4.0, 0.5), 0.85, left_handle, 45.0),
            'firstperson_righthand': from_vanilla((0, -90, 25), (1.13, 3.2, 1.13), 0.68),
            'firstperson_lefthand': from_vanilla((0, 90, -25), (1.13, 3.2, 1.13), 0.68, left_handle, 45.0),
            'gui': {'rotation': [0, 0, -45], 'translation': [-4.5, -4.5, 0], 'scale': [0.6, 0.6, 0.6]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 0, -45], 'translation': [-4.5, -4.5, 0], 'scale': [0.6, 0.6, 0.6]},
        },
    }


# ------------------------------------------------------------------ lang

LANG = {
    'itemGroup.edelraids': 'Edel Raids: Pledged Blades',
    'entity.edelraids.edel_raid': 'Edel Raid',
    'item.edelraids.edel_weapon': 'Edel Raid Weapon',
    'item.edelraids.edel_weapon.named': "%s's %s",
    'item.edelraids.edel_raid_spawn_egg': 'Edel Raid Spawn Egg',
    'edelraids.element.edelraids.wind': 'Wind',
    'edelraids.form.edelraids.longsword': 'Longsword',
    'edelraids.quality.1': 'Third Class',
    'edelraids.quality.2': 'Second Class',
    'edelraids.quality.3': 'First Class',
    'tooltip.edelraids.element': 'Element: %s',
    'tooltip.edelraids.quality': 'Quality: %s',
    'tooltip.edelraids.bond': 'Bond: %s',
    'tooltip.edelraids.bond_awakened': 'Bond: %s (Awakened)',
    'tooltip.edelraids.revert': 'Sneak + use to let her reform',
    'message.edelraids.bonded': '%s pledges herself to you. You are her Pledger.',
    'message.edelraids.not_your_pledger': '%s is bonded to another Pledger.',
    'message.edelraids.stay': '%s will wait here.',
    'message.edelraids.follow': '%s will follow you.',
    'message.edelraids.busy': '%s is in the middle of transforming.',
    'message.edelraids.is_wounded': '%s is wounded and needs time to recover.',
    'message.edelraids.wounded': '%s is wounded and will recover in time.',
    'message.edelraids.recovered': '%s has recovered.',
    'message.edelraids.inventory_full': 'You need a free slot before %s can React.',
    'message.edelraids.react_cancelled': '%s could not complete the React.',
    'message.edelraids.awakened': "%s's weapon form has awakened!",
    'message.edelraids.no_storage': 'An Edel Raid cannot be stored in a container.',
    'command.edelraids.no_target': 'Look at your Edel Raid or hold her weapon form.',
    'command.edelraids.release_prompt': 'This frees %s and resets your bond to 0. Run /edelraids release confirm to continue.',
    'command.edelraids.released': '%s is no longer bonded to you.',
    'command.edelraids.info': '%s: %s %s, %s, bond %s/%s, %s',
    'command.edelraids.bond_set': 'Bond set to %s.',
}


def main():
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'edel_raid.png'), 64, 64, entity_texture())
    write_json(os.path.join(ASSETS, 'geo', 'entity', 'edel_raid.geo.json'), entity_geo())
    write_json(os.path.join(ASSETS, 'animations', 'entity', 'edel_raid.animation.json'), entity_animations())

    write_png(os.path.join(ASSETS, 'textures', 'item', 'weapon', 'longsword.png'), 32, 32, longsword_texture())
    write_json(os.path.join(ASSETS, 'geo', 'item', 'weapon', 'longsword.geo.json'), longsword_geo())
    write_json(os.path.join(ASSETS, 'animations', 'item', 'edel_weapon.animation.json'), weapon_animations())

    write_json(os.path.join(ASSETS, 'models', 'item', 'edel_weapon.json'), weapon_item_model())
    write_json(os.path.join(ASSETS, 'models', 'item', 'edel_raid_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
    write_json(os.path.join(ASSETS, 'lang', 'en_us.json'), dict(sorted(LANG.items())))

    for tag in ('sword', 'sharp_weapon', 'weapon', 'fire_aspect', 'durability'):
        write_json(os.path.join(DATA, 'minecraft', 'tags', 'item', 'enchantable', tag + '.json'),
                   {'replace': False, 'values': [MODID + ':edel_weapon']})
    print('generated resources in', os.path.normpath(ROOT))


if __name__ == '__main__':
    main()
