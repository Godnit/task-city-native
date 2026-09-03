package com.goldedge.trader

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GoldEdgeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = MarketRepository(app)
    val storage = AppStorage(app)

    var snapshot by mutableStateOf<MarketSnapshot?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var trades by mutableStateOf(storage.loadTrades())
        private set

    init {
        refresh()
    }

    fun refresh() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                snapshot = repository.loadSnapshot()
            } catch (t: Throwable) {
                error = t.message ?: "تعذر تحديث بيانات السوق"
            } finally {
                loading = false
            }
        }
    }

    fun saveTrade(trade: TradeEntry) {
        storage.saveTrade(trade)
        trades = storage.loadTrades()
    }

    fun deleteTrade(id: Long) {
        storage.deleteTrade(id)
        trades = storage.loadTrades()
    }

    fun updateNewsLead(minutes: Int) {
        storage.newsLeadMinutes = minutes
    }

    fun updateRiskDefault(value: Double) {
        storage.defaultRiskPct = value
    }
}
