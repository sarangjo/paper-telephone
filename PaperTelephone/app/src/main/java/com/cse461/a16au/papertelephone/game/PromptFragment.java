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

import static com.cse461.a16au.papertelephone.game.GameData.localAddress;

/**
 * Created by sgorti3 on 11/30/2016.
 * Asks the user what they think an image is a drawing of and
 * forwards there response to GameActivity at the end of a turn.
 */
public class PromptFragment extends GameFragment {
    private ImageView mReceivedImageView;
    private EditText mPromptText;
    private String mCreatorAddress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prompt, container, false);
        Bundle args = getArguments();

        Button sendPromptButton = (Button) view.findViewById(R.id.button_send_prompt);

        if (args.getBoolean("start")) {
            // Different help text for start-of-game
            TextView prompt = (TextView) view.findViewById(R.id.prompt_help);
            prompt.setText("Enter something for someone to draw");

            mPromptText = (EditText) view.findViewById(R.id.user_prompt);
            mPromptText.setHint("Enter prompt here");

            sendPromptButton.setText("Send prompt");
            mCreatorAddress = localAddress;
        } else {
            // Grab image view and display the received image
            mReceivedImageView = (ImageView) view.findViewById(R.id.image_received_image);

            byte[] data = args.getByteArray("image");
            Bitmap bitmap = null;
            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
            mPromptText = (EditText) view.findViewById(R.id.user_prompt);
            mReceivedImageView.setImageBitmap(bitmap);
            mCreatorAddress = args.getString(Constants.CREATOR_ADDRESS);
        }

        sendPromptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endTurn();
            }
        });

        return view;
    }

    /**
     * Process the end of a turn, packaging the prompt and passing it through mListener to GameActivity
     */
    @Override
    public void endTurn() {
        String input = mPromptText.getText().toString().trim();

        if (!input.isEmpty()) {
            byte[] prompt = input.getBytes();

            GameData.saveData(mCreatorAddress, prompt);

            ByteBuffer buf = ByteBuffer.allocate(Constants.HEADER_LENGTH + Constants.ADDRESS_LENGTH + prompt.length);
            buf.put(Constants.HEADER_PROMPT);
            buf.put(mCreatorAddress.getBytes());
            buf.put(prompt);

            mListener.sendTurnData(buf.array());
        } else {
            Toast.makeText(getActivity(), "Please submit a prompt.", Toast.LENGTH_SHORT).show();
        }
    }
}
