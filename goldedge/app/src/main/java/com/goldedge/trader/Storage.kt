package com.goldedge.trader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppStorage(context: Context) {
    private val prefs = context.getSharedPreferences("goldedge", Context.MODE_PRIVATE)

    var newsLeadMinutes: Int
        get() = prefs.getInt("news_lead", 20)
        set(value) = prefs.edit().putInt("news_lead", value.coerceIn(5, 60)).apply()

    var defaultRiskPct: Double
        get() = prefs.getString("risk_pct", "1.0")?.toDoubleOrNull() ?: 1.0
        set(value) = prefs.edit().putString("risk_pct", value.coerceIn(0.1, 10.0).toString()).apply()

    fun wasNotified(key: String): Boolean = prefs.getBoolean("notified_$key", false)
    fun markNotified(key: String) = prefs.edit().putBoolean("notified_$key", true).apply()

    fun loadTrades(): List<TradeEntry> {
        val raw = prefs.getString("trades", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        TradeEntry(
                            id = o.getLong("id"),
                            timestamp = o.getLong("timestamp"),
                            side = o.getString("side"),
                            entry = o.getDouble("entry"),
                            stopLoss = o.getDouble("sl"),
                            takeProfit = o.getDouble("tp"),
                            resultR = if (o.isNull("resultR")) null else o.getDouble("resultR"),
                            setupScore = o.optInt("score", 0),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun saveTrade(trade: TradeEntry) {
        val current = loadTrades().toMutableList()
        current.removeAll { it.id == trade.id }
        current.add(0, trade)
        saveTrades(current.take(250))
    }

    fun deleteTrade(id: Long) {
        saveTrades(loadTrades().filterNot { it.id == id })
    }

    private fun saveTrades(trades: List<TradeEntry>) {
        val arr = JSONArray()
        trades.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("timestamp", t.timestamp)
                put("side", t.side)
                put("entry", t.entry)
                put("sl", t.stopLoss)
                put("tp", t.takeProfit)
                put("resultR", t.resultR ?: JSONObject.NULL)
                put("score", t.setupScore)
                put("notes", t.notes)
            })
        }
        prefs.edit().putString("trades", arr.toString()).apply()
    }
}
