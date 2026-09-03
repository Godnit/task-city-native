package com.goldedge.trader

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class NewsAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val storage = AppStorage(applicationContext)
            val lead = storage.newsLeadMinutes
            val events = MarketRepository(applicationContext).fetchNewsOnly()
            val now = Instant.now()
            events.filter { it.country == "USD" && it.isHigh }
                .forEach { event ->
                    val mins = Duration.between(now, event.time).toMinutes()
                    if (mins in 0..lead.toLong()) {
                        val key = "${event.time.epochSecond}_${event.title.hashCode()}"
                        if (!storage.wasNotified(key)) {
                            notifyEvent(event, mins.coerceAtLeast(0))
                            storage.markNotified(key)
                        }
                    }
                }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    private fun notifyEvent(event: EconomicEvent, minutes: Long) {
        ensureChannel(applicationContext)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val n = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("خبر USD قوي بعد $minutes دقيقة")
            .setContentText("${event.title} — راقب الذهب وتجنب الدخول المتأخر")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(event.title.hashCode(), n)
    }

    companion object {
        const val CHANNEL_ID = "goldedge_news"
        fun ensureChannel(context: Context) {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات أخبار الذهب",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيه قبل أخبار USD عالية التأثير"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}

object NewsAlertScheduler {
    private const val WORK_NAME = "goldedge_news_watch"

    fun ensureScheduled(context: Context) {
        NewsAlertWorker.ensureChannel(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<NewsAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
