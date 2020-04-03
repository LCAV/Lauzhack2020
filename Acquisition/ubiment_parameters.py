# Socket port and baudrate are also used as systemID
BEACON_PORT = 7582
SENSORS_PORT = 7586

system_list = {
    'Beacon':{
        'id': BEACON_PORT,
        'port': BEACON_PORT,
        'tags': [444, 14954135790684542069]
    },
    'Sensors':{
        'id': SENSORS_PORT,
        'port': SENSORS_PORT,
        'tags': [0]
    },
}

data_fields = [
    "timestamp",        # time in milliseconds in epoch time
    # the device we want to locate (previously named tag_id)
    "device_id",
    # we use the UDP port as system id (for now at least, but we must change it to locate several device at a time)
    "system_id",
    # anchor used to take the current measure (previously named locator_id)
    "anchor_id",
    # [theta_x, theta_y, theta_z] corresponds to the orientation of the device (radian)
    "theta_x",
    "theta_y",
    "theta_z",
    "txpower",          # transmitting power of the anchor
    "rssi",             # signal strength received by the device
    # [acc_x, acc_y, acc_z] corresponds to the local acceleration vector of the device
    "acc_x",
    "acc_y",
    "acc_z",
    "is_step_detected"  # step detected by imu
]

# These are the index of the values when we write them
data_timestamp = data_fields.index("timestamp")
data_device_id = data_fields.index("device_id")
data_system_id = data_fields.index("system_id")
data_anchor_id = data_fields.index("anchor_id")
data_txpower = data_fields.index("txpower")
data_rssi = data_fields.index("rssi")
data_theta_x = data_fields.index("theta_x")
data_theta_y = data_fields.index("theta_y")
data_theta_z = data_fields.index("theta_z")
data_acc_x = data_fields.index("acc_x")
data_acc_y = data_fields.index("acc_y")
data_acc_z = data_fields.index("acc_z")


def data_dict2list(dic):
    return [dic[f] if f in list(dic.keys()) else 'NaN' for f in data_fields]


def data_list2dict(array):
    dic = {}
    for k in range(len(array)):
        val = array[k]
        if val is not None and not val == 'NaN':
            field = data_fields[k]
            dic[field] = val
    return dic


imu_raw_fields = [
    "timestamp",
    "device_id",
    "system_id",
    "accelerometer_x",
    "accelerometer_y",
    "accelerometer_z",
    "magnetic_field_x",
    "magnetic_field_y",
    "magnetic_field_z",
    "gravity_x",
    "gravity_y",
    "gravity_z",
    "linear_acceleration_x",
    "linear_acceleration_y",
    "linear_acceleration_z",
    "gyroscope_x",
    "gyroscope_y",
    "gyroscope_z",
    "orientation_qx",
    "orientation_qy",
    "orientation_qz",
    "orientation_qw",
    "orientation_gravaccgyro_qx",
    "orientation_gravaccgyro_qy",
    "orientation_gravaccgyro_qz",
    "orientation_gravaccgyro_qw",
    "orientation_accgyro_qx",
    "orientation_accgyro_qy",
    "orientation_accgyro_qz",
    "orientation_accgyro_qw",
    "is_step_detected_sensor",
    "is_step_detected"
]


def imu_raw_dict2list(dic):
    return [dic[f] if f in list(dic.keys()) else 'NaN' for f in imu_raw_fields]
