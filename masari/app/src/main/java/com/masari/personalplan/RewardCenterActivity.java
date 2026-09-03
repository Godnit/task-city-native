package com.masari.personalplan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RewardCenterActivity extends Activity {
    private static final int BG = Color.rgb(246,248,252);
    private static final int NAVY = Color.rgb(24,49,83);
    private static final int TEXT = Color.rgb(31,40,54);
    private static final int MUTED = Color.rgb(103,113,128);
    private static final int GREEN = Color.rgb(22,123,98);
    private static final int GOLD = Color.rgb(184,126,28);
    private static final int PURPLE = Color.rgb(115,83,165);
    private static final int RED = Color.rgb(172,70,61);
    private static final int BORDER = Color.rgb(225,231,239);

    private SharedPreferences prefs;
    private String todayKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("masari_data", MODE_PRIVATE);
        todayKey = dateKey(Calendar.getInstance());
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(17), dp(16), dp(17), dp(34));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("مركز المكافآت", 27, NAVY, true));
        titles.addView(text("نجوم تُصرف، ونقاطك تبقى سجلًا لتقدمك", 13, MUTED, false));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button back = button("رجوع", NAVY);
        head.addView(back, new LinearLayout.LayoutParams(dp(82), dp(42)));
        back.setOnClickListener(v -> finish());
        root.addView(head);

        addWallet(root);
        addDailyClaim(root);
        addStore(root);
        addAchievements(root);
        addWeekHistory(root);
        addMonthlyMetrics(root);
        setContentView(scroll);
    }

    private void addWallet(LinearLayout root) {
        int stars = prefs.getInt("reward_stars", 0);
        int points = planPoints();
        LinearLayout c = card();
        c.setPadding(dp(16), dp(15), dp(16), dp(15));
        c.setBackground(round(Color.rgb(255,249,233), 18, Color.rgb(232,204,139)));
        add(root, c, 14);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text("رصيد المكافآت", 14, MUTED, true));
        left.addView(text("★ " + ar(stars), 31, GOLD, true));
        line.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(pill(ar(points) + " نقطة تقدم", NAVY, Color.WHITE));
        c.addView(line);
        c.addView(detail("النقاط لا تنقص عندما تستخدم مكافأة. الذي يُصرف هو النجوم فقط."));
    }

    private void addDailyClaim(LinearLayout root) {
        Calendar now = Calendar.getInstance();
        int points = dayPoints(now);
        int target = dayTarget(now);
        int pct = percent(points, target);
        boolean claimed = prefs.getBoolean("daily_bonus_claimed_" + todayKey, false);
        LinearLayout c = card();
        c.setPadding(dp(15), dp(13), dp(15), dp(13));
        add(root, c, 10);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        top.addView(text("مكافأة اليوم", 17, NAVY, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(pill(ar(pct) + "٪", pct >= 80 ? GREEN : RED, Color.rgb(248,250,253)));
        c.addView(top);
        c.addView(progress(points, target, pct >= 80 ? GREEN : RED));
        if (claimed) {
            c.addView(detail("✓ استلمت مكافأة هذا اليوم بالفعل."));
            return;
        }
        if (pct < 80) {
            c.addView(detail("عند ٨٠٪ تحصل على ★ واحدة، وعند ١٠٠٪ تحصل على ★★. أكمل المهام الأساسية أولًا."));
            return;
        }
        int gain = pct >= 100 ? 2 : 1;
        c.addView(detail("وصلت للحد المطلوب. استلم " + ar(gain) + " ★ الآن."));
        Button b = button("استلام +" + ar(gain) + " ★", GOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        lp.setMargins(0, dp(8), 0, 0);
        c.addView(b, lp);
        b.setOnClickListener(v -> {
            prefs.edit().putBoolean("daily_bonus_claimed_" + todayKey, true)
                    .putInt("reward_stars", prefs.getInt("reward_stars", 0) + gain).apply();
            Toast.makeText(this, "تمت إضافة النجوم", Toast.LENGTH_SHORT).show();
            render();
        });
    }

    private void addStore(LinearLayout root) {
        TextView h = text("متجر المكافآت", 21, NAVY, true);
        h.setPadding(0, dp(20), 0, dp(5));
        root.addView(h);
        List<Reward> rewards = new ArrayList<>();
        rewards.add(new Reward("٣٠ دقيقة ترفيه إضافي", 1));
        rewards.add(new Reward("ساعة لعبة أو أنمي/مسلسل", 2));
        rewards.add(new Reward("ساعة استكشاف علمي بحرية", 3));
        rewards.add(new Reward("مساء مرن بلا مهام ثانوية", 4));
        JSONArray custom = customRewards();
        for (int i = 0; i < custom.length(); i++) {
            JSONObject o = custom.optJSONObject(i);
            if (o != null) rewards.add(new Reward(o.optString("title"), Math.max(1, o.optInt("cost", 1))));
        }
        for (Reward reward : rewards) addReward(root, reward);
        Button add = button("＋ أضف مكافأة خاصة", NAVY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        lp.setMargins(0, dp(8), 0, 0);
        root.addView(add, lp);
        add.setOnClickListener(v -> showCustomRewardDialog());
    }

    private void addReward(LinearLayout root, Reward r) {
        int balance = prefs.getInt("reward_stars", 0);
        LinearLayout c = card();
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        add(root, c, 7);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        top.addView(text(r.title, 15, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(pill(ar(r.cost) + " ★", GOLD, Color.rgb(255,249,233)));
        c.addView(top);
        Button use = button(balance >= r.cost ? "استخدم المكافأة" : "تحتاج نجومًا أكثر", balance >= r.cost ? GREEN : MUTED);
        use.setEnabled(balance >= r.cost);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        lp.setMargins(0, dp(8), 0, 0);
        c.addView(use, lp);
        use.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("استخدام المكافأة")
                .setMessage("صرف " + r.cost + " ★ على «" + r.title + "»؟\nلن تنقص نقاط تقدمك.")
                .setPositiveButton("استخدم", (d,w) -> redeem(r))
                .setNegativeButton("إلغاء", null).show());
    }

    private void redeem(Reward r) {
        int balance = prefs.getInt("reward_stars", 0);
        if (balance < r.cost) return;
        prefs.edit().putInt("reward_stars", balance - r.cost).apply();
        try {
            JSONArray a = redemptions();
            JSONObject o = new JSONObject();
            o.put("title", r.title); o.put("cost", r.cost); o.put("date", arabicDate());
            a.put(o); prefs.edit().putString("reward_redemptions", a.toString()).apply();
        } catch (Exception ignored) {}
        Toast.makeText(this, "تم صرف المكافأة — استمتع بها", Toast.LENGTH_SHORT).show();
        render();
    }

    private void showCustomRewardDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), 0, dp(18), 0);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText name = new EditText(this);
        name.setHint("مثال: خروج لمكان أحبه"); name.setGravity(Gravity.RIGHT); name.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText cost = new EditText(this);
        cost.setHint("عدد النجوم"); cost.setInputType(InputType.TYPE_CLASS_NUMBER); cost.setGravity(Gravity.RIGHT);
        box.addView(name); box.addView(cost);
        new AlertDialog.Builder(this).setTitle("مكافأة خاصة").setView(box)
                .setPositiveButton("حفظ", (d,w) -> {
                    String n = name.getText().toString().trim(); int c = 1;
                    try { c = Math.max(1, Integer.parseInt(cost.getText().toString().trim())); } catch (Exception ignored) {}
                    if (!n.isEmpty()) {
                        try { JSONArray a = customRewards(); JSONObject o = new JSONObject(); o.put("title", n); o.put("cost", c); a.put(o); prefs.edit().putString("custom_rewards", a.toString()).apply(); } catch (Exception ignored) {}
                    }
                    render();
                }).setNegativeButton("إلغاء", null).show();
    }

    private void addAchievements(LinearLayout root) {
        TextView h = text("إنجازات خاصة", 21, NAVY, true); h.setPadding(0, dp(22), 0, dp(5)); root.addView(h);
        int total = planPoints();
        int strong = countStrongDays();
        int quran = countDoneContains("_quran");
        int english = countDoneEndsWith("_english");
        int workout = countDoneContains("_workout");
        int work = countDoneEndsWith("_work");
        achievement(root, "أول ٥٠٠ نقطة", "ابدأ سجلًا حقيقيًا للاستمرارية.", total, 500);
        achievement(root, "٧ أيام قوية", "سبعة أيام وصلت فيها إلى ٨٠٪ أو أكثر.", strong, 7);
        achievement(root, "١٠ جلسات قرآن", "عشر مهام قرآن منجزة.", quran, 10);
        achievement(root, "١٠ جلسات إنجليزية", "عشر ساعات صباحية منجزة.", english, 10);
        achievement(root, "٨ تمارين A/B", "ثماني جلسات قوة دون مطاردة الإنهاك.", workout, 8);
        achievement(root, "٥ جلسات عمل", "خمس خطوات تنفيذية في السوق/المشروع.", work, 5);
    }

    private void achievement(LinearLayout root, String title, String note, int value, int target) {
        boolean open = value >= target;
        LinearLayout c = card(); c.setPadding(dp(13), dp(11), dp(13), dp(11)); if (!open) c.setAlpha(.72f); add(root, c, 6);
        LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL); line.setGravity(Gravity.CENTER_VERTICAL); line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        line.addView(text(open ? "★" : "☆", 24, open ? GOLD : MUTED, true), new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.addView(text(title, 15, open ? TEXT : MUTED, true)); info.addView(text(note, 12, MUTED, false));
        line.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(pill(ar(Math.min(value,target)) + "/" + ar(target), open ? GREEN : MUTED, Color.rgb(248,250,253))); c.addView(line);
    }

    private void addWeekHistory(LinearLayout root) {
        TextView h = text("آخر ٧ أيام", 21, NAVY, true); h.setPadding(0, dp(22), 0, dp(5)); root.addView(h);
        Calendar d = Calendar.getInstance(); d.add(Calendar.DAY_OF_MONTH, -6);
        for (int i = 0; i < 7; i++) {
            int value = dayPoints(d), target = dayTarget(d), pct = percent(value, target);
            LinearLayout c = card(); c.setPadding(dp(13), dp(10), dp(13), dp(10)); add(root, c, 5);
            LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL); line.setGravity(Gravity.CENTER_VERTICAL); line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            String label = new SimpleDateFormat("EEEE d/M", new Locale("ar")).format(d.getTime());
            line.addView(text(label, 14, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(pill(ar(pct) + "٪", gradeColor(pct), Color.rgb(248,250,253))); c.addView(line); c.addView(progress(value, target, gradeColor(pct)));
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void addMonthlyMetrics(LinearLayout root) {
        TextView h = text("مؤشرات هذا الشهر", 21, NAVY, true); h.setPadding(0, dp(22), 0, dp(5)); root.addView(h);
        int days = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        int q1 = monthDone("quran1"), q2 = monthDone("quran2"), en = monthDone("english");
        int workout = monthDone("workoutA") + monthDone("workoutB");
        int work = monthDone("work");
        int books = monthDone("leap1") + monthDone("leap2") + monthDone("leap3");
        metric(root, "القرآن — المراجعة الجديدة", q1, days, GREEN);
        metric(root, "القرآن — القديمة/البصرية", q2, days, GREEN);
        metric(root, "جلسات الإنجليزية", en, expectedEnglishThisMonth(), Color.rgb(46,94,170));
        metric(root, "تمارين الجسد", workout, expectedWorkoutThisMonth(), RED);
        metric(root, "جلسات العمل", work, expectedWorkThisMonth(), GOLD);
        metric(root, "كتب LeapAhead", books, days * 2, PURPLE);
    }

    private void metric(LinearLayout root, String label, int value, int target, int color) {
        target = Math.max(1,target);
        LinearLayout c = card(); c.setPadding(dp(13), dp(10), dp(13), dp(10)); add(root, c, 6);
        LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL); line.setGravity(Gravity.CENTER_VERTICAL); line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        line.addView(text(label, 14, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(pill(ar(value) + "/" + ar(target), color, Color.rgb(248,250,253))); c.addView(line); c.addView(progress(value, target, color));
    }

    private int dayPoints(Calendar c) { return prefs.getInt("reward_day_points_" + dateKey(c), 0); }

    private int dayTarget(Calendar c) {
        int day = c.get(Calendar.DAY_OF_WEEK);
        int total = 75;
        if (day == Calendar.SATURDAY || day == Calendar.TUESDAY) total += 20;
        if (day != Calendar.FRIDAY) total += 25;
        if (day != Calendar.FRIDAY) total += 25;
        switch(day) {
            case Calendar.SATURDAY: total += 8; break;
            case Calendar.SUNDAY: total += 10; break;
            case Calendar.MONDAY: total += 8; break;
            case Calendar.TUESDAY: total += 10; break;
            case Calendar.WEDNESDAY: total += 12; break;
            case Calendar.THURSDAY: total += 12; break;
        }
        return Math.max(1,total);
    }

    private int planPoints() {
        Calendar start = Calendar.getInstance(); start.set(2026, Calendar.SEPTEMBER, 1);
        Calendar end = Calendar.getInstance(); end.set(2027, Calendar.MAY, 31);
        Calendar now = Calendar.getInstance(); Calendar last = now.before(end) ? now : end; int total = 0;
        while (!start.after(last)) { total += dayPoints(start); start.add(Calendar.DAY_OF_MONTH, 1); }
        return total;
    }

    private int countStrongDays() {
        Calendar d = Calendar.getInstance(); d.set(2026, Calendar.SEPTEMBER, 1); Calendar now = Calendar.getInstance(); int n = 0;
        while (!d.after(now)) { if (percent(dayPoints(d), dayTarget(d)) >= 80) n++; d.add(Calendar.DAY_OF_MONTH, 1); }
        return n;
    }

    private int countDoneContains(String fragment) {
        int n = 0; for (Map.Entry<String,?> e : prefs.getAll().entrySet()) if (e.getKey().startsWith("reward_done_") && e.getKey().contains(fragment) && Boolean.TRUE.equals(e.getValue())) n++; return n;
    }

    private int countDoneEndsWith(String suffix) {
        int n = 0; for (Map.Entry<String,?> e : prefs.getAll().entrySet()) if (e.getKey().startsWith("reward_done_") && e.getKey().endsWith(suffix) && Boolean.TRUE.equals(e.getValue())) n++; return n;
    }

    private int monthDone(String id) {
        Calendar d = Calendar.getInstance(); d.set(Calendar.DAY_OF_MONTH, 1); int month = d.get(Calendar.MONTH), n = 0;
        while (d.get(Calendar.MONTH) == month) { if (prefs.getBoolean("reward_done_" + dateKey(d) + "_" + id, false)) n++; d.add(Calendar.DAY_OF_MONTH, 1); }
        return n;
    }

    private int expectedEnglishThisMonth() {
        Calendar d = Calendar.getInstance(); d.set(Calendar.DAY_OF_MONTH, 1); int month = d.get(Calendar.MONTH), n = 0;
        while (d.get(Calendar.MONTH) == month) { if (d.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) n++; d.add(Calendar.DAY_OF_MONTH, 1); } return n;
    }

    private int expectedWorkoutThisMonth() {
        Calendar d = Calendar.getInstance(); d.set(Calendar.DAY_OF_MONTH, 1); int month = d.get(Calendar.MONTH), n = 0;
        while (d.get(Calendar.MONTH) == month) { int dow = d.get(Calendar.DAY_OF_WEEK); if (dow == Calendar.SATURDAY || dow == Calendar.TUESDAY) n++; d.add(Calendar.DAY_OF_MONTH, 1); } return n;
    }

    private int expectedWorkThisMonth() { return expectedEnglishThisMonth(); }

    private JSONArray customRewards() { try { return new JSONArray(prefs.getString("custom_rewards", "[]")); } catch (Exception e) { return new JSONArray(); } }
    private JSONArray redemptions() { try { return new JSONArray(prefs.getString("reward_redemptions", "[]")); } catch (Exception e) { return new JSONArray(); } }
    private int percent(int value, int target) { return target <= 0 ? 0 : Math.min(999, Math.round(value * 100f / target)); }
    private int gradeColor(int pct) { if (pct >= 80) return GREEN; if (pct >= 50) return GOLD; return RED; }
    private String dateKey(Calendar c) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime()); }
    private String arabicDate() { return new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar")).format(new Date()); }
    private String ar(int n) { return String.valueOf(n).replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤').replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩'); }

    private void add(LinearLayout root, View v, int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0, dp(top), 0, 0); root.addView(v, lp); }
    private LinearLayout card() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); c.setBackground(round(Color.WHITE,18,BORDER)); c.setElevation(dp(1)); return c; }
    private TextView text(String value, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.RIGHT); t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t; }
    private TextView detail(String value) { TextView t = text(value,12,MUTED,false); t.setPadding(0,dp(5),0,0); return t; }
    private TextView pill(String value, int color, int bg) { TextView t = text(value,12,color,true); t.setGravity(Gravity.CENTER); t.setPadding(dp(9),dp(5),dp(9),dp(5)); t.setBackground(round(bg,22)); return t; }
    private Button button(String value, int color) { Button b = new Button(this); b.setText(value); b.setTextSize(13); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(round(color,14)); return b; }
    private ProgressBar progress(int value, int max, int color) { ProgressBar p = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); p.setMax(Math.max(1,max)); p.setProgress(Math.min(value,Math.max(1,max))); p.setProgressTintList(ColorStateList.valueOf(color)); p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(231,235,241))); p.setMinimumHeight(dp(9)); return p; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private GradientDrawable round(int color, int radius, int stroke) { GradientDrawable d = round(color,radius); d.setStroke(dp(1),stroke); return d; }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    static class Reward { final String title; final int cost; Reward(String title,int cost) { this.title=title; this.cost=cost; } }
}
