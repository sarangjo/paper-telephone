package com.cse461.a16au.papertelephone;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by sgorti3 on 11/30/2016.
 * TODO: implement and document
 */
public class PromptFragment extends Fragment {
    private PromptSendListener mListener;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_prompt, container, false);
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


    interface PromptSendListener {
        void sendPrompt(byte[] prompt);
    }
}
