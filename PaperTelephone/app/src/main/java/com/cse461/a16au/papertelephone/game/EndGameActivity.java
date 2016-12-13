package com.cse461.a16au.papertelephone.game;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.cse461.a16au.papertelephone.R;

import java.util.ArrayList;
import java.util.List;

import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.RESULT_LOBBY;
import static com.cse461.a16au.papertelephone.Constants.RESULT_RESTART;
import static com.cse461.a16au.papertelephone.game.GameData.devicesAtStartGame;
import static com.cse461.a16au.papertelephone.game.GameData.namesAtStartGame;

public class EndGameActivity extends AppCompatActivity {
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

        // ViewPager
        PagerAdapter pagerAdapter = new SummaryPagerAdapter(getSupportFragmentManager());

        // Merge the start game lists and current lists

        GameData mGameData = GameData.getInstance();
        List<String> endGameDevices = mGameData.getConnectedDevices();
        List<String> endGameNames = mGameData.getConnectedDeviceNames();

        for (int i = 0; i < endGameDevices.size(); i++) {
            if (!devicesAtStartGame.contains(endGameDevices.get(i))) {
                devicesAtStartGame.add(endGameDevices.get(i));
                namesAtStartGame.add(endGameNames.get(i));
            }
        }

        ViewPager pager = (ViewPager) findViewById(R.id.pager_summary);

        if(pager.getAdapter() != null) {
            pager.setAdapter(null);
        }
        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(0, false);

    }

    private class SummaryPagerAdapter extends FragmentStatePagerAdapter {
        SummaryPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment frag = new SummaryFragment();
            Bundle args = new Bundle();
            if (position == 0) {
                args.putString(DEVICE_ADDRESS, GameData.localAddress);
            } else {
                args.putString(DEVICE_ADDRESS, devicesAtStartGame.get(position - 1));
            }
            frag.setArguments(args);
            return frag;
        }

        public int getCount() {
            return devicesAtStartGame.size() + 1;
        }
    }
}
