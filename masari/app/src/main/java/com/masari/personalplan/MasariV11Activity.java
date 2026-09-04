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
import java.util.Map;

public class MasariV11Activity extends Activity {

    private static final int BG = Color.rgb(247,249,252);
    private static final int CARD = Color.WHITE;
    private static final int NAVY = Color.rgb(8,43,68);
    private static final int GREEN = Color.rgb(25,151,82);
    private static final int GREEN_DARK = Color.rgb(18,128,68);
    private static final int GREEN_SOFT = Color.rgb(231,246,236);
    private static final int TEXT = Color.rgb(24,33,45);
    private static final int MUTED = Color.rgb(112,121,134);
    private static final int BORDER = Color.rgb(230,234,240);
    private static final int GOLD = Color.rgb(229,160,38);
    private static final int BLUE = Color.rgb(60,121,207);
    private static final int ORANGE = Color.rgb(232,141,47);
    private static final int RED = Color.rgb(211,84,75);
    private static final int PURPLE = Color.rgb(132,91,190);
    private static final int TEAL = Color.rgb(48,156,164);
    private static final int PINK = Color.rgb(202,91,137);

    private SharedPreferences prefs;
    private String todayKey;
    private String tab = "home";
    private String filter = "الكل";
    private String rewardTab = "level";
    private final LinkedHashMap<String,Integer> domainColors = new LinkedHashMap<>();

    static class Task {
        String id, title, domain;
        int start, end, points;
        boolean required;
        Task(String id,int start,int end,String title,String domain,int points,boolean required){
            this.id=id; this.start=start; this.end=end; this.title=title; this.domain=domain; this.points=points; this.required=required;
        }
    }

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs=getSharedPreferences("masari_data",MODE_PRIVATE);
        todayKey=dateKey(Calendar.getInstance());
        setupDomains();
        migrateCredits();
        syncCredits();
        render();
    }

    @Override protected void onResume(){
        super.onResume();
        if(prefs!=null){
            todayKey=dateKey(Calendar.getInstance());
            syncCredits();
            render();
        }
    }

    private void setupDomains(){
        domainColors.put("القرآن",GREEN);
        domainColors.put("الإنجليزية والقبول",BLUE);
        domainColors.put("العمل والدخل",ORANGE);
        domainColors.put("الصحة",RED);
        domainColors.put("المعرفة والقراءة",PURPLE);
        domainColors.put("التواصل",TEAL);
        domainColors.put("الأسرة",PINK);
        domainColors.put("الدين والمسجد",Color.rgb(67,130,83));
        domainColors.put("الانضباط",Color.rgb(96,105,118));
    }

    private void migrateCredits(){
        if(!prefs.getBoolean("v11_credit_migration",false)){
            int existing=prefs.getInt("reward_credits",0);
            int stars=prefs.getInt("reward_stars",0);
            prefs.edit().putInt("reward_credits",existing+stars*8).putBoolean("v11_credit_migration",true).apply();
        }
    }

    private void render(){
        LinearLayout shell=new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout body=new FrameLayout(this);
        shell.addView(body,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        View page;
        if("tasks".equals(tab)) page=tasksPage();
        else if("stats".equals(tab)) page=statsPage();
        else if("rewards".equals(tab)) page=rewardsPage();
        else if("more".equals(tab)) page=morePage();
        else page=homePage();

        body.addView(page,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        shell.addView(bottomNav(),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(86)));
        setContentView(shell);
    }

    private View homePage(){
        ScrollView s=scroll();
        LinearLayout r=root(s);

        topBar(r,"مساري");

        LinearLayout hello=new LinearLayout(this);
        hello.setOrientation(LinearLayout.HORIZONTAL);
        hello.setGravity(Gravity.CENTER_VERTICAL);
        hello.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout htxt=new LinearLayout(this);
        htxt.setOrientation(LinearLayout.VERTICAL);
        htxt.addView(text("مرحبًا 👋",24,TEXT,true));
        TextView sub=text(homeMessage(),13,MUTED,false);
        sub.setPadding(0,dp(4),0,0);
        htxt.addView(sub);
        hello.addView(htxt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        TextView avatar=text("م",22,GREEN,true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(GREEN_SOFT,30));
        hello.addView(avatar,new LinearLayout.LayoutParams(dp(58),dp(58)));
        add(r,hello,18);

        section(r,"تقدمك","اليوم حتى الآن");

        LinearLayout pc=card();
        pc.setPadding(dp(14),dp(18),dp(14),dp(13));
        add(r,pc,8);

        LinearLayout rings=new LinearLayout(this);
        rings.setOrientation(LinearLayout.HORIZONTAL);
        rings.setGravity(Gravity.CENTER);
        rings.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addRing(rings,"اليوم",dayRate(Calendar.getInstance()),GREEN);
        addRing(rings,"الأسبوع",weekRate(),BLUE);
        addRing(rings,"الشهر",monthRate(),PURPLE);
        addRing(rings,"السنة",yearRate(),GOLD);
        pc.addView(rings);

        TextView pmsg=center(
                dayRate(Calendar.getInstance())>=80 ? "أحسنت. حافظ على النسق ولا تضف مهامًا جديدة اليوم." : "أنجز المهمة التالية فقط وارفع التقدم خطوة خطوة.",
                12, dayRate(Calendar.getInstance())>=80 ? GREEN : MUTED, true);
        pmsg.setPadding(dp(6),dp(10),dp(6),dp(2));
        pc.addView(pmsg);

        LinearLayout goal=card();
        goal.setPadding(dp(16),dp(15),dp(16),dp(14));
        add(r,goal,14);

        LinearLayout gr=new LinearLayout(this);
        gr.setOrientation(LinearLayout.HORIZONTAL);
        gr.setGravity(Gravity.CENTER_VERTICAL);
        gr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TargetView target=new TargetView(this,GREEN);
        gr.addView(target,new LinearLayout.LayoutParams(dp(54),dp(54)));

        LinearLayout gtx=new LinearLayout(this);
        gtx.setOrientation(LinearLayout.VERTICAL);
        gtx.setPadding(dp(12),0,dp(8),0);
        gtx.addView(text("الهدف اليومي",20,TEXT,true));
        gtx.addView(text("بلوغ ٨٠٪ من نقاط المهام الأساسية مع القرآن والإنجليزية.",12,MUTED,false));
        gr.addView(gtx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        goal.addView(gr);

        int done=countDoneToday(), all=tasksToday().size();
        TextView cnt=text(ar(done)+" من "+ar(all)+" مهام",12,GREEN,true);
        cnt.setPadding(0,dp(11),0,dp(5));
        goal.addView(cnt);
        goal.addView(progress(dayPoints(Calendar.getInstance()),dayTarget(Calendar.getInstance()),GREEN,5));

        section(r,"جدول اليوم","عرض الكل");
        TextView showAll=findActionText(r);
        if(showAll!=null) showAll.setOnClickListener(v->{tab="tasks";render();});

        List<Task> display=homeTasks();
        for(Task t:display) addHomeTask(r,t);

        LinearLayout streakCard=card();
        streakCard.setPadding(dp(16),dp(13),dp(16),dp(13));
        add(r,streakCard,14);
        LinearLayout sr=new LinearLayout(this);
        sr.setOrientation(LinearLayout.HORIZONTAL);
        sr.setGravity(Gravity.CENTER_VERTICAL);
        sr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        DomainIcon fire=new DomainIcon(this,"streak",GOLD);
        sr.addView(fire,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout st=new LinearLayout(this);
        st.setOrientation(LinearLayout.VERTICAL);
        st.setPadding(dp(12),0,dp(8),0);
        st.addView(text("سلسلة الأيام القوية",16,TEXT,true));
        st.addView(text(ar(streak())+" يومًا متتاليًا بنسبة ٨٠٪ فأكثر",11,MUTED,false));
        sr.addView(st,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        streakCard.addView(sr);

        TextView endGap=text(" ",8,MUTED,false);
        endGap.setPadding(0,0,0,dp(16));
        r.addView(endGap);
        return s;
    }

    private List<Task> homeTasks(){
        List<Task> all=tasksToday();
        List<Task> out=new ArrayList<>();
        int now=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60+Calendar.getInstance().get(Calendar.MINUTE);
        for(Task t:all){
            if(t.id.equals("sleep2")||t.id.equals("lunch")||t.id.equals("dinner")) continue;
            if(!done(t) && t.end>=now-30) out.add(t);
            if(out.size()>=5) break;
        }
        if(out.size()<4){
            for(Task t:all){
                if(out.contains(t)||t.id.equals("sleep2")||t.id.equals("lunch")||t.id.equals("dinner")) continue;
                out.add(t);
                if(out.size()>=5) break;
            }
        }
        return out;
    }

    private View tasksPage(){
        ScrollView s=scroll();
        LinearLayout r=root(s);
        topBar(r,"المهام");
        weekStrip(r);

        HorizontalScrollView hs=new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        chips.setPadding(0,dp(13),0,dp(12));
        String[] fs={"الكل","قرآن","إنجليزية","عمل","صحة","قراءة","أسرة"};
        for(String f:fs){
            TextView c=filterChip(f,f.equals(filter));
            chips.addView(c);
            c.setOnClickListener(v->{filter=f;render();});
        }
        hs.addView(chips);
        r.addView(hs);

        LinearLayout h=new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        h.addView(text("مهام اليوم",22,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        h.addView(pill(ar(countDoneToday())+" / "+ar(tasksToday().size()),GREEN,GREEN_SOFT));
        r.addView(h);

        for(Task t:tasksToday()){
            if(matches(t)) addTaskCard(r,t);
        }

        Button add=primaryButton("＋  إضافة مهمة مخصصة");
        LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));
        alp.setMargins(0,dp(18),0,dp(20));
        r.addView(add,alp);
        add.setOnClickListener(v->startActivity(new Intent(this,PlannerCenterActivity.class)));
        return s;
    }

    private View statsPage(){
        ScrollView s=scroll();
        LinearLayout r=root(s);
        topBar(r,"الإحصائيات");

        section(r,"ملخص الشهر","أرقام حقيقية من إنجازاتك");

        LinearLayout summary=card();
        summary.setPadding(dp(14),dp(16),dp(14),dp(14));
        add(r,summary,8);

        LinearLayout sr=new LinearLayout(this);
        sr.setOrientation(LinearLayout.HORIZONTAL);
        sr.setGravity(Gravity.CENTER);
        sr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout ringBox=new LinearLayout(this);
        ringBox.setOrientation(LinearLayout.VERTICAL);
        ringBox.setGravity(Gravity.CENTER);
        ringBox.addView(new RingView(this,monthRate(),GREEN,true),new LinearLayout.LayoutParams(dp(116),dp(116)));
        ringBox.addView(center("التزام الشهر",12,MUTED,true));
        sr.addView(ringBox,new LinearLayout.LayoutParams(0,dp(154),1f));

        LinearLayout nums=new LinearLayout(this);
        nums.setOrientation(LinearLayout.VERTICAL);
        nums.setPadding(dp(12),0,dp(4),0);
        nums.addView(statRow(ar(monthCompleted()),"مهمة مكتملة",GREEN));
        nums.addView(statRow(ar(monthStrongDays()),"يوم قوي",GOLD));
        nums.addView(statRow(formatMinutes(focusMinutes()),"تركيز",BLUE));
        sr.addView(nums,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        summary.addView(sr);

        section(r,"أداء الأسبوع","السبت إلى الجمعة");
        LinearLayout wc=card();
        wc.setPadding(dp(12),dp(13),dp(12),dp(8));
        add(r,wc,7);
        wc.addView(new WeekBars(this),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(188)));

        section(r,"اتجاه آخر ١٤ يومًا","");
        LinearLayout tc=card();
        tc.setPadding(dp(12),dp(13),dp(12),dp(8));
        add(r,tc,7);
        tc.addView(new TrendView(this),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(176)));

        section(r,"تقدم المجالات","كل مجال يتقدم مستقلًا");
        LinearLayout dc=card();
        dc.setPadding(dp(14),dp(8),dp(14),dp(8));
        add(r,dc,7);
        for(String d:domainColors.keySet()) addDomainRow(dc,d);

        LinearLayout metrics=new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        metrics.setPadding(0,dp(12),0,0);
        metrics.addView(metricCard("points",ar(totalPoints()),"نقاط التقدم",GOLD),weight());
        metrics.addView(metricCard("book",ar(monthDone("leap1")+monthDone("leap2")+monthDone("leap3")),"كتب LeapAhead",PURPLE),weight());
        metrics.addView(metricCard("quran",ar(monthDone("quran1")+monthDone("quran2")),"جلسات القرآن",GREEN),weight());
        r.addView(metrics);

        LinearLayout blocker=card();
        blocker.setPadding(dp(16),dp(13),dp(16),dp(13));
        add(r,blocker,12);
        blocker.addView(text("أكثر عائق مسجل",14,TEXT,true));
        TextView b=text(topBlocker(),18,RED,true);
        b.setPadding(0,dp(4),0,dp(4));
        blocker.addView(b);
        blocker.addView(text(blockerAdvice(),11,MUTED,false));
        blocker.setOnClickListener(v->startActivity(new Intent(this,WeeklyPlannerActivity.class)));

        return s;
    }

    private View rewardsPage(){
        ScrollView s=scroll();
        LinearLayout r=root(s);
        topBar(r,"المكافآت");

        int total=totalPoints();
        int level=levelFor(total);
        int base=levelBase(level);
        int next=levelNext(level);

        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        hero.setPadding(dp(18),dp(18),dp(18),dp(17));
        hero.setBackground(round(NAVY,28));
        add(r,hero,12);

        LinearLayout hr=new LinearLayout(this);
        hr.setOrientation(LinearLayout.HORIZONTAL);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        hr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout htxt=new LinearLayout(this);
        htxt.setOrientation(LinearLayout.VERTICAL);
        htxt.addView(text("إجمالي نقاط التقدم",12,Color.rgb(189,210,224),false));
        TextView big=text(ar(total),34,Color.WHITE,true);
        big.setPadding(0,dp(2),0,dp(4));
        htxt.addView(big);
        htxt.addView(text("المستوى "+ar(level)+" • "+levelName(level),15,Color.WHITE,true));
        hr.addView(htxt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        hr.addView(new BadgeView(this,GOLD,"★"),new LinearLayout.LayoutParams(dp(86),dp(86)));
        hero.addView(hr);
        hero.addView(progress(Math.max(0,total-base),Math.max(1,next-base),GREEN,6));
        TextView nxt=text(level>=6?"وصلت إلى أعلى مستوى حاليًا":ar(Math.max(0,next-total))+" نقطة للمستوى التالي",11,Color.rgb(188,218,199),true);
        nxt.setPadding(0,dp(7),0,0);
        hero.addView(nxt);

        LinearLayout tabs=segmentedTabs();
        add(r,tabs,12);

        if("badges".equals(rewardTab)) rewardBadges(r);
        else if("achievements".equals(rewardTab)) rewardAchievements(r);
        else if("store".equals(rewardTab)) rewardStore(r);
        else rewardLevel(r);

        return s;
    }

    private void rewardLevel(LinearLayout r){
        int total=totalPoints(), level=levelFor(total), next=levelNext(level);

        LinearLayout levelCard=card();
        levelCard.setPadding(dp(18),dp(17),dp(18),dp(17));
        add(r,levelCard,14);
        LinearLayout lr=new LinearLayout(this);
        lr.setOrientation(LinearLayout.HORIZONTAL);
        lr.setGravity(Gravity.CENTER_VERTICAL);
        lr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        lr.addView(new BadgeView(this,GREEN,ar(level)),new LinearLayout.LayoutParams(dp(82),dp(82)));
        LinearLayout lt=new LinearLayout(this);
        lt.setOrientation(LinearLayout.VERTICAL);
        lt.setPadding(dp(14),0,0,0);
        lt.addView(text("المستوى الحالي",13,MUTED,false));
        lt.addView(text(levelName(level),26,NAVY,true));
        lt.addView(text(level>=6?"أعلى مستوى حاليًا":"بقي "+ar(Math.max(0,next-total))+" نقطة للمستوى التالي",11,MUTED,false));
        lr.addView(lt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        levelCard.addView(lr);

        section(r,"أوسمة المجالات","تتقدم بحسب نقاط المجال");

        HorizontalScrollView hs=new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout badges=new LinearLayout(this);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        badges.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        badges.setPadding(0,dp(3),0,dp(5));
        String[] first={"القرآن","الإنجليزية والقبول","العمل والدخل","الصحة","الانضباط"};
        for(String d:first) badges.addView(domainBadgeTile(d));
        hs.addView(badges);
        r.addView(hs);

        section(r,"إنجازات قريبة","");
        addAchievementCard(r,"أسبوع ثابت","٥ أيام قوية خلال أسبوع",strongThisWeek(),5,20);
        addAchievementCard(r,"قارئ مستمر","٣٠ كتاب LeapAhead",countDoneIds("leap"),30,25);
        addAchievementCard(r,"مراجعة راسخة","٢٠ جلسة قرآن",countDoneIds("quran"),20,20);

        section(r,"رصيد المكافآت","يُصرف ولا ينقص نقاط تقدمك");
        LinearLayout wallet=card();
        wallet.setPadding(dp(16),dp(14),dp(16),dp(14));
        add(r,wallet,7);
        LinearLayout wr=new LinearLayout(this);
        wr.setOrientation(LinearLayout.HORIZONTAL);
        wr.setGravity(Gravity.CENTER_VERTICAL);
        wr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        wr.addView(new DomainIcon(this,"points",GOLD),new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout wt=new LinearLayout(this);
        wt.setOrientation(LinearLayout.VERTICAL);
        wt.setPadding(dp(12),0,0,0);
        wt.addView(text(ar(credits())+" رصيد",25,NAVY,true));
        wt.addView(text("تكسبه من الأيام القوية والإنجازات، وتستخدمه للامتيازات.",11,MUTED,false));
        wr.addView(wt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        wallet.addView(wr);
    }

    private void rewardBadges(LinearLayout r){
        section(r,"كل الأوسمة","الرتبة الحالية والتالية");
        for(String d:domainColors.keySet()){
            int p=domainPoints(d);
            int tier=badgeTier(p);
            int next=badgeNext(tier);
            LinearLayout c=card();
            c.setPadding(dp(14),dp(13),dp(14),dp(13));
            add(r,c,8);
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.addView(new BadgeView(this,color(d),domainShort(d)),new LinearLayout.LayoutParams(dp(66),dp(66)));
            LinearLayout tx=new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPadding(dp(12),0,0,0);
            tx.addView(text(shortDomain(d),17,TEXT,true));
            tx.addView(text(badgeName(tier)+" • "+ar(p)+" نقطة",11,MUTED,false));
            row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
            c.addView(row);
            c.addView(progress(Math.min(p,next),Math.max(1,next),color(d),4));
            TextView n=text(tier>=4?"أعلى وسام للمجال":"التالي عند "+ar(next)+" نقطة",10,MUTED,false);
            n.setPadding(0,dp(4),0,0);
            c.addView(n);
        }
    }

    private void rewardAchievements(LinearLayout r){
        section(r,"الإنجازات","تُمنح مرة واحدة عند تحققها");
        addAchievementCard(r,"سلسلة قوية","٧ أيام متتالية فوق ٨٠٪",streak(),7,30);
        addAchievementCard(r,"مراجع قرآن","٢٠ جلسة قرآن",countDoneIds("quran"),20,20);
        addAchievementCard(r,"إنجليزية ثابتة","١٥ جلسة إنجليزية",countDoneExact("english"),15,20);
        addAchievementCard(r,"عمل حقيقي","١٠ جلسات عمل",countDoneExact("work"),10,20);
        addAchievementCard(r,"جسم أقوى","٨ جلسات تدريب",countDoneIds("workout"),8,25);
        addAchievementCard(r,"قارئ نشط","٣٠ كتاب LeapAhead",countDoneIds("leap"),30,25);
    }

    private void rewardStore(LinearLayout r){
        section(r,"متجر المكافآت","امتيازات وقت وهواية فقط");
        LinearLayout balance=card();
        balance.setPadding(dp(15),dp(12),dp(15),dp(12));
        add(r,balance,7);
        LinearLayout br=new LinearLayout(this);
        br.setOrientation(LinearLayout.HORIZONTAL);
        br.setGravity(Gravity.CENTER_VERTICAL);
        br.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        br.addView(text("رصيدك",14,MUTED,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        br.addView(pill(ar(credits())+" رصيد",GREEN,GREEN_SOFT));
        balance.addView(br);

        addRewardCard(r,"ترفيه إضافي ٣٠ دقيقة","لعبة أو أنمي بعد إنهاء الأساسيات.",25,"fun30","play");
        addRewardCard(r,"جلسة استكشاف ٤٥ دقيقة","موضوع علمي أو تقني تحبه دون شعور بالذنب.",35,"explore45","discover");
        addRewardCard(r,"جلسة هواية إضافية","كرة، طبخ، رسم، شطرنج أو مهارة تختارها.",45,"hobby","hobby");
        addRewardCard(r,"مساء خفيف","إلغاء مهمة إضافية واحدة فقط في مساء مناسب.",60,"light_evening","moon");
        addRewardCard(r,"ترفيه ممتد ٩٠ دقيقة","جلسة أطول في يوم مناسب، لا تُؤخذ من النوم.",90,"fun90","play");
        addRewardCard(r,"نصف يوم مرن","بعد أسبوع قوي؛ تبقى الصلاة والقرآن والنوم محفوظة.",120,"half_flex","calendar");

        TextView note=text("النوم، الطعام، الراحة الأساسية، الصلاة والقرآن ليست مكافآت تُشترى؛ هي أساس ثابت.",11,MUTED,false);
        note.setPadding(dp(8),dp(12),dp(8),dp(20));
        r.addView(note);
    }

    private View morePage(){
        ScrollView s=scroll();
        LinearLayout r=root(s);
        topBar(r,"المزيد");

        section(r,"التخطيط والمتابعة","");
        addMenuCard(r,"calendar","إدارة الأسبوع","التقويم، التأجيل، التعثر والعوائق",GREEN,()->startActivity(new Intent(this,WeeklyPlannerActivity.class)));
        addMenuCard(r,"tasks","مركز التخطيط","المهام المخصصة، المراجعات والنسخ الاحتياطي",BLUE,()->startActivity(new Intent(this,PlannerCenterActivity.class)));

        section(r,"ملخص حسابك","");
        LinearLayout account=card();
        account.setPadding(dp(16),dp(14),dp(16),dp(14));
        add(r,account,7);
        addKeyValue(account,"الإصدار","0.11.0");
        addKeyValue(account,"إجمالي نقاط التقدم",ar(totalPoints()));
        addKeyValue(account,"رصيد المكافآت",ar(credits()));
        addKeyValue(account,"السلسلة الحالية",ar(streak())+" يوم");
        addKeyValue(account,"أكثر عائق",topBlocker());

        section(r,"فلسفة مساري","");
        LinearLayout note=card();
        note.setPadding(dp(16),dp(14),dp(16),dp(14));
        add(r,note,7);
        note.addView(text("• النقاط تقيس التقدم ولا تُصرف.",12,TEXT,false));
        note.addView(text("• رصيد المكافآت وحده هو الذي يُصرف.",12,TEXT,false));
        note.addView(text("• ٨٠٪ يوم قوي؛ لا تحتاج الكمال كل يوم.",12,TEXT,false));
        note.addView(text("• المهام الإضافية لا تفسد نسبة يومك إذا لم تنفذها.",12,TEXT,false));
        note.addView(text("• لا توجد مقارنة وهمية مع أشخاص آخرين.",12,TEXT,false));
        return s;
    }

    private LinearLayout bottomNav(){
        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        nav.setPadding(dp(6),dp(7),dp(6),dp(7));
        nav.setBackgroundColor(NAVY);

        addNav(nav,"home","الرئيسية");
        addNav(nav,"tasks","المهام");
        addNav(nav,"stats","الإحصائيات");
        addNav(nav,"rewards","المكافآت");
        addNav(nav,"more","المزيد");
        return nav;
    }

    private void addNav(LinearLayout nav,String key,String label){
        boolean active=tab.equals(key);
        LinearLayout item=new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(5),dp(5),dp(5),dp(4));
        if(active) item.setBackground(round(GREEN,22));

        NavIcon iv=new NavIcon(this,key,Color.WHITE);
        item.addView(iv,new LinearLayout.LayoutParams(dp(28),dp(28)));
        TextView t=center(label,11,Color.WHITE,active);
        t.setPadding(0,dp(3),0,0);
        item.addView(t);

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(72),1f);
        lp.setMargins(dp(3),0,dp(3),0);
        nav.addView(item,lp);
        item.setOnClickListener(v->{tab=key;render();});
    }

    private void topBar(LinearLayout r,String title){
        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout right=new FrameLayout(this);
        NavIcon bell=new NavIcon(this,"bell",NAVY);
        FrameLayout.LayoutParams bip=new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER);
        right.addView(bell,bip);
        right.setOnClickListener(v->startActivity(new Intent(this,WeeklyPlannerActivity.class)));
        bar.addView(right,new LinearLayout.LayoutParams(dp(50),dp(50)));

        TextView name=center(title,30,title.equals("مساري")?GREEN:TEXT,true);
        bar.addView(name,new LinearLayout.LayoutParams(0,dp(54),1f));

        FrameLayout left=new FrameLayout(this);
        NavIcon menu=new NavIcon(this,"menu",NAVY);
        FrameLayout.LayoutParams mip=new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER);
        left.addView(menu,mip);
        left.setOnClickListener(v->{tab="more";render();});
        bar.addView(left,new LinearLayout.LayoutParams(dp(50),dp(50)));

        r.addView(bar);
    }

    private void addHomeTask(LinearLayout r,Task t){
        LinearLayout c=card();
        c.setPadding(dp(13),dp(11),dp(13),dp(11));
        add(r,c,7);

        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        CheckView check=new CheckView(this,done(t),color(t.domain));
        row.addView(check,new LinearLayout.LayoutParams(dp(42),dp(42)));

        FrameLayout iconWrap=new FrameLayout(this);
        iconWrap.setBackground(round(soft(color(t.domain)),23));
        DomainIcon di=new DomainIcon(this,iconKey(t.domain),color(t.domain));
        iconWrap.addView(di,new FrameLayout.LayoutParams(dp(30),dp(30),Gravity.CENTER));
        LinearLayout.LayoutParams iwp=new LinearLayout.LayoutParams(dp(48),dp(48));
        iwp.setMargins(dp(9),0,dp(10),0);
        row.addView(iconWrap,iwp);

        LinearLayout tx=new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(text(t.title,15,TEXT,true));
        tx.addView(text(format12(t.start)+" • "+shortDomain(t.domain),11,MUTED,false));
        row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        c.addView(row);
        View.OnClickListener toggle=v->{setDone(t,!done(t));render();};
        check.setOnClickListener(toggle);
        c.setOnClickListener(toggle);
    }

    private void addTaskCard(LinearLayout r,Task t){
        LinearLayout c=card();
        c.setPadding(dp(14),dp(12),dp(14),dp(12));
        add(r,c,8);

        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        CheckView check=new CheckView(this,done(t),color(t.domain));
        row.addView(check,new LinearLayout.LayoutParams(dp(44),dp(44)));

        FrameLayout iw=new FrameLayout(this);
        iw.setBackground(round(soft(color(t.domain)),24));
        iw.addView(new DomainIcon(this,iconKey(t.domain),color(t.domain)),new FrameLayout.LayoutParams(dp(31),dp(31),Gravity.CENTER));
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(dp(50),dp(50));
        ilp.setMargins(dp(9),0,dp(11),0);
        row.addView(iw,ilp);

        LinearLayout tx=new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(text(t.title,15,TEXT,true));
        TextView info=text(format12(t.start)+" - "+format12(t.end)+" • "+shortDomain(t.domain),11,MUTED,false);
        info.setPadding(0,dp(3),0,0);
        tx.addView(info);
        row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        TextView points=pill("+"+ar(t.points),color(t.domain),soft(color(t.domain)));
        row.addView(points);
        c.addView(row);

        View.OnClickListener toggle=v->{setDone(t,!done(t));render();};
        check.setOnClickListener(toggle);
    }

    private View domainBadgeTile(String d){
        LinearLayout c=card();
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(9),dp(12),dp(9),dp(10));
        int tier=badgeTier(domainPoints(d));
        c.addView(new BadgeView(this,color(d),domainShort(d)),new LinearLayout.LayoutParams(dp(66),dp(66)));
        TextView n=center(shortDomain(d),11,TEXT,true);
        n.setPadding(0,dp(7),0,0);
        c.addView(n);
        c.addView(center(badgeName(tier),9,MUTED,false));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(118),dp(126));
        lp.setMargins(dp(4),0,dp(4),0);
        c.setLayoutParams(lp);
        return c;
    }

    private void addDomainRow(LinearLayout parent,String d){
        int pts=domainPoints(d), tier=badgeTier(pts), next=badgeNext(tier);
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0,dp(9),0,dp(9));

        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout iw=new FrameLayout(this);
        iw.setBackground(round(soft(color(d)),20));
        iw.addView(new DomainIcon(this,iconKey(d),color(d)),new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));
        top.addView(iw,new LinearLayout.LayoutParams(dp(40),dp(40)));

        LinearLayout tx=new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(10),0,0,0);
        tx.addView(text(shortDomain(d),13,TEXT,true));
        tx.addView(text(badgeName(tier)+" • "+ar(pts)+" نقطة",9,MUTED,false));
        top.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        top.addView(text(tier>=4?"مكتمل":ar(Math.max(0,next-pts))+" للتالي",9,color(d),true));
        row.addView(top);
        row.addView(progress(Math.min(pts,next),Math.max(1,next),color(d),4));
        parent.addView(row);
    }

    private void addAchievementCard(LinearLayout r,String title,String desc,int value,int target,int reward){
        boolean done=value>=target;
        LinearLayout c=card();
        c.setPadding(dp(14),dp(11),dp(14),dp(11));
        if(!done)c.setAlpha(.72f);
        add(r,c,7);

        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout iw=new FrameLayout(this);
        iw.setBackground(round(done?soft(GOLD):Color.rgb(242,244,247),22));
        iw.addView(new DomainIcon(this,"medal",done?GOLD:MUTED),new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER));
        row.addView(iw,new LinearLayout.LayoutParams(dp(46),dp(46)));

        LinearLayout tx=new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(11),0,dp(6),0);
        tx.addView(text(title,14,done?TEXT:MUTED,true));
        tx.addView(text(desc,10,MUTED,false));
        row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        LinearLayout end=new LinearLayout(this);
        end.setOrientation(LinearLayout.VERTICAL);
        end.setGravity(Gravity.CENTER);
        end.addView(center(ar(Math.min(value,target))+"/"+ar(target),10,done?GREEN:MUTED,true));
        end.addView(center("+"+ar(reward)+" رصيد",9,done?GOLD:MUTED,true));
        row.addView(end);
        c.addView(row);
    }

    private void addRewardCard(LinearLayout r,String title,String sub,int cost,String id,String icon){
        LinearLayout c=card();
        c.setPadding(dp(14),dp(12),dp(14),dp(12));
        add(r,c,8);

        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout iw=new FrameLayout(this);
        iw.setBackground(round(GREEN_SOFT,23));
        iw.addView(new DomainIcon(this,icon,GREEN),new FrameLayout.LayoutParams(dp(29),dp(29),Gravity.CENTER));
        row.addView(iw,new LinearLayout.LayoutParams(dp(48),dp(48)));

        LinearLayout tx=new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(11),0,dp(8),0);
        tx.addView(text(title,14,TEXT,true));
        tx.addView(text(sub,10,MUTED,false));
        row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        TextView buy=pill(ar(cost)+" رصيد",GREEN,GREEN_SOFT);
        buy.setPadding(dp(11),dp(9),dp(11),dp(9));
        row.addView(buy);
        c.addView(row);

        buy.setOnClickListener(v->redeem(title,cost,id));
    }

    private void addMenuCard(LinearLayout r,String icon,String title,String sub,int c,Runnable action){
        LinearLayout box=card();
        box.setPadding(dp(14),dp(12),dp(14),dp(12));
        add(r,box,8);
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        FrameLayout iw=new FrameLayout(this);
        iw.setBackground(round(soft(c),23));
        iw.addView(new DomainIcon(this,icon,c),new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER));
        row.addView(iw,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout tx=new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(11),0,dp(8),0);
        tx.addView(text(title,15,TEXT,true));
        tx.addView(text(sub,10,MUTED,false));
        row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        TextView arrow=center("‹",26,MUTED,false);
        row.addView(arrow,new LinearLayout.LayoutParams(dp(28),dp(42)));
        box.addView(row);
        box.setOnClickListener(v->action.run());
    }

    private LinearLayout segmentedTabs(){
        LinearLayout seg=new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setGravity(Gravity.CENTER);
        seg.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        seg.setPadding(dp(4),dp(4),dp(4),dp(4));
        seg.setBackground(round(Color.WHITE,22,BorderSpec.ONE));

        String[][] items={{"المستوى","level"},{"الأوسمة","badges"},{"الإنجازات","achievements"},{"المتجر","store"}};
        for(String[] x:items){
            boolean active=rewardTab.equals(x[1]);
            TextView t=center(x[0],12,active?Color.WHITE:TEXT,true);
            t.setBackground(active?round(GREEN,18):round(Color.TRANSPARENT,18));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1f);
            lp.setMargins(dp(2),0,dp(2),0);
            seg.addView(t,lp);
            t.setOnClickListener(v->{rewardTab=x[1];render();});
        }
        return seg;
    }

    private void weekStrip(LinearLayout r){
        Calendar start=saturdayStart(Calendar.getInstance());
        LinearLayout strip=card();
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER);
        strip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        strip.setPadding(dp(7),dp(7),dp(7),dp(7));
        add(r,strip,14);

        for(int i=0;i<7;i++){
            Calendar d=(Calendar)start.clone();
            d.add(Calendar.DAY_OF_MONTH,i);
            boolean active=dateKey(d).equals(todayKey);

            LinearLayout x=new LinearLayout(this);
            x.setOrientation(LinearLayout.VERTICAL);
            x.setGravity(Gravity.CENTER);
            x.setPadding(dp(4),dp(5),dp(4),dp(5));
            if(active)x.setBackground(round(GREEN,17));

            String day=new SimpleDateFormat("EEE",new Locale("ar")).format(d.getTime()).replace("،","");
            x.addView(center(day,9,active?Color.WHITE:MUTED,false));
            x.addView(center(ar(d.get(Calendar.DAY_OF_MONTH)),13,active?Color.WHITE:TEXT,true));

            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1f);
            lp.setMargins(dp(2),0,dp(2),0);
            strip.addView(x,lp);
        }
    }

    private void syncCredits(){
        SharedPreferences.Editor e=prefs.edit();
        int credits=prefs.getInt("reward_credits",0);
        Calendar now=Calendar.getInstance();
        int rate=dayRate(now);
        String day=dateKey(now);

        if(rate>=80&&!prefs.getBoolean("v11_credit80_"+day,false)){
            credits+=8; e.putBoolean("v11_credit80_"+day,true);
        }
        if(rate>=100&&!prefs.getBoolean("v11_credit100_"+day,false)){
            credits+=4; e.putBoolean("v11_credit100_"+day,true);
        }

        String wk=weekKey(now);
        if(strongThisWeek()>=5&&!prefs.getBoolean("v11_week_"+wk,false)){
            credits+=20; e.putBoolean("v11_week_"+wk,true);
        }

        credits=grantAchievement(e,credits,"streak7",streak(),7,30);
        credits=grantAchievement(e,credits,"quran20",countDoneIds("quran"),20,20);
        credits=grantAchievement(e,credits,"english15",countDoneExact("english"),15,20);
        credits=grantAchievement(e,credits,"work10",countDoneExact("work"),10,20);
        credits=grantAchievement(e,credits,"workout8",countDoneIds("workout"),8,25);
        credits=grantAchievement(e,credits,"leap30",countDoneIds("leap"),30,25);

        e.putInt("reward_credits",credits).apply();
    }

    private int grantAchievement(SharedPreferences.Editor e,int credits,String id,int value,int target,int amount){
        String key="v11_ach_"+id;
        if(value>=target&&!prefs.getBoolean(key,false)){
            e.putBoolean(key,true);
            return credits+amount;
        }
        return credits;
    }

    private int credits(){return prefs.getInt("reward_credits",0);}

    private void redeem(String title,int cost,String id){
        int c=credits();
        if(c<cost){
            Toast.makeText(this,"رصيدك غير كافٍ",Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("استخدام المكافأة")
                .setMessage(title+"\n\nسيُخصم "+ar(cost)+" من رصيد المكافآت فقط. نقاط تقدمك لن تتغير.")
                .setPositiveButton("استخدام",(d,w)->{
                    prefs.edit()
                            .putInt("reward_credits",c-cost)
                            .putLong("v11_redeem_"+id+"_"+System.currentTimeMillis(),System.currentTimeMillis())
                            .apply();
                    Toast.makeText(this,"تم استخدام المكافأة",Toast.LENGTH_SHORT).show();
                    render();
                })
                .setNegativeButton("إلغاء",null).show();
    }

    private List<Task> tasksToday(){return tasksFor(Calendar.getInstance());}

    private List<Task> tasksFor(Calendar d){
        List<Task> a=new ArrayList<>();
        int day=d.get(Calendar.DAY_OF_WEEK);

        a.add(t("fajr",240,390,"الفجر • المسجد • التحفيظ • الدرس","الدين والمسجد",5,true));
        if(day==Calendar.SATURDAY) a.add(t("workoutA",390,420,"تمرين A — كتف وذراعان + جسم كامل","الصحة",20,true));
        else if(day==Calendar.TUESDAY) a.add(t("workoutB",390,420,"تمرين B — أوتار وقبضة وسرعة","الصحة",20,true));

        a.add(t("english",420,480,englishTitle(day),"الإنجليزية والقبول",day==Calendar.FRIDAY?10:25,day!=Calendar.FRIDAY));
        a.add(t("sleep2",480,570,"نوم تكميلي","الصحة",5,true));
        a.add(t("leap1",570,600,"LeapAhead — الكتاب ١","المعرفة والقراءة",7,true));
        a.add(t("work",600,690,workTitle(day),"العمل والدخل",day==Calendar.FRIDAY?10:25,day!=Calendar.FRIDAY));
        a.add(t("quran1",705,780,"القرآن — مراجعة جديدة: صفحتان","القرآن",14,true));
        a.add(t("lunch",780,810,"الغداء","الانضباط",3,true));
        a.add(t("leap2",810,855,"LeapAhead — الكتاب ٢","المعرفة والقراءة",8,true));
        a.add(t("quran2",885,970,"القرآن — صفحتان + مراجعة قديمة","القرآن",18,true));
        a.add(afternoon(day));
        a.add(t("maghrib",1080,1200,"المغرب • التحفيظ • العشاء","الدين والمسجد",5,true));
        a.add(t("dinner",1200,1230,"العشاء مع الأسرة","الأسرة",3,true));
        a.add(t("leap3",1230,1275,"LeapAhead — الكتاب ٣","المعرفة والقراءة",7,false));
        a.add(t("close",1320,1340,"إغلاق اليوم وتحديد مهام الغد","الانضباط",5,true));
        a.add(t("sleep",1350,1360,"الاستعداد للنوم","الصحة",5,true));

        addCustom(a,d);
        return a;
    }

    private Task afternoon(int day){
        if(day==Calendar.SATURDAY) return t("family_talk",990,1020,"الأسرة + تدريب كلام قصير","الأسرة",8,true);
        if(day==Calendar.SUNDAY) return t("talk1",990,1020,"تدريب التواصل","التواصل",10,true);
        if(day==Calendar.MONDAY) return t("family_friend",990,1020,"خدمة الأسرة + تفقد صديق","الأسرة",8,true);
        if(day==Calendar.TUESDAY) return t("talk2",990,1020,"تدريب الكلام والحزم","التواصل",10,true);
        if(day==Calendar.WEDNESDAY) return t("medicine",990,1050,"مراجعة طب قديم","المعرفة والقراءة",12,true);
        if(day==Calendar.THURSDAY) return t("khatera",990,1030,"خاطرة دينية + تدريب إلقاء","التواصل",12,true);
        return t("explore",990,1050,"استكشاف علمي أو مهارة حياة","المعرفة والقراءة",10,false);
    }

    private String englishTitle(int day){
        if(day==Calendar.SATURDAY) return "الإنجليزية — Vocabulary + Reading Explorer";
        if(day==Calendar.SUNDAY) return "الإنجليزية — Vocabulary + Tactics";
        if(day==Calendar.MONDAY) return "الإنجليزية — Vocabulary + Reading Explorer";
        if(day==Calendar.TUESDAY) return "الإنجليزية — Vocabulary + Tactics";
        if(day==Calendar.WEDNESDAY) return "الإنجليزية — Vocabulary + Oxford Bookworms";
        if(day==Calendar.THURSDAY) return "اختبار الإنجليزية الأسبوعي";
        return "استماع إنجليزي ممتع — يوم خفيف";
    }

    private String workTitle(int day){
        if(day==Calendar.SATURDAY||day==Calendar.SUNDAY) return "العمل — تطوير الأكاديمية";
        if(day==Calendar.MONDAY||day==Calendar.TUESDAY) return "العمل — الوصول للسوق";
        if(day==Calendar.WEDNESDAY) return "العمل — دخل مباشر";
        if(day==Calendar.THURSDAY) return "العمل — مراجعة الأرقام";
        return "مراجعة مالية خفيفة";
    }

    private void addCustom(List<Task> a,Calendar d){
        try{
            JSONArray arr=new JSONArray(prefs.getString("custom_tasks","[]"));
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.optJSONObject(i);
                if(o==null||!o.optBoolean("active",true)) continue;
                int day=o.optInt("day",0);
                if(day!=0&&day!=d.get(Calendar.DAY_OF_WEEK)) continue;
                a.add(t(o.optString("id","custom_"+i),o.optInt("start",960),o.optInt("end",990),
                        o.optString("title","مهمة مخصصة"),o.optString("domain","الانضباط"),
                        o.optInt("points",10),o.optBoolean("required",false)));
            }
        }catch(Exception ignored){}
    }

    private Task t(String id,int start,int end,String title,String domain,int points,boolean required){
        return new Task(id,start,end,title,domain,points,required);
    }

    private boolean done(Task t){
        return prefs.getBoolean("reward_done_"+todayKey+"_"+t.id,false);
    }

    private void setDone(Task t,boolean value){
        boolean old=done(t);
        if(old==value)return;

        int delta=value?t.points:-t.points;
        String dayKey="reward_day_points_"+todayKey;
        String domainKey="reward_domain_"+t.domain;
        int newDay=Math.max(0,prefs.getInt(dayKey,0)+delta);
        int newDomain=Math.max(0,prefs.getInt(domainKey,0)+delta);

        prefs.edit()
                .putBoolean("reward_done_"+todayKey+"_"+t.id,value)
                .putInt(dayKey,newDay)
                .putInt(domainKey,newDomain)
                .apply();

        if(value)Toast.makeText(this,"+"+ar(t.points)+" نقطة",Toast.LENGTH_SHORT).show();
        syncCredits();
    }

    private int dayPoints(Calendar c){return prefs.getInt("reward_day_points_"+dateKey(c),0);}

    private int dayTarget(Calendar c){
        int n=0;
        for(Task t:tasksFor(c))if(t.required)n+=t.points;
        return Math.max(1,n);
    }

    private int dayRate(Calendar c){return pct(dayPoints(c),dayTarget(c));}

    private int weekRate(){
        Calendar c=saturdayStart(Calendar.getInstance());
        int p=0,t=0;
        for(int i=0;i<7;i++){
            p+=dayPoints(c); t+=dayTarget(c); c.add(Calendar.DAY_OF_MONTH,1);
        }
        return pct(p,t);
    }

    private int monthRate(){
        Calendar c=Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,1);
        int month=c.get(Calendar.MONTH),p=0,t=0;
        while(c.get(Calendar.MONTH)==month){
            p+=dayPoints(c); t+=dayTarget(c); c.add(Calendar.DAY_OF_MONTH,1);
        }
        return pct(p,t);
    }

    private int yearRate(){
        Calendar start=Calendar.getInstance();
        start.set(2026,Calendar.SEPTEMBER,1,0,0,0);
        Calendar end=Calendar.getInstance();
        end.set(2027,Calendar.MAY,31,23,59,59);
        Calendar now=Calendar.getInstance();
        if(now.before(start))return 0;
        Calendar c=(Calendar)start.clone();
        int p=0,t=0;
        while(!c.after(now)&&!c.after(end)){
            p+=dayPoints(c); t+=dayTarget(c); c.add(Calendar.DAY_OF_MONTH,1);
        }
        return pct(p,t);
    }

    private int countDoneToday(){
        int n=0;
        for(Task t:tasksToday())if(done(t))n++;
        return n;
    }

    private int totalPoints(){
        int n=0;
        for(String d:domainColors.keySet())n+=domainPoints(d);
        return n;
    }

    private int domainPoints(String d){return prefs.getInt("reward_domain_"+d,0);}

    private int monthDone(String id){
        Calendar c=Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,1);
        int m=c.get(Calendar.MONTH),n=0;
        while(c.get(Calendar.MONTH)==m){
            if(prefs.getBoolean("reward_done_"+dateKey(c)+"_"+id,false))n++;
            c.add(Calendar.DAY_OF_MONTH,1);
        }
        return n;
    }

    private int monthCompleted(){
        Calendar c=Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,1);
        int m=c.get(Calendar.MONTH),n=0;
        while(c.get(Calendar.MONTH)==m){
            String k=dateKey(c);
            for(Task t:tasksFor(c)){
                if(prefs.getBoolean("reward_done_"+k+"_"+t.id,false))n++;
            }
            c.add(Calendar.DAY_OF_MONTH,1);
        }
        return n;
    }

    private int monthStrongDays(){
        Calendar c=Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,1);
        int m=c.get(Calendar.MONTH),n=0;
        while(c.get(Calendar.MONTH)==m){
            if(dayRate(c)>=80)n++;
            c.add(Calendar.DAY_OF_MONTH,1);
        }
        return n;
    }

    private int focusMinutes(){
        Calendar c=Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,1);
        int m=c.get(Calendar.MONTH),sum=0;
        while(c.get(Calendar.MONTH)==m){
            String k=dateKey(c);
            for(Task t:tasksFor(c)){
                boolean focusDomain=t.domain.equals("الإنجليزية والقبول")||t.domain.equals("العمل والدخل")||t.domain.equals("القرآن")||t.domain.equals("المعرفة والقراءة");
                if(focusDomain&&prefs.getBoolean("reward_done_"+k+"_"+t.id,false))sum+=Math.max(0,t.end-t.start);
            }
            c.add(Calendar.DAY_OF_MONTH,1);
        }
        return sum;
    }

    private int countDoneExact(String id){
        int n=0;
        for(Map.Entry<String,?> e:prefs.getAll().entrySet()){
            if(e.getKey().startsWith("reward_done_")&&e.getKey().endsWith("_"+id)&&Boolean.TRUE.equals(e.getValue()))n++;
        }
        return n;
    }

    private int countDoneIds(String contains){
        int n=0;
        for(Map.Entry<String,?> e:prefs.getAll().entrySet()){
            if(e.getKey().startsWith("reward_done_")&&e.getKey().contains("_"+contains)&&Boolean.TRUE.equals(e.getValue()))n++;
        }
        return n;
    }

    private int streak(){
        Calendar c=Calendar.getInstance();
        if(dayRate(c)<80)c.add(Calendar.DAY_OF_MONTH,-1);
        int n=0;
        for(int i=0;i<365;i++){
            if(dayRate(c)>=80){n++;c.add(Calendar.DAY_OF_MONTH,-1);}
            else break;
        }
        return n;
    }

    private int strongThisWeek(){
        Calendar c=saturdayStart(Calendar.getInstance());
        int n=0;
        for(int i=0;i<7;i++){
            if(dayRate(c)>=80)n++;
            c.add(Calendar.DAY_OF_MONTH,1);
        }
        return n;
    }

    private String weekKey(Calendar c){
        Calendar s=saturdayStart(c);
        return dateKey(s);
    }

    private int levelFor(int p){
        if(p>=8000)return 6;
        if(p>=5000)return 5;
        if(p>=3000)return 4;
        if(p>=1500)return 3;
        if(p>=600)return 2;
        return 1;
    }

    private int levelBase(int l){
        if(l==2)return 600;
        if(l==3)return 1500;
        if(l==4)return 3000;
        if(l==5)return 5000;
        if(l==6)return 8000;
        return 0;
    }

    private int levelNext(int l){
        if(l==1)return 600;
        if(l==2)return 1500;
        if(l==3)return 3000;
        if(l==4)return 5000;
        if(l==5)return 8000;
        return 8000;
    }

    private String levelName(int l){
        if(l==2)return "منطلق";
        if(l==3)return "ثابت";
        if(l==4)return "متقدم";
        if(l==5)return "متمكن";
        if(l==6)return "راسخ";
        return "البداية";
    }

    private int badgeTier(int p){
        if(p>=1200)return 4;
        if(p>=700)return 3;
        if(p>=350)return 2;
        if(p>=120)return 1;
        return 0;
    }

    private int badgeNext(int t){
        if(t==0)return 120;
        if(t==1)return 350;
        if(t==2)return 700;
        if(t==3)return 1200;
        return 1200;
    }

    private String badgeName(int t){
        if(t==1)return "برونزي";
        if(t==2)return "فضي";
        if(t==3)return "ذهبي";
        if(t==4)return "متمكن";
        return "لم يُفتح بعد";
    }

    private String topBlocker(){
        try{
            JSONArray a=new JSONArray(prefs.getString("task_state_events","[]"));
            LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();
            Calendar lim=Calendar.getInstance();
            lim.add(Calendar.DAY_OF_MONTH,-30);
            String limKey=dateKey(lim);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);
                if(o==null)continue;
                if("تم".equals(o.optString("status")))continue;
                if(o.optString("date").compareTo(limKey)<0)continue;
                String reason=o.optString("reason");
                if(reason.isEmpty()||"—".equals(reason))continue;
                counts.put(reason,counts.containsKey(reason)?counts.get(reason)+1:1);
            }
            String top="لا توجد بيانات كافية بعد";
            int max=0;
            for(String x:counts.keySet()){
                int v=counts.get(x);
                if(v>max){max=v;top=x;}
            }
            return top;
        }catch(Exception e){
            return "لا توجد بيانات كافية بعد";
        }
    }

    private String blockerAdvice(){
        String x=topBlocker();
        if(x.contains("فكرة"))return "اكتب الفكرة في «لاحقًا» خلال ثوانٍ ثم ارجع للمهمة.";
        if(x.contains("يوتيوب")||x.contains("تصفح"))return "افتح الهاتف بهدف واحد وحدد مؤقتًا قبل التصفح.";
        if(x.contains("نوم")||x.contains("تعب"))return "راجع وقت النوم أولًا؛ لا تعالج الإرهاق بمزيد من الضغط.";
        if(x.contains("وقت"))return "حوّل المهمة إلى نسخة مصغرة ١٠ دقائق بدل إسقاطها بالكامل.";
        return "استمر في تسجيل التأجيل والتعثر حتى يظهر نمط حقيقي.";
    }

    private boolean matches(Task t){
        if("الكل".equals(filter))return true;
        if("قرآن".equals(filter))return t.domain.equals("القرآن");
        if("إنجليزية".equals(filter))return t.domain.equals("الإنجليزية والقبول");
        if("عمل".equals(filter))return t.domain.equals("العمل والدخل");
        if("صحة".equals(filter))return t.domain.equals("الصحة");
        if("قراءة".equals(filter))return t.domain.equals("المعرفة والقراءة");
        if("أسرة".equals(filter))return t.domain.equals("الأسرة");
        return true;
    }

    private String homeMessage(){
        int p=dayRate(Calendar.getInstance());
        if(p>=100)return "أنهيت هدف اليوم. الآن حافظ على النوم والهدوء.";
        if(p>=80)return "يوم قوي حتى الآن — لا تحتاج إضافة شيء جديد.";
        if(p>=40)return "تقدمت جيدًا؛ أكمل أهم مهمة تالية.";
        return "ابدأ بأول مهمة واضحة أمامك.";
    }

    private String shortDomain(String d){
        if(d.equals("الإنجليزية والقبول"))return "الإنجليزية";
        if(d.equals("العمل والدخل"))return "العمل";
        if(d.equals("المعرفة والقراءة"))return "المعرفة";
        if(d.equals("الدين والمسجد"))return "المسجد";
        return d;
    }

    private String domainShort(String d){
        if(d.equals("القرآن"))return "ق";
        if(d.equals("الإنجليزية والقبول"))return "E";
        if(d.equals("العمل والدخل"))return "ع";
        if(d.equals("الصحة"))return "ص";
        if(d.equals("المعرفة والقراءة"))return "م";
        if(d.equals("التواصل"))return "ت";
        if(d.equals("الأسرة"))return "أ";
        if(d.equals("الدين والمسجد"))return "د";
        return "✓";
    }

    private String iconKey(String d){
        if(d.equals("القرآن"))return "quran";
        if(d.equals("الإنجليزية والقبول"))return "headphones";
        if(d.equals("العمل والدخل"))return "briefcase";
        if(d.equals("الصحة"))return "heart";
        if(d.equals("المعرفة والقراءة"))return "book";
        if(d.equals("التواصل"))return "chat";
        if(d.equals("الأسرة"))return "people";
        if(d.equals("الدين والمسجد"))return "moon";
        return "check";
    }

    private int color(String d){
        Integer c=domainColors.get(d);
        return c==null?GREEN:c;
    }

    private int soft(int c){
        return Color.rgb((Color.red(c)+255*6)/7,(Color.green(c)+255*6)/7,(Color.blue(c)+255*6)/7);
    }

    private ScrollView scroll(){
        ScrollView s=new ScrollView(this);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.setBackgroundColor(BG);
        s.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return s;
    }

    private LinearLayout root(ScrollView s){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(20),dp(12),dp(20),dp(30));
        r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        s.addView(r,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        return r;
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        c.setBackground(round(CARD,26,BorderSpec.ONE));
        c.setElevation(dp(1));
        return c;
    }

    private void add(LinearLayout r,View v,int top){
        LinearLayout.LayoutParams lp;
        if(v.getLayoutParams() instanceof LinearLayout.LayoutParams){
            lp=(LinearLayout.LayoutParams)v.getLayoutParams();
            lp.width=ViewGroup.LayoutParams.MATCH_PARENT;
            if(lp.height==0)lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;
        }else{
            lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        lp.setMargins(0,dp(top),0,0);
        r.addView(v,lp);
    }

    private void section(LinearLayout r,String title,String action){
        LinearLayout h=new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        h.setPadding(0,dp(22),0,dp(6));

        TextView t=text(title,23,TEXT,true);
        h.addView(t,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));

        if(!action.isEmpty()){
            TextView a=text(action,11,MUTED,false);
            a.setTag("section_action");
            h.addView(a);
        }
        r.addView(h);
    }

    private TextView findActionText(LinearLayout r){
        if(r.getChildCount()==0)return null;
        View last=r.getChildAt(r.getChildCount()-1);
        if(last instanceof LinearLayout){
            LinearLayout l=(LinearLayout)last;
            for(int i=0;i<l.getChildCount();i++){
                View v=l.getChildAt(i);
                if(v instanceof TextView && "section_action".equals(v.getTag()))return (TextView)v;
            }
        }
        return null;
    }

    private TextView text(String s,int sp,int c,boolean bold){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(c);
        t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private TextView center(String s,int sp,int c,boolean bold){
        TextView t=text(s,sp,c,bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView pill(String s,int c,int bg){
        TextView t=center(s,10,c,true);
        t.setPadding(dp(10),dp(6),dp(10),dp(6));
        t.setBackground(round(bg,16));
        return t;
    }

    private TextView filterChip(String s,boolean active){
        TextView t=center(s,11,active?Color.WHITE:TEXT,true);
        t.setPadding(dp(14),dp(8),dp(14),dp(8));
        t.setBackground(active?round(GREEN,17):round(Color.WHITE,17,BorderSpec.ONE));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(38));
        lp.setMargins(dp(3),0,dp(3),0);
        t.setLayoutParams(lp);
        return t;
    }

    private Button primaryButton(String label){
        Button b=new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(GREEN,19));
        b.setStateListAnimator(null);
        return b;
    }

    private ProgressBar progress(int value,int max,int color,int heightDp){
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        p.setMax(Math.max(1,max));
        p.setProgress(Math.max(0,Math.min(value,max)));
        p.setProgressTintList(ColorStateList.valueOf(color));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(233,237,242)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(heightDp));
        lp.setMargins(0,dp(7),0,0);
        p.setLayoutParams(lp);
        return p;
    }

    private GradientDrawable round(int c,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(c);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable round(int c,int radius,BorderSpec border){
        GradientDrawable d=round(c,radius);
        d.setStroke(dp(1),BORDER);
        return d;
    }

    private enum BorderSpec { ONE }

    private LinearLayout.LayoutParams weight(){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        lp.setMargins(dp(3),0,dp(3),0);
        return lp;
    }

    private void addRing(LinearLayout row,String label,int pct,int c){
        LinearLayout x=new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setGravity(Gravity.CENTER);
        x.addView(new RingView(this,pct,c,false),new LinearLayout.LayoutParams(dp(74),dp(74)));
        TextView l=center(label,10,MUTED,false);
        l.setPadding(0,dp(4),0,0);
        x.addView(l);
        row.addView(x,new LinearLayout.LayoutParams(0,dp(104),1f));
    }

    private LinearLayout statRow(String value,String label,int c){
        LinearLayout x=new LinearLayout(this);
        x.setOrientation(LinearLayout.HORIZONTAL);
        x.setGravity(Gravity.CENTER_VERTICAL);
        x.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        x.setPadding(0,dp(5),0,dp(5));
        TextView v=text(value,19,c,true);
        x.addView(v,new LinearLayout.LayoutParams(dp(88),ViewGroup.LayoutParams.WRAP_CONTENT));
        x.addView(text(label,10,MUTED,false));
        return x;
    }

    private View metricCard(String icon,String value,String label,int c){
        LinearLayout x=card();
        x.setGravity(Gravity.CENTER);
        x.setPadding(dp(7),dp(11),dp(7),dp(10));
        x.addView(new DomainIcon(this,icon,c),new LinearLayout.LayoutParams(dp(29),dp(29)));
        TextView v=center(value,17,TEXT,true);
        v.setPadding(0,dp(4),0,0);
        x.addView(v);
        x.addView(center(label,9,MUTED,false));
        return x;
    }

    private void addKeyValue(LinearLayout parent,String key,String value){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0,dp(7),0,dp(7));
        row.addView(text(key,12,MUTED,false),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        row.addView(text(value,12,TEXT,true));
        parent.addView(row);
    }

    private String dateKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}

    private Calendar saturdayStart(Calendar src){
        Calendar c=(Calendar)src.clone();
        while(c.get(Calendar.DAY_OF_WEEK)!=Calendar.SATURDAY)c.add(Calendar.DAY_OF_MONTH,-1);
        c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
        return c;
    }

    private int pct(int v,int max){
        if(max<=0)return 0;
        return Math.max(0,Math.min(100,Math.round(v*100f/max)));
    }

    private String format12(int minutes){
        int h=(minutes/60)%24, m=minutes%60;
        String ap=h<12?"ص":"م";
        int h12=h%12; if(h12==0)h12=12;
        return ar(h12)+":"+twoAr(m)+" "+ap;
    }

    private String twoAr(int n){
        if(n<10)return "٠"+ar(n);
        return ar(n);
    }

    private String formatMinutes(int min){
        int h=min/60, m=min%60;
        if(h==0)return ar(m)+" د";
        if(m==0)return ar(h)+" س";
        return ar(h)+" س "+ar(m)+" د";
    }

    private String ar(int n){
        return String.valueOf(n)
                .replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤')
                .replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');
    }

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}

    private class RingView extends View {
        int pct,color; boolean big;
        Paint p=new Paint(1);
        RingView(Activity c,int pct,int color,boolean big){super(c);this.pct=pct;this.color=color;this.big=big;}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight(),cx=w/2f,cy=h/2f;
            float stroke=dp(big?9:7);
            float rr=Math.min(w,h)/2f-stroke-dp(2);
            p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(stroke);p.setColor(Color.rgb(232,236,242));
            c.drawCircle(cx,cy,rr,p);
            p.setColor(color);
            RectF oval=new RectF(cx-rr,cy-rr,cx+rr,cy+rr);
            c.drawArc(oval,-90,360*pct/100f,false,p);
            p.setStyle(Paint.Style.FILL);p.setColor(TEXT);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(big?19:14));
            c.drawText(ar(pct)+"٪",cx,cy-(p.ascent()+p.descent())/2,p);
        }
    }

    private class CheckView extends View {
        boolean checked; int color; Paint p=new Paint(1);
        CheckView(Activity c,boolean checked,int color){super(c);this.checked=checked;this.color=color;setClickable(true);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float pad=dp(8);
            RectF rect=new RectF(pad,pad,getWidth()-pad,getHeight()-pad);
            p.setStyle(Paint.Style.FILL);p.setColor(checked?color:Color.WHITE);
            c.drawRoundRect(rect,dp(6),dp(6),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(color);
            c.drawRoundRect(rect,dp(6),dp(6),p);
            if(checked){
                p.setColor(Color.WHITE);p.setStrokeWidth(dp(2.6f));p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);
                Path path=new Path();
                path.moveTo(getWidth()*.32f,getHeight()*.52f);
                path.lineTo(getWidth()*.46f,getHeight()*.66f);
                path.lineTo(getWidth()*.70f,getHeight()*.38f);
                c.drawPath(path,p);
            }
        }
    }

    private class TargetView extends View {
        int color; Paint p=new Paint(1);
        TargetView(Activity a,int c){super(a);color=c;setBackground(round(soft(c),30));}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float cx=getWidth()/2f,cy=getHeight()/2f;
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(color);
            c.drawCircle(cx,cy,dp(13),p);c.drawCircle(cx,cy,dp(7),p);
            p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,dp(2.5f),p);
        }
    }

    private class BadgeView extends View {
        int color; String label; Paint p=new Paint(1);
        BadgeView(Activity a,int c,String label){super(a);this.color=c;this.label=label;}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight(),cx=w/2f,cy=h/2f,rr=Math.min(w,h)*.42f;
            Path hex=new Path();
            for(int i=0;i<6;i++){
                double ang=Math.PI/3*i-Math.PI/2;
                float x=cx+(float)Math.cos(ang)*rr;
                float y=cy+(float)Math.sin(ang)*rr;
                if(i==0)hex.moveTo(x,y); else hex.lineTo(x,y);
            }
            hex.close();
            p.setStyle(Paint.Style.FILL);p.setColor(soft(color));c.drawPath(hex,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(color);c.drawPath(hex,p);
            p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(cx,cy,rr*.58f,p);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(label.length()>1?16:21));
            c.drawText(label,cx,cy-(p.ascent()+p.descent())/2,p);
        }
    }

    private class NavIcon extends View {
        String type; int color; Paint p=new Paint(1);
        NavIcon(Activity a,String t,int c){super(a);type=t;color=c;}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.2f));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(type.equals("home")){
                Path q=new Path();q.moveTo(w*.18f,h*.48f);q.lineTo(w*.50f,h*.20f);q.lineTo(w*.82f,h*.48f);q.lineTo(w*.76f,h*.80f);q.lineTo(w*.24f,h*.80f);q.close();c.drawPath(q,p);
            }else if(type.equals("tasks")){
                c.drawRoundRect(new RectF(w*.20f,h*.20f,w*.80f,h*.80f),dp(2),dp(2),p);
                Path q=new Path();q.moveTo(w*.31f,h*.50f);q.lineTo(w*.43f,h*.62f);q.lineTo(w*.69f,h*.35f);c.drawPath(q,p);
            }else if(type.equals("stats")){
                c.drawLine(w*.25f,h*.72f,w*.25f,h*.46f,p);c.drawLine(w*.50f,h*.72f,w*.50f,h*.28f,p);c.drawLine(w*.75f,h*.72f,w*.75f,h*.38f,p);
            }else if(type.equals("rewards")){
                RectF cup=new RectF(w*.30f,h*.22f,w*.70f,h*.58f);c.drawRoundRect(cup,dp(3),dp(3),p);
                c.drawArc(new RectF(w*.15f,h*.28f,w*.38f,h*.53f),90,180,false,p);
                c.drawArc(new RectF(w*.62f,h*.28f,w*.85f,h*.53f),270,180,false,p);
                c.drawLine(w*.50f,h*.58f,w*.50f,h*.74f,p);c.drawLine(w*.37f,h*.76f,w*.63f,h*.76f,p);
            }else if(type.equals("more")){
                p.setStyle(Paint.Style.FILL);c.drawCircle(w*.27f,h*.50f,dp(2.2f),p);c.drawCircle(w*.50f,h*.50f,dp(2.2f),p);c.drawCircle(w*.73f,h*.50f,dp(2.2f),p);
            }else if(type.equals("menu")){
                c.drawLine(w*.22f,h*.30f,w*.78f,h*.30f,p);c.drawLine(w*.22f,h*.50f,w*.78f,h*.50f,p);c.drawLine(w*.22f,h*.70f,w*.78f,h*.70f,p);
            }else if(type.equals("bell")){
                Path q=new Path();q.moveTo(w*.30f,h*.65f);q.quadTo(w*.35f,h*.56f,w*.35f,h*.40f);q.quadTo(w*.50f,h*.20f,w*.65f,h*.40f);q.quadTo(w*.65f,h*.56f,w*.70f,h*.65f);q.close();c.drawPath(q,p);c.drawLine(w*.42f,h*.74f,w*.58f,h*.74f,p);
            }
        }
    }

    private class DomainIcon extends View {
        String type; int color; Paint p=new Paint(1);
        DomainIcon(Activity a,String type,int color){super(a);this.type=type;this.color=color;}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(type.equals("quran")||type.equals("book")){
                c.drawRoundRect(new RectF(w*.18f,h*.23f,w*.48f,h*.76f),dp(2),dp(2),p);c.drawRoundRect(new RectF(w*.52f,h*.23f,w*.82f,h*.76f),dp(2),dp(2),p);c.drawLine(w*.50f,h*.25f,w*.50f,h*.77f,p);
            }else if(type.equals("headphones")){
                c.drawArc(new RectF(w*.22f,h*.18f,w*.78f,h*.74f),195,150,false,p);c.drawRoundRect(new RectF(w*.18f,h*.48f,w*.31f,h*.73f),dp(2),dp(2),p);c.drawRoundRect(new RectF(w*.69f,h*.48f,w*.82f,h*.73f),dp(2),dp(2),p);
            }else if(type.equals("briefcase")){
                c.drawRoundRect(new RectF(w*.18f,h*.34f,w*.82f,h*.76f),dp(3),dp(3),p);c.drawRoundRect(new RectF(w*.38f,h*.22f,w*.62f,h*.38f),dp(2),dp(2),p);c.drawLine(w*.18f,h*.50f,w*.82f,h*.50f,p);
            }else if(type.equals("heart")){
                Path q=new Path();q.moveTo(w*.50f,h*.78f);q.cubicTo(w*.12f,h*.54f,w*.18f,h*.23f,w*.38f,h*.26f);q.cubicTo(w*.46f,h*.27f,w*.50f,h*.35f,w*.50f,h*.35f);q.cubicTo(w*.50f,h*.35f,w*.55f,h*.27f,w*.63f,h*.26f);q.cubicTo(w*.83f,h*.23f,w*.88f,h*.54f,w*.50f,h*.78f);c.drawPath(q,p);
            }else if(type.equals("chat")){
                c.drawRoundRect(new RectF(w*.18f,h*.20f,w*.82f,h*.66f),dp(5),dp(5),p);Path q=new Path();q.moveTo(w*.33f,h*.66f);q.lineTo(w*.27f,h*.80f);q.lineTo(w*.47f,h*.66f);c.drawPath(q,p);
            }else if(type.equals("people")){
                c.drawCircle(w*.38f,h*.36f,w*.10f,p);c.drawCircle(w*.63f,h*.39f,w*.08f,p);c.drawArc(new RectF(w*.18f,h*.46f,w*.58f,h*.78f),190,160,false,p);c.drawArc(new RectF(w*.48f,h*.50f,w*.82f,h*.76f),190,160,false,p);
            }else if(type.equals("moon")){
                Path q=new Path();q.addCircle(w*.50f,h*.50f,w*.28f,Path.Direction.CW);Path cut=new Path();cut.addCircle(w*.62f,h*.40f,w*.25f,Path.Direction.CW);q.op(cut,Path.Op.DIFFERENCE);p.setStyle(Paint.Style.FILL);c.drawPath(q,p);
            }else if(type.equals("check")){
                Path q=new Path();q.moveTo(w*.25f,h*.50f);q.lineTo(w*.43f,h*.68f);q.lineTo(w*.76f,h*.32f);c.drawPath(q,p);
            }else if(type.equals("streak")){
                Path q=new Path();q.moveTo(w*.50f,h*.14f);q.cubicTo(w*.72f,h*.36f,w*.78f,h*.48f,w*.70f,h*.70f);q.cubicTo(w*.62f,h*.88f,w*.36f,h*.88f,w*.28f,h*.68f);q.cubicTo(w*.20f,h*.50f,w*.30f,h*.36f,w*.42f,h*.27f);q.cubicTo(w*.40f,h*.43f,w*.51f,h*.47f,w*.50f,h*.14f);c.drawPath(q,p);
            }else if(type.equals("medal")){
                c.drawCircle(w*.50f,h*.40f,w*.22f,p);c.drawLine(w*.38f,h*.59f,w*.31f,h*.82f,p);c.drawLine(w*.62f,h*.59f,w*.69f,h*.82f,p);
            }else if(type.equals("points")){
                Path q=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float rr=(i%2==0?w*.30f:w*.13f);float x=w*.5f+(float)Math.cos(a)*rr;float y=h*.5f+(float)Math.sin(a)*rr;if(i==0)q.moveTo(x,y);else q.lineTo(x,y);}q.close();c.drawPath(q,p);
            }else if(type.equals("play")){
                Path q=new Path();q.moveTo(w*.34f,h*.24f);q.lineTo(w*.76f,h*.50f);q.lineTo(w*.34f,h*.76f);q.close();c.drawPath(q,p);
            }else if(type.equals("discover")){
                c.drawCircle(w*.50f,h*.50f,w*.27f,p);c.drawLine(w*.50f,h*.22f,w*.60f,h*.48f,p);c.drawLine(w*.60f,h*.48f,w*.40f,h*.58f,p);
            }else if(type.equals("hobby")){
                c.drawCircle(w*.50f,h*.50f,w*.27f,p);c.drawLine(w*.25f,h*.40f,w*.75f,h*.60f,p);c.drawLine(w*.30f,h*.68f,w*.70f,h*.32f,p);
            }else if(type.equals("calendar")){
                c.drawRoundRect(new RectF(w*.18f,h*.24f,w*.82f,h*.78f),dp(3),dp(3),p);c.drawLine(w*.18f,h*.40f,w*.82f,h*.40f,p);c.drawLine(w*.34f,h*.17f,w*.34f,h*.31f,p);c.drawLine(w*.66f,h*.17f,w*.66f,h*.31f,p);
            }else if(type.equals("tasks")){
                c.drawRoundRect(new RectF(w*.22f,h*.20f,w*.78f,h*.80f),dp(3),dp(3),p);c.drawLine(w*.34f,h*.38f,w*.42f,h*.46f,p);c.drawLine(w*.42f,h*.46f,w*.56f,h*.31f,p);c.drawLine(w*.34f,h*.62f,w*.66f,h*.62f,p);
            }else{
                c.drawCircle(w*.50f,h*.50f,w*.26f,p);
            }
        }
    }

    private class WeekBars extends View {
        Paint p=new Paint(1);
        WeekBars(Activity a){super(a);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight(),base=h-dp(30),top=dp(12);
            Calendar d=saturdayStart(Calendar.getInstance());
            String[] labs={"س","ح","ن","ث","ر","خ","ج"};
            float slot=w/7f;
            for(int i=0;i<7;i++){
                int v=dayRate(d);
                float bh=(base-top)*v/100f;
                float cx=slot*(i+.5f);
                p.setColor(Color.rgb(235,238,243));p.setStyle(Paint.Style.FILL);
                RectF bg=new RectF(cx-dp(10),top,cx+dp(10),base);c.drawRoundRect(bg,dp(8),dp(8),p);
                p.setColor(v>=80?GREEN:BLUE);
                RectF fg=new RectF(cx-dp(10),base-bh,cx+dp(10),base);c.drawRoundRect(fg,dp(8),dp(8),p);
                p.setColor(MUTED);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(10));p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(labs[i],cx,h-dp(8),p);
                d.add(Calendar.DAY_OF_MONTH,1);
            }
        }
    }

    private class TrendView extends View {
        Paint p=new Paint(1);
        TrendView(Activity a){super(a);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight(),left=dp(8),right=w-dp(8),top=dp(12),bottom=h-dp(22);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(Color.rgb(232,236,241));
            for(int i=0;i<4;i++){float y=top+(bottom-top)*i/3f;c.drawLine(left,y,right,y,p);}
            Calendar d=Calendar.getInstance();d.add(Calendar.DAY_OF_MONTH,-13);
            Path path=new Path();
            for(int i=0;i<14;i++){
                float x=left+(right-left)*i/13f;
                float y=bottom-(bottom-top)*dayRate(d)/100f;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
                d.add(Calendar.DAY_OF_MONTH,1);
            }
            p.setColor(GREEN);p.setStrokeWidth(dp(2.4f));p.setStrokeCap(Paint.Cap.ROUND);c.drawPath(path,p);
        }
    }
}
