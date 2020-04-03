package ch.heia.mobiledev.beacondetector;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by serge.ayer on 17.05.2016.
 */
public class BeaconDetailsFragment extends Fragment
{
  // used for logging
  private static final String TAG = BeaconDetailsFragment.class.getSimpleName();

  // reference to the views
  private TextView mFullIDView;
  private TextView mTxPowerView;
  private TextView mHintView;
  private TextView mRSSIView;

  private long lastRefresh = -1;

  // required empty constructor
  public BeaconDetailsFragment()
  {

  }


  @Override
  public void onAttach(Context context)
  {
    Log.d(TAG, "onAttach()");
    super.onAttach(context);
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    Log.d(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    Log.d(TAG, "onCreateView");

    View view = inflater.inflate(R.layout.beacon_details_fragment, container, false);

    mFullIDView = (TextView) view.findViewById(R.id.beacon_fullid);
    mHintView = (TextView) view.findViewById(R.id.beacon_hint);
    mTxPowerView = (TextView) view.findViewById(R.id.beacon_tx_power);
    mRSSIView = (TextView) view.findViewById(R.id.beacon_rssi);

    return view;
  }

  void setFullID(String url)
  {
    if (mFullIDView != null)
    {
      mFullIDView.setText(url);
    }
  }

  void setTxPower(int power)
  {
    if (mTxPowerView != null)
    {
      mTxPowerView.setText("Tx: " + Integer.toString(power) + "dBm");
    }
  }

  void setRSSI(int rssi)
  {
    if (System.currentTimeMillis()-lastRefresh > 50) {
      mRSSIView.setText("RSSI: " + Integer.toString(rssi) + "dBm");
      lastRefresh = System.currentTimeMillis();
    }
  }



  void showHint(int minor)
  {
    // create the fragment for download
    DownloadAsyncTask downloadAsyncTask = new DownloadAsyncTask();

    String url = "";
    switch (minor)
    {
      case 246:
        url = "https://drive.switch.ch/index.php/s/K0gP07NGHXeEr6I/download";
        break;

      case 247:
        url = "https://drive.switch.ch/index.php/s/YqVCLeaF60hz9rw/download";
        break;

      case 248:
        url = "https://drive.switch.ch/index.php/s/hI0WhA4LnrRdkUh/download";
        break;

      case 249:
        url = "https://drive.switch.ch/index.php/s/lO7n6ncMatyKau5/download";
        break;
    }

    if (! url.equals(""))
    {
      downloadAsyncTask.execute(url);
    }
  }


  // internal class implementing the download
  private class DownloadAsyncTask extends AsyncTask<String, Void, String>
  {
    // callers
    @Override
    protected void onPreExecute()
    {
      // nothing to do
    }

    @Override
    protected String doInBackground(String... urls)
    {
      if (urls.length != 1)
      {
        Log.e(TAG, "Not implemented");
        return null;
      }

      // start download and save the results to the output file
      InputStream stream = null;
      String hint = "";
      try
      {
        // open the url connection
        URL url = new URL(urls[0]);
        URLConnection urlConnection = url.openConnection();
        stream = urlConnection.getInputStream();

        // get the size of the file to be downloaded
        int file_size = urlConnection.getContentLength();

        // download using a buffer of buffer_size bytes
        int old_percent = 0;
        int download_size = 0;
        final int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int next;
        while ((next = stream.read(buffer, 0, bufferSize)) != -1 && !isCancelled())
        {
          download_size += next;

          int percent = (int) (((double) (download_size) * 100) / (double) file_size);
          if (percent > old_percent)
          {
            old_percent = percent;
          }

          String addHint = new String(buffer);
          hint = hint + addHint;
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      finally
      {
        if (stream != null)
        {
          try
          {
            stream.close();
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
        }
      }

      return hint;
    }

    @Override
    protected void onCancelled()
    {
      // nothing to do
    }

    @Override
    protected void onPostExecute(String hint)
    {
      // show the hint
      mHintView.setText(hint);
    }
  }
}
