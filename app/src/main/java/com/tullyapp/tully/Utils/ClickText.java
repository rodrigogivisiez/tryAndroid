package com.tullyapp.tully.Utils;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by macbookpro on 13/09/17.
 */

public class ClickText extends android.support.v7.widget.AppCompatEditText
{
    public ClickText (Context context, AttributeSet attrs)
    {
        super (context, attrs);
    }

    @Override
    public boolean isSuggestionsEnabled() {
        super.isSuggestionsEnabled();
        return false;
    }

    boolean canPaste() {
        return false;
    }

}