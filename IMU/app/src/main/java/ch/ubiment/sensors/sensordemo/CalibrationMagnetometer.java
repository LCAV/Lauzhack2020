package ch.ubiment.sensors.sensordemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.ArrayList;


/**
 * Magnetometer Calibration created by Chloe
 */

public class CalibrationMagnetometer {
    private String TAG = "CalibrationMagnetometer";
    // magnetic calibration parameter
    private float[] magneticOffset;
    private float[] magneticScale;
    private float avgDelta;
    private float[] avg_Delta;
    private ArrayList<float[]> magneticField_matrix;

    float magnetometer_max_X = -100;
    float magnetometer_min_X = 100;
    float magnetometer_max_Y = -100;
    float magnetometer_min_Y = 100;
    float magnetometer_max_Z = -100;
    float magnetometer_min_Z = 100;

    private float magnetometer_offset_x;
    private float magnetometer_offset_y;
    private float magnetometer_offset_z;
    private float magnetometer_scale_x;
    private float magnetometer_scale_y;
    private float magnetometer_scale_z;

    public CalibrationMagnetometer(){
        SharedPreferences sharedPref2 = GetContext.getAppContext().getSharedPreferences("dimens", Context.MODE_PRIVATE);
        magnetometer_offset_x = sharedPref2.getFloat("offset_x_saved", 0);
        magnetometer_offset_y = sharedPref2.getFloat("offset_y_saved", 0);
        magnetometer_offset_z = sharedPref2.getFloat("offset_z_saved", 0);
        magnetometer_scale_x = sharedPref2.getFloat("scale_x_saved", 1);
        magnetometer_scale_y = sharedPref2.getFloat("scale_y_saved", 1);
        magnetometer_scale_z = sharedPref2.getFloat("scale_z_saved", 1);

        avgDelta = 0;
        avg_Delta = new float[] {0,0,0};
        magneticField_matrix = new ArrayList();

        magneticOffset = new float[]{magnetometer_offset_x, magnetometer_offset_y, magnetometer_offset_z};
        magneticScale = new float[]{magnetometer_scale_x, magnetometer_scale_y, magnetometer_scale_z};
    }

    public void parameterUpdate(){
        magnetometer_offset_x = (magnetometer_max_X + magnetometer_min_X) / 2;
        magnetometer_offset_y = (magnetometer_max_Y + magnetometer_min_Y) / 2;
        magnetometer_offset_z = (magnetometer_max_Z + magnetometer_min_Z) / 2;
        avg_Delta = new float[]{(magnetometer_max_X - magnetometer_min_X) / 2, (magnetometer_max_Y - magnetometer_min_Y) / 2, (magnetometer_max_Z - magnetometer_min_Z) / 2};
        avgDelta = (avg_Delta[0] + avg_Delta[1] + avg_Delta[2]) / 3;
        magnetometer_scale_x = avgDelta / avg_Delta[0];
        magnetometer_scale_y = avgDelta / avg_Delta[1];
        magnetometer_scale_z = avgDelta / avg_Delta[2];

        magneticOffset = new float[]{magnetometer_offset_x, magnetometer_offset_y, magnetometer_offset_z};
        magneticScale = new float[]{magnetometer_scale_x, magnetometer_scale_y, magnetometer_scale_z};

        magnetometer_max_X = -100;
        magnetometer_min_X = 100;
        magnetometer_max_Y = -100;
        magnetometer_min_Y = 100;
        magnetometer_max_Z = -100;
        magnetometer_min_Z = 100;
    }

    public void contextUpdate(){
        SharedPreferences sharedPref = GetContext.getAppContext().getSharedPreferences("dimens", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("offset_x_saved", magnetometer_offset_x);
        editor.putFloat("offset_y_saved", magnetometer_offset_y);
        editor.putFloat("offset_z_saved", magnetometer_offset_z);
        editor.putFloat("scale_x_saved", magnetometer_scale_x);
        editor.putFloat("scale_y_saved", magnetometer_scale_y);
        editor.putFloat("scale_z_saved", magnetometer_scale_z);
        editor.commit();

        SharedPreferences sharedPref2 = GetContext.getAppContext().getSharedPreferences("string", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor2 = sharedPref2.edit();
        editor2.putString("offset_saved", "offset: " + floatVector2String(magneticOffset));
        editor2.putString("scale_saved", "scale: " + floatVector2String(magneticScale));
        editor2.commit();

        Log.d(TAG,"offset: "+sharedPref2.getString("offset_saved","0,0,0"));
    }

    private String floatVector2String(float[] vector){
        String msg = "[" + vector[0];
        for (int i = 1; i < vector.length; i++) {
            msg += ", " + vector[i];
        }
        msg += "]";
        return msg;
    }

    public float[] magneticField_correction(float[] magneticFieldUncalibrated_vector){
        float[] magneticFieldCalibrated_vector = new float[]{
                (magneticFieldUncalibrated_vector[0] - magneticOffset[0]) * magneticScale[0],
                (magneticFieldUncalibrated_vector[1] - magneticOffset[1]) * magneticScale[1],
                (magneticFieldUncalibrated_vector[2] - magneticOffset[2]) * magneticScale[2]};
        return magneticFieldCalibrated_vector;
    }


    public void updateCalibration(float[] magneticFieldUncalibrated_vector){
        if (magneticFieldUncalibrated_vector[0] > magnetometer_max_X) magnetometer_max_X = magneticFieldUncalibrated_vector[0];
        if (magneticFieldUncalibrated_vector[0] < magnetometer_min_X) magnetometer_min_X = magneticFieldUncalibrated_vector[0];

        if (magneticFieldUncalibrated_vector[1] > magnetometer_max_Y) magnetometer_max_Y = magneticFieldUncalibrated_vector[1];
        if (magneticFieldUncalibrated_vector[1] < magnetometer_min_Y) magnetometer_min_Y = magneticFieldUncalibrated_vector[1];

        if (magneticFieldUncalibrated_vector[2] > magnetometer_max_Z) magnetometer_max_Z = magneticFieldUncalibrated_vector[2];
        if (magneticFieldUncalibrated_vector[2] < magnetometer_min_Z) magnetometer_min_Z = magneticFieldUncalibrated_vector[2];
    }

    public float[] getMagneticOffset(){
        return  magneticOffset;
    }

    public float[] getMagneticScale(){
        return magneticScale;
    }
}
