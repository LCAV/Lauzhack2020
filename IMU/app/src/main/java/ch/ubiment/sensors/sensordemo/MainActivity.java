package ch.ubiment.sensors.sensordemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;


import ch.ubiment.sensors.sensordemo.Algebra.Quaternion;
import ch.ubiment.sensors.sensordemo.Communication.UdpClientSend;
import ch.ubiment.sensors.sensordemo.OrientationAlgorithms.AccGyroFusion;
import ch.ubiment.sensors.sensordemo.OrientationAlgorithms.GravAccGyroFusion;
import ch.ubiment.sensors.sensordemo.OrientationAlgorithms.OrientationFusion;
import ch.ubiment.sensors.sensordemo.OpenGL.OpenGLRenderer;
import ch.ubiment.sensors.sensordemo.OpenGL.Scene;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


//https://github.com/barbeau/gpstest/blob/master/GPSTest/src/main/java/com/android/gpstest/GpsTestActivity.java#L565


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {
    //TextView testView;
    GLSurfaceView surfaceView;

    private SensorManager mSensorManager;

    //definition of all vectors
    private float[] accelerometerUncalibrated_vector = new float[6];  // TYPE_ACCELEROMETER_UNCALIBRATED is available since api26
    private float[] accelerometer_vector = new float[3];  //accelerometer is already a little bit preprocessed.
    private float[] linearAcceleration_vector = new float[3];  // linear acceleration is gravity free
    private float[] gravity_vector = new float[3];

    private float[] magneticFieldUncalibrated_vector = new float[6];
    private float[] magneticField_vector = new float[3];
    private float[] magneticFieldCalibrated_vector = new float[] {0,0,0};

    private float[] gyroscopeUncalibrated_vector = new float[6];
    private float[] gyroscope_vector = new float[3];

    // quaternions to store the orientation of differents orientation algos
    private Quaternion orientationQuaternion = new Quaternion();
    private Quaternion orientationUncalibratedQuaternion =  new Quaternion();
    private Quaternion rotationVectorQuaternion =  new Quaternion();
    private Quaternion orientationGravMagnQuaternion = new Quaternion();
    private Quaternion fusedOrientationQuaternion = new Quaternion();
    private Quaternion fusedGravAccGyroQuaternion = new Quaternion();
    private Quaternion fusedAccGyroQuaternion = new Quaternion();

    private UbiStepDetector ubiStepDetector = new UbiStepDetector(0.02f);

    // These timestamps are expressed in SECONDS
    private float max_ts = Float.MIN_VALUE;
    private float min_ts = Float.MAX_VALUE;
    private float curr_ts = 0.0f;

    //Definition of Measure switches
    boolean isMeasured_AccelerometerUncalibrated = (android.os.Build.VERSION.SDK_INT < 26); // remains TRUE if api doesnt support this sensor.;
    boolean isMeasured_Accelerometer = FALSE;
    boolean isMeasured_Gravity = FALSE;
    boolean isMeasured_LinearAcceleration = FALSE;

    boolean isMeasured_MagneticFieldUncalibrated = FALSE;
    boolean isMeasured_MagneticField = FALSE;

    boolean isMeasured_GyroscopeUncalibrated = FALSE;
    boolean isMeasured_Gyroscope = FALSE;

    boolean isMeasured_RotationVector = FALSE;

    boolean isCalibrating_Magnetometer = FALSE;

    boolean isAndroidStepDetected = FALSE;   // is step detected by android
    boolean isUbiStepDetected = FALSE;         // is step detected by UbiStep
    int androidStepCountInitalValue = -1;   // Android step counter doesnt start at 0. So we will store the initial value in this variable adn compute the difference at each steps
    int androidStepCountTriggered = -1;     // number of time the android step counter trigger the callback. We start at -1 because the callback is also fired once at initialization
    int androidStepCount = -1;              // number of step detected by android
    int ubiStepCount = 0;                   // number of step detected by UbiStep

    /*
    //Definiton of Orientation Vectors
    private float[] accMagOrientation = new float[3];
    private float[] gravMagnOrientation = new float[3];
    */

    // Gyroscope variables
    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    public static final float EPSILON = 0.000000001f;
    private float gyroscope_ts = 0.0f;
    private float gyroscopeUncalibrated_ts = 0.0f;
    private float prev_ts = 0.0f;

    private String android_id;

    private String udp_ip;
    private int udp_port = 7586;
    private UdpClientSend udpClient;

    private String TAG = "MainActivity";
    //private SntpClient sntpClient;      // parameter for the SNTP protocol
    //private boolean isSntpTimeSet = false;  //parameter to determine if the time is already SET
    //private float[] Q = new float[4]; //déclaration du quaternion

    private Scene scene = new Scene();
    OpenGLRenderer openGLRenderer = new OpenGLRenderer(scene);

    // these IDs only concern the OpenGL part
    int gravityVectorID;
    int magneticVectorID;
    int accXVectorID;
    int accYVectorID;
    int accZVectorID;
    int stepVectorSensorID;
    int stepVectorID;

    // fusion of gyroscope and rotation_vector
    OrientationFusion orientationFuser = new OrientationFusion();
    // fusion of gravity acceleration and gyroscope
    GravAccGyroFusion gravAccGyroFuser = new GravAccGyroFusion();
    // fusion of acceleration and gyroscope
    AccGyroFusion accGyroFuser = new AccGyroFusion();

    // some orientation algos are deprecated because they were not good enough so I stop maintaining them
    private static final String [] orientationType = {
            "Gravity+Acceleration+Gyroscope",
            "Acceleration+Gyroscope",
            "Gyroscope",
            "Gravity+Magnetometer (deprecated)",
            "Gyroscope+Rotation Vector (deprecated)",
            "Android (deprecated)"
    };
    private Spinner spinner;
    private String orientationSelection = orientationType[0];

    //calibration of magnetic field
    CalibrationMagnetometer magnetometer_calibrated = new CalibrationMagnetometer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // -----------------------------------------------------------------------------------------
        //---------INIT-VARIABLES
        // -----------------------------------------------------------------------------------------

        // init len 6 vectors
        for (int i = 0; i < 6; i++) {
            magneticFieldUncalibrated_vector[i] = 0;
            gyroscopeUncalibrated_vector[i] = 0;
        }

        // init len 3 vectors
        for (int i = 0; i < 3; i++) {
            accelerometer_vector[i] = 0;

            magneticField_vector[i] = 0;
            gravity_vector[i] = 0;
            linearAcceleration_vector[i] = 0;
            gyroscope_vector[i] = 0;
        }

        float[] one4f = {1.0f, 1.0f, 1.0f, 1.0f};
        float[] one3f = {1.0f, 1.0f, 1.0f};
        float[] zeros3f = {0.0f, 0.0f, 0.0f};
        gravityVectorID = scene.newVector(zeros3f, one4f);
        magneticVectorID = scene.newVector(zeros3f, one4f);
        accXVectorID = scene.newVector(zeros3f, new float[] {1.0f, 0.1f, 0.1f, 1.0f});
        accYVectorID = scene.newVector(zeros3f, new float[] {0.2f, 1.0f, 0.2f, 1.0f});
        accZVectorID = scene.newVector(zeros3f, new float[] {0.4f, 0.4f, 1.0f, 1.0f});
        stepVectorSensorID = scene.newVector(zeros3f, new float[] {0.8f, 0.8f, 0.0f, 1.0f});
        stepVectorID = scene.newVector(zeros3f, new float[] {1.0f, 1.0f, 1.0f, 1.0f});


        // -----------------------------------------------------------------------------------------
        // -------INIT-COMMUNICATION----------------------------------------------------------------
        // -----------------------------------------------------------------------------------------
        // get android_id
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String defaultValue = getResources().getString(R.string.udp_ip_saved);
        udp_ip = sharedPref.getString(getString(R.string.udp_ip_saved), defaultValue);
        Log.d(TAG, "Saved IP is " + udp_ip);
        udpClient = new UdpClientSend(udp_ip, udp_port);
        //sntpClient = new SntpClient();

        // -----------------------------------------------------------------------------------------
        // -------------INIT-VIEWS------------------------------------------------------------------
        // -----------------------------------------------------------------------------------------
        // init opengl view
        setContentView(R.layout.activity_main);
        surfaceView = (GLSurfaceView) findViewById(R.id.openGLDemoSurfaceView);
        surfaceView.setRenderer(openGLRenderer);

        // Set the text in the top bar (name and version). It should be something like: "U-IMU v2.0"
        final TextView topBarTextView = (TextView) findViewById(R.id.topBarTextView);
        String topBarText = getResources().getString(R.string.app_name) + " v" + getResources().getString(R.string.app_version);
        topBarTextView.setText(topBarText);

        // init text input for target IP address
        final EditText udpIpEntry = (EditText) findViewById(R.id.editText);
        udpIpEntry.setText(udp_ip);

        //init textview
        String defaultValue2 = getResources().getString(R.string.offset_saved);
        String offset = sharedPref.getString(getString(R.string.offset_saved), defaultValue2);
        final TextView currentOffset = (TextView) findViewById(R.id.offset);
        currentOffset.setText(offset);

        //init textview
        String defaultValue3 = getResources().getString(R.string.scale_saved);
        String scale = sharedPref.getString(getString(R.string.scale_saved), defaultValue3);
        final TextView currentScale = (TextView) findViewById(R.id.scale);
        currentScale.setText(scale);

        //init spinner for orientation view type
        spinner = (Spinner)findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener((OnItemSelectedListener) this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,orientationType);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setVisibility(View.VISIBLE);


        // -----------------------------------------------------------------------------------------
        // -----------------------------------REGISTER-LISTENERS------------------------------------
        // -----------------------------------------------------------------------------------------
        //initialize sensor manager
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        int delay = SensorManager.SENSOR_DELAY_GAME; // SENSOR_DELAY_GAME means 0.02 delay -> 50 samples/s
        // acceleration sensors
        // TYPE_ACCELEROMETER_UNCALIBRATED is available since api 26 (Android 8.0 Oreo)
        //if (android.os.Build.VERSION.SDK_INT >= 26) mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED), delay);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), delay);  // TYPE_ACCELEROMETER is already little bit preprocessed.
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), delay);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), delay);

        // Magnetometer
        //mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), delay);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), delay);

        // Gyroscopes
        //mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), delay);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), delay);

        // Orientation
        //mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), delay);

        // Step detector: Please, choose either STEP_DETECOR or STEP_COUNTER. Not both of them at the same time
        //mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), delay);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), delay);
    }



    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.button_restartsocket:
                restart_socket();
                break;
            case R.id.button_calibration:
                calibrationParameter();
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
        // We dont unregister because we want to acquire in background
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        mSensorManager.unregisterListener(mSensorListener);
        udpClient.close();
    }


    public void calibrationParameter() {
        /*
        update offset and scale
        first click: start data acquision
        second click: calculation
         */
        Button btn_calibration = (Button) findViewById(R.id.button_calibration);
        isCalibrating_Magnetometer = !isCalibrating_Magnetometer;
        if (isCalibrating_Magnetometer) btn_calibration.setText("processing");
        else{
            btn_calibration.setText("calibration");
            magnetometer_calibrated.parameterUpdate();
            magnetometer_calibrated.contextUpdate();

            SharedPreferences sharedPref = GetContext.getAppContext().getSharedPreferences("string", Context.MODE_PRIVATE);

            String offset = sharedPref.getString("offset_saved", "offset: 0,0,0");
            final TextView currentOffset = (TextView) findViewById(R.id.offset);
            currentOffset.setText(offset);

            String scale = sharedPref.getString("scale_saved", "scale: 1,1,1");
            final TextView currentScale = (TextView) findViewById(R.id.scale);
            currentScale.setText(scale);

            Log.d(TAG,"offset: "+offset);
         }
    }



    SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            acquire(event);
        }
    };



    public void acquire(SensorEvent event){
        float new_ts = event.timestamp * NS2S;
        max_ts = Math.max(new_ts, max_ts);
        min_ts = Math.min(new_ts, min_ts);

        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                // this sensor is only supported by api level 26 or higher
                copyToVector(accelerometerUncalibrated_vector, event);
                isMeasured_AccelerometerUncalibrated = TRUE;
                break;

            case Sensor.TYPE_ACCELEROMETER:
                copyToVector(accelerometer_vector, event);
                isMeasured_Accelerometer = TRUE;
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                copyToVector(linearAcceleration_vector, event);
                isMeasured_LinearAcceleration = TRUE;
                break;

            case Sensor.TYPE_GRAVITY:
                copyToVector(gravity_vector, event);
                isMeasured_Gravity = TRUE;
                break;


            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                copyToVector(magneticFieldUncalibrated_vector, event);
                isMeasured_MagneticFieldUncalibrated = TRUE;
                if (isCalibrating_Magnetometer) magnetometer_calibrated.updateCalibration(magneticFieldUncalibrated_vector);
                magneticFieldCalibrated_vector = magnetometer_calibrated.magneticField_correction(magneticFieldUncalibrated_vector);
                //Log.d(TAG,"calibrated: "+floatVector2String(magneticFieldCalibrated_vector));
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                copyToVector(magneticField_vector, event);

                isMeasured_MagneticField = TRUE;
                break;


            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                copyToVector(gyroscopeUncalibrated_vector, event);
                // the compute orientation function modifies the quaternion input
                orientationUncalibratedQuaternion = compute_orientation_from_angular_velocity(orientationUncalibratedQuaternion, gyroscopeUncalibrated_vector, new_ts, gyroscopeUncalibrated_ts);
                gyroscopeUncalibrated_ts = new_ts;
                isMeasured_GyroscopeUncalibrated = TRUE;
                break;

            case Sensor.TYPE_GYROSCOPE:
                copyToVector(gyroscope_vector, event);
                // the compute orientation function modifies the quaternion input
                orientationQuaternion = compute_orientation_from_angular_velocity(orientationQuaternion, gyroscope_vector, new_ts, gyroscope_ts);
                gyroscope_ts = new_ts;
                isMeasured_Gyroscope = TRUE;
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                float[] rotation_vector_wxyz = new float[4];
                // the compute orientation function modifies the quaternion input
                SensorManager.getQuaternionFromVector(rotation_vector_wxyz, event.values);

                // Store in quaternion
                rotationVectorQuaternion = new Quaternion(rotation_vector_wxyz[1], rotation_vector_wxyz[2], rotation_vector_wxyz[3], rotation_vector_wxyz[0]);
                isMeasured_RotationVector = TRUE;
                break;

            case Sensor.TYPE_STEP_DETECTOR:
                isAndroidStepDetected = TRUE;
                // With step detector, androidStepCountTriggered and androidStepCount will have the same value
                androidStepCountTriggered += 1;
                androidStepCount += 1;
                update_stepCounterTextView();
                break;

            case Sensor.TYPE_STEP_COUNTER:
                isAndroidStepDetected = TRUE;
                // if it is the very first step detected, we store the initial value of the step counter
                if (androidStepCountTriggered == -1){
                    androidStepCountInitalValue = (int)event.values[0];
                }
                androidStepCountTriggered += 1;
                androidStepCount = (int)event.values[0] - androidStepCountInitalValue;
                update_stepCounterTextView();
                break;
        }

        if (
                //isMeasured_AccelerometerUncalibrated &
                isMeasured_Accelerometer &
                isMeasured_LinearAcceleration &
                isMeasured_Gravity &
                //isMeasured_MagneticFieldUncalibrated &
                isMeasured_MagneticField &
                //isMeasured_GyroscopeUncalibrated &
                isMeasured_Gyroscope //&
                //isMeasured_RotationVector

            ){
            isMeasured_AccelerometerUncalibrated = (android.os.Build.VERSION.SDK_INT < 26);  // remains TRUE if api doesnt support this sensor.
            isMeasured_Accelerometer = FALSE;
            isMeasured_LinearAcceleration = FALSE;
            isMeasured_Gravity = FALSE;

            isMeasured_MagneticFieldUncalibrated = FALSE;
            isMeasured_MagneticField = FALSE;

            isMeasured_GyroscopeUncalibrated = FALSE;
            isMeasured_Gyroscope = FALSE;

            isMeasured_RotationVector = FALSE;


            // matrice de rotation grace au magnetic et gravity
            //float[] orientationGravMagnMatrix = new float[9];
            //SensorManager.getRotationMatrix(orientationGravMagnMatrix, null, gravity_vector, magneticField_vector);
            //orientationGravMagnQuaternion = new Quaternion(orientationGravMagnMatrix);

            // Fusion of ROTATION_VECTOR with GYROSCOPE
            // fusedOrientationQuaternion = orientationFuser.update(rotationVectorQuaternion, gyroscope_vector, gyroscope_ts);

            // Fusion of GRAVITY, ACCELERATION and GYROSCOPE
            fusedGravAccGyroQuaternion = gravAccGyroFuser.update(gravity_vector, accelerometer_vector, gyroscope_vector, gyroscope_ts);

            // Fusion of ACCELERATION and GYROSCOPE
            fusedAccGyroQuaternion = accGyroFuser.update(accelerometer_vector, gyroscope_vector, gyroscope_ts);


            isUbiStepDetected = ubiStepDetector.update(accelerometer_vector, fusedAccGyroQuaternion, gyroscope_ts);
            if (isUbiStepDetected){
                ubiStepCount += 1;
                update_stepCounterTextView();
            }

            //Log.d(TAG, "acquire: true");
            send_values();
            update_opengl_view();

            //Log.d(TAG, "EG: dT: " + (new_ts-curr_ts) + " [s] \trate: " + (1.0f/(new_ts-curr_ts)) + " \n");
            curr_ts = new_ts;
            max_ts = Float.MIN_VALUE;
            min_ts = Float.MAX_VALUE;

            Log.d(TAG, "\tUbiStep Counter:" + ubiStepCount + "\tAndroid Step Counter:" + androidStepCountTriggered + "/" + androidStepCount);
            isUbiStepDetected = FALSE;
            isAndroidStepDetected = FALSE;
        }
    }

    public void restart_socket() {
        //replace the udpclient if target IP has changed
        final EditText udpIpEntry = (EditText) findViewById(R.id.editText);
        String new_udp_ip = udpIpEntry.getText().toString();
        udpClient.close();
        udpClient = new UdpClientSend(new_udp_ip, udp_port);
        Log.d(TAG, "New udp client created with ip " + new_udp_ip);

        if (!udp_ip.equals(new_udp_ip)) {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.udp_ip_saved), new_udp_ip);
            editor.commit();
        }
        udp_ip = new_udp_ip;

    }

    /**
     * Send to value through a socket at destination of the python script running on the computer
     * Please, make sure that you respect the format expected by the python script:
     */
    public void send_values(){
        long now = System.currentTimeMillis();
        //long now = (long) (max_ts * (NS2S/1000.0));
        //Log.d(TAG, "" + now);
        String msg = "{\"timestamp\":\"" + now +
                "\",\"Phone_ID\":\"" + android_id +
                "\",\"accelerometer\":\"" + floatVector2String(accelerometer_vector) +
                "\",\"linearAcceleration\":\"" + floatVector2String(linearAcceleration_vector) +
                "\",\"gravity\":\"" + floatVector2String(gravity_vector) +
                //"\",\"magneticFieldUncalibrated\":\"" + floatVector2String(magneticFieldUncalibrated_vector) +
                "\",\"magneticField\":\"" + floatVector2String(magneticField_vector) +
                //"\",\"magneticFieldCalibrated\":\"" + floatVector2String(magneticFieldCalibrated_vector) +
                //"\",\"magneticFieldOffset\":\"" + floatVector2String(magnetometer_calibrated.getMagneticOffset()) +
                //"\",\"magneticFieldScale\":\"" + floatVector2String(magnetometer_calibrated.getMagneticScale()) +
                //"\",\"gyroscopeUncalibrated\":\"" + floatVector2String(gyroscopeUncalibrated_vector) +
                "\",\"gyroscope\":\"" + floatVector2String(gyroscope_vector) +
                "\",\"orientationQuaterionXYZW\":\"" + floatVector2String(orientationQuaternion.getFloatArrayXYZW()) +
                //"\",\"orientationUncalibratedQuaterionXYZW\":\"" + floatVector2String(orientationUncalibratedQuaternion.getFloatArrayXYZW()) +
                "\",\"orientationGravAccGyroQuaterionXYZW\":\"" + floatVector2String(fusedGravAccGyroQuaternion.getFloatArrayXYZW()) +
                "\",\"orientationAccGyroQuaterionXYZW\":\"" + floatVector2String(fusedAccGyroQuaternion.getFloatArrayXYZW()) +
                "\",\"isStepDetectedSensor\":\"" + ((isAndroidStepDetected) ? 1 :  0) +  // 1 if true, 0 if False
                "\",\"isStepDetected\":\"" + ((isUbiStepDetected) ? 1 :  0) +  // 1 if true, 0 if False
                "\"}";
        // if api level >= 26, so the accelerometerUncalirated is available
        if (android.os.Build.VERSION.SDK_INT >= 26){
            // add the accelerometer uncalibrated vector at the end of the json string
            //msg = msg.substring(0, msg.length() - 1) + ",\"accelerometerUncalibrated\":\"" + floatVector2String(accelerometerUncalibrated_vector) + "\"}";
        }

        try {
            udpClient.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * integrates the ouptut of gyroscope [rad/s] over time to calculate a rotation [rad] describing the change of angles over the time step
     * original code: https://developer.android.com/reference/android/hardware/SensorEvent#values
     */
    public Quaternion compute_orientation_from_angular_velocity(Quaternion orientation0, float[] gyro_vector, float gyro_ts, float gyro_ts_prev) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (gyro_ts_prev == 0) {
            return orientation0.normalized();
        }

        final double dT = (gyro_ts - gyro_ts_prev);
        // Axis of the rotation sample, not normalized yet.
        double axisX = gyro_vector[0];
        double axisY = gyro_vector[1];
        double axisZ = gyro_vector[2];

        // Calculate the angular speed of the sample
        double omegaMagnitude = (double) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

        // Normalize the rotation vector if it's big enough to get the axis
        // (that is, EPSILON should represent your maximum allowable margin of error)
        if (omegaMagnitude > EPSILON) {
            axisX /= omegaMagnitude;
            axisY /= omegaMagnitude;
            axisZ /= omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        double thetaOverTwo = omegaMagnitude * dT / 2.0;
        double sinThetaOverTwo = Math.sin(thetaOverTwo);
        double cosThetaOverTwo = Math.cos(thetaOverTwo);

        Quaternion deltaRotationQuat = new Quaternion(
                sinThetaOverTwo * axisX,
                sinThetaOverTwo * axisY,
                sinThetaOverTwo * axisZ,
                cosThetaOverTwo);
        Quaternion orientation1 = (orientation0.times(deltaRotationQuat)).normalized();
        return orientation1;
    }

    private void copyToVector(float[] vector, SensorEvent event){
        for (int i = 0; i < vector.length; i++) {
            vector[i] = event.values[i];
        }
    }

    public void onItemSelected (AdapterView<?> parent, View view, int position, long id){
        orientationSelection = spinner.getSelectedItem().toString();
    }

    public void onNothingSelected(AdapterView<?> arg0) {

    }

    /**
     * The number of steps are displayed on a label over the opengl_view. This function update the text of the label with the current values of the step counters
     */
    public void update_stepCounterTextView(){
        // Set the text just bellow the top bar
        final TextView stepCounterTextView = (TextView) findViewById(R.id.stepCounterTextView);
        String stepCounterText = "UbiStep: " + ubiStepCount + ", Detector: " + androidStepCountTriggered + ", Counter: " + androidStepCount;
        stepCounterTextView.setText(stepCounterText);
    }

    /**
     * Update the 3D scene. (Visulisation of Cubes, Lines etc...)
     */
    public void update_opengl_view(){

        // Acceleration vectors XYZ
        scene.getVectorList().get(accXVectorID).setVertices(new float[]{0.0f, -2.5f, 0.0f, accelerometer_vector[0], -2.5f, 0.0f});
        scene.getVectorList().get(accYVectorID).setVertices(new float[]{0.0f, -2.75f, 0.0f, accelerometer_vector[1], -2.75f, 0.0f});
        scene.getVectorList().get(accZVectorID).setVertices(new float[]{0.0f, -3.0f, 0.0f, accelerometer_vector[2], -3.0f, 0.0f});

        // White line for our UbiStep Detector
        float stepNorm = 2.0f;
        if (!isUbiStepDetected){
            float[] vertices = scene.getVectorList().get(stepVectorID).getVertices();
            stepNorm = vertices[4] * 0.9f;
        }
        scene.getVectorList().get(stepVectorID).setVertices(new float[]{-2.0f, 0.0f, 0.0f, -2.0f, stepNorm, 0.0f });

        // Yellow line for the Android step counter
        stepNorm = 2.0f;
        if (!(isAndroidStepDetected)){
            float[] vertices = scene.getVectorList().get(stepVectorSensorID).getVertices();
            stepNorm = vertices[4] * 0.9f;
        }
        scene.getVectorList().get(stepVectorSensorID).setVertices(new float[]{-1.7f, 0.0f, 0.0f, -1.7f, stepNorm, 0.0f });


        // Cube orientation using the selected orientation algorithm
        float[] rotationCurrent = new float[16];
        Quaternion q;
        switch (orientationSelection){
            case "Gravity+Acceleration+Gyroscope": // "Gravity+Acceleration+Gyroscope"
                q = fusedGravAccGyroQuaternion.copy();
                SensorManager.getRotationMatrixFromVector(rotationCurrent, q.getFloatArrayXYZW());
                break;
            case "Acceleration+Gyroscope": // "Acceleration+Gyroscope"
                q = fusedAccGyroQuaternion.copy();
                SensorManager.getRotationMatrixFromVector(rotationCurrent, q.getFloatArrayXYZW());
                break;
            case "Gyroscope":  // "Gyroscope"
                // orientation obtained from gyroscope. This one can drift
                q = orientationQuaternion.copy();
                SensorManager.getRotationMatrixFromVector(rotationCurrent, q.getFloatArrayXYZW());
                break;
            case "Gyroscope+Rotation Vector (deprecated)":  // "Gyroscope+Rotation Vector (deprecated)"
                // fusion between gyroscope and rotation_vector. Aim to reduce the drift of gyroscope with
                q = fusedOrientationQuaternion.copy();
                SensorManager.getRotationMatrixFromVector(rotationCurrent, q.getFloatArrayXYZW());
                break;
            case "Gravity+Magnetometer (deprecated)":  // "Gravity+Magnetometer (deprecated)"
                // use gravity and magnetometer to get orientation. This is weak.
                float[] down_vector = gravity_vector;
                float[] north_vector = magneticField_vector;
                SensorManager.getRotationMatrix(rotationCurrent, null, down_vector, north_vector);
                break;
            case "Android (deprecated)":  // "Android (deprecated)"
                // orientation processed by android using acc (gravity), gyroscope, and magnetometer (absolute north)
                // this one is sensitive to magnetic interferences
                q = rotationVectorQuaternion.copy();
                SensorManager.getRotationMatrixFromVector(rotationCurrent, q.getFloatArrayXYZW());
                break;
            default:
                q = rotationVectorQuaternion.copy();
                SensorManager.getRotationMatrixFromVector(rotationCurrent, q.getFloatArrayXYZW());
        }

        //Quaternion q = fusedOrientationQuaternion.copy();
        openGLRenderer.setRotationMatrix_Gyro(rotationCurrent);
    }

    /**
     * convert a float array into its string reprensentation (python like).
     * for example if the input vector is {1.0, 2.0, 3.14}, the returned string will be:
     * "[1.0, 2.0, 3.14]"
     * @param vector
     * @return
     */
    private String floatVector2String(float[] vector){
        String msg = "[" + vector[0];
        for (int i = 1; i < vector.length; i++) {
            msg += ", " + vector[i];
        }
        msg += "]";
        return msg;
    }
}


