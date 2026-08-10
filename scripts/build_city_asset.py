from pathlib import Path
import math
import sys

if len(sys.argv) != 3:
    raise SystemExit('usage: build_city_asset.py <templates.obj> <output.obj>')

SRC = Path(sys.argv[1])
OUT = Path(sys.argv[2])
lines = SRC.read_text().splitlines()
verts = [None]
uvs = [None]
groups = {}
cur = None
mat = 'palette'

for line in lines:
    if line.startswith('v '):
        p = line.split()
        verts.append(tuple(map(float, p[1:4])))
    elif line.startswith('vt '):
        p = line.split()
        uvs.append(tuple(map(float, p[1:3])))
    elif line.startswith('g '):
        cur = line[2:].strip()
        groups[cur] = []
    elif line.startswith('usemtl '):
        mat = line.split(None, 1)[1]
    elif line.startswith('f ') and cur:
        groups[cur].append((mat, line.split()[1:]))

required = [f'TEMPLATE_HOUSE_{i:02d}' for i in range(1, 7)] + ['TEMPLATE_SCENERY_TREE_00']
missing = [name for name in required if name not in groups]
if missing:
    raise SystemExit('missing templates: ' + ', '.join(missing))

out = ['# Task City curated neighborhood', 'mtllib scene.mtl']
vcount = 0
tcount = 0

def box(group, material, cx, cy, cz, sx, sy, sz):
    global vcount
    x0, x1 = cx - sx / 2, cx + sx / 2
    y0, y1 = cy - sy / 2, cy + sy / 2
    z0, z1 = cz - sz / 2, cz + sz / 2
    vs = [
        (x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1),
        (x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)
    ]
    fs = [
        (1,2,3),(1,3,4),(5,7,6),(5,8,7),(1,5,6),(1,6,2),
        (2,6,7),(2,7,3),(3,7,8),(3,8,4),(4,8,5),(4,5,1)
    ]
    out.extend([f'g {group}', f'usemtl {material}'])
    for x,y,z in vs:
        out.append(f'v {x:.3f} {y:.3f} {z:.3f}')
    for a,b,c in fs:
        out.append(f'f {vcount+a} {vcount+b} {vcount+c}')
    vcount += 8

def instance(group_name, template_name, tx, tz, rot=0.0, scale=1.0):
    global vcount, tcount
    faces = groups[template_name]
    vis, tis = [], []
    for _, toks in faces:
        for tok in toks:
            a = tok.split('/')
            vis.append(int(a[0]))
            if len(a) > 1 and a[1]:
                tis.append(int(a[1]))
    vv = sorted(set(vis))
    tt = sorted(set(tis))
    vmap = {old: i + 1 for i, old in enumerate(vv)}
    tmap = {old: i + 1 for i, old in enumerate(tt)}
    ca = math.cos(math.radians(rot))
    sa = math.sin(math.radians(rot))
    out.append(f'g {group_name}')
    for old in vv:
        x,y,z = verts[old]
        x *= scale; y *= scale; z *= scale
        xr = x * ca - z * sa + tx
        zr = x * sa + z * ca + tz
        out.append(f'v {xr:.3f} {y:.3f} {zr:.3f}')
    for old in tt:
        u,v = uvs[old]
        out.append(f'vt {u:.4f} {v:.4f}')
    current_material = None
    for material, toks in faces:
        if material != current_material:
            out.append('usemtl ' + material)
            current_material = material
        new_tokens = []
        for tok in toks:
            a = tok.split('/')
            vi = int(a[0])
            ti = int(a[1]) if len(a) > 1 and a[1] else 0
            if ti:
                new_tokens.append(f'{vcount + vmap[vi]}/{tcount + tmap[ti]}')
            else:
                new_tokens.append(str(vcount + vmap[vi]))
        out.append('f ' + ' '.join(new_tokens))
    vcount += len(vv)
    tcount += len(tt)

# Friendly residential block: green base, three vertical roads, two cross streets,
# sidewalks, and twelve individual house lots.
box('SCENERY_GRASS_BASE','grass',0,-0.13,0,25.5,0.20,22.0)
for i,x in enumerate([-5.6, 0.0, 5.6]):
    box(f'SCENERY_ROAD_V{i}','road',x,0.015,0,2.15,0.055,22.2)
    box(f'SCENERY_SIDEWALK_VL{i}','sidewalk',x-1.27,0.03,0,0.38,0.065,22.2)
    box(f'SCENERY_SIDEWALK_VR{i}','sidewalk',x+1.27,0.03,0,0.38,0.065,22.2)
for i,z in enumerate([-3.65, 3.65]):
    box(f'SCENERY_ROAD_H{i}','road',0,0.02,z,25.7,0.06,2.15)
    box(f'SCENERY_SIDEWALK_HB{i}','sidewalk',0,0.035,z-1.27,25.7,0.07,0.38)
    box(f'SCENERY_SIDEWALK_HT{i}','sidewalk',0,0.035,z+1.27,25.7,0.07,0.38)

slots = [
    (-8.4,-7.2),(-2.8,-7.2),(2.8,-7.2),(8.4,-7.2),
    (-8.4,0),(-2.8,0),(2.8,0),(8.4,0),
    (-8.4,7.2),(-2.8,7.2),(2.8,7.2),(8.4,7.2)
]
rotations = [0,7,-5,4,-7,3,6,-4,5,-6,2,-3]
for i,(x,z) in enumerate(slots, 1):
    box(f'SCENERY_LOT_{i:02d}','lotgrass',x,0.015,z,4.45,0.04,4.55)
    template = f'TEMPLATE_HOUSE_{((i-1) % 6) + 1:02d}'
    size = 0.88 if i in (2,7,10) else 0.95
    instance(f'HOUSE_{i:02d}', template, x, z, rotations[i-1], size)

# Asset-pack tree reused at varied rotations/scales around the neighborhood.
tree_points = [
    (-11.1,-9.2),(-10.8,-5.1),(-10.9,-1.7),(-10.9,2.3),(-10.8,5.2),(-11.1,9.2),
    (11.1,-9.1),(10.8,-5.0),(10.9,-1.7),(10.9,2.3),(10.8,5.1),(11.1,9.2),
    (-8.7,-9.3),(-3.0,-9.3),(3.0,-9.3),(8.7,-9.3),
    (-8.7,9.3),(-3.0,9.3),(3.0,9.3),(8.7,9.3),
    (-4.3,-1.8),(4.3,-1.8),(-4.3,5.4),(4.3,5.4)
]
for i,(x,z) in enumerate(tree_points):
    instance(
        f'SCENERY_TREE_{i:02d}',
        'TEMPLATE_SCENERY_TREE_00',
        x, z,
        (i * 37) % 360,
        0.85 + (i % 3) * 0.08
    )

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text('\n'.join(out) + '\n')
print(f'TASK_CITY_ASSET_OK houses=12 trees={len(tree_points)} bytes={OUT.stat().st_size}')
