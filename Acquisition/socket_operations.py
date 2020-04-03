#! /usr/bin/env python3
# -*- coding: utf-8 -*-

"""
socket_operations.py: Define the UDP_IP according to the computer which is used.
"""
import subprocess

WLAN_NAME = 'wlp'


class SocketOperations:
    udp_ip = None
    quiet = False

    @staticmethod
    def get_udp_ip():
        if SocketOperations.udp_ip is None:
            ifconfig = subprocess.run("ifconfig", stdout=subprocess.PIPE).stdout.decode("utf-8")
            ifconfig = ifconfig[ifconfig.find(WLAN_NAME):]
            ifconfig = ifconfig[ifconfig.find('inet addr:'):ifconfig.find('Bcast')]
            ip = ifconfig.replace('inet addr:', '').replace(' ', '')

            if not SocketOperations.quiet:
                # Show the IP address
                print('\nNotification: IP address is', ip, '\n')
                subprocess.Popen(["notify-send", ip])

            SocketOperations.udp_ip = ip

        return SocketOperations.udp_ip

    @staticmethod
    def get_pozyx_usb_port():
        return "/dev/ttyACM0"

