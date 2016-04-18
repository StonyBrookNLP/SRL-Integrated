/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.datastructure;

import edu.cmu.cs.lti.ark.util.ds.Pair;
import java.io.Serializable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.python.google.common.collect.Lists;
import qa.StanfordDepParserSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.util.FileUtil;

/**
 *
 * @author slouvan
 */
public class Sentence implements Serializable {

    static final long serialVersionUID = 2106L;
    private int id = 0;
    private String rawText;
    private String processName;
    DependencyTree depTree;
    HashMap<String, ArrayList<ArgumentSpan>> roleArgAnnotation;
    HashMap<String, ArrayList<ArgumentSpan>> roleArgPropBank;
    HashMap<String, ArrayList<ArgumentSpan>> roleArgPrediction;
    private boolean annotated = false;

    private ArrayList<ArgumentSpan> annotatedArgumentSpanJSON;
    private ArrayList<ArgumentSpan> predictedArgumentSpanJSON;

    public Sentence() {
    }

    public ArrayList<ArgumentSpan> getAnnotatedArgumentSpanJSON() {
        return annotatedArgumentSpanJSON;
    }

    // This is process name + tag
    public String getLexicalUnitFrame() {
        String[] lexUnitFrame = processName.trim().split("\\s+");
        if (lexUnitFrame.length > 1) {
            List<DependencyNode> nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodeCandidates.size() == 0) {
                nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            for (int i = 0; i < nodeCandidates.size(); i++) {
                DependencyNode currentFirst = nodeCandidates.get(i);
                if (currentFirst.getId() + 1 <= depTree.size() && depTree.get(currentFirst.getId() + 1).getForm().equalsIgnoreCase(lexUnitFrame[1])) {
                    return currentFirst.getLemma() + " " + depTree.get(currentFirst.getId() + 1).getLemma() + ".n";
                }
            }
            return processName + ".n"; // IMPOSSIBLE RETURN ACTUALLY
        } else {
            // must be 1
            List<DependencyNode> nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodes.size() == 0) {
                nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            return nodes.get(0).getLemma() + ".n";

        }
    }

    public String getLexicalUnitAndPOSTag() {
        StringBuilder strB = new StringBuilder();
        String[] lexUnitFrame = processName.trim().split("\\s+");
        if (lexUnitFrame.length > 1) {
            List<DependencyNode> nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodeCandidates.size() == 0) {
                nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            for (int i = 0; i < nodeCandidates.size(); i++) {
                DependencyNode currentFirst = nodeCandidates.get(i);
                if (currentFirst.getId() + 1 <= depTree.size() && depTree.get(currentFirst.getId() + 1).getForm().equalsIgnoreCase(lexUnitFrame[1])) {
                    //return currentFirst.getForm() + " " + depTree.get(currentFirst.getId() + 1).getForm() + ".n";
                    strB.append(currentFirst.getForm() + "_" + currentFirst.getCpos() + " " + depTree.get(currentFirst.getId() + 1).getForm() + "_" + depTree.get(currentFirst.getId() + 1).getCpos());
                }
            }
            //return processName + ".n"; // IMPOSSIBLE RETURN ACTUALLY
        } else {
            // must be 1
            List<DependencyNode> nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodes.size() == 0) {
                nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            strB.append(nodes.get(0).getForm() + "_" + nodes.get(0).getCpos());
            //return nodes.get(0).getForm() + ".n";

        }
        return strB.toString().trim();
    }

    public String getLexicalUnitFormFrame() {
        String[] lexUnitFrame = processName.trim().split("\\s+");
        if (lexUnitFrame.length > 1) {
            List<DependencyNode> nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodeCandidates.size() == 0) {
                nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            for (int i = 0; i < nodeCandidates.size(); i++) {
                DependencyNode currentFirst = nodeCandidates.get(i);
                if (currentFirst.getId() + 1 <= depTree.size() && depTree.get(currentFirst.getId() + 1).getForm().equalsIgnoreCase(lexUnitFrame[1])) {
                    return currentFirst.getForm() + " " + depTree.get(currentFirst.getId() + 1).getForm() + ".n";
                }
            }
            return processName + ".n"; // IMPOSSIBLE RETURN ACTUALLY
        } else {
            // must be 1
            List<DependencyNode> nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodes.size() == 0) {
                nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            return nodes.get(0).getForm() + ".n";
        }
    }

    public Pair<List<String>,List<Integer>> getTargetLexAndIdxs() {
        ArrayList<String> targetTokens = Lists.newArrayList();
        ArrayList<Integer> targetTokenIdxs = Lists.newArrayList();
        String[] lexUnitFrame = processName.trim().split("\\s+");
        if (lexUnitFrame.length > 1) {
            List<DependencyNode> nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodeCandidates.size() == 0) {
                nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            for (int i = 0; i < nodeCandidates.size(); i++) {
                DependencyNode currentFirst = nodeCandidates.get(i);
                if (currentFirst.getId() + 1 <= depTree.size() && depTree.get(currentFirst.getId() + 1).getForm().equalsIgnoreCase(lexUnitFrame[1])) {
                    targetTokens.add(currentFirst.getForm());
                    targetTokens.add(depTree.get(currentFirst.getId() + 1).getForm());
                    targetTokenIdxs.add(currentFirst.getId());
                    targetTokenIdxs.add(currentFirst.getId()+1);
                    break;
                }
            }
        } else {
            // must be 1
            List<DependencyNode> nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodes.size() == 0) {
                nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            targetTokens.add(nodes.get(0).getForm());
            targetTokenIdxs.add(nodes.get(0).getId());
        }
        return new Pair<>(targetTokens,targetTokenIdxs);
    }

    public void setAnnotatedArgumentSpanJSON(ArrayList<ArgumentSpan> annotatedArgumentSpanJSON) {
        this.annotatedArgumentSpanJSON = annotatedArgumentSpanJSON;
    }

    public ArrayList<ArgumentSpan> getPredictedArgumentSpanJSON() {
        return predictedArgumentSpanJSON;
    }

    public void setPredictedArgumentSpanJSON(ArrayList<ArgumentSpan> predictedArgumentSpanJSON) {
        this.predictedArgumentSpanJSON = predictedArgumentSpanJSON;
    }

    public void setIdArgumentSpans() {
        getAllAnnotatedArgumentSpan();
    }

    public boolean isAnnotated() {
        return annotated;
    }

    public void setAnnotated(boolean annotated) {
        this.annotated = annotated;
    }

    public int getId() {
        return id;
    }

    public HashMap<String, ArrayList<ArgumentSpan>> getRoleArgPropBank() {
        return roleArgPropBank;
    }

    public void setRoleArgPropBank(HashMap<String, ArrayList<ArgumentSpan>> roleArgPropBank) {
        this.roleArgPropBank = roleArgPropBank;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProcess() {
        return processName;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public HashMap<String, ArrayList<ArgumentSpan>> getRoleArgAnnotation() {
        return roleArgAnnotation;
    }

    public void setRoleArgAnnotation(HashMap<String, ArrayList<ArgumentSpan>> roleArgAnnotation) {
        this.roleArgAnnotation = roleArgAnnotation;
    }

    public HashMap<String, ArrayList<ArgumentSpan>> getRoleArgPrediction() {
        return roleArgPrediction;
    }

    public void setRoleArgPrediction(HashMap<String, ArrayList<ArgumentSpan>> roleArgPrediction) {
        this.roleArgPrediction = roleArgPrediction;
    }

    public void setProcess(String process) {
        this.processName = process;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Sentence(String rawText) throws IOException {
        depTree = StanfordDepParserSingleton.getInstance().parse(rawText);
        this.rawText = rawText;
    }

    public int getNbToken() {
        return depTree.size() - 1;
    }

    public String toString() {
        ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
        for (String role : roleArgAnnotation.keySet()) {
            ArrayList<ArgumentSpan> tempSpan = roleArgAnnotation.get(role);
            for (int j = 0; j < tempSpan.size(); j++) {
                if (tempSpan.get(j).getAnnotatedLabel().equalsIgnoreCase("1")) {
                    spans.add(tempSpan.get(j));
                }
            }
        }

        StringBuffer strBuff = new StringBuffer();
        strBuff.append("Sentence [");
        strBuff.append("id=" + getId() + ",");
        for (ArgumentSpan span : spans) {
            strBuff.append(span);
        }
        strBuff.append("]");

        return strBuff.toString();
    }

    // index on dependency node. index 0 is ROOT
    // this is assuming only one role label for a token
    // if there is no label return NULL;
    public String getRoleLabel(int idx) {
        for (String roleName : roleArgAnnotation.keySet()) {
            ArrayList<ArgumentSpan> spans = roleArgAnnotation.get(roleName);
            for (ArgumentSpan span : spans) {
                if (span.argNodes.stream().anyMatch(node -> node.getId() == idx)) {
                    return roleName;
                }
            }
        }

        return null;
    }

    public ArrayList<Integer> getRoleIdx(String targetRoleName) {
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        for (String roleName : roleArgAnnotation.keySet()) {
            if (roleName.equalsIgnoreCase(targetRoleName)) {
                ArrayList<ArgumentSpan> spans = roleArgAnnotation.get(roleName);
                for (ArgumentSpan span : spans) {
                    for (DependencyNode node : span.getArgNodes()) {
                        idxs.add(node.getId());
                    }
                }
            }
        }
        return idxs;
    }

    public ArrayList<String> getUniqueRoleLabel(int idx) {
        ArrayList<String> roleNames = new ArrayList<String>();
        for (String roleName : roleArgAnnotation.keySet()) {
            ArrayList<ArgumentSpan> spans = roleArgAnnotation.get(roleName);
            for (ArgumentSpan span : spans) {
                if (span.argNodes.stream().anyMatch(node -> node.getId() == idx)) {
                    if (!roleNames.contains(roleName)) {
                        roleNames.add(roleName);
                    }
                }
            }
        }
        return roleNames;
    }

    public ArrayList<String> getAllUniqueRoleLabel() {
        ArrayList<String> roleNames = new ArrayList<String>();
        for (String roleName : roleArgAnnotation.keySet()) {
            if (!roleNames.contains(roleName)) {
                roleNames.add(roleName);
            }
        }
        return roleNames;
    }

    /*public ArrayList<ArgumentSpan> getAnnotatedArgumentSpan(String roleType) {
     return roleArgAnnotation.get(roleType);
     }*/

    /*public ArrayList<ArgumentSpan> getAllAnnotatedArgumentSpan() {
     ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
     for (String roleName : roleArgAnnotation.keySet()) {
     spans.addAll(roleArgAnnotation.get(roleName));
     }

     return spans;
     }*/
    public ArrayList<ArgumentSpan> getAnnotatedArgumentSpan(String roleType) {
        ArrayList<ArgumentSpan> spans = roleArgAnnotation.get(roleType);
        ArrayList<ArgumentSpan> annotatedSpan = new ArrayList<ArgumentSpan>();
        for (int i = 0; i < spans.size(); i++) {
            if (spans.get(i).getAnnotatedLabel().equalsIgnoreCase("1") || spans.get(i).getAnnotatedLabel().equalsIgnoreCase("-1")) {
                annotatedSpan.add(spans.get(i));
            }
        }

        return annotatedSpan;
    }

    public ArrayList<ArgumentSpan> getMultiClassAnnotatedArgumentSpan(String roleType, int totalClass) {
        if (roleType.equalsIgnoreCase("NONE")) {
            ArrayList<ArgumentSpan> annotatedSpan = getAllAnnotatedArgumentSpan();
            ArrayList<ArgumentSpan> noneSpan = new ArrayList<ArgumentSpan>();
            annotatedSpan = (ArrayList<ArgumentSpan>) annotatedSpan.stream().filter(s -> s.getAnnotatedLabel().equalsIgnoreCase("-1")).collect(Collectors.toList());
            Map<String, List<ArgumentSpan>> noneSpanCandidates = annotatedSpan.stream().collect(Collectors.groupingBy(s -> s.getStartIdx() + "_" + s.getEndIdx()));
            for (String key : noneSpanCandidates.keySet()) {
                if (noneSpanCandidates.get(key).size() == totalClass) {
                    noneSpan.addAll(noneSpanCandidates.get(key));
                }
            }

            return noneSpan;
        } else {
            ArrayList<ArgumentSpan> annotatedSpan = getAnnotatedArgumentSpan(roleType);
            annotatedSpan = (ArrayList<ArgumentSpan>) annotatedSpan.stream().filter(s -> s.getAnnotatedLabel().equalsIgnoreCase("1")).collect(Collectors.toList());
            return annotatedSpan;
        }
    }

    public ArrayList<ArgumentSpan> getAllIdentifiedAnnotatedArgumentSpan() {
        ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
        for (String role : roleArgAnnotation.keySet()) {
            ArrayList<ArgumentSpan> tempSpan = roleArgAnnotation.get(role);
            for (int j = 0; j < tempSpan.size(); j++) {
                if (tempSpan.get(j).getAnnotatedLabel().equalsIgnoreCase("1")) {
                    spans.add(tempSpan.get(j));
                }
            }
        }
        //spans.addAll(getAllNONEArgumentSpan());

        return spans;
    }

    /* Training instances */
    public ArrayList<ArgumentSpan> getAllAnnotatedArgumentSpan() {
        ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
        for (String role : roleArgAnnotation.keySet()) {
            ArrayList<ArgumentSpan> tempSpan = roleArgAnnotation.get(role);
            for (int j = 0; j < tempSpan.size(); j++) {
                if (tempSpan.get(j).getAnnotatedLabel().equalsIgnoreCase("1") || tempSpan.get(j).getAnnotatedLabel().equalsIgnoreCase("-1")) {
                    spans.add(tempSpan.get(j));
                }
            }
        }

        return spans;
    }

    public HashMap<String, String> getAllArgumentsThatHaveAnnotation() {
        HashMap<String, String> pairs = new HashMap<String, String>();
        for (String role : roleArgAnnotation.keySet()) {
            ArrayList<ArgumentSpan> tempSpan = roleArgAnnotation.get(role);
            for (int j = 0; j < tempSpan.size(); j++) {
                if (tempSpan.get(j).getAnnotatedLabel().equalsIgnoreCase("1")) {
                    pairs.put(getId() + "_" + tempSpan.get(j).getStartIdx() + "_" + tempSpan.get(j).getEndIdx(), "");
                }
            }
        }
        ArrayList<ArgumentSpan> noneCandidates = new ArrayList<ArgumentSpan>();
        for (String role : roleArgAnnotation.keySet()) {
            ArrayList<ArgumentSpan> tempSpan = roleArgAnnotation.get(role);
            for (int j = 0; j < tempSpan.size(); j++) {
                if (tempSpan.get(j).getAnnotatedLabel().equalsIgnoreCase("-1")) {
                    noneCandidates.add(tempSpan.get(j));
                }
            }
        }
        Map<String, List<ArgumentSpan>> candidateSpans = noneCandidates.stream().collect(Collectors.groupingBy(s -> s.getStartIdx() + "_" + s.getEndIdx()));
        for (String startEndID : candidateSpans.keySet()) {
            List<ArgumentSpan> spans = candidateSpans.get(startEndID);
            if (spans.stream().collect(Collectors.groupingBy(ArgumentSpan::getAnnotatedRole)).keySet().size() == 4) {
                pairs.put(getId() + "_" + startEndID, "");
            }
        }

        return pairs;
    }

    public ArrayList<ArgumentSpan> getAllUniqueAnnotatedArgumentSpan() {
        ArrayList<ArgumentSpan> uniqueSpans = new ArrayList<ArgumentSpan>();
        ArrayList<ArgumentSpan> allAnnotatedSpans = getAllAnnotatedArgumentSpan();
        for (ArgumentSpan span : allAnnotatedSpans) {
            if (!uniqueSpans.contains(span)) {
                uniqueSpans.add(span);
            }
        }
        return uniqueSpans;
    }

    /*
     Obtain unique argument spans from the prediction, if there are same argument spans then 
     select the max
     */
    public ArrayList<ArgumentSpan> getAllPredictedArgumentSpan() {
        ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
        if (roleArgPrediction == null) {
            return null;
        }
        for (String role : roleArgPrediction.keySet()) {
            ArrayList<ArgumentSpan> tempSpan = roleArgPrediction.get(role);
            for (int j = 0; j < tempSpan.size(); j++) {
                spans.add(tempSpan.get(j));
            }
        }

        ArrayList<ArgumentSpan> uniqueSpan = new ArrayList<ArgumentSpan>();
        Map<String, List<ArgumentSpan>> argStartEndPair = spans.stream().collect(Collectors.groupingBy(s -> s.getStartIdx() + "_" + s.getEndIdx()));
        for (String key : argStartEndPair.keySet()) {
            ArgumentSpan bestArg = (ArgumentSpan) FileUtil.deepClone(argStartEndPair.get(key).stream().max(Comparator.comparing(arg -> arg.getMaxProbScore())).get());
            // clone the object
            bestArg.setAnnotatedLabel("");
            bestArg.setAnnotatedLabel("");
            uniqueSpan.add(bestArg);
        }
        return uniqueSpan;
    }

    public DependencyTree getDepTree() {
        return depTree;
    }

    public void clearRoleFiller(String roleName) {
        roleArgAnnotation.put(roleName, new ArrayList<ArgumentSpan>());
    }

    public static void main(String[] args) {

    }

    /* Will return unique argument span for GOLD */
    public ArrayList<ArgumentSpan> getAllAnnotatedArgumentSpanFromJSON(String role) {
        ArrayList<ArgumentSpan> ret = new ArrayList<ArgumentSpan>();
        if (role.equalsIgnoreCase("NONE")) {
            Map<String, List<ArgumentSpan>> temp2 = annotatedArgumentSpanJSON.stream().filter(arg -> arg.getAnnotatedLabel().equalsIgnoreCase("-1"))
                    .collect(Collectors.groupingBy(a -> a.getStartIdxJSON() + "_" + a.getEndIdxJSON()));
            for (String keyStartEndId : temp2.keySet()) {
                List<ArgumentSpan> spans = temp2.get(keyStartEndId);
                int cnt = spans.stream().collect(Collectors.groupingBy(ArgumentSpan::getAnnotatedRole)).keySet().size();
                if (cnt >= 4) {
                    ret.add(spans.get(0));
                }
            }
        } else {
            ArrayList<ArgumentSpan> temp = (ArrayList<ArgumentSpan>) annotatedArgumentSpanJSON.stream().filter(arg -> arg.getAnnotatedRole().equalsIgnoreCase(role) && arg.getAnnotatedLabel().equalsIgnoreCase("1")).collect(Collectors.toList());
            Map<String, List<ArgumentSpan>> temp2 = temp.stream().collect(Collectors.groupingBy(a -> a.getStartIdxJSON() + "_" + a.getEndIdxJSON()));
            for (String startEndId : temp2.keySet()) {
                ret.add(temp2.get(startEndId).get(0));
            }
        }

        return ret;
    }

    public String getLexicalUnitFrameRange() {
        String[] lexUnitFrame = processName.trim().split("\\s+");
        if (lexUnitFrame.length > 1) {
            List<DependencyNode> nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodeCandidates.size() == 0) {
                nodeCandidates = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            for (int i = 0; i < nodeCandidates.size(); i++) {
                DependencyNode currentFirst = nodeCandidates.get(i);
                if (currentFirst.getId() + 1 <= depTree.size() && depTree.get(currentFirst.getId() + 1).getForm().equalsIgnoreCase(lexUnitFrame[1])) {
                    return currentFirst.getId() - 1 + "_" + currentFirst.getId();
                }
            }
            return "1"; // IMPOSSIBLE RETURN ACTUALLY
        } else {
            // must be 1
            List<DependencyNode> nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().equalsIgnoreCase(lexUnitFrame[0])).collect(Collectors.toList());
            if (nodes.size() == 0) {
                nodes = (List<DependencyNode>) depTree.values().stream().filter(n -> n.getForm().contains(lexUnitFrame[0])).collect(Collectors.toList());
            }
            return String.valueOf(nodes.get(0).getId() - 1);

        }
    }
}
