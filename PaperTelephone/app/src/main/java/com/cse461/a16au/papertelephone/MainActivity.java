package com.cse461.a16au.papertelephone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.lobby.LobbyActivity;

import static com.cse461.a16au.papertelephone.GameController.STATE_LOBBY;
import static com.cse461.a16au.papertelephone.services.ConnectServiceFactory.BLUETOOTH;

public class MainActivity extends FragmentActivity implements GameController.StateChangeListener {
  private static final String TAG = "MainActivity";
  private Button buttonStartGame;
  private GameController controller;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    controller = GameController.getInstance();
    controller.registerStateChangeListener(this);

    buttonStartGame = (Button) findViewById(R.id.button_bluetooth);
    buttonStartGame.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            controller.setConnectService(BLUETOOTH);
          }
        });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case Constants.REQUEST_ENABLE_BT:
        // Setup game and connect to other devices now that bluetooth is enabled
        if (resultCode == Activity.RESULT_OK) {
          Log.d(TAG, "Successfully enabled Bluetooth.");
          buttonStartGame.setEnabled(true);
        } else {
          // User did not enable Bluetooth or an error occurred
          Log.d(TAG, "BT not enabled");
          Toast.makeText(this, R.string.bt_not_enabled_exit, Toast.LENGTH_LONG).show();
          finish();
        }
        break;
    }
  }

  @Override
  public void onStateChange(int newState, int oldState) {
    if (newState == STATE_LOBBY) {
      startActivity(new Intent(MainActivity.this, LobbyActivity.class));
      this.controller.unregisterStateChangeListener(this);
    }
  }
}
