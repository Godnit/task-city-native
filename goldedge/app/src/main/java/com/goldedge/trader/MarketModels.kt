package com.goldedge.trader

import java.time.Instant

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

enum class BiasDirection { BULLISH, BEARISH, NEUTRAL }

data class TimeframeAnalysis(
    val label: String,
    val score: Int,
    val direction: BiasDirection,
    val ema20: Double,
    val ema50: Double,
    val rsi14: Double,
    val atr14: Double,
    val lastPrice: Double,
    val changePct: Double,
    val reason: String
)

data class QuoteMetric(
    val label: String,
    val value: Double?,
    val changePct: Double?,
    val direction: BiasDirection,
    val source: String
)

data class SentimentSnapshot(
    val longPct: Int?,
    val shortPct: Int?,
    val delayedMinutes: Int = 60,
    val source: String = "Myfxbook",
    val error: String? = null
)

data class EconomicEvent(
    val title: String,
    val country: String,
    val time: Instant,
    val impact: String,
    val forecast: String,
    val previous: String
) {
    val isHigh: Boolean get() = impact.equals("High", true)
    val isMedium: Boolean get() = impact.equals("Medium", true)
}

data class MarketSnapshot(
    val updatedAt: Instant,
    val goldProxyPrice: Double?,
    val goldProxyChangePct: Double?,
    val timeframes: List<TimeframeAnalysis>,
    val dxy: QuoteMetric?,
    val us10y: QuoteMetric?,
    val sentiment: SentimentSnapshot,
    val news: List<EconomicEvent>,
    val compositeScore: Int,
    val direction: BiasDirection,
    val confidence: Int,
    val riskFlag: String,
    val sessionLabel: String,
    val sourceErrors: List<String> = emptyList()
)

data class TradeEntry(
    val id: Long,
    val timestamp: Long,
    val side: String,
    val entry: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val resultR: Double?,
    val setupScore: Int,
    val notes: String
)

data class RiskResult(
    val riskAmount: Double,
    val rewardAmount: Double,
    val rr: Double,
    val lotSize: Double
)
