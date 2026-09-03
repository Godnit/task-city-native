package com.masari.personalplan;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
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

public class WeeklyPlannerActivity extends Activity {
    private static final int BG=Color.rgb(246,248,252), NAVY=Color.rgb(24,49,83), GREEN=Color.rgb(22,123,98), GOLD=Color.rgb(184,126,28), TEXT=Color.rgb(31,40,54), MUTED=Color.rgb(103,113,128), RED=Color.rgb(165,67,58);
    private SharedPreferences prefs;
    private String tab="week";
    private final String[] days={"السبت","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة"};

    static class T { String id,title,domain; int hour,min,points; T(String i,String t,String d,int h,int m,int p){id=i;title=t;domain=d;hour=h;min=m;points=p;} }

    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("masari_data", Context.MODE_PRIVATE); render(); }

    private void render(){
        LinearLayout shell=new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(BG); shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ScrollView sv=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(18),dp(16),dp(28)); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); sv.addView(root);
        shell.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        header(root); tabs(root);
        if(tab.equals("today")) buildToday(root); else if(tab.equals("blockers")) buildBlockers(root); else if(tab.equals("notify")) buildNotify(root); else buildWeek(root);
        setContentView(shell);
    }

    private void header(LinearLayout root){
        TextView t=text("إدارة أسبوعي",27,NAVY,true); root.addView(t); TextView s=text("تقويم مرن + تأجيل وتعثر + تحليل العوائق",13,MUTED,false); s.setPadding(0,dp(3),0,dp(8)); root.addView(s);
    }

    private void tabs(LinearLayout root){
        LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); String[][] a={{"الأسبوع","week"},{"اليوم","today"},{"العوائق","blockers"},{"التذكير","notify"}};
        for(String[] x:a){ Button b=button(x[0],tab.equals(x[1])?GREEN:NAVY); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(43),1); lp.setMargins(dp(2),0,dp(2),0); r.addView(b,lp); b.setOnClickListener(v->{tab=x[1];render();}); } root.addView(r);
    }

    private void buildWeek(LinearLayout root){
        Button add=button("＋ إضافة مهمة للأسبوع",GREEN); LinearLayout.LayoutParams al=new LinearLayout.LayoutParams(-1,dp(48)); al.setMargins(0,dp(14),0,dp(8)); root.addView(add,al); add.setOnClickListener(v->addWeeklyTaskDialog());
        addNote(root,"الجدول الأساسي مستمر كما هو في مساري. هنا تضيف التغييرات والمواعيد الجديدة بدون إعادة بناء الخطة كلها.");
        Calendar sat=saturdayStart(Calendar.getInstance()); JSONArray extras=getExtras();
        for(int i=0;i<7;i++){
            Calendar d=(Calendar)sat.clone(); d.add(Calendar.DAY_OF_MONTH,i); LinearLayout c=card(); addCard(root,c,8); c.addView(text(days[i]+"  •  "+new SimpleDateFormat("d/M",Locale.US).format(d.getTime()),18,NAVY,true));
            TextView core=text(coreSummary(i),13,TEXT,false); core.setPadding(0,dp(5),0,dp(5)); c.addView(core);
            boolean found=false;
            for(int j=0;j<extras.length();j++){
                JSONObject o=extras.optJSONObject(j); if(o==null||o.optInt("day")!=i) continue; found=true; LinearLayout rr=new LinearLayout(this); rr.setOrientation(LinearLayout.HORIZONTAL); rr.setGravity(Gravity.CENTER_VERTICAL); rr.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                String line=format12(o.optInt("hour"),o.optInt("minute"))+"  —  "+o.optString("title"); rr.addView(text(line,14,GREEN,true),new LinearLayout.LayoutParams(0,-2,1)); Button del=button("حذف",RED); rr.addView(del,new LinearLayout.LayoutParams(dp(72),dp(38))); final int idx=j; del.setOnClickListener(v->deleteExtra(idx)); c.addView(rr);
                if(!o.optString("note").isEmpty()) c.addView(text(o.optString("note"),12,MUTED,false));
            }
            if(!found) c.addView(text("لا توجد إضافات لهذا اليوم.",12,MUTED,false));
        }
    }

    private String coreSummary(int i){
        String s="٤:٠٠ ص مسجد وفجر • ٧:٠٠ ص إنجليزي • ١٠:٠٠ ص عمل • ١١:٤٥ ص قرآن • ١:٣٠ م LeapAhead • ٢:٤٥ م قرآن • ٦:٠٠ م مسجد";
        if(i==0) s="٦:٣٠ ص تمرين A • "+s; if(i==3) s="٦:٣٠ ص تمرين B • "+s; return s;
    }

    private void addWeeklyTaskDialog(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),0,dp(18),0); box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText title=input("اسم المهمة"); EditText note=input("تفاصيل قصيرة"); Spinner day=spinner(days); EditText hour=input("الساعة من ١ إلى ١٢"); hour.setInputType(InputType.TYPE_CLASS_NUMBER); EditText minute=input("الدقيقة ٠-٥٩"); minute.setInputType(InputType.TYPE_CLASS_NUMBER); Spinner ap=spinner(new String[]{"ص","م"});
        box.addView(title); box.addView(note); box.addView(label("اليوم",day)); LinearLayout time=new LinearLayout(this); time.setOrientation(LinearLayout.HORIZONTAL); time.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); time.addView(ap,new LinearLayout.LayoutParams(0,dp(48),1)); time.addView(minute,new LinearLayout.LayoutParams(0,dp(48),1)); time.addView(hour,new LinearLayout.LayoutParams(0,dp(48),1)); box.addView(label("الوقت",time));
        new AlertDialog.Builder(this).setTitle("مهمة أسبوعية").setView(box).setPositiveButton("حفظ",(d,w)->{
            try{ int h=Integer.parseInt(hour.getText().toString()); int m=Integer.parseInt(minute.getText().toString()); if(h<1||h>12||m<0||m>59) throw new Exception(); if(ap.getSelectedItemPosition()==1&&h<12)h+=12; if(ap.getSelectedItemPosition()==0&&h==12)h=0; JSONArray a=getExtras(); JSONObject o=new JSONObject(); o.put("title",title.getText().toString().trim()); o.put("note",note.getText().toString().trim()); o.put("day",day.getSelectedItemPosition()); o.put("hour",h); o.put("minute",m); a.put(o); prefs.edit().putString("week_extra_tasks",a.toString()).apply(); render(); }catch(Exception e){Toast.makeText(this,"أدخل وقتًا صحيحًا",Toast.LENGTH_SHORT).show();}
        }).setNegativeButton("إلغاء",null).show();
    }

    private void buildToday(LinearLayout root){
        addNote(root,"إن أنجزت المهمة من هنا تُضاف نقاطها مثل شاشة اليوم. إذا أجلتها أو تعثرت، اختر السبب حتى يتعلم التطبيق ما الذي يقطعك فعلًا.");
        for(T t:todayTasks()){
            LinearLayout c=card(); addCard(root,c,8); c.addView(text(format12(t.hour,t.min)+"  "+t.title,16,NAVY,true)); c.addView(text(t.domain+" • +"+arabic(t.points)+" نقطة",12,MUTED,false));
            String status=latestStatusToday(t.id); if(!status.isEmpty()){ TextView st=text("الحالة: "+status,12,status.startsWith("تم")?GREEN:(status.startsWith("أجل")?GOLD:RED),true); st.setPadding(0,dp(5),0,dp(5)); c.addView(st); }
            LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL); actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); Button done=button("تم",GREEN), delay=button("أجّل",GOLD), fail=button("تعثر",RED); actions.addView(done,weight()); actions.addView(delay,weight()); actions.addView(fail,weight()); c.addView(actions);
            done.setOnClickListener(v->completeTask(t)); delay.setOnClickListener(v->reasonDialog(t,"أجلت")); fail.setOnClickListener(v->reasonDialog(t,"تعثر"));
        }
    }

    private List<T> todayTasks(){
        Calendar c=Calendar.getInstance(); int d=c.get(Calendar.DAY_OF_WEEK); List<T> a=new ArrayList<>(); a.add(new T("fajr","الفجر والحلقة والدرس","الدين والمسجد",4,0,5)); if(d==Calendar.SATURDAY)a.add(new T("workoutA","تمرين A","الصحة",6,30,20)); if(d==Calendar.TUESDAY)a.add(new T("workoutB","تمرين B","الصحة",6,30,20)); a.add(new T("english","الإنجليزية","الإنجليزية والقبول",7,0,d==Calendar.FRIDAY?10:25)); a.add(new T("leap1","LeapAhead — الكتاب ١","المعرفة والقراءة",9,30,7)); a.add(new T("work","العمل","العمل والدخل",10,0,d==Calendar.FRIDAY?10:25)); a.add(new T("quran1","القرآن — صفحتان","القرآن",11,45,14)); a.add(new T("leap2","LeapAhead — الكتاب ٢","المعرفة والقراءة",13,30,8)); a.add(new T("quran2","القرآن + مراجعة قديمة","القرآن",14,45,18)); a.add(new T("maghrib","المغرب والتحفيظ والعشاء","الدين والمسجد",18,0,5)); a.add(new T("leap3","LeapAhead — الكتاب ٣","المعرفة والقراءة",20,30,7)); a.add(new T("close","إغلاق اليوم","الانضباط",22,0,5)); return a;
    }

    private void completeTask(T t){
        String date=dateKey(Calendar.getInstance()), doneKey="reward_done_"+date+"_"+t.id; if(!prefs.getBoolean(doneKey,false)){
            String dayKey="reward_day_points_"+date, domKey="reward_domain_"+t.domain; prefs.edit().putBoolean(doneKey,true).putInt(dayKey,prefs.getInt(dayKey,0)+t.points).putInt(domKey,prefs.getInt(domKey,0)+t.points).apply();
        } addEvent(t,"تم","—"); Toast.makeText(this,"تم +"+t.points+" نقطة",Toast.LENGTH_SHORT).show(); render();
    }

    private void reasonDialog(T t,String status){
        String[] reasons={"فكرة جديدة شتتتني","يوتيوب/تصفح","تعب أو نقص نوم","طارئ","طلب من شخص","عمل في المسجد","لم أفهم المهمة","لم أرغب بالبدء","ضيق الوقت","سبب آخر"};
        new AlertDialog.Builder(this).setTitle(status+" — ما السبب؟").setItems(reasons,(d,w)->{addEvent(t,status,reasons[w]); render();}).setNegativeButton("إلغاء",null).show();
    }

    private void addEvent(T t,String status,String reason){
        try{JSONArray a=events(); JSONObject o=new JSONObject(); o.put("date",dateKey(Calendar.getInstance())); o.put("id",t.id); o.put("title",t.title); o.put("domain",t.domain); o.put("status",status); o.put("reason",reason); o.put("ts",System.currentTimeMillis()); a.put(o); prefs.edit().putString("task_state_events",a.toString()).apply();}catch(Exception ignored){}
    }

    private String latestStatusToday(String id){ JSONArray a=events(); String date=dateKey(Calendar.getInstance()); for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i); if(o!=null&&date.equals(o.optString("date"))&&id.equals(o.optString("id"))) return o.optString("status")+(o.optString("reason").equals("—")?"":" — "+o.optString("reason"));} return ""; }

    private void buildBlockers(LinearLayout root){
        JSONArray a=events(); LinkedHashMap<String,Integer> counts=new LinkedHashMap<>(); int done=0, delayed=0, failed=0;
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i); if(o==null)continue; String st=o.optString("status"); if(st.equals("تم")){done++;continue;} if(st.equals("أجلت"))delayed++; else if(st.equals("تعثر"))failed++; String r=o.optString("reason"); counts.put(r,counts.containsKey(r)?counts.get(r)+1:1);}
        LinearLayout summary=card(); addCard(root,summary,14); summary.addView(text("سجل الحالات",18,NAVY,true)); summary.addView(text("تم: "+arabic(done)+"   •   تأجيل: "+arabic(delayed)+"   •   تعثر: "+arabic(failed),14,TEXT,true));
        String top="", action="سجّل أسبابك لعدة أيام وسيظهر النمط هنا."; int max=0; for(String r:counts.keySet())if(counts.get(r)>max){max=counts.get(r);top=r;}
        if(!top.isEmpty()) action=actionFor(top);
        LinearLayout c=card(); addCard(root,c,10); c.addView(text(top.isEmpty()?"لا يوجد عائق متكرر بعد":"أكثر عائق: "+top,18,top.isEmpty()?MUTED:RED,true)); if(max>0)c.addView(text("تكرر "+arabic(max)+" مرة",13,MUTED,false)); TextView ac=text(action,14,GREEN,true); ac.setPadding(0,dp(8),0,0); c.addView(ac);
        TextView h=text("تفصيل الأسباب",19,NAVY,true); h.setPadding(0,dp(18),0,dp(5)); root.addView(h); for(String r:counts.keySet()){LinearLayout x=card(); addCard(root,x,6); x.addView(text(r+"  —  "+arabic(counts.get(r))+" مرة",14,TEXT,true));}
    }

    private String actionFor(String r){ if(r.contains("فكرة"))return "الإجراء: اكتبها فورًا في «لاحقًا» ولا تبحث عنها حتى المراجعة الأسبوعية."; if(r.contains("يوتيوب"))return "الإجراء: لا تفتح يوتيوب قبل إنهاء المهمة الحالية، واجعل الترفيه في وقته المحدد."; if(r.contains("نوم")||r.contains("تعب"))return "الإجراء: قدّم إغلاق اليوم والنوم، واستخدم النسخة القصيرة من المهمة عند الإرهاق."; if(r.contains("طلب"))return "الإجراء: تدرب على: حاضر، بعد أن أنهي هذه المهمة/بعد الصلاة."; if(r.contains("وقت"))return "الإجراء: خفّض حجم المهمة وحدد ناتجًا واحدًا فقط لها."; if(r.contains("أفهم"))return "الإجراء: حوّل المهمة إلى أول خطوة صغيرة واضحة قبل بدء وقتها."; if(r.contains("أرغب"))return "الإجراء: ابدأ بخمس دقائق فقط؛ المطلوب بدء الحركة لا إنهاء كل شيء."; return "الإجراء: راجع هذا العائق في المراجعة الأسبوعية واختر تعديلًا واحدًا فقط."; }

    private void buildNotify(LinearLayout root){
        LinearLayout c=card(); addCard(root,c,14); c.addView(text("التذكيرات الذكية",19,NAVY,true)); Switch sw=new Switch(this); sw.setText("تشغيل تذكيرات المهام المهمة"); sw.setTextSize(15); sw.setChecked(prefs.getBoolean("smart_reminders_enabled",true)); sw.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("smart_reminders_enabled",on).apply(); if(on)ReminderScheduler.scheduleNextSevenDays(this); Toast.makeText(this,on?"تم تشغيل التذكيرات":"تم إيقاف التذكيرات الجديدة",Toast.LENGTH_SHORT).show();}); c.addView(sw);
        c.addView(text("التذكير الحالي قبل المهمة بـ١٠ دقائق: الإنجليزية، العمل، القرآن قبل الظهر، القرآن قبل العصر، وإغلاق اليوم.",13,MUTED,false));
        Button now=button("إعادة جدولة الأيام السبعة",GREEN); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46)); lp.setMargins(0,dp(10),0,0); c.addView(now,lp); now.setOnClickListener(v->{ReminderScheduler.scheduleNextSevenDays(this);Toast.makeText(this,"تمت إعادة الجدولة",Toast.LENGTH_SHORT).show();});
    }

    private JSONArray events(){try{return new JSONArray(prefs.getString("task_state_events","[]"));}catch(Exception e){return new JSONArray();}}
    private JSONArray getExtras(){try{return new JSONArray(prefs.getString("week_extra_tasks","[]"));}catch(Exception e){return new JSONArray();}}
    private void deleteExtra(int idx){JSONArray a=getExtras(); if(idx>=0&&idx<a.length())a.remove(idx); prefs.edit().putString("week_extra_tasks",a.toString()).apply(); render();}
    private Calendar saturdayStart(Calendar b){Calendar c=(Calendar)b.clone(); int x=(c.get(Calendar.DAY_OF_WEEK)-Calendar.SATURDAY+7)%7; c.add(Calendar.DAY_OF_MONTH,-x); return c;}
    private String dateKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    private String format12(int h,int m){String ap=h<12?"ص":"م"; int x=h%12;if(x==0)x=12;return arabic(x)+":"+(m<10?"٠":"")+arabic(m)+" "+ap;}
    private String arabic(int n){return String.valueOf(n).replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤').replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');}
    private void addNote(LinearLayout r,String s){LinearLayout c=card();addCard(r,c,12);c.setBackground(round(Color.rgb(235,247,243),16));c.addView(text(s,13,TEXT,false));}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(round(Color.WHITE,16));c.setElevation(dp(1));return c;}
    private void addCard(LinearLayout r,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(top),0,0);r.addView(v,lp);}
    private TextView text(String s,int z,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setBackground(round(color,12));return b;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(42),1);lp.setMargins(dp(2),0,dp(2),0);return lp;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setGravity(Gravity.RIGHT);e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);e.setSingleLine(false);e.setMaxLines(2);return e;}
    private Spinner spinner(String[] x){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,x));return s;}
    private View label(String s,View v){LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.VERTICAL);w.setPadding(0,dp(7),0,0);w.addView(text(s,12,MUTED,true));w.addView(v);return w;}
    private GradientDrawable round(int c,int rad){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(rad));return d;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
