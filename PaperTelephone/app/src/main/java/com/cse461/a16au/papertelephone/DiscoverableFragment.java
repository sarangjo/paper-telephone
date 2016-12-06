package com.cse461.a16au.papertelephone;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;


/**
 * A simple {@link Fragment} subclass.
 */
public class DiscoverableFragment extends Fragment {
    private static final int REQUEST_MAKE_DISCOVERABLE = 1;

    private TextView discoverableView;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                switch (scanMode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        discoverableView.setText("SCAN MODE DISCOVERABLE");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        discoverableView.setText("SCAN MODE CONNECTABLE ONLY");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        discoverableView.setText("SCAN MODE NONE");
                        break;
                }
            }
        }
    };

    public DiscoverableFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DiscoverableFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DiscoverableFragment newInstance() {
        return new DiscoverableFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestDiscoverable(300);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_discoverable, container, false);

        discoverableView = (TextView) view.findViewById(R.id.view_discoverable);

        return view;
    }

    @Override
     public void onStart() {
        super.onStart();

        IntentFilter discoverabilityChanged = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        getActivity().registerReceiver(mReceiver, discoverabilityChanged);
    }

    @Override
    public void onStop() {
        super.onStop();

        getActivity().unregisterReceiver(mReceiver);
    }

    private void requestDiscoverable(int seconds) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
        startActivityForResult(discoverableIntent, REQUEST_MAKE_DISCOVERABLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_MAKE_DISCOVERABLE:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(getActivity(), "Did not turn on discovery.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "Successfully turned on discovery.", Toast.LENGTH_LONG).show();
//                    makeDiscoverableButton.setEnabled(false);
                    new CountDownTimer(resultCode * 1000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            // TODO do something
                            //timeDiscoverableButton.setText(String.format("Discoverable for %d seconds", (millisUntilFinished / 1000)));
                        }

                        @Override
                        public void onFinish() {
                            // TODO: do something
                            //timeDiscoverableButton.setText(getResources().getString(R.string.not_discoverable));
                            //makeDiscoverableButton.setEnabled(true);
                        }
                    }.start();
                }
                break;
        }
    }
}
