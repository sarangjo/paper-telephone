package com.cse461.a16au.papertelephone;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import java.lang.Override;

public class MainActivity extends FragmentActivity {
    private Button buttonStartGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonStartGame = (Button) findViewById(R.id.button_open_lobby);
        buttonStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LobbyActivity.class));
            }
        });

        // Request that bluetooth be enabled if it is disabled, otherwise connect to other devices
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
            buttonStartGame.setEnabled(false);
        } else {
            buttonStartGame.setEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                // Setup game and connect to other devices now that bluetooth is enabled
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Successfully enabled Bluetooth.", Toast.LENGTH_LONG).show();
                    buttonStartGame.setEnabled(true);
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(Constants.APP_NAME, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_exit,
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }
}
