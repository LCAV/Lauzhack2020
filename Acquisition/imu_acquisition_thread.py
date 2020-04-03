from threading import Lock
import math3d
import time
import math
import numpy as np

from base_acquisition import BaseAcquisition
import ubiment_parameters as UBI
from data_writer import DataWriter


# TODO consider moving to a static method in base_acquisition.
def update_deviceID_to_dt(deviceID_to_dt, timestamp, device_id, verbose=False):
    """
    Convert phone time to server time.

    Using recent phones timestamps, it estimates (and keeps track) the time difference between phone and server.
    If the data arrives late, the timestamps can be corrected with no problem.
    Jitters are corrected continuously.
    However the round trip time is assumed to be 0.

    :param deviceID_to_dt: dictionary of all calibrated dts.
    :param timestamp: millisecond timestamp provided with the data
    :param device_id: id of the device which sent the data
    :return: the corrected timestamp in epoch time milliseconds according to this computer
    """
    # compute time difference for this particular data.
    now = int(1000 * time.time())
    current_dt = now - timestamp

    if device_id not in deviceID_to_dt.keys():
        # this happens only for the first received data from a new device
        deviceID_to_dt[device_id] = current_dt
        if verbose:
            print("Update dt from {} to {} for device id {}".format(
                math.floor(deviceID_to_dt[device_id] - 1), current_dt, device_id))

    # check if dt needs to be updated
    if math.floor(deviceID_to_dt[device_id]) > current_dt:
        if verbose:
            print("Update dt from {} to {} for device id {}".format(
                math.floor(deviceID_to_dt[device_id] - 1), current_dt, device_id))
        deviceID_to_dt[device_id] = current_dt
    out = timestamp + math.floor(deviceID_to_dt[device_id])
    # we introduce a fake jitter to favorize the recent data.
    # It allows to correct the jitter of the phone (in case phone time is slower than server time).
    deviceID_to_dt[device_id] += 1 / 50 / 30  # todo, adapt 1/50 in function of the rate
    return out


class IMUAcquisition(BaseAcquisition):
    """
    This is the script to acquire the IMU data from the phone (android).
    The phone must run the version 5.x of U-IMU application ./Embedded/IMU_Sensors/U-IMU_Sensors 
    """

    def __init__(self, datawriter=None, datawriter_raw=None, dataprocessor=None, databuffer=None,
                 visualise=False, reference_orientation=None):
        """
        Warning: make sure that datawriter and datawriter_raw point to distinct output files

        :param datawriter: writes the data in a csv file as according to UBI.data_fields
        :param datawriter_raw: writes the data in a csv file as according to UBI.imu_raw_fields
        :param dataprocessor:
        :param databuffer:
        :param visualise: True to enable plotting with the script ./visualization/mpl_animation_rotation.py
        :param reference_orientation: an instance of PixliveAcquisition class. Only used if visualise is True
        """
        BaseAcquisition.__init__(self, 'Sensors', datawriter=datawriter, dataprocessor=dataprocessor,
                                 databuffer=databuffer)

        # a second datawriter is used to save raw imu fields beside the Ubiment common fields
        self.datawriter_raw = datawriter_raw

        # this dictionary is used for time synchronization. It contains the time difference between each devices
        # and this server
        self.deviceID_to_dt = {}

        # for imu data plotting
        self.visualise = visualise
        if self.visualise:
            self.pixlive_ref = reference_orientation
            self.dt = None
            self.Rot_world2sensors = math3d.thetas_to_Rmat([0, 0, 0], homogenous=False)  # initialize to identity matrix
            self.lock = Lock()
            self.xs = [0]
            self.ys = [0]
            self.zs = [0]
            self.ts = [0]
            self.theta_x = [0]
            self.theta_y = [0]
            self.theta_z = [0]

    def get_xyzt(self, i=0):
        # function called in ./visualization/mpl_animation_rotation.py
        with self.lock:
            out = (self.xs[i], self.ys[i], self.zs[i], self.ts[i])
        return out

    def update_visualisation_data(self, dico):
        """
        Update (xyz), theta_(xyz) and ts for visualisation scripts
        :param dico: dictionary with acquired Ubiment fields from UBI.data_fields
        """
        acc = [0, 0, 0]
        theta_x, theta_y, theta_z = dico["theta_x"], dico["theta_y"], dico["theta_z"]

        if self.pixlive_ref is not None:
            # this reference to pixlive is a handy way to test the transformation from world to local orientation.
            local_theta_xyz = [theta_x, theta_y, theta_z]
            local_acc_xyz = [dico["acc_x"], dico["acc_y"], dico["acc_z"]]

            Rs = math3d.thetas_to_Rmat(local_theta_xyz, homogenous=False)
            # if reference have been updated recently
            if dico['timestamp'] - self.pixlive_ref.ts[0] < 200:
                # this is where we update the Rot_world2sensors matrix
                ref_world_theta_xyz = [self.pixlive_ref.theta_x[0], self.pixlive_ref.theta_y[0], self.pixlive_ref.theta_z[0]]

                # Rotation matrix Rp (Pixlive) and Rs (imu sensors)
                Rp = math3d.thetas_to_Rmat(ref_world_theta_xyz, homogenous=False)
                self.Rot_world2sensors = Rp * Rs.I

            # Rw = oldRp * oldRs.I * Rs
            Rw = self.Rot_world2sensors * Rs

            # convert local to world datas
            imu_world_thetas_xyz = math3d.Rmat_to_thetas(Rw)

            imu_world_acc_xyz = Rw * np.matrix(local_acc_xyz).T
            imu_world_acc_xyz = imu_world_acc_xyz.T.tolist()[0]  # convert 3x1 matrix to list

            theta_x, theta_y, theta_z = imu_world_thetas_xyz

            toprint = "ORIENTATION world:( {:.2f}, {:.2f}, {:.2f}),\t\t local:( {:.2f}, {:.2f}, {:.2f})".format(
                theta_x, theta_y, theta_z,
                local_theta_xyz[0], local_theta_xyz[1], local_theta_xyz[2])
            print(toprint.replace(' -', '-'))

            acc = [
                imu_world_acc_xyz[0],
                imu_world_acc_xyz[1],
                imu_world_acc_xyz[2]
            ]

        # these dirty arrays are used by a visualization script such as visualization/mpl_animation_rotation.py.
        with self.lock:
            now = int(time.time() * 1000)
            self.ts = [now] + [t for t in self.ts if now - t < 1000]
            N = len(self.ts) - 1
            self.xs = [acc[0]] + self.xs[:N]
            self.ys = [acc[1]] + self.ys[:N]
            self.zs = [acc[2]] + self.zs[:N]

            self.theta_x = [theta_x] + self.theta_x[:N]
            self.theta_y = [theta_y] + self.theta_y[:N]
            self.theta_z = [theta_z] + self.theta_z[:N]

    def time_sync(self, timestamp, device_id):
        return update_deviceID_to_dt(self.deviceID_to_dt, timestamp, device_id, self.verbose)

    def process_data(self, json_obj):
        # TODO: add a calibration perdiod
        device_id = int(json_obj['Phone_ID'], 16)
        timestamp = int(float(json_obj['timestamp']))
        timestamp = self.time_sync(timestamp, device_id)

        # save raw imu data in a separate file
        if self.datawriter_raw is not None:
            dico_raw = self.imu_raw_extraction(json_obj)
            dico_raw["timestamp"] = timestamp
            dico_raw["device_id"] = device_id
            dico_raw["system_id"] = self.system_id
            towrite_raw = UBI.imu_raw_dict2list(dico_raw)
            self.datawriter_raw.writeline(towrite_raw)

        # build a dictionary with IMU values for Ubiment
        dico = self.imu_extraction(json_obj)
        dico["timestamp"] = timestamp
        dico["device_id"] = device_id
        dico["system_id"] = self.system_id

        if self.visualise:
            self.update_visualisation_data(dico)

        return dico

    @staticmethod
    def imu_extraction(json_obj):
        # Quaternion rotation vector
        orientation_acc_gyro_quaterion_xzy = list(map(float, eval(json_obj['orientationAccGyroQuaterionXYZW'])))
        qx = orientation_acc_gyro_quaterion_xzy[0]
        qy = orientation_acc_gyro_quaterion_xzy[1]
        qz = orientation_acc_gyro_quaterion_xzy[2]
        qw = orientation_acc_gyro_quaterion_xzy[3]

        # we use tait-bryan angles
        theta_x, theta_y, theta_z = math3d.quaternion_to_thetas(qx, qy, qz, qw)

        # Acceleration
        acc = list(map(float, eval(json_obj['accelerometer'])))

        # Step
        android_step_detected = eval(json_obj['isStepDetectedSensor'])
        ubiment_step_detected = eval(json_obj['isStepDetected'])

        dico = {
            "theta_x": theta_x,
            "theta_y": theta_y,
            "theta_z": theta_z,
            "acc_x": acc[0],
            "acc_y": acc[1],
            "acc_z": acc[2],
            "is_step_detected": android_step_detected or ubiment_step_detected
        }
        return dico

    @staticmethod
    def imu_raw_extraction(json_obj):
        """
        This function extract everything from the json object sent by U-IMU app version 5.x, 
        and write them in a dictionary as according to UBI.imu_raw_fields
        
        However the 3 fields "timestamp", "system_id" and "device_id" 
        will be missing in the returned dictionary and must be added manually afterward
        :param json_obj: dictionary received from U-IMU app v5.x
        :return: raw imu dictionary 
        """

        accelerometer = list(map(float, eval(json_obj['accelerometer'])))
        linear_acceleration = list(map(float, eval(json_obj['linearAcceleration'])))
        gravity = list(map(float, eval(json_obj['gravity'])))

        magnetic_field = list(map(float, eval(json_obj['magneticField'])))

        gyroscope = list(map(float, eval(json_obj['gyroscope'])))

        orientation_quaterion_xyzw = list(map(float, eval(json_obj['orientationQuaterionXYZW'])))
        orientation_gravaccgyro_quaterion_xyzw = list(map(float, eval(json_obj['orientationGravAccGyroQuaterionXYZW'])))
        orientation_accgyro_quaterion_xyzw = list(map(float, eval(json_obj['orientationAccGyroQuaterionXYZW'])))

        isStepDetectedSensor = eval(json_obj['isStepDetectedSensor'])  # isAndroidStepDetected
        isStepDetected = eval(json_obj['isStepDetected'])  # isUbiStepDetected

        dico = {
            "accelerometer_x": accelerometer[0],
            "accelerometer_y": accelerometer[1],
            "accelerometer_z": accelerometer[2],
            "linear_acceleration_x": linear_acceleration[0],
            "linear_acceleration_y": linear_acceleration[1],
            "linear_acceleration_z": linear_acceleration[2],
            "gravity_x": gravity[0],
            "gravity_y": gravity[1],
            "gravity_z": gravity[2],

            "magnetic_field_x": magnetic_field[0],
            "magnetic_field_y": magnetic_field[1],
            "magnetic_field_z": magnetic_field[2],

            "gyroscope_x": gyroscope[0],
            "gyroscope_y": gyroscope[1],
            "gyroscope_z": gyroscope[2],

            "orientation_qx": orientation_quaterion_xyzw[0],
            "orientation_qy": orientation_quaterion_xyzw[1],
            "orientation_qz": orientation_quaterion_xyzw[2],
            "orientation_qw": orientation_quaterion_xyzw[3],
            "orientation_gravaccgyro_qx": orientation_gravaccgyro_quaterion_xyzw[0],
            "orientation_gravaccgyro_qy": orientation_gravaccgyro_quaterion_xyzw[1],
            "orientation_gravaccgyro_qz": orientation_gravaccgyro_quaterion_xyzw[2],
            "orientation_gravaccgyro_qw": orientation_gravaccgyro_quaterion_xyzw[3],
            "orientation_accgyro_qx": orientation_accgyro_quaterion_xyzw[0],
            "orientation_accgyro_qy": orientation_accgyro_quaterion_xyzw[1],
            "orientation_accgyro_qz": orientation_accgyro_quaterion_xyzw[2],
            "orientation_accgyro_qw": orientation_accgyro_quaterion_xyzw[3],

            "is_step_detected_sensor": isStepDetectedSensor,
            "is_step_detected": isStepDetected,
        }
        return dico


if __name__ == '__main__':
    from io_operations import prepare_output
    from os import path
    from base_acquisition import BaseAcquisitionParser

    parser = BaseAcquisitionParser()
    verbose = not parser.quiet()

    source = path.dirname(__file__)
    output_dir = path.join(source, 'output')

    outfile = prepare_output(output_dir, 'data_imu.csv')
    datawriter = DataWriter(outfile, verbose=verbose)

    outfile = prepare_output(output_dir, 'data_imu_raw.csv')
    datawriter_raw = DataWriter(outfile, header=UBI.imu_raw_fields, verbose=verbose)

    thrd_imu = IMUAcquisition(datawriter=datawriter, datawriter_raw=datawriter_raw)

    try:
        thrd_imu.start()
        thrd_imu.join()
    except (KeyboardInterrupt, SystemExit):
        datawriter.onDestroy()
        datawriter_raw.onDestroy()
        print("\nAcquisition stopped.")
