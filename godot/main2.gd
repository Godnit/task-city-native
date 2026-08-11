extends Node

const SAVE_PATH := "user://task_city_v12.json"
const PLOTS := [
	Vector3(-15, 0, -15), Vector3(-5, 0, -15), Vector3(5, 0, -15), Vector3(15, 0, -15),
	Vector3(-15, 0, -5), Vector3(-5, 0, -5), Vector3(5, 0, -5), Vector3(15, 0, -5),
	Vector3(-15, 0, 5), Vector3(-5, 0, 5), Vector3(5, 0, 5), Vector3(15, 0, 5),
	Vector3(-15, 0, 15), Vector3(-5, 0, 15), Vector3(5, 0, 15), Vector3(15, 0, 15)
]

var world: Node3D
var camera: Camera3D
var house_root: Node3D
var tree_root: Node3D
var camera_target: Vector3 = Vector3.ZERO
var camera_size: float = 35.0
var touches: Dictionary = {}
var pinch_previous: float = 0.0

var tasks: Array = []
var houses_data: Array = []
var next_task_id: int = 1
var next_house_id: int = 1
var second_accum: float = 0.0

var house_count_label: Label
var active_count_label: Label
var task_panel: PanelContainer
var panel_title: Label
var list_box: VBoxContainer
var panel_mode: String = ""
var add_dialog: ConfirmationDialog
var task_title_edit: LineEdit
var minutes_spin: SpinBox

func _ready() -> void:
	_build_world()
	_build_ui()
	_load_state()
	_rebuild_houses(false)
	_resolve_expired(false)
	_refresh_ui()

func _process(delta: float) -> void:
	second_accum += delta
	if second_accum >= 1.0:
		second_accum = 0.0
		_resolve_expired(true)
		_refresh_ui()

func _build_world() -> void:
	world = Node3D.new()
	world.name = "World"
	add_child(world)

	var environment_node: WorldEnvironment = WorldEnvironment.new()
	var environment: Environment = Environment.new()
	environment.background_mode = Environment.BG_COLOR
	environment.background_color = Color("64c9f0")
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color("cde5ed")
	environment.ambient_light_energy = 0.33
	environment.tonemap_mode = Environment.TONE_MAPPER_FILMIC
	environment_node.environment = environment
	world.add_child(environment_node)

	var sun: DirectionalLight3D = DirectionalLight3D.new()
	sun.name = "SunLight"
	sun.light_color = Color("fff1c6")
	sun.light_energy = 1.55
	sun.shadow_enabled = true
	sun.directional_shadow_max_distance = 95.0
	sun.rotation_degrees = Vector3(-52.0, -38.0, 0.0)
	world.add_child(sun)

	_create_box(world, Vector3(0, -0.38, 0), Vector3(58, 0.45, 58), Color("4d9e46"))
	_create_box(world, Vector3(0, -0.13, 0), Vector3(55, 0.16, 55), Color("78d360"))

	tree_root = Node3D.new()
	tree_root.name = "Trees"
	world.add_child(tree_root)
	_build_chunky_trees()

	house_root = Node3D.new()
	house_root.name = "Houses"
	world.add_child(house_root)

	camera = Camera3D.new()
	camera.name = "Camera"
	camera.projection = Camera3D.PROJECTION_ORTHOGONAL
	camera.size = camera_size
	camera.position = Vector3(28, 31, 28)
	camera.current = true
	world.add_child(camera)
	_update_camera()

func _build_chunky_trees() -> void:
	var positions: Array = [
		Vector3(-24,0,-22),Vector3(-24,0,-13),Vector3(-24,0,-3),Vector3(-24,0,8),Vector3(-24,0,20),
		Vector3(24,0,-22),Vector3(24,0,-12),Vector3(24,0,-1),Vector3(24,0,10),Vector3(24,0,21),
		Vector3(-18,0,-24),Vector3(-8,0,-24),Vector3(3,0,-24),Vector3(14,0,-24),
		Vector3(-17,0,24),Vector3(-7,0,24),Vector3(4,0,24),Vector3(16,0,24),
		Vector3(-20,0,0),Vector3(20,0,4)
	]
	for index in range(positions.size()):
		var tree_position: Vector3 = positions[index]
		var scale_value: float = 0.90 + float(index % 3) * 0.10
		_create_tree(tree_position, scale_value)

func _create_tree(position_value: Vector3, scale_value: float) -> void:
	var tree: Node3D = Node3D.new()
	tree.position = position_value
	tree.scale = Vector3.ONE * scale_value
	tree_root.add_child(tree)
	_create_box(tree, Vector3(0,0.70,0), Vector3(0.55,1.45,0.55), Color("75472f"))
	_create_box(tree, Vector3(0,2.00,0), Vector3(2.25,1.75,2.10), Color("49a84c"))
	_create_box(tree, Vector3(-0.48,2.65,0.20), Vector3(1.45,1.35,1.45), Color("63bf55"))
	_create_box(tree, Vector3(0.55,2.55,-0.30), Vector3(1.35,1.25,1.35), Color("58b94f"))

func _create_box(parent: Node, position_value: Vector3, size_value: Vector3, color_value: Color, rotation_value: Vector3 = Vector3.ZERO) -> MeshInstance3D:
	var mesh_instance: MeshInstance3D = MeshInstance3D.new()
	var box_mesh: BoxMesh = BoxMesh.new()
	box_mesh.size = size_value
	mesh_instance.mesh = box_mesh
	mesh_instance.position = position_value
	mesh_instance.rotation_degrees = rotation_value
	var material: StandardMaterial3D = StandardMaterial3D.new()
	material.albedo_color = color_value
	material.roughness = 0.88
	mesh_instance.material_override = material
	parent.add_child(mesh_instance)
	return mesh_instance

func _rebuild_houses(animate: bool) -> void:
	for child in house_root.get_children():
		house_root.remove_child(child)
		child.queue_free()
	for house_variant in houses_data:
		var house: Dictionary = house_variant
		var plot: int = int(house.get("plot", -1))
		if plot < 0 or plot >= PLOTS.size():
			continue
		var node: Node3D = _make_house(PLOTS[plot], int(house.get("variant", 0)))
		node.set_meta("house_id", int(house.get("id", 0)))
		if animate:
			node.scale = Vector3.ONE * 0.05
			var tween: Tween = create_tween()
			tween.set_trans(Tween.TRANS_BACK)
			tween.set_ease(Tween.EASE_OUT)
			tween.tween_property(node, "scale", Vector3.ONE, 0.55)

func _make_house(position_value: Vector3, variant: int) -> Node3D:
	var root: Node3D = Node3D.new()
	root.position = position_value
	house_root.add_child(root)
	var wall_colors: Array = [Color("f3dfb9"),Color("eee8d8"),Color("efcb82"),Color("d9e4cf"),Color("e6d5b8"),Color("f0dfc8")]
	var roof_colors: Array = [Color("df5938"),Color("2d86b8"),Color("319272"),Color("c94b37"),Color("287aa4"),Color("e36d38")]
	var wall_color: Color = wall_colors[variant % wall_colors.size()]
	var roof_color: Color = roof_colors[variant % roof_colors.size()]

	_create_box(root, Vector3(0,-0.01,0), Vector3(7.2,0.12,7.0), Color("72c95e"))
	_create_box(root, Vector3(0,0.05,0.25), Vector3(5.8,0.12,5.4), Color("b6df8b"))
	_create_box(root, Vector3(0,1.65,0), Vector3(4.7,3.15,4.0), wall_color)

	_create_box(root, Vector3(0,0.30,2.35), Vector3(2.7,0.22,1.25), Color("e5d4b0"))
	_create_box(root, Vector3(-1.05,1.15,2.35), Vector3(0.22,1.75,0.22), Color("f4ead4"))
	_create_box(root, Vector3(1.05,1.15,2.35), Vector3(0.22,1.75,0.22), Color("f4ead4"))
	_create_box(root, Vector3(0,2.05,2.35), Vector3(2.65,0.24,1.15), Color("f4ead4"))

	_create_box(root, Vector3(-1.05,3.65,0), Vector3(2.65,0.30,4.70), roof_color, Vector3(0,0,-31))
	_create_box(root, Vector3(1.05,3.65,0), Vector3(2.65,0.30,4.70), roof_color, Vector3(0,0,31))

	_create_box(root, Vector3(0,1.05,2.03), Vector3(0.82,1.78,0.13), Color("925531"))
	_create_window(root, Vector3(-1.38,1.55,2.04), Vector3(0.82,0.90,0.14))
	_create_window(root, Vector3(1.38,1.55,2.04), Vector3(0.82,0.90,0.14))
	_create_window(root, Vector3(2.36,1.55,-0.75), Vector3(0.14,0.90,0.86))
	_create_window(root, Vector3(-2.36,1.55,0.65), Vector3(0.14,0.90,0.86))
	_create_box(root, Vector3(1.25,4.15,-0.75), Vector3(0.55,1.05,0.55), Color("a86248"))
	_create_box(root, Vector3(-0.90,3.92,1.30), Vector3(0.85,0.70,0.75), wall_color)
	_create_window(root, Vector3(-0.90,4.00,1.69), Vector3(0.48,0.42,0.10))
	_build_fence(root)
	return root

func _create_window(parent: Node, position_value: Vector3, size_value: Vector3) -> void:
	_create_box(parent, position_value, size_value, Color("b8e5f3"))

func _build_fence(root: Node3D) -> void:
	var white: Color = Color("f7f2e6")
	_create_box(root, Vector3(0,0.35,3.22), Vector3(6.35,0.18,0.14), white)
	_create_box(root, Vector3(0,0.35,-3.22), Vector3(6.35,0.18,0.14), white)
	_create_box(root, Vector3(3.18,0.35,0), Vector3(0.14,0.18,6.35), white)
	_create_box(root, Vector3(-3.18,0.35,0), Vector3(0.14,0.18,6.35), white)
	var fence_points: Array = [-2.7,-1.8,-0.9,0.9,1.8,2.7]
	for x_variant in fence_points:
		var x_value: float = float(x_variant)
		_create_box(root, Vector3(x_value,0.55,3.22), Vector3(0.12,0.75,0.14), white)
		_create_box(root, Vector3(x_value,0.55,-3.22), Vector3(0.12,0.75,0.14), white)

func _build_ui() -> void:
	var canvas: CanvasLayer = CanvasLayer.new()
	canvas.layer = 10
	add_child(canvas)
	var ui: Control = Control.new()
	ui.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	canvas.add_child(ui)

	var sun_label: Label = Label.new()
	sun_label.text = "☀"
	sun_label.add_theme_font_size_override("font_size", 78)
	sun_label.add_theme_color_override("font_color", Color("ffd75c"))
	sun_label.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	sun_label.offset_left = -118
	sun_label.offset_right = -22
	sun_label.offset_top = 170
	sun_label.offset_bottom = 270
	ui.add_child(sun_label)

	var top_panel: PanelContainer = PanelContainer.new()
	top_panel.set_anchors_preset(Control.PRESET_TOP_WIDE)
	top_panel.offset_left = 20
	top_panel.offset_right = -20
	top_panel.offset_top = 20
	top_panel.offset_bottom = 170
	top_panel.add_theme_stylebox_override("panel", _rounded_style(Color(0.97,0.99,1.0,0.95), 28))
	ui.add_child(top_panel)
	var top_box: VBoxContainer = VBoxContainer.new()
	top_box.layout_direction = Control.LAYOUT_DIRECTION_RTL
	top_box.add_theme_constant_override("separation", 10)
	top_panel.add_child(top_box)
	var title_row: HBoxContainer = HBoxContainer.new()
	title_row.layout_direction = Control.LAYOUT_DIRECTION_RTL
	top_box.add_child(title_row)
	var title: Label = Label.new()
	title.text = "مدينة الإنجاز"
	title.add_theme_font_size_override("font_size", 30)
	title.add_theme_color_override("font_color", Color("173947"))
	title.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	title_row.add_child(title)
	var reset_button: Button = _make_button("⌂", false)
	reset_button.custom_minimum_size = Vector2(64,48)
	reset_button.pressed.connect(_reset_camera)
	title_row.add_child(reset_button)

	var stats: HBoxContainer = HBoxContainer.new()
	stats.layout_direction = Control.LAYOUT_DIRECTION_RTL
	stats.add_theme_constant_override("separation", 10)
	top_box.add_child(stats)
	house_count_label = _make_chip("🏠 0 بيت")
	active_count_label = _make_chip("⏱ 0 مهمة نشطة")
	stats.add_child(house_count_label)
	stats.add_child(active_count_label)

	var bottom_panel: PanelContainer = PanelContainer.new()
	bottom_panel.set_anchors_preset(Control.PRESET_BOTTOM_WIDE)
	bottom_panel.offset_left = 20
	bottom_panel.offset_right = -20
	bottom_panel.offset_top = -125
	bottom_panel.offset_bottom = -20
	bottom_panel.add_theme_stylebox_override("panel", _rounded_style(Color(0.97,0.99,1.0,0.96), 28))
	ui.add_child(bottom_panel)
	var nav: HBoxContainer = HBoxContainer.new()
	nav.layout_direction = Control.LAYOUT_DIRECTION_RTL
	nav.add_theme_constant_override("separation", 8)
	bottom_panel.add_child(nav)

	var add_button: Button = _make_button("＋ مهمة", true)
	var tasks_button: Button = _make_button("المهام", false)
	var history_button: Button = _make_button("السجل", false)
	var home_button: Button = _make_button("⌂", false)
	var nav_buttons: Array = [add_button,tasks_button,history_button,home_button]
	for button_variant in nav_buttons:
		var button: Button = button_variant
		button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		button.custom_minimum_size = Vector2(0,78)
		nav.add_child(button)
	add_button.pressed.connect(_show_add_dialog)
	tasks_button.pressed.connect(_show_tasks_panel)
	history_button.pressed.connect(_show_history_panel)
	home_button.pressed.connect(_reset_camera)

	task_panel = PanelContainer.new()
	task_panel.visible = false
	task_panel.set_anchors_preset(Control.PRESET_BOTTOM_WIDE)
	task_panel.offset_left = 20
	task_panel.offset_right = -20
	task_panel.offset_top = -650
	task_panel.offset_bottom = -140
	task_panel.add_theme_stylebox_override("panel", _rounded_style(Color(0.98,0.99,1.0,0.98), 25))
	ui.add_child(task_panel)
	var panel_vbox: VBoxContainer = VBoxContainer.new()
	panel_vbox.layout_direction = Control.LAYOUT_DIRECTION_RTL
	panel_vbox.add_theme_constant_override("separation", 8)
	task_panel.add_child(panel_vbox)
	var panel_header: HBoxContainer = HBoxContainer.new()
	panel_header.layout_direction = Control.LAYOUT_DIRECTION_RTL
	panel_vbox.add_child(panel_header)
	panel_title = Label.new()
	panel_title.add_theme_font_size_override("font_size", 24)
	panel_title.add_theme_color_override("font_color", Color("173947"))
	panel_title.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	panel_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	panel_header.add_child(panel_title)
	var close_button: Button = _make_button("✕", false)
	close_button.custom_minimum_size = Vector2(58,45)
	close_button.pressed.connect(_hide_panel)
	panel_header.add_child(close_button)
	var scroll: ScrollContainer = ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	panel_vbox.add_child(scroll)
	list_box = VBoxContainer.new()
	list_box.layout_direction = Control.LAYOUT_DIRECTION_RTL
	list_box.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	list_box.add_theme_constant_override("separation", 8)
	scroll.add_child(list_box)

	add_dialog = ConfirmationDialog.new()
	add_dialog.title = "مهمة جديدة"
	add_dialog.ok_button_text = "ابدأ المهمة"
	add_dialog.cancel_button_text = "إلغاء"
	add_dialog.min_size = Vector2i(580,360)
	add_child(add_dialog)
	var form: VBoxContainer = VBoxContainer.new()
	form.layout_direction = Control.LAYOUT_DIRECTION_RTL
	form.add_theme_constant_override("separation", 14)
	add_dialog.add_child(form)
	var name_label: Label = Label.new()
	name_label.text = "اسم المهمة"
	name_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	form.add_child(name_label)
	task_title_edit = LineEdit.new()
	task_title_edit.placeholder_text = "مثال: مراجعة فصل الأحياء"
	task_title_edit.alignment = HORIZONTAL_ALIGNMENT_RIGHT
	form.add_child(task_title_edit)
	var duration_label: Label = Label.new()
	duration_label.text = "المدة بالدقائق"
	duration_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	form.add_child(duration_label)
	minutes_spin = SpinBox.new()
	minutes_spin.min_value = 1
	minutes_spin.max_value = 10080
	minutes_spin.step = 1
	minutes_spin.value = 30
	minutes_spin.suffix = " دقيقة"
	form.add_child(minutes_spin)
	add_dialog.confirmed.connect(_add_task)

func _rounded_style(color_value: Color, radius: int) -> StyleBoxFlat:
	var style: StyleBoxFlat = StyleBoxFlat.new()
	style.bg_color = color_value
	style.corner_radius_top_left = radius
	style.corner_radius_top_right = radius
	style.corner_radius_bottom_left = radius
	style.corner_radius_bottom_right = radius
	style.content_margin_left = 14
	style.content_margin_right = 14
	style.content_margin_top = 12
	style.content_margin_bottom = 12
	return style

func _make_button(text_value: String, primary: bool) -> Button:
	var button: Button = Button.new()
	button.text = text_value
	button.add_theme_font_size_override("font_size", 20)
	if primary:
		button.add_theme_color_override("font_color", Color.WHITE)
		button.add_theme_stylebox_override("normal", _rounded_style(Color("1eaf82"), 20))
		button.add_theme_stylebox_override("pressed", _rounded_style(Color("168c6a"), 20))
	else:
		button.add_theme_color_override("font_color", Color("173947"))
		button.add_theme_stylebox_override("normal", _rounded_style(Color("eef6f9"), 20))
		button.add_theme_stylebox_override("pressed", _rounded_style(Color("dfeef3"), 20))
	return button

func _make_chip(text_value: String) -> Label:
	var label: Label = Label.new()
	label.text = text_value
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	label.add_theme_font_size_override("font_size", 19)
	label.add_theme_color_override("font_color", Color("224f50"))
	label.add_theme_stylebox_override("normal", _rounded_style(Color("e8f7ee"), 22))
	label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	label.custom_minimum_size = Vector2(0,52)
	return label

func _show_add_dialog() -> void:
	task_title_edit.text = ""
	minutes_spin.value = 30
	add_dialog.popup_centered(Vector2i(600,400))

func _add_task() -> void:
	var title_value: String = task_title_edit.text.strip_edges()
	if title_value.is_empty():
		return
	var now: int = int(Time.get_unix_time_from_system())
	var minutes_value: int = int(minutes_spin.value)
	var task: Dictionary = {
		"id": next_task_id,
		"title": title_value.substr(0,60),
		"created": now,
		"deadline": now + minutes_value * 60,
		"status": "active"
	}
	tasks.append(task)
	next_task_id += 1
	_save_state()
	_refresh_ui()

func _show_tasks_panel() -> void:
	if task_panel.visible and panel_mode == "tasks":
		_hide_panel()
		return
	panel_mode = "tasks"
	task_panel.visible = true
	_render_panel()

func _show_history_panel() -> void:
	if task_panel.visible and panel_mode == "history":
		_hide_panel()
		return
	panel_mode = "history"
	task_panel.visible = true
	_render_panel()

func _hide_panel() -> void:
	task_panel.visible = false
	panel_mode = ""

func _clear_list() -> void:
	for child in list_box.get_children():
		list_box.remove_child(child)
		child.queue_free()

func _render_panel() -> void:
	_clear_list()
	if panel_mode == "tasks":
		panel_title.text = "المهام النشطة"
		var found: bool = false
		for task_variant in tasks:
			var task: Dictionary = task_variant
			if String(task.get("status", "")) == "active":
				found = true
				_add_task_row(task, true)
		if not found:
			_add_empty("لا توجد مهام نشطة.\nأضف مهمة وابدأ بناء مدينتك 🌱")
	else:
		panel_title.text = "سجل المهام"
		if tasks.is_empty():
			_add_empty("سجل المهام فارغ حتى الآن.")
		else:
			for index in range(tasks.size() - 1, -1, -1):
				var history_task: Dictionary = tasks[index]
				_add_task_row(history_task, false)

func _add_task_row(task: Dictionary, active_mode: bool) -> void:
	var card: PanelContainer = PanelContainer.new()
	card.add_theme_stylebox_override("panel", _rounded_style(Color("f1f8f4"), 18))
	list_box.add_child(card)
	var vbox: VBoxContainer = VBoxContainer.new()
	vbox.layout_direction = Control.LAYOUT_DIRECTION_RTL
	card.add_child(vbox)
	var name_label: Label = Label.new()
	name_label.text = String(task.get("title", ""))
	name_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	name_label.add_theme_font_size_override("font_size", 19)
	name_label.add_theme_color_override("font_color", Color("1c4844"))
	vbox.add_child(name_label)
	if active_mode:
		var deadline: int = int(task.get("deadline", 0))
		var now: int = int(Time.get_unix_time_from_system())
		var remaining: int = maxi(0, deadline - now)
		var remaining_label: Label = Label.new()
		remaining_label.text = "الوقت المتبقي: " + _format_time(remaining)
		remaining_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
		remaining_label.add_theme_color_override("font_color", Color("376b61"))
		vbox.add_child(remaining_label)
		var done_button: Button = _make_button("✓ تم الإنجاز", true)
		done_button.custom_minimum_size = Vector2(0,50)
		var task_id: int = int(task.get("id", 0))
		done_button.pressed.connect(_on_done_pressed.bind(task_id))
		vbox.add_child(done_button)
	else:
		var status_value: String = String(task.get("status", ""))
		var status_label: Label = Label.new()
		if status_value == "done":
			status_label.text = "✓ منجزة"
			status_label.add_theme_color_override("font_color", Color("159365"))
		elif status_value == "failed":
			status_label.text = "انتهى الوقت"
			status_label.add_theme_color_override("font_color", Color("c95749"))
		else:
			status_label.text = "نشطة"
			status_label.add_theme_color_override("font_color", Color("557178"))
		status_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
		vbox.add_child(status_label)

func _add_empty(text_value: String) -> void:
	var label: Label = Label.new()
	label.text = text_value
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	label.custom_minimum_size = Vector2(0,150)
	label.add_theme_font_size_override("font_size", 18)
	label.add_theme_color_override("font_color", Color("557178"))
	list_box.add_child(label)

func _on_done_pressed(task_id: int) -> void:
	_complete_task(task_id)

func _complete_task(task_id: int) -> void:
	var now: int = int(Time.get_unix_time_from_system())
	for task_variant in tasks:
		var task: Dictionary = task_variant
		if int(task.get("id", 0)) != task_id:
			continue
		if String(task.get("status", "")) != "active":
			return
		if now >= int(task.get("deadline", 0)):
			_resolve_expired(true)
			return
		task["status"] = "done"
		_build_house_for_task(task_id)
		_save_state()
		_refresh_ui()
		return

func _build_house_for_task(task_id: int) -> void:
	var occupied: Dictionary = {}
	for house_variant in houses_data:
		var house: Dictionary = house_variant
		occupied[int(house.get("plot", -1))] = true
	var free_plot: int = -1
	for index in range(PLOTS.size()):
		if not occupied.has(index):
			free_plot = index
			break
	if free_plot < 0:
		return
	var now: int = int(Time.get_unix_time_from_system())
	var new_house: Dictionary = {
		"id": next_house_id,
		"plot": free_plot,
		"variant": (next_house_id - 1) % 6,
		"task_id": task_id,
		"built_at": now
	}
	houses_data.append(new_house)
	next_house_id += 1
	var node: Node3D = _make_house(PLOTS[free_plot], int(new_house["variant"]))
	node.set_meta("house_id", int(new_house["id"]))
	node.scale = Vector3.ONE * 0.04
	var tween: Tween = create_tween()
	tween.set_trans(Tween.TRANS_BACK)
	tween.set_ease(Tween.EASE_OUT)
	tween.tween_property(node, "scale", Vector3.ONE, 0.62)

func _resolve_expired(animate: bool) -> void:
	var now: int = int(Time.get_unix_time_from_system())
	var changed: bool = false
	for task_variant in tasks:
		var task: Dictionary = task_variant
		if String(task.get("status", "")) == "active" and now >= int(task.get("deadline", 0)):
			task["status"] = "failed"
			_demolish_latest(animate)
			changed = true
	if changed:
		_save_state()
		_refresh_ui()

func _demolish_latest(animate: bool) -> void:
	if houses_data.is_empty():
		return
	var latest_variant: Variant = houses_data.pop_back()
	var latest: Dictionary = latest_variant
	var house_id: int = int(latest.get("id", 0))
	var target_node: Node3D = null
	for child in house_root.get_children():
		if int(child.get_meta("house_id", -1)) == house_id:
			target_node = child
			break
	if target_node == null:
		return
	if animate:
		var tween: Tween = create_tween()
		tween.set_trans(Tween.TRANS_QUAD)
		tween.set_ease(Tween.EASE_IN)
		tween.tween_property(target_node, "scale", Vector3.ONE * 0.02, 0.45)
		tween.finished.connect(target_node.queue_free)
	else:
		target_node.queue_free()

func _refresh_ui() -> void:
	var active_count: int = 0
	for task_variant in tasks:
		var task: Dictionary = task_variant
		if String(task.get("status", "")) == "active":
			active_count += 1
	house_count_label.text = "🏠 %d بيت" % houses_data.size()
	active_count_label.text = "⏱ %d مهمة نشطة" % active_count
	if task_panel.visible:
		_render_panel()

func _format_time(seconds_value: int) -> String:
	var hours: int = seconds_value / 3600
	var minutes: int = (seconds_value % 3600) / 60
	var seconds: int = seconds_value % 60
	if hours > 0:
		return "%02d:%02d:%02d" % [hours, minutes, seconds]
	return "%02d:%02d" % [minutes, seconds]

func _save_state() -> void:
	var data: Dictionary = {
		"tasks": tasks,
		"houses": houses_data,
		"next_task_id": next_task_id,
		"next_house_id": next_house_id
	}
	var file: FileAccess = FileAccess.open(SAVE_PATH, FileAccess.WRITE)
	if file != null:
		file.store_string(JSON.stringify(data))

func _load_state() -> void:
	if not FileAccess.file_exists(SAVE_PATH):
		return
	var file: FileAccess = FileAccess.open(SAVE_PATH, FileAccess.READ)
	if file == null:
		return
	var parsed: Variant = JSON.parse_string(file.get_as_text())
	if typeof(parsed) != TYPE_DICTIONARY:
		return
	var data: Dictionary = parsed
	tasks = data.get("tasks", [])
	houses_data = data.get("houses", [])
	next_task_id = int(data.get("next_task_id", 1))
	next_house_id = int(data.get("next_house_id", 1))

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		var touch_event: InputEventScreenTouch = event
		if touch_event.pressed:
			touches[touch_event.index] = touch_event.position
		else:
			touches.erase(touch_event.index)
		if touches.size() < 2:
			pinch_previous = 0.0
	elif event is InputEventScreenDrag:
		var drag_event: InputEventScreenDrag = event
		touches[drag_event.index] = drag_event.position
		if touches.size() >= 2:
			var values: Array = touches.values()
			var point_a: Vector2 = values[0]
			var point_b: Vector2 = values[1]
			var distance_value: float = point_a.distance_to(point_b)
			if pinch_previous > 0.0:
				camera_size = clampf(camera_size + (pinch_previous - distance_value) * 0.025, 20.0, 48.0)
				camera.size = camera_size
			pinch_previous = distance_value
		else:
			var viewport_height: float = float(get_viewport().get_visible_rect().size.y)
			var scale_factor: float = camera_size / maxf(500.0, viewport_height)
			camera_target.x -= drag_event.relative.x * scale_factor * 1.30
			camera_target.z -= drag_event.relative.y * scale_factor * 1.30
			camera_target.x = clampf(camera_target.x, -13.0, 13.0)
			camera_target.z = clampf(camera_target.z, -13.0, 13.0)
			_update_camera()

func _update_camera() -> void:
	if camera == null:
		return
	camera.position = camera_target + Vector3(28,31,28)
	camera.look_at(camera_target + Vector3(0,0.8,0), Vector3.UP)

func _reset_camera() -> void:
	camera_target = Vector3.ZERO
	camera_size = 35.0
	camera.size = camera_size
	_update_camera()
