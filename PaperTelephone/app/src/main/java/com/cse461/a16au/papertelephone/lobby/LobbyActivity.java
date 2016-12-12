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
import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;
import com.cse461.a16au.papertelephone.game.EndGameActivity;
import com.cse461.a16au.papertelephone.game.GameActivity;
import com.cse461.a16au.papertelephone.game.GameData;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static com.cse461.a16au.papertelephone.Constants.ADDRESS_LENGTH;
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
import static com.cse461.a16au.papertelephone.Constants.READ_START_ACK;
import static com.cse461.a16au.papertelephone.Constants.READ_DEVICES;
import static com.cse461.a16au.papertelephone.Constants.READ_PING;
import static com.cse461.a16au.papertelephone.Constants.READ_START;
import static com.cse461.a16au.papertelephone.Constants.READ_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.READ_UNKNOWN;
import static com.cse461.a16au.papertelephone.game.GameData.NO_START;
import static com.cse461.a16au.papertelephone.game.GameData.WE_ARE_START;
import static com.cse461.a16au.papertelephone.game.GameData.lastSuccessor;
import static com.cse461.a16au.papertelephone.game.GameData.mLocalAddress;
import static com.cse461.a16au.papertelephone.game.GameData.nextDevice;
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

    private GameData mGameData = null;

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

        // Get out own local MAC address
        mLocalAddress = android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address");

        mGameData = GameData.getInstance();

        // Views
        mConnectedDevicesNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mGameData.getConnectedDeviceNames());
        ListView connectedListView = (ListView) findViewById(R.id.connected_devices);
        connectedListView.setAdapter(mConnectedDevicesNamesAdapter);
        connectedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!mGameData.getConnectedDevices().isEmpty()) {
                    mConnectService.write(HEADER_PING, mGameData.getConnectedDevices().get(position));
                }
            }
        });

        mView = this.findViewById(android.R.id.content);

        Button startGameButton = (Button) findViewById(R.id.button_start_game);
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGameClicked();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_PLAY_GAME:
                if (resultCode == RESULT_OK) {
                    // Game ended successfully
                    Toast.makeText(this, "Game over!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, EndGameActivity.class);
                    startActivityForResult(intent, Constants.REQUEST_END_GAME);
                } else {
                    Toast.makeText(this, "Game did not end correctly", Toast.LENGTH_LONG).show();
                }
                break;
            case Constants.REQUEST_END_GAME:
                if (resultCode == Constants.RESULT_LOBBY) {
                    // Back to lobby, reset stuff
                    mGameData.setStartDevice(NO_START);
                    // TODO anything else to restart?
                } else if (resultCode == Constants.RESULT_RESTART) {
                    mGameData.setStartDevice(NO_START);
                    // TODO; do something here
                } else {
                    Toast.makeText(this, "End game did not return correctly", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    /**
     * After establishing a connection with another device, send the already-connected devices with it.
     */
    private void sendConnectedDevices(String deviceAddress) {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + 4 + ADDRESS_LENGTH * mGameData.getConnectedDevices().size());
        buf.put(HEADER_DEVICES);
        buf.putInt(mGameData.getConnectedDevices().size());
        for (String address : mGameData.getConnectedDevices()) {
            buf.put(address.getBytes());
        }

        mConnectService.write(buf.array(), deviceAddress);
    }

    /**
     * Establishes an ordering for the connected devices when the user hits the start button
     */
    private void startGameClicked() {
        // TODO: change back to 2
        if (mGameData.getConnectedDevices().size() >= 1) {
            if (mGameData.getStartDevice() >= 0) {
                return;
            }

            mGameData.setStartDevice(GameData.WE_ARE_START);

            for (String currDevice : mGameData.getConnectedDevices()) {
                mConnectService.write(HEADER_START, currDevice);
            }

            mGameData.setupUnackedDevices();

//            // Set up our unplaced devices list and initiate the START process
//            for (String address : mGameData.getConnectedDevices()) {
//                unplacedDevices.add(address);
//            }

//            chooseSuccessor();
        } else {
            Toast.makeText(this, "You don't have enough players, the game requires " +
                    "at least 3 players", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Chooses which device we will be sending to. Also informs all other devices
     */
    private void chooseSuccessor() {
        Iterator<String> iter = unplacedDevices.iterator();

        // If there are no more devices left, our successor is the start device
        boolean isLast = !iter.hasNext();
        String nextDeviceAddress;
        if (!isLast) {
            // Pick the next unplaced device as our successor
            nextDeviceAddress = iter.next();
            iter.remove();
        } else {
            nextDeviceAddress = mGameData.getConnectedDevice(mGameData.getStartDevice());
        }

        // Set up the successor packet
        ByteBuffer msg = ByteBuffer.allocate(HEADER_LENGTH + 4 + ADDRESS_LENGTH);
        msg.put(HEADER_SUCCESSOR);
        msg.putInt(lastSuccessor + 1);
        msg.put(nextDeviceAddress.getBytes());

        // Set nextDevice field to store which device we will send prompts and drawings to
        nextDevice = mGameData.getConnectedDevices().indexOf(nextDeviceAddress);

        for (String address : mGameData.getConnectedDevices()) {
            mConnectService.write(msg.array(), address);
        }

        if (isLast) {
            transitionToGame();
        }
    }

    private void transitionToGame() {
        Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
        startActivityForResult(intent, Constants.REQUEST_PLAY_GAME);
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
                String newStartDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);

                // Set the START device
                if (mGameData.getStartDevice() == NO_START) {
                    mGameData.setStartDevice(newStartDeviceAddress);

                    // Ack the new start device
                    mConnectService.write(Constants.HEADER_START_ACK, newStartDeviceAddress);
                } else {
                    String currentStartDevice;
                    if (mGameData.getStartDevice() == WE_ARE_START) {
                        currentStartDevice = mLocalAddress;
                    } else {
                        currentStartDevice = mGameData.getConnectedDevice(mGameData.getStartDevice());
                    }

                    // Check preference by address comparison
                    if (currentStartDevice.compareTo(newStartDeviceAddress) > 0) {
                        // Update our start device to be the one with the lower address
                        mGameData.setStartDevice(mGameData.getConnectedDevices().indexOf(newStartDeviceAddress));

                        // Re-ack the new start device
                        mConnectService.write(Constants.HEADER_START_ACK, newStartDeviceAddress);
                    } else {
                        // We are still start device, so this does not affect anything
                        return;
                    }
                }

                // Setup unplaced devices, i.e. all devices except for the start device and us
                unplacedDevices.clear();
                for (int i = 0; i < mGameData.getConnectedDevices().size(); i++) {
                    String currDevice = mGameData.getConnectedDevice(i);
                    if (!currDevice.equals(newStartDeviceAddress)) {
                        unplacedDevices.add(currDevice);
                    }
                }
                break;
            case READ_START_ACK:
                String deviceAddress = msg.getData().getString(DEVICE_ADDRESS);

                mGameData.removeUnackedDevice(deviceAddress);
                if (mGameData.doneAcking()) {
                    // Setup unplaced devices, i.e. all devices except for us
                    unplacedDevices.clear();
                    for (int i = 0; i < mGameData.getConnectedDevices().size(); i++) {
                        unplacedDevices.add(mGameData.getConnectedDevice(i));
                    }

                    chooseSuccessor();
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
                for (String address : mGameData.getConnectedDevices()) {
                    isUs = isUs && !pairedDeviceAddress.equals(address);
                }

                // If we are the start device and we are the newly paired device, start game
                if ((mGameData.getStartDevice() == -1 && isUs)
                        // If the loop has been completed and all devices have a successor, start game
                        || (mGameData.getStartDevice() != -1 && pairedDeviceAddress.equals(mGameData.getConnectedDevice(mGameData.getStartDevice())))) {
                    transitionToGame();
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
                    if (mGameData.getConnectedDevices().indexOf(addr) >= 0) {
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

                    mGameData.addConnectedDevice(deviceAddress, deviceName);
                    mConnectedDevicesNamesAdapter.notifyDataSetChanged();
                    Snackbar.make(mView, "Connected to " + deviceName, Snackbar.LENGTH_LONG).show();

                    if(GameData.connectionChangeListener != null) {
                        GameData.connectionChangeListener.connection(deviceAddress);
                    }
                    break;
                case MESSAGE_DISCONNECTED:
                    deviceName = msg.getData().getString(DEVICE_NAME);
                    deviceAddress = msg.getData().getString(DEVICE_ADDRESS);

                    mGameData.removeConnectedDevice(deviceAddress, deviceName);
                    mConnectedDevicesNamesAdapter.notifyDataSetChanged();
                    Snackbar.make(mView, "Disconnected from " + deviceName, Snackbar.LENGTH_LONG).show();

                    if(GameData.connectionChangeListener != null) {
                        GameData.connectionChangeListener.disconnection(deviceAddress);
                    }
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
