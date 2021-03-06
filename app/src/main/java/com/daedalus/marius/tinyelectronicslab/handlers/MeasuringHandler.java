package com.daedalus.marius.tinyelectronicslab.handlers;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

import com.daedalus.marius.tinyelectronicslab.activities.WorkerActivity;


public class MeasuringHandler extends Handler {
    WeakReference<WorkerActivity> mTarget;

    public MeasuringHandler(WorkerActivity activity) {
        mTarget = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        final WorkerActivity target = mTarget.get();
        if (target != null) {
                target.onDataReceived((short[]) msg.obj);
        }
    }
}
