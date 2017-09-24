package com.cse461.a16au.papertelephone;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.services.ConnectService;
import com.cse461.a16au.papertelephone.services.ConnectServiceFactory;

import java.nio.ByteBuffer;
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

/**
 * Central state manager and game logic controller.
 * TODO: rename to MainController or something?
 * TODO: this needs a lot of synchronization monitoring
 */

public class GameController {
  public static final String TAG = "GameController";

  private static GameController ourInstance = new GameController();
  /**
   * The current state of the game.
   */
  private int state;
  private String startDevice;
  private boolean isStart;
  private ConnectService connectService;
  private Set<String> unplacedDevices;
  private int lastSuccessorNumber;
  private List<String> connectedDevices;
  private List<String> connectedDeviceNames;
  private Set<String> lobbiedDevices;
  private Set<String> unackedDevices;
  private Set<StateChangeListener> stateChangeListeners;
  private ConnectedDevicesListener connectedDevicesListener;
  private String successor;

  public static GameController getInstance() {
    return ourInstance;
  }

  private GameController() {
    this.setState(STATE_PRE_LOBBY);
  }

  public static final int STATE_PRE_LOBBY = 0;
  public static final int STATE_LOBBY = 1;
  public static final int STATE_SET_START = 2;
  public static final int STATE_PLACEMENT = 3;
  public static final int STATE_IN_GAME = 4;
  public static final int STATE_POST_GAME = 5;


  public synchronized int getState() {
    return state;
  }

  public synchronized void setState(int state) {
    this.state = state;
    for (StateChangeListener l : this.stateChangeListeners) {
      l.onStateChange(state, 0);
    }
  }

  // MESSAGE HANDLERS

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
    String deviceName = msg.getData().getString(DEVICE_NAME);
    String deviceAddress = msg.getData().getString(DEVICE_ADDRESS);

    // TODO: switch this from msg.arg2 to msg.what since the msg.what's have been isolated
    if (msg.arg2 == READ_PING) {
      // TODO send ping back to current toaster
    }
    switch (this.getState()) {
      case STATE_PRE_LOBBY:
        switch (msg.arg2) {
          default:
            Log.e(TAG, "Illegal packet received.");
            break;
        }
        break;
      case STATE_LOBBY:
        switch (msg.arg2) {
          case READ_START:
            // Immediately jump into PLACEMENT mode
            this.setState(STATE_PLACEMENT);

            this.setStartDevice(deviceAddress);
            this.getConnectService().write(deviceAddress, HEADER_START_ACK);
            this.clearUnplacedDevices();
            break;
          case READ_DEVICES:
            ByteBuffer buf = ByteBuffer.wrap((byte[]) msg.obj);

            // Logic for receiving devices mid-game
            int newTurnsLeft = buf.getInt();
            //isGameActive = newTurnsLeft > 0;
            //if (isGameActive) {
            //    turnsLeft = newTurnsLeft + 1; // offset from GameActivity's setupRound
            //}

            // Connect to all devices that we are not already connected to
            int numDevices = buf.getInt();
            for (int i = 0; i < numDevices; i++) {
              byte[] address = new byte[ADDRESS_LENGTH];
              buf.get(address);
              String addr = new String(address);

              if (this.getConnectedDevices().indexOf(addr) >= 0) {
                Log.d(TAG, "Already connected to " + addr);
              } else {
                // mGameData.addDeviceToConnectTo(addr);
                this.getConnectService().connect(addr);
              }
            }

            // Logic for joining mid-game
            // Cross this off our list of devices to connect to
            // mGameData.removeDeviceToConnectTo(deviceAddress);
            // // This is the case where we are joining a game in progress
            // if (mGameData.isDoneConnectingToGameDevices() && isGameActive) {
            //   try {
            //     Thread.sleep(250);
            //   } catch (InterruptedException e) {
            //     e.printStackTrace();
            //   }
            //   Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
            //   intent.putExtra(Constants.JOIN_MID_GAME, true);
            //   startActivityForResult(intent, Constants.REQUEST_PLAY_GAME);
            // }
            break;
          default:
            Log.e(TAG, "Illegal packet received.");
            break;
        }
        break;
      case STATE_SET_START:
        switch (msg.arg2) {
          case READ_START:
            break;
          case READ_START_ACK:
            this.removeUnackedDevice(deviceAddress);
            if (this.unackedDevices.isEmpty()) {
              this.isStart = true;
              // Setup unplaced devices, i.e. all devices except for us
              unplacedDevices.clear();

              for (String device : this.getConnectedDevices()) {
                unplacedDevices.add(device);
              }

              chooseSuccessor();

              this.setState(STATE_PLACEMENT);
            }
            break;
          default:
            Log.e(TAG, "Illegal packet received.");
            break;
        }
        break;
      case STATE_PLACEMENT:
        break;
      case STATE_IN_GAME:
        break;
      case STATE_POST_GAME:
        break;
    }
  }

  private void removeUnackedDevice(String deviceAddress) {
    this.unackedDevices.remove(deviceAddress);
  }

  private Handler networkHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      GameController.this.handleNetworkUpdate(msg);
    }
  };

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
      case STATE_IN_GAME:
        break;
      case STATE_POST_GAME:
        break;
    }
  }

  // DATA MUTATION/RETRIEVAL

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

  private void clearUnplacedDevices() {
    this.unplacedDevices.clear();
  }

  public synchronized String getStartDevice() {
    return this.startDevice;
  }

  public synchronized void setStartDevice(String startDevice) {
    this.startDevice = startDevice;
  }

  public synchronized ConnectService getConnectService() {
    return connectService;
  }

  public synchronized void setConnectService(int connectServiceType) {
    this.connectService = ConnectServiceFactory.getService(connectServiceType);
    assert this.connectService != null;

    // Setup handlers for various messages from connect service
    this.connectService.registerPacketHandler(packetHandler);
    this.connectService.registerNetworkHandler(networkHandler);
  }

  // CONNECTED DEVICES

  public List<String> getConnectedDevices() {
    return connectedDevices;
  }

  public List<String> getConnectedDeviceNames() {
    return connectedDeviceNames;
  }

  public Set<String> getLobbiedDevices() {
    return this.lobbiedDevices;
  }

  public void clearLobbiedDevices() {
    this.lobbiedDevices.clear();
  }

  public interface ConnectedDevicesListener {
    void onDeviceStatusChanged(int status, String address, String name);
  }

  // STATE CHANGE

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

  /**
   * A listener that will be called when the game state changes.
   */
  interface StateChangeListener {
    void onStateChange(int newState, int oldState);
  }

  // ACTUAL OPERATIONS

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

  public void startGameClicked() {
    boolean allLobbied = true;

    for (String device : this.getConnectedDevices()) {
      allLobbied = allLobbied && this.getLobbiedDevices().contains(device);
    }

    if (this.getConnectedDevices().size() >= Constants.MIN_PLAYERS - 1 && allLobbied) {
      this.clearLobbiedDevices();

      if (this.getState() == STATE_LOBBY) {
        this.isStart = true;
        this.setStartDevice(null);

        for (String currDevice : this.getConnectedDevices()) {
          this.getConnectService().write(currDevice, HEADER_START);
          this.unackedDevices.add(currDevice);
        }
      } else {
        Toast.makeText(this, "Someone else hit start", Toast.LENGTH_LONG).show();
      }
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

}
