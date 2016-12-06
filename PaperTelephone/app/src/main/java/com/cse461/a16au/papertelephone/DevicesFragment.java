package com.cse461.a16au.papertelephone;

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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Set;


/**
 * TODO: class comment
 * Use the {@link DevicesFragment#newInstance} factory method to
 * create an instance of this fragment.
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

    private BluetoothAdapter mBluetoothAdapter;

    private DeviceChosenListener mDeviceChosenListener;

    public DevicesFragment() {
        // Required empty constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment DevicesFragment.
     */
    public static DevicesFragment newInstance() {
        return new DevicesFragment();
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
        ((ExpandableListView) view.findViewById(R.id.list_potential_devices)).setAdapter(mDevicesAdapter);

        Button scanButton = (Button) view.findViewById(R.id.button_scan_devices);
        scanButton.setOnClickListener(new View.OnClickListener() {
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
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DeviceChosenListener) {
            mDeviceChosenListener = (DeviceChosenListener) context;
        } else {
            throw new UnsupportedOperationException("Must be DeviceChosenListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDeviceChosenListener = null;
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        mScanProgress.setIndeterminate(true);
        mScanProgress.setVisibility(View.VISIBLE);

        mDevicesAdapter.clearNew();

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBluetoothAdapter.cancelDiscovery();

            // TODO Gross, get the MAC address it from the device not from the string
            String info = ((TextView) v).getText().toString();

            mDeviceChosenListener.deviceChosen(info.substring(info.length() - Constants.ADDRESS_LENGTH));
        }
    };

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

    public interface DeviceChosenListener {
        void deviceChosen(String address);
    }
}
