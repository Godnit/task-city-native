package com.masari.personalplan;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MasariRewardsActivityV2 extends MasariRewardsActivity {
    private static final String STAR_TAG = "masari_reward_center_button";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        addRewardCenterButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRewardCenterButton();
    }

    private void addRewardCenterButton() {
        Button b = new Button(this);
        b.setTag(STAR_TAG);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(10, 0, 10, 0);
        b.setBackground(round(Color.rgb(184,126,28), 999));
        b.setElevation(dpLocal(8));
        b.setOnClickListener(v -> startActivity(new Intent(this, RewardCenterActivity.class)));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dpLocal(76), dpLocal(50));
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        lp.leftMargin = dpLocal(12);
        lp.bottomMargin = dpLocal(78);
        addContentView(b, lp);
        updateRewardCenterButton();
    }

    private void updateRewardCenterButton() {
        View v = getWindow().getDecorView().findViewWithTag(STAR_TAG);
        if (v instanceof Button) {
            int stars = getSharedPreferences("masari_data", MODE_PRIVATE).getInt("reward_stars", 0);
            ((Button) v).setText("★ " + toArabic(stars));
        }
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

    private String toArabic(int n) {
        return String.valueOf(n)
                .replace('0','٠').replace('1','١').replace('2','٢').replace('3','٣').replace('4','٤')
                .replace('5','٥').replace('6','٦').replace('7','٧').replace('8','٨').replace('9','٩');
    }
}
