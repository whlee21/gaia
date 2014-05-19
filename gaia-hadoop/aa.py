#!/usr/bin/python

import re
import sys

if __name__ == '__main__':
    filename = sys.argv[1]
    fh = open(filename)
    line = fh.readline().strip()
    while line != '':
        end = line.find('.nocomments')
        print 'mv ' + line + ' ' + line[0:end]
        line = fh.readline().strip()
    fh.close()