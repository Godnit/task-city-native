package com.masari.personalplan;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(20, 108, 91);
    private static final int NAVY = Color.rgb(25, 54, 93);
    private static final int TEXT = Color.rgb(30, 40, 55);
    private static final int MUTED = Color.rgb(101, 112, 128);
    private static final int BG = Color.rgb(246, 248, 252);
    private static final int GOLD = Color.rgb(173, 118, 24);

    private SharedPreferences prefs;
    private String todayKey;
    private String activeTab = "today";
    private final EditText[] taskInputs = new EditText[3];
    private final CheckBox[] taskChecks = new CheckBox[3];
    private final CheckBox[] indicatorChecks = new CheckBox[5];
    private TextView priorityProgress;
    private TextView indicatorProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        prefs = getSharedPreferences("masari_data", Context.MODE_PRIVATE);
        todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        showTab("today");
    }

    private void showTab(String tab) {
        saveTasks(false);
        activeTab = tab;
        setContentView(buildApp());
    }

    private View buildApp() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout content = new FrameLayout(this);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        shell.addView(content, contentLp);

        View screen;
        switch (activeTab) {
            case "plan": screen = buildPlanScreen(); break;
            case "progress": screen = buildProgressScreen(); break;
            case "later": screen = buildLaterPlaceholder(); break;
            case "more": screen = buildMoreScreen(); break;
            default: screen = buildTodayScreen();
        }
        content.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        shell.addView(buildBottomNav(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));
        return shell;
    }

    private View buildTodayScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);

        addHeader(root, "خطة اليوم • مرحلة التأسيس");
        addPhaseCard(root);
        addTopThreeCard(root);
        addIndicatorsCard(root);

        TextView timelineTitle = text("جدول اليوم الواضح", 22, NAVY, true);
        timelineTitle.setPadding(0, dp(24), 0, dp(4));
        root.addView(timelineTitle);
        TextView timelineSub = text("كل فترة لها مهمة محددة — لا توجد خانات من نوع «عمل أو إنجليزي أو رياضة»", 13, MUTED, false);
        timelineSub.setPadding(0, 0, 0, dp(10));
        root.addView(timelineSub);

        String[][] plan = scheduleForToday();
        for (int i = 0; i < plan.length; i++) {
            boolean highlight = plan[i][3].equals("1");
            addTimelineRow(root, plan[i][0], plan[i][1], plan[i][2], highlight);
        }

        TextView footer = text("هذه النسخة تطبق النقاط الرئيسية ١–٥ من خطة التطبيق.\nالبيانات اليومية محفوظة محليًا على الهاتف.", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, dp(8));
        root.addView(footer);
        return scroll;
    }

    private View buildPlanScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "الخطة • لماذا أستخدم مساري؟");

        TextView title = text("وظيفة التطبيق", 21, NAVY, true);
        title.setPadding(0, dp(18), 0, dp(8));
        root.addView(title);

        LinearLayout card = card();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, card, 0);
        addBullet(card, "ماذا يجب أن أفعل الآن؟");
        addBullet(card, "ما أهم شيء هذا الأسبوع؟");
        addBullet(card, "هل أتقدم فعلًا أم فقط أشعر أنني مشغول؟");
        addBullet(card, "أين أضع الفكرة الجديدة دون أن أفسد جدولي؟");

        TextView philosophy = text("فلسفة مساري", 21, NAVY, true);
        philosophy.setPadding(0, dp(22), 0, dp(8));
        root.addView(philosophy);

        LinearLayout philosophyCard = card();
        philosophyCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, philosophyCard, 0);
        addBullet(philosophyCard, "لا توجد «درجة حياة» وهمية؛ كل مجال يقاس بما يناسبه.");
        addBullet(philosophyCard, "القرآن يقاس بالصفحات والإتقان، والإنجليزية بالدقائق والاختبارات، والعمل بالساعات والدخل.");
        addBullet(philosophyCard, "الهدف هو معرفة التقدم الحقيقي لا جمع نقاط شكلية.");

        TextView now = text("تركيز مرحلة التأسيس", 21, NAVY, true);
        now.setPadding(0, dp(22), 0, dp(8));
        root.addView(now);
        LinearLayout nowCard = card();
        nowCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, nowCard, 0);
        addBullet(nowCard, "تثبيت القرآن يوميًا.");
        addBullet(nowCard, "رفع الإنجليزية باستمرار.");
        addBullet(nowCard, "عمل حقيقي على الدخل ومشروع الأكاديميات.");
        addBullet(nowCard, "تنظيم النوم وبناء الانضباط.");
        addBullet(nowCard, "تدريب الكلام والتواصل بجرعات صغيرة.");
        return scroll;
    }

    private View buildProgressScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "التقدم • أرقام حقيقية فقط");

        int doneTasks = countDoneTasks();
        int doneIndicators = countDoneIndicators();

        addMetricCard(root, "أهم مهام اليوم", "أنجزت " + arabicNumber(doneTasks) + " من ٣", NAVY);
        addMetricCard(root, "مؤشرات اليوم", "أنجزت " + arabicNumber(doneIndicators) + " من ٥", GREEN);

        LinearLayout note = card();
        note.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, note, dp(12));
        TextView noteTitle = text("مهم", 17, GOLD, true);
        note.addView(noteTitle);
        TextView noteText = text("لن أعطيك رقمًا مثل «حياتك ٧٢٪». عند إضافة وحدات القرآن والإنجليزية والعمل لاحقًا سيظهر لكل مجال مقياسه الحقيقي المستقل.", 14, TEXT, false);
        noteText.setPadding(0, dp(6), 0, 0);
        note.addView(noteText);
        return scroll;
    }

    private View buildLaterPlaceholder() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "لاحقًا");
        LinearLayout card = card();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        addCard(root, card, dp(18));
        TextView title = text("التبويب جاهز", 20, NAVY, true);
        card.addView(title);
        TextView msg = text("نظام «لاحقًا» الكامل هو من النقاط التالية في الخطة، لذلك لم أملأه بميزات ناقصة الآن. عند وصول دوره سيصبح مكانًا سريعًا لحفظ كل فكرة دون قطع المهمة الحالية.", 14, TEXT, false);
        msg.setPadding(0, dp(8), 0, 0);
        card.addView(msg);
        return scroll;
    }

    private View buildMoreScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "المزيد");
        addMetricCard(root, "الإصدار", "0.2.0", NAVY);
        addMetricCard(root, "النقاط المنفذة", "١ إلى ٥ من الخطة الرئيسية", GREEN);
        LinearLayout card = card();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, card, dp(12));
        addBullet(card, "يعمل دون إنترنت.");
        addBullet(card, "يحفظ بيانات اليوم محليًا.");
        addBullet(card, "مهيأ للتحديث فوق النسخة السابقة دون حذف البيانات.");
        return scroll;
    }

    private void addHeader(LinearLayout root, String subtitleValue) {
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleBlockLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleRow.addView(titleBlock, titleBlockLp);

        TextView title = text("مساري", 30, NAVY, true);
        titleBlock.addView(title);
        TextView subtitle = text(subtitleValue, 14, MUTED, false);
        subtitle.setPadding(0, dp(2), 0, 0);
        titleBlock.addView(subtitle);

        TextView badge = text("0.2.0", 12, Color.WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(6), dp(10), dp(6));
        badge.setBackground(rounded(NAVY, 30));
        titleRow.addView(badge);
        root.addView(titleRow);

        TextView date = text(arabicDate(), 16, TEXT, true);
        date.setPadding(0, dp(12), 0, 0);
        root.addView(date);
    }

    private void addPhaseCard(LinearLayout root) {
        LinearLayout focusCard = card();
        focusCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, focusCard, dp(16));
        TextView phase = text("المرحلة الحالية", 12, MUTED, true);
        focusCard.addView(phase);
        TextView phaseName = text("التأسيس — أثبّت النظام قبل أن أزيد الأهداف", 17, GREEN, true);
        phaseName.setPadding(0, dp(5), 0, 0);
        focusCard.addView(phaseName);
    }

    private void addTopThreeCard(LinearLayout root) {
        LinearLayout topCard = card();
        topCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        addCard(root, topCard, dp(14));

        LinearLayout sectionHead = new LinearLayout(this);
        sectionHead.setOrientation(LinearLayout.HORIZONTAL);
        sectionHead.setGravity(Gravity.CENTER_VERTICAL);
        sectionHead.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView topTitle = text("أهم ٣ أشياء اليوم", 20, NAVY, true);
        sectionHead.addView(topTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        priorityProgress = text("أنجزت ٠ من ٣", 13, GREEN, true);
        priorityProgress.setGravity(Gravity.CENTER);
        priorityProgress.setPadding(dp(10), dp(6), dp(10), dp(6));
        priorityProgress.setBackground(rounded(Color.rgb(232, 246, 242), 30));
        sectionHead.addView(priorityProgress);
        topCard.addView(sectionHead);

        String[] defaults = {
                "مراجعة ٤ صفحات من القرآن في المسجد",
                "الإنجليزية: ١٥ كلمة جديدة + ٢٠ دقيقة استماع",
                "مشروع الأكاديميات: تحسين شرح درس واحد وتجربته"
        };
        for (int i = 0; i < 3; i++) addTaskRow(topCard, i, defaults[i]);

        Button save = new Button(this);
        save.setText("حفظ مهام اليوم");
        save.setTextSize(15);
        save.setTextColor(Color.WHITE);
        save.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        save.setAllCaps(false);
        save.setBackground(rounded(GREEN, 16));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        saveLp.setMargins(0, dp(12), 0, 0);
        topCard.addView(save, saveLp);
        save.setOnClickListener(v -> saveTasks(true));
        updatePriorityProgress();
    }

    private void addIndicatorsCard(LinearLayout root) {
        LinearLayout card = card();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        addCard(root, card, dp(14));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title = text("المؤشرات اليومية الخمسة", 19, NAVY, true);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        indicatorProgress = text("أنجزت ٠ من ٥", 13, GREEN, true);
        indicatorProgress.setPadding(dp(9), dp(5), dp(9), dp(5));
        indicatorProgress.setBackground(rounded(Color.rgb(232, 246, 242), 24));
        head.addView(indicatorProgress);
        card.addView(head);

        String[] labels = {
                "القرآن",
                "الإنجليزية / القبول",
                "العمل / الدخل",
                "الصحة أو تدريب التواصل",
                "إغلاق اليوم والنوم في الوقت المستهدف"
        };
        for (int i = 0; i < labels.length; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(labels[i]);
            cb.setTextSize(15);
            cb.setTextColor(TEXT);
            cb.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            cb.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));
            cb.setPadding(0, dp(5), 0, dp(5));
            cb.setChecked(prefs.getBoolean("indicator_" + i + "_" + todayKey, false));
            final int idx = i;
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("indicator_" + idx + "_" + todayKey, isChecked).apply();
                updateIndicatorProgress();
            });
            indicatorChecks[i] = cb;
            card.addView(cb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        }
        updateIndicatorProgress();
    }

    private String[][] scheduleForToday() {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_WEEK);
        String lateAfternoon;
        switch (day) {
            case Calendar.SATURDAY:
            case Calendar.MONDAY:
            case Calendar.WEDNESDAY:
                lateAfternoon = "تمارين قوة خفيفة ٢٠ دقيقة + استحمام وتجهيز للمغرب";
                break;
            case Calendar.SUNDAY:
            case Calendar.TUESDAY:
                lateAfternoon = "تدريب الكلام: تسجيل صوت ١٠ دقائق + بدء محادثة واحدة";
                break;
            case Calendar.THURSDAY:
                lateAfternoon = "مراجعة مالية أسبوعية ٢٠ دقيقة + جلوس مع الأسرة";
                break;
            default:
                lateAfternoon = "مراجعة الأسبوع ٢٠ دقيقة + وقت مع الأسرة";
        }

        return new String[][]{
                {"من ٤:٠٠ ص إلى ٦:٣٠ ص", "الفجر • المسجد • التحفيظ • الدرس الشرعي", "ثابت", "0"},
                {"من ٧:٠٠ ص إلى ٨:٠٠ ص", "الإنجليزية: ١٥ كلمة + ٢٠ دقيقة استماع + مراجعة الكلمات", "ساعة التركيز الذهبية", "1"},
                {"من ٨:٠٠ ص إلى ٩:٣٠ ص", "نوم تكميلي", "طاقة", "0"},
                {"من ٩:٣٠ ص إلى ١٠:٠٠ ص", "الفطور + تجهيز مكان العمل", "بداية اليوم الثاني", "0"},
                {"من ١٠:٠٠ ص إلى ١١:٣٠ ص", "مشروع الأكاديميات: تحسين شرح درس واحد + اختبار التطبيق", "عمل مركز", "1"},
                {"من ١١:٤٥ ص إلى ١:٠٠ م", "الظهر + الغداء + عودة للبيت", "ثابت", "0"},
                {"من ١:١٥ م إلى ٢:١٥ م", "المال والتسويق: ٢٠ دقيقة تعلم + ٤٠ دقيقة تطبيق مباشر", "دخل", "0"},
                {"من ٢:٤٥ م إلى ٤:١٠ م", "المسجد: مراجعة ٤ صفحات قرآن + العصر + الدرس", "قرآن ومسجد", "1"},
                {"من ٤:٣٠ م إلى ٥:٣٠ م", lateAfternoon, "تطوير شخصي", "0"},
                {"من ٦:٠٠ م إلى ٨:٠٠ م", "المغرب + التحفيظ + العشاء + محاسبة الطلاب", "ثابت", "0"},
                {"من ٨:٠٠ م إلى ٨:٣٠ م", "العشاء مع الأسرة", "أسرة", "0"},
                {"من ٨:٣٠ م إلى ٩:٣٠ م", "وقت حر بلا شعور بالذنب", "راحة", "0"},
                {"من ٩:٣٠ م إلى ١٠:١٥ م", "مراجعة خفيفة لما أُنجز + تجهيز مهام الغد", "إغلاق اليوم", "0"},
                {"من ١٠:١٥ م إلى ١٠:٣٠ م", "إبعاد الهاتف والاستعداد للنوم", "نوم", "1"},
                {"١٠:٣٠ م", "النوم", "الهدف الليلي", "1"}
        };
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(dp(4), dp(4), dp(4), dp(4));
        nav.setElevation(dp(8));
        nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        addNavItem(nav, "اليوم", "today");
        addNavItem(nav, "الخطة", "plan");
        addNavItem(nav, "التقدم", "progress");
        addNavItem(nav, "لاحقًا", "later");
        addNavItem(nav, "المزيد", "more");
        return nav;
    }

    private void addNavItem(LinearLayout nav, String label, String key) {
        TextView item = text(label, 12, activeTab.equals(key) ? GREEN : MUTED, activeTab.equals(key));
        item.setGravity(Gravity.CENTER);
        item.setBackground(activeTab.equals(key) ? rounded(Color.rgb(235, 247, 243), 16) : null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(2), dp(4), dp(2), dp(4));
        nav.addView(item, lp);
        item.setOnClickListener(v -> showTab(key));
    }

    private void addTaskRow(LinearLayout parent, int index, String defaultText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0, dp(10), 0, 0);

        CheckBox check = new CheckBox(this);
        check.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));
        boolean checked = prefs.getBoolean("task_" + index + "_done_" + todayKey, false);
        check.setChecked(checked);
        taskChecks[index] = check;
        row.addView(check, new LinearLayout.LayoutParams(dp(44), dp(54)));

        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(2);
        input.setTextSize(15);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(rounded(Color.rgb(247, 249, 252), 14, Color.rgb(226, 231, 239)));
        input.setText(prefs.getString("task_" + index + "_text_" + todayKey, defaultText));
        input.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        taskInputs[index] = input;
        row.addView(input, new LinearLayout.LayoutParams(0, dp(58), 1f));

        check.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("task_" + index + "_done_" + todayKey, isChecked).apply();
            updatePriorityProgress();
        });
        parent.addView(row);
    }

    private void addTimelineRow(LinearLayout parent, String time, String label, String tag, boolean highlight) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(highlight ? Color.rgb(235, 247, 243) : Color.WHITE, 16,
                highlight ? Color.rgb(179, 221, 209) : Color.rgb(227, 232, 240)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, 0);
        parent.addView(card, lp);

        TextView timeView = text(time, 13, highlight ? GREEN : NAVY, true);
        timeView.setGravity(Gravity.RIGHT);
        timeView.setPadding(0, 0, 0, dp(5));
        card.addView(timeView);

        TextView labelView = text(label, 15, TEXT, true);
        card.addView(labelView);

        TextView tagView = text(tag, 11, highlight ? GREEN : MUTED, false);
        tagView.setPadding(0, dp(4), 0, 0);
        card.addView(tagView);
    }

    private void addMetricCard(LinearLayout root, String titleValue, String value, int color) {
        LinearLayout card = card();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, card, dp(14));
        TextView title = text(titleValue, 14, MUTED, true);
        card.addView(title);
        TextView metric = text(value, 22, color, true);
        metric.setPadding(0, dp(6), 0, 0);
        card.addView(metric);
    }

    private void addBullet(LinearLayout parent, String value) {
        TextView t = text("•  " + value, 14, TEXT, false);
        t.setPadding(0, dp(5), 0, dp(5));
        parent.addView(t);
    }

    private void addCard(LinearLayout root, View card, int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, topMargin, 0, 0);
        root.addView(card, lp);
    }

    private void saveTasks(boolean toast) {
        if (taskInputs[0] == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < 3; i++) {
            if (taskInputs[i] != null) editor.putString("task_" + i + "_text_" + todayKey, taskInputs[i].getText().toString().trim());
            if (taskChecks[i] != null) editor.putBoolean("task_" + i + "_done_" + todayKey, taskChecks[i].isChecked());
        }
        editor.apply();
        if (toast) Toast.makeText(this, "تم حفظ مهام اليوم", Toast.LENGTH_SHORT).show();
    }

    private int countDoneTasks() {
        int done = 0;
        for (int i = 0; i < 3; i++) if (prefs.getBoolean("task_" + i + "_done_" + todayKey, false)) done++;
        return done;
    }

    private int countDoneIndicators() {
        int done = 0;
        for (int i = 0; i < 5; i++) if (prefs.getBoolean("indicator_" + i + "_" + todayKey, false)) done++;
        return done;
    }

    private void updatePriorityProgress() {
        if (priorityProgress == null) return;
        int done = 0;
        for (CheckBox check : taskChecks) if (check != null && check.isChecked()) done++;
        priorityProgress.setText("أنجزت " + arabicNumber(done) + " من ٣");
    }

    private void updateIndicatorProgress() {
        if (indicatorProgress == null) return;
        int done = 0;
        for (CheckBox check : indicatorChecks) if (check != null && check.isChecked()) done++;
        indicatorProgress.setText("أنجزت " + arabicNumber(done) + " من ٥");
    }

    private String arabicDate() {
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar"));
        return toArabicDigits(fmt.format(Calendar.getInstance().getTime()));
    }

    private String arabicNumber(int n) {
        return toArabicDigits(String.valueOf(n));
    }

    private String toArabicDigits(String value) {
        return value.replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤')
                .replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');
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
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setBackground(rounded(Color.WHITE, 20, Color.rgb(228, 233, 241)));
        card.setElevation(dp(1));
        return card;
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

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor) {
        GradientDrawable d = rounded(color, radiusDp);
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveTasks(false);
    }
}
