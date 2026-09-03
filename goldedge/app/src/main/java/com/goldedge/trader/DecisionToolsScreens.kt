package com.goldedge.trader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

@Composable
internal fun ChecklistScreen(vm: GoldEdgeViewModel) {
    val checks = remember { mutableStateMapOf<String, Boolean>() }
    val checklist = listOf(
        "الاتجاه على H1/H4 موافق" to 15,
        "السعر في دعم/طلب أو مقاومة/عرض واضحة" to 12,
        "حدث Sweep للسيولة" to 14,
        "ظهر CHoCH أو BOS للتأكيد" to 16,
        "عودة إلى FVG / OB أو Retest منطقي" to 11,
        "رفض شمعة واضح على M5/M15" to 8,
        "الجلسة لندن/نيويورك والسيولة مناسبة" to 7,
        "لا يوجد خبر USD قوي خلال 20 دقيقة" to 10,
        "لا يوجد حاجز قريب يخنق الهدف" to 7
    )
    var entry by remember { mutableStateOf("") }
    var sl by remember { mutableStateOf("") }
    var tp by remember { mutableStateOf("") }
    val rr = runCatching {
        val e = entry.toDouble(); val s = sl.toDouble(); val t = tp.toDouble(); abs(t - e) / abs(e - s)
    }.getOrNull()?.takeIf { it.isFinite() }
    val baseScore = checklist.sumOf { (label, weight) -> if (checks[label] == true) weight else 0 }
    val rrBonus = when {
        rr == null -> 0
        rr >= 2.0 -> 8
        rr >= 1.5 -> 5
        rr >= 1.2 -> 2
        else -> -8
    }
    val score = (baseScore + rrBonus).coerceIn(0, 100)
    val grade = when {
        score >= 85 -> "A+ • فرصة شديدة الجودة"
        score >= 75 -> "A • فرصة قوية"
        score >= 63 -> "B • جيدة لكن تحتاج انتقائية"
        score >= 50 -> "C • متوسطة"
        else -> "D • الأفضل الانتظار"
    }
    val nearNews = vm.snapshot?.news?.any {
        it.country == "USD" && it.isHigh && Duration.between(Instant.now(), it.time).toMinutes() in 0..20
    } == true

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard("فلتر الصفقة SMC", "درجة تنظيمية للأسباب قبل الدخول وليست توصية آلية") {
            Row(Modifier.fillMaxWidth()) {
                ScoreGauge(score, when { score >= 75 -> Positive; score >= 55 -> AccentGold; else -> Negative }, "SETUP")
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(grade, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("الشروط $baseScore/100${rr?.let { " • R:R ${it.fmt(2)}" } ?: ""}", color = MutedText, fontSize = 11.sp)
                    if (nearNews) Text("⚠ خبر USD قوي قريب", color = Negative, fontSize = 11.sp)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(8.dp)) {
                checklist.forEach { (text, weight) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { checks[text] = !(checks[text] ?: false) }.padding(vertical = 5.dp, horizontal = 4.dp)
                    ) {
                        Checkbox(checked = checks[text] ?: false, onCheckedChange = { checks[text] = it })
                        Column(Modifier.weight(1f).padding(top = 5.dp)) {
                            Text(text, fontSize = 13.sp)
                            Text("وزن $weight نقاط", color = MutedText, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        SectionCard("تحقق العائد/المخاطرة", "أدخل الأرقام قبل الضغط على Buy/Sell") {
            NumberFields(entry, { entry = it }, sl, { sl = it }, tp, { tp = it })
            Spacer(Modifier.height(8.dp))
            Text(
                rr?.let { "R:R = 1 : ${it.fmt(2)} • ${if (it >= 1.5) "مناسب" else if (it >= 1.2) "متوسط" else "ضعيف"}" }
                    ?: "أدخل Entry / SL / TP",
                color = when { rr == null -> MutedText; rr < 1.2 -> Negative; rr >= 1.5 -> Positive; else -> AccentGold },
                fontWeight = FontWeight.Bold
            )
        }

        InfoCard("الفلتر الأقوى", "سياق الفريم الأكبر + سيولة + تأكيد هيكل + مساحة هدف + توقيت آمن من الأخبار أهم من إضافة مؤشرات كثيرة.", InfoBlue)
    }
}

@Composable
internal fun RiskScreen(vm: GoldEdgeViewModel) {
    var balance by remember { mutableStateOf("1000") }
    var riskPct by remember { mutableStateOf(vm.storage.defaultRiskPct.toString()) }
    var entry by remember { mutableStateOf("") }
    var sl by remember { mutableStateOf("") }
    var tp by remember { mutableStateOf("") }
    val result = IndicatorEngine.risk(
        balance.toDoubleOrNull() ?: 0.0,
        riskPct.toDoubleOrNull() ?: 0.0,
        entry.toDoubleOrNull() ?: 0.0,
        sl.toDoubleOrNull() ?: 0.0,
        tp.toDoubleOrNull() ?: 0.0
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard("حاسبة مخاطرة XAUUSD", "الحساب يفترض 1 Lot = 100 أونصة؛ راجع Contract Size عند وسيطك") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("الرصيد", balance, { balance = cleanNumber(it) }, Modifier.weight(1f))
                NumField("المخاطرة %", riskPct, { riskPct = cleanNumber(it) }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            NumberFields(entry, { entry = it }, sl, { sl = it }, tp, { tp = it })
        }

        if (result != null) {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("خطة الصفقة", color = AccentGold, fontWeight = FontWeight.Bold)
                    ResultLine("المبلغ المعرض للخطر", "${result.riskAmount.fmt(2)} USD")
                    ResultLine("حجم اللوت التقريبي", result.lotSize.fmt(3))
                    ResultLine("R:R", "1 : ${result.rr.fmt(2)}")
                    ResultLine("الربح عند TP", "${result.rewardAmount.fmt(2)} USD")
                    Text(
                        "جودة R:R: ${when { result.rr >= 2 -> "ممتاز"; result.rr >= 1.5 -> "جيد"; result.rr >= 1.2 -> "متوسط"; else -> "ضعيف" }}",
                        color = when { result.rr >= 1.5 -> Positive; result.rr >= 1.2 -> AccentGold; else -> Negative },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        InfoCard("انضباط المخاطرة", "ثبات المخاطرة يجعل أداء الاستراتيجية قابلاً للقياس. لا ترفع اللوت لتعويض خسارة سابقة.", Negative)
    }
}
