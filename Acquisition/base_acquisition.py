#! /usr/bin/env python3
# -*- coding: utf-8 -*-
from abc import ABC, abstractmethod
from json import loads as json_loads
import socket
import time

from argparse import ArgumentParser
from stoppable_thread import StoppableThread
from socket_operations import SocketOperations
import ubiment_parameters as UBI

"""
base_acquisition.py: Base class for all acquisition threads. 
"""


# TODO in StoppableThread there is hardly anything.
# We could move those functionalities to this base class.

# TODO the only method we use of data-processor, writer, databuffer and fingerprinter; is the one which registers new data. If we called that method the same in all classes (for example "register_new_data") it would simplify the implementation quite a bit, we could for example have a list of objects and call "register_new_data" for each.  
class BaseAcquisition(StoppableThread, ABC):
    def __init__(self, name, dataprocessor=None, datawriter=None, databuffer=None, fingerprinter=None):
        StoppableThread.__init__(self)
        print("Init", name)
        self.name = name
        self.verbose = False

        # The mobilephone sends some UDP packet through a socket at on a certain port and IP address.
        self.UDP_IP = SocketOperations.get_udp_ip()
        try:
            self.UDP_PORT = UBI.system_list[name]["port"]
        except KeyError:
            KeyError("Add {} and key 'port' to the ubiment_parameter.py file.".format(name))

        try:
            self.system_id = UBI.system_list[name]["id"]
        except KeyError:
            KeyError("Add {} and key 'id' to the ubiment_parameter.py file.".format(name))

        try:
            if len(UBI.system_list[name]["tags"]) > 1:
                print("Warning: Only 0-length parameters for UBI.system_list[]['tags'] implemented. found multiple tags for {}.".format(self.name))

            self.device_id = UBI.system_list[name]["tags"][0]
        except KeyError:
            KeyError("Add {} and key 'tags' to the ubiment_parameter.py file.".format(name))

        self._running = False
        self.setDaemon(True)

        self.datawriter = datawriter
        self.dataprocessor = dataprocessor
        self.databuffer = databuffer
        self.fingerprinter = fingerprinter
        
    def is_running(self):
        return self._running

    def stop(self):
        print(self.name, "stopped.")
        self._running = False

    def run(self):
        print(self.name, "run()")
        self._running = True
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.bind((self.UDP_IP, self.UDP_PORT))

            while self._running:
                json_str, addr = sock.recvfrom(1024)  # buffer size is 1024 bytes
                json_obj = json_loads(json_str.decode())

                # This abstract method process_data, is different 
                # for each acquisition system.  
                dico = self.process_data(json_obj)
                if dico is None:
                    continue

                if self.verbose:
                    print("{}: new data from \n{}".format(self.name, dico["anchor_id"]))

                towrite = self.dict2list(dico)

                if self.datawriter is not None:
                    self.datawriter.writeline(towrite)

                if self.dataprocessor is not None:
                    self.dataprocessor.register_new_data(towrite)

                if self.databuffer is not None:
                    self.databuffer.update_data(dico)

                if self.fingerprinter is not None:
                    self.fingerprinter.read_stream(dico)

    def dict2list(self, dico):
        # This function was added because IMUAcquisitionRaw uses a different method of conversion.
        # We thus overwrite this method in the IMUAcquisitionRaw class.
        # TODO could also choose the corrrect method based on contents of dico. 
        return UBI.data_dict2list(dico)

    @staticmethod
    def current_milli_time():
        """
        :return: current time in milliseconds
        """
        return int(round(time.time() * 1000))

    # IMPORTANT: this is not an abstract function anymore because some inheriting instances 
    # might have their own "run" function, which does not need the method process_data. 
    def process_data(self, json_obj):
        raise NotImplementedError("call to BaseAcquisitijoinon.process_data, which must be overwritten by inheriting functions.")


class BaseAcquisitionParser:
    def __init__(self):
        self.parser = ArgumentParser(description='Acquisition thread argument parser')
        self.parser.add_argument('--quiet', action='store_true',
                                 help='use this argument to ignore logs and notifications')

        # Parse arguments and disable pop up notifications
        self.args = self.parser.parse_args()
        if self.quiet():
            SocketOperations.quiet = True

    def quiet(self):
        return self.args.quiet
