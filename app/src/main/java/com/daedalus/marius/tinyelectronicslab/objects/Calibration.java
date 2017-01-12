package com.daedalus.marius.tinyelectronicslab.objects;

import android.util.SparseIntArray;

/**
 * Created by Marius on 14.03.2015.
 */
public class Calibration {

    public static int OUT_OF_RANGE_LOW = -1;
    public static int OUT_OF_RANGE_HIGH = -2;

    private SparseIntArray dataSet;
    private short amplitude;

    Calibration() {
        dataSet = new SparseIntArray();
        amplitude = 1000;
    }

    public int valueAt(int vOUTx) {

        int key2;
        int key1;
        for (int i = 1; i < dataSet.size(); i++) {
            key2 = dataSet.keyAt(i);
            key1 = dataSet.keyAt(i - 1);
            if (key2 > vOUTx && key1 < vOUTx) {
                int vOUT2 = key2;
                int vOUT1 = key1;
                int r1 = dataSet.get(vOUT1);
                int r2 = dataSet.get(vOUT2);

                int r0 = ((vOUT1 * r1) - vOUT2 * r2) / (vOUT2 - vOUT1);

                return ((vOUT1 * r1) + r0 * (vOUT1 - vOUTx)) / vOUTx;
            }
        }

        if (vOUTx < dataSet.keyAt(0))
            return OUT_OF_RANGE_HIGH;

        return OUT_OF_RANGE_LOW;
    }

    public int keyAt(int rx) {

        int value1;
        int value2;
        for (int i = 1; i < dataSet.size(); i++) {
            value1 = dataSet.valueAt(i - 1);
            value2 = dataSet.valueAt(i);
            if (value2 < rx && value1 > rx) {
                int vOUT2 = dataSet.keyAt(i);
                int vOUT1 = dataSet.keyAt(i - 1);

                int r0 = ((vOUT1 * value1) - vOUT2 * value2) / (vOUT2 - vOUT1);

                return vOUT1 * (value1 + r0) / (value2 + r0);
            }
        }

        if (rx > dataSet.keyAt(0))
            return OUT_OF_RANGE_HIGH;

        return OUT_OF_RANGE_LOW;
    }

}
