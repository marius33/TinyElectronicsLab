package com.daedalus.marius.tinyelectronicslab.activities;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.daedalus.marius.tinyelectronicslab.R;
import com.daedalus.marius.tinyelectronicslab.objects.FixedSizeQueue;

import java.util.ListIterator;

/**
 * Created by Marius on 13/01/2017.
 */

public class CalibratingActivity extends WorkerActivity {

    public static final int AVERAGE_PEAKS_MARGIN = 2;
    public static final double RMS_MARGIN = 0.001;
    private static final short DELTA_PEAKS = Short.MAX_VALUE / 100;
    private static final double THEORETICAL_RMS = 1 / Math.sqrt(2);

    private int resistance = -1;

    Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        final EditText resistanceTextInput = (EditText) findViewById(R.id.editText_resistance);
        Button calibrateButton = (Button) findViewById(R.id.button_calibrate_now);
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resistance = Integer.parseInt(resistanceTextInput.getText().toString());

                toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.setDuration(Toast.);
                toast.setView(layout);
                toast.show();

            }
        });


    }

    @Override
    public void notifyDataReceived(FixedSizeQueue values) {

        if (resistance > 0) {
            int peakCounter = 0;
            int averagePeaks = mGenerator.getFrequency() * mValues.size() / mReader.getSampleRate();
            int samplesNr = mValues.size();
            ListIterator<Short> it = mValues.listIterator(samplesNr - 1);
            short valp2 = it.previous();
            short valp1 = it.previous();
            short val0 = it.previous();
            short valm1 = it.previous();
            short valm2 = 0;

            it = mValues.listIterator(0);

            short firstPeak = -1;
            short lastPeak = -1;

            long sum = 0;
            short val;
            while (it.hasNext()) {
                val = it.next();
                sum += val * val;

                valm2 = valm1;
                valm1 = val0;
                val0 = valp1;
                valp1 = valp2;
                valp2 = val;
                if (valm2 < valm1 && valm1 < val0 && val0 > valp1 && valp1 > valp2) {
                    peakCounter++;
                    if (firstPeak == -1)
                        firstPeak = val0;
                    lastPeak = val0;
                }

            }

            double vIn = Math.sqrt(sum / mValues.size());

            //should be close to 1 / sqrt(2)
            double relativeRMS = vIn / lastPeak;
            //check to see if signal is stabilized
            int deltaPeak = Math.abs(firstPeak - lastPeak);
            if (deltaPeak < DELTA_PEAKS) {
                //check if average power is according to
                double deltaRMS = Math.abs(relativeRMS - THEORETICAL_RMS);
                if (deltaRMS < RMS_MARGIN) {
                    putCalibration(resistance, (int) vIn, mGenerator.amplitude);
                } else {
                    mGenerator.setAmplitude(mGenerator.getAmpltitude()-DELTA_AMPLITUDE);
                    mHandler.removeMessages(0);
                }
            }
        }

    }

    private void putCalibration(int rx, int rms, int amplitude) {

    }

}
