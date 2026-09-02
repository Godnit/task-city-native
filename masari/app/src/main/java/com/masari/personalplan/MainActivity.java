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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(20, 108, 91);
    private static final int NAVY = Color.rgb(25, 54, 93);
    private static final int TEXT = Color.rgb(30, 40, 55);
    private static final int MUTED = Color.rgb(101, 112, 128);
    private static final int BG = Color.rgb(246, 248, 252);
    private static final int GOLD = Color.rgb(173, 118, 24);
    private static final int SOFT_GREEN = Color.rgb(235, 247, 243);

    private SharedPreferences prefs;
    private String todayKey;
    private String activeTab = "today";
    private final EditText[] taskInputs = new EditText[3];
    private final CheckBox[] taskChecks = new CheckBox[3];
    private final CheckBox[] indicatorChecks = new CheckBox[5];
    private TextView priorityProgress;
    private TextView indicatorProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("masari_data", Context.MODE_PRIVATE);
        todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        showTab("today");
    }

    private void showTab(String tab) {
        saveTasks(false);
        activeTab = tab;
        setContentView(buildApp());
    }

    private View buildApp() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        FrameLayout content = new FrameLayout(this);
        shell.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        View screen;
        switch (activeTab) {
            case "plan": screen = buildPlanScreen(); break;
            case "progress": screen = buildProgressScreen(); break;
            case "later": screen = buildLaterScreen(); break;
            case "more": screen = buildMoreScreen(); break;
            default: screen = buildTodayScreen();
        }
        content.addView(screen, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        shell.addView(buildBottomNav(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));
        return shell;
    }

    private View buildTodayScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "خطة اليوم • مرحلة التأسيس");
        addTodayActions(root);
        addTopThreeCard(root);
        addReserveCard(root);
        addIndicatorsCard(root);

        TextView title = text("جدول اليوم", 22, NAVY, true);
        title.setPadding(0, dp(22), 0, dp(4));
        root.addView(title);
        TextView sub = text("كل فترة لها عمل واحد واضح، والوقت مكتوب بنظام ١٢ ساعة.", 13, MUTED, false);
        sub.setPadding(0, 0, 0, dp(8));
        root.addView(sub);

        for (String[] row : scheduleForToday()) {
            addTimelineRow(root, row[0], row[1], row[2], "1".equals(row[3]));
        }
        return scroll;
    }

    private void addTodayActions(LinearLayout root) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(14), 0, 0);
        root.addView(row, rowLp);

        Button idea = smallButton("＋ لاحقًا", GREEN);
        row.addView(idea, buttonWeight());
        idea.setOnClickListener(v -> showQuickIdeaDialog());

        int emergency = prefs.getInt("emergency_" + todayKey, 0);
        Button urgent = smallButton(emergency > 0 ? "⚡ طارئ " + arabicNumber(emergency) + "د" : "⚡ طارئ", GOLD);
        row.addView(urgent, buttonWeight());
        urgent.setOnClickListener(v -> showEmergencyDialog());

        Button flex = smallButton(isFlexibleToday() ? "✓ اليوم مرن" : "يوم مرن", NAVY);
        row.addView(flex, buttonWeight());
        flex.setOnClickListener(v -> toggleFlexibleToday());
    }

    private void addTopThreeCard(LinearLayout root) {
        LinearLayout topCard = card();
        topCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        addCard(root, topCard, dp(14));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView topTitle = text("أهم ٣ أشياء اليوم", 20, NAVY, true);
        head.addView(topTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        priorityProgress = pill("أنجزت ٠ من ٣", GREEN);
        head.addView(priorityProgress);
        topCard.addView(head);

        String[] defaults = {
                "مراجعة ٤ صفحات من القرآن: صفحتان قبل الظهر وصفحتان قبل العصر",
                "الإنجليزية: ١٥ مفردة + ٢٠ دقيقة استماع + قراءة/قواعد",
                "الأكاديميات: تعديل شرح درس واحد ثم اختبار التطبيق"
        };
        for (int i = 0; i < 3; i++) addTaskRow(topCard, i, defaults[i]);
        updatePriorityProgress();
    }

    private void addReserveCard(LinearLayout root) {
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, c, dp(12));
        TextView title = text("الوقت الاحتياطي", 18, NAVY, true);
        c.addView(title);
        TextView desc = text("إذا انتهى وقت مهمة ولم تكتمل، استخدم ١٥ دقيقة إضافية بدل أن يمتد اليوم بلا حدود.", 13, MUTED, false);
        desc.setPadding(0, dp(4), 0, dp(8));
        c.addView(desc);

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            TextView label = text("المهمة " + arabicNumber(i + 1), 14, TEXT, true);
            row.addView(label, new LinearLayout.LayoutParams(0, dp(44), 1f));
            int used = prefs.getInt("reserve_" + i + "_" + todayKey, 0);
            Button b = smallButton(used > 0 ? "مستخدم +" + arabicNumber(used) + " د" : "استخدام +١٥ د", used > 0 ? GREEN : NAVY);
            row.addView(b, new LinearLayout.LayoutParams(dp(150), dp(42)));
            b.setOnClickListener(v -> {
                int current = prefs.getInt("reserve_" + idx + "_" + todayKey, 0);
                int next = current == 0 ? 15 : 0;
                prefs.edit().putInt("reserve_" + idx + "_" + todayKey, next).apply();
                showTab("today");
            });
            c.addView(row);
        }
    }

    private void addIndicatorsCard(LinearLayout root) {
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, c, dp(12));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title = text("المؤشرات اليومية الخمسة", 18, NAVY, true);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        indicatorProgress = pill("أنجزت ٠ من ٥", GREEN);
        head.addView(indicatorProgress);
        c.addView(head);

        String[] labels = {"القرآن", "الإنجليزية / القبول", "العمل / الدخل", "الصحة أو التواصل", "إغلاق اليوم والنوم"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            CheckBox cb = new CheckBox(this);
            cb.setText(labels[i]);
            cb.setTextSize(15);
            cb.setTextColor(TEXT);
            cb.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            cb.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));
            cb.setChecked(prefs.getBoolean("indicator_" + i + "_" + todayKey, false));
            cb.setOnCheckedChangeListener((buttonView, checked) -> {
                prefs.edit().putBoolean("indicator_" + idx + "_" + todayKey, checked).apply();
                updateIndicatorProgress();
            });
            indicatorChecks[i] = cb;
            c.addView(cb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        }
        updateIndicatorProgress();
    }

    private List<String[]> scheduleForToday() {
        List<String[]> rows = new ArrayList<>();
        boolean flex = isFlexibleToday();
        int emergency = prefs.getInt("emergency_" + todayKey, 0);

        rows.add(r("من ٤:٠٠ ص إلى ٦:٣٠ ص", "الفجر • المسجد • التحفيظ • الدرس الشرعي", "ثابت", false));
        rows.add(r("من ٦:٣٠ ص إلى ٧:٠٠ ص", "تمرين بدني صباحي ٣٠ دقيقة", "صحة", true));
        rows.add(r("من ٧:٠٠ ص إلى ٨:٠٠ ص", "ساعة التركيز الذهبية: إنجليزي — ١٥ مفردة + ٢٠د استماع + ٢٠د قراءة أو قواعد + ٥د مراجعة", "إنجليزي", true));
        rows.add(r("من ٨:٠٠ ص إلى ٩:٣٠ ص", "نوم تكميلي", "نوم", false));
        rows.add(r("من ٩:٣٠ ص إلى ١٠:٠٠ ص", "الفطور + الاستماع إلى كتاب ليب أهيد رقم ١", "كتاب ١ من ٣", false));

        if (!flex) {
            rows.add(r("من ١٠:٠٠ ص إلى ١١:٣٠ ص", "مشروع الأكاديميات: تعديل شرح درس واحد ثم اختبار التطبيق", "عمل مركز", true));
        } else {
            rows.add(r("من ١٠:٠٠ ص إلى ١١:٣٠ ص", "وقت مرن لهذا الأسبوع — لا مشروع جديد اليوم", "اليوم المرن", false));
        }

        rows.add(r("من ١١:٤٥ ص إلى ١:٠٠ م", "المسجد: مراجعة صفحتين من القرآن + صلاة الظهر + العودة للبيت", "قرآن + ظهر", true));
        rows.add(r("من ١:٠٠ م إلى ١:٣٠ م", "الغداء", "وجبة", false));

        if (emergency >= 90) {
            rows.add(r("من ١:٣٠ م إلى ٢:١٥ م", "وقت للطرف الطارئ — جلسة المال والتسويق مؤجلة ولا تُحسب فشلًا", "طارئ", false));
        } else if (!flex) {
            rows.add(r("من ١:٣٠ م إلى ٢:١٥ م", "المال والتسويق: ٢٠د تعلم + ٢٥د تطبيق مباشر على بيع الأكاديميات", "دخل", false));
        } else {
            rows.add(r("من ١:٣٠ م إلى ٢:١٥ م", "راحة اليوم المرن", "مرن", false));
        }

        rows.add(r("من ٢:١٥ م إلى ٢:٤٠ م", "الاستماع إلى كتاب ليب أهيد رقم ٢", "كتاب ٢ من ٣", false));
        rows.add(r("من ٢:٤٥ م إلى ٤:١٠ م", "المسجد: مراجعة صفحتين من القرآن + صلاة العصر + الدرس", "قرآن + عصر", true));

        if (emergency >= 60) {
            rows.add(r("من ٤:٣٠ م إلى ٥:٣٠ م", "وقت الطارئ المحجوز — المهام المرنة تُرحّل ولا تزاحم المسجد", "طارئ", false));
        } else if (emergency == 30) {
            rows.add(r("من ٤:٣٠ م إلى ٥:٠٠ م", "وقت الطارئ المحجوز", "طارئ", false));
            rows.add(r("من ٥:٠٠ م إلى ٥:٣٠ م", "وقت الأسرة والمساعدة في البيت", "أسرة", false));
        } else {
            rows.add(r("من ٤:٣٠ م إلى ٥:٣٠ م", "وقت الأسرة والمساعدة في البيت", "أسرة", false));
        }

        rows.add(r("من ٦:٠٠ م إلى ٨:٠٠ م", "المغرب • التحفيظ • العشاء • محاسبة الطلاب", "ثابت", false));
        rows.add(r("من ٨:٠٠ م إلى ٨:٣٠ م", "العشاء مع الأسرة", "أسرة", false));
        rows.add(r("من ٨:٣٠ م إلى ٩:٠٠ م", "الاستماع إلى كتاب ليب أهيد رقم ٣", "كتاب ٣ من ٣", false));
        rows.add(r("من ٩:٠٠ م إلى ١٠:٠٠ م", flex ? "وقت حر موسع لليوم المرن" : "وقت حر بلا شعور بالذنب", "راحة", false));
        rows.add(r("من ١٠:٠٠ م إلى ١٠:١٥ م", "تحديد أهم ٣ مهام للغد ومراجعة قائمة «لاحقًا» فقط دون بدء شيء جديد", "إغلاق اليوم", false));
        rows.add(r("من ١٠:١٥ م إلى ١٠:٣٠ م", "إبعاد الهاتف والاستعداد للنوم", "نوم", true));
        rows.add(r("١٠:٣٠ م", "النوم", "الهدف الليلي", true));
        return rows;
    }

    private String[] r(String time, String label, String tag, boolean highlight) {
        return new String[]{time, label, tag, highlight ? "1" : "0"};
    }

    private void showEmergencyDialog() {
        String[] choices = {"٣٠ دقيقة", "٦٠ دقيقة", "٩٠ دقيقة", "إلغاء وضع الطارئ"};
        new AlertDialog.Builder(this)
                .setTitle("كم أخذ الطارئ من وقتك؟")
                .setItems(choices, (dialog, which) -> {
                    int value = which == 0 ? 30 : which == 1 ? 60 : which == 2 ? 90 : 0;
                    prefs.edit().putInt("emergency_" + todayKey, value).apply();
                    showTab("today");
                })
                .setNegativeButton("رجوع", null)
                .show();
    }

    private boolean isFlexibleToday() {
        return todayKey.equals(prefs.getString("flex_day_" + weekKey(), ""));
    }

    private void toggleFlexibleToday() {
        String key = "flex_day_" + weekKey();
        if (isFlexibleToday()) prefs.edit().remove(key).apply();
        else prefs.edit().putString(key, todayKey).apply();
        showTab("today");
    }

    private String weekKey() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.WEEK_OF_YEAR);
    }

    private void showQuickIdeaDialog() {
        EditText input = dialogInput("اكتب الفكرة فقط…");
        new AlertDialog.Builder(this)
                .setTitle("خطرت لي فكرة")
                .setMessage("احفظها هنا وارجع فورًا لما كنت تفعله.")
                .setView(input)
                .setPositiveButton("حفظ في لاحقًا", (d, w) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        saveLaterItem(value, "بحث", "", "عادية", "", "", "لاحقًا");
                        Toast.makeText(this, "حُفظت الفكرة في لاحقًا", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private View buildLaterScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "لاحقًا • لا تغيّر خطتك بسبب فكرة جديدة");

        Button add = smallButton("＋ إضافة شيء بالتفصيل", GREEN);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        addLp.setMargins(0, dp(16), 0, dp(8));
        root.addView(add, addLp);
        add.setOnClickListener(v -> showFullLaterDialog());

        JSONArray items = getLaterItems();
        if (items.length() == 0) {
            LinearLayout empty = card();
            empty.setPadding(dp(16), dp(18), dp(16), dp(18));
            addCard(root, empty, dp(8));
            empty.addView(text("القائمة فارغة. أي فكرة تقطع عليك القرآن أو العمل احفظها هنا بدل البحث عنها فورًا.", 14, TEXT, false));
            return scroll;
        }

        for (int i = items.length() - 1; i >= 0; i--) {
            JSONObject o = items.optJSONObject(i);
            if (o != null) addLaterCard(root, o, i);
        }
        return scroll;
    }

    private void addLaterCard(LinearLayout root, JSONObject o, int index) {
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, c, dp(8));
        c.addView(text(o.optString("title"), 17, NAVY, true));
        String info = o.optString("category", "بحث") + " • " + o.optString("importance", "عادية") + " • " + o.optString("status", "لاحقًا");
        TextView meta = text(info, 12, GREEN, true);
        meta.setPadding(0, dp(4), 0, 0);
        c.addView(meta);
        if (!o.optString("why").isEmpty()) c.addView(detail("لماذا؟ " + o.optString("why")));
        if (!o.optString("duration").isEmpty()) c.addView(detail("المدة المتوقعة: " + o.optString("duration")));
        if (!o.optString("review").isEmpty()) c.addView(detail("المراجعة/الموعد: " + o.optString("review")));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        actions.setPadding(0, dp(8), 0, 0);
        Button month = smallButton("هذا الشهر", GREEN);
        Button later = smallButton("لاحقًا", NAVY);
        Button delete = smallButton("حذف", Color.rgb(145, 55, 55));
        actions.addView(month, buttonWeight());
        actions.addView(later, buttonWeight());
        actions.addView(delete, buttonWeight());
        c.addView(actions);
        month.setOnClickListener(v -> setLaterStatus(index, "هذا الشهر"));
        later.setOnClickListener(v -> setLaterStatus(index, "لاحقًا"));
        delete.setOnClickListener(v -> deleteLaterItem(index));
    }

    private TextView detail(String value) {
        TextView t = text(value, 13, TEXT, false);
        t.setPadding(0, dp(4), 0, 0);
        return t;
    }

    private void showFullLaterDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), 0, dp(18), 0);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        EditText title = dialogInput("العنوان");
        EditText why = dialogInput("لماذا أريده؟");
        EditText duration = dialogInput("المدة المتوقعة، مثل: ساعة أو شهر");
        EditText review = dialogInput("متى أراجعه؟ أو موعده إن وجد");
        box.addView(title); box.addView(why); box.addView(duration); box.addView(review);

        String[] cats = {"علم", "تعلم", "مشروع", "تجربة", "مهارة", "بحث"};
        Spinner cat = spinner(cats);
        String[] levels = {"عالية", "عادية", "منخفضة"};
        Spinner importance = spinner(levels);
        box.addView(labeled("التصنيف", cat));
        box.addView(labeled("الأهمية", importance));

        new AlertDialog.Builder(this)
                .setTitle("إضافة إلى لاحقًا")
                .setView(box)
                .setPositiveButton("حفظ", (d, w) -> {
                    String value = title.getText().toString().trim();
                    if (!value.isEmpty()) {
                        saveLaterItem(value, cat.getSelectedItem().toString(), why.getText().toString().trim(), importance.getSelectedItem().toString(), duration.getText().toString().trim(), review.getText().toString().trim(), "لاحقًا");
                        showTab("later");
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private View labeled(String label, View field) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, dp(8), 0, 0);
        wrap.addView(text(label, 12, MUTED, true));
        wrap.addView(field, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return wrap;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        s.setAdapter(adapter);
        return s;
    }

    private EditText dialogInput(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setGravity(Gravity.RIGHT);
        e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        e.setSingleLine(false);
        e.setMaxLines(3);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return e;
    }

    private JSONArray getLaterItems() {
        try { return new JSONArray(prefs.getString("later_items", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void saveLaterItem(String title, String category, String why, String importance, String duration, String review, String status) {
        try {
            JSONArray arr = getLaterItems();
            JSONObject o = new JSONObject();
            o.put("title", title);
            o.put("category", category);
            o.put("why", why);
            o.put("importance", importance);
            o.put("duration", duration);
            o.put("review", review);
            o.put("status", status);
            o.put("created", todayKey);
            arr.put(o);
            prefs.edit().putString("later_items", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void setLaterStatus(int index, String status) {
        try {
            JSONArray arr = getLaterItems();
            JSONObject o = arr.getJSONObject(index);
            o.put("status", status);
            prefs.edit().putString("later_items", arr.toString()).apply();
            showTab("later");
        } catch (Exception ignored) {}
    }

    private void deleteLaterItem(int index) {
        JSONArray arr = getLaterItems();
        if (index >= 0 && index < arr.length()) arr.remove(index);
        prefs.edit().putString("later_items", arr.toString()).apply();
        showTab("later");
    }

    private View buildPlanScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "الخطة • ما الذي نحميه الآن؟");
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, c, dp(16));
        addBullet(c, "الطب هو الهدف الجامعي الأول، والمختبرات البديل.");
        addBullet(c, "القرآن: ٤ صفحات يوميًا في المسجد.");
        addBullet(c, "الإنجليزية: تطوير المفردات والاستماع والقبول.");
        addBullet(c, "الدخل: مشروع الأكاديميات + تعلم البيع بالتطبيق.");
        addBullet(c, "الصحة والتواصل: جرعات صغيرة مستمرة.");
        LinearLayout rule = card();
        rule.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, rule, dp(12));
        rule.addView(text("قاعدة هذه المرحلة", 17, GREEN, true));
        rule.addView(detail("الفكرة الجديدة تُحفظ في «لاحقًا» ولا تغير جدول اليوم."));
        return scroll;
    }

    private View buildProgressScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "التقدم • أرقام اليوم");
        addMetricCard(root, "أهم مهام اليوم", "أنجزت " + arabicNumber(countDoneTasks()) + " من ٣", NAVY);
        addMetricCard(root, "المؤشرات اليومية", "أنجزت " + arabicNumber(countDoneIndicators()) + " من ٥", GREEN);
        int reserve = prefs.getInt("reserve_0_" + todayKey, 0) + prefs.getInt("reserve_1_" + todayKey, 0) + prefs.getInt("reserve_2_" + todayKey, 0);
        addMetricCard(root, "الوقت الاحتياطي المستخدم", arabicNumber(reserve) + " دقيقة", GOLD);
        int emergency = prefs.getInt("emergency_" + todayKey, 0);
        addMetricCard(root, "وقت الطوارئ اليوم", arabicNumber(emergency) + " دقيقة", emergency > 0 ? GOLD : MUTED);
        return scroll;
    }

    private View buildMoreScreen() {
        ScrollView scroll = baseScroll();
        LinearLayout root = baseRoot(scroll);
        addHeader(root, "المزيد");
        addMetricCard(root, "الإصدار", "0.3.0", NAVY);
        addMetricCard(root, "النقاط الرئيسية المنفذة", "١ إلى ١٠", GREEN);
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, c, dp(12));
        addBullet(c, "يعمل دون إنترنت.");
        addBullet(c, "البيانات محفوظة محليًا.");
        addBullet(c, "اليوم المرن: " + (isFlexibleToday() ? "اليوم" : "غير مستخدم اليوم"));
        addBullet(c, "وضع الطارئ: " + arabicNumber(prefs.getInt("emergency_" + todayKey, 0)) + " دقيقة");
        return scroll;
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(dp(4), dp(4), dp(4), dp(4));
        nav.setElevation(dp(8));
        nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addNavItem(nav, "اليوم", "today");
        addNavItem(nav, "الخطة", "plan");
        addNavItem(nav, "التقدم", "progress");
        addNavItem(nav, "لاحقًا", "later");
        addNavItem(nav, "المزيد", "more");
        return nav;
    }

    private void addNavItem(LinearLayout nav, String label, String key) {
        TextView item = text(label, 12, activeTab.equals(key) ? GREEN : MUTED, activeTab.equals(key));
        item.setGravity(Gravity.CENTER);
        if (activeTab.equals(key)) item.setBackground(rounded(SOFT_GREEN, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(2), dp(4), dp(2), dp(4));
        nav.addView(item, lp);
        item.setOnClickListener(v -> showTab(key));
    }

    private void addHeader(LinearLayout root, String subtitleValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        row.addView(block, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        block.addView(text("مساري", 30, NAVY, true));
        block.addView(text(subtitleValue, 14, MUTED, false));
        TextView badge = text("0.3.0", 12, Color.WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(6), dp(10), dp(6));
        badge.setBackground(rounded(NAVY, 30));
        row.addView(badge);
        root.addView(row);
        TextView date = text(arabicDate(), 16, TEXT, true);
        date.setPadding(0, dp(10), 0, 0);
        root.addView(date);
    }

    private void addTaskRow(LinearLayout parent, int index, String defaultText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0, dp(9), 0, 0);
        CheckBox check = new CheckBox(this);
        check.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));
        check.setChecked(prefs.getBoolean("task_" + index + "_done_" + todayKey, false));
        taskChecks[index] = check;
        row.addView(check, new LinearLayout.LayoutParams(dp(44), dp(62)));
        EditText input = new EditText(this);
        input.setText(prefs.getString("task_" + index + "_text_" + todayKey, defaultText));
        input.setTextSize(14);
        input.setTextColor(TEXT);
        input.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        input.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        input.setSingleLine(false);
        input.setMaxLines(3);
        input.setPadding(dp(12), dp(7), dp(12), dp(7));
        input.setBackground(rounded(Color.rgb(247, 249, 252), 14, Color.rgb(226, 231, 239)));
        taskInputs[index] = input;
        row.addView(input, new LinearLayout.LayoutParams(0, dp(64), 1f));
        check.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("task_" + index + "_done_" + todayKey, checked).apply();
            updatePriorityProgress();
        });
        parent.addView(row);
    }

    private void addTimelineRow(LinearLayout parent, String time, String label, String tag, boolean highlight) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        c.setPadding(dp(14), dp(11), dp(14), dp(11));
        c.setBackground(rounded(highlight ? SOFT_GREEN : Color.WHITE, 16, highlight ? Color.rgb(179, 221, 209) : Color.rgb(227, 232, 240)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, 0);
        parent.addView(c, lp);
        TextView t = text(time, 13, highlight ? GREEN : NAVY, true);
        t.setPadding(0, 0, 0, dp(4));
        c.addView(t);
        c.addView(text(label, 15, TEXT, true));
        TextView tagView = text(tag, 11, highlight ? GREEN : MUTED, false);
        tagView.setPadding(0, dp(3), 0, 0);
        c.addView(tagView);
    }

    private void addMetricCard(LinearLayout root, String titleValue, String value, int color) {
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        addCard(root, c, dp(12));
        c.addView(text(titleValue, 13, MUTED, true));
        TextView v = text(value, 21, color, true);
        v.setPadding(0, dp(5), 0, 0);
        c.addView(v);
    }

    private void addBullet(LinearLayout parent, String value) {
        TextView t = text("•  " + value, 14, TEXT, false);
        t.setPadding(0, dp(5), 0, dp(5));
        parent.addView(t);
    }

    private void saveTasks(boolean showToast) {
        if (taskInputs[0] == null) return;
        SharedPreferences.Editor e = prefs.edit();
        for (int i = 0; i < 3; i++) {
            if (taskInputs[i] != null) e.putString("task_" + i + "_text_" + todayKey, taskInputs[i].getText().toString().trim());
            if (taskChecks[i] != null) e.putBoolean("task_" + i + "_done_" + todayKey, taskChecks[i].isChecked());
        }
        e.apply();
        if (showToast) Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show();
    }

    private int countDoneTasks() {
        int n = 0;
        for (int i = 0; i < 3; i++) if (prefs.getBoolean("task_" + i + "_done_" + todayKey, false)) n++;
        return n;
    }

    private int countDoneIndicators() {
        int n = 0;
        for (int i = 0; i < 5; i++) if (prefs.getBoolean("indicator_" + i + "_" + todayKey, false)) n++;
        return n;
    }

    private void updatePriorityProgress() {
        if (priorityProgress != null) priorityProgress.setText("أنجزت " + arabicNumber(countCurrentChecks(taskChecks)) + " من ٣");
    }

    private void updateIndicatorProgress() {
        if (indicatorProgress != null) indicatorProgress.setText("أنجزت " + arabicNumber(countCurrentChecks(indicatorChecks)) + " من ٥");
    }

    private int countCurrentChecks(CheckBox[] list) {
        int n = 0;
        for (CheckBox c : list) if (c != null && c.isChecked()) n++;
        return n;
    }

    private ScrollView baseScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return scroll;
    }

    private LinearLayout baseRoot(ScrollView scroll) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(32));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        c.setBackground(rounded(Color.WHITE, 20, Color.rgb(228, 233, 241)));
        c.setElevation(dp(1));
        return c;
    }

    private void addCard(LinearLayout root, View card, int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, topMargin, 0, 0);
        root.addView(card, lp);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView pill(String value, int color) {
        TextView t = text(value, 12, color, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(9), dp(5), dp(9), dp(5));
        t.setBackground(rounded(SOFT_GREEN, 24));
        return t;
    }

    private Button smallButton(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(color, 13));
        return b;
    }

    private LinearLayout.LayoutParams buttonWeight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        return lp;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor) {
        GradientDrawable d = rounded(color, radiusDp);
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    private String arabicDate() {
        return new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar")).format(Calendar.getInstance().getTime());
    }

    private String arabicNumber(int n) {
        return String.valueOf(n)
                .replace('0', '٠').replace('1', '١').replace('2', '٢').replace('3', '٣').replace('4', '٤')
                .replace('5', '٥').replace('6', '٦').replace('7', '٧').replace('8', '٨').replace('9', '٩');
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveTasks(false);
    }
}
