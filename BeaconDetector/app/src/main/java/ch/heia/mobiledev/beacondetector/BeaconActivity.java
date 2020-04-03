package ch.heia.mobiledev.beacondetector;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import android.bluetooth.le.ScanSettings; // EG: to configure our own settings
import android.os.SystemClock; // EG: for sntp
import android.provider.Settings.Secure;  // EG: for androi_id
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static android.R.attr.id;
import static ch.heia.mobiledev.beacondetector.R.styleable.View;
import static java.security.AccessController.getContext;

public class BeaconActivity extends Activity implements BeaconScanner.BeaconScannerCallback {
    // used for logging
    private static final String TAG = BeaconActivity.class.getSimpleName();

    // used for permissions
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    // used for scan
    private static final long SCAN_PERIOD = -1;//60 * 60000;

    // scanner instance
    private BeaconScanner mBeaconScanner;

    // list of detected acons
    private final List<Beacon> mBeaconList = new ArrayList<>();

    // handler for stop scanning after a given period
    private final Handler mHandler = new Handler();

    // EG
    private UdpClientSend udpClient;
    private SntpClient sntpClient;
    private boolean isSntpTimeSet = false;
    private ScanSettings scanSettings;
    private String udp_ip;
    private int udp_port = 7582;
    private String android_id;

    private boolean useFilters = false;
    private List<ScanFilter> filters = new ArrayList<ScanFilter>();
    private String[] addressList = new String[]{
            "2F:63:2B:F6:E2:E4", //236 NEW
            //"26:69:53:19:78:77", //236 OLD
            "1E:FB:18:79:EA:2B", //237 NEW
            //"2F:C9:8D:EA:E4:23", //237 OLD
            //"29:A3:B5:00:E8:B3", //238
            //"11:15:D8:10:86:54", //239
            //"00:7D:19:B3:1D:53", //240
            "CC:1C:74:85:3D:85", //1  = 241
            //"E8:3C:CC:10:53:D4", //2  = 242
            "F7:4B:E3:C6:1C:C1"  //772
        };


    // String m_wlanMacAdd; EG to get phone ID


    // callback method
    public void OnBeaconDiscovered(Beacon beacon) {
        // get the reference to the BeaconList fragment
        BeaconListFragment beaconListFragment = (BeaconListFragment) getFragmentManager().findFragmentById(R.id.beacon_list_fragment);
        // compare the address to see we already know the beacon
        if (!mBeaconList.contains(beacon)) {

            // add the beacon to the fragment
            if (beaconListFragment != null) {
                beaconListFragment.addBeacon(beacon);

                // add the beacon to the list
                mBeaconList.add(beacon);
            }
        } else {
            // EG: update the beacon in list and refresh the view:

            // Go through the list of known beacons until we find the one corresponding to "beacon"
            for (Beacon b : mBeaconList) {
                if (b.equals(beacon)) {
                    // We found the beacon in the list, let's update its rssi and everything
                    b.setRSSI(beacon.getRSSI());
                    b.setTxPower(beacon.getTxPower());
                    b.setUUID(beacon.getUUID());

                    // We check if the beacon is currently displayed
                    if (beaconListFragment.getCurrentDisplayedBeaconID().equals(b.getAddress())) {
                        //since the beacon is the one which is displayed, we update the "details view"
                        BeaconDetailsFragment beaconDetailsFragment = (BeaconDetailsFragment) getFragmentManager().findFragmentById(R.id.beacon_details_fragment);
                        beaconDetailsFragment.setRSSI(b.getRSSI());
                        beaconDetailsFragment.setTxPower(b.getTxPower());
                        beaconDetailsFragment.setFullID(b.getFullID());
                    }
                    // Since we found the beacon in the list, we can break the for loop
                    break;
                }
            }
        }


        // EG
        //setSntpTime if not done yet
        if (!isSntpTimeSet) {

            new Thread(new Runnable() {
                public void run() {
                    if (sntpClient.requestTime("time2.ethz.ch", 60000)) {
                        isSntpTimeSet = true;
                    } else {
                        Log.i(TAG, "sntpTimeNotSetYet");
                        return;
                    }
                }
            }).start();

        }
        long now = sntpClient.getNtpTime() + SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference();
        Log.v(TAG, "NOW: " + now);
        Log.v(TAG, "android id: " + android_id);



        String msg =
                "{\"PositionTS\":\"" + now +
                        "\",\"Address\":\"" + beacon.getAddress() +
                        "\",\"UUID\":\"" + beacon.getUUID() +
                        "\",\"Major\":\"" + beacon.getMajor() +
                        "\",\"Minor\":\"" + beacon.getMinor() +
                        "\",\"TxPower\":\"" + beacon.getTxPower() +
                        "\",\"Phone_ID\":\"" + android_id +
                        "\",\"RSSI\":\"" + beacon.getRSSI() + "\"}";
        try {
            udpClient.send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android_id = Secure.getString(getApplicationContext().getContentResolver(),Secure.ANDROID_ID);

        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        // create udp client to send data to central machine
        //udpClient = new UdpClientSend("160.98.101.229", 7582);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String defaultValue = getResources().getString(R.string.udp_ip_default);
        udp_ip = sharedPref.getString(getString(R.string.udp_ip_saved), defaultValue);
        Log.d(TAG, "Saved IP is " + udp_ip);

        udpClient = new UdpClientSend(udp_ip, udp_port);
        sntpClient = new SntpClient();

        if (useFilters && addressList.length > 0) {
            for (int k = 0; k<addressList.length; k++) {
                ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(addressList[k]).build();
                filters.add(filter);
            }
        }else {
            filters = null;
        }


        //EG: We create our own scansettings since we want to be in "low latency mode" instead of "low power mode"
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        scanSettings = settingsBuilder.build();

        //Entry text of UDP IP
        final EditText udpIpEntry = (EditText) findViewById(R.id.udp_ip_entry);
        udpIpEntry.setText(udp_ip);

        // Define the function to run when the button is clicked
        Button udpIpButton = (Button) findViewById(R.id.udp_ip_button);
        udpIpButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //String new_udp_ip = getResources().getString(R.string.udp_ip);
                String new_udp_ip = udpIpEntry.getText().toString();
                if (!udp_ip.equals(new_udp_ip)) {
                    udpClient.close();
                    udp_ip = new_udp_ip;
                    udpClient = new UdpClientSend(udp_ip, udp_port);
                    Log.d(TAG, "New udp client created with ip " + udp_ip);

                    SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.udp_ip_saved), udp_ip);
                    editor.commit();
                    Log.d(TAG, "Saved new IP");
                }
            }
        });
        //WifiManager m_wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //m_wlanMacAdd = m_wm.getConnectionInfo().getMacAddress();

        // create the beacon scanner
        mBeaconScanner = new BeaconScanner();
        if (mBeaconScanner.Initialize(this)) {
            // handle permissions
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    showMessageOKCancel(
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                                }
                            });
                } else {
                    // No explanation needed, we can request the permission.

                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                // start scanning
                mBeaconScanner.startScan(mHandler, SCAN_PERIOD, scanSettings, filters);
            }
        }

        // create the job

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onStop()");
        mBeaconScanner.stopScan(mHandler);
    }

    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage("You need to allow access to Location !")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted
                    mBeaconScanner.startScan(mHandler, SCAN_PERIOD, scanSettings, filters);
                }
                //else
                //{
                // permission denied
                //}
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}
