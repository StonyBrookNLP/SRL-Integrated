/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import Util.GlobalV;
import Util.LibSVMUtil;
import qa.util.FileUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import qa.ProcessFrame;
import qa.StanfordDepParserSingleton;
import qa.StanfordLemmatizerSingleton;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;

/**
 *
 * @author slouvan
 */
public class FeatureExtractor implements Serializable {

    HashMap<String, HashMap<String, Integer>> featureIndexPair = new HashMap<String, HashMap<String, Integer>>();
    HashMap<String, HashMap<String, Integer>> featureValueCountPair = new HashMap<String, HashMap<String, Integer>>();
    HashMap<String, Integer> wordIndexPair = new HashMap<String, Integer>();
    ArrayList<String> features = new ArrayList<String>();
    ArrayList<String> featureVectors = new ArrayList<String>();
    transient FrameNetFeatureExtractor fNetExtractor;
    boolean frameBuilt = false;

    public void readFeatureFile(String fileName) throws FileNotFoundException {
        String featureNames[] = FileUtil.readLinesFromFile(fileName);
        for (String featureName : featureNames) {
            if (!featureName.startsWith("#")) {
                // TODO : tidy up this to one method
                if (featureName.contains("frame") && !frameBuilt) {
                    buildFrame();
                    featureValueCountPair.put(featureName, new HashMap<String, Integer>());
                    if (!features.contains("frame_left")) {
                        features.add("frame_left");
                    }
                    if (!features.contains("frame_right")) {
                        features.add("frame_right");
                    }
                } else {
                    featureIndexPair.put(featureName, new HashMap<String, Integer>());
                    featureValueCountPair.put(featureName, new HashMap<String, Integer>());
                    if (!features.contains(featureName)) {
                        features.add(featureName);
                    }
                }
            }
        }
    }

    void buildFrame() {
        // read frame files 
        featureIndexPair.put("frame_left", new HashMap<String, Integer>());
        featureIndexPair.put("frame_right", new HashMap<String, Integer>());

        HashMap<String, Integer> fNameIndexPairLeft = new HashMap<String, Integer>();
        HashMap<String, Integer> fNameIndexPairRight = new HashMap<String, Integer>();
        fNetExtractor = new FrameNetFeatureExtractor();
        String[] fNames = fNetExtractor.getAllFrames();
        int cnt = 1;
        for (String fName : fNames) {
            fNameIndexPairLeft.put(fName, cnt);
            fNameIndexPairRight.put(fName, cnt);
            cnt++;
        }
        featureIndexPair.put("frame_left", fNameIndexPairLeft);
        featureIndexPair.put("frame_right", fNameIndexPairRight);
    }

    // TODO 
    void buildTokens(ArrayList<ProcessFrame> procFrameArr) {
        for (int i = 0; i < procFrameArr.size(); i++) {
            ProcessFrame pFrame = procFrameArr.get(i);
            List<String> tokens = StanfordTokenizerSingleton.getInstance().tokenize(pFrame.getRawText());
            for (int j = 0; j < tokens.size(); j++) {
                if (!wordIndexPair.containsKey(tokens.get(j))) {
                    wordIndexPair.put(tokens.get(j), wordIndexPair.size());
                }
            }
        }
    }

    public void extractFeature(ProcessFrame p, String targetRole, String annotationInfo) throws IOException {
        for (String featureName : features) {
            extractFeature(featureName, p, targetRole, annotationInfo);
        }
    }

    public void extractFeature(String featureName, ProcessFrame p, String targetRole, String annotationInfo) throws IOException {
        ArrayList<Integer> tokenIdx = p.getRoleIdx(targetRole);
        DependencyTree depTree = StanfordDepParserSingleton.getInstance().parse(p.getRawText());
        // Check ada common ancestor yang berada di tokenIdx gak
        DependencyNode headNode = depTree.getHeadNode(tokenIdx);
        // jika ada jadiin anchor
        // kalau gak ada maka di antara tokenIdx, cari yang paling banyak anaknya
        if (headNode == null) {
            System.out.println("No head node");
            System.out.println("Raw text : " + p.getRawText() + " Target role :" + targetRole + " Role filler : " + p.getRoleFiller(targetRole));
            System.out.println("");
        } else {
            //System.out.println("Head node exist");
        }

        //System.out.println(tokenIdx);
        //if (annotationInfo.split("\t")[11].equalsIgnoreCase("0")) {
        //for (int i = 0; i < tokenIdx.size(); i++) {
        switch (featureName) {
            case "process_name":
                String featValues = p.getProcessName();
                updateFeatureHashMap(featureName, featValues);
                break;

            case "pattern":
                featValues = annotationInfo.split("\t")[1];
                updateFeatureHashMap(featureName, featValues);
                break;

            case "lemma":
                featValues = headNode.getLemma();//depTree.get(tokenIdx.get(i)).getLemma();
                //System.out.println(featValues);
                updateFeatureHashMap(featureName, featValues);
                break;

            case "pos":
                featValues = headNode.getCpos();//depTree.get(tokenIdx.get(i)).getCpos();
                updateFeatureHashMap(featureName, featValues);
                break;

            case "parent_word":
                DependencyNode currentNode = headNode;//depTree.get(tokenIdx.get(i));
                DependencyNode parentNode = depTree.get(currentNode.getHeadID());//depTree.get(currentNode.getHeadID());
                featValues = parentNode.getLemma();
                
                //System.out.println("PARENT WORD : "+featValues);
                updateFeatureHashMap(featureName, featValues);
                break;
            case "parent_pos":
                currentNode = headNode;//depTree.get(tokenIdx.get(i));
                parentNode = depTree.get(currentNode.getHeadID());
                featValues = parentNode.getCpos();
                updateFeatureHashMap(featureName, featValues);
                break;

            case "dep_rel_to_process_name":
                String[] processNames = p.getProcessName().split("\\s");
                String processName = "";
                if (processNames.length > 1) {
                    processName = processNames[processNames.length - 1];
                } else {
                    processName = p.getProcessName();
                }
                currentNode = headNode;//depTree.get(tokenIdx.get(i));
                DependencyNode targetNode = depTree.getWordDepNode(processName);
                if (targetNode == null) {
                    featValues = "";
                    System.out.println("DOES NOT EXIST");
                } else {
                    //System.out.println(p.getRawText());
                    featValues = depTree.getDepRelPath(currentNode, targetNode);
                    //System.out.println(featValues);
                }
                updateFeatureHashMap(featureName, featValues);
                break;

            case "pos_path_to_process_name":
                processNames = p.getProcessName().split("\\s");
                processName = "";
                if (processNames.length > 1) {
                    processName = processNames[processNames.length - 1];
                } else {
                    processName = p.getProcessName();
                }
                currentNode = headNode;//depTree.get(tokenIdx.get(i));
                targetNode = depTree.getWordDepNode(processName);
                if (targetNode == null) {
                    featValues = "";
                    //System.out.println("DOES NOT EXIST");
                } else {
                    //System.out.println(p.getRawText());
                    featValues = depTree.getPOSPath(currentNode, targetNode);
                    //System.out.println(featValues);
                }
                updateFeatureHashMap(featureName, featValues);
                break;

            case "child_word_set":
                Set<String> childWordSet = depTree.getChildWordSet(headNode);//depTree.getChildWordSet(depTree.get(tokenIdx.get(i)));
                for (String childWord : childWordSet) {
                    //System.out.println(childWord);
                    updateFeatureHashMap(featureName, childWord);
                }
                break;
            case "child_pos_set":
                Set<String> childPOSSet = depTree.getChildPOSSet(headNode);//depTree.getChildPOSSet(depTree.get(tokenIdx.get(i)));
                for (String childPOS : childPOSSet) {
                    // System.out.println(childPOS);
                    updateFeatureHashMap(featureName, childPOS);

                }
                break;
            case "child_dep_set":
                Set<String> childDepRelSet = depTree.getChildDEPSet(headNode);
                for (String childDepRel : childDepRelSet) {
                    //System.out.println(childDepRel);
                    updateFeatureHashMap(featureName, childDepRel);
                }
                break;
            case "frame_left":
                // go to the left, find a verb, 
                //String lemmaVerb = depTree.getLemmaVerb(true, 3, tokenIdx.get(i));
                String lemmaVerb = depTree.getLemmaVerb(true, 3, headNode.getId());
                if (fNetExtractor == null) {
                    fNetExtractor = new FrameNetFeatureExtractor();
                }
                String[] framesInvoked = fNetExtractor.getFrame(lemmaVerb + ".v");

                if (!lemmaVerb.isEmpty() && framesInvoked != null) {
                    for (String fName : framesInvoked) {
                        if (fName.equalsIgnoreCase("Others_situation_as_stimulus")) {
                            System.out.println(lemmaVerb);
                        }
                        updateFeatureHashMap(featureName, fName);
                    }
                }
         // get the frame

                // updateHashMap
                break;
            case "frame_right":
         // go to the right, find a verb, 
                // get the frame
                // updateHashMap  
                //lemmaVerb = depTree.getLemmaVerb(false, 3, tokenIdx.get(i));
                lemmaVerb = depTree.getLemmaVerb(false, 3, headNode.getId());
                
                if (fNetExtractor == null) {
                    fNetExtractor = new FrameNetFeatureExtractor();
                }
                framesInvoked = fNetExtractor.getFrame(lemmaVerb + ".v");
                if (!lemmaVerb.isEmpty() && framesInvoked != null) {
                    for (String fName : framesInvoked) {
                        updateFeatureHashMap(featureName, fName);
                    }
                }
                break;

        }
        // }
        //}
    }

    public void updateFeatureHashMap(String featureName, String featureValue) {
        HashMap<String, Integer> featValueIndexPair = featureIndexPair.get(featureName);
        HashMap<String, Integer> featValueCountPair = featureValueCountPair.get(featureName);

        if (!featValueIndexPair.containsKey(featureValue)) {
            if (featureName.equalsIgnoreCase("frame_left")) {
                System.out.println("DOUBLE UPDATE");
            }
            featValueIndexPair.put(featureValue, featValueIndexPair.size() + 1);
        }
        if (!featValueCountPair.containsKey(featureValue)) {
            featValueCountPair.put(featureValue, 1);
        } else {
            featValueCountPair.put(featureValue, featValueCountPair.get(featureValue) + 1);
        }

        featureIndexPair.put(featureName, featValueIndexPair);
        featureValueCountPair.put(featureName, featValueCountPair);
    }

    public void extractFeatureVector(ProcessFrame p, String targetRole, String annotationInfo) throws IOException {
        ArrayList<Integer> tokenIdx = p.getRoleIdx(targetRole);
        for (int i = 0; i < tokenIdx.size(); i++) {
            String featVector = extractFeatureVectorValue(tokenIdx.get(i), p, targetRole, annotationInfo, true);
            featureVectors.add(featVector);
        }
    }

    public void updateFeatureVector(StringBuilder featVector, int offset, String featValues, String featureName) {
        if (featureIndexPair.get(featureName).get(featValues) != null) {
            int featIdx = featureIndexPair.get(featureName).get(featValues);
            featVector.append(" ").append(offset + featIdx + ":1");
        }
    }

    public boolean isAnnotationExist(String targetRole, String annotationInfo) {
        try {
            if (targetRole.equalsIgnoreCase("A0")) {
                int label = Integer.parseInt(annotationInfo.split("\t")[9]);
            } else if (targetRole.equalsIgnoreCase("A1")) {
                int label = Integer.parseInt(annotationInfo.split("\t")[10]);
            } else if (targetRole.equalsIgnoreCase("A2")) {
                String[] columns = annotationInfo.split("\t");
                int x = 0;
                int label = Integer.parseInt(annotationInfo.split("\t")[12]);
            } else if (targetRole.equalsIgnoreCase("A3")) {
            } else {
                // Trigger
                int label = Integer.parseInt(annotationInfo.split("\t")[11]);
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private int getClassLabel(String targetRole, String annotationInfo) {
        if (targetRole.equalsIgnoreCase("A0")) {
            int label = Integer.parseInt(annotationInfo.split("\t")[9]);
            return (label == 1) ? 1 : -1;
        } else if (targetRole.equalsIgnoreCase("A1")) {
            int label = Integer.parseInt(annotationInfo.split("\t")[10]);
            return (label == 1) ? 1 : -1;
        } else if (targetRole.equalsIgnoreCase("A2")) {
            String[] columns = annotationInfo.split("\t");
            int x = 0;
            int label = Integer.parseInt(annotationInfo.split("\t")[12]);
            return (label == 1) ? 1 : -1;
        } else if (targetRole.equalsIgnoreCase("A3")) {
            return 0;
        } else {
            // Trigger
            int label = Integer.parseInt(annotationInfo.split("\t")[11]);
            return (label == 1) ? 1 : -1;
        }
    }

    public void dumpFeatureVectors(String fileName) throws FileNotFoundException {
        FileUtil.dumpToFile(featureVectors, fileName, "");
    }

    public void dumpFeaturesIndex(String fileName) throws FileNotFoundException {
        int offset = 0;
        PrintWriter writer = new PrintWriter(fileName);
        for (int i = 0; i < features.size(); i++) {
            String featureName = features.get(i);
            HashMap<String, Integer> featValueIndex = featureIndexPair.get(featureName);
            Map<String, Integer> sortedMap
                    = featValueIndex.entrySet().stream()
                    .sorted(Entry.comparingByValue())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            for (String keySet : sortedMap.keySet()) {
                System.out.println((offset + sortedMap.get(keySet)) + keySet + "\t" + featureName);
                writer.println((offset + sortedMap.get(keySet)) + "\t" + keySet + "\t" + featureName);
            }
            offset += featureIndexPair.get(featureName).size();
        }
        writer.close();
    }

    public String extractFeatureVectorValue(int tokenIdx, ProcessFrame p, String targetRole, String annotationInfo, boolean isLabelAvailable) throws IOException {
        DependencyTree depTree = StanfordDepParserSingleton.getInstance().parse(p.getRawText());
        StringBuilder featStr = new StringBuilder();
        int offset = 0;
        int classLabel = isLabelAvailable ? getClassLabel(targetRole, annotationInfo) : -10;
        for (int i = 0; i < features.size(); i++) {
            String featureName = features.get(i);
            switch (featureName) {
                case "process_name":
                    String featValues = p.getProcessName();
                    updateFeatureVector(featStr, offset, featValues, features.get(i));
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;

                case "pattern":
                    featValues = annotationInfo.split("\t")[1];
                    updateFeatureVector(featStr, offset, featValues, features.get(i));
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;

                case "lemma":
                    featValues = depTree.get(tokenIdx).getLemma();
                    updateFeatureVector(featStr, offset, featValues, features.get(i));
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;

                case "pos":
                    featValues = depTree.get(tokenIdx).getCpos();
                    updateFeatureVector(featStr, offset, featValues, features.get(i));
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;

                case "parent_word":
                    DependencyNode currentNode = depTree.get(tokenIdx);
                    DependencyNode parentNode = depTree.get(currentNode.getHeadID());
                    featValues = parentNode.getLemma();
                    System.out.println("PARENT " + featValues);
                    updateFeatureVector(featStr, offset, featValues, features.get(i));
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;

                case "parent_pos":
                    currentNode = depTree.get(tokenIdx);
                    parentNode = depTree.get(currentNode.getHeadID());
                    featValues = parentNode.getCpos();
                    updateFeatureVector(featStr, offset, featValues, features.get(i));
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;

                case "dep_rel_to_process_name":
                    String[] processNames = p.getProcessName().split("\\s");
                    String processName = "";
                    if (processNames.length > 1) {
                        processName = processNames[processNames.length - 1];
                    } else {
                        processName = p.getProcessName();
                    }
                    currentNode = depTree.get(tokenIdx);
                    DependencyNode targetNode = depTree.getWordDepNode(processName);
                    if (targetNode == null) {
                        featValues = "";
                        System.out.println("DOES NOT EXIST");
                    } else {
                        System.out.println(p.getRawText());
                        featValues = depTree.getDepRelPath(currentNode, targetNode);
                        System.out.println(featValues);
                    }
                    updateFeatureVector(featStr, offset, featValues, features.get(i));
                    offset += featureIndexPair.get(features.get(i)).size();

                    break;

                case "pos_path_to_process_name":
                    processNames = p.getProcessName().split("\\s");
                    processName = "";
                    if (processNames.length > 1) {
                        processName = processNames[processNames.length - 1];
                    } else {
                        processName = p.getProcessName();
                    }
                    currentNode = depTree.get(tokenIdx);
                    targetNode = depTree.getWordDepNode(processName);
                    if (targetNode == null) {
                        featValues = "";
                        System.out.println("DOES NOT EXIST");
                    } else {
                        System.out.println(p.getRawText());
                        featValues = depTree.getPOSPath(currentNode, targetNode);
                        System.out.println(featValues);
                    }
                    updateFeatureVector(featStr, offset, featValues, features.get(i));
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;

                case "child_word_set":
                    Set<String> childWordSet = depTree.getChildWordSet(depTree.get(tokenIdx));
                    for (String childWord : childWordSet) {
                        updateFeatureVector(featStr, offset, childWord, features.get(i));
                        //updateFeatureHashMap(featureName, childWord);
                    }
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;
                case "child_pos_set":
                    Set<String> childPOSSet = depTree.getChildPOSSet(depTree.get(tokenIdx));
                    for (String childPOS : childPOSSet) {
                        updateFeatureVector(featStr, offset, childPOS, features.get(i));
                    }
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;
                case "child_dep_set":
                    Set<String> childDepRelSet = depTree.getChildDEPSet(depTree.get(tokenIdx));
                    for (String childDepRel : childDepRelSet) {
                        updateFeatureVector(featStr, offset, childDepRel, features.get(i));
                    }
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;
                case "frame_left":
                    String lemmaVerb = depTree.getLemmaVerb(true, 3, tokenIdx);
                    if (fNetExtractor == null) {
                        fNetExtractor = new FrameNetFeatureExtractor();
                    }
                    String[] framesInvoked = fNetExtractor.getFrame(lemmaVerb + ".v");
                    if (!lemmaVerb.isEmpty() && framesInvoked != null) {
                        for (String fName : framesInvoked) {
                            updateFeatureVector(featStr, offset, fName, features.get(i));
                        }
                    }
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;
                case "frame_right":
                    lemmaVerb = depTree.getLemmaVerb(false, 3, tokenIdx);
                    if (fNetExtractor == null) {
                        fNetExtractor = new FrameNetFeatureExtractor();
                    }
                    framesInvoked = fNetExtractor.getFrame(lemmaVerb + ".v");
                    if (!lemmaVerb.isEmpty() && framesInvoked != null) {
                        for (String fName : framesInvoked) {
                            updateFeatureVector(featStr, offset, fName, features.get(i));
                        }
                    }
                    offset += featureIndexPair.get(features.get(i)).size();
                    break;
            }
        }

        return String.valueOf(classLabel) + " " + LibSVMUtil.sortIndex(featStr.toString());
    }

    public static void main(String[] args) throws FileNotFoundException {
        // Test read
        new FeatureExtractor().readFeatureFile("./configSBUProcRel/features");
    }

}
