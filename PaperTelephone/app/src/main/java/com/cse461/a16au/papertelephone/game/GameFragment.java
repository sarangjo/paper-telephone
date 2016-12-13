package com.cse461.a16au.papertelephone.game;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

/**
 * Created by sgorti3 on 12/6/2016.
 *
 *
 * this thing is like for one method because two communocation not supported for cyclic inheitience
 * recursion
 *
 */

public abstract class GameFragment extends Fragment {
    protected DataSendListener mListener;

    public abstract void endTurn();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DataSendListener) {
            mListener = (DataSendListener) activity;
        } else {
            throw new RuntimeException("Parent activity must implement DataSendListener.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * TODO: class documentation
     */
    public interface DataSendListener {
        void sendTurnData(byte[] data);
    }
}
