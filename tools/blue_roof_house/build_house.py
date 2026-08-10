import bpy
import math
import os
import sys
import random
from mathutils import Vector

random.seed(7)

out_dir = os.path.abspath('output')
if '--' in sys.argv:
    argv = sys.argv[sys.argv.index('--') + 1:]
    if '--output' in argv:
        i = argv.index('--output')
        if i + 1 < len(argv):
            out_dir = os.path.abspath(argv[i + 1])
os.makedirs(out_dir, exist_ok=True)

bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)
scene = bpy.context.scene
scene.unit_settings.system = 'METRIC'
scene.unit_settings.scale_length = 1.0

def mat(name, color, roughness=0.55, metallic=0.0):
    m = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get('Principled BSDF')
    if bsdf:
        bsdf.inputs['Base Color'].default_value = (*color, 1.0)
        bsdf.inputs['Roughness'].default_value = roughness
        bsdf.inputs['Metallic'].default_value = metallic
    return m

M = {
    'wall': mat('Warm White Siding', (0.88, 0.86, 0.79), 0.72),
    'trim': mat('White Painted Trim', (0.96, 0.96, 0.93), 0.55),
    'blue': mat('Roof Blue', (0.035, 0.19, 0.43), 0.5),
    'blue2': mat('Roof Blue Mid', (0.045, 0.24, 0.53), 0.48),
    'blue3': mat('Roof Blue Light', (0.06, 0.29, 0.62), 0.46),
    'wood': mat('Warm Wood', (0.38, 0.16, 0.065), 0.6),
    'wood2': mat('Light Wood', (0.58, 0.31, 0.12), 0.58),
    'glass': mat('Window Glass', (0.10, 0.28, 0.32), 0.2),
    'stone': mat('Path Stone', (0.68, 0.64, 0.56), 0.78),
    'brick': mat('Chimney Brick', (0.63, 0.37, 0.17), 0.75),
    'brick_light': mat('Chimney Mortar', (0.84, 0.66, 0.44), 0.75),
    'grass': mat('Fresh Grass', (0.24, 0.47, 0.08), 0.9),
    'soil': mat('Soil Edge', (0.30, 0.19, 0.10), 0.95),
    'hedge': mat('Hedge Green', (0.16, 0.36, 0.045), 0.95),
    'leaf': mat('Tree Leaf', (0.23, 0.49, 0.065), 0.95),
    'leaf2': mat('Tree Leaf Light', (0.36, 0.61, 0.10), 0.95),
    'trunk': mat('Tree Trunk', (0.27, 0.12, 0.045), 0.9),
    'white': mat('Fence White', (0.94, 0.94, 0.90), 0.6),
    'pink': mat('Flower Pink', (0.9, 0.18, 0.35), 0.6),
    'yellow': mat('Flower Yellow', (1.0, 0.60, 0.06), 0.55),
    'purple': mat('Flower Purple', (0.50, 0.20, 0.70), 0.6),
}

def assign(obj, material):
    obj.data.materials.append(material)
    return obj

def box(name, loc, scale, material, rot=(0, 0, 0), bevel=0.0):
    bpy.ops.mesh.primitive_cube_add(location=loc, rotation=rot)
    o = bpy.context.object
    o.name = name
    o.dimensions = scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    assign(o, material)
    if bevel > 0:
        mod = o.modifiers.new('Soft edges', 'BEVEL')
        mod.width = bevel
        mod.segments = 2
    return o

def cyl(name, loc, radius, depth, material, vertices=16, rot=(0, 0, 0)):
    bpy.ops.mesh.primitive_cylinder_add(vertices=vertices, radius=radius, depth=depth, location=loc, rotation=rot)
    o = bpy.context.object
    o.name = name
    assign(o, material)
    return o

def sphere(name, loc, scale, material, subdivisions=2):
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=subdivisions, radius=1.0, location=loc)
    o = bpy.context.object
    o.name = name
    o.scale = scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    assign(o, material)
    return o

def gable(name, y, width, eave_z, rise, thickness, material):
    verts = [
        (-width/2, y-thickness/2, eave_z), (width/2, y-thickness/2, eave_z), (0, y-thickness/2, eave_z+rise),
        (-width/2, y+thickness/2, eave_z), (width/2, y+thickness/2, eave_z), (0, y+thickness/2, eave_z+rise)]
    faces = [(0,1,2),(3,5,4),(0,3,4,1),(1,4,5,2),(2,5,3,0)]
    mesh = bpy.data.meshes.new(name+'Mesh')
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    o = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(o)
    assign(o, material)
    return o

def window(name, x, y, z, face='front', w=1.0, h=1.25):
    d = 0.07
    if face in ('front','back'):
        box(name+'_Glass',(x,y,z),(w,d,h),M['glass'],bevel=0.015)
        yy=y+(0.018 if face=='front' else -0.018)
        box(name+'_L',(x-w/2,yy,z),(0.10,d*1.25,h+0.12),M['trim'],bevel=0.02)
        box(name+'_R',(x+w/2,yy,z),(0.10,d*1.25,h+0.12),M['trim'],bevel=0.02)
        box(name+'_T',(x,yy,z+h/2),(w+0.10,d*1.25,0.10),M['trim'],bevel=0.02)
        box(name+'_B',(x,yy,z-h/2),(w+0.10,d*1.25,0.10),M['trim'],bevel=0.02)
        box(name+'_MV',(x,yy+0.005,z),(0.055,d*1.3,h),M['trim'])
        box(name+'_MH',(x,yy+0.005,z),(w,d*1.3,0.055),M['trim'])
        box(name+'_Sill',(x,yy+0.05,z-h/2-0.08),(w+0.25,0.16,0.10),M['wood2'],bevel=0.03)
    else:
        box(name+'_Glass',(x,y,z),(d,w,h),M['glass'],bevel=0.015)
        xx=x+(0.018 if face=='right' else -0.018)
        box(name+'_L',(xx,y-w/2,z),(d*1.25,0.10,h+0.12),M['trim'],bevel=0.02)
        box(name+'_R',(xx,y+w/2,z),(d*1.25,0.10,h+0.12),M['trim'],bevel=0.02)
        box(name+'_T',(xx,y,z+h/2),(d*1.25,w+0.10,0.10),M['trim'],bevel=0.02)
        box(name+'_B',(xx,y,z-h/2),(d*1.25,w+0.10,0.10),M['trim'],bevel=0.02)
        box(name+'_MV',(xx+0.005,y,z),(d*1.3,0.055,h),M['trim'])
        box(name+'_MH',(xx+0.005,y,z),(d*1.3,w,0.055),M['trim'])

def railing_segment(name, start, end, z=1.18):
    x1,y1=start; x2,y2=end
    dx,dy=x2-x1,y2-y1
    length=math.hypot(dx,dy)
    angle=math.atan2(dy,dx)
    for zz in (z,z-0.68):
        box(name+'_rail',((x1+x2)/2,(y1+y2)/2,zz),(length,0.075,0.075),M['white'],rot=(0,0,angle),bevel=0.02)
    n=max(2,int(length/0.28))
    for i in range(n+1):
        t=i/n; x=x1+dx*t; y=y1+dy*t
        box(name+f'_bal_{i}',(x,y,z-0.34),(0.055,0.055,0.72),M['white'],bevel=0.012)

def fence_line(name, start, end, base_z=0.55):
    x1,y1=start; x2,y2=end
    dx,dy=x2-x1,y2-y1
    length=math.hypot(dx,dy); ang=math.atan2(dy,dx)
    for zz in (base_z+0.22,base_z+0.58):
        box(name+'_rail',((x1+x2)/2,(y1+y2)/2,zz),(length,0.065,0.065),M['white'],rot=(0,0,ang),bevel=0.015)
    n=max(2,int(length/0.33))
    for i in range(n+1):
        t=i/n; x=x1+dx*t; y=y1+dy*t
        box(name+f'_p{i}',(x,y,base_z+0.48),(0.10,0.10,0.95),M['white'],bevel=0.02)
        sphere(name+f'_cap{i}',(x,y,base_z+0.99),(0.08,0.08,0.12),M['white'],1)

def tree(name,x,y,z0=0.48,size=1.0):
    cyl(name+'_trunk',(x,y,z0+0.7*size),0.13*size,1.35*size,M['trunk'],vertices=12)
    centers=[(0,0,1.55),(-0.35,0,1.45),(0.33,0.03,1.5),(0.05,-0.28,1.62),(0.03,0.3,1.62)]
    for i,(dx,dy,dz) in enumerate(centers):
        sphere(name+f'_leaf{i}',(x+dx*size,y+dy*size,z0+dz*size),(0.48*size,0.48*size,0.44*size),M['leaf2' if i%2 else 'leaf'],2)

def flower_cluster(name,x,y,z=0.53,count=5):
    mats=[M['pink'],M['yellow'],M['purple']]
    for i in range(count):
        dx=(random.random()-0.5)*0.45; dy=(random.random()-0.5)*0.30
        sphere(name+f'_{i}',(x+dx,y+dy,z+random.random()*0.06),(0.07,0.07,0.06),mats[i%3],1)

box('Diorama_Earth',(0,0,0.18),(9.4,8.2,0.36),M['soil'],bevel=0.26)
box('Diorama_Grass',(0,0,0.42),(9.1,7.9,0.26),M['grass'],bevel=0.24)

hw,hd=5.6,4.7
wall_z0=0.56; wall_h=3.75
box('House_Main',(0,0,wall_z0+wall_h/2),(hw,hd,wall_h),M['wall'],bevel=0.05)
for zi in [0.95+i*0.34 for i in range(9)]:
    box('Front_Siding',(0,-hd/2-0.035,zi),(hw-0.12,0.045,0.035),M['trim'])
    box('Right_Siding',(hw/2+0.035,0,zi),(0.045,hd-0.12,0.035),M['trim'])

eave=wall_z0+wall_h
gable('Front_Gable',-hd/2-0.01,hw,eave-0.03,1.85,0.18,M['wall'])
gable('Back_Gable',hd/2+0.01,hw,eave-0.03,1.85,0.18,M['wall'])

over=0.48; half=hw/2+over; rise=2.05
slope=math.sqrt(half*half+rise*rise); ang=math.atan2(rise,half); roof_depth=hd+2*over
for side in (-1,1):
    rot_y=-ang if side<0 else ang
    cx=side*half/2; cz=eave+rise/2
    box(f'RoofPanel_{side}',(cx,0,cz),(slope,roof_depth,0.17),M['blue'],rot=(0,rot_y,0),bevel=0.025)
    rows=7; cols=10
    for r in range(rows):
        lx=-slope/2+(r+0.56)*slope/rows
        for c in range(cols):
            ly=-roof_depth/2+(c+0.5)*roof_depth/cols
            co=math.cos(rot_y); si=math.sin(rot_y)
            wx=cx+lx*co; wz=cz-lx*si+0.11
            matv=[M['blue'],M['blue2'],M['blue3']][(r+c)%3]
            box(f'Shingle_{side}_{r}_{c}',(wx,ly,wz),(slope/rows*0.92,roof_depth/cols*0.90,0.075),matv,rot=(0,rot_y,0),bevel=0.025)
for i,y in enumerate([-roof_depth/2+0.25+j*0.45 for j in range(int(roof_depth/0.45))]):
    cyl(f'Ridge_{i}',(0,y,eave+rise+0.06),0.13,0.42,M['blue2'],vertices=12,rot=(math.pi/2,0,0))

front_y=-hd/2-0.075
window('FrontWin_L',-1.62,front_y,2.0,'front',1.05,1.30)
window('FrontWin_R',1.65,front_y,2.0,'front',1.05,1.30)
window('UpperFront',0,front_y-0.01,4.55,'front',0.95,1.05)
window('SideWin1',hw/2+0.075,-0.55,2.0,'right',1.02,1.28)
window('SideWin2',hw/2+0.075,1.15,2.0,'right',0.92,1.18)

porch_y=-hd/2-0.78
box('PorchDeck',(0,porch_y,0.78),(4.25,1.45,0.22),M['wood2'],bevel=0.04)
box('PorchRoof',(0,porch_y-0.04,3.35),(4.65,1.75,0.14),M['blue2'],rot=(math.radians(-11),0,0),bevel=0.025)
for x in (-1.95,1.95):
    box('PorchPost',(x,porch_y-0.50,2.08),(0.16,0.16,2.55),M['trim'],bevel=0.025)
    box('PorchCap',(x,porch_y-0.50,3.34),(0.26,0.26,0.11),M['trim'],bevel=0.025)
box('Door',(0,-hd/2-0.082,1.83),(0.95,0.09,1.95),M['wood'],bevel=0.035)
box('DoorInset',(0,-hd/2-0.135,2.15),(0.54,0.045,0.62),M['glass'],bevel=0.02)
box('DoorFrameL',(-0.55,-hd/2-0.12,1.83),(0.11,0.12,2.10),M['trim'],bevel=0.02)
box('DoorFrameR',(0.55,-hd/2-0.12,1.83),(0.11,0.12,2.10),M['trim'],bevel=0.02)
box('DoorFrameT',(0,-hd/2-0.12,2.88),(1.2,0.12,0.12),M['trim'],bevel=0.02)
sphere('DoorKnob',(0.33,-hd/2-0.16,1.74),(0.055,0.055,0.055),M['brick_light'],1)
railing_segment('RailLeft',(-1.95,porch_y-0.58),(-0.72,porch_y-0.58),1.32)
railing_segment('RailRight',(0.72,porch_y-0.58),(1.95,porch_y-0.58),1.32)
railing_segment('RailSideL',(-1.95,porch_y-0.58),(-1.95,porch_y+0.45),1.32)
railing_segment('RailSideR',(1.95,porch_y-0.58),(1.95,porch_y+0.45),1.32)
for i in range(4):
    box(f'PorchStep{i}',(0,porch_y-0.86-i*0.24,0.66-i*0.12),(1.55,0.42,0.18),M['stone'],bevel=0.04)

box('DormerBody',(0,-1.05,4.58),(1.65,1.35,1.42),M['wall'],bevel=0.03)
window('DormerWindow',0,-1.75,4.58,'front',0.75,0.82)
small_half=1.08; small_rise=0.72
small_slope=math.sqrt(small_half**2+small_rise**2); small_ang=math.atan2(small_rise,small_half)
for side in (-1,1):
    ry=-small_ang if side<0 else small_ang
    box('DormerRoof',(side*small_half/2,-1.05,5.78),(small_slope,1.65,0.13),M['blue2'],rot=(0,ry,0),bevel=0.02)
for j in range(3):
    cyl('DormerRidge',(0,-1.45+j*0.38,6.17),0.10,0.34,M['blue3'],vertices=10,rot=(math.pi/2,0,0))

chim_x,chim_y=1.35,0.85
box('Chimney',(chim_x,chim_y,5.55),(0.62,0.72,2.55),M['brick'],bevel=0.025)
for i in range(7):
    box('ChimneyBand',(chim_x,chim_y,4.55+i*0.32),(0.65,0.75,0.04),M['brick_light'])
box('ChimneyCap',(chim_x,chim_y,6.88),(0.82,0.92,0.20),M['brick_light'],bevel=0.04)
box('ChimneyHole',(chim_x,chim_y,6.99),(0.46,0.54,0.06),M['wood'])

for i in range(7):
    box(f'Path_{i}',(0,-3.82-i*0.47,0.57),(1.25,0.40,0.08),M['stone'],bevel=0.06)

for x in (-3.55,-2.95,-2.35,2.35,2.95,3.55):
    box('FrontHedge',(x,-2.95,0.86),(0.52,0.52,0.62),M['hedge'],bevel=0.16)
for y in (-1.8,-1.15,-0.5,0.15,0.8,1.45,2.1):
    box('SideHedge',(3.55,y,0.86),(0.52,0.52,0.62),M['hedge'],bevel=0.16)
for x,y,s in [(-2.35,-2.65,0.42),(2.4,-2.62,0.48),(-2.65,-1.9,0.45),(2.65,-1.8,0.43),(1.55,-3.05,0.34),(-1.55,-3.05,0.34)]:
    sphere('Shrub',(x,y,0.78),(s,s,s*0.85),M['leaf'],2)

tree('TreeLeft',-3.65,1.75,size=1.0)
tree('TreeRight',3.55,2.0,size=0.92)
for idx,(x,y) in enumerate([(-3.3,-2.5),(-2.7,-2.45),(2.75,-2.4),(3.25,-1.5),(-3.5,0.2),(3.45,0.4),(-2.3,-3.0),(2.25,-3.0)]):
    flower_cluster(f'Flowers{idx}',x,y,count=6)

fence_line('FenceFrontL',(-4.2,-3.45),(-0.95,-3.45),0.58)
fence_line('FenceFrontR',(0.95,-3.45),(4.2,-3.45),0.58)
fence_line('FenceLeft',(-4.2,-3.45),(-4.2,3.25),0.58)
fence_line('FenceRight',(4.2,-3.45),(4.2,3.25),0.58)
fence_line('FenceBack',(-4.2,3.25),(4.2,3.25),0.58)

def look_at(obj,target):
    direction=Vector(target)-obj.location
    obj.rotation_euler=direction.to_track_quat('-Z','Y').to_euler()

bpy.ops.object.camera_add(location=(11.6,-14.0,10.6))
cam=bpy.context.object; cam.name='PreviewCamera'; cam.data.lens=56
look_at(cam,(0,-0.45,2.6)); scene.camera=cam
scene.world.color=(0.045,0.045,0.045)
scene.world.use_nodes=True
bg=scene.world.node_tree.nodes.get('Background')
if bg:
    bg.inputs['Color'].default_value=(0.86,0.84,0.78,1); bg.inputs['Strength'].default_value=0.7

bpy.ops.object.light_add(type='AREA',location=(4,-6,12))
key=bpy.context.object; key.data.energy=1050; key.data.shape='DISK'; key.data.size=7; look_at(key,(0,0,2.0))
bpy.ops.object.light_add(type='AREA',location=(-8,-1,7))
fill=bpy.context.object; fill.data.energy=650; fill.data.size=6; look_at(fill,(0,0,2.5))
bpy.ops.object.light_add(type='SUN',location=(0,0,10))
sun=bpy.context.object; sun.rotation_euler=(math.radians(28),math.radians(-18),math.radians(-30)); sun.data.energy=1.35; sun.data.angle=math.radians(12)

try: scene.render.engine='BLENDER_EEVEE_NEXT'
except Exception:
    try: scene.render.engine='BLENDER_EEVEE'
    except Exception: pass
scene.render.resolution_x=1200; scene.render.resolution_y=900; scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG'; scene.render.film_transparent=False
try: scene.view_settings.look='AgX - Medium High Contrast'
except Exception:
    try: scene.view_settings.look='Medium High Contrast'
    except Exception: pass

studio_mat=mat('Studio',(0.78,0.76,0.70),0.92)
box('StudioGround',(0,0,-0.055),(30,30,0.08),studio_mat)
blend_path=os.path.join(out_dir,'blue_roof_cottage.blend')
bpy.ops.wm.save_as_mainfile(filepath=blend_path)

studio=bpy.data.objects.get('StudioGround')
if studio: studio.hide_viewport=True; studio.hide_render=True
bpy.ops.object.select_all(action='DESELECT')
for obj in bpy.context.scene.objects:
    if obj.type in {'MESH','CURVE'} and obj.name!='StudioGround': obj.select_set(True)
try:
    bpy.ops.export_scene.gltf(filepath=os.path.join(out_dir,'blue_roof_cottage.glb'),export_format='GLB',use_selection=True,export_apply=True)
except TypeError:
    bpy.ops.export_scene.gltf(filepath=os.path.join(out_dir,'blue_roof_cottage.glb'),export_format='GLB',use_selection=True)

if studio: studio.hide_render=False
scene.render.filepath=os.path.join(out_dir,'blue_roof_cottage_preview.png')
bpy.ops.render.render(write_still=True)
print('DONE')
