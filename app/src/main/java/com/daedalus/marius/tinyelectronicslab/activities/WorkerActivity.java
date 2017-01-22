package com.daedalus.marius.tinyelectronicslab.activities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.daedalus.marius.tinyelectronicslab.R;
import com.daedalus.marius.tinyelectronicslab.handlers.CalibratingHandler;
import com.daedalus.marius.tinyelectronicslab.handlers.MeasuringHandler;
import com.daedalus.marius.tinyelectronicslab.objects.Calibrations;
import com.daedalus.marius.tinyelectronicslab.objects.FixedSizeQueue;
import com.daedalus.marius.tinyelectronicslab.objects.InputReader;
import com.daedalus.marius.tinyelectronicslab.objects.OutputGenerator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Marius on 04.03.2015.
 */
public abstract class WorkerActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    public static String TAG = "LCR METER";
    public static final int DEFAULT_PREF_FREQ = 1000;

    protected Handler mHandler;

    protected FixedSizeQueue<Short> mValues;

    public OutputGenerator mGenerator;
    public InputReader mReader;

    public Calibrations calib;

    protected boolean forceOutputToAudioJack = false;
    protected boolean keepScreenOn = false;

    private int oldVolume;

    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Select best signal frequency to ensure as best sample matching as possible */
        int preferredSampleRate = InputReader.getPreferredSampleRate();
        int preferredFrequency = DEFAULT_PREF_FREQ;
        List<Integer> preferredFrequencies = getPreferredFrequencies(preferredSampleRate, DEFAULT_PREF_FREQ);
        int outSampleRate = OutputGenerator.getPreferredSampleRate();
        if(outSampleRate==preferredSampleRate){
            preferredFrequency = preferredFrequencies.get(16);
        }
        else{
            List<Integer> preferredOutFrequencies = getPreferredFrequencies(outSampleRate, DEFAULT_PREF_FREQ);
            Set<Integer> s1 = new HashSet<>(preferredFrequencies);
            Set<Integer> s2 = new HashSet<>(preferredOutFrequencies);
            s1.retainAll(s2);

            if(s1.size()==0)
                preferredFrequency = preferredFrequencies.get(16);
            else{
                int fp = Integer.MAX_VALUE;
                for(Integer f : s1)
                    if(Math.abs(DEFAULT_PREF_FREQ - f)<Math.abs(DEFAULT_PREF_FREQ - fp)){
                        fp = f;
                    }
                preferredFrequency = fp;
            }
        }


        mValues = new FixedSizeQueue<>(preferredSampleRate/preferredFrequency);
        mHandler = new MeasuringHandler(this);

        mGenerator = new OutputGenerator();
        mGenerator.amplitude = (short) calib.getAmplitude(0);
        mReader = new InputReader(mHandler, preferredSampleRate);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            setEnabled(false);
            Toast.makeText(this, "Audio focus request not granted", Toast.LENGTH_SHORT).show();
        }

        SharedPreferences sp = getSharedPreferences(SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        forceOutputToAudioJack = sp.getBoolean(SettingsActivity.FORCE_JACK_OUTPUT, false);
        keepScreenOn = sp.getBoolean(SettingsActivity.KEEP_SCREEN_ON, false);

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        //remember the old volume level so we set it back when we leave the app
        oldVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        //set stream volume to max, so that we adjust the volume only from the signal generator
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);

        setEnabled(true);
        routeToAudioJack(forceOutputToAudioJack);
        if(keepScreenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        setEnabled(false);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                oldVolume,
                0);

        if (forceOutputToAudioJack)
            routeToAudioJack(false);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        audioManager.abandonAudioFocus(this);
        mReader.stop();
        mGenerator.stop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Log.d("OnKeyDown", event.toString());
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {

            case AudioManager.AUDIOFOCUS_GAIN:
                setEnabled(true);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                setEnabled(false);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                setEnabled(false);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                setEnabled(false);
                break;
        }
    }

    public class RemoteControlReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                Log.d("RemoteControlReceiver", event.toString());

            } else if (!forceOutputToAudioJack) {
                if (intent.getAction().equals(
                        AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {

                    Log.d("RemoteControlReceiver", "ACTION_BECOMING_NOISY");

                    setEnabled(false);

                } else if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                    int state = intent.getIntExtra("state", -1);
                    Log.d("RemoteControlReceiver", "ACTION_HEADSET_PLUG");
                    switch (state) {
                        case 0:

                            setEnabled(false);
                            Log.d("RemoteControlReceiver", "HEADSET_UNPLUGGED");
                            break;
                        case 1:
                            if (intent.getIntExtra("microphone", -1) == 1) {
                                setEnabled(true);

                                Log.d("RemoteControlReceiver", "HEADSET_PLUGGED and has microphone");
                            } else {
                                setEnabled(false);
                                Log.d("RemoteControlReceiver", "HEADSET_PLUGGED, no microphone");
                            }
                            break;
                    }
                }
            }
        }
    }

    public void setEnabled(boolean enabled) {
        if (!enabled) {
            mGenerator.pause();
            mReader.pause();
        }
    }

    public void onDataReceived(short[] buffer){

        int size = mValues.size();
        int len = buffer.length;
        if(len>size){
            for(int i=len-size; i<len; i++)
                mValues.add(buffer[i]);
        }
        else
            for(short i : buffer)
                mValues.add(i);

        notifyDataReceived(mValues);

    }

    public abstract void notifyDataReceived(FixedSizeQueue values);

    /*Code shamelessly stolen from toggleheadset2
    * Some devices don't automatically toggle to Headphone Mode when plugging
    * in an audio jack, unless it meets some requirements, like a specific impedance,
    * or there is a way from output to ground.*/

    private static final int DEVICE_IN_WIRED_HEADSET = 0x400000;
    private static final int DEVICE_OUT_EARPIECE = 0x1;
    private static final int DEVICE_OUT_WIRED_HEADSET = 0x4;
    private static final int DEVICE_STATE_UNAVAILABLE = 0;
    private static final int DEVICE_STATE_AVAILABLE = 1;

    protected void setDeviceConnectionState(final int device, final int state, final String address) {
        try {
            Class<?> audioSystem = Class.forName("android.media.AudioSystem");
            Method setDeviceConnectionState = audioSystem.getMethod(
                    "setDeviceConnectionState", int.class, int.class, String.class);

            setDeviceConnectionState.invoke(audioSystem, device, state, address);
        } catch (Exception e) {
            Log.e(TAG, "setDeviceConnectionState failed: " + e);
        }
    }

    protected void routeToAudioJack(boolean routeToJack) {
        if (routeToJack) {
            if (!isRoutingHeadset()) {
                Log.d("ROUTING OVERRIDE", "Forcing routing to audio jack");
                setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
                setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
            }
        } else {
            //if (isRoutingHeadset()) {
            Log.d("ROUTING OVERRIDE", "Routing as normal.");
            setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
            setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
            setDeviceConnectionState(DEVICE_OUT_EARPIECE, DEVICE_STATE_AVAILABLE, "");
            //}
        }
    }

    protected boolean isRoutingHeadset() {
        boolean isRoutingHeadset = false;

        try {
            /**
             * Use reflection to get headset routing
             */
            Class<?> audioSystem = Class.forName("android.media.AudioSystem");
            Method getDeviceConnectionState = audioSystem.getMethod(
                    "getDeviceConnectionState", int.class, String.class);

            int retVal = (Integer) getDeviceConnectionState.invoke(audioSystem, DEVICE_IN_WIRED_HEADSET, "");

            isRoutingHeadset = (retVal == 1);
            Log.d(TAG, "getDeviceConnectionState " + retVal);

        } catch (Exception e) {
            Log.e(TAG, "Could not determine status in isRoutingHeadset(): " + e);
        }

        return isRoutingHeadset;
    }

    protected List<Integer> getPreferredFrequencies(int fSampling, int f){

        ArrayList<Integer> goodFreqs = new ArrayList<>();
        int samples = fSampling/f;
        float freq;
        for(int i=-16; i<17; i++){
            freq = fSampling/(samples+i);
            if(freq%1==0) {
                goodFreqs.add(new Integer((int) freq));
            }
        }

        return goodFreqs;

    }

}
