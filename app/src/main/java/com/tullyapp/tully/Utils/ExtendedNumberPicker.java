package com.tullyapp.tully.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.NumberPicker;

import com.tullyapp.tully.R;

import java.lang.reflect.Field;

/**
 * Created by macbookpro on 23/10/17.
 */

public class ExtendedNumberPicker extends NumberPicker {

    public ExtendedNumberPicker(Context context) {
        super(context);
    }

    public ExtendedNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        Class<?> numberPickerClass = null;
        try {
            numberPickerClass = Class.forName("android.widget.NumberPicker");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Field selectionDivider = null;

        try {
            selectionDivider = numberPickerClass.getDeclaredField("mSelectionDivider");

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        try {
            selectionDivider.setAccessible(true);
            selectionDivider.set(this, getResources().getDrawable(R.color.colorAccent));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}