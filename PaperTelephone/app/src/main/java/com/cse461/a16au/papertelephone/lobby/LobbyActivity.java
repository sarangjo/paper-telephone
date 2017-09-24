package com.cse461.a16au.papertelephone.lobby;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.GameController;
import com.cse461.a16au.papertelephone.R;
import com.cse461.a16au.papertelephone.game.EndGameActivity;
import com.cse461.a16au.papertelephone.game.GameActivity;
import com.cse461.a16au.papertelephone.services.ConnectServiceFactory;
import com.cse461.a16au.papertelephone.services.WiFiConnectService;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;

import static com.cse461.a16au.papertelephone.Constants.ADDRESS_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.HEADER_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.HEADER_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECT_FAILED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_DISCONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MIN_PLAYERS;
import static com.cse461.a16au.papertelephone.Constants.NO_START;
import static com.cse461.a16au.papertelephone.Constants.READ_DEVICES;
import static com.cse461.a16au.papertelephone.Constants.READ_PING;
import static com.cse461.a16au.papertelephone.Constants.READ_RTL;
import static com.cse461.a16au.papertelephone.Constants.READ_START;
import static com.cse461.a16au.papertelephone.Constants.READ_START_ACK;
import static com.cse461.a16au.papertelephone.Constants.READ_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.READ_UNKNOWN;
import static com.cse461.a16au.papertelephone.Constants.WE_ARE_START;

/**
 * TODO: class documentation
 */
public class LobbyActivity extends AppCompatActivity
    implements DevicesFragment.ConnectDeviceListener, GameController.ConnectedDevicesListener {
  private static final String TAG = "LobbyActivity";

  /**
   * Adapter for connected devices view
   */
  private ArrayAdapter<String> mConnectedDevicesNamesAdapter;

  private Button mStartGameButton;
  private GameController mController;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_lobby);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    int type = ConnectServiceFactory.BLUETOOTH;

    switch (type) {
      case ConnectServiceFactory.BLUETOOTH:
        // Request that bluetooth be enabled if it is disabled
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
          Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
        }
        break;
      case ConnectServiceFactory.INTERNET:
        break;
      case ConnectServiceFactory.WI_FI:
        // Set up nearby service discovery
        // Useful guide for NSD,
        // https://developer.android.com/training/connect-devices-wirelessly/nsd.html

        final String serviceName = "PaperTelephone";
        final String serviceType = "_papertelephone._tcp";

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setServiceType(serviceType);
        serviceInfo.setPort(((WiFiConnectService) mController.getConnectService()).getPort());

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
        final NsdManager mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);

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
        break;
      default:
        // Unknown Connection Type, exit
        // TODO: Add failure method that gives feedback for use in debugging
    }

    // TODO: do something with our own local MAC address
    String localAddress = android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address");

    mController = GameController.getInstance();

    // Views
    mConnectedDevicesNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mController.getConnectedDeviceNames());
    ListView connectedListView = (ListView) findViewById(R.id.connected_devices);
    connectedListView.setAdapter(mConnectedDevicesNamesAdapter);
    connectedListView.setOnItemClickListener(
        new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mController.sendPing(mConnectedDevicesNamesAdapter.getItem(position));
          }
        });

    mStartGameButton = (Button) findViewById(R.id.button_start_game);
    mStartGameButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mController.startGameClicked();
          }
        });
  }

  @Override
  protected void onResume() {
    super.onResume();

    // In the case that Bluetooth was disabled to start, onResume() will
    // be called when the ACTION_REQUEST_ENABLE activity has returned
    mController.resumeConnectService();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    mController.stopConnectService();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case Constants.REQUEST_PLAY_GAME:
        if (resultCode == RESULT_OK) {
          // Game ended successfully
          Toast.makeText(this, "Game over!", Toast.LENGTH_LONG).show();
          Intent intent = new Intent(this, EndGameActivity.class);
          startActivityForResult(intent, Constants.REQUEST_END_GAME);
        } else {
          Toast.makeText(this, "Game did not end correctly", Toast.LENGTH_LONG).show();
        }
        break;
      case Constants.REQUEST_END_GAME:
        switch (resultCode) {
          case Constants.RESULT_LOBBY:
          case Constants.RESULT_RESTART:
            mController.resetGameData();
            break;
          default:
            Toast.makeText(this, "End game did not return correctly", Toast.LENGTH_LONG).show();
            break;
        }
        break;
    }
  }

  private void updateStartGameButton() {
    if (mController.getConnectedDevices().size() >= MIN_PLAYERS - 1) {
      mStartGameButton.setEnabled(true);
    } else {
      mStartGameButton.setEnabled(false);
    }
  }

  private void transitionToGame() {
    Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
    startActivityForResult(intent, Constants.REQUEST_PLAY_GAME);
  }

  @Override
  public void onDeviceToConnectToSelected(String address) {
    // Connect to device
    mController.getConnectService().connect(address);
  }

  private void handleRead(Message msg, String deviceAddress, String deviceName) {
    ByteBuffer buf;
    switch (msg.arg2) {
      case READ_UNKNOWN:
        Toast.makeText(LobbyActivity.this, "Received unknown format", Toast.LENGTH_SHORT).show();
        break;
      case READ_PING:
        Toast.makeText(LobbyActivity.this, "Received ping from " + deviceName, Toast.LENGTH_SHORT)
            .show();
        break;
      case READ_START:
        // Set the START device
        if (mGameData.getStartDevice().equals(NO_START)) {
          mGameData.setStartDevice(deviceAddress);

          // Ack the new start device
          mConnectService.write(deviceAddress, Constants.HEADER_START_ACK);
        } else {
          String currentStartDevice;
          if (mGameData.getStartDevice().equals(WE_ARE_START)) {
            currentStartDevice = localAddress;
          } else {
            currentStartDevice = mGameData.getStartDevice();
          }

          // Check preference by address comparison
          if (currentStartDevice.compareTo(deviceAddress) > 0) {
            // Update our start device to be the one with the lower address
            mGameData.setStartDevice(deviceAddress);

            // Re-ack the new start device
            mConnectService.write(deviceAddress, Constants.HEADER_START_ACK);
          } else {
            // We are still start device, so we ignore this
            return;
          }
        }

        // Setup unplaced devices, i.e. all devices except for the start device and us
        unplacedDevices.clear();
        for (String currDevice : mGameData.getConnectedDevices()) {
          if (!currDevice.equals(deviceAddress)) {
            unplacedDevices.add(currDevice);
          }
        }
        break;
      case READ_START_ACK:
        // TODO: do we always just blindly accept START ACK's? I think we do, I just don't remmeber
        // TODO: the logic behind that I guess
        break;
      case READ_SUCCESSOR:
        buf = ByteBuffer.wrap((byte[]) msg.obj);

        // Get the successor number and successor address
        lastSuccessorNumber = buf.getInt();
        byte[] successorAddressArr = new byte[ADDRESS_LENGTH];
        buf.get(successorAddressArr);

        String successorAddress = new String(successorAddressArr);
        unplacedDevices.remove(successorAddress);

        // Check if we are the chosen successor
        boolean isUs = true;
        for (String address : mGameData.getConnectedDevices()) {
          isUs = isUs && !successorAddress.equals(address);
        }

        // If we are the start device and we are the new successor, start game
        if ((mGameData.getStartDevice().equals(WE_ARE_START) && isUs)
            // If the loop has been completed and all devices have a successor, start game
            || (!mGameData.getStartDevice().equals(WE_ARE_START)
            && successorAddress.equals(mGameData.getStartDevice()))) {
          transitionToGame();
          return;
        }

        // If removing from the set returns false that means we are the newly paired device
        if (isUs) {
          chooseSuccessor();
        }
        break;
      case READ_RTL:
        Toast.makeText(
            LobbyActivity.this, deviceName + " has returned to lobby", Toast.LENGTH_SHORT)
            .show();
        mGameData.addLobbiedDevice(deviceAddress);
        break;
      case READ_DEVICES:
        // Establish connections with devices that are already in the game
        buf = ByteBuffer.wrap((byte[]) msg.obj);

        int newTurnsLeft = buf.getInt();
        isGameActive = newTurnsLeft > 0;
        if (isGameActive) {
          turnsLeft = newTurnsLeft + 1; // offset from GameActivity's setupRound
        }

        int numDevices = buf.getInt();
        for (int i = 0; i < numDevices; i++) {
          byte[] address = new byte[ADDRESS_LENGTH];
          buf.get(address);
          String addr = new String(address);

          if (mGameData.getConnectedDevices().indexOf(addr) >= 0) {
            // this has already been connected to!
            Log.d(TAG, "Already connected to " + addr);
          } else {
            mGameData.addDeviceToConnectTo(addr);
            onDeviceToConnectToSelected(addr);
          }
        }

        // Cross this off our list of devices to connect to
        mGameData.removeDeviceToConnectTo(deviceAddress);

        // This is the case where we are joining a game in progress
        if (mGameData.isDoneConnectingToGameDevices() && isGameActive) {
          try {
            Thread.sleep(250);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
          intent.putExtra(Constants.JOIN_MID_GAME, true);
          startActivityForResult(intent, Constants.REQUEST_PLAY_GAME);
        }
        break;
    }
  }

  @Override
  public void onDeviceStatusChanged(int status, String address, String name) {
    switch (status) {
      case MESSAGE_CONNECTED:
        Toast.makeText(LobbyActivity.this, "Connected to " + name, Toast.LENGTH_SHORT).show();
        break;
      case MESSAGE_DISCONNECTED:
        Toast.makeText(LobbyActivity.this, "Disconnected from " + name, Toast.LENGTH_SHORT).show();
        break;
      case MESSAGE_CONNECT_FAILED:
        Toast.makeText(LobbyActivity.this, "Unable to connect: " + name + ". Please try again.", Toast.LENGTH_LONG)
            .show();
        break;
    }
    switch (status) {
      case MESSAGE_CONNECTED:
      case MESSAGE_DISCONNECTED:
        mConnectedDevicesNamesAdapter.notifyDataSetChanged();

        // if (GameData.connectionChangeListener != null) {
        //   GameData.connectionChangeListener.disconnection(deviceAddress);
        // }

        this.updateStartGameButton();
        break;
    }
  }
}
