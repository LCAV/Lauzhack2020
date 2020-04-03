package ch.ubiment.sensors.sensordemo.OrientationAlgorithms;

import android.util.Log;

import ch.ubiment.sensors.sensordemo.Algebra.Quaternion;

public class OrientationFusion {

    private String TAG = this.toString();
    /**
     * The Quaternions that contain the current rotation (Angle and axis in Quaternion format) of the Gyroscope
     */
    private Quaternion relativeOrientationQuaternion = new Quaternion();

    private Quaternion pureRelativeOrientationQuaternion = new Quaternion();

    /**
     * The quaternion that contains the absolute orientation as obtained by the rotationVector sensor.
     */
    private Quaternion absoluteOrientationQuaternion = new Quaternion();

    /**
     * The time-stamp being used to record the time when the last gyroscope event occurred.
     */
    private float angular_ts;

    /**
     * This is a filter-threshold for discarding Gyroscope measurements that are below a certain level and
     * potentially are only noise and not real motion. Values from the gyroscope are usually between 0 (stop) and
     * 10 (rapid rotation), so 0.1 seems to be a reasonable threshold to filter noise (usually smaller than 0.1) and
     * real motion (usually > 0.1). Note that there is a chance of missing real motion, if the use is turning the
     * device really slowly, so this value has to find a balance between accepting noise (threshold = 0) and missing
     * slow user-action (threshold > 0.5). 0.1 seems to work fine for most applications.
     *
     */
    private static final double EPSILON = 0.1f;

    /**
     * Value giving the total velocity of the gyroscope (will be high, when the device is moving fast and low when
     * the device is standing still). This is usually a value between 0 and 10 for normal motion. Heavy shaking can
     * increase it to about 25. Keep in mind, that these values are time-depended, so changing the sampling rate of
     * the sensor will affect this value!
     */
    private double angularVelocity = 0;

    /**
     * Flag indicating, whether the orientations were initialised from the rotation vector or not. If false, the
     * gyroscope can not be used (since it's only meaningful to calculate differences from an initial state). If
     * true,
     * the gyroscope can be used normally.
     */
    private boolean positionInitialised = false;

    /**
     * Counter that sums the number of consecutive frames, where the rotationVector and the gyroscope were
     * significantly different (and the dot-product was smaller than 0.7). This event can either happen when the
     * angles of the rotation vector explode (e.g. during fast tilting) or when the device was shaken heavily and
     * the gyroscope is now completely off.
     */
    private int panicCounter = 0;

    /**
     * This weight determines indirectly how much the rotation sensor will be used to correct. This weight will be
     * multiplied by the velocity to obtain the actual weight. (in sensor-fusion-scenario 2 -
     * SensorSelection.GyroscopeAndRotationVector2).
     * Must be a value between 0 and approx. 0.04 (because, if multiplied with a velocity of up to 25, should be still
     * less than 1, otherwise the SLERP will not correctly interpolate). Should be close to zero.
     */
    private static final float INDIRECT_INTERPOLATION_WEIGHT = 0.01f;

    /**
     * The threshold that indicates an outlier of the rotation vector. If the dot-product between the two vectors
     * (gyroscope orientation and rotationVector orientation) falls below this threshold (ideally it should be 1,
     * if they are exactly the same) the system falls back to the gyroscope values only and just ignores the
     * rotation vector.
     *
     * This value should be quite high (> 0.7) to filter even the slightest discrepancies that causes jumps when
     * tiling the device. Possible values are between 0 and 1, where a value close to 1 means that even a very small
     * difference between the two sensors will be treated as outlier, whereas a value close to zero means that the
     * almost any discrepancy between the two sensors is tolerated.
     */
    private static final float OUTLIER_THRESHOLD = 0.85f;

    /**
     * The threshold that indicates a massive discrepancy between the rotation vector and the gyroscope orientation.
     * If the dot-product between the two vectors
     * (gyroscope orientation and rotationVector orientation) falls below this threshold (ideally it should be 1, if
     * they are exactly the same), the system will start increasing the panic counter (that probably indicates a
     * gyroscope failure).
     *
     * This value should be lower than OUTLIER_THRESHOLD (0.5 - 0.7) to only start increasing the panic counter,
     * when there is a huge discrepancy between the two fused sensors.
     */
    private static final float OUTLIER_PANIC_THRESHOLD = 0.75f;

    /**
     * The threshold that indicates that a chaos state has been established rather than just a temporary peak in the
     * rotation vector (caused by exploding angled during fast tilting).
     *
     * If the chaosCounter is bigger than this threshold, the current position will be reset to whatever the
     * rotation vector indicates.
     */
    private static final int PANIC_THRESHOLD = 60;


    ///**
    // * Return the quaternion having the same vertical axis as input quaternion,
    // * and minimizing the distance with the local quaternion (qx=0, qy=0, qz=0, qw=1).
    // * @param flatQuaternion
    // * @return
    // */
    //private Quaternion localFlatQuaternion(Quaternion flatQuaternion){
    //    float[] local_up = {0.0f, 0.0f, 1.0f};
    //    float[] world_up = flatQuaternion.rotateVector(local_up);
    //
    //    // The angle of rotation is acos( d ), where d is the dot product of local_up and world_up (both normalized).
    //    // As local_up={0,0,1}, then d=world_up[2]
    //    double radian = Math.acos(world_up[2]);
    //    // the rotation axis is the vectorial product of local_up with world_up
    //    double[] rotation_axis = {(double)-world_up[1], (double)world_up[0], 0.0};
    //    return new Quaternion(radian, rotation_axis);
    //}

    /**
     *
     * @param absoluteOrientationQuaternion
     * @param angularRate_xyz rate around local x, y, z axis in rad/s (as given by the gyroscope)
     * @param new_angular_ts timestamp in second
     * @return
     */
    public Quaternion update(Quaternion absoluteOrientationQuaternion, float[] angularRate_xyz, float new_angular_ts){
        // update absolute orientation

        if (true){
            this.absoluteOrientationQuaternion = absoluteOrientationQuaternion.normalized();
            if (!positionInitialised) {
                relativeOrientationQuaternion = this.absoluteOrientationQuaternion.copy();
                pureRelativeOrientationQuaternion = this.absoluteOrientationQuaternion.copy();
                angular_ts = new_angular_ts;

                positionInitialised = true;
                return relativeOrientationQuaternion.copy();
            }


            // update relative orientation
            float prev_angular_ts = angular_ts;
            angular_ts = new_angular_ts;
            relativeOrientationQuaternion = this.compute_orientation_from_angular_velocity(relativeOrientationQuaternion, angularRate_xyz, angular_ts, prev_angular_ts);
            pureRelativeOrientationQuaternion = compute_orientation_from_angular_velocity(pureRelativeOrientationQuaternion, angularRate_xyz, angular_ts, prev_angular_ts);

            // estimate fusion
            Quaternion fusedOrientationQuaternion = estimateFusion();
            relativeOrientationQuaternion = fusedOrientationQuaternion.normalized();

            return relativeOrientationQuaternion.copy();



        } else {
            if (!positionInitialised) {
                // TODO that?
                this.absoluteOrientationQuaternion = absoluteOrientationQuaternion.normalized().alignHeadingTo(new Quaternion());
                relativeOrientationQuaternion = this.absoluteOrientationQuaternion.copy();
                pureRelativeOrientationQuaternion = relativeOrientationQuaternion.copy();

                angular_ts = new_angular_ts;
                positionInitialised = true;
                return relativeOrientationQuaternion.copy();
            }

            // update relative orientation
            float prev_angular_ts = angular_ts;
            angular_ts = new_angular_ts;
            relativeOrientationQuaternion = this.compute_orientation_from_angular_velocity(relativeOrientationQuaternion, angularRate_xyz, angular_ts, prev_angular_ts);
            pureRelativeOrientationQuaternion = compute_orientation_from_angular_velocity(pureRelativeOrientationQuaternion, angularRate_xyz, angular_ts, prev_angular_ts);

            // TODO here?
            this.absoluteOrientationQuaternion = absoluteOrientationQuaternion.normalized().alignHeadingTo(relativeOrientationQuaternion);
            // estimate fusion
            Quaternion fusedOrientationQuaternion = estimateFusion();
            relativeOrientationQuaternion = fusedOrientationQuaternion.normalized();

            return relativeOrientationQuaternion.copy();

        }


    }


    /**
     * integrates the ouptut of gyroscope [rad/s] over time to calculate a rotation [rad] describing the change of angles over the time step
     * original code: https://developer.android.com/reference/android/hardware/SensorEvent#values
     */
    private Quaternion compute_orientation_from_angular_velocity(Quaternion orientation0, float[] angularVelocity_xyz, float angular_ts, float angular_ts_prev) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (angular_ts_prev == 0) {
            return orientation0;
        }

        final double dT = (angular_ts - angular_ts_prev);
        // Axis of the rotation sample, not normalized yet.
        double axisX = angularVelocity_xyz[0];
        double axisY = angularVelocity_xyz[1];
        double axisZ = angularVelocity_xyz[2];

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


    /**
     * EG: Function taken and adapted from sensor-fusion-demo-master ImprovedOrientationSensor2Provider
     *
     * @return fused quaternion
     */
    private Quaternion estimateFusion(){
        Quaternion output;

        // Calculate dot-product to calculate whether the two orientation sensors have diverged
        // (if the dot-product is closer to 0 than to 1), because it should be close to 1 if both are the same.
        double dotProd = relativeOrientationQuaternion.dotProduct(absoluteOrientationQuaternion);
        // If they have diverged, rely on gyroscope only (this happens on some devices when the rotation vector "jumps").
        if (Math.abs(dotProd) < OUTLIER_THRESHOLD) {
            // Increase panic counter
            if (Math.abs(dotProd) < OUTLIER_PANIC_THRESHOLD) {
                panicCounter++;
            }

            output = relativeOrientationQuaternion.copy();

        } else {
            // Both are nearly saying the same. Perform normal fusion.

            // Interpolate with a fixed weight between the two absolute quaternions obtained from gyro and rotation vector sensors
            // The weight should be quite low, so the rotation vector corrects the gyro only slowly, and the output keeps responsive.
            output = relativeOrientationQuaternion.slerp(absoluteOrientationQuaternion, (float) (INDIRECT_INTERPOLATION_WEIGHT * angularVelocity));

            // Reset the panic counter because both sensors are saying the same again
            panicCounter = 0;

        }

        if (panicCounter > PANIC_THRESHOLD) {
            Log.d(TAG, "Panic counter is bigger than threshold; this indicates a Gyroscope failure. Panic reset is imminent.");

            if (angularVelocity < 3) {
                Log.d(TAG, "Performing Panic-reset. Resetting orientation to rotation-vector value.");
                panicCounter = 0;

                // Manually set position to whatever rotation vector says.
                output = absoluteOrientationQuaternion.copy();

            } else {
                Log.d(TAG, String.format( "Panic reset delayed due to ongoing motion (user is still shaking the device). Gyroscope Velocity: %.2f > 3", angularVelocity));
            }
        }

        return output;
    }

    public Quaternion getAbsoluteOrientationQuaternion() {
        return absoluteOrientationQuaternion.copy();
    }

    public Quaternion getPureRelativeOrientationQuaternion() {
        return pureRelativeOrientationQuaternion.copy();
    }

    public Quaternion getRelativeOrientationQuaternion() {
        return relativeOrientationQuaternion.copy();
    }
}
