package com.daedalus.marius.tinyelectronicslab.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.daedalus.marius.tinyelectronicslab.C;
import com.daedalus.marius.tinyelectronicslab.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button b = (Button) findViewById(R.id.button_lcr);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getSharedPreferences(SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                if(sp.getFloat(C.INTERNAL_RESISTANCE, -1) != -1){
                    Intent intent = new Intent(MainActivity.this, MultimeterActivity.class);
                    startActivity(intent);
                }
                else{

                }
            }
        });

        b = (Button) findViewById(R.id.button_funct);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FunctionGeneratorActivity.class);
                startActivity(intent);
            }
        });

        b = (Button) findViewById(R.id.button_osc);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OscilloscopeActivity.class);
                startActivity(intent);
            }
        });

        b = (Button) findViewById(R.id.button_settings);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });




    }
}
