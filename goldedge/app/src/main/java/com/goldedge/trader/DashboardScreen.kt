package com.goldedge.trader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
internal fun DashboardScreen(vm: GoldEdgeViewModel) {
    val snapshot = vm.snapshot
    if (snapshot == null && vm.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AccentGold)
                Spacer(Modifier.height(12.dp))
                Text("أجمع بيانات السوق…", color = MutedText)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (snapshot == null) {
                InfoCard("تعذر جلب بيانات السوق", "تحقق من الإنترنت واضغط تحديث. الحاسبة والسجل يعملان دون اتصال.", Negative)
            } else MarketDecisionCard(snapshot)
        }
        if (snapshot != null) {
            item { TimeframesCard(snapshot.timeframes) }
            item { MacroCard(snapshot) }
            item { SentimentCard(snapshot.sentiment) }
            item { NextNewsCard(snapshot.news) }
            item {
                SectionCard("الجلسة والتوقيت", "الجلسة عامل سيولة وليست إشارة دخول منفردة") {
                    Text(snapshot.sessionLabel, color = if (snapshot.sessionLabel.contains("تداخل")) AccentGold else Positive, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("وقت الجهاز: ${DateTimeFormatter.ofPattern("HH:mm").format(java.time.ZonedDateTime.now())}", color = MutedText, fontSize = 10.sp)
                }
            }
            if (snapshot.sourceErrors.isNotEmpty()) {
                item { InfoCard("بعض المصادر لم تتحدث", snapshot.sourceErrors.take(3).joinToString("\n"), Color(0xFFF59E0B)) }
            }
            item {
                InfoCard(
                    "كيف تستخدم الاتجاه؟",
                    "الدرجة المركبة فلتر يجمع الفريمات + DXY + عائد 10Y + الجلسة ويخفض الثقة قرب الأخبار. دخولك الفعلي يبقى من الشارت بعد Sweep/BOS/CHoCH أو رفض واضح ومساحة مناسبة للهدف.",
                    InfoBlue
                )
            }
        }
    }
}

@Composable
private fun MarketDecisionCard(s: MarketSnapshot) {
    val directionColor = biasColor(s.direction)
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الصورة العامة الآن", color = MutedText, fontSize = 12.sp)
                    Text(biasArabic(s.direction), color = directionColor, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                    Text("الثقة ${s.confidence}% • الدرجة ${if (s.compositeScore > 0) "+" else ""}${s.compositeScore}", fontSize = 13.sp)
                }
                ScoreGauge(s.confidence, directionColor, biasArabic(s.direction))
            }
            HorizontalDivider(color = SurfaceTwo)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("مرجع الذهب", color = MutedText, fontSize = 10.sp)
                    Text(s.goldProxyPrice?.fmt(2) ?: "—", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text(s.goldProxyChangePct?.let { pct(it) } ?: "—", color = MutedText, fontSize = 10.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("الجلسة", color = MutedText, fontSize = 10.sp)
                    Text(if (s.sessionLabel.contains("تداخل")) "سيولة قوية" else "سيولة عادية", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(s.sessionLabel, color = MutedText, fontSize = 9.sp)
                }
            }
            Surface(
                color = if (s.riskFlag == "طبيعي") Positive.copy(alpha = 0.10f) else Negative.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (s.riskFlag == "طبيعي") "لا يوجد حاجز خبري قريب ضمن الفلتر الحالي" else s.riskFlag,
                    modifier = Modifier.padding(12.dp),
                    color = if (s.riskFlag == "طبيعي") Positive else Negative,
                    fontSize = 12.sp
                )
            }
            Text("مرجع السعر هنا Gold Futures وليس Spot لدى وسيطك. سعر MT5 هو المرجع للدخول وSL/TP.", color = MutedText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TimeframesCard(list: List<TimeframeAnalysis>) {
    SectionCard("اتجاه الفريمات", "الأولوية للسياق H1/H4، وM5/M15 للتوقيت") {
        if (list.isEmpty()) Text("لا توجد بيانات كافية", color = MutedText)
        else list.forEachIndexed { index, tf ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = biasColor(tf.direction).copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
                    Text(tf.label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = biasColor(tf.direction), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${biasArabic(tf.direction)} • ${tf.score}", fontWeight = FontWeight.Bold)
                    Text(tf.reason, color = MutedText, fontSize = 10.sp, maxLines = 1)
                }
                Text("RSI ${tf.rsi14.roundToInt()}", color = MutedText, fontSize = 11.sp)
            }
            if (index < list.lastIndex) HorizontalDivider(color = SurfaceTwo)
        }
    }
}

@Composable
private fun MacroCard(s: MarketSnapshot) {
    SectionCard("محركات الذهب", "DXY والعوائد عوامل سياقية ولا تستخدم منفردة") {
        MacroRow("DXY", s.dxy)
        HorizontalDivider(color = SurfaceTwo)
        MacroRow("US10Y", s.us10y)
    }
}

@Composable
private fun MacroRow(name: String, metric: QuoteMetric?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.width(76.dp))
        if (metric == null) Text("غير متاح", color = MutedText)
        else {
            val arrow = when (metric.direction) { BiasDirection.BULLISH -> "↑"; BiasDirection.BEARISH -> "↓"; BiasDirection.NEUTRAL -> "→" }
            val goldEffect = when (metric.direction) {
                BiasDirection.BULLISH -> BiasDirection.BEARISH
                BiasDirection.BEARISH -> BiasDirection.BULLISH
                BiasDirection.NEUTRAL -> BiasDirection.NEUTRAL
            }
            Column(Modifier.weight(1f)) {
                Text("$arrow ${metric.value?.fmt(2) ?: "—"}  ${metric.changePct?.let { pct(it) } ?: ""}")
                Text("الأثر السياقي المحتمل على الذهب: ${biasArabic(goldEffect)}", color = biasColor(goldEffect), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SentimentCard(s: SentimentSnapshot) {
    val context = LocalContext.current
    SectionCard("مزاج المتداولين", "Myfxbook • بيانات المجتمع، والمجاني متأخر") {
        if (s.longPct != null && s.shortPct != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("شراء ${s.longPct}%", color = Positive, fontWeight = FontWeight.Bold)
                Text("بيع ${s.shortPct}%", color = Negative, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(12.dp).background(SurfaceTwo, RoundedCornerShape(99.dp))) {
                Box(Modifier.weight(s.longPct.coerceAtLeast(1).toFloat()).fillMaxSize().background(Positive, RoundedCornerShape(topStart = 99.dp, bottomStart = 99.dp)))
                Box(Modifier.weight(s.shortPct.coerceAtLeast(1).toFloat()).fillMaxSize().background(Negative, RoundedCornerShape(topEnd = 99.dp, bottomEnd = 99.dp)))
            }
            Spacer(Modifier.height(8.dp))
            Text("التأخير المعلن يقارب ${s.delayedMinutes} دقيقة. لا نعتبر أغلبية الشراء أو البيع أمر دخول؛ التطرف يستخدم كتحذير فقط.", color = MutedText, fontSize = 11.sp)
        } else Text("تعذر قراءة النسبة تلقائياً من المصدر الآن.", color = MutedText)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { openUrl(context, MarketRepository.MYFXBOOK_XAUUSD) }) { Text("فتح Myfxbook") }
    }
}

@Composable
private fun NextNewsCard(news: List<EconomicEvent>) {
    val now = Instant.now()
    val next = news.firstOrNull { it.country == "USD" && it.time.isAfter(now) }
    val nextHigh = news.firstOrNull { it.country == "USD" && it.isHigh && it.time.isAfter(now) }
    SectionCard("الأخبار القادمة", "أحداث USD من Forex Factory") {
        if (next == null) Text("لا توجد أحداث USD قادمة ضمن الأسبوع الحالي.", color = MutedText)
        else {
            CompactNews(next, now)
            if (nextHigh != null && nextHigh != next) {
                Spacer(Modifier.height(10.dp))
                Text("أقرب خبر قوي", color = Negative, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                CompactNews(nextHigh, now)
            }
        }
    }
}

@Composable
private fun CompactNews(event: EconomicEvent, now: Instant) {
    val mins = Duration.between(now, event.time).toMinutes()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        ImpactDot(event.impact)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(formatEventTime(event.time), color = MutedText, fontSize = 11.sp)
        }
        Text(if (mins >= 0) humanDuration(mins) else "انتهى", color = if (event.isHigh) Negative else AccentGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}
