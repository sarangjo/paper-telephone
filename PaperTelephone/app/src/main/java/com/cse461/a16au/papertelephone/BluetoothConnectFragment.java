package com.cse461.a16au.papertelephone;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by jsunde on 11/18/2016.
 * Use this file for reference in writing the bluetooth related logic
 * https://github.com/googlesamples/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/BluetoothChatFragment.java
 */

public class BluetoothConnectFragment extends Fragment {

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Names of connected devices
    private ArrayList<String> connectedDeviceNames = new ArrayList<String>();

    // Local Bluetooth adapter
    private BluetoothAdapter bluetoothAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Display error message in the case that the device does not support bluetooth
        if(bluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available." +
                    "Our game needs bluetooth to work... Sorry", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Request that bluetooth be enabled if it is disabled, otherwise connect to other devices
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            // TODO: connect to other devices
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: stop connections and shut down
    }

    @Override
    public void onResume() {
        super.onResume();

        // In the case that Bluetooth was disabled to start, onResume() will
        // be called when the ACTION_REQUEST_ENABLE activity has returned
        // TODO: connect to other devices here
    }
}
