import bpy
import os
import sys

out_dir = os.path.abspath('output/blue-roof-house')
if '--' in sys.argv:
    argv = sys.argv[sys.argv.index('--') + 1:]
    if '--output' in argv:
        i = argv.index('--output')
        if i + 1 < len(argv):
            out_dir = os.path.abspath(argv[i + 1])
os.makedirs(out_dir, exist_ok=True)

glb_path = os.path.join(out_dir, 'blue_roof_cottage.glb')
png_path = os.path.join(out_dir, 'blue_roof_cottage_preview.png')

# Ensure the built-in glTF exporter is enabled on distro Blender builds.
try:
    bpy.ops.preferences.addon_enable(module='io_scene_gltf2')
except Exception as exc:
    print('glTF addon enable note:', exc)

studio = bpy.data.objects.get('StudioGround')
if studio:
    studio.hide_render = True
    studio.hide_viewport = True

bpy.ops.object.select_all(action='DESELECT')
for obj in bpy.context.scene.objects:
    if obj.type in {'MESH', 'CURVE'} and obj.name != 'StudioGround':
        obj.hide_set(False)
        obj.select_set(True)

print('Exporting GLB to', glb_path)
bpy.ops.export_scene.gltf(
    filepath=glb_path,
    export_format='GLB',
    use_selection=True
)

if not os.path.isfile(glb_path) or os.path.getsize(glb_path) < 1000:
    raise RuntimeError('GLB export did not produce a valid file')

if studio:
    studio.hide_render = False
    studio.hide_viewport = False

scene = bpy.context.scene
scene.render.filepath = png_path
scene.render.image_settings.file_format = 'PNG'
scene.render.resolution_x = 1200
scene.render.resolution_y = 900
scene.render.resolution_percentage = 100

print('Rendering preview to', png_path)
bpy.ops.render.render(write_still=True)

if not os.path.isfile(png_path) or os.path.getsize(png_path) < 1000:
    raise RuntimeError('Preview render did not produce a valid PNG')

print('EXPORT_OK')
print(glb_path)
print(png_path)
