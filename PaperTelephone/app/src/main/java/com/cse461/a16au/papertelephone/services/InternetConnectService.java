package com.cse461.a16au.papertelephone.services;

import android.app.Activity;

/**
 * TODO: class documentation
 */
class InternetConnectService extends ConnectService {
  private static ConnectService ourInstance = new InternetConnectService();

  private InternetConnectService() {
  }

  static ConnectService getInstance() {
    return ourInstance;
  }

  @Override
  public boolean write(String device, byte[] data) {
    return false;
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }

  @Override
  public void connect(String address) {

  }

  @Override
  public void joinLobby(String lobbyId) {

  }

  @Override
  public void leaveLobby(String lobbyId) {

  }

  @Override
  public String getLocalAddress() {
    return null;
  }

  @Override
  public void setLocalAddress() {

  }

  @Override
  public boolean setupNetwork(Activity callbackActivity) {
    return false;
  }
}
