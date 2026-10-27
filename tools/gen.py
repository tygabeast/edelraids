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
        # In this rig a positive X rotation swings a hanging limb backward, so forward is negative.
        'animation.edel_raid.sit': {'loop': True, 'animation_length': 1.0, 'bones': {
            'root': {'position': [0, -10, 0]},
            'right_leg': {'rotation': [-85, 10, 0]},
            'left_leg': {'rotation': [-85, -10, 0]},
            'right_arm': {'rotation': [-30, 0, 5]},
            'left_arm': {'rotation': [-30, 0, -5]},
        }},
        'animation.edel_raid.wounded_idle': {'loop': True, 'animation_length': 3.0, 'bones': {
            'body': {'rotation': {'0.0': [12, 0, 0], '1.5': [14, 0, 0], '3.0': [12, 0, 0]}},
            'right_arm': {'rotation': [-15, 0, 8]},
            'left_arm': {'rotation': [-35, 20, -10]},
        }},
        'animation.edel_raid.wounded_walk': {'loop': True, 'animation_length': 1.4, 'bones': {
            'body': {'rotation': [12, 0, 0]},
            'right_leg': {'rotation': {'0.0': [14, 0, 0], '0.7': [-14, 0, 0], '1.4': [14, 0, 0]}},
            'left_leg': {'rotation': {'0.0': [-8, 0, 0], '0.7': [8, 0, 0], '1.4': [-8, 0, 0]}},
            'right_arm': {'rotation': {'0.0': [-10, 0, 8], '0.7': [-20, 0, 8], '1.4': [-10, 0, 8]}},
            'left_arm': {'rotation': [-35, 20, -10]},
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


# ------------------------------------------------------------------ Other weapon forms (placeholders, part lists)
# Every form is modelled upright with the hand's grip point at the origin, like the longsword, so they all share the
# item display transforms. Each has a "gem" bone (tinted by element) and an "awakened" bone (shown at high bond).

WOOD = (122, 88, 60, 255)
DARK = (92, 100, 110, 255)
STRING = (232, 230, 220, 255)

COLORS = {'grip': GRIP, 'guard': GUARD, 'blade': STEEL, 'edge': STEEL_EDGE, 'gem': GEM, 'wing': WING,
          'wood': WOOD, 'dark': DARK, 'string': STRING}

# bone -> (parent, [(origin, size, color role), ...])
FORM_PARTS = {
    'greatsword': {
        'grip': ('root', [([-1.5, -2, -1.5], [3, 2, 3], 'guard'), ([-0.75, 0, -0.75], [1.5, 7, 1.5], 'grip')]),
        'guard': ('root', [([-4.5, 7, -1.5], [9, 1.5, 3], 'guard')]),
        'gem': ('guard', [([-1, 7, -2], [2, 2, 1], 'gem')]),
        'blade': ('root', [([-2, 8.5, -0.5], [4, 20, 1], 'blade'), ([-1, 28.5, -0.5], [2, 3, 1], 'edge')]),
        'awakened': ('blade', [([-4, 10, -0.5], [2, 10, 1], 'wing'), ([2, 10, -0.5], [2, 10, 1], 'wing')]),
    },
    'spear': {
        'grip': ('root', [([-1, -6, -1], [2, 1, 2], 'dark'), ([-0.5, -5, -0.5], [1, 27, 1], 'wood')]),
        'guard': ('root', [([-1, 20, -1], [2, 1.5, 2], 'guard')]),
        'gem': ('guard', [([-0.5, 20.25, -1.5], [1, 1, 0.5], 'gem')]),
        'blade': ('root', [([-1.5, 21.5, -0.5], [3, 5, 1], 'blade'), ([-0.5, 26.5, -0.5], [1, 2, 1], 'edge')]),
        'awakened': ('blade', [([-3, 20.5, -0.25], [1.5, 3, 0.5], 'wing'), ([1.5, 20.5, -0.25], [1.5, 3, 0.5], 'wing')]),
    },
    'twin_blades': {
        'grip': ('root', [([-0.5, -3, -0.5], [1, 6, 1], 'grip')]),
        'guard': ('root', [([-2, 3, -1], [4, 1, 2], 'guard'), ([-2, -4, -1], [4, 1, 2], 'guard')]),
        'gem': ('guard', [([-1, 2.75, -1.5], [2, 1.5, 0.5], 'gem')]),
        'blade': ('root', [([-1, 4, -0.5], [2, 11, 1], 'blade'), ([-0.5, 15, -0.5], [1, 2, 1], 'edge'),
                           ([-1, -15, -0.5], [2, 11, 1], 'blade'), ([-0.5, -17, -0.5], [1, 2, 1], 'edge')]),
        'awakened': ('blade', [([1, 11, -0.25], [2, 4, 0.5], 'wing'), ([-3, -15, -0.25], [2, 4, 0.5], 'wing')]),
    },
    'scythe': {
        'grip': ('root', [([-0.5, -6, -0.5], [1, 28, 1], 'wood'), ([-1, 22, -1], [2, 1, 2], 'dark')]),
        'guard': ('root', [([-1, 18, -1], [2, 1, 2], 'dark')]),
        'gem': ('guard', [([-0.75, 19.5, -1.25], [1.5, 1.5, 0.5], 'gem')]),
        'blade': ('root', [([-6, 19.5, -0.25], [6, 2.5, 0.5], 'blade'), ([-10, 18.5, -0.25], [4, 2, 0.5], 'blade'),
                           ([-13, 16.5, -0.25], [3, 2, 0.5], 'edge')]),
        'awakened': ('blade', [([-8, 21, -0.25], [2, 2, 0.5], 'wing'), ([-4, 22, -0.25], [2, 2, 0.5], 'wing')]),
    },
    'axe': {
        'grip': ('root', [([-0.5, -1, -0.5], [1, 16, 1], 'wood'), ([-0.75, 15, -0.75], [1.5, 1, 1.5], 'dark')]),
        'guard': ('root', [([0.5, 11.5, -0.5], [2, 2, 1], 'dark')]),
        'gem': ('guard', [([-3, 11.75, -1], [1.5, 1.5, 0.5], 'gem')]),
        'blade': ('root', [([-5, 10, -0.5], [5, 5, 1], 'blade'), ([-6, 9, -0.5], [1, 7, 1], 'edge')]),
        'awakened': ('blade', [([2.5, 9.5, -0.25], [1.5, 6, 0.5], 'wing')]),
    },
    'war_hammer': {
        'grip': ('root', [([-0.75, -2, -0.75], [1.5, 1, 1.5], 'dark'), ([-0.5, -1, -0.5], [1, 15, 1], 'wood')]),
        'guard': ('root', [([-4.5, 14.5, -1.5], [0.5, 4, 3], 'dark'), ([4, 14.5, -1.5], [0.5, 4, 3], 'dark')]),
        'gem': ('guard', [([-1, 15.5, -2.5], [2, 2, 0.5], 'gem')]),
        'blade': ('root', [([-4, 14, -2], [8, 5, 4], 'blade')]),
        'awakened': ('blade', [([-0.5, 19, -0.5], [1, 3, 1], 'wing'), ([-2, 13, -2.5], [4, 1, 5], 'wing')]),
    },
    'rapier': {
        'grip': ('root', [([-0.75, -1.5, -0.75], [1.5, 1.5, 1.5], 'guard'), ([-0.5, 0, -0.5], [1, 4, 1], 'grip'),
                          ([1, 0, -0.25], [0.5, 4, 0.5], 'guard')]),
        'guard': ('root', [([-1.5, 4, -1.5], [3, 1.5, 3], 'guard')]),
        'gem': ('guard', [([-0.5, 4.25, -2], [1, 1, 0.5], 'gem')]),
        'blade': ('root', [([-0.25, 5.5, -0.25], [0.5, 18, 0.5], 'blade')]),
        'awakened': ('blade', [([-3.5, 4.5, -0.25], [2, 0.5, 0.5], 'wing'), ([1.5, 4.5, -0.25], [2, 0.5, 0.5], 'wing')]),
    },
    'gauntlets': {
        'grip': ('root', [([-2.5, -4.5, -2.5], [5, 3, 5], 'dark')]),
        'guard': ('root', [([-2.25, 3.5, -2.25], [4.5, 1, 4.5], 'guard')]),
        'gem': ('guard', [([-1, 0.5, -2.5], [2, 2, 0.5], 'gem')]),
        'blade': ('root', [([-2, -1.5, -2], [4, 5, 4], 'blade')]),
        'awakened': ('blade', [([-2, 4.5, -1], [1, 1.5, 1], 'wing'), ([-0.5, 4.5, -1], [1, 1.5, 1], 'wing'),
                               ([1, 4.5, -1], [1, 1.5, 1], 'wing')]),
    },
    'bow': {
        'grip': ('root', [([-0.5, -2, -0.5], [1, 4, 1], 'grip')]),
        'guard': ('root', [([-2.25, -13, -0.1], [0.2, 26, 0.2], 'string')]),
        'gem': ('grip', [([-0.75, -0.75, -1], [1.5, 1.5, 0.5], 'gem')]),
        'blade': ('root', [([-0.5, 2, -0.5], [1, 5, 1], 'wood'), ([-1.5, 7, -0.5], [1, 4, 1], 'wood'),
                           ([-2.5, 11, -0.5], [1, 3, 1], 'wood'), ([-0.5, -7, -0.5], [1, 5, 1], 'wood'),
                           ([-1.5, -11, -0.5], [1, 4, 1], 'wood'), ([-2.5, -14, -0.5], [1, 3, 1], 'wood')]),
        'awakened': ('blade', [([-4, 12, -0.25], [2, 3, 0.5], 'wing'), ([-4, -15, -0.25], [2, 3, 0.5], 'wing')]),
    },
}


def pack_form(name, parts, size=64):
    """Shelf-pack box-UV footprints into a size x size sheet; returns (geo, pixels)."""
    px = canvas(size, size)
    x = y = shelf = 0
    bones = [{'name': 'root', 'pivot': [0, 0, 0]}]
    for bone in ('grip', 'guard', 'gem', 'blade', 'awakened'):
        parent, cubes = parts[bone]
        out = []
        for origin, sz, role in cubes:
            sx, sy, sz_ = (max(1, math.ceil(v)) for v in sz)
            w, h = 2 * (sx + sz_), sz_ + sy
            if x + w > size:
                x, y, shelf = 0, y + shelf, 0
            if y + h > size:
                raise ValueError(f'{name}: texture sheet too small')
            col = COLORS[role]
            box_uv(px, x, y, sx, sy, sz_, all_faces(col, 1, 1, 1, 1) if role == 'gem' else all_faces(col, 1.05, 0.85, 0.95, 0.9))
            out.append(cube(origin, sz, [x, y]))
            x += w
            shelf = max(shelf, h)
        first = cubes[0][0]
        bones.append({'name': bone, 'parent': parent, 'pivot': [0, first[1], 0], 'cubes': out})
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.edel_weapon_' + name, 'texture_width': size, 'texture_height': size,
                        'visible_bounds_width': 3, 'visible_bounds_height': 4, 'visible_bounds_offset': [0, 1, 0]},
        'bones': bones}]}
    return geo, px


# ------------------------------------------------------------------ elements and forms (datapack registries)
# Glittering rings: each element beats the next one in its ring (position + 1) and loses to the one before.
# Ring 4 never appears in the series; it is this mod's own and only found in Edel Garden (not rollable).

RING_FORMS = {
    1: ['longsword', 'greatsword', 'spear', 'axe', 'war_hammer', 'bow'],
    2: ['rapier', 'twin_blades', 'longsword', 'war_hammer', 'gauntlets', 'bow'],
    3: ['scythe', 'twin_blades', 'spear', 'rapier', 'gauntlets', 'longsword'],
    4: ['greatsword', 'spear', 'axe', 'scythe', 'war_hammer', 'bow'],
}

# id, display name, ring, position, color, on-hit type, strength
ELEMENTS = [
    ('earth', 'Earth', 1, 0, '#A9773E', 'shatter', 1.0),
    ('wind', 'Wind', 1, 1, '#4FC9A0', 'knockback', 0.5),
    ('fire', 'Fire', 1, 2, '#F26B21', 'ignite', 3.0),
    ('water', 'Water', 1, 3, '#2F80ED', 'slow', 2.0),
    ('greenery', 'Greenery', 1, 4, '#43A047', 'root', 1.0),
    ('wisdom', 'Wisdom', 1, 5, '#5E35B1', 'insight', 0.2),
    ('beauty', 'Beauty', 2, 0, '#F48FB1', 'charm', 0.3),
    ('sound', 'Sound', 2, 1, '#FFB300', 'shield_break', 1.0),
    ('shield', 'Shield', 2, 2, '#5C7FA3', 'ward', 1.0),
    ('sword', 'Sword', 2, 3, '#D7DEE6', 'pierce', 0.4),
    ('love', 'Love', 2, 4, '#E2366F', 'lifesteal', 0.1),
    ('void', 'Void', 2, 5, '#22103A', 'strip', 1.0),
    ('light', 'Light', 3, 0, '#FFF1A8', 'smite', 0.5),
    ('darkness', 'Darkness', 3, 1, '#2B2B33', 'blind', 2.0),
    ('movement', 'Movement', 3, 2, '#9B1B30', 'haste', 2.0),
    ('quiet', 'Quiet', 3, 3, '#9FB0A8', 'silence', 3.0),
    ('life', 'Life', 3, 4, '#FFAB91', 'regen', 3.0),
    ('death', 'Death', 3, 5, '#4B2C3F', 'wither', 3.0),
    ('thunder', 'Thunder', 4, 0, '#FFD60A', 'chain', 3.0),
    ('steel', 'Steel', 4, 1, '#7D8A96', 'flat_damage', 1.5),
    ('ice', 'Ice', 4, 2, '#A5F2FF', 'freeze', 60.0),
    ('time', 'Time', 4, 3, '#C9A227', 'timeworn', 3.0),
    ('dream', 'Dream', 4, 4, '#CE93D8', 'drowse', 4.0),
    ('star', 'Star', 4, 5, '#8C9EFF', 'starfall', 0.5),
]

# id, display name, damage, speed, sweeps, reach bonus, ranged, Better Combat preset
FORMS = [
    ('longsword', 'Longsword', 7.0, 1.6, True, 0.0, False, 'bettercombat:sword'),
    ('greatsword', 'Greatsword', 9.0, 1.0, True, 0.5, False, 'bettercombat:claymore'),
    ('spear', 'Spear', 7.5, 1.2, False, 1.5, False, 'bettercombat:spear'),
    ('twin_blades', 'Twin Blades', 6.5, 1.8, True, 0.0, False, 'bettercombat:twin_blade'),
    ('scythe', 'Scythe', 8.5, 1.1, True, 1.0, False, 'bettercombat:scythe'),
    ('axe', 'Axe', 9.0, 0.9, False, 0.0, False, 'bettercombat:axe'),
    ('war_hammer', 'War Hammer', 10.0, 0.8, False, 0.0, False, 'bettercombat:hammer'),
    ('rapier', 'Rapier', 6.0, 2.0, False, 0.5, False, 'bettercombat:rapier'),
    ('gauntlets', 'Gauntlets', 5.0, 2.4, False, -1.0, False, 'bettercombat:fist'),
    ('bow', 'Bow', 3.0, 1.0, False, 0.0, True, None),
]

# Custom mob effects: id, display name, icon color
EFFECTS = [
    ('shattered', 'Shattered', (169, 119, 62, 255)),
    ('rooted', 'Rooted', (67, 160, 71, 255)),
    ('warded', 'Warded', (92, 127, 163, 255)),
    ('silenced', 'Silenced', (159, 176, 168, 255)),
    ('timeworn', 'Timeworn', (201, 162, 39, 255)),
]


def effect_icon(col):
    px = canvas(18, 18)
    for yy in range(18):
        for xx in range(18):
            d = math.hypot(xx - 8.5, yy - 8.5)
            if d <= 7.5:
                px[yy][xx] = shade(col, 1.15 - d / 15)
            elif d <= 8.5:
                px[yy][xx] = shade(col, 0.55)
    return px


def write_registries():
    base = os.path.join(DATA, MODID, MODID)
    for el, _, ring, pos, color, hit, strength in ELEMENTS:
        obj = {'ring': ring, 'position': pos, 'color': color,
               'forms': [MODID + ':' + f for f in RING_FORMS[ring]],
               'on_hit': {'type': MODID + ':' + hit, 'strength': strength}}
        if ring == 4:
            obj['rollable'] = False
        write_json(os.path.join(base, 'element', el + '.json'), obj)
    for fid, _, dmg, spd, sweeps, reach, ranged, preset in FORMS:
        obj = {'attack_damage': dmg, 'attack_speed': spd}
        if sweeps:
            obj['sweeps'] = True
        if reach:
            obj['reach_bonus'] = reach
        if ranged:
            obj['ranged'] = True
        if preset:
            obj['better_combat_preset'] = preset
        write_json(os.path.join(base, 'weapon_form', fid + '.json'), obj)


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
    'command.edelraids.created': 'Created %s: %s %s, %s.',
    'command.edelraids.unknown_element': 'Unknown element: %s',
    'command.edelraids.unknown_form': 'Unknown weapon form: %s',
    'message.edelraids.recovered_lost': '%s was lost from your inventory and has reformed beside you.',
}
for _el, _name, *_ in ELEMENTS:
    LANG['edelraids.element.edelraids.' + _el] = _name
for _fid, _name, *_ in FORMS:
    LANG['edelraids.form.edelraids.' + _fid] = _name
for _eid, _name, _ in EFFECTS:
    LANG['effect.edelraids.' + _eid] = _name


def main():
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'edel_raid.png'), 64, 64, entity_texture())
    write_json(os.path.join(ASSETS, 'geo', 'entity', 'edel_raid.geo.json'), entity_geo())
    write_json(os.path.join(ASSETS, 'animations', 'entity', 'edel_raid.animation.json'), entity_animations())

    write_png(os.path.join(ASSETS, 'textures', 'item', 'weapon', 'longsword.png'), 32, 32, longsword_texture())
    write_json(os.path.join(ASSETS, 'geo', 'item', 'weapon', 'longsword.geo.json'), longsword_geo())
    write_json(os.path.join(ASSETS, 'animations', 'item', 'edel_weapon.animation.json'), weapon_animations())
    for name, parts in FORM_PARTS.items():
        geo, px = pack_form(name, parts)
        write_json(os.path.join(ASSETS, 'geo', 'item', 'weapon', name + '.geo.json'), geo)
        write_png(os.path.join(ASSETS, 'textures', 'item', 'weapon', name + '.png'), 64, 64, px)
    for eid, _, col in EFFECTS:
        write_png(os.path.join(ASSETS, 'textures', 'mob_effect', eid + '.png'), 18, 18, effect_icon(col))
    write_registries()

    write_json(os.path.join(ASSETS, 'models', 'item', 'edel_weapon.json'), weapon_item_model())
    write_json(os.path.join(ASSETS, 'models', 'item', 'edel_raid_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
    write_json(os.path.join(ASSETS, 'lang', 'en_us.json'), dict(sorted(LANG.items())))

    for tag in ('sword', 'sharp_weapon', 'weapon', 'fire_aspect', 'durability', 'bow'):
        write_json(os.path.join(DATA, 'minecraft', 'tags', 'item', 'enchantable', tag + '.json'),
                   {'replace': False, 'values': [MODID + ':edel_weapon']})
    print('generated resources in', os.path.normpath(ROOT))


if __name__ == '__main__':
    main()
