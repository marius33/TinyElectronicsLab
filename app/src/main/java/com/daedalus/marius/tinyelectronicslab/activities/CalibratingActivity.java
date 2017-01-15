package com.daedalus.marius.tinyelectronicslab.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.daedalus.marius.tinyelectronicslab.R;
import com.daedalus.marius.tinyelectronicslab.objects.FixedSizeQueue;

import java.util.ListIterator;

/**
 * Created by Marius on 13/01/2017.
 */

public class CalibratingActivity extends WorkerActivity {

    public static final int AVERAGE_PEAKS_MARGIN = 2;

    private int resistance = -1;
    private int vIn = -1;
    private int amplitude = -1;

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


            }
        });


    }

    @Override
    public void notifyDataReceived(FixedSizeQueue values) {

        if(resistance>0) {
            int peakCounter = 0;
            int averagePeaks = mGenerator.getFrequency()*mValues.size()/mReader.getSampleRate();
            ListIterator<Short> it = mValues.listIterator(0);
            int valm2 = 0;
            int valm1 = 0;
            int val0 = 0;
            int valp1 = 0;
            int valp2 = 0;

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
                if(valm2<valm1 && valm1<val0 && val0>valp1 && valp1>valp2)
                    peakCounter++;

            }
            vIn = (int) Math.sqrt(sum / mValues.size());
            if((averagePeaks-AVERAGE_PEAKS_MARGIN)<peakCounter && (averagePeaks+AVERAGE_PEAKS_MARGIN)>peakCounter){
                if()
            }
        }

    }

}
