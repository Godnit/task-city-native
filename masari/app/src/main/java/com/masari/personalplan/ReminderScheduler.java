package com.masari.personalplan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class ReminderScheduler {
    private static class Slot {
        final int hour, minute, before;
        final String title, body;
        Slot(int h, int m, int b, String t, String d) { hour=h; minute=m; before=b; title=t; body=d; }
    }

    private static final Slot[] SLOTS = {
            new Slot(7, 0, 10, "الإنجليزية بعد ١٠ دقائق", "جهّز Vocabulary/Reading/Listening قبل أن تبدأ ساعة التركيز."),
            new Slot(10, 0, 10, "كتلة العمل بعد ١٠ دقائق", "حدد خطوة واحدة قابلة للإنجاز في الأكاديمية أو الوصول للسوق."),
            new Slot(11, 45, 10, "القرآن قبل الظهر", "صفحتان مراجعة جديدة — لا تفتح بحثًا جانبيًا."),
            new Slot(14, 45, 10, "القرآن قبل العصر", "صفحتان جديدتان + مراجعة قديمة/اختبار بصري."),
            new Slot(22, 0, 10, "إغلاق اليوم بعد ١٠ دقائق", "حدد مهام الغد وانقل الأفكار إلى لاحقًا ثم أبعد الهاتف.")
    };

    public static void scheduleNextSevenDays(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar now = Calendar.getInstance();
        for (int d = 0; d < 7; d++) {
            Calendar day = (Calendar) now.clone();
            day.add(Calendar.DAY_OF_MONTH, d);
            for (int i = 0; i < SLOTS.length; i++) {
                Slot s = SLOTS[i];
                Calendar when = (Calendar) day.clone();
                when.set(Calendar.HOUR_OF_DAY, s.hour);
                when.set(Calendar.MINUTE, s.minute);
                when.set(Calendar.SECOND, 0);
                when.set(Calendar.MILLISECOND, 0);
                when.add(Calendar.MINUTE, -s.before);
                if (when.before(now)) continue;
                int request = day.get(Calendar.YEAR) * 100000 + day.get(Calendar.DAY_OF_YEAR) * 10 + i;
                Intent in = new Intent(context, ReminderReceiver.class);
                in.putExtra("title", s.title);
                in.putExtra("body", s.body);
                in.putExtra("id", request);
                PendingIntent pi = PendingIntent.getBroadcast(context, request, in, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pi);
                else am.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pi);
            }
        }
    }
}
