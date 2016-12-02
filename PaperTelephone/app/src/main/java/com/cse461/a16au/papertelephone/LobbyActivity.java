package com.cse461.a16au.papertelephone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * TODO: class documentation
 */
public class LobbyActivity extends AppCompatActivity implements DevicesFragment.DeviceChosenListener {
    private static final String TAG = "LobbyActivity";

    // Names of connected devices
    private ArrayAdapter<String> connectedDevicesAdapter;

    /**
     * Access to the Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Our bluetooth service
     */
    private BluetoothConnectService mConnectService = null;

    // TODO: static?
    private static int nextDevice = 0;
    private static int startDevice = -1;
    private static Set<String> unplacedDevices = new HashSet<>();
    private static int lastPair = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setting up singletons
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerHandler(mHandler);

        Button devicesButton = (Button) findViewById(R.id.button_show_devices);
        if (devicesButton != null) {
            devicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(LobbyActivity.this, ListDeviceActivity.class), Constants.REQUEST_CONNECT_DEVICE);
                }
            });
        }

        setupConnectedDevicesList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // In the case that Bluetooth was disabled to start, onResume() will
        // be called when the ACTION_REQUEST_ENABLE activity has returned
        if (mConnectService != null) {
            if (mConnectService.getState() == BluetoothConnectService.STATE_STOPPED) {
                // Start our BluetoothConnectionService
                mConnectService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TODO: Stop connect service to stop connections and shut down?
//        if (mConnectService != null) {
//            mConnectService.stop();
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE:
                if (resultCode == RESULT_OK) {
                    connectDevice(data.getStringExtra(Constants.DEVICE_ADDRESS));
                }
                break;
        }
    }

    /**
     * Set up paired, unpaired, and connected devices.
     */

    private void setupConnectedDevicesList() {
        // Connected devices
        connectedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView connectedListView = (ListView) findViewById(R.id.connected_devices);
        connectedListView.setAdapter(connectedDevicesAdapter);
        connectedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!connectedDevicesAdapter.isEmpty()) {
                    mConnectService.write(Constants.PING, connectedDevicesAdapter.getItem(position));
                }
            }
        });
    }

    /**
     * After the device has been selected, connect to the device.
     */
    private void connectDevice(String address) {
        // Get the BluetoothDevice
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        // Connect to device
        mConnectService.connect(device);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_CONNECTED:
                    String deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    String deviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);
                    connectedDevicesAdapter.add(deviceAddress);
                    Toast.makeText(LobbyActivity.this, "Connected to " + deviceName, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_DISCONNECTED:
                    deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    deviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);
                    connectedDevicesAdapter.remove(deviceAddress);
                    Toast.makeText(LobbyActivity.this, "Disconnected from " + deviceName, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_WRITE:
                    // TODO: do something with what we write out?
                    Toast.makeText(LobbyActivity.this, "Sent data", Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_READ:
                    switch (msg.arg2) {
                        case Constants.READ_TEXT:
                            Toast.makeText(LobbyActivity.this, "Received ping", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.READ_START:
                            if (msg.arg1 == 5) {
                                String startDeviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);
                                startDevice = connectedDevicesAdapter.getPosition(startDeviceAddress);
                                for (int i = 0; i < connectedDevicesAdapter.getCount(); i++) {
                                    String currDevice = connectedDevicesAdapter.getItem(i);
                                    if (!currDevice.equals(startDeviceAddress)) {
                                        unplacedDevices.add(currDevice);
                                    }
                                }
                            } else {
                                ByteBuffer buf = ByteBuffer.wrap(Arrays.copyOfRange((byte[]) msg.obj, 0, 26));
                                buf.get(new byte[5], 0, 5); // Throw away header
                                lastPair = buf.getInt();
                                byte[] pairedDeviceAddress = new byte[17];
                                buf.get(pairedDeviceAddress);

                                // If removing from the set returns false that means we are the
                                // newly paired device so we need to choose our successor
                                if (!unplacedDevices.remove(new String(pairedDeviceAddress))) {
                                    chooseSuccessor();
                                }
                            }
                            break;
                    }

                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(LobbyActivity.this, "Toast: " + msg.getData().getString(Constants.TOAST), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    /**
     * Establishes an ordering for the connected devices when the user hits the start button
     */
    private void start() {
        int connectedDevices = connectedDevicesAdapter.getCount();
        if (connectedDevices < 2) {
            Toast.makeText(this, "You don't have enough players, the game requires" +
                    "at least 3 players", Toast.LENGTH_LONG).show();
        } else {
            byte[] startMsg = Constants.HEADER_START;

            for (int i = 0; i < connectedDevices; i++) {
                String currDevice = connectedDevicesAdapter.getItem(i);
                mConnectService.write(startMsg, currDevice);
            }

            // Sleep for a short time in case another device pressed start at the same time
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            if (startDevice == -1 || mBluetoothAdapter.getAddress().compareTo(connectedDevicesAdapter.getItem(startDevice)) < 0) {
                for (int i = 0; i < connectedDevicesAdapter.getCount(); i++) {
                    unplacedDevices.add(connectedDevicesAdapter.getItem(i));
                }

                chooseSuccessor();
            }
        }
    }

    /**
     * Chooses which device we will be sending to and informs all other devices
     */
    private void chooseSuccessor() {
        int connectedDevices = connectedDevicesAdapter.getCount();

        Iterator<String> iter = unplacedDevices.iterator();
        String nextDeviceAddress;
        if (iter.hasNext()) {
            nextDeviceAddress = iter.next();

            // Remove device from set of unplaced devices
            iter.remove();
        } else {
            nextDeviceAddress = connectedDevicesAdapter.getItem(startDevice);
        }

        ByteBuffer msg = ByteBuffer.allocate(26);
        msg.put(Constants.HEADER_START);
        msg.putInt(lastPair + 1);
        msg.put(nextDeviceAddress.getBytes());

        // Set nextDevice field to store which device we will send prompts and drawings to
        nextDevice = connectedDevicesAdapter.getPosition(nextDeviceAddress);

        for (int i = 0; i < connectedDevices; i++) {
            String currDevice = connectedDevicesAdapter.getItem(i);
            mConnectService.write(msg.array(), currDevice);
        }
    }

    @Override
    public void deviceChosen(String address) {
        connectDevice(address);
    }
}
