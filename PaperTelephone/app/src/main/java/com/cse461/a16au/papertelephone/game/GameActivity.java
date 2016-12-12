package com.cse461.a16au.papertelephone.game;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.BluetoothConnectService;
import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.cse461.a16au.papertelephone.game.GameData.addressToSummaries;
import static com.cse461.a16au.papertelephone.game.GameData.isDone;
import static com.cse461.a16au.papertelephone.game.GameData.nextDevice;
import static com.cse461.a16au.papertelephone.game.GameData.saveData;
import static com.cse461.a16au.papertelephone.game.GameData.turnsLeft;
import static com.cse461.a16au.papertelephone.game.GameData.unfinishedDeviceList;

/**
 * Manages the two fragments that compose the game, DrawingFragment, and PromptFragment and handles
 * the receiving of game data from other devices in order to save it for the next phase of the game.
 */
public class GameActivity extends AppCompatActivity implements GameFragment.DataSendListener {
    private final String TAG = "GAME_ACTIVITY";

    private BluetoothConnectService mConnectService;
    private GameData mGameData;
    private boolean mIsPromptMode;

    private GameFragment mFragment;
    private TextView mTimerTextView;
    private ProgressDialog mProgressDialog;

    // Details for the next turn
    private String mNextPrompt;
    private byte[] mNextImage;
    private String mNextCreatorAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mGameData = GameData.getInstance();

        GameData.connectionChangeListener = new ConnectionChangeListener() {
            @Override
            public void disconnection(String address) {

            }

            @Override
            public void connection(String address) {

            }
        };

        // Set up game data
        turnsLeft = mGameData.getConnectedDevices().size() + 1;
        addressToSummaries = new ConcurrentHashMap<>();

        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerGameHandler(mGameHandler);

        mTimerTextView = (TextView) findViewById(R.id.timer);
        TextView successor = (TextView) findViewById(R.id.successor_text);
        if (nextDevice >= 0) {
            successor.setText("Next: " + mGameData.getConnectedDeviceNames().get(nextDevice));
        } else {
            successor.setText("Invalid successor");
        }

        mIsPromptMode = true;
        mNextCreatorAddress = null;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Waiting for Other Players to Complete Their Turn");
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

        mProgressDialog.dismiss();

        // Finish game
        if (turnsLeft == 0) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        // TODO: Uncomment and test when we are confident that the rest of the game works as intended
        // Start the timer at 30 seconds for the next phase of the game
        /*GameData.turnTimer = new CountDownTimer(Constants.TURN_MILLIS, 1000) {
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
        }.start();*/

        // Switch out the fragments to update the current mode
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mFragment != null) {
            ft.remove(mFragment);
        }
        Bundle args = new Bundle();

        // Set creatorAddress argument
        args.putString(Constants.CREATOR_ADDRESS, mNextCreatorAddress);
        if (mIsPromptMode) {
            if (mFragment == null) {
                args.putBoolean("start", true);
                mFragment = new PromptFragment();
                mFragment.setArguments(args);
            } else {
                args.putBoolean("start", false);
                // Add the image as an arg to the PromptFragment
                args.putByteArray("image", mNextImage);
                mFragment = new PromptFragment();
                mFragment.setArguments(args);
            }
        } else {
            // Pass the prompt to the DrawingFragment
            args.putString("prompt", mNextPrompt);
            mFragment = new DrawingFragment();
            mFragment.setArguments(args);
        }
        ft.add(R.id.game_fragment_container, mFragment).commit();

        // Switch modes and setup for start of the next turn
        mIsPromptMode = !mIsPromptMode;
        startTurn();
    }

    /**
     * Sets up our data structures for the start of a turn.
     */
    private void startTurn() {
        isDone = false;
        unfinishedDeviceList = new ConcurrentSkipListSet<>(mGameData.getConnectedDevices());
        mTimerTextView.setTextColor(getResources().getColor(R.color.colorTimer));

        turnsLeft--;
    }

    /**
     * Sends data (image or prompt) to the "next device"
     * Also sends a DONE packet to all devices in order to inform them that we have completed our turn
     */
    @Override
    public void sendData(byte[] data) {
        // Write this turn's data to our next device
        // THIS APPARENTLY MODIFIES THE PASSED-IN ARRAY??????????????????????!?!?!?!?!?!?!?
        mConnectService.write(Arrays.copyOf(data, data.length), mGameData.getConnectedDevice(nextDevice));
        Log.d(TAG, "Sent Image/Prompt");

        // Write done message to all devices
        ByteBuffer buf = ByteBuffer.allocate(Constants.HEADER_LENGTH + data.length);
        buf.put(Constants.HEADER_DONE);
        buf.put(data);

        for (String device : mGameData.getConnectedDevices()) {
            if (!device.equals(mGameData.getConnectedDevice(nextDevice))) {
                mConnectService.write(buf.array(), device);
                Log.d(TAG, "Sent Done");
            }
        }

        isDone = true;
        if (GameData.turnTimer != null) {
            GameData.turnTimer.cancel();
        }

        mProgressDialog.show();
        if (unfinishedDeviceList.isEmpty()) {
            updateMode();
        }
    }

    /**
     * Handles packets that the game receives from BluetoothConnectService
     */
    private final Handler mGameHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    // TODO: MAKE SURE EVERYTHING *HERE* IS THREAD-SAFE

                    // Add the current image/prompt to the corresponding list in the map from addresses to summaries
                    String creatorAddress = msg.getData().getString(Constants.CREATOR_ADDRESS);
                    byte[] data = (byte[]) msg.obj;
                    saveData(creatorAddress, data);

                    switch (msg.arg2) {
                        case Constants.READ_IMAGE:
                            Toast.makeText(GameActivity.this, "Image received!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Read Image");
                            mNextImage = (byte[]) msg.obj;
                            mNextCreatorAddress = creatorAddress;
                            break;
                        case Constants.READ_PROMPT:
                            Toast.makeText(GameActivity.this, "Prompt received!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Read Prompt");
                            mNextPrompt = new String((byte[]) msg.obj);
                            mNextCreatorAddress = creatorAddress;
                            break;
                        case Constants.READ_DONE:
                            String name = msg.getData().getString(Constants.DEVICE_NAME);
                            Toast.makeText(GameActivity.this, name + " is done with their turn!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Read Done");
                            break;
                    }

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
                case Constants.MESSAGE_DISCONNECTED:
                    if(mGameData.getConnectedDevices().size() <= 2);
                    break;
            }
        }
    };
}