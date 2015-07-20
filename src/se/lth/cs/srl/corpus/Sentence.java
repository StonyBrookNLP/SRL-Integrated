package se.lth.cs.srl.corpus;

import is2.data.SentenceData09;
import is2.io.CONLLReader09;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import qa.ProcessFrame;

public class Sentence extends ArrayList<Word> {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private static final long serialVersionUID = 10;

    private List<Predicate> predicates;
    public HashMap<String, ArrayList<WordProbsPair>> argProbs;

    private Sentence() {
        Word BOS = new Word(this);
        super.add(BOS); //Add the root token
        predicates = new ArrayList<Predicate>();
        argProbs = new HashMap<String, ArrayList<WordProbsPair>>();
    }

    public Sentence(SentenceData09 data, boolean skipTree) {
        this();
        for (int i = 0; i < data.forms.length; ++i) {
            Word nextWord = new Word(data.forms[i], data.plemmas[i], data.ppos[i], data.pfeats[i], this, i + 1);
            super.add(nextWord);
        }
        if (skipTree) {
            return;
        }
        for (int i = 0; i < data.forms.length; ++i) {
            Word curWord = super.get(i + 1);
            curWord.setHead(super.get(data.pheads[i]));
            curWord.setDeprel(data.plabels[i]);
        }
        this.buildDependencyTree();
    }

    public Sentence(String[] words, String[] lemmas, String[] tags, String[] morphs) {
        this();
        for (int i = 1; i < words.length; ++i) { //Skip root-tokens.
            Word nextWord = new Word(words[i], lemmas[i], tags[i], morphs[i], this, i);
            super.add(nextWord);
        }
    }

    private void addPredicate(Predicate pred) {
        predicates.add(pred);
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void buildDependencyTree() {
        for (int i = 1; i < size(); ++i) {
            Word curWord = get(i);
            curWord.setHead(get(curWord.getHeadId()));
        }
    }

    public void buildSemanticTree() {
        for (int i = 0; i < predicates.size(); ++i) {
            Predicate pred = predicates.get(i);
            for (int j = 1; j < super.size(); ++j) {
                Word curWord = get(j);
                String arg = curWord.getArg(i);
                if (!arg.equals("_")) {
                    pred.addArgMap(curWord, arg);
                }
            }
        }
        for (Word w : this) //Free this memory as we no longer need this string array
        {
            w.clearArgArray();
        }
    }

    // s.louvan
    public void buildSemanticTreeWScore() {
        for (int i = 0; i < predicates.size(); ++i) {
            Predicate pred = predicates.get(i);
            for (int j = 1; j < super.size(); ++j) {
                Word curWord = get(j);
                String arg = curWord.getArg(i);
                if (!arg.equals("_")) {
                    String label = arg.split(":")[0];
                    System.out.println(arg);
                    String scoresStr[] = arg.split(":")[1].split(",");
                    pred.addArgMap(curWord, label); // just take the first one, change arg
                    if (argProbs.get(label) == null)
                    {
                        ArrayList<WordProbsPair> arr = new ArrayList<WordProbsPair>();
                        HashMap<String,Double> scores = new HashMap<String,Double>();
                        scores.put("A0", Double.parseDouble(scoresStr[0]));
                        scores.put("A1", Double.parseDouble(scoresStr[1]));
                        scores.put("A2", Double.parseDouble(scoresStr[2]));
                        arr.add(new WordProbsPair(curWord, scores));
                        argProbs.put(label, arr);
                    }
                    else
                    {
                        ArrayList<WordProbsPair> arr = argProbs.get(label);
                        HashMap<String,Double> scores = new HashMap<String,Double>();
                        scores.put("A0", Double.parseDouble(scoresStr[0]));
                        scores.put("A1", Double.parseDouble(scoresStr[1]));
                        scores.put("A2", Double.parseDouble(scoresStr[2]));
                        arr.add(new WordProbsPair(curWord, scores));
                        argProbs.put(label, arr);
                    }
                }
            }
        }
        for (Word w : this) //Free this memory as we no longer need this string array
        {
            w.clearArgArray();
        }
    }

    public String toString() {
        String tag;
        StringBuilder ret = new StringBuilder();
        for (int i = 1; i < super.size(); ++i) {
            Word w = super.get(i);
            ret.append(i).append("\t").append(w.toString());
            if (!(w instanceof Predicate)) //If its not a predicate add the FILLPRED and PRED cols
            {
                ret.append("\t_\t_");
            }
            for (int j = 0; j < predicates.size(); ++j) {
                ret.append("\t");
                Predicate pred = predicates.get(j);
                ret.append((tag = pred.getArgumentTag(w)) != null ? tag : "_");
            }
            ret.append("\n");
        }
        return ret.toString().trim();
    }

    //s.louvan

    public String toStringWithScores() {
        String tag;
        StringBuilder ret = new StringBuilder();
        for (int i = 1; i < super.size(); ++i) {
            Word w = super.get(i);
            ret.append(i).append("\t").append(w.toString());
            if (!(w instanceof Predicate)) //If its not a predicate add the FILLPRED and PRED cols
            {
                ret.append("\t_\t_");
            }
            for (int j = 0; j < predicates.size(); ++j) {
                ret.append("\t");
                Predicate pred = predicates.get(j);
                //ret.append((tag=pred.getArgumentTag(w))!=null?tag:"_");
                if (pred.getArgumentTag(w) != null) {
                    String label = pred.getArgumentTag(w);
                    //A0:[A0:0.5;A1:0.2,A2:0.3]
                    ArrayList<WordProbsPair> wpPair = argProbs.get(label);
                    WordProbsPair wp = wpPair.stream().filter(node -> node.getWord().getIdx() == w.getIdx()).collect(Collectors.toList()).get(0);
                    StringBuilder scores = new StringBuilder();
                    scores.append(wp.getScore("A0")).append(",");
                    scores.append(wp.getScore("A1")).append(",");
                    scores.append(wp.getScore("A2"));
                    // get the scores 
                    ret.append(pred.getArgumentTag(w)).append(":").append(scores);
                } else {
                    ret.append("_");
                }
            }
            ret.append("\n");
        }
        return ret.toString().trim();
    }

    public void makePredicate(int wordIndex) {
        Predicate p = new Predicate(super.get(wordIndex));
        super.set(wordIndex, p);
        addPredicate(p);
    }

    /*
     * Functions used when interfacing with Bohnets parser
     * These need to be fixed. Or rather the Sentence object should go altogether.
     */
    public String[] getFormArray() {
        String[] ret = new String[this.size()];
        //ret[0]="<root>";
        for (int i = 0; i < this.size(); ++i) {
            ret[i] = this.get(i).Form;
        }
        return ret;
    }

    public String[] getPOSArray() {
        String[] ret = new String[this.size()];
        //ret[0]="<root-POS>";
        for (int i = 0; i < this.size(); ++i) {
            ret[i] = this.get(i).POS;
        }
        return ret;
    }

    public String[] getFeats() {
        String[] ret = new String[this.size()];
        ret[0] = CONLLReader09.NO_TYPE;
        for (int i = 1; i < this.size(); ++i) {
            ret[i] = this.get(i).getFeats();
        }
        return ret;
    }

    public void setHeadsAndDeprels(int[] heads, String[] deprels) {
        for (int i = 0; i < heads.length; ++i) {
            Word w = this.get(i + 1);
            w.setHead(this.get(heads[i]));
            w.setDeprel(deprels[i]);
        }
    }

    public static Sentence newDepsOnlySentence(String[] lines) {
        Sentence ret = new Sentence();
        Word nextWord;
        int ix = 1;
        for (String line : lines) {
            String[] cols = WHITESPACE_PATTERN.split(line, 13);
            nextWord = new Word(cols, ret, ix++);
            ret.add(nextWord);
        }
        ret.buildDependencyTree();
        return ret;

    }

    public static Sentence newSentence(String[] lines) {
        Sentence ret = new Sentence();
        Word nextWord;
        int ix = 1;
        for (String line : lines) {
            //System.out.println(line);
            String[] cols = WHITESPACE_PATTERN.split(line);
            if (cols[12].equals("Y")) {
                Predicate pred = new Predicate(cols, ret, ix++);
                ret.addPredicate(pred);
                nextWord = pred;
            } else {
                nextWord = new Word(cols, ret, ix++);
            }
            ret.add(nextWord);
        }
        ret.buildDependencyTree();
        ret.buildSemanticTree();
        return ret;
    }

    public static Sentence newSentenceWithScore(String[] lines) {
        Sentence ret = new Sentence();
        Word nextWord;
        int ix = 1;
        for (String line : lines) {
            //System.out.println(line);
            String[] cols = WHITESPACE_PATTERN.split(line);
            if (cols[12].equals("Y")) {
                Predicate pred = new Predicate(cols, ret, ix++);
                ret.addPredicate(pred);
                nextWord = pred;
            } else {
                nextWord = new Word(cols, ret, ix++);
            }
            ret.add(nextWord);
        }
        ret.buildDependencyTree();
        ret.buildSemanticTreeWScore();
        return ret;
    }

    public static Sentence newSRLOnlySentence(String[] lines) {
        Sentence ret = new Sentence();
        Word nextWord;
        int ix = 1;
        for (String line : lines) {
            String[] cols = WHITESPACE_PATTERN.split(line, 13);
            if (cols[12].charAt(0) == 'Y') {
                Predicate pred = new Predicate(cols, ret, ix++);
                ret.addPredicate(pred);
                nextWord = pred;
            } else {
                nextWord = new Word(cols, ret, ix++);
            }
            ret.add(nextWord);
        }
        ret.buildDependencyTree();
        return ret;
    }

    public final Comparator<Word> wordComparator = new Comparator<Word>() {
        @Override
        public int compare(Word arg0, Word arg1) {
            return indexOf(arg0) - indexOf(arg1);
        }
    };
}
