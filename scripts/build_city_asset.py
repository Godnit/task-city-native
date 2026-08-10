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

out = ['# Task City bright suburban neighborhood', 'mtllib scene.mtl']
vcount = 0
tcount = 0

def rotate_point(x, z, deg):
    ca = math.cos(math.radians(deg))
    sa = math.sin(math.radians(deg))
    return x * ca - z * sa, x * sa + z * ca

def box(group, material, cx, cy, cz, sx, sy, sz, rot=0.0):
    global vcount
    x0, x1 = -sx / 2, sx / 2
    y0, y1 = cy - sy / 2, cy + sy / 2
    z0, z1 = -sz / 2, sz / 2
    raw = [
        (x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1),
        (x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)
    ]
    vs = []
    for x,y,z in raw:
        xr,zr = rotate_point(x,z,rot)
        vs.append((xr+cx,y,zr+cz))
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

def pyramid(group, material, cx, cy, cz, radius, height, sides=8):
    global vcount
    out.extend([f'g {group}', f'usemtl {material}'])
    base = []
    for i in range(sides):
        a = (2*math.pi*i/sides) + 0.17
        base.append((cx + math.cos(a)*radius, cy, cz + math.sin(a)*radius))
    for p in base:
        out.append(f'v {p[0]:.3f} {p[1]:.3f} {p[2]:.3f}')
    out.append(f'v {cx:.3f} {cy+height:.3f} {cz:.3f}')
    top = vcount + sides + 1
    for i in range(sides):
        a = vcount + i + 1
        b = vcount + ((i+1) % sides) + 1
        out.append(f'f {a} {b} {top}')
    # bottom fan is unnecessary from the camera and saves triangles
    vcount += sides + 1

def add_road(group, cx, cz, length, rot=0.0, width=2.05):
    # Sidewalk is a continuous warm border under the asphalt; no square house lots.
    box(group+'_SIDEWALK', 'sidewalk', cx, 0.025, cz, length, 0.075, width+0.95, rot)
    box(group+'_ASPHALT', 'road', cx, 0.080, cz, length, 0.070, width, rot)

def face_points(tokens):
    pts=[]
    for tok in tokens:
        vi=int(tok.split('/')[0])
        pts.append(verts[vi])
    return pts

def template_bounds(template_name):
    ids=[]
    for _, toks in groups[template_name]:
        ids.extend(int(t.split('/')[0]) for t in toks)
    ps=[verts[i] for i in set(ids)]
    return (
        min(p[0] for p in ps), max(p[0] for p in ps),
        min(p[1] for p in ps), max(p[1] for p in ps),
        min(p[2] for p in ps), max(p[2] for p in ps)
    )

def face_normal(ps):
    if len(ps)<3: return (0.0,1.0,0.0)
    a=(ps[1][0]-ps[0][0],ps[1][1]-ps[0][1],ps[1][2]-ps[0][2])
    b=(ps[2][0]-ps[0][0],ps[2][1]-ps[0][1],ps[2][2]-ps[0][2])
    n=(a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
    l=math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]) or 1.0
    return (n[0]/l,n[1]/l,n[2]/l)

def house_part(template_name, material, toks):
    # The pack's windows/detail mesh is separate. For the main mesh, classify the upper
    # sloped/horizontal surfaces as roof and the vertical/lower surfaces as wall.
    if 'window' in material.lower():
        return 'DETAIL'
    b=template_bounds(template_name)
    ymin,ymax=b[2],b[3]
    ps=face_points(toks)
    cy=sum(p[1] for p in ps)/len(ps)
    rel=(cy-ymin)/max(0.001,ymax-ymin)
    ny=abs(face_normal(ps)[1])
    if (rel > 0.48 and ny > 0.20) or rel > 0.79:
        return 'ROOF'
    return 'WALL'

def tree_part(template_name, toks):
    b=template_bounds(template_name)
    xmin,xmax,ymin,ymax,zmin,zmax=b
    ps=face_points(toks)
    cx=(xmin+xmax)/2; cz=(zmin+zmax)/2
    fx=sum(p[0] for p in ps)/len(ps)
    fy=sum(p[1] for p in ps)/len(ps)
    fz=sum(p[2] for p in ps)/len(ps)
    rel=(fy-ymin)/max(0.001,ymax-ymin)
    radial=math.hypot(fx-cx,fz-cz)/max(0.001,max(xmax-xmin,zmax-zmin))
    return 'TRUNK' if rel < 0.47 and radial < 0.16 else 'LEAF'

def instance_house(group_name, template_name, tx, tz, rot=0.0, scale=1.0):
    global vcount, tcount
    faces = groups[template_name]
    vis, tis = [], []
    for _, toks in faces:
        for tok in toks:
            a = tok.split('/')
            vis.append(int(a[0]))
            if len(a) > 1 and a[1]: tis.append(int(a[1]))
    vv=sorted(set(vis)); tt=sorted(set(tis))
    vmap={old:i+1 for i,old in enumerate(vv)}; tmap={old:i+1 for i,old in enumerate(tt)}
    ca=math.cos(math.radians(rot)); sa=math.sin(math.radians(rot))
    # Keep the original shape from the uploaded Mega Pack; only transform its placement.
    for old in vv:
        x,y,z=verts[old]; x*=scale; y*=scale; z*=scale
        xr=x*ca-z*sa+tx; zr=x*sa+z*ca+tz
        out.append(f'v {xr:.3f} {y:.3f} {zr:.3f}')
    for old in tt:
        u,v=uvs[old]; out.append(f'vt {u:.4f} {v:.4f}')
    current=None
    for material,toks in faces:
        part=house_part(template_name,material,toks)
        target=(part,'house_'+part.lower())
        if target != current:
            out.append(f'g {group_name}_{part}')
            out.append('usemtl '+target[1])
            current=target
        nt=[]
        for tok in toks:
            a=tok.split('/'); vi=int(a[0]); ti=int(a[1]) if len(a)>1 and a[1] else 0
            nt.append(f'{vcount+vmap[vi]}/{tcount+tmap[ti]}' if ti else str(vcount+vmap[vi]))
        out.append('f '+' '.join(nt))
    vcount += len(vv); tcount += len(tt)

def instance_tree(group_name, template_name, tx, tz, rot=0.0, scale=1.0):
    global vcount, tcount
    faces=groups[template_name]
    vis=[]; tis=[]
    for _,toks in faces:
        for tok in toks:
            a=tok.split('/'); vis.append(int(a[0]))
            if len(a)>1 and a[1]: tis.append(int(a[1]))
    vv=sorted(set(vis)); tt=sorted(set(tis)); vmap={o:i+1 for i,o in enumerate(vv)}; tmap={o:i+1 for i,o in enumerate(tt)}
    ca=math.cos(math.radians(rot)); sa=math.sin(math.radians(rot))
    for old in vv:
        x,y,z=verts[old]; x*=scale; y*=scale; z*=scale
        xr=x*ca-z*sa+tx; zr=x*sa+z*ca+tz
        out.append(f'v {xr:.3f} {y:.3f} {zr:.3f}')
    for old in tt:
        u,v=uvs[old]; out.append(f'vt {u:.4f} {v:.4f}')
    current=None
    for _,toks in faces:
        part=tree_part(template_name,toks)
        if part!=current:
            out.append(f'g {group_name}_{part}')
            out.append('usemtl '+('tree_trunk' if part=='TRUNK' else 'tree_leaf'))
            current=part
        nt=[]
        for tok in toks:
            a=tok.split('/'); vi=int(a[0]); ti=int(a[1]) if len(a)>1 and a[1] else 0
            nt.append(f'{vcount+vmap[vi]}/{tcount+tmap[ti]}' if ti else str(vcount+vmap[vi]))
        out.append('f '+' '.join(nt))
    vcount+=len(vv); tcount+=len(tt)

# Continuous Clash-like grassy island. No pre-highlighted lots or squares for future houses.
box('SCENERY_GRASS_ISLAND','grass',0,-0.28,0,30.0,0.55,25.5)

# A softer residential road layout with breathing room instead of the old dense checkerboard.
add_road('SCENERY_ROAD_SOUTH', 0.0, -6.0, 29.0, 0)
add_road('SCENERY_ROAD_NORTH', -1.0, 5.3, 27.0, 0)
add_road('SCENERY_ROAD_WEST', -6.4, 0.0, 20.0, 90)
add_road('SCENERY_ROAD_EAST', 7.3, -0.2, 19.0, 90)
# Short angled entries soften the purely rectangular feel.
add_road('SCENERY_ROAD_SWING_A', 11.2, 8.4, 9.0, 18)
add_road('SCENERY_ROAD_SWING_B', -10.9, -9.0, 8.0, -16)

# House positions are intentionally invisible until a task is completed. There are NO lot meshes.
slots=[
    (-11.3,-10.2,8),(-2.3,-10.3,-5),(4.0,-10.1,6),(11.6,-9.7,-8),
    (-11.7,-1.1,-4),(-1.3,-0.8,5),(12.0,-0.5,-6),
    (-11.4,10.1,7),(-2.4,10.0,-5),(5.1,9.6,4),(12.2,11.0,-12),
    (1.7,2.0,3)
]
# The first four are the smaller/cozier templates because existing users may already have 4 houses.
templates=['TEMPLATE_HOUSE_04','TEMPLATE_HOUSE_01','TEMPLATE_HOUSE_05','TEMPLATE_HOUSE_02',
           'TEMPLATE_HOUSE_06','TEMPLATE_HOUSE_03','TEMPLATE_HOUSE_04','TEMPLATE_HOUSE_05',
           'TEMPLATE_HOUSE_01','TEMPLATE_HOUSE_06','TEMPLATE_HOUSE_02','TEMPLATE_HOUSE_03']
scales=[0.82,0.79,0.83,0.80,0.77,0.76,0.80,0.78,0.81,0.75,0.78,0.74]
for i,((x,z,r),template,size) in enumerate(zip(slots,templates,scales),1):
    instance_house(f'HOUSE_{i:02d}',template,x,z,r,size)

# Green scenery around the island, road bends and open spaces. These do not mark house locations.
tree_points=[
    (-14.0,-11.3),(-13.4,-7.3),(-14.0,-2.7),(-13.4,2.4),(-14.1,7.0),(-13.6,11.4),
    (14.0,-11.2),(13.6,-7.3),(14.1,-2.8),(13.7,2.5),(14.1,7.0),(13.8,11.6),
    (-9.0,-12.0),(7.8,-12.0),(-8.8,12.0),(8.0,12.1),
    (-8.8,2.2),(-4.2,2.1),(4.4,2.2),(10.2,2.4),
    (-9.7,-3.5),(10.5,-3.1),(-5.0,8.2),(8.9,7.6)
]
for i,(x,z) in enumerate(tree_points):
    instance_tree(f'SCENERY_TREE_{i:02d}','TEMPLATE_SCENERY_TREE_00',x,z,(i*41)%360,0.75+(i%4)*0.06)

# Low-poly green shrubs make the open grass feel alive without revealing where future houses will spawn.
shrub_points=[(-12,4),(-10,4.4),(-4,-3.4),(2,-3.3),(10,3.5),(12,3.6),(-4,7.5),(2.2,7.8),(9,-7.7),(-1,12.0)]
for i,(x,z) in enumerate(shrub_points):
    pyramid(f'SCENERY_SHRUB_{i:02d}','shrub',x,0.05,z,0.48+(i%3)*0.08,0.65+(i%2)*0.10,8)

OUT.parent.mkdir(parents=True,exist_ok=True)
OUT.write_text('\n'.join(out)+'\n')
print(f'TASK_CITY_ASSET_OK houses={len(slots)} trees={len(tree_points)} shrubs={len(shrub_points)} bytes={OUT.stat().st_size}')
