package ch.heia.mobiledev.beacondetector;

import android.app.ListFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BeaconListFragment extends ListFragment
{
  // used for logging
  private static final String TAG = BeaconListFragment.class.getSimpleName();

  // adapter used for listing beacons
  private BeaconListAdapter mAdapter;

  // EG: The current beacon displayed. We need it to know if how to refresh the details view or not
  private String currentDisplayedBeaconID;

  // required empty constructor
  public BeaconListFragment()
  {

  }

  public String getCurrentDisplayedBeaconID(){ return currentDisplayedBeaconID; }

  // called for adding a beacon
  public void addBeacon(Beacon beacon)
  {
    mAdapter.addBeacon(beacon);
    mAdapter.notifyDataSetInvalidated();
    if (mAdapter.getCount() == 1)
    {
      getListView().setItemChecked(0, true);
      getListView().setSelection(0);
      selectDetails(0);
      selectDetails(0);
    }
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    Log.d(TAG, "onCreateView()");

    View view = inflater.inflate(R.layout.beacon_list_fragment, container, false);

    mAdapter = new BeaconListAdapter();
    setListAdapter(mAdapter);

    return view;
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id)
  {
    super.onListItemClick(l, v, position, id);
    selectDetails(position);

  }

  // sets the details
  private void selectDetails(int position)
  {
    // display the beacon info in the beacon details fragment
    BeaconDetailsFragment beaconDetailsFragment = (BeaconDetailsFragment) getFragmentManager().findFragmentById(R.id.beacon_details_fragment);
    Beacon beacon = (Beacon) mAdapter.getItem(position);
    beaconDetailsFragment.setFullID(beacon.getFullID());
    beaconDetailsFragment.setTxPower(beacon.getTxPower());
    beaconDetailsFragment.setRSSI(beacon.getRSSI());
    beaconDetailsFragment.showHint(beacon.getMinor());

    getListView().setSelector(android.R.color.holo_blue_light);
    getListView().invalidateViews();

    currentDisplayedBeaconID = beacon.getAddress();
  }

  // subclass acting as ListAdapter for listing present beacons
  class BeaconListAdapter extends BaseAdapter
  {
    final List<Beacon> mItems;

    BeaconListAdapter()
    {
      mItems = new ArrayList<>();
    }

    // called for adding a beacon
    void addBeacon(Beacon beacon)
    {
      mItems.add(beacon);
      notifyDataSetInvalidated();
    }

    @Override
    public int getCount()
    {
      return mItems.size();
    }

    @Override
    public Object getItem(int position)
    {
      return mItems.get(position);
    }

    @Override
    public long getItemId(int position)
    {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
      if (convertView == null)
      {
        convertView = new TextView(getActivity().getApplicationContext());
      }
      else
      {
        if (! (convertView instanceof TextView))
        {
          convertView = new TextView(getActivity().getApplicationContext());
        }
      }

      // at this stage convertView is a TextView
      TextView tv = (TextView) convertView;
      tv.setText(Integer.toString(mItems.get(position).getMinor()));
      tv.setTextColor(Color.BLACK);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);

      return tv;
    }
  }
}
