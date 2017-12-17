package com.cse461.a16au.papertelephone.services;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;

/** TODO: class documentation */
public abstract class ConnectService {
  // States
  public static final int STATE_STOPPED = 0;
  public static final int STATE_STARTED = 1;

  int state;
  // TODO 12/17/17 - we should not need the gameHandler, right? Just the packet and network handlers
  // should be sufficient
  //Handler gameHandler;
  protected Handler packetHandler;
  protected Handler networkHandler;
  protected Application applicationContext;
  protected String localAddress;

  public abstract boolean write(String device, byte[] data);

  // TODO: add unregister for handlers

  public void registerPacketHandler(Handler handler) {
    this.packetHandler = handler;
  }

  public void registerNetworkHandler(Handler handler) { this.networkHandler = handler; }

  public synchronized int getState() {
    return state;
  }

  public synchronized void setState(int state) {
    this.state = state;
  }

  public abstract void start();

  public abstract void stop();

  public abstract void connect(String address);

  public abstract String getLocalAddress();

  public void setApplication(Application applicationContext) {
    this.applicationContext = applicationContext;
    this.setLocalAddress();
  }

  public abstract void setLocalAddress();

  /**
   * Initiates the process to setup the network, if it hasn't been setup yet. It is possible that
   * network setup will take an asynchronous callback on completion, in which case this method
   * returns false.
   *
   * @return true if the network has been setup successfully
   */
  public abstract boolean setupNetwork(Activity callbackActivity);
}
