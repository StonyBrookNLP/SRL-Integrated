from pprint import pprint
from nltk.corpus import framenet as fn


def getFrame(lex):
   frames = fn.frames_by_lemma(lex)
   for frame in frames:
      print frame.name +" " +str(frame.ID)


if __name__ == "__main__":
   getFrame('convert.v')
