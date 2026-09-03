package com.goldedge.trader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

class MarketRepository(context: Context) {
    private val client = OkHttpClient.Builder()
        .cache(Cache(context.cacheDir.resolve("http"), 12L * 1024L * 1024L))
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun loadSnapshot(): MarketSnapshot = coroutineScope {
        val errors = mutableListOf<String>()

        val m5 = async { safeCandles("GC=F", "5m", "5d", errors, "ذهب M5") }
        val m15 = async { safeCandles("GC=F", "15m", "5d", errors, "ذهب M15") }
        val h1 = async { safeCandles("GC=F", "60m", "1mo", errors, "ذهب H1") }
        val d1 = async { safeCandles("GC=F", "1d", "6mo", errors, "ذهب D1") }
        val dxy = async { safeCandles("DX-Y.NYB", "15m", "5d", errors, "DXY") }
        val tnx = async { safeCandles("%5ETNX", "15m", "5d", errors, "US10Y") }
        val news = async { safeNews(errors) }
        val sentiment = async { safeSentiment() }

        val m5c = m5.await()
        val m15c = m15.await()
        val h1c = h1.await()
        val d1c = d1.await()
        val h4c = aggregate(h1c, 4)

        val analyses = listOfNotNull(
            IndicatorEngine.analyze("M5", m5c),
            IndicatorEngine.analyze("M15", m15c),
            IndicatorEngine.analyze("H1", h1c),
            IndicatorEngine.analyze("H4", h4c),
            IndicatorEngine.analyze("D1", d1c)
        )

        val dxyMetric = metric("DXY", dxy.await(), "Yahoo Finance")
        val tnxMetric = metric("US10Y", tnx.await(), "Yahoo Finance")
        val sentimentData = sentiment.await()
        val eventList = news.await().sortedBy { it.time }

        val technical = weightedTechnical(analyses)
        var directionalScore = technical.toDouble()
        dxyMetric?.let {
            directionalScore += when (it.direction) {
                BiasDirection.BULLISH -> -8.0
                BiasDirection.BEARISH -> 8.0
                BiasDirection.NEUTRAL -> 0.0
            }
        }
        tnxMetric?.let {
            directionalScore += when (it.direction) {
                BiasDirection.BULLISH -> -6.0
                BiasDirection.BEARISH -> 6.0
                BiasDirection.NEUTRAL -> 0.0
            }
        }
        // Retail sentiment is deliberately tiny and contrarian only at extremes.
        sentimentData.longPct?.let { lp ->
            directionalScore += when {
                lp >= 72 -> -5.0
                lp <= 28 -> 5.0
                else -> 0.0
            }
        }

        val session = MarketSessions.currentLabel()
        if (session.contains("تداخل")) {
            directionalScore += if (directionalScore >= 0) 3 else -3
        }

        val now = Instant.now()
        val nearHigh = eventList.firstOrNull {
            it.country == "USD" && it.isHigh && Duration.between(now, it.time).toMinutes() in 0..30
        }
        val nearMedium = eventList.firstOrNull {
            it.country == "USD" && it.isMedium && Duration.between(now, it.time).toMinutes() in 0..20
        }

        val score = directionalScore.roundToInt().coerceIn(-100, 100)
        val direction = when {
            score >= 18 -> BiasDirection.BULLISH
            score <= -18 -> BiasDirection.BEARISH
            else -> BiasDirection.NEUTRAL
        }
        var confidence = abs(score).coerceIn(0, 100)
        var riskFlag = "طبيعي"
        if (nearHigh != null) {
            confidence = confidence.coerceAtMost(55)
            riskFlag = "خبر USD قوي قريب — لا تطارد الدخول"
        } else if (nearMedium != null) {
            confidence = confidence.coerceAtMost(70)
            riskFlag = "خبر USD متوسط قريب"
        }

        val goldPrice = analyses.firstOrNull { it.label == "M5" }?.lastPrice
        val goldChange = analyses.firstOrNull { it.label == "M5" }?.changePct

        MarketSnapshot(
            updatedAt = now,
            goldProxyPrice = goldPrice,
            goldProxyChangePct = goldChange,
            timeframes = analyses,
            dxy = dxyMetric,
            us10y = tnxMetric,
            sentiment = sentimentData,
            news = eventList,
            compositeScore = score,
            direction = direction,
            confidence = confidence,
            riskFlag = riskFlag,
            sessionLabel = session,
            sourceErrors = errors.toList()
        )
    }

    suspend fun fetchNewsOnly(): List<EconomicEvent> = withContext(Dispatchers.IO) {
        val raw = get(FOREX_FACTORY_JSON)
        parseNews(raw)
    }

    private suspend fun safeCandles(
        symbol: String,
        interval: String,
        range: String,
        errors: MutableList<String>,
        name: String
    ): List<Candle> = try {
        fetchYahoo(symbol, interval, range)
    } catch (t: Throwable) {
        synchronized(errors) { errors += "$name: ${t.message ?: "تعذر التحديث"}" }
        emptyList()
    }

    private suspend fun safeNews(errors: MutableList<String>): List<EconomicEvent> = try {
        fetchNewsOnly()
    } catch (t: Throwable) {
        synchronized(errors) { errors += "الأخبار: ${t.message ?: "تعذر التحديث"}" }
        emptyList()
    }

    private suspend fun safeSentiment(): SentimentSnapshot = try {
        fetchSentiment()
    } catch (t: Throwable) {
        SentimentSnapshot(null, null, error = t.message ?: "تعذر جلب المزاج")
    }

    private suspend fun fetchYahoo(symbol: String, interval: String, range: String): List<Candle> =
        withContext(Dispatchers.IO) {
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=$interval&range=$range&includePrePost=false&events=div%2Csplits"
            val json = JSONObject(get(url))
            val chart = json.getJSONObject("chart")
            val error = chart.opt("error")
            if (error != null && error != JSONObject.NULL) throw IOException("Yahoo: $error")
            val result = chart.getJSONArray("result").getJSONObject(0)
            val times = result.optJSONArray("timestamp") ?: return@withContext emptyList()
            val quote = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
            val opens = quote.optJSONArray("open") ?: JSONArray()
            val highs = quote.optJSONArray("high") ?: JSONArray()
            val lows = quote.optJSONArray("low") ?: JSONArray()
            val closes = quote.optJSONArray("close") ?: JSONArray()
            val out = ArrayList<Candle>(times.length())
            for (i in 0 until times.length()) {
                val o = opens.optDoubleOrNull(i)
                val h = highs.optDoubleOrNull(i)
                val l = lows.optDoubleOrNull(i)
                val c = closes.optDoubleOrNull(i)
                if (o != null && h != null && l != null && c != null) {
                    out += Candle(times.getLong(i), o, h, l, c)
                }
            }
            out
        }

    private suspend fun fetchSentiment(): SentimentSnapshot = withContext(Dispatchers.IO) {
        val html = get(MYFXBOOK_XAUUSD, acceptLanguage = "en-US,en;q=0.9")
        val shortRegex = Regex("(\\d{1,3})%\\s+of the forex traders are currently going short", RegexOption.IGNORE_CASE)
        val longRegex = Regex("(\\d{1,3})%\\s+of the forex traders are currently going long", RegexOption.IGNORE_CASE)
        val tableRegex = Regex("Short\\s*</?[^>]*>.*?(\\d{1,3})\\s*%.*?Long\\s*</?[^>]*>.*?(\\d{1,3})\\s*%", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val shortPct = shortRegex.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val longPct = longRegex.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (shortPct != null || longPct != null) {
            val s = shortPct ?: (100 - (longPct ?: 50))
            val l = longPct ?: (100 - s)
            return@withContext SentimentSnapshot(l, s)
        }
        val table = tableRegex.find(html)
        if (table != null) {
            val s = table.groupValues[1].toIntOrNull()
            val l = table.groupValues[2].toIntOrNull()
            return@withContext SentimentSnapshot(l, s)
        }
        SentimentSnapshot(null, null, error = "المصدر لم يُرجع نسبة قابلة للقراءة")
    }

    private fun parseNews(raw: String): List<EconomicEvent> {
        val arr = JSONArray(raw)
        val out = mutableListOf<EconomicEvent>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val date = obj.optString("date")
            val instant = runCatching { ZonedDateTime.parse(date).toInstant() }.getOrNull() ?: continue
            out += EconomicEvent(
                title = obj.optString("title"),
                country = obj.optString("country"),
                time = instant,
                impact = obj.optString("impact"),
                forecast = obj.optString("forecast"),
                previous = obj.optString("previous")
            )
        }
        return out
    }

    private fun metric(label: String, candles: List<Candle>, source: String): QuoteMetric? {
        val a = IndicatorEngine.analyze(label, candles) ?: return null
        return QuoteMetric(label, a.lastPrice, a.changePct, a.direction, source)
    }

    private fun weightedTechnical(list: List<TimeframeAnalysis>): Int {
        if (list.isEmpty()) return 0
        val weights = mapOf("M5" to 0.10, "M15" to 0.20, "H1" to 0.30, "H4" to 0.25, "D1" to 0.15)
        var total = 0.0
        var used = 0.0
        list.forEach {
            val w = weights[it.label] ?: 0.1
            total += it.score * w
            used += w
        }
        return if (used == 0.0) 0 else (total / used).roundToInt().coerceIn(-100, 100)
    }

    private fun aggregate(input: List<Candle>, group: Int): List<Candle> {
        if (input.size < group) return emptyList()
        return input.chunked(group).mapNotNull { chunk ->
            if (chunk.size < group) null else Candle(
                time = chunk.first().time,
                open = chunk.first().open,
                high = chunk.maxOf { it.high },
                low = chunk.minOf { it.low },
                close = chunk.last().close
            )
        }
    }

    private fun get(url: String, acceptLanguage: String = "en-US,en;q=0.9"): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) GoldEdge/1.0")
            .header("Accept", "application/json,text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", acceptLanguage)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("استجابة فارغة")
        }
    }

    companion object {
        const val FOREX_FACTORY_JSON = "https://nfs.faireconomy.media/ff_calendar_thisweek.json"
        const val FOREX_FACTORY_PAGE = "https://www.forexfactory.com/calendar?currency=USD"
        const val MYFXBOOK_XAUUSD = "https://www.myfxbook.com/community/outlook/XAUUSD"
        const val TRADINGVIEW_XAUUSD = "https://www.tradingview.com/symbols/XAUUSD/technicals/"
    }
}

private fun JSONArray.optDoubleOrNull(index: Int): Double? {
    if (index !in 0 until length()) return null
    val value = opt(index)
    if (value == null || value == JSONObject.NULL) return null
    return when (value) {
        is Number -> value.toDouble()
        else -> value.toString().toDoubleOrNull()
    }
}

object MarketSessions {
    fun currentLabel(now: Instant = Instant.now()): String {
        val london = now.atZone(ZoneId.of("Europe/London"))
        val ny = now.atZone(ZoneId.of("America/New_York"))
        val londonOpen = london.hour in 8 until 17
        val nyOpen = ny.hour in 8 until 17
        return when {
            londonOpen && nyOpen -> "تداخل لندن + نيويورك • سيولة مرتفعة"
            nyOpen -> "جلسة نيويورك"
            londonOpen -> "جلسة لندن"
            else -> "خارج الجلسات الرئيسية"
        }
    }
}
