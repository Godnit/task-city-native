package com.masari.personalplan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PlannerCenterActivity extends Activity {
    private static final int BG = Color.rgb(246,248,252);
    private static final int NAVY = Color.rgb(24,49,83);
    private static final int TEXT = Color.rgb(31,40,54);
    private static final int MUTED = Color.rgb(103,113,128);
    private static final int GREEN = Color.rgb(22,123,98);
    private static final int GOLD = Color.rgb(184,126,28);
    private static final int PURPLE = Color.rgb(115,83,165);
    private static final int RED = Color.rgb(172,70,61);
    private static final int BORDER = Color.rgb(225,231,239);
    private static final int REQ_EXPORT = 701;
    private static final int REQ_IMPORT = 702;

    private SharedPreferences prefs;
    private String mode = "home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("masari_data", MODE_PRIVATE);
        render();
    }

    private void render() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root);
        switch (mode) {
            case "weekly": buildWeekly(root); break;
            case "monthly": buildMonthly(root); break;
            case "custom": buildCustomTasks(root); break;
            case "history": buildHistory(root, 30, false); break;
            case "strong": buildHistory(root, 30, true); break;
            case "data": buildData(root); break;
            default: buildHome(root);
        }
        setContentView(scroll);
    }

    private void addHeader(LinearLayout root) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("مركز المتابعة", 27, NAVY, true));
        titles.addView(text("راجع، عدّل، ثم ارجع للتنفيذ", 13, MUTED, false));
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button back = button(mode.equals("home") ? "رجوع" : "الرئيسية", NAVY);
        row.addView(back, new LinearLayout.LayoutParams(dp(94), dp(42)));
        back.setOnClickListener(v -> {
            if (mode.equals("home")) finish();
            else { mode = "home"; render(); }
        });
        root.addView(row);
    }

    private void buildHome(LinearLayout root) {
        addSummaryCard(root);
        addMenu(root, "المراجعة الأسبوعية", "ما أنجزت؟ ما الذي قطعني؟ وما تعديل الأسبوع القادم؟", GREEN, () -> open("weekly"));
        addMenu(root, "المراجعة الشهرية", "صورة الشهر + الهدف الأهم للشهر التالي.", GOLD, () -> open("monthly"));
        addMenu(root, "المهام المخصصة", "أضف مهمة متكررة بوقت ونقاط ومجال محدد.", PURPLE, () -> open("custom"));
        addMenu(root, "سجل آخر ٣٠ يومًا", "النقاط والنسبة لكل يوم.", NAVY, () -> open("history"));
        addMenu(root, "الأيام القوية فقط", "يعرض الأيام التي وصلت فيها إلى ٨٠٪ أو أكثر.", GREEN, () -> open("strong"));
        addMenu(root, "النسخة الاحتياطية", "تصدير كل بيانات مساري أو استعادتها من ملف JSON.", RED, () -> open("data"));
    }

    private void addSummaryCard(LinearLayout root) {
        int weekPts = 0, weekTarget = 0, strong = 0;
        Calendar d = saturdayStart(Calendar.getInstance());
        for (int i=0;i<7;i++) {
            int p = dayPoints(d), t = dayTarget(d);
            weekPts += p; weekTarget += t;
            if (percent(p,t)>=80) strong++;
            d.add(Calendar.DAY_OF_MONTH,1);
        }
        LinearLayout c = card();
        c.setPadding(dp(16),dp(14),dp(16),dp(14));
        add(root,c,14);
        c.addView(text("هذا الأسبوع",18,NAVY,true));
        c.addView(text(ar(weekPts)+" / "+ar(weekTarget)+" نقطة",22,GREEN,true));
        c.addView(progress(weekPts,weekTarget,GREEN));
        c.addView(detail("أيام قوية حتى الآن: "+ar(strong)+" من ٧"));
    }

    private void buildWeekly(LinearLayout root) {
        TextView h = text("مراجعة الأسبوع",22,NAVY,true); h.setPadding(0,dp(16),0,dp(6)); root.addView(h);
        Calendar d = saturdayStart(Calendar.getInstance());
        int points=0,target=0,strong=0;
        for(int i=0;i<7;i++) { int p=dayPoints(d),t=dayTarget(d); points+=p;target+=t;if(percent(p,t)>=80)strong++; d.add(Calendar.DAY_OF_MONTH,1); }
        addStat(root,"نسبة الأسبوع",ar(percent(points,target))+"٪",GREEN);
        addStat(root,"الأيام القوية",ar(strong)+" / ٧",GOLD);
        addStat(root,"النقاط",ar(points)+" / "+ar(target),NAVY);
        String key = "weekly_review_" + weekKey(Calendar.getInstance());
        JSONObject saved = json(prefs.getString(key,"{}"));
        EditText best = field("أفضل إنجازين هذا الأسبوع", saved.optString("best"));
        EditText blockers = field("ما الذي قطعني أو شتتني؟", saved.optString("blockers"));
        EditText fix = field("مشكلة واحدة سأصلحها الأسبوع القادم", saved.optString("fix"));
        root.addView(best); root.addView(blockers); root.addView(fix);
        Button save = button("حفظ مراجعة الأسبوع",GREEN); root.addView(save,fullButtonLp());
        save.setOnClickListener(v -> {
            try {
                JSONObject o=new JSONObject();o.put("best",best.getText().toString().trim());o.put("blockers",blockers.getText().toString().trim());o.put("fix",fix.getText().toString().trim());o.put("saved",dateKey(Calendar.getInstance()));
                prefs.edit().putString(key,o.toString()).apply();
                Toast.makeText(this,"تم حفظ مراجعة الأسبوع",Toast.LENGTH_SHORT).show();
            } catch(Exception ignored){}
        });
    }

    private void buildMonthly(LinearLayout root) {
        TextView h = text("مراجعة الشهر",22,NAVY,true); h.setPadding(0,dp(16),0,dp(6)); root.addView(h);
        Calendar d=Calendar.getInstance(); d.set(Calendar.DAY_OF_MONTH,1); int month=d.get(Calendar.MONTH); int pts=0,target=0,strong=0;
        while(d.get(Calendar.MONTH)==month){int p=dayPoints(d),t=dayTarget(d);pts+=p;target+=t;if(percent(p,t)>=80)strong++;d.add(Calendar.DAY_OF_MONTH,1);}
        addStat(root,"نسبة الشهر",ar(percent(pts,target))+"٪",GREEN);
        addStat(root,"أيام قوية",ar(strong),GOLD);
        addStat(root,"كتب LeapAhead",ar(monthDone("leap1")+monthDone("leap2")+monthDone("leap3")),PURPLE);
        addStat(root,"جلسات الإنجليزية",ar(monthDone("english")),Color.rgb(46,94,170));
        addStat(root,"جلسات القرآن",ar(monthDone("quran1")+monthDone("quran2")),GREEN);
        String key="monthly_review_"+new SimpleDateFormat("yyyy-MM",Locale.US).format(Calendar.getInstance().getTime());
        JSONObject saved=json(prefs.getString(key,"{}"));
        EditText win=field("أكبر تقدم هذا الشهر",saved.optString("win"));
        EditText lesson=field("أهم درس تعلمته عن نفسي",saved.optString("lesson"));
        EditText next=field("الهدف الأهم للشهر القادم",saved.optString("next"));
        root.addView(win);root.addView(lesson);root.addView(next);
        Button save=button("حفظ مراجعة الشهر",GOLD);root.addView(save,fullButtonLp());
        save.setOnClickListener(v->{try{JSONObject o=new JSONObject();o.put("win",win.getText().toString().trim());o.put("lesson",lesson.getText().toString().trim());o.put("next",next.getText().toString().trim());prefs.edit().putString(key,o.toString()).apply();Toast.makeText(this,"تم الحفظ",Toast.LENGTH_SHORT).show();}catch(Exception ignored){}});
    }

    private void buildCustomTasks(LinearLayout root) {
        TextView h=text("المهام المخصصة",22,NAVY,true);h.setPadding(0,dp(16),0,dp(5));root.addView(h);
        root.addView(detail("استخدمها للأشياء الجديدة التي تريد إضافتها للنظام دون تعديل الكود. اختر إن كانت أساسية فتدخل في هدف اليوم، أو إضافية للمكافأة فقط."));
        Button add=button("＋ إضافة مهمة مخصصة",PURPLE);root.addView(add,fullButtonLp());add.setOnClickListener(v->showCustomTaskDialog());
        JSONArray arr=customTasks();
        if(arr.length()==0){LinearLayout c=card();c.setPadding(dp(14),dp(14),dp(14),dp(14));add(root,c,8);c.addView(text("لا توجد مهام مخصصة بعد.",14,MUTED,false));return;}
        for(int i=0;i<arr.length();i++){
            JSONObject o=arr.optJSONObject(i); if(o==null)continue; final int idx=i;
            LinearLayout c=card();c.setPadding(dp(14),dp(12),dp(14),dp(12));add(root,c,7);
            LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(text(o.optString("title"),16,TEXT,true));info.addView(text(timeRange(o.optInt("start"),o.optInt("end"))+" • "+dayName(o.optInt("day",0)),12,MUTED,false));line.addView(info,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
            line.addView(pill("+"+ar(o.optInt("points",10)),PURPLE,Color.rgb(248,245,252)));c.addView(line);
            c.addView(detail(o.optString("domain","الانضباط")+" • "+(o.optBoolean("required",false)?"أساسية":"إضافية")));
            LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);actions.setPadding(0,dp(8),0,0);
            Button toggle=button(o.optBoolean("active",true)?"إيقاف":"تشغيل",NAVY);Button del=button("حذف",RED);actions.addView(toggle,weightButton());actions.addView(del,weightButton());c.addView(actions);
            toggle.setOnClickListener(v->{JSONArray a=customTasks();JSONObject x=a.optJSONObject(idx);if(x!=null){try{x.put("active",!x.optBoolean("active",true));saveCustomTasks(a);render();}catch(Exception ignored){}}});
            del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف المهمة؟").setPositiveButton("حذف",(d,w)->{JSONArray a=customTasks();a.remove(idx);saveCustomTasks(a);render();}).setNegativeButton("إلغاء",null).show());
        }
    }

    private void showCustomTaskDialog() {
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),0,dp(18),0);box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText title=field("اسم المهمة، مثال: مراجعة الإسعافات","");
        EditText details=field("ما المطلوب بالضبط؟","");
        EditText start=singleField("وقت البداية HH:MM مثل 16:30","16:30");
        EditText end=singleField("وقت النهاية HH:MM مثل 17:00","17:00");
        EditText points=singleField("النقاط","10");points.setInputType(InputType.TYPE_CLASS_NUMBER);
        String[] domains={"القرآن","الإنجليزية والقبول","العمل والدخل","الصحة","المعرفة والقراءة","التواصل","الأسرة","الدين والمسجد","الانضباط"};
        Spinner domain=spinner(domains);
        String[] days={"كل يوم","السبت","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة"};
        Spinner day=spinner(days);
        CheckBox required=new CheckBox(this);required.setText("مهمة أساسية تدخل في هدف اليوم");required.setTextColor(TEXT);required.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.addView(title);box.addView(details);box.addView(label("المجال",domain));box.addView(label("التكرار",day));box.addView(start);box.addView(end);box.addView(points);box.addView(required);
        new AlertDialog.Builder(this).setTitle("مهمة مخصصة").setView(box).setPositiveButton("حفظ",(d,w)->{
            String name=title.getText().toString().trim();int s=parseTime(start.getText().toString(),-1);int e=parseTime(end.getText().toString(),-1);
            if(name.isEmpty()||s<0||e<=s){Toast.makeText(this,"تحقق من الاسم والوقت",Toast.LENGTH_LONG).show();return;}
            int pts=10;try{pts=Math.max(1,Math.min(100,Integer.parseInt(points.getText().toString().trim())));}catch(Exception ignored){}
            try{JSONArray arr=customTasks();JSONObject o=new JSONObject();o.put("id","custom_"+System.currentTimeMillis());o.put("title",name);o.put("details",details.getText().toString().trim());o.put("domain",domain.getSelectedItem().toString());o.put("day",spinnerDayToCalendar(day.getSelectedItemPosition()));o.put("start",s);o.put("end",e);o.put("points",pts);o.put("required",required.isChecked());o.put("active",true);arr.put(o);saveCustomTasks(arr);render();}catch(Exception ignored){}
        }).setNegativeButton("إلغاء",null).show();
    }

    private void buildHistory(LinearLayout root,int days,boolean strongOnly) {
        TextView h=text(strongOnly?"الأيام القوية":"سجل آخر ٣٠ يومًا",22,NAVY,true);h.setPadding(0,dp(16),0,dp(5));root.addView(h);
        Calendar d=Calendar.getInstance();d.add(Calendar.DAY_OF_MONTH,-days+1);int shown=0;
        for(int i=0;i<days;i++){
            int p=dayPoints(d),t=dayTarget(d),pct=percent(p,t);if(!strongOnly||pct>=80){shown++;LinearLayout c=card();c.setPadding(dp(13),dp(10),dp(13),dp(10));add(root,c,5);LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);String label=new SimpleDateFormat("EEEE d/M",new Locale("ar")).format(d.getTime());line.addView(text(label,14,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));line.addView(pill(ar(pct)+"٪",pct>=80?GREEN:(pct>=50?GOLD:RED),Color.rgb(248,250,253)));c.addView(line);c.addView(progress(p,t,pct>=80?GREEN:(pct>=50?GOLD:RED)));c.addView(detail(ar(p)+" / "+ar(t)+" نقطة"));}
            d.add(Calendar.DAY_OF_MONTH,1);
        }
        if(shown==0)root.addView(detail("لا توجد أيام مطابقة في هذه الفترة."));
    }

    private void buildData(LinearLayout root) {
        TextView h=text("النسخة الاحتياطية",22,NAVY,true);h.setPadding(0,dp(16),0,dp(6));root.addView(h);
        root.addView(detail("التصدير يحفظ المهام والنقاط والأوسمة والمراجعات وقائمة لاحقًا والمكافآت في ملف واحد. الاستعادة تستبدل بيانات التطبيق الحالية بالنسخة المختارة."));
        Button export=button("تصدير نسخة احتياطية",GREEN);root.addView(export,fullButtonLp());
        Button restore=button("استعادة من ملف",GOLD);root.addView(restore,fullButtonLp());
        export.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"masari-backup-"+dateKey(Calendar.getInstance())+".json");startActivityForResult(i,REQ_EXPORT);});
        restore.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");startActivityForResult(i,REQ_IMPORT);});
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{if(requestCode==REQ_EXPORT){String payload=makeBackup();try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(payload.getBytes(StandardCharsets.UTF_8));}Toast.makeText(this,"تم تصدير النسخة الاحتياطية",Toast.LENGTH_LONG).show();}else if(requestCode==REQ_IMPORT){StringBuilder sb=new StringBuilder();try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))){String line;while((line=br.readLine())!=null)sb.append(line);}restoreBackup(sb.toString());Toast.makeText(this,"تمت الاستعادة. أعد فتح مساري.",Toast.LENGTH_LONG).show();}}catch(Exception e){Toast.makeText(this,"تعذر تنفيذ العملية",Toast.LENGTH_LONG).show();}}

    private String makeBackup() throws Exception {JSONObject root=new JSONObject();root.put("format","masari-backup-v1");root.put("date",System.currentTimeMillis());JSONObject data=new JSONObject();for(Map.Entry<String,?> e:prefs.getAll().entrySet()){Object v=e.getValue();JSONObject item=new JSONObject();if(v instanceof Integer){item.put("t","i");item.put("v",v);}else if(v instanceof Boolean){item.put("t","b");item.put("v",v);}else if(v instanceof Long){item.put("t","l");item.put("v",v);}else if(v instanceof Float){item.put("t","f");item.put("v",v);}else if(v instanceof String){item.put("t","s");item.put("v",v);}else if(v instanceof Set){item.put("t","ss");JSONArray a=new JSONArray();for(Object x:(Set<?>)v)a.put(String.valueOf(x));item.put("v",a);}else continue;data.put(e.getKey(),item);}root.put("data",data);return root.toString(2);}

    private void restoreBackup(String raw) throws Exception {JSONObject root=new JSONObject(raw);if(!root.optString("format").startsWith("masari-backup"))throw new Exception();JSONObject data=root.getJSONObject("data");SharedPreferences.Editor ed=prefs.edit().clear();java.util.Iterator<String> keys=data.keys();while(keys.hasNext()){String k=keys.next();JSONObject item=data.getJSONObject(k);String t=item.getString("t");switch(t){case"i":ed.putInt(k,item.getInt("v"));break;case"b":ed.putBoolean(k,item.getBoolean("v"));break;case"l":ed.putLong(k,item.getLong("v"));break;case"f":ed.putFloat(k,(float)item.getDouble("v"));break;case"s":ed.putString(k,item.optString("v"));break;case"ss":JSONArray a=item.getJSONArray("v");java.util.HashSet<String> set=new java.util.HashSet<>();for(int i=0;i<a.length();i++)set.add(a.getString(i));ed.putStringSet(k,set);break;}}ed.apply();}

    private void addMenu(LinearLayout root,String title,String note,int color,Runnable action){LinearLayout c=card();c.setPadding(dp(15),dp(13),dp(15),dp(13));add(root,c,8);c.addView(text(title,17,color,true));c.addView(detail(note));c.setOnClickListener(v->action.run());}
    private void open(String x){mode=x;render();}
    private void addStat(LinearLayout root,String name,String value,int color){LinearLayout c=card();c.setPadding(dp(13),dp(10),dp(13),dp(10));add(root,c,5);LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);line.setGravity(Gravity.CENTER_VERTICAL);line.addView(text(name,14,TEXT,true),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));line.addView(pill(value,color,Color.rgb(248,250,253)));c.addView(line);}

    private int dayPoints(Calendar c){return prefs.getInt("reward_day_points_"+dateKey(c),0);}
    private int dayTarget(Calendar c){int day=c.get(Calendar.DAY_OF_WEEK);int total=5+5+7+14+3+8+18+5+5+5;if(day==Calendar.SATURDAY||day==Calendar.TUESDAY)total+=20;if(day!=Calendar.FRIDAY)total+=25;if(day!=Calendar.FRIDAY)total+=25;switch(day){case Calendar.SATURDAY:total+=8;break;case Calendar.SUNDAY:total+=10;break;case Calendar.MONDAY:total+=8;break;case Calendar.TUESDAY:total+=10;break;case Calendar.WEDNESDAY:total+=12;break;case Calendar.THURSDAY:total+=12;break;}total+=customTargetForDay(day);return Math.max(1,total);}
    private int customTargetForDay(int dow){int total=0;JSONArray a=customTasks();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||!o.optBoolean("active",true)||!o.optBoolean("required",false))continue;int day=o.optInt("day",0);if(day==0||day==dow)total+=Math.max(1,o.optInt("points",10));}return total;}
    private int monthDone(String id){Calendar d=Calendar.getInstance();d.set(Calendar.DAY_OF_MONTH,1);int m=d.get(Calendar.MONTH),n=0;while(d.get(Calendar.MONTH)==m){if(prefs.getBoolean("reward_done_"+dateKey(d)+"_"+id,false))n++;d.add(Calendar.DAY_OF_MONTH,1);}return n;}
    private Calendar saturdayStart(Calendar base){Calendar c=(Calendar)base.clone();int dow=c.get(Calendar.DAY_OF_WEEK);c.add(Calendar.DAY_OF_MONTH,-((dow-Calendar.SATURDAY+7)%7));return c;}
    private String weekKey(Calendar c){Calendar s=saturdayStart(c);return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(s.getTime());}
    private int percent(int v,int t){return Math.min(100,Math.round(v*100f/Math.max(1,t)));}
    private String dateKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    private JSONObject json(String s){try{return new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private JSONArray customTasks(){try{return new JSONArray(prefs.getString("custom_tasks","[]"));}catch(Exception e){return new JSONArray();}}
    private void saveCustomTasks(JSONArray a){prefs.edit().putString("custom_tasks",a.toString()).apply();}
    private int spinnerDayToCalendar(int pos){switch(pos){case 1:return Calendar.SATURDAY;case 2:return Calendar.SUNDAY;case 3:return Calendar.MONDAY;case 4:return Calendar.TUESDAY;case 5:return Calendar.WEDNESDAY;case 6:return Calendar.THURSDAY;case 7:return Calendar.FRIDAY;default:return 0;}}
    private String dayName(int day){switch(day){case Calendar.SATURDAY:return"السبت";case Calendar.SUNDAY:return"الأحد";case Calendar.MONDAY:return"الاثنين";case Calendar.TUESDAY:return"الثلاثاء";case Calendar.WEDNESDAY:return"الأربعاء";case Calendar.THURSDAY:return"الخميس";case Calendar.FRIDAY:return"الجمعة";default:return"كل يوم";}}
    private int parseTime(String s,int fallback){try{String[]p=s.trim().split(":");int h=Integer.parseInt(p[0]),m=Integer.parseInt(p[1]);if(h<0||h>23||m<0||m>59)return fallback;return h*60+m;}catch(Exception e){return fallback;}}
    private String timeRange(int start,int end){return hm(start)+" - "+hm(end);}
    private String hm(int min){int h=(min/60)%24,m=min%60;String ap=h<12?"ص":"م";int hh=h%12;if(hh==0)hh=12;return ar(hh)+":"+(m<10?"٠":"")+ar(m)+" "+ap;}

    private ScrollView baseScroll(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);s.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return s;}
    private LinearLayout baseRoot(ScrollView s){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(17),dp(16),dp(17),dp(34));r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);s.addView(r);return r;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);c.setBackground(round(Color.WHITE,18,BORDER));c.setElevation(dp(1));return c;}
    private void add(LinearLayout root,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(top),0,0);root.addView(v,lp);}
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private TextView detail(String s){TextView t=text(s,12,MUTED,false);t.setPadding(0,dp(5),0,0);return t;}
    private TextView pill(String s,int color,int bg){TextView t=text(s,12,color,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(9),dp(5),dp(9),dp(5));t.setBackground(round(bg,20));return t;}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(color,13));return b;}
    private EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(14);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setGravity(Gravity.RIGHT);e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);e.setMinLines(2);e.setMaxLines(4);e.setPadding(dp(10),dp(8),dp(10),dp(8));e.setBackground(round(Color.WHITE,12,BORDER));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(8),0,0);e.setLayoutParams(lp);return e;}
    private EditText singleField(String hint,String value){EditText e=field(hint,value);e.setSingleLine(true);e.setMinLines(1);e.setMaxLines(1);return e;}
    private Spinner spinner(String[] values){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);s.setAdapter(a);return s;}
    private View label(String label,View v){LinearLayout w=new LinearLayout(this);w.setOrientation(LinearLayout.VERTICAL);w.setPadding(0,dp(7),0,0);w.addView(text(label,12,MUTED,true));w.addView(v,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48)));return w;}
    private ProgressBar progress(int value,int target,int color){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,target));p.setProgress(Math.min(value,Math.max(1,target)));p.setProgressTintList(android.content.res.ColorStateList.valueOf(color));p.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(232,236,242)));return p;}
    private LinearLayout.LayoutParams fullButtonLp(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));lp.setMargins(0,dp(10),0,0);return lp;}
    private LinearLayout.LayoutParams weightButton(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(42),1f);lp.setMargins(dp(3),0,dp(3),0);return lp;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable round(int color,int radius,int stroke){GradientDrawable d=round(color,radius);d.setStroke(dp(1),stroke);return d;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String ar(int n){return String.valueOf(n).replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤').replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');}
}
