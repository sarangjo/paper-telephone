package com.cse461.a16au.papertelephone.lobby;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.R;

/**
 * TODO: class comment
 */
public class DevicesFragment extends Fragment {
    private static final String TAG = "DevicesFragment";

    private DevicesListAdapter mDevicesAdapter;
    private ProgressBar mScanProgress;
    private Button mScanButton;

    private BluetoothAdapter mBluetoothAdapter;

    private ConnectDeviceListener mConnectDeviceListener;

    public DevicesFragment() {
        // Required empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Register for broadcasts
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(mReceiver, filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        mScanProgress = (ProgressBar) view.findViewById(R.id.scanning_progress_bar);
        mScanProgress.setIndeterminate(true);

        mScanButton = (Button) view.findViewById(R.id.button_scan_devices);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Look for devices
                doDiscovery();
            }
        });

        ListView list = (ListView) view.findViewById(R.id.list_new_devices);
        mDevicesAdapter = new DevicesListAdapter(getActivity());
        list.setAdapter(mDevicesAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Cancel discovery because it's costly and we're about to connect
                cancelDiscovery();
                mConnectDeviceListener.connectDevice(mDevicesAdapter.getItem(position).getAddress());
            }
        });

        // Start discovery right away
        doDiscovery();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cancelDiscovery();

        // Unregister broadcast listeners
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ConnectDeviceListener) {
            mConnectDeviceListener = (ConnectDeviceListener) context;
        } else {
            throw new UnsupportedOperationException("Must be DeviceChosenListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mConnectDeviceListener = null;
    }

    public void cancelDiscovery() {
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mScanButton.setEnabled(true);
        mScanProgress.setVisibility(View.GONE);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        mScanButton.setEnabled(false);
        mScanProgress.setVisibility(View.VISIBLE);

//        mOldDevicesAdapter.clearNew();
        mDevicesAdapter.clear();

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevicesAdapter.add(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                cancelDiscovery();
            }
        }
    };

    public interface ConnectDeviceListener {
        void connectDevice(String address);
    }

    private class DevicesListAdapter extends ArrayAdapter<BluetoothDevice> {
        DevicesListAdapter(Context context) {
            super(context, -1);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            TextView text = (TextView) view.findViewById(android.R.id.text1);
            BluetoothDevice device = getItem(position);
            text.setText(device.getName() + "\n" + device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                text.setTextColor(getResources().getColor(R.color.colorPairedDevice));
            }
            return view;
        }
    }
}
