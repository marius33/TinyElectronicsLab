package com.daedalus.marius.tinyelectronicslab.objects;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.SparseIntArray;
import android.view.View;

/**
 * Created by Marius on 18.02.2015.
 */
public class Utilities {

    public static final float triangleSlope = (float) (2/Math.PI);

    public static int getMeasurement(int measureSpec, int contentSize) {
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);
        int resultSize = 0;
        switch (specMode) {
            case View.MeasureSpec.UNSPECIFIED:
                resultSize = contentSize;
                break;
            case View.MeasureSpec.AT_MOST:
                resultSize = Math.min(contentSize, specSize);
                break;
            case View.MeasureSpec.EXACTLY:
                resultSize = specSize;
                break;
        }

        return resultSize;
    }

    public static double[] getXValues(SparseIntArray array){
        int size = array.size();
        double[] xs = new double[size];
        for(int i=0; i<size; i++)
            xs[i] = array.keyAt(i);

        return xs;

    }

    public static double[] getYValues(SparseIntArray array){
        int size = array.size();
        double[] ys = new double[size];
        for(int i=0; i<size; i++)
            ys[i] = array.valueAt(i);

        return ys;
    }

    public static double linearInterpolate(double[] points, double x){
        double m = (points[3]-points[1])/(points[2]-points[0]);
        double b = points[3]-m*points[2];
        return m*x + b;
    }

    public static boolean around(int a, int b, int range) {
        return (a <= b + range && a >= b - range);
    }

    public static float triangle(float angle){
        float slope = (float) (2/Math.PI);
        if(angle<=Math.PI)
            return angle*slope-1;
        else
            return 3-angle*slope;

    }

    public static float square(float angle){
        if(angle<=Math.PI)
            return 1;
        return -1;
    }

    public static String valueToString(float value){

        if (value > 1e6f) {
            value = value / 1e6f;
            return String.format("%.3f", value) + "M";
        } else if (value > 1e3f) {
            value = value / 1e3f;
            return String.format("%.3f", value) + "K";
        } else if(value>=1)
            return String.format("%.3f", value);
        else if(value>1e-3f){
            value = value * 1e3f;
            return String.format("%.3f", value) + "m";
        }else if(value>1e-6f){
            value = value * 1e6f;
            return String.format("%.3f", value) + "µ";
        }else if(value>1e-9f){
            value = value * 1e9f;
            return String.format("%.3f", value) + "n";
        }else{
            value = value * 1e12f;
            return String.format("%.3f", value) + "p";
        }
    }

}

