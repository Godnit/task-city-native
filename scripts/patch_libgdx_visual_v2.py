from pathlib import Path

p=Path('app/src/main/java/com/godnit/taskcity/TaskCityGame.kt')
s=p.read_text()

# Narrower neighborhood roads so grass, gardens, houses and trees dominate the scene.
s=s.replace("), 2.05f)\n        addRoadPath(arrayOf(-31f to 6.9f", "), 1.65f)\n        addRoadPath(arrayOf(-31f to 6.9f", 1)
s=s.replace("), 2.0f)\n        addRoadPath(arrayOf(-17f to -7.0f", "), 1.62f)\n        addRoadPath(arrayOf(-17f to -7.0f", 1)
s=s.replace("), 1.95f)\n        addRoadPath(arrayOf(-1.8f to -5.8f", "), 1.55f)\n        addRoadPath(arrayOf(-1.8f to -5.8f", 1)
s=s.replace("), 1.95f)\n        addRoadPath(arrayOf(15.3f to -6.8f", "), 1.55f)\n        addRoadPath(arrayOf(15.3f to -6.8f", 1)
s=s.replace("), 1.95f)\n\n        // Soft green hills", "), 1.55f)\n\n        // Soft green hills", 1)

# Put the sky decoration inside the initial camera framing. The earlier coordinates were above/outside it.
s=s.replace('''        addCloud(-18f, 16f, -18f, 2.0f)
        addCloud(12f, 18f, -20f, 2.5f)
        addScenery(sunModel, -20f, 20f, -22f, 3.0f, 3.0f, 3.0f)''',
'''        addCloud(-10f, 8.0f, -6.0f, 1.25f)
        addCloud(2.0f, 8.7f, -6.0f, 1.15f)
        // Clearly visible warm sun in the achievement-city sky.
        addScenery(sunModel, -6.8f, 10.2f, -4.0f, 1.8f, 1.8f, 1.8f)''',1)

# Earned houses grow around the initial camera center instead of filling a predictable row from the far edge.
old='''        val slots = arrayOf(
            -25f to -10.7f, -19f to -10.4f, -11f to -10.2f, -5f to -10.5f,
            3.8f to -10.1f, 10.3f to -10.4f, 18f to -10.7f, 25f to -10.4f,
            -23f to 0.2f, -10.5f to 0.0f, 6.0f to 0.1f, 22.2f to 0.5f,
            -25f to 10.5f, -18.5f to 10.2f, -11f to 10.3f, -4.2f to 10.7f,
            4.2f to 10.5f, 11f to 10.2f, 18.2f to 10.4f, 25f to 10.6f
        )'''
new='''        val slots = arrayOf(
            -5f to -10.5f, 4.2f to 10.5f, 6.0f to 0.1f, -10.5f to 0.0f,
            3.8f to -10.1f, -4.2f to 10.7f, 10.3f to -10.4f, 11f to 10.2f,
            -11f to -10.2f, -11f to 10.3f, -19f to -10.4f, -18.5f to 10.2f,
            -23f to 0.2f, 18f to -10.7f, 18.2f to 10.4f, 22.2f to 0.5f,
            -25f to -10.7f, -25f to 10.5f, 25f to -10.4f, 25f to 10.6f
        )'''
if old not in s:
    raise SystemExit('slot layout marker not found')
s=s.replace(old,new,1)

# Slightly closer fixed isometric framing: still no zoom/rotation controls.
s=s.replace('val viewHeight = 27.5f','val viewHeight = 24.0f',1)

# Brighter cheerful daylight, with real directional shading plus the visible sun object.
s=s.replace('Gdx.gl.glClearColor(0.42f, 0.78f, 0.96f, 1f)','Gdx.gl.glClearColor(0.47f, 0.82f, 0.99f, 1f)',1)

p.write_text(s)
print('TASKCITY_LIBGDX_VISUAL_V2_OK')
