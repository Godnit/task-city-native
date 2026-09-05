package com.masari.personalplan;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v0.18 is intentionally a presentation/UX layer over v0.17.
 * It keeps v17's data engine, week rules, timer, backup and reward logic intact,
 * while making the screens denser, calmer and easier to read at a glance.
 */
public class MasariV18Activity extends MasariV17Activity {
    private static final int GREEN = Color.rgb(20,145,80);
    private static final int BLUE = Color.rgb(67,130,213);
    private static final int ORANGE = Color.rgb(235,145,49);
    private static final int PURPLE = Color.rgb(133,84,190);
    private static final int RED = Color.rgb(211,78,72);
    private static final int NAVY = Color.rgb(7,42,61);
    private static final int LIGHT_BG = Color.rgb(249,250,251);
    private static final int LIGHT_CARD = Color.WHITE;
    private static final int LIGHT_TEXT = Color.rgb(24,31,39);
    private static final int LIGHT_MUTED = Color.rgb(107,116,126);
    private static final int LIGHT_BORDER = Color.rgb(231,234,237);
    private static final int DARK_BG = Color.rgb(17,24,29);
    private static final int DARK_CARD = Color.rgb(27,36,42);
    private static final int DARK_TEXT = Color.rgb(239,244,246);
    private static final int DARK_MUTED = Color.rgb(164,178,186);
    private static final int DARK_BORDER = Color.rgb(47,60,68);

    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences data;
    private boolean decorating;

    @Override protected void onCreate(Bundle b) {
        data = getSharedPreferences("masari_data", MODE_PRIVATE);
        migrateV18();
        super.onCreate(b);
    }

    @Override public void onContentChanged() {
        super.onContentChanged();
        if (decorating) return;
        ui.post(this::decorateCurrentContent);
    }

    private void migrateV18() {
        if (data.getBoolean("v18_migrated", false)) return;
        data.edit()
                .putBoolean("v18_migrated", true)
                .putBoolean("v18_time_clarity", true)
                .putBoolean("v18_compact_home", true)
                .apply();
    }

    private boolean dark() { return data != null && data.getBoolean("dark_mode", false); }
    private int bg() { return dark() ? DARK_BG : LIGHT_BG; }
    private int card() { return dark() ? DARK_CARD : LIGHT_CARD; }
    private int textColor() { return dark() ? DARK_TEXT : LIGHT_TEXT; }
    private int muted() { return dark() ? DARK_MUTED : LIGHT_MUTED; }
    private int border() { return dark() ? DARK_BORDER : LIGHT_BORDER; }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void decorateCurrentContent() {
        if (decorating) return;
        decorating = true;
        try {
            ViewGroup androidRoot = findViewById(android.R.id.content);
            if (androidRoot == null || androidRoot.getChildCount() == 0) return;
            View shellView = androidRoot.getChildAt(0);
            if (!(shellView instanceof LinearLayout)) return;
            LinearLayout shell = (LinearLayout) shellView;
            compactBottomNavigation(shell);
            if (shell.getChildCount() == 0 || !(shell.getChildAt(0) instanceof FrameLayout)) return;
            FrameLayout body = (FrameLayout) shell.getChildAt(0);
            if (body.getChildCount() == 0) return;
            View page = body.getChildAt(0);
            if (!(page instanceof ScrollView)) return;
            ScrollView scroll = (ScrollView) page;
            if (scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;
            LinearLayout root = (LinearLayout) scroll.getChildAt(0);
            if ("v18".equals(root.getTag())) return;
            root.setTag("v18");
            String screen = topTitle(root);
            if ("مساري".equals(screen)) decorateHome(root);
            else if ("المهام".equals(screen)) decorateTasks(root);
            else if ("الإحصائيات".equals(screen)) decorateStats(root);
            else if ("المكافآت".equals(screen)) decorateRewards(root);
            improveAccessibility(root);
        } finally {
            decorating = false;
        }
    }

    private String topTitle(LinearLayout root) {
        if (root.getChildCount() == 0) return "";
        List<TextView> texts = new ArrayList<>();
        collectTextViews(root.getChildAt(0), texts);
        String[] names = {"مساري","المهام","الإحصائيات","المكافآت","المزيد","مراجعة الأسبوع","المراجعة الشهرية","الأهداف الكبرى","لاحقًا"};
        for (String n : names) for (TextView t : texts) if (n.equals(String.valueOf(t.getText()))) return n;
        return "";
    }

    /* ---------------- HOME ---------------- */

    private void decorateHome(LinearLayout root) {
        View progress = directChildContaining(root, "تقدمك");
        if (progress != null) root.removeView(progress); // detailed progress belongs in Stats, not above today's actions.

        View match = directChildContaining(root, "بطاقة المباراة الأسبوعية");
        View emergency = directChildContainingEither(root, "هل تغيّر يومك؟", "الوضع الحالي:");
        if (match != null || emergency != null) {
            if (match != null) root.removeView(match);
            if (emergency != null) root.removeView(emergency);
            LinearLayout flexibility = new LinearLayout(this);
            flexibility.setOrientation(LinearLayout.HORIZONTAL);
            flexibility.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            flexibility.setPadding(0, dp(5), 0, 0);
            if (match != null) flexibility.addView(compactAction("بطاقة المباراة", match, GREEN), weight());
            if (emergency != null) flexibility.addView(compactAction("ظروف اليوم", emergency, BLUE), weight());
            int goal = indexOfDirectChild(root, "هدف اليوم");
            if (goal < 0) goal = indexOfDirectChild(root, "هدف الخميس");
            int at = goal >= 0 ? Math.min(root.getChildCount(), goal + 1) : Math.min(4, root.getChildCount());
            root.addView(flexibility, at, new LinearLayout.LayoutParams(-1, dp(58)));
        }

        View now = directChildContaining(root, "الآن");
        if (now != null) {
            now.setElevation(dp(1));
            now.setMinimumHeight(dp(104));
        }

        // Keep the home list intentionally short. v17 already limits it; v18 makes the section quieter.
        View scheduleHeader = directChildContaining(root, "جدول اليوم");
        if (scheduleHeader instanceof ViewGroup) {
            scheduleHeader.setPadding(0, dp(7), 0, dp(3));
        }
    }

    private View compactAction(String title, View delegate, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setPadding(dp(9), dp(6), dp(9), dp(6));
        box.setBackground(round(card(), 14, border()));
        TextView dot = new TextView(this);
        dot.setText("●");
        dot.setTextColor(color);
        dot.setTextSize(12);
        dot.setGravity(Gravity.CENTER);
        box.addView(dot, new LinearLayout.LayoutParams(dp(24), -1));
        TextView label = tv(title, 9.5f, textColor(), true);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        box.addView(label, new LinearLayout.LayoutParams(0, -1, 1));
        TextView arrow = tv("‹", 17, muted(), false);
        arrow.setGravity(Gravity.CENTER);
        box.addView(arrow, new LinearLayout.LayoutParams(dp(22), -1));
        box.setOnClickListener(v -> delegate.performClick());
        box.setContentDescription(title);
        return box;
    }

    /* ---------------- TASKS ---------------- */

    private void decorateTasks(LinearLayout root) {
        int total = countClass(root, "CheckView");
        int essentials = Math.max(0, countExactText(root, "أساسي") - 1); // minus filter chip
        LinearLayout timeline = timelineCard(total, essentials);
        int insertAt = Math.min(2, root.getChildCount()); // after title + week strip
        root.addView(timeline, insertAt, new LinearLayout.LayoutParams(-1, dp(104)));

        String section = "";
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child == timeline) continue;
            if (!containsClass(child, "CheckView")) {
                String heading = sectionHeading(child);
                if (!heading.isEmpty()) {
                    section = heading;
                    if ("مكتمل".equals(section) || "متأخر".equals(section) || "لاحقًا اليوم".equals(section)) compactSectionHeader(child);
                }
                continue;
            }
            enhanceTaskCard(child, section);
        }
    }

    private LinearLayout timelineCard(int total, int essentials) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(9), dp(6), dp(9), dp(6));
        box.setBackground(round(card(), 14, border()));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title = tv("مسار اليوم", 10.5f, textColor(), true);
        head.addView(title, new LinearLayout.LayoutParams(0, dp(22), 1));
        TextView now = pillText(formatNow(), GREEN);
        head.addView(now, new LinearLayout.LayoutParams(-2, dp(24)));
        box.addView(head);
        box.addView(new DayTimelineView(), new LinearLayout.LayoutParams(-1, dp(48)));
        String summary;
        if (total == 0 && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) summary = "الجمعة راحة — لا مهام مطلوبة";
        else summary = currentPhase() + "  •  " + ar(total) + " مهام  •  " + ar(essentials) + " أساسية";
        TextView sub = tv(summary, 8, muted(), false);
        sub.setGravity(Gravity.CENTER);
        box.addView(sub, new LinearLayout.LayoutParams(-1, dp(18)));
        return box;
    }

    private void enhanceTaskCard(View cardView, String section) {
        if (!(cardView instanceof ViewGroup)) return;
        List<TextView> texts = new ArrayList<>();
        collectTextViews(cardView, texts);
        TextView timeLine = null, goalLine = null, title = null;
        for (TextView t : texts) {
            String s = String.valueOf(t.getText());
            if (title == null && s.length() > 3 && !s.contains("يخدم:") && !isTier(s) && !looksLikeTimeLine(s)) title = t;
            if (timeLine == null && looksLikeTimeLine(s)) timeLine = t;
            if (goalLine == null && s.startsWith("يخدم:")) goalLine = t;
            if (isTier(s)) styleTier(t, s);
        }
        if (timeLine == null) return;
        int start = parseStartMinutes(String.valueOf(timeLine.getText()));
        if (start < 0) return;
        String titleText = title == null ? "" : String.valueOf(title.getText());
        int duration = durationForTitle(titleText);
        int end = Math.min(1439, start + duration);
        String domain = extractDomain(String.valueOf(timeLine.getText()));
        timeLine.setText(formatTime(start) + " – " + formatTime(end) + "  •  " + durationLabel(duration) + (domain.isEmpty() ? "" : "  •  " + domain));
        timeLine.setTextSize(9.2f);
        timeLine.setTextColor(dark() ? Color.rgb(195,207,214) : Color.rgb(77,88,97));
        timeLine.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        String status = relativeStatus(start, end, section);
        int statusColor = relativeStatusColor(start, end, section);
        if (goalLine != null) {
            String goal = String.valueOf(goalLine.getText());
            goalLine.setText(status + "  •  " + goal);
            goalLine.setTextColor(statusColor);
            goalLine.setTextSize(7.8f);
        } else if (cardView instanceof LinearLayout) {
            TextView extra = tv(status, 7.8f, statusColor, true);
            extra.setPadding(dp(48), dp(2), dp(48), 0);
            ((LinearLayout) cardView).addView(extra);
        }
        cardView.setMinimumHeight(dp(70));
    }

    private void styleTier(TextView t, String tier) {
        int c = "أساسي".equals(tier) ? GREEN : "اختياري".equals(tier) ? PURPLE : muted();
        int fill = withAlpha(c, dark() ? 55 : 24);
        t.setTextColor(c);
        t.setTextSize(7.5f);
        t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        t.setPadding(dp(7), dp(3), dp(7), dp(3));
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(10));
        g.setStroke(dp(1), withAlpha(c, 70));
        t.setBackground(g);
    }

    private void compactSectionHeader(View v) {
        if (!(v instanceof ViewGroup)) return;
        v.setPadding(0, dp(3), 0, dp(2));
        List<TextView> list = new ArrayList<>();
        collectTextViews(v, list);
        for (TextView t : list) if ("مكتمل".equals(String.valueOf(t.getText())) || "متأخر".equals(String.valueOf(t.getText())) || "لاحقًا اليوم".equals(String.valueOf(t.getText()))) t.setTextSize(13.5f);
    }

    /* ---------------- STATS ---------------- */

    private void decorateStats(LinearLayout root) {
        int trendHeader = indexOfDirectChild(root, "اتجاه الشهر");
        if (trendHeader >= 0 && trendHeader + 1 < root.getChildCount()) {
            View chart = root.getChildAt(trendHeader + 1);
            if (chart instanceof LinearLayout) {
                LinearLayout box = (LinearLayout) chart;
                int days = activeDataDays(28);
                if (days < 7) {
                    box.removeAllViews();
                    box.setGravity(Gravity.CENTER);
                    TextView title = tv("نحتاج بيانات أكثر", 11, textColor(), true);
                    title.setGravity(Gravity.CENTER);
                    TextView sub = tv("بعد ٧ أيام فيها تنفيذ فعلي سيظهر اتجاه الشهر بدل خط متقطع قد يضللك.", 8.2f, muted(), false);
                    sub.setGravity(Gravity.CENTER);
                    sub.setPadding(dp(18), dp(5), dp(18), 0);
                    box.addView(title, new LinearLayout.LayoutParams(-1, dp(28)));
                    box.addView(sub, new LinearLayout.LayoutParams(-1, dp(48)));
                } else {
                    TextView note = tv("كل نقطة تمثل نسبة إنجاز يوم فعلي.", 7.8f, muted(), false);
                    note.setGravity(Gravity.CENTER);
                    box.addView(note, new LinearLayout.LayoutParams(-1, dp(22)));
                }
            }
        }
        View domainHeader = directChildContaining(root, "أداء المجالات");
        if (domainHeader instanceof ViewGroup) {
            TextView hint = tv("النسبة = ما نُفذ من المطلوب هذا الأسبوع", 7.5f, muted(), false);
            hint.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            ((ViewGroup) domainHeader).addView(hint);
        }
    }

    /* ---------------- REWARDS ---------------- */

    private void decorateRewards(LinearLayout root) {
        int hero = indexOfDirectChild(root, "نقاط التقدم");
        if (hero < 0) hero = indexOfDirectChild(root, "نقاط التقدم — لا تُصرف");
        if (hero >= 0) {
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.HORIZONTAL);
            info.setGravity(Gravity.CENTER);
            info.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            info.setPadding(dp(9), dp(5), dp(9), dp(5));
            info.setBackground(round(card(), 12, border()));
            TextView a = tv("نقاط المستوى: تبني رتبتك ولا تنقص", 7.8f, GREEN, true);
            TextView b = tv("رصيد المكافآت: هو الذي يُصرف", 7.8f, ORANGE, true);
            a.setGravity(Gravity.CENTER); b.setGravity(Gravity.CENTER);
            info.addView(a, new LinearLayout.LayoutParams(0, dp(30), 1));
            info.addView(b, new LinearLayout.LayoutParams(0, dp(30), 1));
            root.addView(info, Math.min(root.getChildCount(), hero + 1), new LinearLayout.LayoutParams(-1, dp(40)));
        }
    }

    /* ---------------- NAV / ACCESSIBILITY ---------------- */

    private void compactBottomNavigation(LinearLayout shell) {
        if (shell.getChildCount() < 2 || !(shell.getChildAt(1) instanceof LinearLayout)) return;
        LinearLayout nav = (LinearLayout) shell.getChildAt(1);
        ViewGroup.LayoutParams nlp = nav.getLayoutParams();
        if (nlp != null) { nlp.height = dp(66); nav.setLayoutParams(nlp); }
        for (int i=0;i<nav.getChildCount();i++) {
            View item = nav.getChildAt(i);
            ViewGroup.LayoutParams lp = item.getLayoutParams();
            if (lp == null) continue;
            boolean active = item.getBackground() != null;
            lp.height = dp(active ? 56 : 50);
            item.setLayoutParams(lp);
        }
    }

    private void improveAccessibility(View v) {
        if (v instanceof Button) ((Button)v).setMinHeight(dp(44));
        if (v.isClickable() && (v.getContentDescription()==null || v.getContentDescription().length()==0)) {
            String label = firstMeaningfulText(v);
            if (!label.isEmpty()) v.setContentDescription(label);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++) improveAccessibility(g.getChildAt(i));
        }
    }

    /* ---------------- TIME ---------------- */

    private static final Pattern TIME = Pattern.compile("([0-9٠-٩]{1,2})[:：]([0-9٠-٩]{2})\\s*([صم])");

    private boolean looksLikeTimeLine(String s) { return TIME.matcher(s).find() && (s.contains("•") || s.contains("·")); }

    private int parseStartMinutes(String s) {
        Matcher m=TIME.matcher(s); if(!m.find()) return -1;
        int h=parseArabicInt(m.group(1)), min=parseArabicInt(m.group(2));
        String ap=m.group(3);
        if (h==12) h=0;
        if ("م".equals(ap)) h+=12;
        return h*60+min;
    }

    private int durationForTitle(String title) {
        if (title.contains("الفجر والحلقة")) return 150;
        if (title.contains("الفجر والورد")) return 90;
        if (title.contains("النوم التكميلي")) return 90;
        if (title.contains("الإنجليزية:") || title.contains("اختبار الإنجليزية")) return 60;
        if (title.contains("القبول الطبي")) return 90;
        if (title.contains("الطب والقبول للأسبوع")) return 60;
        if (title.contains("العمل:") || title.contains("تطوير المشروع") || title.contains("الوصول للسوق")) return 60;
        if (title.contains("مراجعة القرآن الكبرى")) return 90;
        if (title.contains("مراجعة الإنجليزية الأسبوعية")) return 45;
        if (title.contains("مراجعة العمل والدخل")) return 45;
        if (title.contains("مراجعة جديدة: صفحتان")) return 75;
        if (title.contains("صفحتان + مراجعة قديمة")) return 85;
        if (title.contains("المغرب والتحفيظ والعشاء")) return 120;
        if (title.contains("العشاء مع الأسرة") || title.contains("LeapAhead")) return 30;
        if (title.contains("الغداء")) return 30;
        if (title.contains("إغلاق اليوم")) return 20;
        if (title.contains("الاستعداد للنوم")) return 10;
        if (title.contains("تمرين")) return 30;
        if (title.contains("تدريب") || title.contains("الأسرة")) return 30;
        if (title.contains("مراجعة الصحة") || title.contains("مراجعة الأسرة")) return 30;
        return 30;
    }

    private String relativeStatus(int start, int end, String section) {
        if ("مكتمل".equals(section)) return "✓ مكتملة";
        int now = nowMinutes();
        if ("متأخر".equals(section)) return "متأخرة " + humanDelta(Math.max(0, now-end));
        if (now >= start && now < end) return "الآن • تبقى " + humanDelta(end-now);
        if (now < start) return "بعد " + humanDelta(start-now);
        return "انتهى وقتها";
    }

    private int relativeStatusColor(int start,int end,String section) {
        if ("مكتمل".equals(section)) return GREEN;
        if ("متأخر".equals(section)) return RED;
        int now=nowMinutes();
        if(now>=start&&now<end) return GREEN;
        if(now<start) return BLUE;
        return muted();
    }

    private String humanDelta(int min) {
        if (min <= 0) return "الآن";
        if (min < 60) return ar(min) + " د";
        int h=min/60,m=min%60;
        if (m==0) return ar(h)+" س";
        return ar(h)+" س "+ar(m)+" د";
    }

    private String durationLabel(int min) {
        if (min==120) return "ساعتان";
        if (min==60) return "ساعة";
        if (min>60) return "ساعة و"+ar(min-60)+" د";
        return ar(min)+" دقيقة";
    }

    private String extractDomain(String line) {
        int p=line.lastIndexOf('•');
        if(p<0) return "";
        return line.substring(p+1).trim();
    }

    private int nowMinutes(){Calendar c=Calendar.getInstance();return c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);}
    private String formatNow(){return formatTime(nowMinutes());}
    private String formatTime(int minutes){int h=(minutes/60)%24,m=minutes%60;String ap=h>=12?"م":"ص";int hh=h%12;if(hh==0)hh=12;return ar(hh)+":"+(m<10?"٠":"")+ar(m)+" "+ap;}
    private String currentPhase(){int n=nowMinutes();if(n>=240&&n<720)return"الفجر والصباح";if(n<900)return"الظهر";if(n<1080)return"العصر";if(n<1200)return"المغرب";if(n<1320)return"بعد العشاء";return"وقت الإغلاق والنوم";}

    /* ---------------- DATA SUFFICIENCY ---------------- */

    private int activeDataDays(int span) {
        int n=0; Calendar c=Calendar.getInstance();
        SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd", Locale.US);
        for(int i=0;i<span;i++){
            String k=f.format(c.getTime());
            if(data.getInt("reward_day_points_"+k,0)>0 || hasAnyStatusFor(k)) n++;
            c.add(Calendar.DAY_OF_MONTH,-1);
        }
        return n;
    }

    private boolean hasAnyStatusFor(String dateKey){
        for(String k:data.getAll().keySet()) if(k.startsWith("task_status_"+dateKey+"_") || (k.startsWith("reward_done_"+dateKey+"_") && Boolean.TRUE.equals(data.getAll().get(k)))) return true;
        return false;
    }

    /* ---------------- VIEW HELPERS ---------------- */

    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(2),0,dp(2),0);return p;}

    private TextView tv(String s,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}

    private TextView pillText(String s,int color){TextView t=tv(s,8,color,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(9),0,dp(9),0);GradientDrawable g=new GradientDrawable();g.setColor(withAlpha(color,dark()?48:20));g.setCornerRadius(dp(12));g.setStroke(dp(1),withAlpha(color,65));t.setBackground(g);return t;}

    private GradientDrawable round(int color,int radius,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}

    private int withAlpha(int color,int alpha){return Color.argb(Math.max(0,Math.min(255,alpha)),Color.red(color),Color.green(color),Color.blue(color));}

    private void collectTextViews(View v,List<TextView> out){if(v instanceof TextView)out.add((TextView)v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectTextViews(g.getChildAt(i),out);}}

    private View directChildContaining(LinearLayout root,String query){for(int i=0;i<root.getChildCount();i++)if(containsText(root.getChildAt(i),query))return root.getChildAt(i);return null;}
    private View directChildContainingEither(LinearLayout root,String a,String b){for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);if(containsText(v,a)||containsText(v,b))return v;}return null;}
    private int indexOfDirectChild(LinearLayout root,String query){for(int i=0;i<root.getChildCount();i++)if(containsText(root.getChildAt(i),query))return i;return -1;}

    private boolean containsText(View v,String query){if(v instanceof TextView&&String.valueOf(((TextView)v).getText()).contains(query))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),query))return true;}return false;}
    private boolean containsClass(View v,String simple){if(v.getClass().getSimpleName().equals(simple))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsClass(g.getChildAt(i),simple))return true;}return false;}
    private int countClass(View v,String simple){int n=v.getClass().getSimpleName().equals(simple)?1:0;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)n+=countClass(g.getChildAt(i),simple);}return n;}
    private int countExactText(View v,String exact){int n=(v instanceof TextView&&exact.equals(String.valueOf(((TextView)v).getText())))?1:0;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)n+=countExactText(g.getChildAt(i),exact);}return n;}

    private String sectionHeading(View v){String[] h={"الآن","التالي","لاحقًا اليوم","مكتمل","متأخر","لاحقًا"};for(String s:h)if(hasExactText(v,s))return s;return"";}
    private boolean hasExactText(View v,String x){if(v instanceof TextView&&x.equals(String.valueOf(((TextView)v).getText())))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasExactText(g.getChildAt(i),x))return true;}return false;}
    private boolean isTier(String s){return"أساسي".equals(s)||"مساند".equals(s)||"اختياري".equals(s);}

    private String firstMeaningfulText(View v){List<TextView> list=new ArrayList<>();collectTextViews(v,list);for(TextView t:list){String s=String.valueOf(t.getText()).trim();if(s.length()>1&&!"‹".equals(s)&&!"…".equals(s)&&!"•••".equals(s))return s;}return"";}

    private int parseArabicInt(String s){String e="٠١٢٣٤٥٦٧٨٩";StringBuilder b=new StringBuilder();for(char c:s.toCharArray()){int i=e.indexOf(c);b.append(i>=0?(char)('0'+i):c);}try{return Integer.parseInt(b.toString());}catch(Exception ex){return 0;}}
    private String ar(int n){String s=String.valueOf(n),e="0123456789",a="٠١٢٣٤٥٦٧٨٩";for(int i=0;i<10;i++)s=s.replace(e.charAt(i),a.charAt(i));return s;}

    /* ---------------- TIMELINE DRAWING ---------------- */

    private class DayTimelineView extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String[] labels={"الفجر","الظهر","العصر","المغرب","العشاء","النوم"};
        private final int[] starts={240,720,900,1080,1200,1320};
        DayTimelineView(){super(MasariV18Activity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight(),left=dp(20),right=w-dp(20),y=dp(17);int active=phaseIndex();
            p.setStrokeWidth(dp(2));p.setColor(dark()?Color.rgb(60,72,79):Color.rgb(224,229,232));c.drawLine(left,y,right,y,p);
            float step=(right-left)/(labels.length-1f);
            for(int i=0;i<labels.length;i++){
                float x=right-i*step;boolean on=i==active;
                p.setStyle(Paint.Style.FILL);p.setColor(on?GREEN:(i<active?withAlpha(GREEN,115):(dark()?Color.rgb(76,88,95):Color.rgb(212,219,224))));c.drawCircle(x,y,dp(on?5:3.4f),p);
                p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",on?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(dp(6.8f));p.setColor(on?GREEN:muted());c.drawText(labels[i],x,h-dp(5),p);
            }
        }
        private int phaseIndex(){int n=nowMinutes();int idx=0;for(int i=0;i<starts.length;i++)if(n>=starts[i])idx=i;return idx;}
    }
}
