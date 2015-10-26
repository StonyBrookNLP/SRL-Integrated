#! /usr/bin/python

# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.

__author__ = "slouvan"
__date__ = "$Oct 20, 2015 12:56:06 AM$"

from pprint import pprint
from nltk.corpus import framenet as fn
import sys

def getFrame(lex):
    
    frameList = []
    frames = fn.frames_by_lemma(lex)
    for frame in frames:
        frameList.append(frame.name)
    return frameList

def getAllFrames():
    fNames = []
    for frame in fn.frames():
        #print frame.name +"\t"+str(frame.ID)
        fNames.append(frame.name)
    return fNames

def getNbFrame():
    return len(fn.frames())

if __name__ == "__main__":
    getFrame('burn.v')

