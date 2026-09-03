package com.goldedge.trader

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val AccentGold = Color(0xFFF5C451)
internal val Positive = Color(0xFF2DD4BF)
internal val Negative = Color(0xFFFB7185)
internal val InfoBlue = Color(0xFF60A5FA)
internal val AppBg = Color(0xFF08111F)
internal val SurfaceOne = Color(0xFF111C2E)
internal val SurfaceTwo = Color(0xFF17253A)
internal val MutedText = Color(0xFF94A3B8)

@Composable
internal fun GoldEdgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = AccentGold,
            secondary = Positive,
            tertiary = InfoBlue,
            background = AppBg,
            surface = SurfaceOne,
            surfaceVariant = SurfaceTwo,
            onPrimary = Color(0xFF18120A),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFFD6E0EC),
            error = Negative
        ),
        content = content
    )
}

@Composable
internal fun SectionCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceOne),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = MutedText, fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
            } else Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
internal fun InfoCard(title: String, body: String, accent: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = Color(0xFFD5DFEA), fontSize = 11.sp)
        }
    }
}

@Composable
internal fun ScoreGauge(value: Int, color: Color, label: String) {
    Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = SurfaceTwo,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * (value.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value%", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text(label, color = color, fontSize = 9.sp)
        }
    }
}

@Composable
internal fun StatTile(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = AccentGold, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(title, color = MutedText, fontSize = 9.sp)
        }
    }
}

@Composable
internal fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedText, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
internal fun NumberFields(entry: String, onEntry: (String) -> Unit, sl: String, onSl: (String) -> Unit, tp: String, onTp: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        NumField("Entry", entry, { onEntry(cleanNumber(it)) }, Modifier.weight(1f))
        NumField("SL", sl, { onSl(cleanNumber(it)) }, Modifier.weight(1f))
        NumField("TP", tp, { onTp(cleanNumber(it)) }, Modifier.weight(1f))
    }
}

@Composable
internal fun NumField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(value = value, onValueChange = onValue, label = { Text(label, fontSize = 10.sp) }, singleLine = true, modifier = modifier)
}

@Composable
internal fun ImpactDot(impact: String) {
    val color = when {
        impact.equals("High", true) -> Negative
        impact.equals("Medium", true) -> AccentGold
        else -> InfoBlue
    }
    Box(Modifier.size(12.dp).background(color, CircleShape))
}

internal fun biasArabic(bias: BiasDirection): String = when (bias) {
    BiasDirection.BULLISH -> "صاعد"
    BiasDirection.BEARISH -> "هابط"
    BiasDirection.NEUTRAL -> "محايد"
}

internal fun biasColor(bias: BiasDirection): Color = when (bias) {
    BiasDirection.BULLISH -> Positive
    BiasDirection.BEARISH -> Negative
    BiasDirection.NEUTRAL -> AccentGold
}

internal fun impactArabic(impact: String): String = when {
    impact.equals("High", true) -> "تأثير قوي"
    impact.equals("Medium", true) -> "تأثير متوسط"
    impact.equals("Low", true) -> "تأثير منخفض"
    else -> impact
}

internal fun formatEventTime(instant: Instant): String = DateTimeFormatter
    .ofPattern("EEE d MMM • HH:mm", Locale.getDefault())
    .format(instant.atZone(ZoneId.systemDefault()))

internal fun formatTradeTime(ms: Long): String = DateTimeFormatter
    .ofPattern("d MMM yyyy • HH:mm", Locale.getDefault())
    .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

internal fun humanDuration(minutes: Long): String = when {
    minutes < 60 -> "بعد ${minutes}د"
    minutes < 24 * 60 -> "بعد ${minutes / 60}س ${minutes % 60}د"
    else -> "بعد ${minutes / (24 * 60)}ي"
}

internal fun Double.fmt(decimals: Int): String = String.format(Locale.US, "% .${decimals}f", this).trim()
internal fun Float.fmt(decimals: Int): String = String.format(Locale.US, "% .${decimals}f", this).trim()
internal fun pct(v: Double): String = "${if (v >= 0) "+" else ""}${v.fmt(2)}%"

internal fun cleanNumber(value: String): String {
    val normalized = value.replace('٫', '.').replace(',', '.')
    val firstDot = normalized.indexOf('.')
    return normalized.filterIndexed { index, c -> c.isDigit() || (c == '.' && firstDot == index) }.take(14)
}

internal fun cleanSignedNumber(value: String): String {
    val normalized = value.replace('٫', '.').replace(',', '.')
    return buildString {
        normalized.forEachIndexed { i, c ->
            if (c.isDigit()) append(c)
            else if (c == '-' && i == 0) append(c)
            else if (c == '.' && !contains('.')) append(c)
        }
    }.take(14)
}

internal fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
