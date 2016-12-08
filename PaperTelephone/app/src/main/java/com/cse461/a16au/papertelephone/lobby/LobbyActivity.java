package com.cse461.a16au.papertelephone.lobby;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.BluetoothConnectService;
import com.cse461.a16au.papertelephone.R;
import com.cse461.a16au.papertelephone.game.GameActivity;
import com.cse461.a16au.papertelephone.game.GameData;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static com.cse461.a16au.papertelephone.Constants.ADDRESS_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.DEBUG;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_NAME;
import static com.cse461.a16au.papertelephone.Constants.HEADER_DEVICES;
import static com.cse461.a16au.papertelephone.Constants.HEADER_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.HEADER_PING;
import static com.cse461.a16au.papertelephone.Constants.HEADER_START;
import static com.cse461.a16au.papertelephone.Constants.HEADER_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_DISCONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_READ;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_WRITE;
import static com.cse461.a16au.papertelephone.Constants.READ_DEVICES;
import static com.cse461.a16au.papertelephone.Constants.READ_PING;
import static com.cse461.a16au.papertelephone.Constants.READ_START;
import static com.cse461.a16au.papertelephone.Constants.READ_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.READ_UNKNOWN;
import static com.cse461.a16au.papertelephone.game.GameData.connectedDeviceNames;
import static com.cse461.a16au.papertelephone.game.GameData.connectedDevices;
import static com.cse461.a16au.papertelephone.game.GameData.lastSuccessor;
import static com.cse461.a16au.papertelephone.game.GameData.nextDevice;
import static com.cse461.a16au.papertelephone.game.GameData.startDevice;
import static com.cse461.a16au.papertelephone.game.GameData.turnsLeft;
import static com.cse461.a16au.papertelephone.game.GameData.unplacedDevices;

/**
 * TODO: class documentation
 */
public class LobbyActivity extends AppCompatActivity implements DevicesFragment.ConnectDeviceListener {
    private static final String TAG = "LobbyActivity";

    /**
     * Adapter for connected devices view
     */
    private ArrayAdapter<String> mConnectedDevicesNamesAdapter;

    /**
     * Our bluetooth service
     */
    private BluetoothConnectService mConnectService = null;

    /**
     * Main view.
     */
    private View mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setting up bluetooth
        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerMainHandler(mMainHandler);

        // Views
        mConnectedDevicesNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, connectedDeviceNames);
        ListView connectedListView = (ListView) findViewById(R.id.connected_devices);
        connectedListView.setAdapter(mConnectedDevicesNamesAdapter);
        connectedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!connectedDevices.isEmpty()) {
                    mConnectService.write(HEADER_PING, connectedDevices.get(position));
                }
            }
        });

        mView = this.findViewById(android.R.id.content);

        Button startGameButton = (Button) findViewById(R.id.button_start_game);
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });
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

    /**
     * After establishing a connection with another device, send the already-connected devices with it.
     *
     * @param deviceAddress
     */
    private void sendConnectedDevices(String deviceAddress) {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + 4 + ADDRESS_LENGTH * connectedDevices.size());
        buf.put(HEADER_DEVICES);
        buf.putInt(connectedDevices.size());
        for (String address : connectedDevices) {
            buf.put(address.getBytes());
        }

        mConnectService.write(buf.array(), deviceAddress);
    }

    /**
     * Establishes an ordering for the connected devices when the user hits the start button
     */
    // TODO: test multiple start logic, Sidd and Jakob seem to have uncovered a bug there
    private void startGame() {
        // TODO: change back to 2
        if (connectedDevices.size() >= 1) {
            byte[] startMsg = HEADER_START;

            for (String currDevice : connectedDevices) {
                mConnectService.write(startMsg, currDevice);
            }

            // Sleep for a short time in case another device pressed start at the same time
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to sleep", e);
            }

            // TODO: reset all other GameData?
            turnsLeft = connectedDevices.size();

            // TODO: Does this hacky way work?
            String localAddress = android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address");

            // Ensure that either we're still the start device, or we get preference by address comparison
            if (startDevice == -1 || localAddress.compareTo(connectedDevices.get(startDevice)) < 0) {
                for (String address : connectedDevices) {
                    unplacedDevices.add(address);
                }

                chooseSuccessor();
            }
        // TODO: REMOVE THIS IN THE FINAL VERSION
        } else if (connectedDevices.size() == 0 && DEBUG) {
            Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "You don't have enough players, the game requires " +
                    "at least 3 players", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Chooses which device we will be sending to and informs all other devices
     */
    private void chooseSuccessor() {
        Iterator<String> iter = unplacedDevices.iterator();
        boolean isLast = !iter.hasNext();
        String nextDeviceAddress;
        if (!isLast) {
            nextDeviceAddress = iter.next();
            // Remove device from set of unplaced devices
            iter.remove();
        } else {
            nextDeviceAddress = connectedDevices.get(startDevice);
        }

        ByteBuffer msg = ByteBuffer.allocate(HEADER_LENGTH + 4 + ADDRESS_LENGTH);
        msg.put(HEADER_SUCCESSOR);
        msg.putInt(lastSuccessor + 1);
        msg.put(nextDeviceAddress.getBytes());

        // Set nextDevice field to store which device we will send prompts and drawings to
        nextDevice = connectedDevices.indexOf(nextDeviceAddress);

        for (String address : connectedDevices) {
            mConnectService.write(msg.array(), address);
        }

        if(isLast) {
            Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void connectDevice(String address) {
        // Connect to device
        mConnectService.connect(address);
    }

    private void handleRead(Message msg) {
        ByteBuffer buf;
        switch (msg.arg2) {
            case READ_UNKNOWN:
                Toast.makeText(LobbyActivity.this, "Received unknown format", Toast.LENGTH_SHORT).show();
                break;
            case READ_PING:
                Toast.makeText(LobbyActivity.this, "Received ping from " + msg.getData().getString(DEVICE_NAME),
                        Toast.LENGTH_SHORT).show();
                break;
            case READ_START:
                // Received START message
                String startDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);
                startDevice = connectedDevices.indexOf(startDeviceAddress);

                // Setup unplaced devices, i.e. all devices except for the start device and us
                for (int i = 0; i < connectedDevices.size(); i++) {
                    String currDevice = connectedDevices.get(i);
                    if (!currDevice.equals(startDeviceAddress)) {
                        unplacedDevices.add(currDevice);
                    }
                }
                break;
            case READ_SUCCESSOR:
                // Format of pair tuple: header, pair #, pair address
                // msg.arg1 = HEADER_LENGTH + 4 + ADDRESS_LENGTH);
                buf = ByteBuffer.wrap((byte[]) msg.obj, 0, msg.arg1);

                // Throw away header
                buf.get(new byte[HEADER_LENGTH]);

                lastSuccessor = buf.getInt();
                byte[] pairedDeviceAddressArr = new byte[17];
                buf.get(pairedDeviceAddressArr);

                String pairedDeviceAddress = new String(pairedDeviceAddressArr);
                unplacedDevices.remove(pairedDeviceAddress);

                boolean isUs = true;
                for (String address : connectedDevices) {
                    isUs = isUs && !pairedDeviceAddress.equals(address);
                }

                // If we are the start device and we are the newly paired device, start game
                if ((startDevice == -1 && isUs)
                        // If the loop has been completed and all devices have a successor, start game
                        || (startDevice != -1 && pairedDeviceAddress.equals(connectedDevices.get(startDevice)))) {
                    Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
                    startActivity(intent);
                    // TODO: finish() to prevent more callbacks?
                    return;
                }

                // If removing from the set returns false that means we are the
                // newly paired device so we need to choose our successor
                if (isUs) {
                    chooseSuccessor();
                }
                break;
            case READ_DEVICES:
                // Establish connections with devices that are already in the game
                buf = ByteBuffer.wrap((byte[]) msg.obj, 0, msg.arg1);
                buf.get(new byte[HEADER_LENGTH]);

                int numDevices = buf.getInt();
                String[] addresses = new String[numDevices];
                for (int i = 0; i < numDevices; i++) {
                    byte[] address = new byte[ADDRESS_LENGTH];
                    buf.get(address);
                    addresses[i] = new String(address);
                }

                for (String addr : addresses) {
                    if (GameData.connectedDevices.indexOf(addr) >= 0) {
                        // this has already been connected to!
                        Log.d(TAG, "Already connected to " + addr);
                    } else {
                        connectDevice(addr);
                    }
                }
                break;
        }
    }

    private final Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String deviceName, deviceAddress;
            switch (msg.what) {
                case MESSAGE_CONNECTED:
                    deviceName = msg.getData().getString(DEVICE_NAME);
                    deviceAddress = msg.getData().getString(DEVICE_ADDRESS);

                    // Send a message describing which devices we are already connected to
                    sendConnectedDevices(deviceAddress);

                    connectedDevices.add(deviceAddress);
                    connectedDeviceNames.add(deviceName);
                    mConnectedDevicesNamesAdapter.notifyDataSetChanged();
                    Snackbar.make(mView, "Connected to " + deviceName, Snackbar.LENGTH_LONG).show();
                    break;
                case MESSAGE_DISCONNECTED:
                    deviceName = msg.getData().getString(DEVICE_NAME);
                    deviceAddress = msg.getData().getString(DEVICE_ADDRESS);

                    connectedDevices.remove(deviceAddress);
                    connectedDeviceNames.remove(deviceName);
                    mConnectedDevicesNamesAdapter.notifyDataSetChanged();
                    Snackbar.make(mView, "Disconnected from " + deviceName, Snackbar.LENGTH_LONG).show();
                    break;
                case MESSAGE_WRITE:
                    // TODO: do something with what we write out?
                    Log.d(TAG, "Sent data");
                    break;
                case MESSAGE_READ:
                    handleRead(msg);
                    break;
            }
        }
    };
}
