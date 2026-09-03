package com.masari.personalplan;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MasariRewardsActivityV3 extends MasariRewardsActivityV2 {
    private static final String PLANNER_TAG = "masari_planner_center_button";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        addPlannerButton();
    }

    private void addPlannerButton() {
        Button b = new Button(this);
        b.setTag(PLANNER_TAG);
        b.setAllCaps(false);
        b.setText("متابعة");
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(8,0,8,0);
        b.setBackground(round(Color.rgb(24,49,83), 999));
        b.setElevation(dpLocal(8));
        b.setOnClickListener(v -> startActivity(new Intent(this, PlannerCenterActivity.class)));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dpLocal(78), dpLocal(50));
        lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        lp.rightMargin = dpLocal(12);
        lp.bottomMargin = dpLocal(78);
        addContentView(b, lp);
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
