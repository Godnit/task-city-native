package com.masari.personalplan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MasariV13Activity extends Activity {
    private static final int BG = Color.rgb(247, 249, 251);
    private static final int CARD = Color.WHITE;
    private static final int NAVY = Color.rgb(7, 38, 57);
    private static final int NAVY_2 = Color.rgb(13, 54, 77);
    private static final int GREEN = Color.rgb(36, 163, 94);
    private static final int GREEN_DARK = Color.rgb(22, 128, 72);
    private static final int TEXT = Color.rgb(25, 33, 42);
    private static final int MUTED = Color.rgb(112, 122, 133);
    private static final int BORDER = Color.rgb(229, 234, 239);
    private static final int TRACK = Color.rgb(236, 240, 243);
    private static final int BLUE = Color.rgb(74, 128, 204);
    private static final int ORANGE = Color.rgb(226, 147, 66);
    private static final int GOLD = Color.rgb(221, 167, 55);
    private static final int PURPLE = Color.rgb(137, 104, 186);
    private static final int RED = Color.rgb(205, 94, 86);
    private static final int TEAL = Color.rgb(72, 155, 159);
    private static final int PINK = Color.rgb(193, 104, 139);

    private SharedPreferences prefs;
    private String todayKey;
    private String tab = "home";
    private String taskFilter = "الكل";
    private String rewardTab = "level";
    private final LinkedHashMap<String, Integer> domainColors = new LinkedHashMap<>();

    static class Task {
        String id, title, domain, icon;
        int start, end, points;
        boolean required;
        Task(String id, int start, int end, String title, String domain, int points, boolean required, String icon) {
            this.id = id; this.start = start; this.end = end; this.title = title; this.domain = domain;
            this.points = points; this.required = required; this.icon = icon;
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("masari_data", MODE_PRIVATE);
        todayKey = dateKey(Calendar.getInstance());
        setupDomains();
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs != null) {
            todayKey = dateKey(Calendar.getInstance());
            render();
        }
    }

    private void setupDomains() {
        domainColors.put("القرآن", GREEN);
        domainColors.put("الإنجليزية والقبول", BLUE);
        domainColors.put("العمل والدخل", ORANGE);
        domainColors.put("الصحة", RED);
        domainColors.put("المعرفة والقراءة", PURPLE);
        domainColors.put("التواصل", TEAL);
        domainColors.put("الأسرة", PINK);
        domainColors.put("الدين والمسجد", Color.rgb(77, 137, 91));
        domainColors.put("الانضباط", Color.rgb(107, 117, 128));
    }

    private void render() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout body = new FrameLayout(this);
        shell.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        View page;
        switch (tab) {
            case "tasks": page = tasksPage(); break;
            case "stats": page = statsPage(); break;
            case "rewards": page = rewardsPage(); break;
            case "more": page = morePage(); break;
            default: page = homePage();
        }
        body.addView(page, new FrameLayout.LayoutParams(-1, -1));
        shell.addView(bottomNav(), new LinearLayout.LayoutParams(-1, dp(86)));
        setContentView(shell);
    }

    private View homePage() {
        ScrollView scroll = scroll();
        LinearLayout root = root(scroll);
        root.addView(topBar("مساري", "menu", "calendar"));

        LinearLayout dayBadge = new LinearLayout(this);
        dayBadge.setOrientation(LinearLayout.VERTICAL);
        dayBadge.setGravity(Gravity.CENTER);
        dayBadge.setPadding(0, dp(8), 0, dp(4));
        TextView circle = center(ar(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)), 18, GREEN_DARK, true);
        circle.setBackground(round(soft(GREEN), 30));
        dayBadge.addView(circle, new LinearLayout.LayoutParams(dp(60), dp(60)));
        TextView dayLabel = center("اليوم حتى الآن", 11, MUTED, false);
        dayLabel.setPadding(0, dp(7), 0, 0);
        dayBadge.addView(dayLabel);
        root.addView(dayBadge);

        LinearLayout progressCard = card();
        progressCard.setPadding(dp(14), dp(16), dp(14), dp(13));
        add(root, progressCard, 12);
        LinearLayout rings = new LinearLayout(this);
        rings.setOrientation(LinearLayout.HORIZONTAL);
        rings.setGravity(Gravity.CENTER);
        rings.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addRing(rings, "اليوم", dayRate(Calendar.getInstance()), GREEN);
        addRing(rings, "الأسبوع", weekRate(), BLUE);
        addRing(rings, "الشهر", monthRate(), PURPLE);
        addRing(rings, "السنة", yearRate(), GOLD);
        progressCard.addView(rings);
        TextView guidance = center(dayRate(Calendar.getInstance()) >= 80 ?
                "أنت فوق الهدف اليومي؛ حافظ على النسق بهدوء." :
                "ركّز على المهمة التالية فقط، ثم انتقل لما بعدها.", 11, MUTED, false);
        guidance.setPadding(dp(6), dp(11), dp(6), 0);
        progressCard.addView(guidance);

        LinearLayout goal = card();
        goal.setPadding(dp(16), dp(15), dp(16), dp(14));
        add(root, goal, 12);
        LinearLayout goalHead = new LinearLayout(this);
        goalHead.setOrientation(LinearLayout.HORIZONTAL);
        goalHead.setGravity(Gravity.CENTER_VERTICAL);
        goalHead.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        IconBubble target = new IconBubble(this, "target", GREEN, soft(GREEN));
        goalHead.addView(target, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout gt = new LinearLayout(this);
        gt.setOrientation(LinearLayout.VERTICAL);
        gt.setPadding(dp(11), 0, dp(6), 0);
        gt.addView(text("الهدف اليومي", 20, TEXT, true));
        int required = requiredCountToday();
        int requiredDone = requiredDoneToday();
        int requiredPct = required == 0 ? 0 : Math.min(100, Math.round(requiredDone * 100f / required));
        gt.addView(text(ar(requiredPct) + "٪ من المهام الأساسية", 12, MUTED, false));
        goalHead.addView(gt, new LinearLayout.LayoutParams(0, -2, 1));
        goal.addView(goalHead);
        TextView count = text("من " + ar(required) + " مهمة • أنجزت " + ar(requiredDone), 11, GREEN_DARK, true);
        count.setPadding(0, dp(10), 0, dp(6));
        goal.addView(count);
        goal.addView(progress(requiredDone, Math.max(1, required), GREEN, 5));

        root.addView(sectionHeader("جدول اليوم", "عرض الكل", v -> { tab = "tasks"; render(); }));
        List<Task> preview = homeTasks();
        for (Task task : preview) addTaskCard(root, task, true);
        gap(root, 20);
        return scroll;
    }

    private View tasksPage() {
        ScrollView scroll = scroll();
        LinearLayout root = root(scroll);
        root.addView(topBar("المهام", "menu", "check"));
        root.addView(sectionHeader("جدول اليوم", ar(countDoneToday()) + " / " + ar(tasksToday().size()), null));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] filters = {"الكل", "قرآن", "إنجليزية", "عمل", "صحة", "قراءة", "أسرة"};
        for (String f : filters) {
            TextView chip = filterChip(f, f.equals(taskFilter));
            chips.addView(chip);
            chip.setOnClickListener(v -> { taskFilter = f; render(); });
        }
        hsv.addView(chips);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(52));
        hp.setMargins(0, 0, 0, dp(6));
        root.addView(hsv, hp);

        for (Task task : tasksToday()) if (matches(task)) addTaskCard(root, task, false);
        Button add = primaryButton("إضافة أو تعديل مهمة مخصصة");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(54));
        bp.setMargins(0, dp(14), 0, dp(20));
        root.addView(add, bp);
        add.setOnClickListener(v -> startActivity(new Intent(this, PlannerCenterActivity.class)));
        return scroll;
    }

    private View statsPage() {
        ScrollView scroll = scroll();
        LinearLayout root = root(scroll);
        root.addView(topBar("الإحصائيات", "menu", "stats"));

        root.addView(sectionHeader("نظرة سريعة", "هذا الأسبوع", null));
        LinearLayout rateCard = card();
        rateCard.setPadding(dp(10), dp(14), dp(10), dp(12));
        add(root, rateCard, 8);
        LinearLayout rateRow = new LinearLayout(this);
        rateRow.setOrientation(LinearLayout.HORIZONTAL);
        rateRow.setGravity(Gravity.CENTER);
        rateRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addRing(rateRow, "اليوم", dayRate(Calendar.getInstance()), GREEN);
        addRing(rateRow, "الأسبوع", weekRate(), BLUE);
        addRing(rateRow, "الشهر", monthRate(), PURPLE);
        addRing(rateRow, "السنة", yearRate(), GOLD);
        rateCard.addView(rateRow);

        String strongest = strongestDomainThisWeek();
        String weakest = weakestDomainThisWeek();
        int strongDays = strongDaysThisMonth();
        LinearLayout row1 = metricRow();
        row1.addView(metricCard("check", ar(monthCompleted()), "مهام منجزة", GREEN), weight());
        row1.addView(metricCard("fire", ar(streak()), "سلسلة الإنجاز", ORANGE), weight());
        row1.addView(metricCard("star", ar(strongDays), "أيام +٨٠٪", GOLD), weight());
        root.addView(row1);

        LinearLayout row2 = metricRow();
        row2.addView(metricCard("focus", formatMinutes(focusMinutes()), "دقائق التركيز", BLUE), weight());
        row2.addView(metricCard("quran", ar(quranPagesThisMonth()), "صفحات القرآن", GREEN), weight());
        row2.addView(metricCard("language", ar(englishSessionsThisMonth()), "جلسات الإنجليزية", PURPLE), weight());
        root.addView(row2);

        LinearLayout row3 = metricRow();
        row3.addView(metricCard("briefcase", ar(workSessionsThisMonth()), "جلسات العمل", ORANGE), weight());
        row3.addView(metricCard("up", shortDomain(strongest), "أقوى مجال", GREEN), weight());
        row3.addView(metricCard("down", shortDomain(weakest), "أضعف مجال", RED), weight());
        root.addView(row3);

        root.addView(sectionHeader("أداء المجالات", "تقدم متوازن", null));
        LinearLayout domainCard = card();
        domainCard.setPadding(dp(14), dp(7), dp(14), dp(7));
        add(root, domainCard, 8);
        for (String domain : domainColors.keySet()) addDomainProgress(domainCard, domain);

        root.addView(sectionHeader("آخر ٧ أيام", "نسبة الإنجاز", null));
        LinearLayout bars = card();
        bars.setPadding(dp(10), dp(12), dp(10), dp(8));
        add(root, bars, 10);
        bars.addView(new WeekBars(this), new LinearLayout.LayoutParams(-1, dp(172)));
        gap(root, 20);
        return scroll;
    }

    private View rewardsPage() {
        ScrollView scroll = scroll();
        LinearLayout root = root(scroll);
        root.addView(topBar("المكافآت", "menu", "reward"));

        int total = totalPoints();
        int level = levelFor(total);
        int base = levelBase(level);
        int next = levelNext(level);
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(17));
        hero.setBackground(round(NAVY, 24));
        add(root, hero, 10);

        LinearLayout hr = new LinearLayout(this);
        hr.setOrientation(LinearLayout.HORIZONTAL);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        hr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout hText = new LinearLayout(this);
        hText.setOrientation(LinearLayout.VERTICAL);
        hText.addView(text("إجمالي نقاط التقدم", 12, Color.rgb(190, 205, 215), false));
        hText.addView(text(ar(total), 34, Color.WHITE, true));
        hText.addView(text("المستوى " + ar(level) + " • " + levelName(level), 15, Color.WHITE, true));
        hr.addView(hText, new LinearLayout.LayoutParams(0, -2, 1));
        IconBubble medal = new IconBubble(this, "reward", Color.WHITE, NAVY_2);
        hr.addView(medal, new LinearLayout.LayoutParams(dp(72), dp(72)));
        hero.addView(hr);
        hero.addView(progress(Math.max(0, total - base), Math.max(1, next - base), GREEN, 6));
        TextView nextText = text(level >= 6 ? "أعلى مستوى متاح حاليًا" : "بقي " + ar(Math.max(0, next - total)) + " نقطة للمستوى التالي", 11, Color.rgb(187, 219, 198), false);
        nextText.setPadding(0, dp(7), 0, 0);
        hero.addView(nextText);

        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[][] rewards = {{"level", "المستوى"}, {"badges", "الأوسمة"}, {"achievements", "الإنجازات"}, {"store", "المتجر"}};
        for (String[] item : rewards) {
            TextView chip = rewardChip(item[1], item[0].equals(rewardTab));
            tabs.addView(chip);
            chip.setOnClickListener(v -> { rewardTab = item[0]; render(); });
        }
        tabsScroll.addView(tabs);
        LinearLayout.LayoutParams tsp = new LinearLayout.LayoutParams(-1, dp(58));
        tsp.setMargins(0, dp(10), 0, dp(2));
        root.addView(tabsScroll, tsp);

        switch (rewardTab) {
            case "badges": rewardBadges(root); break;
            case "achievements": rewardAchievements(root); break;
            case "store": rewardStore(root); break;
            default: rewardLevel(root, total, level, next);
        }
        gap(root, 20);
        return scroll;
    }

    private void rewardLevel(LinearLayout root, int total, int level, int next) {
        LinearLayout current = card();
        current.setPadding(dp(16), dp(16), dp(16), dp(15));
        add(root, current, 10);
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        IconBubble icon = new IconBubble(this, "level", GREEN, soft(GREEN));
        r.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(12), 0, 0, 0);
        tx.addView(text("المستوى الحالي", 11, MUTED, false));
        tx.addView(text(levelName(level), 24, TEXT, true));
        tx.addView(text(level >= 6 ? "أنت في أعلى رتبة حالية" : "بقي " + ar(Math.max(0, next - total)) + " نقطة للترقية", 11, MUTED, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        current.addView(r);

        root.addView(sectionHeader("قوة المجالات", "الرتبة الحالية", null));
        LinearLayout domains = card();
        domains.setPadding(dp(14), dp(7), dp(14), dp(7));
        add(root, domains, 8);
        for (String d : domainColors.keySet()) addRankRow(domains, d);
    }

    private void rewardBadges(LinearLayout root) {
        root.addView(sectionHeader("الأوسمة", "حسب تقدم المجالات", null));
        LinearLayout box = card();
        box.setPadding(dp(14), dp(8), dp(14), dp(8));
        add(root, box, 8);
        for (String d : domainColors.keySet()) {
            int pts = domainPoints(d);
            String rank = rankFor(pts);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.setPadding(0, dp(10), 0, dp(10));
            IconBubble badge = new IconBubble(this, "badge", domainColors.get(d), soft(domainColors.get(d)));
            row.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPadding(dp(10), 0, 0, 0);
            tx.addView(text(shortDomain(d), 14, TEXT, true));
            tx.addView(text(rank + " • " + ar(pts) + " نقطة", 10, MUTED, false));
            row.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
            box.addView(row);
            if (!d.equals(lastDomain())) divider(box);
        }
    }

    private void rewardAchievements(LinearLayout root) {
        root.addView(sectionHeader("الإنجازات", "تتقدم تلقائيًا", null));
        addAchievement(root, "سبعة أيام قوية", "حافظ على +٨٠٪ لسبعة أيام", streak(), 7, "fire", GOLD);
        addAchievement(root, "قارئ مستمر", "أكمل ٣٠ جلسة LeapAhead", countDoneIds("leap"), 30, "book", PURPLE);
        addAchievement(root, "مراجعة راسخة", "أكمل ٢٠ جلسة قرآن", countDoneIds("quran"), 20, "quran", GREEN);
        addAchievement(root, "إنجليزية ثابتة", "أكمل ١٥ جلسة إنجليزية", countDoneIds("english"), 15, "language", BLUE);
        addAchievement(root, "عمل منتظم", "أكمل ١٠ جلسات عمل", countDoneIds("work"), 10, "briefcase", ORANGE);
    }

    private void rewardStore(LinearLayout root) {
        int credits = prefs.getInt("reward_credits", 0);
        LinearLayout wallet = card();
        wallet.setPadding(dp(16), dp(15), dp(16), dp(15));
        add(root, wallet, 10);
        wallet.addView(text("رصيد المكافآت", 12, MUTED, false));
        wallet.addView(text(ar(credits) + " رصيد", 27, TEXT, true));
        wallet.addView(text("استخدمه كمكافآت اختيارية دون أن ينقص نقاط تقدمك.", 11, MUTED, false));
        root.addView(sectionHeader("مكافآت مقترحة", "خفيفة وغير مشتتة", null));
        addStoreItem(root, "استراحة ممتعة", "٣٠ دقيقة شيء تحبه", 15, "coffee");
        addStoreItem(root, "تحميل لعبة أو محتوى", "عند توفر الإنترنت والرصيد", 35, "gift");
        addStoreItem(root, "وقت حر أطول", "ساعة بلا مهام بعد يوم قوي", 45, "star");
    }

    private View morePage() {
        ScrollView scroll = scroll();
        LinearLayout root = root(scroll);
        root.addView(topBar("المزيد", "menu", "more"));
        root.addView(sectionHeader("إدارة مساري", "أدواتك الأساسية", null));
        addMenuItem(root, "planner", "تخصيص الجدول", "إضافة أو تعديل المهام والأوقات", GREEN, () -> startActivity(new Intent(this, PlannerCenterActivity.class)));
        addMenuItem(root, "week", "المراجعة الأسبوعية", "راجع تقدمك وعوائقك وخطة الأسبوع", BLUE, () -> startActivity(new Intent(this, WeeklyPlannerActivity.class)));
        addMenuItem(root, "reward", "نظام المكافآت", "النقاط والمستويات والأوسمة", GOLD, () -> { tab = "rewards"; render(); });

        root.addView(sectionHeader("مبادئ مساري", "حتى يبقى التطبيق مريحًا", null));
        LinearLayout rules = card();
        rules.setPadding(dp(15), dp(10), dp(15), dp(10));
        add(root, rules, 8);
        rules.addView(rule("المهمات الأساسية أولًا، والاختيارية بعد توفر الطاقة."));
        rules.addView(rule("لا نضغط الكتب على حساب النوم أو القرآن أو الإنجليزية."));
        rules.addView(rule("النجاح اليومي هو الاستمرار، لا ملء الشاشة بالمهمات."));
        gap(root, 20);
        return scroll;
    }

    private View bottomNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        bar.setPadding(dp(6), dp(7), dp(6), dp(7));
        bar.setBackgroundColor(NAVY);
        addNavItem(bar, "home", "الرئيسية", "home");
        addNavItem(bar, "tasks", "المهام", "tasks");
        addNavItem(bar, "stats", "الإحصائيات", "stats");
        addNavItem(bar, "rewards", "المكافآت", "reward");
        addNavItem(bar, "more", "المزيد", "more");
        return bar;
    }

    private void addNavItem(LinearLayout bar, String key, String label, String icon) {
        boolean active = key.equals(tab);
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), active ? dp(5) : dp(7), dp(4), dp(5));
        if (active) item.setBackground(round(GREEN, 22));
        MiniIcon iv = new MiniIcon(this, icon, Color.WHITE);
        item.addView(iv, new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView txt = center(label, 9, active ? Color.WHITE : Color.rgb(202, 215, 223), active);
        txt.setPadding(0, dp(2), 0, 0);
        item.addView(txt);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, active ? dp(70) : dp(64), 1);
        p.setMargins(dp(3), active ? 0 : dp(4), dp(3), active ? dp(8) : 0);
        bar.addView(item, p);
        item.setOnClickListener(v -> { tab = key; render(); });
    }

    private View topBar(String title, String leftIcon, String rightIcon) {
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        frame.setPadding(0, dp(3), 0, dp(8));
        TextView titleView = center(title, 22, title.equals("مساري") ? GREEN_DARK : TEXT, true);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, dp(50), Gravity.CENTER);
        frame.addView(titleView, tp);
        IconBubble left = new IconBubble(this, leftIcon, TEXT, Color.TRANSPARENT);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        frame.addView(left, lp);
        left.setOnClickListener(v -> { tab = "more"; render(); });
        IconBubble right = new IconBubble(this, rightIcon, GREEN_DARK, soft(GREEN));
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        frame.addView(right, rp);
        return frame;
    }

    private View sectionHeader(String title, String action, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0, dp(16), 0, dp(8));
        row.addView(text(title, 19, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        if (action != null && !action.isEmpty()) {
            TextView a = text(action, 11, GREEN_DARK, true);
            a.setPadding(dp(10), dp(5), dp(10), dp(5));
            a.setBackground(round(soft(GREEN), 14));
            if (listener != null) a.setOnClickListener(listener);
            row.addView(a);
        }
        return row;
    }

    private void addTaskCard(LinearLayout root, Task task, boolean compact) {
        LinearLayout card = card();
        card.setPadding(dp(13), compact ? dp(11) : dp(13), dp(13), compact ? dp(11) : dp(13));
        add(root, card, 8);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int c = domainColors.containsKey(task.domain) ? domainColors.get(task.domain) : GREEN;
        IconBubble icon = new IconBubble(this, task.icon, c, soft(c));
        row.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(10), 0, dp(8), 0);
        TextView title = text(task.title, compact ? 14 : 15, done(task) ? MUTED : TEXT, true);
        if (done(task)) title.setAlpha(0.65f);
        tx.addView(title);
        tx.addView(text(time(task.start) + " - " + time(task.end) + "  •  " + shortDomain(task.domain), 10, MUTED, false));
        row.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        CheckView check = new CheckView(this, done(task));
        row.addView(check, new LinearLayout.LayoutParams(dp(32), dp(32)));
        card.addView(row);
        card.setOnClickListener(v -> { setDone(task, !done(task)); render(); });
    }

    private void addRing(LinearLayout row, String label, int value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.addView(new RingView(this, value, color), new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView t = center(label, 10, MUTED, false);
        t.setPadding(0, dp(4), 0, 0);
        box.addView(t);
        row.addView(box, new LinearLayout.LayoutParams(0, dp(88), 1));
    }

    private LinearLayout metricRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0, dp(8), 0, 0);
        return row;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(118), 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private View metricCard(String icon, String value, String label, int c) {
        LinearLayout box = card();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(5), dp(9), dp(5), dp(9));
        box.addView(new MiniIcon(this, icon, c), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView v = center(value, value.length() > 10 ? 11 : 16, TEXT, true);
        v.setPadding(dp(2), dp(4), dp(2), 0);
        box.addView(v);
        box.addView(center(label, 9, MUTED, false));
        return box;
    }

    private void addDomainProgress(LinearLayout parent, String domain) {
        int c = domainColors.get(domain);
        int pct = weekDomainRate(domain);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(9), 0, dp(9));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(shortDomain(domain), 13, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(text(ar(pct) + "٪", 11, c, true));
        box.addView(head);
        box.addView(progress(pct, 100, c, 5));
        parent.addView(box);
    }

    private void addRankRow(LinearLayout parent, String domain) {
        int pts = domainPoints(domain);
        int c = domainColors.get(domain);
        int next = rankNext(pts);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(10));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        head.addView(text(shortDomain(domain), 13, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(text(rankFor(pts), 11, c, true));
        box.addView(head);
        int base = rankBase(pts);
        box.addView(progress(Math.max(0, pts - base), Math.max(1, next - base), c, 5));
        parent.addView(box);
    }

    private void addAchievement(LinearLayout root, String title, String sub, int current, int target, String icon, int c) {
        LinearLayout box = card();
        box.setPadding(dp(14), dp(13), dp(14), dp(13));
        add(root, box, 8);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.addView(new IconBubble(this, icon, c, soft(c)), new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(10), 0, dp(8), 0);
        tx.addView(text(title, 14, TEXT, true));
        tx.addView(text(sub, 10, MUTED, false));
        tx.addView(progress(Math.min(current, target), target, c, 4));
        row.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text(ar(Math.min(current, target)) + "/" + ar(target), 10, c, true));
        box.addView(row);
    }

    private void addStoreItem(LinearLayout root, String title, String sub, int cost, String icon) {
        LinearLayout box = card();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        add(root, box, 8);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.addView(new IconBubble(this, icon, GOLD, soft(GOLD)), new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(10), 0, dp(8), 0);
        tx.addView(text(title, 14, TEXT, true));
        tx.addView(text(sub, 10, MUTED, false));
        row.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        TextView price = text(ar(cost) + " رصيد", 10, GREEN_DARK, true);
        price.setPadding(dp(10), dp(6), dp(10), dp(6));
        price.setBackground(round(soft(GREEN), 16));
        row.addView(price);
        box.addView(row);
    }

    private void addMenuItem(LinearLayout root, String icon, String title, String sub, int c, Runnable action) {
        LinearLayout box = card();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        add(root, box, 8);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.addView(new IconBubble(this, icon, c, soft(c)), new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(10), 0, 0, 0);
        tx.addView(text(title, 15, TEXT, true));
        tx.addView(text(sub, 10, MUTED, false));
        row.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text("‹", 24, MUTED, false));
        box.addView(row);
        box.setOnClickListener(v -> action.run());
    }

    private TextView filterChip(String label, boolean selected) {
        TextView t = center(label, 11, selected ? Color.WHITE : TEXT, selected);
        t.setPadding(dp(15), dp(9), dp(15), dp(9));
        t.setBackground(round(selected ? GREEN : Color.WHITE, 18, selected ? GREEN : BORDER));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(38));
        p.setMargins(dp(4), dp(5), dp(4), dp(5));
        t.setLayoutParams(p);
        return t;
    }

    private TextView rewardChip(String label, boolean selected) {
        TextView t = center(label, 11, selected ? Color.WHITE : TEXT, selected);
        t.setPadding(dp(17), dp(9), dp(17), dp(9));
        t.setBackground(round(selected ? GREEN : Color.WHITE, 20, selected ? GREEN : BORDER));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(40));
        p.setMargins(dp(4), dp(8), dp(4), dp(8));
        t.setLayoutParams(p);
        return t;
    }

    private List<Task> tasksToday() { return tasksFor(Calendar.getInstance()); }

    private List<Task> tasksFor(Calendar d) {
        List<Task> a = new ArrayList<>();
        int day = d.get(Calendar.DAY_OF_WEEK);
        a.add(t("fajr", 240, 390, "الفجر والحلقة والدرس", "الدين والمسجد", 5, true, "mosque"));
        if (day == Calendar.SATURDAY) a.add(t("workoutA", 390, 420, "تمرين A — كتف وذراعان + جسم كامل", "الصحة", 20, true, "health"));
        if (day == Calendar.TUESDAY) a.add(t("workoutB", 390, 420, "تمرين B — أوتار وقبضة وسرعة", "الصحة", 20, true, "health"));
        a.add(t("english", 420, 480, englishTitle(day), "الإنجليزية والقبول", day == Calendar.FRIDAY ? 10 : 25, day != Calendar.FRIDAY, "language"));
        a.add(t("sleep2", 480, 570, "النوم التكميلي", "الصحة", 5, true, "sleep"));
        a.add(t("leap1", 570, 600, "LeapAhead — الكتاب ١", "المعرفة والقراءة", 7, true, "book"));
        a.add(t("work", 600, 690, workTitle(day), "العمل والدخل", day == Calendar.FRIDAY ? 10 : 25, day != Calendar.FRIDAY, "briefcase"));
        a.add(t("quran1", 705, 780, "القرآن — مراجعة جديدة: صفحتان", "القرآن", 14, true, "quran"));
        a.add(t("lunch", 780, 810, "الغداء", "الانضباط", 3, true, "meal"));
        a.add(t("leap2", 810, 855, "LeapAhead — الكتاب ٢", "المعرفة والقراءة", 8, true, "book"));
        a.add(t("quran2", 885, 970, "القرآن — صفحتان + مراجعة قديمة", "القرآن", 18, true, "quran"));
        a.add(afternoon(day));
        a.add(t("maghrib", 1080, 1200, "المغرب والتحفيظ والعشاء", "الدين والمسجد", 5, true, "mosque"));
        a.add(t("dinner", 1200, 1230, "العشاء مع الأسرة", "الأسرة", 3, true, "home"));
        a.add(t("leap3", 1230, 1275, "LeapAhead — الكتاب ٣", "المعرفة والقراءة", 7, false, "book"));
        a.add(t("close", 1320, 1340, "إغلاق اليوم وتجهيز الغد", "الانضباط", 5, true, "check"));
        a.add(t("sleep", 1340, 1350, "الاستعداد للنوم", "الصحة", 5, true, "sleep"));
        addCustom(a, d);
        return a;
    }

    private Task afternoon(int day) {
        switch (day) {
            case Calendar.SATURDAY: return t("family_talk", 990, 1020, "الأسرة + تدريب كلام قصير", "الأسرة", 8, true, "home");
            case Calendar.SUNDAY: return t("talk1", 990, 1020, "تدريب التواصل", "التواصل", 10, true, "talk");
            case Calendar.MONDAY: return t("family_friend", 990, 1020, "خدمة الأسرة + تفقد صديق", "الأسرة", 8, true, "home");
            case Calendar.TUESDAY: return t("talk2", 990, 1020, "تدريب الكلام والحزم", "التواصل", 10, true, "talk");
            case Calendar.WEDNESDAY: return t("medicine", 990, 1050, "مراجعة طب قديم", "المعرفة والقراءة", 12, true, "book");
            case Calendar.THURSDAY: return t("khatera", 990, 1030, "خاطرة دينية + تدريب إلقاء", "التواصل", 12, true, "talk");
            default: return t("explore", 990, 1050, "استكشاف علمي أو مهارة حياة", "المعرفة والقراءة", 10, false, "star");
        }
    }

    private void addCustom(List<Task> list, Calendar d) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("custom_tasks", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null || !o.optBoolean("active", true)) continue;
                int dy = o.optInt("day", 0);
                if (dy != 0 && dy != d.get(Calendar.DAY_OF_WEEK)) continue;
                list.add(t(o.optString("id", "custom_" + i), o.optInt("start", 960), o.optInt("end", 990),
                        o.optString("title", "مهمة مخصصة"), o.optString("domain", "الانضباط"),
                        o.optInt("points", 10), o.optBoolean("required", false), "check"));
            }
        } catch (Exception ignored) { }
    }

    private Task t(String id, int start, int end, String title, String domain, int points, boolean required, String icon) {
        return new Task(id, start, end, title, domain, points, required, icon);
    }

    private String englishTitle(int d) {
        switch (d) {
            case Calendar.SATURDAY: return "الإنجليزية: Vocabulary + Reading Explorer";
            case Calendar.SUNDAY: return "الإنجليزية: Vocabulary + Tactics";
            case Calendar.MONDAY: return "الإنجليزية: Vocabulary + Reading Explorer";
            case Calendar.TUESDAY: return "الإنجليزية: Vocabulary + Tactics";
            case Calendar.WEDNESDAY: return "الإنجليزية: Vocabulary + Oxford Bookworms";
            case Calendar.THURSDAY: return "اختبار الإنجليزية الأسبوعي";
            default: return "استماع إنجليزي ممتع — يوم خفيف";
        }
    }

    private String workTitle(int d) {
        switch (d) {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY: return "العمل: تطوير الأكاديمية";
            case Calendar.MONDAY:
            case Calendar.TUESDAY: return "العمل: الوصول للسوق";
            case Calendar.WEDNESDAY: return "العمل: دخل مباشر";
            case Calendar.THURSDAY: return "العمل: مراجعة الأرقام";
            default: return "مراجعة مالية خفيفة";
        }
    }

    private List<Task> homeTasks() {
        List<Task> all = tasksToday();
        List<Task> out = new ArrayList<>();
        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE);
        for (Task t : all) {
            if (t.id.equals("sleep2") || t.id.equals("lunch") || t.id.equals("dinner")) continue;
            if (!done(t) && t.end >= now - 30) out.add(t);
            if (out.size() >= 5) break;
        }
        if (out.size() < 4) {
            for (Task t : all) {
                if (out.contains(t) || t.id.equals("sleep2") || t.id.equals("lunch") || t.id.equals("dinner")) continue;
                out.add(t);
                if (out.size() >= 5) break;
            }
        }
        return out;
    }

    private boolean matches(Task t) {
        if ("الكل".equals(taskFilter)) return true;
        if ("قرآن".equals(taskFilter)) return t.domain.equals("القرآن");
        if ("إنجليزية".equals(taskFilter)) return t.domain.equals("الإنجليزية والقبول");
        if ("عمل".equals(taskFilter)) return t.domain.equals("العمل والدخل");
        if ("صحة".equals(taskFilter)) return t.domain.equals("الصحة");
        if ("قراءة".equals(taskFilter)) return t.domain.equals("المعرفة والقراءة");
        if ("أسرة".equals(taskFilter)) return t.domain.equals("الأسرة");
        return true;
    }

    private boolean done(Task task) {
        return prefs.getBoolean("reward_done_" + todayKey + "_" + task.id, false);
    }

    private boolean doneOn(Task task, Calendar d) {
        return prefs.getBoolean("reward_done_" + dateKey(d) + "_" + task.id, false);
    }

    private void setDone(Task task, boolean value) {
        boolean old = done(task);
        if (old == value) return;
        int delta = value ? task.points : -task.points;
        String dayKey = "reward_day_points_" + todayKey;
        String domainKey = "reward_domain_" + task.domain;
        prefs.edit()
                .putBoolean("reward_done_" + todayKey + "_" + task.id, value)
                .putInt(dayKey, Math.max(0, prefs.getInt(dayKey, 0) + delta))
                .putInt(domainKey, Math.max(0, prefs.getInt(domainKey, 0) + delta))
                .apply();
        Toast.makeText(this, value ? "+" + task.points + " نقطة" : "تم التراجع", Toast.LENGTH_SHORT).show();
    }

    private int countDoneToday() { int n = 0; for (Task t : tasksToday()) if (done(t)) n++; return n; }
    private int requiredCountToday() { int n = 0; for (Task t : tasksToday()) if (t.required) n++; return n; }
    private int requiredDoneToday() { int n = 0; for (Task t : tasksToday()) if (t.required && done(t)) n++; return n; }
    private int dayPoints(Calendar c) { return prefs.getInt("reward_day_points_" + dateKey(c), 0); }
    private int dayTarget(Calendar c) { int n = 0; for (Task t : tasksFor(c)) if (t.required) n += t.points; return Math.max(1, n); }
    private int dayRate(Calendar c) { return pct(dayPoints(c), dayTarget(c)); }

    private int weekRate() {
        Calendar c = saturdayStart(Calendar.getInstance());
        Calendar now = Calendar.getInstance();
        int p = 0, t = 0;
        while (!c.after(now)) { p += dayPoints(c); t += dayTarget(c); c.add(Calendar.DAY_OF_MONTH, 1); }
        return pct(p, t);
    }

    private int monthRate() {
        Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1);
        Calendar now = Calendar.getInstance(); int p = 0, t = 0;
        while (!c.after(now)) { p += dayPoints(c); t += dayTarget(c); c.add(Calendar.DAY_OF_MONTH, 1); }
        return pct(p, t);
    }

    private int yearRate() {
        Calendar start = Calendar.getInstance(); start.set(2026, Calendar.SEPTEMBER, 1, 0, 0, 0);
        Calendar now = Calendar.getInstance(); if (now.before(start)) return 0;
        int p = 0, t = 0;
        while (!start.after(now)) { p += dayPoints(start); t += dayTarget(start); start.add(Calendar.DAY_OF_MONTH, 1); }
        return pct(p, t);
    }

    private int monthCompleted() {
        Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1);
        Calendar now = Calendar.getInstance(); int n = 0;
        while (!c.after(now)) {
            String key = dateKey(c);
            for (Task t : tasksFor(c)) if (prefs.getBoolean("reward_done_" + key + "_" + t.id, false)) n++;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return n;
    }

    private int strongDaysThisMonth() {
        Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1);
        Calendar now = Calendar.getInstance(); int n = 0;
        while (!c.after(now)) { if (dayRate(c) >= 80) n++; c.add(Calendar.DAY_OF_MONTH, 1); }
        return n;
    }

    private int streak() {
        Calendar c = Calendar.getInstance();
        if (dayRate(c) < 80) c.add(Calendar.DAY_OF_MONTH, -1);
        int n = 0;
        for (int i = 0; i < 365; i++) {
            if (dayRate(c) >= 80) { n++; c.add(Calendar.DAY_OF_MONTH, -1); } else break;
        }
        return n;
    }

    private int focusMinutes() {
        Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1);
        Calendar now = Calendar.getInstance(); int sum = 0;
        while (!c.after(now)) {
            String key = dateKey(c);
            for (Task t : tasksFor(c)) {
                if (prefs.getBoolean("reward_done_" + key + "_" + t.id, false) &&
                        (t.domain.equals("الإنجليزية والقبول") || t.domain.equals("العمل والدخل") ||
                         t.domain.equals("القرآن") || t.domain.equals("المعرفة والقراءة"))) {
                    sum += Math.max(0, t.end - t.start);
                }
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return sum;
    }

    private int quranPagesThisMonth() {
        Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1);
        Calendar now = Calendar.getInstance(); int pages = 0;
        while (!c.after(now)) {
            for (Task t : tasksFor(c)) if (t.id.startsWith("quran") && doneOn(t, c)) pages += 2;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return pages;
    }

    private int englishSessionsThisMonth() { return monthDoneId("english"); }
    private int workSessionsThisMonth() { return monthDoneId("work"); }

    private int monthDoneId(String idPart) {
        Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1);
        Calendar now = Calendar.getInstance(); int n = 0;
        while (!c.after(now)) {
            for (Task t : tasksFor(c)) if (t.id.contains(idPart) && doneOn(t, c)) n++;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return n;
    }

    private int countDoneIds(String contains) {
        int n = 0;
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (e.getKey().startsWith("reward_done_") && e.getKey().contains(contains) && Boolean.TRUE.equals(e.getValue())) n++;
        }
        return n;
    }

    private int weekDomainRate(String domain) {
        Calendar c = saturdayStart(Calendar.getInstance());
        Calendar now = Calendar.getInstance(); int earned = 0, target = 0;
        while (!c.after(now)) {
            for (Task t : tasksFor(c)) {
                if (!t.domain.equals(domain)) continue;
                if (t.required) target += t.points;
                if (doneOn(t, c)) earned += t.points;
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return pct(earned, Math.max(1, target));
    }

    private String strongestDomainThisWeek() {
        String best = "القرآن"; int score = -1;
        for (String d : domainColors.keySet()) { int v = weekDomainRate(d); if (v > score) { score = v; best = d; } }
        return best;
    }

    private String weakestDomainThisWeek() {
        String worst = "الانضباط"; int score = 101;
        for (String d : domainColors.keySet()) { int v = weekDomainRate(d); if (v < score) { score = v; worst = d; } }
        return worst;
    }

    private int domainPoints(String d) { return prefs.getInt("reward_domain_" + d, 0); }
    private int totalPoints() { int n = 0; for (String d : domainColors.keySet()) n += domainPoints(d); return n; }

    private int levelFor(int p) { if (p >= 8000) return 6; if (p >= 5000) return 5; if (p >= 3000) return 4; if (p >= 1500) return 3; if (p >= 600) return 2; return 1; }
    private int levelBase(int l) { switch (l) { case 2: return 600; case 3: return 1500; case 4: return 3000; case 5: return 5000; case 6: return 8000; default: return 0; } }
    private int levelNext(int l) { switch (l) { case 1: return 600; case 2: return 1500; case 3: return 3000; case 4: return 5000; case 5: return 8000; default: return 10000; } }
    private String levelName(int l) { switch (l) { case 2: return "منتظم"; case 3: return "متقدم"; case 4: return "راسخ"; case 5: return "متقن"; case 6: return "متمكن"; default: return "بداية ثابتة"; } }
    private String rankFor(int p) { if (p >= 1200) return "متمكن"; if (p >= 650) return "ذهبي"; if (p >= 300) return "فضي"; return "برونزي"; }
    private int rankBase(int p) { if (p >= 1200) return 1200; if (p >= 650) return 650; if (p >= 300) return 300; return 0; }
    private int rankNext(int p) { if (p >= 1200) return 1800; if (p >= 650) return 1200; if (p >= 300) return 650; return 300; }

    private String lastDomain() {
        String x = "";
        for (String d : domainColors.keySet()) x = d;
        return x;
    }

    private LinearLayout root(ScrollView s) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(6), dp(16), dp(10));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        s.addView(root, new ScrollView.LayoutParams(-1, -2));
        return root;
    }

    private ScrollView scroll() {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.setBackgroundColor(BG);
        return s;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(CARD, 20, BORDER));
        c.setElevation(dp(1));
        return c;
    }

    private void add(LinearLayout root, View v, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(top), 0, 0);
        root.addView(v, p);
    }

    private void gap(LinearLayout root, int dp) {
        View v = new View(this);
        root.addView(v, new LinearLayout.LayoutParams(1, dp(dp)));
    }

    private void divider(LinearLayout parent) {
        View v = new View(this); v.setBackgroundColor(BORDER);
        parent.addView(v, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    private TextView center(String value, int size, int color, boolean bold) {
        TextView t = text(value, size, color, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private Button primaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(round(GREEN, 24));
        b.setElevation(dp(1));
        return b;
    }

    private TextView rule(String value) {
        TextView t = text("•  " + value, 11, TEXT, false);
        t.setPadding(0, dp(7), 0, dp(7));
        return t;
    }

    private View progress(int value, int max, int color, int height) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(round(TRACK, height));
        int pct = max <= 0 ? 0 : Math.max(0, Math.min(100, Math.round(value * 100f / max)));
        View fill = new View(this);
        fill.setBackground(round(color, height));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(0, dp(height));
        frame.addView(fill, fp);
        frame.post(() -> {
            ViewGroup.LayoutParams p = fill.getLayoutParams();
            p.width = Math.max(0, Math.round(frame.getWidth() * pct / 100f));
            p.height = dp(height);
            fill.setLayoutParams(p);
        });
        frame.setMinimumHeight(dp(height));
        return frame;
    }

    private GradientDrawable round(int color, int radius) { return round(color, radius, color); }
    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        if (stroke != color) g.setStroke(dp(1), stroke);
        return g;
    }

    private int soft(int color) {
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        return Color.rgb((r + 255 * 6) / 7, (g + 255 * 6) / 7, (b + 255 * 6) / 7);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int pct(int a, int b) { return b <= 0 ? 0 : Math.max(0, Math.min(100, Math.round(a * 100f / b))); }

    private String dateKey(Calendar c) { return new SimpleDateFormat("yyyyMMdd", Locale.US).format(c.getTime()); }

    private Calendar saturdayStart(Calendar now) {
        Calendar c = (Calendar) now.clone();
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) c.add(Calendar.DAY_OF_MONTH, -1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private String time(int minute) {
        int h = (minute / 60) % 24, m = minute % 60;
        String suffix = h >= 12 ? "م" : "ص";
        int hh = h % 12; if (hh == 0) hh = 12;
        return ar(hh) + ":" + (m < 10 ? "٠" : "") + ar(m) + " " + suffix;
    }

    private String formatMinutes(int min) {
        if (min < 60) return ar(min) + " د";
        int h = min / 60, m = min % 60;
        return ar(h) + "س" + (m > 0 ? " " + ar(m) + "د" : "");
    }

    private String shortDomain(String d) {
        if (d == null) return "—";
        if (d.equals("الإنجليزية والقبول")) return "الإنجليزية";
        if (d.equals("العمل والدخل")) return "العمل";
        if (d.equals("المعرفة والقراءة")) return "القراءة";
        if (d.equals("الدين والمسجد")) return "المسجد";
        return d;
    }

    private String ar(int n) {
        String s = String.valueOf(n);
        char[] en = "0123456789".toCharArray();
        char[] ar = "٠١٢٣٤٥٦٧٨٩".toCharArray();
        for (int i = 0; i < 10; i++) s = s.replace(en[i], ar[i]);
        return s;
    }

    private static class RingView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int value, color;
        RingView(Activity c, int value, int color) { super(c); this.value = value; this.color = color; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight(), stroke = Math.max(4f, w * .075f);
            RectF r = new RectF(stroke, stroke, w - stroke, h - stroke);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(stroke); paint.setStrokeCap(Paint.Cap.ROUND); paint.setColor(TRACK);
            canvas.drawArc(r, -90, 360, false, paint);
            paint.setColor(color); canvas.drawArc(r, -90, 360 * value / 100f, false, paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(TEXT); paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); paint.setTextSize(w * .23f);
            Paint.FontMetrics fm = paint.getFontMetrics(); float y = h / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(toArabic(value) + "٪", w / 2f, y, paint);
        }
        private static String toArabic(int n) {
            String s = String.valueOf(n); String e = "0123456789", a = "٠١٢٣٤٥٦٧٨٩";
            for (int i = 0; i < 10; i++) s = s.replace(e.charAt(i), a.charAt(i));
            return s;
        }
    }

    private static class CheckView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); private final boolean checked;
        CheckView(Activity c, boolean checked) { super(c); this.checked = checked; }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float s = Math.min(getWidth(), getHeight()); float m = s * .18f;
            p.setStrokeWidth(Math.max(2f, s * .07f)); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(checked ? GREEN : Color.rgb(180, 188, 197));
            c.drawRoundRect(new RectF(m, m, s - m, s - m), s * .16f, s * .16f, p);
            if (checked) {
                p.setStyle(Paint.Style.FILL); p.setColor(GREEN); c.drawRoundRect(new RectF(m, m, s - m, s - m), s * .16f, s * .16f, p);
                p.setStyle(Paint.Style.STROKE); p.setColor(Color.WHITE); p.setStrokeWidth(Math.max(2f, s * .075f));
                Path path = new Path(); path.moveTo(s * .31f, s * .52f); path.lineTo(s * .44f, s * .65f); path.lineTo(s * .70f, s * .36f); c.drawPath(path, p);
            }
        }
    }

    private static class IconBubble extends View {
        private final String type; private final int fg, bg; private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        IconBubble(Activity c, String type, int fg, int bg) { super(c); this.type = type; this.fg = fg; this.bg = bg; }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float w = getWidth(), h = getHeight(), s = Math.min(w, h);
            if (Color.alpha(bg) != 0) { p.setColor(bg); p.setStyle(Paint.Style.FILL); c.drawCircle(w/2f, h/2f, s*.48f, p); }
            drawIcon(c, p, type, fg, w/2f, h/2f, s*.52f);
        }
    }

    private static class MiniIcon extends View {
        private final String type; private final int color; private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        MiniIcon(Activity c, String type, int color) { super(c); this.type = type; this.color = color; }
        @Override protected void onDraw(Canvas c) { super.onDraw(c); drawIcon(c, p, type, color, getWidth()/2f, getHeight()/2f, Math.min(getWidth(), getHeight())*.8f); }
    }

    private static void drawIcon(Canvas c, Paint p, String type, int color, float cx, float cy, float size) {
        p.setColor(color); p.setStrokeWidth(Math.max(2f, size*.10f)); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        float r = size * .32f; Path path = new Path();
        if ("home".equals(type)) {
            path.moveTo(cx-r, cy); path.lineTo(cx, cy-r*.9f); path.lineTo(cx+r, cy); path.lineTo(cx+r*.72f, cy+r); path.lineTo(cx-r*.72f, cy+r); path.close(); c.drawPath(path,p);
        } else if ("tasks".equals(type) || "check".equals(type)) {
            c.drawRoundRect(new RectF(cx-r, cy-r, cx+r, cy+r), r*.28f, r*.28f, p); path.moveTo(cx-r*.55f,cy); path.lineTo(cx-r*.1f,cy+r*.4f); path.lineTo(cx+r*.58f,cy-r*.45f); c.drawPath(path,p);
        } else if ("stats".equals(type)) {
            c.drawLine(cx-r*.8f,cy+r*.8f,cx-r*.8f,cy+r*.1f,p); c.drawLine(cx,cy+r*.8f,cx,cy-r*.2f,p); c.drawLine(cx+r*.8f,cy+r*.8f,cx+r*.8f,cy-r*.8f,p);
        } else if ("reward".equals(type) || "badge".equals(type) || "level".equals(type)) {
            c.drawCircle(cx, cy-r*.18f, r*.72f, p); path.moveTo(cx-r*.38f,cy+r*.45f); path.lineTo(cx-r*.55f,cy+r); path.lineTo(cx,cy+r*.72f); path.lineTo(cx+r*.55f,cy+r); path.lineTo(cx+r*.38f,cy+r*.45f); c.drawPath(path,p);
        } else if ("more".equals(type) || "menu".equals(type)) {
            if ("menu".equals(type)) { c.drawLine(cx-r,cy-r*.55f,cx+r,cy-r*.55f,p); c.drawLine(cx-r,cy,cx+r,cy,p); c.drawLine(cx-r,cy+r*.55f,cx+r,cy+r*.55f,p); }
            else { p.setStyle(Paint.Style.FILL); c.drawCircle(cx-r*.7f,cy,r*.12f,p); c.drawCircle(cx,cy,r*.12f,p); c.drawCircle(cx+r*.7f,cy,r*.12f,p); }
        } else if ("calendar".equals(type)) {
            c.drawRoundRect(new RectF(cx-r,cy-r*.72f,cx+r,cy+r*.9f),r*.22f,r*.22f,p); c.drawLine(cx-r,cy-r*.25f,cx+r,cy-r*.25f,p); c.drawLine(cx-r*.45f,cy-r,cx-r*.45f,cy-r*.48f,p); c.drawLine(cx+r*.45f,cy-r,cx+r*.45f,cy-r*.48f,p);
        } else if ("target".equals(type)) {
            c.drawCircle(cx,cy,r,p); c.drawCircle(cx,cy,r*.52f,p); p.setStyle(Paint.Style.FILL); c.drawCircle(cx,cy,r*.15f,p);
        } else if ("quran".equals(type) || "book".equals(type)) {
            path.moveTo(cx,cy-r*.7f); path.quadTo(cx-r*.85f,cy-r,cx-r*.85f,cy+r*.65f); path.quadTo(cx-r*.35f,cy+r*.35f,cx,cy+r*.7f); path.quadTo(cx+r*.35f,cy+r*.35f,cx+r*.85f,cy+r*.65f); path.quadTo(cx+r*.85f,cy-r,cx,cy-r*.7f); c.drawPath(path,p); c.drawLine(cx,cy-r*.7f,cx,cy+r*.7f,p);
        } else if ("briefcase".equals(type) || "planner".equals(type)) {
            c.drawRoundRect(new RectF(cx-r,cy-r*.45f,cx+r,cy+r*.72f),r*.18f,r*.18f,p); c.drawRect(new RectF(cx-r*.35f,cy-r*.78f,cx+r*.35f,cy-r*.38f),p); c.drawLine(cx-r,cy,cx+r,cy,p);
        } else if ("language".equals(type) || "talk".equals(type)) {
            c.drawCircle(cx,cy,r,p); c.drawLine(cx-r,cy,cx+r,cy,p); c.drawArc(new RectF(cx-r*.55f,cy-r,cx+r*.55f,cy+r),-90,180,false,p); c.drawArc(new RectF(cx-r*.55f,cy-r,cx+r*.55f,cy+r),90,180,false,p);
        } else if ("health".equals(type)) {
            c.drawLine(cx-r,cy,cx+r,cy,p); c.drawLine(cx,cy-r,cx,cy+r,p);
        } else if ("sleep".equals(type)) {
            path.moveTo(cx+r*.45f,cy-r); path.quadTo(cx-r*.8f,cy-r*.3f,cx-r*.1f,cy+r*.75f); path.quadTo(cx+r*.55f,cy+r*.6f,cx+r*.78f,cy+r*.1f); path.quadTo(cx+r*.1f,cy+r*.25f,cx+r*.45f,cy-r); c.drawPath(path,p);
        } else if ("mosque".equals(type)) {
            c.drawLine(cx-r,cy+r*.75f,cx+r,cy+r*.75f,p); c.drawRect(new RectF(cx-r*.65f,cy-r*.15f,cx+r*.65f,cy+r*.75f),p); path.moveTo(cx-r*.75f,cy-r*.15f); path.quadTo(cx,cy-r*1.15f,cx+r*.75f,cy-r*.15f); c.drawPath(path,p);
        } else if ("fire".equals(type) || "up".equals(type)) {
            path.moveTo(cx,cy-r); path.lineTo(cx+r*.75f,cy); path.lineTo(cx+r*.2f,cy); path.lineTo(cx+r*.2f,cy+r); path.lineTo(cx-r*.2f,cy+r); path.lineTo(cx-r*.2f,cy); path.lineTo(cx-r*.75f,cy); path.close(); c.drawPath(path,p);
        } else if ("down".equals(type)) {
            path.moveTo(cx,cy+r); path.lineTo(cx+r*.75f,cy); path.lineTo(cx+r*.2f,cy); path.lineTo(cx+r*.2f,cy-r); path.lineTo(cx-r*.2f,cy-r); path.lineTo(cx-r*.2f,cy); path.lineTo(cx-r*.75f,cy); path.close(); c.drawPath(path,p);
        } else if ("focus".equals(type)) {
            c.drawCircle(cx,cy,r,p); c.drawCircle(cx,cy,r*.45f,p); c.drawLine(cx,cy-r,cx,cy-r*.7f,p); c.drawLine(cx,cy+r*.7f,cx,cy+r,p); c.drawLine(cx-r,cy,cx-r*.7f,cy,p); c.drawLine(cx+r*.7f,cy,cx+r,cy,p);
        } else if ("star".equals(type) || "gift".equals(type) || "coffee".equals(type) || "week".equals(type) || "meal".equals(type)) {
            if ("gift".equals(type)) { c.drawRect(new RectF(cx-r,cy-r*.25f,cx+r,cy+r*.75f),p); c.drawLine(cx,cy-r*.25f,cx,cy+r*.75f,p); c.drawLine(cx-r,cy,cx+r,cy,p); }
            else if ("coffee".equals(type)) { c.drawRoundRect(new RectF(cx-r,cy-r*.45f,cx+r*.55f,cy+r*.65f),r*.18f,r*.18f,p); c.drawArc(new RectF(cx+r*.35f,cy-r*.15f,cx+r,cy+r*.45f),-90,180,false,p); }
            else { path.moveTo(cx,cy-r); for(int i=1;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float rr=(i%2==0?r:r*.45f);path.lineTo(cx+(float)Math.cos(a)*rr,cy+(float)Math.sin(a)*rr);} path.close(); c.drawPath(path,p); }
        } else {
            c.drawCircle(cx,cy,r,p);
        }
    }

    private class WeekBars extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        WeekBars(Activity c) { super(c); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int count = 7; float w = getWidth(), h = getHeight(); float base = h * .77f; float gap = w / count; float bw = gap * .42f;
            Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_MONTH, -6);
            for (int i = 0; i < count; i++) {
                int v = dayRate(c); float bh = (base - h*.12f) * v / 100f;
                p.setColor(TRACK); p.setStyle(Paint.Style.FILL); canvas.drawRoundRect(new RectF(i*gap+gap*.29f,h*.12f,i*gap+gap*.29f+bw,base),bw/2,bw/2,p);
                p.setColor(v >= 80 ? GREEN : BLUE); canvas.drawRoundRect(new RectF(i*gap+gap*.29f,base-bh,i*gap+gap*.29f+bw,base),bw/2,bw/2,p);
                p.setColor(MUTED); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(dp(9)); p.setTypeface(Typeface.DEFAULT);
                String label = new SimpleDateFormat("EEE", new Locale("ar")).format(c.getTime());
                if (label.length() > 2) label = label.substring(0,2);
                canvas.drawText(label, i*gap+gap*.5f, h*.93f, p);
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
    }
}
