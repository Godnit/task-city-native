package com.goldedge.trader

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object IndicatorEngine {
    fun analyze(label: String, candles: List<Candle>): TimeframeAnalysis? {
        if (candles.size < 55) return null
        val closes = candles.map { it.close }
        val ema20 = ema(closes, 20)
        val ema50 = ema(closes, 50)
        val rsi = rsi(closes, 14)
        val atr = atr(candles, 14)
        val last = candles.last().close
        val first = candles[max(0, candles.size - 11)].close
        val changePct = if (first != 0.0) ((last - first) / first) * 100.0 else 0.0

        var score = 0
        val reasons = mutableListOf<String>()

        if (ema20 > ema50) {
            score += 30
            reasons += "EMA20 أعلى EMA50"
        } else {
            score -= 30
            reasons += "EMA20 أسفل EMA50"
        }

        val recent = candles.takeLast(8)
        val higherHighs = recent.takeLast(4).zipWithNext().count { (a, b) -> b.high >= a.high }
        val higherLows = recent.takeLast(4).zipWithNext().count { (a, b) -> b.low >= a.low }
        val lowerHighs = recent.takeLast(4).zipWithNext().count { (a, b) -> b.high <= a.high }
        val lowerLows = recent.takeLast(4).zipWithNext().count { (a, b) -> b.low <= a.low }
        if (higherHighs + higherLows >= 5) {
            score += 20
            reasons += "هيكل قصير صاعد"
        } else if (lowerHighs + lowerLows >= 5) {
            score -= 20
            reasons += "هيكل قصير هابط"
        }

        when {
            rsi in 52.0..68.0 -> {
                score += 15
                reasons += "RSI داعم للصعود"
            }
            rsi in 32.0..48.0 -> {
                score -= 15
                reasons += "RSI داعم للهبوط"
            }
            rsi > 75.0 -> {
                score -= 5
                reasons += "تشبع شرائي"
            }
            rsi < 25.0 -> {
                score += 5
                reasons += "تشبع بيعي"
            }
        }

        val momentum = last - candles[candles.size - 6].close
        if (atr > 0) {
            val normalized = momentum / atr
            when {
                normalized > 1.0 -> {
                    score += 20
                    reasons += "زخم صاعد"
                }
                normalized < -1.0 -> {
                    score -= 20
                    reasons += "زخم هابط"
                }
            }
        }

        val slopeBase = candles.takeLast(12).dropLast(1).map { it.close }.average()
        if (last > slopeBase) score += 15 else score -= 15

        score = score.coerceIn(-100, 100)
        val direction = when {
            score >= 20 -> BiasDirection.BULLISH
            score <= -20 -> BiasDirection.BEARISH
            else -> BiasDirection.NEUTRAL
        }
        return TimeframeAnalysis(
            label = label,
            score = score,
            direction = direction,
            ema20 = ema20,
            ema50 = ema50,
            rsi14 = rsi,
            atr14 = atr,
            lastPrice = last,
            changePct = changePct,
            reason = reasons.take(3).joinToString(" • ")
        )
    }

    fun ema(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return 0.0
        val k = 2.0 / (period + 1.0)
        var value = values.take(min(period, values.size)).average()
        for (i in min(period, values.size) until values.size) {
            value = values[i] * k + value * (1.0 - k)
        }
        return value
    }

    fun rsi(values: List<Double>, period: Int): Double {
        if (values.size <= period) return 50.0
        var gain = 0.0
        var loss = 0.0
        for (i in 1..period) {
            val d = values[i] - values[i - 1]
            if (d >= 0) gain += d else loss -= d
        }
        var avgGain = gain / period
        var avgLoss = loss / period
        for (i in period + 1 until values.size) {
            val d = values[i] - values[i - 1]
            val g = max(d, 0.0)
            val l = max(-d, 0.0)
            avgGain = (avgGain * (period - 1) + g) / period
            avgLoss = (avgLoss * (period - 1) + l) / period
        }
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    fun atr(candles: List<Candle>, period: Int): Double {
        if (candles.size <= period) return 0.0
        val trs = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val c = candles[i]
            val prevClose = candles[i - 1].close
            val tr = max(c.high - c.low, max(abs(c.high - prevClose), abs(c.low - prevClose)))
            trs += tr
        }
        return trs.takeLast(period).average()
    }

    fun risk(balance: Double, riskPct: Double, entry: Double, sl: Double, tp: Double): RiskResult? {
        if (balance <= 0 || riskPct <= 0 || entry <= 0 || sl <= 0 || tp <= 0 || entry == sl) return null
        val riskAmount = balance * (riskPct / 100.0)
        val riskDistance = abs(entry - sl)
        val rewardDistance = abs(tp - entry)
        val rr = rewardDistance / riskDistance
        // XAUUSD common convention: 1 standard lot = 100 troy ounces.
        val lot = riskAmount / (riskDistance * 100.0)
        return RiskResult(riskAmount, riskAmount * rr, rr, lot)
    }
}
