 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qa;

import Util.GlobalV;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class ProcessFrame implements Serializable {

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
    private ArrayList<Integer> underSpecifiedIndex = new ArrayList<Integer>();
    private ArrayList<List<Double>> scores = new ArrayList<List<Double>>(4);

    public ProcessFrame() {
        scores.add(new ArrayList<Double>());
        scores.add(new ArrayList<Double>());
        scores.add(new ArrayList<Double>());
        scores.add(new ArrayList<Double>());
        processName = "";
        rawText = "";
        underGoer = "";
        enabler = "";
        trigger = "";
        underSpecified = "";
        result = "";
    }

    public void setScores(int idx, double[] scoresArr) {
        scores.set(idx, Lists.newArrayList(Doubles.asList(scoresArr)));
    }

    public ArrayList<Integer> getTriggerIdx() {

        return triggerIndex;
    }

    public ArrayList<Integer> getAllLabeledIndex() {
        ArrayList<Integer> labeledIdxs = new ArrayList<Integer>();
        processRoleFillers();
        if (undergoerIndex.size() > 0) {
            labeledIdxs.addAll(undergoerIndex);
        }
        if (enablerIndex.size() > 0) {
            labeledIdxs.addAll(enablerIndex);
        }
        if (resultIndex.size() > 0) {
            labeledIdxs.addAll(resultIndex);
        }
        if (triggerIndex.size() > 0) {
            labeledIdxs.addAll(triggerIndex);
        }
        if (underSpecifiedIndex.size() > 0) {
            labeledIdxs.addAll(underSpecifiedIndex);
        }

        return labeledIdxs;
    }

    public void clearAllIndexes() {
        undergoerIndex.clear();
        enablerIndex.clear();
        resultIndex.clear();
        triggerIndex.clear();
        underSpecifiedIndex.clear();
    }

    public void updateTrigger() {

    }

    public void processRoleFillers() {
        //System.out.println("PROCESSING");
        clearAllIndexes();
        ArrayList<String> roleFillers = new ArrayList<String>();
        String[] undergoers = underGoer.split("\\|");
        String[] enablers = enabler.split("\\|");
        String[] triggers = trigger.split("\\|");
        String[] results = result.split("\\|");
        String[] underspecified = result.split("\\|");

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

        for (String str : underspecified) {
            if (str.trim().length() > 0) {
                roleFillers.add(str.trim() + ":A3");
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
                if (matches != null) {
                    undergoerIndex.addAll(matches);
                }
            }
            if (type.equalsIgnoreCase("A1")) {
                if (matches != null) {
                    enablerIndex.addAll(matches);
                }
            }
            if (type.equalsIgnoreCase("T")) {
                if (matches != null) {
                    triggerIndex.addAll(matches);
                    // Check if VB or process name or NN
                }
            }
            if (type.equalsIgnoreCase("A2")) {
                if (matches != null) {
                    resultIndex.addAll(matches);
                }
            }
            if (type.equalsIgnoreCase("A3")) {
                if (matches != null) {
                    underSpecifiedIndex.addAll(matches);
                }
            }
            if (matches != null) {
                allIdx.addAll(matches);
            }
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

    public ArrayList<Integer> getUnderspecifiedIdx() {

        return underSpecifiedIndex;
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

    public String getQuestionText() {
        return processName;
    }

    public void setTokenizedText(String[] tokenizedText) {
        this.tokenizedText = tokenizedText;
    }

    public boolean isExistUndergoer() {
        return getUndergoerIdx().size() > 0;
    }

    public boolean isExistEnabler() {
        return getEnablerIdx().size() > 0;
    }

    public boolean isExistTrigger() {
        return getTriggerIdx().size() > 0;
    }

    public boolean isExistResult() {
        return getResultIdx().size() > 0;
    }

    public boolean isExistRole(String roleName) {
        if (roleName.equalsIgnoreCase("A0")) {
            return isExistUndergoer();
        }
        if (roleName.equalsIgnoreCase("A1")) {
            return isExistEnabler();
        }
        if (roleName.equalsIgnoreCase("T")) {
            return isExistTrigger();
        }
        if (roleName.equalsIgnoreCase("A2")) {
            return isExistResult();
        }

        return false;
    }

    public static ArrayList<Integer> getIdxMatchesv2(String[] targetPattern, String[] tokenizedSentence, ArrayList<Integer> idxs) {
        boolean inRegion = false;
        int matchStart = 0;
        int matchEnd = targetPattern.length;
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < tokenizedSentence.length && matchStart < matchEnd; i++) {
            if (tokenizedSentence[i].equalsIgnoreCase(targetPattern[matchStart]) && !idxs.contains(i + 1)) {
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

    public String getStringFromIdx(ArrayList<Integer> idxs) {
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < idxs.size(); i++) {
            strBuilder.append(tokenizedText[idxs.get(i) - 1]).append(" ");
        }
        return strBuilder.toString().trim();
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

    public ArrayList<Integer> getRoleIdx(String role) {
        if (role.equalsIgnoreCase(GlobalV.A0)) {
            return getUndergoerIdx();
        } else if (role.equalsIgnoreCase(GlobalV.A1)) {
            return getEnablerIdx();
        } else if (role.equalsIgnoreCase(GlobalV.T)) {
            return getTriggerIdx();
        } else if (role.equalsIgnoreCase(GlobalV.A2)) {
            return getResultIdx();
        } else if (role.equalsIgnoreCase(GlobalV.A3)) {
            return getUnderspecifiedIdx();
        } else {
            return null;
        }
        //if (role.equalsIgnoreCase(GlobalV.A0))
        //    return getUndergoerIdx();
    }

    public void setRoleFiller(String role, String text) {
        if (role.equalsIgnoreCase(GlobalV.A0)) {
            setUnderGoer(text);
        } else if (role.equalsIgnoreCase(GlobalV.A1)) {
            setEnabler(text);
        } else if (role.equalsIgnoreCase(GlobalV.T)) {
            setTrigger(text);
        } else if (role.equalsIgnoreCase(GlobalV.A2)) {
            setResult(text);
        } else if (role.equalsIgnoreCase(GlobalV.A3)) {
            setUnderSpecified(text);
        } else {

        }
    }

    public String getRoleFiller(String role) {
        if (role.equalsIgnoreCase(GlobalV.A0)) {
            return getUnderGoer();
        } else if (role.equalsIgnoreCase(GlobalV.A1)) {
            return getEnabler();
        } else if (role.equalsIgnoreCase(GlobalV.T)) {
            return getTrigger();
        } else if (role.equalsIgnoreCase(GlobalV.A2)) {
            return getResult();
        } else if (role.equalsIgnoreCase(GlobalV.A3)) {
            return getUnderSpecified();
        } else {
            return null;
        }
    }

    public String toStringAnnotation(String pattern, String query) {
        StringBuilder strB = new StringBuilder();
        strB.append(processName + "\t");
        strB.append(pattern + "\t");
        strB.append(query + "\t");
        strB.append(underGoer + "\t");
        strB.append(enabler + "\t");
        strB.append(trigger + "\t");
        strB.append(result + "\t");
        strB.append(underSpecified + "\t");
        strB.append(rawText);

        return strB.toString();
    }

    public String toString() {
        StringBuilder strB = new StringBuilder();
        strB.append(processName + "\t");
        strB.append(underGoer + "\t");
        strB.append(enabler + "\t");
        strB.append(trigger + "\t");
        strB.append(result + "\t");
        strB.append(underSpecified + "\t");
        strB.append(rawText);

        return strB.toString();
    }

    public String toStringWScore() {
        StringBuilder strB = new StringBuilder();
        // WATCHOUT FOR THE INDEX!
        strB.append(processName + "\t");

        strB.append(underGoer + "\t");
        strB.append(scores.get(0).size() == 0 ? "\t\t" : StringUtils.join(scores.get(0), "\t")).append("\t");

        strB.append(enabler + "\t");
        strB.append(scores.get(1).size() == 0 ? "\t\t" : StringUtils.join(scores.get(1), "\t")).append("\t");
        //strB.append(StringUtils.join(scores.get(1), "\t")).append("\t");

        strB.append(trigger + "\t");
        strB.append(scores.get(2).size() == 0 ? "\t" : StringUtils.join(scores.get(2), "\t")).append("\t");

        strB.append(result + "\t");
        strB.append(scores.get(3).size() == 0 ? "\t\t" : StringUtils.join(scores.get(3), "\t")).append("\t");

        strB.append(underSpecified + "\t");
        strB.append("\t\t\t");
        strB.append(rawText);

        return strB.toString();
    }

}

class MyComparator implements java.util.Comparator<String> {

    public int compare(String o1, String o2) {
        if (o1.length() > o2.length()) {
            return -1;
        }
        if (o1.length() == o2.length()) {
            return 0;
        }
        return 1;

    }
}
