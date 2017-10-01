package com.cse461.a16au.papertelephone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.cse461.a16au.papertelephone.lobby.LobbyActivity;
import com.cse461.a16au.papertelephone.services.ConnectServiceFactory;

import static com.cse461.a16au.papertelephone.GameController.STATE_LOBBY;

public class MainActivity extends FragmentActivity implements GameController.StateChangeListener {
  private static final String TAG = "MainActivity";
  private GameController mController;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mController = GameController.getInstance();
    mController.registerStateChangeListener(this);

    ListView networkTypeSelector = (ListView) findViewById(R.id.network_type_selector);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
        android.R.id.text1, ConnectServiceFactory.NETWORK_TYPES);
    networkTypeSelector.setAdapter(adapter);
    networkTypeSelector.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mController.chooseNetworkType(i, MainActivity.this);
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case Constants.REQUEST_ENABLE_BT:
        // Setup game and connect to other devices now that bluetooth is enabled
        if (resultCode == RESULT_OK) {
          Log.d(TAG, "Successfully enabled Bluetooth.");
          mController.setState(STATE_LOBBY);
        } else {
          // User did not enable Bluetooth or an error occurred
          Log.d(TAG, "BT not enabled");
          Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_LONG).show();
        }
        break;
    }
  }

  @Override
  public void onStateChange(int newState, int oldState) {
    if (newState == STATE_LOBBY) {
      startActivity(new Intent(MainActivity.this, LobbyActivity.class));
      this.mController.unregisterStateChangeListener(this);
    }
  }
}
