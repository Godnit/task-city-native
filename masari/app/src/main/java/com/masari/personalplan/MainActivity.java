package com.masari.personalplan;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(20, 108, 91);
    private static final int NAVY = Color.rgb(25, 54, 93);
    private static final int TEXT = Color.rgb(30, 40, 55);
    private static final int MUTED = Color.rgb(101, 112, 128);
    private static final int BG = Color.rgb(246, 248, 252);

    private SharedPreferences prefs;
    private String todayKey;
    private final EditText[] taskInputs = new EditText[3];
    private final CheckBox[] taskChecks = new CheckBox[3];
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        prefs = getSharedPreferences("masari_data", Context.MODE_PRIVATE);
        todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(32));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleBlockLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleRow.addView(titleBlock, titleBlockLp);

        TextView title = text("مساري", 30, NAVY, true);
        titleBlock.addView(title);
        TextView subtitle = text("خطة اليوم • مرحلة التأسيس", 14, MUTED, false);
        subtitle.setPadding(0, dp(2), 0, 0);
        titleBlock.addView(subtitle);

        TextView badge = text("0.1.0", 12, Color.WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(6), dp(10), dp(6));
        badge.setBackground(rounded(NAVY, 30));
        titleRow.addView(badge);
        root.addView(titleRow);

        TextView date = text(arabicDate(), 16, TEXT, true);
        date.setPadding(0, dp(12), 0, 0);
        root.addView(date);

        LinearLayout focusCard = card();
        focusCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp(16), 0, 0);
        root.addView(focusCard, cardLp);

        TextView phase = text("المرحلة الحالية", 12, MUTED, true);
        focusCard.addView(phase);
        TextView phaseName = text("التأسيس — أثبّت النظام قبل أن أزيد الأهداف", 17, GREEN, true);
        phaseName.setPadding(0, dp(5), 0, 0);
        focusCard.addView(phaseName);

        LinearLayout topCard = card();
        topCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(0, dp(14), 0, 0);
        root.addView(topCard, topLp);

        LinearLayout sectionHead = new LinearLayout(this);
        sectionHead.setOrientation(LinearLayout.HORIZONTAL);
        sectionHead.setGravity(Gravity.CENTER_VERTICAL);
        sectionHead.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView topTitle = text("أهم 3 أشياء اليوم", 20, NAVY, true);
        sectionHead.addView(topTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        progressText = text("0 / 3", 14, GREEN, true);
        progressText.setGravity(Gravity.CENTER);
        progressText.setPadding(dp(10), dp(6), dp(10), dp(6));
        progressText.setBackground(rounded(Color.rgb(232, 246, 242), 30));
        sectionHead.addView(progressText);
        topCard.addView(sectionHead);

        String[] defaults = {
                "مراجعة 4 صفحات من القرآن",
                "30 دقيقة إنجليزي",
                "60 دقيقة عمل / مشروع"
        };
        for (int i = 0; i < 3; i++) {
            addTaskRow(topCard, i, defaults[i]);
        }

        Button save = new Button(this);
        save.setText("حفظ مهام اليوم");
        save.setTextSize(15);
        save.setTextColor(Color.WHITE);
        save.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        save.setAllCaps(false);
        save.setBackground(rounded(GREEN, 16));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        saveLp.setMargins(0, dp(12), 0, 0);
        topCard.addView(save, saveLp);
        save.setOnClickListener(v -> saveTasks(true));
        updateProgress();

        TextView timelineTitle = text("جدول اليوم", 22, NAVY, true);
        timelineTitle.setPadding(0, dp(24), 0, dp(4));
        root.addView(timelineTitle);
        TextView timelineSub = text("هيكل مرن — المواعيد الثابتة تبقى، والجلسات تتبدل حسب الأولوية", 13, MUTED, false);
        timelineSub.setPadding(0, 0, 0, dp(10));
        root.addView(timelineSub);

        String[][] times = {
                {"04:00 – 06:30", "الفجر • المسجد • التحفيظ • الدرس", "ثابت"},
                {"07:00 – 08:00", "ساعة التركيز الذهبية", "أهم مهمة"},
                {"08:00 – 09:30", "نوم تكميلي", "طاقة"},
                {"10:00 – 11:30", "جلسة تركيز ثانية", "تركيز"},
                {"11:45 – 13:00", "الظهر • الغداء", "ثابت"},
                {"13:30 – 14:30", "جلسة مرنة: عمل / مشروع / إنجليزي", "مرن"},
                {"قبل العصر", "مراجعة القرآن في المسجد", "قرآن"},
                {"16:30 – 17:30", "أسرة / رياضة / عمل خفيف", "مرن"},
                {"18:00 – 20:00", "المغرب • العشاء • التحفيظ", "ثابت"},
                {"20:30 – 22:30", "جلسة مسائية خفيفة + وقت راحة", "مساء"},
                {"22:30", "إغلاق اليوم والاستعداد للنوم", "نوم"}
        };

        for (int i = 0; i < times.length; i++) {
            addTimelineRow(root, times[i][0], times[i][1], times[i][2], i == 1 || i == 6);
        }

        TextView footer = text("النسخة الأولى: اليوم + أهم 3 + جدول اليوم\nالبيانات محفوظة على هاتفك تلقائيًا.", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, dp(4));
        root.addView(footer);

        return scroll;
    }

    private void addTaskRow(LinearLayout parent, int index, String defaultText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0, dp(10), 0, 0);

        CheckBox check = new CheckBox(this);
        check.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));
        boolean checked = prefs.getBoolean("task_" + index + "_done_" + todayKey, false);
        check.setChecked(checked);
        taskChecks[index] = check;
        row.addView(check, new LinearLayout.LayoutParams(dp(44), dp(50)));

        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(2);
        input.setTextSize(15);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(rounded(Color.rgb(247, 249, 252), 14, Color.rgb(226, 231, 239)));
        input.setText(prefs.getString("task_" + index + "_text_" + todayKey, defaultText));
        input.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        taskInputs[index] = input;
        row.addView(input, new LinearLayout.LayoutParams(0, dp(54), 1f));

        check.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("task_" + index + "_done_" + todayKey, isChecked).apply();
            updateProgress();
        });

        parent.addView(row);
    }

    private void addTimelineRow(LinearLayout parent, String time, String label, String tag, boolean highlight) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(rounded(highlight ? Color.rgb(235, 247, 243) : Color.WHITE, 16,
                highlight ? Color.rgb(179, 221, 209) : Color.rgb(227, 232, 240)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, 0);
        parent.addView(card, lp);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView labelView = text(label, 15, TEXT, true);
        textCol.addView(labelView);
        TextView tagView = text(tag, 11, highlight ? GREEN : MUTED, false);
        tagView.setPadding(0, dp(3), 0, 0);
        textCol.addView(tagView);

        TextView timeView = text(time, 13, highlight ? GREEN : NAVY, true);
        timeView.setGravity(Gravity.CENTER);
        timeView.setPadding(dp(10), dp(7), dp(10), dp(7));
        timeView.setBackground(rounded(highlight ? Color.WHITE : Color.rgb(246, 248, 252), 20));
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timeLp.setMargins(dp(10), 0, 0, 0);
        card.addView(timeView, timeLp);
    }

    private void saveTasks(boolean toast) {
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < 3; i++) {
            editor.putString("task_" + i + "_text_" + todayKey, taskInputs[i].getText().toString().trim());
            editor.putBoolean("task_" + i + "_done_" + todayKey, taskChecks[i].isChecked());
        }
        editor.apply();
        if (toast) Toast.makeText(this, "تم حفظ مهام اليوم", Toast.LENGTH_SHORT).show();
    }

    private void updateProgress() {
        if (progressText == null) return;
        int done = 0;
        for (CheckBox check : taskChecks) {
            if (check != null && check.isChecked()) done++;
        }
        progressText.setText(done + " / 3");
    }

    private String arabicDate() {
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar"));
        return fmt.format(Calendar.getInstance().getTime());
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setBackground(rounded(Color.WHITE, 20, Color.rgb(228, 233, 241)));
        card.setElevation(dp(1));
        return card;
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (taskInputs[0] != null) saveTasks(false);
    }
}
