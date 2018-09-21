package com.tullyapp.tully.Utils;

import android.content.Context;
import android.util.AttributeSet;

import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

/**
 * Created by kathan on 27/03/18.
 */

public class CatRangeBar extends CrystalRangeSeekbar {
    private static final String TAG = CatRangeBar.class.getSimpleName();

    public CatRangeBar(Context context) {
        super(context);
    }

    public CatRangeBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CatRangeBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public float getBarHeight() {
        return 265f;
    }
}