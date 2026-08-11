package com.godnit.taskcity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private CityStore store;
    private CityGLSurfaceView city;
    private TextView houses, active;
    private FrameLayout panel;
    private int panelMode;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<String> demolitionQueue = new ArrayDeque<>();
    private boolean demolishing;
    private ToneGenerator tone;
    private SharedPreferences settings;
    private boolean sound = true, vibration = true;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            resolveExpired();
            refreshHud();
            if (panelMode == 1) renderPanel();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(68,174,221));
        getWindow().setNavigationBarColor(Color.rgb(233,247,255));
        store = new CityStore(this);
        settings = getSharedPreferences("task_city_settings", MODE_PRIVATE);
        sound = settings.getBoolean("sound", true);
        vibration = settings.getBoolean("vibration", true);
        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
        buildUi();
        city.setHouses(store.houses);
        resolveExpired();
        refreshHud();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(84,191,235));
        setContentView(root);
        city = new CityGLSurfaceView(this);
        root.addView(city, match());

        LinearLayout top = vertical();
        top.setPadding(dp(13),dp(9),dp(13),dp(9));
        top.setBackground(round(Color.argb(232,255,255,255),20));
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        tlp.setMargins(dp(10),dp(10),dp(10),0);
        root.addView(top,tlp);

        LinearLayout titleRow = horizontal();
        TextView title = label("مدينة الإنجاز",21,Color.rgb(24,55,69),Typeface.BOLD);
        titleRow.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        Button gear = button("⚙",false);
        gear.setOnClickListener(v -> showSettings());
        titleRow.addView(gear,new LinearLayout.LayoutParams(dp(50),dp(42)));
        top.addView(titleRow);

        LinearLayout stats = horizontal();
        houses = chip("🏠 0 بيت");
        active = chip("⏱ 0 مهمة نشطة");
        stats.addView(houses,new LinearLayout.LayoutParams(0,dp(38),1));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0,dp(38),1);
        alp.setMargins(dp(7),0,0,0);
        stats.addView(active,alp);
        top.addView(stats);

        TextView hint = label("اسحب بإصبع واحد • قرّب وبعّد بإصبعين",12,Color.rgb(33,75,87),Typeface.NORMAL);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(10),dp(7),dp(10),dp(7));
        hint.setBackground(round(Color.argb(205,239,250,255),16));
        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        hlp.topMargin=dp(145);
        root.addView(hint,hlp);
        handler.postDelayed(() -> hint.animate().alpha(0f).setDuration(400).start(),5000);

        LinearLayout bottom = horizontal();
        bottom.setPadding(dp(7),dp(6),dp(7),dp(7));
        bottom.setBackground(round(Color.argb(238,255,255,255),22));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-1,dp(72),Gravity.BOTTOM);
        blp.setMargins(dp(10),0,dp(10),dp(10));
        root.addView(bottom,blp);
        Button add=button("＋ مهمة",true), tasks=button("المهام",false), history=button("السجل",false), home=button("⌂",false);
        bottom.addView(add,weighted(1.2f)); bottom.addView(tasks,weighted(1)); bottom.addView(history,weighted(1)); bottom.addView(home,new LinearLayout.LayoutParams(dp(56),-1));
        add.setOnClickListener(v -> addTaskDialog());
        tasks.setOnClickListener(v -> togglePanel(1));
        history.setOnClickListener(v -> togglePanel(2));
        home.setOnClickListener(v -> {city.resetCamera(); toast("رجعنا إلى وسط المدينة");});

        panel = new FrameLayout(this);
        panel.setVisibility(View.GONE);
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(-1,dp(470),Gravity.BOTTOM);
        plp.setMargins(dp(10),0,dp(10),dp(88));
        root.addView(panel,plp);
    }

    private void togglePanel(int mode) {
        if (panelMode==mode) { panelMode=0; panel.setVisibility(View.GONE); return; }
        panelMode=mode; panel.setVisibility(View.VISIBLE); renderPanel();
    }

    private void renderPanel() {
        if (panelMode==0) return;
        panel.removeAllViews();
        LinearLayout card=vertical();
        card.setPadding(dp(13),dp(10),dp(13),dp(13));
        card.setBackground(round(Color.argb(250,255,255,255),23));
        panel.addView(card,match());
        LinearLayout head=horizontal();
        TextView h=label(panelMode==1?"المهام النشطة":"سجل المهام",19,Color.rgb(28,58,68),Typeface.BOLD);
        head.addView(h,new LinearLayout.LayoutParams(0,dp(42),1));
        Button close=button("✕",false); close.setOnClickListener(v->{panelMode=0;panel.setVisibility(View.GONE);});
        head.addView(close,new LinearLayout.LayoutParams(dp(48),dp(40))); card.addView(head);
        ScrollView scroll=new ScrollView(this); LinearLayout list=vertical(); scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
        card.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if (panelMode==1) {
            List<CityStore.TaskRecord> items=store.activeTasks();
            if(items.isEmpty()) empty(list,"لا توجد مهام نشطة.\nأضف مهمة وابدأ بناء مدينتك 🌱");
            else for(CityStore.TaskRecord t:items) list.addView(taskCard(t,true));
        } else {
            if(store.tasks.isEmpty()) empty(list,"سجل المهام فارغ حتى الآن.");
            else for(int i=store.tasks.size()-1;i>=0;i--) list.addView(taskCard(store.tasks.get(i),false));
        }
    }

    private View taskCard(CityStore.TaskRecord t, boolean activeMode) {
        LinearLayout box=vertical(); box.setPadding(dp(12),dp(9),dp(12),dp(9)); box.setBackground(round(Color.rgb(244,250,247),17));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(6),0,0); box.setLayoutParams(lp);
        TextView name=label(t.title,16,Color.rgb(29,62,58),Typeface.BOLD); name.setGravity(Gravity.RIGHT); box.addView(name);
        if(activeMode && t.status==CityStore.Status.ACTIVE) {
            TextView rem=label("الوقت المتبقي: "+remaining(t.deadline-System.currentTimeMillis()),14,Color.rgb(45,94,83),Typeface.NORMAL); rem.setGravity(Gravity.RIGHT); rem.setPadding(0,dp(4),0,dp(6)); box.addView(rem);
            Button done=button("✓ تم الإنجاز",true); done.setOnClickListener(v -> complete(t.id)); box.addView(done,new LinearLayout.LayoutParams(-1,dp(45)));
        } else {
            String s=t.status==CityStore.Status.DONE?"✓ منجزة":t.status==CityStore.Status.FAILED?"انتهى الوقت":t.status==CityStore.Status.CANCELLED?"ملغاة":"نشطة";
            int c=t.status==CityStore.Status.DONE?Color.rgb(22,153,105):t.status==CityStore.Status.FAILED?Color.rgb(202,80,65):Color.GRAY;
            TextView status=label(s+"  •  "+date(t.createdAt),13,c,Typeface.BOLD); status.setGravity(Gravity.RIGHT); status.setPadding(0,dp(5),0,0); box.addView(status);
        }
        return box;
    }

    private void addTaskDialog() {
        LinearLayout content=vertical(); content.setPadding(dp(8),0,dp(8),0);
        EditText title=new EditText(this); title.setHint("مثال: مراجعة فصل الأحياء"); title.setSingleLine(true); title.setGravity(Gravity.RIGHT); content.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView l=label("اختر الوقت المتاح",14,Color.rgb(45,70,75),Typeface.BOLD); l.setGravity(Gravity.RIGHT); content.addView(l);
        RadioGroup group=new RadioGroup(this); group.setOrientation(RadioGroup.VERTICAL); group.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        long[] mins={15,30,60,120}; String[] names={"15 دقيقة","30 دقيقة","ساعة واحدة","ساعتان"};
        for(int i=0;i<mins.length;i++){RadioButton r=new RadioButton(this);r.setText(names[i]);r.setTag(mins[i]);r.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);group.addView(r,new RadioGroup.LayoutParams(-1,dp(42)));if(i==1)r.setChecked(true);}
        RadioButton customRadio=new RadioButton(this); customRadio.setText("مدة مخصصة بالدقائق"); customRadio.setTag(-1L); customRadio.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); group.addView(customRadio,new RadioGroup.LayoutParams(-1,dp(42))); content.addView(group);
        EditText custom=new EditText(this); custom.setHint("مثال: 45"); custom.setInputType(InputType.TYPE_CLASS_NUMBER); custom.setGravity(Gravity.RIGHT); custom.setVisibility(View.GONE); content.addView(custom,new LinearLayout.LayoutParams(-1,dp(50)));
        group.setOnCheckedChangeListener((g,id)->{View v=g.findViewById(id);custom.setVisibility(v!=null && ((Long)v.getTag())<0?View.VISIBLE:View.GONE);});
        AlertDialog d=new AlertDialog.Builder(this).setTitle("مهمة جديدة").setView(content).setNegativeButton("إلغاء",null).setPositiveButton("ابدأ المهمة",null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String name=title.getText().toString().trim(); if(name.isEmpty()){title.setError("اكتب اسم المهمة");return;} if(name.length()>60){title.setError("الحد الأقصى 60 حرفًا");return;}
            RadioButton selected=group.findViewById(group.getCheckedRadioButtonId()); long m=selected==null?30L:(Long)selected.getTag();
            if(m<0){try{m=Long.parseLong(custom.getText().toString());}catch(Exception e){m=0;}if(m<1||m>10080){custom.setError("اكتب مدة بين 1 و10080 دقيقة");return;}}
            store.addTask(name,m*60000L); feedback(true,false); refreshHud(); if(panelMode==1)renderPanel(); toast("بدأت المهمة ⏱"); d.dismiss();
        })); d.show();
    }

    private void complete(String id) {
        CityStore.TaskRecord t=store.findTask(id); if(t==null||t.status!=CityStore.Status.ACTIVE)return;
        if(System.currentTimeMillis()>=t.deadline){resolveExpired();return;}
        t.status=CityStore.Status.DONE;
        CityStore.HouseRecord h=store.buildHouse(t.id,CityRenderer.PLOT_COUNT); store.save();
        if(h!=null){city.setHouses(store.houses);city.animateBuild(h.plot);feedback(true,true);toast("أحسنت! بُني بيت جديد 🏠");}
        else {feedback(true,false);toast("تم الإنجاز، لكن الحي ممتلئ. سنفتح توسعة لاحقًا.");}
        refreshHud(); if(panelMode!=0)renderPanel();
    }

    private void resolveExpired() {
        long now=System.currentTimeMillis(); List<CityStore.TaskRecord> expired=new ArrayList<>();
        for(CityStore.TaskRecord t:store.tasks) if(t.status==CityStore.Status.ACTIVE && now>=t.deadline){t.status=CityStore.Status.FAILED;expired.add(t);}
        if(expired.isEmpty())return; store.save(); for(CityStore.TaskRecord t:expired)demolitionQueue.offer(t.title); nextDemolition();
    }

    private void nextDemolition() {
        if(demolishing)return; String title=demolitionQueue.poll(); if(title==null)return; CityStore.HouseRecord h=store.latestHouse();
        if(h==null){feedback(false,false);toast("انتهى وقت: "+title+" — لا يوجد بيت ليُهدم");nextDemolition();return;}
        demolishing=true; int plot=h.plot; store.removeHouse(h.id); city.animateDemolish(plot); feedback(false,true); toast("انتهى وقت: "+title+" — هُدم أحدث بيت"); refreshHud();
        handler.postDelayed(()->{city.setHouses(store.houses);demolishing=false;refreshHud();if(panelMode!=0)renderPanel();nextDemolition();},680);
    }

    private void showSettings() {
        String[] items={(sound?"✓ ":"")+"الصوت",(vibration?"✓ ":"")+"الاهتزاز","إعادة الكاميرا للوسط","مسح كل البيانات"};
        new AlertDialog.Builder(this).setTitle("الإعدادات").setItems(items,(d,w)->{
            if(w==0){sound=!sound;settings.edit().putBoolean("sound",sound).apply();}
            else if(w==1){vibration=!vibration;settings.edit().putBoolean("vibration",vibration).apply();}
            else if(w==2)city.resetCamera(); else clearConfirm();
        }).setNegativeButton("إغلاق",null).show();
    }

    private void clearConfirm() {
        new AlertDialog.Builder(this).setTitle("مسح المدينة؟").setMessage("سيتم حذف جميع المهام والبيوت المحفوظة على هذا الجهاز.").setNegativeButton("إلغاء",null).setPositiveButton("مسح",(d,w)->{
            store.clearAll();demolitionQueue.clear();city.setHouses(store.houses);city.resetCamera();refreshHud();if(panelMode!=0)renderPanel();toast("بدأت مدينة جديدة 🌱");
        }).show();
    }

    private void feedback(boolean ok, boolean strong) {
        if(sound&&tone!=null)tone.startTone(ok?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_NACK,ok?90:120);
        if(vibration&&strong){Vibrator v=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(ok?38:70,ok?90:70));}
    }

    private void refreshHud(){houses.setText("🏠 "+store.houses.size()+" بيت");active.setText("⏱ "+store.activeCount()+" مهمة نشطة");}
    private String remaining(long ms){if(ms<=0)return"00:00";long sec=ms/1000,h=sec/3600,m=(sec%3600)/60,s=sec%60;return h>0?String.format(Locale.US,"%02d:%02d:%02d",h,m,s):String.format(Locale.US,"%02d:%02d",m,s);}
    private String date(long t){return new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.US).format(new Date(t));}
    private void empty(LinearLayout l,String s){TextView v=label(s,15,Color.rgb(83,111,118),Typeface.NORMAL);v.setGravity(Gravity.CENTER);v.setPadding(dp(10),dp(55),dp(10),0);l.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    private TextView chip(String s){TextView t=label(s,13,Color.rgb(29,79,72),Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setBackground(round(Color.rgb(231,248,240),18));return t;}
    private TextView label(String s,int size,int color,int style){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.DEFAULT,style);t.setTextDirection(View.TEXT_DIRECTION_RTL);return t;}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(primary?Color.WHITE:Color.rgb(37,70,78));b.setPadding(dp(3),0,dp(3),0);b.setBackground(round(primary?Color.rgb(26,171,124):Color.rgb(239,247,249),17));return b;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    private LinearLayout.LayoutParams weighted(float w){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,w);p.setMargins(dp(4),0,0,0);return p;}
    private FrameLayout.LayoutParams match(){return new FrameLayout.LayoutParams(-1,-1);}
    private GradientDrawable round(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    @Override protected void onResume(){super.onResume();if(city!=null)city.onResume();resolveExpired();handler.removeCallbacks(ticker);handler.post(ticker);}
    @Override protected void onPause(){handler.removeCallbacks(ticker);if(store!=null)store.save();if(city!=null)city.onPause();super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(tone!=null)tone.release();super.onDestroy();}
}
