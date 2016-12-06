package com.cse461.a16au.papertelephone;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class BluetoothManagerFragment extends Fragment {
    private static final String TAG = "BluetoothStartFragment";

    private static int nextDevice = 0;
    private static int startDevice = -1;
    private static Set<String> unplacedDevices = new HashSet<>();
    private static int lastPair = 0;

    // Local Bluetooth adapter
    private BluetoothAdapter bluetoothAdapter = null;

    // Bluetooth Connection Service to handle connections to other devices
    private BluetoothConnectService connectService = null;

    // Views
    private Button makeDiscoverableButton;
    private TextView timeDiscoverableButton;

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
    public void onResume() {
        super.onResume();

        // In the case that Bluetooth was disabled to start, onResume() will
        // be called when the ACTION_REQUEST_ENABLE activity has returned
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
//                Intent intent = new Intent(getActivity(), ListDeviceActivity.class);
//                startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
            }
        });

        timeDiscoverableButton = (TextView) view.findViewById(R.id.title_discoverable_time);
        timeDiscoverableButton.setText(getResources().getString(R.string.not_discoverable));

        // Going to drawing
        Button startDrawing = (Button) view.findViewById(R.id.button_start_drawing);
        startDrawing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(getActivity(), DrawingActivity.class);
//                startActivityForResult(intent, Constants.REQUEST_GET_DRAWING);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case Constants.REQUEST_GET_DRAWING:
                sendDrawing(data);
                break;
        }
    }

    private void sendDrawing(Intent data) {
        // First send header to indicate we're sending an image
        byte[] header = Constants.HEADER_IMAGE;

        if (data != null) {
            // Get byte array from intent
            byte[] img = data.getByteArrayExtra(Constants.EXTRA_IMAGE_DATA);
            if (img != null) {
                ByteBuffer buf = ByteBuffer.allocate(header.length + img.length + 4);

                buf.put(header);
                buf.putInt(img.length);
                buf.put(img);
                // Write it through the service
                connectService.write(buf.array(), ""/*TODO: replace with connectedDevices.get(nextDevice)*/);
                return;
            }
        }
        Toast.makeText(getActivity(), "Please submit a drawing.", Toast.LENGTH_SHORT).show();
    }


}
