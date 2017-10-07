package com.cse461.a16au.papertelephone.services;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
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

  @Override
  public String getLocalAddress() {
    return null;
  }

  @Override
  public void setLocalAddress() {

  }

  @Override
  public void setupNetwork(Activity callbackActivity) {
    // Set up nearby service discovery
    // Useful guide for NSD,
    // https://developer.android.com/training/connect-devices-wirelessly/nsd.html

    final String serviceName = "PaperTelephone";
    final String serviceType = "_papertelephone._tcp";

    NsdServiceInfo serviceInfo = new NsdServiceInfo();
    serviceInfo.setServiceName(serviceName);
    serviceInfo.setServiceType(serviceType);
    serviceInfo.setPort(this.getPort());

    NsdManager.RegistrationListener mRegistrationListener =
        new NsdManager.RegistrationListener() {
          String mServiceName;

          @Override
          public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            // Save the service name.  Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = serviceInfo.getServiceName();

            //
          }

          @Override
          public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Registration failed
            // TODO: Put debugging code here to determine why.
          }

          @Override
          public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            // Service has been unregistered.  This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
          }

          @Override
          public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Unregistration failed
            // TODO: Put debugging code here to determine why.
          }
        };

    // Not sure if this line is correct, the guide is not really helpful here
    final NsdManager mNsdManager = (NsdManager) this.applicationContext.getSystemService(Context.NSD_SERVICE);

    // Register service
    mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    // Implement a resolve listener
    final NsdManager.ResolveListener mResolveListener =
        new NsdManager.ResolveListener() {

          @Override
          public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails.
            Log.e(TAG, "Resolve failed, error code: " + errorCode);
          }

          @Override
          public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(serviceName)) {
              Log.d(TAG, "Same IP.");
              return;
            }

            // TODO: Figure out what to do here
            // I believe that the serviceInfo object here has the info we need to make a network
            // connection, but it's kind of unclear what we're supposed to do with it
            // For reference
            // https://developer.android.com/training/connect-devices-wirelessly/nsd.html#connect
            int port = serviceInfo.getPort();
            InetAddress host = serviceInfo.getHost();
          }
        };

    // Create discovery listener to listen for other devices running our service
    NsdManager.DiscoveryListener mDiscoveryListener =
        new NsdManager.DiscoveryListener() {

          @Override
          public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "Service Discovery Started");
          }

          @Override
          public void onServiceFound(NsdServiceInfo serviceInfo) {
            // Service was found
            Log.d(TAG, "Service discovery success" + serviceInfo);
            if (!serviceInfo.getServiceType().equals(serviceType)) {
              // Not our service, some other device, disregard
              Log.d(TAG, "Unknown Service Type: " + serviceInfo.getServiceType());
            } else if (serviceInfo.getServiceName().equals(serviceName)) {
              // The discovered service was our own
              Log.d(TAG, "Own service found");
            } else if (serviceInfo.getServiceName().contains(serviceName)) {

              mNsdManager.resolveService(serviceInfo, mResolveListener);
            }
          }

          @Override
          public void onServiceLost(NsdServiceInfo serviceInfo) {
            // Service is no longer available
            Log.e(TAG, "Service lost: " + serviceInfo);
          }

          @Override
          public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discover stopped: " + serviceType);
          }

          @Override
          public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed, error code: " + errorCode);
            mNsdManager.stopServiceDiscovery(this);
          }

          @Override
          public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discover failed, error code: " + errorCode);
            mNsdManager.stopServiceDiscovery(this);
          }
        };

    mNsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

  }
}
