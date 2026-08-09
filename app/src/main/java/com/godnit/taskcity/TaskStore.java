package com.godnit.taskcity;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class TaskStore {
    private static final String PREFS = "task_city_data";
    private static final String KEY_TASKS = "tasks";
    private static final String KEY_NORMAL_HOUSES = "normal_houses";
    private static final String KEY_URGENT_HOUSES = "urgent_houses";

    private final SharedPreferences preferences;
    private final List<TaskItem> tasks = new ArrayList<>();
    private int normalHouses;
    private int urgentHouses;

    TaskStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        normalHouses = preferences.getInt(KEY_NORMAL_HOUSES, 0);
        urgentHouses = preferences.getInt(KEY_URGENT_HOUSES, 0);
        loadTasks();
    }

    List<TaskItem> tasksOfType(int type) {
        List<TaskItem> filtered = new ArrayList<>();
        for (TaskItem task : tasks) {
            if (task.type == type) filtered.add(task);
        }
        return filtered;
    }

    void add(String title, int type, long dueAt) {
        tasks.add(new TaskItem(System.nanoTime(), title, type, dueAt));
        save();
    }

    TaskItem complete(long id) {
        Iterator<TaskItem> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            TaskItem task = iterator.next();
            if (task.id == id) {
                iterator.remove();
                if (task.type == TaskItem.NORMAL) normalHouses++;
                else urgentHouses++;
                save();
                return task;
            }
        }
        return null;
    }

    int expireUrgentTasks(long now) {
        int expired = 0;
        Iterator<TaskItem> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            TaskItem task = iterator.next();
            if (task.type == TaskItem.URGENT && task.dueAt > 0 && task.dueAt <= now) {
                iterator.remove();
                if (urgentHouses > 0) urgentHouses--;
                expired++;
            }
        }
        if (expired > 0) save();
        return expired;
    }

    int getHouseCount(int type) {
        return type == TaskItem.NORMAL ? normalHouses : urgentHouses;
    }

    private void loadTasks() {
        String json = preferences.getString(KEY_TASKS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                tasks.add(TaskItem.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            tasks.clear();
        }
    }

    private void save() {
        JSONArray array = new JSONArray();
        for (TaskItem task : tasks) {
            try {
                array.put(task.toJson());
            } catch (JSONException ignored) {
                // This model contains only primitive values, so serialization is safe.
            }
        }
        preferences.edit()
                .putString(KEY_TASKS, array.toString())
                .putInt(KEY_NORMAL_HOUSES, normalHouses)
                .putInt(KEY_URGENT_HOUSES, urgentHouses)
                .apply();
    }
}
