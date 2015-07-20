/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.lth.cs.srl.corpus;

import java.util.HashMap;

/**
 *
 * @author samuellouvan
 */
public class WordProbsPair {
    private Word word;
    private HashMap<String, Double> labelConfidencePair;
    private double x = 0;
    private double y = 0;
    public WordProbsPair(Word w, HashMap<String, Double> lblConfPair)
    {
        this.word = w;
        this.labelConfidencePair = lblConfPair;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public HashMap<String, Double> getLabelConfidencePair() {
        return labelConfidencePair;
    }

    public void setLabelConfidencePair(HashMap<String, Double> labelConfidencePair) {
        this.labelConfidencePair = labelConfidencePair;
    }
    
    public double getScore(int idx)
    {
        if (idx == 0)
            return labelConfidencePair.get("A0");
        else if (idx == 1)
            return labelConfidencePair.get("A1");
        else if (idx == 2)
            return labelConfidencePair.get("A2");
        else
            return labelConfidencePair.get("T");
    }
    public double getScore(String label)
    {
        if (labelConfidencePair.get(label) != null)
            return labelConfidencePair.get(label);
        return 0.0;
    }
    
}
