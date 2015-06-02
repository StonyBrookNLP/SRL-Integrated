#!/bin/sh

DIR=$1
for sentFile in "$DIR/*.sent"; do
	echo $sentFile 
	treeFile = 
	java -mx150m -cp "stanford-parser-full-2015-01-30/*:" edu.stanford.nlp.parser.lexparser.LexicalizedParser -outputFormat "penn" edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz $sentFile  > $TREE_FILE 
	#java -mx150m -cp "stanford-parser-full-2015-01-30/*:" edu.stanford.nlp.trees.EnglishGrammaticalStructure -treeFile "$TREE_FILE"  -conllx  -basic > "$CONLL06_FILE" 
done 
