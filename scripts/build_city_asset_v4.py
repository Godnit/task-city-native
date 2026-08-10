from pathlib import Path
import math
import sys

if len(sys.argv) != 3:
    raise SystemExit('usage: build_city_asset_v4.py <templates.obj> <output.obj>')

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
        p = line.split(); verts.append(tuple(map(float, p[1:4])))
    elif line.startswith('vt '):
        p = line.split(); uvs.append(tuple(map(float, p[1:3])))
    elif line.startswith('g '):
        cur = line[2:].strip(); groups[cur] = []
    elif line.startswith('usemtl '):
        mat = line.split(None, 1)[1]
    elif line.startswith('f ') and cur:
        groups[cur].append((mat, line.split()[1:]))

required = [f'TEMPLATE_HOUSE_{i:02d}' for i in range(1, 7)]
missing = [x for x in required if x not in groups]
if missing:
    raise SystemExit('missing house templates: ' + ', '.join(missing))

out = ['# Task City v4 fixed-view suburban neighborhood', 'mtllib scene.mtl']
vcount = 0
tcount = 0


def rot2(x, z, deg):
    a = math.radians(deg); c = math.cos(a); s = math.sin(a)
    return x*c-z*s, x*s+z*c


def box(group, material, cx, cy, cz, sx, sy, sz, rot=0.0):
    global vcount
    raw = [
        (-sx/2,cy-sy/2,-sz/2),(sx/2,cy-sy/2,-sz/2),(sx/2,cy-sy/2,sz/2),(-sx/2,cy-sy/2,sz/2),
        (-sx/2,cy+sy/2,-sz/2),(sx/2,cy+sy/2,-sz/2),(sx/2,cy+sy/2,sz/2),(-sx/2,cy+sy/2,sz/2)
    ]
    vs=[]
    for x,y,z in raw:
        xr,zr=rot2(x,z,rot); vs.append((xr+cx,y,zr+cz))
    faces=[(1,2,3),(1,3,4),(5,7,6),(5,8,7),(1,5,6),(1,6,2),(2,6,7),(2,7,3),(3,7,8),(3,8,4),(4,8,5),(4,5,1)]
    out.extend([f'g {group}', f'usemtl {material}'])
    out.extend(f'v {x:.3f} {y:.3f} {z:.3f}' for x,y,z in vs)
    out.extend(f'f {vcount+a} {vcount+b} {vcount+c}' for a,b,c in faces)
    vcount += 8


def octa(group, material, cx, cy, cz, rx, ry, rz):
    global vcount
    vs=[(cx,cy+ry,cz),(cx+rx,cy,cz),(cx,cy,cz+rz),(cx-rx,cy,cz),(cx,cy,cz-rz),(cx,cy-ry,cz)]
    fs=[(1,2,3),(1,3,4),(1,4,5),(1,5,2),(6,3,2),(6,4,3),(6,5,4),(6,2,5)]
    out.extend([f'g {group}', f'usemtl {material}'])
    out.extend(f'v {x:.3f} {y:.3f} {z:.3f}' for x,y,z in vs)
    out.extend(f'f {vcount+a} {vcount+b} {vcount+c}' for a,b,c in fs)
    vcount += 6


def road_segment(name, x1, z1, x2, z2, width=2.35):
    dx=x2-x1; dz=z2-z1; length=math.hypot(dx,dz)+0.35
    angle=math.degrees(math.atan2(dz,dx)); cx=(x1+x2)/2; cz=(z1+z2)/2
    box(name+'_CURB','sidewalk',cx,0.015,cz,length,0.08,width+1.10,angle)
    box(name+'_ROAD','road',cx,0.070,cz,length,0.075,width,angle)


def path_segments(prefix, pts, width=2.35):
    for i in range(len(pts)-1):
        road_segment(f'{prefix}_{i:02d}', *pts[i], *pts[i+1], width)


def template_bounds(name):
    ids=[]
    for _, toks in groups[name]: ids += [int(t.split('/')[0]) for t in toks]
    ps=[verts[i] for i in set(ids)]
    return min(p[0] for p in ps),max(p[0] for p in ps),min(p[1] for p in ps),max(p[1] for p in ps),min(p[2] for p in ps),max(p[2] for p in ps)


def face_normal(ps):
    if len(ps)<3: return (0,1,0)
    a=(ps[1][0]-ps[0][0],ps[1][1]-ps[0][1],ps[1][2]-ps[0][2]); b=(ps[2][0]-ps[0][0],ps[2][1]-ps[0][1],ps[2][2]-ps[0][2])
    n=(a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]); L=math.sqrt(sum(x*x for x in n)) or 1
    return tuple(x/L for x in n)


def house_part(template, material, toks):
    if 'window' in material.lower(): return 'DETAIL'
    b=template_bounds(template); ymin,ymax=b[2],b[3]
    ps=[verts[int(t.split('/')[0])] for t in toks]
    cy=sum(p[1] for p in ps)/len(ps); rel=(cy-ymin)/max(0.001,ymax-ymin); ny=abs(face_normal(ps)[1])
    return 'ROOF' if ((rel>0.48 and ny>0.20) or rel>0.79) else 'WALL'


def instance_house(owner, template, tx, tz, scale=0.80, rot=0.0):
    global vcount,tcount
    faces=groups[template]; vis=[]; tis=[]
    for _,toks in faces:
        for tok in toks:
            a=tok.split('/'); vis.append(int(a[0]));
            if len(a)>1 and a[1]: tis.append(int(a[1]))
    vv=sorted(set(vis)); tt=sorted(set(tis)); vm={o:i+1 for i,o in enumerate(vv)}; tm={o:i+1 for i,o in enumerate(tt)}
    ca=math.cos(math.radians(rot)); sa=math.sin(math.radians(rot))
    for old in vv:
        x,y,z=verts[old]; x*=scale; y*=scale; z*=scale
        xr=x*ca-z*sa+tx; zr=x*sa+z*ca+tz; out.append(f'v {xr:.3f} {y:.3f} {zr:.3f}')
    for old in tt:
        u,v=uvs[old]; out.append(f'vt {u:.4f} {v:.4f}')
    current=None
    for material,toks in faces:
        part=house_part(template,material,toks); target=(part,'house_'+part.lower())
        if target!=current:
            out.extend([f'g {owner}_{part}',f'usemtl {target[1]}']); current=target
        nt=[]
        for tok in toks:
            a=tok.split('/'); vi=int(a[0]); ti=int(a[1]) if len(a)>1 and a[1] else 0
            nt.append(f'{vcount+vm[vi]}/{tcount+tm[ti]}' if ti else str(vcount+vm[vi]))
        out.append('f '+' '.join(nt))
    vcount+=len(vv); tcount+=len(tt)


def tree(prefix,x,z,scale=1.0):
    # Fuller Clash-like cartoon tree: brown trunk + three overlapping green crowns.
    box(prefix+'_TRUNK','tree_trunk',x,0.55*scale,z,0.32*scale,1.10*scale,0.32*scale)
    octa(prefix+'_LEAF_A','tree_leaf',x,1.55*scale,z,0.95*scale,0.78*scale,0.95*scale)
    octa(prefix+'_LEAF_B','tree_leaf2',x-0.45*scale,1.45*scale,z+0.08*scale,0.70*scale,0.58*scale,0.70*scale)
    octa(prefix+'_LEAF_C','tree_leaf2',x+0.43*scale,1.43*scale,z-0.05*scale,0.68*scale,0.56*scale,0.68*scale)


def hedge(group,x,z,length,rot=0):
    box(group,'hedge',x,0.28,z,length,0.52,0.42,rot)


def fence(group,x,z,length,rot=0):
    # lightweight white fence rail with posts implied by the thicker profile
    box(group,'fence',x,0.33,z,length,0.42,0.14,rot)


def yard(owner,x,z,seed):
    # These groups start HOUSE_ so they stay invisible until that specific house is earned.
    # No empty lot or marker is visible before construction.
    hedge(owner+'_HEDGE_A',x-1.55,z+1.65,2.6,0)
    hedge(owner+'_HEDGE_B',x+1.55,z+1.65,2.0,0)
    fence(owner+'_FENCE_A',x,z-1.65,3.8,0)
    box(owner+'_PATH','path',x,0.04,z-1.0,0.72,0.06,1.45,0)
    # two compact garden bushes
    octa(owner+'_GARDEN_A','flower',x-1.25,0.28,z-1.10,0.34,0.28,0.34)
    octa(owner+'_GARDEN_B','shrub',x+1.25,0.30,z-1.05,0.38,0.30,0.38)


# Wide grassy world so horizontal panning feels useful. No visible future-house lots.
box('SCENERY_GRASS_WORLD','grass',0,-0.34,0,56.0,0.62,27.0)

# Curved-looking roads approximated by short joined segments, much less grid-like than before.
path_segments('SCENERY_MAIN_SOUTH',[(-28,-7.2),(-21,-7.4),(-14,-6.8),(-7,-5.7),(0,-5.4),(7,-5.8),(14,-6.8),(21,-7.4),(28,-7.0)],2.45)
path_segments('SCENERY_MAIN_NORTH',[(-27,6.8),(-20,6.3),(-13,5.7),(-6,5.9),(1,6.5),(8,6.4),(15,5.8),(22,6.2),(28,7.0)],2.35)
path_segments('SCENERY_LINK_W',[(-17,-7.0),(-16,-1.0),(-17,6.0)],2.25)
path_segments('SCENERY_LINK_C',[(-2,-5.5),(-1.0,0.2),(1,6.4)],2.25)
path_segments('SCENERY_LINK_E',[(15,-6.8),(16,-0.4),(15,5.8)],2.25)

# Twenty potential homes spread across the neighborhood. All share one coherent facing direction.
slots=[
 (-24,-11.0),(-18,-11.2),(-10.5,-10.3),(-4.6,-10.4),(4,-10.2),(10.5,-10.5),(18,-11.0),(24,-10.8),
 (-23,1.0),(-10.8,0.2),(6.0,0.1),(22.5,0.7),
 (-24,11.0),(-18,10.6),(-10.5,10.4),(-4,10.8),(4.5,11.2),(11,10.4),(18,10.7),(24,11.0)
]
templates=[f'TEMPLATE_HOUSE_{((i*5)%6)+1:02d}' for i in range(len(slots))]
scales=[0.80,0.76,0.82,0.78,0.80,0.75,0.81,0.77,0.78,0.79,0.76,0.80,0.82,0.77,0.79,0.76,0.81,0.78,0.80,0.75]
for i,((x,z),template,size) in enumerate(zip(slots,templates,scales),1):
    owner=f'HOUSE_{i:02d}'
    instance_house(owner,template,x,z,size,0.0)
    yard(owner,x,z,i)

# Dense but tidy greenery: larger trees near edges, smaller trees beside roads and central gardens.
tree_points=[
 (-27,-12),(-26,-3),(-27,4),(-26,12),(27,-12),(26,-3),(27,4),(26,12),
 (-21,-3.5),(-20,3.4),(-13,-3.0),(-12,2.6),(-7,-2.3),(-5,2.7),(4,-2.5),(7,2.5),(12,-3.1),(13,2.8),(20,-3.4),(21,3.5),
 (-15,-12.5),(-1,-12.4),(14,-12.3),(-15,12.3),(0,12.5),(14,12.2)
]
for i,(x,z) in enumerate(tree_points): tree(f'SCENERY_TREE_{i:02d}',x,z,0.82+(i%3)*0.09)

# Hedges/shrubs soften road edges without showing future house slots.
for i,(x,z,r,l) in enumerate([
 (-23,-3.8,0,3.2),(-11,-3.4,0,2.8),(8,-3.4,0,3.0),(22,-3.8,0,3.4),
 (-24,3.7,0,3.3),(-9,3.3,0,3.0),(8,3.5,0,3.1),(23,3.8,0,3.2)
]): hedge(f'SCENERY_HEDGE_{i:02d}',x,z,l,r)

# Simple lamps along the two main roads add depth and make the scene feel deliberately lit.
for i,(x,z) in enumerate([(-20,-8.8),(-8,-7.5),(6,-7.4),(20,-8.8),(-20,8.0),(-7,7.7),(7,8.0),(20,8.3)]):
    box(f'SCENERY_LAMP_{i:02d}_POLE','lamp_pole',x,0.85,z,0.12,1.70,0.12)
    octa(f'SCENERY_LAMP_{i:02d}_LIGHT','lamp',x,1.78,z,0.22,0.18,0.22)

OUT.parent.mkdir(parents=True,exist_ok=True)
OUT.write_text('\n'.join(out)+'\n')
print(f'TASK_CITY_V4_ASSET_OK houses={len(slots)} trees={len(tree_points)} bytes={OUT.stat().st_size}')
