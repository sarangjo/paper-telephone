package com.cse461.a16au.papertelephone.services;

import android.os.Handler;

/** TODO: class documentation */
public abstract class ConnectService {
  // States
  public static final int STATE_STOPPED = 0;
  public static final int STATE_STARTED = 1;

  int mState;
  Handler mMainHandler;
  Handler mGameHandler;

  public abstract boolean write(byte[] data, String device);

  public void registerGameHandler(Handler handler) {
    mGameHandler = handler;
  }

  public void unregisterGameHandler(Handler gameHandler) {
    mGameHandler = (mGameHandler.equals(gameHandler) ? null : mGameHandler);
  }

  public void registerMainHandler(Handler handler) {
    mMainHandler = handler;
  }

  public synchronized int getState() {
    return mState;
  }

  public synchronized void setState(int state) {
    this.mState = state;
  }

  public abstract void start();

  public abstract void stop();

  public abstract void connect(String address);
}
