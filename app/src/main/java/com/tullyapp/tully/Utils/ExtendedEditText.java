package com.tullyapp.tully.Utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

/**
 * Created by macbookpro on 21/09/17.
 */

public class ExtendedEditText extends android.support.v7.widget.AppCompatEditText {

    private EditBackPressedListener editBackPressedListener;
    private SelectionChangeListener listener;

    public ExtendedEditText(Context context) {
        super(context);
    }

    public ExtendedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (editBackPressedListener!=null){
                editBackPressedListener.onEditBackPressed();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setEditBackPressedListener(EditBackPressedListener editBackPressedListener){
        this.editBackPressedListener = editBackPressedListener;
    }

    public interface EditBackPressedListener{
        void onEditBackPressed();
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

    @Override
    public boolean isSuggestionsEnabled() {
        return false;
    }

    public void setSelectionChangeListener(SelectionChangeListener listener) {
        this.listener = listener;
    }

    public interface SelectionChangeListener {
        void onTextSelected();
        void onTextUnselected();
    }
}
