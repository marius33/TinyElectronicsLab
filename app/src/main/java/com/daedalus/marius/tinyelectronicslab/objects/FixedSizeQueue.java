package com.daedalus.marius.tinyelectronicslab.objects;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by Marius on 19.03.2015.
 */
public class FixedSizeQueue<E> {

    private LinkedList<E> values;
    private final int size;

    public FixedSizeQueue(int s) {
        values = new LinkedList<>();
        size = s;
        for(int i=0; i<s; i++)
            values.add(null);
    }

    public ListIterator<E> listIterator(int index){
        return values.listIterator(index);
    }

    public int size(){
        return size;
    }

    public void add(E value) {
        values.removeFirst();
        values.addLast(value);
    }

    public void addAll(E[] values){
        if(values.length>size){
            int len = values.length;
            for(int i=len-size; i<len; i++)
                add(values[i]);
        }
        else
            for(E e : values)
                add(e);
    }

}
