package com.cse461.a16au.papertelephone;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.lang.Override;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDegaultAdapter();

        // Display error message in the case that the device does not support bluetooth
        if(bluetoothAdapter == null) {
            // TODO: The device doesn't support bluetooth so we should display some message
            // that the game won't work on their device
        }

        // Enable bluetooth simply by enabling bluetooth discoverability
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        // TODO: Maybe implement onActivityResult()? I'm not entirely sure
        // https://developer.android.com/guide/topics/connectivity/bluetooth.html#EnablingDiscoverability
    }
}
