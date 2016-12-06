package com.cse461.a16au.papertelephone;

/**
 * Created by siddt on 11/29/2016.
 * TODO: documentation
 */

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class GameActivity extends FragmentActivity implements DrawingFragment.DrawingSendListener {
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

    @Override
    public void sendDrawing(byte[] image) {
        // First send header to indicate we're sending an image
        byte[] header = Constants.HEADER_IMAGE;

        if (image != null) {
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
}
