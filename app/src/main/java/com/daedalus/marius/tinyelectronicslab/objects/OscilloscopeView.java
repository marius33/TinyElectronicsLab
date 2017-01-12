package com.daedalus.marius.tinyelectronicslab.objects;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

/**
 * Created by Marius on 15.02.2015.
 */
public class OscilloscopeView extends View implements GestureDetector.OnGestureListener {

    private int sampleRate = 48000;

    private Paint gridPaint;
    private Paint pathPaint;
    private Paint edgePaint;
    private Paint triggerPaint;

    private short[] samples;
    private float[] points;

    private int mHeight;
    private int mWidth;

    //horizontal deflection in microseconds per division
    private int hDeflection;

    //vertical deflection in raw value per division
    private short vDeflection;

    private Path mPath;

    private int offsetY;
    private int offsetX;

    private GestureDetectorCompat mGestureDetector;

    private int hPosition;
    private short triggerLevel;

    private short max = Short.MAX_VALUE;


    public OscilloscopeView(Context context) {
        super(context);
        init(context);
    }

    public OscilloscopeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OscilloscopeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int desiredHeight = (int) (width * 0.8);

        if (height >= desiredHeight)
            height = Utilities.getMeasurement(heightMeasureSpec, desiredHeight);
        else
            width = (int) (height * 1.25);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
        mWidth = w;
        offsetX = (w % 10) / 2;
        offsetY = (h % 8) / 2;
        edgePaint.setStrokeWidth(offsetX);


    }

    public void setSamples(short[] samples) {
        this.samples = samples;

        invalidate();

    }

    public void setSampleRate(int sr) {
        sampleRate = sr;
        int nOfPoints = (hDeflection * sampleRate) / 100000;
        points = new float[nOfPoints];
        invalidate();
    }

    public void setHorizontalDeflection(int defl) {
        hDeflection = defl;
        int nOfPoints = (hDeflection * sampleRate) / 100000;
        points = new float[nOfPoints * 2];
        invalidate();

    }

    public void setVerticalDeflection(int vdefl) {
        max = (short) vdefl;
        invalidate();
    }

    private void init(Context context) {

        setHorizontalDeflection(250);
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.BLACK);

        edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setColor(Color.BLACK);

        pathPaint = new Paint();
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setColor(Color.BLUE);
        pathPaint.setStrokeWidth(3);

        triggerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        triggerPaint.setStrokeWidth(1);
        triggerPaint.setColor(Color.GRAY);

        mPath = new Path();

        mGestureDetector = new GestureDetectorCompat(context, this);

        triggerLevel = 0;
        hPosition = 0;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the grid
        drawGrid(canvas, mWidth / 10, mHeight / 8, offsetX + mWidth / 10, offsetY + mHeight / 8);

        drawPoints(canvas);

        drawTriggerLevel(canvas);
        drawHorizontalPosition(canvas);

    }

    private void drawGrid(Canvas canvas, int dx, int dy, int x0, int y0) {

        // Draw vertical lines
        for (int x = x0 + offsetX / 2; x < mWidth; x += dx)
            canvas.drawLine(x, offsetY, x, mHeight - offsetY / 2, gridPaint);

        // Draw horizontal lines
        for (int y = y0 + offsetY / 2; y < mHeight; y += dy)
            canvas.drawLine(offsetX, y, mWidth - offsetX / 2, y, gridPaint);

        // Draw edges
        canvas.drawLine(0, 0, 0, mHeight, edgePaint);
        canvas.drawLine(mWidth, 0, mWidth, mHeight, edgePaint);
        canvas.drawLine(0, 0, mWidth, 0, edgePaint);
        canvas.drawLine(0, mHeight, mWidth, mHeight, edgePaint);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        ViewParent parent = getParent();

        parent.requestDisallowInterceptTouchEvent(true);

        return super.onTouchEvent(event) || mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        int dHPos = (int) (distanceX / mWidth * points.length / 2);
        if (Utilities.around(hPosition - dHPos, 0, points.length / 4))
            hPosition -= dHPos;
        triggerLevel += (2 * distanceY / mHeight * max);
        invalidate();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        hPosition = 0;
        triggerLevel = 0;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
        return true;
    }


    private void drawTriggerLevel(Canvas canvas) {
        float startY = mHeight / 2 - triggerLevel * mHeight / (2 * max);

        canvas.drawLine(0, startY, mWidth, startY, triggerPaint);
    }

    private void drawHorizontalPosition(Canvas canvas) {
        float startX = mWidth / 2 + mWidth / 2 * hPosition / (points.length / 4);

        canvas.drawLine(startX, 0, startX, mHeight, triggerPaint);
    }

    private void drawPoints(Canvas canvas) {
        if (samples != null) {
            float dx = 2 * mWidth / points.length;
            boolean done = false;
            for (int j = points.length / 4 + hPosition; j < samples.length; j++) {
                if (samples[j - 1] < triggerLevel && samples[j] > triggerLevel) {
                    j -= points.length / 4;
                    for (int i = 0; (i < points.length / 2) && (i + j) < samples.length; i++) {
                        points[i * 2] = i * dx;
                        points[i * 2 + 1] = (-samples[j + i] * mHeight) / (2 * max) + mHeight / 2;
                    }
                    done = true;
                    break;
                }
            }

            if (!done)
                for (int i = 0; (i < points.length / 2) && (i) < samples.length; i++) {
                    points[i * 2] = i * dx;
                    points[i * 2 + 1] = (-samples[i] * mHeight) / (2 * max) + mHeight / 2;
                }

            mPath.reset();
            mPath.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2)
                mPath.lineTo(points[i], points[i + 1]);

            canvas.drawPath(mPath, pathPaint);
        }


    }
}
