/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import static java.util.stream.Collectors.toList;

/**
 *
 * @author slouvan
 */
public class JSONData implements Serializable{
    static final long serialVersionUID = 2106L;
    private String processName;
    private ArrayList<Sentence> sentence;

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public ArrayList<Sentence> getSentence() {
        return sentence;
    }

    public void setSentence(ArrayList<Sentence> sentence) {
        this.sentence = sentence;
    }

    public Sentence getSentence(int sentID) {
        return sentence.stream().filter(s -> s.getId() == sentID).collect(toList()).get(0);
    }

    public ArgumentSpan getPredictedArgumentSpan(int sentID, int startID, int endID) {
        Sentence targetSentence = getSentence(sentID);
        return targetSentence.getPredictedArgumentSpanJSON().stream().filter(a -> a.getStartIdxJSON() == startID && a.getEndIdxJSON() == endID).collect(toList()).get(0);
    }
    
    public ArrayList<String> getUniqueAnnotatedRoleLabelFromJSON()
    {
        ArrayList<String> labels = new ArrayList<String>();
        for (Sentence s : sentence)
        {
            ArrayList<ArgumentSpan> spans = s.getAnnotatedArgumentSpanJSON();
            for (ArgumentSpan span : spans)
            {
                String label= span.getAnnotatedRole();
                if (!labels.contains(label))
                {
                    labels.add(label);
                }
            }
        }

        return labels;
    }
}
