package com.liberty.waveview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * Created by liberty on 2017/9/16.
 */

public class WaveRenderView extends SurfaceView {
    public WaveRenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static final int SAMPLINT_SIZE = 64;

    private float[] samplingX;
    private float[] mapX;

    private int mWidth;

    private void doDraw(Canvas canvas){
        if (samplingX==null){
            mWidth = canvas.getWidth();

            samplingX = new float[SAMPLINT_SIZE+1];
            mapX = new float[SAMPLINT_SIZE+1];
            float gap = mWidth/(float)SAMPLINT_SIZE;
            float x;
            for (int i = 0;i<=SAMPLINT_SIZE;i++){
                x = i*gap;
                samplingX[i] = x;
                mapX[i] = (x/(float)mWidth)*4-2;
            }
        }


    }
}
