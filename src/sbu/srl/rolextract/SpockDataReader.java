/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbu.srl.rolextract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import qa.StanfordDepParserSingleton;
import qa.StanfordTokenizerSingleton;
import qa.dep.DependencyNode;
import qa.dep.DependencyTree;
import qa.util.FileUtil;
import sbu.srl.datastructure.ArgProcessAnnotationData;
import sbu.srl.datastructure.ArgumentSpan;
import sbu.srl.datastructure.Sentence;

/**
 *
 * @author slouvan
 */
// FROM BRAT AND GOOGLE SHEET IDEALLY
// For now, from Google Sheet
public class SpockDataReader {

    public HashMap<String, String> fieldMap = new HashMap<String, String>();
    HashMap<String, Integer> fieldIdxMap = new HashMap<String, Integer>();
    String processFileName;
    ArrayList<Sentence> sentences = new ArrayList<Sentence>();
    boolean skipNotAnnotated = true;

    public SpockDataReader(String processFileName, String configFileName) throws FileNotFoundException {
        this.processFileName = processFileName;
        readConfig(configFileName);
    }

    public SpockDataReader() {

    }

    public void readConfig(String configFileName) throws FileNotFoundException {
        String[] fields = FileUtil.readLinesFromFile(configFileName);
        for (int i = 0; i < fields.length; i++) {
            fieldMap.put(fields[i].split("\t")[0], fields[i].split("\t")[1]);
        }
    }

    private void mapFieldIdx(String fileHeader[]) {
        for (int i = 0; i < fileHeader.length; i++) {
            fieldIdxMap.put(fileHeader[i], i);
        }
    }

    public static ArrayList<Integer> getIdxMatchesv2(String[] targetPattern, String[] tokenizedSentence) {
        boolean inRegion = false;
        int matchStart = 0;
        int matchEnd = targetPattern.length;
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < tokenizedSentence.length && matchStart < matchEnd; i++) {
            if (tokenizedSentence[i].equalsIgnoreCase(targetPattern[matchStart])) {//&& !idxs.contains(i + 1)) {
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

    public Set<String> getRoleLabels() {
        ArrayList<String> roleLabels = new ArrayList<String>();
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sent = sentences.get(i);
            roleLabels.addAll(sent.getAllUniqueRoleLabel());
        }
        Set<String> uniqueRoleLabels = new HashSet<String>(roleLabels);

        return uniqueRoleLabels;
    }

    public void dumpData(String fileName) throws FileNotFoundException
    {

    }
    public void readData() throws FileNotFoundException, IOException {
        List<String[]> data = new ArrayList<>();
        data = FileUtil.readDataObject(processFileName, "\t");
        mapFieldIdx(data.get(0));
        data = data.subList(1, data.size());
        final Map<String, List<String[]>> sentenceMap = data.stream().collect(Collectors.groupingBy(row -> row[fieldIdxMap.get("sentence")]));
        String roles[] = fieldMap.get("role").split(":");

        for (String sentenceStr : sentenceMap.keySet()) {
            Sentence sentence = new Sentence(sentenceStr);
            boolean isAnnotated = false;
            sentence.setRawText(sentenceStr);
            sentence.setProcess(sentenceMap.get(sentenceStr).get(0)[fieldIdxMap.get("process")]);
            HashMap<String, ArrayList<ArgumentSpan>> roleAnnotationSpan = new HashMap<String, ArrayList<ArgumentSpan>>();
            for (String[] sentenceData : sentenceMap.get(sentenceStr)) {
                // Adding argument span here
                for (int i = 0; i < roles.length; i++) {
                    int roleColumnIdx = fieldIdxMap.get(roles[i]);
                    ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
                    if (sentenceData[roleColumnIdx].length() > 0) { // IF role filler is not empty
                        // Set role filler
                        List<String> tokens = StanfordTokenizerSingleton.getInstance().tokenize(sentenceData[roleColumnIdx].trim());
                        List<String> tokenizedRawText = StanfordTokenizerSingleton.getInstance().tokenize(sentence.getRawText());
                        String[] pattern = new String[tokens.size()];
                        tokens.toArray(pattern);
                        ArrayList<Integer> matchIdxs = getIdxMatchesv2(pattern, tokenizedRawText.toArray(new String[tokenizedRawText.size()]));
                        DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(sentence.getRawText());
                        ArrayList<DependencyNode> arrDepNodes = new ArrayList<DependencyNode>();

                        if (matchIdxs != null) {
                            for (int j = 1; j <= tree.lastKey(); j++) {
                                if (matchIdxs.contains(j)) {
                                    arrDepNodes.add(tree.get(j));
                                }
                            }
                        }
                        ArgumentSpan span = new ArgumentSpan(arrDepNodes, roles[i]);
                        int annotationIdx = fieldIdxMap.get("is" + roles[i]);
                        if (sentenceData[annotationIdx].length() > 0) { // IF contains annotation
                            if (matchIdxs != null) {
                                isAnnotated = true;
                                if (sentenceData[annotationIdx].equalsIgnoreCase("1")) {
                                    span.setAnnotatedLabel("1");
                                } else {
                                    span.setAnnotatedLabel("-1");
                                }
                            }
                            span.setPattern(sentenceMap.get(sentenceStr).get(0)[fieldIdxMap.get("pattern")]);
                            spans.add(span);
                        }
                    }
                    if (roleAnnotationSpan.get(roles[i]) == null) {
                        roleAnnotationSpan.put(roles[i], spans);
                    } else {
                        ArrayList<ArgumentSpan> existingSpans = roleAnnotationSpan.get(roles[i]);
                        existingSpans.addAll(spans);
                        roleAnnotationSpan.put(roles[i], existingSpans);
                    }
                }
            }
            if (skipNotAnnotated) {
                if (isAnnotated) {
                    sentence.setAnnotated(isAnnotated);
                    sentence.setRoleArgAnnotation(roleAnnotationSpan);
                    sentences.add(sentence);
                }
            }
            else
            {
                sentence.setRoleArgAnnotation(roleAnnotationSpan);
                sentences.add(sentence);
            }
        }
        
        // SET THE ID
        for (int i = 0; i < sentences.size(); i++)
        {
            sentences.get(i).setId(i);
            int argId = 0;
            ArrayList<ArgumentSpan> spans = sentences.get(i).getAllAnnotatedArgumentSpan();
            for (ArgumentSpan span : spans)
            {
                span.setId(argId++);
            }
        }
    }

    public ArrayList<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(ArrayList<Sentence> sentences) {
        this.sentences = sentences;
    }

    
    public void readProcessData() throws FileNotFoundException, IOException {
        /*String[] lines = FileUtil.readLinesFromFile(processFileName);

         Scanner scanner = new Scanner(new File(processFileName));
         String[] header = scanner.nextLine().split("\t");
         mapFieldIdx(header);
         while (scanner.hasNextLine()) {
         String line = scanner.nextLine();
         String fields[] = line.split("\t");
         ArgProcessAnnotationData procDat = new ArgProcessAnnotationData();
         procDat.setProcessName(fields[fieldIdxMap.get("process")]);
         procDat.setRawText(fields[fieldIdxMap.get("sentence")].trim());
         procDat.setQuery(fields[fieldIdxMap.get("query")]);
         procDat.setPattern(fields[fieldIdxMap.get("pattern")]);
         Sentence sentence = new Sentence(procDat.getRawText());
         // Role 
         String roleFields[] = fieldMap.get("role").split(":");

         // for each role
         // strictly one arg per role
         boolean annotated = false;
         HashMap<String, ArrayList<ArgumentSpan>> roleSpanMap = new HashMap<String, ArrayList<ArgumentSpan>>();
         for (int i = 0; i < roleFields.length; i++) {
         int roleColumnIdx = fieldIdxMap.get(roleFields[i]);
         ArrayList<ArgumentSpan> spans = new ArrayList<ArgumentSpan>();
         if (fields[roleColumnIdx].length() > 0) { // IF role filler is not empty
         // Set role filler
         List<String> tokens = StanfordTokenizerSingleton.getInstance().tokenize(fields[roleColumnIdx].trim());
         List<String> tokenizedRawText = StanfordTokenizerSingleton.getInstance().tokenize(procDat.getRawText());
         String[] pattern = new String[tokens.size()];
         tokens.toArray(pattern);
         ArrayList<Integer> matchIdxs = getIdxMatchesv2(pattern, tokenizedRawText.toArray(new String[tokenizedRawText.size()]));
         DependencyTree tree = StanfordDepParserSingleton.getInstance().parse(procDat.getRawText());
         ArrayList<DependencyNode> arrDepNodes = new ArrayList<DependencyNode>();

         if (matchIdxs != null) {
         for (int j = 1; j <= tree.lastKey(); j++) {
         if (matchIdxs.contains(j)) {
         arrDepNodes.add(tree.get(j));
         }
         }
         }
         ArgumentSpan span = new ArgumentSpan(arrDepNodes, roleFields[i]);
         int annotationIdx = fieldIdxMap.get("is" + roleFields[i]);
         if (annotationIdx < fields.length) { // IF contains annotation
         if (matchIdxs != null) {

         if (fields[annotationIdx].equalsIgnoreCase("1")) {
         span.setAnnotatedLabel("1");
         } else {
         span.setAnnotatedLabel("-1");
         }
         annotated = true;
         }
         }
         spans.add(span);
         }
         roleSpanMap.put(roleFields[i], spans);
         }
         //sentence.setRoleArgMap(roleSpanMap);
         procDat.setSentence(sentence);
         procDat.setAnnotated(annotated);
         //      if not empty then set the role filler
         //      check whether annotation exists or not
         //          if annotation exist then
         //              record its indexes and annotation type
         //              add to array

         // Role annotation
         //procDataArr.add(procDat);
         }*/
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        SpockDataReader reader = new SpockDataReader("/home/slouvan/NetBeansProjects/SRL-Integrated/data/training.tsv",
                "/home/slouvan/NetBeansProjects/SRL-Integrated/configFrameFile/config.txt");
        reader.readData();
    }

}
