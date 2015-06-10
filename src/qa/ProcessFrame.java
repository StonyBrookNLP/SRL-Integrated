 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProcessFrame implements Serializable{

    private String processName;
    private String rawText;
    private String[] tokenizedText;
    private String underGoer;
    private String enabler;
    private String trigger;
    private String result;
    private String underSpecified;
    private ArrayList<Integer> undergoerIndex = new ArrayList<Integer>();
    private ArrayList<Integer> enablerIndex = new ArrayList<Integer>();
    private ArrayList<Integer> triggerIndex = new ArrayList<Integer>();
    private ArrayList<Integer> resultIndex = new ArrayList<Integer>();

    public ArrayList<Integer> getTriggerIdx() {
        

        return triggerIndex;
    }

    public ArrayList<Integer> getAllLabeledIndex()
    {
        ArrayList<Integer> labeledIdxs = new ArrayList<Integer>();
        processRoleFillers();
        if (undergoerIndex.size() > 0)
            labeledIdxs.addAll(undergoerIndex);
        if (enablerIndex.size() > 0)
            labeledIdxs.addAll(enablerIndex);
        if (resultIndex.size() > 0)
            labeledIdxs.addAll(resultIndex);
        if (triggerIndex.size() > 0)
            labeledIdxs.addAll(triggerIndex);
        
        return labeledIdxs;
    }
    public void clearAllIndexes()
    {
        undergoerIndex.clear();
        enablerIndex.clear();
        resultIndex.clear();
        triggerIndex.clear();
    }
    public void processRoleFillers() {
        //System.out.println("PROCESSING");
        clearAllIndexes();
        ArrayList<String> roleFillers = new ArrayList<String>();
        String[] undergoers = underGoer.split("\\|");
        String[] enablers = enabler.split("\\|");
        String[] triggers = trigger.split("\\|");
        String[] results = result.split("\\|");

        ArrayList<Integer> allIdx = new ArrayList<Integer>();
        for (String str : undergoers) {
            if (str.trim().length() > 0) {
                roleFillers.add(str.trim() + ":A0");
            }
        }
        for (String str : enablers) {
            if (str.trim().length() > 0) {
                roleFillers.add(str.trim() + ":A1");
            }
        }
        for (String str : triggers) {
            if (str.trim().length() > 0) {
                roleFillers.add(str.trim() + ":T");
            }
        }
        for (String str : results) {
            if (str.trim().length() > 0) {
                roleFillers.add(str.trim() + ":A2");
            }
        }

        Collections.sort(roleFillers, new MyComparator());
        for (String roleFiller : roleFillers) {
            List<String> tokens = StanfordTokenizerSingleton.getInstance().tokenize(roleFiller.split(":")[0]);
            String[] pattern = new String[tokens.size()];
            tokens.toArray(pattern);
            ArrayList<Integer> matches = getIdxMatchesv2(pattern, tokenizedText, allIdx);
            // Check type
            String type = roleFiller.split(":")[1];
            if (type.equalsIgnoreCase("A0")) {
                if (matches != null)
                    undergoerIndex.addAll(matches);
            }
            if (type.equalsIgnoreCase("A1")) {
                if (matches != null)
                    enablerIndex.addAll(matches);
            }
            if (type.equalsIgnoreCase("T")) {
                if (matches != null)
                    triggerIndex.addAll(matches);
            }
            if (type.equalsIgnoreCase("A2")) {
                if (matches != null)
                    resultIndex.addAll(matches);
            }
            if (matches!= null)
                allIdx.addAll(matches);
        }

    }

    public ArrayList<Integer> getEnablerIdx() {

        return enablerIndex;
    }

    public ArrayList<Integer> getResultIdx() {
 

        return resultIndex;
    }

    public ArrayList<Integer> getUndergoerIdx() {


        return undergoerIndex;
    }

    public String getRawText() {
        return rawText;
    }

    public String getUnderGoer() {
        return underGoer;
    }

    public String getEnabler() {
        return enabler;
    }

    public String getTrigger() {
        return trigger;
    }

    public String getResult() {
        return result;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public void setUnderGoer(String underGoer) {
        this.underGoer = underGoer;
    }

    public void setEnabler(String enabler) {
        this.enabler = enabler;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getUnderSpecified() {
        return underSpecified;
    }

    public void setUnderSpecified(String underSpecified) {
        this.underSpecified = underSpecified;
    }

    public String[] getTokenizedText() {
        return tokenizedText;
    }

    public void setTokenizedText(String[] tokenizedText) {
        this.tokenizedText = tokenizedText;
    }

    public ArrayList<Integer> getIdxMatchesv2(String[] targetPattern, String[] tokenizedSentence, ArrayList<Integer> idxs) {
        boolean inRegion = false;
        int matchStart = 0;
        int matchEnd = targetPattern.length;
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < tokenizedSentence.length && matchStart < matchEnd; i++) {
            if (tokenizedSentence[i].equalsIgnoreCase(targetPattern[matchStart]) && !idxs.contains(i+1)) {
                idx.add(i + 1); // because ConLL index starts from 1 
                if (!inRegion) {
                    inRegion = true;
                }
                matchStart++;
            } else {
                if (inRegion) {
                    inRegion = false;
                    idx.clear();
                    matchStart--;
                }
            }
        }
        if (matchStart == matchEnd) {
            return idx;
        } else {
            if (targetPattern[0].length() > 0) {
                System.out.println(Arrays.toString(tokenizedSentence));
                //System.out.println("ERROR : CANNOT FIND \"" + Arrays.toString(targetPattern) + "\" IN THE SENTENCE");
            }
            return null;
        }
    }

    public ArrayList<Integer> getIdxMatches(String[] targetPattern, String[] tokenizedSentence) {
        boolean inRegion = false;
        int matchStart = 0;
        int matchEnd = targetPattern.length;
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < tokenizedSentence.length && matchStart < matchEnd; i++) {
            if (tokenizedSentence[i].equalsIgnoreCase(targetPattern[matchStart])) {
                idx.add(i + 1); // because ConLL index starts from 1 
                if (!inRegion) {
                    inRegion = true;
                }
                matchStart++;
            } else {
                if (inRegion) {
                    inRegion = false;
                    idx.clear();
                    matchStart--;
                }
            }
        }
        if (matchStart == matchEnd) {
            return idx;
        } else {
            if (targetPattern[0].length() > 0) {
                System.out.println(Arrays.toString(tokenizedSentence));
                System.out.println("ERROR : CANNOT FIND \"" + Arrays.toString(targetPattern) + "\" IN THE SENTENCE");
            }
            return null;
        }
    }
    
    public String toString()
    {
        StringBuilder strB = new StringBuilder();
        strB.append(processName+"\t");
        strB.append(underGoer+"\t");
        strB.append(enabler+"\t");
        strB.append(trigger+"\t");
        strB.append(result+"\t");
        strB.append(underSpecified+"\t");
        strB.append(rawText);
        
        return strB.toString();
    }

}
class MyComparator implements java.util.Comparator<String> {




    public int compare(String o1, String o2) {
        if (o1.length() > o2.length())
            return -1;
        if (o1.length() == o2.length())
            return 0;
        return 1;
        
    }
}