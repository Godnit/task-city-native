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
import java.util.Map;

public class MasariV10Activity extends Activity {
    private static final int BG = Color.rgb(247,249,252);
    private static final int NAVY = Color.rgb(8,43,68);
    private static final int GREEN = Color.rgb(24,142,78);
    private static final int GREEN2 = Color.rgb(38,169,96);
    private static final int TEXT = Color.rgb(25,34,45);
    private static final int MUTED = Color.rgb(111,121,135);
    private static final int BORDER = Color.rgb(229,233,240);
    private static final int GOLD = Color.rgb(226,157,38);
    private static final int BLUE = Color.rgb(57,117,205);
    private static final int ORANGE = Color.rgb(230,139,45);
    private static final int RED = Color.rgb(210,81,73);
    private static final int PURPLE = Color.rgb(129,87,188);
    private static final int TEAL = Color.rgb(49,156,164);
    private static final int PINK = Color.rgb(202,91,136);

    private SharedPreferences prefs;
    private String todayKey;
    private String tab = "home";
    private String taskFilter = "الكل";
    private String rewardTab = "level";
    private final LinkedHashMap<String,Integer> domainColors = new LinkedHashMap<>();

    static class Task {
        String id,title,domain,icon;
        int start,end,points;
        boolean required;
        Task(String id,int start,int end,String title,String domain,int points,boolean required,String icon){
            this.id=id;this.start=start;this.end=end;this.title=title;this.domain=domain;this.points=points;this.required=required;this.icon=icon;
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
        migrateOldStars();
        syncCredits();
        render();
    }

    @Override protected void onResume(){
        super.onResume();
        if(prefs!=null){todayKey=dateKey(Calendar.getInstance());syncCredits();render();}
    }

    private void setupDomains(){
        domainColors.put("القرآن",GREEN);
        domainColors.put("الإنجليزية والقبول",BLUE);
        domainColors.put("العمل والدخل",ORANGE);
        domainColors.put("الصحة",RED);
        domainColors.put("المعرفة والقراءة",PURPLE);
        domainColors.put("التواصل",TEAL);
        domainColors.put("الأسرة",PINK);
        domainColors.put("الدين والمسجد",Color.rgb(62,126,79));
        domainColors.put("الانضباط",Color.rgb(95,105,118));
    }

    private void migrateOldStars(){
        if(!prefs.getBoolean("v10_credit_migration",false)){
            int credits=prefs.getInt("reward_credits",0)+prefs.getInt("reward_stars",0)*10;
            prefs.edit().putInt("reward_credits",credits).putBoolean("v10_credit_migration",true).apply();
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
        if(tab.equals("tasks")) page=tasksPage();
        else if(tab.equals("stats")) page=statsPage();
        else if(tab.equals("rewards")) page=rewardsPage();
        else if(tab.equals("more")) page=morePage();
        else page=homePage();
        body.addView(page,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        shell.addView(bottomNav(),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(76)));
        setContentView(shell);
    }

    private View homePage(){
        ScrollView s=scroll();LinearLayout r=root(s);topBar(r,"مساري","☰","♧");
        LinearLayout welcome=new LinearLayout(this);welcome.setOrientation(LinearLayout.HORIZONTAL);welcome.setGravity(Gravity.CENTER_VERTICAL);welcome.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.addView(text("مرحبًا 👋",20,TEXT,true));tx.addView(text(homeMessage(),12,MUTED,false));welcome.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));welcome.addView(circle("م",50,soft(GREEN),GREEN,20));add(r,welcome,12);

        section(r,"تقدمك","اليوم حتى الآن");
        LinearLayout progressCard=card();progressCard.setPadding(dp(12),dp(13),dp(12),dp(12));add(r,progressCard,6);
        LinearLayout rings=new LinearLayout(this);rings.setOrientation(LinearLayout.HORIZONTAL);rings.setGravity(Gravity.CENTER);rings.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addRing(rings,"اليوم",dayRate(Calendar.getInstance()),GREEN);addRing(rings,"الأسبوع",weekRate(),BLUE);addRing(rings,"الشهر",monthRate(),PURPLE);addRing(rings,"السنة",yearRate(),GOLD);progressCard.addView(rings);
        TextView msg=center(dayRate(Calendar.getInstance())>=80?"يوم قوي — أكمل بهدوء ولا تبالغ.":"أنجز المهمة التالية فقط وارفع التقدم خطوة خطوة.",11,dayRate(Calendar.getInstance())>=80?GREEN:MUTED,true);msg.setPadding(0,dp(7),0,0);progressCard.addView(msg);

        LinearLayout goal=card();goal.setPadding(dp(13),dp(12),dp(13),dp(12));add(r,goal,10);
        LinearLayout gh=new LinearLayout(this);gh.setOrientation(LinearLayout.HORIZONTAL);gh.setGravity(Gravity.CENTER_VERTICAL);gh.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);gh.addView(circle("◎",44,soft(GREEN),GREEN,22));
        LinearLayout gt=new LinearLayout(this);gt.setOrientation(LinearLayout.VERTICAL);gt.setPadding(dp(10),0,0,0);gt.addView(text("الهدف اليومي",16,TEXT,true));gt.addView(text("٨٠٪ من المهام الأساسية + القرآن + الإنجليزية.",11,MUTED,false));gh.addView(gt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));goal.addView(gh);
        TextView done=text(ar(countDoneToday())+" من "+ar(tasksToday().size())+" مهام",11,GREEN,true);done.setPadding(0,dp(8),0,dp(4));goal.addView(done);goal.addView(progress(dayPoints(Calendar.getInstance()),dayTarget(Calendar.getInstance()),GREEN));

        section(r,"جدول اليوم","عرض الكل");int shown=0;for(Task t:tasksToday()){if(t.start<420||t.id.equals("lunch")||t.id.equals("sleep2"))continue;addCompactTask(r,t);shown++;if(shown>=6)break;}
        LinearLayout streak=card();streak.setPadding(dp(13),dp(11),dp(13),dp(11));add(r,streak,10);streak.addView(text("🔥 سلسلة الأيام القوية",15,TEXT,true));streak.addView(text(ar(streak())+" يومًا متتاليًا بنسبة ٨٠٪ فأكثر",11,GREEN,true));
        LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);quick.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);quick.setPadding(0,dp(10),0,0);quick.addView(quickTile("📖","القرآن",GREEN),weight());quick.addView(quickTile("🎧","الإنجليزية",BLUE),weight());quick.addView(quickTile("▣","العمل",ORANGE),weight());quick.addView(quickTile("📚","LeapAhead",PURPLE),weight());r.addView(quick);
        return s;
    }

    private View tasksPage(){
        ScrollView s=scroll();LinearLayout r=root(s);topBar(r,"المهام","⋮","⌕");weekStrip(r);
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);chips.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);chips.setPadding(0,dp(10),0,dp(8));
        String[] filters={"الكل","قرآن","إنجليزية","عمل","صحة","قراءة","أسرة"};for(String f:filters){TextView c=filterChip(f,taskFilter.equals(f));chips.addView(c);c.setOnClickListener(v->{taskFilter=f;render();});}hs.addView(chips);r.addView(hs);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);head.addView(text("مهام اليوم",19,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));head.addView(pill(ar(countDoneToday())+" / "+ar(tasksToday().size()),GREEN,soft(GREEN)));r.addView(head);
        for(Task t:tasksToday()) if(matchesFilter(t)) addFullTask(r,t);
        Button add=button("＋ إضافة مهمة مخصصة",GREEN);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(52));lp.setMargins(0,dp(15),0,dp(8));r.addView(add,lp);add.setOnClickListener(v->startActivity(new Intent(this,PlannerCenterActivity.class)));
        return s;
    }

    private View statsPage(){
        ScrollView s=scroll();LinearLayout r=root(s);topBar(r,"الإحصائيات","⋮","▦");
        LinearLayout summary=card();summary.setPadding(dp(13),dp(13),dp(13),dp(13));add(r,summary,9);LinearLayout sr=new LinearLayout(this);sr.setOrientation(LinearLayout.HORIZONTAL);sr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sr.setGravity(Gravity.CENTER);sr.addView(bigRing(monthRate(),"التزام الشهر",GREEN),new LinearLayout.LayoutParams(0,dp(138),1f));LinearLayout nums=new LinearLayout(this);nums.setOrientation(LinearLayout.VERTICAL);nums.setPadding(dp(8),0,dp(8),0);nums.addView(stat(ar(monthCompleted()),"مهمة مكتملة",GREEN));nums.addView(stat(ar(monthPlanned()),"مهمة مخططة",NAVY));nums.addView(stat(ar(monthStrongDays()),"يوم قوي",GOLD));sr.addView(nums,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));summary.addView(sr);
        section(r,"أداء آخر ٦ أسابيع","نسبة الالتزام");LinearLayout bc=card();bc.setPadding(dp(8),dp(10),dp(8),dp(8));add(r,bc,6);bc.addView(new WeekBars(this),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(185)));
        section(r,"اتجاه آخر ٣٠ يومًا","");LinearLayout tc=card();tc.setPadding(dp(8),dp(10),dp(8),dp(8));add(r,tc,6);tc.addView(new Trend(this),new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(180)));
        section(r,"تقدم المجالات","بحسب نقاطك الفعلية");LinearLayout domains=card();domains.setPadding(dp(12),dp(10),dp(12),dp(10));add(r,domains,6);for(String d:domainColors.keySet())addDomainProgress(domains,d);
        LinearLayout m1=new LinearLayout(this);m1.setOrientation(LinearLayout.HORIZONTAL);m1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);m1.setPadding(0,dp(10),0,0);m1.addView(metric("★",ar(totalPoints()),"نقاط التقدم",GOLD),weight());m1.addView(metric("◷",formatMinutes(focusMinutes()),"تركيز الشهر",BLUE),weight());m1.addView(metric("🔥",ar(streak()),"السلسلة",ORANGE),weight());r.addView(m1);
        LinearLayout m2=new LinearLayout(this);m2.setOrientation(LinearLayout.HORIZONTAL);m2.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);m2.setPadding(0,dp(7),0,0);m2.addView(metric("✓",ar(monthRate())+"٪","الالتزام",GREEN),weight());m2.addView(metric("📚",ar(monthDone("leap1")+monthDone("leap2")+monthDone("leap3")),"LeapAhead",PURPLE),weight());m2.addView(metric("📖",ar(monthDone("quran1")+monthDone("quran2")),"جلسات القرآن",GREEN),weight());r.addView(m2);
        LinearLayout blocker=card();blocker.setPadding(dp(13),dp(11),dp(13),dp(11));add(r,blocker,10);blocker.addView(text("أكثر عائق مسجل",14,TEXT,true));blocker.addView(text(topBlocker(),16,RED,true));blocker.addView(text(blockerAdvice(),10,MUTED,false));blocker.setOnClickListener(v->startActivity(new Intent(this,WeeklyPlannerActivity.class)));
        return s;
    }

    private View rewardsPage(){
        ScrollView s=scroll();LinearLayout r=root(s);topBar(r,"المكافآت","⋮","✦");int total=totalPoints();int level=levelFor(total);int base=levelBase(level);int next=levelNext(level);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);hero.setPadding(dp(16),dp(15),dp(16),dp(15));hero.setBackground(round(NAVY,24));add(r,hero,9);
        LinearLayout hh=new LinearLayout(this);hh.setOrientation(LinearLayout.HORIZONTAL);hh.setGravity(Gravity.CENTER_VERTICAL);hh.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout htx=new LinearLayout(this);htx.setOrientation(LinearLayout.VERTICAL);htx.addView(text("إجمالي نقاط التقدم",12,Color.rgb(191,210,223),false));htx.addView(text(ar(total),31,Color.WHITE,true));htx.addView(text("المستوى "+ar(level)+" • "+levelName(level),14,Color.WHITE,true));hh.addView(htx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));hh.addView(new HexBadge(this,GOLD,"★"),new LinearLayout.LayoutParams(dp(76),dp(76)));hero.addView(hh);hero.addView(progress(Math.max(0,total-base),Math.max(1,next-base),GREEN2));TextView left=text(level>=6?"أعلى مستوى متاح حاليًا":ar(Math.max(0,next-total))+" نقطة للمستوى التالي",10,Color.rgb(183,213,195),true);left.setPadding(0,dp(5),0,0);hero.addView(left);
        LinearLayout seg=segmented();String[][] items={{"المستوى","level"},{"الأوسمة","badges"},{"الإنجازات","ach"},{"المتجر","store"}};for(String[] x:items){TextView b=segment(x[0],rewardTab.equals(x[1]));seg.addView(b,new LinearLayout.LayoutParams(0,dp(46),1f));b.setOnClickListener(v->{rewardTab=x[1];render();});}add(r,seg,10);
        if(rewardTab.equals("badges"))buildBadges(r);else if(rewardTab.equals("ach"))buildAchievements(r);else if(rewardTab.equals("store"))buildStore(r);else buildLevel(r,total,level,next);
        return s;
    }

    private void buildLevel(LinearLayout r,int total,int level,int next){
        LinearLayout c=card();c.setPadding(dp(14),dp(13),dp(14),dp(13));add(r,c,7);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.addView(new HexBadge(this,GREEN,ar(level)),new LinearLayout.LayoutParams(dp(82),dp(82)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(11),0,0,0);tx.addView(text("المستوى الحالي",12,MUTED,false));tx.addView(text(levelName(level),24,NAVY,true));tx.addView(text(level>=6?"وصلت لأعلى مستوى حالي": "بقي "+ar(next-total)+" نقطة للمستوى التالي",10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));c.addView(row);
        section(r,"قوة المجالات","كل مجال يتقدم منفصلًا");for(String d:new String[]{"القرآن","الإنجليزية والقبول","العمل والدخل","الصحة","المعرفة والقراءة"})addBadgeRow(r,d);
    }

    private void buildBadges(LinearLayout r){
        section(r,"الأوسمة","برونزي ← فضي ← ذهبي ← متمكن");for(String d:new String[]{"القرآن","الإنجليزية والقبول","العمل والدخل","الصحة","المعرفة والقراءة","التواصل","الأسرة","الانضباط"})addBadgeRow(r,d);
    }

    private void addBadgeRow(LinearLayout r,String d){
        int p=domainPoints(d),tier=badgeTier(p);LinearLayout c=card();c.setPadding(dp(12),dp(10),dp(12),dp(10));add(r,c,5);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.addView(new HexBadge(this,color(d),domainIcon(d)),new LinearLayout.LayoutParams(dp(62),dp(62)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,0,0);tx.addView(text(shortDomain(d),14,TEXT,true));tx.addView(text(badgeName(tier)+(tier<4?" • التالي عند "+ar(badgeNext(tier))+" نقطة":" • أعلى وسام"),10,MUTED,false));tx.addView(progress(p-badgeBase(tier),Math.max(1,badgeNext(tier)-badgeBase(tier)),color(d)));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));c.addView(row);
    }

    private void buildAchievements(LinearLayout r){
        section(r,"إنجازات حقيقية","تفتح تلقائيًا من سجلك");addAchievement(r,"أسبوع ثابت","٥ أيام قوية خلال آخر ٧ أيام",strongLast7(),5,15,GREEN);addAchievement(r,"قارئ منتظم","٢٠ كتاب LeapAhead",countDoneLike("leap"),20,15,PURPLE);addAchievement(r,"مراجعة راسخة","٢٠ جلسة قرآن",countDoneLike("quran"),20,20,GREEN);addAchievement(r,"إنجليزية مستمرة","١٢ جلسة إنجليزية",countDoneLike("english"),12,15,BLUE);addAchievement(r,"عمل حقيقي","٨ جلسات عمل",countDoneLike("work"),8,15,ORANGE);addAchievement(r,"جسم أقوى","٨ جلسات تمرين",countDoneLike("workout"),8,15,RED);
    }

    private void buildStore(LinearLayout r){
        LinearLayout wallet=card();wallet.setPadding(dp(13),dp(12),dp(13),dp(12));add(r,wallet,7);LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.HORIZONTAL);w.setGravity(Gravity.CENTER_VERTICAL);w.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout wt=new LinearLayout(this);wt.setOrientation(LinearLayout.VERTICAL);wt.addView(text("رصيد المكافآت",12,MUTED,true));wt.addView(text(ar(credits())+" رصيد",25,GREEN,true));w.addView(wt,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));w.addView(circle("✦",50,soft(GREEN),GREEN,23));wallet.addView(w);wallet.addView(text("رصيد المكافآت منفصل عن نقاط التقدم؛ إنفاقه لا ينقص مستواك.",10,MUTED,false));
        section(r,"مكافآت غير مادية","إضافية فوق راحتك المعتادة");reward(r,"🎮","٣٠ دقيقة لعبة أو أنمي إضافية","فوق وقت الترفيه المعتاد.",15,"fun30",PURPLE);reward(r,"🧭","٤٥ دقيقة استكشاف حر","موضوع علمي أو مهارة تحبها.",20,"explore45",TEAL);reward(r,"⚽","جلسة هواية إضافية","كرة، لعبة قصصية، شطرنج أو مهارة.",25,"hobby",GREEN);reward(r,"🌙","مساء خفيف","تلغي المهام الإضافية فقط في مساء واحد.",35,"light",BLUE);reward(r,"🎬","٩٠ دقيقة ترفيه ممتد","في يوم مناسب دون مزاحمة النوم والمسجد.",45,"fun90",ORANGE);reward(r,"🌿","نصف يوم مرن","تحافظ على الأساسيات والباقي راحة أو خروج.",70,"halfday",GREEN);
    }

    private View morePage(){
        ScrollView s=scroll();LinearLayout r=root(s);topBar(r,"المزيد","⋮","⚙");LinearLayout intro=card();intro.setPadding(dp(14),dp(13),dp(14),dp(13));add(r,intro,9);intro.addView(text("مساري الشخصي",19,NAVY,true));intro.addView(text("نظام سنوي للقرآن والإنجليزية والعمل والصحة والتعلم.",10,MUTED,false));addMenu(r,"▦","الأسبوع والتأجيل والعوائق","التقويم المرن وتحليل أسباب التعثر",GREEN,()->startActivity(new Intent(this,WeeklyPlannerActivity.class)));addMenu(r,"✓","المراجعات والمهام المخصصة","مراجعة أسبوعية وشهرية وإضافة مهام",BLUE,()->startActivity(new Intent(this,PlannerCenterActivity.class)));addMenu(r,"♧","التذكيرات الذكية","إدارة تذكيرات المهام المهمة",GOLD,()->startActivity(new Intent(this,WeeklyPlannerActivity.class)));addMenu(r,"↥","النسخة الاحتياطية","تصدير واستعادة بياناتك",PURPLE,()->startActivity(new Intent(this,PlannerCenterActivity.class)));TextView v=center("مساري v0.10.0 • البيانات محفوظة محليًا",10,MUTED,false);v.setPadding(0,dp(18),0,dp(4));r.addView(v);return s;
    }

    private void syncCredits(){
        Calendar now=Calendar.getInstance();String d=dateKey(now);int c=credits();SharedPreferences.Editor e=prefs.edit();boolean changed=false;int rate=dayRate(now);
        if(rate>=80&&!prefs.getBoolean("v10_day80_"+d,false)){c+=10;e.putBoolean("v10_day80_"+d,true);changed=true;}
        if(rate>=100&&!prefs.getBoolean("v10_day100_"+d,false)){c+=10;e.putBoolean("v10_day100_"+d,true);changed=true;}
        if(changed)e.putInt("reward_credits",c).apply();
        awardOnce("v10_ach_strong",strongLast7()>=5,15);awardOnce("v10_ach_leap",countDoneLike("leap")>=20,15);awardOnce("v10_ach_quran",countDoneLike("quran")>=20,20);awardOnce("v10_ach_eng",countDoneLike("english")>=12,15);awardOnce("v10_ach_work",countDoneLike("work")>=8,15);awardOnce("v10_ach_workout",countDoneLike("workout")>=8,15);
    }

    private void awardOnce(String key,boolean yes,int amount){if(yes&&!prefs.getBoolean(key,false)){prefs.edit().putBoolean(key,true).putInt("reward_credits",credits()+amount).apply();}}
    private int credits(){return prefs.getInt("reward_credits",0);}

    private void reward(LinearLayout r,String icon,String title,String desc,int cost,String id,int c){
        LinearLayout x=card();x.setPadding(dp(12),dp(10),dp(12),dp(10));add(r,x,6);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.addView(circle(icon,48,soft(c),c,20));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,dp(7),0);tx.addView(text(title,14,TEXT,true));tx.addView(text(desc,10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));Button b=button(ar(cost)+" رصيد",c);row.addView(b,new LinearLayout.LayoutParams(dp(95),dp(42)));x.addView(row);b.setOnClickListener(v->redeem(title,cost,id));
    }

    private void redeem(String title,int cost,String id){
        if(credits()<cost){Toast.makeText(this,"رصيد المكافآت غير كافٍ",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("استخدام المكافأة؟").setMessage(title+"\n\nسيتم خصم "+ar(cost)+" من رصيد المكافآت فقط، ولن تنقص نقاط تقدمك.").setPositiveButton("استخدام",(d,w)->{prefs.edit().putInt("reward_credits",credits()-cost).putLong("v10_redeem_"+id+"_"+System.currentTimeMillis(),System.currentTimeMillis()).apply();Toast.makeText(this,"استمتع بالمكافأة 🌟",Toast.LENGTH_LONG).show();render();}).setNegativeButton("إلغاء",null).show();
    }

    private void addAchievement(LinearLayout r,String title,String desc,int value,int target,int reward,int c){
        boolean open=value>=target;LinearLayout x=card();x.setPadding(dp(11),dp(9),dp(11),dp(9));if(!open)x.setAlpha(.62f);add(r,x,5);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.addView(new HexBadge(this,open?c:Color.rgb(150,157,166),open?"✓":"·"),new LinearLayout.LayoutParams(dp(54),dp(54)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,0,0);tx.addView(text(title,14,open?TEXT:MUTED,true));tx.addView(text(desc,10,MUTED,false));tx.addView(progress(Math.min(value,target),target,open?c:MUTED));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));row.addView(pill(open?"+"+ar(reward)+" رصيد":ar(Math.min(value,target))+"/"+ar(target),open?GREEN:MUTED,BG));x.addView(row);
    }

    private List<Task> tasksToday(){return tasksFor(Calendar.getInstance());}
    private List<Task> tasksFor(Calendar d){
        List<Task>a=new ArrayList<>();int day=d.get(Calendar.DAY_OF_WEEK);a.add(task("fajr",240,390,"الفجر • المسجد • الحلقة • الدرس","الدين والمسجد",8,true,"☾"));
        if(day==Calendar.SATURDAY)a.add(task("workoutA",390,420,"تمرين A — كتف وذراعان + جسم كامل","الصحة",18,true,"♥"));else if(day==Calendar.TUESDAY)a.add(task("workoutB",390,420,"تمرين B — أوتار وقبضة وسرعة","الصحة",18,true,"♥"));
        a.add(task("english",420,480,englishTitle(day),"الإنجليزية والقبول",day==Calendar.FRIDAY?10:24,day!=Calendar.FRIDAY,"🎧"));a.add(task("sleep2",480,570,"نوم تكميلي","الصحة",8,true,"☁"));a.add(task("leap1",570,600,"LeapAhead — الكتاب ١","المعرفة والقراءة",8,true,"📚"));a.add(task("work",600,690,workTitle(day),"العمل والدخل",day==Calendar.FRIDAY?10:30,day!=Calendar.FRIDAY,"▣"));a.add(task("quran1",705,780,"القرآن — مراجعة جديدة: صفحتان","القرآن",18,true,"📖"));a.add(task("lunch",780,810,"الغداء","الانضباط",0,false,"☕"));a.add(task("leap2",810,855,"LeapAhead — الكتاب ٢","المعرفة والقراءة",8,true,"📚"));a.add(task("quran2",885,970,"القرآن — صفحتان + مراجعة قديمة واختبار بصري","القرآن",22,true,"📖"));a.add(afternoon(day));a.add(task("maghrib",1080,1200,"المغرب • التحفيظ • العشاء","الدين والمسجد",8,true,"☾"));a.add(task("leap3",1230,1275,"LeapAhead — الكتاب ٣ (إضافي)","المعرفة والقراءة",8,false,"📚"));a.add(task("close",1320,1340,"إغلاق اليوم وتحديد الغد","الانضباط",6,true,"✓"));a.add(task("sleep",1340,1350,"إبعاد الهاتف والاستعداد للنوم","الصحة",6,true,"☁"));addCustom(a,d);return a;
    }

    private Task afternoon(int d){
        if(d==Calendar.SATURDAY)return task("family_talk",990,1020,"الأسرة + ١٠ دقائق تدريب كلام","الأسرة",10,true,"👥");
        if(d==Calendar.SUNDAY)return task("talk1",990,1020,"تدريب التواصل + بدء محادثة","التواصل",10,true,"◉");
        if(d==Calendar.MONDAY)return task("family_friend",990,1020,"خدمة الأسرة + تفقد صديق","الأسرة",10,true,"👥");
        if(d==Calendar.TUESDAY)return task("talk2",990,1020,"تدريب الحزم وتنظيم الكلام","التواصل",10,true,"◉");
        if(d==Calendar.WEDNESDAY)return task("medicine",990,1050,"مراجعة طب قديم ٤٥–٦٠ دقيقة","المعرفة والقراءة",14,true,"⚕");
        if(d==Calendar.THURSDAY)return task("khatera",990,1030,"خاطرة دينية + تدريب إلقاء","التواصل",12,true,"◉");
        return task("explore",990,1050,"استكشاف علمي أو مهارة حياة","المعرفة والقراءة",10,false,"✦");
    }

    private void addCustom(List<Task>a,Calendar d){try{JSONArray arr=new JSONArray(prefs.getString("custom_tasks","[]"));for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null||!o.optBoolean("active",true))continue;int day=o.optInt("day",0);if(day!=0&&day!=d.get(Calendar.DAY_OF_WEEK))continue;a.add(task(o.optString("id","custom_"+i),o.optInt("start",960),o.optInt("end",990),o.optString("title","مهمة مخصصة"),o.optString("domain","الانضباط"),o.optInt("points",10),o.optBoolean("required",false),"✦"));}}catch(Exception ignored){}}
    private Task task(String i,int s,int e,String t,String d,int p,boolean r,String icon){return new Task(i,s,e,t,d,p,r,icon);}
    private String englishTitle(int d){if(d==Calendar.SATURDAY||d==Calendar.MONDAY)return"الإنجليزية: مفردات + Reading Explorer";if(d==Calendar.SUNDAY||d==Calendar.TUESDAY)return"الإنجليزية: مفردات + Tactics for Listening";if(d==Calendar.WEDNESDAY)return"الإنجليزية: مفردات + Oxford Bookworms";if(d==Calendar.THURSDAY)return"اختبار الإنجليزية الأسبوعي";return"استماع إنجليزي ممتع — يوم خفيف";}
    private String workTitle(int d){if(d==Calendar.SATURDAY||d==Calendar.SUNDAY)return"العمل: تطوير الأكاديمية — جزء قابل للاختبار";if(d==Calendar.MONDAY||d==Calendar.TUESDAY)return"العمل: الوصول للسوق — عرض أو تواصل حقيقي";if(d==Calendar.WEDNESDAY)return"العمل: دخل مباشر — خدمة أو طالب محتمل";if(d==Calendar.THURSDAY)return"العمل: مراجعة الأرقام والنتائج";return"مراجعة مالية خفيفة";}

    private boolean done(Task t){return prefs.getBoolean("reward_done_"+todayKey+"_"+t.id,false);}
    private void setDone(Task t,boolean v){boolean old=done(t);if(old==v)return;int delta=v?t.points:-t.points;SharedPreferences.Editor e=prefs.edit().putBoolean("reward_done_"+todayKey+"_"+t.id,v);if(t.points>0){String dk="reward_day_points_"+todayKey,dom="reward_domain_"+t.domain;e.putInt(dk,Math.max(0,prefs.getInt(dk,0)+delta));e.putInt(dom,Math.max(0,prefs.getInt(dom,0)+delta));}e.apply();syncCredits();Toast.makeText(this,v?"+"+t.points+" نقطة تقدم":"تم إلغاء الإنجاز",Toast.LENGTH_SHORT).show();}
    private int dayPoints(Calendar c){return prefs.getInt("reward_day_points_"+dateKey(c),0);}
    private int dayTarget(Calendar c){int n=0;for(Task t:tasksFor(c))if(t.required)n+=t.points;return Math.max(1,n);}
    private int dayRate(Calendar c){return percent(dayPoints(c),dayTarget(c));}
    private int weekRate(){Calendar c=saturdayStart(Calendar.getInstance());int p=0,t=0;for(int i=0;i<7;i++){p+=dayPoints(c);t+=dayTarget(c);c.add(Calendar.DAY_OF_MONTH,1);}return percent(p,t);}
    private int monthRate(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int m=c.get(Calendar.MONTH),p=0,t=0;while(c.get(Calendar.MONTH)==m){p+=dayPoints(c);t+=dayTarget(c);c.add(Calendar.DAY_OF_MONTH,1);}return percent(p,t);}
    private int yearRate(){Calendar start=Calendar.getInstance();start.set(2026,Calendar.SEPTEMBER,1,0,0,0);Calendar end=Calendar.getInstance();end.set(2027,Calendar.MAY,31,23,59,59);Calendar now=Calendar.getInstance();if(now.before(start))return 0;Calendar c=(Calendar)start.clone();int p=0,t=0;while(!c.after(now)&&!c.after(end)){p+=dayPoints(c);t+=dayTarget(c);c.add(Calendar.DAY_OF_MONTH,1);}return percent(p,t);}
    private int totalPoints(){int n=0;for(String d:domainColors.keySet())n+=domainPoints(d);return n;}
    private int domainPoints(String d){return prefs.getInt("reward_domain_"+d,0);}
    private int countDoneToday(){int n=0;for(Task t:tasksToday())if(done(t))n++;return n;}
    private int monthDone(String id){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int m=c.get(Calendar.MONTH),n=0;while(c.get(Calendar.MONTH)==m){if(prefs.getBoolean("reward_done_"+dateKey(c)+"_"+id,false))n++;c.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int monthCompleted(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int m=c.get(Calendar.MONTH),n=0;while(c.get(Calendar.MONTH)==m){String k=dateKey(c);for(Task t:tasksFor(c))if(prefs.getBoolean("reward_done_"+k+"_"+t.id,false))n++;c.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int monthPlanned(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int m=c.get(Calendar.MONTH),n=0;while(c.get(Calendar.MONTH)==m){n+=tasksFor(c).size();c.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int monthStrongDays(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int m=c.get(Calendar.MONTH),n=0;while(c.get(Calendar.MONTH)==m){if(dayRate(c)>=80)n++;c.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int focusMinutes(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);int m=c.get(Calendar.MONTH),sum=0;while(c.get(Calendar.MONTH)==m){String k=dateKey(c);for(Task t:tasksFor(c)){boolean focus=t.domain.equals("الإنجليزية والقبول")||t.domain.equals("العمل والدخل")||t.domain.equals("القرآن")||t.domain.equals("المعرفة والقراءة");if(focus&&prefs.getBoolean("reward_done_"+k+"_"+t.id,false))sum+=Math.max(0,t.end-t.start);}c.add(Calendar.DAY_OF_MONTH,1);}return sum;}
    private String formatMinutes(int min){return ar(min/60)+":"+(min%60<10?"٠":"")+ar(min%60)+" س";}
    private int countDoneLike(String word){int n=0;for(Map.Entry<String,?>e:prefs.getAll().entrySet())if(e.getKey().startsWith("reward_done_")&&e.getKey().contains("_"+word)&&Boolean.TRUE.equals(e.getValue()))n++;return n;}
    private int strongLast7(){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_MONTH,-6);int n=0;for(int i=0;i<7;i++){if(dayRate(c)>=80)n++;c.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private int streak(){Calendar c=Calendar.getInstance();if(dayRate(c)<80)c.add(Calendar.DAY_OF_MONTH,-1);int n=0;for(int i=0;i<365;i++){if(dayRate(c)>=80){n++;c.add(Calendar.DAY_OF_MONTH,-1);}else break;}return n;}

    private int levelFor(int p){if(p>=8000)return 6;if(p>=5000)return 5;if(p>=3000)return 4;if(p>=1500)return 3;if(p>=600)return 2;return 1;}
    private int levelBase(int l){if(l==2)return 600;if(l==3)return 1500;if(l==4)return 3000;if(l==5)return 5000;if(l==6)return 8000;return 0;}
    private int levelNext(int l){if(l==1)return 600;if(l==2)return 1500;if(l==3)return 3000;if(l==4)return 5000;if(l==5)return 8000;return 8000;}
    private String levelName(int l){if(l==2)return"منطلق";if(l==3)return"ثابت";if(l==4)return"متقدم";if(l==5)return"متمكن";if(l==6)return"راسخ";return"البداية";}
    private int badgeTier(int p){if(p>=1200)return 4;if(p>=700)return 3;if(p>=350)return 2;if(p>=120)return 1;return 0;}
    private int badgeBase(int t){if(t==1)return 120;if(t==2)return 350;if(t==3)return 700;if(t==4)return 1200;return 0;}
    private int badgeNext(int t){if(t==0)return 120;if(t==1)return 350;if(t==2)return 700;if(t==3)return 1200;return 1200;}
    private String badgeName(int t){if(t==1)return"برونزي";if(t==2)return"فضي";if(t==3)return"ذهبي";if(t==4)return"متمكن";return"لم يُفتح بعد";}

    private String topBlocker(){try{JSONArray a=new JSONArray(prefs.getString("task_state_events","[]"));LinkedHashMap<String,Integer>counts=new LinkedHashMap<>();Calendar limit=Calendar.getInstance();limit.add(Calendar.DAY_OF_MONTH,-30);String min=dateKey(limit);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||"تم".equals(o.optString("status"))||o.optString("date").compareTo(min)<0)continue;String reason=o.optString("reason");if(reason.isEmpty()||reason.equals("—"))continue;counts.put(reason,counts.containsKey(reason)?counts.get(reason)+1:1);}String top="لا توجد بيانات كافية بعد";int max=0;for(String x:counts.keySet())if(counts.get(x)>max){max=counts.get(x);top=x;}return top;}catch(Exception e){return"لا توجد بيانات كافية بعد";}}
    private String blockerAdvice(){String x=topBlocker();if(x.contains("فكرة"))return"اكتب الفكرة في «لاحقًا» ثم ارجع للمهمة.";if(x.contains("يوتيوب")||x.contains("تصفح"))return"حدد هدفًا واحدًا قبل فتح الهاتف واستخدم مؤقتًا.";if(x.contains("نوم")||x.contains("تعب"))return"عالج وقت النوم أولًا بدل زيادة الضغط.";if(x.contains("وقت"))return"استخدم نسخة مصغرة ١٠ دقائق من المهمة.";return"سجّل أسباب التأجيل والتعثر عدة أيام ليظهر النمط الحقيقي.";}
    private String homeMessage(){int p=dayRate(Calendar.getInstance());if(p>=100)return"اكتمل هدف اليوم. حافظ على النوم والهدوء.";if(p>=80)return"يوم قوي حتى الآن — لا تبالغ في إضافة مهام.";if(p>=40)return"تقدمت جيدًا؛ أكمل أهم مهمة تالية.";return"ابدأ بأول مهمة واضحة أمامك.";}

    private boolean matchesFilter(Task t){if(taskFilter.equals("الكل"))return true;if(taskFilter.equals("قرآن"))return t.domain.equals("القرآن");if(taskFilter.equals("إنجليزية"))return t.domain.equals("الإنجليزية والقبول");if(taskFilter.equals("عمل"))return t.domain.equals("العمل والدخل");if(taskFilter.equals("صحة"))return t.domain.equals("الصحة");if(taskFilter.equals("قراءة"))return t.domain.equals("المعرفة والقراءة");if(taskFilter.equals("أسرة"))return t.domain.equals("الأسرة");return true;}
    private String shortDomain(String d){if(d.equals("الإنجليزية والقبول"))return"الإنجليزية";if(d.equals("العمل والدخل"))return"العمل";if(d.equals("المعرفة والقراءة"))return"المعرفة";if(d.equals("الدين والمسجد"))return"المسجد";return d;}
    private String domainIcon(String d){if(d.equals("القرآن"))return"ق";if(d.equals("الإنجليزية والقبول"))return"E";if(d.equals("العمل والدخل"))return"▣";if(d.equals("الصحة"))return"♥";if(d.equals("المعرفة والقراءة"))return"✦";if(d.equals("التواصل"))return"◉";if(d.equals("الأسرة"))return"⌂";return"✓";}
    private int color(String d){Integer c=domainColors.get(d);return c==null?GREEN:c;}
    private int soft(int c){return Color.rgb((Color.red(c)+1275)/6,(Color.green(c)+1275)/6,(Color.blue(c)+1275)/6);}

    private LinearLayout bottomNav(){LinearLayout n=new LinearLayout(this);n.setOrientation(LinearLayout.HORIZONTAL);n.setGravity(Gravity.CENTER);n.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);n.setPadding(dp(5),dp(6),dp(5),dp(6));n.setBackgroundColor(NAVY);addNav(n,"⌂","الرئيسية","home");addNav(n,"☑","المهام","tasks");addNav(n,"▥","الإحصائيات","stats");addNav(n,"♛","المكافآت","rewards");addNav(n,"•••","المزيد","more");return n;}
    private void addNav(LinearLayout n,String icon,String label,String key){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);if(tab.equals(key))x.setBackground(round(GREEN,16));x.addView(center(icon,20,Color.WHITE,true));x.addView(center(label,10,Color.WHITE,tab.equals(key)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f);lp.setMargins(dp(2),0,dp(2),0);n.addView(x,lp);x.setOnClickListener(v->{tab=key;render();});}
    private void topBar(LinearLayout r,String title,String left,String right){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.addView(center(right,20,NAVY,true),new LinearLayout.LayoutParams(dp(44),dp(44)));row.addView(center(title,24,title.equals("مساري")?GREEN:TEXT,true),new LinearLayout.LayoutParams(0,dp(44),1f));row.addView(center(left,20,NAVY,true),new LinearLayout.LayoutParams(dp(44),dp(44)));r.addView(row);}
    private void section(LinearLayout r,String title,String sub){LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.HORIZONTAL);h.setGravity(Gravity.CENTER_VERTICAL);h.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);h.setPadding(0,dp(16),0,dp(5));h.addView(text(title,17,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));if(!sub.isEmpty())h.addView(text(sub,10,MUTED,false));r.addView(h);}
    private void addRing(LinearLayout p,String label,int pct,int c){LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.VERTICAL);w.setGravity(Gravity.CENTER);w.addView(new Ring(this,pct,c),new LinearLayout.LayoutParams(dp(66),dp(66)));w.addView(center(label,10,MUTED,false));p.addView(w,new LinearLayout.LayoutParams(0,dp(90),1f));}
    private void addCompactTask(LinearLayout r,Task t){LinearLayout c=card();c.setPadding(dp(10),dp(8),dp(10),dp(8));add(r,c,5);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);CheckBox cb=new CheckBox(this);cb.setChecked(done(t));cb.setButtonTintList(ColorStateList.valueOf(color(t.domain)));row.addView(cb,new LinearLayout.LayoutParams(dp(42),dp(45)));row.addView(circle(t.icon,38,soft(color(t.domain)),color(t.domain),16));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,dp(9),0);tx.addView(text(t.title,13,TEXT,true));tx.addView(text(time(t.start)+" • "+shortDomain(t.domain),10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));c.addView(row);cb.setOnCheckedChangeListener((v,ch)->{setDone(t,ch);render();});}
    private void addFullTask(LinearLayout r,Task t){LinearLayout c=card();c.setPadding(dp(11),dp(9),dp(11),dp(9));add(r,c,6);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);CheckBox cb=new CheckBox(this);cb.setChecked(done(t));cb.setButtonTintList(ColorStateList.valueOf(color(t.domain)));row.addView(cb,new LinearLayout.LayoutParams(dp(44),dp(52)));row.addView(circle(t.icon,42,soft(color(t.domain)),color(t.domain),17));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,dp(9),0);tx.addView(text(t.title,14,TEXT,true));tx.addView(text(time(t.start)+" — "+time(t.end)+" • "+shortDomain(t.domain),10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));if(t.points>0)row.addView(pill("+"+ar(t.points),color(t.domain),soft(color(t.domain))));c.addView(row);cb.setOnCheckedChangeListener((v,ch)->{setDone(t,ch);render();});}
    private void weekStrip(LinearLayout r){Calendar start=saturdayStart(Calendar.getInstance());LinearLayout strip=new LinearLayout(this);strip.setOrientation(LinearLayout.HORIZONTAL);strip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);for(int i=0;i<7;i++){Calendar d=(Calendar)start.clone();d.add(Calendar.DAY_OF_MONTH,i);boolean now=dateKey(d).equals(todayKey);LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setPadding(dp(4),dp(5),dp(4),dp(5));if(now)x.setBackground(round(GREEN,14));x.addView(center(new SimpleDateFormat("EEE",new Locale("ar")).format(d.getTime()).replace("،",""),9,now?Color.WHITE:MUTED,false));x.addView(center(ar(d.get(Calendar.DAY_OF_MONTH)),13,now?Color.WHITE:TEXT,true));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(57),1f);lp.setMargins(dp(2),0,dp(2),0);strip.addView(x,lp);}add(r,strip,10);}
    private TextView filterChip(String s,boolean active){int c=filterColor(s);TextView t=center(s,11,active?Color.WHITE:c,true);t.setPadding(dp(13),dp(7),dp(13),dp(7));t.setBackground(round(active?c:soft(c),15));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(35));lp.setMargins(dp(3),0,dp(3),0);t.setLayoutParams(lp);return t;}
    private int filterColor(String s){if(s.equals("قرآن"))return GREEN;if(s.equals("إنجليزية"))return BLUE;if(s.equals("عمل"))return ORANGE;if(s.equals("صحة"))return RED;if(s.equals("قراءة"))return PURPLE;if(s.equals("أسرة"))return PINK;return GREEN;}
    private LinearLayout segmented(){LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.HORIZONTAL);s.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);s.setPadding(dp(3),dp(3),dp(3),dp(3));s.setBackground(round(Color.WHITE,18,BORDER));return s;}
    private TextView segment(String s,boolean active){TextView t=center(s,11,active?Color.WHITE:NAVY,true);if(active)t.setBackground(round(GREEN,15));return t;}
    private View quickTile(String icon,String label,int c){LinearLayout x=card();x.setGravity(Gravity.CENTER);x.setPadding(dp(4),dp(9),dp(4),dp(9));x.addView(circle(icon,38,soft(c),c,16));x.addView(center(label,10,TEXT,true));return x;}
    private View bigRing(int pct,String label,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.addView(new Ring(this,pct,c),new LinearLayout.LayoutParams(dp(102),dp(102)));x.addView(center(label,11,MUTED,true));return x;}
    private View stat(String value,String label,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(8),dp(5),dp(8),dp(5));x.addView(text(value,18,c,true));x.addView(text(label,10,MUTED,false));return x;}
    private View metric(String icon,String value,String label,int c){LinearLayout x=card();x.setGravity(Gravity.CENTER);x.setPadding(dp(6),dp(10),dp(6),dp(10));x.addView(center(icon,18,c,true));x.addView(center(value,15,TEXT,true));x.addView(center(label,9,MUTED,false));return x;}
    private void addDomainProgress(LinearLayout p,String d){int pts=domainPoints(d),tier=badgeTier(pts);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,dp(5),0,dp(5));LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.HORIZONTAL);h.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);h.addView(text(shortDomain(d),12,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));h.addView(text(ar(pts)+" • "+badgeName(tier),10,color(d),true));row.addView(h);row.addView(progress(pts-badgeBase(tier),Math.max(1,badgeNext(tier)-badgeBase(tier)),color(d)));p.addView(row);}
    private void addMenu(LinearLayout r,String icon,String title,String sub,int c,Runnable run){LinearLayout x=card();x.setPadding(dp(12),dp(11),dp(12),dp(11));add(r,x,7);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);row.addView(circle(icon,44,soft(c),c,18));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,dp(10),0);tx.addView(text(title,14,TEXT,true));tx.addView(text(sub,10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));row.addView(text("‹",23,MUTED,false));x.addView(row);x.setOnClickListener(v->run.run());}

    private ScrollView scroll(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);s.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return s;}
    private LinearLayout root(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(16),dp(15),dp(16),dp(28));r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);s.addView(r);return r;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);c.setBackground(round(Color.WHITE,19,BORDER));c.setElevation(dp(1));return c;}
    private void add(LinearLayout r,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(top),0,0);r.addView(v,lp);}
    private TextView text(String s,int size,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(c);t.setGravity(Gravity.RIGHT);t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView center(String s,int size,int c,boolean bold){TextView t=text(s,size,c,bold);t.setGravity(Gravity.CENTER);return t;}
    private TextView pill(String s,int c,int bg){TextView t=center(s,10,c,true);t.setPadding(dp(8),dp(4),dp(8),dp(4));t.setBackground(round(bg,18));return t;}
    private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(11);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(c,14));return b;}
    private TextView circle(String s,int size,int bg,int fg,int textSize){TextView t=center(s,textSize,fg,true);t.setBackground(round(bg,999));t.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return t;}
    private ProgressBar progress(int val,int max,int c){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,max));p.setProgress(Math.max(0,Math.min(val,Math.max(1,max))));p.setProgressTintList(ColorStateList.valueOf(c));p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(231,235,241)));p.setMinimumHeight(dp(6));return p;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);lp.setMargins(dp(3),0,dp(3),0);return lp;}
    private GradientDrawable round(int c,int radius){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable round(int c,int radius,int stroke){GradientDrawable d=round(c,radius);d.setStroke(dp(1),stroke);return d;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private int percent(int v,int t){return t<=0?0:Math.max(0,Math.min(100,Math.round(v*100f/t)));}
    private String ar(int n){return String.valueOf(n).replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤').replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');}
    private String dateKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    private Calendar saturdayStart(Calendar c){Calendar x=(Calendar)c.clone();int diff=(x.get(Calendar.DAY_OF_WEEK)-Calendar.SATURDAY+7)%7;x.add(Calendar.DAY_OF_MONTH,-diff);return x;}
    private String time(int m){int h=(m/60)%24,mi=m%60;String ap=h<12?"ص":"م";int hh=h%12;if(hh==0)hh=12;return ar(hh)+":"+(mi<10?"٠":"")+ar(mi)+" "+ap;}

    class Ring extends View{
        Paint p=new Paint(1);int value,color;
        Ring(Activity a,int value,int color){super(a);this.value=value;this.color=color;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float sw=dp(7),pad=sw+dp(2);RectF box=new RectF(pad,pad,getWidth()-pad,getHeight()-pad);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(232,236,242));c.drawArc(box,-90,360,false,p);p.setColor(color);c.drawArc(box,-90,360*value/100f,false,p);p.setStyle(Paint.Style.FILL);p.setColor(TEXT);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(16));c.drawText(ar(value)+"٪",getWidth()/2f,getHeight()/2f+dp(6),p);}
    }

    class HexBadge extends View{
        Paint p=new Paint(1);int color;String label;
        HexBadge(Activity a,int color,String label){super(a);this.color=color;this.label=label;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.42f;Path path=new Path();for(int i=0;i<6;i++){double angle=Math.toRadians(-90+i*60);float x=cx+(float)Math.cos(angle)*r,y=cy+(float)Math.sin(angle)*r;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();p.setStyle(Paint.Style.FILL);p.setColor(soft(color));c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(color);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(cx,cy,r*.61f,p);p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.min(getWidth(),getHeight())*.24f);c.drawText(label,cx,cy+p.getTextSize()*.34f,p);}
    }

    class WeekBars extends View{
        Paint p=new Paint(1);WeekBars(Activity a){super(a);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float left=dp(12),bottom=getHeight()-dp(27),top=dp(16),gap=dp(8),bw=(getWidth()-left*2-gap*5)/6f;Calendar cur=saturdayStart(Calendar.getInstance());cur.add(Calendar.DAY_OF_MONTH,-35);for(int i=0;i<6;i++){int po=0,ta=0;Calendar w=(Calendar)cur.clone();for(int j=0;j<7;j++){po+=dayPoints(w);ta+=dayTarget(w);w.add(Calendar.DAY_OF_MONTH,1);}int v=percent(po,ta);float h=(bottom-top)*v/100f,l=left+i*(bw+gap);p.setColor(i==5?GREEN:Color.rgb(219,226,234));c.drawRoundRect(new RectF(l,bottom-h,l+bw,bottom),dp(5),dp(5),p);p.setColor(MUTED);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(9));c.drawText(ar(v)+"٪",l+bw/2,bottom-h-dp(5),p);c.drawText("أ"+ar(i+1),l+bw/2,getHeight()-dp(7),p);cur.add(Calendar.DAY_OF_MONTH,7);}}
    }

    class Trend extends View{
        Paint p=new Paint(1);Path path=new Path();Trend(Activity a){super(a);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float l=dp(12),r=getWidth()-dp(12),t=dp(18),b=getHeight()-dp(22);p.setStrokeWidth(dp(1));p.setColor(Color.rgb(232,236,241));for(int i=0;i<4;i++){float y=t+(b-t)*i/3f;c.drawLine(l,y,r,y,p);}Calendar d=Calendar.getInstance();d.add(Calendar.DAY_OF_MONTH,-29);path.reset();for(int i=0;i<30;i++){float x=l+(r-l)*i/29f,y=b-(b-t)*dayRate(d)/100f;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);d.add(Calendar.DAY_OF_MONTH,1);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(GREEN);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
    }
}
