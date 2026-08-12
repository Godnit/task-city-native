extends Node3D

const SAVE_PATH := "user://task_city_save.json"
const STATUS_ACTIVE := "active"
const STATUS_COMPLETED := "completed"
const STATUS_FAILED := "failed"

var tasks: Array = []
var houses: Array = []
var next_task_id: int = 1
var plot_positions: Array = []
var house_nodes: Dictionary = {}
var elapsed_second: float = 0.0

var camera_rig: Node3D
var camera: Camera3D
var touch_points: Dictionary = {}
var last_pinch_distance: float = -1.0

var task_list: VBoxContainer
var stats_label: Label
var empty_label: Label
var add_panel: PanelContainer
var task_title_input: LineEdit
var duration_select: OptionButton
var toast_label: Label

var grass_mat: StandardMaterial3D
var road_mat: StandardMaterial3D
var sidewalk_mat: StandardMaterial3D
var trunk_mat: StandardMaterial3D
var leaf_mat: StandardMaterial3D
var window_mat: StandardMaterial3D
var door_mat: StandardMaterial3D

func _ready() -> void:
    randomize()
    _create_materials()
    _create_world()
    _load_game()
    _rebuild_saved_houses()
    _create_ui()
    _resolve_expired_tasks()
    _refresh_ui()

func _process(delta: float) -> void:
    elapsed_second += delta
    if elapsed_second >= 1.0:
        elapsed_second = 0.0
        _resolve_expired_tasks()
        _refresh_ui()

func _create_materials() -> void:
    grass_mat = _material(Color("#7fbd55"), 0.95)
    road_mat = _material(Color("#565b63"), 0.95)
    sidewalk_mat = _material(Color("#c9c4b6"), 0.9)
    trunk_mat = _material(Color("#8d5b3e"), 1.0)
    leaf_mat = _material(Color("#4d9b4b"), 0.95)
    window_mat = _material(Color("#8ed8ee"), 0.25)
    window_mat.metallic = 0.05
    door_mat = _material(Color("#8b5a36"), 0.9)

func _create_world() -> void:
    var env := WorldEnvironment.new()
    var environment := Environment.new()
    environment.background_mode = Environment.BG_COLOR
    environment.background_color = Color("#88c9f4")
    environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    environment.ambient_light_color = Color("#d5edff")
    environment.ambient_light_energy = 0.7
    environment.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    env.environment = environment
    add_child(env)

    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-55.0, -35.0, 0.0)
    sun.light_color = Color("#fff4db")
    sun.light_energy = 1.15
    sun.shadow_enabled = true
    sun.directional_shadow_max_distance = 70.0
    add_child(sun)

    var world := Node3D.new()
    world.name = "CityWorld"
    add_child(world)

    _box(world, Vector3(62.0, 0.25, 62.0), Vector3(0, -0.12, 0), grass_mat)

    _box(world, Vector3(9.0, 0.12, 56.0), Vector3(0, 0.07, 0), road_mat)
    _box(world, Vector3(56.0, 0.12, 9.0), Vector3(0, 0.075, 0), road_mat)

    _box(world, Vector3(1.2, 0.13, 56.0), Vector3(-5.1, 0.08, 0), sidewalk_mat)
    _box(world, Vector3(1.2, 0.13, 56.0), Vector3(5.1, 0.08, 0), sidewalk_mat)
    _box(world, Vector3(56.0, 0.13, 1.2), Vector3(0, 0.085, -5.1), sidewalk_mat)
    _box(world, Vector3(56.0, 0.13, 1.2), Vector3(0, 0.085, 5.1), sidewalk_mat)

    for z in range(-24, 25, 8):
        _box(world, Vector3(0.22, 0.14, 3.2), Vector3(0, 0.15, float(z)), _material(Color("#efe9d6"), 0.9))
    for x in range(-24, 25, 8):
        _box(world, Vector3(3.2, 0.14, 0.22), Vector3(float(x), 0.15, 0), _material(Color("#efe9d6"), 0.9))

    plot_positions = [
        Vector3(-10, 0, -23), Vector3(10, 0, -23),
        Vector3(-18, 0, -16), Vector3(18, 0, -16),
        Vector3(-10, 0, -11), Vector3(10, 0, -11),
        Vector3(-22, 0, -8), Vector3(22, 0, -8),
        Vector3(-11, 0, 11), Vector3(11, 0, 11),
        Vector3(-21, 0, 15), Vector3(21, 0, 15),
        Vector3(-11, 0, 22), Vector3(11, 0, 22),
        Vector3(-25, 0, 23), Vector3(25, 0, 23),
        Vector3(-25, 0, -23), Vector3(25, 0, -23),
        Vector3(-24, 0, 8), Vector3(24, 0, 8)
    ]

    for p in plot_positions:
        _box(world, Vector3(7.2, 0.08, 7.2), Vector3(p.x, 0.02, p.z), _material(Color("#8dca62"), 0.98))

    var tree_positions := [
        Vector3(-29,0,-27), Vector3(-21,0,-27), Vector3(-6,0,-28), Vector3(7,0,-28), Vector3(21,0,-28), Vector3(29,0,-26),
        Vector3(-29,0,-14), Vector3(29,0,-12), Vector3(-29,0,2), Vector3(29,0,3), Vector3(-29,0,18), Vector3(29,0,18),
        Vector3(-27,0,28), Vector3(-14,0,28), Vector3(0,0,28), Vector3(14,0,28), Vector3(27,0,28)
    ]
    for p in tree_positions:
        _create_tree(world, p, 0.85 + randf() * 0.25)

    camera_rig = Node3D.new()
    camera_rig.name = "CameraRig"
    add_child(camera_rig)
    camera = Camera3D.new()
    camera.name = "Camera3D"
    camera.position = Vector3(18.0, 23.0, 22.0)
    camera.fov = 31.0
    camera.near = 0.1
    camera.far = 120.0
    camera.current = true
    camera_rig.add_child(camera)
    camera.look_at(Vector3.ZERO, Vector3.UP)

func _create_ui() -> void:
    var layer := CanvasLayer.new()
    layer.name = "UI"
    add_child(layer)

    var root := Control.new()
    root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    root.layout_direction = Control.LAYOUT_DIRECTION_RTL
    layer.add_child(root)

    var top := PanelContainer.new()
    top.anchor_left = 0.035
    top.anchor_top = 0.02
    top.anchor_right = 0.965
    top.anchor_bottom = 0.105
    top.add_theme_stylebox_override("panel", _panel_style(Color(0.08, 0.12, 0.17, 0.90), 24.0))
    root.add_child(top)

    var top_row := HBoxContainer.new()
    top_row.add_theme_constant_override("separation", 20)
    top.add_child(top_row)

    var title := Label.new()
    title.text = "مدينة الإنجاز"
    title.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    title.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
    title.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
    title.add_theme_font_size_override("font_size", 30)
    title.add_theme_color_override("font_color", Color.WHITE)
    top_row.add_child(title)

    stats_label = Label.new()
    stats_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
    stats_label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
    stats_label.add_theme_font_size_override("font_size", 20)
    stats_label.add_theme_color_override("font_color", Color("#d7f6ff"))
    top_row.add_child(stats_label)

    var list_panel := PanelContainer.new()
    list_panel.anchor_left = 0.035
    list_panel.anchor_top = 0.125
    list_panel.anchor_right = 0.49
    list_panel.anchor_bottom = 0.73
    list_panel.add_theme_stylebox_override("panel", _panel_style(Color(0.08, 0.12, 0.17, 0.84), 22.0))
    root.add_child(list_panel)

    var list_vbox := VBoxContainer.new()
    list_vbox.add_theme_constant_override("separation", 12)
    list_panel.add_child(list_vbox)

    var list_title := Label.new()
    list_title.text = "المهام النشطة"
    list_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
    list_title.add_theme_font_size_override("font_size", 23)
    list_title.add_theme_color_override("font_color", Color.WHITE)
    list_vbox.add_child(list_title)

    var scroll := ScrollContainer.new()
    scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
    scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    list_vbox.add_child(scroll)

    task_list = VBoxContainer.new()
    task_list.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    task_list.add_theme_constant_override("separation", 10)
    scroll.add_child(task_list)

    empty_label = Label.new()
    empty_label.text = "لا توجد مهام الآن\nأضف مهمة وابدأ بناء مدينتك"
    empty_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    empty_label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
    empty_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    empty_label.add_theme_font_size_override("font_size", 19)
    empty_label.add_theme_color_override("font_color", Color("#d3dbe5"))
    task_list.add_child(empty_label)

    var home_button := Button.new()
    home_button.text = "⌂"
    home_button.anchor_left = 0.82
    home_button.anchor_top = 0.125
    home_button.anchor_right = 0.94
    home_button.anchor_bottom = 0.195
    home_button.add_theme_font_size_override("font_size", 26)
    home_button.pressed.connect(_reset_camera)
    root.add_child(home_button)

    var add_button := Button.new()
    add_button.text = "+  مهمة"
    add_button.anchor_left = 0.72
    add_button.anchor_top = 0.88
    add_button.anchor_right = 0.955
    add_button.anchor_bottom = 0.955
    add_button.add_theme_font_size_override("font_size", 24)
    add_button.add_theme_color_override("font_color", Color.WHITE)
    add_button.add_theme_stylebox_override("normal", _panel_style(Color("#e88c2f"), 26.0))
    add_button.add_theme_stylebox_override("hover", _panel_style(Color("#f39b3c"), 26.0))
    add_button.add_theme_stylebox_override("pressed", _panel_style(Color("#cf7620"), 26.0))
    add_button.pressed.connect(_toggle_add_panel)
    root.add_child(add_button)

    add_panel = PanelContainer.new()
    add_panel.anchor_left = 0.08
    add_panel.anchor_top = 0.68
    add_panel.anchor_right = 0.92
    add_panel.anchor_bottom = 0.93
    add_panel.visible = false
    add_panel.add_theme_stylebox_override("panel", _panel_style(Color(0.07, 0.10, 0.15, 0.96), 26.0))
    root.add_child(add_panel)

    var add_v := VBoxContainer.new()
    add_v.add_theme_constant_override("separation", 14)
    add_panel.add_child(add_v)

    var add_title := Label.new()
    add_title.text = "مهمة جديدة"
    add_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
    add_title.add_theme_font_size_override("font_size", 25)
    add_title.add_theme_color_override("font_color", Color.WHITE)
    add_v.add_child(add_title)

    task_title_input = LineEdit.new()
    task_title_input.placeholder_text = "مثال: مراجعة سورة الملك"
    task_title_input.custom_minimum_size = Vector2(0, 58)
    task_title_input.add_theme_font_size_override("font_size", 20)
    add_v.add_child(task_title_input)

    duration_select = OptionButton.new()
    duration_select.custom_minimum_size = Vector2(0, 54)
    duration_select.add_theme_font_size_override("font_size", 19)
    _add_duration("30 دقيقة", 30 * 60)
    _add_duration("ساعة", 60 * 60)
    _add_duration("3 ساعات", 3 * 60 * 60)
    _add_duration("6 ساعات", 6 * 60 * 60)
    _add_duration("12 ساعة", 12 * 60 * 60)
    _add_duration("24 ساعة", 24 * 60 * 60)
    duration_select.select(1)
    add_v.add_child(duration_select)

    var actions := HBoxContainer.new()
    actions.add_theme_constant_override("separation", 12)
    add_v.add_child(actions)

    var cancel := Button.new()
    cancel.text = "إلغاء"
    cancel.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    cancel.custom_minimum_size = Vector2(0, 54)
    cancel.pressed.connect(_toggle_add_panel)
    actions.add_child(cancel)

    var confirm := Button.new()
    confirm.text = "إضافة المهمة"
    confirm.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    confirm.custom_minimum_size = Vector2(0, 54)
    confirm.add_theme_color_override("font_color", Color.WHITE)
    confirm.add_theme_stylebox_override("normal", _panel_style(Color("#2da76e"), 18.0))
    confirm.add_theme_stylebox_override("pressed", _panel_style(Color("#248d5d"), 18.0))
    confirm.pressed.connect(_on_add_task_confirm)
    actions.add_child(confirm)

    toast_label = Label.new()
    toast_label.anchor_left = 0.18
    toast_label.anchor_top = 0.12
    toast_label.anchor_right = 0.82
    toast_label.anchor_bottom = 0.18
    toast_label.visible = false
    toast_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    toast_label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
    toast_label.add_theme_font_size_override("font_size", 22)
    toast_label.add_theme_color_override("font_color", Color.WHITE)
    toast_label.add_theme_stylebox_override("normal", _panel_style(Color(0.05,0.08,0.12,0.90), 18.0))
    root.add_child(toast_label)

func _add_duration(label_text: String, seconds: int) -> void:
    var idx := duration_select.item_count
    duration_select.add_item(label_text)
    duration_select.set_item_metadata(idx, seconds)

func _toggle_add_panel() -> void:
    add_panel.visible = not add_panel.visible
    if add_panel.visible:
        task_title_input.grab_focus()

func _on_add_task_confirm() -> void:
    var title := task_title_input.text.strip_edges()
    if title.is_empty():
        _toast("اكتب اسم المهمة أولاً")
        return
    var seconds := int(duration_select.get_item_metadata(duration_select.selected))
    var now := int(Time.get_unix_time_from_system())
    tasks.append({
        "id": next_task_id,
        "title": title,
        "created": now,
        "deadline": now + seconds,
        "status": STATUS_ACTIVE
    })
    next_task_id += 1
    task_title_input.clear()
    add_panel.visible = false
    _save_game()
    _refresh_ui()
    _toast("تمت إضافة المهمة")

func _refresh_ui() -> void:
    if task_list == null:
        return
    for child in task_list.get_children():
        child.queue_free()

    var active_count := 0
    for task in tasks:
        if String(task.get("status", "")) == STATUS_ACTIVE:
            active_count += 1
            task_list.add_child(_make_task_card(task))

    if active_count == 0:
        empty_label = Label.new()
        empty_label.text = "لا توجد مهام الآن\nأضف مهمة وابدأ بناء مدينتك"
        empty_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
        empty_label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
        empty_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
        empty_label.add_theme_font_size_override("font_size", 18)
        empty_label.add_theme_color_override("font_color", Color("#d3dbe5"))
        task_list.add_child(empty_label)

    if stats_label != null:
        stats_label.text = "🏠 %d   •   ✓ %d" % [houses.size(), _completed_count()]

func _make_task_card(task: Dictionary) -> Control:
    var card := PanelContainer.new()
    card.custom_minimum_size = Vector2(0, 122)
    card.add_theme_stylebox_override("panel", _panel_style(Color(0.13, 0.18, 0.24, 0.95), 16.0))

    var v := VBoxContainer.new()
    v.add_theme_constant_override("separation", 7)
    card.add_child(v)

    var title := Label.new()
    title.text = String(task.get("title", "مهمة"))
    title.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
    title.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    title.add_theme_font_size_override("font_size", 19)
    title.add_theme_color_override("font_color", Color.WHITE)
    v.add_child(title)

    var remaining := maxi(0, int(task.get("deadline", 0)) - int(Time.get_unix_time_from_system()))
    var timer := Label.new()
    timer.text = "⏱ " + _format_remaining(remaining)
    timer.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
    timer.add_theme_font_size_override("font_size", 17)
    timer.add_theme_color_override("font_color", Color("#ffd188"))
    v.add_child(timer)

    var done := Button.new()
    done.text = "تمت المهمة  ✓"
    done.custom_minimum_size = Vector2(0, 42)
    done.add_theme_font_size_override("font_size", 17)
    done.add_theme_color_override("font_color", Color.WHITE)
    done.add_theme_stylebox_override("normal", _panel_style(Color("#2c9d69"), 14.0))
    done.add_theme_stylebox_override("pressed", _panel_style(Color("#227c53"), 14.0))
    done.pressed.connect(_complete_task.bind(int(task.get("id", -1))))
    v.add_child(done)
    return card

func _complete_task(task_id: int) -> void:
    var found := false
    for task in tasks:
        if int(task.get("id", -1)) == task_id and String(task.get("status", "")) == STATUS_ACTIVE:
            task["status"] = STATUS_COMPLETED
            task["completed"] = int(Time.get_unix_time_from_system())
            found = true
            break
    if not found:
        return
    _build_house(true)
    _save_game()
    _refresh_ui()
    _toast("أحسنت! بُني منزل جديد 🏠")

func _resolve_expired_tasks() -> void:
    var now := int(Time.get_unix_time_from_system())
    var changed := false
    var failed_count := 0
    for task in tasks:
        if String(task.get("status", "")) == STATUS_ACTIVE and now >= int(task.get("deadline", 0)):
            task["status"] = STATUS_FAILED
            task["failed"] = now
            failed_count += 1
            changed = true
            _demolish_last_house(true)
    if changed:
        _save_game()
        if failed_count == 1:
            _toast("انتهى وقت المهمة؛ هُدم منزل")
        else:
            _toast("انتهى وقت %d مهام" % failed_count)

func _build_house(animated: bool) -> void:
    var plot_index := _next_free_plot()
    if plot_index < 0:
        _toast("امتلأت المنطقة الحالية")
        return
    var variant := randi() % 6
    var record := {
        "plot": plot_index,
        "variant": variant,
        "built": int(Time.get_unix_time_from_system())
    }
    houses.append(record)
    var node := _create_house(plot_index, variant)
    house_nodes[plot_index] = node
    if animated:
        node.scale = Vector3(0.1, 0.1, 0.1)
        var tween := create_tween().set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
        tween.tween_property(node, "scale", Vector3.ONE, 0.55)

func _demolish_last_house(animated: bool) -> void:
    if houses.is_empty():
        return
    var record: Dictionary = houses.pop_back()
    var plot_index := int(record.get("plot", -1))
    if not house_nodes.has(plot_index):
        return
    var node: Node3D = house_nodes[plot_index]
    house_nodes.erase(plot_index)
    if animated and is_instance_valid(node):
        var tween := create_tween().set_parallel(true)
        tween.tween_property(node, "scale", Vector3(0.05, 0.05, 0.05), 0.52).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_IN)
        tween.tween_property(node, "rotation_degrees:z", 18.0, 0.52)
        tween.finished.connect(func():
            if is_instance_valid(node):
                node.queue_free()
        )
    elif is_instance_valid(node):
        node.queue_free()

func _next_free_plot() -> int:
    var occupied: Dictionary = {}
    for house in houses:
        occupied[int(house.get("plot", -1))] = true
    for i in range(plot_positions.size()):
        if not occupied.has(i):
            return i
    return -1

func _rebuild_saved_houses() -> void:
    for record in houses:
        var plot_index := int(record.get("plot", -1))
        if plot_index >= 0 and plot_index < plot_positions.size():
            var node := _create_house(plot_index, int(record.get("variant", 0)))
            house_nodes[plot_index] = node

func _create_house(plot_index: int, variant: int) -> Node3D:
    var root := Node3D.new()
    root.name = "House_%02d" % plot_index
    root.position = plot_positions[plot_index]
    add_child(root)

    var wall_colors := [
        Color("#f1d4a8"), Color("#f0c7b4"), Color("#dce6c5"),
        Color("#d8e5ef"), Color("#eadfbc"), Color("#ded1e8")
    ]
    var roof_colors := [
        Color("#d95845"), Color("#3d6f9e"), Color("#3f8a66"),
        Color("#a94c3f"), Color("#495f8c"), Color("#c47736")
    ]
    var wall_mat := _material(wall_colors[variant % wall_colors.size()], 0.9)
    var roof_mat := _material(roof_colors[variant % roof_colors.size()], 0.82)
    var trim_mat := _material(Color("#f7f1e3"), 0.88)

    var w := 4.8 + float(variant % 2) * 0.55
    var d := 4.6 + float((variant / 2) % 2) * 0.45
    _box(root, Vector3(w + 0.3, 0.28, d + 0.3), Vector3(0, 0.14, 0), trim_mat)
    _box(root, Vector3(w, 2.5, d), Vector3(0, 1.52, 0), wall_mat)

    var roof_left := _box(root, Vector3(w * 0.58, 0.24, d + 0.55), Vector3(-w * 0.20, 2.98, 0), roof_mat)
    roof_left.rotation_degrees.z = -24.0
    var roof_right := _box(root, Vector3(w * 0.58, 0.24, d + 0.55), Vector3(w * 0.20, 2.98, 0), roof_mat)
    roof_right.rotation_degrees.z = 24.0

    _box(root, Vector3(0.92, 1.55, 0.16), Vector3(0, 1.1, d * 0.5 + 0.08), door_mat)
    _box(root, Vector3(1.05, 0.83, 0.12), Vector3(-1.48, 1.62, d * 0.5 + 0.10), window_mat)
    _box(root, Vector3(1.05, 0.83, 0.12), Vector3(1.48, 1.62, d * 0.5 + 0.10), window_mat)
    _box(root, Vector3(0.1, 0.95, 0.15), Vector3(-1.48, 1.62, d * 0.5 + 0.17), trim_mat)
    _box(root, Vector3(1.18, 0.1, 0.15), Vector3(-1.48, 1.62, d * 0.5 + 0.17), trim_mat)
    _box(root, Vector3(0.1, 0.95, 0.15), Vector3(1.48, 1.62, d * 0.5 + 0.17), trim_mat)
    _box(root, Vector3(1.18, 0.1, 0.15), Vector3(1.48, 1.62, d * 0.5 + 0.17), trim_mat)

    if variant % 3 == 0:
        _box(root, Vector3(1.7, 0.22, 1.2), Vector3(0, 0.2, d * 0.5 + 0.8), trim_mat)
    elif variant % 3 == 1:
        _box(root, Vector3(2.0, 0.18, 0.9), Vector3(0, 2.5, d * 0.5 + 0.35), roof_mat)

    return root

func _create_tree(parent: Node3D, pos: Vector3, scale_factor: float) -> void:
    var root := Node3D.new()
    root.position = pos
    root.scale = Vector3.ONE * scale_factor
    parent.add_child(root)

    _box(root, Vector3(0.45, 1.65, 0.45), Vector3(0, 0.82, 0), trunk_mat)
    var canopy := MeshInstance3D.new()
    var sphere := SphereMesh.new()
    sphere.radius = 1.2
    sphere.height = 2.2
    sphere.radial_segments = 8
    sphere.rings = 5
    canopy.mesh = sphere
    canopy.material_override = leaf_mat
    canopy.position = Vector3(0, 2.25, 0)
    canopy.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_ON
    root.add_child(canopy)

func _box(parent: Node3D, size: Vector3, pos: Vector3, material: Material) -> MeshInstance3D:
    var mesh_instance := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = size
    mesh_instance.mesh = mesh
    mesh_instance.position = pos
    mesh_instance.material_override = material
    mesh_instance.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_ON
    parent.add_child(mesh_instance)
    return mesh_instance

func _material(color: Color, roughness: float) -> StandardMaterial3D:
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    mat.roughness = roughness
    return mat

func _panel_style(color: Color, radius: float) -> StyleBoxFlat:
    var style := StyleBoxFlat.new()
    style.bg_color = color
    var r := int(radius)
    style.corner_radius_top_left = r
    style.corner_radius_top_right = r
    style.corner_radius_bottom_left = r
    style.corner_radius_bottom_right = r
    style.content_margin_left = 18.0
    style.content_margin_right = 18.0
    style.content_margin_top = 12.0
    style.content_margin_bottom = 12.0
    return style

func _completed_count() -> int:
    var count := 0
    for task in tasks:
        if String(task.get("status", "")) == STATUS_COMPLETED:
            count += 1
    return count

func _format_remaining(seconds: int) -> String:
    if seconds >= 86400:
        var days := seconds / 86400
        var hours := (seconds % 86400) / 3600
        return "%d يوم %d س" % [days, hours]
    var h := seconds / 3600
    var m := (seconds % 3600) / 60
    var s := seconds % 60
    return "%02d:%02d:%02d" % [h, m, s]

func _save_game() -> void:
    var data := {
        "version": 1,
        "next_task_id": next_task_id,
        "tasks": tasks,
        "houses": houses
    }
    var file := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
    if file != null:
        file.store_string(JSON.stringify(data))

func _load_game() -> void:
    if not FileAccess.file_exists(SAVE_PATH):
        return
    var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
    if file == null:
        return
    var parsed = JSON.parse_string(file.get_as_text())
    if typeof(parsed) != TYPE_DICTIONARY:
        return
    tasks = parsed.get("tasks", [])
    houses = parsed.get("houses", [])
    next_task_id = int(parsed.get("next_task_id", 1))

func _toast(message: String) -> void:
    if toast_label == null:
        return
    toast_label.text = message
    toast_label.visible = true
    toast_label.modulate.a = 1.0
    var tween := create_tween()
    tween.tween_interval(1.35)
    tween.tween_property(toast_label, "modulate:a", 0.0, 0.45)
    tween.finished.connect(_hide_toast)

func _hide_toast() -> void:
    if toast_label != null:
        toast_label.visible = false
        toast_label.modulate.a = 1.0

func _reset_camera() -> void:
    camera_rig.position = Vector3.ZERO
    camera.position = Vector3(18.0, 23.0, 22.0)
    camera.look_at(Vector3.ZERO, Vector3.UP)

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventScreenTouch:
        var touch := event as InputEventScreenTouch
        if touch.pressed:
            touch_points[touch.index] = touch.position
        else:
            touch_points.erase(touch.index)
        if touch_points.size() < 2:
            last_pinch_distance = -1.0

    elif event is InputEventScreenDrag:
        var drag := event as InputEventScreenDrag
        touch_points[drag.index] = drag.position
        if touch_points.size() == 1:
            _pan_camera(drag.relative)
        elif touch_points.size() >= 2:
            var keys := touch_points.keys()
            var p1: Vector2 = touch_points[keys[0]]
            var p2: Vector2 = touch_points[keys[1]]
            var distance := p1.distance_to(p2)
            if last_pinch_distance > 0.0:
                _zoom_camera(distance - last_pinch_distance)
            last_pinch_distance = distance

    elif event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
        _pan_camera((event as InputEventMouseMotion).relative)

    elif event is InputEventMouseButton:
        var mb := event as InputEventMouseButton
        if mb.pressed and mb.button_index == MOUSE_BUTTON_WHEEL_UP:
            _zoom_camera(35.0)
        elif mb.pressed and mb.button_index == MOUSE_BUTTON_WHEEL_DOWN:
            _zoom_camera(-35.0)

func _pan_camera(delta_pixels: Vector2) -> void:
    if camera == null:
        return
    var right := camera.global_transform.basis.x
    right.y = 0.0
    right = right.normalized()
    var forward := -camera.global_transform.basis.z
    forward.y = 0.0
    forward = forward.normalized()
    var altitude_factor := clampf(camera.position.y / 23.0, 0.65, 1.6)
    var movement := (-delta_pixels.x * right + delta_pixels.y * forward) * 0.018 * altitude_factor
    camera_rig.position += movement
    camera_rig.position.x = clampf(camera_rig.position.x, -16.0, 16.0)
    camera_rig.position.z = clampf(camera_rig.position.z, -16.0, 16.0)

func _zoom_camera(pinch_delta: float) -> void:
    if camera == null:
        return
    var direction := camera.position.normalized()
    var step := -pinch_delta * 0.012
    var distance := camera.position.length()
    var new_distance := clampf(distance + step, 18.0, 42.0)
    camera.position = direction * new_distance
    camera.look_at(Vector3.ZERO, Vector3.UP)
