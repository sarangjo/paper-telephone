package com.cse461.a16au.papertelephone;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by jsunde on 11/18/2016.
 * Use this file for reference in writing the bluetooth related logic
 * https://github.com/googlesamples/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/BluetoothChatFragment.java
 * TODO: Add discoverability logic maybe?
 */

public class BluetoothStartFragment extends Fragment {

    private static final String TAG = "BluetoothStartFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Names of connected devices
    private ArrayList<String> connectedDeviceNames = new ArrayList<String>();

    // Local Bluetooth adapter
    private BluetoothAdapter bluetoothAdapter = null;

    // Bluetooth Connection Service to handle connections to other devices
    private BluetoothConnectService connectService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Display error message in the case that the device does not support bluetooth
        if(bluetoothAdapter == null) {
            FragmentActivity activity = (FragmentActivity) getActivity();
            Toast.makeText(activity, "Bluetooth is not available." +
                    "Our game needs bluetooth to work... Sorry", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Request that bluetooth be enabled if it is disabled, otherwise connect to other devices
        if(!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if(connectService == null) {
            setupGame();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop connect service to stop connections and shut down
        if(connectService != null) {
            connectService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // In the case that Bluetooth was disabled to start, onResume() will
        // be called when the ACTION_REQUEST_ENABLE activity has returned
        // TODO: connect to other devices here
        if(connectService != null) {
            if(connectService.getState() == BluetoothConnectService.STOPPED) {
                // Start our BluetoothConnectionService
                connectService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_start, container, false);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // Setup game and connect to other devices now that bluetooth is enabled
                if (resultCode == Activity.RESULT_OK) {
                    setupGame();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_exit,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    private void connectDevice(Intent data) {
        // Get the address of the device
        String addr = data.getExtras().getString(ListDeviceActivity.DEVICE_ADDRESS);

        // Get the BluetoothDevice
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);

        // Connect to device
        connectService.connect(device);
    }

    // TODO: check if we need a menu for the start fragment, or if we'll just need one for the actual game activity
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.start, menu);
    }
}
