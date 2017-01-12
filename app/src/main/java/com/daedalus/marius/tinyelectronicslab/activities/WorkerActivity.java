package com.daedalus.marius.tinyelectronicslab.activities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
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
import com.daedalus.marius.tinyelectronicslab.objects.InputReader;
import com.daedalus.marius.tinyelectronicslab.objects.OutputGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created by Marius on 04.03.2015.
 */
public abstract class WorkerActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static String TAG = "LCR METER";

    protected Handler mHandler;

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

        calib = readCalibrationFile();

        if (calib != null) {
            if (calib.getRanges().length != 0)
                mHandler = new MeasuringHandler(this);
            else {
                mHandler = new CalibratingHandler(this, false, false);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } else {
            mHandler = new CalibratingHandler(this, false, false);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            calib = new Calibrations();
        }

        mGenerator = new OutputGenerator();
        mGenerator.amplitude = (short) calib.getAmplitude(0);
        mReader = new InputReader(mHandler);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            setEnabled(false);
            Toast.makeText(this, "Audio focus request not granted", Toast.LENGTH_SHORT).show();
        }

        SharedPreferences sp = getSharedPreferences(SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(this);

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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsActivity.FORCE_JACK_OUTPUT))
            forceOutputToAudioJack = sharedPreferences.getBoolean(SettingsActivity.FORCE_JACK_OUTPUT, false);
        else if (key.equals(SettingsActivity.KEEP_SCREEN_ON))
            keepScreenOn = sharedPreferences.getBoolean(SettingsActivity.KEEP_SCREEN_ON, false);
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

    protected Calibrations readCalibrationFile() {

        return null;

    }

    protected boolean writeCalibrationFile() {

        return false;
    }

    public void setEnabled(boolean enabled) {
        if (!enabled) {
            mGenerator.pause();
            mReader.pause();
        }
    }

    public void notifyCalibrationDone() {

        setHandler(new MeasuringHandler(this));
        writeCalibrationFile();
        if (!keepScreenOn)
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void setHandler(Handler handler) {
        mHandler = handler;
        mReader.setHandler(mHandler);
    }

    public abstract void onDataReceived(int vOUTx, short[] buffer);

    public abstract void putResistanceMeasurementPair(int rx, int vOUTx);

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
}
