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

public class MasariV14Activity extends Activity {
    private static final int BG = Color.rgb(248, 250, 252);
    private static final int CARD = Color.WHITE;
    private static final int NAVY = Color.rgb(7, 45, 65);
    private static final int NAVY_2 = Color.rgb(12, 57, 80);
    private static final int GREEN = Color.rgb(23, 145, 82);
    private static final int GREEN_DARK = Color.rgb(17, 117, 67);
    private static final int TEXT = Color.rgb(27, 35, 43);
    private static final int MUTED = Color.rgb(112, 121, 131);
    private static final int BORDER = Color.rgb(229, 233, 237);
    private static final int TRACK = Color.rgb(238, 241, 244);
    private static final int BLUE = Color.rgb(72, 130, 205);
    private static final int ORANGE = Color.rgb(231, 148, 64);
    private static final int GOLD = Color.rgb(224, 166, 43);
    private static final int PURPLE = Color.rgb(136, 95, 189);
    private static final int RED = Color.rgb(207, 92, 82);
    private static final int TEAL = Color.rgb(55, 155, 158);
    private static final int PINK = Color.rgb(198, 101, 137);

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
            this.id=id; this.start=start; this.end=end; this.title=title; this.domain=domain;
            this.points=points; this.required=required; this.icon=icon;
        }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs=getSharedPreferences("masari_data", MODE_PRIVATE);
        todayKey=dateKey(Calendar.getInstance());
        setupDomains();
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs != null) {
            todayKey=dateKey(Calendar.getInstance());
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
        domainColors.put("الدين والمسجد", Color.rgb(68, 132, 83));
        domainColors.put("الانضباط", Color.rgb(105, 116, 129));
    }

    private void render() {
        LinearLayout shell=new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        shell.setBackgroundColor(BG);
        FrameLayout body=new FrameLayout(this);
        shell.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        View page;
        switch(tab){
            case "tasks": page=tasksPage(); break;
            case "stats": page=statsPage(); break;
            case "rewards": page=rewardsPage(); break;
            case "more": page=morePage(); break;
            default: page=homePage();
        }
        body.addView(page,new FrameLayout.LayoutParams(-1,-1));
        shell.addView(bottomNav(),new LinearLayout.LayoutParams(-1,dp(74)));
        setContentView(shell);
    }

    private View homePage() {
        ScrollView s=scroll(); LinearLayout r=root(s);
        r.addView(topBar("مساري","bell","menu"));
        LinearLayout hello=new LinearLayout(this);
        hello.setOrientation(LinearLayout.HORIZONTAL);
        hello.setGravity(Gravity.CENTER_VERTICAL);
        hello.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView avatar=center("م",16,GREEN_DARK,true);
        avatar.setBackground(round(soft(GREEN),24));
        hello.addView(avatar,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout htx=new LinearLayout(this); htx.setOrientation(LinearLayout.VERTICAL); htx.setPadding(dp(9),0,0,0);
        htx.addView(text("مرحبًا، ميمون",18,TEXT,true));
        htx.addView(text(homeMessage(),10,MUTED,false));
        hello.addView(htx,new LinearLayout.LayoutParams(0,-2,1));
        TextView wave=center("👋",19,TEXT,false); hello.addView(wave,new LinearLayout.LayoutParams(dp(38),dp(38)));
        add(r,hello,4);

        LinearLayout pc=card(); pc.setPadding(dp(12),dp(10),dp(12),dp(11)); add(r,pc,10);
        LinearLayout ph=new LinearLayout(this); ph.setOrientation(LinearLayout.HORIZONTAL); ph.setGravity(Gravity.CENTER_VERTICAL); ph.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ph.addView(text("تقدم اليوم",13,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
        ph.addView(text("•••",11,GREEN_DARK,true)); pc.addView(ph);
        LinearLayout period=new LinearLayout(this); period.setOrientation(LinearLayout.HORIZONTAL); period.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); period.setPadding(0,dp(8),0,dp(5));
        addPeriod(period,"اليوم",true); addPeriod(period,"الأسبوع",false); addPeriod(period,"الشهر",false); addPeriod(period,"السنة",false); pc.addView(period);
        LinearLayout rings=new LinearLayout(this); rings.setOrientation(LinearLayout.HORIZONTAL); rings.setGravity(Gravity.CENTER); rings.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addRing(rings,"اليوم",dayRate(Calendar.getInstance()),GREEN,54);
        addRing(rings,"الأسبوع",weekRate(),BLUE,54);
        addRing(rings,"الشهر",monthRate(),PURPLE,54);
        addRing(rings,"السنة",yearRate(),GOLD,54);
        pc.addView(rings);
        TextView guide=center(dayRate(Calendar.getInstance())>=80?"أنت متقدم؛ استمر على هذا النسق 💪":"أنجز المهمة التالية فقط ثم انتقل لما بعدها.",10,MUTED,false);
        guide.setPadding(0,dp(5),0,0); pc.addView(guide);

        LinearLayout goal=card(); goal.setPadding(dp(12),dp(11),dp(12),dp(10)); add(r,goal,9);
        LinearLayout gh=new LinearLayout(this); gh.setOrientation(LinearLayout.HORIZONTAL); gh.setGravity(Gravity.CENTER_VERTICAL); gh.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        gh.addView(new IconBubble(this,"target",GREEN,soft(GREEN)),new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout gtx=new LinearLayout(this); gtx.setOrientation(LinearLayout.VERTICAL); gtx.setPadding(dp(9),0,0,0);
        gtx.addView(text("الهدف اليومي",15,TEXT,true));
        gtx.addView(text("إنجاز ٨٠٪ من المهام الأساسية",10,MUTED,false)); gh.addView(gtx,new LinearLayout.LayoutParams(0,-2,1));
        goal.addView(gh);
        int rd=requiredDoneToday(), rc=requiredCountToday();
        LinearLayout gm=new LinearLayout(this); gm.setOrientation(LinearLayout.HORIZONTAL); gm.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); gm.setPadding(0,dp(7),0,dp(4));
        gm.addView(text(ar(rd)+"/"+ar(rc)+" مهام",10,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
        gm.addView(text(ar(quranPagesThisMonth())+" صفحات قرآن",9,GREEN_DARK,true)); goal.addView(gm);
        goal.addView(progress(rd,Math.max(1,rc),GREEN,5));

        r.addView(sectionHeader("جدول اليوم","عرض الكل",v->{tab="tasks";render();}));
        for(Task t:homeTasks()) addTaskCard(r,t,true);

        LinearLayout streak=card(); streak.setPadding(dp(12),dp(9),dp(12),dp(9)); add(r,streak,8);
        LinearLayout stHead=new LinearLayout(this); stHead.setOrientation(LinearLayout.HORIZONTAL); stHead.setGravity(Gravity.CENTER_VERTICAL); stHead.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout stText=new LinearLayout(this); stText.setOrientation(LinearLayout.VERTICAL);
        stText.addView(text("سلسلة الإنجاز 🔥",12,TEXT,true)); stText.addView(text(ar(streak())+" يوم متتالي",17,TEXT,true));
        stHead.addView(stText,new LinearLayout.LayoutParams(0,-2,1));
        TextView fires=center("🔥  🔥  🔥  🔥  🔥  🔥",13,ORANGE,false); stHead.addView(fires); streak.addView(stHead);

        LinearLayout quick=new LinearLayout(this); quick.setOrientation(LinearLayout.HORIZONTAL); quick.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); quick.setPadding(0,dp(8),0,0);
        quick.addView(quickTile("العمل","briefcase",ORANGE),weightSmall());
        quick.addView(quickTile("الإنجليزية","language",BLUE),weightSmall());
        quick.addView(quickTile("القرآن","quran",GREEN),weightSmall());
        quick.addView(quickTile("كتب","book",PURPLE),weightSmall()); r.addView(quick);
        gap(r,14); return s;
    }

    private View tasksPage() {
        ScrollView s=scroll(); LinearLayout r=root(s);
        r.addView(topBar("المهام","more","filter"));
        r.addView(weekStrip());
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); chips.setPadding(0,dp(7),0,dp(4));
        String[] fs={"الكل","قرآن","إنجليزية","عمل","صحة","أسرة","LeapAhead"};
        for(String f:fs){TextView c=filterChip(f,f.equals(taskFilter)); chips.addView(c); c.setOnClickListener(v->{taskFilter=f;render();});}
        hs.addView(chips); r.addView(hs,new LinearLayout.LayoutParams(-1,dp(48)));
        r.addView(sectionHeader("مهام اليوم",ar(countDoneToday())+" / "+ar(tasksToday().size()),null));
        for(Task t:tasksToday()) if(matches(t)) addTaskCard(r,t,false);
        r.addView(sectionHeader("مهام مؤجلة",ar(overdueCount()),null));
        LinearLayout postponed=card(); postponed.setPadding(dp(12),dp(10),dp(12),dp(10)); add(r,postponed,4);
        if(overdueCount()==0) postponed.addView(text("لا توجد مهام مؤجلة الآن.",10,MUTED,false));
        else postponed.addView(text("لديك "+ar(overdueCount())+" مهمة انتهى وقتها ولم تُنجز؛ ابدأ بالأهم فقط.",10,MUTED,false));
        Button add=primaryButton("+  إضافة مهمة"); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48)); bp.setMargins(0,dp(12),0,dp(18)); r.addView(add,bp);
        add.setOnClickListener(v->startActivity(new Intent(this,PlannerCenterActivity.class)));
        return s;
    }

    private View statsPage() {
        ScrollView s=scroll(); LinearLayout r=root(s);
        r.addView(topBar("الإحصائيات","calendar","more"));
        LinearLayout hero=card(); hero.setPadding(dp(12),dp(11),dp(12),dp(11)); add(r,hero,8);
        LinearLayout hr=new LinearLayout(this); hr.setOrientation(LinearLayout.HORIZONTAL); hr.setGravity(Gravity.CENTER); hr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout counts=new LinearLayout(this); counts.setOrientation(LinearLayout.VERTICAL); counts.setGravity(Gravity.CENTER);
        counts.addView(center(ar(monthCompleted()),18,TEXT,true)); counts.addView(center("مهمة مكتملة",9,MUTED,false));
        counts.addView(center(ar(monthTaskCount()),18,TEXT,true)); counts.addView(center("إجمالي المهام",9,MUTED,false));
        hr.addView(counts,new LinearLayout.LayoutParams(0,dp(106),1));
        LinearLayout ringBox=new LinearLayout(this); ringBox.setOrientation(LinearLayout.VERTICAL); ringBox.setGravity(Gravity.CENTER);
        ringBox.addView(new RingView(this,monthRate(),GREEN),new LinearLayout.LayoutParams(dp(84),dp(84))); ringBox.addView(center("نسبة الإنجاز",9,MUTED,true));
        hr.addView(ringBox,new LinearLayout.LayoutParams(0,dp(106),1));
        LinearLayout change=new LinearLayout(this); change.setOrientation(LinearLayout.VERTICAL); change.setGravity(Gravity.CENTER);
        change.addView(center(ar(weekRate())+"٪",18,GREEN_DARK,true)); change.addView(center("هذا الأسبوع",9,MUTED,false));
        change.addView(center(weekRate()>=70?"ممتاز!":"قابل للتحسن",9,weekRate()>=70?GREEN_DARK:ORANGE,true));
        hr.addView(change,new LinearLayout.LayoutParams(0,dp(106),1)); hero.addView(hr);

        r.addView(sectionHeader("أفضل أسابيع","آخر ٦ أسابيع",null));
        LinearLayout weeks=card(); weeks.setPadding(dp(9),dp(8),dp(9),dp(6)); add(r,weeks,4); weeks.addView(new SixWeekBars(this),new LinearLayout.LayoutParams(-1,dp(145)));
        r.addView(sectionHeader("اتجاه الإنجاز الشهري","",null));
        LinearLayout trend=card(); trend.setPadding(dp(8),dp(8),dp(8),dp(5)); add(r,trend,4); trend.addView(new TrendView(this),new LinearLayout.LayoutParams(-1,dp(150)));

        LinearLayout analytics=new LinearLayout(this); analytics.setOrientation(LinearLayout.HORIZONTAL); analytics.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); analytics.setPadding(0,dp(8),0,0);
        LinearLayout donut=card(); donut.setGravity(Gravity.CENTER); donut.setPadding(dp(8),dp(8),dp(8),dp(8)); donut.addView(text("توزيع المهام",12,TEXT,true)); donut.addView(new DonutView(this),new LinearLayout.LayoutParams(-1,dp(116)));
        analytics.addView(donut,new LinearLayout.LayoutParams(0,dp(168),1));
        LinearLayout blocker=card(); blocker.setGravity(Gravity.CENTER); blocker.setPadding(dp(8),dp(8),dp(8),dp(8)); blocker.addView(text("أكثر سبب تعطيل",12,TEXT,true)); blocker.addView(new IconBubble(this,"block",PURPLE,soft(PURPLE)),new LinearLayout.LayoutParams(dp(52),dp(52))); blocker.addView(center(topBlocker(),10,MUTED,true));
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(0,dp(168),1); blp.setMargins(dp(6),0,0,0); analytics.addView(blocker,blp); r.addView(analytics);

        r.addView(sectionHeader("ملخص الأرقام","هذا الشهر",null));
        LinearLayout m1=metricRow(); m1.addView(metricCard("points",ar(totalPoints()),"النقاط",GOLD),weightMetric()); m1.addView(metricCard("focus",formatMinutes(focusMinutes()),"ساعات التركيز",BLUE),weightMetric()); m1.addView(metricCard("fire",ar(streak()),"سلسلة الإنجاز",ORANGE),weightMetric()); r.addView(m1);
        LinearLayout m2=metricRow(); m2.addView(metricCard("check",ar(monthRate())+"٪","معدل الالتزام",GREEN),weightMetric()); m2.addView(metricCard("book",ar(monthDoneId("leap")),"الكتب المنجزة",PURPLE),weightMetric()); m2.addView(metricCard("quran",ar(quranPagesThisMonth()),"صفحات المراجعة",GREEN),weightMetric()); r.addView(m2);
        r.addView(sectionHeader("أداء المجالات","هذا الأسبوع",null));
        LinearLayout dom=card(); dom.setPadding(dp(12),dp(4),dp(12),dp(4)); add(r,dom,4); for(String d:domainColors.keySet()) addDomainProgress(dom,d);
        gap(r,16); return s;
    }

    private View rewardsPage() {
        ScrollView s=scroll(); LinearLayout r=root(s);
        r.addView(topBar("المكافآت","more","gift"));
        int total=totalPoints(), level=levelFor(total), base=levelBase(level), next=levelNext(level);
        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(dp(14),dp(13),dp(14),dp(12)); hero.setBackground(round(NAVY,18)); add(r,hero,7);
        LinearLayout hh=new LinearLayout(this); hh.setOrientation(LinearLayout.HORIZONTAL); hh.setGravity(Gravity.CENTER_VERTICAL); hh.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        hh.addView(new MedalView(this,level),new LinearLayout.LayoutParams(dp(72),dp(72)));
        LinearLayout ht=new LinearLayout(this); ht.setOrientation(LinearLayout.VERTICAL); ht.setPadding(dp(10),0,0,0);
        ht.addView(text("إجمالي النقاط",9,Color.rgb(188,205,214),false)); ht.addView(text(ar(total),25,Color.WHITE,true)); ht.addView(text("المستوى "+ar(level)+" • "+levelName(level),11,Color.WHITE,true)); hh.addView(ht,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=center(ar(level),16,Color.WHITE,true); badge.setBackground(round(GREEN,18)); hh.addView(badge,new LinearLayout.LayoutParams(dp(44),dp(44))); hero.addView(hh);
        hero.addView(progress(Math.max(0,total-base),Math.max(1,next-base),GREEN,6));
        TextView nx=text(level>=6?"أعلى مستوى متاح":ar(Math.max(0,next-total))+" نقطة للمستوى التالي",9,Color.rgb(188,220,199),false); nx.setPadding(0,dp(5),0,0); hero.addView(nx);

        LinearLayout seg=new LinearLayout(this); seg.setOrientation(LinearLayout.HORIZONTAL); seg.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); seg.setGravity(Gravity.CENTER); seg.setPadding(0,dp(8),0,0);
        addRewardTab(seg,"level","المستوى"); addRewardTab(seg,"badges","الأوسمة"); addRewardTab(seg,"achievements","الإنجازات"); addRewardTab(seg,"store","المتجر"); r.addView(seg,new LinearLayout.LayoutParams(-1,dp(50)));
        switch(rewardTab){case "badges":rewardBadges(r);break;case "achievements":rewardAchievements(r);break;case "store":rewardStore(r);break;default:rewardLevel(r,total,level,next);}
        gap(r,16); return s;
    }

    private void rewardLevel(LinearLayout r,int total,int level,int next) {
        r.addView(sectionHeader("المستوى الحالي","",null));
        LinearLayout current=card(); current.setPadding(dp(12),dp(10),dp(12),dp(10)); add(r,current,3);
        LinearLayout cr=new LinearLayout(this); cr.setOrientation(LinearLayout.HORIZONTAL); cr.setGravity(Gravity.CENTER_VERTICAL); cr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        cr.addView(new IconBubble(this,"level",GREEN,soft(GREEN)),new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout ctx=new LinearLayout(this); ctx.setOrientation(LinearLayout.VERTICAL); ctx.setPadding(dp(10),0,0,0); ctx.addView(text(levelName(level),16,TEXT,true)); ctx.addView(text(level>=6?"أعلى رتبة حالية":"بقي "+ar(Math.max(0,next-total))+" نقطة للمستوى التالي",9,MUTED,false)); cr.addView(ctx,new LinearLayout.LayoutParams(0,-2,1)); current.addView(cr);
        r.addView(sectionHeader("الأوسمة","عرض الكل",v->{rewardTab="badges";render();}));
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false); LinearLayout bs=new LinearLayout(this); bs.setOrientation(LinearLayout.HORIZONTAL); bs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] first={"القرآن","الإنجليزية والقبول","العمل والدخل","الصحة","الانضباط"}; for(String d:first) bs.addView(badgeTile(d)); hs.addView(bs); r.addView(hs,new LinearLayout.LayoutParams(-1,dp(94)));
        r.addView(sectionHeader("إنجازات حديثة","عرض الكل",v->{rewardTab="achievements";render();}));
        LinearLayout ach=card(); ach.setPadding(dp(11),dp(2),dp(11),dp(2)); add(r,ach,3); addAchievementRow(ach,"قارئ متميز","أكمل ٣٠ جلسة قراءة",countDoneIds("leap"),30,200); divider(ach); addAchievementRow(ach,"أسبوع ذهبي","٥ أيام قوية في أسبوع",strongThisWeek(),5,300); divider(ach); addAchievementRow(ach,"مستمر لا يتوقف","٧ أيام متتالية",streak(),7,250);
        r.addView(sectionHeader("متجر المكافآت","عرض الكل",v->{rewardTab="store";render();}));
        LinearLayout shop=new LinearLayout(this); shop.setOrientation(LinearLayout.HORIZONTAL); shop.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); shop.setPadding(0,dp(3),0,0);
        shop.addView(storeTile("كتاب ورقي","book",1000),weightSmall()); shop.addView(storeTile("سماعة","headphones",2000),weightSmall()); shop.addView(storeTile("وقت حر","gift",3000),weightSmall()); r.addView(shop);
    }

    private void rewardBadges(LinearLayout r) {r.addView(sectionHeader("قوة المجالات","الرتبة الحالية",null)); LinearLayout box=card(); box.setPadding(dp(12),dp(4),dp(12),dp(4)); add(r,box,3); for(String d:domainColors.keySet()) addRankRow(box,d);}
    private void rewardAchievements(LinearLayout r) {r.addView(sectionHeader("الإنجازات","تتقدم تلقائيًا",null)); LinearLayout box=card(); box.setPadding(dp(11),dp(2),dp(11),dp(2)); add(r,box,3); addAchievementRow(box,"سبعة أيام قوية","حافظ على +٨٠٪ لسبعة أيام",streak(),7,300); divider(box); addAchievementRow(box,"قارئ مستمر","أكمل ٣٠ جلسة LeapAhead",countDoneIds("leap"),30,250); divider(box); addAchievementRow(box,"مراجعة راسخة","أكمل ٢٠ جلسة قرآن",countDoneIds("quran"),20,200); divider(box); addAchievementRow(box,"إنجليزية ثابتة","أكمل ١٥ جلسة إنجليزية",countDoneIds("english"),15,200);}
    private void rewardStore(LinearLayout r) {LinearLayout wallet=card(); wallet.setPadding(dp(12),dp(10),dp(12),dp(10)); add(r,wallet,5); wallet.addView(text("رصيد المكافآت",10,MUTED,false)); wallet.addView(text(ar(credits())+" رصيد",21,TEXT,true)); r.addView(sectionHeader("المتجر","اختيار مكافأة بعد الإنجاز",null)); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); row.addView(storeTile("كتاب ورقي","book",1000),weightSmall()); row.addView(storeTile("سماعة","headphones",2000),weightSmall()); row.addView(storeTile("وقت حر","gift",3000),weightSmall()); r.addView(row);}

    private View morePage() {ScrollView s=scroll(); LinearLayout r=root(s); r.addView(topBar("المزيد","more","settings")); r.addView(sectionHeader("إدارة مساري","",null)); addMenuItem(r,"planner","تخصيص الجدول","إضافة وتعديل المهام والأوقات",GREEN,()->startActivity(new Intent(this,PlannerCenterActivity.class))); addMenuItem(r,"calendar","المراجعة الأسبوعية","راجع تقدمك والعوائق وخطة الأسبوع",BLUE,()->startActivity(new Intent(this,WeeklyPlannerActivity.class))); addMenuItem(r,"reward","نظام المكافآت","النقاط والمستويات والأوسمة",GOLD,()->{tab="rewards";render();}); r.addView(sectionHeader("قاعدة مساري","",null)); LinearLayout rule=card(); rule.setPadding(dp(12),dp(10),dp(12),dp(10)); add(r,rule,3); rule.addView(text("المهام الأساسية أولًا، ولا نضغط الكتب على حساب النوم أو القرآن أو الإنجليزية.",11,TEXT,false)); gap(r,16); return s;}

    private View topBar(String title,String leftIcon,String rightIcon) {FrameLayout f=new FrameLayout(this); f.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); f.setPadding(0,0,0,dp(3)); TextView tv=center(title,18,"مساري".equals(title)?GREEN_DARK:TEXT,true); f.addView(tv,new FrameLayout.LayoutParams(-2,dp(44),Gravity.CENTER)); IconBubble left=new IconBubble(this,leftIcon,TEXT,Color.TRANSPARENT); f.addView(left,new FrameLayout.LayoutParams(dp(38),dp(38),Gravity.LEFT|Gravity.CENTER_VERTICAL)); IconBubble right=new IconBubble(this,rightIcon,TEXT,Color.TRANSPARENT); f.addView(right,new FrameLayout.LayoutParams(dp(38),dp(38),Gravity.RIGHT|Gravity.CENTER_VERTICAL)); right.setOnClickListener(v->{tab="more";render();}); return f;}
    private View bottomNav() {LinearLayout bar=new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER); bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); bar.setPadding(dp(5),dp(5),dp(5),dp(5)); bar.setBackgroundColor(NAVY); addNav(bar,"home","الرئيسية","home"); addNav(bar,"tasks","المهام","check"); addNav(bar,"stats","الإحصائيات","stats"); addNav(bar,"rewards","المكافآت","reward"); addNav(bar,"more","المزيد","more"); return bar;}
    private void addNav(LinearLayout bar,String key,String label,String icon) {boolean active=key.equals(tab); LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setPadding(dp(2),dp(3),dp(2),dp(3)); if(active)item.setBackground(round(GREEN,16)); item.addView(new MiniIcon(this,icon,Color.WHITE),new LinearLayout.LayoutParams(dp(22),dp(22))); TextView t=center(label,8,active?Color.WHITE:Color.rgb(205,216,223),active); t.setPadding(0,dp(2),0,0); item.addView(t); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,active?dp(62):dp(56),1); p.setMargins(dp(2),active?0:dp(4),dp(2),0); bar.addView(item,p); item.setOnClickListener(v->{tab=key;render();});}
    private View sectionHeader(String title,String action,View.OnClickListener listener) {LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); row.setPadding(0,dp(11),0,dp(5)); row.addView(text(title,16,TEXT,true),new LinearLayout.LayoutParams(0,-2,1)); if(action!=null&&!action.isEmpty()){TextView a=text(action,9,GREEN_DARK,true); if(listener!=null)a.setOnClickListener(listener); row.addView(a);} return row;}
    private void addPeriod(LinearLayout row,String label,boolean selected){TextView t=center(label,9,selected?Color.WHITE:MUTED,selected); t.setBackground(round(selected?GREEN:Color.rgb(246,247,248),11)); row.addView(t,new LinearLayout.LayoutParams(0,dp(28),1));}
    private void addTaskCard(LinearLayout root,Task task,boolean compact) {LinearLayout box=card(); box.setPadding(dp(10),compact?dp(8):dp(9),dp(10),compact?dp(8):dp(9)); add(root,box,compact?6:7); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); int c=domainColors.containsKey(task.domain)?domainColors.get(task.domain):GREEN; row.addView(new IconBubble(this,task.icon,c,soft(c)),new LinearLayout.LayoutParams(dp(38),dp(38))); LinearLayout tx=new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL); tx.setPadding(dp(8),0,dp(6),0); TextView title=text(task.title,compact?12:13,done(task)?MUTED:TEXT,true); tx.addView(title); tx.addView(text(time(task.start)+"  •  "+shortDomain(task.domain),9,MUTED,false)); row.addView(tx,new LinearLayout.LayoutParams(0,-2,1)); row.addView(new CheckView(this,done(task)),new LinearLayout.LayoutParams(dp(28),dp(28))); box.addView(row); box.setOnClickListener(v->{setDone(task,!done(task));render();});}
    private View weekStrip(){LinearLayout strip=new LinearLayout(this); strip.setOrientation(LinearLayout.HORIZONTAL); strip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); strip.setPadding(0,dp(5),0,dp(3)); Calendar c=saturdayStart(Calendar.getInstance()); String[] names={"سبت","أحد","اثن","ثلا","أربع","خمس","جمعة"}; for(int i=0;i<7;i++){Calendar d=(Calendar)c.clone(); d.add(Calendar.DAY_OF_MONTH,i); boolean today=dateKey(d).equals(todayKey); LinearLayout cell=new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL); cell.setGravity(Gravity.CENTER); if(today)cell.setBackground(round(GREEN,10)); cell.addView(center(names[i],7,today?Color.WHITE:MUTED,false)); cell.addView(center(ar(d.get(Calendar.DAY_OF_MONTH)),10,today?Color.WHITE:TEXT,true)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1); p.setMargins(dp(2),0,dp(2),0); strip.addView(cell,p);} return strip;}
    private TextView filterChip(String label,boolean selected){TextView t=center(label,9,selected?Color.WHITE:TEXT,selected); t.setPadding(dp(12),dp(6),dp(12),dp(6)); t.setBackground(round(selected?GREEN:Color.WHITE,15,selected?GREEN:BORDER)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(32)); p.setMargins(dp(3),dp(3),dp(3),dp(3)); t.setLayoutParams(p); return t;}
    private void addRing(LinearLayout row,String label,int value,int color,int size){LinearLayout b=new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); b.setGravity(Gravity.CENTER); b.addView(new RingView(this,value,color),new LinearLayout.LayoutParams(dp(size),dp(size))); TextView l=center(label,8,MUTED,false); l.setPadding(0,dp(2),0,0); b.addView(l); row.addView(b,new LinearLayout.LayoutParams(0,dp(size+20),1));}
    private LinearLayout metricRow(){LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.HORIZONTAL); x.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); x.setPadding(0,dp(6),0,0); return x;} private LinearLayout.LayoutParams weightMetric(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(82),1); p.setMargins(dp(2),0,dp(2),0); return p;} private LinearLayout.LayoutParams weightSmall(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMargins(dp(2),0,dp(2),0); return p;}
    private View metricCard(String icon,String value,String label,int c){LinearLayout x=card(); x.setGravity(Gravity.CENTER); x.setPadding(dp(4),dp(6),dp(4),dp(5)); x.addView(new MiniIcon(this,icon,c),new LinearLayout.LayoutParams(dp(20),dp(20))); x.addView(center(value,value.length()>8?10:13,TEXT,true)); x.addView(center(label,7,MUTED,false)); return x;}
    private View quickTile(String label,String icon,int c){LinearLayout x=card(); x.setGravity(Gravity.CENTER); x.setPadding(dp(4),dp(7),dp(4),dp(6)); x.addView(new IconBubble(this,icon,c,soft(c)),new LinearLayout.LayoutParams(dp(34),dp(34))); x.addView(center(label,8,TEXT,true)); return x;}
    private void addDomainProgress(LinearLayout p,String d){int c=domainColors.get(d),v=weekDomainRate(d); LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setPadding(0,dp(6),0,dp(6)); LinearLayout h=new LinearLayout(this); h.setOrientation(LinearLayout.HORIZONTAL); h.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); h.addView(text(shortDomain(d),10,TEXT,true),new LinearLayout.LayoutParams(0,-2,1)); h.addView(text(ar(v)+"٪",9,c,true)); x.addView(h); x.addView(progress(v,100,c,4)); p.addView(x);}
    private void addRankRow(LinearLayout p,String d){int pts=domainPoints(d),c=domainColors.get(d),next=rankNext(pts),base=rankBase(pts); LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setPadding(0,dp(7),0,dp(7)); LinearLayout h=new LinearLayout(this); h.setOrientation(LinearLayout.HORIZONTAL); h.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); h.addView(text(shortDomain(d),11,TEXT,true),new LinearLayout.LayoutParams(0,-2,1)); h.addView(text(rankFor(pts),9,c,true)); x.addView(h); x.addView(progress(Math.max(0,pts-base),Math.max(1,next-base),c,4)); p.addView(x);}
    private View badgeTile(String d){int c=domainColors.get(d); LinearLayout x=card(); x.setGravity(Gravity.CENTER); x.setPadding(dp(7),dp(7),dp(7),dp(6)); x.addView(new IconBubble(this,iconForDomain(d),c,soft(c)),new LinearLayout.LayoutParams(dp(42),dp(42))); x.addView(center(shortDomain(d),8,TEXT,true)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(74),dp(82)); p.setMargins(dp(3),0,dp(3),0); x.setLayoutParams(p); return x;}
    private void addAchievementRow(LinearLayout p,String title,String sub,int current,int target,int reward){LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); r.setPadding(0,dp(7),0,dp(7)); IconBubble icon=new IconBubble(this,current>=target?"trophy":"star",current>=target?GREEN:GOLD,soft(current>=target?GREEN:GOLD)); r.addView(icon,new LinearLayout.LayoutParams(dp(36),dp(36))); LinearLayout tx=new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL); tx.setPadding(dp(7),0,0,0); tx.addView(text(title,10,TEXT,true)); tx.addView(text(sub,8,MUTED,false)); r.addView(tx,new LinearLayout.LayoutParams(0,-2,1)); r.addView(text("+"+ar(reward),9,GREEN_DARK,true)); p.addView(r);}
    private View storeTile(String title,String icon,int cost){LinearLayout x=card(); x.setGravity(Gravity.CENTER); x.setPadding(dp(4),dp(8),dp(4),dp(7)); x.addView(new IconBubble(this,icon,NAVY,Color.rgb(235,239,243)),new LinearLayout.LayoutParams(dp(48),dp(48))); x.addView(center(title,8,TEXT,true)); x.addView(center(ar(cost),9,GREEN_DARK,true)); return x;}
    private void addRewardTab(LinearLayout p,String key,String label){boolean a=key.equals(rewardTab); LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setGravity(Gravity.CENTER); TextView t=center(label,9,a?GREEN_DARK:TEXT,a); x.addView(t,new LinearLayout.LayoutParams(-1,dp(31))); View line=new View(this); line.setBackgroundColor(a?GREEN:Color.TRANSPARENT); x.addView(line,new LinearLayout.LayoutParams(-1,dp(2))); p.addView(x,new LinearLayout.LayoutParams(0,dp(35),1)); x.setOnClickListener(v->{rewardTab=key;render();});}
    private void addMenuItem(LinearLayout r,String icon,String title,String sub,int c,Runnable action){LinearLayout x=card(); x.setPadding(dp(10),dp(9),dp(10),dp(9)); add(r,x,7); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); row.addView(new IconBubble(this,icon,c,soft(c)),new LinearLayout.LayoutParams(dp(40),dp(40))); LinearLayout tx=new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL); tx.setPadding(dp(8),0,0,0); tx.addView(text(title,12,TEXT,true)); tx.addView(text(sub,9,MUTED,false)); row.addView(tx,new LinearLayout.LayoutParams(0,-2,1)); row.addView(text("‹",20,MUTED,false)); x.addView(row); x.setOnClickListener(v->action.run());}

    private List<Task> tasksToday(){return tasksFor(Calendar.getInstance());}
    private List<Task> tasksFor(Calendar d){List<Task>a=new ArrayList<>(); int day=d.get(Calendar.DAY_OF_WEEK); a.add(t("fajr",240,390,"الفجر والحلقة والدرس","الدين والمسجد",5,true,"mosque")); if(day==Calendar.SATURDAY)a.add(t("workoutA",390,420,"تمرين A — كتف وذراعان + جسم كامل","الصحة",20,true,"health")); if(day==Calendar.TUESDAY)a.add(t("workoutB",390,420,"تمرين B — أوتار وقبضة وسرعة","الصحة",20,true,"health")); a.add(t("english",420,480,englishTitle(day),"الإنجليزية والقبول",day==Calendar.FRIDAY?10:25,day!=Calendar.FRIDAY,"language")); a.add(t("sleep2",480,570,"النوم التكميلي","الصحة",5,true,"sleep")); a.add(t("leap1",570,600,"LeapAhead — الكتاب ١","المعرفة والقراءة",7,true,"book")); a.add(t("work",600,690,workTitle(day),"العمل والدخل",day==Calendar.FRIDAY?10:25,day!=Calendar.FRIDAY,"briefcase")); a.add(t("quran1",705,780,"القرآن — مراجعة جديدة: صفحتان","القرآن",14,true,"quran")); a.add(t("lunch",780,810,"الغداء","الانضباط",3,true,"meal")); a.add(t("leap2",810,855,"LeapAhead — الكتاب ٢","المعرفة والقراءة",8,true,"book")); a.add(t("quran2",885,970,"القرآن — صفحتان + مراجعة قديمة","القرآن",18,true,"quran")); a.add(afternoon(day)); a.add(t("maghrib",1080,1200,"المغرب والتحفيظ والعشاء","الدين والمسجد",5,true,"mosque")); a.add(t("dinner",1200,1230,"العشاء مع الأسرة","الأسرة",3,true,"home")); a.add(t("leap3",1230,1275,"LeapAhead — الكتاب ٣","المعرفة والقراءة",7,false,"book")); a.add(t("close",1320,1340,"إغلاق اليوم وتجهيز الغد","الانضباط",5,true,"check")); a.add(t("sleep",1340,1350,"الاستعداد للنوم","الصحة",5,true,"sleep")); addCustom(a,d); return a;}
    private Task afternoon(int day){switch(day){case Calendar.SATURDAY:return t("family_talk",990,1020,"الأسرة + تدريب كلام قصير","الأسرة",8,true,"home"); case Calendar.SUNDAY:return t("talk1",990,1020,"تدريب التواصل","التواصل",10,true,"talk"); case Calendar.MONDAY:return t("family_friend",990,1020,"خدمة الأسرة + تفقد صديق","الأسرة",8,true,"home"); case Calendar.TUESDAY:return t("talk2",990,1020,"تدريب الكلام والحزم","التواصل",10,true,"talk"); case Calendar.WEDNESDAY:return t("medicine",990,1050,"مراجعة طب قديم","المعرفة والقراءة",12,true,"book"); case Calendar.THURSDAY:return t("khatera",990,1030,"خاطرة دينية + تدريب إلقاء","التواصل",12,true,"talk"); default:return t("explore",990,1050,"استكشاف علمي أو مهارة حياة","المعرفة والقراءة",10,false,"star");}}
    private void addCustom(List<Task>a,Calendar d){try{JSONArray arr=new JSONArray(prefs.getString("custom_tasks","[]")); for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i); if(o==null||!o.optBoolean("active",true))continue; int dy=o.optInt("day",0); if(dy!=0&&dy!=d.get(Calendar.DAY_OF_WEEK))continue; a.add(t(o.optString("id","custom_"+i),o.optInt("start",960),o.optInt("end",990),o.optString("title","مهمة مخصصة"),o.optString("domain","الانضباط"),o.optInt("points",10),o.optBoolean("required",false),"check"));}}catch(Exception ignored){}}
    private Task t(String id,int st,int en,String title,String domain,int pts,boolean req,String icon){return new Task(id,st,en,title,domain,pts,req,icon);}
    private String englishTitle(int d){switch(d){case Calendar.SATURDAY:return"الإنجليزية: Vocabulary + Reading Explorer"; case Calendar.SUNDAY:return"الإنجليزية: Vocabulary + Tactics"; case Calendar.MONDAY:return"الإنجليزية: Vocabulary + Reading Explorer"; case Calendar.TUESDAY:return"الإنجليزية: Vocabulary + Tactics"; case Calendar.WEDNESDAY:return"الإنجليزية: Vocabulary + Oxford Bookworms"; case Calendar.THURSDAY:return"اختبار الإنجليزية الأسبوعي"; default:return"استماع إنجليزي ممتع — يوم خفيف";}}
    private String workTitle(int d){switch(d){case Calendar.SATURDAY: case Calendar.SUNDAY:return"العمل: تطوير الأكاديمية"; case Calendar.MONDAY: case Calendar.TUESDAY:return"العمل: الوصول للسوق"; case Calendar.WEDNESDAY:return"العمل: دخل مباشر"; case Calendar.THURSDAY:return"العمل: مراجعة الأرقام"; default:return"مراجعة مالية خفيفة";}}
    private List<Task> homeTasks(){List<Task> all=tasksToday(),out=new ArrayList<>(); int now=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60+Calendar.getInstance().get(Calendar.MINUTE); for(Task t:all){if(t.id.equals("sleep2")||t.id.equals("lunch")||t.id.equals("dinner"))continue; if(!done(t)&&t.end>=now-30)out.add(t); if(out.size()>=5)break;} if(out.size()<5){for(Task t:all){if(out.contains(t)||t.id.equals("sleep2")||t.id.equals("lunch")||t.id.equals("dinner"))continue; out.add(t); if(out.size()>=5)break;}} return out;}
    private boolean matches(Task t){if("الكل".equals(taskFilter))return true; if("قرآن".equals(taskFilter))return t.domain.equals("القرآن"); if("إنجليزية".equals(taskFilter))return t.domain.equals("الإنجليزية والقبول"); if("عمل".equals(taskFilter))return t.domain.equals("العمل والدخل"); if("صحة".equals(taskFilter))return t.domain.equals("الصحة"); if("أسرة".equals(taskFilter))return t.domain.equals("الأسرة"); if("LeapAhead".equals(taskFilter))return t.id.startsWith("leap"); return true;}
    private boolean done(Task t){return prefs.getBoolean("reward_done_"+todayKey+"_"+t.id,false);} private boolean doneOn(Task t,Calendar d){return prefs.getBoolean("reward_done_"+dateKey(d)+"_"+t.id,false);}
    private void setDone(Task t,boolean val){boolean old=done(t); if(old==val)return; int delta=val?t.points:-t.points; String dk="reward_day_points_"+todayKey,dom="reward_domain_"+t.domain; prefs.edit().putBoolean("reward_done_"+todayKey+"_"+t.id,val).putInt(dk,Math.max(0,prefs.getInt(dk,0)+delta)).putInt(dom,Math.max(0,prefs.getInt(dom,0)+delta)).apply(); Toast.makeText(this,val?"+"+t.points+" نقطة":"تم التراجع",Toast.LENGTH_SHORT).show();}
    private int countDoneToday(){int n=0;for(Task t:tasksToday())if(done(t))n++;return n;} private int requiredCountToday(){int n=0;for(Task t:tasksToday())if(t.required)n++;return n;} private int requiredDoneToday(){int n=0;for(Task t:tasksToday())if(t.required&&done(t))n++;return n;}
    private int overdueCount(){int now=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60+Calendar.getInstance().get(Calendar.MINUTE),n=0; for(Task t:tasksToday())if(!done(t)&&t.end<now)n++; return n;}
    private int dayPoints(Calendar c){return prefs.getInt("reward_day_points_"+dateKey(c),0);} private int dayTarget(Calendar c){int n=0;for(Task t:tasksFor(c))if(t.required)n+=t.points;return Math.max(1,n);} private int dayRate(Calendar c){return pct(dayPoints(c),dayTarget(c));}
    private int weekRate(){return weekRateAt(0);} private int weekRateAt(int weeksBack){Calendar start=saturdayStart(Calendar.getInstance()); start.add(Calendar.DAY_OF_MONTH,-7*weeksBack); Calendar end=(Calendar)start.clone(); end.add(Calendar.DAY_OF_MONTH,6); Calendar now=Calendar.getInstance(); if(end.after(now))end=now; int p=0,t=0; while(!start.after(end)){p+=dayPoints(start);t+=dayTarget(start);start.add(Calendar.DAY_OF_MONTH,1);}return pct(p,t);}
    private int monthRate(){Calendar c=Calendar.getInstance(),now=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int p=0,t=0;while(!c.after(now)){p+=dayPoints(c);t+=dayTarget(c);c.add(Calendar.DAY_OF_MONTH,1);}return pct(p,t);} private int yearRate(){Calendar c=Calendar.getInstance(),now=Calendar.getInstance();c.set(Calendar.MONTH,Calendar.JANUARY);c.set(Calendar.DAY_OF_MONTH,1);int p=0,t=0;while(!c.after(now)){p+=dayPoints(c);t+=dayTarget(c);c.add(Calendar.DAY_OF_MONTH,1);}return pct(p,t);}
    private int monthCompleted(){Calendar c=Calendar.getInstance(),now=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int n=0;while(!c.after(now)){for(Task t:tasksFor(c))if(doneOn(t,c))n++;c.add(Calendar.DAY_OF_MONTH,1);}return n;} private int monthTaskCount(){Calendar c=Calendar.getInstance(),now=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int n=0;while(!c.after(now)){n+=tasksFor(c).size();c.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int monthDoneId(String part){Calendar c=Calendar.getInstance(),now=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int n=0;while(!c.after(now)){for(Task t:tasksFor(c))if(t.id.contains(part)&&doneOn(t,c))n++;c.add(Calendar.DAY_OF_MONTH,1);}return n;} private int quranPagesThisMonth(){Calendar c=Calendar.getInstance(),now=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int n=0;while(!c.after(now)){for(Task t:tasksFor(c))if(t.id.startsWith("quran")&&doneOn(t,c))n+=2;c.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int focusMinutes(){Calendar c=Calendar.getInstance(),now=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int sum=0;while(!c.after(now)){String key=dateKey(c);for(Task t:tasksFor(c))if(prefs.getBoolean("reward_done_"+key+"_"+t.id,false)&&(t.domain.equals("الإنجليزية والقبول")||t.domain.equals("العمل والدخل")||t.domain.equals("القرآن")||t.domain.equals("المعرفة والقراءة")))sum+=Math.max(0,t.end-t.start);c.add(Calendar.DAY_OF_MONTH,1);}return sum;}
    private int streak(){Calendar c=Calendar.getInstance();if(dayRate(c)<80)c.add(Calendar.DAY_OF_MONTH,-1);int n=0;for(int i=0;i<365;i++){if(dayRate(c)>=80){n++;c.add(Calendar.DAY_OF_MONTH,-1);}else break;}return n;} private int strongThisWeek(){Calendar c=saturdayStart(Calendar.getInstance()),now=Calendar.getInstance();int n=0;while(!c.after(now)){if(dayRate(c)>=80)n++;c.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int countDoneIds(String contains){int n=0;for(Map.Entry<String,?>e:prefs.getAll().entrySet())if(e.getKey().startsWith("reward_done_")&&e.getKey().contains(contains)&&Boolean.TRUE.equals(e.getValue()))n++;return n;}
    private int weekDomainRate(String d){Calendar c=saturdayStart(Calendar.getInstance()),now=Calendar.getInstance();int e=0,tg=0;while(!c.after(now)){for(Task t:tasksFor(c)){if(!t.domain.equals(d))continue;if(t.required)tg+=t.points;if(doneOn(t,c))e+=t.points;}c.add(Calendar.DAY_OF_MONTH,1);}return pct(e,Math.max(1,tg));}
    private int domainPoints(String d){return prefs.getInt("reward_domain_"+d,0);} private int totalPoints(){int n=0;for(String d:domainColors.keySet())n+=domainPoints(d);return n;} private int credits(){return prefs.getInt("reward_credits",Math.max(0,totalPoints()/10));}
    private int levelFor(int p){if(p>=8000)return 6;if(p>=5000)return 5;if(p>=3000)return 4;if(p>=1500)return 3;if(p>=600)return 2;return 1;} private int levelBase(int l){switch(l){case 2:return 600;case 3:return 1500;case 4:return 3000;case 5:return 5000;case 6:return 8000;default:return 0;}} private int levelNext(int l){switch(l){case 1:return 600;case 2:return 1500;case 3:return 3000;case 4:return 5000;case 5:return 8000;default:return 10000;}} private String levelName(int l){switch(l){case 2:return"منتظم";case 3:return"متقدم";case 4:return"راسخ";case 5:return"متقن";case 6:return"متمكن";default:return"البداية";}}
    private String rankFor(int p){if(p>=1200)return"متمكن";if(p>=650)return"ذهبي";if(p>=300)return"فضي";return"برونزي";} private int rankBase(int p){if(p>=1200)return 1200;if(p>=650)return 650;if(p>=300)return 300;return 0;} private int rankNext(int p){if(p>=1200)return 1800;if(p>=650)return 1200;if(p>=300)return 650;return 300;}
    private String topBlocker(){String x=prefs.getString("weekly_blocker",""); if(x==null||x.trim().isEmpty())x=prefs.getString("last_blocker",""); return x==null||x.trim().isEmpty()?"لم يُسجّل بعد":x;}
    private String homeMessage(){int r=dayRate(Calendar.getInstance()); if(r>=80)return"يوم ممتاز، حافظ على الهدوء."; int now=Calendar.getInstance().get(Calendar.HOUR_OF_DAY); if(now<12)return"ابدأ بأول مهمة واضحة أمامك."; if(now<18)return"بقي من اليوم ما يكفي لمهمة جيدة."; return"اختم يومك بهدوء وجهّز الغد.";}

    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);r.setPadding(dp(13),dp(3),dp(13),dp(8));s.addView(r,new ScrollView.LayoutParams(-1,-2));return r;} private ScrollView scroll(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(BG);return s;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setBackground(round(CARD,15,BORDER));c.setElevation(dp(0.5f));return c;} private void add(LinearLayout r,View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(top),0,0);r.addView(v,p);} private void gap(LinearLayout r,int v){View x=new View(this);r.addView(x,new LinearLayout.LayoutParams(1,dp(v)));} private void divider(LinearLayout p){View v=new View(this);v.setBackgroundColor(BORDER);p.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);return t;} private TextView center(String s,int size,int color,boolean bold){TextView t=text(s,size,color,bold);t.setGravity(Gravity.CENTER);return t;}
    private Button primaryButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT_BOLD);b.setBackground(round(GREEN,22));b.setElevation(dp(1));return b;}
    private View progress(int value,int max,int color,int height){FrameLayout f=new FrameLayout(this);f.setBackground(round(TRACK,height));int pc=max<=0?0:Math.max(0,Math.min(100,Math.round(value*100f/max)));View fill=new View(this);fill.setBackground(round(color,height));f.addView(fill,new FrameLayout.LayoutParams(0,dp(height)));f.post(()->{ViewGroup.LayoutParams p=fill.getLayoutParams();p.width=Math.round(f.getWidth()*pc/100f);p.height=dp(height);fill.setLayoutParams(p);});f.setMinimumHeight(dp(height));return f;}
    private GradientDrawable round(int c,int r){return round(c,r,c);} private GradientDrawable round(int c,int r,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));if(stroke!=c)g.setStroke(dp(1),stroke);return g;} private int soft(int c){int r=Color.red(c),g=Color.green(c),b=Color.blue(c);return Color.rgb((r+255*6)/7,(g+255*6)/7,(b+255*6)/7);} private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);} private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);} private int pct(int a,int b){return b<=0?0:Math.max(0,Math.min(100,Math.round(a*100f/b)));}
    private String dateKey(Calendar c){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(c.getTime());} private Calendar saturdayStart(Calendar now){Calendar c=(Calendar)now.clone();while(c.get(Calendar.DAY_OF_WEEK)!=Calendar.SATURDAY)c.add(Calendar.DAY_OF_MONTH,-1);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c;}
    private String time(int minute){int h=(minute/60)%24,m=minute%60;String suf=h>=12?"م":"ص";int hh=h%12;if(hh==0)hh=12;return ar(hh)+":"+(m<10?"٠":"")+ar(m)+" "+suf;} private String formatMinutes(int min){if(min<60)return ar(min)+" د";int h=min/60,m=min%60;return ar(h)+"س"+(m>0?" "+ar(m)+"د":"");}
    private String shortDomain(String d){if(d==null)return"—";if(d.equals("الإنجليزية والقبول"))return"الإنجليزية";if(d.equals("العمل والدخل"))return"العمل";if(d.equals("المعرفة والقراءة"))return"القراءة";if(d.equals("الدين والمسجد"))return"المسجد";return d;} private String iconForDomain(String d){if(d.equals("القرآن"))return"quran";if(d.equals("الإنجليزية والقبول"))return"language";if(d.equals("العمل والدخل"))return"briefcase";if(d.equals("الصحة"))return"health";if(d.equals("المعرفة والقراءة"))return"book";return"star";}
    private String ar(int n){String s=String.valueOf(n),e="0123456789",a="٠١٢٣٤٥٦٧٨٩";for(int i=0;i<10;i++)s=s.replace(e.charAt(i),a.charAt(i));return s;}

    private static class RingView extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int value,color;RingView(Activity c,int v,int col){super(c);value=v;color=col;}@Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),st=Math.max(3f,w*.07f);RectF r=new RectF(st,st,w-st,h-st);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(st);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(TRACK);c.drawArc(r,-90,360,false,p);p.setColor(color);c.drawArc(r,-90,360*value/100f,false,p);p.setStyle(Paint.Style.FILL);p.setColor(TEXT);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(w*.20f);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(toArabic(value)+"٪",w/2f,h/2f-(fm.ascent+fm.descent)/2f,p);}}
    private static class CheckView extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final boolean checked;CheckView(Activity c,boolean x){super(c);checked=x;}@Override protected void onDraw(Canvas c){float s=Math.min(getWidth(),getHeight()),m=s*.17f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2f,s*.07f));p.setColor(checked?GREEN:Color.rgb(182,190,198));c.drawRoundRect(new RectF(m,m,s-m,s-m),s*.14f,s*.14f,p);if(checked){p.setStyle(Paint.Style.FILL);p.setColor(GREEN);c.drawRoundRect(new RectF(m,m,s-m,s-m),s*.14f,s*.14f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(Math.max(2f,s*.07f));p.setColor(Color.WHITE);Path path=new Path();path.moveTo(s*.31f,s*.52f);path.lineTo(s*.44f,s*.64f);path.lineTo(s*.70f,s*.37f);c.drawPath(path,p);}}}
    private static class MiniIcon extends View {private final String type;private final int color;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);MiniIcon(Activity c,String t,int col){super(c);type=t;color=col;}@Override protected void onDraw(Canvas c){drawIcon(c,p,type,color,getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())*.70f);}}
    private static class IconBubble extends View {private final String type;private final int fg,bg;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);IconBubble(Activity c,String t,int f,int b){super(c);type=t;fg=f;bg=b;}@Override protected void onDraw(Canvas c){super.onDraw(c);float s=Math.min(getWidth(),getHeight());if(bg!=Color.TRANSPARENT){p.setStyle(Paint.Style.FILL);p.setColor(bg);c.drawRoundRect(new RectF(0,0,getWidth(),getHeight()),s*.30f,s*.30f,p);}drawIcon(c,p,type,fg,getWidth()/2f,getHeight()/2f,s*.53f);}}
    private static class MedalView extends View {private final int level;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);MedalView(Activity c,int l){super(c);level=l;}@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),cx=w/2f,cy=h*.43f,r=Math.min(w,h)*.28f;p.setColor(GOLD);p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,r,p);p.setColor(Color.rgb(255,238,185));c.drawCircle(cx,cy,r*.72f,p);p.setColor(NAVY);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(r*.75f);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(toArabic(level),cx,cy-(fm.ascent+fm.descent)/2,p);p.setColor(ORANGE);Path a=new Path();a.moveTo(cx-r*.5f,cy+r*.65f);a.lineTo(cx-r*.15f,h*.95f);a.lineTo(cx,h*.73f);a.close();c.drawPath(a,p);Path b=new Path();b.moveTo(cx+r*.5f,cy+r*.65f);b.lineTo(cx+r*.15f,h*.95f);b.lineTo(cx,h*.73f);b.close();c.drawPath(b,p);}}
    private class SixWeekBars extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);SixWeekBars(Activity c){super(c);}@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),base=h*.78f,left=w*.06f,slot=w*.145f;int best=0;int[] vals=new int[6];for(int i=0;i<6;i++){vals[i]=weekRateAt(5-i);if(vals[i]>vals[best])best=i;}for(int i=0;i<6;i++){float x=left+i*slot,bh=Math.max(5f,(h*.57f)*vals[i]/100f);p.setColor(i==best?GREEN:Color.rgb(226,231,236));c.drawRoundRect(new RectF(x,base-bh,x+slot*.55f,base),dp(4),dp(4),p);p.setColor(TEXT);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(8));c.drawText(toArabic(vals[i])+"%",x+slot*.275f,base-bh-dp(4),p);p.setColor(MUTED);p.setTextSize(dp(7));c.drawText("أسبوع "+toArabic(i+1),x+slot*.275f,base+dp(16),p);}}}
    private class TrendView extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);TrendView(Activity c){super(c);}@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),l=w*.07f,r=w*.96f,t=h*.12f,b=h*.78f;p.setColor(Color.rgb(232,236,240));p.setStrokeWidth(dp(1));for(int i=0;i<=4;i++){float y=t+(b-t)*i/4f;c.drawLine(l,y,r,y,p);}Path path=new Path();int n=14;for(int i=0;i<n;i++){Calendar d=Calendar.getInstance();d.add(Calendar.DAY_OF_MONTH,-(n-1-i)*2);float x=l+(r-l)*i/(n-1f);float y=b-(b-t)*dayRate(d)/100f;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(GREEN);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);for(int i=0;i<n;i++){Calendar d=Calendar.getInstance();d.add(Calendar.DAY_OF_MONTH,-(n-1-i)*2);float x=l+(r-l)*i/(n-1f),y=b-(b-t)*dayRate(d)/100f;c.drawCircle(x,y,dp(2.2f),p);}p.setTextSize(dp(7));p.setTextAlign(Paint.Align.CENTER);p.setColor(MUTED);c.drawText("بداية الشهر",l,b+dp(16),p);c.drawText("اليوم",r,b+dp(16),p);}}
    private class DonutView extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);DonutView(Activity c){super(c);}@Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),s=Math.min(w,h)*.72f,cx=w/2f,cy=h*.48f,st=s*.17f;RectF rr=new RectF(cx-s/2,cy-s/2,cx+s/2,cy+s/2);int[] cols={GREEN,BLUE,ORANGE,PURPLE,PINK};String[] ds={"القرآن","الإنجليزية والقبول","العمل والدخل","المعرفة والقراءة","الأسرة"};int sum=0;int[] vals=new int[ds.length];for(int i=0;i<ds.length;i++){vals[i]=Math.max(1,domainPoints(ds[i]));sum+=vals[i];}float a=-90;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(st);p.setStrokeCap(Paint.Cap.BUTT);for(int i=0;i<vals.length;i++){float sw=360f*vals[i]/sum;p.setColor(cols[i]);c.drawArc(rr,a,sw,false,p);a+=sw;}p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setColor(TEXT);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(14));c.drawText(toArabic(monthCompleted()),cx,cy+dp(4),p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(dp(7));p.setColor(MUTED);c.drawText("إجمالي",cx,cy+dp(15),p);}}
    private static void drawIcon(Canvas c,Paint p,String type,int col,float cx,float cy,float size){p.setColor(col);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2f,size*.10f));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);float r=size*.36f;RectF q=new RectF(cx-r,cy-r,cx+r,cy+r);Path path=new Path();switch(type){case"home":path.moveTo(cx-r,cy);path.lineTo(cx,cy-r*.8f);path.lineTo(cx+r,cy);path.lineTo(cx+r*.7f,cy+r);path.lineTo(cx-r*.7f,cy+r);path.close();c.drawPath(path,p);break;case"menu":case"more":for(int i=-1;i<=1;i++)c.drawCircle(cx+i*r*.65f,cy,size*.05f,p);break;case"bell":c.drawArc(q,200,140,false,p);c.drawLine(cx-r*.75f,cy+r*.15f,cx-r*.75f,cy+r*.55f,p);c.drawLine(cx-r*.75f,cy+r*.55f,cx+r*.75f,cy+r*.55f,p);break;case"filter":path.moveTo(cx-r,cy-r);path.lineTo(cx+r,cy-r);path.lineTo(cx+r*.2f,cy);path.lineTo(cx+r*.2f,cy+r);c.drawPath(path,p);break;case"check":c.drawRoundRect(q,r*.2f,r*.2f,p);path.moveTo(cx-r*.45f,cy);path.lineTo(cx-r*.08f,cy+r*.38f);path.lineTo(cx+r*.52f,cy-r*.4f);c.drawPath(path,p);break;case"stats":for(int i=0;i<3;i++){float x=cx-r+i*r*.8f;c.drawLine(x,cy+r,x,cy+r*(.4f-i*.55f),p);}break;case"reward":case"trophy":c.drawCircle(cx,cy-r*.15f,r*.55f,p);c.drawLine(cx,cy+r*.4f,cx,cy+r,p);c.drawLine(cx-r*.4f,cy+r,cx+r*.4f,cy+r,p);break;case"target":c.drawCircle(cx,cy,r,p);c.drawCircle(cx,cy,r*.55f,p);c.drawCircle(cx,cy,r*.12f,p);break;case"quran":case"book":c.drawRect(cx-r,cy-r*.7f,cx-r*.05f,cy+r*.7f,p);c.drawRect(cx+r*.05f,cy-r*.7f,cx+r,cy+r*.7f,p);c.drawLine(cx,cy-r*.6f,cx,cy+r*.75f,p);break;case"language":c.drawCircle(cx,cy,r,p);c.drawLine(cx-r,cy,cx+r,cy,p);c.drawOval(new RectF(cx-r*.45f,cy-r,cx+r*.45f,cy+r),p);break;case"briefcase":c.drawRoundRect(new RectF(cx-r,cy-r*.45f,cx+r,cy+r*.65f),r*.15f,r*.15f,p);c.drawRect(cx-r*.35f,cy-r*.75f,cx+r*.35f,cy-r*.45f,p);break;case"health":path.moveTo(cx,cy+r);path.cubicTo(cx-r*1.2f,cy+r*.2f,cx-r*.8f,cy-r,cx,cy-r*.2f);path.cubicTo(cx+r*.8f,cy-r,cx+r*1.2f,cy+r*.2f,cx,cy+r);c.drawPath(path,p);break;case"sleep":c.drawArc(q,80,250,false,p);break;case"mosque":c.drawLine(cx-r,cy+r,cx+r,cy+r,p);c.drawRoundRect(new RectF(cx-r*.65f,cy-r*.05f,cx+r*.65f,cy+r),r*.15f,r*.15f,p);c.drawArc(new RectF(cx-r*.48f,cy-r*.8f,cx+r*.48f,cy+r*.15f),180,180,false,p);break;case"meal":c.drawRect(cx-r*.7f,cy-r*.6f,cx+r*.7f,cy+r*.55f,p);break;case"calendar":c.drawRoundRect(q,r*.18f,r*.18f,p);c.drawLine(cx-r,cy-r*.25f,cx+r,cy-r*.25f,p);break;case"gift":c.drawRect(cx-r,cy-r*.35f,cx+r,cy+r*.75f,p);c.drawLine(cx,cy-r*.35f,cx,cy+r*.75f,p);c.drawLine(cx-r,cy,cx+r,cy,p);break;case"headphones":c.drawArc(q,190,160,false,p);c.drawLine(cx-r,cy,cx-r,cy+r*.7f,p);c.drawLine(cx+r,cy,cx+r,cy+r*.7f,p);break;case"focus":c.drawCircle(cx,cy,r,p);c.drawLine(cx,cy-r,cx,cy-r*.45f,p);break;case"fire":path.moveTo(cx,cy+r);path.cubicTo(cx-r*.8f,cy+r*.1f,cx-r*.2f,cy-r*.2f,cx,cy-r);path.cubicTo(cx+r*.1f,cy-r*.3f,cx+r*.9f,cy+r*.1f,cx,cy+r);c.drawPath(path,p);break;case"star":case"points":drawStar(c,p,cx,cy,r);break;case"block":c.drawCircle(cx,cy,r,p);c.drawLine(cx-r*.7f,cy+r*.7f,cx+r*.7f,cy-r*.7f,p);break;case"planner":case"settings":c.drawRoundRect(q,r*.2f,r*.2f,p);c.drawLine(cx-r*.5f,cy-r*.35f,cx+r*.5f,cy-r*.35f,p);c.drawLine(cx-r*.5f,cy,cx+r*.2f,cy,p);c.drawLine(cx-r*.5f,cy+r*.35f,cx+r*.5f,cy+r*.35f,p);break;case"level":c.drawCircle(cx,cy,r,p);c.drawCircle(cx,cy,r*.45f,p);break;default:c.drawCircle(cx,cy,r,p);}}
    private static void drawStar(Canvas c,Paint p,float cx,float cy,float r){Path x=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float rr=(i%2==0)?r:r*.45f;float px=cx+(float)Math.cos(a)*rr,py=cy+(float)Math.sin(a)*rr;if(i==0)x.moveTo(px,py);else x.lineTo(px,py);}x.close();c.drawPath(x,p);} private static String toArabic(int n){String s=String.valueOf(n),e="0123456789",a="٠١٢٣٤٥٦٧٨٩";for(int i=0;i<10;i++)s=s.replace(e.charAt(i),a.charAt(i));return s;}
}
