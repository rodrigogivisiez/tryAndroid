package com.tullyapp.tully.Utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by macbookpro on 28/09/17.
 */

public class NotifyingScrollView extends ScrollView {

    private ScrollChangeListener listener;

    public NotifyingScrollView(Context context) {
        super(context);
    }

    public NotifyingScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyingScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (listener != null) {
            listener.onScrollChanged();
        }
    }

    public void setScrollChangeListener(ScrollChangeListener listener) {
        this.listener = listener;
    }

    public interface ScrollChangeListener {
        void onScrollChanged();
    }

}
