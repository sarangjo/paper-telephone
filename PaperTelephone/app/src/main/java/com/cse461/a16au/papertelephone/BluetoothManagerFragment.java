package com.cse461.a16au.papertelephone;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by jsunde on 11/18/2016.
 * Use this file for reference in writing the bluetooth related logic
 * https://github.com/googlesamples/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/BluetoothChatFragment.java
 * TODO: Add discoverability logic maybe?
 */

public class BluetoothManagerFragment extends Fragment {
    private static final String TAG = "BluetoothStartFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_MAKE_DISCOVERABLE = 3;
    private static final int REQUEST_GET_DRAWING = 4;
    private static final int NEXTDEVICE = 0; // TODO: needs to be set to indicate the next device after "START" logic
    private static int startDevice = -1;
    private static Set<String> unplacedDevices = new HashSet<>();
    private static int lastPair = 0;

    // Names of connected devices
    private ArrayAdapter<String> connectedDevicesAdapter;

    // Local Bluetooth adapter
    private BluetoothAdapter bluetoothAdapter = null;

    // Bluetooth Connection Service to handle connections to other devices
    private BluetoothConnectService connectService = null;

    // Views
    private Button makeDiscoverableButton;
    private TextView timeDiscoverableButton;
    private ImageView receivedImageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Display error message in the case that the device does not support bluetooth
        if (bluetoothAdapter == null) {
            FragmentActivity activity = (FragmentActivity) getActivity();
            Toast.makeText(activity, "Bluetooth is not available. Our game needs bluetooth to work... Sorry", Toast.LENGTH_LONG).show();
            // activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Request that bluetooth be enabled if it is disabled, otherwise connect to other devices
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getActivity(), "Requesting to enable Bluetooth...", Toast.LENGTH_LONG).show();
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (connectService == null) {
            setupGame();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop connect service to stop connections and shut down
        if (connectService != null) {
            connectService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // In the case that Bluetooth was disabled to start, onResume() will
        // be called when the ACTION_REQUEST_ENABLE activity has returned
        // TODO: connect to other devices here
        if (connectService != null) {
            if (connectService.getState() == BluetoothConnectService.STATE_STOPPED) {
                // Start our BluetoothConnectionService
                connectService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth_start, container, false);

        Button showDevices = (Button) view.findViewById(R.id.button_show_devices);
        showDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListDeviceActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
            }
        });

        makeDiscoverableButton = (Button) view.findViewById(R.id.button_make_discoverable);
        makeDiscoverableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDiscoverable(300);
            }
        });

        timeDiscoverableButton = (TextView) view.findViewById(R.id.title_discoverable_time);
        timeDiscoverableButton.setText(getResources().getString(R.string.not_discoverable));

        // Connected devices list
        connectedDevicesAdapter =
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);

        // Two ListViews for paired and new devices
        ListView connectedListView = (ListView) view.findViewById(R.id.list_connected_devices);
        connectedListView.setAdapter(connectedDevicesAdapter);
        connectedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!connectedDevicesAdapter.isEmpty()) {
                    connectService.write(Constants.PING, connectedDevicesAdapter.getItem(position));
                }
            }
        });

        // Going to drawing
        Button startDrawing = (Button) view.findViewById(R.id.button_start_drawing);
        startDrawing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DrawingActivity.class);
                startActivityForResult(intent, REQUEST_GET_DRAWING);
            }
        });

        // Holding on to image view
        receivedImageView = (ImageView) view.findViewById(R.id.image_received_image);

        return view;
    }

    @Override
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
                    Toast.makeText(getActivity(), "Successfully enabled Bluetooth.", Toast.LENGTH_LONG).show();
                    setupGame();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_exit,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            case REQUEST_MAKE_DISCOVERABLE:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(getActivity(), "Did not turn on discovery.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "Successfully turned on discovery.", Toast.LENGTH_LONG).show();
                    makeDiscoverableButton.setEnabled(false);
                    // Handy dandy countdown timer to show on the screen
                    new CountDownTimer(resultCode * 1000, 1000) {

                        @Override
                        public void onTick(long millisUntilFinished) {
                            timeDiscoverableButton.setText(String.format("Discoverable for %d seconds", (millisUntilFinished / 1000)));
                        }

                        @Override
                        public void onFinish() {
                            timeDiscoverableButton.setText(getResources().getString(R.string.not_discoverable));
                            makeDiscoverableButton.setEnabled(true);
                        }
                    }.start();
                }
                break;
            case REQUEST_GET_DRAWING:
                sendDrawing(data);
                break;
        }
    }

    private void sendDrawing(Intent data) {
        // First send header to indicate we're sending an image
        byte[] header = Constants.HEADER_IMAGE;

        if (data != null) {
            // Get byte array from intent
            byte[] img = data.getByteArrayExtra(DrawingActivity.EXTRA_IMAGE_DATA);
            if (img != null) {
                ByteBuffer buf = ByteBuffer.allocate(header.length + img.length + 4);

                buf.put(header);
                buf.putInt(img.length);
                buf.put(img);
                // Write it through the service
                connectService.write(buf.array(), connectedDevicesAdapter.getItem(NEXTDEVICE));
                return;
            }
        }
        Toast.makeText(getActivity(), "Please submit a drawing.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Establishes an ordering for the connected devices when the user hits the start button
     *
     */
    private void start() {
        int connectedDevices = connectedDevicesAdapter.getCount();
        if(connectedDevices < 2) {
            Toast.makeText(getActivity(), "You don't have enough players, the game requires" +
               "at least 3 players", Toast.LENGTH_LONG).show();
        } else {
            byte[] startMsg = Constants.HEADER_START;

            for(int i = 0; i < connectedDevices; i++) {
               String currDevice = connectedDevicesAdapter.getItem(i);
               connectService.write(startMsg, currDevice);
            }

            // Sleep for a short time in case another device pressed start at the same time
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            if(startDevice == -1 || bluetoothAdapter.getAddress().compareTo(connectedDevicesAdapter.getItem(startDevice)) < 0) {
                for(int i = 0; i < connectedDevicesAdapter.getCount(); i++) {
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
        String nextDevice;
        if(iter.hasNext()) {
            nextDevice = iter.next();

            // Remove device from set of unplaced devices
            iter.remove();
        } else {
            nextDevice = connectedDevicesAdapter.getItem(startDevice);
        }


        ByteBuffer msg = ByteBuffer.allocate(26);
        msg.put(Constants.HEADER_START);
        msg.putInt(lastPair + 1);
        msg.put(nextDevice.getBytes());

        for(int i = 0; i < connectedDevices; i++) {
            String currDevice = connectedDevicesAdapter.getItem(i);
            connectService.write(msg.array(), currDevice);
        }
    }


    /**
     * Make the device discoverable for the specified time.
     */
    private void requestDiscoverable(int seconds) {
        IntentFilter discoverabilityChanged = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        getActivity().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                    int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                    switch (scanMode) {
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            Toast.makeText(getActivity(), "SCAN MODE CHANGED TO DISCOVERABLE", Toast.LENGTH_LONG).show();
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            Toast.makeText(getActivity(), "SCAN MODE CHANGED TO CONNECTABLE ONLY", Toast.LENGTH_LONG).show();
                            break;
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            Toast.makeText(getActivity(), "SCAN MODE CHANGED TO NONE", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }
        }, discoverabilityChanged);

        // Enable bluetooth simply by enabling bluetooth discoverability
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
        startActivityForResult(discoverableIntent, REQUEST_MAKE_DISCOVERABLE);
    }

    private void setupGame() {
        // TODO: implement
        connectService = new BluetoothConnectService(mHandler);
    }

    /**
     * After the device has been selected, connect to the device.
     *
     * @param data the response
     */
    private void connectDevice(Intent data) {
        // Get the address of the device
        String addr = data.getExtras().getString(ListDeviceActivity.DEVICE_ADDRESS);

        // Get the BluetoothDevice
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);

        // Connect to device
        connectService.connect(device);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_CONNECTED:
                    String deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    String deviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);
                    connectedDevicesAdapter.add(deviceAddress);
                    Toast.makeText(getActivity(), "Connected to " + deviceName, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_DISCONNECTED:
                    deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    deviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);
                    connectedDevicesAdapter.remove(deviceAddress);
                    Toast.makeText(getActivity(), "Disconnected from " + deviceName, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_WRITE:
                    // TODO: do something with what we write out?
                    Toast.makeText(getActivity(), "Sent data", Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_READ:
                    switch (msg.arg2) {
                        case Constants.READ_IMAGE:
                            Toast.makeText(getActivity(), "Image incoming...", Toast.LENGTH_SHORT).show();
                            processImage((byte[]) msg.obj);
                            break;
                        case Constants.READ_TEXT:
                            Toast.makeText(getActivity(), "Received ping", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.MESSAGE_RECV_START:
                            if(msg.arg1 == 5) {
                                String startDeviceAddress = msg.getData().getString(Constants.DEVICE_ADDRESS);
                                startDevice = connectedDevicesAdapter.getPosition(startDeviceAddress);
                                for(int i = 0; i < connectedDevicesAdapter.getCount(); i++) {
                                    String currDevice = connectedDevicesAdapter.getItem(i);
                                    if(!currDevice.equals(startDeviceAddress)) {
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
                                if(!unplacedDevices.remove(new String(pairedDeviceAddress))) {
                                    chooseSuccessor();
                                }
                            }
                            break;
                    }

                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getActivity(), "Toast: " + msg.getData().getString(Constants.TOAST), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    /**
     * Process an array of bytes into a bitmap and display it in the view
     *
     * @param data array of bytes containing image information
     */
    private void processImage(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                data.length);
        receivedImageView.setImageBitmap(bitmap);
    }
}
