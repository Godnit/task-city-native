package com.masari.personalplan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 248, 252);
    private static final int NAVY = Color.rgb(24, 49, 83);
    private static final int TEXT = Color.rgb(31, 40, 54);
    private static final int MUTED = Color.rgb(103, 113, 128);
    private static final int GREEN = Color.rgb(22, 123, 98);
    private static final int GOLD = Color.rgb(184, 126, 28);
    private static final int BORDER = Color.rgb(225, 231, 239);

    private static final int DAILY_TARGET = 120;
    private static final int WEEKLY_TARGET = 720;
    private static final int MONTHLY_TARGET = 3000;
    private static final int YEAR_TARGET = 25000;

    private SharedPreferences prefs;
    private String todayKey;
    private String activeTab = "today";

    private final LinkedHashMap<String, Integer> domainColors = new LinkedHashMap<>();
    private final LinkedHashMap<String, String[]> domainBadges = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("masari_data", Context.MODE_PRIVATE);
        todayKey = dateKey(Calendar.getInstance());
        setupDomains();
        showTab("today");
    }

    private void setupDomains() {
        domainColors.put("القرآن", Color.rgb(25, 126, 96));
        domainColors.put("الإنجليزية والقبول", Color.rgb(46, 94, 170));
        domainColors.put("العمل والدخل", Color.rgb(183, 122, 31));
        domainColors.put("الصحة", Color.rgb(184, 77, 61));
        domainColors.put("المعرفة والقراءة", Color.rgb(115, 83, 165));
        domainColors.put("التواصل", Color.rgb(35, 139, 158));
        domainColors.put("الأسرة", Color.rgb(170, 82, 121));
        domainColors.put("الدين والمسجد", Color.rgb(74, 117, 78));
        domainColors.put("الانضباط", Color.rgb(87, 96, 110));

        domainBadges.put("القرآن", new String[]{"بداية المراجعة", "ثابت الورد", "حافظ بصري", "طريق الإتقان"});
        domainBadges.put("الإنجليزية والقبول", new String[]{"فاتح المفردات", "مستمع ثابت", "قارئ قوي", "طريق الطلاقة"});
        domainBadges.put("العمل والدخل", new String[]{"أول تنفيذ", "باني مشروع", "مجرّب السوق", "صانع دخل"});
        domainBadges.put("الصحة", new String[]{"بدأ الحركة", "متدرّب ثابت", "تحمل أعلى", "قوة مستمرة"});
        domainBadges.put("المعرفة والقراءة", new String[]{"قارئ", "مستكشف", "واسع المعرفة", "موسوعي المسار"});
        domainBadges.put("التواصل", new String[]{"صوت أوضح", "محاور", "متحدث واثق", "خطيب متدرّب"});
        domainBadges.put("الأسرة", new String[]{"مبادر", "مساند", "واصل", "سند الأسرة"});
        domainBadges.put("الدين والمسجد", new String[]{"ملازم", "طالب علم", "ثابت المسار", "نافع بإذن الله"});
        domainBadges.put("الانضباط", new String[]{"منتظم", "ثابت", "منضبط", "لا ينقطع بسهولة"});
    }

    private void showTab(String tab) {
        activeTab = tab;
        setContentView(buildApp());
    }

    private View buildApp() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout content = new FrameLayout(this);
        shell.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        View screen;
        switch (activeTab) {
            case "domains": screen = buildDomainsScreen(); break;
            case "badges": screen = buildBadgesScreen(); break;
            case "progress": screen = buildProgressScreen(); break;
            case "later": screen = buildLaterScreen(); break;
            default: screen = buildTodayScreen();
        }
        content.addView(screen, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        shell.addView(buildBottomNav(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));
        return shell;
    }

    private View buildTodayScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "نظام النقاط والمكافآت");

        int dayPoints = getDayPoints(Calendar.getInstance());
        int planPoints = getPlanYearPoints();
        LinearLayout hero = card();
        hero.setPadding(dp(16), dp(15), dp(16), dp(15));
        addCard(root, hero, dp(14));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView total = text(arabicNumber(planPoints) + " نقطة", 25, NAVY, true);
        row.addView(total, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView lvl = pill("المستوى " + arabicNumber(1 + planPoints / 500), GOLD, Color.rgb(255, 247, 226));
        row.addView(lvl);
        hero.addView(row);
        TextView day = text("اليوم: " + arabicNumber(dayPoints) + " / " + arabicNumber(DAILY_TARGET) + " نقطة", 14, GREEN, true);
        day.setPadding(0, dp(8), 0, dp(6));
        hero.addView(day);
        hero.addView(progressBar(dayPoints, DAILY_TARGET, GREEN));

        List<Task> tasks = tasksForToday();
        Task current = currentTask(tasks);
        if (current != null) addCurrentTask(root, current);

        TextView wheelTitle = text("دائرة اليوم", 21, NAVY, true);
        wheelTitle.setPadding(0, dp(20), 0, dp(4));
        root.addView(wheelTitle);
        TextView wheelSub = text("كل قطاع يمثل وقت المهمة، والخط يشير إلى الوقت الحالي.", 13, MUTED, false);
        wheelSub.setPadding(0, 0, 0, dp(8));
        root.addView(wheelSub);
        DayWheelView wheel = new DayWheelView(this, tasks);
        root.addView(wheel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(315)));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        quick.setPadding(0, dp(8), 0, 0);
        Button later = button("＋ فكرة إلى لاحقًا", NAVY);
        quick.addView(later, new LinearLayout.LayoutParams(0, dp(46), 1f));
        later.setOnClickListener(v -> showQuickLaterDialog());
        root.addView(quick);

        TextView tasksTitle = text("مهام اليوم الدقيقة", 22, NAVY, true);
        tasksTitle.setPadding(0, dp(22), 0, dp(3));
        root.addView(tasksTitle);
        TextView hint = text("أنجز المهمة كما هي مكتوبة؛ لا توجد عبارات عامة مثل «اعمل على الإنجليزي». كل مهمة تعطي نقاط مجالها.", 13, MUTED, false);
        hint.setPadding(0, 0, 0, dp(8));
        root.addView(hint);

        for (Task task : tasks) addTaskCard(root, task, current != null && current.id.equals(task.id));
        return scroll;
    }

    private void addCurrentTask(LinearLayout root, Task task) {
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackground(rounded(Color.rgb(238, 248, 244), 18, Color.rgb(169, 216, 202)));
        addCard(root, c, dp(12));
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView now = text("الآن", 13, GREEN, true);
        line.addView(now);
        TextView points = pill("+" + arabicNumber(task.points), domainColor(task.domain), Color.WHITE);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.setMargins(dp(8), 0, 0, 0);
        line.addView(points, pp);
        c.addView(line);
        TextView title = text(task.title, 18, NAVY, true);
        title.setPadding(0, dp(5), 0, 0);
        c.addView(title);
        c.addView(detailText(formatTimeRange(task.startMin, task.endMin) + " • " + task.domain));
        c.addView(detailText(task.details));
    }

    private void addTaskCard(LinearLayout root, Task task, boolean current) {
        LinearLayout c = card();
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (current) c.setBackground(rounded(Color.rgb(240, 247, 255), 18, Color.rgb(164, 193, 226)));
        addCard(root, c, dp(7));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        CheckBox cb = new CheckBox(this);
        cb.setButtonTintList(ColorStateList.valueOf(domainColor(task.domain)));
        cb.setChecked(isTaskDone(task));
        top.addView(cb, new LinearLayout.LayoutParams(dp(45), dp(48)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        info.addView(text(task.title, 16, TEXT, true));
        TextView time = text(formatTimeRange(task.startMin, task.endMin), 12, MUTED, false);
        time.setPadding(0, dp(2), 0, 0);
        info.addView(time);

        TextView pts = pill("+" + arabicNumber(task.points), domainColor(task.domain), Color.rgb(248, 250, 253));
        top.addView(pts);
        c.addView(top);

        TextView d = text(task.details, 13, TEXT, false);
        d.setPadding(dp(4), dp(5), dp(45), 0);
        c.addView(d);
        TextView domain = text(task.domain, 12, domainColor(task.domain), true);
        domain.setPadding(dp(4), dp(6), dp(45), 0);
        c.addView(domain);

        cb.setOnCheckedChangeListener((buttonView, checked) -> {
            setTaskDone(task, checked);
            Toast.makeText(this, checked ? "+" + task.points + " نقطة في " + task.domain : "تم التراجع عن المهمة", Toast.LENGTH_SHORT).show();
            showTab("today");
        });
    }

    private List<Task> tasksForToday() {
        List<Task> t = new ArrayList<>();
        t.add(new Task("fajr", 240, 390, "الفجر والحلقة", "افتح المسجد، صلِّ ما تيسر، أذّن وأقم، أكمل الأذكار، اسمع للطلاب حتى ٦:٠٠، ثم احضر درس الشيخ حتى ٦:٣٠.", "الدين والمسجد", 5));
        t.add(new Task("exercise", 390, 420, "تمرين القوة الصباحي", "٥د إحماء + ٣×١٢ سكوات + ٣×٨ ضغط مائل فقط إذا كان بلا ألم + ٣×٢٠ث بلانك. توقف عن أي حركة تؤلم الكتف.", "الصحة", 10));
        t.add(new Task("english", 420, 480, "ساعة الإنجليزية المركزة", "١٥ مفردة باسترجاع نشط + ٢٠د استماع بلا ترجمة أول مرة + ١٥د قراءة قصيرة + ١٠د مراجعة الكلمات والقواعد.", "الإنجليزية والقبول", 25));
        t.add(new Task("sleep2", 480, 570, "النوم التكميلي", "نم من ٨:٠٠ إلى ٩:٣٠ قدر الإمكان ولا تحوّل هذه الفترة إلى تصفح.", "الصحة", 5));
        t.add(new Task("book1", 570, 600, "ليب أهيد — الكتاب ١", "مع الفطور: استمع ٢٥ دقيقة على الأقل من الكتاب الأول أو أكمله إن كان أقصر.", "المعرفة والقراءة", 5));
        t.add(new Task("academy", 600, 690, "الأكاديمية — درس واحد فقط", "اختر درسًا واحدًا: بسّط ٣ فقرات صعبة، جرّب الشاشات، أصلح خطأ واحد واضح، وأي فكرة إضافية ضعها في «لاحقًا» بدل فتح تعديل جديد.", "العمل والدخل", 20));
        t.add(new Task("quran1", 705, 780, "القرآن قبل الظهر — صفحتان", "راجع صفحتين؛ بعد كل صفحة أغلق المصحف واستحضر أول الصفحة ووسطها وآخرها ومكان آيتين على الأقل.", "القرآن", 10));
        t.add(new Task("marketing", 810, 855, "التسويق — تعلم ثم تطبيق", "٢٠د من دورة التسويق المحملة، ثم ٢٥د تطبيق مباشر: اكتب عرضًا واحدًا للأكاديمية أو عدّل إعلانًا واحدًا فقط.", "العمل والدخل", 15));
        t.add(new Task("book2", 855, 880, "ليب أهيد — الكتاب ٢", "استمع ٢٥ دقيقة على الأقل من الكتاب الثاني أو أكمله إن كان أقصر.", "المعرفة والقراءة", 5));
        t.add(new Task("quran2", 885, 970, "القرآن قبل العصر — صفحتان", "راجع صفحتين جديدتين بنفس اختبار الاستحضار البصري، ثم صلِّ العصر واحضر الدرس.", "القرآن", 10));
        t.add(new Task("family", 990, 1020, "مبادرة للأسرة", "نفّذ خدمة منزلية واحدة دون أن يطلبوها منك أو اسأل والدتك تحديدًا: «هل تحتاجين مني شيئًا الآن؟» ونفّذ إن كان مناسبًا.", "الأسرة", 5));
        t.add(new Task("talk", 1020, 1050, "تدريب الكلام", "سجّل ٥ دقائق عن موضوع تعرفه، ثم أعد الفكرة مرة ثانية بأقل توقفات، وابدأ محادثة واحدة خلال اليوم بدل انتظار الطرف الآخر.", "التواصل", 10));
        t.add(new Task("maghrib", 1080, 1200, "المغرب والتحفيظ والعشاء", "افتح المسجد، التحفيظ بين المغرب والعشاء، صلِّ العشاء ثم حاسب الطلاب وأنهِ ما يلزم للحلقة.", "الدين والمسجد", 5));
        t.add(new Task("book3", 1230, 1260, "ليب أهيد — الكتاب ٣", "استمع ٢٥ دقيقة على الأقل من الكتاب الثالث أو أكمله إن كان أقصر.", "المعرفة والقراءة", 5));
        t.add(new Task("tomorrow", 1320, 1335, "إغلاق اليوم وتحديد الغد", "اختر ٣ مهام رئيسية للغد فقط، وانقل أي فكرة جديدة إلى «لاحقًا» دون البدء بها الليلة.", "الانضباط", 5));
        t.add(new Task("sleep", 1335, 1350, "الاستعداد للنوم", "أوقف Shorts والبحث، ضع الهاتف بعيدًا، جهز للنوم بحيث يكون الهدف ١٠:٣٠ م.", "الصحة", 5));
        return t;
    }

    private Task currentTask(List<Task> tasks) {
        Calendar c = Calendar.getInstance();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        for (Task t : tasks) if (now >= t.startMin && now < t.endMin) return t;
        return null;
    }

    private boolean isTaskDone(Task task) {
        return prefs.getBoolean("reward_done_" + todayKey + "_" + task.id, false);
    }

    private void setTaskDone(Task task, boolean done) {
        boolean old = isTaskDone(task);
        if (old == done) return;
        int delta = done ? task.points : -task.points;
        String dayKey = "reward_day_points_" + todayKey;
        int day = Math.max(0, prefs.getInt(dayKey, 0) + delta);
        String domainKey = "reward_domain_" + task.domain;
        int domain = Math.max(0, prefs.getInt(domainKey, 0) + delta);
        prefs.edit()
                .putBoolean("reward_done_" + todayKey + "_" + task.id, done)
                .putInt(dayKey, day)
                .putInt(domainKey, domain)
                .apply();
    }

    private View buildDomainsScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "المجالات ونقاطك");
        TextView intro = text("كل مجال يتقدم مستقلًا. لا يمكن لنقاط العمل أن تخفي ضعف القرآن أو العكس.", 13, MUTED, false);
        intro.setPadding(0, dp(12), 0, dp(6));
        root.addView(intro);

        for (String domain : domainColors.keySet()) {
            int points = prefs.getInt("reward_domain_" + domain, 0);
            int[] next = badgeProgress(points);
            LinearLayout c = card();
            c.setPadding(dp(15), dp(13), dp(15), dp(13));
            addCard(root, c, dp(8));
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            TextView name = text(domain, 17, TEXT, true);
            top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            top.addView(pill(arabicNumber(points) + " نقطة", domainColor(domain), Color.rgb(248, 250, 253)));
            c.addView(top);
            TextView badge = text("الوسام الحالي: " + currentBadgeName(domain, points), 13, domainColor(domain), true);
            badge.setPadding(0, dp(6), 0, dp(5));
            c.addView(badge);
            if (next[1] > 0) {
                c.addView(progressBar(next[0], next[1], domainColor(domain)));
                c.addView(detailText("باقي " + arabicNumber(Math.max(0, next[1] - next[0])) + " نقطة للوسام التالي"));
            } else {
                c.addView(detailText("وصلت إلى أعلى وسام حالي في هذا المجال."));
            }
        }
        return scroll;
    }

    private View buildBadgesScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "الأوسمة");
        TextView intro = text("تُفتح الأوسمة عند ١٠٠، ٣٠٠، ٧٠٠، ١٥٠٠ نقطة في المجال.", 13, MUTED, false);
        intro.setPadding(0, dp(12), 0, dp(6));
        root.addView(intro);
        int[] thresholds = {100, 300, 700, 1500};

        for (String domain : domainBadges.keySet()) {
            int points = prefs.getInt("reward_domain_" + domain, 0);
            TextView title = text(domain, 18, NAVY, true);
            title.setPadding(0, dp(14), 0, dp(5));
            root.addView(title);
            String[] names = domainBadges.get(domain);
            for (int i = 0; i < names.length; i++) {
                boolean unlocked = points >= thresholds[i];
                LinearLayout c = card();
                c.setPadding(dp(14), dp(11), dp(14), dp(11));
                if (!unlocked) c.setAlpha(0.58f);
                addCard(root, c, dp(5));
                LinearLayout line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(Gravity.CENTER_VERTICAL);
                line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                TextView icon = text(unlocked ? "★" : "☆", 24, unlocked ? GOLD : MUTED, true);
                line.addView(icon, new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.WRAP_CONTENT));
                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                line.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                info.addView(text(names[i], 15, unlocked ? TEXT : MUTED, true));
                info.addView(text(arabicNumber(thresholds[i]) + " نقطة", 12, MUTED, false));
                TextView state = pill(unlocked ? "مفتوح" : "مغلق", unlocked ? GREEN : MUTED, Color.rgb(248, 250, 253));
                line.addView(state);
                c.addView(line);
            }
        }
        return scroll;
    }

    private View buildProgressScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "التقدم اليومي والأسبوعي والشهري والسنوي");

        int daily = getDayPoints(Calendar.getInstance());
        int weekly = getWeekPoints();
        int monthly = getMonthPoints();
        int yearly = getPlanYearPoints();

        addPeriodProgress(root, "اليوم", daily, DAILY_TARGET, "الهدف: يوم قوي عند ١٢٠ نقطة", GREEN);
        addPeriodProgress(root, "هذا الأسبوع", weekly, WEEKLY_TARGET, "الأسبوع من السبت إلى الجمعة", Color.rgb(46, 94, 170));
        addPeriodProgress(root, "هذا الشهر", monthly, MONTHLY_TARGET, "الهدف الشهري ٣٠٠٠ نقطة", GOLD);
        addPeriodProgress(root, "الخطة السنوية", yearly, YEAR_TARGET, "من سبتمبر ٢٠٢٦ إلى نهاية مايو ٢٠٢٧", Color.rgb(115, 83, 165));

        LinearLayout c = card();
        c.setPadding(dp(15), dp(13), dp(15), dp(13));
        addCard(root, c, dp(14));
        c.addView(text("كيف تُحسب؟", 17, NAVY, true));
        c.addView(detailText("المهمة لا تمنحك النقاط إلا مرة واحدة في يومها. إذا ألغيت علامة الإنجاز تُسحب النقاط. التقدم الأسبوعي والشهري والسنوي هو مجموع نقاط الأيام الفعلية."));
        return scroll;
    }

    private void addPeriodProgress(LinearLayout root, String titleValue, int value, int target, String note, int color) {
        LinearLayout c = card();
        c.setPadding(dp(15), dp(13), dp(15), dp(13));
        addCard(root, c, dp(10));
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        line.addView(text(titleValue, 17, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int pct = Math.min(100, Math.round(value * 100f / target));
        line.addView(pill(arabicNumber(pct) + "٪", color, Color.rgb(248, 250, 253)));
        c.addView(line);
        TextView score = text(arabicNumber(value) + " / " + arabicNumber(target) + " نقطة", 14, color, true);
        score.setPadding(0, dp(7), 0, dp(5));
        c.addView(score);
        c.addView(progressBar(value, target, color));
        c.addView(detailText(note));
    }

    private int getDayPoints(Calendar c) {
        return prefs.getInt("reward_day_points_" + dateKey(c), 0);
    }

    private int getWeekPoints() {
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int daysSinceSaturday = (dow - Calendar.SATURDAY + 7) % 7;
        Calendar start = (Calendar) c.clone();
        start.add(Calendar.DAY_OF_MONTH, -daysSinceSaturday);
        int total = 0;
        Calendar p = (Calendar) start.clone();
        for (int i = 0; i < 7; i++) {
            total += getDayPoints(p);
            p.add(Calendar.DAY_OF_MONTH, 1);
        }
        return total;
    }

    private int getMonthPoints() {
        Calendar c = Calendar.getInstance();
        Calendar p = (Calendar) c.clone();
        p.set(Calendar.DAY_OF_MONTH, 1);
        int month = p.get(Calendar.MONTH);
        int total = 0;
        while (p.get(Calendar.MONTH) == month) {
            total += getDayPoints(p);
            p.add(Calendar.DAY_OF_MONTH, 1);
        }
        return total;
    }

    private int getPlanYearPoints() {
        Calendar start = Calendar.getInstance();
        start.set(2026, Calendar.SEPTEMBER, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = Calendar.getInstance();
        end.set(2027, Calendar.MAY, 31, 23, 59, 59);
        Calendar now = Calendar.getInstance();
        Calendar last = now.before(end) ? now : end;
        int total = 0;
        Calendar p = (Calendar) start.clone();
        while (!p.after(last)) {
            total += getDayPoints(p);
            p.add(Calendar.DAY_OF_MONTH, 1);
        }
        return total;
    }

    private int[] badgeProgress(int points) {
        int[] thresholds = {100, 300, 700, 1500};
        int prev = 0;
        for (int threshold : thresholds) {
            if (points < threshold) return new int[]{points - prev, threshold - prev};
            prev = threshold;
        }
        return new int[]{1, 0};
    }

    private String currentBadgeName(String domain, int points) {
        String[] names = domainBadges.get(domain);
        if (names == null) return "—";
        if (points >= 1500) return names[3];
        if (points >= 700) return names[2];
        if (points >= 300) return names[1];
        if (points >= 100) return names[0];
        return "لم يُفتح أول وسام بعد";
    }

    private View buildLaterScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "لاحقًا");
        Button add = button("＋ احفظ فكرة ولا تقطع مهمتك", NAVY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        lp.setMargins(0, dp(14), 0, dp(8));
        root.addView(add, lp);
        add.setOnClickListener(v -> showQuickLaterDialog());

        JSONArray arr = getLaterItems();
        if (arr.length() == 0) {
            LinearLayout c = card();
            c.setPadding(dp(15), dp(15), dp(15), dp(15));
            addCard(root, c, dp(8));
            c.addView(text("أي سؤال أو مجال جديد يخطر لك أثناء مهمة، خزّنه هنا ثم ارجع للمهمة. هذه القائمة لا تعطي نقاطًا حتى تتحول إلى مهمة فعلية في الخطة.", 14, TEXT, false));
        } else {
            for (int i = arr.length() - 1; i >= 0; i--) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                LinearLayout c = card();
                c.setPadding(dp(14), dp(12), dp(14), dp(12));
                addCard(root, c, dp(7));
                c.addView(text(o.optString("title"), 16, TEXT, true));
                c.addView(detailText(o.optString("created", "")));
                final int idx = i;
                Button delete = button("حذف", Color.rgb(145, 62, 62));
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(90), dp(40));
                dlp.gravity = Gravity.LEFT;
                c.addView(delete, dlp);
                delete.setOnClickListener(v -> deleteLaterItem(idx));
            }
        }
        return scroll;
    }

    private void showQuickLaterDialog() {
        EditText input = new EditText(this);
        input.setHint("مثال: كيف تتكون الأعاصير؟");
        input.setGravity(Gravity.RIGHT);
        input.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this)
                .setTitle("احفظها في لاحقًا")
                .setMessage("اكتب الفكرة فقط ثم ارجع لما كنت تفعله.")
                .setView(input)
                .setPositiveButton("حفظ", (d, w) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) saveLater(value);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private JSONArray getLaterItems() {
        try { return new JSONArray(prefs.getString("later_items", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void saveLater(String value) {
        try {
            JSONArray arr = getLaterItems();
            JSONObject o = new JSONObject();
            o.put("title", value);
            o.put("created", arabicDate());
            arr.put(o);
            prefs.edit().putString("later_items", arr.toString()).apply();
            Toast.makeText(this, "حُفظت في لاحقًا", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    private void deleteLaterItem(int index) {
        JSONArray arr = getLaterItems();
        if (index >= 0 && index < arr.length()) arr.remove(index);
        prefs.edit().putString("later_items", arr.toString()).apply();
        showTab("later");
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(dp(3), dp(4), dp(3), dp(4));
        nav.setElevation(dp(8));
        nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addNav(nav, "اليوم", "today");
        addNav(nav, "المجالات", "domains");
        addNav(nav, "الأوسمة", "badges");
        addNav(nav, "التقدم", "progress");
        addNav(nav, "لاحقًا", "later");
        return nav;
    }

    private void addNav(LinearLayout nav, String label, String key) {
        TextView v = text(label, 11, activeTab.equals(key) ? GREEN : MUTED, activeTab.equals(key));
        v.setGravity(Gravity.CENTER);
        if (activeTab.equals(key)) v.setBackground(rounded(Color.rgb(235, 247, 243), 15));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(1), dp(4), dp(1), dp(4));
        nav.addView(v, lp);
        v.setOnClickListener(x -> showTab(key));
    }

    private void addHeader(LinearLayout root, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        row.addView(block, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        block.addView(text("مساري", 30, NAVY, true));
        block.addView(text(subtitle, 14, MUTED, false));
        TextView version = pill("0.4.0", NAVY, Color.WHITE);
        row.addView(version);
        root.addView(row);
        TextView date = text(arabicDate(), 15, TEXT, true);
        date.setPadding(0, dp(9), 0, 0);
        root.addView(date);
    }

    private ProgressBar progressBar(int value, int max, int color) {
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(max);
        p.setProgress(Math.min(value, max));
        p.setProgressTintList(ColorStateList.valueOf(color));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(231, 235, 241)));
        p.setMinimumHeight(dp(9));
        return p;
    }

    private int domainColor(String domain) {
        Integer c = domainColors.get(domain);
        return c == null ? NAVY : c;
    }

    private String formatTimeRange(int start, int end) {
        return minuteToTime(start) + " – " + minuteToTime(end);
    }

    private String minuteToTime(int minute) {
        int h24 = (minute / 60) % 24;
        int m = minute % 60;
        String suffix = h24 < 12 ? "ص" : "م";
        int h = h24 % 12;
        if (h == 0) h = 12;
        return arabicNumber(h) + ":" + twoArabic(m) + " " + suffix;
    }

    private String twoArabic(int n) {
        String s = n < 10 ? "0" + n : String.valueOf(n);
        return toArabicDigits(s);
    }

    private String dateKey(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private String arabicDate() {
        return new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar")).format(new Date());
    }

    private String arabicNumber(int n) { return toArabicDigits(String.valueOf(n)); }

    private String toArabicDigits(String s) {
        return s.replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤')
                .replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');
    }

    private ScrollView baseScroll() {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.setBackgroundColor(BG);
        s.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return s;
    }

    private LinearLayout baseRoot(ScrollView scroll) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(17), dp(17), dp(17), dp(32));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        c.setBackground(rounded(Color.WHITE, 18, BORDER));
        c.setElevation(dp(1));
        return c;
    }

    private void addCard(LinearLayout root, View card, int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, topMargin, 0, 0);
        root.addView(card, lp);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView detailText(String value) {
        TextView t = text(value, 12, MUTED, false);
        t.setPadding(0, dp(5), 0, 0);
        return t;
    }

    private TextView pill(String value, int color, int bg) {
        TextView t = text(value, 12, color, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(9), dp(5), dp(9), dp(5));
        t.setBackground(rounded(bg, 22));
        return t;
    }

    private Button button(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(color, 14));
        return b;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable rounded(int color, int radiusDp, int stroke) {
        GradientDrawable d = rounded(color, radiusDp);
        d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    static class Task {
        final String id;
        final int startMin;
        final int endMin;
        final String title;
        final String details;
        final String domain;
        final int points;
        Task(String id, int startMin, int endMin, String title, String details, String domain, int points) {
            this.id = id; this.startMin = startMin; this.endMin = endMin; this.title = title;
            this.details = details; this.domain = domain; this.points = points;
        }
    }

    class DayWheelView extends View {
        private final List<Task> tasks;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();

        DayWheelView(Context context, List<Task> tasks) {
            super(context);
            this.tasks = tasks;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float radius = Math.min(w, h) * 0.38f;
            float stroke = dp(31);
            oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setColor(Color.rgb(229, 233, 240));
            canvas.drawArc(oval, 0, 360, false, paint);

            for (Task t : tasks) {
                float start = -90f + (t.startMin / 1440f) * 360f;
                float sweep = Math.max(1.5f, ((t.endMin - t.startMin) / 1440f) * 360f - 0.7f);
                int color = domainColor(t.domain);
                paint.setColor(isTaskDone(t) ? color : blendWithWhite(color, 0.54f));
                canvas.drawArc(oval, start, sweep, false, paint);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, radius - stroke * 0.62f, paint);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setColor(NAVY);
            textPaint.setTextSize(dp(18));
            canvas.drawText(arabicNumber(getDayPoints(Calendar.getInstance())), cx, cy - dp(4), textPaint);
            textPaint.setTextSize(dp(10));
            textPaint.setColor(MUTED);
            canvas.drawText("نقطة اليوم", cx, cy + dp(17), textPaint);

            drawHour(canvas, cx, cy, radius + stroke * 0.85f, 0, "٠");
            drawHour(canvas, cx, cy, radius + stroke * 0.85f, 6, "٦");
            drawHour(canvas, cx, cy, radius + stroke * 0.85f, 12, "١٢");
            drawHour(canvas, cx, cy, radius + stroke * 0.85f, 18, "١٨");

            Calendar now = Calendar.getInstance();
            int minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            double angle = Math.toRadians(-90 + (minute / 1440.0) * 360.0);
            float x2 = cx + (radius + stroke * 0.62f) * (float)Math.cos(angle);
            float y2 = cy + (radius + stroke * 0.62f) * (float)Math.sin(angle);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.rgb(35, 42, 53));
            canvas.drawLine(cx, cy, x2, y2, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, dp(4), paint);
        }

        private void drawHour(Canvas canvas, float cx, float cy, float r, int hour, String label) {
            double a = Math.toRadians(-90 + (hour / 24.0) * 360.0);
            float x = cx + r * (float)Math.cos(a);
            float y = cy + r * (float)Math.sin(a) + dp(4);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(dp(10));
            textPaint.setColor(MUTED);
            canvas.drawText(label, x, y, textPaint);
        }

        private int blendWithWhite(int color, float whiteAmount) {
            int r = (int)(Color.red(color) * (1 - whiteAmount) + 255 * whiteAmount);
            int g = (int)(Color.green(color) * (1 - whiteAmount) + 255 * whiteAmount);
            int b = (int)(Color.blue(color) * (1 - whiteAmount) + 255 * whiteAmount);
            return Color.rgb(r, g, b);
        }
    }
}
