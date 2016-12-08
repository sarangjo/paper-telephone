package com.cse461.a16au.papertelephone.game;

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
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DataSendListener) {
            mListener = (DataSendListener) context;
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
        void sendData(byte[] data);
    }
}
