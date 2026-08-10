package com.godnit.taskcity

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.atan2
import kotlin.math.sqrt

class TaskCityGame : ApplicationAdapter() {
    private lateinit var camera: OrthographicCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var dayEnvironment: Environment
    private lateinit var nightEnvironment: Environment
    private val ownedModels = ArrayList<Model>()
    private val scenery = ArrayList<ModelInstance>()
    private val houses = ArrayList<List<ModelInstance>>()

    @Volatile private var urgentMode = false
    @Volatile private var visibleCount = 0
    private var panX = 0f
    private var lastTouchX = 0

    private lateinit var grass: Model
    private lateinit var grassLight: Model
    private lateinit var road: Model
    private lateinit var curb: Model
    private lateinit var white: Model
    private lateinit var wood: Model
    private lateinit var dark: Model
    private lateinit var glass: Model
    private lateinit var trunk: Model
    private lateinit var leaf: Model
    private lateinit var leaf2: Model
    private lateinit var hedge: Model
    private lateinit var path: Model
    private lateinit var flowerRed: Model
    private lateinit var flowerYellow: Model
    private lateinit var lampGlow: Model
    private lateinit var shadow: Model
    private lateinit var sunModel: Model
    private lateinit var cloudModel: Model
    private lateinit var hillModel: Model
    private val wallModels = ArrayList<Model>()
    private val roofModels = ArrayList<Model>()

    fun setCityState(urgent: Boolean, count: Int) {
        urgentMode = urgent
        visibleCount = count.coerceIn(0, 20)
    }

    override fun create() {
        modelBatch = ModelBatch()
        camera = OrthographicCamera()
        camera.near = 0.1f
        camera.far = 140f

        dayEnvironment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.72f, 0.76f, 0.68f, 1f))
            add(DirectionalLight().set(1.0f, 0.94f, 0.78f, -0.65f, -1.0f, -0.45f))
        }
        nightEnvironment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.48f, 0.50f, 0.67f, 1f))
            add(DirectionalLight().set(0.66f, 0.73f, 1.0f, -0.35f, -1.0f, -0.25f))
        }

        buildSharedModels()
        buildWorld()
        resize(Gdx.graphics.width, Gdx.graphics.height)

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (pointer != 0) return false
                lastTouchX = screenX
                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (pointer != 0) return false
                val dx = screenX - lastTouchX
                lastTouchX = screenX
                panX = MathUtils.clamp(panX - dx * 0.045f, -21f, 21f)
                updateCamera()
                return true
            }
        }
        Gdx.app.log("TaskCityGDX", "TASKCITY_LIBGDX_READY houses=${houses.size}")
    }

    private fun makeBox(color: Color, blendedAlpha: Float? = null): Model {
        val builder = ModelBuilder()
        val material = if (blendedAlpha == null) {
            Material(ColorAttribute.createDiffuse(color))
        } else {
            Material(
                ColorAttribute.createDiffuse(Color(color.r, color.g, color.b, blendedAlpha)),
                BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, blendedAlpha)
            )
        }
        val model = builder.createBox(1f, 1f, 1f, material, (Usage.Position or Usage.Normal).toLong())
        ownedModels.add(model)
        return model
    }

    private fun makeSphere(color: Color, divisions: Int = 12): Model {
        val builder = ModelBuilder()
        val model = builder.createSphere(1f, 1f, 1f, divisions, divisions.coerceAtLeast(8), Material(ColorAttribute.createDiffuse(color)), (Usage.Position or Usage.Normal).toLong())
        ownedModels.add(model)
        return model
    }

    private fun makeCylinder(color: Color, divisions: Int = 10): Model {
        val builder = ModelBuilder()
        val model = builder.createCylinder(1f, 1f, 1f, divisions, Material(ColorAttribute.createDiffuse(color)), (Usage.Position or Usage.Normal).toLong())
        ownedModels.add(model)
        return model
    }

    private fun buildSharedModels() {
        grass = makeBox(Color(0.30f, 0.72f, 0.25f, 1f))
        grassLight = makeBox(Color(0.39f, 0.79f, 0.31f, 1f))
        road = makeBox(Color(0.16f, 0.18f, 0.20f, 1f))
        curb = makeBox(Color(0.84f, 0.82f, 0.75f, 1f))
        white = makeBox(Color(0.97f, 0.96f, 0.91f, 1f))
        wood = makeBox(Color(0.55f, 0.30f, 0.14f, 1f))
        dark = makeBox(Color(0.18f, 0.27f, 0.33f, 1f))
        glass = makeBox(Color(0.50f, 0.78f, 0.91f, 1f))
        trunk = makeCylinder(Color(0.39f, 0.22f, 0.10f, 1f), 9)
        leaf = makeSphere(Color(0.12f, 0.52f, 0.18f, 1f), 10)
        leaf2 = makeSphere(Color(0.26f, 0.68f, 0.26f, 1f), 10)
        hedge = makeBox(Color(0.18f, 0.61f, 0.21f, 1f))
        path = makeBox(Color(0.82f, 0.72f, 0.57f, 1f))
        flowerRed = makeSphere(Color(0.96f, 0.28f, 0.32f, 1f), 8)
        flowerYellow = makeSphere(Color(1.0f, 0.72f, 0.18f, 1f), 8)
        lampGlow = makeSphere(Color(1.0f, 0.86f, 0.36f, 1f), 8)
        shadow = makeBox(Color(0.06f, 0.10f, 0.08f, 1f), 0.16f)
        sunModel = makeSphere(Color(1.0f, 0.88f, 0.38f, 1f), 16)
        cloudModel = makeSphere(Color(1f, 1f, 1f, 1f), 12)
        hillModel = makeSphere(Color(0.26f, 0.60f, 0.30f, 1f), 12)

        val wallColors = arrayOf(
            Color(0.94f, 0.86f, 0.67f, 1f), Color(0.90f, 0.74f, 0.52f, 1f),
            Color(0.96f, 0.84f, 0.43f, 1f), Color(0.69f, 0.84f, 0.89f, 1f),
            Color(0.72f, 0.87f, 0.68f, 1f), Color(0.94f, 0.91f, 0.83f, 1f)
        )
        val roofColors = arrayOf(
            Color(0.79f, 0.20f, 0.10f, 1f), Color(0.04f, 0.43f, 0.50f, 1f),
            Color(0.10f, 0.34f, 0.68f, 1f), Color(0.18f, 0.47f, 0.24f, 1f),
            Color(0.89f, 0.36f, 0.10f, 1f), Color(0.25f, 0.34f, 0.55f, 1f)
        )
        wallColors.forEach { wallModels.add(makeBox(it)) }
        roofColors.forEach { roofModels.add(makeBox(it)) }
    }

    private fun instance(model: Model, x: Float, y: Float, z: Float, sx: Float = 1f, sy: Float = 1f, sz: Float = 1f, rotY: Float = 0f, rotZ: Float = 0f): ModelInstance {
        return ModelInstance(model).apply {
            transform.setToTranslation(x, y, z)
            if (rotY != 0f) transform.rotate(Vector3.Y, rotY)
            if (rotZ != 0f) transform.rotate(Vector3.Z, rotZ)
            transform.scale(sx, sy, sz)
        }
    }

    private fun addScenery(model: Model, x: Float, y: Float, z: Float, sx: Float = 1f, sy: Float = 1f, sz: Float = 1f, rotY: Float = 0f, rotZ: Float = 0f) {
        scenery.add(instance(model, x, y, z, sx, sy, sz, rotY, rotZ))
    }

    private fun buildWorld() {
        addScenery(grass, 0f, -0.35f, 0f, 66f, 0.7f, 36f)
        for (i in 0 until 36) {
            val x = -30f + ((i * 4.7f) % 61f)
            val z = -15f + ((i * 7.1f) % 30f)
            addScenery(grassLight, x, 0.015f, z, 1.25f, 0.025f, 1.1f, (i % 4) * 12f)
        }

        addRoadPath(arrayOf(-31f to -7.0f, -22f to -7.3f, -14f to -6.6f, -6f to -5.8f, 2f to -5.7f, 10f to -6.2f, 19f to -7.0f, 31f to -6.9f), 2.05f)
        addRoadPath(arrayOf(-31f to 6.9f, -22f to 6.4f, -13f to 5.9f, -5f to 6.1f, 3f to 6.7f, 11f to 6.4f, 20f to 6.0f, 31f to 7.0f), 2.0f)
        addRoadPath(arrayOf(-17f to -7.0f, -16.2f to -0.4f, -17f to 6.2f), 1.95f)
        addRoadPath(arrayOf(-1.8f to -5.8f, -0.8f to 0.3f, 1.3f to 6.5f), 1.95f)
        addRoadPath(arrayOf(15.3f to -6.8f, 16.0f to -0.4f, 15.1f to 6.1f), 1.95f)

        // Soft green hills and clouds in the far background make the city feel like a place rather than a board.
        for (i in 0 until 8) {
            addScenery(hillModel, -28f + i * 8f, 3.5f + (i % 2), -19f, 10f, 6f, 4f)
        }
        addCloud(-18f, 16f, -18f, 2.0f)
        addCloud(12f, 18f, -20f, 2.5f)
        addScenery(sunModel, -20f, 20f, -22f, 3.0f, 3.0f, 3.0f)

        val slots = arrayOf(
            -25f to -10.7f, -19f to -10.4f, -11f to -10.2f, -5f to -10.5f,
            3.8f to -10.1f, 10.3f to -10.4f, 18f to -10.7f, 25f to -10.4f,
            -23f to 0.2f, -10.5f to 0.0f, 6.0f to 0.1f, 22.2f to 0.5f,
            -25f to 10.5f, -18.5f to 10.2f, -11f to 10.3f, -4.2f to 10.7f,
            4.2f to 10.5f, 11f to 10.2f, 18.2f to 10.4f, 25f to 10.6f
        )
        slots.forEachIndexed { i, p -> houses.add(buildHouse(p.first, p.second, i)) }

        val trees = arrayOf(
            Triple(-30f, -14f, 1.25f), Triple(-29f, -3f, 1.05f), Triple(-28f, 12f, 1.2f),
            Triple(29f, -13f, 1.15f), Triple(30f, -4f, 1.2f), Triple(28f, 12f, 1.2f),
            Triple(-22f, -3.2f, .95f), Triple(-20f, 3.2f, .88f), Triple(-13f, -3.0f, .92f),
            Triple(-9f, 2.9f, .88f), Triple(-5f, -13f, 1.0f), Triple(4f, -2.8f, .9f),
            Triple(8f, 2.9f, .9f), Triple(12f, -13f, .95f), Triple(20f, -3.0f, .92f),
            Triple(22f, 3.2f, .9f), Triple(-13f, 13f, 1.0f), Triple(3f, 13f, .96f), Triple(18f, 13f, 1.0f)
        )
        trees.forEach { addTree(it.first, it.second, it.third) }

        addBench(-7f, 3.0f, 0f)
        addBench(9f, -2.8f, 180f)
        addBench(24f, -2.6f, 8f)
        for (i in 0 until 11) {
            val x = -27f + i * 5.4f
            val z = if (i % 2 == 0) 8.1f else -8.2f
            addScenery(dark, x, 1.0f, z, .12f, 2.0f, .12f)
            addScenery(lampGlow, x, 2.08f, z, .34f, .34f, .34f)
        }
    }

    private fun addRoadPath(points: Array<Pair<Float, Float>>, width: Float) {
        for (i in 0 until points.size - 1) {
            addRoadSegment(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second, width)
        }
    }

    private fun addRoadSegment(x1: Float, z1: Float, x2: Float, z2: Float, width: Float) {
        val dx = x2 - x1
        val dz = z2 - z1
        val length = sqrt(dx * dx + dz * dz) + .22f
        val angle = MathUtils.radiansToDegrees * atan2(dz, dx)
        val cx = (x1 + x2) * .5f
        val cz = (z1 + z2) * .5f
        addScenery(curb, cx, .04f, cz, length + .5f, .10f, width + .75f, angle)
        addScenery(road, cx, .11f, cz, length, .14f, width, angle)
    }

    private fun addCloud(x: Float, y: Float, z: Float, s: Float) {
        addScenery(cloudModel, x, y, z, 2.1f * s, 1.15f * s, 1.0f * s)
        addScenery(cloudModel, x - 1.5f * s, y - .2f * s, z, 1.4f * s, .9f * s, .8f * s)
        addScenery(cloudModel, x + 1.4f * s, y - .1f * s, z, 1.5f * s, .95f * s, .85f * s)
    }

    private fun addTree(x: Float, z: Float, s: Float) {
        addScenery(shadow, x + .45f * s, .02f, z + .35f * s, 2.4f * s, .02f, 1.6f * s, -20f)
        addScenery(trunk, x, .85f * s, z, .45f * s, 1.7f * s, .45f * s)
        addScenery(leaf, x, 2.2f * s, z, 2.2f * s, 2.0f * s, 2.2f * s)
        addScenery(leaf2, x - .7f * s, 2.0f * s, z + .1f * s, 1.5f * s, 1.35f * s, 1.5f * s)
        addScenery(leaf2, x + .68f * s, 2.0f * s, z - .08f * s, 1.45f * s, 1.30f * s, 1.45f * s)
    }

    private fun buildHouse(x: Float, z: Float, index: Int): List<ModelInstance> {
        val parts = ArrayList<ModelInstance>()
        val w = 3.6f + (index % 3) * .22f
        val d = 3.0f + ((index + 1) % 2) * .22f
        val h = 2.35f + ((index + 2) % 3) * .16f
        val wall = wallModels[index % wallModels.size]
        val roof = roofModels[(index * 5) % roofModels.size]

        parts.add(instance(shadow, x + .55f, .02f, z + .42f, 4.6f, .02f, 3.0f, -16f))
        parts.add(instance(wall, x, h * .5f, z, w, h, d))
        // Two separate sloped roof planes make the roof a different color/material from the walls.
        parts.add(instance(roof, x - w * .23f, h + .63f, z, w * .60f, .20f, d + .55f, 0f, 28f))
        parts.add(instance(roof, x + w * .23f, h + .63f, z, w * .60f, .20f, d + .55f, 0f, -28f))

        parts.add(instance(wood, x, .67f, z + d * .5f + .08f, .72f, 1.34f, .14f))
        addHouseWindow(parts, x - 1.05f, 1.42f, z + d * .5f + .09f)
        addHouseWindow(parts, x + 1.05f, 1.42f, z + d * .5f + .09f)
        parts.add(instance(path, x, .10f, z + d * .5f + .55f, 2.55f, .16f, .90f))
        parts.add(instance(path, x, .10f, z + d * .5f + 1.02f, 1.15f, .14f, .48f))
        if (index % 2 == 0) parts.add(instance(white, x + 1.0f, h + .95f, z - .48f, .38f, 1.12f, .38f))

        // Garden/fence belongs to the earned house and remains invisible before the task is completed.
        parts.add(instance(white, x, .34f, z - 2.12f, 4.25f, .55f, .10f))
        parts.add(instance(hedge, x - 1.55f, .34f, z + 2.0f, 1.35f, .68f, .52f))
        parts.add(instance(hedge, x + 1.55f, .32f, z + 2.0f, 1.15f, .62f, .50f))
        for (k in 0 until 4) {
            val fm = if (k % 2 == 0) flowerRed else flowerYellow
            parts.add(instance(fm, x - 1.18f + k * .78f, .22f, z + 1.72f, .28f, .28f, .28f))
        }
        return parts
    }

    private fun addHouseWindow(parts: MutableList<ModelInstance>, x: Float, y: Float, z: Float) {
        parts.add(instance(white, x, y, z, .68f, .78f, .10f))
        parts.add(instance(glass, x, y, z + .065f, .50f, .58f, .08f))
    }

    private fun addBench(x: Float, z: Float, rot: Float) {
        addScenery(wood, x, .48f, z, 1.55f, .16f, .50f, rot)
        addScenery(wood, x, .82f, z - .24f, 1.55f, .72f, .13f, rot)
        addScenery(dark, x - .55f, .25f, z, .12f, .55f, .12f, rot)
        addScenery(dark, x + .55f, .25f, z, .12f, .55f, .12f, rot)
    }

    private fun updateCamera() {
        camera.position.set(22f + panX, 20f, 29f)
        camera.up.set(Vector3.Y)
        camera.lookAt(panX, 0.0f, 0.0f)
        camera.update()
    }

    override fun resize(width: Int, height: Int) {
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        val viewHeight = 27.5f
        camera.viewportHeight = viewHeight
        camera.viewportWidth = viewHeight * aspect
        updateCamera()
    }

    override fun render() {
        if (urgentMode) {
            Gdx.gl.glClearColor(0.28f, 0.28f, 0.49f, 1f)
        } else {
            Gdx.gl.glClearColor(0.42f, 0.78f, 0.96f, 1f)
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

        val env = if (urgentMode) nightEnvironment else dayEnvironment
        modelBatch.begin(camera)
        scenery.forEach { inst ->
            // Sun is the only scenery item hidden in challenge/night mode.
            if (urgentMode && inst.model === sunModel) return@forEach
            modelBatch.render(inst, env)
        }
        val count = visibleCount.coerceIn(0, houses.size)
        for (i in 0 until count) houses[i].forEach { modelBatch.render(it, env) }
        modelBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        ownedModels.forEach { it.dispose() }
        ownedModels.clear()
    }
}
