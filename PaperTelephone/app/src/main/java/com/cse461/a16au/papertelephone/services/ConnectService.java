package com.cse461.a16au.papertelephone.services;

import android.os.Handler;

/** TODO: class documentation */
public abstract class ConnectService {
  // States
  public static final int STATE_STOPPED = 0;
  public static final int STATE_STARTED = 1;

  int state;
  Handler mainHandler;
  Handler gameHandler;
  Handler packetHandler;

  public abstract boolean write(String device, byte[] data);

  /**
   * TODO: implement to simplify packet-sending
   *
   * @param device
   * @param messageType
   * @return
   */
  private boolean write(String device, int messageType) {
    byte[] data = new byte[1];
    return write(device, data);
  }

  public void registerGameHandler(Handler handler) {
    gameHandler = handler;
  }

  public void unregisterGameHandler(Handler gameHandler) {
    this.gameHandler = (this.gameHandler.equals(gameHandler) ? null : this.gameHandler);
  }

  public void registerMainHandler(Handler handler) {
    mainHandler = handler;
  }

  public synchronized int getState() {
    return state;
  }

  public synchronized void setState(int state) {
    this.state = state;
  }

  public abstract void start();

  public abstract void stop();

  public abstract void connect(String address);

  public void registerPacketHandler(Handler handler) {
    this.packetHandler = handler;
  }
}
