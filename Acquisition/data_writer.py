from __future__ import print_function
import sys
from threading import RLock
import csv


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

class DataWriter():
    locker = RLock()

    def __init__(self, targetfile, header=None, verbose=False, verbose_interval=10):
        self.csvfile = open(targetfile, 'w')
        self.csvwriter = csv.writer(self.csvfile, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        self.verbose = verbose
        self.verbose_interval = verbose_interval
        self.count = 0
        if header is not None:
            self.header = header
        else:
            from ubiment_parameters import data_fields
            self.header = data_fields
        
        self.csvwriter.writerow(self.header)
        if self.verbose:
            print("writing file with header \n", self.header)

    def writedict(self, dico):
        """
        write the data from dictionary to the log file
        :param dictionary: Entries of the dictionary must be contained in the self.header array
        :return: None 
        """
        # Missing values are replaced by 'NaN'. Value not existing in the header are ignored
        line = [dico[f] if f in list(dico.keys()) else 'NaN' for f in self.header]
        self.writeline(line)

    def writeline(self, line):
        # we lock this part because we don't want two scripts writing at the same time
        with self.locker:
            if self.verbose and self.count % self.verbose_interval == 0:
                print("Data writer is about to write: ", line)
            self.csvwriter.writerow(line)
            self.csvfile.flush()
            self.count += 1

    def onDestroy(self):
        print("Data writer onDestroy()")
        self.csvfile.close()
