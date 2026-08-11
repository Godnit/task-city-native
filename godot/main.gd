extends Node

const SAVE_PATH := "user://task_city_v12.json"
const PLOTS := [
	Vector3(-15, 0, -15), Vector3(-5, 0, -15), Vector3(5, 0, -15), Vector3(15, 0, -15),
	Vector3(-15, 0, -5),  Vector3(-5, 0, -5),  Vector3(5, 0, -5),  Vector3(15, 0, -5),
	Vector3(-15, 0, 5),   Vector3(-5, 0, 5),   Vector3(5, 0, 5),   Vector3(15, 0, 5),
	Vector3(-15, 0, 15),  Vector3(-5, 0, 15),  Vector3(5, 0, 15),  Vector3(15, 0, 15)
]

var world: Node3D
var camera: Camera3D
var house_root: Node3D
var tree_root: Node3D
var camera_target := Vector3.ZERO
var camera_size := 35.0
var touches := {}
var pinch_previous := 0.0

var tasks: Array = []
var houses_data: Array = []
var next_task_id := 1
var next_house_id := 1
var second_accum := 0.0

var house_count_label: Label
var active_count_label: Label
var task_panel: PanelContainer
var panel_title: Label
var list_box: VBoxContainer
var panel_mode := ""
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

	var env_node := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color("64c9f0")
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color("d8ecf2")
	env.ambient_light_energy = 0.38
	env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
	env_node.environment = env
	world.add_child(env_node)

	var sun := DirectionalLight3D.new()
	sun.name = "SunLight"
	sun.light_color = Color("fff1c7")
	sun.light_energy = 1.45
	sun.shadow_enabled = true
	sun.directional_shadow_max_distance = 90.0
	sun.rotation_degrees = Vector3(-52.0, -38.0, 0.0)
	world.add_child(sun)

	_create_box(world, Vector3(0, -0.36, 0), Vector3(58, 0.45, 58), Color("5aae4e"))
	_create_box(world, Vector3(0, -0.12, 0), Vector3(55, 0.16, 55), Color("77d25e"))

	# A few subtle lawn patches. No mountains or geometry above the map.
	_create_box(world, Vector3(-18, -0.01, 17), Vector3(8, 0.04, 7), Color("82d96a"))
	_create_box(world, Vector3(18, -0.01, -17), Vector3(7, 0.04, 8), Color("82d96a"))

	tree_root = Node3D.new()
	tree_root.name = "Trees"
	world.add_child(tree_root)
	_build_old_style_trees()

	house_root = Node3D.new()
	house_root.name = "Houses"
	world.add_child(house_root)

	camera = Camera3D.new()
	camera.name = "IsometricCamera"
	camera.projection = Camera3D.PROJECTION_ORTHOGONAL
	camera_size = 35.0
	camera.size = camera_size
	camera.position = Vector3(28, 31, 28)
	camera.current = true
	world.add_child(camera)
	_update_camera()

func _build_old_style_trees() -> void:
	# Deliberately returns to the chunkier trees from the first version, but with real Godot shadows.
	var positions := [
		Vector3(-24,0,-22),Vector3(-24,0,-13),Vector3(-24,0,-3),Vector3(-24,0,8),Vector3(-24,0,20),
		Vector3(24,0,-22),Vector3(24,0,-12),Vector3(24,0,-1),Vector3(24,0,10),Vector3(24,0,21),
		Vector3(-18,0,-24),Vector3(-8,0,-24),Vector3(3,0,-24),Vector3(14,0,-24),
		Vector3(-17,0,24),Vector3(-7,0,24),Vector3(4,0,24),Vector3(16,0,24),
		Vector3(-20,0,0),Vector3(20,0,4)
	]
	for i in range(positions.size()):
		_create_tree(positions[i], 0.90 + float(i % 3) * 0.10)

func _create_tree(pos: Vector3, scale_value: float) -> void:
	var n := Node3D.new()
	n.position = pos
	n.scale = Vector3.ONE * scale_value
	tree_root.add_child(n)
	_create_box(n, Vector3(0, 0.70, 0), Vector3(0.55, 1.45, 0.55), Color("75472f"))
	_create_box(n, Vector3(0, 2.00, 0), Vector3(2.25, 1.75, 2.10), Color("49a84c"))
	_create_box(n, Vector3(-0.48, 2.65, 0.20), Vector3(1.45, 1.35, 1.45), Color("63bf55"))
	_create_box(n, Vector3(0.55, 2.55, -0.30), Vector3(1.35, 1.25, 1.35), Color("58b94f"))

func _create_box(parent: Node, pos: Vector3, size: Vector3, color: Color, rotation_deg := Vector3.ZERO) -> MeshInstance3D:
	var mesh_instance := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = size
	mesh_instance.mesh = mesh
	mesh_instance.position = pos
	mesh_instance.rotation_degrees = rotation_deg
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	material.roughness = 0.86
	mesh_instance.material_override = material
	parent.add_child(mesh_instance)
	return mesh_instance

func _rebuild_houses(animate: bool) -> void:
	for child in house_root.get_children():
		house_root.remove_child(child)
		child.queue_free()
	for h in houses_data:
		var plot := int(h.get("plot", -1))
		if plot >= 0 and plot < PLOTS.size():
			var node := _make_house(PLOTS[plot], int(h.get("variant", 0)))
		node.set_meta("house_id", int(h.get("id", 0)))
		if animate:
			node.scale = Vector3.ONE * 0.05
			var tween := create_tween()
			tween.set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
			tween.tween_property(node, "scale", Vector3.ONE, 0.55)

func _make_house(pos: Vector3, variant: int) -> Node3D:
	var root := Node3D.new()
	root.position = pos
	house_root.add_child(root)
	var walls := [Color("f3dfb9"), Color("eee8d8"), Color("efcb82"), Color("d9e4cf"), Color("e6d5b8"), Color("f0dfc8")]
	var roofs := [Color("df5938"), Color("2d86b8"), Color("319272"), Color("c94b37"), Color("287aa4"), Color("e36d38")]
	var wall: Color = walls[variant % walls.size()]
	var roof: Color = roofs[variant % roofs.size()]

	# Yard/foundation
	_create_box(root, Vector3(0,-0.01,0), Vector3(7.2,0.12,7.0), Color("71c95d"))
	_create_box(root, Vector3(0,0.05,0.30), Vector3(5.8,0.12,5.4), Color("b6df8b"))

	# Body
	_create_box(root, Vector3(0,1.65,0), Vector3(4.7,3.15,4.0), wall)
	# Porch slab + columns
	_create_box(root, Vector3(0,0.30,2.35), Vector3(2.7,0.22,1.25), Color("e5d4b0"))
	_create_box(root, Vector3(-1.05,1.15,2.35), Vector3(0.22,1.75,0.22), Color("f4ead4"))
	_create_box(root, Vector3(1.05,1.15,2.35), Vector3(0.22,1.75,0.22), Color("f4ead4"))
	_create_box(root, Vector3(0,2.05,2.35), Vector3(2.65,0.24,1.15), Color("f4ead4"))

	# Real pitched roof made from two sloped meshes; each casts a true shadow.
	_create_box(root, Vector3(-1.05,3.65,0), Vector3(2.65,0.30,4.70), roof, Vector3(0,0,-31))
	_create_box(root, Vector3(1.05,3.65,0), Vector3(2.65,0.30,4.70), roof, Vector3(0,0,31))

	# Door and windows
	_create_box(root, Vector3(0,1.05,2.03), Vector3(0.82,1.78,0.13), Color("925531"))
	_create_window(root, Vector3(-1.38,1.55,2.04), Vector3(0.82,0.90,0.14))
	_create_window(root, Vector3(1.38,1.55,2.04), Vector3(0.82,0.90,0.14))
	_create_window(root, Vector3(2.36,1.55,-0.75), Vector3(0.14,0.90,0.86))
	_create_window(root, Vector3(-2.36,1.55,0.65), Vector3(0.14,0.90,0.86))

	# Chimney and small dormer detail
	_create_box(root, Vector3(1.25,4.15,-0.75), Vector3(0.55,1.05,0.55), Color("a86248"))
	_create_box(root, Vector3(-0.90,3.92,1.30), Vector3(0.85,0.70,0.75), wall)
	_create_window(root, Vector3(-0.90,4.00,1.69), Vector3(0.48,0.42,0.10))

	# Simple white fence, kept well within its own plot.
	_build_fence(root)
	return root

func _create_window(parent: Node, pos: Vector3, size: Vector3) -> void:
	_create_box(parent, pos, size, Color("b8e5f3"))

func _build_fence(root: Node3D) -> void:
	var white := Color("f7f2e6")
	_create_box(root, Vector3(0,0.35,3.22), Vector3(6.35,0.18,0.14), white)
	_create_box(root, Vector3(0,0.35,-3.22), Vector3(6.35,0.18,0.14), white)
	_create_box(root, Vector3(3.18,0.35,0), Vector3(0.14,0.18,6.35), white)
	_create_box(root, Vector3(-3.18,0.35,0), Vector3(0.14,0.18,6.35), white)
	for x in [-2.7,-1.8,-0.9,0.9,1.8,2.7]:
		_create_box(root, Vector3(x,0.55,3.22), Vector3(0.12,0.75,0.14), white)
		_create_box(root, Vector3(x,0.55,-3.22), Vector3(0.12,0.75,0.14), white)

func _build_ui() -> void:
	var canvas := CanvasLayer.new()
	canvas.layer = 10
	add_child(canvas)
	var ui := Control.new()
	ui.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	canvas.add_child(ui)

	# Visible sun disk/icon. It is paired with the actual DirectionalLight3D above.
	var sun_label := Label.new()
	sun_label.text = "☀"
	sun_label.add_theme_font_size_override("font_size", 78)
	sun_label.add_theme_color_override("font_color", Color("ffd75c"))
	sun_label.add_theme_color_override("font_shadow_color", Color(1,0.78,0.20,0.42))
	sun_label.add_theme_constant_override("shadow_offset_x", 4)
	sun_label.add_theme_constant_override("shadow_offset_y", 4)
	sun_label.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	sun_label.offset_left = -115
	sun_label.offset_right = -25
	sun_label.offset_top = 165
	sun_label.offset_bottom = 260
	ui.add_child(sun_label)

	var top_panel := PanelContainer.new()
	top_panel.set_anchors_preset(Control.PRESET_TOP_WIDE)
	top_panel.offset_left = 20
	top_panel.offset_right = -20
	top_panel.offset_top = 20
	top_panel.offset_bottom = 170
	top_panel.add_theme_stylebox_override("panel", _rounded_style(Color(0.97,0.99,1.0,0.95), 28))
	ui.add_child(top_panel)

	var top_box := VBoxContainer.new()
	top_box.layout_direction = Control.LAYOUT_DIRECTION_RTL
	top_box.add_theme_constant_override("separation", 10)
	top_panel.add_child(top_box)
	var title_row := HBoxContainer.new()
	title_row.layout_direction = Control.LAYOUT_DIRECTION_RTL
	top_box.add_child(title_row)
	var title := Label.new()
	title.text = "مدينة الإنجاز"
	title.add_theme_font_size_override("font_size", 30)
	title.add_theme_color_override("font_color", Color("173947"))
	title.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	title_row.add_child(title)
	var reset_btn := _make_button("⌂", false)
	reset_btn.custom_minimum_size = Vector2(64,48)
	reset_btn.pressed.connect(_reset_camera)
	title_row.add_child(reset_btn)

	var stats := HBoxContainer.new()
	stats.layout_direction = Control.LAYOUT_DIRECTION_RTL
	stats.add_theme_constant_override("separation", 10)
	top_box.add_child(stats)
	house_count_label = _make_chip("🏠 0 بيت")
	active_count_label = _make_chip("⏱ 0 مهمة نشطة")
	stats.add_child(house_count_label)
	stats.add_child(active_count_label)

	var bottom_panel := PanelContainer.new()
	bottom_panel.set_anchors_preset(Control.PRESET_BOTTOM_WIDE)
	bottom_panel.offset_left = 20
	bottom_panel.offset_right = -20
	bottom_panel.offset_top = -125
	bottom_panel.offset_bottom = -20
	bottom_panel.add_theme_stylebox_override("panel", _rounded_style(Color(0.97,0.99,1.0,0.96), 28))
	ui.add_child(bottom_panel)
	var nav := HBoxContainer.new()
	nav.layout_direction = Control.LAYOUT_DIRECTION_RTL
	nav.add_theme_constant_override("separation", 8)
	bottom_panel.add_child(nav)
	var add_btn := _make_button("＋ مهمة", true)
	var tasks_btn := _make_button("المهام", false)
	var history_btn := _make_button("السجل", false)
	var home_btn := _make_button("⌂", false)
	for b in [add_btn,tasks_btn,history_btn,home_btn]:
		b.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		b.custom_minimum_size = Vector2(0,78)
		nav.add_child(b)
	add_btn.pressed.connect(_show_add_dialog)
	tasks_btn.pressed.connect(func(): _toggle_panel("tasks"))
	history_btn.pressed.connect(func(): _toggle_panel("history"))
	home_btn.pressed.connect(_reset_camera)

	task_panel = PanelContainer.new()
	task_panel.visible = false
	task_panel.set_anchors_preset(Control.PRESET_BOTTOM_WIDE)
	task_panel.offset_left = 20
	task_panel.offset_right = -20
	task_panel.offset_top = -650
	task_panel.offset_bottom = -140
	task_panel.add_theme_stylebox_override("panel", _rounded_style(Color(0.98,0.99,1.0,0.98), 25))
	ui.add_child(task_panel)
	var panel_v := VBoxContainer.new()
	panel_v.layout_direction = Control.LAYOUT_DIRECTION_RTL
	panel_v.add_theme_constant_override("separation", 8)
	task_panel.add_child(panel_v)
	var ph := HBoxContainer.new()
	ph.layout_direction = Control.LAYOUT_DIRECTION_RTL
	panel_v.add_child(ph)
	panel_title = Label.new()
	panel_title.add_theme_font_size_override("font_size", 24)
	panel_title.add_theme_color_override("font_color", Color("173947"))
	panel_title.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	panel_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	ph.add_child(panel_title)
	var close_btn := _make_button("✕", false)
	close_btn.custom_minimum_size = Vector2(58,45)
	close_btn.pressed.connect(func(): task_panel.visible = false; panel_mode = "")
	ph.add_child(close_btn)
	var scroll := ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	panel_v.add_child(scroll)
	list_box = VBoxContainer.new()
	list_box.layout_direction = Control.LAYOUT_DIRECTION_RTL
	list_box.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	list_box.add_theme_constant_override("separation", 8)
	scroll.add_child(list_box)

	add_dialog = ConfirmationDialog.new()
	add_dialog.title = "مهمة جديدة"
	add_dialog.ok_button_text = "ابدأ المهمة"
	add_dialog.cancel_button_text = "إلغاء"
	add_dialog.min_size = Vector2i(580, 360)
	add_child(add_dialog)
	var form := VBoxContainer.new()
	form.layout_direction = Control.LAYOUT_DIRECTION_RTL
	form.add_theme_constant_override("separation", 14)
	add_dialog.add_child(form)
	var l1 := Label.new()
	l1.text = "اسم المهمة"
	l1.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	form.add_child(l1)
	task_title_edit = LineEdit.new()
	task_title_edit.placeholder_text = "مثال: مراجعة فصل الأحياء"
	task_title_edit.alignment = HORIZONTAL_ALIGNMENT_RIGHT
	form.add_child(task_title_edit)
	var l2 := Label.new()
	l2.text = "المدة بالدقائق"
	l2.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	form.add_child(l2)
	minutes_spin = SpinBox.new()
	minutes_spin.min_value = 1
	minutes_spin.max_value = 10080
	minutes_spin.step = 1
	minutes_spin.value = 30
	minutes_spin.suffix = " دقيقة"
	form.add_child(minutes_spin)
	add_dialog.confirmed.connect(_add_task)

func _rounded_style(color: Color, radius: int) -> StyleBoxFlat:
	var style := StyleBoxFlat.new()
	style.bg_color = color
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
	var b := Button.new()
	b.text = text_value
	b.add_theme_font_size_override("font_size", 20)
	b.add_theme_color_override("font_color", Color.WHITE if primary else Color("173947"))
	b.add_theme_stylebox_override("normal", _rounded_style(Color("1eaf82") if primary else Color("eef6f9"), 20))
	b.add_theme_stylebox_override("pressed", _rounded_style(Color("168c6a") if primary else Color("dfeef3"), 20))
	b.add_theme_stylebox_override("hover", _rounded_style(Color("22ba8a") if primary else Color("f5fafc"), 20))
	return b

func _make_chip(text_value: String) -> Label:
	var l := Label.new()
	l.text = text_value
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	l.add_theme_font_size_override("font_size", 19)
	l.add_theme_color_override("font_color", Color("224f50"))
	l.add_theme_stylebox_override("normal", _rounded_style(Color("e8f7ee"), 22))
	l.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	l.custom_minimum_size = Vector2(0,52)
	return l

func _show_add_dialog() -> void:
	task_title_edit.text = ""
	minutes_spin.value = 30
	add_dialog.popup_centered(Vector2i(600,400))

func _add_task() -> void:
	var title := task_title_edit.text.strip_edges()
	if title.is_empty():
		return
	var now := int(Time.get_unix_time_from_system())
	var minutes := int(minutes_spin.value)
	tasks.append({
		"id": next_task_id,
		"title": title.substr(0,60),
		"created": now,
		"deadline": now + minutes * 60,
		"status": "active"
	})
	next_task_id += 1
	_save_state()
	_refresh_ui()

func _toggle_panel(mode: String) -> void:
	if task_panel.visible and panel_mode == mode:
		task_panel.visible = false
		panel_mode = ""
		return
	panel_mode = mode
	task_panel.visible = true
	_render_panel()

func _render_panel() -> void:
	for child in list_box.get_children():
		list_box.remove_child(child)
		child.queue_free()
	if panel_mode == "tasks":
		panel_title.text = "المهام النشطة"
		var found := false
		for t in tasks:
			if str(t.get("status","")) == "active":
				found = true
				_add_task_row(t, true)
		if not found:
			_add_empty("لا توجد مهام نشطة.\nأضف مهمة وابدأ بناء مدينتك 🌱")
	else:
		panel_title.text = "سجل المهام"
		if tasks.is_empty():
			_add_empty("سجل المهام فارغ حتى الآن.")
		else:
			for i in range(tasks.size() - 1, -1, -1):
				_add_task_row(tasks[i], false)

func _add_task_row(t: Dictionary, active_mode: bool) -> void:
	var card := PanelContainer.new()
	card.add_theme_stylebox_override("panel", _rounded_style(Color("f1f8f4"), 18))
	list_box.add_child(card)
	var v := VBoxContainer.new()
	v.layout_direction = Control.LAYOUT_DIRECTION_RTL
	card.add_child(v)
	var name := Label.new()
	name.text = str(t.get("title",""))
	name.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	name.add_theme_font_size_override("font_size", 19)
	name.add_theme_color_override("font_color", Color("1c4844"))
	v.add_child(name)
	if active_mode:
		var remaining := max(0, int(t.get("deadline",0)) - int(Time.get_unix_time_from_system()))
		var rem := Label.new()
		rem.text = "الوقت المتبقي: " + _format_time(remaining)
		rem.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
		rem.add_theme_color_override("font_color", Color("376b61"))
		v.add_child(rem)
		var done := _make_button("✓ تم الإنجاز", true)
		done.custom_minimum_size = Vector2(0,50)
		var task_id := int(t.get("id",0))
		done.pressed.connect(func(): _complete_task(task_id))
		v.add_child(done)
	else:
		var status := str(t.get("status",""))
		var label := Label.new()
		label.text = "✓ منجزة" if status == "done" else ("انتهى الوقت" if status == "failed" else "نشطة")
		label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
		label.add_theme_color_override("font_color", Color("159365") if status == "done" else Color("c95749"))
		v.add_child(label)

func _add_empty(text_value: String) -> void:
	var l := Label.new()
	l.text = text_value
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	l.custom_minimum_size = Vector2(0,150)
	l.add_theme_font_size_override("font_size", 18)
	l.add_theme_color_override("font_color", Color("557178"))
	list_box.add_child(l)

func _complete_task(task_id: int) -> void:
	var now := int(Time.get_unix_time_from_system())
	for t in tasks:
		if int(t.get("id",0)) == task_id and str(t.get("status","")) == "active":
			if now >= int(t.get("deadline",0)):
				_resolve_expired(true)
				return
			t["status"] = "done"
			_build_house_for_task(task_id)
			_save_state()
			_refresh_ui()
			return

func _build_house_for_task(task_id: int) -> void:
	var occupied := {}
	for h in houses_data:
		occupied[int(h.get("plot",-1))] = true
	var free_plot := -1
	for i in range(PLOTS.size()):
		if not occupied.has(i):
			free_plot = i
			break
	if free_plot < 0:
		return
	var now := int(Time.get_unix_time_from_system())
	var h := {"id":next_house_id,"plot":free_plot,"variant":(next_house_id-1)%6,"task_id":task_id,"built_at":now}
	houses_data.append(h)
	next_house_id += 1
	var node := _make_house(PLOTS[free_plot], int(h["variant"]))
	node.set_meta("house_id", int(h["id"]))
	node.scale = Vector3.ONE * 0.04
	var tween := create_tween()
	tween.set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
	tween.tween_property(node, "scale", Vector3.ONE, 0.62)

func _resolve_expired(animate: bool) -> void:
	var now := int(Time.get_unix_time_from_system())
	var changed := false
	for t in tasks:
		if str(t.get("status","")) == "active" and now >= int(t.get("deadline",0)):
			t["status"] = "failed"
			_demolish_latest(animate)
			changed = true
	if changed:
		_save_state()
		_refresh_ui()

func _demolish_latest(animate: bool) -> void:
	if houses_data.is_empty():
		return
	var h: Dictionary = houses_data.pop_back()
	var id := int(h.get("id",0))
	var target_node: Node3D = null
	for child in house_root.get_children():
		if int(child.get_meta("house_id",-1)) == id:
			target_node = child
			break
	if target_node == null:
		return
	if animate:
		var tween := create_tween()
		tween.set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_IN)
		tween.tween_property(target_node, "scale", Vector3.ONE * 0.02, 0.45)
		tween.tween_callback(target_node.queue_free)
	else:
		target_node.queue_free()

func _refresh_ui() -> void:
	var active_count := 0
	for t in tasks:
		if str(t.get("status","")) == "active":
			active_count += 1
	house_count_label.text = "🏠 %d بيت" % houses_data.size()
	active_count_label.text = "⏱ %d مهمة نشطة" % active_count
	if task_panel.visible:
		_render_panel()

func _format_time(seconds: int) -> String:
	var h := seconds / 3600
	var m := (seconds % 3600) / 60
	var s := seconds % 60
	if h > 0:
		return "%02d:%02d:%02d" % [h,m,s]
	return "%02d:%02d" % [m,s]

func _save_state() -> void:
	var data := {"tasks":tasks,"houses":houses_data,"next_task_id":next_task_id,"next_house_id":next_house_id}
	var file := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
	if file:
		file.store_string(JSON.stringify(data))

func _load_state() -> void:
	if not FileAccess.file_exists(SAVE_PATH):
		return
	var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
	if file == null:
		return
	var parsed = JSON.parse_string(file.get_as_text())
	if typeof(parsed) != TYPE_DICTIONARY:
		return
	tasks = parsed.get("tasks", [])
	houses_data = parsed.get("houses", [])
	next_task_id = int(parsed.get("next_task_id", 1))
	next_house_id = int(parsed.get("next_house_id", 1))

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			touches[event.index] = event.position
		else:
			touches.erase(event.index)
		if touches.size() < 2:
			pinch_previous = 0.0
	elif event is InputEventScreenDrag:
		touches[event.index] = event.position
		if touches.size() >= 2:
			var values := touches.values()
			var distance := (values[0] as Vector2).distance_to(values[1] as Vector2)
			if pinch_previous > 0.0:
				camera_size = clamp(camera_size + (pinch_previous - distance) * 0.025, 20.0, 48.0)
				camera.size = camera_size
			pinch_previous = distance
		else:
			var scale_factor := camera_size / max(500.0, float(get_viewport().get_visible_rect().size.y))
			camera_target.x -= event.relative.x * scale_factor * 1.30
			camera_target.z -= event.relative.y * scale_factor * 1.30
			camera_target.x = clamp(camera_target.x, -13.0, 13.0)
			camera_target.z = clamp(camera_target.z, -13.0, 13.0)
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
