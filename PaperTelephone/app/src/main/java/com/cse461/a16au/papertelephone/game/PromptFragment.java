package com.cse461.a16au.papertelephone.game;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.R;

import java.io.ByteArrayOutputStream;

/**
 * Created by sgorti3 on 11/30/2016.
 * TODO: implement and document
 */
public class PromptFragment extends GameFragment {
    private PromptSendListener mListener;
    private View view;





    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_prompt, container, false);
        Bundle args = getArguments();
        if(args.getBoolean("start")) {
            TextView prompt = (TextView) view.findViewById(R.id.prompt);
            prompt.setText("Enter something for someone to draw");

            EditText editBox = (EditText) view.findViewById(R.id.userGuess);
            editBox.setHint("Enter prompt here");


            Button button = (Button) view.findViewById(R.id.button_send_drawing);
            button.setText("Send prompt");
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
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DrawingFragment.DrawingSendListener) {
            mListener = (PromptFragment.PromptSendListener) context;
        } else {
            throw new RuntimeException("Must implement DrawingSendListener");
        }
    }

    @Override
    public void endTurn() {
        String prompt = ((EditText) view.findViewById(R.id.user_prompt)).getText().toString();
        // Build intent with the drawing data
        byte[] array = prompt.getBytes();
        mListener.sendPrompt(array);
    }


    interface PromptSendListener {
        void sendPrompt(byte[] prompt);
    }
}
