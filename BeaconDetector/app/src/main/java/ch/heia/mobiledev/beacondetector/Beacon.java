package ch.heia.mobiledev.beacondetector;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

public class Beacon implements Parcelable {
    // for logging
    private final static String TAG = Beacon.class.getSimpleName();

    // data members representing the URI beacon
    private int mMajor;
    private int mMinor;
    private int mTxPower;
    private int mUriScheme;
    private int mRSSI;
    private String mUUID;
    private String mAddress;
    private String mOther;


    private Beacon(String mAddress, String mUUID, int mMajor, int mMinor, int mTxPower, int mRSSI) {
        this.mAddress = mAddress;
        this.mUUID = mUUID;
        this.mMajor = mMajor;
        this.mMinor = mMinor;
        this.mTxPower = mTxPower;
        this.mRSSI = mRSSI;
    }

    // constructor from Parcel
    private Beacon(Parcel parcel) {
        mMinor = parcel.readInt();
        mMajor = parcel.readInt();
        mTxPower = parcel.readInt();
        mUriScheme = parcel.readInt();
        mUUID = parcel.readString();
        mAddress = parcel.readString();
    }

    // Parcelable implementation
    public static final Creator<Beacon> CREATOR = new Creator<Beacon>() {
        @Override
        public Beacon createFromParcel(Parcel in) {
            return new Beacon(in);
        }

        @Override
        public Beacon[] newArray(int size) {
            return new Beacon[size];
        }
    };

    // accessors
    String getAddress() {
        return mAddress;
    }

    String getUUID() {
        return mUUID;
    }

    int getMinor() {
        return mMinor;
    }

    int getMajor() {
        return mMajor;
    }

    int getTxPower() {
        return mTxPower;
    }

    void setTxPower(int txPower) {
        mTxPower = txPower;
    }

    int getRSSI() {
        return mRSSI;
    }

    void setRSSI(int rssi) {
        mRSSI = rssi;
    }

    void setUUID(String uuid) {
        mUUID = uuid;
    }

    String getFullID() {
        return mUUID + ", " + mMajor + ", " + mMinor;
    }

    double computeDistance() {
        return Math.pow(10.0, ((double) (mTxPower - mRSSI)) / 20.0);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Beacon) && ((Beacon) o).mAddress.contentEquals(mAddress);
    }

    // method for creating a beacon from scan record
    public static Beacon createFromScanResult(byte[] scanData, int rssi, BluetoothDevice device) {

        // minimal expected size is 38 bytes (or 17 byte for ubimesh adv). Otherwise we do nothing and return NULL
        if (scanData.length < 38) {
            return null;
        }

        // UbiMesh A/B/C
        int byte_index = 2;
        String ubiKey = "Ubi";
        boolean its_a_match = true;

        // Iterate of the 3 first byte of the payload to check if it matches with "Ubi"
        for (int i = 0; i < ubiKey.length(); i++) {
            char ch = ubiKey.charAt(i);
            its_a_match &= ((char) (scanData[byte_index])) == ch;
            byte_index++;
        }
        // if it is an Ubi advetisement packet
        if (its_a_match) {
            // It can be "UbiMesh" or "UbiTest" or whatever. So let's place the byte index after the next "space" character
            boolean spaceFound = false;
            while (!spaceFound) {
                spaceFound = ((char) (scanData[byte_index])) == ' ';
                byte_index++;
            }
            // We could get the channel and even calibrated (tx,N).
            // But for now we will simply forward the payload to the python app as the uuid

            // we get the advertising channel A B or C (37, 38 or 39)
            char channel = (char) (scanData[byte_index]);
            byte_index += 2;


            /*
            // Now we get the txpower stored as an octal (1 byte)
            char txpower_payload = (char) scanData[byte_index];
            byte_index += 2;

            // Finally get the parameter N which is stored in 4 bytes (float 32)
            String n_payload = scanData.toString().substring(byte_index, byte_index+4);

            //String uuidBytes = "UbiMesh-" + channel + txpower_payload + "-" + n_payload;
            */

            // keep the payload as is and store it into the uuid
            String uuid = bytesToASCII(scanData, 2, 12);
            uuid += bytesToHex(scanData, 12, 13); // get tx
            uuid += " ";
            uuid += bytesToHex(scanData, 14, 18);
            uuid += " ";
            uuid += device.getAddress();
            // arbitrary major, minors and txpower
            int major = 85;
            int minor = 77;
            int txpower= 0;

            return new Beacon(device.getAddress(), uuid, major, minor, txpower, rssi);
        }


        int startByte = 2;
        boolean patternFound = false;
        while (startByte <= 5) {
            if (((int) scanData[startByte + 2] & 0x02) == 0x02) {
                if (((int) scanData[startByte + 3] & 0xff) == 0x16) {
                    // iBeacon
                    patternFound = true;
                    break;
                } else if (((int) scanData[startByte + 3] & 0xff) == 0x15) {
                    // new pixlive beacon type
                    patternFound = true;
                    break;
                }
            }
            startByte++;
        }

        if (patternFound) {
            byte[] uuidBytes = new byte[16];
            System.arraycopy(scanData, startByte + 4, uuidBytes, 0, 16);
            String hexString = bytesToHex(uuidBytes);
            //Here is your UUID
            String uuid = hexString.substring(0, 8) + "-" +
                    hexString.substring(8, 12) + "-" +
                    hexString.substring(12, 16) + "-" +
                    hexString.substring(16, 20) + "-" +
                    hexString.substring(20, 32);

            //Here is your Major value
            int major = (scanData[startByte + 20] & 0xff) * 0x100 + (scanData[startByte + 21] & 0xff);

            //Here is your Minor value
            int minor = (scanData[startByte + 22] & 0xff) * 0x100 + (scanData[startByte + 23] & 0xff);
            int txpower = (int) scanData[startByte + 24];

            return new Beacon(device.getAddress(), uuid, major, minor, txpower, rssi);
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMinor);
        dest.writeInt(mMajor);
        dest.writeInt(mTxPower);
        dest.writeInt(mUriScheme);
        dest.writeString(mUUID);
        dest.writeString(mAddress);
    }

    /**
     * EG
     * bytesToHex method
     * Found on the internet
     * http://stackoverflow.com/a/9855338
     */

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String bytesToHex(byte[] bytes, int start_index, int end_index) {
        return bytesToHex(byte_subbarray(bytes, start_index, end_index));
    }

    private static byte[] byte_subbarray(byte[] bytes, int ind0, int ind1){
        int len = ind1 - ind0;
        byte[] subbytes = new byte[len];
        for (int i = 0; i < len; i++) {
            subbytes[i] = bytes[i+ind0];
        }
        return subbytes;
    }

    private static String bytesToASCII(byte[] bytes){
        String out = "";
        for (int i = 0; i < bytes.length; i++) {
            out += (char) bytes[i];
        }
        return out;
    }

    private static String bytesToASCII(byte[] bytes, int start_index, int end_index){
        return bytesToASCII(byte_subbarray(bytes, start_index, end_index));
    }
}
