package com.godnit.taskcity

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Tiny persistent store. Everything lives locally on the phone so timers and
 * the city survive app restarts and periods without Internet access.
 */
class TaskStore(context: Context) {
    data class CityTask(
        val id: String,
        val title: String,
        val createdAt: Long,
        val endAt: Long
    )

    data class ExpireResult(val expiredTasks: Int, val destroyedHouses: Int)

    private val prefs = context.getSharedPreferences("task_city_state", Context.MODE_PRIVATE)
    private val lock = Any()

    fun getTasks(): List<CityTask> = synchronized(lock) {
        readTasks().sortedBy { it.endAt }
    }

    fun addTask(title: String, durationMillis: Long): CityTask = synchronized(lock) {
        val now = System.currentTimeMillis()
        val task = CityTask(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            createdAt = now,
            endAt = now + durationMillis.coerceAtLeast(60_000L)
        )
        val tasks = readTasks().toMutableList()
        tasks += task
        writeTasks(tasks)
        task
    }

    fun completeTask(id: String): Boolean = synchronized(lock) {
        val tasks = readTasks().toMutableList()
        val removed = tasks.removeAll { it.id == id }
        if (!removed) return@synchronized false
        writeTasks(tasks)
        houseCount = houseCount + 1
        true
    }

    fun expireDueTasks(now: Long = System.currentTimeMillis()): ExpireResult = synchronized(lock) {
        val tasks = readTasks()
        val expired = tasks.filter { it.endAt <= now }
        if (expired.isEmpty()) return@synchronized ExpireResult(0, 0)

        writeTasks(tasks.filter { it.endAt > now })
        val before = houseCount
        houseCount = (before - expired.size).coerceAtLeast(0)
        ExpireResult(expired.size, (before - houseCount).coerceAtLeast(0))
    }

    var houseCount: Int
        get() = prefs.getInt(KEY_HOUSES, DEFAULT_DEMO_HOUSES)
        set(value) {
            prefs.edit().putInt(KEY_HOUSES, value.coerceAtLeast(0)).apply()
        }

    private fun readTasks(): List<CityTask> {
        val raw = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            val result = ArrayList<CityTask>(array.length())
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                result += CityTask(
                    id = item.optString("id"),
                    title = item.optString("title", "مهمة"),
                    createdAt = item.optLong("createdAt"),
                    endAt = item.optLong("endAt")
                )
            }
            result.filter { it.id.isNotBlank() && it.endAt > 0L }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun writeTasks(tasks: List<CityTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("createdAt", task.createdAt)
                put("endAt", task.endAt)
            })
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    companion object {
        private const val KEY_TASKS = "tasks_json"
        private const val KEY_HOUSES = "house_count"
        // A small starter neighborhood so the first prototype immediately shows
        // the visual direction. In the final release this can start at zero.
        private const val DEFAULT_DEMO_HOUSES = 6
    }
}
