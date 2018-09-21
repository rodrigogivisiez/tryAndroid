package com.tullyapp.tully.CustomView;

import android.content.Context;
import android.util.AttributeSet;

import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.Percent;

import java.math.BigDecimal;

/**
 * Created by macbookpro on 29/09/17.
 */

public class ImageProgressBar extends android.support.v7.widget.AppCompatImageView {

    private static final BigDecimal MAX = BigDecimal.valueOf(10000);

    public ImageProgressBar(Context context) {
        super(context);
    }

    public ImageProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImageResource(R.drawable.music_progress);
    }

    public ImageProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setImageResource(R.drawable.music_progress);
    }

    public void setCurrentValue(Percent percent){
        int cliDrawableImageLevel = percent.asBigDecimal().multiply(MAX).intValue();
        setImageLevel(cliDrawableImageLevel);
    }
}
