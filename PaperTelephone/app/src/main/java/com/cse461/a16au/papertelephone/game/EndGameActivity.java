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
import com.viewpagerindicator.TitlePageIndicator;

import java.util.ArrayList;
import java.util.List;

import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.Constants.RESULT_LOBBY;
import static com.cse461.a16au.papertelephone.Constants.RESULT_RESTART;

public class EndGameActivity extends AppCompatActivity {
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

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
        mPagerAdapter = new SummaryPagerAdapter(getSupportFragmentManager());
        mAddresses = new ArrayList<>(GameData.getInstance().getConnectedDevices());
        mPager = (ViewPager) findViewById(R.id.pager_summary);
        mPager.setCurrentItem(0, false);
        mPager.setOffscreenPageLimit(3);

        if(mPager.getAdapter() != null) {
            mPager.setAdapter(null);
        }
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(0, false);
        //Bind the title indicator to the adapter
        TitlePageIndicator titleIndicator = (TitlePageIndicator)findViewById(R.id.drawings);
        titleIndicator.setViewPager(mPager);

    }

    private class SummaryPagerAdapter extends FragmentStatePagerAdapter {
        public SummaryPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment frag = new SummaryFragment();
            Bundle args = new Bundle();
            if (position == 0) {
                args.putString(DEVICE_ADDRESS, GameData.localAddress);
            } else {
                args.putString(DEVICE_ADDRESS, mAddresses.get(position - 1));
            }
            frag.setArguments(args);
            return frag;
        }

        public int getCount() {
            return mAddresses.size() + 1;
        }
    }
}
