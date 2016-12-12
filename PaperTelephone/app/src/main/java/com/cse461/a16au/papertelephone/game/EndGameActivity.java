package com.cse461.a16au.papertelephone.game;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.R;

import static com.cse461.a16au.papertelephone.Constants.RESULT_LOBBY;
import static com.cse461.a16au.papertelephone.Constants.RESULT_RESTART;

public class EndGameActivity extends AppCompatActivity {
    ListView mSummariesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_game);

        Button returnToLobbyButton = (Button) findViewById(R.id.button_return_to_lobby);
        Button restartButton = (Button) findViewById(R.id.button_restart);

        returnToLobbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_LOBBY);
                finish();
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_RESTART);
                finish();
            }
        });

        mSummariesListView = (ListView) findViewById(R.id.list_paper_summary);

        TextView creatorView = (TextView) findViewById(R.id.view_creator);
        creatorView.setText(GameData.mName);

        ArrayAdapter<byte[]> adapter = new PaperSummaryAdapter(this, GameData.addressToSummaries.get(GameData.mLocalAddress));
        mSummariesListView.setAdapter(adapter);
    }


}
