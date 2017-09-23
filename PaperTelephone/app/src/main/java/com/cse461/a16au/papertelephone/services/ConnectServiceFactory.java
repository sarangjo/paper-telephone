package com.cse461.a16au.papertelephone.services;

import android.support.annotation.NonNull;

/** TODO: class documentation */
public class ConnectServiceFactory {
  public static final int BLUETOOTH = 0;
  public static final int WI_FI = 1;
  public static final int INTERNET = 2;

  public static int TYPE = BLUETOOTH;

  // TODO: See if there is something other than null that we can return for default:, otherwise
  // we should remove the @NonNull annotation
  @NonNull
  public static ConnectService getService() {
    switch (TYPE) {
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
