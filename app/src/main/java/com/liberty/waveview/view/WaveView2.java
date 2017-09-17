package com.liberty.waveview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.liberty.waveview.Logger;
import com.liberty.waveview.R;

import static android.graphics.Canvas.ALL_SAVE_FLAG;

/**
 * Created by liberty on 2017/9/17.
 */

public class WaveView2 extends View {

    private static final String TAG = WaveView2.class.getSimpleName();
    private boolean mRecordingState = false;
    private float mPhase = 0;
    private float mSpeed = 0.5f;
    private float speed = 0.5f;// 0.15f;
    private float noise = 0.0f;// 0.02f;

    private float curNoise = 0.0f;
    private float preNoise = 0.0f;

    private int mHalfViewWidth;
    private int mHalfViewHeight;
    private int mWidth;
    private int mHeight;

    private int lineStartColor;
    private int lineEndColor;

    private Paint mFillPaint;
    private Paint mLinePaint;

    private Path[] paths = new Path[5];
    private @ColorInt
    int[] colors=new int[5];
    private static final float LINE_WIDTH[] = { 5f, 5f, 5f, 5f, 5f };

    private float[][] lineX=new float[][]{
            {-2,-1.024f,-0.667f,0,0.667f,1.024f,2},
            {-2.267f,-1.221f,-0.933f,-0.259f,0.4f,0.847f,0.321f}
    };

    private PorterDuffXfermode xfermode;

    public WaveView2(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        setLayerType(LAYER_TYPE_HARDWARE,null);
//        this.getHolder().addCallback(this);
//        //设置透明背景，以及解决setXfermode失效的问题，具体原理不明
//        this.getHolder().setFormat(PixelFormat.TRANSPARENT);
//        this.setZOrderOnTop(true);

        mFillPaint = new Paint();
        mFillPaint.setAntiAlias(true);
        mFillPaint.setStyle(Paint.Style.FILL);

        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.STROKE);

        xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

        for (int i =0; i<5;i++){
            paths[i] = new Path();
        }

        colors[0] = context.getResources().getColor(R.color.line1);
        colors[1] = context.getResources().getColor(R.color.line2);
        colors[2] = context.getResources().getColor(R.color.line3);
        colors[3] = context.getResources().getColor(R.color.line4);
        colors[4] = context.getResources().getColor(R.color.line5);

        lineStartColor = context.getResources().getColor(R.color.line1_start);
        lineEndColor = context.getResources().getColor(R.color.line1_end);
    }

    private static final double commonParam =0.75*Math.PI;

    private double calcValueLineOne(float x,float mesc){
        mesc%=2;

        Double a = Math.sin(commonParam*x-(mesc*Math.PI));
//        Double a = Math.sin(commonParam*x-(mPhase));
        Double d = 4+Math.pow(x,4);
        Double b = (4/d);
        Double c = Math.pow(b,2.5);
        Double y = c*a;
        Log.d(TAG,"getLine1:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        doDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHalfViewHeight = h/2;
        mHalfViewWidth = w/2;
        mWidth = w;
        mHeight = h;
    }

    private static final int SAMPLINT_SIZE = 64;
    private float[] samplingX;
    private float[] mapX;

    private volatile float mAmplitude;
    private float[][] crestAndCrossPoints = new float[9][];
    {
        for (int i = 0;i<9;i++){
            crestAndCrossPoints[i]=new float[2];
        }
    }

    private volatile long mesc;
    private long start=-1;

    public void setAmplitude(float amplitude, long mesc) {
        mAmplitude = amplitude*2;
        if (start == -1){
            start = mesc;
            this.mesc = start;
        }else {
            this.mesc = mesc-start;
        }
        postInvalidate();
    }

    private void doDraw(Canvas canvas){
//        cleanCanvas(canvas);
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

        for (int i = 0; i < paths.length; i++) {
            paths[i].rewind();
            paths[i].moveTo(0,mHalfViewHeight);
        }

        float x,y;
        float[] xy;
        int sc=-1;

        Logger.d(TAG,"mesc = "+mesc);
        float offset = mesc/500f;
        Logger.d(TAG,"offset = "+offset);
        float lastV,curV = 0,nextV = (float)(mAmplitude*calcValueLineOne(mapX[0],offset-0.3f));
        Logger.d(TAG,"mAmplitude = "+mAmplitude);
        float absLastV,absCurV,absNextV;
        boolean lastIsCrest = false;

        int crestAndCrossCount = 0;

        sc = canvas.saveLayer(0,0,mWidth,mHeight,null,ALL_SAVE_FLAG);
        mFillPaint.setAlpha((int)(0.4*255));
        mLinePaint.setAlpha((int)(0.4*255));
        for (int i = 0; i<=SAMPLINT_SIZE; i++){
            x = samplingX[i];
            lastV =curV;
            curV = nextV;
            nextV = i<SAMPLINT_SIZE?(float)(mAmplitude*calcValueLineOne(mapX[i+1],offset-0.3f)):0;
            Log.d(TAG,"x = "+x+" nextV = "+nextV+" halfHeight = "+mHalfViewHeight);
            paths[2].lineTo(x,mHalfViewHeight+curV);
            paths[3].lineTo(x,mHalfViewHeight-curV);
            absLastV = Math.abs(lastV);
            absCurV = Math.abs(curV);
            absNextV = Math.abs(nextV);
            if (i== 0||i==SAMPLINT_SIZE ||(lastIsCrest&&absCurV<absLastV&&absCurV<absNextV)){
                xy = crestAndCrossPoints[crestAndCrossCount++];
                xy[0] = x;
                xy[1] = 0;
                lastIsCrest = false;
                Log.d(TAG,"lastIsCrest = "+lastIsCrest+" xy = "+xy[0]+" "+xy[1]);
            }else if (!lastIsCrest&&absCurV>absLastV&&absCurV>absNextV){
                xy = crestAndCrossPoints[crestAndCrossCount++];
                xy[0] = x;
                xy[1] = curV;
                lastIsCrest = true;
                Log.d(TAG,"lastIsCrest = "+lastIsCrest+" xy = "+xy[0]+" "+xy[1]);
            }
        }

        paths[2].lineTo(mWidth,mHalfViewHeight);
        paths[3].lineTo(mWidth,mHalfViewHeight);
//        for (float i=-K;i<=K;i+=0.01){
////            x = mHalfViewWidth*((i+K)/K);
//            x = mHalfViewWidth*i;
//            y = (float) (getLine1(i)*mHalfViewWidth+mHalfViewHeight);
//            paths[0].lineTo(x, y);
//            y = (float) (getLine2(i)*mHalfViewWidth/2+mHalfViewHeight);
//            paths[1].lineTo(x, y);
//        }
        canvas.drawPath(paths[2],mFillPaint);
        canvas.drawPath(paths[3],mFillPaint);

        float startX,crestY,endX;
        RectF rectF = new RectF();

        mFillPaint.setXfermode(xfermode);

        for (int i = 0; i < 9; i++) {
            Logger.d(TAG,"crestAndCrossPoints i = "+i+" "+crestAndCrossPoints[i][0]+" "+crestAndCrossPoints[i][1]);
        }

        for (int i = 2;i<crestAndCrossCount;i+=2){
            startX = crestAndCrossPoints[i-2][0];
            crestY = crestAndCrossPoints[i-1][1];
            endX = crestAndCrossPoints[i][0];
            Log.d(TAG,"startX = "+startX+"  crestY = "+crestY+"  endX = "+endX);
            mFillPaint.setShader(new LinearGradient(0,mHalfViewHeight+crestY,0,mHalfViewHeight-crestY,lineStartColor,lineEndColor, Shader.TileMode.REPEAT));
            rectF.set(startX,mHalfViewHeight+crestY,endX,mHalfViewHeight-crestY);
            canvas.drawRect(rectF,mFillPaint);
        }
        mFillPaint.setShader(null);
        mFillPaint.setXfermode(null);
        mLinePaint.setStrokeWidth(LINE_WIDTH[0]);
        mLinePaint.setColor(colors[0]);
        canvas.drawPath(paths[2],mLinePaint);
        canvas.drawPath(paths[3],mLinePaint);

        canvas.restoreToCount(sc);
//        sc = canvas.saveLayer(0,0,mWidth,mHeight,null,ALL_SAVE_FLAG);
        mFillPaint.setColor(lineStartColor);
//        mFillPaint.setAlpha((int)(0.5*255));
//        for (float i=-K;i<=K;i+=0.01){
//            x = mHalfViewWidth*((i+K)/K);
//            y = (float) (getLine3(i)*mHalfViewWidth/2+mHalfViewHeight);
//            paths[2].lineTo(x, y);
////            paths[2].close();
//            y = (float) (getLine4(i)*mHalfViewWidth/2+mHalfViewHeight);
//            paths[3].lineTo(x, y);
////            paths[3].close();
//            y = (float) (getLine5(i)*mHalfViewHeight/2+mHalfViewHeight);
//            paths[4].lineTo(x,y);
//        }
//        canvas.drawPath(paths[2],mFillPaint);
//        canvas.drawPath(paths[3],mFillPaint);
//        mFillPaint.setStyle(Paint.Style.STROKE);
//        mFillPaint.setStrokeWidth(5f);
//        mFillPaint.setAlpha((int) (0.3*255));
//        canvas.drawPath(baseLine,mFillPaint);
//        mFillPaint.setStyle(Paint.Style.FILL);
//        mFillPaint.setAlpha((int) (0.7*255));
//        mFillPaint.setXfermode(xfermode);
//        mFillPaint.setShader(gradient3);
//        canvas.drawRect(0,15*mHalfViewHeight/16, (float) (mWidth*0.254),17*mHalfViewHeight/16,mFillPaint);
//        mFillPaint.setShader(gradient);
//        canvas.drawRect((float) (mWidth*0.254),mHalfViewHeight*13/16,(float) (mWidth*0.585),19*mHalfViewHeight/16,mFillPaint);
//        mFillPaint.setShader(gradient4);
//        canvas.drawRect((float) (mWidth*0.585),mHalfViewHeight*14/16,mWidth,mHalfViewHeight*18/16,mFillPaint);
//        mFillPaint.setXfermode(null);
//        mFillPaint.setShader(null);
//        mLinePaint.setAlpha((int)(0.5*255));
//        mLinePaint.setColor(colors[2]);
//        mLinePaint.setStrokeWidth(LINE_WIDTH[2]);
//        canvas.drawPath(paths[2],mLinePaint);
//        mLinePaint.setColor(colors[3]);
//        mLinePaint.setStrokeWidth(LINE_WIDTH[3]);
//        canvas.drawPath(paths[3],mLinePaint);
//        mLinePaint.setColor(colors[4]);
//        canvas.drawPath(paths[4],mLinePaint);
//        canvas.restoreToCount(sc);
        sc = canvas.saveLayer(0,0,mWidth,mHeight,null,ALL_SAVE_FLAG);
        mFillPaint.setAlpha((int)(0.7*255));
        mLinePaint.setAlpha((int)(0.7*255));
        int crestAndCrossCount1=0;

        for (int i = 0; i<=SAMPLINT_SIZE; i++){
            x = samplingX[i];
            lastV =curV;
            curV = nextV;
            nextV = i<SAMPLINT_SIZE?(float)(mAmplitude*calcValueLineOne(mapX[i+1],offset)):0;
            Log.d(TAG,"x = "+x+" nextV = "+nextV+" halfHeight = "+mHalfViewHeight);
            paths[0].lineTo(x,mHalfViewHeight+curV);
            paths[1].lineTo(x,mHalfViewHeight-curV);
            absLastV = Math.abs(lastV);
            absCurV = Math.abs(curV);
            absNextV = Math.abs(nextV);
            if (i== 0||i==SAMPLINT_SIZE ||(lastIsCrest&&absCurV<absLastV&&absCurV<absNextV)){
                xy = crestAndCrossPoints[crestAndCrossCount1++];
                xy[0] = x;
                xy[1] = 0;
                lastIsCrest = false;
                Log.d(TAG,"lastIsCrest = "+lastIsCrest+" xy = "+xy[0]+" "+xy[1]);
            }else if (!lastIsCrest&&absCurV>absLastV&&absCurV>absNextV){
                xy = crestAndCrossPoints[crestAndCrossCount1++];
                xy[0] = x;
                xy[1] = curV;
                lastIsCrest = true;
                Log.d(TAG,"lastIsCrest = "+lastIsCrest+" xy = "+xy[0]+" "+xy[1]);
            }
        }

        paths[0].lineTo(mWidth,mHalfViewHeight);
        paths[1].lineTo(mWidth,mHalfViewHeight);
//        for (float i=-K;i<=K;i+=0.01){
////            x = mHalfViewWidth*((i+K)/K);
//            x = mHalfViewWidth*i;
//            y = (float) (getLine1(i)*mHalfViewWidth+mHalfViewHeight);
//            paths[0].lineTo(x, y);
//            y = (float) (getLine2(i)*mHalfViewWidth/2+mHalfViewHeight);
//            paths[1].lineTo(x, y);
//        }
        canvas.drawPath(paths[0],mFillPaint);
        canvas.drawPath(paths[1],mFillPaint);



        mFillPaint.setXfermode(xfermode);

        for (int i = 0; i < 9; i++) {
            Logger.d(TAG,"crestAndCrossPoints i = "+i+" "+crestAndCrossPoints[i][0]+" "+crestAndCrossPoints[i][1]);
        }

        for (int i = 2;i<crestAndCrossCount1;i+=2){
            startX = crestAndCrossPoints[i-2][0];
            crestY = crestAndCrossPoints[i-1][1];
            endX = crestAndCrossPoints[i][0];
            Log.d(TAG,"startX = "+startX+"  crestY = "+crestY+"  endX = "+endX);
            mFillPaint.setShader(new LinearGradient(0,mHalfViewHeight+crestY,0,mHalfViewHeight-crestY,lineStartColor,lineEndColor, Shader.TileMode.REPEAT));
            rectF.set(startX,mHalfViewHeight+crestY,endX,mHalfViewHeight-crestY);
            canvas.drawRect(rectF,mFillPaint);
        }
        mFillPaint.setShader(null);
        mFillPaint.setXfermode(null);
        mLinePaint.setStrokeWidth(LINE_WIDTH[0]);
        mLinePaint.setColor(colors[0]);
        canvas.drawPath(paths[0],mLinePaint);
        canvas.drawPath(paths[1],mLinePaint);
//        canvas.drawPath(paths[1],mFillPaint);
//        mFillPaint.setStyle(Paint.Style.STROKE);
//        mFillPaint.setStrokeWidth(3f);
//        mFillPaint.setAlpha((int)(0.1*255));
//        canvas.drawPath(baseLine,mFillPaint);
//        mFillPaint.setStyle(Paint.Style.FILL);
//        mFillPaint.setXfermode(xfermode);
//        mFillPaint.setAlpha((int)(0.7*255));
//        mFillPaint.setShader(gradient1);
//        canvas.drawRect(0,14*mHalfViewHeight/16,mWidth/3,18*mHalfViewHeight/16,mFillPaint);
//        mFillPaint.setShader(gradient);
//        canvas.drawRect(mWidth/3,12*mHalfViewHeight/16,2*mWidth/3,20*mHalfViewHeight/16,mFillPaint);
//        mFillPaint.setShader(gradient2);
//        canvas.drawRect(2*mWidth/3,14*mHalfViewHeight/16,mWidth,18*mHalfViewHeight/16,mFillPaint);
//        mFillPaint.setXfermode(null);
//        mFillPaint.setShader(null);
//        mLinePaint.setAlpha((int)(0.7*255));
//        mLinePaint.setColor(colors[0]);
//        mLinePaint.setStrokeWidth(LINE_WIDTH[0]);
//        canvas.drawPath(paths[0],mLinePaint);
//        mLinePaint.setColor(colors[1]);
//        mLinePaint.setStrokeWidth(LINE_WIDTH[1]);
//        canvas.drawPath(paths[1],mLinePaint);
        canvas.restoreToCount(sc);

    }
}
