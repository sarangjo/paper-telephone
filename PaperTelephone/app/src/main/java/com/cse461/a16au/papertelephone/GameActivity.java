package com.cse461.a16au.papertelephone;

/**
 * Created by siddt on 11/29/2016.
 * TODO: documentation
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;
import android.widget.Toast;

public class GameActivity extends FragmentActivity {
    private BluetoothConnectService mConnectService;
    private ImageView mReceivedImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mConnectService = BluetoothConnectService.getInstance();
        mConnectService.registerGameHandler(mGameHandler);

        mReceivedImageView = (ImageView) findViewById(R.id.image_received_image);
    }

    private final Handler mGameHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    switch (msg.arg2) {
                        case Constants.READ_UNKNOWN:
                            Toast.makeText(GameActivity.this, "Received unknown format", Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.READ_IMAGE:
                            Toast.makeText(GameActivity.this, "Image incoming...", Toast.LENGTH_SHORT).show();
                            processImage((byte[]) msg.obj);
                            break;
                        case Constants.READ_PING:
                            Toast.makeText(GameActivity.this,"Received ping",Toast.LENGTH_SHORT).show();
                            break;
                        case Constants.READ_PROMPT:
                            // TODO: Add display prompt logic here
                            break;
                    }
                    break;
            }
        }
    };

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
}
