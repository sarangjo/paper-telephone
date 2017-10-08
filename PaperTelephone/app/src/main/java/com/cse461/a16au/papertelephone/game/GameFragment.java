package com.cse461.a16au.papertelephone.game;

import android.app.Activity;
import android.app.Fragment;

/**
 * This thing is like for one method because two communication not supported for cyclic inheritance
 * recursion
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
   * Sends the corresponding data to the next device, whether it be the prompt text, or drawing data
   */
  interface DataSendListener {
    void sendTurnData(byte[] data);
  }
}
