package com.cse461.a16au.papertelephone.game;

/**
 * Created by siddt on 11/29/2016.
 * TODO: documentation
 */

import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.BluetoothConnectService;
import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.cse461.a16au.papertelephone.game.GameData.connectedDevices;

public class GameActivity extends FragmentActivity implements DrawingFragment.DrawingSendListener, PromptFragment.PromptSendListener {
    private BluetoothConnectService mConnectService;
    private boolean isPromptMode;
    private GameFragment mFragment;
    private TextView mTimerTextView;
    private String prompt;
    private byte[] image;
    private boolean isDone;
    private List<String> deviceList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isPromptMode = true;
        isDone = false;
        deviceList = new ArrayList<>(connectedDevices);
        setContentView(R.layout.activity_game);
        updateMode();
        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerGameHandler(mGameHandler);

        mTimerTextView = (TextView) findViewById(R.id.timer);
    }

    private final Handler mGameHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    switch (msg.arg2) {
                        case Constants.READ_IMAGE:
                            Toast.makeText(GameActivity.this, "Image incoming...", Toast.LENGTH_SHORT).show();
                            image = (byte[]) msg.obj;
                            break;
                        case Constants.READ_PING:
                            Toast.makeText(GameActivity.this, "Received ping", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.READ_PROMPT:
                            Toast.makeText(GameActivity.this, "Text incoming...", Toast.LENGTH_SHORT).show();
                            processText((byte[]) msg.obj);
                            if (isDone && deviceList.isEmpty()) {
                               updateMode();
                            }
                            break;
                        case Constants.READ_DONE:
                            // Get address of sender and remove it from our list of devices
                            // keeping track of which devices haven't finished yet
                            Bundle b = msg.getData();
                            String address = b.getString(Constants.DEVICE_ADDRESS);
                            deviceList.remove(address);

                            // If we are done and all other devices are done we move on to the
                            // next phase of the game by calling updateMode()
                            if (isDone && deviceList.isEmpty()) {
                                updateMode();
                            }
                            break;
                    }
                    break;
            }

        }
    };
    
    // Moves on to the next phase of the game
    public void updateMode(){
        if(GameData.turnTimer != null) {
            GameData.turnTimer.cancel();
        }

        // Start the timer at 30 seconds for the next phase of the game
        GameData.turnTimer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                mTimerTextView.setText(String.format("%2ds", millisUntilFinished / 1000));
            }

            public void onFinish() {
                // End the turn, sending the drawing or prompt if it hasn't already been sent
                if(!isDone) {
                    mFragment.endTurn();
                    isDone = true;
                }
                mTimerTextView.setText("00s");
                mTimerTextView.setTextColor(Color.RED);
            }
        }.start();

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if(mFragment != null) {
            ft.remove(mFragment);
        }

        Bundle args = new Bundle();
        if (isPromptMode) {
            if(mFragment == null) {
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

        ft.add(R.id.game_fragment_container, mFragment);
        isPromptMode = !isPromptMode;

        ft.commit();
        isDone = false;
        deviceList = new ArrayList<>(connectedDevices);
    }



    
    private void gameEndTurn() {
        // Write done message to all devices
        byte[] header  = Constants.HEADER_DONE;
        ByteBuffer buf = ByteBuffer.allocate(header.length);
        buf.put(header);

        for(String device : connectedDevices) {
            mConnectService.write(buf.array(), device);
        }

        isDone = true;
        if (deviceList.isEmpty()) {
            updateMode();
        }
        GameData.turnTimer.cancel();
    }

    private void processText(byte[] data) {
        prompt = new String(data);
    }

    @Override
    public void sendDrawing(byte[] image) {
        gameEndTurn();
        // First send header to indicate we're sending an image
        byte[] header = Constants.HEADER_IMAGE;

        if (image != null) {
            updateMode();
            ByteBuffer buf = ByteBuffer.allocate(header.length + image.length + 4);
            buf.put(header);
            buf.putInt(image.length);
            buf.put(image);
            // Write it through the service
            mConnectService.write(buf.array(), ""/*TODO: replace with connectedDevices.get(nextDevice)*/);
            return;
        }

        Toast.makeText(this, "Please submit a drawing.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void sendPrompt(byte[] prompt) {
        gameEndTurn();
        byte[] header = Constants.HEADER_PROMPT;

        if(prompt != null) {
            updateMode();
            ByteBuffer buf = ByteBuffer.allocate(header.length + prompt.length);
            buf.put(header);
            buf.put(prompt);
            mConnectService.write(buf.array(), "");
            return;
        }

        Toast.makeText(this, "Please submit a prompt.", Toast.LENGTH_SHORT).show();

    }


}
