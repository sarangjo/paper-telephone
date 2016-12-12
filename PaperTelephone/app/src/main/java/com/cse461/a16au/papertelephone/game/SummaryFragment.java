package com.cse461.a16au.papertelephone.game;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.R;

import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;

/**
 * A simple {@link Fragment} subclass.
 */
public class SummaryFragment extends Fragment {
    private String mAddress;
    private ListView mSummariesListView;

    public SummaryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        this.mAddress = args.getString(DEVICE_ADDRESS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_summary, container, false);

        GameData gameData = GameData.getInstance();
        TextView creatorView = (TextView) v.findViewById(R.id.view_creator);

        int positionIndex = gameData.getConnectedDevices().indexOf(mAddress);
        if (positionIndex >= gameData.getConnectedDeviceNames().size()) {
            // TODO: assume this is us?
            creatorView.setText(GameData.localName);
        } else {
            creatorView.setText(gameData.getConnectedDeviceNames().get(positionIndex));
        }


        mSummariesListView = (ListView) v.findViewById(R.id.list_paper_summary);
        ArrayAdapter<byte[]> adapter = new SummaryAdapter(getActivity(), GameData.addressToSummaries.get(mAddress));
        mSummariesListView.setAdapter(adapter);

        return v;
    }

}
