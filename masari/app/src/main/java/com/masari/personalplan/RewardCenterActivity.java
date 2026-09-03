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
        int pct = percent(points,target);
        boolean claimed80 = prefs.getBoolean("reward_claim_80_" + todayKey,false);
        boolean claimed100 = prefs.getBoolean("reward_claim_100_" + todayKey,false);

        TextView h = text("مكافأة اليوم",21,NAVY,true);
        h.setPadding(0,dp(22),0,dp(5)); root.addView(h);
        LinearLayout c = card(); c.setPadding(dp(15),dp(13),dp(15),dp(13)); add(root,c,6);
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        l.addView(text("إنجاز اليوم",16,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        l.addView(pill(ar(pct)+"٪",gradeColor(pct),Color.rgb(248,250,253))); c.addView(l);
        c.addView(progress(points,target,gradeColor(pct)));
        c.addView(detail("عند ٨٠٪: ★ واحدة. عند ١٠٠٪: ★★ إضافيتان."));
        if (pct >= 80 && !claimed80) addClaimButton(c,"استلام ★ لمستوى ٨٠٪",1,"reward_claim_80_"+todayKey);
        else c.addView(detail(claimed80 ? "✓ استلمت مكافأة ٨٠٪" : "لم تصل إلى ٨٠٪ بعد."));
        if (pct >= 100 && !claimed100) addClaimButton(c,"استلام ★★ لإكمال اليوم",2,"reward_claim_100_"+todayKey);
        else if (claimed100) c.addView(detail("✓ استلمت مكافأة إكمال اليوم"));
    }

    private void addClaimButton(LinearLayout parent, String label, int stars, String claimKey) {
        Button b = button(label,GOLD); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46)); lp.setMargins(0,dp(9),0,0); parent.addView(b,lp);
        b.setOnClickListener(v -> { prefs.edit().putInt("reward_stars",prefs.getInt("reward_stars",0)+stars).putBoolean(claimKey,true).apply(); Toast.makeText(this,"أضيفت "+stars+" نجمة",Toast.LENGTH_SHORT).show(); render(); });
    }

    private void addStore(LinearLayout root) {
        TextView h=text("متجر المكافآت",21,NAVY,true);h.setPadding(0,dp(22),0,dp(5));root.addView(h);
        addStoreItem(root,"٣٠ دقيقة ترفيه إضافية","فيديو/لعبة/أنمي فوق وقت الراحة المعتاد.",2,"fun30");
        addStoreItem(root,"ساعة لعبة أو أنمي","استخدمها في وقت مناسب لا يزاحم الصلاة أو النوم.",4,"fun60");
        addStoreItem(root,"ساعة استكشاف حر","طقس، إلكترونيات، جيولوجيا، علم نفس... موضوع تحبه.",3,"explore60");
        addStoreItem(root,"مساء مرن","خفف المهمات الإضافية في مساء واحد واستمتع بالراحة.",7,"flex_evening");
        Button custom=button("＋ أضف مكافأة من اختيارك",PURPLE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));lp.setMargins(0,dp(8),0,0);root.addView(custom,lp);custom.setOnClickListener(v->showCustomRewardDialog());
        JSONArray arr = customRewards();
        for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o!=null)addStoreItem(root,o.optString("title"),o.optString("note"),o.optInt("cost",3),"custom"+i);}
    }

    private void addStoreItem(LinearLayout root,String title,String note,int cost,String id) {
        LinearLayout c=card();c.setPadding(dp(14),dp(11),dp(14),dp(11));add(root,c,6);
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(text(title,15,TEXT,true));info.addView(text(note,12,MUTED,false));line.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Button buy=button("★ "+ar(cost),GOLD);line.addView(buy,new LinearLayout.LayoutParams(dp(82),dp(42)));c.addView(line);
        buy.setOnClickListener(v->{int stars=prefs.getInt("reward_stars",0);if(stars<cost){Toast.makeText(this,"تحتاج نجومًا أكثر",Toast.LENGTH_SHORT).show();return;}prefs.edit().putInt("reward_stars",stars-cost).putLong("reward_used_"+id+"_"+System.currentTimeMillis(),System.currentTimeMillis()).apply();new AlertDialog.Builder(this).setTitle("المكافأة جاهزة").setMessage(title+"\n\nاستخدمها في وقت مناسب ثم ارجع لخطة مساري.").setPositiveButton("تم",null).show();render();});
    }

    private void showCustomRewardDialog() {
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),0,dp(18),0);box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText title=input("اسم المكافأة");EditText note=input("ملاحظة قصيرة");EditText cost=input("سعرها بالنجوم");cost.setInputType(InputType.TYPE_CLASS_NUMBER);box.addView(title);box.addView(note);box.addView(cost);
        new AlertDialog.Builder(this).setTitle("مكافأة جديدة").setView(box).setPositiveButton("حفظ",(d,w)->{String t=title.getText().toString().trim();if(t.isEmpty())return;int c=3;try{c=Math.max(1,Integer.parseInt(cost.getText().toString().trim()));}catch(Exception ignored){}try{JSONArray arr=customRewards();JSONObject o=new JSONObject();o.put("title",t);o.put("note",note.getText().toString().trim());o.put("cost",c);arr.put(o);prefs.edit().putString("reward_custom_store",arr.toString()).apply();render();}catch(Exception ignored){}}).setNegativeButton("إلغاء",null).show();
    }

    private JSONArray customRewards(){try{return new JSONArray(prefs.getString("reward_custom_store","[]"));}catch(Exception e){return new JSONArray();}}

    private void addAchievements(LinearLayout root) {
        TextView h=text("الإنجازات الخاصة",21,NAVY,true);h.setPadding(0,dp(22),0,dp(5));root.addView(h);
        achievement(root,"أول ٥٠٠ نقطة","اجمع ٥٠٠ نقطة في الخطة.",planPoints(),500);
        achievement(root,"٧ أيام قوية","حقق ٨٠٪ أو أكثر في ٧ أيام.",countStrongDays(),7);
        achievement(root,"١٠ جلسات قرآن","أكمل ١٠ جلسات مراجعة قرآن.",countDoneContains("_quran"),10);
        achievement(root,"١٠ جلسات إنجليزي","أكمل ١٠ جلسات إنجليزية.",countDoneEndsWith("_english"),10);
        achievement(root,"٨ تمارين","أكمل ٨ جلسات تمرين A/B.",countDoneContains("workout"),8);
        achievement(root,"٥ جلسات عمل","أكمل ٥ جلسات عمل حقيقية.",countDoneEndsWith("_work"),5);
    }

    private void achievement(LinearLayout root,String title,String note,int value,int target) {
        boolean open=value>=target;LinearLayout c=card();c.setPadding(dp(13),dp(10),dp(13),dp(10));if(!open)c.setAlpha(.63f);add(root,c,5);
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);line.addView(text(open?"★":"☆",24,open?GOLD:MUTED,true),new LinearLayout.LayoutParams(dp(38),ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(text(title,15,open?TEXT:MUTED,true));info.addView(text(note,12,MUTED,false));line.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));line.addView(pill(ar(Math.min(value,target))+"/"+ar(target),open?GREEN:MUTED,Color.rgb(248,250,253)));c.addView(line);
    }

    private void addWeekHistory(LinearLayout root) {
        TextView h = text("آخر ٧ أيام", 21, NAVY, true);
        h.setPadding(0, dp(22), 0, dp(5));
        root.addView(h);

        Calendar d = Calendar.getInstance();
        d.add(Calendar.DAY_OF_MONTH, -6);
        for (int i = 0; i < 7; i++) {
            int value = dayPoints(d);
            int target = dayTarget(d);
            int pct = percent(value, target);

            LinearLayout c = card();
            c.setPadding(dp(13), dp(10), dp(13), dp(10));
            add(root, c, 5);

            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            String label = new SimpleDateFormat("EEEE d/M", new Locale("ar")).format(d.getTime());
            line.addView(text(label, 14, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(pill(ar(pct) + "٪", gradeColor(pct), Color.rgb(248,250,253)));
            c.addView(line);
            c.addView(progress(value, target, gradeColor(pct)));
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void addMonthlyMetrics(LinearLayout root) {
        TextView h = text("مؤشرات هذا الشهر", 21, NAVY, true);
        h.setPadding(0, dp(22), 0, dp(5));
        root.addView(h);

        int days = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        int q1 = monthDone("quran1");
        int q2 = monthDone("quran2");
        int en = monthDone("english");
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
        LinearLayout c = card();
        c.setPadding(dp(13), dp(10), dp(13), dp(10));
        add(root, c, 6);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        line.addView(text(label, 14, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(pill(ar(value) + "/" + ar(target), color, Color.rgb(248,250,253)));
        c.addView(line);
        c.addView(progress(value, target, color));
    }

    private int dayPoints(Calendar c) {
        return prefs.getInt("reward_day_points_" + dateKey(c), 0);
    }

    private int dayTarget(Calendar c) {
        int day = c.get(Calendar.DAY_OF_WEEK);
        int total = 5 + 5 + 7 + 14 + 3 + 8 + 18 + 5 + 5 + 5;
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
        total += customRequiredTarget(day);
        return Math.max(1,total);
    }

    private int customRequiredTarget(int dow) {
        int total = 0;
        try {
            JSONArray arr = new JSONArray(prefs.getString("custom_tasks", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null || !o.optBoolean("active", true) || !o.optBoolean("required", false)) continue;
                int day = o.optInt("day", 0);
                if (day == 0 || day == dow) total += Math.max(1, o.optInt("points", 10));
            }
        } catch (Exception ignored) {}
        return total;
    }

    private int planPoints() {
        Calendar start = Calendar.getInstance();
        start.set(2026, Calendar.SEPTEMBER, 1);
        Calendar end = Calendar.getInstance();
        end.set(2027, Calendar.MAY, 31);
        Calendar now = Calendar.getInstance();
        Calendar last = now.before(end) ? now : end;
        int total = 0;
        while (!start.after(last)) {
            total += dayPoints(start);
            start.add(Calendar.DAY_OF_MONTH, 1);
        }
        return total;
    }

    private int countStrongDays() {
        Calendar d = Calendar.getInstance();
        d.set(2026, Calendar.SEPTEMBER, 1);
        Calendar now = Calendar.getInstance();
        int n = 0;
        while (!d.after(now)) {
            if (percent(dayPoints(d), dayTarget(d)) >= 80) n++;
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
        return n;
    }

    private int countDoneContains(String fragment) {
        int n = 0;
        for (Map.Entry<String,?> e : prefs.getAll().entrySet()) {
            if (e.getKey().startsWith("reward_done_") && e.getKey().contains(fragment) && Boolean.TRUE.equals(e.getValue())) n++;
        }
        return n;
    }

    private int countDoneEndsWith(String suffix) {
        int n = 0;
        for (Map.Entry<String,?> e : prefs.getAll().entrySet()) {
            if (e.getKey().startsWith("reward_done_") && e.getKey().endsWith(suffix) && Boolean.TRUE.equals(e.getValue())) n++;
        }
        return n;
    }

    private int monthDone(String id) {
        Calendar d = Calendar.getInstance();
        d.set(Calendar.DAY_OF_MONTH, 1);
        int month = d.get(Calendar.MONTH);
        int n = 0;
        while (d.get(Calendar.MONTH) == month) {
            if (prefs.getBoolean("reward_done_" + dateKey(d) + "_" + id, false)) n++;
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
        return n;
    }

    private int expectedEnglishThisMonth() {
        Calendar d = Calendar.getInstance();
        d.set(Calendar.DAY_OF_MONTH, 1);
        int month = d.get(Calendar.MONTH);
        int n = 0;
        while (d.get(Calendar.MONTH) == month) {
            if (d.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) n++;
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
        return n;
    }

    private int expectedWorkoutThisMonth() {
        Calendar d = Calendar.getInstance();
        d.set(Calendar.DAY_OF_MONTH, 1);
        int month = d.get(Calendar.MONTH);
        int n = 0;
        while (d.get(Calendar.MONTH) == month) {
            int dow = d.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.SATURDAY || dow == Calendar.TUESDAY) n++;
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
        return n;
    }

    private int expectedWorkThisMonth() {
        Calendar d = Calendar.getInstance();
        d.set(Calendar.DAY_OF_MONTH, 1);
        int month = d.get(Calendar.MONTH);
        int n = 0;
        while (d.get(Calendar.MONTH) == month) {
            if (d.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) n++;
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
        return n;
    }

    private int percent(int v,int t){return Math.min(100,Math.round(v*100f/Math.max(1,t)));}
    private int gradeColor(int pct){return pct>=80?GREEN:(pct>=50?GOLD:RED);}
    private String dateKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    private String ar(int n){return String.valueOf(n).replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤').replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');}
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView detail(String s){TextView t=text(s,12,MUTED,false);t.setPadding(0,dp(5),0,0);return t;}
    private TextView pill(String s,int color,int bg){TextView t=text(s,12,color,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(9),dp(5),dp(9),dp(5));t.setBackground(round(bg,20));return t;}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(color,13));return b;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setGravity(Gravity.RIGHT);e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);e.setPadding(dp(8),dp(6),dp(8),dp(6));return e;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);c.setBackground(round(Color.WHITE,18,BORDER));c.setElevation(dp(1));return c;}
    private void add(LinearLayout r,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(top),0,0);r.addView(v,lp);}
    private ProgressBar progress(int value,int target,int color){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,target));p.setProgress(Math.min(value,Math.max(1,target)));p.setProgressTintList(ColorStateList.valueOf(color));p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(232,236,242)));return p;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable round(int color,int radius,int stroke){GradientDrawable d=round(color,radius);d.setStroke(dp(1),stroke);return d;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
