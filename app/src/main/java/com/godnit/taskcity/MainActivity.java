package com.godnit.taskcity;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(17, 35, 63);
    private static final int TEAL = Color.rgb(21, 154, 140);
    private static final int ORANGE = Color.rgb(224, 112, 48);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TaskStore store;
    private CityGLSurfaceView cityView;
    private TextView cityLabel;
    private TextView emptyLabel;
    private Button normalTab;
    private Button urgentTab;
    private ListView taskList;
    private TaskAdapter adapter;
    private int selectedType = TaskItem.NORMAL;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            int oldCount = store.getHouseCount(TaskItem.URGENT);
            int expired = store.expireUrgentTasks(System.currentTimeMillis());
            if (expired > 0) {
                if (selectedType == TaskItem.URGENT) {
                    cityView.getCityRenderer().houseDemolished(oldCount,
                            store.getHouseCount(TaskItem.URGENT));
                }
                Toast.makeText(MainActivity.this,
                        expired == 1 ? "انتهى الوقت وهُدم بيت من مدينة الضروريات"
                                : "انتهى وقت " + expired + " مهام وهُدمت البيوت المستحقة",
                        Toast.LENGTH_LONG).show();
                refresh();
            } else if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        store = new TaskStore(this);
        store.expireUrgentTasks(System.currentTimeMillis());
        setContentView(buildInterface());
        selectCity(TaskItem.NORMAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cityView != null) cityView.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        if (cityView != null) cityView.onPause();
        super.onPause();
    }

    private View buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Color.rgb(245, 241, 232));

        LinearLayout titleArea = new LinearLayout(this);
        titleArea.setOrientation(LinearLayout.VERTICAL);
        titleArea.setPadding(dp(18), dp(12), dp(18), dp(10));
        titleArea.setBackgroundColor(NAVY);
        TextView title = text("مدينة المهام", 25, Color.WHITE, true);
        TextView subtitle = text("أنجز مهامك، وشاهد مدينتك تكبر بيتًا بعد بيت", 13,
                Color.rgb(205, 222, 238), false);
        titleArea.addView(title);
        titleArea.addView(subtitle);
        root.addView(titleArea);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(10), dp(8), dp(10), dp(8));
        normalTab = tabButton("المهام العادية");
        urgentTab = tabButton("المهام الضرورية");
        tabs.addView(normalTab, new LinearLayout.LayoutParams(0, dp(48), 1));
        tabs.addView(urgentTab, new LinearLayout.LayoutParams(0, dp(48), 1));
        normalTab.setOnClickListener(v -> selectCity(TaskItem.NORMAL));
        urgentTab.setOnClickListener(v -> selectCity(TaskItem.URGENT));
        root.addView(tabs);

        FrameLayout cityFrame = new FrameLayout(this);
        cityView = new CityGLSurfaceView(this);
        cityFrame.addView(cityView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cityLabel = text("", 14, Color.WHITE, true);
        cityLabel.setGravity(Gravity.CENTER);
        cityLabel.setPadding(dp(12), dp(7), dp(12), dp(7));
        cityLabel.setBackgroundColor(Color.argb(205, 17, 35, 63));
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.RIGHT);
        labelParams.setMargins(dp(12), dp(12), dp(12), 0);
        cityFrame.addView(cityLabel, labelParams);
        TextView hint = text("حرّك بإصبعك • كبّر وصغّر بإصبعين", 11, Color.WHITE, false);
        hint.setPadding(dp(9), dp(5), dp(9), dp(5));
        hint.setBackgroundColor(Color.argb(150, 0, 0, 0));
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hintParams.bottomMargin = dp(8);
        cityFrame.addView(hint, hintParams);
        root.addView(cityFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 62));

        LinearLayout tasksHeader = new LinearLayout(this);
        tasksHeader.setGravity(Gravity.CENTER_VERTICAL);
        tasksHeader.setPadding(dp(14), dp(8), dp(14), dp(7));
        TextView tasksTitle = text("المهام الحالية", 18, NAVY, true);
        Button addButton = new Button(this);
        addButton.setText("＋ إضافة مهمة");
        addButton.setTextColor(Color.WHITE);
        addButton.setTextSize(14);
        addButton.setAllCaps(false);
        addButton.setBackgroundColor(TEAL);
        addButton.setOnClickListener(v -> showAddDialog());
        tasksHeader.addView(tasksTitle, new LinearLayout.LayoutParams(0, dp(46), 1));
        tasksHeader.addView(addButton, new LinearLayout.LayoutParams(dp(145), dp(46)));
        root.addView(tasksHeader);

        FrameLayout listFrame = new FrameLayout(this);
        taskList = new ListView(this);
        taskList.setDividerHeight(dp(6));
        taskList.setDivider(null);
        taskList.setPadding(dp(10), 0, dp(10), dp(10));
        taskList.setClipToPadding(false);
        listFrame.addView(taskList);
        emptyLabel = text("", 15, Color.rgb(95, 104, 112), false);
        emptyLabel.setGravity(Gravity.CENTER);
        emptyLabel.setPadding(dp(30), dp(18), dp(30), dp(18));
        listFrame.addView(emptyLabel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(listFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 38));
        return root;
    }

    private void selectCity(int type) {
        selectedType = type;
        normalTab.setBackgroundColor(type == TaskItem.NORMAL ? TEAL : Color.rgb(221, 225, 229));
        normalTab.setTextColor(type == TaskItem.NORMAL ? Color.WHITE : NAVY);
        urgentTab.setBackgroundColor(type == TaskItem.URGENT ? ORANGE : Color.rgb(221, 225, 229));
        urgentTab.setTextColor(type == TaskItem.URGENT ? Color.WHITE : NAVY);
        cityView.getCityRenderer().setCity(type, store.getHouseCount(type));
        refresh();
    }

    private void refresh() {
        List<TaskItem> tasks = store.tasksOfType(selectedType);
        adapter = new TaskAdapter(tasks);
        taskList.setAdapter(adapter);
        int houses = store.getHouseCount(selectedType);
        cityLabel.setText((selectedType == TaskItem.NORMAL ? "المدينة الهادئة" : "مدينة التحدي")
                + "  •  " + arabicNumber(houses) + " بيت");
        emptyLabel.setText(selectedType == TaskItem.NORMAL
                ? "لا توجد مهام عادية الآن\nأضف أول مهمة، وعند إنجازها سيُبنى أول بيت"
                : "لا توجد مهام ضرورية الآن\nأضف مهمة وحدد وقتها من دقيقة إلى ٦ ساعات");
        emptyLabel.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setPadding(dp(22), dp(8), dp(22), 0);
        EditText input = new EditText(this);
        input.setHint(selectedType == TaskItem.NORMAL
                ? "مثال: أصنع ثلاثة مواقع جديدة"
                : "مثال: أنهي القالب الجديد");
        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setGravity(Gravity.RIGHT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(false);
        content.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(82)));

        final SeekBar duration = new SeekBar(this);
        final TextView durationLabel = text("المدة: ساعة واحدة", 15, NAVY, true);
        if (selectedType == TaskItem.URGENT) {
            TextView help = text("اختر المدة (من دقيقة إلى ٦ ساعات)", 13,
                    Color.rgb(85, 92, 100), false);
            content.addView(help);
            content.addView(durationLabel);
            duration.setMax(359);
            duration.setProgress(59);
            duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    durationLabel.setText("المدة: " + formatDuration(progress + 1));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            });
            content.addView(duration);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(selectedType == TaskItem.NORMAL ? "مهمة عادية جديدة" : "مهمة ضرورية جديدة")
                .setView(content)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إضافة", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        input.setError("اكتب المهمة أولًا");
                        return;
                    }
                    long dueAt = selectedType == TaskItem.URGENT
                            ? System.currentTimeMillis() + (duration.getProgress() + 1L) * 60_000L
                            : 0L;
                    store.add(title, selectedType, dueAt);
                    refresh();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void completeTask(TaskItem task) {
        new AlertDialog.Builder(this)
                .setTitle("هل أنجزت المهمة؟")
                .setMessage(task.title + "\n\nسيُبنى بيت جديد في مدينتك.")
                .setNegativeButton("ليس بعد", null)
                .setPositiveButton("نعم، أنجزتها", (dialog, which) -> {
                    TaskItem completed = store.complete(task.id);
                    if (completed != null) {
                        cityView.getCityRenderer().houseBuilt(store.getHouseCount(selectedType));
                        Toast.makeText(this, "أحسنت! بدأ بناء بيت جديد 🏠",
                                Toast.LENGTH_SHORT).show();
                        refresh();
                    }
                }).show();
    }

    private final class TaskAdapter extends BaseAdapter {
        private final List<TaskItem> items;

        TaskAdapter(List<TaskItem> items) {
            this.items = items;
        }

        @Override public int getCount() { return items.size(); }
        @Override public TaskItem getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return getItem(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TaskItem task = getItem(position);
            LinearLayout card = new LinearLayout(MainActivity.this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(14), dp(8), dp(10), dp(8));
            card.setBackgroundColor(Color.WHITE);
            card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

            LinearLayout words = new LinearLayout(MainActivity.this);
            words.setOrientation(LinearLayout.VERTICAL);
            TextView taskTitle = text(task.title, 16, NAVY, true);
            taskTitle.setMaxLines(2);
            words.addView(taskTitle);
            TextView detail;
            if (task.type == TaskItem.URGENT) {
                long remaining = Math.max(0L, task.dueAt - System.currentTimeMillis());
                detail = text("الوقت المتبقي: " + formatRemaining(remaining), 13, ORANGE, true);
            } else {
                detail = text("بدون وقت محدد • أنجزها متى استطعت", 12,
                        Color.rgb(100, 112, 120), false);
            }
            words.addView(detail);
            Button done = new Button(MainActivity.this);
            done.setText("✓ تم");
            done.setTextColor(Color.WHITE);
            done.setTextSize(14);
            done.setAllCaps(false);
            done.setBackgroundColor(task.type == TaskItem.NORMAL ? TEAL : ORANGE);
            done.setOnClickListener(v -> completeTask(task));
            card.addView(words, new LinearLayout.LayoutParams(0, dp(68), 1));
            card.addView(done, new LinearLayout.LayoutParams(dp(76), dp(50)));
            return card;
        }
    }

    private Button tabButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes < 60) return arabicNumber(totalMinutes) + " دقيقة";
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (minutes == 0) return arabicNumber(hours) + (hours == 1 ? " ساعة" : " ساعات");
        return arabicNumber(hours) + " س و" + arabicNumber(minutes) + " د";
    }

    private String formatRemaining(long millis) {
        long totalSeconds = (millis + 999L) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return arabicNumber(hours) + ":" + twoDigits(minutes) + ":" + twoDigits(seconds);
    }

    private String twoDigits(long value) {
        return arabicNumber(String.format(Locale.US, "%02d", value));
    }

    private String arabicNumber(long value) {
        return arabicNumber(Long.toString(value));
    }

    private String arabicNumber(String value) {
        char[] western = "0123456789".toCharArray();
        char[] eastern = "٠١٢٣٤٥٦٧٨٩".toCharArray();
        for (int i = 0; i < western.length; i++) value = value.replace(western[i], eastern[i]);
        return value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
