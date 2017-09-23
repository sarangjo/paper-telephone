package com.cse461.a16au.papertelephone;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cse461.a16au.papertelephone.services.ConnectService;
import com.cse461.a16au.papertelephone.services.ConnectServiceFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import static com.cse461.a16au.papertelephone.Constants.ADDRESS_LENGTH;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.DEVICE_NAME;
import static com.cse461.a16au.papertelephone.Constants.HEADER_START_ACK;
import static com.cse461.a16au.papertelephone.Constants.READ_DEVICES;
import static com.cse461.a16au.papertelephone.Constants.READ_START;

/**
 * Central state manager and game logic controller.
 * TODO: rename to MainController or something?
 */

public class GameController {
  public static final String TAG = "GameController";

  private static GameController ourInstance = new GameController();
  /**
   * The current state of the game.
   */
  private int state;
  private String startDevice;
  private ConnectService connectService;
  private Set<String> unplacedDevices;
  private List<String> connectedDevices;
  private Set<StateChangeListener> stateChangeListeners;

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
            //int newTurnsLeft = buf.getInt();
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
        break;
      case STATE_PLACEMENT:
        break;
      case STATE_IN_GAME:
        break;
      case STATE_POST_GAME:
        break;
    }
  }

  private void clearUnplacedDevices() {
    this.unplacedDevices.clear();
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

    this.connectService.registerPacketHandler(packetHandler);
  }

  public List<String> getConnectedDevices() {
    return connectedDevices;
  }

  // TODO: LISTEN TO STATE CHANGE
  // The game controller will send out updates when the state has changed

  /**
   * TODO: make only listen for specific state changes eventually so as to not wastefully spawn
   * crazy numbers of threads
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
}
