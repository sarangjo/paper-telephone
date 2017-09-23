package com.cse461.a16au.papertelephone.lobby;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.R;

/** A simple {@link Fragment} subclass. */
public class DiscoverableFragment extends Fragment {
  private static final int REQUEST_MAKE_DISCOVERABLE = 1;
  private static final String TAG = "DiscoverableFragment";

  private TextView discoverableView;

  private BroadcastReceiver mReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();

          if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
            int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
            switch (scanMode) {
              case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                discoverableView.setText(R.string.scan_mode_discoverable);
                break;
              case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                discoverableView.setText(R.string.scan_mode_connectable_only);
                break;
              case BluetoothAdapter.SCAN_MODE_NONE:
                discoverableView.setText(R.string.scan_mode_none);
                break;
            }
          }
        }
      };

  public DiscoverableFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_discoverable, container, false);

    discoverableView = (TextView) view.findViewById(R.id.view_discoverable);

    Button discoverable = (Button) view.findViewById(R.id.button_make_discoverable);
    discoverable.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            requestDiscoverable(300);
          }
        });

    return view;
  }

  @Override
  public void onStart() {
    super.onStart();

    IntentFilter discoverabilityChanged =
        new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
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
          Log.d(TAG, "Did not turn on discovery.");
        } else {
          Log.d(TAG, "Successfully turned on discovery.");
          //                    makeDiscoverableButton.setEnabled(false);
          //                    new CountDownTimer(resultCode * 1000, 1000) {
          //                        @Override
          //                        public void onTick(long millisUntilFinished) {
          //                            // TODO do something
          //                            timeDiscoverableButton.setText(String.format("Discoverable
          // for %d seconds", (millisUntilFinished / 1000)));
          //                        }
          //
          //                        @Override
          //                        public void onFinish() {
          //                            // TODO: do something
          //
          // timeDiscoverableButton.setText(getResources().getString(R.string.not_discoverable));
          //                            makeDiscoverableButton.setEnabled(true);
          //                        }
          //                    }.start();
        }
        break;
    }
  }
}
