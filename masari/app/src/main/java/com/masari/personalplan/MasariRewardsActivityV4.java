package com.masari.personalplan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MasariRewardsActivityV4 extends MasariRewardsActivityV3 {
    private static final String WEEK_TAG = "masari_week_center_button";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermissionIfNeeded();
        ReminderScheduler.scheduleNextSevenDays(this);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        addWeekButton();
    }

    private void addWeekButton() {
        Button b = new Button(this);
        b.setTag(WEEK_TAG);
        b.setAllCaps(false);
        b.setText("الأسبوع");
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(8, 0, 8, 0);
        b.setBackground(round(Color.rgb(22, 123, 98), 999));
        b.setElevation(dpLocal(8));
        b.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlannerActivity.class)));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dpLocal(82), dpLocal(50));
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = dpLocal(78);
        addContentView(b, lp);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 804);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 804) ReminderScheduler.scheduleNextSevenDays(this);
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dpLocal(radiusDp));
        return d;
    }

    private int dpLocal(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
