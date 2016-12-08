package com.cse461.a16au.papertelephone.game;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;

import java.nio.ByteBuffer;

/**
 * Created by sgorti3 on 11/30/2016.
 * TODO: implement and document
 */
public class PromptFragment extends GameFragment {
    private ImageView mReceivedImageView;
    private EditText mPromptText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prompt, container, false);
        Bundle args = getArguments();
        if (args.getBoolean("start")) {
            TextView prompt = (TextView) view.findViewById(R.id.prompt_help);
            prompt.setText("Enter something for someone to draw");

            mPromptText = (EditText) view.findViewById(R.id.user_prompt);
            mPromptText.setHint("Enter prompt here");

            Button button = (Button) view.findViewById(R.id.button_send_drawing);
            button.setText("Send prompt");
        } else {
            // Grab image view and display the received image
            mReceivedImageView = (ImageView) view.findViewById(R.id.image_received_image);

            byte[] data = args.getByteArray("image");
            Bitmap bitmap = null;
            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0,
                        data.length);
            }

            mReceivedImageView.setImageBitmap(bitmap);

        }

        Button sendPromptButton = (Button) view.findViewById(R.id.button_send_prompt);
        sendPromptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endTurn();
            }
        });

        return view;
    }

    @Override
    public void endTurn() {
        String input = mPromptText.getText().toString().trim();

        if (!input.isEmpty()) {
            byte[] prompt = input.getBytes();

            byte[] header = Constants.HEADER_PROMPT;
            ByteBuffer buf = ByteBuffer.allocate(header.length + prompt.length);
            buf.put(header);
            buf.put(prompt);

            mListener.sendData(buf.array());
        } else {
            Toast.makeText(getActivity(), "Please submit a prompt.", Toast.LENGTH_SHORT).show();
        }
    }
}
