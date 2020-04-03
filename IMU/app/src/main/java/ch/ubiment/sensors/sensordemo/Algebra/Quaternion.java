package ch.ubiment.sensors.sensordemo.Algebra;

import android.util.Log;

/******************************************************************************
 *  Compilation:  javac Quaternion.java
 *  Execution:    java Quaternion
 *
 *  Data type for quaternions.
 *
 *  http://mathworld.wolfram.com/Quaternion.html
 *
 *  The data type is "immutable" so once you create and initialize
 *  a Quaternion, you cannot change it.
 *
 *  % java Quaternion
 *
 ******************************************************************************/

public class Quaternion {
    private double qx, qy, qz, qw;

    /**
     * Create quaternion with q components
     * @param qx
     * @param qy
     * @param qz
     * @param qw
     */
    public Quaternion(double qx, double qy, double qz, double qw) {
        this.qx = qx;
        this.qy = qy;
        this.qz = qz;
        this.qw = qw;
    }


    /**
     *
     * @param radian
     * @param rotation_axis
     */
    public Quaternion(double radian, double[] rotation_axis) {
        //normalize axis
        double norm = Math.sqrt(
                rotation_axis[0]*rotation_axis[0] +
                rotation_axis[1]*rotation_axis[1] +
                rotation_axis[2]*rotation_axis[2]);
        if (norm == 0.0){
            // if the norm == 0, then there is no rotation. So we force quaternion to (0,0,0,1)
            norm = 1.0;
            radian = 0.0;
        }

        double cosHalfAlpha = Math.cos(radian/2.0);
        double sinHalfAlpha = Math.sin(radian/2.0);
        qx = sinHalfAlpha*rotation_axis[0]/norm;
        qy = sinHalfAlpha*rotation_axis[1]/norm;
        qz = sinHalfAlpha*rotation_axis[2]/norm;
        qw = cosHalfAlpha;

    }

    /**
     * Class constructor from Rotation matrix under the form of a row-major len-9 float array
     * * <pre>
     *   /  M[ 0]   M[ 1]   M[ 2]  \
     *   |  M[ 3]   M[ 4]   M[ 5]  |
     *   \  M[ 6]   M[ 7]   M[ 8]  /
     *</pre>
     *
     * @param Rmat: Rotation Matrix (Row-Major len-9 float array)
     */
    public Quaternion(float[] Rmat) {
        int m00 = 0;
        int m01 = 1;
        int m02 = 2;
        int m10 = 3;
        int m11 = 4;
        int m12 = 5;
        int m20 = 6;
        int m21 = 7;
        int m22 = 8;

        float tr = Rmat[m00] + Rmat[m11] + Rmat[m22];
        if (tr > 0) {
            double s = Math.sqrt(tr + 1.0) * 2; // S=4*qw
            qw = 0.25f * s;
            qx = (Rmat[m21] - Rmat[m12]) / s;
            qy = (Rmat[m02] - Rmat[m20]) / s;
            qz = (Rmat[m10] - Rmat[m01]) / s;
        } else if ((Rmat[m00] > Rmat[m11]) & (Rmat[m00] > Rmat[m22])) {
            double s = Math.sqrt(1.0 + Rmat[m00] - Rmat[m11] - Rmat[m22]) * 2; // S=4*qx
            qw = (Rmat[m21] - Rmat[m12]) / s;
            qx = 0.25f * s;
            qy = (Rmat[m01] + Rmat[m10]) / s;
            qz = (Rmat[m02] + Rmat[m20]) / s;
        } else if (Rmat[m11] > Rmat[m22]) {
            double s = Math.sqrt(1.0 + Rmat[m11] - Rmat[m00] - Rmat[m22]) * 2; // S=4*qy
            qw = (Rmat[m02] - Rmat[m20]) / s;
            qx = (Rmat[m01] + Rmat[m10]) / s;
            qy = 0.25f * s;
            qz = (Rmat[m12] + Rmat[m21]) / s;
        } else {
            double s = Math.sqrt(1.0 + Rmat[m22] - Rmat[m00] - Rmat[m11]) * 2; // S=4*qz
            qw = (Rmat[m10] - Rmat[m01]) / s;
            qx = (Rmat[m02] + Rmat[m20]) / s;
            qy = (Rmat[m12] + Rmat[m21]) / s;
            qz = 0.25f * s;
        }

        double n = norm();
        qx /= n;
        qy /= n;
        qz /= n;
        qw /= n;

    }

    public Quaternion() {
        qx = 0.0;
        qy = 0.0;
        qz = 0.0;
        qw = 1.0;
    }



    // return a string representation of the invoking object
    public String toString() {
        return qw + " + " + qx + "i + " + qy + "j + " + qz + "k";
    }

    // return the quaternion norm
    public double norm() {
        return Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
    }

    // EG: return the normalized quaternion
    public Quaternion normalized(){
        double norm = this.norm();
        return new Quaternion(qx/norm, qy/norm, qz/norm, qw/norm);
    }

    // return the quaternion conjugate
    public Quaternion conjugate() {
        return new Quaternion(-qx, -qy, -qz, qw);
    }

    // return a new Quaternion whose value is (this + b)
    public Quaternion plus(Quaternion b) {
        Quaternion a = this;
        return new Quaternion(a.qx +b.qx, a.qy +b.qy, a.qz +b.qz, a.qw +b.qw);
    }

    // return a new Quaternion whose value is (this * b)
    public Quaternion times(Quaternion b) {
        Quaternion a = this;
        double rx = a.qw *b.qx + a.qx *b.qw + a.qy *b.qz - a.qz *b.qy;
        double ry = a.qw *b.qy - a.qx *b.qz + a.qy *b.qw + a.qz *b.qx;
        double rz = a.qw *b.qz + a.qx *b.qy - a.qy *b.qx + a.qz *b.qw;
        double rw = a.qw *b.qw - a.qx *b.qx - a.qy *b.qy - a.qz *b.qz;
        return new Quaternion(rx, ry, rz, rw);
    }

    // return a new Quaternion whose value is the inverse of this
    public Quaternion inverse() {
        double d = qw * qw + qx * qx + qy * qy + qz * qz;
        return new Quaternion(-qx /d, -qy /d, -qz /d, qw /d);
    }


    // return a / b
    // we use the definition a * b^-1 (as opposed to b^-1 a)
    public Quaternion divides(Quaternion b) {
        Quaternion a = this;
        return a.times(b.inverse());
    }


    /**
     * Not tested yet. Dont trust this function
     * @param b
     * @return
     */
    public Quaternion alignHeadingTo(Quaternion b){
        Quaternion a = this.normalized();
        b = b.normalized();
        // find quaternion from this quaternion to heading
        Quaternion a2b = a.divides(b);

        // find rotation Z componnent in convert
        //double[] R = {
        //        1 - 2*qy*qy - 2*qz*qz,	    2*qx*qy - 2*qz*qw,  	2*qx*qz + 2*qy*qw,
        //            2*qx*qy + 2*qz*qw,	1 - 2*qx*qx - 2*qz*qz, 	    2*qy*qz - 2*qx*qw,
        //            2*qx*qz - 2*qy*qw,	    2*qy*qz + 2*qx*qw, 	1 - 2*qx*qx - 2*qy*qy
        //};
        // TODO, need singularity test
        // th_z = math.atan2(R[1, 0], R[0, 0])
        double R10 = 2*a2b.qx*a2b.qy + 2*a2b.qz*a2b.qw;
        double R00 = 1 - 2*a2b.qy*a2b.qy - 2*a2b.qz*a2b.qz;
        double theta_z = -Math.atan2(R10, R00);
        double[] up = {0,0,1};
        Quaternion headingCorrection = new Quaternion(theta_z, up);

        Quaternion out = headingCorrection.times(a);
        return out;
    }


    /**
     * Rotate a vector of float {x, y, z} using this Quaternion
     * @param vec3
     * @return
     */
    public float[] rotateVector(float[] vec3){
        Quaternion q = this;
        // To rotate a vector [x,y,z] with a quaternion, we need to convert it into a "pure" quaternion (x,y,z,0)
        // create the "pure" quaternion
        Quaternion v = new Quaternion(vec3[0], vec3[1], vec3[2], 0.0);
        // compute the rotated vector: p = q*vq
        Quaternion p = q.conjugate().times(v).times(q);
        // extract the actual vector from "pure" vector
        float[] out = {(float)p.qx, (float)p.qy, (float)p.qz};
        return out;
    }

    /**
     * EG:
     * returns an array of types 'float' in the order {qx, qy, qz, qw}
     * @return
     */
    public float[] getFloatArrayXYZW(){
        float[] out = {(float) qx, (float) qy, (float) qz, (float) qw};
        return out;
    }

    /**
     * EG:
     * returns an array of types 'double' in the order {qx, qy, qz, qw}
     * @return
     */
    public double[] getDoubleArrayXYZW(){
        double[] out = {qx, qy, qz, qw};
        return out;
    }

    public double[] getRotationMatrix(){
        double R00 = 1 - 2*qy*qy - 2*qz*qz;
        double R01 = 2*qx*qy - 2*qz*qw;
        double R02 = 2*qx*qz + 2*qy*qw;

        double R10 = 2*qx*qy + 2*qz*qw;
        double R11 = 1 - 2*qx*qx - 2*qz*qz;
        double R12 = 2*qy*qz - 2*qx*qw;

        double R20 = 2*qx*qz - 2*qy*qw;
        double R21 = 2*qy*qz + 2*qx*qw;
        double R22 = 1 - 2*qx*qx - 2*qy*qy;

        double[] R = {
                R00, R01, R02,
                R10, R11, R12,
                R20, R21, R22
        };
        return R;
    }

    public double[] getUpVector(){
        double R20 = 2*qx*qz - 2*qy*qw;
        double R21 = 2*qy*qz + 2*qx*qw;
        double R22 = 1 - 2*qx*qx - 2*qy*qy;
        double norm = Math.sqrt(R20*R20 + R21*R21 + R22*R22);
        double[] out = {R20/norm, R21/norm, R22/norm};
        return out;
    }

    /**
     * Returns the unit world up vector in float
     * @return
     */
    public float[] getUpVectorFloat(){
        double[] upVector = getUpVector();
        float[] out = {(float)upVector[0], (float)upVector[1], (float)upVector[2]};
        return out;
    }

    public double[] getNorthVector(){
        double R10 = 2*qx*qy + 2*qz*qw;
        double R11 = 1 - 2*qx*qx - 2*qz*qz;
        double R12 = 2*qy*qz - 2*qx*qw;
        double norm = Math.sqrt(R10*R10 + R11*R11 + R12*R12);
        double[] out = {R10/norm, R11/norm, R12/norm};
        return out;
    }

    public float[] getNorthVectorFloat(){
        double[] northVector = getNorthVector();
        float[] out = {(float)northVector[0], (float)northVector[1], (float)northVector[2]};
        return out;
    }


    /**
     * EG:
     * Returns a copy of this Quaternion
     * @return copied Quaternion
     */
    public Quaternion copy(){
        return new Quaternion(qx, qy, qz, qw);
    }


    public double dotProduct(Quaternion b){
        Quaternion a = this;
        return a.qx*b.qx + a.qy*b.qy + a.qz*b.qz + a.qw*b.qw;
    }


    /**
     * Get a linear interpolation between this quaternion and the input quaternion, storing the result in the output
     * quaternion.
     *
     * @param b The quaternion to be slerped with this quaternion.
     * @param t The ratio between the two quaternions where 0 <= t <= 1.0 . Increase value of t will bring rotation
     *            closer to the input quaternion.
     * @return
     */
    public Quaternion slerp(Quaternion b, float t) {
        // creates copies of the 2 quaternions so that they can't be modified
        Quaternion a = this.normalized();  // the normalized function returns a normalized COPY
        b = b.normalized();

        // Calculate angle between them.
        double cosHalftheta = this.dotProduct(b);

        // There exists 2 paths between a couple of quaternion, a long one and a short one. We want the short one.
        // If cos(theta/2) is smaller than 0, then we negate bufferQuat so that the the short one is used.
        if (cosHalftheta < 0) {
            cosHalftheta *= -1;
            b.qx *= -1;
            b.qy *= -1;
            b.qz *= -1;
            b.qw *= -1;
        }

        // if qa=qb or qa=-qb then theta = 0 and we can return qa
        if (Math.abs(cosHalftheta) >= 1.0) {
            return a;
        } else {


            double sinHalfTheta = Math.sqrt(1.0 - cosHalftheta * cosHalftheta);
            double halfTheta = Math.acos(cosHalftheta);

            double ratioA = Math.sin((1 - t) * halfTheta) / sinHalfTheta;
            double ratioB = Math.sin(t * halfTheta) / sinHalfTheta;

            //Calculate Quaternion
            Quaternion out = new Quaternion();
            out.qx = a.qx * ratioA + b.qx * ratioB;
            out.qy = a.qy * ratioA + b.qy * ratioB;
            out.qz = a.qz * ratioA + b.qz * ratioB;
            out.qw = a.qw * ratioA + b.qw * ratioB;
            return out.normalized();
        }
    }



    public Quaternion slerp_step(Quaternion b, float step) {
        // creates copies of the 2 quaternions so that they can't be modified
        Quaternion a = this.normalized();  // the normalized function returns a normalized COPY
        b = b.normalized();

        // Calculate angle between them.
        double cosHalftheta = this.dotProduct(b);
        double halfTheta = Math.acos(cosHalftheta);
        float t = (float)Math.min(step / halfTheta, 1.0f);
        return a.slerp(b, t);
    }


    /*
    // sample client for testing
    public static void main(String[] args) {
        Quaternion a = new Quaternion(3.0, 1.0, 0.0, 0.0);
        StdOut.println("a = " + a);

        Quaternion b = new Quaternion(0.0, 5.0, 1.0, -2.0);
        StdOut.println("b = " + b);

        StdOut.println("norm(a)  = " + a.norm());
        StdOut.println("conj(a)  = " + a.conjugate());
        StdOut.println("a + b    = " + a.plus(b));
        StdOut.println("a * b    = " + a.times(b));
        StdOut.println("b * a    = " + b.times(a));
        StdOut.println("a / b    = " + a.divides(b));
        StdOut.println("a^-1     = " + a.inverse());
        StdOut.println("a^-1 * a = " + a.inverse().times(a));
        StdOut.println("a * a^-1 = " + a.times(a.inverse()));
    }
    */

    public static void test(){
        Quaternion q1 = new Quaternion(1,2,3,4);
        Quaternion q2 = new Quaternion(2,0,3,1);

        Quaternion a = q2.conjugate().times(q1.conjugate());
        Quaternion b = (q1.times(q2)).conjugate();
        Log.d("Quaternion test:", a.toString() + ", " + b.toString());
    }

}
