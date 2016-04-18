/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import java.io.Serializable;
import java.util.ArrayList;

public class ArgProcessAnnotationData implements Serializable{


    /*private boolean annotated ;

    public boolean isAnnotated() {
        return annotated;
    }

    public String getKey()
    {
        return processName+"\t"+rawText;
    }
    public void setAnnotated(boolean annotated) {
        this.annotated = annotated;
    }
    
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getProcessName() {
        return processName;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public void setSentence(Sentence sentence) {
        this.sentence = sentence;
    }

    public void setProcessName(String process) {
        this.processName = process;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public boolean isRoleAndAnnotationExist(String roleName) {
        ArrayList<ArgumentSpan> spans = sentence.getArgumentSpan(roleName);
        for (int i = 0; i < spans.size(); i++) {
            if (spans.get(i).getAnnotatedLabel().equalsIgnoreCase("1") || spans.get(i).getAnnotatedLabel().equalsIgnoreCase("-1")) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Integer> getRoleIdx(String roleName) {
        return sentence.getRoleIdx(roleName);
    }

    public void clearRoleFiller(String roleName) {
        sentence.clearRoleFiller(roleName);
    }
    
    public ArrayList<ArgumentSpan> getArgumentSpan(String roleName)
    {
        return sentence.getArgumentSpan(roleName);
    }
    
    public ArrayList<ArgumentSpan> getAllArgumentSpan()
    {
        return sentence.getAllArgumentSpan();
    }*/
}
