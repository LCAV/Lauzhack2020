package ch.ubiment.sensors.sensordemo;


import ch.ubiment.sensors.sensordemo.Algebra.Quaternion;
import ch.ubiment.sensors.sensordemo.Algebra.VectorOperator;

/**
 * Ubiment Step Detector:
 * Uses orientation, accelerometer and timestamps to detect steps.
 */
public class UbiStepDetector {
    String TAG = this.toString();

    private int BUFFER_SIZE;

    // change this threshold according to your sensitivity preferences
    private float STEP_THRESHOLD = 0.08f;  // lower step threshold means higher sensitivity (more step detecteded, but more false-positive)
    private float STEP_DELAY_SECONDS = 0.35f;  // minimum time delay between two consecutive steps

    private float accelerometerDelay;
    private int prevBufferIndex;
    private int currentBufferIndex;
    private int nextBufferIndex;
    private float[] verticalAccelerationBuffer;
    private float[] verticalVelocityBuffer;
    private float lastStepTimeSeconds = 0f;
    private float lastTimeSeconds = 0f;

    private float gravityEstimate = 9.81f;  // initial estimate

    private float verticalPosition = 0.0f;
    private float verticalVelocity = 0.0f;


    /**
     * Class constructor
     * @param accelerometerDelay: interval between to consecutive measures. For example 0.02 sec corresponds to 50 measures/sec
     */
    public UbiStepDetector(float accelerometerDelay){
        this.accelerometerDelay = accelerometerDelay;
        this.BUFFER_SIZE = (int) (0.5 + STEP_DELAY_SECONDS/(accelerometerDelay));
        this.prevBufferIndex = BUFFER_SIZE;
        this.currentBufferIndex = 0;
        this.nextBufferIndex = 1;

        this.verticalAccelerationBuffer = new float[BUFFER_SIZE];
        this.verticalVelocityBuffer = new float[BUFFER_SIZE];

    }

    /**
     * This must be called as soon as a new acceleration and orientation is available.
     * @param currentAccel: xyz local acceleration in m/s^2
     * @param orientation: orientation of the device. The z axis must be aligned with gravity
     * @param timeSeconds: timestamp of the measured acceleration and orientation
     * @return true if a step is detected
     */
    public boolean update(float[] currentAccel, Quaternion orientation, float timeSeconds) {
        // We use the provided orientation to get the vertical axis
        float[] worldZ = orientation.getUpVectorFloat();

        // Using dot product, we extract the tha acceleration along world Z axis
        float dotZ = VectorOperator.dot(worldZ, currentAccel);
        // The acceleration sensors is biased, using 9.81 as gravity would be wrong. So we estimate the norm of gravity thanks to
        gravityEstimate = gravityEstimate*0.9f + 0.1f*dotZ;
        float currentZ = VectorOperator.dot(worldZ, currentAccel) - gravityEstimate;

        if (lastTimeSeconds == 0f){
            // this only occurs at initialisation
            lastTimeSeconds = timeSeconds;
            return false;
        }
        verticalAccelerationBuffer[currentBufferIndex] = currentZ * this.accelerometerDelay;
        float velocityEstimate = VectorOperator.sum(verticalAccelerationBuffer);
        verticalVelocityBuffer[currentBufferIndex] = velocityEstimate;
        float[] minMax = VectorOperator.minMax(verticalVelocityBuffer);
        float minVelocity = minMax[0];
        float maxVelocity = minMax[1];

        //Log.d(TAG, "grav: " + gravityEstimate + "\tvelest: " + velocityEstimate + "\tvel: " + verticalVelocity + "\tpos: " + verticalPosition);
        //Log.d(TAG, "minVelocity: " + minVelocity + "\tmaxVelocity: " + maxVelocity + "\tvel: " + velocityEstimate);
        boolean isStepDetected = false;
        if (    velocityEstimate > STEP_THRESHOLD &&
                //oldVelocityEstimate <= STEP_THRESHOLD &&
                (timeSeconds - lastStepTimeSeconds > STEP_DELAY_SECONDS) &&
                maxVelocity == verticalVelocityBuffer[prevBufferIndex] &&
                minVelocity < -STEP_THRESHOLD) {
            isStepDetected = true;
            lastStepTimeSeconds = timeSeconds;
        }

        prevBufferIndex = currentBufferIndex;
        currentBufferIndex = nextBufferIndex;
        nextBufferIndex = (nextBufferIndex +1) % BUFFER_SIZE;

        lastTimeSeconds = timeSeconds;
        return isStepDetected;
    }


    public float getVerticalVelocity(){
        return verticalVelocity;
    }

    public float getVerticalPosition() {
        return verticalPosition;
    }

}