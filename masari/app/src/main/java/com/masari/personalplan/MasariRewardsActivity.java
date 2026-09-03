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

public class MasariRewardsActivity extends Activity {
    private static final int BG = Color.rgb(246, 248, 252);
    private static final int NAVY = Color.rgb(24, 49, 83);
    private static final int TEXT = Color.rgb(31, 40, 54);
    private static final int MUTED = Color.rgb(103, 113, 128);
    private static final int GREEN = Color.rgb(22, 123, 98);
    private static final int GOLD = Color.rgb(184, 126, 28);
    private static final int BORDER = Color.rgb(225, 231, 239);
    private static final int BONUS = Color.rgb(128, 88, 172);

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
        Calendar now = Calendar.getInstance();
        List<Task> tasks = tasksForDate(now);
        int dayPoints = getDayPoints(now);
        int dayTarget = targetForDate(now);
        int planPoints = getPlanYearPoints();

        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "المكافآت + جدولك الحقيقي");

        LinearLayout hero = card();
        hero.setPadding(dp(16), dp(15), dp(16), dp(15));
        addCard(root, hero, dp(14));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.addView(text(arabicNumber(planPoints) + " نقطة", 25, NAVY, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(pill("المستوى " + arabicNumber(1 + planPoints / 500), GOLD, Color.rgb(255, 247, 226)));
        hero.addView(row);
        TextView score = text("اليوم: " + arabicNumber(dayPoints) + " / " + arabicNumber(dayTarget) + " نقطة أساسية", 14, GREEN, true);
        score.setPadding(0, dp(8), 0, dp(6));
        hero.addView(score);
        hero.addView(progressBar(dayPoints, dayTarget, GREEN));
        if (dayPoints > dayTarget) hero.addView(detailText("تجاوزت الهدف الأساسي بـ " + arabicNumber(dayPoints - dayTarget) + " نقطة إضافية."));

        addLeapAheadCard(root);

        Task current = currentTask(tasks);
        if (current != null) addCurrentTask(root, current);

        TextView wheelTitle = text("دائرة اليوم", 21, NAVY, true);
        wheelTitle.setPadding(0, dp(20), 0, dp(4));
        root.addView(wheelTitle);
        TextView wheelSub = text("القطاعات مأخوذة من جدولك الفعلي، والمهام غير اليومية لا تظهر إلا في يومها.", 13, MUTED, false);
        wheelSub.setPadding(0, 0, 0, dp(8));
        root.addView(wheelSub);
        root.addView(new DayWheelView(this, tasks), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(315)));

        Button later = button("＋ احفظ فكرة في «لاحقًا»", NAVY);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        l.setMargins(0, dp(8), 0, 0);
        root.addView(later, l);
        later.setOnClickListener(v -> showQuickLaterDialog());

        TextView tasksTitle = text("مهام اليوم المحددة", 22, NAVY, true);
        tasksTitle.setPadding(0, dp(22), 0, dp(3));
        root.addView(tasksTitle);
        TextView hint = text("المهام الأساسية تدخل في هدف اليوم. البنفسجية «إضافية» وتعطي نقاطًا لكنها لا تجعل يومك ناقصًا إذا تركتها.", 13, MUTED, false);
        hint.setPadding(0, 0, 0, dp(8));
        root.addView(hint);
        for (Task task : tasks) addTaskCard(root, task, current != null && current.id.equals(task.id));
        return scroll;
    }

    private void addLeapAheadCard(LinearLayout root) {
        int done = 0;
        if (isDoneById("leap1")) done++;
        if (isDoneById("leap2")) done++;
        if (isDoneById("leap3")) done++;
        LinearLayout c = card();
        c.setPadding(dp(15), dp(13), dp(15), dp(13));
        addCard(root, c, dp(12));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        top.addView(text("LeapAhead", 18, domainColor("المعرفة والقراءة"), true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(pill(arabicNumber(done) + " / ٣", domainColor("المعرفة والقراءة"), Color.rgb(247, 243, 252)));
        c.addView(top);
        c.addView(detailText("الحد الأدنى اليومي: كتابان • الهدف: ٣ كتب • المحتوى يتجدد عند ١٢:٠٠ ليلًا."));
        c.addView(detailText(done >= 2 ? "✓ حققت الحد الأدنى اليوم. الكتاب الثالث مكافأة إضافية." : "باقي " + arabicNumber(2 - done) + " للوصول إلى الحد الأدنى."));
        ProgressBar p = progressBar(done, 3, domainColor("المعرفة والقراءة"));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12));
        pp.setMargins(0, dp(7), 0, 0);
        c.addView(p, pp);
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
        line.addView(text("الآن", 13, GREEN, true));
        TextView points = pill("+" + arabicNumber(task.points), domainColor(task.domain), Color.WHITE);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.setMargins(dp(8), 0, 0, 0);
        line.addView(points, pp);
        c.addView(line);
        TextView title = text(task.title, 18, NAVY, true);
        title.setPadding(0, dp(5), 0, 0);
        c.addView(title);
        c.addView(detailText(formatTimeRange(task.startMin, task.endMin) + " • " + task.domain + (task.required ? " • أساسي" : " • إضافي")));
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
        cb.setButtonTintList(ColorStateList.valueOf(task.required ? domainColor(task.domain) : BONUS));
        cb.setChecked(isTaskDone(task));
        top.addView(cb, new LinearLayout.LayoutParams(dp(45), dp(48)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        info.addView(text(task.title, 16, TEXT, true));
        TextView time = text(formatTimeRange(task.startMin, task.endMin) + (task.required ? " • أساسي" : " • إضافي"), 12, task.required ? MUTED : BONUS, false);
        time.setPadding(0, dp(2), 0, 0);
        info.addView(time);
        top.addView(pill("+" + arabicNumber(task.points), task.required ? domainColor(task.domain) : BONUS, Color.rgb(248, 250, 253)));
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

    private List<Task> tasksForDate(Calendar date) {
        List<Task> t = new ArrayList<>();
        int day = date.get(Calendar.DAY_OF_WEEK);
        int week = date.get(Calendar.WEEK_OF_YEAR);
        t.add(new Task("fajr", 240, 390, "الفجر والحلقة والدرس", "فتح المسجد، الصلاة والأذكار، تحفيظ الطلاب حتى ٦:٠٠، ثم درس العلم حتى ٦:٣٠.", "الدين والمسجد", 5, true));

        if (day == Calendar.SATURDAY) {
            t.add(new Task("workoutA", 390, 420, "تمرين A — كتف وذراعان + جسم كامل", "٣د إحماء؛ Scapular push-up على الحائط ٢×٨؛ ضغط مائل بطيء ٢×٦-١٠؛ سحب حقيبة ٢×٨-١٢؛ دوران خارجي للكتف ٢×١٠؛ رسغ/ساعد ٢×١٠؛ Squat ٢×٨-١٢؛ سمانة بطيئة ٢×١٢؛ Fast feet ٤×١٥ث. أي ألم حاد في الكتف = توقف.", "الصحة", 20, true));
        } else if (day == Calendar.TUESDAY) {
            t.add(new Task("workoutB", 390, 420, "تمرين B — أوتار وقبضة وسرعة", "٣د إحماء؛ Wall slide ٢×٨؛ ضغط مائل ٢×٨؛ Hip hinge بحقيبة خفيفة ٢×١٠؛ Split squat ٢×٦-٨ لكل رجل؛ ثبات سمانة ٢×٢٠-٣٠ث؛ قبضة منشفة/حمل ٢×٣٠ث؛ تغيير اتجاه ٥×١٢ث؛ لمسات كرة سريعة ٤×٢٠ث. لا تمارين شد أعصاب قسرية.", "الصحة", 20, true));
        }

        String englishTitle;
        String englishDetails;
        boolean englishRequired = true;
        int englishPoints = 25;
        switch (day) {
            case Calendar.SATURDAY:
                englishTitle = "إنجليزي — Vocabulary + Reading Explorer";
                englishDetails = "English Vocabulary in Use B2: وحدة واحدة مع ٥ كلمات مهمة فقط + Reading Explorer 4: قراءة موقّتة للفكرة ثم الأسئلة ثم تلخيص ٣ جمل.";
                break;
            case Calendar.SUNDAY:
                englishTitle = "إنجليزي — Vocabulary + Tactics";
                englishDetails = "Vocabulary in Use: وحدة + Tactics for Listening Developing: استماع أول بلا نص، أجب، ثم استمع ثانية وراجع الكلمات التي منعت الفهم.";
                break;
            case Calendar.MONDAY:
                englishTitle = "إنجليزي — Vocabulary + Reading Explorer";
                englishDetails = "وحدة Vocabulary in Use + نص Reading Explorer 4 بمؤقت. لا تترجم كل كلمة؛ التقط الفكرة ثم راجع الكلمات المؤثرة فقط.";
                break;
            case Calendar.TUESDAY:
                englishTitle = "إنجليزي — Vocabulary + Tactics";
                englishDetails = "وحدة Vocabulary in Use + Tactics Developing. الهدف فهم الصوت دون قراءة الترجمة من المحاولة الأولى.";
                break;
            case Calendar.WEDNESDAY:
                englishTitle = "إنجليزي — Vocabulary + Oxford Bookworms";
                englishDetails = "وحدة Vocabulary in Use + قراءة ٢٠-٢٥ دقيقة من Oxford Bookworms Stage 4، ثم Stage 5 عندما تصبح 4 سهلة. لخص الفكرة دون ترجمة كلمة بكلمة.";
                break;
            case Calendar.THURSDAY:
                englishTitle = "اختبار الإنجليزية الأسبوعي";
                englishDetails = "Tactics ٢٠د + اختبار ٢٠ كلمة + نص قصير بسرعة + سؤال استماع. سجّل فقط الأخطاء التي تكررت.";
                break;
            default:
                englishTitle = "استماع إنجليزي ممتع — يوم خفيف";
                englishDetails = "٢٠ دقيقة فيديو/درس تحبه بالإنجليزية دون ترجمة أولًا. يوم الجمعة مرن، وهذه مهمة إضافية لا تدخل هدف اليوم.";
                englishRequired = false;
                englishPoints = 10;
        }
        t.add(new Task("english", 420, 480, englishTitle, englishDetails, "الإنجليزية والقبول", englishPoints, englishRequired));
        t.add(new Task("sleep2", 480, 570, "النوم التكميلي", "نم حتى ٩:٣٠ قدر الإمكان. لا تحول هذه الفترة إلى Shorts أو بحث جديد.", "الصحة", 5, true));
        t.add(new Task("leap1", 570, 600, "LeapAhead — الكتاب ١", "مع الفطور: أكمل كتابًا مؤقتًا واحدًا أو استمع ٢٥ دقيقة على الأقل. بعده احتفظ بفكرة واحدة فقط تستحق التذكر.", "المعرفة والقراءة", 7, true));

        String workTitle;
        String workDetails;
        boolean workRequired = day != Calendar.FRIDAY;
        int workPoints = workRequired ? 25 : 10;
        switch (day) {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
                workTitle = "العمل — بناء المنتج";
                workDetails = "أكاديمية واحدة فقط: حسّن درسًا واحدًا أو اختبارًا واحدًا، أصلح أهم خطأ، واختبره على الهاتف. لا تبدأ أكاديمية جديدة.";
                break;
            case Calendar.MONDAY:
            case Calendar.TUESDAY:
                workTitle = "العمل — الوصول للسوق";
                workDetails = "اختر فئة طلاب واحدة، اكتب عرضًا واضحًا واحدًا، ثم نفذ خطوة وصول حقيقية: رسالة/منشور/تجربة إعلان صغيرة/سؤال ٣ أشخاص. لا تكتف بالتعلم.";
                break;
            case Calendar.WEDNESDAY:
                workTitle = "العمل — دخل مباشر";
                workDetails = "ابحث عن فرصة واحدة قابلة للتنفيذ بالهاتف: تقوية إنجليزي/علوم، تلخيص، تدقيق، أو خدمة واضحة. الهدف التواصل مع شخص واحد أو نشر عرض واحد.";
                break;
            case Calendar.THURSDAY:
                workTitle = "العمل — مراجعة الأرقام";
                workDetails = "اكتب: كم رأى العرض؟ كم جرّب؟ كم سأل؟ كم دفع؟ ثم اختر عائقًا واحدًا فقط لإصلاحه في الأسبوع التالي.";
                break;
            default:
                workTitle = "مراجعة مالية خفيفة — يوم مرن";
                workDetails = "راجع ٢٠-٣٠ دقيقة ما حصل هذا الأسبوع، أو اتركها للراحة إذا كان هذا يومك المرن. لا تبدأ مشروعًا جديدًا.";
        }
        t.add(new Task("work", 600, 690, workTitle, workDetails, "العمل والدخل", workPoints, workRequired));
        t.add(new Task("quran1", 705, 780, "القرآن — مراجعة جديدة: صفحتان", "قبل الظهر: صفحتان جديدتان. بعد كل صفحة اختبر: أول الصفحة، وسطها، آخرها، وموقع آيتين على الأقل في الصفحة.", "القرآن", 14, true));
        t.add(new Task("lunch", 780, 810, "الغداء", "من ١:٠٠ إلى ١:٣٠. راحة قصيرة ولا تفتح سلسلة فيديوهات طويلة.", "الانضباط", 3, true));
        t.add(new Task("leap2", 810, 855, "LeapAhead — الكتاب ٢", "الكتاب الثاني هو الذي يكمل الحد الأدنى اليومي. اختر المفيد، وليس فقط لأنه سيختفي عند ١٢ ليلًا.", "المعرفة والقراءة", 8, true));
        t.add(new Task("quran2", 885, 970, "القرآن — صفحتان جديدتان + مراجعة قديمة", "قبل العصر: صفحتان جديدتان، ثم صفحتان قديمتان أو ١٠ دقائق اختبار بصري. اختبر: ماذا قبل/بعد آية عشوائية؟ ما نهاية الآية وبداية التالية؟ ما أول/آخر الصفحة؟", "القرآن", 18, true));
        addAfternoonTask(t, day, week);
        t.add(new Task("maghrib", 1080, 1200, "المغرب والتحفيظ والعشاء", "فتح المسجد، أذكار المساء، التحفيظ، صلاة العشاء، ثم محاسبة الطلاب.", "الدين والمسجد", 5, true));
        t.add(new Task("leap3", 1230, 1275, "LeapAhead — الكتاب ٣ (مكافأة)", "إذا أكملت كتابين فقد حققت الحد الأدنى. الكتاب الثالث هدف إضافي فقط؛ لا تسهر بسببه ولا تزاحم النوم والقرآن.", "المعرفة والقراءة", 7, false));
        t.add(new Task("close", 1320, 1340, "إغلاق اليوم", "حدد ٣ مهام للغد، انقل الأفكار الجديدة إلى «لاحقًا»، ولا تبدأ بحثًا أو مشروعًا جديدًا الآن.", "الانضباط", 5, true));
        t.add(new Task("sleep", 1340, 1350, "الاستعداد للنوم", "أبعد الهاتف واستعد للنوم. الهدف: أن تقترب من ١٠:٣٠ م بدل السهر لإنهاء محتوى مؤقت.", "الصحة", 5, true));
        addCustomTasks(t, date);
        return t;
    }

    private void addCustomTasks(List<Task> tasks, Calendar date) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("custom_tasks", "[]"));
            int dow = date.get(Calendar.DAY_OF_WEEK);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null || !o.optBoolean("active", true)) continue;
                int repeatDay = o.optInt("day", 0);
                if (repeatDay != 0 && repeatDay != dow) continue;
                String id = o.optString("id", "custom_" + i);
                String title = o.optString("title", "مهمة مخصصة");
                String details = o.optString("details", "مهمة أضفتها بنفسك.");
                String domain = o.optString("domain", "الانضباط");
                if (!domainColors.containsKey(domain)) domain = "الانضباط";
                int points = Math.max(1, Math.min(100, o.optInt("points", 10)));
                int start = Math.max(0, Math.min(1439, o.optInt("start", 960)));
                int end = Math.max(start + 5, Math.min(1440, o.optInt("end", start + 30)));
                boolean required = o.optBoolean("required", false);
                tasks.add(new Task(id, start, end, title, details, domain, points, required));
            }
        } catch (Exception ignored) {}
    }

    private void addAfternoonTask(List<Task> t, int day, int week) {
        switch (day) {
            case Calendar.SATURDAY:
                t.add(new Task("family_talk", 990, 1020, "الأسرة + تدريب كلام قصير", "اعمل خدمة واحدة في البيت دون طلب، ثم سجل ٥-١٠ دقائق تتكلم في موضوع تعرفه بأقل توقفات.", "الأسرة", 8, true));
                break;
            case Calendar.SUNDAY:
                t.add(new Task("talk1", 990, 1020, "تدريب التواصل", "سجل ٥-١٠ دقائق: الفكرة → السبب → المثال → الخلاصة. ثم حاول بدء محادثة واحدة بنفسك خلال اليوم.", "التواصل", 10, true));
                break;
            case Calendar.MONDAY:
                t.add(new Task("family_friend", 990, 1020, "خدمة الأسرة + تفقد صديق", "ساعد في البيت بعمل واحد، وأرسل لصديق/زميل رسالة تسأل فيها عن حاله بصدق.", "الأسرة", 8, true));
                break;
            case Calendar.TUESDAY:
                t.add(new Task("talk2", 990, 1020, "تدريب الكلام والحزم", "تكلم ٥-١٠ دقائق، ثم تدرب على رد محترم في موقف خلاف: وضح رأيك بلا تبرير زائد ولا قسوة.", "التواصل", 10, true));
                break;
            case Calendar.WEDNESDAY:
                t.add(new Task("medicine", 990, 1050, "مراجعة طب قديم", "٤٥-٦٠ دقيقة فقط: هذا الأسبوع تشريح، الأسبوع التالي فسيولوجيا، ثم مصطلحات طبية، ثم قراءة تحاليل. راجع ما تعلمته سابقًا ولا تفتح منهجًا جديدًا.", "المعرفة والقراءة", 12, true));
                break;
            case Calendar.THURSDAY:
                t.add(new Task("khatera", 990, 1030, "خاطرة دينية + تدريب إلقاء", "اكتب ٣ نقاط ودليلًا صحيحًا لموضوع واحد، ثم اربطها بكلام بسيط وسجل/ألقِ ٥-١٠ دقائق. الهدف الربط والوضوح لا التكلف.", "التواصل", 12, true));
                break;
            default:
                if (week % 2 == 0) t.add(new Task("explore", 990, 1050, "استكشاف علمي — موضوع واحد", "اختر موضوعًا واحدًا فقط: طقس، جيولوجيا، إلكترونيات، علم نفس... ساعة واحدة ثم أعده إلى «لاحقًا» إن لم يكتمل.", "المعرفة والقراءة", 10, false));
                else t.add(new Task("cook", 990, 1050, "مهارة حياة — طبخ", "تعلم طبقًا بسيطًا أو مهارة منزلية عملية واحدة وطبقها بنفسك.", "الأسرة", 10, false));
        }
    }

    private Task currentTask(List<Task> tasks) {
        Calendar c = Calendar.getInstance();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        for (Task t : tasks) if (now >= t.startMin && now < t.endMin) return t;
        return null;
    }

    private boolean isDoneById(String id) { return prefs.getBoolean("reward_done_" + todayKey + "_" + id, false); }
    private boolean isTaskDone(Task task) { return prefs.getBoolean("reward_done_" + todayKey + "_" + task.id, false); }

    private void setTaskDone(Task task, boolean done) {
        boolean old = isTaskDone(task);
        if (old == done) return;
        int delta = done ? task.points : -task.points;
        String dayKey = "reward_day_points_" + todayKey;
        int day = Math.max(0, prefs.getInt(dayKey, 0) + delta);
        String domainKey = "reward_domain_" + task.domain;
        int domain = Math.max(0, prefs.getInt(domainKey, 0) + delta);
        prefs.edit().putBoolean("reward_done_" + todayKey + "_" + task.id, done).putInt(dayKey, day).putInt(domainKey, domain).apply();
    }

    private int targetForDate(Calendar date) {
        int total = 0;
        for (Task t : tasksForDate(date)) if (t.required) total += t.points;
        return Math.max(1, total);
    }

    private View buildDomainsScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "المجالات ونقاطك");
        TextView intro = text("كل مجال مستقل. الأوسمة تعكس التزامك بالمهام، وليست حكمًا على قيمتك أو إيمانك.", 13, MUTED, false);
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
            top.addView(text(domain, 17, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            top.addView(pill(arabicNumber(points) + " نقطة", domainColor(domain), Color.rgb(248, 250, 253)));
            c.addView(top);
            TextView badge = text("الوسام الحالي: " + currentBadgeName(domain, points), 13, domainColor(domain), true);
            badge.setPadding(0, dp(6), 0, dp(5));
            c.addView(badge);
            if (next[1] > 0) {
                c.addView(progressBar(next[0], next[1], domainColor(domain)));
                c.addView(detailText("باقي " + arabicNumber(Math.max(0, next[1] - next[0])) + " نقطة للوسام التالي"));
            } else c.addView(detailText("وصلت إلى أعلى وسام حالي في هذا المجال."));
        }
        return scroll;
    }

    private View buildBadgesScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "الأوسمة");
        TextView intro = text("تُفتح الأوسمة عند ١٠٠، ٣٠٠، ٧٠٠، ١٥٠٠ نقطة في كل مجال.", 13, MUTED, false);
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
                line.addView(text(unlocked ? "★" : "☆", 24, unlocked ? GOLD : MUTED, true), new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.WRAP_CONTENT));
                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                line.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                info.addView(text(names[i], 15, unlocked ? TEXT : MUTED, true));
                info.addView(text(arabicNumber(thresholds[i]) + " نقطة", 12, MUTED, false));
                line.addView(pill(unlocked ? "مفتوح" : "مغلق", unlocked ? GREEN : MUTED, Color.rgb(248, 250, 253)));
                c.addView(line);
            }
        }
        return scroll;
    }

    private View buildProgressScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "التقدم الحقيقي حسب جدولك");
        Calendar now = Calendar.getInstance();
        addPeriodProgress(root, "اليوم", getDayPoints(now), targetForDate(now), "الهدف يتغير تلقائيًا حسب مهام هذا اليوم. المهام الإضافية لا تدخل الهدف الأساسي.", GREEN);
        addPeriodProgress(root, "هذا الأسبوع", getWeekPoints(), getWeekTarget(), "من السبت إلى الجمعة، ويحسب تمريني السبت والثلاثاء فقط.", Color.rgb(46, 94, 170));
        addPeriodProgress(root, "هذا الشهر", getMonthPoints(), getMonthTarget(), "مجموع أهداف الأيام الفعلية في الشهر.", GOLD);
        addPeriodProgress(root, "الخطة السنوية", getPlanYearPoints(), getPlanYearTarget(), "من سبتمبر ٢٠٢٦ إلى نهاية مايو ٢٠٢٧.", BONUS);
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
        int pct = Math.min(100, Math.round(value * 100f / Math.max(1, target)));
        line.addView(pill(arabicNumber(pct) + "٪", color, Color.rgb(248, 250, 253)));
        c.addView(line);
        TextView score = text(arabicNumber(value) + " / " + arabicNumber(target) + " نقطة", 14, color, true);
        score.setPadding(0, dp(7), 0, dp(5));
        c.addView(score);
        c.addView(progressBar(value, target, color));
        c.addView(detailText(note));
    }

    private int getDayPoints(Calendar c) { return prefs.getInt("reward_day_points_" + dateKey(c), 0); }

    private Calendar saturdayStart(Calendar base) {
        Calendar c = (Calendar) base.clone();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int daysSinceSaturday = (dow - Calendar.SATURDAY + 7) % 7;
        c.add(Calendar.DAY_OF_MONTH, -daysSinceSaturday);
        return c;
    }

    private int getWeekPoints() {
        Calendar p = saturdayStart(Calendar.getInstance());
        int total = 0;
        for (int i = 0; i < 7; i++) { total += getDayPoints(p); p.add(Calendar.DAY_OF_MONTH, 1); }
        return total;
    }

    private int getWeekTarget() {
        Calendar p = saturdayStart(Calendar.getInstance());
        int total = 0;
        for (int i = 0; i < 7; i++) { total += targetForDate(p); p.add(Calendar.DAY_OF_MONTH, 1); }
        return total;
    }

    private int getMonthPoints() {
        Calendar p = (Calendar) Calendar.getInstance().clone();
        p.set(Calendar.DAY_OF_MONTH, 1);
        int month = p.get(Calendar.MONTH);
        int total = 0;
        while (p.get(Calendar.MONTH) == month) { total += getDayPoints(p); p.add(Calendar.DAY_OF_MONTH, 1); }
        return total;
    }

    private int getMonthTarget() {
        Calendar p = (Calendar) Calendar.getInstance().clone();
        p.set(Calendar.DAY_OF_MONTH, 1);
        int month = p.get(Calendar.MONTH);
        int total = 0;
        while (p.get(Calendar.MONTH) == month) { total += targetForDate(p); p.add(Calendar.DAY_OF_MONTH, 1); }
        return total;
    }

    private Calendar planStart() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.SEPTEMBER, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private Calendar planEnd() {
        Calendar c = Calendar.getInstance();
        c.set(2027, Calendar.MAY, 31, 23, 59, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c;
    }

    private int getPlanYearPoints() {
        Calendar start = planStart();
        Calendar end = planEnd();
        Calendar now = Calendar.getInstance();
        Calendar last = now.before(end) ? now : end;
        if (last.before(start)) return 0;
        int total = 0;
        Calendar p = (Calendar) start.clone();
        while (!p.after(last)) { total += getDayPoints(p); p.add(Calendar.DAY_OF_MONTH, 1); }
        return total;
    }

    private int getPlanYearTarget() {
        Calendar p = planStart();
        Calendar end = planEnd();
        int total = 0;
        while (!p.after(end)) { total += targetForDate(p); p.add(Calendar.DAY_OF_MONTH, 1); }
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
            c.addView(text("أي سؤال أو علم أو مشروع يخطر لك أثناء مهمة خزنه هنا. لا يتحول إلى مهمة إلا في مراجعتك الأسبوعية.", 14, TEXT, false));
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
        input.setHint("مثال: تعلم أساسيات الطقس");
        input.setGravity(Gravity.RIGHT);
        input.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this).setTitle("احفظها في لاحقًا").setMessage("اكتب الفكرة فقط ثم ارجع لما كنت تفعله.").setView(input)
                .setPositiveButton("حفظ", (d, w) -> { String value = input.getText().toString().trim(); if (!value.isEmpty()) saveLater(value); })
                .setNegativeButton("إلغاء", null).show();
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
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("مساري", 30, NAVY, true));
        titles.addView(text(subtitle, 13, MUTED, false));
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView ver = pill("0.7.0", NAVY, Color.rgb(238, 242, 248));
        row.addView(ver);
        root.addView(row);
        TextView date = text(arabicDate(), 15, TEXT, true);
        date.setPadding(0, dp(9), 0, 0);
        root.addView(date);
    }

    private ScrollView baseScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return scroll;
    }

    private LinearLayout baseRoot(ScrollView scroll) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(17), dp(17), dp(17), dp(30));
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

    private void addCard(LinearLayout root, View c, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(top), 0, 0);
        root.addView(c, lp);
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

    private TextView pill(String value, int textColor, int bg) {
        TextView t = text(value, 12, textColor, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(9), dp(5), dp(9), dp(5));
        t.setBackground(rounded(bg, 20));
        return t;
    }

    private ProgressBar progressBar(int value, int max, int color) {
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(Math.max(1, max));
        p.setProgress(Math.min(value, Math.max(1, max)));
        p.setProgressTintList(ColorStateList.valueOf(color));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(232, 236, 242)));
        return p;
    }

    private int domainColor(String domain) {
        Integer color = domainColors.get(domain);
        return color == null ? NAVY : color;
    }

    private String formatTimeRange(int start, int end) { return formatTime(start) + " - " + formatTime(end); }

    private String formatTime(int minute) {
        int h24 = (minute / 60) % 24;
        int min = minute % 60;
        String suffix = h24 < 12 ? "ص" : "م";
        int h = h24 % 12;
        if (h == 0) h = 12;
        return arabicNumber(h) + ":" + (min < 10 ? "٠" : "") + arabicNumber(min) + " " + suffix;
    }

    private String arabicDate() {
        return new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar")).format(new Date());
    }

    private String dateKey(Calendar c) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime()); }

    private String arabicNumber(int n) {
        return String.valueOf(n).replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤')
                .replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');
    }

    private Button button(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
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
        final boolean required;
        Task(String id, int startMin, int endMin, String title, String details, String domain, int points, boolean required) {
            this.id = id; this.startMin = startMin; this.endMin = endMin; this.title = title;
            this.details = details; this.domain = domain; this.points = points; this.required = required;
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
                int color = t.required ? domainColor(t.domain) : BONUS;
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
