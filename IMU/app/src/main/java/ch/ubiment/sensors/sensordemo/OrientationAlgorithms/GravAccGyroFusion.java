package ch.ubiment.sensors.sensordemo.OrientationAlgorithms;

import android.hardware.SensorManager;
import android.util.Log;

import ch.ubiment.sensors.sensordemo.Algebra.Quaternion;

public class GravAccGyroFusion {

    private String TAG = this.toString();
    /**
     * The Quaternions that contain the current rotation (Angle and axis in Quaternion format) of the Gyroscope
     */
    private Quaternion relativeOrientationQuaternion = new Quaternion();

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
    private static final double EPSILON = 0.05f;


    /**
     * The acceleration error (max angle with avg acceleration) is multiplied by the ACCELERATION_ERROR_GAIN.
     * By increasing this gain, the contribution of acceleration is decreased compared to gravity.
     */
    private static final float ACCELERATION_ERROR_GAIN = 5.0f;

    /**
     * This is the number of consecutive frame where the acceleration vector have to be stable in
     * order that the acceleration is considered to be representing the gravity
     * If this size is set to 100 and that the sampling rate is 50Hz, then the acceleration
     * needs to be stable for 2 seconds before beeing taken into account.
     * At 50Hz, I recommend a value between 50 and 100, depending on the application
     */
    private static final int ACCELERATION_BUFFER_SIZE = 64;

    private static final float CORRECTION_DAMPING = 0.01f;



    /**
     * Flag indicating, whether the orientations were initialised from the rotation vector or not. If false, the
     * gyroscope can not be used (since it's only meaningful to calculate differences from an initial state). If
     * true,
     * the gyroscope can be used normally.
     */
    private boolean positionInitialised = false;




    private Quaternion gravity_bias = new Quaternion();
    private float[] acc_avg = new float[3];
    private float[][] acc_buffer = new float[ACCELERATION_BUFFER_SIZE][3];
    private int acc_buffer_index = -1;


    private float safe_acos(float val){
        return (float) safe_acos((double) val);
    }

    private double safe_acos(double val){
        if (Math.abs(val) > 1.01) Log.w(TAG, "EG: Math.acos(" + val + "): Is that what you want?");
        return Math.acos(Math.max(-1.0, Math.min(1.0, val)));
    }

    private float dotProduct(float[] u, float[] v){
        int L = u.length;
        float out = 0;
        for (int i=0; i<L; i++) {
            out += u[i]*v[i];
        }
        return out;
    }

    /**
     * cross product (vectorial product) of two len-3 array
     * @param u
     * @param v
     * @return
     */
    private double[] crossProduct_f2d(float[] u, float[] v){
        double[] out = {
                (double)(u[1]*v[2] - u[2]*v[1]),
                (double)(u[2]*v[0] - u[0]*v[2]),
                (double)(u[0]*v[1] - u[1]*v[0])
        };
        return out;
    }

    private float[] normalized(float[] v){
        int L = v.length;
        float norm = 0;
        for (int k=0; k<L; k++){
            norm += v[k]*v[k];
        }
        norm = (float)Math.sqrt(norm);
        if (norm == 0){
            float[] out = new float[L];
            return out;
        }else{
            float[] out = new float[L];
            for (int k=0; k<L; k++){
                out[k] = v[k]/norm;
            }
            return out;
        }
    }
    private double[] normalized(double[] v){
        int L = v.length;
        double norm = 0;
        for (int k=0; k<L; k++){
            norm += v[k]*v[k];
        }
        norm = Math.sqrt(norm);
        if (norm == 0){
            double[] out = {0,0,0};
            return out;
        }else{
            double[] out = new double[L];
            for (int k=0; k<L; k++){
                out[k] = v[k]/norm;
            }
            return out;
        }
    }


    /**
     *
     * @param angularRate_xyz rate around local x, y, z axis in rad/s (as given by the gyroscope)
     * @param new_angular_ts timestamp in second
     * @return
     */
    public Quaternion update(float[] gravity, float[] acceleration, float[] angularRate_xyz, float new_angular_ts){
        // update absolute orientation

        // TODO: could we replace gravity by relativeOrientation.up_vector() ?
        float[] world_up = normalized(gravity);
        //world_up[0] *= -1;
        //world_up[1] *= -1;
        //world_up[2] *= -1;

        float prev_angular_ts = angular_ts;
        angular_ts = new_angular_ts;

        if (!positionInitialised) {
            // INIT ORIENTATIONS
            // The angle of rotation is acos( d ), where d is the dot product of world_up and {0,0,1} (both normalized).
            double radian = safe_acos((double)world_up[2]); // world_up[2] is the dot product of <world_up, {0,0,1}>
            // the rotation axis is the cross product: ( world_up x {0,0,1} )
            double[] rotation_axis = {world_up[1], -world_up[0], 0};

            // computes the quaternion aligning orientation to world_up
            relativeOrientationQuaternion = (new Quaternion(radian, rotation_axis)).normalized();
            absoluteOrientationQuaternion = relativeOrientationQuaternion.copy();

            positionInitialised = true;

            // initialise the acceleration array (except the very first entry which is already set)
            for (int i=0; i<acc_buffer.length; i++){
                acc_buffer[i][0] = gravity[0];
                acc_buffer[i][1] = gravity[1];
                acc_buffer[i][2] = gravity[2];
            }
            acc_avg[0] = gravity[0];
            acc_avg[1] = gravity[1];
            acc_avg[2] = gravity[2];
            return relativeOrientationQuaternion.copy();
        }

        acc_buffer_index = (acc_buffer_index + 1) % acc_buffer.length;
        // UPDATE AVERAGE OF ACCELERATION BUFFER
        // acc_avg is the average acceleration of the acceleration buffer acc_buffer
        // We want to compute the new average knowing that only the acceleration at index acc_buffer_index changes.
        // So the new acc_avg is:
        //      acc_avg - acc_buffer[acc_buffer_index]/ACCELERATION_BUFFER_SIZE  + acceleration/ACCELERATION_BUFFER_SIZE
        // which can be simplified by:
        //      acc_avg + (acceleration - acc_buffer[acc_buffer_index])/ACCELERATION_BUFFER_SIZE
        acc_avg[0] += (acceleration[0] - acc_buffer[acc_buffer_index][0])/ACCELERATION_BUFFER_SIZE;
        acc_avg[1] += (acceleration[1] - acc_buffer[acc_buffer_index][1])/ACCELERATION_BUFFER_SIZE;
        acc_avg[2] += (acceleration[2] - acc_buffer[acc_buffer_index][2])/ACCELERATION_BUFFER_SIZE;
        // UPDATE CIRCULAR BUFFER
        acc_buffer[acc_buffer_index][0] = acceleration[0];
        acc_buffer[acc_buffer_index][1] = acceleration[1];
        acc_buffer[acc_buffer_index][2] = acceleration[2];

        // compute bias
        float t = compute_update_weight(); // determine t ( 0<t<1 & t inversely proporsional to acceleration variations )
        float[] vecA = normalized(gravity);
        float[] vecB = normalized(acc_avg);
        float dot = dotProduct(vecA, vecB);
        double alpha = safe_acos((double)dot);  // angle between gravity and acc_avg
        double[] axis = normalized(crossProduct_f2d(vecB, vecA));  // TODO: this axis could be chosen non-horizontally to correct the heading error?
        Quaternion new_gravity_bias = new Quaternion(alpha, axis);

        // update gravity bias.
        // if t tends to 0:
        //      then gravity_bias tends to gravity_bias
        // if t tends 1:
        //      then gravity_bias tends to new_gravity_bias.
        // it means that when we move, t is small and gravity_bias stay unchanged.
        // when we are stable, t is big and gravity_bias is updated (corrected)
        gravity_bias = gravity_bias.slerp(new_gravity_bias, t);


        // update relative orientation
        relativeOrientationQuaternion = compute_orientation_from_angular_velocity(relativeOrientationQuaternion, angularRate_xyz, angular_ts, prev_angular_ts);
        double[] Rd = relativeOrientationQuaternion.getRotationMatrix();
        //pureRelativeOrientationQuaternion = relativeOrientationQuaternion.copy();

        // We rotate the gravity vector a little bit. The bias is the difference between the gravity and the mean acceleration

        world_up = normalized(gravity_bias.rotateVector(world_up));

        //System.out.println("grav-fused-acc \n(" + vecA[0] + ", " + vecA[1] + ", " + vecA[2] + ") \n(" + world_up[0] + ", " + world_up[1] + ", " + world_up[2] + ") \n(" + vecB[0] + ", " + vecB[1] + ", " + vecB[2] + ")\n........." );

        float[] north_vector = {(float)Rd[3], (float)Rd[4], (float)Rd[5]};
        float[] Rf = new float[9];
        SensorManager.getRotationMatrix(Rf, null, world_up, north_vector);
        absoluteOrientationQuaternion = new Quaternion(Rf);

        relativeOrientationQuaternion = relativeOrientationQuaternion.slerp(absoluteOrientationQuaternion, CORRECTION_DAMPING).normalized();
        return relativeOrientationQuaternion.copy();

    }

    private float compute_update_weight(){
        float lower_dot = 1;
        float dot;
        float[] acc_avg_normalized = normalized(acc_avg);
        System.out.println(TAG + ": acc_avg_normalized: " + acc_avg_normalized[0] + ", " + acc_avg_normalized[1] + ", " + acc_avg_normalized[2]);
        for (int i=0; i<ACCELERATION_BUFFER_SIZE; i++) {
            float[] acc_normalized = {acc_buffer[i][0], acc_buffer[i][1], acc_buffer[i][2]};
            acc_normalized = normalized(acc_normalized);
            dot = dotProduct(acc_avg_normalized, acc_normalized);
            //Log.d(TAG, "\t\tdot: " + dot);
            if (lower_dot > dot){
                lower_dot = dot;
            }
        }
        // get degree difference
        float max_deviation = (float) (safe_acos(lower_dot) * 180.0/Math.PI);
        float t = 1.0f - max_deviation * ACCELERATION_ERROR_GAIN;
        // t must be in [0, 1]
        t = Math.max(0.0f, t);
        //Log.d(TAG, "max_deviation: " + max_deviation + "\tt: " + t);
        return t;
    }


    /**
     * integrates the ouptut of gyroscope [rad/s] over time to calculate a rotation [rad] describing the change of angles over the time step
     * original code: https://developer.android.com/reference/android/hardware/SensorEvent#values
     */
    private Quaternion compute_orientation_from_angular_velocity(Quaternion orientation0, float[] angularVelocity_xyz, float angular_ts, float angular_ts_prev) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (angular_ts_prev == 0) {
            return orientation0.normalized();
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


}
