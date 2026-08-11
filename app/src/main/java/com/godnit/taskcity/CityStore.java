package com.godnit.taskcity;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CityStore {
    public enum Status { ACTIVE, DONE, FAILED, CANCELLED }

    public static final class TaskRecord {
        public String id;
        public String title;
        public long createdAt;
        public long deadline;
        public Status status;
    }

    public static final class HouseRecord {
        public String id;
        public int plot;
        public int variant;
        public String taskId;
        public long builtAt;
    }

    private static final String PREFS = "task_city_store";
    private static final String KEY_DATA = "data_v1";
    private final SharedPreferences prefs;
    public final List<TaskRecord> tasks = new ArrayList<>();
    public final List<HouseRecord> houses = new ArrayList<>();

    public CityStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public TaskRecord addTask(String title, long durationMs) {
        TaskRecord task = new TaskRecord();
        task.id = UUID.randomUUID().toString();
        task.title = title.trim();
        task.createdAt = System.currentTimeMillis();
        task.deadline = task.createdAt + durationMs;
        task.status = Status.ACTIVE;
        tasks.add(task);
        save();
        return task;
    }

    public HouseRecord buildHouse(String sourceTaskId, int plotCount) {
        boolean[] occupied = new boolean[plotCount];
        for (HouseRecord house : houses) {
            if (house.plot >= 0 && house.plot < occupied.length) occupied[house.plot] = true;
        }
        int plot = -1;
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i]) { plot = i; break; }
        }
        if (plot < 0) return null;
        HouseRecord house = new HouseRecord();
        house.id = UUID.randomUUID().toString();
        house.plot = plot;
        house.variant = Math.abs(sourceTaskId.hashCode()) % 6;
        house.taskId = sourceTaskId;
        house.builtAt = System.currentTimeMillis();
        houses.add(house);
        save();
        return house;
    }

    public HouseRecord latestHouse() {
        if (houses.isEmpty()) return null;
        return houses.get(houses.size() - 1);
    }

    public void removeHouse(String houseId) {
        houses.removeIf(h -> h.id.equals(houseId));
        save();
    }

    public TaskRecord findTask(String id) {
        for (TaskRecord task : tasks) if (task.id.equals(id)) return task;
        return null;
    }

    public int activeCount() {
        int count = 0;
        for (TaskRecord task : tasks) if (task.status == Status.ACTIVE) count++;
        return count;
    }

    public List<TaskRecord> activeTasks() {
        List<TaskRecord> result = new ArrayList<>();
        for (TaskRecord task : tasks) if (task.status == Status.ACTIVE) result.add(task);
        return result;
    }

    public void clearAll() {
        tasks.clear();
        houses.clear();
        prefs.edit().clear().apply();
    }

    public void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            JSONArray taskArray = new JSONArray();
            for (TaskRecord task : tasks) {
                JSONObject o = new JSONObject();
                o.put("id", task.id);
                o.put("title", task.title);
                o.put("createdAt", task.createdAt);
                o.put("deadline", task.deadline);
                o.put("status", task.status.name());
                taskArray.put(o);
            }
            JSONArray houseArray = new JSONArray();
            for (HouseRecord house : houses) {
                JSONObject o = new JSONObject();
                o.put("id", house.id);
                o.put("plot", house.plot);
                o.put("variant", house.variant);
                o.put("taskId", house.taskId);
                o.put("builtAt", house.builtAt);
                houseArray.put(o);
            }
            root.put("tasks", taskArray);
            root.put("houses", houseArray);
            prefs.edit().putString(KEY_DATA, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void load() {
        tasks.clear();
        houses.clear();
        String raw = prefs.getString(KEY_DATA, null);
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(raw);
            JSONArray taskArray = root.optJSONArray("tasks");
            if (taskArray != null) {
                for (int i = 0; i < taskArray.length(); i++) {
                    JSONObject o = taskArray.getJSONObject(i);
                    TaskRecord task = new TaskRecord();
                    task.id = o.optString("id");
                    task.title = o.optString("title");
                    task.createdAt = o.optLong("createdAt");
                    task.deadline = o.optLong("deadline");
                    try { task.status = Status.valueOf(o.optString("status", "ACTIVE")); }
                    catch (Exception e) { task.status = Status.ACTIVE; }
                    tasks.add(task);
                }
            }
            JSONArray houseArray = root.optJSONArray("houses");
            if (houseArray != null) {
                for (int i = 0; i < houseArray.length(); i++) {
                    JSONObject o = houseArray.getJSONObject(i);
                    HouseRecord house = new HouseRecord();
                    house.id = o.optString("id");
                    house.plot = o.optInt("plot");
                    house.variant = o.optInt("variant");
                    house.taskId = o.optString("taskId");
                    house.builtAt = o.optLong("builtAt");
                    houses.add(house);
                }
            }
        } catch (Exception e) {
            prefs.edit().remove(KEY_DATA).apply();
        }
    }
}
