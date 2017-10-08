package com.cse461.a16au.papertelephone.lobby;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_CONNECT_FAILED;
import static com.cse461.a16au.papertelephone.Constants.MESSAGE_DISCONNECTED;
import static com.cse461.a16au.papertelephone.Constants.MIN_PLAYERS;
import static com.cse461.a16au.papertelephone.GameController.STATE_IN_GAME_PROMPT;
import static com.cse461.a16au.papertelephone.GameController.STATE_PLACEMENT;

/**
 * Pre-game activity that allows users to discover and connect to other devices and launch a game
 * once they are connected to at least two other devices.
 */
public class LobbyActivity extends AppCompatActivity
    implements DevicesFragment.ConnectDeviceListener,
        GameController.ConnectedDevicesListener,
        GameController.StateChangeListener {
  private static final String TAG = "LobbyActivity";

  /** Adapter for connected devices view */
  private ArrayAdapter<String> mConnectedDevicesNamesAdapter;

  private Button mStartGameButton;
  private GameController mController;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_lobby);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mController = GameController.getInstance();
    mController.registerStateChangeListener(this);
    mController.setToaster(this);

    // Views
    mConnectedDevicesNamesAdapter =
        new ArrayAdapter<>(
            this, android.R.layout.simple_list_item_1, mController.getConnectedDeviceNames());
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

  @Override
  public void onDeviceToConnectToSelected(String address) {
    // Connect to device
    mController.getConnectService().connect(address);
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
        Toast.makeText(
                LobbyActivity.this,
                "Unable to connect: " + name + ". Please try again.",
                Toast.LENGTH_LONG)
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

  @Override
  public void onStateChange(int newState, int oldState) {
    if (oldState == STATE_PLACEMENT && newState == STATE_IN_GAME_PROMPT) {
      Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
      startActivityForResult(intent, Constants.REQUEST_PLAY_GAME);
    }
  }
}
