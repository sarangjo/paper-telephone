package com.cse461.a16au.papertelephone.lobby;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;

import java.util.Set;

/**
 * TODO: class comment
 */
public class DevicesFragment extends Fragment {
    private static final String TAG = "DevicesFragment";

    /**
     * TODO
     */
    private DevicesListAdapter mDevicesAdapter;

    /**
     * TODO
     */
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        mScanProgress = (ProgressBar) view.findViewById(R.id.scanning_progress_bar);

        mDevicesAdapter = new DevicesListAdapter(getActivity());

        // Register for broadcasts
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(mReceiver, filter);

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        for (BluetoothDevice device : pairedDevices) {
            mDevicesAdapter.addPairedDevice(device.getName() + "\n" + device.getAddress());
        }

        // Expandable list view
        ExpandableListView list = (ExpandableListView) view.findViewById(R.id.list_potential_devices);
        list.setAdapter(mDevicesAdapter);
        list.setOnChildClickListener(mDeviceClickListener);

        mScanButton = (Button) view.findViewById(R.id.button_scan_devices);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Look for devices
                doDiscovery();
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
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
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }


    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        // If we're already discovering, stop it
        cancelDiscovery();

        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        mScanProgress.setIndeterminate(true);
        mScanProgress.setVisibility(View.VISIBLE);

        mDevicesAdapter.clearNew();

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
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED && device.getName() != null) {
                    mDevicesAdapter.addNewDevice(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mScanProgress.setVisibility(View.GONE);
            }
        }
    };

    private ExpandableListView.OnChildClickListener mDeviceClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            // Cancel discovery because it's costly and we're about to connect
            cancelDiscovery();

            // TODO Gross, get the MAC address from the BluetoothDevice object, not from the string
            String info = ((TextView) v).getText().toString();

            mConnectDeviceListener.connectDevice(info.substring(info.length() - Constants.ADDRESS_LENGTH));

            return true;
        }
    };

    public interface ConnectDeviceListener {
        void connectDevice(String address);
    }
}
