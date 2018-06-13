package com.cse461.a16au.papertelephone.views;

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
import com.cse461.a16au.papertelephone.game.GameData;
import com.cse461.a16au.papertelephone.views.SummaryAdapter;

import java.util.ArrayList;
import java.util.List;

import static com.cse461.a16au.papertelephone.Constants.DEVICE_ADDRESS;

/**
 * A simple {@link Fragment} displaying a summary of the game played, allowing the user to swipe
 * through the "history" of each original prompt.
 */
public class SummaryFragment extends Fragment {
  private String mAddress;
  private GameData gameData;

  public SummaryFragment() {
    // Required empty public constructor
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    this.mAddress = args.getString(DEVICE_ADDRESS);
    this.gameData = GameData.getInstance();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View v = inflater.inflate(R.layout.fragment_summary, container, false);

    TextView creatorView = (TextView) v.findViewById(R.id.view_creator);

    // Find the index of our address
    int positionIndex = gameData.devicesAtStartGame.indexOf(mAddress);
    if (positionIndex == -1) {
      creatorView.setText(gameData.localName);
    } else {
      creatorView.setText(gameData.namesAtStartGame.get(positionIndex));
    }

    // Tying together the list with the view
    ListView mSummariesListView = (ListView) v.findViewById(R.id.list_paper_summary);
    List<byte[]> display;
    if (gameData.addressToSummaries.containsKey(mAddress)) {
      display = gameData.addressToSummaries.get(mAddress);
    } else {
      display = new ArrayList<>();
    }
    ArrayAdapter<byte[]> adapter = new SummaryAdapter(getActivity(), display);
    mSummariesListView.setAdapter(adapter);

    return v;
  }
}
