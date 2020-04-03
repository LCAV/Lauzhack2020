#! /usr/bin/env python3
# -*- coding: utf-8 -*-

"""
io_operations.py: Some input-output operations used throughout the repository.
"""

import datetime
import json
import time
import os

import numpy as np

def get_available_files(output_dir, exclude=[]):
    available = [f[:-4] for f in os.listdir(output_dir) if f[-3:] == 'csv']
    available.sort()
    [available.remove(excl) for excl in exclude if excl in available]

    print('available files:')
    for i, a in enumerate(available):
        try:
            value = datetime.datetime.fromtimestamp(int(a))
            print(i, ':', a, '\t', value.strftime('%Y-%m-%d %H:%M:%S'))
        except (TypeError, ValueError):
            print(i, ':', a)
    return available


def to_int_if_possible(input):
    input = input.replace(' ', '')
    try:
        num = int(input)
    except ValueError:
        return input
    return num


def make_dirs_safe(path):
    """ Make directory only if it does not exist yet. """ 
    dirname = os.path.dirname(path)
    if not os.path.exists(dirname):
        os.makedirs(dirname)


def read_json(file_json):
    with open(file_json, 'r') as json_data:
        return json.load(json_data)


def read_dataset(dataset_name, context=None):
    ''' Return panda dataframe of dataset, read from csv. 
    
    :param dataset_name: name of csv file. 
    :param context: optional, if given then all anchors that do not appear in context are ignored.
    '''
    import pandas as pd
    if  dataset_name[-4:] != '.csv':
        dataset_name += '.csv'

    # Sample 100 rows of data to determine dtypes.
    df_test = pd.read_csv(dataset_name, nrows=100)
    float_cols = [c for c in df_test if df_test[c].dtype == "float64"]
    float32_cols = {c: np.float32 for c in float_cols}

    # read data in correct precision.
    raw_data = pd.read_csv(dataset_name, engine='c', dtype=float32_cols)

    if context is not None:
        valid_anchors = raw_data.anchor_id.isin(np.append(context.anchor_ids, np.nan))
        if np.sum(valid_anchors) == 0:
            raise ValueError('file {} does not contain any valid measurements.'.format(dataset_name))
        print('number of measurements from unknown anchors: {} / {}'.format(
            np.sum(~valid_anchors), len(raw_data)))
        raw_data = raw_data[valid_anchors]

    # convert anchor_id to string.
    raw_data.loc[:, 'anchor_id'] = raw_data.loc[:, 'anchor_id'].astype(str)

    # remove trailing white spaces
    raw_data['anchor_id'] = raw_data['anchor_id'].map(str.strip)

    # TODO(FD) this used to be necessary for pozyx, but not anymore
    # and it actually messed up results at some point. I removed it for now 
    # but we should do it and make sure that his has the correct effect. 
    # raw_data = raw_data.sort_values('timestamp')

    return raw_data


def find_next(output_dir, mask):
    """ Increase i until file of structure output_dir + mask.format(i) does not exist. """
    i = 0
    while True:
        file_path = output_dir + mask.format(i)
        if os.path.exists(file_path):
            i += 1
        else:
            return file_path


def parse_config(config_json):
    """ Parse configuration json file

    :param str config_json: path to config json file, relative to the repository's root 
                            directory (where this file is stored). 
    :return: configuration dict
    """

    from os import path

    rootdir = path.dirname(path.abspath(__file__)) + "/"
    try:
        print("reading", config_json)
        config = read_json(config_json)
        try:
            config["anchors_file"] = rootdir + config["anchors_file"]
            if not path.isfile(config["anchors_file"]):
                config["anchors_file"] = path.abspath(config["anchors_file"])
        except:
            print('no anchors_file found.')
            

        try:
            config["calibration_file"] = rootdir + config["calibration_file"]
            if not path.isfile(config["calibration_file"]):
                config["calibration_file"] = path.abspath(config["calibration_file"])
        except:
            print('no calibration_file found.')
            
    except Exception as e:
        print(
            "Warning: did not find a valid config file at {}. Continuing with empty parameters. ".format(config_json))
        print(e)
        config = {"systems": []}

    return config


def prepare_output(output_dir='./', output_name=''):
    """ Creates output directory and output file.

    :param str output_dir: output directory
    :param str output_name: output name (optional)
    :return output file path. If output_name is not given, it is set to <current timestamp in ms>.csv.

    """
    if output_name.find('/') >= 0:
        raise NameError('output_name needs to be a file name without directory.')

    if output_name == '':
        output_name = "{}.csv".format(int(time.time()))

    if output_dir  == '':
        output_dir = './'
    elif output_dir[-1] != '/':
        output_dir = output_dir + '/'

    make_dirs_safe(output_dir)

    outfile = output_dir + output_name
    #  if os.path.exists(outfile):
    #      raise NameError("File {} already exists. Don't want to overwrite".format(outfile))
    return outfile


# TODO: compare this with Context. We could
# potentially replace one by the other. 
def fill_from_file(anchors_file, system_id, anchors_position=None, anchors_orientation=None, anchors_ids=None,
                   anchors_scale=None):
    """ Read data about anchors from file. 
    
    :param anchors_file: file name. 
    :param system_id: system id to consider. 
    :param anchors_position: empty dict. 
    :param anchors_orientation: empty dict. 
    :param anchors_ids: empty array.
    :param anchors_scale: empty dict. For now used only by Pixlive.  

    
    """
    import ubiment_parameters as UBI

    with open(anchors_file, 'r') as af:
        headers = af.readline()
        for line in af:
            spl = line.split(',')
            this_system_id = int(spl[UBI.anchor_system_id])

            if this_system_id == system_id:
                anchor_id = to_int_if_possible(spl[UBI.anchor_anchor_id])
                position = [
                    float(spl[UBI.anchor_px]),
                    float(spl[UBI.anchor_py]),
                    float(spl[UBI.anchor_pz])
                ]
                try:
                    orientation = [
                        float(spl[UBI.anchor_theta_x]),
                        float(spl[UBI.anchor_theta_y]),
                        float(spl[UBI.anchor_theta_z])
                    ]
                except IndexError:
                    orientation = [0, 0, 0]
                try:
                    scale = [
                        float(spl[UBI.anchor_scale_x]),
                        float(spl[UBI.anchor_scale_y])
                    ]
                except IndexError:
                    scale = [1, 1]

                if anchors_position is not None:
                    anchors_position[anchor_id] = position
                if anchors_orientation is not None:
                    anchors_orientation[anchor_id] = orientation
                if anchors_ids is not None:
                    anchors_ids.append(anchor_id)
                if anchors_scale is not None:
                    anchors_scale[anchor_id] = scale


def generate_empty_anchors_file(filename):
    import ubiment_parameters as UBI
    with open(filename, 'w') as f:
        start = True
        for a in UBI.anchor_fields:
            if not start: 
                f.write(',')
            f.write(a)
            start = False


def extract_safe(dict_, key):
    if key in dict_:
        return dict_[key]
    else:
        return None
        
def save_params(filename, **kwargs):
    import json
    make_dirs_safe(filename)
    for key in kwargs.keys():
        try:
            # convert numpy arrays to lists because they are ugly otherwise. 
            kwargs[key] = kwargs[key].tolist()
        except AttributeError as e:
            pass
    with open(filename, 'w') as fp:
        json.dump(kwargs, fp, indent=4)
        print('saved parameters as', filename)
