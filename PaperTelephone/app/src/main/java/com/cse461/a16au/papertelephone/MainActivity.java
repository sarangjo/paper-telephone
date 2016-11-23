package com.cse461.a16au.papertelephone;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.Override;

public class MainActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Maybe implement onActivityResult()? I'm not entirely sure
        // https://developer.android.com/guide/topics/connectivity/bluetooth.html#EnablingDiscoverability
    }

    public void startDrawing(View view) {
        Intent intent = new Intent(this, DrawingActivity.class);
        startActivity(intent);
    }
}
