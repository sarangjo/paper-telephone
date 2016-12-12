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
import android.widget.ListView;

import com.cse461.a16au.papertelephone.R;

import java.util.ArrayList;
import java.util.List;

import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.RESULT_LOBBY;
import static com.cse461.a16au.papertelephone.Constants.RESULT_RESTART;

public class EndGameActivity extends AppCompatActivity {
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    private ListView mSummariesListView;

    private List<String> mAddresses;

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
        mAddresses = new ArrayList<>(GameData.addressToSummaries.keySet());

        mPager = (ViewPager) findViewById(R.id.pager_summary);
        mPagerAdapter = new SummaryPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
    }

    private class SummaryPagerAdapter extends FragmentStatePagerAdapter {
        public SummaryPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment frag = new SummaryFragment();
            Bundle args = new Bundle();
            args.putString(DEVICE_ADDRESS, mAddresses.get(position));
            frag.setArguments(args);
            return frag;
        }

        public int getCount() {
            return mAddresses.size();
        }
    }
}
