package com.masari.personalplan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "masari_tasks";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!context.getSharedPreferences("masari_data", Context.MODE_PRIVATE).getBoolean("smart_reminders_enabled", true)) return;
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        int id = intent.getIntExtra("id", (int) System.currentTimeMillis());
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "تذكيرات مساري", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("تذكيرات قبل المهام المهمة");
            ch.enableLights(true);
            ch.setLightColor(Color.rgb(25,151,82));
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(context, MasariV11Activity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title == null ? "مساري" : title)
                .setContentText(body == null ? "اقترب وقت مهمتك" : body)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);
        nm.notify(id, b.build());
    }
}
