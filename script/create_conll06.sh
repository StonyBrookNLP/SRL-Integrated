#!/bin/sh

SENTENCE_FILE=$1
TREE_FILE=$2
CONLL06_FILE=$3
java -mx150m -cp "stanford-parser-full-2015-01-30/*:" edu.stanford.nlp.parser.lexparser.LexicalizedParser -outputFormat "penn" edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz $SENTENCE_FILE  > $TREE_FILE 
java -mx150m -cp "stanford-parser-full-2015-01-30/*:" edu.stanford.nlp.trees.EnglishGrammaticalStructure -treeFile "$TREE_FILE"  -conllx  -basic > "$CONLL06_FILE" 

