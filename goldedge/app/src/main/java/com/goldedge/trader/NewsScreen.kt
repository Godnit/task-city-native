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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant

@Composable
internal fun NewsScreen(vm: GoldEdgeViewModel) {
    val context = LocalContext.current
    val news = vm.snapshot?.news.orEmpty().filter { it.country == "USD" }
    var filter by remember { mutableStateOf("الكل") }
    val now = Instant.now()
    val visible = news.filter {
        when (filter) {
            "قوي" -> it.isHigh
            "متوسط" -> it.isMedium
            else -> true
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("الكل", "قوي", "متوسط").forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
            }
        }

        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (vm.loading) "أحدث التقويم…" else "لا توجد أحداث مطابقة", color = MutedText)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(visible, key = { "${it.time}_${it.title}" }) { event ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ImpactDot(event.impact)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(event.title, fontWeight = FontWeight.Bold)
                                Text("${impactArabic(event.impact)} • ${formatEventTime(event.time)}", color = MutedText, fontSize = 11.sp)
                                if (event.forecast.isNotBlank() || event.previous.isNotBlank()) {
                                    Text("متوقع ${event.forecast.ifBlank { "—" }}  |  سابق ${event.previous.ifBlank { "—" }}", color = androidx.compose.ui.graphics.Color(0xFFCBD5E1), fontSize = 10.sp)
                                }
                            }
                            val mins = Duration.between(now, event.time).toMinutes()
                            Text(if (mins >= 0) humanDuration(mins) else "تم", color = if (event.isHigh) Negative else AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { openUrl(context, MarketRepository.FOREX_FACTORY_PAGE) }, modifier = Modifier.weight(1f)) { Text("المصدر") }
            Button(onClick = vm::refresh, modifier = Modifier.weight(1f)) { Text("تحديث") }
        }
    }
}
