package com.daedalus.marius.tinyelectronicslab.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.daedalus.marius.tinyelectronicslab.R;
import com.daedalus.marius.tinyelectronicslab.handlers.CalibratingHandler;
import com.daedalus.marius.tinyelectronicslab.objects.Calibration;
import com.daedalus.marius.tinyelectronicslab.objects.Calibrations;
import com.daedalus.marius.tinyelectronicslab.objects.FixedSizeQueue;
import com.daedalus.marius.tinyelectronicslab.objects.Utilities;

public class MultimeterActivity extends WorkerActivity {

    private int currentFunctionId;
    private TextView value;
    private TextView unit;
    private RadioGroup rGroup;

    //private short[] mBuffer;
    //private int vRMS = 0;

    private FixedSizeQueue rmsInputs;
    private FixedSizeQueue freqs;

    private int outOfRangeCounter;
    private float oldZx;
    private ToggleButton runStop;
    private ToggleButton autoHold;
    private ToggleButton continuityCheck;

    private boolean continuityColorChanged = false;
    private boolean continuityHasRunnable = false;
    private Runnable colourFlashRunnable;
    private Drawable continuityBackgroundDrawable;

    private Spinner rangeSpinner;


    //private float[] impedances;
    //private float[] frequencies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multimeter);

        value = (TextView) findViewById(R.id.textViewValue);
        unit = (TextView) findViewById(R.id.textViewUnit);
        //value.setTypeface(Typeface.createFromAsset(this.getAssets(),
        //      "digital-7.ttf"));

        //range = (TextView) findViewById(R.id.textViewRange);
        //range.setText("Range: " + calib.getMinImpedance() + " - " + calib.getMaxImpedance());

        //impedances = new float[20];
        //frequencies = new float[20];

        rmsInputs = new FixedSizeQueue(30);
        freqs = new FixedSizeQueue(30);

        runStop = (ToggleButton) findViewById(R.id.runStop);
        runStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mGenerator.start();
                    mReader.start();
                    if (keepScreenOn)
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                } else {
                    mGenerator.pause();
                    mReader.pause();
                    if (keepScreenOn)
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                }

            }
        });

        autoHold = (ToggleButton) findViewById(R.id.autoHold);

        continuityCheck = (ToggleButton) findViewById(R.id.continuity);
        continuityCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!b)
                    if (continuityHasRunnable) {
                        continuityCheck.removeCallbacks(colourFlashRunnable);
                        continuityHasRunnable = false;
                        continuityCheck.setBackgroundDrawable(continuityBackgroundDrawable);
                    }

            }
        });
        continuityBackgroundDrawable = continuityCheck.getBackground();

        colourFlashRunnable = new Runnable() {

            @Override
            public void run() {
                if (continuityColorChanged)
                    continuityCheck.setBackgroundColor(Color.YELLOW);
                else
                    continuityCheck.setBackgroundDrawable(continuityBackgroundDrawable);
                continuityColorChanged = !continuityColorChanged;
                continuityCheck.postDelayed(this, 100);
            }
        };

        rGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        rGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                currentFunctionId = checkedId;
                switch (checkedId) {
                    case R.id.radioOHM:
                        unit.setText("Ω");
                        break;
                    case R.id.radioFARAD:
                        unit.setText("F");
                        break;
                    case R.id.radioHENRY:
                        unit.setText("H");
                        break;
                    case R.id.radioHERZ:
                        unit.setText("Hz");
                        break;
                }
                makeReading(rmsInputs.getAverage(), freqs.getAverage());
            }
        });
        rGroup.check(R.id.radioOHM);

        currentFunctionId = R.id.radioOHM;
        unit.setText("Ω");

        rangeSpinner = (Spinner) findViewById(R.id.rangeSpinner);
        refreshRanges(0);
        rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mGenerator.amplitude = (short) calib.getAmplitude(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        rangeSpinner.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (rangeSpinner.getSelectedItemPosition() != 0) {
                    new AlertDialog.Builder(MultimeterActivity.this)
                            .setTitle("Warning!").setMessage("You are about to delete the current range calibration. Continue?").
                            setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            }).setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            calib.deleteRange(rangeSpinner.getSelectedItemPosition());
                            rangeSpinner.setAdapter(new ArrayAdapter<String>(MultimeterActivity.this,
                                    android.R.layout.simple_spinner_item, calib.getRanges()));
                        }
                    }).create().show();
                }
                return true;
            }
        });

        /*rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                makeReading(vRMS, mBuffer);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });*/

        outOfRangeCounter = 0;
        oldZx = 0;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.lcr_menu, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {

            case R.id.action_activity_osc:
                mReader.stop();
                mGenerator.stop();
                Intent intent = new Intent(this, OscilloscopeActivity.class);
                startActivity(intent);
                finish();
                return true;

            case R.id.action_add_value:
                setHandler(new CalibratingHandler(this, true, false));
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                return true;

            case R.id.action_calibrate:
                setHandler(new CalibratingHandler(this, false, false));
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                calib = new Calibrations();
                refreshRanges(calib.size()-1);
                return true;

            case R.id.action_ranged_calibrate:
                setHandler(new CalibratingHandler(this, false, true));
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                calib.newRange();
                refreshRanges(calib.size()-1);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataReceived(int vOUTx, short[] buffer) {
        if (autoHold.isChecked()) {
            if (Utilities.around(vOUTx, rmsInputs.getAverage(), Short.MAX_VALUE / 5)) {

                freqs.put(calculateFrequency(buffer));
                rmsInputs.put(vOUTx);

                makeReading(rmsInputs.getAverage(), freqs.getAverage());
            }
        } else {

            freqs.put(calculateFrequency(buffer));
            rmsInputs.put(vOUTx);

            makeReading(rmsInputs.getAverage(), freqs.getAverage());
        }

    }

    @Override
    public void putResistanceMeasurementPair(int rx, int vOUTx) {
        int rangePosition = rangeSpinner.getSelectedItemPosition();
        if(rangePosition == -1)
            rangePosition = 0;
        calib.put(vOUTx, rx, rangePosition);
    }

    private void makeReading(int vOUTx, int freq) {
        String value = "NO VALUE";
        float zx = calculateImpedance(vOUTx);

        if (zx == Calibration.OUT_OF_RANGE_HIGH && outOfRangeCounter < 10) {
            outOfRangeCounter++;
            zx = oldZx;
        } else if (zx == Calibration.OUT_OF_RANGE_LOW && outOfRangeCounter < 10) {
            outOfRangeCounter++;
            zx = oldZx;
        } else {
            outOfRangeCounter = 0;
            oldZx = zx;
        }

        switch (currentFunctionId) {

            case R.id.radioOHM:
                if (zx == Calibration.OUT_OF_RANGE_HIGH)
                    value = "O.O.R. - HIGH";
                else if (zx == Calibration.OUT_OF_RANGE_LOW)
                    value = "O.O.R. - LOW";
                else
                    value = Utilities.valueToString(zx);
                break;

            case R.id.radioFARAD:
                if (zx == Calibration.OUT_OF_RANGE_HIGH)
                    value = "O.O.R. - HIGH";
                else if (zx == Calibration.OUT_OF_RANGE_LOW)
                    value = "O.O.R. - LOW";
                else
                    value = Utilities.valueToString(calculateCapacitance(zx, freq));
                break;

            case R.id.radioHENRY:
                if (zx == Calibration.OUT_OF_RANGE_HIGH)
                    value = "O.O.R. - HIGH";
                else if (zx == Calibration.OUT_OF_RANGE_LOW)
                    value = "O.O.R. - LOW";
                else
                    value = Utilities.valueToString(calculateInductance(zx, freq));
                break;

            case R.id.radioHERZ:
                value = Utilities.valueToString(freq);
                break;
        }

        this.value.setText(value);

    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        runStop.setChecked(false);
        runStop.setEnabled(enabled);

    }

    private float calculateImpedance(int vOUTx) {

        //Get calculated impedance
        float value = calib.calculateImpedance(vOUTx, rangeSpinner.getSelectedItemPosition());

        if (continuityCheck.isChecked()) {
            if (Utilities.around((int) value, 20, 20) || value  == Calibration.OUT_OF_RANGE_LOW) {
                if (!continuityHasRunnable) {
                    continuityCheck.postDelayed(colourFlashRunnable, 200);
                    continuityHasRunnable = true;
                }
            } else {
                if (continuityHasRunnable) {
                    continuityCheck.removeCallbacks(colourFlashRunnable);
                    continuityHasRunnable = false;
                    continuityCheck.setBackgroundDrawable(continuityBackgroundDrawable);
                }
            }
        }


        //Average the impedance with the last few values, so it stays more stable
        /*float impedance = value;
        for (int i = 0; i < impedances.length; i++) {
            impedance += impedances[i];
            if (i < impedances.length - 1)
                impedances[i] = impedances[i + 1];
            else
                impedances[i] = value;
        }

        return impedance / (impedances.length + 1);*/
        return value;
    }

    private float calculateInductance(float rx, float frequency) {
        return (float) (rx / (2 * Math.PI * frequency));
    }

    private float calculateCapacitance(float rx, float frequency) {
        return (float) (1 / (rx * 2 * Math.PI * frequency));
    }

    private int calculateFrequency(short[] buffer) {
        double t01 = -1;
        double t02 = -1;
        int t0Counter = -1;
        int sampleRate = mReader.getSampleRate();
        //double dt = 1.0 / sampleRateInHz;

        for (int i = 1; i < buffer.length; i++) {
            if (buffer[i - 1] <= 0 && buffer[i] >= 0 && buffer[i - 1] != buffer[i]) {
                if (t01 == -1) {
                    /*double t1 = (i - 1) * dt;
                    double t2 = i * dt;
                    short dy = (short) (buffer[i] - buffer[i - 1]);
                    double a = dy / dt;
                    double b = buffer[i] - a * t2;
                    t01 = t1 - (b / a);*/
                    t01 = (double) i / sampleRate;
                }
                t0Counter += 1;
            }
        }

        for (int i = buffer.length - 1; i >= 1; i--) {
            if (buffer[i - 1] <= 0 && buffer[i] >= 0 && buffer[i - 1] != buffer[i]) {
                /*double t1 = (i - 1) * dt;
                double t2 = i * dt;
                short dy = (short) (buffer[i] - buffer[i - 1]);
                double a = dy / dt;
                double b = buffer[i] - a * t2;
                t02 = t1 - (b / a);*/
                t02 = (double) i / sampleRate;
                break;
            }
        }

        float value = (float) (t0Counter / (t02 - t01));

        //Averages the frequency values, so it stays more stable
        /*float frequency = value;
        for (int i = 0; i < frequencies.length; i++) {
            frequency += frequencies[i];
            if (i < frequencies.length - 1)
                frequencies[i] = frequencies[i + 1];
            else
                frequencies[i] = value;
        }

        return frequency / (frequencies.length + 1);*/
        return (int) value;

    }

    @Override
    public void notifyCalibrationDone() {
        calib.setAmplitude(mGenerator.amplitude, rangeSpinner.getSelectedItemPosition());
        super.notifyCalibrationDone();
        refreshRanges(calib.size()-1);
    }

    private void refreshRanges(int i){
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, calib.getRanges());
        rangeSpinner.setAdapter(adapter);
        rangeSpinner.setSelection(i);
    }

}