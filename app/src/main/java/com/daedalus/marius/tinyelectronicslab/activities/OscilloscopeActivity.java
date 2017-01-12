package com.daedalus.marius.tinyelectronicslab.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.daedalus.marius.tinyelectronicslab.R;
import com.daedalus.marius.tinyelectronicslab.objects.OscilloscopeView;

/**
 * Created by Marius on 02.03.2015.
 */
public class OscilloscopeActivity extends WorkerActivity {

    private OscilloscopeView oView;

    private ToggleButton genRunStop;
    private ToggleButton oscRunStop;
    private EditText genFrequency;
    private Button setFrequency;
    private Button autoSet;
    private Spinner genFunction;
    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPager = new ViewPager(this);

        mPager.setAdapter(new MPagerAdapter());

        setContentView(mPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.oscilloscope_menu, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_activity_lcr) {
            mReader.stop();
            mGenerator.stop();
            Intent intent = new Intent(this, MultimeterActivity.class);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataReceived(int vOUTx, short[] buffer) {
        oView.setSamples(buffer);

    }

    @Override
    public void putResistanceMeasurementPair(int rx, int vOUTx) {
        calib.put(vOUTx, rx, 0);
    }

    private class MPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = container.getChildAt(position);
            if (position == 0) {
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.oscilloscope, null);

                    oView = (OscilloscopeView) v.findViewById(R.id.oscilloscopeView);
                    oView.setSampleRate(mReader.getSampleRate());

                    oscRunStop = (ToggleButton) v.findViewById(R.id.osciRunStop);
                    oscRunStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            if (b)
                                mReader.start();
                            else
                                mReader.pause();
                        }
                    });

                    Spinner hDefl = (Spinner) v.findViewById(R.id.hDeflSpinner);
                    hDefl.setSelection(2);
                    hDefl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            switch (i) {
                                case 0:
                                    oView.setHorizontalDeflection(50);
                                    break;
                                case 1:
                                    oView.setHorizontalDeflection(100);
                                    break;
                                case 2:
                                    oView.setHorizontalDeflection(250);
                                    break;
                                case 3:
                                    oView.setHorizontalDeflection(500);
                                    break;
                                case 4:
                                    oView.setHorizontalDeflection(1000);
                                    break;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    /*Spinner vDefl = (Spinner) v.findViewById(R.id.vDeflSpinner);
                    vDefl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            switch(i){
                                case 0: oView.setHorizontalDeflection(Short.MAX_VALUE);
                                    break;
                                case 1: oView.setHorizontalDeflection(Short.MAX_VALUE/2);
                                    break;
                                case 2: oView.setHorizontalDeflection(Short.MAX_VALUE/5);
                                    break;
                                case 3: oView.setHorizontalDeflection(Short.MAX_VALUE/10);
                                    break;
                                case 4: oView.setHorizontalDeflection(Short.MAX_VALUE/100);
                                    break;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });*/
                }


            } else {
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.function_generator, null);

                    genRunStop = (ToggleButton) v.findViewById(R.id.genRunStop);
                    genRunStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            if (b)
                                mGenerator.start();
                            else
                                mGenerator.pause();
                        }
                    });


                    final SeekBar genAmplitude = (SeekBar) v.findViewById(R.id.amplitudeBar);
                    genAmplitude.setMax(Short.MAX_VALUE);
                    genAmplitude.setProgress(mGenerator.amplitude);
                    genAmplitude.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            mGenerator.amplitude = (short) i;
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });

                    genFrequency = (EditText) v.findViewById(R.id.frequencyEditText);
                    genFrequency.setText(Integer.toString(mGenerator.getFrequency()));

                    setFrequency = (Button) v.findViewById(R.id.frequencyGo);
                    setFrequency.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String value = genFrequency.getText().toString();
                            if (value.length() == 0)
                                value = "0";
                            mGenerator.setFrequency(Integer.parseInt(value));
                            //calib.setAmplitude(genAmplitude.getProgress());
                        }
                    });

                    genFunction = (Spinner) v.findViewById(R.id.functionSpinner);
                    genFunction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            mGenerator.function = i;
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                }
            }

            container.addView(v);
            return v;

        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }


}
