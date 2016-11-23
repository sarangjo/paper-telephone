package com.cse461.a16au.papertelephone;

/**
 * Created by sgorti3 on 11/21/2016.
 */


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

/**
 * This class does the work of setting up
 * bluetooth connections between phones after
 * initializing and making sure that phones
 * have bluetooth available and that it is turned on
 *
 */
public class BluetoothConnectService {
    public static final int STOPPED = 0;
    private final BluetoothAdapter myAdapater;
    private final Handler myHandler;
    private int state;

    public BluetoothConnectService(Handler handler) {
        myAdapater = BluetoothAdapter.getDefaultAdapter();
        myHandler = handler;
    }


    public void stop() {
        // TODO implement
    }

    public int getState() {
        return state;
    }

    public void start() {
        // TODO: implement
    }

    public void connect(BluetoothDevice device) {
        // TODO: implement
    }
}
