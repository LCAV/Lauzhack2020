# Lauzhack2020

Resources for the Bluetooth/IMU challange at Lauzhack 2020.

## Install instructions for Linux

The provided code was tested with Python 3.8 on Ubuntu 18. 

Make sure you have the `ifconfig` command line tool (`sudo apt install net-tools`). 

Install the python requirements by running 

``` 
pip install -r requirements
```

and you are set. 

## Instructions

Install the two android applications for IMU and Beacon measurements on a smartphone. Opening them will start collecting data. 
You can then run the python script `beacon_acquisition_thread.py`, which opens an UDP port and saves the Bluetooth data received from the phone at `output/data.csv`.

*Important:* Make sure you enter the IP address of your computer in the designated field in the app.

Similarily, you can run `imu_acquisition_thread.py` to acquire IMU data. 
