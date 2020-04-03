package ch.ubiment.sensors.sensordemo.Algebra;

public class VectorOperator {
    public static float sum(float[] array) {
        float retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i];
        }
        return retval;
    }

    public static float[] minMax(float[] array) {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            float val = array[i];
            min = (min > val) ? val : min;
            max = (max < val) ? val : max;
        }
        return new float[] {min, max};
    }

    public static double[] float2double(float[] array){
        int L = array.length;
        double[] out = new double[L];
        for (int i=0; i<L; i++){
            out[i] = (double) array[i];
        }
        return out;
    }

    public static float[] cross(float[] arrayA, float[] arrayB) {
        float[] retArray = new float[3];
        retArray[0] = arrayA[1] * arrayB[2] - arrayA[2] * arrayB[1];
        retArray[1] = arrayA[2] * arrayB[0] - arrayA[0] * arrayB[2];
        retArray[2] = arrayA[0] * arrayB[1] - arrayA[1] * arrayB[0];
        return retArray;
    }
    public static double[] cross(double[] arrayA, double[] arrayB) {
        double[] retArray = new double[3];
        retArray[0] = arrayA[1] * arrayB[2] - arrayA[2] * arrayB[1];
        retArray[1] = arrayA[2] * arrayB[0] - arrayA[0] * arrayB[2];
        retArray[2] = arrayA[0] * arrayB[1] - arrayA[1] * arrayB[0];
        return retArray;
    }


    public static float norm(float[] array) {
        float retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i] * array[i];
        }
        return (float) Math.sqrt(retval);
    }
    public static double norm(double[] array) {
        double retval = 0;
        for (int i = 0; i < array.length; i++) {
            retval += array[i] * array[i];
        }
        return (double) Math.sqrt(retval);
    }



    public static float dot(float[] a, float[] b) {
        float retval = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return retval;
    }
    public static double dot(double[] a, double[] b) {
        double retval = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return retval;
    }



    public static float[] normalize(float[] a) {
        float[] retval = new float[a.length];
        float norm = norm(a);
        for (int i = 0; i < a.length; i++) {
            retval[i] = a[i] / norm;
        }
        return retval;
    }
    public static double[] normalize(double[] a) {
        double[] retval = new double[a.length];
        double norm = norm(a);
        for (int i = 0; i < a.length; i++) {
            retval[i] = a[i] / norm;
        }
        return retval;
    }
}
