package com.liberty.waveview.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.liberty.waveview.R;

/**
 * Created by liberty on 2017/9/8.
 */

public class XfermodeTest extends View {
    private Paint mPaint;

    public XfermodeTest(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_HARDWARE,null);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setFilterBitmap(true);
//        mPaint.setStyle(Paint.Style.STROKE);
//        mPaint.setStrokeWidth(3f);
        mPaint.setStyle(Paint.Style.FILL);
    }

    private Bitmap makeSrc(){
        Bitmap bitmap = Bitmap.createBitmap(600,600, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        canvas.drawCircle(200,200,200,paint);
        return bitmap;
    }

    private Bitmap makeDst(){
        Bitmap bitmap = Bitmap.createBitmap(600,600, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.YELLOW);
        canvas.drawRect(200,200,600,600,paint);
        return bitmap;
    }

    private static final double commonParam =0.75*Math.PI;

    private double getLine1(float x){
        Double a = Math.sin(commonParam*x-0.5*Math.PI);
        Double d = 4+Math.pow(x,4);
        Double b = (4/d);
        Double c = Math.pow(b,2.5);
        Double y = 0.5*c*a;
        Log.d("xxx","getLine1:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    private double getLine2(float x){
        Double a = Math.sin(commonParam*x+0.5*Math.PI);
        Double b = (4/(4+Math.pow(x,4)));
        Double c = Math.pow(b,2.5);
        Double y = 0.5*c*a;
//        Log.d(TAG,"getLine2:"+a+" "+b+" "+c+" "+y);
        return y;
    }

    private int mWidth,mHeight,mHalfWidth,mHalfHeight;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mHalfHeight = mHeight/2;
        mHalfWidth = mWidth/2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        mPaint.setColor(Color.BLUE);
//        canvas.drawCircle(300,300,150,mPaint);
//        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//        mPaint.setColor(Color.YELLOW);
//        canvas.drawRect(300,300,600,600,mPaint);
//        Bitmap src = makeSrc();
//        canvas.drawBitmap(src,0,0,mPaint);
        mPaint.setColor(Color.BLUE);
        Path p1 = new Path();
        Path p2 = new Path();
        p1.moveTo(0,mHalfHeight);
        p2.moveTo(0,mHalfHeight);

        float K = 2.0f;
        float x,y;
        for (float i=-K;i<=K;i+=0.01){
            x = mHalfWidth*((i+K)/K);
            y = (float) (getLine1(i)*mHalfHeight+mHalfHeight);
            p1.lineTo(x, y);
            y = (float) (getLine2(i)*mHalfHeight+mHalfHeight);
            p2.lineTo(x, y);
        }
        canvas.drawPath(p1,mPaint);
        canvas.drawPath(p2,mPaint);
        Path p3 = new Path();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3f);
        p3.moveTo(0,mHalfHeight);
        p3.lineTo(mWidth,mHalfHeight);
        canvas.drawPath(p3,mPaint);
//        canvas.drawCircle(200,200,200,mPaint);
//        canvas.drawRect(0,0,400,400,mPaint);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//        Bitmap dst = makeDst();
//        canvas.drawBitmap(dst,0,0,mPaint);
        LinearGradient gradient = new LinearGradient(0,mHeight/4,mWidth,3*mHeight/4,getResources().getColor(R.color.colorAccent),getResources().getColor(R.color.colorPrimary), Shader.TileMode.REPEAT);
        mPaint.setShader(gradient);
//        mPaint.setColor(Color.YELLOW);
//        canvas.drawCircle(200,200,200,mPaint);
        canvas.drawRect(0,mHeight/4,mWidth,3*mHeight/4,mPaint);
    }
}
