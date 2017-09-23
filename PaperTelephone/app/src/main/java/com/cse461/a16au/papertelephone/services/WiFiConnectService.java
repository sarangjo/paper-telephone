package com.cse461.a16au.papertelephone.services;

import java.io.IOException;
import java.net.ServerSocket;

/** TODO: class documentation */
public class WiFiConnectService extends ConnectService {
  private static final String TAG = "WiFiConnectService";
  private static ConnectService ourInstance = new WiFiConnectService();
  // These should be final, but Android Studio was complaining that they might not
  // get initialized, then I added dummy values in the catch statement in the constructor
  // and it told me that they might already have been assigned to
  private ServerSocket mServerSocket;
  private int mPort;

  private WiFiConnectService() {
    try {
      mServerSocket = new ServerSocket(0);
      mPort = mServerSocket.getLocalPort();
    } catch (IOException e) {
      e.printStackTrace();
      // TODO: Add failure method that gives feedback for use in debugging
    }
  }

  static ConnectService getInstance() {
    return ourInstance;
  }

  public int getPort() {
    return mPort;
  }

  @Override
  public boolean write(String device, byte[] data) {
    return false;
  }

  @Override
  public int getState() {
    return 0;
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}

  @Override
  public void connect(String address) {}
}
