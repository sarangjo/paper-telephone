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

public class GameActivity extends FragmentActivity implements DrawingFragment.DrawingSendListener, PromptFragment.PromptSendListener {
    private BluetoothConnectService mConnectService;
    private ImageView mReceivedImageView;
    private boolean isDrawMode;
    private GameFragment mFragment;
    private TextView mTimerTextView;
    private boolean isDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isDrawMode = true;
        isDone = false;
        setContentView(R.layout.activity_game);

        updateMode();
        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerGameHandler(mGameHandler);

        mReceivedImageView = (ImageView) findViewById(R.id.image_received_image);
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
                            processImage((byte[]) msg.obj);
                            break;
                        case Constants.READ_PING:
                            Toast.makeText(GameActivity.this, "Received ping", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.READ_PROMPT:
                            Toast.makeText(GameActivity.this, "Text incoming...", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
            }

        }
    };


    public void updateMode(){
        if(GameData.turnTimer != null) {
            GameData.turnTimer.cancel();
        }

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

        if (isDrawMode) {
            if(mFragment == null) {
                Bundle args = new Bundle();
                args.putBoolean("start", true);
                mFragment = new PromptFragment();
                mFragment.setArguments(args);
            } else {
                Bundle args = new Bundle();
                args.putBoolean("start", false);
                mFragment = new PromptFragment();
                mFragment.setArguments(args);
            }
        } else {
            mFragment = new DrawingFragment();
        }

        ft.add(R.id.game_fragment_container, mFragment);
        isDrawMode = !isDrawMode;

        ft.commit();
    }



    /**
     * Process an array of bytes into a bitmap and display it in the view
     *
     * @param data array of bytes containing image information
     */
    private void processImage(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
                data.length);
        mReceivedImageView.setImageBitmap(bitmap);
    }
    
    private void endTurn() {
        isDone = true;
        GameData.turnTimer.cancel();
    }

    private void processText(byte[] data) {
        System.out.println("fuck this method");
    }

    @Override
    public void sendDrawing(byte[] image) {
        endTurn();
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
        endTurn();
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
