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

import java.util.ArrayList;
import java.util.List;

import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;
import static com.cse461.a16au.papertelephone.game.GameData.devicesAtStartGame;
import static com.cse461.a16au.papertelephone.game.GameData.namesAtStartGame;

/**
 * A simple {@link Fragment} displaying a summary of the game played, allowing the user to swipe
 * through the "history" of each original prompt.
 */
public class SummaryFragment extends Fragment {
  private String mAddress;

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
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View v = inflater.inflate(R.layout.fragment_summary, container, false);

    TextView creatorView = (TextView) v.findViewById(R.id.view_creator);

    // Find the index of our address
    int positionIndex = devicesAtStartGame.indexOf(mAddress);
    if (positionIndex == -1) {
      creatorView.setText(GameData.localName);
    } else {
      creatorView.setText(namesAtStartGame.get(positionIndex));
    }

    // Tying together the list with the view
    ListView mSummariesListView = (ListView) v.findViewById(R.id.list_paper_summary);
    List<byte[]> display;
    if (GameData.addressToSummaries.containsKey(mAddress)) {
      display = GameData.addressToSummaries.get(mAddress);
    } else {
      display = new ArrayList<>();
    }
    ArrayAdapter<byte[]> adapter = new SummaryAdapter(getActivity(), display);
    mSummariesListView.setAdapter(adapter);

    return v;
  }
}
