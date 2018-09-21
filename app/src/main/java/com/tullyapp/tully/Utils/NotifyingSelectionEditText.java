package com.tullyapp.tully.Utils;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by macbookpro on 28/09/17.
 */

public class NotifyingSelectionEditText extends android.support.v7.widget.AppCompatEditText{

    private SelectionChangeListener listener;

    public NotifyingSelectionEditText(Context context) {
        super(context);
    }

    public NotifyingSelectionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyingSelectionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (listener != null) {
            if (hasSelection()) {
                listener.onTextSelected();
            } else {
                listener.onTextUnselected();
            }
        }
    }
    public void setSelectionChangeListener(SelectionChangeListener listener) {
        this.listener = listener;
    }

    public interface SelectionChangeListener {
        void onTextSelected();
        void onTextUnselected();
    }
}