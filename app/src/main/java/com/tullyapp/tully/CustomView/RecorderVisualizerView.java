package com.tullyapp.tully.CustomView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.tullyapp.tully.R;

import java.util.LinkedList;

/**
 * Created by macbookpro on 01/10/17.
 */

public class RecorderVisualizerView extends View {
    private static final int MAX_AMPLITUDE = 32767;

    private static final int LINE_WIDTH = 1; // width of visualizer line
    private LinkedList<Float> amplitudes; // amplitudes for line lengths
    private int width; // width of this View
    private int height; // height of this View
    private Paint linePaint; // specifies line drawing characteristics

    // constructor
    public RecorderVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs); // call superclass constructor
        linePaint = new Paint(); // create Paint for lines
        linePaint.setColor(getResources().getColor(R.color.colorAccent)); // set color to green
        linePaint.setStrokeWidth(LINE_WIDTH); // set stroke width
    }

    // called when the dimensions of the View change
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w; // new width of this View
        height = h; // new height of this View
        amplitudes = new LinkedList<>();
    }

    // clear all amplitudes to prepare for a new visualization
    public void clear() {
        amplitudes.clear();
    }

    // add the given amplitude to the amplitudes ArrayList
    public void addAmplitude(float amplitude) {
        if (amplitudes!=null){
            invalidate();
            amplitudes.add(amplitude); // add newest to the amplitudes ArrayList
            // if the power lines completely fill the VisualizerView
            if (amplitudes.size() * LINE_WIDTH >= width) {
                amplitudes.remove(0); // remove oldest power value
            }
        }
    }

    // draw the visualizer with scaled lines representing the amplitudes
    @Override
    public void onDraw(Canvas canvas) {
        int middle = height / 2; // get the middle of the View
        float curX = 0; // start curX at zero
        for (float power : amplitudes) {
            if (power>20_000){
                power = power / 2;
            }
            float scaledHeight = ( power / MAX_AMPLITUDE) * (height - 1);
            curX += LINE_WIDTH ; // increase X by LINE_WIDTH
            canvas.drawLine(curX, middle + scaledHeight , curX, middle - scaledHeight, linePaint);
        }
    }
}
