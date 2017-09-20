package com.liberty.waveview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.liberty.waveview.R;

import static android.graphics.Canvas.ALL_SAVE_FLAG;


/**
 * Created by Peter on 2017/9/8 0008.
 */

public class WaveView extends SurfaceView implements SurfaceHolder.Callback{

    private static final String TAG = WaveView.class.getSimpleName();

    private int mHalfViewHeight;
    private int mWidth;
    private int mHeight;

    private int lineStartColor;
    private int lineEndColor;

    private DrawThread mThread;

    private Paint mFillPaint;
    private Paint mLinePaint;

    private Path[] paths = new Path[5];
    private @ColorInt int[] colors=new int[5];
    private static final float LINE_WIDTH[] = { 5f, 5f, 5f, 5f, 5f };

    private float[][] lineX=new float[][]{
            {-2,-1.024f,-0.667f,0,0.667f,1.024f,2},
            {-2.267f,-1.221f,-0.933f,-0.259f,0.4f,0.847f,0.321f}
    };

    private PorterDuffXfermode xfermode;

    private Path baseLine;

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        setLayerType(LAYER_TYPE_HARDWARE,null);
        this.getHolder().addCallback(this);
        //设置透明背景，以及解决setXfermode失效的问题，具体原理不明
        this.getHolder().setFormat(PixelFormat.TRANSPARENT);
        this.setZOrderOnTop(true);

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

        baseLine = new Path();


    }

    private static final double commonParam =0.75*Math.PI;

    private double calcValueLineOne(float x,float mesc){
        mesc%=2;

        double a = Math.sin(commonParam*x-(mesc*Math.PI));
//        Double a = Math.sin(commonParam*x-(mPhase));
        double d = 4+Math.pow(x,4);
        double b = (4/d);
        double c = Math.pow(b,2.5);
        double y = c*a;
//        Log.d(TAG,"getLine1:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new DrawThread(holder);
        mThread.setRun(true);
        mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

//        Log.d(TAG,"width = "+width+" height = "+height+" halfWidth = "+mHalfViewWidth+" halfHeight = "+mHalfViewHeight);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (mSurfaceLock){
            mThread.setRun(false);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHalfViewHeight = h/2;
        mWidth = w;
        mHeight = h;
        baseLine.moveTo(0,mHalfViewHeight);
        baseLine.lineTo(mWidth,mHalfViewHeight);
    }

    public synchronized void setAmplitude(float amplitude) {
        mAmplitude = amplitude;
        Log.d(TAG,"mAmplitude = "+mAmplitude);
    }

    private final Object mSurfaceLock = new Object();

    private class DrawThread extends Thread{
        private SurfaceHolder mHolder;
        private boolean mIsRun = false;

        public DrawThread(SurfaceHolder holder){
            mHolder = holder;
        }

        @Override
        public void run() {
            super.run();
            long start = System.currentTimeMillis();
            while (true){
                synchronized (mSurfaceLock){
                    if (!mIsRun){
                        return;
                    }

                    Canvas canvas = mHolder.lockCanvas();
                    if (canvas!=null){
                        long mesc = System.currentTimeMillis()-start;
                        doDraw(canvas,mesc);
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setRun(boolean isRun){
            this.mIsRun = isRun;
        }
    }

    private Paint cleanPaint;

    private void cleanCanvas(Canvas canvas){
        if (cleanPaint == null){
            cleanPaint = new Paint();
            cleanPaint.setAntiAlias(true);
            cleanPaint.setStyle(Paint.Style.STROKE);
            cleanPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }
        canvas.drawPaint(cleanPaint);
    }

    private static final int SAMPLINT_SIZE = 64;
    private float[] samplingX;
    private float[] mapX;

    private volatile float mAmplitude =0f;
    private float[][] crestAndCrossPoints = new float[9][];
    {
        for (int i = 0;i<9;i++){
            crestAndCrossPoints[i]=new float[2];
        }
    }

    private float[][] crestAndCrossPoints1 = new float[9][];
    {
        for (int i = 0;i<9;i++){
            crestAndCrossPoints1[i]=new float[2];
        }
    }

    private synchronized void doDraw(Canvas canvas,long mesc){
        cleanCanvas(canvas);
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

        float x;
        float[] xy;
        int sc=-1;

        float offset = mesc/500f;
        float lastV,curV = 0,nextV = (float)(mAmplitude*calcValueLineOne(mapX[0],offset-0.3f));
        float absLastV,absCurV,absNextV;
        boolean lastIsCrest = false;

        int crestAndCrossCount = 0;

        sc = canvas.saveLayer(0,0,mWidth,mHeight,null,ALL_SAVE_FLAG);
        mFillPaint.setAlpha((int)(0.7*255));
        mLinePaint.setAlpha((int)(0.7*255));
        for (int i = 0; i<=SAMPLINT_SIZE; i++){
            x = samplingX[i];
            lastV =curV;
            curV = nextV;
            nextV = i<SAMPLINT_SIZE?(float)(mAmplitude*calcValueLineOne(mapX[i+1],offset-0.3f)):0;
//            Log.d(TAG,"x = "+x+" nextV = "+nextV+" halfHeight = "+mHalfViewHeight);
            paths[2].lineTo(x,mHalfViewHeight+curV);
            paths[3].lineTo(x,mHalfViewHeight-curV);
            absLastV = Math.abs(lastV);
            absCurV = Math.abs(curV);
            absNextV = Math.abs(nextV);
            if (i== 0||i==SAMPLINT_SIZE ||(lastIsCrest&&absCurV<absLastV&&absCurV<absNextV)){
                if (crestAndCrossCount<crestAndCrossPoints.length){
                    xy = crestAndCrossPoints[crestAndCrossCount++];
                    xy[0] = x;
                    xy[1] = 0;
                    lastIsCrest = false;
//                    Log.d(TAG,"lastIsCrest = "+lastIsCrest+" xy = "+xy[0]+" "+xy[1]);
                }
            }else if (!lastIsCrest&&absCurV>absLastV&&absCurV>absNextV){
                if (crestAndCrossCount<crestAndCrossPoints.length){
                    xy = crestAndCrossPoints[crestAndCrossCount++];
                    xy[0] = x;
                    xy[1] = curV;
                    lastIsCrest = true;
//                    Log.d(TAG,"lastIsCrest = "+lastIsCrest+" xy = "+xy[0]+" "+xy[1]);
                }
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
//            Log.d(TAG,"crestAndCrossPoints i = "+i+" "+crestAndCrossPoints[i][0]+" "+crestAndCrossPoints[i][1]);
        }

        for (int i = 2;i<crestAndCrossCount;i+=2){
            startX = crestAndCrossPoints[i-2][0];
            crestY = crestAndCrossPoints[i-1][1];
            endX = crestAndCrossPoints[i][0];
//            Log.d(TAG,"startX = "+startX+"  crestY = "+crestY+"  endX = "+endX);
            mFillPaint.setShader(new LinearGradient(0,mHalfViewHeight+crestY,0,mHalfViewHeight-crestY,lineStartColor,lineEndColor, Shader.TileMode.REPEAT));
            rectF.set(startX,mHalfViewHeight+crestY,endX,mHalfViewHeight-crestY);
            canvas.drawRect(rectF,mFillPaint);
        }
        mFillPaint.setShader(null);
        mFillPaint.setXfermode(null);
        mLinePaint.setStrokeWidth(5f);
        mLinePaint.setColor(colors[2]);
        canvas.drawPath(paths[2],mLinePaint);
        mLinePaint.setColor(colors[3]);
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
//            Log.d(TAG,"x = "+x+" nextV = "+nextV+" halfHeight = "+mHalfViewHeight);
            paths[0].lineTo(x,mHalfViewHeight+curV);
            paths[1].lineTo(x,mHalfViewHeight-curV);
            paths[4].lineTo(x,(mHalfViewHeight-curV/5));
            absLastV = Math.abs(lastV);
            absCurV = Math.abs(curV);
            absNextV = Math.abs(nextV);
            if (i== 0||i==SAMPLINT_SIZE ||(lastIsCrest&&absCurV<absLastV&&absCurV<absNextV)){
                if (crestAndCrossCount1<crestAndCrossPoints1.length){
                    xy = crestAndCrossPoints1[crestAndCrossCount1++];
                    xy[0] = x;
                    xy[1] = 0;
                    lastIsCrest = false;
//                    Log.d(TAG,"lastIsCrest = "+lastIsCrest+" xy = "+xy[0]+" "+xy[1]);
                }
            }else if (!lastIsCrest&&absCurV>absLastV&&absCurV>absNextV){
                if (crestAndCrossCount1<crestAndCrossPoints1.length){
                    xy = crestAndCrossPoints1[crestAndCrossCount1++];
                    xy[0] = x;
                    xy[1] = curV;
                    lastIsCrest = true;
//                    Log.d(TAG,"lastIsCrest = "+lastIsCrest+" xy = "+xy[0]+" "+xy[1]);
                }
            }
        }

        paths[0].lineTo(mWidth,mHalfViewHeight);
        paths[1].lineTo(mWidth,mHalfViewHeight);
        paths[4].lineTo(mWidth,mHalfViewHeight);
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
//        canvas.drawPath(paths[4],mFillPaint);


        mFillPaint.setXfermode(xfermode);

        for (int i = 0; i < 9; i++) {
//            Log.d(TAG,"crestAndCrossPoints i = "+i+" "+crestAndCrossPoints[i][0]+" "+crestAndCrossPoints[i][1]);
        }

        for (int i = 2;i<crestAndCrossCount1;i+=2){
            startX = crestAndCrossPoints1[i-2][0];
            crestY = crestAndCrossPoints1[i-1][1];
            endX = crestAndCrossPoints1[i][0];
//            Log.d(TAG,"startX = "+startX+"  crestY = "+crestY+"  endX = "+endX);
            mFillPaint.setShader(new LinearGradient(0,mHalfViewHeight+crestY,0,mHalfViewHeight-crestY,lineStartColor,lineEndColor, Shader.TileMode.REPEAT));
            rectF.set(startX,mHalfViewHeight+crestY,endX,mHalfViewHeight-crestY);
            canvas.drawRect(rectF,mFillPaint);
        }
        mFillPaint.setShader(null);
        mFillPaint.setXfermode(null);
        mLinePaint.setStrokeWidth(5f);

        mLinePaint.setColor(colors[1]);
        canvas.drawPath(paths[1],mLinePaint);
        mLinePaint.setColor(colors[0]);
        canvas.drawPath(paths[0],mLinePaint);
        mLinePaint.setColor(colors[4]);
        canvas.drawPath(paths[4],mLinePaint);
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