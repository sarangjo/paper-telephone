package com.cse461.a16au.papertelephone.services;

import android.support.annotation.NonNull;

/** TODO: class documentation */
public class ConnectServiceFactory {
  public static final int BLUETOOTH = 0;
  public static final int WI_FI = 1;
  public static final int INTERNET = 2;

  public static ConnectService getService() {
    return getService(BLUETOOTH);
  }

  public static ConnectService getService(int type) {
    switch (type) {
      case BLUETOOTH:
        return BluetoothConnectService.getInstance();
      case WI_FI:
        return WiFiConnectService.getInstance();
      case INTERNET:
        return InternetConnectService.getInstance();
      default:
        return null;
    }
  }
}
