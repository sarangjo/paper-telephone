package com.cse461.a16au.papertelephone.game;

/**
 * Created by siddt on 11/29/2016.
 * TODO: documentation
 */

import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.BluetoothConnectService;
import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.cse461.a16au.papertelephone.game.GameData.*;

public class GameActivity extends FragmentActivity implements DataSendListener {
    private BluetoothConnectService mConnectService;
    private boolean isPromptMode;
    private GameFragment mFragment;
    private TextView mTimerTextView;

    private String prompt;
    private byte[] image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerGameHandler(mGameHandler);

        mTimerTextView = (TextView) findViewById(R.id.timer);

        isPromptMode = true;
        updateMode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mConnectService.unregisterGameHandler(mGameHandler);
    }

    /**
     * Switches modes and moves on to the next turn.
     */
    public void updateMode() {
        if (GameData.turnTimer != null) {
            GameData.turnTimer.cancel();
        }

        // Start the timer at 30 seconds for the next phase of the game
        GameData.turnTimer = new CountDownTimer(Constants.TURN_MILLIS, 1000) {
            public void onTick(long millisUntilFinished) {
                mTimerTextView.setText(String.format("%2ds", millisUntilFinished / 1000));
            }

            public void onFinish() {
                // End the turn, sending the drawing or prompt if it hasn't already been sent
                if (!isDone) {
                    mFragment.endTurn();
                    isDone = true;
                }
                mTimerTextView.setText("00s");
                mTimerTextView.setTextColor(Color.RED);
            }
        }.start();

        // Switch out the fragments to update the current mode
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mFragment != null) {
            ft.remove(mFragment);
        }
        Bundle args = new Bundle();
        if (isPromptMode) {
            if (mFragment == null) {
                args.putBoolean("start", true);
                mFragment = new PromptFragment();
                mFragment.setArguments(args);
            } else {
                args.putBoolean("start", false);
                // Add the image as an arg to the PromptFragment
                args.putByteArray("image", image);
                mFragment = new PromptFragment();
                mFragment.setArguments(args);
            }
        } else {
            // Pass the prompt to the DrawingFragment
            args.putString("prompt", prompt);
            mFragment = new DrawingFragment();
            mFragment.setArguments(args);
        }
        ft.add(R.id.game_fragment_container, mFragment).commit();

        // Switch modes and setup for start of the next turn
        isPromptMode = !isPromptMode;
        startTurn();
    }

    /**
     * Sets up our data structures for the start of a turn.
     */
    private void startTurn() {
        isDone = false;
        unfinishedDeviceList = new ArrayList<>(connectedDevices);
        mTimerTextView.setTextColor(getResources().getColor(R.color.colorTimer));
    }

    @Override
    public void sendData(byte[] data) {
        // Write this turn's data to our next device
        mConnectService.write(data, connectedDevices.get(nextDevice));

        // Write done message to all devices
        byte[] header = Constants.HEADER_DONE;
        ByteBuffer buf = ByteBuffer.allocate(header.length);
        buf.put(header);

        for (String device : connectedDevices) {
            mConnectService.write(buf.array(), device);
        }

        isDone = true;
        GameData.turnTimer.cancel();

        if (unfinishedDeviceList.isEmpty()) {
            updateMode();
        }
    }

    private final Handler mGameHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    switch (msg.arg2) {
                        case Constants.READ_IMAGE:
                            Toast.makeText(GameActivity.this, "Image received!", Toast.LENGTH_SHORT).show();
                            image = (byte[]) msg.obj;
                            if (isDone && unfinishedDeviceList.isEmpty()) {
                                updateMode();
                            }
                            break;
                        case Constants.READ_PROMPT:
                            Toast.makeText(GameActivity.this, "Text received!", Toast.LENGTH_SHORT).show();
                            prompt = new String((byte[]) msg.obj);
                            if (isDone && unfinishedDeviceList.isEmpty()) {
                                updateMode();
                            }
                            break;
                        case Constants.READ_DONE:
                            String name = msg.getData().getString(Constants.DEVICE_NAME);
                            Toast.makeText(GameActivity.this, name + " is done with their turn!", Toast.LENGTH_SHORT).show();

                            // Get address of sender and remove it from our list of devices
                            // keeping track of which devices haven't finished yet
                            String address = msg.getData().getString(Constants.DEVICE_ADDRESS);
                            unfinishedDeviceList.remove(address);

                            // If we are done and all other devices are done we move on to the
                            // next phase of the game by calling updateMode()
                            if (isDone && unfinishedDeviceList.isEmpty()) {
                                updateMode();
                            }
                            break;
                    }
                    break;
            }
        }
    };
}