package com.cse461.a16au.papertelephone.lobby;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
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
import com.cse461.a16au.papertelephone.R;
import com.cse461.a16au.papertelephone.game.EndGameActivity;
import com.cse461.a16au.papertelephone.game.GameActivity;
import com.cse461.a16au.papertelephone.game.GameData;
import com.cse461.a16au.papertelephone.services.ConnectService;
import com.cse461.a16au.papertelephone.services.ConnectServiceFactory;
import com.cse461.a16au.papertelephone.services.WiFiConnectService;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;

import static com.cse461.a16au.papertelephone.Constants.ADDRESS_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_NAME;
import static com.cse461.a16au.papertelephone.Constants.HEADER_DEVICES;
import static com.cse461.a16au.papertelephone.Constants.HEADER_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.HEADER_PING;
import static com.cse461.a16au.papertelephone.Constants.HEADER_START;
import static com.cse461.a16au.papertelephone.Constants.HEADER_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECT_FAILED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_DISCONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_READ;
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
import static com.cse461.a16au.papertelephone.game.GameData.lastSuccessorNumber;
import static com.cse461.a16au.papertelephone.game.GameData.localAddress;
import static com.cse461.a16au.papertelephone.game.GameData.successor;
import static com.cse461.a16au.papertelephone.game.GameData.turnsLeft;
import static com.cse461.a16au.papertelephone.game.GameData.unplacedDevices;

/** TODO: class documentation */
public class LobbyActivity extends AppCompatActivity
    implements DevicesFragment.ConnectDeviceListener {
  private static final String TAG = "LobbyActivity";

  /** Adapter for connected devices view */
  private ArrayAdapter<String> mConnectedDevicesNamesAdapter;

  /** Our bluetooth service to handle all bluetooth connections */
  private ConnectService mConnectService = null;

  private GameData mGameData = null;

  /** Main view. */
  private View mView;

  private Button mStartGameButton;

  private boolean isGameActive = false;
  public final Handler mMainHandler =
      new Handler() {
        @Override
        public void handleMessage(Message msg) {
          String deviceName = msg.getData().getString(DEVICE_NAME);
          String deviceAddress = msg.getData().getString(DEVICE_ADDRESS);
          switch (msg.what) {
            case MESSAGE_CONNECTED:
              // Send a message describing which devices we are already connected to
              sendConnectedDevices(deviceAddress);

              mGameData.addConnectedDevice(deviceAddress, deviceName);
              mGameData.addLobbiedDevice(deviceAddress);
              mConnectedDevicesNamesAdapter.notifyDataSetChanged();
              Toast.makeText(LobbyActivity.this, "Connected to " + deviceName, Toast.LENGTH_SHORT)
                  .show();

              // Update the button's enabled-ness
              if (mGameData.getConnectedDevices().size() >= MIN_PLAYERS - 1 && !isGameActive) {
                mStartGameButton.setEnabled(true);
              } else {
                mStartGameButton.setEnabled(false);
              }
              break;
            case MESSAGE_DISCONNECTED:
              mGameData.removeConnectedDevice(deviceAddress, deviceName);
              mGameData.removeLobbiedDevice(deviceAddress);
              mConnectedDevicesNamesAdapter.notifyDataSetChanged();
              //                    Snackbar.make(mView, "Disconnected from " + deviceName,
              // Snackbar.LENGTH_LONG).show();
              Toast.makeText(
                      LobbyActivity.this, "Disconnected from " + deviceName, Toast.LENGTH_SHORT)
                  .show();

              if (GameData.connectionChangeListener != null) {
                GameData.connectionChangeListener.disconnection(deviceAddress);
              }

              if (mGameData.getConnectedDevices().size() >= MIN_PLAYERS - 1) {
                mStartGameButton.setEnabled(true);
              } else {
                mStartGameButton.setEnabled(false);
              }
              break;
            case MESSAGE_CONNECT_FAILED:
              Snackbar.make(
                      mView,
                      "Unable to connect: " + deviceName + ". Please try again.",
                      Snackbar.LENGTH_LONG)
                  .show();
              break;
            case MESSAGE_READ:
              handleRead(msg, deviceAddress, deviceName);
              break;
          }
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_lobby);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // Setting up bluetooth
    mConnectService = ConnectServiceFactory.getService();

    switch (ConnectServiceFactory.TYPE) {
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
        serviceInfo.setPort(((WiFiConnectService) mConnectService).getPort());

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

    mConnectService.registerMainHandler(mMainHandler);

    // Get out own local MAC address
    localAddress =
        android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address");

    mGameData = GameData.getInstance();

    // Views
    mConnectedDevicesNamesAdapter =
        new ArrayAdapter<>(
            this, android.R.layout.simple_list_item_1, mGameData.getConnectedDeviceNames());
    ListView connectedListView = (ListView) findViewById(R.id.connected_devices);
    connectedListView.setAdapter(mConnectedDevicesNamesAdapter);
    connectedListView.setOnItemClickListener(
        new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!mGameData.getConnectedDevices().isEmpty()) {
              mConnectService.write(HEADER_PING, mGameData.getConnectedDevices().get(position));
            }
          }
        });

    mView = this.findViewById(android.R.id.content);

    mStartGameButton = (Button) findViewById(R.id.button_start_game);
    mStartGameButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            startGameClicked();
          }
        });

    if (mGameData.getConnectedDevices().size() >= MIN_PLAYERS - 1) {
      mStartGameButton.setEnabled(true);
    } else {
      mStartGameButton.setEnabled(false);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // In the case that Bluetooth was disabled to start, onResume() will
    // be called when the ACTION_REQUEST_ENABLE activity has returned
    if (mConnectService != null) {
      if (mConnectService.getState() == ConnectService.STATE_STOPPED) {
        // Start our BluetoothConnectionService
        mConnectService.start();
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // TODO: Stop connect service to stop connections and shut down?
    if (mConnectService != null) {
      mConnectService.stop();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case Constants.REQUEST_PLAY_GAME:
        if (resultCode == RESULT_OK) {
          // Game ended successfully
          GameData.connectionChangeListener = null;
          isGameActive = false;

          Toast.makeText(this, "Game over!", Toast.LENGTH_LONG).show();
          Intent intent = new Intent(this, EndGameActivity.class);
          startActivityForResult(intent, Constants.REQUEST_END_GAME);
        } else {
          Toast.makeText(this, "Game did not end correctly", Toast.LENGTH_LONG).show();
        }
        break;
      case Constants.REQUEST_END_GAME:
        if (resultCode == Constants.RESULT_LOBBY) {
          // Back to lobby, reset stuff
          mGameData.setStartDevice(NO_START);

          // TODO anything else to restart?
        } else if (resultCode == Constants.RESULT_RESTART) {
          mGameData.setStartDevice(NO_START);
          // TODO; do something here
        } else {
          Toast.makeText(this, "End game did not return correctly", Toast.LENGTH_LONG).show();
        }
        break;
    }
  }

  /**
   * After establishing a connection with another device, send the already-connected devices with
   * it.
   */
  private void sendConnectedDevices(String deviceAddress) {
    ByteBuffer buf =
        ByteBuffer.allocate(
            HEADER_LENGTH + 4 + 4 + ADDRESS_LENGTH * mGameData.getConnectedDevices().size());
    buf.put(HEADER_DEVICES);
    // Tell the new device if we are in a game or not
    if (GameData.connectionChangeListener == null) {
      buf.putInt(0);
    } else {
      buf.putInt(turnsLeft);
    }

    buf.putInt(mGameData.getConnectedDevices().size());
    for (String address : mGameData.getConnectedDevices()) {
      buf.put(address.getBytes());
    }

    mConnectService.write(buf.array(), deviceAddress);
  }

  /** Establishes an ordering for the connected devices when the user hits the start button */
  private void startGameClicked() {
    boolean allLobbied = true;

    //        for(String device: mGameData.getConnectedDevices()) {
    //            allLobbied = allLobbied && mGameData.getLobbiedDevices().contains(device);
    //        }

    if (mGameData.getConnectedDevices().size() >= Constants.MIN_PLAYERS - 1 && allLobbied) {
      mGameData.clearLobbiedDevices();

      // TODO: this doesn't seem like the right way to check if someone else hit start...
      if (mGameData.getStartDevice().length() == Constants.ADDRESS_LENGTH) {
        Toast.makeText(this, "Someone else hit start", Toast.LENGTH_LONG).show();
        return;
      }

      mGameData.setStartDevice(Constants.WE_ARE_START);

      for (String currDevice : mGameData.getConnectedDevices()) {
        mConnectService.write(HEADER_START, currDevice);
      }

      mGameData.setupUnackedDevices();
    } else {
      if (!allLobbied) {
        Toast.makeText(
                this, "All connected devices have not returned to the lobby.", Toast.LENGTH_LONG)
            .show();
      } else {
        Toast.makeText(
                this,
                "You don't have enough players, the game requires " + "at least 3 players",
                Toast.LENGTH_LONG)
            .show();
      }
    }
  }

  /** Chooses which device we will be sending to. Also informs all other devices */
  private void chooseSuccessor() {
    Iterator<String> iter = unplacedDevices.iterator();

    // If there are no more devices left, our successor is the start device
    boolean isLast = !iter.hasNext();
    String nextDeviceAddress;
    if (!isLast) {
      // Pick the next unplaced device as our successor
      nextDeviceAddress = iter.next();
      iter.remove();
    } else {
      nextDeviceAddress = mGameData.getStartDevice();
    }

    // Set up the successor packet
    ByteBuffer msg = ByteBuffer.allocate(HEADER_LENGTH + 4 + ADDRESS_LENGTH);
    msg.put(HEADER_SUCCESSOR);
    msg.putInt(lastSuccessorNumber + 1);
    msg.put(nextDeviceAddress.getBytes());

    // Set nextDevice field to store which device we will send prompts and drawings to
    successor = nextDeviceAddress;

    for (String address : mGameData.getConnectedDevices()) {
      mConnectService.write(msg.array(), address);
    }

    if (isLast) {
      transitionToGame();
    }
  }

  private void transitionToGame() {
    Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
    startActivityForResult(intent, Constants.REQUEST_PLAY_GAME);
  }

  @Override
  public void connectDevice(String address) {
    // Connect to device
    mConnectService.connect(address);
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
          mConnectService.write(Constants.HEADER_START_ACK, deviceAddress);
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
            mConnectService.write(Constants.HEADER_START_ACK, deviceAddress);
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
        mGameData.removeUnackedDevice(deviceAddress);
        if (mGameData.isDoneAcking()) {
          // Setup unplaced devices, i.e. all devices except for us
          unplacedDevices.clear();
          for (String device : mGameData.getConnectedDevices()) {
            unplacedDevices.add(device);
          }

          chooseSuccessor();
        }
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
            connectDevice(addr);
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
}
