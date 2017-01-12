package com.daedalus.marius.tinyelectronicslab.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.daedalus.marius.tinyelectronicslab.R;

/**
 * Created by Marius on 28.02.2015.
 */
public class SettingsActivity extends ActionBarActivity {

    public static final String FORCE_JACK_OUTPUT = "forceAudioJackOutput";
    public static final String KEEP_SCREEN_ON = "keepScreenOn";
    public static final String SHARED_PREF_NAME = "Shared Pref";

    private CheckBox forceAudioJackOutput;
    private CheckBox keepScreenOn;

    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences sp = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        editor = sp.edit();

        forceAudioJackOutput = (CheckBox) findViewById(R.id.checkBox_forceOutput);
        forceAudioJackOutput.setChecked(sp.getBoolean(FORCE_JACK_OUTPUT, false));
        forceAudioJackOutput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editor.putBoolean(FORCE_JACK_OUTPUT, b).commit();
            }
        });

        keepScreenOn = (CheckBox) findViewById(R.id.checkBox_screenOn);
        keepScreenOn.setChecked(sp.getBoolean(KEEP_SCREEN_ON, false));
        keepScreenOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editor.putBoolean(KEEP_SCREEN_ON, b).commit();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_activity_lcr) {
            startActivity(new Intent(this, MultimeterActivity.class));
            return true;

        } else if (id == R.id.action_activity_osc) {
            startActivity(new Intent(this, OscilloscopeActivity.class));
            return true;

        }

        return super.onOptionsItemSelected(item);
    }


}
