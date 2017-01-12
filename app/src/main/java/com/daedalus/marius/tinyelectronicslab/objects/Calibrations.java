package com.daedalus.marius.tinyelectronicslab.objects;

import android.util.Log;
import android.util.SparseIntArray;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class Calibrations {

    private ArrayList<Calibration> calibrations;
    private int maxAmplitude = 0;

    public Calibrations() {
        calibrations = new ArrayList<Calibration>();
        newRange();
    }

    public void put(int x, int y, int rangeIndex) {
        if (rangeIndex < calibrations.size())
            calibrations.get(rangeIndex).dataSet.put(x, y);
    }

    public void newRange() {
        calibrations.add(new Calibration());
    }

    public void deleteRange(int rangeIndex) {
        if (rangeIndex < calibrations.size())
            calibrations.remove(rangeIndex);
    }


    public int calculateImpedance(int vOUTx, int rangeIndex) {
        return calibrations.get(rangeIndex).valueAt(vOUTx);

    }

    public void setAmplitude(int amplitude, int rangeIndex) {
        calibrations.get(rangeIndex).amplitude = (short) amplitude;
        if(rangeIndex == 0)
            maxAmplitude = amplitude;
    }

    public int getAmplitude(int rangeIndex) {
        return calibrations.get(rangeIndex).amplitude;
    }

    public String[] getRanges() {
        if (calibrations.size() > 0) {
            String[] ranges = new String[calibrations.size()];
            int i = 0;
            int size;
            SparseIntArray dataSet;
            for (Calibration calib : calibrations) {
                dataSet = calib.dataSet;
                size = dataSet.size();
                if (size > 0)
                    ranges[i] = "Range: " + Utilities.valueToString(dataSet.valueAt(size - 1)) + " - "
                            + Utilities.valueToString(dataSet.valueAt(0)) + " Ω";
                else
                    ranges[i] = "Range exists but not yet calibrated! Best to delete.";

                i += 1;
            }
            return ranges;
        }
        return null;
    }

    public int size(){
        return calibrations.size();
    }

    public int size(int i){
        if(i<calibrations.size())
            return calibrations.get(i).dataSet.size();
        return 0;
    }

    public int getMaxAmplitude(){
        return maxAmplitude;
    }

}
