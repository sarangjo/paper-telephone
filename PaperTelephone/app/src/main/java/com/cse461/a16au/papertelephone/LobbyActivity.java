package com.cse461.a16au.papertelephone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

import static com.cse461.a16au.papertelephone.GameData.connectedDevices;
import static com.cse461.a16au.papertelephone.GameData.lastPair;
import static com.cse461.a16au.papertelephone.GameData.nextDevice;
import static com.cse461.a16au.papertelephone.GameData.startDevice;
import static com.cse461.a16au.papertelephone.GameData.unplacedDevices;

/**
 * TODO: class documentation
 */
public class LobbyActivity extends AppCompatActivity implements DevicesFragment.DeviceChosenListener {
    private static final String TAG = "LobbyActivity";

    /**
     * Adapter for connected devices view
     */
    private ArrayAdapter<String> mConnectedDevicesAdapter;

    /**
     * Access to the Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Our bluetooth service
     */
    private BluetoothConnectService mConnectService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setting up singletons
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerMainHandler(mMainHandler);

        // Now that device search is integrated in a fragment, we don't need this
//        Button devicesButton = (Button) findViewById(R.id.button_show_devices);
//        if (devicesButton != null) {
//            devicesButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    startActivityForResult(new Intent(LobbyActivity.this, ListDeviceActivity.class), Constants.REQUEST_CONNECT_DEVICE);
//                }
//            });
//        }

        setupConnectedDevicesList();

        Button startGameButton = (Button) findViewById(R.id.button_start_game);
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
    }

    /**
     * Establishes an ordering for the connected devices when the user hits the start button
     */
    private void start() {
        if (connectedDevices.size() >= 2) {
            byte[] startMsg = Constants.HEADER_START;

            for (String currDevice : connectedDevices) {
                mConnectService.write(startMsg, currDevice);
            }

            // Sleep for a short time in case another device pressed start at the same time
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to sleep", e);
            }


            if (startDevice == -1 || mBluetoothAdapter.getAddress().compareTo(connectedDevices.get(startDevice)) < 0) {
                for (String address : connectedDevices) {
                    unplacedDevices.add(address);
                }

                chooseSuccessor();
            }
        } else if (connectedDevices.size() == 0) {
            Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "You don't have enough players, the game requires " +
                    "at least 3 players", Toast.LENGTH_LONG).show();
        }
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
        mConnectedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, connectedDevices);
        ListView connectedListView = (ListView) findViewById(R.id.connected_devices);
        connectedListView.setAdapter(mConnectedDevicesAdapter);
        connectedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!connectedDevices.isEmpty()) {
                    mConnectService.write(Constants.HEADER_PING, connectedDevices.get(position));
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

    private final Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_CONNECTED:
                    String deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    String deviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);

                    // TODO: send a message describing which devices we are already connected to
                    sendConnectedDevices(deviceAddress);

                    connectedDevices.add(deviceAddress);
                    mConnectedDevicesAdapter.notifyDataSetChanged();

                    Toast.makeText(LobbyActivity.this, "Connected to " + deviceName, Toast.LENGTH_LONG).show();

                    break;
                case Constants.MESSAGE_DISCONNECTED:
                    deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    deviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);
                    connectedDevices.remove(deviceAddress);
                    mConnectedDevicesAdapter.notifyDataSetChanged();

                    Toast.makeText(LobbyActivity.this, "Disconnected from " + deviceName, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_WRITE:
                    // TODO: do something with what we write out?
                    Toast.makeText(LobbyActivity.this, "Sent data", Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_READ:
                    ByteBuffer buf;
                    switch (msg.arg2) {
                        case Constants.READ_UNKNOWN:
                            Toast.makeText(LobbyActivity.this, "Received unknown format", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.READ_PING:
                            Toast.makeText(LobbyActivity.this, "Received ping", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.READ_START:
                            // Received START message
                            String startDeviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);
                            startDevice = connectedDevices.indexOf(startDeviceAddress);
                            for (int i = 0; i < connectedDevices.size(); i++) {
                                String currDevice = connectedDevices.get(i);
                                if (!currDevice.equals(startDeviceAddress)) {
                                    unplacedDevices.add(currDevice);
                                }
                            }
                            break;
                        case Constants.READ_PAIR:
                            // Format of pair tuple: header, pair #, pair address
                            // msg.arg1 = Constants.HEADER_LENGTH + 4 + Constants.ADDRESS_LENGTH);
                            buf = ByteBuffer.wrap((byte[]) msg.obj, 0, msg.arg1);

                            // Throw away header
                            buf.get(new byte[Constants.HEADER_LENGTH]);

                            lastPair = buf.getInt();
                            byte[] pairedDeviceAddressArr = new byte[17];
                            buf.get(pairedDeviceAddressArr);

                            String pairedDeviceAddress = new String(pairedDeviceAddressArr);
                            unplacedDevices.remove(pairedDeviceAddress);

                            if (    // If we are the start device and we are the newly paired device, start game
                                    (startDevice == -1 && pairedDeviceAddress.equals(mBluetoothAdapter.getAddress()))
                                    // If the loop has been completed and all devices have a successor, start game
                                    || pairedDeviceAddress.equals(connectedDevices.get(startDevice))) {
                                Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
                                startActivity(intent);
                                return;
                            }

                            // If removing from the set returns false that means we are the
                            // newly paired device so we need to choose our successor
                            if (pairedDeviceAddress.equals(mBluetoothAdapter.getAddress())) {
                                chooseSuccessor();
                            }
                            break;
                        case Constants.READ_DEVICES:
                            // TODO: establish connections with devices that are already in the game
                            buf = ByteBuffer.wrap((byte[]) msg.obj, 0, msg.arg1);
                            buf.get(new byte[Constants.HEADER_LENGTH]);

                            int numDevices = buf.getInt();
                            String[] addresses = new String[numDevices];
                            for (int i = 0; i < numDevices; i++) {
                                byte[] address = new byte[Constants.ADDRESS_LENGTH];
                                buf.get(address);
                                addresses[i] = new String(address);
                            }

                            joinGame(addresses);
                            break;
                    }
                    break;
            }
        }
    };

    /**
     * Establishes connections with all given addresses.
     */
    private void joinGame(String[] addresses) {
        for (String addr : addresses) {
            if (GameData.connectedDevices.indexOf(addr) >= 0) {
                // this has already been connected to!
                Log.d(TAG, "Already connected to " + addr);
            } else {
                connectDevice(addr);
            }
        }
    }

    /**
     * After establishing a connection with another device, send the already-connected devices with it.
     *
     * @param deviceAddress
     */
    private void sendConnectedDevices(String deviceAddress) {
        ByteBuffer buf = ByteBuffer.allocate(Constants.HEADER_LENGTH + 4 + Constants.ADDRESS_LENGTH * connectedDevices.size());
        buf.put(Constants.HEADER_DEVICES);
        buf.putInt(connectedDevices.size());
        for (String address : connectedDevices) {
            buf.put(address.getBytes());
        }

        mConnectService.write(buf.array(), deviceAddress);
    }

    /**
     * Chooses which device we will be sending to and informs all other devices
     */
    private void chooseSuccessor() {
        Iterator<String> iter = unplacedDevices.iterator();
        String nextDeviceAddress;
        if (iter.hasNext()) {
            nextDeviceAddress = iter.next();

            // Remove device from set of unplaced devices
            iter.remove();
        } else {
            nextDeviceAddress = connectedDevices.get(startDevice);
        }

        ByteBuffer msg = ByteBuffer.allocate(Constants.HEADER_LENGTH + 4 + Constants.ADDRESS_LENGTH);
        msg.put(Constants.HEADER_PAIR);
        msg.putInt(lastPair + 1);
        msg.put(nextDeviceAddress.getBytes());

        // Set nextDevice field to store which device we will send prompts and drawings to
        nextDevice = connectedDevices.indexOf(nextDeviceAddress);

        for (String address : connectedDevices) {
            mConnectService.write(msg.array(), address);
        }
    }

    @Override
    public void deviceChosen(String address) {
        connectDevice(address);
    }
}
