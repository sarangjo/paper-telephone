package com.cse461.a16au.papertelephone.game;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.BluetoothConnectService;
import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.cse461.a16au.papertelephone.Constants.ADDRESS_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.CREATOR_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_NAME;
import static com.cse461.a16au.papertelephone.Constants.HEADER_DONE;
import static com.cse461.a16au.papertelephone.Constants.HEADER_DTG;
import static com.cse461.a16au.papertelephone.Constants.HEADER_GIVE_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.HEADER_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.HEADER_NEW_START;
import static com.cse461.a16au.papertelephone.Constants.HEADER_REQUEST_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.HEADER_RESPONSE_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.JOIN_MID_GAME;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_READ;
import static com.cse461.a16au.papertelephone.Constants.READ_DONE;
import static com.cse461.a16au.papertelephone.Constants.READ_DTG;
import static com.cse461.a16au.papertelephone.Constants.READ_GIVE_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.READ_IMAGE;
import static com.cse461.a16au.papertelephone.Constants.READ_NEW_START;
import static com.cse461.a16au.papertelephone.Constants.READ_PROMPT;
import static com.cse461.a16au.papertelephone.Constants.READ_REQUEST_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.READ_RESPONSE_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.WE_ARE_START;
import static com.cse461.a16au.papertelephone.game.GameData.addressToSummaries;
import static com.cse461.a16au.papertelephone.game.GameData.devicesAtStartGame;
import static com.cse461.a16au.papertelephone.game.GameData.doesEndOnPrompt;
import static com.cse461.a16au.papertelephone.game.GameData.namesAtStartGame;
import static com.cse461.a16au.papertelephone.game.GameData.saveData;
import static com.cse461.a16au.papertelephone.game.GameData.turnsLeft;

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
    private ProgressDialog mWaitingDialog;

    // Details for the next turn
    private String mNextPrompt;
    private byte[] mNextImage;
    private String mNextCreatorAddress;
    private TextView mSuccessorView;
    private List<String> successors;
    private byte[] mDoneMsg;

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
                // Device was dropped in the middle of the game
                if (mGameData.getConnectedDevices().size() < 2) {
                    mProgressDialog.dismiss();
                    Toast.makeText(GameActivity.this, "You no longer have enough players, going to summary page", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                    return;
                }

                mGameData.deviceTurnFinished(address);

                // In the case that the dropped address was our successor, we have to get a new successor
                if (GameData.successor.equals(address)) {
                    successors = new CopyOnWriteArrayList<>();

                    // Ask other devices for their successors
                    for (String device : mGameData.getConnectedDevices()) {
                        mConnectService.write(HEADER_REQUEST_SUCCESSOR, device);
                    }
                }
            }

            @Override
            public void connection(String address) {
                mGameData.addUnfinishedDevice(address);

                // New connection made, let it be our successor
                if (mGameData.getStartDevice().equals(WE_ARE_START)) {
                    ByteBuffer buf = ByteBuffer.allocate(Constants.HEADER_LENGTH + Constants.ADDRESS_LENGTH + 1 + 1);
                    buf.put(HEADER_GIVE_SUCCESSOR);
                    buf.put(GameData.successor.getBytes());
                    buf.put((byte) (mIsPromptMode ? 1 : 0));
                    buf.put((byte) (GameData.doesEndOnPrompt ? 1 : 0));

                    mConnectService.write(buf.array(), address);

                    // Send our current prompt/image to the new device
                    if (mDoneMsg != null) {
                        byte[] dataMsg = Arrays.copyOfRange(mDoneMsg, HEADER_LENGTH, mDoneMsg.length);
                        mConnectService.write(dataMsg, address);
                    }

                    GameData.successor = address;
                    mSuccessorView.setText("Next: " + mGameData.getConnectedDeviceNames().get(mGameData.getConnectedDevices().indexOf(GameData.successor)));
                } else {
                    if (mDoneMsg != null)
                        mConnectService.write(mDoneMsg, address);
                }
            }
        };

        // Set up game data
        addressToSummaries = new ConcurrentHashMap<>();
        devicesAtStartGame = new ArrayList<>(mGameData.getConnectedDevices());
        namesAtStartGame = new ArrayList<>(mGameData.getConnectedDeviceNames());

        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerGameHandler(mGameHandler);

        mTimerTextView = (TextView) findViewById(R.id.timer);
        mSuccessorView = (TextView) findViewById(R.id.successor_text);
        if (GameData.successor != null) {
            mSuccessorView.setText("Next: " + mGameData.getConnectedDeviceNames().get(mGameData.getConnectedDevices().indexOf(GameData.successor)));
        } else {
            mSuccessorView.setText("Invalid successor");
        }

        mNextCreatorAddress = null;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Waiting for Other Players to Complete Their Turn");
        mProgressDialog.setCancelable(false);

        if (getIntent().getBooleanExtra(JOIN_MID_GAME, false)) {
            // joining mid-game
            for (String device : mGameData.getConnectedDevices()) {
                mConnectService.write(HEADER_DTG, device);
            }

            mWaitingDialog = new ProgressDialog(this);
            mWaitingDialog.setMessage("Waiting to join game...");
            mWaitingDialog.setCancelable(false);
            mWaitingDialog.show();

            startRound();
        } else {
            // starting new game
            doesEndOnPrompt = mGameData.getConnectedDevices().size() % 2 == 0;
            turnsLeft = mGameData.getConnectedDevices().size() + 1;
            mIsPromptMode = true;

            updateMode();
            startRound();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mConnectService.unregisterGameHandler(mGameHandler);
    }

    @Override
    public void onBackPressed() {
        // TODO: ask for confirmation first
        // TODO: intentionally disconnect from other devices to signal leaving from gaming
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
//        GameData.turnTimer = new CountDownTimer(TURN_MILLIS, 1000) {
//            public void onTick(long millisUntilFinished) {
//                mTimerTextView.setText(String.format("%2ds", millisUntilFinished / 1000));
//            }
//
//            public void onFinish() {
//                // End the turn, sending the drawing or prompt if it hasn't already been sent
//                if (!isDone) {
//                    mFragment.endTurn();
//                    isDone = true;
//                }
//                mTimerTextView.setText("00s");
//                mTimerTextView.setTextColor(Color.RED);
//            }
//        }.start();

        // Switch out the fragments to update the current mode
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (mFragment != null) {
            ft.remove(mFragment);
        }
        Bundle args = new Bundle();

        // Set creatorAddress argument
        args.putString(CREATOR_ADDRESS, mNextCreatorAddress);

        // Check if this is the start of the game or not
        if (mFragment == null) {
            args.putBoolean("start", true);
        } else {
            args.putBoolean("start", false);
        }
        if (mIsPromptMode) {
            // Pass the image as an arg to the PromptFragment
            args.putByteArray("image", mNextImage);
            mFragment = new PromptFragment();
        } else {
            // Pass the prompt to the DrawingFragment
            args.putString("prompt", mNextPrompt);
            mFragment = new DrawingFragment();
        }
        mFragment.setArguments(args);
        ft.add(R.id.game_fragment_container, mFragment).commit();

        // In preparation for the next round
        mIsPromptMode = !mIsPromptMode;
    }

    /**
     * Sets up our data structures for the start of a turn.
     */
    private void startRound() {
        mDoneMsg = null;
        mGameData.setTurnDone(false);
        mGameData.setupUnfinishedDevices(mGameData.getConnectedDevices());
        mTimerTextView.setTextColor(getResources().getColor(R.color.colorTimer));

        turnsLeft--;
    }

    /**
     * Sends data (image or prompt) to our successor. Also sends a DONE packet to all devices in order
     * to inform them that we have completed our turn.
     */
    @Override
    public void sendTurnData(byte[] data) {
        // Write this turn's data to our next device
        mConnectService.write(Arrays.copyOf(data, data.length), GameData.successor);

        // Write done message to all devices
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + data.length);
        buf.put(HEADER_DONE);
        buf.put(data);

        mDoneMsg = buf.array();

        for (String device : mGameData.getConnectedDevices()) {
            if (!device.equals(GameData.successor)) {
                mConnectService.write(mDoneMsg, device);
            }
        }

        synchronized (this) {
            mGameData.setTurnDone(true);
            if (GameData.turnTimer != null) {
                GameData.turnTimer.cancel();
            }

            mProgressDialog.show();
            if (mGameData.isRoundOver()) {
                updateMode();
                startRound();
            }
        }
    }


    /**
     * Handles packets that the game receives from BluetoothConnectService
     */
    private final Handler mGameHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_READ) {
                String address = msg.getData().getString(DEVICE_ADDRESS);
                String name = msg.getData().getString(DEVICE_NAME);

                switch (msg.arg2) {
                    case READ_IMAGE:
                    case READ_PROMPT:
                    case READ_DONE:
                        String creatorAddress = msg.getData().getString(CREATOR_ADDRESS);

                        // Add the current image/prompt to the corresponding list in the map from addresses to summaries
                        byte[] data = (byte[]) msg.obj;
                        saveData(creatorAddress, data);
                        Toast.makeText(GameActivity.this, name + " is done with their turn!", Toast.LENGTH_SHORT).show();

                        if (msg.arg2 == READ_IMAGE) {
                            mNextImage = data;
                            mNextCreatorAddress = creatorAddress;
                        } else if (msg.arg2 == READ_PROMPT) {
                            mNextPrompt = new String(data);
                            mNextCreatorAddress = creatorAddress;
                        }

                        synchronized (GameActivity.this) {
                            mGameData.deviceTurnFinished(address);

                            // If we are done and all other devices are done we move on to the next round
                            if (mGameData.isRoundOver()) {
                                updateMode();
                                startRound();
                            }
                        }
                        break;
                    case READ_REQUEST_SUCCESSOR:
                        // A device is requesting our successor, so just send it
                        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + ADDRESS_LENGTH);
                        buf.put(HEADER_RESPONSE_SUCCESSOR);
                        buf.put(GameData.successor.getBytes());

                        mConnectService.write(buf.array(), address);
                        break;
                    case READ_RESPONSE_SUCCESSOR:
                        // Getting the successors from all the other devices
                        buf = ByteBuffer.wrap((byte[]) msg.obj);
                        byte[] successor = new byte[ADDRESS_LENGTH];
                        buf.get(successor);
                        String successorAddress = new String(successor);

                        successors.add(successorAddress);

                        if (successors.size() == mGameData.getConnectedDevices().size()) {
                            Set<String> unplacedSuccessors = new HashSet<>(mGameData.getConnectedDevices());

                            for (String device : successors) {
                                unplacedSuccessors.remove(device);
                            }

                            if (GameData.successor.equals(mGameData.getStartDevice())) {
                                for (String device : mGameData.getConnectedDevices()) {
                                    mConnectService.write(HEADER_NEW_START, device);
                                }
                            }

                            GameData.successor = unplacedSuccessors.iterator().next();
                            mSuccessorView.setText("Next: " + mGameData.getConnectedDeviceNames().get(mGameData.getConnectedDevices().indexOf(GameData.successor)));

                            // Send our current prompt/image to the new successor
                            if (mDoneMsg != null) {
                                byte[] dataMsg = Arrays.copyOfRange(mDoneMsg, HEADER_LENGTH, mDoneMsg.length);
                                mConnectService.write(dataMsg, GameData.successor);
                            }
                        }

                        break;
                    case READ_NEW_START:
                        mGameData.setStartDevice(address);
                        // TODO: anything else to do here?
                        break;
                    case READ_GIVE_SUCCESSOR:
                        buf = ByteBuffer.wrap((byte[]) msg.obj);

                        byte[] successorAddressArr = new byte[Constants.ADDRESS_LENGTH];
                        buf.get(successorAddressArr);
                        GameData.successor = new String(successorAddressArr);
                        mSuccessorView.setText("Next: " + mGameData.getConnectedDeviceNames().get(mGameData.getConnectedDevices().indexOf(GameData.successor)));

                        // Now we're ready to start playing!
                        mIsPromptMode = buf.get() == (byte) 0;
                        mWaitingDialog.dismiss();

                        GameData.doesEndOnPrompt = buf.get() == (byte) 1;

                        updateMode();
                        break;
                    case READ_DTG:
                        GameData.connectionChangeListener.connection(address);
                        break;
                }
            }
        }
    };
}