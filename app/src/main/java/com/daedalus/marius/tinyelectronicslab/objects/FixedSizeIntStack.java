package com.daedalus.marius.tinyelectronicslab.objects;

/**
 * Created by Marius on 19.03.2015.
 */
public class FixedSizeIntStack {

    private int[] array;
    private int size;


    public FixedSizeIntStack(int s) {
        array = new int[s];
        size = s;
    }

    public void put(int value) {
        for (int i = 0; i < size - 1; i++)
            array[i] = array[i + 1];
        array[size - 1] = value;
    }

    public int getAverage() {
        int sum = 0;
        for (int i : array)
            sum += i;

        return sum / size;
    }
}
