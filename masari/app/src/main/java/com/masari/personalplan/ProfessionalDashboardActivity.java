package com.masari.personalplan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ProfessionalDashboardActivity extends Activity {
    private static final int BG = Color.rgb(246,248,251);
    private static final int CARD = Color.WHITE;
    private static final int NAVY = Color.rgb(10,42,66);
    private static final int NAVY2 = Color.rgb(16,54,81);
    private static final int GREEN = Color.rgb(22,132,79);
    private static final int GREEN2 = Color.rgb(33,154,91);
    private static final int TEXT = Color.rgb(25,32,42);
    private static final int MUTED = Color.rgb(108,117,130);
    private static final int BORDER = Color.rgb(229,233,239);
    private static final int GOLD = Color.rgb(214,146,35);
    private static final int BLUE = Color.rgb(50,111,198);
    private static final int ORANGE = Color.rgb(225,133,38);
    private static final int RED = Color.rgb(205,81,69);
    private static final int PURPLE = Color.rgb(125,84,181);
    private static final int TEAL = Color.rgb(44,153,165);
    private static final int PINK = Color.rgb(198,88,133);

    private SharedPreferences prefs;
    private String tab = "home";
    private String todayKey;
    private final LinkedHashMap<String,Integer> domainColors = new LinkedHashMap<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
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
        if (prefs != null) { todayKey = dateKey(Calendar.getInstance()); render(); }
    }

    private void setupDomains() {
        domainColors.put("القرآن", GREEN);
        domainColors.put("الإنجليزية والقبول", BLUE);
        domainColors.put("العمل والدخل", ORANGE);
        domainColors.put("الصحة", RED);
        domainColors.put("المعرفة والقراءة", PURPLE);
        domainColors.put("التواصل", TEAL);
        domainColors.put("الأسرة", PINK);
        domainColors.put("الدين والمسجد", Color.rgb(64,126,77));
        domainColors.put("الانضباط", Color.rgb(94,103,116));
    }

    private void render() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        FrameLayout body = new FrameLayout(this);
        shell.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        View screen;
        switch(tab){
            case "tasks": screen = tasksScreen(); break;
            case "stats": screen = statsScreen(); break;
            case "rewards": screen = rewardsScreen(); break;
            case "more": screen = moreScreen(); break;
            default: screen = homeScreen();
        }
        body.addView(screen,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        shell.addView(bottomNav(),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(72)));
        setContentView(shell);
    }

    private View homeScreen() {
        ScrollView s = scroll(); LinearLayout r = root(s);
        addTopBar(r,"مساري","☰","🔔");
        LinearLayout hello = new LinearLayout(this); hello.setOrientation(LinearLayout.HORIZONTAL); hello.setGravity(Gravity.CENTER_VERTICAL); hello.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout ht = new LinearLayout(this); ht.setOrientation(LinearLayout.VERTICAL);
        ht.addView(text("مرحبًا 👋",20,TEXT,true)); ht.addView(text(homeMessage(),12,MUTED,false));
        hello.addView(ht,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView avatar = circleText("م",46,Color.rgb(233,245,238),GREEN,19); hello.addView(avatar);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); hp.setMargins(0,dp(12),0,0); r.addView(hello,hp);

        sectionTitle(r,"تقدم اليوم","");
        LinearLayout progressCard = card(); progressCard.setPadding(dp(14),dp(13),dp(14),dp(13)); add(r,progressCard,8);
        LinearLayout periods = new LinearLayout(this); periods.setOrientation(LinearLayout.HORIZONTAL); periods.setGravity(Gravity.CENTER); periods.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addMiniProgress(periods,"اليوم",dayPercent(Calendar.getInstance()),GREEN);
        addMiniProgress(periods,"الأسبوع",weekPercent(),BLUE);
        addMiniProgress(periods,"الشهر",monthPercent(),PURPLE);
        addMiniProgress(periods,"السنة",planPercent(),GREEN);
        progressCard.addView(periods);
        int todayPct=dayPercent(Calendar.getInstance());
        TextView msg=text(todayPct>=80?"أنت متقدم! استمر على هذا النسق 💪":"ابدأ بالمهمة الحالية وارفع شريط اليوم خطوة خطوة.",12,todayPct>=80?GREEN:MUTED,true); msg.setGravity(Gravity.CENTER); msg.setPadding(0,dp(8),0,0); progressCard.addView(msg);

        LinearLayout goal=card(); goal.setPadding(dp(14),dp(13),dp(14),dp(13)); add(r,goal,10);
        LinearLayout gh=new LinearLayout(this);gh.setOrientation(LinearLayout.HORIZONTAL);gh.setGravity(Gravity.CENTER_VERTICAL);gh.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView targetIcon=circleText("◎",42,Color.rgb(232,247,237),GREEN,22);gh.addView(targetIcon);
        LinearLayout gtx=new LinearLayout(this);gtx.setOrientation(LinearLayout.VERTICAL);gtx.setPadding(dp(10),0,dp(10),0);gtx.addView(text("الهدف اليومي",16,TEXT,true));gtx.addView(text("إنجاز ٨٠٪ من النقاط + ورد القرآن + الإنجليزية",12,MUTED,false));gh.addView(gtx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));goal.addView(gh);
        int doneTasks=countDoneToday(), all=tasksToday().size();
        TextView gs=text(ar(doneTasks)+" / "+ar(all)+" مهام",12,GREEN,true);gs.setPadding(0,dp(8),0,dp(4));goal.addView(gs);goal.addView(progress(dayPoints(Calendar.getInstance()),dayTarget(Calendar.getInstance()),GREEN));

        sectionTitle(r,"جدول اليوم","عرض الكل");
        List<TaskItem> tasks=tasksToday(); int shown=0;
        for(TaskItem t:tasks){ if(t.start<420 || t.id.equals("sleep2") || t.id.equals("lunch")) continue; addCompactTask(r,t); if(++shown>=6)break; }

        LinearLayout streak=card();streak.setPadding(dp(14),dp(12),dp(14),dp(12));add(r,streak,10);
        LinearLayout sr=new LinearLayout(this);sr.setOrientation(LinearLayout.HORIZONTAL);sr.setGravity(Gravity.CENTER_VERTICAL);sr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout stxt=new LinearLayout(this);stxt.setOrientation(LinearLayout.VERTICAL);stxt.addView(text("🔥 سلسلة الإنجاز",16,TEXT,true));stxt.addView(text(ar(streak())+" يومًا متتاليًا فوق ٨٠٪",12,GREEN,true));sr.addView(stxt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView flames=text(flames(streak()),20,GOLD,false);sr.addView(flames);streak.addView(sr);

        LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);quick.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);quick.setPadding(0,dp(12),0,0);
        quick.addView(quickTile("💼","العمل",ORANGE),weight());quick.addView(quickTile("🎧","الإنجليزية",BLUE),weight());quick.addView(quickTile("📖","القرآن",GREEN),weight());quick.addView(quickTile("📚","LeapAhead",PURPLE),weight());r.addView(quick);
        return s;
    }

    private View tasksScreen() {
        ScrollView s=scroll();LinearLayout r=root(s);addTopBar(r,"المهام","⋮","⌕");
        addWeekStrip(r);
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);chips.setPadding(0,dp(10),0,dp(8));
        chips.addView(chip("الكل",GREEN,true)); chips.addView(chip("قرآن",GREEN,false));chips.addView(chip("عمل",ORANGE,false));chips.addView(chip("إنجليزية",BLUE,false));chips.addView(chip("صحة",RED,false));chips.addView(chip("أسرة",TEAL,false));chips.addView(chip("LeapAhead",PURPLE,false));hs.addView(chips);r.addView(hs);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);head.addView(text("مهام اليوم",20,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));head.addView(pill(ar(countDoneToday())+" / "+ar(tasksToday().size())+" منجزة",GREEN,Color.rgb(235,247,240)));r.addView(head);
        for(TaskItem t:tasksToday())addFullTask(r,t);
        Button add=button("＋ إضافة مهمة",GREEN);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52));ap.setMargins(0,dp(16),0,dp(10));r.addView(add,ap);add.setOnClickListener(v->startActivity(new Intent(this,PlannerCenterActivity.class)));
        return s;
    }

    private View statsScreen() {
        ScrollView s=scroll();LinearLayout r=root(s);addTopBar(r,"الإحصائيات","⋮","▣");
        LinearLayout summary=card();summary.setPadding(dp(14),dp(14),dp(14),dp(14));add(r,summary,10);
        LinearLayout sr=new LinearLayout(this);sr.setOrientation(LinearLayout.HORIZONTAL);sr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sr.setGravity(Gravity.CENTER);
        sr.addView(bigStatCircle(dayPercent(Calendar.getInstance()),"نسبة الإنجاز",GREEN),new LinearLayout.LayoutParams(0,dp(140),1));
        LinearLayout nums=new LinearLayout(this);nums.setOrientation(LinearLayout.VERTICAL);nums.setPadding(dp(10),0,dp(10),0);nums.addView(statBox(ar(countMonthDone())+"","مهام مكتملة",GREEN));nums.addView(statBox(ar(monthTaskTotal())+"","إجمالي المهام",NAVY));sr.addView(nums,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));summary.addView(sr);

        sectionTitle(r,"أفضل أسبوع","آخر ٦ أسابيع");LinearLayout bars=card();bars.setPadding(dp(10),dp(12),dp(10),dp(8));add(r,bars,7);bars.addView(new WeeklyBars(this),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(190)));
        sectionTitle(r,"اتجاه الإنجاز الشهري","");LinearLayout line=card();line.setPadding(dp(10),dp(12),dp(10),dp(8));add(r,line,7);line.addView(new TrendChart(this),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(190)));

        LinearLayout duo=new LinearLayout(this);duo.setOrientation(LinearLayout.HORIZONTAL);duo.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);duo.setPadding(0,dp(10),0,0);
        LinearLayout dist=card();dist.setPadding(dp(10),dp(10),dp(10),dp(10));dist.addView(text("توزيع الإنجاز",15,TEXT,true));dist.addView(new DonutView(this),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(155)));duo.addView(dist,weight());
        LinearLayout blocker=card();blocker.setPadding(dp(11),dp(11),dp(11),dp(11));blocker.addView(text("أكثر سبب تعطيل",15,TEXT,true));blocker.addView(circleText("📱",58,Color.rgb(246,239,252),PURPLE,24));blocker.addView(centerText(topBlocker(),12,TEXT,true));blocker.addView(centerText("اضغط الأسبوع ← العوائق للتفاصيل",10,MUTED,false));duo.addView(blocker,weight());r.addView(duo);

        LinearLayout grid1=new LinearLayout(this);grid1.setOrientation(LinearLayout.HORIZONTAL);grid1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);grid1.setPadding(0,dp(10),0,0);grid1.addView(metric("★",ar(totalPoints()),"النقاط المكتسبة",GOLD),weight());grid1.addView(metric("◷",ar(focusHours())+" س","ساعات التركيز",BLUE),weight());grid1.addView(metric("🔥",ar(streak()),"سلسلة الإنجاز",ORANGE),weight());r.addView(grid1);
        LinearLayout grid2=new LinearLayout(this);grid2.setOrientation(LinearLayout.HORIZONTAL);grid2.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);grid2.setPadding(0,dp(7),0,0);grid2.addView(metric("✓",ar(monthRate())+"٪","معدل الالتزام",GREEN),weight());grid2.addView(metric("📚",ar(monthDone("leap1")+monthDone("leap2")+monthDone("leap3")),"الكتب المنجزة",PURPLE),weight());grid2.addView(metric("📖",ar((monthDone("quran1")+monthDone("quran2"))*2),"جلسات القرآن",GREEN),weight());r.addView(grid2);
        return s;
    }

    private View rewardsScreen() {
        ScrollView s=scroll();LinearLayout r=root(s);addTopBar(r,"المكافآت","🎁","⋮");
        int total=totalPoints(),level=1+total/500;
        LinearLayout hero=card();hero.setPadding(dp(17),dp(16),dp(17),dp(16));hero.setBackground(round(NAVY,20));add(r,hero,10);
        LinearLayout hh=new LinearLayout(this);hh.setOrientation(LinearLayout.HORIZONTAL);hh.setGravity(Gravity.CENTER_VERTICAL);hh.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout htxt=new LinearLayout(this);htxt.setOrientation(LinearLayout.VERTICAL);htxt.addView(text("إجمالي النقاط",12,Color.rgb(193,211,224),false));htxt.addView(text(ar(total),28,Color.WHITE,true));hh.addView(htxt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView medal=circleText("★",60,Color.rgb(235,164,41),Color.WHITE,28);hh.addView(medal);hero.addView(hh);hero.addView(text("المستوى "+ar(level)+" • "+levelName(level),14,Color.WHITE,true));hero.addView(progress(total%500,500,GREEN2));

        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);tabs.setPadding(0,dp(10),0,dp(6));tabs.addView(chip("المستوى",GREEN,true),weight());tabs.addView(chip("الأوسمة",GREEN,false),weight());tabs.addView(chip("إنجازات",GREEN,false),weight());tabs.addView(chip("متجر المكافآت",GREEN,false),weight());r.addView(tabs);
        LinearLayout lvl=card();lvl.setPadding(dp(14),dp(12),dp(14),dp(12));add(r,lvl,5);lvl.addView(text("المستوى الحالي",17,TEXT,true));lvl.addView(text(levelName(level),22,NAVY,true));lvl.addView(text("أنت في أفضل "+ar(Math.max(5,100-level*6))+"٪ من مسارك الشخصي هذا العام",12,MUTED,false));

        sectionTitle(r,"الأوسمة","عرض الكل");LinearLayout badgeRow=new LinearLayout(this);badgeRow.setOrientation(LinearLayout.HORIZONTAL);badgeRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);badgeRow.addView(badge("📖","القرآن",GREEN),weight());badgeRow.addView(badge("🎧","الإنجليزية",BLUE),weight());badgeRow.addView(badge("💼","العمل",ORANGE),weight());badgeRow.addView(badge("♥","الصحة",RED),weight());badgeRow.addView(badge("✦","الانضباط",PURPLE),weight());r.addView(badgeRow);
        sectionTitle(r,"إنجازات حديثة","عرض الكل");addAchievement(r,"قارئ متميز","أكملت "+ar(monthDone("leap1")+monthDone("leap2")+monthDone("leap3"))+" كتابًا هذا الشهر",200,GREEN);addAchievement(r,"أسبوع ذهبي","أفضل نسبة أسبوعية: "+ar(bestWeekPercent())+"٪",300,GOLD);addAchievement(r,"مستمر لا يتوقف","سلسلة الإنجاز الحالية: "+ar(streak())+" أيام",250,ORANGE);
        sectionTitle(r,"متجر المكافآت","عرض الكل");LinearLayout shop=new LinearLayout(this);shop.setOrientation(LinearLayout.HORIZONTAL);shop.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);shop.addView(shopItem("📘","كتاب ورقي",1000),weight());shop.addView(shopItem("🎧","سماعة بلوتوث",2000),weight());shop.addView(shopItem("⌚","ساعة ذكية",3000),weight());r.addView(shop);
        Button open=button("فتح مركز المكافآت الكامل",GREEN);LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(50));op.setMargins(0,dp(12),0,0);r.addView(open,op);open.setOnClickListener(v->startActivity(new Intent(this,RewardCenterActivity.class)));
        return s;
    }

    private View moreScreen() {
        ScrollView s=scroll();LinearLayout r=root(s);addTopBar(r,"المزيد","","⚙");
        LinearLayout profile=card();profile.setPadding(dp(15),dp(14),dp(15),dp(14));add(r,profile,10);profile.addView(text("مساري الشخصي",20,NAVY,true));profile.addView(text("نظامك السنوي للقرآن والإنجليزية والعمل والصحة والتعلم",12,MUTED,false));
        addMenu(r,"🗓","التقويم الأسبوعي","عدّل الأيام والمواعيد وشاهد الأسبوع كاملًا",GREEN,()->startActivity(new Intent(this,WeeklyPlannerActivity.class)));
        addMenu(r,"📊","مركز المتابعة","مراجعة أسبوعية وشهرية وسجل ٣٠ يومًا",BLUE,()->startActivity(new Intent(this,PlannerCenterActivity.class)));
        addMenu(r,"⚠","العوائق والتأجيل","سجل أسباب التأجيل والتعثر واعرف أكثر ما يعطلك",ORANGE,()->startActivity(new Intent(this,WeeklyPlannerActivity.class)));
        addMenu(r,"🔔","الإشعارات","تذكيرات قبل الإنجليزية والعمل والقرآن وإغلاق اليوم",PURPLE,()->startActivity(new Intent(this,WeeklyPlannerActivity.class)));
        addMenu(r,"＋","المهام المخصصة","أضف مهمة جديدة بوقت ومجال ونقاط",TEAL,()->startActivity(new Intent(this,PlannerCenterActivity.class)));
        addMenu(r,"💾","النسخة الاحتياطية","صدّر أو استعد بياناتك كاملة",RED,()->startActivity(new Intent(this,PlannerCenterActivity.class)));
        addMenu(r,"★","مركز المكافآت","المتجر والنجوم والإنجازات",GOLD,()->startActivity(new Intent(this,RewardCenterActivity.class)));
        TextView version=centerText("مساري v0.9.0 • البيانات محفوظة محليًا",11,MUTED,false);version.setPadding(0,dp(18),0,dp(4));r.addView(version);
        return s;
    }

    private LinearLayout bottomNav(){
        LinearLayout n=new LinearLayout(this);n.setOrientation(LinearLayout.HORIZONTAL);n.setGravity(Gravity.CENTER);n.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);n.setBackgroundColor(NAVY);n.setPadding(dp(4),dp(5),dp(4),dp(5));
        addNav(n,"⌂","الرئيسية","home");addNav(n,"☑","المهام","tasks");addNav(n,"▥","الإحصائيات","stats");addNav(n,"♜","المكافآت","rewards");addNav(n,"•••","المزيد","more");return n;
    }
    private void addNav(LinearLayout n,String icon,String label,String key){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);if(tab.equals(key))x.setBackground(round(GREEN,14));TextView i=centerText(icon,20,Color.WHITE,true);TextView l=centerText(label,10,Color.WHITE,tab.equals(key));x.addView(i);x.addView(l);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1);lp.setMargins(dp(2),0,dp(2),0);n.addView(x,lp);x.setOnClickListener(v->{tab=key;render();});}

    private void addTopBar(LinearLayout r,String title,String left,String right){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);TextView a=centerText(left,20,NAVY,true);row.addView(a,new LinearLayout.LayoutParams(dp(44),dp(44)));TextView t=centerText(title,22,title.equals("مساري")?GREEN:TEXT,true);row.addView(t,new LinearLayout.LayoutParams(0,dp(44),1));TextView b=centerText(right,19,NAVY,true);row.addView(b,new LinearLayout.LayoutParams(dp(44),dp(44)));r.addView(row);}
    private void sectionTitle(LinearLayout r,String title,String action){LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.HORIZONTAL);h.setGravity(Gravity.CENTER_VERTICAL);h.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);h.setPadding(0,dp(16),0,dp(5));h.addView(text(title,17,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));if(!action.isEmpty())h.addView(text(action,11,BLUE,false));r.addView(h);}

    private void addMiniProgress(LinearLayout p,String label,int pct,int color){LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.VERTICAL);w.setGravity(Gravity.CENTER);w.addView(new RingView(this,pct,color),new LinearLayout.LayoutParams(dp(67),dp(67)));w.addView(centerText(label,10,MUTED,false));p.addView(w,new LinearLayout.LayoutParams(0,dp(90),1));}
    private void addCompactTask(LinearLayout r,TaskItem t){LinearLayout c=card();c.setPadding(dp(10),dp(9),dp(10),dp(9));add(r,c,5);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);CheckBox cb=new CheckBox(this);cb.setChecked(done(t));cb.setButtonTintList(ColorStateList.valueOf(color(t.domain)));row.addView(cb,new LinearLayout.LayoutParams(dp(40),dp(42)));TextView ic=circleText(t.icon,36,soft(color(t.domain)),color(t.domain),16);row.addView(ic);LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,dp(9),0);tx.addView(text(t.title,13,TEXT,true));tx.addView(text(time(t.start)+" • "+shortDomain(t.domain),10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));c.addView(row);cb.setOnCheckedChangeListener((v,ch)->{setDone(t,ch);render();});}
    private void addFullTask(LinearLayout r,TaskItem t){LinearLayout c=card();c.setPadding(dp(11),dp(10),dp(11),dp(10));add(r,c,6);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);CheckBox cb=new CheckBox(this);cb.setChecked(done(t));cb.setButtonTintList(ColorStateList.valueOf(color(t.domain)));row.addView(cb,new LinearLayout.LayoutParams(dp(44),dp(50)));TextView ic=circleText(t.icon,40,soft(color(t.domain)),color(t.domain),18);row.addView(ic);LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,dp(9),0);tx.addView(text(t.title,14,TEXT,true));tx.addView(text(time(t.start)+" - "+time(t.end)+" • "+shortDomain(t.domain),10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));row.addView(pill("+"+ar(t.points),color(t.domain),soft(color(t.domain))));c.addView(row);cb.setOnCheckedChangeListener((v,ch)->{setDone(t,ch);render();});}
    private void addWeekStrip(LinearLayout r){Calendar start=saturdayStart(Calendar.getInstance());LinearLayout strip=new LinearLayout(this);strip.setOrientation(LinearLayout.HORIZONTAL);strip.setGravity(Gravity.CENTER);strip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);for(int i=0;i<7;i++){Calendar d=(Calendar)start.clone();d.add(Calendar.DAY_OF_MONTH,i);boolean now=dateKey(d).equals(todayKey);LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setPadding(dp(5),dp(6),dp(5),dp(6));if(now)x.setBackground(round(GREEN,14));String day=new SimpleDateFormat("EEE",new Locale("ar")).format(d.getTime()).replace("،","");x.addView(centerText(day,9,now?Color.WHITE:MUTED,false));x.addView(centerText(ar(d.get(Calendar.DAY_OF_MONTH)),13,now?Color.WHITE:TEXT,true));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),1);lp.setMargins(dp(2),0,dp(2),0);strip.addView(x,lp);}LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(56));sp.setMargins(0,dp(10),0,0);r.addView(strip,sp);}

    private View quickTile(String icon,String label,int c){LinearLayout x=card();x.setGravity(Gravity.CENTER);x.setPadding(dp(5),dp(9),dp(5),dp(9));x.addView(circleText(icon,36,soft(c),c,16));x.addView(centerText(label,10,TEXT,true));return x;}
    private View chip(String s,int c,boolean active){TextView t=centerText(s,11,active?Color.WHITE:c,true);t.setPadding(dp(12),dp(7),dp(12),dp(7));t.setBackground(round(active?c:soft(c),14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(34));lp.setMargins(dp(3),0,dp(3),0);t.setLayoutParams(lp);return t;}
    private View bigStatCircle(int pct,String label,int c){LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.VERTICAL);w.setGravity(Gravity.CENTER);w.addView(new RingView(this,pct,c),new LinearLayout.LayoutParams(dp(100),dp(100)));w.addView(centerText(label,11,MUTED,true));return w;}
    private View statBox(String value,String label,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(10),dp(7),dp(10),dp(7));x.addView(text(value,19,c,true));x.addView(text(label,10,MUTED,false));return x;}
    private View metric(String icon,String value,String label,int c){LinearLayout x=card();x.setGravity(Gravity.CENTER);x.setPadding(dp(7),dp(10),dp(7),dp(10));x.addView(centerText(icon,18,c,true));x.addView(centerText(value,16,TEXT,true));x.addView(centerText(label,9,MUTED,false));return x;}
    private View badge(String icon,String label,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.addView(circleText(icon,46,soft(c),c,19));x.addView(centerText(label,9,TEXT,true));return x;}
    private void addAchievement(LinearLayout r,String title,String desc,int pts,int c){LinearLayout x=card();x.setPadding(dp(11),dp(9),dp(11),dp(9));add(r,x,5);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.addView(circleText("🏅",40,soft(c),c,17));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,dp(9),0);tx.addView(text(title,13,TEXT,true));tx.addView(text(desc,10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));row.addView(text("+"+ar(pts),11,GREEN,true));x.addView(row);}
    private View shopItem(String icon,String label,int pts){LinearLayout x=card();x.setGravity(Gravity.CENTER);x.setPadding(dp(6),dp(10),dp(6),dp(10));x.addView(centerText(icon,28,TEXT,false));x.addView(centerText(label,9,TEXT,true));x.addView(centerText("★ "+ar(pts),10,GREEN,true));return x;}
    private void addMenu(LinearLayout r,String icon,String title,String sub,int c,Runnable run){LinearLayout x=card();x.setPadding(dp(12),dp(11),dp(12),dp(11));add(r,x,7);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.addView(circleText(icon,42,soft(c),c,18));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,dp(10),0);tx.addView(text(title,14,TEXT,true));tx.addView(text(sub,10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));row.addView(text("‹",24,MUTED,false));x.addView(row);x.setOnClickListener(v->run.run());}

    private List<TaskItem> tasksToday(){return tasksFor(Calendar.getInstance());}
    private List<TaskItem> tasksFor(Calendar d){List<TaskItem>a=new ArrayList<>();int day=d.get(Calendar.DAY_OF_WEEK);a.add(t("fajr",240,390,"الفجر والحلقة والدرس","الدين والمسجد",5,true,"☾"));if(day==Calendar.SATURDAY)a.add(t("workoutA",390,420,"تمرين A — كتف وذراعان + جسم كامل","الصحة",20,true,"♥"));else if(day==Calendar.TUESDAY)a.add(t("workoutB",390,420,"تمرين B — أوتار وقبضة وسرعة","الصحة",20,true,"♥"));a.add(t("english",420,480,englishTitle(day),"الإنجليزية والقبول",day==Calendar.FRIDAY?10:25,day!=Calendar.FRIDAY,"🎧"));a.add(t("sleep2",480,570,"النوم التكميلي","الصحة",5,true,"☁"));a.add(t("leap1",570,600,"LeapAhead — الكتاب ١","المعرفة والقراءة",7,true,"📚"));a.add(t("work",600,690,workTitle(day),"العمل والدخل",day==Calendar.FRIDAY?10:25,day!=Calendar.FRIDAY,"💼"));a.add(t("quran1",705,780,"القرآن — مراجعة جديدة: صفحتان","القرآن",14,true,"📖"));a.add(t("lunch",780,810,"الغداء","الانضباط",3,true,"☕"));a.add(t("leap2",810,855,"LeapAhead — الكتاب ٢","المعرفة والقراءة",8,true,"📚"));a.add(t("quran2",885,970,"القرآن — صفحتان جديدة + مراجعة قديمة","القرآن",18,true,"📖"));a.add(afternoon(day));a.add(t("maghrib",1080,1200,"المغرب والتحفيظ والعشاء","الدين والمسجد",5,true,"☾"));a.add(t("leap3",1230,1275,"LeapAhead — الكتاب ٣","المعرفة والقراءة",7,false,"📚"));a.add(t("close",1320,1340,"إغلاق اليوم","الانضباط",5,true,"✓"));a.add(t("sleep",1340,1350,"الاستعداد للنوم","الصحة",5,true,"☁"));addCustom(a,d);return a;}
    private TaskItem afternoon(int day){switch(day){case Calendar.SATURDAY:return t("family_talk",990,1020,"الأسرة + تدريب كلام قصير","الأسرة",8,true,"👥");case Calendar.SUNDAY:return t("talk1",990,1020,"تدريب التواصل","التواصل",10,true,"◉");case Calendar.MONDAY:return t("family_friend",990,1020,"خدمة الأسرة + تفقد صديق","الأسرة",8,true,"👥");case Calendar.TUESDAY:return t("talk2",990,1020,"تدريب الكلام والحزم","التواصل",10,true,"◉");case Calendar.WEDNESDAY:return t("medicine",990,1050,"مراجعة طب قديم","المعرفة والقراءة",12,true,"⚕");case Calendar.THURSDAY:return t("khatera",990,1030,"خاطرة دينية + تدريب إلقاء","التواصل",12,true,"◉");default:return t("explore",990,1050,"استكشاف علمي أو مهارة حياة","المعرفة والقراءة",10,false,"✦");}}
    private void addCustom(List<TaskItem>a,Calendar d){try{JSONArray arr=new JSONArray(prefs.getString("custom_tasks","[]"));for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null||!o.optBoolean("active",true))continue;int day=o.optInt("day",0);if(day!=0&&day!=d.get(Calendar.DAY_OF_WEEK))continue;a.add(t(o.optString("id","custom_"+i),o.optInt("start",960),o.optInt("end",990),o.optString("title","مهمة مخصصة"),o.optString("domain","الانضباط"),o.optInt("points",10),o.optBoolean("required",false),"✦"));}}catch(Exception ignored){}}
    private TaskItem t(String id,int s,int e,String title,String domain,int pts,boolean req,String icon){return new TaskItem(id,s,e,title,domain,pts,req,icon);}
    private String englishTitle(int d){switch(d){case Calendar.SATURDAY:return"الإنجليزية: Vocabulary + Reading Explorer";case Calendar.SUNDAY:return"الإنجليزية: Vocabulary + Tactics";case Calendar.MONDAY:return"الإنجليزية: Vocabulary + Reading Explorer";case Calendar.TUESDAY:return"الإنجليزية: Vocabulary + Tactics";case Calendar.WEDNESDAY:return"الإنجليزية: Vocabulary + Oxford Bookworms";case Calendar.THURSDAY:return"اختبار الإنجليزية الأسبوعي";default:return"استماع إنجليزي ممتع — يوم خفيف";}}
    private String workTitle(int d){switch(d){case Calendar.SATURDAY:case Calendar.SUNDAY:return"العمل: تطوير الأكاديمية";case Calendar.MONDAY:case Calendar.TUESDAY:return"العمل: الوصول للسوق";case Calendar.WEDNESDAY:return"العمل: دخل مباشر";case Calendar.THURSDAY:return"العمل: مراجعة الأرقام";default:return"مراجعة مالية خفيفة";}}

    private boolean done(TaskItem t){return prefs.getBoolean("reward_done_"+todayKey+"_"+t.id,false);}
    private void setDone(TaskItem t,boolean val){boolean old=done(t);if(old==val)return;int delta=val?t.points:-t.points;String dk="reward_day_points_"+todayKey;String dom="reward_domain_"+t.domain;prefs.edit().putBoolean("reward_done_"+todayKey+"_"+t.id,val).putInt(dk,Math.max(0,prefs.getInt(dk,0)+delta)).putInt(dom,Math.max(0,prefs.getInt(dom,0)+delta)).apply();Toast.makeText(this,val?"+"+t.points+" نقطة":"تم التراجع",Toast.LENGTH_SHORT).show();}
    private int countDoneToday(){int n=0;for(TaskItem t:tasksToday())if(done(t))n++;return n;}
    private int dayPoints(Calendar c){return prefs.getInt("reward_day_points_"+dateKey(c),0);}
    private int dayTarget(Calendar c){int n=0;for(TaskItem t:tasksFor(c))if(t.required)n+=t.points;return Math.max(1,n);}
    private int dayPercent(Calendar c){return pct(dayPoints(c),dayTarget(c));}
    private int weekPercent(){Calendar d=saturdayStart(Calendar.getInstance());int p=0,t=0;for(int i=0;i<7;i++){p+=dayPoints(d);t+=dayTarget(d);d.add(Calendar.DAY_OF_MONTH,1);}return pct(p,t);}
    private int monthPercent(){Calendar d=Calendar.getInstance();d.set(Calendar.DAY_OF_MONTH,1);int m=d.get(Calendar.MONTH),p=0,t=0;while(d.get(Calendar.MONTH)==m){p+=dayPoints(d);t+=dayTarget(d);d.add(Calendar.DAY_OF_MONTH,1);}return pct(p,t);}
    private int planPercent(){Calendar s=Calendar.getInstance();s.set(2026,Calendar.SEPTEMBER,1);Calendar e=Calendar.getInstance();e.set(2027,Calendar.MAY,31);Calendar d=(Calendar)s.clone();int p=0,t=0;while(!d.after(e)){p+=dayPoints(d);t+=dayTarget(d);d.add(Calendar.DAY_OF_MONTH,1);}return pct(p,t);}
    private int streak(){Calendar d=Calendar.getInstance();int n=0;for(int i=0;i<90;i++){if(dayPercent(d)>=80)n++;else if(i>0)break;d.add(Calendar.DAY_OF_MONTH,-1);}return n;}
    private String flames(int n){StringBuilder b=new StringBuilder();for(int i=0;i<Math.min(8,Math.max(1,n));i++)b.append("🔥 ");return b.toString().trim();}
    private int totalPoints(){int t=0;for(String d:domainColors.keySet())t+=prefs.getInt("reward_domain_"+d,0);return t;}
    private int countMonthDone(){Calendar d=Calendar.getInstance();d.set(Calendar.DAY_OF_MONTH,1);int m=d.get(Calendar.MONTH),n=0;while(d.get(Calendar.MONTH)==m){String k=dateKey(d);for(TaskItem t:tasksFor(d))if(prefs.getBoolean("reward_done_"+k+"_"+t.id,false))n++;d.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int monthTaskTotal(){Calendar d=Calendar.getInstance();d.set(Calendar.DAY_OF_MONTH,1);int m=d.get(Calendar.MONTH),n=0;while(d.get(Calendar.MONTH)==m){n+=tasksFor(d).size();d.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int monthDone(String id){Calendar d=Calendar.getInstance();d.set(Calendar.DAY_OF_MONTH,1);int m=d.get(Calendar.MONTH),n=0;while(d.get(Calendar.MONTH)==m){if(prefs.getBoolean("reward_done_"+dateKey(d)+"_"+id,false))n++;d.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int monthRate(){return pct(countMonthDone(),monthTaskTotal());}
    private int focusHours(){int mins=(monthDone("english")*60)+(monthDone("work")*90)+(monthDone("quran1")*35)+(monthDone("quran2")*45);return Math.round(mins/60f);}
    private int bestWeekPercent(){Calendar d=Calendar.getInstance();int best=0;for(int w=0;w<6;w++){Calendar s=saturdayStart(d);int p=0,t=0;for(int i=0;i<7;i++){p+=dayPoints(s);t+=dayTarget(s);s.add(Calendar.DAY_OF_MONTH,1);}best=Math.max(best,pct(p,t));d.add(Calendar.DAY_OF_MONTH,-7);}return best;}
    private String topBlocker(){String[] keys={"يوتيوب / تصفح","فكرة جديدة","نقص نوم","طارئ","ضيق وقت"};int[] c=new int[keys.length];for(String k:prefs.getAll().keySet()){if(!k.startsWith("barrier_")&&!k.startsWith("task_status_"))continue;String v=String.valueOf(prefs.getAll().get(k));for(int i=0;i<keys.length;i++)if(v.contains(keys[i]))c[i]++;}int bi=0;for(int i=1;i<c.length;i++)if(c[i]>c[bi])bi=i;return c[bi]==0?"لا توجد بيانات كافية":keys[bi];}
    private String homeMessage(){int p=dayPercent(Calendar.getInstance());if(p>=100)return"أنهيت هدف اليوم؛ الباقي مكافأة.";if(p>=80)return"يوم قوي — حافظ على الإغلاق الجيد.";if(p>=50)return"قطعت أكثر من نصف الطريق اليوم.";return"لديك مساحة كبيرة لتصنع يومًا جيدًا.";}
    private String levelName(int l){if(l>=12)return"مستوى المتقن";if(l>=8)return"مستوى المنجز";if(l>=5)return"مستوى الثابت";if(l>=3)return"مستوى المتقدم";return"مستوى البداية";}

    private ScrollView scroll(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);s.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(16),dp(12),dp(16),dp(28));r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return r;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);c.setBackground(round(CARD,18,BORDER));c.setElevation(dp(1));return c;}
    private void add(LinearLayout r,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(top),0,0);r.addView(v,lp);}
    private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView centerText(String s,int sp,int c,boolean bold){TextView t=text(s,sp,c,bold);t.setGravity(Gravity.CENTER);return t;}
    private TextView circleText(String s,int size,int bg,int fg,int sp){TextView t=centerText(s,sp,fg,true);t.setBackground(round(bg,999));t.setMinWidth(dp(size));t.setMinHeight(dp(size));return t;}
    private TextView pill(String s,int fg,int bg){TextView t=centerText(s,10,fg,true);t.setPadding(dp(8),dp(4),dp(8),dp(4));t.setBackground(round(bg,12));return t;}
    private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(c,16));return b;}
    private ProgressBar progress(int v,int target,int c){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,target));p.setProgress(Math.min(v,Math.max(1,target)));p.setProgressTintList(ColorStateList.valueOf(c));p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(232,236,241)));return p;}
    private GradientDrawable round(int c,int rad){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(rad));return d;}
    private GradientDrawable round(int c,int rad,int stroke){GradientDrawable d=round(c,rad);d.setStroke(dp(1),stroke);return d;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1);lp.setMargins(dp(3),0,dp(3),0);return lp;}
    private int color(String d){Integer c=domainColors.get(d);return c==null?GREEN:c;}
    private int soft(int c){int r=(Color.red(c)+255*5)/6,g=(Color.green(c)+255*5)/6,b=(Color.blue(c)+255*5)/6;return Color.rgb(r,g,b);}
    private String shortDomain(String d){if(d.contains("الإنجليزية"))return"إنجليزية";if(d.contains("العمل"))return"عمل";if(d.contains("المعرفة"))return"قراءة";if(d.contains("الدين"))return"مسجد";return d;}
    private int pct(int a,int b){return Math.min(100,Math.round(a*100f/Math.max(1,b)));}
    private String ar(int n){return String.valueOf(n).replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤').replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');}
    private String dateKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    private Calendar saturdayStart(Calendar b){Calendar c=(Calendar)b.clone();int x=(c.get(Calendar.DAY_OF_WEEK)-Calendar.SATURDAY+7)%7;c.add(Calendar.DAY_OF_MONTH,-x);return c;}
    private String time(int min){int h=(min/60)%24,m=min%60;String p=h<12?"ص":"م";int hh=h%12;if(hh==0)hh=12;return ar(hh)+":"+(m<10?"٠":"")+ar(m)+" "+p;}

    private static class TaskItem{String id,title,domain,icon;int start,end,points;boolean required;TaskItem(String i,int s,int e,String t,String d,int p,boolean r,String ic){id=i;start=s;end=e;title=t;domain=d;points=p;required=r;icon=ic;}}

    private static class RingView extends View {Paint p=new Paint(1);int pct,c;RingView(Activity a,int v,int color){super(a);pct=v;c=color;}@Override protected void onDraw(Canvas x){super.onDraw(x);float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2,r=Math.min(w,h)*.34f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.min(w,h)*.10f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(232,236,241));x.drawCircle(cx,cy,r,p);p.setColor(c);x.drawArc(new RectF(cx-r,cy-r,cx+r,cy+r),-90,360*pct/100f,false,p);p.setStyle(Paint.Style.FILL);p.setColor(TEXT);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(Math.min(w,h)*.20f);x.drawText(String.valueOf(pct)+"%",cx,cy+p.getTextSize()*.35f,p);}}

    private class WeeklyBars extends View {Paint p=new Paint(1);WeeklyBars(Activity a){super(a);} @Override protected void onDraw(Canvas c){super.onDraw(c);int[] vals=new int[6];Calendar d=Calendar.getInstance();for(int w=5;w>=0;w--){Calendar s=saturdayStart(d);int a=0,b=0;for(int i=0;i<7;i++){a+=dayPoints(s);b+=dayTarget(s);s.add(Calendar.DAY_OF_MONTH,1);}vals[w]=pct(a,b);d.add(Calendar.DAY_OF_MONTH,-7);}float W=getWidth(),H=getHeight(),base=H-30,gap=W/7,bw=gap*.55f;p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT);for(int i=0;i<6;i++){float x=gap*(i+1),bh=(base-25)*vals[i]/100f;p.setColor(i==bestIndex(vals)?GREEN:Color.rgb(231,235,241));c.drawRoundRect(new RectF(x-bw/2,base-bh,x+bw/2,base),8,8,p);p.setColor(MUTED);p.setTextSize(22);c.drawText(String.valueOf(vals[i])+"%",x,base-bh-6,p);p.setTextSize(18);c.drawText("أ"+(i+1),x,H-7,p);}}private int bestIndex(int[]a){int b=0;for(int i=1;i<a.length;i++)if(a[i]>a[b])b=i;return b;}}

    private class TrendChart extends View {Paint p=new Paint(1);TrendChart(Activity a){super(a);} @Override protected void onDraw(Canvas c){super.onDraw(c);int days=Math.min(29,Calendar.getInstance().get(Calendar.DAY_OF_MONTH));if(days<2)days=2;float W=getWidth(),H=getHeight(),left=20,top=20,bottom=H-25;Path path=new Path();for(int i=0;i<days;i++){Calendar d=Calendar.getInstance();d.add(Calendar.DAY_OF_MONTH,-days+1+i);float x=left+(W-left-10)*i/(days-1f),y=bottom-(bottom-top)*dayPercent(d)/100f;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(GREEN);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);for(int i=0;i<days;i+=Math.max(1,days/6)){Calendar d=Calendar.getInstance();d.add(Calendar.DAY_OF_MONTH,-days+1+i);float x=left+(W-left-10)*i/(days-1f),y=bottom-(bottom-top)*dayPercent(d)/100f;c.drawCircle(x,y,5,p);}p.setColor(MUTED);p.setTextSize(18);p.setTextAlign(Paint.Align.CENTER);c.drawText("بداية",left,bottom+19,p);c.drawText("اليوم",W-15,bottom+19,p);}}

    private class DonutView extends View {Paint p=new Paint(1);DonutView(Activity a){super(a);} @Override protected void onDraw(Canvas c){super.onDraw(c);float W=getWidth(),H=getHeight(),cx=W/2,cy=H/2,r=Math.min(W,H)*.31f;int[] cols={GREEN,ORANGE,BLUE,RED,PURPLE,TEAL};String[] ds={"القرآن","العمل والدخل","الإنجليزية والقبول","الصحة","المعرفة والقراءة","التواصل"};int sum=0;int[]v=new int[ds.length];for(int i=0;i<ds.length;i++){v[i]=Math.max(1,prefs.getInt("reward_domain_"+ds[i],0));sum+=v[i];}float st=-90;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(24);for(int i=0;i<v.length;i++){float sw=360f*v[i]/sum;p.setColor(cols[i]);c.drawArc(new RectF(cx-r,cy-r,cx+r,cy+r),st,sw-2,false,p);st+=sw;}p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setColor(TEXT);p.setTextSize(28);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(ar(totalPoints()),cx,cy+8,p);p.setTextSize(17);p.setColor(MUTED);c.drawText("نقطة",cx,cy+31,p);}}
}
