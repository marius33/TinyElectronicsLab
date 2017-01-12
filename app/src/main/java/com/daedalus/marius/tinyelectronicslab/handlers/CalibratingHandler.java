package com.daedalus.marius.tinyelectronicslab.handlers;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.daedalus.marius.tinyelectronicslab.R;
import com.daedalus.marius.tinyelectronicslab.activities.WorkerActivity;
import com.daedalus.marius.tinyelectronicslab.objects.Utilities;

import java.lang.ref.WeakReference;

public class CalibratingHandler extends Handler {

    WeakReference<WorkerActivity> mTarget;

    //time delay to make sure signal stabilizes
    private static final long STABILIZING_DELAY = 5000000000L;
    //number of iterations for the approximation algorithm - after 13 iterations the difference in amplitude is 0
    private static final int MAXIMUM_ITERATIONS = 14;
    //the allowed error margin
    private static final int MAX_MARGIN = 150;

    private enum State {
        MEASURING_FIRST, AVERAGING, MEASURING_NOMINAL, STABILIZING, IDLE, FINDING_MAX_AMP
    }

    private State state;

    private int nominalResistanceValue;
    private int i;
    private int[] values;

    private int maxAmplitude = 0;
    private int dAmp = 0;
    private int oldMax = 0;
    private int dMax = 0;
    private int iteration = 0;
    private long nanoTime = 0;

    private boolean findingMaxDone;
    private boolean firstResistanceDone;

    private ProgressDialog progressDialog;

    public CalibratingHandler(final WorkerActivity activity, boolean append, boolean ranged) {
        super();
        mTarget = new WeakReference<WorkerActivity>(activity);
        state = State.IDLE;
        values = new int[50];
        i = 0;
        iteration = 0;
        dMax = 0;
        nominalResistanceValue = 0;

        if (append) {
            findingMaxDone = true;
            firstResistanceDone = true;

            final EditText nominalValue = new EditText(activity);
            nominalValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            if (progressDialog != null) progressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    activity);
            builder.setMessage(
                    "Please connect a known resistance between the input and output wires and then click \"OK\". "
                            + "Make sure you click \"OK\" only after you have connected the resistance!")
                    .setTitle("Calibration phase")
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog,
                                        int id) {
                                    nominalResistanceValue = Integer
                                            .parseInt(nominalValue
                                                    .getText()
                                                    .toString());
                                    activity.mReader.start();
                                    activity.mGenerator.start();
                                    state = State.STABILIZING;
                                    firstResistanceDone = true;
                                    createProgressBarDialog();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog,
                                        int id) {
                                    activity.notifyCalibrationDone();
                                }
                            })
                    .setView(nominalValue).create()
                    .show();
        } else {
            findingMaxDone = false;
            firstResistanceDone = false;
            if (!ranged) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(
                        "Please connect the input and output wires together. Only click \"OK\" after connecting them!")
                        //"Please connect the input and output wires together. Make sure they are connected before selecting \"OK\"!")
                        .setTitle("Calibration phase")
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        activity.mReader.start();
                                        activity.mGenerator.start();
                                        activity.mGenerator.amplitude = Short.MAX_VALUE;
                                        state = State.STABILIZING;
                                        createProgressBarDialog();
                                    }
                                })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        mTarget.get().notifyCalibrationDone();
                                    }
                                }).create().show();
            } else {
                final EditText nominalValue = new EditText(activity);
                nominalValue.setInputType(InputType.TYPE_CLASS_NUMBER);
                if (progressDialog != null) progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        activity);
                builder.setMessage(
                        "Please connect a known resistance between the input and output wires and then click \"OK\". "
                                + "Make sure you click \"OK\" only after you have connected the resistance!")
                        .setTitle("Calibration phase")
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            DialogInterface dialog,
                                            int id) {
                                        nominalResistanceValue = Integer
                                                .parseInt(nominalValue
                                                        .getText()
                                                        .toString());
                                        activity.mReader.start();
                                        activity.mGenerator.start();
                                        activity.mGenerator.amplitude = Short.MAX_VALUE;
                                        state = State.STABILIZING;
                                        createProgressBarDialog();
                                    }
                                })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            DialogInterface dialog,
                                            int id) {
                                        activity.notifyCalibrationDone();
                                    }
                                })
                        .setView(nominalValue).create()
                        .show();
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        final WorkerActivity target = mTarget.get();
        if (target != null) {

            switch (state) {
                case FINDING_MAX_AMP:

                    if (msg.what == 0) {
                        int max = getAmplitude((short[]) msg.obj);
                        if (oldMax == 0) {

                            if (max <= (target.calib.getMaxAmplitude() - MAX_MARGIN)) {
                                state = State.STABILIZING;
                                findingMaxDone = true;
                            } else {
                                //not related to other amplitude fields
                                maxAmplitude = max;
                                //////////////////////////////////////
                                dAmp = Short.MAX_VALUE / 2;

                                dMax = max;
                                oldMax = max;

                                target.mGenerator.amplitude -= dAmp;
                                state = State.STABILIZING;
                            }

                        } else {
                            if (iteration < MAXIMUM_ITERATIONS) {
                                dAmp /= 2;

                                Log.d("Iteration", Integer.toString(iteration));
                                Log.d("Max", Integer.toString(max));

                                if (Utilities.around(max, maxAmplitude, MAX_MARGIN)) {
                                    target.mGenerator.amplitude -= dAmp;
                                    Log.d("Generator Amplitude --", Short.toString(target.mGenerator.amplitude));
                                } else {
                                    target.mGenerator.amplitude += dAmp;
                                    Log.d("Generator Amplitude ++", Short.toString(target.mGenerator.amplitude));
                                }

                                //if(Utilities.around(oldMax, maxAmplitude, 100));
                                dMax = Math.abs(oldMax - max);
                                oldMax = max;

                                state = state.STABILIZING;
                            } else {
                                state = State.STABILIZING;
                                findingMaxDone = true;
                            }
                            iteration += 1;
                        }
                    }
                    //outputGeneratorId
                    else {

                    }
                    break;

                case MEASURING_NOMINAL:

                    if (msg.what == 0) {

                        state = State.IDLE;
                        progressDialog.dismiss();
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                target);
                        builder.setMessage(
                                "Would you like to add another entry to the LUT? (will increase precision)")
                                .setTitle("Calibration phase")
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(
                                                    DialogInterface dialog, int id) {
                                                final EditText nominalValue = new EditText(
                                                        target);
                                                nominalValue
                                                        .setInputType(InputType.TYPE_CLASS_NUMBER);
                                                AlertDialog.Builder builder = new AlertDialog.Builder(
                                                        target);
                                                builder.setMessage(
                                                        "Please connect a known resistance between the input and output wires and then click \"OK\". "
                                                                + "Make sure you click \"OK\" only after you have connected the resistance!")
                                                        .setTitle(
                                                                "Calibration phase")
                                                        .setPositiveButton(
                                                                R.string.ok,
                                                                new DialogInterface.OnClickListener() {
                                                                    public void onClick(
                                                                            DialogInterface dialog,
                                                                            int id) {
                                                                        createProgressBarDialog();
                                                                        nominalResistanceValue = Integer
                                                                                .parseInt(nominalValue
                                                                                        .getText()
                                                                                        .toString());
                                                                        state = State.AVERAGING;
                                                                    }
                                                                })
                                                        .setNegativeButton(
                                                                R.string.cancel,
                                                                new DialogInterface.OnClickListener() {
                                                                    public void onClick(
                                                                            DialogInterface dialog,
                                                                            int id) {
                                                                        target.notifyCalibrationDone();
                                                                    }
                                                                })
                                                        .setView(nominalValue)
                                                        .create().show();
                                            }
                                        })
                                .setNegativeButton(R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(
                                                    DialogInterface dialog, int id) {
                                                target.notifyCalibrationDone();
                                            }
                                        }).create().show();

                    } else {


                    }
                    break;

                case IDLE:
                    if (msg.what == 0) {


                    } else {


                    }
                    break;

                case AVERAGING:
                    if (msg.what == 0) {
                        values[i] = msg.arg1;
                        i++;
                        if (i == values.length) {
                            int v = 0;
                            for (int j : values) {
                                v += j;
                            }
                            v = v / i;
                            mTarget.get().putResistanceMeasurementPair(nominalResistanceValue, v);
                            i = 0;
                            if (firstResistanceDone)
                                state = State.MEASURING_NOMINAL;
                            else
                                state = State.MEASURING_FIRST;
                        }
                    }

                    break;

                case STABILIZING:
                    if (msg.what == 0) {
                        if (nanoTime == 0)
                            nanoTime = System.nanoTime();
                        else if (System.nanoTime() - nanoTime >= STABILIZING_DELAY) {
                            nanoTime = 0;
                            if (findingMaxDone) {
                                state = State.AVERAGING;
                            } else
                                state = State.FINDING_MAX_AMP;
                        }
                        removeMessages(0);
                    }

                    break;

                case MEASURING_FIRST:
                    state = State.IDLE;
                    final EditText nominalValue = new EditText(target);
                    nominalValue.setInputType(InputType.TYPE_CLASS_NUMBER);
                    progressDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            target);
                    builder.setMessage(
                            "Please connect a known resistance between the input and output wires and then click \"OK\". "
                                    + "Make sure you click \"OK\" only after you have connected the resistance!")
                            .setTitle("Calibration phase")
                            .setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog,
                                                int id) {
                                            nominalResistanceValue = Integer
                                                    .parseInt(nominalValue
                                                            .getText()
                                                            .toString());
                                            state = State.AVERAGING;
                                            firstResistanceDone = true;
                                            createProgressBarDialog();
                                        }
                                    })
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog,
                                                int id) {
                                            target.notifyCalibrationDone();
                                        }
                                    })
                            .setView(nominalValue).create()
                            .show();
                    break;


                default:
                    break;
            }
        }

    }

    private short getAmplitude(short[] buffer) {
        /*short amplitude = 0;
        int j = 0;
        int length = buffer.length - 1;
        for (int i = 1; i < length; i++) {
            if (buffer[i - 1] < buffer[i] && buffer[i] < buffer[i + 1]) {
                amplitude += buffer[i];
                j++;
            }
        }

        return (short) (amplitude / j);*/
        short k = 0;
        for (short s : buffer)
            if (s > k)
                k = s;
        return k;
    }

    private void createProgressBarDialog() {
        progressDialog = new ProgressDialog(mTarget.get());
        progressDialog.setTitle("Calibratin");
        progressDialog.setMessage("Please wait!");
        progressDialog.setIndeterminate(true);
        progressDialog.show();

    }

}
