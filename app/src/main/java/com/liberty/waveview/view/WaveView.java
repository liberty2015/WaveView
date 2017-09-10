package com.liberty.waveview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.liberty.waveview.R;


/**
 * Created by Peter on 2017/9/8 0008.
 */

public class WaveView extends SurfaceView implements SurfaceHolder.Callback{

    private static final String TAG = WaveView.class.getSimpleName();
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

    private LinearGradient gradient,gradient1,gradient2,gradient3,gradient4;

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

    private double getLine1(float x){
        Double a = Math.sin(commonParam*x-(0.5*Math.PI+mPhase));
        Double d = 4+Math.pow(x,4);
        Double b = (4/d);
        Double c = Math.pow(b,2.5);
        Double y = 0.5*c*a;
        Log.d(TAG,"getLine1:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    private double getLine2(float x){
        Double a = Math.sin(commonParam*x+(0.5*Math.PI+mPhase));
        Double b = (4/(4+Math.pow(x,4)));
        Double c = Math.pow(b,2.5);
        Double y = 0.5*c*a;
        Log.d(TAG,"getLine2:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    private double getLine3(float x){
        Double a = Math.sin(commonParam*x-(0.27*Math.PI+mPhase));
        Double b = (4/(4+Math.pow(x,4)));
        Double c = Math.pow(b,2.5);
        Double y = 0.5*c*a;
        Log.d(TAG,"getLine3:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    private double getLine4(float x){
        Double a = Math.sin(commonParam*x+(0.73*Math.PI+mPhase));
        Double b = (4/(4+Math.pow(x,4)));
        Double c = Math.pow(b,2.5);
        Double y = 0.5*c*a;
        Log.d(TAG,"getLine4:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    private double getLine5(float x){
        Double a = Math.sin(commonParam*x-(0.5*Math.PI+mPhase));
        Double b = (4/(4+Math.pow(x,4)));
        Double c = Math.pow(b,2.5);
        Double y = 0.1*c*a;
        Log.d(TAG,"getLine5:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new DrawThread(holder);
        mThread.setRun(true);
        mThread.start();
//        Canvas canvas = holder.lockCanvas();
//        doDraw(canvas);
//        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.d(TAG,"width = "+width+" height = "+height+" halfWidth = "+mHalfViewWidth+" halfHeight = "+mHalfViewHeight);

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
        mHalfViewWidth = w/2;
        mWidth = w;
        mHeight = h;
        baseLine.moveTo(0,mHalfViewHeight);
        baseLine.lineTo(mWidth,mHalfViewHeight);
        gradient = new LinearGradient(mWidth/3,mHalfViewHeight*13/16,mWidth/3,mHalfViewHeight*19/16,lineStartColor,lineEndColor, Shader.TileMode.MIRROR);;
        gradient1 = new LinearGradient(0,14*mHalfViewHeight/16,0,18*mHalfViewHeight/16,lineEndColor,lineStartColor, Shader.TileMode.MIRROR);;
        gradient2 = new LinearGradient(2*mWidth/3,14*mHalfViewHeight/16,2*mWidth/3,18*mHalfViewHeight/16,lineEndColor,lineStartColor, Shader.TileMode.MIRROR);
        gradient3 = new LinearGradient(0,15*mHalfViewHeight/16,0,17*mHalfViewHeight/16,lineEndColor,lineStartColor, Shader.TileMode.MIRROR);;
        gradient4 = new LinearGradient(2*mWidth/3,mHalfViewHeight*14/16,2*mWidth/3,mHalfViewHeight*18/16,lineEndColor,lineStartColor, Shader.TileMode.MIRROR);

    }

    public void setAmplitude(int amplitude, boolean recordingState) {

        mRecordingState = recordingState;
        if (!recordingState) {
            return;
        }

        float f = (float) (amplitude) / 48000 * 2.0f;
        if (f > 0.99f)
            f = 0.99f;

        if (f < 0.1f) {
            f = 0.1f;
        }

        if (Math.abs(f - noise) < 0.001)
            return;

        if (Math.abs(curNoise - noise) > 0.001) {
            if (noise < f && preNoise < noise) {
                preNoise = noise;
                noise = f;
            }

            return;
        }

        preNoise = noise;
        noise = f;

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
            while (true){
                synchronized (mSurfaceLock){
                    if (!mIsRun){
                        return;
                    }
                    mPhase = (float) ((mPhase + Math.PI*mSpeed)%(2*Math.PI));
                    Canvas canvas = mHolder.lockCanvas();
                    if (canvas!=null){
                        doDraw(canvas);
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setRun(boolean isRun){
            this.mIsRun = isRun;
        }
    }

    private final static int K=2;

    private void drawTest(Canvas canvas){
//        canvas.drawColor(Color.WHITE);
//
//        int sc = 0;
//        sc = canvas.save();
//        Bitmap bitmap = Bitmap.createBitmap(mWidth/3,400, Bitmap.Config.ARGB_8888);
//        Canvas canvas1 = new Canvas(bitmap);
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        LinearGradient gradient = new LinearGradient(0,mHalfViewHeight-200,0,mHalfViewHeight+200,
//                lineStartColor,lineEndColor, Shader.TileMode.REPEAT);
//        paint.setShader(gradient);
//        paint.setStyle(Paint.Style.FILL);
//        canvas1.drawRect(0,0,mWidth/3,400,paint);
//
////        mPaint.setShader(gradient);
//        mPaint.setStyle(Paint.Style.FILL);
//        canvas.drawBitmap(bitmap,0,mHalfViewHeight-200,mPaint);
//
//        mPaint.setStyle(Paint.Style.STROKE);
//        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//
//        paths[0].rewind();
//        paths[0].moveTo(0,mHalfViewHeight);
//        paths[1].rewind();
//        paths[1].moveTo(0,mHalfViewHeight);
//
//        float x,y;
//        for (float i =-K;i<=K;i+=0.01){
//            x = mHalfViewWidth*((i+K)/K);
//            for (int j = 0; j < 2; j++) {
//                switch (j){
//                    case 0:{
//                        y = (float) getLine1(i)*mHalfViewHeight+mHalfViewHeight;
//                        paths[j].lineTo(x,y);
//                    }
//                    break;
//                    case 1:{
//                        y = (float) getLine2(i)*mHalfViewHeight+mHalfViewHeight;
//                        paths[j].lineTo(x,y);
//                    }
//                    break;
//                    default:
//                        y=0;
//                }
//                Log.d(TAG,"line"+j+": x="+x+" y="+y);
//                Log.d(TAG,"halfWidth = "+mHalfViewWidth+" halfHeight = "+mHalfViewHeight);
//            }
//        }
//
//        for (int i = 0; i < 2; i++) {
//            mPaint.setColor(colors[i]);
//            mPaint.setStrokeWidth(LINE_WIDTH[i]);
//            canvas.drawPath(paths[i],mPaint);
//        }
//
//        mPaint.setXfermode(null);
//        canvas.restoreToCount(sc);

    }

    private void cleanCanvas(Canvas canvas){
        Paint cleanPaint = new Paint();
        cleanPaint.setAntiAlias(true);
        cleanPaint.setStyle(Paint.Style.STROKE);
        cleanPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(cleanPaint);
    }

    private void doDraw(Canvas canvas){
        cleanCanvas(canvas);
        for (int i = 0; i < paths.length; i++) {
            paths[i].rewind();
            paths[i].moveTo(0,mHalfViewHeight);
        }

        float x,y;
        int sc = canvas.saveLayer(0,0,mWidth,mHeight,null);
        mFillPaint.setColor(lineStartColor);
        mFillPaint.setAlpha((int)(0.5*255));
        float[] lineY = new float[6];
        for (float i=-K;i<=K;i+=0.01){
            x = mHalfViewWidth*((i+K)/K);

            y = (float) (getLine3(i)*mHalfViewWidth/2+mHalfViewHeight);
            paths[2].lineTo(x, y);
//            paths[2].close();
            y = (float) (getLine4(i)*mHalfViewWidth/2+mHalfViewHeight);
            paths[3].lineTo(x, y);
//            paths[3].close();
            y = (float) (getLine5(i)*mHalfViewHeight/2+mHalfViewHeight);
            paths[4].lineTo(x,y);
        }
        canvas.drawPath(paths[2],mFillPaint);
        canvas.drawPath(paths[3],mFillPaint);
        mFillPaint.setStyle(Paint.Style.STROKE);
        mFillPaint.setStrokeWidth(5f);
        mFillPaint.setAlpha((int) (0.3*255));
        canvas.drawPath(baseLine,mFillPaint);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setAlpha((int) (0.7*255));
        mFillPaint.setXfermode(xfermode);
        mFillPaint.setShader(gradient3);
        canvas.drawRect(0,15*mHalfViewHeight/16, (float) (mWidth*0.254),17*mHalfViewHeight/16,mFillPaint);
        mFillPaint.setShader(gradient);
        canvas.drawRect((float) (mWidth*0.254),mHalfViewHeight*13/16,(float) (mWidth*0.585),19*mHalfViewHeight/16,mFillPaint);
        mFillPaint.setShader(gradient4);
        canvas.drawRect((float) (mWidth*0.585),mHalfViewHeight*14/16,mWidth,mHalfViewHeight*18/16,mFillPaint);
        mFillPaint.setXfermode(null);
        mFillPaint.setShader(null);
        mLinePaint.setAlpha((int)(0.5*255));
        mLinePaint.setColor(colors[2]);
        mLinePaint.setStrokeWidth(LINE_WIDTH[2]);
        canvas.drawPath(paths[2],mLinePaint);
        mLinePaint.setColor(colors[3]);
        mLinePaint.setStrokeWidth(LINE_WIDTH[3]);
        canvas.drawPath(paths[3],mLinePaint);
        mLinePaint.setColor(colors[4]);
        canvas.drawPath(paths[4],mLinePaint);
        canvas.restoreToCount(sc);

        sc = canvas.saveLayer(0,0,mWidth,mHeight,null);
        mFillPaint.setAlpha((int)(0.7*255));
        for (float i=-K;i<=K;i+=0.01){
            x = mHalfViewWidth*((i+K)/K);
            y = (float) (getLine1(i)*mHalfViewWidth/2+mHalfViewHeight);
            paths[0].lineTo(x, y);
            y = (float) (getLine2(i)*mHalfViewWidth/2+mHalfViewHeight);
            paths[1].lineTo(x, y);
        }
        canvas.drawPath(paths[0],mFillPaint);
        canvas.drawPath(paths[1],mFillPaint);
        mFillPaint.setStyle(Paint.Style.STROKE);
        mFillPaint.setStrokeWidth(3f);
        mFillPaint.setAlpha((int)(0.1*255));
        canvas.drawPath(baseLine,mFillPaint);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setXfermode(xfermode);
        mFillPaint.setAlpha((int)(0.7*255));
        mFillPaint.setShader(gradient1);
        canvas.drawRect(0,14*mHalfViewHeight/16,mWidth/3,18*mHalfViewHeight/16,mFillPaint);
        mFillPaint.setShader(gradient);
        canvas.drawRect(mWidth/3,12*mHalfViewHeight/16,2*mWidth/3,20*mHalfViewHeight/16,mFillPaint);
        mFillPaint.setShader(gradient2);
        canvas.drawRect(2*mWidth/3,14*mHalfViewHeight/16,mWidth,18*mHalfViewHeight/16,mFillPaint);
        mFillPaint.setXfermode(null);
        mFillPaint.setShader(null);
        mLinePaint.setAlpha((int)(0.7*255));
        mLinePaint.setColor(colors[0]);
        mLinePaint.setStrokeWidth(LINE_WIDTH[0]);
        canvas.drawPath(paths[0],mLinePaint);
        mLinePaint.setColor(colors[1]);
        mLinePaint.setStrokeWidth(LINE_WIDTH[1]);
        canvas.drawPath(paths[1],mLinePaint);
        canvas.restoreToCount(sc);


    }

}