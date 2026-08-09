package com.godnit.taskcity;

import org.json.JSONException;
import org.json.JSONObject;

final class TaskItem {
    static final int NORMAL = 0;
    static final int URGENT = 1;

    final long id;
    final String title;
    final int type;
    final long dueAt;

    TaskItem(long id, String title, int type, long dueAt) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.dueAt = dueAt;
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("title", title);
        object.put("type", type);
        object.put("dueAt", dueAt);
        return object;
    }

    static TaskItem fromJson(JSONObject object) throws JSONException {
        return new TaskItem(
                object.getLong("id"),
                object.getString("title"),
                object.getInt("type"),
                object.optLong("dueAt", 0L)
        );
    }
}
