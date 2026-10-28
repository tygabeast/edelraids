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
import gzip
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


# ------------------------------------------------------------------ Sting Raid entity (same body, artificial stone)

ARM_STONE = (194, 208, 226, 255)
ARM_EDGE = (236, 244, 252, 255)
ARM_DARK = (132, 140, 150, 255)
CRACK = (172, 172, 178, 255)


def sting_texture():
    """The Edel Raid sheet on top (64x64), the forearm in a free spot, and the three arm weapons below (rows 64+).
    The arm weapons are pale stone; the renderer washes them with a sixth of her element."""
    base = entity_texture()
    px = canvas(64, 128)
    for y in range(64):
        px[y] = list(base[y])
    # forearm 4x6x4 at 40,32 (the upper arm reuses the top half of the Edel Raid arm at 40,16)
    box_uv(px, 40, 32, 4, 6, 4, all_faces(SKIN))
    # the artificial gem: a hairline crack across its face, and a chipped shard beside it (1x1x1 at 60,4)
    px[2][58] = CRACK
    box_uv(px, 60, 4, 1, 1, 1, all_faces(GEM, 0.9, 0.9, 0.9, 0.9))
    # arm blade: cuff 5x4x5 at 0,64 and blade 1x13x2 at 20,64
    box_uv(px, 0, 64, 5, 4, 5, all_faces(ARM_DARK))
    rect(px, 5, 69, 15, 1, ARM_STONE)
    box_uv(px, 20, 64, 1, 13, 2, all_faces(ARM_STONE))
    rect(px, 22, 66, 1, 13, ARM_EDGE)       # front edge
    rect(px, 25, 66, 1, 13, ARM_EDGE)       # back edge
    # heavy arm: cuff 5x3x5 at 24,80 and fist 6x7x6 at 0,80
    box_uv(px, 24, 80, 5, 3, 5, all_faces(ARM_DARK))
    box_uv(px, 0, 80, 6, 7, 6, all_faces(ARM_STONE))
    rect(px, 0, 87, 24, 1, ARM_EDGE)        # knuckle ridge
    rect(px, 0, 91, 24, 1, ARM_DARK)
    # launcher arm: tube 5x7x5 at 0,96 and muzzle 3x3x3 at 20,96
    box_uv(px, 0, 96, 5, 7, 5, all_faces(ARM_STONE))
    rect(px, 0, 103, 20, 1, ARM_DARK)
    rect(px, 0, 106, 20, 1, ARM_DARK)
    box_uv(px, 20, 96, 3, 3, 3, all_faces(ARM_DARK))
    rect(px, 27, 97, 1, 1, (40, 42, 48, 255))   # bore, on the muzzle's bottom face
    return px


def sting_geo():
    geo = entity_geo()
    g = geo['minecraft:geometry'][0]
    g['description']['identifier'] = 'geometry.sting_raid'
    g['description']['texture_height'] = 128
    bones = []
    for b in g['bones']:
        b = json.loads(json.dumps(b))
        if b['name'] == 'right_arm':
            b['cubes'] = [cube([-8, 18, -2], [4, 6, 4], [40, 16])]
        if b['name'] == 'gem':
            # cut slightly wrong: tilted, off-centre, with a chipped shard
            b['cubes'] = [cube([-1.1, 28.8, -4.5], [2, 2, 1], [56, 0], [0, 29.8, -4.5], [0, 0, 14]),
                          cube([0.7, 30.4, -4.3], [1, 1, 1], [60, 4], [1.2, 30.9, -4], [0, 0, -20])]
        bones.append(b)
    bones += [
        {'name': 'right_forearm', 'parent': 'right_arm', 'pivot': [-6, 18, 0],
         'cubes': [cube([-8, 12, -2], [4, 6, 4], [40, 32])]},
        {'name': 'sting_blade', 'parent': 'right_arm', 'pivot': [-6, 18, 0], 'cubes': [
            cube([-8.5, 14, -2.5], [5, 4, 5], [0, 64]),
            cube([-6.5, 1, -1], [1, 13, 2], [20, 64]),
        ]},
        {'name': 'sting_heavy', 'parent': 'right_arm', 'pivot': [-6, 18, 0], 'cubes': [
            cube([-8.5, 15, -2.5], [5, 3, 5], [24, 80]),
            cube([-9, 8, -3], [6, 7, 6], [0, 80]),
        ]},
        {'name': 'sting_launcher', 'parent': 'right_arm', 'pivot': [-6, 18, 0], 'cubes': [
            cube([-8.5, 11, -2.5], [5, 7, 5], [0, 96]),
            cube([-7.5, 8, -1.5], [3, 3, 3], [20, 96]),
        ]},
    ]
    g['bones'] = bones
    return geo


def sting_animations():
    base = entity_animations()['animations']
    anims = {k.replace('animation.edel_raid.', 'animation.sting_raid.'): v for k, v in base.items()
             if not k.endswith(('.react', '.reform'))}
    idle = base['animation.edel_raid.idle']['bones']
    walk = base['animation.edel_raid.walk']['bones']
    armed = [-35, 0, 6]
    pop = {'scale': {'0.0': [0.3, 0.3, 0.3], '0.35': [1.1, 1.1, 1.1], '0.5': [1, 1, 1]}}
    anims.update({
        'animation.sting_raid.self_react': {'loop': False, 'animation_length': 0.5, 'bones': {
            'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.2': [-45, 0, 25], '0.5': armed}},
            'body': {'rotation': {'0.0': [0, 0, 0], '0.2': [6, -8, 0], '0.5': [0, 0, 0]}},
            'sting_blade': pop, 'sting_heavy': pop, 'sting_launcher': pop,
        }},
        'animation.sting_raid.reacted_idle': {'loop': True, 'animation_length': 3.0, 'bones': {
            'right_arm': {'rotation': armed},
            'left_arm': idle['left_arm'],
            'body': idle['body'],
        }},
        'animation.sting_raid.reacted_walk': {'loop': True, 'animation_length': 1.0, 'bones': {
            'right_leg': walk['right_leg'], 'left_leg': walk['left_leg'], 'left_arm': walk['left_arm'],
            'right_arm': {'rotation': {'0.0': [-30, 0, 6], '0.5': [-38, 0, 6], '1.0': [-30, 0, 6]}},
        }},
        'animation.sting_raid.stagger': {'loop': True, 'animation_length': 1.0, 'bones': {
            'body': {'rotation': {'0.0': [15, 0, -6], '0.5': [17, 0, 6], '1.0': [15, 0, -6]}},
            'head': {'rotation': [18, 0, 0]},
            'right_arm': {'rotation': [-10, 0, 18]},
            'left_arm': {'rotation': [-28, 0, -14]},
        }},
        'animation.sting_raid.attack': {'loop': False, 'animation_length': 0.4, 'bones': {
            'right_arm': {'rotation': {'0.0': armed, '0.15': [-125, 0, 12], '0.4': [-20, 0, 6]}},
        }},
        'animation.sting_raid.aim': {'loop': True, 'animation_length': 1.0, 'bones': {
            'right_arm': {'rotation': [-90, 0, 0]},
        }},
    })
    return {'format_version': '1.8.0', 'animations': anims}


def stabilizer_texture(refined):
    """16x16 item: an amethyst-like crystal held in a metal clasp (gold for the refined one)."""
    px = canvas(16, 16)
    crystal = (176, 132, 226, 255) if not refined else (214, 178, 255, 255)
    light = shade(crystal, 1.25)
    dark = shade(crystal, 0.7)
    band = (168, 176, 186, 255) if not refined else (232, 196, 82, 255)
    band_dark = shade(band, 0.72)
    for y in range(2, 14):
        half = min(y - 1, 14 - y, 3)
        for x in range(8 - half, 8 + half):
            px[y][x] = light if x < 8 - half + 2 and y < 9 else (dark if x >= 8 + half - 1 else crystal)
    rect(px, 4, 7, 8, 2, band)
    rect(px, 4, 9, 8, 1, band_dark)
    rect(px, 3, 7, 1, 3, band_dark)
    rect(px, 12, 7, 1, 3, band_dark)
    if refined:
        rect(px, 7, 1, 2, 1, (255, 255, 255, 255))
        px[4][12] = (255, 255, 255, 255)
        px[11][3] = (255, 240, 200, 255)
    return px


def write_recipes():
    rec = os.path.join(DATA, MODID, 'recipe')
    adv = os.path.join(DATA, MODID, 'advancement', 'recipes', 'misc')
    write_json(os.path.join(rec, 'stone_stabilizer.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': [' A ', 'IRI', ' A '],
        'key': {'A': {'item': 'minecraft:amethyst_shard'}, 'I': {'item': 'minecraft:iron_ingot'},
                'R': {'item': 'minecraft:redstone'}},
        'result': {'id': MODID + ':stone_stabilizer', 'count': 2}})
    write_json(os.path.join(rec, 'refined_stabilizer.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['ASA', 'SDS', 'ASA'],
        'key': {'A': {'item': 'minecraft:amethyst_shard'}, 'S': {'item': MODID + ':stone_stabilizer'},
                'D': {'item': 'minecraft:diamond'}},
        'result': {'id': MODID + ':refined_stabilizer', 'count': 1}})
    for name, trigger_item in (('stone_stabilizer', 'minecraft:amethyst_shard'), ('refined_stabilizer', MODID + ':stone_stabilizer')):
        write_json(os.path.join(adv, name + '.json'), {
            'parent': 'minecraft:recipes/root',
            'criteria': {
                'has_item': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': trigger_item}]}},
                'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': MODID + ':' + name}},
            },
            'requirements': [['has_item', 'has_the_recipe']],
            'rewards': {'recipes': [MODID + ':' + name]}})


# ------------------------------------------------------------------ NBT (structure templates)

class Tag:
    """A typed NBT value: kind is one of b s i l f d str list compound intarray."""
    def __init__(self, kind, value, elem=None):
        self.kind, self.value, self.elem = kind, value, elem

def B(v): return Tag('b', v)
def I(v): return Tag('i', v)
def L(v): return Tag('l', v)
def D(v): return Tag('d', v)
def S(v): return Tag('str', v)
def LIST(kind, values): return Tag('list', values, kind)
def C(d): return Tag('compound', d)

_IDS = {'b': 1, 's': 2, 'i': 3, 'l': 4, 'f': 5, 'd': 6, 'str': 8, 'list': 9, 'compound': 10, 'intarray': 11}


def _payload(t):
    k = t.kind
    if k == 'b': return struct.pack('>b', t.value)
    if k == 's': return struct.pack('>h', t.value)
    if k == 'i': return struct.pack('>i', t.value)
    if k == 'l': return struct.pack('>q', t.value)
    if k == 'f': return struct.pack('>f', t.value)
    if k == 'd': return struct.pack('>d', t.value)
    if k == 'str':
        b = t.value.encode('utf-8')
        return struct.pack('>H', len(b)) + b
    if k == 'list':
        out = struct.pack('>bi', _IDS[t.elem] if t.value else 0, len(t.value))
        return out + b''.join(_payload(v) for v in t.value)
    if k == 'compound':
        out = b''
        for name, v in t.value.items():
            nb = name.encode('utf-8')
            out += struct.pack('>bH', _IDS[v.kind], len(nb)) + nb + _payload(v)
        return out + b'\x00'
    if k == 'intarray':
        return struct.pack('>i', len(t.value)) + b''.join(struct.pack('>i', x) for x in t.value)
    raise ValueError(k)


def write_nbt(path, root):
    data = struct.pack('>bH', 10, 0) + _payload(root)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        # mtime=0 keeps the file byte-identical between runs
        with gzip.GzipFile(fileobj=f, mode='wb', mtime=0) as gz:
            gz.write(data)


# ------------------------------------------------------------------ Hunter camp

DATA_VERSION = 3955  # Minecraft 1.21.1
CAMP_W, CAMP_H, CAMP_D = 17, 6, 17
CAMP_FLOOR = ['minecraft:grass_block', 'minecraft:coarse_dirt']

# (centre x, centre z) of each cage; a variant uses the first n+1: one cage per captive plus one left empty, which
# hunters fill with an Arc Aile agent's partner if they ever beat him
CAGES = [(3, 13), (13, 13), (8, 14), (2, 9)]
# id, (x, z) of each guard; a variant uses a prefix of each list
CAMP_HUNTERS = [('hunter', (7.5, 9.5)), ('hunter', (9.5, 7.5))]
CAMP_MARKSMEN = [('hunter_marksman', (12.5, 7.5)), ('hunter_marksman', (4.5, 7.5))]
CAMP_STINGS = [('sting_raid', (6.5, 13.5)), ('sting_raid', (10.5, 13.5))]
# name: (cages, marksmen, sting raids, pool weight)
CAMP_VARIANTS = {'small': (1, 0, 1, 3), 'medium': (2, 1, 1, 4), 'large': (3, 2, 2, 2)}


def camp_blocks(cages):
    """{(x, y, z): (block, props, nbt)} for one camp. Everything not listed above the floor is air (clears plants)."""
    blocks = {}
    for x in range(CAMP_W):
        for z in range(CAMP_D):
            near_fire = abs(x - 8) <= 2 and abs(z - 8) <= 2
            path = ((x * 73856093) ^ (z * 19349663)) % 7 == 0  # scattered worn patches
            blocks[(x, 0, z)] = ('minecraft:coarse_dirt' if near_fire or path else 'minecraft:grass_block', {'snowy': 'false'} if not (near_fire or path) else {}, None)
            for y in range(1, CAMP_H):
                blocks[(x, y, z)] = ('minecraft:air', {}, None)

    def put(x, y, z, block, props=None, nbt=None):
        blocks[(x, y, z)] = (block, props or {}, nbt)

    # campfire and two log seats
    put(8, 1, 8, 'minecraft:campfire', {'facing': 'north', 'lit': 'true', 'signal_fire': 'false', 'waterlogged': 'false'})
    put(6, 1, 8, 'minecraft:stripped_spruce_log', {'axis': 'z'})
    put(10, 1, 8, 'minecraft:stripped_spruce_log', {'axis': 'z'})
    # two A-frame tents opening south toward the fire
    for x0, wool in ((1, 'minecraft:brown_wool'), (11, 'minecraft:light_gray_wool')):
        for z in range(1, 6):
            put(x0, 1, z, wool); put(x0 + 4, 1, z, wool)
            put(x0 + 1, 2, z, wool); put(x0 + 3, 2, z, wool)
            put(x0 + 2, 3, z, wool)
        for x in range(x0 + 1, x0 + 4):
            put(x, 1, 1, wool)
        put(x0 + 2, 2, 1, wool)
        put(x0 + 2, 1, 2, 'minecraft:white_carpet')
    # supplies: loot chest, barrel, crafting table, hay
    put(6, 1, 2, 'minecraft:chest', {'facing': 'south', 'type': 'single', 'waterlogged': 'false'},
        C({'id': S('minecraft:chest'), 'LootTable': S(MODID + ':chests/hunter_camp')}))
    put(7, 1, 2, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    put(9, 1, 2, 'minecraft:crafting_table')
    put(10, 1, 2, 'minecraft:hay_block', {'axis': 'y'})
    # lantern posts
    for x, z in ((6, 11), (10, 11)):
        put(x, 1, z, 'minecraft:spruce_fence', {'east': 'false', 'west': 'false', 'north': 'false', 'south': 'false', 'waterlogged': 'false'})
        put(x, 2, z, 'minecraft:lantern', {'hanging': 'false', 'waterlogged': 'false'})
    # cages: 3x3 iron bars, a spruce gate on the side facing the fire, a slab roof
    for cx, cz in CAGES[:cages]:
        ring = [(cx + dx, cz + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1) if (dx, dz) != (0, 0)]
        for y in (1, 2):
            for (x, z) in ring:
                if y == 1 and (x, z) == (cx, cz - 1):
                    put(x, y, z, 'minecraft:spruce_fence_gate', {'facing': 'north', 'open': 'false', 'in_wall': 'false', 'powered': 'false'})
                else:
                    put(x, y, z, 'minecraft:iron_bars', {})
        for (x, z) in ring + [(cx, cz)]:
            put(x, 3, z, 'minecraft:spruce_slab', {'type': 'bottom', 'waterlogged': 'false'})
    # iron bars join to neighbouring bars
    for (x, y, z), (block, props, nbt) in list(blocks.items()):
        if block == 'minecraft:iron_bars':
            conn = {}
            for side, (dx, dz) in (('north', (0, -1)), ('south', (0, 1)), ('east', (1, 0)), ('west', (-1, 0))):
                nb = blocks.get((x + dx, y, z + dz), ('',))[0]
                conn[side] = 'true' if nb == 'minecraft:iron_bars' else 'false'
            conn['waterlogged'] = 'false'
            blocks[(x, y, z)] = (block, conn, nbt)
    return blocks


def camp_entities(cages, marksmen, stings):
    out = []
    for cx, cz in CAGES[:cages]:
        out.append(((cx + 0.5, 1.0, cz + 0.5), {'id': S(MODID + ':edel_raid'), 'Captive': B(1), 'PersistenceRequired': B(1)}))
    for eid, (x, z) in CAMP_HUNTERS + CAMP_MARKSMEN[:marksmen] + CAMP_STINGS[:stings]:
        out.append(((x, 1.0, z), {'id': S(MODID + ':' + eid), 'PersistenceRequired': B(1)}))
    return out


def camp_template(cages, marksmen, stings):
    blocks = camp_blocks(cages + 1)
    palette, index, entries = [], {}, []
    for (x, y, z), (block, props, nbt) in sorted(blocks.items()):
        key = (block, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            state = {'Name': S(block)}
            if props:
                state['Properties'] = C({k: S(v) for k, v in sorted(props.items())})
            palette.append(C(state))
        e = {'pos': LIST('i', [I(x), I(y), I(z)]), 'state': I(index[key])}
        if nbt is not None:
            e['nbt'] = nbt
        entries.append(C(e))
    ents = []
    for (x, y, z), nbt in camp_entities(cages, marksmen, stings):
        ents.append(C({'pos': LIST('d', [D(x), D(y), D(z)]),
                       'blockPos': LIST('i', [I(int(math.floor(x))), I(int(y)), I(int(math.floor(z)))]),
                       'nbt': C(nbt)}))
    return C({'DataVersion': I(DATA_VERSION),
              'size': LIST('i', [I(CAMP_W), I(CAMP_H), I(CAMP_D)]),
              'palette': LIST('compound', palette),
              'blocks': LIST('compound', entries),
              'entities': LIST('compound', ents)}), blocks


CAMP_BIOMES = ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:meadow', 'minecraft:forest',
               'minecraft:birch_forest', 'minecraft:savanna', 'minecraft:savanna_plateau', 'minecraft:taiga',
               'minecraft:old_growth_spruce_taiga', 'minecraft:snowy_plains', 'minecraft:badlands', 'minecraft:wooded_badlands']


def write_camp():
    base = os.path.join(DATA, MODID)
    previews = {}
    for name, (cages, marksmen, stings, _) in CAMP_VARIANTS.items():
        tag, blocks = camp_template(cages, marksmen, stings)  # one more cage than captives, left empty
        write_nbt(os.path.join(base, 'structure', 'hunter_camp', name + '.nbt'), tag)
        previews[name] = {'blocks': [[x, y, z, b] for (x, y, z), (b, _, _) in blocks.items() if b != 'minecraft:air'],
                          'entities': [[p[0], p[1], p[2], n['id'].value] for p, n in camp_entities(cages, marksmen, stings)]}
    write_json(os.path.join(base, 'worldgen', 'template_pool', 'hunter_camp', 'start.json'), {
        'fallback': 'minecraft:empty',
        'elements': [{'weight': w, 'element': {'element_type': 'minecraft:single_pool_element',
                                                 'location': MODID + ':hunter_camp/' + name,
                                                 'projection': 'rigid', 'processors': 'minecraft:empty'}}
                     for name, (_, _, _, w) in CAMP_VARIANTS.items()]})
    write_json(os.path.join(base, 'worldgen', 'structure', 'hunter_camp.json'), {
        'type': 'minecraft:jigsaw', 'biomes': '#' + MODID + ':has_structure/hunter_camp',
        'step': 'surface_structures', 'spawn_overrides': {}, 'terrain_adaptation': 'beard_thin',
        'start_pool': MODID + ':hunter_camp/start', 'size': 1, 'start_height': {'absolute': 0},
        'project_start_to_heightmap': 'WORLD_SURFACE_WG', 'max_distance_from_center': 80, 'use_expansion_hack': False})
    # About as rare as pillager outposts, and kept away from villages.
    write_json(os.path.join(base, 'worldgen', 'structure_set', 'hunter_camp.json'), {
        'structures': [{'structure': MODID + ':hunter_camp', 'weight': 1}],
        'placement': {'type': 'minecraft:random_spread', 'spacing': 32, 'separation': 8, 'salt': 1748395023,
                      'frequency_reduction_method': 'default', 'frequency': 0.25,
                      'exclusion_zone': {'other_set': 'minecraft:villages', 'chunk_count': 6}}})
    write_json(os.path.join(base, 'tags', 'worldgen', 'biome', 'has_structure', 'hunter_camp.json'),
               {'replace': False, 'values': CAMP_BIOMES})
    return previews


def item_entry(item, weight, lo=1, hi=1):
    e = {'type': 'minecraft:item', 'name': item, 'weight': weight}
    if (lo, hi) != (1, 1):
        e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    return e


def write_loot():
    lt = os.path.join(DATA, MODID, 'loot_table')
    write_json(os.path.join(lt, 'chests', 'hunter_camp.json'), {'type': 'minecraft:chest', 'pools': [{
        'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6},
        'entries': [item_entry('minecraft:emerald', 10, 1, 4), item_entry('minecraft:bread', 10, 2, 4),
                    item_entry('minecraft:iron_ingot', 8, 1, 3), item_entry('minecraft:arrow', 8, 4, 12),
                    item_entry('minecraft:amethyst_shard', 6, 2, 5), item_entry('minecraft:redstone', 6, 2, 6),
                    item_entry(MODID + ':stone_stabilizer', 4, 1, 2), item_entry('minecraft:lead', 3),
                    item_entry('minecraft:name_tag', 1)]}],
        'random_sequence': MODID + ':chests/hunter_camp'})
    looting = {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
               'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}
    for eid, extra in (('hunter', None), ('hunter_marksman', ('minecraft:arrow', 0, 2))):
        pools = [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:emerald', 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}, looting]}]},
                 {'rolls': 1, 'conditions': [{'condition': 'minecraft:killed_by_player'},
                                             {'condition': 'minecraft:random_chance', 'chance': 0.06}],
                  'entries': [{'type': 'minecraft:item', 'name': MODID + ':stone_stabilizer'}]}]
        if extra:
            pools.append({'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': extra[0], 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': extra[1], 'max': extra[2]}}, looting]}]})
        write_json(os.path.join(lt, 'entities', eid + '.json'),
                   {'type': 'minecraft:entity', 'pools': pools, 'random_sequence': MODID + ':entities/' + eid})


# ------------------------------------------------------------------ Hunter textures (vanilla humanoid layout)

HUNTER_SKIN = (214, 170, 136, 255)
HUNTER_HAIR = (58, 44, 36, 255)
LEATHER = (118, 86, 58, 255)
BOOTS = (62, 48, 40, 255)
BELT = (44, 36, 30, 255)
BUCKLE = (196, 170, 96, 255)


def hunter_texture(cloak, marksman):
    px = canvas(64, 64)
    # head 8x8x8 at 0,0
    box_uv(px, 0, 0, 8, 8, 8, {'top': HUNTER_HAIR, 'bottom': HUNTER_SKIN, 'right': HUNTER_HAIR, 'front': HUNTER_SKIN,
                                'left': HUNTER_HAIR, 'back': HUNTER_HAIR})
    rect(px, 8, 8, 8, 2, HUNTER_HAIR)
    rect(px, 10, 11, 1, 1, (40, 40, 44, 255))
    rect(px, 13, 11, 1, 1, (40, 40, 44, 255))
    rect(px, 9, 14, 6, 2, shade(HUNTER_SKIN, 0.8) if not marksman else shade(cloak, 0.8))  # stubble, or a face scarf
    # hood on the hat layer (32,0): open at the front and bottom so the face shows
    box_uv(px, 32, 0, 8, 8, 8, all_faces(cloak))
    rect(px, 40, 8, 8, 8, (0, 0, 0, 0))
    rect(px, 48, 0, 8, 8, (0, 0, 0, 0))
    rect(px, 40, 8, 8, 1, shade(cloak, 0.85))
    # body 8x12x4 at 16,16: cloak over a leather jerkin, with a belt
    box_uv(px, 16, 16, 8, 12, 4, all_faces(cloak))
    rect(px, 22, 20, 4, 12, LEATHER)
    rect(px, 16, 28, 24, 1, BELT)
    rect(px, 23, 28, 2, 1, BUCKLE)
    if marksman:  # quiver strap across the chest
        for i in range(8):
            px[20 + i][20 + i] = BELT
    # arm 4x12x4 at 40,16 (the left arm mirrors it): sleeve, then glove
    box_uv(px, 40, 16, 4, 12, 4, all_faces(cloak))
    rect(px, 40, 28, 16, 4, LEATHER)
    # leg 4x12x4 at 0,16: trousers and boots
    box_uv(px, 0, 16, 4, 12, 4, all_faces(shade(LEATHER, 0.7)))
    rect(px, 0, 28, 16, 4, BOOTS)
    return px


# ------------------------------------------------------------------ Arc Aile (agent texture and pass items)

COAT = (232, 236, 242, 255)
COAT_TRIM = (46, 74, 122, 255)
INSIGNIA = (214, 182, 92, 255)
AGENT_HAIR = (40, 36, 44, 255)


def agent_texture():
    """An Arc Aile agent: a long white coat with deep blue trim and a gold insignia, dark trousers and boots."""
    px = canvas(64, 64)
    box_uv(px, 0, 0, 8, 8, 8, {'top': AGENT_HAIR, 'bottom': HUNTER_SKIN, 'right': AGENT_HAIR, 'front': HUNTER_SKIN,
                                'left': AGENT_HAIR, 'back': AGENT_HAIR})
    rect(px, 8, 8, 8, 2, AGENT_HAIR)
    rect(px, 10, 11, 1, 1, (36, 52, 86, 255))
    rect(px, 13, 11, 1, 1, (36, 52, 86, 255))
    rect(px, 11, 14, 2, 1, shade(HUNTER_SKIN, 0.85))
    # the hat layer stays empty (no hood)
    box_uv(px, 16, 16, 8, 12, 4, all_faces(COAT))
    rect(px, 23, 20, 2, 12, COAT_TRIM)          # coat opening
    rect(px, 20, 20, 8, 1, COAT_TRIM)           # collar
    rect(px, 21, 23, 1, 2, INSIGNIA)            # insignia over the heart
    rect(px, 20, 31, 8, 1, COAT_TRIM)           # hem
    box_uv(px, 40, 16, 4, 12, 4, all_faces(COAT))
    rect(px, 40, 29, 16, 2, COAT_TRIM)          # cuffs
    rect(px, 40, 31, 16, 1, HUNTER_SKIN)        # hands
    box_uv(px, 0, 16, 4, 12, 4, all_faces((44, 52, 74, 255)))
    rect(px, 0, 28, 16, 4, (30, 28, 32, 255))   # boots
    return px


def writ_texture():
    """16x16: a rolled writ tied with Arc Aile blue, sealed in gold."""
    px = canvas(16, 16)
    paper, edge = (236, 226, 196, 255), (196, 182, 146, 255)
    for i in range(10):
        x, y = 3 + i, 11 - i
        rect(px, x, y - 1, 2, 3, paper)
        px[y + 1][x] = edge
    rect(px, 2, 11, 3, 3, edge)
    rect(px, 12, 1, 3, 3, edge)
    for i in range(3):
        px[8 - i][6 + i] = COAT_TRIM
        px[9 - i][6 + i] = COAT_TRIM
    rect(px, 9, 8, 3, 3, INSIGNIA)
    px[9][10] = shade(INSIGNIA, 0.7)
    return px


def sigil_texture():
    """16x16: an agent's sigil, a gold-rimmed blue disc with a white wing."""
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 6.5:
                px[y][x] = INSIGNIA if d > 5.2 else COAT_TRIM
    for i in range(5):
        rect(px, 5 + i, 9 - i, 1, 1 + i // 2, COAT)
    px[4][11] = (255, 255, 255, 255)
    return px


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
    'twin_blades': {  # one of a pair: the second sword is in the off hand
        'grip': ('root', [([-0.75, -1, -0.75], [1.5, 1, 1.5], 'guard'), ([-0.5, 0, -0.5], [1, 4, 1], 'grip')]),
        'guard': ('root', [([-2, 4, -1], [4, 1, 2], 'guard')]),
        'gem': ('guard', [([-0.75, 4, -1.5], [1.5, 1, 0.5], 'gem')]),
        'blade': ('root', [([-1, 5, -0.5], [2, 12, 1], 'blade'), ([-0.5, 17, -0.5], [1, 2, 1], 'edge')]),
        'awakened': ('blade', [([1, 9, -0.25], [1.5, 5, 0.5], 'wing')]),
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
    'gauntlets': {  # cuff on the forearm, steel over the hand, gold knuckle plate; geo +y runs toward the fingers
        'grip': ('root', [([-2.5, -3.5, -2.5], [5, 2, 5], 'dark')]),
        'guard': ('root', [([-2.25, 2.5, -2.25], [4.5, 1, 4.5], 'guard')]),
        'gem': ('guard', [([-1, 0, -2.5], [2, 2, 0.5], 'gem')]),
        'blade': ('root', [([-2, -1.5, -2], [4, 4, 4], 'blade')]),
        'awakened': ('blade', [([-2, 3.5, -1], [1, 1.5, 1], 'wing'), ([-0.5, 3.5, -1], [1, 1.5, 1], 'wing'),
                               ([1, 3.5, -1], [1, 1.5, 1], 'wing')]),
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


MIRRORED = {'axe', 'scythe'}


def mirror_x(parts):
    return {bone: (parent, [([-(o[0] + sz[0]), o[1], o[2]], sz, role) for o, sz, role in cubes])
            for bone, (parent, cubes) in parts.items()}


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
    ('greenery', 'Greenery', 1, 4, '#43A047', 'root', 2.0),
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

# id, display name, damage, speed, sweeps, reach bonus, ranged, Better Combat preset, paired (off-hand twin)
FORMS = [
    ('longsword', 'Longsword', 7.0, 1.6, True, 0.0, False, 'bettercombat:sword', False),
    ('greatsword', 'Greatsword', 9.0, 1.0, True, 0.5, False, 'bettercombat:claymore', False),
    ('spear', 'Spear', 7.5, 1.2, False, 1.5, False, 'bettercombat:spear', False),
    ('twin_blades', 'Twin Blades', 6.5, 1.8, True, 0.0, False, 'bettercombat:sword', True),
    ('scythe', 'Scythe', 8.5, 1.1, True, 1.0, False, 'bettercombat:scythe', False),
    ('axe', 'Axe', 9.0, 0.9, False, 0.0, False, 'bettercombat:axe', False),
    ('war_hammer', 'War Hammer', 10.0, 0.8, False, 0.0, False, 'bettercombat:hammer', False),
    ('rapier', 'Rapier', 6.0, 2.0, False, 0.5, False, 'bettercombat:rapier', False),
    ('gauntlets', 'Gauntlets', 5.0, 2.4, False, -1.0, False, 'bettercombat:fist', True),
    ('bow', 'Bow', 3.0, 1.0, False, 0.0, True, None, False),
]

# The arm weapon a Sting Raid of each form grows (forms not listed: blade, or launcher if ranged)
STING_ARMS = {'greatsword': 'heavy', 'axe': 'heavy', 'war_hammer': 'heavy', 'gauntlets': 'heavy', 'bow': 'launcher'}

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


# Songs: (bond 25, bond 50, full song at bond 100) per element, each (name, shape)
SONGS = {
    'earth': [('Ourea Cadence', 'burst'), ('Rhea Sostenuto', 'ward'), ('Gaia Anthem', 'field')],
    'wind': [('Aura Trill', 'dash'), ('Aeolus Fanfare', 'wave'), ('Anemoi Chorus', 'burst')],
    'fire': [('Prometheus Toccata', 'call'), ('Hestia Rondo', 'ward'), ('Hephaestus Anthem', 'burst')],
    'water': [('Nereid Barcarolle', 'wave'), ('Tethys Etude', 'ward'), ('Poseidon Chorus', 'field')],
    'greenery': [('Dryad Serenade', 'field'), ('Chloris Madrigal', 'call'), ('Demeter Hymn', 'burst')],
    'wisdom': [('Metis Fugue', 'call'), ('Mnemosyne Prelude', 'ward'), ('Athena Anthem', 'wave')],
    'beauty': [('Charis Waltz', 'field'), ('Galatea Aria', 'call'), ('Aphrodite Chorus', 'burst')],
    'sound': [('Echo Reprise', 'wave'), ('Orpheus Ballad', 'field'), ('Apollo Hymn', 'burst')],
    'shield': [('Aegis Refrain', 'ward'), ('Perseus Coda', 'call'), ('Ajax Chorale', 'field')],
    'sword': [('Ares Staccato', 'dash'), ('Harpe Sforzando', 'wave'), ('Enyo Anthem', 'burst')],
    'love': [('Eros Arpeggio', 'call'), ('Himeros Romance', 'field'), ('Psyche Chorus', 'burst')],
    'void': [('Charybdis Ostinato', 'wave'), ('Tartarus Drone', 'field'), ('Chaos Anthem', 'burst')],
    'light': [('Eos Aubade', 'dash'), ('Phoebe Cantata', 'call'), ('Helios Hymn', 'burst')],
    'darkness': [('Erebus Tremolo', 'dash'), ('Hecate Nocturne', 'field'), ('Nyx Chorus', 'burst')],
    'movement': [('Hermes Presto', 'dash'), ('Talaria Scherzo', 'field'), ('Iris Hymn', 'wave')],
    'quiet': [('Harpocrates Rest', 'call'), ('Hypnos Lullaby', 'field'), ('Sige Hymn', 'burst')],
    'life': [('Hygieia Air', 'ward'), ('Asclepius Canticle', 'field'), ('Panacea Hymn', 'burst')],
    'death': [('Keres Dirge', 'call'), ('Charon Elegy', 'field'), ('Thanatos Requiem', 'burst')],
    'thunder': [('Astrape Glissando', 'call'), ('Keraunos Timpani', 'burst'), ('Zeus Anthem', 'field')],
    'steel': [('Talos March', 'dash'), ('Adamant Canon', 'ward'), ('Cyclopes Chorus', 'wave')],
    'ice': [('Khione Carol', 'call'), ('Cocytus Lament', 'field'), ('Hyperborea Chorale', 'burst')],
    'time': [('Kairos Tempo', 'dash'), ('Horae Round', 'field'), ('Chronos Symphony', 'burst')],
    'dream': [('Phantasos Fantasia', 'call'), ('Oneiroi Interlude', 'field'), ('Morpheus Hymn', 'burst')],
    'star': [('Asteria Overture', 'call'), ('Pleiades Septet', 'wave'), ('Astraea Hymn', 'burst')],
}

# Per shape, per slot (bond 25, bond 50, full): power, size, duration. Costs are 25, 50, 100.
SHAPE_STATS = {
    'burst': [(4.0, 3.5, 0), (6.0, 4.5, 0), (12.0, 6.0, 0)],
    'wave': [(5.0, 6.5, 0), (7.0, 7.5, 0), (12.0, 9.5, 0)],
    'dash': [(5.0, 6.0, 0), (7.0, 8.0, 0), (12.0, 10.0, 0)],
    'ward': [(4.0, 3.0, 200), (6.0, 3.0, 300), (10.0, 4.0, 400)],
    'field': [(1.5, 3.0, 100), (2.0, 4.0, 140), (3.5, 6.0, 200)],
    'call': [(6.0, 16.0, 0), (8.0, 20.0, 0), (14.0, 24.0, 0)],
}
SONG_COSTS = [25, 50, 100]


def write_songs():
    base = os.path.join(DATA, MODID, MODID, 'song')
    for el, songs in SONGS.items():
        for slot, (name, shape) in enumerate(songs, start=1):
            power, size, duration = SHAPE_STATS[shape][slot - 1]
            obj = {'element': MODID + ':' + el, 'slot': slot, 'shape': shape, 'cost': SONG_COSTS[slot - 1],
                   'power': power, 'size': size}
            if duration:
                obj['duration'] = duration
            write_json(os.path.join(base, f'{el}_{slot}.json'), obj)


def write_registries():
    base = os.path.join(DATA, MODID, MODID)
    for el, _, ring, pos, color, hit, strength in ELEMENTS:
        obj = {'ring': ring, 'position': pos, 'color': color,
               'forms': [MODID + ':' + f for f in RING_FORMS[ring]],
               'on_hit': {'type': MODID + ':' + hit, 'strength': strength}}
        if ring == 4:
            obj['rollable'] = False
        write_json(os.path.join(base, 'element', el + '.json'), obj)
    for fid, _, dmg, spd, sweeps, reach, ranged, preset, paired in FORMS:
        obj = {'attack_damage': dmg, 'attack_speed': spd}
        if sweeps:
            obj['sweeps'] = True
        if reach:
            obj['reach_bonus'] = reach
        if ranged:
            obj['ranged'] = True
        if preset:
            obj['better_combat_preset'] = preset
        if paired:
            obj['paired'] = True
        if fid in STING_ARMS:
            obj['sting_arm'] = STING_ARMS[fid]
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


def from_vanilla(rot, trans, scale, handle=(-5.5, -5.5, 0.0), tilt=-45.0, model_scale=1.0):
    """Our model upright with grip at origin == a vanilla sprite tilted `tilt` degrees with its grip at `handle` (pixels
    from the sprite centre). model_scale shrinks our model to the sprite's size. Works on json values for either hand:
    for a left-hand entry pass the vanilla left entry, the handle mirrored in x and the tilt negated."""
    rv = xyz(*rot)
    r = mul(rv, rot_z(math.radians(tilt)))
    off = apply(rv, [h * scale for h in handle])
    t = [round(trans[i] + off[i], 2) for i in range(3)]
    s = round(scale * model_scale, 3)
    return {'rotation': to_xyz(r), 'translation': t, 'scale': [s, s, s]}


M = [[-1, 0, 0], [0, 1, 0], [0, 0, 1]]


def to_left_json(r_applied, t_applied, scale):
    """ItemTransform.apply mirrors left-hand entries (x translation, y and z rotation); encode an applied transform."""
    return {'rotation': to_xyz(mul(mul(M, r_applied), M)), 'translation': [round(-t_applied[0], 2), round(t_applied[1], 2), round(t_applied[2], 2)],
            'scale': [scale, scale, scale]}


def from_trident(rot, trans, d):
    """Place our spear exactly where the vanilla trident's own geometry sits. d = trident point minus our point (pixels)."""
    r = xyz(*rot)
    off = apply(r, d)
    return {'rotation': [float(v) for v in rot], 'translation': [round(trans[i] + off[i], 2) for i in range(3)], 'scale': [1, 1, 1]}


def worn_on_arm(left):
    """Third person: put the model on the forearm and hand instead of in the grip. Works backwards through
    ItemInHandLayer (rotate X -90, Y 180, translate) so geo +y runs down the arm to the fingers."""
    q = mul(rot_x(math.radians(-90)), rot_y(math.radians(180)))
    qi = [[q[j][i] for j in range(3)] for i in range(3)]  # rotation inverse = transpose
    layer = [-1 if left else 1, 2, -10]
    centre = [1 if left else -1, 6.8, 0]  # cuff on the forearm, knuckles at the end of the hand (arm y 3 to 10.7)
    t = [a - b for a, b in zip(apply(qi, centre), layer)]
    s = 1.1
    if left:
        # the left gauntlet of a pair faces the other way: turn it half round its own length
        return to_left_json(mul(qi, rot_y(math.radians(180))), t, s)
    return {'rotation': to_xyz(qi), 'translation': [round(v, 2) for v in t], 'scale': [s, s, s]}


SWORD_DISPLAY = {
    'thirdperson_righthand': from_vanilla((0, -90, 55), (0, 4.0, 0.5), 0.85),
    'thirdperson_lefthand': from_vanilla((0, 90, -55), (0, 4.0, 0.5), 0.85, (5.5, -5.5, 0.0), 45.0),
    'firstperson_righthand': from_vanilla((0, -90, 25), (1.13, 3.2, 1.13), 0.68),
    'firstperson_lefthand': from_vanilla((0, 90, -25), (1.13, 3.2, 1.13), 0.68, (5.5, -5.5, 0.0), 45.0),
    'gui': {'rotation': [0, 0, -45], 'translation': [-4.5, -4.5, 0], 'scale': [0.6, 0.6, 0.6]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 0, -45], 'translation': [-4.5, -4.5, 0], 'scale': [0.6, 0.6, 0.6]},
}

# The vanilla bow sprite: its grip sits 3.5 px left of and 2 px above the centre, the limbs run up-right to
# down-left, and it is about 19 px tip to tip against our model's 30 px.
BOW_GRIP, BOW_K = (-3.5, 2.0, 0.0), 19.0 / 30.0
BOW_GRIP_L = (3.5, 2.0, 0.0)

FORM_DISPLAY = {
    'spear': {  # first person where the trident is (its tip lines up with ours)
        'firstperson_righthand': from_trident((0, -90, 25), (-3, 17, 1), (-8, -32.5, -8)),
        'firstperson_lefthand': from_trident((0, 90, -25), (13, 17, 1), (8, -32.5, -8)),
        'gui': {'rotation': [0, 0, -45], 'translation': [-4, -4, 0], 'scale': [0.45, 0.45, 0.45]},
    },
    'bow': {
        'thirdperson_righthand': from_vanilla((-80, 260, -40), (-1, -2, 2.5), 0.9, BOW_GRIP, -45, BOW_K),
        'thirdperson_lefthand': from_vanilla((-80, -280, 40), (-1, -2, 2.5), 0.9, BOW_GRIP_L, 45, BOW_K),
        'firstperson_righthand': from_vanilla((0, -90, 25), (1.13, 3.2, 1.13), 0.68, BOW_GRIP, -45, BOW_K),
        'firstperson_lefthand': from_vanilla((0, 90, -25), (1.13, 3.2, 1.13), 0.68, BOW_GRIP_L, 45, BOW_K),
        'gui': from_vanilla((0, 0, 0), (0, 0, 0), 1.0, BOW_GRIP, -45, BOW_K),
        'ground': from_vanilla((0, 0, 0), (0, 2, 0), 0.5, BOW_GRIP, -45, BOW_K),
        'fixed': from_vanilla((0, 180, 0), (0, 0, 0), 1.0, BOW_GRIP, -45, BOW_K),
    },
    'gauntlets': {  # worn over the forearm and hand; the pair's second gauntlet is in the off hand
        'thirdperson_righthand': worn_on_arm(False),
        'thirdperson_lefthand': worn_on_arm(True),
        'firstperson_righthand': {'rotation': to_xyz(mul(xyz(-80, 0, 10), rot_y(math.radians(180)))), 'translation': [0, 0, 0],
                                  'scale': [0.9, 0.9, 0.9]},
        # tilted toward the centre like the right fist (entries for the left hand are mirrored when applied)
        'firstperson_lefthand': {'rotation': [-80, 0, 10], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]},
        'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]},
    },
}


def form_item_model(form):
    display = dict(SWORD_DISPLAY)
    display.update(FORM_DISPLAY.get(form, {}))
    return {'parent': 'builtin/entity', 'gui_light': 'front', 'display': display}


def weapon_item_model():
    """Base model (longsword positions) plus one override per form, chosen by the edelraids:form item property.
    Overrides must cover every index in ascending order so each form resolves to exactly its own model."""
    model = form_item_model('longsword')
    model['overrides'] = [{'predicate': {MODID + ':form': i + 1}, 'model': MODID + ':item/weapon/' + f[0]}
                          for i, f in enumerate(FORMS)]
    return model


# ------------------------------------------------------------------ lang

LANG = {
    'itemGroup.edelraids': 'Edel Raids: Pledged Blades',
    'entity.edelraids.edel_raid': 'Edel Raid',
    'item.edelraids.edel_weapon': 'Edel Raid Weapon',
    'item.edelraids.edel_weapon.named': "%s's %s",
    'item.edelraids.edel_twin': 'Edel Raid Weapon (Pair)',
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
    'key.categories.edelraids': 'Edel Raids',
    'key.edelraids.sing': 'Sing',
    'key.edelraids.next_song': 'Next Song',
    'hud.edelraids.no_song': 'No song yet',
    'hud.edelraids.song': '%s (%s)',
    'message.edelraids.no_songs': '%s knows no songs yet (needs bond %s).',
    'message.edelraids.need_resonance': 'Not enough resonance (%s needed).',
    'message.edelraids.silenced': 'You are silenced and cannot sing.',
    'message.edelraids.learned_song': '%s learned %s!',
    'message.edelraids.recovered_lost': '%s was lost from your inventory and has reformed beside you.',
    'entity.edelraids.sting_raid': 'Sting Raid',
    'entity.edelraids.hunter': 'Hunter',
    'entity.edelraids.arc_aile_agent': 'Arc Aile Agent',
    'item.edelraids.arc_aile_agent_spawn_egg': 'Arc Aile Agent Spawn Egg',
    'item.edelraids.writ_of_passage': 'Writ of Passage',
    'item.edelraids.agent_sigil': "Agent's Sigil",
    'tooltip.edelraids.writ_of_passage': "Arc Aile's leave to enter Edel Garden, granted in good faith.",
    'tooltip.edelraids.agent_sigil': 'Taken from a fallen Arc Aile agent. It can force the way into Edel Garden.',
    'message.edelraids.agent_confront_1': 'Arc Aile Agent: "Release her. She deserves a life without fighting."',
    'message.edelraids.agent_confront_2': 'Arc Aile Agent: "You call that a bond? Let us take her somewhere safe."',
    'message.edelraids.agent_confront_3': 'Arc Aile Agent: "Edel Raids are not weapons. Stand aside."',
    'message.edelraids.agent_yields': 'The agent yields. Right-click to spare him, or strike to finish him.',
    'message.edelraids.agent_not_yours': 'He yielded to someone else. Sparing him is theirs to give.',
    'message.edelraids.agent_spared': 'You spared the agent. He and his partner withdraw, and Arc Aile will hear of it.',
    'message.edelraids.arc_aile_truce': 'Arc Aile will no longer confront you.',
    'message.edelraids.arc_aile_writ': 'The agent hands you a Writ of Passage before he leaves.',
    'message.edelraids.arc_aile_truce_broken': 'Your truce with Arc Aile is broken.',
    'message.edelraids.partner_grieves': '%s will not forgive you. She returns to Edel Garden alone.',
    'message.edelraids.partner_loyal': '%s stays at her partner\'s side. She chose Arc Aile, and him.',
    'message.edelraids.partner_captured': 'The hunters have overpowered an Arc Aile agent and locked his partner %s in a cage.',
    'message.edelraids.captive_taken': 'Arc Aile has taken %s to Edel Garden.',
    'command.edelraids.standing': 'Your standing with Arc Aile is %s (a truce takes %s).',
    'command.edelraids.standing_truce': 'Your standing with Arc Aile is %s (truce at %s): they will not confront you.',
    'command.edelraids.patrol_spawned': 'An Arc Aile patrol of %s appeared nearby.',
    'command.edelraids.patrol_failed': 'No room for a patrol nearby; try somewhere more open.',
    'entity.edelraids.hunter_marksman': 'Hunter Marksman',
    'item.edelraids.hunter_spawn_egg': 'Hunter Spawn Egg',
    'item.edelraids.hunter_marksman_spawn_egg': 'Hunter Marksman Spawn Egg',
    'message.edelraids.captive_guarded': '%s is still guarded. Deal with the hunters and their Sting Raids nearby first.',
    'message.edelraids.captive_freed': '%s is free! Right-click her again to become her Pledger, or she will leave in a few minutes.',
    'message.edelraids.sting_freed_lingering': 'You freed %s, but you already have %s active Sting Raids. She will wait here for a few minutes: let one of yours rest (sneak + right-click) and right-click her to take her along.',
    'item.edelraids.sting_raid_spawn_egg': 'Sting Raid Spawn Egg',
    'item.edelraids.stone_stabilizer': 'Stone Stabilizer',
    'item.edelraids.refined_stabilizer': 'Refined Stabilizer',
    'tooltip.edelraids.stone_stabilizer': 'Use on a subdued Sting Raid to steady her failing stone.',
    'tooltip.edelraids.refined_stabilizer': 'Use on a freed Sting Raid: her stone strains half as fast from now on.',
    'edelraids.sting_arm.blade': 'arm blade',
    'edelraids.sting_arm.heavy': 'heavy arm',
    'edelraids.sting_arm.launcher': 'launcher arm',
    'edelraids.sting.stabilized': 'stabilized',
    'edelraids.sting.unstabilized': 'not stabilized',
    'message.edelraids.sting_freed': 'You freed %s from the hunters. She will follow you.',
    'message.edelraids.sting_failing': "%s's stone is failing. Use a Stone Stabilizer to steady it.",
    'message.edelraids.sting_revived': "%s's stone steadies, and she gets back up.",
    'message.edelraids.sting_steady': "%s's stone is steady for now.",
    'message.edelraids.sting_stabilized': "%s's stone is refined. It will strain half as fast.",
    'message.edelraids.sting_already_stable': "%s's stone has already been refined.",
    'message.edelraids.sting_limit': 'You already have %s active Sting Raids. Let one rest first (sneak + right-click her).',
    'message.edelraids.sting_freed_resting': 'You freed %s. You already have %s active Sting Raids, so she will rest here until you bring her along (sneak + right-click her).',
    'message.edelraids.sting_resting': '%s will rest around here. Resting Sting Raids do not follow you or count toward your active ones.',
    'message.edelraids.sting_active': '%s will come with you again.',
    'message.edelraids.sting_is_resting': '%s is resting here. Sneak + right-click to bring her along.',
    'edelraids.sting.resting': 'resting',
    'edelraids.sting.active': 'active',
    'message.edelraids.sting_not_yours': '%s follows someone else.',
    'command.edelraids.release_sting_prompt': '%s will leave to find her own way. Run /edelraids release confirm to continue.',
    'command.edelraids.released_sting': '%s thanks you and leaves to find her own way.',
    'command.edelraids.sting_info': '%s: %s Sting Raid, %s, strain %s, %s, %s. Active: %s of %s (%s freed in all).',
    'command.edelraids.sting_created': 'Created %s: %s Sting Raid with an %s, freed by you.',
    'command.edelraids.sting_created_hostile': "Created %s: a hunter's %s Sting Raid with an %s.",
}
# Labels for NeoForge's config screen (tooltips fall back to the comments in the config classes).
CONFIG_LANG = {
    'title': 'Edel Raids Configuration',
    'meter': 'Resonance Meter', 'showMeter': 'Show Meter', 'showSongName': 'Show Song Name',
    'anchor': 'Position', 'orientation': 'Orientation', 'length': 'Bar Length',
    'offsetX': 'Horizontal Offset', 'offsetY': 'Vertical Offset',
    'anchor.hotbar_right': 'Right of Hotbar', 'anchor.hotbar_left': 'Left of Hotbar',
    'anchor.top_left': 'Top Left', 'anchor.top_center': 'Top Centre', 'anchor.top_right': 'Top Right',
    'anchor.middle_left': 'Middle Left', 'anchor.middle_right': 'Middle Right',
    'anchor.bottom_left': 'Bottom Left', 'anchor.bottom_right': 'Bottom Right',
    'orientation.horizontal': 'Horizontal', 'orientation.vertical': 'Vertical',
    'reacting': 'Reacting', 'transformTicks': 'Transform Time (ticks)', 'reactRange': 'React Range',
    'wearPerHit': 'Wear per Hit',
    'companion': 'Companion', 'freedLingerTicks': 'Freed Wait Time (ticks)', 'woundedTicks': 'Wounded Time (ticks)', 'regenIntervalTicks': 'Healing Interval (ticks)',
    'bond': 'Bond', 'bondPerKill': 'Bond per Kill', 'awakenBond': 'Awakening Bond',
    'awakenDamageBonus': 'Awakened Damage Bonus', 'bondCompanionTicks': 'Companionship Interval (ticks)',
    'songs': 'Songs', 'songsEnabled': 'Songs Enabled', 'songBond1': 'First Song Bond', 'songBond2': 'Second Song Bond',
    'resonancePerHit': 'Resonance per Hit', 'resonanceDrainDelayTicks': 'Drain Delay (ticks)',
    'resonanceDrainPerSecond': 'Drain per Second', 'songCooldownTicks': 'Song Cooldown (ticks)',
    'songDamageScale': 'Song Damage Scale',
    'balance': 'Balance', 'qualityDamagePerTier': 'Damage per Quality Tier', 'advantageBonus': 'Advantage Bonus',
    'disadvantagePenalty': 'Disadvantage Penalty',
    'sting_raids': 'Sting Raids', 'stingLimit': 'Active Sting Raid Limit', 'strainPerSecond': 'Strain per Second',
    'stabilizedStrainFactor': 'Refined Strain Multiplier', 'strainRecoveryPerSecond': 'Strain Recovery per Second',
    'staggerTicks': 'Stagger Time (ticks)', 'revertDelayTicks': 'Revert Delay (ticks)',
    'subduedEscapeTicks': 'Escape Time (ticks)', 'stingDamageScale': 'Sting Raid Damage Scale',
    'arc_aile': 'Arc Aile', 'standingPerSpare': 'Standing per Spare', 'standingLostPerKill': 'Standing Lost per Kill',
    'standingPerCaptive': 'Standing per Freed Captive', 'truceStanding': 'Truce Standing', 'yieldTicks': 'Yield Time (ticks)',
    'patrolChancePerMinute': 'Patrol Chance per Minute',
    'technical': 'Technical', 'validateIntervalTicks': 'Validation Interval (ticks)', 'debug': 'Debug Logging',
}
for _k, _v in CONFIG_LANG.items():
    LANG['edelraids.configuration.' + _k] = _v
for _el, _name, *_ in ELEMENTS:
    LANG['edelraids.element.edelraids.' + _el] = _name
for _fid, _name, *_ in FORMS:
    LANG['edelraids.form.edelraids.' + _fid] = _name
for _eid, _name, _ in EFFECTS:
    LANG['effect.edelraids.' + _eid] = _name
for _el, _songs in SONGS.items():
    for _slot, (_name, _shape) in enumerate(_songs, start=1):
        LANG[f'song.edelraids.{_el}_{_slot}'] = _name


FORM_INDEX_JAVA = """package com.tygabeast.edelraids.client;

import com.tygabeast.edelraids.EdelRaids;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * GENERATED by tools/gen.py. Index of each built-in form for the edelraids:form item property, which picks the
 * form's item model (and so its hand positions). Forms added by datapacks get 0 and use the longsword positions.
 */
final class FormIndex {
    private static final String[] FORMS = {
@FORMS@
    };
    private static final Map<ResourceLocation, Float> INDEX = new HashMap<>();

    static {
        for (int i = 0; i < FORMS.length; i++) INDEX.put(EdelRaids.id(FORMS[i]), (float) (i + 1));
    }

    private FormIndex() {}

    static float of(ResourceLocation form) {
        Float f = INDEX.get(form);
        return f == null ? 0f : f;
    }
}
"""


def main():
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'edel_raid.png'), 64, 64, entity_texture())
    write_json(os.path.join(ASSETS, 'geo', 'entity', 'edel_raid.geo.json'), entity_geo())
    write_json(os.path.join(ASSETS, 'animations', 'entity', 'edel_raid.animation.json'), entity_animations())
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'sting_raid.png'), 64, 128, sting_texture())
    write_json(os.path.join(ASSETS, 'geo', 'entity', 'sting_raid.geo.json'), sting_geo())
    write_json(os.path.join(ASSETS, 'animations', 'entity', 'sting_raid.animation.json'), sting_animations())
    for name, refined in (('stone_stabilizer', False), ('refined_stabilizer', True)):
        write_png(os.path.join(ASSETS, 'textures', 'item', name + '.png'), 16, 16, stabilizer_texture(refined))
        write_json(os.path.join(ASSETS, 'models', 'item', name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': MODID + ':item/' + name}})
    write_recipes()
    camp_previews = write_camp()
    write_loot()
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'hunter.png'), 64, 64, hunter_texture((92, 70, 54, 255), False))
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'hunter_marksman.png'), 64, 64, hunter_texture((72, 82, 60, 255), True))
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'arc_aile_agent.png'), 64, 64, agent_texture())
    for name, fn in (('writ_of_passage', writ_texture), ('agent_sigil', sigil_texture)):
        write_png(os.path.join(ASSETS, 'textures', 'item', name + '.png'), 16, 16, fn())
        write_json(os.path.join(ASSETS, 'models', 'item', name + '.json'),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': MODID + ':item/' + name}})
    preview_dir = os.environ.get('EDELRAIDS_PREVIEW_DIR')
    if preview_dir:  # layout data for an optional preview render; never written into the project
        write_json(os.path.join(preview_dir, 'camps.json'), camp_previews)

    write_png(os.path.join(ASSETS, 'textures', 'item', 'weapon', 'longsword.png'), 32, 32, longsword_texture())
    write_json(os.path.join(ASSETS, 'geo', 'item', 'weapon', 'longsword.geo.json'), longsword_geo())
    write_json(os.path.join(ASSETS, 'animations', 'item', 'edel_weapon.animation.json'), weapon_animations())
    for name, parts in FORM_PARTS.items():
        geo, px = pack_form(name, mirror_x(parts) if name in MIRRORED else parts)
        write_json(os.path.join(ASSETS, 'geo', 'item', 'weapon', name + '.geo.json'), geo)
        write_png(os.path.join(ASSETS, 'textures', 'item', 'weapon', name + '.png'), 64, 64, px)
    for eid, _, col in EFFECTS:
        write_png(os.path.join(ASSETS, 'textures', 'mob_effect', eid + '.png'), 18, 18, effect_icon(col))
    write_registries()
    write_songs()

    write_json(os.path.join(ASSETS, 'models', 'item', 'edel_weapon.json'), weapon_item_model())
    write_json(os.path.join(ASSETS, 'models', 'item', 'edel_twin.json'), weapon_item_model())
    for f in FORMS:
        write_json(os.path.join(ASSETS, 'models', 'item', 'weapon', f[0] + '.json'), form_item_model(f[0]))
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'java', 'com', 'tygabeast',
                           'edelraids', 'client', 'FormIndex.java'), 'w', newline='\n') as fj:
        fj.write(FORM_INDEX_JAVA.replace('@FORMS@', ',\n'.join('            "' + f[0] + '"' for f in FORMS)))
    write_json(os.path.join(ASSETS, 'models', 'item', 'edel_raid_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
    for egg in ('sting_raid', 'hunter', 'hunter_marksman', 'arc_aile_agent'):
        write_json(os.path.join(ASSETS, 'models', 'item', egg + '_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
    write_json(os.path.join(ASSETS, 'lang', 'en_us.json'), dict(sorted(LANG.items())))

    for tag in ('sword', 'sharp_weapon', 'weapon', 'fire_aspect', 'durability', 'bow'):
        write_json(os.path.join(DATA, 'minecraft', 'tags', 'item', 'enchantable', tag + '.json'),
                   {'replace': False, 'values': [MODID + ':edel_weapon']})
    print('generated resources in', os.path.normpath(ROOT))


if __name__ == '__main__':
    main()
