package com.cse461.a16au.papertelephone.game;

import android.bluetooth.BluetoothAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;

public class EndGameActivity extends AppCompatActivity {
    ListView mSummariesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_game);

        mSummariesListView = (ListView) findViewById(R.id.list_paper_summary);

        TextView creatorView = (TextView) findViewById(R.id.view_creator);
        creatorView.setText(GameData.mName);

        ArrayAdapter<byte[]> adapter = new PaperSummaryAdapter(this, GameData.addressToSummaries.get(GameData.mAddress));
        mSummariesListView.setAdapter(adapter);
    }


}
