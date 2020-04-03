import os.path

import socket
import json
from json import loads as json_loads
from time import time
import struct

import numpy as np

from base_acquisition import BaseAcquisition
from io_operations import fill_from_file


class BeaconAcquisition(BaseAcquisition):
    def __init__(self, dataprocessor=None, datawriter=None, databuffer=None,
                 anchors_file=None, name="Beacon", verbose=False):
        """
        :param dataprocessor:
        :param datawriter:
        :param databuffer:
        :param anchors_file:
        :param name: "Beacon" or "Nordic"
        :param verbose:
        """
        BaseAcquisition.__init__(self, name, dataprocessor, datawriter, databuffer)
        
        self.verbose = verbose

        self.anchor_ids = []
        if anchors_file is not None:
            fill_from_file(anchors_file, self.system_id, anchors_ids=self.anchor_ids)

    def explore(self, signatureID, explorationTimeSec=10):
        print("Beacon explore()")

        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.bind((self.UDP_IP, self.UDP_PORT))
            t0 = time()
            while time()-t0 < explorationTimeSec:
                json_str, addr = sock.recvfrom(1024)  # buffer size is 1024 bytes

                json_obj = json_loads(json_str.decode())
                major = json_obj['Major']
                minor = json_obj['Minor']
                rssi = json_obj['RSSI']

                beacon_id = "-".join([major, minor])

    def run(self):
        BaseAcquisition.run(self)

    def process_data(self, json_obj):
        timestamp = self.current_milli_time()

        # parse json input
        position_ts = int(json_obj['PositionTS'])
        address = json_obj['Address']
        uuid = json_obj['UUID']
        major = int(json_obj['Major'])
        minor = int(json_obj['Minor'])
        tx_power = int(json_obj['TxPower'])
        rssi = int(json_obj['RSSI'])
        device_id = int(json_obj['Phone_ID'], 16)

        # Nordic Boards have their minor id stored in their UUID
        if major == 85 and minor == 77:
            # the minor for nordic is the MAC address converted to integer after removing the ':'
            minor = int(uuid[22:22 + 17].replace(':', ''), 16)

        # filter the desired beacon by their IDs
        beacon_id = "-".join([str(major), str(minor)])
        if len(self.anchor_ids) > 0 and beacon_id not in self.anchor_ids:
            if self.verbose:
                print("{} not in {}".format(beacon_id, self.anchor_ids))
            return None

        output = {
            "timestamp": timestamp,
            "device_id": device_id,
            "anchor_id": beacon_id,
            "txpower": tx_power,
            "rssi": rssi,
            "system_id": self.system_id,
        }
        return output

if __name__ == '__main__':
    from data_writer import DataWriter
    from io_operations import prepare_output
    from base_acquisition import BaseAcquisitionParser

    parser = BaseAcquisitionParser()
    verbose = not parser.quiet()

    source = os.path.dirname(__file__)
    output_dir = os.path.join(source, 'output/')
    outfile = prepare_output(output_dir, 'data_bluetooth.csv')

    datawriter = DataWriter(outfile, verbose=verbose)

    thread_beacon = BeaconAcquisition(name="Beacon", datawriter=datawriter, verbose=verbose)

    try:
        thread_beacon.run()
    except (KeyboardInterrupt, SystemExit):
        datawriter.onDestroy()
        print("\nAcquisition stopped.")
