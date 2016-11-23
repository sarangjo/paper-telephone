package com.cse461.a16au.papertelephone;

/**
 * Created by sgorti3 on 11/21/2016.
 */


import android.bluetooth.BluetoothAdapter;
import android.os.Handler;

/**
 * This class does the work of setting up
 * bluetooth connections between phones after
 * initializing and making sure that phones
 * have bluetooth available and that it is turned on
 *
 */
public class BluetoothConnectService {
    private final BluetoothAdapter myAdapater;
    private final Handler myHandler;

    public BluetoothConnectService(Handler handler) {
        myAdapater = BluetoothAdapter.getDefaultAdapter();
        myHandler = handler;
    }


}
