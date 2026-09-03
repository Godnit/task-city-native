package com.goldedge.trader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
internal fun JournalScreen(vm: GoldEdgeViewModel) {
    var addOpen by remember { mutableStateOf(false) }
    val trades = vm.trades
    val completed = trades.filter { it.resultR != null }
    val winRate = if (completed.isNotEmpty()) completed.count { (it.resultR ?: 0.0) > 0 }.toDouble() / completed.size * 100 else null
    val avgR = completed.mapNotNull { it.resultR }.takeIf { it.isNotEmpty() }?.average()
    val highQuality = completed.filter { it.setupScore >= 75 }
    val highWin = if (highQuality.isNotEmpty()) highQuality.count { (it.resultR ?: 0.0) > 0 }.toDouble() / highQuality.size * 100 else null

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("الصفقات", trades.size.toString(), Modifier.weight(1f))
            StatTile("Win Rate", winRate?.let { "${it.roundToInt()}%" } ?: "—", Modifier.weight(1f))
            StatTile("متوسط R", avgR?.fmt(2) ?: "—", Modifier.weight(1f))
        }
        if (highWin != null) {
            Text("صفقات Score ≥75: نجاح ${highWin.roundToInt()}% (${highQuality.size})", modifier = Modifier.padding(horizontal = 14.dp), color = Positive, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = { addOpen = true }) { Text("+ إضافة صفقة") }
        }

        if (trades.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "لا توجد صفقات مسجلة بعد.\nسجل سبب الدخول ونتيجة R حتى نقيس أي إعداد ينجح فعلاً.",
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trades, key = { it.id }) { t -> TradeCard(t) { vm.deleteTrade(t.id) } }
            }
        }
    }

    if (addOpen) AddTradeDialog(onDismiss = { addOpen = false }) {
        vm.saveTrade(it)
        addOpen = false
    }
}

@Composable
private fun TradeCard(t: TradeEntry, onDelete: () -> Unit) {
    val resultColor = when { t.resultR == null -> MutedText; t.resultR > 0 -> Positive; else -> Negative }
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Surface(
                    color = if (t.side == "BUY") Positive.copy(alpha = 0.14f) else Negative.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(t.side, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = if (t.side == "BUY") Positive else Negative, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Score ${t.setupScore}/100", fontWeight = FontWeight.Bold)
                    Text(formatTradeTime(t.timestamp), color = MutedText, fontSize = 10.sp)
                }
                Text(t.resultR?.let { "${if (it > 0) "+" else ""}${it.fmt(2)}R" } ?: "مفتوحة", color = resultColor, fontWeight = FontWeight.Bold)
            }
            Text("Entry ${t.entry.fmt(2)} • SL ${t.stopLoss.fmt(2)} • TP ${t.takeProfit.fmt(2)}", color = androidx.compose.ui.graphics.Color(0xFFCBD5E1), fontSize = 11.sp)
            if (t.notes.isNotBlank()) Text(t.notes, color = MutedText, fontSize = 11.sp)
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) { Text("حذف", color = Negative, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun AddTradeDialog(onDismiss: () -> Unit, onSave: (TradeEntry) -> Unit) {
    var side by remember { mutableStateOf("BUY") }
    var entry by remember { mutableStateOf("") }
    var sl by remember { mutableStateOf("") }
    var tp by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("75") }
    var notes by remember { mutableStateOf("") }
    val canSave = entry.toDoubleOrNull() != null && sl.toDoubleOrNull() != null && tp.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل صفقة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = side == "BUY", onClick = { side = "BUY" }, label = { Text("BUY") })
                    FilterChip(selected = side == "SELL", onClick = { side = "SELL" }, label = { Text("SELL") })
                }
                NumberFields(entry, { entry = it }, sl, { sl = it }, tp, { tp = it })
                NumField("النتيجة R (اختياري)", result, { result = cleanSignedNumber(it) }, Modifier.fillMaxWidth())
                NumField("Setup Score", score, { score = cleanNumber(it) }, Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it.take(220) }, label = { Text("ملاحظات / سبب الدخول") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            Button(enabled = canSave, onClick = {
                onSave(
                    TradeEntry(
                        id = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis(),
                        side = side,
                        entry = entry.toDouble(),
                        stopLoss = sl.toDouble(),
                        takeProfit = tp.toDouble(),
                        resultR = result.toDoubleOrNull(),
                        setupScore = (score.toIntOrNull() ?: 0).coerceIn(0, 100),
                        notes = notes.trim()
                    )
                )
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
internal fun SettingsDialog(vm: GoldEdgeViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var lead by remember { mutableIntStateOf(vm.storage.newsLeadMinutes) }
    var risk by remember { mutableStateOf(vm.storage.defaultRiskPct.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإعدادات والمصادر") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تنبيه الخبر القوي قبل $lead دقيقة", fontWeight = FontWeight.Bold)
                Slider(value = lead.toFloat(), onValueChange = { lead = it.roundToInt().coerceIn(5, 60) }, valueRange = 5f..60f, steps = 10)
                Text("المخاطرة الافتراضية ${risk.fmt(1)}%", fontWeight = FontWeight.Bold)
                Slider(value = risk, onValueChange = { risk = it }, valueRange = 0.25f..3f)
                HorizontalDivider()
                Text("المصادر المدمجة", color = AccentGold, fontWeight = FontWeight.Bold)
                Text(
                    "• Forex Factory: تقويم أخبار USD\n• Myfxbook: مزاج XAUUSD\n• Yahoo Finance: Gold Futures / DXY / US10Y\n• TradingView: مرجع Technical Ratings",
                    color = MutedText,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = { openUrl(context, MarketRepository.TRADINGVIEW_XAUUSD) }, label = { Text("TradingView") })
                    AssistChip(onClick = { openUrl(context, MarketRepository.MYFXBOOK_XAUUSD) }, label = { Text("Myfxbook") })
                }
                Text("GoldEdge لا يرسل أوامر تداول ولا يَعِد بعائد. الدرجة فلتر قرار وليست توصية مالية.", color = Negative, fontSize = 10.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                vm.updateNewsLead(lead)
                vm.updateRiskDefault(risk.toDouble())
                onDismiss()
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}
