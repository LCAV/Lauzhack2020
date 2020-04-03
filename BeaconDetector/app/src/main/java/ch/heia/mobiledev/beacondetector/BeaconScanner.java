package ch.heia.mobiledev.beacondetector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

class BeaconScanner {
    // interface to be implemented by the owner
    interface BeaconScannerCallback {
        void OnBeaconDiscovered(Beacon beacon);

    }

    // used for logging
    private static final String TAG = BeaconActivity.class.getSimpleName();

    // Bluetooth related data members
    private BluetoothLeScanner mBluetoothLeScanner;

    // scanner callback
    private BeaconScannerCallback mCallback;

    // constructor
    BeaconScanner() {
    }

    // called for initialization
    boolean Initialize(Activity activity) {
        try {
            mCallback = (BeaconScannerCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement BeaconScannerCallback");
        }

        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            // show an error message to the user
            CharSequence text = "Bluetooth not supported on this device";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(activity, text, duration);
            toast.show();
            return false;
        }

        // get the Bluetooth adapter
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Cannot get Bluetooth adapter");

            // show error
            showError(activity, "Cannot get Bluetooth adapter");

            return false;
        }

        // check whether bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            // start the activity for enabling bluetooth
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(enableBluetoothIntent);

            return false;
        }

        // get a Bluetooth scanner
        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothLeScanner == null) {
            Log.e(TAG, "Cannot get Bluetooth scanner");

            // show error
            showError(activity, "Cannot get Bluetooth scanner");

            return false;
        }

        return true;
    }

    void startScan(Handler handler, long scanPeriod, ScanSettings settings, List<ScanFilter> filters) {
        if (scanPeriod > 0) {
            // create the handler for stopping scanning after period
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mBluetoothLeScanner != null && mLeScanCallback != null) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                    }
                }
            }, scanPeriod);
        }

        // create the call
        // start scanning

        // EG
        //mBluetoothLeScanner.startScan(mLeScanCallback);
        mBluetoothLeScanner.startScan(filters, settings, mLeScanCallback);

    }

    void stopScan(Handler handler) {
        mBluetoothLeScanner.stopScan(mLeScanCallback);
    }


    // scan callback called upon device discovery
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            if (result.getScanRecord() != null) {
                Beacon beacon = Beacon.createFromScanResult(result.getScanRecord().getBytes(), result.getRssi(), result.getDevice());
                if (beacon != null) {
                    Log.d(TAG, "Beacon found: ID = " + beacon.getFullID());
                    Log.v(TAG, "\tTxPower = " + beacon.getTxPower() + ", RSSI = " + beacon.getRSSI() + ", distance = " + beacon.computeDistance() + "m");
                    mCallback.OnBeaconDiscovered(beacon);
                }
            }else{
                Log.v(TAG, "What is this beacon?");
            }
        }
    };

    // for showing errors to the user
    private void showError(Context context, String message) {
        // show message in a Toast
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }
}
