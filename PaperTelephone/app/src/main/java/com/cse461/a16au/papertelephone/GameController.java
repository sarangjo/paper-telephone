package com.cse461.a16au.papertelephone;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.lobby.LobbyActivity;
import com.cse461.a16au.papertelephone.services.ConnectService;
import com.cse461.a16au.papertelephone.services.ConnectServiceFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.cse461.a16au.papertelephone.Constants.ADDRESS_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_NAME;
import static com.cse461.a16au.papertelephone.Constants.HEADER_DEVICES;
import static com.cse461.a16au.papertelephone.Constants.HEADER_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.HEADER_PING;
import static com.cse461.a16au.papertelephone.Constants.HEADER_START;
import static com.cse461.a16au.papertelephone.Constants.HEADER_START_ACK;
import static com.cse461.a16au.papertelephone.Constants.HEADER_SUCCESSOR;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECT_FAILED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_DISCONNECTED;
import static com.cse461.a16au.papertelephone.Constants.READ_DEVICES;
import static com.cse461.a16au.papertelephone.Constants.READ_PING;
import static com.cse461.a16au.papertelephone.Constants.READ_START;
import static com.cse461.a16au.papertelephone.Constants.READ_START_ACK;
import static com.cse461.a16au.papertelephone.Constants.READ_SUCCESSOR;

/**
 * Central state manager and game logic controller.
 * TODO: rename to MainController or something?
 * TODO: this needs a lot of synchronization monitoring
 */

public class GameController {
  private static final String TAG = "GameController";

  private static GameController ourInstance = new GameController();

  // TODO: 10/7/2017 organize these fields
  private int state;
  /**
   * String representing the address of the start device.
   */
  private String startDevice;
  private int lastSuccessorNumber;
  private ConnectService connectService;
  private Set<String> unplacedDevices;
  private List<String> connectedDevices;
  private List<String> connectedDeviceNames;
  /**
   * All the lobbied devices, i.e.
   */
  private Set<String> lobbiedDevices;
  private Set<String> unackedDevices;
  private Set<StateChangeListener> stateChangeListeners;
  private ConnectedDevicesListener connectedDevicesListener;
  private String successor;
  private int turnsLeft;
  private String localAddress;
  private Context toaster;
  private int networkType;

  public static GameController getInstance() {
    return ourInstance;
  }

  private GameController() {
    this.stateChangeListeners = new HashSet<>();
    this.connectedDevices = new ArrayList<>();
    this.connectedDeviceNames = new ArrayList<>();
    this.unplacedDevices = new HashSet<>();
    this.lobbiedDevices = new HashSet<>();
    this.unackedDevices = new HashSet<>();

    this.setState(STATE_PRE_LOBBY);
  }

  public static final int STATE_PRE_LOBBY = 0;
  public static final int STATE_LOBBY = 1;
  public static final int STATE_SET_START = 2;
  public static final int STATE_PLACEMENT = 3;
  public static final int STATE_IN_GAME_PROMPT = 4;
  public static final int STATE_IN_GAME_DRAW = 5;
  public static final int STATE_POST_GAME = 6;

  public synchronized int getState() {
    return state;
  }

  public synchronized void setState(int state) {
    this.state = state;
    for (StateChangeListener l : this.stateChangeListeners) {
      l.onStateChange(state, 0);
    }
  }

  //////////////////// MESSAGE HANDLERS ////////////////////

  private Handler packetHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      GameController.this.handlePacket(msg);
    }
  };

  /**
   * Handles logic for receiving a packet.
   *
   * @param msg the message sent from the ConnectService
   */
  private void handlePacket(Message msg) {
    String deviceAddress = msg.getData().getString(DEVICE_ADDRESS);
    if (deviceAddress == null) {
      Toast.makeText(this.getToaster(), "Invalid device address in packet", Toast.LENGTH_LONG).show();
      return;
    }

    // TODO: switch this from msg.arg2 to msg.what since the msg.what's have been isolated
    if (msg.arg2 == READ_PING) {
      Toast.makeText(this.getToaster(), "Received PING from " + deviceAddress, Toast.LENGTH_LONG).show();
    } else {
      switch (this.getState()) {
        case STATE_LOBBY:
          switch (msg.arg2) {
            case READ_START:
              // Acknowledge this device as the start device
              this.setStartDevice(deviceAddress);
              this.getConnectService().write(deviceAddress, HEADER_START_ACK);
              this.transitionToPlacement();
              return;
            case READ_DEVICES:
              ByteBuffer buf = ByteBuffer.wrap((byte[]) msg.obj);

              // Connect to all devices that we are not already connected to
              int numDevices = buf.getInt();
              for (int i = 0; i < numDevices; i++) {
                byte[] addressBuffer = new byte[ADDRESS_LENGTH];
                buf.get(addressBuffer);
                String address = new String(addressBuffer);

                if (this.getConnectedDevices().indexOf(address) >= 0) {
                  Log.d(TAG, "Already connected to " + address);
                } else {
                  this.getConnectService().connect(address);
                }
              }
              return;
          }
          break;
        case STATE_SET_START:
          switch (msg.arg2) {
            case READ_START:
              // Check preference by address comparison
              if (this.getStartDevice().compareTo(deviceAddress) > 0) {
                this.setStartDevice(deviceAddress);
                this.getConnectService().write(deviceAddress, HEADER_START_ACK);
                this.transitionToPlacement();
              } else {
                Log.d(TAG, "Received START from inferior device.");
              }
              return;
            case READ_START_ACK:
              this.removeUnackedDevice(deviceAddress);
              if (this.unackedDevices.isEmpty()) {
                this.transitionToPlacement();
                this.chooseSuccessor();
              }
              return;
          }
          break;
        case STATE_PLACEMENT:
          switch (msg.arg2) {
            case READ_START:
              if (this.getStartDevice().compareTo(deviceAddress) > 0) {
                this.setStartDevice(deviceAddress);
                this.getConnectService().write(deviceAddress, HEADER_START_ACK);
                this.transitionToPlacement();
              }
              break;
            case READ_SUCCESSOR:
              ByteBuffer buf = ByteBuffer.wrap((byte[]) msg.obj);

              // Get the successor number and successor address
              lastSuccessorNumber = buf.getInt();
              byte[] successorAddressArr = new byte[ADDRESS_LENGTH];
              buf.get(successorAddressArr);

              // Remove this from our list of unplaced devices
              String successorAddress = new String(successorAddressArr);
              this.unplacedDevices.remove(successorAddress);

              if (this.unplacedDevices.isEmpty()) {
                transitionToGame();
              } else {
                if (this.getLocalAddress().equals(successorAddress)) {
                  chooseSuccessor();
                }
              }

//              // If we are the start device and we are the new successor, start game
//              if ((mGameData.getStartDevice().equals(WE_ARE_START) && isUs)
//                  // If the loop has been completed and all devices have a successor, start game
//                  || (!mGameData.getStartDevice().equals(WE_ARE_START)
//                  && successorAddress.equals(mGameData.getStartDevice()))) {
//                transitionToGame();
//                return;
//              }

//              // If removing from the set returns false that means we are the newly paired device
//              if (isUs) {
//                chooseSuccessor();
//              }
              return;
          }
          break;
        case STATE_IN_GAME_PROMPT:
          break;
        case STATE_POST_GAME:
          break;
      }
      Log.e(TAG, "Illegal packet received.");
    }
  }

  private Handler networkHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      GameController.this.handleNetworkUpdate(msg);
    }
  };

  /**
   * Handles logic for any network updates such as device connection, disconnection,
   * failed connection
   *
   * @param msg the message sent from the ConnectService
   */
  private void handleNetworkUpdate(Message msg) {
    String deviceName = msg.getData().getString(DEVICE_NAME);
    String deviceAddress = msg.getData().getString(DEVICE_ADDRESS);

    switch (this.getState()) {
      case STATE_PRE_LOBBY:
        switch (msg.what) {
          default:
            Log.e(TAG, "Illegal network update received.");
            break;
        }
        break;
      case STATE_LOBBY:
        switch (msg.what) {
          case MESSAGE_CONNECTED:
            sendConnectedDevices(deviceAddress);

            this.addConnectedDevice(deviceAddress, deviceName);
            this.addLobbiedDevice(deviceAddress);
            break;
          case MESSAGE_DISCONNECTED:
            this.removeConnectedDevice(deviceAddress, deviceName);
            this.removeLobbiedDevice(deviceAddress);
            break;
        }
        // Send along updates to network updates
        switch (msg.what) {
          case MESSAGE_CONNECTED:
          case MESSAGE_DISCONNECTED:
          case MESSAGE_CONNECT_FAILED:
            this.connectedDevicesListener.onDeviceStatusChanged(msg.what, deviceAddress, deviceName);
          default:
            Log.e(TAG, "Illegal network update received.");
            break;
        }
        break;
      case STATE_SET_START:
        break;
      case STATE_PLACEMENT:
        break;
      case STATE_IN_GAME_PROMPT:
        break;
      case STATE_POST_GAME:
        break;
    }
  }

  //////////////////// DATA MUTATION/RETRIEVAL ////////////////////

  private void addLobbiedDevice(String deviceAddress) {
    this.lobbiedDevices.add(deviceAddress);
  }

  private void removeLobbiedDevice(String deviceAddress) {
    this.lobbiedDevices.remove(deviceAddress);
  }

  private void addConnectedDevice(String address, String name) {
    this.connectedDevices.add(address);
    this.connectedDeviceNames.add(name);
  }

  private void removeConnectedDevice(String deviceAddress, String deviceName) {
    this.connectedDevices.remove(deviceAddress);
    this.connectedDeviceNames.remove(deviceName);
  }

  private void removeUnackedDevice(String deviceAddress) {
    this.unackedDevices.remove(deviceAddress);
  }

  private synchronized String getStartDevice() {
    return this.startDevice;
  }

  private synchronized void setStartDevice(String startDevice) {
    this.startDevice = startDevice;
  }

  public synchronized ConnectService getConnectService() {
    return connectService;
  }

  protected synchronized void setConnectService(int connectServiceType, Application applicationContext) {
    this.networkType = connectServiceType;
    this.connectService = ConnectServiceFactory.getService(connectServiceType);
    assert this.connectService != null;

    // Setup handlers for various messages from connect service
    this.connectService.setApplication(applicationContext);
    this.connectService.registerPacketHandler(packetHandler);
    this.connectService.registerNetworkHandler(networkHandler);
    this.setLocalAddress(this.connectService.getLocalAddress());
  }

  public List<String> getConnectedDevices() {
    return connectedDevices;
  }

  public List<String> getConnectedDeviceNames() {
    return connectedDeviceNames;
  }

  private Set<String> getLobbiedDevices() {
    return this.lobbiedDevices;
  }

  private void clearLobbiedDevices() {
    this.lobbiedDevices.clear();
  }

  private String getLocalAddress() {
    return this.localAddress;
  }

  private void setLocalAddress(String address) {
    this.localAddress = address;
  }

  public Context getToaster() {
    return toaster;
  }

  public void setToaster(Context toaster) {
    this.toaster = toaster;
  }

  //////////////////// STATE CHANGE ////////////////////

  /**
   * TODO: make only listen for specific state changes eventually so as to not wastefully spawn
   * crazy numbers of threads
   *
   * @param listener
   */
  public void registerStateChangeListener(StateChangeListener listener) {
    this.stateChangeListeners.add(listener);
  }

  public void unregisterStateChangeListener(StateChangeListener listener) {
    this.stateChangeListeners.remove(listener);
  }

  public void registerConnectedDevicesListener(ConnectedDevicesListener listener) {
    this.connectedDevicesListener = listener;
  }

  public void unregisterConnectedDevicesListener(ConnectedDevicesListener listener) {
    if (this.connectedDevicesListener.equals(listener)) {
      this.connectedDevicesListener = null;
    }
  }

  /**
   * A listener that will be called when the game state changes.
   */
  public interface StateChangeListener {
    void onStateChange(int newState, int oldState);
  }

  //////////////////// OPERATIONS ////////////////////

  /**
   * Adds all connected devices to the set of unplaced devices. Note that this does NOT include
   * self.
   */
  private void transitionToPlacement() {
    this.unplacedDevices.clear();
    for (String device : this.getConnectedDevices()) {
      if (!device.equals(this.getLocalAddress())) {
        this.unplacedDevices.add(device);
      }
    }

    this.setState(STATE_PLACEMENT);
  }

  /**
   * Switches to IN_GAME_PROMPT state
   */
  private void transitionToGame() {
    // TODO: implement
    this.setState(STATE_IN_GAME_PROMPT);
  }

  /**
   * After establishing a connection with another device, send the already-connected devices with
   * it.
   * <p>
   * TODO: would be nice to have a packet creator that's isolated from the controller
   */
  private void sendConnectedDevices(String deviceAddress) {
    ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + 4 + 4 + ADDRESS_LENGTH * this.getConnectedDevices().size());
    buf.put(HEADER_DEVICES);

    // Tell the new device if we are in a game or not
    //if (GameData.connectionChangeListener == null) {
    buf.putInt(0);
    //} else {
    //  buf.putInt(turnsLeft);
    //}

    buf.putInt(this.getConnectedDevices().size());
    for (String address : this.getConnectedDevices()) {
      buf.put(address.getBytes());
    }

    this.getConnectService().write(deviceAddress, buf.array());
  }

  public void sendPing(String address) {
    if (!this.getConnectedDevices().isEmpty()) {
      this.getConnectService().write(address, HEADER_PING);
    }
  }

  public void resumeConnectService() {
    if (this.getConnectService() != null) {
      if (this.getConnectService().getState() == ConnectService.STATE_STOPPED) {
        // Start our BluetoothConnectionService
        this.getConnectService().start();
      }
    }
  }

  public void stopConnectService() {
    // TODO: Stop connect service to stop connections and shut down?
    if (this.getConnectService() != null) {
      this.getConnectService().stop();
    }
  }

  public void resetGameData() {
    // TODO what to do here
    this.setStartDevice(null);
  }

  /**
   *
   */
  public void startGameClicked() {
    // Check that there's a valid number of players connected
    if (this.getConnectedDevices().size() < Constants.MIN_PLAYERS - 1) {
      Toast.makeText(this.getToaster(), "You don't have enough players, the game requires " + "at least 3 players", Toast.LENGTH_LONG).show();
      return;
    }

    // TODO: What is this?
    // Check that lobbied devices contains connected devices
    if (!this.getLobbiedDevices().containsAll(this.getConnectedDevices())) {
      Toast.makeText(this.getToaster(), "All connected devices have not returned to the lobby.", Toast.LENGTH_LONG).show();
      return;
    }

    // Check that we're in the LOBBY state
    if (this.getState() != STATE_LOBBY) {
      Toast.makeText(this.getToaster(), "Not in LOBBY state.", Toast.LENGTH_LONG).show();
      return;
    }

    // Set ourselves as the start device
    this.setStartDevice(this.getConnectService().getLocalAddress());
    this.clearLobbiedDevices();

    // Send off START messages to all connected devices, add all devices to unacked
    for (String currDevice : this.getConnectedDevices()) {
      this.getConnectService().write(currDevice, HEADER_START);
      this.unackedDevices.add(currDevice);
    }

    this.setState(STATE_SET_START);
  }

  /**
   * Chooses which device we will be sending to. Also informs all other devices
   */
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
      nextDeviceAddress = this.getStartDevice();
    }

    // Set up the successor packet
    ByteBuffer msg = ByteBuffer.allocate(HEADER_LENGTH + 4 + ADDRESS_LENGTH);
    msg.put(HEADER_SUCCESSOR);
    msg.putInt(lastSuccessorNumber + 1);
    msg.put(nextDeviceAddress.getBytes());

    // Set nextDevice field to store which device we will send prompts and drawings to
    this.successor = nextDeviceAddress;

    for (String address : this.getConnectedDevices()) {
      this.getConnectService().write(address, msg.array());
    }

    if (isLast) {
      transitionToGame();
    }
  }

  /**
   * @param type
   * @param callbackActivity TODO explain
   * @return true if the network type has been successfully chosen and setup
   */
  public boolean chooseNetworkType(int type, Activity callbackActivity) {
    this.setConnectService(type, callbackActivity.getApplication());
    return (this.getConnectService().setupNetwork(callbackActivity));
  }

  public interface ConnectedDevicesListener {
    void onDeviceStatusChanged(int status, String address, String name);
  }
}
