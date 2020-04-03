package ch.ubiment.sensors.sensordemo.Algebra;

public class Matrix3 {
    String TAG = this.toString();
    double[] M;

    public Matrix3(double[] M){
        this.M = M;
    }

    public Matrix3(Quaternion q){
        M = q.getRotationMatrix();
    }

    public Matrix3 multiply(Matrix3 MIn){
        double[] MOut = new double[9];
        int n = 0;
        for (int i = 0; i<3; i++){
            for (int j = 0; j<3; j++){
                double val = 0;
                for (int k = 0; k<3; k++){
                    int n_ik = 3*i + k;
                    int n_kj = 3*k + j;
                    val += M[n_ik]*MIn.getMk(n_kj);
                }
                MOut[n] = val;
                n++;
            }
        }
        return new Matrix3(MOut);
    }

    public Matrix3 transposed(){
        double[] T = new double[9];
        int k = 0;
        for (int i = 0; i<3; i++) {
            for (int j = 0; j < 3; j++) {
                T[k] = getMij(j, i);
                k++;
            }
        }
        return new Matrix3(T);
    }


    /**
     * multiply Matrix3 with float[] vector of length 3. (return a double[3])
     * @param vec3f
     * @return
     */
    public float[] multiply(float[] vec3f){
        double[] vec3 = {(double) vec3f[0], (double) vec3f[1], (double) vec3f[2]};
        double[] outd = multiply(vec3);
        float[] out = {(float) outd[0], (float) outd[1], (float) outd[2]};
        return out;
    }


    /**
     * multiply Matrix3 with double[] vector of length 3. (return a double[3])
     * @param vec3
     * @return
     */
    public double[] multiply(double[] vec3){
        double[] out = new double[3];
        for (int i = 0; i<3; i++){
            double val = 0;
            //String msg = " /// ";
            for (int j = 0; j<3; j++){
                //double a = getMij(i,j);
                //double b = vec3[i];
                //msg += a+"*"+b+" + ";
                val += getMij(i,j) * vec3[j];
            }
            //Log.d(TAG, msg + "... = "+ val);
            out[i] = val;
        }
        return out;
    }

    public double getMk(int k){
        return M[k];
    }

    public double getMij(int i, int j){
        return M[3*i+j];
    }

}
